package com.mcm.lobby;

import java.util.*;

/**
 * Offline hub validator (not part of the plugin). Generates the hub exactly like the server and walks every
 * registered staircase / stair aisle bottom -> top:
 * <ul>
 *   <li>each cell needs a standable surface within one block of the previous one and 2 blocks of headroom;</li>
 *   <li>the step-up from one cell to the next may be at most half a block (vanilla step height 0.6), i.e. a full
 *       block rise has to go via a stair or slab;</li>
 *   <li>an ascending run must never dip (a stair sunk into the floor) and must start on the lower floor and end
 *       flush on the upper floor.</li>
 * </ul>
 * It also audits connection states (fences, panes, bars, walls against vanilla rules), floating carpets and
 * bottom-half stairs that are not part of any registered walk. Exit code 1 when anything fails.
 * Usage: java -cp out com.mcm.lobby.HubValidate
 */
public final class HubValidate {
    static final double EPS = 1e-6;

    public static void main(String[] args) {
        Vox v = new Vox();
        HubGenerator g = new HubGenerator(v, 20261123L);
        g.generate();
        List<W> walks = new ArrayList<>();
        try {
            int[] fc = (int[]) HubGenerator.class.getDeclaredField("fixCounts").get(g);
            System.out.println("post-pass: fences " + fc[BlockFix.FENCE] + ", panes/bars " + fc[BlockFix.PANE] + ", walls " + fc[BlockFix.WALL]
                    + ", gates " + fc[BlockFix.GATE] + ", floating carpets removed " + fc[BlockFix.CARPET]);
            for (Object o : (List<?>) HubGenerator.class.getDeclaredField("walks").get(g)) {
                Class<?> c = o.getClass();
                walks.add(new W((String) c.getMethod("name").invoke(o), (int[]) c.getMethod("xs").invoke(o), (int[]) c.getMethod("zs").invoke(o),
                        (int) c.getMethod("startY").invoke(o), (int) c.getMethod("endY").invoke(o)));
            }
        } catch (ReflectiveOperationException e) {
            System.out.println("legacy generator (no staircase registry / post-pass): walking the known staircase routes");
            walks = legacyWalks();
        }
        List<String> fails = new ArrayList<>();
        Set<Long> walkCells = new HashSet<>();
        Map<String, int[]> groups = new LinkedHashMap<>(); // group name -> {walks, ok}
        for (W w : walks) {
            List<String> f = walk(v, w, walkCells);
            String grp = w.name().replaceAll(" \\[.*", "");
            int[] c = groups.computeIfAbsent(grp, k -> new int[2]);
            c[0]++;
            if (f.isEmpty()) c[1]++;
            fails.addAll(f);
        }
        System.out.println("staircases walked: " + walks.size() + " columns");
        for (var e : groups.entrySet()) System.out.println("  " + e.getKey() + ": " + e.getValue()[1] + "/" + e.getValue()[0] + " ok");
        // paths / jetties: every registered route walked end to end in both directions
        List<String> pathFails = new ArrayList<>();
        try {
            List<?> pws = (List<?>) HubGenerator.class.getDeclaredField("pathWalks").get(g);
            // the stair/slab steps laid along path slopes are part of the paths, not loose stairs
            for (Object o : (List<?>) HubGenerator.class.getDeclaredField("pathSteps").get(g)) { int[] s = (int[]) o; walkCells.add(key(s[0], s[1], s[2])); }
            int okCount = 0, jumps = 0;
            for (Object o : pws) {
                Class<?> c = o.getClass();
                PW pw = new PW((String) c.getMethod("name").invoke(o), (int[]) c.getMethod("xs").invoke(o),
                        (int[]) c.getMethod("zs").invoke(o), (int[]) c.getMethod("ys").invoke(o));
                int[] jc = new int[1];
                List<String> f = new ArrayList<>(walkPath(v, pw, false, walkCells, jc));
                f.addAll(walkPath(v, pw, true, walkCells, jc));
                jumps += jc[0];
                if (f.isEmpty()) okCount++;
                System.out.println("  " + pw.name() + ": " + pw.xs().length + " cells, " + (f.isEmpty() ? "ok" : f.size() + " faults") + (jc[0] > 0 ? ", " + jc[0] + " one-block jumps" : ""));
                pathFails.addAll(f);
            }
            System.out.println("paths walked: " + okCount + "/" + pws.size() + " ok (" + jumps + " one-block jumps in total, allowed)");
        } catch (ReflectiveOperationException e) {
            System.out.println("generator has no path registry: paths not walked");
        }
        System.out.println("path failures: " + pathFails.size());
        pathFails.forEach(f -> System.out.println("  FAIL " + f));
        fails.addAll(pathFails);
        // connection audit (independent re-check of the final grid)
        List<String> conn = new ArrayList<>();
        List<String> carpets = new ArrayList<>();
        Map<String, Integer> loose = new TreeMap<>();
        for (int[] s : v.sectionCoords()) {
            short[] sec = v.section(s[0], s[1], s[2]);
            for (int i = 0; i < 4096; i++) {
                if (sec[i] == 0) continue;
                int x = (s[0] << 4) + (i & 15), y = (s[1] << 4) + (i >> 8), z = (s[2] << 4) + ((i >> 4) & 15);
                String st = v.get(x, y, z), id = Shapes.base(st);
                if (Shapes.isFence(id) || Shapes.isPane(id) || Shapes.isWall(id)) {
                    for (int d = 0; d < 4; d++) {
                        String n = v.get(x + Shapes.DXZ[d][0], y, z + Shapes.DXZ[d][1]);
                        String want, have = Shapes.prop(st, Shapes.DIRS[d], null);
                        if (Shapes.isFence(id)) want = String.valueOf(BlockFix.fenceConnects(id, n, d));
                        else if (Shapes.isPane(id)) want = String.valueOf(BlockFix.paneConnects(n, d));
                        else want = BlockFix.wallConnects(n, d) ? (BlockFix.coversSide(v.get(x, y + 1, z), d) ? "tall" : "low") : "none";
                        if (!want.equals(have)) conn.add(x + "," + y + "," + z + " " + st + " " + Shapes.DIRS[d] + " should be " + want + " (neighbour " + n + ")");
                    }
                } else if (Shapes.isCarpet(id) && !Shapes.fullTop(v.get(x, y - 1, z))) {
                    carpets.add(x + "," + y + "," + z + " " + id + " on " + v.get(x, y - 1, z));
                } else if (Shapes.isStairs(id) && "bottom".equals(Shapes.prop(st, "half", "bottom"))
                        && !walkCells.contains(key(x, y, z)) && Shapes.passable(v.get(x, y + 1, z))) {
                    String k = id + " near " + (Math.floorDiv(x, 16) * 16) + "," + y + "," + (Math.floorDiv(z, 16) * 16);
                    loose.merge(k, 1, Integer::sum);
                }
            }
        }
        System.out.println("connection mismatches: " + conn.size());
        conn.stream().limit(20).forEach(c -> System.out.println("  " + c));
        System.out.println("floating carpets: " + carpets.size());
        carpets.stream().limit(20).forEach(c -> System.out.println("  " + c));
        System.out.println("walkable bottom stairs outside registered staircases: " + loose.values().stream().mapToInt(Integer::intValue).sum());
        loose.forEach((k, n) -> System.out.println("  " + n + "x " + k));
        System.out.println("staircase failures: " + fails.size());
        fails.forEach(f -> System.out.println("  FAIL " + f));
        boolean ok = fails.isEmpty() && conn.isEmpty() && carpets.isEmpty();
        System.out.println(ok ? "VALIDATION PASSED" : "VALIDATION FAILED");
        System.exit(ok ? 0 : 1);
    }

