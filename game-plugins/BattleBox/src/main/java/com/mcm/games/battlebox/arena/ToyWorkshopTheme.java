package com.mcm.games.battlebox.arena;

/**
 * Toy Workshop: a timber-beamed workshop with a painted-sky nursery mural above dark oak panelling, parquet
 * floor and foam play mats.
 *
 * Archetype: <b>bridge level over an open middle</b>. A toy-train viaduct (deck at y 7) crosses the whole room
 * from side wall to side wall straight over the centre, which sits underneath it in an open floor; the deck is
 * glass over the dais. Left and right flanks are deliberately different. Layout per half (side A shown, side B is
 * the 180-degree copy):
 * <ul>
 *   <li><b>Packing Room</b> (spawn building, x -7..7, z -17..-12): no front door. Its west exit is a long, low play
 *       tunnel that comes out at the top of the shelf corridor; its east side is a wide open porch onto the
 *       play-mat.</li>
 *   <li><b>Left flank: shelf corridor</b> (x -17..-14): a tight, roofed corridor between the side wall and a
 *       4-high toy-shelf wall with one doorway and two windows into the middle.</li>
 *   <li><b>Right flank: open play-mat</b> with low toy blocks only, and the stair up onto the viaduct.</li>
 *   <li><b>Viaduct:</b> a train carriage parked on the deck on each side of the glass section is the only cover
 *       up there; a ladder on the left pier is the second way up. Arched piers at x +-8 and +-15.</li>
 * </ul>
 */
public final class ToyWorkshopTheme extends ArenaTheme {
    @Override public String id() { return "toyworkshop"; }
    @Override public String displayName() { return "Toy Workshop"; }
    @Override public String tagline() { return "Mind the paint and the glue"; }

    @Override
    public String layoutNotes() {
        return "Bridge level: a toy-train viaduct (y 7) crosses the room over the open centre, glass deck over the dais | "
                + "Packing Room spawn: play tunnel exit (left) + wide porch (right) | left flank: tight roofed shelf corridor | "
                + "right flank: open play-mat with low blocks and the viaduct stair | ladder on the left pier";
    }

    private static final String[] TOY = {"red_concrete", "yellow_concrete", "blue_concrete", "lime_concrete", "orange_concrete"};

    @Override
    protected String floorAt(int x, int z) {
        int ax = z <= 0 ? x : -x, az = z <= 0 ? z : -z;
        // foam play-mat on the right flank
        if (ax >= 8 && az >= -11 && az <= -3)
            return (((x + 64) >> 1) + ((z + 64) >> 1) & 1) == 0 ? (z <= 0 ? "red_concrete" : "blue_concrete") : "yellow_concrete";
        // shelf corridor: dark boards
        if (ax <= -14 && az >= -12) return "dark_oak_planks";
        int m = Math.max(Math.abs(x), Math.abs(z));
        if (m == R) return "dark_oak_planks";
        int px = Math.floorMod(x, 4), pz = Math.floorMod(z, 4);
        boolean a = (px < 2) == (pz < 2);
        return a ? "oak_planks" : "birch_planks";
    }

    @Override
    protected String exteriorAt(Face f, int u, int y) {
        if (Math.abs(u) % 5 == 0) return "stripped_dark_oak_log[axis=y]";
        if (y == 4 || y == H) return "dark_oak_planks";
        return y < 4 ? "spruce_planks" : (y % 2 == 0 ? "oak_planks" : "birch_planks");
    }

    @Override protected String subFloorAt(int x, int z) { return "spruce_planks"; }

    @Override
    protected String wallAt(Face f, int u, int y) {
        if (y <= 0) return "dark_oak_planks";
        if (y <= 3) {
            if (Math.abs(u) % 5 == 0) return "stripped_dark_oak_log[axis=y]";
            return y == 3 ? "spruce_planks" : "dark_oak_planks";
        }
        if (y == 4) return "stripped_spruce_log[axis=" + (f == Face.NORTH || f == Face.SOUTH ? "x" : "z") + "]";
        if (y == H - 1) return checker(u, 0, "red_concrete", "yellow_concrete");
        if (y == H) return "dark_oak_planks";
        if (y == H - 2) return "white_concrete";
        return sky(f, u, y, 4);
    }

