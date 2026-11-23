package com.mcm.games.footrace;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.type.Leaves;

import java.util.ArrayList;
import java.util.List;

/**
 * Geometry and construction of the Foot Race track.
 *
 * The track is a 9-wide rectangular loop (outer box x 0..112, z 0..64, inner void x 9..103, z 9..55),
 * run anticlockwise when viewed from above: south straight (+X) -> east straight (+Z) -> north straight (-X)
 * -> west straight (-Z) -> back to the start/finish line on the south straight.
 *
 *  A "Stadium"  (south, +X): starting grid + gate, finish line, hurdles, soul sand (ice fast lane), gaps.
 *  B "Jungle"   (east,  +Z): slime launch pads over a void, three narrow bridges.
 *  C "Nether"   (north, -X): ladder wall climb, magma stripes (hazard), slalom walls.
 *  D "Frost"    (west,  -Z): packed-ice straight with hurdles.
 *  Shortcuts: gold stepping stones across the SE corner, packed-ice stepping stones across the NW corner.
 */
final class Track {
    private Track() {}

    static final int FY = 100;              // floor block layer; players stand at FY+1
    static final int LAPS = 3;
    static final double KILL_Y = FY - 6;    // below this -> back to checkpoint

    // Bounding box used for the clean sweep before building.
    static final int SWEEP_MIN_X = -4, SWEEP_MAX_X = 117, SWEEP_MIN_Z = -4, SWEEP_MAX_Z = 69;
    static final int SWEEP_MIN_Y = FY - 3, SWEEP_MAX_Y = FY + 16;

    static final int GRID_LANES = 7, GRID_ROWS = 8;

    // ---------------------------------------------------------------- geometry

    static boolean isTrack(int x, int z) {
        if (x < 0 || x > 112 || z < 0 || z > 64) return false;
        return !(x >= 9 && x <= 103 && z >= 9 && z <= 55);
    }

    enum Region { A, B, C, D, CORNER }

    static Region region(int x, int z) {
        boolean cx = x <= 8 || x >= 104;
        boolean cz = z <= 8 || z >= 56;
        if (cx && cz) return Region.CORNER;
        if (z <= 8) return Region.A;
        if (x >= 104) return Region.B;
        if (z >= 56) return Region.C;
        return Region.D;
    }

    /** A plane across the track that must be crossed in the running direction. */
    static final class Checkpoint {
        final int index;
        final String name;
        final boolean axisX;   // plane is x = coord (else z = coord)
        final double coord;
        final int sign;        // +1: crossing towards increasing coord, -1: decreasing
        final double min, max; // extent on the other horizontal axis
        final double cx, cz;   // centre on the track (for progress)
        final double rx, rz;   // respawn point
        final float yaw;

        Checkpoint(int index, String name, boolean axisX, double coord, int sign, double min, double max,
                   double cx, double cz, double rx, double rz, float yaw) {
            this.index = index; this.name = name; this.axisX = axisX; this.coord = coord; this.sign = sign;
            this.min = min; this.max = max; this.cx = cx; this.cz = cz; this.rx = rx; this.rz = rz; this.yaw = yaw;
        }

        boolean crossed(Location from, Location to) {
            double a = axisX ? from.getX() : from.getZ();
            double b = axisX ? to.getX() : to.getZ();
            double o = axisX ? to.getZ() : to.getX();
            if (o < min || o > max) return false;
            if (to.getY() < FY - 3 || to.getY() > FY + 14) return false;
            return sign > 0 ? (a < coord && b >= coord) : (a > coord && b <= coord);
        }

        Location respawn(World w) {
            return new Location(w, rx, FY + 1, rz, yaw, 0f);
        }
    }

