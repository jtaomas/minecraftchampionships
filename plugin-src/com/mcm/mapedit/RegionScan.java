package com.mcm.mapedit;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * The tracked box of one edit session (world coordinates) and the machinery to read it: chunk loading with tickets,
 * a baseline snapshot of the freshly pasted map (palette-compressed, 8 KB per non-empty 16^3 section), and the
 * block diff between the baseline and the world. Snapshots are taken on the main thread a few per tick; the heavy
 * per-cell work runs on a worker thread.
 */
final class RegionScan {
    /** Global block-state palette of a session. Index 0 is air (every air variant). */
    static final class Palette {
        private final HashMap<BlockData, Character> ids = new HashMap<>();
        private final ArrayList<BlockData> list = new ArrayList<>();
        synchronized char id(BlockData d) {
            Character c = ids.get(d);
            if (c != null) return c;
            if (list.size() >= 0xFFFE) throw new IllegalStateException("more than 65000 distinct block states in the edit area");
            char n = (char) (list.size() + 1);
            list.add(d);
            ids.put(d, n);
            return n;
        }
        synchronized BlockData get(char c) { return c == 0 ? null : list.get(c - 1); }
        synchronized int size() { return list.size(); }
    }

    final World world;
    /** Inclusive world box, already clipped to the world's height. */
    final int minX, minY, minZ, maxX, maxY, maxZ;
    final int cx0, cx1, cz0, cz1, secLo, secHi, worldSecLo;
    final Palette palette = new Palette();
    /** chunk key -> sections (index = absolute section - secLo); a null section is all air. */
    final Map<Long, char[][]> baseline = new ConcurrentHashMap<>();

    RegionScan(World w, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.world = w;
        this.minX = minX; this.maxX = maxX; this.minZ = minZ; this.maxZ = maxZ;
        this.minY = Math.max(minY, w.getMinHeight());
        this.maxY = Math.min(maxY, w.getMaxHeight() - 1);
        cx0 = minX >> 4; cx1 = maxX >> 4; cz0 = minZ >> 4; cz1 = maxZ >> 4;
        secLo = Math.floorDiv(this.minY, 16); secHi = Math.floorDiv(this.maxY, 16);
        worldSecLo = Math.floorDiv(w.getMinHeight(), 16);
    }

    int chunkCount() { return (cx1 - cx0 + 1) * (cz1 - cz0 + 1); }
    long cells() { return (long) (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1); }
    int sections() { return secHi - secLo + 1; }

    static long ck(int cx, int cz) { return (((long) cx) << 32) | (cz & 0xFFFFFFFFL); }
    static int ckx(long k) { return (int) (k >> 32); }
    static int ckz(long k) { return (int) k; }

    List<Long> chunkKeys() {
        List<Long> out = new ArrayList<>(chunkCount());
        for (int cx = cx0; cx <= cx1; cx++) for (int cz = cz0; cz <= cz1; cz++) out.add(ck(cx, cz));
        return out;
    }

    // ------------------------------------------------------------------ reading a chunk snapshot (worker-safe)

    char[][] scan(ChunkSnapshot s, int cx, int cz) {
        int n = sections();
        char[][] out = new char[n][];
        int xLo = Math.max(minX, cx << 4) & 15, xHi = Math.min(maxX, (cx << 4) + 15) & 15;
        int zLo = Math.max(minZ, cz << 4) & 15, zHi = Math.min(maxZ, (cz << 4) + 15) & 15;
        BlockData lastD = null; char lastId = 0;
        for (int si = 0; si < n; si++) {
            int sy = secLo + si;
            if (s.isSectionEmpty(sy - worldSecLo)) continue;
            int y0 = Math.max(minY, sy << 4), y1 = Math.min(maxY, (sy << 4) + 15);
            char[] arr = new char[4096];
            boolean any = false;
            for (int y = y0; y <= y1; y++) {
                for (int z = zLo; z <= zHi; z++) {
                    for (int x = xLo; x <= xHi; x++) {
                        BlockData d = s.getBlockData(x, y, z);
                        char id;
                        if (d.getMaterial().isAir()) id = 0;
                        else if (d.equals(lastD)) id = lastId;
                        else { id = palette.id(d); lastD = d; lastId = id; }
                        if (id != 0) { arr[((y & 15) << 8) | (z << 4) | x] = id; any = true; }
                    }
                }
            }
            out[si] = any ? arr : null;
        }
        return out;
    }

    /** Receives changed cells (world coordinates, new palette id). */
    interface DiffSink { void changed(int x, int y, int z, char id); }

