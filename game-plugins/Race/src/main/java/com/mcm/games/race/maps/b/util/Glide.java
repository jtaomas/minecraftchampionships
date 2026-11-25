package com.mcm.games.race.maps.b.util;

import com.mcm.games.race.model.TgMapData;

import java.util.ArrayList;
import java.util.List;

/**
 * Elytra glide check. A line is a horizontal polyline (x,z) with an altitude target per waypoint and optional
 * gate windows. The flight is simulated with the vanilla elytra update (per tick, gravity 0.08, lift/drag terms,
 * firework boost) in the vertical plane along the polyline, steered by a simple pitch controller that follows the
 * altitude targets. The trajectory is then swept through the real blocks with the player's gliding box.
 */
public final class Glide {
    private Glide() {}

    /** Waypoint: horizontal position, target feet y, and optional window [yMin, yMax] the flight must pass. */
    public record Wp(double x, double z, double y, double yMin, double yMax, String gate) {
        public static Wp at(double x, double z, double y) { return new Wp(x, z, y, -1e9, 1e9, null); }
        public static Wp gate(double x, double z, double y, double yMin, double yMax, String name) { return new Wp(x, z, y, yMin, yMax, name); }
    }

    public static final class Flight {
        public final List<double[]> pts = new ArrayList<>();   // x, y, z, speed(b/s)
        public double time, length, maxSpeed, minClear = 99;
        public int collisions, fireworks;
        public final List<String> notes = new ArrayList<>();
        public boolean ok = true;
        public double worstGlide = 0;
        public double maxTurnDeg = 0;   // steepest sustained climb ratio not used; kept for report
    }

    /** One vanilla elytra tick in the vertical plane. v = {vh, vy}; pitchDeg > 0 looks down. */
    static void tick(double[] v, double pitchDeg, boolean boost) {
        double f = Math.toRadians(pitchDeg);
        double lookH = Math.cos(f), lookY = -Math.sin(f);
        double e = Math.abs(v[0]);
        double h = Math.cos(f);
        h = h * h;   // look vector length 1 -> min(1, 1/0.4) = 1
        v[1] += 0.08 * (-1 + h * 0.75);
        if (v[1] < 0) {
            double i = v[1] * -0.1 * h;
            v[0] += i;           // look.x / d == 1 in the plane
            v[1] += i;
        }
        if (f < 0) {
            double i = e * -Math.sin(f) * 0.04;
            v[0] -= i;
            v[1] += i * 3.2;
        }
        v[0] += (e - v[0]) * 0.1;
        v[0] *= 0.99;
        v[1] *= 0.98;
        if (boost) {   // FireworkRocketEntity: v += look*0.1 + (look*1.5 - v)*0.5
            v[0] += lookH * 0.1 + (lookH * 1.5 - v[0]) * 0.5;
            v[1] += lookY * 0.1 + (lookY * 1.5 - v[1]) * 0.5;
        }
    }

    /** Steady glide table: pitch -> (glide ratio horizontal/vertical, speed b/s) after 30 s. */
    public static String table() {
        StringBuilder sb = new StringBuilder("  vanilla elytra steady glide (pitch: ratio, speed): ");
        for (int p = -10; p <= 40; p += 10) {
            double[] v = {0.5, 0};
            for (int t = 0; t < 600; t++) tick(v, p, false);
            sb.append(String.format("%d°: 1:%.1f %.1f b/s; ", p, v[0] / Math.max(1e-6, -v[1]), Math.hypot(v[0], v[1]) * 20));
        }
        return sb.toString();
    }

    /**
     * Fly the line. Starts at the first waypoint with the given initial speed (b/tick, horizontal). Uses up to
     * {@code rockets} fireworks when below the target by more than 3 blocks.
     */
    static final double TURN_R = 10;

