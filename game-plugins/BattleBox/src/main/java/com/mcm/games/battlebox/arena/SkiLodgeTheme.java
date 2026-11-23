package com.mcm.games.battlebox.arena;

/**
 * Ski Lodge: an "outdoor" snowfield sealed inside painted walls (sky, block clouds, a pixel sun, snowy
 * mountain silhouettes and a fir-forest band), a red/white checker frieze and an invisible barrier roof.
 *
 * Archetype: <b>river split</b>. A frozen ravine (3 blocks deep, |z| &lt;= 7) runs wall to wall between the two
 * halves; the centre dais sits on an island in the middle of it. Layout per half (side A shown, side B is the
 * 180-degree copy):
 * <ul>
 *   <li><b>Boot Room + porch</b> (spawn building): a small cabin around the gate whose front door opens onto a wide
 *       covered porch; a log-and-chimney screen fills the middle of the porch front, so the porch is left by its
 *       two front corners. A side door leads into the alley next to the lodge.</li>
 *   <li><b>Big lodge</b> (left back corner, x -17..-8): two storeys, indoor stair, upper room with a river-side
 *       balcony that overhangs the ravine. The lodges of the two sides sit on one diagonal.</li>
 *   <li><b>Open slope</b> (right back corner): nearly empty snowfield with a ski-lift tower (ladder to an
 *       exposed platform 8 up) and the stair down into the ravine. The two slopes sit on the other diagonal.</li>
 *   <li><b>River:</b> lower ice level with water patches, ice floes and a frozen-in boat; one plank bridge per
 *       side from the bank onto the island (right of centre) and one stair per side from the ice up onto the
 *       island (left of centre).</li>
 * </ul>
 */
public final class SkiLodgeTheme extends ArenaTheme {
    @Override public String id() { return "skilodge"; }
    @Override public String displayName() { return "Ski Lodge"; }
    @Override public String tagline() { return "Fresh powder, thin ice"; }

    @Override
    public String layoutNotes() {
        return "River split: frozen ravine wall to wall, centre on a mid-river island | Boot Room + wide porch (2 porch corners + side door) | "
                + "big two-storey lodge with river balcony (one diagonal) | open slope with ski-lift tower (other diagonal) | "
                + "one bridge + one ice stair onto the island per side";
    }

    private static final Pal SNOW = Pal.of("snow_block", 7, "white_concrete_powder", 2, "calcite", 1);
    private static final Pal ICE = Pal.of("packed_ice", 6, "blue_ice", 2, "ice", 1);
    private static final Pal LOG = Pal.of("spruce_log[axis=y]", 4, "stripped_spruce_log[axis=y]", 1);
    private static final Pal FIR = Pal.of("spruce_leaves[persistent=true]", 6, "green_terracotta", 1);
    private static final Pal STONE = Pal.of("cobblestone", 4, "stone", 2, "mossy_cobblestone", 1, "andesite", 2);

    /** River half-width (ravine |z| &lt;= RIV), island half-sizes. */
    private static final int RIV = 7, ISX = 6, ISZ = 5, DEEP = -3;

    @Override
    protected String floorAt(int x, int z) {
        int ax = Math.abs(x), az = Math.abs(z);
        if (ax <= 6 && az >= 11 && az <= 13) return ((az + ax) % 3 == 0) ? "stripped_spruce_wood[axis=x]" : "spruce_planks";   // porch deck
        return SNOW.at(x, 0, z, seed);
    }

    @Override protected String subFloorAt(int x, int z) { return "packed_ice"; }

    private int mountain(Face f, int u) {
        double n = Noise.fbm2(u * 0.11 + f.ordinal() * 13, 0.5, seed + 7);
        return 3 + (int) Math.round(n * 5);
    }

