package com.mcm.games.railroadrush;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.TextDisplay;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.logging.Level;

/**
 * Railroad Rush: each side builds a railroad through its own canyon course.
 * Gold (from ore and Gold Rushes) buys rails and supplies; TNT carts ride the track and must reach
 * the copper checkpoint doors to blast them open; at the end a golden cart runs the track and its
 * powered reach decides the placement.
 */
public final class RailroadRushGame implements McmGame, Listener {

    static final String WORLD_NAME = "mcc_railroadrush";
    static final int LANE_SPACING = 24;
    static final int PLAY_Y = 64;
    static final int START_Z = 7;
    static final int[] DOORS = {55, 111, 171};
    static final int END_Z = 181;
    static final int GAME_SECONDS = 480;
    /** {start second, duration} */
    static final int[][] RUSHES = {{45, 40}, {200, 35}, {340, 35}};
    static final int CART_START = 24, CART_BOOST = 18, CART_MAX = 48, CART_STEP_TICKS = 5, CART_INTERVAL = 11;
    static final int MINE_X = -120, MINE_Z = 90, MINE_R = 20;
    static final Material WEAK = Material.OAK_PLANKS;
    static final int WEAK_TICKS = 160;
    static final int[] PLACE_POINTS = {40, 32, 27, 23, 19, 16, 13, 10, 8, 6};
    static final int DOOR_POINTS = 15, EMERALD_POINTS = 5;
    static final int MAX_CHAIN = 1500;

    enum Phase { IDLE, BUILDING, COUNTDOWN, PLAYING, FINAL }

    private record WeakBlock(Block block, long at) {}

    private record Offer(int slot, Material mat, int amount, int cost, String name, boolean tutorial, String desc) {}

    private static final List<Offer> OFFERS = List.of(
            new Offer(10, Material.RAIL, 8, 4, "Rails x8", true, "Lay track. Curves and slopes form automatically."),
            new Offer(11, Material.POWERED_RAIL, 1, 3, "Powered Rail", true, "Refills a cart's power gauge (+" + CART_BOOST + ")."),
            new Offer(12, null, 4, 4, "Track Bed x4", false, "Team concrete: permanent bridge blocks."),
            new Offer(13, Material.SAND, 4, 2, "Sand x4", false, "Cheap bridge blocks, but explosions destroy them."),
            new Offer(14, Material.FIRE_CHARGE, 2, 4, "Dynamite x2", false, "Right-click a block: weakens stone, grabs gold."),
            new Offer(15, Material.SHEARS, 1, 4, "Shears", false, "Needed to cut through leaves. Limited uses."));

    private static final class ShopHolder implements InventoryHolder {
        final Lane lane;
        Inventory inv;
        ShopHolder(Lane lane) { this.lane = lane; }
        @Override public Inventory getInventory() { return inv; }
    }

    private static final class GoldRunner {
        final List<Pos> path;
        final BlockDisplay display;
        int idx;
        boolean done;
        GoldRunner(List<Pos> path, BlockDisplay display) { this.path = path; this.display = display; }
    }

    private final RailroadRushPlugin plugin;
    private final NamespacedKey lanesKey;
    private final BlockJobs jobs;
    private final Random rnd = new Random();

    private McmGameContext ctx;
    private World world;
    private Phase phase = Phase.IDLE;
    private boolean running;
    private boolean finished = true;
    private int gen;
    private Layout layout;
    private BlockData railNS, leavesData;

    private final List<Lane> lanes = new ArrayList<>();
    private final Map<UUID, Lane> laneOf = new HashMap<>();
    private final Map<UUID, Lane> traders = new HashMap<>();
    private final Map<UUID, Location> rushReturn = new HashMap<>();
    private final ArrayDeque<WeakBlock> weak = new ArrayDeque<>();
    private final Set<BukkitTask> pending = new HashSet<>();
    private final List<GoldRunner> runners = new ArrayList<>();
    private BukkitTask mainTask;

    private long tick;
    private int sec, countdown, rushEndSec, finalTicks, finalLength, finalWait;
    private boolean inRush;

    RailroadRushGame(RailroadRushPlugin plugin) {
        this.plugin = plugin;
        this.lanesKey = new NamespacedKey(plugin, "lanes_built");
        this.jobs = new BlockJobs(plugin);
    }

    // ------------------------------------------------------------------ McmGame

    @Override public String id() { return "railroadrush"; }
    @Override public String displayName() { return "Railroad Rush"; }
    @Override public Material icon() { return Material.RAIL; }
    @Override public String description() { return "Build the longest railroad: mine gold, buy rails, blast copper doors with TNT carts."; }

