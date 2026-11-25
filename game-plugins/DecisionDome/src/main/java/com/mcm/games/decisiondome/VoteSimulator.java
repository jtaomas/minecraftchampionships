package com.mcm.games.decisiondome;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Headless scripted vote runner used by /dome simulate and the engine test. */
final class VoteSimulator {
    record Summary(int votes, int ties, int negativeWinners, int refills, int confirmations, long millis) {
        String describe() {
            return votes + " votes in " + millis + "ms; ties=" + ties
                    + ", negative winners=" + negativeWinners + ", refills=" + refills
                    + ", confirmation dome=" + confirmations;
        }
    }

    private VoteSimulator() {}

    static Summary run(int n, long seed) {
        long start = System.currentTimeMillis();
        int ties = 0;
        int negative = 0;
        int refills = 0;
        int confirmations = 0;
        List<String> games = List.of("battlebox", "skybattle", "tgttos", "parkour", "gridrunners",
                "sands", "meltdown", "survival", "holeinthewalls", "bingo");
        for (int i = 0; i < n; i++) {
            long s = seed + i * 7919L;
            DialGeometry geo = DialGeometry.orrery();
            VoteEngine engine = new VoteEngine(games, Map.of(), 8, s, false, false, 3, true, geo);
            engine.start();
            engine.forcePhase(Phase.VOTING);
            DomeRng rng = new DomeRng(s ^ 0x51A7L);
            int mobs = 30 + rng.nextInt(31);
            for (int k = 0; k < mobs; k++) {
                double a = rng.nextDouble() * Math.PI * 2.0;
                double r = geo.innerRadius() + rng.nextDouble() * (geo.outerRadius() - geo.innerRadius());
                Egg.Kind kind;
                int roll = rng.nextInt(100);
                if (roll < 8) kind = Egg.Kind.FROG;
                else if (roll < 12) kind = Egg.Kind.MEGA_CHICKEN;
                else if (roll < 14) kind = Egg.Kind.MEGA_FROG;
                else kind = Egg.Kind.CHICKEN;
                engine.dropChicken(UUID.randomUUID(), "Team", kind, Math.cos(a) * r, Math.sin(a) * r);
            }
            engine.forcePhase(Phase.TALLY);
            VoteTally tally = engine.tally();
            if (tally != null && tally.tie()) ties++;
            if (tally != null && tally.hasWinner()) {
                VoteTally.Line line = tally.line(tally.winnerSlot());
                if (line != null && line.score() < 0) negative++;
            }
            engine.forcePhase(Phase.REMOVE);
            engine.forcePhase(Phase.REFILL);
            if (!engine.lastRefilled().isEmpty()) refills++;
        }
        // One Confirmation Dome controls the no-vote path.
        VoteEngine confirmation = new VoteEngine(List.of("battlebox"), Map.of(), 8, seed, false, false, 3, true,
                DialGeometry.orrery());
        confirmation.start();
        if (confirmation.confirmation()) confirmations++;
        long ms = System.currentTimeMillis() - start;
        return new Summary(n, ties, negative, refills, confirmations, ms);
    }
}
