package com.mcm.games.rocketspleef.map;

/**
 * Easter Eggs! (MCC 4). Reference: research/mcc_archive/rocket_spleef_old/frames/wiki/Easter_Eggs.png. A dense floating
 * cluster of EGG-SHAPED eggs (taller than wide, narrow at the top) around a central big cyan block with a white
 * "windows" pattern and stuff on top. Some eggs are open at the top with a brown (chocolate) inside, some have
 * light-grey caps, some are striped. A dark-blue crescent / egg-cup piece with a white square sits on the left.
 * Image left = -x, image bottom = towards the camera (+z) and lower.
 */
public final class EasterEggsMap implements MapGen {
    enum Pattern { SOLID, DIAMONDS, SPOTS, DOTS, STRIPES, SQUARES }

    /** centre, horizontal radius, base colour, pattern, pattern colour, open top, grey cap */
    record Egg(double x, double y, double z, double r, String base, Pattern pat, String patId, boolean open, boolean cap) {}

    static final String W = "minecraft:white_concrete";
    static final String BROWN = "minecraft:brown_concrete";
    static final String CAP = "minecraft:light_gray_concrete";
    static final String CYAN = "minecraft:light_blue_concrete";   // the reference's centre piece is a bright sky-cyan
    static final String BLUE = "minecraft:blue_concrete";
    static final double STRETCH = 1.45;  // egg height / width
    static final double TAPER = 0.28;    // narrower at the top, wider at the bottom

    /** the central block: centre x/z, half width, bottom y, height */
    static final int BOX_HALF = 7, BOX_Y = 12, BOX_H = 11;

    static final Egg[] EGGS = {
            // around the centre block, touching it
            new Egg(-12, 15, 3, 5.5, "minecraft:yellow_concrete", Pattern.DIAMONDS, W, false, false),
            new Egg(13, 17, -1, 6.0, "minecraft:magenta_concrete", Pattern.DIAMONDS, W, false, false),
            new Egg(-6, 8, 11, 3.5, BLUE, Pattern.DOTS, W, false, false),
            new Egg(3, 6, 12, 4.0, "minecraft:orange_concrete", Pattern.DOTS, W, true, false),
            new Egg(11, 9, 9, 3.5, "minecraft:pink_concrete", Pattern.SOLID, W, true, false),
            new Egg(5, 21, -10, 3.5, W, Pattern.STRIPES, "minecraft:lime_concrete", false, false),
            // outer ring, different heights
            new Egg(25, 13, 4, 5.0, W, Pattern.SPOTS, "minecraft:red_concrete", false, false),
            new Egg(-23, 8, 10, 4.5, W, Pattern.SPOTS, "minecraft:red_concrete", true, false),
            new Egg(-6, 1, 18, 4.5, W, Pattern.STRIPES, "minecraft:lime_concrete", true, false),
            new Egg(15, 2, 15, 4.5, "minecraft:pink_concrete", Pattern.SOLID, W, false, true),
            new Egg(6, -5, 23, 5.0, "minecraft:cyan_concrete", Pattern.STRIPES, W, false, true),
            new Egg(-2, -7, 27, 4.5, "minecraft:magenta_concrete", Pattern.SOLID, W, false, false),
            new Egg(-15, -6, 24, 4.0, W, Pattern.SPOTS, "minecraft:green_concrete", false, false),
            new Egg(-15, 23, -9, 4.0, W, Pattern.STRIPES, "minecraft:green_concrete", false, false),
            new Egg(10, 28, -14, 3.0, W, Pattern.SPOTS, "minecraft:lime_concrete", false, false),
            new Egg(-5, 28, -14, 3.5, "minecraft:cyan_concrete", Pattern.SOLID, W, false, true),
            new Egg(21, 24, -13, 4.0, "minecraft:orange_concrete", Pattern.STRIPES, W, false, false),
            new Egg(-23, 25, -16, 4.0, "minecraft:lime_concrete", Pattern.DOTS, W, false, false),
    };

    @Override public String id() { return "easter_eggs"; }
    @Override public String displayName() { return "Easter Eggs!"; }
    @Override public String reference() { return "game-plugins/research/mcc_archive/rocket_spleef_old/frames/wiki/Easter_Eggs.png"; }

    @Override
    public Canvas generate() {
        Canvas c = new Canvas();
        centreBlock(c);
        crescent(c, -31, 18, -3, 6.5);
        for (Egg e : EGGS) egg(c, e);
        return c;
    }

