package com.mcm.games.tgttos.model;

/**
 * One TGTTOS map generator (pure Java, no Bukkit classes). Implementations need a public no-arg constructor;
 * the runtime instantiates them by fully-qualified class name from {@code config.yml -> maps:}.
 *
 * <p>Expected class names (listed in the default config; any other public non-abstract {@code TgMap} class in
 * {@code com.mcm.games.tgttos.maps..} is also auto-discovered from the jar):
 * {@code maps.a.LaundryLineMap, maps.a.DamFaceMap, maps.a.BookshelfRunMap, maps.a.OrchardDropMap,
 * maps.b.KiteFestivalMap, maps.b.FrozenCanalMap, maps.b.TicketGatesMap, maps.b.QuarryStepsMap}.
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
     * Decoy chickens (default none). Each entry is a local block position {@code {x, y, z}} (feet block of the
     * chicken, same local integer convention as {@link TgMapData#pen}); the chicken stands at the block centre
     * (x + 0.5, y, z + 0.5). Decoys have no name tag (real goal chickens show a visible name), never move, can't
     * be hurt or pushed, and punching one does nothing. Positions may be inside or outside the pen. Removed at
     * round end like every other game entity.
     */
    default java.util.List<int[]> decoyChickens() { return java.util.List.of(); }
}
