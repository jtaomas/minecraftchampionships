package com.mcm.games.acerace;

import com.mcm.api.McmGameContext;

import java.util.UUID;

/** Progress + placement scoring, scaled to the project's 50-150 top end. */
public final class RaceScoring {
    private static final int[] PLACE_POINTS = {
            100, 90, 81, 73, 66, 60, 55, 51, 47, 44,
            41, 38, 35, 32, 29,
            27, 25, 23, 21, 19,
            17, 15, 14, 13, 12,
            11, 10, 9, 8, 7,
            6, 6, 5, 5, 4,
            4, 3, 3, 2, 2
    };

    public void checkpoint(McmGameContext ctx, UUID player, int cp, int total) {
        if (ctx != null) ctx.addPoints(player, 1, "Checkpoint " + cp + "/" + total);
    }

    public void lap(McmGameContext ctx, UUID player, int lap, int laps) {
        if (ctx != null) ctx.addPoints(player, 3, "Lap " + lap + "/" + laps);
    }

    public void finish(McmGameContext ctx, UUID player) {
        if (ctx != null) ctx.addPoints(player, 8, "Finished");
    }

    public void place(McmGameContext ctx, UUID player, int place) {
        if (ctx == null || place < 1 || place > PLACE_POINTS.length) return;
        ctx.addPoints(player, PLACE_POINTS[place - 1], "Place " + place);
    }

    public int placePoints(int place) {
        return (place >= 1 && place <= PLACE_POINTS.length) ? PLACE_POINTS[place - 1] : 0;
    }
}
