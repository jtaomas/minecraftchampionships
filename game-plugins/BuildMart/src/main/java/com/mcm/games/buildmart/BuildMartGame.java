package com.mcm.games.buildmart;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;

import java.io.File;
import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

/**
 * Big Sales at Build Mart, Classic Mall version (MCC 3 to 13).
 *
 * Old rules (MCC 13 tutorial, bm-1/frames/021550..021640): 12 minutes; each team has a private build zone with templates
 * of THREE builds to copy on the neighbouring plots; as you complete builds new ones spawn; run or ride your trolley through
 * Build Mart to collect resources and bring them back through the portal; falling in a hole loses everything you carry.
 * Scoring (021640): each build is worth 30 coins per player to begin with and 2 less every time a team completes it; at the
 * very end the % of your unfinished builds you filled in is paid too. These are the real MCC coins (config coins.*,
 * game-plugins/COINS.md); the core applies the event multiplier.
 */
public final class BuildMartGame implements McmGame, Listener, CommandExecutor {
    static final String WORLD = "mcc_buildmart";
    static final String ENT_TAG = "bm_ent";

    // ---------------------------------------------------------------- config
    private int gameSeconds, pregameSeconds, regrowTicks, coinBase, coinDecay, coinMin;
    private boolean bookEnabled, fast;

    // ---------------------------------------------------------------- state
    final JavaPlugin plugin;
    final NamespacedKey kitKey, bookKey;
    private final Random random = new Random();
    private List<BuildDef> builds = new ArrayList<>();
    private World world;
    private MallMap map;
    final Trolley trolley;

    private McmGameContext ctx;
    private boolean running, playing;
    private BukkitTask task;
    private long tick;
    private int secondsLeft, pregameLeft;
    private final List<Entity> entities = new ArrayList<>();
    private final Set<Long> forced = new HashSet<>();
    private final Map<Long, Long> regrow = new HashMap<>();
    private final Map<String, Team> teams = new LinkedHashMap<>();
    private final Map<UUID, String> sideOf = new HashMap<>();
    private final List<BuildDef> deck = new ArrayList<>();
    private final Map<String, Integer> completions = new HashMap<>();
    private final Set<Long> dirty = new HashSet<>();      // room*3+slot
    private final Map<UUID, Long> portalCd = new HashMap<>();
    private final Map<UUID, Long> counterCd = new HashMap<>();

    static final class Team {
        final String side; final int room;
        final BuildDef[] slot = new BuildDef[3];
        final TextDisplay[] holo = new TextDisplay[3];
        int next, built, coins, points;
        Team(String side, int room) { this.side = side; this.room = room; }
    }

    record Progress(int correct, int total, int wrong) {
        boolean complete() { return total > 0 && correct == total && wrong == 0; }
    }

    public BuildMartGame(JavaPlugin plugin) {
        this.plugin = plugin;
        kitKey = new NamespacedKey(plugin, "kit");
        bookKey = new NamespacedKey(plugin, "book");
        trolley = new Trolley(plugin, this);
        loadConfig();
    }

