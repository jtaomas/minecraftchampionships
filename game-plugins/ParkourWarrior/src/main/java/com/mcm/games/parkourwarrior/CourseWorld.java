package com.mcm.games.parkourwarrior;

import com.mcm.api.McmBuild;
import com.mcm.games.parkourwarrior.CourseModel.Kind;
import com.mcm.games.parkourwarrior.CourseModel.Obstacle;
import com.mcm.games.parkourwarrior.CourseModel.Pos;
import com.mcm.games.parkourwarrior.stub.StubCourse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Owns the {@code mcc_parkourwarrior} world: picks the generator, places a {@link CourseModel} with a
 * {@link McmBuild.BuildQueue}, spawns labels, keeps the course chunks loaded during a game and toggles timed blocks.
 */
final class CourseWorld {
    static final String WORLD_NAME = "mcc_parkourwarrior";
    static final String MAP_CLASS = "com.mcm.games.parkourwarrior.map.CourseMap";
    static final String TAG = "pw_course";

    enum State { NONE, GENERATING, BUILDING, READY, FAILED }

    private final JavaPlugin plugin;
    private World world;
    private CourseModel model;
    private State state = State.NONE;
    private String generatorUsed = "-";
    private String builtKey;
    private String pendingKey;
    private int offX, offY, offZ;
    private final List<Runnable> waiting = new ArrayList<>();
    private McmBuild.BuildQueue queue;
    private int generation;
    private final Map<String, BlockData> cache = new HashMap<>();
    private int[] timedState = new int[0];
    private final Set<Long> tickets = new HashSet<>();

    CourseWorld(JavaPlugin plugin) { this.plugin = plugin; }

    World world() { return world; }
    CourseModel model() { return model; }
    State state() { return state; }
    String generatorUsed() { return generatorUsed; }
    boolean ready() { return state == State.READY && model != null && world != null; }
    boolean busy() { return state == State.GENERATING || state == State.BUILDING; }

    // ------------------------------------------------------------------ world

