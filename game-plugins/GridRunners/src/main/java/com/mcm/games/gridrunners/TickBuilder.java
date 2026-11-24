package com.mcm.games.gridrunners;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.function.LongPredicate;
import java.util.logging.Level;

/**
 * Runs queued build jobs on the main thread, spread over ticks: each tick it keeps running jobs until
 * {@code budgetMs} of work has been done (a single job may overshoot by its own size, so jobs are kept small).
 * A job may also be resumable: it gets the tick deadline (System.nanoTime) and returns true once it is finished.
 * {@link #cancel()} drops everything, including the completion callback; safe to call any time, repeatedly.
 */
final class TickBuilder {
    private final Plugin plugin;
    private final String name;
    private final long budgetNanos;
    private final ArrayDeque<LongPredicate> jobs = new ArrayDeque<>();
    private BukkitTask task;
    private Runnable onDone;
    private int generation;
    private boolean debug;
    private int ticks;
    private long maxNanos, totalNanos;

    TickBuilder(Plugin plugin, String name, int budgetMs) {
        this.plugin = plugin;
        this.name = name;
        this.budgetNanos = budgetMs * 1_000_000L;
    }

    /** One-shot job. */
    TickBuilder add(Runnable r) {
        jobs.add(deadline -> { r.run(); return true; });
        return this;
    }

    /** Resumable job: called with the tick deadline until it returns true. */
    TickBuilder addStep(LongPredicate step) {
        jobs.add(step);
        return this;
    }

    /** Start processing next tick; {@code done} runs once after the last job (not if cancelled). */
    void start(Runnable done, boolean debugTiming) {
        this.onDone = done;
        this.debug = debugTiming;
        ticks = 0; maxNanos = 0; totalNanos = 0;
        if (task != null || !plugin.isEnabled()) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    boolean isRunning() { return task != null; }

    void cancel() {
        generation++;
        if (task != null) { task.cancel(); task = null; }
        jobs.clear();
        onDone = null;
    }

    private void tick() {
        int gen = generation;
        long start = System.nanoTime();
        long deadline = start + budgetNanos;
        int ran = 0;
        while (!jobs.isEmpty() && System.nanoTime() < deadline) {
            LongPredicate job = jobs.peekFirst();
            boolean finished;
            try {
                finished = job.test(deadline);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, name + " build job failed", t);
                finished = true;
            }
            if (gen != generation) return; // cancelled from inside a job
            if (finished) jobs.pollFirst();
            ran++;
        }
        long spent = System.nanoTime() - start;
        ticks++;
        totalNanos += spent;
        maxNanos = Math.max(maxNanos, spent);
        if (debug) {
            plugin.getLogger().info(String.format("[debug-timing] %s tick %d: %.2f ms, %d job step(s), %d left",
                    name, ticks, spent / 1e6, ran, jobs.size()));
        }
        if (jobs.isEmpty()) {
            if (task != null) { task.cancel(); task = null; }
            if (debug) {
                plugin.getLogger().info(String.format("[debug-timing] %s done: %d ticks, %.1f ms total, worst tick %.2f ms",
                        name, ticks, totalNanos / 1e6, maxNanos / 1e6));
            }
            Runnable r = onDone;
            onDone = null;
            if (r != null) r.run();
        }
    }
}
