package com.mcm.games.tgttos.maps.b;

import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.maps.b.util.Noise;
import com.mcm.games.tgttos.maps.b.util.Noise.Pal;
import com.mcm.games.tgttos.maps.b.util.Stairs;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

import java.util.ArrayList;
import java.util.List;

/**
 * Frozen Canal (boats on ice), v2: a ~1,150-block point-to-point ice race through a snowy canal town, folded into the
 * slot with hairpins and switchbacks (geometry in {@link CanalCourse}). Packed ice, red-brick quays three blocks high
 * (boats can't leave the canal, players on foot can't climb out), downhill only (1-block step-downs, boats can't
 * climb). Twelve named segments: Grid Straight (40-boat start lock, footbridges with piers), Mill Hairpin, Weavers'
 * Chicane, Cellar Hairpin, Cellar Steps (four 1-block drops), the Fish Market split (5-wide raised blue-ice causeway
 * with open edges onto a slush flat vs the walled ring, ~30% longer), Brewery Bend, the Blue Mile (blue-ice stripe,
 * arch bridges), the Bell Switchbacks (two braking hairpins), Tannery Sweeper (slush on the apexes), Lantern Bend and
 * the Barge Basin (moored barges, finish arch, last drop) to the dock. Then on foot: out of the boats, up the 3-wide
 * Dock Stairs (or the wider side flights) and across the 3-wide jetty over the frozen mere (knocked off = down on the
 * ice, back up the skaters' steps) or round by the wide east bridge, to the rink pen on Lantern Island under the tall
 * lantern tree.
 *
 * <p>Local frame: start lock x -21..20 / z 0..8, ice y = 0 there (boats at y 1), town ground y = 3, course
 * downhill to y = -6 at the dock.
 */
public final class FrozenCanal implements TgMap {

    @Override public String id() { return "frozen_canal"; }
    @Override public String displayName() { return "Frozen Canal"; }
    @Override public MapType type() { return MapType.BOAT; }
    @Override public Kit kit() { return Kit.BOAT; }

    @Override
    public TgMapData build(long seed) { return generate(seed).d; }

    /** Town ground block level (quay copings, bridge decks). */
    public static final int T = 3;
    /** Frozen mere ice block level. */
    public static final int MERE = 0;
    public static final int LIMIT = 246;

    // finish area (the dock end is at x 104, heading -X, ice level -6; checked in generate)
    public static final int DOCK_X = 104, DOCK_Z = 28;
    /** Landing stage at the dock end (top block y -5): players step out of the boats onto it. */
    public static final Region STAGE = Region.of(96, -5, 21, 103, -5, 35);
    public static final double[] ISLAND = {84.5, 66.5, 13};
    public static final double[] MERE_E = {70, 100, 48, 58};      // centre x, z, radius x, z
    /** On-foot finish zone (dock, quay, mere, island): the only place where a racer may stand on town level. */
    public static final Region FINISH_ZONE = Region.of(58, -8, 8, 126, 12, 96);
    public static final Region JETTY = Region.of(84, T, 44, 86, T, 54);
    public static final Region EAST_BRIDGE = Region.of(97, T, 64, 112, T, 68);

    static final Pal BRICK = new Pal("bricks", 6, "mud_bricks", 1, "granite", 1);
    static final Pal QUAY = new Pal("stone_bricks", 5, "polished_andesite", 2, "cracked_stone_bricks", 1);
    static final Pal ICE = new Pal("packed_ice", 8, "ice", 2);
    static final Pal MERE_ICE = new Pal("packed_ice", 5, "ice", 4, "blue_ice", 1);
    static final Pal ROCK = new Pal("stone", 5, "andesite", 3, "cobblestone", 2, "tuff", 1);

    /** Course geometry of the last generate() call (validators read it). */
    public CanalCourse course;
    public final List<int[]> bridges = new ArrayList<>();     // {sCentre*10, headroomAtCentre}

    // per-cell flags (indexed like course)
    private boolean[] island, mere, house;

    public G generate(long seed) {
        TgMapData d = new TgMapData(id(), seed);
        G g = new G(d);
        course = new CanalCourse().raster();
        CanalCourse c = course;
        double[] dk = c.main.at(c.main.length());
        if (Math.abs(dk[2] - 270) > 0.5 || Math.floor(dk[0]) != DOCK_X || Math.abs(dk[1] - DOCK_Z - 0.1) > 1)
            throw new IllegalStateException("dock moved: " + dk[0] + "," + dk[1] + " heading " + dk[2]);
        island = new boolean[c.kind.length];
        mere = new boolean[c.kind.length];
        house = new boolean[c.kind.length];
        long t0 = System.nanoTime();
        plan(g);
        ground(g);
        long t1 = System.nanoTime();
        canal(g);
        startLock(g);
        bridges(g);
        features(g);
        finish(g);
        long t2 = System.nanoTime();
        g.scenery = true;
        town(g);
        lamps(g);
        g.scenery = false;
        long t3 = System.nanoTime();
        if (System.getProperty("canal.prof") != null)
            System.out.printf("  prof: ground %d ms, canal+finish %d ms, town %d ms%n", (t1 - t0) / 1000000, (t2 - t1) / 1000000, (t3 - t2) / 1000000);

        d.voidY = -16;
        d.playBounds = Region.of(-128, -18, -84, LIMIT, 72, LIMIT);
        d.overviewPoints.add(new Spawn(60.5, 70, -40.5, 20f, 40f));
        d.overviewPoints.add(new Spawn(84.5, 30, 30.5, 0f, 25f));
        double ring = c.cutTo - c.cutFrom;
        d.notes.put("track", String.format("%.0f blocks of boat track (safe line), %.0f with the causeway, 12 segments, ice y 0 -> %d",
                c.main.length(), c.main.length() - ring + c.cut.length(), c.dockLevel));
        d.notes.put("safeLine", "middle of the hairpins, the walled Fish Market ring, packed-ice edges of the Blue Mile, wide barge gaps, side flights + east bridge on foot");
        d.notes.put("fastLine", "inside blue-ice kerbs, the raised causeway (open edges onto slush), blue stripe, 3-wide barge gaps, Dock Stairs + jetty");
        g.finish();
        return g;
    }

