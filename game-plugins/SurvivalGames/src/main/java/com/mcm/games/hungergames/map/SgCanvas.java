package com.mcm.games.hungergames.map;

import com.mcm.games.hungergames.LootTable;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * Bounded pure-Java voxel canvas for fast, deduplicated SG arena generation.
 */
public final class SgCanvas {
    public static final int MAX_R = 130;
    public static final int SIZE = MAX_R * 2 + 1;
    public static final int MIN_Y = 54;
    public static final int MAX_Y = 115;
    public static final int HEIGHT = MAX_Y - MIN_Y + 1;

    public record BlockEdit(int x, int y, int z, Material mat) {}
    public record ChestEntry(int x, int y, int z, LootTable.Tier tier) {}
    public record PedestalEntry(int x, int y, int z, float yaw) {}
    public record AirdropSpot(int x, int z) {}

    private static final Material[] MATERIALS = Material.values();

    private final short[] grid = new short[SIZE * HEIGHT * SIZE];
    private final List<ChestEntry> chests = new ArrayList<>();
    private final List<PedestalEntry> pedestals = new ArrayList<>();
    private final List<AirdropSpot> airdrops = new ArrayList<>();

    public SgCanvas() {
        Arrays.fill(grid, (short) 0);
    }

    private int index(int x, int y, int z) {
        int dx = x + MAX_R;
        int dy = y - MIN_Y;
        int dz = z + MAX_R;
        if (dx < 0 || dx >= SIZE || dy < 0 || dy >= HEIGHT || dz < 0 || dz >= SIZE) return -1;
        return (dy * SIZE + dz) * SIZE + dx;
    }

    public boolean inBounds(int x, int y, int z) {
        return index(x, y, z) >= 0;
    }

    public void set(int x, int y, int z, Material mat) {
        int idx = index(x, y, z);
        if (idx < 0) return;
        grid[idx] = (mat == null || mat == Material.AIR) ? (short) 0 : (short) (mat.ordinal() + 1);
    }

