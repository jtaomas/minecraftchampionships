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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Offline route tool for imported sections ({@link ImportedCourse}). Every section is placed on its own in its
 * section-local frame (start block at 0,0,0 with a 3x3 pad, completion pad at its end), the easiest route from the
 * start pad to the end pad is searched ({@link Router}), every jump is simulated ({@link JumpSim}), impossible jumps
 * are banned and the search repeated. Writes {@code importDir/routes.txt} (section-local nodes) and a report and
 * marker dumps into outDir. The full course check is still {@code Validate} (PreviewMain) on the assembled course.
 * Usage: java ImportRoute importDir outDir [SLOT ...]   (no slots = every section; others keep their old routes)
 */
public final class ImportRoute {
    public static void main(String[] args) throws Exception {
        File dir = new File(args[0]);
        File out = new File(args[1]);
        new File(out, "dumps").mkdirs();
        List<String> only = new ArrayList<>();
        for (int i = 2; i < args.length; i++) only.add(args[i].toUpperCase(Locale.ROOT));
        ImportedCourse ic = ImportedCourse.load(dir);
        if (ic == null) { System.out.println("no course.txt in " + dir); System.exit(1); }
        for (String p : ic.problems) System.out.println("PROBLEM " + p);
        Map<String, List<String>> keep = new LinkedHashMap<>();
        File rf = new File(dir, "routes.txt");
        if (rf.isFile()) for (String l : Files.readAllLines(rf.toPath(), StandardCharsets.UTF_8)) {
            String[] t = l.trim().split("\\s+");
            if (t.length > 1 && !l.startsWith("#")) keep.computeIfAbsent(t[1], k -> new ArrayList<>()).add(l);
        }
        StringBuilder rep = new StringBuilder();
        for (ImportedCourse.Section s : ic.sections.values()) {
            if (!only.isEmpty() && !only.contains(s.slot)) continue;
            long t0 = System.currentTimeMillis();
            W w = new W(1);
            Map<Long, String> bl = ic.blocks(s);
            int[] bb = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (Map.Entry<Long, String> b : bl.entrySet()) {
                long k = b.getKey();
                int x = W.kx(k), y = W.ky(k), z = W.kz(k);
                if (z < -1) continue;    // as in the course: nothing behind the start pad
                w.set(x, y, z, b.getValue());
                int[] a = {x, y, z};
                for (int q = 0; q < 3; q++) { bb[q] = Math.min(bb[q], a[q]); bb[q + 3] = Math.max(bb[q + 3], a[q]); }
            }
            int[] e = s.endLocal();
            List<int[]> start = pad(w, 0, 0, 0), goal = pad(w, e[0], e[1], e[2]);
            int killY = Math.min(1, e[1] + 1) - s.killDepth;
            int[] box = {Math.min(bb[0], -2), killY - 1, Math.min(bb[2], -2), Math.max(bb[3], 2), Math.max(bb[4], e[1] + 4), Math.max(bb[5], e[2] + 2)};
            Phys.World wd = new Phys.World(w, new HashMap<>());
            List<int[]> via = new ArrayList<>(), avoid = new ArrayList<>();
            for (int[] v : s.via) via.add(s.toLocal(v[0], v[1], v[2]));
            for (int[] a : s.avoid) {
                int[] p = s.toLocal(a[0], a[1], a[2]), q = s.toLocal(a[3], a[4], a[5]);
                avoid.add(new int[]{p[0], p[1], p[2], q[0], q[1], q[2]});
            }
            Router.Grid gr = new Router.Grid(wd, box, killY, avoid);
            Router.Bans bans = new Router.Bans();
            Router.Result res = null;
            List<JumpSim.Res> sims = null;
            String err = null;
            int iter;
            JumpSim.Kill kill = new JumpSim.Kill(killY, new ArrayList<>(), new ArrayList<>());
            JumpSim.Cfg cfg = new JumpSim.Cfg();
            cfg.allowLate = true;
            for (iter = 0; iter < 60; iter++) {
                res = Router.route(gr, start, via, goal, bans);
                if (res.error != null) { err = res.error; break; }
                sims = new ArrayList<>();
                List<Phys.P> prev = null;
                int bad = -1;
                for (int i = 1; i < res.nodes.size(); i++) {
                    JumpSim.Res r = JumpSim.eval(wd, res.nodes.get(i - 1), List.of(res.nodes.get(i)), kill, cfg, prev);
                    prev = r.arrivals;
                    sims.add(r);
                    if (r.best == 0) { bad = i - 1; break; }
                }
                if (bad < 0) break;
                Router.Cell[] j = res.jumps.get(bad);
                bans.ban(j[0].key(), j[1].key());
                for (int[] a : res.nodes.get(bad).cells) for (int[] b : res.nodes.get(bad + 1).cells)
                    bans.ban(W.key(a[0], a[1], a[2]), W.key(b[0], b[1], b[2]));
            }
            rep.append(String.format(Locale.ROOT, "%s %s  (%d blocks, end %d,%d,%d)  grid %d cells  %d reroutes  %d ms%n", s.slot, s.name, bl.size(),
                    e[0], e[1], e[2], res == null ? 0 : res.cells, iter, System.currentTimeMillis() - t0));
            if (err != null || res == null || sims == null) {
                rep.append("  ERROR ").append(err).append('\n');
                int[] gc = via.isEmpty() ? goal.get(4) : via.get(via.size() - 1);
                StringBuilder fr = new StringBuilder("  reachable cells closest to the target (section-local):");
                for (Router.Cell c : Router.frontier(gr, start, gc[0], gc[1], gc[2], 8)) fr.append(' ').append(c.x).append(',').append(c.y).append(',').append(c.z);
                rep.append(fr).append('\n');
                keep.remove(s.slot);
                continue;
            }
            double pass = 1;
            int hardW = 99, hardB = 99;
            List<String> lines = new ArrayList<>();
            for (Ob.Node n : res.nodes) {
                StringBuilder sb = new StringBuilder("node " + s.slot + " " + (n.t == Ob.T.LADDER ? "L" : n.t == Ob.T.ICE ? "I" : "A"));
                for (int[] c : n.cells) sb.append(' ').append(c[0]).append(',').append(c[1]).append(',').append(c[2]);
                lines.add(sb.toString());
            }
            for (int i = 0; i < sims.size(); i++) {
                JumpSim.Res r = sims.get(i);
                Router.Cell[] j = res.jumps.get(i);
                rep.append(String.format(Locale.ROOT, "  %2d %-48s window %d/%d%s   %d,%d,%d -> %d,%d,%d%n", i + 1, r.describe(), r.worst, r.best,
                        r.trivial ? " trivial" : "", j[0].x, j[0].y, j[0].z, j[1].x, j[1].y, j[1].z));
                if (!r.trivial) {
                    pass *= 1 - Validate.failP((r.worst + r.best) / 2.0);
                    if (r.worst * 10 + r.best < hardW * 10 + hardB) { hardW = r.worst; hardB = r.best; }
                }
            }
            rep.append(String.format(Locale.ROOT, "  steps %d, hardest window %s, est. attempts %.1f, path cells %d%n", sims.size(),
                    hardW == 99 ? "-" : hardW + "/" + hardB, 1 / Math.max(1e-6, pass), res.path.size()));
            keep.put(s.slot, lines);
            markerDump(w, res, sims, killY, new File(out, "dumps/route_" + s.slot + ".mcmdump.gz"));
        }
        try (BufferedWriter bw = Files.newBufferedWriter(rf.toPath(), StandardCharsets.UTF_8)) {
            bw.write("# written by tools/ImportRoute (Parkour Warrior): simulated foothold route of every imported section, section-local.\n");
            for (List<String> l : keep.values()) for (String x : l) { bw.write(x); bw.write('\n'); }
        }
        Files.write(new File(out, "import_routes.txt").toPath(), rep.toString().getBytes(StandardCharsets.UTF_8));
        System.out.print(rep);
    }