    // ------------------------------------------------------------------ plan

    private int ix(int x, int z) { return course.idx(x, z); }

    private boolean inMere(int x, int z) {
        double ex = (x + 0.5 - MERE_E[0]) / MERE_E[2], ez = (z + 0.5 - MERE_E[1]) / MERE_E[3];
        return ex * ex + ez * ez <= 1;
    }

    private boolean inIsland(int x, int z) {
        return Math.hypot(x + 0.5 - ISLAND[0], z + 0.5 - ISLAND[1]) <= ISLAND[2];
    }

    private void plan(G g) {
        CanalCourse c = course;
        for (int x = c.x1; x <= c.x2; x++)
            for (int z = c.z1; z <= c.z2; z++) {
                int i = ix(x, z);
                if (Math.abs(x) > LIMIT || Math.abs(z) > LIMIT) continue;
                double n = Noise.fbm2(x * 0.045, z * 0.045, g.seed + 21);
                double ex = (x - 92) / 112.0, ez = (z - 112) / 92.0;
                boolean core = ex * ex + ez * ez <= 1 + 0.12 * n
                        || (x >= FINISH_ZONE.minX() && x <= FINISH_ZONE.maxX() + 8 && z >= FINISH_ZONE.minZ() && z <= FINISH_ZONE.maxZ());
                boolean in = c.margin[i] <= 15 + 8 * n || core || c.kind[i] != CanalCourse.NONE;
                island[i] = in;
                if (in && c.kind[i] == CanalCourse.NONE && c.margin[i] > 9 && inMere(x, z) && !inIsland(x, z)) mere[i] = true;
            }
    }

    // ------------------------------------------------------------------ ground, mere, island skirt

