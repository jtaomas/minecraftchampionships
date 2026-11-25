package com.mcm.games.rocketspleef.map;

import java.util.Random;

/**
 * Rings (MCC 1). Reference: research/mcc_archive/rocket_spleef_old/frames/wiki/Rings.png (seen from below).
 * A tall dark-grey pillar with notches and ledges; around it THIN (2-3 block), separate, jagged rings with big open air
 * gaps between them (you see sky between the rings). The rings are broken arcs and C-shapes with chunky edges: wide
 * sweeping bands low down (blue, teal), narrow bands hugging the pillar (the two greens), a gold band with chunky arms,
 * big flared orange petals, and a small red ring on top.
 */
public final class RingsMap implements MapGen {
    static final String PILLAR = "minecraft:gray_concrete";
    static final double PILLAR_R = 3.2;
    static final int PILLAR_BOTTOM = -6, PILLAR_TOP = 66;

    enum Kind { BAND, SWEEP, ARMS, PETALS }

    /**
     * One ring: colour, bottom y, thickness, inner/outer radius, kind, arcs as {startDeg, endDeg} pairs, seed.
     * SWEEP grows the outer radius along the arc (a C that sweeps outward); ARMS adds chunky arms; PETALS flares.
     */
    record Ring(String id, int y, int thick, double rIn, double rOut, Kind kind, double[] arcs, int seed) {}

    static final Ring[] RINGS = {
            new Ring("minecraft:blue_concrete",    0, 3, 8, 17, Kind.SWEEP,  new double[]{-20, 230, 255, 320}, 1),
            new Ring("minecraft:cyan_concrete",   10, 3, 6, 15, Kind.SWEEP,  new double[]{60, 340}, 2),
            new Ring("minecraft:green_concrete",  20, 2, 3.5, 9, Kind.BAND,  new double[]{0, 100, 130, 230, 260, 340}, 3),
            new Ring("minecraft:green_wool",      28, 2, 3.5, 10, Kind.BAND, new double[]{-40, 150, 175, 290}, 4),
            new Ring("minecraft:yellow_concrete", 37, 2, 3.5, 8, Kind.ARMS,  new double[]{0, 360}, 5),
            new Ring("minecraft:orange_concrete", 46, 3, 4, 8, Kind.PETALS,  new double[]{0, 360}, 6),
            new Ring("minecraft:red_concrete",    57, 2, 3.5, 10, Kind.BAND, new double[]{20, 160, 200, 330}, 7),
    };

    @Override public String id() { return "rings"; }
    @Override public String displayName() { return "Rings"; }
    @Override public String reference() { return "game-plugins/research/mcc_archive/rocket_spleef_old/frames/wiki/Rings.png"; }

    @Override
    public Canvas generate() {
        Canvas c = new Canvas();
        pillar(c);
        for (Ring r : RINGS) ring(c, r);
        return c;
    }

    /** Pillar with notches (1-block recesses) and small ledges, as in the image. */
    private static void pillar(Canvas c) {
        c.cylinder(0.5, PILLAR_BOTTOM, PILLAR_TOP, 0.5, PILLAR_R, PILLAR);
        Random rnd = new Random(77);
        for (int i = 0; i < 26; i++) {
            int y = PILLAR_BOTTOM + 2 + rnd.nextInt(PILLAR_TOP - PILLAR_BOTTOM - 4);
            double a = rnd.nextDouble() * Math.PI * 2;
            int x = (int) Math.round(Math.cos(a) * 3.3), z = (int) Math.round(Math.sin(a) * 3.3);
            int h = 2 + rnd.nextInt(4);
            boolean notch = rnd.nextBoolean();
            for (int dy = 0; dy < h; dy++) {
                if (notch) c.set(x, y + dy, z, null);                 // recess
                else {                                                // ledge sticking out one block
                    int lx = (int) Math.round(Math.cos(a) * 4.3), lz = (int) Math.round(Math.sin(a) * 4.3);
                    if (dy < 2) c.set(lx, y + dy, lz, PILLAR);
                }
            }
        }
    }

    /** Chunky 2x2-cell noise in [-1, 1], so edges step in blocks of two like the reference. */
    private static double cell(int x, int z, int seed) {
        int cx = Math.floorDiv(x, 2), cz = Math.floorDiv(z, 2);
        long h = cx * 73856093L ^ cz * 19349663L ^ seed * 83492791L;
        h = (h ^ (h >>> 13)) * 0x5bd1e995L;
        return ((h >>> 8) & 0xFFFF) / 32767.5 - 1.0;
    }

    private static void ring(Canvas c, Ring r) {
        int ext = (int) Math.ceil(r.rOut() * 2.4) + 2;
        for (int x = -ext; x <= ext; x++)
            for (int z = -ext; z <= ext; z++) {
                double d = Math.hypot(x, z);
                double deg = Math.toDegrees(Math.atan2(z, x));
                double t = arcT(r.arcs(), deg);           // position 0..1 inside its arc, or -1 if outside
                if (t < 0) continue;
                double n = cell(x, z, r.seed());
                double inner = r.rIn() + 0.6 * n;
                double outer;
                int lift = 0;                              // petal tips rise a block or two
                switch (r.kind()) {
                    case SWEEP -> outer = r.rOut() * (0.75 + 0.45 * t) + 1.4 * n;
                    case ARMS -> {
                        double arm = Math.pow(Math.max(0, Math.cos(Math.toRadians(deg) * 4 + r.seed())), 10);
                        outer = r.rOut() + 1.2 * n + arm * 8.5;
                        // arms are narrow and stepped: break them into chunks
                        if (arm > 0.3 && d > r.rOut() + 1 && cell(x + 7, z + 3, r.seed()) < -0.55) continue;
                    }
                    case PETALS -> {
                        double petal = Math.pow(Math.max(0, Math.sin(Math.toRadians(deg) * 3 + 0.6)), 3);
                        outer = r.rOut() + 1.2 * n + petal * 13.0;
                        // hollow notch inside each petal so they read as separate flared petals
                        if (petal > 0.55 && d > r.rOut() + 4 && d < r.rOut() + 4 + petal * 4 && cell(x, z, r.seed() + 9) > 0.2) continue;
                        lift = d > r.rOut() + 7 ? 1 : 0;
                        if (d > r.rOut() + 11) lift = 2;
                    }
                    default -> outer = r.rOut() + 1.2 * n;
                }
                if (d < inner || d > outer) continue;
                // jagged top/bottom: chunks one block up or down
                int top = r.y() + r.thick() + lift + (cell(x + 11, z - 5, r.seed()) > 0.6 ? 1 : 0);
                int bottom = r.y() + lift + (cell(x - 9, z + 13, r.seed()) > 0.55 ? 1 : 0);
                if (d > outer - 1.0) bottom = Math.max(bottom, top - 2);          // thinner rim
                for (int y = bottom; y < top; y++) c.set(x, y, z, r.id());
            }
    }

    /** Where {@code deg} lies inside one of the arcs (0..1), or -1. Arcs are {start, end} pairs in degrees. */
    private static double arcT(double[] arcs, double deg) {
        for (int i = 0; i + 1 < arcs.length; i += 2) {
            double s = arcs[i], e = arcs[i + 1], span = e - s;
            double rel = ((deg - s) % 360 + 360) % 360;
            if (rel <= span) return rel / span;
        }
        return -1;
    }
}
