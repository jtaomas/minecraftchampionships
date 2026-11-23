package com.mcm.games.skyblockle.map;

import com.mcm.games.skyblockle.MapModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Final passes over the finished block store (blocks are placed without physics, so nothing is computed by the
 * game): fence / pane / bars / wall / gate connection states, leaf distances (so trees behave like natural trees:
 * they stand until chopped, then decay and drop saplings), and carpets that have no support.
 */
final class PostPass {
    private PostPass() {}

    private static final int[][] D4 = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final String[] DN = {"north", "east", "south", "west"};

    record Result(int connections, int leaves, int persistentLeaves, int carpetsDropped) {}

    static Result run(MapModel.Blocks b) {
        List<int[]> conn = new ArrayList<>();
        List<int[]> leaves = new ArrayList<>();
        List<int[]> logs = new ArrayList<>();
        List<int[]> carpets = new ArrayList<>();
        b.forEach((x, y, z, s) -> {
            String id = Bs.id(s);
            if (Bs.kind(id) != 0) conn.add(new int[]{x, y, z});
            if (id.endsWith("_leaves")) leaves.add(new int[]{x, y, z});
            if (id.endsWith("_log") || id.endsWith("_wood")) logs.add(new int[]{x, y, z});
            if (id.endsWith("_carpet")) carpets.add(new int[]{x, y, z});
        });
        String[] out = new String[conn.size()];
        for (int i = 0; i < conn.size(); i++) { int[] p = conn.get(i); out[i] = connected(b, p[0], p[1], p[2]); }
        for (int i = 0; i < conn.size(); i++) { int[] p = conn.get(i); b.set(p[0], p[1], p[2], out[i]); }

        // leaf distance: BFS from logs through leaves (6-neighbourhood), capped at 7
        Map<Long, Integer> dist = new HashMap<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int[] l : logs) { q.add(new int[]{l[0], l[1], l[2], 0}); }
        int[][] d6 = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            if (c[3] >= 6) continue;
            for (int[] d : d6) {
                int nx = c[0] + d[0], ny = c[1] + d[1], nz = c[2] + d[2];
                String s = b.get(nx, ny, nz);
                if (s == null || !Bs.id(s).endsWith("_leaves")) continue;
                long k = Tpl.key(nx, ny, nz);
                if (dist.containsKey(k)) continue;
                dist.put(k, c[3] + 1);
                q.add(new int[]{nx, ny, nz, c[3] + 1});
            }
        }
        int persistent = 0;
        for (int[] l : leaves) {
            String s = b.get(l[0], l[1], l[2]);
            Integer d = dist.get(Tpl.key(l[0], l[1], l[2]));
            String n = Bs.withProp(s, "distance", String.valueOf(d == null ? 7 : d));
            if (d == null) { n = Bs.withProp(n, "persistent", "true"); persistent++; }
            b.set(l[0], l[1], l[2], n);
        }
        int dropped = 0;
        for (int[] c : carpets) if (!Bs.fullSolid(b.get(c[0], c[1] - 1, c[2]))) { b.set(c[0], c[1], c[2], null); dropped++; }
        return new Result(conn.size(), leaves.size(), persistent, dropped);
    }

    private static boolean gateAligned(String gate, int d) {
        String f = Bs.prop(gate, "facing");
        boolean gateNS = "north".equals(f) || "south".equals(f);
        return gateNS == (d == 1 || d == 3);
    }

    private static boolean connects(int k, String selfId, String nb, int d) {
        if (Bs.isAir(nb)) return false;
        String nid = Bs.id(nb);
        int nk = Bs.kind(nid);
        switch (k) {
            case 1:
                if (nk == 1) return selfId.equals("nether_brick_fence") == nid.equals("nether_brick_fence");
                if (nk == 4) return gateAligned(nb, d);
                return Bs.fullSolid(nb);
            case 2:
                return nk == 2 || nk == 3 || Bs.fullSolid(nb);
            case 3:
                if (nk == 3 || nk == 2) return true;
                if (nk == 4) return gateAligned(nb, d);
                return Bs.fullSolid(nb);
            default:
                return false;
        }
    }

    private static String connected(MapModel.Blocks b, int x, int y, int z) {
        String s = b.get(x, y, z);
        String head = Bs.head(s);
        String id = Bs.id(s);
        int k = Bs.kind(id);
        if (k == 4) {
            String facing = Bs.prop(s, "facing");
            if (facing == null) facing = "north";
            String open = Bs.prop(s, "open");
            return head + "[facing=" + facing + ",in_wall=false,open=" + (open == null ? "false" : open) + ",powered=false]";
        }
        boolean[] c = new boolean[4];
        for (int d = 0; d < 4; d++) c[d] = connects(k, id, b.get(x + D4[d][0], y, z + D4[d][1]), d);
        StringBuilder st = new StringBuilder();
        if (k == 3) {
            String above = b.get(x, y + 1, z);
            String aid = Bs.id(above);
            boolean aboveFull = Bs.fullSolid(above);
            String[] side = new String[4];
            for (int d = 0; d < 4; d++) side[d] = !c[d] ? "none" : aboveFull ? "tall" : "low";
            boolean straightNS = c[0] && c[2] && !c[1] && !c[3];
            boolean straightEW = c[1] && c[3] && !c[0] && !c[2];
            boolean postAbove = aid.contains("lantern") || aid.contains("torch") || aid.contains("banner")
                    || aid.contains("rod") || Bs.kind(aid) == 3;
            boolean up = !(straightNS || straightEW) || postAbove;
            for (int d = 0; d < 4; d++) { if (d > 0) st.append(','); st.append(DN[d]).append('=').append(side[d]); }
            st.append(",up=").append(up).append(",waterlogged=false");
        } else {
            for (int d = 0; d < 4; d++) { if (d > 0) st.append(','); st.append(DN[d]).append('=').append(c[d]); }
            st.append(",waterlogged=false");
        }
        return head + "[" + st + "]";
    }
}
