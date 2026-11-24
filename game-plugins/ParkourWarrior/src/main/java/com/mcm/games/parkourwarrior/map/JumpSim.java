package com.mcm.games.parkourwarrior.map;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Proves single route steps with {@link Phys}: for a takeoff node A and target node(s) it searches start
 * alignments x jump ticks x steering strategies and reports the <b>timing window</b> = number of distinct jump ticks
 * (or ladder release ticks) that land and stay on the target. Model of the player (research Part 3.2):
 * <ul>
 *   <li>starts at rest at the back of A on the line towards the target (maximum run-up the foothold offers), sprint
 *   key and W held (input 0.98, no 45-degree strafe), facing the target or a fixed outward angle until a switch tick;</li>
 *   <li>8 start alignments 0.035 b apart (one tick of travel at sprint speed): window worst / best over them;</li>
 *   <li>a jump only counts when the player box still overlaps A at the jump tick unless {@code allowLate} (gold /
 *   hard: the one-tick-after-the-edge jump the Y-first collision allows);</li>
 *   <li>success = first ground contact on the target and still standing on it 8 ticks later (aiming back at the
 *   cell centre, letting go of W when close); ladder target = feet inside a ladder cell;</li>
 *   <li>slime nodes are pass-through: the player releases jump (1.21.1 jump overrides the bounce), bounces and
 *   steers to the next node; the whole A -> slime -> B sequence is one window;</li>
 *   <li>ladder takeoff: the player climbs k ticks pushing into the ladder, then turns to the target holding W (and
 *   optionally jump); the window is the number of release ticks k that work;</li>
 *   <li>falling below the obstacle's kill-Y or into ANY obstacle's kill column below its kill-Y fails the jump.</li>
 * </ul>
 */
final class JumpSim {
    private JumpSim() {}

    static boolean TRACE = false;

    /** Kill volumes: own kill-Y plus every obstacle's area column (minX, minZ, maxX, maxZ, killY). */
    static final class Kill {
        final double ownKillY;
        final List<double[]> areas;
        final List<String> ids;
        Kill(double own, List<double[]> areas, List<String> ids) { this.ownKillY = own; this.areas = areas; this.ids = ids; }
        String hit(double x, double y, double z) {
            if (y < ownKillY) return "own kill-Y";
            int bx = (int) Math.floor(x), bz = (int) Math.floor(z);
            for (int i = 0; i < areas.size(); i++) {
                double[] a = areas.get(i);
                if (bx >= a[0] && bx <= a[2] && bz >= a[1] && bz <= a[3] && y < a[4]) return "kill volume of " + ids.get(i);
            }
            return null;
        }
    }

    static final class Target {
        final Set<Long> cells = new HashSet<>();
        final List<int[]> list = new ArrayList<>();
        final boolean ladder, slime;
        Target(Ob.Node n) {
            ladder = n.t == Ob.T.LADDER;
            slime = n.t == Ob.T.SLIME;
            for (int[] c : n.cells) { cells.add(W.key(c[0], c[1], c[2])); list.add(c); }
        }
    }

    static final class Res {
        int worst = 0, best = 0, airMin = 99, airMax = 0, firstJump = 99;
        boolean curved, trivial;
        int deg, sw;
        double gap, dh;
        int wallLen;
        String kind = "jump", fail = "", note = "";
        boolean killHit;
        /** Landing states of successful runs (start states for chaining into the next step). */
        final List<Phys.P> arrivals = new ArrayList<>();
        int restWorst, restBest, chainWorst, chainBest;
        String describe() {
            return String.format(Locale.ROOT, "%s gap %.1f dh %+.2f%s", kind, gap, dh, curved ? " curved " + deg + "deg/sw" + sw : "");
        }
    }

    // ------------------------------------------------------------------ geometry helpers

    static double top(Phys.World wd, int[] c) { return c[1] + Phys.top(wd.state(c[0], c[1], c[2])); }

    /** Horizontal gap in air blocks between two cells (Euclidean on the per-axis gaps). */
    static double gap(int[] a, int[] b) {
        double gx = Math.max(0, Math.abs(a[0] - b[0]) - 1), gz = Math.max(0, Math.abs(a[2] - b[2]) - 1);
        return Math.sqrt(gx * gx + gz * gz);
    }

