package com.mcm.games.parkourwarrior.map;

import com.mcm.games.parkourwarrior.CourseModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Offline validators for the generated course (fail the build on any ERROR).
 * <ul>
 *   <li><b>Movement simulation</b> ({@link JumpSim} / {@link Phys}): every consecutive foothold pair of every
 *   obstacle is simulated tick by tick; its timing window (jump ticks that land) must be at least 1 and at least the
 *   tier minimum ({@link Tier}). Neo walls of 3+ are rejected everywhere, 2-walls outside gold / hard.</li>
 *   <li><b>Bypass search</b> ({@link Bypass}): the pad must not be reachable without the intended footholds.</li>
 *   <li><b>Kill-volume overlap</b>: no foothold, frame walkway or straight jump line lies in any obstacle's kill
 *   column below its kill-Y (the runtime kills there; this was the Spine Scramble instant death).</li>
 *   <li><b>Timed phase</b>: every run of timed footholds can be crossed for a window of start ticks per cycle.</li>
 *   <li>Checkpoint pad after every obstacle (pad material only there), kill-Y below every foothold, footholds belong
 *   to real objects, no foothold-height decoys, lengths, neighbour ideas, >= 3 timed obstacles, difficulty ordering
 *   (main ramp, bronze < silver < gold per zone, gold above the Runaway Rails floor and within ~25 attempts, the
 *   hard ending the hardest), fence/pane/wall connections set (post-pass), staircases (walk check).</li>
 * </ul>
 */
final class Validate {
    private Validate() {}

    enum Tier {
        Z1(3, 4, true), Z2(2, 3, true), Z3(2, 3, true), Z4(1, 2, true), Z5(1, 2, true),
        BRONZE(1, 2, true), SILVER(0, 1, true), GOLD(0, 1, true), EASY(3, 4, true), MEDIUM(1, 2, true), HARD(0, 1, true);
        final int minWorst, minBest;
        final boolean late;
        Tier(int w, int b, boolean late) { this.minWorst = w; this.minBest = b; this.late = late; }
    }

    static Tier tier(Ob o) {
        if (o.kind == CourseModel.Kind.MAIN) return Tier.values()[o.stage - 1];
        if (o.kind == CourseModel.Kind.BONUS) return o.index == 1 ? Tier.BRONZE : o.index == 2 ? Tier.SILVER : Tier.GOLD;
        return o.ending == CourseModel.Difficulty.EASY ? Tier.EASY : o.ending == CourseModel.Difficulty.MEDIUM ? Tier.MEDIUM : Tier.HARD;
    }

    static final class Report {
        final StringBuilder text = new StringBuilder();
        int errors, warnings;
        final Map<String, Double> attempts = new LinkedHashMap<>();
        final Map<String, String> tableRow = new LinkedHashMap<>();
        void err(String s) { errors++; text.append("  ERROR ").append(s).append('\n'); }
        void warn(String s) { warnings++; text.append("  warn  ").append(s).append('\n'); }
        /** Imported sections keep their map's own scenery: object-size and decoy findings are warnings there. */
        void errUnless(boolean imported, String s) { if (imported) warn(s); else err(s); }
        void line(String s) { text.append(s).append('\n'); }
    }

    /** Fail chance of one jump from its mean window, calibrated on the owner playtest: Runaway Rails (two 4b at window 1, one 3+1 at 1/2) about 10 tries, 3+1 at 1/2 feels easy. */
    static double failP(double w) {
        double[][] t = {{0, 0.97}, {0.5, 0.88}, {1, 0.65}, {1.5, 0.35}, {2, 0.18}, {2.5, 0.11}, {3, 0.06}, {4, 0.03}, {5, 0.015}, {6, 0.008}, {8, 0.003}, {12, 0.001}};
        if (w >= 12) return 0.001;
        for (int i = 1; i < t.length; i++) if (w <= t[i][0]) {
            double f = (w - t[i - 1][0]) / (t[i][0] - t[i - 1][0]);
            return t[i - 1][1] + f * (t[i][1] - t[i - 1][1]);
        }
        return 0.001;
    }

