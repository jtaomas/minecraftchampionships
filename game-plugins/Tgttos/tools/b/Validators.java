import com.mcm.games.tgttos.maps.b.util.Boat;
import com.mcm.games.tgttos.maps.b.util.Bs;
import com.mcm.games.tgttos.maps.b.util.Check;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.maps.b.util.Glide;
import com.mcm.games.tgttos.maps.b.util.Walk;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/** Map-specific validators for the B maps. */
final class Validators {
    private Validators() {}

    static final class Out {
        Check.Report rep = new Check.Report();
        double[] cam;
        Map<String, TgMapData.Blocks> extraDumps;
    }

    static Out run(String id, G g, long ms) {
        Out o = new Out();
        Check.Report r = o.rep;
        TgMapData d = g.d;
        Set<String> types = new HashSet<>();
        d.blocks.forEach((x, y, z, s) -> types.add(Bs.id(s)));
        Region bb = d.blockBounds();
        r.line("=== " + id + " ===");
        r.line(String.format("  %d blocks, %d block types, generated in %d ms, bounds x %d..%d y %d..%d z %d..%d, scenery share %.0f%%",
                d.blocks.size(), types.size(), ms, bb.minX(), bb.maxX(), bb.minY(), bb.maxY(), bb.minZ(), bb.maxZ(), g.sceneryShare() * 100));
        r.check(bb.minX() >= -250 && bb.maxX() <= 250 && bb.minZ() >= -250 && bb.maxZ() <= 250 && bb.minY() >= -60 && bb.maxY() <= 180, "inside the contract box (x/z -250..250, y -60..180)");
        r.check(types.size() >= 25, "material richness: " + types.size() + " block types (>= 25)");
        Kit kit = switch (id) {
            case "kite_festival" -> Kit.ELYTRA_FIREWORKS;
            case "frozen_canal" -> Kit.BOAT;
            case "ticket_gates" -> Kit.SNOWBALLS_ONLY;
            default -> Kit.CONCRETE_PICKAXE_SNOWBALLS;
        };
        Check.contract(d, kit, r);
        Check.start(d, r);
        Check.postPass(d, g.post, r);
        Check.stairs(g, r);
        r.check(d.chickenSpawns.size() >= 8 && d.chickenSpawns.stream().allMatch(c -> d.pen.contains(c.x(), c.y(), c.z())),
                d.chickenSpawns.size() + " chicken spawns inside the pen " + (d.pen.maxX() - d.pen.minX() + 1) + " x " + (d.pen.maxZ() - d.pen.minZ() + 1));
        Spawn mid = d.startSpawns.get(d.startSpawns.size() / 2);
        if (!g.landmark.isEmpty()) {
            Check.landmark(d, Check.afterGo(d, d.blocks), g.landmark, r);
            double[] t = g.landmark.get(0);
            o.cam = new double[]{mid.x(), mid.y() + 1.62, mid.z(), t[0], t[1], t[2]};
        } else {
            r.line("  --    landmark ray-cast not required (indoor map)");
            o.cam = new double[]{mid.x(), mid.y() + 1.62, mid.z(), mid.x(), mid.y() + 1.62, mid.z() + 40};
        }
        switch (id) {
            case "quarry_steps" -> quarry(g, r);
            case "ticket_gates" -> TicketV.run(g, r, o);
            case "frozen_canal" -> CanalV.run(g, r);
            case "kite_festival" -> KiteV.run(g, r);
            default -> { }
        }
        r.line(r.errors == 0 ? "  RESULT: all checks passed" : "  RESULT: " + r.errors + " problem(s)");
        return o;
    }

    // ------------------------------------------------------------------ shared walk reporting

    static List<int[]> starts(TgMapData d) {
        List<int[]> s = new ArrayList<>();
        for (Spawn p : d.startSpawns) s.add(new int[]{(int) Math.floor(p.x()), (int) Math.floor(p.y()), (int) Math.floor(p.z())});
        return s;
    }

    /** Print one walking/bridging line; returns its time (s) or -1. */
    static double line(Check.Report r, String name, Walk w, TgMapData d, List<Region> stages, Predicate<int[]> skipStraight, int maxStraight) {
        Walk.Result res = w.route(starts(d), stages);
        if (!res.ok()) { r.err(name + ": NO ROUTE (" + res.visited + " cells searched)"); return -1; }
        Walk.Node e = res.end;
        int[] st = w.longestStraight(res.path, skipStraight);
        if (System.getenv("DEBUGPATH") != null) {
            StringBuilder sb = new StringBuilder("        path: ");
            String last = "";
            for (Walk.Node n : res.path) { String h = n.how.split(" ")[0]; if (!h.equals(last) || h.equals("jump")) sb.append(n.how).append("@").append(n.x).append(",").append(n.y).append(",").append(n.z).append(" "); last = h; }
            r.line(sb.toString());
        }
        r.check(st[0] <= maxStraight, String.format("%s: path %.0f blocks, %d jumps, hardest %s (difficulty %.1f), %d blocks placed, "
                        + "longest straight free stretch %d (at %d,%d,%d; limit %d), est. %.1f s (expert pace)",
                name, e.len, e.jumps, e.hardest, e.maxGap, e.blocks, st[0], st[1], st[2], st[3], maxStraight, e.cost));
        return e.cost;
    }