    static int[][] nearest(List<int[]> a, List<int[]> b) {
        int[][] best = null;
        double bg = 1e18;
        for (int[] p : a) for (int[] q : b) {
            double g = gap(p, q) * 1000 + Math.abs(p[1] - q[1]) + Math.hypot(p[0] - q[0], p[2] - q[2]) * 0.01;
            if (g < bg) { bg = g; best = new int[][]{p, q}; }
        }
        return best;
    }

    private static boolean overlapsCell(double x, double z, int[] c, double m) {
        return x + Phys.HW - m > c[0] && x - Phys.HW + m < c[0] + 1 && z + Phys.HW - m > c[2] && z - Phys.HW + m < c[2] + 1;
    }

    /** True when the player box at (x, z) overlaps an A cell whose top equals y (standing on A). */
    private static boolean onA(Phys.World wd, List<int[]> a, double x, double y, double z, double m) {
        for (int[] c : a) {
            if (Math.abs(top(wd, c) - y) > 1e-4) continue;
            // the real shape may be thinner than the cell (posts, panes): test the boxes
            for (double[] b : wd.boxes(c[0], c[1], c[2])) {
                if (Math.abs(b[4] - y) > 1e-4) continue;
                if (x + Phys.HW - m > b[0] && x - Phys.HW + m < b[3] && z + Phys.HW - m > b[2] && z - Phys.HW + m < b[5]) return true;
            }
        }
        return false;
    }

    private static double yawTo(double x, double z, double tx, double tz) {
        return Math.toDegrees(Math.atan2(-(tx - x), tz - z));
    }

    // ------------------------------------------------------------------ evaluation

    static final class Cfg {
        boolean allowLate;
        int aligns = 8;
        boolean wide; // search curved strategies even when the direct one works
        /** Aim point for the first target (the chosen run-up line), null = the nearest cell centre. */
        double[] aim0;
    }

    /** Evaluate A -> targets[0] (-> targets[1] ... when the earlier ones are slime). */
    static Res eval(Phys.World wd, Ob.Node aNode, List<Ob.Node> tnodes, Kill kill, Cfg cfg) { return eval(wd, aNode, tnodes, kill, cfg, null); }

