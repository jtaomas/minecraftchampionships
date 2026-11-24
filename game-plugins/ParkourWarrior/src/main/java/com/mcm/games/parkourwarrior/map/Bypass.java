package com.mcm.games.parkourwarrior.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bypass search (research 2.3): from an obstacle's start area, flood-fill over EVERY standable block near the
 * obstacle (decoration, frames, rails and side ledges included; timed blocks counted as present) with an
 * easy-jump model, with the intended footholds removed. If the completion pad is still reachable, the obstacle
 * has a bypass. Run once with all intended footholds removed and once per foothold (skippable foothold).
 *
 * <p>Easy-jump model (deliberately generous so it finds more than a casual player would): walk to a neighbour
 * cell (8 directions) rising at most 0.6 or dropping any amount down to 5; jump flat or down with a horizontal gap
 * of up to 3 air blocks (3.5 when dropping 1+), rise up to +1.0 with a gap of up to 2; the arc needs free body room
 * from 1.0 to 2.8 above the higher foothold over the gap columns (jumping over low stuff is allowed); ladders and
 * vines connect vertically and to cells within 2 (catch) / 1 (exit); 2 blocks of headroom on every foothold.
 */
final class Bypass {
    private Bypass() {}

    static final class Cell {
        final int x, y, z;
        final double feet;
        final boolean ladder;
        Cell(int x, int y, int z, double feet, boolean ladder) { this.x = x; this.y = y; this.z = z; this.feet = feet; this.ladder = ladder; }
        long key() { return W.key(x, y, z); }
    }

