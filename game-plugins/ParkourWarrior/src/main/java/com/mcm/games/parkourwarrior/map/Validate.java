package com.mcm.games.parkourwarrior.map;

import com.mcm.games.parkourwarrior.CourseModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Offline validators for the generated course. Jump model (vanilla sprint-jump, conservative):
 * flat / drop gap <= 4, rise of up to 1 block allows gap <= 3, no rise > 1 without ladder or slime, 2 blocks of
 * headroom at takeoff, landing and along the arc, a block 2 above the takeoff feet (head-hitter) forbids rising
 * and limits the gap to 3, a neo limits the gap to 3. Ladder: catch gap <= 3 with a rung within reach; leave
 * from the top to a landing no higher than the top rung + 1 with gap <= 2. Slime: bounce rise <= drop - 1.
 * Easy table: gap <= 3 flat/drop, <= 2 with rise, ladder catch <= 2, ladder exit gap <= 1.
 */
final class Validate {
    private Validate() {}

    static final class Report {
        final StringBuilder text = new StringBuilder();
        int errors, warnings;
        final Map<String, Double> diff = new HashMap<>();
        void err(String s) { errors++; text.append("  ERROR ").append(s).append('\n'); }
        void warn(String s) { warnings++; text.append("  warn  ").append(s).append('\n'); }
        void line(String s) { text.append(s).append('\n'); }
    }

    private static boolean free(W w, int x, int y, int z, Map<Long, String> timed) {
        String s = timed.containsKey(W.key(x, y, z)) ? timed.get(W.key(x, y, z)) : w.get(x, y, z);
        return Bs.passable(s);
    }

    private static String at(W w, int x, int y, int z, Map<Long, String> timed) {
        String t = timed.get(W.key(x, y, z));
        return t != null ? t : w.get(x, y, z);
    }

    static double gap(int[] a, int[] b) {
        double gx = Math.max(0, Math.abs(a[0] - b[0]) - 1), gz = Math.max(0, Math.abs(a[2] - b[2]) - 1);
        return Math.sqrt(gx * gx + gz * gz);
    }

    record Jump(double gap, double rise, String kind, boolean ok, boolean easyOk, String why, double hard) {}