    /** Big cyan block with white 2x2 "windows" on every side, a white rim on top, and clutter on top. */
    private static void centreBlock(Canvas c) {
        int h = BOX_HALF;
        for (int x = -h; x <= h; x++)
            for (int z = -h; z <= h; z++)
                for (int y = BOX_Y; y < BOX_Y + BOX_H; y++) {
                    boolean side = Math.abs(x) == h || Math.abs(z) == h;
                    String id = CYAN;
                    if (side) {
                        int u = Math.abs(x) == h ? z + h : x + h;          // across the face
                        int v = y - BOX_Y;
                        if (v >= 2 && v <= BOX_H - 3 && Math.floorMod(u - 2, 4) < 2 && Math.floorMod(v - 2, 4) < 2) id = W;
                    }
                    if (y == BOX_Y + BOX_H - 1 && (Math.abs(x) == h || Math.abs(z) == h)) id = W;   // white rim
                    c.set(x, y, z, id);
                }
        int top = BOX_Y + BOX_H;
        // stuff on top: cyan stubs, white chunks, a chocolate lump, a small blue egg and a pink bit
        int[][] stubs = {{-5, -5, 4}, {-1, -6, 2}, {4, -4, 3}, {-6, 1, 2}, {5, 3, 1}};
        for (int[] s : stubs)
            for (int dx = 0; dx < 2; dx++) for (int dz = 0; dz < 2; dz++)
                for (int y = 0; y < s[2]; y++) c.set(s[0] + dx, top + y, s[1] + dz, y == s[2] - 1 ? W : CYAN);
        c.ellipsoid(1.5, top + 1.5, 0.5, 3.0, 2.0, 2.5, BROWN);
        c.ellipsoid(-3, top + 1, 3, 1.5, 1.2, 1.5, W);
        c.ellipsoid(3.5, top + 1, 4, 1.4, 1.0, 1.4, "minecraft:pink_concrete");
        egg(c, new Egg(5, top + 3.5, -2, 2.4, BLUE, Pattern.SOLID, W, false, false));
    }

    /** The dark-blue crescent / egg-cup on the left: a blue egg with a big bite out of its right side, white square. */
    private static void crescent(Canvas c, double ex, double ey, double ez, double r) {
        Canvas tmp = new Canvas();
        egg(tmp, new Egg(ex, ey, ez, r, BLUE, Pattern.SOLID, W, false, false));
        double bx = ex + r * 0.7, by = ey + r * 0.55, bz = ez + r * 0.55, br = r * 0.9;   // bite faces the camera-right, like the image
        tmp.forEach((x, y, z, id) -> {
            double dx = x + 0.5 - bx, dy = (y + 0.5 - by) / 1.2, dz = z + 0.5 - bz;
            if (dx * dx + dy * dy + dz * dz < br * br) return;   // the bite
            c.set(x, y, z, id);
        });
        // white square on the front (+z) face
        int fz = (int) Math.floor(ez + r * 0.9);
        for (int x = (int) ex - 2; x <= (int) ex - 1; x++)
            for (int y = (int) ey - 3; y <= (int) ey - 2; y++)
                for (int z = fz - 1; z <= fz + 2; z++)
                    if (c.solid(x, y, z) && !c.solid(x, y, z + 1)) c.set(x, y, z, W);
    }

    static void egg(Canvas c, Egg e) {
        double r = e.r(), ry = r * STRETCH;
        int cutY = (int) Math.floor(e.y() + ry * 0.45);             // open eggs: top removed above here
        for (int x = (int) Math.floor(e.x() - r * 1.3); x <= Math.ceil(e.x() + r * 1.3); x++)
            for (int y = (int) Math.floor(e.y() - ry); y <= Math.ceil(e.y() + ry); y++)
                for (int z = (int) Math.floor(e.z() - r * 1.3); z <= Math.ceil(e.z() + r * 1.3); z++) {
                    double hy = (y + 0.5 - e.y()) / ry;
                    if (hy < -1 || hy > 1) continue;
                    // egg profile: radius at this height, narrower at the top (hy > 0), wider below
                    double prof = Math.sqrt(1 - hy * hy) * (1 - TAPER * hy);
                    double dx = x + 0.5 - e.x(), dz = z + 0.5 - e.z();
                    double rad = Math.sqrt(dx * dx + dz * dz) / r;
                    if (rad > prof) continue;
                    boolean shell = rad > prof - 1.6 / r || hy > 1 - 1.4 / ry || hy < -1 + 1.4 / ry;
                    if (e.open() && y > cutY) continue;
                    if (e.open() && y > cutY - 2 && rad < prof - 1.1 / r) continue;   // hollow cup above the chocolate
                    String id;
                    if (e.open() && !shell) id = BROWN;
                    else if (!shell) id = e.base();
                    else if (e.cap() && hy > 0.55) id = CAP;
                    else id = pattern(e, x, y, z) ? e.patId() : e.base();
                    c.set(x, y, z, id);
                }
    }

    /** Surface pattern in egg coordinates: u = around (blocks), v = up (blocks). */
    private static boolean pattern(Egg e, int x, int y, int z) {
        double ang = Math.atan2(z + 0.5 - e.z(), x + 0.5 - e.x());
        int u = (int) Math.floor((ang + Math.PI) / (2 * Math.PI) * Math.round(2 * Math.PI * e.r()));
        int v = y - (int) Math.floor(e.y());
        return switch (e.pat()) {
            case SOLID -> false;
            case DIAMONDS -> {
                int row = Math.floorDiv(v, 4);
                int uu = Math.floorMod(u + (row & 1) * 3, 6) - 3, vv = Math.floorMod(v, 4) - 2;
                yield Math.abs(uu) + Math.abs(vv) <= 1;
            }
            case SPOTS -> {
                int row = Math.floorDiv(v, 5);
                yield Math.floorMod(u + (row & 1) * 3, 6) < 2 && Math.floorMod(v, 5) < 2;
            }
            case DOTS -> {
                int row = Math.floorDiv(v, 3);
                yield Math.floorMod(u + (row & 1) * 2, 4) == 0 && Math.floorMod(v, 3) == 0;
            }
            case STRIPES -> Math.floorMod(v, 4) < 2;
            case SQUARES -> {
                int row = Math.floorDiv(v, 4);
                yield Math.floorMod(u + (row & 1) * 2, 5) < 2 && Math.floorMod(v, 4) < 2;
            }
        };
    }
}
