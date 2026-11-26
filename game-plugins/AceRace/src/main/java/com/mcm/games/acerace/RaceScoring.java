package com.mcm.games.acerace;

import com.mcm.api.McmGameContext;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.UUID;

/**
 * MCC Ace Race coins (base x1.0; the core applies the event multiplier). See game-plugins/COINS.md.
 * Finishing pays {@code finish-first} to the 1st finisher and {@code finish-step} less to each later finisher;
 * on top, the final placement pays the {@code placement} bonus. No coins for checkpoints, laps or not finishing.
 */
public final class RaceScoring {
    int finishFirst = 400;
    int finishStep = 10;
    int checkpointCoins = 0;
    int lapCoins = 0;
    /** 1st 300, 2nd 240, 3rd 180, 4th-9th 120, 10th-14th 60, 15th-19th 15, then nothing. */
    int[] placement = {300, 240, 180, 120, 120, 120, 120, 120, 120, 60, 60, 60, 60, 60, 15, 15, 15, 15, 15};

    /** Reads the {@code coins:} section of config.yml (missing keys keep the MCC defaults). */
    public void load(ConfigurationSection c) {
        if (c == null) return;
        finishFirst = c.getInt("coins.finish-first", finishFirst);
        finishStep = c.getInt("coins.finish-step", finishStep);
        checkpointCoins = c.getInt("coins.checkpoint", checkpointCoins);
        lapCoins = c.getInt("coins.lap", lapCoins);
        List<Integer> l = c.getIntegerList("coins.placement");
        if (!l.isEmpty()) placement = l.stream().mapToInt(Integer::intValue).toArray();
    }

    public void checkpoint(McmGameContext ctx, UUID player, int cp, int total) {
        if (ctx != null && checkpointCoins > 0) ctx.addObjectiveCoins(player, checkpointCoins, null, "Checkpoint " + cp + "/" + total + " completed");
    }

    public void lap(McmGameContext ctx, UUID player, int lap, int laps) {
        if (ctx != null && lap < laps) ctx.addObjectiveCoins(player, lapCoins, "LAP COMPLETED!", "Lap " + lap + "/" + laps + " completed");
    }

    /** Kept for callers; the finish coins depend on the place and are paid by {@link #place}. */
    public void finish(McmGameContext ctx, UUID player) { }

    /** Finish coins for the {@code place}-th finisher (1-based): 400, 390, 380, ... never below 0. */
    public int finishCoins(int place) {
        return place < 1 ? 0 : Math.max(0, finishFirst - finishStep * (place - 1));
    }

    /** Placement bonus for {@code place} (1-based), 0 past the table. */
    public int placementCoins(int place) {
        return (place >= 1 && place <= placement.length) ? placement[place - 1] : 0;
    }

    public void place(McmGameContext ctx, UUID player, int place) {
        if (ctx == null || place < 1) return;
        int total = finishCoins(place) + placementCoins(place);
        ctx.addObjectiveCoins(player, total, "LAP COMPLETED!", "Finished in " + ordinal(place) + " place");
    }

    private static String ordinal(int n) {
        int m = n % 100;
        if (m >= 11 && m <= 13) return n + "th";
        return n + switch (n % 10) { case 1 -> "st"; case 2 -> "nd"; case 3 -> "rd"; default -> "th"; };
    }

    /** Total coins for a place (finish + placement bonus), for HUD/result text. */
    public int placePoints(int place) {
        return finishCoins(place) + placementCoins(place);
    }
}
