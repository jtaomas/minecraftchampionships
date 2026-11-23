package com.mcm.games.findthebutton.map;

/**
 * Where a pure-Java arena generator writes its blocks. Coordinates are local to the arena origin
 * (centre of the floor, floor surface at y = 0). {@code blockData} is a vanilla block-state string such as
 * {@code minecraft:oak_stairs[facing=north,half=top]} ("minecraft:" may be left out); null or air clears.
 */
public interface MapSink {
    void set(int x, int y, int z, String blockData);
}
