package com.mcm.games.tgttos.imported;

import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapFeatures;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Pos;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.SparseBlocks;
import com.mcm.games.tgttos.model.TgMapData.Spawn;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Loads maps imported from community world saves at plugin enable / {@code /race reload} (worker thread, no Bukkit world
 * access). Each map is a folder {@code plugins/McmRace/imported/<id>/} holding:
 * <ul>
 *   <li>{@code map.yml} - metadata (see {@code IMPORTED_MAPS.md} in the plugin folder of the repo);</li>
 *   <li>the block file named by {@code blocks:} (default {@code blocks.mcmi.gz}): gzip, big-endian:
 *       {@code "MCMI"}, int version (1), int paletteSize, paletteSize x UTF block-state strings, int blockCount,
 *       blockCount x (short x, short y, short z, int paletteIndex) in map-local coordinates.</li>
 * </ul>
 * The importer tool ({@code _candidates/tools/mca2tg.py}, not in the repo) writes both. If the folder does not exist
 * nothing happens. A broken map is logged and skipped; it never stops the built-in maps.
 */
public final class ImportedMaps {
    /** Local x must stay inside the slot (slots are {@code map-spacing} = 600 apart on x). */
    public static final int MAX_ABS_X = 290;
    private static final int MAX_BLOCKS = 6_000_000;

    private ImportedMaps() {}

    public static List<ImportedMap> loadAll(File dir, Logger log) {
        List<ImportedMap> out = new ArrayList<>();
        if (dir == null || !dir.isDirectory()) return out;
        File[] subs = dir.listFiles(File::isDirectory);
        if (subs == null) return out;
        Arrays.sort(subs);
        for (File f : subs) {
            File meta = new File(f, "map.yml");
            if (!meta.isFile()) continue;
            long t0 = System.nanoTime();
            try {
                ImportedMap m = load(f, meta);
                log.info("TGTTOS imported map " + m.id + " (" + m.name + ", " + m.type + ", by " + m.credit + "): "
                        + m.data.blocks.size() + " blocks, loaded in " + (System.nanoTime() - t0) / 1_000_000 + " ms from " + f.getName());
                out.add(m);
            } catch (Throwable t) {
                log.warning("TGTTOS: imported map in " + f + " skipped: " + t);
            }
        }
        return out;
    }

