package com.mcm.games.findthebutton.map;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Offline reachability check for hiding spots (pure Java, runs at plugin enable and in the offline tool).
 *
 * <p>Model (deliberately conservative, so a pass is trustworthy):
 * <ul>
 *   <li>A standing node is a column (x, z) and a feet height on top of a block (slabs 0.5, carpets 1/16, fences
 *       1.5 ...) with 1.8 blocks of passable cells above the feet.</li>
 *   <li>Moves go to the 4 horizontal neighbours only: step up to +0.6 (stairs/slabs) without a jump, jump up to
 *       +1.25 when both columns are clear up to the landing height + body + 0.2, and drop down any distance when
 *       the target column is clear above the landing up to the start height.</li>
 *   <li>Flood fill starts from every start-pad cell.</li>
 *   <li>A spot is pressable when the button cell is empty, its support is a full sturdy block, and from some
 *       reachable node the straight line from the eye (feet + 1.62, cell centre) to the centre of the button's
 *       hit box is at most {@value #REACH} blocks long (vanilla reach is 4.5) and crosses nothing but air or
 *       invisible light blocks before entering the button cell (plants, torches, panes ... all block it).</li>
 * </ul>
 */
public final class Reach {
    public static final double REACH = 4.0;
    private static final double EYE = 1.62, BODY = 1.8, STEP = 0.6, JUMP = 1.25;

    private final Grid g;
    private final int minX, maxX, minZ, maxZ, minY, maxY;
    /** Reachable nodes: key(x, z) -> list of feet heights. */
    private final Map<Long, List<Double>> reach = new HashMap<>();
    private int nodeCount;

    public Reach(FtbMap map) {
        this.g = map.grid();
        this.minX = FtbMap.MIN - 1; this.maxX = FtbMap.MAX + 1;
        this.minZ = FtbMap.MIN - 1; this.maxZ = FtbMap.MAX + 1;
        this.minY = 0; this.maxY = map.height;
        flood(map);
    }

    public int reachableNodes() { return nodeCount; }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xffffffffL); }

    private boolean passable(int x, int y, int z) { return Shapes.passable(g.get(x, y, z)); }

    /** Cells [from, to) in the column (excluding the support cell) are all passable. */
    private boolean clear(int x, int z, double from, double to, int supportY) {
        int a = (int) Math.floor(from + 1e-6), b = (int) Math.floor(to - 1e-6);
        for (int y = a; y <= b; y++) {
            if (y == supportY) continue;
            if (!passable(x, y, z)) return false;
        }
        return true;
    }

    /** Standable feet heights in a column, with the support cell y. */
    private List<double[]> stands(int x, int z) {
        List<double[]> out = new ArrayList<>();
        for (int y = minY; y <= maxY; y++) {
            double t = Shapes.standTop(g.get(x, y, z));
            if (t < 0) continue;
            double feet = y + t;
            if (feet < 1 - 1e-6 || feet > maxY + 1) continue;
            if (clear(x, z, feet, feet + BODY, y)) out.add(new double[]{feet, y});
        }
        return out;
    }

    private final Map<Long, List<double[]>> standCache = new HashMap<>();

    private List<double[]> standsCached(int x, int z) {
        return standCache.computeIfAbsent(key(x, z), k -> stands(x, z));
    }

    private void flood(FtbMap map) {
        ArrayDeque<double[]> q = new ArrayDeque<>();   // x, z, feet, supportY
        Set<String> seen = new HashSet<>();
        for (int x = map.padX1(); x <= map.padX2(); x++)
            for (int z = map.padZ1(); z <= map.padZ2(); z++)
                for (double[] s : standsCached(x, z))
                    if (Math.abs(s[0] - 1.0) < 0.2 && seen.add(x + "," + z + "," + s[0])) q.add(new double[]{x, z, s[0], s[1]});
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            double[] n = q.poll();
            int x = (int) n[0], z = (int) n[1];
            double fa = n[2];
            int sa = (int) n[3];
            reach.computeIfAbsent(key(x, z), k -> new ArrayList<>()).add(fa);
            nodeCount++;
            for (int[] d : dirs) {
                int nx = x + d[0], nz = z + d[1];
                if (nx < minX || nx > maxX || nz < minZ || nz > maxZ) continue;
                for (double[] s : standsCached(nx, nz)) {
                    double fb = s[0];
                    int sb = (int) s[1];
                    boolean ok;
                    if (fb <= fa + 1e-6) {
                        ok = clear(nx, nz, fb, fa + BODY, sb);                     // walk off / drop
                    } else if (fb <= fa + STEP || (fb <= fa + 1.0 + 1e-6 && stairUp(nx, sb, nz, d))) {
                        ok = clear(x, z, fa, fb + BODY, sa);                       // auto step / walk up a stair
                    } else if (fb <= fa + JUMP) {
                        double apex = fb + BODY + 0.2;                             // head must clear the landing height
                        ok = clear(x, z, fa, apex, sa) && clear(nx, nz, fb, apex, sb);
                    } else ok = false;
                    if (ok && seen.add(nx + "," + nz + "," + fb)) q.add(new double[]{nx, nz, fb, sb});
                }
            }
        }
    }

    /** Bottom-half stair whose high side is in the move direction: walked up in two half steps, no jump. */
    private boolean stairUp(int x, int y, int z, int[] d) {
        String b = g.get(x, y, z);
        if (!States.id(b).endsWith("_stairs") || !"bottom".equals(Shapes.prop(b, "half"))) return false;
        String want = d[0] > 0 ? "east" : d[0] < 0 ? "west" : d[1] > 0 ? "south" : "north";
        return want.equals(Shapes.prop(b, "facing"));
    }

    /** Result of checking one spot. */
    public record Result(Spot spot, boolean ok, String detail) {}

    public Result check(Spot s) {
        String cell = g.get(s.x(), s.y(), s.z());
        if (!States.isAir(cell) && !States.id(cell).equals("light")) return new Result(s, false, "button cell is not empty: " + cell);
        String sup = g.get(s.sx(), s.sy(), s.sz());
        if (!Shapes.fullSolid(sup)) return new Result(s, false, "support is not a full block: " + sup);
        if (s.x() < FtbMap.MIN - 1 || s.x() > FtbMap.MAX + 1 || s.z() < FtbMap.MIN - 1 || s.z() > FtbMap.MAX + 1)
            return new Result(s, false, "outside the map");
        double[] c = s.centre();
        double best = Double.MAX_VALUE;
        String bestAt = null;
        int near = 0;
        for (Map.Entry<Long, List<Double>> e : reach.entrySet()) {
            int x = (int) (e.getKey() >> 32), z = (int) (long) e.getKey().intValue();
            for (double feet : e.getValue()) {
                double ex = x + 0.5, ey = feet + EYE, ez = z + 0.5;
                double d = Math.sqrt((ex - c[0]) * (ex - c[0]) + (ey - c[1]) * (ey - c[1]) + (ez - c[2]) * (ez - c[2]));
                if (d > REACH) continue;
                near++;
                if (lineClear(ex, ey, ez, c, s) && d < best) {
                    best = d;
                    bestAt = String.format("(%d, %.2f, %d)", x, feet, z);
                }
            }
        }
        if (bestAt == null) return new Result(s, false, near == 0 ? "no reachable standing spot within " + REACH
                : near + " reachable standing spots in range but none with a clear line to the button");
        return new Result(s, true, String.format("press from %s, eye distance %.2f", bestAt, best));
    }

    private boolean lineClear(double ex, double ey, double ez, double[] c, Spot s) {
        double dx = c[0] - ex, dy = c[1] - ey, dz = c[2] - ez;
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int steps = (int) Math.ceil(len / 0.02);
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) Math.floor(ex + dx * t), y = (int) Math.floor(ey + dy * t), z = (int) Math.floor(ez + dz * t);
            if (x == s.x() && y == s.y() && z == s.z()) return true;
            String b = g.get(x, y, z);
            if (States.isAir(b) || States.id(b).equals("light")) continue;
            return false;
        }
        return false;
    }

    /** Checks every spot of a map. Also reports start-pad problems and any stray button in the build. */
    public static List<String> validate(FtbMap map, List<Result> results) {
        List<String> problems = new ArrayList<>();
        Reach r = new Reach(map);
        for (int x = map.padX1(); x <= map.padX2(); x++)
            for (int z = map.padZ1(); z <= map.padZ2(); z++)
                if (!r.isReachable(x, z, 1.0)) problems.add("start pad cell (" + x + ", " + z + ") is not standable");
        map.grid().forEach((x, y, z, b) -> {
            if (States.id(b).endsWith("_button")) problems.add("stray button in the build at (" + x + "," + y + "," + z + ")");
        });
        for (Spot s : map.spots()) {
            Result res = r.check(s);
            results.add(res);
            if (!res.ok()) problems.add("spot '" + s.name() + "': " + res.detail());
        }
        problems.addAll(map.grid().validateStairs());
        return problems;
    }

    public boolean isReachable(int x, int z, double feet) {
        List<Double> l = reach.get(key(x, z));
        if (l == null) return false;
        for (double f : l) if (Math.abs(f - feet) < 0.2) return true;
        return false;
    }
}