    static Report run(W w) {
        Report r = new Report();
        Map<Long, String> timed = new HashMap<>();
        for (Ob o : w.obs) timed.putAll(o.timedPreview);
        r.line("Parkour Warrior course validation  (seed " + w.model.seed + ")");
        r.line("blocks: " + w.b.size() + ", obstacles: " + w.obs.size() + ", timed groups: " + w.model.timed.size());

        int jumps = 0, easyChecked = 0, decoys = 0, ledges = 0, tiny = 0, footholds = 0;
        Set<String> ids = new HashSet<>(), names = new HashSet<>();
        for (Ob o : w.obs) {
            StringBuilder head = new StringBuilder();
            List<Jump> js = new ArrayList<>();
            if (!ids.add(o.id)) r.err(o.id + ": duplicate id");
            if (!names.add(o.name)) r.err(o.id + ": duplicate name " + o.name);
            if (o.padBox == null) { r.err(o.id + ": no completion pad"); continue; }
            // pad material
            for (int x = o.padBox.minX(); x <= o.padBox.maxX(); x++) for (int z = o.padBox.minZ(); z <= o.padBox.maxZ(); z++)
                if (!W.PAD.equals(w.get(x, o.padBox.minY(), z))) r.err(o.id + ": pad block missing at " + x + "," + o.padBox.minY() + "," + z);
            int minFeet = o.minFeet();
            if (o.killY >= minFeet) r.err(o.id + ": killY " + o.killY + " not below lowest foothold feet " + minFeet);
            double drop = 0;
            for (int i = 1; i < o.route.size(); i++) {
                Ob.Node a = o.route.get(i - 1), b = o.route.get(i);
                Jump j = jump(w, timed, a, b, drop);
                if (b.t == Ob.T.SLIME) drop = feetOf(w, timed, a) - feetOf(w, timed, b);
                js.add(j);
                jumps++;
                if (!j.ok) r.err(o.id + " jump " + i + " (" + j.kind + "): " + j.why);
                if (o.easy) { easyChecked++; if (!j.easyOk && j.ok) r.err(o.id + " jump " + i + " outside EASY table: " + j.kind + " gap " + f(j.gap) + " rise " + f(j.rise)); }
            }
            // foothold objects: each area node must belong to a solid object clearly bigger than the foothold
            for (Ob.Node n : o.route) {
                if (n.t == Ob.T.LADDER || n.t == Ob.T.TIMED) continue;
                footholds++;
                int obj = objectSize(w, n.cells, 400);
                if (obj < Math.max(8, n.cells.size() * 1.5 + 4)) { tiny++; r.err(o.id + ": foothold at " + c(n.cells.get(0)) + " belongs to an object of only " + obj + " blocks (plain cube?)"); }
            }
            // decoys and catch ledges
            Set<Long> legit = new HashSet<>(o.safe);
            for (Ob.Node n : o.route) for (int[] c : n.cells) legit.add(W.key(c[0], c[1], c[2]));
            int[] bb = bbox(o);
            int maxFeet = o.maxFeet();
            List<double[]> segs = new ArrayList<>();
            for (int i = 1; i < o.route.size(); i++) {
                int[][] p = nearest(o.route.get(i - 1), o.route.get(i));
                segs.add(new double[]{p[0][0] + .5, p[0][2] + .5, p[1][0] + .5, p[1][2] + .5,
                        Math.min(feetCell(w, timed, p[0], o.route.get(i - 1)), feetCell(w, timed, p[1], o.route.get(i))),
                        Math.max(feetCell(w, timed, p[0], o.route.get(i - 1)), feetCell(w, timed, p[1], o.route.get(i)))});
            }
            int od = 0, ol = 0;
            for (int x = bb[0] - 3; x <= bb[3] + 3; x++) for (int z = bb[2] - 3; z <= bb[5] + 3; z++) for (int y = o.killY - 1; y <= maxFeet + 1; y++) {
                String s = at(w, x, y, z, timed);
                if (Bs.isAir(s)) continue;
                double top = Bs.standTop(s);
                if (top <= 0) continue;
                if (!free(w, x, y + 1, z, timed) || !free(w, x, y + 2, z, timed)) continue;
                long k = W.key(x, y, z);
                if (legit.contains(k) || w.frameWalk.contains(k) || w.padCells.contains(k)) continue;
                if (isLegitElsewhere(w, k)) continue;
                double feet = y + top;
                if (feet < o.killY) continue;
                boolean dec = false;
                for (double[] sg : segs) {
                    if (feet < sg[4] - 0.01 || feet > sg[5] + 0.01) continue;
                    if (segDist(x + .5, z + .5, sg) <= 2.0) { dec = true; break; }
                }
                if (dec) { od++; if (od <= 4) r.err(o.id + ": decoy standable block at foothold height near the route: " + x + "," + y + "," + z + " " + Bs.id(s)); }
                else { ol++; if (ol <= 2) r.warn(o.id + ": catch ledge above kill-Y (not on route) " + x + "," + y + "," + z + " " + Bs.id(s)); }
            }
            if (od > 4) r.err(o.id + ": ... " + (od - 4) + " more decoys");
            decoys += od; ledges += ol;
            for (CourseModel.Timed t : o.timed) for (CourseModel.BlockPos p : t.positions())
                if (!Bs.isAir(w.get(p.x(), p.y(), p.z()))) r.err(o.id + ": timed position not air in the static store " + p);
            // length
            int sx = o.wx(0, 0), sz = o.wz(0, 0), ex = o.wx(o.endX, o.endZ), ez = o.wz(o.endX, o.endZ);
            double span = Math.hypot(ex - sx, ez - sz) - 3;
            if (o.kind != CourseModel.Kind.ENDING && (span < 15 || span > 35)) r.err(o.id + ": length " + f(span) + " outside 15..35");
            if (o.kind == CourseModel.Kind.ENDING && (span < 12 || span > 35)) r.err(o.id + ": ending length " + f(span) + " outside 12..35");
            double maxGap = 0, maxRise = 0, hard = 0;
            List<Double> hs = new ArrayList<>();
            for (Jump j : js) { maxGap = Math.max(maxGap, j.gap); maxRise = Math.max(maxRise, j.rise); hs.add(j.hard); }
            hs.sort((p, q) -> Double.compare(q, p));
            for (int i = 0; i < Math.min(4, hs.size()); i++) hard += hs.get(i);
            hard /= Math.max(1, Math.min(4, hs.size()));
            r.diff.put(o.id, hard);
            int up = o.maxFeet() - o.minFeet();
            head.append(String.format(Locale.ROOT, "%-10s %-24s %-7s len %4.1f  jumps %2d  maxGap %.1f  climb %2d  killY %d  diff %.2f%s%s",
                    o.id, o.name, o.idea, span, js.size(), maxGap, up, o.killY, hard, o.easy ? "  [easy table]" : "",
                    o.difficultyTag.isEmpty() ? "" : "  sign " + o.difficultyTag));
            r.text.insert(r.text.length(), "");
            r.line(head.toString());
        }

        // ---- course-level checks
        r.line("");
        r.line("== course checks");
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
        // difficulty ramp
        double[] mainD = new double[6], bonD = new double[6];
        for (int s = 1; s <= 5; s++) {
            for (int i = 1; i <= 3; i++) { mainD[s] += r.diff.getOrDefault("M" + s + "-" + i, 0.0) / 3; bonD[s] += r.diff.getOrDefault("B" + s + "-" + i, 0.0) / 3; }
            r.line(String.format(Locale.ROOT, "stage %d difficulty: main %.2f  bonus %.2f", s, mainD[s], bonD[s]));
            if (bonD[s] <= mainD[s]) r.err("bonus path " + s + " not harder than main stage " + s);
            if (s > 1 && bonD[s] < bonD[s - 1] - 0.05) r.err("bonus path " + s + " easier than bonus path " + (s - 1));
        }
        if (mainD[5] <= mainD[1]) r.err("main line does not ramp up (stage 5 <= stage 1)");
        double hardEnd = (r.diff.getOrDefault("E-HARD-1", 0.0) + r.diff.getOrDefault("E-HARD-2", 0.0)) / 2;
        double medEnd = (r.diff.getOrDefault("E-MEDIUM-1", 0.0) + r.diff.getOrDefault("E-MEDIUM-2", 0.0)) / 2;
        double easyEnd = (r.diff.getOrDefault("E-EASY-1", 0.0) + r.diff.getOrDefault("E-EASY-2", 0.0)) / 2;
        r.line(String.format(Locale.ROOT, "endings difficulty: easy %.2f  medium %.2f  hard %.2f", easyEnd, medEnd, hardEnd));
        double maxOther = 0;
        for (Map.Entry<String, Double> e : r.diff.entrySet()) if (!e.getKey().startsWith("E-HARD")) maxOther = Math.max(maxOther, e.getValue());
        if (hardEnd <= maxOther) r.err("Hard ending (" + f(hardEnd) + ") is not the hardest thing on the map (max other " + f(maxOther) + ")");
        if (!(easyEnd < medEnd && medEnd < hardEnd)) r.err("endings not ordered easy < medium < hard");
        // stairs
        List<String> se = Stairs.validate(w.b, w.lanes);
        r.line("staircases: " + w.lanes.size() + " lanes, problems " + se.size());
        for (String s : se) r.err("stairs: " + s);
        // bounds, materials
        w.model.recomputeBounds();
        r.line("bounds: x " + w.model.minX + ".." + w.model.maxX + "  y " + w.model.minY + ".." + w.model.maxY + "  z " + w.model.minZ + ".." + w.model.maxZ);
        if (w.model.minY < 40 || w.model.maxY > 250) r.err("course outside y 40..250");
        Set<String> mats = new HashSet<>();
        w.b.forEach((x, y, z, s) -> mats.add(Bs.id(s)));
        r.line("distinct block types: " + mats.size());
        // spawns etc.
        if (w.model.spawns.isEmpty()) r.err("no spawns");
        if (w.model.hubs.size() != 5) r.err("expected 5 hubs");
        if (w.model.finishes.size() != 3) r.err("expected 3 finishes");
        if (w.model.fork == null) r.err("no fork");
        for (CourseModel.Pos p : spawnLike(w)) {
            int x = (int) Math.floor(p.x()), y = (int) Math.floor(p.y()), z = (int) Math.floor(p.z());
            if (Bs.standTop(w.get(x, y - 1, z)) < 0 || !Bs.passable(w.get(x, y, z)) || !Bs.passable(w.get(x, y + 1, z)))
                r.err("respawn/spawn point not standable: " + p);
        }
        r.line("");
        r.line(String.format(Locale.ROOT, "SUMMARY jumps %d (easy-table checked %d), footholds %d (tiny objects %d), decoys %d, catch ledges %d (warning)",
                jumps, easyChecked, footholds, tiny, decoys, ledges));
        r.line("RESULT: " + (r.errors == 0 ? "PASS" : "FAIL") + "  errors " + r.errors + "  warnings " + r.warnings);
        return r;
    }

