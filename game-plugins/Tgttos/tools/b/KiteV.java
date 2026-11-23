import com.mcm.games.tgttos.maps.b.KiteFestival;
import com.mcm.games.tgttos.maps.b.util.Bs;
import com.mcm.games.tgttos.maps.b.util.Check;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.maps.b.util.Glide;
import com.mcm.games.tgttos.maps.b.util.Glide.Wp;
import com.mcm.games.tgttos.model.TgMapData;

import java.util.ArrayList;
import java.util.List;

/** Kite Festival: gantry drop, arch screens, obstacle spacing, SAFE / FAST glide lines (vanilla elytra model). */
final class KiteV {
    private KiteV() {}

    static double c(double z) { return Math.floor(KiteFestival.cx(z)) + 0.5; }

    static void run(G g, Check.Report r) {
        TgMapData d = g.d;
        TgMapData go = Check.view(d, Check.afterGo(d, d.blocks));
        int fx = 0;
        for (TgMapData.Pos p : d.dropFloor) if (go.blocks.get(p.x(), p.y(), p.z()) == null) fx++;
        int under = 0;
        for (int x = -18; x <= 17; x++) for (int z = 0; z <= 6; z++) for (int y = 1; y < KiteFestival.GANTRY_Y; y++) if (d.blocks.get(x, y, z) != null) under++;
        r.check(fx == 36 * 7 && under == 0, "gantry floor drops at GO: " + fx + " floor blocks removed, " + under + " blocks below the gantry (free fall into the valley)");
        // arch walls seal the valley (and the ridges) below the crossbar except the window and the big gate
        int sealed = 0;
        StringBuilder wins = new StringBuilder();
        for (int[] a : KiteFestival.ARCHES) {
            int z = a[0], cc = (int) Math.floor(KiteFestival.cx(z)), holes = 0, win = 0, gate = 0;
            int[] gx = KiteFestival.gateX(z, a[3]);
            for (int x = cc - KiteFestival.HALF - 18; x <= cc + KiteFestival.HALF + 17; x++)
                for (int y = 2; y <= a[1]; y++)
                    if (Bs.passable(d.blocks.get(x, y, z))) {
                        if (x >= cc - 2 && x <= cc + 2 && y >= a[2] && y <= a[2] + 4) win++;
                        else if (x >= gx[0] && x <= gx[1] && y >= a[4] && y <= a[4] + 9) gate++;
                        else holes++;
                    }
            if (holes == 0 && win == 25 && gate == 90) sealed++;
            wins.append(String.format(" z%d: window y %d-%d at x %d, gate %s x %d..%d y %d-%d, wall top y %d;", z, a[2], a[2] + 4, cc - 2,
                    a[3] > 0 ? "right" : "left", gx[0], gx[1], a[4], a[4] + 9, a[1] + 1));
        }
        r.check(sealed == 3, "3 arch walls seal valley + ridges except a 5 x 5 window (skill gate, straight) and a 9 x 10 gate at alternating walls (choke for the slalom):" + wins);
        // obstacle spacing along the course
        List<Integer> obs = new ArrayList<>(List.of(7));
        for (int[] k : KiteFestival.KITES) obs.add(k[0]);
        for (int[] a : KiteFestival.ARCHES) obs.add(a[0]);
        obs.add(117);
        obs.sort(Integer::compare);
        int gap = 0;
        for (int i = 1; i < obs.size(); i++) gap = Math.max(gap, obs.get(i) - obs.get(i - 1));
        r.check(gap <= 25, "an obstacle every <= 25 blocks along the glide (z " + obs + "; largest gap " + gap + ")");
        r.line(Glide.table());

        List<Wp> fast = List.of(Wp.at(0.5, 7, 39), Wp.at(c(22), 22, 31),
                Wp.gate(c(45), 45, 21, 19.05, 23.35, "window 1"), Wp.at(c(62), 62, 19),
                Wp.gate(c(80), 80, 17, 15.05, 19.35, "window 2"), Wp.at(c(96), 96, 15.5),
                Wp.gate(c(112), 112, 14, 12.05, 16.35, "window 3"), Wp.at(0.5, 118, 12.4),
                Wp.gate(0.5, 121, 12.0, 11.0, 13.0, "landing ledge"));
        List<Wp> fast2 = new ArrayList<>(fast.subList(0, 7));
        fast2.add(Wp.at(0.5, 128, 20));
        fast2.add(Wp.gate(0.5, 141, 19.6, 19.05, 23, "into the pen"));
        List<Wp> safe = new ArrayList<>(List.of(Wp.at(0.5, 8, 39)));
        String[] names = {"gate 1", "gate 2", "gate 3"};
        for (int i = 0; i < 3; i++) {
            int[] a = KiteFestival.ARCHES[i];
            int[] gx = KiteFestival.gateX(a[0], a[3]);
            safe.add(Wp.gate((gx[0] + gx[1] + 1) / 2.0, a[0], a[4] + 4.5, a[4] + 0.05, a[4] + 9.35, names[i]));
        }
        safe.add(Wp.at(0.5, 130, 22.5));
        safe.add(Wp.gate(0.5, 141, 20.2, 19.05, 24, "into the pen"));
        r.line("  glide lines (vanilla elytra tick, pitch controller on altitude targets, swept 0.6 box through the blocks; 0.6 s free fall before gliding):");
        double ts = fly(r, "SAFE  firework at the drop, slalom through the three big gates", go, safe, 2, 0);
        // fast: land on the ledge under the meadow, then 5 steps + 18 blocks on foot into the pen (sprint 5.6 b/s)
        double walk = (7 + 18) / 5.6 + 0.4;
        double tf = fly(r, String.format("FAST' one firework at the drop, three windows, land on the ledge (+%.1f s on foot into the pen)", walk), go, fast, 1, walk);
        double tf2 = fly(r, "FAST  same windows with both fireworks, pull up over the hedge into the pen", go, fast2, 2, 0);
        if (tf2 > 0 && (tf < 0 || tf2 < tf)) tf = tf2;
        if (ts > 0 && tf > 0) {
            r.check(tf <= ts * 0.8, String.format("fast line saves %.0f%% vs the safe line (>= 20%%)", 100 * (1 - tf / ts)));
            r.line(String.format("  time profile: elytra is fast - expert %.0f s fast / %.0f s safe incl. the drop; a missed window = void respawn (+~4 s and"
                    + " another try), median ~%.0f s. Inherently below the 20-35 s target (reported).", tf, ts, ts * 1.6 + 5));
        }
    }

    static double fly(Check.Report r, String name, TgMapData d, List<Wp> wps, int rockets, double extra) {
        Glide.Flight f = Glide.fly(d, wps, 0.35, rockets, 6);
        double t = f.time + 0.6 + extra;
        r.check(f.ok, String.format("%s: %.0f blocks flown, total %.1f s, top speed %.0f b/s, %d fireworks, closest clearance %.2f; %s",
                name, f.length, t, f.maxSpeed, f.fireworks, f.minClear, String.join("; ", f.notes)));
        return f.ok ? t : -1;
    }
}
