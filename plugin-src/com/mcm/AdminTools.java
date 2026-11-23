package com.mcm;

import com.mcm.api.McmApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

/**
 * The ops-only "Admin Tools" feather and its chest menus (fast test, skip wait, skip round, maps, shortcuts).
 * The feather is identified only by a PersistentDataContainer tag, never by name. A light repeating task keeps it
 * in inventory slot 17 (top-right) and in hotbar slot 8 when those slots are empty, for online ops only, so it is
 * reachable during games even after they clear inventories; it never overwrites a game item. Everyone else (and
 * de-opped players) have it removed. Every button runs the same code path as the matching /mcm command.
 */
final class AdminTools implements Listener {
    static final int HOTBAR_SLOT = 8;   // lobby uses 0 (team wool) and 4 (compass)
    static final int INV_SLOT = 17;     // top-right of the main inventory
    private static NamespacedKey KEY;

    private final MinecraftMondayPlugin plugin;
    private BukkitTask syncTask;

    AdminTools(MinecraftMondayPlugin plugin) {
        this.plugin = plugin;
        KEY = new NamespacedKey(plugin, "admin_tool");
    }

    void enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        syncTask = Bukkit.getScheduler().runTaskTimer(plugin, this::syncAll, 20L, 10L);
    }

    void disable() {
        if (syncTask != null) { syncTask.cancel(); syncTask = null; }
        HandlerList.unregisterAll(this);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menu) p.closeInventory();
        }
    }

    // ------------------------------------------------------------------ item

    static boolean isTool(ItemStack is) {
        if (KEY == null || is == null || is.getType() != Material.FEATHER || !is.hasItemMeta()) return false;
        ItemMeta m = is.getItemMeta();
        return m != null && m.getPersistentDataContainer().has(KEY, PersistentDataType.BYTE);
    }

    private static ItemStack makeTool() {
        ItemStack is = new ItemStack(Material.FEATHER);
        ItemMeta m = is.getItemMeta();
        m.setDisplayName(ChatColor.GOLD + "" + ChatColor.BOLD + "Admin Tools " + ChatColor.GRAY + "(Right-Click)");
        m.setLore(List.of(ChatColor.GRAY + "Fast test, skip wait, skip round, maps,",
                ChatColor.GRAY + "start/stop games, lobbies. Ops only.",
                ChatColor.DARK_GRAY + "Also: click it in your inventory or /mcm tools"));
        m.addItemFlags(ItemFlag.values());
        m.getPersistentDataContainer().set(KEY, PersistentDataType.BYTE, (byte) 1);
        is.setItemMeta(m);
        return is;
    }

    private boolean shouldHave(Player p) { return p.isOp() && plugin.adminToolsItemEnabled(); }

    private void syncAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            try { sync(p); } catch (Throwable ignored) { }
        }
    }

    /**
     * Keep exactly the allowed copies for ops; strip it from everyone else. Lobby and built-in games: slot 17 +
     * hotbar slot 8. During an external (MCC) game: slot 17 only, because the game's own right-click handlers may
     * react to any feather (e.g. Rocket Spleef updrafts); the feather is opened by clicking it in the inventory.
     * During games listed in admin-tools-hidden-in-games (inventory scanners such as Bingo): no copy at all.
     */
    private void sync(Player p) {
        PlayerInventory inv = p.getInventory();
        boolean allowed = shouldHave(p) && !plugin.toolHiddenForRunningGame();
        boolean hotbarAllowed = allowed && !plugin.externalGameRunning();
        ItemStack[] contents = inv.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (!isTool(contents[i])) continue;
            boolean keep = allowed && (i == INV_SLOT || (i == HOTBAR_SLOT && hotbarAllowed));
            if (!keep) inv.setItem(i, null);
            else if (contents[i].getAmount() != 1) contents[i].setAmount(1);
        }
        if (isTool(p.getItemOnCursor()) && !(p.getOpenInventory().getTopInventory().getHolder() instanceof Menu)) {
            p.setItemOnCursor(null);
        }
        if (!allowed) return;
        // only ever fill an EMPTY slot (never overwrite a game item)
        if (isEmpty(inv.getItem(INV_SLOT))) inv.setItem(INV_SLOT, makeTool());
        if (hotbarAllowed && isEmpty(inv.getItem(HOTBAR_SLOT))) inv.setItem(HOTBAR_SLOT, makeTool());
    }

    private static boolean isEmpty(ItemStack is) { return is == null || is.getType() == Material.AIR || is.getAmount() <= 0; }

    // ------------------------------------------------------------------ protection + opening

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (!isTool(e.getItem())) return;
        e.setCancelled(true);
        e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) openLater(e.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        ItemStack hand = e.getPlayer().getInventory().getItem(e.getHand());
        if (!isTool(hand)) return;
        e.setCancelled(true);
        if (e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) openLater(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrop(PlayerDropItemEvent e) {
        if (isTool(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (isTool(e.getMainHandItem()) || isTool(e.getOffHandItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        if (isTool(e.getItemInHand())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(AdminTools::isTool);
        e.getItemsToKeep().removeIf(AdminTools::isTool);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(ItemSpawnEvent e) {
        Item it = e.getEntity();
        if (isTool(it.getItemStack())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) sync(p); });
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDrag(InventoryDragEvent e) {
        if (isTool(e.getOldCursor()) || isTool(e.getCursor())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder() instanceof Menu menu) {
            e.setCancelled(true);
            if (e.getClickedInventory() == top && p.isOp()) {
                int slot = e.getRawSlot();
                Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) menuClick(p, menu, slot); });
            }
            return;
        }
        boolean involves = isTool(e.getCurrentItem()) || isTool(e.getCursor());
        if (!involves && e.getClick() == ClickType.NUMBER_KEY && e.getHotbarButton() >= 0) {
            involves = isTool(p.getInventory().getItem(e.getHotbarButton()));
        }
        if (!involves && (e.getClick() == ClickType.SWAP_OFFHAND || e.getAction() == InventoryAction.HOTBAR_SWAP)) {
            involves = isTool(p.getInventory().getItemInOffHand());
        }
        if (!involves) return;
        e.setCancelled(true);
        if (isTool(e.getCurrentItem()) && e.getClickedInventory() instanceof PlayerInventory && p.isOp()) {
            openLater(p);
        }
    }

    // ------------------------------------------------------------------ menus

    private enum Kind { MAIN, START_PICK, MAP_GAME_PICK, MAPS }

    private static final class Menu implements InventoryHolder {
        final Kind kind;
        final String gameId;
        final List<String> ids = new ArrayList<>();
        Inventory inv;
        Menu(Kind kind, String gameId) { this.kind = kind; this.gameId = gameId; }
        @Override public Inventory getInventory() { return inv; }
    }

    private static final String T_MAIN = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Admin Tools";
    private static final int BACK_SLOT = 49;

    private void openLater(Player p) {
        Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline() && p.isOp()) openMain(p); });
    }

    private static ItemStack button(Material m, String name, String... lore) {
        ItemStack is = new ItemStack(m);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(name);
        List<String> l = new ArrayList<>();
        for (String s : lore) if (s != null && !s.isEmpty()) l.add(s);
        im.setLore(l);
        im.addItemFlags(ItemFlag.values());
        is.setItemMeta(im);
        return is;
    }

    void openMain(Player p) {
        if (!p.isOp()) { p.sendMessage(ChatColor.RED + "Only tournament admins (OPs) can use the Admin Tools."); return; }
        Menu m = new Menu(Kind.MAIN, null);
        Inventory inv = Bukkit.createInventory(m, 27, T_MAIN);
        m.inv = inv;
        boolean fast = McmApi.isFastTest();
        String running = plugin.runningGameId();
        String wait = plugin.pendingWaitDescription();
        inv.setItem(4, button(Material.PAPER, ChatColor.WHITE + "" + ChatColor.BOLD + "Status",
                ChatColor.GRAY + "Game: " + ChatColor.WHITE + (running == null ? "none (lobby)" : McmApi.displayName(running)),
                ChatColor.GRAY + "Waiting on: " + ChatColor.WHITE + (wait == null ? "nothing" : wait),
                ChatColor.GRAY + "Fast test: " + (fast ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF")));
        inv.setItem(10, button(fast ? Material.LIME_DYE : Material.GRAY_DYE,
                ChatColor.AQUA + "" + ChatColor.BOLD + "Fast test: " + (fast ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"),
                ChatColor.GRAY + "Cuts every countdown, vote, intermission and", ChatColor.GRAY + "results wait to 1 s. Saved across restarts.",
                ChatColor.YELLOW + "Click to turn " + (fast ? "OFF" : "ON") + ChatColor.DARK_GRAY + "  (/mcm fast)"));
        inv.setItem(11, button(Material.CLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "Skip wait",
                ChatColor.GRAY + "Finish the countdown / vote / intermission /", ChatColor.GRAY + "results reveal that is running right now.",
                ChatColor.DARK_GRAY + "/mcm skipwait"));
        inv.setItem(12, button(Material.SPECTRAL_ARROW, ChatColor.GOLD + "" + ChatColor.BOLD + "Skip round / map",
                ChatColor.GRAY + "End only the current round or map (no points)", ChatColor.GRAY + "and go on to the next one.",
                ChatColor.DARK_GRAY + "/mcm skip"));
        inv.setItem(13, button(Material.FILLED_MAP, ChatColor.AQUA + "" + ChatColor.BOLD + "Maps",
                ChatColor.GRAY + "Pick the map the next round uses.",
                ChatColor.GRAY + (running == null ? "No game running: pick a game first." : "Game: " + McmApi.displayName(running)),
                ChatColor.DARK_GRAY + "/mcm map"));
        inv.setItem(14, button(Material.ARROW, ChatColor.GOLD + "" + ChatColor.BOLD + "Skip whole game",
                ChatColor.GRAY + "End the entire current game/event and", ChatColor.GRAY + "advance the championship.",
                ChatColor.DARK_GRAY + "/mcm next"));
        inv.setItem(15, button(Material.NETHER_STAR, ChatColor.YELLOW + "" + ChatColor.BOLD + "Start a game",
                ChatColor.GRAY + "Play one game on its own (stops the current one).", ChatColor.DARK_GRAY + "/mcm play <game>"));
        inv.setItem(16, button(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "Stop game",
                ChatColor.GRAY + "Stop everything and return to the lobby.", ChatColor.DARK_GRAY + "/mcm stop"));
        inv.setItem(20, button(Material.GRASS_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "MM lobby",
                ChatColor.GRAY + "Teleport to the Minecraft Monday lobby.", ChatColor.DARK_GRAY + "/mcm lobby mm"));
        inv.setItem(22, button(Material.COMPASS, ChatColor.GOLD + "" + ChatColor.BOLD + "Tournament Controller",
                ChatColor.GRAY + "Open the full /mcm controller (championship, teams, scores)."));
        if (plugin.hasHub()) {
            inv.setItem(24, button(Material.ENDER_EYE, ChatColor.GOLD + "" + ChatColor.BOLD + "MCC lobby",
                    ChatColor.GRAY + "Teleport to the MCC hub island.", ChatColor.DARK_GRAY + "/mcm lobby tp"));
        }
        p.openInventory(inv);
    }

    private void openGamePicker(Player p, Kind kind) {
        Menu m = new Menu(kind, null);
        String title = kind == Kind.START_PICK ? ChatColor.DARK_RED + "Admin Tools " + ChatColor.DARK_GRAY + "- Start a game"
                : ChatColor.DARK_RED + "Admin Tools " + ChatColor.DARK_GRAY + "- Maps: pick a game";
        Inventory inv = Bukkit.createInventory(m, 54, title);
        m.inv = inv;
        int i = 0;
        for (String id : plugin.gameIds()) {
            if (i >= 45) break;
            m.ids.add(id);
            String extra = kind == Kind.MAP_GAME_PICK
                    ? (plugin.mapsOf(id).isEmpty() ? ChatColor.DARK_GRAY + "no selectable maps" : ChatColor.GRAY + "" + plugin.mapsOf(id).size() + " maps")
                    : ChatColor.YELLOW + "Click to play";
            inv.setItem(i++, button(plugin.gameIcon(id), ChatColor.AQUA + "" + ChatColor.BOLD + McmApi.displayName(id),
                    ChatColor.DARK_GRAY + "id: " + id, extra));
        }
        inv.setItem(BACK_SLOT, button(Material.OAK_DOOR, ChatColor.WHITE + "Back"));
        p.openInventory(inv);
    }

    private void openMaps(Player p, String gameId) {
        List<String> maps = plugin.mapsOf(gameId);
        String name = McmApi.displayName(gameId);
        if (maps.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + name + " has no selectable maps.");
        }
        Menu m = new Menu(Kind.MAPS, gameId);
        Inventory inv = Bukkit.createInventory(m, 54, ChatColor.DARK_RED + "Admin Tools " + ChatColor.DARK_GRAY + "- " + ChatColor.stripColor(name));
        m.inv = inv;
        int i = 0;
        for (String id : maps) {
            if (i >= 45) break;
            m.ids.add(id);
            inv.setItem(i++, button(Material.MAP, ChatColor.AQUA + "" + ChatColor.BOLD + id,
                    ChatColor.GRAY + "Next round of " + name + " uses this map.", ChatColor.YELLOW + "Click to select"));
        }
        if (maps.isEmpty()) {
            inv.setItem(22, button(Material.BARRIER, ChatColor.RED + "No selectable maps",
                    ChatColor.GRAY + name + " does not expose any maps."));
        }
        inv.setItem(BACK_SLOT, button(Material.OAK_DOOR, ChatColor.WHITE + "Back"));
        p.openInventory(inv);
    }

    private void menuClick(Player p, Menu m, int slot) {
        if (!p.isOp()) { p.closeInventory(); return; }
        switch (m.kind) {
            case MAIN -> {
                switch (slot) {
                    case 10 -> { plugin.cmdFast(p, null); openMain(p); }
                    case 11 -> { p.closeInventory(); plugin.cmdSkipWait(p); }
                    case 12 -> { p.closeInventory(); plugin.cmdSkipRound(p); }
                    case 13 -> {
                        String running = plugin.runningGameId();
                        if (running != null) openMaps(p, running); else openGamePicker(p, Kind.MAP_GAME_PICK);
                    }
                    case 14 -> { p.closeInventory(); plugin.cmdSkipEvent(p); }
                    case 15 -> openGamePicker(p, Kind.START_PICK);
                    case 16 -> { p.closeInventory(); plugin.cmdStop(p); }
                    case 20 -> plugin.goMm(p);
                    case 22 -> plugin.openController(p);
                    case 24 -> { if (plugin.hasHub()) plugin.goHub(p); }
                    default -> { return; }
                }
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            }
            case START_PICK, MAP_GAME_PICK -> {
                if (slot == BACK_SLOT) { openMain(p); return; }
                if (slot < 0 || slot >= m.ids.size()) return;
                String id = m.ids.get(slot);
                if (m.kind == Kind.START_PICK) { p.closeInventory(); plugin.adminStart(p, id); }
                else openMaps(p, id);
            }
            case MAPS -> {
                if (slot == BACK_SLOT) {
                    if (plugin.runningGameId() != null) openMain(p); else openGamePicker(p, Kind.MAP_GAME_PICK);
                    return;
                }
                if (slot < 0 || slot >= m.ids.size()) return;
                if (plugin.forceMap(p, m.gameId, m.ids.get(slot))) p.closeInventory();
            }
        }
    }

    /** Re-render open main menus (e.g. after /mcm fast). */
    void refreshOpenMenus() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof Menu m && m.kind == Kind.MAIN && p.isOp()) {
                Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) openMain(p); });
            }
        }
    }
}
