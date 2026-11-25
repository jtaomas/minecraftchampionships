package com.mcm.games.race;

import com.mcm.api.McmBuild;
import com.mcm.api.McmFx;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.race.model.Kit;
import com.mcm.games.race.model.MapFeatures;
import com.mcm.games.race.model.TgMapData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** The Race game: 6 rounds on 6 maps, race to punch a chicken. */
final class RaceGame implements McmGame {
    static final String ENT_TAG = "tg_ent";
    static final String CHICKEN_TAG = "tg_chicken";
    static final String DECOY_TAG = "tg_decoy";

    enum Phase { IDLE, LOADING, PRE, RUNNING, INTERMISSION, RESULTS }

    /** Per-player state for one game. */
    static final class Racer {
        final UUID id;
        final String name;
        String side;
        boolean finished;
        int[] places;           // per round; 0 = not finished / not played
        int points;
        int spawnIdx;
        // karma
        double karma, protection;
        long lockUntil;
        UUID lastVictim; long lastVictimTick;
        long lastKnockTick = -1000;
        long lastRespawnTick = -1000;
        boolean glowing;
        Boat boat;
        String tempMsg; long tempUntil;
        // movement tracking (gliding knockback, lethal falls)
        Location lastLoc;
        Vector vel = new Vector();
        double airPeakY = Double.NaN;
        boolean refillPending;
        // map features (reset every round): highest checkpoint, best bonus tier per gate group, refills taken this attempt
        int checkpoint = -1;
        final Map<String, Integer> bonus = new LinkedHashMap<>();
        final Set<Integer> rings = new HashSet<>();
        long lastDepotTick = -1000;
        int roundBonus;
        Racer(UUID id, String name, int rounds) { this.id = id; this.name = name; this.places = new int[rounds]; }
    }

    private final RacePlugin plugin;
    private final Random rnd = new Random();

    private McmGameContext ctx;
    private Settings s;
    private World world;
    private Phase phase = Phase.IDLE;
    private boolean running, finishCalled;

    private final List<MapRegistry.Entry> plan = new ArrayList<>();
    private MapRegistry.Entry forcedNext;
    private int round;                     // 1-based
    private MapRegistry.Entry cur;
    private TgMapData data;
    private Map<String, String> variantChoice = Map.of();
    private final Set<Material> hazardMats = new HashSet<>();

    private final Map<UUID, Racer> racers = new LinkedHashMap<>();
    private final Map<String, List<UUID>> sideMembers = new LinkedHashMap<>();
    private final List<UUID> finishOrder = new ArrayList<>();
    private final Set<String> bonusSides = new HashSet<>();
    private final Set<Long> placed = new HashSet<>();
    private final List<Entity> spawned = new ArrayList<>();
    private final List<Chicken> chickens = new ArrayList<>();
    private final List<Chicken> decoys = new ArrayList<>();
    private final Map<UUID, Location> decoyHome = new HashMap<>();
    /** Spawn point of chickens.get(i) / decoys.get(i) (index-parallel), for re-spawning one that went missing. */
    private final List<Location> chickenAt = new ArrayList<>(), decoyAt = new ArrayList<>();
    private int chickenRespawns;
    /** Chicken punches this round (for /race info). */
    private int punchAccepted, punchRejected;
    private String lastPunch = "-";

    private BukkitTask tickTask, secondTask, countdownTask, revealTask;
    private final List<BukkitTask> delayed = new ArrayList<>();
    private FeatureRuntime fx;            // movers / toggles of the current round (null = map has none)
    private final CrashCheck crash = new CrashCheck();   // elytra crash rule (MapFeatures.crashKills, feedback 3)
    private long goTick;
    private Paster.Job job;               // paste of the current round's map (null once done)
    private Prebuilt prebuilt;            // next round's map, pasted during this round
    private long loadStarted;
    private BukkitTask waitTask;
    /** Late joiners who are not participants: confined spectators. */
    private final Set<UUID> watchers = new HashSet<>();
    private long tick;
    private int preLeft, timeLeft;
    private boolean timeSkipped;
    private long goMillis;
    /** Points handed out this round (for /mcm skip: the skipped round pays nothing). */
    private final Map<UUID, Integer> roundPts = new HashMap<>();
    private final Map<String, Integer> roundSidePts = new HashMap<>();
    private final Map<UUID, Integer> sideShare = new HashMap<>();

    RaceGame(RacePlugin plugin) { this.plugin = plugin; }

    @Override public String id() { return "race"; }
    @Override public String displayName() { return "Race"; }
    @Override public Material icon() { return Material.ELYTRA; }
    @Override public String description() { return "Elytra and boat races: cross the finish line first."; }
    @Override public boolean useCoreScoreboard() { return true; }

    boolean isRunning() { return running; }
    World world() { return world; }
    Phase phase() { return phase; }
    Settings settings() { return s; }
    TgMapData data() { return data; }
    MapRegistry.Entry current() { return cur; }
    /** Chat feedback log (core 3.2.0+ optional hook; no @Override so older cores still load us). */
    public String currentMap() { return cur == null ? null : cur.map.id(); }

    void forceNext(MapRegistry.Entry e) { forcedNext = e; }

    // ===================================================================== lifecycle

