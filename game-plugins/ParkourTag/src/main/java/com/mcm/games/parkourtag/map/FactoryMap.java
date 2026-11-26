package com.mcm.games.parkourtag.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory (MCC 14, the first non-box Parkour Tag arena), recreated from the official Noxcrew MCC 14 update top-down
 * (research/mcc_archive/parkour_tag/pt-3/frames/m14_041*_factory_topdown_*.jpg, levelled 33 degrees and traced at
 * about 26 px per block), plus the official render and POV stills (pt-1/frames/parkourtag_factory_*.png,
 * 000044/000046/000049) and Zibly's Factory rebuild. The layout is the PLAN table below; tools/factory_plan.py draws
 * previews/factory_trace.png from it.
 *
 * <p>Orientation: north (az 0) has the slate roof and the grey/cyan block, the orange pipe crosses the east half,
 * the green clock-face yard is south-east and the sand ramp roof is south-west. The two cages are the loading bays at
 * the north and south ends (the reference shows no cage position, so those two are the one assumption).
 * Palette: light-grey stone floor and walls, red slab awnings on white pillars, orange pipes, yellow/black hazard
 * checks, cyan windows, green wall, blue banner walls.
 */
public final class FactoryMap implements PtMap {
    static final int M = 8;
    static final int W = 34, D = 32;
    static final int Y0 = 1;
    static final int WALL = 11;

