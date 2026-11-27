package com.mcm.games.acerace;

import com.mcm.api.McmApi;
import com.mcm.api.McmFx;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.api.McmSpectator;
import com.mcm.api.Waits;
import com.mcm.games.acerace.map.RaceGeo.BoostPad;
import com.mcm.games.acerace.map.RaceGeo.BoostRing;
import com.mcm.games.acerace.map.RaceGeo.EffectSpec;
import com.mcm.games.acerace.map.RaceGeo.Hazard;
import com.mcm.games.acerace.map.RaceGeo.ItemSpec;
import com.mcm.games.acerace.map.RaceGeo.Loadout;
import com.mcm.games.acerace.map.RaceGeo.PadKind;
import com.mcm.games.acerace.map.RaceGeo.Pos;
import com.mcm.games.acerace.map.RaceGeo.Region;
import com.mcm.games.acerace.map.RaceGeo.Section;
import com.mcm.games.acerace.map.RaceGeo.Spawn;
import com.mcm.games.acerace.map.RaceMapData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Phase-1 Ace Race runtime: map loading, start pen, countdown, checkpoints, pads, rings, falls,
 * finish, HUD and the admin fast-test/skip hooks. Map geometry is pure data; this class is Bukkit.
 */
public final class RaceGame implements McmGame {
    public static final String ID = "acerace";

    private enum State { IDLE, LOADING, PRE, RACING, ENDING, FINISHED }

    private final AceRacePlugin plugin;
    private final RaceRegistry registry;
    private final RaceWorld world;
    private final RaceScoring scoring = new RaceScoring();
    private final RaceHud hud = new RaceHud();
    private final Map<UUID, Racer> racers = new LinkedHashMap<>();

    private McmGameContext ctx;
    private RaceMapData data;
    private String mapId;
    private int slot;
    private State state = State.IDLE;
    private int raceTick, timeLeft, finishCount;
    private long raceStartMillis;
    private BukkitTask tickTask, countdownTask, endTask;
    private boolean finishCalled;
    private String forcedMap;
    private String lastMap;

    public RaceGame(AceRacePlugin plugin, RaceRegistry registry, RaceWorld world) {
        this.plugin = plugin;
        this.registry = registry;
        this.world = world;
    }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Ace Race"; }
    @Override public Material icon() { return Material.FEATHER; }
    @Override public String description() { return "A movement race: checkpoints, pads, wing rings and a clean line."; }
    @Override public boolean useCoreScoreboard() { return true; }

    @Override
    public List<String> maps() {
        return registry.ids();
    }

    @Override
    public boolean forceNextMap(String id) {
        if (registry.map(id) == null) return false;
        forcedMap = id.toLowerCase(Locale.ROOT);
        return true;
    }

    @Override
    public String currentMap() {
        return mapId;
    }

    @Override
    public void start(McmGameContext context) {
        stop();
        this.ctx = context;
        this.finishCalled = false;
        this.finishCount = 0;
        this.racers.clear();

        String chosen = forcedMap != null ? forcedMap : registry.next(lastMap);
        forcedMap = null;
        if (chosen == null || registry.map(chosen) == null) {
            context.finish("No Ace Race maps registered");
            return;
        }
        this.mapId = chosen.toLowerCase(Locale.ROOT);
        this.data = registry.data(mapId);
        if (data == null) {
            context.finish("Ace Race map data missing: " + mapId);
            return;
        }
        scoring.load(plugin.getConfig());
        if (world.world() == null) {
            world.ensureWorld();
        }
        if (world.world() == null) {
            context.finish("Ace Race world unavailable");
            return;
        }
        List<String> errors = data.check();
        if (!errors.isEmpty()) {
            plugin.getLogger().warning("Ace Race map '" + mapId + "' failed validation: " + errors);
            context.finish("Ace Race map invalid: " + mapId);
            return;
        }
        this.slot = registry.slot(mapId);
        this.lastMap = mapId;
        this.state = State.LOADING;
        this.world.paste(slot, data, this::beginPre);
    }

