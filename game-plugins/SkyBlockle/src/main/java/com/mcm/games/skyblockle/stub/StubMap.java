package com.mcm.games.skyblockle.stub;

import com.mcm.games.skyblockle.MapModel;
import com.mcm.games.skyblockle.MapModel.IslandType;

import java.util.Random;

/**
 * Minimal stand-in generator (pure Java) used until / unless {@code com.mcm.games.skyblockle.map.SkyMap} exists,
 * or when config {@code map.generator: stub}. Plain tapered islands on each wedge's line to a simple castle disc,
 * one chest per island type, a working cobblestone generator. Not the real map design.
 */
public final class StubMap {
    private StubMap() {}

    public static MapModel generate(int wedges, long seed) {
        int n = Math.max(1, wedges);
        MapModel m = new MapModel(n, seed);
        Random r = new Random(seed);
        MapModel.Blocks b = m.blocks;

        // castle: flat arena disc, raised rim, 4 corner towers, red/yellow accents
        int cr = 24, ar = 13;
        island(b, 0, 0, 0, cr, 10, "minecraft:stone_bricks");
        for (int x = -cr; x <= cr; x++) for (int z = -cr; z <= cr; z++) {
            double d = Math.sqrt(x * x + z * z);
            if (d > cr + 0.5) continue;
            b.set(x, 0, z, d <= ar + 0.5 ? "minecraft:smooth_stone" : "minecraft:stone_bricks");
            if (d > cr - 0.5) b.set(x, 1, z, (x + z) % 4 == 0 ? "minecraft:red_concrete" : "minecraft:stone_brick_wall");
        }
        int t = 17;
        int[][] towers = {{t, t}, {-t, t}, {t, -t}, {-t, -t}};
        for (int[] tw : towers) {
            for (int y = 1; y <= 9; y++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++)
                if (Math.abs(dx) == 2 || Math.abs(dz) == 2) b.set(tw[0] + dx, y, tw[1] + dz, y == 9 ? "minecraft:yellow_concrete" : "minecraft:stone_bricks");
            chest(m, tw[0], 1, tw[1], IslandType.CASTLE, -1);
        }
        m.castleX = 0; m.castleY = 0; m.castleZ = 0;
        m.arenaRadius = ar;
        m.castleRadius = cr;

        for (int w = 0; w < n; w++) {
            double a = Math.toRadians(w * 360.0 / n);
            double dx = -Math.sin(a), dz = Math.cos(a);
            double px = Math.cos(a), pz = Math.sin(a); // perpendicular
            // start island (~6x6) at r=135: grass, dirt, oak tree, chest, team marker, spawns
            int sx = (int) Math.round(dx * 135), sz = (int) Math.round(dz * 135);
            island(b, sx, 0, sz, 3, 5, "minecraft:dirt");
            for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) if (x * x + z * z <= 10) b.set(sx + x, 0, sz + z, "minecraft:grass_block");
            tree(b, sx + 2, 1, sz + 2);
            chest(m, sx - 2, 1, sz - 2, IslandType.START, w);
            float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz)); // face the centre
            int[][] sp = {{0, 0}, {1, 0}, {0, 1}, {-1, 0}};
            for (int[] s : sp) {
                b.set(sx + s[0], 0, sz + s[1], "minecraft:white_concrete");
                m.teamMarkers.add(new MapModel.TeamMarker(sx + s[0], 0, sz + s[1], w, "_concrete"));
                m.spawns.add(new MapModel.Spawn(w, sx + s[0] + 0.5, 1, sz + s[1] + 0.5, yaw));
            }
            // generator island at r=150 offset sideways
            int gx = (int) Math.round(dx * 150 + px * 14), gz = (int) Math.round(dz * 150 + pz * 14);
            island(b, gx, 0, gz, 4, 6, "minecraft:stone");
            cobbleGenerator(b, gx - 2, 1, gz);
            chest(m, gx + 2, 1, gz + 2, IslandType.GENERATOR, w);
            // fort / brewing / enchanting / mid on the line to the centre
            ring(m, dx, dz, 103, 5, "minecraft:nether_bricks", IslandType.FORT, w);
            ring(m, dx, dz, 70, 3, "minecraft:stone", IslandType.BREWING, w);
            int bx = (int) Math.round(dx * 70) + 1, bz = (int) Math.round(dz * 70);
            b.set(bx, 1, bz, "minecraft:brewing_stand");
            ring(m, dx, dz, 48, 3, "minecraft:moss_block", IslandType.ENCHANTING, w);
            int ex = (int) Math.round(dx * 48) + 1, ez = (int) Math.round(dz * 48);
            b.set(ex, 1, ez, "minecraft:enchanting_table");
            // mid islands between wedges
            double a2 = Math.toRadians((w + 0.5) * 360.0 / n);
            ring(m, -Math.sin(a2), Math.cos(a2), 119, 3, "minecraft:grass_block", IslandType.MID, -1);
        }
        m.mapRadius = 165;
        m.rings = new MapModel.Rings(cr, 48, 70, 103, 119, 135, 150, 165);
        m.minY = -12;
        m.maxY = 12;
        m.notes.put("generator", "stub");
        return m;
    }

    private static void ring(MapModel m, double dx, double dz, int radius, int size, String top, IslandType type, int wedge) {
        int x = (int) Math.round(dx * radius), z = (int) Math.round(dz * radius);
        island(m.blocks, x, 0, z, size, size + 2, top);
        chest(m, x - 1, 1, z, type, wedge);
    }

    private static void island(MapModel.Blocks b, int cx, int y, int cz, int r, int depth, String top) {
        for (int d = 0; d <= depth; d++) {
            double rr = r * (1.0 - d / (double) (depth + 1)) + 0.5;
            String blk = d == 0 ? top : d < 3 ? "minecraft:dirt" : "minecraft:stone";
            for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++)
                if (x * x + z * z <= rr * rr) b.set(cx + x, y - d, cz + z, blk);
        }
    }

    private static void chest(MapModel m, int x, int y, int z, IslandType type, int wedge) {
        m.blocks.set(x, y, z, "minecraft:chest[facing=south]");
        m.chests.add(new MapModel.ChestSpot(x, y, z, type, wedge));
    }

    private static void tree(MapModel.Blocks b, int x, int y, int z) {
        for (int i = 0; i < 5; i++) b.set(x, y + i, z, "minecraft:oak_log");
        for (int dy = 3; dy <= 5; dy++) for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
            if (Math.abs(dx) + Math.abs(dz) > (dy == 5 ? 1 : 3)) continue;
            if (dx == 0 && dz == 0 && dy < 5) continue;
            b.set(x + dx, y + dy, z + dz, "minecraft:oak_leaves[persistent=true]");
        }
    }

    /** Trench along +X: lava | cobble | air (pit below) | water, walled. Mining the cobble makes new cobble. */
    private static void cobbleGenerator(MapModel.Blocks b, int x, int y, int z) {
        for (int i = -1; i <= 4; i++) {
            b.set(x + i, y, z - 1, "minecraft:stone");
            b.set(x + i, y, z + 1, "minecraft:stone");
        }
        b.set(x - 1, y, z, "minecraft:stone");
        b.set(x + 4, y, z, "minecraft:stone");
        b.set(x, y, z, "minecraft:lava");
        b.set(x + 1, y, z, "minecraft:cobblestone");
        b.set(x + 2, y, z, "minecraft:air");
        b.set(x + 2, y - 1, z, "minecraft:air");   // pit catches the water
        b.set(x + 3, y, z, "minecraft:water");
    }
}
