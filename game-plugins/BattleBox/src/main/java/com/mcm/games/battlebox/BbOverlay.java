package com.mcm.games.battlebox;

import com.mcm.api.McmMapOverlay;
import com.mcm.games.battlebox.arena.ArenaGeo;
import com.mcm.games.battlebox.arena.ArenaTheme;
import org.bukkit.Bukkit;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Map Edit overlays (core {@code /mcm mapedit}) for Battle Box: {@code plugins/McmBattleBox/mapedits/<theme id>.overlay.gz}
 * holds the edits to one arena copy in arena-local coordinates. {@link ArenaPlacer} layers it over the generated grid
 * of every arena copy, every round, so the overlay wins everywhere except in the cells the game paints itself
 * ({@link ArenaTheme#managedZones}: centre pad, team gates, spawn pads, kit rooms). Calls are guarded: a core without
 * Map Edit changes nothing.
 */
final class BbOverlay {
    /** Blocks may be edited this far outside the arena's own box, as long as they stay clear of the neighbouring arena slot. */
    static final int MARGIN = 16;
    private static final int SLOT_HALF = 49;   // arena slots are 100 apart
    private static final int MIN_LOCAL_Y = -128, MAX_LOCAL_Y = 255;   // world y -64..319 at ARENA_Y 64

    private static Plugin plugin;
    private static final Map<String, Layer> CACHE = new ConcurrentHashMap<>();

    private BbOverlay() {}

    static void init(Plugin p) { plugin = p; }

    static void invalidate(String themeId) { CACHE.remove(themeId); }

    /** The overlay of a theme ready to be layered over a grid: only cells outside the managed zones, inside the edit box. */
    static final class Layer {
        private final McmMapOverlay source;
        private final Map<Long, BlockData> blocks;
        /** Local bounds {minX, minY, minZ, maxX, maxY, maxZ} of the blocks. */
        final int[] bounds;

        private Layer(McmMapOverlay source, Map<Long, BlockData> blocks, int[] bounds) {
            this.source = source; this.blocks = blocks; this.bounds = bounds;
        }

        BlockData get(int lx, int ly, int lz) {
            if (lx < bounds[0] || lx > bounds[3] || ly < bounds[1] || ly > bounds[4] || lz < bounds[2] || lz > bounds[5]) return null;
            return blocks.get(McmMapOverlay.key(lx, ly, lz));
        }

        int size() { return blocks.size(); }
    }

    /** Local box {x0, y0, z0, x1, y1, z1} in which edits are tracked and applied: the arena box plus a margin. */
    static int[] box(ArenaTheme t, ArenaGeo geo) {
        int[] mn = t.minBounds(geo), mx = t.maxBounds(geo);
        return new int[]{
                Math.min(mn[0], Math.max(mn[0] - MARGIN, -SLOT_HALF)), Math.max(MIN_LOCAL_Y, mn[1] - MARGIN),
                Math.min(mn[2], Math.max(mn[2] - MARGIN, -SLOT_HALF)),
                Math.max(mx[0], Math.min(mx[0] + MARGIN, SLOT_HALF)), Math.min(MAX_LOCAL_Y, mx[1] + MARGIN),
                Math.max(mx[2], Math.min(mx[2] + MARGIN, SLOT_HALF))};
    }

    /** The theme's overlay layer, or null when it has none (or Map Edit is unavailable). Cached until the file changes. */
    static Layer of(ArenaTheme t, ArenaGeo geo) {
        if (plugin == null || t == null) return null;
        try {
            McmMapOverlay ov = McmMapOverlay.of(plugin, t.id());
            if (ov == null || ov.isEmpty()) { CACHE.remove(t.id()); return null; }
            Layer c = CACHE.get(t.id());
            if (c != null && c.source == ov) return c;
            Layer l = build(t, geo, ov);
            if (l == null) { CACHE.remove(t.id()); return null; }
            CACHE.put(t.id(), l);
            return l;
        } catch (Throwable e) {
            plugin.getLogger().log(Level.WARNING, "Battle Box: map overlay of " + t.id() + " could not be applied; playing the original arena", e);
            return null;
        }
    }

    private static Layer build(ArenaTheme t, ArenaGeo geo, McmMapOverlay ov) {
        int[] box = box(t, geo);
        List<ArenaTheme.Zone> zones = t.managedZones(geo);
        Map<String, BlockData> parsed = new HashMap<>();
        Map<Long, BlockData> blocks = new HashMap<>(Math.max(16, ov.size() * 4 / 3));
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        int[] skipped = {0};
        ov.forEach((x, y, z, state) -> {
            if (x < box[0] || x > box[3] || y < box[1] || y > box[4] || z < box[2] || z > box[5]) { skipped[0]++; return; }
            for (ArenaTheme.Zone zn : zones) if (zn.contains(x, y, z)) { skipped[0]++; return; }
            BlockData d = parsed.computeIfAbsent(state, s -> {
                try { return Bukkit.createBlockData(s); } catch (IllegalArgumentException e) { return null; }
            });
            if (d == null) { skipped[0]++; return; }
            blocks.put(McmMapOverlay.key(x, y, z), d);
            b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.min(b[2], z);
            b[3] = Math.max(b[3], x); b[4] = Math.max(b[4], y); b[5] = Math.max(b[5], z);
        });
        if (blocks.isEmpty()) return null;
        plugin.getLogger().info("Battle Box: map overlay of " + t.id() + ": " + blocks.size() + " blocks"
                + (skipped[0] > 0 ? " (" + skipped[0] + " ignored: game-managed cells or outside the edit box)" : ""));
        return new Layer(ov, blocks, b);
    }
}
