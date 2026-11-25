package com.mcm.games.race.maps.b;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Frozen Canal course geometry (pure Java): turtle-built centre lines with width / level tables, rasterised into a
 * cell grid. Every lane cell knows its path, arc position s along that path, signed lateral offset d (+ = right of
 * the heading, i.e. +X when heading +Z) and its ice level. Used by {@link FrozenCanal} and the offline validator.
 */
public final class CanalCourse {

    /** Turtle path: heading in degrees, 0 = +Z, 90 = +X. Samples every <= 0.25 blocks. */
    public static final class Path {
        public final String name;
        final List<double[]> pts = new ArrayList<>();          // x, z, heading, s
        public final Map<String, Double> marks = new LinkedHashMap<>();
        /** Arcs as {s0, s1, deg, radius}. */
        public final List<double[]> arcs = new ArrayList<>();
        double x, z, h, s;
        private final List<double[]> hw = new ArrayList<>();    // {s, half width} knots
        private final List<double[]> steps = new ArrayList<>(); // {s, dy}
        int baseLevel;

        Path(String name, double x, double z, double h, int baseLevel) {
            this.name = name;
            this.x = x;
            this.z = z;
            this.h = h;
            this.baseLevel = baseLevel;
            pts.add(new double[]{x, z, h, 0});
        }

        Path straight(double len) {
            int n = Math.max(1, (int) Math.ceil(len / 0.25));
            double st = len / n, r = Math.toRadians(h);
            for (int i = 0; i < n; i++) {
                x += Math.sin(r) * st;
                z += Math.cos(r) * st;
                s += st;
                pts.add(new double[]{x, z, h, s});
            }
            return this;
        }

        /** Arc turning by deg (+ = towards +X when heading +Z, i.e. clockwise seen from above) with centre-line radius r. */
        Path arc(double deg, double r) {
            double len = Math.abs(Math.toRadians(deg)) * r;
            int n = Math.max(1, (int) Math.ceil(len / 0.25));
            double dh = deg / n, st = len / n;
            double s0 = s;
            for (int i = 0; i < n; i++) {
                h += dh / 2;
                double rr = Math.toRadians(h);
                x += Math.sin(rr) * st;
                z += Math.cos(rr) * st;
                h += dh / 2;
                s += st;
                pts.add(new double[]{x, z, h, s});
            }
            arcs.add(new double[]{s0, s, deg, r});
            return this;
        }

        Path mark(String m) { marks.put(m, s); return this; }

        /** Half width knot (linear interpolation between knots). */
        Path width(double atS, double halfWidth) { hw.add(new double[]{atS, halfWidth}); return this; }

        /** Ice level changes by dy from s on (step-downs are dy = -1). */
        Path step(double atS, int dy) { steps.add(new double[]{atS, dy}); return this; }

        public double length() { return s; }

        public double mark0(String m) { Double v = marks.get(m); if (v == null) throw new IllegalArgumentException(m); return v; }

        public double halfWidth(double at) {
            if (hw.isEmpty()) return 5.5;
            if (at <= hw.get(0)[0]) return hw.get(0)[1];
            for (int i = 1; i < hw.size(); i++) {
                double[] a = hw.get(i - 1), b = hw.get(i);
                if (at <= b[0]) return a[1] + (b[1] - a[1]) * (at - a[0]) / Math.max(1e-9, b[0] - a[0]);
            }
            return hw.get(hw.size() - 1)[1];
        }

        public int level(double at) {
            int y = baseLevel;
            for (double[] st : steps) if (at >= st[0]) y += (int) st[1];
            return y;
        }

        public double maxHalfWidth() { double m = 0; for (double[] k : hw) m = Math.max(m, k[1]); return m; }

        /** Sample nearest to arc position at: {x, z, heading, s}. */
        public double[] at(double at) {
            int lo = 0, hi = pts.size() - 1;
            while (lo < hi) {
                int mid = (lo + hi) >>> 1;
                if (pts.get(mid)[3] < at) lo = mid + 1; else hi = mid;
            }
            return pts.get(lo);
        }

        /** World point at arc position at and lateral offset d (+ = right of heading). */
        public double[] point(double at, double d) {
            double[] p = at(at);
            double r = Math.toRadians(p[2]);
            return new double[]{p[0] + Math.cos(r) * d, p[1] - Math.sin(r) * d};
        }

        /** Arc (deg, radius) containing s, or null on a straight. */
        public double[] arcAt(double at) {
            for (double[] a : arcs) if (at >= a[0] && at <= a[1]) return a;
            return null;
        }

        public List<double[]> samples() { return pts; }
    }

    // ------------------------------------------------------------------ the course

