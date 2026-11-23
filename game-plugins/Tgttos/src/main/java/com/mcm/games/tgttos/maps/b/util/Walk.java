package com.mcm.games.tgttos.maps.b.util;

import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.function.Predicate;

/**
 * On-foot route search over the real blocks (pure Java). Nodes are feet cells. Moves: walk / step up 1 / drop,
 * ladders, running jumps (flat gap <= 4, minus 1 per block of rise, with 3 blocks of arc clearance) and,
 * optionally, bridging (place a block under the next cell, or pillar up 1) inside a build box. Costs are seconds
 * (sprint 5.6 b/s, bridging {@link #bridgeRate} b/s); the result reports the blocks placed.
 */
public final class Walk {
    public static final double SPRINT = 5.6;
    public double bridgeRate = 2.2;
    public double pillarTime = 0.55;

    private final TgMapData.Blocks b;
    private final TgMapData d;
    /** Where blocks may be placed (null = no bridging). */
    public Region build;
    /** Extra filter on feet cells (e.g. stay inside a line's corridor). */
    public Predicate<int[]> allow = p -> true;
    /** Cost weight per placed block (raise it to find the least-blocks line). */
    public double blockWeight = 0;
    public int maxBlocks = 64;

    public Walk(TgMapData d, TgMapData.Blocks blocks) {
        this.d = d;
        this.b = blocks;
    }

    // ------------------------------------------------------------------ cell tests

    boolean pass(int x, int y, int z) { return Bs.passable(b.get(x, y, z)); }

    boolean hazard(int x, int y, int z) {
        if (y < d.voidY) return true;
        for (Region r : d.hazardRegions) if (r.contains(x + 0.5, y + 0.1, z + 0.5)) return true;
        if (!d.hazardBlocks.isEmpty()) {
            String f = b.get(x, y, z), u = b.get(x, y - 1, z);
            if (f != null && d.hazardBlocks.contains(Bs.full(Bs.head(f)))) return true;
            if (u != null && d.hazardBlocks.contains(Bs.full(Bs.head(u)))) return true;
        }
        return d.playBounds != null && !d.playBounds.contains(x + 0.5, y + 0.1, z + 0.5);
    }

    boolean ladder(int x, int y, int z) { return Bs.isLadder(b.get(x, y, z)); }

    /** Feet height when standing in cell (x,y,z) on real blocks, or NaN. */
    double feet(int x, int y, int z) {
        if (!pass(x, y, z) || !pass(x, y + 1, z)) return Double.NaN;
        if (hazard(x, y, z)) return Double.NaN;
        if (ladder(x, y, z)) return y;
        double t = Bs.standTop(b.get(x, y - 1, z));
        if (t < 0) {
            // stair/slab IN the feet cell is handled by the cell above it; water: allow swimming only at surface
            String s = b.get(x, y - 1, z);
            if (s != null && Bs.id(s).equals("water") && pass(x, y, z)) return Double.NaN;
            return Double.NaN;
        }
        if (t > 1.0) return Double.NaN;      // fence/wall tops: not a walking surface in this model
        return y - 1 + t;
    }

    public boolean standable(int x, int y, int z) { return !Double.isNaN(feet(x, y, z)); }

    // ------------------------------------------------------------------ search

    public static final class Node {
        public final int x, y, z;
        public double cost, len;
        public int blocks, jumps;
        public double maxGap;
        public String hardest = "none";
        public Node prev;
        public String how = "start";

        Node(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
    }

    public static final class Result {
        public Node end;
        public List<Node> path = new ArrayList<>();
        public int visited;
        public boolean ok() { return end != null; }
    }

    private static long key(int x, int y, int z) { return TgMapData.SparseBlocks.key(x, y, z); }

    private static final int[][] D8 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    /**
     * Multi-stage search: from the start cells through each waypoint box in order to the last box.
     * Returns the cheapest path (by time + blockWeight * blocks).
     */
    public Result route(List<int[]> starts, List<Region> stages) {
        List<Node> front = new ArrayList<>();
        for (int[] s : starts) {
            int y = s[1];
            for (int k = 0; k < 3 && !standable(s[0], y, s[2]); k++) y++;
            if (standable(s[0], y, s[2])) front.add(new Node(s[0], y, s[2]));
        }
        Result res = new Result();
        for (Region goal : stages) {
            Map<Long, Node> best = new HashMap<>();
            PriorityQueue<Node> pq = new PriorityQueue<>((a, c) -> Double.compare(score(a), score(c)));
            for (Node n : front) { best.put(key(n.x, n.y, n.z), n); pq.add(n); }
            List<Node> reached = new ArrayList<>();
            double bestGoal = Double.MAX_VALUE;
            while (!pq.isEmpty()) {
                Node n = pq.poll();
                if (best.get(key(n.x, n.y, n.z)) != n) continue;
                res.visited++;
                if (score(n) > bestGoal + 6) break;
                if (goal.contains(n.x + 0.5, n.y + 0.1, n.z + 0.5)) {
                    reached.add(n);
                    bestGoal = Math.min(bestGoal, score(n));
                    continue;
                }
                expand(n, best, pq);
            }
            if (reached.isEmpty()) return res;
            front = reached;
        }
        Node e = Collections.min(front, (a, c) -> Double.compare(score(a), score(c)));
        res.end = e;
        for (Node n = e; n != null; n = n.prev) res.path.add(n);
        Collections.reverse(res.path);
        return res;
    }

