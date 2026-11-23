package com.mcm.games.decisiondome;

import com.mcm.api.McmApi;
import com.mcm.api.McmGame;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

/** The generated dome: a void world with a hub, a ring of vote wedges and a glass hemisphere. */
final class DomeArena implements VoteStage {
    static final String WORLD_NAME = "mcc_dome";
    static final String TAG = "mcm_dome";
    static final int HUB_R = 6;       // neutral centre radius
    static final int WEDGE_R = 20;    // outer radius of the voting wedges
    static final int RING_R = 23;     // outer floor ring
    static final int SHELL_R = 24;    // glass dome radius
    static final double LABEL_R = 13.5;
    static final int MAX_WEDGES = 12;

    private static final Material[] COLORS = {
            Material.RED_CONCRETE, Material.ORANGE_CONCRETE, Material.YELLOW_CONCRETE, Material.LIME_CONCRETE,
            Material.GREEN_CONCRETE, Material.CYAN_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.BLUE_CONCRETE,
            Material.PURPLE_CONCRETE, Material.MAGENTA_CONCRETE, Material.PINK_CONCRETE, Material.WHITE_CONCRETE
    };
    static final String[] COLOR_CODES = {"§c", "§6", "§e", "§a", "§2", "§3", "§b", "§9", "§5", "§d", "§d", "§f"};

    private final Plugin plugin;
    private World world;
    private int floorY = 100;

    private int wedgeCount = 0;
    private final List<List<Block>> wedgeBlocks = new ArrayList<>();
    private final List<TextDisplay> labels = new ArrayList<>();
    private final List<ItemDisplay> icons = new ArrayList<>();
    private float iconYaw = 0f;

    DomeArena(Plugin plugin) { this.plugin = plugin; }

    @Override public World world() { return world; }
    int floorY() { return floorY; }
    int wedgeCount() { return wedgeCount; }
    boolean isDomeWorld(World w) { return world != null && w != null && w.equals(world); }

    static Component c(String legacy) { return LegacyComponentSerializer.legacySection().deserialize(legacy); }

    /** Create / load the void world and build the static parts of the dome. Safe to call repeatedly. */
    World ensureWorld() {
        if (world != null && Bukkit.getWorld(WORLD_NAME) == world) return world;
        World main = Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
        // The core teleports LOBBY-state players whose Y drops below (lobbySpawnY - 50), in any world,
        // so keep the dome floor at or above the main world's spawn height.
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
        buildWedges(1);
        return w;
    }

