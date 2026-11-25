package com.mcm.games.rocketspleef.map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An imported Rocket Spleef Rush arena loaded from {@code imported-arenas/<id>/}.
 * Stores compact local block coordinates and palette indices; resolves {@link BlockData}
 * on the main thread when built into a world.
 */
public final class ImportedRsArena implements RocketArenaMap {
    private final String id;
    private final String displayName;
    private final int baseY;
    private final int[] size;
    private final List<int[]> spawns;
    private final String[] palette;
    private final short[] xs;
    private final short[] ys;
    private final short[] zs;
    private final int[] palIdx;
    private final int removedBlocks;
    private final int maxHorizRadius;

    public ImportedRsArena(String id, String displayName, int baseY, int[] size,
                           List<int[]> spawns, String[] palette,
                           short[] xs, short[] ys, short[] zs, int[] palIdx,
                           int removedBlocks, int maxHorizRadius) {
        this.id = id;
        this.displayName = displayName;
        this.baseY = baseY;
        this.size = size;
        this.spawns = List.copyOf(spawns);
        this.palette = palette;
        this.xs = xs;
        this.ys = ys;
        this.zs = zs;
        this.palIdx = palIdx;
        this.removedBlocks = removedBlocks;
        this.maxHorizRadius = maxHorizRadius;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public boolean supportsDynamicPads() {
        return false;
    }

    public int baseY() {
        return baseY;
    }

    public int[] size() {
        return size.clone();
    }

    public int blockCount() {
        return xs.length;
    }

    public int spawnCount() {
        return spawns.size();
    }

    public int removedBlocks() {
        return removedBlocks;
    }

    public int maxHorizRadius() {
        return maxHorizRadius;
    }

    @Override
    public void generate(RsCanvas canvas, int scaleRadius) {
        // Imported arenas bypass RsCanvas and paste BlockData directly in build().
    }

    @Override
    public BuildResult build(World world, int scaleRadius) {
        BlockData[] resolved = new BlockData[palette.length];
        for (int i = 0; i < palette.length; i++) {
            resolved[i] = resolveBlockData(palette[i]);
        }

        int minH = world != null ? world.getMinHeight() : -64;
        int maxH = world != null ? world.getMaxHeight() : 320;

        List<RsCanvas.Pos> placed = new ArrayList<>(xs.length);
        for (int i = 0; i < xs.length; i++) {
            int wx = xs[i];
            int wy = baseY + ys[i];
            int wz = zs[i];
            if (wy < minH || wy >= maxH) continue;

            BlockData data = resolved[palIdx[i]];
            if (data == null || data.getMaterial().isAir()) continue;

            if (world != null) {
                world.getBlockAt(wx, wy, wz).setBlockData(data, false);
            }
            placed.add(new RsCanvas.Pos(wx, wy, wz));
        }

        // Sort outer-to-inner with slight jitter for late-round & overtime crumbling
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        record Weighted(RsCanvas.Pos pos, double score) {}
        List<Weighted> weighted = new ArrayList<>(placed.size());
        for (RsCanvas.Pos p : placed) {
            double dist = Math.hypot(p.x(), p.z()) + rng.nextDouble(-2.5, 2.5);
            weighted.add(new Weighted(p, dist));
        }
        weighted.sort(Comparator.comparingDouble(Weighted::score).reversed());
        List<RsCanvas.Pos> crumbleOrder = new ArrayList<>(weighted.size());
        for (Weighted w : weighted) {
            crumbleOrder.add(w.pos());
        }

        List<Location> spawnLocs = new ArrayList<>(spawns.size());
        for (int[] s : spawns) {
            float yaw = (float) Math.toDegrees(Math.atan2(s[0], -s[2]));
            spawnLocs.add(new Location(world, s[0] + 0.5, baseY + s[1] + 0.05, s[2] + 0.5, yaw, 0f));
        }
        if (spawnLocs.isEmpty()) {
            spawnLocs.add(new Location(world, 0.5, baseY + 10.05, 0.5));
        }

        return new BuildResult(spawnLocs, crumbleOrder);
    }

    private static BlockData resolveBlockData(String state) {
        try {
            return Bukkit.createBlockData(state);
        } catch (IllegalArgumentException ignored) {
            int bracket = state.indexOf('[');
            String base = bracket < 0 ? state : state.substring(0, bracket);
            try {
                return Bukkit.createBlockData(base);
            } catch (IllegalArgumentException ignored2) {
                Material mat = Material.matchMaterial(base);
                if (mat != null && mat.isBlock()) {
                    return mat.createBlockData();
                }
                return Material.STONE.createBlockData();
            }
        }
    }
}
