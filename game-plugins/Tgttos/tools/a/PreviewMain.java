import com.mcm.games.tgttos.maps.a.util.AMap;
import com.mcm.games.tgttos.maps.a.util.Gen;
import com.mcm.games.tgttos.maps.a.util.Route;
import com.mcm.games.tgttos.maps.a.util.Validate;
import com.mcm.games.tgttos.model.TgMapData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Offline tool for builder-A maps (no Bukkit): generate, validate (report -> outDir/validation.txt), write
 * MCMDUMP files (full map + route overlay) and a camera file for tools/a/view.py.
 * Usage: java PreviewMain outDir seed mapClass...      exit 2 if any validator fails.
 */
public final class PreviewMain {
    public static void main(String[] args) throws Exception {
        File out = new File(args[0]);
        out.mkdirs();
        String seedArg = args[1];   // "live" = each map's in-game seed (Seeds.fixed)
        long seed = seedArg.equals("live") ? Long.MIN_VALUE : Long.parseLong(seedArg);
        StringBuilder all = new StringBuilder("Builder-A TGTTOS maps: validation report (" + (seed == Long.MIN_VALUE ? "in-game seed of each map" : "seed " + seed) + ")\n"
                + "Movement model: walk/sprint 5.6 b/s (median 4.3), step 0.6 / stairs 1, jumps edge-to-edge gap <= 4 flat, -1 per block of rise,\n"
                + "drops reach further (vanilla fall time), bridging 0.30 s/block (median 0.55), pillar 0.40 s/block (median 0.55), ladders 2.35 b/s.\n\n");
        int errors = 0;
        for (int i = 2; i < args.length; i++) {
            AMap m = (AMap) Class.forName(args[i]).getDeclaredConstructor().newInstance();
            long t0 = System.currentTimeMillis();
            Gen g = m.generate(seed == Long.MIN_VALUE ? com.mcm.games.tgttos.model.Seeds.fixed(m.id()) : seed);
            long gen = System.currentTimeMillis() - t0;
            t0 = System.currentTimeMillis();
            Validate v = Validate.run(m, g);
            long val = System.currentTimeMillis() - t0;
            errors += v.errors;
            String txt = v.text.toString().replaceFirst("\n", String.format("  [generate %d ms, validate %d ms, %d blocks]\n", gen, val, g.b.size()));
            all.append(txt).append('\n');
            System.out.print(txt);
            dump(g.d, new File(out, m.id() + ".mcmdump.gz"), null);
            {   // GO state: barrier and drop floor removed (for the start view)
                java.util.Map<TgMapData.Pos, String> saved = new java.util.HashMap<>();
                for (TgMapData.Pos q : g.d.startBarrier) { saved.put(q, g.d.blocks.get(q.x(), q.y(), q.z())); g.d.blocks.set(q.x(), q.y(), q.z(), null); }
                for (TgMapData.Pos q : g.d.dropFloor) { saved.put(q, g.d.blocks.get(q.x(), q.y(), q.z())); g.d.blocks.set(q.x(), q.y(), q.z(), null); }
                dump(g.d, new File(out, m.id() + "_go.mcmdump.gz"), null);
                for (java.util.Map.Entry<TgMapData.Pos, String> e : saved.entrySet()) g.d.blocks.set(e.getKey().x(), e.getKey().y(), e.getKey().z(), e.getValue());
            }
            List<int[]> marks = new ArrayList<>();
            for (Map.Entry<String, Route.Result> e : v.lineResults.entrySet()) {
                String mat = e.getKey().startsWith("FAST") ? "minecraft:red_stained_glass" : "minecraft:lime_stained_glass";
                int code = e.getKey().startsWith("FAST") ? 1 : 2;
                for (Route.Step s : e.getValue().path) marks.add(new int[]{s.x(), s.y(), s.z(), code});
            }
            dump(g.d, new File(out, m.id() + "_routes.mcmdump.gz"), marks);
            StringBuilder ps = new StringBuilder();
            for (Map.Entry<String, Route.Result> e : v.lineResults.entrySet()) {
                ps.append("== ").append(e.getKey()).append(System.lineSeparator());
                for (Route.Step s : e.getValue().path) ps.append(String.format("%4d %4d %4d  h=%.1f %-7s gap=%.1f rise=%.1f t=%.2f%n", s.x(), s.y(), s.z(), s.h(), s.how(), s.gap(), s.rise(), s.t()));
            }
            Files.write(new File(out, m.id() + "_paths.txt").toPath(), ps.toString().getBytes(StandardCharsets.UTF_8));
            TgMapData.Region st = g.d.startStrip, pen = g.d.pen;
            String cam = String.format(java.util.Locale.ROOT, "{\"start\":[%.1f,%.1f,%.1f],\"look\":[%.1f,%.1f,%.1f],\"min\":[%d,%d,%d],\"pitchScale\":%.2f}",
                    (st.minX() + st.maxX() + 1) / 2.0, st.minY() + 1.62 + 0.5, m.type() == com.mcm.games.tgttos.model.MapType.DROP ? st.maxZ() + 3.5 : st.minZ() + 1.0,
                    pen.centerX(), (double) pen.minY() + 4, pen.centerZ(), minX, minY, minZ,
                    m.type() == com.mcm.games.tgttos.model.MapType.DROP ? 0.8 : 0.35);
            Files.write(new File(out, m.id() + ".cam.json").toPath(), cam.getBytes(StandardCharsets.UTF_8));
        }
        Files.write(new File(out, "validation.txt").toPath(), all.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(errors == 0 ? "ALL PASS" : errors + " validator problems");
        if (errors > 0) System.exit(2);
    }

    static int minX, minY, minZ;

    static void dump(TgMapData d, File f, List<int[]> marks) throws IOException {
        List<int[]> pos = new ArrayList<>();
        List<String> st = new ArrayList<>();
        int[] mn = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, mx = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        java.util.Map<Long, String> over = new java.util.HashMap<>();
        if (marks != null) for (int[] m : marks) over.put(TgMapData.SparseBlocks.key(m[0], m[1], m[2]), m[3] == 1 ? "minecraft:red_stained_glass" : "minecraft:lime_stained_glass");
        d.blocks.forEach((x, y, z, s) -> {
            pos.add(new int[]{x, y, z});
            st.add(s);
            mn[0] = Math.min(mn[0], x); mn[1] = Math.min(mn[1], y); mn[2] = Math.min(mn[2], z);
            mx[0] = Math.max(mx[0], x); mx[1] = Math.max(mx[1], y); mx[2] = Math.max(mx[2], z);
        });
        for (Map.Entry<Long, String> e : over.entrySet()) {
            long k = e.getKey();
            pos.add(new int[]{TgMapData.SparseBlocks.kx(k), TgMapData.SparseBlocks.ky(k), TgMapData.SparseBlocks.kz(k)});
            st.add(e.getValue());
        }
        minX = mn[0]; minY = mn[1]; minZ = mn[2];
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)), StandardCharsets.UTF_8))) {
            w.write("MCMDUMP 1 " + (mx[0] - mn[0] + 1) + " " + (mx[1] - mn[1] + 1) + " " + (mx[2] - mn[2] + 1) + "\n");
            for (int i = 0; i < pos.size(); i++) {
                int[] p = pos.get(i);
                if (p[0] < mn[0] || p[1] < mn[1] || p[2] < mn[2] || p[0] > mx[0] || p[1] > mx[1] || p[2] > mx[2]) continue;
                w.write((p[0] - mn[0]) + " " + (p[1] - mn[1]) + " " + (p[2] - mn[2]) + " " + st.get(i) + "\n");
            }
        }
    }
}
