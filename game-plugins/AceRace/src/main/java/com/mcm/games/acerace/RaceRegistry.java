package com.mcm.games.acerace;

import com.mcm.games.acerace.map.RaceMap;
import com.mcm.games.acerace.map.RaceMapData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Map registry: ids, slots, cached pure-Java data and rotation. */
public final class RaceRegistry {
    private final Map<String, RaceMap> maps = new LinkedHashMap<>();
    private final Map<String, RaceMapData> data = new LinkedHashMap<>();
    private final Map<String, Integer> slots = new LinkedHashMap<>();

    public void register(RaceMap map, int slot) {
        String id = map.id().toLowerCase(Locale.ROOT);
        maps.put(id, map);
        slots.put(id, slot);
    }

    public RaceMap map(String id) {
        return id == null ? null : maps.get(id.toLowerCase(Locale.ROOT));
    }

    public RaceMapData data(String id) {
        if (id == null) return null;
        String key = id.toLowerCase(Locale.ROOT);
        RaceMap map = maps.get(key);
        if (map == null) return null;
        return data.computeIfAbsent(key, k -> map.build(0L));
    }

    public int slot(String id) {
        Integer s = slots.get(id == null ? null : id.toLowerCase(Locale.ROOT));
        return s == null ? 0 : s;
    }

    public List<String> ids() {
        return new ArrayList<>(maps.keySet());
    }

    public List<RaceMap> maps() {
        return new ArrayList<>(maps.values());
    }

    /** Next map in registration order; a null/unknown current starts at the first. */
    public String next(String current) {
        List<String> ids = ids();
        if (ids.isEmpty()) return null;
        if (current == null) return ids.get(0);
        int i = ids.indexOf(current.toLowerCase(Locale.ROOT));
        return ids.get((i + 1) % ids.size());
    }
}
