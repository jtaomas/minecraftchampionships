package com.mcm.games.parkourtag.map;

import java.util.List;

/**
 * The Dustbowl (MCC 21+), recreated from the MCC 21 stills (research/mcc_archive/parkour_tag/pt-2/frames/0222*..0229*,
 * dustbowl_wiki_render.png) and the Noxcrew MCC 21 update flyover (pt-3/frames/u21_*). Layout follows the traced
 * plan in tools/dustbowl_plan.py (previews/dustbowl_trace.png): a 40 x 48 street pit inside a stadium.
 *
 * <p>Orientation: north (az 0) = commentary booth end, west = PARKOUR TAG billboard, east = MCC logo.
 * Route colour language (MCC): teal/cyan = keep going, purple/blue toy blocks = jump, oak planks = safe ledge.
 */
public final class DustbowlMap implements PtMap {
    static final int M = 12;          // margin around the pit (walls + stands)
    static final int W = 40, D = 48;  // pit size
    static final int Y0 = 1;          // street surface block; feet stand at Y0 + 1
    static final int RIM = 3;         // rim ledge height (standing height above the street)

    private static final String BRICK = "polished_blackstone_bricks";
    private static final String TEAL = "cyan_terracotta";
    private static final String GO = "cyan_concrete";
    private static final String DARK = "deepslate_tiles";
    private static final String DARK2 = "polished_deepslate";
    private static final String LIGHT_BRICK = "stone_bricks";
    private static final String PLANK = "oak_planks";

    private final List<Cage> cages = List.of(
            new Cage(M + 20, Y0 + RIM + 1, M - 1, 0, 1, BRICK, BRICK),
            new Cage(M + 20, Y0 + RIM + 1, M + D, 0, -1, BRICK, BRICK));

    @Override public String id() { return "dustbowl"; }
    @Override public String displayName() { return "The Dustbowl"; }
    @Override public boolean finished() { return true; }
    @Override public int sizeX() { return W + 2 * M; }
    @Override public int sizeY() { return Y0 + 36; }
    @Override public int sizeZ() { return D + 2 * M; }
    @Override public List<Cage> cages() { return cages; }
    @Override public int[] playBox() { return new int[]{M, M, M + W - 1, M + D - 1}; }
    @Override public double[] spectator() { return new double[]{M + 20.5, Y0 + 22, M + 24.5, 0, 90}; }

    /** Top block y for a standing height h above the street. */
    private static int top(int h) { return Y0 + h; }

    @Override
    public void generate(BlockSink sink) {
        Painter p = new Painter(sink, M);
        street(p);
        enclosure(p);
        stands(p);
        rim(p);
        northWest(p);
        chevronScaffold(p);
        kiosk(p);
        lounge(p);
        faceBuilding(p);
        louver(p);
        westToyBlocks(p);
        chainPillars(p);
        centralGate(p);
        eastPlatform(p);
        dicePlanters(p);
        toyStair(p);
        southEast(p);
        southWest(p);
        crates(p);
        props(p);
        for (Cage c : cages) c.build(sink);
        cageRamps(p);
        containment(p);
    }

    // ------------------------------------------------------------------ ground

    private void street(Painter p) {
        p.fill(0, 0, 0, W - 1, 0, D - 1, "packed_mud");
        for (int x = 0; x < W; x++)
            for (int z = 0; z < D; z++) {
                // terracotta street with brown worn paths along the main N-S and E-W streets (022412 brown patches)
                boolean path = (x >= 19 && x <= 20 && (z < 15 || z > 28)) || (z == 13 || z == 14) && x > 6 && x < 31
                        || (x == 26 || x == 27) && z > 12 && z < 31 || (z == 29 || z == 30) && x > 13 && x < 31;
                boolean worn = path && ((x * 7 + z * 13) % 5 != 0);
                p.set(x, Y0, z, worn ? "packed_mud" : "terracotta");
            }
    }

