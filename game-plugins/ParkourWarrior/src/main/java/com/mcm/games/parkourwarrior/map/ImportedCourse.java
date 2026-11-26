package com.mcm.games.parkourwarrior.map;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Course sections cut from community parkour maps (runtime data, never in the jar or the repo). Read from the
 * plugin's data folder {@code imported/}. Each section replaces ONE obstacle slot of the built-in course (M1-1 ...
 * B5-3, E-HARD-2): its blocks (obstacle + the map's own scenery around it) are pasted into the slot, turned to the
 * slot's direction, with its start on the previous checkpoint and a completion pad at its end; the course chain then
 * continues from that pad. Slots without a section keep the built-in obstacle. No folder / no course.txt = the
 * built-in course, unchanged.
 *
 * <p>Files: {@code course.txt} (below), the block pieces it names ({@code *.mcmi.gz}, MCMI v1 written by
 * {@code _candidates/tools/mca2tg.py}: piece-local x,y,z + palette index) and {@code routes.txt} (written by the
 * offline tool {@code tools/ImportRoute}: the simulated foothold route of every section in section-local coordinates;
 * it gives the kill height and kill column at runtime and the validator's route).
 *
 * <pre>
 * credit KEY text...                 credit shown to players for sections using KEY ("Section from ... by ...")
 * section SLOT PIECEFILE IDEA name...
 *   base X Y Z                       (first) the piece's world origin: the following coordinates are world coordinates
 *   origin X Y Z                     piece-local block the player starts on (it becomes the previous pad's centre)
 *   end X Y Z                        piece-local centre block of the 3x3 completion pad
 *   turn Q                           quarter turns clockwise so that the section runs towards +z (forward)
 *   crop X0 Y0 Z0 X1 Y1 Z1           only blocks of the piece inside this piece-local box (repeatable; none = all)
 *   drop X0 Y0 Z0 X1 Y1 Z1           leave out blocks in this piece-local box (repeatable)
 *   via X Y Z / avoid X0 Y0 Z0 X1 Y1 Z1   route-tool hints (piece-local)
 *   set X Y Z STATE                  edit on top of the source (air removes); every edit is listed in the survey
 *   killdepth N                      kill height = lowest foothold - N (default 6)
 *   tag EASY|MEDIUM|HARD             difficulty sign (bonus paths)
 *   credit KEY
 * end
 * builtin SLOT KEY THEME LEVEL       a moving-mechanic obstacle from {@link Mechanics} (conveyor, bars, ledges, doors,
 *                                    barrels, press, beams, checker) in this slot, themed mesa|garden|jungle|ice, level 1..3
 * classic SLOT FROMSLOT              the built-in (classic course) obstacle of FROMSLOT in this slot, e.g. classic B1-3 B5-3
 * zone N NAME...                     hub / zone name of stage N (default: the classic zone names)
 * mapcredit TEXT...                  credit line on the start island and in /pw info
 * endname EASY|MEDIUM|HARD TEXT...   the ending's name on its fork gate
 * map ID                             the map id (mcc6, mcc10, ...)
 * </pre>
 */
final class ImportedCourse {

    static final class Section {
        String slot, file, idea, name, tag = "", credit;
        int[] origin, end;
        int turn, killDepth = 6;
        int[] base = {0, 0, 0};
        /** Edits on top of the source (set X Y Z state, source coordinates; state air removes). */
        final List<Object[]> sets = new ArrayList<>();
        final List<int[]> crop = new ArrayList<>(), drop = new ArrayList<>(), via = new ArrayList<>(), avoid = new ArrayList<>();
        /** routes.txt: nodes in section-local coordinates (type char, cells). */
        final List<Object[]> route = new ArrayList<>();
        /** Section-local blocks (x, y, z -> state), loaded on demand. */
        Map<Long, String> blocks;

        /** Piece-local -> section-local (start block at 0,0,0, running towards +z). */
        int[] toLocal(int x, int y, int z) {
            int dx = x - origin[0], dz = z - origin[2];
            return new int[]{Bs.rx(dx, dz, turn), y - origin[1], Bs.rz(dx, dz, turn)};
        }

        int[] endLocal() { return toLocal(end[0], end[1], end[2]); }

        boolean keep(int x, int y, int z) {
            for (int[] d : drop) if (in(d, x, y, z)) return false;
            if (crop.isEmpty()) return true;
            for (int[] c : crop) if (in(c, x, y, z)) return true;
            return false;
        }
    }

    static boolean in(int[] b, int x, int y, int z) {
        return x >= Math.min(b[0], b[3]) && x <= Math.max(b[0], b[3]) && y >= Math.min(b[1], b[4]) && y <= Math.max(b[1], b[4])
                && z >= Math.min(b[2], b[5]) && z <= Math.max(b[2], b[5]);
    }

    final File dir;
    final Map<String, String> credits = new LinkedHashMap<>();
    final Map<String, Section> sections = new LinkedHashMap<>();
    final List<String> problems = new ArrayList<>();
    /** builtin lines: slot -> {key, theme, level}. */
    final Map<String, String[]> builtins = new LinkedHashMap<>();
    /** classic lines: slot -> classic slot. */
    final Map<String, String> classics = new LinkedHashMap<>();
    final Map<Integer, String> zones = new HashMap<>();
    String mapCredit = "", mapId = "imported";
    /** endname EASY|MEDIUM|HARD text: the ending's name on its fork gate. */
    final Map<String, String> endNames = new HashMap<>();
    private final Map<String, Object[]> pieceCache = new HashMap<>();

    private ImportedCourse(File dir) { this.dir = dir; }

    /** Load {@code dir/course.txt} (+ routes.txt); null when there is none. Problems are collected, not thrown. */
    static ImportedCourse load(File dir) {
        if (dir == null) return null;
        File f = new File(dir, "course.txt");
        if (!f.isFile()) return null;
        ImportedCourse ic = new ImportedCourse(dir);
        ic.mapId = dir.getName().toLowerCase(Locale.ROOT);
        try {
            ic.parse(Files.readAllLines(f.toPath(), StandardCharsets.UTF_8));
            File r = new File(dir, "routes.txt");
            if (r.isFile()) ic.parseRoutes(Files.readAllLines(r.toPath(), StandardCharsets.UTF_8));
        } catch (IOException | RuntimeException e) {
            ic.problems.add("could not read " + f + ": " + e);
        }
        return ic;
    }

    private static int i(String s) { return Integer.parseInt(s.trim()); }

    /** Coordinates written in the source world: minus the piece origin given by {@code base}. */
    private static int[] rel(Section s, int[] c) {
        for (int k = 0; k < c.length; k++) c[k] -= s.base[k % 3];
        return c;
    }

    private static int[] box(String[] t) { return new int[]{i(t[1]), i(t[2]), i(t[3]), i(t[4]), i(t[5]), i(t[6])}; }

    private static String rest(String[] t, int from) {
        StringBuilder sb = new StringBuilder();
        for (int k = from; k < t.length; k++) { if (k > from) sb.append(' '); sb.append(t[k]); }
        return sb.toString();
    }

    private void parse(List<String> lines) {
        Section cur = null;
        int ln = 0;
        for (String raw : lines) {
            ln++;
            String l = raw.strip();
            if (!l.isEmpty() && l.charAt(0) == '﻿') l = l.substring(1);
            if (l.isEmpty() || l.startsWith("#")) continue;
            String[] t = l.split("\\s+");
            try {
                if (cur != null) {
                    switch (t[0]) {
                        case "base" -> cur.base = new int[]{i(t[1]), i(t[2]), i(t[3])};
                        case "origin" -> cur.origin = rel(cur, new int[]{i(t[1]), i(t[2]), i(t[3])});
                        case "end" -> {
                            if (t.length == 1) {
                                if (cur.origin == null || cur.end == null) problems.add("section " + cur.slot + " needs origin and end");
                                else sections.put(cur.slot, cur);
                                cur = null;
                            } else cur.end = rel(cur, new int[]{i(t[1]), i(t[2]), i(t[3])});
                        }
                        case "turn" -> cur.turn = i(t[1]) & 3;
                        case "crop" -> cur.crop.add(rel(cur, box(t)));
                        case "drop" -> cur.drop.add(rel(cur, box(t)));
                        case "via" -> cur.via.add(rel(cur, new int[]{i(t[1]), i(t[2]), i(t[3])}));
                        case "avoid" -> cur.avoid.add(rel(cur, box(t)));
                        case "killdepth" -> cur.killDepth = i(t[1]);
                        case "set" -> cur.sets.add(new Object[]{rel(cur, new int[]{i(t[1]), i(t[2]), i(t[3])}), t[4]});
                        case "tag" -> cur.tag = t[1].toUpperCase(Locale.ROOT);
                        case "credit" -> cur.credit = t[1];
                        default -> problems.add("line " + ln + ": '" + t[0] + "' inside section " + cur.slot);
                    }
                    continue;
                }
                switch (t[0]) {
                    case "credit" -> credits.put(t[1], rest(t, 2));
                    case "builtin" -> builtins.put(t[1].toUpperCase(Locale.ROOT), new String[]{t[2], t.length > 3 ? t[3] : "mesa", t.length > 4 ? t[4] : "1"});
                    case "classic" -> classics.put(t[1].toUpperCase(Locale.ROOT), t[2].toUpperCase(Locale.ROOT));
                    case "zone" -> zones.put(i(t[1]), rest(t, 2));
                    case "mapcredit" -> mapCredit = rest(t, 1);
                    case "endname" -> endNames.put(t[1].toUpperCase(Locale.ROOT), rest(t, 2));
                    case "map" -> mapId = t[1].toLowerCase(Locale.ROOT);
                    case "section" -> {
                        cur = new Section();
                        cur.slot = t[1].toUpperCase(Locale.ROOT);
                        cur.file = t[2];
                        cur.idea = t[3];
                        cur.name = rest(t, 4);
                    }
                    default -> problems.add("line " + ln + ": unknown command '" + t[0] + "'");
                }
            } catch (RuntimeException e) {
                problems.add("line " + ln + ": " + e);
            }
        }
        if (cur != null) problems.add("section " + cur.slot + " has no 'end'");
    }

    private void parseRoutes(List<String> lines) {
        for (String raw : lines) {
            String l = raw.strip();
            if (l.isEmpty() || l.startsWith("#")) continue;
            String[] t = l.split("\\s+");
            Section s = t.length > 2 ? sections.get(t[1]) : null;
            if (s == null || !t[0].equals("node")) continue;
            List<int[]> cells = new ArrayList<>();
            for (int k = 3; k < t.length; k++) {
                String[] c = t[k].split(",");
                cells.add(new int[]{i(c[0]), i(c[1]), i(c[2])});
            }
            s.route.add(new Object[]{t[2].charAt(0), cells});
        }
    }

    Section section(String slot) { return sections.get(slot); }

    String credit(Section s) {
        String c = s.credit == null ? null : credits.get(s.credit);
        return c == null ? "" : c;
    }

    /**
     * Build the section into its course slot, or return null (problem recorded) when it cannot be used, so the
     * caller falls back to the built-in obstacle. The start pad (3x3 + headroom) of the previous obstacle is kept;
     * behind the start (local z < -1) the section only fills air so it never cuts into the previous obstacle.
     */
    Ob make(W w, Ob.Slot slot) {
        Section s = sections.get(slot.id());
        if (s == null) return null;
        if (s.route.size() < 3) { problems.add(slot.id() + ": no route in routes.txt (run tools/ImportRoute); built-in obstacle kept"); return null; }
        Map<Long, String> bl;
        try { bl = blocks(s); } catch (IOException | RuntimeException e) { problems.add(slot.id() + ": " + e); return null; }
        Ob o = new Ob(w, slot, s.name, s.idea);
        o.imported = true;
        o.replica = true;
        o.credit = credit(s);
        o.killDepth = s.killDepth;
        if (slot.kind() == com.mcm.games.parkourwarrior.CourseModel.Kind.BONUS) o.difficultyTag = s.tag;
        int[] e = s.endLocal();
        for (Map.Entry<Long, String> b : bl.entrySet()) {
            long k = b.getKey();
            int x = W.kx(k), y = W.ky(k), z = W.kz(k);
            if (Math.abs(x) <= 1 && Math.abs(z) <= 1 && y >= 0 && y <= 3) continue;
            if (z < -1) continue;    // behind the start = the hub deck / previous obstacle
            o.set(x, y, z, b.getValue());
        }
        // headroom above the start pad
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = 1; y <= 3; y++)
            if (!o.air(x, y, z) && !Bs.passable(o.get(x, y, z))) o.set(x, y, z, null);
        // the start node is the routed one (the previous pad + the source's own start platform)
        Ob.Node st = o.route.get(0);
        @SuppressWarnings("unchecked") List<int[]> c0 = (List<int[]>) s.route.get(0)[1];
        java.util.Set<Long> have = new java.util.HashSet<>();
        for (int[] c : st.cells) have.add(W.key(c[0], c[1], c[2]));
        for (int[] c : c0) {
            if (c[2] < -1) continue;
            int[] wc = {o.wx(c[0], c[2]), o.wy(c[1]), o.wz(c[0], c[2])};
            if (have.add(W.key(wc[0], wc[1], wc[2]))) st.cells.add(wc);
        }
        for (int n = 1; n < s.route.size() - 1; n++) {
            Object[] nd = s.route.get(n);
            char t = (char) nd[0];
            @SuppressWarnings("unchecked") List<int[]> cells = (List<int[]>) nd[1];
            o.cells(t == 'L' ? Ob.T.LADDER : t == 'I' ? Ob.T.ICE : Ob.T.AREA, cells.toArray(new int[0][]));
        }
        o.end(e[0], e[1], e[2]);
        Ob.Node pn = o.route.get(o.route.size() - 1);
        @SuppressWarnings("unchecked") List<int[]> cl = (List<int[]>) s.route.get(s.route.size() - 1)[1];
        java.util.Set<Long> got = new java.util.HashSet<>();
        for (int[] c : pn.cells) got.add(W.key(c[0], c[1], c[2]));
        for (int[] c : cl) {
            int[] wc = {o.wx(c[0], c[2]), o.wy(c[1]), o.wz(c[0], c[2])};
            if (got.add(W.key(wc[0], wc[1], wc[2]))) pn.cells.add(wc);
        }
        return o;
    }

    /** Section-local blocks of a section (cached per section). */
    Map<Long, String> blocks(Section s) throws IOException {
        if (s.blocks != null) return s.blocks;
        Object[] pc = pieceCache.get(s.file);
        if (pc == null) { pc = readMcmi(new File(dir, s.file)); pieceCache.put(s.file, pc); }
        String[] pal = (String[]) pc[0];
        short[] xyz = (short[]) pc[1];
        int[] pi = (int[]) pc[2];
        Map<Long, String> out = new HashMap<>();
        String[] turned = new String[pal.length];
        for (int k = 0; k < pal.length; k++) turned[k] = pal[k] == null ? null : Bs.rot(pal[k], s.turn);
        for (int k = 0; k < pi.length; k++) {
            int x = xyz[3 * k], y = xyz[3 * k + 1], z = xyz[3 * k + 2];
            String st = turned[pi[k]];
            if (st == null || !s.keep(x, y, z)) continue;
            int[] l = s.toLocal(x, y, z);
            out.put(W.key(l[0], l[1], l[2]), st);
        }
        for (Object[] st : s.sets) {
            int[] p = (int[]) st[0], l = s.toLocal(p[0], p[1], p[2]);
            String v = (String) st[1];
            if (Bs.isAir(v)) out.remove(W.key(l[0], l[1], l[2]));
            else out.put(W.key(l[0], l[1], l[2]), Bs.rot(Bs.full(v), s.turn));
        }
        s.blocks = out;
        return out;
    }

    /** MCMI v1 -> {palette (air = null), xyz shorts, palette indices}. */
    static Object[] readMcmi(File f) throws IOException {
        try (InputStream in = new GZIPInputStream(new BufferedInputStream(new FileInputStream(f), 1 << 16), 1 << 16);
             DataInputStream d = new DataInputStream(new BufferedInputStream(in, 1 << 16))) {
            byte[] magic = new byte[4];
            d.readFully(magic);
            if (!new String(magic, StandardCharsets.US_ASCII).equals("MCMI")) throw new IOException("not an MCMI file: " + f);
            int ver = d.readInt();
            if (ver != 1) throw new IOException("MCMI version " + ver);
            int np = d.readInt();
            if (np < 0 || np > 1_000_000) throw new IOException("bad palette size");
            String[] pal = new String[np];
            for (int k = 0; k < np; k++) {
                byte[] b = new byte[d.readUnsignedShort()];
                d.readFully(b);
                String s = new String(b, StandardCharsets.UTF_8);
                pal[k] = Bs.isAir(s) ? null : Bs.full(s);
            }
            int n = d.readInt();
            if (n < 0 || n > 20_000_000) throw new IOException("bad block count");
            short[] xyz = new short[3 * n];
            int[] p = new int[n];
            for (int k = 0; k < n; k++) {
                xyz[3 * k] = d.readShort(); xyz[3 * k + 1] = d.readShort(); xyz[3 * k + 2] = d.readShort();
                int v = d.readInt();
                p[k] = v < 0 || v >= np ? 0 : v;
            }
            if (np == 0) throw new IOException("empty palette");
            return new Object[]{pal, xyz, p};
        }
    }
}
