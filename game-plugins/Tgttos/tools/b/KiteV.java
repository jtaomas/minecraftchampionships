import com.mcm.games.tgttos.maps.b.KiteCourse;
import com.mcm.games.tgttos.maps.b.KiteCourse.Hole;
import com.mcm.games.tgttos.maps.b.KiteCourse.K;
import com.mcm.games.tgttos.maps.b.KiteCourse.Piece;
import com.mcm.games.tgttos.maps.b.KiteCourse.Screen;
import com.mcm.games.tgttos.maps.b.KiteCourse.Tube;
import com.mcm.games.tgttos.maps.b.KiteFestival;
import com.mcm.games.tgttos.maps.b.util.Bs;
import com.mcm.games.tgttos.maps.b.util.Check;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;

/** Kite Festival v2: course sealing, gates, tunnels, hazards, ledges, SAFE / FAST glide lines (vanilla elytra model). */
final class KiteV {
    private KiteV() {}

    static KiteGlide.Flight lastSafe, lastFast;
    static final double SAFE_LAND_Z = 104;
    static final int LEDGE_ROCKETS = 3;

    static void run(G g, Check.Report r) {
        TgMapData d = g.d;
        TgMapData.Blocks go = Check.afterGo(d, d.blocks);
        r.line(String.format("  course: centre line %.0f blocks (4 legs, 3 hairpins r 55), %d gates + 2 tubes, gantry floor y %d -> landing meadow y %d, %d hazard boxes outside the canyon",
                KiteCourse.LEN, KiteCourse.SCREENS.size(), KiteCourse.GANTRY_Y, KiteCourse.FIELD_Y, d.hazardRegions.size()));
        gantry(d, go, r);
        screens(d, go, r);
        tubes(go, r);
        seal(d, go, r);
        r.line(KiteGlide.table());

        int rk = 2 + KiteFestival.EXTRA_ROCKETS;
        List<KiteGlide.Wp> safe = line(false, false), safeR = line(false, true), fast = line(true, true), fast0 = line(true, false);
        double walk = (SAFE_LAND_Z - KiteFestival.PEN_Z2 + 2) / 5.6 + 0.5, walkF = 0.5;
        r.line(String.format("  lines (start: 1 s to walk off the gantry edge, 0.4 s free fall; end: safe lands at z %.0f and runs %.1f s through the choke into the pen, fast glides over the hedges into the pen, +0.5 s punch):", SAFE_LAND_Z, walk - 0.5));
        double ts = fly(r, "SAFE  lime openings, outside of the hairpins, 11x11 tube, low stack ring, needle detour; NO rockets", go, d, safe, 0, false, walk);
        double tsr = fly(r, "SAFE' same line with two boosts per hairpin + rescue rockets when slow and low (" + rk + " available)", go, d, safeR, rk, true, walk);
        lastSafe = flown;
        KiteGlide.EAGER = 1.4;   // expert: a rocket whenever the glide drops under 28 b/s with the nose level or down
        double tf = fly(r, "FAST  orange / red openings, insides of the hairpins, 5x5 tube, high stack ring, three needles; a rocket whenever slower than 28 b/s", go, d, fast, rk, false, walkF);
        KiteGlide.EAGER = 0;
        lastFast = flown;
        double tf0 = fly(r, "FAST' same openings without rockets", go, d, fast0, 0, false, walkF);
        if ((ts > 0 || tsr > 0) && tf > 0) {
            double base = tsr > 0 ? tsr : ts;
            r.check(tf <= base * 0.8, String.format("fast line saves %.0f%% vs the safe line with the same rocket habit (>= 20%%)", 100 * (1 - tf / base)));
            double med = base * 1.2 + 1.5 * 8, weak = base * 1.35 + 4 * 8;
            r.line(String.format("  time profile: expert FAST %.0f s (%s without rockets); safe line flown perfectly: %s without rockets, %.0f s with boosts;"
                    + " median (safe line, 20%% slower flying than the model pilot, 1-2 landings at ~8 s) ~%.0f s; weak (35%% slower, 3-5 landings) ~%.0f s -> DNF at 100 s. Targets: expert 35-42, median 70-85.",
                    tf, tf0 > 0 ? String.format("%.0f s", tf0) : "not flyable", ts > 0 ? String.format("%.0f s", ts) : "short of height at the S-canyon", base, med, weak));
            r.check(tf >= 33 && tf <= 45, String.format("expert time %.0f s within 33-45 s", tf));
        }
        ledges(go, d, r);
        hazards(d, r);
        // gate spacing along the course
        List<Double> ss = new ArrayList<>();
        for (Screen sc : KiteCourse.SCREENS) ss.add(sc.s());
        ss.sort(Double::compare);
        double gap = 0, at = 0;
        for (int i = 1; i < ss.size(); i++) if (ss.get(i) - ss.get(i - 1) > gap) { gap = ss.get(i) - ss.get(i - 1); at = ss.get(i - 1); }
        r.line(String.format("        largest gap between gates: %.0f blocks after s %.0f (%s; hairpins have no gates, the turn itself is the obstacle)", gap, at, KiteCourse.segment(at + 1)));
    }

