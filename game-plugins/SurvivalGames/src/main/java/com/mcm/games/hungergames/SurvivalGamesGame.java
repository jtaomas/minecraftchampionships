package com.mcm.games.hungergames;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.api.Waits;
import com.mcm.games.hungergames.map.SgCanvas;
import com.mcm.games.hungergames.map.SgMap;
import com.mcm.games.hungergames.map.SgMapRegistry;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Modern 1.21 MCC-style Survival Games / Hunger Games (id: hungergames).
 */
public final class SurvivalGamesGame implements McmGame, Listener {
    private enum Phase { IDLE, BUILDING, COUNTDOWN, GRACE, ACTIVE, DEATHMATCH }

    private record ImportedChest(int x, int y, int z, LootTable.Tier tier) {}

    private static final int INFINITE_XP_LEVEL = 30;
    private static final int IMPORTED_CHEST_SCAN_CAP = 140;
    private static final int CHUNKS_SCANNED_PER_TICK = 24;
    private static final Set<String> SKIPPED_WORLD_ENTRIES = Set.of(
            "uid.dat", "session.lock", "map.yml", "level.dat_old", "playerdata", "stats", "entities"
    );
    private static final Set<Material> PLACEABLE_BLOCKS = Set.of(
            Material.OAK_PLANKS, Material.COBBLESTONE, Material.COBWEB, Material.FIRE
    );

    private final SurvivalGamesPlugin plugin;
    private final ArenaBuilder arenaBuilder = new ArenaBuilder();
    private final Object importedIoLock = new Object();

    private volatile boolean running;
    private volatile Phase phase = Phase.IDLE;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private int buildGeneration;

    private McmGameContext ctx;
    private World world;
    private World importedRuntimeWorld;
    private SgMap activeMap;
    private ImportedSgWorld activeImported;
    private String forcedMap;
    private BukkitTask mainTask;
    private BukkitTask importedScanTask;

    private final List<Location> importedPedestals = new ArrayList<>();
    private final List<ImportedChest> importedChests = new ArrayList<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Map<UUID, String> sideOfPlayer = new HashMap<>();
    private final List<String> eliminationOrder = new ArrayList<>();
    private final Set<Location> activeAirdrops = new HashSet<>();
    private final Set<Location> playerPlacedBlocks = new HashSet<>();

    private Location pendingAirdropLoc;
    private int pendingAirdropCountdown;

    private int phaseSecondsLeft;
    private int gameSecondsLeft;
    private int gameTotalSeconds;
    private boolean chestsRefilled;
    private boolean deathmatchStarted;