    /**
     * As above; {@code prev} = landing states on A from the previous step: the player may also keep the momentum
     * and jump again from where it landed (chained). The window is the better of resetting (rest start) and chaining.
     */
    static Res eval(Phys.World wd, Ob.Node aNode, List<Ob.Node> tnodes, Kill kill, Cfg cfg, List<Phys.P> prev) {
        Res r = new Res();
        List<Target> tg = new ArrayList<>();
        for (Ob.Node n : tnodes) tg.add(new Target(n));
        Target first = tg.get(0), last = tg.get(tg.size() - 1);
        List<int[]> a = aNode.cells;
        Set<Long> aSet = new HashSet<>();
        for (int[] c : a) aSet.add(W.key(c[0], c[1], c[2]));
        int[][] nb = nearest(a, first.list);
        r.gap = gap(nb[0], nb[1]);
        double ta = aNode.t == Ob.T.LADDER ? nb[0][1] : top(wd, nb[0]);
        double tb = first.ladder ? nb[1][1] : top(wd, nb[1]);
        r.dh = tb - ta;
        r.kind = aNode.t == Ob.T.LADDER ? (first.ladder ? "ladder-to-ladder" : "ladder exit")
                : first.ladder ? "ladder catch" : first.slime ? "slime bounce" : aNode.t == Ob.T.ICE ? "ice jump"
                : first.list.size() == 1 && Phys.top(wd.state(nb[1][0], nb[1][1], nb[1][2])) != 1.0 ? "precision" : "jump";
        if (tg.size() > 1 && last.ladder) r.kind += "->ladder";
        if (aNode.t == Ob.T.LADDER) return evalLadder(wd, aNode, tg, aSet, kill, r);

        // ---- ground takeoff: candidate run-up lines (centre to centre, and edge to edge along the shortest way)
        List<double[]> lines = new ArrayList<>();   // {Ex, Ez, Px, Py, Pz}
        double[] P0 = aim(wd, nb[1], first);
        double[] ra = topRect(wd, nb[0]), rb = first.ladder ? new double[]{nb[1][0], nb[1][2], nb[1][0] + 1, nb[1][2] + 1} : topRect(wd, nb[1]);
        lines.add(new double[]{(ra[0] + ra[2]) / 2, (ra[1] + ra[3]) / 2, P0[0], P0[1], P0[2]});
        if (!first.ladder) {
            double[] e = closest(ra, rb);
            double ddx = e[2] - e[0], ddz = e[3] - e[1], dd = Math.hypot(ddx, ddz);
            if (dd > 1e-6) {
                ddx /= dd; ddz /= dd;
                double px = Math.max(rb[0] + 0.02, Math.min(rb[2] - 0.02, e[2] + ddx * 0.3));
                double pz = Math.max(rb[1] + 0.02, Math.min(rb[3] - 0.02, e[3] + ddz * 0.3));
                lines.add(new double[]{e[0], e[1], px, P0[1], pz});
            }
        }
        // the same lines shifted sideways (a wide takeoff lets the player run along its outer edge, e.g. for neos)
        int base = lines.size();
        for (int li = 0; li < base; li++) {
            double[] ln = lines.get(li);
            double dx = ln[2] - ln[0], dz = ln[4] - ln[1], dl = Math.hypot(dx, dz);
            if (dl < 1e-6) continue;
            double nx = -dz / dl, nz = dx / dl;
            for (double off : new double[]{-0.9, -0.45, 0.45, 0.9})
                lines.add(new double[]{ln[0] + nx * off, ln[1] + nz * off, ln[2] + nx * off, ln[3], ln[4] + nz * off});
        }
        List<List<Phys.P>> lineStarts = new ArrayList<>();
        double y0 = ta;
        int[] out = new int[3];
        int bw = -1, bb = -1;
        int[] bestS = {0, 0};
        List<Phys.P> bestStarts = null;
        double[] bestLine = null;
        for (double[] ln : lines) {
            double dx = ln[2] - ln[0], dz = ln[4] - ln[1], dl = Math.hypot(dx, dz);
            if (dl < 1e-6) { dx = 0; dz = 1; dl = 1; }
            dx /= dl; dz /= dl;
            double smax = 0;
            boolean any = false;
            for (double s = 0; s <= 5.0; s += 0.05) {
                double x = ln[0] - dx * s, z = ln[1] - dz * s;
                if (!onA(wd, a, x, y0, z, 0.02) || Phys.blocked(wd, x, y0 + 0.001, z)) { if (any) break; else continue; }
                any = true;
                smax = s;
            }
            List<Phys.P> starts = new ArrayList<>();
            for (int k = 0; k < cfg.aligns; k++) {
                double s = smax - k * 0.035;
                double x = ln[0] - dx * s, z = ln[1] - dz * s;
                if (s < -0.25 || !onA(wd, a, x, y0, z, 0.02)) break;
                starts.add(rest(wd, x, y0, z));
            }
            lineStarts.add(starts);
            if (starts.isEmpty()) continue;
            cfg.aim0 = new double[]{ln[2], ln[3], ln[4]};
            // walk-off (never jumping)
            boolean walk = false;
            for (Phys.P st : starts) if (run(wd, st, 0, 0, -1, a, tg, aSet, kill, cfg, out, null) == 1) { walk = true; break; }
            if (walk) {
                r.trivial = true; r.worst = r.best = 6; r.kind += " (walk/drop)";
                for (Phys.P st : starts) run(wd, st, 0, 0, -1, a, tg, aSet, kill, cfg, out, r.arrivals);
                cfg.aim0 = null;
                return r;
            }
            Res t1 = new Res();
            int[] w = windows(wd, starts, 0, 0, a, tg, aSet, kill, cfg, t1, 40);
            if (w[0] + w[1] > bw + bb) {
                bw = w[0]; bb = w[1]; bestStarts = starts; bestLine = ln;
                copyStats(t1, r);
            }
        }
        if (bestStarts == null) {
            Phys.P st = rest(wd, nb[0][0] + 0.5, y0, nb[0][2] + 0.5);
            bestStarts = new ArrayList<>(List.of(st));
            bestLine = lines.get(0);
            bw = bb = 0;
        }
        double[] curLine = bestLine;
        // curved strategies (neos, corners) when the straight ones are poor: every run-up line
        if (bb < 3 || bw < 2 || cfg.wide) {
            for (int li = 0; li < lines.size(); li++) {
                List<Phys.P> st = lineStarts.get(li);
                if (st.isEmpty()) continue;
                double[] ln = lines.get(li);
                cfg.aim0 = new double[]{ln[2], ln[3], ln[4]};
                List<int[]> cand = new ArrayList<>();
                List<Phys.P> one = List.of(st.get(st.size() / 2));
                Res tmp = new Res();
                for (int deg = -60; deg <= 60; deg += 5) {
                    if (deg == 0) continue;
                    for (int sw = 0; sw <= 14; sw += 2) {
                        int[] ww = windows(wd, one, deg, sw, a, tg, aSet, kill, cfg, tmp, 40);
                        if (ww[1] > 0) cand.add(new int[]{deg, sw, ww[1]});
                    }
                }
                cand.sort((p, q) -> q[2] - p[2]);
                for (int i = 0; i < Math.min(6, cand.size()); i++) {
                    Res t2 = new Res();
                    int[] ww = windows(wd, st, cand.get(i)[0], cand.get(i)[1], a, tg, aSet, kill, cfg, t2, 40);
                    if (ww[0] + ww[1] > bw + bb) {
                        bw = ww[0]; bb = ww[1]; bestS = new int[]{cand.get(i)[0], cand.get(i)[1]};
                        curLine = ln;
                        r.arrivals.clear();
                        copyStats(t2, r);
                    }
                }
            }
        }
        cfg.aim0 = new double[]{curLine[2], curLine[3], curLine[4]};
        double dx = curLine[2] - curLine[0], dz = curLine[4] - curLine[1];
        r.restWorst = bw; r.restBest = bb;
        // chained starts: a spread of the previous step's landing states
        List<Phys.P> chain = new ArrayList<>();
        if (prev != null && !prev.isEmpty()) {
            List<Phys.P> ps = new ArrayList<>(prev);
            final double fdx = dx, fdz = dz;
            ps.sort((p, q) -> Double.compare(p.x * fdx + p.z * fdz, q.x * fdx + q.z * fdz));
            int n = Math.min(8, ps.size());
            for (int i = 0; i < n; i++) chain.add(ps.get((int) Math.round(i * (ps.size() - 1) / Math.max(1.0, n - 1))));
        }
        if (!chain.isEmpty()) {
            int[] cw = {0, 0};
            Res t3 = null;
            for (double[] ln : lines) {
                cfg.aim0 = new double[]{ln[2], ln[3], ln[4]};
                for (int[] sg : bestS[0] != 0 ? new int[][]{bestS, {0, 0}} : new int[][]{{0, 0}}) {
                    Res t4 = new Res();
                    int[] c2 = windows(wd, chain, sg[0], sg[1], a, tg, aSet, kill, cfg, t4, 24);
                    if (t3 == null || c2[0] + c2[1] > cw[0] + cw[1]) { cw = c2; t3 = t4; }
                }
            }
            r.chainWorst = cw[0]; r.chainBest = cw[1];
            if (cw[1] > 0) {
                if (cw[0] + cw[1] >= bw + bb) r.arrivals.clear();
                r.arrivals.addAll(t3.arrivals);
                r.airMin = Math.min(r.airMin, t3.airMin); r.airMax = Math.max(r.airMax, t3.airMax);
                r.firstJump = Math.min(r.firstJump, t3.firstJump);
            }
            bw = Math.max(bw, cw[0]);
            bb = Math.max(bb, cw[1]);
        }
        cfg.aim0 = null;
        r.worst = bw; r.best = bb;
        if (bestS[0] != 0) { r.curved = true; r.deg = bestS[0]; r.sw = bestS[1]; }
        r.wallLen = wallLen(wd, nb[0], nb[1], ta);
        return r;
    }

