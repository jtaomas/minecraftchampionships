package com.mcm.games.parkourwarrior.map;

import com.mcm.games.parkourwarrior.CourseModel;
import com.mcm.games.parkourwarrior.CourseModel.Pos;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A replica world imported 1:1 (runtime data in {@code maps/<id>/}, never in the jar or the repo): the whole block file as it
 * is, one single line of sections, with the checkpoints its command blocks implemented now done by the plugin. Written by
 * {@code tools/mcc6_import.py}. {@code course.txt} (first directive {@code format linear}):
 *
 * <pre>
 * format linear
 * map ID / title TEXT / credit TEXT / blocks FILE.mcmi.gz   (MCMI v1, coordinates are the course coordinates)
 * fall Y                           feet below Y = a fall -> back to the last checkpoint
 * start X Y Z YAW                  spawn (and respawn before checkpoint 1)
 * section K NAME...                name of section K (1..N, 3 per stage)
 * zone S NAME...                   stage name
 * cp N box X0 Y0 Z0 X1 Y1 Z1 respawn X Y Z YAW   checkpoint N: the exact volume (doubles) and where it respawns
 * finish box X0 Y0 Z0 X1 Y1 Z1     the volume that finishes the run
 * overview NAME... X Y Z PITCH     spectator view
 * sign X Y Z L1|L2|L3|L4           sign text (front)
 * </pre>
 */
public final class FullCourse {
    private FullCourse() {}

    /** True when {@code dir/course.txt} starts with {@code format linear}. */
    public static boolean isLinear(File dir) {
        if (dir == null) return false;
        File f = new File(dir, "course.txt");
        if (!f.isFile()) return false;
        try {
            for (String raw : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
                String l = raw.strip();
                if (!l.isEmpty() && l.charAt(0) == '\uFEFF') l = l.substring(1);
                if (l.isEmpty() || l.startsWith("#")) continue;
                return l.equals("format linear");
            }
        } catch (IOException ignored) { }
        return false;
    }

    private static double d(String s) { return Double.parseDouble(s); }

    private static Pos pos(String[] t, int i) { return new Pos(d(t[i]), d(t[i + 1]), d(t[i + 2]), (float) d(t[i + 3])); }

    private static CourseModel.Trigger box(String[] t, int i) {
        return new CourseModel.Trigger(d(t[i]), d(t[i + 1]), d(t[i + 2]), d(t[i + 3]), d(t[i + 4]), d(t[i + 5]));
    }

    private static String rest(String[] t, int from) { return String.join(" ", java.util.Arrays.copyOfRange(t, from, t.length)); }