    void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        gameSeconds = c.getInt("game-seconds", 720);
        pregameSeconds = c.getInt("pregame-seconds", 20);
        regrowTicks = (int) Math.round(c.getDouble("regrow-seconds", 3) * 20);
        coinBase = c.getInt("coins.per-build", 30);
        coinDecay = c.getInt("coins.decay-per-completion", 2);
        coinMin = c.getInt("coins.min-per-build", 10);
        bookEnabled = c.getBoolean("shopping-list-book", true);
    }

    @Override public String id() { return "buildmart"; }
    @Override public String displayName() { return "Build Mart"; }
    @Override public Material icon() { return Material.CHEST_MINECART; }   // owner: a new icon - the shopping cart
    @Override public String description() { return "Big Sales at Build Mart (Classic Mall): shop with your trolley, copy the builds."; }
    @Override public List<String> maps() { return List.of("classic"); }
    @Override public boolean forceNextMap(String id) { return "classic".equalsIgnoreCase(id); }
    @Override public String currentMap() { return "classic"; }

    // ================================================================= world

    void enable() {
        builds = BuildDef.loadAll(pluginFile(), plugin.getLogger());
        ensureWorld();
    }

    private File pluginFile() {
        try {
            var m = JavaPlugin.class.getDeclaredMethod("getFile");
            m.setAccessible(true);
            return (File) m.invoke(plugin);
        } catch (Exception e) { throw new IllegalStateException(e); }
    }

    World ensureWorld() {
        World w = Bukkit.getWorld(WORLD);
        if (w == null) {
            w = new WorldCreator(WORLD).generator(new VoidGenerator()).environment(World.Environment.NORMAL)
                    .generateStructures(false).createWorld();
        }
        if (w == null) return null;
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.DO_TILE_DROPS, false);
        w.setGameRule(GameRule.MOB_GRIEFING, false);
        w.setTime(6000);
        w.setStorm(false);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setSpawnLocation(0, MallMap.FY + 1, 30);
        world = w;
        map = new MallMap(w);
        NamespacedKey vk = new NamespacedKey(plugin, "mall_version");
        Integer v = w.getPersistentDataContainer().get(vk, PersistentDataType.INTEGER);
        if (v == null || v != MallMap.VERSION) {
            long t0 = System.currentTimeMillis();
            long n = map.generate();
            w.getPersistentDataContainer().set(vk, PersistentDataType.INTEGER, MallMap.VERSION);
            w.save();
            plugin.getLogger().info("Generated the Classic Mall v" + MallMap.VERSION + ": " + n + " blocks in "
                    + (System.currentTimeMillis() - t0) + " ms");
        } else {
            map.index();
        }
        return w;
    }

    // ================================================================= lifecycle

    @Override
    public void start(McmGameContext context) {
        stop();
        this.ctx = context;
        try {
            if (ensureWorld() == null) throw new IllegalStateException("world " + WORLD + " unavailable");
            if (builds.size() < 4) throw new IllegalStateException("only " + builds.size() + " builds loaded");
            tick = 0;
            secondsLeft = fast ? Math.min(gameSeconds, 240) : gameSeconds;
            pregameLeft = fast ? 3 : pregameSeconds;
            for (long k : map.chunks(MallMap.ROOMS)) {
                int cx = (int) (k >> 32), cz = (int) k;
                world.setChunkForceLoaded(cx, cz, true);
                forced.add(k);
            }
            resetStock();
            clearAllRooms();
            spawnLabels();
            deck.clear();
            deck.addAll(builds);
            Collections.shuffle(deck, random);
            completions.clear();

            int i = 0;
            for (var e : context.sides().entrySet()) {
                if (e.getValue() == null || e.getValue().isEmpty()) continue;
                if (i >= MallMap.ROOMS) {
                    plugin.getLogger().warning("Build Mart has " + MallMap.ROOMS + " build zones; " + e.getKey() + " spectates");
                    continue;
                }
                Team t = new Team(e.getKey(), i++);
                teams.put(t.side, t);
                for (Player p : e.getValue()) sideOf.put(p.getUniqueId(), t.side);
            }
            for (Team t : teams.values()) {
                for (int s = 0; s < 3; s++) setSlot(t, s, nextBuild(t));
            }
            running = true;
            playing = false;
            for (Player p : new ArrayList<>(context.participants())) {
                Team t = teamOf(p);
                if (t == null) { p.setGameMode(GameMode.SPECTATOR); p.teleport(arrive()); continue; }
                preparePlayer(p);
                p.teleport(roomSpawn(t));
            }
            tutorial();
            context.setTimer(secondsLeft, secondsLeft);
            updateSidebar();
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickLoop, 1L, 1L);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Build Mart failed to start", t);
            McmGameContext c = ctx;
            stop();
            if (c != null) Bukkit.getScheduler().runTask(plugin, () -> c.finish("Build Mart failed to start"));
        }
    }

    @Override
    public void stop() {
        running = false;
        playing = false;
        if (task != null) { task.cancel(); task = null; }
        resetPlayers();
        cleanup();
        ctx = null;
    }

    @Override
    public boolean skipWait() {
        if (running && !playing && pregameLeft > 0) { pregameLeft = 1; return true; }
        return false;
    }

    @Override
    public boolean onLateJoin(Player p, McmGameContext c) {
        if (!running) return false;
        String side = null;
        String team = c.teamOf(p.getUniqueId());
        if (team != null && teams.containsKey("TEAM:" + team)) side = "TEAM:" + team;
        if (side == null) return false;
        sideOf.put(p.getUniqueId(), side);
        preparePlayer(p);
        p.teleport(roomSpawn(teams.get(side)));
        return true;
    }

    private void endGame(String reason) {
        if (!running || ctx == null) return;
        McmGameContext c = ctx;
        running = false;
        playing = false;
        if (task != null) { task.cancel(); task = null; }
        try { payPartials(c); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "partial pay failed", t); }
        resetPlayers();
        ctx = null;
        try { c.finish(reason); } finally { cleanup(); }
    }

    private void resetPlayers() {
        trolley.removeAll();
        for (UUID id : new ArrayList<>(sideOf.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            if (p.getVehicle() != null) p.leaveVehicle();
            p.getInventory().clear();
            p.setItemOnCursor(null);
            p.setAllowFlight(false);
            p.setFlying(false);
        }
    }

    private void cleanup() {
        for (Entity e : entities) if (e != null && e.isValid()) e.remove();
        entities.clear();
        if (world != null) {
            for (Entity e : world.getEntities()) {
                if (e.getScoreboardTags().contains(ENT_TAG) || e instanceof Item || (e instanceof Boat && !(e.getPassengers().stream().anyMatch(x -> x instanceof Player))))
                    e.remove();
            }
            if (map != null) { clearAllRooms(); resetStock(); }
            for (long k : forced) world.setChunkForceLoaded((int) (k >> 32), (int) k, false);
        }
        forced.clear();
        teams.clear();
        sideOf.clear();
        regrow.clear();
        dirty.clear();
        portalCd.clear();
    }

    // ================================================================= players

    private Location roomSpawn(Team t) {
        double[] s = MallMap.roomSpawn(t.room);
        return new Location(world, s[0], s[1], s[2], (float) s[3], 0);
    }

    private Location arrive() {
        double[] a = MallMap.MALL_ARRIVE;
        return new Location(world, a[0], a[1], a[2], (float) a[3], 0);
    }

    Team teamOf(Player p) { String s = sideOf.get(p.getUniqueId()); return s == null ? null : teams.get(s); }

    boolean isParticipant(Player p) { return running && sideOf.containsKey(p.getUniqueId()); }

    boolean trolleyAllowed(Player p) {
        return playing && isParticipant(p) && p.getWorld() == world && MallMap.inMall(p.getLocation().getX(), p.getLocation().getZ());
    }

    private ItemStack kit(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.setUnbreakable(true);
        if (m.name().endsWith("_PICKAXE") || m.name().endsWith("_AXE") || m.name().endsWith("_SHOVEL"))
            meta.addEnchant(Enchantment.EFFICIENCY, 4, true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(kitKey, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(meta);
        return it;
    }

    boolean isKit(ItemStack it) {
        return it != null && it.hasItemMeta() && it.getItemMeta().getPersistentDataContainer().has(kitKey, PersistentDataType.BYTE);
    }

    void preparePlayer(Player p) {
        if (p.getVehicle() != null) p.leaveVehicle();
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        p.setItemOnCursor(null);
        p.setHealth(Math.min(20, p.getHealth() <= 0 ? 20 : 20));
        p.setFoodLevel(20);
        p.setSaturation(20);
        p.setFireTicks(0);
        p.setAllowFlight(false);
        p.setFlying(false);
        giveKit(p);
    }

    void giveKit(Player p) {
        // MCC 13 hotbar (cart/frames/ref/11, 12): pickaxe, axe, shovel, shears, Shopping Cart in slot 5
        p.getInventory().setItem(0, kit(Material.DIAMOND_PICKAXE, "Pickaxe"));
        p.getInventory().setItem(1, kit(Material.DIAMOND_AXE, "Axe"));
        p.getInventory().setItem(2, kit(Material.DIAMOND_SHOVEL, "Shovel"));
        p.getInventory().setItem(3, kit(Material.SHEARS, "Shears"));
        p.getInventory().setItem(Trolley.SLOT, trolley.item());
        Team t = teamOf(p);
        if (bookEnabled && t != null) p.getInventory().setItem(8, book(t));
    }

    /** Fell in a hole: lose every carried resource, keep the kit, back to your build zone. */
    private void fall(Player p) {
        Team t = teamOf(p);
        if (t == null) return;
        trolley.reclaim(p, false);
        p.closeInventory();
        p.setItemOnCursor(null);
        int lost = 0;
        ItemStack[] inv = p.getInventory().getContents();
        for (int i = 0; i < inv.length; i++) {
            if (inv[i] != null && !isKit(inv[i])) { lost += inv[i].getAmount(); p.getInventory().setItem(i, null); }
        }
        trolley.giveBack(p);
        p.setFallDistance(0);
        p.setVelocity(new org.bukkit.util.Vector());
        p.teleport(roomSpawn(t));
        broadcast(colorOf(t) + p.getName() + " §fwas thrown out by security!");
        p.showTitle(Title.title(lc("§cYou fell!"), lc("§7You lost the resources you were carrying"),
                Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1600), Duration.ofMillis(400))));
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        plugin.getLogger().info("[bm] " + p.getName() + " fell into a pit, lost " + lost + " items");
    }

    private String colorOf(Team t) {
        if (t.side.startsWith("TEAM:")) return ctx.teamColor(t.side.substring(5)).toString();
        return "§f";
    }

    // ================================================================= loop

    private void tickLoop() {
        if (!running) return;
        tick++;
        if (!dirty.isEmpty()) {
            for (Long k : new ArrayList<>(dirty)) {
                dirty.remove(k);
                Team t = teamByRoom((int) (k / 3));
                if (t != null) check(t, (int) (k % 3));
            }
        }
        if (tick % 2 == 0) {
            for (UUID id : new ArrayList<>(sideOf.keySet())) {
                Player p = Bukkit.getPlayer(id);
                if (p == null || p.getWorld() != world || p.getGameMode() == GameMode.SPECTATOR) continue;
                Location l = p.getLocation();
                if (l.getY() < MallMap.FALL_Y && MallMap.inMall(l.getX(), l.getZ())) { fall(p); continue; }
                if (l.getY() < 0) { p.teleport(roomSpawn(teamOf(p))); continue; }
                if (playing) portal(p);
            }
        }
        if (tick % 4 == 0) blockTitles();
        if (tick % 10 == 0 && !map.leds.isEmpty()) {
            // data-centre status lights: a few rack lamps flicker on and off
            for (int i = 0; i < 6; i++) {
                long[] l = map.leds.get(random.nextInt(map.leds.size()));
                Block lb = world.getBlockAt((int) l[0], (int) l[1], (int) l[2]);
                if (lb.getBlockData() instanceof org.bukkit.block.data.Lightable lt) { lt.setLit(!lt.isLit()); lb.setBlockData(lt, false); }
            }
        }
        if (!regrow.isEmpty()) {
            Iterator<Map.Entry<Long, Long>> it = regrow.entrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                if (e.getValue() > tick) continue;
                it.remove();
                long k = e.getKey();
                BlockData bd = map.stock.get(k);
                Block b = blockAt(k);
                if (bd != null && b.getType().isAir()) b.setBlockData(bd, false);
                else if (bd != null) regrow.put(k, tick + 20);     // someone stands in the spot: try again
            }
        }
        if (tick % 20 == 0) second();
    }

    private void second() {
        if (!playing) {
            pregameLeft--;
            if (pregameLeft <= 5 && pregameLeft > 0) {
                for (Player p : online()) {
                    p.showTitle(Title.title(lc("§b§oStarting in"), lc("§e▸" + pregameLeft + "◂"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                }
            }
            if (pregameLeft <= 0) {
                playing = true;
                for (Player p : online()) {
                    p.showTitle(Title.title(lc("§a§lGO!"), lc("§7Head through the portal into Build Mart"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(300))));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                }
            }
            return;
        }
        secondsLeft--;
        if (ctx != null) ctx.setTimer(Math.max(0, secondsLeft), fast ? 240 : gameSeconds);
        for (Team t : teams.values()) for (int s = 0; s < 3; s++) check(t, s);
        refreshHolos();
        updateSidebar();
        if (secondsLeft == 60 || secondsLeft == 30 || secondsLeft == 10) broadcast("§e" + secondsLeft + " seconds left!");
        if (secondsLeft <= 0) endGame("Time's up");
    }

    private List<Player> online() {
        List<Player> out = new ArrayList<>();
        for (UUID id : sideOf.keySet()) { Player p = Bukkit.getPlayer(id); if (p != null) out.add(p); }
        return out;
    }

    private Team teamByRoom(int room) {
        for (Team t : teams.values()) if (t.room == room) return t;
        return null;
    }

    private void portal(Player p) {
        Block feet = p.getLocation().getBlock();
        if (feet.getType() != Material.NETHER_PORTAL && p.getEyeLocation().getBlock().getType() != Material.NETHER_PORTAL) return;
        Long cd = portalCd.get(p.getUniqueId());
        if (cd != null && cd > tick) return;
        Location l = p.getLocation();
        Team t = teamOf(p);
        if (t == null) return;
        if (MallMap.inMall(l.getX(), l.getZ()) && MallMap.MALL_PORTAL.in(feet.getX(), feet.getZ())) {
            trolley.reclaim(p, true);
            portalCd.put(p.getUniqueId(), tick + 40);
            p.teleport(roomSpawn(t));
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.25f, 1.6f);
        } else if (MallMap.roomAt(l.getX(), l.getZ()) >= 0 && MallMap.roomPortal(MallMap.roomAt(l.getX(), l.getZ())).in(feet.getX(), feet.getZ())) {
            portalCd.put(p.getUniqueId(), tick + 40);
            p.teleport(arrive());
            p.playSound(p.getLocation(), Sound.BLOCK_PORTAL_TRAVEL, 0.25f, 1.6f);
        }
    }

    /** MCC 7 "block titles": the name of the block you look at, in green above the hotbar; your plot progress on your plot. */
    private void blockTitles() {
        for (Player p : online()) {
            if (p.getWorld() != world) continue;
            Team t = teamOf(p);
            String msg = null;
            if (t != null && playing) {
                Location l = p.getLocation();
                for (int s = 0; s < 3; s++) {
                    int px = MallMap.plotX(t.room, s), pz = MallMap.copyZ();
                    if (Math.abs(l.getX() - (px + 0.5)) <= 5.5 && Math.abs(l.getZ() - (pz + 0.5)) <= 5.5 && t.slot[s] != null) {
                        Progress pr = evaluate(t, s);
                        msg = "§f" + t.slot[s].name + " §7• §a" + pr.correct + "/" + pr.total
                                + (pr.wrong > 0 ? " §7• §c" + pr.wrong + " wrong" : "");
                    }
                }
            }
            if (msg == null) {
                RayTraceResult r = p.rayTraceBlocks(6);
                if (r != null && r.getHitBlock() != null) msg = "§a" + Canonical.nice(r.getHitBlock().getType());
            }
            if (msg != null) actionBar(p, msg);
        }
    }

    // ================================================================= plots

    private BuildDef nextBuild(Team t) {
        for (int guard = 0; guard < deck.size(); guard++) {
            BuildDef d = deck.get(t.next++ % deck.size());
            boolean dup = false;
            for (BuildDef a : t.slot) if (a == d) dup = true;
            if (!dup) return d;
        }
        return deck.get(t.next++ % deck.size());
    }

    private void clearVolume(int px, int pz, boolean floorToo) {
        for (int x = px - 3; x <= px + 3; x++)
            for (int z = pz - 3; z <= pz + 3; z++) {
                for (int y = MallMap.PLOT_FLOOR_Y + BuildDef.MAX_H + 2; y > MallMap.PLOT_FLOOR_Y; y--)
                    world.getBlockAt(x, y, z).setType(Material.AIR, false);
                if (floorToo) world.getBlockAt(x, MallMap.PLOT_FLOOR_Y, z).setType(Material.SMOOTH_QUARTZ, false);
            }
    }

    private void placeFloor(int px, int pz, BuildDef d) {
        for (int z = 0; z < 7; z++) for (int x = 0; x < 7; x++)
            world.getBlockAt(px - 3 + x, MallMap.PLOT_FLOOR_Y, pz - 3 + z).setBlockData(d.floor[z][x], false);
    }

    void placeBuild(int px, int pz, BuildDef d, String rotateFirstStair) {
        boolean rotated = false;
        for (int y = 0; y < d.height; y++)
            for (int z = 0; z < 7; z++)
                for (int x = 0; x < 7; x++) {
                    BlockData bd = d.cells[y][z][x];
                    if (bd == null) continue;
                    bd = bd.clone();
                    if (bd instanceof Leaves l) l.setPersistent(true);
                    if (rotateFirstStair != null && !rotated && bd instanceof org.bukkit.block.data.type.Stairs st) {
                        st.setFacing(st.getFacing() == org.bukkit.block.BlockFace.NORTH ? org.bukkit.block.BlockFace.SOUTH
                                : org.bukkit.block.BlockFace.NORTH);
                        rotated = true;
                    }
                    world.getBlockAt(px - 3 + x, MallMap.PLOT_FLOOR_Y + 1 + y, pz - 3 + z).setBlockData(bd, false);
                }
    }

    void setSlot(Team t, int s, BuildDef d) {
        int px = MallMap.plotX(t.room, s);
        clearVolume(px, MallMap.templateZ(), true);
        clearVolume(px, MallMap.copyZ(), true);
        t.slot[s] = d;
        if (d != null) {
            placeFloor(px, MallMap.templateZ(), d);
            placeFloor(px, MallMap.copyZ(), d);
            placeBuild(px, MallMap.templateZ(), d, null);
        }
        if (t.holo[s] == null || !t.holo[s].isValid()) {
            t.holo[s] = world.spawn(new Location(world, px + 0.5, MallMap.FY + 3.2, MallMap.ROOM_Z + 0.5), TextDisplay.class, e -> {
                e.setBillboard(Display.Billboard.CENTER);
                e.setPersistent(false);
                e.setShadowed(true);
                e.addScoreboardTag(ENT_TAG);
            });
            entities.add(t.holo[s]);
        }
        holoText(t, s);
        refreshBooks(t);
    }

    private void holoText(Team t, int s) {
        BuildDef d = t.slot[s];
        String txt = d == null ? "§7Empty plot #" + (s + 1)
                : "§f§l" + d.name + "\n§e" + d.starString() + "\n§6[+" + coinValue(d) + "⛃]\n§7Completions: "
                + completions.getOrDefault(d.id, 0);
        t.holo[s].text(lc(txt));
    }

    private void refreshHolos() {
        for (Team t : teams.values()) for (int s = 0; s < 3; s++) if (t.holo[s] != null && t.holo[s].isValid()) holoText(t, s);
    }

    int coinValue(BuildDef d) { return Math.max(coinMin, coinBase - coinDecay * completions.getOrDefault(d.id, 0)); }

    Progress evaluate(Team t, int s) {
        BuildDef d = t.slot[s];
        if (d == null) return new Progress(0, 0, 0);
        int px = MallMap.plotX(t.room, s) - 3, pz = MallMap.copyZ() - 3;
        int correct = 0, total = 0, wrong = 0;
        for (int y = 0; y < BuildDef.MAX_H; y++)
            for (int z = 0; z < 7; z++)
                for (int x = 0; x < 7; x++) {
                    String want = y < d.height ? d.canon[y][z][x] : Canonical.AIR;
                    String have = Canonical.of(world.getBlockAt(px + x, MallMap.PLOT_FLOOR_Y + 1 + y, pz + z).getBlockData());
                    if (!want.equals(Canonical.AIR)) {
                        total++;
                        if (want.equals(have)) correct++;
                        else if (!have.equals(Canonical.AIR)) wrong++;
                    } else if (!have.equals(Canonical.AIR)) wrong++;
                }
        return new Progress(correct, total, wrong);
    }

    private void check(Team t, int s) {
        if (!playing || t.slot[s] == null) return;
        if (evaluate(t, s).complete()) completeBuild(t, s);
    }

    private void completeBuild(Team t, int s) {
        BuildDef d = t.slot[s];
        int coins = coinValue(d);
        completions.merge(d.id, 1, Integer::sum);
        t.built++;
        t.coins += coins;
        List<Player> members = members(t);
        // MCC (MCC 3-13): the build value is paid to EACH player of the team (30 coins per player at first)
        if (ctx != null && coins > 0) {
            for (Player p : members) ctx.addCoins(p.getUniqueId(), coins, "Build Mart: " + d.name);
            t.points += coins * members.size();
        }
        for (Player p : members) {
            p.sendMessage(lc("§fYour team completed §e" + d.name + " §7(" + d.starString() + ")"));
            p.showTitle(Title.title(lc("§a§lBUILD COMPLETED!"), lc("§f" + d.name),
                    Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1500), Duration.ofMillis(300))));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f);
        }
        broadcast(ctx == null ? "" : ctx.sideLabel(t.side) + " §fcompleted §e" + d.name + "§f! §7(" + completions.get(d.id) + "x)");
        plugin.getLogger().info("[bm] " + t.side + " completed " + d.id + " slot " + (s + 1) + " for " + coins + " coins ("
                + coins * members.size() + " team coins)");
        int px = MallMap.plotX(t.room, s), pz = MallMap.copyZ();
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, px + 0.5, MallMap.FY + 5, pz + 0.5, 80, 2, 2, 2, 0.4);
        setSlot(t, s, nextBuild(t));
        dirty.remove((long) t.room * 3 + s);
    }

    private void payPartials(McmGameContext c) {
        for (Team t : teams.values()) {
            List<Player> members = members(t);
            int coins = 0;
            for (int s = 0; s < 3; s++) {
                if (t.slot[s] == null) continue;
                Progress pr = evaluate(t, s);
                if (pr.total == 0) continue;
                coins += (int) Math.floor((double) pr.correct / pr.total * coinValue(t.slot[s]));
            }
            if (coins > 0) {
                for (Player p : members) c.addCoins(p.getUniqueId(), coins, "Build Mart: unfinished builds filled in");
                t.points += coins * members.size();
            }
            t.coins += coins;
        }
        List<Team> order = new ArrayList<>(teams.values());
        order.sort((a, b) -> b.built != a.built ? b.built - a.built : b.coins - a.coins);
        c.broadcast("§6§lBuild Mart §7- builds completed:");
        for (Team t : order) c.broadcast("  " + c.sideLabel(t.side) + "§7: §f" + t.built + " §7(" + t.coins + "⛃)");
    }

    private List<Player> members(Team t) {
        List<Player> out = new ArrayList<>();
        for (var e : sideOf.entrySet()) if (e.getValue().equals(t.side)) { Player p = Bukkit.getPlayer(e.getKey()); if (p != null) out.add(p); }
        return out;
    }

    /** room, slot of a copy plot build volume containing the block, or null. */
    private int[] copyPlotAt(Block b) {
        int y = b.getY();
        if (y <= MallMap.PLOT_FLOOR_Y || y > MallMap.PLOT_FLOOR_Y + BuildDef.MAX_H) return null;
        int room = MallMap.roomAt(b.getX(), b.getZ());
        if (room < 0) return null;
        if (Math.abs(b.getZ() - MallMap.copyZ()) > 3) return null;
        for (int s = 0; s < 3; s++) if (Math.abs(b.getX() - MallMap.plotX(room, s)) <= 3) return new int[]{room, s};
        return null;
    }

    private boolean ownPlot(Player p, Block b) {
        Team t = teamOf(p);
        int[] at = copyPlotAt(b);
        return t != null && at != null && at[0] == t.room && t.slot[at[1]] != null;
    }

    // ================================================================= stock

    private Block blockAt(long k) {
        int x = (int) (k >> 38), z = (int) ((k << 26) >> 38), y = (int) (k & 0xFFF);
        if (y >= 2048) y -= 4096;
        return world.getBlockAt(x, y, z);
    }

    private void resetStock() {
        for (var e : map.stock.entrySet()) {
            Block b = blockAt(e.getKey());
            if (!b.getBlockData().equals(e.getValue())) b.setBlockData(e.getValue(), false);
        }
        regrow.clear();
    }

    private void clearAllRooms() {
        for (int r = 0; r < MallMap.ROOMS; r++) {
            for (int s = 0; s < 3; s++) {
                clearVolume(MallMap.plotX(r, s), MallMap.templateZ(), true);
                clearVolume(MallMap.plotX(r, s), MallMap.copyZ(), true);
                Block c = world.getBlockAt(MallMap.plotX(r, s) - 6, MallMap.FY + 1, MallMap.copyZ() + 6);
                if (c.getState() instanceof Container ct) ct.getInventory().clear();
            }
            MallMap.Box b = MallMap.roomBox(r);
            for (int z = b.z1(); z <= b.z2(); z++) {
                Block f = world.getBlockAt(b.x2() - 1, MallMap.FY + 1, z);
                if (f.getState() instanceof Container ct) ct.getInventory().clear();
            }
        }
    }

    private void spawnLabels() {
        for (MallMap.Label l : map.labels) {
            TextDisplay td = world.spawn(new Location(world, l.x(), l.y(), l.z()), TextDisplay.class, e -> {
                e.setBillboard(Display.Billboard.CENTER);
                e.setPersistent(false);
                e.addScoreboardTag(ENT_TAG);
                e.text(lc(l.text()));
                e.setTransformation(new org.bukkit.util.Transformation(new org.joml.Vector3f(), new org.joml.AxisAngle4f(),
                        new org.joml.Vector3f(l.scale(), l.scale(), l.scale()), new org.joml.AxisAngle4f()));
                if (l.text().startsWith("§0")) e.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            });
            entities.add(td);
        }
    }

    // ================================================================= book

    private ItemStack book(Team t) {
        ItemStack it = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta m = (BookMeta) it.getItemMeta();
        m.setTitle("Shopping List");
        m.setAuthor("Build Mart");
        m.displayName(lc("§eShopping List").decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        List<Component> pages = new ArrayList<>();
        for (int s = 0; s < 3; s++) {
            BuildDef d = t.slot[s];
            if (d == null) continue;
            List<String> lines = new ArrayList<>();
            for (var e : d.materials.entrySet()) lines.add("§0" + Canonical.nice(e.getKey()) + " §8" + e.getValue() + "x");
            int i = 0;
            boolean first = true;
            do {
                StringBuilder sb = new StringBuilder();
                if (first) sb.append("§l").append(d.name).append("§r\n§6").append(d.starString()).append("\n§0Materials:\n");
                int max = first ? 9 : 12;
                for (int k = 0; k < max && i < lines.size(); k++) sb.append(lines.get(i++)).append('\n');
                pages.add(lc(sb.toString()));
                first = false;
            } while (i < lines.size());
        }
        if (pages.isEmpty()) pages.add(lc("§7Your build zone has no builds right now."));
        m.pages(pages);
        m.getPersistentDataContainer().set(bookKey, PersistentDataType.BYTE, (byte) 1);
        m.getPersistentDataContainer().set(kitKey, PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(m);
        return it;
    }

    private void refreshBooks(Team t) {
        if (!bookEnabled || !running) return;
        for (Player p : members(t)) {
            ItemStack[] inv = p.getInventory().getContents();
            int at = 8;
            for (int i = 0; i < inv.length; i++)
                if (inv[i] != null && inv[i].hasItemMeta() && inv[i].getItemMeta().getPersistentDataContainer().has(bookKey)) {
                    at = i; p.getInventory().setItem(i, null);
                }
            if (p.getInventory().getItem(at) == null) p.getInventory().setItem(at, book(t));
            else p.getInventory().addItem(book(t));
        }
    }

    // ================================================================= HUD

    private void updateSidebar() {
        if (ctx == null) return;
        List<String> lines = new ArrayList<>();
        lines.add("§aBuilds completed:");
        List<Team> order = new ArrayList<>(teams.values());
        order.sort((a, b) -> b.built - a.built);
        for (int i = 0; i < Math.min(5, order.size()); i++)
            lines.add(" " + ctx.sideLabel(order.get(i).side) + "§7: §f" + order.get(i).built);
        ctx.setSidebarLines(lines);
    }

    void actionBar(Player p, String s) { p.sendActionBar(lc(s)); }

    private void broadcast(String s) {
        if (ctx != null) ctx.broadcast(s); else Bukkit.broadcast(lc(s));
    }

    static Component lc(String s) { return LegacyComponentSerializer.legacySection().deserialize(s); }

    private void tutorial() {
        // MCC 13 how-to-play card, word for word (bm-1/frames/021550, 021610, 021620)
        broadcast("§e§lWelcome to Big Sales at Build Mart!");
        broadcast("§fBuild Mart is a speed-building challenge! You have " + (secondsLeft / 60) + " minutes to run out into Build Mart, "
                + "collect resources, bring them back to your plot, and build as many monuments as you can!");
        broadcast("§fEach team has their own private build zone. You'll have templates of §lthree§f different builds "
                + "that you must copy in the neighbouring build plots. As you complete builds, new ones will spawn.");
        broadcast("§fUse your trolley to make traversing Build Mart faster. "
                + "§c- But be careful you don't fall in any holes, as you'll lose all the resources you're carrying.");
        broadcast("§7Extra tips: use the provided furnaces and crafting tables to create anything you can't find around Build Mart.");
    }

    // ================================================================= events

    private boolean ours(World w) { return running && w == world; }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld())) return;
        if (!isParticipant(p)) { if (p.getGameMode() != GameMode.CREATIVE) e.setCancelled(true); return; }
        e.setCancelled(true);
        if (!playing) return;
        Block b = e.getBlock();
        long k = MallMap.key(b.getX(), b.getY(), b.getZ());
        BlockData st = map.stock.get(k);
        if (st != null) {
            ItemStack drop = new ItemStack(BuildDef.itemOf(st));
            if (!p.getInventory().addItem(drop).isEmpty()) { actionBar(p, "§cYour inventory is full!"); return; }
            b.setType(Material.AIR, false);
            regrow.put(k, tick + regrowTicks);
            p.playSound(b.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.4f, 1.4f);
            return;
        }
        if (ownPlot(p, b)) {
            BlockData bd = b.getBlockData();
            Material item = BuildDef.itemOf(bd);
            boolean giveBack = bd.getMaterial() != Material.NETHER_PORTAL && bd.getMaterial() != Material.WATER
                    && bd.getMaterial() != Material.LAVA && item != Material.BARRIER;
            b.setType(Material.AIR, true);
            if (giveBack) p.getInventory().addItem(new ItemStack(item));
            int[] at = copyPlotAt(b);
            dirty.add((long) at[0] * 3 + at[1]);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld())) return;
        if (!isParticipant(p)) { if (p.getGameMode() != GameMode.CREATIVE) e.setCancelled(true); return; }
        Block b = e.getBlockPlaced();
        if (!playing || !ownPlot(p, b)) {
            e.setCancelled(true);
            if (playing) actionBar(p, "§cYou can only build on your team's build plots!");
            return;
        }
        int[] at = copyPlotAt(b);
        dirty.add((long) at[0] * 3 + at[1]);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucket(PlayerBucketEmptyEvent e) {
        if (!ours(e.getPlayer().getWorld())) return;
        Block b = e.getBlockClicked().getRelative(e.getBlockFace());
        if (!playing || !ownPlot(e.getPlayer(), b)) { e.setCancelled(true); return; }
        int[] at = copyPlotAt(b);
        dirty.add((long) at[0] * 3 + at[1]);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (!ours(e.getPlayer().getWorld())) return;
        if (!playing || !ownPlot(e.getPlayer(), e.getBlockClicked())) e.setCancelled(true);
        else { int[] at = copyPlotAt(e.getBlockClicked()); dirty.add((long) at[0] * 3 + at[1]); }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) {
        if (!ours(e.getBlock().getWorld())) return;
        if (e.getPlayer() == null || copyPlotAt(e.getBlock()) == null) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent e) {
        if (!ours(e.getWorld())) return;
        for (var st : e.getBlocks()) if (copyPlotAt(st.getBlock()) == null) { e.setCancelled(true); return; }
        for (var st : e.getBlocks()) { int[] at = copyPlotAt(st.getBlock()); dirty.add((long) at[0] * 3 + at[1]); }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) {
        if (e.getBlock().getWorld() != world) return;
        int[] to = copyPlotAt(e.getToBlock());
        if (to == null) { e.setCancelled(true); return; }
        dirty.add((long) to[0] * 3 + to[1]);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) { if (e.getBlock().getWorld() == world) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent e) { if (e.getBlock().getWorld() == world) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) { if (e.getBlock().getWorld() == world) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) { if (e.getBlock().getWorld() == world) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent e) { if (e.getFrom().getWorld() == world) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent e) { if (e.getFrom().getWorld() == world) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && e.getEntity().getWorld() == world && running) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) { if (e.getEntity().getWorld() == world && running) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (ours(e.getPlayer().getWorld()) && isKit(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !ours(p.getWorld())) return;
        if (e.getView().getTopInventory().getType() == InventoryType.CRAFTING) return;
        if (isKit(e.getCurrentItem()) || isKit(e.getCursor())
                || (e.getHotbarButton() >= 0 && isKit(p.getInventory().getItem(e.getHotbarButton())))) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!ours(p.getWorld()) || e.getClickedBlock() == null || e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Block b = e.getClickedBlock();
        for (MallMap.Counter c : map.counters) {
            if (b.getX() == c.x() && b.getY() == c.y() && b.getZ() == c.z()) {
                e.setCancelled(true);
                if (!playing || !isParticipant(p)) return;
                Long cd = counterCd.get(p.getUniqueId());
                if (cd != null && cd > tick) return;
                counterCd.put(p.getUniqueId(), tick + 10);
                if (c.item() != Material.COAL && p.getInventory().contains(c.item())) { actionBar(p, "§eYou already have " + c.name()); return; }
                p.getInventory().addItem(new ItemStack(c.item(), c.item() == Material.COAL ? 8 : 1));
                p.playSound(b.getLocation(), Sound.BLOCK_BARREL_OPEN, 0.6f, 1.2f);
                return;
            }
        }
        if (b.getType() == Material.CHEST && isParticipant(p)) {
            Team t = teamOf(p);
            if (t == null || MallMap.roomAt(b.getX(), b.getZ()) != t.room) e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (running) trolley.reclaim(e.getPlayer(), true);
    }

    // ================================================================= admin command

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] a) {
        if (a.length == 0) {
            say(sender, "/bm status | fast | fill <team> <slot> [rotate] | clear <team> <slot> | assign <team> <build> | "
                    + "list | stock | regen | tp <mall|room> [n] | dump <mall|room|plot> | preview <build> [slot] | kit <player> | reload");
            return true;
        }
        try {
            switch (a[0].toLowerCase(Locale.ROOT)) {
                case "stock" -> {
                    Map<Material, Integer> n = new TreeMap<>();
                    for (BlockData bd : map.stock.values()) n.merge(bd.getMaterial(), 1, Integer::sum);
                    StringBuilder sb = new StringBuilder("stock: " + map.stock.size() + " blocks, " + n.size() + " materials, "
                            + map.labels.size() + " labels, " + map.leds.size() + " blinking lamps:");
                    n.forEach((m, c) -> sb.append(' ').append(m.name().toLowerCase()).append('=').append(c));
                    say(sender, sb.toString());
                    plugin.getLogger().info(sb.toString());
                }
                case "debug" -> { trolley.debug = !trolley.debug; say(sender, "trolley debug " + trolley.debug); }
                case "fast" -> { fast = !fast; say(sender, "Build Mart fast mode " + (fast ? "ON (3 s pregame, 4 min game)" : "OFF")); }
                case "reload" -> { loadConfig(); builds = BuildDef.loadAll(pluginFile(), plugin.getLogger()); say(sender, "reloaded, " + builds.size() + " builds"); }
                case "list" -> {
                    for (BuildDef d : builds) say(sender, d.id + " §7" + d.name + " " + d.starString() + " " + d.blockCount + " blocks " + d.materials.size() + " materials");
                }
                case "status" -> {
                    say(sender, "running=" + running + " playing=" + playing + " secondsLeft=" + secondsLeft + " trolleys=" + trolley.count());
                    for (Team t : teams.values()) {
                        StringBuilder sb = new StringBuilder("team " + t.room + " " + t.side + " built=" + t.built + " coins=" + t.coins + " points=" + t.points + " |");
                        for (int s = 0; s < 3; s++) {
                            BuildDef d = t.slot[s];
                            if (d == null) { sb.append(" [").append(s + 1).append(": empty]"); continue; }
                            Progress pr = evaluate(t, s);
                            sb.append(" [").append(s + 1).append(": ").append(d.id).append(' ').append(pr.correct).append('/').append(pr.total)
                                    .append(" wrong=").append(pr.wrong).append(']');
                        }
                        say(sender, sb.toString());
                    }
                }
                case "fill" -> {
                    Team t = team(sender, a[1]);
                    int s = Integer.parseInt(a[2]) - 1;
                    if (t.slot[s] == null) { say(sender, "slot empty"); return true; }
                    int px = MallMap.plotX(t.room, s);
                    clearVolume(px, MallMap.copyZ(), false);
                    placeBuild(px, MallMap.copyZ(), t.slot[s], a.length > 3 && a[3].equalsIgnoreCase("rotate") ? "rotate" : null);
                    dirty.add((long) t.room * 3 + s);
                    say(sender, "filled team " + t.room + " slot " + (s + 1) + " with " + t.slot[s].id + (a.length > 3 ? " (one stair rotated)" : ""));
                }
                case "clear" -> {
                    Team t = team(sender, a[1]);
                    int s = Integer.parseInt(a[2]) - 1;
                    setSlot(t, s, null);
                    say(sender, "cleared team " + t.room + " slot " + (s + 1));
                }
                case "assign" -> {
                    Team t = team(sender, a[1]);
                    BuildDef d = builds.stream().filter(x -> x.id.equalsIgnoreCase(a[2])).findFirst().orElse(null);
                    if (d == null) { say(sender, "unknown build " + a[2]); return true; }
                    int free = -1;
                    for (int s = 0; s < 3; s++) if (t.slot[s] == null) { free = s; break; }
                    if (free < 0) { say(sender, "REFUSED: all 3 plots of team " + t.room + " are busy - finish a build first"); return true; }
                    setSlot(t, free, d);
                    say(sender, "assigned " + d.id + " to team " + t.room + " slot " + (free + 1));
                }
                case "regen" -> {
                    NamespacedKey vk = new NamespacedKey(plugin, "mall_version");
                    if (world != null) world.getPersistentDataContainer().remove(vk);
                    ensureWorld();
                    say(sender, "regenerated");
                }
                case "tp" -> {
                    if (!(sender instanceof Player p)) { say(sender, "players only"); return true; }
                    if (a[1].equalsIgnoreCase("mall")) p.teleport(arrive());
                    else {
                        double[] s = MallMap.roomSpawn(a.length > 2 ? Integer.parseInt(a[2]) : 0);
                        p.teleport(new Location(world, s[0], s[1], s[2], (float) s[3], 0));
                    }
                }
                case "kit" -> { Player p = Bukkit.getPlayerExact(a[1]); if (p != null) giveKit(p); }
                case "preview" -> {
                    BuildDef d = builds.stream().filter(x -> x.id.equalsIgnoreCase(a[1])).findFirst().orElse(null);
                    if (d == null) { say(sender, "unknown build"); return true; }
                    int s = a.length > 2 ? Integer.parseInt(a[2]) - 1 : 0;
                    int px = MallMap.plotX(MallMap.ROOMS - 1, s);
                    clearVolume(px, MallMap.templateZ(), true);
                    placeFloor(px, MallMap.templateZ(), d);
                    placeBuild(px, MallMap.templateZ(), d, null);
                    say(sender, "preview " + d.id + " on room " + (MallMap.ROOMS - 1) + " template " + (s + 1) + " at " + px + "," + MallMap.templateZ());
                }
                case "fdump" -> {
                    // full-state dump for the preview renderers: bm fdump x1,y1,z1,x2,y2,z2 <name>
                    String[] c = a[1].split(",");
                    int x1 = Integer.parseInt(c[0]), y1 = Integer.parseInt(c[1]), z1 = Integer.parseInt(c[2]);
                    int x2 = Integer.parseInt(c[3]), y2 = Integer.parseInt(c[4]), z2 = Integer.parseInt(c[5]);
                    File dir = new File(plugin.getDataFolder(), "dumps");
                    dir.mkdirs();
                    File out = new File(dir, a[2] + ".mcmdump.gz");
                    try (var w = new java.io.PrintWriter(new java.io.OutputStreamWriter(new java.util.zip.GZIPOutputStream(
                            new java.io.FileOutputStream(out)), java.nio.charset.StandardCharsets.UTF_8))) {
                        w.println("MCMDUMP 1 " + (x2 - x1 + 1) + " " + (y2 - y1 + 1) + " " + (z2 - z1 + 1));
                        for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) for (int z = z1; z <= z2; z++) {
                            BlockData bd = world.getBlockAt(x, y, z).getBlockData();
                            if (!bd.getMaterial().isAir()) w.println((x - x1) + " " + (y - y1) + " " + (z - z1) + " " + bd.getAsString());
                        }
                    }
                    say(sender, "dumped " + out.getAbsolutePath());
                }
                case "dump" -> {
                    File dir = new File(plugin.getDataFolder(), "dumps");
                    dir.mkdirs();
                    String what = a[1].toLowerCase(Locale.ROOT);
                    Location lo, hi;
                    switch (what) {
                        case "mall" -> { lo = new Location(world, -68, 62, -151); hi = new Location(world, 68, 91, 47); }
                        case "room" -> { MallMap.Box b = MallMap.roomBox(a.length > 2 ? Integer.parseInt(a[2]) : 0);
                            lo = new Location(world, b.x1(), 62, b.z1()); hi = new Location(world, b.x2(), 82, b.z2()); }
                        case "plot" -> { int px = MallMap.plotX(MallMap.ROOMS - 1, a.length > 2 ? Integer.parseInt(a[2]) - 1 : 0);
                            lo = new Location(world, px - 7, 63, MallMap.templateZ() - 6); hi = new Location(world, px + 7, 78, MallMap.templateZ() + 9); }
                        default -> {
                            String[] c = a[2].split(",");
                            lo = new Location(world, Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]));
                            hi = new Location(world, Integer.parseInt(c[3]), Integer.parseInt(c[4]), Integer.parseInt(c[5]));
                        }
                    }
                    File out = new File(dir, what + (a.length > 2 && !what.equals("mall") ? "_" + a[2].replace(',', '_') : "") + ".mcmdump.gz");
                    com.mcm.api.McmBuild.dumpRegion(world, lo, hi, out, r -> say(sender, "dumped " + out.getAbsolutePath()));
                }
                default -> say(sender, "unknown subcommand");
            }
        } catch (Exception ex) {
            say(sender, "error: " + ex);
        }
        return true;
    }

    private Team team(CommandSender sender, String arg) {
        Player p = Bukkit.getPlayerExact(arg);
        if (p != null && teamOf(p) != null) return teamOf(p);
        int room = Integer.parseInt(arg);
        Team t = teamByRoom(room);
        if (t == null) throw new IllegalArgumentException("no team in room " + room);
        return t;
    }

    private void say(CommandSender s, String msg) {
        s.sendMessage(lc("§6[BM] §f" + msg));
        if (!(s instanceof org.bukkit.command.ConsoleCommandSender)) plugin.getLogger().info("[bm] " + msg.replaceAll("§.", ""));
    }
}
