package com.mcm.games.battlebox.arena;

/**
 * Greenhouse Conservatory: a white iron-and-glass hall on a red-brick knee wall with a glass roof and a raised
 * lantern bay over the centre.
 *
 * Archetype (round 3): <b>terraced garden</b> - no building around the centre. The dais sits in an open court;
 * each half rises from it in a planting terrace, and the two flanks go in opposite directions: one up into a
 * pergola and gazebo, the other down into a sunken fern walk. Layout per half (side A, which looks toward +z, so
 * +x is on its LEFT; side B is the 180-degree copy):
 * <ul>
 *   <li><b>Court</b> (floor): the dais, a fountain, low hedges and an orange tree - things you fight around.</li>
 *   <li><b>Planting terrace</b> (top y 2, stand 3, x -9..8, z -11..-7): flower beds, an orange tree and an urn as
 *       cover; reached by a 3-wide back stair from the front yard, a side stair from the rim path and the potting
 *       bridge from the shed roof; left by the front stair or by dropping through any gap in the planter boxes.</li>
 *   <li><b>Left flank (+x): pergola walk + gazebo</b> (deck y 5, stand 6): back stair along the side wall, a short
 *       stair from the terrace, or a ladder from the court; the gazebo overlooks the dais under a leaf canopy, the
 *       ground under it is a shaded lane.</li>
 *   <li><b>Right flank (-x): sunken fern walk</b> (2 deep, stand -1, x -16..-13) with a rill: hidden from the
 *       floor; stairs out at its back, its middle (rim path, terrace side stair) and into the court.</li>
 *   <li><b>Potting Shed</b> (spawn building): right door, left door and an indoor stair to the roof, from which
 *       the potting bridge leads onto the terrace.</li>
 * </ul>
 */
public final class GreenhouseTheme extends ArenaTheme {
    @Override public String id() { return "greenhouse"; }
    @Override public String displayName() { return "Greenhouse Conservatory"; }
    @Override public String tagline() { return "Glass, iron and a lot of flower pots"; }

    @Override
    public String layoutNotes() {
        return "Terraced garden: open court round the centre (fountain, hedges, orange tree) | planting terrace across each half "
                + "(stand 3, 3 stairs + drop gaps) | left flank: pergola walk + gazebo over the court (stand 6; back stair, "
                + "terrace stair, court ladder) | right flank: sunken fern walk 2 deep (3 stair exits) | Potting Shed spawn: "
                + "2 side doors + roof stair onto the potting bridge";
    }

    private static final Pal BRICK = Pal.of("bricks", 8, "granite", 1, "mud_bricks", 1);
    private static final Pal STONEB = Pal.of("stone_bricks", 5, "mossy_stone_bricks", 3, "cracked_stone_bricks", 1);
    private static final Pal LEAVES = Pal.of("azalea_leaves[persistent=true]", 5, "flowering_azalea_leaves[persistent=true]", 3,
            "oak_leaves[persistent=true]", 2);
    private static final Pal FLOWERS = Pal.of("pink_tulip", 2, "red_tulip", 2, "allium", 2, "oxeye_daisy", 2, "cornflower", 2,
            "short_grass", 3, "fern", 2, "azure_bluet", 1, "lily_of_the_valley", 1, "dandelion", 1);
    private static final Pal SOIL = Pal.of("grass_block", 5, "moss_block", 3, "rooted_dirt", 1);

    @Override
    protected String floorAt(int x, int z) {
        // side A coordinates (side B is the 180-degree copy)
        int ax = z <= 0 ? x : -x, az = z <= 0 ? z : -z;
        int m = Math.max(Math.abs(x), Math.abs(z));
        if (m == R) return SOIL.at(x, 0, z, seed);
        if (az <= -12 && Math.abs(ax) >= 6) return rnd(x, 0, z, 71) < 0.25 ? "moss_block" : rnd(x, 0, z, 72) < 0.3 ? "rooted_dirt" : "grass_block";
        if (ax >= 9 && ax <= 16 && az >= -11 && az <= -5) return ((x + z) & 1) == 0 ? "polished_andesite" : "smooth_stone";   // shaded lane
        if (m == 5) return "smooth_quartz";
        if (m == 6 || m == 7) return m == 6 ? "polished_diorite" : (((x + z) & 1) == 0 ? "calcite" : "polished_diorite");   // court border
        return (((x + 64) >> 1) + ((z + 64) >> 1) & 1) == 0 ? "birch_planks" : "calcite";
    }

