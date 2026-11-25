package com.mcm.games.parkourwarrior.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Route finder for imported sections (offline tool; never run by the server). Finds the EASIEST way from the start
 * cells to the goal cells through every waypoint over every standable block of the section (scenery included):
 * walking is cheap, jumps cost more the longer / higher they are, so the result is the way a player would really
 * take - which is what the obstacle's difficulty is judged on. The cell path is then cut into foothold nodes at
 * every jump (walk-connected cells form one node, widened by nearby flat cells for the run-up), ready for
 * {@link JumpSim}. Jumps the simulation finds impossible are banned and the route is searched again.
 *
 * <p>Jump model (generous; the simulation is the judge): flat or rising up to +0.5 with an air gap up to 3.3,
 * +0.5..+1.0 up to 3.0, +1.0..+1.25 only to the next block, drops (-1 and lower) up to 4.6, falls down to -10;
 * body room from 1.0 to 2.6 above the higher foothold along the line; ladders / vines catch within 2, exit within 2.
 */
final class Router {
    private Router() {}

    static final class Cell {
        final int x, y, z;
        final double feet;
        final boolean ladder;
        Cell(int x, int y, int z, double feet, boolean ladder) { this.x = x; this.y = y; this.z = z; this.feet = feet; this.ladder = ladder; }
        long key() { return W.key(x, y, z); }
        int[] arr() { return new int[]{x, y, z}; }
    }

    static final class Result {
        final List<Ob.Node> nodes = new ArrayList<>();
        final List<Cell> path = new ArrayList<>();
        /** Path cells of each node boundary (from, to): step i of the simulation = jumps.get(i). */
        final List<Cell[]> jumps = new ArrayList<>();
        String error;
        int cells;
    }

    /** Banned jump edges (from key, to key), shared between attempts. */
    static final class Bans {
        final Set<Long> pairs = new HashSet<>();
        void ban(long a, long b) { pairs.add(a * 31 + b * 1_000_003L); }
        boolean banned(long a, long b) { return pairs.contains(a * 31 + b * 1_000_003L); }
    }

    final static class Grid {
        final Phys.World wd;
        final Map<Long, Cell> cells = new HashMap<>();
        final Map<Long, List<Cell>> byCol = new HashMap<>();
        final int killY;
        Grid(Phys.World wd, int[] box, int killY, List<int[]> avoid) {
            this.wd = wd;
            this.killY = killY;
            for (int x = box[0]; x <= box[3]; x++) for (int z = box[2]; z <= box[5]; z++) for (int y = Math.max(box[1], killY - 1); y <= box[4]; y++) {
                String s = wd.state(x, y, z);
                if (s == null) continue;
                boolean lad = Phys.climbable(s);
                double t = Phys.top(s);
                if (!lad && t <= 0) continue;
                double feet = lad ? y : y + t;
                if (feet < killY) continue;
                if (inAvoid(avoid, x, y, z)) continue;
                if (!lad && !room(wd, x, feet, z)) continue;
                if (!lad && t > 1.0 && Phys.climbable(wd.state(x, y + 1, z))) continue;
                Cell c = new Cell(x, y, z, feet, lad);
                cells.put(c.key(), c);
                byCol.computeIfAbsent(W.key(x, 0, z), k -> new ArrayList<>()).add(c);
            }
        }
        Cell get(int x, int y, int z) { return cells.get(W.key(x, y, z)); }
    }

    static boolean inAvoid(List<int[]> avoid, int x, int y, int z) {
        if (avoid == null) return false;
        for (int[] a : avoid)
            if (x >= Math.min(a[0], a[3]) && x <= Math.max(a[0], a[3]) && y >= Math.min(a[1], a[4]) && y <= Math.max(a[1], a[4])
                    && z >= Math.min(a[2], a[5]) && z <= Math.max(a[2], a[5])) return true;
        return false;
    }