    private double score(Node n) { return n.cost + blockWeight * n.blocks; }

    private void offer(Node from, int x, int y, int z, double dt, double dl, int blocks, String how, double gap,
                       Map<Long, Node> best, PriorityQueue<Node> pq) {
        if (!allow.test(new int[]{x, y, z})) return;
        int nb = from.blocks + blocks;
        if (nb > maxBlocks) return;
        Node n = new Node(x, y, z);
        n.cost = from.cost + dt;
        n.len = from.len + dl;
        n.blocks = nb;
        n.jumps = from.jumps + (how.startsWith("jump") ? 1 : 0);
        n.maxGap = from.maxGap;
        n.hardest = from.hardest;
        if (gap > n.maxGap) { n.maxGap = gap; n.hardest = how + " at (" + x + "," + y + "," + z + ")"; }
        n.prev = from;
        n.how = how;
        long k = key(x, y, z);
        Node o = best.get(k);
        if (o != null && score(o) <= score(n)) return;
        best.put(k, n);
        pq.add(n);
    }

    private boolean placed(Node n) { return n.how.startsWith("bridge") || n.how.startsWith("pillar"); }

    private void expand(Node n, Map<Long, Node> best, PriorityQueue<Node> pq) {
        double fh = placed(n) ? n.y : feet(n.x, n.y, n.z);
        if (Double.isNaN(fh)) fh = n.y;
        boolean onLadder = ladder(n.x, n.y, n.z) || ladder(n.x, n.y + 1, n.z);
        // ladder climbing
        if (onLadder && standableOrLadder(n.x, n.y + 1, n.z)) offer(n, n.x, n.y + 1, n.z, 1 / 2.35, 1, 0, "ladder", 0, best, pq);
        for (int[] dd : D8) {
            int nx = n.x + dd[0], nz = n.z + dd[1];
            boolean diag = dd[0] != 0 && dd[1] != 0;
            double step = diag ? Math.sqrt(2) : 1;
            if (diag && (!pass(n.x + dd[0], n.y, n.z) || !pass(n.x + dd[0], n.y + 1, n.z)
                    || !pass(n.x, n.y, n.z + dd[1]) || !pass(n.x, n.y + 1, n.z + dd[1]))) {
                // corner blocked: no diagonal walk (jumps below still test their own line)
            } else {
                // walk: same level, step up (<= 1.0 with a jump if > 0.6), drop up to 24
                for (int dy = 1; dy >= -24; dy--) {
                    int ny = n.y + dy;
                    double f = feet(nx, ny, nz);
                    if (Double.isNaN(f)) {
                        if (dy <= 0 && !pass(nx, ny, nz)) break;
                        continue;
                    }
                    double rise = f - fh;
                    if (rise > 1.01) continue;
                    if (rise > 0.6 && !pass(n.x, n.y + 2, n.z)) continue;          // no room to jump up
                    if (dy == 1 && !pass(nx, n.y + 2, nz)) continue;
                    double dt = step / SPRINT + (rise > 0.6 ? 0.12 : 0) + (rise < -1 ? 0.04 * -rise : 0);
                    offer(n, nx, ny, nz, dt, step + Math.abs(rise), 0, rise < -1.5 ? "drop" : "walk", 0, best, pq);
                    break;
                }
            }
            // running jumps along this direction
            for (int dist = 2; dist <= 5; dist++) {
                int jx = n.x + dd[0] * dist, jz = n.z + dd[1] * dist;
                double hgap = (diag ? dist * Math.sqrt(2) : dist) - 1;   // air between the blocks
                if (hgap > 4.25) break;
                boolean clear = true;
                for (int k = 1; k < dist && clear; k++) {
                    int cx = n.x + dd[0] * k, cz = n.z + dd[1] * k;
                    for (int y = n.y; y <= n.y + 2; y++) if (!pass(cx, y, cz)) { clear = false; break; }
                    if (diag && clear) {   // the arc also passes the two side cells of each diagonal step
                        if (!pass(cx - dd[0], n.y + 1, cz) && !pass(cx, n.y + 1, cz - dd[1])) clear = false;
                    }
                }
                if (!clear) break;
                if (!pass(n.x, n.y + 2, n.z)) break;     // head-hitter at take-off kills the long jump
                boolean prevSupported = dist > 2 || true;
                for (int dy = 1; dy >= -8; dy--) {
                    int ny = n.y + dy;
                    double f = feet(jx, ny, jz);
                    if (Double.isNaN(f)) {
                        if (dy <= 0 && !pass(jx, ny, jz)) break;
                        continue;
                    }
                    double rise = f - fh;
                    double allowGap = 4 - Math.max(0, Math.ceil(rise - 0.01));
                    if (rise < -1.5) allowGap = 4.25;
                    if (hgap > allowGap + 0.01) break;
                    if (dy == 1 && !pass(jx, n.y + 2, jz)) break;
                    // skip trivial "jumps" over solid ground: at least one gap column must be non-standable
                    boolean realGap = false;
                    for (int k = 1; k < dist; k++)
                        if (!standable(n.x + dd[0] * k, n.y, n.z + dd[1] * k) && !standable(n.x + dd[0] * k, n.y + 1, n.z + dd[1] * k)) realGap = true;
                    if (!realGap || !prevSupported) break;
                    double horiz = diag ? dist * Math.sqrt(2) : dist;
                    String how = String.format("jump gap %.1f%s", hgap, rise > 0.1 ? String.format(" up %.1f", rise) : rise < -0.6 ? String.format(" down %.0f", -rise) : "");
                    double diff = hgap + Math.max(0, rise) * 1.0;
                    offer(n, jx, ny, jz, horiz / SPRINT + 0.15 + (hgap >= 3.5 ? 0.25 : 0), horiz + Math.abs(rise), 0, how, diff, best, pq);
                    break;
                }
            }
            // bridging: orthogonal only, place a block under the next cell (same level)
            if (build != null && !diag) {
                int ny = n.y;
                if (pass(nx, ny, nz) && pass(nx, ny + 1, nz) && Double.isNaN(feet(nx, ny, nz))
                        && Bs.isAir(b.get(nx, ny - 1, nz)) && build.contains(nx + 0.5, ny - 1 + 0.5, nz + 0.5)
                        && !hazard(nx, ny, nz)) {
                    offer(n, nx, ny, nz, 1 / bridgeRate, 1, 1, "bridge", 0, best, pq);
                }
            }
        }
        // pillar up one (place under yourself)
        if (build != null && pass(n.x, n.y + 2, n.z) && build.contains(n.x + 0.5, n.y + 0.5, n.z + 0.5)
                && Double.isNaN(feet(n.x, n.y + 1, n.z)) && !hazard(n.x, n.y + 1, n.z))
            offer(n, n.x, n.y + 1, n.z, pillarTime, 1, 1, "pillar", 0, best, pq);
    }

