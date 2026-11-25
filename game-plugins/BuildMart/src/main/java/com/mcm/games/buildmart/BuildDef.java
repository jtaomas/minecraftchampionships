package com.mcm.games.buildmart;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Slab;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * One MCC build, authored from its official render (bm-2/frames/builds). 7 x 7 footprint, up to 10 layers on a 7 x 7
 * themed floor. Coordinates: x = 0..6 left to right seen from the front, z = 0..6 back (north) to front (south).
 */
final class BuildDef {
    static final int SIZE = 7;
    static final int MAX_H = 10;

    final String id, name, debut, reference;
    final int stars;
    final BlockData[][] floor = new BlockData[SIZE][SIZE];          // [z][x]
    final BlockData[][][] cells;                                     // [y][z][x], null = air
    final String[][][] canon;                                        // [y][z][x]
    final int height;
    final int blockCount;
    final LinkedHashMap<Material, Integer> materials = new LinkedHashMap<>();
    final List<String> guessed = new ArrayList<>();

    private BuildDef(JsonObject o) {
        id = o.get("id").getAsString();
        name = o.get("name").getAsString();
        debut = o.has("debut") ? o.get("debut").getAsString() : "";
        reference = o.has("reference") ? o.get("reference").getAsString() : "";
        stars = o.get("stars").getAsInt();
        if (stars < 1 || stars > 7) throw new IllegalArgumentException(id + ": stars " + stars);
        Map<Character, BlockData> pal = new HashMap<>();
        for (var e : o.getAsJsonObject("palette").entrySet()) {
            if (e.getKey().length() != 1) throw new IllegalArgumentException(id + ": palette key " + e.getKey());
            BlockData bd = Bukkit.createBlockData(e.getValue().getAsString());
            pal.put(e.getKey().charAt(0), bd);
        }
        JsonArray fl = o.getAsJsonArray("floor");
        rows(fl, "floor");
        for (int z = 0; z < SIZE; z++) {
            String r = fl.get(z).getAsString();
            for (int x = 0; x < SIZE; x++) {
                BlockData bd = pal.get(r.charAt(x));
                if (bd == null) throw new IllegalArgumentException(id + ": floor char '" + r.charAt(x) + "'");
                floor[z][x] = bd;
            }
        }
        JsonArray ls = o.getAsJsonArray("layers");
        if (ls.size() > MAX_H) throw new IllegalArgumentException(id + ": " + ls.size() + " layers > " + MAX_H);
        height = ls.size();
        cells = new BlockData[height][SIZE][SIZE];
        canon = new String[height][SIZE][SIZE];
        int count = 0;
        for (int y = 0; y < height; y++) {
            JsonArray layer = ls.get(y).getAsJsonArray();
            rows(layer, "layer " + y);
            for (int z = 0; z < SIZE; z++) {
                String r = layer.get(z).getAsString();
                for (int x = 0; x < SIZE; x++) {
                    char c = r.charAt(x);
                    if (c == '.') { canon[y][z][x] = Canonical.AIR; continue; }
                    BlockData bd = pal.get(c);
                    if (bd == null) throw new IllegalArgumentException(id + ": layer " + y + " char '" + c + "'");
                    cells[y][z][x] = bd;
                    canon[y][z][x] = Canonical.of(bd);
                    count++;
                    countMaterial(bd);
                }
            }
        }
        blockCount = count;
        if (o.has("guessed")) for (JsonElement g : o.getAsJsonArray("guessed")) guessed.add(g.getAsString());
    }

    private void rows(JsonArray a, String what) {
        if (a.size() != SIZE) throw new IllegalArgumentException(id + ": " + what + " has " + a.size() + " rows");
        for (JsonElement e : a) if (e.getAsString().length() != SIZE)
            throw new IllegalArgumentException(id + ": " + what + " row '" + e.getAsString() + "' is not 7 wide");
    }

    /** Shopping list: a door/bed counts once, a double slab as 2 slabs, a portal as nothing (lit with flint and steel). */
    private void countMaterial(BlockData bd) {
        Material m = bd.getMaterial();
        if (m == Material.NETHER_PORTAL) return;
        if (bd instanceof Bisected bi && !(bd instanceof org.bukkit.block.data.type.Stairs)
                && !(bd instanceof org.bukkit.block.data.type.TrapDoor) && bi.getHalf() == Bisected.Half.TOP) return;
        if (bd instanceof Bed bed && bed.getPart() == Bed.Part.HEAD) return;
        Material item = itemOf(bd);
        int n = (bd instanceof Slab s && s.getType() == Slab.Type.DOUBLE) ? 2 : 1;
        materials.merge(item, n, Integer::sum);
    }

    static Material itemOf(BlockData bd) {
        Material m = bd.getMaterial();
        if (m == Material.WATER) return Material.WATER_BUCKET;
        if (m == Material.LAVA) return Material.LAVA_BUCKET;
        try {
            Material p = bd.getPlacementMaterial();
            if (p != null && p.isItem() && !p.isAir()) return p;
        } catch (Throwable ignored) { }
        return m.isItem() ? m : Material.BARRIER;
    }

    String starString() { return "★".repeat(stars); }

    // ------------------------------------------------------------------ loading

    static List<BuildDef> loadAll(File pluginJar, Logger log) {
        List<String> names = new ArrayList<>();
        try (JarFile jar = new JarFile(pluginJar)) {
            for (Enumeration<JarEntry> en = jar.entries(); en.hasMoreElements(); ) {
                String n = en.nextElement().getName();
                if (n.startsWith("builds/") && n.endsWith(".json")) names.add(n);
            }
        } catch (Exception e) {
            log.severe("Could not list builds in " + pluginJar + ": " + e);
        }
        Collections.sort(names);
        List<BuildDef> out = new ArrayList<>();
        for (String n : names) {
            try (InputStream in = BuildDef.class.getClassLoader().getResourceAsStream(n)) {
                if (in == null) continue;
                JsonObject o = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                out.add(new BuildDef(o));
            } catch (Exception e) {
                log.severe("Build " + n + " is invalid and was skipped: " + e.getMessage());
            }
        }
        log.info("Loaded " + out.size() + " Build Mart builds");
        return out;
    }
}
