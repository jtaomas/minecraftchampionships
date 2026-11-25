package com.mcm.games.decisiondome;

import com.mcm.api.McmApi;
import com.mcm.api.McmVoteRequest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;

import static com.mcm.games.decisiondome.DomeArena.c;

/**
 * One complete vote: teams, pods, eggs, chickens, power-ups, the phase clock and the exactly-once
 * callback. All methods run on the server main thread.
 */
final class DomeSession {
    private final DecisionDomePlugin plugin;
    private final DomeController controller;
    private final McmVoteRequest request;
    private final DomeVenue venue;
    private final DomeSettings settings;
    private final boolean testMode;
    private final Consumer<String> callback;
    private final String sessionId;
    private final long seed;

    private final AtomicBoolean delivered = new AtomicBoolean(false);
    private final PlayerStateService players = new PlayerStateService();
    private final List<TeamPod> pods = new ArrayList<>();
    private final Map<UUID, TeamPod> podOf = new LinkedHashMap<>();
    private final LinkedHashSet<UUID> baseEggGranted = new LinkedHashSet<>();
    private final LinkedHashSet<UUID> thrownEggs = new LinkedHashSet<>();

    private VoteEngine engine;
    private Dial dial;
    private BossBar bar;
    private EggService eggs;
    private ChickenService chickens;
    private PowerUpService powerUps;
    private DomeLog log;
    private Phase lastPhase = Phase.IDLE;
    private int lastHudSecond = Integer.MIN_VALUE;
    private int pendingNameAt = -1;
    private boolean running;
    private boolean cleaned;
    private String winner;
    private long startedMillis;
    private int fireworkAt = -1;

    DomeSession(DecisionDomePlugin plugin, DomeController controller, McmVoteRequest request,
                DomeVenue venue, DomeSettings settings, boolean testMode, Consumer<String> callback,
                long seed) {
        this.plugin = plugin;
        this.controller = controller;
        this.request = request == null ? McmVoteRequest.simple(List.of()) : request;
        this.venue = venue;
        this.settings = settings;
        this.testMode = testMode;
        this.callback = callback;
        this.seed = seed;
        this.sessionId = Long.toHexString((seed ^ System.nanoTime()) & 0xFFFFFFFFFFFFL);
    }

    String sessionId() { return sessionId; }
    boolean testMode() { return testMode; }
    boolean running() { return running; }
    VoteEngine engine() { return engine; }
    DomeVenue venue() { return venue; }
    List<TeamPod> pods() { return pods; }
    String winner() { return winner; }

    // ---------------------------------------------------------------------------------------------
    // Start
    // ---------------------------------------------------------------------------------------------