    /** kind name ax0 az0 ax1 az1 h | source. h = standing height above the street. */
    public static final String PLAN = """
            slate  slateRoof    8  1 14  6 7 | m14_0416 dark slate stepped roof (top centre), rising north
            grey   neBlock     20  0 25  3 8 | m14_0416 big grey block (top right) with cyan strip
            cyan   neCyan      23  4 27  5 5 | m14_0416 cyan windows right of it
            grey   nwVent       3  5  7  9 7 | m14_0416 grey block with fan vent (top left)
            wood   redRoof      1 10  5 14 5 | m14_0416 brown-red roof house (west)
            yellow yellowAwn    1 15  4 17 3 | m14_0416 yellow awning under it
            white  westWall     6 15  7 24 6 | m14_0416 white vent-panel wall (west corridor)
            orange swOrange     0 20  3 23 3 | m14_0416 small orange roof (south-west)
            cyan   swTeal       3 24  6 27 3 | m14_0416 teal block (south-west)
            rampS  sandRoof     7 23 13 29 5 | m14_0416 sand roof rising south (bottom left); Zibly sand awning
            green  clockYard   25 24 32 31 4 | m14_0416 dark green wall roof with two clock faces (south-east)
            grey   centreGrey  19 25 24 29 4 | m14_0416 grey block south of centre
            orange pipeEast    20 18 33 20 6 | m14_0416 giant orange pipe across the east half
            orange pipeStem    16 22 17 26 4 | m14_0416 orange pipe stem going south
            orange pipeElbow   16 27 19 29 3 | m14_0416 orange elbow along the south; owner: was x13-19 and sealed the south cage in, now a 2-wide street (x14-15) runs from the cage north
            orange archA       23  5 27  7 6 | m14_0416 orange arch pipes (upper right)
            orange archB       26  9 31 11 5 | m14_0416 second orange arch
            leaf   leaves      21  6 23  8 3 | m14_0416 leaf block cluster
            cyan   cyanStrip   20 12 28 13 3 | m14_0416 cyan wall strip (right of the dark court)
            red    ventBlocks  30 13 32 16 2 | m14_0416 red vent blocks east edge
            floor  darkCourt   21  8 28 16 0 | m14_0416 black loading-dock floor (right of centre)
            hazard hazardW      7 12  8 14 0 | m14_0416 yellow/black tape (west)
            hazard hazardS     14 28 18 28 0 | m14_0416 yellow/black tape (south)
            pad    pad01       13  8 14  9 2 | m14_0416 red slab awning centre
            pad    pad02       10  9 11 10 3 | m14_0416 red awning
            pad    pad03       13 12 14 13 2 | m14_0416 red awning
            pad    pad04       15 14 17 16 3 | m14_0416 big red/white box (centre)
            pad    pad05        9 17 10 18 2 | m14_0416 red awning
            pad    pad06       11 19 12 20 3 | m14_0416 red awning
            pad    pad07       14 19 15 20 2 | m14_0416 red awning
            pad    pad08       12 21 13 22 3 | m14_0416 red awning
            pad    pad09       18 13 19 14 2 | m14_0416 red awning (right of centre)
            pad    pad10       20 11 21 12 3 | m14_0416 red awning
            pad    pad11       16  8 17  9 3 | m14_0416 red awning
            pad    pad12       17 16 18 17 2 | m14_0416 red awning
            pad    pad13       17 20 18 21 3 | m14_0416 red awning
            pad    pad14       19 22 20 23 2 | m14_0416 red awning
            step   crate01     12  6 12  6 1 | m14_0416 crate by the slate roof
            step   crate02     11 12 11 12 1 | m14_0416 crate
            step   crate03     15 18 15 18 1 | m14_0416 crate
            step   crate04     19 15 19 15 1 | m14_0416 crate
            step   crate05      9 20  9 20 1 | m14_0416 crate
            step   crate06     16 11 16 11 1 | m14_0416 crate
            step   crate07     22 17 22 17 1 | m14_0416 crate by the pipe
            step   crate08     29 12 29 12 1 | m14_0416 crate east
            step   crate09      8 11  8 11 1 | m14_0416 crate west
            step   crate10     23 22 23 22 1 | m14_0416 crate
            step   crate11     12 26 12 26 1 | m14_0416 crate by the pipe elbow
            step   stair01     13 26 13 26 2 | m14_0416 step up to the elbow
            step   crate12     10 22 10 22 1 | m14_0416 crate by the sand roof
            step   crate13     11  7 11  7 1 | m14_0416 crate by the slate roof
            step   bayStep1    17 30 17 30 1 | owner: second way out of the south bay, crates up onto the pipe elbow
            step   bayStep2    18 30 18 30 2 | owner: second way out of the south bay
            bridge bridgeN1    15  1 16  1 7 | owner: upper parkour loop - slate roof to the grey block, 2-gap jump
            bridge bridgeN2    19  1 19  1 7 | owner: upper loop (then up onto the grey block)
            bridge bridgeNW     7  1  7  4 7 | owner: upper loop - fan-vent block to the slate roof top
            pad    padNW        2  9  2  9 6 | owner: upper loop - red roof up to the fan-vent block
            bridge bridgeW      6 14  6 14 5 | owner: upper loop - white wall down to the red roof
            pad    padW1        8 22  8 22 3 | owner: upper loop - pad chain from the sand roof up to the white wall
            pad    padW2        8 20  8 20 4 | owner: upper loop
            pad    padW3        8 18  8 18 5 | owner: upper loop
            bridge bridgeS1    14 26 15 26 4 | owner: upper loop - sand roof to the pipe stem over the new street
            bridge bridgeSE1   21 21 21 21 5 | owner: upper loop - big pipe down to the grey block, 2-gap jump
            bridge bridgeSE2   21 24 21 24 5 | owner: upper loop
            bridge bridgeE1    29 12 29 13 5 | owner: upper loop - second arch to the big pipe, 2-gap jump
            bridge bridgeE2    29 16 29 17 5 | owner: upper loop
            """;

    record Feature(String kind, String name, int x0, int z0, int x1, int z1, int h) {}

    static final List<Feature> FEATURES = parse();