    static ImportedMap load(File folder, File metaFile) throws IOException {
        YamlConfiguration y = YamlConfiguration.loadConfiguration(metaFile);
        String id = req(y.getString("id"), "id").toLowerCase(Locale.ROOT);
        if (!id.matches("[a-z0-9_]{1,32}")) throw new IOException("id must be [a-z0-9_]{1,32}: " + id);
        String name = y.getString("name", id);
        String typeS = y.getString("type", "run").toLowerCase(Locale.ROOT);
        MapType type; Kit kit;
        switch (typeS) {
            // owner: TGTTOS is run / jump / bridge / punch. Elytra and boat maps belong to the Race plugin, droppers to the core Dropper.
            case "bridge", "run" -> { type = MapType.BRIDGE; kit = Kit.CONCRETE_PICKAXE_SNOWBALLS; }
            case "parkour" -> { type = MapType.PARKOUR; kit = Kit.SNOWBALLS_ONLY; }
            case "elytra", "glide", "boat", "drop" -> throw new IOException("type " + typeS + " is not a TGTTOS map (TGTTOS takes bridge | parkour)");
            default -> throw new IOException("type must be bridge|parkour: " + typeS);
        }
        String credit = y.getString("credit", "unknown");

        // ---- blocks
        File bf = new File(folder, y.getString("blocks", "blocks.mcmi.gz"));
        PackedBlocks blocks = readBlocks(bf);
        TgMapData d = new TgMapData(id, 0L, blocks);

        // ---- our own additions (clear first, then fills), before anything reads the blocks
        for (int[] b : boxes(y, "clear")) fillBox(d, b, null);
        for (Object o : y.getList("add", List.of())) {
            List<?> l = (List<?>) o;
            if (l.size() != 7) throw new IOException("add entries are [x1,y1,z1,x2,y2,z2,block]");
            int[] b = new int[6];
            for (int i = 0; i < 6; i++) b[i] = ((Number) l.get(i)).intValue();
            fillBox(d, b, String.valueOf(l.get(6)));
        }

        // ---- start
        ConfigurationSection st = y.getConfigurationSection("start");
        if (st == null) throw new IOException("start: section missing");
        d.startStrip = region(intList(st, "strip"), "start.strip");
        float yaw = (float) st.getDouble("yaw", 0);
        double step = Math.max(0.5, st.getDouble("spawn-step", 1.0));
        d.startSpawns.addAll(grid(d.startStrip, step, yaw, 40));
        String barrierBlock = st.getString("barrier-block", "minecraft:barrier");
        for (int[] b : boxes(st, "barrier")) forBox(b, (x, yy, z) -> {
            if (blocks.get(x, yy, z) == null) blocks.set(x, yy, z, barrierBlock);
            d.startBarrier.add(new Pos(x, yy, z));
        });
        String floorBlock = st.getString("drop-floor-block", "minecraft:glass");
        for (int[] b : boxes(st, "drop-floor")) forBox(b, (x, yy, z) -> {
            if (blocks.get(x, yy, z) == null) blocks.set(x, yy, z, floorBlock);
            d.dropFloor.add(new Pos(x, yy, z));
        });
        if (kit == Kit.BOAT) {
            ConfigurationSection bg = y.getConfigurationSection("boats");
            if (bg == null) throw new IOException("boat map needs boats: {box, step, yaw}");
            Region r = region(intList(bg, "box"), "boats.box");
            d.boatSpawns.addAll(grid(r, Math.max(1.5, bg.getDouble("step", 2.0)), (float) bg.getDouble("yaw", yaw), 40));
        }

        // ---- goal: a long, obvious row of chickens across the finish line (owner rule), or a classic pen
        ConfigurationSection goal = y.getConfigurationSection("goal");
        if (goal != null) {
            buildGoalRow(d, blocks, goal);
        } else {
        ConfigurationSection pen = y.getConfigurationSection("pen");
        if (pen == null) throw new IOException("goal: (row) or pen: section missing");
        d.pen = region(intList(pen, "box"), "pen.box");
        String ring = pen.getString("ring", "minecraft:hay_block");
        String floor = pen.getString("floor", null);
        Region p = d.pen;
        for (int x = p.minX() - 1; x <= p.maxX() + 1; x++)
            for (int z = p.minZ() - 1; z <= p.maxZ() + 1; z++) {
                boolean edge = x < p.minX() || x > p.maxX() || z < p.minZ() || z > p.maxZ();
                if (floor != null && blocks.get(x, p.minY() - 1, z) == null) blocks.set(x, p.minY() - 1, z, floor);
                if (edge && ring != null && !ring.isEmpty() && !ring.equals("none")) blocks.set(x, p.minY(), z, ring);
                else if (!edge) for (int yy = p.minY(); yy <= p.maxY(); yy++) blocks.set(x, yy, z, null);
            }
        for (int[] gate : boxes(pen, "gaps")) fillBox(d, gate, null);
        List<Spawn> cs = grid(p, 1.0, 0f, 0);
        for (int i = 0; i < cs.size() && d.chickenSpawns.size() < 10; i += Math.max(1, cs.size() / 10)) d.chickenSpawns.add(cs.get(i));
        while (d.chickenSpawns.size() < 8 && !cs.isEmpty()) d.chickenSpawns.add(cs.get(d.chickenSpawns.size() % cs.size()));
        }

        // ---- rules
        d.voidY = y.getInt("void-y", -20);
        for (String h : y.getStringList("hazard-blocks")) d.hazardBlocks.add(h.contains(":") ? h : "minecraft:" + h);
        for (int[] b : boxes(y, "hazard-regions")) d.hazardRegions.add(Region.of(b[0], b[1], b[2], b[3], b[4], b[5]));
        d.playBounds = region(intList(y, "play-bounds"), "play-bounds");
        if (y.isList("build-bounds")) d.buildBounds = region(intList(y, "build-bounds"), "build-bounds");
        d.extraItems.addAll(y.getStringList("extra-items"));
        List<Double> ov = y.getDoubleList("overview");
        if (ov.size() >= 3) d.overviewPoints.add(new Spawn(ov.get(0), ov.get(1), ov.get(2), ov.size() > 3 ? ov.get(3).floatValue() : 0f,
                ov.size() > 4 ? ov.get(4).floatValue() : 30f));
        d.notes.put("source", y.getString("source", "?"));
        d.notes.put("credit", credit);
        d.notes.put("licence", y.getString("licence", "?"));
        ConfigurationSection notes = y.getConfigurationSection("notes");
        if (notes != null) for (String k : notes.getKeys(false)) d.notes.put(k, String.valueOf(notes.get(k)));

        // ---- features
        MapFeatures f = new MapFeatures();
        int order = 1;
        for (Map<?, ?> m : y.getMapList("checkpoints")) {
            String n = String.valueOf(m.containsKey("name") ? m.get("name") : "cp" + order);
            Region trig = region(nums(m.get("trigger")), "checkpoint trigger");
            List<Spawn> sp = new ArrayList<>();
            if (m.containsKey("spawn-box")) sp.addAll(grid(region(nums(m.get("spawn-box")), "spawn-box"), 1.0, num(m.get("yaw"), 0), 0));
            if (m.containsKey("spawn")) { int[] s = nums(m.get("spawn")); sp.add(new Spawn(s[0] + 0.5, s[1], s[2] + 0.5, s.length > 3 ? s[3] : num(m.get("yaw"), 0))); }
            if (sp.isEmpty()) throw new IOException("checkpoint " + n + " needs spawn or spawn-box");
            List<Spawn> boats = kit == Kit.BOAT ? sp : List.of();
            int o = m.containsKey("order") ? ((Number) m.get("order")).intValue() : order;
            f.checkpoints.add(new MapFeatures.Checkpoint(o, n, trig, sp, boats));
            order = o + 1;
        }
        for (Map<?, ?> m : y.getMapList("refills"))
            f.refills.add(new MapFeatures.Refill(String.valueOf(m.get("name")), region(nums(m.get("box")), "refill box"),
                    m.containsKey("rockets") ? ((Number) m.get("rockets")).intValue() : 2));
        for (Map<?, ?> m : y.getMapList("depots")) {
            int[] b = nums(m.get("boat"));
            f.depots.add(new MapFeatures.Depot(String.valueOf(m.get("name")), region(nums(m.get("area")), "depot area"),
                    new Spawn(b[0] + 0.5, b[1], b[2] + 0.5, b.length > 3 ? b[3] : 0)));
        }
        for (Map<?, ?> m : y.getMapList("bonus"))
            f.gates.add(new MapFeatures.BonusGate(String.valueOf(m.get("group")), String.valueOf(m.get("name")),
                    ((Number) m.get("tier")).intValue(), region(nums(m.get("box")), "bonus box")));

        for (Map<?, ?> m : y.getMapList("movers")) f.movers.add(mover(m));

        List<int[]> decoys = new ArrayList<>();
        for (Object o : y.getList("decoys", List.of())) decoys.add(nums(o));

        blocks.seal();

        // ---- sanity: everything must stay inside the slot on x
        int[] bad = {0};
        blocks.forEach((x, yy, z, s) -> { if (Math.abs(x) > MAX_ABS_X) bad[0]++; });
        if (bad[0] > 0) throw new IOException(bad[0] + " blocks outside local x +-" + MAX_ABS_X + " (would overlap the next slot)");
        if (Math.abs(d.playBounds.minX()) > MAX_ABS_X || Math.abs(d.playBounds.maxX()) > MAX_ABS_X)
            throw new IOException("play-bounds outside local x +-" + MAX_ABS_X);

        int lethal = y.getInt("lethal-fall", 0);
        int rs = y.getInt("round-seconds", 0);
        boolean rot = y.getBoolean("rotation", true);
        return new ImportedMap(id, name, type, kit, credit, lethal, rs, rot, decoys, f.isEmpty() ? MapFeatures.NONE : f, d, folder.getName());
    }

