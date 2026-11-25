package com.mcm.games.rocketspleef.map;

import java.util.List;

/**
 * One old-MCC Rocket Spleef map, generated into a {@link Canvas} at 1:1 scale for about 40 players (pure Java).
 * Local origin: x/z = map centre, y = 0 is the lowest block of the map.
 */
public interface MapGen {
    String id();
    String displayName();
    /** Reference image (repo-relative) the generator recreates. */
    String reference();
    Canvas generate();

    /** The three maps, in round order. */
    static List<MapGen> all() {
        return List.of(new NanotubeMap(), new RingsMap(), new EasterEggsMap());
    }

    static MapGen byId(String id) {
        for (MapGen m : all()) if (m.id().equalsIgnoreCase(id)) return m;
        return null;
    }
}
