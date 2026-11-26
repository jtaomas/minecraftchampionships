package com.mcm.games.parkourtag.map;

import java.util.ArrayList;
import java.util.List;

/**
 * Urban Jungle (MCC 12, Halloween/Pride variants later), recreated from the Zibly block-by-block rebuild's true
 * top-down and oblique frames (research/mcc_archive/parkour_tag/pt-3/frames/zib_*), the MCC 12 stills
 * (pt-2/frames/0157*..0203*, urbanjungle_wiki_render.png). The layout is the traced plan in PLAN below, which
 * tools/uj_plan.py reads to draw previews/urbanjungle_trace.png.
 *
 * <p>Orientation: north (az 0) = pink/purple window facade + cage, south = same, mirrored. The street is grey; the
 * play space is the red slab roofs (continuous roof-height route) plus paving steps and crates back up from the street.
 * Palette: light-blue sky walls with white clouds, red brick + white trim, red/orange roof slabs, grey stone brick,
 * black slate towers, one dark spire.
 */
public final class UrbanJungleMap implements PtMap {
    static final int M = 8;           // margin around the pit (wall ring, cages)
    static final int W = 28, D = 36;  // pit size
    static final int Y0 = 1;          // street surface block; feet stand at Y0 + 1
    static final int ROW = 5;         // north / south roof row (cage exit height)

