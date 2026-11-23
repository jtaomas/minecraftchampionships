package com.mcm.games.findthebutton.map;

import static com.mcm.games.findthebutton.map.Spot.Dir.*;

/**
 * Below decks of a sailing ship: crew quarters (south, start) with hammocks, a cannon, cargo and a raised hatch
 * ledge; through the bulkhead door the captain's cabin with desk, chart cabinet and bookshelf.
 */
public final class ShipCabinMap extends FtbMap {
    public ShipCabinMap() { super(7); }

    @Override public String id() { return "shipcabin"; }
    @Override public String displayName() { return "Ship Cabin"; }
    @Override public int previewCutY() { return 5; }
    @Override public String tagline() { return "Search the decks before the captain returns"; }

    @Override
    protected void generate() {
        Pal floor = (x, y, z) -> (x % 4 == 0) ? "minecraft:dark_oak_planks" : "minecraft:spruce_planks";
        Pal hull = (x, y, z) -> {
            int along = (x == 0 || x == 21) ? z : x;
            if (along % 4 == 0) return "minecraft:stripped_spruce_log[axis=y]";
            if (y == 1) return "minecraft:dark_oak_planks";
            return hash(x, y, z, 61) < 0.2 ? "minecraft:stripped_spruce_wood[axis=y]" : "minecraft:spruce_planks";
        };
        shell(floor, hull, one("minecraft:dark_oak_planks"));
        // deck beams (axis z) at y = 6
        for (int x = 4; x <= 16; x += 4) for (int z = 1; z <= 20; z++) set(x, 6, z, log("minecraft:stripped_spruce_log", 'z'));
        // portholes (light-blue glass, sea behind)
        for (int z : new int[]{5, 15}) {
            set(21, 3, z, "minecraft:light_blue_stained_glass");
        }

        // ---- bulkhead (z 10..11) with a doorway at x 10..11
        fill(1, 1, 10, 20, 7, 11, mix(62, "minecraft:spruce_planks", 3, "minecraft:dark_oak_planks", 1));
        fill(10, 1, 10, 11, 2, 11, (String) null);
        for (int y = 1; y <= 3; y++) { set(9, y, 11, "minecraft:stripped_spruce_log[axis=y]"); set(12, y, 11, "minecraft:stripped_spruce_log[axis=y]"); }
        set(10, 3, 11, log("minecraft:stripped_spruce_log", 'x')); set(11, 3, 11, log("minecraft:stripped_spruce_log", 'x'));

        // ===== captain's cabin (z 1..9)
        table(8, 3, 11, 4, "minecraft:dark_oak_fence", "minecraft:dark_oak");
        set(9, 1, 5, stairs("minecraft:dark_oak", "south", false));
        set(9, 3, 3, "minecraft:candle[candles=3,lit=true]");
        spot("under the captain's desk", "medium", 10, 1, 4, DOWN);
        // chart cabinet along the north wall, one cubby empty
        fill(3, 1, 1, 6, 2, 1, "minecraft:dark_oak_planks");
        set(3, 2, 1, "minecraft:barrel[facing=south]"); set(6, 2, 1, "minecraft:barrel[facing=south]");
        set(5, 2, 1, null);
        set(4, 3, 1, "minecraft:potted_bamboo");
        spot("empty cubby of the chart cabinet", "medium", 5, 2, 1, NORTH);
        set(13, 1, 1, "minecraft:cartography_table");
        set(14, 1, 1, "minecraft:lectern[facing=south]");
        // bookshelf with a gap behind it (x = 20 corridor)
        fill(19, 1, 2, 19, 3, 4, "minecraft:bookshelf");
        spot("behind the captain's bookshelf", "medium", 20, 1, 3, EAST);
        // porthole niche in the west hull
        set(0, 2, 6, null);
        set(-1, 2, 6, "minecraft:light_blue_stained_glass");
        spot("in the porthole niche", "medium", 0, 2, 6, WEST);
        spot("under a deck beam", "medium", 12, 5, 6, UP);
        set(2, 1, 8, "minecraft:chest[facing=east]");
        set(1, 1, 8, "minecraft:barrel[facing=up]");
        hangingLantern(8, 5, 7, 5);
        hangingLantern(16, 5, 3, 5);

        // ===== crew quarters (z 12..20)
        // raised hatch ledge on the east side (x 16..20, z 12..20, top at y = 4) with stairs climbing east
        fill(16, 1, 12, 20, 3, 20, mix(63, "minecraft:spruce_planks", 3, "minecraft:dark_oak_planks", 1));
        staircase("ledge stairs", "minecraft:spruce", 13, 13, 0, 1, 0, 3);
        set(14, 1, 13, "minecraft:spruce_planks");
        for (int z = 12; z <= 20; z++) if (z != 13) set(16, 4, z, "minecraft:spruce_fence");
        set(19, 4, 20, "minecraft:chest[facing=north]");
        set(20, 4, 19, "minecraft:barrel[facing=up]");
        spot("up on the hatch ledge", "hard", 20, 4, 20, DOWN);
        set(18, 6, 17, "minecraft:spruce_trapdoor[facing=north,half=top,open=false]");
        // nook under the top step (x = 15)
        spot("under the ledge stairs", "medium", 15, 1, 13, EAST);

        // hammocks (wool slung between chains)
        for (int x = 3; x <= 6; x++) set(x, 3, 17, "minecraft:white_wool");
        set(2, 3, 17, "minecraft:chain[axis=x]"); set(7, 3, 17, "minecraft:chain[axis=x]");
        for (int x = 9; x <= 12; x++) set(x, 3, 14, "minecraft:light_gray_wool");
        set(8, 3, 14, "minecraft:chain[axis=x]"); set(13, 3, 14, "minecraft:chain[axis=x]");
        spot("under a hammock", "easy", 4, 2, 17, UP);

        // cannon on the west side, aimed at a gun port
        set(2, 1, 13, "minecraft:dark_oak_planks"); set(3, 1, 13, "minecraft:dark_oak_planks");
        set(2, 2, 13, "minecraft:black_concrete"); set(3, 2, 13, "minecraft:black_concrete");
        set(1, 2, 13, "minecraft:polished_blackstone_wall");
        set(0, 2, 13, "minecraft:iron_bars");
        spot("under the cannon muzzle", "medium", 1, 1, 13, WEST);

        // cargo barrels in the south-west corner
        set(1, 1, 19, "minecraft:barrel[facing=up]"); set(2, 1, 19, "minecraft:barrel[facing=up]");
        set(1, 2, 19, "minecraft:barrel[facing=up]"); set(2, 1, 20, "minecraft:barrel[facing=up]");
        set(3, 1, 20, "minecraft:spruce_planks");
        spot("behind the cargo barrels", "medium", 1, 1, 20, DOWN);
        hangingLantern(8, 5, 18, 5);
        hangingLantern(12, 5, 13, 5);

        lightGrid(4, 5);
        lightGrid(5, 2);
    }
}
