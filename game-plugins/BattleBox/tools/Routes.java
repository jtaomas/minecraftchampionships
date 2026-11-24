import com.mcm.games.battlebox.arena.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline route check (walkability flood-fill) over a generated arena grid.
 *
 * Movement model: a player stands on any block with a top (full blocks, slabs, stairs, carpets, fence tops) with
 * 1.8 blocks of free body space; moves to the 4 neighbouring columns may rise at most 1 block (walking up onto a
 * stair or slab, or a jump of 1 with headroom over the take-off spot), may drop any height, and need free space in the
 * target column from its feet up to the higher of the two head heights. Ladders can be climbed up and down.
 * The gate glass counts as open, the 9 centre cells and lava as solid. The goal is standing on the hazard ring
 * around the centre cells (feet y RING_Y + 1 = 3).
 *
 * Per side: (1) the centre and every lane waypoint must be reachable from the spawn room; (2) for every intended
 * exit, with all the side's other exits closed the centre must still be reachable (the exit works and leads
 * somewhere); (3) with all exits closed the centre must be unreachable (no unregistered way out).
 */
public final class Routes {
    private final Grid g;
    private final ArenaGeo geo;
    private List<Grid.Route> closed = new ArrayList<>();

    /** Strict player-box model (default). false = the round-2 model, kept only to report what it missed. */
    final boolean strict;

    Routes(Grid g, ArenaGeo geo) { this(g, geo, true); }

    Routes(Grid g, ArenaGeo geo, boolean strict) { this.g = g; this.geo = geo; this.strict = strict; }

    private boolean isClosed(int x, int y, int z) {
        for (Grid.Route r : closed) if (r.contains(x, y, z)) return true;
        return false;
    }

    private boolean gateGlass(int x, int y, int z) {
        return Math.abs(z) == geo.W && Math.abs(x) <= geo.GATE_HALF && y >= 1 && y <= geo.GATE_TOP
                && States.id(g.get(x, y, z)).endsWith("_stained_glass");
    }

    boolean pass(int x, int y, int z) {
        if (isClosed(x, y, z)) return false;
        if (gateGlass(x, y, z)) return true;
        if (geo.isCentreCell(x, y, z)) return false;
        String b = g.get(x, y, z);
        if (States.id(b).equals("lava")) return false;
        return Shapes.passable(b);
    }

    private double top(int x, int y, int z) {
        if (isClosed(x, y, z) || gateGlass(x, y, z)) return -1;
        if (geo.isCentreCell(x, y, z)) return 1.0;
        return Shapes.standTop(g.get(x, y, z));
    }

    private boolean ladder(int x, int y, int z) { return !isClosed(x, y, z) && States.id(g.get(x, y, z)).equals("ladder"); }

    /** A place a player can be: column (x, z), feet height f, lowest body cell lo. */
    private static final class Node {
        final int x, z, lo; final double f; final boolean step;
        Node(int x, int z, double f, int lo, boolean step) { this.x = x; this.z = z; this.f = f; this.lo = lo; this.step = step; }
        long key() { return key(x, z, f); }
        static long key(int x, int z, double f) { return (((long) (x + 512)) << 40) | (((long) (z + 512)) << 20) | (Math.round(f * 16) + 4096); }
    }

    private Map<Long, List<Node>> cols;
    private Map<Long, Node> byKey;

    private static long colKey(int x, int z) { return (((long) (x + 512)) << 20) | (z + 512); }

    private boolean clear(int x, int z, int lo, int hi) {
        for (int y = lo; y <= hi; y++) if (!pass(x, y, z)) return false;
        return true;
    }

    private void build() {
        cols = new HashMap<>();
        byKey = new HashMap<>();
        for (int x = g.minX; x <= g.maxX; x++)
            for (int z = g.minZ; z <= g.maxZ; z++) {
                List<Node> l = new ArrayList<>();
                for (int ys = g.minY; ys <= g.maxY - 2; ys++) {
                    double t = top(x, ys, z);
                    if (t < 0) continue;
                    double f = ys + t;
                    int lo = Math.max(ys + 1, (int) Math.floor(f));
                    int hi = (int) Math.floor(f + 1.8 - 1e-9);
                    String sid = States.id(g.get(x, ys, z));
                    if (clear(x, z, lo, hi)) l.add(new Node(x, z, f, lo, sid.endsWith("_stairs") || sid.endsWith("_slab")));
                }
                for (int y = g.minY; y <= g.maxY - 1; y++)
                    if (ladder(x, y, z) && pass(x, y + 1, z) && byKeyOf(l, x, z, y) == null) l.add(new Node(x, z, y, y, false));
                cols.put(colKey(x, z), l);
                for (Node n : l) byKey.put(n.key(), n);
            }
    }

    private static Node byKeyOf(List<Node> l, int x, int z, double f) {
        for (Node n : l) if (Math.abs(n.f - f) < 1e-6) return n;
        return null;
    }