    public static Flight fly(TgMapData d, List<Wp> wps, double v0, int rockets, double eyeLead) {
        Flight fl = new Flight();
        double[] cum = new double[wps.size()];
        for (int i = 1; i < wps.size(); i++)
            cum[i] = cum[i - 1] + Math.hypot(wps.get(i).x() - wps.get(i - 1).x(), wps.get(i).z() - wps.get(i - 1).z());
        double total = cum[cum.length - 1];
        double[] turn = new double[wps.size()];
        for (int i = 1; i < wps.size() - 1; i++) {
            double h1 = Math.atan2(wps.get(i).x() - wps.get(i - 1).x(), wps.get(i).z() - wps.get(i - 1).z());
            double h2 = Math.atan2(wps.get(i + 1).x() - wps.get(i).x(), wps.get(i + 1).z() - wps.get(i).z());
            turn[i] = Math.abs(Math.atan2(Math.sin(h2 - h1), Math.cos(h2 - h1)));
            fl.maxTurnDeg = Math.max(fl.maxTurnDeg, Math.toDegrees(turn[i]));
        }
        double s = 0, y = wps.get(0).y();
        double[] v = {v0, 0};
        double pitch = 10;
        int boostLeft = 0;
        int gateIdx = 1;
        for (int t = 0; t < 20 * 60; t++) {
            // target altitude a little ahead
            double ahead = s + Math.max(2, v[0] * eyeLead);
            double yt = interp(wps, cum, Math.min(total, ahead));
            double vyd = (yt - y) / Math.max(4, eyeLead);
            double err = v[1] - vyd;                 // positive -> rising too fast -> nose down
            pitch += clamp(err * 60, -6, 6);
            pitch = clamp(pitch, -45, 70);
            // stall protection (what a player does): when slow, never pull up - nose down until flying speed is back
            if (v[0] < 1.1 && y > yt - 6) pitch = Math.max(pitch, 12 + (1.1 - v[0]) * 30);
            if (rockets > 0 && boostLeft == 0 && ((fl.fireworks == 0 && s > 3) || (yt - y > 3 && s > 10))) { rockets--; fl.fireworks++; boostLeft = 26; pitch = -20; }
            tick(v, pitch, boostLeft > 0);
            // turning: vanilla blends the horizontal velocity 10%/tick towards the look direction, so a turn rate w needs
            // the look to lead the velocity by ~w/0.1 rad, which scrubs speed by sqrt(0.82 + 0.18 cos(lead)) per tick.
            // Corners of the polyline are spread over +-TURN_R blocks.
            double kappa = 0;
            for (int i = 1; i < wps.size() - 1; i++) if (Math.abs(s - cum[i]) <= TURN_R) kappa += turn[i] / (2 * TURN_R);
            if (kappa > 0) {
                double lead = Math.min(1.2, kappa * v[0] / 0.1);
                v[0] *= Math.sqrt(0.82 + 0.18 * Math.cos(lead));
            }
            if (boostLeft > 0) boostLeft--;
            s += v[0];
            y += v[1];
            double[] xz = pos(wps, cum, Math.min(total, s));
            double sp = Math.hypot(v[0], v[1]) * 20;
            fl.maxSpeed = Math.max(fl.maxSpeed, sp);
            fl.pts.add(new double[]{xz[0], y, xz[1], sp});
            while (gateIdx < wps.size() && s >= cum[gateIdx]) {
                Wp g = wps.get(gateIdx);
                if (g.gate() != null) {
                    boolean in = y >= g.yMin() && y <= g.yMax();
                    fl.notes.add(String.format("%s at y %.1f (window %.0f..%.0f) %s", g.gate(), y, g.yMin(), g.yMax(), in ? "OK" : "MISSED"));
                    if (!in) fl.ok = false;
                }
                gateIdx++;
            }
            if (s >= total) { fl.time = (t + 1) / 20.0; break; }
            if (y < d.voidY) { fl.ok = false; fl.notes.add(String.format("fell below void at s=%.0f", s)); fl.time = (t + 1) / 20.0; break; }
        }
        fl.length = 0;
        for (int i = 1; i < fl.pts.size(); i++) {
            double[] a = fl.pts.get(i - 1), c = fl.pts.get(i);
            fl.length += Math.sqrt(sq(c[0] - a[0]) + sq(c[1] - a[1]) + sq(c[2] - a[2]));
        }
        // sweep the gliding box (0.6 x 0.6 x 0.6) through the blocks
        for (double[] p : fl.pts) {
            double clear = clearance(d.blocks, p[0], p[1], p[2]);
            fl.minClear = Math.min(fl.minClear, clear);
            if (clear <= 0) {
                if (fl.collisions == 0) fl.notes.add(String.format("first contact at (%.1f, %.1f, %.1f)", p[0], p[1], p[2]));
                fl.collisions++;
            }
        }
        if (fl.collisions > 0) { fl.ok = false; fl.notes.add(fl.collisions + " ticks inside blocks"); }
        return fl;
    }

    /** Distance (in blocks, 0..2) from the gliding box centre to the nearest solid cell; 0 = collides. */
    static double clearance(TgMapData.Blocks b, double x, double y, double z) {
        double best = 2.5;
        for (int dx = -2; dx <= 2; dx++)
            for (int dy = -2; dy <= 2; dy++)
                for (int dz = -2; dz <= 2; dz++) {
                    int cx = (int) Math.floor(x) + dx, cy = (int) Math.floor(y + 0.3) + dy, cz = (int) Math.floor(z) + dz;
                    if (Bs.passable(b.get(cx, cy, cz))) continue;
                    double nx = clamp(x, cx, cx + 1), ny = clamp(y + 0.3, cy, cy + 1), nz = clamp(z, cz, cz + 1);
                    double dist = Math.sqrt(sq(nx - x) + sq(ny - y - 0.3) + sq(nz - z)) - 0.3;
                    best = Math.min(best, Math.max(0, dist));
                }
        return best;
    }

    static double interp(List<Wp> w, double[] cum, double s) {
        for (int i = 1; i < w.size(); i++)
            if (s <= cum[i]) {
                double t = (s - cum[i - 1]) / Math.max(1e-9, cum[i] - cum[i - 1]);
                return w.get(i - 1).y() + (w.get(i).y() - w.get(i - 1).y()) * t;
            }
        return w.get(w.size() - 1).y();
    }

    static double[] pos(List<Wp> w, double[] cum, double s) {
        for (int i = 1; i < w.size(); i++)
            if (s <= cum[i]) {
                double t = (s - cum[i - 1]) / Math.max(1e-9, cum[i] - cum[i - 1]);
                return new double[]{w.get(i - 1).x() + (w.get(i).x() - w.get(i - 1).x()) * t,
                        w.get(i - 1).z() + (w.get(i).z() - w.get(i - 1).z()) * t};
            }
        Wp l = w.get(w.size() - 1);
        return new double[]{l.x(), l.z()};
    }

    static double clamp(double v, double a, double c) { return Math.max(a, Math.min(c, v)); }

    static double sq(double v) { return v * v; }
}
