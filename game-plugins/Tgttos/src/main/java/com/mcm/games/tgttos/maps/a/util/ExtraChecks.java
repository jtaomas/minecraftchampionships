package com.mcm.games.tgttos.maps.a.util;

import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Map-specific offline checks added after playtest 1 (only run for maps that opt in, so the other builder-A maps'
 * reports are unchanged):
 * <ul>
 *   <li>Lethal-fall maps ({@link TgMap#lethalFallHeight()} > 0): every lethal-height drop on each line lands in water
 *       at least 2 deep, the dry surfaces around each landing pool are a lethal fall from the take-off (a miss
 *       fails), ordinary moves stay below the limit, all water is contained, and nobody can descend to the bottom
 *       without a water landing (dry-descent search). Decoy chickens stand on solid ground inside the play box.</li>
 *   <li>Void maps ({@link Design#voidReport}): count and widths of the void crossings per line, share of the walked
 *       path with a void edge beside it, and the size of each designated brawl spot and its void edge.</li>
 * </ul>
 */
final class ExtraChecks {
    private ExtraChecks() {}

    static void run(Validate v, TgMap map, Gen g, List<int[]> strip, List<Pos> gone) {
        int lethal = map.lethalFallHeight();
        if (lethal > 0) lethal(v, map, g, strip, gone, lethal);
        if (g.design.voidReport) voids(v, g);
        decoys(v, map, g, gone);
        if (g.design.holeScan) holes(v, g, strip, gone);
    }

    // ------------------------------------------------------------------ unintended holes (walkable surfaces next to the void)

    private static void holes(Validate v, Gen g, List<int[]> strip, List<Pos> gone) {
        TgMapData d = g.d;
        Design de = g.design;
        Route.Params wp = Route.Params.expert(false);
        wp.searchBox = de.searchBox;
        wp.falls = true;
        Route r = new Route(d.blocks, gone, wp);
        Set<Long> reach = r.reachable(strip, 2_000_000);
        java.util.TreeMap<String, String> found = new java.util.TreeMap<>();
        int byDesign = 0;
        for (long k : reach) {
            int x = TgMapData.SparseBlocks.kx(k), y = TgMapData.SparseBlocks.ky(k), z = TgMapData.SparseBlocks.kz(k);
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {0, 0}}) {
                int nx = x + o[0], nz = z + o[1];
                if (!voidCol(d, nx, nz, y + 1)) continue;
                boolean ok = false;
                for (Region a : de.voidByDesign) if (a.contains(nx + 0.5, a.minY(), nz + 0.5)) ok = true;
                if (ok) { byDesign++; continue; }
                found.put(String.format("%d,%d", nx, nz), String.format("void column %d,%d (beside walkable %d,%d,%d)", nx, nz, x, y, z));
            }
        }
        v.ok(found.isEmpty(), String.format("hole scan: %d walkable cells (no bridging, falls allowed); %d unintended void columns beside them%s; %d edge cells next to void by design",
                reach.size(), found.size(), found.isEmpty() ? "" : ": " + String.join("; ", found.values()), byDesign));
    }

    // ------------------------------------------------------------------ lethal falls (drop maps)

    private static void lethal(Validate v, TgMap map, Gen g, List<int[]> strip, List<Pos> gone, int L) {
        TgMapData d = g.d;
        Design de = g.design;
        v.text.append("  -- lethal falls: limit ").append(L).append(" blocks (drop + 1.25 jump peak counts)\n");
        Route.Params rp = Route.Params.expert(false);
        rp.searchBox = de.searchBox;
        Route r = new Route(d.blocks, gone, rp);
        int minIntended = Integer.MAX_VALUE;
        double maxOrdinary = 0;
        boolean allWater = true, allDeep = true, allMiss = true;
        for (Design.Line line : de.lines) {
            Route.Result res = v.lineResults.get(line.name);
            if (res == null || !res.ok) continue;
            int k = 0;
            for (int i = 1; i < res.path.size(); i++) {
                Route.Step a = res.path.get(i - 1), s = res.path.get(i);
                double drop = a.h() - s.h();
                boolean air = s.how() == Route.How.JUMP || s.how() == Route.How.FALL;
                double eff = drop + (air ? 1.25 : 0);
                if (eff < L) { maxOrdinary = Math.max(maxOrdinary, eff); continue; }
                k++;
                boolean water = isWater(d, s.x(), s.y(), s.z());
                int depth = depth(d, s.x(), s.y(), s.z());
                // nearest dry surface around the landing: the fall onto it must also be lethal
                double worst = Double.MAX_VALUE;
                String at = "";
                for (int dx = -5; dx <= 5; dx++)
                    for (int dz = -5; dz <= 5; dz++) {
                        int x = s.x() + dx, z = s.z() + dz;
                        // first thing a faller meets in this column, from the take-off height down
                        for (int y = a.y() + 1; y >= s.y() - 6; y--) {
                            String b = d.blocks.get(x, y, z);
                            if (isWater(d, x, y, z)) break;
                            if (Bs.passable(b)) continue;
                            double top = y + Math.max(0, Bs.standTop(b));
                            if (top <= s.y() + 4) {
                                double f = a.h() - top;
                                if (f < worst) { worst = f; at = "(" + x + "," + y + "," + z + ")"; }
                            }
                            break;
                        }
                    }
                minIntended = Math.min(minIntended, (int) Math.floor(drop));
                allWater &= water;
                allDeep &= depth >= 2;
                allMiss &= worst >= L;
                v.info(String.format("%s drop %d: (%d,%d,%d) -> (%d,%d,%d) falls %.0f into %s, water depth %d, nearest dry surface %.0f below take-off %s",
                        line.name, k, a.x(), a.y(), a.z(), s.x(), s.y(), s.z(), drop, water ? "WATER" : "NOT WATER (" + Bs.id(d.blocks.get(s.x(), s.y() - 1, s.z())) + ")",
                        depth, worst == Double.MAX_VALUE ? -1 : worst, at));
            }
            v.ok(k >= 3, line.name + " has >= 3 lethal-height drop stages (" + k + ")");
        }
        v.ok(allWater, "every lethal-height drop on the lines lands in water");
        v.ok(allDeep, "every landing pool on the lines is at least 2 deep");
        v.ok(allMiss, "missing the water is a lethal fall (every dry surface within 5 of each landing is >= " + L + " below the take-off)");
        v.ok(maxOrdinary < L && minIntended >= L + 2, String.format("limit consistent: ordinary hops reach %.2f < %d, smallest intended drop %d >= %d",
                maxOrdinary, L, minIntended == Integer.MAX_VALUE ? -1 : minIntended, L + 2));

        // all water contained and at least 2 deep
        int[] loose = {0}, shallow = {0}, cells = {0};
        List<int[]> loosePos = new ArrayList<>();
        d.blocks.forEach((x, y, z, s) -> {
            if (!Bs.id(s).equals("water")) return;
            cells[0]++;
            int[][] nb = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, -1, 0}};
            for (int[] n : nb) if (Bs.isAir(d.blocks.get(x + n[0], y + n[1], z + n[2]))) { loose[0]++; if (loosePos.size() < 4) loosePos.add(new int[]{x, y, z}); break; }
            if (!isWater(d, x, y + 1, z) && Bs.isAir(d.blocks.get(x, y + 1, z)) && depth(d, x, y, z) < 2) shallow[0]++;
        });
        StringBuilder lp = new StringBuilder();
        for (int[] q : loosePos) lp.append(" (").append(q[0]).append(',').append(q[1]).append(',').append(q[2]).append(')');
        v.ok(loose[0] == 0, "all water contained (no air beside or below a water block): " + cells[0] + " water blocks, " + loose[0] + " loose" + lp);
        v.ok(shallow[0] == 0, "every pool surface is at least 2 deep (" + shallow[0] + " shallow columns)");

        // dry descent: from the start and from every pool, without any lethal-height drop
        Route.Params dp = Route.Params.expert(false);
        dp.searchBox = de.searchBox;
        dp.falls = true;
        dp.lethal = L;
        dp.dryOnly = true;
        Route dr = new Route(d.blocks, gone, dp);
        Set<Long> fromStart = dr.reachable(strip, 600000);
        double minStart = minH(dr, fromStart);
        double stripH = d.startStrip.minY();
        v.ok(minStart > stripH - L, String.format("dry descent from the start stops at h %.1f (start %d): no way down without a water landing", minStart, (int) stripH));
        boolean penDry = false;
        for (long k : fromStart) if (d.pen.contains(TgMapData.SparseBlocks.kx(k), TgMapData.SparseBlocks.ky(k), TgMapData.SparseBlocks.kz(k))) { penDry = true; break; }
        List<List<int[]>> pools = pools(d, r);
        int bad = 0;
        StringBuilder ps = new StringBuilder();
        double bottom = Double.MAX_VALUE;
        for (List<int[]> pool : pools) bottom = Math.min(bottom, pool.get(0)[1]);
        for (List<int[]> pool : pools) {
            int py = pool.get(0)[1];
            Set<Long> reach = dr.reachable(pool, 600000);
            double mh = minH(dr, reach);
            boolean okp = py <= bottom + 0.5 || mh > py - L;
            if (!okp) bad++;
            ps.append(String.format(" [%d cells @y%d (%d,%d): min %.0f]", pool.size(), py, pool.get(0)[0], pool.get(0)[2], mh));
        }
        v.ok(!penDry && bad == 0, "dry descent: pen not reachable dry from the start, and no pool above the bottom leads lower than " + L + " without water (" + pools.size() + " pools:" + ps + ")");

        // the next stage is hidden from the start: share of spawns that see a pool below the first stage
        double firstPool = Double.MIN_VALUE;
        for (List<int[]> pool : pools) firstPool = Math.max(firstPool, pool.get(0)[1]);
        int vis = 0;
        for (Spawn sp : d.startSpawns) {
            boolean seen = false;
            for (List<int[]> pool : pools) {
                if (pool.get(0)[1] >= firstPool - 0.5) continue;
                for (int[] c : pool) if (Validate.ray(d.blocks, sp.x(), sp.y() + 1.62, sp.z(), c[0] + 0.5, c[1] + 0.9, c[2] + 0.5)) { seen = true; break; }
                if (seen) break;
            }
            if (seen) vis++;
        }
        v.info(String.format("lower pools (below the first stage) visible from %d/%d start spawns", vis, d.startSpawns.size()));
    }

    private static boolean isWater(TgMapData d, int x, int y, int z) { return Bs.id(d.blocks.get(x, y, z)).equals("water") && d.blocks.get(x, y, z) != null; }

    /** Water blocks in the column from y down (y itself included). */
    private static int depth(TgMapData d, int x, int y, int z) {
        int n = 0;
        while (n < 10 && isWater(d, x, y - n, z)) n++;
        return n;
    }

    /** Surface water cells (feet node, open above), grouped into 4-connected pools at one level. */
    private static List<List<int[]>> pools(TgMapData d, Route r) {
        Set<Long> surf = new HashSet<>();
        d.blocks.forEach((x, y, z, s) -> {
            if (Bs.id(s).equals("water") && Bs.isAir(d.blocks.get(x, y + 1, z))) surf.add(Route.key(x, y, z));
        });
        Set<Long> seen = new HashSet<>();
        List<List<int[]>> out = new ArrayList<>();
        for (long k : surf) {
            if (!seen.add(k)) continue;
            List<int[]> pool = new ArrayList<>();
            java.util.ArrayDeque<Long> dq = new java.util.ArrayDeque<>();
            dq.add(k);
            while (!dq.isEmpty()) {
                long c = dq.poll();
                int x = TgMapData.SparseBlocks.kx(c), y = TgMapData.SparseBlocks.ky(c), z = TgMapData.SparseBlocks.kz(c);
                pool.add(new int[]{x, y, z});
                for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    long nk = Route.key(x + n[0], y, z + n[1]);
                    if (surf.contains(nk) && seen.add(nk)) dq.add(nk);
                }
            }
            out.add(pool);
        }
        out.sort((a, b) -> Integer.compare(b.get(0)[1], a.get(0)[1]));
        return out;
    }

    private static double minH(Route r, Set<Long> cells) {
        double m = Double.MAX_VALUE;
        for (long k : cells) m = Math.min(m, r.heightAt(TgMapData.SparseBlocks.kx(k), TgMapData.SparseBlocks.ky(k), TgMapData.SparseBlocks.kz(k)));
        return m;
    }

    // ------------------------------------------------------------------ void crossings (knock-off maps)

    /** Nothing to land on in this column from y down to the void line. */
    static boolean voidCol(TgMapData d, int x, int z, int y) {
        for (int yy = y; yy >= d.voidY; yy--) if (!Bs.isAir(d.blocks.get(x, yy, z))) return false;
        return true;
    }

    private static void voids(Validate v, Gen g) {
        TgMapData d = g.d;
        Design de = g.design;
        for (Design.Line line : de.lines) {
            Route.Result res = v.lineResults.get(line.name);
            if (res == null || !res.ok) continue;
            int crossings = 0, walk = 0, exposed = 0;
            double maxGap = 0;
            List<String> widths = new ArrayList<>();
            for (int i = 1; i < res.path.size(); i++) {
                Route.Step a = res.path.get(i - 1), s = res.path.get(i);
                if (s.how() == Route.How.JUMP || s.how() == Route.How.FALL) {
                    int low = Math.min(a.y(), s.y()) - 1;
                    boolean overVoid = false;
                    int n = 16;
                    for (int t = 1; t < n && !overVoid; t++) {
                        double f = t / (double) n;
                        int cx = (int) Math.floor(a.x() + 0.5 + (s.x() - a.x()) * f), cz = (int) Math.floor(a.z() + 0.5 + (s.z() - a.z()) * f);
                        if ((cx == a.x() && cz == a.z()) || (cx == s.x() && cz == s.z())) continue;
                        if (voidCol(d, cx, cz, low)) overVoid = true;
                    }
                    if (overVoid) {
                        crossings++;
                        maxGap = Math.max(maxGap, s.gap());
                        widths.add(String.format("%.1f%s", s.gap(), s.rise() > 0.01 ? "+" + Math.round(s.rise()) : s.rise() < -0.01 ? "-" + Math.round(-s.rise()) : ""));
                    }
                } else if (s.how() == Route.How.WALK) {
                    walk++;
                    boolean edge = false;
                    for (int dx = -1; dx <= 1 && !edge; dx++)
                        for (int dz = -1; dz <= 1 && !edge; dz++)
                            if ((dx != 0 || dz != 0) && voidCol(d, s.x() + dx, s.z() + dz, s.y() - 1)) edge = true;
                    if (edge) exposed++;
                }
            }
            v.ok(crossings >= de.minVoidCrossings, String.format("%s crosses open void %d times (>= %d), gaps %s, widest %.1f",
                    line.name, crossings, de.minVoidCrossings, widths, maxGap));
            v.info(String.format("%s: %d of %d walked cells (%.0f%%) have a void drop right beside them (knock-off exposure)",
                    line.name, exposed, walk, 100.0 * exposed / Math.max(1, walk)));
        }
        Route.Params rp = Route.Params.expert(false);
        rp.searchBox = de.searchBox;
        Route r = new Route(d.blocks, null, rp);
        int i = 0;
        for (Region b : de.brawl) {
            i++;
            int cells = 0, edge = 0;
            for (int x = b.minX(); x <= b.maxX(); x++)
                for (int z = b.minZ(); z <= b.maxZ(); z++)
                    for (int y = b.maxY(); y >= b.minY(); y--) {
                        if (!r.isStandable(x, y, z)) continue;
                        cells++;
                        boolean e = false;
                        for (int[] n : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}})
                            if (voidCol(d, x + n[0], z + n[1], y - 1)) e = true;
                        if (e) edge++;
                        break;
                    }
            v.ok(cells >= 80 && edge >= 10, String.format("brawl spot %d: %d x %d box, %d standable cells, %d on a void edge (>= 80 / >= 10)",
                    i, b.maxX() - b.minX() + 1, b.maxZ() - b.minZ() + 1, cells, edge));
        }
    }

    // ------------------------------------------------------------------ decoys

    private static void decoys(Validate v, TgMap map, Gen g, List<Pos> gone) {
        TgMapData d = g.d;
        int okc = 0, n = 0;
        List<int[]> spots = com.mcm.games.tgttos.model.DecoySpots.effective(map, d);
        for (int[] c : spots) {
            n++;
            boolean ok = d.playBounds.contains(c[0], c[1], c[2]) && Bs.passable(d.blocks.get(c[0], c[1], c[2]))
                    && Bs.standTop(d.blocks.get(c[0], c[1] - 1, c[2])) > 0 && !d.pen.contains(c[0], c[1], c[2]);
            if (ok) okc++;
        }
        v.ok(okc == n && n > 0, n + " wandering decoy chickens (" + (map.decoyChickens().isEmpty() ? "scattered over the later part by DecoySpots" : "map list")
                + ") on solid ground, inside playBounds, outside the goal pen (" + okc + "/" + n + ")");
    }
}
