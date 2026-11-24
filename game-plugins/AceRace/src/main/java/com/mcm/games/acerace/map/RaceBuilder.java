package com.mcm.games.acerace.map;

import com.mcm.games.acerace.map.RaceGeo.Pos;

import java.util.ArrayList;
import java.util.List;

/** Small pure-Java shape helper used by map generators. All coordinates are local to the map. */
public final class RaceBuilder {
    private final RaceBlocks blocks = new RaceBlocks();
    private final List<Pos> barrier = new ArrayList<>();

    public RaceBlocks blocks() {
        return blocks;
    }

    public List<Pos> barrier() {
        return barrier;
    }

    public RaceBuilder set(int x, int y, int z, String state) {
        blocks.set(x, y, z, state);
        return this;
    }

    public RaceBuilder fill(int x1, int y1, int z1, int x2, int y2, int z2, String state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++)
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
                    blocks.set(x, y, z, state);
        return this;
    }

    /**
     * A walkable floor at {@code y}: the top block is at y, one support block below at y-1, and an
     * edge stripe on the four sides.
     */
    public RaceBuilder floor(int x1, int x2, int z1, int z2, int y, String top, String edge) {
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                blocks.set(x, y, z, (x == minX || x == maxX || z == minZ || z == maxZ) ? edge : top);
                blocks.set(x, y - 1, z, "minecraft:stone");
            }
        }
        return this;
    }

    public RaceBuilder barrier(int x, int y, int z, String state) {
        blocks.set(x, y, z, state);
        barrier.add(new Pos(x, y, z));
        return this;
    }

    /** A decorative hoop in the Y/Z plane at x. Non-solid blocks only. */
    public RaceBuilder ringYZ(int x, int cy, int cz, int radius, String state) {
        for (int a = 0; a < 360; a += 12) {
            double rad = Math.toRadians(a);
            int y = cy + (int) Math.round(Math.sin(rad) * radius);
            int z = cz + (int) Math.round(Math.cos(rad) * radius);
            blocks.set(x, y, z, state);
        }
        return this;
    }
}
