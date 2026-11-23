package com.mcm.games.battlebox.arena;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

/**
 * In-memory block grid over a fixed local bounding box (pure Java). Generators write into it; the runtime
 * copies it into the world, the offline tool writes it as an MCMDUMP file.
 */
public final class Grid implements MapSink {
    public final int minX, minY, minZ, maxX, maxY, maxZ;
    private final int sx, sy, sz;
    private final int[] cells;               // palette index + 1, 0 = air
    private final List<String> palette = new ArrayList<>();
    private final Map<String, Integer> index = new HashMap<>();

    public Grid(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = minX; this.minY = minY; this.minZ = minZ;
        this.maxX = maxX; this.maxY = maxY; this.maxZ = maxZ;
        this.sx = maxX - minX + 1; this.sy = maxY - minY + 1; this.sz = maxZ - minZ + 1;
        this.cells = new int[sx * sy * sz];
    }

    public boolean inside(int x, int y, int z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    private int idx(int x, int y, int z) { return ((y - minY) * sz + (z - minZ)) * sx + (x - minX); }

    @Override
    public void set(int x, int y, int z, String b) {
        if (!inside(x, y, z)) return;
        if (States.isAir(b)) { cells[idx(x, y, z)] = 0; return; }
        String f = States.full(b);
        Integer i = index.get(f);
        if (i == null) { palette.add(f); i = palette.size(); index.put(f, i); }
        cells[idx(x, y, z)] = i;
    }

    /** Block at a position ("minecraft:air" outside or empty). */
    public String get(int x, int y, int z) {
        if (!inside(x, y, z)) return "minecraft:air";
        int v = cells[idx(x, y, z)];
        return v == 0 ? "minecraft:air" : palette.get(v - 1);
    }

    public boolean isAir(int x, int y, int z) {
        return !inside(x, y, z) || cells[idx(x, y, z)] == 0;
    }

    public long count() {
        long n = 0;
        for (int c : cells) if (c != 0) n++;
        return n;
    }

    public int distinctIds() {
        Set<String> s = new HashSet<>();
        for (String p : palette) s.add(States.id(p));
        return s.size();
    }

    // ------------------------------------------------------------------ post-pass

    private static final int[][] DIRS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final String[] DIR_NAMES = {"north", "east", "south", "west"};

    private static String strip(String b) {
        int s = b.indexOf('[');
        return s < 0 ? b : b.substring(0, s);
    }

    /** Fence family: wooden fences connect to each other, nether brick fences only to nether brick fences. */
    private static boolean sameFenceFamily(String a, String b) {
        return a.equals("nether_brick_fence") == b.equals("nether_brick_fence");
    }

    /** Does a fence gate let a fence/wall connect from direction index d (the fence's side)? */
    private static boolean gateAligned(String gate, int d) {
        String f = Shapes.prop(gate, "facing");
        boolean gateNS = "north".equals(f) || "south".equals(f);   // gate spans east-west
        boolean connEW = d == 1 || d == 3;
        return gateNS == connEW;
    }

    private static boolean connects(int k, String selfId, String nb, int d) {
        if (States.isAir(nb)) return false;
        String nid = States.id(nb);
        int nk = Shapes.kind(nid);
        switch (k) {
            case 1: // fence
                if (nk == 1) return sameFenceFamily(selfId, nid);
                if (nk == 4) return gateAligned(nb, d);
                return Shapes.fullSolid(nb);
            case 2: // pane / bars
                return nk == 2 || nk == 3 || Shapes.fullSolid(nb);
            case 3: // wall
                if (nk == 3 || nk == 2) return true;
                if (nk == 4) return gateAligned(nb, d);
                return Shapes.fullSolid(nb);
            default:
                return false;
        }
    }

    /**
     * Final pass over the finished grid: recompute every fence, pane, iron bars, wall and fence-gate state from
     * its neighbours (vanilla rules), and drop carpets that are not on a full solid block. Returns the number of
     * carpets removed.
     */
    public int postProcess() {
        List<int[]> todo = new ArrayList<>();
        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++)
                for (int x = minX; x <= maxX; x++) {
                    int v = cells[idx(x, y, z)];
                    if (v == 0) continue;
                    if (Shapes.kind(States.id(palette.get(v - 1))) != 0) todo.add(new int[]{x, y, z});
                }
        String[] out = new String[todo.size()];
        for (int i = 0; i < todo.size(); i++) {
            int[] p = todo.get(i);
            out[i] = connected(p[0], p[1], p[2]);
        }
        for (int i = 0; i < todo.size(); i++) { int[] p = todo.get(i); set(p[0], p[1], p[2], out[i]); }
        int removed = 0;
        for (int y = minY + 1; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++)
                for (int x = minX; x <= maxX; x++) {
                    String id = States.id(get(x, y, z));
                    if (!(id.endsWith("_carpet") || id.equals("moss_carpet"))) continue;
                    if (!Shapes.fullSolid(get(x, y - 1, z))) { set(x, y, z, null); removed++; }
                }
        return removed;
    }

