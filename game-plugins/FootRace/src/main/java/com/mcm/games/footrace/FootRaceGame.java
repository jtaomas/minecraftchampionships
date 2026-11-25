package com.mcm.games.footrace;

import com.mcm.api.McmGame;
import com.mcm.api.McmCoins;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.*;

public final class FootRaceGame implements McmGame, Listener {

    static final String WORLD_NAME = "mcc_footrace";
    static final int RACE_SECONDS = 300;
    static final int COUNTDOWN_SECONDS = 10;
    // MCC coins (MCC 1-6 Foot Race; base x1.0, config coins.*; the core applies the event multiplier). COINS.md.
    double lapCoins = 0;            // MCC: nothing per lap
    double finishFirst = 500;       // MCC: the 1st finisher
    double finishStep = 12.5;       // MCC: 12.5 less for every later finisher
    double finishMin = 0;           // MCC: no floor (no coins if you don't finish)

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private enum State { IDLE, BUILDING, COUNTDOWN, RACING, ENDING }

    private static final class Runner {
        final UUID id;
        final String name;
        final int slot;
        int lap;            // completed laps
        int seq;            // index into Track.ORDER of the next checkpoint
        int lastCp = -1;    // last checkpoint passed (-1 = still on the grid)
        boolean finished;
        int place;
        long finishMs, lapStartMs, bestLapMs = Long.MAX_VALUE;
        int lastLaunchTick = -1000, lastRespawnTick = -1000, lastWarnTick = -1000, lastPunchTick = -1000;

        Runner(UUID id, String name, int slot) { this.id = id; this.name = name; this.slot = slot; }
    }

    private final JavaPlugin plugin;
    private State state = State.IDLE;
    private McmGameContext ctx;
    private World world;
    private final Map<UUID, Runner> runners = new HashMap<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final List<int[]> placed = new ArrayList<>();
    private BukkitTask clearTask;
    private int finishCount;
    private long raceStartMs;
    private int secondsLeft;
    private SpectatorGuard guard;

    FootRaceGame(JavaPlugin plugin) { this.plugin = plugin; }

    // ------------------------------------------------------------------ McmGame

    @Override public String id() { return "footrace"; }
    @Override public String displayName() { return "Foot Race"; }
    @Override public Material icon() { return Material.LEATHER_BOOTS; }
    @Override public String description() { return "3 laps, checkpoints in order, shortcuts and pitfalls."; }
    @Override public boolean useCoreScoreboard() { return true; }