    static List<String> check(Phys.World wd, Ob o, int killY) {
        List<String> out = new ArrayList<>();
        int[] bb = Validate.bbox(o);
        int m = 5;
        int x0 = bb[0] - m, x1 = bb[3] + m, z0 = bb[2] - m, z1 = bb[5] + m;
        int y0 = killY, y1 = o.maxFeet() + 4;
        // standable cells
        Map<Long, Cell> cells = new HashMap<>();
        Map<Long, List<Cell>> byCol = new HashMap<>();
        for (int x = x0; x <= x1; x++) for (int z = z0; z <= z1; z++) for (int y = y0 - 1; y <= y1; y++) {
            String s = wd.state(x, y, z);
            if (s == null) continue;
            boolean lad = Phys.climbable(s);
            double t = Phys.top(s);
            if (!lad && t <= 0) continue;
            double feet = lad ? y : y + t;
            if (feet < killY) continue;
            if (!lad && !room(wd, x, feet, z)) continue;
            Cell c = new Cell(x, y, z, feet, lad);
            cells.put(c.key(), c);
            byCol.computeIfAbsent(W.key(x, 0, z), k -> new ArrayList<>()).add(c);
        }
        Set<Long> start = new HashSet<>(), goal = new HashSet<>();
        for (int[] c : o.route.get(0).cells) start.add(W.key(c[0], c[1], c[2]));
        for (int[] c : o.route.get(o.route.size() - 1).cells) goal.add(W.key(c[0], c[1], c[2]));
        Set<Long> intended = new HashSet<>();
        for (int i = 1; i < o.route.size() - 1; i++) for (int[] c : o.route.get(i).cells) intended.add(W.key(c[0], c[1], c[2]));
        intended.removeAll(start);
        intended.removeAll(goal);
        Map<Long, List<Long>> adj = new HashMap<>();
        for (Cell c : cells.values()) {
            List<Long> l = new ArrayList<>();
            for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
                List<Cell> col = byCol.get(W.key(c.x + dx, 0, c.z + dz));
                if (col == null) continue;
                for (Cell d : col) if (d != c && edge(wd, c, d)) l.add(d.key());
            }
            adj.put(c.key(), l);
        }
        String all = reach(adj, start, goal, intended);
        if (all != null) out.add("BYPASS: the pad is reachable without any intended foothold (" + all + ")");
        else for (int i = 1; i < o.route.size() - 1; i++) {
            Set<Long> rem = new HashSet<>();
            for (int[] c : o.route.get(i).cells) rem.add(W.key(c[0], c[1], c[2]));
            rem.removeAll(start);
            rem.removeAll(goal);
            if (rem.isEmpty()) continue;
            String via = reach(adj, start, goal, rem);
            if (via != null) { out.add("BYPASS: foothold " + i + " (" + Validate.c(o.route.get(i).cells.get(0)) + ") can be skipped (" + via + ")"); break; }
        }
        return out;
    }

    private static boolean room(Phys.World wd, int x, double feet, int z) {
        int a = (int) Math.floor(feet + 1e-6), b = (int) Math.floor(feet + 1.79);
        for (int y = a; y <= b; y++) {
            for (double[] bx : wd.boxes(x, y, z)) {
                // ignore the block's own top part that pokes into this cell below the feet
                if (bx[4] <= feet + 1e-6) continue;
                if (bx[3] > x + 0.2 && bx[0] < x + 0.8 && bx[5] > z + 0.2 && bx[2] < z + 0.8) return false;
            }
        }
        return true;
    }

    private static boolean edge(Phys.World wd, Cell a, Cell b) {
        int adx = Math.abs(a.x - b.x), adz = Math.abs(a.z - b.z);
        double dh = b.feet - a.feet;
        if (a.ladder && b.ladder) return adx + adz == 0 ? Math.abs(a.y - b.y) == 1 : (adx <= 2 && adz <= 2 && Math.abs(dh) <= 1);
        double g = Math.sqrt(Math.pow(Math.max(0, adx - 1), 2) + Math.pow(Math.max(0, adz - 1), 2));
        if (b.ladder) return g <= 2.0 && dh <= 1.0 && dh >= -4;
        if (a.ladder) return g <= 1.0 && dh <= 1.0 && dh >= -4;
        if (adx <= 1 && adz <= 1) return dh <= 0.6001 && dh >= -5;
        if (dh > 1.0001 || dh < -5) return false;
        double lim = dh > 0.01 ? 2.0 : dh <= -0.99 ? 3.5 : 3.0;
        if (g > lim + 1e-6) return false;
        // arc: free body room over the gap columns
        double hi = Math.max(a.feet, b.feet);
        double ax = a.x + 0.5, az = a.z + 0.5, bx = b.x + 0.5, bz = b.z + 0.5;
        int steps = (int) Math.ceil(Math.hypot(bx - ax, bz - az) * 4);
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            int cx = (int) Math.floor(ax + (bx - ax) * t), cz = (int) Math.floor(az + (bz - az) * t);
            if ((cx == a.x && cz == a.z) || (cx == b.x && cz == b.z)) continue;
            int y0 = (int) Math.floor(hi + 1.0), y1 = (int) Math.floor(hi + 2.8);
            for (int y = y0; y <= y1; y++) for (double[] bb : wd.boxes(cx, y, cz))
                if (bb[4] > hi + 1.0 && bb[1] < hi + 2.8) return false;
        }
        return true;
    }

    private static String reach(Map<Long, List<Long>> adj, Set<Long> start, Set<Long> goal, Set<Long> removed) {
        ArrayDeque<Long> q = new ArrayDeque<>();
        Map<Long, Long> prev = new HashMap<>();
        for (long s : start) if (adj.containsKey(s)) { q.add(s); prev.put(s, s); }
        while (!q.isEmpty()) {
            long c = q.poll();
            if (goal.contains(c)) {
                // describe the first off-route cell on the path
                long k = c;
                List<Long> path = new ArrayList<>();
                while (prev.get(k) != k) { path.add(k); k = prev.get(k); }
                StringBuilder sb = new StringBuilder("via ");
                int n = 0;
                for (int i = path.size() - 1; i >= 0 && n < 3; i--) {
                    long p = path.get(i);
                    if (start.contains(p) || goal.contains(p)) continue;
                    sb.append(W.kx(p)).append(',').append(W.ky(p)).append(',').append(W.kz(p)).append(' ');
                    n++;
                }
                return sb.toString().trim();
            }
            for (long d : adj.get(c)) {
                if (prev.containsKey(d) || removed.contains(d)) continue;
                prev.put(d, c);
                q.add(d);
            }
        }
        return null;
    }
}