    static KiteGlide.Flight flown;

    static double fly(Check.Report r, String name, TgMapData.Blocks go, TgMapData d, List<KiteGlide.Wp> wps, int rockets, boolean rescue, double walk) {
        if (System.getenv("KITE_TRACE") != null && name.startsWith(System.getenv("KITE_TRACE"))) KiteGlide.TRACE = new StringBuilder();
        KiteGlide.Flight f = KiteGlide.fly(go, d.voidY, wps, 0.25, 8, rockets, rescue, 6);
        if (KiteGlide.TRACE != null) {
            try { java.nio.file.Files.writeString(java.nio.file.Path.of("tools/b/out/kite_trace.txt"), KiteGlide.TRACE); } catch (java.io.IOException ignored) { }
            KiteGlide.TRACE = null;
        }
        flown = f;
        double t = 1.0 + f.time + walk;
        boolean hz = false;
        for (double[] p : f.pts) { for (Region h : d.hazardRegions) if (h.contains(p[0], p[1], p[2])) { hz = true; break; } if (hz) break; }
        if (hz) { f.ok = false; f.notes.add("flies into a hazard box"); }
        String msg = String.format("%s: %.0f blocks flown, total %.1f s (flight %.1f s, avg %.0f b/s), speed %.0f..%.0f b/s, %d rockets, gates %d/%d, closest clearance %.2f, lowest %.1f above the floor%s",
                name, f.length, t, f.time, f.length / Math.max(0.1, f.time), f.minSpeed, f.maxSpeed, f.rockets, f.gatesOk, f.gates, f.minClear, f.minFloorMargin,
                f.notes.isEmpty() ? "" : "; " + String.join("; ", f.notes));
        if (rockets == 0) r.line("  info  " + msg + (f.ok ? "" : "  (informational: the course is built to be flown with rockets)"));
        else r.check(f.ok, msg);
        if (System.getenv("KITE_SPLITS") != null) r.line("        splits: " + String.join(", ", f.splits));
        return f.ok ? t : -1;
    }

    // ------------------------------------------------------------------ lines

    static Hole pick(Screen sc, boolean fast) {
        if (fast) for (K k : new K[]{K.NEEDLE, K.FAST, K.SAFE, K.FINISH}) { Hole h = sc.get(k); if (h != null) return h; }
        for (K k : new K[]{K.SAFE, K.FINISH}) { Hole h = sc.get(k); if (h != null) return h; }
        return sc.holes().get(0);
    }