    // ------------------------------------------------------------------ block file

    static PackedBlocks readBlocks(File f) throws IOException {
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(new FileInputStream(f), 1 << 16), 1 << 16))) {
            byte[] mg = new byte[4];
            in.readFully(mg);
            if (!new String(mg, java.nio.charset.StandardCharsets.US_ASCII).equals("MCMI")) throw new IOException("not an MCMI block file: " + f);
            int ver = in.readInt();
            if (ver != 1) throw new IOException("unsupported MCMI version " + ver);
            int np = in.readInt();
            if (np <= 0 || np > 65536) throw new IOException("bad palette size " + np);
            String[] pal = new String[np];
            for (int i = 0; i < np; i++) pal[i] = SparseBlocks.normalize(in.readUTF());
            int n = in.readInt();
            if (n < 0 || n > MAX_BLOCKS) throw new IOException("bad block count " + n + " (max " + MAX_BLOCKS + ")");
            long[] k = new long[n];
            int[] p = new int[n];
            int m = 0;
            for (int i = 0; i < n; i++) {
                int x = in.readShort(), yy = in.readShort(), z = in.readShort(), pi = in.readInt();
                if (pi < 0 || pi >= np) throw new IOException("bad palette index at block " + i);
                if (pal[pi] == null) continue;                    // air entries are skipped
                k[m] = SparseBlocks.key(x, yy, z); p[m] = pi; m++;
            }
            return PackedBlocks.of(k, p, pal, m);
        }
    }

    /**
     * {name, motion: pendulum|rotor|slider, pivot: [x,y,z], axis: x|y|z, amplitude (deg; rotor: start angle), period,
     * phase (ticks), boxes: [[x0,y0,z0,x1,y1,z1,block,hazard]...] in the mover frame, solid: bool,
     * slider only: slide: [dx,dy,dz], open, move, closed (ticks); tier (0 = plain)}.
     */
    static MapFeatures.Mover mover(Map<?, ?> m) throws IOException {
        String name = String.valueOf(m.get("name"));
        List<?> pv = (List<?>) m.get("pivot");
        if (pv == null || pv.size() != 3) throw new IOException("mover " + name + ": pivot [x,y,z] missing");
        double px = dbl(pv.get(0)), py = dbl(pv.get(1)), pz = dbl(pv.get(2));
        List<MapFeatures.Box> boxes = new ArrayList<>();
        for (Object o : (List<?>) m.get("boxes")) {
            List<?> b = (List<?>) o;
            boxes.add(new MapFeatures.Box(dbl(b.get(0)), dbl(b.get(1)), dbl(b.get(2)), dbl(b.get(3)), dbl(b.get(4)), dbl(b.get(5)),
                    SparseBlocks.normalize(String.valueOf(b.get(6))), b.size() < 8 || Boolean.TRUE.equals(b.get(7))));
        }
        if (boxes.isEmpty()) throw new IOException("mover " + name + " has no boxes");
        String motion = String.valueOf(m.containsKey("motion") ? m.get("motion") : "pendulum").toLowerCase(Locale.ROOT);
        char axis = String.valueOf(m.containsKey("axis") ? m.get("axis") : "x").charAt(0);
        int period = m.containsKey("period") ? ((Number) m.get("period")).intValue() : 80;
        int phase = m.containsKey("phase") ? ((Number) m.get("phase")).intValue() : 0;
        double amp = m.containsKey("amplitude") ? dbl(m.get("amplitude")) : 45;
        MapFeatures.Mover mv = switch (motion) {
            case "rotor" -> MapFeatures.Mover.rotor(name, px, py, pz, axis, amp, period, phase, boxes);
            case "slider" -> {
                List<?> s = (List<?>) m.get("slide");
                yield MapFeatures.Mover.slider(name, px, py, pz, dbl(s.get(0)), dbl(s.get(1)), dbl(s.get(2)),
                        ((Number) m.get("open")).intValue(), ((Number) m.get("move")).intValue(), ((Number) m.get("closed")).intValue(), phase, boxes);
            }
            default -> MapFeatures.Mover.pendulum(name, px, py, pz, axis, amp, period, phase, boxes);
        };
        if (Boolean.TRUE.equals(m.get("solid"))) mv = mv.withSolid();
        return mv;
    }

    private static double dbl(Object o) { return ((Number) o).doubleValue(); }

    /**
     * goal: {row: [x1,y,z1,x2,y,z2] (feet line of the chickens, along x or z), facing: north|south|east|west (the side
     * the racers come from), frame: block, floor: block}. Builds the finish: a floor under the row, a 2-high back wall,
     * posts at both ends and a lit beam 5 above the row across the whole width; chickens every 2 blocks along it.
     * The pen is the 1-deep row itself (chickens are kept on their line).
     */
    static void buildGoalRow(TgMapData d, PackedBlocks blocks, ConfigurationSection g) throws IOException {
        List<Integer> r = g.getIntegerList("row");
        if (r.size() != 6) throw new IOException("goal.row must be [x1,y,z1,x2,y,z2]");
        int x1 = Math.min(r.get(0), r.get(3)), x2 = Math.max(r.get(0), r.get(3)), yy = r.get(1);
        int z1 = Math.min(r.get(2), r.get(5)), z2 = Math.max(r.get(2), r.get(5));
        boolean alongX = (x2 - x1) >= (z2 - z1);
        if (alongX ? z1 != z2 : x1 != x2) throw new IOException("goal.row must be a straight line along x or z");
        String facing = g.getString("facing", alongX ? "north" : "west").toLowerCase(Locale.ROOT);
        int back = switch (facing) { case "north", "west" -> 1; default -> -1; };   // back wall on the far side
        String frame = g.getString("frame", "minecraft:stripped_dark_oak_log[axis=y]");
        String wall = g.getString("wall", "minecraft:hay_block");
        String floor = g.getString("floor", "minecraft:moss_block");
        String light = g.getString("light", "minecraft:shroomlight");
        int len = alongX ? x2 - x1 : z2 - z1;
        for (int i = -1; i <= len + 1; i++) {
            int x = alongX ? x1 + i : x1, z = alongX ? z1 : z1 + i;
            int bx = alongX ? x : x + back, bz = alongX ? z + back : z;
            int fx = alongX ? x : x - back, fz = alongX ? z - back : z;
            for (int k = -1; k <= 1; k++) {           // floor under row, wall side and front side
                int ox = alongX ? x : x + k * back, oz = alongX ? z + k * back : z;
                if (blocks.get(ox, yy - 1, oz) == null) blocks.set(ox, yy - 1, oz, floor);
            }
            for (int h = 0; h <= 4; h++) { blocks.set(x, yy + h, z, null); blocks.set(fx, yy + h, fz, null); }
            boolean end = i == -1 || i == len + 1;
            if (end) for (int h = 0; h <= 5; h++) blocks.set(x, yy + h, z, frame);
            else {
                blocks.set(bx, yy, bz, wall); blocks.set(bx, yy + 1, bz, wall);
                blocks.set(x, yy + 5, z, i % 3 == 0 ? light : frame.replace("[axis=y]", alongX ? "[axis=x]" : "[axis=z]"));
            }
        }
        d.pen = alongX ? Region.of(x1, yy, z1, x2, yy + 1, z1) : Region.of(x1, yy, z1, x1, yy + 1, z2);
        for (int i = 0; i <= len; i += len >= 16 ? 2 : 1)
            d.chickenSpawns.add(new Spawn((alongX ? x1 + i : x1) + 0.5, yy, (alongX ? z1 : z1 + i) + 0.5, 0f));
        while (d.chickenSpawns.size() < 8) d.chickenSpawns.add(d.chickenSpawns.get(d.chickenSpawns.size() % Math.max(1, len / 2 + 1)));
    }

    // ------------------------------------------------------------------ helpers

    @FunctionalInterface interface Cell { void at(int x, int y, int z); }

    private static void forBox(int[] b, Cell c) {
        for (int x = Math.min(b[0], b[3]); x <= Math.max(b[0], b[3]); x++)
            for (int y = Math.min(b[1], b[4]); y <= Math.max(b[1], b[4]); y++)
                for (int z = Math.min(b[2], b[5]); z <= Math.max(b[2], b[5]); z++) c.at(x, y, z);
    }

    private static void fillBox(TgMapData d, int[] b, String block) { forBox(b, (x, y, z) -> d.blocks.set(x, y, z, block)); }

    /** Spawn grid over a region's floor layer (minY), block centres, every {@code step} blocks; repeated up to {@code min}. */
    static List<Spawn> grid(Region r, double step, float yaw, int min) {
        List<Spawn> out = new ArrayList<>();
        for (double x = r.minX() + 0.5; x <= r.maxX() + 0.5; x += step)
            for (double z = r.minZ() + 0.5; z <= r.maxZ() + 0.5; z += step)
                out.add(new Spawn(x, r.minY(), z, yaw));
        int base = out.size();
        for (int i = 0; base > 0 && out.size() < min; i++) out.add(out.get(i % base));
        return out;
    }

    private static String req(String s, String what) throws IOException {
        if (s == null || s.isBlank()) throw new IOException(what + " missing");
        return s;
    }

    private static List<Integer> intList(ConfigurationSection c, String key) throws IOException {
        List<Integer> l = c.getIntegerList(key);
        if (l.size() != 6) throw new IOException(key + " must be [x1,y1,z1,x2,y2,z2]");
        return l;
    }

    private static Region region(List<Integer> l, String what) {
        return Region.of(l.get(0), l.get(1), l.get(2), l.get(3), l.get(4), l.get(5));
    }

    private static Region region(int[] b, String what) throws IOException {
        if (b.length != 6) throw new IOException(what + " must be [x1,y1,z1,x2,y2,z2]");
        return Region.of(b[0], b[1], b[2], b[3], b[4], b[5]);
    }

    private static List<int[]> boxes(ConfigurationSection c, String key) throws IOException {
        List<int[]> out = new ArrayList<>();
        for (Object o : c.getList(key, List.of())) {
            int[] b = nums(o);
            if (b.length != 6) throw new IOException(key + " entries must be [x1,y1,z1,x2,y2,z2]");
            out.add(b);
        }
        return out;
    }

    private static int[] nums(Object o) {
        List<?> l = (List<?>) o;
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = (int) Math.floor(((Number) l.get(i)).doubleValue());
        return a;
    }

    private static float num(Object o, float def) { return o instanceof Number n ? n.floatValue() : def; }
}