    private void rim(Painter p) {
        for (int x = 0; x < W; x++)
            for (int z = 0; z < D; z++) {
                if (x != 0 && x != W - 1 && z != 0 && z != D - 1) continue;
                p.fill(x, Y0 + 1, z, x, top(RIM) - 1, z, BRICK);
                p.set(x, top(RIM), z, TEAL);
            }
    }

    /** Black brick walls with a teal band, green arrow decals on white (west) and blue (north) bands. */
    private void enclosure(Painter p) {
        int h = Y0 + 9;
        for (int x = -2; x <= W + 1; x++)
            for (int z = -2; z <= D + 1; z++) {
                if (x >= 0 && x < W && z >= 0 && z < D) continue;
                p.fill(x, 0, z, x, h, z, BRICK);
                p.set(x, Y0 + 5, z, TEAL);
            }
        // west inner face: white band with lime arrows pointing north; east: same pointing south
        for (int z = 0; z < D; z++) {
            for (int y = Y0 + 6; y <= Y0 + 8; y++) { p.set(-1, y, z, "white_concrete"); p.set(W, y, z, "white_concrete"); }
        }
        for (int z = 2; z < D - 2; z += 6) { arrow(p, -1, z, 0, -1); arrow(p, W, z + 2, 0, 1); }
        // north + south inner faces: blue band, arrows pointing west / east
        for (int x = 0; x < W; x++) {
            for (int y = Y0 + 6; y <= Y0 + 8; y++) { p.set(x, y, -1, "blue_concrete"); p.set(x, y, D, "blue_concrete"); }
        }
        for (int x = 2; x < W - 2; x += 6) { if (x < 15 || x > 25) { arrow(p, x + 2, -1, -1, 0); arrow(p, x, D, 1, 0); } }
    }

    /** A 3x3 chevron arrow on a wall plane; (dx, dz) = the way it points. */
    private void arrow(Painter p, int ax, int az, int dx, int dz) {
        int[][] cells = {{0, 2}, {1, 1}, {0, 0}, {-1, 2}, {0, 1}, {-1, 0}};
        for (int[] c : cells) p.set(ax + dx * c[0], Y0 + 6 + c[1], az + dz * c[0], "lime_concrete");
    }

