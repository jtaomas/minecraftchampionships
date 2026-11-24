import com.mcm.games.tgttos.maps.b.KiteCourse;
import com.mcm.games.tgttos.maps.b.KiteFestival;
import com.mcm.games.tgttos.maps.b.util.Bs;
import com.mcm.games.tgttos.maps.b.util.Check;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Kite Festival offline runner (only this map, so the shared B report is not overwritten): generic contract
 * checks + KiteV, report out/kite_validation.txt, dump + camera for the renderers, and a profile CSV for the
 * unfolded side view. Usage: java KiteMain outDir [--seed N]
 */
public final class KiteMain {
    public static void main(String[] args) throws Exception {
        Locale.setDefault(Locale.ROOT);
        File out = new File(args.length > 0 ? args[0] : "tools/b/out");
        out.mkdirs();
        long seed = 20261124L;
        for (int i = 1; i < args.length; i++) if (args[i].equals("--seed")) seed = Long.parseLong(args[++i]);
        // twice: the first call warms up the JIT, the second is timed (the runtime builds once at plugin enable)
        new KiteFestival().generate(seed);
        long t0 = System.nanoTime();
        G g = new KiteFestival().generate(seed);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        TgMapData d = g.d;
        Check.Report r = new Check.Report();
        Set<String> types = new HashSet<>();
        d.blocks.forEach((x, y, z, s) -> types.add(Bs.id(s)));
        Region bb = d.blockBounds();
        r.line("TGTTOS Kite Festival v2 - validation report (seed " + seed + ")");
        r.line("=== kite_festival ===");
        r.line(String.format("  %d blocks, %d block types, generated in %d ms, bounds x %d..%d y %d..%d z %d..%d, scenery share %.0f%%; paste >= %d ticks at the BuildQueue cap of 40k blocks/tick (more if the 25 ms/tick budget binds)",
                d.blocks.size(), types.size(), ms, bb.minX(), bb.maxX(), bb.minY(), bb.maxY(), bb.minZ(), bb.maxZ(), g.sceneryShare() * 100, (d.blocks.size() + 39999) / 40000));
        r.check(bb.minX() >= -250 && bb.maxX() <= 250 && bb.minZ() >= -250 && bb.maxZ() <= 250 && bb.minY() >= -60 && bb.maxY() <= 180, "inside the contract box (x/z -250..250, y -60..180)");
        r.check(ms < 1000, "generation under a second (" + ms + " ms)");
        r.check(types.size() >= 25, "material richness: " + types.size() + " block types (>= 25)");
        Check.contract(d, Kit.ELYTRA_FIREWORKS, r);
        Check.start(d, r);
        Check.postPass(d, g.post, r);
        Check.stairs(g, r);
        r.check(d.chickenSpawns.size() >= 8 && d.chickenSpawns.stream().allMatch(c -> d.pen.contains(c.x(), c.y(), c.z())),
                d.chickenSpawns.size() + " chicken spawns inside the pen " + (d.pen.maxX() - d.pen.minX() + 1) + " x " + (d.pen.maxZ() - d.pen.minZ() + 1));
        Check.landmark(d, Check.afterGo(d, d.blocks), g.landmark, r);
        r.line("        (offline ray only: the goal kite is ~330 blocks from the gantry, beyond a normal client view distance)");
        r.line("  kit: elytra + kit.fireworks (2) + " + d.extraItems + " from extraItems = " + (2 + com.mcm.games.tgttos.maps.b.KiteFestival.EXTRA_ROCKETS) + " rockets; no mid-course refills (runtime has no ring/checkpoint hooks)");
        KiteV.run(g, r);
        r.line(r.errors == 0 ? "  RESULT: all checks passed" : "  RESULT: " + r.errors + " problem(s)");
        System.out.print(r.text);
        Files.write(new File(out, "kite_validation.txt").toPath(), r.text.toString().getBytes(StandardCharsets.UTF_8));

        Spawn mid = d.startSpawns.get(d.startSpawns.size() / 2);
        double[] cam = {mid.x(), mid.y() + 1.62, mid.z(), KiteCourse.LA, 150, 90};
        dump(d.blocks, new File(out, "kite_festival.mcmdump.gz"), cam);
        // profile CSV for the unfolded side view
        try (PrintWriter w = new PrintWriter(new File(out, "kite_profile.csv"), StandardCharsets.UTF_8)) {
            w.println("kind,s,a,b,c,label");
            for (int s = 0; s <= (int) KiteCourse.LEN; s++)
                w.printf("plan,%d,%d,%d,%.2f,%.2f%n", s, KiteCourse.floorY(s), KiteCourse.crestY(s), KiteCourse.safeY(s), KiteCourse.fastY(s));
            for (KiteCourse.Screen sc : KiteCourse.SCREENS)
                for (KiteCourse.Hole h : sc.holes()) {
                    int y0 = h.slot() ? KiteCourse.floorY(sc.s()) + 1 : h.y0(), y1 = h.slot() ? KiteCourse.crestY(sc.s()) - 1 : h.y0() + h.h() - 1;
                    w.printf("hole,%.1f,%d,%d,%s,%s%n", sc.s(), y0, y1, h.k(), sc.name());
                }
            for (String nm : new String[]{"safe", "fast"}) {
                KiteGlide.Flight f = nm.equals("safe") ? KiteV.lastSafe : KiteV.lastFast;
                if (f == null) continue;
                for (int i = 0; i < f.pts.size(); i += 2) {
                    double[] p = f.pts.get(i);
                    w.printf("%s,%.1f,%.2f,%.1f,%.2f,%n", nm, KiteCourse.nearest(p[0], p[2])[0], p[1], p[3], i / 20.0);
                }
            }
            for (int i = 0; i < KiteCourse.PIECES.size(); i++) w.printf("piece,%.1f,0,0,0,%s%n", KiteCourse.PIECES.get(i).s0(), KiteCourse.PIECES.get(i).name());
        }
        System.out.println(r.errors == 0 ? "ALL CHECKS PASSED" : r.errors + " validator problem(s)");
        if (r.errors > 0) System.exit(2);
    }

