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
}