    World ensureWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w != null && (w.getGenerator() == null || !w.getGenerator().getClass().getSimpleName().contains("Void"))) {
            // owner: oceans either side of the course - Multiverse loads the world with the vanilla generator first
            plugin.getLogger().warning("Parkour Warrior: " + WORLD_NAME + " was loaded with generator "
                    + (w.getGenerator() == null ? "vanilla" : w.getGenerator().getClass().getName()) + "; re-creating it as a void world.");
            for (org.bukkit.entity.Player p : w.getPlayers()) p.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
            if (Bukkit.unloadWorld(w, false)) w = null;
            else plugin.getLogger().severe("Parkour Warrior: could not unload " + WORLD_NAME + "; vanilla terrain may show.");
        }
        if (w == null) {
            File folder = new File(Bukkit.getWorldContainer(), WORLD_NAME);
            if (folder.exists()) deleteFolder(folder.toPath());
            w = McmBuild.voidWorld(WORLD_NAME);
        } else {
            McmBuild.voidWorld(WORLD_NAME);
        }
        world = w;
        if (w != null) {
            w.setGameRule(GameRule.KEEP_INVENTORY, true);
            w.setGameRule(GameRule.FALL_DAMAGE, false);
            w.setGameRule(GameRule.DROWNING_DAMAGE, false);
            w.setGameRule(GameRule.FIRE_DAMAGE, false);
            w.setGameRule(GameRule.FREEZE_DAMAGE, false);
            w.setGameRule(GameRule.NATURAL_REGENERATION, true);
            w.setGameRule(GameRule.DO_TILE_DROPS, false);
            w.setGameRule(GameRule.DO_ENTITY_DROPS, false);
            w.setGameRule(GameRule.DO_MOB_LOOT, false);
            w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            w.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false);
            w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
            w.setPVP(false);
            w.setAutoSave(false);
            w.setDifficulty(org.bukkit.Difficulty.PEACEFUL);
        }
        return w;
    }

    private void deleteFolder(Path root) {
        try (Stream<Path> s = Files.walk(root)) {
            s.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException e) {
            plugin.getLogger().warning("Parkour Warrior: could not delete old world folder: " + e);
        }
    }

    // ------------------------------------------------------------------ generator

    static boolean mapClassPresent(ClassLoader cl) {
        try { Class.forName(MAP_CLASS, false, cl); return true; } catch (ClassNotFoundException e) { return false; }
    }

    /** Generate (pure Java; safe off the main thread). mode: auto | coursemap | stub. */
    static CourseModel generate(String mode, long seed, java.util.logging.Logger log, String[] used) {
        return generate(mode, seed, null, log, used);
    }

    /**
     * importDir: the data folder's {@code imported/} (course sections cut from community maps, loaded at runtime
     * only); null or absent = the built-in course.
     */
    static CourseModel generate(String mode, long seed, java.io.File importDir, java.util.logging.Logger log, String[] used) {
        CourseModel m = null;
        if (!"stub".equals(mode)) {
            try {
                Class<?> c = Class.forName(MAP_CLASS, true, CourseWorld.class.getClassLoader());
                Object o;
                Method g2 = null;
                try { g2 = c.getMethod("generate", long.class, java.io.File.class); } catch (NoSuchMethodException ignored) {}
                if (g2 != null && importDir != null && new java.io.File(importDir, "course.txt").isFile()) o = g2.invoke(null, seed, importDir);
                else o = c.getMethod("generate", long.class).invoke(null, seed);
                if (o instanceof CourseModel cm && cm.notes.containsKey("imported"))
                    log.info("Parkour Warrior: imported sections " + cm.notes.get("imported")
                            + (cm.notes.containsKey("import problems") ? " (problems: " + cm.notes.get("import problems") + ")" : ""));
                if (o instanceof CourseModel cm) { m = cm; used[0] = "CourseMap"; }
                else log.warning("Parkour Warrior: CourseMap.generate returned " + o + "; using the stub course");
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                if ("coursemap".equals(mode)) log.warning("Parkour Warrior: map.generator is 'coursemap' but " + MAP_CLASS + ".generate(long) is missing; using the stub course");
            } catch (InvocationTargetException e) {
                log.log(Level.SEVERE, "Parkour Warrior: CourseMap.generate failed; using the stub course", e.getCause());
            } catch (Throwable t) {
                log.log(Level.SEVERE, "Parkour Warrior: CourseMap could not be used; using the stub course", t);
            }
            if (m != null && (m.obstacles.isEmpty() || (m.spawns.isEmpty() && m.hub(1) == null))) {
                log.severe("Parkour Warrior: CourseMap returned a course without obstacles or spawns; using the stub course");
                m = null;
            }
        }
        if (m == null) { m = StubCourse.generate(seed); used[0] = "stub"; }
        if (m.minX == 0 && m.maxX == 0 && m.minY == 0 && m.maxY == 0) m.recomputeBounds();
        validate(m, log);
        return m;
    }

    /** Log contract problems (missing ids, hubs, finishes); the game still runs with what exists. */
    static void validate(CourseModel m, java.util.logging.Logger log) {
        if (m.linear != null) {      // a replica imported 1:1: one line of sections, no hubs / bonus paths / endings
            if (m.linear.checkpoints.size() + 1 != m.linear.sectionCount() || m.linear.finish == null)
                log.warning("Parkour Warrior: linear course has " + m.linear.checkpoints.size() + " checkpoints for " + m.linear.sectionCount() + " sections (finish " + (m.linear.finish != null) + ")");
            return;
        }
        List<String> missing = new ArrayList<>();
        for (int s = 1; s <= 5; s++) {
            for (int i = 1; i <= 3; i++) {
                if (m.obstacle(CourseModel.mainId(s, i)) == null) missing.add(CourseModel.mainId(s, i));
                if (m.obstacle(CourseModel.bonusId(s, i)) == null) missing.add(CourseModel.bonusId(s, i));
            }
            if (m.hub(s) == null) missing.add("hub " + s);
        }
        for (CourseModel.Difficulty d : CourseModel.Difficulty.values()) {
            for (int i = 1; i <= 2; i++) if (m.obstacle(CourseModel.endingId(d, i)) == null) missing.add(CourseModel.endingId(d, i));
            if (!m.finishes.containsKey(d)) missing.add("finish " + d);
        }
        if (m.fork == null) missing.add("fork");
        if (m.overviews.isEmpty()) missing.add("overviews");
        if (m.spawns.isEmpty()) missing.add("spawns");
        for (Obstacle o : m.obstacles) if (o.entry() == null || o.pad() == null) missing.add(o.id() + " entry/pad");
        if (!missing.isEmpty()) log.warning("Parkour Warrior: course is missing " + missing);
        if (m.timed.size() < 3) log.warning("Parkour Warrior: course has only " + m.timed.size() + " timed block groups (spec wants >= 3 timed obstacles)");
    }

    // ------------------------------------------------------------------ prepare / build

    /**
     * Make sure the course for these settings is generated and placed, then run {@code ready} (main thread).
     * {@code force} regenerates even if the same course is already placed.
     */
    void prepare(Settings s, boolean force, Runnable ready) { prepare(s, CLASSIC, force, ready); }

    static final String CLASSIC = "classic";

    /** Folder holding the private imported maps (one sub-folder with a course.txt per map; never in git or the jar). */
    java.io.File mapsRoot(Settings s) {
        java.io.File f = new java.io.File(s.mapsFolder);
        return f.isAbsolute() ? f : new java.io.File(plugin.getDataFolder(), s.mapsFolder);
    }

    /** Map ids that can be built now: classic plus every maps/<id>/course.txt, in pool order when a pool is set. */
    List<String> availableMaps(Settings s) {
        List<String> found = new ArrayList<>();
        found.add(CLASSIC);
        java.io.File[] dirs = mapsRoot(s).listFiles(java.io.File::isDirectory);
        if (dirs != null) {
            java.util.Arrays.sort(dirs);
            for (java.io.File d : dirs) if (new java.io.File(d, "course.txt").isFile()) found.add(d.getName().toLowerCase(java.util.Locale.ROOT));
        }
        if (s.mapPool.isEmpty()) return found;
        List<String> out = new ArrayList<>();
        for (String id : s.mapPool) if (found.contains(id) && !out.contains(id)) out.add(id);
        return out.isEmpty() ? List.of(CLASSIC) : out;
    }

    /** Built map id (classic, mcc6, ...). */
    String mapId() { return builtMap; }

    private String builtMap = CLASSIC, pendingMap = CLASSIC;
    /** The Map Edit overlay applied to the placed course (its cells are wiped when the course is replaced). */
    private com.mcm.api.McmMapOverlay placedOverlay;
    private MoverRuntime movers;

    MoverRuntime movers() { return movers; }

    void prepare(Settings s, String mapId, boolean force, Runnable ready) {
        if (world == null) ensureWorld();
        if (world == null) { plugin.getLogger().severe("Parkour Warrior: world missing"); state = State.FAILED; return; }
        final String map = mapId == null ? CLASSIC : mapId.toLowerCase(java.util.Locale.ROOT);
        final java.io.File importDir = CLASSIC.equals(map) ? null : new java.io.File(mapsRoot(s), map);
        String key = s.generator + "|" + s.seed + "|" + s.offX + "," + s.offY + "," + s.offZ + "|" + map;
        if (!force && state == State.READY && key.equals(builtKey)) { if (ready != null) ready.run(); return; }
        if (!force && busy() && key.equals(pendingKey)) { if (ready != null) waiting.add(ready); return; }
        cancelBuild();
        if (ready != null) waiting.add(ready);
        pendingKey = key;
        pendingMap = map;
        state = State.GENERATING;
        final int gen = ++generation;
        final String mode = s.generator;
        final long seed = s.seed;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String[] used = {"?"};
            CourseModel m;
            try {
                m = generate(mode, seed, importDir, plugin.getLogger(), used);
                if (m != null && importDir != null && !map.equals(m.mapId)) {
                    plugin.getLogger().warning("Parkour Warrior: map '" + map + "' could not be loaded from " + importDir + " (no usable course.txt); built the classic course");
                }
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Parkour Warrior: course generation crashed", t);
                m = null;
            }
            final CourseModel fm = m;
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (gen != generation) return;
                if (fm == null) { state = State.FAILED; waiting.clear(); return; }
                generatorUsed = used[0];
                builtMap = importDir != null && map.equals(fm.mapId) ? map : CLASSIC;
                place(fm, s, key, gen);
            });
        });
    }

    private void place(CourseModel m, Settings s, String key, int gen) {
        state = State.BUILDING;
        World w = world;
        queue = new McmBuild.BuildQueue(plugin, s.blocksPerTick);
        McmBuild.BuildQueue q = queue;
        removeLabels();
        if (movers != null) { movers.stop(); movers = null; }
        // wipe the previously placed course
        if (model != null) {
            final CourseModel old = model;
            final int ox = offX, oy = offY, oz = offZ;
            BlockData air = Material.AIR.createBlockData();
            old.blocks.forEach((x, y, z, b) -> q.set(w, x + ox, y + oy, z + oz, air));
            for (CourseModel.Timed t : old.timed) for (CourseModel.BlockPos p : t.positions()) q.set(w, p.x() + ox, p.y() + oy, p.z() + oz, air);
            PwOverlays.wipe(q, w, placedOverlay, ox, oy, oz);   // blocks the previous course had added with a Map Edit overlay
        }
        model = m;
        offX = s.offX; offY = s.offY; offZ = s.offZ;
        m.blocks.forEach((x, y, z, b) -> q.set(w, x + offX, y + offY, z + offZ, data(b)));
        timedState = new int[m.timed.size()];
        for (int i = 0; i < m.timed.size(); i++) {
            CourseModel.Timed t = m.timed.get(i);
            timedState[i] = -1;
            for (CourseModel.BlockPos p : t.positions()) q.set(w, p.x() + offX, p.y() + offY, p.z() + offZ, data(t.blockState()));
        }
        placedOverlay = PwOverlays.place(plugin, q, w, builtMap, offX, offY, offZ, this::data);   // Map Edit overlay (core /mcm mapedit) on top
        long total = q.total();
        q.then(() -> {
            if (gen != generation) return;
            state = State.READY;
            builtKey = key;
            if (movers != null) movers.stop();
            movers = new MoverRuntime(world, m, offX, offY, offZ, this::data);
            applySigns(m, world, offX, offY, offZ);
            plugin.getLogger().info("Parkour Warrior: course placed (" + generatorUsed + ", seed " + m.seed + ", " + total + " block ops, "
                    + m.obstacles.size() + " obstacles, " + m.timed.size() + " timed groups)");
            List<Runnable> r = new ArrayList<>(waiting);
            waiting.clear();
            for (Runnable x : r) {
                try { x.run(); } catch (Throwable t) { plugin.getLogger().log(Level.SEVERE, "Parkour Warrior: after-build step failed", t); }
            }
        });
        q.start();
    }

    /** Write the text of a linear course's signs (the block strings carry no text); front side, waxed. */
    void applySigns(CourseModel m, World w, int ox, int oy, int oz) {
        if (m == null || m.linear == null || w == null) return;
        int n = 0;
        for (CourseModel.SignText st : m.linear.signs) {
            try {
                org.bukkit.block.BlockState bs = w.getBlockAt(st.x() + ox, st.y() + oy, st.z() + oz).getState();
                if (!(bs instanceof org.bukkit.block.Sign sign)) continue;
                org.bukkit.block.sign.SignSide side = sign.getSide(org.bukkit.block.sign.Side.FRONT);
                for (int i = 0; i < 4 && i < st.lines().size(); i++) side.line(i, net.kyori.adventure.text.Component.text(st.lines().get(i)));
                sign.setWaxed(true);
                sign.update(true, false);
                n++;
            } catch (Throwable t) {
                plugin.getLogger().warning("Parkour Warrior: sign at " + st.x() + "," + st.y() + "," + st.z() + ": " + t);
            }
        }
        if (n > 0) plugin.getLogger().info("Parkour Warrior: wrote the text of " + n + " signs");
    }

    private void cancelBuild() {
        generation++;
        if (queue != null) { queue.cancel(); queue = null; }
        if (state == State.GENERATING || state == State.BUILDING) state = model != null ? State.FAILED : State.NONE;
        waiting.clear();
    }

    /** Stop any generation/build (plugin disable). */
    void shutdown() {
        cancelBuild();
        removeLabels();
        releaseChunks();
    }

    BlockData data(String s) {
        if (s == null) return Material.AIR.createBlockData();
        BlockData d = cache.get(s);
        if (d != null) return d;
        try {
            d = Bukkit.createBlockData(s);
        } catch (IllegalArgumentException e) {
            int i = s.indexOf('[');
            try { d = Bukkit.createBlockData(i > 0 ? s.substring(0, i) : s); }
            catch (IllegalArgumentException e2) { d = Material.STONE.createBlockData(); }
            plugin.getLogger().warning("Parkour Warrior: bad block state '" + s + "', using " + d.getAsString());
        }
        cache.put(s, d);
        return d;
    }

    // ------------------------------------------------------------------ coordinates

    Location loc(Pos p) { return loc(p, 0f); }

    Location loc(Pos p, float pitch) {
        return new Location(world, p.x() + offX, p.y() + offY, p.z() + offZ, p.yaw(), pitch);
    }

    double lx(Location l) { return l.getX() - offX; }
    double ly(Location l) { return l.getY() - offY; }
    double lz(Location l) { return l.getZ() - offZ; }

    Location overview(int i) {
        if (model == null || model.overviews.isEmpty()) {
            Location l = model == null ? new Location(world, 0.5, 120, 0.5) : loc(new Pos((model.minX + model.maxX) / 2.0, model.maxY + 30, (model.minZ + model.maxZ) / 2.0, 0));
            l.setPitch(60);
            return l;
        }
        CourseModel.Overview o = model.overviews.get(Math.floorMod(i, model.overviews.size()));
        return loc(o.pos(), o.pitch());
    }

    /** True if the location is within the course bounds plus a margin (spectator confinement). */
    boolean inBounds(Location l, int margin) {
        if (model == null || l.getWorld() != world) return false;
        double x = lx(l), y = ly(l), z = lz(l);
        return x >= model.minX - margin && x <= model.maxX + margin && z >= model.minZ - margin && z <= model.maxZ + margin
                && y >= model.minY - margin && y <= model.maxY + margin + 40;
    }

    // ------------------------------------------------------------------ game-time services

    /** Keep every course chunk loaded (labels, timed blocks) for the game. */
    void holdChunks() {
        if (model == null || world == null) return;
        int cx0 = (model.minX + offX) >> 4, cx1 = (model.maxX + offX) >> 4;
        int cz0 = (model.minZ + offZ) >> 4, cz1 = (model.maxZ + offZ) >> 4;
        long count = (long) (cx1 - cx0 + 1) * (cz1 - cz0 + 1);
        if (count > 4096) { plugin.getLogger().warning("Parkour Warrior: course spans " + count + " chunks; not force-loading"); return; }
        for (int cx = cx0; cx <= cx1; cx++) for (int cz = cz0; cz <= cz1; cz++) {
            world.addPluginChunkTicket(cx, cz, plugin);
            tickets.add(((long) cx << 32) ^ (cz & 0xFFFFFFFFL));
        }
    }

    void releaseChunks() {
        if (world != null) world.removePluginChunkTickets(plugin);
        tickets.clear();
    }

    void spawnLabels() {
        removeLabels();
        if (model == null || world == null) return;
        final Settings lblSettings = Settings.load(plugin.getConfig(), plugin.getLogger());
        for (Obstacle o : model.obstacles) {
            if (o.label() == null) continue;
            String color = switch (o.kind()) {
                case MAIN -> "§f";
                case BONUS -> o.index() == 1 ? "§6" : o.index() == 2 ? "§7" : "§e";
                case ENDING -> o.ending() == CourseModel.Difficulty.EASY ? "§a" : o.ending() == CourseModel.Difficulty.MEDIUM ? "§6" : "§c";
            };
            String sub = switch (o.kind()) {
                case MAIN -> "§6+" + Scoring.coins(lblSettings, o) + " coins";
                case BONUS -> "§6+" + Scoring.coins(lblSettings, o) + " coins §8(bonus " + o.index() + "/3)";
                case ENDING -> "§8" + cap(o.ending().name()) + " ending " + o.index() + "/2";
            };
            String code = o.kind() == CourseModel.Kind.ENDING ? o.ending().name().charAt(0) + "-" + o.index() : o.id();
            label(o.label(), "§7[" + code + "] " + color + "§l" + o.name() + "\n" + sub);
        }
        for (CourseModel.Label l : model.labels) label(l.pos(), ChatColor.translateAlternateColorCodes('&', l.text()));
    }

    private void label(Pos p, String text) {
        Location l = loc(p);
        try {
            world.spawn(l, TextDisplay.class, td -> {
                td.setText(text);
                td.setBillboard(Display.Billboard.CENTER);
                td.setShadowed(true);
                td.setSeeThrough(false);
                td.setDefaultBackground(false);
                td.setBackgroundColor(Color.fromARGB(96, 0, 0, 0));
                td.setPersistent(false);
                td.setViewRange(0.6f);
                td.addScoreboardTag(TAG);
            });
        } catch (Throwable t) {
            plugin.getLogger().warning("Parkour Warrior: could not spawn label at " + p + ": " + t);
        }
    }

    void removeLabels() {
        if (world == null) return;
        for (Entity e : new ArrayList<>(world.getEntities())) if (e.getScoreboardTags().contains(TAG)) e.remove();
    }

    /** Toggle timed blocks for game tick {@code tick}; particles during the warning window. */
    void updateTimed(long tick) {
        if (model == null || world == null) return;
        Particle.DustOptions dust = new Particle.DustOptions(Color.fromRGB(0x5FD8FF), 1.2f);
        for (int i = 0; i < model.timed.size() && i < timedState.length; i++) {
            CourseModel.Timed t = model.timed.get(i);
            int st = t.onAt(tick) ? 2 : t.warnAt(tick) ? 1 : 0;
            if (st != timedState[i]) {
                timedState[i] = st;
                String b = st == 2 ? t.blockState() : st == 1 && t.warnState() != null ? t.warnState() : t.offState();
                BlockData d = data(b);
                for (CourseModel.BlockPos p : t.positions()) world.getBlockAt(p.x() + offX, p.y() + offY, p.z() + offZ).setBlockData(d, false);
            }
            if (st == 1 && tick % 3 == 0) {
                for (CourseModel.BlockPos p : t.positions()) {
                    world.spawnParticle(Particle.DUST, p.x() + offX + 0.5, p.y() + offY + 0.5, p.z() + offZ + 0.5, 4, 0.3, 0.3, 0.3, 0, dust);
                }
            }
        }
    }

    /** Put every timed block in its "on" state (between games, so admins can see the course). */
    void resetTimed() {
        if (model == null || world == null) return;
        for (int i = 0; i < model.timed.size(); i++) {
            CourseModel.Timed t = model.timed.get(i);
            BlockData d = data(t.blockState());
            for (CourseModel.BlockPos p : t.positions()) world.getBlockAt(p.x() + offX, p.y() + offY, p.z() + offZ).setBlockData(d, false);
            if (i < timedState.length) timedState[i] = -1;
        }
    }

    int mainCount() { return model == null ? 15 : model.ofKind(Kind.MAIN).size(); }

    static String cap(String s) { return s.isEmpty() ? s : s.charAt(0) + s.substring(1).toLowerCase(java.util.Locale.ROOT); }
}
