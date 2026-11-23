package com.mcm.games.battlebox.arena;

/** The decorated holding platform used between rounds (pure Java). Players stand at (0, 1, 0). */
public final class HoldingArea {
    private HoldingArea() {}

    public static final int RADIUS = 10;

    public static Grid generate() {
        Grid g = new Grid(-RADIUS - 2, -4, -RADIUS - 2, RADIUS + 2, 8, RADIUS + 2);
        String[] ring = {"red_concrete", "orange_concrete", "yellow_concrete", "lime_concrete", "light_blue_concrete",
                "blue_concrete", "purple_concrete", "pink_concrete"};
        for (int x = -RADIUS - 1; x <= RADIUS + 1; x++)
            for (int z = -RADIUS - 1; z <= RADIUS + 1; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > RADIUS + 0.5) continue;
                String top;
                if (d > RADIUS - 0.5) top = ((x + z) & 1) == 0 ? "yellow_concrete" : "black_concrete";
                else if (d > RADIUS - 1.5) top = "smooth_quartz";
                else if (Math.max(Math.abs(x), Math.abs(z)) <= 1) top = "sea_lantern";
                else if (Math.max(Math.abs(x), Math.abs(z)) == 2) top = ((x + z) & 1) == 0 ? "yellow_concrete" : "black_concrete";
                else {
                    int sector = (int) Math.floor((Math.atan2(z, x) + Math.PI) / (2 * Math.PI) * 8) % 8;
                    top = d < 6 ? "white_concrete" : ring[sector];
                }
                g.set(x, 0, z, top);
                int depth = 1 + (int) ((RADIUS + 0.5 - d) * 0.35);
                for (int y = -1; y >= -depth; y--) g.set(x, y, z, y == -1 ? "quartz_bricks" : "light_gray_concrete");
                if (d > RADIUS - 0.5) {
                    g.set(x, 1, z, "white_stained_glass_pane");
                    for (int y = 2; y <= 6; y++) g.set(x, y, z, "barrier");
                }
            }
        // lamp posts with banners on the cardinal points
        int[][] posts = {{0, -7}, {7, 0}, {0, 7}, {-7, 0}, {5, 5}, {-5, -5}, {5, -5}, {-5, 5}};
        for (int i = 0; i < posts.length; i++) {
            int[] p = posts[i];
            for (int y = 1; y <= 3; y++) g.set(p[0], y, p[1], "quartz_pillar");
            g.set(p[0], 4, p[1], "sea_lantern");
            g.set(p[0], 5, p[1], ring[i].replace("concrete", "banner"));
        }
        g.connect();
        return g;
    }
}
