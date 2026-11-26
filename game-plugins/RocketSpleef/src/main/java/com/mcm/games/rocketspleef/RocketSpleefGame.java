package com.mcm.games.rocketspleef;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.rocketspleef.map.MapGen;
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
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The original MCC Rocket Spleef (MCC 1-13). Three 4:00 rounds on Nanotube, Rings and Easter Eggs!; a purple-glint
 * shovel that fires fireball rockets (clip of 3), one firework per round, an unbreakable elytra, and TNT Time for the
 * final 2 minutes (fireworks removed, night sky, TNT rains onto the map). Terrain is only lost to rockets and TNT.
 * Round flow measured from the MCC 13 VODs (notes.md section 7): 15 s in a drop room ("Dropping in 10..1"), the floor
 * opens, 4:00 of play, "Round Over!", 10 s "Next Round".
 */
public final class RocketSpleefGame implements McmGame, Listener {

    static final String ID = "rocketspleef";
    static final String WORLD_NAME = "mcc_" + ID;
    static final String LAUNCHER_NAME = "Rocket Launcher";

    enum Phase { IDLE, COUNTDOWN, ACTIVE, TNT_TIME, INTERMISSION }

    private record Hit(UUID by, long timeMs) {}
    private record Rocket(UUID shooter, Snowball entity, Vector velocity, int bornTick) {}

    private final JavaPlugin plugin;
    private RocketConfig cfg;
    private World world;

    private McmGameContext ctx;
    private Phase phase = Phase.IDLE;
    private int round = 0;
    private int phaseTicksLeft = 0;
    private int roundTick = 0;          // ticks since the drop (ACTIVE + TNT_TIME)
    private int tickCounter = 0;
    private boolean finished = false;
    private String forcedNextMapId;

    private PastedMap map;              // the map of the current round (or a /rs show preview)
    private final List<int[]> dropRoom = new ArrayList<>();
    private final Set<UUID> alive = new HashSet<>();
    private final Map<UUID, Hit> lastHit = new HashMap<>();
    private final Map<UUID, Integer> ammo = new HashMap<>();
    private final Map<UUID, Integer> reloadLeft = new HashMap<>();
    private final Map<UUID, Integer> nextShotTick = new HashMap<>();
    private final Map<UUID, Double> carry = new HashMap<>();
    private final List<Rocket> rockets = new ArrayList<>();
    private final Set<UUID> ourTnt = new HashSet<>();
    private double tntAccumulator = 0;
    private int startCount = 0;
    private boolean multiSide = false;
    private BukkitTask mainTask;

    /** Debug/test counters (read by /rs debug). */
    int tntSpawned = 0, rocketsFired = 0, blocksBlasted = 0;

