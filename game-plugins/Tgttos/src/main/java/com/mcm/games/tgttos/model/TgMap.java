package com.mcm.games.tgttos.model;

/**
 * One TGTTOS map generator (pure Java, no Bukkit classes). Implementations need a public no-arg constructor;
 * the runtime instantiates them by fully-qualified class name from {@code config.yml -> maps:}.
 *
 * <p>Expected class names (listed in the default config; any other public non-abstract {@code TgMap} class in
 * {@code com.mcm.games.tgttos.maps..} is also auto-discovered from the jar):
 * {@code maps.a.LaundryLineMap, maps.a.DamFaceMap, maps.a.BookshelfRunMap, maps.b.TicketGates}
 * (elytra / boat / drop maps moved to the Race plugin, feedback 3).
 *
 * <p>{@link #build(long)} may be called more than once (preview tools, validators, each game) and must be
 * deterministic for a given seed. It runs on the main thread at game start, so keep it to well under a second.
 */
public interface TgMap {
    /** Stable lowercase id, e.g. {@code "laundry_line"}. Used by {@code /tg map <id>}. */
    String id();

    /** Display name for title card and sidebar, e.g. {@code "Laundry Line"}. Keep it under ~16 chars. */
    String displayName();

    MapType type();

    Kit kit();

    /** Generate the map. See {@link TgMapData} for the coordinate conventions. */
    TgMapData build(long seed);

    /**
     * Lethal falls (default off). When {@code > 0}: a racer who lands on anything that is not water after
     * falling at least this many blocks (highest point since last standing / swimming / climbing, minus landing
     * height) fails and is sent back to a start spawn with a fresh kit, exactly like a void fall, plus a chat
     * message. No health damage is ever dealt. Gliding, riding, climbing and swimming reset the fall.
     * {@code 0} = off (every map except a deliberate drop map).
     */
    default int lethalFallHeight() { return 0; }

    /**
     * Decoy chickens (default: none listed = the runtime scatters {@link DecoySpots#DEFAULT_COUNT} over the later part
     * of the map with {@link DecoySpots#pick}). Each entry is a local block position {@code {x, y, z}} (feet block,
     * same convention as {@link TgMapData#pen}); the chicken starts at the block centre. Owner rules (playtest 2): decoys
     * look exactly like goal chickens (same "Punch me!" name tag), wander (put back home when further than
     * {@code goal.decoy-leash-radius}), can't be hurt or pushed, are never in the real pen, and punching one only
     * answers {@code goal.decoy-message} ("haha lol") with a sound. Removed at round end like every other game entity.
     */
    default java.util.List<int[]> decoyChickens() { return java.util.List.of(); }

    /** False = an empty {@link #decoyChickens()} means really no decoys (imported maps: one honest goal row). */
    default boolean scatterDecoys() { return true; }

    /**
     * Interactive features (default none): moving hazards, timed block toggles, line-bonus gates, checkpoints,
     * rocket refills, boat depots. Local coordinates of the slot like {@link TgMapData}. Must describe the map
     * {@link #build(long)} produced (deterministic maps return a fixed set). See {@link MapFeatures} and the README
     * section "Map contract additions".
     */
    default MapFeatures features() { return MapFeatures.NONE; }

    /** Round length in seconds for this map; {@code 0} (default) = config {@code round-seconds}. */
    default int roundSeconds() { return 0; }

    /** Builder credit shown to players as "Map by ..." ({@code null} = our own map, nothing shown). */
    default String credit() { return null; }

    /** {@code false} = only played when chosen with {@code /tg map} / {@code /mcm map}, never picked at random. */
    default boolean inRotation() { return true; }
}
