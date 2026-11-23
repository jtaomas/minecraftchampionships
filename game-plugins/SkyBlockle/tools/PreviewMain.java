import com.mcm.games.skyblockle.MapModel;
import com.mcm.games.skyblockle.map.SkyMap;
import com.mcm.games.skyblockle.map.Validate;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

/**
 * Offline Sky Blockle map tool (no Bukkit): generates the map, runs all validators (also for every side count
 * 2..10), writes the validation report and MCMDUMP files for the renderer.
 * Usage: java PreviewMain outDir [wedges] [seed]
 * Exit code 2 if any validator fails.
 */
public final class PreviewMain {
    interface Keep { boolean keep(int x, int y, int z); }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "target/offline");
        out.mkdirs();
        int n = args.length > 1 ? Integer.parseInt(args[1]) : 10;
        long seed = args.length > 2 ? Long.parseLong(args[2]) : 20261123L;
        StringBuilder all = new StringBuilder();
        int errors = 0;
        SkyMap.Build main = null;
        for (int w = 2; w <= 10; w++) {
            long t0 = System.currentTimeMillis();
            SkyMap.Build b = SkyMap.build(w, seed);
            Validate.Report r = Validate.run(b);
            errors += r.errors;
            String head = "=== " + w + " wedges (" + (System.currentTimeMillis() - t0) + " ms) ===\n";
            all.append(head).append(r.text).append('\n');
            System.out.print(w == n ? head + r.text : head + summary(r.text.toString()));
            if (w == n) main = b;
        }
        Files.write(new File(out, "validation.txt").toPath(), all.toString().getBytes(StandardCharsets.UTF_8));
        MapModel m = main.model;
        dump(m, new File(out, "map_full.mcmdump.gz"), (x, y, z) -> true);
        double half = Math.toRadians(180.0 / n + 2);
        dump(m, new File(out, "wedge0.mcmdump.gz"), (x, y, z) -> {
            double d = Math.hypot(x, z);
            if (d < 20) return false;
            double a = Math.atan2(-x, z);                 // wedge 0 axis = 0 (towards +Z)
            return Math.abs(a) <= half;
        });
        MapModel.Spawn s0 = m.spawnsFor(0).get(0);
        int sx = (int) Math.floor(s0.x()), sz = (int) Math.floor(s0.z());
        dump(m, new File(out, "start0.mcmdump.gz"), (x, y, z) -> Math.abs(x - sx) <= 9 && z - sz >= -7 && z - sz <= 17);
        dump(m, new File(out, "castle.mcmdump.gz"), (x, y, z) -> Math.abs(x) <= 30 && Math.abs(z) <= 30);
        dump(m, new File(out, "castle_cut.mcmdump.gz"), (x, y, z) -> Math.abs(x) <= 30 && Math.abs(z) <= 30 && !(x >= 0 && z >= 0 && y >= 1));
        dump(m, new File(out, "castle_slice.mcmdump.gz"), (x, y, z) -> Math.abs(x) <= 30 && z >= 0 && z <= 30);
        System.out.println("dumps written to " + out + (errors == 0 ? "" : "  (" + errors + " validator problems)"));
        if (errors > 0) System.exit(2);
    }

    private static String summary(String t) {
        StringBuilder sb = new StringBuilder();
        for (String l : t.split("\n")) if (l.contains("PATH GAPS") || l.contains("RESULT") || l.contains("ERROR") || l.startsWith("Sky Blockle")) sb.append(l).append('\n');
        return sb.toString();
    }

    static void dump(MapModel m, File f, Keep k) throws IOException {
        List<int[]> pos = new ArrayList<>();
        List<String> st = new ArrayList<>();
        int[] mn = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, mx = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        m.blocks.forEach((x, y, z, s) -> {
            if (!k.keep(x, y, z)) return;
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
        System.out.println("  " + f.getName() + ": " + pos.size() + " blocks");
    }
}