    public RocketSpleefGame(JavaPlugin plugin, RocketConfig cfg, World world) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.world = world;
        new SpectatorGuard(plugin, p -> {
            if (ctx == null || phase == Phase.IDLE || this.world == null || map == null) return null;
            if (!p.getWorld().equals(this.world) && !ctx.participants().contains(p)) return null;
            double r = map.radius + 24.0;
            BoundingBox box = new BoundingBox(-r, map.minWorldY - 20.0, -r, r, map.maxWorldY + 40.0, r);
            return new SpectatorGuard.Zone(this.world, box, spectatorHome());
        });
    }

    void setConfig(RocketConfig cfg) { this.cfg = cfg; }
    RocketConfig config() { return cfg; }
    Phase phase() { return phase; }
    World world() { return world; }

    private Location spectatorHome() {
        return new Location(world, 0.5, map.maxWorldY + 12.0, map.radius + 8.0, 180f, 35f);
    }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Rocket Spleef"; }
    @Override public Material icon() { return Material.IRON_SHOVEL; }
    @Override public String description() { return "Original MCC Rocket Spleef: fireball shovels, elytra, one firework, TNT Time."; }

    @Override
    public List<String> maps() {
        List<String> ids = new ArrayList<>();
        for (MapGen m : MapGen.all()) ids.add(m.id());
        return ids;
    }

    @Override
    public boolean forceNextMap(String id) {
        if (MapGen.byId(id) == null) return false;
        forcedNextMapId = id.toLowerCase();
        return true;
    }

    @Override public String currentMap() { return map == null ? null : map.gen.id(); }

    @Override
    public void start(McmGameContext ctx) {
        stop();
        if (world == null) world = RocketSpleefPlugin.createVoidWorld(WORLD_NAME);
        if (world == null) {
            ctx.broadcast("§cRocket Spleef failed to load its world.");
            ctx.finish("World load failure");
            return;
        }
        this.ctx = ctx;
        finished = false;
        round = 0;
        tickCounter = 0;
        startNextRound();
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    @Override
    public void stop() {
        if (mainTask != null) { mainTask.cancel(); mainTask = null; }
        clearRoundState();
        clearMap();
        if (world != null) world.setTime(6000L);
        phase = Phase.IDLE;
        ctx = null;
    }

    @Override
    public boolean skipWait() {
        if (ctx == null) return false;
        if (phase == Phase.COUNTDOWN || phase == Phase.INTERMISSION) { phaseTicksLeft = 1; return true; }
        return false;
    }

    @Override
    public boolean skipRound() {
        if (ctx == null || phase == Phase.IDLE) return false;
        if (phase == Phase.INTERMISSION) { phaseTicksLeft = 1; return true; }
        endRound(false);
        return true;
    }

    /** Admin/test: jump straight to TNT Time. */
    boolean forceTntTime() {
        if (ctx == null || phase != Phase.ACTIVE) return false;
        roundTick = cfg.tntTimeAtSeconds * 20;
        phaseTicksLeft = 1;
        return true;
    }

    // ------------------------------------------------------------------ map

    void clearMap() {
        removeDropRoom();
        if (map != null) { map.clear(); map = null; }
    }

    /** /rs show: paste a map for a look-around while no game runs. */
    PastedMap showMap(String id) {
        if (ctx != null) return null;
        MapGen g = MapGen.byId(id);
        if (g == null) return null;
        if (world == null) world = RocketSpleefPlugin.createVoidWorld(WORLD_NAME);
        clearMap();
        map = new PastedMap(g, world, cfg.mapAnchorY);
        return map;
    }

    private void clearRoundState() {
        for (Rocket r : rockets) if (r.entity() != null && !r.entity().isDead()) r.entity().remove();
        rockets.clear();
        if (world != null) {
            for (Entity e : world.getEntitiesByClass(TNTPrimed.class)) if (ourTnt.contains(e.getUniqueId())) e.remove();
        }
        ourTnt.clear();
        alive.clear();
        lastHit.clear();
        ammo.clear();
        reloadLeft.clear();
        nextShotTick.clear();
        carry.clear();
        tntAccumulator = 0;
    }

    /**
     * Old MCC (MCC 13 VOD): everyone waits in a drop room above the map, then the floor opens and they fall onto the
     * map. The room covers all of the map's standing spots and each player waits straight above one of them.
     * Returns standing spots inside the room.
     */
    private List<Location> buildDropRoom() {
        removeDropRoom();
        // The room covers every standing spot of the map, and each player is placed straight above one of them, so the
        // drop lands on the map (Nanotube is hollow: a small room above one spot would drop most players into the tube).
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, topY = map.maxWorldY;
        for (Location l : map.spawns) {
            minX = Math.min(minX, l.getBlockX()); maxX = Math.max(maxX, l.getBlockX());
            minZ = Math.min(minZ, l.getBlockZ()); maxZ = Math.max(maxZ, l.getBlockZ());
            topY = Math.max(topY, l.getBlockY());
        }
        minX -= 2; maxX += 2; minZ -= 2; maxZ += 2;
        int fy = topY + cfg.dropRoomHeight;
        BlockData glass = Bukkit.createBlockData("minecraft:light_blue_stained_glass");
        BlockData barrier = Bukkit.createBlockData("minecraft:barrier");
        for (int x = minX - 1; x <= maxX + 1; x++)
            for (int z = minZ - 1; z <= maxZ + 1; z++)
                for (int y = 0; y <= 4; y++) {
                    boolean wall = x == minX - 1 || x == maxX + 1 || z == minZ - 1 || z == maxZ + 1;
                    BlockData bd = y == 0 ? glass : (wall || y == 4) ? barrier : null;
                    if (bd == null) continue;
                    Block b = world.getBlockAt(x, fy + y, z);
                    if (!b.getType().isAir()) continue;
                    b.setBlockData(bd, false);
                    dropRoom.add(new int[]{x, fy + y, z});
                }
        List<Location> spots = new ArrayList<>();
        for (Location l : map.spawns) spots.add(new Location(world, l.getBlockX() + 0.5, fy + 1, l.getBlockZ() + 0.5, 0f, 60f));
        Collections.shuffle(spots, new Random(round));
        return spots;
    }

    private void removeDropRoom() {
        if (world != null) for (int[] b : dropRoom) world.getBlockAt(b[0], b[1], b[2]).setType(Material.AIR, false);
        dropRoom.clear();
    }

    // ------------------------------------------------------------------ rounds

    private void startNextRound() {
        round++;
        clearRoundState();
        clearMap();
        world.setTime(6000L);
        List<MapGen> all = MapGen.all();
        MapGen gen = forcedNextMapId != null ? MapGen.byId(forcedNextMapId) : all.get((round - 1) % all.size());
        forcedNextMapId = null;
        map = new PastedMap(gen, world, cfg.mapAnchorY);

        List<Player> players = new ArrayList<>(ctx.participants());
        players.sort(Comparator.comparing((Player p) -> String.valueOf(ctx.teamOf(p.getUniqueId()))).thenComparing(Player::getName));
        startCount = players.size();
        multiSide = ctx.sides().size() > 1;
        List<Location> room = buildDropRoom();
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            p.teleport(room.get(i % room.size()));
            p.setGameMode(GameMode.SURVIVAL);
            p.getInventory().clear();
            p.setHealth(p.getMaxHealth());
            p.setFoodLevel(20);
            p.setFallDistance(0f);
            p.setFireTicks(0);
            p.setVelocity(new Vector());
            p.setGliding(false);
            p.setAllowFlight(false);
            p.setFlying(false);
            UUID id = p.getUniqueId();
            alive.add(id);
            ammo.put(id, Math.min(cfg.clip, cfg.startAmmo));
            reloadLeft.put(id, 0);
            nextShotTick.put(id, 0);
            carry.put(id, 0.0);
            giveKit(p);
        }

        int cd = FastTest.secs(cfg.countdownSeconds);
        phase = Phase.COUNTDOWN;
        phaseTicksLeft = cd * 20;
        roundTick = 0;
        ctx.broadcast("§6§lRound " + round + "/" + cfg.rounds + " §e- Map: §b" + gen.displayName());
        updateHud();
    }

    static boolean isLauncher(ItemStack it) {
        if (it == null || it.getType() != Material.IRON_SHOVEL || !it.hasItemMeta()) return false;
        Component n = it.getItemMeta().displayName();
        return n != null && net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(n).equals(LAUNCHER_NAME);
    }

    private void giveKit(Player p) {
        ItemStack elytra = new ItemStack(Material.ELYTRA);
        ItemMeta em = elytra.getItemMeta();
        em.setUnbreakable(true);
        em.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
        elytra.setItemMeta(em);
        p.getInventory().setItem(EquipmentSlot.CHEST, elytra);

        // Old MCC: a shovel with the purple enchantment glint that shoots fireballs (notes.md K1)
        ItemStack launcher = new ItemStack(Material.IRON_SHOVEL);
        ItemMeta lm = launcher.getItemMeta();
        lm.displayName(Component.text(LAUNCHER_NAME, NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
        lm.lore(List.of(Component.text("Right-click: fire a rocket (clip of " + cfg.clip + ")", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Melee hit: high knockback", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
        lm.setEnchantmentGlintOverride(true);
        lm.setUnbreakable(true);
        lm.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        launcher.setItemMeta(lm);
        p.getInventory().setItem(0, launcher);

        if (cfg.fireworks > 0) {
            ItemStack fw = new ItemStack(Material.FIREWORK_ROCKET, cfg.fireworks);
            FireworkMeta fm = (FireworkMeta) fw.getItemMeta();
            fm.setPower(Math.max(0, Math.min(3, cfg.fireworkFlight)));
            fm.displayName(Component.text("Firework (1 per round)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            fw.setItemMeta(fm);
            p.getInventory().setItem(1, fw);
        }
        p.setLevel(Math.min(cfg.clip, cfg.startAmmo));
        p.setExp(0f);
    }

    private void tick() {
        if (ctx == null || phase == Phase.IDLE || finished) return;
        tickCounter++;
        tickRockets();
        if (phase == Phase.COUNTDOWN && phaseTicksLeft % 20 == 0) {
            int sec = phaseTicksLeft / 20;
            if (sec >= 1 && sec <= cfg.dropInTitleSeconds) {
                for (UUID id : alive) {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null) continue;
                    p.showTitle(Title.title(Component.text("Dropping in", NamedTextColor.AQUA),
                            Component.text("> " + sec + " <", sec <= 3 ? NamedTextColor.RED : NamedTextColor.WHITE),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.8f, sec <= 3 ? 1.6f : 1.0f);
                }
            }
        }
        if (phase == Phase.ACTIVE && cfg.tntWarningSeconds > 0
                && roundTick == (cfg.tntTimeAtSeconds - cfg.tntWarningSeconds) * 20) {
            ctx.broadcast("§6[Rocket Spleef] §e" + cfg.tntWarningSeconds + " seconds until TNT time starts!");
        }
        if (phase == Phase.ACTIVE || phase == Phase.TNT_TIME) {
            roundTick++;
            tickReload();
            if (phase == Phase.TNT_TIME) tickTnt();
            checkVoid();
        }
        if (phaseTicksLeft > 0) phaseTicksLeft--;
        if (tickCounter % 20 == 0) updateHud();
        if (phase != Phase.IDLE && phaseTicksLeft <= 0 && ctx != null) advancePhase();
    }

    private void advancePhase() {
        switch (phase) {
            case COUNTDOWN -> {
                phase = Phase.ACTIVE;
                phaseTicksLeft = cfg.tntTimeAtSeconds * 20 - roundTick;
                removeDropRoom();   // the floor opens: everyone drops onto the map
                for (UUID id : alive) {
                    Player p = Bukkit.getPlayer(id);
                    if (p == null) continue;
                    p.clearTitle();
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.2f);
                }
            }
            case ACTIVE -> startTntTime();
            case TNT_TIME -> endRound(true);
            case INTERMISSION -> {
                if (round >= cfg.rounds) finishGame("All " + cfg.rounds + " rounds complete!");
                else startNextRound();
            }
            default -> {}
        }
    }

    private void startTntTime() {
        phase = Phase.TNT_TIME;
        phaseTicksLeft = Math.max(1, (cfg.roundSeconds - cfg.tntTimeAtSeconds) * 20);
        if (cfg.tntNight) world.setTime(18000L);   // VOD: the sky turns to night when TNT Time starts
        for (UUID id : alive) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            p.getInventory().remove(Material.FIREWORK_ROCKET);
            p.showTitle(Title.title(Component.text("TNT Time!", NamedTextColor.RED, TextDecoration.BOLD),
                    Component.text("Fireworks removed", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(400))));
            p.playSound(p.getLocation(), Sound.ENTITY_TNT_PRIMED, 1f, 0.8f);
        }
        ctx.broadcast("§c§lTNT TIME! §eFireworks are gone and TNT is falling!");
    }

    // ------------------------------------------------------------------ launcher

    private void tickReload() {
        for (UUID id : alive) {
            int a = ammo.getOrDefault(id, 0);
            Player p = Bukkit.getPlayer(id);
            if (a >= cfg.clip) {
                reloadLeft.put(id, 0);
                if (p != null) { p.setLevel(a); p.setExp(0f); }
                continue;
            }
            int left = reloadLeft.getOrDefault(id, 0);
            if (left <= 0) left = cfg.reloadTicks;
            left--;
            if (left <= 0) {
                a++;
                ammo.put(id, a);
                left = a < cfg.clip ? cfg.reloadTicks : 0;
            }
            reloadLeft.put(id, left);
            if (p != null) {
                p.setLevel(a);
                p.setExp(a >= cfg.clip ? 0f : Math.min(0.99f, 1f - (float) left / cfg.reloadTicks));
            }
        }
    }

    private void fire(Player p) {
        UUID id = p.getUniqueId();
        if (phase != Phase.ACTIVE && phase != Phase.TNT_TIME) return;
        int a = ammo.getOrDefault(id, 0);
        if (a <= 0 || tickCounter < nextShotTick.getOrDefault(id, 0)) return;
        ammo.put(id, a - 1);
        // VOD: firing restarts the reload bar ("Reloading (2/3)" at 7/11 -> "(1/3)" empty)
        if (cfg.reloadResetsOnFire || reloadLeft.getOrDefault(id, 0) <= 0) reloadLeft.put(id, cfg.reloadTicks);
        nextShotTick.put(id, tickCounter + cfg.shotGapTicks);
        p.setCooldown(Material.IRON_SHOVEL, cfg.shotGapTicks);
        p.setLevel(a - 1);

        Vector vel = p.getEyeLocation().getDirection().normalize().multiply(cfg.rocketSpeed);
        Snowball sb = p.launchProjectile(Snowball.class, vel);
        sb.setGravity(false);
        sb.setItem(new ItemStack(Material.FIRE_CHARGE));
        sb.setVelocity(vel);
        rockets.add(new Rocket(id, sb, vel, tickCounter));
        rocketsFired++;
        world.playSound(p.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.8f, 1.2f);
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
            Vector dir = r.velocity().clone().normalize();
            double step = r.velocity().length() + 0.35;
            RayTraceResult bh = world.rayTraceBlocks(pos, dir, step, FluidCollisionMode.NEVER, true);
            Location hitAt = null;
            if (bh != null && bh.getHitBlock() != null) hitAt = bh.getHitPosition().toLocation(world);
            for (UUID id : alive) {
                if (id.equals(r.shooter()) && tickCounter - r.bornTick() < 4) continue;
                Player p = Bukkit.getPlayer(id);
                if (p == null || !p.getWorld().equals(world)) continue;
                RayTraceResult ph = p.getBoundingBox().expand(0.3).rayTrace(pos.toVector(), dir, step);
                if (ph != null && (hitAt == null || ph.getHitPosition().distanceSquared(pos.toVector()) < hitAt.toVector().distanceSquared(pos.toVector()))) {
                    hitAt = ph.getHitPosition().toLocation(world);
                }
            }
            if (hitAt != null) {
                blast(r.shooter(), hitAt, cfg.blastRadius, cfg.knockbackRadius, true);
                sb.remove();
                it.remove();
                continue;
            }
            sb.setVelocity(r.velocity());
            world.spawnParticle(Particle.FLAME, pos, 3, 0.08, 0.08, 0.08, 0.01);
            world.spawnParticle(Particle.SMOKE, pos, 1, 0.05, 0.05, 0.05, 0.0);
        }
    }

    /**
     * Removes a sphere of blocks and knocks back every alive player in range (teammates too). A player at the blast
     * centre is launched up to {@code rocket-jump-height} blocks; the push falls off to half at the edge.
     * Credit (last hit) only goes to a shooter on another team; TNT ({@code shooter == null}) gives none.
     */
    void blast(UUID shooter, Location c, double blockR, double kbR, boolean rocket) {
        world.spawnParticle(Particle.EXPLOSION, c, 1);
        world.playSound(c, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, rocket ? 1.3f : 0.9f);
        int rc = (int) Math.ceil(blockR);
        double r2 = blockR * blockR;
        int cx = c.getBlockX(), cy = c.getBlockY(), cz = c.getBlockZ();
        for (int dx = -rc; dx <= rc; dx++)
            for (int dy = -rc; dy <= rc; dy++)
                for (int dz = -rc; dz <= rc; dz++) {
                    double ex = cx + dx + 0.5 - c.getX(), ey = cy + dy + 0.5 - c.getY(), ez = cz + dz + 0.5 - c.getZ();
                    if (ex * ex + ey * ey + ez * ez > r2) continue;
                    Block b = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    if (!b.getType().isAir() && b.getType() != Material.BARRIER) { b.setType(Material.AIR, false); blocksBlasted++; }
                }
        long now = System.currentTimeMillis();
        String shooterTeam = shooter == null ? null : ctx.teamOf(shooter);
        for (UUID id : alive) {
            Player t = Bukkit.getPlayer(id);
            if (t == null || !t.getWorld().equals(world)) continue;
            Location feet = t.getLocation();
            Vector to = feet.toVector().add(new Vector(0, 0.9, 0)).subtract(c.toVector());
            double d = to.length();
            if (d > kbR) continue;
            double scale = 1.0 - 0.5 * Math.max(0, d - 1.0) / Math.max(0.01, kbR - 1.0);
            Vector h = new Vector(to.getX(), 0, to.getZ());
            double hl = h.length();
            // a blast straight below pushes up, not sideways: horizontal share grows with the sideways offset
            if (hl > 1e-2) h.multiply(cfg.knockbackHorizontal * scale * Math.min(1.0, hl / Math.max(0.01, d) * 1.5) / hl);
            else h.zero();
            double up = to.getY() >= -0.6 ? cfg.rocketJumpVelocity * scale : 0.25 * scale;  // blast below or level: lift
            Vector v = t.getVelocity();
            t.setVelocity(new Vector(v.getX() * 0.3 + h.getX(), Math.max(v.getY(), up), v.getZ() * 0.3 + h.getZ()));
            t.setFallDistance(0f);
            if (shooter != null && !id.equals(shooter)) {
                String tt = ctx.teamOf(id);
                if (shooterTeam == null || !shooterTeam.equals(tt)) lastHit.put(id, new Hit(shooter, now));
            }
        }
    }

    // ------------------------------------------------------------------ TNT Time

    private void tickTnt() {
        tntAccumulator += cfg.tntPerSecond / 20.0;
        while (tntAccumulator >= 1.0) {
            tntAccumulator -= 1.0;
            dropTnt();
        }
    }

    private void dropTnt() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int tries = 0; tries < 12; tries++) {
            int x = rnd.nextInt(-map.radius, map.radius + 1), z = rnd.nextInt(-map.radius, map.radius + 1);
            Block top = world.getHighestBlockAt(x, z);
            if (top.getType().isAir() || top.getY() < map.minWorldY) continue;
            int h = rnd.nextInt(cfg.tntDropMin, Math.max(cfg.tntDropMin, cfg.tntDropMax) + 1);
            TNTPrimed tnt = world.spawn(new Location(world, x + 0.5, top.getY() + 1 + h, z + 0.5), TNTPrimed.class, e -> {
                e.setFuseTicks(cfg.tntFuseTicks);
                e.setVelocity(new Vector());
            });
            ourTnt.add(tnt.getUniqueId());
            tntSpawned++;
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrime(ExplosionPrimeEvent e) {
        if (!(e.getEntity() instanceof TNTPrimed) || !ourTnt.remove(e.getEntity().getUniqueId())) return;
        e.setCancelled(true);
        if (ctx == null || world == null) return;
        blast(null, e.getEntity().getLocation(), cfg.tntBlastRadius, cfg.tntKnockbackRadius, false);
    }

    // ------------------------------------------------------------------ eliminations and scoring

    private void checkVoid() {
        List<UUID> fallen = new ArrayList<>();
        for (UUID id : alive) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.getWorld().equals(world) || p.getLocation().getY() < map.minWorldY - cfg.elimDepth) fallen.add(id);
        }
        for (UUID id : fallen) eliminate(id);
        if (roundOver()) endRound(false);
    }

    private boolean roundOver() {
        if (phase != Phase.ACTIVE && phase != Phase.TNT_TIME) return false;
        return alive.isEmpty() || (multiSide && startCount > 1 && aliveSides() <= 1);
    }

    private int aliveSides() {
        Set<String> s = new HashSet<>();
        for (UUID id : alive) { String t = ctx.teamOf(id); s.add(t != null ? "T:" + t : "S:" + id); }
        return s.size();
    }

    private void eliminate(UUID victimId) {
        if (!alive.remove(victimId)) return;
        Player victim = Bukkit.getPlayer(victimId);
        String vName = victim != null ? victim.getName() : "A player";
        if (victim != null) {
            victim.getInventory().clear();
            SpectatorGuard.makeSpectator(victim, spectatorHome());
            victim.playSound(victim.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.4f, 1.6f);
        }
        // MCC coins: every survivor outlives this player (the core keeps fractions exact)
        for (UUID s : alive) if (cfg.ptsOutlive > 0) coins(s, cfg.ptsOutlive, "Outlived a player");
        // chat wording from the MCC 13 VOD
        Hit h = lastHit.get(victimId);
        Player killer = h != null && System.currentTimeMillis() - h.timeMs() <= cfg.killCreditMs ? Bukkit.getPlayer(h.by()) : null;
        if (killer != null) {
            if (cfg.ptsElimination > 0) killCoins(h.by(), cfg.ptsElimination, victimId, vName);
            ctx.broadcast("§c" + vName + " §7was eliminated by §a" + killer.getName());
        } else {
            ctx.broadcast("§c" + vName + " §7fell off the map and eliminated themselves!");
        }
        String team = ctx.teamOf(victimId);
        if (team != null) {
            boolean left = false;
            for (UUID id : alive) if (team.equals(ctx.teamOf(id))) { left = true; break; }
            if (!left) ctx.broadcast("§e" + team + " §7have all been eliminated!");
        }
    }

    /** MCC coins (base x1.0); logged so the bot test (and the owner) can see every award. */
    private void coins(UUID id, double coins, String reason) {
        ctx.addCoins(id, coins, reason);
        Player p = Bukkit.getPlayer(id);
        plugin.getLogger().info("[coins] " + (p != null ? p.getName() : id) + " +" + coins + " x" + ctx.coinMultiplier() + " " + reason);
    }

    private void killCoins(UUID killer, double coins, UUID victimId, String victimName) {
        String reason = "You eliminated " + victimName;
        ctx.addKillCoins(killer, coins, victimId, victimName, reason);
        Player p = Bukkit.getPlayer(killer);
        plugin.getLogger().info("[coins] " + (p != null ? p.getName() : killer) + " +" + coins + " x" + ctx.coinMultiplier() + " " + reason);
    }

    private void endRound(boolean timeRanOut) {
        for (Rocket r : rockets) if (r.entity() != null && !r.entity().isDead()) r.entity().remove();
        rockets.clear();
        removeDropRoom();
        // Survival bonus only when every survivor is on ONE team (W-RS coin rewards)
        if (!alive.isEmpty() && aliveSides() == 1) {
            for (UUID id : alive) if (cfg.ptsSoleTeamSurvival > 0) coins(id, cfg.ptsSoleTeamSurvival, "Survived the round");
        }
        List<String> names = new ArrayList<>();
        for (UUID id : alive) { Player p = Bukkit.getPlayer(id); if (p != null) names.add(p.getName()); }
        if (timeRanOut) ctx.broadcast("§6[Rocket Spleef] §eThe time ran out! Last players standing: §a" + String.join(", ", names));
        ctx.broadcast("§e§lRound Over!" + (names.isEmpty() ? "" : " §7Survivors: §a" + names.size()));
        for (Player p : ctx.participants()) {
            p.showTitle(Title.title(Component.text("Round Over!", NamedTextColor.RED, TextDecoration.BOLD), Component.empty(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(2500), Duration.ofMillis(400))));
        }
        for (UUID id : alive) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) { p.getInventory().clear(); p.setVelocity(new Vector()); }
        }
        phase = Phase.INTERMISSION;
        phaseTicksLeft = FastTest.secs(cfg.intermissionSeconds) * 20;
    }

    private void finishGame(String reason) {
        if (finished) return;
        finished = true;
        McmGameContext c = ctx;
        stop();
        if (c != null) c.finish(reason);
    }

    private void updateHud() {
        if (ctx == null) return;
        int left, total;
        List<String> lines = new ArrayList<>();
        lines.add("§eRound: §f" + round + "/" + cfg.rounds);
        lines.add("§eMap: §b" + (map != null ? map.gen.displayName() : "-"));
        if (phase == Phase.ACTIVE || phase == Phase.TNT_TIME) {
            total = cfg.roundSeconds;
            left = Math.max(0, cfg.roundSeconds - roundTick / 20);
            if (phase == Phase.ACTIVE) {
                int s = Math.max(0, cfg.tntTimeAtSeconds - roundTick / 20);
                lines.add("§cTNT Time starts in: §f" + (s / 60) + ":" + String.format("%02d", s % 60));
            } else {
                lines.add("§cTime left: §f" + (left / 60) + ":" + String.format("%02d", left % 60));
            }
        } else {
            left = Math.max(0, (phaseTicksLeft + 19) / 20);
            total = Math.max(1, phase == Phase.COUNTDOWN ? cfg.countdownSeconds : cfg.intermissionSeconds);
            lines.add((phase == Phase.COUNTDOWN ? "§eGame begins: §f" : "§eNext Round: §f") + String.format("00:%02d", left));
        }
        ctx.setTimer(left, total);
        lines.add("§aPlayers Alive: §f" + alive.size() + "/" + startCount);
        ctx.setSidebarLines(lines);
    }

    // ------------------------------------------------------------------ listeners

    private boolean inArena(Player p) {
        return ctx != null && phase != Phase.IDLE && world != null && p.getWorld().equals(world);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!inArena(p) || !alive.contains(p.getUniqueId())) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null) return;
        if (isLauncher(item)) {
            e.setCancelled(true);
            if (e.getHand() == EquipmentSlot.HAND) fire(p);
        } else if (item.getType() == Material.FIREWORK_ROCKET) {
            // vanilla elytra boost; "You can use fireworks ONLY during elytra flight" (MCC 13 tutorial)
            if (!p.isGliding() || phase != Phase.ACTIVE) e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball sb) || world == null || !sb.getWorld().equals(world)) return;
        for (Rocket r : rockets) {
            if (r.entity() != null && r.entity().getUniqueId().equals(sb.getUniqueId())) {
                e.setCancelled(true);   // the swept ray in tickRockets() decides hits
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !inArena(p)) return;
        if (cfg.punching && e instanceof EntityDamageByEntityEvent be && be.getDamager() instanceof Player attacker
                && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && (phase == Phase.ACTIVE || phase == Phase.TNT_TIME)
                && alive.contains(p.getUniqueId()) && alive.contains(attacker.getUniqueId())) {
            e.setDamage(0.0);        // vanilla knockback, no damage
            String at = ctx.teamOf(attacker.getUniqueId());
            if (at == null || !at.equals(ctx.teamOf(p.getUniqueId()))) {
                lastHit.put(p.getUniqueId(), new Hit(attacker.getUniqueId(), System.currentTimeMillis()));
            }
            // MCC 13 tutorial: "The melee attack on your rocket launcher (shovel) has high knockback"
            if (cfg.meleeKnockback > 0 && isLauncher(attacker.getInventory().getItemInMainHand())) {
                Vector dir = p.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0);
                if (dir.lengthSquared() < 1e-4) dir = attacker.getLocation().getDirection().setY(0);
                Vector push = dir.normalize().multiply(cfg.meleeKnockback).setY(0.35);
                Bukkit.getScheduler().runTask(plugin, () -> p.setVelocity(p.getVelocity().add(push)));
            }
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler public void onItemDamage(PlayerItemDamageEvent e) { if (inArena(e.getPlayer())) e.setCancelled(true); }
    @EventHandler public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && inArena(p) && alive.contains(p.getUniqueId())) e.setCancelled(true);
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
        if (ctx != null && alive.contains(e.getPlayer().getUniqueId())) {
            eliminate(e.getPlayer().getUniqueId());
            if (roundOver()) endRound(false);
        }
    }

    /** Test/debug summary for /rs debug. */
    String debugLine() {
        return "phase=" + phase + " round=" + round + " roundTick=" + roundTick + " alive=" + alive.size()
                + " map=" + currentMap() + " rockets=" + rocketsFired + " blasted=" + blocksBlasted + " tnt=" + tntSpawned
                + " jumpV=" + String.format("%.3f", cfg.rocketJumpVelocity) + " time=" + (world == null ? -1 : world.getTime());
    }
}
