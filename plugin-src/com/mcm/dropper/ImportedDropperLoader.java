package com.mcm.dropper;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

/**
 * Reads imported dropper shafts from {@code imported-droppers/<id>/} (map.yml + blocks.mcmi.gz, the format of the
 * game plugins' importer: gzip, big-endian {@code "MCMI"}, int version 1, int paletteSize, paletteSize x UTF
 * block-state strings, int blockCount, blockCount x (short x, short y, short z, int paletteIndex), map-local).
 * <p>
 * The files are third-party data and treated as untrusted: size and count limits, a strict block-state syntax, a safe
 * YAML parser with no aliases, plain file names only; nothing in them is ever executed. Pure Java, no Bukkit calls
 * (runs on a worker thread; block states are resolved later on the main thread).
 * <p>
 * Changes made for the Multi-Stage Dropper (the files are written for TGTTOS):
 * <ul>
 *   <li>{@code start.drop-floor} is not built: the player is put in the air above the shaft and drops at once;
 *       {@code start.barrier} walls are built (as in TGTTOS).</li>
 *   <li>{@code goal} / {@code pen} (the chicken finish) is not built: landing in the bottom pool completes the stage.</li>
 *   <li>The exit opening from the pool into the finish hall is sealed with barrier blocks: every empty cell right
 *       around the pool's top layer, from the water surface up 8 blocks.</li>
 *   <li>Portals, command/structure blocks, spawners and similar are not pasted.</li>
 * </ul>
 */
public final class ImportedDropperLoader {
    /** Bump when the transformations above change, so already pasted shafts are pasted again. */
    static final String LOADER_VERSION = "dropper-loader-1";
    static final int MAX_HALF = 120;            // |local x|, |local z| limit (slots are 300 apart)
    static final int MIN_Y = -400, MAX_Y = 400;
    static final int MAX_BLOCKS = 2_000_000;
    static final int MAX_PALETTE = 4096;
    static final long MAX_BLOCK_FILE = 64L << 20;
    static final long MAX_YAML = 256L << 10;
    static final int MAX_BOX_CELLS = 2_000_000;
    static final int SEAL_HEIGHT = 8;
    static final String SEAL_BLOCK = "minecraft:barrier";

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

    private ImportedDropperLoader() {}