    private static void copyStats(Res from, Res to) {
        to.airMin = from.airMin; to.airMax = from.airMax; to.firstJump = from.firstJump; to.killHit |= from.killHit;
        to.arrivals.clear(); to.arrivals.addAll(from.arrivals);
        if (to.fail.isEmpty()) to.fail = from.fail;
    }

    /** Horizontal rect {x0, z0, x1, z1} of the highest collision box of a cell (the surface stood on). */
    static double[] topRect(Phys.World wd, int[] c) {
        double best = -1; double[] r = null;
        for (double[] b : wd.boxes(c[0], c[1], c[2])) {
            if (b[4] > best + 1e-9) { best = b[4]; r = new double[]{b[0], b[2], b[3], b[5]}; }
            else if (Math.abs(b[4] - best) < 1e-9) r = new double[]{Math.min(r[0], b[0]), Math.min(r[1], b[2]), Math.max(r[2], b[3]), Math.max(r[3], b[5])};
        }
        return r == null ? new double[]{c[0], c[2], c[0] + 1, c[2] + 1} : r;
    }

    /** Closest points between two rects: {ax, az, bx, bz}; on overlapping axes both use the middle of the overlap. */
    static double[] closest(double[] a, double[] b) {
        double[] o = new double[4];
        for (int ax = 0; ax < 2; ax++) {
            double lo = Math.max(a[ax], b[ax]), hi = Math.min(a[ax + 2], b[ax + 2]);
            if (lo <= hi) { o[ax] = o[ax + 2] = (lo + hi) / 2; }
            else if (a[ax + 2] < b[ax]) { o[ax] = a[ax + 2]; o[ax + 2] = b[ax]; }
            else { o[ax] = a[ax]; o[ax + 2] = b[ax + 2]; }
        }
        return o;
    }

