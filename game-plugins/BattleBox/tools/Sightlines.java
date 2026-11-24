import com.mcm.games.battlebox.arena.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Offline sightline check over a generated arena grid (eye height 1.62 above the feet). Standing spots are the
 * ones the route check can reach from a spawn (unreachable tops of props and walls are ignored). Glass, panes, bars,
 * fences, walls, leaves and small props count as see-through; the 9 centre cells and the gates count as empty
 * (they get broken / opened during play), so the numbers are a worst case.
 */
public final class Sightlines {
    static final double EYE = 1.62;
    static final double[] BODY = {0.15, 0.9, 1.62, 1.75};

    final Grid g;
    final ArenaGeo geo;
    final double[] lo, hi;   // opaque band per cell, NaN = see-through
    final int sx, sy, sz;

    /** Standing spots players can actually reach (from the route check); null = every standing spot. */
    java.util.Set<String> reachable;

    Sightlines(Grid g, ArenaGeo geo, java.util.Set<String> reachable) {
        this.reachable = reachable;
        this.g = g; this.geo = geo;
        sx = g.maxX - g.minX + 1; sy = g.maxY - g.minY + 1; sz = g.maxZ - g.minZ + 1;
        lo = new double[sx * sy * sz]; hi = new double[sx * sy * sz];
        for (int x = g.minX; x <= g.maxX; x++)
            for (int y = g.minY; y <= g.maxY; y++)
                for (int z = g.minZ; z <= g.maxZ; z++) {
                    int i = idx(x, y, z);
                    String b = g.get(x, y, z);
                    double[] band = Shapes.sightBand(b);
                    if (geo.isCentreCell(x, y, z) || States.id(b).endsWith("_stained_glass")) band = null;
                    lo[i] = band == null ? Double.NaN : band[0];
                    hi[i] = band == null ? Double.NaN : band[1];
                }
    }

    int idx(int x, int y, int z) { return ((y - g.minY) * sz + (z - g.minZ)) * sx + (x - g.minX); }

    boolean clear(double[] a, double[] b) {
        double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int n = (int) Math.ceil(len / 0.07);
        for (int i = 1; i < n; i++) {
            double t = i / (double) n;
            double x = a[0] + dx * t, y = a[1] + dy * t, z = a[2] + dz * t;
            int cx = (int) Math.floor(x), cy = (int) Math.floor(y), cz = (int) Math.floor(z);
            if (cx < g.minX || cx > g.maxX || cy < g.minY || cy > g.maxY || cz < g.minZ || cz > g.maxZ) return false;
            int k = idx(cx, cy, cz);
            double l = lo[k];
            if (Double.isNaN(l)) continue;
            double f = y - cy;
            if (f >= l - 1e-6 && f <= hi[k] + 1e-6) return false;
        }
        return true;
    }

    /** Standing positions {x, feetY, z} inside the floor box. */
    List<double[]> walkable() {
        List<double[]> out = new ArrayList<>();
        int R = geo.R;
        for (int x = -R; x <= R; x++)
            for (int z = -R; z <= R; z++)
                for (int y = g.minY + 1; y <= geo.CEIL - 2; y++) {
                    String feet = g.get(x, y, z), head = g.get(x, y + 1, z);
                    String id = States.id(feet);
                    if (id.equals("water") || id.equals("lava")) continue;
                    if (geo.isCentreCell(x, y, z)) continue;
                    double top = Shapes.standTop(g.get(x, y - 1, z));
                    if (top > 0.99 && top < 1.01 && Shapes.passable(feet) && Shapes.passable(head)) {
                        if (reachable == null || reachable.contains(x + "," + (y * 16) + "," + z)) out.add(new double[]{x + 0.5, y, z + 0.5});
                    } else if (id.endsWith("_slab") && "bottom".equals(Shapes.prop(feet, "type"))
                            && Shapes.passable(head) && Shapes.passable(g.get(x, y + 2, z))) {
                        if (reachable == null || reachable.contains(x + "," + (y * 16 + 8) + "," + z)) out.add(new double[]{x + 0.5, y + 0.5, z + 0.5});
                    }
                }
        return out;
    }

    /** Eye points (3 per cell) of a side's spawn exit: the gate cells and the first arena row. */
    List<double[]> exitEyes(boolean b) {
        List<double[]> l = new ArrayList<>();
        int sg = b ? 1 : -1;
        for (int row : new int[]{geo.W, geo.R})
            for (int x = -geo.GATE_HALF; x <= geo.GATE_HALF; x++)
                for (double fx : new double[]{0.2, 0.5, 0.8})
                    l.add(new double[]{x + fx, 1 + EYE, sg * row + 0.5});
        return l;
    }

