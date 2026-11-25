import com.mcm.games.race.maps.b.CanalCourse;
import com.mcm.games.race.maps.b.FrozenCanal;
import com.mcm.games.race.maps.b.util.Bs;
import com.mcm.games.race.maps.b.util.Check;
import com.mcm.games.race.maps.b.util.G;
import com.mcm.games.race.maps.b.util.Walk;
import com.mcm.games.race.model.TgMapData;
import com.mcm.games.race.model.TgMapData.Region;
import com.mcm.games.race.model.TgMapData.Spawn;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Frozen Canal v2 validator: multi-level boat graph (one ice level per column, moves level or 1 down, never up),
 * continuity / trap pockets / open edges / snags / drop heights, lane widths, bridge headroom, the on-foot
 * escape flood (nobody climbs out of the canal before the finish), the on-foot finish routes, and timed SAFE / FAST
 * lines with a 1-D vanilla boat model.
 */
final class CanalV {
    private CanalV() {}

    static final int YMIN = -12, YMAX = 2;

    /** Ice level per column (Integer.MIN_VALUE = no lane), friction per column. */
    static final class Lanes {
        final Map<Long, Integer> lvl = new HashMap<>();
        final Map<Long, Double> fric = new HashMap<>();
        final Map<Long, Integer> clear = new HashMap<>();
        int x1, x2, z1, z2;

        int y(int x, int z) { Integer v = lvl.get(k(x, z)); return v == null ? Integer.MIN_VALUE : v; }
        boolean lane(int x, int z) { return lvl.containsKey(k(x, z)); }
        double f(int x, int z) { return fric.getOrDefault(k(x, z), 0.6); }
        int clearance(int x, int z) { return clear.getOrDefault(k(x, z), 0); }
    }

