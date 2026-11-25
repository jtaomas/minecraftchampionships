package com.mcm.games.decisiondome;

import com.mcm.api.McmApi;
import com.mcm.api.McmGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * The plugin-owned fallback venue in the void world {@code mcc_dome}. This class owns only static
 * geometry and block mutations; the vote runtime and all entities live in {@link DomeSession}.
 */
final class DomeArena {
    static final String WORLD_NAME = "mcc_dome";
    static final String TAG = "mcm_dome";
    static final int HUB_R = 6;
    static final int WEDGE_R = 20;
    static final int RING_R = 23;
    static final int SHELL_R = 24;
    static final int MAX_WEDGES = 8;

    static final Material[] COLORS = {
            Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
            Material.GREEN_CONCRETE, Material.CYAN_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.BLUE_CONCRETE,
            Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE, Material.PINK_CONCRETE, Material.WHITE_CONCRETE
    };
    static final String[] COLOR_CODES = {"§c", "§6", "§e", "§a", "§2", "§3", "§b", "§9", "§5", "§d", "§d", "§f"};

    /** Pod centres from the Nocturne Orrery schematic: 36 degrees apart with the 90-degree stair gap. */
    static final double[] POD_ANGLES_DEG = {18, 54, 72, 108, 126, 162, 198, 234, 270, 306};

    private final Plugin plugin;
    private World world;
    private int floorY = 100;
    private int wedgeCount;
    private double baseAngle = Math.PI / 2.0;
    private final List<List<Block>> wedgeBlocks = new ArrayList<>();
    private final List<Block> dividerBlocks = new ArrayList<>();
    private final List<List<Block>> podBlocks = new ArrayList<>();

    DomeArena(Plugin plugin) { this.plugin = plugin; }

    static Component c(String legacy) { return LegacyComponentSerializer.legacySection().deserialize(legacy); }

    World world() { return world; }

    int floorY() { return floorY; }

    boolean isDomeWorld(World w) { return world != null && w != null && w.equals(world); }

