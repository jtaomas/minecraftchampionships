package com.mcm.games.rocketspleef;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rocket Spleef Rush: three stacked floating floors, rocket launchers that blast holes in them,
 * two updrafts (double-jump) per round, and the void below. Last side standing wins the round.
 */
public final class RocketSpleefGame implements McmGame, Listener {

    // ---- tuning ----
    static final String ID = "rocketspleef";
    static final String WORLD_NAME = "mcc_" + ID;
    static final int ROUNDS = 3;
    static final int COUNTDOWN_SECONDS = 5;
    static final int ACTIVE_SECONDS = 120;
    static final int OVERTIME_SECONDS = 45;
    static final int INTERMISSION_SECONDS = 7;
    static final int[] FLOOR_Y = {100, 88, 76};
    static final int ELIM_Y = 68;
    static final int CLEAR_RADIUS = 40;

    static final Material LAUNCHER = Material.BLAZE_ROD;
    static final Material UPDRAFT = Material.FEATHER;
    static final int ROCKET_COOLDOWN_TICKS = 18;
    static final double ROCKET_SPEED = 1.8;
    static final int ROCKET_LIFE_TICKS = 70;
    static final double BLAST_RADIUS = 2.7;
    static final double KNOCKBACK_RADIUS = 4.5;
    static final int UPDRAFTS_PER_ROUND = 2;
    static final long UPDRAFT_COOLDOWN_MS = 1200;
    static final long KO_CREDIT_MS = 10_000;

    // ---- scoring ----
    /** Survival points shared out per round: each elimination gives every survivor POOL / (startCount - 1). */
    static final double SURVIVAL_POOL = 18.0;
    static final int KO_POINTS = 5;
    static final int LAST_SIDE_BONUS = 12;
    static final int SHARED_SURVIVOR_BONUS = 6;
    static final int[] PLACEMENT_BONUS = {5, 3, 2};

    static final Material[][] THEMES = {
            {Material.WHITE_CONCRETE, Material.LIGHT_BLUE_CONCRETE, Material.BLUE_CONCRETE},
            {Material.LIME_CONCRETE, Material.YELLOW_CONCRETE, Material.GREEN_CONCRETE},
            {Material.PINK_CONCRETE, Material.MAGENTA_CONCRETE, Material.PURPLE_CONCRETE},
    };

    enum Phase { IDLE, COUNTDOWN, ACTIVE, OVERTIME, INTERMISSION }

    private record Hit(UUID shooter, long time) {}

    private record Rocket(UUID shooter, Snowball entity, int bornTick) {}

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;

    private McmGameContext ctx;
    private World world;
    private BukkitTask task;
    private boolean running;
    private boolean finished;

    private Phase phase = Phase.IDLE;
    private int round;
    private int phaseTicks;
    private int tickCount;
    private int[] floorRadius = {18, 20, 22};
    private double decayRadius;

    private final Set<UUID> players = new LinkedHashSet<>();
    private final Map<UUID, String> sideOf = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Set<UUID> quitThisRound = new HashSet<>();
    private final Map<UUID, Integer> updrafts = new HashMap<>();
    private final Map<UUID, Long> updraftUsed = new HashMap<>();
    private final Map<UUID, Hit> lastHit = new HashMap<>();
    private final Map<UUID, Double> survivalAcc = new HashMap<>();
    private final Map<UUID, Location> spawnOf = new HashMap<>();
    private final Map<UUID, Integer> totalKos = new HashMap<>();
    private final List<UUID> elimOrder = new ArrayList<>();
    private final Map<UUID, Rocket> rockets = new LinkedHashMap<>();
    private int roundStartCount;
    private int initialSides;

    public RocketSpleefGame(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    // =====================================================================================
    // McmGame
    // =====================================================================================

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Rocket Spleef Rush"; }
    @Override public Material icon() { return Material.FIREWORK_ROCKET; }
    @Override public String description() { return "Blast the floors out from under everyone. Last side standing wins."; }
    @Override public boolean useCoreScoreboard() { return true; }

    @Override
    public void start(McmGameContext ctx) {
        if (running) stop();
        this.ctx = ctx;
        this.finished = false;
        resetState();

        ensureWorld();
        if (world == null) {
            ctx.broadcast("§cRocket Spleef Rush could not create its world.");
            finishOnce("World could not be created");
            return;
        }

        Map<String, List<Player>> sides = ctx.sides();
        for (Map.Entry<String, List<Player>> e : sides.entrySet()) {
            for (Player p : e.getValue()) {
                sideOf.put(p.getUniqueId(), e.getKey());
            }
        }
        for (Player p : new ArrayList<>(ctx.participants())) {
            players.add(p.getUniqueId());
            names.put(p.getUniqueId(), p.getName());
            sideOf.putIfAbsent(p.getUniqueId(), "SOLO:" + p.getUniqueId());
        }
        if (players.isEmpty()) {
            finishOnce("No players");
            return;
        }

        int n = players.size();
        int r = Math.max(14, Math.min(32, 14 + (int) Math.ceil(n * 0.45)));
        floorRadius = new int[]{r, r + 2, r + 4};

        loadChunks();
        running = true;
        ctx.broadcast("§6§lRocket Spleef Rush §7- blast the floor from under your rivals! "
                + "§eRight-click §7the launcher to fire, §edouble-jump §7(or use the feather) for an updraft.");
        startRound();
        if (running) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
        }
    }