    /** Stepped stands with banded wool seats on all four sides, billboard (west), MCC logo (east), booth (north). */
    private void stands(Painter p) {
        String[] seat = {"red_wool", "lime_wool", "yellow_wool", "purple_wool", "magenta_wool", "light_blue_wool", "orange_wool"};
        for (int r = 0; r < 7; r++) {
            int y = Y0 + 10 + r;
            int lo = -3 - r, hiX = W + 2 + r, hiZ = D + 2 + r;
            for (int x = lo; x <= hiX; x++)
                for (int z = lo; z <= hiZ; z++) {
                    boolean ring = x == lo || x == hiX || z == lo || z == hiZ;
                    if (!ring) continue;
                    p.fill(x, Y0 + 9, z, x, y - 1, z, "light_gray_concrete");
                    int band = Math.floorMod((x + z) / 4, seat.length);
                    p.set(x, y, z, r == 0 ? "white_concrete" : seat[(band + r) % seat.length]);
                }
        }
        // front railing on the first row
        for (int x = -3; x <= W + 2; x++) { p.set(x, Y0 + 11, -3, "white_stained_glass_pane[east=true,west=true]"); p.set(x, Y0 + 11, D + 2, "white_stained_glass_pane[east=true,west=true]"); }
        for (int z = -2; z <= D + 1; z++) { p.set(-3, Y0 + 11, z, "white_stained_glass_pane[north=true,south=true]"); p.set(W + 2, Y0 + 11, z, "white_stained_glass_pane[north=true,south=true]"); }

        // back wall
        int back = -10;
        for (int x = back; x <= W + 9; x++)
            for (int z = back; z <= D + 9; z++)
                if (x == back || x == W + 9 || z == back || z == D + 9) p.fill(x, Y0 + 9, z, x, Y0 + 17, z, "white_concrete");

        // PARKOUR TAG billboard on the west back wall: red field, white block letters, blue shadow (022552, 022648)
        p.fill(back, Y0 + 15, 1, back, Y0 + 34, D - 2, "red_concrete");
        p.fill(back + 1, Y0 + 15, 1, back + 1, Y0 + 15, D - 2, "white_concrete");
        // viewer faces west: text advances south -> north (dz = -1)
        p.text("PARKOUR", back, Y0 + 32, D - 4, 0, -1, "white_concrete", "blue_concrete");
        p.text("TAG", back, Y0 + 23, D - 16, 0, -1, "white_concrete", "blue_concrete");

        // MCC logo on the east back wall: yellow field, white letters (022420, 022632)
        p.fill(W + 9, Y0 + 15, 6, W + 9, Y0 + 28, D - 7, "yellow_concrete");
        p.text("MCC", W + 9, Y0 + 25, 15, 0, 1, "white_concrete", "orange_concrete");

        // commentary booth over the north stands: white box, red/white awning, windows, checker floor (022304, 022632)
        int bz0 = back, bz1 = -5;
        p.fill(10, Y0 + 17, bz0, 29, Y0 + 17, bz1, "white_concrete");
        for (int x = 10; x <= 29; x++) for (int z = bz0; z <= bz1; z++) p.set(x, Y0 + 17, z, (x + z) % 2 == 0 ? "white_concrete" : "black_concrete");
        p.walls(10, Y0 + 18, bz0, 29, Y0 + 23, bz1, "white_concrete");
        p.fill(11, Y0 + 19, bz1, 28, Y0 + 21, bz1, "light_blue_stained_glass");
        p.fill(10, Y0 + 24, bz0, 29, Y0 + 24, bz1, "smooth_quartz");
        for (int x = 10; x <= 29; x++) p.set(x, Y0 + 23, bz1 + 1, x % 2 == 0 ? "red_concrete" : "white_concrete");
        p.fill(18, Y0 + 25, bz1, 21, Y0 + 27, bz1, "red_concrete");
        p.fill(13, Y0 + 24, bz1 - 1, 33, Y0 + 32, bz1 - 1, "red_concrete");
        p.text("MCC", 15, Y0 + 31, bz1, 1, 0, "white_concrete", "white_concrete");

        // red brick stadium pillars (wiki render top edge)
        int[][] cols = {{-9, -9}, {W + 8, -9}, {-9, D + 8}, {W + 8, D + 8}};
        for (int[] c : cols) {
            p.fill(c[0] - 1, Y0 + 9, c[1] - 1, c[0] + 1, Y0 + 30, c[1] + 1, "bricks");
            p.fill(c[0] - 1, Y0 + 31, c[1] - 1, c[0] + 1, Y0 + 31, c[1] + 1, "red_nether_bricks");
        }
    }

    // ------------------------------------------------------------------ north band

    /** NW: oak plank staircase tower up to the rim, white trim (022412 top-left). */
    private void northWest(Painter p) {
        p.fill(1, Y0 + 1, 1, 5, top(4), 4, BRICK);
        p.fill(1, top(4), 1, 5, top(4), 4, PLANK);
        p.walls(1, top(4), 1, 5, top(4), 4, "white_concrete");
        p.set(3, top(4), 1, PLANK);
        p.fill(1, Y0 + 1, 5, 5, top(3), 5, PLANK);
        p.fill(1, Y0 + 1, 6, 5, top(2), 6, PLANK);
        p.fill(1, Y0 + 1, 7, 5, top(1), 7, PLANK);
        p.fill(1, top(3), 5, 1, top(3), 7, "white_concrete");
    }

    /** Black scaffold with a yellow/black chevron fascia, deck at h5, open underneath (022412, 022640, 022648). */
    private void chevronScaffold(Painter p) {
        int deck = top(5);
        for (int x : new int[]{7, 10, 11, 14}) for (int z : new int[]{1, 6}) p.fill(x, Y0 + 1, z, x, deck - 1, z, "polished_blackstone");
        p.fill(7, deck, 1, 14, deck, 6, "black_concrete");
        for (int x = 7; x <= 14; x++)
            for (int y = deck - 1; y <= deck; y++) {
                boolean yellow = Math.floorMod(x + y, 4) < 2;
                p.set(x, y, 6, yellow ? "yellow_concrete" : "black_concrete");
            }
        for (int x = 8; x <= 13; x++) p.set(x, deck + 1, 1, "iron_bars[east=true,west=true]");
    }