    @Override
    public void start(McmGameContext context) {
        if (running) cleanup(false);
        this.ctx = context;
        finished = false;
        running = true;
        gen++;
        tick = 0;
        sec = 0;
        inRush = false;

        world = ensureWorld();
        if (world == null) {
            context.broadcast("§cRailroad Rush could not create its world.");
            finishGame("Railroad Rush world error");
            return;
        }
        for (Entity e : new ArrayList<>(world.getEntities())) if (!(e instanceof Player)) e.remove();

        railNS = Bukkit.createBlockData(Material.RAIL, "[shape=north_south]");
        leavesData = Bukkit.createBlockData(Layout.LEAVES, "[persistent=true]");
        layout = new Layout(new Random(rnd.nextLong()));

        lanes.clear();
        laneOf.clear();
        int i = 0, solo = 0;
        for (Map.Entry<String, List<Player>> en : context.sides().entrySet()) {
            List<Player> ps = en.getValue();
            if (ps == null || ps.isEmpty()) continue;
            String key = en.getKey();
            Material conc;
            if (key.startsWith("TEAM:")) conc = teamConcrete(key.substring(5));
            else conc = SOLO_CONCRETE[solo++ % SOLO_CONCRETE.length];
            int mult = Math.max(1, Math.min(3, (int) Math.ceil(4.0 / ps.size())));
            Lane l = new Lane(key, i, i * LANE_SPACING, conc, mult, context.sideLabel(key));
            l.gold = 8 * mult;
            for (Player p : ps) {
                l.members.add(p.getUniqueId());
                laneOf.put(p.getUniqueId(), l);
            }
            lanes.add(l);
            i++;
        }
        if (lanes.isEmpty()) {
            finishGame("No players for Railroad Rush");
            return;
        }

        phase = Phase.BUILDING;
        for (Player p : players()) p.sendTitle("§6Railroad Rush", "§7Carving the canyon...", 5, 60, 10);

        int hw = getHighWater();
        int count = Math.max(lanes.size(), hw);
        setHighWater(count);
        jobs.cancel();
        for (int li = 0; li < count; li++) {
            final int idx = li;
            final Lane l = li < lanes.size() ? lanes.get(li) : null;
            for (int z = Layout.MIN_Z; z <= Layout.MAX_Z; z++) {
                final int fz = z;
                jobs.add(() -> buildSlice(idx, l, fz));
            }
        }
        jobs.add(this::buildMine);
        final int myGen = gen;
        jobs.start(120, () -> {
            if (myGen != gen || !running) return;
            setHighWater(lanes.size());
            afterBuild();
        });
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 1L, 1L);
    }

    @Override
    public void stop() {
        finished = true;
        if (!running && phase == Phase.IDLE) return;
        cleanup(true);
        ctx = null;
    }

    // ------------------------------------------------------------------ lifecycle

    private void finishGame(String reason) {
        if (finished) return;
        finished = true;
        McmGameContext c = ctx;
        cleanup(true);
        ctx = null;
        if (c != null) c.finish(reason);
    }

    private void cleanup(boolean clearArena) {
        running = false;
        phase = Phase.IDLE;
        inRush = false;
        gen++;
        if (mainTask != null) { mainTask.cancel(); mainTask = null; }
        for (BukkitTask t : new ArrayList<>(pending)) t.cancel();
        pending.clear();
        jobs.cancel();
        if (world != null) {
            for (Entity e : new ArrayList<>(world.getEntities())) {
                if (e instanceof Player p) {
                    if (p.getOpenInventory().getTopInventory().getHolder() instanceof ShopHolder) p.closeInventory();
                    p.getInventory().clear();
                } else {
                    e.remove();
                }
            }
        }
        for (Lane l : lanes) l.carts.clear();
        lanes.clear();
        laneOf.clear();
        traders.clear();
        rushReturn.clear();
        weak.clear();
        runners.clear();
        if (clearArena && world != null && plugin.isEnabled()) queueClear();
    }

    private void queueClear() {
        int hw = getHighWater();
        for (int li = 0; li < hw; li++) {
            final int idx = li;
            for (int z = Layout.MIN_Z; z <= Layout.MAX_Z; z++) {
                final int fz = z;
                jobs.add(() -> buildSlice(idx, null, fz));
            }
        }
        jobs.add(this::clearMine);
        jobs.start(150, () -> setHighWater(0));
    }

    private void afterBuild() {
        phase = Phase.COUNTDOWN;
        countdown = 6;
        for (Lane l : lanes) spawnTrader(l, 4);
        for (Player p : players()) {
            Lane l = laneOf.get(p.getUniqueId());
            if (l == null) continue;
            Location loc = new Location(world, l.ox + 0.5 + rnd.nextInt(5) - 2, PLAY_Y, 2.5, 0f, 0f);
            p.teleport(loc);
            p.setGameMode(GameMode.SURVIVAL);
            heal(p);
            giveKit(p);
        }
        if (ctx != null) {
            ctx.setTimer(GAME_SECONDS, GAME_SECONDS);
            hud();
        }
    }

    private void giveKit(Player p) {
        p.getInventory().clear();
        ItemStack pick = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta pm = pick.getItemMeta();
        pm.setUnbreakable(true);
        pm.displayName(legacy("§fPickaxe"));
        pick.setItemMeta(pm);
        p.getInventory().setItem(0, pick);
        p.getInventory().setItem(1, weakPlanks());
        p.getInventory().setItem(2, new ItemStack(Material.RAIL, 8));
    }

    private ItemStack weakPlanks() {
        ItemStack it = new ItemStack(WEAK, 32);
        ItemMeta m = it.getItemMeta();
        m.displayName(legacy("§7Weak Planks"));
        m.lore(List.of(legacy("§7Unlimited. Crumble after 8 seconds."), legacy("§7Rails can't be placed on them.")));
        it.setItemMeta(m);
        return it;
    }

    private ItemStack dynamite(int amount) {
        ItemStack it = new ItemStack(Material.FIRE_CHARGE, amount);
        ItemMeta m = it.getItemMeta();
        m.displayName(legacy("§cDynamite"));
        m.lore(List.of(legacy("§7Right-click a block to light it."), legacy("§7Weakens stone, clears leaves, grabs gold."), legacy("§7Can't open copper doors.")));
        it.setItemMeta(m);
        return it;
    }

    // ------------------------------------------------------------------ world & arena

    private World ensureWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            w = new WorldCreator(WORLD_NAME).generator(new VoidGenerator()).generateStructures(false).createWorld();
        }
        if (w == null) return null;
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.DO_TILE_DROPS, false);
        w.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        w.setGameRule(GameRule.DO_MOB_LOOT, false);
        w.setGameRule(GameRule.MOB_GRIEFING, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setDifficulty(Difficulty.EASY); // Fool's Gold skeletons need a non-peaceful world
        w.setTime(6000);
        w.setStorm(false);
        w.setThundering(false);
        w.setSpawnLocation(0, PLAY_Y, 2);
        return w;
    }

    private int getHighWater() {
        if (world == null) return 0;
        Integer v = world.getPersistentDataContainer().get(lanesKey, PersistentDataType.INTEGER);
        return v == null ? 0 : Math.max(0, Math.min(200, v));
    }

    private void setHighWater(int n) {
        if (world != null) world.getPersistentDataContainer().set(lanesKey, PersistentDataType.INTEGER, n);
    }

    /** Writes one z-slice of a lane box (lane null = clear to air). */
    private void buildSlice(int index, Lane lane, int z) {
        if (world == null) return;
        int ox = index * LANE_SPACING;
        for (int lx = Layout.MIN_LX; lx <= Layout.MAX_LX; lx++) {
            for (int y = Layout.MIN_Y; y <= Layout.MAX_Y; y++) {
                Block b = world.getBlockAt(ox + lx, y, z);
                if (lane == null || layout == null) {
                    if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                    continue;
                }
                Material m = layout.get(lx, y, z);
                if (m == Layout.PAD) m = lane.concrete;
                if (m == Material.RAIL) b.setBlockData(railNS, false);
                else if (m == Layout.LEAVES) b.setBlockData(leavesData, false);
                else if (b.getType() != m) b.setType(m, false);
            }
        }
    }

    private void buildMine() {
        if (world == null) return;
        Random r = new Random();
        int R = MINE_R;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                boolean wall = Math.abs(x) >= R - 1 || Math.abs(z) >= R - 1;
                for (int y = 58; y <= 70; y++) {
                    Material m;
                    if (y == 70) m = Material.BARRIER;
                    else if (y == 59) m = Math.floorMod(x + z, 2) == 0 ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE;
                    else if (y == 58 || wall) m = Material.DEEPSLATE_BRICKS;
                    else m = Material.AIR;
                    world.getBlockAt(MINE_X + x, y, MINE_Z + z).setType(m, false);
                }
            }
        }
        for (int i = 0; i < 110; i++) {
            int cx = r.nextInt(2 * (R - 3) + 1) - (R - 3);
            int cz = r.nextInt(2 * (R - 3) + 1) - (R - 3);
            int cy = 60 + r.nextInt(4);
            for (int dx = -2; dx <= 2; dx++) for (int dy = -2; dy <= 2; dy++) for (int dz = -2; dz <= 2; dz++) {
                if (dx * dx + dy * dy + dz * dz > 3) continue;
                int x = cx + dx, y = cy + dy, z = cz + dz;
                if (Math.abs(x) >= R - 1 || Math.abs(z) >= R - 1) continue;
                if (Math.abs(x) <= 3 && Math.abs(z) <= 3) continue;
                if (y < 60 || y > 68) continue;
                double v = r.nextDouble();
                Material m = v < 0.45 ? Material.GOLD_ORE : v < 0.53 ? Material.DEEPSLATE_GOLD_ORE : Material.STONE;
                world.getBlockAt(MINE_X + x, y, MINE_Z + z).setType(m, false);
            }
        }
    }

    private void clearMine() {
        if (world == null) return;
        for (int x = -MINE_R; x <= MINE_R; x++) for (int z = -MINE_R; z <= MINE_R; z++) for (int y = 58; y <= 70; y++) {
            Block b = world.getBlockAt(MINE_X + x, y, MINE_Z + z);
            if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
        }
    }

    private static final Material[] SOLO_CONCRETE = {
            Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.MAGENTA_CONCRETE,
            Material.BROWN_CONCRETE, Material.GRAY_CONCRETE, Material.BLACK_CONCRETE};

    private static Material teamConcrete(String team) {
        return switch (team.toLowerCase(Locale.ROOT)) {
            case "red" -> Material.RED_CONCRETE;
            case "orange" -> Material.ORANGE_CONCRETE;
            case "yellow" -> Material.YELLOW_CONCRETE;
            case "lime" -> Material.LIME_CONCRETE;
            case "green" -> Material.GREEN_CONCRETE;
            case "cyan" -> Material.CYAN_CONCRETE;
            case "aqua" -> Material.LIGHT_BLUE_CONCRETE;
            case "blue" -> Material.BLUE_CONCRETE;
            case "purple" -> Material.PURPLE_CONCRETE;
            case "pink" -> Material.PINK_CONCRETE;
            default -> Material.WHITE_CONCRETE;
        };
    }

    // ------------------------------------------------------------------ ticking

    private void onTick() {
        if (!running) return;
        tick++;
        try {
            switch (phase) {
                case COUNTDOWN -> { if (tick % 20 == 0) countdownStep(); }
                case PLAYING -> {
                    crumbleWeak();
                    if (!inRush && tick % CART_STEP_TICKS == 0) stepCarts();
                    if (running && tick % 20 == 0) second();
                }
                case FINAL -> finalTick();
                default -> { }
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Railroad Rush tick error", ex);
        }
    }

    private void countdownStep() {
        countdown--;
        if (ctx != null && ctx.participants().isEmpty()) { finishGame("Everyone left Railroad Rush"); return; }
        if (countdown > 0) {
            for (Player p : players()) {
                p.sendTitle("§e" + countdown, "§7Lay rails from your start rail!", 0, 25, 0);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            }
            return;
        }
        phase = Phase.PLAYING;
        sec = 0;
        for (Player p : players()) {
            p.sendTitle("§a§lGO!", "§7Build your railroad to the copper gate", 0, 40, 10);
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1f, 1f);
            p.sendMessage("§6§lRAILROAD RUSH §7- build the longest railroad!");
            p.sendMessage("§7• Lay rails from your start rail. Connect them to the §6copper gate§7 to open checkpoint 1.");
            p.sendMessage("§7• After that, §cTNT carts§7 ride your track. They lose 1 power per block; §epowered rails§7 refill them. Get one to the next copper door to blast it open.");
            p.sendMessage("§7• Spend team §6gold§7 at the Gold Trader. Mine gold ore; §6Gold Rushes§7 take everyone to the mine.");
            p.sendMessage("§7• At the end a §6golden cart§7 runs your track with the same power rules. Its reach is your score.");
        }
    }

    private void second() {
        sec++;
        if (ctx == null || ctx.participants().isEmpty()) { finishGame("Everyone left Railroad Rush"); return; }

        if (inRush) {
            if (sec >= rushEndSec) endRush();
        } else {
            for (int[] r : RUSHES) if (sec == r[0]) { startRush(r[1]); break; }
        }

        for (Lane l : lanes) {
            walk(l);
            if (l.doorsOpened == 0 && l.rawMaxZ >= DOORS[0] - 1) openDoor(l);
        }

        if (!inRush) {
            for (Lane l : lanes) {
                if (l.doorsOpened < 1 || l.doorsOpened >= 3 || l.carts.size() >= 2 || sec < l.nextCartSec) continue;
                int fromZ = DOORS[l.doorsOpened - 1];
                int idx = -1;
                for (int i = 0; i < l.chain.size(); i++) if (l.chain.get(i).z() >= fromZ) { idx = i; break; }
                if (idx >= 0) {
                    spawnCart(l, l.chain.get(idx), idx > 0 ? l.chain.get(idx - 1) : null);
                    l.nextCartSec = sec + CART_INTERVAL;
                }
            }
        }

        for (Player p : players()) {
            if (p.getWorld().equals(world) && p.getGameMode() != GameMode.SPECTATOR && p.getLocation().getY() < 52) respawn(p, null);
        }

        hud();
        if (sec >= GAME_SECONDS) beginFinal();
    }

    private void hud() {
        if (ctx == null) return;
        List<String> lines = new ArrayList<>();
        int nextRush = -1;
        for (int[] r : RUSHES) if (r[0] > sec) { nextRush = r[0]; break; }
        if (inRush) lines.add("§6§lGOLD RUSH §e" + fmt(rushEndSec - sec));
        else if (phase == Phase.FINAL) lines.add("§6Golden cart run!");
        else if (nextRush > 0) lines.add("§7Gold Rush in §e" + fmt(nextRush - sec));
        else lines.add("§7Final stretch!");
        List<Lane> sorted = ranked();
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            Lane l = sorted.get(i);
            lines.add("§7" + (i + 1) + ". " + l.label + " §e" + l.reach + "m §7" + l.doorsOpened + "cp");
        }
        ctx.setSidebarLines(lines);
        if (phase == Phase.PLAYING) ctx.setTimer(Math.max(0, GAME_SECONDS - sec), GAME_SECONDS);

        for (Player p : players()) {
            Lane l = laneOf.get(p.getUniqueId());
            if (l == null) continue;
            String bar;
            if (inRush) bar = "§6§lGOLD RUSH §e" + (rushEndSec - sec) + "s left §7| §6Team gold: §e" + l.gold;
            else bar = "§6Gold: §e" + l.gold + " §7| §bCheckpoints: §f" + l.doorsOpened + "/3 §7| §eCart reach: §f" + l.reach
                    + "m §7| §fTrack: " + l.rawProgress + "m";
            p.sendActionBar(legacy(bar));
        }
    }

    private List<Lane> ranked() {
        List<Lane> sorted = new ArrayList<>(lanes);
        sorted.sort(Comparator.comparingInt((Lane l) -> -l.reach)
                .thenComparingInt(l -> -l.doorsOpened)
                .thenComparingInt(l -> -l.rawProgress)
                .thenComparingInt(l -> l.chain.size()));
        return sorted;
    }

    // ------------------------------------------------------------------ track

    /** Follows the lane's track from its start rail, simulating cart power. */
    private void walk(Lane l) {
        Pos start = new Pos(l.ox, PLAY_Y, START_Z);
        if (!Track.isRail(world, start)) world.getBlockAt(start.x(), start.y(), start.z()).setBlockData(railNS, false);
        List<Pos> chain = new ArrayList<>();
        Set<Pos> seen = new HashSet<>();
        Pos prev = null, cur = start;
        int power = CART_START, maxZ = START_Z, reachZ = START_Z, reachIdx = 0, idx = 0;
        boolean powered = true;
        while (cur != null && chain.size() < MAX_CHAIN && seen.add(cur)) {
            chain.add(cur);
            if (idx > 0 && powered) {
                power--;
                if (Track.type(world, cur) == Material.POWERED_RAIL) power = Math.min(CART_MAX, power + CART_BOOST);
            }
            if (powered) {
                reachZ = Math.max(reachZ, cur.z());
                reachIdx = idx;
                if (power <= 0) powered = false;
            }
            maxZ = Math.max(maxZ, cur.z());
            Pos n = Track.next(world, prev, cur);
            prev = cur;
            cur = n;
            idx++;
        }
        l.chain = chain;
        l.rawMaxZ = maxZ;
        l.rawProgress = maxZ - START_Z;
        l.reach = reachZ - START_Z;
        l.reachIndex = reachIdx;
    }

    private void openDoor(Lane l) {
        int k = l.doorsOpened;
        if (k >= 3) return;
        int z = DOORS[k];
        for (int lx = -5; lx <= 5; lx++) for (int y = 64; y <= 72; y++) world.getBlockAt(l.ox + lx, y, z).setType(Material.AIR, false);
        Location c = new Location(world, l.ox + 0.5, 67, z + 0.5);
        world.spawnParticle(Particle.EXPLOSION, c, 8, 3, 2, 0.3, 0);
        world.spawnParticle(Particle.WAX_OFF, c, 60, 4, 3, 0.5, 0);
        world.playSound(c, Sound.BLOCK_COPPER_BREAK, 2f, 0.6f);
        l.doorsOpened++;
        l.nextCartSec = sec + 3;
        for (Player p : onlineMembers(l)) {
            ctx.addPoints(p.getUniqueId(), DOOR_POINTS, "Railroad Rush checkpoint " + (k + 1));
            p.sendTitle("§6Checkpoint " + (k + 1) + "!", k == 0 ? "§7The trader now sells everything. TNT carts incoming!" : (k == 2 ? "§7Final door down! Extend into the station." : "§7Next room unlocked"), 5, 50, 10);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }
        ctx.broadcast(l.label + " §7opened checkpoint §e" + (k + 1) + "§7!");
        if (k < 2) spawnTrader(l, z + 3);
    }

    private void spawnTrader(Lane l, int z) {
        Location loc = new Location(world, l.ox + 4.5, PLAY_Y, z + 0.5, 90f, 0f);
        Villager v = world.spawn(loc, Villager.class, vi -> {
            vi.setAI(false);
            vi.setInvulnerable(true);
            vi.setSilent(true);
            vi.setPersistent(false);
            vi.setCollidable(false);
            vi.setProfession(Villager.Profession.TOOLSMITH);
            vi.customName(legacy("§6Gold Trader"));
            vi.setCustomNameVisible(true);
        });
        traders.put(v.getUniqueId(), l);
    }

    // ------------------------------------------------------------------ TNT carts

    private Location cartLoc(Pos p) { return new Location(world, p.x() + 0.5, p.y() + 0.05, p.z() + 0.5); }

    private void spawnCart(Lane l, Pos cur, Pos prev) {
        TntCart c = new TntCart();
        c.cur = cur;
        c.prev = prev;
        c.power = CART_START;
        Location loc = cartLoc(cur);
        c.body = world.spawn(loc, BlockDisplay.class, d -> {
            d.setBlock(Material.TNT.createBlockData());
            d.setTransformation(new Transformation(new Vector3f(-0.35f, 0f, -0.35f), new AxisAngle4f(), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
            d.setTeleportDuration(CART_STEP_TICKS);
            d.setPersistent(false);
        });
        c.label = world.spawn(loc.clone().add(0, 1.1, 0), TextDisplay.class, t -> {
            t.setBillboard(Display.Billboard.CENTER);
            t.setTeleportDuration(CART_STEP_TICKS);
            t.setPersistent(false);
        });
        updateLabel(c);
        l.carts.add(c);
        world.playSound(loc, Sound.ENTITY_MINECART_RIDING, 0.8f, 1.2f);
        for (Player p : onlineMembers(l)) p.sendActionBar(legacy("§cA TNT cart is rolling down your track! Keep it powered."));
    }

    private void stepCarts() {
        for (Lane l : lanes) for (TntCart c : new ArrayList<>(l.carts)) stepCart(l, c);
    }

    private void stepCart(Lane l, TntCart c) {
        if (!Track.isRail(world, c.cur)) { explodeCart(l, c); return; }
        if (l.doorsOpened < 3 && c.cur.z() >= DOORS[l.doorsOpened] - 1 && Math.abs(c.cur.x() - l.ox) <= 5) { explodeCart(l, c); return; }
        Pos n = Track.next(world, c.prev, c.cur);
        if (n != null) {
            c.prev = c.cur;
            c.cur = n;
            c.power--;
            if (Track.type(world, n) == Material.POWERED_RAIL) {
                c.power = Math.min(CART_MAX, c.power + CART_BOOST);
                c.warned = false;
            }
            Location loc = cartLoc(n);
            if (c.body.isValid()) c.body.teleport(loc);
            if (c.label.isValid()) c.label.teleport(loc.clone().add(0, 1.1, 0));
        } else {
            c.power--;
        }
        if (c.power <= 0) { explodeCart(l, c); return; }
        if (c.power <= 6 && !c.warned) {
            c.warned = true;
            world.playSound(cartLoc(c.cur), Sound.ENTITY_TNT_PRIMED, 1f, 1f);
        }
        updateLabel(c);
    }

    private void updateLabel(TntCart c) {
        if (c.label == null || !c.label.isValid()) return;
        int filled = Math.max(0, Math.min(10, Math.round(c.power * 10f / CART_MAX)));
        String col = c.power > 12 ? "§a" : c.power > 6 ? "§e" : "§c";
        String bar = col + "■".repeat(filled) + "§8" + "■".repeat(10 - filled);
        c.label.text(legacy("§c§lTNT §r" + bar));
    }

    private void explodeCart(Lane l, TntCart c) {
        c.remove();
        l.carts.remove(c);
        blast(l, cartLoc(c.cur).add(0, 0.5, 0), 3.0, null, true);
    }

    /**
     * Custom explosion. Stone degrades one level (stone bricks, cracked, cobble, gravel, gone), leaves, sand and weak
     * planks are destroyed, gold ore is auto-collected. Rails, concrete, terrain and copper are never broken
     * (a cart blast next to the next copper door opens it).
     */
    private void blast(Lane lane, Location c, double r, Player source, boolean cart) {
        if (world == null) return;
        Lane goldTo = lane != null ? lane : (source != null ? laneOf.get(source.getUniqueId()) : null);
        int R = (int) Math.ceil(r);
        int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ();
        int gold = 0;
        for (int dx = -R; dx <= R; dx++) for (int dy = -R; dy <= R; dy++) for (int dz = -R; dz <= R; dz++) {
            if (dx * dx + dy * dy + dz * dz > r * r) continue;
            Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
            if (lane != null) {
                int lx = b.getX() - lane.ox;
                if (Math.abs(lx) > 6 || b.getY() < 62 || b.getY() > 72) continue;
            } else if (!inMine(b)) {
                continue;
            }
            Material m = b.getType();
            Material to = null;
            switch (m) {
                case STONE_BRICKS -> to = Material.CRACKED_STONE_BRICKS;
                case CRACKED_STONE_BRICKS -> to = Material.COBBLESTONE;
                case COBBLESTONE -> to = Material.GRAVEL;
                case GRAVEL, SAND, OAK_PLANKS -> to = Material.AIR;
                case GOLD_ORE -> { to = Material.AIR; gold += 1; }
                case DEEPSLATE_GOLD_ORE -> { to = Material.AIR; gold += 2; }
                case STONE -> { if (lane == null) to = Material.AIR; }
                default -> { if (Tag.LEAVES.isTagged(m)) to = Material.AIR; }
            }
            if (to != null) b.setType(to, to == Material.AIR);
        }
        if (gold > 0 && goldTo != null) {
            int g = gold * goldTo.goldMult;
            goldTo.gold += g;
            if (source != null) source.sendActionBar(legacy("§6+" + g + " gold §7(dynamite)"));
        }
        world.spawnParticle(Particle.EXPLOSION_EMITTER, c, 1);
        world.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);
        for (Player p : world.getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            Vector d = p.getLocation().toVector().subtract(c.toVector());
            double dist = d.length();
            if (dist > r + 1.5) continue;
            Vector push = dist < 0.1 ? new Vector(0, 0.6, 0) : d.normalize().multiply(0.7).setY(0.45);
            p.setVelocity(push);
        }
        if (cart && lane != null && lane.doorsOpened < 3) {
            int dz = DOORS[lane.doorsOpened];
            if (c.getZ() >= dz - 3.5 && c.getZ() <= dz + 1) openDoor(lane);
        }
    }

    // ------------------------------------------------------------------ gold rush

    private Location randomMineSpot() {
        return new Location(world, MINE_X + 0.5 + rnd.nextInt(7) - 3, 60, MINE_Z + 0.5 + rnd.nextInt(7) - 3, rnd.nextFloat() * 360f, 0f);
    }

    private boolean inMine(Block b) {
        return Math.abs(b.getX() - MINE_X) <= MINE_R && Math.abs(b.getZ() - MINE_Z) <= MINE_R && b.getY() >= 55 && b.getY() <= 72;
    }

    private void startRush(int dur) {
        inRush = true;
        rushEndSec = sec + dur;
        buildMine();
        for (Player p : players()) {
            if (!p.getWorld().equals(world) || p.getGameMode() == GameMode.SPECTATOR) continue;
            Lane l = laneOf.get(p.getUniqueId());
            if (l == null) continue;
            Location here = p.getLocation();
            rushReturn.put(p.getUniqueId(), inOwnLane(l, here) && here.getY() > 60 ? here : laneRespawn(l));
            p.teleport(randomMineSpot());
            p.setFallDistance(0);
            p.sendTitle("§6§lGOLD RUSH!", "§eMine gold ore for your team! " + dur + "s", 5, 50, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        }
        ctx.broadcast("§6§lGOLD RUSH! §eEveryone to the mine for " + dur + " seconds. Watch out for Fool's Gold!");
    }

    private void endRush() {
        inRush = false;
        for (Entity e : new ArrayList<>(world.getEntities())) if (e instanceof Skeleton || e instanceof AbstractArrow) e.remove();
        for (Player p : players()) {
            Location back = rushReturn.remove(p.getUniqueId());
            Lane l = laneOf.get(p.getUniqueId());
            if (l == null || !p.getWorld().equals(world) || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (back == null) back = laneRespawn(l);
            p.teleport(back);
            p.setFallDistance(0);
            p.sendTitle("§eBack to the tracks!", "§6Team gold: " + l.gold, 5, 30, 10);
        }
        rushReturn.clear();
        for (Lane l : lanes) l.nextCartSec = sec + 3;
    }

    private void spawnFoolsGold(Location loc) {
        world.spawn(loc, Skeleton.class, s -> {
            s.customName(legacy("§6Fool's Gold"));
            s.setCustomNameVisible(true);
            s.setPersistent(false);
            s.setRemoveWhenFarAway(false);
            if (s.getEquipment() != null) {
                s.getEquipment().setHelmet(new ItemStack(Material.GOLDEN_HELMET));
                s.getEquipment().setHelmetDropChance(0f);
                s.getEquipment().setItemInMainHandDropChance(0f);
            }
        });
        world.playSound(loc, Sound.ENTITY_SKELETON_AMBIENT, 1f, 0.7f);
    }

    // ------------------------------------------------------------------ final run

    private void beginFinal() {
        if (inRush) endRush();
        phase = Phase.FINAL;
        finalTicks = 0;
        finalWait = 0;
        finalLength = 1;
        for (Lane l : lanes) {
            for (TntCart c : l.carts) c.remove();
            l.carts.clear();
            walk(l);
            List<Pos> path = new ArrayList<>(l.chain.subList(0, Math.min(l.chain.size(), l.reachIndex + 1)));
            if (path.isEmpty()) path.add(new Pos(l.ox, PLAY_Y, START_Z));
            BlockDisplay d = world.spawn(cartLoc(path.get(0)), BlockDisplay.class, bd -> {
                bd.setBlock(Material.GOLD_BLOCK.createBlockData());
                bd.setTransformation(new Transformation(new Vector3f(-0.35f, 0f, -0.35f), new AxisAngle4f(), new Vector3f(0.7f, 0.7f, 0.7f), new AxisAngle4f()));
                bd.setTeleportDuration(2);
                bd.setPersistent(false);
                bd.setGlowing(true);
            });
            runners.add(new GoldRunner(path, d));
            finalLength = Math.max(finalLength, path.size());
        }
        ctx.setTimer(0, GAME_SECONDS);
        hud();
        for (Player p : players()) {
            p.sendTitle("§6§lGolden Cart Run!", "§7How far does your railroad really go?", 5, 60, 10);
            p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1f, 0.7f);
        }
    }

    private void finalTick() {
        finalTicks++;
        int speed = Math.max(1, (int) Math.ceil(finalLength / 120.0));
        boolean all = true;
        if (finalTicks % 2 == 0) {
            for (GoldRunner r : runners) {
                if (r.idx < r.path.size() - 1) {
                    r.idx = Math.min(r.path.size() - 1, r.idx + speed);
                    if (r.display.isValid()) r.display.teleport(cartLoc(r.path.get(r.idx)));
                    all = false;
                } else if (!r.done) {
                    r.done = true;
                    Location at = cartLoc(r.path.get(r.idx)).add(0, 0.5, 0);
                    world.spawnParticle(Particle.FIREWORK, at, 30, 0.3, 0.5, 0.3, 0.05);
                    world.playSound(at, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.6f);
                }
            }
        } else {
            for (GoldRunner r : runners) if (r.idx < r.path.size() - 1) { all = false; break; }
        }
        if (all && ++finalWait > 50) awardAndFinish();
        else if (finalTicks > 20 * 30) awardAndFinish(); // hard cap
    }

    private void awardAndFinish() {
        if (ctx == null) return;
        List<Lane> sorted = ranked();
        ctx.broadcast("§6§lRailroad Rush results:");
        for (int i = 0; i < sorted.size(); i++) {
            Lane l = sorted.get(i);
            int place = i < PLACE_POINTS.length ? PLACE_POINTS[i] : 4;
            int dist = l.reach / 5;
            for (Player p : onlineMembers(l)) {
                ctx.addPoints(p.getUniqueId(), place, "Railroad Rush #" + (i + 1));
                if (dist > 0) ctx.addPoints(p.getUniqueId(), dist, "Railroad length (" + l.reach + " blocks)");
            }
            if (i < 10) ctx.broadcast(" §e#" + (i + 1) + " " + l.label + " §7- §f" + l.reach + " blocks§7, " + l.doorsOpened + "/3 checkpoints");
        }
        finishGame("Railroad Rush complete");
    }

    // ------------------------------------------------------------------ helpers

    private List<Player> players() {
        return ctx == null ? new ArrayList<>() : new ArrayList<>(ctx.participants());
    }

    private List<Player> onlineMembers(Lane l) {
        List<Player> out = new ArrayList<>();
        for (Player p : players()) if (laneOf.get(p.getUniqueId()) == l) out.add(p);
        return out;
    }

    private Lane laneAt(int x) {
        int idx = Math.floorDiv(x + LANE_SPACING / 2, LANE_SPACING);
        if (idx < 0 || idx >= lanes.size()) return null;
        return lanes.get(idx);
    }

    private boolean inOwnLane(Lane l, Location loc) {
        return Math.abs(loc.getX() - (l.ox + 0.5)) <= 7 && loc.getZ() >= Layout.MIN_Z && loc.getZ() <= Layout.MAX_Z;
    }

    private Location laneRespawn(Lane l) {
        int k = l.doorsOpened;
        int z = k == 0 ? 2 : DOORS[k - 1] + 2;
        return new Location(world, l.ox + 0.5, PLAY_Y, z + 0.5, 0f, 0f);
    }

    private void respawn(Player p, String msg) {
        Lane l = laneOf.get(p.getUniqueId());
        Location to = inRush ? randomMineSpot() : (l != null ? laneRespawn(l) : world.getSpawnLocation());
        p.teleport(to);
        p.setFallDistance(0);
        p.setVelocity(new Vector());
        p.setFireTicks(0);
        heal(p);
        if (msg != null) p.sendTitle("", msg, 0, 30, 10);
    }

    @SuppressWarnings("deprecation")
    private static void heal(Player p) {
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(10f);
    }

    private void addGold(Lane l, int amount, Player p) {
        l.gold += amount;
        if (p != null) p.sendActionBar(legacy("§6+" + amount + " gold §7(team: " + l.gold + ")"));
        if (p != null) p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 1.4f);
    }

    private void later(long delay, Runnable r) {
        final int g = gen;
        final BukkitTask[] holder = new BukkitTask[1];
        holder[0] = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pending.remove(holder[0]);
            if (g == gen && running) {
                try { r.run(); } catch (Exception ex) { plugin.getLogger().log(Level.WARNING, "Railroad Rush task error", ex); }
            }
        }, delay);
        pending.add(holder[0]);
    }

    private void crumbleWeak() {
        while (!weak.isEmpty() && weak.peekFirst().at() <= tick) {
            Block b = weak.pollFirst().block();
            if (b.getType() == WEAK) {
                world.spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5), 12, 0.3, 0.3, 0.3, 0, WEAK.createBlockData());
                b.setType(Material.AIR, true);
            }
        }
    }

    private void refillWeak(Player p) {
        later(1, () -> {
            if (!p.isOnline()) return;
            int size = p.getInventory().getStorageContents().length;
            for (int i = 0; i < size; i++) {
                ItemStack it = p.getInventory().getItem(i);
                if (it != null && it.getType() == WEAK) {
                    if (it.getAmount() < 32) { it.setAmount(32); p.getInventory().setItem(i, it); }
                    return;
                }
            }
            p.getInventory().addItem(weakPlanks());
        });
    }

    private static Component legacy(String s) {
        return LegacyComponentSerializer.legacySection().deserialize(s).decoration(TextDecoration.ITALIC, false);
    }

    private static String fmt(int s) {
        s = Math.max(0, s);
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    private boolean inWorld(Entity e) {
        return running && world != null && e.getWorld().equals(world);
    }

    // ------------------------------------------------------------------ shop

    private void openShop(Player p, Lane l) {
        ShopHolder h = new ShopHolder(l);
        Inventory inv = Bukkit.createInventory(h, 27, legacy("§6Gold Trader"));
        h.inv = inv;
        renderShop(inv, l);
        p.openInventory(inv);
    }

    private void renderShop(Inventory inv, Lane l) {
        ItemStack pane = icon(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);
        inv.setItem(4, icon(Material.GOLD_INGOT, "§6Team gold: §e" + l.gold,
                List.of("§7Mine gold ore to earn more.", "§7Gold Rushes send everyone to the mine.")));
        for (Offer o : OFFERS) {
            boolean locked = !o.tutorial() && l.doorsOpened < 1;
            Material mat = locked ? Material.BARRIER : (o.mat() == null ? l.concrete : o.mat());
            List<String> lore = new ArrayList<>();
            lore.add("§7" + o.desc());
            lore.add("");
            lore.add("§6Cost: §e" + o.cost() + " gold");
            if (locked) lore.add("§cUnlocks at checkpoint 1");
            else lore.add(l.gold >= o.cost() ? "§aClick to buy" : "§cNot enough gold");
            inv.setItem(o.slot(), icon(mat, (locked ? "§8" : "§e") + o.name(), lore));
        }
        inv.setItem(22, icon(Material.TNT, "§cTNT carts", List.of(
                "§7From checkpoint 1 on, a TNT cart rides",
                "§7your track every ~" + CART_INTERVAL + "s. It loses 1 power",
                "§7per block (and while stuck) and explodes",
                "§7when empty or when it reaches a door.",
                "§7Powered rails refill it.")));
    }

    private ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if (m != null) {
            m.displayName(legacy(name));
            List<Component> ls = new ArrayList<>();
            for (String s : lore) ls.add(legacy(s));
            m.lore(ls);
            it.setItemMeta(m);
        }
        return it;
    }

    private void buy(Player p, Lane l, Offer o) {
        if (!running || phase != Phase.PLAYING) return;
        if (!o.tutorial() && l.doorsOpened < 1) {
            p.sendMessage("§cThat unlocks once your team opens checkpoint 1.");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (l.gold < o.cost()) {
            p.sendMessage("§cNot enough team gold (" + l.gold + "/" + o.cost() + ").");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (p.getInventory().firstEmpty() == -1) {
            p.sendMessage("§cYour inventory is full.");
            return;
        }
        l.gold -= o.cost();
        Material mat = o.mat() == null ? l.concrete : o.mat();
        ItemStack goods;
        if (mat == Material.FIRE_CHARGE) goods = dynamite(o.amount());
        else if (mat == Material.SHEARS) {
            goods = new ItemStack(Material.SHEARS);
            ItemMeta m = goods.getItemMeta();
            if (m instanceof Damageable d) d.setDamage(Material.SHEARS.getMaxDurability() - 40);
            goods.setItemMeta(m);
        } else goods = new ItemStack(mat, o.amount());
        p.getInventory().addItem(goods);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
    }

    // ------------------------------------------------------------------ listeners

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!inWorld(p)) return;
        e.setDropItems(false);
        e.setExpToDrop(0);
        Lane lane = laneOf.get(p.getUniqueId());
        if (phase != Phase.PLAYING || lane == null) { e.setCancelled(true); return; }
        Block b = e.getBlock();
        Material m = b.getType();

        if (inMine(b)) {
            if (!inRush) { e.setCancelled(true); return; }
            switch (m) {
                case GOLD_ORE -> {
                    addGold(lane, lane.goldMult, p);
                    if (rnd.nextDouble() < 0.06) {
                        Location at = b.getLocation().add(0.5, 0, 0.5);
                        later(1, () -> spawnFoolsGold(at));
                    }
                }
                case DEEPSLATE_GOLD_ORE -> addGold(lane, 2 * lane.goldMult, p);
                case STONE, OAK_PLANKS -> { }
                default -> e.setCancelled(true);
            }
            return;
        }

        if (laneAt(b.getX()) != lane || Math.abs(b.getX() - lane.ox) > 6) { e.setCancelled(true); return; }
        switch (m) {
            case RAIL, POWERED_RAIL -> {
                if (b.getX() == lane.ox && b.getY() == PLAY_Y && b.getZ() == START_Z) { e.setCancelled(true); return; }
                p.getInventory().addItem(new ItemStack(m));
            }
            case SAND -> p.getInventory().addItem(new ItemStack(Material.SAND));
            case OAK_PLANKS, GRAVEL, COBBLESTONE -> { }
            case GOLD_ORE -> addGold(lane, lane.goldMult, p);
            case EMERALD_ORE -> {
                ctx.addPoints(p.getUniqueId(), EMERALD_POINTS, "Railroad Rush emerald");
                p.sendActionBar(legacy("§a+" + EMERALD_POINTS + " points §7(emerald)"));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.6f);
            }
            default -> {
                if (Tag.LEAVES.isTagged(m)) {
                    if (p.getInventory().getItemInMainHand().getType() != Material.SHEARS) {
                        e.setCancelled(true);
                        p.sendActionBar(legacy("§cYou need shears (or an explosion) to clear leaves."));
                    }
                } else if (m == lane.concrete && b.getZ() > START_Z) {
                    p.getInventory().addItem(new ItemStack(m));
                } else {
                    e.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!inWorld(p)) return;
        Lane lane = laneOf.get(p.getUniqueId());
        if (phase != Phase.PLAYING || lane == null) { e.setCancelled(true); return; }
        Block b = e.getBlockPlaced();
        Material m = b.getType();

        if (inMine(b)) {
            if (m == WEAK) { weak.add(new WeakBlock(b, tick + WEAK_TICKS)); refillWeak(p); }
            else e.setCancelled(true);
            return;
        }
        int lx = b.getX() - lane.ox;
        if (laneAt(b.getX()) != lane || Math.abs(lx) > 5 || b.getY() < 62 || b.getY() > 72 || b.getZ() <= START_Z || b.getZ() > END_Z) {
            e.setCancelled(true);
            return;
        }
        if (m == WEAK) {
            weak.add(new WeakBlock(b, tick + WEAK_TICKS));
            refillWeak(p);
        } else if (m == Material.RAIL || m == Material.POWERED_RAIL) {
            Material below = b.getRelative(0, -1, 0).getType();
            if (below == WEAK || Tag.LEAVES.isTagged(below) || !below.isSolid()) {
                e.setCancelled(true);
                p.sendActionBar(legacy("§cRails need solid ground. Weak planks can't hold track."));
            }
        } else if (m != lane.concrete && m != Material.SAND) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!inWorld(p)) return;
        ItemStack it = e.getItem();
        if (it == null || it.getType() != Material.FIRE_CHARGE) return;
        e.setCancelled(true);
        if (phase != Phase.PLAYING || e.getHand() != EquipmentSlot.HAND || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block target = e.getClickedBlock();
        if (target == null) return;
        Lane own = laneOf.get(p.getUniqueId());
        if (own == null) return;
        Lane lane;
        if (inMine(target)) lane = null;
        else {
            lane = laneAt(target.getX());
            if (lane != own) return;
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand.getType() != Material.FIRE_CHARGE) return;
        hand.setAmount(hand.getAmount() - 1);
        Location c = target.getLocation().add(0.5, 0.5, 0.5);
        world.playSound(c, Sound.ENTITY_TNT_PRIMED, 1f, 1.3f);
        for (int t = 0; t < 30; t += 5) later(t, () -> world.spawnParticle(Particle.SMOKE, c, 6, 0.2, 0.2, 0.2, 0.01));
        later(30, () -> blast(lane, c, 2.3, p, false));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityInteract(PlayerInteractEntityEvent e) {
        if (!inWorld(e.getPlayer())) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getRightClicked() instanceof Villager v && traders.containsKey(v.getUniqueId())) {
            Lane l = laneOf.get(e.getPlayer().getUniqueId());
            if (l != null && phase == Phase.PLAYING) openShop(e.getPlayer(), l);
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof ShopHolder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= 27) return;
        for (Offer o : OFFERS) {
            if (o.slot() == slot) {
                buy(p, h.lane, o);
                renderShop(e.getView().getTopInventory(), h.lane);
                return;
            }
        }
    }

    @EventHandler
    public void onInvDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof ShopHolder) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        Entity ent = e.getEntity();
        if (!inWorld(ent)) return;
        if (ent instanceof Player p) {
            if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
                e.setCancelled(true);
                respawn(p, null);
                return;
            }
            boolean mobHit = false;
            if (e instanceof EntityDamageByEntityEvent ed) {
                Entity d = ed.getDamager();
                if (d instanceof Skeleton) mobHit = true;
                if (d instanceof Projectile pr && pr.getShooter() instanceof Skeleton) mobHit = true;
            }
            if (!mobHit || phase != Phase.PLAYING) { e.setCancelled(true); return; }
            if (e.getFinalDamage() >= p.getHealth()) {
                e.setCancelled(true);
                respawn(p, "§6Fool's Gold got you!");
            }
            return;
        }
        if (ent instanceof Skeleton) {
            boolean byPlayer = false;
            if (e instanceof EntityDamageByEntityEvent ed) {
                Entity d = ed.getDamager();
                byPlayer = d instanceof Player || (d instanceof Projectile pr && pr.getShooter() instanceof Player);
            }
            if (!byPlayer) e.setCancelled(true);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (e.getTo().getY() >= 52) return;
        Player p = e.getPlayer();
        if (!inWorld(p) || p.getGameMode() == GameMode.SPECTATOR || laneOf.get(p.getUniqueId()) == null) return;
        if (phase == Phase.IDLE || phase == Phase.BUILDING) return;
        respawn(p, "§7You fell! Back to your checkpoint.");
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (!inWorld(e.getEntity())) return;
        e.setCancelled(true);
        e.getEntity().setFoodLevel(20);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (inWorld(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onItemSpawn(ItemSpawnEvent e) {
        if (inWorld(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onChangeBlock(EntityChangeBlockEvent e) {
        // stops sand/gravel falling and any mob griefing in the arena
        if (inWorld(e.getEntity()) && (e.getEntity() instanceof FallingBlock || !(e.getEntity() instanceof Player))) e.setCancelled(true);
    }

    @EventHandler
    public void onCombust(EntityCombustEvent e) {
        if (inWorld(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (!inWorld(e.getEntity())) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        if (!inWorld(e.getEntity())) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!running || world == null) return;
        Lane l = laneOf.get(e.getPlayer().getUniqueId());
        if (l == null || !e.getPlayer().getWorld().equals(world)) return;
        e.setRespawnLocation(inRush ? randomMineSpot() : laneRespawn(l));
    }

    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent e) {
        if (inWorld(e.getEntity()) && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) e.setCancelled(true);
    }

    @EventHandler
    public void onIgnite(BlockIgniteEvent e) {
        if (running && world != null && e.getBlock().getWorld().equals(world)) e.setCancelled(true);
    }

    @EventHandler
    public void onBurn(BlockBurnEvent e) {
        if (running && world != null && e.getBlock().getWorld().equals(world)) e.setCancelled(true);
    }

    @EventHandler
    public void onLeaves(LeavesDecayEvent e) {
        if (world != null && e.getBlock().getWorld().equals(world)) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        rushReturn.remove(e.getPlayer().getUniqueId());
    }
}
