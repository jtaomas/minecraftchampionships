package com.mcm.games.parkourwarrior.map;

/** Shape helpers in an obstacle's local frame. */
final class Sh {
    private Sh() {}

    /** Filled disc (y fixed) radius r (euclidean, r + 0.5 test). */
    static void disc(Ob o, int cx, int y, int cz, double r, String s) {
        int R = (int) Math.ceil(r);
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++)
            if (x * x + z * z <= (r + 0.5) * (r + 0.5) - 0.5) o.set(cx + x, y, cz + z, s);
    }

    static boolean inDisc(int x, int z, double r) { return x * x + z * z <= (r + 0.5) * (r + 0.5) - 0.5; }

    /** Ring between r0 (exclusive) and r1 (inclusive). */
    static void ring(Ob o, int cx, int y, int cz, double r0, double r1, String s) {
        int R = (int) Math.ceil(r1);
        for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++)
            if (inDisc(x, z, r1) && !inDisc(x, z, r0)) o.set(cx + x, y, cz + z, s);
    }

    /** Vertical chain from y0 to y1 inclusive. */
    static void chain(Ob o, int x, int y0, int y1, int z) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) o.set(x, y, z, "chain[axis=y,waterlogged=false]");
    }

    /** Hanging lantern with chain from y (lantern) up to yTop. */
    static void hangLantern(Ob o, int x, int y, int yTop, int z, boolean soul) {
        chain(o, x, y + 1, yTop, z);
        o.set(x, y, z, (soul ? "soul_lantern" : "lantern") + "[hanging=true,waterlogged=false]");
    }

    /**
     * Rough rock / cloud underside below a footprint: tapers downward with noise. cells = local x,z pairs at
     * level y (the underside starts at y - 1).
     */
    static void taper(Ob o, int cx, int y, int cz, double r, int depth, String[] pal, int salt) {
        for (int d = 1; d <= depth; d++) {
            double rr = r * (1.0 - (double) d / (depth + 1)) + 0.3;
            int R = (int) Math.ceil(rr) + 1;
            for (int x = -R; x <= R; x++) for (int z = -R; z <= R; z++) {
                double n = W.h(o.wx(cx + x, cz + z), d, o.wz(cx + x, cz + z), salt);
                if (x * x + z * z <= (rr + n * 0.9 - 0.3) * (rr + n * 0.9 - 0.3)) {
                    String s = pal[Math.min(pal.length - 1, (int) ((d / (double) depth) * pal.length * 0.8 + n * 1.2))];
                    o.soft(cx + x, y - d, cz + z, s);
                }
            }
        }
    }

    static String stair(String mat, String facing, boolean top) { return Bs.stairs(mat, facing, top); }

    /**
     * A themed base around and below an end pad at local (x, y, z): a 5x5 rim (registered safe) and a body that
     * tapers below. rim may be null (pad only on the body).
     */
    static void padBase(Ob o, int x, int y, int z, String rim, String body, String under, int depth) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
            if (edge && rim != null && !(Math.abs(dx) == 2 && Math.abs(dz) == 2)) { o.set(x + dx, y, z + dz, rim); o.safe(x + dx, y, z + dz, x + dx, z + dz); }
            if (!edge || (rim != null && !(Math.abs(dx) == 2 && Math.abs(dz) == 2))) o.set(x + dx, y - 1, z + dz, body);
        }
        for (int d = 2; d <= depth + 1; d++) {
            int rr = Math.max(0, 2 - (d - 1) * 2 / Math.max(1, depth));
            for (int dx = -rr; dx <= rr; dx++) for (int dz = -rr; dz <= rr; dz++) o.soft(x + dx, y - d, z + dz, under);
        }
    }

    static final String[] TOY = {"red", "yellow", "blue", "lime", "orange", "light_blue", "magenta", "white"};

    /** Stud (button) on top of a toy brick cell. */
    static String stud(String colour) {
        switch (colour) {
            case "red": return "mangrove_button[face=floor,facing=north,powered=false]";
            case "yellow": return "bamboo_button[face=floor,facing=north,powered=false]";
            case "blue": case "light_blue": return "warped_button[face=floor,facing=north,powered=false]";
            case "magenta": return "cherry_button[face=floor,facing=north,powered=false]";
            case "orange": return "acacia_button[face=floor,facing=north,powered=false]";
            case "lime": return "jungle_button[face=floor,facing=north,powered=false]";
            default: return "birch_button[face=floor,facing=north,powered=false]";
        }
    }
}
