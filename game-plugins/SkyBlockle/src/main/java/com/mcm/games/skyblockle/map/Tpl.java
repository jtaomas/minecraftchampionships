package com.mcm.games.skyblockle.map;

import com.mcm.games.skyblockle.MapModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An island template in its own local frame: centred on x = z = 0, standing surface at y = 0, and its
 * "front" (the side that faces the castle) pointing to -z (north). Instances place it with a translation and a
 * quarter turn, so every copy is block-for-block identical.
 */
final class Tpl {
    final String name;
    final Map<Long, String> blocks = new HashMap<>();
    final List<int[]> chests = new ArrayList<>();          // local x, y, z
    final List<MapModel.IslandType> chestTypes = new ArrayList<>();
    /** Loot variant per chest (null = plain table); "hidden" = buried chest, reached by digging. */
    final List<String> chestVariants = new ArrayList<>();
    final List<int[]> spawns = new ArrayList<>();          // local feet x, y, z
    final List<int[]> lanes = new ArrayList<>();           // local staircase lanes: x, z, floorY, dx, dz, n
    /** Ladder columns that must be climbable: local x, yFrom, z, yTo (feet heights). */
    final List<int[]> climbs = new ArrayList<>();
    boolean pillar;                                          // chest on top of a column: reached by towering up

    Tpl(String name) { this.name = name; }

    static long key(int x, int y, int z) {
        return ((long) (x & 0xFFFFF) << 40) | ((long) (z & 0xFFFFF) << 20) | (y & 0xFFFFF);
    }

    static int kx(long k) { return (int) (k >> 40) << 12 >> 12; }
    static int kz(long k) { return (int) ((k >> 20) & 0xFFFFF) << 12 >> 12; }
    static int ky(long k) { return (int) (k & 0xFFFFF) << 12 >> 12; }

    void set(int x, int y, int z, String b) {
        if (Bs.isAir(b)) blocks.remove(key(x, y, z));
        else blocks.put(key(x, y, z), Bs.full(b));
    }

    String get(int x, int y, int z) { return blocks.get(key(x, y, z)); }

    boolean has(int x, int y, int z) { return blocks.containsKey(key(x, y, z)); }

    void chest(int x, int y, int z, String facing, MapModel.IslandType type) { chest(x, y, z, facing, type, null); }

    void chest(int x, int y, int z, String facing, MapModel.IslandType type, String variant) {
        set(x, y, z, "chest[facing=" + facing + ",type=single,waterlogged=false]");
        chests.add(new int[]{x, y, z});
        chestTypes.add(type);
        chestVariants.add(variant);
    }

    boolean hidden(int c) { return "hidden".equals(chestVariants.get(c)); }

    /** Highest non-air y in the column, or Integer.MIN_VALUE. */
    int top(int x, int z) {
        int best = Integer.MIN_VALUE;
        for (long k : blocks.keySet()) if (kx(k) == x && kz(k) == z) best = Math.max(best, ky(k));
        return best;
    }
}
