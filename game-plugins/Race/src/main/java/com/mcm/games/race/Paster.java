package com.mcm.games.race;

import com.mcm.games.race.model.TgMapData;
import com.mcm.games.race.model.TgMapData.SparseBlocks;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Tick-budgeted map paster (replaces the core BuildQueue for Race maps).
 * <ol>
 *   <li>Worker thread: the block list (stale clears from the previous paste at the slot, map blocks, the chosen
 *       variant options) is grouped per chunk. No Bukkit calls.</li>
 *   <li>Main thread: chunks are requested with {@code getChunkAtAsync} (a few in flight), each gets a plugin chunk
 *       ticket when it arrives, and only loaded chunks are written - never a synchronous chunk load/generate.</li>
 *   <li>Writing runs at most {@link #budgetNanos} per tick (default 15 ms), no physics.</li>
 * </ol>
 * Tickets stay on a slot's chunks until {@link #release} (the slot is being played or about to be). Main thread only.
 */
final class Paster {
    private final Plugin plugin;
    private final MapPlacer placer;
    private final ExecutorService worker;
    /** Per slot: packed local keys of every block pasted there last time (for clearing stale blocks). */
    private final Map<Integer, Set<Long>> pasted = new HashMap<>();
    /** Per slot: chunk keys holding our plugin ticket. */
    private final Map<Integer, Set<Long>> tickets = new HashMap<>();
    private final List<Job> jobs = new ArrayList<>();
    private BukkitTask task;
    long budgetNanos = 15_000_000L;
    int maxInFlight = 12;
    boolean debugTiming;

    Paster(Plugin plugin, MapPlacer placer, ExecutorService worker) {
        this.plugin = plugin;
        this.placer = placer;
        this.worker = worker;
    }

    /** One paste of one map into one slot. */
    final class Job {
        final String label;
        final World world;
        final int slot;
        final TgMapData data;
        final Map<String, String> choice;
        final List<Runnable> onDone = new ArrayList<>();
        volatile boolean cancelled;
        boolean done, failed;
        // prepared on the worker
        volatile long[] chunkKeys;
        int[][] ops;          // per chunk: x, y, z, state (world coordinates), 4 ints per block
        int[] opCount;
        String[] states;
        Set<Long> now, old;
        // main-thread state
        BlockData[] datas;
        int requested, inFlight;
        final ArrayDeque<Integer> ready = new ArrayDeque<>();
        int curChunk = -1, curOp;
        Chunk curChunkObj;
        int chunksDone;
        long blocks, totalBlocks, ticks, nanos, maxNanos, started = System.nanoTime();

        Job(String label, World world, int slot, TgMapData data, Map<String, String> choice) {
            this.label = label; this.world = world; this.slot = slot; this.data = data; this.choice = choice;
        }

        boolean prepared() { return chunkKeys != null; }

        double progress() {
            if (done) return 1;
            if (!prepared() || totalBlocks == 0) return 0;
            return Math.min(1, blocks / (double) totalBlocks);
        }

        Job then(Runnable r) { if (done) r.run(); else onDone.add(r); return this; }
    }

    /** Start pasting {@code d} at {@code slot}. {@code then} runs on the main thread once every block is set. */
    Job paste(String label, World w, int slot, TgMapData d, Map<String, String> choice) {
        for (Job j : new ArrayList<>(jobs)) if (j.slot == slot) cancel(j);   // newest paste of a slot wins
        Job j = new Job(label, w, slot, d, choice == null ? Map.of() : Map.copyOf(choice));
        j.old = pasted.get(slot);
        jobs.add(j);
        final int ox = placer.ox(slot), oy = placer.oy(), oz = placer.oz(slot);
        final int minY = w.getMinHeight(), maxY = w.getMaxHeight();
        worker.execute(() -> {
            try {
                prepare(j, ox, oy, oz, minY, maxY);
            } catch (Throwable t) {
                plugin.getLogger().severe("Race paste " + label + ": preparing failed: " + t);
                j.failed = true;
            }
            Bukkit.getScheduler().runTask(plugin, () -> { if (!j.cancelled) ensureTask(); });
        });
        return j;
    }

    /**
     * Map Edit: paste into a slot nothing has ever used (no margin clear, no stale-set bookkeeping kept afterwards).
     * Remember to {@link #forgetSlot} when the edit session ends.
     */
    Job pasteFresh(String label, World w, int slot, TgMapData d, Map<String, String> choice) {
        pasted.put(slot, Set.of());
        return paste(label, w, slot, d, choice);
    }

    /** Drop the bookkeeping and chunk tickets of a slot (an edit slot that is no longer used). */
    void forgetSlot(int slot) {
        pasted.remove(slot);
        Set<Long> t = tickets.remove(slot);
        World w = Bukkit.getWorld(RacePlugin.WORLD);
        if (t != null && w != null) for (long k : t) w.removePluginChunkTicket((int) (k >> 32), (int) k, plugin);
    }

    /** Worker: group every write by chunk (clears, then map blocks, then variants - later writes win). */
    private void prepare(Job j, int ox, int oy, int oz, int minY, int maxY) {
        TgMapData d = j.data;
        Set<Long> now = new HashSet<>(Math.max(16, d.blocks.size() * 4 / 3));
        d.blocks.forEach((x, y, z, b) -> now.add(SparseBlocks.key(x, y, z)));
        for (TgMapData.VariantGroup g : d.variants)
            for (List<TgMapData.Placement> l : g.options.values())
                for (TgMapData.Placement p : l) now.add(SparseBlocks.key(p.x(), p.y(), p.z()));
        LinkedHashMap<String, Integer> stateIdx = new LinkedHashMap<>();
        stateIdx.put(null, 0);                                  // 0 = air
        Map<Long, int[]> buf = new LinkedHashMap<>();
        Map<Long, Integer> cnt = new HashMap<>();
        Acc acc = (wx, wy, wz, s) -> {
            if (j.cancelled || wy < minY || wy >= maxY) return;
            long ck = (((long) (wx >> 4)) << 32) | ((wz >> 4) & 0xFFFFFFFFL);
            int n = cnt.getOrDefault(ck, 0);
            int[] a = buf.get(ck);
            if (a == null) { a = new int[64]; buf.put(ck, a); }
            if ((n + 1) * 4 > a.length) { a = Arrays.copyOf(a, a.length * 2); buf.put(ck, a); }
            Integer si = stateIdx.get(s);
            if (si == null) { si = stateIdx.size(); stateIdx.put(s, si); }
            a[n * 4] = wx; a[n * 4 + 1] = wy; a[n * 4 + 2] = wz; a[n * 4 + 3] = si;
            cnt.put(ck, n + 1);
        };
        if (j.old != null)
            for (long k : j.old)
                if (!now.contains(k)) acc.put(ox + SparseBlocks.kx(k), oy + SparseBlocks.ky(k), oz + SparseBlocks.kz(k), null);
        d.blocks.forEach((x, y, z, b) -> acc.put(ox + x, oy + y, oz + z, b));
        for (TgMapData.VariantGroup g : d.variants) {
            String opt = j.choice.get(g.name);
            if (opt == null) continue;
            List<TgMapData.Placement> l = g.options.get(opt);
            if (l == null) continue;
            for (TgMapData.Placement p : l) acc.put(ox + p.x(), oy + p.y(), oz + p.z(), SparseBlocks.normalize(p.block()));
        }
        int n = buf.size(), i = 0;
        long[] keys = new long[n];
        int[][] ops = new int[n][];
        int[] counts = new int[n];
        long total = 0;
        for (Map.Entry<Long, int[]> e : buf.entrySet()) {
            keys[i] = e.getKey(); ops[i] = e.getValue(); counts[i] = cnt.get(e.getKey()); total += counts[i]; i++;
        }
        String[] st = new String[stateIdx.size()];
        for (Map.Entry<String, Integer> e : stateIdx.entrySet()) st[e.getValue()] = e.getKey();
        j.now = now;
        j.states = st;
        j.ops = ops;
        j.opCount = counts;
        j.totalBlocks = total;
        j.chunkKeys = keys;          // published last (prepared())
    }

    @FunctionalInterface private interface Acc { void put(int x, int y, int z, String state); }

    private void ensureTask() {
        if (task == null && !jobs.isEmpty()) task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    void cancel(Job j) {
        if (j == null || j.done || j.cancelled) return;
        j.cancelled = true;
        jobs.remove(j);
        // blocks may have been written already: remember both sets so the next paste of the slot clears them
        if (j.now != null) {
            Set<Long> u = new HashSet<>(j.now);
            if (j.old != null) u.addAll(j.old);
            pasted.put(j.slot, u);
        }
        if (debugTiming) plugin.getLogger().info("Race paste " + j.label + " cancelled at " + Math.round(j.progress() * 100) + "%");
    }

    void cancelAll() {
        for (Job j : new ArrayList<>(jobs)) cancel(j);
        if (task != null) { task.cancel(); task = null; }
    }

    /** Drop the chunk tickets of every slot not in {@code keep}. */
    void release(Set<Integer> keep) {
        for (var it = tickets.entrySet().iterator(); it.hasNext(); ) {
            var e = it.next();
            if (keep.contains(e.getKey())) continue;
            boolean busy = false;
            for (Job j : jobs) if (j.slot == e.getKey()) busy = true;
            if (busy) continue;
            World w = Bukkit.getWorld(RacePlugin.WORLD);
            if (w != null) for (long k : e.getValue()) w.removePluginChunkTicket((int) (k >> 32), (int) k, plugin);
            it.remove();
        }
    }

    void releaseAll() { release(Set.of()); }

    /**
     * Keep chunk (cx, cz) loaded and entity-ticking for as long as the slot's tickets are held (released together
     * with the slot's paste tickets by {@link #release}). Used for the goal pen / decoy chunks, which can be far
     * outside every player's view distance (Kite Festival: ~350 blocks from the gantry). Loads the chunk
     * synchronously if needed (a handful of chunks, once per round). Returns true if the chunk is loaded now.
     */
    boolean hold(World w, int slot, int cx, int cz) {
        long k = (((long) cx) << 32) | (cz & 0xFFFFFFFFL);
        Chunk ch = w.getChunkAt(cx, cz);       // loads it if it is not
        ch.addPluginChunkTicket(plugin);
        tickets.computeIfAbsent(slot, s -> new HashSet<>()).add(k);
        return w.isChunkLoaded(cx, cz);
    }

    /** Does the slot hold a ticket on chunk (cx, cz)? (for /race info) */
    boolean holds(int slot, int cx, int cz) {
        Set<Long> s = tickets.get(slot);
        return s != null && s.contains((((long) cx) << 32) | (cz & 0xFFFFFFFFL));
    }

    private void tick() {
        if (jobs.isEmpty()) { if (task != null) { task.cancel(); task = null; } return; }
        long t0 = System.nanoTime(), deadline = t0 + budgetNanos;
        Job j = jobs.get(0);   // one job at a time, in order
        if (j.failed) { jobs.remove(j); j.done = true; finish(j, false); return; }
        if (!j.prepared()) return;
        if (j.datas == null) {
            j.datas = new BlockData[j.states.length];
            for (int i = 0; i < j.states.length; i++) j.datas[i] = placer.data(j.states[i]);
        }
        // keep a few async chunk loads in flight
        while (j.inFlight < maxInFlight && j.requested < j.chunkKeys.length) request(j, j.requested++);
        long placedThisTick = 0;
        int n = 0;
        outer:
        while (true) {
            if (j.curChunk < 0) {
                Integer next = j.ready.poll();
                if (next == null) break;
                if (next < 0) { j.chunksDone++; continue; }
                j.curChunk = next; j.curOp = 0;
                long k = j.chunkKeys[next];
                j.curChunkObj = j.world.isChunkLoaded((int) (k >> 32), (int) k) ? j.world.getChunkAt((int) (k >> 32), (int) k) : null;
                if (j.curChunkObj == null) {        // unloaded again (should not happen with a ticket): load it again
                    j.curChunk = -1;
                    request(j, next);
                    continue;
                }
            }
            int[] a = j.ops[j.curChunk];
            int cnt = j.opCount[j.curChunk];
            Chunk ch = j.curChunkObj;
            while (j.curOp < cnt) {
                int b = j.curOp * 4;
                ch.getBlock(a[b] & 15, a[b + 1], a[b + 2] & 15).setBlockData(j.datas[a[b + 3]], false);
                j.curOp++; j.blocks++; placedThisTick++;
                if ((++n & 255) == 0 && System.nanoTime() > deadline) break outer;
            }
            j.ops[j.curChunk] = null;               // free memory as we go
            j.curChunk = -1; j.curChunkObj = null;
            j.chunksDone++;
            if (System.nanoTime() > deadline) break;
        }
        long dt = System.nanoTime() - t0;
        j.ticks++; j.nanos += dt; j.maxNanos = Math.max(j.maxNanos, dt);
        if (debugTiming && (placedThisTick > 0 || j.ticks % 20 == 0))
            plugin.getLogger().info(String.format("Race paste %s tick %d: %.1f ms, %d blocks, chunks %d/%d (%d loading), %.0f%%",
                    j.label, j.ticks, dt / 1e6, placedThisTick, j.chunksDone, j.chunkKeys.length, j.inFlight, j.progress() * 100));
        if (j.chunksDone >= j.chunkKeys.length && j.curChunk < 0) {
            jobs.remove(j);
            pasted.put(j.slot, j.now);
            j.done = true;
            finish(j, true);
        }
    }

    /** Async load of chunk {@code idx} of the job; it is ticketed and queued for writing when it arrives (main thread). */
    private void request(Job j, int idx) {
        long k = j.chunkKeys[idx];
        int cx = (int) (k >> 32), cz = (int) k;
        j.inFlight++;
        j.world.getChunkAtAsync(cx, cz, true).whenComplete((ch, err) -> {
            j.inFlight--;
            if (j.cancelled) return;
            if (ch == null) {
                plugin.getLogger().warning("Race paste " + j.label + ": chunk " + cx + "," + cz + " failed to load: " + err);
                j.ready.add(-idx - 1);              // skip it
                return;
            }
            ch.addPluginChunkTicket(plugin);
            tickets.computeIfAbsent(j.slot, s -> new HashSet<>()).add(k);
            j.ready.add(idx);
        });
    }

    private void finish(Job j, boolean ok) {
        long wall = (System.nanoTime() - j.started) / 1_000_000;
        plugin.getLogger().info(String.format("Race paste %s %s: %d blocks in %d chunks, %d ticks, %d ms wall, %.0f ms work (max %.1f ms/tick)",
                j.label, ok ? "done" : "FAILED", j.blocks, j.chunkKeys == null ? 0 : j.chunkKeys.length, j.ticks, wall, j.nanos / 1e6, j.maxNanos / 1e6));
        for (Runnable r : j.onDone) {
            try { r.run(); } catch (Throwable t) { plugin.getLogger().severe("Race paste callback threw: " + t); }
        }
        j.onDone.clear();
        if (jobs.isEmpty() && task != null) { task.cancel(); task = null; }
    }
}
