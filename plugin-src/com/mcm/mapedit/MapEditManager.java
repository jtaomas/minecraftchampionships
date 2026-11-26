package com.mcm.mapedit;

import com.mcm.api.McmApi;
import com.mcm.api.McmEditSpace;
import com.mcm.api.McmGame;
import com.mcm.api.McmMapEditable;
import com.mcm.api.McmMapOverlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * Map Edit mode (Map Edit core): an op opens a game's map "as played" (built by the game itself, no game logic), edits it
 * in creative, and on leaving / disconnecting / {@code /mcm mapedit save} the difference from the original is stored
 * as a {@link McmMapOverlay} in the game's plugin folder, which the game applies on every later build.
 *
 * <p>Sessions: one editor per map, any number of maps at once. Only while no game is running (a starting game evicts
 * the editors first, saving their work).
 */
public final class MapEditManager implements Listener {
    private enum Phase { BUILDING, LOADING, SCANNING, APPLYING, EDITING, SAVING, CLOSED }

    private static final class Saved {
        Location loc; GameMode gm; ItemStack[] contents; boolean allowFlight, flying; double health; int food;
    }

    private final class Session {
        final UUID uid; final String name; final String gameId, mapId, key;
        final McmMapEditable game; final Plugin owner;
        volatile Phase phase = Phase.BUILDING;
        volatile boolean cancelled;
        McmEditSpace space; RegionScan scan;
        final Set<Long> tickets = ConcurrentHashMap.newKeySet();
        McmMapOverlay existing;
        Saved saved; BossBar bar;
        long enteredAt, lastSave; int saves;
        boolean leaveWhenDone, leaveTeleport = true, playerGone, quiet;
        String enterAnchor;
        BukkitTask buildTimeout;
        Session(Player p, String gameId, String mapId, McmMapEditable game, Plugin owner) {
            this.uid = p.getUniqueId(); this.name = p.getName(); this.gameId = gameId; this.mapId = mapId;
            this.key = gameId + "/" + mapId; this.game = game; this.owner = owner;
        }
    }