    @Override
    protected String ceilingAt(int x, int z) {
        if (Math.abs(x) % 5 == 0) return "dark_oak_planks";
        if (Math.abs(z) % 4 == 2 && Math.abs(x) % 5 == 2) return "ochre_froglight[axis=y]";
        if (Math.abs(z) % 4 == 0 && Math.abs(x) % 5 == 3) return "glowstone";
        return "spruce_planks";
    }



    @Override
    protected String[] roomPalette() {
        return new String[]{"oak_planks", "spruce_planks", "dark_oak_planks", "oak", "ochre_froglight"};
    }

    @Override
    protected void roof() {
        // exposed roof trusses below the ceiling, running along x every 5 blocks
        for (int z = -R; z <= R; z += 4) {
            for (int x = -R; x <= R; x++) set(x, CEIL - 1, z, "stripped_dark_oak_log[axis=x]");
        }
        for (int x = -R; x <= R; x += 5) {
            for (int z = -R; z <= R; z++) if (Math.floorMod(z, 4) != 0) set(x, CEIL - 1, z, "dark_oak_slab[type=top]");
        }
        // pitched outer roof (for the exterior look)
        for (int x = -W; x <= W; x++)
            for (int z = -W; z <= W; z++) {
                int rise = (W - Math.abs(x)) / 3;
                for (int y = CEIL + 1; y <= CEIL + rise; y++) set(x, y, z, y == CEIL + rise ? "red_nether_bricks" : "dark_oak_planks");
            }
    }



    // ------------------------------------------------------------------ layout

    private static final Pal PLANK = Pal.of("spruce_planks", 6, "oak_planks", 2, "stripped_spruce_wood[axis=y]", 1);
    private final Mat wallT = (x, y, z) -> y == 1 ? "dark_oak_planks" : y == 3 ? "stripped_dark_oak_wood[axis=y]" : PLANK.at(x, y, z, seed);
    private final Mat postT = m("stripped_dark_oak_log[axis=y]");
    private static final int DECK = 7;

    @Override
    protected void layout() {
        packingRoom();
        playTunnel();
        shelfCorridor();
        viaduct();
        playMat();
        workbenchCorner();
        for (int[] h : new int[][]{{-16, -15}, {13, -15}, {-3, -9}, {4, -6}}) symHangingLantern(h[0], CEIL - 2, h[1], 2, "lantern");
        int[][] mobiles = {{-4, -6}, {11, -12}, {-10, -14}};
        for (int i = 0; i < mobiles.length; i++) {
            int[] h = mobiles[i];
            sym(h[0], CEIL - 2, h[1], "chain[axis=y]");
            sym(h[0], CEIL - 3, h[1], TOY[i % TOY.length].replace("concrete", "wool"));
            sym(h[0] + 1, CEIL - 3, h[1], "end_rod[facing=east]");
            sym(h[0] - 1, CEIL - 3, h[1], "end_rod[facing=west]");
        }

        waypoint("tunnel", -10, 1, -14);
        waypoint("shelf corridor (top)", -15, 1, -12);
        waypoint("shelf corridor (bottom)", -15, 1, -3);
        waypoint("shelf doorway", -12, 1, -7);
        waypoint("porch", 9, 1, -14);
        waypoint("play-mat", 15, 1, -5);
        waypoint("viaduct deck", 12, DECK + 1, -1);
        waypoint("viaduct glass over the centre", 0, DECK + 1, -2);
        waypoint("viaduct far end", -16, DECK + 1, 0);
        waypoint("under the viaduct", -12, 1, 0);
        waypoint("workbench corner", -9, 1, -11);
    }