    static Report run(W w) {
        Report r = new Report();
        Map<Long, String> timed = new HashMap<>();
        for (Ob o : w.obs) timed.putAll(o.timedPreview);
        Phys.World wd = new Phys.World(w, timed);
        r.line("Parkour Warrior course validation  (seed " + w.model.seed + ")");
        r.line("blocks: " + w.b.size() + ", obstacles: " + w.obs.size() + ", timed groups: " + w.model.timed.size());
        r.line("movement model: Phys (1.21.1 tick simulation, W only 0.98, sprint, one-tick-late jump allowed, rest start at the back of each foothold, 8 alignments, chained landings)");
        // kill volumes of every obstacle (as the runtime builds them)
        List<double[]> areas = new ArrayList<>();
        List<String> areaIds = new ArrayList<>();
        for (Ob o : w.obs) {
            if (o.built == null || o.built.area() == null) continue;
            CourseModel.Box a = o.built.area();
            areas.add(new double[]{a.minX(), a.minZ(), a.maxX(), a.maxZ(), o.killY});
            areaIds.add(o.id);
        }

        int jumps = 0, decoys = 0, ledges = 0, tiny = 0, footholds = 0, bypasses = 0, killOverlaps = 0;
        Set<String> ids = new HashSet<>(), names = new HashSet<>();
        for (Ob o : w.obs) {
            Tier tier = tier(o);
            if (!ids.add(o.id)) r.err(o.id + ": duplicate id");
            if (!names.add(o.name)) r.err(o.id + ": duplicate name " + o.name);
            if (o.padBox == null) { r.err(o.id + ": no completion pad"); continue; }
            for (int x = o.padBox.minX(); x <= o.padBox.maxX(); x++) for (int z = o.padBox.minZ(); z <= o.padBox.maxZ(); z++)
                if (!W.PAD.equals(w.get(x, o.padBox.minY(), z))) r.err(o.id + ": pad block missing at " + x + "," + o.padBox.minY() + "," + z);
            int minFeet = o.minFeet();
            if (o.killY >= minFeet) r.err(o.id + ": killY " + o.killY + " not below lowest foothold feet " + minFeet);

            // ---- movement simulation per step
            JumpSim.Kill kill = new JumpSim.Kill(o.killY, areas, areaIds);
            JumpSim.Cfg cfg = new JumpSim.Cfg();
            cfg.allowLate = true;   // the one-tick-late jump (Y-first collision) is real 1.21 physics and happens unasked
            double pass = 1.0;
            int hardW = 99, hardB = 99;
            String hardDesc = "-";
            List<JumpSim.Res> stepRes = new ArrayList<>();
            List<int[]> stepIdx = new ArrayList<>();
            StringBuilder detail = new StringBuilder();
            List<Phys.P> prevArr = null;
            for (int i = 1; i < o.route.size(); ) {
                Ob.Node a = o.route.get(i - 1);
                List<Ob.Node> tg = new ArrayList<>();
                int j = i;
                tg.add(o.route.get(j));
                while (o.route.get(j).t == Ob.T.SLIME && j + 1 < o.route.size()) { j++; tg.add(o.route.get(j)); }
                JumpSim.Res res = JumpSim.eval(wd, a, tg, kill, cfg, prevArr);
                prevArr = res.arrivals;
                stepRes.add(res);
                stepIdx.add(new int[]{i - 1, j});
                jumps++;
                String tag = o.id + " step " + i + (j > i ? "-" + j : "") + " (" + res.describe() + ")";
                detail.append(String.format(Locale.ROOT, "    %-5s %-52s window %d/%d  (rest %d/%d, chained %d/%d)%s%n", (i + (j > i ? "-" + j : "")), res.describe(),
                        res.worst, res.best, res.restWorst, res.restBest, res.chainWorst, res.chainBest, res.trivial ? " trivial" : ""));
                if (res.best == 0) {
                    r.err(tag + ": IMPOSSIBLE in the simulation" + (res.killHit ? " (every landing is inside a kill volume)" : "")
                            + (res.fail.isEmpty() ? "" : " - " + res.fail));
                } else {
                    if (res.worst < tier.minWorst) r.err(tag + ": window " + res.worst + " at the worst alignment < " + tier.minWorst + " (tier " + tier + ")");
                    if (res.best < tier.minBest) r.err(tag + ": window " + res.best + " at the best alignment < " + tier.minBest + " (tier " + tier + ")");
                }
                if (res.curved && res.wallLen >= 1) {
                    if (res.wallLen >= 3) r.err(tag + ": neo around a " + res.wallLen + "-block wall (never buildable)");
                    else if (res.wallLen == 2 && tier != Tier.GOLD && tier != Tier.HARD) r.err(tag + ": double neo outside gold / hard");
                    else if (tier == Tier.Z1 || tier == Tier.Z2 || tier == Tier.EASY) r.err(tag + ": neo in zone 1-2 / easy ending");
                }
                if (!res.trivial) {
                    double we = (res.worst + res.best) / 2.0;
                    pass *= 1 - failP(we);
                    if (res.worst * 10 + res.best < hardW * 10 + hardB) { hardW = res.worst; hardB = res.best; hardDesc = res.describe(); }
                }
                i = j + 1;
            }
            // ---- timed phase windows
            double ph = phaseCheck(r, o, stepRes, stepIdx, tier);
            if (ph < 1) pass *= Math.max(0.3, ph);
            pass *= moverCheck(r, wd, o, stepRes, stepIdx, tier);
            crumbleCheck(r, o, stepRes, stepIdx, tier);
            for (CourseModel.Belt bt : o.belts) if (Math.hypot(bt.vx(), bt.vz()) > 0.12) r.err(o.id + ": belt faster than 0.12 b/t");
            double att = 1.0 / Math.max(1e-6, pass);
            r.attempts.put(o.id, att);
            String hd = hardW == 99 ? "none (walk)" : hardDesc;
            r.tableRow.put(o.id, String.format(Locale.ROOT, "| %s | %s | %s | %s | %s | %s | %.1f |", o.id, o.name, tier, o.idea, hd,
                    hardW == 99 ? "-" : hardW + "/" + hardB, att));

            // ---- footholds belong to real objects
            for (Ob.Node n : o.route) {
                if (n.t == Ob.T.LADDER || n.t == Ob.T.TIMED) continue;
                footholds++;
                int obj = objectSize(w, n.cells, 400);
                if (obj < Math.max(8, n.cells.size() * 1.5 + 4)) { tiny++; r.errUnless(o.imported, o.id + ": foothold at " + c(n.cells.get(0)) + " belongs to an object of only " + obj + " blocks (plain cube?)"); }
            }
            // ---- decoys near the route at foothold height
            Set<Long> legit = new HashSet<>(o.safe);
            for (Ob.Node n : o.route) for (int[] cc : n.cells) legit.add(W.key(cc[0], cc[1], cc[2]));
            int[] bb = bbox(o);
            int maxFeet = o.maxFeet();
            List<double[]> segs = new ArrayList<>();
            for (int i = 1; i < o.route.size(); i++) {
                int[][] p = JumpSim.nearest(o.route.get(i - 1).cells, o.route.get(i).cells);
                double fa = feetCell(wd, p[0], o.route.get(i - 1)), fb = feetCell(wd, p[1], o.route.get(i));
                segs.add(new double[]{p[0][0] + .5, p[0][2] + .5, p[1][0] + .5, p[1][2] + .5, Math.min(fa, fb), Math.max(fa, fb)});
            }
            int od = 0, ol = 0;
            for (int x = bb[0] - 3; x <= bb[3] + 3; x++) for (int z = bb[2] - 3; z <= bb[5] + 3; z++) for (int y = o.killY - 1; y <= maxFeet + 1; y++) {
                String s = wd.state(x, y, z);
                if (Bs.isAir(s)) continue;
                double top = Phys.top(s);
                if (top <= 0) continue;
                if (Phys.shape(wd.state(x, y + 1, z)).length > 0 || Phys.shape(wd.state(x, y + 2, z)).length > 0) continue;
                long k = W.key(x, y, z);
                if (legit.contains(k) || w.frameWalk.contains(k) || w.padCells.contains(k) || isLegitElsewhere(w, k)) continue;
                double feet = y + top;
                if (feet < o.killY) continue;
                boolean dec = false;
                for (double[] sg : segs) {
                    if (feet < sg[4] - 0.01 || feet > sg[5] + 0.01) continue;
                    if (segDist(x + .5, z + .5, sg) <= 2.0) { dec = true; break; }
                }
                if (dec) { od++; if (od <= 4) r.errUnless(o.imported, o.id + ": decoy standable block at foothold height near the route: " + x + "," + y + "," + z + " " + Bs.id(s)); }
                else ol++;
            }
            if (od > 4) r.errUnless(o.imported, o.id + ": ... " + (od - 4) + " more decoys");
            decoys += od; ledges += ol;
            // ---- bypass search
            for (String b : Bypass.check(wd, o, o.killY)) { bypasses++; r.err(o.id + ": " + b); }
            // ---- timed positions empty in the static store
            for (CourseModel.Timed t : o.timed) for (CourseModel.BlockPos p : t.positions())
                if (!Bs.isAir(w.get(p.x(), p.y(), p.z()))) r.err(o.id + ": timed position not air in the static store " + p);
            // ---- length
            int sx = o.wx(0, 0), sz = o.wz(0, 0), ex = o.wx(o.endX, o.endZ), ez = o.wz(o.endX, o.endZ);
            double span = o.kind == CourseModel.Kind.ENDING ? Math.hypot(Math.hypot(ex - sx, ez - sz), o.endY) - 3 : Math.hypot(ex - sx, ez - sz) - 3;
            if (o.kind != CourseModel.Kind.ENDING && (span < 15 || span > (o.imported ? 60 : 35))) r.err(o.id + ": length " + f(span) + " outside 15..35");
            if (o.kind == CourseModel.Kind.ENDING && (span < 8 || span > 35)) r.err(o.id + ": ending length " + f(span) + " outside 8..35");
            r.line(String.format(Locale.ROOT, "%-10s %-24s %-7s %-6s len %4.1f  steps %2d  tightest %s window %s  est. attempts %.1f  killY %d",
                    o.id, o.name, o.idea, tier, span, stepRes.size(), hardDesc, hardW == 99 ? "-" : hardW + "/" + hardB, att, o.killY));
            r.text.append(detail);
        }

        // ---- kill-volume overlap: footholds, frame walkways and jump lines vs every obstacle's kill column
        r.line("");
        r.line("== course checks");
        List<double[]> pts = new ArrayList<>();   // x, feet, z, owner index
        for (int oi = 0; oi < w.obs.size(); oi++) {
            Ob o = w.obs.get(oi);
            for (int i = 0; i < o.route.size(); i++) {
                Ob.Node n = o.route.get(i);
                for (int[] cc : n.cells) pts.add(new double[]{cc[0] + 0.5, feetCell(wd, cc, n), cc[2] + 0.5, oi});
                if (i > 0) {
                    int[][] p = JumpSim.nearest(o.route.get(i - 1).cells, n.cells);
                    double fa = feetCell(wd, p[0], o.route.get(i - 1)), fb = feetCell(wd, p[1], n);
                    for (int k = 1; k < 8; k++) {
                        double t = k / 8.0;
                        pts.add(new double[]{p[0][0] + .5 + (p[1][0] - p[0][0]) * t, Math.min(fa, fb), p[0][2] + .5 + (p[1][2] - p[0][2]) * t, oi});
                    }
                }
            }
        }
        for (long k : w.frameWalk) {
            String s = w.get(W.kx(k), W.ky(k), W.kz(k));
            double t = Phys.top(s);
            if (t > 0) pts.add(new double[]{W.kx(k) + 0.5, W.ky(k) + t, W.kz(k) + 0.5, -1});
        }
        Set<String> seenOverlap = new HashSet<>();
        for (double[] p : pts) for (int i = 0; i < areas.size(); i++) {
            double[] a = areas.get(i);
            int bx = (int) Math.floor(p[0]), bz = (int) Math.floor(p[2]);
            if (bx >= a[0] && bx <= a[2] && bz >= a[1] && bz <= a[3] && p[1] < a[4]) {
                String who = p[3] < 0 ? "frame walkway" : w.obs.get((int) p[3]).id;
                String key = who + "|" + areaIds.get(i);
                if (seenOverlap.add(key)) {
                    killOverlaps++;
                    r.err("kill-volume overlap: " + who + " position " + f(p[0]) + "," + f(p[1]) + "," + f(p[2]) + " is inside the kill column of "
                            + areaIds.get(i) + " (kill-Y " + (int) a[4] + ")");
                }
            }
        }
        r.line("kill-volume overlaps: " + killOverlaps);
        // pad material only used for pads
        Set<Long> padBoxes = new HashSet<>();
        for (Ob o : w.obs) if (o.padBox != null)
            for (int x = o.padBox.minX(); x <= o.padBox.maxX(); x++) for (int z = o.padBox.minZ(); z <= o.padBox.maxZ(); z++) padBoxes.add(W.key(x, o.padBox.minY(), z));
        int stray = 0;
        for (long k : w.padCells) if (!padBoxes.contains(k)) { stray++; if (stray < 4) r.err("pad material used outside a pad at " + W.kx(k) + "," + W.ky(k) + "," + W.kz(k)); }
        r.line("pad blocks: " + w.padCells.size() + " (stray " + stray + ")");
        // neighbours with the same idea
        List<List<String>> lines = new ArrayList<>();
        List<String> main = new ArrayList<>();
        for (int s = 1; s <= 5; s++) for (int i = 1; i <= 3; i++) main.add("M" + s + "-" + i);
        lines.add(main);
        for (int s = 1; s <= 5; s++) lines.add(List.of("B" + s + "-1", "B" + s + "-2", "B" + s + "-3"));
        for (CourseModel.Difficulty d : CourseModel.Difficulty.values()) lines.add(List.of("M5-3", "E-" + d + "-1", "E-" + d + "-2"));
        for (List<String> l : lines) for (int i = 1; i < l.size(); i++) {
            Ob a = find(w, l.get(i - 1)), b = find(w, l.get(i));
            if (a == null || b == null) { r.err("missing obstacle " + (a == null ? l.get(i - 1) : l.get(i))); continue; }
            if (a.idea.equals(b.idea)) r.err("neighbours " + a.id + " and " + b.id + " share the idea " + a.idea);
        }
        if (w.obs.size() != 36) r.err("expected 36 obstacles, have " + w.obs.size());
        Set<String> timedObs = new TreeSet<>();
        for (CourseModel.Timed t : w.model.timed) timedObs.add(t.obstacleId());
        r.line("timed obstacles: " + timedObs);
        if (timedObs.size() < 3) r.err("fewer than 3 timed obstacles");
        // ---- difficulty ordering (estimated attempts)
        double[] mainA = new double[6];
        for (int s = 1; s <= 5; s++) {
            double mx = 0;
            for (int i = 1; i <= 3; i++) { double a = r.attempts.getOrDefault("M" + s + "-" + i, 1.0); mainA[s] += a / 3; mx = Math.max(mx, a); }
            double b1 = r.attempts.getOrDefault("B" + s + "-1", 0.0), b2 = r.attempts.getOrDefault("B" + s + "-2", 0.0), b3 = r.attempts.getOrDefault("B" + s + "-3", 0.0);
            r.line(String.format(Locale.ROOT, "stage %d est. attempts: main avg %.2f (max %.2f)  bronze %.2f  silver %.2f  gold %.2f", s, mainA[s], mx, b1, b2, b3));
            if (b1 <= mx) r.err("bronze B" + s + "-1 (" + f(b1) + ") not harder than the hardest main obstacle of stage " + s + " (" + f(mx) + ")");
            if (b2 <= b1) r.err("silver B" + s + "-2 not harder than bronze B" + s + "-1");
            if (b3 <= b2) r.err("gold B" + s + "-3 not harder than silver B" + s + "-2");
            if (b3 > 26) r.err("gold B" + s + "-3 needs about " + f(b3) + " attempts (> ~25)");
            if (s > 1 && mainA[s] < mainA[s - 1] * 0.98) r.err("main line does not ramp: stage " + s + " (" + f(mainA[s]) + ") easier than stage " + (s - 1));
        }
        if (mainA[5] < mainA[1] * 1.5) r.err("main line ramp too flat (stage 5 " + f(mainA[5]) + " vs stage 1 " + f(mainA[1]) + ")");
        Double rr = null;
        for (Ob o : w.obs) if (o.name.equals("Runaway Rails")) rr = r.attempts.get(o.id);
        if (rr != null) {
            r.line(String.format(Locale.ROOT, "Runaway Rails (owner calibration, floor of gold): est. attempts %.2f", rr));
            for (int s = 1; s <= 5; s++) {
                double g = r.attempts.getOrDefault("B" + s + "-3", 0.0);
                if (g < rr * 0.95) r.err("gold B" + s + "-3 (" + f(g) + ") easier than the Runaway Rails floor (" + f(rr) + ")");
            }
        }
        double e1 = avg(r, "E-EASY"), e2 = avg(r, "E-MEDIUM"), e3 = avg(r, "E-HARD");
        r.line(String.format(Locale.ROOT, "endings est. attempts: easy %.2f  medium %.2f  hard %.2f", e1, e2, e3));
        double maxOther = 0;
        for (Map.Entry<String, Double> e : r.attempts.entrySet()) if (!e.getKey().startsWith("E-HARD")) maxOther = Math.max(maxOther, e.getValue());
        if (e3 <= maxOther) r.err("Hard ending (" + f(e3) + ") is not the hardest thing on the map (max other " + f(maxOther) + ")");
        if (!(e1 < e2 && e2 < e3)) r.err("endings not ordered easy < medium < hard");
        double m4 = (mainA[4]);
        if (e2 < m4 * 0.9) r.err("medium ending (" + f(e2) + ") easier than main zone 4 (" + f(m4) + ")");
        // ---- connections (post-pass ran)
        int unconnected = 0;
        final int[] uc = {0};
        w.b.forEach((x, y, z, s) -> {
            int k = Bs.kind(Bs.id(s));
            if ((k == 1 || k == 2 || k == 3) && Bs.prop(s, "north") == null) uc[0]++;
        });
        unconnected = uc[0];
        r.line("connection post-pass: " + (unconnected == 0 ? "ok" : unconnected + " fences/panes/walls without connection states"));
        if (unconnected > 0) r.err(unconnected + " fences/panes/walls without connection states (post-pass not run?)");
        // stairs
        List<String> se = Stairs.validate(w.b, w.lanes);
        r.line("staircases: " + w.lanes.size() + " lanes, problems " + se.size());
        for (String s : se) r.err("stairs: " + s);
        w.model.recomputeBounds();
        r.line("bounds: x " + w.model.minX + ".." + w.model.maxX + "  y " + w.model.minY + ".." + w.model.maxY + "  z " + w.model.minZ + ".." + w.model.maxZ);
        if (w.model.minY < 40 || w.model.maxY > 250) r.err("course outside y 40..250");
        Set<String> mats = new HashSet<>();
        w.b.forEach((x, y, z, s) -> mats.add(Bs.id(s)));
        r.line("distinct block types: " + mats.size());
        if (w.model.spawns.isEmpty()) r.err("no spawns");
        List<String> lo = labelOverlaps(w);
        r.line("label overlaps: " + lo.size());
        for (String x : lo) r.err("labels: " + x);
        if (w.model.hubs.size() != 5) r.err("expected 5 hubs");
        if (w.model.finishes.size() != 3) r.err("expected 3 finishes");
        if (w.model.fork == null) r.err("no fork");
        for (CourseModel.Pos p : spawnLike(w)) {
            int x = (int) Math.floor(p.x()), y = (int) Math.floor(p.y()), z = (int) Math.floor(p.z());
            if (Phys.top(w.get(x, y - 1, z)) <= 0 || Phys.shape(w.get(x, y, z)).length > 0 || Phys.shape(w.get(x, y + 1, z)).length > 0)
                r.err("respawn/spawn point not standable: " + p);
        }
        r.line("");
        r.line(String.format(Locale.ROOT, "SUMMARY steps %d, footholds %d (tiny objects %d), decoys %d, bypasses %d, kill overlaps %d, catch ledges %d (info)",
                jumps, footholds, tiny, decoys, bypasses, killOverlaps, ledges));
        r.line("RESULT: " + (r.errors == 0 ? "PASS" : "FAIL") + "  errors " + r.errors + "  warnings " + r.warnings);
        return r;
    }

