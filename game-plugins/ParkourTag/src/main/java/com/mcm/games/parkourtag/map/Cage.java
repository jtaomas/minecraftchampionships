package com.mcm.games.parkourtag.map;

import java.util.ArrayList;
import java.util.List;

/**
 * A spawn cage (MCC: a small team room with a team-coloured glass front, a door that opens after
 * "Starting in 5..1", and a "Select Hunter:" wall sign).
 *
 * <p>(x, y, z) is the centre cell of the door in the front wall, at feet height. (fx, fz) points out of the cage,
 * into the arena. The room behind the front wall is 5 wide, 3 deep and 3 high. The door is 3 wide and 2 high.
 * Walls, floor and roof are one block thick.
 */
public record Cage(int x, int y, int z, int fx, int fz, String wall, String floor) {
    public static final int HALF = 2;    // interior lateral half-width
    public static final int DEPTH = 3;   // interior depth
    public static final int HEIGHT = 3;  // interior height

    /** Lateral (right-hand) direction. */
    public int rx() { return -fz; }
    public int rz() { return fx; }

    /** World-local cell at lateral l, depth d (d &gt; 0 is inside, 0 is the front wall), height dy above feet. */
    public int[] at(int l, int d, int dy) {
        return new int[]{x + rx() * l - fx * d, y + dy, z + rz() * l - fz * d};
    }

    public String facingName() {
        if (fz > 0) return "south";
        if (fz < 0) return "north";
        return fx > 0 ? "east" : "west";
    }

    /** Yaw that looks out of the cage. */
    public float yaw() {
        return (float) Math.toDegrees(Math.atan2(-fx, fz));
    }

    /** Builds the frame with neutral glass and a closed (black) door. */
    public void build(BlockSink s) {
        int w = HALF + 1;
        for (int l = -w; l <= w; l++) {
            for (int d = 0; d <= DEPTH + 1; d++) {
                for (int dy = -1; dy <= HEIGHT; dy++) {
                    int[] c = at(l, d, dy);
                    boolean shell = dy == -1 || dy == HEIGHT || Math.abs(l) == w || d == DEPTH + 1 || d == 0;
                    if (!shell) { s.set(c[0], c[1], c[2], "air"); continue; }
                    if (dy == -1) s.set(c[0], c[1], c[2], floor);
                    else if (d == 0 && dy < HEIGHT && Math.abs(l) < w) s.set(c[0], c[1], c[2], isDoor(l, dy) ? "black_stained_glass" : "white_stained_glass");
                    else s.set(c[0], c[1], c[2], wall);
                }
            }
        }
        int[] sg = signPos();
        s.set(sg[0], sg[1], sg[2], "oak_wall_sign[facing=" + facingName() + "]");
        int[] lamp = at(0, 2, HEIGHT);
        s.set(lamp[0], lamp[1], lamp[2], "ochre_froglight");
    }

    private static boolean isDoor(int l, int dy) { return Math.abs(l) <= 1 && dy >= 0 && dy <= 1; }

    public List<int[]> doorCells() {
        List<int[]> out = new ArrayList<>();
        for (int l = -1; l <= 1; l++) for (int dy = 0; dy <= 1; dy++) out.add(at(l, 0, dy));
        return out;
    }

    public List<int[]> glassCells() {
        List<int[]> out = new ArrayList<>();
        for (int l = -HALF; l <= HALF; l++) for (int dy = 0; dy < HEIGHT; dy++) if (!isDoor(l, dy)) out.add(at(l, 0, dy));
        return out;
    }

    /** Cells of the interior floor surface (team carpet goes here). */
    public List<int[]> carpetCells() {
        List<int[]> out = new ArrayList<>();
        for (int l = -HALF; l <= HALF; l++) for (int d = 1; d <= DEPTH; d++) out.add(at(l, d, 0));
        return out;
    }

    /** The sign: on the back wall, centre, at eye level, facing the door. */
    public int[] signPos() { return at(0, DEPTH, 1); }

    /** Spawn point {x, y, z, yaw} for slot i (0..4), on the middle row of the room. */
    public double[] slot(int i) {
        int l = new int[]{0, -1, 1, -2, 2}[Math.floorMod(i, 5)];
        int[] c = at(l, 2, 0);
        return new double[]{c[0] + 0.5, c[1], c[2] + 0.5, yaw()};
    }
}
