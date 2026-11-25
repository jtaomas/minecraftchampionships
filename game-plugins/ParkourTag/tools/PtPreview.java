import com.mcm.games.parkourtag.map.*;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Offline preview + route checks for Parkour Tag maps (no server needed).
 * Usage: java PtPreview outDir [mapId...]
 *   <id>_full.mcmdump.gz   everything
 *   <id>_pit.mcmdump.gz    barriers removed, stands cut back (for top-down and iso views of the play space)
 * Writes <id>_routes.txt. Exit code 2 if a cage cannot reach the other cage or a reachable spot is a trap.
 *
 * Movement model (conservative, vanilla, no potions): walk / step up 1 / drop any height; jumps over a 1-block gap
 * up 1 or level, over a 2-block gap level or down, diagonal (1,1) up 1, (2,1) level or down; ladders climb.
 */
public final class PtPreview {
    record P(int x, int y, int z) {}

    static MapGrid g;
    static int sx, sy, sz;

    static boolean solid(String b) {
        if (b == null) return false;
        String n = b.replace("minecraft:", "");
        if (n.startsWith("barrier")) return true;
        return !(n.contains("carpet") || n.startsWith("ladder") || n.startsWith("chain") || n.contains("sign")
                || n.startsWith("lantern") || n.contains("_pane") || n.startsWith("iron_bars"));
    }
    static boolean passable(String b) {
        if (b == null) return true;
        String n = b.replace("minecraft:", "");
        return n.contains("carpet") || n.startsWith("ladder") || n.contains("sign") || n.startsWith("lantern") || n.startsWith("chain");
    }
    static boolean ladder(int x, int y, int z) { String b = g.get(x, y, z); return b != null && b.contains("ladder"); }
    static boolean free(int x, int y, int z) { return passable(g.get(x, y, z)); }
    static boolean stand(int x, int y, int z) {
        if (!free(x, y, z) || !free(x, y + 1, z)) return false;
        String below = g.get(x, y - 1, z);
        return (solid(below) && !below.contains("barrier")) || ladder(x, y, z);
    }

