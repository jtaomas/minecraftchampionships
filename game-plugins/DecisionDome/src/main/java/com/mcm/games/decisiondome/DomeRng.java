package com.mcm.games.decisiondome;

/**
 * Small seeded RNG (SplitMix64) used for movement, hatching and the tie roll. Deterministic for a
 * given seed and call order, with no Bukkit or {@link java.util.Random} dependency.
 */
public final class DomeRng {
    private long state;
    private long calls;

    public DomeRng(long seed) {
        this.state = seed == 0 ? 0x9E3779B97F4A7C15L : seed;
    }

    public long nextLong() {
        calls++;
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public int nextInt(int bound) {
        if (bound <= 0) return 0;
        long r = nextLong() & 0x7FFFFFFFFFFFFFFFL;
        return (int) (r % bound);
    }

    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    public boolean nextBoolean() { return (nextLong() & 1L) != 0L; }

    /** Stable independent stream for a named decision (tie roll, one subsystem, etc.). */
    public DomeRng fork(String label) {
        long h = 0xCBF29CE484222325L;
        if (label != null) {
            for (int i = 0; i < label.length(); i++) {
                h ^= label.charAt(i);
                h *= 0x100000001B3L;
            }
        }
        return new DomeRng(h ^ nextLong() ^ state);
    }

    public long calls() { return calls; }

    public long state() { return state; }

    public static long mix(long seed, long a, long b) {
        long z = seed + 0x9E3779B97F4A7C15L * (a + 1);
        z ^= b + 0x9E3779B97F4A7C15L + (z << 6) + (z >>> 2);
        z *= 0xBF58476D1CE4E5B9L;
        z ^= z >>> 27;
        z *= 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
