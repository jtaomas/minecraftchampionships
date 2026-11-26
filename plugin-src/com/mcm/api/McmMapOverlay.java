package com.mcm.api;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The saved result of a Map Edit session (Map Edit core): every block of a map that differs from what the game's own
 * builder pastes, as vanilla block-state strings in the map's LOCAL coordinates (world position minus the origin the
 * game pastes the map at), so it applies to any slot / arena copy and survives plugin updates and map regeneration.
 *
 * <p>A game applies it <b>after</b> its own build, before its own game logic needs the blocks:
 * <pre>{@code
 * McmMapOverlay ov = McmMapOverlay.of(plugin, mapId);        // null = no overlay (cached, cheap to call every build)
 * if (ov != null) ov.forEach((x, y, z, state) -> mySink.set(ox + x, oy + y, oz + z, state));
 * }</pre>
 * The overlay is always the full difference from the original, so applying it twice is harmless. Air is the state
 * {@code "minecraft:air"}. File: {@code <game plugin data folder>/mapedits/<mapId>.overlay.gz}.
 *
 * <p>Wrap calls in {@code try/catch (Throwable)} (or {@link LinkageError}) in a game that must also run on an older core.
 * @since the Map Edit core update
 */
public final class McmMapOverlay {
    /** Current file format. A core refuses (and logs) files of a newer format and the game plays the original map. */
    public static final int FORMAT = 1;
    private static final int MAGIC = 0x4D434D4F; // "MCMO"
    /** Hard sanity cap when reading (the editor enforces its own, lower, cap when saving). */
    public static final int MAX_READ = 4_000_000;

    private static final Logger LOG = Logger.getLogger("McmMapOverlay");
    private static final Map<String, McmMapOverlay> CACHE = new ConcurrentHashMap<>();

    @FunctionalInterface
    public interface Visitor { void block(int lx, int ly, int lz, String state); }

    public final String gameId;
    public final String mapId;
    public final String editor;
    public final long savedAt;
    private final int[] xs, ys, zs;
    private final int[] idx;
    private final String[] palette;
    private int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
    private int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