    public static CourseModel load(long seed, File dir) {
        CourseModel m = new CourseModel(seed);
        CourseModel.Linear lin = new CourseModel.Linear();
        m.linear = lin;
        String blocksFile = null, title = null;
        Pos start = null;
        Map<Integer, String> sections = new HashMap<>(), zones = new HashMap<>();
        Map<Integer, CourseModel.Trigger> boxes = new HashMap<>();
        Map<Integer, Pos> respawns = new HashMap<>();
        List<String> problems = new ArrayList<>();
        int ln = 0;
        try {
            for (String raw : Files.readAllLines(new File(dir, "course.txt").toPath(), StandardCharsets.UTF_8)) {
                ln++;
                String l = raw.strip();
                if (!l.isEmpty() && l.charAt(0) == '\uFEFF') l = l.substring(1);
                if (l.isEmpty() || l.startsWith("#")) continue;
                String[] t = l.split("\\s+");
                try {
                    switch (t[0]) {
                        case "format" -> { }
                        case "map" -> m.mapId = t[1].toLowerCase(Locale.ROOT);
                        case "title" -> title = rest(t, 1);
                        case "credit" -> m.credit = rest(t, 1);
                        case "blocks" -> blocksFile = t[1];
                        case "fall" -> lin.fallY = d(t[1]);
                        case "start" -> start = pos(t, 1);
                        case "section" -> sections.put(Integer.parseInt(t[1]), rest(t, 2));
                        case "zone" -> zones.put(Integer.parseInt(t[1]), rest(t, 2));
                        case "cp" -> {
                            int n = Integer.parseInt(t[1]);
                            boxes.put(n, box(t, 3));          // cp N box x0 y0 z0 x1 y1 z1 respawn x y z yaw
                            respawns.put(n, pos(t, 10));
                        }
                        case "finish" -> lin.finish = box(t, 2);
                        case "overview" -> {
                            int n = t.length;
                            m.overviews.add(new CourseModel.Overview(String.join(" ", java.util.Arrays.copyOfRange(t, 1, n - 4)),
                                    new Pos(d(t[n - 4]), d(t[n - 3]), d(t[n - 2]), 0f), (float) d(t[n - 1])));
                        }
                        case "sign" -> {
                            String text = l.replaceFirst("^sign\\s+\\S+\\s+\\S+\\s+\\S+\\s?", "");
                            String[] lines = text.split("\\|", -1);
                            List<String> ls = new ArrayList<>();
                            for (int i = 0; i < 4; i++) ls.add(i < lines.length ? lines[i] : "");
                            lin.signs.add(new CourseModel.SignText(Integer.parseInt(t[1]), Integer.parseInt(t[2]), Integer.parseInt(t[3]), ls));
                        }
                        default -> problems.add("line " + ln + ": unknown command '" + t[0] + "'");
                    }
                } catch (RuntimeException e) {
                    problems.add("line " + ln + ": " + e);
                }
            }
        } catch (IOException e) {
            problems.add("could not read course.txt: " + e);
        }
        if (blocksFile == null || start == null || sections.isEmpty() || lin.finish == null) {
            problems.add("course.txt is incomplete (blocks/start/section/finish)");
            m.notes.put("import problems", String.join("; ", problems));
            return m;      // no obstacles -> CourseWorld falls back to the stub course
        }
        try {
            Object[] pc = ImportedCourse.readMcmi(new File(dir, blocksFile));
            String[] pal = (String[]) pc[0];
            short[] xyz = (short[]) pc[1];
            int[] pi = (int[]) pc[2];
            for (int k = 0; k < pi.length; k++) {
                String s = pal[pi[k]];
                if (s != null) m.blocks.set(xyz[3 * k], xyz[3 * k + 1], xyz[3 * k + 2], s);
            }
        } catch (IOException | RuntimeException e) {
            problems.add("blocks: " + e);
            m.notes.put("import problems", String.join("; ", problems));
            return m;
        }

        int n = 0;
        while (sections.containsKey(n + 1)) n++;
        for (int k = 1; k <= n; k++) lin.sections.add(sections.get(k));
        for (int k = 1; k < n; k++) {
            if (!boxes.containsKey(k)) { problems.add("checkpoint " + k + " missing"); continue; }
            lin.checkpoints.add(new CourseModel.Checkpoint(k, boxes.get(k), respawns.get(k)));
        }
        m.spawns.add(start);
        int killY = (int) Math.floor(lin.fallY);
        int per = lin.perStage;
        for (int k = 1; k <= n; k++) {
            int stage = (k - 1) / per + 1, idx = (k - 1) % per + 1;
            Pos entry = k == 1 ? start : respawns.get(k - 1);
            CourseModel.Trigger tr = k < n ? boxes.get(k) : lin.finish;
            if (entry == null || tr == null) continue;
            m.obstacles.add(new CourseModel.Obstacle(CourseModel.mainId(stage, idx), sections.get(k), CourseModel.Kind.MAIN, stage, idx, null,
                    entry, tr.blockBox(), killY, null, null));
            if (idx == 1) m.hubs.add(new CourseModel.Hub(stage, entry, entry, zones.getOrDefault(stage, "Stage " + stage)));
        }
        if (m.overviews.isEmpty()) m.overviews.add(new CourseModel.Overview("Overview", new Pos(0.5, 150, 150, 0f), 90f));
        m.recomputeBounds();
        m.notes.put("format", "linear (1:1 import, command blocks replaced by plugin code)");
        m.notes.put("title", title == null ? m.mapId : title);
        m.notes.put("imported", "all " + n + " sections");
        if (!problems.isEmpty()) m.notes.put("import problems", String.join("; ", problems));
        return m;
    }
}