    private void beginPre() {
        if (ctx == null || state != State.LOADING) return;
        state = State.PRE;
        raceTick = 0;
        timeLeft = data.timeLimitSeconds();
        finishCount = 0;
        racers.clear();
        restoreBarrier();

        List<Player> participants = new ArrayList<>(ctx.participants());
        int index = 0;
        for (Player p : participants) {
            if (index >= data.startGrid.size()) {
                McmSpectator.makeSpectator(p, overview());
                continue;
            }
            Racer r = new Racer(p.getUniqueId(), index);
            racers.put(p.getUniqueId(), r);
            teleport(p, data.startGrid.get(index));
            r.prev = local(p.getLocation());
            prepare(p);
            index++;
        }
        if (racers.isEmpty()) {
            ctx.finish("No Ace Race participants");
            return;
        }
        hud.board(ctx, data, 1, data.laps(), 0, checkpoints(), racers.size(), racers.size(),
                timeLeft, data.timeLimitSeconds());
        startCountdown();
    }

    private void startCountdown() {
        int seconds = McmApi.isFastTest() ? 1 : data.countdownSeconds();
        countdownTask = new BukkitRunnable() {
            int left = seconds;

            @Override
            public void run() {
                if (ctx == null || state != State.PRE) {
                    cancel();
                    return;
                }
                if (left <= 0) {
                    cancel();
                    beginRace();
                    return;
                }
                for (Racer r : racers.values()) {
                    Player p = Bukkit.getPlayer(r.id);
                    if (p == null) continue;
                    McmFx.title(p, ChatColor.YELLOW + "" + ChatColor.BOLD + left,
                            ChatColor.GRAY + "Get ready", 0, 20, 0);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                }
                left--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void beginRace() {
        if (ctx == null || state != State.PRE) return;
        state = State.RACING;
        raceTick = 0;
        timeLeft = data.timeLimitSeconds();
        raceStartMillis = System.currentTimeMillis();
        clearBarrier();
        for (Racer r : racers.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p == null) continue;
            r.prev = local(p.getLocation());
            p.setWalkSpeed(0.2f);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
        }
        tickTask = new BukkitRunnable() {
            @Override public void run() { tick(); }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void tick() {
        if (ctx == null || state != State.RACING) return;
        raceTick++;
        if (raceTick % 20 == 0) {
            timeLeft--;
            if (timeLeft <= 0) {
                endRace("Time");
                return;
            }
            int maxCp = 0;
            int maxLap = 1;
            for (Racer r : racers.values()) {
                maxCp = Math.max(maxCp, Math.min(r.nextCp, checkpoints()));
                maxLap = Math.max(maxLap, r.lap);
            }
            hud.board(ctx, data, maxLap, data.laps(), maxCp, checkpoints(), alive(), racers.size(),
                    timeLeft, data.timeLimitSeconds());
        }

        for (Racer r : new ArrayList<>(racers.values())) {
            if (r.finished || r.dnf) continue;
            Player p = Bukkit.getPlayer(r.id);
            if (p == null || !p.isOnline()) {
                r.online = false;
                r.dnf = true;
                continue;
            }
            if (p.getGameMode() == GameMode.SPECTATOR) {
                r.online = false;
                r.dnf = true;
                continue;
            }
            Location loc = p.getLocation();
            Vector cur = local(loc);
            if (raceTick < r.respawnGrace) {
                r.prev = cur;
                continue;
            }
            if (cur.getY() < data.voidY || !data.playBounds.contains(cur)) {
                respawn(p, r, "void");
                continue;
            }
            if (checkHazard(p, r, cur)) continue;

            Section next = r.nextCp < data.sections.size() ? data.sections.get(r.nextCp) : null;
            if (next != null && cur.getY() < next.killY()) {
                respawn(p, r, "fall");
                continue;
            }
            if (next != null) {
                for (int id : next.padIds()) {
                    if (id < 0 || id >= data.pads.size()) continue;
                    BoostPad pad = data.pads.get(id);
                    if (pad.trigger().contains(cur)) applyPad(p, r, pad);
                }
                for (int id : next.ringIds()) {
                    if (id < 0 || id >= data.rings.size()) continue;
                    checkRing(p, r, data.rings.get(id), cur);
                }
                if (next.gate().crossed(r.prev, cur)) {
                    advance(p, r, next);
                }
            } else if (data.finishGate.crossed(r.prev, cur)) {
                crossFinish(p, r);
            }
            r.prev = cur;
        }

        if (raceTick % 4 == 0) {
            for (Racer r : racers.values()) {
                Player p = Bukkit.getPlayer(r.id);
                if (p == null || r.finished || r.dnf) continue;
                hud.action(p, r.lap, data.laps(), Math.min(r.nextCp, checkpoints()), checkpoints(), position(r));
            }
        }

        if (allFinished()) endRace("All finished");
    }

    private void advance(Player p, Racer r, Section section) {
        if (section.index() != r.nextCp) return;
        r.lastCp = section.index();
        r.nextCp++;
        if (section.index() > 0) {
            scoring.checkpoint(ctx, r.id, section.index(), checkpoints());
            long splitTicks = Math.max(1, raceTick - r.lastSplitTick);
            boolean showSplit = data.laps() <= 1 || r.lap >= 2;
            hud.checkpoint(p, section.index(), checkpoints(), splitTicks / 20.0, showSplit);
            if (!showSplit) McmFx.actionBar(p, ChatColor.GREEN + "Checkpoint " + section.index() + "/" + checkpoints());
        }
        r.lastSplitTick = raceTick;
        applyLoadout(p, section.onEnter());
    }

    private void crossFinish(Player p, Racer r) {
        if (r.lap < data.laps()) {
            scoring.lap(ctx, r.id, r.lap, data.laps());
            r.lap++;
            r.nextCp = 0;
            r.lastCp = -1;
            r.lastSplitTick = raceTick;
            if (!data.loop) {
                Location to = world.toWorld(slot, data.lapSpawn.x(), data.lapSpawn.y(), data.lapSpawn.z(), data.lapSpawn.yaw());
                p.teleport(to);
                p.setVelocity(new Vector());
                p.setFallDistance(0f);
                r.prev = local(to);
            }
            McmFx.title(p, ChatColor.GOLD + "" + ChatColor.BOLD + "LAP " + r.lap + "/" + data.laps(),
                    ChatColor.GRAY + "Keep going", 3, 30, 5);
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
        } else {
            finishRacer(p, r);
        }
    }

    private void finishRacer(Player p, Racer r) {
        if (r.finished) return;
        r.finished = true;
        r.dnf = false;
        finishCount++;
        r.place = finishCount;
        long millis = Math.max(0L, System.currentTimeMillis() - raceStartMillis);
        scoring.finish(ctx, r.id);
        scoring.place(ctx, r.id, r.place);
        hud.finish(p, r.place, millis);
        McmSpectator.makeSpectator(p, overview());
        if (allFinished()) endRace("All finished");
    }

    private boolean checkHazard(Player p, Racer r, Vector cur) {
        for (Hazard h : data.hazards) {
            if (h.killY() != 0 && cur.getY() < h.killY()) {
                respawn(p, r, "hazard");
                return true;
            }
            for (Region region : h.regions()) {
                if (region.contains(cur)) {
                    respawn(p, r, (h.message() == null || h.message().isBlank()) ? "hazard" : h.message());
                    return true;
                }
            }
            Block feet = p.getLocation().getBlock();
            Block below = feet.getRelative(BlockFace.DOWN);
            String feetKey = feet.getType().getKey().toString();
            String belowKey = below.getType().getKey().toString();
            if (h.blockTypes().contains(feetKey) || h.blockTypes().contains(belowKey)) {
                respawn(p, r, (h.message() == null || h.message().isBlank()) ? "hazard" : h.message());
                return true;
            }
        }
        return false;
    }

    private void respawn(Player p, Racer r, String reason) {
        if (r.finished || r.dnf) return;
        Section s = data.sections.get(Math.max(0, Math.min(r.lastCp, data.sections.size() - 1)));
        Location to = world.toWorld(slot, s.respawn().x(), s.respawn().y(), s.respawn().z(), s.respawn().yaw());
        p.teleport(to);
        p.setVelocity(new Vector());
        p.setFallDistance(0f);
        p.setFireTicks(0);
        p.setWalkSpeed(0.2f);
        applyLoadout(p, s.onRespawn());
        r.prev = local(to);
        r.respawnGrace = raceTick + 10;
        McmFx.actionBar(p, ChatColor.RED + "Recovered " + ChatColor.GRAY + "(" + reason + ")");
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
    }

    private void applyPad(Player p, Racer r, BoostPad pad) {
        int ready = r.padCooldown.getOrDefault(pad.id(), -100);
        if (ready > raceTick) return;
        r.padCooldown.put(pad.id(), raceTick + Math.max(1, pad.cooldownTicks()));
        Vector dir = new Vector(pad.dx(), pad.dy(), pad.dz());
        if (dir.lengthSquared() < 0.0001) return;
        dir.normalize();
        Vector v = p.getVelocity();
        if (pad.kind() == PadKind.SPEED) {
            v.add(dir.multiply(pad.speed()));
            double horizontal = Math.min(0.9, Math.hypot(v.getX(), v.getZ()));
            double angle = Math.atan2(v.getZ(), v.getX());
            v.setX(Math.cos(angle) * horizontal);
            v.setZ(Math.sin(angle) * horizontal);
        } else {
            v = dir.multiply(pad.speed());
        }
        p.setVelocity(v);
        if (pad.grant() != null) applyLoadout(p, pad.grant());
        scheduleGlide(p);
        p.playSound(p.getLocation(), pad.kind() == PadKind.SPEED ? Sound.ENTITY_FIREWORK_ROCKET_LAUNCH : Sound.ENTITY_SLIME_JUMP, 0.8f, 1.2f);
    }

    private void checkRing(Player p, Racer r, BoostRing ring, Vector cur) {
        int ready = r.ringCooldown.getOrDefault(ring.id(), -100);
        if (ready > raceTick) return;
        Vector center = new Vector(ring.x(), ring.y(), ring.z());
        Vector normal = new Vector(ring.nx(), ring.ny(), ring.nz());
        if (normal.lengthSquared() < 0.0001) return;
        normal.normalize();
        double d0 = r.prev.clone().subtract(center).dot(normal);
        double d1 = cur.clone().subtract(center).dot(normal);
        if (d0 > 0 || d1 < 0 || d0 == d1) return;
        double t = -d0 / (d1 - d0);
        Vector hit = r.prev.clone().add(cur.clone().subtract(r.prev).multiply(t));
        if (hit.distance(center) > ring.radius()) return;
        if (ring.requireGlide() && !p.isGliding()) return;
        r.ringCooldown.put(ring.id(), raceTick + Math.max(1, ring.cooldownTicks()));
        Vector v = p.getVelocity().add(normal.multiply(ring.strength()));
        p.setVelocity(v);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.9f, 1.4f);
    }

    private void endRace(String reason) {
        if (state != State.RACING) return;
        state = State.ENDING;
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        timeLeft = 0;
        for (Racer r : racers.values()) {
            if (!r.finished) r.dnf = true;
        }
        List<Racer> top = racers.values().stream()
                .filter(r -> r.finished)
                .sorted(Comparator.comparingInt(r -> r.place))
                .limit(5).toList();
        if (ctx != null && !top.isEmpty()) {
            ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "ACE RACE - " + reason);
            for (Racer r : top) {
                Player p = Bukkit.getPlayer(r.id);
                String name = p == null ? "?" : p.getName();
                ctx.broadcast(ChatColor.GRAY + "" + r.place + ". " + ChatColor.WHITE + name);
            }
        }
        if (ctx != null) {
            ctx.setSidebarLines(List.of(
                    ChatColor.GOLD + "" + ChatColor.BOLD + "ACE RACE",
                    ChatColor.GRAY + "Map " + ChatColor.WHITE + data.mapId,
                    ChatColor.YELLOW + "Finished",
                    ChatColor.GRAY + "Players " + ChatColor.WHITE + finishCount + "/" + racers.size()
            ));
            ctx.setTimer(0, data.timeLimitSeconds());
        }
        long delay = Waits.ticks(60L);
        endTask = new BukkitRunnable() {
            @Override public void run() { finishGame(); }
        }.runTaskLater(plugin, delay);
    }

    private void finishGame() {
        if (finishCalled) return;
        finishCalled = true;
        state = State.FINISHED;
        if (ctx != null) ctx.finish("Finished");
    }

    @Override
    public void stop() {
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        if (endTask != null) { endTask.cancel(); endTask = null; }
        for (Racer r : racers.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p == null) continue;
            p.setWalkSpeed(0.2f);
            if (p.getGameMode() == GameMode.SPECTATOR) p.setGameMode(GameMode.ADVENTURE);
        }
        racers.clear();
        ctx = null;
        data = null;
        mapId = null;
        state = State.IDLE;
        finishCalled = false;
        finishCount = 0;
    }

    @Override
    public boolean skipWait() {
        if (state == State.PRE) {
            if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
            beginRace();
            return true;
        }
        if (state == State.ENDING) {
            if (endTask != null) { endTask.cancel(); endTask = null; }
            finishGame();
            return true;
        }
        return false;
    }

    @Override
    public boolean skipRound() {
        return false;
    }

    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        McmSpectator.makeSpectator(player, overview());
        return true;
    }

