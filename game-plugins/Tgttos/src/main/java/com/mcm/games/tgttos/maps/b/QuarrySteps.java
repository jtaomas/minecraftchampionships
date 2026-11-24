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

import java.util.List;

/**
 * Quarry Steps (push + bridge), v2: a bigger open sandstone quarry (bowl 55 x 52) and a deliberately cluttered,
 * confusing works yard on top.
 *
 * <p>Built line: a 4-wide haul road cut into the left wall steps down three benches (collapsed 3-block gap, boulder
 * squeeze), runs along the far wall 15 below the rim (one 3-block gap, a 2-wide narrow ledge), then climbs out by
 * the 3-wide "cut" staircase (choke) or the wide double ramp on the right. Own-block line: bridge ~52 straight
 * across the open bowl (no cover, nothing under it but water 21 below) and stair-bridge 5 up the far face, bending
 * around five hanging sheet piles and a rock spire. Blocks regenerate, so the bridge is never short of blocks; it
 * is long and exposed instead, and rivals on the apron, the haul road and the rim can snowball or break it.
 *
 * <p>The yard: block stacks, crates, portacabins with open doors, scaffolds with a ladder and nothing on top, a
 * raised loading dock (dead end), parked machinery, a drill rig and four fake pens built exactly like the real one
 * (striped ring, hay, floodlights; the tower-crane hook hangs over the first fake). Fake pens hold unnamed decoy
 * chickens ({@link #decoyChickens()}); only the real goal chickens carry the "Punch me!" name tag. The real pen
 * sits behind a row of cabins at the back of the yard.
 *
 * <p>Local frame: start floor y = 0, strip x -18..17 / z 0..6, course towards +Z, far rim floor y = 5.
 */
public final class QuarrySteps implements TgMap {

    @Override public String id() { return "quarry_steps"; }
    @Override public String displayName() { return "Quarry Steps"; }
    @Override public MapType type() { return MapType.PUSH_BRIDGE; }
    @Override public Kit kit() { return Kit.CONCRETE_PICKAXE_SNOWBALLS; }

    // key geometry (also used by the validator tool)
    public static final int RIM_FAR = 5;           // far rim / yard floor block y
    public static final int BENCH = -10;           // far-wall haul-road floor
    public static final int WATER = -21;
    public static final int BX = 27;               // bowl half width (x -27..27)
    public static final int BZ1 = 11, BZ2 = 62;    // bowl z range; the far rim starts at z 63
    public static final Region PEN = Region.of(-6, RIM_FAR + 1, 101, 5, RIM_FAR + 3, 112);
    /** Pen rings {x1, z1, x2, z2}: index 0 is the real pen, 1..4 are the fakes. */
    public static final int[][] PENS = {{-7, 100, 6, 113}, {4, 70, 15, 79}, {-44, 86, -33, 95}, {42, 44, 53, 53}, {28, 92, 39, 101}};
    /** Scaffold with a ladder and a dead-end deck at y 10 (one decoy sits up there). */
    static final int[] SCAFFOLD_A = {-27, 109, -21, 114}, SCAFFOLD_B = {47, 64, 52, 69};
    static final int DECK = RIM_FAR + 5;

    /** Unnamed decoy chickens in the fake pens and on the scaffold deck (feet blocks). */
    public static final List<int[]> DECOYS = List.of(
            new int[]{8, RIM_FAR + 1, 73}, new int[]{12, RIM_FAR + 1, 77},
            new int[]{-40, RIM_FAR + 1, 89}, new int[]{-36, RIM_FAR + 1, 92},
            new int[]{45, RIM_FAR + 1, 47}, new int[]{50, RIM_FAR + 1, 50},
            new int[]{31, RIM_FAR + 1, 95}, new int[]{36, RIM_FAR + 1, 98},
            new int[]{-23, DECK + 1, 111});

    @Override public List<int[]> decoyChickens() { return DECOYS; }

    /** Portacabins {x, z} (8 x 5, door in the -z wall). */
    static final int[][] CABINS = {{-24, 68}, {-14, 88}, {-3, 89}, {18, 86}, {-30, 120}, {22, 106}, {40, 113}, {-52, 70}, {-12, 124}, {44, 96}};

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

    static boolean yard(int x, int z) { return (z >= BZ2 + 1 && z <= 130 && Math.abs(x) <= 56) || (x >= 28 && x <= 56 && z >= 40 && z <= BZ2); }

    /** Terrain height of the ground outside the bowl. */
    static int height(G g, int x, int z) {
        double n = Noise.fbm2(x * 0.06, z * 0.06, g.seed + 11);
        int ax = Math.abs(x);
        if (z >= 131) return (int) Math.round(5 + Math.min(34, (z - 131) * 1.4) + n * 6);   // back face of the quarry
        if (yard(x, z)) return RIM_FAR;
        if (z >= BZ2 + 1) return (int) Math.round(RIM_FAR + 3 + (ax - 56) * 0.5 + n * 6);
        if (z < -1 && ax <= 8) return 0;                                                   // site entrance road
        if (z < -1) return (int) Math.round(Math.max(1, 3 + n * 4));                       // behind the start
        if (z < BZ1 && ax <= BX) return 0;                                                 // apron
        if (x <= -BX - 1) return (int) Math.round(Math.max(6, 6 + (ax - 28) * 0.35 + n * 5));   // left rim hill
        return (int) Math.round(Math.max(9, 9 + (ax - 28) * 0.3 + n * 5));                 // right hill (crusher)
    }

