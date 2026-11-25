package com.mcm.games.race.model;

/**
 * The seed a map is built with in game when {@code random-seeds: false} (the default): MapRegistry.Entry.fixedSeed()
 * uses the same formula. The offline validators default to it so they check the layout that is actually pasted
 * (feedback 3: the Dam Face holes passed the validator because it ran on a different seed than the server).
 */
public final class Seeds {
    private Seeds() {}

    public static long fixed(String mapId) { return mapId.hashCode() * 2654435761L; }
}