    @Override
    protected String wallAt(Face f, int u, int y) {
        if (y <= 1) return y <= 0 ? "spruce_planks" : "snow_block";
        if (y == H) return "spruce_planks";
        if (y == H - 1) return checker(u, 0, "red_concrete", "white_concrete");
        int c = Math.floorMod(u + f.ordinal() * 2, 5);
        int top = 5 + Math.floorMod(u * 7 + f.ordinal(), 3) / 2;
        int half = Math.abs(c - 2);
        if (y >= 2 && y <= top && half <= (top - y + 1) / 2 + (y <= 2 ? 1 : 0)) return y == top ? "snow_block" : (((u + y) & 3) == 0 ? "green_terracotta" : "spruce_leaves[persistent=true]");
        if (y == 2) return "snow_block";
        int m = mountain(f, u);
        if (y <= m) return y == m ? "white_concrete" : y == m - 1 ? "light_gray_concrete" : (((u * 3 + y) % 4 == 0) ? "stone" : "gray_concrete");
        return sky(f, u, y, 4);
    }

    @Override
    protected String exteriorAt(Face f, int u, int y) {
        if (Math.abs(u) % 6 == 0) return "stripped_spruce_log[axis=y]";
        return y == H || y == 0 ? "spruce_planks" : "spruce_log[axis=" + (f == Face.NORTH || f == Face.SOUTH ? "x" : "z") + "]";
    }

    @Override protected String ceilingAt(int x, int z) { return "barrier"; }

    @Override
    protected String[] roomPalette() {
        return new String[]{"spruce_planks", "stripped_spruce_log[axis=y]", "spruce_planks", "spruce", "lantern"};
    }

    @Override
    protected void roomDecor(boolean b) {
        int sg = b ? 1 : -1;
        set(-4, 1, sg * (geo.ROOM_OUT - 1), "red_concrete");
        set(-4, 2, sg * (geo.ROOM_OUT - 1), "red_concrete");
        set(4, 1, sg * (geo.ROOM_OUT - 1), "blue_concrete");
        set(4, 2, sg * (geo.ROOM_OUT - 1), "blue_concrete");
    }

    @Override
    protected void roof() {
        for (int u = -W; u <= W; u++) {
            set(u, CEIL, -W, "spruce_slab[type=bottom]");
            set(u, CEIL, W, "spruce_slab[type=bottom]");
            set(-W, CEIL, u, "spruce_slab[type=bottom]");
            set(W, CEIL, u, "spruce_slab[type=bottom]");
        }
    }

    @Override protected String daisStairs() { return "spruce"; }
    @Override protected String daisBody() { return "stripped_spruce_wood[axis=y]"; }

    // ------------------------------------------------------------------ layout

    private static final Pal LOGWALL = Pal.of("spruce_planks", 5, "stripped_spruce_wood[axis=y]", 2, "spruce_wood[axis=y]", 1);
    private final Mat logW = (x, y, z) -> y == 1 ? STONE.at(x, y, z, seed) : LOGWALL.at(x, y, z, seed);
    private final Mat logPost = m("spruce_log[axis=y]");
    private final Mat gableFill = (x, y, z) -> ((y & 1) == 0) ? "spruce_planks" : "stripped_spruce_wood[axis=y]";

    @Override
    protected void layout() {
        river();
        island();
        bootRoom();
        lodge();
        liftTower();
        slope();
        // drifts along the walls of the bank (only on free snow)
        for (int a = -R; a <= R; a++)
            for (int[] p : new int[][]{{a, -R}, {-R, a}, {R, a}}) {
                if (p[1] >= -RIV - 1) continue;
                if (Math.abs(p[0]) <= 7 && p[1] == -R) continue;
                int layers = 1 + (int) (rnd(p[0], 1, p[1], 5) * 2);
                if (States.isAir(get(p[0], 1, p[1])) && States.isAir(get(-p[0], 1, -p[1])) && Shapes.fullSolid(get(p[0], 0, p[1])))
                    sym(p[0], 1, p[1], "snow[layers=" + layers + "]");
            }

        waypoint("alley by the lodge", -7, 1, -15);
        waypoint("lodge ground floor", -12, 1, -12);
        waypoint("lodge upper room", -11, 5, -12);
        waypoint("lodge balcony", -12, 5, -8);
        waypoint("open slope", 10, 1, -12);
        waypoint("lift platform", 12, 9, -15);
        waypoint("bridge", 2, 1, -7);
        waypoint("ice level (left)", -13, DEEP + 1, -4);
        waypoint("ice level (right)", 16, DEEP + 1, -3);
        waypoint("island stair top", -6, 1, -2);
        waypoint("enemy bank via ravine", -15, 1, 9);
    }