    /** Spawn building: x -7..7, z -17..-12, roof y 4; west door into the tunnel, east side an open porch. */
    private void packingRoom() {
        for (int z = -17; z <= -12; z++)
            for (int y = 1; y <= 3; y++) { symM(-7, y, z, wallT); symM(7, y, z, wallT); }
        for (int x = -7; x <= 7; x++)
            for (int y = 1; y <= 3; y++) symM(x, y, -12, wallT);
        for (int y = 1; y <= 3; y++)
            for (int[] p : new int[][]{{-7, -12}, {7, -12}, {-7, -17}, {7, -17}, {-3, -12}, {3, -12}}) symM(p[0], y, p[1], postT);
        for (int x = -7; x <= 9; x++)
            for (int z = -17; z <= -12; z++) {
                boolean edge = Math.abs(x) == 7 || x == 9 || z == -12;
                sym(x, 4, z, edge ? "dark_oak_planks" : ((x + z) & 1) == 0 ? "yellow_concrete" : "white_concrete");
            }
        for (int x = -7; x <= 9; x += 2) sym(x, 5, -12, TOY[Math.floorMod(x, TOY.length)]);
        for (int z = -16; z <= -12; z += 2) { sym(-7, 5, z, TOY[Math.floorMod(z, TOY.length)]); sym(9, 5, z, TOY[Math.floorMod(z + 2, TOY.length)]); }
        // west door to the tunnel; the east wall is opened into a 4-wide porch under an awning to x 9
        symClear(-7, 1, -14, -7, 2, -13);
        symClear(7, 1, -16, 7, 3, -13);
        for (int z = -17; z <= -12; z++) if (z == -17 || z == -12) for (int y = 1; y <= 3; y++) { sym(8, y, z, wallT.at(8, y, z)); sym(9, y, z, y == 3 ? "stripped_dark_oak_wood[axis=y]" : "stripped_dark_oak_log[axis=y]"); }
        sym(9, 3, -14, "lantern[hanging=true]");
        for (int x = -1; x <= 1; x++)
            for (int z = -17; z <= -13; z++) { set(x, 0, z, teamA + "_concrete"); set(-x, 0, -z, teamB + "_concrete"); }
        set(0, 3, -13, teamA + "_wall_banner[facing=south]");
        set(0, 3, 13, teamB + "_wall_banner[facing=north]");
        // gift boxes and the wrapping bench (corners only)
        giftBox(-6, -17, "red", "yellow"); giftBox(5, -17, "blue", "white");
        for (int x = 4; x <= 6; x++) sym(x, 1, -13, "spruce_slab[type=top]");
        sym(4, 2, -13, "red_wool"); sym(6, 2, -13, "loom[facing=north]");
        sym(-6, 1, -16, "barrel[facing=up]"); sym(-5, 1, -13, "crafting_table");
        sym(-4, 3, -15, "lantern[hanging=true]"); sym(4, 3, -15, "lantern[hanging=true]");
        // a long parcel-chute screen along the front wall inside (decor)
        for (int x = -2; x <= 2; x++) sym(x, 3, -13, "spruce_trapdoor[facing=north,half=top,open=false]");

        exitDoor("play tunnel", -7, 1, -14, -7, 3, -13);
        exitDoor("east porch", 7, 1, -16, 7, 4, -13);
    }

    private void giftBox(int x0, int z0, String col, String ribbon) {
        for (int x = x0; x <= x0 + 1; x++) {
            sym(x, 1, z0, col + "_concrete");
            sym(x, 2, z0, x == x0 ? ribbon + "_wool" : col + "_concrete");
        }
        sym(x0, 3, z0, ribbon + "_carpet");
    }

    /** Play tunnel: 2 wide, 2 high, from the Packing Room's west door (x -7) to the shelf corridor (x -14). */
    private void playTunnel() {
        String[] ring = {"red_concrete", "yellow_concrete", "blue_concrete", "lime_concrete"};
        for (int x = -13; x <= -8; x++) {
            String c = ring[Math.floorMod(x, ring.length)];
            for (int y = 1; y <= 2; y++) { sym(x, y, -15, c); sym(x, y, -12, c); }
            for (int z = -15; z <= -12; z++) sym(x, 3, z, c);
            sym(x, 1, -14, null); sym(x, 2, -14, null); sym(x, 1, -13, null); sym(x, 2, -13, null);
            sym(x, 0, -14, "white_concrete"); sym(x, 0, -13, "white_concrete");
        }
        sym(-10, 4, -14, "end_rod[facing=up]");
        for (int x = -13; x <= -8; x += 2) sym(x, 4, -15, "white_carpet");
    }