    @Override
    public void start(McmGameContext ctx) {
        if (running) stop();
        this.ctx = ctx;
        plugin.reloadConfig();
        s = Settings.load(plugin.getConfig());
        plugin.placer().baseY = s.baseY;
        plugin.placer().spacing = s.spacing;
        world = plugin.world();
        running = true;
        finishCalled = false;
        phase = Phase.LOADING;
        racers.clear(); sideMembers.clear(); placed.clear(); spawned.clear(); chickens.clear(); decoys.clear(); decoyHome.clear();
        round = 0;
        watchers.clear();
        prebuilt = null; job = null;
        plugin.applyDebug();
        if (world == null) { abort("§cRace: game world could not be created."); return; }
        if (!plugin.registry().ready()) {
            // maps are still being generated off-thread (plugin just enabled / reloaded): wait, never block a tick
            ctx.broadcast("§7Race: the maps are still being built, the game starts in a moment...");
            long t0 = System.currentTimeMillis();
            waitTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                if (!running) { waitTask.cancel(); waitTask = null; return; }
                if (plugin.registry().ready()) { waitTask.cancel(); waitTask = null; beginGame(); }
                else if (System.currentTimeMillis() - t0 > 120_000) { waitTask.cancel(); waitTask = null; abort("§cRace: maps did not finish building."); }
            }, 10L, 10L);
            return;
        }
        beginGame();
    }

    private void beginGame() {
        List<MapRegistry.Entry> pool = new ArrayList<>(plugin.registry().playable());
        List<MapRegistry.Entry> real = new ArrayList<>();
        for (MapRegistry.Entry e : pool) if (!e.stub) real.add(e);
        if (real.size() >= s.rounds) pool = real;
        // maps marked out of rotation (imported map.yml "rotation: false") are only played when forced with /race map
        List<MapRegistry.Entry> rot = new ArrayList<>();
        for (MapRegistry.Entry e : pool) if (e.map.inRotation()) rot.add(e);
        if (!rot.isEmpty()) pool = rot;
        if (pool.isEmpty()) { abort("§cRace: no playable maps are installed."); return; }
        Collections.shuffle(pool, rnd);
        if (forcedNext != null && !forcedNext.broken) { pool.remove(forcedNext); pool.add(0, forcedNext); }
        forcedNext = null;
        plan.clear();
        for (int i = 0; i < s.rounds; i++) plan.add(pool.get(i % pool.size()));
        if (pool.size() < s.rounds)
            plugin.getLogger().warning("Race: only " + pool.size() + " playable maps for " + s.rounds + " rounds; maps repeat.");

        for (Player p : ctx.participants()) {
            Racer r = new Racer(p.getUniqueId(), p.getName(), s.rounds);
            r.side = McmFx.sideKeyOf(ctx, p);
            racers.put(p.getUniqueId(), r);
        }
        for (Map.Entry<String, List<Player>> e : ctx.sides().entrySet()) {
            List<UUID> ids = new ArrayList<>();
            for (Player p : e.getValue()) ids.add(p.getUniqueId());
            sideMembers.put(e.getKey(), ids);
        }
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 1L, 1L);
        secondTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onSecond, 20L, 20L);
        ctx.broadcast("§6§lRace §7- " + s.rounds + " rounds. " + (s.finishLine ? "First across the §efinish line§7 wins!" : "Cross the map and §ewhack a chicken§7!"));
        prepareRound(1);
    }

    private void abort(String msg) {
        ctx.broadcast(msg);
        plugin.getLogger().warning(msg);
        delay(1, this::endGame);
    }

    @Override
    public void stop() {
        if (!running) return;
        running = false;
        cancelTasks();
        cleanupRound();
        plugin.paster().releaseAll();           // after the cleanup, so the chickens' chunks are still loaded for it
        resetPlayers();
        phase = Phase.IDLE;
    }

    private void cancelTasks() {
        for (BukkitTask t : new BukkitTask[]{tickTask, secondTask, countdownTask, revealTask}) if (t != null) t.cancel();
        tickTask = secondTask = countdownTask = revealTask = null;
        for (BukkitTask t : delayed) t.cancel();
        delayed.clear();
        if (waitTask != null) { waitTask.cancel(); waitTask = null; }
        if (job != null) { plugin.paster().cancel(job); job = null; }
        if (prebuilt != null) { plugin.paster().cancel(prebuilt.job); prebuilt = null; }
    }

    private void endGame() {
        if (finishCalled) return;
        finishCalled = true;
        boolean wasRunning = running;
        running = false;
        cancelTasks();
        cleanupRound();
        plugin.paster().releaseAll();
        resetPlayers();
        phase = Phase.IDLE;
        if (wasRunning || ctx != null) ctx.finish("Race finished");
    }

    private void delay(long ticks, Runnable r) {
        BukkitTask[] ref = new BukkitTask[1];
        ref[0] = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            delayed.remove(ref[0]);
            if (running) r.run();
        }, ticks);
        delayed.add(ref[0]);
    }

    // ===================================================================== rounds

    /** A map pasted ahead of its round (into its own slot, while the current round is played). */
    private static final class Prebuilt {
        final MapRegistry.Entry entry; final TgMapData data; final Map<String, String> choice; final Paster.Job job;
        Prebuilt(MapRegistry.Entry entry, TgMapData data, Map<String, String> choice, Paster.Job job) {
            this.entry = entry; this.data = data; this.choice = choice; this.job = job;
        }
    }

    /**
     * The map data for an entry: the validated fixed-seed build, or (random-seeds) a fresh build on the worker
     * thread. {@code then} runs on the main thread; with null data if the map could not be built at all.
     */
    private void obtainData(MapRegistry.Entry e, java.util.function.Consumer<TgMapData> then) {
        if (!s.randomSeeds && e.cached != null) { then.accept(e.cached); return; }
        long seed = rnd.nextLong();
        plugin.worker().execute(() -> {
            TgMapData d;
            try { d = e.map.build(seed); }
            catch (Throwable t) { plugin.getLogger().severe("Race: map " + e.map.id() + " failed to build: " + t); d = e.cached; }
            TgMapData out = d;
            Bukkit.getScheduler().runTask(plugin, () -> then.accept(out));
        });
    }

    private void prepareRound(int n) {
        if (!running) return;
        round = n;
        if (forcedNext != null && !forcedNext.broken) { plan.set(n - 1, forcedNext); forcedNext = null; }
        cur = plan.get(n - 1);
        phase = Phase.LOADING;
        for (Racer r : racers.values()) {
            r.finished = false; r.karma = 0; r.protection = 0; r.lockUntil = 0;
            r.airPeakY = Double.NaN; r.lastLoc = null; r.vel = new Vector(); r.refillPending = false;
            r.checkpoint = -1; r.bonus.clear(); r.rings.clear(); r.roundBonus = 0;
        }
        punchAccepted = punchRejected = 0;
        lastPunch = "-";
        finishOrder.clear();
        bonusSides.clear();
        roundPts.clear(); roundSidePts.clear(); sideShare.clear();
        timeSkipped = false;
        loadStarted = System.currentTimeMillis();
        ctx.setSidebarLines(sidebar());
        ctx.setTimer(s.preRoundSeconds, s.preRoundSeconds);
        Prebuilt pb = prebuilt;
        prebuilt = null;
        if (pb != null && pb.entry == cur && !pb.job.cancelled) {
            data = pb.data;
            variantChoice = pb.choice;
            job = pb.job;
            onDataReady(n, true);
            return;
        }
        if (pb != null) plugin.paster().cancel(pb.job);
        obtainData(cur, d -> {
            if (!running || round != n || phase != Phase.LOADING) return;
            if (d == null) { abort("§cRace: map " + cur.map.displayName() + " could not be built."); return; }
            data = d;
            variantChoice = Variants.pick(data, rnd);
            job = plugin.paster().paste("round " + n + " " + cur.map.id(), world, cur.slot, data, variantChoice);
            onDataReady(n, false);
        });
    }

    /** Data chosen and its paste started (or finished, if it was pre-built): start the round once every block is set. */
    private void onDataReady(int n, boolean prebuiltUsed) {
        hazardMats.clear();
        hazardMats.add(Material.LAVA);
        for (String h : data.hazardBlocks) {
            String b = h; int i = b.indexOf('['); if (i > 0) b = b.substring(0, i);
            Material m = Material.matchMaterial(b);
            if (m != null) hazardMats.add(m);
        }
        if (!variantChoice.isEmpty())
            plugin.getLogger().info("Race round " + n + " " + cur.map.id() + " variants: " + Variants.summary(data, variantChoice));
        Paster.Job j = job;
        if (prebuiltUsed)
            plugin.getLogger().info("Race round " + n + " " + cur.map.id() + ": using the map pre-built during the last round ("
                    + (j.done ? "ready" : Math.round(j.progress() * 100) + "% done, waiting") + ")");
        j.then(() -> {
            if (!running || round != n || phase != Phase.LOADING || job != j) return;
            job = null;
            if (j.failed) { abort("§cRace: map " + cur.map.displayName() + " could not be pasted."); return; }
            plugin.getLogger().info("Race round " + n + " " + cur.map.id() + ": map ready " + (System.currentTimeMillis() - loadStarted) + " ms after the round was called");
            beginPre();
        });
    }

    /** Paste the next round's map now, into its own slot, while this round is played (no visible wait later). */
    private void prebuildNext() {
        if (round >= plan.size() || prebuilt != null) return;
        MapRegistry.Entry nx = plan.get(round);
        if (nx == cur || nx.slot == cur.slot || nx.broken) return;    // same slot: can only be pasted after this round
        int r = round;
        obtainData(nx, d -> {
            if (!running || round != r || d == null || prebuilt != null || plan.get(r) != nx) return;
            Map<String, String> choice = Variants.pick(d, rnd);
            prebuilt = new Prebuilt(nx, d, choice, plugin.paster().paste("pre-build round " + (r + 1) + " " + nx.map.id(), world, nx.slot, d, choice));
        });
    }
    private void beginPre() {
        phase = Phase.PRE;
        plugin.paster().release(Set.of(cur.slot));     // older slots' chunks may unload now; this map's stay loaded
        preLeft = fastTest() ? 1 : s.preRoundSeconds;
        spawnChickens();
        MapFeatures mf = cur.map.features();
        if (mf != null && !mf.isEmpty()) {
            fx = new FeatureRuntime(world, cur.slot, mf, plugin.placer(), data, spawned);
            fx.begin();
        }
        List<Player> online = racingPlayers();
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < data.startSpawns.size(); i++) idx.add(i);
        Collections.shuffle(idx, rnd);
        int k = 0;
        for (Player p : online) {
            Racer r = racers.get(p.getUniqueId());
            r.spawnIdx = idx.isEmpty() ? 0 : idx.get(k++ % idx.size());
            placeAtStart(p, r, r.spawnIdx, true);
        }
        Kit kit = cur.map.kit();
        if (fastTest()) {
            McmFx.title(online, "§6§l" + cur.map.displayName(), "", 0, 20, 0);
            countdownTask = McmFx.countdown(plugin, online, 1, "§7Chaos begins in...", () -> { countdownTask = null; go(); });
        } else McmFx.titleCard(online, "§6§l" + cur.map.displayName(), "§7Round " + round + "/" + s.rounds + " §8- §e" + cur.map.type().label());
        ctx.broadcast("§6Round " + round + "/" + s.rounds + ": §e§l" + cur.map.displayName() + " §7(" + cur.map.type().label() + ") §8- §7" + kitText(kit, online.isEmpty() ? null : online.get(0))
                + (cur.map.credit() != null ? " §8(Map by §7" + cur.map.credit() + "§8)" : ""));
        ctx.setTimer(preLeft, s.preRoundSeconds);
        ctx.setSidebarLines(sidebar());
    }

    /**
     * Round announcement of the kit, counted from what a racer actually holds after {@link #giveKit} (base kit +
     * the map's extraItems), so it can't drift from the real inventory.
     */
    private String kitText(Kit kit, Player sample) {
        Map<Material, Integer> have = new LinkedHashMap<>();
        if (sample != null) {
            for (ItemStack it : sample.getInventory().getContents())
                if (it != null && !it.getType().isAir()) have.merge(it.getType(), it.getAmount(), Integer::sum);
        } else {
            // nobody online to look at: same computation as giveKit
            if (kit.bridging()) { have.put(Material.WHITE_CONCRETE, s.concrete); have.put(s.pickaxe, 1); }
            if (kit != Kit.ELYTRA_FIREWORKS && s.snowballs > 0 && s.raceSnowballs) have.put(Material.SNOWBALL, s.snowballs);
            if (kit == Kit.ELYTRA_FIREWORKS) { have.put(Material.ELYTRA, 1); if (s.fireworks > 0) have.put(Material.FIREWORK_ROCKET, s.fireworks); }
            for (String x : data.extraItems) { ItemStack it = parseItem(x); if (it != null) have.merge(it.getType(), it.getAmount(), Integer::sum); }
        }
        List<String> parts = new ArrayList<>();
        if (kit == Kit.BOAT) parts.add("You start in a boat");
        if (kit == Kit.ELYTRA_FIREWORKS || have.containsKey(Material.ELYTRA)) parts.add("Elytra");
        int concrete = 0;
        for (Iterator<Map.Entry<Material, Integer>> it = have.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Material, Integer> e = it.next();
            if (e.getKey().name().endsWith("_CONCRETE")) { concrete += e.getValue(); it.remove(); }
        }
        if (concrete > 0) parts.add((s.infiniteBlocks ? "endless" : String.valueOf(concrete)) + " team concrete");
        have.remove(Material.ELYTRA);
        for (Map.Entry<Material, Integer> e : have.entrySet()) parts.add(itemText(e.getKey(), e.getValue()));
        String list = parts.isEmpty() ? "No items" : String.join(", ", parts);
        String tail = switch (kit) {
            case CONCRETE_PICKAXE_SNOWBALLS -> " Break rival bridges!";
            case SNOWBALLS_ONLY -> " No building on this one.";
            case BOAT -> "";
            case ELYTRA_FIREWORKS -> " The floor drops " + (s.elytraFloorDelaySeconds > 0 ? s.elytraFloorDelaySeconds + " s after GO - shove!" : "at GO!");
        };
        String fall = cur.map.lethalFallHeight() > 0 ? " §cBig falls only end well in water!" : "";
        MapFeatures mf = cur.map.features();
        StringBuilder ft = new StringBuilder();
        if (mf != null && !mf.isEmpty()) {
            if (!mf.refills.isEmpty()) ft.append(" §6Gold rings refill rockets.");
            if (!mf.depots.isEmpty()) ft.append(" §bLost your boat? Depots give a fresh one.");
            if (!mf.checkpoints.isEmpty()) ft.append(" §bCheckpoints save your progress.");
            if (!mf.gates.isEmpty() && s.bonusEnabled) ft.append(" §aLime = easy§7, §6orange = medium +" + s.tierPoints(1) + "§7, §cred = hard +" + s.tierPoints(2)
                    + " §7per section (best per section, max " + s.bonusCap + ", paid if you finish)!");
            if (mf.crashKills && s.crashKills) ft.append(" §cCrashing into a wall or a hard landing sends you back to your checkpoint!");
            if (!mf.movers.isEmpty() || !mf.toggles.isEmpty()) ft.append(" §cMind the moving traps.");
        }
        return list + "." + tail + fall + ft;
    }

    private static String itemText(Material m, int n) {
        String name = switch (m) {
            case FIREWORK_ROCKET -> "firework";
            case SNOWBALL -> "snowball";
            default -> m.name().toLowerCase(Locale.ROOT).replace('_', ' ');
        };
        if (m.getMaxStackSize() == 1 && n == 1) return name;
        return n + " " + name + (n == 1 || name.endsWith("s") ? "" : "s");
    }

    /** Round length: the map's own {@code roundSeconds()} if it sets one (imported maps), else config round-seconds. */
    private int roundSecs() {
        int m = cur == null ? 0 : cur.map.roundSeconds();
        return m > 0 ? m : s.roundSeconds;
    }

    private void go() {
        if (!running || phase != Phase.PRE) return;
        phase = Phase.RUNNING;
        timeLeft = roundSecs();
        goMillis = System.currentTimeMillis();
        goTick = tick;
        int ox = plugin.placer().ox(cur.slot), oy = plugin.placer().oy(), oz = plugin.placer().oz(cur.slot);
        for (TgMapData.Pos p : data.startBarrier) world.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setType(Material.AIR, false);
        // elytra maps: the gantry floor stays a few seconds after GO so racers can punch each other off it
        boolean holdFloor = cur.map.kit() == Kit.ELYTRA_FIREWORKS && s.elytraFloorDelaySeconds > 0 && !data.dropFloor.isEmpty();
        if (holdFloor) {
            int r = round;
            TgMapData d = data;
            delay(s.elytraFloorDelaySeconds * 20L, () -> { if (phase == Phase.RUNNING && round == r && data == d) dropFloor(); });
        } else dropFloor();
        if (!data.startBarrier.isEmpty()) {
            TgMapData.Pos p = data.startBarrier.get(data.startBarrier.size() / 2);
            McmFx.soundAt(new Location(world, ox + p.x(), oy + p.y(), oz + p.z()), Sound.BLOCK_GLASS_BREAK, 2f, 0.8f);
        } else if (data.startBarrier.isEmpty() && data.dropFloor.isEmpty()) {
            plugin.getLogger().warning("Race map " + cur.map.id() + " has no start barrier; players were only held by the strip check.");
        }
        ctx.setTimer(timeLeft, roundSecs());
        ctx.setSidebarLines(sidebar());
        prebuildNext();
    }

    private void dropFloor() {
        if (data.dropFloor.isEmpty()) return;
        int ox = plugin.placer().ox(cur.slot), oy = plugin.placer().oy(), oz = plugin.placer().oz(cur.slot);
        for (TgMapData.Pos p : data.dropFloor) world.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setType(Material.AIR, false);
        McmFx.sound(racingPlayers(), Sound.BLOCK_PISTON_CONTRACT, 1f, 0.6f);
    }

    private void endRound(String why) {
        if (!running || (phase != Phase.RUNNING && phase != Phase.PRE)) return;
        phase = Phase.INTERMISSION;
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        List<Player> online = participants();
        McmFx.title(online, "§e§lRound over", "§7" + why, 5, 40, 10);
        McmFx.sound(online, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 0.7f);
        StringBuilder sb = new StringBuilder("§6Round " + round + " results §8(" + cur.map.displayName() + ")§7:");
        for (int i = 0; i < Math.min(5, finishOrder.size()); i++) {
            Racer r = racers.get(finishOrder.get(i));
            sb.append("\n §e").append(ordinal(i + 1)).append(" §f").append(r.name);
            if (r.roundBonus > 0) sb.append(" §6+").append(r.roundBonus).append(" line");
        }
        List<String> bl = new ArrayList<>();
        for (UUID id : finishOrder) { Racer r = racers.get(id); if (r != null && r.roundBonus > 0) bl.add(r.name + " +" + r.roundBonus); }
        if (!bl.isEmpty()) sb.append("\n §6Line bonus: §e").append(String.join("§7, §e", bl));
        int dnf = 0;
        for (Player p : online) { Racer r = racers.get(p.getUniqueId()); if (r != null && !r.finished) dnf++; }
        if (finishOrder.isEmpty()) sb.append("\n §7Nobody made it to the other side.");
        if (dnf > 0) sb.append("\n §8").append(dnf).append(" did not finish (0 points)");
        ctx.broadcast(sb.toString());
        for (Player p : online) {
            Racer r = racers.get(p.getUniqueId());
            if (r == null) continue;
            if (p.isInsideVehicle()) p.leaveVehicle();
            p.getInventory().clear();
            p.setGlowing(false);
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            r.glowing = false;
            if (p.getWorld().equals(world)) p.setGameMode(GameMode.SPECTATOR);
        }
        cleanupRound();
        ctx.setSidebarLines(sidebar());
        long wait = fastTest() ? 10L : s.intermissionSeconds * 20L;
        if (round < plan.size()) delay(wait, () -> prepareRound(round + 1));
        else delay(wait, this::finalResults);
    }

    private void finalResults() {
        phase = Phase.RESULTS;
        Map<String, Integer> sidePts = new HashMap<>();
        for (Racer r : racers.values()) sidePts.merge(r.side, r.points, Integer::sum);
        List<String> order = new ArrayList<>(sidePts.keySet());
        order.sort((a, b) -> Integer.compare(sidePts.get(b), sidePts.get(a)));
        List<String> det = new ArrayList<>();
        for (String k : order) det.add(sidePts.get(k) + " pts");
        List<Racer> top = new ArrayList<>(racers.values());
        top.sort((a, b) -> Integer.compare(b.points, a.points));
        StringBuilder sb = new StringBuilder("§6§lRace - final §7(points this game):");
        for (int i = 0; i < Math.min(5, top.size()); i++)
            sb.append("\n §e#").append(i + 1).append(" §f").append(top.get(i).name).append(" §7- §a").append(top.get(i).points);
        ctx.broadcast(sb.toString());
        for (Player p : participants()) {
            Racer r = racers.get(p.getUniqueId());
            if (r != null) p.sendMessage("§7Your rounds: " + placesText(r) + " §8| §7points: §a" + r.points);
        }
        if (fastTest()) { delay(10, this::endGame); return; }
        revealTask = McmFx.placementReveal(plugin, ctx, order, det, () -> { revealTask = null; endGame(); });
    }

    // ===================================================================== players

    private List<Player> participants() {
        List<Player> out = new ArrayList<>();
        if (ctx == null) return out;
        for (Player p : ctx.participants()) if (p.isOnline() && racers.containsKey(p.getUniqueId())) out.add(p);
        return out;
    }

    /** Online participants in the game world (racing or not). */
    private List<Player> racingPlayers() {
        return participants();
    }

    private void placeAtStart(Player p, Racer r, int spawnIdx, boolean first) {
        TgMapData.Spawn sp = data.startSpawns.get(Math.floorMod(spawnIdx, data.startSpawns.size()));
        List<TgMapData.Spawn> bs = data.boatSpawns.isEmpty() ? data.startSpawns : data.boatSpawns;
        placeAt(p, r, sp, bs.get(Math.floorMod(spawnIdx, bs.size())), first);
    }

    /** Put a racer at {@code sp} (boat maps: seated in a fresh boat at {@code boatSp}) with a fresh kit. */
    private void placeAt(Player p, Racer r, TgMapData.Spawn sp, TgMapData.Spawn boatSp, boolean first) {
        if (p.isInsideVehicle()) p.leaveVehicle();
        removeBoat(r);
        Location l = plugin.placer().loc(world, cur.slot, sp);
        p.setFallDistance(0f);
        p.setVelocity(new Vector());
        p.setFireTicks(0);
        r.airPeakY = Double.NaN;
        r.lastLoc = null;
        r.vel = new Vector();
        Kit kit = cur.map.kit();
        if (first) {
            p.setGameMode(kit.bridging() ? GameMode.SURVIVAL : GameMode.ADVENTURE);
            p.setAllowFlight(false);
            p.setFlying(false);
            p.setHealth(Math.max(1, maxHealth(p)));
            p.setFoodLevel(20);
            p.setSaturation(20f);
            for (PotionEffect pe : p.getActivePotionEffects()) p.removePotionEffect(pe.getType());
            p.setGlowing(false);
            r.glowing = false;
        } else if (p.getGameMode() != GameMode.SURVIVAL && p.getGameMode() != GameMode.ADVENTURE) {
            p.setGameMode(kit.bridging() ? GameMode.SURVIVAL : GameMode.ADVENTURE);
        }
        // Must stay collidable: on CraftBukkit/Paper setCollidable(false) also makes a living entity un-pickable,
        // so projectiles (our snowballs) fly straight through it. Player collision is already off via the
        // scoreboard team COLLISION_RULE NEVER (ScoreTeams), which vanilla push logic honours on server and client.
        p.setCollidable(true);
        if (p.isGliding()) p.setGliding(false);
        if (kit == Kit.BOAT) {
            seatInBoat(p, r, boatSp == null ? sp : boatSp);
        } else {
            p.teleport(l);
        }
        giveKit(p, kit);
    }

    /** Fresh boat at {@code b}, racer seated in it (their previous boat is removed). */
    private void seatInBoat(Player p, Racer r, TgMapData.Spawn b) {
        if (p.isInsideVehicle()) p.leaveVehicle();
        removeBoat(r);
        Location bl = plugin.placer().loc(world, cur.slot, b);
        p.teleport(bl);
        Boat boat = world.spawn(bl, Boat.class, e -> {
            e.addScoreboardTag(ENT_TAG);
            e.setPersistent(false);
        });
        spawned.add(boat);
        r.boat = boat;
        boat.addPassenger(p);
    }

    private static double maxHealth(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return a == null ? 20 : a.getValue();
    }

    private void giveKit(Player p, Kit kit) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(null);
        switch (kit) {
            case CONCRETE_PICKAXE_SNOWBALLS -> {
                ChatColor c = McmFx.sideColor(ctx, McmFx.sideKeyOf(ctx, p));
                Material concrete = McmFx.concreteOf(c);
                int left = s.concrete, slot = 0;
                while (left > 0 && slot < 9) {
                    int n = Math.min(64, left);
                    inv.setItem(slot == 1 ? 3 : slot, new ItemStack(concrete, n));
                    left -= n; slot = slot == 0 ? 3 : slot + 1;
                }
                inv.setItem(1, unbreakable(new ItemStack(s.pickaxe)));
                giveSnowballs(inv, 2);
            }
            case SNOWBALLS_ONLY, BOAT -> {
                giveSnowballs(inv, 0);
            }
            case ELYTRA_FIREWORKS -> {
                inv.setChestplate(unbreakable(new ItemStack(Material.ELYTRA)));
                if (s.fireworks > 0) inv.setItem(0, new ItemStack(Material.FIREWORK_ROCKET, s.fireworks));
            }
        }
        for (String x : data.extraItems) {
            ItemStack it = parseItem(x);
            if (it != null) inv.addItem(it);
        }
        inv.setHeldItemSlot(0);
        p.updateInventory();
    }

    /** kit.snowballs snowballs: a stack of 16 in the kit slot, the rest in the next free slots (snowballs stack to 16). */
    private void giveSnowballs(PlayerInventory inv, int slot) {
        if (!s.raceSnowballs) return;   // Race: no snowballs by default (race.snowballs)
        int left = s.snowballs;
        if (left <= 0) return;
        inv.setItem(slot, new ItemStack(Material.SNOWBALL, Math.min(16, left)));
        left -= Math.min(16, left);
        while (left > 0) { int n = Math.min(16, left); inv.addItem(new ItemStack(Material.SNOWBALL, n)); left -= n; }
    }

    private ItemStack parseItem(String spec) {
        if (spec == null || spec.isBlank()) return null;
        String sp = spec.trim();
        int count = 1;
        int last = sp.lastIndexOf(':');
        if (last > 0) {
            try { count = Integer.parseInt(sp.substring(last + 1)); sp = sp.substring(0, last); } catch (NumberFormatException ignored) {}
        }
        Material m = Material.matchMaterial(sp);
        if (m == null || !m.isItem()) { plugin.getLogger().warning("Race: unknown extra item '" + spec + "'"); return null; }
        return unbreakable(new ItemStack(m, Math.max(1, Math.min(m.getMaxStackSize(), count))));
    }

    private static ItemStack unbreakable(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null && it.getType().getMaxDurability() > 0) { meta.setUnbreakable(true); it.setItemMeta(meta); }
        return it;
    }

    private void removeBoat(Racer r) {
        if (r.boat != null) {
            for (Entity e : r.boat.getPassengers()) r.boat.removePassenger(e);
            r.boat.remove();
            spawned.remove(r.boat);
            r.boat = null;
        }
    }

    /** Void / hazard / out of bounds: back to the start with the starting kit. */
    void respawn(Player p, boolean hazard) { respawn(p, hazard, null); }

    /** @param fallLine chat line for a lethal fall (null = a void/hazard pun). */
    private void respawn(Player p, boolean hazard, String fallLine) {
        Racer r = racers.get(p.getUniqueId());
        if (r == null || data == null) return;
        if (tick - r.lastRespawnTick < 5) return;
        r.lastRespawnTick = tick;
        r.rings.clear();                        // a new attempt: refills can be taken again
        MapFeatures.Checkpoint cp = phase == Phase.RUNNING ? checkpointOf(r) : null;
        if (cp != null && !cp.spawns().isEmpty()) {
            TgMapData.Spawn sp = cp.spawns().get(rnd.nextInt(cp.spawns().size()));
            TgMapData.Spawn bs = cp.boats().isEmpty() ? sp : cp.boats().get(rnd.nextInt(cp.boats().size()));
            placeAt(p, r, sp, bs, false);
        } else {
            int idx = rnd.nextInt(Math.max(1, data.startSpawns.size()));
            placeAtStart(p, r, idx, false);
        }
        if (phase == Phase.RUNNING) {
            String name = McmFx.sideColor(ctx, r.side) + p.getName() + "§7";
            if (fallLine != null) {
                ctx.broadcast("§8☠ " + name + " " + fallLine);
                if (cur.map.lethalFallHeight() > 0) p.sendMessage("§cYou missed the water! §7A fall that big only ends well in water. Back to the start.");
            } else {
                List<String> lines = hazard ? s.punsHazard : s.punsVoid;
                String line = lines.get(rnd.nextInt(lines.size()));
                ctx.broadcast("§8☠ §7" + (line.contains("%s") ? String.format(line, name) : name + " " + line));
            }
            McmFx.sound(p, Sound.ENTITY_CHICKEN_HURT, 1f, 0.8f);
            temp(r, cp != null ? "§cBack to checkpoint §f" + cp.name() + "§c! §7Kit refilled." : "§cBack to the start! §7Kit refilled.");
        }
    }

    /**
     * Lethal falls (map rule {@code lethalFallHeight() > 0}): tracks the highest point since the racer last stood,
     * swam, climbed, glided or rode; landing on a non-water block after a drop of at least {@code height} blocks
     * fails the racer (respawn at the start). Never deals damage.
     */
    private void checkFall(Player p, Racer r, double y, int height) {
        boolean reset = p.isInWater() || p.isGliding() || p.isInsideVehicle() || p.isClimbing() || p.isFlying()
                || p.hasPotionEffect(PotionEffectType.LEVITATION) || p.hasPotionEffect(PotionEffectType.SLOW_FALLING);
        if (reset) { r.airPeakY = Double.NaN; return; }
        @SuppressWarnings("deprecation") boolean ground = p.isOnGround();
        if (!ground) {
            if (Double.isNaN(r.airPeakY) || y > r.airPeakY) r.airPeakY = y;
            return;
        }
        double peak = r.airPeakY;
        r.airPeakY = Double.NaN;
        if (Double.isNaN(peak)) return;
        double drop = peak - y;
        if (drop < height) return;
        Block under = p.getLocation().clone().add(0, -0.2, 0).getBlock();
        if (under.getType() == Material.WATER || p.getLocation().getBlock().getType() == Material.WATER) return;
        plugin.getLogger().info("Race lethal fall: " + p.getName() + " fell " + String.format("%.1f", drop)
                + " blocks onto " + under.getType() + " (limit " + height + ") -> back to start");
        respawn(p, false, "§7fell §c" + (int) Math.round(drop) + " blocks §7and missed the water.");
    }

    private MapFeatures.Checkpoint checkpointOf(Racer r) {
        if (fx == null || r.checkpoint < 0) return null;
        MapFeatures.Checkpoint best = null;
        for (MapFeatures.Checkpoint c : fx.features().checkpoints) if (c.order() == r.checkpoint) best = c;
        return best;
    }

    /** Line bonus a racer has earned this round (best tier per group, capped at bonus.cap). */
    private int bonusTotal(Racer r) {
        int t = 0;
        for (int tier : r.bonus.values()) t += s.tierPoints(tier);
        return Math.min(s.bonusCap, t);
    }

    private String bonusText(Racer r) {
        List<String> l = new ArrayList<>();
        for (Map.Entry<String, Integer> e : r.bonus.entrySet()) l.add(e.getKey() + " " + TIER_NAME[Math.min(3, e.getValue())]);
        return String.join(", ", l) + (bonusTotal(r) >= s.bonusCap ? ", capped at " + s.bonusCap : "");
    }

    /** Does the segment a-b (local) pass through the box (block-inclusive region)? */
    private static boolean through(TgMapData.Region b, double x0, double y0, double z0, double x1, double y1, double z1) {
        if (Math.max(x0, x1) < b.minX() || Math.min(x0, x1) >= b.maxX() + 1 || Math.max(y0, y1) < b.minY() || Math.min(y0, y1) >= b.maxY() + 1
                || Math.max(z0, z1) < b.minZ() || Math.min(z0, z1) >= b.maxZ() + 1) return false;
        double len = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        int n = Math.max(1, (int) Math.ceil(len / 0.25));
        for (int k = 0; k <= n; k++) {
            double u = k / (double) n;
            if (b.contains(x0 + (x1 - x0) * u, y0 + (y1 - y0) * u, z0 + (z1 - z0) * u)) return true;
        }
        return false;
    }

    /** Gliding racers: does their (0.6 x 0.6 x 0.6) hitbox, shrunk by 0.05, touch a hazard block (fire ring, lava curtain)? */
    private boolean boxTouches(Player p) {
        org.bukkit.util.BoundingBox bb = p.getBoundingBox().expand(-0.05);
        for (int x = (int) Math.floor(bb.getMinX()); x <= (int) Math.floor(bb.getMaxX()); x++)
            for (int y = (int) Math.floor(bb.getMinY()); y <= (int) Math.floor(bb.getMaxY()); y++)
                for (int z = (int) Math.floor(bb.getMinZ()); z <= (int) Math.floor(bb.getMaxZ()); z++)
                    if (hazardMats.contains(world.getBlockAt(x, y, z).getType())) return true;
        return false;
    }

    private static final String[] TIER_COL = {"§a", "§6", "§c", "§5"};
    private static final String[] TIER_NAME = {"easy", "medium", "hard", "hard"};

    /**
     * Movers, checkpoints, refills, bonus gates and depots for one racer this tick (running, not finished).
     * Returns true if the racer was sent back (hit by a mover).
     */
    private boolean featureStep(Player p, Racer r, Location prev, Location now) {
        MapFeatures mf = fx.features();
        double off = p.isGliding() ? 0.3 : p.isInsideVehicle() ? 0.5 : 0.9;
        double x1 = lx(now.getX()), y1 = ly(now.getY()) + off, z1 = lz(now.getZ());
        double x0 = x1, y0 = y1, z0 = z1;
        if (prev != null && prev.getWorld() == now.getWorld() && prev.distanceSquared(now) < 100) {
            x0 = lx(prev.getX()); y0 = ly(prev.getY()) + off; z0 = lz(prev.getZ());
        }
        if (mf.crashKills && s.crashKills && !p.isInsideVehicle()) {
            boolean fresh = prev == null || tick - r.lastRespawnTick < 10 || tick - goTick < 10;
            String why = crash.step(p, r.vel, fresh, s, mf, new double[]{x1, ly(now.getY()), z1});
            if (why != null) {
                McmFx.soundAt(now, Sound.ENTITY_PLAYER_BIG_FALL, 1.5f, 0.8f);
                McmFx.burst(now.clone().add(0, 0.5, 0), Color.RED, 16);
                p.sendMessage("§c✖ Crash! §7You " + why + ". §7Fly through the openings - walls, nose dives and big falls send you back.");
                respawn(p, true, "§7" + why + "§7.");
                return true;
            }
        }
        if (!mf.movers.isEmpty()) {
            double rad = p.isGliding() ? 0.35 : 0.4;
            MapFeatures.Mover m = fx.hit(x0, y0, z0, x1, y1, z1, rad);
            if (m == null && !p.isGliding()) m = fx.hit(x0, y0 + 0.8, z0, x1, y1 + 0.8, z1, rad);
            if (m != null) {
                McmFx.soundAt(now, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.5f, 0.6f);
                McmFx.burst(now.clone().add(0, 0.5, 0), Color.RED, 16);
                respawn(p, true, "§7was flattened by the §c" + m.name() + "§7.");
                return true;
            }
        }
        for (MapFeatures.Checkpoint c : mf.checkpoints)
            if (c.order() > r.checkpoint && through(c.trigger(), x0, y0, z0, x1, y1, z1)) {
                r.checkpoint = c.order();
                temp(r, "§b✔ Checkpoint: §f" + c.name());
                McmFx.sound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.4f);
            }
        for (int i = 0; i < mf.refills.size(); i++) {
            MapFeatures.Refill rf = mf.refills.get(i);
            if (r.rings.contains(i) || !through(rf.box(), x0, y0, z0, x1, y1, z1)) continue;
            r.rings.add(i);
            p.getInventory().addItem(new ItemStack(Material.FIREWORK_ROCKET, rf.rockets()));
            temp(r, "§6+" + rf.rockets() + " rockets §7(" + rf.name() + ")");
            McmFx.sound(p, Sound.ENTITY_ITEM_PICKUP, 1f, 0.8f);
        }
        for (MapFeatures.BonusGate g : mf.gates) {
            if (!through(g.box(), x0, y0, z0, x1, y1, z1)) continue;
            int had = r.bonus.getOrDefault(g.group(), 0);
            int tier = Math.min(3, g.tier());
            if (g.tier() <= had) {   // this section is already paid at this tier or better: still say so
                temp(r, TIER_COL[tier] + "✔ " + TIER_NAME[tier] + " gate §7(" + g.group() + " already paid) §8| §7bonus §f" + bonusTotal(r) + "/" + s.bonusCap);
                continue;
            }
            int before = bonusTotal(r);
            r.bonus.put(g.group(), g.tier());
            int after = bonusTotal(r);
            temp(r, TIER_COL[tier] + "§l+" + (after - before) + " " + TIER_NAME[tier] + " line §7(" + g.group() + ") §8| §7bonus §f" + after + "/" + s.bonusCap
                    + " §8(paid at the finish)");
            McmFx.sound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 0.7f + 0.25f * tier);
        }
        if (cur.map.kit() == Kit.BOAT && !p.isInsideVehicle() && tick - r.lastDepotTick > 20)
            for (MapFeatures.Depot d : mf.depots)
                if (d.area().contains(x1, ly(now.getY()) + 0.1, z1)) {
                    r.lastDepotTick = tick;
                    seatInBoat(p, r, d.boat());
                    temp(r, "§bFresh boat §7(" + d.name() + ")");
                    McmFx.sound(p, Sound.ENTITY_BOAT_PADDLE_WATER, 1f, 1f);
                    break;
                }
        return false;
    }

    /** Is any racer (or their boat) inside this world box? (toggles never set a solid block into an occupied cell) */
    private boolean occupied(org.bukkit.util.BoundingBox box) {
        for (Player p : participants()) {
            if (!p.getWorld().equals(world)) continue;
            if (p.getBoundingBox().overlaps(box)) return true;
            if (p.getVehicle() != null && p.getVehicle().getBoundingBox().overlaps(box)) return true;
        }
        return false;
    }

    private void temp(Racer r, String msg) { r.tempMsg = msg; r.tempUntil = tick + 40; }

    /**
     * Any punch on a Race chicken (goal or decoy), from the listener. Decides, logs why, and finishes the racer
     * when it is a real goal chicken. {@code via} = which event saw it ("attack" or "damage").
     */
    void chickenPunched(Player p, Chicken c, String via) {
        boolean decoy = isDecoy(c);
        Racer r = racers.get(p.getUniqueId());
        String why;
        if (decoy) {
            why = "decoy chicken (does nothing)";
            Long last = decoyMsgTick.get(p.getUniqueId());
            if (last == null || tick - last > 10) {
                decoyMsgTick.put(p.getUniqueId(), tick);
                p.sendMessage(s.decoyMessage);
                McmFx.soundAt(c.getLocation(), Sound.ENTITY_CHICKEN_AMBIENT, 1.4f, 1.8f);
                McmFx.sound(p, Sound.ENTITY_VILLAGER_NO, 0.8f, 1.3f);
            }
        }
        else if (!running) why = "no game running";
        else if (phase != Phase.RUNNING) why = "round not running (phase " + phase + ")";
        else if (r == null) why = "not a participant";
        else if (r.finished) why = "already finished";
        else if (!p.getWorld().equals(world)) why = "not in the game world";
        else if (p.getGameMode() == GameMode.SPECTATOR) why = "spectator";
        else why = null;
        String where = c.isValid() && data != null && data.pen != null
                ? (data.pen.contains(lx(c.getLocation().getX()), ly(c.getLocation().getY()), lz(c.getLocation().getZ())) ? "in pen" : "OUTSIDE pen")
                : "?";
        if (why != null) {
            punchRejected++;
            lastPunch = p.getName() + " rejected: " + why;
            if (s != null && s.logPunches)
                plugin.getLogger().info("Race chicken punch by " + p.getName() + " (" + p.getGameMode() + ", via " + via + ", "
                        + where + ") REJECTED: " + why);
            return;
        }
        punchAccepted++;
        lastPunch = p.getName() + " accepted (" + ordinal(finishOrder.size() + 1) + ")";
        if (s.logPunches)
            plugin.getLogger().info("Race chicken punch by " + p.getName() + " (" + p.getGameMode() + ", via " + via + ", "
                    + where + ") ACCEPTED -> finishes " + ordinal(finishOrder.size() + 1) + " on " + cur.map.id());
        whack(p, c);
    }

    /** Finishes a racer (goal chicken punched). Use {@link #chickenPunched} from listeners. */
    void whack(Player p, Chicken chicken) {
        if (!running || phase != Phase.RUNNING) return;
        Racer r = racers.get(p.getUniqueId());
        if (r == null || r.finished) return;
        r.finished = true;
        finishOrder.add(r.id);
        int place = finishOrder.size();
        long ms = System.currentTimeMillis() - goMillis;
        int pts = s.placementPoints(place) + s.top10Bonus(place);
        r.places[round - 1] = place;
        r.points += pts;
        ctx.addPoints(r.id, pts, "Race " + ordinal(place) + " place");
        roundPts.merge(r.id, pts, Integer::sum);
        int bonus = fx == null ? 0 : bonusTotal(r);
        r.roundBonus = bonus;
        if (bonus > 0) {
            r.points += bonus;
            ctx.addPoints(r.id, bonus, "Race line bonus");
            roundPts.merge(r.id, bonus, Integer::sum);
            p.sendMessage("§6Line bonus: §e+" + bonus + " §7(" + bonusText(r) + ")");
        }
        String col = place == 1 ? "§6" : place == 2 ? "§f" : place == 3 ? "§c" : "§e";
        McmFx.title(p, col + "§l" + ordinal(place), (chicken != null ? "§c§lWhack! §7" : "§a§lFinish! §7") + time(ms) + " §8| §a+" + pts + (bonus > 0 ? " §6+" + bonus + " line" : ""), 2, 40, 10);
        McmFx.sound(p, place <= 3 ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        Location cl = chicken != null ? chicken.getLocation() : p.getLocation();
        if (chicken != null) McmFx.soundAt(cl, Sound.ENTITY_CHICKEN_HURT, 1.5f, 1.2f);
        McmFx.soundAt(cl, chicken != null ? Sound.ENTITY_PLAYER_ATTACK_CRIT : Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
        McmFx.burst(cl.clone().add(0, 0.5, 0), Color.WHITE, 24);
        McmFx.teamFirework(cl.clone().add(0, 1.5, 0), McmFx.sideColor(ctx, r.side), place <= 3);
        if (chicken != null) spawnWhackText(cl.clone().add(0, 1.3, 0));
        ctx.broadcast("§a✔ " + McmFx.sideColor(ctx, r.side) + p.getName() + (chicken != null ? " §7whacked a chicken! §e" : " §7crossed the line! §e") + ordinal(place)
                + " place §8(" + time(ms) + ", +" + pts + (bonus > 0 ? " §6+" + bonus + " line bonus§8" : "") + ")");
        // to spectator, confined to the map
        if (p.isInsideVehicle()) p.leaveVehicle();
        removeBoat(r);
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setGlowing(false);
        r.glowing = false;
        p.removePotionEffect(PotionEffectType.SLOWNESS);
        p.setGameMode(GameMode.SPECTATOR);
        checkTeamBonus();
        ctx.setSidebarLines(sidebar());
        checkRoundEnd();
    }

    private void spawnWhackText(Location l) {
        TextDisplay td = world.spawn(l, TextDisplay.class, e -> {
            e.text(McmFx.text("§c§lWhack!"));
            e.setBillboard(Display.Billboard.CENTER);
            e.addScoreboardTag(ENT_TAG);
            e.setPersistent(false);
        });
        spawned.add(td);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { td.remove(); spawned.remove(td); }, 30L);
    }

    private void checkTeamBonus() {
        if (bonusSides.size() >= s.teamBonus.size()) return;
        Set<UUID> online = new HashSet<>();
        for (Player p : participants()) online.add(p.getUniqueId());
        for (Map.Entry<String, List<UUID>> e : sideMembers.entrySet()) {
            if (bonusSides.contains(e.getKey())) continue;
            int on = 0; boolean all = true;
            for (UUID u : e.getValue()) {
                if (!online.contains(u)) continue;
                on++;
                Racer r = racers.get(u);
                if (r == null || !r.finished) { all = false; break; }
            }
            if (!all || on < 2) continue;
            int bonus = s.teamBonus.get(bonusSides.size());
            bonusSides.add(e.getKey());
            if (bonus <= 0) continue;
            // MCC team pool "shared amongst team members": each online member gets the MCC share (pool / 4)
            int share = (int) Math.round(ctx.teamShare(bonus, on));
            int total = share * on;
            ctx.addSidePoints(e.getKey(), total, "Race team finish bonus");
            roundSidePts.merge(e.getKey(), total, Integer::sum);
            for (UUID u : e.getValue()) { Racer r = racers.get(u); if (r != null && online.contains(u)) { r.points += share; sideShare.merge(u, share, Integer::sum); } }
            ctx.broadcast("§b★ " + McmFx.sideLabel(ctx, e.getKey()) + " §7all made it! §bTeam bonus +" + bonus + " §8(split)");
            if (bonusSides.size() >= s.teamBonus.size()) return;
        }
    }

    private void checkRoundEnd() {
        if (phase != Phase.RUNNING) return;
        List<Player> on = participants();
        boolean any = false;
        for (Player p : on) { Racer r = racers.get(p.getUniqueId()); if (r != null && !r.finished) { any = true; break; } }
        if (!any) delay(10, () -> endRound(on.isEmpty() ? "Everyone left" : "Everyone made it!"));
    }

    void onQuit(Player p) {
        if (!running) return;
        Racer r = racers.get(p.getUniqueId());
        if (r != null) removeBoat(r);
        p.setGlowing(false);
        p.setCollidable(true);
        // participants() drops the player after this event; re-check next tick
        delay(2, () -> {
            if (phase == Phase.RUNNING) { checkTeamBonus(); checkRoundEnd(); }
            if (participants().isEmpty() && (phase == Phase.PRE || phase == Phase.RUNNING)) endRound("Everyone left");
        });
    }

    // ===================================================================== chickens & cleanup

    /**
     * Goal chickens and decoys. The pen can be far outside every player's view distance (Kite Festival: ~350 blocks
     * from the gantry, Frozen Canal: ~120), so the chunks of every chicken spot get their own plugin ticket for the
     * whole round ({@link Paster#hold}, released with the slot) before anything is spawned, and the chickens are
     * persistent (a non-persistent entity is discarded when its chunk unloads - the cause of the empty Kite pen).
     * {@link #checkChickens} re-spawns any that still go missing.
     */
    private void spawnChickens() {
        chickenAt.clear(); decoyAt.clear(); chickenRespawns = 0;
        if (s.finishLine) return;   // Race: the finish is a line / gate volume (TgMapData.pen), no chickens
        if (data.chickenSpawns.isEmpty()) return;
        // owner (feedback 3): the finish is a whole row of chickens - one chicken on every spot of the row
        int n = Math.min(40, Math.max(data.chickenSpawns.size(), Math.min(s.chickenCount, 40)));
        for (int i = 0; i < n; i++) chickenAt.add(plugin.placer().loc(world, cur.slot, data.chickenSpawns.get(i % data.chickenSpawns.size())));
        int bad = 0;
        for (int[] dp : decoySpots()) {
            if (dp == null || dp.length < 3) { bad++; continue; }
            Location l = plugin.placer().loc(world, cur.slot, dp[0] + 0.5, dp[1], dp[2] + 0.5);
            l.setYaw(rnd.nextFloat() * 360f - 180f);
            decoyAt.add(l);
        }
        Set<Long> held = new HashSet<>();
        int unloaded = 0;
        List<Location> all = new ArrayList<>(chickenAt); all.addAll(decoyAt);
        for (Location l : all)
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {   // the spot's chunk and its neighbours (wandering)
                int cx = (l.getBlockX() >> 4) + dx, cz = (l.getBlockZ() >> 4) + dz;
                if (!held.add((((long) cx) << 32) | (cz & 0xFFFFFFFFL))) continue;
                if (!world.isChunkLoaded(cx, cz)) unloaded++;
                plugin.paster().hold(world, cur.slot, cx, cz);
            }
        if (unloaded > 0) plugin.getLogger().info("Race round " + round + " " + cur.map.id() + ": loaded " + unloaded + " chicken chunks that were not loaded");
        for (int i = 0; i < chickenAt.size(); i++) chickens.add(spawnGoal(chickenAt.get(i)));
        for (int i = 0; i < decoyAt.size(); i++) decoys.add(spawnDecoy(decoyAt.get(i)));
        if (bad > 0) plugin.getLogger().warning("Race map " + cur.map.id() + ": " + bad + " decoy chicken entries are not {x,y,z}");
        int ok = 0;
        for (Chicken c : chickens) if (c.isValid()) ok++;
        if (ok < chickens.size()) plugin.getLogger().warning("Race round " + round + " " + cur.map.id() + ": only " + ok + "/" + chickens.size() + " goal chickens are valid right after spawning");
        if (s.logPunches)
            plugin.getLogger().info("Race round " + round + " " + cur.map.id() + ": spawned " + chickens.size() + " goal chickens"
                    + (decoys.isEmpty() ? "" : " + " + decoys.size() + " decoys") + " (" + held.size() + " chunks held)");
    }

    /** Decoy feet blocks for this round (local). */
    private List<int[]> decoySpots() {
        if (!s.decoysEnabled) return List.of();   // owner (feedback 3): no fakes - decoys off by default
        List<int[]> l = cur.map.decoyChickens();
        if (l != null && !l.isEmpty()) return l;
        if (!cur.map.scatterDecoys()) return List.of();
        return decoyCache.computeIfAbsent(data, d -> com.mcm.games.race.model.DecoySpots.pick(d, com.mcm.games.race.model.DecoySpots.DEFAULT_COUNT, d.seed));
    }
    private final Map<TgMapData, List<int[]>> decoyCache = new java.util.WeakHashMap<>();
    private final Map<UUID, Long> decoyMsgTick = new HashMap<>();

    private Chicken spawnGoal(Location l) {
        Chicken c = world.spawn(l, Chicken.class, e -> {
            e.addScoreboardTag(ENT_TAG);
            e.addScoreboardTag(CHICKEN_TAG);
            e.setInvulnerable(true);
            e.setPersistent(true);              // survives a chunk unload; removed by tag at round end / on load
            e.setRemoveWhenFarAway(false);
            e.setCollidable(false);
            e.setAdult();
            e.setAgeLock(true);
            e.setAI(s.chickenWander);
            e.setSilent(false);
            e.customName(McmFx.text(s.chickenName));
            e.setCustomNameVisible(true);
        });
        spawned.add(c);
        return c;
    }

    private Chicken spawnDecoy(Location l) {
        Chicken c = world.spawn(l, Chicken.class, e -> {
            e.addScoreboardTag(ENT_TAG);
            e.addScoreboardTag(DECOY_TAG);
            e.setInvulnerable(true);
            e.setPersistent(true);
            e.setRemoveWhenFarAway(false);
            e.setCollidable(false);
            e.setAdult();
            e.setAgeLock(true);
            e.setAI(true);                      // wanders; put back home when further than goal.decoy-leash-radius
            e.setSilent(false);
            e.customName(McmFx.text(s.chickenName));   // same "Punch me!" tag as the real ones (owner, playtest 2)
            e.setCustomNameVisible(true);
        });
        decoyHome.put(c.getUniqueId(), l);
        spawned.add(c);
        return c;
    }

    /** Once a second (pre-round and running): re-spawn any goal chicken or decoy that is gone, with a warning. */
    private void checkChickens() {
        if (world == null || cur == null) return;
        for (int i = 0; i < chickens.size() && i < chickenAt.size(); i++) {
            Chicken c = chickens.get(i);
            if (c.isValid()) continue;
            Location at = chickenAt.get(i);
            boolean loaded = world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4);
            plugin.getLogger().warning("Race round " + round + " " + cur.map.id() + ": goal chicken " + i + " is gone (dead=" + c.isDead()
                    + ", chunk loaded=" + loaded + "); re-spawning it in the pen");
            if (!loaded) plugin.paster().hold(world, cur.slot, at.getBlockX() >> 4, at.getBlockZ() >> 4);
            spawned.remove(c);
            chickens.set(i, spawnGoal(at));
            chickenRespawns++;
        }
        for (int i = 0; i < decoys.size() && i < decoyAt.size(); i++) {
            Chicken c = decoys.get(i);
            if (c.isValid()) continue;
            Location at = decoyAt.get(i);
            boolean loaded = world.isChunkLoaded(at.getBlockX() >> 4, at.getBlockZ() >> 4);
            plugin.getLogger().warning("Race round " + round + " " + cur.map.id() + ": decoy " + i + " is gone (chunk loaded=" + loaded + "); re-spawning it");
            if (!loaded) plugin.paster().hold(world, cur.slot, at.getBlockX() >> 4, at.getBlockZ() >> 4);
            spawned.remove(c);
            decoyHome.remove(c.getUniqueId());
            decoys.set(i, spawnDecoy(at));
            chickenRespawns++;
        }
    }

    /** A chunk of our world loaded its entities: drop Race entities left over from an earlier round (persistent chickens). */
    void onEntitiesLoad(List<Entity> list) {
        for (Entity e : list)
            if (e.getScoreboardTags().contains(ENT_TAG) && !spawned.contains(e)) e.remove();
    }

    private void cleanupRound() {
        if (fx != null) { fx.stop(); fx = null; }
        crash.clear();
        for (Entity e : new ArrayList<>(spawned)) if (e != null && e.isValid()) e.remove();
        spawned.clear();
        chickens.clear();
        decoys.clear();
        decoyHome.clear();
        chickenAt.clear(); decoyAt.clear();
        flying.clear(); flyingLast.clear(); flyingSince.clear(); snowDone.clear();
        for (Racer r : racers.values()) r.boat = null;
        if (world != null) {
            for (long k : placed) {
                Block b = world.getBlockAt(TgMapData.SparseBlocks.kx(k), TgMapData.SparseBlocks.ky(k), TgMapData.SparseBlocks.kz(k));
                if (b.getType().name().endsWith("_CONCRETE")) b.setType(Material.AIR, false);
            }
            for (Entity e : world.getEntities()) {
                if (e instanceof Player) continue;
                if (e.getScoreboardTags().contains(ENT_TAG) || e instanceof org.bukkit.entity.Item
                        || e instanceof org.bukkit.entity.Projectile || e instanceof Boat) e.remove();
            }
        }
        placed.clear();
    }

    private void resetPlayers() {
        if (ctx == null) return;
        List<Player> all = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) if (racers.containsKey(p.getUniqueId()) || (world != null && p.getWorld().equals(world))) all.add(p);
        for (Player p : all) {
            if (p.isInsideVehicle()) p.leaveVehicle();
            p.setGlowing(false);
            p.setCollidable(true);
            p.removePotionEffect(PotionEffectType.SLOWNESS);
            if (world != null && p.getWorld().equals(world)) {
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                if (p.isGliding()) p.setGliding(false);
            }
        }
        ScoreTeams.clear(Bukkit.getOnlinePlayers());
    }

    // ===================================================================== blocks

    static long key(Block b) { return TgMapData.SparseBlocks.key(b.getX(), b.getY(), b.getZ()); }

    /** May this player place a block here? (bridging maps, racing, concrete, inside build bounds, into air) */
    boolean canPlace(Player p, Block b, Material type, Material replaced) {
        if (phase != Phase.RUNNING || data == null || !cur.map.kit().bridging()) return false;
        Racer r = racers.get(p.getUniqueId());
        if (r == null || r.finished) return false;
        if (!type.name().endsWith("_CONCRETE")) return false;
        if (!(replaced == Material.AIR || replaced == Material.CAVE_AIR || replaced == Material.VOID_AIR)) return false;
        TgMapData.Region box = data.buildBounds != null ? data.buildBounds : data.playBounds;
        return box == null || box.contains(lx(b.getX() + 0.5), ly(b.getY() + 0.5), lz(b.getZ() + 0.5));
    }

    void onPlaced(Player p, Block b) {
        placed.add(key(b));
        if (s.infiniteBlocks) scheduleRefill(p, b.getType());
    }

    /** Infinite bridging blocks: top the player's concrete back up to kit.concrete (one pending refill per player). */
    private void scheduleRefill(Player p, Material m) {
        Racer r = racers.get(p.getUniqueId());
        if (r == null || r.refillPending) return;
        r.refillPending = true;
        UUID id = p.getUniqueId();
        delay(s.refillDelayTicks, () -> {
            r.refillPending = false;
            Player q = Bukkit.getPlayer(id);
            if (q != null && q.isOnline() && isRacing(q) && cur.map.kit().bridging()) refill(q, m);
        });
    }

    /** Counts every stack of {@code m} (main inventory + off hand) so a refill never adds a second full stack. */
    private void refill(Player p, Material m) {
        PlayerInventory inv = p.getInventory();
        int total = 0;
        for (int i = 0; i < 36; i++) { ItemStack it = inv.getItem(i); if (it != null && it.getType() == m) total += it.getAmount(); }
        ItemStack off = inv.getItemInOffHand();
        if (off.getType() == m) total += off.getAmount();
        int need = s.concrete - total;
        if (need <= 0) return;
        if (off.getType() == m && off.getAmount() < 64) {
            int add = Math.min(need, 64 - off.getAmount());
            off.setAmount(off.getAmount() + add);
            inv.setItemInOffHand(off);
            need -= add;
        }
        for (int i = 0; i < 36 && need > 0; i++) {
            ItemStack it = inv.getItem(i);
            if (it == null || it.getType() != m || it.getAmount() >= 64) continue;
            int add = Math.min(need, 64 - it.getAmount());
            it.setAmount(it.getAmount() + add);
            inv.setItem(i, it);
            need -= add;
        }
        // stack used up completely: put it back in its kit slot (0, then 3, 4, ...) or the first free slot
        int[] pref = {0, 3, 4, 5, 6, 7, 8};
        for (int i = 0; i < pref.length && need > 0; i++) {
            ItemStack it = inv.getItem(pref[i]);
            if (it != null && !it.getType().isAir()) continue;
            int n = Math.min(64, need);
            inv.setItem(pref[i], new ItemStack(m, n));
            need -= n;
        }
        if (need > 0) inv.addItem(new ItemStack(m, Math.min(64, need)));
    }

    /** Only player-placed concrete can be broken (anyone's), only while racing. */
    boolean canBreak(Player p, Block b) {
        if (phase != Phase.RUNNING || data == null || !cur.map.kit().bridging()) return false;
        Racer r = racers.get(p.getUniqueId());
        if (r == null || r.finished) return false;
        return placed.contains(key(b));
    }

    void onBroken(Block b) { placed.remove(key(b)); }

    double lx(double wx) { return plugin.placer().lx(cur.slot, wx); }
    double ly(double wy) { return plugin.placer().ly(wy); }
    double lz(double wz) { return plugin.placer().lz(cur.slot, wz); }

    // ===================================================================== combat & karma

    boolean isRacing(Player p) {
        if (!running || phase != Phase.RUNNING) return false;
        Racer r = racers.get(p.getUniqueId());
        return r != null && !r.finished && p.getWorld().equals(world) && p.getGameMode() != GameMode.SPECTATOR;
    }

    boolean isParticipant(Player p) { return racers.containsKey(p.getUniqueId()); }
    boolean isFinished(Player p) { Racer r = racers.get(p.getUniqueId()); return r != null && r.finished; }

    /** A racer punched another racer. Returns false if punching is locked by karma. */
    /** The finish volume: the map's goal region grown by 1 block each way (a fast flyer or boat cannot skip it in one tick... mostly; see README). */
    private static TgMapData.Region finishVolume(TgMapData.Region pen) {
        return TgMapData.Region.of(pen.minX() - 1, pen.minY() - 1, pen.minZ() - 1, pen.maxX() + 1, pen.maxY() + 1, pen.maxZ() + 1);
    }

    void punch(Player attacker, Player victim) {
        if (!s.punching) return;   // Race: punching off by default (race.punching)
        Racer a = racers.get(attacker.getUniqueId()), v = racers.get(victim.getUniqueId());
        if (a == null || v == null) return;
        if (s.kEnabled && a.lockUntil > tick) {
            temp(a, "§4Karma lock! §7You can't punch for " + Math.max(1, (a.lockUntil - tick + 19) / 20) + " s.");
            McmFx.sound(attacker, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return;
        }
        if (tick - v.lastKnockTick < s.hitCooldownTicks) return;
        // karma: punching a high-karma player gives none; repeat victims give more
        if (s.kEnabled && v.karma < s.kGlowFrom) {
            double gain = s.kPerPunch;
            if (victim.getUniqueId().equals(a.lastVictim) && tick - a.lastVictimTick <= s.kSameSeconds * 20L) gain += s.kSameBonus;
            a.karma += gain;
        }
        a.lastVictim = victim.getUniqueId();
        a.lastVictimTick = tick;
        if (s.kEnabled && a.karma >= s.kLockFrom && a.lockUntil <= tick) {
            a.lockUntil = tick + s.kLockSeconds * 20L;
            temp(a, "§4Karma lock! §7Too much punching - " + s.kLockSeconds + " s cooldown.");
            McmFx.sound(attacker, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
        }
        float cd = attacker.getAttackCooldown();
        double h = s.kbPunchH * (cd < 0.9f ? 0.7 : 1.0) + (attacker.isSprinting() && cd >= 0.9f ? s.kbSprint : 0);
        double yaw = Math.toRadians(attacker.getLocation().getYaw());
        Vector dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        knock(victim, v, dir, h, s.kbPunchV);
        if (s.kEnabled) v.protection = Math.min(s.protMax, v.protection + s.protPerHit);
    }

    // ------------------------------------------------------------------ snowballs

    /** Racers' snowballs in flight: id -> snowball, and its position at the previous sweep. */
    private final Map<UUID, org.bukkit.entity.Snowball> flying = new HashMap<>();
    private final Map<UUID, Vector> flyingLast = new HashMap<>();
    private final Map<UUID, Long> flyingSince = new HashMap<>();
    /** Snowballs whose hit was already decided (the three detection paths may all see the same one). */
    private final Set<UUID> snowDone = new HashSet<>();

    void trackSnowball(org.bukkit.entity.Snowball sb) {
        if (!running) return;
        flying.put(sb.getUniqueId(), sb);
        flyingLast.put(sb.getUniqueId(), sb.getLocation().toVector());
        flyingSince.put(sb.getUniqueId(), tick);
    }

    /**
     * Our own hit test, every tick: the segment each racer snowball moved since the last tick against every other
     * racer's (slightly grown) hitbox. Does not depend on vanilla projectile picking or on a damage event.
     */
    private void sweepSnowballs() {
        if (flying.isEmpty()) return;
        List<Player> targets = new ArrayList<>();
        for (Player p : participants()) if (isRacing(p)) targets.add(p);
        for (java.util.Iterator<Map.Entry<UUID, org.bukkit.entity.Snowball>> it = flying.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, org.bukkit.entity.Snowball> en = it.next();
            UUID id = en.getKey();
            org.bukkit.entity.Snowball sb = en.getValue();
            Vector last = flyingLast.get(id);
            if (!sb.isValid() || snowDone.contains(id) || tick - flyingSince.getOrDefault(id, tick) > 200 || !sb.getWorld().equals(world)) {
                it.remove(); flyingLast.remove(id); flyingSince.remove(id);
                continue;
            }
            Vector now = sb.getLocation().toVector();
            flyingLast.put(id, now);
            if (last == null) continue;
            Vector seg = now.clone().subtract(last);
            double len = seg.length();
            UUID shooter = sb.getShooter() instanceof Player sp ? sp.getUniqueId() : null;
            for (Player p : targets) {
                if (p.getUniqueId().equals(shooter)) continue;
                org.bukkit.util.BoundingBox bb = p.getBoundingBox().expand(0.3);
                boolean hit = len < 1e-4 ? bb.contains(now) : bb.rayTrace(last, seg.clone().multiply(1 / len), len) != null;
                if (!hit) continue;
                snowballHit(sb, p, "sweep");
                sb.remove();
                break;
            }
        }
    }

    /** A snowball touched a player (from the hit event, the damage event or the sweep). Decides, logs, knocks back. */
    void snowballHit(org.bukkit.entity.Snowball sb, Player victim, String via) {
        if (!snowDone.add(sb.getUniqueId())) return;
        Player shooter = sb.getShooter() instanceof Player sp ? sp : null;
        Racer v = racers.get(victim.getUniqueId());
        String why;
        if (!running || phase != Phase.RUNNING) why = "round not running (phase " + phase + ")";
        else if (shooter != null && shooter.equals(victim)) why = "own snowball";
        else if (shooter != null && !isRacing(shooter)) why = "thrower not racing (finished / spectator / not a participant)";
        else if (v == null) why = "victim not a participant";
        else if (!isRacing(victim)) why = "victim not racing (finished / spectator)";
        else why = null;
        String who = (shooter == null ? "?" : shooter.getName()) + " -> " + victim.getName();
        if (why != null) {
            if (s != null && s.logSnowballs) plugin.getLogger().info("Race snowball " + who + " (via " + via + ") REJECTED: " + why);
            return;
        }
        Vector dir = sb.getVelocity().clone().setY(0);
        if (dir.lengthSquared() < 1e-6) {
            Vector last = flyingLast.get(sb.getUniqueId());
            if (last != null) dir = sb.getLocation().toVector().subtract(last).setY(0);
        }
        if (dir.lengthSquared() < 1e-6 && shooter != null)
            dir = victim.getLocation().toVector().subtract(shooter.getLocation().toVector()).setY(0);
        String mode = victim.isInsideVehicle() ? "boat" : victim.isGliding() ? "gliding" : "on foot";
        if (s.logSnowballs) plugin.getLogger().info("Race snowball " + who + " (via " + via + ", " + mode + ") ACCEPTED");
        knock(victim, v, dir, s.kbSnowH, s.kbSnowV);
        if (s.kEnabled) v.protection = Math.min(s.protMax, v.protection + s.protPerHit);
        McmFx.soundAt(victim.getLocation(), Sound.BLOCK_SNOW_BREAK, 1f, 1.2f);
        if (s.kEnabled && s.snowKarma && shooter != null) {
            Racer a = racers.get(shooter.getUniqueId());
            if (a != null && v.karma < s.kGlowFrom) {
                a.karma += s.kPerPunch;
                if (a.karma >= s.kLockFrom && a.lockUntil <= tick) {
                    a.lockUntil = tick + s.kLockSeconds * 20L;
                    temp(a, "§4Karma lock! §7Too much punching - " + s.kLockSeconds + " s cooldown.");
                }
            }
        }
    }

    private void knock(Player victim, Racer v, Vector dirIn, double h, double up) {
        if (dirIn.lengthSquared() < 1e-6) return;
        Vector dir = dirIn.clone().normalize();
        double mult = takenMult(v);
        Entity veh = victim.getVehicle();
        if (victim.isGliding() && veh == null) {
            // Elytra: the server does not know a glider's real velocity (the client flies it), and setVelocity
            // replaces it. Add the shove to the measured flight velocity instead of wiping it out.
            Vector fly = v.vel.clone();
            if (fly.lengthSquared() > 9) fly.normalize().multiply(3);
            double gm = s.glideKbMult;
            victim.setVelocity(new Vector(fly.getX() + dir.getX() * h * mult * gm,
                    fly.getY() + up * 0.6 * Math.min(1.25, mult),
                    fly.getZ() + dir.getZ() * h * mult * gm));
            v.lastKnockTick = tick;
            victim.playHurtAnimation(0);
            McmFx.soundAt(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1f);
            return;
        }
        Vector cur = victim.getVelocity();
        Vector out = new Vector(cur.getX() * 0.5 + dir.getX() * h * mult,
                Math.min(0.45, Math.max(cur.getY() * 0.5, 0) + up * Math.min(1.25, mult)),
                cur.getZ() * 0.5 + dir.getZ() * h * mult);
        if (veh != null) veh.setVelocity(out.clone().setY(Math.min(0.2, out.getY())));
        else victim.setVelocity(out);
        v.lastKnockTick = tick;
        victim.playHurtAnimation(0);
        McmFx.soundAt(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1f);
    }

    private double takenMult(Racer v) {
        if (!s.kEnabled) return 1.0;          // owner (playtest 2): karma off = always full knockback
        double m = 1.0;
        if (v.karma > s.kMoreFrom) m = Math.min(s.kMaxMult, 1 + s.kPerKarma * (v.karma - s.kMoreFrom));
        return m * (1 - Math.max(0, Math.min(0.95, v.protection)));
    }

    private String karmaText(Racer r) {
        if (r.lockUntil > tick) return "§4Locked";
        if (r.karma >= s.kGlowFrom) return "§cMenace";
        if (r.karma > s.kMoreFrom) return "§eRowdy";
        return "§aCalm";
    }

    // ===================================================================== ticks

    private void onTick() {
        if (!running) return;
        tick++;
        if (data == null || cur == null) return;
        List<Player> on = participants();
        if (tick % 5 == 0) {
            List<String> names = new ArrayList<>();
            Set<String> red = new HashSet<>();
            for (Racer r : racers.values()) { names.add(r.name); if (r.glowing) red.add(r.name); }
            for (Chicken c : chickens) names.add(c.getUniqueId().toString());
            for (Chicken c : decoys) names.add(c.getUniqueId().toString());
            List<Player> viewers = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getWorld().equals(world)) viewers.add(p);
            ScoreTeams.sync(viewers, names, red);
        }
        if (phase != Phase.PRE && phase != Phase.RUNNING && phase != Phase.INTERMISSION) return;
        int lethalFall = cur.map.lethalFallHeight();
        if (phase == Phase.RUNNING) sweepSnowballs();
        if (phase == Phase.RUNNING && fx != null) {
            List<Location> near = new ArrayList<>();
            for (Player p : on) if (p.getWorld().equals(world) && p.getGameMode() != GameMode.SPECTATOR) near.add(p.getLocation());
            fx.tick(tick - goTick, near, this::occupied);
        }
        for (UUID w : watchers) {
            Player p = Bukkit.getPlayer(w);
            if (p != null && p.isOnline() && p.getWorld().equals(world) && p.getGameMode() == GameMode.SPECTATOR) confine(p);
        }
        for (Player p : on) {
            if (!p.getWorld().equals(world)) continue;
            Racer r = racers.get(p.getUniqueId());
            Location l = p.getLocation();
            double x = lx(l.getX()), y = ly(l.getY()), z = lz(l.getZ());
            // measured velocity (blocks/tick) for gliding knockback
            Location prevLoc = r.lastLoc;
            if (r.lastLoc != null && r.lastLoc.getWorld() == l.getWorld() && r.lastLoc.distanceSquared(l) < 64)
                r.vel = l.toVector().subtract(r.lastLoc.toVector());
            else r.vel = new Vector();
            r.lastLoc = l;
            if (p.getGameMode() == GameMode.SPECTATOR) { confine(p); continue; }
            if (phase == Phase.PRE) {
                boolean out = data.startStrip != null && !data.startStrip.contains(x, y, z) && !p.isInsideVehicle();
                if (out || y < data.voidY) placeAtStart(p, r, r.spawnIdx, false);
                continue;
            }
            if (phase != Phase.RUNNING || r.finished) continue;
            if (y < data.voidY || (data.playBounds != null && !data.playBounds.contains(x, y, z))) { respawn(p, false); continue; }
            // Race finish: crossing the finish line / gate volume (the map's pen region, 1 block of slack all round)
            if (s.finishLine && data.pen != null && finishVolume(data.pen).contains(x, y, z)) { whack(p, null); continue; }
            boolean hz = false;
            for (TgMapData.Region hr : data.hazardRegions) if (hr.contains(x, y, z)) { hz = true; break; }
            if (!hz && !hazardMats.isEmpty()) {
                Block feet = l.getBlock();
                hz = hazardMats.contains(feet.getType()) || hazardMats.contains(l.clone().add(0, -0.2, 0).getBlock().getType());
            }
            if (!hz && p.isGliding() && !hazardMats.isEmpty()) hz = boxTouches(p);
            if (hz) { respawn(p, true); continue; }
            if (fx != null && featureStep(p, r, prevLoc, l)) continue;
            if (lethalFall > 0) checkFall(p, r, y, lethalFall);
            if (p.getFireTicks() > 0) p.setFireTicks(0);
        }
    }

    /** Spectators stay inside the current map's play box (clamped back to its nearest point). */
    private void confine(Player p) {
        if (data == null || data.playBounds == null) return;
        Location l = p.getLocation();
        double x = lx(l.getX()), y = ly(l.getY()), z = lz(l.getZ());
        if (data.playBounds.contains(x, y, z)) return;
        TgMapData.Region b = data.playBounds;
        double cx = Math.max(b.minX() + 0.5, Math.min(b.maxX() + 0.5, x));
        double cy = Math.max(b.minY() + 0.5, Math.min(b.maxY() + 0.5, y));
        double cz = Math.max(b.minZ() + 0.5, Math.min(b.maxZ() + 0.5, z));
        p.teleport(plugin.placer().loc(world, cur.slot, cx, cy, cz).setDirection(l.getDirection()));
    }

    /** May a spectator teleport (spectate menu) to {@code to}? Only inside the current map's play box. */
    boolean spectatorMayGo(Location to) {
        if (to == null || !inWorld(to.getWorld())) return false;
        if (data == null || data.playBounds == null || cur == null
                || (phase != Phase.PRE && phase != Phase.RUNNING && phase != Phase.INTERMISSION)) return true;
        return data.playBounds.contains(lx(to.getX()), ly(to.getY()), lz(to.getZ()));
    }

    boolean isWatcher(Player p) { return watchers.contains(p.getUniqueId()); }

    /**
     * Core hook: a player joined mid-game. A participant who had not finished this round races again from the map
     * start (fresh kit) while the round is in its pre-round or running phase; everyone else becomes a spectator at
     * the current map's overview point, confined to its play box. Between rounds (map still loading) the core default
     * is used; participants are placed automatically when the next round starts.
     */
    @Override
    public boolean onLateJoin(Player p, McmGameContext c) {
        if (!running || data == null || cur == null || world == null) return false;
        if (phase != Phase.PRE && phase != Phase.RUNNING && phase != Phase.INTERMISSION) return false;
        Racer r = racers.get(p.getUniqueId());
        if (r != null && !r.finished && (phase == Phase.PRE || phase == Phase.RUNNING)) {
            placeAtStart(p, r, rnd.nextInt(Math.max(1, data.startSpawns.size())), true);
            p.sendMessage("§aWelcome back! §7You rejoin the race from the start of §e" + cur.map.displayName() + "§7.");
            plugin.getLogger().info("Race late join: " + p.getName() + " resumes racing on " + cur.map.id());
            return true;
        }
        if (r == null) watchers.add(p.getUniqueId());
        p.getInventory().clear();
        p.getInventory().setArmorContents(null);
        p.setGameMode(GameMode.SPECTATOR);
        TgMapData.Spawn sp = !data.overviewPoints.isEmpty() ? data.overviewPoints.get(0) : data.startSpawns.get(0);
        p.teleport(plugin.placer().loc(world, cur.slot, sp));
        p.sendMessage("§7You are spectating §e" + cur.map.displayName() + "§7" + (r != null ? " (you already finished this round)." : "."));
        plugin.getLogger().info("Race late join: " + p.getName() + " spectates " + cur.map.id() + (r == null ? " (not a participant)" : " (finished)"));
        return true;
    }

    private void onSecond() {
        if (!running) return;
        List<Player> on = participants();
        switch (phase) {
            case PRE -> {
                preLeft--;
                ctx.setTimer(Math.max(0, preLeft), s.preRoundSeconds);
                if (preLeft == 3 && countdownTask == null) {
                    countdownTask = McmFx.countdown(plugin, on, 3, "§7Chaos begins in...", () -> { countdownTask = null; go(); });
                }
                if (preLeft <= -2 && phase == Phase.PRE && countdownTask == null) go(); // safety
                checkChickens();
            }
            case RUNNING -> {
                timeLeft--;
                if (s.timeSkip && !timeSkipped && timeLeft > s.timeSkipSeconds && on.size() >= 2) {
                    int fin = 0;
                    for (Player p : on) if (isFinished(p)) fin++;
                    if (fin >= Math.ceil(on.size() * s.timeSkipFraction)) {
                        timeSkipped = true;
                        timeLeft = s.timeSkipSeconds;
                        ctx.broadcast("§6⏩ Time skip! §7" + Math.round(s.timeSkipFraction * 100) + "% have finished - §e" + s.timeSkipSeconds + " s left!");
                        McmFx.sound(on, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
                    }
                }
                ctx.setTimer(Math.max(0, timeLeft), roundSecs());
                if (timeLeft <= 5 && timeLeft > 0) McmFx.sound(on, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.5f);
                karmaSecond(on);
                checkChickens();
                leashChickens();
                if (timeLeft <= 0) { endRound("Time's up!"); return; }
            }
            default -> { }
        }
        ctx.setSidebarLines(sidebar());
        actionBars(on);
    }

    private void karmaSecond(List<Player> on) {
        if (!s.kEnabled) return;
        for (Player p : on) {
            Racer r = racers.get(p.getUniqueId());
            if (r == null) continue;
            if (r.lockUntil > 0 && r.lockUntil <= tick) {
                r.lockUntil = 0;
                r.karma = Math.min(r.karma, (s.kMoreFrom + s.kGlowFrom) / 2.0);
                temp(r, "§aYou can punch again.");
            }
            r.karma = Math.max(0, r.karma - s.kDecay);
            r.protection = Math.max(0, r.protection - s.protDecay * s.protPerHit);
            boolean glow = !r.finished && (r.karma >= s.kGlowFrom || r.lockUntil > tick);
            if (glow != r.glowing) { r.glowing = glow; p.setGlowing(glow); }
            if (glow && s.kSlowness) p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 0, false, false, true));
        }
    }

    private void leashChickens() {
        if (data.pen == null) return;
        for (int ci = 0; ci < chickens.size(); ci++) {
            Chicken c = chickens.get(ci);
            if (!c.isValid()) continue;
            Location l = c.getLocation();
            if (!data.pen.contains(lx(l.getX()), ly(l.getY()), lz(l.getZ()))) {
                TgMapData.Spawn sp = data.chickenSpawns.get(ci % data.chickenSpawns.size());   // back to its own spot in the row
                c.teleport(plugin.placer().loc(world, cur.slot, sp));
                c.setVelocity(new Vector());
            }
        }
        for (Chicken c : decoys) {
            if (!c.isValid()) continue;
            Location home = decoyHome.get(c.getUniqueId());
            if (home != null && (c.getLocation().distanceSquared(home) > (double) s.decoyRadius * s.decoyRadius || c.getLocation().getY() < home.getY() - 3)) {
                c.teleport(home);
                c.setVelocity(new Vector());
            }
        }
    }

    private void actionBars(List<Player> on) {
        for (Player p : on) {
            Racer r = racers.get(p.getUniqueId());
            if (r == null) continue;
            String msg;
            if (r.tempMsg != null && r.tempUntil > tick) msg = r.tempMsg;
            else if (phase == Phase.LOADING) {
                Paster.Job j = job;
                msg = "§7Preparing the next map" + (cur == null ? "" : " §e" + cur.map.displayName()) + "§7... §f"
                        + (j == null ? "0" : String.valueOf(Math.round(j.progress() * 100))) + "%";
            }
            else if (phase == Phase.PRE) msg = "§eStarting round in §f" + clock(Math.max(0, preLeft)) + " §8| §7" + cur.map.displayName();
            else if (phase == Phase.RUNNING && r.finished) msg = "§aFinished " + ordinal(r.places[round - 1]) + " §8| §7Rounds: " + placesText(r);
            else if (phase == Phase.RUNNING) msg = (s.kEnabled ? "§7Karma: " + karmaText(r) + " §8| " : "")
                    + (fx != null && !fx.features().gates.isEmpty() && s.bonusEnabled ? "§6Line bonus §f" + bonusTotal(r) + "§7/" + s.bonusCap + " §8| " : "")
                    + "§7Rounds: " + placesText(r);
            else msg = "§7Rounds: " + placesText(r) + " §8| §7Points: §a" + r.points;
            McmFx.actionBar(p, msg);
        }
    }

    private List<String> sidebar() {
        List<String> l = new ArrayList<>();
        l.add("§7Round §f" + Math.max(1, round) + "§7/§f" + s.rounds);
        l.add("§7Map: §b" + (cur == null ? "-" : cur.map.displayName()));
        int n = participants().size();
        l.add("§7Finished: §a" + finishOrder.size() + "§7/§f" + n);
        switch (phase) {
            case LOADING -> l.add("§7Building map...");
            case PRE -> l.add("§eStarting in §f" + clock(Math.max(0, preLeft)));
            case RUNNING -> l.add(timeSkipped ? "§6Time skip!" : "§7First: §f" + (finishOrder.isEmpty() ? "-" : racers.get(finishOrder.get(0)).name));
            case INTERMISSION -> l.add(round < plan.size() ? "§7Next map soon..." : "§7Final results soon...");
            default -> l.add("§7Results");
        }
        Racer best = null;
        for (Racer r : racers.values()) if (best == null || r.points > best.points) best = r;
        if (best != null && best.points > 0) l.add("§7Top: §e" + best.name + " §7(" + best.points + ")");
        return l;
    }

    private String placesText(Racer r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(round, r.places.length); i++) {
            if (sb.length() > 0) sb.append(' ');
            int pl = r.places[i];
            boolean live = i == round - 1 && (phase == Phase.PRE || phase == Phase.RUNNING || phase == Phase.LOADING) && pl == 0;
            sb.append(live ? "§8…" : pl == 0 ? "§8-" : (pl <= 3 ? "§6" : "§f") + ordinal(pl));
        }
        return sb.length() == 0 ? "§8-" : sb.toString();
    }

    static String ordinal(int n) {
        int m100 = n % 100, m10 = n % 10;
        String suf = (m100 >= 11 && m100 <= 13) ? "th" : m10 == 1 ? "st" : m10 == 2 ? "nd" : m10 == 3 ? "rd" : "th";
        return n + suf;
    }

    static String time(long ms) {
        long s = ms / 1000, t = (ms % 1000) / 100;
        return (s / 60) + ":" + String.format("%02d", s % 60) + "." + t;
    }

    static String clock(int sec) { return (sec / 60) + ":" + String.format("%02d", sec % 60); }

    // ===================================================================== admin testing hooks (core 3.2.x; no @Override on purpose)

    /** McmApi.isFastTest() via reflection (false on an older core). */
    static boolean fastTest() {
        try {
            Object o = Class.forName("com.mcm.api.McmApi").getMethod("isFastTest").invoke(null);
            return o instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    /** /mcm skipwait: end the pre-round, intermission or results wait now. */
    public boolean skipWait() {
        if (!running) return false;
        switch (phase) {
            case PRE -> {
                if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
                go();
                return true;
            }
            case INTERMISSION -> {
                for (BukkitTask t : delayed) t.cancel();
                delayed.clear();
                if (round < plan.size()) prepareRound(round + 1); else finalResults();
                return true;
            }
            case RESULTS -> {
                if (revealTask != null) { revealTask.cancel(); revealTask = null; }
                for (BukkitTask t : delayed) t.cancel();
                delayed.clear();
                endGame();
                return true;
            }
            default -> { return false; }
        }
    }

    /** /mcm skip: end the current round now, take back its points, go to the next map (or finish normally). */
    public boolean skipRound() {
        if (!running || phase == Phase.RESULTS || phase == Phase.IDLE) return false;
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        for (BukkitTask t : delayed) t.cancel();
        delayed.clear();
        if (job != null) { plugin.paster().cancel(job); job = null; }   // a pre-built next map is kept
        if (phase != Phase.INTERMISSION) {
            for (Map.Entry<UUID, Integer> e : roundPts.entrySet()) {
                Racer r = racers.get(e.getKey());
                if (e.getValue() != 0) ctx.addPoints(e.getKey(), -e.getValue(), "Race round skipped");
                if (r != null) r.points -= e.getValue();
            }
            for (Map.Entry<UUID, Integer> e : sideShare.entrySet()) {
                Racer r = racers.get(e.getKey());
                if (r != null) r.points -= e.getValue();
            }
            for (Map.Entry<String, Integer> e : roundSidePts.entrySet())
                ctx.addSidePoints(e.getKey(), -e.getValue(), "Race round skipped");
            for (Racer r : racers.values()) if (round >= 1 && round <= r.places.length) r.places[round - 1] = 0;
            roundPts.clear(); roundSidePts.clear(); sideShare.clear();
            finishOrder.clear();
            ctx.broadcast("§c§lRound " + round + " skipped by an admin §7(no points).");
            for (Player p : participants()) {
                Racer r = racers.get(p.getUniqueId());
                if (p.isInsideVehicle()) p.leaveVehicle();
                p.getInventory().clear();
                p.getInventory().setArmorContents(null);
                p.setGlowing(false);
                p.removePotionEffect(PotionEffectType.SLOWNESS);
                if (r != null) r.glowing = false;
                if (p.getWorld().equals(world)) p.setGameMode(GameMode.SPECTATOR);
            }
            cleanupRound();
        }
        phase = Phase.INTERMISSION;
        if (round < plan.size()) delay(1, () -> prepareRound(round + 1)); else delay(1, this::finalResults);
        return true;
    }

    /** /mcm map: ids of the playable maps. */
    public List<String> maps() {
        List<String> out = new ArrayList<>();
        for (MapRegistry.Entry e : plugin.registry().playable()) out.add(e.map.id());
        return out;
    }

    /** /mcm map &lt;id&gt;: the next round (or the first round of the next game) uses this map. */
    public boolean forceNextMap(String id) {
        MapRegistry.Entry e = plugin.registry().byId(id);
        if (e == null || e.broken) return false;
        forceNext(e);
        return true;
    }


    // ===================================================================== listener helpers

    boolean inWorld(World w) { return running && w != null && w.equals(world); }

    /** Goal chicken or decoy (both protected). */
    boolean isGameChicken(Entity e) {
        return e instanceof Chicken && (e.getScoreboardTags().contains(CHICKEN_TAG) || e.getScoreboardTags().contains(DECOY_TAG));
    }

    boolean isDecoy(Entity e) { return e.getScoreboardTags().contains(DECOY_TAG); }

    /** /race info (no map): live goal chickens and punch log for the running round. */
    List<String> liveStatus() {
        List<String> out = new ArrayList<>();
        if (!running || cur == null || data == null) { out.add("§7No Race game running."); return out; }
        out.add("§6Live: §fround " + round + "/" + plan.size() + " §7on §e" + cur.map.id() + " §7(slot " + cur.slot + "), phase §f" + phase);
        int alive = 0, inPen = 0;
        for (Chicken c : chickens) {
            if (!c.isValid()) continue;
            alive++;
            Location l = c.getLocation();
            if (data.pen != null && data.pen.contains(lx(l.getX()), ly(l.getY()), lz(l.getZ()))) inPen++;
        }
        int dAlive = 0;
        for (Chicken c : decoys) if (c.isValid()) dAlive++;
        out.add("§7Goal chickens: §a" + alive + "§7/" + chickens.size() + " alive, " + inPen + " in pen §8| §7decoys: " + dAlive + "/" + decoys.size()
                + " §8| §7lethal fall: " + (cur.map.lethalFallHeight() > 0 ? cur.map.lethalFallHeight() + " blocks" : "off"));
        if (fx != null) out.addAll(fx.status());
        out.add("§7Chicken chunks held by ticket; re-spawned this round: §f" + chickenRespawns);
        for (int i = 0; i < chickens.size(); i++) out.add(chickenLine("goal", i, chickens.get(i), i < chickenAt.size() ? chickenAt.get(i) : null));
        for (int i = 0; i < decoys.size(); i++) out.add(chickenLine("decoy", i, decoys.get(i), i < decoyAt.size() ? decoyAt.get(i) : null));
        out.add("§7Chicken punches this round: §a" + punchAccepted + " accepted§7, §c" + punchRejected + " rejected§7; last: §f" + lastPunch);
        out.add("§7Finished: " + finishOrder.size() + "/" + participants().size());
        return out;
    }

    /** One /race info line per chicken: valid, ticking, its chunk loaded / load level / ticket held, where it is. */
    private String chickenLine(String kind, int i, Chicken c, Location home) {
        Location l = c.isValid() ? c.getLocation() : home;
        if (l == null) return "§8 - " + kind + " " + i + " §c(no location)";
        int cx = l.getBlockX() >> 4, cz = l.getBlockZ() >> 4;
        boolean loaded = world.isChunkLoaded(cx, cz);
        String level = loaded ? world.getChunkAt(cx, cz).getLoadLevel().name() : "UNLOADED";
        boolean inPen = data.pen != null && data.pen.contains(lx(l.getX()), ly(l.getY()), lz(l.getZ()));
        return String.format("§8 - %s %d: %s%s §7chunk %d,%d %s%s §8| §7local %.1f %.1f %.1f%s%s", kind, i,
                c.isValid() ? "§avalid" : "§cGONE", c.isValid() && !c.isTicking() ? " §e(not ticking)" : "",
                cx, cz, loaded ? "§aloaded §7" + level : "§cNOT loaded", plugin.paster().holds(cur.slot, cx, cz) ? " §7ticket" : " §cno ticket",
                lx(l.getX()), ly(l.getY()), lz(l.getZ()), kind.equals("goal") ? (inPen ? " §7in pen" : " §cOUTSIDE pen") : "",
                c.isValid() && !c.isInvulnerable() ? " §cNOT invulnerable" : "");
    }

    boolean isOwnBoat(Player p, Entity vehicle) {
        Racer r = racers.get(p.getUniqueId());
        return r != null && r.boat != null && r.boat.equals(vehicle);
    }

    Collection<Racer> racers() { return racers.values(); }
}