    /** Every valid shaft under {@code dir} (sorted by folder name). Broken ones are logged and skipped. */
    public static List<ImportedDropper> loadAll(File dir, Logger log) {
        List<ImportedDropper> out = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) return out;
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) return out;
        Arrays.sort(subs);
        Set<String> ids = new HashSet<>();
        for (File f : subs) {
            if (f.getName().startsWith(".")) continue;
            long t0 = System.nanoTime();
            try {
                ImportedDropper d = load(f);
                if (!ids.add(d.id)) throw new IOException("duplicate id " + d.id);
                out.add(d);
                if (log != null) log.info(String.format(Locale.ROOT,
                        "Imported dropper %s (\"%s\", by %s): %d blocks, pool %d water y %d..%d, %d exit-seal barriers, %d removed, loaded in %d ms",
                        d.id, d.name, d.credit, d.blockCount, d.poolWater, d.poolBottomY, d.poolTopY, d.sealBlocks, d.removedBlocks,
                        (System.nanoTime() - t0) / 1_000_000));
            } catch (Throwable t) {
                if (log != null) log.warning("Imported dropper in " + f.getName() + " skipped: " + t.getMessage());
            }
        }
        return out;
    }

    public static ImportedDropper load(File folder) throws IOException {
        if (!ID.matcher(folder.getName()).matches()) throw new IOException("folder name must be [a-z0-9_]{1,32}");
        File meta = new File(folder, "map.yml");
        if (!meta.isFile()) throw new IOException("map.yml missing");
        if (meta.length() > MAX_YAML) throw new IOException("map.yml larger than " + MAX_YAML + " bytes");
        byte[] yBytes = Files.readAllBytes(meta.toPath());
        Map<String, Object> y = parseYaml(yBytes);

        String type = str(y, "type", "");
        if (!type.equalsIgnoreCase("drop")) throw new IOException("type is '" + type + "', not a dropper map (type: drop)");
        String id = str(y, "id", "").toLowerCase(Locale.ROOT);
        if (!ID.matcher(id).matches()) throw new IOException("id must be [a-z0-9_]{1,32}");
        String name = text(str(y, "name", id), 48);
        String credit = text(str(y, "credit", "unknown"), 48);
        if (name.isEmpty()) name = id;
        if (credit.isEmpty()) credit = "unknown";

        String bfName = str(y, "blocks", "blocks.mcmi.gz");
        if (!FILE_NAME.matcher(bfName).matches() || bfName.contains("..")) throw new IOException("blocks: must be a plain file name");
        File bf = new File(folder, bfName);
        if (!bf.isFile()) throw new IOException("block file " + bfName + " missing");
        if (bf.length() > MAX_BLOCK_FILE) throw new IOException("block file larger than " + MAX_BLOCK_FILE + " bytes");
        byte[] bBytes = Files.readAllBytes(bf.toPath());

        Blocks b = readBlocks(bBytes);
        int removed = b.removed;

        // ---- our own additions from map.yml (clear first, then fills)
        for (int[] box : boxes(y.get("clear"), "clear")) fill(b, box, null);
        Object addO = y.get("add");
        if (addO != null) {
            if (!(addO instanceof List<?> adds)) throw new IOException("add: must be a list");
            for (Object o : adds) {
                if (!(o instanceof List<?> l) || l.size() != 7) throw new IOException("add entries are [x1,y1,z1,x2,y2,z2,block]");
                int[] box = new int[6];
                for (int i = 0; i < 6; i++) box[i] = intOf(l.get(i), "add");
                fill(b, box, state(String.valueOf(l.get(6))));
            }
        }

        // ---- start strip (feet layer = minY) and its glass walls; the drop floor is left out on purpose
        Map<String, Object> st = section(y, "start");
        if (st == null) throw new IOException("start: section missing");
        int[] strip = box6(st.get("strip"), "start.strip");
        float yaw = (float) dbl(st.get("yaw"), 0);
        String barrierBlock = state(str(st, "barrier-block", "minecraft:barrier"));
        int barriers = 0;
        for (int[] box : boxes(st.get("barrier"), "start.barrier")) {
            int[] n = norm(box);
            for (int xx = n[0]; xx <= n[3]; xx++) for (int yy = n[1]; yy <= n[4]; yy++) for (int zz = n[2]; zz <= n[5]; zz++)
                if (b.get(xx, yy, zz) == null) { b.set(xx, yy, zz, barrierBlock); barriers++; }
        }
        int[] s = norm(strip);
        List<double[]> spawns = new ArrayList<>();
        double cx = (s[0] + s[3]) / 2.0, cz = (s[2] + s[5]) / 2.0;
        spawns.add(new double[]{Math.floor(cx) + 0.5, s[1], Math.floor(cz) + 0.5});
        for (int xx = s[0]; xx <= s[3]; xx++) for (int zz = s[2]; zz <= s[5]; zz++)
            if (xx != (int) Math.floor(cx) || zz != (int) Math.floor(cz)) spawns.add(new double[]{xx + 0.5, s[1], zz + 0.5});

        // ---- the pool: the water at the bottom of the shaft (lowest water layer + up to 7 more)
        b.flat();
        int minWaterY = Integer.MAX_VALUE;
        for (int i = 0; i < b.n; i++) if (b.isWater(i)) minWaterY = Math.min(minWaterY, b.y(i));
        if (minWaterY == Integer.MAX_VALUE) throw new IOException("no water pool at the bottom of the shaft");
        int pMinX = Integer.MAX_VALUE, pMaxX = Integer.MIN_VALUE, pMinZ = Integer.MAX_VALUE, pMaxZ = Integer.MIN_VALUE, pTop = minWaterY, water = 0;
        for (int i = 0; i < b.n; i++) {
            if (!b.isWater(i) || b.y(i) > minWaterY + 7) continue;
            water++;
            pMinX = Math.min(pMinX, b.x(i)); pMaxX = Math.max(pMaxX, b.x(i));
            pMinZ = Math.min(pMinZ, b.z(i)); pMaxZ = Math.max(pMaxZ, b.z(i));
            pTop = Math.max(pTop, b.y(i));
        }
        if (water < 16) throw new IOException("pool too small (" + water + " water blocks)");
        if (pTop >= s[1] - 16) throw new IOException("pool is not below the start");

        // ---- seal: barriers in every empty cell next to the pool's top layer, surface .. surface+SEAL_HEIGHT
        Set<Long> top = new HashSet<>();
        for (int i = 0; i < b.n; i++) if (b.isWater(i) && b.y(i) == pTop) top.add(col(b.x(i), b.z(i)));
        Set<Long> ring = new HashSet<>();
        for (long c : top) {
            int x = colX(c), z = colZ(c);
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                long n = col(x + dx, z + dz);
                if (!top.contains(n)) ring.add(n);
            }
        }
        int seal = 0;
        for (long c : ring) for (int yy = pTop; yy <= pTop + SEAL_HEIGHT; yy++) {
            int x = colX(c), z = colZ(c);
            if (b.get(x, yy, z) == null) { b.set(x, yy, z, SEAL_BLOCK); seal++; }
        }

        // ---- the stage start: the column nearest the strip centre with at least 8 blocks of open fall under it
        //      (no drop floor here, so a canopy right under the centre would leave the player standing on it)
        for (int i = 0; i < spawns.size(); i++) {
            double[] c = spawns.get(i);
            int x = (int) Math.floor(c[0]), z = (int) Math.floor(c[2]), fy = (int) c[1];
            boolean open = true;
            for (int yy = fy - 1; yy >= fy - 8 && open; yy--) {
                String bl = b.get(x, yy, z);
                if (bl != null && !bl.equals("minecraft:light") && !bl.startsWith("minecraft:light[")) open = false;
            }
            if (!open) continue;
            if (i > 0) {
                double ccx = Math.floor(cx) + 0.5, ccz = Math.floor(cz) + 0.5;
                int best = i;
                for (int j = i + 1; j < spawns.size(); j++) {
                    double[] o = spawns.get(j);
                    int ox = (int) Math.floor(o[0]), oz = (int) Math.floor(o[2]);
                    boolean op = true;
                    for (int yy = fy - 1; yy >= fy - 8 && op; yy--) {
                        String bl = b.get(ox, yy, oz);
                        if (bl != null && !bl.equals("minecraft:light") && !bl.startsWith("minecraft:light[")) op = false;
                    }
                    double dj = Math.hypot(o[0] - ccx, o[2] - ccz), db = Math.hypot(spawns.get(best)[0] - ccx, spawns.get(best)[2] - ccz);
                    if (op && dj < db) best = j;
                }
                double[] t = spawns.get(0); spawns.set(0, spawns.get(best)); spawns.set(best, t);
            }
            break;
        }

        // ---- rules
        int lethal = intOf(y.getOrDefault("lethal-fall", 0), "lethal-fall");
        if (lethal < 0 || lethal > 400) throw new IOException("lethal-fall out of range");
        Blocks.Packed p = b.pack();
        int[] bounds = p.bounds;
        int voidY = intOf(y.getOrDefault("void-y", bounds[1] - 2), "void-y");
        int[] play = y.get("play-bounds") != null ? order(box6(y.get("play-bounds"), "play-bounds"))
                : new int[]{bounds[0] - 2, bounds[1] - 2, bounds[2] - 2, bounds[3] + 2, bounds[4] + 12, bounds[5] + 2};
        for (double[] sp : spawns)
            if (sp[0] < play[0] || sp[0] > play[3] + 1 || sp[2] < play[2] || sp[2] > play[5] + 1 || sp[1] < play[1] || sp[1] <= voidY)
                throw new IOException("start strip outside play-bounds");

        // ---- spot-check samples: 32 blocks spread over the shaft
        int ns = Math.min(32, p.keys.length);
        long[] sk = new long[ns];
        int[] sp = new int[ns];
        for (int i = 0; i < ns; i++) {
            int j = (int) ((long) i * (p.keys.length - 1) / Math.max(1, ns - 1));
            sk[i] = p.keys[j]; sp[i] = p.pal[j];
        }

        String hash;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(LOADER_VERSION.getBytes(StandardCharsets.UTF_8));
            md.update(yBytes);
            md.update(bBytes);
            hash = HexFormat.of().formatHex(md.digest()).substring(0, 32);
        } catch (NoSuchAlgorithmException e) { throw new IOException(e); }

        return new ImportedDropper(id, name, credit, folder.getName(), hash, yaw, lethal, voidY, play, spawns,
                new int[]{pMinX, minWaterY, pMinZ, pMaxX, pTop, pMaxZ}, water, bounds, seal, removed, barriers,
                p.keys, p.pal, p.palette, sk, sp);
    }

    // ------------------------------------------------------------------ keys

    /** Local block key: 16 bits each (x, z, y) with a +32768 bias; sorted keys group by x, then z, then y. */
    static long key(int x, int y, int z) { return ((x + 32768L) << 32) | ((z + 32768L) << 16) | (y + 32768L); }
    static int kx(long k) { return (int) (k >>> 32) - 32768; }
    static int kz(long k) { return (int) ((k >>> 16) & 0xFFFF) - 32768; }
    static int ky(long k) { return (int) (k & 0xFFFF) - 32768; }
    private static long col(int x, int z) { return ((x + 32768L) << 16) | (z + 32768L); }
    private static int colX(long c) { return (int) (c >>> 16) - 32768; }
    private static int colZ(long c) { return (int) (c & 0xFFFF) - 32768; }

    // ------------------------------------------------------------------ block file

    /** Mutable block store used while loading (a few hundred thousand entries). */
    static final class Blocks {
        final List<String> palette = new ArrayList<>();
        final Map<String, Integer> palIdx = new HashMap<>();
        final Map<Long, Integer> map = new LinkedHashMap<>();
        int removed;
        // flat view rebuilt on demand
        private long[] fk; private int[] fp; int n; private boolean dirty = true;

        int idx(String s) { return palIdx.computeIfAbsent(s, k -> { palette.add(k); return palette.size() - 1; }); }
        String get(int x, int y, int z) { Integer i = map.get(key(x, y, z)); return i == null ? null : palette.get(i); }
        void set(int x, int y, int z, String s) throws IOException {
            check(x, y, z);
            if (s == null) map.remove(key(x, y, z)); else map.put(key(x, y, z), idx(s));
            dirty = true;
        }
        /** Rebuild the indexed view (x(i), y(i), z(i), isWater(i), n) after changes. */
        void flat() {
            if (!dirty) return;
            n = map.size();
            fk = new long[n]; fp = new int[n];
            int i = 0;
            for (Map.Entry<Long, Integer> e : map.entrySet()) { fk[i] = e.getKey(); fp[i] = e.getValue(); i++; }
            dirty = false;
        }
        int x(int i) { return kx(fk[i]); }
        int y(int i) { return ky(fk[i]); }
        int z(int i) { return kz(fk[i]); }
        boolean isWater(int i) { String s = palette.get(fp[i]); return s.equals("minecraft:water") || s.startsWith("minecraft:water["); }

        record Packed(long[] keys, int[] pal, String[] palette, int[] bounds) {}

        Packed pack() throws IOException {
            if (map.isEmpty()) throw new IOException("no blocks");
            long[] k = new long[map.size()];
            int i = 0;
            for (long key : map.keySet()) k[i++] = key;
            Arrays.sort(k);
            int[] p = new int[k.length];
            int[] bd = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
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
        if (Math.abs(x) > MAX_HALF || Math.abs(z) > MAX_HALF || y < MIN_Y || y > MAX_Y)
            throw new IOException("block at " + x + "," + y + "," + z + " outside the allowed local box (|x|,|z| <= " + MAX_HALF + ")");
    }

    static Blocks readBlocks(byte[] raw) throws IOException {
        Blocks b = new Blocks();
        try (InputStream gz = new GZIPInputStream(new ByteArrayInputStream(raw), 1 << 16);
             DataInputStream in = new DataInputStream(new LimitedStream(gz, 64L * MAX_BLOCKS))) {
            byte[] mg = new byte[4];
            in.readFully(mg);
            if (!new String(mg, StandardCharsets.US_ASCII).equals("MCMI")) throw new IOException("not an MCMI block file");
            int ver = in.readInt();
            if (ver != 1) throw new IOException("unsupported MCMI version " + ver);
            int np = in.readInt();
            if (np <= 0 || np > MAX_PALETTE) throw new IOException("bad palette size " + np);
            String[] pal = new String[np];
            boolean[] banned = new boolean[np];
            for (int i = 0; i < np; i++) {
                String s = in.readUTF();
                if (s.isEmpty() || s.equals("air") || s.equals("minecraft:air") || s.equals("minecraft:cave_air") || s.equals("minecraft:void_air")) continue;
                String f = state(s);
                String base = f.indexOf('[') < 0 ? f : f.substring(0, f.indexOf('['));
                if (BANNED.contains(base)) { banned[i] = true; continue; }
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
        b.flat();
        return b;
    }

    /** Normalised, syntax-checked block state ("minecraft:" added when missing). */
    static String state(String s) throws IOException {
        if (s == null) throw new IOException("null block state");
        String f = s.trim().toLowerCase(Locale.ROOT);
        if (f.indexOf(':') < 0) f = "minecraft:" + f;
        if (f.length() > 300 || !STATE.matcher(f).matches()) throw new IOException("bad block state '" + abbrev(s) + "'");
        return f;
    }

    private static String abbrev(String s) { return s.length() > 60 ? s.substring(0, 60) + "..." : s; }

    /** Plain display text: no colour codes or control characters, at most {@code max} characters. */
    static String text(String s, int max) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length() && sb.length() < max; i++) {
            char c = s.charAt(i);
            if (c == '§' || Character.isISOControl(c)) continue;
            sb.append(c);
        }
        return sb.toString().trim();
    }

    /** Caps the number of decompressed bytes (gzip bomb guard). */
    private static final class LimitedStream extends java.io.FilterInputStream {
        private long left;
        LimitedStream(InputStream in, long limit) { super(in); left = limit; }
        @Override public int read() throws IOException {
            if (left <= 0) throw new IOException("block file decompresses to too much data");
            int r = super.read();
            if (r >= 0) left--;
            return r;
        }
        @Override public int read(byte[] b, int off, int len) throws IOException {
            if (left <= 0) throw new IOException("block file decompresses to too much data");
            int r = super.read(b, off, (int) Math.min(len, left));
            if (r > 0) left -= r;
            return r;
        }
    }

    // ------------------------------------------------------------------ yaml helpers

    @SuppressWarnings("unchecked")
    static Map<String, Object> parseYaml(byte[] bytes) throws IOException {
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

    private static Map<String, Object> section(Map<String, Object> y, String k) throws IOException {
        Object o = y.get(k);
        if (o == null) return null;
        if (!(o instanceof Map<?, ?> m)) throw new IOException(k + ": must be a section");
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<?, ?> e : m.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
        return out;
    }

    private static String str(Map<String, Object> y, String k, String def) {
        Object o = y.get(k);
        return o == null ? def : String.valueOf(o);
    }

    private static double dbl(Object o, double def) {
        if (o instanceof Number n && Double.isFinite(n.doubleValue())) return n.doubleValue();
        return def;
    }

    private static int intOf(Object o, String what) throws IOException {
        if (!(o instanceof Number n) || !Double.isFinite(n.doubleValue()) || Math.abs(n.doubleValue()) > 100_000)
            throw new IOException(what + ": number expected");
        return (int) Math.floor(n.doubleValue());
    }

    private static int[] box6(Object o, String what) throws IOException {
        if (!(o instanceof List<?> l) || l.size() != 6) throw new IOException(what + " must be [x1,y1,z1,x2,y2,z2]");
        int[] a = new int[6];
        for (int i = 0; i < 6; i++) a[i] = intOf(l.get(i), what);
        return a;
    }

    private static List<int[]> boxes(Object o, String what) throws IOException {
        List<int[]> out = new ArrayList<>();
        if (o == null) return out;
        if (!(o instanceof List<?> l)) throw new IOException(what + ": must be a list");
        if (l.size() > 10_000) throw new IOException(what + ": too many entries");
        for (Object e : l) out.add(box6(e, what));
        return out;
    }

    private static int[] order(int[] b) {
        return new int[]{Math.min(b[0], b[3]), Math.min(b[1], b[4]), Math.min(b[2], b[5]), Math.max(b[0], b[3]), Math.max(b[1], b[4]), Math.max(b[2], b[5])};
    }

    /** Ordered box that is filled cell by cell (size-limited). */
    private static int[] norm(int[] b) throws IOException {
        int[] n = order(b);
        long cells = (long) (n[3] - n[0] + 1) * (n[4] - n[1] + 1) * (n[5] - n[2] + 1);
        if (cells > MAX_BOX_CELLS) throw new IOException("box too large (" + cells + " cells)");
        return n;
    }

    private static void fill(Blocks b, int[] box, String s) throws IOException {
        int[] n = norm(box);
        for (int x = n[0]; x <= n[3]; x++) for (int y = n[1]; y <= n[4]; y++) for (int z = n[2]; z <= n[5]; z++) {
            check(x, y, z);
            if (s == null) b.map.remove(key(x, y, z)); else b.map.put(key(x, y, z), b.idx(s));
        }
        b.dirty = true;
    }
}
