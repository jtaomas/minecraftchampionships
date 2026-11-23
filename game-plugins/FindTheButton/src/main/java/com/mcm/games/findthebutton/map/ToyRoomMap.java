package com.mcm.games.findthebutton.map;

import static com.mcm.games.findthebutton.map.Spot.Dir.*;

/** Kid's toy room: play fort with a tower, dollhouse, toy train tunnel, block pyramid, wardrobe, teddy bear. */
public final class ToyRoomMap extends FtbMap {
    public ToyRoomMap() { super(7); }

    @Override public String id() { return "toyroom"; }
    @Override public String displayName() { return "Toy Room"; }
    @Override public String tagline() { return "Tidy up! Where did the button roll?"; }

    private static final String[] BLOCKS = {"minecraft:red_concrete", "minecraft:yellow_concrete", "minecraft:blue_concrete",
            "minecraft:lime_concrete", "minecraft:orange_concrete", "minecraft:magenta_concrete"};

    private static String toy(int x, int y, int z) { return BLOCKS[(int) (hash(x, y, z, 51) * BLOCKS.length)]; }

    @Override
    protected void generate() {
        Pal floor = checker("minecraft:white_concrete", "minecraft:light_blue_concrete");
        Pal wall = (x, y, z) -> {
            if (y == 1) return "minecraft:white_concrete";
            if (y == 3) return "minecraft:yellow_terracotta";
            if (y == 2) return "minecraft:light_blue_terracotta";
            return ((x + z) / 3) % 2 == 0 ? "minecraft:white_terracotta" : "minecraft:pink_terracotta";
        };
        shell(floor, wall, mix(52, "minecraft:white_concrete", 6, "minecraft:light_blue_concrete", 1, "minecraft:pearlescent_froglight[axis=y]", 0.3));

        // ---- play fort (x 1..7, z 1..6, top at y = 3) with crenellations, stairs from the south, tower in the corner
        fill(1, 1, 1, 7, 2, 6, mix(53, "minecraft:stone_bricks", 3, "minecraft:white_concrete", 1));
        staircase("fort stairs", "minecraft:stone_brick", 6, 8, 0, 0, -1, 2);
        for (int x = 1; x <= 7; x++) if (x != 6) set(x, 3, 6, (x & 1) == 0 ? "minecraft:stone_brick_wall" : "minecraft:red_concrete");
        for (int z = 1; z <= 6; z++) set(7, 3, z, (z & 1) == 0 ? "minecraft:stone_brick_wall" : "minecraft:blue_concrete");
        // tower 3x3 (x 1..3, z 1..3) from y = 3 to 5, hollow centre, door on the south side
        fill(1, 3, 1, 3, 5, 3, "minecraft:white_concrete");
        fill(2, 3, 2, 2, 4, 2, (String) null);
        fill(2, 3, 3, 2, 4, 3, (String) null);
        for (int x = 1; x <= 3; x++) for (int z = 1; z <= 3; z++) if ((x + z) % 2 == 0) set(x, 6, z, "minecraft:red_concrete");
        spot("inside the fort tower", "hard", 2, 4, 2, NORTH);
        // cubby hole in the fort's east face
        set(7, 2, 3, null);
        spot("cubby hole in the play fort", "medium", 7, 2, 3, WEST);
        set(8, 1, 1, "minecraft:red_banner[rotation=8]");

        // ---- dollhouse (x 14..18, z 2..5), door on the south side
        fill(14, 1, 2, 18, 3, 5, "minecraft:pink_concrete");
        for (int y = 1; y <= 3; y++) { set(14, y, 2, "minecraft:white_concrete"); set(18, y, 2, "minecraft:white_concrete"); set(14, y, 5, "minecraft:white_concrete"); set(18, y, 5, "minecraft:white_concrete"); }
        fill(15, 1, 3, 17, 3, 4, (String) null);
        fill(16, 1, 5, 16, 2, 5, (String) null);
        set(14, 2, 3, "minecraft:glass_pane"); set(18, 2, 4, "minecraft:glass_pane");
        for (int x = 13; x <= 19; x++) {
            set(x, 4, 2, stairs("minecraft:red_nether_brick", "south", false));
            set(x, 4, 5, stairs("minecraft:red_nether_brick", "north", false));
            set(x, 4, 3, "minecraft:red_nether_bricks"); set(x, 4, 4, "minecraft:red_nether_bricks");
            set(x, 5, 3, stairs("minecraft:red_nether_brick", "south", false));
            set(x, 5, 4, stairs("minecraft:red_nether_brick", "north", false));
        }
        set(17, 1, 3, "minecraft:white_bed[facing=north,part=head]");
        set(17, 1, 4, "minecraft:white_bed[facing=north,part=foot]");
        spot("inside the dollhouse", "medium", 15, 2, 3, NORTH);
        spot("behind the dollhouse", "medium", 16, 1, 1, SOUTH);

        // ---- toy block pyramid (x 9..11, z 4..6), one block missing
        for (int x = 9; x <= 11; x++) for (int z = 4; z <= 6; z++) set(x, 1, z, toy(x, 1, z));
        set(10, 2, 5, toy(10, 2, 5));
        set(10, 1, 4, null);
        spot("gap in the toy block pyramid", "medium", 10, 1, 4, SOUTH);

        // ---- toy train: rails in a loop with a green hill tunnel (x 12..14, z 12..14)
        for (int x = 12; x <= 18; x++) { set(x, 1, 11, "minecraft:rail[shape=east_west]"); set(x, 1, 16, "minecraft:rail[shape=east_west]"); }
        for (int z = 12; z <= 15; z++) { set(13, 1, z, "minecraft:rail[shape=north_south]"); set(18, 1, z, "minecraft:rail[shape=north_south]"); }
        fill(12, 1, 12, 14, 3, 14, mix(54, "minecraft:lime_concrete", 3, "minecraft:green_concrete", 1));
        fill(13, 1, 12, 13, 2, 14, (String) null);
        for (int z = 12; z <= 14; z++) set(13, 1, z, "minecraft:rail[shape=north_south]");
        set(13, 4, 13, "minecraft:lime_concrete");
        spot("ceiling of the train tunnel", "medium", 13, 2, 13, UP);

        // ---- wardrobe (x 1..2, z 14..16, 3 high) with toy blocks to climb
        fill(1, 1, 14, 2, 3, 16, "minecraft:birch_planks");
        for (int y = 1; y <= 3; y++) set(2, y, 15, "minecraft:stripped_birch_log[axis=y]");
        set(3, 1, 14, toy(3, 1, 14));
        set(3, 1, 15, toy(3, 1, 15)); set(3, 2, 15, toy(3, 2, 15));
        spot("on top of the wardrobe (climb the blocks)", "hard", 1, 4, 15, DOWN);

        // ---- toy chest and teddy bear on the east side
        set(19, 1, 10, "minecraft:chest[facing=west]");
        set(19, 1, 11, "minecraft:chest[facing=west]");
        spot("behind the toy chest", "easy", 20, 1, 10, DOWN);
        fill(19, 1, 19, 19, 2, 20, "minecraft:brown_wool");
        set(19, 3, 20, "minecraft:brown_wool");
        set(20, 1, 19, "minecraft:brown_wool");
        set(19, 3, 19, "minecraft:brown_wool");
        spot("behind the teddy bear", "medium", 20, 1, 20, DOWN);

        // rug in the middle and a ceiling mobile
        for (int x = 7; x <= 11; x++) for (int z = 9; z <= 13; z++) set(x, 1, z, ((x + z) & 1) == 0 ? "minecraft:yellow_carpet" : "minecraft:white_carpet");
        for (int[] m : new int[][]{{8, 10}, {10, 12}, {9, 9}}) {
            set(m[0], 7, m[1], "minecraft:chain[axis=y]");
            set(m[0], 6, m[1], toy(m[0], 6, m[1]));
        }
        spot("under the ceiling mobile", "medium", 9, 5, 9, UP);

        lightGrid(4, 6);
        lightGrid(5, 2);
    }
}
