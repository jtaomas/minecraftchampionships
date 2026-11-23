package com.mcm.lobby;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Post-pass over the finished hub grid. The core places blocks without physics, so every connection state has
 * to be baked in here, following vanilla rules:
 * <ul>
 *   <li>fences connect to same-family fences, aligned fence gates and sturdy faces (not leaves/barriers/...);</li>
 *   <li>glass panes / iron bars connect to panes, bars, walls and sturdy faces;</li>
 *   <li>walls connect to walls, panes, bars, aligned gates and sturdy faces; sides are low/tall (tall when the
 *       block above covers that side) and {@code up} follows WallBlock.shouldRaisePost;</li>
 *   <li>fence gates get {@code in_wall} next to walls;</li>
 *   <li>carpets that do not rest on a full top face (slabs, stairs, fences, air ...) are removed.</li>
 * </ul>
 */
final class BlockFix {
    private BlockFix() {}

    static final int FENCE = 1, PANE = 2, WALL = 3, GATE = 4, CARPET = 5;

    /** Counters of the last run (for the preview log): fences, panes, walls, gates, carpets removed. */
    static int[] apply(Vox v) {
        List<String> pal = v.palette();
        int[] kind = new int[pal.size()];
        for (int i = 1; i < pal.size(); i++) {
            String id = Shapes.base(pal.get(i));
            kind[i] = Shapes.isFenceGate(id) ? GATE : Shapes.isFence(id) ? FENCE : Shapes.isPane(id) ? PANE
                    : Shapes.isWall(id) ? WALL : Shapes.isCarpet(id) ? CARPET : 0;
        }
        List<int[]> todo = new ArrayList<>();
        for (int[] s : v.sectionCoords()) {
            short[] sec = v.section(s[0], s[1], s[2]);
            int bx = s[0] << 4, by = s[1] << 4, bz = s[2] << 4;
            for (int i = 0; i < 4096; i++) {
                int id = sec[i];
                if (id <= 0 || id >= kind.length || kind[id] == 0) continue;
                todo.add(new int[]{bx + (i & 15), by + (i >> 8), bz + ((i >> 4) & 15), kind[id]});
            }
        }
        // walls depend on the (side) state of the wall above -> top-down
        todo.sort((a, b) -> Integer.compare(b[1], a[1]));
        int[] count = new int[6];
        // 1. carpets first (removing one can change nothing else that matters here)
        for (int[] t : todo) if (t[3] == CARPET) {
            if (!Shapes.fullTop(v.get(t[0], t[1] - 1, t[2]))) { v.set(t[0], t[1], t[2], null); count[CARPET]++; }
        }
        // 2. gates (in_wall) and horizontal connections of fences/panes; wall sides; then wall posts
        for (int[] t : todo) {
            int x = t[0], y = t[1], z = t[2];
            String cur = v.get(x, y, z);
            String id = Shapes.base(cur);
            Map<String, String> p = Shapes.props(cur);
            switch (t[3]) {
                case FENCE -> {
                    for (int d = 0; d < 4; d++) p.put(Shapes.DIRS[d], String.valueOf(fenceConnects(id, v.get(x + Shapes.DXZ[d][0], y, z + Shapes.DXZ[d][1]), d)));
                    p.putIfAbsent("waterlogged", "false");
                    count[FENCE]++;
                }
                case PANE -> {
                    for (int d = 0; d < 4; d++) p.put(Shapes.DIRS[d], String.valueOf(paneConnects(v.get(x + Shapes.DXZ[d][0], y, z + Shapes.DXZ[d][1]), d)));
                    p.putIfAbsent("waterlogged", "false");
                    count[PANE]++;
                }
                case WALL -> {
                    String above = v.get(x, y + 1, z);
                    for (int d = 0; d < 4; d++) {
                        boolean c = wallConnects(v.get(x + Shapes.DXZ[d][0], y, z + Shapes.DXZ[d][1]), d);
                        p.put(Shapes.DIRS[d], !c ? "none" : coversSide(above, d) ? "tall" : "low");
                    }
                    p.put("up", String.valueOf(raisePost(p, above)));
                    p.putIfAbsent("waterlogged", "false");
                    count[WALL]++;
                }
                case GATE -> {
                    int f = Math.max(0, Shapes.dirIndex(p.getOrDefault("facing", "north")));
                    int l = (f + 1) & 3, r = (f + 3) & 3;
                    boolean inWall = Shapes.isWall(Shapes.base(v.get(x + Shapes.DXZ[l][0], y, z + Shapes.DXZ[l][1])))
                            || Shapes.isWall(Shapes.base(v.get(x + Shapes.DXZ[r][0], y, z + Shapes.DXZ[r][1])));
                    p.put("facing", Shapes.DIRS[f]);
                    p.put("in_wall", String.valueOf(inWall));
                    p.putIfAbsent("open", "false");
                    p.putIfAbsent("powered", "false");
                    count[GATE]++;
                }
                default -> { continue; }
            }
            v.set(x, y, z, Shapes.state(id, sorted(p)));
        }
        return count;
    }

