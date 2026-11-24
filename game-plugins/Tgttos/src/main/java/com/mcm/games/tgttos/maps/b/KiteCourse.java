package com.mcm.games.tgttos.maps.b;

import java.util.ArrayList;
import java.util.List;

/**
 * Kite Festival course plan (pure data, shared by the generator and the offline validator).
 *
 * <p>The course is a canyon folded into the 500 x 500 slot as four legs joined by three 180-degree turns:
 * leg A x -164.5 (+Z, from the gantry), turn 1 round (-109.5, 160), leg B x -54.5 (-Z), turn 2 round (0.5, -160),
 * leg C x 55.5 (+Z), turn 3 round (110.5, 160), leg D x 165.5 (-Z) into the landing field and the pen. Canyon interior
 * half width 18.5 (37 columns), turn radius 55 on the centre line. {@code s} = distance along the centre line.
 *
 * <p>Two nominal altitude profiles (feet y over s): SAFE (wide openings) and FAST (tight openings / tunnel / needle).
 * Every screen opening is placed on its line's profile, the canyon floor follows the lowest line 15 below
 * (never rising), the wall crest the highest line 16 above.
 */
public final class KiteCourse {
    private KiteCourse() {}

    public static final double HW = 18.5;
    public static final double R = 55;
    public static final double LA = -164.5, LB = -54.5, LC = 55.5, LD = 165.5;
    public static final double Z_TOP = 160, Z_BOT = -160, Z_START = -8, Z_END = 60;
    public static final int GANTRY_Y = 173, FIELD_Y = -45;
    /** Line radii in the turns (centre line = 55): safe outside, fast inside. */
    public static final double R_SAFE = 65, R_FAST = 44;

    // ------------------------------------------------------------------ centre line

    /** A straight (x const, z0 -> z1) or a half-circle arc (centre, angle a0 -> a1). */
    public record Piece(String name, boolean arc, double x, double z0, double z1, double cx, double cz, double a0, double a1, double s0, double len) {
        public double s1() { return s0 + len; }
    }

    public static final List<Piece> PIECES = new ArrayList<>();
    public static final double LEN;

    static {
        double s = 0;
        s = add(new Piece("leg A", false, LA, Z_START, Z_TOP, 0, 0, 0, 0, s, Z_TOP - Z_START));
        s = add(new Piece("turn 1", true, 0, 0, 0, -109.5, Z_TOP, Math.PI, 0, s, Math.PI * R));
        s = add(new Piece("leg B", false, LB, Z_TOP, Z_BOT, 0, 0, 0, 0, s, Z_TOP - Z_BOT));
        s = add(new Piece("turn 2", true, 0, 0, 0, 0.5, Z_BOT, Math.PI, 2 * Math.PI, s, Math.PI * R));
        s = add(new Piece("leg C", false, LC, Z_BOT, Z_TOP, 0, 0, 0, 0, s, Z_TOP - Z_BOT));
        s = add(new Piece("turn 3", true, 0, 0, 0, 110.5, Z_TOP, Math.PI, 0, s, Math.PI * R));
        s = add(new Piece("leg D", false, LD, Z_TOP, Z_END, 0, 0, 0, 0, s, Z_TOP - Z_END));
        LEN = s;
    }

    private static double add(Piece p) { PIECES.add(p); return p.s1(); }

    /** Centre-line point {x, z} at s. */
    public static double[] at(double s) {
        for (Piece p : PIECES) {
            if (s > p.s1() && p != PIECES.get(PIECES.size() - 1)) continue;
            double t = Math.max(0, Math.min(p.len(), s - p.s0()));
            if (!p.arc()) return new double[]{p.x(), p.z0() + Math.signum(p.z1() - p.z0()) * t};
            double a = p.a0() + Math.signum(p.a1() - p.a0()) * t / R;
            return new double[]{p.cx() + R * Math.cos(a), p.cz() + R * Math.sin(a)};
        }
        return new double[]{LD, Z_END};
    }

    /** Point at s on a turn at radius r (safe / fast line in the turn): {x, z}. */
    public static double[] atRadius(Piece p, double t, double r) {
        double a = p.a0() + Math.signum(p.a1() - p.a0()) * t / R;
        return new double[]{p.cx() + r * Math.cos(a), p.cz() + r * Math.sin(a)};
    }

