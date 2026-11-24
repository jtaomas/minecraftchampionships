package com.mcm.games.rocketspleef.map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bounded pure-Java voxel canvas for Rocket Spleef Rush floating void arenas.
 * Exports placed solid blocks sorted outer-to-inner for clean crumbling and fast clearing.
 */
public final class RsCanvas {
    public static final int MAX_R = 46;
    public static final int SIZE = MAX_R * 2 + 1;
    public static final int MIN_Y = 64;
    public static final int MAX_Y = 128;
    public static final int HEIGHT = MAX_Y - MIN_Y + 1;

    public record Pos(int x, int y, int z) {}

    private static final Material[] MATERIALS = Material.values();

    private final short[] grid = new short[SIZE * HEIGHT * SIZE];
    private final List<Pos> spawns = new ArrayList<>();

    public RsCanvas() {
        Arrays.fill(grid, (short) 0);
    }

    private int index(int x, int y, int z) {
        int dx = x + MAX_R;
        int dy = y - MIN_Y;
        int dz = z + MAX_R;
        if (dx < 0 || dx >= SIZE || dy < 0 || dy >= HEIGHT || dz < 0 || dz >= SIZE) return -1;
        return (dy * SIZE + dz) * SIZE + dx;
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

    public void disc(int cx, int y, int cz, double r, Material mat) {
        int ri = (int) Math.ceil(r);
        double r2 = r * r;
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dz = -ri; dz <= ri; dz++) {
                if (dx * dx + dz * dz <= r2) {
                    set(cx + dx, y, cz + dz, mat);
                }
            }
        }
    }

    public void ring(int cx, int y, int cz, double innerR, double outerR, Material mat) {
        int ri = (int) Math.ceil(outerR);
        double in2 = innerR * innerR;
        double out2 = outerR * outerR;
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dz = -ri; dz <= ri; dz++) {
                double d2 = dx * dx + dz * dz;
                if (d2 >= in2 && d2 <= out2) {
                    set(cx + dx, y, cz + dz, mat);
                }
            }
        }
    }

    public void taperedIsland(int cx, int topY, int cz, int radius, int depth,
                              Material topMat, Material subMat, Material tipMat) {
        disc(cx, topY, cz, radius, topMat);
        for (int d = 1; d <= depth; d++) {
            double frac = 1.0 - (double) d / (depth + 0.5);
            double r = Math.max(0.8, radius * frac);
            disc(cx, topY - d, cz, r, (d == depth) ? tipMat : subMat);
        }
    }

    public void bridge(int x1, int y1, int z1, int x2, int y2, int z2, int halfWidth, Material mat) {
        int steps = Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1))) * 2;
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int y = (int) Math.round(y1 + (y2 - y1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                    set(x + dx, y, z + dz, mat);
                }
            }
        }
    }

    public void pillar(int cx, int y1, int y2, int cz, double r, Material mat) {
        for (int y = y1; y <= y2; y++) {
            disc(cx, y, cz, r, mat);
        }
    }

    public void dome(int cx, int baseY, int cz, int radius, Material shellMat) {
        for (int dy = 0; dy <= radius; dy++) {
            double r = Math.sqrt(Math.max(0, radius * radius - dy * dy));
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.hypot(dx, dz);
                    if (d <= r + 0.4 && d >= r - 1.1) {
                        set(cx + dx, baseY + dy, cz + dz, shellMat);
                    }
                }
            }
        }
    }

    public void arch(int x1, int z1, int x2, int z2, int baseY, int peakH, int halfW, Material mat) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1)) * 2;
        if (steps == 0) return;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.round(x1 + (x2 - x1) * t);
            int z = (int) Math.round(z1 + (z2 - z1) * t);
            int y = baseY + (int) Math.round(Math.sin(Math.PI * t) * peakH);
            for (int dx = -halfW; dx <= halfW; dx++) {
                for (int dz = -halfW; dz <= halfW; dz++) {
                    set(x + dx, y, z + dz, mat);
                }
            }
        }
    }

    public void tree(int cx, int groundY, int cz, Material log, Material leaves, int height) {
        for (int y = groundY + 1; y <= groundY + height; y++) {
            set(cx, y, cz, log);
        }
        int crown = groundY + height;
        disc(cx, crown - 1, cz, 2.2, leaves);
        disc(cx, crown, cz, 2.2, leaves);
        disc(cx, crown + 1, cz, 1.3, leaves);
    }

    public void spawn(int x, int floorY, int z) {
        if (get(x, floorY, z) == Material.AIR) {
            set(x, floorY, z, Material.SMOOTH_STONE);
        }
        for (int dy = 1; dy <= 3; dy++) {
            set(x, floorY + dy, z, Material.AIR);
        }
        spawns.add(new Pos(x, floorY, z));
    }

    public List<Pos> getSpawns() {
        return spawns;
    }

    /**
     * Flush non-air blocks into {@code world} and return all solid block positions sorted outer-to-inner.
     */
    public List<Pos> buildInto(World world) {
        List<Pos> placed = new ArrayList<>(16_000);
        for (int dy = 0; dy < HEIGHT; dy++) {
            int y = MIN_Y + dy;
            for (int dz = 0; dz < SIZE; dz++) {
                int z = dz - MAX_R;
                for (int dx = 0; dx < SIZE; dx++) {
                    int idx = (dy * SIZE + dz) * SIZE + dx;
                    int val = grid[idx];
                    if (val != 0) {
                        int x = dx - MAX_R;
                        Material mat = MATERIALS[val - 1];
                        if (world != null) {
                            Block b = world.getBlockAt(x, y, z);
                            if (b.getType() != mat) {
                                b.setType(mat, false);
                            }
                        }
                        placed.add(new Pos(x, y, z));
                    }
                }
            }
        }
        // Sort outermost blocks first with slight organic jitter for crumbling
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        record Weighted(Pos pos, double score) {}
        List<Weighted> weighted = new ArrayList<>(placed.size());
        for (Pos p : placed) {
            double dist = Math.hypot(p.x(), p.z()) + rng.nextDouble(-2.5, 2.5);
            weighted.add(new Weighted(p, dist));
        }
        weighted.sort(Comparator.comparingDouble(Weighted::score).reversed());
        List<Pos> ordered = new ArrayList<>(weighted.size());
        for (Weighted w : weighted) ordered.add(w.pos());
        return ordered;
    }

    public List<Location> spawnLocations(World world) {
        List<Location> list = new ArrayList<>(spawns.size());
        for (Pos p : spawns) {
            float yaw = (float) Math.toDegrees(Math.atan2(p.x(), -p.z()));
            list.add(new Location(world, p.x() + 0.5, p.y() + 1.05, p.z() + 0.5, yaw, 0f));
        }
        return list;
    }
}
