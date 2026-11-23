package com.mcm.games.tgttos.maps.a.util;

import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Movement model + shortest-time search over the generated blocks (pure Java).
 *
 * <p>Nodes are feet cells a player can stand in (2 free cells, a top to stand on), ladder cells, water cells,
 * and - when bridging is allowed - "virtual" cells whose floor is air inside the build box (a block must be
 * placed there). Moves: walk (step up 0.6, stairs 1), bridge (1 block, orthogonal, same level), pillar
 * (1 block, straight up), ladder climb, swim, jump (gap measured edge to edge: flat gap <= 4, minus 1 per block
 * of rise, up to 1.25 rise; drops reach further, simulated vanilla fall time), and long falls (drop maps).
 * Costs are seconds (expert or median movement parameters).
 */
public final class Route {

    public static final class Params {
        public boolean bridge;
        public double sprint = 5.6, waterSpeed = 2.0, ladderUp = 2.35, ladderDown = 3.5;
        public double bridgeSec = 0.30, pillarSec = 0.40, blockPenalty = 0.10, jumpExtra = 0.12;
        public int maxBlocks = 64;
        public Region buildBox;
        public List<Region> corridor;       // null = anywhere in searchBox
        public Region searchBox;
        public boolean falls = false;
        public boolean noJumps = false;
        public double maxGap = 4.0;
        public int fallRadius = 17;

        public static Params expert(boolean bridge) {
            Params p = new Params();
            p.bridge = bridge;
            return p;
        }

        /** Median player: slower sprint, sneak-bridging, more time per jump. */
        public static Params median(boolean bridge) {
            Params p = new Params();
            p.bridge = bridge;
            p.sprint = 4.3;
            p.bridgeSec = 0.55;
            p.pillarSec = 0.55;
            p.jumpExtra = 0.45;
            p.ladderUp = 2.2;
            return p;
        }
    }

    public enum How { START, WALK, BRIDGE, PILLAR, JUMP, FALL, LADDER, SWIM }

    public record Step(int x, int y, int z, double h, How how, double gap, double rise, double t) {}

    public static final class Result {
        public final List<Step> path = new ArrayList<>();
        public double time;
        public int blocks;
        public double length;
        public double hardGap = 0, hardRiseAtGap = 0, maxRise = 0, maxFall = 0;
        public int jumps, gaps3plus, falls;
        public double longestSprint;
        public String sprintAt = "";
        public boolean ok;
        public String failedLeg = "";

        public String hardest() {
            if (jumps == 0) return "none";
            return String.format("gap %.1f%s", hardGap, hardRiseAtGap > 0.01 ? String.format(" rise +%.1f", hardRiseAtGap)
                    : hardRiseAtGap < -0.01 ? String.format(" drop %.1f", -hardRiseAtGap) : " flat");
        }
    }

    // ------------------------------------------------------------------ node model

    static final byte NONE = 0, SOLID = 1, LADDER = 2, WATER = 3, VIRTUAL = 4;

    private final TgMapData.Blocks b;
    private final Set<Long> removed;
    private final Params p;
    private final Map<Long, Long> cache = new HashMap<>();   // key -> packed (type, stairy, H*1000)
    private final Map<Long, int[]> columns = new HashMap<>();

    public Route(TgMapData.Blocks b, Collection<TgMapData.Pos> removedAtGo, Params p) {
        this.b = b;
        this.p = p;
        this.removed = new HashSet<>();
        if (removedAtGo != null) for (TgMapData.Pos q : removedAtGo) removed.add(key(q.x(), q.y(), q.z()));
    }

    static long key(int x, int y, int z) { return TgMapData.SparseBlocks.key(x, y, z); }

    String blk(int x, int y, int z) {
        if (!removed.isEmpty() && removed.contains(key(x, y, z))) return null;
        return b.get(x, y, z);
    }

    boolean pass(int x, int y, int z) { return Bs.passable(blk(x, y, z)); }

    private boolean allowed(int x, int y, int z) {
        if (p.searchBox != null && !p.searchBox.contains(x, y, z)) return false;
        if (p.corridor == null) return true;
        for (Region r : p.corridor) if (r.contains(x, y, z)) return true;
        return false;
    }

    /** Packed node info: bits 0..1 unused; type in bits 56..63, stairy bit 48, H*1000 in low 40 bits (offset). */
    private long info(int x, int fy, int z) {
        long k = key(x, fy, z);
        Long c = cache.get(k);
        if (c != null) return c;
        long v = compute(x, fy, z);
        cache.put(k, v);
        return v;
    }

