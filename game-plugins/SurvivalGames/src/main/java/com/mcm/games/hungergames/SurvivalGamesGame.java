package com.mcm.games.hungergames;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.mcm.api.McmCoins;
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
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EnchantingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Modern 1.21.1 MCC-faithful Survival Games / Hunger Games (id: hungergames).
 */
public final class SurvivalGamesGame implements McmGame, Listener {
    private enum Phase { IDLE, BUILDING, COUNTDOWN, GRACE, ACTIVE, LAST_STAND }

    private enum CrateState { INCOMING, LANDED, BREAKING }

    private record ImportedChest(int x, int y, int z, LootTable.Tier tier) {}

    private record AttackRecord(UUID attackerId, long timestampMs) {}

    private static final class CoinCrate {
        final Location loc;
        CrateState state;
        int dropSecondsLeft;
        UUID breakerId;
        int breakTicksDone;
        final int breakTicksTotal;

        CoinCrate(Location loc, int dropSecondsLeft, int breakTicksTotal) {
            this.loc = loc;
            this.state = CrateState.INCOMING;
            this.dropSecondsLeft = dropSecondsLeft;
            this.breakTicksTotal = breakTicksTotal;
        }

        Location center() {
            return loc.clone().add(0.5, 0.5, 0.5);
        }
    }

    private static final int INFINITE_XP_LEVEL = 30;
    private static final int IMPORTED_CHEST_SCAN_CAP = 140;
    private static final int MAX_CHUNKS_SCANNED_PER_TICK = 8;
    private static final long CHUNK_SCAN_BUDGET_NANOS = 12_000_000L;
    private static final double DEFAULT_MAX_HEALTH = 20.0;
    private static final double LAST_STAND_MIN_HEALTH = 2.0;
    private static final double CRATE_BREAK_RANGE_SQ = 4.5 * 4.5;

    private static final Set<String> SKIPPED_WORLD_ENTRIES = Set.of(
            "uid.dat", "session.lock", "map.yml", "level.dat_old", "playerdata", "stats", "entities"
    );
    private static final Set<Material> PLACEABLE_BLOCKS = Set.of(
            Material.OAK_PLANKS, Material.COBBLESTONE, Material.COBWEB, Material.FIRE, Material.TNT
    );
    private static final Set<PotionEffectType> HARMFUL_POTION_EFFECTS = Set.of(
            PotionEffectType.INSTANT_DAMAGE,
            PotionEffectType.POISON,
            PotionEffectType.SLOWNESS,
            PotionEffectType.WEAKNESS
    );

    private final SurvivalGamesPlugin plugin;
    private final ArenaBuilder arenaBuilder = new ArenaBuilder();
    private final Object importedIoLock = new Object();
    private final Map<String, Integer> runtimeWorldGen = new HashMap<>();

    private volatile boolean running;
    private volatile Phase phase = Phase.IDLE;
    private final AtomicBoolean finished = new AtomicBoolean(false);
    private int buildGeneration;

    private McmGameContext ctx;
    private World world;
    private World importedRuntimeWorld;
    private int importedRuntimeGen;
    private SgMap activeMap;
    private ImportedSgWorld activeImported;
    private String forcedMap;
    private BukkitTask mainTask;
    private BukkitTask importedScanTask;
    private BukkitTask crateVisualTask;

    private final List<Location> importedPedestals = new ArrayList<>();
    private final List<ImportedChest> importedChests = new ArrayList<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Map<UUID, String> sideOfPlayer = new HashMap<>();
    private final List<String> eliminationOrder = new ArrayList<>();
    private final Map<UUID, AttackRecord> lastAttacker = new HashMap<>();
    private final Set<Location> playerPlacedBlocks = new HashSet<>();
    private final Set<UUID> playerPlacedBoats = new HashSet<>();

    private final List<CoinCrate> activeCrates = new ArrayList<>();
    private final List<Integer> scheduledCrateElapsed = new ArrayList<>();
    private int nextCrateScheduleIdx;

