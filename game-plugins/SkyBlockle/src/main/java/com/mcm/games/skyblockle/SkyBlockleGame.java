package com.mcm.games.skyblockle;

import com.mcm.api.McmFx;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Sky Blockle: one 18-minute round, one life, last side standing. State machine + scoring + HUD. */
public final class SkyBlockleGame implements McmGame {
    enum State { IDLE, PREPARING, COUNTDOWN, LIVE, ENDING }

    private static final ChatColor[] SOLO_COLOURS = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.DARK_GREEN, ChatColor.DARK_AQUA,
            ChatColor.AQUA, ChatColor.BLUE, ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE, ChatColor.WHITE, ChatColor.GRAY};

    final JavaPlugin plugin;
    final MapWorld map;
    private final Random random = new Random();

    McmGameContext ctx;
    State state = State.IDLE;
    Settings settings;
    LootTables loot;
    BorderSchedule border;
    int elapsed;
    private boolean finished;
    private int token;
    private BukkitTask tickTask, finishTask, countdownTask, introTask, celebrateTask;
    private int lastSegment = Integer.MIN_VALUE;
    private final Set<Integer> refillsDone = new LinkedHashSet<>();

    final Map<UUID, String> sideOf = new HashMap<>();
    final Map<String, List<UUID>> sideMembers = new LinkedHashMap<>();
    final Map<String, Integer> sideWedge = new HashMap<>();
    final Map<String, ChatColor> sideColour = new HashMap<>();
    final Set<UUID> alive = new LinkedHashSet<>();
    final Set<String> eliminatedSides = new LinkedHashSet<>();
    final Map<UUID, Integer> kills = new HashMap<>();
    final Map<UUID, String> names = new HashMap<>();
    final Map<UUID, UUID> lastDamager = new HashMap<>();
    /** How each eliminated player died: true = killed by an enemy player (for the admin respawn command). */
    final Map<UUID, Boolean> diedToPlayer = new HashMap<>();
    /** Seconds x20 the end of the game is held after a non-PvP death so an admin can /sbl respawn (default, not researched). */
    private static final int RESPAWN_WINDOW_TICKS = 20 * 20;
    private boolean lastDeathRespawnable;
    private String lastDeathName;
    private org.bukkit.scheduler.BukkitTask endHoldTask;
    final Map<UUID, Long> lastDamageAt = new HashMap<>();
    int totalSides;
    /** Lootable graves of dead players (video research). */
    Graves graves;
    /** Final place of each side (1 = best), assigned when a side is eliminated and at the end. */
    final Map<String, Integer> sidePlace = new HashMap<>();
    /** Points this player earned in this game (for the end summary). */
    final Map<UUID, Integer> earned = new HashMap<>();
    private BukkitTask particleTask;

    void award(UUID u, int pts, String reason) {
        if (pts == 0 || ctx == null) return;
        ctx.addPoints(u, pts, reason);
        earned.merge(u, pts, Integer::sum);
    }

    public SkyBlockleGame(JavaPlugin plugin) {
        this.plugin = plugin;
        this.map = new MapWorld(plugin);
        this.graves = new Graves(map);
    }

    @Override public String id() { return "skyblockle"; }
    @Override public String displayName() { return "Sky Blockle"; }
    @Override public Material icon() { return Material.GRASS_BLOCK; }
    @Override public String description() { return "Skyblock battle royale: mine, craft, loot, bridge to the castle. One life."; }
    @Override public boolean useCoreScoreboard() { return true; }

    boolean active() { return state != State.IDLE && ctx != null; }
    boolean isAlive(UUID u) { return alive.contains(u); }
    boolean pvpOn() { return state == State.LIVE && elapsed >= settings.graceSeconds; }
    boolean sameSide(UUID a, UUID b) {
        String sa = sideOf.get(a), sb = sideOf.get(b);
        return sa != null && sa.equals(sb);
    }
    int buildCapY() { return map.maxWorldY() + (settings == null ? 24 : settings.buildAbove); }
    int voidY() { return map.minWorldY() - (settings == null ? 12 : settings.voidDepth); }

    Settings loadSettings() {
        plugin.reloadConfig();
        return Settings.load(plugin.getConfig(), plugin.getLogger());
    }

    LootTables loadLoot() {
        File f = new File(plugin.getDataFolder(), "loot.yml");
        if (!f.exists()) plugin.saveResource("loot.yml", false);
        return LootTables.load(f, plugin.getLogger());
    }

    // ================================================================ lifecycle

    @Override
    public void start(McmGameContext context) {
        if (state != State.IDLE) stop();
        this.ctx = context;
        this.finished = false;
        resetData();
        settings = loadSettings();
        loot = loadLoot();
        final int myToken = ++token;

        World w = map.ensureWorld();
        if (w == null) {
            context.broadcast(ChatColor.RED + "Sky Blockle: could not create the world.");
            finishOnce("World could not be created");
            return;
        }
        applyWorldRules(w);

        // sides -> wedges (one wedge per side, evenly spaced; more sides than wedges share wedges)
        Map<String, List<Player>> sides = context.sides();
        List<String> keys = new ArrayList<>(sides.keySet());
        int wedges = Math.max(settings.minWedges, Math.min(settings.maxWedges, Math.max(1, keys.size())));
        Map<Integer, ChatColor> wedgeColours = new HashMap<>();
        int solo = 0;
        for (int i = 0; i < keys.size(); i++) {
            String k = keys.get(i);
            List<UUID> members = new ArrayList<>();
            for (Player p : sides.get(k)) {
                members.add(p.getUniqueId());
                sideOf.put(p.getUniqueId(), k);
                names.put(p.getUniqueId(), p.getName());
                alive.add(p.getUniqueId());
            }
            sideMembers.put(k, members);
            int wedge = i % wedges;
            sideWedge.put(k, wedge);
            ChatColor c = k.startsWith("TEAM:") ? context.teamColor(k.substring(5)) : SOLO_COLOURS[solo++ % SOLO_COLOURS.length];
            sideColour.put(k, c);
            wedgeColours.putIfAbsent(wedge, c);
        }
        totalSides = keys.size();
        if (keys.size() > wedges) {
            plugin.getLogger().warning("Sky Blockle: " + keys.size() + " sides but only " + wedges + " wedges; some sides share a start island");
        }

        if (alive.isEmpty()) {
            context.broadcast(ChatColor.RED + "Sky Blockle: nobody to play.");
            finishOnce("No players");
            return;
        }
        state = State.PREPARING;
        context.setTimer(settings.gameSeconds, settings.gameSeconds);
        context.setSidebarLines(List.of(ChatColor.GRAY + "Generating the sky...", ChatColor.GRAY + "Sides: " + ChatColor.WHITE + totalSides));
        context.broadcast(ChatColor.AQUA + "Sky Blockle: generating the map...");

        final long seed = settings.seed != 0 ? settings.seed : random.nextLong();
        final String mode = settings.generator;
        final int fw = wedges;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String[] used = new String[1];
            MapModel m;
            try {
                m = MapWorld.generate(mode, fw, seed, plugin.getLogger(), used);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Sky Blockle: map generation failed", t);
                m = null;
            }
            final MapModel model = m;
            final String gen = used[0];
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (myToken != token || state != State.PREPARING) return;
                if (model == null) {
                    ctx.broadcast(ChatColor.RED + "Sky Blockle: map generation failed.");
                    finishOnce("Map generation failed");
                    return;
                }
                plugin.getLogger().info("Sky Blockle: map from " + gen + " (wedges " + fw + ", seed " + seed + ", "
                        + model.blocks.size() + " blocks, " + model.chests.size() + " chests, " + model.spawns.size() + " spawns)");
                border = settings.border(model, plugin.getLogger());
                map.build(model, settings, loot, random, wedgeColours,
                        p -> { if (myToken == token && ctx != null) McmFx.actionBar(ctx.participants(), ChatColor.AQUA + "Building the sky... " + ChatColor.WHITE + (int) Math.round(p * 100) + "%"); },
                        () -> { if (myToken == token && state == State.PREPARING) beginCountdown(); });
            });
        });
    }

    private void applyWorldRules(World w) {
        w.setDifficulty(settings.difficulty);
        w.setGameRule(GameRule.NATURAL_REGENERATION, settings.naturalRegen);
        w.setGameRule(GameRule.FALL_DAMAGE, settings.fallDamage);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.KEEP_INVENTORY, false);
        w.setPVP(true);
        resetBorder();
    }

    private void beginCountdown() {
        state = State.COUNTDOWN;
        World w = map.world();
        MapModel m = map.model();
        // teleport sides to their wedge's start spawns
        Map<Integer, Integer> nextSpawn = new HashMap<>();
        for (Map.Entry<String, List<UUID>> e : sideMembers.entrySet()) {
            int wedge = sideWedge.getOrDefault(e.getKey(), 0);
            List<MapModel.Spawn> spawns = m.spawnsFor(wedge);
            for (UUID u : e.getValue()) {
                Player p = Bukkit.getPlayer(u);
                if (p == null || !alive.contains(u)) continue;
                Location to;
                if (spawns.isEmpty()) {
                    plugin.getLogger().warning("Sky Blockle: wedge " + wedge + " has no spawns; using the castle");
                    to = map.castleCentre();
                } else {
                    int i = nextSpawn.merge(wedge, 1, Integer::sum) - 1;
                    MapModel.Spawn s = spawns.get(i % spawns.size());
                    to = map.toWorld(s.x(), s.y(), s.z());
                    to.setYaw(s.yaw());
                }
                preparePlayer(p);
                p.teleport(to);
            }
        }
        // everyone else online who is in our world/participating: spectate
        for (Player p : ctx.participants()) {
            if (!alive.contains(p.getUniqueId())) makeSpectator(p, map.overview());
        }
        WorldBorder wb = w.getWorldBorder();
        Location c = map.castleCentre();
        wb.setCenter(c.getX(), c.getZ());
        wb.setSize(2 * border.initialRadius());
        wb.setDamageAmount(0);
        wb.setDamageBuffer(0);
        wb.setWarningDistance((int) Math.max(1, settings.warnDistance));
        wb.setWarningTime(15);

        List<Player> players = new ArrayList<>(ctx.participants());
        McmFx.titleCard(players, ChatColor.GREEN + "" + ChatColor.BOLD + "Sky Blockle",
                ChatColor.GRAY + "Mine, craft, loot - last side standing wins");
        ctx.broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "Sky Blockle " + ChatColor.GRAY + "- one life. Gather, craft and bridge to the centre castle. "
                + (settings.graceSeconds > 0 ? "PvP turns on after " + fmt(settings.graceSeconds) + ". " : "")
                + "The border closes in on the castle.");
        updateSidebar();
        final int myToken = token;
        introTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            introTask = null;
            if (myToken != token || state != State.COUNTDOWN) return;
            countdownTask = McmFx.countdown(plugin, ctx.participants(), FastTest.secs(settings.countdownSeconds), ChatColor.GRAY + "Get ready!", () -> {
                countdownTask = null;
                if (myToken == token && state == State.COUNTDOWN) go();
            });
        }, FastTest.ticks(80L));
    }

    private void go() {
        state = State.LIVE;
        elapsed = 0;
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            PlayerInventory inv = p.getInventory();
            for (ItemSpec s : settings.startInventory) {
                Map<Integer, ItemStack> left = inv.addItem(s.build(random));
                for (ItemStack it : left.values()) p.getWorld().dropItem(p.getLocation(), it);
            }
        }
        if (settings.graceSeconds > 0) {
            ctx.broadcast(ChatColor.YELLOW + "Grace period: PvP is off for " + fmt(settings.graceSeconds) + ".");
        }
        lastSegment = Integer.MIN_VALUE;
        updateBorder();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        if (settings.borderEnabled && settings.particlesDistance > 0) {
            particleTask = Bukkit.getScheduler().runTaskTimer(plugin, this::borderParticles, 10L, 10L);
        }
        ctx.setTimer(settings.gameSeconds, settings.gameSeconds);
        updateSidebar();
    }

    @Override
    public void stop() {
        token++;
        cancelTasks();
        boolean wasActive = state != State.IDLE;
        state = State.IDLE;
        try { resetPlayersInWorld(); } catch (Throwable t) { plugin.getLogger().warning("Sky Blockle: reset players failed: " + t); }
        resetBorder();
        map.cancel();
        if (wasActive && plugin.isEnabled()) {
            try { map.wipe(null); } catch (Throwable t) { plugin.getLogger().warning("Sky Blockle: wipe failed: " + t); }
        }
        ctx = null;
        resetData();
    }

    private void cancelTasks() {
        for (BukkitTask t : new BukkitTask[]{tickTask, finishTask, countdownTask, introTask, celebrateTask, particleTask}) {
            if (t != null) try { t.cancel(); } catch (Throwable ignored) {}
        }
        tickTask = finishTask = countdownTask = introTask = celebrateTask = particleTask = null;
    }

    private void resetData() {
        graves.clear();
        sidePlace.clear(); earned.clear();
        sideOf.clear(); sideMembers.clear(); sideWedge.clear(); sideColour.clear();
        alive.clear(); eliminatedSides.clear(); kills.clear(); names.clear();
        lastDamager.clear(); lastDamageAt.clear(); refillsDone.clear(); diedToPlayer.clear();
        lastDeathRespawnable = false; lastDeathName = null;
        if (endHoldTask != null) { endHoldTask.cancel(); endHoldTask = null; }
        totalSides = 0; elapsed = 0; lastSegment = Integer.MIN_VALUE;
    }

    void resetBorder() {
        World w = map.world();
        if (w == null) return;
        WorldBorder wb = w.getWorldBorder();
        wb.setCenter(0, 0);
        wb.setSize(59_999_968);
        wb.setDamageAmount(0);
    }

    private void finishOnce(String reason) {
        if (finished || ctx == null) return;
        finished = true;
        McmGameContext c = ctx;
        token++;
        cancelTasks();
        state = State.IDLE;
        try { resetPlayersInWorld(); } catch (Throwable t) { plugin.getLogger().warning("Sky Blockle: reset players failed: " + t); }
        try { c.finish(reason); }
        finally {
            resetBorder();
            if (plugin.isEnabled()) map.wipe(null);
            ctx = null;
            resetData();
        }
    }

    // ================================================================ tick

    private void tick() {
        if (state != State.LIVE || ctx == null) return;
        elapsed++;
        int left = Math.max(0, settings.gameSeconds - elapsed);
        ctx.setTimer(left, settings.gameSeconds);

        if (settings.graceSeconds > 0) {
            if (elapsed == settings.graceSeconds) {
                ctx.broadcast(ChatColor.RED + "" + ChatColor.BOLD + "PvP is now ON!");
                McmFx.title(ctx.participants(), "", ChatColor.RED + "PvP is now ON", 5, 40, 10);
                McmFx.sound(ctx.participants(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
            } else if (settings.graceSeconds - elapsed == 10) {
                ctx.broadcast(ChatColor.YELLOW + "PvP turns on in 10 seconds.");
            }
        }
        for (int t : settings.refillAt) {
            if (t == elapsed && refillsDone.add(t)) {
                map.refill(settings, loot, random);
                ctx.broadcast(ChatColor.GOLD + "Chests have been refilled!");
                McmFx.sound(ctx.participants(), Sound.BLOCK_CHEST_OPEN, 0.8f, 1f);
            }
        }
        updateBorder();
        borderDamageAndHud();
        guardPlayers();
        updateSidebar();
        if (elapsed >= settings.gameSeconds) endGame(true, "Time is up");
    }

    private void updateBorder() {
        World w = map.world();
        if (w == null || border == null) return;
        WorldBorder wb = w.getWorldBorder();
        if (!settings.borderEnabled) { wb.setSize(59_999_968); return; }
        int seg = border.segmentAt(elapsed);
        if (seg == lastSegment) {
            // warn 30 s before a move
            double next = border.nextMoveStart(elapsed);
            if (!Double.isNaN(next) && (int) next - elapsed == 30) {
                ctx.broadcast(ChatColor.RED + "[!] The border will shrink in 30 seconds! Red = dead.");
            }
            if (settings.lastStandSeconds > 0 && elapsed == settings.lastStandAt) {
                ctx.broadcast(ChatColor.DARK_RED + "" + ChatColor.BOLD + "LAST STAND! " + ChatColor.RED + fmt(settings.lastStandSeconds) + " to go!");
                McmFx.title(ctx.participants(), ChatColor.DARK_RED + "" + ChatColor.BOLD + "LAST STAND", "", 5, 40, 10);
            }
            return;
        }
        lastSegment = seg;
        if (seg >= 0 && border.movingAt(elapsed)) {
            long secs = Math.max(1, Math.round(border.segmentEndTime(seg) - elapsed));
            wb.setSize(2 * border.radiusAt(elapsed));
            wb.setSize(2 * border.segmentEndRadius(seg), secs);
            if (elapsed > 0) {
                ctx.broadcast(ChatColor.RED + "[!] Border shrinking! " + ChatColor.GRAY + "(to " + (int) Math.round(border.segmentEndRadius(seg))
                        + " blocks from the centre over " + fmt((int) secs) + ")");
                McmFx.title(ctx.participants(), "", ChatColor.RED + "Border shrinking", 5, 40, 10);
                McmFx.sound(ctx.participants(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 0.7f);
            }
            if (settings.lastStandSeconds > 0 && elapsed == settings.lastStandAt) {
                ctx.broadcast(ChatColor.DARK_RED + "" + ChatColor.BOLD + "LAST STAND! " + ChatColor.RED + fmt(settings.lastStandSeconds) + " to go!");
                McmFx.title(ctx.participants(), ChatColor.DARK_RED + "" + ChatColor.BOLD + "LAST STAND", ChatColor.RED + "Border shrinking", 5, 40, 10);
            }
        } else {
            wb.setSize(2 * border.radiusAt(elapsed));
        }
    }

    /** Current border half-size (vanilla border, interpolating while it moves). */
    double borderRadius() {
        World w = map.world();
        return w == null ? 0 : w.getWorldBorder().getSize() / 2.0;
    }

    /** Blocks outside the border (> 0 = outside), Chebyshev distance because the vanilla border is square. */
    double outsideBy(Location l) {
        World w = map.world();
        if (w == null) return -1;
        WorldBorder wb = w.getWorldBorder();
        Location c = wb.getCenter();
        double r = wb.getSize() / 2.0;
        return Math.max(Math.abs(l.getX() - c.getX()), Math.abs(l.getZ() - c.getZ())) - r;
    }

    private static final org.bukkit.Particle.DustOptions WALL_DUST =
            new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(0x40E0E8), 2.0f);

    /** Cyan particle wall on the border edges near each player in the world (only that player sees it). */
    private void borderParticles() {
        World w = map.world();
        if (w == null || state != State.LIVE || settings == null) return;
        WorldBorder wb = w.getWorldBorder();
        double cx = wb.getCenter().getX(), cz = wb.getCenter().getZ(), r = wb.getSize() / 2.0;
        int near = settings.particlesDistance;
        for (Player p : w.getPlayers()) {
            Location l = p.getLocation();
            double px = l.getX(), py = l.getY(), pz = l.getZ();
            double[][] edges = {{cx + r, 0}, {cx - r, 0}, {cz + r, 1}, {cz - r, 1}};
            for (double[] e : edges) {
                boolean alongZ = e[1] == 0; // wall at fixed x, runs along z
                double d = Math.abs((alongZ ? px : pz) - e[0]);
                if (d > near) continue;
                double centre = alongZ ? pz : px;
                for (int t = -14; t <= 14; t += 2) {
                    double a = Math.floor(centre) + t;
                    if (Math.abs(a - (alongZ ? cz : cx)) > r) continue;
                    for (int dy = -3; dy <= 5; dy += 2) {
                        double x = alongZ ? e[0] : a, z = alongZ ? a : e[0];
                        p.spawnParticle(org.bukkit.Particle.DUST, x, py + dy, z, 1, 0.3, 0.3, 0.3, 0, WALL_DUST);
                    }
                }
            }
        }
    }

    private void borderDamageAndHud() {
        double dps = border == null ? 1 : border.damageAt(elapsed);
        for (UUID u : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(u);
            if (p == null || p.getWorld() != map.world()) continue;
            double out = settings.borderEnabled ? outsideBy(p.getLocation()) : -1e9;
            if (out > 0) {
                McmFx.actionBar(p, ChatColor.RED + "" + ChatColor.BOLD + "⚠ OUTSIDE THE BORDER " + ChatColor.RESET + ChatColor.RED + "- get back in! ("
                        + String.format(java.util.Locale.ROOT, "%.1f", dps / 2.0) + " hearts/s)");
                try {
                    p.damage(dps, DamageSource.builder(DamageType.OUTSIDE_BORDER).build());
                } catch (Throwable t) {
                    p.damage(dps);
                }
            } else if (out > -settings.warnDistance) {
                McmFx.actionBar(p, ChatColor.GOLD + "⚠ Border " + (int) Math.ceil(-out) + " blocks away" + ChatColor.GRAY + "  |  Kills: " + ChatColor.WHITE + kills.getOrDefault(u, 0));
            } else {
                McmFx.actionBar(p, ChatColor.GRAY + "Kills: " + ChatColor.WHITE + kills.getOrDefault(u, 0)
                        + (pvpOn() ? "" : ChatColor.GRAY + "  |  " + ChatColor.GREEN + "PvP in " + fmt(settings.graceSeconds - elapsed)));
            }
        }
    }

    /** Void check (backup for the move listener) and spectator confinement. */
    private void guardPlayers() {
        World w = map.world();
        if (w == null) return;
        for (Player p : new ArrayList<>(w.getPlayers())) {
            UUID u = p.getUniqueId();
            if (alive.contains(u)) {
                map.markDirtyBlock(p.getLocation().getBlockX(), p.getLocation().getBlockZ());
                if (p.getLocation().getY() < voidY()) eliminate(u, p, "fell out of the world", true);
            } else if (p.getGameMode() == GameMode.SPECTATOR) {
                confineSpectator(p);
            }
        }
    }

    void confineSpectator(Player p) {
        Location l = p.getLocation();
        Location c = map.castleCentre();
        double max = map.mapRadius() + settings.spectatorMargin;
        double dx = l.getX() - c.getX(), dz = l.getZ() - c.getZ();
        double d = Math.sqrt(dx * dx + dz * dz);
        double minY = map.minWorldY() - settings.spectatorMargin, maxY = map.maxWorldY() + settings.spectatorMargin * 2;
        if (d <= max && l.getY() >= minY && l.getY() <= maxY) return;
        Location to = l.clone();
        if (d > max) { to.setX(c.getX() + dx / d * (max - 2)); to.setZ(c.getZ() + dz / d * (max - 2)); }
        to.setY(Math.max(minY + 2, Math.min(maxY - 2, l.getY())));
        p.teleport(to);
    }

    void updateSidebar() {
        if (ctx == null) return;
        List<String> lines = new ArrayList<>();
        // layout per video research: Next Shrink / LAST STAND, blank, Players Alive, Teams Alive (+ border, kills)
        int t = state == State.LIVE || state == State.ENDING ? elapsed : 0;
        if (border != null && settings.borderEnabled) {
            if (settings.lastStandSeconds > 0 && t >= settings.lastStandAt && t < settings.lastStandAt + settings.lastStandSeconds) {
                lines.add(ChatColor.DARK_RED + "" + ChatColor.BOLD + "LAST STAND: " + ChatColor.RED + fmt(settings.lastStandAt + settings.lastStandSeconds - t));
            } else {
                double next = border.nextMoveStart(t);
                lines.add(Double.isNaN(next) ? ChatColor.GRAY + "Next Shrink: " + ChatColor.DARK_GRAY + "-"
                        : ChatColor.GRAY + "Next Shrink: " + ChatColor.YELLOW + fmt((int) Math.max(0, next - t)));
            }
        }
        lines.add(" ");
        lines.add(ChatColor.GRAY + "Players Alive: " + ChatColor.WHITE + alive.size() + ChatColor.GRAY + "/" + sideOf.size());
        lines.add(ChatColor.GRAY + "Teams Alive: " + ChatColor.WHITE + aliveSides().size() + ChatColor.GRAY + "/" + totalSides);
        if (border != null && settings.borderEnabled) {
            lines.add(ChatColor.GRAY + "Border: " + ChatColor.WHITE + (int) Math.round(borderRadius())
                    + (state == State.LIVE && border.movingAt(elapsed) ? ChatColor.RED + " shrinking" : ""));
        }
        if (settings.graceSeconds > 0 && state != State.ENDING) {
            lines.add(pvpOn() ? ChatColor.RED + "PvP: ON" : ChatColor.GREEN + "PvP in " + fmt(settings.graceSeconds - (state == State.LIVE ? elapsed : 0)));
        }
        UUID top = null;
        int best = 0;
        for (Map.Entry<UUID, Integer> e : kills.entrySet()) if (e.getValue() > best) { best = e.getValue(); top = e.getKey(); }
        lines.add(ChatColor.GRAY + "Top kills: " + (top == null ? ChatColor.DARK_GRAY + "-" : coloured(top) + ChatColor.GRAY + " (" + best + ")"));
        ctx.setSidebarLines(lines);
    }

    // ================================================================ deaths & scoring

    /** The player who gets credit for a death of {@code victim}: last enemy attacker within the window, else null. */
    UUID creditFor(UUID victim) {
        UUID k = lastDamager.get(victim);
        Long at = lastDamageAt.get(victim);
        if (k == null || at == null) return null;
        if (System.currentTimeMillis() - at > settings.killCreditSeconds * 1000L) return null;
        if (k.equals(victim)) return null;
        return k;
    }

    void recordHit(UUID victim, UUID attacker) {
        lastDamager.put(victim, attacker);
        lastDamageAt.put(victim, System.currentTimeMillis());
    }

    /**
     * Eliminate a player (death of any cause or a quit). {@code p} may be null if offline. Safe to call twice.
     * {@code voidDeath} drops nothing (config). {@code spectate} = make them a spectator (false for quitters).
     */
    void eliminate(UUID victim, Player p, String cause, boolean voidDeath) { eliminate(victim, p, cause, voidDeath, true); }

    void eliminate(UUID victim, Player p, String cause, boolean voidDeath, boolean spectate) {
        if (ctx == null || !alive.remove(victim)) return;
        String side = sideOf.get(victim);
        UUID killer = creditFor(victim);
        boolean scoring = state == State.LIVE;
        boolean enemyKill = killer != null && !sameSide(killer, victim);
        diedToPlayer.put(victim, enemyKill);
        lastDeathRespawnable = !enemyKill && p != null && p.isOnline() && spectate;
        lastDeathName = names.get(victim);
        if (killer != null) {
            if (enemyKill) {
                kills.merge(killer, 1, Integer::sum);
                if (scoring) award(killer, settings.killPoints, "Sky Blockle elimination");
            } else if (scoring) {
                award(killer, settings.teamKillPoints, "Sky Blockle team kill");
            }
        }
        Player kp = killer == null ? null : Bukkit.getPlayer(killer);

        // +N to every other living player
        if (scoring) for (UUID u : new ArrayList<>(alive)) award(u, settings.surviveDeathPoints, "Sky Blockle survival");

        // chat feed (video research): "[+N] <victim> <death message>" for the living, plain for everyone else;
        // "[+N] You killed <victim>!" for the killer
        String death = coloured(victim) + ChatColor.GRAY + " " + cause
                + (killer != null && enemyKill ? (cause.startsWith("was ") ? " by " : " whilst fighting ") + coloured(killer) : "");
        for (Player o : Bukkit.getOnlinePlayers()) {
            boolean living = alive.contains(o.getUniqueId());
            String prefix = scoring && living && settings.surviveDeathPoints != 0 ? ChatColor.GREEN + "[+" + settings.surviveDeathPoints + "] " : ChatColor.RED + "☠ ";
            o.sendMessage(prefix + death);
        }
        if (kp != null && enemyKill) {
            kp.sendMessage((scoring && settings.killPoints != 0 ? ChatColor.GOLD + "[+" + settings.killPoints + "] " : "") + ChatColor.GOLD + "You killed " + coloured(victim) + ChatColor.GOLD + "!");
            McmFx.actionBar(kp, ChatColor.GOLD + "⚔ Eliminated " + coloured(victim));
            McmFx.sound(kp, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
        }
        if (p != null && p.isOnline()) {
            McmFx.title(p, ChatColor.RED + "" + ChatColor.BOLD + "ELIMINATED", kp != null && enemyKill ? ChatColor.GRAY + "by " + coloured(killer) : "", 0, 40, 15);
            McmFx.sound(p, Sound.ENTITY_ITEM_BREAK, 1f, 0.6f);
            McmFx.burst(p.getLocation().add(0, 1, 0), McmFx.colorOf(side == null ? ChatColor.WHITE : sideColour.getOrDefault(side, ChatColor.WHITE)), 40);
        }
        if (scoring && settings.surviveDeathPoints != 0) McmFx.actionBar(onlineOf(alive), ChatColor.GREEN + "+" + settings.surviveDeathPoints + " survival");
        if (settings.announceRemaining.contains(alive.size()) && alive.size() > 0 && aliveSides().size() > 1) {
            ctx.broadcast(ChatColor.YELLOW + "[▶] Only " + alive.size() + " players remain!");
        }

        if (p != null && p.isOnline()) {
            World w = map.world();
            boolean inWorld = w != null && p.getWorld() == w;
            Location deathAt = p.getLocation();
            boolean onMap = inWorld && !voidDeath && deathAt.getY() > voidY();
            boolean graved = onMap && settings.grave && graves.create(p, deathAt, buildCapY());
            if (graved && settings.dropXp) {
                int xp = Math.min(p.getLevel() * 7, 100);
                if (xp > 0) w.spawn(deathAt, ExperienceOrb.class, o -> o.setExperience(xp));
            }
            boolean drop = !graved && inWorld && (voidDeath ? settings.voidDropItems : settings.dropItems) && deathAt.getY() > voidY();
            PlayerInventory inv = p.getInventory();
            if (drop) {
                for (ItemStack it : inv.getContents()) if (it != null && !it.getType().isAir()) w.dropItemNaturally(deathAt, it);
                if (settings.dropXp) {
                    int xp = Math.min(p.getLevel() * 7, 100);
                    if (xp > 0) w.spawn(deathAt, ExperienceOrb.class, o -> o.setExperience(xp));
                }
            }
            inv.clear();
            p.setLevel(0);
            p.setExp(0);
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            p.setFireTicks(0);
            p.setFallDistance(0);
            if (!p.isDead()) heal(p);
            if (spectate && inWorld) {
                Location to = voidDeath || deathAt.getY() < map.minWorldY() ? map.overview() : deathAt.clone().add(0, 2, 0);
                makeSpectator(p, to);
                p.sendMessage(ChatColor.GRAY + "You are now spectating.");
            }
        }

        if (side != null && !eliminatedSides.contains(side)) {
            boolean any = false;
            for (UUID u : sideMembers.getOrDefault(side, List.of())) if (alive.contains(u)) { any = true; break; }
            if (!any) {
                eliminatedSides.add(side);
                sidePlace.put(side, aliveSides().size() + 1);
                McmFx.teamEliminated(side, ctx);
            }
        }
        updateSidebar();
        checkEnd();
    }

    /**
     * Admin respawn (owner request): put an eliminated player back in the game on their side's start island with
     * the starting kit. By default only for deaths that were not caused by an enemy player (void, fall, border...);
     * {@code force} allows any. Returns null on success or the reason it was refused.
     */
    String adminRespawn(Player p, boolean force) {
        if (ctx == null || state != State.LIVE) return "No Sky Blockle game is live (a game that has already ended cannot be re-opened).";
        UUID u = p.getUniqueId();
        String side = sideOf.get(u);
        if (side == null) return p.getName() + " is not a participant of this game.";
        if (alive.contains(u)) return p.getName() + " is still alive.";
        if (!force && Boolean.TRUE.equals(diedToPlayer.get(u))) return p.getName() + " was killed by a player. Add 'force' to respawn them anyway.";
        World w = map.world();
        if (w == null) return "The map world is not loaded.";
        List<MapModel.Spawn> spawns = map.model().spawnsFor(sideWedge.getOrDefault(side, 0));
        Location to;
        if (spawns.isEmpty()) to = map.castleCentre();
        else {
            MapModel.Spawn sp = spawns.get(random.nextInt(spawns.size()));
            to = map.toWorld(sp.x(), sp.y(), sp.z());
            to.setYaw(sp.yaw());
        }
        // the start island may have been mined away or be outside the border: make sure there is something to stand on
        org.bukkit.block.Block below = to.clone().add(0, -1, 0).getBlock();
        if (below.getType().isAir()) below.setType(org.bukkit.Material.COBBLESTONE, false);
        alive.add(u);
        diedToPlayer.remove(u);
        lastDamager.remove(u);
        lastDamageAt.remove(u);
        if (eliminatedSides.remove(side)) sidePlace.remove(side);
        preparePlayer(p);
        p.teleport(to);
        PlayerInventory inv = p.getInventory();
        for (ItemSpec is : settings.startInventory) {
            Map<Integer, ItemStack> left = inv.addItem(is.build(random));
            for (ItemStack it : left.values()) p.getWorld().dropItem(p.getLocation(), it);
        }
        if (endHoldTask != null) { endHoldTask.cancel(); endHoldTask = null; }
        ctx.broadcast(ChatColor.AQUA + "[▶] " + coloured(u) + ChatColor.AQUA + " was respawned by an admin.");
        McmFx.title(p, ChatColor.AQUA + "" + ChatColor.BOLD + "RESPAWNED", ChatColor.GRAY + "back on your island", 0, 40, 15);
        updateSidebar();
        return null;
    }

    List<String> aliveSides() {
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, List<UUID>> e : sideMembers.entrySet()) {
            for (UUID u : e.getValue()) if (alive.contains(u)) { out.add(e.getKey()); break; }
        }
        return out;
    }

    private void checkEnd() {
        if (state != State.LIVE && state != State.COUNTDOWN && state != State.PREPARING) return;
        int n = aliveSides().size();
        boolean over = n == 0 || (totalSides >= 2 && n == 1);
        if (!over) return;
        // Owner request: a death that was not caused by a player can be undone with /sbl respawn. Hold the end of
        // the game for a short window so an admin has time to do it (only while live, only once per death).
        if (state == State.LIVE && lastDeathRespawnable && RESPAWN_WINDOW_TICKS > 0) {
            lastDeathRespawnable = false;
            if (endHoldTask != null) endHoldTask.cancel();
            String who = lastDeathName == null ? "the player" : lastDeathName;
            for (Player o : Bukkit.getOnlinePlayers()) if (o.isOp())
                o.sendMessage(ChatColor.AQUA + "[Admin] " + who + " did not die to a player. /sbl respawn " + who
                        + ChatColor.GRAY + " within " + (RESPAWN_WINDOW_TICKS / 20) + " s to bring them back; otherwise the game ends.");
            endHoldTask = Bukkit.getScheduler().runTaskLater(plugin, () -> { endHoldTask = null; checkEnd(); }, RESPAWN_WINDOW_TICKS);
            return;
        }
        if (endHoldTask != null) return; // still inside the respawn window
        if (n == 0) endGame(false, "Everyone was eliminated");
        else endGame(false, "Last side standing");
    }

    /** End: +alive-at-end to every survivor, rank surviving sides (tiebreak at time-out), celebrate, then finish. */
    private void endGame(boolean timeout, String reason) {
        if (ctx == null || state == State.ENDING || state == State.IDLE) return;
        boolean wasLive = state == State.LIVE;
        state = State.ENDING;
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (introTask != null) { introTask.cancel(); introTask = null; }
        map.cancel(); // stop a build still running (game ended during preparation)
        World w = map.world();
        if (w != null) w.getWorldBorder().setSize(w.getWorldBorder().getSize());

        List<Player> survivors = onlineOf(alive);
        if (wasLive && settings.aliveAtEndPoints != 0) {
            for (UUID u : new ArrayList<>(alive)) award(u, settings.aliveAtEndPoints, "Sky Blockle survived");
            String msg = timeout ? "You survived until the end!" : "You and your teammates were the last players standing!";
            for (Player p : survivors) p.sendMessage(ChatColor.GREEN + "[+" + settings.aliveAtEndPoints + "] " + msg);
        }

        List<String> remaining = rankSides(aliveSides());
        for (int i = 0; i < remaining.size(); i++) {
            int place = i + 1;
            if (i > 0 && compareSides(remaining.get(i - 1), remaining.get(i)) == 0) place = sidePlace.getOrDefault(remaining.get(i - 1), place);
            sidePlace.put(remaining.get(i), place);
        }
        List<String> winners = new ArrayList<>();
        if (!remaining.isEmpty()) {
            winners.add(remaining.get(0));
            if (timeout) for (int i = 1; i < remaining.size(); i++) if (compareSides(remaining.get(0), remaining.get(i)) == 0) winners.add(remaining.get(i));
        }
        String head;
        if (winners.isEmpty()) head = ChatColor.GRAY + "No winner";
        else if (winners.size() == 1) head = ctx.sideLabel(winners.get(0)) + ChatColor.GOLD + " wins!";
        else {
            StringBuilder sb = new StringBuilder(ChatColor.GOLD + "Draw: ");
            for (int i = 0; i < winners.size(); i++) sb.append(i > 0 ? ChatColor.GRAY + ", " : "").append(ctx.sideLabel(winners.get(i)));
            head = sb.toString();
        }
        ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Sky Blockle over! " + ChatColor.RESET + head + ChatColor.GRAY + " (" + reason + ")");
        if (timeout && remaining.size() > 1) {
            StringBuilder sb = new StringBuilder(ChatColor.GRAY + "Time-out ranking (" + String.join(", ", settings.tiebreak) + "): ");
            for (int i = 0; i < remaining.size(); i++) {
                String s = remaining.get(i);
                sb.append(i > 0 ? ChatColor.GRAY + ", " : "").append(ChatColor.YELLOW).append("#").append(i + 1).append(" ")
                        .append(ctx.sideLabel(s)).append(ChatColor.GRAY).append(" (").append(aliveCount(s)).append(" alive, ")
                        .append(sideKills(s)).append(" kills)");
            }
            ctx.broadcast(sb.toString());
        }
        List<Player> everyone = new ArrayList<>(ctx.participants());
        McmFx.title(everyone, ChatColor.GOLD + "" + ChatColor.BOLD + "Game Over!", head, 5, 70, 15);
        // per-player summary (video research)
        for (Map.Entry<UUID, String> e : sideOf.entrySet()) {
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null) continue;
            Integer place = sidePlace.get(e.getValue());
            if (place != null) p.sendMessage(ChatColor.YELLOW + "You finished in " + ordinal(place) + " place!");
            p.sendMessage(ChatColor.YELLOW + "You eliminated " + kills.getOrDefault(e.getKey(), 0) + " opponent" + (kills.getOrDefault(e.getKey(), 0) == 1 ? "" : "s") + ".");
            p.sendMessage(ChatColor.YELLOW + "You earned " + earned.getOrDefault(e.getKey(), 0) + " points this round.");
        }
        McmFx.sound(everyone, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        List<Player> winnerPlayers = new ArrayList<>();
        for (String s : winners) for (UUID u : sideMembers.getOrDefault(s, List.of())) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) winnerPlayers.add(p);
        }
        for (Player p : survivors) { p.setInvulnerable(true); }
        if (!winnerPlayers.isEmpty()) {
            celebrateTask = McmFx.celebrate(plugin, winnerPlayers,
                    McmFx.colorOf(winners.size() == 1 ? sideColour.getOrDefault(winners.get(0), ChatColor.GOLD) : ChatColor.GOLD));
        }
        updateSidebar();
        final int myToken = token;
        endReason = reason;
        finishTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            finishTask = null;
            if (myToken == token) finishOnce(reason);
        }, FastTest.secs(settings.endDelaySeconds) * 20L);
    }

    // ================================================================ admin testing hooks
    // (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    private String endReason = "Sky Blockle over";

    /** Ends the pre-game freeze/countdown, the PvP grace period, or the end celebration. */
    public boolean skipWait() {
        if (ctx == null) return false;
        if (state == State.COUNTDOWN) {
            if (introTask != null) { introTask.cancel(); introTask = null; }
            if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
            go();
            return true;
        }
        if (state == State.LIVE && settings != null && !pvpOn()) {
            settings.graceSeconds = elapsed;   // settings are reloaded every start, so this only affects this game
            ctx.broadcast(ChatColor.RED + "" + ChatColor.BOLD + "PvP is now ON!" + ChatColor.GRAY + " (grace skipped by an admin)");
            McmFx.title(ctx.participants(), "", ChatColor.RED + "PvP is now ON", 5, 40, 10);
            McmFx.sound(ctx.participants(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.2f);
            updateSidebar();
            return true;
        }
        if (state == State.ENDING && finishTask != null) {
            finishTask.cancel();
            finishTask = null;
            finishOnce(endReason);
            return true;
        }
        return false;
    }

    /** One round only: nothing to skip to. */
    public boolean skipRound() { return false; }

    private int aliveCount(String side) {
        int n = 0;
        for (UUID u : sideMembers.getOrDefault(side, List.of())) if (alive.contains(u)) n++;
        return n;
    }

    private int sideKills(String side) {
        int n = 0;
        for (UUID u : sideMembers.getOrDefault(side, List.of())) n += kills.getOrDefault(u, 0);
        return n;
    }

    private double sideHealth(String side) {
        double h = 0;
        for (UUID u : sideMembers.getOrDefault(side, List.of())) {
            if (!alive.contains(u)) continue;
            Player p = Bukkit.getPlayer(u);
            if (p != null) h += p.getHealth() + p.getAbsorptionAmount();
        }
        return h;
    }

    /** Negative if a ranks above b by the configured tiebreak criteria. */
    private int compareSides(String a, String b) {
        for (String c : settings.tiebreak) {
            int r = switch (c) {
                case "players-alive", "alive", "players" -> Integer.compare(aliveCount(b), aliveCount(a));
                case "kills" -> Integer.compare(sideKills(b), sideKills(a));
                case "health" -> Double.compare(sideHealth(b), sideHealth(a));
                default -> 0;
            };
            if (r != 0) return r;
        }
        return 0;
    }

    private List<String> rankSides(List<String> sides) {
        List<String> out = new ArrayList<>(sides);
        out.sort(this::compareSides);
        return out;
    }

    // ================================================================ players

    void preparePlayer(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setInvulnerable(false);
        p.getInventory().clear();
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        heal(p);
        p.setFoodLevel(20);
        p.setSaturation(5);
        p.setExhaustion(0);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setLevel(settings.startXpLevels);
        p.setExp(0);
        p.setAllowFlight(false);
        p.setFlying(false);
    }

    private static void heal(Player p) {
        AttributeInstance max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
    }

    void makeSpectator(Player p, Location to) {
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);
        if (to != null && to.getWorld() != null) p.teleport(to);
    }

    private void resetPlayersInWorld() {
        World w = map.world();
        if (w == null) return;
        for (Player p : new ArrayList<>(w.getPlayers())) {
            p.getInventory().clear();
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            p.setInvulnerable(false);
            p.setFireTicks(0);
            p.setFallDistance(0);
            heal(p);
            p.setFoodLevel(20);
            p.setLevel(0);
            p.setExp(0);
            p.setGameMode(GameMode.SURVIVAL);
        }
    }

    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        if (state == State.IDLE || state == State.PREPARING || map.world() == null || map.model() == null) return false;
        player.getInventory().clear();
        makeSpectator(player, map.overview());
        player.sendMessage(ChatColor.GRAY + "Sky Blockle is running - you are spectating.");
        return true;
    }

    // ================================================================ helpers

    String coloured(UUID u) {
        String side = sideOf.get(u);
        ChatColor c = side == null ? ChatColor.WHITE : sideColour.getOrDefault(side, ChatColor.WHITE);
        return c + names.getOrDefault(u, "?");
    }

    static List<Player> onlineOf(Collection<UUID> ids) {
        List<Player> out = new ArrayList<>();
        for (UUID u : ids) { Player p = Bukkit.getPlayer(u); if (p != null) out.add(p); }
        return out;
    }

    static String ordinal(int n) {
        int m = n % 100;
        String suf = (m >= 11 && m <= 13) ? "th" : switch (n % 10) { case 1 -> "st"; case 2 -> "nd"; case 3 -> "rd"; default -> "th"; };
        return n + suf;
    }

    static String fmt(int secs) {
        secs = Math.max(0, secs);
        return (secs / 60) + ":" + String.format("%02d", secs % 60);
    }

    // ---------------------------------------------------------------- admin helpers

    /** /sbl build: generate + place a map outside a game for inspection. */
    void adminBuild(Player admin, int wedges, long seed) {
        if (state != State.IDLE) { admin.sendMessage(ChatColor.RED + "A game is running."); return; }
        Settings s = loadSettings();
        LootTables lt = loadLoot();
        World w = map.ensureWorld();
        if (w == null) { admin.sendMessage(ChatColor.RED + "World missing."); return; }
        long sd = seed != 0 ? seed : random.nextLong();
        admin.sendMessage(ChatColor.GRAY + "Generating " + wedges + " wedges, seed " + sd + "...");
        String[] used = new String[1];
        MapModel m = MapWorld.generate(s.generator, wedges, sd, plugin.getLogger(), used);
        Map<Integer, ChatColor> cols = new HashMap<>();
        for (int i = 0; i < wedges; i++) cols.put(i, SOLO_COLOURS[i % SOLO_COLOURS.length]);
        UUID id = admin.getUniqueId();
        map.build(m, s, lt, random, cols, p -> {
            Player a = Bukkit.getPlayer(id);
            if (a != null) McmFx.actionBar(a, ChatColor.AQUA + "Building... " + (int) Math.round(p * 100) + "%");
        }, () -> {
            Player a = Bukkit.getPlayer(id);
            if (a == null) return;
            a.sendMessage(ChatColor.GREEN + "Built map from " + used[0] + ": " + m.blocks.size() + " blocks, chests " + map.chestCounts()
                    + ", brewing stands " + map.brewingStands() + ", spawns " + m.spawns.size() + ". /sbl tp to visit, /sbl clear to wipe.");
        });
    }
}