    @Override
    public void start(McmGameContext context) {
        if (state != State.IDLE) stop();
        if (guard == null) guard = new SpectatorGuard(plugin, this::spectatorZone);
        this.ctx = context;
        plugin.saveDefaultConfig();
        var cfg = plugin.getConfig();
        lapCoins = Math.max(0, cfg.getDouble("coins.lap", 0));
        finishFirst = Math.max(0, cfg.getDouble("coins.finish-first", 500));
        finishStep = Math.max(0, cfg.getDouble("coins.finish-step", 12.5));
        finishMin = Math.max(0, cfg.getDouble("coins.finish-minimum", 0));
        runners.clear();
        finishCount = 0;
        World w = ensureWorld();
        if (w == null) {
            McmGameContext c = ctx;
            ctx = null;
            c.broadcast("§cFoot Race could not load its world.");
            c.finish("Foot Race world failed to load");
            return;
        }
        if (clearTask != null) { clearTask.cancel(); clearTask = null; }
        clearPlacedNow();
        state = State.BUILDING;
        ctx.setSidebarLines(List.of("§7Building the track..."));

        // Sweep the arena box over a few ticks (removes any leftovers), then build and line up.
        tasks.add(new BukkitRunnable() {
            int x = Track.SWEEP_MIN_X;

            @Override
            public void run() {
                if (state != State.BUILDING || world == null) { cancel(); return; }
                int end = Math.min(Track.SWEEP_MAX_X, x + 24);
                for (; x <= end; x++)
                    for (int z = Track.SWEEP_MIN_Z; z <= Track.SWEEP_MAX_Z; z++)
                        for (int y = Track.SWEEP_MIN_Y; y <= Track.SWEEP_MAX_Y; y++) {
                            Block b = world.getBlockAt(x, y, z);
                            if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
                        }
                if (x > Track.SWEEP_MAX_X) {
                    cancel();
                    Track.Builder builder = new Track.Builder(world, placed);
                    Track.build(builder);
                    Track.buildStartBox(builder);
                    beginCountdown();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L));
    }

    @Override
    public void stop() {
        cancelTasks();
        if (clearTask != null) { clearTask.cancel(); clearTask = null; }
        McmGameContext c = ctx;
        state = State.IDLE;
        ctx = null;
        if (c != null) resetPlayersInWorld(c);
        runners.clear();
        clearPlacedNow();
    }

    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        if (world == null || state == State.IDLE) return false;
        SpectatorGuard.makeSpectator(player, Track.spectatorSpot(world));
        player.sendMessage("§6Foot Race §7is in progress — you are spectating.");
        return true;
    }

    /** Spectators (finished runners, late joiners) stay over the track: the build box plus a margin. */
    private static final org.bukkit.util.BoundingBox WATCH_BOX = new org.bukkit.util.BoundingBox(
            Track.SWEEP_MIN_X - 20, Track.SWEEP_MIN_Y - 10, Track.SWEEP_MIN_Z - 20,
            Track.SWEEP_MAX_X + 21, Track.SWEEP_MAX_Y + 30, Track.SWEEP_MAX_Z + 21);

    private SpectatorGuard.Zone spectatorZone(Player p) {
        if (state == State.IDLE || world == null || p.getWorld() != world) return null;
        return new SpectatorGuard.Zone(world, WATCH_BOX, Track.spectatorSpot(world));
    }

    // ------------------------------------------------------------------ world

    World ensureWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            try {
                w = new WorldCreator(WORLD_NAME)
                        .generator(new VoidGenerator())
                        .environment(World.Environment.NORMAL)
                        .generateStructures(false)
                        .createWorld();
            } catch (Throwable t) {
                plugin.getLogger().severe("Could not create world " + WORLD_NAME + ": " + t);
                w = null;
            }
        }
        if (w != null) {
            w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            w.setGameRule(GameRule.FALL_DAMAGE, false);
            w.setGameRule(GameRule.KEEP_INVENTORY, true);
            w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            w.setGameRule(GameRule.DO_FIRE_TICK, false);
            w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
            w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
            w.setGameRule(GameRule.DO_INSOMNIA, false);
            w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
            w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
            w.setDifficulty(Difficulty.PEACEFUL);
            w.setTime(6000);
            w.setStorm(false);
            w.setThundering(false);
            Location sp = Track.spectatorSpot(w);
            w.setSpawnLocation(sp.getBlockX(), sp.getBlockY(), sp.getBlockZ());
        }
        world = w;
        return w;
    }

    private void clearPlacedNow() {
        if (world == null) { placed.clear(); return; }
        for (int[] p : placed) {
            Block b = world.getBlockAt(p[0], p[1], p[2]);
            if (b.getType() != Material.AIR) b.setType(Material.AIR, false);
        }
        placed.clear();
    }

    private void cancelTasks() {
        for (BukkitTask t : tasks) {
            try { t.cancel(); } catch (Throwable ignored) { }
        }
        tasks.clear();
    }

    // ------------------------------------------------------------------ countdown

    private void beginCountdown() {
        if (ctx == null || world == null) return;
        state = State.COUNTDOWN;

        // Line up side by side so team-mates start next to each other.
        List<Player> order = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        Map<String, List<Player>> sides = ctx.sides();
        Map<UUID, String> sideOf = new HashMap<>();
        for (Map.Entry<String, List<Player>> e : sides.entrySet())
            for (Player p : e.getValue())
                if (p != null && p.isOnline() && seen.add(p.getUniqueId())) { order.add(p); sideOf.put(p.getUniqueId(), e.getKey()); }
        for (Player p : new ArrayList<>(ctx.participants()))
            if (seen.add(p.getUniqueId())) order.add(p);

        for (int i = 0; i < order.size(); i++) {
            Player p = order.get(i);
            Runner r = new Runner(p.getUniqueId(), p.getName(), i);
            runners.put(p.getUniqueId(), r);
            Location slot = Track.gridSlot(world, i);
            world.getBlockAt(slot.getBlockX(), Track.FY, slot.getBlockZ())
                    .setType(padMaterial(sideOf.get(p.getUniqueId())), false);
            preparePlayer(p);
            p.teleport(slot);
        }

        ctx.broadcast("§6§lFOOT RACE §7— run §f" + Track.LAPS + " laps§7. Cross every checkpoint §fin order§7; "
                + "falling or touching §cmagma§7 sends you back to your last checkpoint.");
        ctx.broadcast("§7Slime pads launch you over the gap. §eGold§7 and §bice§7 stepping stones are risky corner "
                + "shortcuts. Punch rivals to knock them around!");
        ctx.broadcast("§7Coins: §e" + McmCoins.format(finishFirst) + "§7 for the 1st finisher, §e"
                + McmCoins.format(finishStep) + "§7 less for each later finisher. No coins if you don't finish.");
        ctx.setSidebarLines(List.of("§7Laps: §f" + Track.LAPS, "§7Checkpoints: §f6 per lap",
                "§7Time limit: §f" + fmtSeconds(RACE_SECONDS), "§eGet ready..."));

        final int total = FastTest.secs(COUNTDOWN_SECONDS);
        tasks.add(new BukkitRunnable() {
            int left = total;

            @Override
            public void run() {
                if (state != State.COUNTDOWN || ctx == null) { cancel(); return; }
                ctx.setTimer(left, total);
                for (Player p : new ArrayList<>(ctx.participants())) {
                    if (left == total) {
                        p.sendTitle("§6§lFoot Race", "§7" + Track.LAPS + " laps • checkpoints in order", 5, 50, 10);
                    } else if (left <= 5 && left > 0) {
                        String col = left <= 3 ? (left == 1 ? "§a" : left == 2 ? "§e" : "§c") : "§f";
                        p.sendTitle(col + "§l" + left, "§7Get ready to run!", 0, 22, 2);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                }
                if (left <= 0) {
                    cancel();
                    startRace();
                    return;
                }
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L));
    }

    private void preparePlayer(Player p) {
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
        try { p.setHealth(Math.min(20.0, p.getMaxHealth())); } catch (Throwable ignored) { }
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setVelocity(new Vector());
        p.setAllowFlight(false);
        p.setFlying(false);
    }

    private static Material padMaterial(String sideKey) {
        if (sideKey == null || !sideKey.startsWith("TEAM:")) return Material.WHITE_CONCRETE;
        switch (sideKey.substring(5).toLowerCase(Locale.ROOT)) {
            case "red": return Material.RED_CONCRETE;
            case "orange": return Material.ORANGE_CONCRETE;
            case "yellow": return Material.YELLOW_CONCRETE;
            case "lime": return Material.LIME_CONCRETE;
            case "green": return Material.GREEN_CONCRETE;
            case "cyan": return Material.CYAN_CONCRETE;
            case "aqua": return Material.LIGHT_BLUE_CONCRETE;
            case "blue": return Material.BLUE_CONCRETE;
            case "purple": return Material.PURPLE_CONCRETE;
            case "pink": return Material.PINK_CONCRETE;
            default: return Material.WHITE_CONCRETE;
        }
    }

    // ------------------------------------------------------------------ race

    private void startRace() {
        if (ctx == null || world == null) return;
        Track.openStartBox(world);
        state = State.RACING;
        raceStartMs = System.currentTimeMillis();
        secondsLeft = RACE_SECONDS;
        for (Runner r : runners.values()) r.lapStartMs = raceStartMs;
        for (Player p : new ArrayList<>(ctx.participants())) {
            p.sendTitle("§a§lGO!", "§7Lap 1/" + Track.LAPS, 0, 25, 8);
            p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);
            p.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1f, 0.8f);
        }
        ctx.setTimer(secondsLeft, RACE_SECONDS);
        updateHud();

        tasks.add(new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (ctx == null || (state != State.RACING && state != State.ENDING)) { cancel(); return; }
                ticks++;
                if (ticks % 20 == 0 && state == State.RACING) {
                    secondsLeft--;
                    ctx.setTimer(Math.max(0, secondsLeft), RACE_SECONDS);
                    if (secondsLeft == 60 || secondsLeft == 30 || (secondsLeft <= 10 && secondsLeft > 0))
                        ctx.broadcast("§6Foot Race §7ends in §e" + secondsLeft + "s§7!");
                    if (secondsLeft <= 0) { timeUp(); return; }
                }
                if (ticks % 5 == 0) updateHud();
                if (ticks % 10 == 0 && state == State.RACING) {
                    backupVoidCheck();
                    checkAllFinished();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L));
    }

    private boolean isRacer(Player p) {
        if (world == null || ctx == null || p.getWorld() != world) return false;
        Runner r = runners.get(p.getUniqueId());
        return r != null && !r.finished;
    }

    private void backupVoidCheck() {
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (!isRacer(p)) continue;
            if (p.getLocation().getY() < Track.KILL_Y) respawn(p, runners.get(p.getUniqueId()), "§cYou fell!");
        }
    }

    private void checkAllFinished() {
        if (ctx == null || state != State.RACING) return;
        List<Player> parts = new ArrayList<>(ctx.participants());
        if (parts.isEmpty()) { end("Everyone left the race"); return; }
        boolean anyRunning = false;
        for (Player p : parts) {
            Runner r = runners.get(p.getUniqueId());
            if (r != null && !r.finished) { anyRunning = true; break; }
        }
        if (anyRunning) return;
        state = State.ENDING;
        ctx.broadcast("§6Foot Race §7— every runner has crossed the line!");
        tasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> end("All runners finished"), FastTest.ticks(60L)));
    }

    private void timeUp() {
        if (ctx == null) return;
        state = State.ENDING;
        List<String> dnf = new ArrayList<>();
        for (Player p : new ArrayList<>(ctx.participants())) {
            Runner r = runners.get(p.getUniqueId());
            if (r != null && !r.finished) {
                dnf.add(p.getName());
                p.sendTitle("§c§lTime's up!", "§7You completed §f" + r.lap + "§7/" + Track.LAPS + " laps", 0, 50, 10);
            }
        }
        ctx.broadcast("§6Foot Race §7— §cTime's up!" + (dnf.isEmpty() ? "" : " §7Did not finish: §f" + String.join("§7, §f", dnf)));
        end("Time limit reached");
    }

    // ------------------------------------------------------------------ admin testing hooks
    // (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    /** Ends the start countdown (the race starts now) or the short wait after everyone finished. */
    public boolean skipWait() {
        if (ctx == null) return false;
        if (state == State.COUNTDOWN) {
            cancelTasks();   // only the countdown runs now
            startRace();
            return true;
        }
        if (state == State.ENDING) {
            end("All runners finished");
            return true;
        }
        return false;
    }

    /** A single race: nothing to skip to. */
    public boolean skipRound() { return false; }

    private void end(String reason) {
        if (ctx == null) return;
        McmGameContext c = ctx;
        cancelTasks();
        state = State.IDLE;
        ctx = null;
        resetPlayersInWorld(c);
        runners.clear();
        try {
            c.finish(reason);
        } finally {
            // Remove the track once the core has moved everyone out.
            clearTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                clearTask = null;
                if (state == State.IDLE) clearPlacedNow();
            }, 2L);
        }
    }

    private void resetPlayersInWorld(McmGameContext c) {
        if (world == null) return;
        for (Player p : new ArrayList<>(c.participants())) {
            if (p.getWorld() != world) continue;
            p.setGameMode(GameMode.SURVIVAL);
            p.setVelocity(new Vector());
            p.setFallDistance(0);
        }
    }

    // ------------------------------------------------------------------ movement

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (state != State.RACING && state != State.COUNTDOWN) return;
        Player p = e.getPlayer();
        if (!isRacer(p)) return;
        Runner r = runners.get(p.getUniqueId());
        Location from = e.getFrom(), to = e.getTo();
        if (to == null || from.getWorld() != to.getWorld()) return;

        if (to.getY() < Track.KILL_Y) { respawn(p, r, "§cYou fell!"); return; }
        if (state != State.RACING) return;
        if (from.distanceSquared(to) > 25) return; // ignore anything that is not a real step

        int now = Bukkit.getCurrentTick();
        Block below = world.getBlockAt(to.getBlockX(), (int) Math.floor(to.getY() - 0.2), to.getBlockZ());
        Material bt = below.getType();
        if (bt == Material.MAGMA_BLOCK) { respawn(p, r, "§6Burned! §7Back to your checkpoint."); return; }
        if (bt == Material.SLIME_BLOCK && Track.isLaunchZone(below.getX(), below.getZ())
                && to.getY() - Track.FY < 1.4 && now - r.lastLaunchTick > 15) {
            r.lastLaunchTick = now;
            p.setVelocity(new Vector(0, 0.9, 1.2));
            p.playSound(p.getLocation(), Sound.ENTITY_SLIME_JUMP, 1f, 0.8f);
            world.spawnParticle(Particle.CLOUD, to, 8, 0.3, 0.1, 0.3, 0.02);
        }

        // Leaving the grid over the start line: from now on respawns go to the start line, not the grid.
        if (r.lastCp < 0 && Track.CPS[0].crossed(from, to)) r.lastCp = 0;

        int next = Track.ORDER[r.seq];
        if (Track.CPS[next].crossed(from, to)) {
            passCheckpoint(p, r, next);
            return;
        }
        // Anti-cutting feedback: crossing any other checkpoint out of order does nothing.
        for (Track.Checkpoint cp : Track.CPS) {
            if (cp.index == next || cp.index == r.lastCp) continue;
            if (cp.index == 0 && r.lastCp < 0) continue; // leaving the grid on lap 1
            if (cp.crossed(from, to) && now - r.lastWarnTick > 40) {
                r.lastWarnTick = now;
                p.sendTitle("", "§cMissed a checkpoint! §7Go back to §f" + Track.CPS[next].name, 0, 40, 10);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            }
        }
    }

    private void passCheckpoint(Player p, Runner r, int cp) {
        if (cp != 0) {
            r.seq++;
            r.lastCp = cp;
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.0f + r.seq * 0.12f);
            return;
        }
        long now = System.currentTimeMillis();
        long lapMs = now - r.lapStartMs;
        r.lapStartMs = now;
        if (lapMs < r.bestLapMs) r.bestLapMs = lapMs;
        r.lap++;
        r.seq = 0;
        r.lastCp = 0;
        if (lapCoins > 0) ctx.addCoins(r.id, lapCoins, "Foot Race lap " + r.lap);
        if (r.lap >= Track.LAPS) {
            finishRunner(p, r, now);
            return;
        }
        boolean finalLap = r.lap == Track.LAPS - 1;
        p.sendTitle(finalLap ? "§c§lFINAL LAP" : "§e§lLap " + (r.lap + 1) + "/" + Track.LAPS,
                "§7Last lap §f" + fmtMs(lapMs), 0, 35, 8);
        p.sendMessage("§6Foot Race §7» Lap " + r.lap + " in §f" + fmtMs(lapMs));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
    }

    private void finishRunner(Player p, Runner r, long now) {
        r.finished = true;
        r.place = ++finishCount;
        r.finishMs = now - raceStartMs;
        double bonus = McmCoins.linear(finishFirst, finishStep, r.place, finishMin);
        if (bonus > 0) ctx.addCoins(r.id, bonus, "Foot Race finished #" + r.place);
        ctx.broadcast("§6Foot Race §7» " + coloredName(p) + " §7finished §e" + ordinal(r.place)
                + " §7in §f" + fmtMs(r.finishMs) + " §7(best lap §f" + fmtMs(r.bestLapMs) + "§7)");
        p.sendTitle("§a§lFINISHED " + ordinal(r.place), "§f" + fmtMs(r.finishMs) + " §8| §6+" + McmCoins.format(bonus * ctx.coinMultiplier()) + " coins", 0, 60, 15);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        SpectatorGuard.makeSpectator(p, Track.spectatorSpot(world));
    }

    private void respawn(Player p, Runner r, String msg) {
        if (r == null || world == null) return;
        int now = Bukkit.getCurrentTick();
        if (now - r.lastRespawnTick < 5) return;
        r.lastRespawnTick = now;
        Location loc = (state != State.RACING || r.lastCp < 0)
                ? Track.gridSlot(world, r.slot) : Track.CPS[r.lastCp].respawn(world);
        p.teleport(loc);
        p.setVelocity(new Vector());
        p.setFallDistance(0);
        p.setFireTicks(0);
        p.sendActionBar(LEGACY.deserialize(msg));
        p.playSound(loc, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.2f);
    }

    // ------------------------------------------------------------------ rules

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (state == State.IDLE || world == null || e.getEntity().getWorld() != world) return;
        e.setCancelled(true);
        if (state != State.RACING || !(e instanceof EntityDamageByEntityEvent ed)) return;
        if (!(e.getEntity() instanceof Player victim) || !(ed.getDamager() instanceof Player attacker)) return;
        if (!isRacer(victim) || !isRacer(attacker) || victim.equals(attacker)) return;
        Runner ar = runners.get(attacker.getUniqueId());
        int now = Bukkit.getCurrentTick();
        if (now - ar.lastPunchTick < 10) return;
        ar.lastPunchTick = now;
        Vector dir = victim.getLocation().toVector().subtract(attacker.getLocation().toVector()).setY(0);
        if (dir.lengthSquared() < 1.0e-4) dir = attacker.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 1.0e-4) return;
        dir.normalize().multiply(0.55).setY(0.32);
        victim.setVelocity(victim.getVelocity().multiply(0.3).add(dir));
        world.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 1f);
    }

    private boolean activeIn(Entity en) {
        return state != State.IDLE && world != null && en.getWorld() == world;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent e) {
        if (!activeIn(e.getEntity())) return;
        e.setCancelled(true);
        e.getEntity().setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        if (activeIn(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (activeIn(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        if (activeIn(e.getPlayer())) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ HUD

    private double progress(Player p, Runner r) {
        if (r.finished) return Track.LAPS * Track.PERIMETER + 10000 - r.place;
        double lap = r.lap * Track.PERIMETER;
        if (p.getWorld() != world) return lap + Track.lapStartProgress(r.seq);
        Location l = p.getLocation();
        return lap + Track.lapProgress(l.getX(), l.getZ(), r.seq);
    }

    private void updateHud() {
        if (ctx == null) return;
        List<Player> parts = new ArrayList<>(ctx.participants());
        List<Object[]> ranked = new ArrayList<>();
        for (Player p : parts) {
            Runner r = runners.get(p.getUniqueId());
            if (r != null) ranked.add(new Object[]{p, r, progress(p, r)});
        }
        ranked.sort((a, b) -> Double.compare((double) b[2], (double) a[2]));

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < Math.min(5, ranked.size()); i++) {
            Player p = (Player) ranked.get(i)[0];
            Runner r = (Runner) ranked.get(i)[1];
            String name = p.getName().length() > 12 ? p.getName().substring(0, 12) : p.getName();
            String info;
            if (r.finished) info = "§a✔ " + fmtShort(r.finishMs);
            else {
                int pct = (int) Math.floor(100.0 * (double) ranked.get(i)[2] / (Track.LAPS * Track.PERIMETER));
                info = "§7L" + Math.min(r.lap + 1, Track.LAPS) + " §f" + pct + "%";
            }
            lines.add("§e" + (i + 1) + ". " + teamColor(p) + name + " " + info);
        }
        lines.add("§7Finished: §a" + finishCount + "§7/§f" + ranked.size());
        ctx.setSidebarLines(lines);

        long now = System.currentTimeMillis();
        for (int i = 0; i < ranked.size(); i++) {
            Player p = (Player) ranked.get(i)[0];
            Runner r = (Runner) ranked.get(i)[1];
            String bar;
            if (r.finished) {
                bar = "§a✔ Finished " + ordinal(r.place) + " §8| §f" + fmtMs(r.finishMs);
            } else {
                bar = "§6Lap §f" + Math.min(r.lap + 1, Track.LAPS) + "§7/" + Track.LAPS
                        + "  §8|  §6Position §f" + (i + 1) + "§7/" + ranked.size()
                        + "  §8|  §6CP §f" + r.seq + "§7/" + Track.CPS.length
                        + "  §8|  §f" + fmtMs(now - r.lapStartMs);
            }
            p.sendActionBar(LEGACY.deserialize(bar));
        }
    }

    private String teamColor(Player p) {
        try {
            ChatColor c = ctx.teamColor(ctx.teamOf(p.getUniqueId()));
            return c == null ? "§f" : c.toString();
        } catch (Throwable t) {
            return "§f";
        }
    }

    private String coloredName(Player p) { return teamColor(p) + p.getName(); }

    private static String fmtMs(long ms) {
        if (ms < 0 || ms == Long.MAX_VALUE) return "--:--.-";
        long m = ms / 60000, s = (ms / 1000) % 60, t = (ms / 100) % 10;
        return m + ":" + (s < 10 ? "0" : "") + s + "." + t;
    }

    private static String fmtShort(long ms) {
        long m = ms / 60000, s = (ms / 1000) % 60;
        return m + ":" + (s < 10 ? "0" : "") + s;
    }

    private static String fmtSeconds(int sec) { return fmtShort(sec * 1000L); }

    private static String ordinal(int n) {
        int m100 = n % 100, m10 = n % 10;
        String suf = (m100 >= 11 && m100 <= 13) ? "th" : m10 == 1 ? "st" : m10 == 2 ? "nd" : m10 == 3 ? "rd" : "th";
        return n + suf;
    }
}
