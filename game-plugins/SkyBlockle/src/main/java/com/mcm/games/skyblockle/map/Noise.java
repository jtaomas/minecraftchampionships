package com.mcm.games.skyblockle.map;

/** Deterministic hash + value noise and a weighted palette (pure Java). */
final class Noise {
    private Noise() {}

    static double hash01(int x, int y, int z, long seed) {
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= x * 0x632BE59BD9B4E019L;
        h ^= y * 0x85157AF5L;
        h ^= z * 0xC2B2AE3D27D4EB4FL;
        h ^= (h >>> 31);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 27);
        h *= 0x94D049BB133111EBL;
        h ^= (h >>> 31);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static double smooth(double t) { return t * t * (3 - 2 * t); }

    static double value2(double x, double y, long seed) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double fx = smooth(x - x0), fy = smooth(y - y0);
        double a = hash01(x0, y0, 0, seed), b = hash01(x0 + 1, y0, 0, seed);
        double c = hash01(x0, y0 + 1, 0, seed), d = hash01(x0 + 1, y0 + 1, 0, seed);
        double ab = a + (b - a) * fx, cd = c + (d - c) * fx;
        return ab + (cd - ab) * fy;
    }

    static double fbm2(double x, double y, long seed) {
        return value2(x, y, seed) * 0.65 + value2(x * 2.1, y * 2.1, seed + 17) * 0.35;
    }

    /** Weighted block palette picked by position hash. */
    static final class Pal {
        private final String[] blocks;
        private final double[] cum;

        Pal(Object... pairs) {
            blocks = new String[pairs.length / 2];
            cum = new double[pairs.length / 2];
            double t = 0;
            for (int i = 0; i < blocks.length; i++) {
                blocks[i] = (String) pairs[2 * i];
                t += ((Number) pairs[2 * i + 1]).doubleValue();
                cum[i] = t;
            }
        }

        String at(int x, int y, int z, long seed) {
            double r = hash01(x, y, z, seed) * cum[cum.length - 1];
            for (int i = 0; i < cum.length; i++) if (r < cum[i]) return blocks[i];
            return blocks[blocks.length - 1];
        }
    }
}
