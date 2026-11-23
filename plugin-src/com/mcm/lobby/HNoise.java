package com.mcm.lobby;

import java.util.Random;

/** Pure-Java seeded simplex noise (same algorithm as McmBuild.Noise) so the hub generator runs offline. */
final class HNoise {
    private static final int[][] GRAD3 = {{1,1,0},{-1,1,0},{1,-1,0},{-1,-1,0},{1,0,1},{-1,0,1},{1,0,-1},{-1,0,-1},
            {0,1,1},{0,-1,1},{0,1,-1},{0,-1,-1}};
    private static final double F2 = 0.5 * (Math.sqrt(3.0) - 1.0);
    private static final double G2 = (3.0 - Math.sqrt(3.0)) / 6.0;
    private final short[] perm = new short[512];
    private final short[] permMod12 = new short[512];

    HNoise(long seed) {
        short[] p = new short[256];
        for (short i = 0; i < 256; i++) p[i] = i;
        Random r = new Random(seed);
        for (int i = 255; i > 0; i--) { int j = r.nextInt(i + 1); short t = p[i]; p[i] = p[j]; p[j] = t; }
        for (int i = 0; i < 512; i++) { perm[i] = p[i & 255]; permMod12[i] = (short) (perm[i] % 12); }
    }

    private static int fastFloor(double x) { int xi = (int) x; return x < xi ? xi - 1 : xi; }

    double noise2(double xin, double yin) {
        double n0, n1, n2;
        double s = (xin + yin) * F2;
        int i = fastFloor(xin + s), j = fastFloor(yin + s);
        double t = (i + j) * G2;
        double x0 = xin - (i - t), y0 = yin - (j - t);
        int i1, j1;
        if (x0 > y0) { i1 = 1; j1 = 0; } else { i1 = 0; j1 = 1; }
        double x1 = x0 - i1 + G2, y1 = y0 - j1 + G2;
        double x2 = x0 - 1.0 + 2.0 * G2, y2 = y0 - 1.0 + 2.0 * G2;
        int ii = i & 255, jj = j & 255;
        int gi0 = permMod12[ii + perm[jj]];
        int gi1 = permMod12[ii + i1 + perm[jj + j1]];
        int gi2 = permMod12[ii + 1 + perm[jj + 1]];
        double t0 = 0.5 - x0 * x0 - y0 * y0;
        if (t0 < 0) n0 = 0; else { t0 *= t0; n0 = t0 * t0 * (GRAD3[gi0][0] * x0 + GRAD3[gi0][1] * y0); }
        double t1 = 0.5 - x1 * x1 - y1 * y1;
        if (t1 < 0) n1 = 0; else { t1 *= t1; n1 = t1 * t1 * (GRAD3[gi1][0] * x1 + GRAD3[gi1][1] * y1); }
        double t2 = 0.5 - x2 * x2 - y2 * y2;
        if (t2 < 0) n2 = 0; else { t2 *= t2; n2 = t2 * t2 * (GRAD3[gi2][0] * x2 + GRAD3[gi2][1] * y2); }
        return 70.0 * (n0 + n1 + n2);
    }

    double fbm(double x, double y, int octaves) {
        double sum = 0, amp = 1, freq = 1, norm = 0;
        for (int o = 0; o < octaves; o++) {
            sum += amp * noise2(x * freq + o * 31.7, y * freq - o * 17.3);
            norm += amp; amp *= 0.5; freq *= 2.0;
        }
        return sum / norm;
    }

    /** Ridged fractal noise in [0, 1] (sharp crests). */
    double ridged(double x, double y, int octaves) {
        double sum = 0, amp = 1, freq = 1, norm = 0;
        for (int o = 0; o < octaves; o++) {
            double n = 1.0 - Math.abs(noise2(x * freq + o * 13.1, y * freq + o * 7.7));
            sum += amp * n * n;
            norm += amp; amp *= 0.5; freq *= 2.1;
        }
        return sum / norm;
    }

    static double hash01(int x, int y, int z, long seed) {
        long h = seed * 0x9E3779B97F4A7C15L;
        h ^= x * 0xC2B2AE3D27D4EB4FL; h = mix(h);
        h ^= y * 0x165667B19E3779F9L; h = mix(h);
        h ^= z * 0x27D4EB2F165667C5L; h = mix(h);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static long mix(long h) {
        h ^= h >>> 33; h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33; h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return h;
    }
}