    /** Old name of the post-pass (used by the holding area). */
    public void connect() { postProcess(); }

    private String connected(int x, int y, int z) {
        String b = get(x, y, z);
        String head = strip(b);
        String id = States.id(b);
        int k = Shapes.kind(id);
        if (k == 4) {
            String facing = Shapes.prop(b, "facing");
            if (facing == null) facing = "north";
            String open = Shapes.prop(b, "open");
            boolean ns = facing.equals("north") || facing.equals("south");
            boolean inWall = ns ? (Shapes.kind(States.id(get(x + 1, y, z))) == 3 || Shapes.kind(States.id(get(x - 1, y, z))) == 3)
                    : (Shapes.kind(States.id(get(x, y, z + 1))) == 3 || Shapes.kind(States.id(get(x, y, z - 1))) == 3);
            return head + "[facing=" + facing + ",in_wall=" + inWall + ",open=" + (open == null ? "false" : open) + "]";
        }
        boolean[] c = new boolean[4];
        for (int d = 0; d < 4; d++) c[d] = connects(k, id, get(x + DIRS[d][0], y, z + DIRS[d][1]), d);
        StringBuilder st = new StringBuilder();
        if (k == 3) {
            String above = get(x, y + 1, z);
            String aid = States.id(above);
            boolean aboveFull = Shapes.fullSolid(above);
            String[] side = new String[4];
            for (int d = 0; d < 4; d++) {
                if (!c[d]) { side[d] = "none"; continue; }
                boolean tall = aboveFull;
                if (!tall && Shapes.kind(aid) == 3) tall = connects(3, aid, get(x + DIRS[d][0], y + 1, z + DIRS[d][1]), d);
                side[d] = tall ? "tall" : "low";
            }
            boolean straightNS = c[0] && c[2] && !c[1] && !c[3] && side[0].equals(side[2]);
            boolean straightEW = c[1] && c[3] && !c[0] && !c[2] && side[1].equals(side[3]);
            boolean postAbove = aid.contains("torch") || aid.contains("lantern") || aid.contains("sign") || aid.contains("banner")
                    || aid.contains("candle") || aid.contains("end_rod") || aid.contains("lightning_rod") || aid.contains("pressure_plate")
                    || aid.contains("potted") || aid.equals("flower_pot")
                    || (Shapes.kind(aid) == 3 && "true".equals(Shapes.prop(connected(x, y + 1, z), "up")));
            boolean up = !(straightNS || straightEW) || postAbove;
            for (int d = 0; d < 4; d++) { if (d > 0) st.append(','); st.append(DIR_NAMES[d]).append('=').append(side[d]); }
            st.append(",up=").append(up);
        } else {
            for (int d = 0; d < 4; d++) { if (d > 0) st.append(','); st.append(DIR_NAMES[d]).append('=').append(c[d]); }
        }
        return head + "[" + st + "]";
    }

    // ------------------------------------------------------------------ staircases

    /** One registered staircase lane: first step at (x, floorY + 1, z), ascending along (dx, dz) for n steps. */
    public static final class Lane {
        public final int x, z, floorY, dx, dz, n;
        public final String name;
        public Lane(String name, int x, int z, int floorY, int dx, int dz, int n) {
            this.name = name; this.x = x; this.z = z; this.floorY = floorY; this.dx = dx; this.dz = dz; this.n = n;
        }
    }

    private final List<Lane> lanes = new ArrayList<>();

    public void addLane(Lane l) { lanes.add(l); }

    public List<Lane> lanes() { return lanes; }

    private boolean free(int x, int y, int z) { return Shapes.passable(get(x, y, z)); }

