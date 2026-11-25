package com.mcm.games.race.maps.b;

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

    /**
     * Opening kinds. Owner (feedback 3): three lines, one colour each - SAFE = EASY (lime, 9-11), FAST = MEDIUM
     * (orange, 5-7), NEEDLE / BRUTAL = HARD (red, 3x3 rings and 1-wide slots). FINISH = the last wall's 1-wide slit.
     */
    public enum K {
        SAFE(0), FAST(1), NEEDLE(2), FINISH(0), BRUTAL(2);
        /** Line-bonus tier: 0 easy, 1 medium, 2 hard. */
        public final int tier;
        K(int tier) { this.tier = tier; }
        public String label() { return tier == 0 ? "EASY" : tier == 1 ? "MEDIUM" : "HARD"; }
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

    /** Opening widths of the three lines (feedback 3): easy 9, medium 6 (5-wide mast slots), hard 3 (1-wide mast slits). */
    public static final int WE = 9, WM = 6, WH = 3;
    /** Minimum clear wall between two openings of a screen (edge to edge, sideways or vertically). */
    public static final int GAP = 5;
    /** Height of the last wall's 1-wide slit (from the meadow floor + 1). */
    public static final int LAST_H = 12;


    /** Side by side: easy / medium / hard centred dx from the leg centre, each on its line's height. */
    static void row(String seg, String name, int leg, int z, double e, double m, double h) {
        screen(seg, name, leg, z, 0, hole(leg, z, K.SAFE, e, WE, 0), hole(leg, z, K.FAST, m, WM, 0), hole(leg, z, K.NEEDLE, h, WH, 0));
    }

    /** Mast row: full-height slots side by side (easy 9, medium 5, hard 1). */
    static void masts(String seg, String name, int leg, int z, double e, double m, double h) {
        screen(seg, name, leg, z, 1, slot(leg, K.SAFE, e, 9), slot(leg, K.FAST, m, 5), slot(leg, K.BRUTAL, h, 1));
    }

    /**
     * Stacked at dx (only where the safe line runs 20+ below the fast one): easy on the safe line, medium on the fast
     * line, hard 3 x 3 between them - a dive below the medium opening (easy pushed down if needed to keep GAP).
     */
    static void stack(String seg, String name, int leg, int z, double dx) {
        Hole e = hole(leg, z, K.SAFE, dx, WE, 0), m = hole(leg, z, K.FAST, dx, WM, 0);
        if (m.y0() - (e.y0() + e.h()) < 2 * GAP + WH) e = new Hole(e.x0(), m.y0() - 2 * GAP - WH - e.h(), e.w(), e.h(), e.k());
        int room = m.y0() - (e.y0() + e.h());
        int hy = m.y0() - GAP - WH;
        Hole h = new Hole(x0(leg, dx, WH), hy, WH, WH, K.NEEDLE);
        screen(seg, name, leg, z, 0, e, m, h);
    }

    static {
        final int A = 0, B = 1, C = 2, D = 3;
        K S = K.SAFE, F = K.FAST;
        // Drop Gate Run: two wall-to-wall 7-tall letterboxes (The Glider's Challenge L02-L03, MIT), then the three lines
        // split: from here every gate has an easy (lime), a medium (orange) and a hard (red) opening, never closer than
        // GAP blocks, side by side on some gates and stacked on others (owner, feedback 3)
        screen("Drop Gate Run", "drop 1", A, 30, 0, rect(A, 30, S, 0, 37, 7, 0));
        screen("Drop Gate Run", "drop 2", A, 55, 0, rect(A, 55, S, 0, 37, 7, 0));
        row("Drop Gate Run", "drop 3", A, 80, -10.5, 3, 13.5);
        row("Canyon Slalom", "slalom 1", A, 108, -10.5, 3, 13.5);
        row("Canyon Slalom", "slalom 2", A, 138, 10.5, -3, -13.5);
        // Tunnel Split (leg B runs -Z): easy 11 x 11 tube on the outside, medium 5 x 5 tube with two bends
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
        row("Lift Hill", "lift 1", B, 36, 10.5, -3, -13.5);
        row("Lift Hill", "lift 2", B, 10, -10.5, 3, 13.5);
        row("Lift Hill", "lift 3", B, -16, 10.5, -3, -13.5);
        // Ring Stack: the hard ring is the first fire ring (The Glider's Challenge L14 fire rings, MIT)
        row("Ring Stack", "ring stack", B, -42, 10.5, -3, -13.5);
        // Tether Forest: mast rows of full-height slots (Glider's Challenge L13 bar curtain: the 1-wide slit is hard)
        masts("Tether Forest", "masts 1", B, -68, 10.5, -3, -13.5);
        masts("Tether Forest", "masts 2", B, -98, -10.5, 3, 13.5);
        masts("Tether Forest", "masts 3", B, -128, 10.5, -3, -13.5);
        // Dive & Pull-up (leg C, +Z)
        row("Dive & Pull-up", "low arch", C, -136, 10.5, -3, -13.5);
        // (medium 2 lower: the medium line comes out of the low-arch dive)
        screen("Dive & Pull-up", "pull-up 1", C, -110, 0, hole(C, -110, K.SAFE, 10.5, WE, 0), hole(C, -110, K.FAST, -3, WM, -2), hole(C, -110, K.NEEDLE, -13.5, WH, -2));
        row("Dive & Pull-up", "pull-up 2", C, -84, -10.5, 3, 13.5);
        // Needle: stacked - the hard 3 x 3 sits between the easy and the medium openings (a dive)
        stack("Needle", "needle 1", C, -58, 0);
        row("Needle", "needle 2", C, -30, -10.5, 3, 13.5);
        stack("Needle", "needle 3", C, -2, 0);
        // S-Canyon
        row("S-Canyon", "swing 1", C, 24, 10.5, -3, -13.5);
        stack("S-Canyon", "swing 2", C, 54, 0);
        row("S-Canyon", "swing 3", C, 84, -10.5, 3, 13.5);
        stack("S-Canyon", "swing 4", C, 114, 0);
        // the last wall (owner, feedback 3): one 1-wide slit everyone must thread, 12 tall from the meadow up
        screen("Finish & Field", "last wall", D, 124, 3, new Hole(x0(D, 0, 1), FIELD_Y + 1, 1, LAST_H, K.FINISH));
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
