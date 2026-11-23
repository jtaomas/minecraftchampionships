package com.mcm.games.decisiondome;

import com.mcm.api.McmApi;
import com.mcm.api.McmFx;
import com.mcm.api.McmLobby;
import com.mcm.api.McmVoteProvider;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.function.Consumer;
import java.util.logging.Level;

import static com.mcm.games.decisiondome.DomeArena.c;

/** The vote state machine. All methods run on the main thread. */
public final class DomeVote implements McmVoteProvider {
    enum State { IDLE, VOTING, SPINNING, ELIMINATING, ANNOUNCE }

    static final int VOTE_TICKS = 20 * 20;   // 20 seconds of voting
    static final int SPIN_TICKS = 80;         // ~4 seconds of spinning highlight
    static final int ANNOUNCE_TICKS = 70;

    private final JavaPlugin plugin;
    private final DomeArena arena;
    private final HubStage hubStage;
    /** Stage of the running vote: the MCC hub's dome venue (MCC events) or the fallback dome world. */
    private VoteStage stage;
    private boolean hubMode = false;
    // elimination (hub mode)
    private final List<Integer> elimOrder = new ArrayList<>();
    private int elimStep = 0;
    private final Random rng = new Random();

    private State state = State.IDLE;
    private boolean testMode = false;
    private List<String> ids = List.of();
    private Consumer<String> callback;          // nulled the moment it is used or cancelled
    private BukkitTask task;
    private BukkitTask shortcutTask;            // for 0/1-option votes
    private BossBar bar;
    private int tick;
    private int[] votes = new int[0];
    private final Map<UUID, Location> testReturn = new HashMap<>();
    private boolean testPreferHub = false;

    // spin
    private final List<Integer> spinSteps = new ArrayList<>();
    private int spinStart, spinStep, spinLit = -1, winner = -1, endTick;

    DomeVote(JavaPlugin plugin, DomeArena arena) {
        this.plugin = plugin;
        this.arena = arena;
        this.hubStage = new HubStage(plugin);
        this.stage = arena;
    }

    State state() { return state; }
    VoteStage stage() { return stage; }
    boolean isHubMode() { return isActive() && hubMode; }
    Location hubArrival(Random r) { return hubStage.arrival(r); }
    boolean inStageWorld(World w) { return isActive() && stage != null && stage.isStageWorld(w); }
    boolean isActive() { return state != State.IDLE; }
    boolean isTest() { return testMode; }

    // ---------------- McmVoteProvider ----------------

    @Override
    public void runVote(List<String> remainingGameIds, Consumer<String> onChosen) {
        cleanup();
        testReturn.clear();
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        if (remainingGameIds != null) for (String s : remainingGameIds) if (s != null && !s.isBlank()) uniq.add(s);
        List<String> list = new ArrayList<>(uniq);
        callback = onChosen;
        testMode = false;
        if (list.size() <= 1) {
            final String only = list.isEmpty() ? null : list.get(0);
            shortcutTask = Bukkit.getScheduler().runTask(plugin, () -> { shortcutTask = null; fire(only); });
            return;
        }
        begin(list);
    }

    @Override
    public void cancelVote() {
        callback = null;
        cleanup();
        testReturn.clear();
    }

    // ---------------- admin ----------------

    /** Demo vote that only announces the result. Returns false if a real vote is running. */
    boolean startTest(List<String> list, boolean preferHub) {
        if (isActive() && !testMode) return false;
        cleanup();
        testReturn.clear();
        for (Player p : Bukkit.getOnlinePlayers()) testReturn.put(p.getUniqueId(), p.getLocation().clone());
        callback = null;
        testMode = true;
        testPreferHub = preferHub;
        begin(new ArrayList<>(new LinkedHashSet<>(list)));
        return true;
    }

    /** /dome cancel: test -> stop and send players back; real vote -> let the core continue in playlist order. */
    void adminCancel() {
        if (!isActive()) return;
        if (testMode) {
            cleanup();
            returnTestPlayers();
        } else {
            Consumer<String> cb = callback;
            callback = null;
            cleanup();
            broadcast("§b[Dome] §7Vote cancelled by an admin; continuing in playlist order.");
            if (cb != null) safeAccept(cb, null);
        }
    }