    /** Orange terracotta kiosk, two storeys, white rim, iron-bar window, roof reachable by ladder (022412, d010). */
    private void kiosk(Painter p) {
        int x0 = 25, x1 = 31, z0 = 1, z1 = 7;
        p.walls(x0, Y0 + 1, z0, x1, top(6), z1, "orange_terracotta");
        p.fill(x0 + 1, top(3), z0 + 1, x1 - 1, top(3), z1 - 1, PLANK);
        p.fill(x0, top(6), z0, x1, top(6), z1, "smooth_stone");
        p.walls(x0, top(7), z0, x1, top(7), z1, "white_concrete");
        p.walls(x0, top(3), z0, x1, top(3), z1, "white_terracotta");
        p.clear(27, Y0 + 1, z1, 29, Y0 + 2, z1);                     // door
        p.fill(27, top(4), z1, 29, top(5), z1, "iron_bars[east=true,west=true]");
        p.clear(x0, Y0 + 1, 4, x0, Y0 + 2, 4);                       // side door to the west
        for (int y = Y0 + 1; y <= top(6); y++) p.set(30, y, 2, "ladder[facing=west]");
        p.clear(30, top(3), 2, 30, top(3), 2);
        p.set(30, top(3), 2, "ladder[facing=west]");
        p.set(30, top(6), 2, "ladder[facing=west]");
        p.fill(26, Y0 + 1, 2, 27, Y0 + 1, 2, "spruce_slab");
        p.set(26, Y0 + 1, 6, "barrel[facing=up]");
    }

    /** NE dark lounge with a purple interior (022456, 022828) and a cream bed shape on the roof (022640). */
    private void lounge(Painter p) {
        int x0 = 33, x1 = 39, z0 = 1, z1 = 10;
        p.walls(x0, Y0 + 1, z0, x1, top(4), z1, DARK2);
        p.fill(x0, top(5), z0, x1, top(5), z1, DARK);
        p.fill(x0 + 1, Y0, z0 + 1, x1 - 1, Y0, z1 - 1, "purple_wool");
        p.clear(x0, Y0 + 1, 4, x0, Y0 + 3, 6);                       // west opening
        p.clear(35, Y0 + 1, z1, 37, Y0 + 3, z1);                     // south opening
        p.fill(37, Y0 + 1, 2, 38, Y0 + 1, 3, "blue_wool");           // sofas / beds
        p.fill(35, Y0 + 1, 2, 35, Y0 + 1, 3, "purple_wool");
        p.set(38, Y0 + 1, 8, "purple_wool");
        p.set(36, top(4), 5, "lantern[hanging=true]");
        p.fill(35, top(5) + 1, 3, 37, top(5) + 1, 4, "yellow_terracotta");
        p.fill(35, top(5) + 1, 5, 37, top(5) + 1, 5, "white_wool");
        // bridge from the east platform (h4) to the lounge roof (h5)
        p.fill(34, Y0 + 1, 11, 36, top(4), 12, PLANK);
    }

    /** Dark "face" building with white slab teeth and eyes, N-S passage under it (wiki render, d010). */
    private void faceBuilding(Painter p) {
        int x0 = 14, x1 = 24, z0 = 8, z1 = 12;
        p.fill(x0, Y0 + 1, z0, x1, top(6), z1, DARK2);
        p.fill(x0, top(6), z0, x1, top(6), z1, DARK);
        p.clear(19, Y0 + 1, z0, 20, Y0 + 3, z1);                     // passage from the north cage
        p.clear(x0 + 1, Y0 + 1, 10, x1 - 1, Y0 + 2, 10);             // E-W crawl corridor inside
        p.set(x0, Y0 + 1, 10, "air"); p.set(x0, Y0 + 2, 10, "air");
        p.set(x1, Y0 + 1, 10, "air"); p.set(x1, Y0 + 2, 10, "air");
        for (int x = x0 + 1; x <= x1 - 1; x += 2) p.set(x, top(4), z1 + 1, "smooth_quartz_slab[type=top]");
        p.fill(16, top(5), z1, 17, top(5), z1, "white_concrete");
        p.fill(21, top(5), z1, 22, top(5), z1, "white_concrete");
        p.fill(18, top(6) + 1, 9, 21, top(6) + 1, 11, "cyan_stained_glass");
    }

