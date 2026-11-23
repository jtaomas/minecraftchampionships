package com.mcm.games.railroadrush;

/** Immutable block position. */
record Pos(int x, int y, int z) {
    Pos add(int dx, int dy, int dz) { return new Pos(x + dx, y + dy, z + dz); }
    Pos down() { return new Pos(x, y - 1, z); }
}
