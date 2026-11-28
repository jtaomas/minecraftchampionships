package com.mcm.lobby;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java generator for the Block Showdown hub island (world mcc_lobby): ocean, noise terrain with forested
 * hills, stone mountains, beach and lagoon; arrival plaza, banner gate, red-carpet approach, the Decision Dome
 * venue (exterior + interior), side attractions, paths, foliage and hot-air balloons.
 * Writes into a {@link Vox}; returns the {@link HubLayout} with every key location.
 */
public final class HubGenerator {
    /**
     * Generator version, stored in the world's build marker. Bump it whenever the generated blocks change so a
     * server with an older hub clears and rebuilds it on the next start.
     * v2: staircase helper (first step on the floor, last step flush), baked fence/pane/wall connections,
     *     no floating carpets, walkable stair aisles in the dome bowl.
     * v3: blended flatten zones (no 3-block cliff on the rainbow path), graded paths with stair steps, canopy
     *     trimmed over paths, the harbour rebuilt as a natural cove (HubHarbour).
     * v4: Nocturne Orrery exterior and bowl, ten team pods, 8/6-wedge dial, brass orrery reveal.
     * v5: Decision Dome interior: ten tall team cage towers, white-quartz dial on a floating shadow ring, a
     *     gold/white/yellow crown truss in place of the orrery, white concourse and warm team lighting.
     * v6: the downloaded Decision Dome recreation placed 1:1 inside our shell (pit, ten team cages, dial, crown),
     *     joined by a paved ring with two low tiers; the red-carpet corridor runs from the tunnel onto its terrace.
     * v7: Minecraft Championships branding across plaza, 10 framed team booths, flush guide runners, wayfinding signs.
     */
    public static final int VERSION = 8;
    static final int SEA = HubLayout.SEA;
    static final int OCEAN = 330;      // ocean half-extent
    static final int ISLAND_R = 172;   // nominal coast radius

    final Vox v;
    final Draw d;
    final HubLayout L = new HubLayout();
    final long seed;
    final HNoise n1, n2, n3, n4;

    // heightmap over [-OCEAN, OCEAN)
    final int N = OCEAN * 2;
    final int[] h = new int[N * N];
    final byte[] surf = new byte[N * N];     // 0 ocean, 1 sand, 2 grass, 3 stone, 4 snow, 5 paved/structure
    final boolean[] reserved = new boolean[N * N];

    /** A walkable route checked by the offline validator: cells (x, z) in walking order, bottom to top. */
    record Walk(String name, int[] xs, int[] zs, int startY, int endY) {}
    /** Every staircase / stair aisle of the hub (filled by {@link #staircase} and the dome aisles). */
    final List<Walk> walks = new ArrayList<>();
    /** Post-pass counters from {@link BlockFix#apply}. */
    int[] fixCounts = new int[6];

    // zones
    static final int PLAZA_X = 0, PLAZA_Z = 80, PLAZA_R = 40;
    static final int DOME_X = 0, DOME_Z = -62;
    static final int GROUND = 70;        // plaza paving block y (stand at 71)
    static final int APPROACH_Y = 73;    // approach paving y

    public HubGenerator(Vox v, long seed) {
        this.v = v;
        this.seed = seed;
        this.d = new Draw(v, seed);
        n1 = new HNoise(seed);
        n2 = new HNoise(seed * 7 + 3);
        n3 = new HNoise(seed * 13 + 5);
        n4 = new HNoise(seed * 17 + 11);
    }

