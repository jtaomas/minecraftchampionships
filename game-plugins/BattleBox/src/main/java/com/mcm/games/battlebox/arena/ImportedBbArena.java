package com.mcm.games.battlebox.arena;

/**
 * Immutable parsed imported Battle Box arena loaded from {@code imported-arenas/<id>/}
 * ({@code map.yml} + {@code blocks.mcmi.gz}). Pure Java, no Bukkit calls.
 */
public final class ImportedBbArena {
    public final String id;
    public final String name;
    public final String folder;
    public final int sizeX, sizeY, sizeZ;
    public final int floorY;
    public final int centreX, centreY, centreZ;
    public final int spawnAX, spawnAY, spawnAZ;
    public final int spawnBX, spawnBY, spawnBZ;
    public final int minX, minY, minZ, maxX, maxY, maxZ;
    public final int blockCount;
    public final int removedBlocks;
    public final long[] keys;
    public final int[] pal;
    public final String[] palette;

    ImportedBbArena(
            String id, String name, String folder,
            int sizeX, int sizeY, int sizeZ, int floorY,
            int centreX, int centreY, int centreZ,
            int spawnAX, int spawnAY, int spawnAZ,
            int spawnBX, int spawnBY, int spawnBZ,
            int[] bounds, int removedBlocks,
            long[] keys, int[] pal, String[] palette) {
        this.id = id;
        this.name = name;
        this.folder = folder;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.floorY = floorY;
        this.centreX = centreX;
        this.centreY = centreY;
        this.centreZ = centreZ;
        this.spawnAX = spawnAX;
        this.spawnAY = spawnAY;
        this.spawnAZ = spawnAZ;
        this.spawnBX = spawnBX;
        this.spawnBY = spawnBY;
        this.spawnBZ = spawnBZ;
        this.minX = bounds[0];
        this.minY = bounds[1];
        this.minZ = bounds[2];
        this.maxX = bounds[3];
        this.maxY = bounds[4];
        this.maxZ = bounds[5];
        this.blockCount = keys.length;
        this.removedBlocks = removedBlocks;
        this.keys = keys;
        this.pal = pal;
        this.palette = palette;
    }

    /** Copy all stored blocks into {@code grid}. */
    public void writeTo(Grid grid) {
        for (int i = 0; i < keys.length; i++) {
            long k = keys[i];
            grid.set(ImportedBbLoader.kx(k), ImportedBbLoader.ky(k), ImportedBbLoader.kz(k), palette[pal[i]]);
        }
    }
}
