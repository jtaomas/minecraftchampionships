package com.mcm.games.skyblockle.map;

import com.mcm.games.skyblockle.MapModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sky Blockle map generator (pure Java, no Bukkit). N identical team wedges (2..10, evenly spaced) around the
 * centre castle, following SPEC.md "Confirmed" and research/skyblockle_maps.md (image-search pass).
 *
 * <p>Each team's spoke (all on the wedge axis, so it works for any N): generator shrine (directly behind the
 * start) - start - red pillar - fort hut - sand spike - enchanting deck - castle. The shared spoke halfway to the
 * neighbour carries: brewing deck - showpiece - red pillar - grass island - red spike - rim asteroid. Behind each
 * generator sits a scenery boulder. Islands sit on one plane with small height offsets; the pillars and the
 * castle give the vertical drama. If a shared island would be more than 30 blocks from everything a team can
 * already reach (small N), small stepping rocks are added symmetrically.
 *
 * <p>Every island is a template placed by translation + quarter turn, so all copies are block-identical.
 */
public final class SkyMap {
    private SkyMap() {}

    // ring radii (horizontal distance of island centres from the castle centre)
    static final int R_CASTLE = 25, R_ENCH = 46, R_AXSPIKE = 69, R_FORT = 93, R_PILLAR = 118, R_START = 139,
            R_GEN = 152, R_BOULDER = 176;
    static final int R_BREW = 70, R_SHOW = 96, R_BPILLAR = 119, R_GRASS = 141, R_BSPIKE = 163, R_ASTEROID = 190;
    static final int GAP_MIN = 13, GAP_MAX = 30;

    /** A placed template. */
    static final class Inst {
        final String role;
        final Tpl tpl;
        final int x, y, z, q;
        final int wedge;      // owning wedge, -1 = shared
        final int slot;       // wedge index of the axis, or of the between-slot (slot i lies between wedge i and i+1)
        final boolean axis;
        int landY = Integer.MIN_VALUE;   // castle: feet height of the landing ring
        Set<Long> foot;                   // footprint columns (key(x, 0, z))

        Inst(String role, Tpl tpl, int x, int y, int z, int q, int wedge, int slot, boolean axis) {
            this.role = role; this.tpl = tpl; this.x = x; this.y = y; this.z = z; this.q = q;
            this.wedge = wedge; this.slot = slot; this.axis = axis;
        }

        int wx(int lx, int lz) { return x + Bs.rx(lx, lz, q); }
        int wz(int lx, int lz) { return z + Bs.rz(lx, lz, q); }

        Set<Long> footprint() {
            if (foot == null) {
                foot = new HashSet<>();
                for (long k : tpl.blocks.keySet()) foot.add(Tpl.key(wx(Tpl.kx(k), Tpl.kz(k)), 0, wz(Tpl.kx(k), Tpl.kz(k))));
            }
            return foot;
        }

        void place(MapModel.Blocks out) {
            for (Map.Entry<Long, String> e : tpl.blocks.entrySet()) {
                long k = e.getKey();
                int lx = Tpl.kx(k), ly = Tpl.ky(k), lz = Tpl.kz(k);
                out.set(wx(lx, lz), y + ly, wz(lx, lz), Bs.rot(e.getValue(), q));
            }
        }
    }

    /** Everything the validators need besides the model. */
    public static final class Build {
        public final MapModel model;
        final List<Inst> insts = new ArrayList<>();
        final List<Stairs.Lane> lanes = new ArrayList<>();
        final List<int[]> climbs = new ArrayList<>();      // world x, yFrom, z, yTo
        PostPass.Result post;
        int islets;

        Build(MapModel m) { this.model = m; }
    }

    /** Entry point used by the game. */
    public static MapModel generate(int wedges, long seed) {
        return build(wedges, seed).model;
    }

    static double[] dir(double deg) {
        double a = Math.toRadians(deg);
        return new double[]{-Math.sin(a), Math.cos(a)};
    }

    static double wedgeAngle(int w, int n) { return w * 360.0 / n; }

    private static int quarterFacing(double deg) {
        double[] u = dir(deg);
        double ix = -u[0], iz = -u[1];          // towards the centre
        int best = 0;
        double bd = -9;
        for (int q = 0; q < 4; q++) {
            double d = Bs.rx(0, -1, q) * ix + Bs.rz(0, -1, q) * iz;
            if (d > bd + 1e-9) { bd = d; best = q; }
        }
        return best;
    }

    private static Inst at(String role, Tpl t, double r, double deg, int y, int wedge, int slot, boolean axis) {
        double[] u = dir(deg);
        int x = (int) Math.round(r * u[0]), z = (int) Math.round(r * u[1]);
        return new Inst(role, t, x, y, z, quarterFacing(deg), wedge, slot, axis);
    }