    /** /dome end: finish the voting phase now. */
    void endVotingNow() {
        if (state == State.VOTING) tick = VOTE_TICKS;
    }

    void shutdown() {
        callback = null;
        cleanup();
        testReturn.clear();
    }

    // ---------------- flow ----------------

    private void begin(List<String> list) {
        // MCC events (and hub tests) vote in the core's Decision Dome venue; everything else uses the fallback dome world
        hubMode = HubStage.available() && (testMode ? testPreferHub : McmLobby.isMccEvent());
        stage = hubMode ? hubStage : arena;
        if (list.size() > stage.maxOptions()) list = new ArrayList<>(list.subList(0, stage.maxOptions()));
        if (hubMode && !stage.prepare(list)) {
            hubMode = false;
            stage = arena;
            if (list.size() > stage.maxOptions()) list = new ArrayList<>(list.subList(0, stage.maxOptions()));
        }
        if (!hubMode && !stage.prepare(list)) {
            plugin.getLogger().warning("Dome world unavailable; picking randomly.");
            String pick = list.isEmpty() ? null : list.get(rng.nextInt(list.size()));
            shortcutTask = Bukkit.getScheduler().runTask(plugin, () -> { shortcutTask = null; finishWith(pick); });
            state = State.ANNOUNCE; // so isActive() is true until the shortcut runs
            return;
        }
        if (list.size() > stage.maxOptions()) list = new ArrayList<>(list.subList(0, stage.maxOptions()));
        ids = list;
        votes = new int[ids.size()];

        bar = Bukkit.createBossBar("§e§lDECISION DOME §7- stand on a game to vote!", BarColor.YELLOW, BarStyle.SEGMENTED_20);
        bar.setProgress(1.0);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bringIn(p);
        }
        broadcast("§b§l[Decision Dome] §fStand on the game you want to play next! §7(" + VOTE_TICKS / 20 + "s)");
        broadcast("§7More players on a platform = a better chance it gets picked.");
        state = State.VOTING;
        tick = 0;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSafe, 1L, 1L);
    }

    /** Teleport a player into the dome hub and prepare them. */
    void bringIn(Player p) {
        if (!p.isOnline() || stage == null || stage.world() == null) return;
        if (p.isInsideVehicle()) p.leaveVehicle();
        if (!p.isDead()) p.teleport(stage.arrival(rng));
        p.setGameMode(GameMode.ADVENTURE);
        p.setFallDistance(0f);
        p.setFireTicks(0);
        p.setFoodLevel(20);
        AttributeInstance max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (max != null && !p.isDead()) p.setHealth(Math.max(1.0, max.getValue()));
        if (bar != null) bar.addPlayer(p);
        p.showTitle(Title.title(c("§e§lDECISION DOME"), c("§fStand on the game you want to play!"),
                Title.Times.times(Duration.ofMillis(250), Duration.ofSeconds(2), Duration.ofMillis(500))));
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.4f);
    }

    void onJoin(Player p) {
        if (!isActive()) return;
        // the core teleports joiners to the lobby on join; pull them in shortly after
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isActive() && p.isOnline()) {
                if (testMode) testReturn.putIfAbsent(p.getUniqueId(), p.getLocation().clone());
                bringIn(p);
            }
        }, 5L);
    }

    void onQuit(Player p) {
        if (bar != null) bar.removePlayer(p);
        testReturn.remove(p.getUniqueId());
    }

    private void tickSafe() {
        try {
            tick();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Decision Dome tick failed; picking a game now", t);
            String pick = ids.isEmpty() ? null : ids.get(rng.nextInt(ids.size()));
            finishWith(pick);
        }
    }

    private void tick() {
        stage.tickVisuals(tick);
        switch (state) {
            case VOTING -> tickVoting();
            case SPINNING -> tickSpinning();
            case ELIMINATING -> tickEliminating();
            case ANNOUNCE -> tickAnnounce();
            default -> { }
        }
        tick++;
    }

    /** Voting length: 20 s, or 1 s in the admin's fast-test mode. */
    private static int voteTicks() { return (int) FastTest.ticks(VOTE_TICKS); }

    private void tickVoting() {
        int left = voteTicks() - tick;
        if (tick % 5 == 0 || left <= 0) {
            countVotes();
            for (int i = 0; i < ids.size(); i++) stage.updateLabel(i, ids.get(i), votes[i], false);
            stage.setCountdown(Math.max(0, (left + 19) / 20));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!stage.isStageWorld(p.getWorld())) continue;
                int w = stage.optionAt(p.getLocation());
                if (w >= 0 && w < ids.size()) {
                    p.sendActionBar(c("§aVoting for " + DomeArena.COLOR_CODES[w % DomeArena.COLOR_CODES.length] + "§l"
                            + McmApi.displayName(ids.get(w)) + " §7(" + votes[w] + ")"));
                } else {
                    p.sendActionBar(c("§7Not voting - §fstep onto a platform!"));
                }
            }
        }
        if (bar != null) {
            bar.setProgress(Math.max(0.0, Math.min(1.0, left / (double) voteTicks())));
            if (tick % 20 == 0) {
                int secs = Math.max(0, (left + 19) / 20);
                bar.setTitle("§e§lDECISION DOME §7- §f" + secs + "s §7to vote");
                bar.setColor(secs <= 5 ? BarColor.RED : BarColor.YELLOW);
                if (secs <= 5 && secs > 0) {
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.showTitle(Title.title(c((secs <= 3 ? "§c§l" : "§e§l") + secs), c("§7Lock in your vote!"),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(150))));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.8f + (5 - secs) * 0.15f);
                    }
                } else if (secs == 10) {
                    broadcast("§b[Dome] §f10 seconds left to vote!");
                }
            }
        }
        if (left <= 0) startSpin();
    }

    private void countVotes() {
        Arrays.fill(votes, 0);
        World w = stage.world();
        if (w == null) return;
        for (Player p : w.getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
            int idx = stage.optionAt(p.getLocation());
            if (idx >= 0 && idx < votes.length) votes[idx]++;
        }
    }

    private void startSpin() {
        countVotes();
        int total = 0;
        for (int v : votes) total += v;
        if (total == 0) {
            winner = rng.nextInt(ids.size());
        } else {
            int roll = rng.nextInt(total);
            winner = 0;
            for (int i = 0; i < votes.length; i++) {
                roll -= votes[i];
                if (roll < 0) { winner = i; break; }
            }
        }
        StringBuilder sb = new StringBuilder("§b[Dome] §fVotes: ");
        for (int i = 0; i < ids.size(); i++) {
            if (i > 0) sb.append("§7, ");
            sb.append(DomeArena.COLOR_CODES[i % DomeArena.COLOR_CODES.length]).append(McmApi.displayName(ids.get(i)))
                    .append(" §f").append(votes[i]);
        }
        broadcast(sb.toString());
        if (total == 0) broadcast("§b[Dome] §7Nobody voted - the dome will pick at random!");
        stage.setCountdown(-1);
        if (FastTest.on()) { announce(); return; }   // admin fast-test: no spin / elimination show
        if (hubMode) { startElimination(); return; }

        // build a decelerating schedule of highlight steps that ends on the winner
        spinSteps.clear();
        double t = 0, d = 1.6;
        while (t < SPIN_TICKS) {
            spinSteps.add((int) Math.round(t));
            t += d;
            d *= 1.10;
        }
        int n = ids.size();
        int k = spinSteps.size();
        spinStart = Math.floorMod(winner - (k - 1), n);
        spinStep = 0;
        spinLit = -1;
        endTick = spinSteps.get(k - 1) + 15;
        if (bar != null) {
            bar.setTitle("§d§lPICKING THE NEXT GAME...");
            bar.setColor(BarColor.PURPLE);
            bar.setProgress(1.0);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (stage.isStageWorld(p.getWorld())) p.sendActionBar(c("§dThe dome is deciding..."));
        }
        state = State.SPINNING;
        tick = -1; // incremented to 0 at the end of this tick
    }

    /** Hub mode: knock options out one by one (never the pre-drawn winner), then reveal on the podium. */
    private void startElimination() {
        elimOrder.clear();
        for (int i = 0; i < ids.size(); i++) if (i != winner) elimOrder.add(i);
        // least-voted options go first; ties in random order
        Collections.shuffle(elimOrder, rng);
        elimOrder.sort(Comparator.comparingInt(i -> votes[i]));
        elimStep = 0;
        if (bar != null) {
            bar.setTitle("§d§lELIMINATING...");
            bar.setColor(BarColor.PURPLE);
            bar.setProgress(1.0);
        }
        McmFx.title(Bukkit.getOnlinePlayers(), "§d§lTHE DOME DECIDES", "§7Watch the pads...", 5, 30, 10);
        state = State.ELIMINATING;
        tick = -1;
    }

    private void tickEliminating() {
        int total = elimOrder.size();
        // pace: slow at first, faster in the middle, a dramatic pause before the last one
        int interval = total <= 1 ? 30 : Math.max(10, 26 - elimStep * 2);
        if (elimStep == total - 1) interval = 40;
        if (tick >= 25 && tick % interval == 0 && elimStep < total) {
            int idx = elimOrder.get(elimStep++);
            stage.eliminate(idx, ids.get(idx));
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!stage.isStageWorld(p.getWorld())) continue;
                p.sendActionBar(c("§8✖ §7" + McmApi.displayName(ids.get(idx)) + " §8is out §7(" + (total - elimStep + 1) + " left)"));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f + 0.05f * elimStep);
            }
            if (bar != null) bar.setProgress(Math.max(0.0, 1.0 - elimStep / (double) Math.max(1, total)));
            tick = 0;
            return;
        }
        if (elimStep >= total && tick >= 20) announce();
    }

    private void tickSpinning() {
        int n = ids.size();
        while (spinStep < spinSteps.size() && tick >= spinSteps.get(spinStep)) {
            int idx = Math.floorMod(spinStart + spinStep, n);
            if (spinLit >= 0) stage.highlight(spinLit, false);
            stage.highlight(idx, true);
            spinLit = idx;
            float pitch = 0.6f + 1.2f * spinStep / Math.max(1, spinSteps.size() - 1);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (stage.isStageWorld(p.getWorld())) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, pitch);
            }
            spinStep++;
        }
        if (bar != null) bar.setProgress(Math.max(0.0, 1.0 - tick / (double) endTick));
        if (tick >= endTick) {
            announce();
        }
    }

    private void announce() {
        if (spinLit >= 0) stage.highlight(spinLit, false);
        String id = ids.get(winner);
        String name = McmApi.displayName(id);
        stage.winner(winner, id, votes[winner]);
        broadcast("§6§l[Decision Dome] §e" + name + " §fwas chosen" + (testMode ? " §7(test vote - nothing will start)" : "!"));
        McmFx.title(Bukkit.getOnlinePlayers(), "§6§l" + name, testMode ? "§7was chosen (test)" : "§eis the next game!", 4, 56, 10);
        McmFx.sound(Bukkit.getOnlinePlayers(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        if (bar != null) {
            bar.setTitle("§6§l" + name + " §ewins the vote!");
            bar.setColor(BarColor.YELLOW);
            bar.setProgress(1.0);
        }
        state = State.ANNOUNCE;
        tick = -1;
    }

    private void tickAnnounce() {
        if (tick % 12 == 0 && tick <= 36) {
            if (hubMode) {
                Location l = tick % 24 == 0 ? stage.revealLocation() : stage.optionLocation(winner);
                if (l != null) McmFx.teamFirework(l.clone().add(0, 3, 0), HubStage.DYES[winner % HubStage.DYES.length].getColor(), true);
            } else spawnFirework(stage.optionLocation(winner));
        }
        if (tick >= FastTest.ticks(ANNOUNCE_TICKS)) {
            finishWith(ids.get(winner));
        }
    }

    private void spawnFirework(Location l) {
        World w = l.getWorld();
        if (w == null) return;
        try {
            Firework fw = w.spawn(l, Firework.class, f -> {
                f.setPersistent(false);
                f.addScoreboardTag(DomeArena.TAG);
                FireworkMeta fm = f.getFireworkMeta();
                fm.addEffect(FireworkEffect.builder().withColor(Color.YELLOW, Color.ORANGE, Color.WHITE)
                        .with(FireworkEffect.Type.BALL_LARGE).withFlicker().build());
                fm.setPower(1);
                f.setFireworkMeta(fm);
            });
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (fw.isValid()) fw.detonate(); }, 12L);
        } catch (Throwable ignored) { }
    }

    // ---------------- admin testing hook ----------------

    /**
     * Admin "skip the wait" (core /mcm skipwait, if it forwards to the vote provider): while voting, the option with
     * the most votes right now wins (ties / no votes: random among the leaders) with no spin; during the spin,
     * elimination or announcement, jump to the result now. Returns true if something was skipped.
     */
    public boolean skipWait() {
        switch (state) {
            case VOTING -> {
                if (ids.isEmpty()) return false;
                countVotes();
                int best = -1;
                List<Integer> leaders = new ArrayList<>();
                for (int i = 0; i < votes.length; i++) {
                    if (votes[i] > best) { best = votes[i]; leaders.clear(); }
                    if (votes[i] == best) leaders.add(i);
                }
                winner = leaders.get(rng.nextInt(leaders.size()));
                stage.setCountdown(-1);
                broadcast("§b[Dome] §7Voting ended by an admin; the current leader wins.");
                announce();
                return true;
            }
            case SPINNING, ELIMINATING -> {
                if (winner < 0 || winner >= ids.size()) return false;
                announce();
                return true;
            }
            case ANNOUNCE -> {
                if (shortcutTask != null || winner < 0 || winner >= ids.size()) return false;
                finishWith(ids.get(winner));
                return true;
            }
            default -> { return false; }
        }
    }

    /** End of a vote (normal or error path): clean up, then hand the result to the core exactly once. */
    private void finishWith(String id) {
        boolean wasTest = testMode;
        Consumer<String> cb = callback;
        callback = null;
        cleanup();
        if (wasTest) {
            Bukkit.getScheduler().runTaskLater(plugin, this::returnTestPlayers, 20L);
        } else if (cb != null) {
            safeAccept(cb, id);
        }
    }

    private void fire(String id) {
        Consumer<String> cb = callback;
        callback = null;
        if (cb != null) safeAccept(cb, id);
    }

    private void safeAccept(Consumer<String> cb, String id) {
        try {
            cb.accept(id);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "onChosen callback threw", t);
        }
    }

    private void returnTestPlayers() {
        if (isActive()) { testReturn.clear(); return; } // a new vote started meanwhile
        for (Map.Entry<UUID, Location> e : testReturn.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            Location l = e.getValue();
            if (p == null || !p.isOnline() || l == null || l.getWorld() == null) continue;
            if (arena.isDomeWorld(p.getWorld()) || hubStage.isStageWorld(p.getWorld())) p.teleport(l);
        }
        testReturn.clear();
    }

    /** Stop everything (tasks, bar, labels, highlights). Never calls the callback. Safe to call any time. */
    private void cleanup() {
        if (task != null) { task.cancel(); task = null; }
        if (shortcutTask != null) { shortcutTask.cancel(); shortcutTask = null; }
        if (bar != null) { bar.removeAll(); bar = null; }
        try { if (arena.world() != null) arena.clear(); } catch (Throwable ignored) { }
        try { if (hubMode || HubStage.available()) hubStage.clear(); } catch (Throwable ignored) { }
        elimOrder.clear();
        hubMode = false;
        spinSteps.clear();
        spinLit = -1;
        state = State.IDLE;
        testMode = false;
        ids = List.of();
    }

    private static void broadcast(String msg) {
        Bukkit.getServer().sendMessage(c(msg));
    }
}