    // ------------------------------------------------------------------ west band

    /** Long white building with a black/white louver roof, walk-through inside, ladder on the east face (022412). */
    private void louver(Painter p) {
        int x0 = 1, x1 = 5, z0 = 11, z1 = 32;
        p.walls(x0, Y0 + 1, z0, x1, top(3), z1, "white_concrete");
        for (int x = x0; x <= x1; x++)
            for (int z = z0; z <= z1; z++) p.set(x, top(4), z, (x % 2 == 0) ? "black_concrete" : "white_concrete");
        p.clear(x1, Y0 + 1, 15, x1, Y0 + 2, 16);
        p.clear(x1, Y0 + 1, 26, x1, Y0 + 2, 27);
        p.clear(3, Y0 + 1, z0, 3, Y0 + 2, z0);
        p.clear(3, Y0 + 1, z1, 3, Y0 + 2, z1);
        for (int y = Y0 + 1; y <= top(4); y++) p.set(6, y, 21, "ladder[facing=east]");
        for (int z = z0 + 2; z <= z1 - 2; z += 4) p.set(3, top(3), z, "sea_lantern");
    }

    private void cube(Painter p, int ax, int az, int h, String mat) {
        p.fill(ax, Y0 + 1, az, ax + 1, top(h), az + 1, mat);
    }

    /** Purple/blue/teal toy-block climb up the west side to a teal column, oak beam to the face roof (022412, d026). */
    private void westToyBlocks(Painter p) {
        cube(p, 11, 22, 1, GO);
        cube(p, 9, 20, 2, "purple_wool");
        cube(p, 7, 18, 3, "blue_wool");
        cube(p, 9, 16, 4, "magenta_wool");
        cube(p, 7, 14, 5, "purple_wool");
        cube(p, 9, 12, 6, GO);
        p.fill(9, top(4), 12, 10, top(4), 13, PLANK);                // oak belt on the teal column (d026)
        p.fill(11, top(6), 12, 13, top(6), 12, PLANK);               // beam to the face building roof
        p.fill(7, top(3) - 1, 18, 8, top(3) - 1, 19, "light_blue_wool");
    }

    /** Light brick pillars climbing 2..5 with chains between them (d010, d026, u21_0342). */
    private void chainPillars(Painter p) {
        int[][] ps = {{10, 28, 2}, {10, 26, 3}, {12, 26, 4}, {12, 28, 5}};
        for (int[] q : ps) {
            p.fill(q[0], Y0 + 1, q[1], q[0], top(q[2]), q[1], LIGHT_BRICK);
            p.set(q[0], top(q[2]), q[1], "chiseled_stone_bricks");
        }
        p.set(11, top(3), 26, "chain[axis=x]");
        p.set(12, top(4), 27, "chain[axis=z]");
        p.set(10, top(2), 27, "chain[axis=z]");
        p.set(9, Y0 + 1, 29, PLANK);
        p.fill(13, top(5), 27, 14, top(5), 27, PLANK);               // ledge on to the gate's west arm
        p.fill(13, Y0 + 1, 27, 13, top(5) - 1, 27, LIGHT_BRICK);
    }

    // ------------------------------------------------------------------ centre