    private boolean standableOrLadder(int x, int y, int z) {
        return pass(x, y, z) && pass(x, y + 1, z) && !hazard(x, y, z);
    }

    // ------------------------------------------------------------------ metrics

    /** Longest run of standable cells at one feet level in a straight line along (dx,dz) through (x,y,z). */
    public int straightRun(int x, int y, int z, int dx, int dz, Predicate<int[]> stop) {
        if (!standable(x, y, z)) return 0;
        double f0 = feet(x, y, z);
        int n = 1;
        for (int s = 1; s < 80; s++) {
            int cx = x + dx * s, cz = z + dz * s;
            if (!standable(cx, y, cz) || Math.abs(feet(cx, y, cz) - f0) > 0.01 || stop.test(new int[]{cx, y, cz})) break;
            if (dx != 0 && dz != 0 && (!pass(cx - dx, y, cz) || !pass(cx, y, cz - dz) || !pass(cx - dx, y + 1, cz) || !pass(cx, y + 1, cz - dz))) break;
            n++;
        }
        for (int s = 1; s < 80; s++) {
            int cx = x - dx * s, cz = z - dz * s;
            if (!standable(cx, y, cz) || Math.abs(feet(cx, y, cz) - f0) > 0.01 || stop.test(new int[]{cx, y, cz})) break;
            if (dx != 0 && dz != 0 && (!pass(cx + dx, y, cz) || !pass(cx, y, cz + dz) || !pass(cx + dx, y + 1, cz) || !pass(cx, y + 1, cz + dz))) break;
            n++;
        }
        return n;
    }

    /**
     * Longest straight free stretch the line offers: for every walked node, the straight run of flat standable
     * cells through it along the line's local direction. Bridged cells are ignored.
     */
    public int[] longestStraight(List<Node> path, Predicate<int[]> skip) {
        int best = 0, bx = 0, by = 0, bz = 0;
        for (int i = 0; i < path.size(); i++) {
            Node n = path.get(i);
            if (placed(n) || skip.test(new int[]{n.x, n.y, n.z})) continue;
            Node a = path.get(Math.max(0, i - 3)), c = path.get(Math.min(path.size() - 1, i + 3));
            int dx = Integer.signum(c.x - a.x), dz = Integer.signum(c.z - a.z);
            if (Math.abs(c.x - a.x) * 2 < Math.abs(c.z - a.z)) dx = 0;
            if (Math.abs(c.z - a.z) * 2 < Math.abs(c.x - a.x)) dz = 0;
            if (dx == 0 && dz == 0) continue;
            int r = straightRun(n.x, n.y, n.z, dx, dz, skip);
            if (dx != 0 && dz != 0) r = (int) Math.round(r * Math.sqrt(2));
            if (r > best) { best = r; bx = n.x; by = n.y; bz = n.z; }
        }
        return new int[]{best, bx, by, bz};
    }
}
