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
import java.util.function.IntFunction;
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

    private static final Color[] PAD_COLORS = {
            Color.fromRGB(255, 65, 65), Color.fromRGB(255, 155, 45), Color.fromRGB(255, 225, 55),
            Color.fromRGB(115, 240, 65), Color.fromRGB(35, 175, 55), Color.fromRGB(35, 185, 195),
            Color.fromRGB(85, 210, 255), Color.fromRGB(65, 105, 255), Color.fromRGB(175, 70, 240),
            Color.fromRGB(255, 125, 200)
    };
    private static final ChatColor[] PAD_CHAT = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.DARK_GREEN,
            ChatColor.DARK_AQUA, ChatColor.AQUA, ChatColor.BLUE, ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE
    };
    private static final String[] TEAM_NAMES = {"Red", "Orange", "Yellow", "Lime", "Green", "Cyan", "Aqua", "Blue", "Purple", "Pink"};
    private static final String[] TEAM_CODES = {"§c", "§6", "§e", "§a", "§2", "§3", "§b", "§9", "§5", "§d"};

    private final JavaPlugin plugin;
    private final Supplier<List<String>> leaderboardLines;
    private final BiConsumer<Player, Integer> teamJoin;
    private final BooleanSupplier intermission;
    private final IntFunction<String> teamPadText;

    private World world;
    private HubLayout layout;
    private boolean ready = false;
    private boolean building = false;
    private McmBuild.BuildQueue queue;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private TextDisplay board;
    private final List<TextDisplay> teamDisplays = new ArrayList<>();
    private final List<Entity> entities = new ArrayList<>();
    private final List<Entity> voteEntities = new ArrayList<>();
    private final String[] podState = new String[10];
    private final Map<UUID, Long> padCooldown = new HashMap<>();
    private final Map<String, BlockData> dataCache = new HashMap<>();

    public HubManager(JavaPlugin plugin, Supplier<List<String>> leaderboardLines, BiConsumer<Player, Integer> teamJoin, BooleanSupplier intermission) {
        this(plugin, leaderboardLines, teamJoin, intermission, null);
    }

    public HubManager(JavaPlugin plugin, Supplier<List<String>> leaderboardLines, BiConsumer<Player, Integer> teamJoin,
                      BooleanSupplier intermission, IntFunction<String> teamPadText) {
        this.plugin = plugin;
        this.leaderboardLines = leaderboardLines;
        this.teamJoin = teamJoin;
        this.intermission = intermission;
        this.teamPadText = teamPadText;
        java.util.Arrays.fill(podState, "normal");
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
        setDial(layout.dialCapacity);
        for (int i = 0; i < 10; i++) setPodState(i, "normal");
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
        teamDisplays.clear();
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
            d.setBackgroundColor(Color.fromARGB(135, 10, 12, 40));
            d.setViewRange(4.0f);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale, scale, scale), new AxisAngle4f()));
            d.text(McmFx.text(legacy));
        });
        entities.add(td);
        return td;
    }

    public void refreshDisplays() {
        if (!ready) return;
        updateBoard();
        updateTeamDisplays();
    }

    private void spawnDisplays() {
        removeTagged();
        for (int i = 0; i < layout.labels.size(); i++) {
            double[] p = layout.labels.get(i);
            Display.Billboard bb = p[3] != 0 ? Display.Billboard.FIXED : Display.Billboard.VERTICAL;
            TextDisplay lbl = text(p[0], p[1], p[2], layout.labelText.get(i), 2.1f, bb, (float) p[3]);
            lbl.setLineWidth(380);
        }
        double[] lb = layout.leaderboard;
        board = text(lb[0] + 0.15, lb[1] - 0.5, lb[2], "§7Loading standings...", 1.3f, Display.Billboard.FIXED, (float) lb[3]);
        board.setBackgroundColor(Color.fromARGB(190, 10, 14, 34));
        board.setLineWidth(460);
        for (int i = 0; i < layout.teamPads.size() && i < TEAM_NAMES.length; i++) {
            double[] p = layout.teamPads.get(i);
            String label = teamPadLabelText(i);
            TextDisplay td = text(p[0], p[1] + 3.8, p[2], label, 1.15f, Display.Billboard.VERTICAL, 0f);
            td.setBackgroundColor(Color.fromARGB(165, 10, 12, 34));
            td.setLineWidth(240);
            teamDisplays.add(td);
        }
        refreshDisplays();
    }

    private String teamPadLabelText(int i) {
        if (teamPadText != null) {
            try {
                String txt = teamPadText.apply(i);
                if (txt != null && !txt.isBlank()) return txt;
            } catch (Throwable ignored) {}
        }
        return TEAM_CODES[i] + "§l" + TEAM_NAMES[i] + "\n§e▶ STEP TO JOIN";
    }

    private void updateTeamDisplays() {
        for (int i = 0; i < teamDisplays.size(); i++) {
            TextDisplay td = teamDisplays.get(i);
            if (td != null && td.isValid()) {
                td.text(McmFx.text(teamPadLabelText(i)));
            }
        }
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
        // leaderboard + team booth label refresh
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::refreshDisplays, 40L, 40L));
        // shimmering team booth particle rings
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            double phase = 0;
            @Override public void run() {
                if (world.getPlayers().isEmpty() || layout == null) return;
                phase += 0.45;
                for (int i = 0; i < layout.teamPads.size() && i < PAD_COLORS.length; i++) {
                    double[] p = layout.teamPads.get(i);
                    Particle.DustOptions dust = new Particle.DustOptions(PAD_COLORS[i], 1.15f);
                    for (int k = 0; k < 2; k++) {
                        double ang = phase + k * Math.PI;
                        world.spawnParticle(Particle.DUST,
                                p[0] + Math.cos(ang) * 1.35, p[1] + 0.25, p[2] + Math.sin(ang) * 1.35,
                                1, 0.04, 0.05, 0.04, 0, dust, false);
                    }
                }
            }
        }, 20L, 8L));
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
        // ambient fireworks during intermissions: config mcc-hub.ambient-fireworks-per-minute (default 3, 0 = off;
        // was ~13/min). Real events (game wins, the podium, the vote reveal) fire their own shows independently.
        final double perMinute = Math.max(0, plugin.getConfig().getDouble("mcc-hub.ambient-fireworks-per-minute", 5.0));
        if (perMinute > 0) tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (world.getPlayers().isEmpty() || !intermission.getAsBoolean()) return;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            // checked once a second: chance perMinute / 60 per check
            if (r.nextDouble() >= perMinute / 60.0 || layout.fireworkSpots.isEmpty()) return;
            double[] p = layout.fireworkSpots.get(r.nextInt(layout.fireworkSpots.size()));
            ChatColor[] cols = {ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.AQUA, ChatColor.BLUE, ChatColor.LIGHT_PURPLE, ChatColor.DARK_PURPLE};
            McmFx.teamFirework(new Location(world, p[0] + r.nextDouble(-14, 14), p[1] + r.nextDouble(-4, 10), p[2] + r.nextDouble(-14, 14)),
                    cols[r.nextInt(cols.length)], r.nextBoolean());
        }, 60L, 20L));
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
        if (rescueFromPit(e.getPlayer(), to)) return;
        for (int i = 0; i < layout.teamPads.size(); i++) {
            double[] p = layout.teamPads.get(i);
            if (Math.abs(to.getX() - p[0]) <= 1.5 && Math.abs(to.getZ() - p[2]) <= 1.5 && Math.abs(to.getY() - p[1]) < 1.2) {
                long now = System.currentTimeMillis();
                Long last = padCooldown.get(e.getPlayer().getUniqueId());
                if (last != null && now - last < 2500) return;
                padCooldown.put(e.getPlayer().getUniqueId(), now);
                try {
                    teamJoin.accept(e.getPlayer(), i);
                    if (i < PAD_CHAT.length) {
                        McmFx.teamFirework(new Location(world, p[0], p[1] + 5.5, p[2]), PAD_CHAT[i], false);
                    }
                    e.getPlayer().playSound(to, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);
                    refreshDisplays();
                } catch (Throwable t) { plugin.getLogger().log(Level.WARNING, "[hub] team pad", t); }
                return;
            }
        }
    }

    /** v6: anyone who drops into the recreation's 42-deep pit is put back on the terrace instead of stranded. */
    private boolean rescueFromPit(Player p, Location to) {
        if (layout.dialBandY == Integer.MIN_VALUE || to.getY() >= layout.stageY - 6) return false;
        GameMode gm = p.getGameMode();
        if (gm == GameMode.SPECTATOR || gm == GameMode.CREATIVE) return false;
        if (Math.hypot(to.getX() - (layout.domeX + 0.5), to.getZ() - (layout.domeZ + 0.5)) > 27.0) return false;
        Location back = interiorSpawn();
        plugin.getServer().getScheduler().runTask(plugin, () -> { p.setFallDistance(0f); p.teleport(back); });
        return true;
    }

    // =====================================================================================================
    // McmLobby.Venue
    // =====================================================================================================

    private Location loc(double x, double y, double z, float yaw) { return new Location(world, x, y, z, yaw, 0f); }

    @Override public Location hubSpawn() { return spawn(); }
    @Override public Location entrance() { double[] e = layout.entrance; return loc(e[0], e[1], e[2], (float) e[3]); }
    @Override public Location interiorSpawn() { double[] e = layout.interiorSpawn; return loc(e[0], e[1], e[2], (float) e[3]); }
    @Override public Location domeCenter() { return loc(layout.domeX + 0.5, layout.stageY + 1, layout.domeZ + 0.5, 0f); }
    @Override public int padCount() { return dialCapacity(); }
    @Override public Location padCenter(int i) { return wedgeCenter(i); }
    @Override public double padRadius() { return 2.0; }
    @Override public Location revealPodium() { return loc(layout.podium[0], layout.podium[1], layout.podium[2], 0f); }
    @Override public Location interiorTimer() { return loc(layout.timer[0], layout.timer[1], layout.timer[2], 0f); }
    @Override public Location countdownOrigin() { return new Location(world, layout.clockX0, layout.clockY0, layout.clockZ); }

    @Override public List<Location> standSpots() {
        List<Location> out = new ArrayList<>();
        for (double[] s : layout.seats) out.add(loc(s[0], s[1], s[2], (float) s[3]));
        return out;
    }

    // =====================================================================================================
    // Venue v2 — dial, pods, dividers, reveal
    // =====================================================================================================

    private int dialN() { return layout == null ? 8 : (layout.dialCapacity == 6 ? 6 : 8); }
    private String[] dialDyes(int n) { return n == 6 ? HubVenue.WEDGE_DYES6 : HubVenue.WEDGE_DYES8; }

    /** Y of the repaintable wedge band (v6: the dial's rim course; older layouts: the stage floor). */
    private int bandY() { return layout.dialBandY != Integer.MIN_VALUE ? layout.dialBandY : layout.stageY; }

    private void setBlock(int x, int y, int z, String id) {
        if (world == null || layout == null) return;
        if (x < layout.minX - 2 || x > layout.maxX + 2 || z < layout.minZ - 2 || z > layout.maxZ + 2) return;
        world.getBlockAt(x, y, z).setBlockData(data("minecraft:" + id), false);
    }

    private void putAccent(int[] a, String dye, boolean lit, boolean force) {
        int kind = a[3];
        if (kind == HubLayout.K_KEEP && !force) return;
        setBlock(a[0], a[1], a[2], HubVenue.accentBlock(kind, dye, lit));
    }

    private void clearAccent(int[] a) { setBlock(a[0], a[1], a[2], "air"); }

    private void paintBand(int n, String[] dyes, boolean restore) {
        for (int[] c : layout.dialBand) {
            int idx = HubVenue.wedgeIndex(HubVenue.angDeg(c[0], c[1]), n);
            putAccent(new int[]{c[0], bandY(), c[1], c[2]}, dyes[idx % dyes.length], true, restore);
        }
        paintSeams(n);
    }

    /** Keep the active divider seam as a dark inlay over the colour field. */
    private void paintSeams(int n) {
        for (int[] c : (n == 6 ? layout.dividerSeam6 : layout.dividerSeam8)) {
            setBlock(c[0], bandY(), c[1], "deepslate_tiles");
        }
    }

    /** Show the active wedge set, hide the other, and repaint the shared colour field. */
    private void applyDialMode() {
        if (!ready || layout == null) return;
        int n = dialN();
        String[] dyes = dialDyes(n);
        // hide the inactive set first (the 8 and 6 totems overlap at the cardinal axes)
        if (n == 8) {
            for (List<int[]> l : layout.wedgeAccents6) for (int[] a : l) clearAccent(a);
        } else {
            for (List<int[]> l : layout.wedgeAccents8) for (int[] a : l) clearAccent(a);
        }
        // then place the active set
        if (n == 8) {
            for (int i = 0; i < 8; i++) for (int[] a : layout.wedgeAccents8[i]) putAccent(a, dyes[i], true, true);
        } else {
            for (int i = 0; i < 6; i++) for (int[] a : layout.wedgeAccents6[i]) putAccent(a, dyes[i], true, true);
        }
        paintBand(n, dyes, true);
    }

    /** Restore every active wedge to its built jewel colour without touching dividers or pods. */
    private void restoreWedges() {
        if (!ready || layout == null) return;
        int n = dialN();
        String[] dyes = dialDyes(n);
        if (n == 8) {
            for (int i = 0; i < 8; i++) for (int[] a : layout.wedgeAccents8[i]) putAccent(a, dyes[i], true, false);
        } else {
            for (int i = 0; i < 6; i++) for (int[] a : layout.wedgeAccents6[i]) putAccent(a, dyes[i], true, false);
        }
        paintBand(n, dyes, false);
    }

    @Override public int apiVersion() { return 2; }

    @Override public int dialCapacity() { return layout == null ? 8 : dialN(); }

    @Override public void setDial(int games) {
        if (layout == null) return;
        layout.dialCapacity = games <= 6 ? 6 : 8;
        if (!ready) return;
        applyDialMode();
        setDividers(0);
    }

    @Override public Location wedgeCenter(int i) {
        int n = dialCapacity();
        if (i < 0) i = 0;
        if (i >= n) i = n - 1;
        double[] w = n == 6 ? layout.wedge6[i] : layout.wedge8[i];
        if (w[0] != 0 || w[2] != 0) return loc(w[0], w[1], w[2], (float) w[3]);
        double a = Math.toRadians(90.0 + i * (360.0 / n));
        return loc(HubVenue.CX + 0.5 + Math.cos(a) * 8.5, layout.stageY + 1, HubVenue.CZ + 0.5 + Math.sin(a) * 8.5, 0f);
    }

    @Override public double wedgeRadius() { return layout == null ? 12.0 : layout.dialRadius; }

    @Override public double wedgeInnerRadius() { return layout == null ? wedgeRadius() * 0.62 : layout.dialInner; }

    @Override public void setWedge(int i, DyeColor color, boolean lit) {
        if (!ready || layout == null) return;
        int n = dialN();
        if (i < 0 || i >= n) return;
        String dye = color == null ? null : color.name().toLowerCase(Locale.ROOT);
        List<int[]> acc = n == 8 ? layout.wedgeAccents8[i] : layout.wedgeAccents6[i];
        for (int[] a : acc) putAccent(a, dye, lit, false);
        for (int[] c : layout.dialBand) {
            if (HubVenue.wedgeIndex(HubVenue.angDeg(c[0], c[1]), n) != i) continue;
            putAccent(new int[]{c[0], bandY(), c[1], c[2]}, dye, lit, true);
        }
        paintSeams(n);
    }

    @Override public int podCount() { return 10; }

    @Override public Location podSpawn(int i) {
        if (i < 0) i = 0;
        if (i > 9) i = 9;
        double[] p = layout.pods[i];
        return loc(p[0], p[1], p[2], (float) p[3]);
    }

    @Override public void setPodGate(int i, boolean open) {
        if (!ready || i < 0 || i >= 10) return;
        for (int[] a : layout.podGates[i]) {
            if (open) setBlock(a[0], a[1], a[2], "air");
            else setBlock(a[0], a[1], a[2], a[1] == layout.stageY + 2 ? "cut_copper" : "black_concrete");
        }
    }

    @Override public void setPodState(int i, String state) {
        if (!ready || i < 0 || i >= 10) return;
        String st = state == null ? "normal" : state.toLowerCase(Locale.ROOT);
        if (!st.equals("normal") && !st.equals("caged") && !st.equals("dunked") && !st.equals("frozen") && !st.equals("invaded")) st = "normal";
        podState[i] = st;
        String team = Draw.TEAM_DYES[i];
        String dye;
        boolean lit;
        switch (st) {
            case "caged" -> { dye = "gray"; lit = false; }
            case "dunked" -> { dye = "cyan"; lit = false; }
            case "frozen" -> { dye = "light_blue"; lit = true; }
            case "invaded" -> { dye = "magenta"; lit = true; }
            default -> { dye = team; lit = true; }
        }
        java.util.Set<Long> podKeys = new java.util.HashSet<>();
        for (int[] a : layout.podAccents[i]) if (a[3] == HubLayout.K_KEEP) podKeys.add(podKey(a[0], a[1], a[2]));
        for (int[] a : layout.podAccents[i]) {
            int kind = a[3];
            if (kind == HubLayout.K_KEEP) continue;
            if (podKeys.contains(podKey(a[0], a[1], a[2]))) continue;
            setBlock(a[0], a[1], a[2], HubVenue.accentBlock(kind, dye, lit));
        }
        for (int[] a : layout.podBars[i]) setBlock(a[0], a[1], a[2], "caged".equals(st) ? "iron_bars" : "air");
        setPodGate(i, !st.equals("caged") && !st.equals("frozen"));
    }

    private static long podKey(int x, int y, int z) {
        return ((long) x << 40) ^ ((long) (y & 0xFFFF) << 20) ^ (z & 0xFFFFFL);
    }

    @Override public void setDividers(double progress) {
        if (!ready || layout == null) return;
        double p = Math.max(0.0, Math.min(1.0, progress));
        int n = dialN();
        for (int set = 0; set < 2; set++) {
            boolean active = (set == 0) == (n == 8);
            List<int[]>[] lists = set == 0 ? layout.divider8 : layout.divider6;
            for (List<int[]> l : lists) for (int[] a : l) {
                int h = a[1] - layout.stageY;
                if (active && p * 3.0 + 1e-6 >= h) {
                    String b = h == 3 && ((a[0] + a[2]) & 1) == 0 ? "chiseled_polished_blackstone" : "polished_blackstone";
                    setBlock(a[0], a[1], a[2], b);
                } else setBlock(a[0], a[1], a[2], "air");
            }
        }
    }

    @Override public void clearRuntime() {
        if (!ready) return;
        clearVoteTexts();
        setDividers(0);
        for (int i = 0; i < 10; i++) {
            setPodGate(i, true);
            setPodState(i, "normal");
        }
        applyDialMode();
    }

    @Override public void revealWedge(int i, String gameId) {
        if (!ready || layout == null) return;
        int n = dialN();
        if (i < 0 || i >= n) return;
        setDividers(1.0);
        for (int j = 0; j < n; j++) setWedge(j, null, false);
        setWedge(i, DyeColor.YELLOW, true);
        List<int[]> acc = n == 8 ? layout.wedgeAccents8[i] : layout.wedgeAccents6[i];
        for (int[] a : acc) {
            int kind = a[3];
            if (kind == HubLayout.K_CONCRETE || kind == HubLayout.K_WOOL || kind == HubLayout.K_GLASS) setBlock(a[0], a[1], a[2], "gold_block");
            else if (kind == HubLayout.K_LIGHT) setBlock(a[0], a[1], a[2], "ochre_froglight");
        }
        for (int[] c : layout.dialBand) {
            if (HubVenue.wedgeIndex(HubVenue.angDeg(c[0], c[1]), n) != i) continue;
            setBlock(c[0], bandY(), c[1], c[2] == HubLayout.K_LIGHT ? "ochre_froglight" : "gold_block");
        }
        paintSeams(n);
        revealText(gameId);
    }

    @Override public int dialGeneration() { return layout == null ? 0 : layout.dialGeneration; }

    @Override public Location revealSurface(int face) {
        if (face < 0) face = 0;
        if (face > 3) face = 3;
        double[] f = layout.revealSurfaces[face];
        return loc(f[0], f[1], f[2], (float) f[3]);
    }

    @Override public Location interiorClock() {
        double[] c = layout.clock[0] == 0 && layout.clock[2] == 0 ? layout.timer : layout.clock;
        return loc(c[0], c[1], c[2], 0f);
    }

    @Override public void setPad(int i, DyeColor color, boolean lit) { setWedge(i, color, lit); }

    @Override public void resetPads() {
        if (!ready) return;
        restoreWedges();
    }

    private void clearVoteTexts() {
        for (Entity e : voteEntities) if (e != null && e.isValid()) e.remove();
        voteEntities.clear();
    }

    /** Four-sided reveal title on the orrery cube faces. */
    private void revealText(String gameId) {
        clearVoteTexts();
        if (world == null || layout == null) return;
        String name = gameId == null || gameId.isBlank() ? "CHOSEN" : gameId.replace('_', ' ').toUpperCase(Locale.ROOT);
        for (double[] f : layout.revealSurfaces) {
            Location at = new Location(world, f[0], f[1], f[2], (float) f[3], 0f);
            TextDisplay td = world.spawn(at, TextDisplay.class, d -> {
                d.setPersistent(false);
                d.addScoreboardTag(TAG);
                d.setBillboard(Display.Billboard.CENTER);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setShadowed(true);
                d.setBackgroundColor(Color.fromARGB(150, 8, 10, 26));
                d.setViewRange(7.0f);
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.5f, 1.5f, 1.5f), new AxisAngle4f()));
                d.text(McmFx.text("§6§l" + name));
            });
            if (td != null) voteEntities.add(td);
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