    private static double avg(Report r, String prefix) {
        double s = 0; int n = 0;
        for (Map.Entry<String, Double> e : r.attempts.entrySet()) if (e.getKey().startsWith(prefix)) { s += e.getValue(); n++; }
        return n == 0 ? 0 : s / n;
    }

    /**
     * Timed runs: for every start tick in one cycle, can a player leaving the solid foothold before the run cross
     * every timed foothold while it is on (airtime from the simulation, minimum stay = the next jump's first good
     * tick + 1, waiting allowed while the block stays on)? Returns the fraction of good start ticks (1 when none).
     */
    private static double phaseCheck(Report r, Ob o, List<JumpSim.Res> res, List<int[]> idx, Tier tier) {
        if (o.timed.isEmpty()) return 1;
        Map<Long, CourseModel.Timed> group = new HashMap<>();
        for (CourseModel.Timed t : o.timed) for (CourseModel.BlockPos p : t.positions()) group.put(W.key(p.x(), p.y(), p.z()), t);
        // step index by target node
        Map<Integer, Integer> stepInto = new HashMap<>();
        for (int s = 0; s < idx.size(); s++) stepInto.put(idx.get(s)[1], s);
        double worstFrac = 1;
        for (int i = 1; i < o.route.size(); i++) {
            if (o.route.get(i).t != Ob.T.TIMED || o.route.get(i - 1).t == Ob.T.TIMED) continue;
            int j = i;
            while (j + 1 < o.route.size() && o.route.get(j + 1).t == Ob.T.TIMED) j++;
            List<CourseModel.Timed> g = new ArrayList<>();
            List<Integer> air = new ArrayList<>(), stay = new ArrayList<>();
            boolean ok = true;
            for (int k = i; k <= j; k++) {
                int[] c0 = o.route.get(k).cells.get(0);
                CourseModel.Timed t = group.get(W.key(c0[0], c0[1], c0[2]));
                Integer sIn = stepInto.get(k), sOut = stepInto.get(k + 1);
                if (t == null || sIn == null || sOut == null) { ok = false; break; }
                JumpSim.Res in = res.get(sIn), out = res.get(sOut);
                g.add(t);
                air.add(in.airMax == 0 ? 12 : (in.airMin + in.airMax + 1) / 2);
                stay.add(Math.max(2, (out.firstJump >= 99 ? 4 : out.firstJump) + 1));
            }
            if (!ok) { r.err(o.id + ": timed run " + i + ".." + j + " could not be analysed"); continue; }
            int P = 1;
            for (CourseModel.Timed t : g) P = lcm(P, t.periodTicks());
            P = Math.min(P, 7200);
            int good = 0;
            for (int tau = 0; tau < P; tau++) {
                long t = tau;
                boolean pass = true;
                for (int k = 0; k < g.size() && pass; k++) {
                    long arrive = t + air.get(k);
                    long depart = arrive + stay.get(k);
                    if (k + 1 < g.size()) {
                        long d = -1;
                        for (long dd = depart; dd < depart + g.get(k).periodTicks(); dd++) {
                            if (!g.get(k).onAt(dd)) break;
                            if (g.get(k + 1).onAt(dd + air.get(k + 1))) { d = dd; break; }
                        }
                        if (d < 0) { pass = false; break; }
                        depart = d;
                    }
                    for (long q = arrive; q <= depart; q++) if (!g.get(k).onAt(q)) { pass = false; break; }
                    t = depart;
                }
                if (pass) good++;
            }
            double frac = good / (double) P;
            double win = frac * g.get(0).periodTicks();
            int need = (tier == Tier.SILVER || tier == Tier.GOLD || tier == Tier.HARD) ? 6 : 10;
            r.line(String.format(Locale.ROOT, "  %s timed run %d..%d: %d good start ticks per %d-tick cycle (window %.0f)", o.id, i, j, good, P, win));
            if (win < need) r.err(o.id + ": timed run " + i + ".." + j + " has a phase window of " + f(win) + " ticks < " + need);
            worstFrac = Math.min(worstFrac, Math.min(1, win / 20.0));
        }
        return worstFrac;
    }