    static boolean room(Phys.World wd, int x, double feet, int z) {
        int a = (int) Math.floor(feet + 1e-6), b = (int) Math.floor(feet + 1.79);
        for (int y = a; y <= b; y++) {
            for (double[] bx : wd.boxes(x, y, z)) {
                if (bx[4] <= feet + 1e-6) continue;
                if (bx[3] > x + 0.2 && bx[0] < x + 0.8 && bx[5] > z + 0.2 && bx[2] < z + 0.8) return false;
            }
        }
        return true;
    }

    /** Edge cost, or -1. type[0] = 0 walk, 1 jump, 2 ladder move. */
    static double edge(Phys.World wd, Cell a, Cell b, int[] type) {
        int adx = Math.abs(a.x - b.x), adz = Math.abs(a.z - b.z);
        double dh = b.feet - a.feet;
        double g = Math.sqrt(Math.pow(Math.max(0, adx - 1), 2) + Math.pow(Math.max(0, adz - 1), 2));
        if (a.ladder && b.ladder) {
            type[0] = 2;
            if (adx + adz == 0) return Math.abs(a.y - b.y) == 1 ? 0.4 : -1;
            return adx <= 1 && adz <= 1 && Math.abs(dh) <= 1 ? 1.5 : -1;
        }
        if (b.ladder) {
            type[0] = 1;
            return g <= 2.0 && dh <= 1.0 && dh >= -4 && arc(wd, a, b) ? jumpCost(g, Math.max(0, dh)) : -1;
        }
        if (a.ladder) {
            type[0] = 1;
            return g <= 2.0 && dh <= 1.0 && dh >= -6 && arc(wd, a, b) ? jumpCost(g, 0) : -1;
        }
        if (adx <= 1 && adz <= 1) {
            if (adx + adz == 2 && !diagonalFree(wd, a, b)) return -1;
            if (dh <= 0.6001 && dh >= -3.0) { type[0] = 0; return adx + adz == 2 ? 1.41 : 1.0; }
            if (dh > 0.6 && dh <= 1.2501) { type[0] = 1; return 2.0 + (dh > 1.01 ? 4 : 0); }
            if (dh < -3 && dh >= -10) { type[0] = 0; return 2.0; }
            return -1;
        }
        type[0] = 1;
        if (dh > 1.2501 || dh < -10) return -1;
        double lim = dh > 1.0001 ? 1.0 : dh > 0.5001 ? 3.0 : dh > 0.01 ? 3.4 : dh > -0.99 ? 4.1 : Math.min(7.0, 4.7 + 0.35 * (-dh - 1));
        if (g > lim + 1e-6) return -1;
        if (!arc(wd, a, b)) return -1;
        return jumpCost(g, dh);
    }

    private static boolean diagonalFree(Phys.World wd, Cell a, Cell b) {
        // walking diagonally needs one of the two side columns free at body height
        double f = Math.max(a.feet, b.feet);
        return !Phys.blocked(wd, a.x + 0.5, f + 0.1, b.z + 0.5) || !Phys.blocked(wd, b.x + 0.5, f + 0.1, a.z + 0.5);
    }

    static double jumpCost(double g, double dh) {
        double eff = g + (dh > 0 ? dh : dh * 0.25);
        double c = eff <= 1 ? 0 : eff <= 2 ? 1 : eff <= 3 ? 3 : eff <= 3.6 ? 9 : eff <= 4.1 ? 30 : 90;
        return 2.5 + c;
    }

