package com.mcm.games.parkourwarrior;

import com.mcm.games.parkourwarrior.CourseModel.Difficulty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure scoring maths (spec v2): stone medals flat, metal medals by count held (cumulative table), team multiplier
 * from ending finishers, and the multiplier extra split across the side's members by ending weight.
 */
final class Scoring {
    private Scoring() {}

    enum Medal { STONE, BRONZE, SILVER, GOLD }

    /** Total points of holding {@code n} medals of a metal type (table[n-1]; beyond the table: last + last step). */
    static int metalTotal(int[] table, int n) {
        if (n <= 0 || table.length == 0) return 0;
        if (n <= table.length) return table[n - 1];
        int last = table[table.length - 1];
        int step = table.length >= 2 ? last - table[table.length - 2] : last;
        return last + step * (n - table.length);
    }

    static int medalPoints(Settings s, int stone, int bronze, int silver, int gold) {
        return stone * s.stone + metalTotal(s.bronze, bronze) + metalTotal(s.silver, silver) + metalTotal(s.gold, gold);
    }

    /** Points gained by the medal that raised the count of {@code type} from {@code newCount - 1} to {@code newCount}. */
    static int delta(Settings s, Medal type, int newCount) {
        return switch (type) {
            case STONE -> s.stone;
            case BRONZE -> metalTotal(s.bronze, newCount) - metalTotal(s.bronze, newCount - 1);
            case SILVER -> metalTotal(s.silver, newCount) - metalTotal(s.silver, newCount - 1);
            case GOLD -> metalTotal(s.gold, newCount) - metalTotal(s.gold, newCount - 1);
        };
    }

    static double multiplier(Settings s, List<Difficulty> finishedEndings) {
        double m = 1.0;
        for (Difficulty d : finishedEndings) if (d != null) m += s.multInc.getOrDefault(d, 0.0);
        return m;
    }

    record Member(UUID id, int medalPoints, Difficulty ending) {}

    /**
     * Multiplier extra per member: extra = sum(medal points) * (mult - 1), shared by weight
     * (none / easy / medium / hard). Rounded per member (largest remainder, so the shares add up to round(extra)).
     */
    static Map<UUID, Integer> shares(Settings s, List<Member> members, double mult) {
        Map<UUID, Integer> out = new HashMap<>();
        if (members.isEmpty()) return out;
        int sum = 0;
        for (Member m : members) sum += m.medalPoints();
        double extra = sum * Math.max(0, mult - 1.0);
        int target = (int) Math.round(extra);
        if (target <= 0) { for (Member m : members) out.put(m.id(), 0); return out; }
        double wsum = 0;
        double[] w = new double[members.size()];
        for (int i = 0; i < w.length; i++) {
            Difficulty d = members.get(i).ending();
            w[i] = d == null ? s.weightNone : s.weight.getOrDefault(d, s.weightNone);
            wsum += w[i];
        }
        if (wsum <= 0) { for (int i = 0; i < w.length; i++) w[i] = 1; wsum = w.length; }
        int given = 0;
        double[] rem = new double[w.length];
        int[] base = new int[w.length];
        for (int i = 0; i < w.length; i++) {
            double exact = target * w[i] / wsum;
            base[i] = (int) Math.floor(exact);
            rem[i] = exact - base[i];
            given += base[i];
        }
        while (given < target) {
            int best = 0;
            for (int i = 1; i < rem.length; i++) if (rem[i] > rem[best]) best = i;
            base[best]++; rem[best] = -1; given++;
        }
        for (int i = 0; i < w.length; i++) out.put(members.get(i).id(), base[i]);
        return out;
    }
}