    private int phaseSecondsLeft;
    private int gameSecondsLeft;
    private int gameTotalSeconds;
    private boolean chestsRefilled;
    private boolean lastStandStarted;
    private int lastStandDecayTick;
    private double borderStartRadius;
    private double borderLastStandRadius;
    private int borderShrinkTotalSeconds;

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
        return "1.21.1 MCC Survival Games with infinite XP enchanting, 3-state Coin Crates, and Last Stand.";
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
        this.lastAttacker.clear();
        this.activeCrates.clear();
        this.scheduledCrateElapsed.clear();
        this.nextCrateScheduleIdx = 0;
        this.playerPlacedBlocks.clear();
        this.playerPlacedBoats.clear();
        this.importedPedestals.clear();
        this.importedChests.clear();
        this.chestsRefilled = false;
        this.lastStandStarted = false;
        this.lastStandDecayTick = 0;
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
        } else {
            String configuredDefault = plugin.getConfig().getString("default-map", "").trim().toLowerCase(Locale.ROOT);
            if (!configuredDefault.isEmpty()) {
                if (imported.containsKey(configuredDefault)) {
                    this.activeImported = imported.get(configuredDefault);
                } else if (SgMapRegistry.ids().contains(configuredDefault)) {
                    this.activeMap = SgMapRegistry.byId(configuredDefault);
                }
            }
            if (this.activeImported == null && this.activeMap == null) {
                if (plugin.getConfig().getBoolean("prefer-imported-maps", true) && !imported.isEmpty()) {
                    List<ImportedSgWorld> list = new ArrayList<>(imported.values());
                    this.activeImported = list.get(ThreadLocalRandom.current().nextInt(list.size()));
                } else {
                    List<SgMap> allMaps = SgMapRegistry.all();
                    this.activeMap = allMaps.get(ThreadLocalRandom.current().nextInt(allMaps.size()));
                }
            }
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
        this.borderStartRadius = initialRadius;
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
                p.leaveVehicle();
                p.teleport(fallback);
            }
            if (!Bukkit.unloadWorld(existing, false)) {
                plugin.getLogger().warning("Could not unload existing runtime world '" + runtimeName + "'; aborting load.");
                endGame("Failed to unload previous instance of '" + imp.id() + "'.");
                return;
            }
        }

        synchronized (importedIoLock) {
            runtimeWorldGen.put(runtimeName, gen);
        }

        Path targetDir = plugin.getServer().getWorldContainer().toPath().resolve(runtimeName);
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (!running || buildGeneration != gen) return;
            try {
                synchronized (importedIoLock) {
                    if (!running || buildGeneration != gen) return;
                    runtimeWorldGen.put(runtimeName, gen);
                    deleteRecursivelyWithRetry(targetDir);
                    copyWorldTemplate(imp.templateDir(), targetDir);
                }
            } catch (IOException ex) {
                plugin.getLogger().severe("Failed to copy imported SG world '" + imp.id() + "': " + ex.getMessage());
                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (running && buildGeneration == gen) {
                        endGame("Failed to load imported map '" + imp.id() + "'.");
                    }
                });
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!running || buildGeneration != gen) return;
                if (Bukkit.getWorld(runtimeName) != null) {
                    plugin.getLogger().warning("Runtime world '" + runtimeName + "' already loaded before createWorld().");
                    endGame("Runtime world conflict for '" + imp.id() + "'.");
                    return;
                }
                World loaded = new WorldCreator(runtimeName).createWorld();
                if (loaded == null) {
                    endGame("WorldCreator failed for '" + imp.id() + "'.");
                    return;
                }
                this.world = loaded;
                this.importedRuntimeWorld = loaded;
                this.importedRuntimeGen = gen;
                plugin.configureWorld(loaded);
                loaded.setAutoSave(false);

                WorldBorder border = loaded.getWorldBorder();
                this.borderStartRadius = imp.radius();
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
            long tickStart = System.nanoTime();
            int scannedThisTick = 0;
            while (idx[0] < chunks.size() && scannedThisTick < MAX_CHUNKS_SCANNED_PER_TICK) {
                if (scannedThisTick > 0 && (System.nanoTime() - tickStart) >= CHUNK_SCAN_BUDGET_NANOS) {
                    break;
                }
                int[] coord = chunks.get(idx[0]++);
                scannedThisTick++;
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
        List<Location> list = activeImported != null ? importedPedestals : arenaBuilder.getPedestals();
        if (!list.isEmpty()) return list;
        return List.of(new Location(world, activeCenterX() + 0.5, activeCenterY() + 1.0, activeCenterZ() + 0.5));
    }

    private void onArenaBuilt() {
        if (!running || ctx == null) return;

        List<Location> pedestals = activePedestals();
        // Order participants grouped by team side so teammates spawn on adjacent pedestals (MCC style).
        List<Player> orderedPlayers = new ArrayList<>();
        Set<UUID> added = new HashSet<>();
        for (List<Player> sidePlayers : ctx.sides().values()) {
            for (Player p : sidePlayers) {
                if (p != null && p.isOnline() && added.add(p.getUniqueId())) {
                    orderedPlayers.add(p);
                }
            }
        }
        for (Player p : ctx.participants()) {
            if (p != null && p.isOnline() && added.add(p.getUniqueId())) {
                orderedPlayers.add(p);
            }
        }

        for (int i = 0; i < orderedPlayers.size(); i++) {
            Player p = orderedPlayers.get(i);
            Location spawn = pedestals.get(i % pedestals.size()).clone();
            if (i >= pedestals.size()) {
                double offsetAngle = (2.0 * Math.PI * (i / pedestals.size())) / 4.0;
                spawn.add(Math.cos(offsetAngle) * 0.35, 0.0, Math.sin(offsetAngle) * 0.35);
            }
            p.leaveVehicle();
            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            resetMaxHealth(p);
            p.setHealth(DEFAULT_MAX_HEALTH);
            p.setFoodLevel(20);
            p.setSaturation(20.0f); // MCC 8+: full saturation at start
            p.setLevel(INFINITE_XP_LEVEL);
            p.setExp(0f);
            p.getInventory().clear();
            giveStarterKit(p);
        }

        gameTotalSeconds = Math.max(30, plugin.getConfig().getInt("timings.game-seconds", 660));
        gameSecondsLeft = gameTotalSeconds;
        borderLastStandRadius = plugin.getConfig().getDouble("border.last-stand-radius", 20.0);
        int lastStandAt = getLastStandTriggerRemainingSeconds();
        borderShrinkTotalSeconds = Math.max(15, gameTotalSeconds - lastStandAt);
        scheduleCoinCrates(lastStandAt);

        int countdown = Waits.seconds(plugin.getConfig().getInt("timings.countdown-seconds", 10));
        if (countdown <= 0) {
            startGraceOrActive();
            return;
        }

        phase = Phase.COUNTDOWN;
        phaseSecondsLeft = countdown;
        startTickLoop();
    }

    private int getLastStandTriggerRemainingSeconds() {
        int configured = plugin.getConfig().getInt("timings.last-stand-at-remaining-seconds", 90);
        return Math.max(5, Math.min(gameTotalSeconds - 10, configured));
    }

    private void scheduleCoinCrates(int lastStandAt) {
        scheduledCrateElapsed.clear();
        nextCrateScheduleIdx = 0;
        int crateCount = Math.max(0, plugin.getConfig().getInt("timings.crate-count", 4));
        if (crateCount == 0) return;

        int dropSeconds = Math.max(3, plugin.getConfig().getInt("timings.crate-drop-seconds", 20));
        int activeSpan = Math.max(10, gameTotalSeconds - lastStandAt);
        int earliest = Math.min(35, Math.max(6, activeSpan / 5));
        int latest = Math.max(earliest, activeSpan - dropSeconds - 4);

        if (crateCount == 1 || latest <= earliest) {
            scheduledCrateElapsed.add((earliest + latest) / 2);
            return;
        }
        double step = (double) (latest - earliest) / (crateCount - 1);
        for (int i = 0; i < crateCount; i++) {
            int t = (int) Math.round(earliest + step * i);
            if (scheduledCrateElapsed.isEmpty() || t > scheduledCrateElapsed.get(scheduledCrateElapsed.size() - 1)) {
                scheduledCrateElapsed.add(t);
            } else {
                scheduledCrateElapsed.add(scheduledCrateElapsed.get(scheduledCrateElapsed.size() - 1) + 2);
            }
        }
    }

    private void giveStarterKit(Player p) {
        ItemStack compass = new ItemStack(Material.COMPASS, 1);
        ItemMeta meta = compass.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.AQUA + "Crate Tracker");
            meta.setLore(List.of(ChatColor.GRAY + "Points to active Coin Crates"));
            compass.setItemMeta(meta);
        }
        p.getInventory().addItem(compass);
        if (activeBoatRecommended()) {
            p.getInventory().addItem(new ItemStack(Material.OAK_BOAT, 1));
        }

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        if (boots.getItemMeta() instanceof LeatherArmorMeta lMeta) {
            lMeta.setColor(teamColorToBukkit(ctx.teamColor(ctx.teamOf(p.getUniqueId()))));
            boots.setItemMeta(lMeta);
        }
        p.getInventory().setBoots(boots);
    }

    private static void resetMaxHealth(Player p) {
        AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr != null && attr.getBaseValue() != DEFAULT_MAX_HEALTH) {
            attr.setBaseValue(DEFAULT_MAX_HEALTH);
        }
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
        int grace = Waits.seconds(plugin.getConfig().getInt("timings.grace-seconds", 25));
        if (grace <= 0) {
            startActivePhase();
            return;
        }
        phase = Phase.GRACE;
        phaseSecondsLeft = grace;
        ctx.broadcast(ChatColor.GREEN + "GO! " + ChatColor.YELLOW + "Grace period active for " + grace + "s! Enchanting & anvils have infinite Lv.30!");
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
        double targetDiameter = Math.min(border.getSize(), borderLastStandRadius * 2.0);
        border.setSize(targetDiameter, Math.max(15, borderShrinkTotalSeconds));

        startCrateVisualTask();
        if (mainTask == null) {
            startTickLoop();
        }
    }

    private void startTickLoop() {
        if (mainTask != null) mainTask.cancel();
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickSecond, 20L, 20L);
    }

    private void startCrateVisualTask() {
        if (crateVisualTask != null) crateVisualTask.cancel();
        crateVisualTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickCrates5Ticks, 5L, 5L);
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

        // ACTIVE or LAST_STAND
        gameSecondsLeft--;
        ctx.setTimer(Math.max(0, gameSecondsLeft), gameTotalSeconds);

        int elapsed = gameTotalSeconds - gameSecondsLeft;
        int refillAt = plugin.getConfig().getInt("timings.refill-at-elapsed-seconds", 270);
        if (refillAt > 0 && !chestsRefilled && elapsed >= refillAt) {
            chestsRefilled = true;
            refillActiveChests();
            ctx.broadcast(ChatColor.AQUA + "★ ALL CHESTS REFILLED WITH UPGRADED LOOT! ★");
        }

        if (phase == Phase.ACTIVE
                && nextCrateScheduleIdx < scheduledCrateElapsed.size()
                && elapsed >= scheduledCrateElapsed.get(nextCrateScheduleIdx)) {
            nextCrateScheduleIdx++;
            announceCoinCrate();
        }

        tickIncomingCrates();

        int lastStandAt = getLastStandTriggerRemainingSeconds();
        if (!lastStandStarted && gameSecondsLeft <= lastStandAt) {
            startLastStand();
        }
        if (phase == Phase.LAST_STAND) {
            tickLastStandDecay();
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
            if (!m.isAir() && m != Material.WATER && m != Material.LAVA && !m.name().endsWith("_LEAVES")) {
                return y;
            }
        }
        return activeImported.centerY();
    }

    private void announceCoinCrate() {
        int dropSeconds = Math.max(3, plugin.getConfig().getInt("timings.crate-drop-seconds", 20));
        int breakSeconds = Math.max(1, plugin.getConfig().getInt("timings.crate-break-seconds", 6));

        double currentRadius = world.getWorldBorder().getSize() / 2.0;
        double ratePerSec = borderShrinkTotalSeconds > 0
                ? Math.max(0.0, (borderStartRadius - borderLastStandRadius) / borderShrinkTotalSeconds)
                : 0.0;
        double projectedRadius = Math.max(borderLastStandRadius,
                currentRadius - ratePerSec * (dropSeconds + breakSeconds + 4));
        double maxR = Math.max(4.0, projectedRadius - 6.0);
        double minR = Math.min(6.0, maxR * 0.4);

        int cx = activeCenterX();
        int cz = activeCenterZ();
        Location chosen = null;

        for (int attempt = 0; attempt < 28; attempt++) {
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
            double dist = minR + ThreadLocalRandom.current().nextDouble() * Math.max(1.0, maxR - minR);
            int x = cx + (int) Math.round(Math.cos(angle) * dist);
            int z = cz + (int) Math.round(Math.sin(angle) * dist);
            int topY = findSafeTopY(x, z);
            Block ground = world.getBlockAt(x, topY, z);
            Block cratePos = world.getBlockAt(x, topY + 1, z);
            Block headPos = world.getBlockAt(x, topY + 2, z);
            if (ground.getType().isSolid()
                    && !ground.isLiquid()
                    && cratePos.isPassable() && !cratePos.isLiquid()
                    && headPos.isPassable() && !headPos.isLiquid()
                    && findCrateAt(cratePos.getLocation()) == null) {
                chosen = cratePos.getLocation();
                break;
            }
        }
        if (chosen == null) {
            int topY = findSafeTopY(cx, cz);
            chosen = new Location(world, cx, topY + 1, cz);
        }

        CoinCrate crate = new CoinCrate(chosen, dropSeconds, breakSeconds * 20);
        activeCrates.add(crate);

        ctx.broadcast(ChatColor.WHITE + "★ COIN CRATE incoming in " + dropSeconds + "s at "
                + ChatColor.AQUA + chosen.getBlockX() + ", " + chosen.getBlockY() + ", " + chosen.getBlockZ()
                + ChatColor.GRAY + " (White Beacon — follow your Compass!)");
    }

    private void tickIncomingCrates() {
        for (CoinCrate crate : activeCrates) {
            if (crate.state != CrateState.INCOMING) continue;
            crate.dropSecondsLeft--;
            if (crate.dropSecondsLeft <= 0) {
                landCoinCrate(crate);
            }
        }
    }

    private void landCoinCrate(CoinCrate crate) {
        crate.state = CrateState.LANDED;
        Block base = world.getBlockAt(crate.loc.getBlockX(), crate.loc.getBlockY() - 1, crate.loc.getBlockZ());
        if (!base.getType().isSolid() || base.isLiquid()) {
            base.setType(Material.GOLD_BLOCK, false);
        }
        Block block = world.getBlockAt(crate.loc);
        playerPlacedBlocks.remove(block.getLocation());
        block.setType(Material.BARREL, false);

        Firework fw = (Firework) world.spawnEntity(crate.center().add(0.0, 1.0, 0.0), EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = fw.getFireworkMeta();
        meta.addEffect(FireworkEffect.builder().withColor(Color.LIME, Color.YELLOW).with(FireworkEffect.Type.BALL_LARGE).build());
        meta.setPower(1);
        fw.setFireworkMeta(meta);

        ctx.broadcast(ChatColor.GREEN + "★ COIN CRATE LANDED at "
                + ChatColor.AQUA + crate.loc.getBlockX() + ", " + crate.loc.getBlockY() + ", " + crate.loc.getBlockZ()
                + ChatColor.GRAY + " (Green Beacon — break to claim +180 team coins!)");
    }

    private void tickCrates5Ticks() {
        if (!running || world == null) return;
        Iterator<CoinCrate> it = activeCrates.iterator();
        while (it.hasNext()) {
            CoinCrate crate = it.next();
            Color beamColor = switch (crate.state) {
                case INCOMING -> Color.WHITE;
                case LANDED -> Color.LIME;
                case BREAKING -> Color.RED;
            };
            spawnBeaconBeam(crate.center(), beamColor);

            if (crate.state != CrateState.BREAKING) continue;

            Player breaker = crate.breakerId != null ? Bukkit.getPlayer(crate.breakerId) : null;
            if (breaker == null
                    || !breaker.isOnline()
                    || !alive.contains(breaker.getUniqueId())
                    || breaker.getGameMode() == GameMode.SPECTATOR
                    || !breaker.getWorld().equals(world)
                    || breaker.getLocation().distanceSquared(crate.center()) > CRATE_BREAK_RANGE_SQ) {
                crate.state = CrateState.LANDED;
                crate.breakerId = null;
                crate.breakTicksDone = 0;
                continue;
            }

            crate.breakTicksDone += 5;
            if (crate.breakTicksDone % 10 == 0) {
                world.playSound(crate.center(), Sound.BLOCK_WOOD_HIT, 0.9f, 1.1f);
                world.spawnParticle(Particle.CRIT, crate.center().add(0.0, 0.4, 0.0), 8, 0.25, 0.25, 0.25, 0.05);
            }
            int pct = Math.min(100, (crate.breakTicksDone * 100) / Math.max(1, crate.breakTicksTotal));
            sendBreakProgressBar(breaker, pct);

            if (crate.breakTicksDone >= crate.breakTicksTotal) {
                completeCoinCrateBreak(crate, breaker);
                it.remove();
            }
        }
    }

    private void spawnBeaconBeam(Location center, Color color) {
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.8f);
        for (double dy = 0.8; dy <= 34.0; dy += 1.6) {
            world.spawnParticle(Particle.DUST, center.getX(), center.getY() + dy, center.getZ(), 1, 0.08, 0.1, 0.08, 0.0, dust);
        }
    }

    private static void sendBreakProgressBar(Player breaker, int pct) {
        int filled = Math.max(0, Math.min(10, pct / 10));
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.RED).append("Breaking Coin Crate ").append(ChatColor.DARK_GRAY).append("[");
        bar.append(ChatColor.GREEN);
        for (int i = 0; i < filled; i++) bar.append('█');
        bar.append(ChatColor.GRAY);
        for (int i = filled; i < 10; i++) bar.append('░');
        bar.append(ChatColor.DARK_GRAY).append("] ").append(ChatColor.YELLOW).append(pct).append("%");
        breaker.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(bar.toString()));
    }

    private void completeCoinCrateBreak(CoinCrate crate, Player breaker) {
        world.getBlockAt(crate.loc).setType(Material.AIR, false);
        world.playSound(crate.center(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, crate.center(), 24, 0.4, 0.5, 0.4, 0.1);

        String side = sideOfPlayer.get(breaker.getUniqueId());
        int pts = plugin.getConfig().getInt("coins.airdrop", 180);
        if (side != null) {
            if (pts > 0) ctx.addTeamCoins(side, pts, "Coin Crate");
            ctx.broadcast(ctx.sideLabel(side) + ChatColor.LIGHT_PURPLE + " broke open a Coin Crate! (+" + pts + " team coins)");
        }

        Location dropOrigin = crate.center().add(0.0, 0.25, 0.0);
        for (ItemStack item : LootTable.createCrateBurst()) {
            world.dropItemNaturally(dropOrigin, item);
        }
    }

    private CoinCrate findCrateAt(Location blockLoc) {
        for (CoinCrate c : activeCrates) {
            if (c.loc.getBlockX() == blockLoc.getBlockX()
                    && c.loc.getBlockY() == blockLoc.getBlockY()
                    && c.loc.getBlockZ() == blockLoc.getBlockZ()) {
                return c;
            }
        }
        return null;
    }

    private boolean isNearAnyCrate(Location blockLoc) {
        for (CoinCrate c : activeCrates) {
            if (Math.abs(c.loc.getBlockX() - blockLoc.getBlockX()) <= 1
                    && Math.abs(c.loc.getBlockZ() - blockLoc.getBlockZ()) <= 1
                    && blockLoc.getBlockY() >= c.loc.getBlockY()
                    && blockLoc.getBlockY() <= c.loc.getBlockY() + 2) {
                return true;
            }
        }
        return false;
    }

    private void startLastStand() {
        lastStandStarted = true;
        lastStandDecayTick = 0;
        phase = Phase.LAST_STAND;
        int interval = Math.max(1, plugin.getConfig().getInt("timings.last-stand-decay-interval-seconds", 3));
        ctx.broadcast(ChatColor.DARK_RED + "⚔ LAST STAND! " + ChatColor.RED
                + "Max health now decays by 0.5❤ every " + interval + "s down to 1❤!");
        for (Player p : ctx.participants()) {
            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.7f, 1.0f);
        }
    }

    private void tickLastStandDecay() {
        int interval = Math.max(1, plugin.getConfig().getInt("timings.last-stand-decay-interval-seconds", 3));
        lastStandDecayTick++;
        if (lastStandDecayTick % interval != 0) return;

        for (UUID uuid : alive) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline() || !p.getWorld().equals(world)) continue;
            AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            if (attr == null) continue;
            double curMax = attr.getBaseValue();
            if (curMax > LAST_STAND_MIN_HEALTH) {
                double nextMax = Math.max(LAST_STAND_MIN_HEALTH, curMax - 1.0);
                attr.setBaseValue(nextMax);
                if (p.getHealth() > nextMax) {
                    p.setHealth(nextMax);
                }
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.45f, 0.75f);
            }
        }
    }

    private void updateCompassAndActionBar() {
        CoinCrate focusCrate = findFocusCrate();
        Location centerLoc = new Location(world, activeCenterX() + 0.5, activeCenterY(), activeCenterZ() + 0.5);

        for (UUID uuid : alive) {
            Player p = Bukkit.getPlayer(uuid);
            if (p == null || !p.isOnline()) continue;

            if (focusCrate != null) {
                p.setCompassTarget(focusCrate.loc);
            } else {
                p.setCompassTarget(centerLoc);
            }

            if (isBreakingAnyCrate(uuid)) {
                continue;
            }

            String barText;
            if (phase == Phase.GRACE) {
                barText = ChatColor.GREEN + "GRACE PERIOD: " + ChatColor.WHITE + phaseSecondsLeft + "s"
                        + ChatColor.GRAY + " • Loot & Enchant (Lv.30)";
            } else if (focusCrate != null) {
                int dist = p.getWorld().equals(world)
                        ? (int) Math.round(p.getLocation().distance(focusCrate.center()))
                        : 0;
                if (focusCrate.state == CrateState.INCOMING) {
                    barText = ChatColor.WHITE + "Coin Crate incoming in " + focusCrate.dropSecondsLeft + "s"
                            + ChatColor.GRAY + " (" + dist + "m)";
                } else if (focusCrate.state == CrateState.BREAKING) {
                    barText = ChatColor.RED + "Coin Crate CONTESTED at "
                            + focusCrate.loc.getBlockX() + ", " + focusCrate.loc.getBlockZ()
                            + ChatColor.GRAY + " (" + dist + "m)";
                } else {
                    barText = ChatColor.GREEN + "Coin Crate LANDED at "
                            + focusCrate.loc.getBlockX() + ", " + focusCrate.loc.getBlockZ()
                            + ChatColor.GRAY + " (" + dist + "m)";
                }
            } else if (phase == Phase.LAST_STAND) {
                AttributeInstance attr = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double hearts = attr != null ? attr.getBaseValue() / 2.0 : 10.0;
                int borderR = (int) (world.getWorldBorder().getSize() / 2.0);
                barText = ChatColor.DARK_RED + "⚔ LAST STAND" + ChatColor.GRAY + " • "
                        + ChatColor.RED + String.format(Locale.ROOT, "Max %.1f❤", hearts)
                        + ChatColor.GRAY + " • Border " + ChatColor.AQUA + borderR + "m";
            } else {
                int borderR = (int) (world.getWorldBorder().getSize() / 2.0);
                int elapsed = gameTotalSeconds - gameSecondsLeft;
                String nextStr = nextCrateScheduleIdx < scheduledCrateElapsed.size()
                        ? " • Next Crate in " + Math.max(0, scheduledCrateElapsed.get(nextCrateScheduleIdx) - elapsed) + "s"
                        : "";
                barText = ChatColor.AQUA + "Border: " + borderR + "m" + ChatColor.GRAY + nextStr;
            }
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(barText));
        }
    }

    private CoinCrate findFocusCrate() {
        for (CoinCrate c : activeCrates) {
            if (c.state == CrateState.BREAKING) return c;
        }
        for (CoinCrate c : activeCrates) {
            if (c.state == CrateState.LANDED) return c;
        }
        return activeCrates.isEmpty() ? null : activeCrates.get(0);
    }

    private boolean isBreakingAnyCrate(UUID playerId) {
        for (CoinCrate c : activeCrates) {
            if (c.state == CrateState.BREAKING && playerId.equals(c.breakerId)) return true;
        }
        return false;
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

        Player resolvedKiller = killer;
        if (resolvedKiller == null || resolvedKiller.getUniqueId().equals(victimId)) {
            resolvedKiller = resolveRecentAttacker(victimId);
        }

        int killPts = plugin.getConfig().getInt("coins.kill", 65);
        int survPts = plugin.getConfig().getInt("coins.outlive", 3);

        Player victimPlayer = Bukkit.getPlayer(victimId);
        String victimName = victimPlayer != null ? victimPlayer.getName() : "a player";
        if (resolvedKiller != null && !resolvedKiller.getUniqueId().equals(victimId) && killPts > 0) {
            ctx.addKillCoins(resolvedKiller.getUniqueId(), killPts, victimId, victimName, "You eliminated " + victimName);
        }
        if (survPts > 0) {
            for (UUID survivor : alive) {
                ctx.addCoins(survivor, survPts, victimName + " was eliminated");
            }
        }

        String victimSide = sideOfPlayer.get(victimId);
        if (victimSide != null && !isSideAlive(victimSide) && !eliminationOrder.contains(victimSide)) {
            eliminationOrder.add(victimSide);
            int remainingSides = countLivingSides();
            ctx.broadcast(ChatColor.RED + "☠ " + ctx.sideLabel(victimSide) + ChatColor.RED
                    + " has been eliminated! (" + remainingSides + " teams remaining)");
        }

        checkWinCondition();
    }

    private Player resolveRecentAttacker(UUID victimId) {
        AttackRecord rec = lastAttacker.remove(victimId);
        if (rec == null) return null;
        long windowMs = Math.max(1L, plugin.getConfig().getLong("combat.kill-assist-window-seconds", 10L)) * 1000L;
        if (System.currentTimeMillis() - rec.timestampMs() > windowMs) return null;
        Player p = Bukkit.getPlayer(rec.attackerId());
        return (p != null && p.isOnline()) ? p : null;
    }

    private boolean isSideAlive(String side) {
        for (UUID uuid : alive) {
            if (side.equals(sideOfPlayer.get(uuid))) return true;
        }
        return false;
    }

    private int countLivingSides() {
        Set<String> livingSides = new LinkedHashSet<>();
        for (UUID uuid : alive) {
            String side = sideOfPlayer.get(uuid);
            if (side != null) livingSides.add(side);
        }
        return livingSides.size();
    }

    private void checkWinCondition() {
        if (!running || finished.get()) return;
        if (ctx.sides().size() <= 1) return; // Solo or single-team admin testing

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
        if (ctx.sides().size() <= 1 || phase == Phase.BUILDING) return;

        List<String> living = new ArrayList<>();
        Set<String> livingSet = new LinkedHashSet<>();
        for (UUID uuid : alive) {
            String side = sideOfPlayer.get(uuid);
            if (side != null && livingSet.add(side)) living.add(side);
        }

        List<String> eliminatedRanked = new ArrayList<>();
        for (int i = eliminationOrder.size() - 1; i >= 0; i--) {
            String s = eliminationOrder.get(i);
            if (!livingSet.contains(s) && !eliminatedRanked.contains(s)) {
                eliminatedRanked.add(s);
            }
        }

        int[] bonuses = McmCoins.toIntArray(plugin.getConfig().getList("coins.placement"),
                new int[]{650, 600, 560, 520, 480, 450, 420, 390, 360, 330});

        // Tie-average placement pool across surviving teams if time expires with multiple teams alive.
        if (!living.isEmpty()) {
            double totalPool = 0.0;
            for (int r = 1; r <= living.size(); r++) {
                totalPool += McmCoins.placementOrLast(bonuses, r);
            }
            double avgPool = totalPool / living.size();
            String label = living.size() == 1 ? "Team Placement #1" : "Team Placement #1 (Tied)";
            if (avgPool > 0) {
                for (String side : living) {
                    ctx.addTeamCoins(side, avgPool, label);
                }
            }
        }

        int rankOffset = living.size();
        for (int i = 0; i < eliminatedRanked.size(); i++) {
            int rank = rankOffset + i + 1;
            int pool = McmCoins.placementOrLast(bonuses, rank);
            if (pool > 0) {
                ctx.addTeamCoins(eliminatedRanked.get(i), pool, "Team Placement #" + rank);
            }
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
        if (crateVisualTask != null) {
            crateVisualTask.cancel();
            crateVisualTask = null;
        }
        arenaBuilder.cancel();
        ctx.finish(reason);
        unloadImportedRuntimeWorld();
    }

    private void cleanupWorldState() {
        if (ctx != null) {
            for (Player p : ctx.participants()) {
                p.leaveVehicle();
                resetMaxHealth(p);
                p.setLevel(0);
                p.setExp(0f);
            }
        }
        if (world != null) {
            for (CoinCrate c : activeCrates) {
                if (c.state != CrateState.INCOMING) {
                    world.getBlockAt(c.loc).setType(Material.AIR, false);
                }
            }
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
        activeCrates.clear();
        playerPlacedBlocks.clear();
        playerPlacedBoats.clear();
        lastAttacker.clear();
    }

    private void unloadImportedRuntimeWorld() {
        if (importedRuntimeWorld == null) return;
        World toUnload = importedRuntimeWorld;
        String runtimeName = toUnload.getName();
        int myGen = importedRuntimeGen;
        importedRuntimeWorld = null;
        if (world == toUnload) {
            world = null;
        }
        Location fallback = Bukkit.getWorlds().get(0).getSpawnLocation();
        for (Player p : new ArrayList<>(toUnload.getPlayers())) {
            p.leaveVehicle();
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
                    if (!Objects.equals(runtimeWorldGen.get(runtimeName), myGen)) {
                        return;
                    }
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
        if (crateVisualTask != null) {
            crateVisualTask.cancel();
            crateVisualTask = null;
        }
        arenaBuilder.cancel();
        if (ctx != null) {
            for (Player p : ctx.participants()) {
                p.leaveVehicle();
                resetMaxHealth(p);
                p.setLevel(0);
                p.setExp(0f);
            }
        }
        alive.clear();
        sideOfPlayer.clear();
        eliminationOrder.clear();
        lastAttacker.clear();
        activeCrates.clear();
        scheduledCrateElapsed.clear();
        importedPedestals.clear();
        importedChests.clear();
        if (world != null) {
            for (Location loc : playerPlacedBlocks) {
                world.getBlockAt(loc).setType(Material.AIR, false);
            }
            playerPlacedBlocks.clear();
            playerPlacedBoats.clear();
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
        if ((phase == Phase.BUILDING || phase == Phase.COUNTDOWN)
                && e.getPlayer().getGameMode() != GameMode.SPECTATOR
                && e.getTo() != null) {
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
                Location clamped = new Location(world, cx + 0.5, activeCenterY() + 14.0, cz + 0.5,
                        e.getTo().getYaw(), e.getTo().getPitch());
                e.setTo(clamped);
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

        Player attacker = resolveAttacker(e.getDamager());
        if (attacker == null) return;

        // Allow self-inflicted damage (e.g., Ender Pearls, Wind Charge self-plays) during active play.
        if (attacker.getUniqueId().equals(victim.getUniqueId())) {
            if (phase != Phase.ACTIVE && phase != Phase.LAST_STAND) {
                e.setCancelled(true);
            }
            return;
        }

        if (phase != Phase.ACTIVE && phase != Phase.LAST_STAND) {
            e.setCancelled(true);
            return;
        }
        String s1 = sideOfPlayer.get(attacker.getUniqueId());
        String s2 = sideOfPlayer.get(victim.getUniqueId());
        if (s1 != null && s1.equals(s2)) {
            e.setCancelled(true);
            return;
        }

        // Nerf ultra-rare 1.21 Mace smash damage cap.
        if (e.getDamager() instanceof Player directAttacker
                && directAttacker.getInventory().getItemInMainHand().getType() == Material.MACE) {
            double maxMaceDmg = plugin.getConfig().getDouble("combat.mace-max-damage", 11.0);
            if (e.getDamage() > maxMaceDmg) {
                e.setDamage(maxMaceDmg);
            }
        }

        lastAttacker.put(victim.getUniqueId(), new AttackRecord(attacker.getUniqueId(), System.currentTimeMillis()));

        // Interrupt Coin Crate breaking when taking enemy PvP damage.
        for (CoinCrate c : activeCrates) {
            if (c.state == CrateState.BREAKING && victim.getUniqueId().equals(c.breakerId)) {
                c.state = CrateState.LANDED;
                c.breakerId = null;
                c.breakTicksDone = 0;
                victim.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                        new TextComponent(ChatColor.RED + "Coin Crate break interrupted!"));
            }
        }
    }

    @EventHandler
    public void onPotionSplash(PotionSplashEvent e) {
        if (!isInGameWorld(e.getPotion())) return;
        if (!(e.getPotion().getShooter() instanceof Player thrower)) return;

        boolean harmful = false;
        for (PotionEffect eff : e.getPotion().getEffects()) {
            if (HARMFUL_POTION_EFFECTS.contains(eff.getType())) {
                harmful = true;
                break;
            }
        }
        if (!harmful) return;

        if (phase != Phase.ACTIVE && phase != Phase.LAST_STAND) {
            e.setCancelled(true);
            return;
        }

        String throwerSide = sideOfPlayer.get(thrower.getUniqueId());
        for (LivingEntity target : e.getAffectedEntities()) {
            if (!(target instanceof Player victim)) continue;
            if (victim.getUniqueId().equals(thrower.getUniqueId())) continue;
            String victimSide = sideOfPlayer.get(victim.getUniqueId());
            if (throwerSide != null && throwerSide.equals(victimSide)) {
                e.setIntensity(target, 0.0);
            } else {
                lastAttacker.put(victim.getUniqueId(), new AttackRecord(thrower.getUniqueId(), System.currentTimeMillis()));
            }
        }
    }

    private static Player resolveAttacker(Entity damager) {
        if (damager instanceof Player p) {
            return p;
        }
        if (damager instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            return p;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) {
            return p;
        }
        return null;
    }

    @EventHandler
    public void onInventoryOpen(InventoryOpenEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        if (e.getInventory() instanceof EnchantingInventory ei) {
            ei.setSecondary(new ItemStack(Material.LAPIS_LAZULI, 64));
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        if (e.getInventory() instanceof EnchantingInventory ei) {
            ei.setSecondary(null);
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !isInGameWorld(p)) return;
        if (e.getClickedInventory() instanceof EnchantingInventory && e.getSlot() == 1) {
            e.setCancelled(true);
            return;
        }
        if (e.getInventory() instanceof AnvilInventory && e.getSlotType() == InventoryType.SlotType.RESULT) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (running && p.isOnline() && isInGameWorld(p)) {
                    p.setLevel(INFINITE_XP_LEVEL);
                    p.setExp(0f);
                }
            });
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
                if (e.getInventory() instanceof EnchantingInventory ei) {
                    ei.setSecondary(new ItemStack(Material.LAPIS_LAZULI, 64));
                }
            }
        });
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (!isInGameWorld(e.getEntity())) return;
        Player victim = e.getEntity();
        resetMaxHealth(victim);
        handleElimination(victim.getUniqueId(), victim.getKiller());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        World gameWorld = world;
        e.setRespawnLocation(new Location(gameWorld, activeCenterX() + 0.5, activeCenterY() + 14.0, activeCenterZ() + 0.5));
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (running && world != null && world.equals(gameWorld) && e.getPlayer().isOnline() && e.getPlayer().getWorld().equals(gameWorld)) {
                resetMaxHealth(e.getPlayer());
                e.getPlayer().setGameMode(GameMode.SPECTATOR);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        resetMaxHealth(e.getPlayer());
        if (!running || ctx == null) return;
        if (alive.contains(e.getPlayer().getUniqueId())) {
            handleElimination(e.getPlayer().getUniqueId(), null);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!isInGameWorld(e.getPlayer()) || ctx == null) return;
        Player p = e.getPlayer();

        // Prevent boat placement during BUILDING / COUNTDOWN
        if ((phase == Phase.BUILDING || phase == Phase.COUNTDOWN)
                && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.RIGHT_CLICK_AIR)
                && e.getItem() != null
                && e.getItem().getType().name().endsWith("_BOAT")) {
            e.setCancelled(true);
            return;
        }

        if (e.getClickedBlock() == null) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK && e.getAction() != Action.LEFT_CLICK_BLOCK) return;

        CoinCrate crate = findCrateAt(e.getClickedBlock().getLocation());
        if (crate == null || crate.state == CrateState.INCOMING) return;

        e.setCancelled(true); // Never open barrel/chest GUI; tools do not speed up breaking
        tryStartCrateBreak(crate, p);
    }

    private void tryStartCrateBreak(CoinCrate crate, Player p) {
        if (phase != Phase.ACTIVE && phase != Phase.LAST_STAND) return;
        if (!alive.contains(p.getUniqueId()) || p.getGameMode() == GameMode.SPECTATOR) return;

        if (crate.state == CrateState.LANDED) {
            crate.state = CrateState.BREAKING;
            crate.breakerId = p.getUniqueId();
            crate.breakTicksDone = 0;
            world.playSound(crate.center(), Sound.BLOCK_CHEST_OPEN, 0.8f, 0.9f);
            sendBreakProgressBar(p, 0);
        }
    }

    @EventHandler
    public void onEntityPlace(EntityPlaceEvent e) {
        if (!running || world == null || !e.getEntity().getWorld().equals(world)) return;
        if (e.getEntity() instanceof Boat boat) {
            if (phase == Phase.BUILDING || phase == Phase.COUNTDOWN) {
                e.setCancelled(true);
                return;
            }
            playerPlacedBoats.add(boat.getUniqueId());
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent e) {
        if (!isInGameWorld(e.getEntered())) return;
        if (phase == Phase.BUILDING || phase == Phase.COUNTDOWN) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onVehicleDestroy(VehicleDestroyEvent e) {
        if (!isInGameWorld(e.getVehicle())) return;
        if (e.getVehicle() instanceof Boat boat) {
            playerPlacedBoats.remove(boat.getUniqueId());
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        Block block = e.getBlock();
        Location loc = block.getLocation();

        CoinCrate crate = findCrateAt(loc);
        if (crate != null) {
            e.setCancelled(true);
            tryStartCrateBreak(crate, e.getPlayer());
            return;
        }

        if (playerPlacedBlocks.remove(loc)) {
            return;
        }
        // MCC P21: leaves are breakable (without dropping saplings/sticks)
        if (block.getType().name().endsWith("_LEAVES")) {
            e.setDropItems(false);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        if (phase != Phase.ACTIVE && phase != Phase.LAST_STAND) {
            e.setCancelled(true);
            return;
        }
        Location loc = e.getBlock().getLocation();
        if (isNearAnyCrate(loc)) {
            e.setCancelled(true);
            return;
        }
        Material mat = e.getBlock().getType();
        if (mat == Material.TNT) {
            // Auto-igniting tactical TNT: explodes for player damage/knockback only, zero terrain grief
            e.getBlock().setType(Material.AIR, false);
            int fuse = Math.max(10, plugin.getConfig().getInt("combat.tnt-fuse-ticks", 45));
            TNTPrimed primed = (TNTPrimed) world.spawnEntity(loc.clone().add(0.5, 0.0, 0.5), EntityType.TNT);
            primed.setFuseTicks(fuse);
            primed.setSource(e.getPlayer());
            world.playSound(loc, Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);
            return;
        }
        if (PLACEABLE_BLOCKS.contains(mat)) {
            playerPlacedBlocks.add(loc);
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onExplode(EntityExplodeEvent e) {
        if (!isInGameWorld(e.getEntity())) return;
        // Preserve map geometry completely; only allow destroying player-placed tactical blocks.
        Iterator<Block> it = e.blockList().iterator();
        while (it.hasNext()) {
            Block b = it.next();
            Location loc = b.getLocation();
            if (!playerPlacedBlocks.remove(loc)) {
                it.remove();
            }
        }
    }

    @EventHandler
    public void onEntityAddToWorld(EntityAddToWorldEvent e) {
        if (!e.getWorld().getName().startsWith("mcc_hg_")) return;
        Entity entity = e.getEntity();
        if (isUnsafeLegacyEntity(entity)) {
            // Defer by 1 tick so Moonrise is never mid-section-status-update during remove().
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (entity.isValid()) {
                    purgeUnsafeLegacyEntity(entity);
                }
            });
        }
    }

    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        if (!e.getWorld().getName().startsWith("mcc_hg_")) return;
        for (Entity entity : e.getEntities()) {
            purgeUnsafeLegacyEntity(entity);
        }
    }

    private static boolean isUnsafeLegacyEntity(Entity entity) {
        if (entity == null || entity instanceof Player) return false;
        if (entity instanceof Fireball || entity instanceof Mob || entity instanceof ArmorStand) {
            return true;
        }
        Vector v = entity.getVelocity();
        return !Double.isFinite(v.getX()) || !Double.isFinite(v.getY()) || !Double.isFinite(v.getZ());
    }

    private static void purgeUnsafeLegacyEntity(Entity entity) {
        if (isUnsafeLegacyEntity(entity)) {
            entity.remove();
        }
    }
}
