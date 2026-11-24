package com.mcm.dropper;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Imported dropper stages for the core Multi-Stage Dropper. At enable the folders in
 * {@code plugins/MinecraftMondayCore/imported-droppers/} are read on a worker thread; each valid shaft gets a slot in
 * {@code dropper_world} (slot k centred at x = {@link #ORIGIN_X} - k * {@link #SPACING}, z = {@link #ORIGIN_Z}, far from
 * the built-in stages at x 6..33, z 5023..5325) and is pasted there once, spread over ticks. The paste is remembered
 * in {@code imported-droppers-state.yml} (slot, content hash, world box); on the next start a matching hash plus a
 * spot check of 32 blocks skips the paste, a changed hash clears the old box and pastes again.
 * A stage is offered to the game only once it is ready. No folder, or a broken one: nothing changes for the game.
 */
public final class ImportedDroppers {
    public static final String DIR = "imported-droppers";
    public static final int ORIGIN_X = -1100, ORIGIN_Z = 5174, SPACING = 300, DEFAULT_BASE_Y = 64;
    /** Built-in stage area (with margin) that no imported shaft may touch: x, z min/max. */
    static final int[] BUILT_IN_AREA = {-20, 4980, 80, 5380};

    private final JavaPlugin plugin;
    private final List<ImportedDropper> stages = new ArrayList<>();
    private final DropperPaster paster;
    private final File stateFile;
    private YamlConfiguration state;
    private boolean enabled;

    public ImportedDroppers(JavaPlugin plugin) {
        this.plugin = plugin;
        this.paster = new DropperPaster(plugin);
        this.stateFile = new File(plugin.getDataFolder(), "imported-droppers-state.yml");
    }

    public File folder() { return new File(plugin.getDataFolder(), DIR); }

    /** Every loaded shaft (ready or still pasting). Main thread. */
    public List<ImportedDropper> all() { return List.copyOf(stages); }

    /** Shafts that are pasted and can be played. Main thread. */
    public List<ImportedDropper> ready() {
        List<ImportedDropper> out = new ArrayList<>();
        for (ImportedDropper d : stages) if (d.ready) out.add(d);
        return out;
    }

    public boolean pasting() { return paster.busy(); }

    /** Start loading (worker thread) and pasting (main thread, budgeted). Does nothing without the folder or world. */
    public void enable(World world) {
        File dir = folder();
        if (world == null || !dir.isDirectory()) return;
        enabled = true;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<ImportedDropper> loaded;
            try {
                loaded = ImportedDropperLoader.loadAll(dir, plugin.getLogger());
            } catch (Throwable t) {
                plugin.getLogger().warning("Imported droppers: loading failed: " + t);
                return;
            }
            if (loaded.isEmpty()) return;
            Bukkit.getScheduler().runTask(plugin, () -> { if (enabled) place(world, loaded); });
        });
    }

    public void disable() {
        enabled = false;
        paster.cancelAll();
    }

    // ------------------------------------------------------------------ placement

    private void place(World w, List<ImportedDropper> loaded) {
        state = YamlConfiguration.loadConfiguration(stateFile);
        Set<Integer> used = new HashSet<>();
        for (String id : state.getKeys(false)) if (state.isInt(id + ".slot")) used.add(state.getInt(id + ".slot"));
        List<int[]> taken = new ArrayList<>();
        for (ImportedDropper d : loaded) {
            try {
                int slot = state.getInt(d.id + ".slot", -1);
                if (slot < 0) { slot = 0; while (used.contains(slot)) slot++; used.add(slot); }
                int oy = fitY(w, d);
                if (oy == Integer.MIN_VALUE) {
                    plugin.getLogger().warning("Imported dropper " + d.id + " skipped: " + (d.maxY - d.minY + 1)
                            + " blocks tall does not fit the world height " + w.getMinHeight() + ".." + (w.getMaxHeight() - 1));
                    continue;
                }
                d.slot = slot; d.ox = ORIGIN_X - slot * SPACING; d.oy = oy; d.oz = ORIGIN_Z;
                int[] wb = d.worldBounds();
                int[] pb = {d.ox + d.playBounds[0], d.oz + d.playBounds[2], d.ox + d.playBounds[3], d.oz + d.playBounds[5]};
                if (overlaps(pb, BUILT_IN_AREA) || overlaps(new int[]{wb[0], wb[2], wb[3], wb[5]}, BUILT_IN_AREA)) {
                    plugin.getLogger().warning("Imported dropper " + d.id + " skipped: slot " + slot + " overlaps the built-in stages");
                    continue;
                }
                boolean clash = false;
                for (int[] t : taken) if (overlaps(new int[]{wb[0] - 8, wb[2] - 8, wb[3] + 8, wb[5] + 8}, t)) clash = true;
                if (clash) { plugin.getLogger().warning("Imported dropper " + d.id + " skipped: overlaps another imported shaft"); continue; }
                taken.add(new int[]{wb[0], wb[2], wb[3], wb[5]});
                stages.add(d);

                BlockData[] datas = resolve(d);
                String oldHash = state.getString(d.id + ".hash");
                int[] oldBox = box(state.getIntegerList(d.id + ".box"));
                boolean sameOrigin = state.getInt(d.id + ".oy", Integer.MIN_VALUE) == d.oy;
                if (d.hash.equals(oldHash) && sameOrigin && oldBox != null) {
                    spotCheck(w, d, datas).thenAccept(ok -> {
                        if (!enabled) return;
                        if (ok) {
                            d.ready = true;
                            d.keys = null; d.pal = null;    // not needed any more
                            plugin.getLogger().info("Imported dropper " + d.id + " already in " + w.getName() + " at "
                                    + d.ox + " " + d.oy + " " + d.oz + " (slot " + d.slot + "), ready");
                        } else {
                            plugin.getLogger().info("Imported dropper " + d.id + ": blocks missing in the world, pasting again");
                            startPaste(w, d, datas, oldBox);
                        }
                    });
                } else {
                    startPaste(w, d, datas, oldBox);
                }
            } catch (Throwable t) {
                plugin.getLogger().warning("Imported dropper " + d.id + " skipped: " + t);
                stages.remove(d);
            }
        }
    }

    private void startPaste(World w, ImportedDropper d, BlockData[] datas, int[] clearIn) {
        // never clear outside this shaft's own slot column (the state file is only a hint)
        int[] clear = null;
        if (clearIn != null) {
            int h = SPACING / 2 - 1;
            clear = new int[]{Math.max(clearIn[0], d.ox - h), clearIn[1], Math.max(clearIn[2], d.oz - h),
                    Math.min(clearIn[3], d.ox + h), clearIn[4], Math.min(clearIn[5], d.oz + h)};
            if (clear[0] > clear[3] || clear[2] > clear[5] || clear[1] > clear[4]) clear = null;
        }
        plugin.getLogger().info("Imported dropper " + d.id + ": pasting " + d.blockCount + " blocks at " + d.ox + " " + d.oy + " " + d.oz
                + " (slot " + d.slot + ")" + (clear != null ? ", clearing the previous version first" : ""));
        // forget the old record first: if the server stops mid-paste, the next start pastes again
        state.set(d.id + ".slot", d.slot);
        state.set(d.id + ".hash", null);
        state.set(d.id + ".box", toList(clear != null ? union(clear, d.worldBounds()) : d.worldBounds()));
        save();
        paster.paste(w, d, datas, clear, ok -> {
            state.set(d.id + ".slot", d.slot);
            state.set(d.id + ".hash", d.hash);
            state.set(d.id + ".oy", d.oy);
            state.set(d.id + ".box", toList(d.worldBounds()));
            state.set(d.id + ".name", d.name);
            save();
            d.ready = true;
            d.keys = null; d.pal = null;
        });
    }

    /** Base y: 64 (as in the game plugins) unless the shaft would leave the world height, then shifted to fit. */
    static int fitY(World w, ImportedDropper d) {
        int lo = w.getMinHeight() + 1, hi = w.getMaxHeight() - 1;
        int oy = DEFAULT_BASE_Y;
        if (oy + d.maxY > hi) oy = hi - d.maxY;
        if (oy + d.minY < lo) oy = lo - d.minY;
        if (oy + d.maxY > hi || oy + d.minY < lo) return Integer.MIN_VALUE;
        return oy;
    }

    /** Block data per palette entry (main thread). Unknown states fall back to their base block, then to stone. */
    private BlockData[] resolve(ImportedDropper d) {
        BlockData[] out = new BlockData[d.palette.length];
        int bad = 0;
        for (int i = 0; i < d.palette.length; i++) {
            String s = d.palette[i];
            try {
                out[i] = Bukkit.createBlockData(s);
            } catch (IllegalArgumentException e) {
                bad++;
                int b = s.indexOf('[');
                try { out[i] = Bukkit.createBlockData(b > 0 ? s.substring(0, b) : s); }
                catch (IllegalArgumentException e2) { out[i] = Material.STONE.createBlockData(); }
            }
        }
        if (bad > 0) plugin.getLogger().warning("Imported dropper " + d.id + ": " + bad + " unknown block states replaced");
        return out;
    }

    /** True if at most one of the sample blocks differs from the data (by material). Loads the chunks async. */
    private CompletableFuture<Boolean> spotCheck(World w, ImportedDropper d, BlockData[] datas) {
        Map<Long, CompletableFuture<?>> loads = new HashMap<>();
        for (long k : d.sampleKeys) {
            int wx = d.ox + ImportedDropperLoader.kx(k), wz = d.oz + ImportedDropperLoader.kz(k);
            loads.computeIfAbsent(DropperPaster.chunkKey(wx >> 4, wz >> 4), ck -> w.getChunkAtAsync(wx >> 4, wz >> 4, true));
        }
        CompletableFuture<Boolean> out = new CompletableFuture<>();
        CompletableFuture.allOf(loads.values().toArray(new CompletableFuture[0])).whenComplete((v, err) ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (err != null) { out.complete(false); return; }
                    int miss = 0;
                    for (int i = 0; i < d.sampleKeys.length; i++) {
                        long k = d.sampleKeys[i];
                        int wx = d.ox + ImportedDropperLoader.kx(k), wy = d.oy + ImportedDropperLoader.ky(k), wz = d.oz + ImportedDropperLoader.kz(k);
                        if (w.getBlockAt(wx, wy, wz).getType() != datas[d.samplePal[i]].getMaterial()) miss++;
                    }
                    out.complete(miss <= 1);
                }));
        return out;
    }

    private void save() {
        try { state.save(stateFile); } catch (Exception e) { plugin.getLogger().warning("Could not save " + stateFile.getName() + ": " + e); }
    }

    private static boolean overlaps(int[] a, int[] b) {      // {minX, minZ, maxX, maxZ}
        return a[0] <= b[2] && a[2] >= b[0] && a[1] <= b[3] && a[3] >= b[1];
    }

    private static int[] box(List<Integer> l) { return l != null && l.size() == 6 ? l.stream().mapToInt(Integer::intValue).toArray() : null; }

    private static List<Integer> toList(int[] a) { List<Integer> l = new ArrayList<>(); for (int v : a) l.add(v); return l; }

    private static int[] union(int[] a, int[] b) {
        return new int[]{Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2]), Math.max(a[3], b[3]), Math.max(a[4], b[4]), Math.max(a[5], b[5])};
    }
}