    public G generate(long seed) {
        TgMapData d = new TgMapData(id(), seed);
        G g = new G(d);
        occ = new boolean[129 * 112];

        terrain(g);
        bowl(g);
        haulRoad(g);
        startStrip(g);
        pens(g);
        g.scenery = true;
        excavator(g);
        cableways(g);
        crusher(g);
        crane(g);
        clutter(g);
        props(g);
        backdrop(g);
        g.scenery = false;

        d.voidY = -30;
        d.playBounds = Region.of(-84, -30, -34, 84, 90, 160);
        d.buildBounds = Region.of(-BX, -20, 7, BX, 9, BZ2 + 2);
        d.hazardRegions.add(Region.of(-BX, -40, BZ1, BX, -20, BZ2));        // flooded quarry floor
        d.hazardRegions.add(Region.of(-90, -40, -1, -BX - 1, 90, BZ2));     // left hill (out of the quarry)
        d.hazardRegions.add(Region.of(BX + 1, -40, -1, 90, 90, 39));        // right hill / crusher
        d.overviewPoints.add(new Spawn(0.5, 42, -14.5, 0f, 35f));
        d.overviewPoints.add(new Spawn(0.5, 40, 140.5, 180f, 30f));
        d.notes.put("safeLine", "haul road: down the left wall (3 benches, a 3-block gap, a boulder squeeze), along the far wall (3-block gap, 2-wide ledge), up the 3-wide cut (choke) or the wide double ramp");
        d.notes.put("fastLine", "bridge ~52 straight across the open bowl + 5 up the far face, bending around 5 hanging sheet piles and a rock spire; exposed to snowballs and pickaxes the whole way");
        d.notes.put("decoys", "4 fake pens built like the real one (crane hook over the first) + a scaffold deck: 9 unnamed decoy chickens; the real pen is behind the cabin row at the back");
        g.finish();
        return g;
    }

    // ------------------------------------------------------------------ yard occupancy (x -64..64, z 30..141)

    private boolean[] occ;

    private int oi(int x, int z) { return x < -64 || x > 64 || z < 30 || z > 141 ? -1 : (x + 64) * 112 + (z - 30); }

    private void reserve(int x1, int z1, int x2, int z2, int pad) {
        for (int x = x1 - pad; x <= x2 + pad; x++) for (int z = z1 - pad; z <= z2 + pad; z++) { int i = oi(x, z); if (i >= 0) occ[i] = true; }
    }

