package com.mcm.api;

import java.util.List;
import java.util.Locale;

/**
 * Pure helpers for MCC coin scoring (no Bukkit calls, so they can be unit-tested without a server).
 * Games award <b>base</b> MCC coins (the x1.0 values from the wiki); the core multiplies them by the event
 * multiplier of the game slot. See {@code game-plugins/COINS.md}.
 * @since 3.3.0
 */
public final class McmCoins {
    private McmCoins() {}

    /** MCC regular 8-game event: game 1 x1.0, 2-3 x1.5, 4-5 x2.0, 6-7 x2.5, 8 x3.0. */
    public static final double[] MCC_EIGHT = {1.0, 1.5, 1.5, 2.0, 2.0, 2.5, 2.5, 3.0};
    /** MCC 6-game event: game 1 x1.0, 2 x1.5, 3-4 x2.0, 5 x2.5, 6 x3.0. */
    public static final double[] MCC_SIX = {1.0, 1.5, 2.0, 2.0, 2.5, 3.0};

    /**
     * Event multiplier for the coin game at {@code index} (0-based) of {@code games} coin games.
     * 8 games use {@code eight} as is and 6 games use {@code six}; any other length is mapped proportionally
     * onto {@code eight}, so the first game is always the first value and the last game the last value.
     */
    public static double multiplier(int index, int games, double[] eight, double[] six) {
        if (games <= 0 || index < 0) return 1.0;
        if (eight == null || eight.length == 0) eight = MCC_EIGHT;
        if (six != null && six.length > 0 && games == six.length) return six[Math.min(index, six.length - 1)];
        if (games == eight.length) return eight[Math.min(index, eight.length - 1)];
        if (games == 1) return eight[0];
        int i = (int) Math.round(Math.min(index, games - 1) * (eight.length - 1) / (double) (games - 1));
        return eight[Math.max(0, Math.min(eight.length - 1, i))];
    }

    /** Converts a config list to an array, or returns {@code def} when the list is missing or empty. */
    public static double[] toArray(List<?> l, double[] def) {
        if (l == null || l.isEmpty()) return def;
        double[] a = new double[l.size()];
        for (int i = 0; i < a.length; i++) {
            Object o = l.get(i);
            a[i] = o instanceof Number n ? n.doubleValue() : Double.parseDouble(String.valueOf(o));
        }
        return a;
    }

    /** Converts a config list of numbers to an int array, or returns {@code def} when missing or empty. */
    public static int[] toIntArray(List<?> l, int[] def) {
        if (l == null || l.isEmpty()) return def;
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; i++) {
            Object o = l.get(i);
            a[i] = o instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(o).trim());
        }
        return a;
    }

    /** Placement table lookup, 1-based: {@code table[place-1]}, 0 past the end of the table. */
    public static int placement(int[] table, int place) {
        if (table == null || place < 1 || place > table.length) return 0;
        return table[place - 1];
    }

    /** Placement table lookup that repeats the last value past the end of the table (0 for an empty table). */
    public static int placementOrLast(int[] table, int place) {
        if (table == null || table.length == 0 || place < 1) return 0;
        return table[Math.min(place, table.length) - 1];
    }

    /** {@code first - step * (place - 1)}, never below {@code min}. place is 1-based. */
    public static double linear(double first, double step, int place, double min) {
        return Math.max(min, first - step * (Math.max(1, place) - 1));
    }

    /**
     * MCC tie rule ("split of the sum of coins amongst the tied placements"): the players who share placements
     * {@code place .. place+tied-1} (1-based) each get the average of those table values.
     */
    public static double tiedShare(int[] table, int place, int tied) {
        if (tied <= 0) return 0;
        double sum = 0;
        for (int k = place; k < place + tied; k++) sum += placement(table, k);
        return sum / tied;
    }

    /** Whole coins to credit now and the fraction to carry: returns floor(carry + coins) (with a tiny epsilon). */
    public static int whole(double exact) {
        return (int) Math.floor(exact + 1e-9);
    }

    /** "40", "22.5", "-25": whole numbers without a decimal, otherwise one decimal (as the MCC chat feed). */
    public static String format(double coins) {
        double r = Math.round(coins * 10.0) / 10.0;
        if (Math.abs(r - Math.rint(r)) < 1e-9) return String.valueOf((long) Math.rint(r));
        return String.format(Locale.ROOT, "%.1f", r);
    }

    /** "x1.0", "x2.5". */
    public static String formatMultiplier(double m) {
        return "x" + String.format(Locale.ROOT, "%.1f", m);
    }
}
