package com.mcm.games.decisiondome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Pure-Java Decision Dome vote engine. No Bukkit types, no world access: eggs, chickens, movement,
 * the phase clock, removal, refill, tally and the tie roll all live here.
 *
 * <p>The Bukkit runtime submits throws/landings and mirrors the logical chicken positions onto real
 * entities. At tally time the logical position is authoritative; the entity is only the visual.</p>
 */
public final class VoteEngine {
    private final Dial dial;
    private final Map<UUID, String> teams;
    private final DomeRng rng;
    private final DomeRng tieRng;
    private final DomeRng idRng;
    private final boolean powerUpsAllowed;
    private final boolean audienceVote;
    private final boolean fastTest;
    private final int eventIndex;
    private final Map<Phase, Integer> overrides = new LinkedHashMap<>();

    private final Map<UUID, Egg> eggs = new LinkedHashMap<>();
    private final Map<UUID, Chicken> chickens = new LinkedHashMap<>();
    private final Map<Integer, Long> stickyUntil = new LinkedHashMap<>();

    private Phase phase = Phase.IDLE;
    private int phaseTick;
    private int totalTick;
    private boolean finished;
    private boolean tallied;
    private boolean confirmation;
    private boolean refillAfterRemoval = true;
    private long freezeAllUntil;
    private VoteTally tally;
    private String lastRemovedGame;
    private List<String> lastRefilled = List.of();
    private final List<Phase> phaseHistory = new ArrayList<>();

    public VoteEngine(List<String> gameIds, Map<UUID, String> teams, int capacity, long seed,
                      boolean powerUpsAllowed, boolean audienceVote, int eventIndex, boolean fastTest,
                      DialGeometry geometry) {
        this(new Dial(gameIds, capacity, geometry), teams, seed, powerUpsAllowed, audienceVote,
                eventIndex, fastTest);
    }

    public VoteEngine(Dial dial, Map<UUID, String> teams, long seed, boolean powerUpsAllowed,
                      boolean audienceVote, int eventIndex, boolean fastTest) {
        this.dial = dial == null ? new Dial(List.of(), 8, DialGeometry.orrery()) : dial;
        this.teams = teams == null ? Map.of() : Map.copyOf(teams);
        this.rng = new DomeRng(seed);
        this.tieRng = new DomeRng(DomeRng.mix(seed, 17, 0xD07EL));
        this.idRng = new DomeRng(DomeRng.mix(seed, 31, 0x1DL));
        this.powerUpsAllowed = powerUpsAllowed;
        this.audienceVote = audienceVote;
        this.eventIndex = eventIndex;
        this.fastTest = fastTest;
    }

    // ---------------------------------------------------------------------------------------------
    // Clock
    // ---------------------------------------------------------------------------------------------

    public void start() {
        if (phase != Phase.IDLE) return;
        phaseTick = 0;
        totalTick = 0;
        if (dial.liveCount() == 0) {
            phase = Phase.FINISHED;
            finished = true;
            phaseHistory.add(phase);
            return;
        }
        confirmation = dial.liveCount() == 1;
        setPhase(confirmation ? Phase.OPENING : Phase.OPENING);
    }

    /** Advance one server tick. Phase-entry work (tally, removal, refill) happens here. */
    public void tick() {
        if (phase == Phase.IDLE) {
            start();
            return;
        }
        if (phase == Phase.FINISHED) {
            finished = true;
            return;
        }
        totalTick++;
        phaseTick++;
        if (phase.movesChickens() && totalTick % 6 == 0) wander();
        if (phaseTick >= durationOf(phase)) advance();
    }

    public void tick(int ticks) {
        for (int i = 0; i < ticks && !finished; i++) tick();
    }

    /** Move to the next phase immediately. Used by end/skip and by tests. */
    public Phase advance() {
        if (phase == Phase.FINISHED || phase == Phase.IDLE) return phase;
        Phase next = switch (phase) {
            case OPENING -> confirmation ? Phase.REVEAL : Phase.VOTING;
            case VOTING -> Phase.SETTLE;
            case SETTLE -> Phase.DIVIDERS;
            case DIVIDERS -> Phase.TALLY;
            case TALLY -> Phase.REVEAL;
            case REVEAL -> confirmation ? Phase.TELEPORT : Phase.REMOVE;
            case REMOVE -> Phase.REFILL;
            case REFILL -> Phase.TELEPORT;
            case TELEPORT -> Phase.FINISHED;
            default -> Phase.FINISHED;
        };
        setPhase(next);
        return phase;
    }

