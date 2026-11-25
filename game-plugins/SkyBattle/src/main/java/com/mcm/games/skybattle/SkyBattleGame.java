package com.mcm.games.skybattle;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.BlockState;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class SkyBattleGame implements McmGame, Listener {
    // timings (seconds)
    private static final int COUNTDOWN = 10;
    private static final int GAME_LEN = 480;
    private static final int BORDER1_START = 45, BORDER1_END = 210;
    private static final int BORDER2_START = 240, BORDER2_END = 330;
    private static final int BORDER3_START = 390, BORDER3_END = 450;
    private static final int HEIGHT_START = 120, HEIGHT_END = 420;
    private static final int CEIL_FROM = Arena.BASE_Y + 40, CEIL_TO = Arena.BASE_Y + 11;
    private static final int FLOOR_FROM = Arena.BASE_Y - 35, FLOOR_TO = Arena.BASE_Y - 5;
    private static final int REFILL_EVERY = 20, REFILL_TARGET = 32;
    private static final long KILL_CREDIT_MS = 12_000;

    // points
    // MCC coins (base x1.0, config coins.*; the core applies the event multiplier). See game-plugins/COINS.md.
    private int KILL_POINTS = 40;                                            // MCC: personal elimination
    private int[] PLACEMENT = {65, 55, 46, 37, 29, 22, 16, 12, 8, 5};       // MCC: team pool by elimination order
    private int surviveRoundPool = 50;                                       // MCC: survive the round, split across survivors

    private static final Map<String, DyeColor> TEAM_DYES = Map.of(
            "Red", DyeColor.RED, "Orange", DyeColor.ORANGE, "Yellow", DyeColor.YELLOW,
            "Lime", DyeColor.LIME, "Green", DyeColor.GREEN, "Cyan", DyeColor.CYAN,
            "Aqua", DyeColor.LIGHT_BLUE, "Blue", DyeColor.BLUE, "Purple", DyeColor.PURPLE,
            "Pink", DyeColor.PINK);
    private static final DyeColor[] PALETTE = {
            DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN,
            DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.PINK, DyeColor.MAGENTA,
            DyeColor.WHITE, DyeColor.BROWN, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK};

    private enum State { IDLE, COUNTDOWN, LIVE, ENDED }

    private final JavaPlugin plugin;
    private final Arena arena;
    private final Random random = new Random();

    private McmGameContext ctx;
    private State state = State.IDLE;
    private BukkitTask tickTask;
    private BukkitTask finishTask;
    private int elapsed;
    private boolean finished;
    private int survivalPoints = 2;

    private final Map<UUID, String> sideOf = new HashMap<>();
    private final Map<String, DyeColor> sideDye = new LinkedHashMap<>();
    private final Map<String, List<UUID>> sideMembers = new LinkedHashMap<>();
    private final Set<UUID> alive = new LinkedHashSet<>();
    private final Set<String> placedSides = new HashSet<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, UUID> lastDamager = new HashMap<>();
    private final Map<UUID, Long> lastDamageAt = new HashMap<>();
    private int totalSides;

    public SkyBattleGame(JavaPlugin plugin) {
        this.plugin = plugin;
        this.arena = new Arena(plugin);
    }

    @Override public String id() { return "skybattle"; }
    @Override public String displayName() { return "Sky Battle"; }
    @Override public Material icon() { return Material.GRASS_BLOCK; }
    @Override public String description() { return "Bridge between sky islands, loot chests and be the last side standing."; }
    @Override public boolean useCoreScoreboard() { return true; }

    // ================================================================ lifecycle

    /** Create the void world ahead of time so the first start is quick. */
    public void prepareWorld() {
        if (state == State.IDLE) arena.ensureWorld();
    }

    @Override
    public void start(McmGameContext context) {
        if (state != State.IDLE) stop();
        this.ctx = context;
        this.finished = false;
        resetData();

        World world = arena.ensureWorld();
        if (world == null) {
            finishNow("Sky Battle world could not be created");
            return;
        }
        Map<String, List<Player>> sides = new LinkedHashMap<>();
        for (Map.Entry<String, List<Player>> e : context.sides().entrySet()) {
            List<Player> online = new ArrayList<>();
            for (Player p : e.getValue()) if (p != null && p.isOnline()) online.add(p);
            if (!online.isEmpty()) sides.put(e.getKey(), online);
        }
        if (sides.isEmpty()) {
            finishNow("No players");
            return;
        }
        totalSides = sides.size();
        int participantCount = 0;
        for (List<Player> l : sides.values()) participantCount += l.size();
        plugin.saveDefaultConfig();
        var cfg = plugin.getConfig();
        survivalPoints = Math.max(0, cfg.getInt("coins.outlive", 2));                 // MCC: another player dies, you are alive
        KILL_POINTS = Math.max(0, cfg.getInt("coins.elimination", 40));
        surviveRoundPool = Math.max(0, cfg.getInt("coins.survive-round", 50));
        PLACEMENT = com.mcm.api.McmCoins.toIntArray(cfg.getList("coins.placement"), PLACEMENT);

        // colours: teams keep their colour, solos get unused colours
        Set<DyeColor> used = new HashSet<>();
        for (String key : sides.keySet()) {
            if (key.startsWith("TEAM:")) {
                DyeColor d = TEAM_DYES.get(key.substring(5));
                if (d != null) { sideDye.put(key, d); used.add(d); }
            }
        }
        int pi = 0;
        for (String key : sides.keySet()) {
            if (sideDye.containsKey(key)) continue;
            DyeColor pick = null;
            for (int i = 0; i < PALETTE.length; i++) {
                DyeColor c = PALETTE[(pi + i) % PALETTE.length];
                if (!used.contains(c)) { pick = c; break; }
            }
            if (pick == null) pick = PALETTE[pi % PALETTE.length];
            pi++;
            used.add(pick);
            sideDye.put(key, pick);
        }

        // islands: one per side, at least 4 so the map always looks complete
        List<String> order = new ArrayList<>(sides.keySet());
        java.util.Collections.shuffle(order, random);
        List<DyeColor> islandColours = new ArrayList<>();
        for (String key : order) islandColours.add(sideDye.get(key));
        while (islandColours.size() < 4) islandColours.add(null);

        arena.clear();
        LootTable loot = new LootTable(random);
        arena.build(islandColours, loot);

        WorldBorder border = world.getWorldBorder();
        border.setCenter(0.5, 0.5);
        border.setSize(arena.initialBorder());
        border.setDamageAmount(0);
        border.setDamageBuffer(0);
        border.setWarningDistance(6);
        border.setWarningTime(10);

        for (int i = 0; i < order.size(); i++) {
            String key = order.get(i);
            List<Player> members = sides.get(key);
            List<UUID> ids = new ArrayList<>();
            Location c = arena.islandCenter(i);
            for (int j = 0; j < members.size(); j++) {
                Player p = members.get(j);
                UUID u = p.getUniqueId();
                ids.add(u);
                sideOf.put(u, key);
                names.put(u, p.getName());
                alive.add(u);
                kills.put(u, 0);
                Location spawn = c.clone();
                if (members.size() > 1) {
                    double a = 2 * Math.PI * j / members.size();
                    spawn.add(Math.cos(a) * 1.6, 0, Math.sin(a) * 1.6);
                }
                face(spawn, 0.5, 0.5);
                preparePlayer(p, sideDye.get(key));
                p.teleport(spawn);
            }
            sideMembers.put(key, ids);
        }

        state = State.COUNTDOWN;
        elapsed = 0;
        context.broadcast(ChatColor.AQUA + "" + ChatColor.BOLD + "Sky Battle! " + ChatColor.RESET + ChatColor.GRAY
                + "Loot your island, bridge to the centre and be the last side standing. The border and the sky close in!");
        context.setTimer(GAME_LEN, GAME_LEN);
        updateSidebar();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    @Override
    public void stop() {
        cancelTasks();
        if (state == State.IDLE && ctx == null) {
            arena.clear();
            return;
        }
        resetPlayersInWorld();
        arena.clear();
        resetBorder();
        state = State.IDLE;
        ctx = null;
        resetData();
    }

    private void cancelTasks() {
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        if (finishTask != null) { finishTask.cancel(); finishTask = null; }
    }

    private void resetData() {
        sideOf.clear();
        sideDye.clear();
        sideMembers.clear();
        alive.clear();
        placedSides.clear();
        kills.clear();
        names.clear();
        lastDamager.clear();
        lastDamageAt.clear();
        totalSides = 0;
        elapsed = 0;
    }

    private void resetBorder() {
        World w = arena.world();
        if (w == null || Bukkit.getWorld(Arena.WORLD_NAME) != w) return;
        WorldBorder b = w.getWorldBorder();
        b.setSize(arena.initialBorder());
    }

    /** Call ctx.finish exactly once, then clean up the arena. */
    private void finishNow(String reason) {
        if (finished) return;
        finished = true;
        cancelTasks();
        McmGameContext c = ctx;
        resetPlayersInWorld();
        state = State.IDLE;
        try {
            if (c != null) c.finish(reason);
        } finally {
            arena.clear();
            resetBorder();
            ctx = null;
            resetData();
        }
    }

    // ================================================================ game loop

    private void tick() {
        if (ctx == null || state == State.IDLE || state == State.ENDED) return;
        World world = arena.world();
        if (world == null || Bukkit.getWorld(Arena.WORLD_NAME) != world) {
            finishNow("Sky Battle world unloaded");
            return;
        }
        elapsed++;

        // drop anyone who left or was moved out of the arena
        for (UUID u : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(u);
            if (p == null || !p.isOnline()) eliminate(u, null, "disconnected", false);
            else if (p.getWorld() != world) eliminate(u, p, "left the arena", false);
        }
        if (state != State.COUNTDOWN && state != State.LIVE) return;

        if (state == State.COUNTDOWN) {
            int left = COUNTDOWN - elapsed;
            if (left > 0) {
                for (Player p : alivePlayers()) {
                    p.sendTitle(ChatColor.YELLOW + "" + left, ChatColor.GRAY + "Loot your chests!", 0, 25, 5);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                }
            } else {
                state = State.LIVE;
                elapsed = COUNTDOWN;
                for (Player p : alivePlayers()) {
                    p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "FIGHT!", ChatColor.GRAY + "Last side standing wins", 0, 30, 10);
                    p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
                }
            }
            ctx.setTimer(GAME_LEN, GAME_LEN);
            updateSidebar();
            return;
        }

        int t = elapsed - COUNTDOWN;
        WorldBorder border = world.getWorldBorder();
        double midSize = 2 * (arena.midRadius() + Arena.MID_R + 4);
        if (t == BORDER1_START) {
            border.setSize(midSize, BORDER1_END - BORDER1_START);
            ctx.broadcast(ChatColor.RED + "The border is shrinking! Head for the middle.");
        } else if (t == BORDER2_START) {
            border.setSize(2 * (Arena.CENTER_R + 3), BORDER2_END - BORDER2_START);
            ctx.broadcast(ChatColor.RED + "The border is closing in on the centre island!");
        } else if (t == BORDER3_START) {
            border.setSize(6, BORDER3_END - BORDER3_START);
            ctx.broadcast(ChatColor.DARK_RED + "Final border collapse!");
        }
        if (t == HEIGHT_START) ctx.broadcast(ChatColor.GOLD + "The sky is lowering and the void is rising!");

        int ceiling = ceiling(t), floor = floor(t);
        double dmg = t >= BORDER3_START ? 3.0 : 2.0;
        for (Player p : alivePlayers()) {
            Location l = p.getLocation();
            boolean outBorder = !border.isInside(l);
            boolean outHeight = l.getY() > ceiling || l.getY() < floor;
            if (outBorder || outHeight) {
                p.sendActionBar(ChatColor.RED + "" + ChatColor.BOLD + (outBorder ? "Outside the border!" : "Outside the safe height!"));
                p.damage(dmg, DamageSource.builder(DamageType.OUTSIDE_BORDER).build());
                if (!alive.contains(p.getUniqueId())) continue;
            } else {
                int k = kills.getOrDefault(p.getUniqueId(), 0);
                p.sendActionBar(ChatColor.GRAY + "Kills: " + ChatColor.RED + k + ChatColor.DARK_GRAY + "  |  "
                        + ChatColor.GRAY + "Safe height: " + ChatColor.YELLOW + floor + "-" + ceiling);
            }
            if (t >= HEIGHT_START && ceiling - l.getY() < 6) showCeiling(p, ceiling);
            if (t > 0 && t % REFILL_EVERY == 0) refillBlocks(p);
        }

        ctx.setTimer(Math.max(0, GAME_LEN - t), GAME_LEN);
        updateSidebar();
        if (state == State.LIVE && t >= GAME_LEN) endGame("Time's up!");
    }

    private int ceiling(int t) { return (int) Math.round(lerp(CEIL_FROM, CEIL_TO, t)); }
    private int floor(int t) { return (int) Math.round(lerp(FLOOR_FROM, FLOOR_TO, t)); }

    private static double lerp(double from, double to, int t) {
        if (t <= HEIGHT_START) return from;
        if (t >= HEIGHT_END) return to;
        return from + (to - from) * (t - HEIGHT_START) / (double) (HEIGHT_END - HEIGHT_START);
    }

    private void showCeiling(Player p, int ceiling) {
        Particle.DustOptions dust = new Particle.DustOptions(org.bukkit.Color.RED, 1.2f);
        Location l = p.getLocation();
        for (int dx = -4; dx <= 4; dx += 2) for (int dz = -4; dz <= 4; dz += 2) {
            p.spawnParticle(Particle.DUST, l.getX() + dx, ceiling + 1, l.getZ() + dz, 1, 0, 0, 0, 0, dust);
        }
    }

    private void refillBlocks(Player p) {
        String side = sideOf.get(p.getUniqueId());
        DyeColor dye = side == null ? DyeColor.WHITE : sideDye.getOrDefault(side, DyeColor.WHITE);
        Material wool = Material.valueOf(dye.name() + "_WOOL");
        int have = 0;
        for (ItemStack it : p.getInventory().getStorageContents()) if (it != null && it.getType() == wool) have += it.getAmount();
        if (have < REFILL_TARGET) p.getInventory().addItem(new ItemStack(wool, REFILL_TARGET - have));
    }

    private void updateSidebar() {
        if (ctx == null) return;
        int t = Math.max(0, elapsed - COUNTDOWN);
        World w = arena.world();
        int borderSize = w == null ? 0 : (int) Math.round(w.getWorldBorder().getSize());
        UUID top = null;
        int topKills = 0;
        for (Map.Entry<UUID, Integer> e : kills.entrySet()) {
            if (e.getValue() > topKills) { topKills = e.getValue(); top = e.getKey(); }
        }
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.WHITE + "Players alive: " + ChatColor.GREEN + alive.size());
        lines.add(ChatColor.WHITE + "Sides alive: " + ChatColor.GREEN + aliveSides().size() + ChatColor.GRAY + "/" + totalSides);
        lines.add(ChatColor.WHITE + "Border: " + ChatColor.YELLOW + borderSize + "m");
        lines.add(ChatColor.WHITE + "Safe Y: " + ChatColor.YELLOW + floor(t) + "-" + ceiling(t));
        lines.add(ChatColor.WHITE + "Top kills: " + (top == null ? ChatColor.GRAY + "none"
                : ChatColor.RED + names.getOrDefault(top, "?") + ChatColor.GRAY + " (" + topKills + ")"));
        String phase;
        if (state == State.COUNTDOWN) phase = "Starting";
        else if (t < BORDER1_START) phase = "Loot up";
        else if (t < BORDER3_START) phase = "Border shrinking";
        else phase = "Final collapse";
        lines.add(ChatColor.WHITE + "Phase: " + ChatColor.AQUA + phase);
        ctx.setSidebarLines(lines);
    }

    // ================================================================ deaths & scoring

    private List<Player> alivePlayers() {
        List<Player> list = new ArrayList<>();
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline()) list.add(p);
        }
        return list;
    }

    private Set<String> aliveSides() {
        Set<String> s = new LinkedHashSet<>();
        for (UUID u : alive) {
            String side = sideOf.get(u);
            if (side != null) s.add(side);
        }
        return s;
    }

    private String coloured(UUID u) {
        String side = sideOf.get(u);
        String team = ctx == null ? null : ctx.teamOf(u);
        ChatColor c = ctx == null ? ChatColor.WHITE : ctx.teamColor(team);
        if (team == null && side != null) c = chatFor(sideDye.get(side));
        return c + names.getOrDefault(u, "?") + ChatColor.RESET;
    }

    private static ChatColor chatFor(DyeColor d) {
        if (d == null) return ChatColor.WHITE;
        return switch (d) {
            case RED -> ChatColor.RED;
            case ORANGE -> ChatColor.GOLD;
            case YELLOW -> ChatColor.YELLOW;
            case LIME -> ChatColor.GREEN;
            case GREEN -> ChatColor.DARK_GREEN;
            case CYAN -> ChatColor.DARK_AQUA;
            case LIGHT_BLUE -> ChatColor.AQUA;
            case BLUE -> ChatColor.BLUE;
            case PURPLE -> ChatColor.DARK_PURPLE;
            case PINK, MAGENTA -> ChatColor.LIGHT_PURPLE;
            case BLACK -> ChatColor.DARK_GRAY;
            case GRAY, BROWN -> ChatColor.GRAY;
            default -> ChatColor.WHITE;
        };
    }

    /**
     * Eliminate a player. {@code p} may be null if they are offline. Safe to call for someone already out.
     */
    private void eliminate(UUID victim, Player p, String cause, boolean dropItems) {
        eliminate(victim, p, cause, dropItems, true);
    }

    private void eliminate(UUID victim, Player p, String cause, boolean dropItems, boolean spectate) {
        if (ctx == null || !alive.remove(victim)) return;
        String victimSide = sideOf.get(victim);

        UUID killer = lastDamager.get(victim);
        Long at = lastDamageAt.get(victim);
        if (killer != null && (at == null || System.currentTimeMillis() - at > KILL_CREDIT_MS)) killer = null;
        if (killer != null && (killer.equals(victim) || victimSide != null && victimSide.equals(sideOf.get(killer)))) killer = null;

        if (killer != null) {
            ctx.broadcast(ChatColor.GRAY + "☠ " + coloured(victim) + ChatColor.GRAY + " was eliminated by " + coloured(killer)
                    + ChatColor.GRAY + " (" + cause + ")");
            kills.merge(killer, 1, Integer::sum);
            if (KILL_POINTS > 0) ctx.addCoins(killer, KILL_POINTS, "Sky Battle elimination");
            Player kp = Bukkit.getPlayer(killer);
            if (kp != null) {
                kp.sendTitle("", ChatColor.RED + "⚔ Eliminated " + names.getOrDefault(victim, "?"), 0, 30, 10);
                kp.playSound(kp.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            }
        } else {
            ctx.broadcast(ChatColor.GRAY + "☠ " + coloured(victim) + ChatColor.GRAY + " " + cause);
        }

        // survival points to every player still alive
        if (state == State.LIVE) {
            for (UUID u : new ArrayList<>(alive)) {
                if (survivalPoints > 0) ctx.addCoins(u, survivalPoints, "Sky Battle survival");
            }
        }

        if (p != null && p.isOnline()) {
            World w = arena.world();
            PlayerInventory inv = p.getInventory();
            if (dropItems && w != null && p.getWorld() == w && p.getLocation().getY() > Arena.VOID_Y + 5) {
                Location loc = p.getLocation();
                for (ItemStack it : inv.getContents()) {
                    if (it != null && it.getType() != Material.AIR) w.dropItemNaturally(loc, it);
                }
            }
            inv.clear();
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            p.setFireTicks(0);
            p.setFallDistance(0);
            if (spectate && w != null && p.getWorld() == w) {
                p.setGameMode(GameMode.SPECTATOR);
                p.teleport(arena.spectatorPoint());
                p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "ELIMINATED", ChatColor.GRAY + "You are now spectating", 0, 40, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_DEATH, 1f, 1f);
            }
        }

        if (victimSide != null && !placedSides.contains(victimSide)) {
            boolean sideAlive = false;
            for (UUID u : sideMembers.getOrDefault(victimSide, List.of())) if (alive.contains(u)) { sideAlive = true; break; }
            if (!sideAlive) {
                int place = aliveSides().size() + 1;
                awardPlacement(victimSide, place);
            }
        }
        checkEnd();
    }

    private void awardPlacement(String side, int place) {
        if (!placedSides.add(side)) return;
        int pts = com.mcm.api.McmCoins.placementOrLast(PLACEMENT, place);
        ctx.broadcast(ctx.sideLabel(side) + ChatColor.GRAY + " is out! Placed " + ChatColor.YELLOW + "#" + place
                + ChatColor.GRAY + " (+" + pts + " team coins)");
        if (pts > 0) ctx.addTeamCoins(side, pts, "Sky Battle placement #" + place);
    }

    private void checkEnd() {
        if (state != State.LIVE && state != State.COUNTDOWN) return;
        int aliveSides = aliveSides().size();
        if (aliveSides == 0 || (totalSides >= 2 && aliveSides <= 1)) endGame(aliveSides == 1 ? "Last side standing!" : "Everyone was eliminated");
    }

    private void endGame(String reason) {
        if (state == State.ENDED || state == State.IDLE || ctx == null) return;
        state = State.ENDED;
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        World w = arena.world();
        if (w != null) w.getWorldBorder().setSize(w.getWorldBorder().getSize());

        // rank surviving sides: more players alive, then more total health
        List<String> remaining = new ArrayList<>(aliveSides());
        Map<String, double[]> score = new HashMap<>();
        for (String s : remaining) {
            double cnt = 0, hp = 0;
            for (UUID u : sideMembers.getOrDefault(s, List.of())) {
                if (!alive.contains(u)) continue;
                cnt++;
                Player p = Bukkit.getPlayer(u);
                if (p != null) hp += p.getHealth();
            }
            score.put(s, new double[]{cnt, hp});
        }
        remaining.sort(Comparator.<String>comparingDouble(s -> -score.get(s)[0]).thenComparingDouble(s -> -score.get(s)[1]));

        if (!remaining.isEmpty()) {
            String winner = remaining.get(0);
            ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Sky Battle over! " + ChatColor.RESET + ctx.sideLabel(winner)
                    + ChatColor.GOLD + " wins (" + reason + ")");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (w != null && p.getWorld() == w) {
                    p.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "GAME OVER", ctx.sideLabel(winner) + ChatColor.GOLD + " wins!", 5, 60, 10);
                }
            }
        } else {
            ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Sky Battle over! " + ChatColor.RESET + ChatColor.GRAY + reason);
        }
        // MCC: 50 coins for surviving the entire round, split across the surviving winners
        List<UUID> survivors = new ArrayList<>();
        for (UUID u : alive) if (Bukkit.getPlayer(u) != null) survivors.add(u);
        if (surviveRoundPool > 0 && !survivors.isEmpty()) {
            double each = surviveRoundPool / (double) survivors.size();
            for (UUID u : survivors) ctx.addCoins(u, each, "Survived the round");
        }
        for (int i = 0; i < remaining.size(); i++) {
            String s = remaining.get(i);
            if (placedSides.contains(s)) continue;
            int place = i + 1;
            int pts = com.mcm.api.McmCoins.placementOrLast(PLACEMENT, place);
            placedSides.add(s);
            if (pts > 0) ctx.addTeamCoins(s, pts, "Sky Battle placement #" + place);
            ctx.broadcast(ctx.sideLabel(s) + ChatColor.GRAY + " survived - placed " + ChatColor.YELLOW + "#" + place
                    + ChatColor.GRAY + " (+" + pts + " team coins)");
        }
        // keep the HUD frozen during the victory pause, make survivors invulnerable
        for (Player p : alivePlayers()) p.setInvulnerable(true);
        finishTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            finishTask = null;
            finishNow(reason);
        }, 80L);
    }

    // ================================================================ player setup

    private void face(Location from, double tx, double tz) {
        double dx = tx - from.getX(), dz = tz - from.getZ();
        from.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        from.setPitch(0);
    }

    private void preparePlayer(Player p, DyeColor dye) {
        p.setGameMode(GameMode.SURVIVAL);
        p.setInvulnerable(false);
        p.getInventory().clear();
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        AttributeInstance max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(max == null ? 20 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(10);
        p.setExhaustion(0);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setLevel(0);
        p.setExp(0);
        p.setAllowFlight(false);
        p.setFlying(false);

        PlayerInventory inv = p.getInventory();
        inv.setItem(0, new ItemStack(Material.WOODEN_SWORD));
        inv.setItem(1, new ItemStack(Material.STONE_PICKAXE));
        inv.setItem(2, new ItemStack(Material.valueOf(dye.name() + "_WOOL"), 48));
        inv.setItem(8, new ItemStack(Material.COOKED_BEEF, 3));
        inv.setChestplate(dyed(Material.LEATHER_CHESTPLATE, dye));
        inv.setBoots(dyed(Material.LEATHER_BOOTS, dye));
    }

    private static ItemStack dyed(Material m, DyeColor dye) {
        ItemStack it = new ItemStack(m);
        if (it.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(dye.getColor());
            it.setItemMeta(meta);
        }
        return it;
    }

    /** Reset everyone still in the Sky Battle world so nothing leaks into the lobby. */
    private void resetPlayersInWorld() {
        World w = arena.world();
        if (w == null || Bukkit.getWorld(Arena.WORLD_NAME) != w) return;
        for (Player p : new ArrayList<>(w.getPlayers())) {
            p.getInventory().clear();
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            p.setInvulnerable(false);
            p.setFireTicks(0);
            p.setFallDistance(0);
            AttributeInstance max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
            p.setHealth(max == null ? 20 : max.getValue());
            p.setFoodLevel(20);
            p.setGameMode(GameMode.SURVIVAL);
        }
    }

    // ================================================================ listeners

    private boolean inGameWorld(Entity e) {
        World w = arena.world();
        return state != State.IDLE && ctx != null && w != null && e.getWorld() == w;
    }

    private boolean inGameWorld(org.bukkit.block.Block b) {
        World w = arena.world();
        return state != State.IDLE && ctx != null && w != null && b.getWorld() == w;
    }

    private static Player attackerOf(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !inGameWorld(victim)) return;
        if (state != State.LIVE || !alive.contains(victim.getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        if (e instanceof EntityDamageByEntityEvent ed) {
            Player attacker = attackerOf(ed.getDamager());
            if (attacker != null && !attacker.equals(victim)) {
                UUID a = attacker.getUniqueId();
                String sa = sideOf.get(a), sv = sideOf.get(victim.getUniqueId());
                if (!alive.contains(a) || (sa != null && sa.equals(sv))) {
                    e.setCancelled(true);
                    return;
                }
                lastDamager.put(victim.getUniqueId(), a);
                lastDamageAt.put(victim.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethal(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !inGameWorld(victim)) return;
        if (state != State.LIVE || !alive.contains(victim.getUniqueId())) return;
        if (e.getFinalDamage() < victim.getHealth()) return;
        if (victim.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || victim.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return;
        e.setCancelled(true);
        eliminate(victim.getUniqueId(), victim, causeText(e.getCause()), true);
    }

    private static String causeText(EntityDamageEvent.DamageCause c) {
        return switch (c) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> "slain";
            case PROJECTILE -> "shot";
            case FALL -> "fell to their death";
            case WORLD_BORDER, CUSTOM -> "was caught by the closing sky";
            case FALLING_BLOCK, SUFFOCATION -> "was crushed";
            default -> "died";
        };
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!inGameWorld(p) || !alive.contains(p.getUniqueId())) return;
        Location to = e.getTo();
        if (state == State.COUNTDOWN) {
            Location from = e.getFrom();
            if (from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY()) {
                Location back = from.clone();
                back.setYaw(to.getYaw());
                back.setPitch(to.getPitch());
                e.setTo(back);
            }
            return;
        }
        if (state == State.LIVE && to.getY() < Arena.VOID_Y) {
            eliminate(p.getUniqueId(), p, "fell into the void", false);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!inGameWorld(e.getBlock())) return;
        if (state != State.LIVE || !alive.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaced(BlockPlaceEvent e) {
        if (!inGameWorld(e.getBlock())) return;
        arena.track(e.getBlock());
        if (e instanceof BlockMultiPlaceEvent multi) {
            for (BlockState s : multi.getReplacedBlockStates()) arena.track(s.getBlock());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!inGameWorld(e.getBlock())) return;
        if (state != State.LIVE || !alive.contains(e.getPlayer().getUniqueId())) e.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (ctx == null || !alive.contains(p.getUniqueId())) return;
        if (state != State.LIVE && state != State.COUNTDOWN) return;
        eliminate(p.getUniqueId(), p, "disconnected", inGameWorld(p), false);
    }
}
