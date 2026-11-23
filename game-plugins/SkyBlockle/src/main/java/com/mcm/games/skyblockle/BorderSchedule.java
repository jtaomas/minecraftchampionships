package com.mcm.games.skyblockle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Piecewise-linear border schedule: (seconds after GO, half-size) points, plus a damage-per-second curve.
 * Pure Java. Radius tokens (map, castle, arena, ring names, with optional +N / -N) are resolved against a MapModel.
 */
public final class BorderSchedule {
    private final double[] times;
    private final double[] radii;
    private final double[] dmgTimes;
    private final double[] dmg;

    public BorderSchedule(List<double[]> radiusPoints, List<double[]> damagePoints) {
        List<double[]> r = sorted(radiusPoints);
        if (r.isEmpty()) r.add(new double[]{0, 1000});
        times = new double[r.size()];
        radii = new double[r.size()];
        for (int i = 0; i < r.size(); i++) { times[i] = r.get(i)[0]; radii[i] = Math.max(1, r.get(i)[1]); }
        List<double[]> d = sorted(damagePoints);
        if (d.isEmpty()) d.add(new double[]{0, 1});
        dmgTimes = new double[d.size()];
        dmg = new double[d.size()];
        for (int i = 0; i < d.size(); i++) { dmgTimes[i] = d.get(i)[0]; dmg[i] = Math.max(0, d.get(i)[1]); }
    }

    private static List<double[]> sorted(List<double[]> in) {
        List<double[]> out = new ArrayList<>();
        if (in != null) for (double[] p : in) if (p != null && p.length >= 2) out.add(p);
        out.sort((a, b) -> Double.compare(a[0], b[0]));
        return out;
    }

    private static double interp(double[] ts, double[] vs, double t) {
        if (t <= ts[0]) return vs[0];
        for (int i = 1; i < ts.length; i++) {
            if (t <= ts[i]) {
                double span = ts[i] - ts[i - 1];
                if (span <= 0) return vs[i];
                return vs[i - 1] + (vs[i] - vs[i - 1]) * (t - ts[i - 1]) / span;
            }
        }
        return vs[vs.length - 1];
    }

    /** Border half-size at time t (seconds after GO). */
    public double radiusAt(double t) { return interp(times, radii, t); }

    /** Damage per second outside the border at time t. */
    public double damageAt(double t) { return interp(dmgTimes, dmg, t); }

    /** Index of the segment [times[i], times[i+1]) containing t, or -1 before the first / -2 after the last point. */
    public int segmentAt(double t) {
        if (t < times[0]) return -1;
        for (int i = 0; i + 1 < times.length; i++) if (t >= times[i] && t < times[i + 1]) return i;
        return -2;
    }

    public double segmentEndTime(int seg) { return times[seg + 1]; }
    public double segmentEndRadius(int seg) { return radii[seg + 1]; }

    /** True if the border is moving at time t. */
    public boolean movingAt(double t) {
        int s = segmentAt(t);
        return s >= 0 && radii[s + 1] != radii[s];
    }

    /** Start time of the next segment in which the border moves, at or after t; NaN if none. */
    public double nextMoveStart(double t) {
        for (int i = 0; i + 1 < times.length; i++) {
            if (radii[i + 1] != radii[i] && times[i] >= t) return times[i];
        }
        return Double.NaN;
    }

    public double initialRadius() { return radii[0]; }
    public double finalRadius() { return radii[radii.length - 1]; }

    /** Resolve "map", "castle+5", "120", ... against the model. Unknown tokens throw IllegalArgumentException. */
    public static double resolve(Object token, MapModel m) {
        if (token instanceof Number n) return n.doubleValue();
        String s = String.valueOf(token).trim().toLowerCase(Locale.ROOT).replace(" ", "");
        double off = 0;
        int p = Math.max(s.lastIndexOf('+'), s.lastIndexOf('-'));
        if (p > 0) {
            off = Double.parseDouble(s.substring(p + 1)) * (s.charAt(p) == '-' ? -1 : 1);
            s = s.substring(0, p);
        }
        double base;
        switch (s) {
            case "map" -> base = m.mapRadius;
            case "castle" -> base = m.castleRadius;
            case "arena" -> base = m.arenaRadius;
            case "enchanting" -> base = m.rings.enchanting();
            case "brewing" -> base = m.rings.brewing();
            case "fort" -> base = m.rings.fort();
            case "pillar" -> base = m.rings.pillar();
            case "start" -> base = m.rings.start();
            case "generator" -> base = m.rings.generator();
            case "rock" -> base = m.rings.rock();
            default -> base = Double.parseDouble(s);
        }
        return base + off;
    }
}
