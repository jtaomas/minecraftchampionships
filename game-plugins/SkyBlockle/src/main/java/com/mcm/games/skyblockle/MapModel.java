package com.mcm.games.skyblockle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java result of the Sky Blockle map generator (no Bukkit types). Contract between the game mechanics
 * (package {@code com.mcm.games.skyblockle}) and the generator ({@code com.mcm.games.skyblockle.map.SkyMap}).
 *
 * <p>Generator entry point: {@code com.mcm.games.skyblockle.map.SkyMap.generate(int wedges, long seed) -> MapModel}.
 *
 * <p><b>Coordinates</b> are local, integer block coordinates. The map centre (centre of the castle arena floor)
 * is x = 0, z = 0. y = 0 is the map reference height; the runtime places local (0,0,0) at world
 * (0, {@code map.base-y}, 0) from config, so the map is centred on the world origin (needed for the vanilla
 * world border). Keep everything within local y -60 .. +120.
 *
 * <p><b>Blocks</b> are vanilla block-state strings such as {@code minecraft:oak_stairs[facing=north,half=top]}
 * ("minecraft:" may be omitted). null / "air" clears. Chests listed in {@link #chests} must also be placed as
 * chest blocks in {@link #blocks} (the runtime fills them; it does not place them).
 *
 * <p><b>Wedges</b> are numbered 0..wedges-1, evenly spaced. Wedge i's line to the centre points at angle
 * {@code i * 360 / wedges} degrees measured from +Z (south) towards -X (Minecraft yaw convention), so the
 * runtime can assign sides to wedges in order.
 */
public final class MapModel {

    /** Island types; the loot system reads {@code loot.yml} section {@code name().toLowerCase()}. */
    public enum IslandType { START, GENERATOR, MID, FORT, BREWING, ENCHANTING, CASTLE }

    /** A player start position. {@code wedge} 0..wedges-1. Feet position (stand on the block below). */
    public record Spawn(int wedge, double x, double y, double z, float yaw) {}

    /**
     * A chest block to fill. {@code wedge} is the owning wedge, or -1 for neutral/shared islands.
     * {@code variant} (optional, may be null) selects a more specific loot table {@code <type>_<variant>} in
     * loot.yml, e.g. MID + "pillar" -> {@code mid_pillar}; GENERATOR + "hidden" -> {@code generator_hidden}.
     * Known variants: start; generator: hut, hidden; mid: pillar, spike, grass, showpiece. Without a variant the
     * runtime infers pillar/spike/grass for MID chests from the block under the chest.
     */
    public record ChestSpot(int x, int y, int z, IslandType type, int wedge, String variant) {
        public ChestSpot(int x, int y, int z, IslandType type, int wedge) { this(x, y, z, type, wedge, null); }
    }

    /**
     * A block the runtime recolours in the colour of the side that owns {@code wedge} (the generator does not
     * know team colours). {@code suffix} is the colour-less part of a dyed block id plus optional state, e.g.
     * {@code "_concrete"}, {@code "_wool"}, {@code "_stained_glass"}, {@code "_terracotta"}, {@code "_carpet"},
     * {@code "_banner[rotation=8]"}, {@code "_wall_banner[facing=north]"}. The runtime places
     * {@code minecraft:<colour><suffix>}; also put a neutral fallback (e.g. white) in {@link #blocks} for previews.
     */
    public record TeamMarker(int x, int y, int z, int wedge, String suffix) {}

    /** Ring radii (blocks from the centre, horizontal) of the island rings, for info, border and HUD. */
    public record Rings(int castle, int enchanting, int brewing, int fort, int pillar,
                        int start, int generator, int rock) {}

    /** Sparse block store; also the sink the generator writes into. */
    public interface Blocks {
        void set(int x, int y, int z, String blockData);
        /** Block-state string at the position, or null for air / unset. */
        String get(int x, int y, int z);
        /** Visit every non-air block. Order unspecified. */
        void forEach(Visitor v);
        int size();
    }

    @FunctionalInterface
    public interface Visitor { void block(int x, int y, int z, String blockData); }

    /** Default sparse implementation (HashMap keyed by packed coordinates). */
    public static final class SparseBlocks implements Blocks {
        private final Map<Long, String> map = new HashMap<>();
        private final Map<String, String> intern = new HashMap<>();

        static long key(int x, int y, int z) {
            return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
        }
        static int kx(long k) { return (int) (k >> 38) << 6 >> 6; }
        static int kz(long k) { return (int) ((k >> 12) & 0x3FFFFFF) << 6 >> 6; }
        static int ky(long k) { return (int) (k & 0xFFF) << 20 >> 20; }

        @Override public void set(int x, int y, int z, String b) {
            long k = key(x, y, z);
            if (b == null || b.isEmpty() || b.equals("air") || b.equals("minecraft:air")) { map.remove(k); return; }
            String f = b.indexOf(':') < 0 ? "minecraft:" + b : b;
            map.put(k, intern.computeIfAbsent(f, s -> s));
        }
        @Override public String get(int x, int y, int z) { return map.get(key(x, y, z)); }
        @Override public void forEach(Visitor v) {
            for (Map.Entry<Long, String> e : map.entrySet()) {
                long k = e.getKey();
                v.block(kx(k), ky(k), kz(k), e.getValue());
            }
        }
        @Override public int size() { return map.size(); }
    }

    public final int wedges;
    public final long seed;
    public final Blocks blocks;
    public final List<Spawn> spawns = new ArrayList<>();
    public final List<ChestSpot> chests = new ArrayList<>();
    public final List<TeamMarker> teamMarkers = new ArrayList<>();
    /** Castle centre (arena floor surface block, local). Normally 0, floorY, 0. */
    public int castleX, castleY, castleZ;
    /** Radius of the flat castle arena floor (the final border target). */
    public int arenaRadius = 12;
    /** Outer radius of the castle build. */
    public int castleRadius = 25;
    /** Horizontal radius containing every block of the map. */
    public int mapRadius = 170;
    public Rings rings = new Rings(22, 48, 70, 103, 119, 135, 150, 165);
    /** Lowest / highest local y used by any block (for cleanup bounds and spectator limits). */
    public int minY = -40, maxY = 80;
    /** Free-form generator notes/validator results (e.g. "gap.min" -> "14"). Informational. */
    public final Map<String, String> notes = new LinkedHashMap<>();

    public MapModel(int wedges, long seed, Blocks blocks) {
        this.wedges = wedges;
        this.seed = seed;
        this.blocks = blocks;
    }

    public MapModel(int wedges, long seed) { this(wedges, seed, new SparseBlocks()); }

    /** Spawns for one wedge (empty list if none). */
    public List<Spawn> spawnsFor(int wedge) {
        List<Spawn> out = new ArrayList<>();
        for (Spawn s : spawns) if (s.wedge() == wedge) out.add(s);
        return Collections.unmodifiableList(out);
    }
}
