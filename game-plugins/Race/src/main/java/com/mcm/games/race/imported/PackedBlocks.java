package com.mcm.games.race.imported;

import com.mcm.games.race.model.TgMapData;
import com.mcm.games.race.model.TgMapData.SparseBlocks;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Compact read-mostly block store for imported maps (hundreds of thousands to a few million blocks): sorted packed
 * keys + palette indices, about 12 bytes per block instead of ~100 for {@link SparseBlocks}. Writes after loading go to
 * a small overlay map (the runtime does not normally write map blocks; the importer adds start/pen blocks before
 * {@link #seal}).
 */
public final class PackedBlocks implements TgMapData.Blocks {
    private long[] keys = new long[0];
    private int[] pal = new int[0];
    private String[] palette = new String[0];
    private int n;
    /** Overlay: key -> state, null value = removed (air). */
    private final Map<Long, String> overlay = new HashMap<>();
    private int overlayAdds;

    /** Build from parallel arrays (keys need not be sorted; later duplicates win). */
    static PackedBlocks of(long[] k, int[] p, String[] palette, int count) {
        PackedBlocks b = new PackedBlocks();
        Integer[] order = new Integer[count];
        for (int i = 0; i < count; i++) order[i] = i;
        // sort indices by key, stable so a later duplicate stays after an earlier one
        Arrays.sort(order, (a, c) -> Long.compare(k[a], k[c]));
        long[] ks = new long[count];
        int[] ps = new int[count];
        int m = 0;
        for (int i = 0; i < count; i++) {
            int j = order[i];
            if (m > 0 && ks[m - 1] == k[j]) { ps[m - 1] = p[j]; continue; }
            ks[m] = k[j]; ps[m] = p[j]; m++;
        }
        b.keys = m == count ? ks : Arrays.copyOf(ks, m);
        b.pal = m == count ? ps : Arrays.copyOf(ps, m);
        b.palette = palette;
        b.n = m;
        return b;
    }

    private int find(long k) { return Arrays.binarySearch(keys, 0, n, k); }

    @Override public void set(int x, int y, int z, String blockData) {
        long k = SparseBlocks.key(x, y, z);
        String f = SparseBlocks.normalize(blockData);
        boolean base = find(k) >= 0;
        if (f == null && !base) { overlay.remove(k); return; }
        overlay.put(k, f);
        overlayAdds++;
    }

    @Override public String get(int x, int y, int z) {
        long k = SparseBlocks.key(x, y, z);
        if (!overlay.isEmpty() && overlay.containsKey(k)) return overlay.get(k);
        int i = find(k);
        return i < 0 ? null : palette[pal[i]];
    }

    @Override public void forEach(TgMapData.Visitor v) {
        boolean ov = !overlay.isEmpty();
        for (int i = 0; i < n; i++) {
            long k = keys[i];
            if (ov && overlay.containsKey(k)) continue;
            v.block(SparseBlocks.kx(k), SparseBlocks.ky(k), SparseBlocks.kz(k), palette[pal[i]]);
        }
        if (ov) for (Map.Entry<Long, String> e : overlay.entrySet()) {
            if (e.getValue() == null) continue;
            long k = e.getKey();
            v.block(SparseBlocks.kx(k), SparseBlocks.ky(k), SparseBlocks.kz(k), e.getValue());
        }
    }

    @Override public int size() {
        if (overlay.isEmpty()) return n;
        int s = n;
        for (Map.Entry<Long, String> e : overlay.entrySet()) {
            boolean base = find(e.getKey()) >= 0;
            if (base && e.getValue() == null) s--;
            else if (!base && e.getValue() != null) s++;
        }
        return s;
    }

    /** Fold the overlay into the packed arrays (call once after the importer's own additions). */
    void seal() {
        if (overlay.isEmpty()) return;
        Map<String, Integer> pi = new HashMap<>();
        for (int i = 0; i < palette.length; i++) pi.putIfAbsent(palette[i], i);
        long[] k2 = Arrays.copyOf(keys, n + overlay.size());
        int[] p2 = Arrays.copyOf(pal, n + overlay.size());
        String[] pal2 = Arrays.copyOf(palette, palette.length + overlay.size());
        int pn = palette.length, m = n;
        boolean[] dead = new boolean[n + overlay.size()];
        for (Map.Entry<Long, String> e : overlay.entrySet()) {
            int i = find(e.getKey());
            if (e.getValue() == null) { if (i >= 0) dead[i] = true; continue; }
            Integer idx = pi.get(e.getValue());
            if (idx == null) { idx = pn; pal2[pn++] = e.getValue(); pi.put(e.getValue(), idx); }
            if (i >= 0) p2[i] = idx;
            else { k2[m] = e.getKey(); p2[m] = idx; m++; }
        }
        int c = 0;
        for (int i = 0; i < m; i++) if (!dead[i]) { k2[c] = k2[i]; p2[c] = p2[i]; c++; }
        PackedBlocks b = of(k2, p2, Arrays.copyOf(pal2, pn), c);
        keys = b.keys; pal = b.pal; palette = b.palette; n = b.n;
        overlay.clear();
    }
}