    /** Nearest centre-line point: {s, distance, piece index, cap} (cap = 1 behind the start / beyond the end). */
    public static double[] nearest(double x, double z) {
        double best = 1e9, bd = 1e9, bs = 0, bp = -1, cap = 0;
        for (int i = 0; i < PIECES.size(); i++) {
            Piece p = PIECES.get(i);
            double dist, s, c = 0;
            if (!p.arc()) {
                double dir = Math.signum(p.z1() - p.z0());
                double t = (z - p.z0()) * dir;
                if (t < 0) {
                    if (i != 0) continue;
                    dist = Math.abs(x - p.x()); s = 0; c = -t;
                } else if (t > p.len()) {
                    if (i != PIECES.size() - 1) continue;
                    dist = Math.abs(x - p.x()); s = p.s1(); c = t - p.len();
                } else { dist = Math.abs(x - p.x()); s = p.s0() + t; }
            } else {
                double a = Math.atan2(z - p.cz(), x - p.cx());
                double lo = Math.min(p.a0(), p.a1()), hi = Math.max(p.a0(), p.a1());
                if (hi > Math.PI + 1e-9 && a < 0) a += 2 * Math.PI;
                if (a < lo - 1e-9 || a > hi + 1e-9) continue;
                dist = Math.abs(Math.hypot(x - p.cx(), z - p.cz()) - R);
                s = p.s0() + Math.abs(a - p.a0()) * R;
            }
            double cmp = Math.max(dist, c);
            if (cmp < best) { best = cmp; bd = dist; bs = s; bp = i; cap = c; }
        }
        return new double[]{bs, bd, bp, cap};
    }

    /** s of a leg position (straight legs only). */
    public static double sOn(int leg, double z) {
        Piece p = PIECES.get(leg * 2);
        return p.s0() + (z - p.z0()) * Math.signum(p.z1() - p.z0());
    }

    public static double legX(int leg) { return PIECES.get(leg * 2).x(); }

    // ------------------------------------------------------------------ altitude plan

    /** Nominal feet y of the safe line: control points (s, y). */
    static final double[][] SAFE = {{14.5, 174}, {40, 162}, {65, 154}, {146, 140}, {341, 112}, {439, 98},
            {543, 83}, {661, 66}, {834, 39}, {1000, 15}, {1154, -4}, {1326, -32}, {1362, -37}, {1415, -44}};
    /** Nominal feet y of the fast line. */
    static final double[][] FAST = {{14.5, 174}, {40, 162}, {65, 154}, {90, 149}, {146, 141}, {341, 114},
            {369, 112}, {439, 104}, {465, 103}, {517, 107}, {543, 104}, {661, 82}, {760, 64}, {834, 51}, {858, 43}, {884, 44},
            {910, 45}, {936, 44}, {992, 40}, {1018, 36}, {1108, 26}, {1154, 18}, {1326, -22}, {1362, -37}, {1415, -44}};

    public static double safeY(double s) { return interp(SAFE, s); }
    public static double fastY(double s) { return interp(FAST, s); }

    static double interp(double[][] t, double s) {
        if (s <= t[0][0]) return t[0][1];
        for (int i = 1; i < t.length; i++)
            if (s <= t[i][0]) return t[i - 1][1] + (t[i][1] - t[i - 1][1]) * (s - t[i - 1][0]) / (t[i][0] - t[i - 1][0]);
        return t[t.length - 1][1];
    }

    // ------------------------------------------------------------------ screens

    public enum K {
        SAFE(0), FAST(1), NEEDLE(2), FINISH(0), BRUTAL(3);
        /** Line-bonus tier of an opening of this kind (lime 0, orange 1, red 2, brutal 1x1 / 1-wide / fire 1x1 3). */
        public final int tier;
        K(int tier) { this.tier = tier; }
    }

    /** Opening: cells x0..x0+w-1, y0..y0+h-1 (y0 = Integer.MIN_VALUE: full-height slot of a mast row). */
    public record Hole(int x0, int y0, int w, int h, K k) {
        public boolean slot() { return y0 == Integer.MIN_VALUE; }
        public double cx() { return x0 + w / 2.0; }
        /** Feet window of a gliding player (0.6 high box). */
        public double feetMin() { return y0 + 0.05; }
        public double feetMax() { return y0 + h - 0.65; }
        public double feetMid() { return y0 + (h - 0.6) / 2.0; }
    }

