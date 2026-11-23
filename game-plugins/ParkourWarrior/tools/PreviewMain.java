package com.mcm.games.parkourwarrior.map;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Offline Parkour Warrior course tool (no Bukkit): builds the course, runs the validators (report:
 * out/validation.txt), writes MCMDUMP files for the renderer and a render list (out/renders.txt: dump png args).
 * Usage: java PreviewMain outDir [seed]. Exit code 2 if a validator fails.
 */
public final class PreviewMain {
    interface Keep { boolean keep(int x, int y, int z); }

    static final List<String> renders = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "tools/out");
        File dd = new File(out, "dumps");
        dd.mkdirs();
        long seed = args.length > 1 ? Long.parseLong(args[1]) : 20261124L;
        long t0 = System.currentTimeMillis();
        W w = CourseMap.build(seed);
        Validate.Report r = Validate.run(w);
        String txt = r.text + "\n(generated + validated in " + (System.currentTimeMillis() - t0) + " ms)\n";
        Files.write(new File(out, "validation.txt").toPath(), txt.getBytes(StandardCharsets.UTF_8));
        System.out.print(txt);
        Map<Long, String> timed = new HashMap<>();
        for (Ob o : w.obs) timed.putAll(o.timedPreview);
        dump(w, timed, new File(dd, "full.mcmdump.gz"), (x, y, z) -> true);
        renders.add("full.mcmdump.gz course_top.png --top --scale 3");
        renders.add("full.mcmdump.gz course_iso.png --angle 1");
        var hubs = w.model.hubs;
        for (int s = 1; s <= 5; s++) {
            int hz = (int) Math.floor(hubs.get(s - 1).respawn().z()) - 4;
            int nz = s < 5 ? (int) Math.floor(hubs.get(s).respawn().z()) - 4 : (int) Math.floor(w.model.fork.z());
            int a = hz - 12, b = nz - 6;
            dump(w, timed, new File(dd, "zone" + s + ".mcmdump.gz"), (x, y, z) -> z >= a && z <= b && x >= -40 && x <= 110);
            renders.add("zone" + s + ".mcmdump.gz zone" + s + ".png --angle 1");
            int hx = (int) Math.floor(hubs.get(s - 1).respawn().x());
            dump(w, timed, new File(dd, "hub" + s + ".mcmdump.gz"), (x, y, z) -> Math.abs(x - hx) <= 14 && Math.abs(z - hz) <= 14);
            renders.add("hub" + s + ".mcmdump.gz hub" + s + ".png --angle 1");
        }
        int fz = (int) Math.floor(w.model.fork.z()) - 2, fx = (int) Math.floor(w.model.fork.x());
        dump(w, timed, new File(dd, "fork.mcmdump.gz"), (x, y, z) -> Math.abs(x - fx) <= 24 && z >= fz - 12 && z <= fz + 12);
        renders.add("fork.mcmdump.gz fork.png --angle 1");
        int k = 0;
        for (String e : new String[]{"easy", "medium", "hard"}) {
            int gx = fx + Frame.GATES[k++];
            dump(w, timed, new File(dd, "ending_" + e + ".mcmdump.gz"), (x, y, z) -> Math.abs(x - gx) <= 9 && z >= fz + 4 && z <= fz + 60);
            renders.add("ending_" + e + ".mcmdump.gz ending_" + e + ".png --angle 1");
        }
        dump(w, timed, new File(dd, "endings.mcmdump.gz"), (x, y, z) -> Math.abs(x - fx) <= 26 && z >= fz - 10 && z <= fz + 60);
        renders.add("endings.mcmdump.gz endings_all.png --angle 1");
        for (Ob o : w.obs) {
            int[] bb = Validate.bbox(o);
            int m = 6;
            dump(w, timed, new File(dd, "ob_" + o.id + ".mcmdump.gz"), (x, y, z) -> x >= bb[0] - m && x <= bb[3] + m && z >= bb[2] - m && z <= bb[5] + m
                    && y >= bb[1] - 14 && y <= bb[4] + 14);
            renders.add("ob_" + o.id + ".mcmdump.gz ob_" + o.id + ".png --angle " + (o.q == 3 ? 0 : 1));
        }
        Files.write(new File(out, "renders.txt").toPath(), (String.join("\n", renders) + "\n").getBytes(StandardCharsets.UTF_8));
        System.out.println("dumps written to " + dd + (r.errors == 0 ? "" : "  (" + r.errors + " validator problems)"));
        if (r.errors > 0) System.exit(2);
    }

    static void dump(W w, Map<Long, String> timed, File f, Keep k) throws IOException {
        List<int[]> pos = new ArrayList<>();
        List<String> st = new ArrayList<>();
        int[] mn = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}, mx = {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        w.b.forEach((x, y, z, s) -> {
            if (!k.keep(x, y, z)) return;
            pos.add(new int[]{x, y, z}); st.add(s);
        });
        for (Map.Entry<Long, String> e : timed.entrySet()) {
            int x = W.kx(e.getKey()), y = W.ky(e.getKey()), z = W.kz(e.getKey());
            if (!k.keep(x, y, z)) continue;
            pos.add(new int[]{x, y, z}); st.add(e.getValue());
        }
        if (pos.isEmpty()) return;
        for (int[] p : pos) for (int i = 0; i < 3; i++) { mn[i] = Math.min(mn[i], p[i]); mx[i] = Math.max(mx[i], p[i]); }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)), StandardCharsets.UTF_8))) {
            bw.write("MCMDUMP 1 " + (mx[0] - mn[0] + 1) + " " + (mx[1] - mn[1] + 1) + " " + (mx[2] - mn[2] + 1) + "\n");
            for (int i = 0; i < pos.size(); i++) {
                int[] p = pos.get(i);
                bw.write((p[0] - mn[0]) + " " + (p[1] - mn[1]) + " " + (p[2] - mn[2]) + " " + st.get(i) + "\n");
            }
        }
    }
}
