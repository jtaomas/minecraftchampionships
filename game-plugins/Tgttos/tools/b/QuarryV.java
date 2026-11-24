import com.mcm.games.tgttos.maps.b.QuarrySteps;
import com.mcm.games.tgttos.maps.b.util.Bs;
import com.mcm.games.tgttos.maps.b.util.Check;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.maps.b.util.Walk;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Quarry Steps v2 validator: haul-road lines (cut / ramp), the straight bridge (expert / average, least blocks,
 * exposure), the mixed line, choke and ledge widths, decoy pens (reachable, away from the real pen), timing.
 * Walk model: sprint 5.6 b/s, flat gaps <= 4, ladders, bridging only inside buildBounds (blocks regenerate, so no cap).
 */
final class QuarryV {
    private QuarryV() {}

    static final int Y = QuarrySteps.RIM_FAR, B = QuarrySteps.BENCH;

    static List<int[]> starts(TgMapData d) {
        List<int[]> s = new ArrayList<>();
        for (Spawn p : d.startSpawns) s.add(new int[]{(int) Math.floor(p.x()), (int) Math.floor(p.y()), (int) Math.floor(p.z())});
        return s;
    }

    static Walk.Result line(Check.Report r, String name, Walk w, TgMapData d, List<Region> stages, Predicate<int[]> skip, int maxStraight) {
        Walk.Result res = w.route(starts(d), stages);
        if (!res.ok()) { r.err(name + ": NO ROUTE (" + res.visited + " cells searched)"); return res; }
        Walk.Node e = res.end;
        int[] st = w.longestStraight(res.path, skip);
        if (System.getenv("DEBUGPATH") != null) {
            StringBuilder sb = new StringBuilder("        path: ");
            String last = "";
            for (Walk.Node n : res.path) { String h = n.how.split(" ")[0]; if (!h.equals(last) || h.equals("jump")) sb.append(n.how).append("@").append(n.x).append(",").append(n.y).append(",").append(n.z).append(" "); last = h; }
            r.line(sb.toString());
        }
        r.check(st[0] <= maxStraight, String.format("%s: path %.0f blocks, %d jumps, hardest %s (difficulty %.1f), %d blocks placed, "
                        + "longest straight free stretch %d (at %d,%d,%d; limit %d), est. %.1f s (expert pace)",
                name, e.len, e.jumps, e.hardest, e.maxGap, e.blocks, st[0], st[1], st[2], st[3], maxStraight, e.cost));
        return res;
    }

