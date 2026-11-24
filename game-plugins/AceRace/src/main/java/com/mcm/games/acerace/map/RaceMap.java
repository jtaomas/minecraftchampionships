package com.mcm.games.acerace.map;

import java.util.List;

/** A pure-Java Ace Race map: identity, per-map rules and a deterministic blockout builder. */
public interface RaceMap {
    String id();

    String displayName();

    default String themeId() {
        return id();
    }

    int laps();

    int timeLimitSeconds();

    default int countdownSeconds() {
        return 10;
    }

    RaceMapData build(long seed);

    default int blockBudget() {
        return 350_000;
    }

    default List<String> notes() {
        return List.of();
    }
}