    /** The ravine: |z| &lt;= RIV dug to y DEEP, ice floor with water patches; bank faces in stone and ice. */
    private void river() {
        for (int x = -R; x <= R; x++)
            for (int z = -RIV; z <= 0; z++) {
                for (int y = DEEP + 1; y <= 0; y++) sym(x, y, z, (String) null);
                double n = Noise.fbm2(x * 0.21, z * 0.33, seed + 41);
                String fl = n > 0.62 ? "blue_ice" : ICE.at(x, DEEP, z, seed);
                sym(x, DEEP, z, fl);
                if (n < 0.3 && Math.abs(x) > ISX + 2) sym(x, DEEP + 1, z, "water");   // shallow open water (slow)
            }
        // bank faces (z = +-(RIV+1) at y DEEP+1..-1) and the walls below the painted band
        for (int x = -R; x <= R; x++)
            for (int y = DEEP + 1; y <= -1; y++) sym(x, y, -RIV - 1, y == -1 ? "snow_block" : STONE.at(x, y, -RIV - 1, seed + 2));
        for (int z = -RIV; z <= RIV; z++)
            for (int y = DEEP; y <= -1; y++) {
                set(-W, y, z, y == -1 ? "snow_block" : "packed_ice");
                set(W, y, z, y == -1 ? "snow_block" : "packed_ice");
                set(-W - 1, y, z, "spruce_planks");
                set(W + 1, y, z, "spruce_planks");
            }
        // ice floes (stepping stones, top flush with the banks' feet level - 1) and a frozen-in rowboat
        for (int[] f : new int[][]{{-14, -3}, {-12, -5}, {15, -1}, {-3, -7}})
            for (int y = DEEP + 1; y <= DEEP + 1; y++) sym(f[0], y, f[1], "packed_ice");
        sym(-12, DEEP + 2, -4, "snow[layers=2]");
        // frozen rowboat (cover on the ice, right of centre)
        int bx = 8, bz = -3;
        for (int x = bx; x <= bx + 4; x++) {
            boolean end = x == bx || x == bx + 4;
            sym(x, DEEP + 1, bz, end ? "spruce_planks" : "spruce_slab[type=bottom]");
            sym(x, DEEP + 1, bz - 1, "spruce_planks");
            sym(x, DEEP + 1, bz + 1, "spruce_planks");
            sym(x, DEEP + 2, bz - 1, end ? "spruce_planks" : "spruce_trapdoor[facing=south,half=bottom,open=true]");
            sym(x, DEEP + 2, bz + 1, end ? "spruce_planks" : "spruce_trapdoor[facing=north,half=bottom,open=true]");
        }
        sym(bx + 2, DEEP + 2, bz, "spruce_fence");
        sym(bx + 2, DEEP + 3, bz, "spruce_fence");
        sym(bx + 2, DEEP + 4, bz, "white_wool");
        // frozen waterfall against each end wall of the ravine (x 16..17, z -2..2): ice cover at the river mouths
        for (int z = -2; z <= 0; z++)
            for (int y = DEEP + 1; y <= 5; y++) {
                int depth = y <= 0 ? 2 : (y <= 3 ? 1 : 0);
                for (int d = 0; d <= depth; d++) {
                    int x = R - d;
                    if (Math.abs(z) == 2 && d > 0) continue;
                    sym(x, y, z, (y + z + d) % 3 == 0 ? "blue_ice" : "packed_ice");
                }
            }
        for (int z = -1; z <= 0; z++) sym(R - 2, DEEP + 1, z, "water");
        // plank bridge from the bank onto the island (right of centre), 2 wide between rails
        for (int x = 1; x <= 4; x++)
            for (int z = -RIV; z <= -ISZ - 1; z++) {
                sym(x, 0, z, x == 1 || x == 4 ? "stripped_spruce_wood[axis=z]" : "spruce_planks");
                if (x == 1 || x == 4) sym(x, 1, z, "spruce_fence");
            }
        for (int y = DEEP + 1; y <= -1; y++) { sym(1, y, -RIV, "spruce_log[axis=y]"); sym(4, y, -RIV, "spruce_log[axis=y]"); }
        sym(1, 2, -RIV, "lantern"); sym(4, 2, -ISZ - 1, "lantern");
        // stair from the ice up onto the bank (right flank) and up onto the island (left of centre)
        staircase("Bank steps", 14, -4, DEEP, N, 0, 2, "spruce", "packed_ice");
        staircase("Island steps", -10, -2, DEEP, E, 0, 2, "spruce", "packed_ice");
        // lanterns on posts along the bank edge
        for (int x : new int[]{-5, 6, 16}) { sym(x, 1, -RIV - 1, "spruce_fence"); sym(x, 2, -RIV - 1, "lantern"); }
    }

