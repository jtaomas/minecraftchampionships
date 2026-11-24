package com.mcm.games.tgttos.maps.a.util;

import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Offline validators for one generated map; writes a text report and counts errors. */
public final class Validate {
    public final StringBuilder text = new StringBuilder();
    public int errors;
    public final Map<String, Route.Result> lineResults = new HashMap<>();

    void ok(boolean pass, String what) {
        text.append(pass ? "  [ok]   " : "  [FAIL] ").append(what).append('\n');
        if (!pass) errors++;
    }

    void info(String s) { text.append("         ").append(s).append('\n'); }

    private static final Set<String> RESERVED = Set.of("white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "cyan", "purple", "blue", "green", "red");

    public static Validate run(TgMap map, Gen g) {
        Validate v = new Validate();
        v.all(map, g);
        return v;
    }

    private List<int[]> stripStarts(Route r, Region s) {
        List<int[]> out = new ArrayList<>();
        for (int x = s.minX(); x <= s.maxX(); x++)
            for (int y = s.minY(); y <= s.maxY(); y++)
                for (int z = s.minZ(); z <= s.maxZ(); z++) if (r.isStandable(x, y, z)) out.add(new int[]{x, y, z});
        return out;
    }

    private void all(TgMap map, Gen g) {
        TgMapData d = g.d;
        Design de = g.design;
        Kit kit = map.kit();
        lethal = map.lethalFallHeight();
        text.append("==== ").append(map.displayName()).append("  (").append(map.getClass().getName()).append(", ")
                .append(map.type()).append(", kit ").append(kit).append(") ====\n");

        // ---- contract + materials
        List<String> c = d.check(kit);
        ok(c.isEmpty(), "contract check " + (c.isEmpty() ? "" : c));
        int[] dyed = {0};
        Set<String> ids = new HashSet<>();
        int[] total = {0}, scenery = {0};
        d.blocks.forEach((x, y, z, s) -> {
            String id = Bs.id(s);
            ids.add(id);
            total[0]++;
            if (id.endsWith("_concrete") || id.endsWith("_concrete_powder")) {
                String col = id.replace("_concrete_powder", "").replace("_concrete", "");
                dyed[0]++;   // the contract forbids every *_concrete (also gray/black/brown), not only team colours
            }
            boolean inLane = false;
            for (Region r : de.lane) if (r.contains(x, y, z)) { inLane = true; break; }
            if (!inLane) scenery[0]++;
        });
        ok(dyed[0] == 0, "no concrete / concrete powder in the build (" + dyed[0] + ")");
        ok(ids.size() >= 25, "distinct block types >= 25 (" + ids.size() + ")");
        double share = 100.0 * scenery[0] / Math.max(1, total[0]);
        ok(share >= 40 && share <= 60.5, String.format("scenery share 40-60%% (%.0f%% of %d blocks outside the route envelope)", share, total[0]));

        // ---- post-pass and stairs
        int bad = PostPass.verify(d.blocks);
        ok(bad == 0, "connection post-pass holds (fences/panes/walls/bars: " + bad + " mismatches)");
        List<String> se = Stairs.validate(d.blocks, de.flights);
        ok(se.isEmpty(), "staircase helper: " + de.flights.size() + " flights walkable" + (se.isEmpty() ? "" : " " + se.subList(0, Math.min(5, se.size()))));

        // ---- start strip
        Route.Params pre = Route.Params.expert(false);
        pre.searchBox = de.searchBox;
        Route rp = new Route(d.blocks, null, pre);
        List<int[]> strip = stripStarts(rp, d.startStrip);
        int spawnsOk = 0;
        for (Spawn s : d.startSpawns) {
            int x = (int) Math.floor(s.x()), y = (int) Math.floor(s.y() + 0.01), z = (int) Math.floor(s.z());
            if (d.startStrip.contains(s.x(), s.y(), s.z()) && rp.isStandable(x, y, z)) spawnsOk++;
        }
        ok(strip.size() >= 40 && spawnsOk >= 40, "start strip holds 40 (" + strip.size() + " standable cells, " + spawnsOk + "/" + d.startSpawns.size() + " spawns valid)");
        Set<Long> pen0 = rp.reachable(strip, 200000);
        int out = 0;
        for (long k : pen0) {
            int x = TgMapData.SparseBlocks.kx(k), y = TgMapData.SparseBlocks.ky(k), z = TgMapData.SparseBlocks.kz(k);
            if (!d.startStrip.contains(x, y, z)) out++;
        }
        ok(out == 0, "start strip fenced in before GO (cells reachable outside: " + out + ")");
        ok(!d.startBarrier.isEmpty() && d.startBarrier.stream().allMatch(p -> d.blocks.get(p.x(), p.y(), p.z()) != null),
                "start barrier blocks present (" + d.startBarrier.size() + ")");

        // ---- landmark visibility
        int vis = 0;
        for (Spawn s : d.startSpawns) {
            double ex = s.x(), ey = s.y() + 1.62, ez = s.z();
            for (Pos l : de.landmark) if (ray(d.blocks, ex, ey, ez, l.x() + 0.5, l.y() + 0.5, l.z() + 0.5)) { vis++; break; }
        }
        double vf = 100.0 * vis / Math.max(1, d.startSpawns.size());
        ok(vf >= 75, String.format("landmark visible from the start (ray-cast): %d/%d spawns (%.0f%%)", vis, d.startSpawns.size(), vf));

        // ---- built route without placing blocks
        List<Pos> gone = new ArrayList<>(d.startBarrier);
        gone.addAll(d.dropFloor);
        Route.Params wp = Route.Params.expert(false);
        wp.searchBox = de.searchBox;
        wp.falls = true;
        wp.lethal = map.lethalFallHeight();
        Route walk = new Route(d.blocks, gone, wp);
        Route.Result wr = walk.search(strip, d.pen);
        ok(wr.ok, "built route start -> pen without placing blocks" + (wr.ok ? String.format(" (%.1f s expert, %d jumps, hardest %s)", wr.time, wr.jumps, wr.hardest()) : ""));

        // ---- lines
        Route.Result safeR = null, fastR = null, safeM = null, fastM = null;
        for (Design.Line line : de.lines) {
            Route.Result ex = runLine(d, de, line, strip, gone, false);
            Route.Result med = runLine(d, de, line, strip, gone, true);
            lineResults.put(line.name, ex);
            text.append("  -- line ").append(line.name).append(": ").append(line.description).append('\n');
            if (!ex.ok) { ok(false, line.name + " line completes (failed at leg " + ex.failedLeg + ")"); continue; }
            ok(true, line.name + " line completes");
            info(String.format("path %.0f blocks, blocks placed %d, jumps %d (3+ gaps: %d, falls %d), hardest jump %s, max rise %.1f",
                    ex.length, ex.blocks, ex.jumps, ex.gaps3plus, ex.falls, ex.hardest(), ex.maxRise));
            info(String.format("estimated time: expert %.1f s, median %.1f s (+ fails)", ex.time, med.ok ? med.time : -1));
            ok(ex.longestSprint <= 15.01, String.format("%s longest obstacle-free straight sprint %.1f <= 15 %s", line.name, ex.longestSprint, ex.sprintAt));
            ok(ex.blocks <= 64, line.name + " blocks within the 64 kit (" + ex.blocks + ")");
            if (line.name.startsWith("SAFE")) { safeR = ex; safeM = med; }
            if (line.name.startsWith("FAST")) { fastR = ex; fastM = med; }
        }
        if (safeR != null && fastR != null && safeR.ok && fastR.ok) {
            double save = 100 * (1 - fastR.time / safeR.time);
            double saveM = (safeM != null && fastM != null && safeM.ok && fastM.ok) ? 100 * (1 - fastM.time / safeM.time) : -1;
            ok(save >= 20, String.format("FAST saves >= 20%% vs SAFE (expert %.0f%%, median %.0f%%)", save, saveM));
            ok(fastR.time <= 35.5, String.format("best line by an expert <= 35 s (%.1f s)", fastR.time));
        }

        // ---- lazy straight line
        if (kit.bridging() && de.lazyCorridor != null) {
            Route.Params lp = Route.Params.expert(true);
            lp.searchBox = de.searchBox;
            lp.buildBox = d.buildBounds != null ? d.buildBounds : d.playBounds;
            lp.corridor = List.of(de.lazyCorridor, d.startStrip);
            lp.blockPenalty = 20;
            lp.noJumps = true;
            lp.maxBlocks = 400;
            Route lr = new Route(d.blocks, gone, lp);
            List<int[]> ls = new ArrayList<>();
            for (int[] s : strip) if (de.lazyCorridor.contains(s[0], s[1], s[2])) ls.add(s);
            Route.Result res = lr.search(ls, d.pen);
            ok(res.ok && res.blocks >= 40, "lazy straight line (walk + bridge, no jumps) needs >= 40 blocks (" + (res.ok ? res.blocks + " blocks, corridor x " + de.lazyCorridor.minX() + ".." + de.lazyCorridor.maxX() : "no path") + ")");
        }

        // ---- map-wide free sprint
        Route.Params sp = Route.Params.expert(false);
        sp.searchBox = de.searchBox;
        sp.falls = true;
        sp.lethal = map.lethalFallHeight();
        Route sr = new Route(d.blocks, gone, sp);
        Set<Long> reach = sr.reachable(strip, 600000);
        int[] best = sprint(sr, reach, d, de, true), bestX = sprint(sr, reach, d, de, false);
        ok(best[0] <= 15, "map-wide: longest straight flat run along the course (z) " + best[0] + " <= 15 at (" + best[1] + "," + best[2] + "," + best[3] + ")  [" + reach.size() + " reachable cells]");
        info("longest straight flat run across (x): " + bestX[0] + " at (" + bestX[1] + "," + bestX[2] + "," + bestX[3] + ")");
        boolean inPlay = true;
        for (Route.Result r : lineResults.values()) for (Route.Step s : r.path) if (!d.playBounds.contains(s.x(), s.y(), s.z())) inPlay = false;
        ok(inPlay && d.playBounds.contains(d.pen.minX(), d.pen.minY(), d.pen.minZ()), "lines and pen inside playBounds");
        ExtraChecks.run(this, map, g, strip, gone);
        text.append("  RESULT ").append(errors == 0 ? "PASS" : errors + " problem(s)").append('\n');
    }

    private int[] sprint(Route r, Set<Long> reach, TgMapData d, Design de, boolean alongZ) {
        int[] best = {0, 0, 0, 0};
        for (long k : reach) {
            int x = TgMapData.SparseBlocks.kx(k), y = TgMapData.SparseBlocks.ky(k), z = TgMapData.SparseBlocks.kz(k);
            int px = alongZ ? x : x - 1, pz = alongZ ? z - 1 : z;
            if (reach.contains(Route.key(px, y, pz)) && run1(r, d, de, px, y, pz) && Math.abs(r.heightAt(px, y, pz) - r.heightAt(x, y, z)) < 0.01) continue;
            if (!run1(r, d, de, x, y, z)) continue;
            double h = r.heightAt(x, y, z);
            int n = 1;
            while (true) {
                int nx = alongZ ? x : x + n, nz = alongZ ? z + n : z;
                if (!reach.contains(Route.key(nx, y, nz)) || !run1(r, d, de, nx, y, nz) || Math.abs(r.heightAt(nx, y, nz) - h) > 0.01) break;
                n++;
            }
            if (n - 1 > best[0]) { best[0] = n - 1; best[1] = x; best[2] = y; best[3] = z; }
        }
        return best;
    }

    private boolean run1(Route r, TgMapData d, Design de, int x, int y, int z) {
        if (!r.isStandable(x, y, z)) return false;
        if (d.startStrip.contains(x, y, z) || d.pen.contains(x, y, z)) return false;
        for (Region e : de.sprintExempt) if (e.contains(x, y, z)) return false;
        return true;
    }

    private int lethal;

    private Route.Result runLine(TgMapData d, Design de, Design.Line line, List<int[]> strip, List<Pos> gone, boolean median) {
        Route.Params p = median ? Route.Params.median(line.bridge) : Route.Params.expert(line.bridge);
        p.searchBox = de.searchBox;
        p.buildBox = d.buildBounds != null ? d.buildBounds : d.playBounds;
        p.falls = true;
        p.maxGap = line.maxGap;
        p.lethal = lethal;
        List<Route.Result> legs = new ArrayList<>();
        List<int[]> starts = strip;
        for (Design.Leg leg : line.legs) {
            List<Region> cor = new ArrayList<>(leg.corridor());
            cor.add(leg.target());
            if (starts == strip) cor.add(d.startStrip);
            p.corridor = cor;
            Route r = new Route(d.blocks, gone, p);
            Route.Result res = r.search(starts, leg.target());
            if (!res.ok) { Route.Result f = new Route.Result(); f.ok = false; f.failedLeg = leg.name(); return f; }
            legs.add(res);
            Route.Step e = res.path.get(res.path.size() - 1);
            starts = List.of(new int[]{e.x(), e.y(), e.z()});
        }
        return Route.join(legs, p);
    }

    /** Line of sight: every cell strictly between the points is see-through (not a full opaque cube). */
    public static boolean ray(TgMapData.Blocks b, double x1, double y1, double z1, double x2, double y2, double z2) {
        double len = Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1) + (z2 - z1) * (z2 - z1));
        int n = (int) Math.ceil(len * 4);
        int ex = (int) Math.floor(x2), ey = (int) Math.floor(y2), ez = (int) Math.floor(z2);
        for (int i = 1; i < n; i++) {
            double t = i / (double) n;
            int x = (int) Math.floor(x1 + (x2 - x1) * t), y = (int) Math.floor(y1 + (y2 - y1) * t), z = (int) Math.floor(z1 + (z2 - z1) * t);
            if (x == ex && y == ey && z == ez) return true;
            String s = b.get(x, y, z);
            if (s == null) continue;
            String id = Bs.id(s);
            if (id.contains("glass") || id.equals("water") || id.equals("barrier") || id.equals("light")) continue;
            if (Bs.fullSolid(s) || id.endsWith("_leaves")) return false;
        }
        return true;
    }
}