    private static long pack(byte type, boolean stairy, double h) {
        return ((long) type << 56) | (stairy ? 1L << 48 : 0) | ((long) Math.round(h * 1000) + (1L << 39));
    }

    static byte type(long v) { return (byte) (v >>> 56); }

    static boolean stairy(long v) { return ((v >>> 48) & 1) != 0; }

    static double hOf(long v) { return ((v & ((1L << 40) - 1)) - (1L << 39)) / 1000.0; }

    private long compute(int x, int fy, int z) {
        if (!allowed(x, fy, z)) return pack(NONE, false, 0);
        String feet = blk(x, fy, z), head = blk(x, fy + 1, z);
        if (!Bs.passable(feet) || !Bs.passable(head)) return pack(NONE, false, 0);
        if (Bs.isLadder(feet)) return pack(LADDER, false, fy);
        if (feet != null && Bs.id(feet).equals("water")) return pack(WATER, false, fy);
        String below = blk(x, fy - 1, z);
        double st = Bs.standTop(below);
        if (st > 0) {
            double h = fy - 1 + st;
            if (h > fy + 0.01 && !Bs.passable(blk(x, fy + 2, z))) return pack(NONE, false, 0);
            String bid = Bs.id(below);
            boolean stairy = bid.endsWith("_stairs");
            return pack(SOLID, stairy, h);
        }
        if (p.bridge && Bs.isAir(below) && p.buildBox != null && p.buildBox.contains(x, fy - 1, z))
            return pack(VIRTUAL, false, fy);
        return pack(NONE, false, 0);
    }

    public boolean isStandable(int x, int fy, int z) {
        byte t = type(info(x, fy, z));
        return t == SOLID;
    }

    public double heightAt(int x, int fy, int z) { return hOf(info(x, fy, z)); }

    // ------------------------------------------------------------------ jump physics

    private static final double[] AIR = new double[200];      // ticks to fall to -d (index d*2), after a jump
    static {
        for (int i = 0; i < AIR.length; i++) {
            double target = -i / 2.0;
            double y = 0, vy = 0.42;
            int t = 0;
            while (t < 400) {
                y += vy; vy = (vy - 0.08) * 0.98; t++;
                if (vy < 0 && y <= target) break;
            }
            AIR[i] = t;
        }
    }

    static double airTicks(double drop) {
        int i = (int) Math.round(Math.max(0, drop) * 2);
        return i < AIR.length ? AIR[i] : AIR[AIR.length - 1] + (drop - (AIR.length - 1) / 2.0) / 3.9;
    }

    /** Max edge-to-edge gap for a sprint jump that lands {@code drop} blocks lower (calibrated: flat = 4). */
    public static double gapForDrop(double drop) { return 4.0 + 0.29 * (airTicks(drop) - airTicks(0)); }

    // ------------------------------------------------------------------ search

    private record Edge(int x, int y, int z, How how, double cost, int blocks, double gap, double rise) {}