    /** style 0 = kite banner screen, 1 = mast row (slots), 2 = tunnel mouth wall, 3 = finish frame. */
    public record Screen(String seg, String name, int leg, int z, int style, List<Hole> holes) {
        public double s() { return sOn(leg, z); }
        public Hole get(K k) { for (Hole h : holes) if (h.k() == k) return h; return null; }
    }

    /** Tunnel tube: inner size, path points {z, centre x, bottom y}. */
    public record Tube(String name, K k, int inner, int leg, double[][] path) {
        /** {centre x, bottom y} at z (linear between path points). */
        public double[] at(double z) {
            for (int i = 1; i < path.length; i++) {
                double za = path[i - 1][0], zb = path[i][0];
                if ((z - za) * (z - zb) <= 0) {
                    double t = (z - za) / (zb - za);
                    return new double[]{path[i - 1][1] + (path[i][1] - path[i - 1][1]) * t, path[i - 1][2] + (path[i][2] - path[i - 1][2]) * t};
                }
            }
            double[] l = path[path.length - 1];
            return new double[]{l[1], l[2]};
        }
        public int x0(double z) { return (int) Math.round(at(z)[0] - inner / 2.0); }
        public int y0(double z) { return (int) Math.round(at(z)[1]); }
    }

    public static final List<Screen> SCREENS = new ArrayList<>();
    public static final List<Tube> TUBES = new ArrayList<>();

    static int x0(int leg, double dx, int w) { return (int) Math.round(legX(leg) + dx - w / 2.0); }

    /** Hole of kind k centred dx from the leg centre, on its line's nominal height (+dy). */
    static Hole hole(int leg, int z, K k, double dx, int w, double dy) {
        double s = sOn(leg, z);
        double y = (k == K.SAFE ? safeY(s) : fastY(s)) + dy;
        return new Hole(x0(leg, dx, w), (int) Math.round(y - (w - 0.6) / 2.0), w, w, k);
    }

    /** Rectangular opening w x h centred dx from the leg centre on its line's nominal height (+dy). */
    static Hole rect(int leg, int z, K k, double dx, int w, int h, double dy) {
        double s = sOn(leg, z);
        double y = (k == K.SAFE ? safeY(s) : fastY(s)) + dy;
        return new Hole(x0(leg, dx, w), (int) Math.round(y - (h - 0.6) / 2.0), w, h, k);
    }

    static Hole slot(int leg, K k, double dx, int w) { return new Hole(x0(leg, dx, w), Integer.MIN_VALUE, w, 0, k); }

    static void screen(String seg, String name, int leg, int z, int style, Hole... hs) { SCREENS.add(new Screen(seg, name, leg, z, style, List.of(hs))); }