    /** Point to aim at on a target cell (centre of its top; in front of the plate for ladders). */
    static double[] aim(Phys.World wd, int[] c, Target t) {
        if (t.ladder) return new double[]{c[0] + 0.5, c[1] + 0.5, c[2] + 0.5};
        double best = 0; double[] bb = null;
        for (double[] b : wd.boxes(c[0], c[1], c[2])) if (bb == null || b[4] > best) { best = b[4]; bb = b; }
        if (bb == null) return new double[]{c[0] + 0.5, c[1] + 1, c[2] + 0.5};
        return new double[]{(bb[0] + bb[3]) / 2, bb[4], (bb[2] + bb[5]) / 2};
    }

    /** {worst, best} window over the start alignments for one strategy. */
    private static int[] windows(Phys.World wd, List<Phys.P> starts, int deg, int sw, List<int[]> a, List<Target> tg, Set<Long> aSet,
                                 Kill kill, Cfg cfg, Res r, int maxJt) {
        int worst = 99, best = 0;
        int[] out = new int[3];
        List<Integer> per = new ArrayList<>();
        for (Phys.P st : starts) {
            int n = 0;
            for (int jt = 0; jt < maxJt; jt++) {
                int res = run(wd, st, deg, sw, jt, a, tg, aSet, kill, cfg, out, r.arrivals.size() < 48 ? r.arrivals : null);
                if (res == -1) break;            // left A before the jump tick
                if (res == 1) {
                    n++;
                    r.airMin = Math.min(r.airMin, out[0]); r.airMax = Math.max(r.airMax, out[0]);
                    r.firstJump = Math.min(r.firstJump, jt);
                } else if (out[1] == 2) r.killHit = true;
                else if (out[1] == 3 && r.fail.isEmpty()) r.fail = "landed on a block outside the route";
            }
            per.add(n);
            best = Math.max(best, n);
        }
        if (maxJt < 40) {
            // chained arrivals are not all equally likely: use the lower quartile as "worst"
            per.sort(null);
            worst = per.isEmpty() ? 0 : per.get(per.size() / 4);
        } else for (int n : per) worst = Math.min(worst, n);
        return new int[]{worst == 99 ? 0 : worst, best};
    }

    /**
     * One run from rest at {@code st}. Returns 1 success, 0 fail, -1 the jump tick could not happen (left A /
     * not standing on A). out[0] = airtime, out[1] = reason (2 kill volume, 3 landed elsewhere).
     */
    static Phys.P rest(Phys.World wd, double x, double y, double z) {
        Phys.P p = new Phys.P();
        p.x = x; p.y = y; p.z = z; p.onGround = true; p.vy = -0.0784;
        p.sup = Phys.support(wd, p);
        return p;
    }