    private static List<CourseModel.Pos> spawnLike(W w) {
        List<CourseModel.Pos> l = new ArrayList<>(w.model.spawns);
        for (CourseModel.Hub h : w.model.hubs) { l.add(h.respawn()); l.add(h.bonusEntry()); }
        if (w.model.fork != null) l.add(w.model.fork);
        for (CourseModel.Obstacle o : w.model.obstacles) l.add(o.entry());
        return l;
    }

    private static boolean isLegitElsewhere(W w, long k) {
        for (Ob o : w.obs) {
            if (o.safe.contains(k)) return true;
        }
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

    private static double segDist(double px, double pz, double[] s) {
        double ax = s[0], az = s[1], bx = s[2], bz = s[3];
        double dx = bx - ax, dz = bz - az, l2 = dx * dx + dz * dz;
        double t = l2 == 0 ? 0 : Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / l2));
        double qx = ax + t * dx - px, qz = az + t * dz - pz;
        return Math.sqrt(qx * qx + qz * qz);
    }

    private static double feetCell(W w, Map<Long, String> timed, int[] c, Ob.Node n) {
        if (n.t == Ob.T.LADDER) return c[1];
        double t = Bs.standTop(at(w, c[0], c[1], c[2], timed));
        return c[1] + Math.max(0, t);
    }

    private static double feetOf(W w, Map<Long, String> timed, Ob.Node n) {
        double m = -1e9;
        for (int[] c : n.cells) m = Math.max(m, feetCell(w, timed, c, n));
        return m;
    }

    /** Nearest pair of cells (by gap, then by |dy|). */
    static int[][] nearest(Ob.Node a, Ob.Node b) {
        int[][] best = null;
        double bg = 1e9;
        for (int[] p : a.cells) for (int[] q : b.cells) {
            double g = gap(p, q) * 100 + Math.abs(p[1] - q[1]) * 0.01;
            if (g < bg) { bg = g; best = new int[][]{p, q}; }
        }
        return best;
    }

    private static Jump jump(W w, Map<Long, String> timed, Ob.Node a, Ob.Node b, double lastDrop) {
        // ladder catch
        if (b.t == Ob.T.LADDER) {
            double best = 1e9; int[] pa = null, pb = null;
            double fa = 0;
            for (int[] p : a.cells) for (int[] q : b.cells) {
                double feet = feetCell(w, timed, p, a);
                if (a.t == Ob.T.LADDER) feet = p[1];
                if (q[1] < feet - 3 || q[1] > feet + 1) continue;
                double g = gap(p, q);
                if (g < best) { best = g; pa = p; pb = q; fa = feet; }
            }
            if (pa == null) return new Jump(0, 0, "ladder catch", false, false, "no rung within reach of the takeoff", 9);
            String back = backing(w, b);
            if (back != null) return new Jump(best, 0, "ladder catch", false, false, back, 9);
            boolean ok = best <= 3.0001, easy = best <= 2.0001;
            return new Jump(best, 0, "ladder catch", ok, easy, "ladder catch gap " + f(best), best + 1.0);
        }
        if (a.t == Ob.T.LADDER) {
            int yt = Integer.MIN_VALUE;
            for (int[] p : a.cells) yt = Math.max(yt, p[1]);
            double best = 1e9; double rise = 0;
            for (int[] p : a.cells) if (p[1] == yt) for (int[] q : b.cells) {
                double feet = feetCell(w, timed, q, b);
                if (feet > yt + 1.01) continue;
                double g = gap(p, q);
                if (g < best) { best = g; rise = feet - (yt + 1); }
            }
            if (best > 1e8) return new Jump(0, 0, "ladder exit", false, false, "landing above the top rung + 1", 9);
            return new Jump(best, rise, "ladder exit", best <= 2.0001, best <= 1.0001, "ladder exit gap " + f(best), best + 0.5);
        }
        int[][] p = nearest(a, b);
        double fa = feetCell(w, timed, p[0], a), fb = feetCell(w, timed, p[1], b);
        double g = gap(p[0], p[1]);
        double rise = fb - fa;
        String kind = a.t == Ob.T.SLIME ? "slime bounce" : b.neo ? "neo" : "jump";
        if (Bs.standTop(at(w, p[1][0], p[1][1], p[1][2], timed)) < 0) return new Jump(g, rise, kind, false, false, "no block to land on at " + c(p[1]), 9);
        // headroom
        for (int[] cc : p) for (int h = 1; h <= 2; h++) {
            int yy = (int) Math.floor((cc == p[0] ? fa : fb)) + h - 1;
            if (!free(w, cc[0], yy, cc[2], timed)) return new Jump(g, rise, kind, false, false, "no 2-block headroom at " + c(cc), 9);
        }
        boolean head = !free(w, p[0][0], (int) Math.floor(fa) + 2, p[0][2], timed);
        double lim = 4.0, elim = 3.0;
        String why = kind + " gap " + f(g) + " rise " + f(rise);
        if (a.t == Ob.T.SLIME) {
            double maxRise = Math.max(1.0, lastDrop - 1);
            if (rise > maxRise + 1e-6) return new Jump(g, rise, kind, false, false, "slime bounce rise " + f(rise) + " > " + f(maxRise) + " (drop " + f(lastDrop) + ")", 9);
            lim = 3; elim = 2;
        } else {
            if (rise > 1.0001) return new Jump(g, rise, kind, false, false, "rise " + f(rise) + " > 1 without ladder/slime", 9);
            if (rise > 0.01) { lim = 3; elim = 2; }
        }
        if (head) {
            if (rise > 0.01 && a.t != Ob.T.SLIME) return new Jump(g, rise, kind, false, false, "head-hitter above takeoff but the jump rises", 9);
            lim = Math.min(lim, 3); elim = Math.min(elim, 2); kind = "head-hitter";
        }
        if (b.neo) { lim = Math.min(lim, 3); elim = Math.min(elim, 2); }
        if (!b.neo) {
            // arc clearance on the straight line
            double ax = p[0][0] + .5, az = p[0][2] + .5, bx = p[1][0] + .5, bz = p[1][2] + .5;
            int steps = (int) Math.ceil(Math.hypot(bx - ax, bz - az) * 4);
            for (int i = 1; i < steps; i++) {
                double t = i / (double) steps;
                int cx = (int) Math.floor(ax + (bx - ax) * t), cz = (int) Math.floor(az + (bz - az) * t);
                if ((cx == p[0][0] && cz == p[0][2]) || (cx == p[1][0] && cz == p[1][2])) continue;
                double hh;
                if (a.t == Ob.T.SLIME) hh = Math.max(fa, fb) + 0.2;
                else {
                    double sh = Math.max(0, Math.hypot(bx - ax, bz - az) * t - 0.5);
                    hh = Math.max(Math.min(fa, fb), fa + traj(sh / 0.33));
                    if (head) hh = Math.min(hh, Math.floor(fa) + 0.2);
                }
                int y0 = (int) Math.floor(hh), y1 = (int) Math.floor(hh + 1.79);
                boolean blocked = false;
                for (int yy = y0; yy <= y1; yy++) if (!free(w, cx, yy, cz, timed)) blocked = true;
                if (blocked)
                    return new Jump(g, rise, kind, false, false, "arc blocked at " + cx + "," + y0 + "," + cz + " (" + Bs.id(at(w, cx, y0, cz, timed)) + "/" + Bs.id(at(w, cx, y0 + 1, cz, timed)) + ")", 9);
            }
        }
        boolean ok = g <= lim + 1e-4, easy = g <= elim + 1e-4;
        double hard = g + (rise > 0.01 ? 1.0 : 0) + (head ? 1.0 : 0) + (b.neo ? 1.5 : 0) + (b.t == Ob.T.TIMED ? 1.0 : 0)
                + (a.t == Ob.T.ICE || b.t == Ob.T.ICE ? 0.5 : 0) + (b.cells.size() == 1 ? 1.0 : b.cells.size() == 2 ? 0.5 : 0);
        if (a.t == Ob.T.ICE) kind += " (ice takeoff)";
        if (b.t == Ob.T.TIMED) kind += " (timed)";
        return new Jump(g, rise, kind, ok, easy, ok ? why : why + " exceeds limit " + f(lim), hard);
    }

    private static String backing(W w, Ob.Node lad) {
        for (int[] c : lad.cells) {
            String s = w.get(c[0], c[1], c[2]);
            if (s == null) return "ladder block missing at " + c(c);
            if (Bs.id(s).equals("vine")) continue;
            String f = Bs.prop(s, "facing");
            int dx = 0, dz = 0;
            switch (f == null ? "" : f) {
                case "north": dz = 1; break;
                case "south": dz = -1; break;
                case "east": dx = -1; break;
                case "west": dx = 1; break;
                default: return "ladder without facing at " + c(c);
            }
            if (!Bs.fullSolid(w.get(c[0] + dx, c[1], c[2] + dz))) return "ladder at " + c(c) + " has no solid backing";
        }
        return null;
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

    /** Height above takeoff of a sprint jump after t ticks (vy0 0.42, gravity 0.08, drag 0.98). */
    static double traj(double t) {
        double y = 0, v = 0.42;
        int n = (int) Math.floor(t);
        for (int i = 0; i < n; i++) { y += v; v = (v - 0.08) * 0.98; }
        return y + v * (t - n);
    }

    static String f(double d) { return String.format(Locale.ROOT, "%.1f", d); }

    static String c(int[] p) { return p[0] + "," + p[1] + "," + p[2]; }
}
