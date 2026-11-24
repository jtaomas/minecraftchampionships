package com.mcm.games.bingo;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Finds the nearest sizeable underground air pocket around a player without lagging the server:
 * chunk snapshots of the loaded chunks around the player are taken a couple per tick on the main thread, the scan runs
 * on an async thread, and the result is delivered back on the main thread.
 *
 * <p>Scan: the volume is split into 4x4x4 cells; a cell is "cave" if at least 36 of its 64 blocks are air and it is
 * at least 4 blocks below the column's surface. Connected cave cells form pockets; a pocket counts if it has at
 * least {@link #MIN_CELLS} cells (about 300+ air blocks). The pocket the player stands in is ignored.
 */
final class CaveFinder {
    static final int MIN_CELLS = 6;
    private static final int SNAPSHOTS_PER_TICK = 2;

    private final Plugin plugin;

    CaveFinder(Plugin plugin) { this.plugin = plugin; }

    /** {@code done} gets (target or null, scan ms) on the main thread. */
    void find(Player p, int radiusChunks, BiConsumer<Location, Double> done) {
        World w = p.getWorld();
        Location eye = p.getLocation();
        int pcx = eye.getBlockX() >> 4, pcz = eye.getBlockZ() >> 4;
        List<int[]> todo = new ArrayList<>();
        for (int dx = -radiusChunks; dx <= radiusChunks; dx++)
            for (int dz = -radiusChunks; dz <= radiusChunks; dz++) todo.add(new int[]{pcx + dx, pcz + dz});
        List<ChunkSnapshot> snaps = new ArrayList<>();
        new BukkitRunnable() {
            int i = 0;
            @Override public void run() {
                if (!p.isOnline() || !p.getWorld().equals(w)) { cancel(); return; }
                for (int k = 0; k < SNAPSHOTS_PER_TICK && i < todo.size(); k++, i++) {
                    int[] c = todo.get(i);
                    if (w.isChunkLoaded(c[0], c[1])) snaps.add(w.getChunkAt(c[0], c[1]).getChunkSnapshot(true, false, false));
                }
                if (i < todo.size()) return;
                cancel();
                final int minY = w.getMinHeight();
                final double px = eye.getX(), py = eye.getY(), pz = eye.getZ();
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    long t0 = System.nanoTime();
                    double[] res = scan(snaps, minY, px, py, pz);
                    double ms = (System.nanoTime() - t0) / 1e6;
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (!p.isOnline()) return;
                        done.accept(res == null ? null : new Location(w, res[0], res[1], res[2]), ms);
                    });
                });
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    /** Returns {x, y, z} of the nearest qualifying cave cell centre, or null. Pure; safe off the main thread. */
    static double[] scan(List<ChunkSnapshot> snaps, int worldMinY, double px, double py, double pz) {
        int yLo = Math.max(worldMinY + 4, (int) py - 56), yHi = (int) py + 24;
        yLo = (yLo >> 2) << 2;
        Set<Long> cave = new HashSet<>();
        for (ChunkSnapshot s : snaps) {
            int baseX = s.getX() << 4, baseZ = s.getZ() << 4;
            for (int gx = 0; gx < 16; gx += 4)
                for (int gz = 0; gz < 16; gz += 4) {
                    int surf = s.getHighestBlockYAt(gx + 2, gz + 2);
                    int top = Math.min(yHi, surf - 4);
                    for (int gy = yLo; gy + 3 <= top; gy += 4) {
                        int air = 0;
                        for (int dx = 0; dx < 4; dx++)
                            for (int dz = 0; dz < 4; dz++)
                                for (int dy = 0; dy < 4; dy++)
                                    if (s.getBlockType(gx + dx, gy + dy, gz + dz).isAir()) air++;
                        if (air >= 36) cave.add(key((baseX + gx) >> 2, gy >> 2, (baseZ + gz) >> 2));
                    }
                }
        }
        if (cave.isEmpty()) return null;
        long playerKey = key(((int) Math.floor(px)) >> 2, ((int) Math.floor(py)) >> 2, ((int) Math.floor(pz)) >> 2);
        Set<Long> seen = new HashSet<>();
        double best = Double.MAX_VALUE;
        double[] out = null;
        for (long start : cave) {
            if (seen.contains(start)) continue;
            List<Long> cluster = new ArrayList<>();
            ArrayDeque<Long> q = new ArrayDeque<>();
            q.add(start);
            seen.add(start);
            boolean mine = false;
            while (!q.isEmpty()) {
                long k = q.poll();
                cluster.add(k);
                if (near(k, playerKey)) mine = true;
                int x = kx(k), y = ky(k), z = kz(k);
                for (int[] d : NB) {
                    long n = key(x + d[0], y + d[1], z + d[2]);
                    if (cave.contains(n) && seen.add(n)) q.add(n);
                }
            }
            if (mine || cluster.size() < MIN_CELLS) continue;
            for (long k : cluster) {
                double cx = (kx(k) << 2) + 2, cy = (ky(k) << 2) + 2, cz = (kz(k) << 2) + 2;
                double d = (cx - px) * (cx - px) + (cy - py) * (cy - py) * 1.5 + (cz - pz) * (cz - pz);
                if (d < best) { best = d; out = new double[]{cx, cy, cz}; }
            }
        }
        return out;
    }

    private static final int[][] NB = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private static boolean near(long a, long b) {
        return Math.abs(kx(a) - kx(b)) <= 1 && Math.abs(ky(a) - ky(b)) <= 1 && Math.abs(kz(a) - kz(b)) <= 1;
    }

    private static long key(int x, int y, int z) { return ((long) (x & 0x3FFFFF) << 42) | ((long) (z & 0x3FFFFF) << 20) | (y & 0xFFFFF); }
    private static int kx(long k) { return (int) (k << 0 >> 42); }
    private static int kz(long k) { return (int) ((k << 22) >> 42); }
    private static int ky(long k) { return (int) ((k << 44) >> 44); }

    /** "NE", "S", ... for a horizontal direction. */
    static String compass(Vector d) {
        double ang = Math.toDegrees(Math.atan2(d.getX(), -d.getZ())); // 0 = north (-Z), 90 = east (+X)
        if (ang < 0) ang += 360;
        String[] n = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        return n[(int) Math.round(ang / 45) % 8];
    }
}
