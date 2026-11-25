package com.mcm.games.rocketspleef.map;

/**
 * Nanotube (MCC 3). Reference: research/mcc_archive/rocket_spleef_old/frames/wiki/Nanotube.png, a pentagonal
 * tube seen end-on from above. Each level is an outer pentagon of blue spheres (big at the corners, small at the
 * edge midpoints) joined by dark-grey stepped links, and an inner pentagon (turned 36 degrees) of lime spheres joined
 * by white links, with white spokes out to the outer ring. Levels repeat down the tube and are joined by vertical
 * zig-zag links (dark outside, white inside).
 */
public final class NanotubeMap implements MapGen {
    // palette (swappable): sampled from the reference image, see notes.md section 4
    static final String OUTER = "minecraft:light_blue_concrete";
    static final String INNER = "minecraft:lime_concrete";
    static final String DARK_LINK = "minecraft:gray_concrete";
    static final String LIGHT_LINK = "minecraft:white_concrete";

    static final double R_OUT = 22.0;     // outer pentagon circumradius
    static final double R_IN = 12.0;      // inner pentagon circumradius
    static final double S_OUT_V = 3.6, S_OUT_M = 2.5, S_IN_V = 3.0, S_IN_M = 2.0;
    static final int LEVELS = 5;
    static final int GAP = 10;            // vertical distance between levels

    @Override public String id() { return "nanotube"; }
    @Override public String displayName() { return "Nanotube"; }
    @Override public String reference() { return "game-plugins/research/mcc_archive/rocket_spleef_old/frames/wiki/Nanotube.png"; }

    private static double[] pt(double r, double angDeg, double y) {
        double a = Math.toRadians(angDeg);
        return new double[]{Math.cos(a) * r, y, Math.sin(a) * r};
    }

    @Override
    public Canvas generate() {
        Canvas c = new Canvas();
        int top = (LEVELS - 1) * GAP + 4;
        for (int l = 0; l < LEVELS; l++) {
            double y = top - l * GAP;
            double[][] ov = new double[5][], om = new double[5][], iv = new double[5][], im = new double[5][];
            for (int i = 0; i < 5; i++) {
                double a = -90 + i * 72;                       // outer corner points "up" (north) as in the image
                ov[i] = pt(R_OUT, a, y);
                om[i] = pt(R_OUT * Math.cos(Math.toRadians(36)), a + 36, y);
                iv[i] = pt(R_IN, a + 36, y);                   // inner pentagon turned 36 degrees
                im[i] = pt(R_IN * Math.cos(Math.toRadians(36)), a + 72, y);
            }
            for (int i = 0; i < 5; i++) {
                int j = (i + 1) % 5;
                // links first, spheres on top
                link(c, ov[i], om[i], DARK_LINK);
                link(c, om[i], ov[j], DARK_LINK);
                link(c, iv[i], im[i], LIGHT_LINK);
                link(c, im[i], iv[j], LIGHT_LINK);
                link(c, iv[i], om[i], LIGHT_LINK);            // spoke: inner corner -> outer edge midpoint
                if (l < LEVELS - 1) {
                    // vertical zig-zag to the level below: outer corner to the corner below, offset half a step
                    double yb = y - GAP;
                    link(c, ov[i], new double[]{om[i][0], yb, om[i][2]}, DARK_LINK);
                    link(c, iv[i], new double[]{im[i][0], yb, im[i][2]}, LIGHT_LINK);
                }
            }
            for (int i = 0; i < 5; i++) {
                c.sphere(ov[i][0], ov[i][1], ov[i][2], S_OUT_V, OUTER);
                c.sphere(om[i][0], om[i][1], om[i][2], S_OUT_M, OUTER);
                c.sphere(iv[i][0], iv[i][1], iv[i][2], S_IN_V, INNER);
                c.sphere(im[i][0], im[i][1], im[i][2], S_IN_M, INNER);
            }
        }
        return c;
    }

    private static void link(Canvas c, double[] a, double[] b, String id) {
        c.stepLine(a[0], a[1], a[2], b[0], b[1], b[2], id);
    }
}
