package com.mcm.games.gridrunners;

import java.lang.reflect.Method;

/**
 * Reads the core's admin fast-test flag (McmApi.isFastTest(), core 3.2.x) by reflection so this jar still
 * loads on an older core that lacks it (then it is always false).
 */
final class FastTest {
    private static Method method;
    private static boolean looked;

    private FastTest() {}

    static boolean on() {
        if (!looked) {
            looked = true;
            try { method = Class.forName("com.mcm.api.McmApi").getMethod("isFastTest"); } catch (Throwable ignored) { method = null; }
        }
        if (method == null) return false;
        try { return Boolean.TRUE.equals(method.invoke(null)); } catch (Throwable t) { return false; }
    }

    /** {@code seconds}, or at most 1 while fast-test mode is on. */
    static int secs(int seconds) { return on() ? Math.min(seconds, 1) : seconds; }

    /** {@code ticks}, or at most 20 while fast-test mode is on. */
    static long ticks(long ticks) { return on() ? Math.min(ticks, 20L) : ticks; }
}