    private static final int[][] D = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private Map<Long, Node> flood(Node start) {
        Map<Long, Node> seen = new HashMap<>();
        ArrayDeque<Node> q = new ArrayDeque<>();
        seen.put(start.key(), start);
        q.add(start);
        while (!q.isEmpty()) {
            Node n = q.poll();
            List<Node> next = new ArrayList<>();
            // ladder: climb up one cell, or let go / climb down to anything below in the same column
            List<Node> own = cols.get(colKey(n.x, n.z));
            int fy = (int) Math.floor(n.f + 1e-6);
            if (ladder(n.x, fy, n.z)) {
                Node up = byKeyOf(own, n.x, n.z, fy + 1);
                if (up != null && pass(n.x, fy + 2, n.z)) next.add(up);
            }
            for (Node m : own) if (m.f < n.f - 1e-6 && clear(n.x, n.z, m.lo, n.lo)) next.add(m);
            for (int[] d : D) {
                List<Node> nb = cols.get(colKey(n.x + d[0], n.z + d[1]));
                if (nb == null) continue;
                for (Node m : nb) {
                    double rise = m.f - n.f;
                    if (rise > 1.0 + 1e-6) continue;
                    int headCell = (int) Math.floor(Math.max(n.f, m.f) + 1.8 - 1e-9);
                    if (!clear(m.x, m.z, m.lo, headCell)) continue;
                    // Player box (0.6 x 1.8): the step-up (stair half, slab, or jump) happens while the box is still
                    // in the column it leaves, so that column needs headroom up to the HIGHER head height too.
                    // (The old check only required this for jumps, so a block over a stair step passed.)
                    if (strict && !clear(n.x, n.z, n.lo, headCell)) continue;
                    if (!strict && rise > 0.6 && !m.step && !clear(n.x, n.z, n.lo, (int) Math.floor(m.f + 1.8 - 1e-9))) continue;
                    next.add(m);
                }
            }
            for (Node m : next) if (seen.putIfAbsent(m.key(), m) == null) q.add(m);
        }
        return seen;
    }

    private Node spawnNode(boolean b) {
        double[] s = geo.spawn(b, 0);
        int x = (int) Math.floor(s[0]), z = (int) Math.floor(s[2]);
        return byKey.get(Node.key(x, z, s[1]));
    }

    private boolean reachesCentre(Map<Long, Node> seen) {
        for (Node n : seen.values())
            if (Math.max(Math.abs(n.x), Math.abs(n.z)) == 2 && Math.abs(n.f - (geo.RING_Y + 1)) < 1e-6) return true;
        return false;
    }

    private boolean reaches(Map<Long, Node> seen, Grid.Route w) {
        for (Node n : seen.values())
            if (n.x == w.x1 && n.z == w.z1 && Math.abs(n.f - w.y1) < 0.6) return true;
        return false;
    }

    /** Every spot reachable from either spawn (no exits closed), as "x,feetY*16,z" keys of the block cell. */
    java.util.Set<String> reachableSpots() {
        java.util.Set<String> out = new java.util.HashSet<>();
        closed = new ArrayList<>();
        build();
        for (boolean b : new boolean[]{false, true}) {
            Node st = spawnNode(b);
            if (st == null) continue;
            for (Node n : flood(st).values()) out.add(n.x + "," + Math.round(n.f * 16) + "," + n.z);
        }
        return out;
    }

    /** Runs the check; ok[0] is set false on any failure. Returns the report text. */
    String report(String id, boolean[] ok) {
        StringBuilder sb = new StringBuilder();
        for (boolean b : new boolean[]{false, true}) {
            String side = b ? "B" : "A";
            List<Grid.Route> exits = new ArrayList<>(), lanes = new ArrayList<>();
            for (Grid.Route r : g.routes()) if (r.sideB == b) (r.exit ? exits : lanes).add(r);
            closed = new ArrayList<>();
            build();
            Node start = spawnNode(b);
            if (start == null) { sb.append(id).append(" side ").append(side).append(": ROUTE FAIL no standing spot at the spawn\n"); ok[0] = false; continue; }
            Map<Long, Node> all = flood(start);
            boolean centre = reachesCentre(all);
            List<String> missing = new ArrayList<>();
            for (Grid.Route w : lanes) if (!reaches(all, w)) missing.add(w.name + " (" + w.x1 + "," + w.y1 + "," + w.z1 + ")");
            if (!centre || !missing.isEmpty()) ok[0] = false;
            sb.append(id).append(" side ").append(side).append(": ").append(all.size()).append(" reachable spots, centre ")
                    .append(centre ? "reachable" : "ROUTE FAIL unreachable").append(", lanes ").append(lanes.size() - missing.size())
                    .append("/").append(lanes.size()).append(missing.isEmpty() ? "" : "  ROUTE FAIL missing: " + String.join(", ", missing)).append('\n');
            List<String> exitRes = new ArrayList<>();
            for (Grid.Route e : exits) {
                closed = new ArrayList<>();
                for (Grid.Route o : exits) if (o != e) closed.add(o);
                build();
                Node st = spawnNode(b);
                boolean via = st != null && reachesCentre(flood(st));
                if (!via) ok[0] = false;
                exitRes.add(e.name + (via ? " OK" : " ROUTE FAIL (blocked)"));
            }
            closed = new ArrayList<>(exits);
            build();
            Node st = spawnNode(b);
            boolean leak = st != null && reachesCentre(flood(st));
            if (leak) ok[0] = false;
            sb.append(id).append(" side ").append(side).append(": exits ").append(String.join(", ", exitRes))
                    .append(leak ? "  ROUTE FAIL: centre reachable with every exit closed (unregistered opening)" : "  (all closed -> sealed)").append('\n');
            closed = new ArrayList<>();
        }
        return sb.toString();
    }
}
