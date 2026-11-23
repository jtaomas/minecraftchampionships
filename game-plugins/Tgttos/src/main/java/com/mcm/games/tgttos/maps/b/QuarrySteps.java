package com.mcm.games.tgttos.maps.b;

import com.mcm.games.tgttos.maps.b.util.Bs;
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

/**
 * Quarry Steps (push + bridge). Start on the near rim of an open sandstone quarry. Built line: a 4-wide haul road
 * cut into the left wall that steps down three benches (one collapsed 3-block gap, a boulder squeeze), runs along
 * the far wall 15 below the rim, then climbs out by the 3-wide "cut" staircase (choke) or the wide double ramp on
 * the right (longer). Own-block line: bridge 35 across the bowl and stair-bridge 6 up the far face, bending around
 * the hanging excavator bucket, a crane hook, a skip, a girder and a rock spire (>= 41 blocks of the 64).
 * Falling = the flooded quarry floor = respawn. Goal: site-office yard pen under a tower crane.
 *
 * <p>Local frame: start floor y = 0, strip x -18..17 / z 0..6, course towards +Z, far rim floor y = 5.
 */
public final class QuarrySteps implements TgMap {

    @Override public String id() { return "quarry_steps"; }
    @Override public String displayName() { return "Quarry Steps"; }
    @Override public MapType type() { return MapType.PUSH_BRIDGE; }
    @Override public Kit kit() { return Kit.CONCRETE_PICKAXE_SNOWBALLS; }

    // key geometry (also used by the validator tool)
    public static final int RIM_FAR = 5;           // far rim floor block y
    public static final int BENCH = -10;           // far-wall haul-road floor
    public static final int WATER = -21;
    public static final Region PEN = Region.of(-6, 6, 63, 5, 8, 74);

    @Override
    public TgMapData build(long seed) { return generate(seed).d; }

    // palettes
    private static final Pal SAND_HI = new Pal("sandstone", 50, "smooth_sandstone", 24, "cut_sandstone", 8, "chiseled_sandstone", 1);
    private static final Pal SAND_MID = new Pal("sandstone", 30, "smooth_sandstone", 20, "white_terracotta", 6, "terracotta", 3);
    private static final Pal GREY = new Pal("andesite", 30, "stone", 30, "tuff", 14, "cobblestone", 6, "polished_andesite", 4);
    private static final Pal DEEP = new Pal("stone", 30, "deepslate", 20, "tuff", 20, "cobbled_deepslate", 8);
    private static final Pal ROAD = new Pal("packed_mud", 40, "coarse_dirt", 30, "smooth_sandstone", 14, "mud_bricks", 4);
    private static final Pal YELLOW = new Pal("yellow_terracotta", 70, "yellow_wool", 20, "honeycomb_block", 6);
    private static final Pal STEEL = new Pal("iron_block", 30, "light_gray_terracotta", 30, "polished_andesite", 20, "gray_terracotta", 10);

    /** Rock by depth: banded sandstone over grey stone over dark deep rock. */
    static String rock(G g, int x, int y, int z) {
        double n = Noise.value2(x * 0.15, z * 0.15, g.seed + 5) * 3;
        double yy = y + n;
        if (yy > 1) return SAND_HI.at(x, y, z, g.seed);
        if (yy > -6) return ((int) Math.floor(yy)) % 3 == 0 ? GREY.at(x, y, z, g.seed) : SAND_MID.at(x, y, z, g.seed);
        if (yy > -15) return ((int) Math.floor(yy)) % 4 == 0 ? SAND_MID.at(x, y, z, g.seed) : GREY.at(x, y, z, g.seed);
        return DEEP.at(x, y, z, g.seed);
    }

    /** Terrain height of the scenery ground outside the bowl and the route areas. */
    static int height(G g, int x, int z) {
        double n = Noise.fbm2(x * 0.06, z * 0.06, g.seed + 11);
        int ax = Math.abs(x);
        if (z >= 99) return (int) Math.round(5 + Math.min(32, (z - 99) * 1.4) + n * 6);   // back face of the quarry
        if (z >= 49) {
            if (ax <= 40) return RIM_FAR;
            return (int) Math.round(RIM_FAR + 3 + (ax - 40) * 0.5 + n * 6);
        }
        if (x >= 20 && z >= 26 && x <= 40) return RIM_FAR;                                // right plateau
        if (z < 11 && ax <= 19) return 0;                                                 // apron
        if (z < -1 && ax <= 8) return 0;                                                  // site entrance road
        if (x <= -20) return (int) Math.round(4 + (ax - 20) * 0.35 + n * 5);             // left rim hill
        if (x >= 20) return (int) Math.round(9 + (ax - 20) * 0.3 + n * 5);               // right hill (crusher)
        return (int) Math.round(3 + n * 4);                                               // behind the start
    }