    /** Shelf corridor on the left flank: x -17..-14 between the side wall and a toy-shelf wall at x -13. */
    private void shelfCorridor() {
        for (int z = -11; z <= -2; z++)
            for (int y = 1; y <= 4; y++) {
                String b = y == 4 ? "dark_oak_planks" : ((z + y) % 3 == 0 ? "bookshelf" : TOY[Math.floorMod(z * 3 + y, TOY.length)]);
                sym(-13, y, z, b);
            }
        // doorway into the middle and two windows (glass you can see but not walk through)
        symClear(-13, 1, -7, -13, 2, -6);
        sym(-13, 2, -10, "glass_pane"); sym(-13, 2, -3, "glass_pane");
        // low shelves along the side wall (x -17) with toys on them - nooks, not a second lane
        for (int z = -11; z <= -3; z++) {
            if (z == -7) continue;
            sym(-17, 1, z, (z & 1) == 0 ? "barrel[facing=east]" : "dark_oak_slab[type=top]");
            double r = rnd(-17, 2, z, 33);
            sym(-17, 2, z, r < 0.3 ? "potted_cactus" : r < 0.6 ? TOY[(int) (r * 100) % TOY.length].replace("concrete", "candle") + "[candles=3,lit=true]" : "decorated_pot");
        }
        // roof over the corridor at y 5 (one block above the shelf wall top)
        for (int x = -17; x <= -13; x++)
            for (int z = -11; z <= -3; z++) sym(x, 5, z, x == -13 ? "stripped_dark_oak_wood[axis=z]" : (z & 1) == 0 ? "spruce_planks" : "dark_oak_slab[type=top]");
        for (int z = -10; z <= -4; z += 3) sym(-15, 4, z, "lantern[hanging=true]");
        // a stack of toy crates at the corridor head
        toyCube(-17, 1, -15, 2, 1);
    }

    /** The toy-train viaduct across the middle: deck y 7 (stand on 8), z -2..2, glass over the dais. */
    private void viaduct() {
        for (int x = -R; x <= R; x++)
            for (int z = -2; z <= 0; z++) {
                boolean edge = Math.abs(z) == 2;
                String b = Math.abs(x) <= 4 && !edge ? "glass" : edge ? "stripped_spruce_wood[axis=x]" : "spruce_planks";
                if (Math.abs(x) <= 4 && edge) b = "white_concrete";
                sym(x, DECK, z, b);
                if (z == 0 && Math.abs(x) > 4) sym(x, DECK + 1, 0, "rail[shape=east_west]");
            }
        // arched piers at x +-8 and +-15: columns at z +-2, beam across at y 5..6
        for (int px : new int[]{8, 15})
            for (int s : new int[]{-1, 1})
                for (int y = 1; y <= DECK - 1; y++) {
                    sym(px, y, 2 * s, y >= 5 ? "red_concrete" : "white_concrete");
                    if (y >= 5) for (int z = -1; z <= 1; z++) sym(px, y, z, y == 6 ? "red_concrete" : "yellow_concrete");
                }
        // parked carriages on the deck (x 6..10 and the mirrored one), walkways stay free along z = +-2
        for (int x = 6; x <= 10; x++)
            for (int z = -1; z <= 1; z++) {
                boolean end = x == 6 || x == 10;
                sym(x, DECK + 1, z, Math.abs(z) == 1 && !end && (x & 1) == 0 ? "glass_pane" : end ? "yellow_concrete" : "lime_concrete");
                sym(x, DECK + 2, z, Math.abs(z) == 1 && !end ? "lime_concrete" : "yellow_concrete");
                sym(x, DECK + 3, z, Math.abs(z) == 1 ? "red_concrete" : "red_wool");
            }
        for (int x = 7; x <= 9; x++) sym(x, DECK + 1, 0, null);
        sym(8, DECK + 4, 0, "lantern");
        // buffers at the deck ends
        sym(R, DECK + 1, -1, "red_concrete"); sym(R, DECK + 1, 1, "red_concrete");
        // main stair up from the play-mat (right flank), climbing toward the viaduct
        staircase("Viaduct stairs", 13, -9, 0, S, DECK, 2, "oak", "stripped_oak_wood[axis=y]");
        for (int z = -9; z <= -3; z++) if (States.isAir(get(14, 1, z))) for (int y = 1; y <= z + 10 && y <= DECK - 1; y++) sym(14, y, z, (y & 1) == 0 ? "yellow_concrete" : "blue_concrete");
        // ladder up the left pier (x -8, climbing on its south face)
        symLadder(-8, -3, 1, DECK, N);
        sym(-8, DECK + 1, -2, "air");
        for (int x : new int[]{-16, -11, 11}) { sym(x, DECK + 1, -2, "lantern"); }
    }