    // yaw: 0 = +Z, 90 = -X, 180 = -Z, -90 = +X
    static final Checkpoint[] CPS = {
            new Checkpoint(0, "Start/Finish", true, 30, +1, -1, 10, 30, 4.5, 33.5, 4.5, -90f),
            new Checkpoint(1, "Stadium", true, 70, +1, -1, 10, 70, 4.5, 72.5, 4.5, -90f),
            new Checkpoint(2, "Jungle Gate", false, 24, +1, 103, 114, 108.5, 24, 108.5, 25.5, 0f),
            new Checkpoint(3, "Jungle Exit", false, 53, +1, 103, 114, 108.5, 53, 108.5, 54.5, 0f),
            new Checkpoint(4, "Nether Pit", true, 68, -1, 55, 66, 68, 60.5, 66.5, 60.5, 90f),
            new Checkpoint(5, "Frost Gate", false, 40, -1, -1, 10, 4.5, 40, 4.5, 38.5, 180f),
    };

    /** Order of checkpoints within a lap: crossing 0 (finish line) after 5 completes the lap. */
    static final int[] ORDER = {1, 2, 3, 4, 5, 0};

    // Centre line rectangle for progress measurement.
    private static final double L = 4.5, R = 108.5, S = 4.5, N = 60.5;
    static final double PERIMETER = 2 * (R - L) + 2 * (N - S); // 320
    private static final double S0 = pathS(CPS[0].cx, CPS[0].cz);
    private static final double[] CP_REL = new double[CPS.length];

    static {
        for (int i = 0; i < CPS.length; i++) CP_REL[i] = rel(CPS[i].cx, CPS[i].cz);
        CP_REL[0] = 0;
    }

    /** Distance along the centre line from the SW corner (anticlockwise). */
    static double pathS(double x, double z) {
        double best = Double.MAX_VALUE, s = 0;
        // seg0: (L,S) -> (R,S)
        double t = clamp(x, L, R); double d = sq(x - t) + sq(z - S);
        if (d < best) { best = d; s = t - L; }
        // seg1: (R,S) -> (R,N)
        t = clamp(z, S, N); d = sq(x - R) + sq(z - t);
        if (d < best) { best = d; s = (R - L) + (t - S); }
        // seg2: (R,N) -> (L,N)
        t = clamp(x, L, R); d = sq(x - t) + sq(z - N);
        if (d < best) { best = d; s = (R - L) + (N - S) + (R - t); }
        // seg3: (L,N) -> (L,S)
        t = clamp(z, S, N); d = sq(x - L) + sq(z - t);
        if (d < best) { s = 2 * (R - L) + (N - S) + (N - t); }
        return s;
    }

    static double rel(double x, double z) {
        return ((pathS(x, z) - S0) % PERIMETER + PERIMETER) % PERIMETER;
    }

    /** Progress inside the current lap, clamped between the last passed and next checkpoint. */
    static double lapProgress(double x, double z, int seq) {
        double lo = seq == 0 ? 0 : CP_REL[ORDER[seq - 1]];
        int next = ORDER[seq];
        double hi = next == 0 ? PERIMETER : CP_REL[next];
        double r = rel(x, z);
        if (r > hi + PERIMETER / 2) r -= PERIMETER;
        else if (r < lo - PERIMETER / 2) r += PERIMETER;
        return clamp(r, lo, hi);
    }