    public G generate(long seed) {
        TgMapData d = new TgMapData(id(), seed);
        G g = new G(d);

        terrain(g);
        bowl(g);
        haulRoad(g);
        startStrip(g);
        yard(g);
        g.scenery = true;
        excavator(g);
        cableways(g);
        crusher(g);
        crane(g);
        props(g);
        backdrop(g);
        g.scenery = false;

        d.voidY = -30;
        d.playBounds = Region.of(-40, -30, -1, 40, 70, 98);
        d.buildBounds = Region.of(-20, -20, 8, 20, 9, 66);
        d.hazardRegions.add(Region.of(-20, -40, 11, 20, -20, 48));        // flooded quarry floor
        d.hazardRegions.add(Region.of(-40, -40, -1, -21, 70, 48));        // left hill (out of the quarry)
        d.hazardRegions.add(Region.of(21, -40, -1, 40, 70, 25));          // right hill / crusher
        d.overviewPoints.add(new Spawn(34.5, 34, -6.5, 35f, 35f));
        d.overviewPoints.add(new Spawn(0.5, 30, 84.5, 180f, 30f));
        d.notes.put("safeLine", "haul road: R1 down the left wall, R2 along the far wall, up the 3-wide cut (choke) or the wide double ramp");
        d.notes.put("fastLine", "bridge 38 straight across + 6 up the far face, bending around 5 hanging sheet piles and a rock spire (>= 44 blocks of 64)");
        g.finish();
        return g;
    }

    // ------------------------------------------------------------------ terrain

    private void terrain(G g) {
        g.scenery = true;
        int[][] h = new int[129][149];
        for (int x = -64; x <= 64; x++)
            for (int z = -24; z <= 124; z++) h[x + 64][z + 24] = height(g, x, z);
        for (int x = -64; x <= 64; x++)
            for (int z = -24; z <= 124; z++) {
                if (inBowl(x, z)) continue;
                // rounded island outline: fade out the far corners
                double ex = x / 66.0, ez = (z - 50) / 76.0;
                if (ex * ex + ez * ez > 1.0 && Math.abs(x) > 20) continue;
                int top = h[x + 64][z + 24];
                int low = top;
                boolean edge = false;
                for (int[] dd : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + dd[0], nz = z + dd[1];
                    if (nx < -64 || nx > 64 || nz < -24 || nz > 124) { edge = true; continue; }
                    double fx = nx / 66.0, fz = (nz - 50) / 76.0;
                    if (fx * fx + fz * fz > 1.0 && Math.abs(nx) > 20) { edge = true; continue; }
                    if (inBowl(nx, nz)) { low = Math.min(low, -24); continue; }
                    low = Math.min(low, h[nx + 64][nz + 24]);
                }
                int bottom = edge ? -26 - (int) (g.rnd(x, 0, z) * 8) : Math.min(top - 3, low - 1);
                boolean route = (z <= 10 && Math.abs(x) <= 19) || (z >= 49 && z <= 98 && Math.abs(x) <= 40) || (x >= 20 && x <= 40 && z >= 26 && z <= 52);
                g.scenery = !route;
                for (int y = bottom; y <= top; y++) g.set(x, y, z, rock(g, x, y, z));
                // top dressing
                if (route) g.set(x, top, z, ROAD.at(x, top, z, g.seed));
                else if (top > RIM_FAR + 1 || x < -20 || x > 20) {
                    double r = g.rnd(x, top, z, 3);
                    g.set(x, top, z, r < 0.35 ? "coarse_dirt" : r < 0.55 ? "sandstone" : r < 0.75 ? "grass_block" : r < 0.85 ? "rooted_dirt" : "smooth_sandstone");
                    double p = g.rnd(x, top + 1, z, 4);
                    if (g.get(x, top, z).contains("grass") && p < 0.3) g.set(x, top + 1, z, p < 0.1 ? "fern" : "short_grass");
                    else if (p > 0.97) g.set(x, top + 1, z, "dead_bush");
                }
            }
        g.scenery = false;
    }

    static boolean inBowl(int x, int z) { return x >= -19 && x <= 19 && z >= 11 && z <= 48; }

    private void bowl(G g) {
        // bowl floor and water
        g.scenery = true;
        for (int x = -19; x <= 19; x++)
            for (int z = 11; z <= 48; z++) {
                for (int y = -26; y <= WATER - 1; y++) g.set(x, y, z, DEEP.at(x, y, z, g.seed));
                g.set(x, WATER, z, "water");
            }
        // near face (z = 10) and right face (x = 20) already come from the terrain; texture the far face
        // boulders in the water, half submerged
        for (int i = 0; i < 9; i++) {
            double bx = -12 + g.rnd(i, 1, 2) * 26, bz = 14 + g.rnd(i, 2, 3) * 22;
            g.ellipsoid(bx, WATER, bz, 1.5 + g.rnd(i, 3, 4) * 1.5, 1.6, 1.5 + g.rnd(i, 4, 5), GREY);
        }
        // rock spire left standing in the bowl (obstacle for the straight bridge)
        for (int y = WATER - 1; y <= 15; y++) {
            double r = y <= 12 ? 2.4 : 2.4 - (y - 12) * 0.6;
            g.cyl(11.5, 37.5, r, y, y, y > 0 ? SAND_HI : y > -10 ? SAND_MID : GREY);
        }
        g.set(11, 16, 37, "dead_bush");
        g.scenery = false;
    }

