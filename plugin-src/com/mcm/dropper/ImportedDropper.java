package com.mcm.dropper;

import java.util.List;

/**
 * One dropper shaft imported from a community map, loaded at runtime from
 * {@code plugins/MinecraftMondayCore/imported-droppers/<id>/} (never from the jar or the repository).
 * Coordinates in this object are map-local; the shaft is pasted at {@code (ox, oy, oz)} in {@code dropper_world}.
 * <p>
 * Stage rules (Multi-Stage Dropper): the player starts in the air above the shaft at {@link #spawn} and completes
 * the stage by landing in the water pool at the bottom ({@link #inPool}). Falling {@link #lethalFall} blocks or more
 * onto anything else, lava/fire, the void ({@link #voidY}) or leaving {@link #playBounds} sends them back to the start.
 */
public final class ImportedDropper {
    public final String id, name, credit, folderName, hash;
    public final float yaw;
    /** Fall height (blocks) that counts as a death on landing (not in water). 0 = any fall damage counts (built-in rule). */
    public final int lethalFall;
    /** Local y below which a player is respawned. */
    public final int voidY;
    /** Local box {minX, minY, minZ, maxX, maxY, maxZ}: leaving it (horizontally, or below minY) respawns the player. */
    public final int[] playBounds;
    /** Local feet positions {x, y, z} (block centres) of the start strip; index 0 is the strip centre. */
    public final List<double[]> spawns;
    /** Local pool box: water cells of the bottom pool lie within x/z and y {@code poolBottomY..poolTopY}. */
    public final int poolMinX, poolMaxX, poolMinZ, poolMaxZ, poolBottomY, poolTopY, poolWater;
    /** Local bounds of every pasted block. */
    public final int minX, minY, minZ, maxX, maxY, maxZ;
    /** Loader report counters (for the log and the validation). */
    public final int sealBlocks, removedBlocks, barrierBlocks;
    // ---- block data (sorted by key, see ImportedDropperLoader.key); dropped after a successful paste
    long[] keys;
    int[] pal;
    final String[] palette;
    public final int blockCount;
    /** A few (key, palette index) pairs kept for the "is it still pasted?" spot check. */
    final long[] sampleKeys;
    final int[] samplePal;

    // ---- placement (main thread)
    int slot = -1;
    int ox, oy, oz;
    volatile boolean ready;

    ImportedDropper(String id, String name, String credit, String folderName, String hash, float yaw, int lethalFall, int voidY,
                    int[] playBounds, List<double[]> spawns, int[] pool, int poolWater, int[] bounds,
                    int sealBlocks, int removedBlocks, int barrierBlocks,
                    long[] keys, int[] pal, String[] palette, long[] sampleKeys, int[] samplePal) {
        this.id = id; this.name = name; this.credit = credit; this.folderName = folderName; this.hash = hash;
        this.yaw = yaw; this.lethalFall = lethalFall; this.voidY = voidY; this.playBounds = playBounds;
        this.spawns = List.copyOf(spawns);
        this.poolMinX = pool[0]; this.poolBottomY = pool[1]; this.poolMinZ = pool[2];
        this.poolMaxX = pool[3]; this.poolTopY = pool[4]; this.poolMaxZ = pool[5];
        this.poolWater = poolWater;
        this.minX = bounds[0]; this.minY = bounds[1]; this.minZ = bounds[2];
        this.maxX = bounds[3]; this.maxY = bounds[4]; this.maxZ = bounds[5];
        this.sealBlocks = sealBlocks; this.removedBlocks = removedBlocks; this.barrierBlocks = barrierBlocks;
        this.keys = keys; this.pal = pal; this.palette = palette; this.blockCount = keys.length;
        this.sampleKeys = sampleKeys; this.samplePal = samplePal;
    }

    public boolean isReady() { return ready; }
    public int slot() { return slot; }
    public int ox() { return ox; }
    public int oy() { return oy; }
    public int oz() { return oz; }

    /** World feet position of the start (strip centre). */
    public double[] spawn() {
        double[] s = spawns.get(0);
        return new double[]{ox + s[0], oy + s[1], oz + s[2]};
    }

    /** World centre of the shaft (x, z) - the middle of the pool. */
    public double centreX() { return ox + (poolMinX + poolMaxX) / 2.0 + 0.5; }
    public double centreZ() { return oz + (poolMinZ + poolMaxZ) / 2.0 + 0.5; }

    /** True if world block (bx, by, bz) lies in the pool box (1 block of slack around it, 1 above the surface). */
    public boolean inPool(int bx, int by, int bz) {
        int x = bx - ox, y = by - oy, z = bz - oz;
        return x >= poolMinX - 1 && x <= poolMaxX + 1 && z >= poolMinZ - 1 && z <= poolMaxZ + 1
                && y >= poolBottomY && y <= poolTopY + 1;
    }

    /** True if world position is outside the play bounds horizontally, or below the void line / bounds floor. */
    public boolean outOfBounds(double wx, double wy, double wz) {
        double x = wx - ox, y = wy - oy, z = wz - oz;
        return y < voidY || y < playBounds[1] || x < playBounds[0] || x > playBounds[3] + 1 || z < playBounds[2] || z > playBounds[5] + 1;
    }

    /** World bounding box of the pasted blocks {x1, y1, z1, x2, y2, z2}. */
    public int[] worldBounds() {
        return new int[]{ox + minX, oy + minY, oz + minZ, ox + maxX, oy + maxY, oz + maxZ};
    }
}