    record W(String name, int[] xs, int[] zs, int startY, int endY) {}

    /** The pre-fix staircases (same routes as the registry of the fixed generator), for before/after comparison. */
    static List<W> legacyWalks() {
        List<W> out = new ArrayList<>();
        for (int x = -15; x <= 15; x++) out.add(new W("dome entrance [x=" + x + "]", new int[]{x, x, x, x, x}, new int[]{-2, -3, -4, -5, -6}, 73, 76));
        for (int x = -22; x <= 22; x++) out.add(new W("plaza gate [x=" + x + "]", new int[]{x, x, x, x, x}, new int[]{41, 40, 39, 38, 37}, 70, 73));
        for (int k = 0; k < 10; k++) {
            double a = Math.toRadians(k * 36.0 + 1.0);
            List<int[]> cells = new ArrayList<>();
            for (double t = 26.8; t <= 45.3; t += 0.02) {
                int x = (int) Math.floor(Math.cos(a) * t), z = (int) Math.floor(-62 + Math.sin(a) * t);
                if (Math.hypot(x, z + 62) > 45.3) break;
                if (!cells.isEmpty()) {
                    int[] p = cells.get(cells.size() - 1);
                    if (p[0] == x && p[1] == z) continue;
                    if (p[0] != x && p[1] != z) {
                        double ex = Math.abs(-Math.sin(a) * (x + 0.5) + Math.cos(a) * (p[1] + 0.5 + 62));
                        double ez = Math.abs(-Math.sin(a) * (p[0] + 0.5) + Math.cos(a) * (z + 0.5 + 62));
                        cells.add(ex <= ez ? new int[]{x, p[1]} : new int[]{p[0], z});
                    }
                }
                cells.add(new int[]{x, z});
            }
            int[] xs = cells.stream().mapToInt(c -> c[0]).toArray(), zs = cells.stream().mapToInt(c -> c[1]).toArray();
            out.add(new W("dome aisle " + (k * 36) + "deg", xs, zs, 64, 79));
        }
        return out;
    }

