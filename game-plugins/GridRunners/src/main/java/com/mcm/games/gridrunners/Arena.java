package com.mcm.games.gridrunners;

import com.mcm.games.gridrunners.rooms.Room;
import com.mcm.games.gridrunners.rooms.RoomType;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.DyeColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;

import java.util.Random;

/** World creation and course construction. */
public final class Arena {
    private Arena() {}

    static final class VoidGenerator extends ChunkGenerator {
        @Override
        public Location getFixedSpawnLocation(World world, Random random) {
            return new Location(world, 0.5, GridRunnersGame.Y0 + 1, 0.5);
        }
    }

    public static World ensureWorld() {
        World w = Bukkit.getWorld(GridRunnersGame.WORLD);
        if (w == null) {
            w = new WorldCreator(GridRunnersGame.WORLD)
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false)
                    .createWorld();
        }
        if (w == null) return null;
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.MOB_GRIEFING, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.DO_MOB_LOOT, false);
        w.setGameRule(GameRule.DO_TILE_DROPS, false);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.SPAWN_RADIUS, 0);
        w.setDifficulty(Difficulty.NORMAL);
        w.setTime(6000);
        w.setStorm(false);
        w.setThundering(false);
        w.setSpawnLocation(0, GridRunnersGame.Y0 + 1, 0);
        return w;
    }

    private static void set(World w, int x, int y, int z, Material m) {
        w.getBlockAt(x, y, z).setType(m, false);
    }

    public static Material concrete(DyeColor c) { return Material.valueOf(c.name() + "_CONCRETE"); }

    public static Material glass(DyeColor c) { return Material.valueOf(c.name() + "_STAINED_GLASS"); }

    /** Footprint of a lane (blocks), shared by clearing and chunk preloading. */
    public static final int FOOT_MIN_DX = -11, FOOT_MAX_DX = 47, FOOT_MIN_Z = -2, FOOT_MAX_Z = 47;

    /** Chunk coordinates {cx, cz} covering a lane's footprint. */
    public static java.util.List<int[]> laneChunks(int laneIndex) {
        int x0 = laneIndex * GridRunnersGame.LANE_SPACING;
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int cx = Math.floorDiv(x0 + FOOT_MIN_DX, 16); cx <= Math.floorDiv(x0 + FOOT_MAX_DX, 16); cx++)
            for (int cz = Math.floorDiv(FOOT_MIN_Z, 16); cz <= Math.floorDiv(FOOT_MAX_Z, 16); cz++)
                out.add(new int[]{cx, cz});
        return out;
    }

    /**
     * Resumable clear of the whole footprint a lane can occupy (~41k blocks), one x-slice (700 blocks) at a time
     * until the tick deadline. Never loads chunks: columns in unloaded chunks are skipped; if such a chunk is not
     * known to be empty, {@code onIncomplete} is told so the lane is not marked clean.
     */
    public static java.util.function.LongPredicate clearLaneStep(World w, int laneIndex,
                                                                 java.util.function.BiPredicate<Integer, Integer> knownEmpty,
                                                                 Runnable onComplete, Runnable onIncomplete) {
        final int x0 = laneIndex * GridRunnersGame.LANE_SPACING;
        final int y0 = GridRunnersGame.Y0;
        final int[] next = {x0 + FOOT_MIN_DX};
        final boolean[] incomplete = {false};
        return deadline -> {
            while (next[0] <= x0 + FOOT_MAX_DX) {
                int x = next[0]++;
                for (int z = FOOT_MIN_Z; z <= FOOT_MAX_Z; z++) {
                    int cx = x >> 4, cz = z >> 4;
                    if (!w.isChunkLoaded(cx, cz)) {
                        if (!knownEmpty.test(cx, cz)) incomplete[0] = true;
                        continue;
                    }
                    for (int y = y0 - 1; y <= y0 + 12; y++) {
                        var b = w.getBlockAt(x, y, z);
                        if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                    }
                }
                if (System.nanoTime() >= deadline) return false;
            }
            if (incomplete[0]) onIncomplete.run(); else onComplete.run();
            return true;
        };
    }

    /** Floors, walls, ceilings, doors, start door and entry hall (~7.5k blocks). Room contents are separate. */
    public static void buildShell(World w, Lane lane) {
        int x0 = lane.x0, z0 = lane.z0, y0 = GridRunnersGame.Y0;
        Material team = concrete(lane.color);
        // Floors, walls, ceiling
        for (int dx = 0; dx <= 45; dx++) {
            for (int dz = 0; dz <= 45; dz++) {
                boolean wx = dx % 15 == 0, wz = dz % 15 == 0;
                int x = x0 + dx, z = z0 + dz;
                if (wx || wz) {
                    boolean corner = wx && wz;
                    for (int yy = 0; yy <= 9; yy++) {
                        Material m;
                        if (yy == 0) m = Material.POLISHED_DEEPSLATE;
                        else if (yy == 9 || corner) m = team;
                        else if (yy == 1 || yy == 8) m = Material.POLISHED_DEEPSLATE;
                        else if (yy == 5) m = team;
                        else m = Material.SMOOTH_QUARTZ;
                        set(w, x, y0 + yy, z, m);
                    }
                } else {
                    Room r = lane.rooms[(dz / 15) * 3 + (dx / 15)];
                    set(w, x, y0, z, r.floorAt(dx % 15, dz % 15));
                    set(w, x, y0 + 9, z, Material.GLASS);
                }
            }
        }
        // Doors between rooms
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                Room a = lane.room(col, row);
                Room east = lane.room(col + 1, row);
                Room south = lane.room(col, row + 1);
                if (east != null) setDoor(w, lane, a, east, doorMaterial(a, east));
                if (south != null) setDoor(w, lane, a, south, doorMaterial(a, south));
            }
        }
        // Start door
        for (int dz = 6; dz <= 9; dz++)
            for (int yy = 1; yy <= 3; yy++) set(w, x0, y0 + yy, z0 + dz, Material.TINTED_GLASS);
        // Entry hall (x0-9 .. x0-1, z0+2 .. z0+13)
        for (int dx = -9; dx <= -1; dx++) {
            for (int dz = 2; dz <= 13; dz++) {
                int x = x0 + dx, z = z0 + dz;
                boolean wall = dx == -9 || dz == 2 || dz == 13;
                if (wall) {
                    set(w, x, y0, z, Material.POLISHED_DEEPSLATE);
                    for (int yy = 1; yy <= 4; yy++) set(w, x, y0 + yy, z, yy == 3 ? team : Material.SMOOTH_QUARTZ);
                    set(w, x, y0 + 5, z, team);
                } else {
                    set(w, x, y0, z, ((dx + dz) & 1) == 0 ? team : Material.WHITE_CONCRETE);
                    set(w, x, y0 + 5, z, glass(lane.color));
                }
            }
        }
    }

    private static Material doorMaterial(Room a, Room b) {
        return (a.type == RoomType.FINISH || b.type == RoomType.FINISH) ? Material.GOLD_BLOCK : Material.TINTED_GLASS;
    }

    private static void setDoor(World w, Lane lane, Room a, Room b, Material m) {
        int y0 = GridRunnersGame.Y0;
        if (a.row == b.row) {
            int wx = lane.x0 + 15 * Math.max(a.col, b.col);
            for (int dz = 6; dz <= 9; dz++)
                for (int yy = 1; yy <= 3; yy++) set(w, wx, y0 + yy, lane.z0 + 15 * a.row + dz, m);
        } else {
            int wz = lane.z0 + 15 * Math.max(a.row, b.row);
            for (int dx = 6; dx <= 9; dx++)
                for (int yy = 1; yy <= 3; yy++) set(w, lane.x0 + 15 * a.col + dx, y0 + yy, wz, m);
        }
    }

    public static void openDoor(World w, Lane lane, Room a, Room b) {
        setDoor(w, lane, a, b, Material.AIR);
        Location c;
        if (a.row == b.row) c = new Location(w, lane.x0 + 15 * Math.max(a.col, b.col) + 0.5, GridRunnersGame.Y0 + 2, lane.z0 + 15 * a.row + 8);
        else c = new Location(w, lane.x0 + 15 * a.col + 8, GridRunnersGame.Y0 + 2, lane.z0 + 15 * Math.max(a.row, b.row) + 0.5);
        w.spawnParticle(Particle.CLOUD, c, 20, 1, 1, 1, 0.02);
        w.playSound(c, Sound.BLOCK_IRON_DOOR_OPEN, 1f, 0.8f);
    }

    public static void openStartDoor(World w, Lane lane) {
        for (int dz = 6; dz <= 9; dz++)
            for (int yy = 1; yy <= 3; yy++) set(w, lane.x0, GridRunnersGame.Y0 + yy, lane.z0 + dz, Material.AIR);
        Location c = new Location(w, lane.x0 + 0.5, GridRunnersGame.Y0 + 2, lane.z0 + 8);
        w.spawnParticle(Particle.CLOUD, c, 20, 0.5, 1, 1, 0.02);
        w.playSound(c, Sound.BLOCK_IRON_DOOR_OPEN, 1f, 1f);
    }
}