    void start() {
        startedMillis = System.currentTimeMillis();
        log = new DomeLog(plugin, sessionId);
        int capacity = request.dialCapacity() == 6 ? 6 : settings.dialCapacity;
        if (capacity < 6 || capacity > 8) capacity = settings.dialCapacity;
        List<String> ids = VoteEngine.normaliseIds(request.gameIds());
        if (ids.size() > settings.maxGames) ids = new ArrayList<>(ids.subList(0, settings.maxGames));
        int blankSlots = blankSlots(capacity, request.eventIndex(), ids.size(), settings.refillVote);
        dial = new Dial(ids, capacity, blankSlots, venue.geometry());
        int refilledNow = request.eventIndex() == settings.refillVote
                ? Math.min(dial.size(), Math.max(0, settings.refillVote - 1)) : 0;

        boolean powerUpsAllowed = testMode
                ? settings.powerUps && !request.audienceVote()
                : settings.powerUpsFor(request.eventIndex(), request.audienceVote());
        engine = new VoteEngine(dial, request.teams(), seed, powerUpsAllowed,
                request.audienceVote(), request.eventIndex(), FastTest.on());
        engine.refillAfterRemoval(false);
        for (Map.Entry<Phase, Integer> e : settings.phaseTicks().entrySet()) {
            engine.overrideDuration(e.getKey(), e.getValue());
        }
        engine.start();

        if (!venue.prepare(dial)) {
            throw new IllegalStateException("Dome venue could not be prepared");
        }
        venue.setDividers(0.0);

        eggs = new EggService(plugin, sessionId);
        chickens = new ChickenService(plugin, sessionId);
        DomeRng powerRng = new DomeRng(DomeRng.mix(seed, 0x5057, 9));
        powerUps = new PowerUpService(plugin, sessionId, settings, eggs, engine, venue, powerRng,
                new PowerUpService.Hooks() {
                    @Override public boolean throwExtraEgg(Player player, Egg.Kind kind) {
                        return throwEgg(player, kind, true);
                    }
                    @Override public void grantEggItem(Player player, Egg.Kind kind) { giveEgg(player, kind, false); }
                    @Override public void announce(String message) { rawSay(message); }
                    @Override public void tell(Player player, String message) { if (player != null) player.sendMessage(c(message)); }
                }, podOf);

        buildPods();
        teleportAndPrepare();

        bar = Bukkit.createBossBar("§e§lGET READY TO VOTE...", BarColor.YELLOW, BarStyle.SOLID);
        for (Player p : Bukkit.getOnlinePlayers()) bar.addPlayer(p);

        running = true;
        lastPhase = engine.phase();
        updateHud(true);
        domeSay("The dome is open. Throw your egg at the game you want.");
        if (engine.confirmation()) {
            String only = engine.bestKnownResult();
            domeSay("Confirmation Dome: §e" + McmApi.displayName(only) + " §fis the only game left. No vote.");
        } else {
            domeSay("§e30 seconds §fto vote. One egg each; the chicken's wedge at tally is the vote.");
            if (refilledNow > 0) announceNewGames(refilledNow);
            for (TeamPod pod : pods) {
                if (pod.members().isEmpty()) continue;
                for (UUID uuid : pod.members()) {
                    Player p = Bukkit.getPlayer(uuid);
                    if (p != null && p.isOnline()) {
                        giveEgg(p, Egg.Kind.CHICKEN, true);
                        baseEggGranted.add(uuid);
                    }
                }
            }
            if (powerUpsAllowed) powerUps.grant(pods);
        }
        log.event("start", "seed=" + seed + " ids=" + ids + " capacity=" + capacity
                + " venue=" + venue.id() + " v" + venue.apiVersion() + " confirmation=" + engine.confirmation());
    }

    /**
     * How many wedges are blank before this vote. Before the refill vote the chosen slots stay blank;
     * from the refill vote on, the dial is topped up from the remaining pool as slots free up.
     */
    static int blankSlots(int capacity, int eventIndex, int remaining, int refillVote) {
        int all = Math.max(0, capacity - Math.min(capacity, remaining));
        if (eventIndex <= 1) return all;
        if (eventIndex < refillVote) return Math.min(capacity, eventIndex - 1);
        return all;
    }

    private void announceNewGames(int count) {
        StringBuilder names = new StringBuilder();
        int from = Math.max(0, dial.size() - count);
        for (int i = from; i < dial.size(); i++) {
            Wedge w = dial.wedge(i);
            if (w == null || w.gameId() == null) continue;
            if (names.length() > 0) names.append("§7, §f");
            names.append(McmApi.displayName(w.gameId()));
        }
        if (names.length() > 0) domeSay("§6New Game Added! §f" + names + " §7topped up the dial.");
    }

