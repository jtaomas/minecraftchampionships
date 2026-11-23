package com.mcm.games.findthebutton.map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Find the Button map: one or two enclosed rooms built by a pure-Java generator into a {@link Grid}, plus its
 * hand-placed hiding spots and start pad. No Bukkit classes here, so the same code runs offline (preview +
 * reachability validation) and on the server.
 *
 * <p>Local coordinates: the floor blocks are at y = 0 (players stand at y = 1), the playable interior is
 * x 1..20, z 1..20, y 1..{@link #height}; the outer walls are at x/z = 0 and 21 and the ceiling at height + 1.
 * North is -z. The start pad is near the south wall and players face north.
 */
public abstract class FtbMap {
    public static final int MIN = 1, MAX = 20;

    /** Interior height (air from y = 1 to y = height). */
    public final int height;
    private Grid grid;
    private final List<Spot> spots = new ArrayList<>();
    protected Grid g;

    // start pad (inclusive floor cells players spawn on, feet at y = 1) and facing
    protected int padX1 = 8, padZ1 = 18, padX2 = 13, padZ2 = 20;
    protected float padYaw = 180f;   // 180 = facing north (-z)

    protected FtbMap(int height) { this.height = height; }

    public abstract String id();
    public abstract String displayName();
    public abstract String tagline();

    /** Write the map into {@link #g}. Call {@link #spot} next to each hiding place. */
    protected abstract void generate();

    /** The finished grid (built once, connection post-pass applied). */
    public final synchronized Grid grid() {
        if (grid == null) {
            g = new Grid(-1, -1, -1, 22, height + 3, 22);
            spots.clear();
            generate();
            g.postProcess();
            grid = g;
        }
        return grid;
    }

    public final List<Spot> spots() { grid(); return Collections.unmodifiableList(spots); }

    /** Highest y kept in the offline cut-away preview (roofs/beams above it are left out). */
    public int previewCutY() { return height; }

    public int padX1() { return padX1; }
    public int padZ1() { return padZ1; }
    public int padX2() { return padX2; }
    public int padZ2() { return padZ2; }
    public float padYaw() { return padYaw; }

    // ------------------------------------------------------------------ helpers for generators

    protected final void spot(String name, String difficulty, int x, int y, int z, Spot.Dir support) {
        spots.add(new Spot(name, difficulty, x, y, z, support));
    }

    protected final void set(int x, int y, int z, String b) { g.set(x, y, z, b); }

    protected final String get(int x, int y, int z) { return g.get(x, y, z); }

    protected final boolean air(int x, int y, int z) { return g.isAir(x, y, z); }

    /** Block source by position. */
    public interface Pal { String at(int x, int y, int z); }

    protected static Pal one(String b) { return (x, y, z) -> b; }

    /** Deterministic weighted pick: {@code mix(salt, "a", 3, "b", 1)}. */
    protected static Pal mix(long salt, Object... pairs) {
        int n = pairs.length / 2;
        String[] ids = new String[n];
        double[] cum = new double[n];
        double tot = 0;
        for (int i = 0; i < n; i++) {
            ids[i] = (String) pairs[2 * i];
            tot += ((Number) pairs[2 * i + 1]).doubleValue();
            cum[i] = tot;
        }
        final double total = tot;
        return (x, y, z) -> {
            double r = hash(x, y, z, salt) * total;
            for (int i = 0; i < n; i++) if (r < cum[i]) return ids[i];
            return ids[n - 1];
        };
    }

    /** Two-block checkerboard. */
    protected static Pal checker(String a, String b) { return (x, y, z) -> ((x + z) & 1) == 0 ? a : b; }

    protected static double hash(int x, int y, int z, long seed) {
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= x * 0x632BE59BD9B4E019L;
        h ^= y * 0x85157AF5L;
        h ^= z * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 31);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 33);
        return (h >>> 11) * 0x1.0p-53;
    }

    protected final void fill(int x1, int y1, int z1, int x2, int y2, int z2, String b) {
        fill(x1, y1, z1, x2, y2, z2, one(b));
    }

    protected final void fill(int x1, int y1, int z1, int x2, int y2, int z2, Pal p) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    g.set(x, y, z, p == null ? null : p.at(x, y, z));
    }

    /** Floor (y = 0), four outer walls and the ceiling (y = height + 1). */
    protected final void shell(Pal floor, Pal wall, Pal ceiling) {
        fill(0, 0, 0, 21, 0, 21, floor);
        fill(0, height + 1, 0, 21, height + 1, 21, ceiling);
        for (int y = 1; y <= height; y++)
            for (int i = 0; i <= 21; i++) {
                g.set(i, y, 0, wall.at(i, y, 0));
                g.set(i, y, 21, wall.at(i, y, 21));
                g.set(0, y, i, wall.at(0, y, i));
                g.set(21, y, i, wall.at(21, y, i));
            }
    }

    protected static String stairs(String mat, String facing, boolean top) { return States.stairs(mat, facing, top); }

    protected static String slab(String mat, boolean top) { return States.slab(mat, top); }

    protected static String log(String wood, char axis) { return wood + "[axis=" + axis + "]"; }

    /**
     * Staircase helper: the first step sits ON the floor at (x, floorY + 1, z), each next step one block further
     * along (dx, dz) and one higher; n steps, so the landing must be the block at (x + dx*n, floorY + n, z + dz*n).
     * Registered for {@link Grid#validateStairs()}.
     */
    protected final void staircase(String name, String mat, int x, int z, int floorY, int dx, int dz, int n) {
        String facing = dz < 0 ? "north" : dz > 0 ? "south" : dx > 0 ? "east" : "west";
        for (int i = 1; i <= n; i++) g.set(x + dx * (i - 1), floorY + i, z + dz * (i - 1), stairs(mat, facing, false));
        g.addLane(new Grid.Lane(name, x, z, floorY, dx, dz, n));
    }

    protected final void lantern(int x, int y, int z, boolean hanging) {
        g.set(x, y, z, "minecraft:lantern[hanging=" + hanging + "]");
    }

    /** Chain from the ceiling down to a hanging lantern at y. */
    protected final void hangingLantern(int x, int y, int z, int topY) {
        for (int yy = y + 1; yy <= topY; yy++) g.set(x, yy, z, "minecraft:chain[axis=y]");
        lantern(x, y, z, true);
    }

    /** Invisible light source (level 15) in an empty cell; no collision and no hit box for players. */
    protected final void light(int x, int y, int z) {
        if (g.isAir(x, y, z)) g.set(x, y, z, "minecraft:light[level=15]");
    }

    /** Invisible lights on a grid just under the ceiling, plus a lower layer, so nothing is dark. */
    protected final void lightGrid(int step, int y) {
        for (int x = 2; x <= 19; x += step)
            for (int z = 2; z <= 19; z += step) light(x, y, z);
    }

    /** Table: fence legs at the four corners (y = 1) and a bottom-slab top at y = 2 (gap below for hiding). */
    protected final void table(int x1, int z1, int x2, int z2, String fence, String slabMat) {
        int ax = Math.min(x1, x2), bx = Math.max(x1, x2), az = Math.min(z1, z2), bz = Math.max(z1, z2);
        g.set(ax, 1, az, fence); g.set(bx, 1, az, fence); g.set(ax, 1, bz, fence); g.set(bx, 1, bz, fence);
        fill(ax, 2, az, bx, 2, bz, slab(slabMat, false));
    }

    protected final void wallTorch(int x, int y, int z, String facing) {
        g.set(x, y, z, "minecraft:wall_torch[facing=" + facing + "]");
    }
}