    private void setPhase(Phase next) {
        this.phase = next;
        this.phaseTick = 0;
        this.phaseHistory.add(next);
        switch (next) {
            case DIVIDERS -> {
                for (Chicken c : chickens.values()) {
                    if (c.alive()) c.settle();
                }
            }
            case TALLY -> {
                if (tally == null) {
                    tally = VoteTally.count(dial, chickens.values(), tieRng, audienceVote);
                    tallied = true;
                }
                for (Chicken c : chickens.values()) if (c.alive()) c.count();
            }
            case REVEAL -> {
                if (tally == null) {
                    tally = VoteTally.count(dial, chickens.values(), tieRng, audienceVote);
                    tallied = true;
                }
            }
            case REMOVE -> removeWinner();
            case REFILL -> lastRefilled = refillAfterRemoval ? dial.refill() : List.of();
            case FINISHED -> finished = true;
            default -> { }
        }
    }

    public int durationOf(Phase p) {
        if (p == null) return 0;
        int base = overrides.getOrDefault(p, p.normalTicks());
        if (fastTest) base = Math.min(base, p.fastTicks());
        return Math.max(1, base);
    }

    public void overrideDuration(Phase p, int ticks) {
        if (p != null && ticks > 0) overrides.put(p, ticks);
    }

    public int remainingTicks() {
        if (phase == Phase.FINISHED || phase == Phase.IDLE) return 0;
        return Math.max(0, durationOf(phase) - phaseTick);
    }

    public int phaseTick() { return phaseTick; }

    public int totalTick() { return totalTick; }

    public Phase phase() { return phase; }

    public boolean finished() { return finished || phase == Phase.FINISHED; }

    public boolean confirmation() { return confirmation; }

    public List<Phase> phaseHistory() { return Collections.unmodifiableList(phaseHistory); }

    public void endVoting() {
        if (phase == Phase.VOTING) advance();
    }

    /** Force a tally now (admin skip). Freezes movement and counts once. */
    public VoteTally forceTally() {
        if (tally == null) {
            for (Chicken c : chickens.values()) if (c.alive()) c.settle();
            tally = VoteTally.count(dial, chickens.values(), tieRng, audienceVote);
            tallied = true;
            if (phase != Phase.FINISHED) {
                phase = Phase.TALLY;
                phaseTick = 0;
            }
        }
        return tally;
    }

    /** Finish the whole show now, keeping the current (or freshly forced) result. */
    public String finishNow() {
        forceTally();
        phase = Phase.FINISHED;
        phaseTick = 0;
        finished = true;
        return winnerGame();
    }

    /** Test/admin helper: jump forward to a target phase, running every entry action on the way. */
    public void forcePhase(Phase target) {
        if (target == null) return;
        if (phase == Phase.IDLE) start();
        if (target == Phase.VOTING) {
            confirmation = false;
            phase = Phase.VOTING;
            phaseTick = 0;
            if (!phaseHistory.contains(Phase.VOTING)) phaseHistory.add(Phase.VOTING);
            return;
        }
        if (target == Phase.TALLY) {
            forceTally();
            return;
        }
        int guard = 0;
        while (phase != target && phase != Phase.FINISHED && guard++ < 32) advance();
    }

    // ---------------------------------------------------------------------------------------------
    // Eggs and chickens
    // ---------------------------------------------------------------------------------------------

    /** Create the logical egg at throw time. Returns null if the ballot is not open. */
    public Egg throwEgg(UUID owner, String team, Egg.Kind kind, double x, double z) {
        if (phase != Phase.VOTING) return null;
        UUID id = new UUID(idRng.nextLong(), idRng.nextLong());
        Egg egg = new Egg(id, owner, team == null ? teams.getOrDefault(owner, "") : team, kind, totalTick, x, z);
        eggs.put(id, egg);
        return egg;
    }

    /**
     * Resolve an egg at its impact point. In-flight eggs are still accepted during SETTLE so a throw
     * at 0:00 is not lost. Returns the hatched logical chicken, or null.
     */
    public Chicken landEgg(UUID eggId, double x, double z) {
        Egg egg = eggId == null ? null : eggs.get(eggId);
        if (egg == null) return null;
        if (!phase.acceptsHatch()) {
            egg.lost();
            return null;
        }
        egg.land(x, z);
        Chicken chicken = newChicken(egg.owner(), egg.team(), egg.kind(), x, z);
        egg.hatched();
        return chicken;
    }