    /**
     * Walk every registered staircase from bottom to top. Each step must be a bottom-half stair facing up the
     * flight (rise at most 1, no jump), the first step must sit directly on the floor, every step and the landing
     * need 2 blocks of headroom, and the flight must end on a landing flush with the last step.
     * Returns the problems found (empty = all walkable).
     */
    public List<String> validateStairs() {
        List<String> errs = new ArrayList<>();
        for (Lane l : lanes) {
            String dir = l.dz < 0 ? "north" : l.dz > 0 ? "south" : l.dx > 0 ? "east" : "west";
            String where = l.name + " @(" + l.x + "," + (l.floorY + 1) + "," + l.z + ")" + dir;
            int ax = l.x - l.dx, az = l.z - l.dz;
            double approach = Shapes.standTop(get(ax, l.floorY, az));
            if (approach < 0.99 || approach > 1.01) errs.add(where + ": no floor to step from at (" + ax + "," + l.floorY + "," + az + ") = " + get(ax, l.floorY, az));
            if (!free(ax, l.floorY + 1, az) || !free(ax, l.floorY + 2, az)) errs.add(where + ": approach blocked at (" + ax + "," + (l.floorY + 1) + "," + az + ")");
            if (Shapes.standTop(get(l.x, l.floorY, l.z)) < 0.99) errs.add(where + ": first step not standing on the floor");
            double feet = l.floorY + 1;
            for (int i = 1; i <= l.n; i++) {
                int cx = l.x + l.dx * (i - 1), cz = l.z + l.dz * (i - 1), cy = l.floorY + i;
                String b = get(cx, cy, cz);
                String bid = States.id(b);
                double top;
                if (bid.endsWith("_stairs")) {
                    if (!"bottom".equals(Shapes.prop(b, "half")) || !dir.equals(Shapes.prop(b, "facing")))
                        errs.add(where + ": step " + i + " is a stair in the wrong orientation: " + b);
                    top = cy + 1;
                } else if (bid.endsWith("_slab") && "bottom".equals(Shapes.prop(b, "type"))) {
                    top = cy + 0.5;
                } else {
                    errs.add(where + ": step " + i + " missing at (" + cx + "," + cy + "," + cz + ") = " + b);
                    top = cy + 1;
                }
                if (top - feet > 1.0001) errs.add(where + ": step " + i + " rises " + (top - feet) + " (needs a jump)");
                if (!free(cx, cy + 1, cz) || !free(cx, cy + 2, cz)) errs.add(where + ": no headroom above step " + i
                        + " (" + get(cx, cy + 1, cz) + " / " + get(cx, cy + 2, cz) + ")");
                feet = top;
            }
            int lx = l.x + l.dx * l.n, lz = l.z + l.dz * l.n, ly = l.floorY + l.n;
            double land = Shapes.standTop(get(lx, ly, lz));
            if (land < 0.99 || land > 1.01) errs.add(where + ": no landing flush with the last step at (" + lx + "," + ly + "," + lz + ") = " + get(lx, ly, lz));
            else if (ly + 1 - feet > 0.0001) errs.add(where + ": landing is higher than the last step");
            if (!free(lx, ly + 1, lz) || !free(lx, ly + 2, lz)) errs.add(where + ": no headroom on the landing");
        }
        return errs;
    }


    // ------------------------------------------------------------------ routes (offline route check)

    /**
     * An intended exit of a spawn building (a doorway box: blocking every cell in it closes that exit) or a lane
     * waypoint (a single standing spot, feet at y1) that must be reachable from the side's spawn room.
     */
    public static final class Route {
        public final String name;
        public final boolean exit, sideB;
        public final int x1, y1, z1, x2, y2, z2;
        public Route(String name, boolean exit, boolean sideB, int x1, int y1, int z1, int x2, int y2, int z2) {
            this.name = name; this.exit = exit; this.sideB = sideB;
            this.x1 = Math.min(x1, x2); this.y1 = Math.min(y1, y2); this.z1 = Math.min(z1, z2);
            this.x2 = Math.max(x1, x2); this.y2 = Math.max(y1, y2); this.z2 = Math.max(z1, z2);
        }
        public boolean contains(int x, int y, int z) {
            return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
        }
    }

    private final List<Route> routes = new ArrayList<>();

    public void addRoute(Route r) { routes.add(r); }

    public List<Route> routes() { return routes; }

    // ------------------------------------------------------------------ dump

    /** Write an MCMDUMP file (gzip). {@code filter} may drop cells (null keeps all). */
    public void writeDump(File f, DumpFilter filter) throws IOException {
        try (BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(f)),
                StandardCharsets.UTF_8))) {
            w.write("MCMDUMP 1 " + sx + " " + sy + " " + sz + "\n");
            for (int y = minY; y <= maxY; y++)
                for (int z = minZ; z <= maxZ; z++)
                    for (int x = minX; x <= maxX; x++) {
                        int v = cells[idx(x, y, z)];
                        if (v == 0) continue;
                        if (filter != null && !filter.keep(x, y, z)) continue;
                        w.write((x - minX) + " " + (y - minY) + " " + (z - minZ) + " " + palette.get(v - 1) + "\n");
                    }
        }
    }

    public interface DumpFilter { boolean keep(int x, int y, int z); }

    /** Visit every non-air cell. */
    public void forEach(CellVisitor v) {
        for (int y = minY; y <= maxY; y++)
            for (int z = minZ; z <= maxZ; z++)
                for (int x = minX; x <= maxX; x++) {
                    int c = cells[idx(x, y, z)];
                    if (c != 0) v.visit(x, y, z, palette.get(c - 1));
                }
    }

    public interface CellVisitor { void visit(int x, int y, int z, String block); }
}