    /** Mid-river island: rock body up to the ground level, snow top (the dais is added later by the base class). */
    private void island() {
        for (int x = -ISX; x <= ISX; x++)
            for (int z = -ISZ; z <= 0; z++) {
                boolean edge = Math.abs(x) == ISX || z == -ISZ;
                for (int y = DEEP + 1; y <= -1; y++) sym(x, y, z, edge ? (y == -1 ? "snow_block" : STONE.at(x, y, z, seed + 9)) : "stone");
                sym(x, 0, z, SNOW.at(x, 0, z, seed + 3));
            }
        // small fir and a lamp on the island's left/right ends (low cover beside the dais)
        sym(-6, 1, -4, "spruce_leaves[persistent=true]"); sym(-6, 2, -4, "snow[layers=2]");
        sym(6, 1, -4, "spruce_fence"); sym(6, 2, -4, "spruce_fence"); sym(6, 3, -4, "lantern");
        sym(-6, 1, 0, "snow_block"); sym(-6, 2, 0, "snow[layers=3]");
    }

    /** Spawn building: Boot Room (x -5..5, z -17..-14) + porch (x -6..6, z -13..-11) with a central screen. */
    private void bootRoom() {
        // cabin walls
        for (int z = -17; z <= -14; z++)
            for (int y = 1; y <= 3; y++) { symM(-5, y, z, logW); symM(5, y, z, logW); }
        for (int x = -5; x <= 5; x++)
            for (int y = 1; y <= 3; y++) symM(x, y, -14, logW);
        for (int y = 1; y <= 3; y++)
            for (int[] p : new int[][]{{-5, -14}, {5, -14}, {-5, -17}, {5, -17}}) symM(p[0], y, p[1], logPost);
        symPlate(-5, -17, 5, -14, 4, m("stripped_spruce_wood[axis=y]"), m("spruce_planks"));
        symGable(-5, -17, 5, -14, 5, true, "spruce", gableFill, "snow_block");
        // front door (2 wide, left of the axis) onto the porch; side door into the alley by the lodge
        symClear(-1, 1, -14, 0, 2, -14);
        symClear(-5, 1, -16, -5, 2, -15);
        // inside: coat pegs and boot boxes along the back corners only (doors stay free)
        sym(-4, 1, -17, "barrel[facing=up]"); sym(4, 1, -17, "barrel[facing=up]");
        sym(4, 1, -15, "spruce_stairs[facing=west,half=bottom]");
        sym(4, 1, -16, "spruce_stairs[facing=west,half=bottom]");
        sym(-3, 3, -16, "lantern[hanging=true]"); sym(3, 3, -16, "lantern[hanging=true]");
        for (int x = -1; x <= 1; x++)
            for (int z = -17; z <= -15; z++) { set(x, 0, z, teamA + "_concrete"); set(-x, 0, -z, teamB + "_concrete"); }

        // porch: roof at y 4 on log posts, fenced ends, screen in the middle of the front
        for (int x = -6; x <= 6; x++)
            for (int z = -13; z <= -11; z++) sym(x, 4, z, z == -11 || Math.abs(x) == 6 ? "stripped_spruce_wood[axis=x]" : "spruce_planks");
        for (int x = -6; x <= 6; x++) sym(x, 5, -11, ((x & 1) == 0) ? "spruce_slab[type=bottom]" : "snow[layers=2]");
        for (int y = 1; y <= 3; y++) { sym(-6, y, -11, "spruce_log[axis=y]"); sym(6, y, -11, "spruce_log[axis=y]"); }
        for (int z = -14; z <= -12; z++) { sym(-6, 1, z, "spruce_fence"); sym(6, 1, z, "spruce_fence"); }
        // screen: stone chimney breast with a fireplace facing the porch, woodstore on its sides
        for (int x = -3; x <= 3; x++)
            for (int y = 1; y <= 3; y++) sym(x, y, -11, Math.abs(x) == 3 ? "spruce_log[axis=y]" : Math.abs(x) == 2 ? "spruce_log[axis=z]" : STONE.at(x, y, -11, seed + 4));
        sym(0, 1, -12, "campfire[lit=true,signal_fire=false]");
        sym(-1, 1, -12, "cobblestone_wall"); sym(1, 1, -12, "cobblestone_wall");
        for (int y = 4; y <= 7; y++) sym(0, y, -11, y == 7 ? "campfire[lit=true,signal_fire=false]" : "bricks");
        sym(-1, 6, -11, "snow[layers=2]");
        for (int x = -2; x <= 2; x += 4) sym(x, 4, -10, "lantern[hanging=true]");
        set(-1, 3, -15, teamA + "_wall_banner[facing=south]");
        set(1, 3, 15, teamB + "_wall_banner[facing=north]");
        // porch benches against the cabin front (not in front of the door)
        sym(-4, 1, -13, "spruce_stairs[facing=south,half=bottom]");
        sym(3, 1, -13, "barrel[facing=up]"); sym(4, 1, -13, "spruce_slab[type=top]"); sym(4, 2, -13, "lantern");

        exitDoor("porch left corner", -5, 1, -11, -4, 3, -11);
        exitDoor("porch right corner", 4, 1, -11, 5, 3, -11);
        exitDoor("side door to the lodge alley", -5, 1, -16, -5, 3, -15);
    }