    record PW(String name, int[] xs, int[] zs, int[] ys) {}

    static boolean clear(Vox v, int x, int y0, int y1, int z) {
        for (int y = y0; y <= y1; y++) if (!Shapes.passable(v.get(x, y, z))) return false;
        return true;
    }

    /**
     * Walk a path's centre line (a 0.6 x 1.8 player keeps to the centre). Each move may rise at most 1 block
     * (a plain step-up is 0.6; anything between 0.6 and 1.0 is a jump and needs 3 free blocks above both cells);
     * the floor must be found within 2 blocks up / 3 down; 2 blocks of headroom everywhere. Walking the route
     * reversed turns every drop into a rise, so a drop deeper than 1 block fails too.
     */
    static List<String> walkPath(Vox v, PW w, boolean reverse, Set<Long> cells, int[] jumps) {
        List<String> out = new ArrayList<>();
        int n = w.xs().length;
        String nm = w.name() + (reverse ? " (reversed)" : "");
        int py = 0;
        double exit = Double.NaN;
        for (int k = 0; k < n; k++) {
            int i = reverse ? n - 1 - k : k;
            int x = w.xs()[i], z = w.zs()[i];
            int d = 2;
            String at = "(" + x + "," + z + ")";
            int top, bottom;
            if (k == 0) { top = w.ys()[i] + 2; bottom = w.ys()[i] - 3; }
            else {
                int px = w.xs()[reverse ? i + 1 : i - 1], pz = w.zs()[reverse ? i + 1 : i - 1];
                int dx = x - px, dz = z - pz;
                if (Math.abs(dx) + Math.abs(dz) != 1) { out.add(nm + ": cells " + at + " not adjacent to the previous one"); return out; }
                d = dx > 0 ? 1 : dx < 0 ? 3 : dz > 0 ? 2 : 0;
                top = py + 2; bottom = py - 3;
            }
            int sy = Integer.MIN_VALUE;
            for (int y = top; y >= bottom; y--) {
                if (!Shapes.passable(v.get(x, y, z)) && clear(v, x, y + 1, y + 1, z)) { sy = y; break; }
            }
            if (sy == Integer.MIN_VALUE) {
                if (!reverse) out.add(nm + ": " + at + " no standable floor between y " + bottom + " and " + top + " (water, wall or a drop)");
                return out;
            }
            String st = v.get(x, sy, z);
            double[] h = heights(st, sy, d);
            boolean jump = false;
            if (k > 0) {
                double rise = h[0] - exit;
                if (rise > 1.0 + EPS) out.add(nm + ": " + at + " rises " + rise + " blocks onto " + Shapes.base(st) + " y " + sy + " - more than a 1-block jump");
                else if (rise > 0.6 + EPS) {
                    jump = true;
                    jumps[0]++;
                    if (System.getenv("HUB_SHOW_JUMPS") != null) System.out.println("    jump " + nm + " " + at + " onto " + Shapes.base(st) + " y " + sy + " dir " + d + " rise " + rise + " from y " + py);
                    int ppx = w.xs()[reverse ? i + 1 : i - 1], ppz = w.zs()[reverse ? i + 1 : i - 1];
                    if (!clear(v, ppx, py + 1, py + 3, ppz) || !clear(v, x, sy + 1, sy + 3, z))
                        out.add(nm + ": " + at + " 1-block jump onto y " + sy + " without 3 blocks of headroom");
                }
            }
            if (!jump && !clear(v, x, sy + 1, sy + 2, z)) out.add(nm + ": " + at + " y " + sy + " headroom blocked (" + v.get(x, sy + 2, z) + ")");
            cells.add(key(x, sy, z));
            exit = h[1];
            py = sy;
        }
        return out;
    }