    public SurvivalGamesGame(SurvivalGamesPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String id() {
        return "hungergames";
    }

    @Override
    public String displayName() {
        return "Hunger Games (SG)";
    }

    @Override
    public Material icon() {
        return Material.IRON_SWORD;
    }

    @Override
    public String description() {
        return "Modern MCC Survival Games with enchanting, shrinking border, airdrops, and Cornucopia deathmatch.";
    }

    @Override
    public List<String> maps() {
        List<String> all = new ArrayList<>(plugin.getImportedWorlds().keySet());
        all.addAll(SgMapRegistry.ids());
        return all;
    }

    @Override
    public boolean forceNextMap(String id) {
        if (id == null) return false;
        String lower = id.toLowerCase(Locale.ROOT);
        if (!maps().contains(lower)) return false;
        forcedMap = lower;
        return true;
    }

    @Override
    public String currentMap() {
        return running ? activeMapId() : null;
    }

    private String activeMapId() {
        if (activeImported != null) return activeImported.id();
        return activeMap != null ? activeMap.id() : null;
    }

    private String activeMapDisplayName() {
        if (activeImported != null) return activeImported.displayName();
        return activeMap != null ? activeMap.displayName() : "-";
    }

    private int activeMapRadius() {
        if (activeImported != null) return activeImported.radius();
        return activeMap != null ? activeMap.radius() : ArenaBuilder.DEFAULT_RADIUS;
    }

    private boolean activeBoatRecommended() {
        if (activeImported != null) return activeImported.boatRecommended();
        return activeMap != null && activeMap.boatRecommended();
    }

    private int activeCenterX() {
        return activeImported != null ? activeImported.centerX() : 0;
    }

    private int activeCenterY() {
        return activeImported != null ? activeImported.centerY() : ArenaBuilder.BASE_Y;
    }

    private int activeCenterZ() {
        return activeImported != null ? activeImported.centerZ() : 0;
    }

    @Override
    public boolean skipWait() {
        if (!running) return false;
        if (phase == Phase.COUNTDOWN) {
            phaseSecondsLeft = 0;
            startGraceOrActive();
            return true;
        }
        if (phase == Phase.GRACE) {
            phaseSecondsLeft = 0;
            startActivePhase();
            return true;
        }
        return false;
    }

    @Override
    public boolean skipRound() {
        return false;
    }

    @Override
    public void start(McmGameContext ctx) {
        stop();
        final int gen = ++buildGeneration;
        this.ctx = ctx;
        this.running = true;
        this.finished.set(false);
        this.phase = Phase.BUILDING;
        this.alive.clear();
        this.sideOfPlayer.clear();
        this.eliminationOrder.clear();
        this.activeAirdrops.clear();
        this.playerPlacedBlocks.clear();
        this.importedPedestals.clear();
        this.importedChests.clear();
        this.pendingAirdropLoc = null;
        this.pendingAirdropCountdown = 0;
        this.chestsRefilled = false;
        this.deathmatchStarted = false;
        this.activeImported = null;
        this.activeMap = null;

        Map<String, ImportedSgWorld> imported = plugin.getImportedWorlds();
        if (forcedMap != null) {
            if (imported.containsKey(forcedMap)) {
                this.activeImported = imported.get(forcedMap);
            } else {
                this.activeMap = SgMapRegistry.byId(forcedMap);
            }
            this.forcedMap = null;
        } else if (plugin.getConfig().getBoolean("prefer-imported-maps", true) && !imported.isEmpty()) {
            List<ImportedSgWorld> list = new ArrayList<>(imported.values());
            this.activeImported = list.get(ThreadLocalRandom.current().nextInt(list.size()));
        } else {
            List<SgMap> allMaps = SgMapRegistry.all();
            this.activeMap = allMaps.get(ThreadLocalRandom.current().nextInt(allMaps.size()));
        }

        for (Map.Entry<String, List<Player>> entry : ctx.sides().entrySet()) {
            for (Player p : entry.getValue()) {
                alive.add(p.getUniqueId());
                sideOfPlayer.put(p.getUniqueId(), entry.getKey());
            }
        }

        if (activeImported != null) {
            ctx.broadcast(ChatColor.GOLD + "Loading imported map " + ChatColor.YELLOW + activeImported.displayName()
                    + ChatColor.GOLD + " (" + activeImported.id() + ")...");
            prepareAndLoadImportedWorld(activeImported, gen);
            return;
        }

        this.world = plugin.getGameWorld();
        WorldBorder border = world.getWorldBorder();
        int initialRadius = plugin.getConfig().getInt("border.initial-radius", activeMap.radius());
        border.setCenter(0.5, 0.5);
        border.setSize(initialRadius * 2.0);
        border.setDamageBuffer(1.0);
        border.setDamageAmount(1.5);
        border.setWarningDistance(8);
        border.setWarningTime(5);

        ctx.broadcast(ChatColor.GOLD + "Generating " + ChatColor.YELLOW + activeMap.displayName()
                + ChatColor.GOLD + " (" + activeMap.id() + ")...");
        arenaBuilder.buildAsync(plugin, world, activeMap.id(), this::onArenaBuilt);
    }

    private void prepareAndLoadImportedWorld(ImportedSgWorld imp, int gen) {
        String runtimeName = "mcc_hg_" + imp.id();
        World existing = Bukkit.getWorld(runtimeName);
        if (existing != null) {
            Location fallback = Bukkit.getWorlds().get(0).getSpawnLocation();
            for (Player p : new ArrayList<>(existing.getPlayers())) {
                p.teleport(fallback);
            }
            if (!Bukkit.unloadWorld(existing, false)) {
                plugin.getLogger().warning("Could not unload existing runtime world '" + runtimeName + "'; aborting load.");
                if (ctx != null) {
                    ctx.broadcast(ChatColor.RED + "Failed to unload previous instance of '" + imp.id() + "'.");
                }
                stop();
                return;
            }
        }

        Path targetDir = plugin.getServer().getWorldContainer().toPath().resolve(runtimeName);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!running || buildGeneration != gen) return;
            try {
                synchronized (importedIoLock) {
                    if (!running || buildGeneration != gen) return;
                    deleteRecursivelyWithRetry(targetDir);
                    copyWorldTemplate(imp.templateDir(), targetDir);
                }
            } catch (IOException ex) {
                plugin.getLogger().severe("Failed to copy imported SG world '" + imp.id() + "': " + ex.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (running && buildGeneration == gen && ctx != null) {
                        ctx.broadcast(ChatColor.RED + "Failed to load imported map '" + imp.id() + "'.");
                        stop();
                    }
                });
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!running || buildGeneration != gen) return;
                if (Bukkit.getWorld(runtimeName) != null) {
                    plugin.getLogger().warning("Runtime world '" + runtimeName + "' already loaded before createWorld().");
                    stop();
                    return;
                }
                World loaded = new WorldCreator(runtimeName).createWorld();
                if (loaded == null) {
                    if (ctx != null) {
                        ctx.broadcast(ChatColor.RED + "WorldCreator returned null for '" + runtimeName + "'.");
                    }
                    stop();
                    return;
                }
                this.world = loaded;
                this.importedRuntimeWorld = loaded;
                plugin.configureWorld(loaded);
                loaded.setAutoSave(false);

                WorldBorder border = loaded.getWorldBorder();
                border.setCenter(imp.centerX() + 0.5, imp.centerZ() + 0.5);
                border.setSize(imp.radius() * 2.0);
                border.setDamageBuffer(1.0);
                border.setDamageAmount(1.5);
                border.setWarningDistance(8);
                border.setWarningTime(5);

