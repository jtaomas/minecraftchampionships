package com.mcm.games.battlebox.arena;

/**
 * Harbour Canal: a stone-lined canal divides the two halves, with moored cargo barges on the flanks,
 * a central footbridge to the dais island, and a dockside crane overlooking the water.
 */
public final class CanalShipsTheme extends ArenaTheme {
    @Override public String id() { return "canalships"; }
    @Override public String displayName() { return "Harbour Canal"; }
    @Override public String tagline() { return "Cross the canal, hold the barges"; }

    @Override
    public String layoutNotes() {
        return "Canal split: shallow canal (|z|<=6) with central footbridge to dais island | "
                + "Boathouse spawn (left/right front arches + side dock door) | "
                + "moored cargo barges with gangplanks | dockside crane platform (y 5)";
    }

    private static final Pal STONE = Pal.of("stone_bricks", 5, "mossy_stone_bricks", 2, "cracked_stone_bricks", 1, "andesite", 2);
    private static final Pal DECK = Pal.of("oak_planks", 6, "stripped_oak_wood[axis=x]", 2, "spruce_planks", 1);

    @Override
    protected String floorAt(int x, int z) {
        if (Math.abs(z) <= 6) {
            if (Math.abs(x) <= 5 && Math.abs(z) <= 4) return "stone_bricks"; // dais island
            if (Math.abs(x) <= 2) return "oak_planks"; // central footbridge
            return "water";
        }
        if (Math.abs(x) >= 15) return "cobblestone";
        return STONE.at(x, 0, z, seed);
    }

    @Override
    protected String subFloorAt(int x, int z) {
        return Math.abs(z) <= 6 ? "sand" : "stone";
    }

    @Override
    protected String wallAt(Face f, int u, int y) {
        if (y <= 1) return y <= 0 ? "stone_bricks" : "cobblestone";
        if (y == H) return "dark_oak_planks";
        if (y == H - 1) return checker(u, 0, "blue_concrete", "white_concrete");
        return sky(f, u, y, 3);
    }

    @Override
    protected String exteriorAt(Face f, int u, int y) {
        return y == 0 || y == H ? "stone_bricks" : "cobblestone";
    }

    @Override
    protected String ceilingAt(int x, int z) { return "barrier"; }

    @Override
    protected String[] roomPalette() {
        return new String[]{"oak_planks", "stone_bricks", "spruce_planks", "oak", "lantern"};
    }

    @Override
    protected String daisStairs() { return "stone_brick"; }

    @Override
    protected String daisBody() { return "stone_bricks"; }

    @Override
    protected void layout() {
        // Boathouse porch (z -18..-11, x -5..5, roof y 4): solid front screen at z=-11
        // blocks all spawn-to-spawn sightlines; only exits are left/right side arches and side dock door.
        symFill(-5, 4, -18, 5, 4, -11, "dark_oak_planks");
        symFill(-5, 1, -18, -5, 3, -11, "stone_bricks");
        symFill(5, 1, -18, 5, 3, -11, "stone_bricks");
        symFill(-5, 1, -11, 5, 3, -11, "stone_bricks");
        symFill(-5, 1, -12, -5, 3, -12, "air");
        symFill(5, 1, -12, 5, 3, -12, "air");
        symFill(-5, 1, -14, -5, 3, -14, "air");
        // Baffle pillars outside side exits to block diagonal spawn sightlines
        symFill(-7, 1, -11, -7, 4, -9, "stone_bricks");
        symFill(7, 1, -11, 7, 4, -9, "stone_bricks");

        exitDoor("left front arch", -5, 1, -12, -5, 3, -12);
        exitDoor("right front arch", 5, 1, -12, 5, 3, -12);
        exitDoor("side dock door", -5, 1, -14, -5, 3, -14);

        // Canal bed: deepen water where there is no bridge or island
        for (int x = -17; x <= 17; x++) {
            for (int z = -6; z <= 6; z++) {
                if (Math.abs(x) <= 5 && Math.abs(z) <= 4) continue;
                if (Math.abs(x) <= 2) continue;
                set(x, 0, z, "water");
                set(x, -1, z, "sand");
            }
        }

        // Central footbridge railings
        sym(-2, 1, -6, "oak_fence");
        sym(2, 1, -6, "oak_fence");
        sym(-2, 1, -5, "oak_fence");
        sym(2, 1, -5, "oak_fence");

        // Left flank: moored cargo barge (x -15..-9, z -6..0)
        symFill(-15, 0, -6, -9, 0, 0, "dark_oak_planks");
        symFill(-15, 1, -6, -15, 1, 0, "spruce_planks");
        symFill(-9, 1, -6, -9, 1, 0, "spruce_planks");
        symFill(-15, 1, -6, -9, 1, -6, "spruce_planks");
        symFill(-15, 1, 0, -9, 1, 0, "spruce_planks");
        // Gangplank from bank (z=-7) onto barge
        symFill(-13, 0, -7, -11, 0, -6, "oak_planks");
        symFill(-13, 1, -6, -11, 1, -6, "air");
        // Barge cargo crates
        symFill(-14, 1, -4, -12, 2, -2, "oak_planks");
        sym(-13, 3, -3, "lantern");
        // Gangplank from barge to dais island
        symFill(-9, 0, -3, -6, 0, -2, "oak_planks");
        symFill(-9, 1, -3, -9, 1, -2, "air");

        // Left back: dockside crane / warehouse platform (y 4)
        symFill(-16, 1, -12, -12, 3, -9, "stone_bricks");
        symFill(-16, 4, -12, -12, 4, -9, "oak_planks");
        staircase("crane stair", -11, -11, 0, "west", 4, 1, "oak", "oak_planks");

        // Right flank: cargo yard cover + pier
        symFill(8, 1, -11, 11, 2, -9, "stripped_spruce_wood[axis=x]");
        symFill(13, 1, -9, 15, 2, -7, "oak_planks");
        symFill(9, 0, -6, 11, 0, -3, "oak_planks"); // pier into canal
        sym(10, 1, -3, "lantern");

        waypoint("central footbridge", 0, 1, -6);
        waypoint("left barge deck", -12, 1, -5);
        waypoint("crane platform", -14, 5, -10);
        waypoint("right cargo yard", 10, 1, -8);
        waypoint("right pier", 10, 1, -4);
    }
}