    // ------------------------------------------------------------------ haul road

    private void shelf(G g, int x1, int x2, int z1, int z2, int floor) {
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                for (int y = WATER - 1; y < floor; y++) g.set(x, y, z, rock(g, x, y, z));
                g.set(x, floor, z, ROAD.at(x, floor, z, g.seed));
                for (int y = floor + 1; y <= floor + 4; y++) g.set(x, y, z, null);
            }
    }

    private void haulRoad(G g) {
        // left wall face above the road (x = -20 column is terrain); make sure it stands tall enough
        for (int z = 11; z <= 48; z++)
            for (int y = WATER - 1; y <= 4; y++) g.set(-20, y, z, rock(g, -20, y, z));
        // R1 down the left wall (x -19..-16)
        shelf(g, -19, -16, 11, 14, 0);
        shelf(g, -19, -16, 18, 20, -3);
        Stairs.flight(g, "R1a", "sandstone", "sandstone", -19, 17, -3, 0, -1, 3, 4, WATER - 1);
        // collapsed section z 21..23 (3-block gap), rubble far below
        shelf(g, -19, -16, 24, 28, -3);
        Stairs.flight(g, "R1b", "sandstone", "sandstone", -19, 31, -6, 0, -1, 3, 4, WATER - 1);
        shelf(g, -19, -16, 32, 35, -6);
        // boulder squeeze: only x -19..-18 stays open over z 33..34
        g.scenery = true;
        g.ellipsoid(-15.8, -4.5, 34.0, 1.6, 2.0, 1.1, GREY);
        g.scenery = false;
        for (int z = 33; z <= 35; z++) for (int x = -19; x <= -18; x++) for (int y = -5; y <= -3; y++) g.set(x, y, z, null);
        Stairs.flight(g, "R1c", "sandstone", "sandstone", -19, 39, BENCH, 0, -1, 4, 4, WATER - 1);
        shelf(g, -19, -16, 40, 48, BENCH);
        // R2 along the far wall (z 42..45), broken once at x -6..-4 (3-block gap), rubble pile at x 1..2
        shelf(g, -15, -7, 45, 48, BENCH);
        shelf(g, -3, 11, 45, 48, BENCH);
        shelf(g, 14, 19, 45, 48, BENCH);
        g.scenery = true;
        g.ellipsoid(1.5, BENCH + 1, 46.5, 1.2, 1.4, 1.2, GREY);
        g.scenery = false;
        for (int x = 0; x <= 3; x++) g.set(x, BENCH + 1, 48, null);
        for (int x = 0; x <= 3; x++) g.set(x, BENCH + 2, 48, null);
        // far wall face (z = 46..47) from the water up to the rim
        for (int x = -19; x <= 19; x++)
            for (int y = WATER - 1; y <= RIM_FAR; y++) { g.set(x, y, 49, rock(g, x, y, 49)); g.set(x, y, 50, rock(g, x, y, 50)); }
        // the cut: 3-wide straight staircase from the end of R2 up through the right wall (choke), z 45..47
        for (int x = 20; x <= 35; x++)
            for (int z = 45; z <= 47; z++)
                for (int y = x == 35 ? RIM_FAR + 1 : BENCH + 1; y <= RIM_FAR + 6; y++) g.set(x, y, z, null);
        for (int x = 20; x <= 34; x++)
            for (int y = BENCH; y <= RIM_FAR; y++) { g.set(x, y, 44, rock(g, x, y, 44)); g.set(x, y, 48, rock(g, x, y, 48)); }
        Stairs.flight(g, "cut", "smooth_sandstone", "sandstone", 20, 45, BENCH, 1, 0, 15, 3, BENCH - 2);
        // wide ramp: flight D up -z on the right shelf, landing pad, flight E up +x into the plateau
        shelf(g, 15, 19, 32, 36, -2);
        Stairs.flight(g, "rampD", "sandstone", "sandstone", 15, 44, BENCH, 0, -1, 8, 5, WATER - 1);
        for (int x = 20; x <= 27; x++)
            for (int z = 32; z <= 36; z++)
                for (int y = x == 27 ? RIM_FAR + 1 : -1; y <= RIM_FAR + 6; y++) g.set(x, y, z, null);
        Stairs.flight(g, "rampE", "sandstone", "sandstone", 20, 32, -2, 1, 0, 7, 5, -4);
        for (int x = 20; x <= 26; x++) { for (int y = -3; y <= RIM_FAR + 1; y++) { g.set(x, y, 31, rock(g, x, y, 31)); g.set(x, y, 37, rock(g, x, y, 37)); } }
        // parapet blocks along the inner edges of the shelves would block falls; instead mark edges with stripes
        for (int z = 11; z <= 48; z++) {
            int f = roadFloor(z);
            if (f != Integer.MIN_VALUE && g.get(-16, f, z) != null && Bs.id(g.get(-16, f, z)).equals("packed_mud"))
                g.set(-16, f, z, (z & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
        }
    }

    static int roadFloor(int z) {
        if (z <= 14) return 0;
        if (z >= 18 && z <= 28) return -3;
        if (z >= 32 && z <= 35) return -6;
        if (z >= 40) return BENCH;
        return Integer.MIN_VALUE;
    }

    // ------------------------------------------------------------------ start

    private void startStrip(G g) {
        TgMapData d = g.d;
        Pal floor = new Pal("polished_blackstone", 50, "polished_deepslate", 30, "smooth_basalt", 20);
        for (int x = -19; x <= 18; x++)
            for (int z = -1; z <= 6; z++) {
                for (int y = -3; y <= -1; y++) g.set(x, y, z, rock(g, x, y, z));
                g.set(x, 0, z, floor.at(x, 0, z, g.seed));
                for (int y = 1; y <= 5; y++) g.set(x, y, z, null);
            }
        // start line: hazard stripes along z = 6, edge trim
        for (int x = -18; x <= 17; x++) g.set(x, 0, 6, ((x + 40) / 2) % 2 == 0 ? "yellow_terracotta" : "black_terracotta");
        for (int z = -1; z <= 6; z++) { g.set(-19, 0, z, "chiseled_polished_blackstone"); g.set(18, 0, z, "chiseled_polished_blackstone"); }
        // glass pen around the strip; front row is the barrier removed at GO
        for (int y = 1; y <= 3; y++) {
            for (int x = -19; x <= 18; x++) g.set(x, y, -1, "glass");
            for (int z = -1; z <= 7; z++) { g.set(-19, y, z, "glass"); g.set(18, y, z, "glass"); }
            for (int x = -18; x <= 17; x++) { g.set(x, y, 7, "glass"); d.startBarrier.add(new TgMapData.Pos(x, y, 7)); }
        }
        d.startStrip = Region.of(-18, 1, 0, 17, 4, 6);
        g.startSpawns(-18, 17, 0, 6, 1, 15, 3);
        // apron edge marker (no railing: you bridge off it)
        for (int x = -15; x <= 19; x++) g.set(x, 0, 10, (x & 1) == 0 ? "smooth_sandstone" : "cut_sandstone");
    }

    // ------------------------------------------------------------------ yard and pen

    private void yard(G g) {
        TgMapData d = g.d;
        int y = RIM_FAR;
        // pen: low wall ring with stripes; two gaps on the near side
        for (int x = -7; x <= 6; x++)
            for (int z = 62; z <= 75; z++) {
                boolean ring = x == -7 || x == 6 || z == 62 || z == 75;
                g.set(x, y, z, ring ? "polished_andesite" : ((x + z) & 1) == 0 ? "smooth_sandstone" : "sandstone");
                if (ring) g.set(x, y + 1, z, ((x + z) & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
            }
        for (int x = -3; x <= -2; x++) g.set(x, y + 1, 62, null);
        for (int x = 1; x <= 2; x++) g.set(x, y + 1, 62, null);
        for (int z = 66; z <= 67; z++) g.set(6, y + 1, z, null);
        d.pen = Region.of(-6, y + 1, 63, 5, y + 3, 74);
        g.chickens(d.pen, 12);
        // hay and troughs inside the pen
        g.set(-5, y + 1, 73, "hay_block");
        g.set(-4, y + 1, 73, "hay_block[axis=x]");
        g.set(4, y + 1, 73, "composter[level=3]");
        // obstacle field between the rim edge and the pen: stone block stacks, pipe racks, a barrier line
        g.scenery = true;
        // block storage yard: jittered stacks of quarried blocks (no straight sprint anywhere in the yard)
        for (int gz = 52; gz <= 94; gz += 5)
            for (int gx = -38; gx <= 36; gx += 5) {
                int x = gx + (int) (g.rnd(gx, 7, gz) * 2.99), z = gz + (int) (g.rnd(gx, 8, gz) * 2.99);
                int w = 2 + (int) (g.rnd(gx, 9, gz) * 1.99), dz = 2 + (int) (g.rnd(gx, 10, gz) * 1.99);
                if (x + w >= -8 && x <= 7 && z + dz >= 61 && z <= 76) continue;      // pen ring + margin
                if (x + w >= 12 && x <= 18 && z + dz >= 76 && z <= 82) continue;     // crane base
                stack(g, x, y, z, w, dz);
            }
        for (int gz = 26; gz <= 50; gz += 5)
            for (int gx = 21; gx <= 37; gx += 5) {
                int x = gx + (int) (g.rnd(gx, 7, gz) * 2.99), z = gz + (int) (g.rnd(gx, 8, gz) * 2.99);
                int w = 2 + (int) (g.rnd(gx, 9, gz) * 1.99), dz = 2 + (int) (g.rnd(gx, 10, gz) * 1.99);
                if (z + dz >= 30 && z <= 38 && x <= 30) continue;                     // ramp trench + landing
                if (z + dz >= 43 && z <= 49 && x <= 37) continue;                     // cut trench + exit
                stack(g, x, y, z, w, dz);
            }
        stack(g, 8, y, 70, 3, 2);
        stack(g, -11, y, 66, 2, 3);
        // pipe racks (stacked logs) along the yard sides
        for (int z = 51; z <= 73; z += 11) {
            g.fill(-24, y + 1, z, -20, y + 1, z + 1, "stripped_spruce_log[axis=x]");
            g.fill(-23, y + 2, z, -21, y + 2, z + 1, "stripped_spruce_log[axis=x]");
        }
        g.scenery = false;
    }

    private void stack(G g, int x, int y, int z, int w, int dz) {
        for (int i = 0; i < w; i++) for (int j = 0; j < dz; j++) if (g.get(x + i, y + 1, z + j) != null || Math.abs(x + i) > 40 || z + j > 98) return;
        int hgt = 2 + (int) (g.rnd(x, 1, z) * 2.2);
        Pal m = g.rnd(x, 2, z) < 0.5 ? new Pal("smooth_stone", 3, "stone", 1) : g.rnd(x, 3, z) < 0.5 ? new Pal("stone_bricks", 3, "cracked_stone_bricks", 1) : new Pal("cut_sandstone", 3, "smooth_sandstone", 1);
        g.fill(x, y + 1, z, x + w - 1, y + hgt, z + dz - 1, m);
        if (g.rnd(x, 4, z) < 0.5) g.set(x + (int) (g.rnd(x, 5, z) * w), y + hgt + 1, z, "smooth_stone_slab[type=bottom]");
        if (g.rnd(x, 6, z) < 0.25) g.set(x, y + hgt + 1, z + dz - 1, "lantern[hanging=false]");
    }

    // ------------------------------------------------------------------ machinery

    private void excavator(G g) {
        // tracked excavator on the left rim pad, boom over the bowl to the hanging bucket
        int py = 7;
        for (int x = -36; x <= -24; x++)
            for (int z = 17; z <= 31; z++) {
                for (int y = py - 6; y < py; y++) g.set(x, y, z, rock(g, x, y, z));
                g.set(x, py, z, ROAD.at(x, py, z, g.seed));
                for (int y = py + 1; y <= py + 14; y++) g.set(x, y, z, null);
            }
        for (int x = -34; x <= -26; x++) {
            for (int z : new int[]{18, 19, 28, 29}) {
                g.set(x, py + 1, z, "black_wool");
                g.set(x, py + 2, z, (x & 1) == 0 ? "gray_wool" : "black_wool");
            }
        }
        g.fill(-34, py + 3, 18, -26, py + 3, 29, "gray_terracotta");
        g.fill(-35, py + 4, 19, -25, py + 8, 28, YELLOW);
        g.fill(-34, py + 9, 20, -26, py + 9, 27, "yellow_terracotta");
        // cab with windows facing the bowl
        g.fill(-27, py + 9, 21, -24, py + 12, 25, YELLOW);
        g.fill(-24, py + 10, 22, -24, py + 11, 24, "light_gray_stained_glass");
        g.fill(-26, py + 10, 21, -25, py + 11, 21, "light_gray_stained_glass");
        g.fill(-27, py + 13, 21, -24, py + 13, 25, "black_terracotta");
        // counterweight + stripes
        g.fill(-36, py + 4, 20, -36, py + 7, 27, "gray_terracotta");
        for (int z = 20; z <= 27; z++) g.set(-36, py + 6, z, (z & 1) == 0 ? "black_terracotta" : "yellow_terracotta");
        g.set(-30, py + 10, 24, "lantern[hanging=false]");
        // boom: two-block-thick line up and over the road to the tip, arm down to the bucket
        double[] a = {-24, py + 9, 24}, b = {-12, 27, 26}, c = {-3, 21, 27};
        for (int k = 0; k < 2; k++) for (int j = 0; j < 2; j++) {
            g.line(a[0], a[1] + k, a[2] + j, b[0], b[1] + k, b[2] + j, "yellow_terracotta");
            g.line(b[0], b[1] + k, b[2] + j, c[0], c[1] + k, c[2] + j, "yellow_terracotta");
        }
        g.line(a[0], a[1] + 2, a[2] + 1, b[0], b[1] + 2, b[2] + 1, "black_terracotta");
        g.tether(-14, 20, 25, -20, py + 14, 25, "chain");
        // bucket raised high over the left half of the bowl (overhead scenery, not a stepping stone)
        int bx1 = -6, bx2 = 0, bz1 = 25, bz2 = 29, by1 = 15, by2 = 20;
        for (int x = bx1; x <= bx2; x++)
            for (int z = bz1; z <= bz2; z++)
                for (int y = by1; y <= by2; y++) {
                    boolean shell = x == bx1 || x == bx2 || z == bz1 || z == bz2 || y == by1;
                    if (!shell) continue;
                    if (y == by1 && (x == bx1 || x == bx2) && (z == bz1 || z == bz2)) continue;
                    g.set(x, y, z, y == by2 ? "yellow_terracotta" : STEEL.at(x, y, z, g.seed));
                }
        for (int x = bx1 + 1; x <= bx2 - 1; x += 2) g.set(x, by1 - 1, bz2, "pointed_dripstone[thickness=tip,vertical_direction=down]");
        for (int x = bx1 + 1; x < bx2; x++) for (int z = bz1 + 1; z < bz2; z++) g.set(x, by1 + 1, z, g.rnd(x, 1, z) < 0.6 ? "sandstone" : "andesite");
    }

    private void tower(G g, int x, int z, int top) {
        for (int y = height(g, x, z); y <= top; y++) {
            g.set(x, y, z, "spruce_log");
            g.set(x + 1, y, z, (y % 4 == 0) ? "spruce_log" : "spruce_fence");
            g.set(x, y, z + 1, (y % 4 == 0) ? "spruce_log" : "spruce_fence");
            g.set(x + 1, y, z + 1, "spruce_log");
        }
        g.fill(x - 1, top + 1, z - 1, x + 2, top + 1, z + 2, "spruce_slab[type=bottom]");
        g.set(x, top + 2, z, "sea_lantern");
        g.set(x + 1, top + 2, z + 1, "sea_lantern");
    }

    /** Hanging steel sheet piles (tall plates) in the gap: they bend the straight bridge, their tops are too high to use. */
    static final int[][] PLATES = {{-12, 17, -7}, {-3, 24, 3}, {4, 31, 9}, {-9, 38, -3}, {-1, 44, 6}};
    static final int PLATE_LO = -6, PLATE_HI = 12, CABLE = 22;

    private void cableways(G g) {
        for (int[] c : new int[][]{{-27, 8}, {-27, 47}, {25, 8}, {25, 47}}) tower(g, c[0], c[1], CABLE);
        for (int z = 9; z <= 47; z++) { g.set(-25, CABLE, z, "chain[axis=z]"); g.set(24, CABLE, z, "chain[axis=z]"); }
        for (int[] pl : PLATES) {
            int x1 = pl[0], z = pl[1], x2 = pl[2];
            for (int x = -24; x <= 23; x++) g.set(x, CABLE, z, "chain[axis=x]");
            for (int x = x1; x <= x2; x++)
                for (int y = PLATE_LO; y <= PLATE_HI; y++) {
                    String b = y == PLATE_HI || y == PLATE_LO ? "yellow_terracotta"
                            : ((x + y) & 3) == 0 ? "black_terracotta" : ((x + y) & 3) == 2 ? "yellow_terracotta" : "iron_block";
                    if (x == x1 || x == x2) b = "gray_terracotta";
                    g.set(x, y, z, b);
                }
            for (int y = PLATE_HI + 1; y < CABLE; y++) { g.set(x1 + 1, y, z, "chain[axis=y]"); g.set(x2 - 1, y, z, "chain[axis=y]"); }
            g.set(x1, PLATE_HI + 1, z, "lightning_rod[facing=up]");
            g.set(x2, PLATE_HI + 1, z, "lightning_rod[facing=up]");
        }
    }

    private void crusher(G g) {
        // conveyor from the quarry floor up to a crusher shed on the right hill
        double[] a = {13, WATER + 1, 16}, b = {34, 20, 16};
        for (int k = 0; k < 3; k++) {
            g.line(a[0], a[1], a[2] + k, b[0], b[1], b[2] + k, "gray_terracotta");
            g.line(a[0], a[1] + 1, a[2] + k, b[0], b[1] + 1, b[2] + k, k == 1 ? "black_wool" : "iron_bars");
        }
        for (int x = 16; x <= 32; x += 4) {
            int topY = (int) (a[1] + (x - a[0]) / (b[0] - a[0]) * (b[1] - a[1]));
            for (int y = Math.max(WATER, x > 19 ? height(g, x, 17) : WATER); y < topY; y++) { g.set(x, y, 16, "iron_bars"); g.set(x, y, 18, "iron_bars"); }
        }
        // crusher shed
        int hy = height(g, 38, 12);
        g.fill(33, hy, 6, 46, hy + 9, 22, new Pal("light_gray_terracotta", 40, "gray_terracotta", 20, "smooth_stone", 20));
        g.clear(34, hy + 1, 7, 45, hy + 8, 21);
        g.fill(33, hy + 10, 5, 47, hy + 10, 23, "dark_oak_slab[type=bottom]");
        for (int z = 8; z <= 20; z += 3) g.fill(33, hy + 5, z, 33, hy + 6, z + 1, "light_gray_stained_glass");
        g.fill(33, hy + 1, 15, 33, hy + 4, 18, "black_terracotta");
        g.fill(40, hy + 11, 10, 41, hy + 16, 11, "bricks");
        g.set(40, hy + 17, 10, "campfire[lit=true,signal_fire=true]");
        // stockpiles
        for (int i = 0; i < 5; i++) {
            double cx = 26 + g.rnd(i, 7, 1) * 30, cz = -10 + g.rnd(i, 8, 2) * 12;
            int base = height(g, (int) cx, (int) cz);
            for (int y = 0; y < 6; y++) g.cyl(cx, cz, 5 - y * 0.8, base + y, base + y, i % 2 == 0 ? new Pal("gravel", 1) : new Pal("sand", 3, "sandstone", 1));
        }
    }

    private void crane(G g) {
        int y0 = RIM_FAR + 1, top = 48;
        int tx = 14, tz = 78;
        // lattice mast
        for (int y = y0; y <= top; y++)
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++) {
                    boolean corner = (i != 1) && (j != 1);
                    if (corner) g.set(tx + i, y, tz + j, "yellow_terracotta");
                    else if ((y + i + j) % 3 == 0 && !(i == 1 && j == 1)) g.set(tx + i, y, tz + j, "iron_bars");
                }
        g.fill(tx - 1, y0 - 1, tz - 1, tx + 3, y0 - 1, tz + 3, "polished_andesite");
        // cab and slewing ring
        g.fill(tx - 1, top + 1, tz - 1, tx + 3, top + 1, tz + 3, "gray_terracotta");
        g.fill(tx + 3, top + 2, tz, tx + 4, top + 4, tz + 2, YELLOW);
        g.fill(tx + 4, top + 3, tz, tx + 4, top + 3, tz + 2, "light_gray_stained_glass");
        // jib over the pen (towards -x) and counter-jib with weights (towards +x)
        for (int x = tx - 30; x <= tx + 12; x++) {
            boolean jib = x <= tx;
            g.set(x, top + 2, tz + 1, (x & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
            if (jib && (x % 3 == 0)) g.set(x, top + 3, tz + 1, "iron_bars");
        }
        g.fill(tx + 9, top - 1, tz, tx + 12, top + 1, tz + 2, "smooth_stone");
        g.fill(tx, top + 3, tz + 1, tx + 2, top + 7, tz + 1, "yellow_terracotta");
        g.line(tx + 1, top + 7, tz + 1, tx - 28, top + 3, tz + 1, "chain[axis=x]");
        g.line(tx + 1, top + 7, tz + 1, tx + 11, top + 3, tz + 1, "chain[axis=x]");
        // hook over the pen with a big flag
        for (int y = 22; y <= top + 1; y++) g.set(-1, y, tz + 1, "chain[axis=y]");
        g.fill(-2, 20, tz + 1, 0, 21, tz + 1, "iron_block");
        g.set(-1, 19, tz + 1, "iron_bars");
        // MCM pennant on top of the cab
        for (int y = top + 8; y <= top + 14; y++) g.set(tx + 1, y, tz + 1, "end_rod[facing=up]");
        for (int i = 0; i < 6; i++)
            for (int y = top + 9 + i / 2; y <= top + 14 - i / 2; y++) g.set(tx + 2 + i, y, tz + 1, i % 2 == 0 ? "red_wool" : "white_wool");
        g.text("MCM", tx - 22, top + 1, tz + 2, 1, 0, 1, "black_wool");
        g.landmark.add(new double[]{tx + 1.5, top + 12, tz + 1.5});
        g.landmark.add(new double[]{tx + 1.5, top + 1, tz + 1.5});
        // floodlights at the pen corners
        for (int[] c : new int[][]{{-9, 61}, {8, 61}, {-9, 77}, {8, 77}}) {
            for (int y = y0; y <= y0 + 6; y++) g.set(c[0], y, c[1], "iron_bars");
            g.set(c[0], y0 + 7, c[1], "sea_lantern");
            g.set(c[0], y0 + 8, c[1], "daylight_detector[inverted=false,power=0]");
        }
    }

    private void props(G g) {
        // site offices (portacabins) along the yard edges
        int y = RIM_FAR + 1;
        int[][] cabins = {{-30, 55}, {-30, 67}, {24, 67}, {24, 81}, {-22, 83}};
        for (int[] c : cabins) {
            g.fill(c[0], y, c[1], c[0] + 7, y + 3, c[1] + 4, "white_terracotta");
            g.clear(c[0] + 1, y, c[1] + 1, c[0] + 6, y + 2, c[1] + 3);
            g.fill(c[0], y + 4, c[1], c[0] + 7, y + 4, c[1] + 4, "light_gray_terracotta");
            g.fill(c[0] + 2, y + 1, c[1], c[0] + 3, y + 2, c[1], "light_blue_stained_glass_pane");
            g.fill(c[0] + 5, y + 1, c[1], c[0] + 6, y + 2, c[1], "light_blue_stained_glass_pane");
            g.fill(c[0] + 2, y + 1, c[1] + 4, c[0] + 5, y + 2, c[1] + 4, "light_blue_stained_glass_pane");
            g.fill(c[0], y, c[1], c[0] + 7, y, c[1] + 4, "gray_terracotta");
            g.set(c[0] + 1, y + 1, c[1] + 2, "barrel[facing=up,open=false]");
            g.set(c[0] + 3, y + 3, c[1] + 2, "lantern[hanging=true]");
        }
        // fences + lanterns along the scenery rim edges (never on the route)
        for (int z = 12; z <= 47; z += 1) {
            int hx = height(g, -21, z);
            g.set(-21, hx + 1, z, "spruce_fence");
            if (z % 8 == 4) { g.set(-21, hx + 2, z, "spruce_fence"); g.set(-21, hx + 3, z, "lantern[hanging=false]"); }
        }
        for (int z = 11; z <= 25; z++) {
            int hx = height(g, 21, z);
            g.set(21, hx + 1, z, "spruce_fence");
            if (z % 8 == 4) { g.set(21, hx + 2, z, "spruce_fence"); g.set(21, hx + 3, z, "lantern[hanging=false]"); }
        }
        // dump truck on the entrance road behind the start
        int tz = -14;
        g.fill(-4, 1, tz - 3, 2, 1, tz + 3, "black_wool");
        g.fill(-4, 2, tz - 3, 2, 4, tz - 1, YELLOW);
        g.fill(-3, 3, tz - 2, 1, 4, tz - 2, "gravel");
        g.fill(-4, 2, tz + 1, 2, 5, tz + 3, YELLOW);
        g.fill(-3, 4, tz + 3, 1, 4, tz + 3, "light_gray_stained_glass");
        for (int x : new int[]{-5, 3}) for (int z : new int[]{tz - 2, tz + 2}) g.set(x, 1, z, "black_wool");
        // site sign over the entrance: QUARRY
        for (int yy = 1; yy <= 17; yy++) { g.set(-13, yy, -6, "spruce_log"); g.set(13, yy, -6, "spruce_log"); }
        g.fill(-12, 10, -6, 12, 10, -6, "spruce_planks");
        g.fill(-12, 11, -6, 12, 17, -6, "black_wool");
        g.text("QUARRY", -11, 16, -6, 1, 0, 1, "yellow_wool");
        g.fill(-12, 18, -6, 12, 18, -6, "spruce_slab[type=bottom]");
        // small spruce trees on the outer hills
        for (int i = 0; i < 40; i++) {
            int x = (int) (-60 + g.rnd(i, 1, 9) * 120), z = (int) (-20 + g.rnd(i, 2, 9) * 140);
            if (Math.abs(x) < 44 && z > -4 && z < 100) continue;
            double ex = x / 66.0, ez = (z - 50) / 76.0;
            if (ex * ex + ez * ez > 0.9) continue;
            int hy = height(g, x, z);
            int hgt = 4 + (int) (g.rnd(i, 3, 9) * 4);
            for (int yy = 1; yy <= hgt; yy++) g.set(x, hy + yy, z, "spruce_log");
            for (int yy = 2; yy <= hgt + 1; yy++) {
                int r = Math.max(0, (hgt + 2 - yy) / 2);
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= r + 1 && !(dx == 0 && dz == 0 && yy <= hgt)) g.setIfAir(x + dx, hy + yy, z + dz, "spruce_leaves");
            }
        }
    }

    private void backdrop(G g) {
        // strata stripes and a lit tunnel mouth on the back face of the quarry
        for (int x = -8; x <= 8; x++)
            for (int y = RIM_FAR + 1; y <= RIM_FAR + 7; y++) {
                int z = 99 + (int) Math.ceil((y - RIM_FAR) / 1.4);
                boolean arch = Math.abs(x) <= 4 && (y <= RIM_FAR + 4 || Math.abs(x) <= 3 && y <= RIM_FAR + 5);
                if (arch) for (int k = 0; k < 6; k++) g.set(x, y, z + k, null);
                else if (Math.abs(x) <= 5) g.set(x, y, z, "stripped_dark_oak_log[axis=y]");
            }
        g.fill(-5, RIM_FAR + 6, 100, 5, RIM_FAR + 6, 100, "dark_oak_planks");
        g.set(-4, RIM_FAR + 4, 101, "lantern[hanging=false]");
        g.set(4, RIM_FAR + 4, 101, "lantern[hanging=false]");
        for (int x = -3; x <= 3; x++) g.set(x, RIM_FAR + 1, 99, "rail[shape=north_south]");
    }
}