    private final Plugin plugin;
    private final Map<String, Session> byMap = new LinkedHashMap<>();
    private final Map<UUID, Session> byPlayer = new HashMap<>();
    private final NamespacedKey toolKey;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "MapEdit-scan"); t.setDaemon(true); return t; });
    private BukkitTask hud;
    private final File stateDir;

    public MapEditManager(Plugin plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "mapedit_tool");
        this.stateDir = new File(plugin.getDataFolder(), "mapedit-state");
    }

    public void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        hud = Bukkit.getScheduler().runTaskTimer(plugin, this::hudTick, 20L, 20L);
    }

    /** Server stop / plugin disable: save every open session synchronously and give players their state back. */
    public void disable() {
        if (hud != null) hud.cancel();
        for (Session s : new ArrayList<>(byMap.values())) {
            try {
                if (s.phase == Phase.EDITING || s.phase == Phase.SAVING) {
                    Player p = Bukkit.getPlayer(s.uid);
                    if (p != null && !s.playerGone) restorePlayer(p, s, true);
                    saveSync(s);
                } else {
                    s.cancelled = true;
                    closeSession(s);
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "MapEdit: could not save " + s.key + " on shutdown", t);
            }
        }
        worker.shutdownNow();
    }

    private int maxOverlay() { return plugin.getConfig().getInt("mapedit.max-overlay-blocks", 750_000); }
    private int maxChunks() { return plugin.getConfig().getInt("mapedit.max-region-chunks", 4096); }
    private int autosaveMinutes() { return plugin.getConfig().getInt("mapedit.autosave-minutes", 10); }

    // ================================================================== public hooks

    public boolean isEditing(Player p) { return byPlayer.containsKey(p.getUniqueId()); }

    /** A game is about to start: save and send every editor back (their blocks are saved in the background). */
    public void evictAll(String reason) {
        for (Session s : new ArrayList<>(byMap.values())) {
            Player p = Bukkit.getPlayer(s.uid);
            if (p != null) p.sendMessage(ChatColor.GOLD + "[Map Edit] " + ChatColor.YELLOW + reason + " Saving and leaving " + s.key + "...");
            if (s.phase == Phase.EDITING) { detach(s, p, true); }
            else if (s.phase != Phase.SAVING && s.phase != Phase.CLOSED) { s.cancelled = true; if (p != null) cancelEarly(s, p); }
        }
    }

    // ================================================================== commands

    private static final String USAGE =
            "/mcm mapedit [<game> <map> [anchor]] | list [game] | save | leave | discard | tp <anchor> | anchors | status | reset <game> <map> | backups <game> <map> | restore <game> <map> [n]";

    public boolean command(CommandSender sender, String[] args) {
        if (sender instanceof Player p && !p.isOp()) { p.sendMessage(ChatColor.RED + "Only tournament admins (OPs) can use Map Edit."); return true; }
        String sub = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        Player p = sender instanceof Player pl ? pl : null;
        switch (sub) {
            case "" -> { if (p == null) listChat(sender, null); else openMenu(p); }
            case "list", "ls" -> listChat(sender, args.length > 2 ? args[2] : null);
            case "save" -> { if (needPlayer(sender)) return true; Session s = active(p); if (s != null) saveCmd(s, p, false); }
            case "leave", "exit", "done", "quit" -> { if (needPlayer(sender)) return true; Session s = active(p); if (s != null) saveCmd(s, p, true); }
            case "discard", "cancel" -> { if (needPlayer(sender)) return true; Session s = active(p); if (s != null) discard(s, p); }
            case "tp", "goto" -> { if (needPlayer(sender)) return true; Session s = active(p); if (s != null) tpAnchor(s, p, args.length > 2 ? args[2] : null); }
            case "anchors" -> { if (needPlayer(sender)) return true; Session s = active(p); if (s != null) tpAnchor(s, p, null); }
            case "status" -> status(sender);
            case "reset" -> resetCmd(sender, args);
            case "backups" -> backupsCmd(sender, args);
            case "restore", "undo" -> restoreCmd(sender, args);
            case "help" -> sender.sendMessage(ChatColor.GOLD + "Map Edit: " + ChatColor.YELLOW + USAGE);
            default -> {
                if (needPlayer(sender)) return true;
                if (args.length < 3) { sender.sendMessage(ChatColor.YELLOW + "Usage: " + USAGE); return true; }
                enter(p, args[1], args[2], args.length > 3 ? args[3] : null);
            }
        }
        return true;
    }

    private boolean needPlayer(CommandSender s) {
        if (s instanceof Player) return false;
        s.sendMessage("This Map Edit command is for players in game (console can use list, status, reset, backups, restore).");
        return true;
    }

    private Session active(Player p) {
        Session s = byPlayer.get(p.getUniqueId());
        if (s == null) { p.sendMessage(ChatColor.RED + "You are not in Map Edit mode. Use /mcm mapedit <game> <map>."); return null; }
        return s;
    }

    public List<String> tab(CommandSender sender, String[] args) {
        List<String> out = new ArrayList<>();
        if (sender instanceof Player p && !p.isOp()) return out;
        if (args.length == 2) {
            out.addAll(List.of("list", "save", "leave", "discard", "tp", "anchors", "status", "reset", "backups", "restore"));
            out.addAll(McmApi.mapEditableGames());
        } else if (args.length == 3) {
            String a = args[1].toLowerCase(Locale.ROOT);
            if (List.of("list", "reset", "backups", "restore").contains(a) || McmApi.mapEditable(a) != null) {
                if (McmApi.mapEditable(a) != null) out.addAll(maps(a));
                else out.addAll(McmApi.mapEditableGames());
            } else if (a.equals("tp") && sender instanceof Player p) {
                Session s = byPlayer.get(p.getUniqueId());
                if (s != null && s.space != null) out.addAll(s.space.anchors.keySet());
            }
        } else if (args.length == 4) {
            String a = args[1].toLowerCase(Locale.ROOT);
            if (List.of("reset", "backups", "restore").contains(a)) out.addAll(maps(args[2].toLowerCase(Locale.ROOT)));
        }
        String pre = args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(pre));
        return out;
    }

    private List<String> maps(String gameId) {
        McmMapEditable e = McmApi.mapEditable(gameId);
        if (e == null) return List.of();
        try { return e.editableMaps(); } catch (Throwable t) { return List.of(); }
    }

    // ================================================================== listing

    private String gameName(String id) {
        McmGame g = McmApi.game(id);
        return g != null ? g.displayName() : id;
    }

    private Material gameIcon(String id) {
        McmGame g = McmApi.game(id);
        try { return g != null ? g.icon() : Material.COMMAND_BLOCK; } catch (Throwable t) { return Material.COMMAND_BLOCK; }
    }

    private String mapName(McmMapEditable e, String id) {
        try { String n = e.editableMapName(id); return n == null ? id : n; } catch (Throwable t) { return id; }
    }

    private McmMapOverlay overlayOf(String gameId, String mapId) {
        Plugin o = McmApi.mapEditableOwner(gameId);
        return o == null ? null : McmMapOverlay.of(o, mapId);
    }

    private void listChat(CommandSender s, String only) {
        List<String> games = McmApi.mapEditableGames();
        if (games.isEmpty()) { s.sendMessage(ChatColor.YELLOW + "No installed game exposes editable maps."); return; }
        s.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Map Edit" + ChatColor.GRAY + " - click a map to edit it ("
                + ChatColor.YELLOW + "*" + ChatColor.GRAY + " = has saved edits)");
        for (String g : games) {
            if (only != null && !g.equalsIgnoreCase(only)) continue;
            McmMapEditable e = McmApi.mapEditable(g);
            Component line = Component.text(gameName(g) + " ", NamedTextColor.GOLD).append(Component.text("(" + g + "): ", NamedTextColor.DARK_GRAY));
            List<String> ids = maps(g);
            for (String m : ids) {
                McmMapOverlay ov = overlayOf(g, m);
                String busy = null;
                try { busy = e.editBlockedReason(m); } catch (Throwable ignored) { }
                Session ses = byMap.get(g + "/" + m);
                NamedTextColor col = busy != null ? NamedTextColor.RED : ses != null ? NamedTextColor.AQUA : NamedTextColor.GREEN;
                String hover = mapName(e, m) + (ov != null ? "\n" + ov.size() + " edited blocks, by " + ov.editor + ", " + fmt(ov.savedAt) : "\nno saved edits")
                        + (ses != null ? "\nbeing edited by " + ses.name : "") + (busy != null ? "\n" + busy : "");
                line = line.append(Component.text("[" + m + (ov != null ? "*" : "") + "] ", col)
                        .clickEvent(ClickEvent.runCommand("/mcm mapedit " + g + " " + m))
                        .hoverEvent(HoverEvent.showText(Component.text(hover))));
            }
            if (ids.isEmpty()) line = line.append(Component.text("(no maps)", NamedTextColor.GRAY));
            s.sendMessage(line);
        }
    }

    private static String fmt(long ms) { return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(ms)); }

    private void status(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "Map Edit sessions: " + ChatColor.WHITE + byMap.size());
        for (Session x : byMap.values())
            s.sendMessage(ChatColor.GRAY + " " + x.key + " - " + x.name + " [" + x.phase + "]" + (x.saves > 0 ? ", saved " + x.saves + "x" : ""));
    }

    // ================================================================== GUI

    private static final class MenuHolder implements InventoryHolder {
        final String game; final int page; Inventory inv;
        final Map<Integer, String> slots = new HashMap<>();
        MenuHolder(String game, int page) { this.game = game; this.page = page; }
        @Override public Inventory getInventory() { return inv; }
    }

    private ItemStack icon(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        if (lore.length > 0) im.setLore(Arrays.asList(lore));
        it.setItemMeta(im);
        return it;
    }

    public void openMenu(Player p) {
        List<String> games = McmApi.mapEditableGames();
        MenuHolder h = new MenuHolder(null, 0);
        h.inv = Bukkit.createInventory(h, 54, ChatColor.DARK_GREEN + "Map Edit - choose a game");
        int slot = 10;
        for (String g : games) {
            if (slot % 9 == 8) slot += 2;
            if (slot >= 44) break;
            int maps = maps(g).size(), edited = 0;
            for (String m : maps(g)) if (overlayOf(g, m) != null) edited++;
            h.inv.setItem(slot, icon(gameIcon(g), ChatColor.GOLD + "" + ChatColor.BOLD + gameName(g),
                    ChatColor.GRAY + "" + maps + " editable map(s)", ChatColor.GRAY + "" + edited + " with saved edits", "", ChatColor.YELLOW + "Click to browse"));
            h.slots.put(slot, g);
            slot++;
        }
        if (games.isEmpty()) h.inv.setItem(22, icon(Material.BARRIER, ChatColor.RED + "No game exposes editable maps"));
        p.openInventory(h.inv);
    }

    private void openGameMenu(Player p, String game, int page) {
        McmMapEditable e = McmApi.mapEditable(game);
        if (e == null) { openMenu(p); return; }
        List<String> ids = maps(game);
        MenuHolder h = new MenuHolder(game, page);
        h.inv = Bukkit.createInventory(h, 54, ChatColor.DARK_GREEN + "Map Edit - " + gameName(game));
        int from = page * 45;
        for (int i = from; i < Math.min(ids.size(), from + 45); i++) {
            String m = ids.get(i);
            McmMapOverlay ov = overlayOf(game, m);
            Session ses = byMap.get(game + "/" + m);
            String busy = null;
            try { busy = e.editBlockedReason(m); } catch (Throwable ignored) { }
            Material mat = null;
            try { mat = e.editableMapIcon(m); } catch (Throwable ignored) { }
            if (mat == null) mat = ov != null ? Material.FILLED_MAP : Material.MAP;
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.DARK_GRAY + m);
            lore.add(ov != null ? ChatColor.YELLOW + "" + ov.size() + " edited blocks (" + ov.editor + ", " + fmt(ov.savedAt) + ")" : ChatColor.GRAY + "no saved edits");
            if (ses != null) lore.add(ChatColor.AQUA + "Being edited by " + ses.name);
            if (busy != null) lore.add(ChatColor.RED + busy);
            lore.add("");
            lore.add(ChatColor.GREEN + "Click to edit in creative");
            ItemStack it = icon(mat, ChatColor.WHITE + "" + ChatColor.BOLD + mapName(e, m), lore.toArray(new String[0]));
            h.inv.setItem(i - from, it);
            h.slots.put(i - from, m);
        }
        h.inv.setItem(45, icon(Material.ARROW, ChatColor.YELLOW + "Back to games"));
        if (page > 0) h.inv.setItem(48, icon(Material.PAPER, ChatColor.YELLOW + "Previous page"));
        if (from + 45 < ids.size()) h.inv.setItem(50, icon(Material.PAPER, ChatColor.YELLOW + "Next page"));
        h.inv.setItem(53, icon(Material.BARRIER, ChatColor.RED + "Close"));
        p.openInventory(h.inv);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof MenuHolder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getView().getTopInventory()) return;
        if (!p.isOp()) { p.closeInventory(); return; }
        int slot = e.getSlot();
        if (h.game == null) {
            String g = h.slots.get(slot);
            if (g != null) openGameMenu(p, g, 0);
            return;
        }
        if (slot == 45) { openMenu(p); return; }
        if (slot == 53) { p.closeInventory(); return; }
        if (slot == 48 && h.page > 0) { openGameMenu(p, h.game, h.page - 1); return; }
        if (slot == 50) { openGameMenu(p, h.game, h.page + 1); return; }
        String map = h.slots.get(slot);
        if (map != null) { p.closeInventory(); enter(p, h.game, map, null); }
    }

    // ================================================================== enter

    private void enter(Player p, String gameArg, String mapArg, String anchor) {
        String gameId = gameArg.toLowerCase(Locale.ROOT);
        McmMapEditable game = McmApi.mapEditable(gameId);
        if (game == null) {
            for (String g : McmApi.mapEditableGames()) if (gameName(g).equalsIgnoreCase(gameArg)) { gameId = g; game = McmApi.mapEditable(g); }
        }
        if (game == null) { p.sendMessage(ChatColor.RED + "Unknown game '" + gameArg + "'. Editable: " + String.join(", ", McmApi.mapEditableGames())); return; }
        if (byPlayer.containsKey(p.getUniqueId())) { p.sendMessage(ChatColor.RED + "You are already editing " + byPlayer.get(p.getUniqueId()).key + ". /mcm mapedit leave first."); return; }
        String mapId = null;
        List<String> ids = maps(gameId);
        for (String m : ids) if (m.equalsIgnoreCase(mapArg)) mapId = m;
        if (mapId == null) {
            List<String> pre = new ArrayList<>();
            for (String m : ids) if (m.toLowerCase(Locale.ROOT).startsWith(mapArg.toLowerCase(Locale.ROOT))) pre.add(m);
            if (pre.size() == 1) mapId = pre.get(0);
        }
        if (mapId == null) { p.sendMessage(ChatColor.RED + "Unknown map '" + mapArg + "' of " + gameId + ". Maps: " + String.join(", ", ids)); return; }
        String running = McmApi.currentGameId();
        if (running != null && !running.equals("lobby")) {
            p.sendMessage(ChatColor.RED + "A game is running (" + running + "). Map Edit is only available in the lobby; /mcm stop first.");
            return;
        }
        String key = gameId + "/" + mapId;
        Session other = byMap.get(key);
        if (other != null) { p.sendMessage(ChatColor.RED + key + " is already being edited by " + other.name + "."); return; }
        String why = null;
        try { why = game.editBlockedReason(mapId); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "editBlockedReason", t); }
        if (why != null) { p.sendMessage(ChatColor.RED + "Cannot edit " + key + " now: " + why); return; }
        Plugin owner = McmApi.mapEditableOwner(gameId);
        if (owner == null) { p.sendMessage(ChatColor.RED + "Game plugin of " + gameId + " is not available."); return; }

        Session s = new Session(p, gameId, mapId, game, owner);
        s.enterAnchor = anchor;
        byMap.put(key, s);
        byPlayer.put(p.getUniqueId(), s);
        p.sendMessage(ChatColor.GOLD + "[Map Edit] " + ChatColor.YELLOW + "Building " + mapName(game, mapId) + " for editing...");
        s.buildTimeout = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (s.phase == Phase.BUILDING) { s.cancelled = true; fail(s, "the game did not finish building the map in 3 minutes"); }
        }, 20L * 180);
        try {
            game.buildForEdit(mapId, space -> {
                if (Bukkit.isPrimaryThread()) onBuilt(s, space);
                else Bukkit.getScheduler().runTask(plugin, () -> onBuilt(s, space));
            });
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "MapEdit: " + key + " buildForEdit threw", t);
            fail(s, "the game threw while building the map: " + t);
        }
    }

    private void fail(Session s, String why) {
        Player p = Bukkit.getPlayer(s.uid);
        if (p != null && !s.quiet) p.sendMessage(ChatColor.RED + "[Map Edit] Could not open " + s.key + ": " + why);
        plugin.getLogger().warning("MapEdit: " + s.name + " " + s.key + " failed: " + why);
        s.cancelled = true;
        closeSession(s);
    }

    private void cancelEarly(Session s, Player p) {
        if (p != null) p.sendMessage(ChatColor.YELLOW + "[Map Edit] Cancelled (nothing was changed).");
        closeSession(s);
    }

    private void onBuilt(Session s, McmEditSpace space) {
        if (s.buildTimeout != null) s.buildTimeout.cancel();
        if (s.phase == Phase.CLOSED) { return; }
        if (s.phase != Phase.BUILDING) return;
        if (space == null) { fail(s, "the game could not build the map (see the server log)"); return; }
        if (s.cancelled) { s.space = space; closeSession(s); return; }
        s.space = space;
        Player p = Bukkit.getPlayer(s.uid);
        if (p == null) { closeSession(s); return; }
        s.existing = McmMapOverlay.of(s.owner, s.mapId);
        int[] ob = s.existing == null ? null : s.existing.bounds();
        int ax = space.minX, ay = space.minY, az = space.minZ, bx = space.maxX, by = space.maxY, bz = space.maxZ;
        if (ob != null) { ax = Math.min(ax, ob[0]); ay = Math.min(ay, ob[1]); az = Math.min(az, ob[2]); bx = Math.max(bx, ob[3]); by = Math.max(by, ob[4]); bz = Math.max(bz, ob[5]); }
        RegionScan sc = new RegionScan(space.world, space.originX + ax, space.originY + ay, space.originZ + az,
                space.originX + bx, space.originY + by, space.originZ + bz);
        if (sc.chunkCount() > maxChunks() || sc.cells() > 200_000_000L) {
            fail(s, "the edit area is too big (" + sc.chunkCount() + " chunks, " + sc.cells() + " cells)");
            return;
        }
        s.scan = sc;
        s.phase = Phase.LOADING;
        p.sendMessage(ChatColor.GRAY + "[Map Edit] Loading " + sc.chunkCount() + " chunks...");
        new RegionScan.LoadJob(plugin, sc.world, sc.chunkKeys(), s.tickets, () -> s.cancelled, () -> {
            if (s.cancelled) { closeSession(s); return; }
            baseline(s);
        }).start();
        // a cancelled load never calls back: closeSession is triggered by whoever cancelled
    }

    private void baseline(Session s) {
        s.phase = Phase.SCANNING;
        RegionScan sc = s.scan;
        new RegionScan.SnapJob(plugin, sc.world, sc.chunkKeys(), worker, () -> s.cancelled,
                (cx, cz, snap) -> sc.baseline.put(RegionScan.ck(cx, cz), sc.scan(snap, cx, cz)),
                err -> {
                    if (s.cancelled) { closeSession(s); return; }
                    if (err != null) { plugin.getLogger().log(Level.SEVERE, "MapEdit baseline", err); fail(s, "could not read the map: " + err); return; }
                    applyExisting(s);
                }).start();
    }

    private void applyExisting(Session s) {
        s.phase = Phase.APPLYING;
        McmMapOverlay ov = s.existing;
        if (ov == null || ov.isEmpty()) { finishEnter(s); return; }
        McmEditSpace sp = s.space;
        int n = ov.size();
        int[] xs = new int[n], ys = new int[n], zs = new int[n];
        String[] st = new String[n];
        int[] i = {0};
        ov.forEach((x, y, z, state) -> { int k = i[0]++; xs[k] = x; ys[k] = y; zs[k] = z; st[k] = state; });
        Map<String, BlockData> cache = new HashMap<>();
        int[] cur = {0};
        BukkitTask[] t = new BukkitTask[1];
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (s.cancelled) { t[0].cancel(); closeSession(s); return; }
            long deadline = System.nanoTime() + 25_000_000L;
            int minH = sp.world.getMinHeight(), maxH = sp.world.getMaxHeight();
            while (cur[0] < n) {
                int k = cur[0]++;
                int y = sp.originY + ys[k];
                if (y >= minH && y < maxH) {
                    BlockData d = cache.computeIfAbsent(st[k], str -> { try { return Bukkit.createBlockData(str); } catch (IllegalArgumentException e) { return null; } });
                    if (d != null) sp.world.getBlockAt(sp.originX + xs[k], y, sp.originZ + zs[k]).setBlockData(d, false);
                }
                if ((cur[0] & 255) == 0 && System.nanoTime() > deadline) break;
            }
            if (cur[0] >= n) { t[0].cancel(); finishEnter(s); }
        }, 1L, 1L);
    }

    private void finishEnter(Session s) {
        Player p = Bukkit.getPlayer(s.uid);
        if (s.cancelled || p == null || !p.isOnline()) { closeSession(s); return; }
        McmEditSpace sp = s.space;
        Saved sv = new Saved();
        sv.loc = p.getLocation().clone(); sv.gm = p.getGameMode(); sv.contents = p.getInventory().getContents().clone();
        sv.allowFlight = p.getAllowFlight(); sv.flying = p.isFlying(); sv.health = p.getHealth(); sv.food = p.getFoodLevel();
        s.saved = sv;
        persistState(p, sv);
        Location target = sp.spawn;
        if (s.enterAnchor != null) {
            for (Map.Entry<String, Location> a : sp.anchors.entrySet())
                if (a.getKey().equalsIgnoreCase(s.enterAnchor)) target = a.getValue();
        }
        if (p.isInsideVehicle()) p.leaveVehicle();
        p.getInventory().clear();
        p.setGameMode(GameMode.CREATIVE);
        p.setAllowFlight(true);
        p.setFlying(true);
        p.setFoodLevel(20);
        p.setHealth(p.getMaxHealth());
        s.phase = Phase.EDITING;
        s.enteredAt = s.lastSave = System.currentTimeMillis();
        s.bar = Bukkit.createBossBar(ChatColor.GREEN + "" + ChatColor.BOLD + "MAP EDIT: " + s.key + ChatColor.GRAY + "  /mcm mapedit save | leave",
                BarColor.GREEN, BarStyle.SOLID);
        s.bar.addPlayer(p);
        giveTools(p);
        p.teleport(target);
        Bukkit.getScheduler().runTaskLater(plugin, () -> reassert(s), 2L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> reassert(s), 10L);
        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "[Map Edit] " + ChatColor.GREEN + s.key + ChatColor.GRAY + " is yours: creative, no game logic running.");
        p.sendMessage(ChatColor.GRAY + "Edit area: " + ChatColor.WHITE + (sp.maxX - sp.minX + 1) + "x" + (sp.maxY - sp.minY + 1) + "x" + (sp.maxZ - sp.minZ + 1)
                + ChatColor.GRAY + " blocks round the map; blocks outside it are not saved. "
                + (s.existing != null ? ChatColor.YELLOW + "Showing the map with " + s.existing.size() + " saved edited blocks." : "No saved edits yet."));
        for (String note : sp.notes) p.sendMessage(ChatColor.GRAY + " - " + note);
        p.sendMessage(ChatColor.YELLOW + "/mcm mapedit save" + ChatColor.GRAY + " keeps editing, " + ChatColor.YELLOW + "/mcm mapedit leave" + ChatColor.GRAY
                + " (or the red leave item, or disconnecting) saves and exits. " + ChatColor.YELLOW + "/mcm mapedit discard" + ChatColor.GRAY + " exits without saving."
                + (sp.anchors.isEmpty() ? "" : " " + ChatColor.YELLOW + "/mcm mapedit tp <" + sp.anchors.size() + " places>"));
        plugin.getLogger().info("MapEdit: " + s.name + " entered " + s.key + " (" + sp.world.getName() + " origin " + sp.originX + "," + sp.originY + "," + sp.originZ
                + ", " + s.scan.chunkCount() + " chunks, overlay " + (s.existing == null ? "none" : s.existing.size() + " blocks") + ")");
    }

    // ================================================================== player state

    private void giveTools(Player p) {
        p.getInventory().setItem(6, tool(Material.EMERALD, "save", ChatColor.GREEN + "" + ChatColor.BOLD + "Save changes (keep editing)"));
        p.getInventory().setItem(7, tool(Material.BARRIER, "leave", ChatColor.RED + "" + ChatColor.BOLD + "Save & leave Map Edit"));
    }

    private ItemStack tool(Material m, String id, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta im = it.getItemMeta();
        im.setDisplayName(name);
        im.setLore(List.of(ChatColor.GRAY + "Right-click"));
        im.getPersistentDataContainer().set(toolKey, PersistentDataType.STRING, id);
        it.setItemMeta(im);
        return it;
    }

    private String toolId(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(toolKey, PersistentDataType.STRING);
    }

    private void persistState(Player p, Saved sv) {
        try {
            stateDir.mkdirs();
            YamlConfiguration y = new YamlConfiguration();
            y.set("gamemode", sv.gm.name());
            y.set("loc", sv.loc);
            y.set("contents", Arrays.asList(sv.contents));
            y.set("allowFlight", sv.allowFlight);
            y.set("health", sv.health);
            y.set("food", sv.food);
            y.save(new File(stateDir, p.getUniqueId() + ".yml"));
        } catch (Throwable t) {
            plugin.getLogger().warning("MapEdit: could not persist " + p.getName() + "'s inventory: " + t);
        }
    }

    private void restorePlayer(Player p, Session s, boolean teleport) {
        if (s.playerGone) return;
        s.playerGone = true;
        Saved sv = s.saved;
        if (s.bar != null) { s.bar.removeAll(); s.bar = null; }
        if (sv == null) return;
        try {
            if (p.isInsideVehicle()) p.leaveVehicle();
            p.getInventory().clear();
            p.getInventory().setContents(sv.contents);
            p.setGameMode(sv.gm);
            p.setAllowFlight(sv.allowFlight || sv.gm == GameMode.CREATIVE || sv.gm == GameMode.SPECTATOR);
            if (!sv.allowFlight && sv.gm != GameMode.CREATIVE) p.setFlying(false);
            p.setFoodLevel(sv.food);
            p.setHealth(Math.max(1, Math.min(p.getMaxHealth(), sv.health)));
            if (teleport) {
                Location l = sv.loc;
                if (l == null || l.getWorld() == null || Bukkit.getWorld(l.getWorld().getUID()) == null) l = Bukkit.getWorlds().get(0).getSpawnLocation();
                p.teleport(l);
            }
            p.updateInventory();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "MapEdit: restoring " + p.getName() + " failed", t);
        }
        new File(stateDir, p.getUniqueId() + ".yml").delete();
    }

    /** After a crash while editing: give the player their inventory and mode back at login. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        File f = new File(stateDir, p.getUniqueId() + ".yml");
        if (!f.isFile() || byPlayer.containsKey(p.getUniqueId())) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline() || byPlayer.containsKey(p.getUniqueId())) return;
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                List<?> raw = y.getList("contents");
                if (raw != null) {
                    ItemStack[] arr = new ItemStack[raw.size()];
                    for (int i = 0; i < arr.length; i++) arr[i] = raw.get(i) instanceof ItemStack it ? it : null;
                    p.getInventory().setContents(arr);
                }
                GameMode gm = GameMode.valueOf(y.getString("gamemode", "ADVENTURE"));
                p.setGameMode(gm);
                p.setAllowFlight(gm == GameMode.CREATIVE || y.getBoolean("allowFlight"));
                plugin.getLogger().info("MapEdit: restored " + p.getName() + "'s inventory and mode after an interrupted edit session");
                p.sendMessage(ChatColor.GOLD + "[Map Edit] " + ChatColor.GRAY + "Your inventory was restored after an interrupted edit session.");
            } catch (Throwable t) {
                plugin.getLogger().warning("MapEdit: could not restore saved state of " + p.getName() + ": " + t);
            }
            f.delete();
        }, 40L);
    }

    private void reassert(Session s) {
        Player p = Bukkit.getPlayer(s.uid);
        if (p == null || s.phase == Phase.CLOSED || s.playerGone || (s.phase != Phase.EDITING && s.phase != Phase.SAVING)) return;
        if (p.getGameMode() != GameMode.CREATIVE) p.setGameMode(GameMode.CREATIVE);
        if (!p.getAllowFlight()) p.setAllowFlight(true);
    }

    private void hudTick() {
        long now = System.currentTimeMillis();
        for (Session s : new ArrayList<>(byMap.values())) {
            if (s.phase != Phase.EDITING && s.phase != Phase.SAVING) continue;
            Player p = Bukkit.getPlayer(s.uid);
            if (p == null || s.playerGone) continue;
            reassert(s);
            if (s.space != null && p.getWorld().equals(s.space.world) && p.getLocation().getY() < s.space.world.getMinHeight() - 8)
                p.teleport(s.space.spawn);
            String extra = s.phase == Phase.SAVING ? " - saving..." : s.saves > 0 ? " - saved " + (now - s.lastSave) / 60000 + " min ago" : " - unsaved";
            p.sendActionBar(Component.text("MAP EDIT: " + s.key + extra, NamedTextColor.GREEN).decorate(TextDecoration.BOLD));
            int am = autosaveMinutes();
            if (s.phase == Phase.EDITING && am > 0 && now - s.lastSave > am * 60_000L && !s.quiet) {
                s.lastSave = now;
                startSave(s, p, false, true);
            }
        }
    }

    // ================================================================== listeners

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && byPlayer.containsKey(p.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && byPlayer.containsKey(p.getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (toolId(e.getItemDrop().getItemStack()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (toolId(e.getMainHandItem()) != null || toolId(e.getOffHandItem()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onToolClick(InventoryClickEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof MenuHolder) return;
        if (toolId(e.getCurrentItem()) != null || toolId(e.getCursor()) != null) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        String id = toolId(e.getItem());
        if (id == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        Session s = byPlayer.get(p.getUniqueId());
        if (s == null) { p.getInventory().remove(e.getItem()); return; }
        if (id.equals("save")) saveCmd(s, p, false);
        else if (id.equals("leave")) saveCmd(s, p, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Session s = byPlayer.get(e.getPlayer().getUniqueId());
        if (s == null || s.phase != Phase.EDITING || s.space == null || s.playerGone) return;
        if (e.getPlayer().getWorld().equals(s.space.world)) return;
        e.getPlayer().sendMessage(ChatColor.YELLOW + "[Map Edit] You left the edit world: saving " + s.key + ".");
        detach(s, e.getPlayer(), false);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        Session s = byPlayer.get(p.getUniqueId());
        if (s == null) return;
        switch (s.phase) {
            case EDITING -> {
                plugin.getLogger().info("MapEdit: " + p.getName() + " disconnected while editing " + s.key + ": saving");
                s.quiet = true;
                restorePlayer(p, s, true);
                byPlayer.remove(p.getUniqueId());
                if (Bukkit.isStopping()) saveSync(s); else startSave(s, null, true, false);
            }
            case SAVING -> {
                s.leaveWhenDone = true; s.quiet = true;
                restorePlayer(p, s, true);
                byPlayer.remove(p.getUniqueId());
            }
            case CLOSED -> byPlayer.remove(p.getUniqueId());
            default -> { s.cancelled = true; s.quiet = true; byPlayer.remove(p.getUniqueId()); closeSession(s); }
        }
    }

    // ================================================================== save / leave

    private void saveCmd(Session s, Player p, boolean leave) {
        if (s.phase == Phase.SAVING) { p.sendMessage(ChatColor.YELLOW + "[Map Edit] Already saving..."); if (leave) s.leaveWhenDone = true; return; }
        if (s.phase != Phase.EDITING) { p.sendMessage(ChatColor.YELLOW + "[Map Edit] Not ready yet (" + s.phase + ")."); return; }
        s.leaveTeleport = true;
        startSave(s, p, leave, false);
    }

    /** Give the player their state back right now and save in the background (game starting, world change). */
    private void detach(Session s, Player p, boolean teleport) {
        if (s.phase != Phase.EDITING) return;
        if (p != null) restorePlayer(p, s, teleport);
        byPlayer.remove(s.uid);
        startSave(s, p, true, false);
    }

    private void discard(Session s, Player p) {
        if (s.phase == Phase.SAVING) { p.sendMessage(ChatColor.YELLOW + "[Map Edit] A save is running; wait for it."); return; }
        if (s.phase != Phase.EDITING) { p.sendMessage(ChatColor.YELLOW + "[Map Edit] Not ready yet (" + s.phase + ")."); return; }
        plugin.getLogger().info("MapEdit: " + s.name + " discarded their edits of " + s.key);
        restorePlayer(p, s, true);
        byPlayer.remove(p.getUniqueId());
        p.sendMessage(ChatColor.YELLOW + "[Map Edit] Left " + s.key + " without saving (the saved overlay is unchanged).");
        closeSession(s);
    }

    private void tpAnchor(Session s, Player p, String id) {
        if (s.phase != Phase.EDITING || s.space == null) { p.sendMessage(ChatColor.YELLOW + "[Map Edit] Not ready."); return; }
        if (id == null) {
            p.sendMessage(ChatColor.GOLD + "Places in " + s.key + ": " + ChatColor.WHITE + (s.space.anchors.isEmpty() ? "(none)" : String.join(", ", s.space.anchors.keySet())));
            return;
        }
        for (Map.Entry<String, Location> a : s.space.anchors.entrySet()) {
            if (a.getKey().equalsIgnoreCase(id)) { p.teleport(a.getValue()); p.sendMessage(ChatColor.GRAY + "[Map Edit] -> " + a.getKey()); return; }
        }
        p.sendMessage(ChatColor.RED + "No such place. Places: " + String.join(", ", s.space.anchors.keySet()));
    }

    // diff result
    private static final class Diff {
        int[] x = new int[1024], y = new int[1024], z = new int[1024];
        char[] id = new char[1024];
        int n; boolean overflow; final int cap;
        Diff(int cap) { this.cap = cap; }
        synchronized void add(int px, int py, int pz, char pid) {
            if (n >= cap) { overflow = true; return; }
            if (n == x.length) {
                int m = n * 2;
                x = Arrays.copyOf(x, m); y = Arrays.copyOf(y, m); z = Arrays.copyOf(z, m); id = Arrays.copyOf(id, m);
            }
            x[n] = px; y[n] = py; z[n] = pz; id[n] = pid; n++;
        }
    }

    private void startSave(Session s, Player p, boolean leave, boolean auto) {
        if (s.phase != Phase.EDITING) return;
        s.phase = Phase.SAVING;
        s.leaveWhenDone = leave;
        if (p != null && !s.quiet) p.sendMessage(ChatColor.GRAY + "[Map Edit] " + (auto ? "Autosaving" : "Saving") + " " + s.key + " (comparing " + s.scan.chunkCount() + " chunks)...");
        Diff diff = new Diff(maxOverlay());
        RegionScan sc = s.scan;
        new RegionScan.SnapJob(plugin, sc.world, sc.chunkKeys(), worker, () -> false,
                (cx, cz, snap) -> sc.diff(sc.scan(snap, cx, cz), sc.baseline.get(RegionScan.ck(cx, cz)), cx, cz, diff::add),
                err -> finishSave(s, diff, err)).start();
    }

    private void saveSync(Session s) {
        s.phase = Phase.SAVING;
        Diff diff = new Diff(maxOverlay());
        RegionScan sc = s.scan;
        Throwable err = sc.runSync(sc.chunkKeys(), (cx, cz, snap) -> sc.diff(sc.scan(snap, cx, cz), sc.baseline.get(RegionScan.ck(cx, cz)), cx, cz, diff::add));
        finishSave(s, diff, err);
    }

    private void finishSave(Session s, Diff diff, Throwable err) {
        Player p = Bukkit.getPlayer(s.uid);
        boolean online = p != null && p.isOnline() && !s.quiet;
        if (err != null || diff.overflow) {
            String why = err != null ? "reading the world failed: " + err : "too many changed blocks (more than " + maxOverlay() + ")";
            plugin.getLogger().severe("MapEdit: " + s.name + " " + s.key + " NOT saved: " + why);
            if (err != null) plugin.getLogger().log(Level.SEVERE, "MapEdit scan error", err);
            if (online) p.sendMessage(ChatColor.RED + "[Map Edit] NOT saved: " + why + ". You are still editing; undo some changes or /mcm mapedit discard.");
            if (s.playerGone) closeSession(s); else s.phase = Phase.EDITING;
            s.leaveWhenDone = false;
            return;
        }
        // build the overlay
        McmEditSpace sp = s.space;
        McmMapOverlay.Builder b = new McmMapOverlay.Builder();
        Map<Character, String> strs = new HashMap<>();
        for (int i = 0; i < diff.n; i++) {
            char c = diff.id[i];
            String str = strs.get(c);
            if (str == null) {
                BlockData d = s.scan.palette.get(c);
                str = d == null ? "minecraft:air" : d.getAsString();
                strs.put(c, str);
            }
            b.add(diff.x[i] - sp.originX, diff.y[i] - sp.originY, diff.z[i] - sp.originZ, str);
        }
        McmMapOverlay ov = b.build(s.gameId, s.mapId, s.name);
        List<String> warnings = new ArrayList<>();
        try { warnings.addAll(markerWarnings(s)); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "MapEdit markers", t); }
        try { warnings.addAll(s.game.editWarnings(s.mapId, sp)); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "editWarnings", t); }
        File f = McmMapOverlay.file(s.owner, s.mapId);
        String result;
        try {
            McmMapOverlay old = McmMapOverlay.load(f);
            int oldN = old == null ? 0 : old.size();
            if (f.isFile()) backup(f, s.mapId);
            if (ov.isEmpty()) {
                if (f.isFile()) Files.deleteIfExists(f.toPath());
                McmMapOverlay.invalidate(f);
                result = "no differences from the original: overlay " + (oldN > 0 ? "removed (was " + oldN + " blocks, backed up)" : "not needed");
            } else {
                ov.write(f);
                result = ov.size() + " blocks differ from the original" + (oldN > 0 ? " (was " + oldN + ")" : "");
            }
        } catch (IOException | RuntimeException ex) {
            plugin.getLogger().log(Level.SEVERE, "MapEdit: could not write " + f, ex);
            if (online) p.sendMessage(ChatColor.RED + "[Map Edit] NOT saved: could not write the overlay file: " + ex);
            if (s.playerGone) closeSession(s); else s.phase = Phase.EDITING;
            s.leaveWhenDone = false;
            return;
        }
        s.saves++;
        s.lastSave = System.currentTimeMillis();
        plugin.getLogger().info("MapEdit: " + s.name + " saved " + s.key + ": " + result + " -> " + f.getName());
        try { s.game.overlayChanged(s.mapId); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "overlayChanged", t); }
        if (online) {
            p.sendMessage(ChatColor.GREEN + "[Map Edit] Saved " + s.key + ": " + result + ". It is used the next time the game builds this map.");
            int shown = 0;
            for (String w : warnings) { if (shown++ >= 10) { p.sendMessage(ChatColor.YELLOW + "  ...and " + (warnings.size() - 10) + " more warning(s), see the server log"); break; } p.sendMessage(ChatColor.YELLOW + "  ! " + w); }
        }
        for (String w : warnings) plugin.getLogger().warning("MapEdit " + s.key + ": " + w);
        if (s.leaveWhenDone) {
            boolean wasHere = p != null && !s.playerGone;
            if (wasHere) restorePlayer(p, s, s.leaveTeleport);
            byPlayer.remove(s.uid);
            if (online && wasHere) p.sendMessage(ChatColor.GRAY + "[Map Edit] You left Map Edit.");
            closeSession(s);
        } else {
            s.phase = Phase.EDITING;
        }
    }

    private List<String> markerWarnings(Session s) {
        List<String> out = new ArrayList<>();
        McmEditSpace sp = s.space;
        int bad = 0;
        for (McmEditSpace.Marker m : sp.markers) {
            int x = sp.originX + m.lx(), y = sp.originY + m.ly(), z = sp.originZ + m.lz();
            if (!sp.world.isChunkLoaded(x >> 4, z >> 4)) continue;
            Block feet = sp.world.getBlockAt(x, y, z);
            String problem = null;
            switch (m.kind()) {
                case FREE -> { if (feet.getType().isSolid() || feet.getRelative(0, 1, 0).getType().isSolid()) problem = "is blocked by a solid block"; }
                case STAND -> {
                    if (feet.getType().isSolid() || feet.getRelative(0, 1, 0).getType().isSolid()) problem = "is blocked by a solid block";
                    else if (!feet.getRelative(0, -1, 0).getType().isSolid()) problem = "has no floor under it";
                }
                case SOLID -> { if (feet.getType().isAir()) problem = "was removed"; }
            }
            if (problem != null) {
                bad++;
                if (bad <= 6) out.add(m.label() + " at " + m.lx() + "," + m.ly() + "," + m.lz() + " " + problem + " (the game needs it)");
            }
        }
        if (bad > 6) out.add("...and " + (bad - 6) + " more game markers are affected");
        return out;
    }

    // ================================================================== session end

    private void closeSession(Session s) {
        if (s.phase == Phase.CLOSED) return;
        s.phase = Phase.CLOSED;
        if (s.buildTimeout != null) s.buildTimeout.cancel();
        if (s.bar != null) { s.bar.removeAll(); s.bar = null; }
        Player p = Bukkit.getPlayer(s.uid);
        if (byPlayer.get(s.uid) == s) byPlayer.remove(s.uid);
        if (byMap.get(s.key) == s) byMap.remove(s.key);
        if (s.scan != null) {
            for (long k : s.tickets) {
                try { s.scan.world.removePluginChunkTicket(RegionScan.ckx(k), RegionScan.ckz(k), plugin); } catch (Throwable ignored) { }
            }
            s.tickets.clear();
            s.scan.baseline.clear();
        }
        if (s.space != null) {
            try { s.game.editClosed(s.mapId, s.space); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "editClosed", t); }
        }
        if (p != null && s.saved != null && !s.playerGone) restorePlayer(p, s, true);
    }

    // ================================================================== overlay files

    private static File backupDirOf(File overlayFile) { return new File(overlayFile.getAbsoluteFile().getParentFile(), "backups"); }

    private void backup(File f, String mapId) throws IOException {
        File dir = backupDirOf(f);
        dir.mkdirs();
        String base = McmMapOverlay.safeId(mapId);
        String stamp = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new Date());
        Files.copy(f.toPath(), new File(dir, base + "." + stamp + ".overlay.gz").toPath(), StandardCopyOption.REPLACE_EXISTING);
        File[] all = dir.listFiles((d, n) -> n.startsWith(base + ".") && n.substring(base.length() + 1).matches("\\d{8}-\\d{6}-\\d{3}\\.overlay\\.gz"));
        if (all != null && all.length > 25) {
            Arrays.sort(all);
            for (int i = 0; i < all.length - 25; i++) all[i].delete();
        }
    }

    private String[] resolve(CommandSender sender, String[] args, int at) {
        if (args.length < at + 2) { sender.sendMessage(ChatColor.YELLOW + "Usage: /mcm mapedit " + args[1] + " <game> <map>"); return null; }
        String g = args[at].toLowerCase(Locale.ROOT);
        if (McmApi.mapEditable(g) == null) { sender.sendMessage(ChatColor.RED + "Unknown game '" + args[at] + "'. Editable: " + String.join(", ", McmApi.mapEditableGames())); return null; }
        String map = null;
        for (String m : maps(g)) if (m.equalsIgnoreCase(args[at + 1])) map = m;
        if (map == null) { sender.sendMessage(ChatColor.RED + "Unknown map '" + args[at + 1] + "'. Maps: " + String.join(", ", maps(g))); return null; }
        return new String[]{g, map};
    }

    private void resetCmd(CommandSender sender, String[] args) {
        String[] gm = resolve(sender, args, 2);
        if (gm == null) return;
        String key = gm[0] + "/" + gm[1];
        if (byMap.containsKey(key)) { sender.sendMessage(ChatColor.RED + key + " is being edited by " + byMap.get(key).name + "; finish or discard first."); return; }
        Plugin owner = McmApi.mapEditableOwner(gm[0]);
        File f = McmMapOverlay.file(owner, gm[1]);
        if (!f.isFile()) { sender.sendMessage(ChatColor.YELLOW + key + " has no saved edits."); return; }
        try {
            McmMapOverlay old = McmMapOverlay.load(f);
            backup(f, gm[1]);
            Files.deleteIfExists(f.toPath());
            McmMapOverlay.invalidate(f);
            try { McmApi.mapEditable(gm[0]).overlayChanged(gm[1]); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "overlayChanged", t); }
            plugin.getLogger().info("MapEdit: " + sender.getName() + " reset " + key + " (dropped " + (old == null ? "?" : old.size()) + " edited blocks, backup kept)");
            sender.sendMessage(ChatColor.GREEN + "Dropped the saved edits of " + key + " (" + (old == null ? "?" : old.size()) + " blocks). A backup was kept: /mcm mapedit restore " + gm[0] + " " + gm[1]);
        } catch (IOException ex) {
            sender.sendMessage(ChatColor.RED + "Reset failed: " + ex);
        }
    }

    private File[] backupsOf(String game, String map) {
        Plugin owner = McmApi.mapEditableOwner(game);
        if (owner == null) return new File[0];
        String base = McmMapOverlay.safeId(map);
        File[] all = backupDirOf(McmMapOverlay.file(owner, map)).listFiles((d, n) -> n.startsWith(base + ".") && n.substring(base.length() + 1).matches("\\d{8}-\\d{6}-\\d{3}\\.overlay\\.gz"));
        if (all == null) return new File[0];
        Arrays.sort(all, (a, b) -> b.getName().compareTo(a.getName()));
        return all;
    }

    private void backupsCmd(CommandSender sender, String[] args) {
        String[] gm = resolve(sender, args, 2);
        if (gm == null) return;
        File[] b = backupsOf(gm[0], gm[1]);
        sender.sendMessage(ChatColor.GOLD + "Backups of " + gm[0] + "/" + gm[1] + " (newest first): " + ChatColor.WHITE + (b.length == 0 ? "none" : ""));
        for (int i = 0; i < b.length && i < 15; i++) {
            McmMapOverlay o = McmMapOverlay.load(b[i]);
            sender.sendMessage(ChatColor.GRAY + " " + (i + 1) + ") " + b[i].getName() + (o == null ? " (unreadable)" : " - " + o.size() + " blocks, " + o.editor));
        }
    }

    private void restoreCmd(CommandSender sender, String[] args) {
        String[] gm = resolve(sender, args, 2);
        if (gm == null) return;
        String key = gm[0] + "/" + gm[1];
        if (byMap.containsKey(key)) { sender.sendMessage(ChatColor.RED + key + " is being edited; finish first."); return; }
        File[] b = backupsOf(gm[0], gm[1]);
        int n = 1;
        if (args.length > 4) { try { n = Integer.parseInt(args[4]); } catch (NumberFormatException e) { n = 1; } }
        if (b.length == 0 || n < 1 || n > b.length) { sender.sendMessage(ChatColor.RED + "No such backup (see /mcm mapedit backups " + gm[0] + " " + gm[1] + ")."); return; }
        Plugin owner = McmApi.mapEditableOwner(gm[0]);
        File f = McmMapOverlay.file(owner, gm[1]);
        try {
            if (f.isFile()) backup(f, gm[1]);
            f.getParentFile().mkdirs();
            Files.copy(b[n - 1].toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
            McmMapOverlay.invalidate(f);
            try { McmApi.mapEditable(gm[0]).overlayChanged(gm[1]); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "overlayChanged", t); }
            plugin.getLogger().info("MapEdit: " + sender.getName() + " restored " + key + " from " + b[n - 1].getName());
            sender.sendMessage(ChatColor.GREEN + "Restored " + key + " from backup " + n + " (the previous overlay was backed up too).");
        } catch (IOException ex) {
            sender.sendMessage(ChatColor.RED + "Restore failed: " + ex);
        }
    }
}