    public void onQuit(Player player) {
        Racer r = racers.get(player.getUniqueId());
        if (r == null || r.finished) return;
        r.online = false;
        r.dnf = true;
        if (state == State.RACING && allFinished()) endRace("All finished");
    }

    public boolean isRunning() {
        return state != State.IDLE && state != State.FINISHED;
    }

    public boolean isPre() {
        return state == State.PRE;
    }

    public boolean isRaceWorld(World w) {
        return world.world() != null && world.world().equals(w);
    }

    public boolean isParticipant(Player p) {
        return p != null && racers.containsKey(p.getUniqueId());
    }

    public void confineSpectator(Player p) {
        if (data == null || p.getGameMode() != GameMode.SPECTATOR) return;
        Vector local = local(p.getLocation());
        if (!data.spectatorBox.contains(local)) p.teleport(overview());
    }

    public boolean isInsideSpectator(Location loc) {
        return data == null || (loc != null && data.spectatorBox.contains(local(loc)));
    }

    public int checkpoints() {
        return Math.max(0, data == null ? 0 : data.sections.size() - 1);
    }

    public RaceMapData data() {
        return data;
    }

    public int slot() {
        return slot;
    }

    private void prepare(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        p.setItemOnCursor(null);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.setVelocity(new Vector());
        p.setWalkSpeed(0f);
        p.setFoodLevel(20);
        p.setHealth(Math.min(20.0, p.getMaxHealth()));
        for (var effect : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(effect.getType());
    }

    private void teleport(Player p, Spawn spawn) {
        p.teleport(world.toWorld(slot, spawn.x(), spawn.y(), spawn.z(), spawn.yaw()));
        p.setVelocity(new Vector());
        p.setFallDistance(0f);
    }

    private Location overview() {
        if (data != null && !data.overviews.isEmpty()) {
            Spawn s = data.overviews.get(0);
            return world.toWorld(slot, s.x(), s.y(), s.z(), s.yaw());
        }
        return world.world() == null ? new Location(Bukkit.getWorlds().get(0), 0, 100, 0)
                : world.world().getSpawnLocation();
    }

    private Vector local(Location loc) {
        return new Vector(loc.getX() - world.slotX(slot),
                loc.getY() - world.baseY(),
                loc.getZ() - world.slotZ(slot));
    }

    private void restoreBarrier() {
        if (world.world() == null || data == null) return;
        for (Pos p : data.startBarrier) {
            Location l = world.toWorld(slot, p);
            String state = data.blocks.get(p.x(), p.y(), p.z());
            if (state != null) {
                try {
                    world.world().getBlockAt(l).setBlockData(Bukkit.createBlockData(state), false);
                    continue;
                } catch (Throwable ignored) {}
            }
            world.world().getBlockAt(l).setType(Material.CYAN_STAINED_GLASS, false);
        }
    }

    private void clearBarrier() {
        if (world.world() == null) return;
        for (Pos p : data.startBarrier) {
            Location l = world.toWorld(slot, p);
            world.world().getBlockAt(l).setType(Material.AIR, false);
        }
    }

    private boolean allFinished() {
        for (Racer r : racers.values()) {
            if (!r.finished && !r.dnf) return false;
            if (r.online && !r.finished && !r.dnf) return false;
        }
        return true;
    }

    private int alive() {
        int n = 0;
        for (Racer r : racers.values()) if (!r.dnf && !r.finished) n++;
        return Math.max(n, 1);
    }

    private int position(Racer r) {
        int ahead = 0;
        for (Racer other : racers.values()) {
            if (other == r) continue;
            if (other.finished && !r.finished) ahead++;
            else if (!other.finished && !r.finished && other.nextCp > r.nextCp) ahead++;
        }
        return ahead + 1;
    }

    private void applyLoadout(Player p, Loadout loadout) {
        if (loadout == null) return;
        if (loadout.clearInventory()) {
            p.getInventory().clear();
            for (var effect : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(effect.getType());
        }
        for (ItemSpec spec : loadout.items()) {
            Material m = Material.matchMaterial(spec.material());
            if (m == null) continue;
            ItemStack item = new ItemStack(m, Math.max(1, spec.amount()));
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                if (spec.name() != null) meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', spec.name()));
                meta.setUnbreakable(spec.unbreakable());
                for (String e : spec.enchants()) {
                    String[] parts = e.split(":", 2);
                    if (parts.length != 2) continue;
                    Enchantment ench = Enchantment.getByKey(NamespacedKey.minecraft(parts[0].toLowerCase(Locale.ROOT)));
                    if (ench == null) continue;
                    try { meta.addEnchant(ench, Integer.parseInt(parts[1]), true); } catch (NumberFormatException ignored) {}
                }
                item.setItemMeta(meta);
            }
            if (spec.slot() >= 0 && spec.slot() <= 40) p.getInventory().setItem(spec.slot(), item);
            else p.getInventory().addItem(item);
        }
        for (EffectSpec e : loadout.effects()) {
            PotionEffectType type = PotionEffectType.getByKey(NamespacedKey.minecraft(e.type().toLowerCase(Locale.ROOT)));
            if (type != null) p.addPotionEffect(new PotionEffect(type, e.durationTicks(), e.amplifier()));
        }
        scheduleGlide(p);
    }

    private boolean hasElytra(Player p) {
        ItemStack chest = p.getInventory().getChestplate();
        return chest != null && chest.getType() == Material.ELYTRA;
    }

    /** One-tick delay so a launch pad's upward velocity is visible before setGliding. */
    private void scheduleGlide(Player p) {
        if (!hasElytra(p)) return;
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isOnline() && !p.isOnGround() && hasElytra(p) && !p.isGliding()) {
                p.setGliding(true);
            }
        });
    }

    private static final class Racer {
        final UUID id;
        final int grid;
        int nextCp = 0;
        int lastCp = -1;
        int lap = 1;
        int place;
        boolean finished, dnf, online = true;
        int respawnGrace;
        int lastSplitTick;
        Vector prev;
        final Map<Integer, Integer> padCooldown = new HashMap<>();
        final Map<Integer, Integer> ringCooldown = new HashMap<>();

        Racer(UUID id, int grid) {
            this.id = id;
            this.grid = grid;
        }
    }
}
