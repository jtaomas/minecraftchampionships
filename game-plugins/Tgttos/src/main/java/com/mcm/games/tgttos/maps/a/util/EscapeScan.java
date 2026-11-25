package com.mcm.games.tgttos.maps.a.util;

import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * Feedback 3 (Dam Face playtest: "the right side of the map is a killbox and a hole into the void", "there are still
 * gaps in the map here"): the old hole scan only looked beside cells a racer can WALK to inside the design search box,
 * on the validator's seed. This scan floods everything a racer can actually get to and fails on any way out:
 * <ul>
 *   <li>walk / jump / fall anywhere (search box = play box + 10, no design box);</li>
 *   <li>bridging kits: every air cell of the build box connected to a reachable cell (pillar / bridge there and stand
 *       on the block), plus the cells right above the build box;</li>
 *   <li>snowball / punch knockback: from every standing cell, 16 directions, pushed up to 6 blocks with a 1.15-block
 *       arc (1.5-block fences and walls stop it), may stop anywhere on the way and drops straight down;</li>
 *   <li>landings are fed back in (4 rounds).</li>
 * </ul>
 * Fails on any reachable column empty down to the void (outside {@link Design#voidByDesign}), any landing pocket
 * from which the racer cannot walk back to the course (no build box to pillar out of), and any reachable hazard
 * block (lava, fire, magma, powder snow, cactus, berry bush).
 */
final class EscapeScan {
    private EscapeScan() {}

    static final double PUSH = 6.0, LIFT = 1.15;

    static void run(Validate v, Gen g, List<int[]> strip, List<Pos> gone, boolean bridging) {
        TgMapData d = g.d;
        Design de = g.design;
        Region pb = d.playBounds;
        Region box = Region.of(pb.minX() - 10, d.voidY, pb.minZ() - 10, pb.maxX() + 10, pb.maxY() + 6, pb.maxZ() + 10);
        Set<Long> removed = new HashSet<>();
        for (Pos q : gone) removed.add(TgMapData.SparseBlocks.key(q.x(), q.y(), q.z()));
        Blk w = (x, y, z) -> removed.contains(TgMapData.SparseBlocks.key(x, y, z)) ? null : d.blocks.get(x, y, z);

        Route.Params wp = Route.Params.expert(false);
        wp.searchBox = box;
        wp.falls = true;
        wp.fallRadius = 24;
        wp.strictFalls = true;   // falls / dropping jumps must clear their whole path (no falling through rock)
        Route r = new Route(d.blocks, gone, wp);
        Set<Long> course = r.reachable(strip, 4_000_000);
        Set<Long> stand = new HashSet<>(course);

        // bridging: the connected air of the build box
        Set<Long> built = new HashSet<>();
        Region bb = d.buildBounds;
        if (bridging && bb != null) {
            // feet cells a racer can hold by bridging / pillaring: 2 air cells for the body, a block under the feet that
            // is either there or may be placed (inside the build box); moves between such cells (6 directions)
            ArrayDeque<int[]> dq = new ArrayDeque<>();
            for (long k : course) {
                int x = kx(k), y = ky(k), z = kz(k);
                if (holdable(w, bb, x, y, z) && built.add(k)) dq.add(new int[]{x, y, z});
            }
            int[][] n6 = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
            while (!dq.isEmpty()) {
                int[] c = dq.poll();
                for (int[] o : n6) {
                    int x = c[0] + o[0], y = c[1] + o[1], z = c[2] + o[2];
                    if (!holdable(w, bb, x, y, z)) continue;
                    if (built.add(key(x, y, z))) dq.add(new int[]{x, y, z});
                }
            }
            stand.addAll(built);
        }
        // leaving the play box = respawn: walking / jumping out of it is a way out too
        TreeMap<String, String> outs = new TreeMap<>();
        for (long k : stand) {
            int x = kx(k), y = ky(k), z = kz(k);
            if (!pb.contains(x + 0.5, y + 0.1, z + 0.5)) outs.putIfAbsent(x + "," + z, String.format("out of the play box at %d,%d,%d", x, y, z));
        }
        Set<Long> home = new HashSet<>(stand);   // cells from which the course is reachable

        TreeMap<String, String> voids = new TreeMap<>();
        TreeMap<String, String> pockets = new TreeMap<>();
        TreeMap<String, String> hazards = new TreeMap<>();
        Set<Long> pocketCells = new HashSet<>();
        int byDesign = 0, pushes = 0;
        List<Long> todo = new ArrayList<>(stand);
        for (int round = 0; round < 4 && !todo.isEmpty(); round++) {
            Set<Long> landings = new HashSet<>();
            for (long k : todo) {
                int sx = kx(k), sy = ky(k), sz = kz(k);
                hazard(w, sx, sy, sz, hazards, "standing");
                for (int a = 0; a < 16; a++) {
                    double ux = Math.cos(a * Math.PI / 8), uz = Math.sin(a * Math.PI / 8);
                    double h = sy;
                    for (double s = 0.5; s <= PUSH + 1e-9; s += 0.5) {
                        double px = sx + 0.5 + ux * s, pz = sz + 0.5 + uz * s;
                        double arc = sy + LIFT * Math.sin(Math.PI * Math.min(1, s / PUSH));
                        // once over a drop the racer keeps falling: never higher than the previous step's arc
                        double nh = Math.min(arc, h + 0.6);
                        int bx = (int) Math.floor(px), bz = (int) Math.floor(pz);
                        if (!body(w, bx, nh, bz)) break;
                        h = nh;
                        pushes++;
                        if (!pb.contains(px, h + 0.1, pz)) { outs.putIfAbsent(bx + "," + bz, String.format("out of the play box at %d,%d,%d (pushed %.1f from %d,%d,%d)", bx, (int) Math.floor(h), bz, s, sx, sy, sz)); break; }
                        // stopped here (a weaker hit): drop straight down
                        int fy = (int) Math.floor(h);
                        int land = Integer.MIN_VALUE;
                        for (int y = fy; y >= d.voidY; y--) {
                            String b = w.get(bx, y, bz);
                            if (b != null && Bs.id(b).equals("water")) { land = y; break; }
                            if (!Bs.passable(b)) { land = y + 1; break; }
                        }
                        if (land == Integer.MIN_VALUE) {
                            boolean ok = false;
                            for (Region rg : de.voidByDesign) if (rg.contains(bx + 0.5, rg.minY(), bz + 0.5)) ok = true;
                            if (ok) byDesign++;
                            else voids.putIfAbsent(bx + "," + bz, String.format("void column %d,%d (pushed %.1f from %d,%d,%d)", bx, bz, s, sx, sy, sz));
                            break;
                        }
                        long lk = key(bx, land, bz);
                        if (!stand.contains(lk) && Bs.passable(w.get(bx, land, bz)) && Bs.passable(w.get(bx, land + 1, bz))) landings.add(lk);
                    }
                }
            }
            // new landings: can the racer walk back to the course from there?
            todo = new ArrayList<>();
            for (long lk : landings) {
                if (stand.contains(lk) || pocketCells.contains(lk)) continue;
                Set<Long> rs = r.reachable(List.of(new int[]{kx(lk), ky(lk), kz(lk)}), 20_000);
                boolean back = false;
                for (long q : rs) if (home.contains(q)) { back = true; break; }
                if (!back && rs.size() < 20_000) {
                    pocketCells.addAll(rs);
                    pocketCells.add(lk);
                    pockets.putIfAbsent(kx(lk) + "," + ky(lk) + "," + kz(lk), String.format("pocket at %d,%d,%d (%d cells, no way back)", kx(lk), ky(lk), kz(lk), rs.size()));
                }
                for (long q : rs) if (stand.add(q)) todo.add(q);
                stand.add(lk);
                if (back) home.addAll(rs);
            }
        }
        boolean ok = voids.isEmpty() && pockets.isEmpty() && hazards.isEmpty() && outs.isEmpty();
        v.ok(ok, String.format("escape scan (walk/jump/fall anywhere%s, snowball knockback up to %.0f blocks): %d standing cells, %d pushes; %d void columns reachable%s; %d exits from the play box (= respawn)%s; %d dead-end pockets%s; %d hazard cells%s; %d by design",
                bridging && bb != null ? ", bridge/pillar in the build box (" + built.size() + " holdable cells)" : "", PUSH, stand.size(), pushes,
                voids.size(), voids.isEmpty() ? "" : ": " + first(voids, 25),
                outs.size(), outs.isEmpty() ? "" : ": " + first(outs, 15),
                pockets.size(), pockets.isEmpty() ? "" : ": " + first(pockets, 15),
                hazards.size(), hazards.isEmpty() ? "" : ": " + first(hazards, 10), byDesign));
    }

    interface Blk { String get(int x, int y, int z); }

    /** A racer can hold feet cell (x, y, z) with the bridging kit: body fits, and the cell under the feet is solid or placeable. */
    private static boolean holdable(Blk w, Region bb, int x, int y, int z) {
        if (x < bb.minX() || x > bb.maxX() || z < bb.minZ() || z > bb.maxZ() || y < bb.minY() || y > bb.maxY() + 1) return false;
        if (!Bs.passable(w.get(x, y, z)) || !Bs.passable(w.get(x, y + 1, z))) return false;
        String below = w.get(x, y - 1, z);
        return !Bs.passable(below) || (Bs.isAir(below) && bb.contains(x, y - 1, z));
    }

    /** The 0.6 x 1.8 body at feet height h over column (x, z) fits (centre column only; fences / walls count 1.5 high). */
    private static boolean body(Blk w, int x, double h, int z) {
        int f = (int) Math.floor(h);
        for (int y = f; y <= (int) Math.floor(h + 1.79); y++) if (!Bs.passable(w.get(x, y, z))) return false;
        String under = w.get(x, f - 1, z);
        if (under != null && h - f < 0.5) {
            String id = Bs.id(under);
            if (id.contains("fence") || id.endsWith("_wall") || id.contains("bars") || id.contains("pane")) return false;
        }
        return true;
    }

    private static void hazard(Blk w, int x, int y, int z, TreeMap<String, String> out, String how) {
        for (int dy = -1; dy <= 1; dy++) {
            String b = w.get(x, y + dy, z);
            if (b == null) continue;
            String id = Bs.id(b);
            if (id.equals("lava") || id.equals("fire") || id.equals("soul_fire") || id.equals("magma_block") || id.equals("powder_snow")
                    || id.equals("cactus") || id.equals("sweet_berry_bush") || id.contains("campfire"))
                out.putIfAbsent(x + "," + (y + dy) + "," + z, id + " at " + x + "," + (y + dy) + "," + z + " (" + how + ")");
        }
    }

    private static String first(TreeMap<String, String> m, int n) {
        List<String> l = new ArrayList<>();
        for (String s : m.values()) { if (l.size() >= n) { l.add("... +" + (m.size() - n)); break; } l.add(s); }
        return String.join("; ", l);
    }

    static long key(int x, int y, int z) { return TgMapData.SparseBlocks.key(x, y, z); }
    static int kx(long k) { return TgMapData.SparseBlocks.kx(k); }
    static int ky(long k) { return TgMapData.SparseBlocks.ky(k); }
    static int kz(long k) { return TgMapData.SparseBlocks.kz(k); }
}
