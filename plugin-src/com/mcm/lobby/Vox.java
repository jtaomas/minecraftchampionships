package com.mcm.lobby;

import java.util.*;

/**
 * Sparse voxel grid of block-state strings (pure Java, no Bukkit), stored as 16^3 sections of palette indices.
 * Index 0 = air. Used by the hub generator both offline (dump for the renderer) and at runtime
 * (the core places the sections over ticks).
 */
public final class Vox {
    private final HashMap<Long, short[]> sections = new HashMap<>();
    private final ArrayList<String> palette = new ArrayList<>();
    private final HashMap<String, Integer> index = new HashMap<>();
    private final ArrayList<String> baseIds = new ArrayList<>();
    int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    public Vox() { palette.add(null); baseIds.add(null); }

    private static long key(int sx, int sy, int sz) {
        return (((long) sx & 0x3FFFFFL) << 42) | (((long) sy & 0xFFFFFL) << 22) | ((long) sz & 0x3FFFFFL);
    }

    private int idOf(String s) {
        if (s == null) return 0;
        if (s.indexOf(':') < 0) s = "minecraft:" + s;
        if (s.equals("minecraft:air")) return 0;
        Integer i = index.get(s);
        if (i != null) return i;
        palette.add(s);
        int b = s.indexOf('[');
        String base = b < 0 ? s : s.substring(0, b);
        baseIds.add(base.substring(base.indexOf(':') + 1));
        index.put(s, palette.size() - 1);
        if (palette.size() > 32000) throw new IllegalStateException("palette overflow");
        return palette.size() - 1;
    }

    public void set(int x, int y, int z, String s) {
        int id = idOf(s);
        long k = key(x >> 4, y >> 4, z >> 4);
        short[] sec = sections.get(k);
        if (sec == null) {
            if (id == 0) return;
            sec = new short[4096];
            sections.put(k, sec);
        }
        sec[((y & 15) << 8) | ((z & 15) << 4) | (x & 15)] = (short) id;
        if (id != 0) {
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
        }
    }

    int rawId(int x, int y, int z) {
        short[] sec = sections.get(key(x >> 4, y >> 4, z >> 4));
        return sec == null ? 0 : sec[((y & 15) << 8) | ((z & 15) << 4) | (x & 15)];
    }

    /** Full block-state string or null for air. */
    public String get(int x, int y, int z) { return palette.get(rawId(x, y, z)); }

    /** Block id without namespace/states (e.g. "oak_stairs") or null. */
    public String id(int x, int y, int z) { return baseIds.get(rawId(x, y, z)); }

    public boolean air(int x, int y, int z) { return rawId(x, y, z) == 0; }

    public void setIfAir(int x, int y, int z, String s) { if (air(x, y, z)) set(x, y, z, s); }

    /** Highest non-air y in the column in [minYSearch, startY], or Integer.MIN_VALUE. */
    public int top(int x, int z, int startY, int minYSearch) {
        for (int y = startY; y >= minYSearch; y--) if (!air(x, y, z)) return y;
        return Integer.MIN_VALUE;
    }

    public List<String> palette() { return Collections.unmodifiableList(palette); }

    /** Section coordinates {sx, sy, sz}, sorted bottom-up. */
    public List<int[]> sectionCoords() {
        List<int[]> out = new ArrayList<>(sections.size());
        for (long k : sections.keySet()) {
            int sx = (int) (k >> 42); if (sx >= (1 << 21)) sx -= (1 << 22);
            int sy = (int) ((k >> 22) & 0xFFFFFL); if (sy >= (1 << 19)) sy -= (1 << 20);
            int sz = (int) (k & 0x3FFFFFL); if (sz >= (1 << 21)) sz -= (1 << 22);
            out.add(new int[]{sx, sy, sz});
        }
        out.sort((a, b) -> a[1] != b[1] ? Integer.compare(a[1], b[1]) : a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(a[2], b[2]));
        return out;
    }

    /** Raw section array (palette indices; layout y*256 + z*16 + x) or null. */
    public short[] section(int sx, int sy, int sz) { return sections.get(key(sx, sy, sz)); }

    public long countBlocks() {
        long n = 0;
        for (short[] s : sections.values()) for (short v : s) if (v != 0) n++;
        return n;
    }

    /** Count of non-air blocks inside a box. */
    public long countBlocks(int x1, int y1, int z1, int x2, int y2, int z2) {
        long n = 0;
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = y1; y <= y2; y++) if (rawId(x, y, z) != 0) n++;
        return n;
    }

    public int minX() { return minX; } public int minY() { return minY; } public int minZ() { return minZ; }
    public int maxX() { return maxX; } public int maxY() { return maxY; } public int maxZ() { return maxZ; }
}