    // ------------------------------------------------------------------ moving parts (PLAN_v3 4.2)

    /** Safe consecutive start ticks a hazard step needs per tier (rule 2). */
    static int moverNeed(Tier t) {
        return switch (t) { case GOLD, HARD -> 6; case BRONZE, SILVER -> 10; default -> 15; };
    }

    /** Fail chance of picking the right moment out of a W-tick safe window (telegraphed, waiting allowed). */
    static double moverFail(double win) { return Math.min(0.6, 2.5 / Math.max(1, win)); }

    /**
     * Hazard sweep: every route foothold needs a safe waiting cell (no hazard touches a runner standing on it during
     * the whole period); every step's flight (straight line from the departure edge to the landing cell, vertical
     * profile from the jump physics, airtime = the simulation's longest, 6 ticks of run-up and 6 of landing) is tested
     * against every mover for every start tick of the common period, with the runtime's display lag (t - 1 .. t + 1)
     * and the runtime's body test. The longest run of consecutive safe start ticks must reach {@link #moverNeed}.
     * Returns the pass factor for the attempts estimate.
     */
    static double moverCheck(Report r, Phys.World wd, Ob o, List<JumpSim.Res> res, List<int[]> idx, Tier tier) {
        if (o.movers.isEmpty()) return 1;
        int P = 1;
        for (CourseModel.Mover mv : o.movers) P = lcm(P, Math.max(1, mv.periodTicks()));
        P = Math.min(P, 2400);
        for (int i = 0; i < o.route.size(); i++) {
            Ob.Node n = o.route.get(i);
            boolean any = false;
            for (int[] c : n.cells) {
                double fy = feetCell(wd, c, n);
                boolean hit = false;
                for (int t = 0; t < P && !hit; t++) for (CourseModel.Mover mv : o.movers)
                    if (mv.hitsPlayer(t, c[0] + 0.5, fy, c[2] + 0.5)) { hit = true; break; }
                if (!hit) { any = true; break; }
            }
            if (!any) r.err(o.id + ": route foothold " + i + " at " + c(n.cells.get(0)) + " has no safe waiting cell (a mover reaches every cell)");
        }
        double factor = 1;
        for (int s = 0; s < res.size(); s++) {
            JumpSim.Res jr = res.get(s);
            Ob.Node a = o.route.get(idx.get(s)[0]), b = o.route.get(idx.get(s)[1]);
            int[][] pr = JumpSim.nearest(a.cells, b.cells);
            double ax = pr[0][0] + 0.5, az = pr[0][2] + 0.5, bxx = pr[1][0] + 0.5, bzz = pr[1][2] + 0.5;
            double ay = feetCell(wd, pr[0], a), by = feetCell(wd, pr[1], b);
            int air = Math.max(4, jr.airMax == 0 ? 12 : jr.airMax);
            List<double[]> path = new ArrayList<>();
            double dx = bxx - ax, dz = bzz - az, dl = Math.max(1e-6, Math.hypot(dx, dz));
            for (int k = 0; k < 6; k++) path.add(new double[]{ax - dx / dl * (0.45 - 0.08 * k), ay, az - dz / dl * (0.45 - 0.08 * k)});
            double vy = 0.42, y = ay;
            double[] ys = new double[air + 1];
            for (int k = 0; k <= air; k++) { ys[k] = y; y += vy; vy = (vy - 0.08) * 0.98; }
            double err = by - ys[air];
            for (int k = 0; k <= air; k++) {
                double u = k / (double) air;
                path.add(new double[]{ax + dx * u, Math.max(Math.min(ay, by), ys[k] + err * u), az + dz * u});
            }
            for (int k = 0; k < 6; k++) path.add(new double[]{bxx, by, bzz});
            boolean[] good = new boolean[P];
            boolean touched = false;
            for (int tau = 0; tau < P; tau++) {
                boolean ok = true;
                for (int k = 0; k < path.size() && ok; k++) {
                    double[] q = path.get(k);
                    double t = tau + k - 6;
                    for (CourseModel.Mover mv : o.movers) {
                        if (mv.hitsPlayer(t - 1, q[0], q[1], q[2]) || mv.hitsPlayer(t, q[0], q[1], q[2]) || mv.hitsPlayer(t + 1, q[0], q[1], q[2])) { ok = false; break; }
                    }
                }
                good[tau] = ok;
                if (!ok) touched = true;
            }
            if (!touched) continue;
            int best = 0, run = 0;
            for (int k = 0; k < 2 * P; k++) { if (good[k % P]) { run++; best = Math.max(best, Math.min(run, P)); } else run = 0; }
            int need = moverNeed(tier);
            r.line(String.format(Locale.ROOT, "  %s mover step %d: %d consecutive safe start ticks per %d-tick cycle (need %d)", o.id, s + 1, best, P, need));
            if (best < need) r.err(o.id + ": mover step " + (s + 1) + " has only " + best + " consecutive safe start ticks < " + need);
            factor *= 1 - moverFail(best);
        }
        return factor;
    }