    @Override
    public void stop() {
        boolean wasRunning = running;
        running = false;
        phase = Phase.IDLE;
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeRockets();
        if (wasRunning) {
            resetPlayers(); // only while we own the players; after finish the core has them
        }
        if (world != null) {
            clearArena();
            world.removePluginChunkTickets(plugin);
        }
        alive.clear();
    }

    @Override
    public boolean onLateJoin(Player player, McmGameContext ctx) {
        if (!running || world == null) return false;
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(spectatorPoint());
        player.sendMessage("§7Rocket Spleef Rush is in progress - you are spectating.");
        return true;
    }

    // =====================================================================================
    // World / arena
    // =====================================================================================

    private void ensureWorld() {
        world = Bukkit.getWorld(WORLD_NAME);
        if (world == null) {
            WorldCreator wc = new WorldCreator(WORLD_NAME)
                    .environment(World.Environment.NORMAL)
                    .generator(new VoidGenerator())
                    .generateStructures(false);
            world = wc.createWorld();
        }
        if (world == null) return;
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.FALL_DAMAGE, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(6000);
        world.setStorm(false);
        world.setThundering(false);
        world.setSpawnLocation(0, FLOOR_Y[0] + 15, 0);
    }

    private void loadChunks() {
        int min = Math.floorDiv(-CLEAR_RADIUS, 16);
        int max = Math.floorDiv(CLEAR_RADIUS, 16);
        for (int cx = min; cx <= max; cx++) {
            for (int cz = min; cz <= max; cz++) {
                world.addPluginChunkTicket(cx, cz, plugin);
            }
        }
    }

    private void clearArena() {
        for (int y : FLOOR_Y) {
            for (int x = -CLEAR_RADIUS; x <= CLEAR_RADIUS; x++) {
                for (int z = -CLEAR_RADIUS; z <= CLEAR_RADIUS; z++) {
                    Block b = world.getBlockAt(x, y, z);
                    if (!b.getType().isAir()) b.setType(Material.AIR, false);
                }
            }
        }
    }

