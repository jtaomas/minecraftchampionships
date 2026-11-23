package com.mcm.games.parkourwarrior.map;

import com.mcm.games.parkourwarrior.CourseModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One obstacle under construction, in its own local frame: x = sideways, y = up, z = forward. Local (0,0,0) is
 * the centre block of the start area (the previous pad / hub deck edge; feet at y = 1). The obstacle ends on a
 * 3x3 checkpoint pad placed with {@link #end}. Facing strings are written as if forward were south (+z) and are
 * rotated with the frame.
 *
 * <p>The intended route is registered as an ordered list of {@link Node}s (foothold areas, ladders, slime,
 * timed blocks); the validator checks every consecutive pair.
 */
final class Ob {
    enum T { AREA, LADDER, SLIME, ICE, TIMED }

    /** A foothold group. cells = world positions of the blocks stood on (for LADDER: the ladder blocks). */
    static final class Node {
        final T t;
        final List<int[]> cells = new ArrayList<>();
        boolean neo;          // the jump INTO this node goes around a wall (no straight-line clearance)
        String note;
        Node(T t) { this.t = t; }
    }

    final W w;
    final String id, name, idea;
    final CourseModel.Kind kind;
    final int stage, index;
    final CourseModel.Difficulty ending;
    final int ox, oy, oz, q;
    final List<Node> route = new ArrayList<>();
    /** Extra legit standable cells (parts of the foothold objects that are not on the route itself). */
    final Set<Long> safe = new HashSet<>();
    final Map<Long, String> timedPreview = new HashMap<>();
    final List<CourseModel.Timed> timed = new ArrayList<>();
    /** Must pass the easy jump table. */
    boolean easy;
    int killDepth = 5;
    String difficultyTag = "";       // EASY / MEDIUM / HARD sign for bonus obstacles
    int endX, endY, endZ;            // local centre of the completion pad
    CourseModel.Box padBox;
    CourseModel.Obstacle built;
    int killY;

    Ob(W w, String id, String name, String idea, CourseModel.Kind kind, int stage, int index,
       CourseModel.Difficulty ending, int ox, int oy, int oz, int q) {
        this.w = w; this.id = id; this.name = name; this.idea = idea; this.kind = kind; this.stage = stage;
        this.index = index; this.ending = ending; this.ox = ox; this.oy = oy; this.oz = oz; this.q = q & 3;
        w.obs.add(this);
        // start area (pad or deck provided by the caller)
        area(-1, 0, -1, 1, 1);
    }

    /** Where an obstacle goes: id/kind and its local frame origin (start-area centre block) and quarter turns. */
    record Slot(String id, CourseModel.Kind kind, int stage, int index, CourseModel.Difficulty end, int x, int y, int z, int q) {}

    Ob(W w, Slot s, String name, String idea) {
        this(w, s.id(), name, idea, s.kind(), s.stage(), s.index(), s.end(), s.x(), s.y(), s.z(), s.q());
    }

    // ------------------------------------------------------------------ frame

    int wx(int x, int z) { return ox + Bs.rx(x, z, q); }
    int wz(int x, int z) { return oz + Bs.rz(x, z, q); }
    int wy(int y) { return oy + y; }

    void set(int x, int y, int z, String s) { w.set(wx(x, z), wy(y), wz(x, z), s == null ? null : Bs.rot(Bs.full(s), q)); }

    void soft(int x, int y, int z, String s) { if (air(x, y, z)) set(x, y, z, s); }

    String get(int x, int y, int z) { return w.get(wx(x, z), wy(y), wz(x, z)); }

    boolean air(int x, int y, int z) { return Bs.isAir(get(x, y, z)); }

    void fill(int x0, int y0, int z0, int x1, int y1, int z1, String s) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) set(x, y, z, s);
    }

    /** Local facing name -> world facing name. */
    String face(String localFacing) { return Bs.prop(Bs.rot("x[facing=" + localFacing + "]", q), "facing"); }

    // ------------------------------------------------------------------ route

    Node area(int x0, int y, int z0, int x1, int z1) { return areaT(T.AREA, x0, y, z0, x1, z1); }

    Node areaT(T t, int x0, int y, int z0, int x1, int z1) {
        Node n = new Node(t);
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                n.cells.add(new int[]{wx(x, z), wy(y), wz(x, z)});
        route.add(n);
        return n;
    }

    /** Area from explicit local cells {x, y, z}. */
    Node cells(T t, int[]... c) {
        Node n = new Node(t);
        for (int[] p : c) n.cells.add(new int[]{wx(p[0], p[2]), wy(p[1]), wz(p[0], p[2])});
        route.add(n);
        return n;
    }

    /** Mark a local rect as legit standable (part of a foothold object, not a decoy). */
    void safe(int x0, int y, int z0, int x1, int z1) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) safe.add(W.key(wx(x, z), wy(y), wz(x, z)));
    }

    /**
     * Ladder column at local (x, y0..y1, z); {@code facing} is the ladder's facing (it hangs on the block on the
     * opposite side, which must be solid).
     */
    Node ladder(int x, int y0, int y1, int z, String facing) {
        Node n = new Node(T.LADDER);
        for (int y = y0; y <= y1; y++) {
            set(x, y, z, "ladder[facing=" + facing + ",waterlogged=false]");
            n.cells.add(new int[]{wx(x, z), wy(y), wz(x, z)});
        }
        route.add(n);
        return n;
    }

    /** Vine strip (climbable like a ladder for the validator); vines hang on the block on side {@code side}. */
    Node vine(int x, int y0, int y1, int z, String side) {
        Node n = new Node(T.LADDER);
        n.note = "vine";
        String st = "vine[east=false,north=false,south=false,up=false,west=false]";
        st = Bs.withProp(st, side, "true");
        for (int y = y0; y <= y1; y++) {
            set(x, y, z, st);
            n.cells.add(new int[]{wx(x, z), wy(y), wz(x, z)});
        }
        route.add(n);
        return n;
    }

    /**
     * Timed blocks (toggled by the runtime, NOT placed in the static store). Adds a TIMED route node.
     * @param cells local {x,y,z}
     */
    Node timed(String state, int period, int on, int phase, int warn, String warnState, int[]... cells) {
        Node n = new Node(T.TIMED);
        List<CourseModel.BlockPos> ps = new ArrayList<>();
        String ws = Bs.rot(Bs.full(state), q);
        for (int[] c : cells) {
            int X = wx(c[0], c[2]), Y = wy(c[1]), Z = wz(c[0], c[2]);
            w.set(X, Y, Z, null);
            ps.add(new CourseModel.BlockPos(X, Y, Z));
            n.cells.add(new int[]{X, Y, Z});
            timedPreview.put(W.key(X, Y, Z), ws);
        }
        timed.add(new CourseModel.Timed(id, ps, ws, period, on, phase, warn, warnState == null ? null : Bs.full(warnState), null));
        n.note = "timed " + on + "/" + period + "t phase " + phase;
        route.add(n);
        return n;
    }

    /** Completion pad: 3x3 pad blocks centred on local (x, y, z), registered as the last route node. */
    void end(int x, int y, int z) {
        endX = x; endY = y; endZ = z;
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            set(x + dx, y, z + dz, W.PAD);
            for (int h = 1; h <= 3; h++) if (!air(x + dx, y + h, z + dz) && !Bs.passable(get(x + dx, y + h, z + dz))) set(x + dx, y + h, z + dz, null);
        }
        area(x - 1, y, z - 1, x + 1, z + 1);
        int ax = wx(x - 1, z - 1), az = wz(x - 1, z - 1), bx = wx(x + 1, z + 1), bz = wz(x + 1, z + 1);
        padBox = new CourseModel.Box(ax, wy(y), az, bx, wy(y) + 2, bz);
    }

    /** World position of local (x, y, z) as feet position (block centre). */
    CourseModel.Pos pos(int x, int y, int z, float yawOffset) {
        // forward local +z = world south (yaw 0) when q = 0; Bs.rot(q=1) maps south -> west = yaw 90
        float yaw = (float) (((90.0 * q) + yawOffset) % 360.0);
        return CourseModel.Pos.block(wx(x, z), wy(y), wz(x, z), yaw);
    }

    int minFeet() {
        int m = Integer.MAX_VALUE;
        for (Node n : route) for (int[] c : n.cells) m = Math.min(m, c[1] + (n.t == T.LADDER ? 0 : 1));
        return m;
    }

    int maxFeet() {
        int m = Integer.MIN_VALUE;
        for (Node n : route) for (int[] c : n.cells) m = Math.max(m, c[1] + 1);
        return m;
    }
}
