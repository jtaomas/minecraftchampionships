import com.mcm.games.tgttos.maps.b.KiteCourse;
import com.mcm.games.tgttos.maps.b.util.Bs;
import com.mcm.games.tgttos.model.TgMapData;

import java.util.ArrayList;
import java.util.List;

/**
 * Kite Festival glide model (copy of maps.b.util.Glide, extended): vanilla elytra tick in the vertical plane along a
 * horizontal polyline, pitch controller on altitude targets, turn speed scrub from the polyline curvature, firework
 * boost (flight 1: 26 ticks, v += look*0.1 + (look*1.5 - v)*0.5 per tick), rockets fired at marked waypoints (expert)
 * or as a rescue when the pilot is slow and low. Swept with the 0.6 gliding box through the real blocks.
 */
final class KiteGlide {
    private KiteGlide() {}

    /** Waypoint: position, target feet y, optional gate window, optional "fire a rocket here". */
    record Wp(double x, double z, double y, double yMin, double yMax, String gate, boolean rocket) {
        static Wp at(double x, double z, double y) { return new Wp(x, z, y, -1e9, 1e9, null, false); }
        static Wp gate(double x, double z, double y, double lo, double hi, String name) { return new Wp(x, z, y, lo, hi, name, false); }
        Wp withRocket() { return new Wp(x, z, y, yMin, yMax, gate, true); }
    }

    static final class Flight {
        final List<double[]> pts = new ArrayList<>();   // x, y, z, speed b/s, path s
        double time, length, maxSpeed, minSpeed = 99, minClear = 99, minFloorMargin = 999;
        int collisions, rockets, gates, gatesOk;
        final List<String> notes = new ArrayList<>();
        final List<String> splits = new ArrayList<>();
        boolean ok = true;
    }

    static final int BOOST_TICKS = 26;