    public void loseEgg(UUID eggId) {
        Egg egg = eggs.get(eggId);
        if (egg != null) egg.lost();
    }

    public void cancelEgg(UUID eggId) {
        Egg egg = eggs.get(eggId);
        if (egg != null) egg.cancel();
    }

    /** Direct logical drop for tests and scripted simulations. */
    public Chicken dropChicken(UUID owner, String team, Egg.Kind kind, double x, double z) {
        return newChicken(owner, team, kind, x, z);
    }

    private Chicken newChicken(UUID owner, String team, Egg.Kind kind, double x, double z) {
        UUID id = new UUID(idRng.nextLong(), idRng.nextLong());
        String resolvedTeam = team == null ? teams.getOrDefault(owner, "") : team;
        long chickenSeed = DomeRng.mix(id.getMostSignificantBits(), id.getLeastSignificantBits(), kind.value());
        Chicken chicken = new Chicken(id, owner, resolvedTeam, kind, totalTick, x, z, chickenSeed);
        chickens.put(id, chicken);
        return chicken;
    }

    public Egg egg(UUID id) { return id == null ? null : eggs.get(id); }

    public Chicken chicken(UUID id) { return id == null ? null : chickens.get(id); }

    public List<Egg> eggs() { return List.copyOf(eggs.values()); }

    public List<Egg> eggsInFlight() {
        List<Egg> out = new ArrayList<>();
        for (Egg e : eggs.values()) if (e.inFlight()) out.add(e);
        return out;
    }

    public List<Chicken> chickens() { return List.copyOf(chickens.values()); }

    public int chickenCount() {
        int n = 0;
        for (Chicken c : chickens.values()) if (c.alive()) n++;
        return n;
    }

    public int eggCount() { return eggsInFlight().size(); }

    /** Mirror a visual entity's position back into the logical chicken (clamped to the venue). */
    public void updateChickenPosition(UUID chickenId, double x, double z) {
        Chicken c = chickens.get(chickenId);
        if (c == null || !c.alive()) return;
        double max = dial.geometry().outerRadius() + 4.0;
        double r = Math.hypot(x, z);
        if (r > max) {
            double s = max / r;
            x *= s;
            z *= s;
        }
        c.position(x, z);
    }

