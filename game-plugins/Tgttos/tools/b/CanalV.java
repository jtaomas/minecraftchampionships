import com.mcm.games.tgttos.maps.b.FrozenCanal;
import com.mcm.games.tgttos.maps.b.util.Boat;
import com.mcm.games.tgttos.maps.b.util.Bs;
import com.mcm.games.tgttos.maps.b.util.Check;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.List;

/** Frozen Canal: boat spawns, lane widths, bridge headroom, SAFE / FAST boat lines. */
final class CanalV {
    private CanalV() {}

    static void run(G g, Check.Report r) {
        TgMapData d = g.d;
        TgMapData go = Check.view(d, Check.afterGo(d, d.blocks));
        // boat spawns
        int okSp = 0;
        double minPair = 99;
        for (Spawn s : d.boatSpawns) {
            int x = (int) Math.floor(s.x()), z = (int) Math.floor(s.z());
            String f = d.blocks.get(x, 0, z);
            boolean ice = f != null && (Bs.id(f).contains("ice"));
            if (ice && Bs.passable(d.blocks.get(x, 1, z)) && Bs.passable(d.blocks.get(x, 2, z)) && d.startStrip.contains(s.x(), s.y(), s.z())) okSp++;
            for (Spawn t : d.boatSpawns) if (t != s) minPair = Math.min(minPair, Math.hypot(t.x() - s.x(), t.z() - s.z()));
        }
        r.check(d.boatSpawns.size() >= 40 && okSp == d.boatSpawns.size() && minPair >= 1.6,
                String.format("%d boat spawns (%d on ice inside the tray with headroom), closest pair %.2f apart (boat is 1.375 wide)", d.boatSpawns.size(), okSp, minPair));

        Region area = Region.of(-50, 0, -2, 50, 0, 160);
        Boat b = new Boat(go, 0, area);
        // canal width in the straights (centre-line cells)
        // canal width quay to quay on rows across the axis-aligned straights (lane surface or thin-ice hole)
        int minW = 99;
        StringBuilder pw = new StringBuilder();
        int[][] rows = {{0, 22}, {24, 64}, {24, 84}, {-6, 132}};
        for (int[] q : rows) {
            int w = 0;
            for (int dir : new int[]{-1, 1})
                for (int x = q[0] + (dir > 0 ? 1 : 0); Math.abs(x - q[0]) < 40; x += dir) {
                    String f = d.blocks.get(x, 0, q[1]);
                    boolean lane = f == null ? d.blocks.get(x, -1, q[1]) == null : Bs.id(f).contains("ice") || Bs.id(f).equals("snow_block");
                    if (!lane) break;
                    w++;
                }
            pw.append(" ").append(w);
            minW = Math.min(minW, w);
        }
        r.check(minW >= 20, "canal at least " + minW + " wide quay to quay on the straights (rows:" + pw + ")");
        int lockW = 0;
        for (int x = 18; x <= 28; x++) if (b.clearance(x, 74) > 0) lockW++;
        r.check(lockW >= 3 && lockW <= 5, "choke: the lock is " + lockW + " wide (13 long); the bypass channel round the island is 8 wide and longer");
        // bridges: headroom exactly 2
        int[][] br = {{26, -12, 12}, {100, -8, 30}, {124, -18, 6}};
        int headOk = 0, cells = 0;
        for (int[] q : br)
            for (int x = q[1]; x <= q[2]; x++)
                for (int z = q[0]; z <= q[0] + 2; z++) {
                    String f = d.blocks.get(x, 0, z);
                    if (f == null || !Bs.id(f).contains("ice") && !Bs.id(f).equals("snow_block")) continue;
                    if (!Bs.passable(d.blocks.get(x, 1, z))) continue;
                    cells++;
                    if (Bs.passable(d.blocks.get(x, 2, z)) && !Bs.passable(d.blocks.get(x, 3, z))) headOk++;
                }
        r.check(cells > 0 && headOk == cells, "3 low footbridges: " + headOk + "/" + cells + " lane cells under them have exactly 2 headroom (boat fits, standing player does not jump over)");
        // holes lead into the void
        int holes = 0, deep = 0;
        for (int x = -40; x <= 50; x++)
            for (int z = 9; z <= 140; z++) {
                if (d.blocks.get(x, 0, z) != null || d.blocks.get(x, -1, z) != null) continue;
                boolean lane = false;
                for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) { String n = d.blocks.get(x + o[0], 0, z + o[1]); if (n != null && Bs.id(n).contains("ice")) lane = true; }
                if (!lane) continue;
                holes++;
                boolean open = true;
                for (int y = -2; y >= d.voidY - 2; y--) if (d.blocks.get(x, y, z) != null) open = false;
                if (open) deep++;
            }
        r.check(holes > 20 && deep == holes, holes + " thin-ice holes beside the lane, " + deep + " drop straight into the void (respawn)");

