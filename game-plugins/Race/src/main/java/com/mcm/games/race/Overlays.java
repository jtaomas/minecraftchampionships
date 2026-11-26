package com.mcm.games.race;

import com.mcm.api.McmMapOverlay;
import com.mcm.games.race.model.TgMapData;
import com.mcm.games.race.model.TgMapData.SparseBlocks;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Map Edit overlays (core {@code /mcm mapedit}): the blocks an op changed in creative, stored in
 * {@code plugins/McmRace/mapedits/<map>.overlay.gz}. {@link #apply} layers them over a map's generated / imported
 * blocks, so everything downstream (the paster's stale clearing, FeatureRuntime's restores, the finish-row checks) sees
 * the edited map; a map's own generator data is never modified. Safe on a core without Map Edit (returns the data
 * unchanged).
 */
final class Overlays {
    private static Plugin plugin;

    private Overlays() {}

    static void init(Plugin p) { plugin = p; }

    /** {@code base} with the saved overlay of {@code mapId} on top, or {@code base} itself if there is none / it cannot be read. */
    static TgMapData apply(String mapId, TgMapData base) {
        if (plugin == null || base == null) return base;
        try {
            McmMapOverlay ov = McmMapOverlay.of(plugin, mapId);
            if (ov == null || ov.isEmpty()) return base;
            Map<Long, String> over = ov.toMap();
            Layered blocks = new Layered(base.blocks, over);
            TgMapData d = new TgMapData(base.mapId, base.seed, blocks);
            d.startSpawns.addAll(base.startSpawns);
            d.startStrip = base.startStrip;
            d.startBarrier.addAll(base.startBarrier);
            d.dropFloor.addAll(base.dropFloor);
            d.boatSpawns.addAll(base.boatSpawns);
            d.chickenSpawns.addAll(base.chickenSpawns);
            d.pen = base.pen;
            d.voidY = base.voidY;
            d.hazardBlocks.addAll(base.hazardBlocks);
            d.hazardRegions.addAll(base.hazardRegions);
            d.playBounds = base.playBounds;
            d.buildBounds = base.buildBounds;
            d.variantRules.addAll(base.variantRules);
            d.overviewPoints.addAll(base.overviewPoints);
            d.extraItems.addAll(base.extraItems);
            d.notes.putAll(base.notes);
            for (TgMapData.VariantGroup g : base.variants) {
                TgMapData.VariantGroup g2 = new TgMapData.VariantGroup(g.name, g.category);
                g2.weights.putAll(g.weights);
                for (Map.Entry<String, List<TgMapData.Placement>> e : g.options.entrySet()) {
                    List<TgMapData.Placement> keep = new ArrayList<>(e.getValue().size());
                    for (TgMapData.Placement pl : e.getValue())
                        if (!over.containsKey(SparseBlocks.key(pl.x(), pl.y(), pl.z()))) keep.add(pl);   // an edit beats the round's variant
                    g2.options.put(e.getKey(), keep);
                }
                d.variants.add(g2);
            }
            d.notes.put("mapedit", ov.size() + " edited blocks by " + ov.editor);
            return d;
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Race: map overlay of " + mapId + " could not be applied; playing the original map", t);
            return base;
        }
    }

    /** Base blocks with an overlay layer on top (a null state in the layer = air). Does not copy the base. */
    static final class Layered implements TgMapData.Blocks {
        private final TgMapData.Blocks base;
        private final Map<Long, String> over = new HashMap<>();
        private final int size;

        Layered(TgMapData.Blocks base, Map<Long, String> overlay) {
            this.base = base;
            int n = base.size();
            for (Map.Entry<Long, String> e : overlay.entrySet()) {
                long k = e.getKey();
                String s = SparseBlocks.normalize(e.getValue());
                over.put(k, s);
                boolean had = base.get(SparseBlocks.kx(k), SparseBlocks.ky(k), SparseBlocks.kz(k)) != null;
                if (had && s == null) n--;
                else if (!had && s != null) n++;
            }
            this.size = n;
        }

        @Override public void set(int x, int y, int z, String b) { over.put(SparseBlocks.key(x, y, z), SparseBlocks.normalize(b)); }

        @Override public String get(int x, int y, int z) {
            long k = SparseBlocks.key(x, y, z);
            if (over.containsKey(k)) return over.get(k);
            return base.get(x, y, z);
        }

        @Override public void forEach(TgMapData.Visitor v) {
            base.forEach((x, y, z, b) -> { if (!over.containsKey(SparseBlocks.key(x, y, z))) v.block(x, y, z, b); });
            for (Map.Entry<Long, String> e : over.entrySet()) {
                if (e.getValue() == null) continue;
                long k = e.getKey();
                v.block(SparseBlocks.kx(k), SparseBlocks.ky(k), SparseBlocks.kz(k), e.getValue());
            }
        }

        @Override public int size() { return size; }
    }
}