    public static final int MAIN = 0, CUT = 1;
    /** Cellar Steps: first drop this far after the hairpin, then every STEP_GAP. */
    public static final double STEP0 = 10, STEP_GAP = 12;
    /** The ring drops 1 this far after the split, once it has fully separated from the causeway (no pockets). */
    public static final double RING_STEP = 18;
    public final Path main, cut;
    public final List<Path> paths = new ArrayList<>();
    /** Arc positions on main where the causeway (cut) leaves and rejoins. */
    public final double cutFrom, cutTo;
    /** Named segments in race order: name -> {s0, s1} on main. */
    public final Map<String, double[]> segments = new LinkedHashMap<>();
    /** Final level of the main lane at the dock. */
    public final int dockLevel;

    public CanalCourse() {
        Path m = new Path("main", 0.0, 9, 0, 0);
        m.mark("grid")
                .straight(88).mark("funnel")
                .straight(10).mark("mill")
                .arc(-180, 12.5).mark("chicane")              // Mill Hairpin: 180 deg, inside radius ~7
                .arc(50, 22).straight(10).arc(-50, 22).straight(8).arc(-40, 22).straight(8).arc(40, 22)
                .straight(14).arc(35, 24).straight(4).arc(-35, 24).mark("chicaneEnd")
                .straight(8).mark("cellar")
                .arc(180, 12.5).mark("cascade")              // Cellar Hairpin at the south end
                .straight(63).mark("split")
                .arc(-50, 38).straight(16).arc(100, 38).straight(16).arc(-50, 38).mark("merge")   // Fish Market ring (safe)
                .straight(8).mark("bend")
                .arc(90, 22).mark("blue")
                .straight(168).mark("brake")
                .arc(180, 12).straight(12).arc(-180, 12).mark("sweeper")
                .straight(10).arc(70, 40).straight(12).arc(-40, 40).straight(10).arc(60, 30).mark("sweepEnd")
                .straight(14).mark("lastBend")
                .arc(90, 22).mark("basin")
                .straight(56).mark("finish")
                .straight(20).mark("dock");
        double sMill = m.mark0("mill"), sCas = m.mark0("cascade"), sSplit = m.mark0("split"), sMerge = m.mark0("merge");
        double sBlue = m.mark0("blue"), sBrake = m.mark0("brake"), sSweep = m.mark0("sweeper"), sBasin = m.mark0("basin");
        double sLast = m.mark0("lastBend"), sFin = m.mark0("finish"), sDock = m.mark0("dock");
        m.width(0, 21).width(24, 7.5).width(m.mark0("funnel"), 7.5).width(sMill, 5.5)
                .width(m.mark0("chicane"), 5.5).width(m.mark0("chicane") + 30, 6).width(m.mark0("chicaneEnd"), 6)
                .width(m.mark0("cellar"), 5.5).width(sCas, 5.5).width(sCas + 14, 6.5).width(sSplit - 6, 6.5).width(sSplit + 6, 5.5)
                .width(sMerge, 5.5).width(sMerge + 8, 6.5).width(sBlue, 6.5).width(sBrake - 10, 6.5).width(sBrake, 5.5)
                .width(sSweep, 5.5).width(sSweep + 12, 6.5).width(sLast, 6.5).width(sBasin, 6.5).width(sBasin + 8, 8.5)
                .width(sFin - 4, 8.5).width(sFin + 6, 7.5).width(sDock, 7.5);
        // Cellar Steps: four 1-block step-downs 18 apart (0 -> -4); the ring drops once more right after the split,
        // the causeway stays up (raised) and drops at its end; the last 1-block drop under the finish arch
        for (int k = 0; k < 4; k++) m.step(sCas + STEP0 + STEP_GAP * k, -1);
        m.step(sSplit + RING_STEP, -1);
        m.step(sFin + 2, -1);
        main = m;
        // the causeway: straight chord from the split to the merge, blue ice, 5 wide, raised 1 over the ring
        cutFrom = sSplit + 2;
        cutTo = sMerge - 2;
        double[] a = m.at(cutFrom), b = m.at(cutTo);
        double dx = b[0] - a[0], dz = b[1] - a[1];
        double len = Math.hypot(dx, dz);
        double hd = Math.toDegrees(Math.atan2(dx, dz));
        Path c = new Path("cut", a[0], a[1], hd, m.level(cutFrom));
        c.straight(len);
        c.width(0, 2.5).width(len, 2.5);
        c.step(len - 3, -1);
        cut = c;
        paths.add(main);
        paths.add(cut);
        dockLevel = m.level(sDock);

        seg("Grid Straight", 0, m.mark0("funnel"));
        seg("Mill Hairpin", m.mark0("funnel"), m.mark0("chicane"));
        seg("Weavers' Chicane", m.mark0("chicane"), m.mark0("chicaneEnd"));
        seg("Cellar Hairpin", m.mark0("chicaneEnd"), sCas);
        seg("Cellar Steps", sCas, sSplit);
        seg("Fish Market split", sSplit, sMerge);
        seg("Brewery Bend", sMerge, sBlue);
        seg("Blue Mile", sBlue, sBrake);
        seg("Bell Switchbacks", sBrake, sSweep);
        seg("Tannery Sweeper", sSweep, sLast);
        seg("Lantern Bend", sLast, sBasin);
        seg("Barge Basin + finish straight", sBasin, sDock);
    }