    /** Right flank: open foam play-mat with only low toy blocks. */
    private void playMat() {
        toyCube(9, 1, -7, 0, 0);
        toyCube(16, 1, -11, 3, 1);
        // stacking rings (1 high) and a ball
        for (int[] p : new int[][]{{10, -4}, {16, -6}}) { sym(p[0], 1, p[1], "orange_concrete"); }
        sym(15, 1, -4, "lime_wool");
        spinningTop(9, -11);
    }

    /** Left-middle: workbench, paint pots and a toy-block tower (cover between the corridor and the centre lane). */
    private void workbenchCorner() {
        for (int x = -11; x <= -8; x++) {
            sym(x, 1, -9, x == -11 || x == -8 ? "stripped_oak_log[axis=y]" : "oak_planks");
            sym(x, 2, -9, "oak_slab[type=bottom]");
        }
        sym(-10, 2, -9, "crafting_table"); sym(-9, 2, -9, "smithing_table");
        paintPot(-10, -4, "blue");
        for (int y = 1; y <= 4; y++) { sym(-6, y, -6, TOY[y % TOY.length]); }
        sym(-6, 5, -6, "lightning_rod[facing=up]");
        sym(-3, 1, -6, "red_concrete"); sym(-3, 2, -6, "white_concrete");
    }

    private void toyCube(int x0, int y0, int z0, int c, int letter) {
        String body = TOY[c % TOY.length];
        for (int x = x0; x <= x0 + 1; x++)
            for (int y = y0; y <= y0 + 1; y++)
                for (int z = z0; z <= z0 + 1; z++) {
                    boolean face = ((x + y + z + letter) & 1) == 0;
                    sym(x, y, z, face ? "white_concrete" : body);
                }
    }

    private void paintPot(int cx, int cz, String colour) {
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++) {
                boolean corner = Math.abs(dx) == 1 && Math.abs(dz) == 1;
                int x = cx + dx, z = cz + dz;
                sym(x, 1, z, corner ? "iron_block" : "white_concrete");
                sym(x, 2, z, corner ? "iron_block" : (dx == 0 && dz == 0 ? colour + "_concrete" : "white_concrete"));
                if (!corner) sym(x, 3, z, colour + "_concrete_powder");
            }
        sym(cx + 1, 4, cz, "lightning_rod[facing=up]");
    }

    private void spinningTop(int x0, int z0) {
        sym(x0, 1, z0, "red_concrete");
        sym(x0 + 1, 1, z0 + 1, "white_concrete");
        for (int x = x0; x <= x0 + 1; x++)
            for (int z = z0; z <= z0 + 1; z++) {
                sym(x, 2, z, ((x + z) & 1) == 0 ? "red_concrete" : "white_concrete");
                sym(x, 3, z, ((x + z) & 1) == 0 ? "yellow_concrete" : "blue_concrete");
            }
        sym(x0, 4, z0, "lightning_rod[facing=up]");
    }
}