    /** MCMDUMP 1 (gzip, coordinates relative to the min corner) + camera file "ex ey ez tx ty tz" in dump coordinates. */
    static void dump(TgMapData.Blocks b, File f, double[] cam) throws java.io.IOException {
        int[] mn = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, mx = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        b.forEach((x, y, z, s) -> {
            mn[0] = Math.min(mn[0], x); mn[1] = Math.min(mn[1], y); mn[2] = Math.min(mn[2], z);
            mx[0] = Math.max(mx[0], x); mx[1] = Math.max(mx[1], y); mx[2] = Math.max(mx[2], z);
        });
        try (java.io.Writer w = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(f)), StandardCharsets.UTF_8))) {
            w.write("MCMDUMP 1 " + (mx[0] - mn[0] + 1) + " " + (mx[1] - mn[1] + 1) + " " + (mx[2] - mn[2] + 1) + "\n");
            StringBuilder sb = new StringBuilder();
            b.forEach((x, y, z, s) -> sb.append(x - mn[0]).append(' ').append(y - mn[1]).append(' ').append(z - mn[2]).append(' ').append(s).append('\n'));
            w.write(sb.toString());
        }
        String c = String.format(Locale.ROOT, "%.2f %.2f %.2f %.2f %.2f %.2f%n", cam[0] - mn[0], cam[1] - mn[1], cam[2] - mn[2], cam[3] - mn[0], cam[4] - mn[1], cam[5] - mn[2]);
        Files.write(new File(f.getPath().replace(".mcmdump.gz", ".cam")).toPath(), c.getBytes(StandardCharsets.UTF_8));
        System.out.println("  " + f.getName() + ": " + b.size() + " blocks");
    }
}
