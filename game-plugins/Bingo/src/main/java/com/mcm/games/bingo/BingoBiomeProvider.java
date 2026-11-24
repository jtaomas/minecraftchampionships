package com.mcm.games.bingo;

import org.bukkit.block.Biome;
import org.bukkit.generator.BiomeParameterPoint;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Biome source of mcc_bingo. Terrain still comes from the vanilla noise generator; this only decides the biome:
 * inside a planned game area it is the area's cell biome, except where vanilla terrain has water (ocean, river,
 * beach), which keeps a water biome tinted to the cell's climate, and underground (climate depth > 0.2) below cells
 * that have a cave biome. Outside planned areas (area 0 around the world spawn, never played) it is vanilla.
 *
 * <p>Called on world-gen threads: only reads immutable layouts from a concurrent map and the vanilla provider.
 */
final class BingoBiomeProvider extends BiomeProvider {

    private final Map<Integer, AreaLayout> layouts = new ConcurrentHashMap<>();
    private volatile BiomeProvider vanilla;

    void attachVanilla(BiomeProvider v) { this.vanilla = v; }

    void register(AreaLayout layout) { layouts.put(layout.index, layout); }

    static int areaIndexOf(int x) { return Math.floorDiv(x + BingoWorld.AREA_SPACING / 2, BingoWorld.AREA_SPACING); }

    private AreaLayout layoutAt(int x) { return layouts.get(areaIndexOf(x)); }

    private Biome vanillaAt(WorldInfo info, int x, int y, int z) {
        BiomeProvider v = vanilla;
        if (v == null) return Biome.PLAINS;
        try {
            Biome b = v.getBiome(info, x, y, z);
            return b != null ? b : Biome.PLAINS;
        } catch (RuntimeException ex) {
            return Biome.PLAINS;
        }
    }

    @Override
    public Biome getBiome(WorldInfo info, int x, int y, int z) {
        AreaLayout l = layoutAt(x);
        if (l == null) return vanillaAt(info, x, y, z);
        AreaLayout.Cell c = l.cellAt(x, z);
        Biome v = vanillaAt(info, x, y, z);
        if (y < 0 && c.under() != null) return c.under();
        if (AreaLayout.isWater(v)) return AreaLayout.mapWater(v, c.surface());
        return c.surface();
    }

    @Override
    public Biome getBiome(WorldInfo info, int x, int y, int z, BiomeParameterPoint p) {
        AreaLayout l = layoutAt(x);
        if (l == null) return vanillaAt(info, x, y, z);
        AreaLayout.Cell c = l.cellAt(x, z);
        double depth = p.getDepth(), cont = p.getContinentalness();
        if (depth > 0.2) {
            if (c.under() != null) return c.under();
            if (cont < -0.19) return AreaLayout.mapWater(cont < -0.455 ? Biome.DEEP_OCEAN : Biome.OCEAN, c.surface());
            return c.surface();
        }
        // Vanilla only ever places ocean/beach at low continentalness and rivers in valleys (|weirdness| ~ 0):
        // ask vanilla only there, everything else is land.
        if (cont < -0.10 || Math.abs(p.getWeirdness()) < 0.06) {
            Biome v = vanillaAt(info, x, y, z);
            if (AreaLayout.isWater(v)) return AreaLayout.mapWater(v, c.surface());
        }
        return c.surface();
    }

    /** Every overworld biome (vanilla structure sets and feature order are built from this list). */
    @Override
    public List<Biome> getBiomes(WorldInfo info) {
        EnumSet<Biome> out = EnumSet.allOf(Biome.class);
        out.removeAll(EnumSet.of(Biome.CUSTOM, Biome.THE_VOID, Biome.NETHER_WASTES, Biome.SOUL_SAND_VALLEY,
                Biome.CRIMSON_FOREST, Biome.WARPED_FOREST, Biome.BASALT_DELTAS, Biome.THE_END, Biome.SMALL_END_ISLANDS,
                Biome.END_MIDLANDS, Biome.END_HIGHLANDS, Biome.END_BARRENS));
        return new ArrayList<>(out);
    }
}