    /** kind name ax0 az0 ax1 az1 h | source. Rectangles are inclusive; h = standing height above the street. */
    public static final String PLAN = """
            house  northRow     4  0 23  3 5 | zib_1424 zib_1421 dormer row with purple windows (north)
            house  southRow     4 32 23 35 5 | zib_1424 pink awning row (south, mirrored)
            slate  nwTower      0  0  3  4 8 | zib_1424 NW black slate tower (stepped)
            slate  neTower     24  0 27  4 8 | zib_1424 NE black slate tower
            slate  seTower     24 31 27 35 8 | zib_1424 SE black slate tower
            house  swHouse      0 31  3 35 5 | zib_1424 SW brick corner house
            stripe awningHouse  0  5  3 15 6 | zib_1424 west striped awning house, wiki render
            orange orangeW      4  6  5  8 3 | zib_1424 orange awning pieces west
            dark   darkBoxW     4 11  6 13 2 | zib_1424 dark grey box with crates
            orange kioskA       7  8 10 11 3 | zib_1424 orange kiosk roof (upper left)
            step   stepsA      11  8 11 11 1 | zib_1424 grey stone steps beside the kiosk
            house  midHouse    12  9 14 13 3 | zib_1424 red-capped brick house (centre north)
            orange orangeUp    12  4 14  6 3 | zib_1424 orange roof under the dormers
            step   stepsB      15  4 16  6 1 | zib_1424 planter and paving next to it
            house  redSmallA   17  6 19  8 3 | zib_1424 red slab roof (upper right)
            house  redNE       21  5 23  7 4 | zib_1424 red roof by the NE tower
            house  redB        24  7 26  9 3 | zib_1424 red roof east
            orange kioskB      17 11 20 13 3 | zib_1424 orange kiosk roof (right of centre)
            step   eastSteps1  21 10 22 14 1 | zib_1424 light stone paving
            step   eastSteps2  23 10 23 14 2 | zib_1424 light stone paving (higher)
            house  eastHouse   24 10 27 21 4 | zib_1424 east brick wall houses
            house  redLong     17 16 19 22 3 | zib_1424 long red slab roof east of the spire
            house  spireBase   12 16 15 19 3 | zib_1424 centre; wiki render brick base of the spire
            house  redLow      12 22 14 22 2 | zib_1424 small red slab wall south of the spire
            dark   darkE       21 21 23 24 2 | zib_1424 dark grey box east
            grey   swGrey       0 15  8 19 3 | zib_1424 SW stone-brick building
            orange swOrange1    0 16  3 18 4 | zib_1424 orange roof on it
            orange swOrange2    5 20  9 23 4 | zib_1424 orange roof south of it
            step   stepPocket   4 21  4 21 1 | zib_1424 crate/paving step in the SW alley
            grey   swTerrace    0 22  4 25 2 | zib_1424 grey terrace + planter
            white  swWhite      0 27  3 28 3 | zib_1424 white + red slab box
            house  swBrickRow   0 29  3 30 3 | zib_1424 SW brick row
            house  chimneyHouse 8 26 10 29 4 | zib_1424 chimney house, grey cap
            grey   southSteps  11 29 16 30 1 | zib_1424 grey stone steps south
            orange bigSE       17 26 23 30 4 | zib_1424 big orange hip roof (SE)
            house  redSE       24 25 26 28 3 | zib_1424 red roof east
            grey   seTerrace   24 29 27 30 2 | zib_1424 grey terrace by SE tower
            ring   spireDeck   11 15 16 20 6 | owner: upper parkour - walkway ring around the spire, above the roofs
            step   deckPadN1   18 14 18 14 4 | owner: pads from the orange kiosk up to the spire deck
            step   deckPadN2   16 14 16 14 5 | owner: pads up to the spire deck (2-gap jump)
            bridge westSpan1    4 14  6 14 5 | owner: high span from the striped awning house to the spire deck
            bridge westSpan2    9 14 10 14 5 | owner: high span, 2-gap jump in the middle
            bridge eastSpan1   23 19 23 19 5 | owner: high span from the east houses to the spire deck
            bridge eastSpan2   20 19 21 19 5 | owner: high span, 2-gap jump
            step   deckPadE    18 18 18 18 5 | owner: last hop onto the spire deck from the east span
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
            new Cage(M + 14, Y0 + ROW + 1, M - 1, 0, 1, "red_concrete", "spruce_planks"),
            new Cage(M + 14, Y0 + ROW + 1, M + D, 0, -1, "red_concrete", "spruce_planks"));

    @Override public String id() { return "urbanjungle"; }
    @Override public String displayName() { return "Urban Jungle"; }
    @Override public boolean finished() { return true; }
    @Override public int sizeX() { return W + 2 * M; }
    @Override public int sizeY() { return Y0 + 34; }
    @Override public int sizeZ() { return D + 2 * M; }
    @Override public List<Cage> cages() { return cages; }
    @Override public int[] playBox() { return new int[]{M, M, M + W - 1, M + D - 1}; }
    @Override public double[] spectator() { return new double[]{M + 14.5, Y0 + 20, M + 18.5, 0, 90}; }
    @Override public boolean pitKeep(int x, int y, int z, String b) {
        boolean outside = x < M || x >= M + W || z < M || z >= M + D;
        return !(outside && (b.contains("light_blue_concrete") || b.contains("white_concrete")));
    }

    private static int top(int h) { return Y0 + h; }

    @Override
    public void generate(BlockSink sink) {
        Painter p = new Painter(sink, M);
        street(p);
        enclosure(p);
        for (Feature f : FEATURES) building(p, f);
        details(p);
        chimneys(p);
        spire(p);
        for (Cage c : cages) {
            // brick pedestal under each cage so it does not float
            int fz = c.fz();
            for (int l = -3; l <= 3; l++) for (int d = 0; d <= Cage.DEPTH + 1; d++) {
                int[] a = c.at(l, d, 0);
                for (int y = 1; y < a[1] - 1 + 1; y++) sink.set(a[0], y, a[2], "bricks");
            }
            c.build(sink);
        }
        containment(p);
    }

    // ------------------------------------------------------------------ ground and sky walls

    private void street(Painter p) {
        p.fill(0, 0, 0, W - 1, 0, D - 1, "stone");
        for (int x = 0; x < W; x++)
            for (int z = 0; z < D; z++) {
                int hsh = Math.floorMod(x * 31 + z * 17, 11);
                p.set(x, Y0, z, hsh < 2 ? "polished_andesite" : (hsh == 2 ? "andesite" : "light_gray_concrete"));
            }
    }

    /** Light-blue sky walls with white clouds and a white frame band (MCC: painted sky box). */
    private void enclosure(Painter p) {
        int h = Y0 + 17;
        for (int x = -2; x <= W + 1; x++)
            for (int z = -2; z <= D + 1; z++) {
                if (x >= 0 && x < W && z >= 0 && z < D) continue;
                p.fill(x, 0, z, x, h, z, "light_blue_concrete");
            }
        // clouds: deterministic white blocks, 3 long, on the inner faces
        for (int i = 0; i < 26; i++) {
            int a = 2 + (i * 7) % (W - 5), b = 2 + (i * 11) % (D - 5);
            int y = Y0 + 8 + (i * 5) % 7;
            for (int k = 0; k < 3 + i % 3; k++) {
                if (i % 4 == 0) p.set(a + k, y, -1, "white_concrete");
                else if (i % 4 == 1) p.set(a + k, y, D, "white_concrete");
                else if (i % 4 == 2) p.set(-1, y, b + k, "white_concrete");
                else p.set(W, y, b + k, "white_concrete");
            }
        }
        // white picture frames either side of the cages (the cage windows sit in them)
        for (int x = 9; x <= 19; x++) for (int y = Y0 + 8; y <= Y0 + 15; y++) {
            boolean edge = x == 9 || x == 19 || y == Y0 + 8 || y == Y0 + 15;
            if (edge) { p.set(x, y, -1, "white_concrete"); p.set(x, y, D, "white_concrete"); }
        }
    }

    // ------------------------------------------------------------------ buildings

    private void building(Painter p, Feature f) {
        int h = f.h();
        switch (f.kind()) {
            case "step" -> p.fill(f.x0(), Y0 + 1, f.z0(), f.x1(), top(h), f.z1(), "stone_bricks");
            case "slate" -> slate(p, f);
            case "dark" -> { p.fill(f.x0(), Y0 + 1, f.z0(), f.x1(), top(h), f.z1(), "gray_concrete"); }
            case "bridge", "ring" -> {   // upper walkways: only the walking layer, the street and roofs below stay open
                boolean ring = f.kind().equals("ring");
                for (int x = f.x0(); x <= f.x1(); x++)
                    for (int z = f.z0(); z <= f.z1(); z++) {
                        if (ring && x != f.x0() && x != f.x1() && z != f.z0() && z != f.z1()) continue;
                        p.set(x, top(h), z, Math.floorMod(x + z, 3) == 0 ? "stripped_dark_oak_log[axis=y]" : "dark_oak_planks");
                    }
            }
            default -> {
                String body = switch (f.kind()) { case "grey" -> "stone_bricks"; case "white" -> "white_concrete"; default -> "bricks"; };
                String roof = switch (f.kind()) {
                    case "orange" -> "orange_concrete";
                    case "grey" -> "light_gray_concrete";
                    case "white" -> "red_concrete";
                    default -> "red_concrete";
                };
                p.fill(f.x0(), Y0 + 1, f.z0(), f.x1(), top(h) - 1, f.z1(), body);
                for (int x = f.x0(); x <= f.x1(); x++)
                    for (int z = f.z0(); z <= f.z1(); z++) {
                        String r = roof;
                        if (f.kind().equals("stripe")) r = (z % 2 == 0) ? "white_concrete" : "red_concrete";
                        p.set(x, top(h), z, r);
                    }
                // windows (black) on the perimeter, white cornice band
                for (int x = f.x0(); x <= f.x1(); x++)
                    for (int z = f.z0(); z <= f.z1(); z++) {
                        boolean edge = x == f.x0() || x == f.x1() || z == f.z0() || z == f.z1();
                        if (!edge) continue;
                        if (h >= 3 && (x + z) % 3 == 1) p.set(x, Y0 + 2, z, "black_concrete");
                        if (h >= 4) p.set(x, top(h) - 1, z, "white_concrete");
                    }
            }
        }
    }

    /** Black slate corner tower: stepped, one block per column, highest at the outer wall. */
    private void slate(Painter p, Feature f) {
        boolean west = (f.x0() + f.x1()) / 2 < W / 2;
        int cols = f.x1() - f.x0() + 1;
        for (int i = 0; i < cols; i++) {
            int x = west ? f.x0() + i : f.x1() - i;
            int h = f.h() - i;   // outer column = h, one block lower per column towards the street
            p.fill(x, Y0 + 1, f.z0(), x, top(h) - 1, f.z1(), "blackstone");
            for (int z = f.z0(); z <= f.z1(); z++) p.set(x, top(h), z, "polished_blackstone_bricks");
        }
    }

    private void details(Painter p) {
        // purple glowing windows on the street faces of the north / south rows (zib_1421: purple frames)
        for (int[] w : new int[][]{{6, 8}, {12, 16}, {19, 21}}) {
            for (int x = w[0]; x <= w[1]; x++) for (int y = Y0 + 2; y <= Y0 + 3; y++) {
                p.set(x, y, 3, "purple_stained_glass");
                p.set(x, y, D - 4, "magenta_stained_glass");
            }
        }
        // dormers: black slate caps with an arch, on the roof rows (1-block steps, not obstacles)
        for (int[] d : new int[][]{{6, 8}, {19, 21}}) {
            for (int z : new int[]{0, D - 3}) {
                p.fill(d[0], top(ROW) + 1, z, d[1], top(ROW) + 1, z + 2, "polished_blackstone_bricks");
                p.fill(d[0] + 1, top(ROW) + 2, z + 1, d[1] - 1, top(ROW) + 2, z + 1, "polished_blackstone_bricks");
                p.set(d[0] + 1, top(ROW) + 1, z == 0 ? 2 : z, "black_concrete");
            }
        }
        // balcony rails over the windows
        for (int x = 5; x <= 22; x++) {
            p.set(x, top(ROW) + 1, 3, "iron_bars");
            p.set(x, top(ROW) + 1, D - 4, "iron_bars");
        }
        // brown cage landing in the south (pink awning) and red slab edge lines already come from the row roofs
        for (int x = 10; x <= 17; x++) for (int z = D - 4; z <= D - 1; z++) if (z >= D - 3) p.set(x, top(ROW), z, "brown_terracotta");
        for (int x = 10; x <= 17; x++) for (int z = 0; z <= 2; z++) p.set(x, top(ROW), z, "brown_terracotta");
        // oak crates and planters at the foot of the houses (1 block: step up from the street)
        int[][] crates = {{9, 6}, {6, 10}, {11, 13}, {16, 9}, {20, 14}, {10, 14}, {15, 24}, {22, 20}, {7, 25}, {10, 24},
                {19, 24}, {16, 28}, {21, 9}, {13, 25}};
        for (int[] c : crates) p.set(c[0], Y0 + 1, c[1], "spruce_planks");
        int[][] plants = {{15, 5}, {16, 5}, {5, 24}, {2, 23}, {20, 10}, {4, 28}};
        for (int[] c : plants) { p.set(c[0], Y0 + 1, c[1], "moss_block"); p.set(c[0], Y0 + 2, c[1], "oak_leaves[persistent=true]"); }
    }

    /** About ten brick chimneys with grey caps at roof corners, each with a 1-block step beside it (the jump pivots). */
    private void chimneys(Painter p) {
        String[][] cs = {{"kioskA", "nw"}, {"midHouse", "se"}, {"redSmallA", "ne"}, {"eastHouse", "nw"}, {"eastHouse", "se"},
                {"redLong", "se"}, {"swGrey", "ne"}, {"swHouse", "sw"}, {"southRow", "se"}, {"northRow", "sw"}, {"bigSE", "se"},
                {"awningHouse", "sw"}};
        for (String[] c : cs) {
            Feature f = null;
            for (Feature x : FEATURES) if (x.name().equals(c[0])) f = x;
            if (f == null) continue;
            boolean east = c[1].endsWith("e"), south = c[1].startsWith("s");
            int x = east ? f.x1() : f.x0(), z = south ? f.z1() : f.z0();
            int sx = east ? x - 1 : x + 1;
            p.set(x, top(f.h()) + 1, z, "bricks");
            p.set(x, top(f.h()) + 2, z, "gray_concrete");
            p.set(sx, top(f.h()) + 1, z, "bricks");
        }
    }

    /** The single dark landmark: a tall grey column with a red tip on the brick base in the centre. */
    private void spire(Painter p) {
        p.fill(13, top(3) + 1, 17, 14, top(15), 18, "gray_concrete");
        p.fill(13, top(16), 17, 14, top(16), 18, "red_concrete");
        p.set(13, top(17), 17, "red_concrete");
    }

    /** Barrier ring on the wall top and a barrier roof over the pit (players cannot leave; spectators fly through). */
    private void containment(Painter p) {
        int roof = Y0 + 28;
        for (int x = -1; x <= W; x++)
            for (int z = -1; z <= D; z++) {
                boolean ring = x == -1 || x == W || z == -1 || z == D;
                if (ring) p.fill(x, Y0 + 18, z, x, roof, z, "barrier");
                p.set(x, roof, z, "barrier");
            }
    }
}
