package com.mcm.games.acerace;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public final class AceRaceGame implements McmGame, Listener {
    static final String ID = "acerace";
    static final String WORLD_NAME = "mcc_" + ID;
    static final int TIME_LIMIT = 360;
    static final int LAPS = 2;
    static final int COUNTDOWN_TICKS = 7 * 20;
    static final int CP_POINTS = 2;
    static final int LAP_POINTS = 10;
    static final int FINISH_POINTS = 15;
    static final int[] PLACE_POINTS = {50, 40, 32, 26, 22, 18, 15, 12, 10, 8};

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private final JavaPlugin plugin;
    private McmGameContext ctx;
    private Track track;
    private World world;
    private BukkitTask task;
    private boolean running;
    private boolean ending;
    private boolean racing;
    private long tick;
    private long goTick;
    private int countdown;
    private int finishCount;
    private long endAtTick = -1;
    private long fastestLap = Long.MAX_VALUE;
    private final Map<UUID, Racer> racers = new LinkedHashMap<>();
    private final Map<String, Long> bestSplits = new HashMap<>();
    private List<Racer> ranking = new ArrayList<>();

    private static final class Racer {
        final UUID id;
        final String name;
        final int slot;
        int lap = 1;
        int nextCp = 1;
        int lastCp = 0;
        boolean finished;
        int place;
        long finishTicks;
        long lapStartTicks;
        long padCdUntil;
        long respawnCdUntil;
        long glideAt = -1;
        long itemCdUntil;
        double dist;
        Vector prev;
        final Map<Integer, Long> ringCd = new HashMap<>();

        Racer(Player p, int slot) {
            this.id = p.getUniqueId();
            this.name = p.getName();
            this.slot = slot;
        }

        int progress(int perLap) { return finished ? Integer.MAX_VALUE : (lap - 1) * perLap + nextCp; }
    }

    public AceRaceGame(JavaPlugin plugin) { this.plugin = plugin; }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Ace Race"; }
    @Override public Material icon() { return Material.ELYTRA; }
    @Override public String description() { return "Race 2 laps: jump pads, elytra, riptide, parkour. Fastest wins!"; }
    @Override public boolean useCoreScoreboard() { return true; }

    // =================================================================== lifecycle

    @Override
    public void start(McmGameContext ctx) {
        if (running) stop();
        this.ctx = ctx;
        running = true;
        ending = false;
        racing = false;
        tick = 0;
        goTick = 0;
        finishCount = 0;
        endAtTick = -1;
        fastestLap = Long.MAX_VALUE;
        racers.clear();
        bestSplits.clear();
        ranking = new ArrayList<>();

        world = ensureWorld();
        if (world == null) {
            ctx.broadcast("§cAce Race: could not create the arena world.");
            Bukkit.getScheduler().runTask(plugin, () -> endGame("Arena world unavailable"));
            return;
        }
        track = new Track(world);
        track.build();

        // Grid order: teammates next to each other.
        List<Player> order = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (List<Player> side : ctx.sides().values())
            for (Player p : side) if (p != null && p.isOnline() && seen.add(p.getUniqueId())) order.add(p);
        for (Player p : new ArrayList<>(ctx.participants())) if (seen.add(p.getUniqueId())) order.add(p);

        int i = 0;
        for (Player p : order) {
            Racer r = new Racer(p, i);
            racers.put(r.id, r);
            track.colorSlot(i, teamBlock(ctx.teamOf(r.id)));
            Location slot = track.gridSlot(i);
            preparePlayer(p);
            p.teleport(slot);
            r.prev = slot.toVector();
            i++;
        }
        countdown = COUNTDOWN_TICKS;
        ctx.setTimer(TIME_LIMIT, TIME_LIMIT);
        ctx.broadcast("§6§lAce Race §7- " + LAPS + " laps. Pads launch you, gold rings boost you, magma and the void send you back.");
        ctx.broadcast("§7Use the §cred dye §7to return to your last checkpoint. Points for checkpoints, laps and finishing position!");
        updateSidebar();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickLoop, 1L, 1L);
    }

    @Override
    public void stop() {
        boolean wasRunning = running;
        running = false;
        racing = false;
        if (task != null) {
            task.cancel();
            task = null;
        }
        if (wasRunning) cleanupPlayers();
        clearArena();
    }

    private void endGame(String reason) {
        if (ending || !running) return;
        ending = true;
        if (task != null) {
            task.cancel();
            task = null;
        }
        McmGameContext c = ctx;
        try {
            announceResults();
            cleanupPlayers();
        } catch (Throwable t) {
            plugin.getLogger().warning("Ace Race end cleanup error: " + t);
        }
        running = false;
        racing = false;
        try {
            c.finish(reason);
        } finally {
            clearArena();
        }
    }

    private World ensureWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            WorldCreator wc = new WorldCreator(WORLD_NAME)
                    .generator(new VoidGenerator())
                    .environment(World.Environment.NORMAL)
                    .generateStructures(false);
            w = wc.createWorld();
        }
        if (w == null) return null;
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setDifficulty(Difficulty.PEACEFUL);
        w.setStorm(false);
        w.setThundering(false);
        w.setTime(6000);
        w.setSpawnLocation(0, Track.BASE_Y + 1, 8);
        return w;
    }

    private void clearArena() {
        if (track != null) {
            if (Bukkit.getWorld(WORLD_NAME) != null) {
                try {
                    track.clear();
                } catch (Throwable t) {
                    plugin.getLogger().warning("Ace Race arena clear error: " + t);
                }
            }
            track = null;
        }
    }

    private void cleanupPlayers() {
        for (UUID id : new ArrayList<>(racers.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline()) continue;
            if (world != null && !p.getWorld().equals(world)) continue;
            resetPlayer(p);
            p.setGameMode(GameMode.SURVIVAL);
        }
    }

    // =================================================================== main loop

    private void tickLoop() {
        if (!running || ending || track == null) return;
        tick++;
        List<Player> players = new ArrayList<>(ctx.participants());

        if (!racing) {
            countdownTick(players);
            if (tick % 20 == 0) updateSidebar();
            if (players.isEmpty()) endGame("No racers left");
            return;
        }

        for (Player p : players) {
            Racer r = racers.get(p.getUniqueId());
            if (r == null || r.finished || !p.isOnline() || !p.getWorld().equals(world)) continue;
            try {
                handleRacer(p, r);
            } catch (Throwable t) {
                plugin.getLogger().warning("Ace Race tick error for " + p.getName() + ": " + t);
            }
            if (!running || ending) return;
        }

        if (tick % 10 == 0) computeRanking(players);
        if (tick % 4 == 0) sendActionBars(players);
        if (tick % 10 == 0) padParticles();

        long raceTicks = tick - goTick;
        if (raceTicks % 20 == 0) {
            int left = (int) Math.max(0, TIME_LIMIT - raceTicks / 20);
            ctx.setTimer(left, TIME_LIMIT);
            updateSidebar();
            if (left == 60 || left == 30) ctx.broadcast("§e" + left + " seconds left in Ace Race!");
            if (left <= 5 && left > 0)
                for (Player p : players) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
            if (left <= 0) {
                endGame("Time's up!");
                return;
            }
        }

        if (endAtTick < 0) {
            boolean anyRacing = false;
            for (Player p : players) {
                Racer r = racers.get(p.getUniqueId());
                if (r != null && !r.finished) { anyRacing = true; break; }
            }
            if (!anyRacing) endAtTick = tick + (players.isEmpty() ? 0 : 60);
        }
        if (endAtTick >= 0 && tick >= endAtTick) endGame(players.isEmpty() ? "No racers left" : "All racers finished!");
    }

    private void countdownTick(List<Player> players) {
        for (Player p : players) {
            Racer r = racers.get(p.getUniqueId());
            if (r == null || !p.getWorld().equals(world)) continue;
            if (p.getLocation().getY() < Track.BASE_Y - 8) {
                Location s = track.gridSlot(r.slot);
                p.teleport(s);
                p.setVelocity(new Vector());
                p.setFallDistance(0);
            }
        }
        if (countdown == COUNTDOWN_TICKS) {
            for (Player p : players) title(p, "§6§lACE RACE", "§e" + LAPS + " laps §7- get ready!", 5, 40, 10);
        }
        if (countdown % 20 == 0 && countdown <= 100 && countdown > 0) {
            int s = countdown / 20;
            String col = s <= 2 ? "§c" : s == 3 ? "§6" : "§e";
            for (Player p : players) {
                title(p, col + "§l" + s, "", 0, 22, 0);
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
            }
        }
        countdown--;
        if (countdown <= 0) go(players);
    }

    private void go(List<Player> players) {
        racing = true;
        goTick = tick;
        track.openGate();
        for (Player p : players) {
            title(p, "§a§lGO!", "", 0, 20, 10);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            Racer r = racers.get(p.getUniqueId());
            if (r != null) {
                r.lapStartTicks = 0;
                r.prev = p.getLocation().toVector();
            }
        }
        ctx.setTimer(TIME_LIMIT, TIME_LIMIT);
    }

    private void handleRacer(Player p, Racer r) {
        Location loc = p.getLocation();
        Vector cur = loc.toVector();
        Vector prev = r.prev == null ? cur.clone() : r.prev;
        r.prev = cur.clone();

        Track.Checkpoint last = track.checkpoints.get(r.lastCp);
        if (loc.getY() < last.voidY() || loc.getY() < 40) {
            respawn(p, r, "§cYou fell! Back to checkpoint.");
            return;
        }

        if (r.glideAt > 0 && tick >= r.glideAt) {
            r.glideAt = -1;
            ItemStack chest = p.getInventory().getChestplate();
            if (chest != null && chest.getType() == Material.ELYTRA && !p.isOnGround()) p.setGliding(true);
        }

        long below = Track.key(loc.getBlockX(), (int) Math.floor(loc.getY() - 0.2), loc.getBlockZ());
        if (track.hazards.contains(below) && p.isOnGround()) {
            p.playSound(loc, Sound.BLOCK_FIRE_EXTINGUISH, 0.8f, 1.2f);
            respawn(p, r, "§cOuch, magma! Back to checkpoint.");
            return;
        }
        Track.Pad pad = track.pads.get(below);
        if (pad != null && tick >= r.padCdUntil) applyPad(p, r, pad);

        checkRings(p, r, prev, cur);
        checkProgress(p, r, prev, cur);
    }

    // =================================================================== mechanics

    private void applyPad(Player p, Racer r, Track.Pad pad) {
        Vector fwd = new Vector(pad.dx(), 0, pad.dz());
        Location loc = p.getLocation();
        switch (pad.type()) {
            case SPEED -> {
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 2, false, false, true));
                Vector v = p.getVelocity();
                v.add(fwd.multiply(0.8));
                p.setVelocity(v);
                p.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.4f, 1.8f);
                r.padCdUntil = tick + 20;
            }
            case JUMP -> {
                p.setVelocity(fwd.multiply(0.45).setY(1.2));
                p.playSound(loc, Sound.ENTITY_SLIME_JUMP, 1f, 0.8f);
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 10, 0.4, 0.2, 0.4, 0);
                r.padCdUntil = tick + 10;
            }
            case JUMP_FAR -> {
                p.setVelocity(fwd.multiply(1.1).setY(0.85));
                p.playSound(loc, Sound.ENTITY_SLIME_JUMP, 1f, 1.2f);
                world.spawnParticle(Particle.HAPPY_VILLAGER, loc, 10, 0.4, 0.2, 0.4, 0);
                r.padCdUntil = tick + 10;
            }
            case LAUNCH -> {
                p.getInventory().setChestplate(unbreakable(Material.ELYTRA, "§bAce Wings"));
                p.setVelocity(fwd.multiply(0.7).setY(1.6));
                p.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
                world.spawnParticle(Particle.FIREWORK, loc, 20, 0.3, 0.2, 0.3, 0.05);
                r.glideAt = tick + 6;
                r.padCdUntil = tick + 20;
                actionBar(p, "§bLook where you want to fly. Gold rings boost you!");
            }
        }
    }

    private void checkRings(Player p, Racer r, Vector prev, Vector cur) {
        List<Track.Ring> rings = track.rings;
        for (int i = 0; i < rings.size(); i++) {
            Track.Ring ring = rings.get(i);
            Vector c = ring.center();
            Vector d = new Vector(ring.dx(), 0, ring.dz());
            Vector side = new Vector(-ring.dz(), 0, ring.dx());
            Vector p0 = prev.clone().add(new Vector(0, 0.9, 0));
            Vector p1 = cur.clone().add(new Vector(0, 0.9, 0));
            double a0 = p0.clone().subtract(c).dot(d);
            double a1 = p1.clone().subtract(c).dot(d);
            Vector q;
            if (a0 < 0 && a1 >= 0) {
                double t = a0 / (a0 - a1);
                q = p0.clone().add(p1.clone().subtract(p0).multiply(t));
            } else if (Math.abs(a1) < 0.6) {
                q = p1;
            } else continue;
            Vector rel = q.clone().subtract(c);
            double lat = rel.dot(side);
            double vert = rel.getY();
            if (Math.sqrt(lat * lat + vert * vert) > ring.radius()) continue;
            Long cd = r.ringCd.get(i);
            if (cd != null && tick < cd) continue;
            r.ringCd.put(i, tick + 20);
            Vector v;
            if (p.isGliding()) {
                v = p.getLocation().getDirection().multiply(1.8);
            } else {
                v = d.clone().multiply(1.25).setY(0.35);
            }
            p.setVelocity(v);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
            world.spawnParticle(Particle.END_ROD, p.getLocation(), 15, 0.5, 0.5, 0.5, 0.05);
            actionBar(p, "§6§lBOOST!");
        }
    }

    private boolean segmentHits(Track.Region reg, Vector a, Vector b) {
        if (reg == null) return false;
        for (int i = 1; i <= 4; i++) {
            Vector s = a.clone().add(b.clone().subtract(a).multiply(i / 4.0));
            if (reg.contains(s)) return true;
        }
        return false;
    }

    private void checkProgress(Player p, Racer r, Vector prev, Vector cur) {
        int cpCount = track.checkpoints.size();
        boolean isFinish = r.nextCp >= cpCount;
        Track.Region reg = isFinish ? track.finish : track.checkpoints.get(r.nextCp).region();
        if (!segmentHits(reg, prev, cur)) return;
        long t = tick - goTick;
        if (!isFinish) {
            int idx = r.nextCp;
            Track.Checkpoint cp = track.checkpoints.get(idx);
            r.lastCp = idx;
            r.nextCp++;
            ctx.addPoints(r.id, CP_POINTS, "Ace Race checkpoint");
            loadout(p, cp.trident());
            String key = r.lap + ":" + idx;
            Long best = bestSplits.putIfAbsent(key, t);
            String delta = best == null ? "§6§lLEADER" : "§c+" + fmtDelta(t - best);
            title(p, "§e" + cp.name(), "§f" + fmt(t) + "  " + delta, 0, 25, 8);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.4f);
            if (cp.trident()) actionBar(p, "§bRiptide: hold right-click in the water, then let go!");
        } else {
            lapComplete(p, r, t);
        }
    }

    private void lapComplete(Player p, Racer r, long t) {
        long lapTime = t - r.lapStartTicks;
        boolean record = lapTime < fastestLap;
        if (record) fastestLap = lapTime;
        if (r.lap < LAPS) {
            r.lap++;
            r.nextCp = 1;
            r.lastCp = 0;
            r.lapStartTicks = t;
            ctx.addPoints(r.id, LAP_POINTS, "Ace Race lap");
            title(p, "§b§lLAP " + r.lap + "/" + LAPS, "§fLap time " + fmt(lapTime) + (record ? " §6(fastest!)" : ""), 0, 35, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
            if (record) ctx.broadcast("§6" + r.name + " §eset the fastest lap: §f" + fmt(lapTime));
            Location s = track.checkpoints.get(0).respawn().clone();
            p.teleport(s);
            p.setVelocity(new Vector());
            p.setFallDistance(0);
            r.prev = s.toVector();
            loadout(p, false);
        } else {
            r.finished = true;
            finishCount++;
            r.place = finishCount;
            r.finishTicks = t;
            int placePts = placementPoints(r.place);
            ctx.addPoints(r.id, FINISH_POINTS, "Ace Race finish");
            ctx.addPoints(r.id, placePts, "Ace Race #" + r.place);
            ctx.broadcast("§a" + colored(r) + " §afinished §f#" + r.place + " §ain §f" + fmt(t)
                    + " §7(+" + (FINISH_POINTS + placePts) + ")");
            title(p, "§a§lFINISHED!", "§f#" + r.place + " §7- §f" + fmt(t), 0, 60, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            world.spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 40, 0.5, 0.8, 0.5, 0.3);
            resetPlayer(p);
            p.setGameMode(GameMode.SPECTATOR);
            p.teleport(track.spectatorSpot);
            computeRanking(new ArrayList<>(ctx.participants()));
            updateSidebar();
        }
    }

    private static int placementPoints(int place) {
        if (place <= PLACE_POINTS.length) return PLACE_POINTS[place - 1];
        if (place <= 15) return 6;
        if (place <= 25) return 4;
        return 2;
    }

    private void respawn(Player p, Racer r, String msg) {
        if (tick < r.respawnCdUntil) return;
        r.respawnCdUntil = tick + 5;
        Track.Checkpoint cp = track.checkpoints.get(r.lastCp);
        Location to = cp.respawn().clone();
        p.setGliding(false);
        p.teleport(to);
        p.setVelocity(new Vector());
        p.setFallDistance(0);
        p.setFireTicks(0);
        r.glideAt = -1;
        r.prev = to.toVector();
        loadout(p, cp.trident());
        p.playSound(to, Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 1.4f);
        if (msg != null) actionBar(p, msg);
    }

    // =================================================================== players

    private void preparePlayer(Player p) {
        p.setGameMode(GameMode.ADVENTURE);
        resetPlayer(p);
        AttributeInstance hp = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (hp != null) p.setHealth(hp.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        loadout(p, false);
    }

    private void resetPlayer(Player p) {
        p.getInventory().clear();
        p.getInventory().setArmorContents(new ItemStack[4]);
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
        p.setGliding(false);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.setFallDistance(0);
        p.setFireTicks(0);
    }

    private void loadout(Player p, boolean trident) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(new ItemStack[4]);
        p.setGliding(false);
        if (trident) {
            ItemStack t = unbreakable(Material.TRIDENT, "§bRiptide Trident");
            ItemMeta m = t.getItemMeta();
            m.addEnchant(Enchantment.RIPTIDE, 3, true);
            t.setItemMeta(m);
            inv.setItem(0, t);
            p.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20 * 120, 0, false, false, true));
        } else {
            p.removePotionEffect(PotionEffectType.DOLPHINS_GRACE);
        }
        ItemStack back = new ItemStack(Material.RED_DYE);
        ItemMeta bm = back.getItemMeta();
        bm.setDisplayName("§cBack to Checkpoint §7(right-click)");
        back.setItemMeta(bm);
        inv.setItem(8, back);
        inv.setHeldItemSlot(0);
    }

    private static ItemStack unbreakable(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setUnbreakable(true);
        meta.setDisplayName(name);
        it.setItemMeta(meta);
        return it;
    }

    private static Material teamBlock(String team) {
        if (team == null) return Material.WHITE_CONCRETE;
        return switch (team.toLowerCase(Locale.ROOT)) {
            case "red" -> Material.RED_CONCRETE;
            case "orange" -> Material.ORANGE_CONCRETE;
            case "yellow" -> Material.YELLOW_CONCRETE;
            case "lime" -> Material.LIME_CONCRETE;
            case "green" -> Material.GREEN_CONCRETE;
            case "cyan" -> Material.CYAN_CONCRETE;
            case "aqua" -> Material.LIGHT_BLUE_CONCRETE;
            case "blue" -> Material.BLUE_CONCRETE;
            case "purple" -> Material.PURPLE_CONCRETE;
            case "pink" -> Material.PINK_CONCRETE;
            default -> Material.WHITE_CONCRETE;
        };
    }

    // =================================================================== HUD

    private void computeRanking(List<Player> players) {
        int perLap = track.checkpoints.size();
        List<Racer> list = new ArrayList<>();
        for (Player p : players) {
            Racer r = racers.get(p.getUniqueId());
            if (r == null) continue;
            if (!r.finished && p.getWorld().equals(world)) {
                Track.Region target = r.nextCp >= perLap ? track.finish : track.checkpoints.get(r.nextCp).region();
                r.dist = target == null ? 0 : p.getLocation().toVector().distanceSquared(target.center());
            }
            list.add(r);
        }
        list.sort((a, b) -> {
            if (a.finished && b.finished) return Integer.compare(a.place, b.place);
            if (a.finished) return -1;
            if (b.finished) return 1;
            int c = Integer.compare(b.progress(perLap), a.progress(perLap));
            return c != 0 ? c : Double.compare(a.dist, b.dist);
        });
        ranking = list;
    }

    private void sendActionBars(List<Player> players) {
        int perLap = track.checkpoints.size();
        int total = ranking.size();
        long t = tick - goTick;
        for (Player p : players) {
            Racer r = racers.get(p.getUniqueId());
            if (r == null || r.finished) continue;
            if (tick < r.respawnCdUntil + 20) continue; // let respawn/pad messages show briefly
            int pos = ranking.indexOf(r) + 1;
            String next = r.nextCp >= perLap ? (r.lap == LAPS ? "FINISH" : "Lap line") : track.checkpoints.get(r.nextCp).name();
            actionBar(p, "§bLap " + r.lap + "/" + LAPS + " §8| §7CP §f" + (r.nextCp - 1) + "/" + (perLap - 1)
                    + " §8| §7Next: §e" + next + " §8| §fP" + (pos <= 0 ? "-" : pos) + "/" + total
                    + " §8| §a" + fmt(t - r.lapStartTicks));
        }
    }

    private void updateSidebar() {
        if (ctx == null || track == null) return;
        int perLap = track.checkpoints.size();
        List<String> lines = new ArrayList<>();
        int total = 0;
        for (Player p : ctx.participants()) if (racers.containsKey(p.getUniqueId())) total++;
        lines.add("§7Finished: §a" + finishCount + "§7/§f" + total);
        if (ranking.isEmpty()) {
            lines.add("§7" + LAPS + " laps, " + (perLap - 1) + " checkpoints");
        }
        for (int i = 0; i < Math.min(5, ranking.size()); i++) {
            Racer r = ranking.get(i);
            String nm = r.name.length() > 11 ? r.name.substring(0, 11) : r.name;
            String col = String.valueOf(ctx.teamColor(ctx.teamOf(r.id)));
            String status = r.finished ? "§a" + fmt(r.finishTicks) : "§7L" + r.lap + " " + (r.nextCp - 1) + "/" + (perLap - 1);
            lines.add("§e" + (i + 1) + ". " + col + nm + " " + status);
        }
        ctx.setSidebarLines(lines);
    }

    private void padParticles() {
        for (Location l : track.padParticles) world.spawnParticle(Particle.HAPPY_VILLAGER, l, 1, 0.2, 0.05, 0.2, 0);
    }

    private void announceResults() {
        if (ctx == null) return;
        List<Racer> done = new ArrayList<>();
        for (Racer r : racers.values()) if (r.finished) done.add(r);
        done.sort(Comparator.comparingInt(r -> r.place));
        ctx.broadcast("§6§l=== Ace Race results ===");
        if (done.isEmpty()) ctx.broadcast("§7Nobody finished.");
        for (int i = 0; i < Math.min(5, done.size()); i++) {
            Racer r = done.get(i);
            ctx.broadcast("§e#" + r.place + " " + colored(r) + " §f" + fmt(r.finishTicks));
        }
        if (fastestLap != Long.MAX_VALUE) ctx.broadcast("§7Fastest lap: §f" + fmt(fastestLap));
    }

    private String colored(Racer r) {
        return ctx.teamColor(ctx.teamOf(r.id)) + r.name;
    }

    private static String fmt(long ticks) {
        long ms = Math.max(0, ticks) * 50;
        return String.format(Locale.ROOT, "%d:%02d.%02d", ms / 60000, (ms / 1000) % 60, (ms / 10) % 100);
    }

    private static String fmtDelta(long ticks) {
        long ms = Math.max(0, ticks) * 50;
        return String.format(Locale.ROOT, "%d.%02ds", ms / 1000, (ms / 10) % 100);
    }

    private static void title(Player p, String title, String sub, int in, int stay, int out) {
        p.showTitle(Title.title(LEGACY.deserialize(title), LEGACY.deserialize(sub),
                Title.Times.times(Duration.ofMillis(in * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(out * 50L))));
    }

    private static void actionBar(Player p, String msg) {
        p.sendActionBar(LEGACY.deserialize(msg));
    }

    // =================================================================== listeners

    private boolean inGame(Entity e) {
        return running && world != null && e.getWorld().equals(world);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && inGame(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent e) {
        if (inGame(e.getEntity())) {
            e.setCancelled(true);
            e.getEntity().setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        if (inGame(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (inGame(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        if (inGame(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent e) {
        if (inGame(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (inGame(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && inGame(p) && p.getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!inGame(p)) return;
        if (e.getAction() == Action.PHYSICAL) {
            e.setCancelled(true);
            return;
        }
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (it == null || it.getType() != Material.RED_DYE) return;
        e.setCancelled(true);
        Racer r = racers.get(p.getUniqueId());
        if (r == null || r.finished || !racing || ending || track == null) return;
        if (tick < r.itemCdUntil) return;
        r.itemCdUntil = tick + 20;
        r.respawnCdUntil = 0;
        respawn(p, r, "§eBack to checkpoint.");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (!inGame(p)) return;
        resetPlayer(p);
        p.setGameMode(GameMode.SURVIVAL);
    }
}