    static {
        final int A = 0, B = 1, C = 2, D = 3;
        K S = K.SAFE, F = K.FAST, N = K.NEEDLE;
        K X = K.BRUTAL;
        // 2 drop gate run: rebuilt from The Glider's Challenge L02-L03 (MIT): wall-to-wall 7-tall letterbox slots at
        // canyon width, then the third gate offers a 7 (fast) and a 3x3 (red) beside the lime 11
        screen("Drop Gate Run", "drop 1", A, 30, 0, rect(A, 30, S, 0, 37, 7, 0));
        screen("Drop Gate Run", "drop 2", A, 55, 0, rect(A, 55, S, 0, 37, 7, 0));
        screen("Drop Gate Run", "drop 3", A, 80, 0, hole(A, 80, S, -4, 11, 0), hole(A, 80, F, 9, 7, 0), hole(A, 80, N, -14, 3, 0));
        // 3 canyon slalom: safe 9 swings wide, fast 7 cuts the inside
        // (Glider's Challenge L05-L06 slalom: lime 9 / orange 7 / red 4 per fin)
        screen("Canyon Slalom", "slalom 1", A, 108, 0, hole(A, 108, S, 7, 9, 0), hole(A, 108, F, -1, 7, 0), hole(A, 108, N, -10, 4, 0));
        screen("Canyon Slalom", "slalom 2", A, 138, 0, hole(A, 138, S, -7, 9, 0), hole(A, 138, F, 6, 7, 0), hole(A, 138, N, 14, 4, 0));
        // 5 tunnel split (leg B runs -Z): safe 11 x 11 tube on the outside of the turn exit, fast 5 x 5 tube with two bends
        double sx = 8, fx = -10;
        Tube safeT = new Tube("safe tube", S, 11, B, new double[][]{
                {132, LB + sx, 0}, {110, LB + 11, 0}, {85, LB + 5, 0}, {62, LB + 8, 0}});
        Tube fastT = new Tube("fast tube", F, 5, B, new double[][]{
                {132, LB + fx, 0}, {118, LB + fx, 0}, {104, LB - 5, 0}, {90, LB - 5, 0}, {76, LB - 11, 0}, {62, LB - 9, 0}});
        for (Tube t : new Tube[]{safeT, fastT})
            for (double[] p : t.path()) {
                double s = sOn(B, p[0]);
                p[2] = Math.round((t.k() == S ? safeY(s) : fastY(s)) - (t.inner() - 0.6) / 2.0);
            }
        TUBES.add(safeT);
        TUBES.add(fastT);
        screen("Tunnel Split", "tunnel mouth", B, 132, 2, tubeHole(safeT, 132), tubeHole(fastT, 132));
        screen("Tunnel Split", "tunnel exit", B, 62, 2, tubeHole(safeT, 62), tubeHole(fastT, 62));
        // 6 lift hill: fast climbs straight up the middle (6 wide), safe slaloms below it (9 wide)
        screen("Lift Hill", "lift 1", B, 36, 0, hole(B, 36, S, 8, 9, 0), hole(B, 36, F, -5, 6, 0));
        screen("Lift Hill", "lift 2", B, 10, 0, hole(B, 10, S, -8, 9, 0), hole(B, 10, F, -2, 6, 0));
        screen("Lift Hill", "lift 3", B, -16, 0, hole(B, -16, S, 8, 9, 0), hole(B, -16, F, 0, 6, 0));
        // 7 ring stack: low 11 (safe), mid 7 (fast), high 3x3 fire ring (red) and a 1x1 fire ring (brutal) beside it:
        // the fire rings are rebuilt from The Glider's Challenge L14 (MIT)
        Hole low = hole(B, -42, S, 0, 11, 0), high = hole(B, -42, N, 0, 3, 0);
        int midY = (low.y0() + low.h() + high.y0() - 7) / 2;
        screen("Ring Stack", "ring stack", B, -42, 0, low, new Hole(x0(B, 0, 7), midY, 7, 7, F), high, hole(B, -42, X, 7, 1, 0));
        // 8 tether forest: mast rows with full-height slots, safe 9 weaves, fast 5 runs almost straight
        // (Glider's Challenge L13 bar curtain: a 1-wide full-height slit between the masts = brutal line)
        screen("Tether Forest", "masts 1", B, -68, 1, slot(B, S, -8, 9), slot(B, F, 3, 5), slot(B, X, 11, 1));
        screen("Tether Forest", "masts 2", B, -98, 1, slot(B, S, 10, 9), slot(B, F, 3, 5), slot(B, X, -7, 1));
        screen("Tether Forest", "masts 3", B, -128, 1, slot(B, S, -9, 9), slot(B, F, 8, 5), slot(B, X, 0, 1));
        // 10 dive and pull-up (leg C, +Z): fast dives through a low 7, then climbs two rising 6s; safe keeps to the side
        screen("Dive & Pull-up", "low arch", C, -136, 0, hole(C, -136, S, 8, 11, 0), hole(C, -136, F, -8, 7, 0));
        screen("Dive & Pull-up", "pull-up 1", C, -110, 0, hole(C, -110, S, 11, 9, 0), hole(C, -110, F, -3, 6, 0));
        screen("Dive & Pull-up", "pull-up 2", C, -84, 0, hole(C, -84, S, 11, 9, 0), hole(C, -84, F, 0, 6, 0), hole(C, -84, N, -8, 3, 0));
        // 11 the needle: three 4 x 4 red gates in a dead straight line; safe detours left / right through 9s, lower
        // owner (playtest 2): "a one-block gap in a massive wall": a 1x1 pinhole, then a 1x3 slit (Glider's Challenge L08 / L10)
        screen("Needle", "needle 1", C, -58, 0, hole(C, -58, N, 0, 4, 0), hole(C, -58, S, 9, 9, 0), hole(C, -58, X, -6, 1, 0));
        screen("Needle", "needle 2", C, -30, 0, hole(C, -30, N, 0, 4, 0), hole(C, -30, S, -9, 9, 0), rect(C, -30, X, 6, 1, 3, 0));
        screen("Needle", "needle 3", C, -2, 0, hole(C, -2, N, 0, 4, 0), hole(C, -2, S, 9, 9, 0));
        // 12 S-canyon: safe 9 swings wall to wall, fast 6 cuts the middle
        screen("S-Canyon", "swing 1", C, 24, 0, hole(C, 24, S, -9, 9, 0), hole(C, 24, F, -3, 6, 0));
        screen("S-Canyon", "swing 2", C, 54, 0, hole(C, 54, S, 9, 9, 0), hole(C, 54, F, 4, 6, 0));
        screen("S-Canyon", "swing 3", C, 84, 0, hole(C, 84, S, -9, 9, 0), hole(C, 84, F, -3, 6, 0));
        screen("S-Canyon", "swing 4", C, 114, 0, hole(C, 114, S, 9, 9, 0), hole(C, 114, F, 3, 6, 0));
        // 14 finish frame over the landing field (leg D, -Z)
        screen("Finish & Field", "finish frame", D, 124, 3, new Hole(x0(D, 0, 15), FIELD_Y + 1, 15, 15, K.FINISH));
    }