    /** Blackstone gate: core tower h8 with an E-W arch (main chokepoint), side arms h5, cyan skylight (wiki render, d018). */
    private void centralGate(Painter p) {
        // arms
        p.fill(15, Y0 + 1, 17, 16, top(5), 26, BRICK);
        p.fill(23, Y0 + 1, 17, 24, top(5), 26, BRICK);
        p.fill(15, top(5), 17, 16, top(5), 26, DARK);
        p.fill(23, top(5), 17, 24, top(5), 26, DARK);
        // core
        p.fill(18, Y0 + 1, 18, 21, top(8), 25, BRICK);
        p.fill(18, top(8), 18, 21, top(8), 25, DARK);
        p.fill(19, top(8), 21, 20, top(8), 22, "cyan_stained_glass");
        // the arch through everything at az 21..22
        p.clear(15, Y0 + 1, 21, 24, Y0 + 3, 22);
        p.fill(15, Y0, 21, 24, Y0, 22, "packed_ice");
        p.fill(18, Y0 + 4, 21, 21, Y0 + 4, 22, LIGHT_BRICK);
        // white eyes on the north face, cyan window on the south face
        p.fill(18, top(7), 17, 19, top(7), 17, "smooth_quartz_slab[type=bottom]");
        p.fill(20, top(7), 17, 21, top(7), 17, "smooth_quartz_slab[type=bottom]");
        p.fill(19, top(5), 26, 20, top(6), 26, "cyan_stained_glass");
        // steps from the west arm (h5) to the core (h8) in the west alley
        p.fill(17, Y0 + 1, 26, 17, top(6), 26, GO);
        p.fill(17, Y0 + 1, 25, 17, top(7), 25, GO);
        // and from the street to the east arm
        p.fill(22, Y0 + 1, 29, 22, top(1), 29, PLANK);
        p.fill(23, Y0 + 1, 29, 24, top(2), 29, "blue_wool");
        p.fill(23, Y0 + 1, 28, 24, top(3), 28, "purple_wool");
        p.fill(23, Y0 + 1, 27, 24, top(4), 27, "blue_wool");
        // and to the west arm from the north
        p.fill(15, Y0 + 1, 13, 16, top(1), 13, PLANK);
        p.fill(15, Y0 + 1, 14, 16, top(2), 14, PLANK);
        p.fill(15, Y0 + 1, 15, 16, top(3), 15, "purple_wool");
        p.fill(15, Y0 + 1, 16, 16, top(4), 16, "blue_wool");
    }

    // ------------------------------------------------------------------ east band

    /** Dark platform h4 with a purple stripe, a covered blue-ice tunnel through it (u21_0339) and ice tiles on top. */
    private void eastPlatform(Painter p) {
        int x0 = 31, x1 = 39, z0 = 13, z1 = 33;
        p.fill(x0, Y0 + 1, z0, x1, top(4), z1, DARK2);
        p.fill(x0, top(4), z0, x1, top(4), z1, DARK);
        p.fill(x0, Y0 + 2, 15, x0, Y0 + 3, 31, "purple_concrete");
        p.fill(x0, Y0 + 1, 15, x0, Y0 + 1, 31, TEAL);
        // ice tunnel N-S
        p.clear(34, Y0 + 1, z0, 36, Y0 + 3, z1);
        p.fill(34, Y0, z0, 36, Y0, z1, "packed_ice");
        p.fill(33, Y0 + 1, z0 + 1, 33, Y0 + 3, z1 - 1, LIGHT_BRICK);
        p.fill(37, Y0 + 1, z0 + 1, 37, Y0 + 3, z1 - 1, LIGHT_BRICK);
        p.fill(34, top(4), z0, 36, top(4), z1, "polished_blackstone");
        // side entrance from the street into the tunnel
        p.clear(x0, Y0 + 1, 23, 33, Y0 + 3, 24);
        p.fill(x0, Y0, 23, 33, Y0, 24, "packed_ice");
        // ice squares on the roof (022412 light blue on the dark platform)
        p.fill(32, top(4), 28, 33, top(4), 31, "blue_ice");
        p.fill(38, top(4), 17, 38, top(4), 20, "blue_ice");
        // a cross street splits it into two dark buildings (022412 right side shows two blocks with a gap)
        p.clear(x0, Y0 + 1, 22, x1 - 1, top(4), 25);
        for (int z = 22; z <= 25; z++) for (int x = x0; x < x1; x++) p.set(x, Y0, z, "terracotta");
        // white window panels on the street faces (d010 right, 022640)
        for (int z : new int[]{14, 18, 27, 31}) { p.fill(x0, top(3), z, x0, top(3), z + 1, "white_stained_glass"); }
        for (int x : new int[]{32, 37}) { p.fill(x, top(3), 21, x + 1, top(3), 21, "white_stained_glass"); p.fill(x, top(3), 26, x + 1, top(3), 26, "white_stained_glass"); }
        // steps up the west face
        p.fill(30, Y0 + 1, 30, 30, top(1), 30, GO);
        p.fill(30, Y0 + 1, 29, 30, top(2), 29, GO);
        p.fill(30, Y0 + 1, 28, 30, top(3), 28, GO);
        p.fill(30, Y0 + 1, 16, 30, top(2), 16, PLANK);
        p.fill(29, Y0 + 1, 16, 29, top(1), 16, PLANK);
        p.fill(30, Y0 + 1, 15, 30, top(3), 15, "purple_wool");
    }