    /** Waypoints of a line, ordered along the centre line. */
    static List<KiteGlide.Wp> line(boolean fast, boolean rockets) {
        List<Object[]> e = new ArrayList<>();
        e.add(new Object[]{14.5, KiteGlide.Wp.at(KiteCourse.LA, 7.5, KiteCourse.GANTRY_Y + 1)});
        for (Screen sc : KiteCourse.SCREENS) {
            Hole h = pick(sc, fast);
            double s = sc.s();
            KiteGlide.Wp w;
            if (h.slot()) {
                double y = fast ? KiteCourse.fastY(s) : KiteCourse.safeY(s);
                w = KiteGlide.Wp.gate(h.cx(), sc.z() + 0.5, y, KiteCourse.floorY(s) + 1.05, KiteCourse.crestY(s) - 0.65, sc.name());
            } else w = KiteGlide.Wp.gate(h.cx(), sc.z() + 0.5, h.feetMid(), h.feetMin(), h.feetMax(), sc.name());
            if (rockets && fast && List.of("tunnel mouth", "tunnel exit", "lift 2", "low arch", "needle 2").contains(sc.name())) w = w.withRocket();
            e.add(new Object[]{s, w});
        }
        for (Tube t : KiteCourse.TUBES) {
            if ((t.k() == K.FAST) != fast) continue;
            for (int i = 1; i < t.path().length - 1; i++) {
                double z = t.path()[i][0];
                double x = t.x0(z) + t.inner() / 2.0, b = t.y0(z);
                e.add(new Object[]{KiteCourse.sOn(1, z), KiteGlide.Wp.gate(x, z + 0.5, b + (t.inner() - 0.6) / 2, b + 0.05, b + t.inner() - 0.65, t.name() + " bend " + i)});
            }
        }
        for (int i = 1; i < KiteCourse.PIECES.size(); i += 2) {
            Piece p = KiteCourse.PIECES.get(i);
            int steps = 12;
            for (int k = 0; k <= steps; k++) {
                double t = p.len() * k / steps;
                double[] xz = KiteCourse.atRadius(p, t, fast ? KiteCourse.R_FAST : KiteCourse.R_SAFE);
                double s = p.s0() + t;
                KiteGlide.Wp w = KiteGlide.Wp.at(xz[0], xz[1], fast ? KiteCourse.fastY(s) : KiteCourse.safeY(s));
                if (rockets && (k == 1 || k == 4 || k == 7 || k == 10)) w = w.withRocket();   // boosts through each hairpin (fired only when slow)
                e.add(new Object[]{s, w});
            }
        }
        // safe: land in the field before the choke and walk; fast: glide over the choke hedge and land inside the pen
        double lz = fast ? (KiteFestival.PEN_Z1 + KiteFestival.PEN_Z2) / 2.0 + 2 : SAFE_LAND_Z;
        e.add(new Object[]{KiteCourse.sOn(3, lz), KiteGlide.Wp.gate(KiteCourse.LD, lz + 0.5, KiteCourse.FIELD_Y + 1.2, KiteCourse.FIELD_Y + 0.9, KiteCourse.FIELD_Y + 3.5, fast ? "landing in the pen" : "landing")});
        e.sort(Comparator.comparingDouble(o -> (Double) o[0]));
        List<KiteGlide.Wp> out = new ArrayList<>();
        for (Object[] o : e) out.add((KiteGlide.Wp) o[1]);
        return out;
    }

    // ------------------------------------------------------------------ checks

    static void gantry(TgMapData d, TgMapData.Blocks go, Check.Report r) {
        int removed = 0;
        for (TgMapData.Pos p : d.dropFloor) if (go.get(p.x(), p.y(), p.z()) == null) removed++;
        int f = KiteCourse.floorY(10), under = 0;
        for (TgMapData.Pos p : d.dropFloor) for (int y = f + 2; y < p.y(); y++) if (d.blocks.get(p.x(), y, p.z()) != null) under++;
        r.check(removed == d.dropFloor.size() && under == 0, String.format("gantry floor (%d blocks) drops 3 s after GO into the canyon: %d removed, %d blocks between it and the meadow %d below",
                d.dropFloor.size(), removed, under, KiteCourse.GANTRY_Y - f));
    }