    static void run(G g, List<int[]> decoys, Check.Report r) {
        TgMapData d = g.d;
        TgMapData.Blocks go = Check.afterGo(d, d.blocks);
        TgMapData v = Check.view(d, go);
        Predicate<int[]> skip = p -> Region.of(-27, -5, -1, 27, 10, 10).contains(p[0] + 0.5, p[1] + 0.1, p[2] + 0.5)
                || d.pen.contains(p[0] + 0.5, p[1] + 0.1, p[2] + 0.5);
        r.line("  route lines (walk: sprint 5.6 b/s, flat gap <= 4, -1 per block of rise; bridging inside buildBounds, blocks unlimited):");

        // ---- built lines
        Walk w = new Walk(v, go);
        Region benchC = Region.of(-27, -5, 43, -24, -5, 51);
        Region r2east = Region.of(23, B + 1, 58, 27, B + 1, 62);
        Region cutTop = Region.of(43, Y + 1, 59, 44, Y + 1, 61);
        Walk.Result cut = line(r, "SAFE  haul road + cut (choke 3 wide)", w, d, List.of(benchC, r2east, cutTop, d.pen), skip, 15);
        Region pad = Region.of(23, -1, 44, 27, -1, 49);
        Region plateau = Region.of(35, Y + 1, 44, 38, Y + 1, 48);
        Walk.Result ramp = line(r, "SAFE' haul road + wide ramp", w, d, List.of(benchC, r2east, pad, plateau, d.pen), skip, 15);
        r.check(cut.ok() && ramp.ok(), "the built route is completable without placing blocks (both exits)");

        // ---- straight bridge
        Walk wb = new Walk(v, go);
        wb.build = d.buildBounds;
        wb.maxBlocks = 999;
        wb.allow = p -> Math.abs(p[0]) <= 18 || p[2] < 11 || p[2] > 62;
        wb.bridgeRate = 4.0;
        wb.pillarTime = 0.35;
        Walk.Result fe = line(r, "FAST  bridge straight across (expert 4 b/s)", wb, d, List.of(d.pen), skip, 15);
        wb.bridgeRate = 2.2;
        wb.pillarTime = 0.55;
        Walk.Result fa = line(r, "FAST  same line, average bridger 2.2 b/s", wb, d, List.of(d.pen), skip, 15);
        // least blocks across the central corridor and how exposed that bridge is
        Walk wm = new Walk(v, go);
        wm.build = d.buildBounds;
        wm.maxBlocks = 999;
        wm.allow = p -> wb.allow.test(p) && (p[1] >= -1 || p[2] < 11);
        wm.blockWeight = 1000;
        Walk.Result least = wm.route(starts(d), List.of(Region.of(-27, Y + 1, 63, 27, Y + 2, 66)));
        int need = least.ok() ? least.end.blocks : -1;
        r.check(need >= 50, "straight-line bridging need (corridor |x| <= 18, least-blocks search): " + need + " blocks (>= 50)");
        if (least.ok()) {
            int overWater = 0, run = 0, longest = 0, cover = 0, placed = 0;
            for (Walk.Node n : least.path) {
                boolean pl = n.how.startsWith("bridge") || n.how.startsWith("pillar");
                if (!pl) { run = 0; continue; }
                placed++;
                run++;
                longest = Math.max(longest, run);
                boolean open = true;
                for (int y = n.y - 2; y > QuarrySteps.WATER; y--) if (!Bs.passable(go.get(n.x, y, n.z))) { open = false; break; }
                if (open) overWater++;
                // cover: any solid block within 1 sideways at body height
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                    if ((dx != 0 || dz != 0) && !Bs.passable(go.get(n.x + dx, n.y, n.z + dz))) { cover++; dx = 2; break; }
            }
            r.check(overWater >= placed * 0.8 && longest >= 25, String.format("bridge exposure: %d of %d placed blocks hang over open water (nothing below down to the floor), "
                    + "longest unbroken placed run %d, %d blocks with any cover beside them (sheet piles)", overWater, placed, longest, cover));
        }
        // mixed: bridge to the ramp pad, then the ramp
        Walk wx = new Walk(v, go);
        wx.build = d.buildBounds;
        wx.maxBlocks = 999;
        Walk.Result mixed = line(r, "MIXED bridge to the ramp pad, ramp E up (avg bridger)", wx, d, List.of(pad, plateau, d.pen), skip, 15);

        // ---- widths
        int choke = 99;
        for (int x = 29; x <= 42; x++) {
            int sy = B + (x - 27);
            int wd = 0;
            for (int z = 54; z <= 66; z++) if (Bs.passable(go.get(x, sy + 1, z)) && Bs.passable(go.get(x, sy + 2, z)) && !Bs.passable(go.get(x, sy, z))) wd++;
            choke = Math.min(choke, wd);
        }
        r.check(choke == 3, "the cut is a 3-wide choke over its whole length (narrowest " + choke + ")");
        int ledge = 99;
        for (int x = 8; x <= 16; x++) {
            int wd = 0;
            for (int z = 55; z <= 63; z++) if (Bs.standTop(go.get(x, B, z)) > 0.99 && Bs.passable(go.get(x, B + 1, z))) wd++;
            ledge = Math.min(ledge, wd);
        }
        r.check(ledge >= 1 && ledge <= 2, "narrow far-wall ledge x 8..16 is 2 wide with 1-wide pinches (narrowest " + ledge + "): one snowball and you are swimming");

        // ---- decoys: reachable on foot from the start (bridge allowed), in or near a fake pen, away from the real pen
        Walk wd = new Walk(v, go);
        wd.build = d.buildBounds;
        wd.maxBlocks = 999;
        int reach = 0, far = 0;
        double minDist = 1e9;
        for (int[] p : decoys) {
            double dx = Math.max(0, Math.max(d.pen.minX() - p[0], p[0] - d.pen.maxX())), dz = Math.max(0, Math.max(d.pen.minZ() - p[2], p[2] - d.pen.maxZ()));
            double dist = Math.hypot(dx, dz);
            minDist = Math.min(minDist, dist);
            if (dist >= 12) far++;
            Walk.Result res = wd.route(starts(d), List.of(Region.of(p[0] - 1, p[1], p[2] - 1, p[0] + 1, p[1], p[2] + 1)));
            if (res.ok()) reach++;
        }
        r.check(decoys.size() >= 6 && decoys.size() <= 10 && reach == decoys.size() && far == decoys.size(),
                String.format("%d decoys: %d reachable on foot, all >= 12 from the real pen (nearest %.0f); %d fake pens + scaffold deck",
                        decoys.size(), reach, minDist, QuarrySteps.PENS.length - 1));

        // ---- timing
        double safe = cut.ok() ? cut.end.cost : -1, wide = ramp.ok() ? ramp.end.cost : -1;
        double fast = fe.ok() ? fe.end.cost : -1, avgB = fa.ok() ? fa.end.cost : -1;
        double safeBest = Math.min(safe < 0 ? 1e9 : safe, wide < 0 ? 1e9 : wide);
        if (safe > 0 && fast > 0)
            r.check(fast <= safeBest * 0.8, String.format("expert bridge saves %.0f%% vs the best haul-road line (>= 20%%)", 100 * (1 - fast / safeBest)));
        r.line(String.format("  time profile: expert bridge %.0f s, expert road %.0f s (cut) / %.0f s (ramp), mixed %.0f s; "
                        + "median players (x1.6 + crowd at the choke / pen search) road ~%.0f s, bridge ~%.0f s",
                fast, safe, wide, mixed.ok() ? mixed.end.cost : -1, safeBest * 1.6 + 8, avgB * 1.35 + 6));
    }

    /** Blocks view with a marker block at every decoy (preview only). */
    static TgMapData.Blocks withDecoys(TgMapData d, List<int[]> decoys) {
        Check.Overlay o = new Check.Overlay(d.blocks);
        for (int[] p : decoys) o.set(p[0], p[1], p[2], "minecraft:white_wool");
        return o;
    }
}