    // ------------------------------------------------------------------ south band

    /** White dot planter boxes on dark posts, rim at h3 (022412 lower left, d026). */
    private void dicePlanters(Painter p) {
        int[][] boxes = {{6, 31}, {10, 31}, {6, 35}, {10, 35}, {8, 38}};
        for (int[] b : boxes) {
            p.fill(b[0] + 1, Y0 + 1, b[1] + 1, b[0] + 1, top(1), b[1] + 1, "polished_blackstone");
            p.walls(b[0], top(2), b[1], b[0] + 2, top(3), b[1] + 2, "smooth_quartz");
            p.set(b[0] + 1, top(2), b[1] + 1, "gray_concrete");
            p.set(b[0] + 1, top(3), b[1] + 1, "air");
            p.set(b[0] + 1, top(3), b[1], "chiseled_quartz_block");
        }
        p.fill(9, Y0 + 1, 34, 9, top(2), 34, PLANK);
        p.set(9, Y0 + 1, 33, PLANK);
    }

    /** The big purple/pink/blue toy-block staircase climbing from SW (h1) to NE (h7) (022412, 022640). */
    private void toyStair(Painter p) {
        String[] cols = {"purple_wool", "blue_wool", "magenta_wool", "light_blue_wool", "purple_wool", GO, "blue_wool"};
        for (int i = 0; i < 7; i++) {
            int x = 16 + 2 * i;
            int z = 37 - i;
            for (int k = 0; k < 2; k++) {
                String m = cols[(i + k) % cols.length];
                p.fill(x, Y0 + 1, z + 2 * k, x + 1, top(i + 1), z + 2 * k + 1, m);
            }
        }
        // a lower second line in front so the climb is reachable from the street on both sides
        cube(p, 18, 41, 1, GO);
        cube(p, 28, 35, 3, "magenta_wool");
        cube(p, 28, 37, 2, "light_blue_wool");
        cube(p, 28, 39, 1, GO);
    }

    /** Ice rink at street level with a quartz kerb (022700, 022704), dark block in the corner with steps. */
    private void southEast(Painter p) {
        p.fill(32, Y0, 36, 38, Y0, 43, "blue_ice");
        p.walls(31, Y0 + 1, 35, 38, Y0 + 1, 44, "quartz_slab[type=bottom]");
        p.clear(31, Y0 + 1, 39, 31, Y0 + 1, 40);
        p.clear(34, Y0 + 1, 35, 36, Y0 + 1, 35);
        p.fill(32, Y0 + 1, 45, 39, top(3), 47, BRICK);
        p.fill(32, top(3), 45, 39, top(3), 46, TEAL);
        p.fill(31, Y0 + 1, 46, 31, top(1), 46, GO);
        p.fill(31, Y0 + 1, 45, 31, top(2), 45, GO);
        p.set(31, Y0 + 1, 47, BRICK);
    }