    private void ground(G g) {
        CanalCourse c = course;
        g.scenery = true;
        for (int x = c.x1; x <= c.x2; x++)
            for (int z = c.z1; z <= c.z2; z++) {
                int i = ix(x, z);
                if (!island[i] || c.kind[i] != CanalCourse.NONE) continue;
                if (mere[i]) {
                    g.set(x, MERE, z, MERE_ICE.at(x, MERE, z, g.seed));
                    continue;
                }
                boolean rim = false, edge = false;
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++) {
                        int j = ix(x + dx, z + dz);
                        if (j < 0 || !island[j]) edge = true;
                        else if (mere[j]) rim = true;
                    }
                double r = Noise.fbm2(x * 0.13, z * 0.13, g.seed + 8);
                String top = r < 0.56 ? "snow_block" : r < 0.78 ? "stone_bricks" : r < 0.9 ? "polished_andesite" : "cobblestone";
                if (rim) {
                    for (int y = MERE; y < T; y++) g.set(x, y, z, QUAY.at(x, y, z, g.seed));
                    top = "stone_bricks";
                }
                g.set(x, T, z, top);
                if (top.equals("snow_block") && g.rnd(x, 1, z) < 0.35 && !FINISH_ZONE.contains(x + 0.5, T, z + 0.5)) g.set(x, T + 1, z, "snow[layers=" + (1 + (int) (g.rnd(x, 2, z) * 2)) + "]");
                if (edge) {
                    int depth = 3 + (int) (Noise.fbm2(x * 0.2, z * 0.2, g.seed + 4) * 9);
                    for (int y = T - depth; y < T; y++) g.set(x, y, z, y > T - 2 ? "dirt" : ROCK.at(x, y, z, g.seed));
                }
            }
        g.scenery = false;
    }

    // ------------------------------------------------------------------ canal

    private void canal(G g) {
        CanalCourse c = course;
        for (int x = c.x1; x <= c.x2; x++)
            for (int z = c.z1; z <= c.z2; z++) {
                int i = ix(x, z);
                int k = c.kind[i];
                if (k == CanalCourse.NONE || !island[i]) continue;
                int lv = c.level[i];
                if (k == CanalCourse.LANE || k == CanalCourse.FLAT) {
                    g.set(x, lv - 1, z, (x + z & 7) == 0 ? "mud_bricks" : "stone");
                    g.set(x, lv, z, k == CanalCourse.FLAT ? "snow_block" : c.path[i] == CanalCourse.CUT ? "blue_ice" : ICE.at(x, lv, z, g.seed));
                    for (int y = lv + 1; y <= T + 1; y++) g.set(x, y, z, null);
                } else {
                    int foot = Math.min(c.minLevel[i], lv);
                    boolean inner = c.margin[i] <= 1.0;
                    if (inner) for (int y = foot; y < T; y++) g.set(x, y, z, BRICK.at(x, y, z, g.seed));
                    else for (int y = Math.max(foot, T - 2); y < T; y++) g.set(x, y, z, "stone");
                    g.set(x, T, z, QUAY.at(x, T, z, g.seed));
                    g.set(x, T + 1, z, null);
                }
            }
        // the causeway's open edge: a chiselled kerb face (the step down onto the slush reads from the boat)
        for (int x = c.x1; x <= c.x2; x++)
            for (int z = c.z1; z <= c.z2; z++) {
                int i = ix(x, z);
                if (c.kind[i] != CanalCourse.LANE || c.path[i] != CanalCourse.CUT) continue;
                for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int j = ix(x + o[0], z + o[1]);
                    if (j >= 0 && c.kind[j] == CanalCourse.FLAT && c.level[j] < c.level[i]) g.set(x, c.level[i] - 1, z, "chiseled_stone_bricks");
                }
            }
    }

    // ------------------------------------------------------------------ start lock

    private void startLock(G g) {
        TgMapData d = g.d;
        g.scenery = true;
        // lock chamber: brick walls 2 thick round the tray, coping at T
        for (int x = -23; x <= 22; x++)
            for (int z = -3; z <= 8; z++) {
                boolean wall = x <= -22 || x >= 21 || z <= -1;
                if (!wall) continue;
                for (int y = -2; y < T; y++) g.set(x, y, z, BRICK.at(x, y, z, g.seed));
                g.set(x, T, z, QUAY.at(x, T, z, g.seed));
                g.set(x, T + 1, z, z == -3 || x == -23 || x == 22 ? (x % 4 == 0 || z % 4 == 0 ? "stone_brick_wall" : "spruce_fence") : null);
            }
        // lock gate gantry high over the chamber mouth (clear of the sight line to the lantern tree)
        int gy = T + 12;
        for (int x = -24; x <= 23; x++) { g.set(x, gy, 9, "dark_oak_planks"); g.set(x, gy + 1, 9, ((x + 40) / 3) % 2 == 0 ? "red_wool" : "white_wool"); }
        for (int x = -24; x <= 23; x += 4) g.set(x, gy - 1, 9, "lantern[hanging=true]");
        for (int y = T + 1; y < gy; y++) { g.set(-24, y, 9, "dark_oak_log"); g.set(23, y, 9, "dark_oak_log"); }
        g.text("GO", 3, gy - 2, 9, -1, 0, 1, "black_wool");
        g.scenery = false;
        for (int x = -21; x <= 20; x++)
            for (int z = 0; z <= 8; z++) {
                g.set(x, -1, z, "stone");
                g.set(x, 0, z, z == 8 ? ((x & 1) == 0 ? "blue_ice" : "packed_ice") : "packed_ice");
                for (int y = 1; y <= T + 1; y++) g.set(x, y, z, null);
            }
        // glass lock gate at z 9 (removed at GO)
        for (int y = 1; y <= T; y++)
            for (int x = -21; x <= 20; x++) { g.set(x, y, 9, "glass"); d.startBarrier.add(new TgMapData.Pos(x, y, 9)); }
        d.startStrip = Region.of(-21, 1, 0, 20, 4, 8);
        for (int r = 0; r < 2; r++)
            for (int i = 0; i < 20; i++) {
                double x = -19.5 + i * 2.05, z = 2.5 + r * 3.5;
                d.boatSpawns.add(new Spawn(x, 1, z, 0f));
                d.startSpawns.add(new Spawn(x, 1, z, 0f));
            }
    }

    // ------------------------------------------------------------------ bridges

    /**
     * Footbridge across the main canal at arc position sb (axis-aligned straights only): deck at T over the lane and
     * the walls, railings, lanterns; piers = full-block columns in the lane at the given lateral ranges.
     */
    private void bridge(G g, double sb, double[][] piers, boolean arch) {
        CanalCourse c = course;
        double[] p = c.main.at(sb);
        double hw = c.main.halfWidth(sb) + CanalCourse.WALL_W + 1;
        int cx = (int) Math.floor(p[0]), cz = (int) Math.floor(p[1]);
        int head = 99;
        for (int x = cx - 14; x <= cx + 14; x++)
            for (int z = cz - 14; z <= cz + 14; z++) {
                int i = ix(x, z);
                if (i < 0 || c.path[i] != CanalCourse.MAIN || Math.abs(c.sArr[i] - sb) > 1.5 || Math.abs(c.dArr[i]) > hw) continue;
                if (c.kind[i] == CanalCourse.NONE && Math.abs(c.dArr[i]) > hw - 0.5) continue;
                boolean mid = Math.abs(c.sArr[i] - sb) <= 0.5;
                g.scenery = true;
                g.set(x, T, z, mid ? "spruce_planks" : "stone_bricks");
                if (arch && c.kind[i] == CanalCourse.LANE) {
                    double t = Math.abs(c.dArr[i]) / c.main.halfWidth(sb);
                    int drop = (int) Math.floor((1 - Math.sqrt(Math.max(0, 1 - t * t))) * 3);
                    for (int y = T - drop; y < T; y++) g.set(x, y, z, "stone_bricks");
                    if (drop > 0) g.set(x, T - drop - 0, z, "stone_bricks");
                }
                if (!mid) {
                    boolean post = ((int) Math.floor(c.dArr[i] + 100) % 4) == 0;
                    g.set(x, T + 1, z, post ? "stone_brick_wall" : "spruce_fence");
                    if (post && ((int) Math.floor(c.dArr[i] + 100) % 8) == 0) g.set(x, T + 2, z, "lantern[hanging=false]");
                } else g.set(x, T + 1, z, null);
                g.scenery = false;
                if (c.kind[i] == CanalCourse.LANE) {
                    boolean pier = false;
                    for (double[] pr : piers)
                        if (c.dArr[i] >= pr[0] && c.dArr[i] <= pr[1]) {
                            pier = true;
                            for (int y = c.level[i] + 1; y < T; y++) g.set(x, y, z, y == c.level[i] + 1 ? "stone_bricks" : BRICK.at(x, y, z, g.seed));
                        }
                    if (!pier) {
                        int hr = 0;
                        while (hr < 12 && g.get(x, c.level[i] + 1 + hr, z) == null) hr++;
                        head = Math.min(head, hr);
                    }
                }
            }
        bridges.add(new int[]{(int) Math.round(sb * 10), head});
    }

    private void bridges(G g) {
        CanalCourse.Path m = course.main;
        double[][] none = {};
        // Grid Straight: two low footbridges (2 headroom) whose piers split the lane into three
        bridge(g, 36, new double[][]{{-5.0, -3.0}, {3.0, 5.0}}, false);
        bridge(g, 68, new double[][]{{-1.2, 1.2}}, false);
        // Cellar Steps: one between the second and third drop
        bridge(g, m.mark0("cascade") + CanalCourse.STEP0 + CanalCourse.STEP_GAP * 1.5, new double[][]{{-1.2, 1.2}}, false);
        // Blue Mile: three arch bridges, no piers (scenery at speed)
        double sb = m.mark0("blue");
        bridge(g, sb + 35, none, true);
        bridge(g, sb + 85, none, true);
        bridge(g, sb + 135, none, true);
        // Bell Switchbacks: the bell footbridge over the short straight
        double br = m.mark0("brake") + Math.PI * 12 + 6;
        bridge(g, br, none, false);
        // bell tower on the island inside the switchbacks
        double[] p = m.point(br, 0);
        int bx = (int) Math.floor(p[0]), bz = (int) Math.floor(p[1]) + 12;
        g.scenery = true;
        for (int y = T + 1; y <= T + 14; y++)
            for (int a = 0; a < 3; a++) for (int b = 0; b < 3; b++) {
                boolean corner = (a != 1 && b != 1);
                if (corner || y <= T + 9 || y >= T + 13) g.set(bx + a, y, bz + b, y >= T + 13 ? "dark_oak_planks" : corner ? "stripped_spruce_log" : BRICK.at(bx + a, y, bz + b, g.seed));
                else g.set(bx + a, y, bz + b, null);
            }
        g.set(bx + 1, T + 12, bz + 1, "bell[attachment=ceiling,facing=north]");
        g.set(bx + 1, T + 15, bz + 1, "lightning_rod[facing=up]");
        g.scenery = false;
    }

    // ------------------------------------------------------------------ lane features (materials, obstacles)

    private void features(G g) {
        CanalCourse c = course;
        CanalCourse.Path m = c.main;
        double sBlue = m.mark0("blue"), sBrake = m.mark0("brake");
        double sBasin = m.mark0("basin"), sFin = m.mark0("finish");
        for (int x = c.x1; x <= c.x2; x++)
            for (int z = c.z1; z <= c.z2; z++) {
                int i = ix(x, z);
                if (c.kind[i] != CanalCourse.LANE || c.path[i] != CanalCourse.MAIN || !island[i]) continue;
                double s = c.sArr[i], dd = c.dArr[i], hw = m.halfWidth(s);
                int lv = c.level[i];
                String mat = null;
                // Blue Mile: 5-wide blue-ice stripe
                if (s >= sBlue && s <= sBrake && Math.abs(dd) <= 2.5) mat = "blue_ice";
                double[] arc = m.arcAt(s);
                if (arc != null) {
                    double inside = Math.signum(arc[2]) * dd;
                    double f = (s - arc[0]) / (arc[1] - arc[0]);
                    if (Math.abs(arc[2]) >= 150) {
                        if (inside >= hw - 3) mat = "blue_ice";                                  // inside kerb (fast)
                        else if (inside <= -(hw - 1.6) && f > 0.2 && f < 0.8 && ((x + z) & 3) != 0) mat = "snow_block";   // drift wide: slush
                    } else if (s > m.mark0("chicane") && s < m.mark0("chicaneEnd")) {
                        if (inside >= hw - 2.2 && f > 0.3 && f < 0.75) mat = "snow_block";            // chicane apexes
                    } else if (s > m.mark0("sweeper") && s < sBasin) {
                        if (inside >= hw - 3.2 && f > 0.3 && f < 0.7) mat = "snow_block";             // sweeper / lantern bend apexes
                    } else if (s > m.mark0("merge") && s < sBlue) {
                        if (inside >= hw - 2.5) mat = "blue_ice";                                     // brewery bend kerb
                    }
                }
                // start-grid stripes and step-down lip markers
                if (s > 0 && s < 1.2) mat = ((x + 64) & 1) == 0 ? "blue_ice" : "packed_ice";
                double sc0 = m.mark0("cascade") + CanalCourse.STEP0;
                for (double st : new double[]{sc0, sc0 + CanalCourse.STEP_GAP, sc0 + 2 * CanalCourse.STEP_GAP, sc0 + 3 * CanalCourse.STEP_GAP, sFin + 2})
                    if (s >= st - 1.2 && s < st) mat = "blue_ice";
                if (mat != null) g.set(x, lv, z, mat);
            }
        // mooring dolphins (2x2 timber piles, 1 high + lantern) in the chicane straights and the grid straight
        double sc = m.mark0("chicane");
        dolphin(g, sc + 24, 2.0);      // off-centre: a 3-wide squeeze on one side, the wide way on the other
        dolphin(g, sc + 52, -3.5);
        dolphin(g, 82, -3);
        // Barge Basin: three moored barges (hull only at boat height): tight 3-wide gaps (fast) or the wide side
        barge(g, sBasin + 10, sBasin + 19, -1.6, 1.6);
        barge(g, sBasin + 28, sBasin + 37, 2.4, 5.6);
        barge(g, sBasin + 44, sBasin + 52, -5.6, -2.4);
        // finish arch over the basin at the last drop
        finishArch(g, sFin + 2);
    }

    private void dolphin(G g, double s, double dd) {
        double[] p = course.main.point(s, dd);
        int x0 = (int) Math.floor(p[0]), z0 = (int) Math.floor(p[1]);
        int lv = course.level[ix(x0, z0)];
        for (int x = x0; x <= x0 + 1; x++)
            for (int z = z0; z <= z0 + 1; z++) g.set(x, lv + 1, z, "stripped_spruce_log");
        g.set(x0, lv + 2, z0, "torch");          // no stepping stone: nothing solid above boat height
        g.set(x0 + 1, lv + 2, z0 + 1, "torch");
    }

    /** A barge on the main lane between arc positions s1..s2 and lateral d1..d2 (straights). */
    private void barge(G g, double s1, double s2, double d1, double d2) {
        CanalCourse c = course;
        double[] p = c.main.at((s1 + s2) / 2);
        int cx = (int) Math.floor(p[0]), cz = (int) Math.floor(p[1]);
        for (int x = cx - 12; x <= cx + 12; x++)
            for (int z = cz - 12; z <= cz + 12; z++) {
                int i = ix(x, z);
                if (i < 0 || c.kind[i] != CanalCourse.LANE || c.path[i] != CanalCourse.MAIN) continue;
                double s = c.sArr[i], dd = c.dArr[i];
                if (s < s1 || s > s2 || dd < d1 || dd > d2) continue;
                int lv = c.level[i];
                boolean rim = s < s1 + 1 || s > s2 - 1 || dd < d1 + 1 || dd > d2 - 1;
                boolean mid = Math.abs(dd - (d1 + d2) / 2) < 0.6;
                g.set(x, lv + 1, z, rim ? "dark_oak_planks" : "spruce_planks");
                g.scenery = true;
                if (rim) g.set(x, lv + 2, z, "dark_oak_slab[type=bottom]");
                else if (mid && s > s2 - 3) { g.set(x, lv + 2, z, "red_terracotta"); g.set(x, lv + 3, z, "dark_oak_slab[type=bottom]"); }
                else if (mid && ((int) s % 3) == 0) g.set(x, lv + 2, z, "barrel[facing=up,open=false]");
                g.scenery = false;
            }
    }

    private void finishArch(G g, double sa) {
        CanalCourse c = course;
        double[] p = c.main.at(sa);
        int ax = (int) Math.floor(p[0]);
        int hw = (int) Math.ceil(c.main.halfWidth(sa)) + 2;
        int zc = (int) Math.floor(p[1]);
        g.scenery = true;
        int top = T + 9;
        for (int k = 0; k < 2; k++) {
            int x = ax + k;
            for (int y = T + 1; y <= top; y++) {
                g.set(x, y, zc - hw, (y / 2) % 2 == 0 ? "red_wool" : "white_wool");
                g.set(x, y, zc + hw, (y / 2) % 2 == 0 ? "red_wool" : "white_wool");
            }
            for (int z = zc - hw; z <= zc + hw; z++) { g.set(x, top, z, ((z + 40) / 2) % 2 == 0 ? "red_wool" : "white_wool"); g.set(x, top + 1, z, "black_wool"); }
        }
        g.text("FINISH", ax + 2, top - 1, zc + 11, 0, -1, 1, "black_wool");
        g.scenery = false;
    }

    // ------------------------------------------------------------------ finish: dock, stairs, jetty, island, pen

    private void finish(G g) {
        TgMapData d = g.d;
        int dl = course.dockLevel;              // -6
        // masonry block of the dock: x 84..103, z 10..44, from the bed up to the quay
        for (int x = 84; x <= 103; x++)
            for (int z = 10; z <= 44; z++) {
                int i = ix(x, z);
                if (i >= 0 && course.kind[i] == CanalCourse.LANE) continue;
                for (int y = dl - 1; y < T; y++) g.set(x, y, z, y > T - 3 ? QUAY.at(x, y, z, g.seed) : BRICK.at(x, y, z, g.seed));
                g.set(x, T, z, QUAY.at(x, T, z, g.seed));
                for (int y = T + 1; y <= T + 3; y++) g.set(x, y, z, null);
            }
        // landing stage (one block above the ice): boats stop at its edge
        for (int x = STAGE.minX(); x <= STAGE.maxX(); x++)
            for (int z = STAGE.minZ(); z <= STAGE.maxZ(); z++) {
                g.set(x, dl + 1, z, ((x + z) & 1) == 0 ? "spruce_planks" : "stripped_spruce_wood");
                for (int y = dl + 2; y <= T; y++) g.set(x, y, z, null);
            }
        // stage edge bollards are NOT placed (they would snag boats); a yellow stripe instead
        for (int z = STAGE.minZ(); z <= STAGE.maxZ(); z++) g.set(STAGE.maxX(), dl + 1, z, (z & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
        // Dock Stairs: 3 wide straight up west (the choke)
        Stairs.flight(g, "dockStairs", "stone_brick", "stone_bricks", 95, 27, dl + 1, -1, 0, 8, 3, dl);
        // side flights: 4 wide up north and south off the stage corners (wider, longer way)
        Stairs.flight(g, "dockNorth", "spruce", "spruce_planks", 98, 36, dl + 1, 0, 1, 8, 4, dl);
        Stairs.flight(g, "dockSouth", "spruce", "spruce_planks", 98, 20, dl + 1, 0, -1, 8, 4, dl);
        // the quay between the stairs and the mere: clear, lamps at the edges
        g.scenery = true;
        for (int[] q : new int[][]{{92, 24}, {92, 32}, {102, 46}, {94, 46}, {78, 44}})
            lampPost(g, q[0], q[1]);
        g.scenery = false;
        // jetty: 3 wide planks over the mere at T, posts down to the ice
        for (int x = JETTY.minX(); x <= JETTY.maxX(); x++)
            for (int z = JETTY.minZ(); z <= JETTY.maxZ() + 2; z++) {
                g.set(x, T, z, x == JETTY.minX() + 1 ? "spruce_planks" : "spruce_slab[type=top]");
                for (int y = T + 1; y <= T + 3; y++) g.set(x, y, z, null);
                if ((x == JETTY.minX() || x == JETTY.maxX()) && z % 4 == 0)
                    for (int y = MERE + 1; y < T; y++) g.set(x, y, z, "spruce_log");
            }
        // east bridge: 5 wide stone, parapet walls, longer way round
        for (int x = EAST_BRIDGE.minX(); x <= EAST_BRIDGE.maxX(); x++)
            for (int z = EAST_BRIDGE.minZ() - 1; z <= EAST_BRIDGE.maxZ() + 1; z++) {
                boolean edge = z < EAST_BRIDGE.minZ() || z > EAST_BRIDGE.maxZ();
                g.set(x, T, z, edge ? "stone_bricks" : QUAY.at(x, T, z, g.seed));
                g.set(x, T - 1, z, "stone_brick_slab[type=top]");
                g.set(x, T + 1, z, edge ? "stone_brick_wall" : null);
                if (edge && x % 5 == 0) g.set(x, T + 2, z, "lantern[hanging=false]");
            }
        // Lantern Island: solid masonry ring, snow top, skaters' steps on the north side
        for (int x = (int) (ISLAND[0] - ISLAND[2]) - 1; x <= ISLAND[0] + ISLAND[2] + 1; x++)
            for (int z = (int) (ISLAND[1] - ISLAND[2]) - 1; z <= ISLAND[1] + ISLAND[2] + 1; z++) {
                if (!inIsland(x, z)) continue;
                boolean rim = !inIsland(x + 1, z) || !inIsland(x - 1, z) || !inIsland(x, z + 1) || !inIsland(x, z - 1);
                for (int y = MERE; y < T; y++) if (rim || y == T - 1) g.set(x, y, z, rim ? QUAY.at(x, y, z, g.seed) : "dirt");
                g.set(x, T, z, rim ? "stone_bricks" : g.rnd(x, 3, z) < 0.6 ? "snow_block" : "packed_ice");
                for (int y = T + 1; y <= T + 4; y++) g.set(x, y, z, null);
            }
        // skaters' steps out of the mere: one onto the island (north), two onto the quay by the jetty
        stepsOut(g, "isleSteps", 83, 81, 0, -1);
        stepsOut(g, "quayStepsW", 77, 44, 0, -1);
        stepsOut(g, "quayStepsE", 90, 44, 0, -1);
        // pen: low striped ring 14 x 14, ice inside, gaps on the jetty and bridge sides
        int x1 = 78, x2 = 91, z1 = 56, z2 = 69;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                boolean ring = x == x1 || x == x2 || z == z1 || z == z2;
                g.set(x, T, z, ring ? "polished_andesite" : ((x + z) & 1) == 0 ? "packed_ice" : "snow_block");
                g.set(x, T + 1, z, ring ? (((x + z) & 1) == 0 ? "red_wool" : "white_wool") : null);
            }
        for (int x = 84; x <= 86; x++) g.set(x, T + 1, z1, null);
        for (int z = 65; z <= 67; z++) g.set(x2, T + 1, z, null);
        d.pen = Region.of(x1 + 1, T + 1, z1 + 1, x2 - 1, T + 3, z2 - 1);
        g.chickens(d.pen, 12);
        // path from the island shore to the pen gaps
        for (int z = 54; z <= 55; z++) for (int x = 84; x <= 86; x++) g.set(x, T, z, "spruce_planks");
        for (int x = 92; x <= 97; x++) for (int z = 65; z <= 67; z++) g.set(x, T, z, "stone_bricks");
        landmark(g, 84, 76);
    }

    /** A 3-wide, 3-step flight from the mere ice up to the quay, with a masonry box round it. */
    private void stepsOut(G g, String name, int x, int z, int dx, int dz) {
        // flight ascends along (dx,dz); masonry 1 around
        for (int a = -1; a <= 3; a++)
            for (int i = -1; i <= 4; i++) {
                int cx = x + (dz != 0 ? a : dx * i), cz = z + (dz != 0 ? dz * i : a);
                for (int y = MERE; y < T; y++) g.set(cx, y, cz, QUAY.at(cx, y, cz, g.seed));
                g.set(cx, T, cz, "stone_bricks");
            }
        Stairs.flight(g, name, "stone_brick", "stone_bricks", x, z, MERE, dx, dz, 3, 3, MERE);
        // floor in front of the first step (on the ice)
        for (int a = 0; a < 3; a++) {
            int fx = x + (dz != 0 ? a : -dx), fz = z + (dz != 0 ? -dz : a);
            g.set(fx, MERE, fz, "packed_ice");
            for (int y = MERE + 1; y <= MERE + 3; y++) g.set(fx, y, fz, null);
        }
    }

    private void landmark(G g, int tx, int tz) {
        g.scenery = true;
        int gy = T;
        for (int y = gy; y <= gy + 44; y++) g.set(tx, y, tz, "spruce_log");
        for (int y = gy + 7; y <= gy + 42; y++) {
            double r = (gy + 44 - y) * 0.27 + 1;
            int ri = (int) Math.ceil(r);
            for (int dx = -ri; dx <= ri; dx++)
                for (int dz = -ri; dz <= ri; dz++) {
                    double dd = Math.hypot(dx, dz);
                    if (dd > r || (dx == 0 && dz == 0)) continue;
                    if ((y % 4 == 0 && dd > r - 1.2) || dd > r - 0.8 && g.rnd(tx + dx, y, tz + dz) < 0.12)
                        g.set(tx + dx, y, tz + dz, ((dx + dz + y) & 1) == 0 ? "sea_lantern" : "glowstone");
                    else if (y % 4 != 3 || dd < r - 1) g.set(tx + dx, y, tz + dz, dd > r - 1.2 ? "spruce_leaves[persistent=true,distance=7]" : null);
                }
        }
        for (int y = gy + 44; y <= gy + 48; y++) g.set(tx, y, tz, "gold_block");
        g.set(tx - 1, gy + 46, tz, "gold_block"); g.set(tx + 1, gy + 46, tz, "gold_block");
        g.set(tx, gy + 46, tz - 1, "gold_block"); g.set(tx, gy + 46, tz + 1, "gold_block");
        g.landmark.add(new double[]{tx + 0.5, gy + 46.5, tz - 0.5});
        g.landmark.add(new double[]{tx + 0.5, gy + 48.5, tz + 0.5});
        g.scenery = false;
    }

    // ------------------------------------------------------------------ town

    private boolean sightCorridor(int x, int z) {
        // keep the line from the start lock to the lantern tree free of houses
        double ax = 0, az = 4, bx = 84.5, bz = 76.5;
        double vx = bx - ax, vz = bz - az;
        double t = Math.max(0, Math.min(1, ((x + 0.5 - ax) * vx + (z + 0.5 - az) * vz) / (vx * vx + vz * vz)));
        return Math.hypot(x + 0.5 - (ax + vx * t), z + 0.5 - (az + vz * t)) < 7;
    }

    private boolean freeTown(int x, int z, double minMargin) {
        int i = ix(x, z);
        if (i < 0 || !island[i] || mere[i] || course.kind[i] != CanalCourse.NONE || course.margin[i] < minMargin) return false;
        if (FINISH_ZONE.contains(x, 0, z) || inIsland(x, z) || sightCorridor(x, z)) return false;
        if (x >= -26 && x <= 25 && z >= -6 && z <= 12) return false;          // start lock surroundings
        return Math.abs(x) <= LIMIT - 2 && Math.abs(z) <= LIMIT - 2;
    }

    private void town(G g) {
        CanalCourse c = course;
        // row houses facing the canals: grid placement, footprint fully on free town at 3+ from the copings
        for (int gx = c.x1 + 2; gx <= c.x2 - 9; gx += 8)
            for (int gz = c.z1 + 2; gz <= c.z2 - 9; gz += 8) {
                int hx = gx + (int) (g.rnd(gx, 1, gz) * 2), hz = gz + (int) (g.rnd(gx, 2, gz) * 2);
                int w = 5 + (int) (g.rnd(gx, 3, gz) * 3), dp = 5 + (int) (g.rnd(gx, 4, gz) * 3);
                boolean ok = true;
                double mind = 1e9;
                for (int x = hx - 1; x <= hx + w + 1 && ok; x++)
                    for (int z = hz - 1; z <= hz + dp + 1 && ok; z++) {
                        if (!freeTown(x, z, 1.5)) ok = false;
                        else { mind = Math.min(mind, c.margin[ix(x, z)]); if (house[ix(x, z)]) ok = false; }
                    }
                if (!ok || mind > 22) continue;
                house(g, hx, hz, w, dp);
                for (int x = hx - 1; x <= hx + w + 1; x++) for (int z = hz - 1; z <= hz + dp + 1; z++) house[ix(x, z)] = true;
            }
        // snowy spruces in the open spaces of the town core (sparse)
        for (int k = 0; k < 160; k++) {
            int x = (int) (c.x1 + g.rnd(k, 5, 7) * (c.x2 - c.x1)), z = (int) (c.z1 + g.rnd(k, 6, 7) * (c.z2 - c.z1));
            if (!freeTown(x, z, 5) || house[ix(x, z)]) continue;
            boolean clear = true;
            for (int dx = -2; dx <= 2 && clear; dx++) for (int dz = -2; dz <= 2 && clear; dz++) { int j = ix(x + dx, z + dz); if (j < 0 || house[j] || !freeTown(x + dx, z + dz, 3)) clear = false; }
            if (!clear) continue;
            spruce(g, x, z, 5 + (int) (g.rnd(k, 8, 7) * 5));
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) house[ix(x + dx, z + dz)] = true;
        }
    }

    private void spruce(G g, int x, int z, int hgt) {
        for (int y = T + 1; y <= T + hgt; y++) g.set(x, y, z, "spruce_log");
        for (int y = T + 3; y <= T + hgt + 1; y++) {
            int r = Math.max(0, (T + hgt + 2 - y) / 2);
            r = Math.min(r, 2);
            for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++)
                if (Math.abs(dx) + Math.abs(dz) <= r + 1 && !(dx == 0 && dz == 0 && y <= T + hgt)) {
                    g.setIfAir(x + dx, y, z + dz, "spruce_leaves[persistent=true,distance=7]");
                    if (g.rnd(x + dx, y, z + dz) < 0.4) g.setIfAir(x + dx, y + 1, z + dz, "snow[layers=1]");
                }
        }
    }

    private void house(G g, int x0, int z0, int w, int dp) {
        int base = T + 1;
        int h = 5 + (int) (g.rnd(x0, 5, z0) * 6);
        double r0 = g.rnd(x0, 6, z0);
        String wall = r0 < 0.5 ? "bricks" : r0 < 0.7 ? "white_terracotta" : r0 < 0.85 ? "mud_bricks" : "light_gray_terracotta";
        String trim = r0 < 0.5 ? "stripped_spruce_log" : "stripped_dark_oak_log";
        boolean alongX = w >= dp;
        for (int x = x0; x <= x0 + w; x++)
            for (int z = z0; z <= z0 + dp; z++) {
                boolean edge = x == x0 || x == x0 + w || z == z0 || z == z0 + dp;
                if (!edge) continue;
                boolean corner = (x == x0 || x == x0 + w) && (z == z0 || z == z0 + dp);
                for (int y = base; y <= base + h; y++) {
                    String b = corner ? trim : wall;
                    int ly = y - base;
                    if (!corner && ly >= 1 && ly < h - 1 && ly % 3 != 0 && ((x + z) & 1) == 0) b = "yellow_stained_glass_pane";
                    if (ly == 0 && !corner) b = "stone_bricks";
                    g.set(x, y, z, b);
                }
            }
        g.set(x0 + w / 2, base + 2, z0 + dp / 2, "lantern[hanging=false]");
        g.set(x0 + w / 2, base + 1, z0 + dp / 2, "spruce_planks");
        // gable roof with snow
        int span = alongX ? dp : w;
        for (int i = 0; i <= span / 2 + 1; i++) {
            int ry = base + h + 1 + i;
            for (int a = (alongX ? x0 - 1 : z0 - 1); a <= (alongX ? x0 + w + 1 : z0 + dp + 1); a++) {
                int lo = (alongX ? z0 : x0) - 1 + i, hi = (alongX ? z0 + dp : x0 + w) + 1 - i;
                if (lo > hi) continue;
                for (int s : new int[]{lo, hi}) {
                    int x = alongX ? a : s, z = alongX ? s : a;
                    String facing = alongX ? (s == lo ? "south" : "north") : (s == lo ? "east" : "west");
                    g.set(x, ry, z, lo == hi ? "dark_oak_slab[type=bottom]" : "spruce_stairs[facing=" + facing + ",half=bottom,shape=straight]");
                    if (g.rnd(x, ry, z) < 0.7) g.setIfAir(x, ry + 1, z, "snow[layers=1]");
                }
                for (int s = lo + 1; s < hi; s++) {
                    int x = alongX ? a : s, z = alongX ? s : a;
                    if (a == (alongX ? x0 : z0) || a == (alongX ? x0 + w : z0 + dp)) g.set(x, ry, z, wall);
                }
            }
        }
        if (g.rnd(x0, 9, z0) < 0.7) {
            int cx = x0 + 1, cz = z0 + 1, top = base + h + span / 2 + 2;
            for (int y = base + h; y <= top; y++) g.set(cx, y, cz, "bricks");
            g.set(cx, top + 1, cz, "campfire[lit=true,signal_fire=false]");
        }
    }

    private void lampPost(G g, int x, int z) {
        for (int y = T + 1; y <= T + 3; y++) g.set(x, y, z, "dark_oak_fence");
        g.set(x, T + 4, z, "lantern[hanging=false]");
    }

    private void lamps(G g) {
        CanalCourse c = course;
        // lamp posts along the quays (behind the coping), every ~10 blocks of canal
        for (int x = c.x1; x <= c.x2; x++)
            for (int z = c.z1; z <= c.z2; z++) {
                int i = ix(x, z);
                if (c.kind[i] != CanalCourse.NONE || !island[i] || mere[i] || c.margin[i] < 2.2 || c.margin[i] > 3.2) continue;
                if (g.rnd(x, 9, z) > 0.07 || g.get(x, T + 1, z) != null || FINISH_ZONE.contains(x, 0, z)) continue;
                boolean near = false;
                for (int dx = -4; dx <= 4 && !near; dx++) for (int dz = -4; dz <= 4 && !near; dz++)
                    if (g.get(x + dx, T + 4, z + dz) != null && g.get(x + dx, T + 4, z + dz).contains("lantern")) near = true;
                if (near) continue;
                lampPost(g, x, z);
            }
        // mere: lanterns on the rim every few blocks
        for (int x = c.x1; x <= c.x2; x++)
            for (int z = c.z1; z <= c.z2; z++) {
                int i = ix(x, z);
                if (!island[i] || mere[i] || c.kind[i] != CanalCourse.NONE) continue;
                boolean rim = false;
                for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) { int j = ix(x + o[0], z + o[1]); if (j >= 0 && mere[j]) rim = true; }
                if (rim && (x * 7 + z * 3) % 11 == 0 && g.get(x, T + 1, z) == null && com.mcm.games.tgttos.maps.b.util.Bs.fullSolid(g.get(x, T, z))
                        && !JETTY.contains(x, T, z) && !EAST_BRIDGE.contains(x, T, z))
                    g.set(x, T + 1, z, "lantern[hanging=false]");
            }
    }
}