    /** Create / load the void world and its permanent shell. Safe to call repeatedly. */
    World ensureWorld() {
        if (world != null && Bukkit.getWorld(WORLD_NAME) == world) return world;
        World main = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        floorY = Math.max(100, main == null ? 100 : main.getSpawnLocation().getBlockY());
        floorY = Math.min(floorY, 280);
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            WorldCreator wc = new WorldCreator(WORLD_NAME)
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false)
                    .generator(new VoidGenerator(floorY));
            w = wc.createWorld();
        }
        if (w == null) {
            plugin.getLogger().severe("Could not create world " + WORLD_NAME);
            return null;
        }
        world = w;
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setStorm(false);
        w.setThundering(false);
        w.setTime(6000);
        w.setSpawnLocation(0, floorY + 1, 0);
        for (int cx = -2; cx <= 1; cx++) for (int cz = -2; cz <= 1; cz++) w.addPluginChunkTicket(cx, cz, plugin);
        removeStrayEntities();
        buildStatic();
        buildDial(8, baseAngle);
        buildPods();
        return w;
    }

    void removeStrayEntities() {
        if (world == null) return;
        for (Entity e : world.getEntities()) {
            for (String tag : e.getScoreboardTags()) {
                if (tag.equals(TAG) || tag.startsWith(TAG + ":")) { e.remove(); break; }
            }
        }
    }

    private void buildStatic() {
        int lim = SHELL_R + 1;
        for (int x = -lim; x <= lim; x++) {
            for (int z = -lim; z <= lim; z++) {
                double r = Math.hypot(x + 0.5, z + 0.5);
                if (r <= HUB_R) {
                    Material m;
                    if (r < 1.5) m = Material.SEA_LANTERN;
                    else if (r < 2.5) m = Material.GOLD_BLOCK;
                    else if (r > HUB_R - 1) m = Material.POLISHED_DEEPSLATE;
                    else m = ((x + z) & 1) == 0 ? Material.SMOOTH_QUARTZ : Material.QUARTZ_BRICKS;
                    world.getBlockAt(x, floorY, z).setType(m, false);
                } else if (r > WEDGE_R && r <= SHELL_R + 0.5) {
                    Material m = Material.DARK_PRISMARINE;
                    if (r > WEDGE_R && r <= WEDGE_R + 1) m = Material.POLISHED_DEEPSLATE;
                    else if (r > 21.6 && r <= 22.6 && Math.floorMod(x * 7 + z * 13, 9) == 0) m = Material.SEA_LANTERN;
                    world.getBlockAt(x, floorY, z).setType(m, false);
                }
                for (int dy = 1; dy <= SHELL_R + 1; dy++) {
                    Block b = world.getBlockAt(x, floorY + dy, z);
                    double d = Math.sqrt((x + 0.5) * (x + 0.5) + (dy - 0.5) * (dy - 0.5) + (z + 0.5) * (z + 0.5));
                    if (d >= RING_R + 0.5 && d < SHELL_R + 0.75) {
                        Material glass = dy <= 3 ? Material.LIGHT_BLUE_STAINED_GLASS
                                : (Math.floorMod(x, 6) == 0 || Math.floorMod(z, 6) == 0) ? Material.WHITE_STAINED_GLASS : Material.GLASS;
                        b.setType(glass, false);
                    } else if (d < RING_R + 0.5 && r > WEDGE_R && b.getType() != Material.AIR) {
                        b.setType(Material.AIR, false);
                    }
                }
                world.getBlockAt(x, floorY - 1, z).setType(r <= SHELL_R + 0.5 ? Material.BARRIER : Material.AIR, false);
            }
        }
    }

    /** Lay out n coloured wedges. Wedge 0 is centred on {@code baseAngle}. */
    boolean buildDial(int n, double baseAngle) {
        if (world == null) return false;
        n = Math.max(1, Math.min(MAX_WEDGES, n));
        this.wedgeCount = n;
        this.baseAngle = baseAngle;
        wedgeBlocks.clear();
        dividerBlocks.clear();
        for (int i = 0; i < n; i++) wedgeBlocks.add(new ArrayList<>());
        double step = Math.PI * 2.0 / n;
        for (int x = -WEDGE_R - 1; x <= WEDGE_R + 1; x++) {
            for (int z = -WEDGE_R - 1; z <= WEDGE_R + 1; z++) {
                double cx = x + 0.5, cz = z + 0.5;
                double r = Math.hypot(cx, cz);
                if (r < HUB_R || r > WEDGE_R) continue;
                double rel = DialGeometry.normalise(Math.atan2(cz, cx) - (baseAngle - step / 2.0));
                int idx = Math.min((int) Math.floor(rel / step), n - 1);
                double local = rel - idx * step;
                double arc = Math.min(local, step - local) * r;
                Block floor = world.getBlockAt(x, floorY, z);
                Block above = world.getBlockAt(x, floorY + 1, z);
                if (n > 1 && arc < 0.55) {
                    floor.setType(Material.POLISHED_BLACKSTONE, false);
                    above.setType(Material.AIR, false);
                    dividerBlocks.add(floor);
                } else {
                    floor.setType(COLORS[idx % COLORS.length], false);
                    above.setType(Material.AIR, false);
                    wedgeBlocks.get(idx).add(floor);
                }
            }
        }
        setDividers(0.0);
        return true;
    }

    private void buildPods() {
        podBlocks.clear();
        for (int i = 0; i < POD_ANGLES_DEG.length; i++) {
            List<Block> blocks = new ArrayList<>();
            double a = Math.toRadians(POD_ANGLES_DEG[i]);
            double px = Math.cos(a) * 21.7;
            double pz = Math.sin(a) * 21.7;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    Block b = world.getBlockAt((int) Math.floor(px) + dx, floorY, (int) Math.floor(pz) + dz);
                    b.setType(Material.POLISHED_DEEPSLATE, false);
                    blocks.add(b);
                }
            }
            // A low back rail on the outside so the pod reads as a room, not a pad.
            double ox = Math.cos(a), oz = Math.sin(a);
            for (int sideways = -1; sideways <= 1; sideways++) {
                for (int h = 1; h <= 2; h++) {
                    double sx = -oz * sideways * 0.9;
                    double sz = ox * sideways * 0.9;
                    Block b = world.getBlockAt((int) Math.floor(px + ox * 1.4 + sx), floorY + h,
                            (int) Math.floor(pz + oz * 1.4 + sz));
                    b.setType(Material.STONE_BRICK_WALL, false);
                }
            }
            podBlocks.add(blocks);
        }
    }

    int wedgeAt(double x, double z) {
        if (wedgeCount <= 0) return -1;
        return new DialGeometry(floorY, HUB_R, WEDGE_R, baseAngle).wedgeAt(x, z, wedgeCount);
    }

    int wedgeAt(Location l) {
        if (l == null || !isDomeWorld(l.getWorld())) return -1;
        if (l.getY() < floorY - 0.5 || l.getY() > floorY + 6) return -1;
        return wedgeAt(l.getX(), l.getZ());
    }

    void setDividers(double progress) {
        if (world == null) return;
        int height = progress <= 0.02 ? 0 : Math.max(1, Math.min(3, (int) Math.ceil(progress * 3.0)));
        for (Block floor : dividerBlocks) {
            for (int dy = 1; dy <= 3; dy++) {
                Block b = world.getBlockAt(floor.getX(), floor.getY() + dy, floor.getZ());
                if (dy <= height) b.setType(Material.WHITE_STAINED_GLASS_PANE, false);
                else if (b.getType() == Material.WHITE_STAINED_GLASS_PANE || b.getType() == Material.BARRIER) {
                    b.setType(Material.AIR, false);
                }
            }
        }
    }

    void setWedge(int slot, Wedge.State state, int colorIndex) {
        if (slot < 0 || slot >= wedgeBlocks.size()) return;
        Material m = switch (state) {
            case ACTIVE -> COLORS[Math.floorMod(colorIndex, COLORS.length)];
            case HIGHLIGHT -> Material.SEA_LANTERN;
            case WINNER -> Material.GOLD_BLOCK;
            case REMOVED, BLANK -> Material.BLACK_CONCRETE;
        };
        for (Block b : wedgeBlocks.get(slot)) b.setType(m, false);
    }

    void restoreWedges() {
        for (int i = 0; i < wedgeBlocks.size(); i++) setWedge(i, Wedge.State.ACTIVE, i);
    }

    void setPodColor(int pod, int colorIndex) {
        if (pod < 0 || pod >= podBlocks.size()) return;
        Material m = COLORS[Math.floorMod(colorIndex, COLORS.length)];
        for (Block b : podBlocks.get(pod)) b.setType(m, false);
    }

    void clearRuntime() {
        if (world == null) return;
        setDividers(0.0);
        restoreWedges();
        for (int i = 0; i < podBlocks.size(); i++) {
            for (Block b : podBlocks.get(i)) b.setType(Material.POLISHED_DEEPSLATE, false);
        }
    }

    Location center() { return new Location(world, 0.5, floorY, 0.5); }

    Location revealLocation() { return new Location(world, 0.5, floorY + 3.0, 0.5); }

    Location at(double x, double z, double dy) { return new Location(world, 0.5 + x, floorY + dy, 0.5 + z); }

    Location wedgeCenter(int slot) {
        if (wedgeCount <= 0) return center();
        double a = baseAngle + slot * (Math.PI * 2.0 / wedgeCount);
        double r = (HUB_R + WEDGE_R) / 2.0;
        return new Location(world, Math.cos(a) * r, floorY + 1, Math.sin(a) * r);
    }

    Location podSpawn(int pod) {
        double a = Math.toRadians(POD_ANGLES_DEG[Math.floorMod(pod, POD_ANGLES_DEG.length)]);
        double x = Math.cos(a) * 21.2;
        double z = Math.sin(a) * 21.2;
        float yaw = (float) Math.toDegrees(Math.atan2(x, -z));
        return new Location(world, x, floorY + 1, z, yaw, 0f);
    }

    Location hubSpot(Random rng) {
        double a = rng.nextDouble() * Math.PI * 2;
        double rad = 1.0 + rng.nextDouble() * (HUB_R - 2.5);
        double x = Math.cos(a) * rad, z = Math.sin(a) * rad;
        float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
        return new Location(world, x, floorY + 1, z, yaw, 0f);
    }

    static String labelText(int colorIndex, String id, int votes, boolean showVotes, boolean winner) {
        String col = COLOR_CODES[Math.floorMod(colorIndex, COLOR_CODES.length)];
        String name = McmApi.displayName(id);
        if (winner) return "§6§l★ " + name + " ★" + (showVotes ? "\n§e" + votes + (votes == 1 ? " vote" : " votes") : "");
        return col + "§l" + name + (showVotes ? "\n§f" + votes + (votes == 1 ? " §7vote" : " §7votes") : "");
    }

    static Material iconFor(String id) {
        McmGame g = McmApi.game(id);
        if (g != null) {
            try {
                Material m = g.icon();
                if (m != null && m != Material.COMMAND_BLOCK && m.isItem() && m != Material.AIR) return m;
            } catch (Throwable ignored) { }
        }
        String s = id == null ? "" : id.toLowerCase(Locale.ROOT);
        switch (s) {
            case "sg": return Material.IRON_SWORD;
            case "parkour": return Material.FEATHER;
            case "dropper": return Material.WATER_BUCKET;
            case "tntrun": return Material.TNT;
            case "spleef": return Material.DIAMOND_SHOVEL;
            case "kitpvp": return Material.SHIELD;
            case "survival": return Material.GOLDEN_APPLE;
            case "oneshot": return Material.BOW;
            default: break;
        }
        String[][] keys = {
                {"build", "BRICKS"}, {"sky", "ELYTRA"}, {"battle", "NETHERITE_SWORD"}, {"parkour", "FEATHER"},
                {"race", "LEATHER_BOOTS"}, {"rocket", "FIREWORK_ROCKET"}, {"wall", "BRICK_WALL"}, {"sand", "SAND"},
                {"boat", "OAK_BOAT"}, {"dodge", "SNOWBALL"}, {"bingo", "FILLED_MAP"}, {"melt", "BLUE_ICE"},
                {"grid", "CHEST"}, {"tgttos", "CHICKEN_SPAWN_EGG"}, {"chicken", "CHICKEN_SPAWN_EGG"},
                {"rush", "IRON_BOOTS"}, {"spleef", "DIAMOND_SHOVEL"}, {"tnt", "TNT"}, {"bow", "BOW"},
                {"pvp", "IRON_SWORD"}, {"drop", "WATER_BUCKET"}, {"hunt", "COMPASS"}, {"hide", "ENDER_EYE"},
                {"fish", "FISHING_ROD"}, {"paint", "BRUSH"}, {"music", "JUKEBOX"}, {"pit", "MAGMA_BLOCK"}
        };
        for (String[] k : keys) {
            if (s.contains(k[0])) {
                Material m = Material.matchMaterial(k[1]);
                if (m != null) return m;
            }
        }
        return g != null ? Material.COMMAND_BLOCK : Material.PAPER;
    }

    static Color colorFor(int index) {
        return switch (Math.floorMod(index, 8)) {
            case 0 -> Color.RED;
            case 1 -> Color.ORANGE;
            case 2 -> Color.YELLOW;
            case 3 -> Color.LIME;
            case 4 -> Color.GREEN;
            case 5 -> Color.TEAL;
            case 6 -> Color.AQUA;
            default -> Color.PURPLE;
        };
    }

    /** Generates no blocks at all. */
    static final class VoidGenerator extends ChunkGenerator {
        private final int y;
        VoidGenerator(int y) { this.y = y; }
        @Override public Location getFixedSpawnLocation(World world, Random random) {
            return new Location(world, 0.5, y + 1, 0.5);
        }
    }
}
