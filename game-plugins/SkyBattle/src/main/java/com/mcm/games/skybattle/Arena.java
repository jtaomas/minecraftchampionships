package com.mcm.games.skybattle;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.DyeColor;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

/** Owns the void world, generates the sky map and remembers every block it (or a player) placed. */
public final class Arena {
    public static final String WORLD_NAME = "mcc_skybattle";
    public static final int BASE_Y = 100;
    public static final int VOID_Y = BASE_Y - 45;
    public static final int START_R = 5;
    public static final int MID_R = 4;
    public static final int CENTER_R = 10;

    private final JavaPlugin plugin;
    private final Random random = new Random();
    private final Set<Long> placed = new HashSet<>();
    private World world;

    private double ringRadius = 36;
    private double midRadius = 20;
    private final List<Location> islandCenters = new ArrayList<>();

    public Arena(JavaPlugin plugin) { this.plugin = plugin; }

    public World world() { return world; }
    public double ringRadius() { return ringRadius; }
    public double midRadius() { return midRadius; }
    public double initialBorder() { return 2 * (ringRadius + START_R + 10); }
    public Location islandCenter(int i) { return islandCenters.get(i).clone(); }

    public Location spectatorPoint() {
        Location l = new Location(world, 0.5, BASE_Y + 30, 0.5);
        l.setPitch(70);
        return l;
    }