    private void wander() {
        double max = dial.geometry().outerRadius() + 4.0;
        for (Chicken c : chickens.values()) {
            if (!c.alive() || c.frozen(totalTick) || totalTick < freezeAllUntil) continue;
            int slot = dial.wedgeAt(c.x(), c.z());
            Long sticky = slot < 0 ? null : stickyUntil.get(slot);
            if (sticky != null && sticky > totalTick && c.random() < 0.75) continue;
            double step = 0.12 + c.random() * 0.42;
            double angle = c.random() * Math.PI * 2.0;
            double nx = c.x() + Math.cos(angle) * step;
            double nz = c.z() + Math.sin(angle) * step;
            double nr = Math.hypot(nx, nz);
            if (nr > max && nr > 1.0e-9) {
                nx *= max / nr;
                nz *= max / nr;
            }
            c.position(nx, nz);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Effects used by power-ups
    // ---------------------------------------------------------------------------------------------

    public boolean freezeChicken(UUID chickenId, long ticks) {
        Chicken c = chickens.get(chickenId);
        if (c == null || !c.alive()) return false;
        c.freeze(totalTick + Math.max(1, ticks));
        return true;
    }

    public void freezeAll(long ticks) {
        freezeAllUntil = Math.max(freezeAllUntil, totalTick + Math.max(1, ticks));
    }

    public void levitateWedge(int slot, long ticks) {
        for (Chicken c : chickens.values()) {
            if (c.alive() && dial.wedgeAt(c.x(), c.z()) == slot) c.levitate(totalTick + Math.max(1, ticks));
        }
    }

    public void stickyWedge(int slot, long ticks) {
        stickyUntil.merge(slot, totalTick + Math.max(1, ticks), Math::max);
    }

    public boolean swapChickens(UUID a, UUID b) {
        Chicken ca = chickens.get(a);
        Chicken cb = chickens.get(b);
        if (ca == null || cb == null || !ca.alive() || !cb.alive()) return false;
        double x = ca.x(), z = ca.z();
        ca.position(cb.x(), cb.z());
        cb.position(x, z);
        return true;
    }

    public Chicken duplicateChicken(UUID chickenId) {
        Chicken c = chickens.get(chickenId);
        if (c == null || !c.alive() || chickenCount() >= 200) return null;
        double a = rng.nextDouble() * Math.PI * 2.0;
        Chicken copy = newChicken(c.owner(), c.team(), c.kind(),
                c.x() + Math.cos(a) * 0.45, c.z() + Math.sin(a) * 0.45);
        copy.scale(c.scalePercent());
        return copy;
    }

    public boolean transmute(UUID chickenId) {
        Chicken c = chickens.get(chickenId);
        if (c == null || !c.alive()) return false;
        Egg.Kind next = switch (c.kind()) {
            case CHICKEN -> Egg.Kind.FROG;
            case FROG -> Egg.Kind.CHICKEN;
            case MEGA_CHICKEN -> Egg.Kind.MEGA_FROG;
            case MEGA_FROG -> Egg.Kind.MEGA_CHICKEN;
        };
        c.kind(next);
        return true;
    }

    public int removeChickensInWedge(int slot) {
        int removed = 0;
        for (Chicken c : chickens.values()) {
            if (c.alive() && dial.wedgeAt(c.x(), c.z()) == slot) {
                c.remove();
                removed++;
            }
        }
        return removed;
    }

    public Chicken randomChicken(DomeRng source) {
        List<Chicken> alive = new ArrayList<>();
        for (Chicken c : chickens.values()) if (c.alive()) alive.add(c);
        if (alive.isEmpty()) return null;
        return alive.get((source == null ? rng : source).nextInt(alive.size()));
    }

    public List<Integer> liveSlotsWithChickens() {
        LinkedHashSet<Integer> slots = new LinkedHashSet<>();
        for (Chicken c : chickens.values()) {
            if (!c.alive()) continue;
            int slot = dial.wedgeAt(c.x(), c.z());
            if (slot >= 0 && dial.isLive(slot)) slots.add(slot);
        }
        return new ArrayList<>(slots);
    }

    // ---------------------------------------------------------------------------------------------
    // Result access
    // ---------------------------------------------------------------------------------------------

    public Dial dial() { return dial; }

    public VoteTally tally() { return tally; }

    public boolean tallied() { return tallied; }

    public String winnerGame() {
        return tally == null ? null : tally.winnerGameId();
    }

    public String lastRemovedGame() { return lastRemovedGame; }

    public List<String> lastRefilled() { return lastRefilled; }

    /** Safe result for an error path: the tallied winner, else the first live game, else null. */
    public String bestKnownResult() {
        if (tally != null && tally.winnerGameId() != null) return tally.winnerGameId();
        List<Wedge> live = dial.liveWedges();
        return live.isEmpty() ? null : live.get(0).gameId();
    }

    private void removeWinner() {
        if (tally == null || !tally.hasWinner()) return;
        int slot = tally.winnerSlot();
        lastRemovedGame = dial.removeWinner(slot);
        for (Chicken c : chickens.values()) {
            if (!c.alive()) continue;
            if (dial.wedgeAt(c.x(), c.z()) == slot) c.remove();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Misc
    // ---------------------------------------------------------------------------------------------

    public boolean powerUpsAllowed() { return powerUpsAllowed; }

    public boolean audienceVote() { return audienceVote; }

    public int eventIndex() { return eventIndex; }

    /** The live runtime tops up the dial at the start of the refill vote; the engine test can refill on demand. */
    public void refillAfterRemoval(boolean enabled) { this.refillAfterRemoval = enabled; }

    public boolean fastTest() { return fastTest; }

    public DomeRng rng() { return rng; }

    public String seedDebug() {
        return "rng=" + rng.calls() + " tie=" + tieRng.calls() + " ids=" + idRng.calls();
    }

    public static String normaliseId(String id) {
        return id == null ? "" : id.toLowerCase(Locale.ROOT).trim();
    }

    public static List<String> normaliseIds(List<String> ids) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (ids != null) {
            for (String id : ids) {
                String n = normaliseId(id);
                if (!n.isBlank()) out.add(n);
            }
        }
        return new ArrayList<>(out);
    }
}
