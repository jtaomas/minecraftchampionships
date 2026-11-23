package com.mcm.games.findthebutton.map;

import static com.mcm.games.findthebutton.map.Spot.Dir.*;

/** Old library: book walls, a reading gallery along the north wall reached by a staircase, a cold fireplace. */
public final class LibraryMap extends FtbMap {
    public LibraryMap() { super(7); }

    @Override public String id() { return "library"; }
    @Override public String displayName() { return "Library"; }
    @Override public String tagline() { return "Shh... it's somewhere between the books"; }

    @Override
    protected void generate() {
        Pal floor = checker("minecraft:dark_oak_planks", "minecraft:spruce_planks");
        Pal wall = (x, y, z) -> {
            int along = (x == 0 || x == 21) ? z : x;
            if (along % 5 == 0 || along == 21) return "minecraft:stripped_dark_oak_log[axis=y]";
            if (y == 1 || y == height) return "minecraft:dark_oak_planks";
            return hash(x, y, z, 11) < 0.12 ? "minecraft:chiseled_bookshelf[facing=" + inward(x, z) + "]" : "minecraft:bookshelf";
        };
        shell(floor, wall, mix(3, "minecraft:dark_oak_planks", 3, "minecraft:spruce_planks", 1));

        // ceiling beams with hanging lanterns
        for (int x = 1; x <= 20; x++) {
            set(x, 7, 7, log("minecraft:stripped_dark_oak_log", 'x'));
            set(x, 7, 14, log("minecraft:stripped_dark_oak_log", 'x'));
        }
        for (int x : new int[]{4, 9}) hangingLantern(x, 6, 7, 6);
        for (int x : new int[]{4, 10, 16}) hangingLantern(x, 6, 14, 6);

        // ---- reading gallery along the north wall (floor y = 4, players stand at 5)
        fill(1, 4, 1, 20, 4, 5, mix(5, "minecraft:spruce_planks", 4, "minecraft:stripped_spruce_wood[axis=x]", 1));
        for (int x : new int[]{5, 10, 15}) fill(x, 1, 5, x, 3, 5, "minecraft:stripped_dark_oak_log[axis=y]");
        for (int x = 1; x <= 20; x++) if (x != 17) set(x, 5, 5, "minecraft:dark_oak_fence");
        set(1, 5, 1, "minecraft:barrel[facing=up]");                          // gallery corner barrel
        // spot: on top of the barrel in the gallery corner
        spot("gallery barrel", "medium", 1, 6, 1, DOWN);
        set(2, 5, 1, "minecraft:lectern[facing=south]");
        set(8, 5, 1, stairs("minecraft:spruce", "north", false));            // reading chairs
        set(9, 5, 1, stairs("minecraft:spruce", "north", false));
        set(12, 5, 1, "minecraft:barrel[facing=up]");
        set(12, 6, 1, "minecraft:potted_fern");
        // spot: on the north face of the ceiling beam, only reachable from the gallery
        spot("ceiling beam (from the gallery)", "hard", 12, 7, 6, SOUTH);

        // staircase up to the gallery (x = 17, climbing north), hollow storage nook under the top steps
        staircase("gallery stairs", "minecraft:spruce", 17, 9, 0, 0, -1, 4);
        set(17, 3, 6, "minecraft:spruce_planks");
        fill(17, 1, 7, 17, 2, 7, "minecraft:spruce_planks");
        set(17, 1, 8, "minecraft:spruce_planks");
        fill(18, 1, 6, 18, 3, 6, "minecraft:bookshelf");
        spot("under the gallery stairs", "medium", 17, 1, 6, EAST);

        // under the gallery: reading corner and a button on the gallery's underside
        set(3, 1, 2, stairs("minecraft:dark_oak", "north", false));
        set(4, 1, 2, stairs("minecraft:dark_oak", "north", false));
        set(2, 1, 1, "minecraft:barrel[facing=up]");
        set(2, 2, 1, "minecraft:candle[candles=3,lit=true]");
        set(13, 1, 1, "minecraft:chest[facing=south]");
        set(14, 1, 1, "minecraft:barrel[facing=south]");
        spot("underside of the gallery", "easy", 8, 3, 3, UP);
        spot("behind the gallery pillar", "easy", 10, 1, 4, SOUTH);

        // ---- cold fireplace on the east wall
        fill(21, 1, 11, 21, 7, 15, mix(7, "minecraft:bricks", 4, "minecraft:mud_bricks", 1));
        fill(20, 1, 11, 20, 7, 15, mix(8, "minecraft:bricks", 4, "minecraft:mud_bricks", 1));
        fill(20, 1, 12, 20, 2, 14, (String) null);
        set(20, 1, 13, "minecraft:campfire[lit=false,facing=west]");
        for (int z = 11; z <= 15; z++) set(19, 4, z, slab("minecraft:dark_oak", true));
        set(19, 5, 12, "minecraft:candle[candles=2,lit=true]");
        set(19, 5, 14, "minecraft:candle[candles=1,lit=true]");
        spot("inside the cold fireplace", "medium", 20, 1, 14, SOUTH);
        set(19, 1, 16, "minecraft:barrel[facing=up]");
        set(19, 2, 16, "minecraft:potted_fern");
        spot("behind the potted fern", "easy", 20, 1, 16, DOWN);

        // ---- reading desks with chairs
        table(6, 9, 8, 10, "minecraft:spruce_fence", "minecraft:dark_oak");
        set(6, 1, 8, stairs("minecraft:spruce", "north", false));
        set(8, 1, 11, stairs("minecraft:spruce", "south", false));
        set(7, 3, 9, "minecraft:lantern[hanging=false]");
        spot("under the reading desk", "medium", 7, 1, 10, DOWN);
        table(12, 9, 14, 10, "minecraft:spruce_fence", "minecraft:dark_oak");
        set(12, 1, 8, stairs("minecraft:spruce", "north", false));
        set(14, 1, 11, stairs("minecraft:spruce", "south", false));
        set(13, 3, 10, "minecraft:candle[candles=4,lit=true]");
        set(10, 1, 12, "minecraft:lectern[facing=south]");

        // ---- freestanding double book stack (z = 15..16), one book missing on the north face
        fill(2, 1, 15, 7, 3, 16, "minecraft:bookshelf");
        set(4, 2, 15, null);
        spot("gap in the book stack", "easy", 4, 2, 15, SOUTH);
        spot("between the book stack and the wall", "medium", 1, 2, 15, EAST);
        for (int x = 2; x <= 7; x++) if (x % 2 == 0) set(x, 4, 15, "minecraft:potted_azure_bluet");

        // ---- hollow niche in the west book wall at knee height
        set(0, 2, 12, null);
        set(-1, 2, 12, "minecraft:bookshelf");
        spot("hollow in the book wall", "medium", 0, 2, 12, WEST);

        // ---- red reading-room runner
        for (int z = 11; z <= 17; z++) { set(10, 1, z, "minecraft:red_carpet"); set(11, 1, z, "minecraft:red_carpet"); }
        set(10, 1, 12, "minecraft:lectern[facing=south]");

        lightGrid(4, 6);
        lightGrid(4, 2);
    }

    private static String inward(int x, int z) {
        if (z == 0) return "south";
        if (z == 21) return "north";
        if (x == 0) return "east";
        return "west";
    }
}