    /** Red carpet band and the teal step ramp up to the south rim (022412 bottom left). */
    private void southWest(Painter p) {
        p.fill(1, Y0, 41, 12, Y0, 43, "red_concrete");
        for (int x = 2; x <= 13; x++) {
            p.fill(x, Y0 + 1, 44, x, top(1), 44, GO);
            p.fill(x, Y0 + 1, 45, x, top(2), 45, GO);
            p.fill(x, Y0 + 1, 46, x, top(3), 46, TEAL);
        }
    }

    /** Oak crates: wood = safe ledge (022328, d010). */
    private void crates(Painter p) {
        int[][] cs = {{6, 9}, {15, 7}, {23, 13}, {13, 24}, {27, 22}, {28, 14}, {8, 29}, {26, 30}, {30, 9}, {14, 42}, {30, 44}};
        for (int[] c : cs) p.set(c[0], Y0 + 1, c[1], PLANK);
        p.set(27, Y0 + 2, 22, PLANK);
    }

    /**
     * The small MCC street furniture that fills the pit (022412, 022640, d010, d026): white window-box lamps on dark
     * posts, teal/purple toy stacks and oak crates in the open streets, hanging chains between buildings.
     */
    private void props(Painter p) {
        // window-box lamps: dark post, white glass box with a grey lid (1x1, standable at h3)
        int[][] lamps = {{15, 4}, {17, 6}, {23, 5}, {16, 42}, {24, 43}, {27, 42}, {3, 37}, {3, 34}, {37, 34}, {29, 11}};
        for (int[] l : lamps) {
            p.fill(l[0], Y0 + 1, l[1], l[0], top(1), l[1], "polished_blackstone");
            p.set(l[0], top(2), l[1], "white_stained_glass");
            p.set(l[0], top(3), l[1], "smooth_quartz");
        }
        // toy stacks in the east street (go / jump colours), stepping up to the north dark building
        cube(p, 28, 18, 1, GO);
        cube(p, 28, 20, 2, "purple_wool");
        p.fill(28, Y0 + 1, 22, 29, top(3), 22, "blue_wool");
        cube(p, 26, 25, 1, "magenta_wool");
        cube(p, 28, 26, 2, GO);
        // west street: purple block and a teal step against the louver
        cube(p, 7, 24, 2, "purple_wool");
        p.fill(6, Y0 + 1, 13, 6, top(1), 13, GO);
        // north street props
        p.fill(16, Y0 + 1, 3, 17, top(2), 3, "purple_wool");
        p.fill(22, Y0 + 1, 3, 23, top(1), 4, GO);
        // south street: crates and a teal block by the toy stair
        p.fill(14, Y0 + 1, 37, 15, top(2), 37, PLANK);
        p.set(13, Y0 + 1, 38, PLANK);
        // hanging chains: lounge to kiosk, louver to the west toy blocks, gate arms
        for (int y = top(5) - 2; y <= top(5) - 1; y++) { p.set(32, y, 3, "chain[axis=y]"); p.set(6, y, 26, "chain[axis=y]"); }
        p.fill(17, top(5), 20, 17, top(5), 20, "chain[axis=x]");
        p.fill(22, top(5), 23, 22, top(5), 23, "chain[axis=x]");
    }

    /** Blue steps down from each cage door to the street (022440). */
    private void cageRamps(Painter p) {
        for (int x = 19; x <= 21; x++) {
            p.fill(x, Y0 + 1, 1, x, top(2), 1, "blue_concrete");
            p.fill(x, Y0 + 1, 2, x, top(1), 2, "blue_concrete");
            p.fill(x, Y0 + 1, D - 2, x, top(2), D - 2, "blue_concrete");
            p.fill(x, Y0 + 1, D - 3, x, top(1), D - 3, "blue_concrete");
        }
    }

    /** Barrier ring on the wall top and a barrier roof over the pit (players cannot leave; spectators fly through). */
    private void containment(Painter p) {
        int roof = Y0 + 26;
        for (int x = -1; x <= W; x++)
            for (int z = -1; z <= D; z++) {
                boolean ring = x == -1 || x == W || z == -1 || z == D;
                if (ring) p.fill(x, Y0 + 10, z, x, roof, z, "barrier");
                p.set(x, roof, z, "barrier");
            }
    }
}
