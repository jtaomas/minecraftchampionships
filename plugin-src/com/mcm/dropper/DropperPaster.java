package com.mcm.dropper;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tick-budgeted paster for imported dropper shafts (main thread only, one job at a time).
 * Chunks are loaded with {@code getChunkAtAsync} (a few in flight) and held by a plugin chunk ticket while the job
 * runs, so the main thread never loads or generates a chunk synchronously. Each tick writes for at most
 * {@link #budgetNanos} (no physics). A job can first clear an old box (a previous version of the shaft) chunk by chunk.
 */
final class DropperPaster {
    private final Plugin plugin;
    private final List<Job> jobs = new ArrayList<>();
    private BukkitTask task;
    long budgetNanos = 8_000_000L;
    int maxInFlight = 8;

    DropperPaster(Plugin plugin) { this.plugin = plugin; }

    final class Job {
        final String label;
        final World world;
        final long[] chunkKeys;
        final int[][] ops;        // per chunk: lx, y, lz, dataIndex (4 ints per block)
        final int[] opCount;
        final BlockData[] datas;
        final int[] clear;        // world box {x1,y1,z1,x2,y2,z2} or null
        final Runnable onDone;
        final ArrayDeque<Integer> ready = new ArrayDeque<>();
        final Set<Long> ticketed = new HashSet<>();
        int requested, inFlight, chunksDone;
        int cur = -1, curOp;
        long clearCursor = -1;    // >= 0 while the current chunk is being cleared
        Chunk curChunk;
        long blocks, cleared, ticks, nanos, maxNanos, started = System.nanoTime();
        boolean cancelled;

        Job(String label, World world, long[] chunkKeys, int[][] ops, int[] opCount, BlockData[] datas, int[] clear, Runnable onDone) {
            this.label = label; this.world = world; this.chunkKeys = chunkKeys; this.ops = ops; this.opCount = opCount;
            this.datas = datas; this.clear = clear; this.onDone = onDone;
        }
    }

    static long chunkKey(int cx, int cz) { return (((long) cx) << 32) | (cz & 0xFFFFFFFFL); }

    /**
     * Queue a paste of {@code d} at its placement ({@code d.ox/oy/oz}). {@code datas[i]} is the resolved block data of
     * palette entry {@code i}. {@code clear} (world box, may be null) is emptied first. {@code onDone(true)} runs on the
     * main thread when every block is written.
     */
    void paste(World w, ImportedDropper d, BlockData[] datas, int[] clear, java.util.function.Consumer<Boolean> onDone) {
        Map<Long, int[]> buf = new LinkedHashMap<>();
        Map<Long, Integer> cnt = new java.util.HashMap<>();
        int minY = w.getMinHeight(), maxY = w.getMaxHeight();
        for (int i = 0; i < d.keys.length; i++) {
            long k = d.keys[i];
            int wx = d.ox + ImportedDropperLoader.kx(k), wy = d.oy + ImportedDropperLoader.ky(k), wz = d.oz + ImportedDropperLoader.kz(k);
            if (wy < minY || wy >= maxY || datas[d.pal[i]] == null) continue;
            long ck = chunkKey(wx >> 4, wz >> 4);
            int n = cnt.getOrDefault(ck, 0);
            int[] a = buf.get(ck);
            if (a == null) { a = new int[256]; buf.put(ck, a); }
            if ((n + 1) * 4 > a.length) { a = Arrays.copyOf(a, a.length * 2); buf.put(ck, a); }
            a[n * 4] = wx & 15; a[n * 4 + 1] = wy; a[n * 4 + 2] = wz & 15; a[n * 4 + 3] = d.pal[i];
            cnt.put(ck, n + 1);
        }
        if (clear != null) {
            for (int cx = clear[0] >> 4; cx <= clear[3] >> 4; cx++)
                for (int cz = clear[2] >> 4; cz <= clear[5] >> 4; cz++) {
                    long ck = chunkKey(cx, cz);
                    if (!buf.containsKey(ck)) { buf.put(ck, new int[0]); cnt.put(ck, 0); }
                }
        }
        long[] keys = new long[buf.size()];
        int[][] ops = new int[buf.size()][];
        int[] counts = new int[buf.size()];
        int i = 0;
        for (Map.Entry<Long, int[]> e : buf.entrySet()) { keys[i] = e.getKey(); ops[i] = e.getValue(); counts[i] = cnt.get(e.getKey()); i++; }
        jobs.add(new Job(d.id, w, keys, ops, counts, datas, clear, () -> onDone.accept(true)));
        if (task == null) task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    boolean busy() { return !jobs.isEmpty(); }

    void cancelAll() {
        for (Job j : jobs) { j.cancelled = true; releaseTickets(j); }
        jobs.clear();
        if (task != null) { task.cancel(); task = null; }
    }

    private void tick() {
        if (jobs.isEmpty()) { if (task != null) { task.cancel(); task = null; } return; }
        long t0 = System.nanoTime(), deadline = t0 + budgetNanos;
        Job j = jobs.get(0);
        while (j.inFlight < maxInFlight && j.requested < j.chunkKeys.length) request(j, j.requested++);
        int n = 0;
        outer:
        while (true) {
            if (j.cur < 0) {
                Integer next = j.ready.poll();
                if (next == null) break;
                if (next < 0) { j.chunksDone++; continue; }       // chunk failed to load: skipped
                long k = j.chunkKeys[next];
                int cx = (int) (k >> 32), cz = (int) k;
                if (!j.world.isChunkLoaded(cx, cz)) { request(j, next); continue; }
                j.cur = next; j.curOp = 0; j.curChunk = j.world.getChunkAt(cx, cz);
                j.clearCursor = j.clear != null ? 0 : -1;
            }
            Chunk ch = j.curChunk;
            if (j.clearCursor >= 0) {
                // clear the part of the old box inside this chunk: cursor runs over (lx, lz, y)
                int[] c = j.clear;
                int bx = ch.getX() << 4, bz = ch.getZ() << 4;
                int x0 = Math.max(c[0], bx), x1 = Math.min(c[3], bx + 15), z0 = Math.max(c[2], bz), z1 = Math.min(c[5], bz + 15);
                int y0 = Math.max(c[1], j.world.getMinHeight()), y1 = Math.min(c[4], j.world.getMaxHeight() - 1);
                long w = x1 - x0 + 1, dz = z1 - z0 + 1, h = y1 - y0 + 1;
                long total = (w > 0 && dz > 0 && h > 0) ? w * dz * h : 0;
                while (j.clearCursor < total) {
                    long q = j.clearCursor++;
                    int y = (int) (q % h) + y0; q /= h;
                    int z = (int) (q % dz) + z0; int x = (int) (q / dz) + x0;
                    Block b = ch.getBlock(x & 15, y, z & 15);
                    if (!b.getType().isAir()) { b.setType(org.bukkit.Material.AIR, false); j.cleared++; }
                    if ((++n & 1023) == 0 && System.nanoTime() > deadline) break outer;
                }
                j.clearCursor = -1;
            }
            int[] a = j.ops[j.cur];
            int cnt = j.opCount[j.cur];
            while (j.curOp < cnt) {
                int b = j.curOp * 4;
                ch.getBlock(a[b], a[b + 1], a[b + 2]).setBlockData(j.datas[a[b + 3]], false);
                j.curOp++; j.blocks++;
                if ((++n & 255) == 0 && System.nanoTime() > deadline) break outer;
            }
            j.ops[j.cur] = null;
            j.cur = -1; j.curChunk = null;
            j.chunksDone++;
            if (System.nanoTime() > deadline) break;
        }
        long dt = System.nanoTime() - t0;
        j.ticks++; j.nanos += dt; j.maxNanos = Math.max(j.maxNanos, dt);
        if (j.chunksDone >= j.chunkKeys.length && j.cur < 0) {
            jobs.remove(j);
            releaseTickets(j);
            plugin.getLogger().info(String.format(java.util.Locale.ROOT,
                    "Imported dropper %s pasted: %d blocks (%d old cleared) in %d chunks, %d ticks, %d ms wall, %.0f ms work (max %.1f ms/tick)",
                    j.label, j.blocks, j.cleared, j.chunkKeys.length, j.ticks, (System.nanoTime() - j.started) / 1_000_000, j.nanos / 1e6, j.maxNanos / 1e6));
            try { j.onDone.run(); } catch (Throwable t) { plugin.getLogger().severe("Imported dropper paste callback threw: " + t); }
            if (jobs.isEmpty() && task != null) { task.cancel(); task = null; }
        }
    }

    private void request(Job j, int idx) {
        long k = j.chunkKeys[idx];
        int cx = (int) (k >> 32), cz = (int) k;
        j.inFlight++;
        j.world.getChunkAtAsync(cx, cz, true).whenComplete((ch, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
            j.inFlight--;
            if (j.cancelled) return;
            if (ch == null) {
                plugin.getLogger().warning("Imported dropper " + j.label + ": chunk " + cx + "," + cz + " failed to load: " + err);
                j.ready.add(-idx - 1);
                return;
            }
            if (j.ticketed.add(k)) ch.addPluginChunkTicket(plugin);
            j.ready.add(idx);
        }));
    }

    private void releaseTickets(Job j) {
        for (long k : j.ticketed) {
            try { j.world.removePluginChunkTicket((int) (k >> 32), (int) k, plugin); } catch (Throwable ignored) {}
        }
        j.ticketed.clear();
    }
}
