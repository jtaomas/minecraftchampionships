package com.mcm.games.hitw;

import org.bukkit.*;
import org.bukkit.block.Block;

/** The mcc_hitw void world and the floating platform built in it. */
final class Arena {
    static final String WORLD_NAME = "mcc_hitw";
    /** Platform spans -HALF..HALF on both X and Z (17x17). */
    static final int HALF = 8;
    static final int FLOOR_Y = 99;
    /** Lowest wall row; walls occupy WALL_Y .. WALL_Y + WALL_H - 1. */
    static final int WALL_Y = 100;
    static final int WALL_H = 5;
    /** Walls start this many cells from the centre and travel to the same distance on the other side. */
    static final int START = 14;
    /** Players below this Y are out. */
    static final double ELIM_Y = 90;

    private static final int CLEAR_R = 19;
    private static final int CLEAR_MIN_Y = 78, CLEAR_MAX_Y = 112;

    World world() { return Bukkit.getWorld(WORLD_NAME); }

    World ensureWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            WorldCreator wc = new WorldCreator(WORLD_NAME)
                    .environment(World.Environment.NORMAL)
                    .generator(new VoidGenerator())
                    .generateStructures(false);
            w = wc.createWorld();
        }
        if (w == null) return null;
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setTime(6000);
        w.setStorm(false);
        w.setThundering(false);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setPVP(true);
        w.setSpawnLocation(0, FLOOR_Y + 1, 0);
        return w;
    }

    /** Remove everything we could have built. Safe if the world is not loaded. */
    void clear() {
        World w = world();
        if (w == null) return;
        for (int x = -CLEAR_R; x <= CLEAR_R; x++)
            for (int z = -CLEAR_R; z <= CLEAR_R; z++)
                for (int y = CLEAR_MIN_Y; y <= CLEAR_MAX_Y; y++) {
                    Block b = w.getBlockAt(x, y, z);
                    if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                }
    }

    void build() {
        World w = world();
        if (w == null) return;
        // platform
        for (int x = -HALF; x <= HALF; x++)
            for (int z = -HALF; z <= HALF; z++) {
                int ring = Math.max(Math.abs(x), Math.abs(z));
                Material top;
                if (ring == HALF) top = Material.BLACK_CONCRETE;
                else if (ring == HALF - 1) top = Material.GRAY_CONCRETE;
                else top = ((x + z) & 1) == 0 ? Material.WHITE_CONCRETE : Material.LIGHT_GRAY_CONCRETE;
                w.getBlockAt(x, FLOOR_Y, z).setType(top, false);
                w.getBlockAt(x, FLOOR_Y - 1, z).setType(Material.SMOOTH_STONE, false);
            }
        w.getBlockAt(0, FLOOR_Y - 2, 0).setType(Material.SEA_LANTERN, false);

        // wall "launch gates" on all four sides (no floor parts, so nobody lands on them)
        int g = START + 2;
        for (int side = 0; side < 4; side++) {
            boolean axisX = side < 2;
            int a = (side % 2 == 0) ? g : -g;
            for (int lat = -HALF - 2; lat <= HALF + 2; lat++) {
                setAxis(w, axisX, a, WALL_Y + WALL_H + 1, lat, Material.POLISHED_BLACKSTONE);
            }
            for (int y = WALL_Y - 3; y <= WALL_Y + WALL_H + 1; y++) {
                setAxis(w, axisX, a, y, -HALF - 2, Material.POLISHED_BLACKSTONE_BRICKS);
                setAxis(w, axisX, a, y, HALF + 2, Material.POLISHED_BLACKSTONE_BRICKS);
            }
            setAxis(w, axisX, a, WALL_Y + WALL_H + 2, -HALF - 2, Material.SEA_LANTERN);
            setAxis(w, axisX, a, WALL_Y + WALL_H + 2, HALF + 2, Material.SEA_LANTERN);
            setAxis(w, axisX, a, WALL_Y + WALL_H + 2, 0, Material.LIME_STAINED_GLASS);
        }

        // corner pillars
        for (int sx = -1; sx <= 1; sx += 2)
            for (int sz = -1; sz <= 1; sz += 2) {
                int x = sx * 13, z = sz * 13;
                for (int y = FLOOR_Y - 14; y <= WALL_Y + 3; y++)
                    w.getBlockAt(x, y, z).setType(Material.QUARTZ_PILLAR, false);
                w.getBlockAt(x, WALL_Y + 4, z).setType(Material.SEA_LANTERN, false);
            }
    }

    private static void setAxis(World w, boolean axisX, int axis, int y, int lat, Material m) {
        if (axisX) w.getBlockAt(axis, y, lat).setType(m, false);
        else w.getBlockAt(lat, y, axis).setType(m, false);
    }

    /** Ring index counted from the outside (0 = outermost). */
    void paintRing(int ringFromOutside, Material m) {
        World w = world();
        if (w == null) return;
        int r = HALF - ringFromOutside;
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++)
                if (Math.max(Math.abs(x), Math.abs(z)) == r) {
                    Block b = w.getBlockAt(x, FLOOR_Y, z);
                    if (b.getType() != Material.AIR) b.setType(m, false);
                }
    }

    void removeRing(int ringFromOutside) {
        World w = world();
        if (w == null) return;
        int r = HALF - ringFromOutside;
        for (int x = -r; x <= r; x++)
            for (int z = -r; z <= r; z++)
                if (Math.max(Math.abs(x), Math.abs(z)) == r) {
                    w.getBlockAt(x, FLOOR_Y, z).setType(Material.AIR, false);
                    w.getBlockAt(x, FLOOR_Y - 1, z).setType(Material.AIR, false);
                    if (((x + z) & 3) == 0)
                        w.spawnParticle(Particle.CLOUD, x + 0.5, FLOOR_Y + 0.5, z + 0.5, 2, 0.2, 0.2, 0.2, 0.01);
                }
    }

    Location spectatorPoint() {
        World w = world();
        return new Location(w, 0.5, WALL_Y + 12, -START - 8.5, 0f, 32f);
    }
}