    private static int run(Phys.World wd, Phys.P st, int deg, int sw, int jt, List<int[]> a, List<Target> tg, Set<Long> aSet,
                           Kill kill, Cfg cfg, int[] out, List<Phys.P> sink) {
        Phys.P p = st.copy();
        out[0] = 0; out[1] = 0;
        int stage = 0, jumpAt = -1;
        Target last = tg.get(tg.size() - 1);
        for (int t = 0; t < 90; t++) {
            Target cur = tg.get(stage);
            int[] tc = nearestCell(cur, p.x, p.z);
            double[] P = stage == 0 && cfg.aim0 != null ? cfg.aim0 : aim(wd, tc, cur);
            double yaw = yawTo(p.x, p.z, P[0], P[2]);
            boolean pre = jt >= 0 && (jumpAt < 0 ? true : t <= jumpAt + sw);
            if (deg != 0 && pre && stage == 0) yaw += deg;
            double mag = Math.hypot(P[0] - p.x, P[2] - p.z) < 0.05 ? 0 : 0.98;
            boolean jump = t == jt;
            if (jump) {
                if (!p.onGround) return -1;
                if (!cfg.allowLate && !onA(wd, a, p.x, p.y, p.z, 1e-4)) return -1;
            }
            Phys.tick(wd, p, yaw, mag, jump);
            if (TRACE) System.out.printf(Locale.ROOT, "   jt %d t %d pos %.3f %.3f %.3f v %.3f %.3f %.3f og %s sp %s%n", jt, t, p.x, p.y, p.z, p.vx, p.vy, p.vz, p.onGround, p.sprint);
            if (jump) { if (!p.jumped) return -1; jumpAt = t; }
            if (jt >= 0 && jumpAt < 0 && !p.onGround && t < jt) return -1;
            String k = kill.hit(p.x, p.y, p.z);
            if (k != null) { out[1] = k.startsWith("kill volume") ? 2 : 0; return 0; }
            if (cur.ladder && cur.cells.contains(W.key((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z)))) {
                if (stage == tg.size() - 1) { out[0] = jumpAt < 0 ? t : t - jumpAt; return 1; }
                stage++;
                continue;
            }
            if (p.onGround) {
                List<Long> sups = Phys.supports(wd, p);
                boolean onLast = false, onCur = false, onA = false, onPrev = false;
                for (long s : sups) {
                    if (last.cells.contains(s)) onLast = true;
                    if (cur.cells.contains(s)) onCur = true;
                    if (aSet.contains(s)) onA = true;
                    for (int i = 0; i < stage; i++) if (tg.get(i).cells.contains(s)) onPrev = true;
                }
                if (onLast && !last.slime) {
                    out[0] = jumpAt < 0 ? t : t - jumpAt;
                    Phys.P landed = p.copy();
                    boolean ok = stick(wd, p, last, kill);
                    if (ok && sink != null) sink.add(landed);
                    if (TRACE) System.out.println("   landed on target, stick " + ok);
                    return ok ? 1 : 0;
                }
                if (TRACE) System.out.println("   ground contact, sups " + sups.size() + " onA " + onA + " onCur " + onCur);
                if (onCur && cur.slime) { if (stage < tg.size() - 1) stage++; continue; }
                if (onPrev) continue;
                if (onA && (jumpAt < 0 || (t - jumpAt) <= 1)) continue;
                if (sups.isEmpty()) continue;
                out[1] = onA ? 0 : 3;
                return 0;
            }
            double lowest = 1e9;
            for (int[] c : last.list) lowest = Math.min(lowest, c[1]);
            if (p.y < lowest - 4) return 0;
        }
        return 0;
    }

    private static int[] nearestCell(Target t, double x, double z) {
        int[] best = null;
        double bd = 1e18;
        for (int[] c : t.list) {
            double d = Math.pow(c[0] + 0.5 - x, 2) + Math.pow(c[2] + 0.5 - z, 2) + c[1] * 1e-6;
            if (d < bd) { bd = d; best = c; }
        }
        return best;
    }