    private void seg(String n, double a, double b) { segments.put(n, new double[]{a, b}); }

    // ------------------------------------------------------------------ rasterisation

    public static final int NONE = 0, LANE = 1, WALL = 2, FLAT = 3;

    public int x1, z1, x2, z2, w, h;
    /** Per cell: kind, path index, s, d, level, min level of lanes within reach (wall foot), margin to the nearest lane edge. */
    public int[] kind, path, level, minLevel;
    public float[] sArr, dArr, margin;

    public static final double WALL_W = 2.0;

    /** Rasterise into the cell grid (call once). */
    public CanalCourse raster() {
        double minX = 1e9, minZ = 1e9, maxX = -1e9, maxZ = -1e9;
        for (Path p : paths)
            for (double[] q : p.pts) { minX = Math.min(minX, q[0]); maxX = Math.max(maxX, q[0]); minZ = Math.min(minZ, q[1]); maxZ = Math.max(maxZ, q[1]); }
        int pad = 40;
        x1 = (int) Math.floor(minX) - pad; z1 = (int) Math.floor(minZ) - pad;
        x2 = (int) Math.ceil(maxX) + pad; z2 = (int) Math.ceil(maxZ) + pad;
        w = x2 - x1 + 1; h = z2 - z1 + 1;
        int n = w * h;
        kind = new int[n]; path = new int[n]; level = new int[n]; minLevel = new int[n];
        sArr = new float[n]; dArr = new float[n]; margin = new float[n];
        java.util.Arrays.fill(margin, 1e9f);
        java.util.Arrays.fill(minLevel, Integer.MAX_VALUE);
        java.util.Arrays.fill(path, -1);
        float[] dist = new float[n];
        for (int pi = 0; pi < paths.size(); pi++) {
            Path p = paths.get(pi);
            java.util.Arrays.fill(dist, 1e9f);
            float[] ps = new float[n], pd = new float[n], pa = new float[n];
            int last = p.pts.size() - 1;
            for (int qi = 0; qi <= last; qi++) {
                double[] q = p.pts.get(qi);
                double r = Math.toRadians(q[2]), sn = Math.sin(r), cs = Math.cos(r);
                boolean end = qi == 0 || qi == last;
                int cx = (int) Math.floor(q[0]), cz = (int) Math.floor(q[1]);
                int lv = p.level(q[3]);
                double hwq = p.halfWidth(q[3]);
                int reach = (int) Math.ceil(hwq + WALL_W + 2);
                double ml = (hwq + WALL_W + 1.5) * (hwq + WALL_W + 1.5), rr2 = (reach + 0.5) * (reach + 0.5);
                for (int x = Math.max(x1, cx - reach); x <= Math.min(x2, cx + reach); x++) {
                    double vx = x + 0.5 - q[0];
                    int base = (x - x1) * h - z1;
                    for (int z = Math.max(z1, cz - reach); z <= Math.min(z2, cz + reach); z++) {
                        double vz = z + 0.5 - q[1];
                        double d2 = vx * vx + vz * vz;
                        if (d2 > rr2) continue;
                        int i = base + z;
                        if (d2 <= ml && lv < minLevel[i]) minLevel[i] = lv;
                        float df = dist[i];
                        if (d2 < (double) df * df) {
                            dist[i] = (float) Math.sqrt(d2); ps[i] = (float) q[3]; pd[i] = (float) (vx * cs - vz * sn);
                            double along = vx * sn + vz * cs;
                            pa[i] = end && (qi == 0 ? along < -0.5 : along > 0.5) ? (float) Math.abs(along) : 0f;
                        }
                    }
                }
            }
            for (int i = 0; i < n; i++) {
                if (dist[i] > 1e8f) continue;
                double mg = Math.abs(pd[i]) - p.halfWidth(ps[i]);
                // past the open ends of a path: square end (start / dock wall)
                if (pa[i] > 0) mg = Math.max(mg, pa[i]);
                // the raised causeway owns every cell of its own lane (it runs inside the ring's lane at both ends)
                boolean own = pi == CUT && mg <= 0;
                if (mg < margin[i] || own) { margin[i] = (float) Math.min(mg, margin[i]); path[i] = pi; sArr[i] = ps[i]; dArr[i] = pd[i]; }
            }
        }
        for (int i = 0; i < n; i++) {
            if (path[i] < 0) continue;
            Path p = paths.get(path[i]);
            level[i] = p.level(sArr[i]);
            kind[i] = margin[i] <= 0 ? LANE : margin[i] <= WALL_W ? WALL : NONE;
        }
        // slush flat inside the Fish Market ring: between the ring and the causeway, at the lower level
        List<double[]> poly = new ArrayList<>();
        for (double[] q : main.pts) if (q[3] >= cutFrom && q[3] <= cutTo) poly.add(new double[]{q[0], q[1]});
        int flatLevel = main.level(cutTo);
        double px1 = 1e9, px2 = -1e9, pz1 = 1e9, pz2 = -1e9;
        for (double[] q : poly) { px1 = Math.min(px1, q[0]); px2 = Math.max(px2, q[0]); pz1 = Math.min(pz1, q[1]); pz2 = Math.max(pz2, q[1]); }
        for (int x = (int) Math.floor(px1) - 1; x <= (int) Math.ceil(px2) + 1; x++)
            for (int z = (int) Math.floor(pz1) - 1; z <= (int) Math.ceil(pz2) + 1; z++) {
                int i = idx(x, z);
                if (kind[i] == LANE) continue;
                if (!inPoly(poly, x + 0.5, z + 0.5)) continue;
                kind[i] = FLAT;
                level[i] = flatLevel;
                minLevel[i] = Math.min(minLevel[i], flatLevel);
            }
        // walls are at least 2 thick everywhere round the lanes (tapers and path ends can leave gaps otherwise)
        for (int x = x1 + 2; x <= x2 - 2; x++)
            for (int z = z1 + 2; z <= z2 - 2; z++) {
                int i = idx(x, z);
                if (kind[i] != LANE && kind[i] != FLAT) continue;
                for (int dx = -2; dx <= 2; dx++)
                    for (int dz = -2; dz <= 2; dz++) {
                        int j = idx(x + dx, z + dz);
                        if (kind[j] == LANE || kind[j] == FLAT) continue;
                        float mg = Math.max(Math.abs(dx), Math.abs(dz)) == 1 ? 0.5f : 1.5f;
                        if (kind[j] == NONE) {
                            if (path[j] < 0) { path[j] = path[i]; sArr[j] = sArr[i]; dArr[j] = dArr[i]; }
                            level[j] = level[i];
                            kind[j] = WALL;
                            margin[j] = mg;
                        } else if (margin[j] > mg) margin[j] = mg;
                        minLevel[j] = Math.min(minLevel[j], level[i]);
                    }
            }
        return this;
    }