    /** Big two-storey lodge in the left back corner: x -17..-8, z -17..-9. */
    private void lodge() {
        int x1 = -17, x2 = -8, z1 = -17, z2 = -9;
        symRoom(x1, z1, x2, z2, 1, 3, logW, logPost);
        symPlate(x1, z1, x2, z2, 4, m("stripped_spruce_wood[axis=y]"), m("spruce_planks"));
        symRoom(x1, z1, x2, z2, 5, 7, (x, y, z) -> LOGWALL.at(x, y, z, seed + 5), logPost);
        symGable(x1, z1, x2, z2, 8, true, "dark_oak", gableFill, "snow_block");
        for (int z = z1 - 1; z <= z2 + 1; z++) for (int x = x1; x <= x2 + 1; x++) {
            int top = -1;
            for (int y = 14; y >= 8; y--) if (!States.isAir(get(x, y, z))) { top = y; break; }
            if (top > 0 && States.isAir(get(x, top + 1, z)) && States.id(get(x, top, z)).endsWith("_stairs") && rnd(x, top, z, 7) < 0.55)
                sym(x, top + 1, z, "snow[layers=1]");
        }
        // ground doors: east (alley), river side (bank)
        symClear(x2, 1, -16, x2, 2, -15);
        symClear(-12, 1, z2, -11, 2, z2);
        for (int x : new int[]{-15, -14, -9}) sym(x, 2, z2, "glass_pane");
        for (int z = -14; z <= -12; z++) sym(x2, 2, z, "glass_pane");
        // indoor stair along the back wall, climbing west
        staircase("Lodge stairs", -10, -16, 0, Wst, 4, 1, "spruce", "spruce_planks");
        for (int x = -14; x <= -11; x++) sym(x, 5, -15, "spruce_fence");   // stairwell guard upstairs
        // ground floor: fireplace on the west wall, tables, rugs
        for (int z = -14; z <= -11; z++) for (int y = 1; y <= 3; y++) sym(-16, y, z, STONE.at(-16, y, z, seed + 6));
        sym(-16, 1, -13, "campfire[lit=true,signal_fire=false]"); sym(-16, 2, -13, "air"); sym(-16, 1, -12, "campfire[lit=true,signal_fire=false]"); sym(-16, 2, -12, "air");
        sym(-13, 1, -12, "spruce_fence"); sym(-13, 2, -12, "spruce_pressure_plate");
        sym(-14, 1, -12, "spruce_stairs[facing=east,half=bottom]");
        sym(-12, 1, -12, "air");
        sym(-10, 1, -11, "spruce_stairs[facing=north,half=bottom]");
        sym(-10, 1, -12, "spruce_fence"); sym(-10, 2, -12, "spruce_pressure_plate");
        sym(-13, 3, -13, "lantern[hanging=true]");
        sym(-10, 3, -15, "lantern[hanging=true]");
        // upper floor: river windows, balcony door, beds
        for (int x = -16; x <= -9; x++) if (x != -13 && x != -12) { sym(x, 5, z2, "glass_pane"); sym(x, 6, z2, "glass_pane"); }
        symClear(-13, 5, z2, -12, 6, z2);
        for (int z = -14; z <= -11; z++) sym(x1, 6, z, "glass_pane");
        sym(-16, 5, -12, "red_bed[facing=west,part=foot]"); sym(-17 + 1, 5, -11, "red_bed[facing=west,part=foot]");
        sym(-9, 5, -12, "barrel[facing=up]"); sym(-9, 5, -11, "spruce_stairs[facing=east,half=bottom]");
        sym(-12, 7, -12, "lantern[hanging=true]");
        sym(-14, 4, -11, "red_wool"); sym(-14, 5, -11, "red_carpet"); sym(-15, 5, -11, "white_carpet");
        // balcony over the bank and the edge of the ravine: y 4 deck, z -8..-7
        for (int x = -16; x <= -9; x++)
            for (int z = -8; z <= -7; z++) sym(x, 4, z, z == -7 || x == -16 || x == -9 ? "stripped_spruce_wood[axis=y]" : "spruce_planks");
        for (int x = -16; x <= -9; x++) sym(x, 5, -7, "spruce_fence");
        sym(-16, 5, -8, "spruce_fence"); sym(-9, 5, -8, "spruce_fence");
        for (int y = DEEP + 1; y <= 3; y++) { sym(-16, y, -7, "spruce_log[axis=y]"); sym(-9, y, -7, "spruce_log[axis=y]"); }
        sym(-16, 6, -7, "lantern"); sym(-9, 6, -7, "lantern");
        sym(-12, 3, -8, "lantern[hanging=true]");
    }

