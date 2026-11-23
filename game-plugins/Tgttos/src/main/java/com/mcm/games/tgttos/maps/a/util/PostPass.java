package com.mcm.games.tgttos.maps.a.util;

import com.mcm.games.tgttos.model.TgMapData;

import java.util.ArrayList;
import java.util.List;

/**
 * Final pass over the finished block store (blocks are placed without physics, so nothing is computed by the
 * game): fence / pane / bars / wall / gate connection states, persistent leaves (no decay), and carpets or
 * other support-needing blocks without support removed. {@link #verify} recomputes and counts mismatches.
 */
public final class PostPass {
    private PostPass() {}

    private static final int[][] D4 = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
    private static final String[] DN = {"north", "east", "south", "west"};

    public record Result(int connections, int leaves, int dropped) {}

    public static Result run(TgMapData.Blocks b) {
        List<int[]> conn = new ArrayList<>();
        List<int[]> leaves = new ArrayList<>();
        List<int[]> needSupport = new ArrayList<>();
        b.forEach((x, y, z, s) -> {
            String id = Bs.id(s);
            if (Bs.kind(id) != 0) conn.add(new int[]{x, y, z});
            if (id.endsWith("_leaves")) leaves.add(new int[]{x, y, z});
            if (id.endsWith("_carpet") || id.equals("flower_pot") || id.startsWith("potted_") || id.endsWith("_pressure_plate")
                    || id.equals("short_grass") || id.equals("fern") || id.endsWith("_tulip") || id.equals("poppy") || id.equals("dandelion")
                    || id.equals("cornflower") || id.equals("oxeye_daisy") || id.equals("azure_bluet") || id.equals("allium")
                    || id.equals("lily_of_the_valley") || id.equals("sweet_berry_bush"))
                needSupport.add(new int[]{x, y, z});
        });
        String[] out = new String[conn.size()];
        for (int i = 0; i < conn.size(); i++) { int[] p = conn.get(i); out[i] = connected(b, p[0], p[1], p[2]); }
        for (int i = 0; i < conn.size(); i++) { int[] p = conn.get(i); b.set(p[0], p[1], p[2], out[i]); }
        for (int[] l : leaves) {
            String s = b.get(l[0], l[1], l[2]);
            b.set(l[0], l[1], l[2], Bs.withProp(Bs.withProp(s, "persistent", "true"), "distance", "7"));
        }
        int dropped = 0;
        for (int[] c : needSupport) {
            String below = b.get(c[0], c[1] - 1, c[2]);
            String id = Bs.id(b.get(c[0], c[1], c[2]));
            boolean ok = Bs.fullSolid(below) || (Bs.id(below).endsWith("_slab") && "top".equals(Bs.prop(below, "type")));
            if (id.endsWith("_carpet") || id.endsWith("_pressure_plate")) ok = Bs.fullSolid(below);
            if (!ok) { b.set(c[0], c[1], c[2], null); dropped++; }
        }
        return new Result(conn.size(), leaves.size(), dropped);
    }

    /** Number of connecting blocks whose state differs from the recomputed one (0 = post-pass ran and held). */
    public static int verify(TgMapData.Blocks b) {
        int[] bad = {0};
        b.forEach((x, y, z, s) -> {
            if (Bs.kind(Bs.id(s)) == 0) return;
            if (!Bs.norm(s).equals(Bs.norm(connected(b, x, y, z)))) bad[0]++;
        });
        return bad[0];
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

    private static String connected(TgMapData.Blocks b, int x, int y, int z) {
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
            boolean straightNS = c[0] && c[2] && !c[1] && !c[3];
            boolean straightEW = c[1] && c[3] && !c[0] && !c[2];
            boolean postAbove = aid.contains("lantern") || aid.contains("torch") || aid.contains("banner")
                    || aid.contains("rod") || Bs.kind(aid) == 3;
            boolean up = !(straightNS || straightEW) || postAbove;
            for (int d = 0; d < 4; d++) { if (d > 0) st.append(','); st.append(DN[d]).append('=').append(!c[d] ? "none" : aboveFull ? "tall" : "low"); }
            st.append(",up=").append(up).append(",waterlogged=false");
        } else {
            for (int d = 0; d < 4; d++) { if (d > 0) st.append(','); st.append(DN[d]).append('=').append(c[d]); }
            st.append(",waterlogged=false");
        }
        return head + "[" + st + "]";
    }
}