    private McmMapOverlay(String gameId, String mapId, String editor, long savedAt, int[] xs, int[] ys, int[] zs, int[] idx, String[] palette) {
        this.gameId = gameId; this.mapId = mapId; this.editor = editor; this.savedAt = savedAt;
        this.xs = xs; this.ys = ys; this.zs = zs; this.idx = idx; this.palette = palette;
        for (int i = 0; i < xs.length; i++) {
            minX = Math.min(minX, xs[i]); maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]); maxY = Math.max(maxY, ys[i]);
            minZ = Math.min(minZ, zs[i]); maxZ = Math.max(maxZ, zs[i]);
        }
    }

    public int size() { return xs.length; }
    public boolean isEmpty() { return xs.length == 0; }

    /** Inclusive local bounds {minX, minY, minZ, maxX, maxY, maxZ}; null when empty. */
    public int[] bounds() { return xs.length == 0 ? null : new int[]{minX, minY, minZ, maxX, maxY, maxZ}; }

    public void forEach(Visitor v) {
        for (int i = 0; i < xs.length; i++) v.block(xs[i], ys[i], zs[i], palette[idx[i]]);
    }

    /** Block-state string at a local position, or null if the overlay does not touch it. Linear; build {@link #toMap()} for many lookups. */
    public String get(int lx, int ly, int lz) {
        for (int i = 0; i < xs.length; i++) if (xs[i] == lx && ys[i] == ly && zs[i] == lz) return palette[idx[i]];
        return null;
    }

    /** Packed key (x 26 bits, z 26 bits, y 12 bits: same packing as the TGTTOS sparse store) to state. */
    public Map<Long, String> toMap() {
        Map<Long, String> m = new HashMap<>(Math.max(16, xs.length * 4 / 3));
        for (int i = 0; i < xs.length; i++) m.put(key(xs[i], ys[i], zs[i]), palette[idx[i]]);
        return m;
    }

    public static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static boolean isAir(String state) {
        return state == null || state.equals("minecraft:air") || state.equals("air")
                || state.equals("minecraft:cave_air") || state.equals("minecraft:void_air");
    }

    /**
     * Convenience: write every entry into {@code w} at origin (ox, oy, oz) without physics. Main thread, chunks must be
     * loaded. For small overlays or tools; big maps should use their own tick-budgeted pasters with {@link #forEach}.
     * @return blocks written
     */
    public int applyTo(World w, int ox, int oy, int oz) {
        Map<String, BlockData> cache = new HashMap<>();
        int n = 0, minH = w.getMinHeight(), maxH = w.getMaxHeight();
        for (int i = 0; i < xs.length; i++) {
            int y = oy + ys[i];
            if (y < minH || y >= maxH) continue;
            BlockData d = cache.computeIfAbsent(palette[idx[i]], s -> {
                try { return Bukkit.createBlockData(s); } catch (IllegalArgumentException e) { return null; }
            });
            if (d == null) continue;
            w.getBlockAt(ox + xs[i], y, oz + zs[i]).setBlockData(d, false);
            n++;
        }
        return n;
    }

    // ------------------------------------------------------------------ files

    /** {@code <owner data folder>/mapedits/<mapId>.overlay.gz} */
    public static File file(Plugin owner, String mapId) {
        return new File(new File(owner.getDataFolder(), "mapedits"), safeId(mapId) + ".overlay.gz");
    }

    /** Lower-case file-safe form of a map id. */
    public static String safeId(String id) {
        String s = id == null ? "map" : id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        return s.isEmpty() || s.startsWith(".") ? "map_" + s : s;
    }

    /** The overlay of a map of {@code owner}, or null if none / unreadable. Cached by file time and size. */
    public static McmMapOverlay of(Plugin owner, String mapId) { return load(file(owner, mapId)); }

    /** Load (cached by file time and size). Null if the file is missing, corrupt or of a newer format (a warning is logged). */
    public static McmMapOverlay load(File f) {
        if (f == null || !f.isFile()) { if (f != null) CACHE.remove(f.getAbsolutePath()); return null; }
        String key = f.getAbsolutePath();
        long stamp = f.lastModified() * 31 + f.length();
        McmMapOverlay c = CACHE.get(key);
        if (c != null && c.stamp == stamp) return c;
        try {
            McmMapOverlay o = read(f);
            o.stamp = stamp;
            CACHE.put(key, o);
            return o;
        } catch (Throwable t) {
            LOG.log(Level.WARNING, "Map overlay " + f + " ignored (the original map is used): " + t);
            CACHE.remove(key);
            return null;
        }
    }

    private long stamp;

    /** Forget cached copies (after a save/reset). */
    public static void invalidate(File f) { if (f != null) CACHE.remove(f.getAbsolutePath()); }

    private static McmMapOverlay read(File f) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new BufferedInputStream(new FileInputStream(f)))))) {
            if (in.readInt() != MAGIC) throw new IOException("not a map overlay file");
            int fmt = in.readInt();
            if (fmt > FORMAT) throw new IOException("format " + fmt + " is newer than this core supports (" + FORMAT + ")");
            String game = in.readUTF(), map = in.readUTF(), editor = in.readUTF();
            long at = in.readLong();
            int pn = in.readInt();
            if (pn < 0 || pn > 70000) throw new IOException("bad palette size " + pn);
            String[] pal = new String[pn];
            for (int i = 0; i < pn; i++) pal[i] = in.readUTF();
            int n = in.readInt();
            if (n < 0 || n > MAX_READ) throw new IOException("bad entry count " + n);
            int[] xs = new int[n], ys = new int[n], zs = new int[n], idx = new int[n];
            int x = 0, y = 0, z = 0;
            for (int i = 0; i < n; i++) {
                y += unzig(readVar(in));
                z += unzig(readVar(in));
                x += unzig(readVar(in));
                int p = readVar(in);
                if (p >= pn) throw new IOException("bad palette index");
                xs[i] = x; ys[i] = y; zs[i] = z; idx[i] = p;
            }
            if (in.readInt() != MAGIC) throw new IOException("truncated");
            return new McmMapOverlay(game, map, editor, at, xs, ys, zs, idx, pal);
        }
    }

    /** Write atomically (temp file, then move). Callers that replace an existing overlay back it up first. */
    public void write(File f) throws IOException {
        File dir = f.getAbsoluteFile().getParentFile();
        if (dir != null) dir.mkdirs();
        File tmp = new File(f.getPath() + ".tmp");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(tmp)), 1 << 16)))) {
            out.writeInt(MAGIC);
            out.writeInt(FORMAT);
            out.writeUTF(gameId == null ? "" : gameId);
            out.writeUTF(mapId == null ? "" : mapId);
            out.writeUTF(editor == null ? "" : editor);
            out.writeLong(savedAt);
            out.writeInt(palette.length);
            for (String s : palette) out.writeUTF(s);
            out.writeInt(xs.length);
            int x = 0, y = 0, z = 0;
            for (int i = 0; i < xs.length; i++) {
                writeVar(out, zig(ys[i] - y));
                writeVar(out, zig(zs[i] - z));
                writeVar(out, zig(xs[i] - x));
                writeVar(out, idx[i]);
                x = xs[i]; y = ys[i]; z = zs[i];
            }
            out.writeInt(MAGIC);
        } catch (IOException | RuntimeException e) {
            tmp.delete();
            throw e;
        }
        try {
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        invalidate(f);
    }

    private static int zig(int v) { return (v << 1) ^ (v >> 31); }
    private static int unzig(int v) { return (v >>> 1) ^ -(v & 1); }

    private static void writeVar(DataOutputStream out, int v) throws IOException {
        if (v < 0) throw new IOException("negative varint (entries must be sorted)");
        while ((v & ~0x7F) != 0) { out.writeByte((v & 0x7F) | 0x80); v >>>= 7; }
        out.writeByte(v);
    }

    private static int readVar(DataInputStream in) throws IOException {
        int r = 0, shift = 0;
        while (true) {
            int b = in.readUnsignedByte();
            r |= (b & 0x7F) << shift;
            if ((b & 0x80) == 0) return r;
            shift += 7;
            if (shift > 35) throw new IOException("bad varint");
        }
    }

    // ------------------------------------------------------------------ building

    /** Collects entries (any order; a later entry for the same position wins) and builds a sorted overlay. */
    public static final class Builder {
        private final Map<Long, String> m = new HashMap<>();

        public Builder add(int lx, int ly, int lz, String state) {
            m.put(key(lx, ly, lz), state == null || state.isEmpty() ? "minecraft:air" : (state.indexOf(':') < 0 ? "minecraft:" + state : state));
            return this;
        }

        public int size() { return m.size(); }

        public McmMapOverlay build(String gameId, String mapId, String editor) {
            int n = m.size();
            long[] order = new long[n];
            int[][] cell = new int[n][];
            int i = 0;
            List<String> pal = new ArrayList<>();
            Map<String, Integer> pi = new HashMap<>();
            for (Map.Entry<Long, String> e : m.entrySet()) {
                long k = e.getKey();
                int x = (int) (k >> 38) << 6 >> 6, z = (int) ((k >> 12) & 0x3FFFFFF) << 6 >> 6, y = (int) (k & 0xFFF) << 20 >> 20;
                Integer p = pi.get(e.getValue());
                if (p == null) { p = pal.size(); pal.add(e.getValue()); pi.put(e.getValue(), p); }
                cell[i] = new int[]{x, y, z, p};
                i++;
            }
            Integer[] ord = new Integer[n];
            for (int j = 0; j < n; j++) ord[j] = j;
            Arrays.sort(ord, (a, b) -> {
                int[] A = cell[a], B = cell[b];
                if (A[1] != B[1]) return Integer.compare(A[1], B[1]);
                if (A[2] != B[2]) return Integer.compare(A[2], B[2]);
                return Integer.compare(A[0], B[0]);
            });
            int[] xs = new int[n], ys = new int[n], zs = new int[n], idx = new int[n];
            for (int j = 0; j < n; j++) {
                int[] c = cell[ord[j]];
                xs[j] = c[0]; ys[j] = c[1]; zs[j] = c[2]; idx[j] = c[3];
            }
            return new McmMapOverlay(gameId, mapId, editor, System.currentTimeMillis(), xs, ys, zs, idx, pal.toArray(new String[0]));
        }
    }
}