    /** Ski-lift tower on the open slope: mast + ladder to an exposed platform at y 8, bullwheel and cable above. */
    private void liftTower() {
        int cx = 13, cz = -14;
        for (int y = 1; y <= 7; y++) {
            sym(cx, y, cz - 1, y % 3 == 0 ? "stripped_spruce_log[axis=y]" : "spruce_log[axis=y]");
            for (int[] c : new int[][]{{-1, -1}, {1, -1}, {-1, 1}, {1, 1}}) sym(cx + c[0], y, cz + c[1], y == 4 ? "iron_block" : "iron_bars");
        }
        symLadder(cx, cz, 1, 8, S);
        for (int x = cx - 2; x <= cx + 2; x++)
            for (int z = cz - 2; z <= cz + 2; z++) {
                if (x == cx && z == cz) continue;
                boolean edge = Math.abs(x - cx) == 2 || Math.abs(z - cz) == 2;
                sym(x, 8, z, edge ? "stripped_spruce_wood[axis=y]" : "spruce_planks");
                if (edge && !(x == cx && z == cz + 2)) sym(x, 9, z, "spruce_fence");
            }
        sym(cx - 2, 10, cz - 2, "lantern"); sym(cx + 2, 10, cz - 2, "lantern");
        // bullwheel on the mast top (above head height) and the cable running to the side wall
        for (int x = cx - 2; x <= cx + 2; x++)
            for (int y = 11; y <= 13; y++) {
                double d = Math.hypot(x - cx, y - 12.5);
                if (d > 1.2 && d < 2.3) sym(x, y, cz - 1, "stripped_spruce_wood[axis=x]");
            }
        sym(cx, 12, cz - 1, "iron_block");
        for (int y = 9; y <= 11; y++) sym(cx, y, cz - 1, "spruce_fence");
        for (int x = cx + 3; x <= R; x++) sym(x, 13, cz - 1, "chain[axis=x]");
        for (int x = 6; x <= cx - 3; x++) sym(x, 13, cz - 1, "chain[axis=x]");
        // a lift chair hanging over the slope
        sym(8, 12, cz - 1, "chain[axis=y]");
        sym(8, 11, cz - 1, "dark_oak_slab[type=top]"); sym(8, 10, cz - 1, "dark_oak_trapdoor[facing=south,half=bottom,open=true]");
    }