    /** After landing: aim at the cell centre for 8 ticks; must stay on the target. */
    private static boolean stick(Phys.World wd, Phys.P p, Target t, Kill kill) {
        if (t.ladder) return true;
        double ty = p.y;
        long s0 = p.sup;
        int[] c = s0 != Long.MIN_VALUE && t.cells.contains(s0) ? new int[]{W.kx(s0), W.ky(s0), W.kz(s0)} : nearestCell(t, p.x, p.z);
        double[] P = aim(wd, c, t);
        int off = 0;
        for (int i = 0; i < 8; i++) {
            double d = Math.hypot(P[0] - p.x, P[2] - p.z);
            Phys.tick(wd, p, yawTo(p.x, p.z, P[0], P[2]), d < 0.35 ? 0 : 0.98, false);
            if (kill.hit(p.x, p.y, p.z) != null) return false;
            if (!p.onGround) off++; else off = 0;
            if (p.y < ty - 0.6 || off > 3) return false;
        }
        return true;
    }

    // ------------------------------------------------------------------ ladder takeoff

    private static Res evalLadder(Phys.World wd, Ob.Node aNode, List<Target> tg, Set<Long> aSet, Kill kill, Res r) {
        Target first = tg.get(0);
        // the ladder column nearest to the target, lowest rung
        int[][] nb = nearest(aNode.cells, first.list);
        int lx = nb[0][0], lz = nb[0][2];
        int lowY = Integer.MAX_VALUE, highY = Integer.MIN_VALUE;
        for (int[] c : aNode.cells) if (c[0] == lx && c[2] == lz) { lowY = Math.min(lowY, c[1]); highY = Math.max(highY, c[1]); }
        String st = wd.state(lx, lowY, lz);
        String f = Bs.prop(st, "facing");
        double px = lx + 0.5, pz = lz + 0.5, intoYaw;
        // ladder facing F hangs on the block on the opposite side; the plate sits on that side of the cell
        if ("north".equals(f)) { pz = lz + 13 / 16.0 - Phys.HW - 1e-3; intoYaw = 0; }
        else if ("south".equals(f)) { pz = lz + 3 / 16.0 + Phys.HW + 1e-3; intoYaw = 180; }
        else if ("west".equals(f)) { px = lx + 13 / 16.0 - Phys.HW - 1e-3; intoYaw = -90; }
        else if ("east".equals(f)) { px = lx + 3 / 16.0 + Phys.HW + 1e-3; intoYaw = 90; }
        else if ("true".equals(Bs.prop(st, "south"))) { pz = lz + 1 - Phys.HW - 1e-3; intoYaw = 0; }    // vine on the block to the south
        else if ("true".equals(Bs.prop(st, "north"))) { pz = lz + Phys.HW + 1e-3; intoYaw = 180; }
        else if ("true".equals(Bs.prop(st, "east"))) { px = lx + 1 - Phys.HW - 1e-3; intoYaw = -90; }
        else if ("true".equals(Bs.prop(st, "west"))) { px = lx + Phys.HW + 1e-3; intoYaw = 90; }
        else { intoYaw = 0; }
        int maxK = (int) Math.ceil((highY - lowY + 1) / 0.1176) + 10;
        int bestN = 0;
        int[] out = new int[3];
        for (int mode = 0; mode < 4; mode++) {
            boolean holdJump = (mode & 1) == 1;
            int deg = (mode & 2) == 0 ? 0 : 1;
            for (int dd = (deg == 0 ? 0 : -40); dd <= (deg == 0 ? 0 : 40); dd += 10) {
                int n = 0;
                Res tmp = new Res();
                for (int k = 0; k <= maxK; k++) {
                    int res = runLadder(wd, px, lowY, pz, intoYaw, k, holdJump, dd, tg, aSet, aNode, kill, out, r.arrivals.size() < 48 ? r.arrivals : null);
                    if (res == 1) { n++; tmp.airMin = Math.min(tmp.airMin, out[0]); tmp.airMax = Math.max(tmp.airMax, out[0]); }
                    else if (out[1] == 2) r.killHit = true;
                }
                if (n > bestN) { bestN = n; r.airMin = tmp.airMin; r.airMax = tmp.airMax; r.deg = dd; r.note = holdJump ? "jump held" : ""; }
            }
        }
        // a release window of n ticks: report it on the same scale as jump-tick windows
        r.worst = r.best = bestN;
        r.firstJump = 2;
        return r;
    }