    static boolean inPoly(List<double[]> poly, double x, double z) {
        boolean in = false;
        for (int i = 0, j = poly.size() - 1; i < poly.size(); j = i++) {
            double[] a = poly.get(i), b = poly.get(j);
            if ((a[1] > z) != (b[1] > z) && x < (b[0] - a[0]) * (z - a[1]) / (b[1] - a[1]) + a[0]) in = !in;
        }
        return in;
    }

    public int idx(int x, int z) {
        if (x < x1 || x > x2 || z < z1 || z > z2) return -1;
        return (x - x1) * h + (z - z1);
    }

    public int kindAt(int x, int z) { int i = idx(x, z); return i < 0 ? NONE : kind[i]; }

    /** Text map for debugging (1 char per cell, every step-th cell). */
    public String ascii(int step) {
        StringBuilder sb = new StringBuilder();
        for (int z = z2; z >= z1; z -= step) {
            for (int x = x1; x <= x2; x += step) {
                int i = idx(x, z);
                char c = switch (kind[i]) {
                    case LANE -> path[i] == CUT ? '=' : level[i] == 0 ? '.' : (char) ('0' + Math.min(9, -level[i]));
                    case WALL -> '#';
                    case FLAT -> '~';
                    default -> ' ';
                };
                sb.append(c);
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    public static void main(String[] a) {
        CanalCourse c = new CanalCourse().raster();
        System.out.println(c.ascii(Integer.parseInt(a.length > 0 ? a[0] : "2")));
        System.out.printf("main length %.0f, cut %.0f, bbox x %d..%d z %d..%d, dock level %d%n", c.main.length(), c.cut.length(), c.x1, c.x2, c.z1, c.z2, c.dockLevel);
        for (Map.Entry<String, Double> e : c.main.marks.entrySet()) {
            double[] p = c.main.at(e.getValue());
            System.out.printf("  %-11s s=%6.1f  at (%.1f, %.1f) heading %.0f level %d%n", e.getKey(), e.getValue(), p[0], p[1], p[2], c.main.level(e.getValue()));
        }
    }
}