    /** Open slope details (right back corner): mostly empty snow, a few firs, ski rack, sled. */
    private void slope() {
        firTree(9, -16, 5);
        firTree(16, -11, 4);
        skiRack(9, -17);
        sled(16, -15);
        snowman(9, -10);
        // a low snow drift ridge (1 high) across the slope - the only cover in the open
        for (int x = 9; x <= 12; x++) sym(x, 1, -9, x == 9 || x == 12 ? "snow[layers=4]" : "snow_block");
        for (int[] l : new int[][]{{7, -9}, {16, -9}}) lampPost(l[0], l[1]);
    }

    private void firTree(int cx, int cz, int h) {
        for (int y = 1; y <= h; y++) sym(cx, y, cz, "spruce_log[axis=y]");
        int r = h >= 6 ? 2 : 1;
        for (int y = 2; y <= h + 1; y++) {
            int tier = (h + 1 - y);
            int rr = Math.min(r + 1, (tier + 2) / 2);
            if (y == h + 1) rr = 0;
            for (int dx = -rr; dx <= rr; dx++)
                for (int dz = -rr; dz <= rr; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > rr + (y % 2 == 0 ? 0 : -1) && rr > 0) continue;
                    if (dx == 0 && dz == 0 && y <= h) continue;
                    int x = cx + dx, z = cz + dz;
                    if (Math.abs(x) > R || Math.abs(z) > R) continue;
                    if (!States.isAir(get(x, y, z))) continue;
                    sym(x, y, z, FIR.at(x, y, z, seed));
                    if (States.isAir(get(x, y + 1, z))) sym(x, y + 1, z, "snow[layers=1]");
                }
        }
        sym(cx, h + 2, cz, "snow[layers=2]");
        set(cx, h + 1, cz, "spruce_leaves[persistent=true]");
        set(-cx, h + 1, -cz, "spruce_leaves[persistent=true]");
    }

    private void skiRack(int x0, int z) {
        String[] skis = {"red_concrete", "blue_concrete", "yellow_concrete", "lime_concrete"};
        for (int i = 0; i < 4; i++) {
            int x = x0 + i;
            sym(x, 1, z, "spruce_fence");
            sym(x, 2, z, skis[i]);
            sym(x, 3, z, i % 2 == 0 ? skis[i] : "spruce_slab[type=bottom]");
        }
        sym(x0 + 4, 1, z, "stripped_spruce_log[axis=y]"); sym(x0 + 4, 2, z, "lantern");
    }

    private void snowman(int x, int z) {
        sym(x, 1, z, "snow_block");
        sym(x, 2, z, "snow_block");
        sym(x, 3, z, "carved_pumpkin[facing=south]");
        sym(x, 4, z, "black_concrete");
    }

    private void sled(int x, int z) {
        sym(x, 1, z, States.slab("spruce", false));
        sym(x, 1, z + 1, States.slab("spruce", false));
        sym(x, 1, z - 1, States.stairs("dark_oak", S, false));
        sym(x, 2, z - 1, "red_wool");
    }

    private void lampPost(int x, int z) {
        sym(x, 1, z, "dark_oak_fence");
        sym(x, 2, z, "dark_oak_fence");
        sym(x, 3, z, "dark_oak_fence");
        sym(x, 4, z, "lantern");
    }
}