    static boolean arc(Phys.World wd, Cell a, Cell b) {
        double hi = Math.max(a.feet, b.feet);
        double ax = a.x + 0.5, az = a.z + 0.5, bx = b.x + 0.5, bz = b.z + 0.5;
        int steps = (int) Math.ceil(Math.hypot(bx - ax, bz - az) * 4);
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            int cx = (int) Math.floor(ax + (bx - ax) * t), cz = (int) Math.floor(az + (bz - az) * t);
            if ((cx == a.x && cz == a.z) || (cx == b.x && cz == b.z)) continue;
            // body room over the gap (head hitters are allowed: the simulation judges the real arc)
            int y0 = (int) Math.floor(hi + 1.0), y1 = (int) Math.floor(hi + 1.95);
            for (int y = y0 - 1; y <= y1; y++) for (double[] bb : wd.boxes(cx, y, cz))
                if (bb[4] > hi + 1.0 && bb[1] < hi + 1.95) return false;
        }
        return true;
    }

    /** Dijkstra from {@code from} to any cell in {@code to}. */
    static List<Cell> search(Grid gr, Set<Long> from, Set<Long> to, Bans bans) {
        Map<Long, Double> dist = new HashMap<>();
        Map<Long, Long> prev = new HashMap<>();
        PriorityQueue<double[]> pq = new PriorityQueue<>((p, q) -> Double.compare(p[0], q[0]));
        for (long k : from) if (gr.cells.containsKey(k)) { dist.put(k, 0.0); prev.put(k, k); pq.add(new double[]{0, Double.longBitsToDouble(k)}); }
        int[] type = new int[1];
        while (!pq.isEmpty()) {
            double[] e = pq.poll();
            long k = Double.doubleToRawLongBits(e[1]);
            if (e[0] > dist.getOrDefault(k, Double.MAX_VALUE)) continue;
            if (to.contains(k)) {
                List<Cell> path = new ArrayList<>();
                long c = k;
                while (true) { path.add(0, gr.cells.get(c)); long p = prev.get(c); if (p == c) break; c = p; }
                return path;
            }
            Cell a = gr.cells.get(k);
            for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
                List<Cell> col = gr.byCol.get(W.key(a.x + dx, 0, a.z + dz));
                if (col == null) continue;
                for (Cell b : col) {
                    if (b == a) continue;
                    double c = edge(gr.wd, a, b, type);
                    if (c < 0) continue;
                    if (type[0] == 1 && bans.banned(k, b.key())) continue;
                    double nd = e[0] + c;
                    long bk = b.key();
                    if (nd < dist.getOrDefault(bk, Double.MAX_VALUE)) {
                        dist.put(bk, nd); prev.put(bk, k);
                        pq.add(new double[]{nd, Double.longBitsToDouble(bk)});
                    }
                }
            }
        }
        return null;
    }

    /** Diagnostics: the n reachable cells closest to (gx, gy, gz). */
    static List<Cell> frontier(Grid gr, List<int[]> start, int gx, int gy, int gz, int n) {
        ArrayDeque<Cell> q = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        for (int[] c : start) { Cell k = gr.get(c[0], c[1], c[2]); if (k != null && seen.add(k.key())) q.add(k); }
        List<Cell> all = new ArrayList<>();
        int[] type = new int[1];
        while (!q.isEmpty()) {
            Cell a = q.poll();
            all.add(a);
            for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) {
                List<Cell> col = gr.byCol.get(W.key(a.x + dx, 0, a.z + dz));
                if (col == null) continue;
                for (Cell b : col) if (!seen.contains(b.key()) && edge(gr.wd, a, b, type) >= 0) { seen.add(b.key()); q.add(b); }
            }
        }
        all.sort((p, r) -> Double.compare(Math.hypot(Math.hypot(p.x - gx, p.z - gz), p.y - gy), Math.hypot(Math.hypot(r.x - gx, r.z - gz), r.y - gy)));
        return all.subList(0, Math.min(n, all.size()));
    }

    /**
     * Route start -> vias -> goal. {@code box} = search box (x0,y0,z0,x1,y1,z1). Returns nodes ready for the simulation.
     */
    static Result route(Grid gr, List<int[]> start, List<int[]> vias, List<int[]> goal, Bans bans) {
        Result r = new Result();
        Phys.World wd = gr.wd;
        r.cells = gr.cells.size();
        List<Set<Long>> legs = new ArrayList<>();
        Set<Long> s0 = new HashSet<>();
        for (int[] c : start) s0.add(W.key(c[0], c[1], c[2]));
        legs.add(s0);
        for (int[] v : vias) { Set<Long> s = new HashSet<>(); s.add(W.key(v[0], v[1], v[2])); legs.add(s); }
        Set<Long> g = new HashSet<>();
        for (int[] c : goal) g.add(W.key(c[0], c[1], c[2]));
        legs.add(g);
        for (int i = 0; i + 1 < legs.size(); i++) {
            for (long k : legs.get(i + 1)) if (!gr.cells.containsKey(k)) {
                r.error = "waypoint/goal cell " + W.kx(k) + "," + W.ky(k) + "," + W.kz(k) + " is not standable";
                return r;
            }
            Set<Long> from = i == 0 ? legs.get(0) : Set.of(r.path.get(r.path.size() - 1).key());
            List<Cell> p = search(gr, from, legs.get(i + 1), bans);
            if (p == null) { r.error = "no route for leg " + i + " (to " + describe(legs.get(i + 1)) + ")"; return r; }
            if (!r.path.isEmpty()) p.remove(0);
            r.path.addAll(p);
        }
        // cut into nodes at every jump
        int[] type = new int[1];
        List<List<Cell>> groups = new ArrayList<>();
        List<Cell> cur = new ArrayList<>();
        cur.add(r.path.get(0));
        for (int i = 1; i < r.path.size(); i++) {
            Cell a = r.path.get(i - 1), b = r.path.get(i);
            edge(wd, a, b, type);
            boolean ladderSwitch = a.ladder != b.ladder;
            if (type[0] == 1 || ladderSwitch) { groups.add(cur); cur = new ArrayList<>(); r.jumps.add(new Cell[]{a, b}); }
            cur.add(b);
        }
        groups.add(cur);
        Set<Long> used = new HashSet<>();
        for (List<Cell> gl : groups) for (Cell c : gl) used.add(c.key());
        for (int gi = 0; gi < groups.size(); gi++) {
            List<Cell> gl = groups.get(gi);
            boolean lad = gl.get(0).ladder;
            Ob.Node n = new Ob.Node(lad ? Ob.T.LADDER : Ob.T.AREA);
            Set<Long> in = new HashSet<>();
            if (gi == 0) for (long k : s0) { Cell c = gr.cells.get(k); if (c != null && in.add(k)) n.cells.add(c.arr()); }
            if (gi == groups.size() - 1 && !lad) for (long k : g) { Cell c = gr.cells.get(k); if (c != null && in.add(k)) n.cells.add(c.arr()); }
            for (Cell c : gl) if (in.add(c.key())) n.cells.add(c.arr());
            if (!lad) widen(gr, n, in, used, gl);
            boolean ice = true;
            for (int[] c : n.cells) { String id = Bs.id(wd.state(c[0], c[1], c[2])); if (!id.contains("ice")) { ice = false; break; } }
            if (ice && !lad) n = retype(n, Ob.T.ICE);
            r.nodes.add(n);
        }
        return r;
    }

    private static Ob.Node retype(Ob.Node n, Ob.T t) {
        Ob.Node m = new Ob.Node(t);
        m.cells.addAll(n.cells);
        m.note = n.note;
        return m;
    }

    /** Add flat walk-connected cells at the same feet height within 3 blocks of the group (run-up room), max 48. */
    private static void widen(Grid gr, Ob.Node n, Set<Long> in, Set<Long> used, List<Cell> gl) {
        ArrayDeque<Cell> q = new ArrayDeque<>(gl);
        int added = 0;
        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty() && added < 48) {
            Cell c = q.poll();
            for (int[] d : d4) {
                List<Cell> col = gr.byCol.get(W.key(c.x + d[0], 0, c.z + d[1]));
                if (col == null) continue;
                for (Cell b : col) {
                    if (b.ladder || Math.abs(b.feet - c.feet) > 1e-6 || in.contains(b.key()) || used.contains(b.key())) continue;
                    boolean near = false;
                    for (Cell o : gl) if (Math.max(Math.abs(o.x - b.x), Math.abs(o.z - b.z)) <= 3) { near = true; break; }
                    if (!near) continue;
                    in.add(b.key());
                    used.add(b.key());
                    n.cells.add(b.arr());
                    q.add(b);
                    added++;
                }
            }
        }
    }

    private static String describe(Set<Long> s) {
        long k = s.iterator().next();
        return W.kx(k) + "," + W.ky(k) + "," + W.kz(k) + (s.size() > 1 ? " +" + (s.size() - 1) : "");
    }
}