    List<double[]> exitBody(boolean b) {
        List<double[]> l = new ArrayList<>();
        int sg = b ? 1 : -1;
        for (int row : new int[]{geo.W, geo.R})
            for (int x = -geo.GATE_HALF; x <= geo.GATE_HALF; x++)
                for (double fx : new double[]{0.2, 0.5, 0.8})
                    for (double h : BODY) l.add(new double[]{x + fx, 1 + h, sg * row + 0.5});
        return l;
    }

    static String pct(long a, long n) { return n == 0 ? "n/a" : String.format(Locale.ROOT, "%.1f%%", 100.0 * a / n); }

    /** Runs all checks; returns the report text. ok[0] is set false if spawn-to-spawn is not fully blocked. */
    String report(String id, boolean[] ok) {
        StringBuilder sb = new StringBuilder();
        List<double[]> pos = walkable();
        List<double[]> aSide = new ArrayList<>(), bSide = new ArrayList<>();
        for (double[] p : pos) { if (p[2] < 0) aSide.add(p); else if (p[2] > 1) bSide.add(p); }
        List<double[]> eyesA = exitEyes(false), bodyB = exitBody(true);

        // 1. spawn exit to spawn exit
        long n = 0, blocked = 0;
        for (double[] a : eyesA) for (double[] t : bodyB) { n++; if (!clear(a, t)) blocked++; }
        if (blocked != n) ok[0] = false;
        sb.append(id).append(": spawn exit -> enemy spawn exit: ").append(pct(blocked, n)).append(" of ").append(n).append(" rays blocked\n");

        // 2. spawn exit to the far half
        long hidden = 0;
        long rays = 0, rb = 0;
        for (double[] p : bSide) {
            boolean seen = false;
            for (double h : BODY) {
                double[] t = {p[0], p[1] + h, p[2]};
                for (double[] a : eyesA) { rays++; if (clear(a, t)) seen = true; else rb++; }
            }
            if (!seen) hidden++;
        }
        sb.append(id).append(": spawn exit -> far half: ").append(pct(hidden, bSide.size())).append(" of ").append(bSide.size())
                .append(" standing spots fully hidden (").append(pct(rb, rays)).append(" of rays)\n");

        // 3. anywhere on side A -> side B's spawn exit
        long blind = 0;
        List<String> leaks = new ArrayList<>();
        for (double[] p : aSide) {
            double[] e = {p[0], p[1] + EYE, p[2]};
            double[] seen = null;
            for (double[] t : bodyB) if (clear(e, t)) { seen = t; break; }
            if (seen == null) blind++;
            else if (leaks.size() < 6) leaks.add(String.format(Locale.ROOT, "(%.1f,%.1f,%.1f)->(%.1f,%.2f,%.1f)", p[0], p[1], p[2], seen[0], seen[1], seen[2]));
        }
        if (blind != aSide.size()) ok[0] = false;
        sb.append(id).append(": own half -> enemy spawn exit: ").append(pct(blind, aSide.size())).append(" of ").append(aSide.size())
                .append(" standing spots can't see it").append(leaks.isEmpty() ? "" : "  leaks at " + String.join(" ", leaks)).append('\n');

        // 4. long cross-arena sightlines (back thirds), eye to eye
        long cn = 0, cb = 0;
        for (double[] p : aSide) {
            if (p[2] > -6) continue;
            double[] e = {p[0], p[1] + EYE, p[2]};
            for (double[] q : bSide) {
                if (q[2] < 6) continue;
                cn++;
                if (!clear(e, new double[]{q[0], q[1] + EYE, q[2]})) cb++;
            }
        }
        sb.append(id).append(": back third -> enemy back third (eye to eye): ").append(pct(cb, cn)).append(" of ").append(cn).append(" lines blocked\n");

        // 5. centre visibility from mid-range ground spots and from high spots (own half)
        List<double[]> ctr = new ArrayList<>();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) ctr.add(new double[]{x + 0.5, geo.CENTRE_Y + 0.95, z + 0.5});   // top face of the cell
        long mid = 0, midSee = 0, high = 0, highSee = 0;
        for (double[] p : aSide) {
            boolean isMid = p[1] < 1.6 && p[2] <= -5.5 && p[2] >= -11;
            boolean isHigh = p[1] >= 4;
            if (!isMid && !isHigh) continue;
            double[] e = {p[0], p[1] + EYE, p[2]};
            boolean sees = false;
            for (double[] c : ctr) if (clear(e, c)) { sees = true; break; }
            if (isMid) { mid++; if (sees) midSee++; }
            if (isHigh) { high++; if (sees) highSee++; }
        }
        sb.append(id).append(": centre visible from ").append(pct(midSee, mid)).append(" of mid-range ground spots (z -11..-6), ")
                .append(pct(highSee, high)).append(" of high spots (feet >= 4)\n");
        return sb.toString();
    }
}
