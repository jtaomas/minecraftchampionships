package com.mcm.games.parkourwarrior;

import com.mcm.games.parkourwarrior.CourseModel.Difficulty;
import com.mcm.games.parkourwarrior.CourseModel.Kind;
import com.mcm.games.parkourwarrior.CourseModel.Obstacle;

/** Coin amounts (no medals, no team multiplier): a challenge pays when it is completed; later stages and harder tracks pay more. */
final class Scoring {
    private Scoring() {}

    private static int scaled(Settings s, int base, int stage) {
        return (int) Math.round(base * (1.0 + s.stageScale * Math.max(0, stage - 1)));
    }

    /** Coins for completing an obstacle (0 for an ending part: endings pay on the finish). */
    static int coins(Settings s, Obstacle o) {
        if (o.kind() == Kind.MAIN) return scaled(s, s.mainCoins, o.stage());
        if (o.kind() == Kind.BONUS) {
            int base = o.index() == 1 ? s.bonusBronze : o.index() == 2 ? s.bonusSilver : s.bonusGold;
            return scaled(s, base, o.stage());
        }
        return 0;
    }

    /**
     * Coins for completing section {@code k} (1-based) of a linear (replica) course: coins.course.<mapId>.checkpoints[k-1] (the last
     * entry repeats), else coins.course.<mapId>.checkpoint, else the main-line default for its stage (coins.main x stage scale).
     * No difficulty branches or multipliers. Section {@code k == total} is the finish ({@link #finish}).
     */
    static int linearCheckpoint(Settings s, String mapId, int k, int perStage) {
        Settings.CourseCoins c = mapId == null ? null : s.courseCoins.get(mapId.toLowerCase(java.util.Locale.ROOT));
        if (c != null && !c.checkpoints.isEmpty()) return c.checkpoints.get(Math.min(Math.max(k, 1), c.checkpoints.size()) - 1);
        if (c != null && c.checkpoint >= 0) return c.checkpoint;
        return scaled(s, s.mainCoins, (Math.max(k, 1) - 1) / Math.max(1, perStage) + 1);
    }

    /** Coins for finishing a linear course: coins.course.<mapId>.finish, else the hard-ending value (the last section is the hardest). */
    static int finish(Settings s, String mapId) {
        Settings.CourseCoins c = mapId == null ? null : s.courseCoins.get(mapId.toLowerCase(java.util.Locale.ROOT));
        return c != null && c.finish >= 0 ? c.finish : s.endHard;
    }

    /** Coins for completing {@code o}; a linear course pays its own table (a single line of sections). */
    static int coins(Settings s, CourseModel m, Obstacle o) {
        if (m != null && m.linear != null && o.kind() == Kind.MAIN) {
            int k = (o.stage() - 1) * m.linear.perStage + o.index();
            return k >= m.linear.sectionCount() ? finish(s, m.mapId) : linearCheckpoint(s, m.mapId, k, m.linear.perStage);
        }
        return coins(s, o);
    }

    static int ending(Settings s, Difficulty d) {
        return d == Difficulty.EASY ? s.endEasy : d == Difficulty.MEDIUM ? s.endMedium : s.endHard;
    }
}