    private static int runLadder(Phys.World wd, double x, int y, double z, double intoYaw, int k, boolean holdJump, int deg,
                                 List<Target> tg, Set<Long> aSet, Ob.Node aNode, Kill kill, int[] out, List<Phys.P> sink) {
        Phys.P p = new Phys.P();
        p.x = x; p.y = y; p.z = z; p.onGround = false;
        p.sprint = false;
        out[0] = 0; out[1] = 0;
        int stage = 0;
        Target last = tg.get(tg.size() - 1);
        for (int t = 0; t < 140; t++) {
            Target cur = tg.get(stage);
            int[] tc = nearestCell(cur, p.x, p.z);
            double[] P = aim(wd, tc, cur);
            double yaw;
            boolean jump;
            if (t < k) { yaw = intoYaw; jump = false; }
            else { yaw = yawTo(p.x, p.z, P[0], P[2]) + deg; jump = holdJump; }
            if (t >= k && Math.hypot(P[0] - p.x, P[2] - p.z) < 0.05) yaw = intoYaw;
            Phys.tick(wd, p, yaw, 0.98, jump);
            if (TRACE) System.out.printf(Locale.ROOT, "   L k %d j %s d %d t %d pos %.3f %.3f %.3f v %.3f %.3f %.3f og %s%n", k, holdJump, deg, t, p.x, p.y, p.z, p.vx, p.vy, p.vz, p.onGround);
            String kh = kill.hit(p.x, p.y, p.z);
            if (kh != null) { out[1] = kh.startsWith("kill volume") ? 2 : 0; return 0; }
            long feet = W.key((int) Math.floor(p.x), (int) Math.floor(p.y), (int) Math.floor(p.z));
            if (cur.ladder && cur.cells.contains(feet) && !aSet.contains(feet)) {
                if (stage == tg.size() - 1) { out[0] = t - k; return 1; }
                stage++;
                continue;
            }
            if (p.onGround) {
                List<Long> sups = Phys.supports(wd, p);
                boolean onLast = false, onCur = false;
                for (long s : sups) { if (last.cells.contains(s)) onLast = true; if (cur.cells.contains(s)) onCur = true; }
                if (onLast && !last.slime) { out[0] = t - k; Phys.P landed = p.copy(); boolean ok = stick(wd, p, last, kill); if (ok && sink != null) sink.add(landed); return ok ? 1 : 0; }
                if (onCur && cur.slime) { if (stage < tg.size() - 1) stage++; continue; }
                if (t >= k + 3) return 0;      // standing somewhere else after leaving the ladder
            }
            if (p.y < y - 5) return 0;
        }
        return 0;
    }

    /** Number of consecutive blocked columns on the straight line between two cells at body height (neo wall). */
    static int wallLen(Phys.World wd, int[] a, int[] b, double feet) {
        double ax = a[0] + 0.5, az = a[2] + 0.5, bx = b[0] + 0.5, bz = b[2] + 0.5;
        int steps = (int) Math.ceil(Math.hypot(bx - ax, bz - az) * 8);
        Set<Long> cols = new HashSet<>();
        for (int i = 1; i < steps; i++) {
            double t = i / (double) steps;
            int cx = (int) Math.floor(ax + (bx - ax) * t), cz = (int) Math.floor(az + (bz - az) * t);
            if ((cx == a[0] && cz == a[2]) || (cx == b[0] && cz == b[2])) continue;
            boolean low = false, high = false;
            int y0 = (int) Math.floor(feet + 0.2), y1 = (int) Math.floor(feet + 1.6);
            for (int yy = y0; yy <= y1; yy++) {
                if (Phys.shape(wd.state(cx, yy, cz)).length > 0) { if (yy == y0) low = true; else high = true; }
            }
            if (low && high) cols.add(W.key(cx, 0, cz));
        }
        return cols.size();
    }
}