    private static final int[][] D8 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};

    private List<Edge> edges(int x, int fy, int z) {
        List<Edge> out = new ArrayList<>();
        long ui = info(x, fy, z);
        byte ut = type(ui);
        double uh = hOf(ui);
        boolean ustair = stairy(ui);
        int orthWalk = 0;
        for (int[] d : D8) {
            int nx = x + d[0], nz = z + d[1];
            boolean diag = d[0] != 0 && d[1] != 0;
            for (int fy2 = fy + 1; fy2 >= fy - 1; fy2--) {
                long vi = info(nx, fy2, nz);
                byte vt = type(vi);
                if (vt == NONE) continue;
                double vh = hOf(vi), dh = vh - uh;
                boolean loose = ustair || stairy(vi) || ut == LADDER || vt == LADDER || ut == WATER;
                if (dh > (loose ? 1.01 : 0.61) || dh < -1.01) continue;
                if (vt == VIRTUAL) {
                    if (diag || Math.abs(dh) > 0.01 || ut == LADDER || ut == WATER) continue;
                    out.add(new Edge(nx, fy2, nz, How.BRIDGE, p.bridgeSec + p.blockPenalty, 1, 0, 0));
                    if (!diag) orthWalk++;
                    break;
                }
                if (diag) {
                    int ly = Math.max(fy, fy2);
                    if (!pass(x + d[0], ly, z) || !pass(x + d[0], ly + 1, z) || !pass(x, ly, z + d[1]) || !pass(x, ly + 1, z + d[1])) continue;
                }
                double dist = diag ? 1.4142 : 1.0;
                double speed = (ut == WATER || vt == WATER) ? p.waterSpeed : p.sprint;
                double c = dist / speed + (dh > 0.61 && vt != LADDER ? 0.15 : 0);
                out.add(new Edge(nx, fy2, nz, vt == WATER ? How.SWIM : How.WALK, c, 0, 0, 0));
                if (!diag) orthWalk++;
                break;
            }
        }
        // ladder / swim vertical
        if (ut == LADDER) {
            if (type(info(x, fy + 1, z)) != NONE) out.add(new Edge(x, fy + 1, z, How.LADDER, 1 / p.ladderUp, 0, 0, 1));
            if (type(info(x, fy - 1, z)) != NONE) out.add(new Edge(x, fy - 1, z, How.LADDER, 1 / p.ladderDown, 0, 0, -1));
        }
        if (ut == WATER && type(info(x, fy + 1, z)) != NONE) out.add(new Edge(x, fy + 1, z, How.SWIM, 0.45, 0, 0, 1));
        // pillar
        if (p.bridge && (ut == SOLID || ut == VIRTUAL) && Math.abs(uh - fy) < 0.01) {
            long vi = info(x, fy + 1, z);
            if (type(vi) == VIRTUAL) out.add(new Edge(x, fy + 1, z, How.PILLAR, p.pillarSec + p.blockPenalty, 1, 0, 1));
        }
        if (ut == LADDER || ut == WATER) return out;
        // jumps (only from edge nodes)
        if (!p.noJumps && (orthWalk < 4 || ut == VIRTUAL)) {
            for (int dx = -5; dx <= 5; dx++)
                for (int dz = -5; dz <= 5; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    double gx = Math.max(Math.abs(dx) - 1, 0), gz = Math.max(Math.abs(dz) - 1, 0);
                    double g = Math.sqrt(gx * gx + gz * gz);
                    if (g > Math.min(4.0, p.maxGap) + 0.0001) continue;
                    boolean adjacent = Math.abs(dx) <= 1 && Math.abs(dz) <= 1;
                    for (int fy2 = fy + 2; fy2 >= fy - 10; fy2--) {
                        long vi = info(x + dx, fy2, z + dz);
                        byte vt = type(vi);
                        if (vt == NONE || vt == VIRTUAL) {
                            if (!pass(x + dx, fy2, z + dz) && fy2 <= fy + 1) break;   // solid in the body range: column blocked
                            continue;
                        }
                        double vh = hOf(vi), rise = vh - uh;
                        if (rise > 1.26) continue;
                        if (adjacent && rise <= 0.61 && rise >= -1.01) break;      // plain walk
                        boolean ok = rise > 0 ? g <= 4.0 - rise + 1e-6 : g <= gapForDrop(-rise) + 1e-6;
                        if (!ok) break;
                        if (!clear(x, fy, z, x + dx, fy2, z + dz)) break;
                        double hd = Math.sqrt(dx * dx + dz * dz);
                        double air = (rise >= 0 ? 11 + rise * 3 : airTicks(-rise)) / 20.0;
                        double c = Math.max(hd / 5.0, air) + p.jumpExtra + (g >= 3 ? p.jumpExtra : 0);
                        out.add(new Edge(x + dx, fy2, z + dz, How.JUMP, c, 0, g, rise));
                        break;
                    }
                }
        }
        // long falls
        if (p.falls && ut == SOLID && orthWalk < 4) {
            int r = p.fallRadius;
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++) {
                    double gx = Math.max(Math.abs(dx) - 1, 0), gz = Math.max(Math.abs(dz) - 1, 0);
                    double g = Math.sqrt(gx * gx + gz * gz);
                    if (g > r) continue;
                    int[] col = column(x + dx, z + dz);
                    for (int fy2 : col) {               // descending
                        if (fy2 > fy - 11) continue;
                        long vi = info(x + dx, fy2, z + dz);
                        if (type(vi) != SOLID && type(vi) != WATER) continue;
                        double vh = hOf(vi), drop = uh - vh;
                        if (g > gapForDrop(drop) - 0.25) break;
                        if (!fallClear(x, fy, z, x + dx, fy2, z + dz)) break;
                        double c = airTicks(drop) / 20.0 + p.jumpExtra;
                        out.add(new Edge(x + dx, fy2, z + dz, How.FALL, c, 0, g, -drop));
                        break;
                    }
                }
        }
        return out;
    }

    /** Node feet heights in a column, descending (only SOLID/WATER nodes). */
    private int[] column(int x, int z) {
        long k = key(x, 0, z);
        int[] c = columns.get(k);
        if (c != null) return c;
        List<Integer> ys = new ArrayList<>();
        int y0 = p.searchBox != null ? p.searchBox.maxY() : 180, y1 = p.searchBox != null ? p.searchBox.minY() : -60;
        for (int y = y0; y >= y1; y--) {
            byte t = type(info(x, y, z));
            if (t == SOLID || t == WATER) ys.add(y);
        }
        c = ys.stream().mapToInt(Integer::intValue).toArray();
        columns.put(k, c);
        return c;
    }

    /** Body clearance along a jump: 2 cells at the higher feet level along the straight line. */
    private boolean clear(int x1, int y1, int z1, int x2, int y2, int z2) {
        int ly = Math.max(y1, y2);
        double len = Math.sqrt((x2 - x1) * (x2 - x1) + (z2 - z1) * (z2 - z1));
        int n = Math.max(2, (int) Math.ceil(len * 2.5));
        for (int i = 1; i < n; i++) {
            double t = i / (double) n;
            int cx = (int) Math.floor(x1 + 0.5 + (x2 - x1) * t), cz = (int) Math.floor(z1 + 0.5 + (z2 - z1) * t);
            if ((cx == x1 && cz == z1) || (cx == x2 && cz == z2)) continue;
            if (!pass(cx, ly, cz) || !pass(cx, ly + 1, cz)) return false;
        }
        for (int y = y2; y <= y1 + 1; y++) if (!pass(x2, y, z2)) return false;
        return true;
    }

    /** Fall clearance: the landing column is free from the landing feet up to the take-off head, and the mid point is free. */
    private boolean fallClear(int x1, int y1, int z1, int x2, int y2, int z2) {
        for (int y = y2; y <= y1 + 1; y++) if (!pass(x2, y, z2)) return false;
        int mx = (int) Math.floor((x1 + x2) / 2.0 + 0.5), mz = (int) Math.floor((z1 + z2) / 2.0 + 0.5);
        int my = (y1 + y2) / 2;
        for (int y = my; y <= y1 + 1; y++) if (!pass(mx, y, mz)) return false;
        return true;
    }

    private record Q(double cost, long k, int x, int y, int z, int blocks) {}

    private record Prev(long from, How how, double gap, double rise, double t) {}

    /** Shortest-time search from the start cells to any node inside the target box. */
    public Result search(List<int[]> starts, Region target) {
        Map<Long, Double> best = new HashMap<>();
        Map<Long, Prev> prev = new HashMap<>();
        Map<Long, int[]> pos = new HashMap<>();
        PriorityQueue<Q> pq = new PriorityQueue<>((a, c) -> Double.compare(a.cost, c.cost));
        for (int[] s : starts) {
            byte t = type(info(s[0], s[1], s[2]));
            if (t == NONE) continue;
            long k = key(s[0], s[1], s[2]);
            best.put(k, 0.0);
            pos.put(k, s);
            pq.add(new Q(0, k, s[0], s[1], s[2], 0));
        }
        Result r = new Result();
        long goal = Long.MIN_VALUE;
        int goalBlocks = 0;
        int expanded = 0;
        while (!pq.isEmpty()) {
            Q q = pq.poll();
            Double bc = best.get(q.k);
            if (bc != null && q.cost > bc + 1e-9) continue;
            if (target.contains(q.x, q.y, q.z) && type(info(q.x, q.y, q.z)) != VIRTUAL) { goal = q.k; goalBlocks = q.blocks; break; }
            if (++expanded > 3_000_000) break;
            for (Edge e : edges(q.x, q.y, q.z)) {
                int nb = q.blocks + e.blocks;
                if (nb > p.maxBlocks) continue;
                double nc = q.cost + e.cost;
                long nk = key(e.x, e.y, e.z);
                Double old = best.get(nk);
                if (old != null && old <= nc) continue;
                best.put(nk, nc);
                prev.put(nk, new Prev(q.k, e.how, e.gap, e.rise, e.cost));
                pos.put(nk, new int[]{e.x, e.y, e.z});
                pq.add(new Q(nc, nk, e.x, e.y, e.z, nb));
            }
        }
        if (goal == Long.MIN_VALUE) { r.ok = false; return r; }
        r.ok = true;
        List<Step> rev = new ArrayList<>();
        long k = goal;
        while (true) {
            int[] c = pos.get(k);
            Prev pv = prev.get(k);
            double h = hOf(info(c[0], c[1], c[2]));
            if (pv == null) { rev.add(new Step(c[0], c[1], c[2], h, How.START, 0, 0, 0)); break; }
            rev.add(new Step(c[0], c[1], c[2], h, pv.how, pv.gap, pv.rise, pv.t));
            k = pv.from;
        }
        for (int i = rev.size() - 1; i >= 0; i--) r.path.add(rev.get(i));
        r.blocks = goalBlocks;
        summarize(r, p);
        return r;
    }

    /** All nodes reachable from the starts (no target), as feet cells. */
    public Set<Long> reachable(List<int[]> starts, int limit) {
        Set<Long> seen = new HashSet<>();
        java.util.ArrayDeque<int[]> dq = new java.util.ArrayDeque<>();
        for (int[] s : starts) if (type(info(s[0], s[1], s[2])) != NONE && seen.add(key(s[0], s[1], s[2]))) dq.add(s);
        while (!dq.isEmpty() && seen.size() < limit) {
            int[] c = dq.poll();
            for (Edge e : edges(c[0], c[1], c[2])) {
                if (e.blocks > 0) continue;
                if (seen.add(key(e.x, e.y, e.z))) dq.add(new int[]{e.x, e.y, e.z});
            }
        }
        return seen;
    }

    static void summarize(Result r, Params p) {
        double t = 0, len = 0;
        for (int i = 1; i < r.path.size(); i++) {
            Step a = r.path.get(i - 1), s = r.path.get(i);
            t += s.t();
            double hd = Math.hypot(s.x() - a.x(), s.z() - a.z());
            len += hd + ((s.how() == How.LADDER || s.how() == How.PILLAR || s.how() == How.SWIM) ? Math.abs(s.y() - a.y()) : 0);
            if (s.how() == How.JUMP || s.how() == How.FALL) {
                if (s.how() == How.JUMP) r.jumps++; else r.falls++;
                if (s.how() == How.JUMP && s.gap() >= 2.99) r.gaps3plus++;
                double score = s.gap() + Math.max(0, s.rise());
                double best = r.hardGap + Math.max(0, r.hardRiseAtGap);
                if (s.how() == How.JUMP && score > best) { r.hardGap = s.gap(); r.hardRiseAtGap = s.rise(); }
                r.maxRise = Math.max(r.maxRise, s.rise());
                r.maxFall = Math.max(r.maxFall, -s.rise());
            }
        }
        r.time = t - r.blocks * p.blockPenalty;
        r.length = len;
        // longest flat straight sprint: maximal sub-path of flat WALK steps whose points stay within 0.75 of a straight line
        int n = r.path.size();
        double bestLen = 0;
        int bi = 0, bj = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                Step s = r.path.get(j);
                if (s.how() != How.WALK || Math.abs(s.h() - r.path.get(i).h()) > 0.01) break;
                Step a = r.path.get(i);
                double lx = s.x() - a.x(), lz = s.z() - a.z(), L = Math.hypot(lx, lz);
                boolean straight = true;
                for (int k = i + 1; k < j && straight; k++) {
                    Step m = r.path.get(k);
                    double cr = Math.abs((m.x() - a.x()) * lz - (m.z() - a.z()) * lx) / Math.max(L, 1e-9);
                    if (cr > 0.75) straight = false;
                }
                if (!straight) break;
                if (L > bestLen) { bestLen = L; bi = i; bj = j; }
            }
        }
        r.longestSprint = bestLen;
        if (bestLen > 0) {
            Step a = r.path.get(bi), c = r.path.get(bj);
            r.sprintAt = "(" + a.x() + "," + a.y() + "," + a.z() + ")->(" + c.x() + "," + c.y() + "," + c.z() + ")";
        }
    }

    /** Concatenate leg results. */
    public static Result join(List<Result> legs, Params p) {
        Result r = new Result();
        r.ok = true;
        for (Result l : legs) {
            if (!l.ok) { r.ok = false; break; }
            int from = r.path.isEmpty() ? 0 : 1;
            for (int i = from; i < l.path.size(); i++) r.path.add(l.path.get(i));
            r.blocks += l.blocks;
        }
        if (r.ok) summarize(r, p);
        return r;
    }
}