    /** Load or create the void world. Deletes a stale folder the first time so each server start gets a clean map. */
    public World ensureWorld() {
        World existing = Bukkit.getWorld(WORLD_NAME);
        if (existing == null) {
            File folder = new File(Bukkit.getWorldContainer(), WORLD_NAME);
            if (folder.exists()) deleteFolder(folder.toPath());
            WorldCreator wc = new WorldCreator(WORLD_NAME)
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false);
            existing = wc.createWorld();
        }
        world = existing;
        if (world == null) return null;
        world.setSpawnLocation(0, BASE_Y + 25, 0);
        world.setDifficulty(Difficulty.NORMAL);
        world.setPVP(true);
        world.setSpawnFlags(false, false);
        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, false);
        world.setGameRule(GameRule.FALL_DAMAGE, true);
        world.setGameRule(GameRule.NATURAL_REGENERATION, true);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        world.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        world.setGameRule(GameRule.DO_INSOMNIA, false);
        world.setGameRule(GameRule.SPAWN_RADIUS, 0);
        world.setAutoSave(false);
        return world;
    }

    private void deleteFolder(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) {}
            });
        } catch (IOException e) {
            plugin.getLogger().warning("Could not delete old Sky Battle world: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- tracking

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (long) ((y + 64) & 0xFFF);
    }

    public void track(Block b) {
        if (world != null && b.getWorld() == world) placed.add(key(b.getX(), b.getY(), b.getZ()));
    }

    private void set(int x, int y, int z, Material m) {
        world.getBlockAt(x, y, z).setType(m, false);
        placed.add(key(x, y, z));
    }

    /** Remove every generated or player-placed block, plus dropped items and projectiles. */
    public void clear() {
        if (world == null || Bukkit.getWorld(WORLD_NAME) != world) { placed.clear(); return; }
        for (long k : placed) {
            int x = (int) ((k >>> 38) & 0x3FFFFFF);
            if ((x & 0x2000000) != 0) x |= ~0x3FFFFFF;
            int z = (int) ((k >>> 12) & 0x3FFFFFF);
            if ((z & 0x2000000) != 0) z |= ~0x3FFFFFF;
            int y = (int) (k & 0xFFF) - 64;
            Block b = world.getBlockAt(x, y, z);
            Material type = b.getType();
            if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                if (b.getState() instanceof Chest chest) chest.getBlockInventory().clear();
            }
            if (type != Material.AIR) b.setType(Material.AIR, false);
        }
        placed.clear();
        for (Entity e : new ArrayList<>(world.getEntities())) {
            if (!(e instanceof Player)) e.remove();
        }
    }

    // ---------------------------------------------------------------- generation

    /**
     * Build the map. One starting island per entry in {@code colours} (null = neutral, unused island),
     * a ring of mid islands and a big central island. Chests are filled from the loot table.
     */
    public void build(List<DyeColor> colours, LootTable loot) {
        islandCenters.clear();
        int n = colours.size();
        ringRadius = Math.max(36, n * 22 / (2 * Math.PI));
        midRadius = ringRadius * 0.55;

        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / n - Math.PI / 2;
            int cx = (int) Math.round(Math.cos(a) * ringRadius);
            int cz = (int) Math.round(Math.sin(a) * ringRadius);
            islandCenters.add(new Location(world, cx + 0.5, BASE_Y + 1, cz + 0.5));
            buildStartIsland(cx, cz, colours.get(i), loot);
        }

        int m = Math.max(4, Math.min(12, n));
        for (int i = 0; i < m; i++) {
            double a = 2 * Math.PI * (i + 0.5) / m - Math.PI / 2;
            int cx = (int) Math.round(Math.cos(a) * midRadius);
            int cz = (int) Math.round(Math.sin(a) * midRadius);
            int cy = BASE_Y + 1 + random.nextInt(3);
            island(cx, cy, cz, MID_R, i % 2 == 0 ? Material.MOSS_BLOCK : Material.GRASS_BLOCK,
                    Material.ANDESITE, Material.STONE);
            chest(cx, cy + 1, cz, LootTable.Tier.MID, loot);
            set(cx + 2, cy + 1, cz + 1, Material.LANTERN);
        }

        buildCenter(loot);
    }

    private void buildStartIsland(int cx, int cz, DyeColor colour, LootTable loot) {
        int cy = BASE_Y;
        Material concrete = colour == null ? Material.SMOOTH_STONE : Material.valueOf(colour.name() + "_CONCRETE");
        Material wool = colour == null ? Material.WHITE_WOOL : Material.valueOf(colour.name() + "_WOOL");
        island(cx, cy, cz, START_R, Material.GRASS_BLOCK, concrete, Material.STONE);

        double len = Math.max(1, Math.hypot(cx, cz));
        // outward direction (away from the centre) and a perpendicular
        int ox = (int) Math.round(cx / len * 3), oz = (int) Math.round(cz / len * 3);
        int px = (int) Math.round(-cz / len), pz = (int) Math.round(cx / len);

        // team flag on the outer edge
        int fx = cx + ox + px * 3, fz = cz + oz + pz * 3;
        for (int y = 1; y <= 3; y++) set(fx, cy + y, fz, Material.OAK_FENCE);
        set(fx, cy + 4, fz, wool);
        set(fx, cy + 5, fz, wool);
        set(fx, cy + 6, fz, Material.LANTERN);
        // two starter chests
        chest(cx + ox + px, cy + 1, cz + oz + pz, LootTable.Tier.ISLAND, loot);
        chest(cx + ox - px, cy + 1, cz + oz - pz, LootTable.Tier.ISLAND, loot);
    }

    private void buildCenter(LootTable loot) {
        int cy = BASE_Y + 3;
        island(0, cy, 0, CENTER_R, Material.GRASS_BLOCK, Material.STONE_BRICKS, Material.STONE);
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > 4.4) continue;
                set(dx, cy + 1, dz, Material.STONE_BRICKS);
                set(dx, cy + 2, dz, d > 3.4 ? Material.CHISELED_STONE_BRICKS : Material.POLISHED_ANDESITE);
            }
        }
        chest(1, cy + 3, 0, LootTable.Tier.CENTER, loot);
        chest(-1, cy + 3, 0, LootTable.Tier.CENTER, loot);
        chest(0, cy + 3, 1, LootTable.Tier.CENTER, loot);
        chest(0, cy + 3, -1, LootTable.Tier.CENTER, loot);
        set(0, cy + 3, 0, Material.LANTERN);
        chest(7, cy + 1, 0, LootTable.Tier.MID, loot);
        chest(-7, cy + 1, 0, LootTable.Tier.MID, loot);
        chest(0, cy + 1, 7, LootTable.Tier.MID, loot);
        chest(0, cy + 1, -7, LootTable.Tier.MID, loot);
        tree(6, cy + 1, 6);
        tree(-6, cy + 1, -6);
    }

    private void tree(int x, int y, int z) {
        for (int i = 0; i < 4; i++) set(x, y + i, z, Material.OAK_LOG);
        for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) for (int dy = 2; dy <= 4; dy++) {
            if (Math.abs(dx) + Math.abs(dz) + (dy - 2) > 3) continue;
            Block b = world.getBlockAt(x + dx, y + dy, z + dz);
            if (b.getType() != Material.AIR) continue;
            set(x + dx, y + dy, z + dz, Material.OAK_LEAVES);
            if (b.getBlockData() instanceof Leaves leaves) { leaves.setPersistent(true); b.setBlockData(leaves, false); }
        }
    }

    private void chest(int x, int y, int z, LootTable.Tier tier, LootTable loot) {
        set(x, y, z, Material.CHEST);
        Block b = world.getBlockAt(x, y, z);
        if (b.getState() instanceof Chest chest) loot.fill(chest.getBlockInventory(), tier);
    }

    /** A floating disc with a tapered rocky underside. */
    private void island(int cx, int cy, int cz, int r, Material top, Material edge, Material rock) {
        for (int dx = -r - 1; dx <= r + 1; dx++) {
            for (int dz = -r - 1; dz <= r + 1; dz++) {
                double d = Math.hypot(dx, dz);
                if (d > r + 0.5) continue;
                int depth = 1 + (int) ((r + 0.5 - d) * 1.1) + random.nextInt(2);
                for (int y = 0; y <= depth; y++) {
                    Material m;
                    if (y == 0) m = d > r - 0.7 ? edge : top;
                    else if (y <= 2) m = Material.DIRT;
                    else m = random.nextInt(8) == 0 ? Material.COBBLESTONE : rock;
                    set(cx + dx, cy - y, cz + dz, m);
                }
            }
        }
    }
}