    static long key(int x, int y, int z) { return ((long) x << 40) ^ ((long) (y & 0xFFFF) << 20) ^ (z & 0xFFFFFL); }

    /** Surface heights of a block for a player moving in direction d: {entry, exit}. */
    static double[] heights(String st, int y, int d) {
        String id = Shapes.base(st);
        if (Shapes.isSlab(id)) {
            String t = Shapes.prop(st, "type", "bottom");
            double h = t.equals("bottom") ? y + 0.5 : y + 1;
            return new double[]{h, h};
        }
        if (Shapes.isStairs(id) && "bottom".equals(Shapes.prop(st, "half", "bottom"))) {
            int f = Shapes.dirIndex(Shapes.prop(st, "facing", "north"));
            if (f == d) return new double[]{y + 0.5, y + 1};                 // walking up the stair
            if (f == Shapes.opposite(d)) return new double[]{y + 1, y + 0.5}; // walking down it
            return new double[]{y + 0.5, y + 1};                             // across: either half
        }
        if (Shapes.isFence(id) || Shapes.isWall(id) || Shapes.isFenceGate(id)) return new double[]{y + 1.5, y + 1.5};
        return new double[]{y + 1, y + 1};
    }

    static List<String> walk(Vox v, W w, Set<Long> cells) {
        List<String> out = new ArrayList<>();
        int py = w.startY();
        double exit = Double.NaN;
        boolean ascending = w.endY() > w.startY();
        for (int i = 0; i < w.xs().length; i++) {
            int x = w.xs()[i], z = w.zs()[i];
            int d = 2;
            if (i > 0) {
                int dx = x - w.xs()[i - 1], dz = z - w.zs()[i - 1];
                if (Math.abs(dx) + Math.abs(dz) != 1) { out.add(w.name() + ": cells " + (i - 1) + "->" + i + " not adjacent"); return out; }
                d = dx > 0 ? 1 : dx < 0 ? 3 : dz > 0 ? 2 : 0;
            }
            int sy = Integer.MIN_VALUE;
            for (int y = (i == 0 ? w.startY() : py + 1); y >= (i == 0 ? w.startY() : py - 1); y--) {
                if (!Shapes.passable(v.get(x, y, z))) { sy = y; break; }
            }
            String at = "(" + x + "," + z + ")";
            if (sy == Integer.MIN_VALUE) { out.add(w.name() + ": step " + i + " " + at + " has no floor within 1 block of y " + py); return out; }
            String st = v.get(x, sy, z);
            for (int hy = sy + 1; hy <= sy + 2; hy++)
                if (!Shapes.passable(v.get(x, hy, z))) out.add(w.name() + ": step " + i + " " + at + " y " + sy + " headroom blocked by " + v.get(x, hy, z) + " at y " + hy);
            double[] h = heights(st, sy, d);
            if (i > 0) {
                double rise = h[0] - exit;
                if (rise > 0.5 + 0.1 + EPS) out.add(w.name() + ": step " + i + " " + at + " rises " + rise + " (onto " + Shapes.base(st) + " y " + sy + ") - needs a jump");
                if (ascending && rise < -EPS && i < w.xs().length) out.add(w.name() + ": step " + i + " " + at + " dips " + (-rise) + " (" + Shapes.base(st) + " y " + sy + " sunk below the previous step)");
            }
            if (i == 0 && sy != w.startY()) out.add(w.name() + ": foot " + at + " floor at y " + sy + ", expected " + w.startY());
            if (i == w.xs().length - 1 && sy != w.endY()) out.add(w.name() + ": head " + at + " floor at y " + sy + ", expected " + w.endY());
            cells.add(key(x, sy, z));
            exit = h[1];
            py = sy;
        }
        return out;
    }
}