    static Predicate<int[]> outside(Region... rs) {
        return p -> {
            for (Region q : rs) if (q.contains(p[0] + 0.5, p[1] + 0.1, p[2] + 0.5)) return true;
            return false;
        };
    }

    // ------------------------------------------------------------------ Quarry Steps

    static void quarry(G g, Check.Report r) {
        TgMapData d = g.d;
        TgMapData.Blocks go = Check.afterGo(d, d.blocks);
        TgMapData v = Check.view(d, go);
        Predicate<int[]> skip = outside(Region.of(-19, -5, -1, 18, 10, 10), d.pen);
        r.line("  route lines (walk: sprint 5.6 b/s, flat gap <= 4, -1 per block of rise; bridging inside buildBounds):");

        Walk w = new Walk(v, go);
        Region r1 = Region.of(-19, -6, 32, -16, -4, 35);
        Region r2cut = Region.of(17, -9, 45, 19, -9, 47);
        Region cutTop = Region.of(35, 6, 45, 36, 6, 47);
        double safe = line(r, "SAFE  haul road + cut (choke 3 wide)", w, d, List.of(r1, r2cut, cutTop, d.pen), skip, 15);
        Region r2right = Region.of(15, -9, 45, 19, -9, 48);
        Region pad = Region.of(15, -1, 32, 19, -1, 36);
        Region plateau = Region.of(27, 6, 32, 30, 6, 36);
        double wide = line(r, "SAFE' haul road + wide ramp", w, d, List.of(r1, r2right, pad, plateau, d.pen), skip, 15);

        Walk wb = new Walk(v, go);
        wb.build = d.buildBounds;
        wb.allow = p -> Math.abs(p[0]) <= 12 || p[2] < 11 || p[2] > 49;
        wb.bridgeRate = 4.0;
        wb.pillarTime = 0.35;
        double fastExpert = line(r, "FAST  bridge straight across (expert 4 b/s)", wb, d, List.of(d.pen), skip, 15);
        wb.bridgeRate = 2.2;
        wb.pillarTime = 0.55;
        double fastAvg = line(r, "FAST  same line, average bridger 2.2 b/s", wb, d, List.of(d.pen), skip, 15);
        // least blocks for the straight crossing (central corridor)
        Walk wm = new Walk(v, go);
        wm.build = d.buildBounds;
        wm.allow = p -> wb.allow.test(p) && (p[1] >= -1 || p[2] < 11);
        wm.blockWeight = 1000;
        Walk.Result least = wm.route(starts(d), List.of(Region.of(-12, 6, 49, 12, 7, 53)));
        int need = least.ok() ? least.end.blocks : -1;
        if (System.getenv("DEBUGPATH") != null && least.ok()) {
            StringBuilder sb = new StringBuilder("        least-blocks path: ");
            String last = "";
            for (Walk.Node n : least.path) { String h = n.how.split(" ")[0]; if (!h.equals(last) || h.equals("jump")) sb.append(n.how).append("@").append(n.x).append(",").append(n.y).append(",").append(n.z).append(" "); last = h; }
            r.line(sb.toString());
        }
        r.check(need >= 40, "straight-line bridging need (central corridor |x| <= 12, least-blocks search): " + need + " blocks (>= 40 of 64)");
        Walk wf = new Walk(v, go);
        wf.build = d.buildBounds;
        wf.allow = wb.allow;
        wf.blockWeight = 1000;
        Walk.Result lb = wf.route(starts(d), List.of(d.pen));
        r.line("        (mixed: bridge, drop onto the far bench and take the cut: least " + (lb.ok() ? lb.end.blocks : -1) + " blocks)");
        // mixed line: bridge to the ramp landing, then the ramp
        Walk wx = new Walk(v, go);
        wx.build = d.buildBounds;
        double mixed = line(r, "MIXED bridge to the ramp pad, ramp E up (avg bridger)", wx, d, List.of(pad, plateau, d.pen), skip, 15);
        // no-block route exists
        r.check(safe > 0, "the built route is completable without placing blocks");
        if (safe > 0 && fastExpert > 0)
            r.check(fastExpert <= safe * 0.8, String.format("fast line saves %.0f%% vs the safe line for an expert (>= 20%%)", 100 * (1 - fastExpert / safe)));
        r.line(String.format("  time profile: expert bridge %.0f s, expert road %.0f s; median players (x1.6 + crowd) road ~%.0f s, bridge ~%.0f s",
                fastExpert, safe, safe * 1.6 + 6, fastAvg * 1.35 + 4));
    }
}
