package com.mcm.games.acerace.map;

import java.util.HashMap;
import java.util.Map;

/** Sparse local block store: packed position key -> block-state string. Pure Java, no Bukkit. */
public final class RaceBlocks {
    private final Map<Long, String> blocks = new HashMap<>();

    public void set(int x, int y, int z, String state) {
        if (state == null) return;
        String s = state.trim().toLowerCase();
        if (s.equals("air") || s.equals("minecraft:air") || s.isEmpty()) {
            blocks.remove(key(x, y, z));
            return;
        }
        blocks.put(key(x, y, z), s.contains(":") ? s : "minecraft:" + s);
    }

    public String get(int x, int y, int z) {
        return blocks.get(key(x, y, z));
    }

    public int size() {
        return blocks.size();
    }

    public void forEach(BlockConsumer consumer) {
        for (Map.Entry<Long, String> e : blocks.entrySet()) {
            long k = e.getKey();
            consumer.accept(x(k), y(k), z(k), e.getValue());
        }
    }

    @FunctionalInterface
    public interface BlockConsumer {
        void accept(int x, int y, int z, String state);
    }

    public static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFFL);
    }

    public static int x(long k) {
        return (int) (k >> 38);
    }

    public static int y(long k) {
        return (int) (k << 52 >> 52);
    }

    public static int z(long k) {
        return (int) (k << 26 >> 38);
    }
}