    static void screens(TgMapData d, TgMapData.Blocks go, Check.Report r) {
        int bad = 0;
        StringBuilder sb = new StringBuilder();
        String seg = "";
        for (Screen sc : KiteCourse.SCREENS) {
            double s = sc.s();
            int f = KiteCourse.floorY(s), c = KiteCourse.crestY(s);
            double lx = KiteCourse.legX(sc.leg());
            int holeCells = 0, door = 0, leak = 0, expect = 0;
            for (Hole h : sc.holes()) expect += h.slot() ? h.w() * (c - 1 - f) : h.w() * h.h();
            for (int x = (int) Math.floor(lx - 19.5); x <= (int) Math.floor(lx + 19.5); x++)
                for (int y = f + 1; y <= c; y++) {
                    if (!Bs.passable(go.get(x, y, sc.z()))) continue;
                    boolean inHole = false;
                    for (Hole h : sc.holes()) {
                        int y0 = h.slot() ? f + 1 : h.y0(), y1 = h.slot() ? c - 1 : h.y0() + h.h() - 1;
                        if (x >= h.x0() && x < h.x0() + h.w() && y >= y0 && y <= y1) inHole = true;
                    }
                    if (inHole) holeCells++;
                    else if (y <= f + 2) door++;
                    else leak++;
                }
            boolean ok = leak == 0 && holeCells == expect && door <= 2;
            for (Hole h : sc.holes()) if (!h.slot() && (h.y0() < f + 4 && h.k() != K.FINISH || h.y0() + h.h() > c - 2)) { ok = false; sb.append(" [").append(sc.name()).append(" hole outside floor+4..crest-2]"); }
            if (!ok) bad++;
            if (!sc.seg().equals(seg)) { sb.append("\n        ").append(sc.seg()).append(":"); seg = sc.seg(); }
            sb.append(" ").append(sc.name()).append(String.format(" (s %.0f, floor %d crest %d: ", s, f, c));
            List<String> hs = new ArrayList<>();
            for (Hole h : sc.holes())
                hs.add(h.k().name().toLowerCase() + " " + (h.slot() ? h.w() + "-wide slot" : h.w() + "x" + h.h() + " y" + h.y0()) + String.format(" dx%+.0f", h.cx() - lx));
            sb.append(String.join(", ", hs)).append(leak > 0 ? "; LEAK " + leak : "").append(holeCells != expect ? "; hole cells " + holeCells + "/" + expect : "").append(")");
        }
        r.check(bad == 0, KiteCourse.SCREENS.size() + " gates seal the canyon wall to wall, floor to crest, except their openings (+ a 1x2 walk-through door at the floor):" + sb);
    }

    static void tubes(TgMapData.Blocks go, Check.Report r) {
        for (Tube t : KiteCourse.TUBES) {
            int blocked = 0, open = 0, len = 0;
            for (int z = 63; z <= 131; z++) {
                int x0 = t.x0(z), y0 = t.y0(z), w = t.inner();
                len++;
                for (int x = x0; x < x0 + w; x++) for (int y = y0; y < y0 + w; y++) if (!Bs.passable(go.get(x, y, z))) blocked++;
                for (int x = x0 - 1; x <= x0 + w; x++) for (int y = y0 - 1; y <= y0 + w; y++) {
                    boolean ring = x == x0 - 1 || x == x0 + w || y == y0 - 1 || y == y0 + w;
                    if (ring && Bs.passable(go.get(x, y, z))) open++;
                }
            }
            r.check(blocked == 0 && open == 0, String.format("%s: %dx%d, %d long, interior clear (%d blocked), shell closed (%d gaps), drops %d over its length",
                    t.name(), t.inner(), t.inner(), len, blocked, open, t.y0(131) - t.y0(63)));
        }
    }

