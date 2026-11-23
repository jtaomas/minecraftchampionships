package com.mcm.games.railroadrush;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.logging.Level;

/** Runs block-placing steps spread over ticks. */
final class BlockJobs {
    private final Plugin plugin;
    private final ArrayDeque<Runnable> queue = new ArrayDeque<>();
    private BukkitTask task;
    private Runnable done;

    BlockJobs(Plugin plugin) { this.plugin = plugin; }

    void add(Runnable r) { queue.add(r); }

    void start(int stepsPerTick, Runnable onDone) {
        done = onDone;
        if (task != null) task.cancel();
        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int n = 0;
            while (n < stepsPerTick && !queue.isEmpty()) {
                Runnable r = queue.poll();
                try { r.run(); } catch (Exception ex) { plugin.getLogger().log(Level.WARNING, "Arena build step failed", ex); }
                n++;
            }
            if (queue.isEmpty()) {
                BukkitTask t = task;
                task = null;
                if (t != null) t.cancel();
                Runnable d = done;
                done = null;
                if (d != null) d.run();
            }
        }, 1L, 1L);
    }

    void cancel() {
        queue.clear();
        if (task != null) { task.cancel(); task = null; }
        done = null;
    }
}