    private void buildArena() {
        clearArena();
        for (int i = 0; i < FLOOR_Y.length; i++) {
            int y = FLOOR_Y[i];
            int r = floorRadius[i];
            Material[] theme = THEMES[i];
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d > r + 0.3) continue;
                    Material m;
                    if (d >= r - 0.7) m = theme[2];
                    else if (d < 1.5) m = Material.SEA_LANTERN;
                    else m = ((int) (d / 3)) % 2 == 0 ? theme[0] : theme[1];
                    world.getBlockAt(x, y, z).setType(m, false);
                }
            }
        }
    }

    private boolean isFloorBlock(Block b) {
        if (b.getType().isAir() || b.getType() == Material.BARRIER) return false;
        int y = b.getY();
        for (int fy : FLOOR_Y) if (fy == y) return true;
        return false;
    }

    private Location spectatorPoint() {
        int r = floorRadius[0];
        return new Location(world, 0.5, FLOOR_Y[0] + 14, -(r + 12) + 0.5, 0f, 30f);
    }

    // =====================================================================================
    // Rounds
    // =====================================================================================

    private void resetState() {
        players.clear();
        sideOf.clear();
        names.clear();
        alive.clear();
        quitThisRound.clear();
        updrafts.clear();
        updraftUsed.clear();
        lastHit.clear();
        survivalAcc.clear();
        spawnOf.clear();
        totalKos.clear();
        elimOrder.clear();
        rockets.clear();
        round = 0;
        phaseTicks = 0;
        tickCount = 0;
        phase = Phase.IDLE;
    }

    private List<Player> onlineParticipants() {
        List<Player> list = new ArrayList<>();
        if (ctx == null) return list;
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (p != null && p.isOnline() && players.contains(p.getUniqueId())) list.add(p);
        }
        return list;
    }

    private void startRound() {
        round++;
        removeRockets();
        buildArena();
        alive.clear();
        quitThisRound.clear();
        lastHit.clear();
        elimOrder.clear();
        survivalAcc.clear();
        spawnOf.clear();
        updrafts.clear();
        updraftUsed.clear();

        List<Player> online = onlineParticipants();
        if (online.isEmpty()) {
            endGame("Everyone left");
            return;
        }

        // group by side, keeping a stable order
        Map<String, List<Player>> bySide = new LinkedHashMap<>();
        for (Player p : online) {
            bySide.computeIfAbsent(sideOf.get(p.getUniqueId()), k -> new ArrayList<>()).add(p);
        }
        List<String> sideKeys = new ArrayList<>(bySide.keySet());
        Collections.shuffle(sideKeys);
        initialSides = sideKeys.size();

        int top = FLOOR_Y[0];
        double padRadius = floorRadius[0] * 0.62;
        int[][] offsets = {{0, 0}, {1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}};
        double angleOffset = ThreadLocalRandom.current().nextDouble(Math.PI * 2);
        for (int i = 0; i < sideKeys.size(); i++) {
            String key = sideKeys.get(i);
            double ang = angleOffset + Math.PI * 2 * i / sideKeys.size();
            int px = (int) Math.round(Math.cos(ang) * padRadius);
            int pz = (int) Math.round(Math.sin(ang) * padRadius);
            Material pad = padMaterial(key);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    world.getBlockAt(px + dx, top, pz + dz).setType(pad, false);
                }
            }
            float yaw = (float) Math.toDegrees(Math.atan2(px, -pz)); // face the centre
            List<Player> members = bySide.get(key);
            for (int m = 0; m < members.size(); m++) {
                int[] off = offsets[m % offsets.length];
                Location loc = new Location(world, px + off[0] + 0.5, top + 1, pz + off[1] + 0.5, yaw, 0f);
                Player p = members.get(m);
                spawnOf.put(p.getUniqueId(), loc);
                preparePlayer(p);
                p.teleport(loc);
                alive.add(p.getUniqueId());
                updrafts.put(p.getUniqueId(), UPDRAFTS_PER_ROUND);
                giveItems(p);
            }
        }
        roundStartCount = alive.size();
        decayRadius = floorRadius[FLOOR_Y.length - 1] + 1;
        phase = Phase.COUNTDOWN;
        phaseTicks = 0;

        for (Player p : online) {
            title(p, "§6Round " + round + "§7/§6" + ROUNDS, "§eGet ready...", 5, 40, 10);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
        }
        ctx.broadcast("§6Round " + round + "/" + ROUNDS + " §7- " + roundStartCount + " players, "
                + initialSides + " sides. Floors regenerated.");
        updateHud();
    }

    private Material padMaterial(String sideKey) {
        if (sideKey == null || !sideKey.startsWith("TEAM:")) return Material.ORANGE_GLAZED_TERRACOTTA;
        String team = sideKey.substring(5).toLowerCase();
        return switch (team) {
            case "red" -> Material.RED_WOOL;
            case "orange" -> Material.ORANGE_WOOL;
            case "yellow" -> Material.YELLOW_WOOL;
            case "lime" -> Material.LIME_WOOL;
            case "green" -> Material.GREEN_WOOL;
            case "cyan" -> Material.CYAN_WOOL;
            case "aqua" -> Material.LIGHT_BLUE_WOOL;
            case "blue" -> Material.BLUE_WOOL;
            case "purple" -> Material.PURPLE_WOOL;
            case "pink" -> Material.PINK_WOOL;
            default -> Material.WHITE_WOOL;
        };
    }

    private void preparePlayer(Player p) {
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
        AttributeInstance maxHp = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHp != null) p.setHealth(maxHp.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.setVelocity(new Vector());
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setCooldown(LAUNCHER, 0);
    }

    private void giveItems(Player p) {
        ItemStack launcher = new ItemStack(LAUNCHER);
        ItemMeta lm = launcher.getItemMeta();
        lm.displayName(text("§6§lRocket Launcher §7(Right-click)"));
        lm.lore(List.of(text("§7Fires a rocket that blasts"), text("§7a hole in the floor and"), text("§7knocks players back.")));
        lm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        launcher.setItemMeta(lm);
        p.getInventory().setItem(0, launcher);
        updateFeathers(p);
        p.getInventory().setHeldItemSlot(0);
    }

    private void updateFeathers(Player p) {
        int c = updrafts.getOrDefault(p.getUniqueId(), 0);
        if (c <= 0) {
            p.getInventory().setItem(1, null);
            return;
        }
        ItemStack f = new ItemStack(UPDRAFT, c);
        ItemMeta fm = f.getItemMeta();
        fm.displayName(text("§b§lUpdraft §7(Right-click or double-jump)"));
        fm.lore(List.of(text("§7Launches you upwards."), text("§7Limited uses per round.")));
        f.setItemMeta(fm);
        p.getInventory().setItem(1, f);
    }

    private void tick() {
        if (!running || ctx == null || world == null) return;
        tickCount++;
        phaseTicks++;
        pruneOffline();
        if (!running) return;

        switch (phase) {
            case COUNTDOWN -> {
                for (UUID id : new ArrayList<>(alive)) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null && p.getWorld() == world && p.getLocation().getY() < FLOOR_Y[0] - 2) {
                        Location s = spawnOf.get(id);
                        if (s != null) p.teleport(s);
                    }
                }
                if (phaseTicks % 20 == 0) {
                    int left = FastTest.secs(COUNTDOWN_SECONDS) - phaseTicks / 20;
                    if (left > 0) {
                        for (Player p : onlineParticipants()) {
                            title(p, "§e" + left, "§7Rockets unlock soon", 0, 25, 0);
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.2f);
                        }
                    }
                }
                if (phaseTicks >= FastTest.secs(COUNTDOWN_SECONDS) * 20) {
                    phase = Phase.ACTIVE;
                    phaseTicks = 0;
                    for (Player p : onlineParticipants()) {
                        title(p, "§a§lGO!", "§7Blast them into the void!", 0, 20, 10);
                        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
                    }
                }
            }
            case ACTIVE, OVERTIME -> {
                tickRockets();
                voidCheck();
                updateFlight();
                if (phase == Phase.OVERTIME && phaseTicks % 20 == 0) decayStep();
                if (checkRoundEnd()) return;
                if (phase == Phase.ACTIVE && phaseTicks >= ACTIVE_SECONDS * 20) {
                    phase = Phase.OVERTIME;
                    phaseTicks = 0;
                    ctx.broadcast("§c§lOVERTIME! §7The floors are crumbling from the edges!");
                    for (Player p : onlineParticipants()) {
                        title(p, "§c§lOvertime", "§7The floors are crumbling!", 5, 40, 10);
                        p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.4f);
                    }
                } else if (phase == Phase.OVERTIME && phaseTicks >= OVERTIME_SECONDS * 20) {
                    endRound();
                    return;
                }
            }
            case INTERMISSION -> {
                if (phaseTicks >= FastTest.secs(INTERMISSION_SECONDS) * 20) {
                    if (round < ROUNDS) startRound();
                    else endGame("Rocket Spleef Rush complete");
                    return;
                }
            }
            default -> { }
        }
        if (!running) return;
        if (tickCount % 20 == 0) updateHud();
        if (tickCount % 10 == 0 && (phase == Phase.ACTIVE || phase == Phase.OVERTIME || phase == Phase.COUNTDOWN)) {
            actionBars();
        }
    }

    private void pruneOffline() {
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) {
                eliminate(id, true);
            }
        }
    }

    private void voidCheck() {
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            if (p.getWorld() != world) {
                eliminate(id, true);
            } else if (p.getLocation().getY() < ELIM_Y) {
                eliminate(id, false);
            }
        }
    }

    private void updateFlight() {
        long now = System.currentTimeMillis();
        for (UUID id : alive) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || p.getGameMode() != GameMode.ADVENTURE) continue;
            boolean can = updrafts.getOrDefault(id, 0) > 0
                    && now - updraftUsed.getOrDefault(id, 0L) >= UPDRAFT_COOLDOWN_MS;
            if (p.getAllowFlight() != can) {
                p.setAllowFlight(can);
                if (!can) p.setFlying(false);
            }
        }
    }

    private int aliveSideCount() {
        Set<String> s = new HashSet<>();
        for (UUID id : alive) s.add(sideOf.get(id));
        return s.size();
    }

    private boolean checkRoundEnd() {
        if (phase != Phase.ACTIVE && phase != Phase.OVERTIME) return false;
        if (alive.isEmpty() || (initialSides >= 2 && aliveSideCount() <= 1)) {
            endRound();
            return true;
        }
        return false;
    }

    private void eliminate(UUID id, boolean quit) {
        if (!alive.remove(id)) return;
        String name = nameOf(id);
        Player victim = Bukkit.getPlayer(id);
        if (quit) {
            quitThisRound.add(id);
            if (ctx != null) ctx.broadcast("§7" + name + " left the arena.");
        } else {
            elimOrder.add(id);
            UUID killer = null;
            Hit h = lastHit.get(id);
            if (h != null && System.currentTimeMillis() - h.time() <= KO_CREDIT_MS
                    && !h.shooter().equals(id)
                    && !Objects.equals(sideOf.get(h.shooter()), sideOf.get(id))) {
                killer = h.shooter();
            }
            Player kp = killer == null ? null : Bukkit.getPlayer(killer);
            if (kp != null && kp.isOnline() && players.contains(killer)) {
                ctx.addPoints(killer, KO_POINTS, "Blasted " + name + " into the void");
                totalKos.merge(killer, 1, Integer::sum);
                ctx.broadcast("§c☄ " + colored(id) + " §7was blasted into the void by " + colored(killer));
                kp.playSound(kp.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.2f);
                actionBar(kp, "§a+" + KO_POINTS + " §7Knocked off §f" + name);
            } else {
                ctx.broadcast("§c☄ " + colored(id) + " §7fell into the void.");
            }

            // survival points for everyone still standing
            double share = SURVIVAL_POOL / Math.max(1, roundStartCount - 1);
            for (UUID a : new ArrayList<>(alive)) {
                Player ap = Bukkit.getPlayer(a);
                if (ap == null) continue;
                double acc = survivalAcc.getOrDefault(a, 0.0) + share;
                int whole = (int) Math.floor(acc);
                if (whole > 0) {
                    ctx.addPoints(a, whole, "Outlived " + name);
                    acc -= whole;
                }
                survivalAcc.put(a, acc);
            }
            if (alive.size() > 0 && alive.size() <= 3) {
                ctx.broadcast("§e" + alive.size() + " player" + (alive.size() == 1 ? "" : "s") + " remaining!");
            }
        }
        lastHit.remove(id);
        if (victim != null && victim.isOnline()) {
            victim.getInventory().clear();
            victim.setAllowFlight(false);
            victim.setFlying(false);
            victim.setVelocity(new Vector());
            victim.setFallDistance(0f);
            if (!quit && world != null) {
                victim.setGameMode(GameMode.SPECTATOR);
                victim.teleport(spectatorPoint());
                title(victim, "§c§lEliminated", "§7You'll be back next round", 5, 40, 10);
                victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.8f);
            }
        }
    }

    private void endRound() {
        phase = Phase.INTERMISSION;
        phaseTicks = 0;
        removeRockets();

        // flush fractional survival points
        for (Map.Entry<UUID, Double> e : survivalAcc.entrySet()) {
            if (e.getValue() >= 0.5 && isOnline(e.getKey())) {
                ctx.addPoints(e.getKey(), 1, "Survival");
            }
        }
        survivalAcc.clear();

        List<UUID> survivors = new ArrayList<>();
        for (UUID id : alive) if (isOnline(id)) survivors.add(id);
        Set<String> survivorSides = new LinkedHashSet<>();
        for (UUID id : survivors) survivorSides.add(sideOf.get(id));

        String subtitle;
        if (survivors.isEmpty()) {
            subtitle = "§7Nobody survived!";
            ctx.broadcast("§6Round " + round + " over §7- nobody survived the void.");
        } else if (survivorSides.size() == 1) {
            String side = survivorSides.iterator().next();
            String label = ctx.sideLabel(side);
            subtitle = label + " §7is the last side standing!";
            ctx.broadcast("§6Round " + round + " over §7- " + label + " §7is the last side standing! §a(+"
                    + LAST_SIDE_BONUS + " each)");
            for (UUID id : survivors) ctx.addPoints(id, LAST_SIDE_BONUS, "Last side standing");
        } else {
            subtitle = "§7" + survivors.size() + " players from " + survivorSides.size() + " sides survived";
            ctx.broadcast("§6Round " + round + " over §7- time! " + survivors.size() + " survivors from "
                    + survivorSides.size() + " sides. §a(+" + SHARED_SURVIVOR_BONUS + " each)");
            for (UUID id : survivors) ctx.addPoints(id, SHARED_SURVIVOR_BONUS, "Survived the round");
        }

        // placement bonus for the last ones to fall
        int place = 0;
        for (int i = elimOrder.size() - 1; i >= 0 && place < PLACEMENT_BONUS.length; i--) {
            UUID id = elimOrder.get(i);
            if (!isOnline(id)) continue;
            ctx.addPoints(id, PLACEMENT_BONUS[place], "Late elimination");
            place++;
        }

        for (Player p : onlineParticipants()) {
            title(p, "§6Round " + round + " over", subtitle, 5, 60, 15);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1f);
            if (alive.contains(p.getUniqueId())) {
                p.getInventory().clear();
                p.setAllowFlight(true); // keep survivors from dropping into the void during the break
                p.setFlying(true);
            }
        }
        if (round >= ROUNDS) {
            ctx.broadcast("§7Final results in a few seconds...");
        }
        updateHud();
    }

    // ------------------------------------------------------------------ admin testing hooks
    // (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    /** Ends the round countdown (rockets unlock now) or the intermission (next round / final results now). */
    public boolean skipWait() {
        if (!running || ctx == null) return false;
        if (phase == Phase.COUNTDOWN) {
            phaseTicks = FastTest.secs(COUNTDOWN_SECONDS) * 20 - 1;   // the next tick runs the normal GO transition
            return true;
        }
        if (phase == Phase.INTERMISSION) {
            phaseTicks = FastTest.secs(INTERMISSION_SECONDS) * 20 - 1;
            return true;
        }
        return false;
    }

    /** Ends the current round now without round-end points, then the next round starts (or the game ends). */
    public boolean skipRound() {
        if (!running || ctx == null) return false;
        if (phase == Phase.COUNTDOWN || phase == Phase.ACTIVE || phase == Phase.OVERTIME) {
            removeRockets();
            survivalAcc.clear();   // unflushed survival fractions of the skipped round are dropped
            ctx.broadcast("§7Round " + round + " was skipped by an admin (no round-end points).");
            phase = Phase.INTERMISSION;
        } else if (phase != Phase.INTERMISSION) {
            return false;
        }
        phaseTicks = FastTest.secs(INTERMISSION_SECONDS) * 20 - 1;   // next tick: startRound() or endGame(), as normal
        return true;
    }

    private void endGame(String reason) {
        if (ctx != null && !totalKos.isEmpty()) {
            List<Map.Entry<UUID, Integer>> list = new ArrayList<>(totalKos.entrySet());
            list.sort((a, b) -> b.getValue() - a.getValue());
            StringBuilder sb = new StringBuilder("§6Top blasters: ");
            for (int i = 0; i < Math.min(3, list.size()); i++) {
                if (i > 0) sb.append("§7, ");
                sb.append(colored(list.get(i).getKey())).append(" §7(").append(list.get(i).getValue()).append(")");
            }
            ctx.broadcast(sb.toString());
        }
        running = false;
        phase = Phase.IDLE;
        if (task != null) {
            task.cancel();
            task = null;
        }
        removeRockets();
        resetPlayers();
        if (world != null) {
            clearArena();
            world.removePluginChunkTickets(plugin);
        }
        alive.clear();
        finishOnce(reason);
    }

    private void finishOnce(String reason) {
        if (finished || ctx == null) return;
        finished = true;
        running = false;
        McmGameContext c = ctx;
        c.finish(reason);
    }

    private void resetPlayers() {
        if (ctx == null) return;
        List<Player> list;
        try {
            list = new ArrayList<>(ctx.participants());
        } catch (Throwable t) {
            return;
        }
        for (Player p : list) {
            if (p == null || !p.isOnline()) continue;
            p.getInventory().clear();
            p.setCooldown(LAUNCHER, 0);
            p.setCooldown(UPDRAFT, 0);
            if (p.getGameMode() != GameMode.CREATIVE && p.getGameMode() != GameMode.SPECTATOR) {
                p.setFlying(false);
                p.setAllowFlight(false);
            }
            p.setFallDistance(0f);
        }
    }

    // =====================================================================================
    // Overtime decay
    // =====================================================================================

    private void decayStep() {
        double maxR = floorRadius[FLOOR_Y.length - 1] + 1;
        double step = maxR / OVERTIME_SECONDS;
        decayRadius = Math.max(0, decayRadius - step);
        int lim = (int) Math.ceil(maxR) + 1;
        boolean anyParticles = false;
        for (int y : FLOOR_Y) {
            for (int x = -lim; x <= lim; x++) {
                for (int z = -lim; z <= lim; z++) {
                    double d = Math.sqrt(x * x + z * z);
                    if (d <= decayRadius - 2) continue;
                    Block b = world.getBlockAt(x, y, z);
                    if (b.getType().isAir()) continue;
                    if (d > decayRadius) {
                        BlockData data = b.getBlockData();
                        b.setType(Material.AIR, false);
                        if (ThreadLocalRandom.current().nextInt(6) == 0) {
                            world.spawnParticle(Particle.BLOCK, x + 0.5, y + 0.5, z + 0.5, 6, 0.3, 0.3, 0.3, data);
                            anyParticles = true;
                        }
                    } else if (b.getType() != Material.RED_CONCRETE) {
                        b.setType(Material.RED_CONCRETE, false);
                    }
                }
            }
        }
        if (anyParticles) {
            for (Player p : onlineParticipants()) {
                p.playSound(p.getLocation(), Sound.BLOCK_STONE_BREAK, 0.4f, 0.7f);
            }
        }
    }

    // =====================================================================================
    // Rockets
    // =====================================================================================

    private void fireRocket(Player p) {
        Vector dir = p.getEyeLocation().getDirection().normalize().multiply(ROCKET_SPEED);
        Snowball sb = p.launchProjectile(Snowball.class, dir);
        sb.setGravity(false);
        sb.setItem(new ItemStack(Material.FIREWORK_ROCKET));
        sb.setSilent(true);
        rockets.put(sb.getUniqueId(), new Rocket(p.getUniqueId(), sb, tickCount));
        world.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 0.8f);
    }

    private void tickRockets() {
        Iterator<Map.Entry<UUID, Rocket>> it = rockets.entrySet().iterator();
        while (it.hasNext()) {
            Rocket r = it.next().getValue();
            Snowball sb = r.entity();
            if (sb == null || !sb.isValid() || sb.isDead()) {
                it.remove();
                continue;
            }
            Location l = sb.getLocation();
            if (tickCount - r.bornTick() > ROCKET_LIFE_TICKS || l.getY() < ELIM_Y - 30) {
                sb.remove();
                it.remove();
                continue;
            }
            world.spawnParticle(Particle.FLAME, l, 2, 0.05, 0.05, 0.05, 0.01);
            world.spawnParticle(Particle.SMOKE, l, 1, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private void removeRockets() {
        for (Rocket r : rockets.values()) {
            if (r.entity() != null && r.entity().isValid()) r.entity().remove();
        }
        rockets.clear();
        if (world != null) {
            for (Snowball sb : world.getEntitiesByClass(Snowball.class)) sb.remove();
        }
    }

    private void explode(Location at, UUID shooter, Player direct) {
        if (world == null || at.getWorld() != world) return;
        int bx = at.getBlockX(), by = at.getBlockY(), bz = at.getBlockZ();
        int br = (int) Math.ceil(BLAST_RADIUS);
        double r2 = BLAST_RADIUS * BLAST_RADIUS;
        BlockData sample = null;
        for (int dx = -br; dx <= br; dx++) {
            for (int dy = -br; dy <= br; dy++) {
                for (int dz = -br; dz <= br; dz++) {
                    if (dx * dx + dy * dy + dz * dz > r2) continue;
                    Block b = world.getBlockAt(bx + dx, by + dy, bz + dz);
                    if (!isFloorBlock(b)) continue;
                    if (sample == null) sample = b.getBlockData();
                    b.setType(Material.AIR, false);
                }
            }
        }
        world.spawnParticle(Particle.EXPLOSION, at, 1);
        world.spawnParticle(Particle.FLAME, at, 15, 0.6, 0.6, 0.6, 0.05);
        if (sample != null) world.spawnParticle(Particle.BLOCK, at, 30, 1.2, 0.3, 1.2, sample);
        world.playSound(at, Sound.ENTITY_GENERIC_EXPLODE, 1.1f, 1.3f);

        Player shooterPlayer = Bukkit.getPlayer(shooter);
        long now = System.currentTimeMillis();
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || p.getWorld() != world) continue;
            boolean isDirect = p.equals(direct);
            Vector diff = p.getLocation().add(0, 0.9, 0).toVector().subtract(at.toVector());
            double d = diff.length();
            if (d > KNOCKBACK_RADIUS && !isDirect) continue;
            double f = isDirect ? 1.0 : Math.max(0, 1 - d / KNOCKBACK_RADIUS);
            Vector dir = d < 0.05 ? new Vector(0, 1, 0) : diff.multiply(1.0 / d);
            double strength = 0.55 + 1.3 * f + (isDirect ? 0.4 : 0);
            Vector kb = dir.multiply(strength);
            kb.setY(Math.max(kb.getY(), 0) + 0.3 + 0.35 * f);
            Vector v = p.getVelocity().multiply(0.25).add(kb);
            if (v.length() > 3.2) v.normalize().multiply(3.2);
            p.setVelocity(v);
            p.setFallDistance(0f);
            if (!id.equals(shooter)) {
                lastHit.put(id, new Hit(shooter, now));
                if (isDirect && shooterPlayer != null) {
                    actionBar(shooterPlayer, "§6Direct hit on §f" + p.getName() + "§6!");
                    shooterPlayer.playSound(shooterPlayer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.8f, 1.6f);
                }
            }
        }
    }

    private void useUpdraft(Player p) {
        if (phase != Phase.ACTIVE && phase != Phase.OVERTIME) return;
        UUID id = p.getUniqueId();
        int c = updrafts.getOrDefault(id, 0);
        if (c <= 0) {
            actionBar(p, "§cNo updrafts left this round!");
            return;
        }
        long now = System.currentTimeMillis();
        if (now - updraftUsed.getOrDefault(id, 0L) < UPDRAFT_COOLDOWN_MS) return;
        updrafts.put(id, c - 1);
        updraftUsed.put(id, now);
        Vector v = p.getLocation().getDirection().setY(0);
        if (v.lengthSquared() > 0.0001) v.normalize().multiply(0.45);
        v.setY(1.25);
        p.setVelocity(v);
        p.setFallDistance(0f);
        p.setFlying(false);
        p.setAllowFlight(false);
        world.spawnParticle(Particle.CLOUD, p.getLocation(), 25, 0.4, 0.1, 0.4, 0.05);
        world.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1.4f);
        updateFeathers(p);
    }

    // =====================================================================================
    // HUD
    // =====================================================================================

    private void updateHud() {
        if (ctx == null) return;
        int total = ACTIVE_SECONDS + OVERTIME_SECONDS;
        int left;
        String phaseLine;
        switch (phase) {
            case COUNTDOWN -> { left = total; phaseLine = "§7Starting..."; }
            case ACTIVE -> { left = total - phaseTicks / 20; phaseLine = "§aFight! §7Overtime in " + (ACTIVE_SECONDS - phaseTicks / 20) + "s"; }
            case OVERTIME -> { left = OVERTIME_SECONDS - phaseTicks / 20; phaseLine = "§c⚠ Floors crumbling!"; }
            case INTERMISSION -> {
                total = FastTest.secs(INTERMISSION_SECONDS);
                left = total - phaseTicks / 20;
                phaseLine = round >= ROUNDS ? "§7Final results..." : "§7Next round soon";
            }
            default -> { left = 0; phaseLine = ""; }
        }
        ctx.setTimer(Math.max(0, left), Math.max(1, total));
        List<String> lines = new ArrayList<>();
        lines.add("§fRound: §e" + Math.max(1, round) + "§7/" + ROUNDS);
        lines.add("§fAlive: §a" + alive.size() + "§7/" + roundStartCount);
        lines.add("§fSides alive: §b" + aliveSideCount() + "§7/" + initialSides);
        lines.add(phaseLine);
        UUID topKo = null;
        int best = 0;
        for (Map.Entry<UUID, Integer> e : totalKos.entrySet()) {
            if (e.getValue() > best) { best = e.getValue(); topKo = e.getKey(); }
        }
        if (topKo != null) lines.add("§fTop KOs: §6" + nameOf(topKo) + " §7(" + best + ")");
        ctx.setSidebarLines(lines);
    }

    private void actionBars() {
        for (UUID id : alive) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            int c = updrafts.getOrDefault(id, 0);
            StringBuilder ups = new StringBuilder();
            for (int i = 0; i < UPDRAFTS_PER_ROUND; i++) ups.append(i < c ? "§b●" : "§8○");
            String rocket = phase == Phase.COUNTDOWN ? "§7Locked"
                    : p.hasCooldown(LAUNCHER) ? "§cReloading" : "§aReady";
            actionBar(p, "§6Rocket: " + rocket + " §8| §bUpdrafts: " + ups + " §7(double-jump)");
        }
    }

    // =====================================================================================
    // Listeners
    // =====================================================================================

    private boolean inGame(Entity e) {
        return running && world != null && e != null && e.getWorld() == world;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!inGame(p)) return;
        if (e.getHand() != EquipmentSlot.HAND) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        if (item.getType() == LAUNCHER) {
            e.setCancelled(true);
            if (!alive.contains(p.getUniqueId())) return;
            if (phase != Phase.ACTIVE && phase != Phase.OVERTIME) {
                actionBar(p, "§cRockets unlock when the round starts!");
                return;
            }
            if (p.hasCooldown(LAUNCHER)) return;
            fireRocket(p);
            p.setCooldown(LAUNCHER, ROCKET_COOLDOWN_TICKS);
        } else if (item.getType() == UPDRAFT) {
            e.setCancelled(true);
            if (!alive.contains(p.getUniqueId())) return;
            useUpdraft(p);
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (!inGame(p) || !e.isFlying()) return;
        if (!alive.contains(p.getUniqueId())) return;
        if (phase != Phase.ACTIVE && phase != Phase.OVERTIME) return;
        if (p.getGameMode() != GameMode.ADVENTURE && p.getGameMode() != GameMode.SURVIVAL) return;
        e.setCancelled(true);
        p.setFlying(false);
        useUpdraft(p);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile pr = e.getEntity();
        Rocket r = rockets.remove(pr.getUniqueId());
        if (r == null) return;
        if (!inGame(pr)) {
            pr.remove();
            return;
        }
        Location at = pr.getLocation();
        Player direct = null;
        if (e.getHitEntity() instanceof Player hp && alive.contains(hp.getUniqueId())) direct = hp;
        pr.remove();
        explode(at, r.shooter(), direct);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && inGame(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (inGame(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (inGame(e.getPlayer()) && e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (inGame(e.getPlayer()) && e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (inGame(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (inGame(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!running) return;
        UUID id = e.getPlayer().getUniqueId();
        if (alive.contains(id)) {
            e.getPlayer().getInventory().clear();
            e.getPlayer().setAllowFlight(false);
            eliminate(id, true);
        }
    }

    // =====================================================================================
    // Helpers
    // =====================================================================================

    private boolean isOnline(UUID id) {
        Player p = Bukkit.getPlayer(id);
        return p != null && p.isOnline();
    }

    private String nameOf(UUID id) {
        String n = names.get(id);
        if (n != null) return n;
        OfflinePlayer op = Bukkit.getOfflinePlayer(id);
        return op.getName() == null ? "?" : op.getName();
    }

    private String colored(UUID id) {
        String team = ctx == null ? null : ctx.teamOf(id);
        String color = ctx == null ? "§f" : String.valueOf(ctx.teamColor(team));
        return color + nameOf(id);
    }

    private static Component text(String legacy) {
        return LEGACY.deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    private static void title(Player p, String title, String sub, int in, int stay, int out) {
        p.showTitle(Title.title(LEGACY.deserialize(title), LEGACY.deserialize(sub),
                Title.Times.times(Duration.ofMillis(in * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(out * 50L))));
    }

    private static void actionBar(Player p, String msg) {
        p.sendActionBar(LEGACY.deserialize(msg));
    }
}
