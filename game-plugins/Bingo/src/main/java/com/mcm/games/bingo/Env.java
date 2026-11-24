package com.mcm.games.bingo;

import org.bukkit.block.Biome;

import java.util.*;

/**
 * What one game area provides: the biomes that really exist inside its border (from the plugin's own layout) and the
 * structures that are there for sure. Items on the card are drawn only from what this set satisfies.
 * Tags: {@code b:<biome key>} for a biome, {@code s:<structure>} for a structure.
 */
public final class Env {
    public final Set<String> tags;

    public Env(Collection<Biome> biomes, Collection<String> structures) {
        Set<String> t = new HashSet<>();
        for (Biome b : biomes) t.add(biomeTag(b));
        for (String s : structures) t.add("s:" + s);
        this.tags = Collections.unmodifiableSet(t);
    }

    public static String biomeTag(Biome b) { return "b:" + b.name().toLowerCase(Locale.ROOT); }

    /** True if at least one tag of {@code anyOf} is present. */
    public boolean has(Set<String> anyOf) {
        for (String s : anyOf) if (tags.contains(s)) return true;
        return false;
    }

    public boolean satisfies(BingoItem item) {
        for (Set<String> need : item.needs) if (!has(need)) return false;
        return true;
    }

    /** Structures this plugin guarantees in every area without placing them (vanilla, per-chunk chance). */
    public static final List<String> ALWAYS_STRUCTURES = List.of("mineshaft");

    @Override public String toString() { return new TreeSet<>(tags).toString(); }
}
