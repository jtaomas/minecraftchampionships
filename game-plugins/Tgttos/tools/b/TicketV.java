import com.mcm.games.tgttos.maps.b.TicketGates;
import com.mcm.games.tgttos.maps.b.util.Check;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.maps.b.util.Walk;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Ticket Gates: turnstile variant consistency + safe / fast / drop lines. */
final class TicketV {
    private TicketV() {}

    static Map<String, String> all(TgMapData d, String opt) {
        Map<String, String> m = new HashMap<>();
        for (TgMapData.VariantGroup gr : d.variants) m.put(gr.name, opt);
        return m;
    }

    static void run(G g, Check.Report r, Validators.Out o) {
        TgMapData d = g.d;
        int z0 = TicketGates.GATE_Z;
        // rule + options
        int groups = 0;
        boolean opts = true;
        for (TgMapData.VariantGroup gr : d.variants) {
            groups++;
            opts &= gr.options.keySet().containsAll(List.of("OPEN", "JAMMED", "DROP"));
        }
        boolean rule = d.variantRules.stream().anyMatch(v -> v.category().equals("turnstile") && v.option().equals("OPEN") && v.minCount() >= 4);
        r.check(groups == 12 && opts && rule, groups + " turnstile groups with OPEN / JAMMED / DROP, rule: at least 4 OPEN per round");

        // each gate, each option, everything else JAMMED
        Region tunnelExit = Region.of(-21, 1, 41, 21, 1, 41);   // platform A
        int okGates = 0;
        StringBuilder detail = new StringBuilder();
        for (int i = 0; i < 12; i++) {
            String name = String.format("gate_%02d", i + 1);
            int gx = TicketGates.gateX(i);
            Region box = Region.of(gx - 1, -10, z0 - 3, gx + 2, 4, z0 + 5);
            Region entry = Region.of(gx, 1, z0 - 1, gx + 1, 1, z0 - 1);
            Region beyond = Region.of(gx, 1, z0 + 4, gx + 1, 1, z0 + 4);
            boolean good = true;
            for (String opt : List.of("OPEN", "JAMMED", "DROP")) {
                Map<String, String> pick = all(d, "JAMMED");
                pick.put(name, opt);
                Check.Overlay ov = Check.withOptions(d, pick);
                TgMapData v = Check.view(d, Check.afterGo(d, ov));
                Walk w = new Walk(v, v.blocks);
                w.allow = p -> box.contains(p[0] + 0.5, p[1] + 0.1, p[2] + 0.5);
                List<int[]> st = List.of(new int[]{gx, 1, z0 - 2});
                boolean through = w.route(st, List.of(beyond)).ok();
                boolean oneIn = w.route(st, List.of(Region.of(gx, 1, z0, gx + 1, 1, z0))).ok();
                boolean twoIn = w.route(st, List.of(Region.of(gx, 1, z0 + 1, gx + 1, 1, z0 + 1))).ok();
                boolean down = w.route(st, List.of(Region.of(gx - 1, TicketGates.LOW + 1, z0 - 3, gx + 2, TicketGates.LOW + 1, z0 + 5))).ok();
                boolean res;
                if (opt.equals("OPEN")) res = through && !down;
                else if (opt.equals("JAMMED")) res = oneIn && !twoIn && !through && !down;
                else {
                    Walk wr = new Walk(v, v.blocks);
                    boolean rejoin = down && wr.route(List.of(new int[]{gx, TicketGates.LOW + 1, z0 + 1}), List.of(tunnelExit)).ok();
                    res = down && !through && rejoin;
                }
                if (!res) { good = false; detail.append(" ").append(name).append(":").append(opt).append(" FAILED"); }
            }
            if (good) okGates++;
        }
        r.check(okGates == 12, "turnstile variants consistent: " + okGates + "/12 (OPEN passes, JAMMED blocks after 1 block, DROP falls to the tunnel that rejoins at platform A)" + detail);

        Predicate<int[]> skip = p -> true;   // owner, playtest 4: no posts to break up straight runs (they read as random walls)
        Region coupl1 = Region.of(TicketGates.C1 - 2, -5, TicketGates.T1, TicketGates.C1 + 2, 3, TicketGates.T1 + 5);
        Region coupl2 = Region.of(TicketGates.C2 - 2, -5, TicketGates.T2, TicketGates.C2 + 2, 3, TicketGates.T2 + 5);
        Region coupl3 = Region.of(TicketGates.C3 - 2, -5, TicketGates.T3, TicketGates.C3 + 2, 3, TicketGates.T3 + 5);
        Predicate<int[]> noCouplers = p -> !coupl1.contains(p[0] + .5, p[1] + .1, p[2] + .5) && !coupl2.contains(p[0] + .5, p[1] + .1, p[2] + .5)
                && !coupl3.contains(p[0] + .5, p[1] + .1, p[2] + .5);
        r.line("  route lines (walk: sprint 5.6 b/s; variants as stated; movers and pitfalls not timed):");
        // SAFE: one edge turnstile open, the rest jammed, carriage doors (no coupling anvils)
        Map<String, String> edge = all(d, "JAMMED");
        edge.put("gate_01", "OPEN");
        TgMapData vs = Check.view(d, Check.afterGo(d, Check.withOptions(d, edge)));
        Walk ws = new Walk(vs, vs.blocks);
        ws.allow = noCouplers;
        double safe = Validators.line(r, "SAFE  edge turnstile + carriage doors", ws, vs, List.of(d.pen), skip, 999);
        TgMapData vf = Check.view(d, Check.afterGo(d, Check.withOptions(d, all(d, "OPEN"))));
        Walk wf = new Walk(vf, vf.blocks);
        double fast = Validators.line(r, "FAST  open turnstile + coupling anvils (all gates open)", wf, vf, List.of(d.pen), skip, 999);
        TgMapData vd = Check.view(d, Check.afterGo(d, Check.withOptions(d, all(d, "DROP"))));
        Walk wd = new Walk(vd, vd.blocks);
        double drop = Validators.line(r, "DROP  through a dropping turnstile + tunnel stairs", wd, vd, List.of(d.pen), skip, 999);
        // every pitfall open at once: a route that never steps on one must still exist (knowing the map pays off)
        Check.Overlay holes = new Check.Overlay(Check.afterGo(d, Check.withOptions(d, all(d, "OPEN"))));
        for (int[] p : TicketGates.pitfalls()) holes.set(p[0], 0, p[1], null);
        TgMapData vh = Check.view(d, holes);
        Walk wh = new Walk(vh, vh.blocks);
        double clean = Validators.line(r, "CLEAN every pitfall avoided (all of them dropped)", wh, vh, List.of(d.pen), skip, 999);
        if (safe > 0 && fast > 0) {
            r.check(fast < safe, String.format("the gamble line is faster than the safe line (%.1f s vs %.1f s)", fast, safe));
            r.check(fast >= 30 && fast <= 70, String.format("expert time %.1f s within 30-70 s (perfect line, no wrong turns or waiting on lamps; real rounds run far longer)", fast));
        }
        {   // anvils + pitfalls
            var tf = TicketGates.feats();
            int anvils = 0, pits = 0, badPits = 0;
            for (var tr : tf.traps) {
                if (tr.name().startsWith("track")) {
                    var a0 = tr.gone().get(0); var a1 = tr.gone().get(1);
                    String b0 = d.blocks.get(a0.x(), a0.y(), a0.z()), b1 = d.blocks.get(a1.x(), a1.y(), a1.z());
                    boolean pit = false;
                    for (var h : d.hazardRegions) if (h.contains(a1.x() + 0.5, a1.y() - 0.5, a1.z() + 0.5)) pit = true;
                    if (b0 != null && b0.contains("anvil") && b1 != null && b1.contains("iron_bars") && pit) anvils++;
                } else {
                    pits++;
                    var c = tr.gone().get(0);
                    boolean good = d.blocks.get(c.x(), 0, c.z()) != null && d.blocks.get(c.x(), 1, c.z()) == null;
                    for (int y = -1; y >= d.voidY; y--) good &= d.blocks.get(c.x(), y, c.z()) == null;
                    for (var h : d.hazardRegions) if (h.contains(c.x() + 0.5, -1.5, c.z() + 0.5)) good = false;
                    if (!good) { badPits++; r.line("        bad " + tr.name() + " at " + tr.gone().get(0)); }
                }
            }
            r.check(anvils == 3, anvils + "/3 coupling anvil traps over the live pits");
            r.check(pits >= 40 && badPits == 0, pits + " hidden single-block pitfalls (a floor block with nothing under it but the void, nothing on top)" + (badPits > 0 ? ", " + badPits + " BAD" : ""));
            r.check(tf.movers.size() == 6, tf.movers.size() + " swinging lamps over platforms B and C");
            r.check(tf.checkpoints.size() == 3, tf.checkpoints.size() + " checkpoints (platform A, platform D, maze entrance)");
            var m = TicketGates.maze();
            int dead = 0;
            for (int cx = 0; cx < TicketGates.MAZE_CELLS; cx++) for (int rr = 0; rr < TicketGates.MAZE_CELLS; rr++) if (m.openings(cx, rr) == 1) dead++;
            int best = m.shortest();
            r.check(best >= 2 * TicketGates.MAZE_CELLS, "maze: shortest way through is " + best + " cells (>= " + 2 * TicketGates.MAZE_CELLS + ", twice straight across)");
            r.line(String.format("  maze: %d x %d cells, %d dead ends, no traps, walls %d high, %d entrances / %d exits",
                    TicketGates.MAZE_CELLS, TicketGates.MAZE_CELLS, dead, TicketGates.CEIL, m.in.length, m.out.length));
        }
        // a cutaway (no roof / walls above y 9) for the iso preview
        Check.Overlay cut = new Check.Overlay(d.blocks);
        d.blocks.forEach((x, y, z, s) -> { if (y > 9 || (z < 0 && y > 2)) cut.set(x, y, z, null); });
        o.extraDumps = new LinkedHashMap<>();
        o.extraDumps.put("cut", cut);
    }
}
