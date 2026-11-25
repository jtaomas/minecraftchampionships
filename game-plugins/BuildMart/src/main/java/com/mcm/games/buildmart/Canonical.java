package com.mcm.games.buildmart;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.Set;
import java.util.TreeMap;

/**
 * Canonical block state used by the build check. Keeps what the player controls and what MCC checks since MCC All-Stars
 * (block type, facing, half, slab type, axis, open, face, rotation, attachment, hanging) and drops what is computed by the
 * game or is invisible (stair shape, fence/wall/pane connections, door hinge, powered, waterlogged, leaf distance...).
 * A wrong-facing stair therefore does NOT count, a flipped door hinge does.
 */
final class Canonical {
    private Canonical() { }

    static final String AIR = "air";

    private static final Set<String> IGNORED = Set.of(
            "shape", "waterlogged", "powered", "hinge", "distance", "persistent", "snowy",
            "north", "south", "east", "west", "up", "down", "lit", "signal_fire", "level", "age",
            "triggered", "occupied", "in_wall", "note", "instrument", "moisture", "stage", "bites",
            "has_book", "has_record", "conditional", "enabled", "inverted", "power", "mode", "unstable",
            "honey_level", "candles", "pickles", "eggs", "hatch", "layers_ignored");

    /** Canonical key for a block, or "air" for air, cave air and flowing water/lava (only sources count). */
    static String of(BlockData data) {
        Material m = data.getMaterial();
        if (m.isAir()) return AIR;
        String s = data.getAsString();
        if (m == Material.WATER || m == Material.LAVA) {
            if (s.contains("level=") && !s.contains("level=0")) return AIR;
            return m.getKey().toString();
        }
        int b = s.indexOf('[');
        if (b < 0) return s;
        String id = s.substring(0, b);
        TreeMap<String, String> keep = new TreeMap<>();
        for (String kv : s.substring(b + 1, s.length() - 1).split(",")) {
            int e = kv.indexOf('=');
            if (e < 0) continue;
            String k = kv.substring(0, e);
            if (IGNORED.contains(k)) continue;
            // A double slab is a full block: "double" is kept, top/bottom too.
            keep.put(k, kv.substring(e + 1));
        }
        if (keep.isEmpty()) return id;
        StringBuilder sb = new StringBuilder(id).append('[');
        boolean first = true;
        for (var en : keep.entrySet()) {
            if (!first) sb.append(',');
            sb.append(en.getKey()).append('=').append(en.getValue());
            first = false;
        }
        return sb.append(']').toString();
    }

    /** Human name, e.g. "Stone Brick Stairs". */
    static String nice(Material m) {
        String[] parts = m.getKey().getKey().split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }
}
