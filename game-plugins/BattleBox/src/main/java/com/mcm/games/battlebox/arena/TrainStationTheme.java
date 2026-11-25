package com.mcm.games.battlebox.arena;

/**
 * Grand Terminus: parallel passenger train carriages along the flanks, a central ticket-booth
 * divider blocking spawn-to-spawn sightlines, and an open concourse around the dais.
 */
public final class TrainStationTheme extends ArenaTheme {
    @Override public String id() { return "trainstation"; }
    @Override public String displayName() { return "Grand Terminus"; }
    @Override public String tagline() { return "All aboard the flank express"; }

    @Override
    public String layoutNotes() {
        return "Rail concourse: parallel train carriages on flanks (walk-through interiors + roof access) | "
                + "Terminus spawn (left/right platform doors + side alley) | "
                + "central ticket-booth divider | open concourse around dais";
    }

    private static final Pal CONCOURSE = Pal.of("polished_andesite", 6, "stone_bricks", 2, "andesite", 2);

    @Override
    protected String floorAt(int x, int z) {
        if (Math.abs(x) >= 10 && Math.abs(x) <= 14 && Math.abs(z) <= 12) return "gravel";
        return CONCOURSE.at(x, 0, z, seed);
    }

    @Override
    protected String subFloorAt(int x, int z) { return "stone"; }

    @Override
    protected String wallAt(Face f, int u, int y) {
        if (y <= 2) return y <= 0 ? "polished_deepslate" : "bricks";
        if (y == H) return "polished_deepslate";
        if (y == H - 1) return checker(u, 0, "yellow_concrete", "black_concrete");
        if (y >= 4 && y <= 8 && Math.abs(u) % 6 <= 2) return "glass_pane";
        return "terracotta";
    }

    @Override
    protected String exteriorAt(Face f, int u, int y) {
        return y == 0 || y == H ? "polished_deepslate" : "bricks";
    }

    @Override
    protected String ceilingAt(int x, int z) { return "glass"; }

    @Override
    protected String[] roomPalette() {
        return new String[]{"polished_andesite", "bricks", "dark_oak_planks", "iron_bars", "lantern"};
    }

    @Override
    protected String daisStairs() { return "polished_andesite"; }

    @Override
    protected String daisBody() { return "polished_andesite"; }

    @Override
    protected void layout() {
        // Terminus porch (z -18..-11, x -5..5, roof y 4): solid ticket-wall at z=-11
        symFill(-5, 4, -18, 5, 4, -11, "polished_deepslate");
        symFill(-5, 1, -18, -5, 3, -11, "bricks");
        symFill(5, 1, -18, 5, 3, -11, "bricks");
        symFill(-5, 1, -11, 5, 3, -11, "bricks");
        symFill(-5, 1, -12, -5, 3, -12, "air");
        symFill(5, 1, -12, 5, 3, -12, "air");
        symFill(-5, 1, -14, -5, 3, -14, "air");
        // Baffle pillars outside side exits to block diagonal spawn sightlines
        symFill(-7, 1, -11, -7, 4, -9, "bricks");
        symFill(7, 1, -11, 7, 4, -9, "bricks");

        exitDoor("left platform door", -5, 1, -12, -5, 3, -12);
        exitDoor("right platform door", 5, 1, -12, 5, 3, -12);
        exitDoor("side alley door", -5, 1, -14, -5, 3, -14);

        // Central ticket-booth / clock-pillar divider at z=-7
        symFill(-2, 1, -8, 2, 4, -6, "bricks");
        symFill(-1, 2, -8, 1, 3, -6, "glass_pane");
        sym(0, 5, -7, "lantern");

        // Left flank: passenger train carriage (x -14..-10, z -11..1)
        symFill(-14, 1, -11, -10, 4, 1, "cyan_terracotta");
        symFill(-13, 1, -10, -11, 3, 0, "air"); // hollow interior
        // Carriage doors (front, back, and two side doors)
        symFill(-13, 1, -11, -11, 3, -11, "air");
        symFill(-13, 1, 1, -11, 3, 1, "air");
        symFill(-10, 1, -7, -10, 3, -6, "air");
        symFill(-10, 1, -3, -10, 3, -2, "air");
        // Carriage windows
        sym(-10, 2, -9, "glass_pane");
        sym(-10, 2, -4, "glass_pane");
        sym(-10, 2, -1, "glass_pane");
        // Carriage roof + stair up from platform
        symFill(-14, 4, -11, -10, 4, 1, "polished_deepslate");
        staircase("carriage roof stair", -9, -11, 0, "west", 4, 1, "polished_andesite", "polished_andesite");

        // Right flank: luggage carts & platform benches
        symFill(8, 1, -10, 10, 2, -8, "oak_planks");
        symFill(12, 1, -6, 14, 2, -4, "stripped_spruce_wood[axis=x]");
        sym(9, 3, -9, "lantern");

        waypoint("concourse centre-left", -5, 1, -7);
        waypoint("carriage interior", -12, 1, -5);
        waypoint("carriage roof", -12, 5, -5);
        waypoint("right platform", 9, 1, -6);
    }
}
