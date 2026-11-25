package com.mcm.games.decisiondome;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plain-Java proof of the vote engine. Run with:
 *   java -cp target/classes;target/test-classes com.mcm.games.decisiondome.VoteEngineTest
 * (On Linux/macOS use ':' instead of ';' in the classpath.)
 */
public final class VoteEngineTest {
    private static int passed;
    private static int failed;
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public static void main(String[] args) {
        geometry();
        dialRefill();
        tallyPlurality();
        tallyNegative();
        tieLeadersOnly();
        tieUniformAcrossSeeds();
        megaValues();
        removalAndRefill();
        phaseClock();
        removedWedgeIgnored();
        ceiling();
        confirmationDome();
        zeroVotes();
        audienceInverted();
        eggLifecycle();
        determinism();
        removalDropsWinnerChickens();
        simulator();
        hubDialV6();

        System.out.println();
        System.out.println("RESULT: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // ---------------------------------------------------------------------------------------------
    // Cases
    // ---------------------------------------------------------------------------------------------

    private static void geometry() {
        DialGeometry g = DialGeometry.orrery();
        for (int i = 0; i < 8; i++) {
            double[] p = point(g, i, 8, 9.5);
            int slot = g.wedgeAt(p[0], p[1], 8);
            check("geometry 8-wedge centre " + i, slot == i, "got " + slot);
        }
        double boundary = g.boundaryAngle(0, 8);
        double bx = Math.cos(boundary) * 10.0, bz = Math.sin(boundary) * 10.0;
        int a = g.wedgeAt(bx, bz, 8);
        int b = g.wedgeAt(bx, bz, 8);
        check("geometry boundary stable", a == b && (a == 0 || a == 7), "got " + a + "/" + b);
        check("geometry hub excluded", g.wedgeAt(3.0, 0.0, 8) == -1, "hub should be -1");
        check("geometry outside excluded", g.wedgeAt(13.0, 0.0, 8) == -1, "outside should be -1");
        for (int i = 0; i < 6; i++) {
            double[] p = point(g, i, 6, 10.0);
            check("geometry 6-wedge centre " + i, g.wedgeAt(p[0], p[1], 6) == i, "wrong slot");
        }
    }

    private static void dialRefill() {
        List<String> games = games(10);
        Dial dial = new Dial(games, 8, DialGeometry.orrery());
        check("dial 8 live from 10", dial.liveCount() == 8 && dial.pool().size() == 2,
                "live=" + dial.liveCount() + " pool=" + dial.pool().size());
        String removed = dial.removeWinner(2);
        check("dial removal blanks slot", removed != null && !dial.isLive(2) && dial.wedge(2).state() == Wedge.State.REMOVED,
                "state=" + dial.wedge(2).state());
        List<String> added = dial.refill();
        check("dial refill fills blank", added.size() == 1 && dial.isLive(2) && !removed.equals(dial.wedge(2).gameId()),
                "added=" + added + " now=" + dial.wedge(2).gameId());
        check("dial keeps slot index", dial.wedge(2).slot() == 2, "slot moved");
    }

    private static void tallyPlurality() {
        VoteEngine e = engine(8, 1L);
        e.forcePhase(Phase.VOTING);
        drop(e, 0, Egg.Kind.CHICKEN, 3);
        drop(e, 1, Egg.Kind.CHICKEN, 5);
        drop(e, 0, Egg.Kind.FROG, 2);
        VoteTally t = e.forceTally();
        check("plurality most chickens wins", t.winnerSlot() == 1, t.debug());
        check("plurality counts signed", t.line(0).score() == 1 && t.line(1).score() == 5, t.debug());
        check("plurality counts every chicken", t.countedChickens() == 10, t.debug());
        check("plurality tracks frogs", t.frogCount() == 2, t.debug());
    }

    private static void tallyNegative() {
        VoteEngine e = engine(8, 2L);
        e.forcePhase(Phase.VOTING);
        for (int i = 0; i < 8; i++) drop(e, i, Egg.Kind.FROG, 1);
        VoteTally t = e.forceTally();
        check("negative totals legal", t.line(0).score() == -1, t.debug());
        VoteTally.Line winner = t.line(t.winnerSlot());
        check("least-negative wedge can win", winner != null && winner.score() == -1, t.debug());

        VoteEngine one = engine(8, 3L);
        one.forcePhase(Phase.VOTING);
        drop(one, 2, Egg.Kind.FROG, 1);
        VoteTally t2 = one.forceTally();
        check("one frog is recorded as -1", t2.line(2).score() == -1, t2.debug());
        check("zero beats a single negative", t2.line(t2.winnerSlot()).score() == 0, t2.debug());
    }

    private static void tieLeadersOnly() {
        VoteEngine e = engine(8, 4L);
        e.forcePhase(Phase.VOTING);
        drop(e, 0, Egg.Kind.CHICKEN, 5);
        drop(e, 1, Egg.Kind.CHICKEN, 5);
        drop(e, 2, Egg.Kind.CHICKEN, 3);
        VoteTally t = e.forceTally();
        check("tie detected", t.tie(), t.debug());
        check("tie leaders only", t.leaders().size() == 2 && t.leaders().contains(0) && t.leaders().contains(1), t.debug());
        check("tie winner is a leader", t.winnerSlot() == 0 || t.winnerSlot() == 1, t.debug());
    }

    private static void tieUniformAcrossSeeds() {
        boolean saw0 = false, saw1 = false;
        for (int seed = 0; seed < 30; seed++) {
            VoteEngine e = engine(8, 1000L + seed);
            e.forcePhase(Phase.VOTING);
            drop(e, 0, Egg.Kind.CHICKEN, 5);
            drop(e, 1, Egg.Kind.CHICKEN, 5);
            drop(e, 2, Egg.Kind.CHICKEN, 3);
            VoteTally t = e.forceTally();
            if (t.winnerSlot() == 0) saw0 = true;
            if (t.winnerSlot() == 1) saw1 = true;
            if (t.winnerSlot() != 0 && t.winnerSlot() != 1) {
                check("tie never picks a non-leader", false, "seed " + seed + " winner " + t.winnerSlot());
                return;
            }
        }
        check("tie is uniform among leaders", saw0 && saw1, "saw0=" + saw0 + " saw1=" + saw1);
    }

    private static void megaValues() {
        VoteEngine e = engine(8, 5L);
        e.forcePhase(Phase.VOTING);
        drop(e, 3, Egg.Kind.MEGA_CHICKEN, 1);
        drop(e, 3, Egg.Kind.MEGA_FROG, 1);
        drop(e, 4, Egg.Kind.MEGA_CHICKEN, 2);
        VoteTally t = e.forceTally();
        check("mega chicken +3 / mega frog -3", t.line(3).score() == 0, t.debug());
        check("mega chickens add three", t.line(4).score() == 6, t.debug());
        check("mega count", t.megaCount() == 4, t.debug());
    }

    private static void removalAndRefill() {
        VoteEngine e = engine(10, 6L);
        e.forcePhase(Phase.VOTING);
        drop(e, 1, Egg.Kind.CHICKEN, 4);
        VoteTally t = e.forceTally();
        check("removal setup winner", t.winnerSlot() == 1, t.debug());
        String removed = e.dial().wedge(1).gameId();
        e.forcePhase(Phase.REVEAL);
        e.forcePhase(Phase.REMOVE);
        check("winner removed from dial", !e.dial().isLive(1), "slot 1 still live");
        check("removed game recorded", removed.equals(e.lastRemovedGame()), e.lastRemovedGame());
        e.forcePhase(Phase.REFILL);
        check("refill adds a new game", e.dial().isLive(1), "slot 1 not refilled");
        check("refill is announced list", e.lastRefilled().size() == 1, "added=" + e.lastRefilled());
        check("refill picks the pool game", !removed.equals(e.dial().wedge(1).gameId()), "same game returned");
    }

    private static void phaseClock() {
        VoteEngine normal = new VoteEngine(games(8), Map.of(), 8, 20L, false, false, 3, false,
                DialGeometry.orrery());
        check("phase opening 5s", normal.durationOf(Phase.OPENING) == 100, "" + normal.durationOf(Phase.OPENING));
        check("phase voting 30s", normal.durationOf(Phase.VOTING) == 600, "" + normal.durationOf(Phase.VOTING));
        check("phase settle 13s", normal.durationOf(Phase.SETTLE) == 260, "" + normal.durationOf(Phase.SETTLE));
        check("phase dividers 1.5s", normal.durationOf(Phase.DIVIDERS) == 30, "" + normal.durationOf(Phase.DIVIDERS));
        check("phase tally 1s", normal.durationOf(Phase.TALLY) == 20, "" + normal.durationOf(Phase.TALLY));
        check("phase reveal 5s", normal.durationOf(Phase.REVEAL) == 100, "" + normal.durationOf(Phase.REVEAL));
        check("phase remove 2s", normal.durationOf(Phase.REMOVE) == 40, "" + normal.durationOf(Phase.REMOVE));
        check("phase refill 2s", normal.durationOf(Phase.REFILL) == 40, "" + normal.durationOf(Phase.REFILL));
        check("phase teleport 8s", normal.durationOf(Phase.TELEPORT) == 160, "" + normal.durationOf(Phase.TELEPORT));
        int total = 0;
        for (Phase p : List.of(Phase.OPENING, Phase.VOTING, Phase.SETTLE, Phase.DIVIDERS, Phase.TALLY,
                Phase.REVEAL, Phase.REMOVE, Phase.REFILL, Phase.TELEPORT)) total += normal.durationOf(p);
        check("phase total 67.5s", total == 1350, "total=" + total);

        VoteEngine fast = new VoteEngine(games(8), Map.of(), 8, 21L, false, false, 3, true,
                DialGeometry.orrery());
        for (Phase p : List.of(Phase.OPENING, Phase.VOTING, Phase.SETTLE, Phase.DIVIDERS, Phase.TALLY,
                Phase.REVEAL, Phase.REMOVE, Phase.REFILL, Phase.TELEPORT)) {
            check("fast phase <=1s " + p, fast.durationOf(p) <= 20, "" + fast.durationOf(p));
        }
    }

    private static void removedWedgeIgnored() {
        Dial dial = new Dial(games(8), 8, DialGeometry.orrery());
        dial.removeWinner(2);
        double[] p = point(DialGeometry.orrery(), 2, 8, 9.5);
        Chicken c = new Chicken(UUID.randomUUID(), OWNER, "Test", Egg.Kind.CHICKEN, 0, p[0], p[1], 1L);
        VoteTally t = VoteTally.count(dial, List.of(c), new DomeRng(1L), false);
        check("votes on a removed wedge are ignored", t.line(2).score() == 0 && t.ignoredRemoved() == 1, t.debug());
        check("removed-wedge-only round still picks a live game", t.hasWinner(), t.debug());
    }

    private static void ceiling() {
        Dial dial = new Dial(games(20), 8, DialGeometry.orrery());
        check("15-game ceiling", dial.allGames().size() == 15, "games=" + dial.allGames().size());
    }

    private static void confirmationDome() {
        VoteEngine e = new VoteEngine(List.of("battlebox"), Map.of(), 8, 7L, false, false, 8, true,
                DialGeometry.orrery());
        e.start();
        check("confirmation dome detected", e.confirmation(), "not confirmation");
        e.tick(100);
        check("confirmation ends", e.finished(), "still " + e.phase());
        check("confirmation never votes", !e.phaseHistory().contains(Phase.VOTING), e.phaseHistory().toString());
        check("confirmation picks the only game", "battlebox".equals(e.winnerGame()), "winner=" + e.winnerGame());
    }

    private static void zeroVotes() {
        VoteEngine e = engine(8, 8L);
        e.forcePhase(Phase.VOTING);
        VoteTally t = e.forceTally();
        check("zero votes flagged", t.emptyVotes(), t.debug());
        check("zero votes picks a live wedge", t.hasWinner(), t.debug());
        check("zero votes tie among all live", t.leaders().size() == 8, t.debug());
    }

    private static void audienceInverted() {
        VoteEngine e = new VoteEngine(List.of("a", "b"), Map.of(), 8, 9L, false, true, 5, true,
                DialGeometry.orrery());
        e.forcePhase(Phase.VOTING);
        drop(e, 0, Egg.Kind.CHICKEN, 6);
        drop(e, 1, Egg.Kind.CHICKEN, 1);
        VoteTally t = e.forceTally();
        check("audience vote is inverted", t.winnerSlot() == 1, t.debug());
        check("audience flag recorded", t.audience(), t.debug());
    }

    private static void eggLifecycle() {
        VoteEngine e = engine(8, 10L);
        e.forcePhase(Phase.VOTING);
        Egg egg = e.throwEgg(OWNER, "Red", Egg.Kind.MEGA_FROG, 1.0, 1.0);
        check("egg created while voting", egg != null, "null egg");
        e.forcePhase(Phase.SETTLE);
        check("no new throws in settle", e.throwEgg(OWNER, "Red", Egg.Kind.CHICKEN, 1, 1) == null, "settle accepted throw");
        Chicken c = e.landEgg(egg.id(), 9.0, 0.0);
        check("in-flight egg resolves in settle", c != null && c.value() == -3, "hatch failed");
        check("one egg one chicken", e.chickenCount() == 1, "count=" + e.chickenCount());
    }

    private static void determinism() {
        VoteEngine a = engine(8, 11L);
        VoteEngine b = engine(8, 11L);
        for (VoteEngine e : List.of(a, b)) {
            e.forcePhase(Phase.VOTING);
            drop(e, 0, Egg.Kind.CHICKEN, 4);
            drop(e, 3, Egg.Kind.FROG, 2);
            drop(e, 5, Egg.Kind.MEGA_CHICKEN, 1);
            e.tick(40);
        }
        VoteTally ta = a.forceTally();
        VoteTally tb = b.forceTally();
        check("deterministic scores", ta.scoreList().equals(tb.scoreList()), ta.scoreList() + " vs " + tb.scoreList());
        check("deterministic winner", ta.winnerSlot() == tb.winnerSlot(), ta.debug() + " vs " + tb.debug());
        List<String> pa = positions(a), pb = positions(b);
        check("deterministic movement", pa.equals(pb), pa + " vs " + pb);
    }

    private static void removalDropsWinnerChickens() {
        VoteEngine e = engine(8, 12L);
        e.forcePhase(Phase.VOTING);
        drop(e, 0, Egg.Kind.CHICKEN, 4);
        drop(e, 1, Egg.Kind.CHICKEN, 1);
        int before = e.chickenCount();
        VoteTally t = e.forceTally();
        e.forcePhase(Phase.REVEAL);
        e.forcePhase(Phase.REMOVE);
        int after = e.chickenCount();
        check("winner wedge chickens removed", after == before - 4, "before=" + before + " after=" + after + " " + t.debug());
    }

    private static void simulator() {
        VoteSimulator.Summary s = VoteSimulator.run(100, 0xD0DE5EEDL);
        System.out.println("   simulate: " + s.describe());
        check("simulator runs 100 votes", s.votes() == 100, s.describe());
        check("simulator sees a tie or a negative winner", s.ties() > 0 || s.negativeWinners() > 0, s.describe());
    }

    /** v6 hub dial (r 1 .. 8.3, wedge 0 at 67.5 deg): chickens never walk off the band; eggs hatch once; slots persist. */
    private static void hubDialV6() {
        DialGeometry g = DialGeometry.of(66.5, 1.0, 8.3, Math.toRadians(67.5));
        VoteEngine e = new VoteEngine(games(8), Map.of(), 8, 77L, false, false, 2, false, g);
        e.start();
        e.forcePhase(Phase.VOTING);
        List<Egg> thrown = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            Egg egg = e.throwEgg(UUID.randomUUID(), "Red", Egg.Kind.CHICKEN, 0, 0);
            double a = i * 0.7, r = 7.9 + (i % 4) * 0.6;   // some land on the rim, past the band
            e.landEgg(egg.id(), Math.cos(a) * r, Math.sin(a) * r);
            thrown.add(egg);
        }
        check("v6 egg hatches only once", e.landEgg(thrown.get(0).id(), 1, 1) == null, "second hatch");
        for (int t = 0; t < 600; t++) e.tick();
        int off = 0;
        for (Chicken c : e.chickens()) if (c.alive() && e.dial().wedgeAt(c.x(), c.z()) < 0) off++;
        check("v6 chickens stay on the wedge band", off == 0, off + " chickens off the dial");
        int counted = 0;
        VoteTally tally = e.forceTally();
        for (Wedge w : e.dial().wedges()) counted += w.chickens();
        check("v6 every chicken is counted", counted == 40, "counted=" + counted + " " + tally);

        List<String> board = new ArrayList<>(games(8));
        board.set(3, null);                      // game 3 was played last time
        List<String> remaining = new ArrayList<>(games(10));
        remaining.remove(games(8).get(3));
        List<String> added = new ArrayList<>();
        Dial keep = Dial.fromLayout(board, remaining, 8, false, g, added);
        check("v6 board keeps slots", games(8).get(5).equals(keep.wedge(5).gameId()) && keep.wedge(3).gameId() == null
                && added.isEmpty(), keep.layout().toString());
        Dial refill = Dial.fromLayout(board, remaining, 8, true, g, added);
        check("v6 refill fills the played slot", refill.wedge(3).gameId() != null && added.size() == 1
                && added.get(0).equals(refill.wedge(3).gameId()) && games(8).get(7).equals(refill.wedge(7).gameId()),
                refill.layout() + " added=" + added);
    }

    // ---------------------------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------------------------

    private static VoteEngine engine(int gameCount, long seed) {
        return new VoteEngine(games(gameCount), Map.of(), 8, seed, false, false, 3, true,
                DialGeometry.orrery());
    }

    private static List<String> games(int n) {
        String[] names = {"battlebox", "skybattle", "tgttos", "parkour", "gridrunners", "sands",
                "meltdown", "survival", "holeinthewalls", "bingo", "rocket", "buildmart"};
        List<String> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(names[i % names.length] + (i >= names.length ? i : ""));
        return out;
    }

    private static void drop(VoteEngine e, int wedge, Egg.Kind kind, int count) {
        for (int i = 0; i < count; i++) {
            double[] p = point(DialGeometry.orrery(), wedge, e.dial().size(), 9.5 + (i % 3) * 0.25);
            e.dropChicken(OWNER, "Test", kind, p[0], p[1]);
        }
    }

    private static double[] point(DialGeometry g, int wedge, int wedges, double radius) {
        double a = g.centreAngle(wedge, wedges);
        return new double[]{Math.cos(a) * radius, Math.sin(a) * radius};
    }

    private static List<String> positions(VoteEngine e) {
        List<String> out = new ArrayList<>();
        for (Chicken c : e.chickens()) out.add(String.format(java.util.Locale.ROOT, "%.4f,%.4f", c.x(), c.z()));
        return out;
    }

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name + "  (" + detail + ")");
        }
    }
}
