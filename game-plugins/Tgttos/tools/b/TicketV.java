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
        Region tunnelExit = Region.of(-21, 1, 41, 21, 1, 41);
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

        Predicate<int[]> skip = Validators.outside(Region.of(-18, 0, -1, 17, 4, 6), d.pen);
        Region coupl1 = Region.of(TicketGates.C1 - 2, -5, TicketGates.T1, TicketGates.C1 + 2, 3, TicketGates.T1 + 5);
        Region coupl2 = Region.of(TicketGates.C2 - 2, -5, TicketGates.T2, TicketGates.C2 + 2, 3, TicketGates.T2 + 5);
        r.line("  route lines (walk: sprint 5.6 b/s; variants as stated):");
        // SAFE: every turnstile jammed, carriages through doors (no coupling gaps)
        TgMapData vs = Check.view(d, Check.afterGo(d, Check.withOptions(d, all(d, "JAMMED"))));
        Walk ws = new Walk(vs, vs.blocks);
        ws.allow = p -> !coupl1.contains(p[0] + .5, p[1] + .1, p[2] + .5) && !coupl2.contains(p[0] + .5, p[1] + .1, p[2] + .5);
        double safe = Validators.line(r, "SAFE  staff corridor + carriage doors (all gates jammed)", ws, vs, List.of(d.pen), skip, 15);
        // FAST: all open, couplers allowed
        TgMapData vf = Check.view(d, Check.afterGo(d, Check.withOptions(d, all(d, "OPEN"))));
        Walk wf = new Walk(vf, vf.blocks);
        double fast = Validators.line(r, "FAST  open turnstile + coupling jumps (all gates open)", wf, vf, List.of(d.pen), skip, 15);
        Walk wf2 = new Walk(vf, vf.blocks);
        wf2.allow = ws.allow;
        double openDoors = Validators.line(r, "MID   open turnstile + carriage doors", wf2, vf, List.of(d.pen), skip, 15);
        // DROP: middle gates drop, rest jammed: forced through the tunnel
        Map<String, String> dropPick = all(d, "DROP");
        TgMapData vd = Check.view(d, Check.afterGo(d, Check.withOptions(d, dropPick)));
        Walk wd = new Walk(vd, vd.blocks);
        wd.allow = p -> p[0] > -22;   // not the staff corridor
        double drop = Validators.line(r, "DROP  through a dropping turnstile + tunnel stairs + couplers", wd, vd, List.of(d.pen), skip, 15);
        if (safe > 0 && fast > 0) {
            r.check(fast <= safe * 0.8, String.format("fast line saves %.0f%% vs the safe line (>= 20%%)", 100 * (1 - fast / safe)));
            r.check(fast >= 25 && fast <= 35, String.format("expert time %.1f s within 25-35 s (owner, playtest 2: longer map: third track + queue hall, plaza moved %d along)", fast, com.mcm.games.tgttos.maps.b.TicketGates.DZ));
            double gamble = (fast + 2.5) * 0.45 + (fast + 4.0) * 0.3 + drop * 0.25;   // a jam costs ~2.5-4 s to back out and retry
            r.line(String.format("  gamble estimate (1/3 open, 1/3 jammed, 1/3 drop, crowd-following): ~%.0f s expert; median ~%.0f s; safe line median ~%.0f s",
                    gamble, gamble * 1.6 + 5, safe * 1.5 + 4));
        }
        {   // coupling anvil traps: the cells are the anvil + its stand, the trigger is the feet cell on it, below is the hazard pit
            var tf = com.mcm.games.tgttos.maps.b.TicketGates.feats();
            int ok = 0;
            StringBuilder tb = new StringBuilder();
            for (var tr : tf.traps) {
                var a0 = tr.gone().get(0); var a1 = tr.gone().get(1);
                String b0 = d.blocks.get(a0.x(), a0.y(), a0.z()), b1 = d.blocks.get(a1.x(), a1.y(), a1.z());
                boolean pit = false;
                for (var h : d.hazardRegions) if (h.contains(a1.x() + 0.5, a1.y() - 0.5, a1.z() + 0.5)) pit = true;
                boolean good = b0 != null && b0.contains("anvil") && b1 != null && b1.contains("iron_bars") && tr.trigger().contains(a0.x() + 0.5, a0.y() + 1.0, a0.z() + 0.5) && pit;
                if (good) ok++;
                tb.append(String.format(" %s at (%d,%d,%d)%s;", tr.name(), a0.x(), a0.y(), a0.z(), good ? "" : " BAD"));
            }
            r.check(ok == 3 && tf.traps.size() == 3, String.format("3 coupling anvil traps (creak %.1f s after a racer lands, drop into the live pit, back %.1f s later):%s",
                    com.mcm.games.tgttos.maps.b.TicketGates.TRAP_DELAY / 20.0, com.mcm.games.tgttos.maps.b.TicketGates.TRAP_RESTORE / 20.0, tb));
        }
        // a cutaway (no roof / walls above y 9) for the iso preview
        Check.Overlay cut = new Check.Overlay(d.blocks);
        d.blocks.forEach((x, y, z, s) -> { if (y > 9 || (z < 0 && y > 2)) cut.set(x, y, z, null); });
        o.extraDumps = new LinkedHashMap<>();
        o.extraDumps.put("cut", cut);
    }
}
