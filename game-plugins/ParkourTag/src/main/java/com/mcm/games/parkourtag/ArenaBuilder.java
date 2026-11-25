package com.mcm.games.parkourtag;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Places and clears arena copies spread over several ticks. */
final class ArenaBuilder {
    private static final int BLOCKS_PER_TICK = 30000;

    private interface Job { boolean step(int[] budget); }

    private final Plugin plugin;
    private final Deque<Job> jobs = new ArrayDeque<>();
    private final List<Runnable> whenIdle = new ArrayList<>();
    private BukkitTask task;

    /** One block of a map, already parsed. */
    record Placement(int x, int y, int z, org.bukkit.block.data.BlockData data) {}

    ArenaBuilder(Plugin plugin) { this.plugin = plugin; }

    /** Sets every non-air block in the box [x0..x1] x [y0..y1] x [z0..z1] to air. */
    void clear(World world, int x0, int y0, int z0, int x1, int y1, int z1) {
        jobs.add(new Job() {
            int x = x0, y = y0, z = z0;
            @Override public boolean step(int[] budget) {
                if (!world.equals(Bukkit.getWorld(world.getUID()))) return true;
                while (budget[0] > 0) {
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                    budget[0]--;
                    if (++z > z1) { z = z0; if (++x > x1) { x = x0; if (++y > y1) return true; } }
                }
                return false;
            }
        });
        ensureTask();
    }

    void build(World world, int ox, int oy, int oz, List<Placement> bp) {
        jobs.add(new Job() {
            int i = 0;
            @Override public boolean step(int[] budget) {
                if (!world.equals(Bukkit.getWorld(world.getUID()))) return true;
                while (budget[0] > 0 && i < bp.size()) {
                    Placement p = bp.get(i++);
                    world.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setBlockData(p.data(), false);
                    budget[0]--;
                }
                return i >= bp.size();
            }
        });
        ensureTask();
    }

    /** Runs r once the queue is empty (on a later tick). */
    void whenIdle(Runnable r) {
        whenIdle.add(r);
        ensureTask();
    }

    void cancelCallbacks() { whenIdle.clear(); }

    boolean busy() { return !jobs.isEmpty(); }

    private void ensureTask() {
        if (task != null && !task.isCancelled()) return;
        if (!plugin.isEnabled()) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        int[] budget = {BLOCKS_PER_TICK};
        while (budget[0] > 0 && !jobs.isEmpty()) {
            Job j = jobs.peek();
            boolean done;
            try {
                done = j.step(budget);
            } catch (Throwable t) {
                plugin.getLogger().warning("Arena build job failed: " + t);
                done = true;
            }
            if (done) jobs.poll();
        }
        if (jobs.isEmpty()) {
            List<Runnable> callbacks = new ArrayList<>(whenIdle);
            whenIdle.clear();
            if (task != null) { task.cancel(); task = null; }
            for (Runnable r : callbacks) {
                try { r.run(); } catch (Throwable t) { plugin.getLogger().warning("Arena callback failed: " + t); }
            }
        }
    }
}