    static double lapStartProgress(int seq) {
        return seq == 0 ? 0 : CP_REL[ORDER[seq - 1]];
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
    private static double sq(double v) { return v * v; }

    static Location gridSlot(World w, int slot) {
        int i = Math.floorMod(slot, GRID_LANES * GRID_ROWS);
        int row = i / GRID_LANES, lane = i % GRID_LANES;
        return new Location(w, 25 - 2 * row + 0.5, FY + 1, 1 + lane + 0.5, -90f, 0f);
    }

    static Location spectatorSpot(World w) {
        return new Location(w, 56.5, FY + 13, 32.5, -90f, 30f);
    }

    // ---------------------------------------------------------------- launch pads

    /** Slime pads on the east straight launch players forward (+Z). */
    static boolean isLaunchZone(int x, int z) {
        return x >= 104 && x <= 112 && z >= 27 && z <= 28;
    }

    // ---------------------------------------------------------------- building

    /** Records every block it sets so the arena can be removed again. */
    static final class Builder {
        final World w;
        final List<int[]> placed;

        Builder(World w, List<int[]> placed) { this.w = w; this.placed = placed; }

        void set(int x, int y, int z, Material m) {
            w.getBlockAt(x, y, z).setType(m, false);
            if (m != Material.AIR) placed.add(new int[]{x, y, z});
        }

        void set(int x, int y, int z, BlockData d) {
            w.getBlockAt(x, y, z).setBlockData(d, false);
            placed.add(new int[]{x, y, z});
        }

        Material get(int x, int y, int z) { return w.getBlockAt(x, y, z).getType(); }
    }

    private static int hash(int x, int z) {
        int h = x * 73856093 ^ z * 19349663;
        h ^= (h >>> 13);
        return h & 0xFF;
    }

    private static Material floorMat(int x, int z) {
        int h = hash(x, z);
        switch (region(x, z)) {
            case A:
                return (z == 0 || z == 8) ? Material.WHITE_CONCRETE : Material.RED_TERRACOTTA;
            case B:
                return h < 120 ? Material.MOSSY_STONE_BRICKS : h < 220 ? Material.STONE_BRICKS : Material.CRACKED_STONE_BRICKS;
            case C:
                return h < 150 ? Material.NETHER_BRICKS : Material.RED_NETHER_BRICKS;
            case D:
                return (x == 0 || x == 8) ? Material.LIGHT_BLUE_CONCRETE : (h < 200 ? Material.SNOW_BLOCK : Material.WHITE_CONCRETE);
            default:
                return Material.POLISHED_ANDESITE;
        }
    }

    private static Material underMat(Region r) {
        switch (r) {
            case A: return Material.TERRACOTTA;
            case B: return Material.MOSSY_COBBLESTONE;
            case C: return Material.NETHERRACK;
            case D: return Material.PACKED_ICE;
            default: return Material.ANDESITE;
        }
    }

    private static Material rimMat(Region r) {
        switch (r) {
            case A: return Material.WHITE_CONCRETE;
            case B: return Material.MOSSY_COBBLESTONE;
            case C: return Material.POLISHED_BLACKSTONE_BRICKS;
            case D: return Material.PACKED_ICE;
            default: return Material.STONE_BRICKS;
        }
    }

    /** Rim openings where shortcuts leave/join the track. */
    private static boolean skipRim(int x, int z) {
        return (z == 9 && x >= 91 && x <= 96)
                || (x == 103 && z >= 15 && z <= 21)
                || (z == 55 && x >= 15 && x <= 21)
                || (x == 9 && z >= 41 && z <= 47);
    }

    static final int[] MAGMA_STRIPES = {85, 86, 79, 80, 81, 74, 75};

    static void build(Builder b) {
        // 1. floor
        for (int x = 0; x <= 112; x++)
            for (int z = 0; z <= 64; z++)
                if (isTrack(x, z)) b.set(x, FY, z, floorMat(x, z));

        // 2. floor features
        // finish line (checkered)
        for (int x = 30; x <= 31; x++)
            for (int z = 0; z <= 8; z++)
                b.set(x, FY, z, ((x + z) & 1) == 0 ? Material.BLACK_CONCRETE : Material.WHITE_CONCRETE);
        // soul sand bog with a blue-ice fast lane on the inside edge
        for (int x = 56; x <= 66; x++)
            for (int z = 0; z <= 8; z++)
                b.set(x, FY, z, z == 8 ? Material.BLUE_ICE : Material.SOUL_SAND);
        // gaps
        for (int x : new int[]{76, 77, 81, 82, 86, 87})
            for (int z = 0; z <= 8; z++) b.set(x, FY, z, Material.AIR);
        // slime launch pads + void
        for (int x = 104; x <= 112; x++) {
            for (int z = 27; z <= 28; z++) b.set(x, FY, z, Material.SLIME_BLOCK);
            for (int z = 29; z <= 34; z++) b.set(x, FY, z, Material.AIR);
        }
        // narrow bridges
        for (int x = 104; x <= 112; x++)
            for (int z = 44; z <= 51; z++) b.set(x, FY, z, Material.AIR);
        for (int z = 44; z <= 51; z++) {
            b.set(105, FY, z, Material.OAK_PLANKS);
            if (z != 47 && z != 48) b.set(108, FY, z, Material.MOSSY_COBBLESTONE);
            b.set(111, FY, z, Material.JUNGLE_PLANKS);
        }
        // magma stripes (hazard)
        for (int x : MAGMA_STRIPES)
            for (int z = 56; z <= 64; z++) b.set(x, FY, z, Material.MAGMA_BLOCK);
        // frost straight: packed ice
        for (int z = 16; z <= 36; z++)
            for (int x = 1; x <= 7; x++) b.set(x, FY, z, Material.PACKED_ICE);

        // shortcut stepping stones
        int[][] se = {{94, 11}, {97, 13}, {99, 16}, {102, 18}};
        for (int[] s : se) b.set(s[0], FY, s[1], Material.GOLD_BLOCK);
        int[][] nw = {{18, 53}, {16, 50}, {13, 48}, {11, 45}};
        for (int[] s : nw) b.set(s[0], FY, s[1], Material.PACKED_ICE);

        // 3. underside (only where there is floor, so gaps stay open)
        for (int x = 0; x <= 112; x++)
            for (int z = 0; z <= 64; z++)
                if (isTrack(x, z) && b.get(x, FY, z) != Material.AIR) b.set(x, FY - 1, z, underMat(region(x, z)));
        for (int[] s : se) b.set(s[0], FY - 1, s[1], Material.YELLOW_TERRACOTTA);
        for (int[] s : nw) b.set(s[0], FY - 1, s[1], Material.LIGHT_BLUE_TERRACOTTA);

        // 4. rims (1-high kerb) wherever a neighbouring track cell has floor
        for (int x = -1; x <= 113; x++) {
            for (int z = -1; z <= 65; z++) {
                if (isTrack(x, z) || skipRim(x, z)) continue;
                Region nr = null;
                for (int dx = -1; dx <= 1 && nr == null; dx++)
                    for (int dz = -1; dz <= 1 && nr == null; dz++) {
                        int nx = x + dx, nz = z + dz;
                        if (isTrack(nx, nz) && b.get(nx, FY, nz) != Material.AIR) nr = region(nx, nz);
                    }
                if (nr != null) b.set(x, FY + 1, z, rimMat(nr));
            }
        }
        // magma on the kerbs too, so the stripes cannot be skipped along the edge
        for (int x : MAGMA_STRIPES)
            for (int z : new int[]{55, 65})
                if (b.get(x, FY + 1, z) != Material.AIR) b.set(x, FY + 1, z, Material.MAGMA_BLOCK);

        // 5. decoration on the outer kerbs
        for (int x = 34; x <= 103; x += 4)
            if (b.get(x, FY + 1, -1) != Material.AIR)
                b.set(x, FY + 2, -1, ((x / 4) & 1) == 0 ? Material.RED_WOOL : Material.WHITE_WOOL);
        Leaves leaves = (Leaves) Material.JUNGLE_LEAVES.createBlockData();
        leaves.setPersistent(true);
        for (int z = 10; z <= 54; z += 5)
            if (b.get(113, FY + 1, z) != Material.AIR) b.set(113, FY + 2, z, leaves);
        for (int x = 10; x <= 100; x += 6)
            if (b.get(x, FY + 1, 65) == Material.POLISHED_BLACKSTONE_BRICKS) b.set(x, FY + 2, 65, Material.SHROOMLIGHT);
        for (int z = 10; z <= 54; z += 5)
            if (b.get(-1, FY + 1, z) != Material.AIR) b.set(-1, FY + 2, z, Material.SEA_LANTERN);

        // 6. obstacles
        // stadium hurdles
        for (int x : new int[]{36, 40, 44, 48, 52})
            for (int z = 0; z <= 8; z++)
                b.set(x, FY + 1, z, (z & 1) == 0 ? Material.RED_CONCRETE : Material.WHITE_CONCRETE);
        // frost hurdles
        for (int z : new int[]{33, 28, 23, 18})
            for (int x = 0; x <= 8; x++)
                b.set(x, FY + 1, z, (x & 1) == 0 ? Material.LIGHT_BLUE_CONCRETE : Material.WHITE_CONCRETE);
        // nether ladder wall (covers the kerbs too)
        for (int x = 92; x <= 94; x++)
            for (int z = 55; z <= 65; z++)
                for (int y = FY + 1; y <= FY + 7; y++)
                    b.set(x, y, z, y == FY + 7 ? Material.GILDED_BLACKSTONE : Material.POLISHED_BLACKSTONE_BRICKS);
        Directional ladder = (Directional) Material.LADDER.createBlockData();
        ladder.setFacing(BlockFace.EAST);
        for (int z = 56; z <= 64; z++)
            for (int y = FY + 1; y <= FY + 7; y++) b.set(95, y, z, ladder);
        // nether slalom walls
        int[] slalom = {62, 56, 50, 44};
        for (int k = 0; k < slalom.length; k++) {
            int zFrom = (k % 2 == 0) ? 55 : 58;
            int zTo = (k % 2 == 0) ? 62 : 65;
            for (int z = zFrom; z <= zTo; z++)
                for (int y = FY + 1; y <= FY + 3; y++)
                    b.set(slalom[k], y, z, y == FY + 3 ? Material.CRIMSON_HYPHAE : Material.NETHER_WART_BLOCK);
        }

        // 7. finish arch
        for (int x = 30; x <= 31; x++) {
            for (int z : new int[]{-2, 10})
                for (int y = FY - 1; y <= FY + 6; y++) b.set(x, y, z, Material.QUARTZ_PILLAR);
            for (int z = -2; z <= 10; z++)
                b.set(x, FY + 7, z, ((x + z) & 1) == 0 ? Material.BLACK_CONCRETE : Material.WHITE_CONCRETE);
        }

        // 8. spectator platform in the middle
        for (int x = 51; x <= 61; x++)
            for (int z = 27; z <= 37; z++) {
                boolean edge = x == 51 || x == 61 || z == 27 || z == 37;
                b.set(x, FY + 12, z, edge ? Material.WHITE_CONCRETE : Material.LIGHT_GRAY_STAINED_GLASS);
            }
    }

    // ---------------------------------------------------------------- start box

    static List<int[]> startBoxPositions() {
        List<int[]> list = new ArrayList<>();
        for (int z = 0; z <= 8; z++)
            for (int y = FY + 1; y <= FY + 4; y++) {
                list.add(new int[]{9, y, z});
                list.add(new int[]{28, y, z});
            }
        for (int y = FY + 2; y <= FY + 4; y++) {
            for (int x = 9; x <= 28; x++) {
                list.add(new int[]{x, y, -1});
                list.add(new int[]{x, y, 9});
            }
        }
        return list;
    }

    static void buildStartBox(Builder b) {
        for (int[] p : startBoxPositions())
            b.set(p[0], p[1], p[2], p[0] == 28 && p[2] >= 0 && p[2] <= 8
                    ? Material.RED_STAINED_GLASS : Material.GLASS);
    }

    static void openStartBox(World w) {
        for (int[] p : startBoxPositions()) w.getBlockAt(p[0], p[1], p[2]).setType(Material.AIR, false);
    }
}
