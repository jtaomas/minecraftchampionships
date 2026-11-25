package com.mcm.games.rocketspleef;

import com.mcm.api.McmApi;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.rocketspleef.map.ArenaMaps;
import com.mcm.games.rocketspleef.map.ImportedRsArena;
import com.mcm.games.rocketspleef.map.RocketArenaMap;
import com.mcm.games.rocketspleef.map.RsCanvas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Rocket Spleef Rush: floating void arenas, unbreakable Elytra, swept-raycast Rocket Launcher,
 * Updraft charges, outer-to-inner crumbling, mid-round sky-pads, and 10s knock-off kill credit.
 */
public final class RocketSpleefGame implements McmGame, Listener {

    static final String ID = "rocketspleef";
    static final String WORLD_NAME = "mcc_" + ID;

    enum Phase { IDLE, COUNTDOWN, ACTIVE, OVERTIME, INTERMISSION }

    private record Hit(UUID shooter, long timeMs) {}
    private record Rocket(UUID shooter, Snowball entity, Vector velocity, int bornTick) {}
    private record CrumbleWarning(Block block, int vanishTick) {}

    private final JavaPlugin plugin;
    private final RocketConfig cfg;
    private World world;

    private McmGameContext ctx;
    private Phase phase = Phase.IDLE;
    private int round = 0;
    private int phaseTicksLeft = 0;
    private int totalPhaseSeconds = 1;
    private int tickCounter = 0;
    private boolean finished = false;

    private RocketArenaMap currentArena;
    private String forcedNextMapId;
    private List<RsCanvas.Pos> activeBlocks = new ArrayList<>();
    private final List<RsCanvas.Pos> dynamicBlocks = new ArrayList<>();
    private int crumbleCursor = 0;
    private int padsSpawned = 0;
    private int activeElimY = 64;
    private int activeClearRadius = 46;
    private double spectatorY = 108.0;

    private final Set<UUID> alive = new HashSet<>();
    private final List<UUID> elimOrder = new ArrayList<>();
    private final Map<UUID, Hit> lastHit = new HashMap<>();
    private final Map<UUID, Integer> updrafts = new HashMap<>();
    private final Map<UUID, Long> lastUpdraftMs = new HashMap<>();
    private final Map<UUID, Long> lastRocketMs = new HashMap<>();
    private final Map<UUID, Double> survivalCarry = new HashMap<>();
    private final List<Rocket> rockets = new ArrayList<>();
    private final List<CrumbleWarning> pendingCrumbles = new ArrayList<>();

    private int startCount = 0;
    private boolean hasMultipleSides = false;
    private BukkitTask mainTask;

