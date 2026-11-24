package com.mcm.games.hungergames;

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
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
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

/**
 * Modern 1.21 MCC-style Survival Games / Hunger Games (id: hungergames).
 */
public final class SurvivalGamesGame implements McmGame, Listener {
    private enum Phase { IDLE, BUILDING, COUNTDOWN, GRACE, ACTIVE, DEATHMATCH }

    private static final int INFINITE_XP_LEVEL = 30;
    private static final Set<Material> PLACEABLE_BLOCKS = Set.of(
            Material.OAK_PLANKS, Material.COBBLESTONE, Material.COBWEB, Material.FIRE
    );

    private final SurvivalGamesPlugin plugin;
    private final ArenaBuilder arenaBuilder = new ArenaBuilder();

    private volatile boolean running;
    private volatile Phase phase = Phase.IDLE;
    private final AtomicBoolean finished = new AtomicBoolean(false);

    private McmGameContext ctx;
    private World world;
    private SgMap activeMap;
    private String forcedMap;
    private BukkitTask mainTask;

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
        return SgMapRegistry.ids();
    }

    @Override
    public boolean forceNextMap(String id) {
        if (id == null) return false;
        String lower = id.toLowerCase(Locale.ROOT);
        if (!SgMapRegistry.ids().contains(lower)) return false;
        forcedMap = lower;
        return true;
    }

    @Override
    public String currentMap() {
        return (running && activeMap != null) ? activeMap.id() : null;
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
        this.ctx = ctx;
        this.world = plugin.getGameWorld();
        this.running = true;
        this.finished.set(false);
        this.phase = Phase.BUILDING;
        this.alive.clear();
        this.sideOfPlayer.clear();
        this.eliminationOrder.clear();
        this.activeAirdrops.clear();
        this.playerPlacedBlocks.clear();
        this.pendingAirdropLoc = null;
        this.pendingAirdropCountdown = 0;
        this.chestsRefilled = false;
        this.deathmatchStarted = false;

        List<SgMap> allMaps = SgMapRegistry.all();
        if (forcedMap != null) {
            this.activeMap = SgMapRegistry.byId(forcedMap);
            this.forcedMap = null;
        } else {
            this.activeMap = allMaps.get(ThreadLocalRandom.current().nextInt(allMaps.size()));
        }

        for (Map.Entry<String, List<Player>> entry : ctx.sides().entrySet()) {
            for (Player p : entry.getValue()) {
                alive.add(p.getUniqueId());
                sideOfPlayer.put(p.getUniqueId(), entry.getKey());
            }
        }

        WorldBorder border = world.getWorldBorder();
        int initialRadius = plugin.getConfig().getInt("border.initial-radius", activeMap.radius());
        border.setCenter(0.5, 0.5);
        border.setSize(initialRadius * 2.0);
        border.setDamageBuffer(1.0);
        border.setDamageAmount(1.5);

        ctx.broadcast(ChatColor.GOLD + "Generating " + ChatColor.YELLOW + activeMap.displayName()
                + ChatColor.GOLD + " (" + activeMap.id() + ")...");
        arenaBuilder.buildAsync(plugin, world, activeMap.id(), this::onArenaBuilt);
    }

    private void onArenaBuilt() {
        if (!running || ctx == null) return;

        List<Location> pedestals = arenaBuilder.getPedestals();
        List<Player> players = new ArrayList<>(ctx.participants());
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Location spawn = pedestals.get(i % pedestals.size()).clone();
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
        if (activeMap != null && activeMap.boatRecommended()) {
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
            arenaBuilder.refillChests(world);
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

    private void announceAirdrop() {
        double maxR = Math.max(18.0, (world.getWorldBorder().getSize() / 2.0) - 10.0);
        List<SgCanvas.AirdropSpot> candidates = new ArrayList<>();
        for (SgCanvas.AirdropSpot spot : arenaBuilder.getAirdrops()) {
            if (Math.hypot(spot.x(), spot.z()) <= maxR) {
                candidates.add(spot);
            }
        }

        int x, z;
        if (!candidates.isEmpty()) {
            SgCanvas.AirdropSpot chosen = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            x = chosen.x();
            z = chosen.z();
        } else {
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0;
            double dist = 10.0 + ThreadLocalRandom.current().nextDouble() * Math.max(5.0, maxR - 10.0);
            x = (int) Math.round(Math.cos(angle) * dist);
            z = (int) Math.round(Math.sin(angle) * dist);
        }

        int topY = arenaBuilder.findSafeTopY(world, x, z);
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
        List<Location> pedestals = arenaBuilder.getPedestals();
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
        world.getWorldBorder().setSize(dmDiameter, 20);
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
        lines.add(ChatColor.YELLOW + "Map: " + ChatColor.WHITE + (activeMap != null ? activeMap.displayName() : "-"));
        lines.add(ChatColor.YELLOW + "Phase: " + ChatColor.WHITE + phase.name());
        lines.add(ChatColor.YELLOW + "Alive: " + ChatColor.GREEN + alive.size());
        lines.add(ChatColor.YELLOW + "Border: " + ChatColor.AQUA + (int) (world.getWorldBorder().getSize() / 2.0) + "m");
        ctx.setSidebarLines(lines);
    }

    private void handleElimination(UUID victimId, Player killer) {
        if (!alive.remove(victimId)) return;

        int killPts = plugin.getConfig().getInt("scoring.kill-points", 10);
        int survPts = plugin.getConfig().getInt("scoring.survival-per-elimination", 1);

        if (killer != null && !killer.getUniqueId().equals(victimId)) {
            ctx.addPoints(killer.getUniqueId(), killPts, "Elimination");
        }
        for (UUID survivor : alive) {
            ctx.addPoints(survivor, survPts, "Survived elimination");
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

        int[] bonuses = {
                plugin.getConfig().getInt("scoring.placement-1st", 40),
                plugin.getConfig().getInt("scoring.placement-2nd", 30),
                plugin.getConfig().getInt("scoring.placement-3rd", 22),
                plugin.getConfig().getInt("scoring.placement-4th", 16),
                plugin.getConfig().getInt("scoring.placement-5th", 12)
        };
        for (int i = 0; i < ranked.size() && i < bonuses.length; i++) {
            if (bonuses[i] > 0) {
                ctx.addSidePoints(ranked.get(i), bonuses[i], "Team Placement #" + (i + 1));
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
        arenaBuilder.cancel();
        ctx.finish(reason);
    }

    private void cleanupWorldState() {
        for (Location loc : playerPlacedBlocks) {
            world.getBlockAt(loc).setType(Material.AIR, false);
        }
        playerPlacedBlocks.clear();
        for (Player p : ctx.participants()) {
            p.setLevel(0);
            p.setExp(0f);
        }
        if (world != null) {
            for (Entity e : world.getEntities()) {
                if (!(e instanceof Player)) {
                    e.remove();
                }
            }
            world.getWorldBorder().reset();
        }
    }

    @Override
    public void stop() {
        running = false;
        phase = Phase.IDLE;
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        arenaBuilder.cancel();
        alive.clear();
        sideOfPlayer.clear();
        eliminationOrder.clear();
        activeAirdrops.clear();
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
        if (phase == Phase.COUNTDOWN && e.getTo() != null) {
            if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ()) {
                Location back = e.getFrom().clone();
                back.setYaw(e.getTo().getYaw());
                back.setPitch(e.getTo().getPitch());
                e.setTo(back);
            }
        } else if (e.getPlayer().getGameMode() == GameMode.SPECTATOR && e.getTo() != null) {
            int maxR = (activeMap != null ? activeMap.radius() : ArenaBuilder.DEFAULT_RADIUS) + 12;
            if (Math.hypot(e.getTo().getX(), e.getTo().getZ()) > maxR) {
                e.setTo(new Location(world, 0.5, ArenaBuilder.BASE_Y + 14.0, 0.5));
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
        e.setRespawnLocation(new Location(world, 0.5, ArenaBuilder.BASE_Y + 14.0, 0.5));
        Bukkit.getScheduler().runTask(plugin, () -> e.getPlayer().setGameMode(GameMode.SPECTATOR));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        handleElimination(e.getPlayer().getUniqueId(), null);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!isInGameWorld(e.getPlayer())) return;
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        Location loc = e.getClickedBlock().getLocation();
        if (activeAirdrops.remove(loc)) {
            world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.2f);
            String side = sideOfPlayer.get(e.getPlayer().getUniqueId());
            if (side != null) {
                int pts = plugin.getConfig().getInt("scoring.airdrop-team-points", 24);
                ctx.addSidePoints(side, pts, "Supply Airdrop");
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
}
