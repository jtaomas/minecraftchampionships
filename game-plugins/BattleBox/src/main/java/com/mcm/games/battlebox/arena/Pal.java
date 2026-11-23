package com.mcm.games.battlebox.arena;

import java.util.ArrayList;
import java.util.List;

/** Weighted block palette, picked deterministically by position. */
public final class Pal {
    private final List<String> blocks = new ArrayList<>();
    private final List<Double> cum = new ArrayList<>();
    private double total;

    /** Alternating (block, weight) pairs: {@code Pal.of("stone", 5, "andesite", 2)}. */
    public static Pal of(Object... pairs) {
        Pal p = new Pal();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            p.total += ((Number) pairs[i + 1]).doubleValue();
            p.blocks.add((String) pairs[i]);
            p.cum.add(p.total);
        }
        return p;
    }

    public String at(int x, int y, int z, long seed) {
        double r = Noise.hash01(x, y, z, seed) * total;
        for (int i = 0; i < cum.size(); i++) if (r < cum.get(i)) return blocks.get(i);
        return blocks.get(blocks.size() - 1);
    }
}