    private boolean free(int x1, int z1, int x2, int z2) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) { int i = oi(x, z); if (i < 0 || occ[i] || !yard(x, z)) return false; }
        return true;
    }

    // ------------------------------------------------------------------ terrain

    private void terrain(G g) {
        g.scenery = true;
        int X0 = -82, X1 = 82, Z0 = -32, Z1 = 158;
        int[][] h = new int[X1 - X0 + 1][Z1 - Z0 + 1];
        for (int x = X0; x <= X1; x++)
            for (int z = Z0; z <= Z1; z++) h[x - X0][z - Z0] = height(g, x, z);
        for (int x = X0; x <= X1; x++)
            for (int z = Z0; z <= Z1; z++) {
                if (inBowl(x, z) || !onIsland(x, z)) continue;
                int top = h[x - X0][z - Z0];
                int low = top;
                boolean edge = false;
                for (int[] dd : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + dd[0], nz = z + dd[1];
                    if (nx < X0 || nx > X1 || nz < Z0 || nz > Z1 || !onIsland(nx, nz)) { edge = true; continue; }
                    if (inBowl(nx, nz)) { low = Math.min(low, -24); continue; }
                    low = Math.min(low, h[nx - X0][nz - Z0]);
                }
                int bottom = edge ? -26 - (int) (g.rnd(x, 0, z) * 8) : Math.min(top - 3, low - 1);
                boolean route = (z <= 10 && Math.abs(x) <= BX) || yard(x, z);
                g.scenery = !route;
                for (int y = bottom; y <= top; y++) g.set(x, y, z, rock(g, x, y, z));
                if (route) g.set(x, top, z, ROAD.at(x, top, z, g.seed));
                else if (top > RIM_FAR + 1 || Math.abs(x) > BX) {
                    double r = Noise.fbm2(x * 0.11, z * 0.11, g.seed + 21) * 0.8 + g.rnd(x, top, z, 3) * 0.2;
                    r = (r - 0.3) * 1.9;
                    g.set(x, top, z, r < 0.35 ? "coarse_dirt" : r < 0.55 ? "sandstone" : r < 0.75 ? "grass_block" : r < 0.85 ? "rooted_dirt" : "smooth_sandstone");
                    double p = g.rnd(x, top + 1, z, 4);
                    if (g.get(x, top, z).contains("grass") && p < 0.3) g.set(x, top + 1, z, p < 0.1 ? "fern" : "short_grass");
                    else if (p > 0.97) g.set(x, top + 1, z, "dead_bush");
                }
            }
        g.scenery = false;
    }

    static boolean onIsland(int x, int z) {
        double ex = x / 82.0, ez = (z - 63) / 95.0;
        return ex * ex + ez * ez <= 1.0;
    }

    static boolean inBowl(int x, int z) { return Math.abs(x) <= BX && z >= BZ1 && z <= BZ2; }

    private void bowl(G g) {
        g.scenery = true;
        for (int x = -BX; x <= BX; x++)
            for (int z = BZ1; z <= BZ2; z++) {
                for (int y = -26; y <= WATER - 1; y++) g.set(x, y, z, DEEP.at(x, y, z, g.seed));
                g.set(x, WATER, z, "water");
            }
        // boulders in the water, half submerged
        for (int i = 0; i < 14; i++) {
            double bx = -20 + g.rnd(i, 1, 2) * 42, bz = 14 + g.rnd(i, 2, 3) * 38;
            g.ellipsoid(bx, WATER, bz, 1.5 + g.rnd(i, 3, 4) * 1.5, 1.6, 1.5 + g.rnd(i, 4, 5), GREY);
        }
        // rock spire left standing in the bowl (obstacle for the straight bridge)
        for (int y = WATER - 1; y <= 15; y++) {
            double r = y <= 12 ? 2.4 : 2.4 - (y - 12) * 0.6;
            g.cyl(15.5, 46.5, r, y, y, y > 0 ? SAND_HI : y > -10 ? SAND_MID : GREY);
        }
        g.set(15, 16, 46, "dead_bush");
        // drill marks / strata on the near face under the apron
        for (int x = -BX; x <= BX; x += 3) for (int y = -18; y <= -2; y += 4) g.set(x, y, BZ1 - 1, "smooth_sandstone");
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
        int L = -BX, R = -BX + 3;              // left road x -27..-24
        // R1 down the left wall
        shelf(g, L, R, 11, 16, 0);
        Stairs.flight(g, "R1a", "sandstone", "sandstone", L, 19, -3, 0, -1, 3, 4, WATER - 1);
        shelf(g, L, R, 20, 28, -3);
        // collapsed section z 29..31 (3-block gap), rubble far below
        g.scenery = true;
        g.ellipsoid(-25.5, WATER + 1, 30, 2.2, 1.5, 2.0, GREY);
        g.scenery = false;
        shelf(g, L, R, 32, 39, -3);
        Stairs.flight(g, "R1b", "sandstone", "sandstone", L, 42, -6, 0, -1, 3, 4, WATER - 1);
        shelf(g, L, R, 43, 51, -6);
        // boulder squeeze: only x -27..-26 stays open over z 46..47
        g.scenery = true;
        g.ellipsoid(-23.8, -4.5, 46.5, 1.6, 2.0, 1.1, GREY);
        g.scenery = false;
        for (int z = 45; z <= 48; z++) for (int x = L; x <= L + 1; x++) for (int y = -5; y <= -3; y++) g.set(x, y, z, null);
        Stairs.flight(g, "R1c", "sandstone", "sandstone", L, 55, BENCH, 0, -1, 4, 4, WATER - 1);
        shelf(g, L, R, 56, BZ2, BENCH);
        // dead-end ledge off bench B: a 2-wide diving board into the bowl
        shelf(g, R + 1, -14, 25, 26, -3);
        g.set(-14, -3, 25, "spruce_planks"); g.set(-14, -3, 26, "spruce_planks");
        // R2 along the far wall (z 58..62): a 3-block gap at x -11..-9, a 2-wide ledge at x 8..16, rubble at x 1..2
        shelf(g, R + 1, -12, 58, BZ2, BENCH);
        shelf(g, -8, 7, 58, BZ2, BENCH);
        shelf(g, 8, 16, 61, BZ2, BENCH);
        shelf(g, 17, BX, 58, BZ2, BENCH);
        // fallen blocks on R2 (break up the straight, force a weave next to the edge)
        for (int y = BENCH + 1; y <= BENCH + 2; y++) {
            for (int z = 58; z <= 60; z++) g.set(-2, y, z, rock(g, -2, y, z));
            for (int z = 58; z <= 60; z++) g.set(-18, y, z, rock(g, -18, y, z));
            for (int z = 61; z <= BZ2; z++) g.set(-15, y, z, rock(g, -15, y, z));
            for (int x = 0; x <= 1; x++) for (int z = 61; z <= BZ2; z++) g.set(x, y, z, rock(g, x, y, z));
            g.set(9, y, 62, rock(g, 9, y, 62));                 // two pinches on the narrow ledge: weave at the edge
            g.set(14, y, 61, rock(g, 14, y, 61));
            for (int z = 61; z <= BZ2; z++) g.set(20, y, z, rock(g, 20, y, z));
        }
        // the cut: 3-wide straight staircase from the end of R2 up through the right wall (choke), z 59..61
        for (int x = BX + 1; x <= 43; x++)
            for (int z = 59; z <= 61; z++)
                for (int y = x == 43 ? RIM_FAR + 1 : BENCH + 1; y <= RIM_FAR + 6; y++) g.set(x, y, z, null);
        for (int x = BX + 1; x <= 42; x++)
            for (int y = BENCH - 2; y <= RIM_FAR; y++) { g.set(x, y, 58, rock(g, x, y, 58)); g.set(x, y, 62, rock(g, x, y, 62)); }
        Stairs.flight(g, "cut", "smooth_sandstone", "sandstone", BX + 1, 59, BENCH, 1, 0, 15, 3, BENCH - 2);
        // wide ramp: flight D up -z on the right shelf, landing pad, flight E up +x into the plateau
        shelf(g, 23, BX, 44, 49, -2);
        shelf(g, 25, BX, 37, 43, -2);                   // dead-end tongue of the pad
        Stairs.flight(g, "rampD", "sandstone", "sandstone", 23, 57, BENCH, 0, -1, 8, 5, WATER - 1);
        for (int x = BX + 1; x <= 35; x++)
            for (int z = 44; z <= 48; z++)
                for (int y = x == 35 ? RIM_FAR + 1 : -1; y <= RIM_FAR + 6; y++) g.set(x, y, z, null);
        for (int x = BX + 1; x <= 34; x++)
            for (int y = -4; y <= RIM_FAR; y++) { g.set(x, y, 43, rock(g, x, y, 43)); g.set(x, y, 49, rock(g, x, y, 49)); }
        Stairs.flight(g, "rampE", "sandstone", "sandstone", BX + 1, 44, -2, 1, 0, 7, 5, -4);
        // edge stripes on the inner edges (no parapets: you can be knocked off)
        for (int z = 11; z <= BZ2; z++) {
            int f = roadFloor(z);
            if (f != Integer.MIN_VALUE && g.get(R, f, z) != null && Bs.id(g.get(R, f, z)).equals("packed_mud"))
                g.set(R, f, z, (z & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
        }
        for (int x = R + 1; x <= BX; x++) {
            int z = x >= 8 && x <= 16 ? 61 : 58;
            if (g.get(x, BENCH, z) != null && Bs.id(g.get(x, BENCH, z)).equals("packed_mud")) g.set(x, BENCH, z, (x & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
        }
    }

    static int roadFloor(int z) {
        if (z <= 16) return 0;
        if (z >= 20 && z <= 39 && (z <= 28 || z >= 32)) return -3;
        if (z >= 43 && z <= 51) return -6;
        if (z >= 56) return BENCH;
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
        for (int x = -BX + 4; x <= BX; x++) g.set(x, 0, 10, (x & 1) == 0 ? "smooth_sandstone" : "cut_sandstone");
    }

    // ------------------------------------------------------------------ pens (one real, four fakes)

    private void pens(G g) {
        TgMapData d = g.d;
        int y = RIM_FAR;
        for (int k = 0; k < PENS.length; k++) {
            int[] p = PENS[k];
            int x1 = p[0], z1 = p[1], x2 = p[2], z2 = p[3];
            for (int x = x1; x <= x2; x++)
                for (int z = z1; z <= z2; z++) {
                    boolean ring = x == x1 || x == x2 || z == z1 || z == z2;
                    g.set(x, y, z, ring ? "polished_andesite" : ((x + z) & 1) == 0 ? "smooth_sandstone" : "sandstone");
                    for (int yy = y + 1; yy <= y + 4; yy++) g.set(x, yy, z, null);
                    if (ring) g.set(x, y + 1, z, ((x + z) & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
                }
            // two gaps in every side
            int mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
            for (int i = -3; i <= -2; i++) { g.set(mx + i, y + 1, z1, null); g.set(mx - i, y + 1, z2, null); }
            for (int i = 0; i <= 1; i++) { g.set(x1, y + 1, mz + i, null); g.set(x2, y + 1, mz - i, null); }
            // hay, composter, floodlights at the corners
            g.set(x1 + 1, y + 1, z2 - 1, "hay_block[axis=y]");
            g.set(x1 + 2, y + 1, z2 - 1, "hay_block[axis=x]");
            g.set(x2 - 1, y + 1, z2 - 1, "composter[level=3]");
            for (int[] c : new int[][]{{x1 - 2, z1 - 2}, {x2 + 2, z1 - 2}, {x1 - 2, z2 + 2}, {x2 + 2, z2 + 2}}) {
                if (!yard(c[0], c[1])) continue;
                for (int yy = y + 1; yy <= y + 6; yy++) g.set(c[0], yy, c[1], "iron_bars");
                g.set(c[0], y + 7, c[1], "sea_lantern");
                g.set(c[0], y + 8, c[1], "daylight_detector[inverted=false,power=0]");
            }
            reserve(x1, z1, x2, z2, 2);
        }
        int[] rp = PENS[0];
        d.pen = Region.of(rp[0] + 1, y + 1, rp[1] + 1, rp[2] - 1, y + 3, rp[3] - 1);
        g.chickens(d.pen, 12);
        for (int[] dc : DECOYS) reserve(dc[0], dc[2], dc[0], dc[2], 1);
    }

    // ------------------------------------------------------------------ machinery

    private void excavator(G g) {
        // tracked excavator on the left rim pad, boom over the left wall to the hanging bucket
        int py = 9;
        for (int x = -50; x <= -38; x++)
            for (int z = 22; z <= 36; z++) {
                for (int y = py - 6; y < py; y++) g.set(x, y, z, rock(g, x, y, z));
                g.set(x, py, z, ROAD.at(x, py, z, g.seed));
                for (int y = py + 1; y <= py + 14; y++) g.set(x, y, z, null);
            }
        for (int x = -48; x <= -40; x++)
            for (int z : new int[]{23, 24, 33, 34}) {
                g.set(x, py + 1, z, "black_wool");
                g.set(x, py + 2, z, (x & 1) == 0 ? "gray_wool" : "black_wool");
            }
        g.fill(-48, py + 3, 23, -40, py + 3, 34, "gray_terracotta");
        g.fill(-49, py + 4, 24, -39, py + 8, 33, YELLOW);
        g.fill(-48, py + 9, 25, -40, py + 9, 32, "yellow_terracotta");
        g.fill(-41, py + 9, 26, -38, py + 12, 30, YELLOW);
        g.fill(-38, py + 10, 27, -38, py + 11, 29, "light_gray_stained_glass");
        g.fill(-40, py + 10, 26, -39, py + 11, 26, "light_gray_stained_glass");
        g.fill(-41, py + 13, 26, -38, py + 13, 30, "black_terracotta");
        g.fill(-50, py + 4, 25, -50, py + 7, 32, "gray_terracotta");
        for (int z = 25; z <= 32; z++) g.set(-50, py + 6, z, (z & 1) == 0 ? "black_terracotta" : "yellow_terracotta");
        g.set(-44, py + 10, 29, "lantern[hanging=false]");
        double[] a = {-38, py + 9, 29}, b = {-27, 31, 30}, c = {-24, 24, 31};
        for (int k = 0; k < 2; k++) for (int j = 0; j < 2; j++) {
            g.line(a[0], a[1] + k, a[2] + j, b[0], b[1] + k, b[2] + j, "yellow_terracotta");
            g.line(b[0], b[1] + k, b[2] + j, c[0], c[1] + k, c[2] + j, "yellow_terracotta");
        }
        g.line(a[0], a[1] + 2, a[2] + 1, b[0], b[1] + 2, b[2] + 1, "black_terracotta");
        g.tether(-24, 23, 31, -34, py + 14, 31, "chain");
        // bucket raised high over the collapsed gap (overhead scenery, not a stepping stone)
        int bx1 = -27, bx2 = -21, bz1 = 28, bz2 = 33, by1 = 17, by2 = 22;
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

    /** Hanging steel sheet piles {x1, z, x2} in the gap: they bend the straight bridge; their tops are too high to use. */
    public static final int[][] PLATES = {{-16, 24, -5}, {-4, 31, 7}, {5, 38, 17}, {-13, 45, -2}, {-3, 52, 9}};
    public static final int PLATE_LO = -6, PLATE_HI = 11, CABLE = 22;

    private void cableways(G g) {
        for (int[] c : new int[][]{{-37, 8}, {-37, 56}, {34, 8}, {34, 54}}) tower(g, c[0], c[1], CABLE);
        reserve(33, 53, 36, 56, 1);
        for (int z = 9; z <= 56; z++) { g.set(-35, CABLE, z, "chain[axis=z]"); g.set(33, CABLE, z, "chain[axis=z]"); }
        for (int[] pl : PLATES) {
            int x1 = pl[0], z = pl[1], x2 = pl[2];
            for (int x = -34; x <= 32; x++) g.set(x, CABLE, z, "chain[axis=x]");
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
        double[] a = {21, WATER + 1, 16}, b = {44, 22, 16};
        for (int k = 0; k < 3; k++) {
            g.line(a[0], a[1], a[2] + k, b[0], b[1], b[2] + k, "gray_terracotta");
            g.line(a[0], a[1] + 1, a[2] + k, b[0], b[1] + 1, b[2] + k, k == 1 ? "black_wool" : "iron_bars");
        }
        for (int x = 24; x <= 42; x += 4) {
            int topY = (int) (a[1] + (x - a[0]) / (b[0] - a[0]) * (b[1] - a[1]));
            for (int y = Math.max(WATER, x > BX ? height(g, x, 17) : WATER); y < topY; y++) { g.set(x, y, 16, "iron_bars"); g.set(x, y, 18, "iron_bars"); }
        }
        int hy = height(g, 50, 12);
        g.fill(43, hy, 6, 56, hy + 9, 22, new Pal("light_gray_terracotta", 40, "gray_terracotta", 20, "smooth_stone", 20));
        g.clear(44, hy + 1, 7, 55, hy + 8, 21);
        g.fill(43, hy + 10, 5, 57, hy + 10, 23, "dark_oak_slab[type=bottom]");
        for (int z = 8; z <= 20; z += 3) g.fill(43, hy + 5, z, 43, hy + 6, z + 1, "light_gray_stained_glass");
        g.fill(43, hy + 1, 15, 43, hy + 4, 18, "black_terracotta");
        g.fill(50, hy + 11, 10, 51, hy + 16, 11, "bricks");
        g.set(50, hy + 17, 10, "campfire[lit=true,signal_fire=true]");
        for (int i = 0; i < 6; i++) {
            double cx = 34 + g.rnd(i, 7, 1) * 34, cz = -14 + g.rnd(i, 8, 2) * 14;
            int base = height(g, (int) cx, (int) cz);
            for (int y = 0; y < 6; y++) g.cyl(cx, cz, 5 - y * 0.8, base + y, base + y, i % 2 == 0 ? new Pal("gravel", 1) : new Pal("sand", 3, "sandstone", 1));
        }
    }

    private void crane(G g) {
        int y0 = RIM_FAR + 1, top = 56;
        int tx = 10, tz = 118;
        reserve(tx - 1, tz - 1, tx + 3, tz + 3, 1);
        for (int y = y0; y <= top; y++)
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++) {
                    boolean corner = (i != 1) && (j != 1);
                    if (corner) g.set(tx + i, y, tz + j, "yellow_terracotta");
                    else if ((y + i + j) % 3 == 0 && !(i == 1 && j == 1)) g.set(tx + i, y, tz + j, "iron_bars");
                }
        g.fill(tx - 1, y0 - 1, tz - 1, tx + 3, y0 - 1, tz + 3, "polished_andesite");
        g.fill(tx - 1, top + 1, tz - 1, tx + 3, top + 1, tz + 3, "gray_terracotta");
        g.fill(tx, top + 2, tz - 2, tx + 2, top + 4, tz - 1, YELLOW);
        g.fill(tx, top + 3, tz - 2, tx + 2, top + 3, tz - 2, "light_gray_stained_glass");
        // jib towards the bowl (-z) over the first fake pen, counter-jib with weights (+z)
        for (int z = tz - 46; z <= tz + 12; z++) {
            boolean jib = z <= tz;
            g.set(tx + 1, top + 2, z, (z & 1) == 0 ? "yellow_terracotta" : "black_terracotta");
            if (jib && (z % 3 == 0)) g.set(tx + 1, top + 3, z, "iron_bars");
        }
        g.fill(tx, top - 1, tz + 9, tx + 2, top + 1, tz + 12, "smooth_stone");
        g.fill(tx + 1, top + 3, tz, tx + 1, top + 7, tz + 2, "yellow_terracotta");
        g.line(tx + 1, top + 7, tz + 1, tx + 1, top + 3, tz - 44, "chain[axis=z]");
        g.line(tx + 1, top + 7, tz + 1, tx + 1, top + 3, tz + 11, "chain[axis=z]");
        // hook over the first FAKE pen
        int hz = 75;
        for (int y = 22; y <= top + 1; y++) g.set(tx + 1, y, hz, "chain[axis=y]");
        g.fill(tx, 20, hz, tx + 2, 21, hz, "iron_block");
        g.set(tx + 1, 19, hz, "iron_bars");
        // pennant on the cab
        for (int y = top + 8; y <= top + 14; y++) g.set(tx + 1, y, tz + 1, "end_rod[facing=up]");
        for (int i = 0; i < 6; i++)
            for (int y = top + 9 + i / 2; y <= top + 14 - i / 2; y++) g.set(tx + 2 + i, y, tz + 1, i % 2 == 0 ? "red_wool" : "white_wool");
        g.landmark.add(new double[]{tx + 1.5, top + 12, tz + 1.5});
        g.landmark.add(new double[]{tx + 1.5, top + 1, tz + 1.5});
    }

    /** Portacabin 8 x 5 with an open door in the -z wall and a dead-end interior. */
    private void cabin(G g, int cx, int cz) {
        int y = RIM_FAR + 1;
        g.fill(cx, y, cz, cx + 7, y + 3, cz + 4, "white_terracotta");
        g.clear(cx + 1, y, cz + 1, cx + 6, y + 2, cz + 3);
        g.fill(cx, y + 4, cz, cx + 7, y + 4, cz + 4, "light_gray_terracotta");
        g.fill(cx + 1, y + 1, cz, cx + 2, y + 2, cz, "light_blue_stained_glass_pane[east=true,west=true]");
        g.fill(cx + 5, y + 1, cz + 4, cx + 6, y + 2, cz + 4, "light_blue_stained_glass_pane[east=true,west=true]");
        g.fill(cx, y, cz, cx + 7, y, cz + 4, "gray_terracotta");
        g.clear(cx + 4, y, cz, cx + 5, y + 1, cz);                    // doorway
        g.clear(cx + 1, y, cz + 1, cx + 6, y, cz + 3);
        g.set(cx + 1, y, cz + 2, "barrel[facing=up,open=false]");
        g.set(cx + 6, y, cz + 3, "crafting_table");
        g.set(cx + 3, y + 2, cz + 2, "lantern[hanging=true]");
        reserve(cx, cz, cx + 7, cz + 4, 1);
        reserve(cx + 3, cz - 3, cx + 6, cz - 1, 0);                    // keep the door approach clear
    }

    /** Scaffold: corner posts, a plank deck at DECK with rails, a ladder up the south side; nothing beyond. */
    private void scaffold(G g, int[] s) {
        int x1 = s[0], z1 = s[1], x2 = s[2], z2 = s[3], y = RIM_FAR + 1;
        for (int[] c : new int[][]{{x1, z1}, {x2, z1}, {x1, z2}, {x2, z2}})
            for (int yy = y; yy <= DECK + 3; yy++) g.set(c[0], yy, c[1], "stripped_spruce_log[axis=y]");
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                boolean post = (x == x1 || x == x2) && (z == z1 || z == z2);
                if (!post) g.set(x, DECK, z, ((x + z) & 1) == 0 ? "spruce_planks" : "oak_planks");
                if (!post && (x == x1 || x == x2 || z == z1)) g.set(x, DECK + 1, z, x == x1 || x == x2 ? "spruce_fence[north=true,south=true]" : "spruce_fence[east=true,west=true]");
            }
        // cross bracing at the mid level
        for (int x = x1 + 1; x < x2; x++) { g.set(x, y + 2, z1, "spruce_slab[type=top]"); }
        int lx = (x1 + x2) / 2;
        for (int yy = y; yy <= DECK; yy++) g.set(lx, yy, z2, "stripped_spruce_log[axis=y]");
        for (int yy = y; yy <= DECK; yy++) g.set(lx, yy, z2 + 1, "ladder[facing=south]");
        for (int x = x1; x <= x2; x++) g.set(x, DECK + 1, z2, x == lx || x == x1 || x == x2 ? g.get(x, DECK + 1, z2) : "spruce_fence[east=true,west=true]");
        g.set(lx, DECK + 1, z2, null);
        g.set(x2 - 1, DECK + 1, z1 + 1, "barrel[facing=up,open=false]");
        g.set(x1 + 1, DECK + 1, z1 + 1, "lantern[hanging=false]");
        reserve(x1, z1, x2, z2 + 2, 1);
    }

    private void stack(G g, int x, int z, int w, int dz, int kind) {
        int y = RIM_FAR;
        if (!free(x - 1, z - 1, x + w, z + dz)) return;
        for (int i = 0; i < w; i++) for (int j = 0; j < dz; j++) if (g.get(x + i, y + 1, z + j) != null || g.get(x + i, y, z + j) == null) return;
        int hgt = 2 + (int) (g.rnd(x, 1, z) * 2.2);
        if (kind == 0) {
            Pal m = g.rnd(x, 2, z) < 0.5 ? new Pal("smooth_stone", 3, "stone", 1) : g.rnd(x, 3, z) < 0.5 ? new Pal("stone_bricks", 3, "cracked_stone_bricks", 1) : new Pal("cut_sandstone", 3, "smooth_sandstone", 1);
            g.fill(x, y + 1, z, x + w - 1, y + hgt, z + dz - 1, m);
            if (g.rnd(x, 4, z) < 0.5) g.set(x + (int) (g.rnd(x, 5, z) * w), y + hgt + 1, z, "smooth_stone_slab[type=bottom]");
        } else if (kind == 1) {        // crates
            for (int i = 0; i < w; i++) for (int j = 0; j < dz; j++) {
                int hh = 1 + (int) (g.rnd(x + i, 6, z + j) * 2.6);
                for (int k = 1; k <= hh; k++) g.set(x + i, y + k, z + j, g.rnd(x + i, y + k, z + j, 7) < 0.5 ? "barrel[facing=up,open=false]" : g.rnd(x + i, k, z + j, 8) < 0.5 ? "spruce_planks" : "stripped_oak_wood[axis=y]");
            }
        } else {                       // skip bin
            g.fill(x, y + 1, z, x + w - 1, y + 2, z + dz - 1, YELLOW);
            if (w > 2 && dz > 2) g.set(x + 1, y + 2, z + 1, "gravel");
        }
        reserve(x, z, x + w - 1, z + dz - 1, 0);
    }

    private void clutter(G g) {
        int y = RIM_FAR;
        // keep the arrival edge, the trench exits and the plateau lane clear
        reserve(-BX, BZ2 + 1, BX, BZ2 + 2, 0);
        reserve(BX + 1, 57, 46, 63, 0);
        reserve(BX + 1, 42, 38, 50, 0);
        for (int[] c : CABINS) cabin(g, c[0], c[1]);
        scaffold(g, SCAFFOLD_A);
        scaffold(g, SCAFFOLD_B);
        // raised loading dock: a flight up, nothing beyond (dead end)
        g.fill(-56, y + 1, 122, -46, y + 2, 129, new Pal("stone_bricks", 3, "mossy_stone_bricks", 1));
        g.fill(-56, y + 3, 122, -46, y + 3, 129, "spruce_planks");
        Stairs.flight(g, "dock", "stone_brick", "stone_bricks", -52, 119, y, 0, 1, 3, 3, y);
        for (int x = -55; x <= -48; x += 3) g.set(x, y + 4, 127, "barrel[facing=up,open=false]");
        reserve(-56, 117, -46, 129, 1);
        // parked mini excavator in the yard
        int ex = 34, ez = 70;
        for (int x = ex; x <= ex + 6; x++) for (int z : new int[]{ez, ez + 1, ez + 5, ez + 6}) { g.set(x, y + 1, z, "black_wool"); g.set(x, y + 2, z, "gray_wool"); }
        g.fill(ex, y + 3, ez, ex + 6, y + 3, ez + 6, "gray_terracotta");
        g.fill(ex + 1, y + 4, ez + 1, ex + 5, y + 6, ez + 5, YELLOW);
        g.fill(ex + 1, y + 5, ez + 1, ex + 1, y + 6, ez + 3, "light_gray_stained_glass");
        g.line(ex, y + 6, ez + 3, ex - 5, y + 9, ez + 3, "yellow_terracotta");
        g.line(ex - 5, y + 9, ez + 3, ex - 7, y + 2, ez + 3, "yellow_terracotta");
        g.fill(ex - 8, y + 1, ez + 2, ex - 7, y + 2, ez + 4, STEEL);
        reserve(ex - 9, ez - 1, ex + 7, ez + 7, 1);
        // dump truck in the yard
        int tx = -52, tz = 104;
        g.fill(tx, y + 1, tz, tx + 6, y + 1, tz + 6, "black_wool");
        g.fill(tx, y + 2, tz, tx + 6, y + 4, tz + 3, YELLOW);
        g.fill(tx + 1, y + 4, tz + 1, tx + 5, y + 4, tz + 2, "gravel");
        g.fill(tx, y + 2, tz + 4, tx + 6, y + 5, tz + 6, YELLOW);
        g.fill(tx + 1, y + 4, tz + 6, tx + 5, y + 4, tz + 6, "light_gray_stained_glass");
        reserve(tx, tz, tx + 6, tz + 6, 1);
        // generator
        g.fill(46, y + 1, 84, 49, y + 2, 86, "gray_terracotta");
        g.fill(46, y + 3, 84, 49, y + 3, 86, "iron_block");
        g.set(47, y + 4, 85, "end_rod[facing=up]");
        g.set(45, y + 1, 85, "smoker[facing=west,lit=true]");
        reserve(45, 84, 49, 86, 1);
        // drill rig: lattice mast with lights
        int rx = -21, rz = 95;
        for (int yy = y + 1; yy <= y + 18; yy++)
            for (int i = 0; i < 3; i++) for (int j = 0; j < 3; j++) {
                boolean corner = (i != 1) && (j != 1);
                if (corner) g.set(rx + i, yy, rz + j, "gray_terracotta");
                else if (yy % 3 == 0 && !(i == 1 && j == 1)) g.set(rx + i, yy, rz + j, "iron_bars");
            }
        g.set(rx + 1, y + 19, rz + 1, "sea_lantern");
        g.fill(rx - 1, y + 1, rz + 3, rx + 3, y + 3, rz + 5, YELLOW);
        reserve(rx - 1, rz, rx + 3, rz + 5, 1);
        // pipe racks along the yard sides
        for (int z = 72; z <= 126; z += 13) {
            if (!free(-60, z, -57, z + 1)) continue;
            g.fill(-56, y + 1, z, -52, y + 1, z + 1, "stripped_spruce_log[axis=x]");
            g.fill(-55, y + 2, z, -53, y + 2, z + 1, "stripped_spruce_log[axis=x]");
            reserve(-56, z, -52, z + 1, 1);
        }
        // block stacks, crates and skips on a jittered grid over the whole yard and the plateau
        for (int gz = 40; gz <= 128; gz += 6)
            for (int gx = -56; gx <= 54; gx += 6) {
                int x = gx + (int) (g.rnd(gx, 7, gz) * 2.99), z = gz + (int) (g.rnd(gx, 8, gz) * 2.99);
                int w = 2 + (int) (g.rnd(gx, 9, gz) * 1.99), dz = 2 + (int) (g.rnd(gx, 10, gz) * 1.99);
                double k = g.rnd(gx, 11, gz);
                stack(g, x, z, w, dz, k < 0.55 ? 0 : k < 0.85 ? 1 : 2);
            }
        // no straight free sprint > 13 anywhere on the yard / plateau floor
        g.breakRuns(-56, 56, 40, 130, y, 13, p -> {
            int i = oi(p[0], p[1]);
            String here = g.get(p[0], y, p[1]);
            if (here != null && Bs.id(here).endsWith("_stairs")) return true;
            if (i >= 0 && occ[i]) {
                for (int[] pp : PENS) if (p[0] >= pp[0] - 1 && p[0] <= pp[2] + 1 && p[1] >= pp[1] - 1 && p[1] <= pp[3] + 1) return true;
                for (int[] dc : DECOYS) if (Math.abs(p[0] - dc[0]) <= 1 && Math.abs(p[1] - dc[2]) <= 1) return true;
            }
            return (p[0] >= 43 && p[0] <= 45 && p[1] >= 59 && p[1] <= 61) || (p[0] >= 35 && p[0] <= 37 && p[1] >= 44 && p[1] <= 48)
                    || (Math.abs(p[0]) <= BX && p[1] <= BZ2 + 1);
        }, "stone_brick_wall", null);
    }

    private void props(G g) {
        // fences + lanterns along the scenery rim edges (never on the route)
        for (int z = 12; z <= BZ2; z++) {
            int hx = height(g, -BX - 2, z);
            g.set(-BX - 2, hx + 1, z, "spruce_fence");
            if (z % 8 == 4) { g.set(-BX - 2, hx + 2, z, "spruce_fence"); g.set(-BX - 2, hx + 3, z, "lantern[hanging=false]"); }
        }
        for (int z = 11; z <= 38; z++) {
            int hx = height(g, BX + 2, z);
            g.set(BX + 2, hx + 1, z, "spruce_fence");
            if (z % 8 == 4) { g.set(BX + 2, hx + 2, z, "spruce_fence"); g.set(BX + 2, hx + 3, z, "lantern[hanging=false]"); }
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
        g.text("QUARRY", 11, 16, -7, -1, 0, 1, "yellow_wool");
        g.fill(-12, 18, -6, 12, 18, -6, "spruce_slab[type=bottom]");
        // spruce trees on the outer hills
        for (int i = 0; i < 70; i++) {
            int x = (int) (-78 + g.rnd(i, 1, 9) * 156), z = (int) (-28 + g.rnd(i, 2, 9) * 182);
            if (Math.abs(x) < 60 && z > -4 && z < 132) continue;
            double ex = x / 82.0, ez = (z - 63) / 95.0;
            if (ex * ex + ez * ez > 0.85) continue;
            int hy = height(g, x, z);
            int hgt = 4 + (int) (g.rnd(i, 3, 9) * 4);
            for (int yy = 1; yy <= hgt; yy++) g.set(x, hy + yy, z, "spruce_log");
            for (int yy = 2; yy <= hgt + 1; yy++) {
                int r = Math.max(0, (hgt + 2 - yy) / 2);
                for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++)
                    if (Math.abs(dx) + Math.abs(dz) <= r + 1 && !(dx == 0 && dz == 0 && yy <= hgt)) g.setIfAir(x + dx, hy + yy, z + dz, "spruce_leaves[persistent=true,distance=7]");
            }
        }
    }

    private void backdrop(G g) {
        // timbered tunnel mouth on the back face of the quarry
        for (int x = -8; x <= 8; x++)
            for (int y = RIM_FAR + 1; y <= RIM_FAR + 7; y++) {
                int z = 131 + (int) Math.ceil((y - RIM_FAR) / 1.4);
                boolean arch = Math.abs(x) <= 4 && (y <= RIM_FAR + 4 || Math.abs(x) <= 3 && y <= RIM_FAR + 5);
                if (arch) for (int k = 0; k < 6; k++) g.set(x, y, z + k, null);
                else if (Math.abs(x) <= 5) g.set(x, y, z, "stripped_dark_oak_log[axis=y]");
            }
        g.fill(-5, RIM_FAR + 6, 132, 5, RIM_FAR + 6, 132, "dark_oak_planks");
        g.set(-4, RIM_FAR + 4, 133, "lantern[hanging=false]");
        g.set(4, RIM_FAR + 4, 133, "lantern[hanging=false]");
        for (int x = -3; x <= 3; x++) g.set(x, RIM_FAR + 1, 131, "rail[shape=north_south]");
    }
}
