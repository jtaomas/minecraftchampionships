package com.mcm.games.parkourtag.map;

import java.util.List;

/**
 * One hand-built Parkour Tag map, recreated from MCC reference images. Pure Java (no Bukkit), so the same data
 * builds the arena copies at runtime and renders offline previews ({@code tools/PtPreview.java}).
 * Coordinates are local: x 0..sizeX-1, z 0..sizeZ-1, y 0..sizeY-1 (y 0 is the lowest layer of the map).
 */
public interface PtMap {
    String id();

    /** Name shown on the sidebar and in chat, e.g. "The Dustbowl". */
    String displayName();

    /** Only finished maps are picked for a game. */
    boolean finished();

    int sizeX();
    int sizeY();
    int sizeZ();

    /** Writes every block of the map, including both cage frames (see {@link Cage#build}). */
    void generate(BlockSink sink);

    /** The two cages: index 0 for the first side of a pairing, 1 for the second. */
    List<Cage> cages();

    /** Spectator overview point {x, y, z, yaw, pitch}, local coordinates. */
    double[] spectator();

    /** A player whose feet go below this local y is sent back (void safety). */
    default int voidY() { return -4; }

    /** Preview only: which blocks the pit view (no walls or stands in the way) keeps. */
    default boolean pitKeep(int x, int y, int z, String block) { return y < 16; }

    /** The play area {x0, z0, x1, z1} (local, inclusive), used by the offline route checks. */
    default int[] playBox() { return new int[]{0, 0, sizeX() - 1, sizeZ() - 1}; }
}