    /** 3D flood fill of the air inside the canyon below the crest: must never reach outside columns or the space under the floor. */
    static void seal(TgMapData d, TgMapData.Blocks go, Check.Report r) {
        int X0 = -250, Y0 = -64, Y1 = 200, NX = 501, NY = Y1 - Y0 + 1, NZ = 501;
        short[][] lim = new short[NX][NZ];
        byte[][] kind = new byte[NX][NZ];   // 0 outside, 1 canyon/band
        short[][] flo = new short[NX][NZ];
        for (int x = 0; x < NX; x++)
            for (int z = 0; z < NZ; z++) {
                double[] n = KiteCourse.nearest(x + X0 + 0.5, z + X0 + 0.5);
                boolean near = n[3] == 0 ? n[1] <= KiteCourse.HW + 2.5 : n[3] <= 4 && n[1] <= KiteCourse.HW + 2.5;
                kind[x][z] = (byte) (near ? 1 : 0);
                lim[x][z] = (short) KiteCourse.crestY(n[0]);
                flo[x][z] = (short) (n[3] == 0 && n[1] <= KiteCourse.HW ? KiteCourse.floorY(n[0]) : -999);
            }
        BitSet seen = new BitSet(NX * NY * NZ);
        int[] q = new int[8_000_000];
        int[] par = System.getenv("KITE_SEALDBG") != null ? new int[8_000_000] : null;
        int qh = 0, qt = 0, outside = 0, under = 0, cells = 0;
        int sx = (int) Math.floor(KiteCourse.LA) - X0, sy = 160 - Y0, sz = 20 - X0;
        seen.set((sx * NZ + sz) * NY + sy);
        q[qt++] = (sx * NZ + sz) * NY + sy;
        int[][] dd = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        String firstLeak = "";
        while (qh < qt) {
            int id = q[qh++];
            int y = id % NY, xz = id / NY, z = xz % NZ, x = xz / NZ;
            cells++;
            for (int[] dv : dd) {
                int nx = x + dv[0], ny = y + dv[1], nz = z + dv[2];
                if (nx < 0 || nz < 0 || ny < 0 || nx >= NX || nz >= NZ || ny >= NY) continue;
                int wy = ny + Y0;
                if (wy > lim[nx][nz]) continue;
                int nid = (nx * NZ + nz) * NY + ny;
                if (seen.get(nid)) continue;
                if (!Bs.passable(go.get(nx + X0, wy, nz + X0))) continue;
                seen.set(nid);
                if (kind[nx][nz] == 0) {
                    if (outside++ == 0) {
                        firstLeak = " first at " + (nx + X0) + "," + wy + "," + (nz + X0);
                        if (par != null) {
                            StringBuilder tr = new StringBuilder();
                            int k = qh - 1;
                            for (int st = 0; st < 400 && k > 0; st++, k = par[k]) {
                                int qid = q[k], yy = qid % NY, xxz = qid / NY;
                                if (st % 8 == 0) tr.append(" <- ").append(xxz / NZ + X0).append(',').append(yy + Y0).append(',').append(xxz % NZ + X0);
                            }
                            firstLeak += " path:" + tr;
                        }
                    }
                    continue;
                }
                if (flo[nx][nz] != -999 && wy < flo[nx][nz]) { if (under++ == 0 && firstLeak.isEmpty()) firstLeak = " first under the floor at " + (nx + X0) + "," + wy + "," + (nz + X0); continue; }
                if (qt >= q.length) { r.err("seal flood fill overflow"); return; }
                if (par != null) par[qt] = qh - 1;
                q[qt++] = nid;
            }
        }
        r.check(outside == 0 && under == 0, String.format("canyon sealed below the crest: flood fill of %d air cells from the gantry reaches %d outside cells and %d cells under the floor%s",
                cells, outside, under, firstLeak));
        int pen = 0;
        Region p = d.pen;
        int ci = (int) Math.floor(p.centerX()) - X0, ck = (int) Math.floor(p.centerZ()) - X0;
        if (seen.get((ci * NZ + ck) * NY + (p.minY() - Y0))) pen = 1;
        r.check(pen == 1, "the pen is connected to the gantry through the canyon air (flood fill)");
    }

