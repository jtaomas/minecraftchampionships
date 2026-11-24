package com.mcm.games.acerace;

import com.mcm.api.McmBuild;
import com.mcm.games.acerace.map.RaceBlocks;
import com.mcm.games.acerace.map.RaceGeo.Pos;
import com.mcm.games.acerace.map.RaceGeo.Region;
import com.mcm.games.acerace.map.RaceMapData;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Owns the single mcc_acerace void world and its fixed map slots. A map is pasted into its slot
 * once per session with a tick-budgeted BuildQueue; static maps are not wiped between games.
 */
public final class RaceWorld {
    private static final String TAG = "acerace";

    private final Plugin plugin;
    private final String worldName;
    private final int baseY;
    private final int slotSpacingX;
    private final int slotSpacingZ;
    private final int blocksPerTick;

    private World world;
    private final Map<Integer, Set<Long>> pasted = new HashMap<>();
    private final Map<Integer, Set<Long>> ticketed = new HashMap<>();
    private final Map<Integer, String> built = new HashMap<>();
    private final Map<String, BlockData> dataCache = new HashMap<>();

    public RaceWorld(Plugin plugin) {
        this.plugin = plugin;
        this.worldName = plugin.getConfig().getString("world-name", "mcc_acerace");
        this.baseY = plugin.getConfig().getInt("base-y", 80);
        this.slotSpacingX = plugin.getConfig().getInt("slot-spacing-x", 1800);
        this.slotSpacingZ = plugin.getConfig().getInt("slot-spacing-z", 1400);
        this.blocksPerTick = plugin.getConfig().getInt("blocks-per-tick", 20_000);
    }

    public void ensureWorld() {
        world = McmBuild.voidWorld(worldName);
        if (world == null) {
            plugin.getLogger().severe("Ace Race: could not create/load world " + worldName);
            return;
        }
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setPVP(false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setSpawnLocation(0, baseY + 1, 0);
    }

    public World world() {
        return world;
    }

    public int baseY() {
        return baseY;
    }

    public int slotX(int slot) {
        return slot * slotSpacingX;
    }

    public int slotZ(int slot) {
        return 0;
    }

    public Location toWorld(int slot, double x, double y, double z, float yaw) {
        return new Location(world, slotX(slot) + x, baseY + y, slotZ(slot) + z, yaw, 0f);
    }

    public Location toWorld(int slot, Pos p) {
        return new Location(world, slotX(slot) + p.x(), baseY + p.y(), slotZ(slot) + p.z());
    }

    public boolean isBuilt(int slot, RaceMapData data) {
        return data != null && data.mapId.equals(built.get(slot));
    }

    /**
     * Paste a map into its slot once. {@code done} runs on the main thread when every block is set.
     * If the slot already holds this map, it runs immediately.
     */
    public void paste(int slot, RaceMapData data, Runnable done) {
        if (world == null || data == null) {
            if (done != null) done.run();
            return;
        }
        if (isBuilt(slot, data)) {
            if (done != null) done.run();
            return;
        }

        final int ox = slotX(slot), oy = baseY, oz = slotZ(slot);
        final Set<Long> old = pasted.getOrDefault(slot, Set.of());
        final Set<Long> now = new HashSet<>();

        McmBuild.BuildQueue queue = new McmBuild.BuildQueue(plugin, Math.max(1, blocksPerTick));
        data.blocks.forEach((x, y, z, state) -> {
            long key = RaceBlocks.key(x, y, z);
            now.add(key);
            BlockData bd = blockData(state);
            if (bd != null) queue.set(world, ox + x, oy + y, oz + z, bd);
        });

        // Clear blocks from a previous paste at this slot that the new map does not use.
        for (long k : old) {
            if (now.contains(k)) continue;
            int x = RaceBlocks.x(k), y = RaceBlocks.y(k), z = RaceBlocks.z(k);
            queue.set(world, ox + x, oy + y, oz + z, Material.AIR);
        }

        addTickets(slot, data.playBounds);
        pasted.put(slot, now);
        built.remove(slot);
        queue.then(() -> {
            built.put(slot, data.mapId);
            if (done != null) done.run();
        }).start();
    }

    /** Clear a slot's pasted blocks and allow a fresh paste. */
    public void clearSlot(int slot, Runnable done) {
        if (world == null) {
            if (done != null) done.run();
            return;
        }
        Set<Long> old = pasted.remove(slot);
        built.remove(slot);
        if (old == null || old.isEmpty()) {
            if (done != null) done.run();
            return;
        }
        int ox = slotX(slot), oy = baseY, oz = slotZ(slot);
        McmBuild.BuildQueue queue = new McmBuild.BuildQueue(plugin, Math.max(1, blocksPerTick));
        for (long k : old) {
            queue.set(world, ox + RaceBlocks.x(k), oy + RaceBlocks.y(k), oz + RaceBlocks.z(k), Material.AIR);
        }
        queue.then(done).start();
    }

    private void addTickets(int slot, Region bounds) {
        if (bounds == null) return;
        Set<Long> keys = ticketed.computeIfAbsent(slot, k -> new HashSet<>());
        for (int cx = (int) Math.floor(bounds.minX / 16.0); cx <= (int) Math.floor(bounds.maxX / 16.0); cx++) {
            for (int cz = (int) Math.floor(bounds.minZ / 16.0); cz <= (int) Math.floor(bounds.maxZ / 16.0); cz++) {
                long key = ((long) cx << 32) ^ (cz & 0xFFFFFFFFL);
                if (keys.add(key)) world.addPluginChunkTicket(cx, cz, plugin);
            }
        }
    }

    private BlockData blockData(String state) {
        if (state == null) return null;
        BlockData cached = dataCache.get(state);
        if (cached != null) return cached;
        try {
            BlockData bd = Bukkit.createBlockData(state);
            dataCache.put(state, bd);
            return bd;
        } catch (Throwable t) {
            plugin.getLogger().warning("Ace Race: invalid block state '" + state + "': " + t.getMessage());
            return null;
        }
    }

    public void disable() {
        if (world != null) {
            for (Map.Entry<Integer, Set<Long>> e : ticketed.entrySet()) {
                for (long key : e.getValue()) {
                    int cx = (int) (key >> 32);
                    int cz = (int) key;
                    world.removePluginChunkTicket(cx, cz, plugin);
                }
            }
        }
        ticketed.clear();
        pasted.clear();
        built.clear();
        dataCache.clear();
    }
}
