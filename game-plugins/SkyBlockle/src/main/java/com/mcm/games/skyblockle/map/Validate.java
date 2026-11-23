package com.mcm.games.skyblockle.map;

import com.mcm.games.skyblockle.MapModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Offline (and optional runtime) checks of a generated map: bridging gaps along every team's path, shared-island
 * links, island overlaps, wedge identity, chest reachability (island graph + walking), staircases and ladders.
 */
public final class Validate {
    private Validate() {}

    public static final class Report {
        public final StringBuilder text = new StringBuilder();
        public int errors;
        double pathMin = Double.MAX_VALUE, pathMax = 0;

        void line(String s) { text.append(s).append('\n'); }
        void err(String s) { errors++; line("  ERROR " + s); }
    }

    static final String[] PATH = {"start", "pillar", "fort", "spike", "enchanting", "castle"};

    public static Report run(SkyMap.Build b) {
        Report r = new Report();
        MapModel m = b.model;
        int n = m.wedges;
        r.line("Sky Blockle map, " + n + " wedges, seed " + m.seed + ": " + m.blocks.size() + " blocks, "
                + (b.insts.size() - 4) + " islands (incl. castle, " + b.islets + " stepping rocks), " + m.chests.size() + " chests");
        r.line("post-pass: " + b.post.connections() + " fence/pane/wall states set, " + b.post.leaves() + " leaves ("
                + b.post.persistentLeaves() + " persistent), " + b.post.carpetsDropped() + " carpets dropped");
        gaps(b, r);
        overlaps(b, r);
        identity(b, r);
        chests(b, r);
        List<String> st = Stairs.validate(m.blocks, b.lanes);
        r.line("staircases: " + b.lanes.size() + " lanes walked bottom to top" + (st.isEmpty() ? ", all OK" : ""));
        for (String e : st) r.err("stairs " + e);
        climbs(b, r);
        castleWalk(b, r);
        r.line(r.errors == 0 ? "RESULT: all checks passed" : "RESULT: " + r.errors + " problem(s)");
        return r;
    }

    private static SkyMap.Inst find(SkyMap.Build b, String role, int wedge) {
        for (SkyMap.Inst i : b.insts) if (i.role.equals(role) && (i.wedge == wedge || role.equals("castle"))) return i;
        return null;
    }

    private static void gaps(SkyMap.Build b, Report r) {
        int n = b.model.wedges;
        r.line("bridging gaps along each team's path (blocks to bridge; required " + SkyMap.GAP_MIN + ".." + SkyMap.GAP_MAX + "):");
        double[][] mm = new double[PATH.length][2];
        for (double[] a : mm) { a[0] = 1e9; a[1] = -1; }
        double genMin = 1e9, genMax = -1;
        for (int w = 0; w < n; w++) {
            StringBuilder sb = new StringBuilder("  wedge " + w + ": ");
            SkyMap.Inst g = find(b, "generator", w), st = find(b, "start", w);
            double gg = SkyMap.gap(st, g)[0];
            genMin = Math.min(genMin, gg); genMax = Math.max(genMax, gg);
            sb.append(String.format("gen|%.0f| ", gg));
            for (int k = 0; k + 1 < PATH.length; k++) {
                SkyMap.Inst a = find(b, PATH[k], w), c = find(b, PATH[k + 1], w);
                double gp = SkyMap.gap(a, c)[0];
                int dy = c.y - a.y;
                mm[k][0] = Math.min(mm[k][0], gp); mm[k][1] = Math.max(mm[k][1], gp);
                r.pathMin = Math.min(r.pathMin, gp); r.pathMax = Math.max(r.pathMax, gp);
                sb.append(String.format("%s->%s %.1f (dy %+d)  ", PATH[k], PATH[k + 1], gp, c.role.equals("castle") ? b.model.castleY + 6 - a.y : dy));
                if (gp < SkyMap.GAP_MIN || gp > SkyMap.GAP_MAX) r.err("wedge " + w + " gap " + PATH[k] + "->" + PATH[k + 1] + " = " + String.format("%.1f", gp));
            }
            if (w < 3 || w == n - 1) r.line(sb.toString());
        }
        r.line(String.format("  generator shrine sits directly behind the start: gap %.0f..%.0f (research: almost touching)", genMin, genMax));
        for (int k = 0; k + 1 < PATH.length; k++)
            r.line(String.format("  %-10s -> %-10s min %.1f  max %.1f", PATH[k], PATH[k + 1], mm[k][0], mm[k][1]));
        r.line(String.format("  PATH GAPS overall: min %.1f, max %.1f", r.pathMin, r.pathMax));
        // shared spoke and links to it (wedge 0 side, all other sides are copies)
        r.line("shared spoke between wedge 0 and wedge 1 (nearest gap of each island to anything else):");
        for (SkyMap.Inst i : b.insts) {
            if (i.axis || i.slot != 0 || i.role.equals("tower") || i.role.equals("castle")) continue;
            double best = 1e9;
            String bestRole = "";
            for (SkyMap.Inst o : b.insts) {
                if (o == i || o.role.equals("tower") || !SkyMap.coarseNear(i, o, 40)) continue;
                double gp = SkyMap.gap(i, o)[0];
                if (gp < best) { best = gp; bestRole = o.role + (o.axis ? "(w" + o.wedge + ")" : ""); }
            }
            r.line(String.format("  %-14s r=%5.1f  nearest %-16s %.1f", i.role, Math.hypot(i.x, i.z), bestRole, best));
            if (best > SkyMap.GAP_MAX) r.err("shared " + i.role + " is " + String.format("%.1f", best) + " from everything");
        }
    }