    /** Dropping ledges: a runner landing on one must be able to jump off within its delay; it must come back. */
    static void crumbleCheck(Report r, Ob o, List<JumpSim.Res> res, List<int[]> idx, Tier tier) {
        if (o.crumbles.isEmpty()) return;
        int minDelay = (tier == Tier.GOLD || tier == Tier.HARD) ? 12 : 20;
        Map<Integer, Integer> stepFrom = new HashMap<>();
        for (int s = 0; s < idx.size(); s++) stepFrom.put(idx.get(s)[0], s);
        for (CourseModel.Crumble cr : o.crumbles) {
            if (cr.delayTicks() < minDelay) r.err(o.id + ": dropping ledge delay " + cr.delayTicks() + " < " + minDelay);
            if (cr.restoreTicks() < 20) r.err(o.id + ": dropping ledge restore " + cr.restoreTicks() + " < 20 ticks");
            Set<Long> cells = new HashSet<>();
            for (CourseModel.BlockPos p : cr.cells()) cells.add(W.key(p.x(), p.y(), p.z()));
            for (int i = 0; i < o.route.size(); i++) {
                boolean on = false;
                for (int[] c : o.route.get(i).cells) if (cells.contains(W.key(c[0], c[1], c[2]))) on = true;
                if (!on) continue;
                if (i == 0) { r.err(o.id + ": a dropping ledge is the start area"); continue; }
                Integer s = stepFrom.get(i);
                if (s == null) { r.err(o.id + ": dropping ledge is the last foothold"); continue; }
                int stay = Math.max(2, (res.get(s).firstJump >= 99 ? 4 : res.get(s).firstJump) + 2);
                if (stay > cr.delayTicks()) r.err(o.id + ": dropping ledge at foothold " + i + " needs " + stay + " ticks to leave > delay " + cr.delayTicks());
            }
        }
    }