    @Override protected String subFloorAt(int x, int z) { return "dirt"; }

    @Override
    protected String wallAt(Face f, int u, int y) {
        if (y <= 2) return y == 2 && Math.abs(u) % 4 == 0 ? "polished_granite" : BRICK.at(u, y, f.ordinal(), seed);
        if (y == 3) return "white_concrete";
        if (y == H) return (Math.abs(u) & 1) == 0 ? "white_concrete" : "lime_concrete";
        if (y == H - 1) return "white_concrete";
        if (Math.abs(u) % 6 == 0 || y == 8) return "white_concrete";
        return sky(f, u, y, 3);
    }

    @Override
    protected String exteriorAt(Face f, int u, int y) {
        if (y <= 3) return BRICK.at(u, y, f.ordinal() + 7, seed);
        if (Math.abs(u) % 6 == 0 || y == 8 || y == H) return "white_concrete";
        return "light_blue_stained_glass";
    }

    @Override
    protected String ceilingAt(int x, int z) {
        if (Math.abs(x) < 6 && Math.abs(z) < 6) return null;   // open to the lantern bay
        if (Math.abs(x) == W || Math.abs(z) == W) return "white_concrete";
        return (Math.abs(x) % 4 == 0 || Math.abs(z) % 4 == 0) ? "white_concrete" : "glass";
    }

    @Override
    protected String[] roomPalette() {
        return new String[]{"spruce_planks", "bricks", "white_concrete", "spruce", "glowstone"};
    }

    @Override protected String daisStairs() { return "smooth_quartz"; }

