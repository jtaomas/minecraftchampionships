package com.mcm.games.battlebox.arena;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Reads imported Battle Box arenas from {@code imported-arenas/<id>/} ({@code map.yml} + {@code blocks.mcmi.gz},
 * big-endian {@code "MCMI"} v1 format).
 * <p>
 * Files are treated as untrusted: size/count limits, strict block-state syntax, SnakeYAML {@link SafeConstructor}
 * without aliases, plain file names only, and banned blocks stripped. Pure Java, no Bukkit calls.
 */
public final class ImportedBbLoader {
    static final int MAX_HALF = 48;            // |local x|, |local z| limit (arena slots are 100 apart)
    static final int MIN_Y = -64, MAX_Y = 256;
    static final int MAX_BLOCKS = 2_000_000;
    static final int MAX_PALETTE = 4096;
    static final long MAX_BLOCK_FILE = 64L << 20;
    static final long MAX_YAML = 256L << 10;

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,32}");
    private static final Pattern STATE = Pattern.compile(
            "minecraft:[a-z0-9_]{1,64}(\\[[a-z0-9_]{1,32}=[a-z0-9_]{1,32}(,[a-z0-9_]{1,32}=[a-z0-9_]{1,32}){0,15}])?");
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_.-]{1,64}");

    /** Never pasted (replaced by air): they teleport, run commands, spawn mobs or build structures. */
    private static final Set<String> BANNED = Set.of(
            "minecraft:nether_portal", "minecraft:end_portal", "minecraft:end_portal_frame", "minecraft:end_gateway",
            "minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block",
            "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw", "minecraft:spawner",
            "minecraft:trial_spawner", "minecraft:vault", "minecraft:respawn_anchor", "minecraft:tnt",
            "minecraft:sculk_shrieker", "minecraft:sculk_sensor", "minecraft:calibrated_sculk_sensor");

    private ImportedBbLoader() {}

    /** Load every valid arena under {@code dir} (sorted by folder name). Broken ones are logged and skipped. */
    public static List<ImportedBbArena> loadAll(File dir, Logger log) {
        List<ImportedBbArena> out = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) return out;
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) return out;
        Arrays.sort(subs);
        Set<String> ids = new HashSet<>();
        for (File f : subs) {
            if (f.getName().startsWith(".")) continue;
            long t0 = System.nanoTime();
            try {
                ImportedBbArena a = load(f);
                if (!ids.add(a.id)) throw new IOException("duplicate id " + a.id);
                out.add(a);
                if (log != null) {
                    log.info(String.format(Locale.ROOT,
                            "Imported Battle Box arena %s (\"%s\"): %d blocks, %d palette, centre=(%d,%d,%d), spawns=(%d,%d,%d)/(%d,%d,%d), %d stripped, loaded in %d ms",
                            a.id, a.name, a.blockCount, a.palette.length,
                            a.centreX, a.centreY, a.centreZ,
                            a.spawnAX, a.spawnAY, a.spawnAZ,
                            a.spawnBX, a.spawnBY, a.spawnBZ,
                            a.removedBlocks, (System.nanoTime() - t0) / 1_000_000));
                }
            } catch (Throwable t) {
                if (log != null) log.warning("Imported Battle Box arena in " + f.getName() + " skipped: " + t.getMessage());
            }
        }
        return out;
    }

    public static ImportedBbArena load(File folder) throws IOException {
        if (!ID.matcher(folder.getName()).matches()) {
            throw new IOException("folder name must be [a-z0-9_]{1,32}");
        }
        File meta = new File(folder, "map.yml");
        if (!meta.isFile()) throw new IOException("map.yml missing");
        if (meta.length() > MAX_YAML) throw new IOException("map.yml larger than " + MAX_YAML + " bytes");
        byte[] yBytes = Files.readAllBytes(meta.toPath());
        Map<String, Object> y = parseYaml(yBytes);

        String id = str(y, "id", "").toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) throw new IOException("id must be [a-z0-9_]{1,32}");
        String name = text(str(y, "name", id), 48);
        if (name.isEmpty()) name = id;

        int[] size = vec3(y.get("size"), "size");
        if (size[0] <= 0 || size[1] <= 0 || size[2] <= 0) throw new IOException("size components must be > 0");
        int floorY = intOf(y.getOrDefault("floor_y", 0), "floor_y");
        int[] centre = vec3(y.get("centre"), "centre");
        int[] spawnA = vec3(y.get("spawn_a"), "spawn_a");
        int[] spawnB = vec3(y.get("spawn_b"), "spawn_b");
        check(centre[0], centre[1], centre[2]);
        check(spawnA[0], spawnA[1], spawnA[2]);
        check(spawnB[0], spawnB[1], spawnB[2]);

        String bfName = str(y, "blocks", "blocks.mcmi.gz");
        if (!FILE_NAME.matcher(bfName).matches() || bfName.contains("..")) {
            throw new IOException("blocks: must be a plain file name");
        }
        File bf = new File(folder, bfName);
        if (!bf.isFile()) throw new IOException("block file " + bfName + " missing");
        if (bf.length() > MAX_BLOCK_FILE) throw new IOException("block file larger than " + MAX_BLOCK_FILE + " bytes");
        byte[] bBytes = Files.readAllBytes(bf.toPath());

        Blocks b = readBlocks(bBytes);
        Blocks.Packed p = b.pack();

        return new ImportedBbArena(
                id, name, folder.getName(),
                size[0], size[1], size[2], floorY,
                centre[0], centre[1], centre[2],
                spawnA[0], spawnA[1], spawnA[2],
                spawnB[0], spawnB[1], spawnB[2],
                p.bounds, b.removed, p.keys, p.pal, p.palette);
    }

    // ------------------------------------------------------------------ keys

    static long key(int x, int y, int z) {
        return ((x + 32768L) << 32) | ((z + 32768L) << 16) | (y + 32768L);
    }

    static int kx(long k) { return (int) (k >>> 32) - 32768; }
    static int kz(long k) { return (int) ((k >>> 16) & 0xFFFF) - 32768; }
    static int ky(long k) { return (int) (k & 0xFFFF) - 32768; }

    // ------------------------------------------------------------------ block file

    private static final class Blocks {
        final List<String> palette = new ArrayList<>();
        final Map<String, Integer> palIdx = new HashMap<>();
        final Map<Long, Integer> map = new LinkedHashMap<>();
        int removed;

        int idx(String s) {
            return palIdx.computeIfAbsent(s, k -> { palette.add(k); return palette.size() - 1; });
        }

        record Packed(long[] keys, int[] pal, String[] palette, int[] bounds) {}

        Packed pack() throws IOException {
            if (map.isEmpty()) throw new IOException("no blocks");
            long[] k = new long[map.size()];
            int i = 0;
            for (long key : map.keySet()) k[i++] = key;
            Arrays.sort(k);
            int[] p = new int[k.length];
            int[] bd = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE,
                    Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
            for (i = 0; i < k.length; i++) {
                p[i] = map.get(k[i]);
                int x = kx(k[i]), y = ky(k[i]), z = kz(k[i]);
                bd[0] = Math.min(bd[0], x); bd[1] = Math.min(bd[1], y); bd[2] = Math.min(bd[2], z);
                bd[3] = Math.max(bd[3], x); bd[4] = Math.max(bd[4], y); bd[5] = Math.max(bd[5], z);
            }
            return new Packed(k, p, palette.toArray(new String[0]), bd);
        }
    }

    private static void check(int x, int y, int z) throws IOException {
        if (Math.abs(x) > MAX_HALF || Math.abs(z) > MAX_HALF || y < MIN_Y || y > MAX_Y) {
            throw new IOException("block at " + x + "," + y + "," + z
                    + " outside allowed local box (|x|,|z| <= " + MAX_HALF + ", y in " + MIN_Y + ".." + MAX_Y + ")");
        }
    }

    private static Blocks readBlocks(byte[] raw) throws IOException {
        Blocks b = new Blocks();
        try (InputStream gz = new GZIPInputStream(new ByteArrayInputStream(raw), 1 << 16);
             DataInputStream in = new DataInputStream(new LimitedStream(gz, 64L * MAX_BLOCKS))) {
            byte[] mg = new byte[4];
            in.readFully(mg);
            if (!new String(mg, StandardCharsets.US_ASCII).equals("MCMI")) {
                throw new IOException("not an MCMI block file");
            }
            int ver = in.readInt();
            if (ver != 1) throw new IOException("unsupported MCMI version " + ver);
            int np = in.readInt();
            if (np <= 0 || np > MAX_PALETTE) throw new IOException("bad palette size " + np);
            String[] pal = new String[np];
            boolean[] banned = new boolean[np];
            for (int i = 0; i < np; i++) {
                String s = in.readUTF();
                if (s.isEmpty() || s.equals("air") || s.equals("minecraft:air")
                        || s.equals("minecraft:cave_air") || s.equals("minecraft:void_air")) {
                    continue;
                }
                String f = state(s);
                String base = f.indexOf('[') < 0 ? f : f.substring(0, f.indexOf('['));
                if (BANNED.contains(base)) {
                    banned[i] = true;
                    continue;
                }
                pal[i] = f;
            }
            int n = in.readInt();
            if (n < 0 || n > MAX_BLOCKS) throw new IOException("bad block count " + n + " (max " + MAX_BLOCKS + ")");
            int[] local = new int[np];
            Arrays.fill(local, -1);
            for (int i = 0; i < n; i++) {
                int x = in.readShort(), y = in.readShort(), z = in.readShort(), pi = in.readInt();
                if (pi < 0 || pi >= np) throw new IOException("bad palette index at block " + i);
                if (banned[pi]) { b.removed++; continue; }
                if (pal[pi] == null) continue;
                check(x, y, z);
                if (local[pi] < 0) local[pi] = b.idx(pal[pi]);
                b.map.put(key(x, y, z), local[pi]);
            }
            if (in.read() != -1) throw new IOException("trailing data after the block list");
        }
        return b;
    }

    static String state(String s) throws IOException {
        if (s == null) throw new IOException("null block state");
        String f = s.trim().toLowerCase(Locale.ROOT);
        if (f.indexOf(':') < 0) f = "minecraft:" + f;
        if (f.length() > 300 || !STATE.matcher(f).matches()) {
            throw new IOException("bad block state '" + abbrev(s) + "'");
        }
        return f;
    }

    private static String abbrev(String s) {
        return s.length() > 60 ? s.substring(0, 60) + "..." : s;
    }

    static String text(String s, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < max; i++) {
            char c = s.charAt(i);
            if (c == '§' || Character.isISOControl(c)) continue;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private static final class LimitedStream extends FilterInputStream {
        private long left;
        LimitedStream(InputStream in, long limit) { super(in); left = limit; }

        @Override
        public int read() throws IOException {
            if (left <= 0) throw new IOException("block file decompresses to too much data");
            int r = super.read();
            if (r >= 0) left--;
            return r;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (left <= 0) throw new IOException("block file decompresses to too much data");
            int r = super.read(b, off, (int) Math.min(len, left));
            if (r > 0) left -= r;
            return r;
        }
    }

    // ------------------------------------------------------------------ yaml helpers

    private static Map<String, Object> parseYaml(byte[] bytes) throws IOException {
        LoaderOptions o = new LoaderOptions();
        o.setMaxAliasesForCollections(0);
        o.setAllowRecursiveKeys(false);
        o.setAllowDuplicateKeys(false);
        o.setCodePointLimit((int) MAX_YAML);
        o.setNestingDepthLimit(20);
        try {
            Object r = new Yaml(new SafeConstructor(o)).load(new String(bytes, StandardCharsets.UTF_8));
            if (!(r instanceof Map<?, ?> m)) throw new IOException("map.yml is not a mapping");
            Map<String, Object> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : m.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
            return out;
        } catch (IOException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new IOException("map.yml: " + e.getClass().getSimpleName() + ": " + abbrev(String.valueOf(e.getMessage())));
        }
    }

    private static String str(Map<String, Object> y, String k, String def) {
        Object o = y.get(k);
        return o == null ? def : String.valueOf(o);
    }

    private static int intOf(Object o, String what) throws IOException {
        if (!(o instanceof Number n) || !Double.isFinite(n.doubleValue()) || Math.abs(n.doubleValue()) > 100_000) {
            throw new IOException(what + ": number expected");
        }
        return (int) Math.floor(n.doubleValue());
    }

    private static int[] vec3(Object o, String what) throws IOException {
        if (!(o instanceof List<?> l) || l.size() != 3) throw new IOException(what + " must be [x,y,z]");
        return new int[]{intOf(l.get(0), what), intOf(l.get(1), what), intOf(l.get(2), what)};
    }
}