    static long k(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

    static double friction(String id) {
        return switch (id) { case "blue_ice" -> 0.989; case "ice", "packed_ice" -> 0.98; case "snow_block" -> 0.6; default -> 0; };
    }

    static Lanes lanes(TgMapData.Blocks b, CanalCourse c) {
        Lanes L = new Lanes();
        L.x1 = c.x1; L.x2 = c.x2; L.z1 = c.z1; L.z2 = c.z2;
        for (int x = c.x1; x <= c.x2; x++)
            for (int z = c.z1; z <= c.z2; z++) {
                int kd = c.kindAt(x, z);
                boolean tray = x >= -21 && x <= 20 && z >= 0 && z <= 8;
                if (kd != CanalCourse.LANE && kd != CanalCourse.FLAT && !tray) continue;   // the mere ice is not boat track
                for (int y = YMAX; y >= YMIN; y--) {
                    String s = b.get(x, y, z);
                    if (s == null) continue;
                    double f = friction(Bs.id(s));
                    if (f > 0 && Bs.passable(b.get(x, y + 1, z)) && Bs.passable(b.get(x, y + 2, z))) { L.lvl.put(k(x, z), y); L.fric.put(k(x, z), f); break; }
                    // a bridge deck / arch overhead: keep looking down for the ice
                }
            }
        // Chebyshev clearance from non-lane cells (2-D)
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int x = c.x1; x <= c.x2; x++)
            for (int z = c.z1; z <= c.z2; z++)
                if (!L.lane(x, z)) { L.clear.put(k(x, z), 0); q.add(new int[]{x, z}); }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int cv = L.clear.get(k(p[0], p[1]));
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    int nx = p[0] + dx, nz = p[1] + dz;
                    if (nx < c.x1 || nx > c.x2 || nz < c.z1 || nz > c.z2 || L.clear.containsKey(k(nx, nz))) continue;
                    L.clear.put(k(nx, nz), cv + 1);
                    q.add(new int[]{nx, nz});
                }
        }
        return L;
    }

    /** Boat move a -> b (8-neighbour): same level or one down; diagonals need both side cells passable for the boat. */
    static boolean move(Lanes L, int x, int z, int nx, int nz) {
        if (!L.lane(nx, nz)) return false;
        int y = L.y(x, z), ny = L.y(nx, nz);
        if (ny > y || ny < y - 1) return false;
        if (nx != x && nz != z) {
            int a = L.y(nx, z), b = L.y(x, nz);
            if (a == Integer.MIN_VALUE || b == Integer.MIN_VALUE || a > y || b > y) return false;
        }
        return true;
    }

    static void run(G g, Check.Report r) {
        TgMapData d = g.d;
        FrozenCanal map = new FrozenCanal();
        map.generate(d.seed);
        CanalCourse c = map.course;
        TgMapData.Blocks go = Check.afterGo(d, d.blocks);
        TgMapData v = Check.view(d, go);
        features(d, c, r);

        // ---- boat spawns
        int okSp = 0;
        double minPair = 99;
        for (Spawn s : d.boatSpawns) {
            int x = (int) Math.floor(s.x()), z = (int) Math.floor(s.z());
            String f = d.blocks.get(x, 0, z);
            if (f != null && Bs.id(f).contains("ice") && Bs.passable(d.blocks.get(x, 1, z)) && Bs.passable(d.blocks.get(x, 2, z)) && d.startStrip.contains(s.x(), s.y(), s.z())) okSp++;
            for (Spawn t : d.boatSpawns) if (t != s) minPair = Math.min(minPair, Math.hypot(t.x() - s.x(), t.z() - s.z()));
        }
        r.check(d.boatSpawns.size() >= 40 && okSp == d.boatSpawns.size() && minPair >= 1.6,
                String.format("%d boat spawns (%d on ice inside the start lock with headroom), closest pair %.2f apart (boat is 1.375 wide)", d.boatSpawns.size(), okSp, minPair));

        Lanes L = lanes(go, c);
        if (System.getenv("CANALCOL") != null)
            for (String xz : System.getenv("CANALCOL").split(";")) {
                int cx = Integer.parseInt(xz.split(",")[0]), cz = Integer.parseInt(xz.split(",")[1]);
                int i = c.idx(cx, cz);
                StringBuilder sb = new StringBuilder(String.format("  dbg %d,%d kind %d path %d s %.1f d %.1f margin %.2f level %d minLevel %d:", cx, cz, c.kind[i], c.path[i], c.sArr[i], c.dArr[i], c.margin[i], c.level[i], c.minLevel[i]));
                for (int y = -8; y <= 5; y++) { String s = go.get(cx, y, cz); sb.append(" ").append(y).append("=").append(s == null ? "." : Bs.id(s)); }
                r.line(sb.toString());
            }
        r.line(String.format("  course: main (safe) centre line %.0f blocks, causeway %.0f (replaces %.0f of ring) -> fast centre line %.0f; ice y 0 -> %d; %d lane columns",
                c.main.length(), c.cut.length(), c.cutTo - c.cutFrom, c.main.length() - (c.cutTo - c.cutFrom) + c.cut.length(), c.dockLevel, L.lvl.size()));
        StringBuilder segs = new StringBuilder("  segments:");
        int n = 1;
        for (Map.Entry<String, double[]> e : c.segments.entrySet())
            segs.append(String.format(" %d %s %.0f;", n++, e.getKey(), e.getValue()[1] - e.getValue()[0]));
        r.line(segs.toString());

        // ---- continuity, traps
        List<int[]> startCells = new ArrayList<>(), dockCells = new ArrayList<>();
        for (int x = -21; x <= 20; x++) for (int z = 0; z <= 8; z++) if (L.lane(x, z)) startCells.add(new int[]{x, z});
        for (int x = FrozenCanal.STAGE.maxX() + 1; x <= FrozenCanal.STAGE.maxX() + 2; x++)
            for (int z = FrozenCanal.STAGE.minZ(); z <= FrozenCanal.STAGE.maxZ(); z++) if (L.lane(x, z)) dockCells.add(new int[]{x, z});
        Set<Long> fwd = new HashSet<>(), rev = new HashSet<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int[] s : startCells) { fwd.add(k(s[0], s[1])); q.add(s); }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nx = p[0] + dx, nz = p[1] + dz;
                if (fwd.contains(k(nx, nz)) || !move(L, p[0], p[1], nx, nz)) continue;
                fwd.add(k(nx, nz)); q.add(new int[]{nx, nz});
            }
        }
        for (int[] s : dockCells) { rev.add(k(s[0], s[1])); q.add(s); }
        while (!q.isEmpty()) {
            int[] p = q.poll();
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int nx = p[0] + dx, nz = p[1] + dz;
                if (rev.contains(k(nx, nz)) || !L.lane(nx, nz) || !move(L, nx, nz, p[0], p[1])) continue;
                rev.add(k(nx, nz)); q.add(new int[]{nx, nz});
            }
        }
        boolean dockReached = false;
        for (int[] s : dockCells) if (fwd.contains(k(s[0], s[1]))) dockReached = true;
        int traps = 0, unreached = 0;
        int[] trapAt = null, unreachedAt = null;
        for (long key : L.lvl.keySet()) {
            int x = (int) (key >> 32), z = (int) key;
            if (fwd.contains(key) && !rev.contains(key)) { traps++; if (trapAt == null) trapAt = new int[]{x, L.y(x, z), z}; }
            if (!fwd.contains(key)) { unreached++; if (unreachedAt == null) unreachedAt = new int[]{x, L.y(x, z), z}; }
        }
        r.check(dockReached, "boat continuity: start lock -> dock reachable downhill only (" + fwd.size() + " lane columns reachable)");
        r.check(traps == 0, "no trap pockets: every reachable lane cell can still reach the dock (" + traps + " trap cells" + (trapAt == null ? "" : ", e.g. " + trapAt[0] + "," + trapAt[1] + "," + trapAt[2]) + ")");
        r.check(unreached <= 20, "unreachable lane cells: " + unreached + (unreachedAt == null ? "" : " (e.g. " + unreachedAt[0] + "," + unreachedAt[1] + "," + unreachedAt[2] + ")"));

        // ---- walls, snags, drops, lips
        int open = 0, snag = 0, bigDrop = 0, lips = 0;
        String openAt = "", snagAt = "";
        for (long key : fwd) {
            int x = (int) (key >> 32), z = (int) key, y = L.y(x, z);
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                int nx = x + o[0], nz = z + o[1];
                if (L.lane(nx, nz)) {
                    int ny = L.y(nx, nz);
                    if (y - ny > 1) bigDrop++;
                    if (ny == y + 1) lips++;
                    continue;
                }
                if (Bs.passable(go.get(nx, y + 1, nz))) { open++; if (openAt.isEmpty()) openAt = " e.g. " + nx + "," + (y + 1) + "," + nz + " beside " + x + "," + y + "," + z; }
            }
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                String s = go.get(x + dx, y + 1, z + dz);
                if (s != null && !Bs.passable(s) && !Bs.fullSolid(s)) { snag++; if (snagAt.isEmpty()) snagAt = " e.g. " + Bs.id(s) + " at " + (x + dx) + "," + (y + 1) + "," + (z + dz); }
            }
        }
        r.check(open == 0, "walls: every reachable lane edge is a full block at boat height (" + open + " open edges" + openAt + ")");
        r.check(snag == 0, "no snags: no fence / slab / stair / lantern / wall post at boat height next to the lane (" + snag + snagAt + ")");
        r.check(bigDrop == 0, "elevation: every drop is <= 1 block (" + bigDrop + " bigger), downhill only; " + lips + " up-lips (step faces seen from below and the causeway's open edges - boats can't climb them, by design)");

        // ---- widths along the main centre line
        double total = 0, wide = 0;
        int minW = 99;
        double minWAt = 0;
        for (double s = 2; s < c.main.length() - 2; s += 2) {
            double[] p = c.main.at(s);
            int w = width(L, c, p);
            total += 2;
            if (w >= 11) wide += 2;
            if (w < minW) { minW = w; minWAt = s; }
        }
        double[] pc = c.cut.at(c.cut.length() / 2);
        int cutW = 0;
        for (int dd = -4; dd <= 4; dd++) {
            double[] q2 = c.cut.point(c.cut.length() / 2, dd);
            int xx = (int) Math.floor(q2[0]), zz = (int) Math.floor(q2[1]);
            if (L.lane(xx, zz) && Bs.id(go.get(xx, L.y(xx, zz), zz)).equals("blue_ice")) cutW++;
        }
        r.check(wide / total >= 0.7 && minW >= 5, String.format("lane width: %.0f%% of the main line is >= 11 wide (BDC rule: >= 70%%), narrowest %d at s %.0f; causeway %d wide (blue ice, open edges)",
                100 * wide / total, minW, minWAt, cutW));
        // bridges
        StringBuilder bh = new StringBuilder();
        boolean low = false;
        for (int[] b : map.bridges) { bh.append(String.format(" s%.0f:%d", b[0] / 10.0, b[1])); if (b[1] < 2) low = true; }
        r.check(!low && !map.bridges.isEmpty(), map.bridges.size() + " bridges, headroom over the lane centre (blocks):" + bh + " (2 = low footbridge: boat fits, a standing player can't hop up)");

        // ---- on foot: nobody climbs out of the canal before the finish; finish routes
        Walk w = new Walk(v, go);
        int[] esc = escape(w, go, d);
        r.check(esc[0] == 0, "on foot: no way out of the canal onto the town before the finish (" + esc[0] + " reachable town cells outside the finish zone"
                + (esc[0] > 0 ? ", e.g. " + esc[1] + "," + esc[2] + "," + esc[3] : "") + "; jump model: gap <= 4 level, <= 3 with +1)");
        List<int[]> stage = new ArrayList<>();
        for (int x = FrozenCanal.STAGE.minX() + 3; x <= FrozenCanal.STAGE.maxX(); x++)
            for (int z = FrozenCanal.STAGE.minZ() + 2; z <= FrozenCanal.STAGE.maxZ() - 2; z += 3) stage.add(new int[]{x, FrozenCanal.STAGE.minY() + 1, z});
        Walk.Result fastFoot = w.route(stage, List.of(d.pen));
        Walk ws = new Walk(v, go);
        Region stairs = Region.of(87, -6, 26, 95, 8, 30);
        ws.allow = p -> !FrozenCanal.JETTY.contains(p[0], FrozenCanal.T, p[2]) && !stairs.contains(p[0] + 0.5, p[1] + 0.1, p[2] + 0.5);
        Walk.Result safeFoot = ws.route(stage, List.of(Region.of(97, FrozenCanal.T + 1, 60, 112, FrozenCanal.T + 1, 72), d.pen));
        double ff = foot(r, "FOOT FAST  stage -> Dock Stairs (3 wide) -> jetty (3 wide) -> pen", fastFoot);
        double fs = foot(r, "FOOT SAFE  stage -> side flight (4 wide) -> quay -> east bridge (5 wide) -> pen", safeFoot);
        // knocked off the jetty: back up from the mere ice
        Walk wm = new Walk(v, go);
        Walk.Result back = wm.route(List.of(new int[]{85, FrozenCanal.MERE + 1, 50}), List.of(d.pen));
        r.check(back.ok(), back.ok() ? String.format("knocked off the jetty onto the mere ice: back to the pen via the skaters' steps in %.1f s (expert pace)", back.end.cost)
                : "knocked off the jetty: NO WAY BACK UP");

        // ---- timed lines
        List<int[]> from = startCells;
        Predicate<int[]> safeAllow = p -> {
            int i = c.idx(p[0], p[1]);
            return i < 0 || (c.path[i] != CanalCourse.CUT && c.kind[i] != CanalCourse.FLAT);
        };
        Line safe = route(L, go, from, dockCells, 3, safeAllow);
        if (safe == null) {
            // where does a 5-wide boat line get stuck?
            Set<Long> seen = new HashSet<>();
            ArrayDeque<int[]> qq = new ArrayDeque<>();
            for (int[] s : from) if (L.clearance(s[0], s[1]) >= 3) { seen.add(k(s[0], s[1])); qq.add(s); }
            double far = 0; int[] farAt = null;
            while (!qq.isEmpty()) {
                int[] p = qq.poll();
                int i = c.idx(p[0], p[1]);
                if (i >= 0 && c.path[i] == CanalCourse.MAIN && c.sArr[i] > far) { far = c.sArr[i]; farAt = p; }
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                    int nx = p[0] + dx, nz = p[1] + dz;
                    if (seen.contains(k(nx, nz)) || !move(L, p[0], p[1], nx, nz) || L.clearance(nx, nz) < 3 || !safeAllow.test(new int[]{nx, nz})) continue;
                    seen.add(k(nx, nz)); qq.add(new int[]{nx, nz});
                }
            }
            r.line(String.format("  dbg: a 5-wide line gets as far as s %.0f at %d,%d", far, farAt == null ? 0 : farAt[0], farAt == null ? 0 : farAt[1]));
        }
        Line fast = route(L, go, from, dockCells, 2, p -> true);
        double ts = report(r, "SAFE  boats: middle lines, the walled Fish Market ring, wide barge gaps", safe);
        double tf = report(r, "FAST  boats: inside kerbs, the raised causeway, blue stripe, 3-wide barge gaps", fast);
        if (ts > 0 && tf > 0 && ff > 0 && fs > 0) {
            double dis = 1.0;
            double expertFast = tf + dis + ff, expertSafe = ts + dis + fs;
            r.check(expertFast <= expertSafe * 0.9, String.format("expert totals (boat + 1 s dismount + on foot): FAST %.0f s, SAFE %.0f s (fast saves %.0f%%)",
                    expertFast, expertSafe, 100 * (1 - expertFast / expertSafe)));
            r.line(String.format("  time profile [D]: expert %.0f-%.0f s; median (safe line, ~1.5x boat time for wall hits / pile-ups / bad hairpins, + 8 s stairs+jetty crowd) ~%.0f s;"
                    + " weak players who wall-slam every hairpin or fall off the causeway or jetty repeatedly are cut off by the time skip (half finished -> 20 s left; round 300 s)", expertFast, expertSafe, ts * 1.5 + dis + fs + 8));
        }
    }

    static int width(Lanes L, CanalCourse c, double[] p) {
        double rr = Math.toRadians(p[2]);
        int w = 0;
        for (int dir : new int[]{-1, 1})
            for (double dd = dir > 0 ? 0.5 : -0.5; Math.abs(dd) < 14; dd += dir * 0.5) {
                int x = (int) Math.floor(p[0] + Math.cos(rr) * dd), z = (int) Math.floor(p[1] - Math.sin(rr) * dd);
                int kd = c.kindAt(x, z);
                if (kd != CanalCourse.LANE) break;          // quay wall to quay wall (piers / barges don't count)
                w++;
            }
        return (int) Math.round(w / 2.0);
    }

    // ------------------------------------------------------------------ on foot

    /** Flood on foot from the start; returns {count, x, y, z} of reachable town-level feet cells outside the finish zone. */
    static int[] escape(Walk w, TgMapData.Blocks b, TgMapData d) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (Spawn s : d.startSpawns) {
            int x = (int) Math.floor(s.x()), y = (int) Math.floor(s.y()), z = (int) Math.floor(s.z());
            if (w.standable(x, y, z) && seen.add(TgMapData.SparseBlocks.key(x, y, z))) q.add(new int[]{x, y, z});
        }
        int bad = 0;
        int[] at = null;
        int top = FrozenCanal.T + 1;
        while (!q.isEmpty()) {
            int[] p = q.poll();
            if (p[1] >= top) {
                // town level: fine (and terminal) inside the finish zone, an escape anywhere else
                if (!FrozenCanal.FINISH_ZONE.contains(p[0], 0, p[2])) { bad++; if (at == null) at = p; }
                continue;
            }
            for (int dx = -4; dx <= 4; dx++)
                for (int dz = -4; dz <= 4; dz++) {
                    double hd = Math.hypot(dx, dz);
                    if (hd == 0 || hd > 4.6) continue;
                    boolean jump = Math.max(Math.abs(dx), Math.abs(dz)) > 1;
                    int nx = p[0] + dx, nz = p[2] + dz;
                    for (int ny = p[1] + 1; ny >= p[1] - 8; ny--) {
                        if (ny > p[1] && (hd > 3.3 || !Bs.passable(b.get(p[0], p[1] + 2, p[2])))) continue;   // a jump up needs room to jump
                        if (!w.standable(nx, ny, nz)) continue;
                        if (jump && !lineClear(b, p, nx, ny, nz)) break;
                        if (seen.add(TgMapData.SparseBlocks.key(nx, ny, nz))) q.add(new int[]{nx, ny, nz});
                        break;
                    }
                }
        }
        return at == null ? new int[]{0, 0, 0, 0} : new int[]{bad, at[0], at[1], at[2]};
    }

    /** The arc of a jump: cells between take-off and landing are free at the higher feet level and one above. */
    static boolean lineClear(TgMapData.Blocks b, int[] a, int bx, int by, int bz) {
        int y0 = Math.max(a[1], by);
        int steps = 8;
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            int x = (int) Math.floor(a[0] + 0.5 + (bx - a[0]) * t), z = (int) Math.floor(a[2] + 0.5 + (bz - a[2]) * t);
            if (!Bs.passable(b.get(x, y0, z)) || !Bs.passable(b.get(x, y0 + 1, z))) return false;
        }
        return true;
    }

    static double foot(Check.Report r, String name, Walk.Result res) {
        if (!res.ok()) { r.err(name + ": NO ROUTE"); return -1; }
        Walk.Node e = res.end;
        r.ok(String.format("%s: %.0f blocks on foot, %d jumps, est. %.1f s (expert pace)", name, e.len, e.jumps, e.cost));
        return e.cost;
    }

    // ------------------------------------------------------------------ boat lines

    record Line(List<int[]> cells, double length, double time, int slush, int blue, int minW, int drops, int maxGap, String gapAt) {}

    static Line route(Lanes L, TgMapData.Blocks b, List<int[]> from, List<int[]> to, int minClear, Predicate<int[]> allow) {
        Set<Long> goal = new HashSet<>();
        for (int[] t : to) goal.add(k(t[0], t[1]));
        Map<Long, Double> best = new HashMap<>();
        Map<Long, long[]> prev = new HashMap<>();
        PriorityQueue<double[]> pq = new PriorityQueue<>((a, c) -> Double.compare(a[0], c[0]));
        for (int[] s : from) if (L.clearance(s[0], s[1]) >= minClear) { best.put(k(s[0], s[1]), 0.0); pq.add(new double[]{0, s[0], s[1]}); }
        long end = Long.MIN_VALUE;
        while (!pq.isEmpty()) {
            double[] cur = pq.poll();
            int x = (int) cur[1], z = (int) cur[2];
            if (cur[0] > best.getOrDefault(k(x, z), Double.MAX_VALUE)) continue;
            if (goal.contains(k(x, z))) { end = k(x, z); break; }
            for (int dx = -1; dx <= 1; dx++)
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    int nx = x + dx, nz = z + dz;
                    if (!move(L, x, z, nx, nz) || L.clearance(nx, nz) < minClear && !goal.contains(k(nx, nz)) || !allow.test(new int[]{nx, nz})) continue;
                    double f = L.f(nx, nz);
                    double vv = f >= 0.989 ? 60 : f >= 0.98 ? 35 : 1.5;
                    double nc = cur[0] + Math.hypot(dx, dz) / vv + 0.002 * (4 - Math.min(4, L.clearance(nx, nz)));
                    if (nc < best.getOrDefault(k(nx, nz), Double.MAX_VALUE)) {
                        best.put(k(nx, nz), nc);
                        prev.put(k(nx, nz), new long[]{x, z});
                        pq.add(new double[]{nc, nx, nz});
                    }
                }
        }
        if (end == Long.MIN_VALUE) return null;
        List<int[]> cells = new ArrayList<>();
        int cx = (int) (end >> 32), cz = (int) end;
        while (true) {
            cells.add(new int[]{cx, cz});
            long[] p = prev.get(k(cx, cz));
            if (p == null) break;
            cx = (int) p[0]; cz = (int) p[1];
        }
        Collections.reverse(cells);
        return analyse(L, b, cells);
    }

    static Line analyse(Lanes L, TgMapData.Blocks b, List<int[]> cells) {
        int n = cells.size();
        double[] seg = new double[n];
        double len = 0;
        for (int i = 1; i < n; i++) { seg[i] = Math.hypot(cells.get(i)[0] - cells.get(i - 1)[0], cells.get(i)[1] - cells.get(i - 1)[1]); len += seg[i]; }
        double[] turn = new double[n];
        for (int i = 4; i < n - 4; i++) {
            double h1 = Math.atan2(cells.get(i)[0] - cells.get(i - 4)[0], cells.get(i)[1] - cells.get(i - 4)[1]);
            double h2 = Math.atan2(cells.get(i + 4)[0] - cells.get(i)[0], cells.get(i + 4)[1] - cells.get(i)[1]);
            turn[i] = Math.toDegrees(Math.abs(Math.atan2(Math.sin(h2 - h1), Math.cos(h2 - h1))));
        }
        int slush = 0, blue = 0, minW = 99, drops = 0, maxGap = 0;
        String gapAt = "";
        double since = 0;
        double[] vmax = new double[n];
        for (int i = 0; i < n; i++) {
            int[] c = cells.get(i);
            double f = L.f(c[0], c[1]);
            if (f < 0.9) slush++;
            if (f > 0.985) blue++;
            minW = Math.min(minW, 2 * L.clearance(c[0], c[1]) - 1);
            if (i > 0 && L.y(c[0], c[1]) < L.y(cells.get(i - 1)[0], cells.get(i - 1)[1])) drops++;
            vmax[i] = turn[i] > 60 ? 0.55 : turn[i] > 35 ? 0.8 : turn[i] > 20 ? 1.2 : 9;
            since += seg[i];
            boolean feat = turn[i] > 20 || L.clearance(c[0], c[1]) <= 2 || f < 0.9 || (i > 0 && L.y(c[0], c[1]) != L.y(cells.get(i - 1)[0], cells.get(i - 1)[1]));
            int y = L.y(c[0], c[1]);
            for (int dx = -2; dx <= 2 && !feat; dx++)
                for (int dz = -2; dz <= 2 && !feat; dz++)
                    if (!Bs.passable(b.get(c[0] + dx, y + 1, c[1] + dz)) && L.lane(c[0] + dx, c[1] + dz) || !Bs.passable(b.get(c[0] + dx, y + 3, c[1] + dz)) && L.lane(c[0] + dx, c[1] + dz)) feat = true;
            if (feat) {
                if (since > maxGap) { maxGap = (int) Math.round(since); gapAt = "ends at " + c[0] + "," + c[1]; }
                since = 0;
            }
        }
        double v = 0, t = 0;
        for (int i = 1; i < n; i++) {
            int[] c = cells.get(i);
            double f = L.f(c[0], c[1]);
            double limit = 9, dist = 0;
            for (int j = i; j < n && dist < 30; j++) { dist += seg[j]; limit = Math.min(limit, vmax[j] + dist * 0.035); }
            double remain = seg[i];
            while (remain > 1e-6) {
                v = v * f + 0.04;
                if (f < 0.9) v = Math.min(v, 0.075);
                if (v > limit) v = Math.max(limit, v * 0.9);
                double st = Math.min(remain, v);
                remain -= st;
                t += st / Math.max(v, 1e-3) / 20.0;
            }
        }
        return new Line(cells, len, t, slush, blue, minW, drops, maxGap, gapAt);
    }

    static double report(Check.Report r, String name, Line l) {
        if (l == null) { r.err(name + ": NO BOAT LANE"); return -1; }
        r.ok(String.format("%s: %.0f blocks, %d drops, %d slush / %d blue-ice cells, narrowest %d, longest featureless stretch %d (%s), est. %.1f s (avg %.0f b/s)",
                name, l.length(), l.drops(), l.slush(), l.blue(), l.minW(), l.maxGap(), l.gapAt(), l.time(), l.length() / l.time()));
        return l.time();
    }

    // ------------------------------------------------------------------ interactive features (playtest 2)

    /**
     * Pendulum: lane cells at boat height are tested at every phase with the runtime hit test (player in a boat:
     * sphere r 0.4 at feet + 0.5); the packed-ice edges (|d| >= 4.9) must be clear at every phase, the middle hit at
     * some phase, and no display box may cross a block. Toggles: every cell is a lane cell of the right path; the
     * lock gate's open frame leaves the causeway route as the generator built it. Checkpoints / depots: spawn on ice
     * with headroom. Bonus regions: on lane cells.
     */
    static void features(TgMapData d, CanalCourse c, Check.Report r) {
        com.mcm.games.race.model.MapFeatures f = FrozenCanal.feats();
        StringBuilder sb = new StringBuilder();
        int bad = 0;
        for (var m : f.movers) {
            int edgeHit = 0, midHit = 0, edges = 0, mids = 0, blocked = 0;
            int R = (int) Math.ceil(m.reach()) + 2;
            for (int x = (int) m.px() - R; x <= (int) m.px() + R; x++)
                for (int z = (int) m.pz() - R; z <= (int) m.pz() + R; z++) {
                    int i = c.idx(x, z);
                    if (i < 0 || c.kind[i] != CanalCourse.LANE) continue;
                    double dd = Math.abs(c.dArr[i]);
                    boolean edge = dd >= 4.9, mid = dd <= 2.0;
                    if (!edge && !mid) continue;
                    boolean hit = false;
                    for (int t = 0; t < m.periodTicks() && !hit; t++) hit = m.hits(t, x + 0.5, c.level[i] + 1.5, z + 0.5, 0.4);
                    if (edge) { edges++; if (hit) edgeHit++; }
                    if (mid) { mids++; if (hit) midHit++; }
                }
            for (int t = 0; t < m.periodTicks(); t += 2)
                for (var p : m.cells(t)) if (d.blocks.get(p.x(), p.y(), p.z()) != null) blocked++;
            boolean ok = edgeHit == 0 && edges > 0 && midHit > 0 && blocked == 0;
            if (!ok) bad++;
            sb.append(String.format("%n        %s: %d edge lane cells (|d| >= 4.9) hit at no phase (%d hit), %d/%d middle cells swept, %d display cells inside blocks over a period",
                    m.name(), edges, edgeHit, midHit, mids, blocked));
        }
        for (var t : f.toggles) {
            int lane = 0, all = 0;
            for (var fr : t.frames())
                for (var p : fr.blocks()) {
                    all++;
                    int i = c.idx(p.x(), p.z());
                    if (i >= 0 && c.kind[i] == CanalCourse.LANE) lane++;
                }
            int f0 = 0;
            for (var p : t.frames().get(0).blocks()) if (java.util.Objects.equals(norm(d.blocks.get(p.x(), p.y(), p.z())), norm(p.block()))) f0++;
            boolean ok = lane == all && f0 == t.frames().get(0).blocks().size() && !t.indicators().isEmpty();
            if (!ok) bad++;
            sb.append(String.format("%n        %s: %d frames, period %.1f s, %d cells (%d on the lane), frame 0 = the built map (%d/%d), %d indicator blocks warn %.1f s before",
                    t.name(), t.frames().size(), t.period() / 20.0, t.frames().get(0).blocks().size(), lane / Math.max(1, t.frames().size()), f0,
                    t.frames().get(0).blocks().size(), t.indicators().size(), t.warnTicks() / 20.0));
        }
        int spOk = 0, spAll = 0;
        List<Spawn> sps = new ArrayList<>();
        for (var cp : f.checkpoints) sps.addAll(cp.boats());
        for (var dp : f.depots) sps.add(dp.boat());
        for (Spawn s : sps) {
            spAll++;
            int x = (int) Math.floor(s.x()), y = (int) Math.floor(s.y()), z = (int) Math.floor(s.z());
            if (d.blocks.get(x, y, z) == null && d.blocks.get(x, y + 1, z) == null && d.blocks.get(x, y - 1, z) != null) spOk++;
        }
        if (spOk != spAll) bad++;
        int gOk = 0;
        for (var g : f.gates) { int i = c.idx(g.box().minX(), g.box().minZ()), j = c.idx(g.box().maxX(), g.box().maxZ()); if (i >= 0 && j >= 0 && (c.kind[i] == CanalCourse.LANE || c.kind[j] == CanalCourse.LANE)) gOk++; }
        if (gOk != f.gates.size()) bad++;
        r.check(bad == 0, String.format("features: %d mover, %d toggles, %d bonus regions on the lane (%d ok: hairpin kerbs orange, causeway gate red, blue stripe orange), %d checkpoints + %d depots (%d/%d boat spawns on ice with headroom)%s",
                f.movers.size(), f.toggles.size(), f.gates.size(), gOk, f.checkpoints.size(), f.depots.size(), spOk, spAll, sb));
    }

    private static String norm(String b) { return b == null ? null : b.startsWith("minecraft:") ? b.substring(10) : b; }
}
