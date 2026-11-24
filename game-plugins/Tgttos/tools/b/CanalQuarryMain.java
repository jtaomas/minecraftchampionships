import com.mcm.games.tgttos.maps.b.FrozenCanal;
import com.mcm.games.tgttos.maps.b.QuarrySteps;
import com.mcm.games.tgttos.maps.b.util.Bs;
import com.mcm.games.tgttos.maps.b.util.Check;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Offline tool for Frozen Canal and Quarry Steps only (no Bukkit): generate, validate (common checks + CanalV /
 * QuarryV), dump for the renderers. Usage: java CanalQuarryMain outDir [frozen_canal] [quarry_steps] [--seed N].
 * Report: outDir/canal_quarry_validation.txt. Exit code 2 if any check fails.
 */
public final class CanalQuarryMain {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "tools/b/out");
        out.mkdirs();
        long seed = 20261124L;
        List<String> only = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--seed")) seed = Long.parseLong(args[++i]);
            else only.add(args[i]);
        }
        StringBuilder all = new StringBuilder("TGTTOS Frozen Canal + Quarry Steps - validation report (seed " + seed + ")\n\n");
        int errors = 0;
        for (String id : new String[]{"frozen_canal", "quarry_steps"}) {
            if (!only.isEmpty() && !only.contains(id)) continue;
            long t0 = System.nanoTime();
            G g;
            List<int[]> decoys = List.of();
            Kit kit;
            if (id.equals("frozen_canal")) { FrozenCanal m = new FrozenCanal(); g = m.generate(seed); kit = m.kit(); decoys = m.decoyChickens(); }
            else { QuarrySteps m = new QuarrySteps(); g = m.generate(seed); kit = m.kit(); decoys = m.decoyChickens(); }
            long ms = (System.nanoTime() - t0) / 1_000_000;
            Check.Report r = new Check.Report();
            double[] cam = common(id, g, kit, decoys, ms, r);
            if (id.equals("frozen_canal")) CanalV.run(g, r); else QuarryV.run(g, decoys, r);
            r.line(r.errors == 0 ? "  RESULT: all checks passed" : "  RESULT: " + r.errors + " problem(s)");
            errors += r.errors;
            all.append(r.text).append('\n');
            System.out.print(r.text);
            dump(g.d, null, new File(out, id + ".mcmdump.gz"), cam);
            if (id.equals("quarry_steps")) dump(g.d, QuarryV.withDecoys(g.d, decoys), new File(out, id + "_decoys.mcmdump.gz"), null);
        }
        Files.write(new File(out, "canal_quarry_validation.txt").toPath(), all.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(errors == 0 ? "ALL CHECKS PASSED" : errors + " validator problem(s)");
        if (errors > 0) System.exit(2);
    }

    /** Dump of the blocks (or an override store) plus a camera file (dump-relative), same format as PreviewMain. */
    static void dump(TgMapData d, TgMapData.Blocks override, File f, double[] cam) throws java.io.IOException {
        TgMapData.Blocks b = override != null ? override : d.blocks;
        List<int[]> pos = new ArrayList<>();
        List<String> st = new ArrayList<>();
        int[] mn = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, mx = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        b.forEach((x, y, z, s) -> {
            pos.add(new int[]{x, y, z});
            st.add(s);
            mn[0] = Math.min(mn[0], x); mn[1] = Math.min(mn[1], y); mn[2] = Math.min(mn[2], z);
            mx[0] = Math.max(mx[0], x); mx[1] = Math.max(mx[1], y); mx[2] = Math.max(mx[2], z);
        });
        try (java.io.Writer w = new java.io.BufferedWriter(new java.io.OutputStreamWriter(new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(f)), StandardCharsets.UTF_8))) {
            w.write("MCMDUMP 1 " + (mx[0] - mn[0] + 1) + " " + (mx[1] - mn[1] + 1) + " " + (mx[2] - mn[2] + 1) + "\n");
            for (int i = 0; i < pos.size(); i++) {
                int[] p = pos.get(i);
                w.write((p[0] - mn[0]) + " " + (p[1] - mn[1]) + " " + (p[2] - mn[2]) + " " + st.get(i) + "\n");
            }
        }
        if (cam != null) {
            String c = String.format(java.util.Locale.ROOT, "%.2f %.2f %.2f %.2f %.2f %.2f%n", cam[0] - mn[0], cam[1] - mn[1], cam[2] - mn[2],
                    cam[3] - mn[0], cam[4] - mn[1], cam[5] - mn[2]);
            Files.write(new File(f.getPath().replace(".mcmdump.gz", ".cam")).toPath(), c.getBytes(StandardCharsets.UTF_8));
        }
        System.out.println("  " + f.getName() + ": " + pos.size() + " blocks");
    }

    /** Common checks (same set as Validators.run) plus decoys; returns the start-view camera. */
    static double[] common(String id, G g, Kit kit, List<int[]> decoys, long ms, Check.Report r) {
        TgMapData d = g.d;
        Set<String> types = new HashSet<>();
        d.blocks.forEach((x, y, z, s) -> types.add(Bs.id(s)));
        Region bb = d.blockBounds();
        r.line("=== " + id + " ===");
        r.line(String.format("  %d blocks, %d block types, generated in %d ms, bounds x %d..%d y %d..%d z %d..%d, scenery share %.0f%%",
                d.blocks.size(), types.size(), ms, bb.minX(), bb.maxX(), bb.minY(), bb.maxY(), bb.minZ(), bb.maxZ(), g.sceneryShare() * 100));
        r.check(bb.minX() >= -250 && bb.maxX() <= 250 && bb.minZ() >= -250 && bb.maxZ() <= 250 && bb.minY() >= -60 && bb.maxY() <= 180, "inside the contract box (x/z -250..250, y -60..180)");
        r.check(types.size() >= 25, "material richness: " + types.size() + " block types (>= 25)");
        r.check(bb.minX() >= d.playBounds.minX() && bb.maxX() <= d.playBounds.maxX() && bb.minZ() >= d.playBounds.minZ() && bb.maxZ() <= d.playBounds.maxZ(),
                "every block inside playBounds x/z (spectators can see it all)");
        Check.contract(d, kit, r);
        Check.start(d, r);
        Check.postPass(d, g.post, r);
        Check.stairs(g, r);
        r.check(d.chickenSpawns.size() >= 8 && d.chickenSpawns.stream().allMatch(c -> d.pen.contains(c.x(), c.y(), c.z())),
                d.chickenSpawns.size() + " goal chicken spawns inside the pen " + (d.pen.maxX() - d.pen.minX() + 1) + " x " + (d.pen.maxZ() - d.pen.minZ() + 1));
        // decoys: inside play bounds, never inside the real pen, standing on a block with headroom
        int in = 0, inPen = 0, standing = 0;
        for (int[] p : decoys) {
            if (d.playBounds.contains(p[0] + 0.5, p[1], p[2] + 0.5)) in++;
            if (d.pen.contains(p[0] + 0.5, p[1], p[2] + 0.5)) inPen++;
            if (Bs.passable(d.blocks.get(p[0], p[1], p[2])) && Bs.passable(d.blocks.get(p[0], p[1] + 1, p[2])) && Bs.standTop(d.blocks.get(p[0], p[1] - 1, p[2])) > 0) standing++;
        }
        if (!decoys.isEmpty())
            r.check(in == decoys.size() && inPen == 0 && standing == decoys.size(),
                    decoys.size() + " decoy chickens: " + in + " inside playBounds, " + inPen + " inside the real pen, " + standing + " standing on a block with headroom");
        else r.line("  --    no decoy chickens on this map");
        Spawn mid = d.startSpawns.get(d.startSpawns.size() / 2);
        Check.landmark(d, Check.afterGo(d, d.blocks), g.landmark, r);
        double[] t = g.landmark.get(0);
        return new double[]{mid.x(), mid.y() + 1.62, mid.z(), t[0], t[1], t[2]};
    }
}