    int idx(int x, int z) { return (x + OCEAN) * N + (z + OCEAN); }
    boolean inMap(int x, int z) { return x >= -OCEAN && x < OCEAN && z >= -OCEAN && z < OCEAN; }
    int hAt(int x, int z) { return inMap(x, z) ? h[idx(x, z)] : SEA - 8; }
    void reserve(int x, int z) { if (inMap(x, z)) reserved[idx(x, z)] = true; }
    void reserveDisc(double cx, double cz, double r) {
        for (int x = (int) (cx - r - 1); x <= cx + r + 1; x++) for (int z = (int) (cz - r - 1); z <= cz + r + 1; z++)
            if ((x + 0.5 - cx) * (x + 0.5 - cx) + (z + 0.5 - cz) * (z + 0.5 - cz) <= r * r) reserve(x, z);
    }
    void reserveBox(int x1, int z1, int x2, int z2) { for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) reserve(x, z); }
    boolean isReserved(int x, int z) { return !inMap(x, z) || reserved[idx(x, z)]; }

    /** Generate everything. */
    public HubLayout generate() {
        terrain();
        new HubVenue(this).build();
        new HubPlaza(this).build();
        new HubSights(this).build();
        vegetation();
        new HubSights(this).balloons();
        trimPathCanopy();
        fixCounts = BlockFix.apply(v);
        L.minX = -OCEAN; L.maxX = OCEAN - 1; L.minZ = -OCEAN; L.maxZ = OCEAN - 1;
        L.minY = v.minY(); L.maxY = v.maxY();
        return L;
    }

    // =====================================================================================================
    // Terrain
    // =====================================================================================================

    static double smooth(double t) { t = Math.max(0, Math.min(1, t)); return t * t * (3 - 2 * t); }

    /** Coast radius at a position (domain-warped noise, so the outline is irregular, not lobed). */
    double coast(double x, double z) {
        double wx = x + 40 * n3.fbm(x * 0.004 + 11, z * 0.004 - 7, 2);
        double wz = z + 40 * n3.fbm(x * 0.004 - 31, z * 0.004 + 3, 2);
        double r = ISLAND_R + 26 * n1.fbm(wx * 0.006 + 5, wz * 0.006 + 5, 4) + 7 * n2.noise2(wx * 0.025 + 9, wz * 0.025 + 9);
        double ang = Math.atan2(z, x);
        r += 10 * Math.max(0, Math.sin(ang));   // wide southern beach
        return r;
    }

    /** Small offshore islets (backdrop): {x, z, radius, height}. */
    static final double[][] ISLETS = {{-215, -60, 16, 18}, {200, 150, 13, 10}, {-120, 215, 11, 8}, {150, -205, 18, 26}, {235, -20, 9, 7}};

    record Peak(double x, double z, double height, double radius) {}

    final List<Peak> peaks = List.of(
            new Peak(-112, -112, 74, 62),
            new Peak(18, -168, 86, 64),
            new Peak(128, -98, 64, 56),
            new Peak(-150, -40, 40, 40),
            new Peak(74, -150, 54, 44));

    /** Flatten zones: returns {weight, targetHeight}. */
    double[] flatten(int x, int z) {
        // Soft zones (plaza, venue, attraction pads) are blended by weight^4: where two zones with different target
        // heights overlap (plaza 69 / rainbow pad 66) they meet in a gentle slope instead of the 3-block cliff a
        // "strongest zone wins" rule leaves where the winner switches. Zones with equal targets are unaffected.
        double w = 0, sw = 0, st = 0;
        // plaza circle
        double dp = Math.hypot(x - PLAZA_X, z - PLAZA_Z);
        double wp = 1 - smooth((dp - 46) / 26.0);
        // venue
        double dv = Math.hypot(x - DOME_X, z - DOME_Z);
        double wv = 1 - smooth((dv - 60) / 26.0);
        double[][] soft = new double[2 + HubSights.PADS.length][];
        soft[0] = new double[]{wp, GROUND - 1};
        soft[1] = new double[]{wv, GROUND - 1};
        for (int i = 0; i < HubSights.PADS.length; i++) {
            double[] p = HubSights.PADS[i];
            double dd = Math.hypot(x - p[0], z - p[1]);
            soft[2 + i] = new double[]{1 - smooth((dd - p[2]) / 16.0), p[3]};
        }
        for (double[] s : soft) {
            if (s[0] <= 0) continue;
            w = Math.max(w, s[0]);
            double k = s[0] * s[0] * s[0] * s[0];
            sw += k;
            st += k * s[1];
        }
        double target = sw > 0 ? st / sw : GROUND - 1;
        // approach + raised terraces either side
        if (z > -30 && z < 44) {
            double ax = Math.abs(x);
            double wz = 1 - smooth((Math.max(-22 - z, z - 38)) / 10.0);
            if (ax <= 31) {
                double wa = wz;
                if (wa >= w) { w = wa; target = APPROACH_Y - 1; }
            } else if (ax <= 58) {
                double wt = Math.min(wz, 1 - smooth((ax - 50) / 14.0));
                if (wt >= w * 0.999) { w = Math.max(w, wt); target = 77; }
            }
        }
        return new double[]{w, target};
    }

    double rawHeight(int x, int z) {
        double r = Math.hypot(x, z);
        double ang = Math.atan2(z, x);
        double cr = coast(x, z);
        double t = cr - r; // inland distance
        // lagoon (south-west), connected to the sea
        double lx = -100, lz = 118;
        double dl = Math.hypot(x - lx, (z - lz) * 1.25) + 6 * n3.noise2(x * 0.05, z * 0.05);
        double channel = distToSeg(x, z, -100, 118, -150, 165);
        double ocean = SEA - 3 - Math.min(8, Math.max(0, -t) / 8.0) + 1.5 * n2.noise2(x * 0.03, z * 0.03);
        for (double[] is : ISLETS) {
            double di = Math.hypot(x - is[0], z - is[1]) / (is[2] * (1 + 0.25 * n4.noise2(x * 0.08, z * 0.08)));
            if (di < 1.3) ocean = Math.max(ocean, SEA - 2 + (is[3] + 2) * Math.pow(Math.max(0, 1 - di * di / 1.69), 1.2) * (0.8 + 0.4 * n4.ridged(x * 0.05, z * 0.05, 3)));
        }
        if (t < -2) return ocean;
        double beachW = 12 + 14 * Math.max(0, Math.sin(ang));
        double base;
        if (t < beachW) base = SEA - 1.5 + 3.5 * smooth((t + 2) / (beachW + 2));
        else base = SEA + 2 + 6 * smooth((t - beachW) / 35.0);
        // hills
        double forest = 0.5 + 0.5 * n1.noise2(x * 0.006 + 40, z * 0.006 - 20);
        double hills = (n2.fbm(x * 0.012, z * 0.012, 4) * 0.5 + 0.5);
        base += smooth((t - beachW) / 40.0) * hills * (8 + 22 * forest);
        // mountains
        double m = 0;
        for (Peak p : peaks) {
            double dd = Math.hypot(x - p.x, z - p.z) / p.radius;
            if (dd >= 1.25) continue;
            double shape = Math.pow(Math.max(0, 1 - dd * dd / 1.5625), 1.6);
            double rid = n4.ridged(x * 0.022 + p.x, z * 0.022 + p.z, 4);
            double mh = p.height * shape * (0.62 + 0.55 * rid) * (0.25 + 0.75 * smooth((t + 4) / 34.0));
            m = Math.max(m, mh);
        }
        base += m;
        // lagoon carve
        double lagoonDepth = 0;
        if (dl < 40) lagoonDepth = Math.max(lagoonDepth, (SEA - 4) + 6 * smooth((dl - 26) / 14.0));
        if (channel < 12) lagoonDepth = lagoonDepth == 0 ? (SEA - 3) + 6 * smooth((channel - 6) / 6.0) : Math.min(lagoonDepth, (SEA - 3) + 6 * smooth((channel - 6) / 6.0));
        if (lagoonDepth != 0) base = Math.min(base, lagoonDepth);
        // coastal cliffs on the north side
        if (Math.sin(ang) < -0.3 && t > 0 && t < 16) base = Math.max(base, SEA + 2 + (t / 16.0) * 7 * (0.5 + 0.5 * n3.noise2(x * 0.08, z * 0.08)));
        return base;
    }

    static double distToSeg(double px, double pz, double ax, double az, double bx, double bz) {
        double vx = bx - ax, vz = bz - az;
        double t = ((px - ax) * vx + (pz - az) * vz) / (vx * vx + vz * vz);
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + vx * t), pz - (az + vz * t));
    }

    void terrain() {
        // 1. heights
        for (int x = -OCEAN; x < OCEAN; x++) for (int z = -OCEAN; z < OCEAN; z++) {
            double hh = rawHeight(x, z);
            double[] f = flatten(x, z);
            if (f[0] > 0 && hh > SEA - 1) hh = hh + (f[1] - hh) * f[0];
            h[idx(x, z)] = (int) Math.round(hh);
        }
        // 2. columns
        for (int x = -OCEAN; x < OCEAN; x++) for (int z = -OCEAN; z < OCEAN; z++) column(x, z);
    }

    /**
     * Re-shape one terrain column to a new top height (used by attractions that sculpt the land, e.g. the harbour
     * cove): clears what stood above the lower of the two tops and redraws the column (land or sea floor + water).
     */
    void reshape(int x, int z, int newTop) {
        if (!inMap(x, z)) return;
        int old = h[idx(x, z)];
        for (int y = Math.min(old, newTop) + 1; y <= Math.max(old, SEA) + 3; y++) d.air(x, y, z);
        h[idx(x, z)] = newTop;
        column(x, z);
    }

    /** Draw the terrain column at (x, z) from the heightmap. */
    void column(int x, int z) {
        {
            int top = h[idx(x, z)];
            int slope = 0;
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) slope = Math.max(slope, Math.abs(top - hAt(x + o[0], z + o[1])));
            if (top <= SEA - 1) {
                // sea floor / lagoon floor
                boolean edge = Math.abs(x) >= OCEAN - 1 || Math.abs(z) >= OCEAN - 1 || x == -OCEAN || z == -OCEAN;
                d.set(x, top - 1, z, "sandstone");
                double nn = n3.noise2(x * 0.07, z * 0.07);
                String fl = nn > 0.55 ? "gravel" : nn < -0.6 ? "clay" : "sand";
                d.set(x, top, z, fl);
                for (int y = top + 1; y <= SEA; y++) d.set(x, y, z, edge ? "barrier" : "water");
                if (!edge && top < SEA - 2 && d.rnd(x, 1, z, 5) < 0.06 && Math.hypot(x, z) < ISLAND_R + 60)
                    d.set(x, top + 1, z, "seagrass");
                if (!edge && top < SEA - 3 && d.rnd(x, 2, z, 6) < 0.012 && Math.hypot(x, z) < ISLAND_R + 40) {
                    String[] coral = {"brain_coral_block", "tube_coral_block", "fire_coral_block", "horn_coral_block", "bubble_coral_block"};
                    d.set(x, top + 1, z, coral[(int) (d.rnd(x, 3, z, 7) * coral.length)]);
                }
                surf[idx(x, z)] = 0;
                return;
            }
            int bottom = SEA - 10;
            double r = Math.hypot(x, z);
            boolean beach = top <= SEA + 2 && r > 80;
            byte s;
            if (beach) s = 1;
            else if (top > 122 && slope < 4) s = 4;
            else if (slope >= 3 || top > 128) s = 3;
            else s = 2;
            surf[idx(x, z)] = s;
            for (int y = bottom; y <= top; y++) {
                int depth = top - y;
                String b;
                if (s == 1) b = depth < 3 ? "sand" : "sandstone";
                else if (s == 2) b = depth == 0 ? grassTop(x, z, top) : depth < 4 ? "dirt" : rock(x, y, z);
                else if (s == 4) b = depth < 2 ? "snow_block" : rock(x, y, z);
                else b = rock(x, y, z);
                d.set(x, y, z, b);
            }
            if (s == 2 && top > 118 && d.rnd(x, 9, z, 2) < 0.5) d.set(x, top + 1, z, "snow[layers=1]");
        }
    }

    String grassTop(int x, int z, int top) {
        double p = n3.noise2(x * 0.045 + 100, z * 0.045);
        if (top > 100 && p > 0.1) return "podzol[snowy=false]";
        if (p > 0.62) return "coarse_dirt";
        if (p < -0.66) return "moss_block";
        return "grass_block[snowy=false]";
    }

    /** Layered rock with strata (gives mountains real shaping). */
    String rock(int x, int y, int z) {
        double band = y * 0.55 + 6 * n2.noise2(x * 0.02, z * 0.02);
        int b = Math.floorMod((int) Math.floor(band), 9);
        double r = d.rnd(x, y, z, 77);
        return switch (b) {
            case 0, 1 -> r < 0.7 ? "stone" : "andesite";
            case 2 -> r < 0.6 ? "andesite" : "stone";
            case 3 -> r < 0.75 ? "tuff" : "stone";
            case 4 -> r < 0.8 ? "stone" : "cobblestone";
            case 5 -> r < 0.6 ? "calcite" : "diorite";
            case 6 -> r < 0.7 ? "stone" : "gravel";
            case 7 -> r < 0.65 ? "andesite" : "tuff";
            default -> r < 0.85 ? "stone" : "mossy_cobblestone";
        };
    }

    // =====================================================================================================
    // Paths (curved, snapped to terrain)
    // =====================================================================================================

    /** Catmull-Rom path through points (x,z pairs), width w, paved with a palette. Lamps every lampEvery blocks. */
    void path(double[] pts, double w, String style, int lampEvery) {
        int np = pts.length / 2;
        double dist = 0, lastLamp = 0;
        int side = 1;
        java.util.LinkedHashSet<Long> cellsPaved = new java.util.LinkedHashSet<>();
        List<int[]> centre = new ArrayList<>();
        for (int i = 0; i < np - 1; i++) {
            double x0 = pts[Math.max(0, i - 1) * 2], z0 = pts[Math.max(0, i - 1) * 2 + 1];
            double x1 = pts[i * 2], z1 = pts[i * 2 + 1];
            double x2 = pts[(i + 1) * 2], z2 = pts[(i + 1) * 2 + 1];
            double x3 = pts[Math.min(np - 1, i + 2) * 2], z3 = pts[Math.min(np - 1, i + 2) * 2 + 1];
            double segLen = Math.hypot(x2 - x1, z2 - z1);
            int steps = (int) Math.ceil(segLen * 2);
            double px = x1, pz = z1;
            for (int s = 0; s <= steps; s++) {
                double t = s / (double) steps;
                double t2 = t * t, t3 = t2 * t;
                double cx = 0.5 * ((2 * x1) + (-x0 + x2) * t + (2 * x0 - 5 * x1 + 4 * x2 - x3) * t2 + (-x0 + 3 * x1 - 3 * x2 + x3) * t3);
                double cz = 0.5 * ((2 * z1) + (-z0 + z2) * t + (2 * z0 - 5 * z1 + 4 * z2 - z3) * t2 + (-z0 + 3 * z1 - 3 * z2 + z3) * t3);
                double step = Math.hypot(cx - px, cz - pz);
                dist += step;
                double tx = cx - px, tz = cz - pz;
                double tl = Math.max(1e-6, Math.hypot(tx, tz));
                px = cx; pz = cz;
                addCell(centre, (int) Math.floor(cx), (int) Math.floor(cz));
                double hw = w / 2.0;
                for (int ox = (int) Math.floor(cx - hw - 1); ox <= cx + hw + 1; ox++)
                    for (int oz = (int) Math.floor(cz - hw - 1); oz <= cz + hw + 1; oz++) {
                        double dd = Math.hypot(ox + 0.5 - cx, oz + 0.5 - cz);
                        if (dd > hw) continue;
                        if (!inMap(ox, oz) || surf[idx(ox, oz)] == 5 || surf[idx(ox, oz)] == 0) continue;
                        int gy = h[idx(ox, oz)];
                        boolean edge = dd > hw - 1.0;
                        String b;
                        if (style.equals("stone")) b = edge ? d.pick(ox, gy, oz, 41, "stone_bricks", 3, "mossy_stone_bricks", 1, "cracked_stone_bricks", 1)
                                : d.pick(ox, gy, oz, 42, "polished_andesite", 3, "andesite", 1, "stone", 1, "gravel", 1);
                        else if (style.equals("sand")) b = edge ? "smooth_sandstone" : d.pick(ox, gy, oz, 43, "sandstone", 2, "cut_sandstone", 1, "smooth_sandstone", 1);
                        else b = edge ? d.pick(ox, gy, oz, 44, "coarse_dirt", 2, "grass_block[snowy=false]", 1, "rooted_dirt", 1)
                                : d.pick(ox, gy, oz, 45, "dirt_path", 5, "gravel", 1, "coarse_dirt", 1, "packed_mud", 1);
                        d.set(ox, gy, oz, b);
                        // remove plants above
                        for (int yy = gy + 1; yy <= gy + 2; yy++) if (!d.isAir(ox, yy, oz) && !d.id(ox, yy, oz).contains("log")) d.air(ox, yy, oz);
                        reserve(ox, oz);
                        cellsPaved.add(cellKey(ox, oz));
                    }
                if (lampEvery > 0 && dist - lastLamp >= lampEvery) {
                    lastLamp = dist;
                    side = -side;
                    double nx = -tz / tl * side, nz = tx / tl * side;
                    int lx = (int) Math.floor(cx + nx * (hw + 1.2)), lz = (int) Math.floor(cz + nz * (hw + 1.2));
                    if (inMap(lx, lz) && surf[idx(lx, lz)] != 0 && surf[idx(lx, lz)] != 5) {
                        int gy = h[idx(lx, lz)];
                        d.set(lx, gy + 1, lz, "spruce_fence");
                        d.set(lx, gy + 2, lz, "spruce_fence");
                        d.set(lx, gy + 3, lz, "spruce_fence");
                        d.set(lx, gy + 4, lz, "lantern[hanging=false]");
                        reserve(lx, lz);
                    }
                }
            }
        }
        // grading: where the terrain under the paving jumps by 2+ blocks (lagoon banks, terrace edges) the lower
        // cells are banked up until neighbouring cells differ by at most one block (a causeway ramp, never a cut)
        boolean changed = true;
        java.util.Map<Long, Integer> raised = new java.util.HashMap<>();
        while (changed) {
            changed = false;
            for (long k : cellsPaved) {
                int x = (int) (k >> 32), z = (int) k;
                int gy = h[idx(x, z)];
                for (int f = 0; f < 4; f++) {
                    int nx = x + Shapes.DXZ[f][0], nz = z + Shapes.DXZ[f][1];
                    if (!cellsPaved.contains(cellKey(nx, nz))) continue;
                    int ny = h[idx(nx, nz)];
                    if (ny - gy > 1) { raised.putIfAbsent(k, gy); gy = ny - 1; h[idx(x, z)] = gy; changed = true; }
                }
            }
        }
        String fill = style.equals("stone") ? "stone" : style.equals("sand") ? "sandstone" : "dirt";
        for (var e : raised.entrySet()) {
            int x = (int) (e.getKey() >> 32), z = (int) (long) e.getKey();
            int oldY = e.getValue(), gy = h[idx(x, z)];
            String top = d.v.get(x, oldY, z);
            for (int y = oldY; y < gy; y++) d.set(x, y, z, fill);
            d.set(x, gy, z, top);
            for (int yy = gy + 1; yy <= gy + 2; yy++) if (!d.isAir(x, yy, z) && !d.id(x, yy, z).contains("log")) d.air(x, yy, z);
        }
        // 1-block rises along the paving get a step: a stair on the lower cell facing its higher neighbour (the
        // slope direction when it has several), so consecutive rises form a flight and the path is walked, not jumped
        String stepMat = style.equals("stone") ? "polished_andesite" : style.equals("sand") ? "smooth_sandstone" : "spruce";
        List<int[]> steps = new ArrayList<>();
        for (long k : cellsPaved) {
            int x = (int) (k >> 32), z = (int) k;
            int gy = h[idx(x, z)];
            int dir = -1, best = -1;
            for (int f = 0; f < 4; f++) {
                int nx = x + Shapes.DXZ[f][0], nz = z + Shapes.DXZ[f][1];
                if (!inMap(nx, nz) || h[idx(nx, nz)] != gy + 1) continue;
                // the next path cell up, or the paving of a structure the path runs into (plaza, aprons)
                boolean structure = !cellsPaved.contains(cellKey(nx, nz)) && surf[idx(nx, nz)] == 5
                        && Shapes.fullTop(d.v.get(nx, gy + 1, nz)) && d.isAir(nx, gy + 2, nz) && d.isAir(nx, gy + 3, nz);
                if (!cellsPaved.contains(cellKey(nx, nz)) && !structure) continue;
                // prefer the direction of the actual slope (the cell behind is not higher)
                int bx = x - Shapes.DXZ[f][0], bz = z - Shapes.DXZ[f][1];
                int score = cellsPaved.contains(cellKey(bx, bz)) && h[idx(bx, bz)] <= gy ? 2 : 1;
                if (score > best) { best = score; dir = f; }
            }
            if (dir >= 0) steps.add(new int[]{x, z, gy, dir});
        }
        for (int[] s : steps) {
            int x = s[0], z = s[1], gy = s[2];
            if (!d.isAir(x, gy + 1, z)) continue;
            d.set(x, gy + 1, z, Draw.stairs(stepMat, Shapes.DIRS[s[3]], false));
            for (int yy = gy + 2; yy <= gy + 4; yy++) if (!d.isAir(x, yy, z) && !d.id(x, yy, z).contains("log")) d.air(x, yy, z);
            pathSteps.add(new int[]{x, gy + 1, z});
        }
        for (long k : cellsPaved) pathCells.add(new int[]{(int) (k >> 32), (int) k});
        registerPathWalk("path " + style + " (" + (int) pts[0] + "," + (int) pts[1] + ")->(" + (int) pts[pts.length - 2] + "," + (int) pts[pts.length - 1] + ")", centre);
    }

    static long cellKey(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }

    /** Append a cell to a 4-connected route (a diagonal move gets an intermediate cell). */
    static void addCell(List<int[]> cells, int x, int z) {
        if (!cells.isEmpty()) {
            int[] p = cells.get(cells.size() - 1);
            if (p[0] == x && p[1] == z) return;
            if (p[0] != x && p[1] != z) cells.add(new int[]{x, p[1]});
            else if (Math.abs(p[0] - x) + Math.abs(p[1] - z) > 1) {
                // a jump of 2+ cells along an axis (should not happen with half-block sampling): fill it
                int sx = Integer.signum(x - p[0]), sz = Integer.signum(z - p[1]);
                for (int cx = p[0] + sx, cz = p[1] + sz; cx != x || cz != z; cx += sx, cz += sz) cells.add(new int[]{cx, cz});
            }
        }
        cells.add(new int[]{x, z});
    }

    /** A straight route between cell centres (x1,z1) -> (x2,z2), 4-connected. */
    static List<int[]> lineCells(int x1, int z1, int x2, int z2) {
        List<int[]> out = new ArrayList<>();
        int n = Math.max(Math.abs(x2 - x1), Math.abs(z2 - z1)) * 2;
        for (int i = 0; i <= n; i++) {
            double t = n == 0 ? 0 : i / (double) n;
            addCell(out, (int) Math.floor(x1 + 0.5 + (x2 - x1) * t), (int) Math.floor(z1 + 0.5 + (z2 - z1) * t));
        }
        return out;
    }

    /** A walkable route (path, jetty...) checked end to end, in both directions, by the offline validator. */
    record PathWalk(String name, int[] xs, int[] zs, int[] ys) {}
    final List<PathWalk> pathWalks = new ArrayList<>();
    /** Every paved path cell {x, z}, and the step blocks {x, y, z} placed on them. */
    final List<int[]> pathCells = new ArrayList<>();
    final List<int[]> pathSteps = new ArrayList<>();

    /**
     * After the vegetation: leaves and plants that grew over a path (canopies spreading in from beside it) are
     * trimmed to 3 blocks of headroom above the walking surface, so nobody bumps their head or has to jump.
     */
    void trimPathCanopy() {
        for (int[] c : pathCells) {
            int x = c[0], z = c[1];
            int gy = h[idx(x, z)];
            for (int y = gy + 1; y <= gy + 4; y++) {
                String id = d.id(x, y, z);
                if (id == null) continue;
                if (id.endsWith("_leaves") || id.equals("vine") || Shapes.passable(d.v.get(x, y, z)) && !Shapes.isCarpet(id) && !id.equals("water") && !id.equals("lava")) d.air(x, y, z);
            }
        }
    }

    void registerPathWalk(String name, List<int[]> cells) {
        if (cells.size() < 2) return;
        int n = cells.size();
        int[] xs = new int[n], zs = new int[n], ys = new int[n];
        for (int i = 0; i < n; i++) { xs[i] = cells.get(i)[0]; zs[i] = cells.get(i)[1]; ys[i] = hAt(xs[i], zs[i]); }
        pathWalks.add(new PathWalk(name, xs, zs, ys));
    }

    // =====================================================================================================
    // Vegetation
    // =====================================================================================================

    void vegetation() {
        for (int x = -OCEAN + 2; x < OCEAN - 2; x++) for (int z = -OCEAN + 2; z < OCEAN - 2; z++) {
            int i = idx(x, z);
            if (reserved[i]) continue;
            byte s = surf[i];
            int y = h[i];
            if (s == 0 || s == 5) continue;
            if (!d.isAir(x, y + 1, z)) continue;
            String top = d.id(x, y, z);
            if (top == null) continue;
            double r = d.rnd(x, 0, z, 101);
            double forest = 0.5 + 0.5 * n1.noise2(x * 0.006 + 40, z * 0.006 - 20);
            double clump = n4.noise2(x * 0.04, z * 0.04);
            if (s == 1) {
                // beach: palms
                if (r < 0.0045 && y >= SEA + 1 && !nearReserved(x, z, 4)) Trees.palm(d, x, y + 1, z, d.rnd(x, 1, z, 3), (int) (r * 1e6));
                else if (r > 0.995 && y >= SEA + 1) d.set(x, y + 1, z, "dead_bush");
                continue;
            }
            if (s != 2 || !(top.equals("grass_block") || top.equals("podzol") || top.equals("moss_block") || top.equals("coarse_dirt"))) {
                if (s == 3 && r < 0.004 && y < 120) d.set(x, y + 1, z, "moss_carpet");
                continue;
            }
            double density = 0.004 + 0.05 * smooth((forest - 0.35) / 0.4) * (0.6 + 0.4 * clump);
            double dc = Math.hypot(x - PLAZA_X, z - PLAZA_Z);
            if (dc < 70) density *= 0.35;
            if (r < density && !nearReserved(x, z, 3)) {
                double kind = 0.5 + 0.5 * n2.noise2(x * 0.01 + 7, z * 0.01 - 3);
                double sz = d.rnd(x, 2, z, 9);
                int salt = (int) (r * 1e7) + x * 31 + z;
                // cherry grove east of plaza
                if (Math.hypot(x - 70, z - 50) < 32 || Math.hypot(x + 60, z - 30) < 18) Trees.cherry(d, x, y + 1, z, sz, salt);
                else if (y > 100) Trees.spruce(d, x, y + 1, z, sz, salt);
                else Trees.forestTree(d, x, y + 1, z, kind, sz, salt);
                continue;
            }
            double rr = d.rnd(x, 5, z, 102);
            if (rr < 0.20) d.set(x, y + 1, z, "short_grass");
            else if (rr < 0.23) {
                d.set(x, y + 1, z, "tall_grass[half=lower]");
                d.set(x, y + 2, z, "tall_grass[half=upper]");
            } else if (rr < 0.265) {
                String[] fl = {"poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet", "allium", "lily_of_the_valley", "red_tulip", "orange_tulip", "pink_tulip"};
                double fz = 0.5 + 0.5 * n3.noise2(x * 0.03, z * 0.03);
                d.set(x, y + 1, z, fl[Math.min(fl.length - 1, (int) (fz * fl.length))]);
            } else if (rr < 0.272) Trees.bush(d, x, y + 1, z, 0.2, x * 7 + z);
            else if (rr < 0.276 && y > 85) d.set(x, y + 1, z, "fern");
        }
    }

    boolean nearReserved(int x, int z, int r) {
        for (int dx = -r; dx <= r; dx += Math.max(1, r)) for (int dz = -r; dz <= r; dz += Math.max(1, r))
            if (isReserved(x + dx, z + dz)) return true;
        return isReserved(x, z);
    }

    // =====================================================================================================
    // Staircases
    // =====================================================================================================

    /**
     * Straight staircase of bottom-half stairs, ascending toward {@code facing}.
     * <p>{@code (bx,by,bz)} is the lower floor block at the foot (the block you stand on before the first step) and
     * {@code (tx,ty,tz)} the upper floor block at the head, both on the centre line. The rise {@code n = ty - by}
     * is climbed with exactly n stairs at by+1 .. ty (the first sits on the floor, the last is flush with the upper
     * level), so the run between the two floor blocks must be n + 1. {@code width} columns are centred on the line
     * (odd widths are symmetric); {@code mat(lat)} gives the stair material (without "_stairs") and {@code under(lat)}
     * the block beneath each step, by the lateral world coordinate (x for north/south stairs, z for east/west).
     */
    void staircase(String name, int bx, int by, int bz, int tx, int ty, int tz, String facing, int width,
                   java.util.function.IntFunction<String> mat, java.util.function.IntFunction<String> under) {
        int f = Shapes.dirIndex(facing);
        if (f < 0) throw new IllegalArgumentException(name + ": bad facing " + facing);
        int fx = Shapes.DXZ[f][0], fz = Shapes.DXZ[f][1];
        int n = ty - by;
        int run = (tx - bx) * fx + (tz - bz) * fz;
        int lateralOff = (tx - bx) * fz + (tz - bz) * fx;
        if (n < 1 || run != n + 1 || lateralOff != 0)
            throw new IllegalArgumentException(name + ": rise " + n + " needs a run of " + (n + 1) + " along " + facing + ", got run " + run + " / lateral " + lateralOff);
        int lx = Math.abs(fz), lz = Math.abs(fx); // lateral unit vector
        for (int w = -(width - 1) / 2; w <= width / 2; w++) {
            int cx = bx + lx * w, cz = bz + lz * w;
            int lat = lx != 0 ? cx : cz;
            int[] xs = new int[n + 2], zs = new int[n + 2];
            for (int i = 0; i <= n + 1; i++) { xs[i] = cx + fx * i; zs[i] = cz + fz * i; }
            for (int i = 1; i <= n; i++) {
                int x = xs[i], z = zs[i], y = by + i;
                for (int yy = by - 2; yy < y - 1; yy++) d.set(x, yy, z, "stone");
                d.set(x, y - 1, z, under.apply(lat));
                d.set(x, y, z, Draw.stairs(mat.apply(lat), facing, false));
                for (int yy = y + 1; yy <= y + 4; yy++) d.air(x, yy, z);
                paved(x, z, y);
            }
            walks.add(new Walk(name + " [" + (lx != 0 ? "x=" : "z=") + lat + "]", xs, zs, by, ty));
        }
    }

    /** Mark a column as built (structure/paved) at the given top height. */
    void paved(int x, int z, int top) {
        if (!inMap(x, z)) return;
        surf[idx(x, z)] = 5;
        reserved[idx(x, z)] = true;
        h[idx(x, z)] = top;
    }
}