    static Hole tubeHole(Tube t, int z) { return new Hole(t.x0(z), t.y0(z), t.inner(), t.inner(), t.k()); }

    // ------------------------------------------------------------------ floor / crest

    private static int[] floorArr, crestArr;

    /** Canyon floor (top block y) at s: 15 below the lowest line within the next 40 blocks, never rising, >= the field. */
    public static int floorY(double s) { ensure(); return floorArr[idx(s)]; }

    /** Wall / screen crest (top block y) at s: 16 above the highest line or opening within +-30, capped. */
    public static int crestY(double s) { ensure(); return crestArr[idx(s)]; }

    private static int idx(double s) { return (int) Math.max(0, Math.min(floorArr.length - 1, Math.round(s))); }

    private static synchronized void ensure() {
        if (floorArr != null) return;
        int n = (int) Math.ceil(LEN) + 1;
        double[] hi = new double[n], lo = new double[n];
        for (int i = 0; i < n; i++) { hi[i] = Math.max(safeY(i), fastY(i)); lo[i] = Math.min(safeY(i), fastY(i)); }
        for (Screen sc : SCREENS)
            for (Hole h : sc.holes()) {
                if (h.slot()) continue;
                int i = (int) Math.round(sc.s());
                hi[i] = Math.max(hi[i], h.y0() + h.h());
                lo[i] = Math.min(lo[i], h.y0() - 1);
            }
        int[] f = new int[n], c = new int[n];
        double[] fm = new double[n];
        double prev = 1e9;
        double sf = sOn(3, 124) - 2;
        for (int i = 0; i < n; i++) {
            double m = 1e9;
            for (int j = i; j <= Math.min(n - 1, i + 40); j++) m = Math.min(m, lo[j]);
            prev = Math.min(prev, Math.floor(m - 15));
            fm[i] = i >= sf ? FIELD_Y : Math.max(FIELD_Y, prev);
        }
        // never steeper than 1 block per 4 along the course (no cliffs in the meadow, walls stay closed)
        for (int i = n - 2; i >= 0; i--) fm[i] = Math.min(fm[i], fm[i + 1] + 0.25);
        for (int i = 0; i < n; i++) {
            f[i] = (int) Math.floor(fm[i]);
            double mx = -1e9;
            for (int j = Math.max(0, i - 30); j <= Math.min(n - 1, i + 30); j++) mx = Math.max(mx, hi[j]);
            int cr = (int) Math.ceil(mx + 16);
            if (i < 40) cr = Math.min(cr, 171);
            c[i] = Math.min(178, cr);
        }
        floorArr = f;
        crestArr = c;
    }

    /** Segment name at s (for reports). */
    public static String segment(double s) {
        String[][] seg = {{"0", "Launch Gantry"}, {"16", "Drop Gate Run"}, {"92", "Canyon Slalom"}, {"168", "Turn 1 (hairpin + ledge A)"},
                {"352", "Tunnel Split"}, {"441", "Lift Hill"}, {"530", "Ring Stack"}, {"556", "Tether Forest"},
                {"661", "Turn 2 (hairpin + ledge B)"}, {"834", "Dive & Pull-up"}, {"915", "Needle"}, {"995", "S-Canyon"},
                {"1154", "Turn 3 (spiral + ledge C)"}, {"1326", "Finish & Field"}};
        String r = seg[0][1];
        for (String[] x : seg) if (s >= Double.parseDouble(x[0])) r = x[1];
        return r;
    }
}