    public static Build build(int wedges, long seed) {
        if (wedges < 2 || wedges > 10) throw new IllegalArgumentException("wedges must be 2..10, got " + wedges);
        int n = wedges;
        MapModel m = new MapModel(n, seed);
        Build b = new Build(m);
        long s = seed;

        Tpl start = Islands.start(s), gen = Islands.generator(s), pillar = Islands.pillar(s, 18),
                pillarB = Islands.pillar(s + 1, 15), fort = Islands.fort(s), spikeS = Islands.spike(s, false),
                spikeR = Islands.spike(s, true), ench = Islands.enchanting(s), brew = Islands.brewing(s),
                grass = Islands.grass(s), boulder = Islands.boulder(s), asteroid = Islands.asteroid(s),
                islet = Islands.islet(s);
        Tpl[] shows = {Islands.hourglass(s), Islands.pickaxe(s), Islands.planter(s)};
        pillarB = renamed(pillarB, "pillar_shared");

        // castle (one piece, placed first) + its 4 towers
        Castle.build(m.blocks, s);
        Tpl tower = Castle.tower(s);
        for (int q = 0; q < 4; q++) {
            Inst t = new Inst("tower", tower, 0, 0, 0, q, -1, -1, false);
            t.place(m.blocks);
            b.insts.add(t);
        }
        Tpl castleFoot = new Tpl("castle");
        for (int x = -28; x <= 28; x++) for (int z = -28; z <= 28; z++)
            if (x * x + z * z <= Castle.APRON_END * Castle.APRON_END || Castle.inTowerFoot(x, z)) castleFoot.set(x, 0, z, "stone");
        Inst castle = new Inst("castle", castleFoot, 0, 0, 0, 0, -1, -1, false);
        castle.landY = Castle.RIM_Y + 1;
        b.insts.add(castle);

        for (int w = 0; w < n; w++) {
            double a = wedgeAngle(w, n), h = a + 180.0 / n;
            int sb = w;                                   // between slot w lies between wedge w and w+1
            b.insts.add(at("generator", gen, R_GEN, a, -1, w, w, true));
            b.insts.add(at("start", start, R_START, a, 0, w, w, true));
            b.insts.add(at("pillar", pillar, R_PILLAR, a, -2, w, w, true));
            b.insts.add(at("fort", fort, R_FORT, a, 3, w, w, true));
            b.insts.add(at("spike", spikeS, R_AXSPIKE, a, 1, w, w, true));
            b.insts.add(at("enchanting", ench, R_ENCH, a, 4, w, w, true));
            b.insts.add(at("boulder", boulder, R_BOULDER, a, -5, w, w, true));
            b.insts.add(at("brewing", brew, R_BREW, h, 2, -1, sb, false));
            b.insts.add(at("showpiece", shows[sb % shows.length], R_SHOW, h, 1, -1, sb, false));
            b.insts.add(at("pillar_shared", pillarB, R_BPILLAR, h, -1, -1, sb, false));
            b.insts.add(at("grass", grass, R_GRASS, h, 2, -1, sb, false));
            b.insts.add(at("spike_shared", spikeR, R_BSPIKE, h, -3, -1, sb, false));
            b.insts.add(at("asteroid", asteroid, R_ASTEROID, h, -6, -1, sb, false));
        }
        addStepping(b, n, islet);

        for (Inst i : b.insts) if (!i.role.equals("tower") && !i.role.equals("castle")) i.place(m.blocks);
        b.post = PostPass.run(m.blocks);

        // chests, spawns, lanes, climbs
        for (Inst i : b.insts) {
            if (i.role.equals("castle")) continue;
            for (int c = 0; c < i.tpl.chests.size(); c++) {
                int[] p = i.tpl.chests.get(c);
                m.chests.add(new MapModel.ChestSpot(i.wx(p[0], p[2]), i.y + p[1], i.wz(p[0], p[2]), i.tpl.chestTypes.get(c), i.wedge));
            }
            if (i.role.equals("start")) {
                double a = wedgeAngle(i.wedge, n);
                float yaw = (float) (((a + 180.0) % 360.0 + 540.0) % 360.0 - 180.0);
                for (int[] p : i.tpl.spawns)
                    m.spawns.add(new MapModel.Spawn(i.wedge, i.wx(p[0], p[2]) + 0.5, i.y + p[1], i.wz(p[0], p[2]) + 0.5, yaw));
            }
            for (int[] l : i.tpl.lanes) {
                Stairs.Lane lane = new Stairs.Lane(i.role + "#" + i.q, l[0], l[1], l[2], l[3], l[4], l[5]).rotated(i.q);
                b.lanes.add(new Stairs.Lane(lane.name(), i.x + lane.x(), i.z + lane.z(), i.y + lane.floorY(), lane.dx(), lane.dz(), lane.n()));
            }
            for (int[] c : i.tpl.climbs) b.climbs.add(new int[]{i.wx(c[0], c[2]), i.y + c[1], i.wz(c[0], c[2]), i.y + c[3]});
        }
        // castle wall ladders (built directly by Castle)
        for (int q = 0; q < 4; q++) b.climbs.add(new int[]{Bs.rx(14, 15, q), Castle.RIM_Y + 1, Bs.rz(14, 15, q), Castle.WALL_TOP + 1});

        m.castleX = 0; m.castleY = 0; m.castleZ = 0;
        m.arenaRadius = 12;
        m.castleRadius = 27;
        m.rings = new MapModel.Rings(R_CASTLE, R_ENCH, R_BREW, R_FORT, R_PILLAR, R_START, R_GEN, R_ASTEROID);
        int[] ext = {0, Integer.MAX_VALUE, Integer.MIN_VALUE};
        m.blocks.forEach((x, y, z, st) -> {
            int d = (int) Math.ceil(Math.sqrt((double) x * x + (double) z * z));
            if (d > ext[0]) ext[0] = d;
            if (y < ext[1]) ext[1] = y;
            if (y > ext[2]) ext[2] = y;
        });
        m.mapRadius = ext[0] + 1;
        m.minY = ext[1];
        m.maxY = ext[2];
        m.notes.put("blocks", String.valueOf(m.blocks.size()));
        m.notes.put("islands", String.valueOf(b.insts.size() - 4));
        m.notes.put("stepping-islets", String.valueOf(b.islets));
        m.notes.put("chests", String.valueOf(m.chests.size()));
        m.notes.put("team-markers", "none (research: no team colours on the map)");
        return b;
    }

