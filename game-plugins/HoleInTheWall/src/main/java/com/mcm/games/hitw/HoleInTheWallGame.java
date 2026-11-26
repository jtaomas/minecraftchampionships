package com.mcm.games.hitw;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public final class HoleInTheWallGame implements McmGame, Listener {
    private enum State { IDLE, COUNTDOWN, PLAYING, ENDING }

    private static final int MAIN_SECONDS = 300;      // normal round length
    private static final int OVERDRIVE_SECONDS = 60;  // extremely fast walls, then hard end
    private static final int SINGLE_WALL_SECONDS = 120; // owner: one wall at a time (no doubles, no sandwiches) until 2:00
    private static final int COUNTDOWN_SECONDS = 5;
    private static final int[] INTERVAL_BY_LEVEL = {6, 5, 5, 4, 4, 3, 3, 3, 2, 2}; // ticks per cell, levels 1..10
    // MCC coins (base x1.0, config coins.*; the core applies the event multiplier). See game-plugins/COINS.md.
    private int[] PLACEMENT_BONUS = {100, 70, 30};   // MCC: top 3 (tied players split the sum of the tied places)
    private int wallCoins = 2;                       // MCC: per wall survived
    private static final int PVP_GRACE_TICKS = 100;

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final HoleInTheWallPlugin plugin;
    private final Arena arena = new Arena();
    private final Random rnd = new Random();

    private State state = State.IDLE;
    private McmGameContext ctx;
    private BukkitTask loopTask;
    private BukkitTask endTask;

    private final Set<UUID> participants = new LinkedHashSet<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, Location> spawns = new HashMap<>();
    /** Elimination order: each entry is {uuid, tick}. */
    private final List<Map.Entry<UUID, Integer>> eliminations = new ArrayList<>();
    private final Map<UUID, Integer> lastHit = new HashMap<>();

    private final List<Wall> walls = new ArrayList<>();
    private Map<Long, BlockData> rendered = new HashMap<>();

    private int initialCount;
    private int perElimPoints;
    private int countdownTicks;
    private int elapsedTicks;
    private int waveCooldown;
    private int wavesCleared;
    private int sidesNow = 1;
    private int platformHalf = Arena.HALF;
    private boolean overdrive;
    private boolean finished;
    /** Config {@code difficulty-head-start-seconds}: the wall ramp starts as if this much time had passed. */
    private int headStartSec = 60;
    private SpectatorGuard guard;
    /**
     * MCC: Standard + Sike are always on and the other types join one at a time ("New Wall Type appearing").
     * This game's order, picked at start; type i joins at UNLOCK_SECONDS[i] on the difficulty clock.
     */
    private static final int[] UNLOCK_SECONDS = {0, 30, 100, 140, 190, 240, 290};
    private final List<WallType> unlockOrder = new ArrayList<>();
    private int announcedUnlocks;
    private int lastLevel;
    private int lastTierBand;
    /** Config {@code debug-walls} (default off): log every wall's pattern and types to the console. */
    private boolean debugWalls;

    HoleInTheWallGame(HoleInTheWallPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ McmGame

    boolean isRunning() { return state != State.IDLE; }
    Arena arena() { return arena; }

    @Override public String id() { return "hitw"; }
    @Override public String displayName() { return "Hole in the Wall"; }
    @Override public Material icon() { return Material.LIME_CONCRETE; }
    @Override public String description() { return "Dodge walls by slipping through their holes. Last one standing wins."; }
    @Override public boolean useCoreScoreboard() { return true; }

    @Override
    public void start(McmGameContext context) {
        if (state != State.IDLE) stop();
        this.ctx = context;
        this.finished = false;
        resetFields();
        try {
            plugin.reloadConfig();
            headStartSec = Math.max(0, Math.min(MAIN_SECONDS, plugin.getConfig().getInt("difficulty-head-start-seconds", 60)));
            debugWalls = plugin.getConfig().getBoolean("debug-walls", false);
        } catch (Throwable t) {
            headStartSec = 60;
            debugWalls = false;
        }
        pickUnlockOrder();
        if (guard == null) guard = new SpectatorGuard(plugin, this::spectatorZone);

        World w = arena.ensureWorld();
        if (w == null) {
            plugin.getLogger().severe("Could not create world " + Arena.WORLD_NAME);
            finishNow("World could not be created");
            return;
        }
        arena.clear();
        arena.build();
        HitwEdit.applyOverlay(plugin, w);   // saved Map Edit overlay (core /mcm mapedit)

        // spawn positions: one spot per side around the centre, team-coloured pads
        Map<String, List<Player>> sides = context.sides();
        List<String> sideKeys = new ArrayList<>();
        for (Map.Entry<String, List<Player>> e : sides.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) sideKeys.add(e.getKey());
        }
        int m = sideKeys.size();
        double radius = m <= 1 ? 0 : (m <= 10 ? 5 : 5.5);
        Set<UUID> placed = new HashSet<>();
        for (int i = 0; i < m; i++) {
            String key = sideKeys.get(i);
            double ang = 2 * Math.PI * i / Math.max(1, m);
            double cx = 0.5 + radius * Math.cos(ang), cz = 0.5 + radius * Math.sin(ang);
            int bx = (int) Math.floor(cx), bz = (int) Math.floor(cz);
            Material pad = padMaterial(key, sides.get(key));
            w.getBlockAt(bx, Arena.FLOOR_Y, bz).setType(pad, false);
            if (m <= 10) {
                w.getBlockAt(bx + 1, Arena.FLOOR_Y, bz).setType(pad, false);
                w.getBlockAt(bx - 1, Arena.FLOOR_Y, bz).setType(pad, false);
                w.getBlockAt(bx, Arena.FLOOR_Y, bz + 1).setType(pad, false);
                w.getBlockAt(bx, Arena.FLOOR_Y, bz - 1).setType(pad, false);
            }
            List<Player> members = sides.get(key);
            for (int j = 0; j < members.size(); j++) {
                Player p = members.get(j);
                if (p == null || !p.isOnline() || !placed.add(p.getUniqueId())) continue;
                double off = members.size() == 1 ? 0 : 0.9;
                double a2 = 2 * Math.PI * j / members.size();
                double px = clampIn(bx + 0.5 + off * Math.cos(a2)), pz = clampIn(bz + 0.5 + off * Math.sin(a2));
                Location loc = new Location(w, px, Arena.FLOOR_Y + 1, pz);
                loc.setDirection(new Vector(0.5 - px, 0, 0.5 - pz).lengthSquared() < 0.01 ? new Vector(0, 0, 1) : new Vector(0.5 - px, 0, 0.5 - pz));
                spawns.put(p.getUniqueId(), loc);
            }
        }
        // anyone not covered by sides()
        for (Player p : new ArrayList<>(context.participants())) {
            if (placed.add(p.getUniqueId())) {
                spawns.put(p.getUniqueId(), new Location(w, 0.5 + rnd.nextDouble() * 4 - 2, Arena.FLOOR_Y + 1, 0.5 + rnd.nextDouble() * 4 - 2));
            }
        }

        for (Player p : new ArrayList<>(context.participants())) {
            Location loc = spawns.get(p.getUniqueId());
            if (loc == null) continue;
            participants.add(p.getUniqueId());
            alive.add(p.getUniqueId());
            names.put(p.getUniqueId(), coloredName(p));
            preparePlayer(p);
            p.teleport(loc);
        }
        initialCount = alive.size();
        syncGhosts();
        perElimPoints = initialCount >= 2 ? Math.max(0, plugin.getConfig().getInt("coins.outlive", 2)) : 0;   // MCC: 2 per player death while alive
        wallCoins = Math.max(0, plugin.getConfig().getInt("coins.wall", 2));
        java.util.List<Integer> pb = plugin.getConfig().getIntegerList("coins.placement");
        PLACEMENT_BONUS = pb.size() >= 1 ? pb.stream().mapToInt(Integer::intValue).toArray() : new int[]{100, 70, 30};

        context.broadcast("§a§lHole in the Wall §7— walls with holes sweep across the platform.");
        context.broadcast("§7Slip through the holes (some need a jump!) or get pushed into the void.");
        StringBuilder top = new StringBuilder();
        for (int k = 0; k < PLACEMENT_BONUS.length; k++) top.append(k == 0 ? "" : "/").append(PLACEMENT_BONUS[k]);
        context.broadcast("§7Coins: §6+" + perElimPoints + "§7 every time a player falls while you stand, §6+" + wallCoins
                + "§7 per wall survived. Top " + PLACEMENT_BONUS.length + ": §6" + top + "§7.");
        context.broadcast("§7Walls speed up; after 5 min: §cOVERDRIVE§7.");

        state = State.COUNTDOWN;
        countdownTicks = FastTest.secs(COUNTDOWN_SECONDS) * 20;
        context.setTimer(MAIN_SECONDS, MAIN_SECONDS);
        updateSidebar();
        loopTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    @Override
    public void stop() {
        cancelTasks();
        boolean wasRunning = state != State.IDLE;
        state = State.IDLE;
        walls.clear();
        if (wasRunning) {
            renderWalls();
            clearItems();
            clearGhosts();
            arena.clear();
        }
        rendered.clear();
        ctx = null;
    }

    // ------------------------------------------------------------------ main loop

    private void tick() {
        if (ctx == null || state == State.IDLE || state == State.ENDING) return;
        World w = arena.world();
        if (w == null) { endGame("Arena world unloaded"); return; }

        refreshAlive();
        if (Bukkit.getCurrentTick() % 5 == 0) syncGhosts();

        if (state == State.COUNTDOWN) {
            // keep people on the platform while counting down
            for (UUID id : new ArrayList<>(alive)) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && (p.getWorld() != w || p.getLocation().getY() < Arena.FLOOR_Y - 4)) {
                    Location s = spawns.get(id);
                    if (s != null) { p.teleport(s); p.setFallDistance(0); }
                }
            }
            if (countdownTicks % 20 == 0) {
                int sec = countdownTicks / 20;
                if (sec > 0) {
                    ctx.setTimer(MAIN_SECONDS, MAIN_SECONDS);
                    titleAll("§a" + sec, "§7Get ready to dodge!", 0, 25, 0);
                    soundAll(Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.2f);
                }
            }
            if (alive.isEmpty()) { endGame("Everyone left"); return; }
            if (--countdownTicks <= 0) {
                state = State.PLAYING;
                elapsedTicks = 0;
                waveCooldown = 20;
                titleAll("§a§lGO!", "§7Find the holes!", 0, 20, 10);
                soundAll(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                List<String> already = new ArrayList<>();
                for (WallType t : unlockedTypes()) already.add(t.color + t.label);
                announcedUnlocks = unlockedTypes().size();
                lastLevel = level();
                lastTierBand = tierBand(lastLevel);
                ctx.broadcast("§7Walls start at §e" + difficultyName() + "§7"
                        + (headStartSec > 0 ? " (a " + headStartSec + " s head start)" : "")
                        + ". Wall types: §aStandard§7, §aSike§7" + (already.isEmpty() ? "" : ", " + String.join("§7, ", already)) + "§7.");
            }
            return;
        }

        // ---- PLAYING
        elapsedTicks++;

        // falls
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            if (p.getWorld() != w || p.getLocation().getY() < Arena.ELIM_Y) eliminate(p, true);
        }
        if (checkEnd()) return;

        // scheduled events
        int sec = elapsedTicks / 20;
        boolean onSecond = elapsedTicks % 20 == 0;
        if (onSecond) {
            if (sec == 115) warnShrink(0);
            else if (sec == 120) shrink(0);
            else if (sec == 205) warnShrink(1);
            else if (sec == 210) shrink(1);
            else if (sec == MAIN_SECONDS) startOverdrive();
            if (sec == SINGLE_WALL_SECONDS - 30 || sec == SINGLE_WALL_SECONDS - 10) warnDoubleWalls(SINGLE_WALL_SECONDS - sec);
            else if (sec >= SINGLE_WALL_SECONDS - 5 && sec < SINGLE_WALL_SECONDS) warnDoubleWalls(SINGLE_WALL_SECONDS - sec);
            else if (sec == SINGLE_WALL_SECONDS) {
                titleAll("§c§lDOUBLE WALLS", "§7Two walls can now come at once!", 5, 50, 10);
                soundAll(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
            }
            else if (sec >= MAIN_SECONDS + OVERDRIVE_SECONDS) { endGame("Time is up"); return; }
            // wall-type unlocks follow the difficulty clock (head start included), announced when they really happen
            if (!overdrive && sec > 0) announceProgress();
        }

        // waves
        if (waveCooldown > 0) waveCooldown--;
        // owner: never two walls on the field at once before 2:00 of real play time
        if (waveCooldown <= 0 && walls.size() < 6 && (sec >= SINGLE_WALL_SECONDS || walls.isEmpty())) spawnWave();

        // walls
        for (Wall wall : new ArrayList<>(walls)) {
            wall.tick(this);
            if (state != State.PLAYING) return; // game ended inside a callback
        }
        Iterator<Wall> it = walls.iterator();
        while (it.hasNext()) {
            Wall wall = it.next();
            if (wall.removed) {
                it.remove();
                Wall.Wave wave = wall.wave;
                if (--wave.remaining <= 0 && wave.crossed && !alive.isEmpty()) {
                    wavesCleared++;
                    for (UUID id : new ArrayList<>(alive)) {
                        if (Bukkit.getPlayer(id) != null && wallCoins > 0) ctx.addPoints(id, wallCoins, "Survived a wall");
                    }
                }
            }
        }
        renderWalls();

        if (onSecond) {
            int left = overdrive ? (MAIN_SECONDS + OVERDRIVE_SECONDS - sec) : (MAIN_SECONDS - sec);
            ctx.setTimer(Math.max(0, left), overdrive ? OVERDRIVE_SECONDS : MAIN_SECONDS);
            updateSidebar();
        }
    }

    /**
     * Seconds used for the difficulty ramp (wall level / speed / spawn spacing / hole count / wall-type unlocks):
     * real play time plus the configured head start. The game clock, shrinks, overdrive and scoring use real time.
     */
    private int difficultySec() {
        return elapsedTicks / 20 + headStartSec;
    }

    private int level() {
        return Math.min(INTERVAL_BY_LEVEL.length, 1 + difficultySec() / 30);
    }

    private int currentInterval() {
        return overdrive ? 1 : INTERVAL_BY_LEVEL[level() - 1];
    }

    int platformHalf() { return platformHalf; }

    // ------------------------------------------------------------------ waves & walls

    private void spawnWave() {
        int lvl = level();
        int count;
        if (overdrive) count = 3 + rnd.nextInt(2);
        else if (lvl <= 2) count = 1;
        else if (lvl == 3) count = rnd.nextDouble() < 0.3 ? 2 : 1;
        else if (lvl <= 5) count = rnd.nextBoolean() ? 2 : 1;
        else if (lvl <= 7) count = 2;
        else if (lvl <= 9) count = rnd.nextDouble() < 0.35 ? 3 : 2;
        else count = 3;
        boolean singleOnly = !overdrive && elapsedTicks / 20 < SINGLE_WALL_SECONDS;
        if (singleOnly) count = 1;
        count = Math.min(count, 6 - walls.size());
        if (count <= 0) return;

        int interval = currentInterval();

        // free sides: {axisX ? 1 : 0, dir}; a Sandwich takes both ends of one axis
        List<int[]> dirs = new ArrayList<>(List.of(new int[]{1, 1}, new int[]{1, -1}, new int[]{0, 1}, new int[]{0, -1}));
        Collections.shuffle(dirs, rnd);
        Wall.Wave wave = new Wall.Wave();
        StringBuilder announce = new StringBuilder();
        int made = 0;
        for (int i = 0; i < count && !dirs.isEmpty(); i++) {
            int[] d = dirs.remove(0);
            EnumSet<WallType> types = overdrive ? EnumSet.of(WallType.STANDARD) : rollTypes();
            int delay = made == 0 ? 0 : rnd.nextInt(Math.max(1, interval * 6)) + interval * 2;
            int[] partner = null;
            if (types.contains(WallType.SANDWICH)) {
                for (int[] o : dirs) if (o[0] == d[0] && o[1] == -d[1]) { partner = o; break; }
                if (singleOnly || partner == null || walls.size() + made + 2 > 6) types = EnumSet.of(WallType.STANDARD);
                else dirs.remove(partner);
            }
            Wall wall = new Wall(d[0] == 1, d[1], types, interval, overdrive, rollTier(lvl), platformHalf, delay, wave, rnd);
            walls.add(wall);
            wave.remaining++;
            made++;
            logWall(wall);
            if (partner != null) {
                Wall other = new Wall(partner[0] == 1, partner[1], EnumSet.copyOf(types), interval, overdrive,
                        wall.tier, platformHalf, delay, wave, rnd);
                walls.add(other);
                wave.remaining++;
                made++;
                logWall(other);
            }
            if (announce.length() > 0) announce.append("§7, ");
            announce.append(wall.label()).append(" §7from the §f").append(partner != null ? "both sides" : wall.fromName());
        }
        sidesNow = made;
        actionBarAll("§e⚠ " + announce);
        soundAll(Sound.BLOCK_PISTON_EXTEND, 0.8f, 0.7f);

        int travel = overdrive ? Wall.TOTAL / 2 : interval * Wall.TOTAL;
        double factor = overdrive ? 0.6 : Math.max(0.45, 0.85 - 0.05 * (lvl - 1));
        waveCooldown = Math.max(overdrive ? 14 : 30, (int) (travel * factor));
    }

    private EnumSet<WallType> rollTypes() {
        if (rnd.nextDouble() < 0.10) return EnumSet.of(WallType.SIKE);
        List<WallType> pool = unlockedTypes();
        if (pool.isEmpty() || rnd.nextDouble() < 0.45) return EnumSet.of(WallType.STANDARD);
        // the newest type shows up a bit more often, like MCC's "New Wall Type appearing"
        WallType first = rnd.nextDouble() < 0.3 ? pool.get(pool.size() - 1) : pool.get(rnd.nextInt(pool.size()));
        EnumSet<WallType> set = EnumSet.of(first);
        Set<WallType> combinable = EnumSet.of(WallType.FROZEN, WallType.WALTZ, WallType.SPRINT, WallType.MORPH);
        if (combinable.contains(first) && rnd.nextDouble() < 0.2) {
            List<WallType> second = new ArrayList<>();
            for (WallType t : pool) if (t != first && combinable.contains(t)) second.add(t);
            if (!second.isEmpty()) set.add(second.get(rnd.nextInt(second.size())));
        }
        return set;
    }

    /** This game's unlock order: early types first, the nastiest ones (Dance, Vengeful) not before the fifth. */
    private void pickUnlockOrder() {
        unlockOrder.clear();
        announcedUnlocks = 0;
        List<WallType> left = new ArrayList<>(List.of(WallType.specials()));
        for (int i = 0; i < UNLOCK_SECONDS.length && !left.isEmpty(); i++) {
            int maxStage = i < 2 ? 0 : i < 4 ? 1 : 2;
            List<WallType> ok = new ArrayList<>();
            for (WallType t : left) if (t.stage <= maxStage) ok.add(t);
            if (ok.isEmpty()) ok = left;
            WallType pick = ok.get(rnd.nextInt(ok.size()));
            unlockOrder.add(pick);
            left.remove(pick);
        }
    }

    private List<WallType> unlockedTypes() {
        int sec = difficultySec();
        List<WallType> out = new ArrayList<>();
        for (int i = 0; i < unlockOrder.size() && i < UNLOCK_SECONDS.length; i++)
            if (sec >= UNLOCK_SECONDS[i]) out.add(unlockOrder.get(i));
        return out;
    }

    /**
     * Pattern tier (MCC diagram sets E / M / D / X) by wall level: easy walls first, extreme walls at the end.
     * Weights per level 1..10 for {Easy, Medium, Difficult, Extreme}.
     */
    private static final int[][] TIER_WEIGHTS = {
            {100, 0, 0, 0}, {100, 0, 0, 0}, {70, 30, 0, 0}, {40, 50, 10, 0}, {20, 50, 30, 0},
            {0, 40, 50, 10}, {0, 25, 50, 25}, {0, 10, 50, 40}, {0, 0, 40, 60}, {0, 0, 40, 60}};

    private int rollTier(int lvl) {
        int[] w = overdrive ? new int[]{0, 0, 30, 70} : TIER_WEIGHTS[Math.max(1, Math.min(10, lvl)) - 1];
        int r = rnd.nextInt(100);
        for (int t = 0; t < 4; t++) { r -= w[t]; if (r < 0) return t; }
        return 3;
    }

    /** Which tier mix a level uses (for "Wall difficulty increased!"). */
    private static int tierBand(int lvl) {
        return lvl <= 2 ? 0 : lvl <= 3 ? 1 : lvl <= 5 ? 2 : lvl <= 8 ? 3 : 4;
    }

    /** MCC's difficulty names (MCC Island table), one per wall level. */
    private static final String[] DIFFICULTY_NAMES = {"Very Easy", "Easy", "Normal", "Moderate", "Challenging",
            "Difficult", "Very Difficult", "Extreme", "Crazy Extreme", "Crazy Extreme"};

    private String difficultyName() {
        return overdrive ? "Overdrive" : DIFFICULTY_NAMES[level() - 1];
    }

    private void logWall(Wall wall) {
        if (!debugWalls) return;
        StringBuilder t = new StringBuilder();
        for (WallType x : wall.types) { if (t.length() > 0) t.append('+'); t.append(x.name()); }
        plugin.getLogger().info("[walls] " + wall.patternName + " (" + WallPatterns.TIER_NAMES[wall.tier] + ") " + t
                + " from " + wall.fromName() + " axisX=" + wall.axisX + " dir=" + wall.dir + " level=" + level());
    }

    /** Vengeful walls follow one living player: the one nearest the wall's middle when it first looks. */
    private final Map<Wall, UUID> vengefulTargets = new IdentityHashMap<>();

    double vengefulTarget(Wall wall) {
        World w = arena.world();
        if (w == null || alive.isEmpty()) return Double.NaN;
        UUID id = vengefulTargets.get(wall);
        Player p = id == null ? null : Bukkit.getPlayer(id);
        if (p == null || !alive.contains(id)) {
            List<UUID> ids = new ArrayList<>(alive);
            id = ids.get(rnd.nextInt(ids.size()));
            vengefulTargets.put(wall, id);
            p = Bukkit.getPlayer(id);
            if (p == null) return Double.NaN;
        }
        Location l = p.getLocation();
        return wall.axisX ? l.getZ() : l.getX();
    }

    /** Called by a wall after each single-cell step: push anyone it runs into. */
    void onWallStep(Wall wall, int moveDir) {
        int pos = wall.pos();
        if (moveDir == wall.dir && wall.dir * pos == Arena.HALF + 1) wall.wave.crossed = true;
        if (Math.abs(pos) > Arena.HALF + 1) return;
        World w = arena.world();
        if (w == null) return;
        final double eps = 0.08;
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || p.getWorld() != w) continue;
            BoundingBox bb = p.getBoundingBox();
            double aMin = wall.axisX ? bb.getMinX() : bb.getMinZ();
            double aMax = wall.axisX ? bb.getMaxX() : bb.getMaxZ();
            if (aMax - eps <= pos || aMin + eps >= pos + 1) continue;
            double lMin = wall.axisX ? bb.getMinZ() : bb.getMinX();
            double lMax = wall.axisX ? bb.getMaxZ() : bb.getMaxX();
            int laMin = (int) Math.floor(lMin + eps), laMax = (int) Math.floor(lMax - eps);
            int rMin = (int) Math.floor(bb.getMinY() - Arena.WALL_Y + eps);
            int rMax = (int) Math.floor(bb.getMaxY() - Arena.WALL_Y - eps);
            if (rMax < 0 || rMin >= Arena.WALL_H) continue;
            rMin = Math.max(0, rMin);
            rMax = Math.min(Arena.WALL_H - 1, rMax);
            boolean hit = false;
            for (int la = laMin; la <= laMax && !hit; la++) {
                int idx = la + Arena.HALF;
                if (idx < 0 || idx >= Wall.LANES) continue;
                for (int r = rMin; r <= rMax; r++) if (wall.solid(idx, r)) { hit = true; break; }
            }
            if (!hit) continue;
            push(p, wall, pos, bb, moveDir);
        }
    }

    private void push(Player p, Wall wall, int pos, BoundingBox bb, int moveDir) {
        Location l = p.getLocation();
        double halfW = (wall.axisX ? bb.getWidthX() : bb.getWidthZ()) / 2.0;
        double target = moveDir > 0 ? pos + 1 + halfW + 0.02 : pos - halfW - 0.02;
        if (wall.axisX) l.setX(target); else l.setZ(target);
        p.teleport(l);
        Vector v = wall.axisX ? new Vector(moveDir * 0.45, 0.12, 0) : new Vector(0, 0.12, moveDir * 0.45);
        p.setVelocity(v);
        p.setFallDistance(0);
        p.playSound(p.getLocation(), Sound.ENTITY_SLIME_SQUISH, 0.7f, 0.8f);
    }

    /** Sound / particle cues for special walls. */
    void wallEvent(Wall wall, String what) {
        World w = arena.world();
        if (w == null) return;
        int pos = wall.pos();
        Location c = wall.axisX ? new Location(w, pos + 0.5, Arena.WALL_Y + 2, 0.5) : new Location(w, 0.5, Arena.WALL_Y + 2, pos + 0.5);
        switch (what) {
            case "sike" -> {
                w.spawnParticle(Particle.CLOUD, c, 60, wall.axisX ? 0.3 : 4, 1.2, wall.axisX ? 4 : 0.3, 0.02);
                w.playSound(c, Sound.ENTITY_CHICKEN_EGG, 1.5f, 0.6f);
                actionBarAll("§aSike! §7That one was fake.");
            }
            case "ender" -> {
                w.spawnParticle(Particle.PORTAL, c, 120, wall.axisX ? 0.3 : 4, 1.2, wall.axisX ? 4 : 0.3, 0.3);
                w.playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 1.5f, 0.8f);
                actionBarAll("§5Ender wall §7now comes from the §f" + wall.fromName() + "§7!");
            }
            case "warp" -> w.playSound(c, Sound.ENTITY_ENDERMAN_TELEPORT, 1.2f, 1.6f);
            case "morph" -> w.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 2f, 0.6f);
            case "trick" -> {
                w.spawnParticle(Particle.BLOCK, c, 160, wall.axisX ? 0.3 : 4, 1.2, wall.axisX ? 4 : 0.3, 0.1,
                        Material.SLIME_BLOCK.createBlockData());
                w.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.6f, 0.7f);
                actionBarAll("§cTrick! §7It broke apart.");
            }
            case "sideswipe-from" -> w.spawnParticle(Particle.SWEEP_ATTACK, c, 12, wall.axisX ? 0.3 : 4, 1, wall.axisX ? 4 : 0.3, 0);
            case "sideswipe" -> {
                w.spawnParticle(Particle.SWEEP_ATTACK, c, 12, wall.axisX ? 0.3 : 4, 1, wall.axisX ? 4 : 0.3, 0);
                w.playSound(c, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2f, 0.6f);
                actionBarAll("§9Sideswipe! §7It now comes from the §f" + wall.fromName() + "§7!");
            }
            case "sandwich" -> w.playSound(c, Sound.BLOCK_PISTON_EXTEND, 1.5f, 1.4f);
            case "dance" -> w.playSound(c, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 0.8f);
            case "frozen" -> w.playSound(c, Sound.BLOCK_GLASS_BREAK, 1.2f, 1.6f);
            case "returning" -> w.playSound(c, Sound.BLOCK_PISTON_CONTRACT, 1.5f, 0.6f);
            default -> { }
        }
    }

    /** Place / remove wall blocks to match the current wall positions (diff against what is in the world). */
    private void renderWalls() {
        World w = arena.world();
        Map<Long, BlockData> want = new HashMap<>();
        for (Wall wall : walls) {
            if (wall.removed) continue;
            int pos = wall.pos();
            if (Math.abs(pos) > Arena.START) continue;
            WallType capType = wall.capType();
            BlockData cap = capType == null ? PISTON_UP : capType.capMaterial.createBlockData();
            for (int li = 0; li < Wall.LANES; li++) {
                int lat = li - Arena.HALF;
                for (int r = 0; r < Arena.WALL_H; r++) {
                    byte c = wall.cells[li][r];
                    BlockData bd = switch (c) {
                        case Wall.SOLID -> SLIME;
                        case Wall.CAP -> cap;
                        // a ladder hangs on the side wall of its hole ('<' = the block at the lower lateral index)
                        case Wall.LADDER_L -> ladder(wall.axisX ? BlockFace.SOUTH : BlockFace.EAST);
                        case Wall.LADDER_R -> ladder(wall.axisX ? BlockFace.NORTH : BlockFace.WEST);
                        default -> null;
                    };
                    if (bd == null) continue;
                    int x = wall.axisX ? pos : lat, z = wall.axisX ? lat : pos;
                    want.merge(key(x, Arena.WALL_Y + r, z), bd, (a, b) -> a.getMaterial() == Material.LADDER ? b : a);
                }
            }
        }
        if (w != null) {
            for (Map.Entry<Long, BlockData> e : rendered.entrySet()) {
                if (!want.containsKey(e.getKey())) setBlock(w, e.getKey(), AIR_DATA);
            }
            for (Map.Entry<Long, BlockData> e : want.entrySet()) {
                if (!e.getValue().equals(rendered.get(e.getKey()))) setBlock(w, e.getKey(), e.getValue());
            }
        }
        rendered = want;
    }

    private static final BlockData SLIME = Material.SLIME_BLOCK.createBlockData();
    private static final BlockData AIR_DATA = Material.AIR.createBlockData();
    private static final BlockData PISTON_UP = Material.PISTON.createBlockData(d -> ((Directional) d).setFacing(BlockFace.UP));
    private static final Map<BlockFace, BlockData> LADDERS = new EnumMap<>(BlockFace.class);

    private static BlockData ladder(BlockFace facing) {
        return LADDERS.computeIfAbsent(facing, f -> Material.LADDER.createBlockData(d -> ((Directional) d).setFacing(f)));
    }

    private static long key(int x, int y, int z) {
        return ((long) (x + 512) << 32) | ((long) (y & 0xFFF) << 16) | (long) (z + 512);
    }

    private static void setBlock(World w, long k, BlockData d) {
        int x = (int) (k >>> 32) - 512;
        int y = (int) ((k >>> 16) & 0xFFF);
        int z = (int) (k & 0xFFFF) - 512;
        w.getBlockAt(x, y, z).setBlockData(d, false);
    }

    // ------------------------------------------------------------------ phases

    /** Heads-up that two walls at once begin in {@code secs} seconds (30 s, 10 s, then the last 5 s). */
    private void warnDoubleWalls(int secs) {
        if (secs >= 10) titleAll("", "§e⚠ Two walls at once in §f" + secs + "§e seconds!", 0, 50, 10);
        else actionBarAll("§c⚠ Two walls at once in §f" + secs + "§c...");
        soundAll(secs <= 5 ? Sound.BLOCK_NOTE_BLOCK_HAT : Sound.BLOCK_NOTE_BLOCK_PLING, 1f, secs <= 5 ? 1.5f : 1.0f);
    }

    private void warnShrink(int ring) {
        arena.paintRing(ring, Material.RED_TERRACOTTA);
        titleAll("", "§cThe platform is shrinking!", 0, 40, 10);
        soundAll(Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
    }

    private void shrink(int ring) {
        arena.removeRing(ring);
        platformHalf = Arena.HALF - ring - 1;
        soundAll(Sound.BLOCK_GLASS_BREAK, 1f, 0.6f);
        actionBarAll("§cThe edge crumbles away!");
    }

    private void startOverdrive() {
        overdrive = true;
        titleAll("§c§lOVERDRIVE", "§7The walls go wild!", 5, 40, 10);
        soundAll(Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.2f);
        if (ctx != null) ctx.broadcast("§c§lOVERDRIVE! §7Walls now come from every side at full speed.");
    }

    /** MCC's chat/banner cues: new wall type, wall speed up, wall difficulty up. */
    private void announceProgress() {
        if (ctx == null) return;
        List<WallType> now = unlockedTypes();
        while (announcedUnlocks < now.size()) {
            WallType t = now.get(announcedUnlocks++);
            titleAll("", "§7New Wall Type appearing: " + t.color + "§l" + t.label, 0, 40, 10);
            ctx.broadcast("§7New Wall Type appearing: " + t.color + t.label);
            soundAll(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.6f);
        }
        int lvl = level();
        if (lvl > lastLevel) {
            lastLevel = lvl;
            ctx.broadcast("§eWall speed increased! §7(" + difficultyName() + ")");
            int band = tierBand(lvl);
            if (band > lastTierBand) {
                lastTierBand = band;
                ctx.broadcast("§cWall difficulty increased!");
            }
        }
    }

    // ------------------------------------------------------------------ players

    private void refreshAlive() {
        Set<UUID> online = new HashSet<>();
        for (Player p : ctx.participants()) online.add(p.getUniqueId());
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline() || !online.contains(id)) {
                alive.remove(id);
                eliminations.add(Map.entry(id, elapsedTicks));
                if (state == State.PLAYING && ctx != null)
                    ctx.broadcast(names.getOrDefault(id, "Someone") + " §7left the game.");
            }
        }
    }

    private static String ordinalOf(int n) {
        int m = n % 100;
        if (m >= 11 && m <= 13) return n + "th";
        return n + switch (n % 10) { case 1 -> "st"; case 2 -> "nd"; case 3 -> "rd"; default -> "th"; };
    }

    private void eliminate(Player p, boolean award) {
        UUID id = p.getUniqueId();
        if (!alive.remove(id)) return;
        eliminations.add(Map.entry(id, elapsedTicks));
        unGhost(p);
        Location spec = arena.spectatorPoint();
        SpectatorGuard.makeSpectator(p, spec.getWorld() != null ? spec : null);
        int remaining = alive.size();
        titleAll(p, "§c§lKnocked off!", "§7" + remaining + " still standing");
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.7f);
        ctx.broadcast(names.getOrDefault(id, p.getName()) + " §7fell into the void! §f" + remaining + " §7remain.");
        if (award && perElimPoints > 0) {
            for (UUID other : new ArrayList<>(alive)) {
                Player op = Bukkit.getPlayer(other);
                if (op == null) continue;
                ctx.addPoints(other, perElimPoints, "Outlived " + p.getName());
            }
        }
    }

    // ------------------------------------------------------------------ ghosts (no collision, translucent to each other)

    private static final String GHOST_TEAM = "hitw_ghost";

    /**
     * Living players are invisible and share a team with "see friendly invisibles" (so they see each other as
     * translucent ghosts) and collision rule NEVER. Collision and translucency are decided by each viewer's own
     * scoreboard, and the core hands every player a fresh scoreboard each second, so this is re-applied on every
     * living player's current board a few times a second. Never setCollidable(false): on Paper that also makes
     * players untargetable by projectiles.
     */
    private void syncGhosts() {
        List<String> names = new ArrayList<>();
        List<Player> living = new ArrayList<>();
        for (UUID id : alive) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            living.add(p);
            names.add(p.getName());
            if (!p.hasPotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY))
                p.addPotionEffect(new PotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY,
                        PotionEffect.INFINITE_DURATION, 0, false, false, false));
        }
        org.bukkit.scoreboard.Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player v : living) {
            org.bukkit.scoreboard.Scoreboard sb = v.getScoreboard();
            if (sb == null || sb == main) continue;   // never touch the main scoreboard
            org.bukkit.scoreboard.Team t = sb.getTeam(GHOST_TEAM);
            if (t == null) {
                t = sb.registerNewTeam(GHOST_TEAM);
                t.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE, org.bukkit.scoreboard.Team.OptionStatus.NEVER);
                t.setCanSeeFriendlyInvisibles(true);
                t.setAllowFriendlyFire(false);
            }
            for (String n : names) if (!t.hasEntry(n)) t.addEntry(n);
            for (String n : new ArrayList<>(t.getEntries())) if (!names.contains(n)) t.removeEntry(n);
        }
    }

    /** One player stops being a ghost: effect off, name out of the team on every board. */
    private void unGhost(Player p) {
        if (p == null) return;
        p.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        org.bukkit.scoreboard.Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player v : Bukkit.getOnlinePlayers()) {
            org.bukkit.scoreboard.Scoreboard sb = v.getScoreboard();
            if (sb == null || sb == main) continue;
            org.bukkit.scoreboard.Team t = sb.getTeam(GHOST_TEAM);
            if (t != null && t.hasEntry(p.getName())) t.removeEntry(p.getName());
        }
    }

    /** Game end / stop: nobody is a ghost any more. */
    private void clearGhosts() {
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
        }
        org.bukkit.scoreboard.Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Player v : Bukkit.getOnlinePlayers()) {
            org.bukkit.scoreboard.Scoreboard sb = v.getScoreboard();
            if (sb == null || sb == main) continue;
            org.bukkit.scoreboard.Team t = sb.getTeam(GHOST_TEAM);
            if (t != null) try { t.unregister(); } catch (IllegalStateException ignored) { }
        }
    }

    // ------------------------------------------------------------------ spectators

    /** Spectators stay around the platform (the cleared build box plus some room). */
    private static final BoundingBox WATCH_BOX = new BoundingBox(-40, Arena.ELIM_Y - 15, -40, 41, Arena.WALL_Y + 40, 41);

    private SpectatorGuard.Zone spectatorZone(Player p) {
        World w = arena.world();
        if (state == State.IDLE || w == null || !p.getWorld().equals(w)) return null;
        return new SpectatorGuard.Zone(w, WATCH_BOX, arena.spectatorPoint());
    }

    /** Late joiners and rejoiners (the core no longer counts them) spectate from the overview. */
    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        if (state == State.IDLE || arena.world() == null) return false;
        SpectatorGuard.makeSpectator(player, arena.spectatorPoint());
        player.sendMessage("§a§lHole in the Wall §7is in progress — you are spectating.");
        return true;
    }

    private boolean checkEnd() {
        if (alive.isEmpty()) { endGame("Everyone fell"); return true; }
        if (initialCount >= 2 && alive.size() <= 1) { endGame("Last one standing"); return true; }
        return false;
    }

    private void endGame(String reason) {
        if (state == State.IDLE || state == State.ENDING || ctx == null) return;
        state = State.ENDING;
        if (loopTask != null) { loopTask.cancel(); loopTask = null; }
        walls.clear();
        renderWalls();
        clearGhosts();

        // placements: survivors first (tied), then in reverse elimination order grouped by tick
        List<List<UUID>> groups = new ArrayList<>();
        if (!alive.isEmpty()) groups.add(new ArrayList<>(alive));
        for (int i = eliminations.size() - 1; i >= 0; ) {
            int t = eliminations.get(i).getValue();
            List<UUID> g = new ArrayList<>();
            while (i >= 0 && eliminations.get(i).getValue() == t) { g.add(eliminations.get(i).getKey()); i--; }
            groups.add(g);
        }
        int place = 0;
        List<String> podium = new ArrayList<>();
        for (List<UUID> g : groups) {
            if (place >= PLACEMENT_BONUS.length) break;
            int sum = 0;
            for (int k = place; k < Math.min(PLACEMENT_BONUS.length, place + g.size()); k++) sum += PLACEMENT_BONUS[k];
            int each = (int) Math.round(sum / (double) g.size());   // MCC: tied players split the sum of the tied places
            StringBuilder sb = new StringBuilder();
            for (UUID id : g) {
                if (Bukkit.getPlayer(id) != null && each > 0) ctx.addPoints(id, each, "Finished in " + ordinalOf(place + 1) + " place");
                if (sb.length() > 0) sb.append("§7, ");
                sb.append(names.getOrDefault(id, "?"));
            }
            podium.add("§e#" + (place + 1) + " " + sb + " §7(+" + each + ")");
            place += g.size();
        }
        ctx.broadcast("§a§lHole in the Wall over! §7" + reason + ".");
        for (String line : podium) ctx.broadcast(line);
        String winner = groups.isEmpty() ? "Nobody" : namesOf(groups.get(0));
        titleAll("§a§l" + (groups.isEmpty() ? "Game over" : "Winner"), winner, 5, 60, 15);
        soundAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        ctx.setTimer(0, MAIN_SECONDS);

        final String r = reason;
        endReason = reason;
        endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endTask = null;
            if (state == State.ENDING) finishNow(r);
        }, FastTest.ticks(80L));
    }

    // ------------------------------------------------------------------ admin testing hooks
    // (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    private String endReason = "Hole in the Wall over";

    /** Ends the start countdown (walls start next tick) or the results screen. */
    public boolean skipWait() {
        if (ctx == null) return false;
        if (state == State.COUNTDOWN) {
            countdownTicks = 1;   // the next tick runs the normal GO transition
            return true;
        }
        if (state == State.ENDING && endTask != null) {
            endTask.cancel();
            endTask = null;
            finishNow(endReason);
            return true;
        }
        return false;
    }

    /** A single round: nothing to skip to. */
    public boolean skipRound() { return false; }

    private void finishNow(String reason) {
        if (finished) return;
        finished = true;
        McmGameContext c = ctx;
        cancelTasks();
        walls.clear();
        renderWalls();
        clearItems();
        clearGhosts();
        state = State.IDLE;
        ctx = null;
        try {
            if (c != null) c.finish(reason);
        } finally {
            rendered.clear();
            arena.clear();
        }
    }

    private void cancelTasks() {
        if (loopTask != null) { loopTask.cancel(); loopTask = null; }
        if (endTask != null) { endTask.cancel(); endTask = null; }
    }

    private void resetFields() {
        participants.clear();
        alive.clear();
        names.clear();
        spawns.clear();
        eliminations.clear();
        lastHit.clear();
        walls.clear();
        rendered.clear();
        initialCount = 0;
        perElimPoints = 0;
        elapsedTicks = 0;
        waveCooldown = 0;
        wavesCleared = 0;
        sidesNow = 1;
        platformHalf = Arena.HALF;
        overdrive = false;
        vengefulTargets.clear();
    }

    private void preparePlayer(Player p) {
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        AttributeInstance max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(max != null ? max.getValue() : 20.0);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setVelocity(new Vector());
        p.setAllowFlight(false);
        p.setFlying(false);
        p.getInventory().setHeldItemSlot(0);
        p.getInventory().setBoots(teamBoots(p));
    }

    /** Owner request: everyone wears leather boots dyed in their team colour, so ghosts can be told apart. */
    private ItemStack teamBoots(Player p) {
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        if (boots.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta) {
            ChatColor c = ctx == null ? ChatColor.WHITE : ctx.teamColor(ctx.teamOf(p.getUniqueId()));
            meta.setColor(bootColor(c));
            meta.setUnbreakable(true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_UNBREAKABLE, org.bukkit.inventory.ItemFlag.HIDE_DYE);
            boots.setItemMeta(meta);
        }
        return boots;
    }

    private static Color bootColor(ChatColor c) {
        if (c == null) return Color.WHITE;
        return switch (c) {
            case RED -> Color.fromRGB(0xFF5555);
            case DARK_RED -> Color.fromRGB(0xAA0000);
            case GOLD -> Color.fromRGB(0xFFAA00);
            case YELLOW -> Color.fromRGB(0xFFFF55);
            case GREEN -> Color.fromRGB(0x55FF55);
            case DARK_GREEN -> Color.fromRGB(0x00AA00);
            case AQUA -> Color.fromRGB(0x55FFFF);
            case DARK_AQUA -> Color.fromRGB(0x00AAAA);
            case BLUE -> Color.fromRGB(0x5555FF);
            case DARK_BLUE -> Color.fromRGB(0x0000AA);
            case LIGHT_PURPLE -> Color.fromRGB(0xFF55FF);
            case DARK_PURPLE -> Color.fromRGB(0xAA00AA);
            case GRAY -> Color.fromRGB(0xAAAAAA);
            case DARK_GRAY -> Color.fromRGB(0x555555);
            case BLACK -> Color.fromRGB(0x1D1D21);
            default -> Color.WHITE;
        };
    }

    private void clearItems() {
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.getInventory().clear();
        }
    }

    private Material padMaterial(String sideKey, List<Player> members) {
        if (sideKey == null || !sideKey.startsWith("TEAM:")) return Material.SEA_LANTERN;
        String team = sideKey.substring(5).toLowerCase(Locale.ROOT);
        return switch (team) {
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
            default -> Material.SEA_LANTERN;
        };
    }

    private String coloredName(Player p) {
        String team = ctx.teamOf(p.getUniqueId());
        ChatColor c = ctx.teamColor(team);
        return (c == null ? "§f" : c.toString()) + p.getName();
    }

    private String namesOf(List<UUID> ids) {
        StringBuilder sb = new StringBuilder();
        for (UUID id : ids) {
            if (sb.length() > 0) sb.append("§7, ");
            sb.append(names.getOrDefault(id, "?"));
        }
        return sb.toString();
    }

    private static double clampIn(double v) {
        return Math.max(-Arena.HALF + 1.5, Math.min(Arena.HALF - 0.5, v));
    }

    // ------------------------------------------------------------------ HUD helpers

    private void updateSidebar() {
        if (ctx == null) return;
        int interval = currentInterval();
        double speed = 20.0 / interval * (overdrive ? 2 : 1);
        List<String> lines = new ArrayList<>();
        lines.add("§7Difficulty §e" + (overdrive ? "§c§lOVERDRIVE" : difficultyName()));
        lines.add("§7Wall speed §b" + String.format(Locale.ROOT, "%.1f", speed) + " b/s");
        lines.add("§7Walls at once §e" + sidesNow);
        lines.add("§7Alive §a" + alive.size() + "§7/" + initialCount);
        lines.add("§7Walls survived §f" + wavesCleared);
        int size = platformHalf * 2 + 1;
        lines.add("§7Platform §f" + size + "x" + size);
        ctx.setSidebarLines(lines);
    }

    private List<Player> audience() {
        List<Player> out = new ArrayList<>();
        for (UUID id : participants) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) out.add(p);
        }
        return out;
    }

    private void titleAll(String title, String sub, int in, int stay, int out) {
        Title t = Title.title(LEGACY.deserialize(title), LEGACY.deserialize(sub),
                Title.Times.times(Duration.ofMillis(in * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(out * 50L)));
        for (Player p : audience()) p.showTitle(t);
    }

    private void titleAll(Player only, String title, String sub) {
        only.showTitle(Title.title(LEGACY.deserialize(title), LEGACY.deserialize(sub),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(2000), Duration.ofMillis(500))));
    }

    private void actionBarAll(String msg) {
        Component c = LEGACY.deserialize(msg);
        for (Player p : audience()) p.sendActionBar(c);
    }

    private void soundAll(Sound s, float vol, float pitch) {
        for (Player p : audience()) p.playSound(p.getLocation(), s, vol, pitch);
    }

    // ------------------------------------------------------------------ rules (only while running, only in our world)

    private boolean active() { return state != State.IDLE; }

    private static boolean inWorld(Entity e) {
        return e != null && e.getWorld().getName().equals(Arena.WORLD_NAME);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!active() || !inWorld(e.getEntity())) return;
        e.setCancelled(true);
        // Owner feedback: no player-vs-player hitting in this game; all damage and knockback is cancelled.
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (active() && inWorld(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (active() && inWorld(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (active() && inWorld(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (active() && inWorld(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (active() && inWorld(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(org.bukkit.event.inventory.InventoryClickEvent e) {
        if (active() && e.getWhoClicked() instanceof Player p && inWorld(p)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (active() && inWorld(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (active() && inWorld(e.getPlayer()) && e.getAction() == org.bukkit.event.block.Action.PHYSICAL) e.setCancelled(true);
    }
}