        List<Region> feats = new ArrayList<>();
        int[][] slush = {{-6, 18, 4, 2}, {5, 26, 3, 2}, {28, 66, 3, 3}, {20, 90, 3, 2}, {-12, 128, 4, 2}, {-1, 124, 3, 2}, {8, 106, 3, 3}};
        for (int[] s : slush) feats.add(Region.of(s[0] - 1, 0, s[1] - 1, s[0] + s[2], 0, s[1] + s[3]));
        feats.add(FrozenCanal.LOCK);
        Region from = Region.of(-21, 0, 0, 20, 0, 8);
        Region to = Region.of(-16, 0, 134, 4, 0, 139);
        r.line("  boat lines (vanilla boat on ice: +0.04 b/tick^2, friction 0.98 ice / 0.989 blue ice / 0.6 snow; braking for bends):");
        if (System.getenv("DEBUGPATH") != null) {
            for (int i = 0; i < d.startSpawns.size(); i += 4) {
                Spawn sp = d.startSpawns.get(i);
                double[] t = g.landmark.get(0);
                double x1 = sp.x(), y1 = sp.y() + 1.62, z1 = sp.z();
                double len = Math.sqrt((t[0] - x1) * (t[0] - x1) + (t[1] - y1) * (t[1] - y1) + (t[2] - z1) * (t[2] - z1));
                int steps = (int) Math.ceil(len / 0.2);
                for (int k = 1; k < steps - 3; k++) {
                    double u = k / (double) steps;
                    int x = (int) Math.floor(x1 + (t[0] - x1) * u), y = (int) Math.floor(y1 + (t[1] - y1) * u), z = (int) Math.floor(z1 + (t[2] - z1) * u);
                    if (!Check.sees(go.blocks, x1, y1, z1, x + 0.5, y + 0.5, z + 0.5 + 0.0) && d.blocks.get(x, y, z) != null && !Bs.passable(d.blocks.get(x, y, z))) {
                        r.line("        dbg eye " + i + " blocked near " + x + "," + y + "," + z + " " + d.blocks.get(x, y, z));
                        break;
                    }
                }
            }
        }
        if (System.getenv("DEBUGPATH") != null)
            for (int zz = 10; zz <= 140; zz += 6) {
                Boat.Line t = b.route(from, Region.of(-50, 0, zz, 50, 0, zz), 3, c -> !FrozenCanal.LOCK.contains(c[0], 0, c[1]), feats);
                r.line("        dbg reach z " + zz + ": " + (t != null));
            }
        if (System.getenv("DEBUGGRID") != null) {
            String[] zr = System.getenv("DEBUGGRID").split(",");
            for (int zz = Integer.parseInt(zr[0]); zz <= Integer.parseInt(zr[1]); zz++) {
                StringBuilder sb = new StringBuilder(String.format("        %4d ", zz));
                for (int xx = -25; xx <= 48; xx++) { int c = b.clearance(xx, zz); sb.append(c == 0 ? (go.blocks.get(xx, 0, zz) == null ? 'o' : '#') : (char) ('0' + Math.min(9, c))); }
                r.line(sb.toString());
            }
        }
        Boat.Line safe = b.route(from, to, 3, c -> !FrozenCanal.LOCK.contains(c[0], 0, c[1]), feats);
        Boat.Line fast = b.route(from, to, 2, c -> true, feats);
        double ts = report(r, "SAFE  middle of the bends (5+ wide), bypass round the lock", safe);
        double tf = report(r, "FAST  blue-ice insides between the holes, through the lock", fast);
        if (ts > 0 && tf > 0) {
            r.check(tf <= ts * 0.8, String.format("fast line saves %.0f%% vs the safe line (>= 20%%)", 100 * (1 - tf / ts)));
            r.line(String.format("  time profile: boats are fast on ice - expert %.0f s fast / %.0f s safe; median with collisions, wall hits and the boat scramble ~%.0f s",
                    tf, ts, ts * 1.8 + 6));
        }
    }

    static double report(Check.Report r, String name, Boat.Line l) {
        if (l == null) { r.err(name + ": NO BOAT LANE"); return -1; }
        r.check(l.maxGapBetweenFeatures() <= 25, String.format("%s: %.0f blocks, %d slush cells, %d blue-ice cells, narrowest %d, longest featureless stretch %d (%s; limit 25), est. %.1f s",
                name, l.length(), l.slushCells(), l.blueCells(), l.minWidth(), l.maxGapBetweenFeatures(), l.featureNote(), l.time()));
        return l.time();
    }
}
