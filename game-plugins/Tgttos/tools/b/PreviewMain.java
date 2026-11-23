import com.mcm.games.tgttos.maps.b.FrozenCanal;
import com.mcm.games.tgttos.maps.b.KiteFestival;
import com.mcm.games.tgttos.maps.b.QuarrySteps;
import com.mcm.games.tgttos.maps.b.TicketGates;
import com.mcm.games.tgttos.maps.b.util.G;
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
 * Offline tool for the B maps (no Bukkit): generates each map, runs all validators (report: out/validation.txt),
 * writes MCMDUMP files + camera files for the renderers. Usage: java PreviewMain outDir [mapId ...] [--seed N]
 * Exit code 2 if any validator fails.
 */
public final class PreviewMain {
    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "tools/b/out");
        out.mkdirs();
        long seed = 20261124L;
        List<String> only = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--seed")) seed = Long.parseLong(args[++i]);
            else only.add(args[i]);
        }
        StringBuilder all = new StringBuilder("TGTTOS maps, builder B - validation report (seed " + seed + ")\n\n");
        int errors = 0;
        Object[][] maps = {{new KiteFestival(), "kite_festival"}, {new FrozenCanal(), "frozen_canal"},
                {new TicketGates(), "ticket_gates"}, {new QuarrySteps(), "quarry_steps"}};
        for (Object[] m : maps) {
            String id = (String) m[1];
            if (!only.isEmpty() && !only.contains(id)) continue;
            long t0 = System.nanoTime();
            G g = switch (id) {
                case "kite_festival" -> ((KiteFestival) m[0]).generate(seed);
                case "frozen_canal" -> ((FrozenCanal) m[0]).generate(seed);
                case "ticket_gates" -> ((TicketGates) m[0]).generate(seed);
                default -> ((QuarrySteps) m[0]).generate(seed);
            };
            long ms = (System.nanoTime() - t0) / 1_000_000;
            Validators.Out r = Validators.run(id, g, ms);
            errors += r.rep.errors;
            all.append(r.rep.text).append('\n');
            System.out.print(r.rep.text);
            dump(g.d, null, new File(out, id + ".mcmdump.gz"), r.cam);
            if (r.extraDumps != null)
                for (Map.Entry<String, TgMapData.Blocks> e : r.extraDumps.entrySet())
                    dump(g.d, e.getValue(), new File(out, id + "_" + e.getKey() + ".mcmdump.gz"), null);
        }
        Files.write(new File(out, "validation.txt").toPath(), all.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println(errors == 0 ? "ALL CHECKS PASSED" : errors + " validator problem(s)");
        if (errors > 0) System.exit(2);
    }

    /** Write a dump of the blocks (or an override store) plus a camera file (dump-relative coordinates). */
    static void dump(TgMapData d, TgMapData.Blocks override, File f, double[] cam) throws IOException {
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
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)), StandardCharsets.UTF_8))) {
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
}