    public Material get(int x, int y, int z) {
        int idx = index(x, y, z);
        if (idx < 0) return Material.AIR;
        int v = grid[idx];
        return v == 0 ? Material.AIR : MATERIALS[v - 1];
    }

    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, Material mat) {
        int lx = Math.min(x1, x2), hx = Math.max(x1, x2);
        int ly = Math.min(y1, y2), hy = Math.max(y1, y2);
        int lz = Math.min(z1, z2), hz = Math.max(z1, z2);
        for (int y = ly; y <= hy; y++) {
            for (int z = lz; z <= hz; z++) {
                for (int x = lx; x <= hx; x++) {
                    set(x, y, z, mat);
                }
            }
        }
    }

    public void cylinder(int cx, int cz, int y1, int y2, double radius, Material mat) {
        int r = (int) Math.ceil(radius);
        double rSq = radius * radius;
        for (int x = cx - r; x <= cx + r; x++) {
            for (int z = cz - r; z <= cz + r; z++) {
                double dx = x - cx;
                double dz = z - cz;
                if (dx * dx + dz * dz <= rSq) {
                    for (int y = y1; y <= y2; y++) {
                        set(x, y, z, mat);
                    }
                }
            }
        }
    }

    public void hollowBox(int x1, int y1, int z1, int x2, int y2, int z2, Material wall, Material floor, Material roof) {
        int lx = Math.min(x1, x2), hx = Math.max(x1, x2);
        int ly = Math.min(y1, y2), hy = Math.max(y1, y2);
        int lz = Math.min(z1, z2), hz = Math.max(z1, z2);
        for (int y = ly; y <= hy; y++) {
            for (int z = lz; z <= hz; z++) {
                for (int x = lx; x <= hx; x++) {
                    boolean edge = x == lx || x == hx || z == lz || z == hz;
                    if (y == ly && floor != null) {
                        set(x, y, z, floor);
                    } else if (y == hy && roof != null) {
                        set(x, y, z, roof);
                    } else if (edge) {
                        set(x, y, z, wall);
                    } else {
                        set(x, y, z, Material.AIR);
                    }
                }
            }
        }
    }

    public void doorway(int x, int yBase, int z, int dx, int dz, int width, int height) {
        for (int i = 0; i < width; i++) {
            for (int dy = 0; dy < height; dy++) {
                set(x + dx * i, yBase + dy, z + dz * i, Material.AIR);
            }
        }
    }

    public void staircase(int startX, int startY, int startZ, int dx, int dz, int steps, Material stepMat) {
        for (int i = 0; i < steps; i++) {
            int sx = startX + dx * i;
            int sy = startY + i;
            int sz = startZ + dz * i;
            set(sx, sy, sz, stepMat);
            for (int clear = 1; clear <= 3; clear++) {
                set(sx, sy + clear, sz, Material.AIR);
            }
        }
    }

    public void chest(int x, int y, int z, LootTable.Tier tier) {
        set(x, y, z, Material.CHEST);
        chests.add(new ChestEntry(x, y, z, tier));
    }

    public void pedestal(int x, int y, int z, Material baseMat) {
        set(x, y, z, baseMat);
        set(x, y + 1, z, Material.AIR);
        set(x, y + 2, z, Material.AIR);
        float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
        pedestals.add(new PedestalEntry(x, y, z, yaw));
    }

    public void enchantArea(int cx, int yFloor, int cz) {
        set(cx, yFloor + 1, cz, Material.ENCHANTING_TABLE);
        int[][] shelves = {
                {-2, -2}, {-1, -2}, {0, -2}, {1, -2}, {2, -2},
                {-2, 2}, {-1, 2}, {0, 2}, {1, 2}, {2, 2},
                {-2, -1}, {-2, 0}, {-2, 1},
                {2, -1}, {2, 0}, {2, 1}
        };
        for (int[] s : shelves) {
            set(cx + s[0], yFloor + 1, cz + s[1], Material.BOOKSHELF);
            set(cx + s[0], yFloor + 2, cz + s[1], Material.BOOKSHELF);
        }
        set(cx - 2, yFloor + 1, cz - 2, Material.CRAFTING_TABLE);
        set(cx + 2, yFloor + 1, cz + 2, Material.ANVIL);
    }

    public void tree(int cx, int groundY, int cz, Material log, Material leaves, int height, int crownR) {
        for (int y = groundY + 1; y <= groundY + height; y++) {
            set(cx, y, cz, log);
        }
        int crownBase = groundY + height - 1;
        for (int dy = 0; dy <= 3; dy++) {
            int r = (dy == 3) ? Math.max(1, crownR - 1) : (dy == 0 ? crownR - 1 : crownR);
            for (int x = cx - r; x <= cx + r; x++) {
                for (int z = cz - r; z <= cz + r; z++) {
                    if (Math.abs(x - cx) == r && Math.abs(z - cz) == r && dy != 1) continue;
                    if (get(x, crownBase + dy, z) == Material.AIR) {
                        set(x, crownBase + dy, z, leaves);
                    }
                }
            }
        }
    }

    public void addAirdropSpot(int x, int z) {
        airdrops.add(new AirdropSpot(x, z));
    }

    public int topSolidY(int x, int z) {
        for (int y = MAX_Y; y >= MIN_Y; y--) {
            Material m = get(x, y, z);
            if (m != Material.AIR && m != Material.WATER) return y;
        }
        return MIN_Y;
    }

    public static double noise2D(double x, double z, long seed) {
        double v1 = valueNoise(x * 0.035, z * 0.035, seed);
        double v2 = valueNoise(x * 0.085, z * 0.085, seed + 1337L) * 0.5;
        double v3 = valueNoise(x * 0.18, z * 0.18, seed + 7919L) * 0.25;
        return (v1 + v2 + v3) / 1.75;
    }

    private static double valueNoise(double x, double z, long seed) {
        int ix = (int) Math.floor(x);
        int iz = (int) Math.floor(z);
        double fx = x - ix;
        double fz = z - iz;
        double ux = fx * fx * (3.0 - 2.0 * fx);
        double uz = fz * fz * (3.0 - 2.0 * fz);
        double a = hash(ix, iz, seed);
        double b = hash(ix + 1, iz, seed);
        double c = hash(ix, iz + 1, seed);
        double d = hash(ix + 1, iz + 1, seed);
        return (a + (b - a) * ux) + ((c + (d - c) * ux) - (a + (b - a) * ux)) * uz;
    }

    private static double hash(int x, int z, long seed) {
        long h = seed ^ (x * 341873128712L) ^ (z * 132897987541L);
        h = (h ^ (h >>> 30)) * 0xbf58476d1ce4e5b9L;
        h = (h ^ (h >>> 27)) * 0x94d049bb133111ebL;
        h = h ^ (h >>> 31);
        return ((h & 0xffffL) / 32767.5) - 1.0;
    }

    public List<ChestEntry> getChests() {
        return chests;
    }

    public List<AirdropSpot> getAirdrops() {
        return airdrops;
    }

    public List<Location> toPedestalLocations(World world) {
        List<Location> list = new ArrayList<>(pedestals.size());
        for (PedestalEntry p : pedestals) {
            list.add(new Location(world, p.x() + 0.5, p.y() + 1.0, p.z() + 0.5, p.yaw(), 0f));
        }
        return list;
    }

    public List<BlockEdit> exportDiff(BitSet prevOccupied) {
        BitSet nextOccupied = new BitSet(grid.length);
        List<BlockEdit> edits = new ArrayList<>(300_000);
        for (int dy = 0; dy < HEIGHT; dy++) {
            int y = MIN_Y + dy;
            for (int dz = 0; dz < SIZE; dz++) {
                int z = dz - MAX_R;
                for (int dx = 0; dx < SIZE; dx++) {
                    int idx = (dy * SIZE + dz) * SIZE + dx;
                    int val = grid[idx];
                    if (val != 0) {
                        nextOccupied.set(idx);
                        edits.add(new BlockEdit(dx - MAX_R, y, z, MATERIALS[val - 1]));
                    } else if (prevOccupied != null && prevOccupied.get(idx)) {
                        edits.add(new BlockEdit(dx - MAX_R, y, z, Material.AIR));
                    }
                }
            }
        }
        if (prevOccupied != null) {
            prevOccupied.clear();
            prevOccupied.or(nextOccupied);
        }
        return edits;
    }
}