    private static Map<String, String> sorted(Map<String, String> p) {
        Map<String, String> m = new LinkedHashMap<>();
        p.keySet().stream().sorted().forEach(k -> m.put(k, p.get(k)));
        return m;
    }

    /** Fence gate attaches on direction d (from the neighbour's view) when its bar runs across d. */
    static boolean gateAligned(String gate, int d) {
        int f = Shapes.dirIndex(Shapes.prop(gate, "facing", "north"));
        return f >= 0 && (f & 1) != (d & 1);
    }

    /** Does a fence of type {@code self} connect to {@code n} lying in direction d? */
    static boolean fenceConnects(String self, String n, int d) {
        String id = Shapes.base(n);
        if (Shapes.isAir(id)) return false;
        if (Shapes.isFence(id)) return Shapes.isWoodenFence(id) == Shapes.isWoodenFence(self);
        if (Shapes.isFenceGate(id)) return gateAligned(n, d);
        return !Shapes.isConnectionException(id) && Shapes.sturdyFace(n, Shapes.opposite(d));
    }

    static boolean paneConnects(String n, int d) {
        String id = Shapes.base(n);
        if (Shapes.isAir(id)) return false;
        if (Shapes.isPane(id) || Shapes.isWall(id)) return true;
        return !Shapes.isConnectionException(id) && Shapes.sturdyFace(n, Shapes.opposite(d));
    }

    static boolean wallConnects(String n, int d) {
        String id = Shapes.base(n);
        if (Shapes.isAir(id)) return false;
        if (Shapes.isWall(id) || Shapes.isPane(id)) return true;
        if (Shapes.isFenceGate(id)) return gateAligned(n, d);
        return !Shapes.isConnectionException(id) && Shapes.sturdyFace(n, Shapes.opposite(d));
    }

    /** Does the block above cover the wall's side d at its bottom face (-> tall side)? */
    static boolean coversSide(String above, int d) {
        String id = Shapes.base(above);
        if (Shapes.isAir(id)) return false;
        if (Shapes.isWall(id)) return !"none".equals(Shapes.prop(above, Shapes.DIRS[d], "none"));
        if (Shapes.isFence(id) || Shapes.isPane(id)) return "true".equals(Shapes.prop(above, Shapes.DIRS[d], "false"));
        if (Shapes.isSlab(id)) return !"top".equals(Shapes.prop(above, "type", "bottom"));
        if (Shapes.isStairs(id)) return "bottom".equals(Shapes.prop(above, "half", "bottom")) || Shapes.dirIndex(Shapes.prop(above, "facing", "north")) == d;
        return Shapes.isFullCube(above);
    }

    /** WallBlock.shouldRaisePost. */
    static boolean raisePost(Map<String, String> p, String above) {
        String aid = Shapes.base(above);
        if (Shapes.isWall(aid) && "true".equals(Shapes.prop(above, "up", "false"))) return true;
        String n = p.get("north"), e = p.get("east"), s = p.get("south"), w = p.get("west");
        boolean nN = "none".equals(n), eN = "none".equals(e), sN = "none".equals(s), wN = "none".equals(w);
        if ((nN && sN && wN && eN) || nN != sN || wN != eN) return true;
        if ("tall".equals(n) && "tall".equals(s) || "tall".equals(e) && "tall".equals(w)) return false;
        return coversPost(above);
    }

    /** Is the 2x2 px post-test column at the bottom of the block above covered (or a post-override block)? */
    static boolean coversPost(String above) {
        String id = Shapes.base(above);
        if (Shapes.isAir(id)) return false;
        if (Shapes.passable(above)) {
            // torches, signs, banners, pressure plates are #wall_post_override
            return id.endsWith("torch") || id.endsWith("_sign") || id.endsWith("_banner") || id.endsWith("_pressure_plate") || id.equals("tripwire");
        }
        if (Shapes.isSlab(id)) return !"top".equals(Shapes.prop(above, "type", "bottom"));
        if (Shapes.isStairs(id)) return "bottom".equals(Shapes.prop(above, "half", "bottom"));
        if (id.equals("lantern") || id.equals("soul_lantern")) return !"true".equals(Shapes.prop(above, "hanging", "false"));
        if (id.equals("chain")) return "y".equals(Shapes.prop(above, "axis", "y"));
        if (id.equals("end_rod") || id.equals("lightning_rod")) return true;
        return true; // full cubes, fences, walls, campfires, pots ... all reach down to the centre
    }
}