    public RocketSpleefGame(JavaPlugin plugin, RocketConfig cfg, World world) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.world = world;
    }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Rocket Spleef Rush"; }
    @Override public Material icon() { return Material.FIREWORK_ROCKET; }
    @Override public String description() { return "Elytra, rocket launchers, updrafts, and crumbling void arenas."; }

    @Override
    public List<String> maps() {
        return ArenaMaps.ids();
    }

    @Override
    public boolean forceNextMap(String id) {
        if (ArenaMaps.get(id) == null) return false;
        this.forcedNextMapId = id.toLowerCase();
        return true;
    }

    @Override
    public String currentMap() {
        return currentArena == null ? null : currentArena.id();
    }

    @Override
    public void start(McmGameContext ctx) {
        stop();
        if (this.world == null) {
            this.world = RocketSpleefPlugin.createVoidWorld(WORLD_NAME);
        }
        if (this.world == null) {
            ctx.broadcast("§cRocket Spleef Rush failed to load world.");
            ctx.finish("World load failure");
            return;
        }
        this.ctx = ctx;
        this.finished = false;
        this.round = 0;
        this.tickCounter = 0;
        startNextRound();
        this.mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    @Override
    public void stop() {
        if (mainTask != null) {
            mainTask.cancel();
            mainTask = null;
        }
        clearEntitiesAndState();
        clearCurrentBlocks();
        phase = Phase.IDLE;
        ctx = null;
    }

    @Override
    public boolean skipWait() {
        if (ctx == null || phase == Phase.IDLE) return false;
        if (phase == Phase.COUNTDOWN || phase == Phase.INTERMISSION) {
            phaseTicksLeft = 1;
            return true;
        }
        return false;
    }

    @Override
    public boolean skipRound() {
        if (ctx == null || phase == Phase.IDLE) return false;
        endRound(false, "Round skipped by admin");
        return true;
    }

    private void clearCurrentBlocks() {
        if (world == null) return;
        if (currentArena != null && !activeBlocks.isEmpty()) {
            currentArena.clear(world, activeBlocks);
        }
        for (RsCanvas.Pos p : dynamicBlocks) {
            Block b = world.getBlockAt(p.x(), p.y(), p.z());
            if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
        }
        activeBlocks.clear();
        dynamicBlocks.clear();
    }

    private void clearEntitiesAndState() {
        for (Rocket r : rockets) {
            if (r.entity() != null && !r.entity().isDead()) {
                r.entity().remove();
            }
        }
        rockets.clear();
        pendingCrumbles.clear();
        alive.clear();
        elimOrder.clear();
        lastHit.clear();
        updrafts.clear();
        lastUpdraftMs.clear();
        lastRocketMs.clear();
        survivalCarry.clear();
    }

    private void startNextRound() {
        round++;
        clearEntitiesAndState();
        clearCurrentBlocks();
        crumbleCursor = 0;
        padsSpawned = 0;

        if (forcedNextMapId != null) {
            currentArena = ArenaMaps.get(forcedNextMapId);
            forcedNextMapId = null;
        } else {
            currentArena = ArenaMaps.forRound(round - 1, cfg.preferImportedMaps);
        }

        if (currentArena instanceof ImportedRsArena imp) {
            activeElimY = Math.min(cfg.elimY, imp.baseY() - 12);
            activeClearRadius = Math.max(cfg.clearRadius, imp.maxHorizRadius());
            spectatorY = imp.baseY() + 30.0;
        } else {
            activeElimY = cfg.elimY;
            activeClearRadius = cfg.clearRadius;
            spectatorY = 108.0;
        }

        List<Player> players = new ArrayList<>(ctx.participants());
        int scaleRadius = Math.max(24, Math.min(34, 20 + players.size() / 2));
        RocketArenaMap.BuildResult build = currentArena.build(world, scaleRadius);
        activeBlocks = new ArrayList<>(build.crumbleOrder());
        List<Location> spawns = build.spawns();

        startCount = players.size();
        hasMultipleSides = ctx.sides().size() > 1;

        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            Location spawn = spawns.get(i % spawns.size());
            p.teleport(spawn);
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.setFallDistance(0f);
            p.setAllowFlight(cfg.allowDoubleJump);
            p.setFlying(false);

            giveKit(p);
            alive.add(p.getUniqueId());
            updrafts.put(p.getUniqueId(), cfg.updraftCharges);
            survivalCarry.put(p.getUniqueId(), 0.0);
        }

        int cdSec = McmApi.isFastTest() ? 1 : cfg.countdownSeconds;
        phase = Phase.COUNTDOWN;
        totalPhaseSeconds = cdSec;
        phaseTicksLeft = cdSec * 20;

        ctx.broadcast("§6§lRound " + round + "/" + cfg.rounds + " §e— Map: §b" + currentArena.displayName());
        updateHud();
    }

    private void giveKit(Player p) {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta em = elytra.getItemMeta();
        em.setUnbreakable(true);
        em.displayName(Component.text("Rocket Spleef Elytra", NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
        em.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        elytra.setItemMeta(em);
        p.getInventory().setItem(EquipmentSlot.CHEST, elytra);

        ItemStack launcher = new ItemStack(Material.BLAZE_ROD);
        ItemMeta lm = launcher.getItemMeta();
        lm.displayName(Component.text("Rocket Launcher", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        launcher.setItemMeta(lm);
        p.getInventory().setItem(0, launcher);

        if (cfg.updraftCharges > 0) {
            ItemStack feather = new ItemStack(Material.FEATHER, cfg.updraftCharges);
            ItemMeta fm = feather.getItemMeta();
            fm.displayName(Component.text("Updraft", NamedTextColor.WHITE, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            feather.setItemMeta(fm);
            p.getInventory().setItem(1, feather);
        }
    }

    private void tick() {
        if (ctx == null || phase == Phase.IDLE || finished) return;
        tickCounter++;

        tickRockets();
        tickCrumbles();

        if (phaseTicksLeft > 0) {
            phaseTicksLeft--;
        }

        if (tickCounter % 20 == 0) {
            updateHud();
        }

        if (phase == Phase.ACTIVE || phase == Phase.OVERTIME) {
            checkVoidEliminations();

            if (phase == Phase.ACTIVE) {
                int secLeft = phaseTicksLeft / 20;
                int elapsed = cfg.activeSeconds - secLeft;
                if (currentArena != null && currentArena.supportsDynamicPads()) {
                    if ((elapsed == 35 && padsSpawned == 0) || (elapsed == 75 && padsSpawned == 1)) {
                        padsSpawned++;
                        spawnDynamicPad();
                    }
                }
                // Gentle outer-edge crumbling in the final 40s of active play
                if (secLeft <= 40 && tickCounter % 10 == 0) {
                    queueCrumbleBatch(8);
                }
            } else if (phase == Phase.OVERTIME && tickCounter % 5 == 0) {
                queueCrumbleBatch(35);
            }
        }

        if (phaseTicksLeft <= 0) {
            advancePhase();
        }
    }

    private void spawnDynamicPad() {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        double angle = rng.nextDouble() * Math.PI * 2.0;
        double dist = rng.nextDouble(8.0, 20.0);
        int cx = (int) Math.round(Math.cos(angle) * dist);
        int cz = (int) Math.round(Math.sin(angle) * dist);
        int cy = rng.nextInt(96, 108);

        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -4; dz <= 4; dz++) {
                if (dx * dx + dz * dz <= 18) {
                    Material m = (dx * dx + dz * dz >= 10) ? Material.LIGHT_BLUE_CONCRETE : Material.WHITE_CONCRETE;
                    Block b = world.getBlockAt(cx + dx, cy, cz + dz);
                    b.setType(m, false);
                    dynamicBlocks.add(new RsCanvas.Pos(cx + dx, cy, cz + dz));
                }
            }
        }
        world.spawnParticle(Particle.CLOUD, new Location(world, cx + 0.5, cy + 1.5, cz + 0.5), 35, 2.5, 0.5, 2.5, 0.05);
        ctx.broadcast("§b★ New floating sky-island spawned above (§f" + cx + ", " + cy + ", " + cz + "§b)!");
    }

    private void advancePhase() {
        switch (phase) {
            case COUNTDOWN -> {
                phase = Phase.ACTIVE;
                totalPhaseSeconds = cfg.activeSeconds;
                phaseTicksLeft = cfg.activeSeconds * 20;
                for (UUID id : alive) {
                    Player p = Bukkit.getPlayer(id);
                    if (p != null) {
                        p.showTitle(Title.title(
                                Component.text("GO!", NamedTextColor.GREEN, TextDecoration.BOLD),
                                Component.text("Spacebar to Glide • Right-click Blaze Rod / Feather!", NamedTextColor.YELLOW),
                                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(300))
                        ));
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
                    }
                }
            }
            case ACTIVE -> {
                phase = Phase.OVERTIME;
                totalPhaseSeconds = cfg.overtimeSeconds;
                phaseTicksLeft = cfg.overtimeSeconds * 20;
                ctx.broadcast("§c§lOVERTIME! §ePlatforms are rapidly crumbling inward!");
            }
            case OVERTIME -> endRound(true, "Time's up!");
            case INTERMISSION -> {
                if (round >= cfg.rounds) {
                    finishGame("All " + cfg.rounds + " rounds complete!");
                } else {
                    startNextRound();
                }
            }
            default -> {}
        }
    }

    private void tickRockets() {
        Iterator<Rocket> it = rockets.iterator();
        while (it.hasNext()) {
            Rocket r = it.next();
            Snowball sb = r.entity();
            if (sb == null || sb.isDead() || tickCounter - r.bornTick() > cfg.rocketLifeTicks) {
                if (sb != null && !sb.isDead()) sb.remove();
                it.remove();
                continue;
            }

            Location pos = sb.getLocation();
            Vector vel = r.velocity();
            double stepLen = vel.length();

            RayTraceResult blockHit = world.rayTraceBlocks(pos, vel.clone().normalize(), stepLen + 0.35, FluidCollisionMode.NEVER, true);
            if (blockHit != null && blockHit.getHitBlock() != null) {
                explodeRocket(r.shooter(), blockHit.getHitPosition().toLocation(world));
                sb.remove();
                it.remove();
                continue;
            }

            Player hitPlayer = null;
            for (UUID id : alive) {
                if (id.equals(r.shooter()) && tickCounter - r.bornTick() < 4) continue;
                Player p = Bukkit.getPlayer(id);
                if (p == null || !p.getWorld().equals(world)) continue;
                if (p.getBoundingBox().expand(0.35).rayTrace(pos.toVector(), vel.clone().normalize(), stepLen + 0.35) != null) {
                    hitPlayer = p;
                    break;
                }
            }
            if (hitPlayer != null) {
                explodeRocket(r.shooter(), hitPlayer.getLocation());
                sb.remove();
                it.remove();
                continue;
            }

            sb.setVelocity(vel);
            world.spawnParticle(Particle.FIREWORK, pos, 2, 0.05, 0.05, 0.05, 0.01);
        }
    }

    private void explodeRocket(UUID shooterId, Location center) {
        world.spawnParticle(Particle.EXPLOSION, center, 1);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.2f);

        int rCeil = (int) Math.ceil(cfg.blastRadius);
        double r2 = cfg.blastRadius * cfg.blastRadius;
        for (int dx = -rCeil; dx <= rCeil; dx++) {
            for (int dy = -rCeil; dy <= rCeil; dy++) {
                for (int dz = -rCeil; dz <= rCeil; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= r2) {
                        Block b = world.getBlockAt(center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                        if (b.getType() != Material.AIR) {
                            b.setType(Material.AIR, false);
                        }
                    }
                }
            }
        }

        long now = System.currentTimeMillis();
        String shooterTeam = ctx.teamOf(shooterId);
        for (UUID id : alive) {
            Player target = Bukkit.getPlayer(id);
            if (target == null || !target.getWorld().equals(world)) continue;
            double dist = target.getLocation().distance(center);
            if (dist <= cfg.knockbackRadius) {
                Vector push = target.getLocation().toVector().subtract(center.toVector());
                if (push.lengthSquared() < 0.01) push = new Vector(0, 1, 0);
                double scale = 1.0 - (dist / cfg.knockbackRadius) * 0.5;
                push.normalize().multiply(cfg.knockbackStrength * scale);
                push.setY(Math.max(push.getY(), cfg.knockbackVertical * scale));
                target.setVelocity(target.getVelocity().add(push));

                if (!id.equals(shooterId)) {
                    String targetTeam = ctx.teamOf(id);
                    if (shooterTeam == null || !shooterTeam.equals(targetTeam)) {
                        lastHit.put(id, new Hit(shooterId, now));
                    }
                }
            }
        }
    }

    private void queueCrumbleBatch(int count) {
        int marked = 0;
        while (crumbleCursor < activeBlocks.size() && marked < count && pendingCrumbles.size() < 400) {
            RsCanvas.Pos pos = activeBlocks.get(crumbleCursor++);
            Block b = world.getBlockAt(pos.x(), pos.y(), pos.z());
            if (b.getType() != Material.AIR && b.getType() != Material.RED_CONCRETE) {
                b.setType(Material.RED_CONCRETE, false);
                pendingCrumbles.add(new CrumbleWarning(b, tickCounter + 20));
                marked++;
            }
        }
    }

    private void tickCrumbles() {
        Iterator<CrumbleWarning> it = pendingCrumbles.iterator();
        while (it.hasNext()) {
            CrumbleWarning cw = it.next();
            if (tickCounter >= cw.vanishTick()) {
                if (cw.block().getType() == Material.RED_CONCRETE) {
                    cw.block().setType(Material.AIR, false);
                }
                it.remove();
            }
        }
    }

    private void checkVoidEliminations() {
        List<UUID> fallen = new ArrayList<>();
        for (UUID id : alive) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || p.getLocation().getY() < activeElimY) {
                fallen.add(id);
            }
        }
        for (UUID id : fallen) {
            eliminate(id);
        }
        if (alive.isEmpty() || (hasMultipleSides && countAliveSides() <= 1 && startCount > 1)) {
            endRound(true, "Round over!");
        }
    }

    private int countAliveSides() {
        Set<String> sides = new HashSet<>();
        for (UUID id : alive) {
            String t = ctx.teamOf(id);
            sides.add(t != null ? "TEAM:" + t : "SOLO:" + id);
        }
        return sides.size();
    }

    private void eliminate(UUID victimId) {
        if (!alive.remove(victimId)) return;
        elimOrder.add(victimId);

        Player victim = Bukkit.getPlayer(victimId);
        if (victim != null) {
            victim.setGameMode(GameMode.SPECTATOR);
            victim.setAllowFlight(true);
            victim.teleport(new Location(world, 0.5, spectatorY, 0.5));
            victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.5f);
        }

        if (startCount > 1 && !alive.isEmpty()) {
            double share = cfg.survivalPool / (startCount - 1);
            for (UUID survId : alive) {
                double total = survivalCarry.getOrDefault(survId, 0.0) + share;
                int whole = (int) Math.floor(total);
                survivalCarry.put(survId, total - whole);
                if (whole > 0) {
                    ctx.addPoints(survId, whole, "Out-survived opponent");
                }
            }
        }

        Hit hit = lastHit.get(victimId);
        if (hit != null && System.currentTimeMillis() - hit.timeMs() <= cfg.koCreditMs) {
            Player killer = Bukkit.getPlayer(hit.shooter());
            if (killer != null) {
                ctx.addPoints(hit.shooter(), cfg.koPoints, "Rocket knock-off");
                ctx.broadcast("§c" + (victim != null ? victim.getName() : "Player") + " §7was knocked into the void by §a" + killer.getName() + "§7!");
                return;
            }
        }
        if (victim != null) {
            ctx.broadcast("§c" + victim.getName() + " §7fell into the void!");
        }
    }

    private void endRound(boolean awardEndPoints, String reason) {
        if (awardEndPoints && !alive.isEmpty()) {
            int bonus = (hasMultipleSides && countAliveSides() == 1) ? cfg.lastSideBonus : cfg.sharedSurvivorBonus;
            for (UUID id : alive) {
                ctx.addPoints(id, bonus, "Round survivor");
            }
            int n = elimOrder.size();
            for (int i = 0; i < cfg.placementBonus.length && i < n; i++) {
                UUID id = elimOrder.get(n - 1 - i);
                ctx.addPoints(id, cfg.placementBonus[i], "Late elimination #" + (i + 1));
            }
        }
        ctx.broadcast("§e" + reason);

        int interSec = McmApi.isFastTest() ? 1 : cfg.intermissionSeconds;
        phase = Phase.INTERMISSION;
        totalPhaseSeconds = interSec;
        phaseTicksLeft = interSec * 20;
    }

    private void finishGame(String reason) {
        if (finished) return;
        finished = true;
        McmGameContext c = this.ctx;
        stop();
        if (c != null) {
            c.finish(reason);
        }
    }

    private void updateHud() {
        if (ctx == null) return;
        int secLeft = Math.max(0, (phaseTicksLeft + 19) / 20);
        ctx.setTimer(secLeft, Math.max(1, totalPhaseSeconds));
        List<String> lines = new ArrayList<>();
        lines.add("§eRound: §f" + round + "/" + cfg.rounds);
        lines.add("§eMap: §b" + (currentArena != null ? currentArena.displayName() : "-"));
        lines.add("§eAlive: §a" + alive.size() + "§7/" + startCount);
        lines.add("§ePhase: §f" + phase.name());
        ctx.setSidebarLines(lines);
    }

    // -------------------------------------------------------------------------
    // Listeners (strictly scoped to mcc_rocketspleef while running)
    // -------------------------------------------------------------------------

    private boolean inArena(Player p) {
        return ctx != null && phase != Phase.IDLE && world != null && p.getWorld().equals(world);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!inArena(p) || e.getTo() == null) return;
        if (phase == Phase.COUNTDOWN && alive.contains(p.getUniqueId())) {
            if (e.getFrom().getX() != e.getTo().getX() || e.getFrom().getZ() != e.getTo().getZ()) {
                Location back = e.getFrom().clone();
                back.setYaw(e.getTo().getYaw());
                back.setPitch(e.getTo().getPitch());
                e.setTo(back);
            }
        } else if (p.getGameMode() == GameMode.SPECTATOR) {
            if (Math.hypot(e.getTo().getX(), e.getTo().getZ()) > activeClearRadius + 16) {
                e.setTo(new Location(world, 0.5, spectatorY, 0.5));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!inArena(p) || !alive.contains(p.getUniqueId())) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != EquipmentSlot.HAND) return;

        ItemStack item = p.getInventory().getItemInMainHand();
        if (item.getType() == Material.BLAZE_ROD) {
            e.setCancelled(true);
            if (phase == Phase.COUNTDOWN || phase == Phase.INTERMISSION) return;
            long now = System.currentTimeMillis();
            long last = lastRocketMs.getOrDefault(p.getUniqueId(), 0L);
            if (now - last < cfg.rocketCooldownTicks * 50L) return;
            lastRocketMs.put(p.getUniqueId(), now);
            p.setCooldown(Material.BLAZE_ROD, cfg.rocketCooldownTicks);

            Vector dir = p.getEyeLocation().getDirection().normalize().multiply(cfg.rocketSpeed);
            Snowball sb = p.launchProjectile(Snowball.class, dir);
            sb.setGravity(false);
            sb.setItem(new ItemStack(Material.FIREWORK_ROCKET));
            rockets.add(new Rocket(p.getUniqueId(), sb, dir, tickCounter));
            world.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.1f);
        } else if (item.getType() == Material.FEATHER) {
            e.setCancelled(true);
            triggerUpdraft(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onToggleFlight(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        if (!inArena(p) || !alive.contains(p.getUniqueId())) return;
        if (p.getGameMode() != GameMode.SURVIVAL) return;
        e.setCancelled(true);
        p.setFlying(false);
        if (cfg.allowDoubleJump) {
            triggerUpdraft(p);
        }
    }

    private void triggerUpdraft(Player p) {
        if (phase == Phase.COUNTDOWN || phase == Phase.INTERMISSION) return;
        int charges = updrafts.getOrDefault(p.getUniqueId(), 0);
        if (charges <= 0) return;
        long now = System.currentTimeMillis();
        if (now - lastUpdraftMs.getOrDefault(p.getUniqueId(), 0L) < cfg.updraftCooldownMs) return;

        lastUpdraftMs.put(p.getUniqueId(), now);
        updrafts.put(p.getUniqueId(), charges - 1);
        if (charges - 1 <= 0) {
            p.getInventory().remove(Material.FEATHER);
        } else {
            ItemStack f = p.getInventory().getItem(1);
            if (f != null && f.getType() == Material.FEATHER) f.setAmount(charges - 1);
        }
        Vector boost = p.getLocation().getDirection().setY(0);
        if (boost.lengthSquared() > 0.001) boost.normalize().multiply(0.35);
        boost.setY(cfg.updraftVelocity);
        p.setVelocity(boost);
        world.playSound(p.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.4f);
        world.spawnParticle(Particle.CLOUD, p.getLocation(), 12, 0.3, 0.1, 0.3, 0.05);
    }

    @EventHandler public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && inArena(p)) e.setCancelled(true);
    }

    @EventHandler public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && inArena(p)) { e.setCancelled(true); p.setFoodLevel(20); }
    }

    @EventHandler public void onBreak(BlockBreakEvent e) { if (inArena(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onPlace(BlockPlaceEvent e) { if (inArena(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onDrop(PlayerDropItemEvent e) { if (inArena(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onSwap(PlayerSwapHandItemsEvent e) { if (inArena(e.getPlayer())) e.setCancelled(true); }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (ctx != null && alive.remove(e.getPlayer().getUniqueId())) {
            if (alive.isEmpty() || (hasMultipleSides && countAliveSides() <= 1 && startCount > 1)) {
                endRound(true, "Round over!");
            }
        }
    }
}
