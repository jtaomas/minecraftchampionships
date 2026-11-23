package com.mcm.api;

/**
 * Fast-test aware waits. Every non-gameplay delay (countdowns, intermissions, reveals, celebrations,
 * return-to-lobby delays) should be passed through here so that the admin's fast-test mode
 * ({@link McmApi#isFastTest()}) cuts it to at most 1 second.
 * @since 3.2.0
 */
public final class Waits {
    private Waits() {}

    /** {@code normalTicks}, or at most 20 ticks (1 s) while fast-test mode is on. Never negative. */
    public static long ticks(long normalTicks) {
        long t = Math.max(0L, normalTicks);
        return McmApi.isFastTest() ? Math.min(t, 20L) : t;
    }

    /** Integer overload of {@link #ticks(long)}. */
    public static int ticks(int normalTicks) { return (int) ticks((long) normalTicks); }

    /** {@code normalSeconds}, or at most 1 while fast-test mode is on. Never negative. */
    public static int seconds(int normalSeconds) {
        int s = Math.max(0, normalSeconds);
        return McmApi.isFastTest() ? Math.min(s, 1) : s;
    }

    /** True while fast-test mode is on (same as {@link McmApi#isFastTest()}). */
    public static boolean fast() { return McmApi.isFastTest(); }
}
