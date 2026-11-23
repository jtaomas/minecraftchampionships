package com.mcm.games.tgttos;

import com.mcm.api.McmBuild;
import com.mcm.api.McmFx;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.TgMapData;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/** The TGTTOS game: 6 rounds on 6 maps, race to punch a chicken. */
final class TgttosGame implements McmGame {
    static final String ENT_TAG = "tg_ent";
    static final String CHICKEN_TAG = "tg_chicken";

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
        Racer(UUID id, String name, int rounds) { this.id = id; this.name = name; this.places = new int[rounds]; }
    }

    private final TgttosPlugin plugin;
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

    private BukkitTask tickTask, secondTask, countdownTask, revealTask;
    private final List<BukkitTask> delayed = new ArrayList<>();
    private McmBuild.BuildQueue queue;
    private long tick;
    private int preLeft, timeLeft;
    private boolean timeSkipped;
    private long goMillis;
    /** Points handed out this round (for /mcm skip: the skipped round pays nothing). */
    private final Map<UUID, Integer> roundPts = new HashMap<>();
    private final Map<String, Integer> roundSidePts = new HashMap<>();
    private final Map<UUID, Integer> sideShare = new HashMap<>();

    TgttosGame(TgttosPlugin plugin) { this.plugin = plugin; }

    @Override public String id() { return "tgttos"; }
    @Override public String displayName() { return "To Get To The Other Side"; }
    @Override public Material icon() { return Material.FEATHER; }
    @Override public String description() { return "6 maps, 6 crossings: get to the other side and whack a chicken."; }
    @Override public boolean useCoreScoreboard() { return true; }

    boolean isRunning() { return running; }
    World world() { return world; }
    Phase phase() { return phase; }
    Settings settings() { return s; }
    TgMapData data() { return data; }
    MapRegistry.Entry current() { return cur; }

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
        racers.clear(); sideMembers.clear(); placed.clear(); spawned.clear(); chickens.clear();
        round = 0;
        if (world == null) { abort("§cTGTTOS: game world could not be created."); return; }

        List<MapRegistry.Entry> pool = new ArrayList<>(plugin.registry().playable());
        List<MapRegistry.Entry> real = new ArrayList<>();
        for (MapRegistry.Entry e : pool) if (!e.stub) real.add(e);
        if (real.size() >= s.rounds) pool = real;
        if (pool.isEmpty()) { abort("§cTGTTOS: no playable maps are installed."); return; }
        Collections.shuffle(pool, rnd);
        if (forcedNext != null && !forcedNext.broken) { pool.remove(forcedNext); pool.add(0, forcedNext); }
        forcedNext = null;
        plan.clear();
        for (int i = 0; i < s.rounds; i++) plan.add(pool.get(i % pool.size()));
        if (pool.size() < s.rounds)
            plugin.getLogger().warning("TGTTOS: only " + pool.size() + " playable maps for " + s.rounds + " rounds; maps repeat.");

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
        ctx.broadcast("§6§lTo Get To The Other Side §7- " + s.rounds + " rounds. Cross the map and §ewhack a chicken§7!");
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
        resetPlayers();
        phase = Phase.IDLE;
    }

    private void cancelTasks() {
        for (BukkitTask t : new BukkitTask[]{tickTask, secondTask, countdownTask, revealTask}) if (t != null) t.cancel();
        tickTask = secondTask = countdownTask = revealTask = null;
        for (BukkitTask t : delayed) t.cancel();
        delayed.clear();
        if (queue != null) { queue.cancel(); queue = null; }
    }

    private void endGame() {
        if (finishCalled) return;
        finishCalled = true;
        boolean wasRunning = running;
        running = false;
        cancelTasks();
        cleanupRound();
        resetPlayers();
        phase = Phase.IDLE;
        if (wasRunning || ctx != null) ctx.finish("TGTTOS finished");
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

    private void prepareRound(int n) {
        if (!running) return;
        round = n;
        if (forcedNext != null && !forcedNext.broken) { plan.set(n - 1, forcedNext); forcedNext = null; }
        cur = plan.get(n - 1);
        phase = Phase.LOADING;
        try {
            data = s.randomSeeds || cur.cached == null ? cur.map.build(rnd.nextLong()) : cur.cached;
        } catch (Throwable t) {
            plugin.getLogger().severe("TGTTOS: map " + cur.map.id() + " failed to build: " + t);
            data = cur.cached;
        }
        if (data == null) { abort("§cTGTTOS: map " + cur.map.displayName() + " could not be built."); return; }
        variantChoice = Variants.pick(data, rnd);
        hazardMats.clear();
        hazardMats.add(Material.LAVA);
        for (String h : data.hazardBlocks) {
            String b = h; int i = b.indexOf('['); if (i > 0) b = b.substring(0, i);
            Material m = Material.matchMaterial(b);
            if (m != null) hazardMats.add(m);
        }
        for (Racer r : racers.values()) { r.finished = false; r.karma = 0; r.protection = 0; r.lockUntil = 0; }
        finishOrder.clear();
        bonusSides.clear();
        roundPts.clear(); roundSidePts.clear(); sideShare.clear();
        timeSkipped = false;
        ctx.setSidebarLines(sidebar());
        ctx.setTimer(s.preRoundSeconds, s.preRoundSeconds);
        queue = new McmBuild.BuildQueue(plugin);
        plugin.placer().enqueue(queue, world, cur.slot, data, variantChoice);
        queue.then(() -> { queue = null; if (running) beginPre(); });
        queue.start();
        if (!variantChoice.isEmpty())
            plugin.getLogger().info("TGTTOS round " + n + " " + cur.map.id() + " variants: " + Variants.summary(data, variantChoice));
    }

    private void beginPre() {
        phase = Phase.PRE;
        preLeft = fastTest() ? 1 : s.preRoundSeconds;
        spawnChickens();
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
        ctx.broadcast("§6Round " + round + "/" + s.rounds + ": §e§l" + cur.map.displayName() + " §7(" + cur.map.type().label() + ") §8- §7" + kitText(kit));
        ctx.setTimer(preLeft, s.preRoundSeconds);
        ctx.setSidebarLines(sidebar());
    }

    private String kitText(Kit kit) {
        return switch (kit) {
            case CONCRETE_PICKAXE_SNOWBALLS -> s.concrete + " team concrete, pickaxe, " + s.snowballs + " snowballs. Break rival bridges!";
            case SNOWBALLS_ONLY -> s.snowballs + " snowballs. No building on this one.";
            case BOAT -> "You start in a boat. " + s.snowballs + " snowballs.";
            case ELYTRA_FIREWORKS -> "Elytra + " + s.fireworks + " firework" + (s.fireworks == 1 ? "" : "s") + ". The floor drops at GO!";
        };
    }

    private void go() {
        if (!running || phase != Phase.PRE) return;
        phase = Phase.RUNNING;
        timeLeft = s.roundSeconds;
        goMillis = System.currentTimeMillis();
        int ox = plugin.placer().ox(cur.slot), oy = plugin.placer().oy(), oz = plugin.placer().oz(cur.slot);
        for (TgMapData.Pos p : data.startBarrier) world.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setType(Material.AIR, false);
        for (TgMapData.Pos p : data.dropFloor) world.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setType(Material.AIR, false);
        if (!data.startBarrier.isEmpty()) {
            TgMapData.Pos p = data.startBarrier.get(data.startBarrier.size() / 2);
            McmFx.soundAt(new Location(world, ox + p.x(), oy + p.y(), oz + p.z()), Sound.BLOCK_GLASS_BREAK, 2f, 0.8f);
        } else if (data.startBarrier.isEmpty() && data.dropFloor.isEmpty()) {
            plugin.getLogger().warning("TGTTOS map " + cur.map.id() + " has no start barrier; players were only held by the strip check.");
        }
        if (!data.dropFloor.isEmpty()) McmFx.sound(racingPlayers(), Sound.BLOCK_PISTON_CONTRACT, 1f, 0.6f);
        ctx.setTimer(timeLeft, s.roundSeconds);
        ctx.setSidebarLines(sidebar());
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
        }
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
        StringBuilder sb = new StringBuilder("§6§lTGTTOS - final §7(points this game):");
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
        if (p.isInsideVehicle()) p.leaveVehicle();
        removeBoat(r);
        TgMapData.Spawn sp = data.startSpawns.get(Math.floorMod(spawnIdx, data.startSpawns.size()));
        Location l = plugin.placer().loc(world, cur.slot, sp);
        p.setFallDistance(0f);
        p.setVelocity(new Vector());
        p.setFireTicks(0);
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
        p.setCollidable(false);
        if (p.isGliding()) p.setGliding(false);
        if (kit == Kit.BOAT) {
            List<TgMapData.Spawn> bs = data.boatSpawns.isEmpty() ? data.startSpawns : data.boatSpawns;
            TgMapData.Spawn b = bs.get(Math.floorMod(spawnIdx, bs.size()));
            Location bl = plugin.placer().loc(world, cur.slot, b);
            p.teleport(bl);
            Boat boat = world.spawn(bl, Boat.class, e -> {
                e.addScoreboardTag(ENT_TAG);
                e.setPersistent(false);
            });
            spawned.add(boat);
            r.boat = boat;
            boat.addPassenger(p);
        } else {
            p.teleport(l);
        }
        giveKit(p, kit);
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
                if (s.snowballs > 0) inv.setItem(2, new ItemStack(Material.SNOWBALL, Math.min(16, s.snowballs)));
            }
            case SNOWBALLS_ONLY, BOAT -> {
                if (s.snowballs > 0) inv.setItem(0, new ItemStack(Material.SNOWBALL, Math.min(16, s.snowballs)));
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

    private ItemStack parseItem(String spec) {
        if (spec == null || spec.isBlank()) return null;
        String sp = spec.trim();
        int count = 1;
        int last = sp.lastIndexOf(':');
        if (last > 0) {
            try { count = Integer.parseInt(sp.substring(last + 1)); sp = sp.substring(0, last); } catch (NumberFormatException ignored) {}
        }
        Material m = Material.matchMaterial(sp);
        if (m == null || !m.isItem()) { plugin.getLogger().warning("TGTTOS: unknown extra item '" + spec + "'"); return null; }
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
    void respawn(Player p, boolean hazard) {
        Racer r = racers.get(p.getUniqueId());
        if (r == null || data == null) return;
        if (tick - r.lastRespawnTick < 5) return;
        r.lastRespawnTick = tick;
        int idx = rnd.nextInt(Math.max(1, data.startSpawns.size()));
        placeAtStart(p, r, idx, false);
        if (phase == Phase.RUNNING) {
            List<String> lines = hazard ? s.punsHazard : s.punsVoid;
            String name = McmFx.sideColor(ctx, r.side) + p.getName() + "§7";
            String line = lines.get(rnd.nextInt(lines.size()));
            ctx.broadcast("§8☠ §7" + (line.contains("%s") ? String.format(line, name) : name + " " + line));
            McmFx.sound(p, Sound.ENTITY_CHICKEN_HURT, 1f, 0.8f);
            temp(r, "§cBack to the start! §7Kit refilled.");
        }
    }

    private void temp(Racer r, String msg) { r.tempMsg = msg; r.tempUntil = tick + 40; }

    /** Called by the listener when a racer punches a goal chicken. */
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
        ctx.addPoints(r.id, pts, "TGTTOS " + ordinal(place) + " place");
        roundPts.merge(r.id, pts, Integer::sum);
        String col = place == 1 ? "§6" : place == 2 ? "§f" : place == 3 ? "§c" : "§e";
        McmFx.title(p, col + "§l" + ordinal(place), "§c§lWhack! §7" + time(ms) + " §8| §a+" + pts, 2, 40, 10);
        McmFx.sound(p, place <= 3 ? Sound.UI_TOAST_CHALLENGE_COMPLETE : Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        Location cl = chicken.getLocation();
        McmFx.soundAt(cl, Sound.ENTITY_CHICKEN_HURT, 1.5f, 1.2f);
        McmFx.soundAt(cl, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        McmFx.burst(cl.clone().add(0, 0.5, 0), Color.WHITE, 24);
        McmFx.teamFirework(cl.clone().add(0, 1.5, 0), McmFx.sideColor(ctx, r.side), place <= 3);
        spawnWhackText(cl.clone().add(0, 1.3, 0));
        ctx.broadcast("§a✔ " + McmFx.sideColor(ctx, r.side) + p.getName() + " §7whacked a chicken! §e" + ordinal(place)
                + " place §8(" + time(ms) + ", +" + pts + ")");
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
            ctx.addSidePoints(e.getKey(), bonus, "TGTTOS team finish bonus");
            roundSidePts.merge(e.getKey(), bonus, Integer::sum);
            int share = bonus / Math.max(1, on);
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

    private void spawnChickens() {
        if (data.chickenSpawns.isEmpty()) return;
        int n = Math.max(data.chickenSpawns.size() >= 8 ? 8 : 1, Math.min(s.chickenCount, 40));
        for (int i = 0; i < n; i++) {
            TgMapData.Spawn sp = data.chickenSpawns.get(i % data.chickenSpawns.size());
            Location l = plugin.placer().loc(world, cur.slot, sp);
            Chicken c = world.spawn(l, Chicken.class, e -> {
                e.addScoreboardTag(ENT_TAG);
                e.addScoreboardTag(CHICKEN_TAG);
                e.setInvulnerable(true);
                e.setPersistent(false);
                e.setRemoveWhenFarAway(false);
                e.setCollidable(false);
                e.setAdult();
                e.setAgeLock(true);
                e.setAI(s.chickenWander);
                e.setSilent(false);
            });
            chickens.add(c);
            spawned.add(c);
        }
    }

    private void cleanupRound() {
        for (Entity e : new ArrayList<>(spawned)) if (e != null && e.isValid()) e.remove();
        spawned.clear();
        chickens.clear();
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

    void onPlaced(Block b) { placed.add(key(b)); }

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
    void punch(Player attacker, Player victim) {
        Racer a = racers.get(attacker.getUniqueId()), v = racers.get(victim.getUniqueId());
        if (a == null || v == null) return;
        if (a.lockUntil > tick) {
            temp(a, "§4Karma lock! §7You can't punch for " + Math.max(1, (a.lockUntil - tick + 19) / 20) + " s.");
            McmFx.sound(attacker, Sound.BLOCK_NOTE_BLOCK_BASS, 0.8f, 0.6f);
            return;
        }
        if (tick - v.lastKnockTick < s.hitCooldownTicks) return;
        // karma: punching a high-karma player gives none; repeat victims give more
        if (v.karma < s.kGlowFrom) {
            double gain = s.kPerPunch;
            if (victim.getUniqueId().equals(a.lastVictim) && tick - a.lastVictimTick <= s.kSameSeconds * 20L) gain += s.kSameBonus;
            a.karma += gain;
        }
        a.lastVictim = victim.getUniqueId();
        a.lastVictimTick = tick;
        if (a.karma >= s.kLockFrom && a.lockUntil <= tick) {
            a.lockUntil = tick + s.kLockSeconds * 20L;
            temp(a, "§4Karma lock! §7Too much punching - " + s.kLockSeconds + " s cooldown.");
            McmFx.sound(attacker, Sound.ENTITY_VILLAGER_NO, 1f, 0.8f);
        }
        float cd = attacker.getAttackCooldown();
        double h = s.kbPunchH * (cd < 0.9f ? 0.7 : 1.0) + (attacker.isSprinting() && cd >= 0.9f ? s.kbSprint : 0);
        double yaw = Math.toRadians(attacker.getLocation().getYaw());
        Vector dir = new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
        knock(victim, v, dir, h, s.kbPunchV);
        v.protection = Math.min(s.protMax, v.protection + s.protPerHit);
    }

    void snowballHit(Player shooter, Player victim, Vector velocity) {
        Racer v = racers.get(victim.getUniqueId());
        if (v == null) return;
        Vector dir = velocity.clone().setY(0);
        if (dir.lengthSquared() < 1e-6 && shooter != null) dir = victim.getLocation().toVector().subtract(shooter.getLocation().toVector()).setY(0);
        knock(victim, v, dir, s.kbSnowH, s.kbSnowV);
    }

    private void knock(Player victim, Racer v, Vector dirIn, double h, double up) {
        if (dirIn.lengthSquared() < 1e-6) return;
        Vector dir = dirIn.clone().normalize();
        double mult = takenMult(v);
        Vector cur = victim.getVelocity();
        Vector out = new Vector(cur.getX() * 0.5 + dir.getX() * h * mult,
                Math.min(0.45, Math.max(cur.getY() * 0.5, 0) + up * Math.min(1.25, mult)),
                cur.getZ() * 0.5 + dir.getZ() * h * mult);
        Entity veh = victim.getVehicle();
        if (veh != null) veh.setVelocity(out.clone().setY(Math.min(0.2, out.getY())));
        else victim.setVelocity(out);
        v.lastKnockTick = tick;
        victim.playHurtAnimation(0);
        McmFx.soundAt(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 0.8f, 1f);
    }

    private double takenMult(Racer v) {
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
            List<Player> viewers = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (p.getWorld().equals(world)) viewers.add(p);
            ScoreTeams.sync(viewers, names, red);
        }
        if (phase != Phase.PRE && phase != Phase.RUNNING && phase != Phase.INTERMISSION) return;
        for (Player p : on) {
            if (!p.getWorld().equals(world)) continue;
            Racer r = racers.get(p.getUniqueId());
            Location l = p.getLocation();
            double x = lx(l.getX()), y = ly(l.getY()), z = lz(l.getZ());
            if (p.getGameMode() == GameMode.SPECTATOR) {
                if (data.playBounds != null && !data.playBounds.contains(x, y, z)) {
                    TgMapData.Region b = data.playBounds;
                    double cx = Math.max(b.minX() + 0.5, Math.min(b.maxX() + 0.5, x));
                    double cy = Math.max(b.minY() + 0.5, Math.min(b.maxY() + 0.5, y));
                    double cz = Math.max(b.minZ() + 0.5, Math.min(b.maxZ() + 0.5, z));
                    p.teleport(plugin.placer().loc(world, cur.slot, cx, cy, cz).setDirection(l.getDirection()));
                }
                continue;
            }
            if (phase == Phase.PRE) {
                boolean out = data.startStrip != null && !data.startStrip.contains(x, y, z) && !p.isInsideVehicle();
                if (out || y < data.voidY) placeAtStart(p, r, r.spawnIdx, false);
                continue;
            }
            if (phase != Phase.RUNNING || r.finished) continue;
            if (y < data.voidY || (data.playBounds != null && !data.playBounds.contains(x, y, z))) { respawn(p, false); continue; }
            boolean hz = false;
            for (TgMapData.Region hr : data.hazardRegions) if (hr.contains(x, y, z)) { hz = true; break; }
            if (!hz && !hazardMats.isEmpty()) {
                Block feet = l.getBlock();
                hz = hazardMats.contains(feet.getType()) || hazardMats.contains(l.clone().add(0, -0.2, 0).getBlock().getType());
            }
            if (hz) respawn(p, true);
            if (p.getFireTicks() > 0) p.setFireTicks(0);
        }
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
                ctx.setTimer(Math.max(0, timeLeft), s.roundSeconds);
                if (timeLeft <= 5 && timeLeft > 0) McmFx.sound(on, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.5f);
                karmaSecond(on);
                leashChickens();
                if (timeLeft <= 0) { endRound("Time's up!"); return; }
            }
            default -> { }
        }
        ctx.setSidebarLines(sidebar());
        actionBars(on);
    }

    private void karmaSecond(List<Player> on) {
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
        for (Chicken c : chickens) {
            if (!c.isValid()) continue;
            Location l = c.getLocation();
            if (!data.pen.contains(lx(l.getX()), ly(l.getY()), lz(l.getZ()))) {
                TgMapData.Spawn sp = data.chickenSpawns.get(rnd.nextInt(data.chickenSpawns.size()));
                c.teleport(plugin.placer().loc(world, cur.slot, sp));
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
            else if (phase == Phase.PRE) msg = "§eStarting round in §f" + clock(Math.max(0, preLeft)) + " §8| §7" + cur.map.displayName();
            else if (phase == Phase.RUNNING && r.finished) msg = "§aFinished " + ordinal(r.places[round - 1]) + " §8| §7Rounds: " + placesText(r);
            else if (phase == Phase.RUNNING) msg = "§7Karma: " + karmaText(r) + " §8| §7Rounds: " + placesText(r);
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
        if (queue != null) { queue.cancel(); queue = null; }
        if (phase != Phase.INTERMISSION) {
            for (Map.Entry<UUID, Integer> e : roundPts.entrySet()) {
                Racer r = racers.get(e.getKey());
                if (e.getValue() != 0) ctx.addPoints(e.getKey(), -e.getValue(), "TGTTOS round skipped");
                if (r != null) r.points -= e.getValue();
            }
            for (Map.Entry<UUID, Integer> e : sideShare.entrySet()) {
                Racer r = racers.get(e.getKey());
                if (r != null) r.points -= e.getValue();
            }
            for (Map.Entry<String, Integer> e : roundSidePts.entrySet())
                ctx.addSidePoints(e.getKey(), -e.getValue(), "TGTTOS round skipped");
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

    boolean isGameChicken(Entity e) { return e instanceof Chicken && e.getScoreboardTags().contains(CHICKEN_TAG); }

    boolean isOwnBoat(Player p, Entity vehicle) {
        Racer r = racers.get(p.getUniqueId());
        return r != null && r.boat != null && r.boat.equals(vehicle);
    }

    Collection<Racer> racers() { return racers.values(); }
}