    private static void overlaps(SkyMap.Build b, Report r) {
        int bad = 0;
        List<SkyMap.Inst> l = new ArrayList<>();
        for (SkyMap.Inst i : b.insts) if (!i.role.equals("tower")) l.add(i);
        for (int a = 0; a < l.size(); a++)
            for (int c = a + 1; c < l.size(); c++) {
                SkyMap.Inst x = l.get(a), y = l.get(c);
                if (!SkyMap.coarseNear(x, y, 5)) continue;
                double gp = SkyMap.gap(x, y)[0];
                boolean genPair = (x.role.equals("generator") && y.role.equals("start") || y.role.equals("generator") && x.role.equals("start")) && x.wedge == y.wedge;
                if (genPair ? gp < 0.5 : gp < 4) { bad++; if (bad < 8) r.err("islands too close: " + x.role + " / " + y.role + " gap " + String.format("%.1f", gp)); }
            }
        r.line("island spacing: " + (bad == 0 ? "no overlaps, every pair at least 4 apart (generator/start pair excepted)" : bad + " pairs too close"));
    }

    /** Every copy of a template must contain exactly the same blocks (turned back by its quarter turn). */
    private static void identity(SkyMap.Build b, Report r) {
        Map<String, SkyMap.Inst> first = new HashMap<>();
        Map<String, Integer> copies = new HashMap<>();
        int mismatches = 0;
        MapModel.Blocks blk = b.model.blocks;
        for (SkyMap.Inst i : b.insts) {
            if (i.role.equals("castle")) continue;
            SkyMap.Inst f = first.get(i.tpl.name);
            copies.merge(i.tpl.name, 1, Integer::sum);
            if (f == null) { first.put(i.tpl.name, i); continue; }
            int[] bb = bbox(i.tpl);
            for (int lx = bb[0]; lx <= bb[3]; lx++) for (int ly = bb[1]; ly <= bb[4]; ly++) for (int lz = bb[2]; lz <= bb[5]; lz++) {
                String si = Bs.norm(Bs.rot(blk.get(i.wx(lx, lz), i.y + ly, i.wz(lx, lz)), 4 - i.q));
                String sf = Bs.norm(Bs.rot(blk.get(f.wx(lx, lz), f.y + ly, f.wz(lx, lz)), 4 - f.q));
                if (si == null ? sf != null : !si.equals(sf)) {
                    mismatches++;
                    if (mismatches <= 5) r.err("copy of " + i.tpl.name + " at (" + i.x + "," + i.z + ") differs at local (" + lx + "," + ly + "," + lz + "): " + si + " vs " + sf);
                }
            }
        }
        // per-wedge layout: same roles at the same polar position (within rounding)
        int n = b.model.wedges;
        double worstR = 0, worstA = 0;
        for (SkyMap.Inst i : b.insts) {
            if (i.slot < 0) continue;
            double base = SkyMap.wedgeAngle(i.slot, n);
            double rr = Math.hypot(i.x, i.z), ang = Math.toDegrees(Math.atan2(-i.x, i.z)) - base;
            ang = ((ang % 360) + 540) % 360 - 180;
            for (SkyMap.Inst o : b.insts) {
                if (o.slot != 0 || !o.role.equals(i.role) || !o.tpl.name.equals(i.tpl.name) && !i.role.equals("showpiece")) continue;
                double ro = Math.hypot(o.x, o.z), ao = Math.toDegrees(Math.atan2(-o.x, o.z));
                ao = ((ao % 360) + 540) % 360 - 180;
                if (Math.abs(ao - ang) > 3) continue;
                worstR = Math.max(worstR, Math.abs(ro - rr));
                worstA = Math.max(worstA, Math.abs(ao - ang) * Math.PI / 180 * rr);
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : copies.entrySet()) sb.append(e.getKey()).append(" x").append(e.getValue()).append(", ");
        r.line("wedge identity: " + (mismatches == 0 ? "all copies block-identical" : mismatches + " differing cells") + " (" + sb
                + "positions equal within " + String.format("%.2f", Math.max(worstR, worstA)) + " blocks of rounding). Showpieces cycle 3 designs (scenery, no chests).");
    }

    private static int[] bbox(Tpl t) {
        int[] bb = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        for (long k : t.blocks.keySet()) {
            int x = Tpl.kx(k), y = Tpl.ky(k), z = Tpl.kz(k);
            bb[0] = Math.min(bb[0], x); bb[1] = Math.min(bb[1], y); bb[2] = Math.min(bb[2], z);
            bb[3] = Math.max(bb[3], x); bb[4] = Math.max(bb[4], y); bb[5] = Math.max(bb[5], z);
        }
        return bb;
    }

    // ------------------------------------------------------------------ walking

    static boolean stand(MapModel.Blocks b, int x, int y, int z) {
        if (!Bs.passable(b.get(x, y, z)) || !Bs.passable(b.get(x, y + 1, z))) return false;
        if (Bs.isLadder(b.get(x, y, z))) return true;
        String below = b.get(x, y - 1, z);
        return below != null && Bs.standTop(below) > 0;
    }

    /** Walkable nodes (feet positions) reachable from the given starts, inside a box. */
    static Set<Long> walk(MapModel.Blocks b, List<int[]> starts, int[] box) {
        Set<Long> seen = new HashSet<>();
        ArrayDeque<int[]> q = new ArrayDeque<>();
        for (int[] s : starts) if (seen.add(Tpl.key(s[0], s[1], s[2]))) q.add(s);
        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] c = q.poll();
            int x = c[0], y = c[1], z = c[2];
            List<int[]> next = new ArrayList<>();
            for (int[] d : d4) {
                int nx = x + d[0], nz = z + d[1];
                if (nx < box[0] || nx > box[3] || nz < box[2] || nz > box[5]) continue;
                if (stand(b, nx, y + 1, nz) && Bs.passable(b.get(x, y + 2, z))) next.add(new int[]{nx, y + 1, nz});
                if (stand(b, nx, y, nz)) { next.add(new int[]{nx, y, nz}); continue; }
                if (!Bs.passable(b.get(nx, y, nz)) || !Bs.passable(b.get(nx, y + 1, nz))) continue;
                for (int dy = 1; dy <= 3; dy++) {
                    if (!Bs.passable(b.get(nx, y - dy, nz))) break;
                    if (stand(b, nx, y - dy, nz)) { next.add(new int[]{nx, y - dy, nz}); break; }
                }
            }
            if (Bs.isLadder(b.get(x, y, z)) && Bs.passable(b.get(x, y + 1, z)) && Bs.passable(b.get(x, y + 2, z))) next.add(new int[]{x, y + 1, z});
            if (Bs.isLadder(b.get(x, y - 1, z))) next.add(new int[]{x, y - 1, z});
            for (int[] nn : next) {
                if (nn[1] < box[1] || nn[1] > box[4]) continue;
                if (seen.add(Tpl.key(nn[0], nn[1], nn[2]))) q.add(nn);
            }
        }
        return seen;
    }

    private static int[] worldBox(SkyMap.Inst i) {
        int[] bb = bbox(i.tpl);
        int[] o = {Integer.MAX_VALUE, i.y + bb[1] - 2, Integer.MAX_VALUE, Integer.MIN_VALUE, i.y + bb[4] + 3, Integer.MIN_VALUE};
        for (long k : i.footprint()) {
            o[0] = Math.min(o[0], Tpl.kx(k) - 2); o[2] = Math.min(o[2], Tpl.kz(k) - 2);
            o[3] = Math.max(o[3], Tpl.kx(k) + 2); o[5] = Math.max(o[5], Tpl.kz(k) + 2);
        }
        return o;
    }

    /** Landing nodes: standable feet positions on the footprint's outer boundary (where a bridge arrives). */
    static List<int[]> landings(MapModel.Blocks b, SkyMap.Inst i, int lo, int hi) {
        List<int[]> out = new ArrayList<>();
        Set<Long> f = i.footprint();
        for (long k : f) {
            int x = Tpl.kx(k), z = Tpl.kz(k);
            boolean edge = !f.contains(Tpl.key(x + 1, 0, z)) || !f.contains(Tpl.key(x - 1, 0, z))
                    || !f.contains(Tpl.key(x, 0, z + 1)) || !f.contains(Tpl.key(x, 0, z - 1));
            if (!edge) continue;
            for (int y = hi; y >= lo; y--) if (stand(b, x, y, z)) { out.add(new int[]{x, y, z}); break; }
        }
        return out;
    }

    private static boolean chestOpen(MapModel.Blocks b, int x, int y, int z, Set<Long> reach) {
        int[][] d4 = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : d4) for (int dy = -1; dy <= 1; dy++) if (reach.contains(Tpl.key(x + d[0], y + dy, z + d[1]))) return true;
        return reach.contains(Tpl.key(x, y + 1, z));
    }

    private static void chests(SkyMap.Build b, Report r) {
        MapModel m = b.model;
        MapModel.Blocks blk = m.blocks;
        int bad = 0, viaTower = 0;
        Map<String, Integer> byType = new HashMap<>();
        // island graph: every chest island reachable from every start through gaps <= 30
        Map<Integer, Set<SkyMap.Inst>> reachByWedge = new HashMap<>();
        for (SkyMap.Inst s : b.insts) if (s.role.equals("start")) reachByWedge.put(s.wedge, SkyMap.reachable(b.insts, s));
        SkyMap.Inst castle = find(b, "castle", -1);
        for (SkyMap.Inst i : b.insts) {
            if (i.tpl.chests.isEmpty()) continue;
            SkyMap.Inst home = i.role.equals("tower") ? castle : i;
            for (Map.Entry<Integer, Set<SkyMap.Inst>> e : reachByWedge.entrySet())
                if (!e.getValue().contains(home)) { bad++; if (bad < 6) r.err(i.role + " at (" + i.x + "," + i.z + ") not reachable from start of wedge " + e.getKey() + " within " + SkyMap.GAP_MAX); }
            Set<Long> reach;
            if (i.role.equals("tower")) reach = null;
            else reach = walk(blk, landings(blk, i, i.y - 2, i.y + 2), worldBox(i));
            for (int c = 0; c < i.tpl.chests.size(); c++) {
                int[] p = i.tpl.chests.get(c);
                int x = i.wx(p[0], p[2]), y = i.y + p[1], z = i.wz(p[0], p[2]);
                byType.merge(i.tpl.chestTypes.get(c).name(), 1, Integer::sum);
                String s = blk.get(x, y, z);
                if (s == null || !Bs.id(s).equals("chest")) { r.err("no chest block at " + x + "," + y + "," + z); continue; }
                if (Bs.fullSolid(blk.get(x, y + 1, z))) r.err("chest lid blocked at " + x + "," + y + "," + z);
                if (reach == null) continue;    // castle chests: checked by castleWalk
                if (chestOpen(blk, x, y, z, reach)) continue;
                if (i.tpl.pillar) {
                    Set<Long> top = new HashSet<>();
                    for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) if (stand(blk, x + dx, y, z + dz)) top.add(Tpl.key(x + dx, y, z + dz));
                    if (chestOpen(blk, x, y, z, top)) { viaTower++; continue; }
                }
                r.err(i.role + " chest at " + x + "," + y + "," + z + " cannot be walked to");
            }
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : byType.entrySet()) sb.append(e.getKey()).append('=').append(e.getValue()).append(' ');
        r.line("chests: " + m.chests.size() + " registered (" + sb.toString().trim() + "); every chest island reachable from every start "
                + "through gaps <= " + SkyMap.GAP_MAX + (bad == 0 ? "" : " EXCEPT " + bad) + "; walk-checked from the island edges ("
                + viaTower + " pillar-top chests reached by towering up, standing room next to them verified)");
    }

    private static void climbs(SkyMap.Build b, Report r) {
        MapModel.Blocks blk = b.model.blocks;
        int bad = 0;
        for (int[] c : b.climbs) {
            for (int y = c[1]; y < c[3]; y++) {
                String s = blk.get(c[0], y, c[2]);
                if (!Bs.isLadder(s)) { bad++; r.err("ladder missing at " + c[0] + "," + y + "," + c[2]); break; }
                String f = Bs.prop(s, "facing");
                int dx = "east".equals(f) ? -1 : "west".equals(f) ? 1 : 0, dz = "south".equals(f) ? -1 : "north".equals(f) ? 1 : 0;
                if (!Bs.fullSolid(blk.get(c[0] + dx, y, c[2] + dz))) { bad++; r.err("ladder at " + c[0] + "," + y + "," + c[2] + " has nothing to hang on"); break; }
            }
        }
        r.line("ladders: " + b.climbs.size() + " climbs checked" + (bad == 0 ? ", all attached and continuous" : ""));
    }

    /** Castle: from the outer apron/balconies reach the arena floor, every tower chest, and get back up again. */
    private static void castleWalk(SkyMap.Build b, Report r) {
        MapModel.Blocks blk = b.model.blocks;
        SkyMap.Inst castle = find(b, "castle", -1);
        int[] box = {-30, -2, -30, 30, 30, 30};
        Set<Long> in = walk(blk, landings(blk, castle, Castle.RIM_Y + 1, Castle.RIM_Y + 1), box);
        boolean floor = in.contains(Tpl.key(0, 1, 0));
        int towerChests = 0, ok = 0;
        for (SkyMap.Inst i : b.insts) {
            if (!i.role.equals("tower")) continue;
            for (int[] p : i.tpl.chests) {
                towerChests++;
                if (chestOpen(blk, i.wx(p[0], p[2]), i.y + p[1], i.wz(p[0], p[2]), in)) ok++;
                else r.err("castle chest at " + i.wx(p[0], p[2]) + "," + (i.y + p[1]) + "," + i.wz(p[0], p[2]) + " not reachable on foot");
            }
        }
        List<int[]> fromFloor = new ArrayList<>();
        fromFloor.add(new int[]{0, 1, 0});
        Set<Long> out = walk(blk, fromFloor, box);
        boolean back = out.contains(Tpl.key(0, Castle.RIM_Y + 1, 19)) || out.contains(Tpl.key(19, Castle.RIM_Y + 1, 0));
        boolean wallTop = out.contains(Tpl.key(15, Castle.WALL_TOP + 1, 15));
        if (!floor) r.err("castle arena floor not reachable from the apron");
        if (!back) r.err("castle: no way back up from the arena floor");
        if (!wallTop) r.err("castle: wall top not reachable by the inner ladders");
        r.line("castle walk: apron -> arena floor " + (floor ? "OK" : "NO") + ", tower chests reachable " + ok + "/" + towerChests
                + ", floor -> rim " + (back ? "OK" : "NO") + ", rim -> wall top by ladder " + (wallTop ? "OK" : "NO"));
    }
}
