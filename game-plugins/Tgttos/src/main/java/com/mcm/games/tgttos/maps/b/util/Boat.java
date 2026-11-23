package com.mcm.games.tgttos.maps.b.util;

import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Predicate;

/**
 * Boat-on-ice lane check on one surface level: lane cells are ice / packed / blue ice (or snow = slush) with 2
 * passable blocks above. A clearance transform gives the lane width around every cell; a line search keeps to
 * cells of at least the requested width, then a 1-D boat model (vanilla: +0.04 b/tick², friction 0.98 ice, 0.989
 * blue ice, 0.6 other) with braking for bends estimates the time.
 */
public final class Boat {
    private final TgMapData d;
    private final int y;            // surface block level (boat sits at y + 1)
    private final Map<Long, Integer> clear = new HashMap<>();
    private final Map<Long, Double> fric = new HashMap<>();

    public Boat(TgMapData d, int surfaceY, Region area) {
        this.d = d;
        this.y = surfaceY;
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int x = area.minX() - 1; x <= area.maxX() + 1; x++)
            for (int z = area.minZ() - 1; z <= area.maxZ() + 1; z++) {
                double f = surface(x, z);
                if (f > 0) fric.put(k(x, z), f);
            }
        // multi-source BFS (Chebyshev) from non-lane cells
        for (int x = area.minX() - 1; x <= area.maxX() + 1; x++)
            for (int z = area.minZ() - 1; z <= area.maxZ() + 1; z++)
                if (!fric.containsKey(k(x, z))) { clear.put(k(x, z), 0); q.add(new int[]{x, z}); }
        while (!q.isEmpty()) {
            int[] c = q.poll();
            int cv = clear.get(k(c[0], c[1]));
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    int nx = c[0] + dx, nz = c[1] + dz;
                    if (nx < area.minX() - 1 || nx > area.maxX() + 1 || nz < area.minZ() - 1 || nz > area.maxZ() + 1) continue;
                    long kk = k(nx, nz);
                    if (clear.containsKey(kk)) continue;
                    clear.put(kk, cv + 1);
                    q.add(new int[]{nx, nz});
                }
        }
    }

    private static long k(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

    /** Friction of the lane surface, or 0 when the cell is not a lane cell. */
    private double surface(int x, int z) {
        String s = d.blocks.get(x, y, z);
        if (s == null) return 0;
        if (!Bs.passable(d.blocks.get(x, y + 1, z)) || !Bs.passable(d.blocks.get(x, y + 2, z))) return 0;
        String id = Bs.id(s);
        if (id.equals("blue_ice")) return 0.989;
        if (id.equals("ice") || id.equals("packed_ice")) return 0.98;
        if (id.equals("snow_block")) return 0.6;
        return 0;
    }

    /** Half-width class: 3 = lane at least 5 wide around the cell. */
    public int clearance(int x, int z) { Integer c = clear.get(k(x, z)); return c == null ? 0 : c; }

    public record Line(List<int[]> cells, double length, double time, int maxGapBetweenFeatures, String featureNote,
                       int slushCells, int blueCells, int minWidth) {}

    /** Cheapest path (by estimated time per cell) between two boxes keeping clearance >= minClear. */
    public Line route(Region from, Region to, int minClear, Predicate<int[]> allow, List<Region> features) {
        Map<Long, Double> best = new HashMap<>();
        Map<Long, long[]> prev = new HashMap<>();
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, b) -> Double.compare(a[0], b[0]));
        for (int x = from.minX(); x <= from.maxX(); x++)
            for (int z = from.minZ(); z <= from.maxZ(); z++)
                if (clearance(x, z) >= minClear) { best.put(k(x, z), 0.0); pq.add(new double[]{0, x, z}); }
        long endKey = Long.MIN_VALUE;
        while (!pq.isEmpty()) {
            double[] c = pq.poll();
            int x = (int) c[1], z = (int) c[2];
            if (c[0] > best.getOrDefault(k(x, z), Double.MAX_VALUE)) continue;
            if (to.contains(x + 0.5, to.minY() + 0.1, z + 0.5)) { endKey = k(x, z); break; }
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    int nx = x + dx, nz = z + dz;
                    if (clearance(nx, nz) < minClear || !allow.test(new int[]{nx, nz})) continue;
                    double f = fric.getOrDefault(k(nx, nz), 0.6);
                    double v = f >= 0.989 ? 60 : f >= 0.98 ? 35 : 1.5;
                    double nc = c[0] + Math.hypot(dx, dz) / v + 0.002 * (4 - Math.min(4, clearance(nx, nz)));
                    if (nc < best.getOrDefault(k(nx, nz), Double.MAX_VALUE)) {
                        best.put(k(nx, nz), nc);
                        prev.put(k(nx, nz), new long[]{x, z});
                        pq.add(new double[]{nc, nx, nz});
                    }
                }
        }
        if (endKey == Long.MIN_VALUE) return null;
        List<int[]> cells = new ArrayList<>();
        long cur = endKey;
        int cx = (int) (cur >> 32), cz = (int) cur;
        while (true) {
            cells.add(new int[]{cx, cz});
            long[] p = prev.get(k(cx, cz));
            if (p == null) break;
            cx = (int) p[0];
            cz = (int) p[1];
        }
        Collections.reverse(cells);
        return analyse(cells, features);
    }

    private Line analyse(List<int[]> cells, List<Region> features) {
        // smooth heading every 3 cells
        int n = cells.size();
        double len = 0;
        double[] seg = new double[n];
        for (int i = 1; i < n; i++) { seg[i] = Math.hypot(cells.get(i)[0] - cells.get(i - 1)[0], cells.get(i)[1] - cells.get(i - 1)[1]); len += seg[i]; }
        double[] turn = new double[n];   // heading change (deg) over +-4 cells
        for (int i = 4; i < n - 4; i++) {
            double h1 = Math.atan2(cells.get(i)[0] - cells.get(i - 4)[0], cells.get(i)[1] - cells.get(i - 4)[1]);
            double h2 = Math.atan2(cells.get(i + 4)[0] - cells.get(i)[0], cells.get(i + 4)[1] - cells.get(i)[1]);
            double dh = Math.toDegrees(Math.abs(Math.atan2(Math.sin(h2 - h1), Math.cos(h2 - h1))));
            turn[i] = dh;
        }
        // features: turns > 25 deg, overhead blocks / obstacles listed by the map, narrow lane (clearance <= 2)
        double since = 0;
        int maxGap = 0;
        String where = "";
        int slush = 0, blue = 0, minW = 99;
        double vmax[] = new double[n];
        for (int i = 0; i < n; i++) {
            int[] c = cells.get(i);
            since += seg[i];
            boolean feat = turn[i] > 25 || clearance(c[0], c[1]) <= 2;
            for (Region r : features) if (r.contains(c[0] + 0.5, r.minY() + 0.1, c[1] + 0.5)) feat = true;
            for (int dx = -2; dx <= 2 && !feat; dx++)
                for (int dz = -2; dz <= 2 && !feat; dz++)
                    if (!Bs.passable(d.blocks.get(c[0] + dx, y + 1, c[1] + dz)) || !Bs.passable(d.blocks.get(c[0] + dx, y + 3, c[1] + dz))) feat = true;
            if (feat) {
                if (since > maxGap) { maxGap = (int) Math.round(since); where = "ends at (" + c[0] + "," + c[1] + ")"; }
                since = 0;
            }
            double f = fric.getOrDefault(k(c[0], c[1]), 0.6);
            if (f < 0.9) slush++;
            if (f > 0.985) blue++;
            minW = Math.min(minW, 2 * clearance(c[0], c[1]) - 1);
            // bend speed limit (b/tick): sharp turns force braking
            vmax[i] = turn[i] > 60 ? 0.55 : turn[i] > 35 ? 0.8 : turn[i] > 20 ? 1.2 : 9;
        }
        if (since > maxGap) { maxGap = (int) Math.round(since); where = "at the end"; }
        // 1-D boat model with look-ahead braking
        double v = 0, t = 0;
        for (int i = 1; i < n; i++) {
            int[] c = cells.get(i);
            double f = fric.getOrDefault(k(c[0], c[1]), 0.6);
            double limit = 9;
            double dist = 0;
            for (int j = i; j < n && dist < 30; j++) { dist += seg[j]; limit = Math.min(limit, vmax[j] + dist * 0.035); }
            double remain = seg[i];
            while (remain > 1e-6) {
                v = v * f + 0.04;              // forward key held
                if (f < 0.9) v = Math.min(v, 0.075);
                if (v > limit) v = Math.max(limit, v * 0.9);    // braking (back key / steering scrub)
                double stepD = Math.min(remain, v);
                remain -= stepD;
                t += stepD / Math.max(v, 1e-3) / 20.0;
            }
        }
        return new Line(cells, len, t, maxGap, where, slush, blue, minW);
    }
}
