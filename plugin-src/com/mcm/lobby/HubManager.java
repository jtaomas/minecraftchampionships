package com.mcm.lobby;

import com.mcm.api.McmBuild;
import com.mcm.api.McmFx;
import com.mcm.api.McmLobby;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Level;

/**
 * Runtime side of the MCC hub: creates/loads world {@code mcc_lobby}, builds it once from {@link HubGenerator}
 * (tick-spread, marker file in the world folder), and runs the ambience (searchlights, fireworks, labels,
 * live leaderboard, team pads). Implements the {@link McmLobby.Venue} API for the Decision Dome.
 */
public final class HubManager implements Listener, McmLobby.Venue {
    public static final String WORLD = "mcc_lobby";
    public static final long SEED = 20261123L;
    private static final String TAG = "mcm_hub";
    private static final String MARKER = "mcm_hub_built.txt";

    private final JavaPlugin plugin;
    private final Supplier<List<String>> leaderboardLines;
    private final BiConsumer<Player, Integer> teamJoin;
    private final BooleanSupplier intermission;

    private World world;
    private HubLayout layout;
    private boolean ready = false;
    private boolean building = false;
    private McmBuild.BuildQueue queue;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private TextDisplay board;
    private final List<Entity> entities = new ArrayList<>();
    private final Map<UUID, Long> padCooldown = new HashMap<>();
    private final Map<String, BlockData> dataCache = new HashMap<>();

    public HubManager(JavaPlugin plugin, Supplier<List<String>> leaderboardLines, BiConsumer<Player, Integer> teamJoin, BooleanSupplier intermission) {
        this.plugin = plugin;
        this.leaderboardLines = leaderboardLines;
        this.teamJoin = teamJoin;
        this.intermission = intermission;
    }

    public boolean isReady() { return ready && world != null; }
    public boolean isBuilding() { return building; }
    @Override public World world() { return world; }
    public HubLayout layout() { return layout; }
    public boolean isHubWorld(World w) { return w != null && world != null && w.equals(world); }

    public Location spawn() {
        if (world == null || layout == null) return null;
        return new Location(world, layout.spawnX + 0.5, layout.spawnY, layout.spawnZ + 0.5, layout.spawnYaw, 0f);
    }

    // =====================================================================================================
    // lifecycle
    // =====================================================================================================

    public void enable() {
        world = createWorld();
        if (world == null) { plugin.getLogger().severe("[hub] could not create world " + WORLD); return; }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        removeTagged();
        // marker "v<N> built ...": same generator version -> just load the layout; an older version -> the
        // generator changed, so clear the old blocks and rebuild; no marker -> first build into the empty world
        int builtVersion = markedVersion();
        if (builtVersion == HubGenerator.VERSION) build(false, false);
        else {
            if (builtVersion > 0) plugin.getLogger().info("[hub] hub was built by generator v" + builtVersion
                    + ", current is v" + HubGenerator.VERSION + " - rebuilding");
            build(true, builtVersion > 0);
        }
    }

    public void disable() {
        HandlerList.unregisterAll(this);
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
        if (queue != null) queue.cancel();
        removeTagged();
        ready = false;
    }

