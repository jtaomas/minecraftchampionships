package com.mcm.games.hungergames;

import com.mcm.games.hungergames.map.SgCanvas;
import com.mcm.games.hungergames.map.SgMap;
import com.mcm.games.hungergames.map.SgMapRegistry;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Async tick-spread builder driven by {@link SgMap} and {@link SgCanvas}.
 * Tracks previous non-air positions via a compact BitSet so switching maps clears old structures cleanly.
 */
public final class ArenaBuilder {
    public static final int DEFAULT_RADIUS = 124;
    public static final int BASE_Y = 64;
    private static final int BLOCKS_PER_TICK = 35_000;

    private final BitSet prevOccupied = new BitSet(SgCanvas.SIZE * SgCanvas.HEIGHT * SgCanvas.SIZE);
    private final List<Location> pedestals = new ArrayList<>();
    private final List<SgCanvas.ChestEntry> chests = new ArrayList<>();
    private final List<SgCanvas.AirdropSpot> airdrops = new ArrayList<>();
    private SgMap activeMap;
    private BukkitTask buildTask;

    public List<Location> getPedestals() {
        return pedestals;
    }

    public List<SgCanvas.ChestEntry> getChests() {
        return chests;
    }

    public List<SgCanvas.AirdropSpot> getAirdrops() {
        return airdrops;
    }

    public SgMap getActiveMap() {
        return activeMap;
    }

    public void cancel() {
        if (buildTask != null) {
            buildTask.cancel();
            buildTask = null;
        }
    }

    public void buildAsync(Plugin plugin, World world, String mapId, Runnable onComplete) {
        cancel();
        pedestals.clear();
        chests.clear();
        airdrops.clear();

        activeMap = SgMapRegistry.byId(mapId);
        SgCanvas canvas = new SgCanvas();
        activeMap.generate(canvas);

        pedestals.addAll(canvas.toPedestalLocations(world));
        chests.addAll(canvas.getChests());
        airdrops.addAll(canvas.getAirdrops());

        List<SgCanvas.BlockEdit> edits = canvas.exportDiff(prevOccupied);
        final int[] index = {0};

        buildTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int end = Math.min(index[0] + BLOCKS_PER_TICK, edits.size());
            for (int i = index[0]; i < end; i++) {
                SgCanvas.BlockEdit e = edits.get(i);
                Block b = world.getBlockAt(e.x(), e.y(), e.z());
                if (b.getType() != e.mat()) {
                    b.setType(e.mat(), false);
                }
            }
            index[0] = end;
            if (index[0] >= edits.size()) {
                cancel();
                refillChests(world);
                onComplete.run();
            }
        }, 1L, 1L);
    }

    public void refillChests(World world) {
        for (SgCanvas.ChestEntry entry : chests) {
            Block b = world.getBlockAt(entry.x(), entry.y(), entry.z());
            if (b.getType() != Material.CHEST) {
                b.setType(Material.CHEST, false);
            }
            if (b.getState() instanceof Chest chest) {
                LootTable.fill(chest.getBlockInventory(), entry.tier());
            }
        }
    }

    public int findSafeTopY(World world, int x, int z) {
        for (int y = SgCanvas.MAX_Y; y >= SgCanvas.MIN_Y; y--) {
            Material m = world.getBlockAt(x, y, z).getType();
            if (m != Material.AIR && m != Material.WATER && m != Material.OAK_LEAVES
                    && m != Material.BIRCH_LEAVES && m != Material.SPRUCE_LEAVES) {
                return y;
            }
        }
        return BASE_Y;
    }
}