    void removeStrayEntities() {
        if (world == null) return;
        for (Entity e : world.getEntities()) if (e.getScoreboardTags().contains(TAG)) e.remove();
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
                    else if (r > 21.6 && r <= 22.6 && (Math.floorMod(x * 7 + z * 13, 9) == 0)) m = Material.SEA_LANTERN;
                    world.getBlockAt(x, floorY, z).setType(m, false);
                }
                for (int dy = 1; dy <= SHELL_R + 1; dy++) {
                    double d = Math.sqrt((x + 0.5) * (x + 0.5) + (dy - 0.5) * (dy - 0.5) + (z + 0.5) * (z + 0.5));
                    Block b = world.getBlockAt(x, floorY + dy, z);
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

    /** Index of the wedge at the given horizontal position, or -1 if on the hub / outside. */
    int wedgeAt(double x, double z) {
        if (wedgeCount <= 0) return -1;
        double r = Math.hypot(x, z);
        if (r <= HUB_R || r > WEDGE_R + 0.6) return -1;
        double a = Math.atan2(z, x);
        if (a < 0) a += Math.PI * 2;
        int idx = (int) Math.floor(a / (Math.PI * 2 / wedgeCount));
        return Math.min(Math.max(idx, 0), wedgeCount - 1);
    }

    int wedgeAt(Location l) {
        if (l == null || !isDomeWorld(l.getWorld())) return -1;
        if (l.getY() < floorY || l.getY() > floorY + 6) return -1;
        return wedgeAt(l.getX(), l.getZ());
    }

    /** Lay out n wedges (floor colours + dividers). Removes labels. */
    void buildWedges(int n) {
        clearLabels();
        n = Math.max(1, Math.min(MAX_WEDGES, n));
        wedgeCount = n;
        wedgeBlocks.clear();
        for (int i = 0; i < n; i++) wedgeBlocks.add(new ArrayList<>());
        double w = Math.PI * 2 / n;
        for (int x = -WEDGE_R - 1; x <= WEDGE_R + 1; x++) {
            for (int z = -WEDGE_R - 1; z <= WEDGE_R + 1; z++) {
                double cx = x + 0.5, cz = z + 0.5;
                double r = Math.hypot(cx, cz);
                if (r <= HUB_R || r > WEDGE_R) continue;
                double a = Math.atan2(cz, cx);
                if (a < 0) a += Math.PI * 2;
                int idx = Math.min((int) Math.floor(a / w), n - 1);
                double local = a - idx * w;
                double arc = Math.min(local, w - local) * r;
                Block floor = world.getBlockAt(x, floorY, z);
                Block above = world.getBlockAt(x, floorY + 1, z);
                if (n > 1 && arc < 0.6) {
                    floor.setType(Material.POLISHED_BLACKSTONE, false);
                    above.setType(r >= HUB_R + 2.5 ? Material.WHITE_STAINED_GLASS : Material.AIR, false);
                } else {
                    floor.setType(COLORS[idx % COLORS.length], false);
                    above.setType(Material.AIR, false);
                    wedgeBlocks.get(idx).add(floor);
                }
            }
        }
    }

    void spawnLabels(List<String> ids) {
        clearLabels();
        double w = Math.PI * 2 / wedgeCount;
        for (int i = 0; i < wedgeCount && i < ids.size(); i++) {
            double a = (i + 0.5) * w;
            double lx = Math.cos(a) * LABEL_R, lz = Math.sin(a) * LABEL_R;
            final String id = ids.get(i);
            final int idx = i;
            Location tl = new Location(world, lx, floorY + 4.3, lz);
            TextDisplay td = world.spawn(tl, TextDisplay.class, d -> {
                d.setPersistent(false);
                d.addScoreboardTag(TAG);
                d.setBillboard(Display.Billboard.CENTER);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setShadowed(true);
                d.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.6f, 1.6f, 1.6f), new AxisAngle4f()));
                d.text(c(labelText(idx, id, 0, false)));
            });
            labels.add(td);
            Location il = new Location(world, lx, floorY + 2.4, lz);
            ItemDisplay idp = world.spawn(il, ItemDisplay.class, d -> {
                d.setPersistent(false);
                d.addScoreboardTag(TAG);
                d.setItemStack(new ItemStack(iconFor(id)));
                d.setBillboard(Display.Billboard.FIXED);
                d.setTeleportDuration(3);
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.5f, 1.5f, 1.5f), new AxisAngle4f()));
            });
            icons.add(idp);
        }
    }

    static String labelText(int idx, String id, int votes, boolean winner) {
        String col = COLOR_CODES[idx % COLOR_CODES.length];
        String name = McmApi.displayName(id);
        if (winner) return "§6§l★ " + name + " ★\n§e" + votes + (votes == 1 ? " vote" : " votes");
        return col + "§l" + name + "\n§f" + votes + (votes == 1 ? " §7vote" : " §7votes");
    }

    @Override public void updateLabel(int idx, String id, int votes, boolean winner) {
        if (idx < 0 || idx >= labels.size()) return;
        TextDisplay td = labels.get(idx);
        if (td != null && td.isValid()) td.text(c(labelText(idx, id, votes, winner)));
    }

    /** Slowly spin the floating icons. Call every few ticks. */
    void spinIcons() {
        iconYaw = (iconYaw + 9f) % 360f;
        for (ItemDisplay d : icons) if (d != null && d.isValid()) d.setRotation(iconYaw, 0f);
    }

    void setWedgeMaterial(int idx, Material m) {
        if (idx < 0 || idx >= wedgeBlocks.size()) return;
        for (Block b : wedgeBlocks.get(idx)) b.setType(m, false);
    }

    void restoreWedge(int idx) { setWedgeMaterial(idx, COLORS[idx % COLORS.length]); }

    void restoreAllWedges() { for (int i = 0; i < wedgeBlocks.size(); i++) restoreWedge(i); }

    Location labelLocation(int idx) {
        double a = (idx + 0.5) * (Math.PI * 2 / Math.max(1, wedgeCount));
        return new Location(world, Math.cos(a) * LABEL_R, floorY + 1.5, Math.sin(a) * LABEL_R);
    }

    /** Random spot on the hub, facing outwards. */
    Location hubSpot(Random rng) {
        double a = rng.nextDouble() * Math.PI * 2;
        double rad = 1.0 + rng.nextDouble() * (HUB_R - 2.5);
        double x = Math.cos(a) * rad, z = Math.sin(a) * rad;
        float yaw = (float) Math.toDegrees(Math.atan2(-x, z));
        return new Location(world, x, floorY + 1, z, yaw, 0f);
    }

    void clearLabels() {
        for (TextDisplay d : labels) if (d != null && d.isValid()) d.remove();
        for (ItemDisplay d : icons) if (d != null && d.isValid()) d.remove();
        labels.clear();
        icons.clear();
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


    // ---------------- VoteStage (fallback dome world) ----------------
    @Override public int maxOptions() { return MAX_WEDGES; }
    @Override public boolean prepare(List<String> ids) {
        if (ensureWorld() == null) return false;
        buildWedges(ids.size());
        spawnLabels(ids);
        return true;
    }
    @Override public boolean isStageWorld(World w) { return isDomeWorld(w); }
    @Override public Location arrival(Random rng) { return hubSpot(rng); }
    @Override public int optionAt(Location l) { return wedgeAt(l); }
    @Override public void tickVisuals(int tick) { if (tick % 3 == 0) spinIcons(); }
    @Override public void setCountdown(int seconds) { }
    @Override public void highlight(int idx, boolean on) { if (on) setWedgeMaterial(idx, Material.SEA_LANTERN); else restoreWedge(idx); }
    @Override public void eliminate(int idx, String id) { setWedgeMaterial(idx, Material.GRAY_CONCRETE); }
    @Override public void winner(int idx, String id, int votes) { setWedgeMaterial(idx, Material.GOLD_BLOCK); updateLabel(idx, id, votes, true); }
    @Override public Location optionLocation(int idx) { return labelLocation(idx); }
    @Override public Location revealLocation() { return new Location(world, 0.5, floorY + 1, 0.5); }
    @Override public void clear() { if (world != null) { clearLabels(); restoreAllWedges(); } }
    @Override public double voidY() { return floorY - 6; }

    /** Generates no blocks at all. */
    static final class VoidGenerator extends ChunkGenerator {
        private final int y;
        VoidGenerator(int y) { this.y = y; }
        @Override public Location getFixedSpawnLocation(World world, Random random) { return new Location(world, 0.5, y + 1, 0.5); }
    }
}