    static List<P> moves(P p) {
        List<P> out = new ArrayList<>();
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        if (ladder(p.x, p.y, p.z) && stand(p.x, p.y + 1, p.z)) out.add(new P(p.x, p.y + 1, p.z));
        if (ladder(p.x, p.y, p.z) && stand(p.x, p.y - 1, p.z)) out.add(new P(p.x, p.y - 1, p.z));
        for (int[] d : dirs) {
            // walk / step up 1 / drop
            int nx = p.x + d[0], nz = p.z + d[1];
            if (stand(nx, p.y + 1, nz) && free(p.x, p.y + 2, p.z)) out.add(new P(nx, p.y + 1, nz));
            for (int dy = 0; dy >= -12; dy--) {
                if (!free(nx, p.y + dy, nz) || !free(nx, p.y + dy + 1, nz)) break;
                if (stand(nx, p.y + dy, nz)) { out.add(new P(nx, p.y + dy, nz)); break; }
            }
            // gap jumps straight: distance 2 (up 1 / level / down), distance 3 (level / down)
            for (int dist = 2; dist <= 3; dist++) {
                boolean clear = true;
                for (int k = 1; k < dist; k++) for (int h = 0; h <= 2; h++) if (!free(p.x + d[0] * k, p.y + h, p.z + d[1] * k)) clear = false;
                if (!clear || !free(p.x, p.y + 2, p.z)) continue;
                int tx = p.x + d[0] * dist, tz = p.z + d[1] * dist;
                for (int dy = (dist == 2 ? 1 : 0); dy >= -6; dy--) if (stand(tx, p.y + dy, tz)) { out.add(new P(tx, p.y + dy, tz)); break; }
            }
        }
        // diagonals: (1,1) up 1 / level / down, (2,1) level / down
        int[][] diag = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}, {2, 1}, {2, -1}, {-2, 1}, {-2, -1}, {1, 2}, {1, -2}, {-1, 2}, {-1, -2}};
        for (int[] d : diag) {
            boolean far = Math.abs(d[0]) + Math.abs(d[1]) == 3;
            int tx = p.x + d[0], tz = p.z + d[1];
            if (!free(p.x, p.y + 2, p.z)) continue;
            // corner cells must be open at head height so the jump is not blocked
            if (!far && !(free(p.x + d[0], p.y + 1, p.z) || free(p.x, p.y + 1, p.z + d[1]))) continue;
            for (int dy = far ? 0 : 1; dy >= -6; dy--) if (stand(tx, p.y + dy, tz)) { out.add(new P(tx, p.y + dy, tz)); break; }
        }
        return out;
    }

    static Set<P> bfs(P start, Map<P, List<P>> graph, boolean reverse) {
        Set<P> seen = new HashSet<>();
        Deque<P> q = new ArrayDeque<>();
        seen.add(start); q.add(start);
        while (!q.isEmpty()) {
            P p = q.poll();
            for (P n : graph.getOrDefault(p, List.of())) if (seen.add(n)) q.add(n);
        }
        return seen;
    }

    static int dist(P a, Set<P> goals, Map<P, List<P>> graph) {
        Map<P, Integer> d = new HashMap<>();
        Deque<P> q = new ArrayDeque<>();
        d.put(a, 0); q.add(a);
        while (!q.isEmpty()) {
            P p = q.poll();
            if (goals.contains(p)) return d.get(p);
            for (P n : graph.getOrDefault(p, List.of())) if (!d.containsKey(n)) { d.put(n, d.get(p) + 1); q.add(n); }
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "target/offline");
        out.mkdirs();
        boolean broken = false;
        for (PtMap m : Maps.all()) {
            if (args.length > 1 && !Arrays.asList(args).subList(1, args.length).contains(m.id())) continue;
            g = MapGrid.of(m);
            sx = m.sizeX(); sy = m.sizeY(); sz = m.sizeZ();
            StringBuilder rep = new StringBuilder(m.id() + ": " + g.count() + " blocks, size " + sx + "x" + sy + "x" + sz + "\n");
            g.writeDump(new File(out, m.id() + "_full.mcmdump.gz"), (x, y, z, b) -> !b.contains("barrier"));
            // pit view: no barriers, nothing above the stands' first rows
            g.writeDump(new File(out, m.id() + "_pit.mcmdump.gz"), (x, y, z, b) -> !b.contains("barrier") && y < 16);

            for (Cage c : m.cages()) for (int[] d : c.doorCells()) g.set(d[0], d[1], d[2], "air");   // doors open (round live)
            // graph over the containment box (the pit and the cages): standable cells inside the barrier ring
            Map<P, List<P>> graph = new HashMap<>();
            Map<P, List<P>> rev = new HashMap<>();
            for (int x = 0; x < sx; x++) for (int z = 0; z < sz; z++) for (int y = 1; y < sy - 1; y++) {
                if (!stand(x, y, z)) continue;
                P p = new P(x, y, z);
                List<P> ms = moves(p);
                graph.put(p, ms);
                for (P n : ms) rev.computeIfAbsent(n, k -> new ArrayList<>()).add(p);
            }
            List<P> cageSpots = new ArrayList<>();
            for (Cage c : m.cages()) { double[] s = c.slot(0); cageSpots.add(new P((int) Math.floor(s[0]), (int) s[1], (int) Math.floor(s[2]))); }
            Set<P> fromA = bfs(cageSpots.get(0), graph, false), fromB = bfs(cageSpots.get(1), graph, false);
            Set<P> toA = bfs(cageSpots.get(0), rev, false), toB = bfs(cageSpots.get(1), rev, false);
            // cage doors are glass while the cage is shut; the graph treats them as solid, so open them: walk out manually
            rep.append("  standable cells: ").append(graph.size()).append("\n");
            rep.append("  reachable from cage A: ").append(fromA.size()).append(", from cage B: ").append(fromB.size()).append("\n");
            Set<P> traps = new HashSet<>(fromA);
            traps.removeAll(toA);
            Set<P> trapsB = new HashSet<>(fromB);
            trapsB.removeAll(toB);
            traps.addAll(trapsB);
            rep.append("  traps (reachable but no way back to the cage): ").append(traps.size()).append("\n");
            int shown = 0;
            for (P t : traps) { if (shown++ < 12) rep.append("    trap at ").append(t).append(" ").append(g.get(t.x, t.y - 1, t.z)).append("\n"); }
            boolean aToB = fromA.contains(cageSpots.get(1)), bToA = fromB.contains(cageSpots.get(0));
            rep.append("  cage A -> cage B: ").append(aToB).append(" (").append(dist(cageSpots.get(0), Set.of(cageSpots.get(1)), graph)).append(" moves), cage B -> cage A: ")
                    .append(bToA).append(" (").append(dist(cageSpots.get(1), Set.of(cageSpots.get(0)), graph)).append(" moves)\n");
            // heights reached: how many distinct standing heights and per-height cell counts
            TreeMap<Integer, Integer> byY = new TreeMap<>();
            for (P p : fromA) byY.merge(p.y, 1, Integer::sum);
            rep.append("  reachable cells per feet height: ").append(byY).append("\n");
            // unreachable roofs: standable cells above the street not reached
            int[] box = m.playBox();
            TreeMap<String, Integer> unreached = new TreeMap<>();
            int pitCells = 0, pitReached = 0;
            for (P p : graph.keySet()) {
                if (p.x < box[0] || p.x > box[2] || p.z < box[1] || p.z > box[3]) continue;
                pitCells++;
                if (fromA.contains(p)) { pitReached++; continue; }
                String below = g.get(p.x, p.y - 1, p.z).replace("minecraft:", "");
                unreached.merge(below + "@y" + p.y, 1, Integer::sum);
            }
            rep.append("  play area: ").append(pitReached).append("/").append(pitCells).append(" standable cells reachable\n");
            rep.append("  unreachable in the play area (surface@feetY=count): ").append(unreached).append("\n");
            if (!aToB || !bToA || !traps.isEmpty()) broken = true;
            System.out.print(rep);
            Files.write(new File(out, m.id() + "_routes.txt").toPath(), rep.toString().getBytes(StandardCharsets.UTF_8));
        }
        if (broken) System.exit(2);
    }
}
