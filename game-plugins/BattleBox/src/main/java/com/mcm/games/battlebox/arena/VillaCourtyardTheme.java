package com.mcm.games.battlebox.arena;

/**
 * Sunlit Villa: terracotta-and-sandstone courtyard with side-exit loggias (no front door),
 * cypress-hedge lanes, and a raised colonnade balcony overlooking the central dais.
 */
public final class VillaCourtyardTheme extends ArenaTheme {
    @Override public String id() { return "villacourtyard"; }
    @Override public String displayName() { return "Sunlit Villa"; }
    @Override public String tagline() { return "Side loggias, cypress lanes"; }

    @Override
    public String layoutNotes() {
        return "Villa courtyard: no front door — side-tucked loggia exits + garden arch | "
                + "cypress-hedge 3-lane courtyard | raised colonnade balcony (y 4) on left flank | "
                + "terracotta pillars and fountain cover";
    }

    private static final Pal PAVING = Pal.of("smooth_sandstone", 6, "sandstone", 2, "cut_sandstone", 2);

    @Override
    protected String floorAt(int x, int z) {
        if (Math.abs(x) >= 13 && Math.abs(z) <= 12) return "grass_block";
        return PAVING.at(x, 0, z, seed);
    }

    @Override
    protected String subFloorAt(int x, int z) { return "sandstone"; }

    @Override
    protected String wallAt(Face f, int u, int y) {
        if (y <= 1) return y <= 0 ? "sandstone" : "terracotta";
        if (y == H) return "smooth_sandstone";
        if (y == H - 1) return checker(u, 0, "orange_concrete", "white_concrete");
        return sky(f, u, y, 3);
    }

    @Override
    protected String exteriorAt(Face f, int u, int y) {
        return y == 0 || y == H ? "smooth_sandstone" : "terracotta";
    }

    @Override
    protected String ceilingAt(int x, int z) { return "barrier"; }

    @Override
    protected String[] roomPalette() {
        return new String[]{"smooth_sandstone", "terracotta", "dark_oak_planks", "sandstone", "lantern"};
    }

    @Override
    protected String daisStairs() { return "smooth_sandstone"; }

    @Override
    protected String daisBody() { return "smooth_sandstone"; }

    @Override
    protected void layout() {
        // Villa Atrium (z -18..-11, x -5..5, roof y 4): solid front wall at z=-11 (no front door!)
        symFill(-5, 4, -18, 5, 4, -11, "smooth_sandstone");
        symFill(-5, 1, -11, 5, 3, -11, "terracotta");
        symFill(-5, 1, -18, -5, 3, -12, "terracotta");
        symFill(5, 1, -18, 5, 3, -12, "terracotta");
        // Side-tucked loggia exits at x=-5 and x=5 (z -13..-12), plus a back-garden arch at x=-5, z=-13
        symFill(-5, 1, -12, -5, 3, -12, "air");
        symFill(5, 1, -12, 5, 3, -12, "air");
        symFill(-5, 1, -13, -5, 3, -13, "air");
        // Baffle terracotta pillars outside side loggias to block diagonal sightlines
        symFill(-7, 1, -11, -7, 3, -9, "terracotta");
        symFill(7, 1, -11, 7, 3, -9, "terracotta");

        exitDoor("left loggia", -5, 1, -12, -5, 3, -12);
        exitDoor("right loggia", 5, 1, -12, 5, 3, -12);
        exitDoor("garden arch", -5, 1, -13, -5, 3, -13);

        // Cypress hedges forming 3-lane courtyard
        symFill(-5, 1, -9, -3, 3, -8, "oak_leaves[persistent=true]");
        symFill(3, 1, -9, 5, 3, -8, "oak_leaves[persistent=true]");
        symFill(-1, 1, -7, 1, 2, -6, "smooth_sandstone"); // central planter cover
        sym(0, 3, -7, "lantern");

        // Left flank: raised colonnade balcony (y 4)
        symFill(-15, 1, -11, -11, 3, -4, "smooth_sandstone");
        symFill(-14, 1, -10, -12, 3, -5, "air"); // shaded colonnade underpass
        symFill(-11, 1, -9, -11, 3, -6, "air"); // side arch into underpass
        symFill(-15, 4, -11, -11, 4, -4, "terracotta");
        staircase("colonnade stair", -10, -11, 0, "west", 4, 1, "smooth_sandstone", "smooth_sandstone");

        // Right flank: cypress garden & fountain cover
        symFill(9, 1, -10, 11, 3, -9, "oak_leaves[persistent=true]");
        symFill(12, 1, -6, 14, 2, -4, "smooth_sandstone");
        sym(13, 3, -5, "lantern");

        waypoint("left loggia court", -8, 1, -12);
        waypoint("colonnade underpass", -13, 1, -7);
        waypoint("colonnade balcony", -13, 5, -7);
        waypoint("right cypress garden", 10, 1, -7);
    }
}