    /** Worker-safe: compare a freshly scanned chunk with its baseline. */
    void diff(char[][] cur, char[][] base, int cx, int cz, DiffSink sink) {
        int n = sections();
        int xLo = Math.max(minX, cx << 4) & 15, xHi = Math.min(maxX, (cx << 4) + 15) & 15;
        int zLo = Math.max(minZ, cz << 4) & 15, zHi = Math.min(maxZ, (cz << 4) + 15) & 15;
        for (int si = 0; si < n; si++) {
            char[] c = cur == null ? null : cur[si], b = base == null ? null : base[si];
            if (c == null && b == null) continue;
            if (c != null && b != null && Arrays.equals(c, b)) continue;
            int sy = secLo + si;
            int y0 = Math.max(minY, sy << 4), y1 = Math.min(maxY, (sy << 4) + 15);
            for (int y = y0; y <= y1; y++) {
                for (int z = zLo; z <= zHi; z++) {
                    for (int x = xLo; x <= xHi; x++) {
                        int i = ((y & 15) << 8) | (z << 4) | x;
                        char cv = c == null ? 0 : c[i], bv = b == null ? 0 : b[i];
                        if (cv != bv) sink.changed((cx << 4) + x, y, (cz << 4) + z, cv);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ jobs

    /** Chunk loading with plugin tickets, a few in flight; {@code done} runs on the main thread. */
    static final class LoadJob {
        private final Plugin plugin; private final World world; private final List<Long> keys; private final java.util.Set<Long> tickets;
        private int next, inFlight, finished, failed;
        private boolean complete;
        private final Runnable done;
        private final java.util.function.BooleanSupplier cancelled;
        private BukkitTask task;

        LoadJob(Plugin plugin, World world, List<Long> keys, java.util.Set<Long> tickets, java.util.function.BooleanSupplier cancelled, Runnable done) {
            this.plugin = plugin; this.world = world; this.keys = keys; this.tickets = tickets; this.cancelled = cancelled; this.done = done;
        }

        int finished() { return finished; }
        int total() { return keys.size(); }

        void start() {
            pump();
            if (!complete) task = Bukkit.getScheduler().runTaskTimer(plugin, this::pump, 1L, 1L);
        }

        private void pump() {
            if (complete) return;
            if (cancelled.getAsBoolean()) { complete = true; if (task != null) task.cancel(); return; }
            while (inFlight < 16 && next < keys.size()) {
                long k = keys.get(next++);
                int cx = ckx(k), cz = ckz(k);
                if (world.isChunkLoaded(cx, cz)) {
                    world.addPluginChunkTicket(cx, cz, plugin);
                    tickets.add(k);
                    finished++;
                    continue;
                }
                inFlight++;
                world.getChunkAtAsync(cx, cz, true).whenComplete((ch, err) -> {
                    inFlight--; finished++;
                    if (ch == null) { failed++; return; }
                    if (cancelled.getAsBoolean()) return;
                    ch.addPluginChunkTicket(plugin);
                    tickets.add(k);
                });
            }
            if (next >= keys.size() && inFlight == 0 && finished >= keys.size()) {
                complete = true;
                if (task != null) task.cancel();
                if (failed > 0) plugin.getLogger().warning("MapEdit: " + failed + " chunk(s) failed to load");
                done.run();
            }
        }
    }

    interface ChunkWork { void run(int cx, int cz, ChunkSnapshot snap); }

    /**
     * Takes snapshots of chunks a few per tick on the main thread and processes them on {@code exec}. Keeps at most
     * 24 snapshots queued. {@code done} runs on the main thread with the first error (or null).
     */
    static final class SnapJob {
        private final Plugin plugin; private final World world; private final List<Long> keys; private final ChunkWork work;
        private final Executor exec; private final Consumer<Throwable> done;
        private final java.util.function.BooleanSupplier cancelled;
        private final AtomicInteger inFlight = new AtomicInteger(), finished = new AtomicInteger();
        private volatile Throwable error;
        private int next;
        private BukkitTask task;
        private boolean complete;

        SnapJob(Plugin plugin, World world, List<Long> keys, Executor exec, java.util.function.BooleanSupplier cancelled, ChunkWork work, Consumer<Throwable> done) {
            this.plugin = plugin; this.world = world; this.keys = keys; this.exec = exec; this.cancelled = cancelled; this.work = work; this.done = done;
        }

        int finished() { return finished.get(); }
        int total() { return keys.size(); }

        void start() {
            tick();
            if (!complete) task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }

        private void tick() {
            if (complete) return;
            if (cancelled.getAsBoolean()) { complete = true; if (task != null) task.cancel(); return; }
            int perTick = 0;
            while (next < keys.size() && inFlight.get() < 24 && perTick < 10 && error == null) {
                long k = keys.get(next++);
                int cx = ckx(k), cz = ckz(k);
                Chunk ch = world.getChunkAt(cx, cz);
                ChunkSnapshot snap = ch.getChunkSnapshot(false, false, false);
                inFlight.incrementAndGet();
                perTick++;
                exec.execute(() -> {
                    try { work.run(cx, cz, snap); } catch (Throwable t) { if (error == null) error = t; }
                    finally { inFlight.decrementAndGet(); finished.incrementAndGet(); }
                });
            }
            boolean allSubmitted = next >= keys.size() || error != null;
            if (allSubmitted && inFlight.get() == 0) {
                complete = true;
                if (task != null) task.cancel();
                done.accept(error);
            }
        }
    }

    /** Same as {@link SnapJob} but on the calling (main) thread, for server shutdown. Returns the first error or null. */
    Throwable runSync(List<Long> keys, ChunkWork work) {
        for (long k : keys) {
            try {
                Chunk ch = world.getChunkAt(ckx(k), ckz(k));
                work.run(ckx(k), ckz(k), ch.getChunkSnapshot(false, false, false));
            } catch (Throwable t) { return t; }
        }
        return null;
    }
}
