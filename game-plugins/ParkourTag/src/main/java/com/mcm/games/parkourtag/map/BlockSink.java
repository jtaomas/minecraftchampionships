package com.mcm.games.parkourtag.map;

/** Receives map blocks as vanilla block-state strings ("minecraft:" may be left out). Null or "air" clears. */
@FunctionalInterface
public interface BlockSink {
    void set(int x, int y, int z, String blockData);
}
