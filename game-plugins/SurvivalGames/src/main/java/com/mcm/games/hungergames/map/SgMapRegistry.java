package com.mcm.games.hungergames.map;

import java.util.List;
import java.util.Locale;

/**
 * Registry of procedural Survival Games maps.
 */
public final class SgMapRegistry {
    private static final List<SgMap> ALL = List.of(
            new CityDistrictMap(),
            new WoodlandBasinMap(),
            new HarbourCitadelMap()
    );

    private SgMapRegistry() {}

    public static List<String> ids() {
        return ALL.stream().map(SgMap::id).toList();
    }

    public static SgMap byId(String id) {
        if (id != null) {
            String lower = id.toLowerCase(Locale.ROOT);
            for (SgMap m : ALL) {
                if (m.id().equals(lower)) return m;
            }
        }
        return ALL.get(0);
    }

    public static List<SgMap> all() {
        return ALL;
    }
}
