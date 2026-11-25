package com.mcm.games.rocketspleef.map;

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
 * Loads imported Rocket Spleef Rush arenas from {@code imported-arenas/<id>/}
 * ({@code map.yml} + {@code blocks.mcmi.gz} in MCMI v1 format).
 * Pure Java, no Bukkit calls, safe for startup or off-thread loading.
 */
public final class ImportedRsLoader {
    static final int MAX_HALF = 160;
    static final int MIN_LOCAL_Y = -128;
    static final int MAX_LOCAL_Y = 320;
    static final int MAX_BLOCKS = 2_000_000;
    static final int MAX_PALETTE = 4096;
    static final long MAX_BLOCK_FILE = 64L << 20;
    static final long MAX_YAML = 256L << 10;

    private static final Pattern ID = Pattern.compile("[a-z0-9_]{1,32}");
    private static final Pattern STATE = Pattern.compile(
            "minecraft:[a-z0-9_]{1,64}(\\[[a-z0-9_]{1,32}=[a-z0-9_]{1,32}(,[a-z0-9_]{1,32}=[a-z0-9_]{1,32}){0,15}])?");
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9_.-]{1,64}");

    private static final Set<String> BANNED = Set.of(
            "minecraft:nether_portal", "minecraft:end_portal", "minecraft:end_portal_frame", "minecraft:end_gateway",
            "minecraft:command_block", "minecraft:chain_command_block", "minecraft:repeating_command_block",
            "minecraft:structure_block", "minecraft:structure_void", "minecraft:jigsaw", "minecraft:spawner",
            "minecraft:trial_spawner", "minecraft:vault", "minecraft:respawn_anchor", "minecraft:tnt",
            "minecraft:sculk_shrieker", "minecraft:sculk_sensor", "minecraft:calibrated_sculk_sensor"
    );

    private ImportedRsLoader() {}

    /**
     * Scans {@code dir} for subdirectories containing {@code map.yml} and {@code blocks.mcmi.gz}.
     * Broken arenas are logged and skipped.
     */
    public static List<ImportedRsArena> loadAll(File dir, Logger log) {
        List<ImportedRsArena> out = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) return out;
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) return out;
        Arrays.sort(subs);
        Set<String> ids = new HashSet<>();
        for (File f : subs) {
            if (f.getName().startsWith(".")) continue;
            long t0 = System.nanoTime();
            try {
                ImportedRsArena arena = load(f);
                if (!ids.add(arena.id())) {
                    throw new IOException("duplicate id " + arena.id());
                }
                out.add(arena);
                if (log != null) {
                    log.info(String.format(Locale.ROOT,
                            "Imported RocketSpleef arena %s (\"%s\"): %d blocks, %d spawns, base_y=%d, %d removed, loaded in %d ms",
                            arena.id(), arena.displayName(), arena.blockCount(), arena.spawnCount(),
                            arena.baseY(), arena.removedBlocks(), (System.nanoTime() - t0) / 1_000_000));
                }
            } catch (Throwable t) {
                if (log != null) {
                    log.warning("Imported RocketSpleef arena in " + f.getName() + " skipped: " + t.getMessage());
                }
            }
        }
        return out;
    }

    public static ImportedRsArena load(File folder) throws IOException {
        if (!ID.matcher(folder.getName()).matches()) {
            throw new IOException("folder name must be [a-z0-9_]{1,32}");
        }
        File meta = new File(folder, "map.yml");
        if (!meta.isFile()) throw new IOException("map.yml missing");
        if (meta.length() > MAX_YAML) throw new IOException("map.yml larger than " + MAX_YAML + " bytes");

        Map<String, Object> y = parseYaml(Files.readAllBytes(meta.toPath()));

        String id = str(y, "id", "").toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) throw new IOException("id must be [a-z0-9_]{1,32}");

        String name = text(str(y, "name", id), 48);
        if (name.isEmpty()) name = id;

        int baseY = intOf(y.getOrDefault("base_y", 78), "base_y");
        if (baseY < -64 || baseY > 256) throw new IOException("base_y out of range: " + baseY);

        int[] size = intList(y.get("size"), 3, "size");

        Object spawnsObj = y.get("spawns");
        if (!(spawnsObj instanceof List<?> rawSpawns) || rawSpawns.isEmpty()) {
            throw new IOException("spawns: must be a non-empty list");
        }
        List<int[]> spawns = new ArrayList<>(rawSpawns.size());
        for (Object entry : rawSpawns) {
            int[] sp = intList(entry, 3, "spawns[]");
            spawns.add(sp);
        }

        String bfName = str(y, "blocks", "blocks.mcmi.gz");
        if (!FILE_NAME.matcher(bfName).matches() || bfName.contains("..")) {
            throw new IOException("blocks: must be a plain file name");
        }
        File bf = new File(folder, bfName);
        if (!bf.isFile()) throw new IOException("block file " + bfName + " missing");
        if (bf.length() > MAX_BLOCK_FILE) throw new IOException("block file larger than " + MAX_BLOCK_FILE + " bytes");

        byte[] rawBlocks = Files.readAllBytes(bf.toPath());
        ParsedBlocks pb = readBlocks(rawBlocks);
        if (pb.count == 0) throw new IOException("arena has no blocks");

        return new ImportedRsArena(
                id, name, baseY, size, spawns,
                pb.palette, pb.xs, pb.ys, pb.zs, pb.palIdx,
                pb.removed, pb.maxHorizRadius
        );
    }

    private record ParsedBlocks(
            String[] palette,
            short[] xs, short[] ys, short[] zs,
            int[] palIdx,
            int count,
            int removed,
            int maxHorizRadius
    ) {}

    private static ParsedBlocks readBlocks(byte[] raw) throws IOException {
        List<String> outPalette = new ArrayList<>();
        Map<String, Integer> palMap = new HashMap<>();
        int removed = 0;
        int maxHoriz = 0;

        short[] xs;
        short[] ys;
        short[] zs;
        int[] palIdx;
        int count = 0;

        try (InputStream gz = new GZIPInputStream(new ByteArrayInputStream(raw), 1 << 16);
             DataInputStream in = new DataInputStream(new LimitedStream(gz, 64L * MAX_BLOCKS))) {
            byte[] magic = new byte[4];
            in.readFully(magic);
            if (!new String(magic, StandardCharsets.US_ASCII).equals("MCMI")) {
                throw new IOException("not an MCMI block file");
            }
            int ver = in.readInt();
            if (ver != 1) throw new IOException("unsupported MCMI version " + ver);

            int np = in.readInt();
            if (np <= 0 || np > MAX_PALETTE) throw new IOException("bad palette size " + np);
            String[] rawPal = new String[np];
            boolean[] banned = new boolean[np];
            for (int i = 0; i < np; i++) {
                String s = in.readUTF();
                if (s.isEmpty() || s.equals("air") || s.equals("minecraft:air")
                        || s.equals("minecraft:cave_air") || s.equals("minecraft:void_air")) {
                    continue;
                }
                String norm = state(s);
                int br = norm.indexOf('[');
                String base = br < 0 ? norm : norm.substring(0, br);
                if (BANNED.contains(base)) {
                    banned[i] = true;
                    continue;
                }
                rawPal[i] = norm;
            }

            int n = in.readInt();
            if (n < 0 || n > MAX_BLOCKS) throw new IOException("bad block count " + n);

            xs = new short[n];
            ys = new short[n];
            zs = new short[n];
            palIdx = new int[n];
            int[] remapped = new int[np];
            Arrays.fill(remapped, -1);

            for (int i = 0; i < n; i++) {
                int x = in.readShort();
                int y = in.readShort();
                int z = in.readShort();
                int pi = in.readInt();
                if (pi < 0 || pi >= np) throw new IOException("bad palette index at block " + i);
                if (banned[pi]) {
                    removed++;
                    continue;
                }
                if (rawPal[pi] == null) continue;
                if (Math.abs(x) > MAX_HALF || Math.abs(z) > MAX_HALF || y < MIN_LOCAL_Y || y > MAX_LOCAL_Y) {
                    removed++;
                    continue;
                }

                int mapped = remapped[pi];
                if (mapped < 0) {
                    mapped = palMap.computeIfAbsent(rawPal[pi], k -> {
                        outPalette.add(k);
                        return outPalette.size() - 1;
                    });
                    remapped[pi] = mapped;
                }
                xs[count] = (short) x;
                ys[count] = (short) y;
                zs[count] = (short) z;
                palIdx[count] = mapped;
                count++;
                maxHoriz = Math.max(maxHoriz, Math.max(Math.abs(x), Math.abs(z)));
            }

            if (in.read() != -1) throw new IOException("trailing data after the block list");
        }

        return new ParsedBlocks(
                outPalette.toArray(new String[0]),
                Arrays.copyOf(xs, count),
                Arrays.copyOf(ys, count),
                Arrays.copyOf(zs, count),
                Arrays.copyOf(palIdx, count),
                count,
                removed,
                maxHoriz
        );
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
            for (Map.Entry<?, ?> e : m.entrySet()) {
                out.put(String.valueOf(e.getKey()), e.getValue());
            }
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

    private static int[] intList(Object o, int expectedLen, String what) throws IOException {
        if (!(o instanceof List<?> l) || l.size() != expectedLen) {
            throw new IOException(what + " must be a list of " + expectedLen + " integers");
        }
        int[] out = new int[expectedLen];
        for (int i = 0; i < expectedLen; i++) {
            out[i] = intOf(l.get(i), what);
        }
        return out;
    }

    private static final class LimitedStream extends FilterInputStream {
        private long left;

        LimitedStream(InputStream in, long limit) {
            super(in);
            this.left = limit;
        }

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
}