    private static List<Feature> parse() {
        List<Feature> out = new ArrayList<>();
        for (String line : PLAN.split("\n")) {
            String[] parts = line.split("\\|")[0].trim().split("\\s+");
            if (parts.length < 7) continue;
            out.add(new Feature(parts[0], parts[1], Integer.parseInt(parts[2]), Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]), Integer.parseInt(parts[5]), Integer.parseInt(parts[6])));
        }
        return out;
    }

    private final List<Cage> cages = List.of(
            new Cage(M + 17, Y0 + 1, M - 1, 0, 1, "orange_concrete", "black_concrete"),
            new Cage(M + 17, Y0 + 1, M + D, 0, -1, "orange_concrete", "black_concrete"));

    @Override public String id() { return "factory"; }
    @Override public String displayName() { return "Factory"; }
    @Override public boolean finished() { return true; }
    @Override public int sizeX() { return W + 2 * M; }
    @Override public int sizeY() { return Y0 + 30; }
    @Override public int sizeZ() { return D + 2 * M; }
    @Override public List<Cage> cages() { return cages; }
    @Override public int[] playBox() { return new int[]{M, M, M + W - 1, M + D - 1}; }
    @Override public double[] spectator() { return new double[]{M + 17.5, Y0 + 18, M + 16.5, 0, 90}; }
    @Override public boolean pitKeep(int x, int y, int z, String b) {
        boolean outside = x < M || x >= M + W || z < M || z >= M + D;
        return !(outside && (b.contains("stone_bricks") || b.contains("blue_concrete") || b.contains("green_concrete")
                || b.contains("light_gray_concrete") || b.contains("white_concrete")));
    }

    private static int top(int h) { return Y0 + h; }

    /** Corner chamfers: the footprint is an octagon-ish yard (the MCC perimeter steps in and out). */
    private static boolean cut(int x, int z) {
        int c = 5;
        return x + z < c || (W - 1 - x) + z < c || x + (D - 1 - z) < c || (W - 1 - x) + (D - 1 - z) < c;
    }

    @Override
    public void generate(BlockSink sink) {
        Painter p = new Painter(sink, M);
        floor(p);
        enclosure(p);
        for (Feature f : FEATURES) building(p, f);
        details(p);
        for (Cage c : cages) {
            for (int l = -3; l <= 3; l++) for (int d = 0; d <= Cage.DEPTH + 1; d++) {
                int[] a = c.at(l, d, 0);
                for (int y = 0; y < a[1] - 1; y++) sink.set(a[0], y, a[2], "stone_bricks");
            }
            c.build(sink);
            // hazard checks around the loading-bay door (reference: black/yellow bay frames)
            for (int l = -3; l <= 3; l++) for (int dy = -1; dy <= Cage.HEIGHT; dy++) {
                boolean frame = Math.abs(l) == 3 || dy == Cage.HEIGHT;
                if (!frame) continue;
                int[] a = c.at(l, 0, dy);
                sink.set(a[0], a[1], a[2], (Math.floorMod(l + dy, 2) == 0) ? "yellow_concrete" : "black_concrete");
            }
        }
        containment(p);
    }

    private void floor(Painter p) {
        p.fill(0, 0, 0, W - 1, 0, D - 1, "stone");
        for (int x = 0; x < W; x++)
            for (int z = 0; z < D; z++) {
                int hsh = Math.floorMod(x * 29 + z * 13, 9);
                String b = hsh == 0 ? "andesite" : (hsh == 1 ? "smooth_stone" : "light_gray_concrete");
                if (hsh >= 2 && (x % 6 == 0 || z % 6 == 0)) b = "polished_andesite";   // paving seams
                p.set(x, Y0, z, b);
            }
    }

    /** Stone-brick facade walls with windows and banner bands; the corner chamfers are solid blocks. */
    private void enclosure(Painter p) {
        int h = Y0 + WALL;
        for (int x = -2; x <= W + 1; x++)
            for (int z = -2; z <= D + 1; z++) {
                boolean inside = x >= 0 && x < W && z >= 0 && z < D && !cut(x, z);
                if (inside) continue;
                p.fill(x, 0, z, x, h, z, "stone_bricks");
            }
        // chamfer fill is also floor-level solid: top it with grey roof
        for (int x = 0; x < W; x++) for (int z = 0; z < D; z++) if (cut(x, z)) p.set(x, h, z, "light_gray_concrete");
        // banner bands on the four inner faces: blue (north/south), green (west), grey windows (east)
        for (int x = 0; x < W; x++)
            for (int y = Y0 + 5; y <= Y0 + 9; y++) {
                p.set(x, y, -1, "blue_concrete");
                p.set(x, y, D, "blue_concrete");
            }
        for (int z = 0; z < D; z++)
            for (int y = Y0 + 4; y <= Y0 + 9; y++) {
                p.set(-1, y, z, "green_concrete");
                if (z % 4 != 1) p.set(W, y, z, "stone_bricks"); else p.set(W, y, z, "black_stained_glass");
            }
        // MCC lettering on the north banner (white blocks, seen from the court), south banner repeated
        p.text("MCC", 4, Y0 + 9, -1, 1, 0, "white_concrete", null);
        p.text("MCC", 25, Y0 + 9, D, 1, 0, "white_concrete", null);
        // orange / black hazard band along the base of the north and south walls
        for (int x = 0; x < W; x++) {
            p.set(x, Y0 + 1, -1, Math.floorMod(x, 2) == 0 ? "yellow_concrete" : "black_concrete");
            p.set(x, Y0 + 1, D, Math.floorMod(x, 2) == 0 ? "yellow_concrete" : "black_concrete");
        }
        // the blue pylon (landmark): white column with blue banner and orange pipe elbows, east end of the pipe
        p.fill(32, Y0 + 1, 17, 33, Y0 + 24, 18, "white_concrete");
        p.fill(32, Y0 + 6, 16, 33, Y0 + 24, 16, "blue_concrete");
        p.fill(32, Y0 + 24, 14, 33, Y0 + 24, 20, "orange_concrete");
    }

    private void building(Painter p, Feature f) {
        int h = f.h();
        switch (f.kind()) {
            case "floor" -> {
                for (int x = f.x0(); x <= f.x1(); x++) for (int z = f.z0(); z <= f.z1(); z++) p.set(x, Y0, z, "black_concrete");
            }
            case "hazard" -> {
                for (int x = f.x0(); x <= f.x1(); x++) for (int z = f.z0(); z <= f.z1(); z++)
                    p.set(x, Y0, z, Math.floorMod(x + z, 2) == 0 ? "yellow_concrete" : "black_concrete");
            }
            case "step" -> p.fill(f.x0(), Y0 + 1, f.z0(), f.x1(), top(h), f.z1(), "spruce_planks");
            case "bridge" -> {   // catwalk: only the walking layer, so the street underneath stays open
                for (int x = f.x0(); x <= f.x1(); x++) for (int z = f.z0(); z <= f.z1(); z++)
                    p.set(x, top(h), z, Math.floorMod(x + z, 2) == 0 ? "yellow_concrete" : "black_concrete");
            }
            case "pad" -> {
                p.fill(f.x0(), Y0 + 1, f.z0(), f.x1(), top(h) - 1, f.z1(), "white_concrete");
                p.fill(f.x0(), top(h), f.z0(), f.x1(), top(h), f.z1(), "red_concrete");
            }
            case "slate" -> {
                int rows = f.z1() - f.z0() + 1;   // highest at the north row
                for (int i = 0; i < rows; i++) {
                    int z = f.z0() + i, hh = h - i;
                    p.fill(f.x0(), Y0 + 1, z, f.x1(), top(hh) - 1, z, "deepslate_tiles");
                    p.fill(f.x0(), top(hh), z, f.x1(), top(hh), z, "polished_deepslate");
                }
            }
            case "rampS" -> {
                int rows = f.z1() - f.z0() + 1;   // highest at the south row
                for (int i = 0; i < rows; i++) {
                    int z = f.z0() + i, hh = Math.max(1, h - (rows - 1 - i) / 2 * 1 - (rows - 1 - i) % 2 * 0);
                    hh = Math.max(2, h - (rows - 1 - i) / 2);
                    p.fill(f.x0(), Y0 + 1, z, f.x1(), top(hh) - 1, z, "smooth_sandstone");
                    p.fill(f.x0(), top(hh), z, f.x1(), top(hh), z, "cut_sandstone");
                }
            }
            default -> {
                String body = switch (f.kind()) {
                    case "white" -> "white_concrete";
                    case "cyan" -> "cyan_concrete";
                    case "green" -> "green_concrete";
                    case "orange" -> "orange_concrete";
                    case "wood" -> "red_terracotta";
                    case "yellow" -> "yellow_concrete";
                    case "red" -> "red_concrete";
                    case "leaf" -> "oak_leaves[persistent=true]";
                    default -> "stone_bricks";
                };
                String roof = switch (f.kind()) {
                    case "grey" -> "light_gray_concrete";
                    case "wood" -> "red_terracotta";
                    case "leaf" -> "oak_leaves[persistent=true]";
                    default -> body;
                };
                p.fill(f.x0(), Y0 + 1, f.z0(), f.x1(), top(h) - 1, f.z1(), body);
                p.fill(f.x0(), top(h), f.z0(), f.x1(), top(h), f.z1(), roof);
                if (f.kind().equals("grey") || f.kind().equals("white") || f.kind().equals("cyan")) {
                    for (int x = f.x0(); x <= f.x1(); x++)
                        for (int z = f.z0(); z <= f.z1(); z++) {
                            boolean edge = x == f.x0() || x == f.x1() || z == f.z0() || z == f.z1();
                            if (edge && h >= 4 && (x + z) % 3 == 1) p.set(x, Y0 + 3, z, "black_stained_glass");
                        }
                }
            }
        }
    }

    private void details(Painter p) {
        // clock faces on the green yard roof (two, white ring with black hands)
        for (int[] c : new int[][]{{28, 26}, {28, 29}}) {
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
                p.set(c[0] + dx, top(4), c[1] + dz, (dx == 0 && dz == 0) ? "black_concrete" : "white_concrete");
        }
        // grey parapet ring on the green yard (reference: grey wall ring)
        for (int x = 25; x <= 32; x++) for (int z = 24; z <= 31; z++) {
            boolean edge = x == 25 || x == 32 || z == 24 || z == 31;
            if (edge) p.set(x, top(5), z, "stone_bricks");
        }
        // vent-panel dots on the west wall (white concrete with grey dots)
        for (int z = 15; z <= 24; z++) if (z % 2 == 0) p.set(6, Y0 + 3, z, "light_gray_concrete");
        // ladders up the facades (reference: ladders and iron rails link street, awnings and roofs): x, z, facing, roof h
        Object[][] ladders = {
                {8, 7, "east", 7}, {21, 4, "south", 8}, {6, 12, "east", 5}, {8, 19, "east", 6}, {28, 23, "north", 5},
                {21, 21, "south", 6}, {15, 24, "west", 4}, {24, 8, "west", 6}, {31, 12, "north", 5},
                {26, 4, "west", 5}, {3, 18, "east", 3}};
        for (Object[] l : ladders) {
            for (int y = Y0 + 1; y <= top((Integer) l[3]) + 1; y++) p.set((Integer) l[0], y, (Integer) l[1], "ladder[facing=" + l[2] + "]");
        }
    }

    private void containment(Painter p) {
        int roof = Y0 + 26;
        for (int x = -1; x <= W; x++)
            for (int z = -1; z <= D; z++) {
                boolean ring = x == -1 || x == W || z == -1 || z == D;
                if (ring) p.fill(x, Y0 + WALL + 1, z, x, roof, z, "barrier");
                p.set(x, roof, z, "barrier");
            }
    }
}