    /**
     * Floating labels too close to each other (the owner's "overlapping checkpoint text"). Text displays grow upwards
     * from their position; obstacle name labels are two lines (name + medal).
     */
    static List<String> labelOverlaps(W w) {
        List<double[]> pts = new ArrayList<>();
        List<String> who = new ArrayList<>();
        for (Ob o : w.obs) if (o.built != null && o.built.label() != null) {
            CourseModel.Pos p = o.built.label();
            pts.add(new double[]{p.x(), p.y(), p.z(), 0.6}); who.add(o.id + " name");
        }
        for (CourseModel.Label l : w.model.labels) {
            int lines = 1;
            for (char ch : l.text().toCharArray()) if (ch == '\n') lines++;
            pts.add(new double[]{l.pos().x(), l.pos().y(), l.pos().z(), 0.3 * lines});
            who.add("label '" + l.text().replace('\n', '|') + "'");
        }
        List<String> out = new ArrayList<>();
        for (int i = 0; i < pts.size(); i++) for (int j = i + 1; j < pts.size(); j++) {
            double[] a = pts.get(i), b = pts.get(j);
            double h = Math.hypot(a[0] - b[0], a[2] - b[2]);
            boolean vy = a[1] < b[1] + b[3] + 0.15 && b[1] < a[1] + a[3] + 0.15;
            if (h < 2.5 && vy) out.add(who.get(i) + " and " + who.get(j) + " overlap (" + f(h) + " blocks apart)");
        }
        return out;
    }