    /** 3x3 pad centred on (x, y, z) with 3 blocks of headroom (as in the course); returns its cells. */
    static List<int[]> pad(W w, int x, int y, int z) {
        List<int[]> c = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            w.set(x + dx, y, z + dz, W.PAD);
            for (int h = 1; h <= 3; h++) {
                String s = w.get(x + dx, y + h, z + dz);
                if (!Bs.isAir(s) && !Bs.passable(s)) w.set(x + dx, y + h, z + dz, null);
            }
            c.add(new int[]{x + dx, y, z + dz});
        }
        return c;
    }

    /** Section around the route; path cells coloured: walk = white, jump landings by window, takeoffs light blue. */
    static void markerDump(W w, Router.Result res, List<JumpSim.Res> sims, int killY, File f) throws IOException {
        int[] bb = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (Router.Cell c : res.path) {
            int[] a = c.arr();
            for (int k = 0; k < 3; k++) { bb[k] = Math.min(bb[k], a[k]); bb[k + 3] = Math.max(bb[k + 3], a[k]); }
        }
        Map<Long, String> mark = new HashMap<>();
        for (Router.Cell c : res.path) if (!c.ladder) mark.put(c.key(), "minecraft:white_concrete");
        for (int i = 0; i < res.jumps.size() && i < sims.size(); i++) {
            JumpSim.Res s = sims.get(i);
            Router.Cell from = res.jumps.get(i)[0], to = res.jumps.get(i)[1];
            String col = s.trivial ? "white" : s.worst >= 4 ? "lime" : s.worst >= 2 ? "yellow" : s.worst >= 1 ? "orange" : s.best >= 1 ? "red" : "black";
            if (!to.ladder) mark.put(to.key(), "minecraft:" + col + "_concrete");
            if (!from.ladder) mark.put(from.key(), "minecraft:light_blue_concrete");
        }
        int m = 6, x0 = bb[0] - m, x1 = bb[3] + m, z0 = bb[2] - m, z1 = bb[5] + m, y0 = Math.min(bb[1], killY) - 4, y1 = bb[4] + 3;
        List<String> rows = new ArrayList<>();
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) {
            String s = mark.getOrDefault(W.key(x, y, z), w.get(x, y, z));
            if (s == null || Bs.isAir(s)) continue;
            rows.add((x - x0) + " " + (y - y0) + " " + (z - z0) + " " + s);
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)), StandardCharsets.UTF_8))) {
            bw.write("MCMDUMP 1 " + (x1 - x0 + 1) + " " + (y1 - y0 + 1) + " " + (z1 - z0 + 1) + "\n");
            for (String r : rows) { bw.write(r); bw.write('\n'); }
        }
    }
}
