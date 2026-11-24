package com.mcm.games.battlebox.arena;

/**
 * Shared Battle Box arena geometry in local coordinates (pure Java). The origin is the centre of the floor;
 * the floor surface is y = 0, so players stand at y = 1. Side A spawns at negative z, side B at positive z.
 *
 * <pre>
 *  floor        |x|,|z| &lt;= R                       (R = 17 gives the 35x35 inner floor)
 *  walls        |x| or |z| == R+1, y -1..H          (H = 13), ceiling layer y = H+1; skin at R+2
 *  centre       3x3 cells at y = 2 set INTO a 9x9 two-step dais, flush with the hazard-stripe ring (y 2);
 *               sea lanterns sit under the cells (y 1) and in the four ring corners (the lantern frame)
 *  gates        z = -(R+1) / +(R+1), |x| &lt;= 2, y 1..3
 *  spawn rooms  |x| &lt;= 5, |z| in R+2..R+7, floor y 0, ceiling y 5
 * </pre>
 * The layouts are hand-designed for R = 17, so the size is fixed.
 */
public final class ArenaGeo {
    public static final int DEFAULT_R = 17;
    public final int R = DEFAULT_R;
    public final int H = 13;
    public final int W;          // wall coordinate (R + 1)
    public final int CEIL;       // ceiling layer y
    /** y of the 9 objective cells: inside the dais top, flush with the hazard ring (was 3 = on top of it). */
    public final int CENTRE_Y = 2;
    /** y of the hazard-stripe ring around the cells (players stand on it at RING_Y + 1). */
    public final int RING_Y = 2;
    public final int DAIS = 4;   // dais half-size (steps at 4 and 3, ring at 2, cells at <= 1)
    public final int GATE_HALF = 2, GATE_TOP = 3;
    public final int ROOM_HALF = 5, ROOM_IN, ROOM_OUT, ROOM_CEIL = 5;
    public final int minX, maxX, minY, maxY, minZ, maxZ;

    public ArenaGeo() {
        this.W = R + 1;
        this.CEIL = H + 1;
        this.ROOM_IN = R + 2;
        this.ROOM_OUT = R + 8;
        this.minX = -(R + 3);
        this.maxX = R + 3;
        this.minZ = -(R + 9);
        this.maxZ = R + 9;
        this.minY = -3;
        this.maxY = H + 6;
    }

    public Grid newGrid() { return new Grid(minX, minY, minZ, maxX, maxY, maxZ); }

    /** True for the 9 centre cells (y = CENTRE_Y). */
    public boolean isCentreCell(int x, int y, int z) {
        return y == CENTRE_Y && Math.abs(x) <= 1 && Math.abs(z) <= 1;
    }

    /** Spawn point i (0..n) for side B? (+z) or A (-z): {x, y, z, yaw}. */
    public double[] spawn(boolean sideB, int i) {
        int[] xs = {0, -2, 2, -4, 4, -1, 1, -3, 3};
        int[] dz = {0, 0, 0, 0, 0, 2, 2, 2, 2};
        int k = Math.floorMod(i, xs.length);
        double z = R + 4 + dz[k];
        return sideB ? new double[]{xs[k] + 0.5, 1, z + 0.5, 180} : new double[]{-xs[k] + 0.5, 1, -z + 0.5, 0};
    }

    /**
     * Spectator overview for a side: high above its own half, looking down at the centre.
     * {x, y, z, yaw, pitch}.
     */
    public double[] overview(boolean sideB, int i) {
        double dx = (Math.floorMod(i, 3) - 1) * 3;
        double z = -(R - 5);
        double y = H - 1;
        // pitch so the eye line hits the centre pad: atan(drop / distance)
        double pitch = Math.toDegrees(Math.atan2(y + 1.62 - CENTRE_Y, Math.abs(z)));
        return sideB ? new double[]{-dx + 0.5, y, -z + 0.5, 180, pitch} : new double[]{dx + 0.5, y, z + 0.5, 0, pitch};
    }

    /** Spectators may roam inside this local box (inclusive); outside it they are sent back to the overview. */
    public boolean inSpectatorBox(double x, double y, double z) {
        return Math.abs(x - 0.5) <= W + 0.5 && Math.abs(z - 0.5) <= W + 0.5 && y >= -1 && y <= CEIL + 4;
    }
}