                scanImportedChestsAndPedestals(imp, loaded, gen);
            });
        });
    }

    private void scanImportedChestsAndPedestals(ImportedSgWorld imp, World loaded, int gen) {
        int scanR = Math.min(imp.radius(), IMPORTED_CHEST_SCAN_CAP);
        int minCx = (imp.centerX() - scanR) >> 4;
        int maxCx = (imp.centerX() + scanR) >> 4;
        int minCz = (imp.centerZ() - scanR) >> 4;
        int maxCz = (imp.centerZ() + scanR) >> 4;

        List<int[]> chunks = new ArrayList<>();
        double maxDist = scanR + 16.0;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                double chunkCenterX = (cx << 4) + 8.0;
                double chunkCenterZ = (cz << 4) + 8.0;
                if (Math.hypot(chunkCenterX - imp.centerX(), chunkCenterZ - imp.centerZ()) <= maxDist) {
                    chunks.add(new int[]{cx, cz});
                }
            }
        }
        // Scan Cornucopia (center) chunks first so central chests are guaranteed to be discovered & filled first.
        chunks.sort(Comparator.comparingDouble(c -> {
            double dx = ((c[0] << 4) + 8.0) - imp.centerX();
            double dz = ((c[1] << 4) + 8.0) - imp.centerZ();
            return dx * dx + dz * dz;
        }));

        final int[] idx = {0};
        if (importedScanTask != null) importedScanTask.cancel();
        importedScanTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!running || buildGeneration != gen) {
                if (importedScanTask != null) {
                    importedScanTask.cancel();
                    importedScanTask = null;
                }
                return;
            }
            int end = Math.min(idx[0] + CHUNKS_SCANNED_PER_TICK, chunks.size());
            for (int i = idx[0]; i < end; i++) {
                int[] coord = chunks.get(i);
                Chunk chunk = loaded.getChunkAt(coord[0], coord[1]);
                for (Entity ent : chunk.getEntities()) {
                    purgeUnsafeLegacyEntity(ent);
                }
                for (BlockState state : chunk.getTileEntities()) {
                    if (state instanceof Chest chest) {
                        double dist = Math.hypot(chest.getX() - imp.centerX(), chest.getZ() - imp.centerZ());
                        LootTable.Tier tier = dist <= 20.0 ? LootTable.Tier.CORNUCOPIA : LootTable.Tier.OUTER;
                        importedChests.add(new ImportedChest(chest.getX(), chest.getY(), chest.getZ(), tier));
                        LootTable.fill(chest.getBlockInventory(), tier);
                    }
                }
            }
            idx[0] = end;
            if (idx[0] >= chunks.size()) {
                importedScanTask.cancel();
                importedScanTask = null;
                findImportedPedestals(imp, loaded);
                plugin.getLogger().info("Prepared imported SG map '" + imp.id() + "': "
                        + importedChests.size() + " chests, " + importedPedestals.size() + " pedestals.");
                onArenaBuilt();
            }
        }, 1L, 1L);
    }

    private void findImportedPedestals(ImportedSgWorld imp, World w) {
        importedPedestals.clear();
        int[] radii = {14, 15, 13, 16, 12, 17, 11, 10, 9, 8, 7, 18, 19, 6, 5, 20, 21, 22, 4};
        int[] searchRadii = {8, 7, 10, 12, 14, 6, 16, 5, 9, 11, 13, 15, 18};
        int minY = Math.max(w.getMinHeight() + 1, imp.centerY() - 16);
        int maxY = Math.min(w.getMaxHeight() - 3, imp.centerY() + 20);
        Set<Long> usedXZ = new HashSet<>();

        for (int i = 0; i < 24; i++) {
            double angle = (2.0 * Math.PI * i) / 24.0;
            Location found = null;
            int chosenX = 0, chosenZ = 0;

            for (int r : radii) {
                int px = imp.centerX() + (int) Math.round(Math.cos(angle) * r);
                int pz = imp.centerZ() + (int) Math.round(Math.sin(angle) * r);
                if (usedXZ.contains(packXZ(px, pz))) continue;
                found = findSafePedestalY(w, px, pz, imp.centerY(), minY, maxY, imp.centerX(), imp.centerZ());
                if (found != null) { chosenX = px; chosenZ = pz; break; }
            }
            if (found == null) {
                outer:
                for (int r : searchRadii) {
                    for (int deg = 4; deg <= 44; deg += 4) {
                        for (int sgn = -1; sgn <= 1; sgn += 2) {
                            double a = angle + Math.toRadians(deg * sgn);
                            int px = imp.centerX() + (int) Math.round(Math.cos(a) * r);
                            int pz = imp.centerZ() + (int) Math.round(Math.sin(a) * r);
                            if (usedXZ.contains(packXZ(px, pz))) continue;
                            found = findSafePedestalY(w, px, pz, imp.centerY(), minY, maxY, imp.centerX(), imp.centerZ());
                            if (found != null) { chosenX = px; chosenZ = pz; break outer; }
                        }
                    }
                }
            }
            if (found == null) {
                spiral:
                for (int dist = 1; dist <= 22; dist++) {
                    for (int dx = -dist; dx <= dist; dx++) {
                        for (int dz = -dist; dz <= dist; dz++) {
                            if (Math.abs(dx) != dist && Math.abs(dz) != dist) continue;
                            int px = imp.centerX() + dx;
                            int pz = imp.centerZ() + dz;
                            if (usedXZ.contains(packXZ(px, pz))) continue;
                            found = findSafePedestalY(w, px, pz, imp.centerY(), minY, maxY, imp.centerX(), imp.centerZ());
                            if (found != null) { chosenX = px; chosenZ = pz; break spiral; }
                        }
                    }
                }
            }
            if (found == null) {
                found = findSafePedestalY(w, imp.centerX(), imp.centerZ(), imp.centerY(), minY, maxY, imp.centerX(), imp.centerZ());
                chosenX = imp.centerX();
                chosenZ = imp.centerZ();
            }
            if (found == null) {
                found = new Location(w, imp.centerX() + 0.5, imp.centerY() + 1.0, imp.centerZ() + 0.5);
                chosenX = imp.centerX();
                chosenZ = imp.centerZ();
            }
            usedXZ.add(packXZ(chosenX, chosenZ));
            importedPedestals.add(found);
        }
    }

    private static long packXZ(int x, int z) {
        return (((long) x) << 32) | (z & 0xFFFFFFFFL);
    }

    private static Location findSafePedestalY(World w, int px, int pz, int targetY, int minY, int maxY, int cx, int cz) {
        int maxOffset = Math.max(targetY - minY, maxY - targetY);
        for (int offset = 0; offset <= maxOffset; offset++) {
            for (int sign = 0; sign < (offset == 0 ? 1 : 2); sign++) {
                int y = sign == 0 ? targetY + offset : targetY - offset;
                if (y < minY || y > maxY) continue;
                Block ground = w.getBlockAt(px, y, pz);
                Block above1 = w.getBlockAt(px, y + 1, pz);
                Block above2 = w.getBlockAt(px, y + 2, pz);
                if (isSafeSpawnGround(ground.getType()) && isPassableDry(above1) && isPassableDry(above2)) {
                    float yaw = (float) Math.toDegrees(Math.atan2(-(cx - px), cz - pz));
                    return new Location(w, px + 0.5, y + 1.0, pz + 0.5, yaw, 0f);
                }
            }
        }
        return null;
    }

    private static boolean isSafeSpawnGround(Material m) {
        String nm = m.name();
        return m.isSolid()
                && !nm.endsWith("_LEAVES")
                && !nm.endsWith("_FENCE")
                && !nm.endsWith("_FENCE_GATE")
                && !nm.endsWith("_WALL")
                && m != Material.BARRIER
                && m != Material.CHEST
                && m != Material.TRAPPED_CHEST
                && m != Material.BARREL
                && m != Material.ENDER_CHEST
                && m != Material.CRAFTING_TABLE
                && m != Material.ENCHANTING_TABLE
                && m != Material.CACTUS
                && m != Material.MAGMA_BLOCK
                && m != Material.CAMPFIRE
                && m != Material.SOUL_CAMPFIRE
                && m != Material.POWDER_SNOW
                && m != Material.POINTED_DRIPSTONE;
    }

    private static boolean isPassableDry(Block b) {
        Material m = b.getType();
        return b.isPassable()
                && !b.isLiquid()
                && m != Material.FIRE
                && m != Material.SOUL_FIRE
                && m != Material.SWEET_BERRY_BUSH
                && m != Material.COBWEB;
    }

    private static void deleteRecursivelyWithRetry(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        IOException last = null;
        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                deleteRecursively(dir);
                return;
            } catch (IOException ex) {
                last = ex;
                try {
                    Thread.sleep(80L);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        if (last != null) throw last;
    }

    private static void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> walk = Files.walk(dir)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }

    private static void copyWorldTemplate(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(dir);
                if (!rel.toString().isEmpty() && SKIPPED_WORLD_ENTRIES.contains(rel.getName(0).toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(target.resolve(rel));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path rel = source.relativize(file);
                if (SKIPPED_WORLD_ENTRIES.contains(rel.getFileName().toString())) {
                    return FileVisitResult.CONTINUE;
                }
                Files.copy(file, target.resolve(rel), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private List<Location> activePedestals() {
        return activeImported != null ? importedPedestals : arenaBuilder.getPedestals();
    }

    private void onArenaBuilt() {
        if (!running || ctx == null) return;

        List<Location> pedestals = activePedestals();
        List<Player> players = new ArrayList<>(ctx.participants());
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Location spawn = pedestals.get(i % pedestals.size()).clone();
            if (i >= pedestals.size()) {
                double offsetAngle = (2.0 * Math.PI * (i / pedestals.size())) / 4.0;
                spawn.add(Math.cos(offsetAngle) * 0.35, 0.0, Math.sin(offsetAngle) * 0.35);
            }
            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setSaturation(5f);
            p.setLevel(INFINITE_XP_LEVEL);
            p.setExp(0f);
            p.getInventory().clear();
            giveStarterKit(p);
        }

        gameTotalSeconds = plugin.getConfig().getInt("timings.game-seconds", 300);
        gameSecondsLeft = gameTotalSeconds;

        int countdown = Waits.seconds(plugin.getConfig().getInt("timings.countdown-seconds", 10));
        if (countdown <= 0) {
            startGraceOrActive();
            return;
        }

        phase = Phase.COUNTDOWN;
        phaseSecondsLeft = countdown;
        startTickLoop();
    }

    private void giveStarterKit(Player p) {
        ItemStack compass = new ItemStack(Material.COMPASS, 1);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Player Tracker");
            compass.setItemMeta(meta);
        }
        p.getInventory().addItem(compass);
        if (activeBoatRecommended()) {
            p.getInventory().addItem(new ItemStack(Material.OAK_BOAT, 1));
        }
        p.getInventory().addItem(new ItemStack(Material.COOKED_BEEF, 4));

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        if (boots.getItemMeta() instanceof LeatherArmorMeta lMeta) {
            lMeta.setColor(teamColorToBukkit(ctx.teamColor(ctx.teamOf(p.getUniqueId()))));
            boots.setItemMeta(lMeta);
        }
        p.getInventory().setBoots(boots);
    }

    private Color teamColorToBukkit(ChatColor cc) {
        if (cc == null) return Color.WHITE;
        return switch (cc) {
            case RED, DARK_RED -> Color.RED;
            case GOLD -> Color.ORANGE;
            case YELLOW -> Color.YELLOW;
            case GREEN -> Color.LIME;
            case DARK_GREEN -> Color.GREEN;
            case DARK_AQUA -> Color.TEAL;
            case AQUA -> Color.AQUA;
            case BLUE, DARK_BLUE -> Color.BLUE;
            case DARK_PURPLE -> Color.PURPLE;
            case LIGHT_PURPLE -> Color.FUCHSIA;
            default -> Color.WHITE;
        };
    }

    private void startGraceOrActive() {
        int grace = Waits.seconds(plugin.getConfig().getInt("timings.grace-seconds", 15));
        if (grace <= 0) {
            startActivePhase();
            return;
        }
        phase = Phase.GRACE;
        phaseSecondsLeft = grace;
        ctx.broadcast(ChatColor.GREEN + "GO! " + ChatColor.YELLOW + "Grace period active for " + grace + "s! Enchanting tables give infinite Lv.30!");
        for (Player p : ctx.participants()) {
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
        }
        if (mainTask == null) {
            startTickLoop();
        }
    }

    private void startActivePhase() {
        phase = Phase.ACTIVE;
        ctx.broadcast(ChatColor.RED + "Grace period has ended! PvP is now ENABLED!");
        for (Player p : ctx.participants()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.0f);
        }

        WorldBorder border = world.getWorldBorder();
        double finalDiameter = plugin.getConfig().getInt("border.final-radius", 16) * 2.0;
        border.setSize(finalDiameter, Math.max(30, gameSecondsLeft - 20));

        if (mainTask == null) {
            startTickLoop();
        }
    }

    private void startTickLoop() {
        if (mainTask != null) mainTask.cancel();
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSecond, 20L, 20L);
    }

    private void tickSecond() {
        if (!running || ctx == null) return;

        if (phase == Phase.COUNTDOWN) {
            ctx.setTimer(phaseSecondsLeft, Math.max(1, phaseSecondsLeft));
            for (Player p : ctx.participants()) {
                p.sendTitle(ChatColor.GOLD + "" + phaseSecondsLeft, ChatColor.YELLOW + "Prepare for Survival Games!", 0, 22, 5);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, 1.0f);
            }
            if (--phaseSecondsLeft <= 0) {
                startGraceOrActive();
            }
            updateSidebar();
            return;
        }

        if (phase == Phase.GRACE) {
            ctx.setTimer(gameSecondsLeft, gameTotalSeconds);
            if (--phaseSecondsLeft <= 0) {
                startActivePhase();
            }
            updateCompassAndActionBar();
            updateSidebar();
            return;
        }

        // ACTIVE or DEATHMATCH
        gameSecondsLeft--;
        ctx.setTimer(Math.max(0, gameSecondsLeft), gameTotalSeconds);

        int elapsed = gameTotalSeconds - gameSecondsLeft;
        int refillAt = plugin.getConfig().getInt("timings.refill-at-elapsed-seconds", 135);
        if (!chestsRefilled && elapsed >= refillAt) {
            chestsRefilled = true;
            refillActiveChests();
            ctx.broadcast(ChatColor.AQUA + "★ ALL CHESTS REFILLED WITH UPGRADED LOOT! ★");
        }

        int airdropInterval = plugin.getConfig().getInt("timings.airdrop-interval-seconds", 65);
        if (elapsed > 25 && elapsed % airdropInterval == 0 && phase == Phase.ACTIVE && pendingAirdropLoc == null) {
            announceAirdrop();
        }

        tickPendingAirdrop();
        tickActiveAirdropBeacons();

        int dmAt = plugin.getConfig().getInt("timings.deathmatch-at-remaining-seconds", 60);
        if (!deathmatchStarted && gameSecondsLeft <= dmAt) {
            startDeathmatch();
        }

        updateCompassAndActionBar();
        updateSidebar();

        if (gameSecondsLeft <= 0) {
            endGame("Time's up!");
        }
    }

    private void refillActiveChests() {
        if (activeImported == null) {
            arenaBuilder.refillChests(world);
            return;
        }
        for (ImportedChest entry : importedChests) {
            Block b = world.getBlockAt(entry.x(), entry.y(), entry.z());
            if (b.getState() instanceof Chest chest) {
                LootTable.fill(chest.getBlockInventory(), entry.tier());
            }
        }
    }

    private int findSafeTopY(int x, int z) {
        if (activeImported == null) {
            return arenaBuilder.findSafeTopY(world, x, z);
        }
        int minY = Math.max(world.getMinHeight() + 1, activeImported.minY());
        int maxY = Math.min(world.getMaxHeight() - 2, activeImported.maxY());
        for (int y = maxY; y >= minY; y--) {
            Material m = world.getBlockAt(x, y, z).getType();
            if (m != Material.AIR && m != Material.WATER && !m.name().endsWith("_LEAVES")) {
                return y;
            }
        }
        return activeImported.centerY();
    }

    private void announceAirdrop() {
        double maxR = Math.max(18.0, (world.getWorldBorder().getSize() / 2.0) - 10.0);
        int cx = activeCenterX();
        int cz = activeCenterZ();
        int x, z;
        if (activeImported == null) {
            List<SgCanvas.AirdropSpot> candidates = new ArrayList<>();
            for (SgCanvas.AirdropSpot spot : arenaBuilder.getAirdrops()) {
                if (Math.hypot(spot.x() - cx, spot.z() - cz) <= maxR) {
                    candidates.add(spot);
                }
            }
            if (!candidates.isEmpty()) {
                SgCanvas.AirdropSpot chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
                x = chosen.x();
                z = chosen.z();
            } else {
                double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
                double dist = 10.0 + ThreadLocalRandom.current().nextDouble() * Math.max(5.0, maxR - 10.0);
                x = cx + (int) Math.round(Math.cos(angle) * dist);
                z = cz + (int) Math.round(Math.sin(angle) * dist);
            }
        } else {
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
            double dist = 10.0 + ThreadLocalRandom.current().nextDouble() * Math.max(5.0, maxR - 10.0);
            x = cx + (int) Math.round(Math.cos(angle) * dist);
            z = cz + (int) Math.round(Math.sin(angle) * dist);
        }

        int topY = findSafeTopY(x, z);
        pendingAirdropLoc = new Location(world, x, topY + 1, z);
        pendingAirdropCountdown = 15;

        ctx.broadcast(ChatColor.LIGHT_PURPLE + "★ SUPPLY AIRDROP incoming in 15s at "
                + ChatColor.AQUA + x + ", " + (topY + 1) + ", " + z + ChatColor.LIGHT_PURPLE + "!");
    }

    private void tickPendingAirdrop() {
        if (pendingAirdropLoc == null) return;
        world.spawnParticle(Particle.END_ROD, pendingAirdropLoc.clone().add(0.5, 4.0, 0.5), 18, 0.4, 4.0, 0.4, 0.02);
        pendingAirdropCountdown--;
        if (pendingAirdropCountdown <= 0) {
            spawnAirdropCrate(pendingAirdropLoc);
            pendingAirdropLoc = null;
        }
    }

    private void spawnAirdropCrate(Location loc) {
        Block base = world.getBlockAt(loc.getBlockX(), loc.getBlockY() - 1, loc.getBlockZ());
        if (base.getType() == Material.AIR || base.getType() == Material.WATER) {
            base.setType(Material.GOLD_BLOCK, false);
        }
        Block crate = world.getBlockAt(loc);
        crate.setType(Material.CHEST, false);
        if (crate.getState() instanceof Chest chest) {
            LootTable.fill(chest.getBlockInventory(), LootTable.Tier.AIRDROP);
        }
        activeAirdrops.add(crate.getLocation());

        Firework fw = (Firework) world.spawnEntity(loc.clone().add(0.5, 1.5, 0.5), EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(Color.AQUA, Color.YELLOW).with(FireworkEffect.Type.BALL_LARGE).build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);

        ctx.broadcast(ChatColor.LIGHT_PURPLE + "★ SUPPLY AIRDROP LANDED at "
                + ChatColor.AQUA + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
                + ChatColor.LIGHT_PURPLE + "!");
    }

    private void tickActiveAirdropBeacons() {
        for (Location loc : activeAirdrops) {
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc.clone().add(0.5, 1.2, 0.5), 10, 0.3, 1.5, 0.3, 0.05);
        }
    }

    private void startDeathmatch() {
        deathmatchStarted = true;
        phase = Phase.DEATHMATCH;
        ctx.broadcast(ChatColor.DARK_RED + "⚔ CORNUCOPIA DEATHMATCH! " + ChatColor.RED + "Teleporting survivors to center!");
        List<Location> pedestals = activePedestals();
        int idx = 0;
        for (UUID uuid : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                p.teleport(pedestals.get(idx % pedestals.size()).clone());
                p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.7f, 1.0f);
                idx++;
            }
        }
        double dmDiameter = plugin.getConfig().getInt("border.deathmatch-radius", 22) * 2.0;
        WorldBorder border = world.getWorldBorder();
        double targetDiameter = Math.min(border.getSize(), dmDiameter);
        border.setSize(targetDiameter, 20);
    }

    private void updateCompassAndActionBar() {
        for (UUID uuid : alive) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;
            Player nearest = findNearestEnemy(p);
            if (nearest != null) {
                p.setCompassTarget(nearest.getLocation());
                int dist = (int) Math.round(p.getLocation().distance(nearest.getLocation()));
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.AQUA + "Nearest Enemy: " + ChatColor.WHITE + nearest.getName()
                                + ChatColor.GRAY + " (" + dist + "m)"));
            } else {
                p.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.GREEN + "No enemies remaining"));
            }
        }
    }

    private Player findNearestEnemy(Player viewer) {
        String mySide = sideOfPlayer.get(viewer.getUniqueId());
        Player best = null;
        double bestDistSq = Double.MAX_VALUE;
        for (UUID uuid : alive) {
            if (uuid.equals(viewer.getUniqueId())) continue;
            if (mySide != null && mySide.equals(sideOfPlayer.get(uuid))) continue;
            Player other = Bukkit.getPlayer(uuid);
            if (other == null || !other.isOnline() || !other.getWorld().equals(world)) continue;
            double d = viewer.getLocation().distanceSquared(other.getLocation());
            if (d < bestDistSq) {
                bestDistSq = d;
                best = other;
            }
        }
        return best;
    }

    private void updateSidebar() {
        if (ctx == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.YELLOW + "Map: " + ChatColor.WHITE + activeMapDisplayName());
        lines.add(ChatColor.YELLOW + "Phase: " + ChatColor.WHITE + phase.name());
        lines.add(ChatColor.YELLOW + "Alive: " + ChatColor.GREEN + alive.size());
        int borderRadius = world != null ? (int) (world.getWorldBorder().getSize() / 2.0) : activeMapRadius();
        lines.add(ChatColor.YELLOW + "Border: " + ChatColor.AQUA + borderRadius + "m");
        ctx.setSidebarLines(lines);
    }

    private void handleElimination(UUID victimId, Player killer) {
        if (!alive.remove(victimId)) return;

        // MCC coins (base x1.0; the core applies the event multiplier). See game-plugins/COINS.md.
        int killPts = plugin.getConfig().getInt("coins.kill", 65);       // MCC: personal elimination
        int survPts = plugin.getConfig().getInt("coins.outlive", 3);     // MCC: another player eliminated, you alive

        Player victimPlayer = Bukkit.getPlayer(victimId);
        String victimName = victimPlayer != null ? victimPlayer.getName() : "a player";
        if (killer != null && !killer.getUniqueId().equals(victimId) && killPts > 0) {
            ctx.addKillCoins(killer.getUniqueId(), killPts, victimId, victimName, "You eliminated " + victimName);
        }
        if (survPts > 0) for (UUID survivor : alive) {
            ctx.addCoins(survivor, survPts, victimName + " was eliminated");
        }

        String victimSide = sideOfPlayer.get(victimId);
        if (victimSide != null && !isSideAlive(victimSide) && !eliminationOrder.contains(victimSide)) {
            eliminationOrder.add(victimSide);
        }

        checkWinCondition();
    }

    private boolean isSideAlive(String side) {
        for (UUID uuid : alive) {
            if (side.equals(sideOfPlayer.get(uuid))) return true;
        }
        return false;
    }

    private void checkWinCondition() {
        if (!running || finished.get()) return;
        if (ctx.participants().size() <= 1) return; // Solo admin testing

        Set<String> livingSides = new LinkedHashSet<>();
        for (UUID uuid : alive) {
            String side = sideOfPlayer.get(uuid);
            if (side != null) livingSides.add(side);
        }
        if (livingSides.size() <= 1) {
            String winnerSide = livingSides.isEmpty() ? null : livingSides.iterator().next();
            endGame(winnerSide != null ? ctx.sideLabel(winnerSide) + ChatColor.GOLD + " wins Survival Games!" : "All players eliminated!");
        }
    }

    private void awardTeamPlacements() {
        if (ctx.participants().size() <= 1) return;
        List<String> ranked = new ArrayList<>();
        // Living sides tie/rank at top
        Set<String> living = new LinkedHashSet<>();
        for (UUID uuid : alive) {
            String side = sideOfPlayer.get(uuid);
            if (side != null) living.add(side);
        }
        ranked.addAll(living);
        // Eliminated sides in reverse order (last eliminated = highest remaining rank)
        for (int i = eliminationOrder.size() - 1; i >= 0; i--) {
            String s = eliminationOrder.get(i);
            if (!ranked.contains(s)) ranked.add(s);
        }

        // MCC team placement by how long the team survived: 1st 650 ... 10th 330, shared amongst the team
        int[] bonuses = com.mcm.api.McmCoins.toIntArray(plugin.getConfig().getList("coins.placement"),
                new int[]{650, 600, 560, 520, 480, 450, 420, 390, 360, 330});
        for (int i = 0; i < ranked.size(); i++) {
            int pool = com.mcm.api.McmCoins.placementOrLast(bonuses, i + 1);
            if (pool > 0) ctx.addTeamCoins(ranked.get(i), pool, "Team Placement #" + (i + 1));
        }
    }

    private void endGame(String reason) {
        if (!finished.compareAndSet(false, true)) return;
        awardTeamPlacements();
        cleanupWorldState();
        running = false;
        phase = Phase.IDLE;
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        if (importedScanTask != null) {
            importedScanTask.cancel();
            importedScanTask = null;
        }
        arenaBuilder.cancel();
        ctx.finish(reason);
        unloadImportedRuntimeWorld();
    }

    private void cleanupWorldState() {
        if (ctx != null) {
            for (Player p : ctx.participants()) {
                p.setLevel(0);
                p.setExp(0f);
            }
        }
        if (world != null) {
            for (Location loc : playerPlacedBlocks) {
                world.getBlockAt(loc).setType(Material.AIR, false);
            }
            for (Entity e : world.getEntities()) {
                if (!(e instanceof Player)) {
                    e.remove();
                }
            }
            world.getWorldBorder().reset();
        }
        playerPlacedBlocks.clear();
    }

    private void unloadImportedRuntimeWorld() {
        if (importedRuntimeWorld == null) return;
        World toUnload = importedRuntimeWorld;
        String runtimeName = toUnload.getName();
        importedRuntimeWorld = null;
        if (world == toUnload) {
            world = null;
        }
        Location fallback = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (Player p : new ArrayList<>(toUnload.getPlayers())) {
            p.teleport(fallback);
        }
        boolean unloaded = Bukkit.unloadWorld(toUnload, false);
        if (!unloaded) {
            plugin.getLogger().warning("Failed to unload runtime world '" + runtimeName + "'.");
            return;
        }
        Path runtimeDir = plugin.getServer().getWorldContainer().toPath().resolve(runtimeName);
        if (plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                synchronized (importedIoLock) {
                    try {
                        deleteRecursivelyWithRetry(runtimeDir);
                    } catch (IOException ex) {
                        plugin.getLogger().warning("Could not delete runtime world folder '" + runtimeName + "': " + ex.getMessage());
                    }
                }
            });
        }
    }

    @Override
    public void stop() {
        buildGeneration++;
        running = false;
        phase = Phase.IDLE;
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        if (importedScanTask != null) {
            importedScanTask.cancel();
            importedScanTask = null;
        }
        arenaBuilder.cancel();
        alive.clear();
        sideOfPlayer.clear();
        eliminationOrder.clear();
        activeAirdrops.clear();
        importedPedestals.clear();
        importedChests.clear();
        pendingAirdropLoc = null;
        if (world != null) {
            for (Location loc : playerPlacedBlocks) {
                world.getBlockAt(loc).setType(Material.AIR, false);
            }
            playerPlacedBlocks.clear();
            for (Entity e : world.getEntities()) {
                if (!(e instanceof Player)) {
                    e.remove();
                }
            }
            world.getWorldBorder().reset();
        }
        unloadImportedRuntimeWorld();
        world = null;
        activeImported = null;
        activeMap = null;
    }

    // -------------------------------------------------------------------------
    // World-scoped Listeners
    // -------------------------------------------------------------------------

    private boolean isInGameWorld(Entity e) {
        return running && world != null && e != null && e.getWorld().equals(world);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        if (phase == Phase.COUNTDOWN && e.getPlayer().getGameMode() != GameMode.SPECTATOR && e.getTo() != null) {
            if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ()) {
                Location back = e.getFrom().clone();
                back.setYaw(e.getTo().getYaw());
                back.setPitch(e.getTo().getPitch());
                e.setTo(back);
            }
        } else if (e.getPlayer().getGameMode() == GameMode.SPECTATOR && e.getTo() != null) {
            int maxR = activeMapRadius() + 12;
            int cx = activeCenterX();
            int cz = activeCenterZ();
            if (Math.hypot(e.getTo().getX() - cx, e.getTo().getZ() - cz) > maxR) {
                e.setTo(new Location(world, cx + 0.5, activeCenterY() + 14.0, cz + 0.5));
            }
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (!isInGameWorld(e.getEntity())) return;
        if (phase == Phase.BUILDING || phase == Phase.COUNTDOWN) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent e) {
        if (!isInGameWorld(e.getEntity())) return;
        if (!(e.getEntity() instanceof Player victim)) return;

        Player attacker = null;
        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
        }
        if (attacker == null) return;

        if (phase != Phase.ACTIVE && phase != Phase.DEATHMATCH) {
            e.setCancelled(true);
            return;
        }
        String s1 = sideOfPlayer.get(attacker.getUniqueId());
        String s2 = sideOfPlayer.get(victim.getUniqueId());
        if (s1 != null && s1.equals(s2)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent e) {
        if (!isInGameWorld(e.getEnchanter())) return;
        Player p = e.getEnchanter();
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (running && p.isOnline()) {
                p.setLevel(INFINITE_XP_LEVEL);
                p.setExp(0f);
            }
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!isInGameWorld(e.getEntity())) return;
        Player victim = e.getEntity();
        handleElimination(victim.getUniqueId(), victim.getKiller());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        World gameWorld = world;
        e.setRespawnLocation(new Location(gameWorld, activeCenterX() + 0.5, activeCenterY() + 14.0, activeCenterZ() + 0.5));
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (running && world != null && world.equals(gameWorld) && e.getPlayer().isOnline() && e.getPlayer().getWorld().equals(gameWorld)) {
                e.getPlayer().setGameMode(GameMode.SPECTATOR);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!running || ctx == null) return;
        if (alive.contains(e.getPlayer().getUniqueId())) {
            handleElimination(e.getPlayer().getUniqueId(), null);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!isInGameWorld(e.getPlayer()) || ctx == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Location loc = e.getClickedBlock().getLocation();
        if (activeAirdrops.remove(loc)) {
            world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            String side = sideOfPlayer.get(e.getPlayer().getUniqueId());
            if (side != null) {
                int pts = plugin.getConfig().getInt("coins.airdrop", 180);   // MCC: airdrop crate, shared amongst the team
                if (pts > 0) ctx.addTeamCoins(side, pts, "Supply Airdrop");
                ctx.broadcast(ctx.sideLabel(side) + ChatColor.LIGHT_PURPLE + " captured a Supply Airdrop!");
            }
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        Location loc = e.getBlock().getLocation();
        if (playerPlacedBlocks.remove(loc)) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        if (phase != Phase.ACTIVE && phase != Phase.DEATHMATCH) {
            e.setCancelled(true);
            return;
        }
        if (PLACEABLE_BLOCKS.contains(e.getBlock().getType())) {
            playerPlacedBlocks.add(e.getBlock().getLocation());
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent e) {
        if (e.getWorld().getName().startsWith("mcc_hg_")) {
            purgeUnsafeLegacyEntity(e.getEntity());
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        if (!e.getWorld().getName().startsWith("mcc_hg_")) return;
        for (Entity entity : e.getEntities()) {
            purgeUnsafeLegacyEntity(entity);
        }
    }

    private static void purgeUnsafeLegacyEntity(Entity entity) {
        if (entity == null || entity instanceof Player) return;
        if (entity instanceof Fireball || entity instanceof Mob || entity instanceof ArmorStand) {
            entity.remove();
            return;
        }
        Vector v = entity.getVelocity();
        if (!Double.isFinite(v.getX()) || !Double.isFinite(v.getY()) || !Double.isFinite(v.getZ())) {
            entity.remove();
        }
    }
}