    private static Tpl renamed(Tpl t, String name) {
        Tpl r = new Tpl(name);
        r.blocks.putAll(t.blocks);
        r.chests.addAll(t.chests);
        r.chestTypes.addAll(t.chestTypes);
        r.pillar = t.pillar;
        return r;
    }

    // ------------------------------------------------------------------ gaps + stepping rocks

    /** Smallest horizontal gap (blocks to bridge = centre distance - 1) between two footprints, with the closest cells. */
    static double[] gap(Inst a, Inst b) {
        double best = Double.MAX_VALUE;
        long ba = 0, bb = 0;
        // prune: only cells near the other island's centre side
        for (long ka : a.footprint()) {
            int ax = Tpl.kx(ka), az = Tpl.kz(ka);
            for (long kb : b.footprint()) {
                double dx = ax - Tpl.kx(kb), dz = az - Tpl.kz(kb);
                double d = dx * dx + dz * dz;
                if (d < best) { best = d; ba = ka; bb = kb; }
            }
        }
        return new double[]{Math.sqrt(best) - 1, Tpl.kx(ba), Tpl.kz(ba), Tpl.kx(bb), Tpl.kz(bb)};
    }

    static boolean coarseNear(Inst a, Inst b, double lim) {
        double dx = a.x - b.x, dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz) < lim + 60;
    }

    /** Islands reachable from wedge 0's start using gaps <= GAP_MAX. */
    static Set<Inst> reachable(List<Inst> all, Inst from) { return reachable(all, from, GAP_MAX); }

    static Set<Inst> reachable(List<Inst> all, Inst from, double limit) {
        Set<Inst> seen = new HashSet<>();
        List<Inst> todo = new ArrayList<>();
        seen.add(from);
        todo.add(from);
        Map<Long, Double> cache = new HashMap<>();
        while (!todo.isEmpty()) {
            Inst c = todo.remove(todo.size() - 1);
            for (Inst o : all) {
                if (seen.contains(o) || o.role.equals("tower")) continue;
                if (!coarseNear(c, o, limit)) continue;
                if (gap(c, o)[0] <= limit) { seen.add(o); todo.add(o); }
            }
        }
        return seen;
    }

    private static void addStepping(Build b, int n, Tpl islet) {
        for (int iter = 0; iter < 12; iter++) {
            Inst start0 = null;
            for (Inst i : b.insts) if (i.role.equals("start") && i.wedge == 0) start0 = i;
            Set<Inst> reach = reachable(b.insts, start0, GAP_MAX - 3);   // margin: other copies round differently
            Inst target = null, from = null;
            double[] link = null;
            for (Inst o : b.insts) {
                if (o.axis || o.slot != 0 || reach.contains(o) || o.role.equals("tower")) continue;
                for (Inst r : reach) {
                    if (r.role.equals("castle") || r.role.equals("tower")) continue;
                    double[] g = gap(r, o);
                    if (link == null || g[0] < link[0]) { link = g; target = o; from = r; }
                }
            }
            if (target == null) return;
            double ax = link[1], az = link[2], bx = link[3], bz = link[4];
            double dist = Math.hypot(bx - ax, bz - az);
            int k = 1;
            while ((dist - 1 - 3.0 * k) / (k + 1) > GAP_MAX - 4) k++;
            for (int j = 1; j <= k; j++) {
                double t = j / (double) (k + 1);
                double px = ax + (bx - ax) * t, pz = az + (bz - az) * t;
                int py = (int) Math.round(from.y + (target.y - from.y) * t);
                double r = Math.hypot(px, pz);
                double phi = Math.toDegrees(Math.atan2(-px, pz));   // angle from +Z towards -X (wedge 0 axis = 0)
                for (int w = 0; w < n; w++) {
                    double a = wedgeAngle(w, n);
                    b.insts.add(at("islet", islet, r, a + phi, py, -1, w, false));
                    b.insts.add(at("islet", islet, r, a + 360.0 / n - phi, py, -1, w, false));
                    b.islets += 2;
                }
            }
        }
    }
}