    @Override
    protected void roof() {
        int top = CEIL + 3;
        for (int x = -6; x <= 6; x++)
            for (int z = -6; z <= 6; z++) {
                boolean ring = Math.abs(x) == 6 || Math.abs(z) == 6;
                if (ring) {
                    for (int y = CEIL; y < top; y++) {
                        boolean post = (Math.abs(x) % 3 == 0 && Math.abs(z) % 3 == 0) || y == CEIL;
                        set(x, y, z, post ? "white_concrete" : "glass_pane");
                    }
                }
                set(x, top, z, ring || x % 3 == 0 || z % 3 == 0 ? "white_concrete" : "glass");
            }
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) set(sx * 6, top + 1, sz * 6, "end_rod[facing=up]");
        set(0, top + 1, 0, "lightning_rod[facing=up]");
        for (int y = top - 1; y >= top - 5; y--) set(0, y, 0, "chain[axis=y]");
        set(0, top - 6, 0, "sea_lantern");
        for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) set(d[0], top - 6, d[1], "lantern[hanging=true]");
    }

    // ------------------------------------------------------------------ layout

    private final Mat wallM = (x, y, z) -> y <= 2 ? BRICK.at(x, y, z, seed) : "white_concrete";
    private final Mat post = m("quartz_pillar[axis=y]");
    private static final Pal BED = Pal.of("moss_block", 4, "grass_block", 4, "podzol", 1, "rooted_dirt", 1);
    private static final Pal KERB = Pal.of("mossy_stone_bricks", 4, "stone_bricks", 3, "cracked_stone_bricks", 1);

    /** Terrace footprint (side A) and its top (blocks at y 2, players stand at 3). */
    private static final int TX1 = -9, TX2 = 8, TZ1 = -11, TZ2 = -7, TTOP = 2;
    /** Pergola / gazebo deck height (players stand at 6). */
    private static final int DECK = 5;

    @Override
    protected void layout() {
        terrace();
        fernWalk();
        pottingShed();
        pergola();
        court();
        yards();
        for (int[] h : new int[][]{{-15, -16}, {-3, -9}, {12, -14}, {-11, -6}})
            symHangingLantern(h[0], CEIL - 1, h[1], 3, "lantern");

        waypoint("shed roof", -4, 5, -16);
        waypoint("potting bridge", 0, 5, -12);
        waypoint("terrace", -5, 3, -9);
        waypoint("left back yard", 10, 1, -15);
        waypoint("pergola walk", 15, 6, -10);
        waypoint("gazebo", 11, 6, -7);
        waypoint("under the gazebo", 11, 1, -7);
        waypoint("court left lane", 7, 1, -4);
        waypoint("right back yard", -9, 1, -15);
        waypoint("rim path", -11, 1, -12);
        waypoint("fern walk", -14, -1, -9);
        waypoint("fountain", -11, 1, -3);
        waypoint("enemy terrace via the court", 0, 3, 9);
    }

    // ---- planting terrace -------------------------------------------------------------------------------------

    private void terrace() {
        for (int x = TX1; x <= TX2; x++)
            for (int z = TZ1; z <= TZ2; z++) {
                boolean edge = x == TX1 || x == TX2 || z == TZ1 || z == TZ2;
                symM(x, 1, z, edge ? m(BRICK, 31) : m("dirt"));
                String top;
                if (edge) top = KERB.at(x, TTOP, z, seed + 3);
                else if (z == -9 || z == -8) top = ((x & 1) == 0) ? "birch_planks" : "calcite";   // the terrace path
                else top = BED.at(x, TTOP, z, seed + 4);
                sym(x, TTOP, z, top);
                if (!edge && z == -10 && Math.abs(x) % 3 != 0 && x != 2 && x != 3 && x != 4)
                    sym(x, TTOP + 1, z, FLOWERS.at(x, 3, z, seed + 6));
            }
        // ivy on the front (court) face, brick buttresses every 4
        for (int x = TX1; x <= TX2; x++) {
            if (Math.floorMod(x, 4) == 1) for (int y = 1; y <= 2; y++) sym(x, y, TZ2, "polished_granite");
            else if (rnd(x, 1, TZ2, 63) < 0.55 && (x < -3 || x > -2)) sym(x, 2, TZ2 + 1, "vine[north=true]");
        }
        // front planter boxes (1-high cover at terrace level) with gaps to drop into the court
        for (int x : new int[]{-9, -8, -7, -1, 0, 6, 7, 8}) {
            sym(x, TTOP + 1, TZ2, x == -9 || x == 8 ? "spruce_planks" : "moss_block");
            if (x != -9 && x != 8) sym(x, TTOP + 2, TZ2, FLOWERS.at(x, 4, TZ2, seed + 8));
        }
        // stairs: 3-wide back stair from the front yard, 2-wide front stair into the court, side stair from the rim
        staircase("Terrace back stair", 4, TZ1, 0, S, TTOP, 3, "birch", "bricks");
        staircase("Terrace front stair", -3, TZ2 + 1, 0, N, TTOP, 2, "birch", "bricks");
        staircase("Terrace side stair", -11, -8, 0, E, TTOP, 1, "stone_brick", "stone_bricks");
        // cover on top: an orange tree in a planter (right), a stone urn (middle), a potting bench (left corner)
        orangeTree(-6, TTOP + 1, -10, 3);
        sym(0, TTOP + 1, -10, "chiseled_stone_bricks");
        sym(-1, TTOP + 1, -10, "stone_brick_stairs[facing=east,half=bottom]");
        sym(1, TTOP + 1, -10, "stone_brick_stairs[facing=west,half=bottom]");
        sym(0, TTOP + 2, -10, "decorated_pot");
        sym(6, TTOP + 1, -11, "barrel[facing=up]");
        sym(7, TTOP + 1, -11, "spruce_slab[type=top]");
        sym(7, TTOP + 2, -11, "potted_red_tulip");
        sym(8, TTOP + 1, -11, "composter[level=6]");
        sym(-8, TTOP + 1, -11, "potted_fern");
    }

    /** Small orange tree: trunk from y0 (h blocks), round canopy with a few orange "fruit". Mirrored. */
    private void orangeTree(int x, int y0, int z, int h) {
        sym(x, y0, z, "terracotta");
        for (int y = y0 + 1; y < y0 + h; y++) sym(x, y, z, "jungle_log[axis=y]");
        int cy = y0 + h;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                for (int dy = 0; dy <= 1; dy++) {
                    if (dy == 1 && Math.abs(dx) + Math.abs(dz) == 2) continue;
                    String leaf = rnd(x + dx, cy + dy, z + dz, 91) < 0.18 ? "orange_concrete" : "azalea_leaves[persistent=true]";
                    symIfAir(x + dx, cy + dy, z + dz, leaf);
                }
        sym(x, cy - 1 + 0, z, "jungle_log[axis=y]");
    }

    // ---- sunken fern walk (west flank) ------------------------------------------------------------------------

    private void fernWalk() {
        int x1 = -16, x2 = -13, z1 = -14, z2 = -4, floor = -2;
        for (int x = x1; x <= x2; x++)
            for (int z = z1; z <= z2; z++) {
                sym(x, 0, z, (String) null);
                sym(x, -1, z, (String) null);
                sym(x, floor, z, rnd(x, floor, z, 81) < 0.3 ? "packed_mud" : rnd(x, floor, z, 82) < 0.4 ? "moss_block" : "podzol");
            }
        // rill along the wall: one block deeper, water level with the path floor
        for (int z = z1 + 1; z <= z2 - 1; z++) {
            sym(x1, floor - 1, z, "mud");
            sym(x1, floor, z, "water");
            if (rnd(x1, 0, z, 83) < 0.3) sym(x1, floor + 1, z, "lily_pad");
        }
        // kerbs: the trench walls at y -1..0 in mossy stone (the floor face would show dirt)
        for (int z = z1 - 1; z <= z2 + 1; z++) {
            for (int y = -1; y <= 0; y++) { sym(x1 - 1, y, z, KERB.at(x1 - 1, y, z, seed)); sym(x2 + 1, y, z, KERB.at(x2 + 1, y, z, seed + 1)); }
        }
        for (int x = x1; x <= x2; x++)
            for (int y = -1; y <= 0; y++) { sym(x, y, z1 - 1, KERB.at(x, y, z1 - 1, seed + 2)); sym(x, y, z2 + 1, KERB.at(x, y, z2 + 1, seed + 3)); }
        // three ways out: back (to the right back yard), middle (to the rim path) and front (into the court)
        staircase("Fern walk back steps", -14, z1, floor, N, 0, 2, "mossy_stone_brick", "mossy_stone_bricks");
        staircase("Fern walk side steps", x2, -10, floor, E, 0, 1, "mossy_stone_brick", "mossy_stone_bricks");
        staircase("Fern walk court steps", -13, z2, floor, S, 0, 2, "mossy_stone_brick", "mossy_stone_bricks");
        // ferns and a fern arch over the walk (the arch sits on the floor level above: cover for the rim, not the walk)
        for (int[] f : new int[][]{{-15, -12}, {-13, -7}, {-15, -6}})
            if (States.isAir(get(f[0], floor + 1, f[1]))) sym(f[0], floor + 1, f[1], "fern");
        for (int x = x1 - 1; x <= x2 + 1; x++) {
            symIfAir(x, 1, -7, x == x1 - 1 || x == x2 + 1 ? "mossy_stone_brick_wall" : "flowering_azalea_leaves[persistent=true]");
        }
        sym(x1 - 1, 2, -7, "lantern");
        for (int z = z1; z <= z2; z++)
            if (rnd(-12, 0, z, 84) < 0.4) symIfAir(x2, 0, z, "vine[east=true]");
    }

    // ---- spawn building ---------------------------------------------------------------------------------------

    /** Potting Shed: x -5..5, z -17..-13, roof y 4 (stand 5); right door (x -5), left door (x 5), roof stair. */
    private void pottingShed() {
        for (int z = -17; z <= -13; z++)
            for (int y = 1; y <= 3; y++) { symM(-5, y, z, wallM); symM(5, y, z, wallM); }
        for (int x = -5; x <= 5; x++)
            for (int y = 1; y <= 3; y++) symM(x, y, -13, wallM);
        for (int y = 1; y <= 3; y++)
            for (int[] p : new int[][]{{-5, -13}, {5, -13}, {-5, -17}, {5, -17}, {-2, -13}, {2, -13}}) symM(p[0], y, p[1], post);
        for (int x = -3; x <= 3; x++) if (Math.abs(x) != 2 && x != 0) sym(x, 2, -13, "lime_glazed_terracotta");   // solid tiles, not windows
        for (int x = -5; x <= 5; x++)
            for (int z = -17; z <= -13; z++) {
                boolean edge = Math.abs(x) == 5 || z == -13;
                sym(x, 4, z, edge ? "white_concrete" : ((x + z) & 1) == 0 ? "smooth_quartz" : "birch_planks");
            }
        // doors: right (west, toward the fern walk) and left (east, toward the pergola) at different depths
        symClear(-5, 1, -16, -5, 2, -15);
        symClear(5, 1, -15, 5, 2, -14);
        staircase("Shed roof stair", -3, -14, 0, E, 4, 1, "birch", "smooth_quartz");
        // roof: parapet with gaps (bridge in the middle, a drop on the left), a guard round the stair hole
        for (int z = -17; z <= -14; z++) { sym(-5, 5, z, "birch_fence"); sym(5, 5, z, "birch_fence"); }
        for (int x = -5; x <= 5; x++) if (x < -1 || x == 2 || x == 5) sym(x, 5, -13, (x & 1) == 0 ? "birch_fence" : "potted_red_tulip");
        for (int x = -3; x <= -1; x++) sym(x, 5, -15, "birch_fence");
        sym(-4, 5, -15, "birch_fence");
        sym(4, 5, -17, "composter[level=4]"); sym(3, 5, -17, "potted_azalea_bush"); sym(-2, 5, -17, "potted_fern");
        sym(-5, 6, -13, "lantern"); sym(5, 6, -17, "lantern");
        // potting bridge: from the roof over the front yard; you drop 2 onto the terrace
        for (int x = -1; x <= 1; x++) sym(x, 4, -12, x == 0 ? "birch_planks" : "stripped_birch_log[axis=z]");
        sym(-2, 4, -12, "birch_trapdoor[facing=east,half=top,open=false]"); sym(2, 4, -12, "birch_trapdoor[facing=west,half=top,open=false]");
        // team floor, banners, benches in the back corners (the doors and the stair stay free)
        for (int x = -1; x <= 1; x++)
            for (int z = -17; z <= -15; z++) { set(x, 0, z, teamA + "_concrete"); set(-x, 0, -z, teamB + "_concrete"); }
        set(3, 3, -14, teamA + "_wall_banner[facing=north]");
        set(-3, 3, 14, teamB + "_wall_banner[facing=south]");
        sym(-4, 1, -17, "barrel[facing=up]"); sym(-4, 2, -17, "potted_red_tulip");
        sym(4, 1, -17, "spruce_slab[type=top]"); sym(4, 2, -17, "potted_fern");
        sym(3, 1, -17, "composter[level=5]");
        sym(-4, 3, -16, "lantern[hanging=true]"); sym(3, 3, -15, "lantern[hanging=true]");

        exitDoor("right door", -5, 1, -16, -5, 3, -15);
        exitDoor("left door", 5, 1, -15, 5, 3, -14);
        exitDoor("roof stair", -4, 1, -14, 0, 4, -14);
    }

    // ---- pergola walk + gazebo (east flank) -------------------------------------------------------------------

    private void pergola() {
        String deck = "birch_planks", trim = "stripped_birch_wood[axis=y]";
        staircase("Pergola stair", 16, -16, 0, S, DECK, 1, "birch", "stripped_birch_wood[axis=y]");
        // walkway x 13..16, z -11..-9 and gazebo x 9..14, z -9..-5
        for (int x = 13; x <= 16; x++)
            for (int z = -11; z <= -9; z++) sym(x, DECK, z, (z == -11 || x == 16) ? trim : deck);
        for (int x = 9; x <= 14; x++)
            for (int z = -9; z <= -5; z++) {
                boolean edge = x == 9 || x == 14 || z == -9 || z == -5;
                sym(x, DECK, z, edge ? trim : ((x + z) & 1) == 0 ? deck : "birch_slab[type=double]");
            }
        // posts (quartz) under the decks
        for (int[] p : new int[][]{{9, -9}, {9, -5}, {14, -5}, {14, -9}, {13, -11}, {16, -9}})
            for (int y = 1; y < DECK; y++) sym(p[0], y, p[1], "quartz_pillar[axis=y]");
        // ladder from the court on the gazebo's front-left post
        symLadder(9, -4, 1, DECK, S);
        // the short stair from the terrace up onto the gazebo
        staircase("Gazebo stair", 6, -8, TTOP, E, DECK, 1, "birch", "bricks");
        // railings: fences, with a planter front facing the centre; gaps at the ladder, the gazebo stair and the walkway
        for (int x = 9; x <= 14; x++) {
            if (x == 9) continue;                                   // ladder gap at (9, -5)
            sym(x, DECK + 1, -5, (x & 1) == 0 ? "moss_block" : "birch_fence");
            if (x <= 12) sym(x, DECK + 1, -9, "birch_fence");
        }
        for (int z = -8; z <= -5; z++) sym(14, DECK + 1, z, "birch_fence");
        for (int z = -9; z <= -6; z++) if (z != -8) sym(9, DECK + 1, z, "birch_fence");
        for (int x = 13; x <= 15; x++) sym(x, DECK + 1, -11, "birch_fence");
        sym(13, DECK + 1, -10, "birch_fence");
        for (int x = 10; x <= 14; x += 2) if (States.id(get(x, DECK + 1, -5)).equals("moss_block")) sym(x, DECK + 2, -5, FLOWERS.at(x, 7, -5, seed + 9));
        // pergola posts and the leaf canopy (y 8): open lattice over the walkway, dome over the gazebo
        for (int[] p : new int[][]{{14, -9}, {9, -9}, {14, -5}})
            for (int y = DECK + 1; y <= DECK + 2; y++) sym(p[0], y, p[1], "birch_fence");
        for (int x = 9; x <= 14; x++)
            for (int z = -9; z <= -5; z++) {
                double r = Math.hypot(x - 11.5, z + 7);
                if (r < 3.3) sym(x, DECK + 3, z, rnd(x, 8, z, 93) < 0.3 ? "flowering_azalea_leaves[persistent=true]" : "azalea_leaves[persistent=true]");
            }
        for (int z = -11; z <= -9; z++) sym(15, DECK + 3, z, "stripped_birch_log[axis=z]");
        for (int x = 13; x <= 16; x++) if ((x & 1) == 1) sym(x, DECK + 3, -10, "flowering_azalea_leaves[persistent=true]");
        // lanterns hang IN the canopy (y 8), above head height on the deck
        sym(11, DECK + 3, -7, "lantern[hanging=true]"); sym(11, DECK + 4, -7, "chain[axis=y]");
        sym(15, DECK + 3, -10, "lantern[hanging=true]"); sym(15, DECK + 4, -10, "chain[axis=y]");
        // under the gazebo: a shaded lane with a potting table and pots (low cover)
        sym(12, 1, -8, "spruce_slab[type=top]"); sym(13, 1, -8, "spruce_slab[type=top]");
        sym(12, 2, -8, "potted_cactus"); sym(13, 2, -8, "flower_pot");
        sym(11, 1, -6, "decorated_pot");
        sym(15, 1, -6, "barrel[facing=up]"); sym(15, 2, -6, "potted_azalea_bush");
    }

    // ---- court ------------------------------------------------------------------------------------------------

    private void court() {
        // fountain beside the right lane: stone ring, water, lily pads, a lamp
        int fx = -9, fz = -3;
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    sym(fx, 0, fz, "prismarine_bricks");
                    sym(fx, 1, fz, "water");
                    sym(fx, 2, fz, "lily_pad");
                } else sym(fx + dx, 1, fz + dz, Math.abs(dx) + Math.abs(dz) == 2 ? "chiseled_stone_bricks" : "mossy_stone_bricks");
            }
        sym(fx - 1, 2, fz - 1, "potted_red_tulip"); sym(fx + 1, 2, fz + 1, "potted_azalea_bush");
        sym(fx + 1, 2, fz - 1, "lantern");
        // low hedges in the left lane and an orange tree at the court's right front corner
        for (int z = -2; z <= -1; z++) { sym(7, 1, z, "azalea_leaves[persistent=true]"); }
        sym(7, 0, -2, "moss_block"); sym(7, 0, -1, "moss_block");
        orangeTree(-7, 1, -5, 3);
        // flower pots at the foot of the terrace
        for (int x : new int[]{-8, 2, 5}) sym(x, 1, -6, x == 2 ? "potted_allium" : "potted_fern");
    }

    // ---- yards ------------------------------------------------------------------------------------------------

    private void yards() {
        // left back yard: compost bays and a wheelbarrow, path to the pergola stair kept free along z -16
        for (int x = 10; x <= 13; x++) {
            sym(x, 1, -17, x == 10 || x == 13 ? "spruce_fence" : "podzol");
            if (x == 11 || x == 12) sym(x, 2, -17, "composter[level=8]");
        }
        sym(8, 1, -13, "spruce_stairs[facing=east,half=bottom]");
        sym(9, 1, -13, "oak_trapdoor[facing=east,half=bottom,open=true]");
        sym(7, 1, -17, "barrel[facing=up]"); sym(7, 2, -17, "potted_fern");
        sym(15, 1, -13, "hay_block[axis=x]");
        // right back yard: cold frames (glass under trapdoor lids), barrels and a water butt
        for (int x = -9; x <= -7; x++) { sym(x, 1, -13, "glass"); sym(x, 2, -13, "birch_trapdoor[facing=south,half=bottom,open=false]"); }
        sym(-10, 1, -17, "barrel[facing=up]"); sym(-11, 1, -17, "water_cauldron[level=3]"); sym(-12, 1, -17, "barrel[facing=up]");
        sym(-7, 1, -17, "spruce_slab[type=top]"); sym(-7, 2, -17, "potted_cactus");
        // lamp posts
        for (int[] l : new int[][]{{-12, -13}, {8, -17}, {16, -5}}) {
            sym(l[0], 1, l[1], "birch_fence"); sym(l[0], 2, l[1], "birch_fence"); sym(l[0], 3, l[1], "lantern");
        }
    }
}