    private World createWorld() {
        World w = Bukkit.getWorld(WORLD);
        if (w == null) {
            w = new WorldCreator(WORLD).environment(World.Environment.NORMAL).generateStructures(false)
                    .generator(new HubVoid()).createWorld();
        }
        if (w == null) return null;
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.DO_WARDEN_SPAWNING, false);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.MOB_GRIEFING, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setTime(18000L);
        w.setStorm(false);
        w.setThundering(false);
        w.setClearWeatherDuration(20 * 60 * 60 * 24);
        w.setKeepSpawnInMemory(false);
        w.getWorldBorder().setCenter(0, 0);
        w.getWorldBorder().setSize(2000);
        return w;
    }

    /** Generator version recorded in the build marker, 0 when there is no (readable) marker. */
    private int markedVersion() {
        try {
            File f = new File(world.getWorldFolder(), MARKER);
            if (!f.exists()) return 0;
            String s = Files.readString(f.toPath(), StandardCharsets.UTF_8).trim();
            if (!s.startsWith("v")) return 1; // unknown/legacy content: treat as an old build
            int e = 1;
            while (e < s.length() && Character.isDigit(s.charAt(e))) e++;
            return e > 1 ? Integer.parseInt(s.substring(1, e)) : 1;
        } catch (Throwable t) { return 1; }
    }

    private void mark() {
        try {
            Files.writeString(new File(world.getWorldFolder(), MARKER).toPath(),
                    "v" + HubGenerator.VERSION + " built " + new Date() + "\n", StandardCharsets.UTF_8);
        } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "[hub] could not write marker", t); }
    }

    /**
     * Generate the layout (async, pure Java) and, if {@code place}, place every block over ticks. With
     * {@code clearFirst} the old blocks in the hub's extent are cleared chunk by chunk first (rebuild).
     */
    public synchronized void build(boolean place, boolean clearFirst) {
        if (building || world == null) return;
        building = true;
        ready = place ? false : ready;
        if (place) { for (BukkitTask t : tasks) t.cancel(); tasks.clear(); removeTagged(); }
        plugin.getLogger().info("[hub] generating the MCC hub layout" + (place ? " and placing blocks (this takes a while)..." : "..."));
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Vox vox = new Vox();
            HubLayout lay;
            try {
                lay = new HubGenerator(vox, SEED).generate();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "[hub] generator failed", t);
                Bukkit.getScheduler().runTask(plugin, () -> building = false);
                return;
            }
            final HubLayout fl = lay;
            Bukkit.getScheduler().runTask(plugin, () -> {
                layout = fl;
                if (!place) { building = false; finishReady(); return; }
                placeAll(vox, clearFirst);
            });
        });
    }

    private BlockData data(String s) {
        BlockData d = dataCache.get(s);
        if (d != null) return d;
        try { d = Bukkit.createBlockData(s); }
        catch (Throwable t) {
            plugin.getLogger().warning("[hub] bad block state '" + s + "', using stone");
            d = Material.STONE.createBlockData();
        }
        dataCache.put(s, d);
        return d;
    }

    private void placeAll(Vox vox, boolean clearFirst) {
        List<String> pal = vox.palette();
        BlockData[] pd = new BlockData[pal.size()];
        for (int i = 1; i < pal.size(); i++) pd[i] = data(pal.get(i));
        final BlockData air = Material.AIR.createBlockData();
        queue = new McmBuild.BuildQueue(plugin, 60_000);
        // group sections by chunk column so each chunk is loaded once
        Map<Long, List<int[]>> byChunk = new LinkedHashMap<>();
        for (int[] s : vox.sectionCoords()) byChunk.computeIfAbsent(((long) s[0] << 32) ^ (s[2] & 0xffffffffL), k -> new ArrayList<>()).add(s);
        List<Long> keys = new ArrayList<>(byChunk.keySet());
        // build from the centre outwards so the plaza/venue appear first
        keys.sort(Comparator.comparingDouble(k -> { int cx = (int) (k >> 32), cz = (int) (long) k; return Math.hypot(cx, cz); }));
        int minY = layout.minY, maxY = Math.min(world.getMaxHeight() - 1, layout.maxY + 20);
        long[] total = {0};
        for (long k : keys) {
            int cx = (int) (k >> 32), cz = (int) k;
            if (clearFirst) queue.region(world, cx * 16, minY, cz * 16, cx * 16 + 15, maxY, cz * 16 + 15, (x, y, z) -> air, true);
            for (int[] s : byChunk.get(k)) {
                final short[] sec = vox.section(s[0], s[1], s[2]);
                final int bx = s[0] << 4, by = s[1] << 4, bz = s[2] << 4;
                for (short v : sec) if (v != 0) total[0]++;
                queue.region(world, bx, by, bz, bx + 15, by + 15, bz + 15, (x, y, z) -> {
                    int id = sec[((y - by) << 8) | ((z - bz) << 4) | (x - bx)];
                    return id == 0 ? null : pd[id];
                }, false);
            }
        }
        plugin.getLogger().info("[hub] queued " + total[0] + " blocks in " + keys.size() + " chunks");
        final BukkitTask progress = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (queue != null && queue.isRunning()) plugin.getLogger().info(String.format("[hub] building... %.0f%%", queue.progress() * 100));
        }, 200L, 200L);
        queue.then(() -> {
            progress.cancel();
            building = false;
            mark();
            plugin.getLogger().info("[hub] MCC hub built (" + total[0] + " blocks).");
            finishReady();
        });
        queue.start();
    }

    private void finishReady() {
        if (world == null || layout == null) return;
        world.setSpawnLocation(layout.spawnX, layout.spawnY, layout.spawnZ);
        world.getWorldBorder().setCenter(0, 0);
        world.getWorldBorder().setSize(2000);
        // keep the plaza + venue loaded so displays and beacons stay alive
        for (int cx = -5; cx <= 4; cx++) for (int cz = -9; cz <= 8; cz++) world.addPluginChunkTicket(cx, cz, plugin);
        ready = true;
        showCountdown(-1);
        resetPads();
        spawnDisplays();
        startAmbience();
    }

    /** /mcm lobby rebuild */
    public void rebuild() {
        if (building) return;
        ready = false;
        try { new File(world.getWorldFolder(), MARKER).delete(); } catch (Throwable ignored) {}
        build(true, true);
    }

    public String status() {
        if (world == null) return "world not loaded";
        if (building) return queue != null && queue.isRunning() ? String.format("building %.0f%%", queue.progress() * 100) : "generating layout";
        return ready ? "ready (spawn " + layout.spawnX + "," + layout.spawnY + "," + layout.spawnZ + ")" : "not built";
    }

    // =====================================================================================================
    // displays + ambience
    // =====================================================================================================

    private void removeTagged() {
        for (Entity e : entities) if (e != null && e.isValid()) e.remove();
        entities.clear();
        board = null;
        if (world == null) return;
        for (Entity e : world.getEntities()) if (e.getScoreboardTags().contains(TAG)) e.remove();
    }

    private TextDisplay text(double x, double y, double z, String legacy, float scale, Display.Billboard bb, float yaw) {
        Location l = new Location(world, x, y, z, yaw, 0f);
        TextDisplay td = world.spawn(l, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(TAG);
            d.setBillboard(bb);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setShadowed(true);
            d.setBackgroundColor(Color.fromARGB(110, 10, 12, 40));
            d.setViewRange(4.0f);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
            d.text(McmFx.text(legacy));
        });
        entities.add(td);
        return td;
    }

    private void spawnDisplays() {
        removeTagged();
        for (int i = 0; i < layout.labels.size(); i++) {
            double[] p = layout.labels.get(i);
            text(p[0], p[1], p[2], layout.labelText.get(i), 2.2f, Display.Billboard.VERTICAL, (float) p[3]);
        }
        double[] lb = layout.leaderboard;
        board = text(lb[0], lb[1] - 4.5, lb[2], "§7Loading standings...", 1.6f, Display.Billboard.VERTICAL, (float) lb[3]);
        board.setLineWidth(400);
        String[] names = {"Red", "Orange", "Yellow", "Lime", "Green", "Cyan", "Aqua", "Blue", "Purple", "Pink"};
        String[] codes = {"§c", "§6", "§e", "§a", "§2", "§3", "§b", "§9", "§5", "§d"};
        for (int i = 0; i < layout.teamPads.size() && i < names.length; i++) {
            double[] p = layout.teamPads.get(i);
            text(p[0], p[1] + 1.6, p[2], codes[i] + "§l" + names[i] + "\n§7step to join", 0.9f, Display.Billboard.CENTER, 0f);
        }
        updateBoard();
    }

    private void updateBoard() {
        if (board == null || !board.isValid()) return;
        try {
            board.text(McmFx.text(String.join("\n", leaderboardLines.get())));
        } catch (Throwable t) { /* ignore */ }
    }

    private void startAmbience() {
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
        // leaderboard refresh
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::updateBoard, 40L, 40L));
        // searchlight beams (particle shafts sweeping across the night sky)
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            long t = 0;
            @Override public void run() {
                t += 3;
                if (world.getPlayers().isEmpty()) return;
                Particle.DustOptions white = new Particle.DustOptions(Color.fromRGB(235, 240, 255), 3.0f);
                Particle.DustOptions gold = new Particle.DustOptions(Color.fromRGB(255, 214, 90), 3.0f);
                int i = 0;
                for (double[] s : layout.searchlights) {
                    double yaw = t * 0.012 + s[3] * 1.7;
                    double pitch = Math.toRadians(62 + 14 * Math.sin(t * 0.009 + s[3]));
                    double dx = Math.cos(yaw) * Math.cos(pitch), dy = Math.sin(pitch), dz = Math.sin(yaw) * Math.cos(pitch);
                    Particle.DustOptions c = (i++ % 2 == 0) ? white : gold;
                    for (double d = 2; d <= 96; d += 2.5) {
                        world.spawnParticle(Particle.DUST, s[0] + dx * d, s[1] + dy * d, s[2] + dz * d, 1, 0.15, 0.15, 0.15, 0, c, true);
                    }
                }
            }
        }, 20L, 3L));
        // ambient fireworks during intermissions
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (world.getPlayers().isEmpty() || !intermission.getAsBoolean()) return;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            if (r.nextDouble() < 0.45 || layout.fireworkSpots.isEmpty()) return;
            double[] p = layout.fireworkSpots.get(r.nextInt(layout.fireworkSpots.size()));
            ChatColor[] cols = {ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.AQUA, ChatColor.BLUE, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE};
            McmFx.teamFirework(new Location(world, p[0] + r.nextDouble(-14, 14), p[1] + r.nextDouble(-4, 10), p[2] + r.nextDouble(-14, 14)),
                    cols[r.nextInt(cols.length)], r.nextBoolean());
        }, 60L, 50L));
    }

    // =====================================================================================================
    // team pads
    // =====================================================================================================

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!ready || !isHubWorld(e.getTo().getWorld())) return;
        if (e.getFrom().getBlockX() == e.getTo().getBlockX() && e.getFrom().getBlockZ() == e.getTo().getBlockZ()
                && e.getFrom().getBlockY() == e.getTo().getBlockY()) return;
        Location to = e.getTo();
        for (int i = 0; i < layout.teamPads.size(); i++) {
            double[] p = layout.teamPads.get(i);
            if (Math.abs(to.getX() - p[0]) <= 1.5 && Math.abs(to.getZ() - p[2]) <= 1.5 && Math.abs(to.getY() - p[1]) < 1.2) {
                long now = System.currentTimeMillis();
                Long last = padCooldown.get(e.getPlayer().getUniqueId());
                if (last != null && now - last < 2500) return;
                padCooldown.put(e.getPlayer().getUniqueId(), now);
                try { teamJoin.accept(e.getPlayer(), i); } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "[hub] team pad", t); }
                return;
            }
        }
    }

    // =====================================================================================================
    // McmLobby.Venue
    // =====================================================================================================

    private Location loc(double x, double y, double z, float yaw) { return new Location(world, x, y, z, yaw, 0f); }

    @Override public Location hubSpawn() { return spawn(); }
    @Override public Location entrance() { double[] e = layout.entrance; return loc(e[0], e[1], e[2], (float) e[3]); }
    @Override public Location interiorSpawn() { double[] e = layout.interiorSpawn; return loc(e[0], e[1], e[2], (float) e[3]); }
    @Override public Location domeCenter() { return loc(layout.domeX + 0.5, layout.stageY + 1, layout.domeZ + 0.5, 0f); }
    @Override public int padCount() { return HubLayout.PAD_COUNT; }
    @Override public Location padCenter(int i) { double[] p = layout.pads[i]; return loc(p[0], p[1], p[2], 0f); }
    @Override public double padRadius() { return layout.padRadius; }
    @Override public Location revealPodium() { return loc(layout.podium[0], layout.podium[1], layout.podium[2], 0f); }
    @Override public Location interiorTimer() { return loc(layout.timer[0], layout.timer[1], layout.timer[2], 0f); }
    @Override public Location countdownOrigin() { return new Location(world, layout.clockX0, layout.clockY0, layout.clockZ); }

    @Override public List<Location> standSpots() {
        List<Location> out = new ArrayList<>();
        for (double[] s : layout.seats) out.add(loc(s[0], s[1], s[2], (float) s[3]));
        return out;
    }

    @Override public void setPad(int i, DyeColor color, boolean lit) {
        if (!ready || i < 0 || i >= HubLayout.PAD_COUNT) return;
        String dye = color == null ? null : color.name().toLowerCase(Locale.ROOT);
        for (int[] a : layout.padAccents[i]) {
            String b = switch (a[3]) {
                case HubLayout.K_CONCRETE -> a[1] == layout.stageY ? (dye == null ? "light_gray_concrete" : dye + "_concrete") : (dye == null ? "gray_concrete" : dye + "_concrete");
                case HubLayout.K_GLASS -> dye == null ? "gray_stained_glass" : dye + "_stained_glass";
                case HubLayout.K_WOOL -> dye == null ? "gray_wool" : dye + "_wool";
                case HubLayout.K_LIGHT -> lit ? (dye == null ? "sea_lantern" : "ochre_froglight") : "black_concrete";
                default -> null;
            };
            if (b != null) world.getBlockAt(a[0], a[1], a[2]).setBlockData(data("minecraft:" + b), false);
        }
    }

    @Override public void resetPads() {
        if (!ready) return;
        for (int i = 0; i < HubLayout.PAD_COUNT; i++) {
            DyeColor c;
            try { c = DyeColor.valueOf(Draw.PAD_DYES[i].toUpperCase(Locale.ROOT)); } catch (Throwable t) { c = DyeColor.WHITE; }
            setPad(i, c, true);
        }
    }

    @Override public void showCountdown(int seconds) {
        if (world == null || layout == null) return;
        String text = Glyphs.clockText(seconds);
        BlockData on = data(layout.clockOn), off = data(layout.clockOff);
        for (int c = 0; c < Glyphs.CLOCK_W; c++) for (int r = 0; r < Glyphs.SEG_H; r++) {
            world.getBlockAt(layout.clockX0 + c, layout.clockY0 + (Glyphs.SEG_H - 1 - r), layout.clockZ)
                    .setBlockData(Glyphs.clockPixel(text, c, r) ? on : off, false);
        }
    }

    /** Void generator for the hub world; spawn on the plaza. */
    static final class HubVoid extends McmBuild.VoidGenerator {
        @Override public Location getFixedSpawnLocation(World world, Random random) { return new Location(world, 0.5, 71, 108.5); }
    }
}