    private static int lcm(int a, int b) { int x = a, y = b; while (y != 0) { int t = x % y; x = y; y = t; } return a / x * b; }

    private static List<CourseModel.Pos> spawnLike(W w) {
        List<CourseModel.Pos> l = new ArrayList<>(w.model.spawns);
        for (CourseModel.Hub h : w.model.hubs) { l.add(h.respawn()); l.add(h.bonusEntry()); }
        if (w.model.fork != null) l.add(w.model.fork);
        for (CourseModel.Obstacle o : w.model.obstacles) l.add(o.entry());
        return l;
    }

    private static boolean isLegitElsewhere(W w, long k) {
        for (Ob o : w.obs) if (o.safe.contains(k)) return true;
        return false;
    }

    private static Ob find(W w, String id) {
        for (Ob o : w.obs) if (o.id.equals(id)) return o;
        return null;
    }

    static int[] bbox(Ob o) {
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (Ob.Node n : o.route) for (int[] c : n.cells) for (int i = 0; i < 3; i++) { b[i] = Math.min(b[i], c[i]); b[i + 3] = Math.max(b[i + 3], c[i]); }
        return b;
    }

    /** Bounding box of the route without the start area (route[0]); used for kill columns. */
    static int[] bboxNoStart(Ob o) {
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (int i = 1; i < o.route.size(); i++) for (int[] c : o.route.get(i).cells) for (int k = 0; k < 3; k++) { b[k] = Math.min(b[k], c[k]); b[k + 3] = Math.max(b[k + 3], c[k]); }
        return b;
    }