    static void tick(double[] v, double pitchDeg, boolean boost) {
        double f = Math.toRadians(pitchDeg);
        double lookH = Math.cos(f), lookY = -Math.sin(f);
        double e = Math.abs(v[0]);
        double h = Math.cos(f);
        h = h * h;
        v[1] += 0.08 * (-1 + h * 0.75);
        if (v[1] < 0) {
            double i = v[1] * -0.1 * h;
            v[0] += i;
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
        if (boost) {
            v[0] += lookH * 0.1 + (lookH * 1.5 - v[0]) * 0.5;
            v[1] += lookY * 0.1 + (lookY * 1.5 - v[1]) * 0.5;
        }
    }

    static String table() {
        StringBuilder sb = new StringBuilder("  glide model: vanilla elytra tick (gravity 0.08, lift/drag as in LivingEntity.travel); steady glide (pitch: ratio, speed): ");
        for (int p = -10; p <= 40; p += 10) {
            double[] v = {0.5, 0};
            for (int t = 0; t < 600; t++) tick(v, p, false);
            sb.append(String.format("%d°: 1:%.1f %.1f b/s; ", p, v[0] / Math.max(1e-6, -v[1]), Math.hypot(v[0], v[1]) * 20));
        }
        double[] v = {1.0, 0};
        for (int t = 0; t < 26; t++) tick(v, 0, true);
        sb.append(String.format("rocket (flight 1, ~1.3 s) holds %.1f b/s level", Math.hypot(v[0], v[1]) * 20));
        return sb.toString();
    }

    static double TURN_R = 15, GAIN = env("KITE_GAIN", 40), STEP = env("KITE_STEP", 4), EAGER = 0, LEAK = env("KITE_LEAK", 0.1);
    static double env(String k, double d) { String v = System.getenv(k); return v == null ? d : Double.parseDouble(v); }
    static StringBuilder TRACE;

    /**
     * @param fallTicks ticks of free fall before the glide opens (walking off the gantry)
     * @param rockets rockets available
     * @param rescue fire a rocket when slow (< 22 b/s) and more than 4 below the target
     */
    static Flight fly(TgMapData.Blocks blocks, int voidY, List<Wp> wps, double v0, int fallTicks, int rockets, boolean rescue, double eyeLead) {
        Flight fl = new Flight();
        int n = wps.size();
        double[] cum = new double[n];
        for (int i = 1; i < n; i++) cum[i] = cum[i - 1] + Math.hypot(wps.get(i).x() - wps.get(i - 1).x(), wps.get(i).z() - wps.get(i - 1).z());
        double total = cum[n - 1];
        double[] turn = new double[n], spread = new double[n];
        for (int i = 1; i < n - 1; i++) {
            double h1 = Math.atan2(wps.get(i).x() - wps.get(i - 1).x(), wps.get(i).z() - wps.get(i - 1).z());
            double h2 = Math.atan2(wps.get(i + 1).x() - wps.get(i).x(), wps.get(i + 1).z() - wps.get(i).z());
            turn[i] = Math.abs(Math.atan2(Math.sin(h2 - h1), Math.cos(h2 - h1)));
            // a pilot spreads each heading change over the run-up and run-out (half of each neighbouring leg, max TURN_R)
            spread[i] = Math.max(2, Math.min(TURN_R, 0.5 * Math.min(cum[i] - cum[i - 1], cum[i + 1] - cum[i])));
        }
        double s = 0, y = wps.get(0).y();
        double[] v = {v0, 0};
        double pitch = 10;
        int boostLeft = 0, gateIdx = 1, armed = 0;
        for (int t = 0; t < 20 * 150; t++) {
            if (t < fallTicks) {
                v[1] = (v[1] - 0.08) * 0.98;
                v[0] *= 0.91;
                s += v[0];
                y += v[1];
            } else {
                double ahead = s + Math.max(2, v[0] * eyeLead);
                double yt = interp(wps, cum, Math.min(total, ahead));
                // the next gate within the look-ahead wins: aim at its middle, not past it
                for (int i = gateIdx; i < n && cum[i] <= ahead; i++) if (wps.get(i).gate() != null) { yt = wps.get(i).y(); break; }
                double vyd = (yt - y) / Math.max(4, eyeLead);
                double err = v[1] - vyd;
                pitch = pitch + LEAK * (10 - pitch) + clamp(err * GAIN, -STEP, STEP);   // leaky integrator: no wind-up past the needed pitch
                pitch = clamp(pitch, -45, 70);
                // a pilot never pulls up while slow: below ~22 b/s the nose goes down until flying speed is back
                if (boostLeft == 0 && v[0] < 1.1) pitch = Math.max(pitch, v[0] < 0.8 ? 15 : -8 + (1.1 - v[0]) * 60);
                while (gateIdx < n && s >= cum[gateIdx]) {
                    Wp g = wps.get(gateIdx);
                    if (g.gate() != null) {
                        fl.gates++;
                        boolean in = y >= g.yMin() && y <= g.yMax();
                        if (in) fl.gatesOk++;
                        else { fl.ok = false; fl.notes.add(String.format("%s MISSED at y %.1f (window %.1f..%.1f)", g.gate(), y, g.yMin(), g.yMax())); }
                        fl.splits.add(String.format("%s %.1fs", g.gate(), (t + 1) / 20.0));
                    }
                    if (g.rocket()) armed = 40;
                    gateIdx++;
                }
                // a marked spot arms a rocket; it is fired as soon as the pilot is below rocket speed (a rocket slows a faster glider down)
                if (armed > 0 && rockets > 0 && boostLeft == 0 && v[0] < 1.55 && pitch > -8) { rockets--; fl.rockets++; boostLeft = BOOST_TICKS; armed = 0; }
                if (armed > 0) armed--;
                // expert habit: any time the glide drops below EAGER (blocks/tick) and the nose is not up, fire (keeps 1 for the end)
                if (EAGER > 0 && rockets > 1 && boostLeft == 0 && v[0] < EAGER && pitch > -8 && t > fallTicks + 140) { rockets--; fl.rockets++; boostLeft = BOOST_TICKS; }
                if (rescue && rockets > 0 && boostLeft == 0 && v[0] < 1.1 && yt - y > 4) { rockets--; fl.rockets++; boostLeft = BOOST_TICKS; fl.notes.add(String.format("rescue rocket at s=%.0f", s)); }
                tick(v, pitch, boostLeft > 0);
                double kappa = 0;
                for (int i = 1; i < n - 1; i++) if (Math.abs(s - cum[i]) <= spread[i]) kappa += turn[i] / (2 * spread[i]);
                if (kappa > 0 && boostLeft == 0) {   // a boosting rocket pulls the velocity onto the look vector: no scrub
                    double lead = Math.min(1.2, kappa * v[0] / 0.1);
                    v[0] *= Math.sqrt(0.82 + 0.18 * Math.cos(lead));
                }
                if (boostLeft > 0) boostLeft--;
                s += v[0];
                y += v[1];
            }
            double[] xz = pos(wps, cum, Math.min(total, s));
            double sp = Math.hypot(v[0], v[1]) * 20;
            fl.maxSpeed = Math.max(fl.maxSpeed, sp);
            if (t > 40) fl.minSpeed = Math.min(fl.minSpeed, sp);
            fl.pts.add(new double[]{xz[0], y, xz[1], sp, s});
            if (TRACE != null && t % 10 == 0) TRACE.append(String.format("t %.1f s %.0f y %.1f target %.1f v %.1f pitch %.0f boost %d%n", t / 20.0, s, y,
                    interp(wps, cum, Math.min(total, s)), sp, pitch, boostLeft));
            if (s >= total) { fl.time = (t + 1) / 20.0; break; }
            if (y < voidY) { fl.ok = false; fl.notes.add(String.format("fell below void at s=%.0f", s)); fl.time = (t + 1) / 20.0; break; }
            if (t == 20 * 150 - 1) { fl.ok = false; fl.notes.add("did not arrive"); fl.time = 150; }
        }
        for (int i = 1; i < fl.pts.size(); i++) {
            double[] a = fl.pts.get(i - 1), c = fl.pts.get(i);
            fl.length += Math.sqrt(sq(c[0] - a[0]) + sq(c[1] - a[1]) + sq(c[2] - a[2]));
        }
        for (int i = 0; i < fl.pts.size(); i++) {
            double[] p = fl.pts.get(i);
            double clear = clearance(blocks, p[0], p[1], p[2]);
            fl.minClear = Math.min(fl.minClear, clear);
            if (i < fl.pts.size() - 12) {
                double[] nn = KiteCourse.nearest(p[0], p[2]);
                fl.minFloorMargin = Math.min(fl.minFloorMargin, p[1] - (KiteCourse.floorY(nn[0]) + 1));
            }
            if (clear <= 0) {
                if (fl.collisions == 0) fl.notes.add(String.format("first contact at (%.1f, %.1f, %.1f)", p[0], p[1], p[2]));
                fl.collisions++;
            }
        }
        if (fl.collisions > 0) { fl.ok = false; fl.notes.add(fl.collisions + " ticks inside blocks"); }
        return fl;
    }

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
                return new double[]{w.get(i - 1).x() + (w.get(i).x() - w.get(i - 1).x()) * t, w.get(i - 1).z() + (w.get(i).z() - w.get(i - 1).z()) * t};
            }
        Wp l = w.get(w.size() - 1);
        return new double[]{l.x(), l.z()};
    }

    static double clamp(double v, double a, double c) { return Math.max(a, Math.min(c, v)); }

    static double sq(double v) { return v * v; }
}