    static void ledges(TgMapData.Blocks go, TgMapData d, Check.Report r) {
        List<KiteGlide.Wp> safe = line(false, false);
        StringBuilder sb = new StringBuilder();
        int bad = 0;
        for (int[] l : KiteFestival.LEDGES) {
            double s = KiteCourse.nearest(l[0] + 0.5, l[1] + 0.5)[0];
            int f = KiteCourse.floorY(s);
            // jump off the platform edge, then rejoin the safe line at its next three gates
            double ex = l[0] + 0.5 + 3 * l[3], ez = l[1] + 0.5 + 3 * l[4];
            List<KiteGlide.Wp> w = new ArrayList<>();
            w.add(KiteGlide.Wp.at(ex, ez, l[2] + 1).withRocket());
            w.add(KiteGlide.Wp.at(ex + 6 * l[3], ez + 6 * l[4], l[2] - 1));
            int gates = 0;
            boolean started = false;
            for (KiteGlide.Wp p : safe) {
                double ps = KiteCourse.nearest(p.x(), p.z())[0];
                if (!started && ps > s + 8) started = true;
                if (!started) continue;
                w.add(p);
                if (p.gate() != null && ++gates >= 3) break;
            }
            KiteGlide.Flight f2 = KiteGlide.fly(go, d.voidY, w, 0.2, 6, LEDGE_ROCKETS, true, 6);
            if (!f2.ok) bad++;
            sb.append(String.format("%n        ledge at (%d, %d): platform y %d, %d above the floor (ladder %d rungs, ~%.0f s climb); relaunch glide to the next %d gates %s%s",
                    l[0], l[1], l[2], l[2] - f, l[2] - f, (l[2] - f) / 2.35, gates, f2.ok ? "OK" : "FAILS", f2.ok ? "" : " (" + String.join("; ", f2.notes) + ")"));
        }
        r.check(bad == 0, KiteFestival.LEDGES.size() + " wall ledges (ladder from the meadow, open 5x5 platform = shoving spot), each relaunches onto the safe line with at most " + LEDGE_ROCKETS + " rockets (one to take off):" + sb);
    }

    static void hazards(TgMapData d, Check.Report r) {
        int bad = 0;
        for (TgMapData.Spawn s : d.startSpawns) for (Region h : d.hazardRegions) if (h.contains(s.x(), s.y(), s.z())) bad++;
        for (Region h : d.hazardRegions) {
            if (h.contains(d.pen.centerX(), d.pen.minY(), d.pen.centerZ())) bad++;
            for (int[] l : KiteFestival.LEDGES) if (h.contains(l[0] + 0.5, l[2] + 1, l[1] + 0.5)) bad++;
            for (int[] dc : KiteFestival.DECOYS) if (h.contains(dc[0] + 0.5, dc[1], dc[2] + 0.5)) bad++;
        }
        int dec = 0;
        for (int[] dc : KiteFestival.DECOYS) if (d.playBounds.contains(dc[0] + 0.5, dc[1], dc[2] + 0.5) && Bs.passable(d.blocks.get(dc[0], dc[1], dc[2]))) dec++;
        r.check(bad == 0 && dec == KiteFestival.DECOYS.size(), String.format("%d hazard boxes cover everything outside the canyon walls (leaving the canyon = back to the start); none touches a spawn, the pen, a ledge or a decoy; %d decoy chickens on hay bales in the field",
                d.hazardRegions.size(), dec));
    }
}