    private static double segDist(double px, double pz, double[] s) {
        double ax = s[0], az = s[1], bx = s[2], bz = s[3];
        double dx = bx - ax, dz = bz - az, l2 = dx * dx + dz * dz;
        double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / l2));
        double qx = ax + t * dx - px, qz = az + t * dz - pz;
        return Math.sqrt(qx * qx + qz * qz);
    }

    static double feetCell(Phys.World wd, int[] c, Ob.Node n) {
        if (n.t == Ob.T.LADDER) return c[1];
        return c[1] + Math.max(0, Phys.top(wd.state(c[0], c[1], c[2])));
    }

    /** Count connected non-air, non-pad blocks (6-neighbour) from the cells, capped. */
    static int objectSize(W w, List<int[]> cells, int cap) {
        ArrayDeque<int[]> q = new ArrayDeque<>();
        Set<Long> seen = new HashSet<>();
        for (int[] c : cells) { q.add(c); seen.add(W.key(c[0], c[1], c[2])); }
        int n = 0;
        int[][] d6 = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!q.isEmpty() && n < cap) {
            int[] c = q.poll();
            n++;
            for (int[] d : d6) {
                int x = c[0] + d[0], y = c[1] + d[1], z = c[2] + d[2];
                long k = W.key(x, y, z);
                if (seen.contains(k)) continue;
                String s = w.get(x, y, z);
                if (Bs.isAir(s) || W.PAD.equals(s)) continue;
                seen.add(k);
                q.add(new int[]{x, y, z});
            }
        }
        return n;
    }

    static String f(double d) { return String.format(Locale.ROOT, "%.1f", d); }

    static String c(int[] p) { return p[0] + "," + p[1] + "," + p[2]; }
}