    private void buildPods() {
        Map<String, List<UUID>> groups = new LinkedHashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead() || p.getGameMode() == org.bukkit.GameMode.SPECTATOR) continue;
            String team = request.teams().get(p.getUniqueId());
            if (team == null || team.isBlank()) team = "Solos";
            groups.computeIfAbsent(team, k -> new ArrayList<>()).add(p.getUniqueId());
        }
        List<String> keys = new ArrayList<>(groups.keySet());
        keys.sort((a, b) -> a.equals("Solos") ? 1 : b.equals("Solos") ? -1 : a.compareToIgnoreCase(b));
        int max = Math.max(1, Math.min(10, settings.podCount));
        if (keys.size() > max) {
            List<String> merged = new ArrayList<>(keys.subList(0, max - 1));
            List<UUID> rest = new ArrayList<>();
            for (String k : keys.subList(max - 1, keys.size())) rest.addAll(groups.get(k));
            groups.put("Mixed", rest);
            keys = merged;
            keys.add("Mixed");
        }
        int color = 0;
        for (String key : keys) {
            TeamPod pod = new TeamPod(pods.size(), key, color++);
            for (UUID uuid : groups.getOrDefault(key, List.of())) {
                pod.addMember(uuid);
                podOf.put(uuid, pod);
            }
            pods.add(pod);
        }
        // Keep ten visual pods even with fewer teams, so the dial ring matches the venue.
        while (pods.size() < max) {
            pods.add(new TeamPod(pods.size(), "Empty", color++));
        }
        for (TeamPod pod : pods) {
            venue.setPodColor(pod.index(), pod.colorIndex());
            venue.setPodGate(pod.index(), true);
            venue.setPodState(pod.index(), "normal");
        }
    }

    private void teleportAndPrepare() {
        for (TeamPod pod : pods) {
            Location spawn = venue.podSpawn(pod.index());
            for (UUID uuid : pod.members()) {
                Player p = Bukkit.getPlayer(uuid);
                if (p == null || !p.isOnline()) continue;
                players.capture(p);
                players.prepare(p, spawn);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Tick
    // ---------------------------------------------------------------------------------------------

    void tickSafe() {
        if (!running) return;
        try {
            tick();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Decision Dome tick failed", t);
            try {
                String best = engine == null ? null : engine.bestKnownResult();
                finishOnce(best);
            } catch (Throwable ignored) {
                abort();
            }
        }
    }

    private void tick() {
        if (!running || engine == null) return;
        Phase before = engine.phase();
        engine.tick();
        chickenSync();
        if (engine.totalTick() % 4 == 0) powerUps.tick();
        venue.tick(engine.totalTick());
        Phase after = engine.phase();
        if (after != before) {
            lastPhase = after;
            log.phase(after, engine.totalTick());
            onPhaseEnter(before, after);
        }
        updateHud(false);
        fireworkTick();
        if (pendingNameAt > 0 && engine.totalTick() >= pendingNameAt) {
            pendingNameAt = -1;
            announceWinnerName();
        }
        if (engine.finished()) {
            finishOnce(engine.winnerGame());
        }
    }

    private void chickenSync() {
        chickens.sync(engine, venue);
    }

    private void onPhaseEnter(Phase from, Phase to) {
        switch (to) {
            case VOTING -> {
                domeSay("§aVoting is open. §fThrow your egg into a wedge!");
                for (TeamPod pod : pods) venue.setPodGate(pod.index(), true);
            }
            case SETTLE -> {
                for (TeamPod pod : pods) venue.setPodGate(pod.index(), false);
                domeSay("Voting has closed. §7Chickens still count during the settle.");
            }
            case DIVIDERS -> {
                venue.setDividers(1.0);
                playAll(Sound.BLOCK_GLASS_PLACE, 1f, 0.7f);
                log.event("dividers", "raised");
            }
            case TALLY -> {
                domeSay("§6Counting chickens...");
                log.tally(engine.tally());
            }
            case REVEAL -> beginReveal();
            case REMOVE -> removeWinnerVisual();
            case REFILL -> announceRefill();
            case TELEPORT -> {
                domeSay("§eTeleporting soon...");
            }
            default -> { }
        }
    }

    private void beginReveal() {
        VoteTally tally = engine.tally();
        if (tally == null) tally = engine.forceTally();
        winner = tally == null ? null : tally.winnerGameId();
        int slot = tally == null ? -1 : tally.winnerSlot();
        if (winner == null || slot < 0) {
            domeSay("§cNo game could be chosen.");
            return;
        }
        venue.reveal(slot, winner, tally);
        showCounts(tally);
        String name = McmApi.displayName(winner);
        if (engine.confirmation()) {
            domeSay("§6Confirmation Dome: §e" + name + " §fis the only game left.");
        } else {
            domeSay("§6The chosen game is...");
            if (tally.tie()) domeSay("§eVote tied! §fPicking a game from the ones voted for.");
        }
        pendingNameAt = engine.totalTick() + 10;
        Title title = Title.title(c("§6§l" + name), c("§eTeleporting soon..."),
                Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(3), Duration.ofMillis(500)));
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isVenueWorld(p.getWorld())) p.showTitle(title);
        }
        fireworkAt = engine.totalTick();
        log.winner(winner, tally);
    }

    private void announceWinnerName() {
        if (winner == null) return;
        if (engine.confirmation()) return;
        domeSay("§6§l" + McmApi.displayName(winner) + "!");
        playAll(Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    private void removeWinnerVisual() {
        VoteTally tally = engine.tally();
        if (tally == null || tally.winnerSlot() < 0) return;
        int slot = tally.winnerSlot();
        venue.setWedge(slot, Wedge.State.REMOVED, null, slot);
        venue.updateLabel(slot, null, 0, false);
        domeSay("§7" + McmApi.displayName(tally.winnerGameId()) + " has been removed from the dial.");
    }

    private void announceRefill() {
        List<String> added = engine.lastRefilled();
        if (added == null || added.isEmpty()) return;
        StringBuilder names = new StringBuilder();
        for (String id : added) {
            if (names.length() > 0) names.append("§7, §f");
            names.append(McmApi.displayName(id));
        }
        domeSay("§6New Game Added! §f" + names + " §7added to the dome.");
        for (Wedge w : dial.wedges()) {
            if (w.live()) {
                venue.setWedge(w.slot(), Wedge.State.ACTIVE, w.gameId(), w.colorIndex());
                venue.updateLabel(w.slot(), w.gameId(), 0, false);
            }
        }
    }

    private void showCounts(VoteTally tally) {
        for (VoteTally.Line line : tally.lines()) {
            if (line.gameId() == null) {
                venue.updateLabel(line.slot(), null, 0, false);
            } else {
                venue.updateLabel(line.slot(), line.gameId(), line.score(), true);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------
    // HUD
    // ---------------------------------------------------------------------------------------------

    private void updateHud(boolean force) {
        if (bar == null || engine == null) return;
        Phase p = engine.phase();
        String label = engine.confirmation() && p == Phase.OPENING ? "GAME CHOSEN IN" : p.hudLabel();
        int seconds = groupSecondsRemaining();
        int total = groupSecondsTotal();
        double progress = total <= 0 ? 0 : Math.max(0.0, Math.min(1.0, seconds / (double) total));
        bar.setProgress(progress);
        if (label != null) {
            bar.setTitle("§e§l" + label + ": §f" + mmss(seconds));
            bar.setColor(seconds <= 5 && p == Phase.VOTING ? BarColor.RED : BarColor.YELLOW);
        }
        if (force || seconds != lastHudSecond) {
            lastHudSecond = seconds;
            venue.showCountdown(seconds);
        }
    }

    private int groupSecondsRemaining() {
        Phase p = engine.phase();
        int ticks = engine.remainingTicks();
        if (p == Phase.SETTLE) ticks += engine.durationOf(Phase.DIVIDERS) + engine.durationOf(Phase.TALLY);
        else if (p == Phase.DIVIDERS) ticks += engine.durationOf(Phase.TALLY);
        else if (p == Phase.REVEAL) ticks += engine.durationOf(Phase.REMOVE) + engine.durationOf(Phase.REFILL) + engine.durationOf(Phase.TELEPORT);
        else if (p == Phase.REMOVE) ticks += engine.durationOf(Phase.REFILL) + engine.durationOf(Phase.TELEPORT);
        else if (p == Phase.REFILL) ticks += engine.durationOf(Phase.TELEPORT);
        return Math.max(0, (ticks + 19) / 20);
    }

    private int groupSecondsTotal() {
        Phase p = engine.phase();
        if (p == null) return 0;
        return switch (p) {
            case OPENING -> seconds(engine.durationOf(Phase.OPENING));
            case VOTING -> seconds(engine.durationOf(Phase.VOTING));
            case SETTLE -> seconds(engine.durationOf(Phase.SETTLE) + engine.durationOf(Phase.DIVIDERS) + engine.durationOf(Phase.TALLY));
            case DIVIDERS -> seconds(engine.durationOf(Phase.DIVIDERS) + engine.durationOf(Phase.TALLY));
            case TALLY -> seconds(engine.durationOf(Phase.TALLY));
            case REVEAL -> seconds(engine.durationOf(Phase.REVEAL) + engine.durationOf(Phase.REMOVE) + engine.durationOf(Phase.REFILL) + engine.durationOf(Phase.TELEPORT));
            case REMOVE -> seconds(engine.durationOf(Phase.REMOVE) + engine.durationOf(Phase.REFILL) + engine.durationOf(Phase.TELEPORT));
            case REFILL -> seconds(engine.durationOf(Phase.REFILL) + engine.durationOf(Phase.TELEPORT));
            case TELEPORT -> seconds(engine.durationOf(Phase.TELEPORT));
            default -> 0;
        };
    }

    private static int seconds(int ticks) { return Math.max(0, (ticks + 19) / 20); }

    static String mmss(int seconds) {
        int s = Math.max(0, seconds);
        return String.format(Locale.ROOT, "%02d:%02d", s / 60, s % 60);
    }

    // ---------------------------------------------------------------------------------------------
    // Eggs and interactions
    // ---------------------------------------------------------------------------------------------

    boolean tryThrowEgg(Player player, ItemStack item) {
        if (!running || engine == null || !eggs.isEgg(item)) return false;
        if (engine.phase() != Phase.VOTING) {
            tell(player, "§7Voting is closed.");
            return true;
        }
        TeamPod pod = podOf.get(player.getUniqueId());
        if (pod == null) {
            tell(player, "§cYou are not in a team pod.");
            return true;
        }
        if (pod.dunked()) {
            tell(player, "§cYour pod has been dunked.");
            return true;
        }
        if (pod.frozen(engine.totalTick())) {
            tell(player, "§bYour pod is frozen.");
            return true;
        }
        if (!inPod(player, pod)) {
            tell(player, "§7Return to your pod to throw.");
            return true;
        }
        Egg.Kind kind = eggs.kindOf(item);
        consumeOne(player, item);
        if (!throwEgg(player, kind, false)) {
            giveEgg(player, kind, false);
        }
        return true;
    }

    private boolean throwEgg(Player player, Egg.Kind kind, boolean extra) {
        if (!running || engine == null || engine.phase() != Phase.VOTING) return false;
        TeamPod pod = podOf.get(player.getUniqueId());
        String team = pod == null ? request.teams().getOrDefault(player.getUniqueId(), "") : pod.teamKey();
        Location c = venue.center();
        double lx = player.getLocation().getX() - (c == null ? 0 : c.getX());
        double lz = player.getLocation().getZ() - (c == null ? 0 : c.getZ());
        Egg logical = engine.throwEgg(player.getUniqueId(), team, kind, lx, lz);
        if (logical == null) return false;
        org.bukkit.entity.Egg projectile = eggs.launch(player, logical);
        if (projectile == null) {
            engine.cancelEgg(logical.id());
            return false;
        }
        thrownEggs.add(player.getUniqueId());
        log.throwEgg(logical);
        return true;
    }

    boolean usePowerUp(Player player, ItemStack item) {
        if (!running) return false;
        return powerUps.use(player, item);
    }

    /** Handle the impact of one of this session's egg projectiles. */
    boolean handleEggHit(Projectile projectile, ProjectileHitEvent event) {
        if (!running || engine == null || !eggs.owns(projectile)) return false;
        Egg logical = eggs.logical(projectile);
        Location hit = projectile.getLocation().clone();
        if (event != null) {
            BlockFace face = event.getHitBlockFace();
            if (face != null) hit.add(new Vector(face.getModX() * 0.12, face.getModY() * 0.12, face.getModZ() * 0.12));
        }
        eggs.remove(projectile);
        projectile.remove();
        if (logical == null) return true;
        Location c = venue.center();
        double lx = hit.getX() - (c == null ? 0 : c.getX());
        double lz = hit.getZ() - (c == null ? 0 : c.getZ());
        if (hit.getY() < venue.geometry().floorY() - 6) {
            engine.loseEgg(logical.id());
            return true;
        }
        Chicken chicken = engine.landEgg(logical.id(), lx, lz);
        if (chicken != null) {
            chickens.spawn(chicken, venue);
            log.hatch(chicken);
        }
        return true;
    }

    boolean ownsProjectile(Projectile projectile) { return running && eggs != null && eggs.owns(projectile); }

    boolean isVoteEntity(Entity entity) { return running && chickens != null && chickens.isVoteEntity(entity); }

    boolean isVenueWorld(World world) { return venue != null && venue.isVenueWorld(world); }

    void onJoin(Player player) {
        if (!running || player == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!running || !player.isOnline()) return;
            TeamPod pod = podOf.get(player.getUniqueId());
            if (pod == null) {
                String team = request.teams().get(player.getUniqueId());
                if (team != null && !team.isBlank()) {
                    for (TeamPod candidate : pods) {
                        if (team.equals(candidate.teamKey())) { pod = candidate; break; }
                    }
                }
                if (pod == null && !pods.isEmpty()) pod = pods.get(pods.size() - 1);
                if (pod != null) {
                    pod.addMember(player.getUniqueId());
                    podOf.put(player.getUniqueId(), pod);
                }
            }
            if (pod == null) return;
            if (!players.has(player.getUniqueId())) players.capture(player);
            players.prepare(player, venue.podSpawn(pod.index()));
            if (bar != null) bar.addPlayer(player);
            if (engine.phase() == Phase.OPENING || engine.phase() == Phase.VOTING) {
                boolean needs = !baseEggGranted.contains(player.getUniqueId())
                        || (!thrownEggs.contains(player.getUniqueId()) && !hasDomeEgg(player));
                if (needs) {
                    giveEgg(player, Egg.Kind.CHICKEN, true);
                    baseEggGranted.add(player.getUniqueId());
                }
            }
        }, 5L);
    }

    void onQuit(Player player) {
        if (bar != null && player != null) bar.removePlayer(player);
    }

    void onRespawn(Player player) {
        if (!running || player == null) return;
        TeamPod pod = podOf.get(player.getUniqueId());
        if (pod == null) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (running && player.isOnline()) player.teleport(venue.podSpawn(pod.index()));
        }, 2L);
    }

    private boolean inPod(Player player, TeamPod pod) {
        Location spawn = venue.podSpawn(pod.index());
        if (spawn == null || spawn.getWorld() == null) return true;
        if (!spawn.getWorld().equals(player.getWorld())) return false;
        if (Math.abs(spawn.getY() - player.getY()) > 4) return false;
        return spawn.distanceSquared(player.getLocation()) <= 64.0;
    }

    private void giveEgg(Player player, Egg.Kind kind, boolean base) {
        if (player == null || !player.isOnline() || eggs == null) return;
        ItemStack item = eggs.makeEgg(kind, player.getUniqueId());
        Map<Integer, ItemStack> left = player.getInventory().addItem(item);
        for (ItemStack drop : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), drop);
    }

    private void consumeOne(Player player, ItemStack item) {
        if (item.getAmount() > 1) item.setAmount(item.getAmount() - 1);
        else player.getInventory().removeItem(item);
    }

    private boolean hasDomeEgg(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (eggs.isEgg(item)) return true;
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------
    // Admin / result
    // ---------------------------------------------------------------------------------------------

    void endVoting() {
        if (engine != null && engine.phase() == Phase.VOTING) engine.endVoting();
    }

    boolean skipWait() {
        if (!running || engine == null) return false;
        String result = engine.finishNow();
        finishOnce(result);
        return true;
    }

    /** Admin cancel: a test returns players; a real vote lets the core continue in playlist order. */
    void adminCancel() {
        if (!running) return;
        if (testMode) abort();
        else finishOnce(null);
    }

    void error(Throwable t) {
        plugin.getLogger().log(Level.SEVERE, "Decision Dome session failed", t);
        String best = null;
        try { best = engine == null ? null : engine.bestKnownResult(); } catch (Throwable ignored) { }
        finishOnce(best);
    }

    void abort() {
        if (delivered.getAndSet(true)) return;
        running = false;
        cleanup();
        controller.sessionFinished(this);
    }

    private void finishOnce(String id) {
        if (!delivered.compareAndSet(false, true)) return;
        winner = id;
        running = false;
        try { log.winner(id, engine == null ? null : engine.tally()); } catch (Throwable ignored) { }
        try { cleanup(); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "Dome cleanup failed", t); }
        controller.sessionFinished(this);
        if (callback != null) {
            try {
                callback.accept(id);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "onChosen callback threw", t);
            }
        }
    }

    private void cleanup() {
        if (cleaned) return;
        cleaned = true;
        running = false;
        if (bar != null) {
            bar.removeAll();
            bar = null;
        }
        try { if (chickens != null) chickens.removeAll(venue == null ? null : venue.world()); } catch (Throwable ignored) { }
        try { if (eggs != null) eggs.clear(); } catch (Throwable ignored) { }
        try { if (powerUps != null) powerUps.clear(); } catch (Throwable ignored) { }
        try {
            if (venue != null) {
                for (TeamPod pod : pods) {
                    venue.setPodGate(pod.index(), true);
                    venue.setPodState(pod.index(), "normal");
                }
                venue.clearRuntime();
            }
        } catch (Throwable ignored) { }
        try {
            if (venue != null && venue.world() != null) {
                String tag = DomeArena.TAG + ":" + sessionId;
                for (Entity e : venue.world().getEntities()) {
                    boolean remove = false;
                    for (String t : e.getScoreboardTags()) {
                        if (t.startsWith(tag)) { remove = true; break; }
                    }
                    if (remove) e.remove();
                }
            }
        } catch (Throwable ignored) { }
        try { players.restoreAll(testMode); } catch (Throwable ignored) { }
        if (log != null) log.event("cleanup", "winner=" + winner + " elapsed=" + (System.currentTimeMillis() - startedMillis) + "ms");
    }

    // ---------------------------------------------------------------------------------------------
    // Presentation helpers
    // ---------------------------------------------------------------------------------------------

    private void domeSay(String message) {
        Bukkit.getServer().sendMessage(c("§6[Decision Dome] §f" + message));
    }

    private void rawSay(String message) {
        Bukkit.getServer().sendMessage(c(message));
    }

    private void tell(Player player, String message) {
        if (player != null) player.sendMessage(c(message));
    }

    private void playAll(Sound sound, float volume, float pitch) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isVenueWorld(p.getWorld())) p.playSound(p.getLocation(), sound, volume, pitch);
        }
    }

    void fireworkTick() {
        if (!running || fireworkAt < 0) return;
        if (engine.totalTick() - fireworkAt > 60) { fireworkAt = -1; return; }
        if ((engine.totalTick() - fireworkAt) % 12 != 0) return;
        Location l = venue.revealLocation();
        if (l == null || l.getWorld() == null) return;
        spawnFirework(l.clone().add(0, 4, 0), winner);
    }

    private void spawnFirework(Location l, String gameId) {
        World w = l.getWorld();
        if (w == null) return;
        try {
            Firework fw = w.spawn(l, Firework.class, f -> {
                f.setPersistent(false);
                f.addScoreboardTag(DomeArena.TAG + ":" + sessionId + ":fx");
                FireworkMeta meta = f.getFireworkMeta();
                meta.addEffect(FireworkEffect.builder().withColor(Color.YELLOW, Color.ORANGE, Color.WHITE)
                        .with(FireworkEffect.Type.BALL_LARGE).withFlicker().build());
                meta.setPower(1);
                f.setFireworkMeta(meta);
            });
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (fw.isValid()) fw.detonate(); }, 12L);
        } catch (Throwable ignored) { }
    }

    String status() {
        if (engine == null) return "starting";
        VoteTally t = engine.tally();
        return "session=" + sessionId + " phase=" + engine.phase() + " tick=" + engine.totalTick()
                + " seed=" + seed + " venue=" + venue.id() + "/v" + venue.apiVersion()
                + " dial=" + dial.liveCount() + "/" + dial.size() + "+" + dial.pool().size()
                + " chickens=" + engine.chickenCount() + " eggs=" + engine.eggCount()
                + " powerups=" + (powerUps == null ? 0 : powerUps.heldCount())
                + (t == null ? "" : " tally=" + t.debug());
    }
}
