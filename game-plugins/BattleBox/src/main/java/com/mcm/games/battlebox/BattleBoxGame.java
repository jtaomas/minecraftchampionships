package com.mcm.games.battlebox;

import com.mcm.api.McmBuild;
import com.mcm.api.McmFx;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.battlebox.arena.ArenaGeo;
import com.mcm.games.battlebox.arena.ArenaTheme;
import com.mcm.games.battlebox.arena.Grid;
import com.mcm.games.battlebox.arena.HoldingArea;
import com.mcm.games.battlebox.arena.Themes;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Difficulty;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Battle Box v2. Sides play a round robin; every round each pair gets its own freshly generated arena copy
 * (all copies share the round's theme). Kit select (menu) behind glass gates, a one-life fight (no time limit by
 * default; config fight-seconds > 0 sets one), and a
 * short results phase. Win by filling the 3x3 centre with your team's blocks, or by having more centre blocks
 * at time-out.
 */
public final class BattleBoxGame implements McmGame {
    static final String WORLD_NAME = "mcc_battlebox";

    static final int ARENA_Y = 64;
    static final int ARENA_SPACING = 100;
    static final int HOLD_X = -200, HOLD_Y = 110, HOLD_Z = 0;
    static final int ADMIN_X = 0, ADMIN_Z = -400;

    private static final int INTRO_SECONDS = 10;
    private static final int PRACTICE_ROUNDS = 3;
    private static final int MIN_ROUNDS = 3;

    static final int KILL_POINTS = 3;
    static final int WIN_POINTS = 8;
    static final int DRAW_POINTS = 3;
    static final int BYE_POINTS = 4;
    private static final long ASSIST_WINDOW_MS = 10_000;

    enum Phase { IDLE, INTRO, BUILDING, KIT, FIGHT, END }

    private final BattleBoxPlugin plugin;
    final Abilities abilities;

    private McmGameContext ctx;
    private World world;
    private Phase phase = Phase.IDLE;
    private boolean running;
    private boolean finished;

    private BukkitTask secondTask, fastTask;
    private McmBuild.BuildQueue queue;
    private McmBuild.BuildQueue adminQueue;
    private int buildToken;
    private boolean buildDone;

    // config (read at start)
    private boolean woolMode;
    private boolean rotate;
    private boolean preferImported;
    private String forceTheme;
    private int firstKitSeconds, kitSeconds, fightSeconds, endSeconds, maxRounds;
    /** Seconds since the gates opened (shown instead of a countdown when there is no fight time limit). */
    private int fightElapsed;
    private ArenaGeo geo = new ArenaGeo();

    private final List<Side> sides = new ArrayList<>();
    private final List<List<Side[]>> schedule = new ArrayList<>();
    private final List<Match> matches = new ArrayList<>();
    private final List<Side> byes = new ArrayList<>();
    private final Set<UUID> roster = new HashSet<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Map<UUID, Match> matchOf = new HashMap<>();
    private final Map<UUID, Kit> kitChoice = new HashMap<>();
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, UUID> lastAttacker = new HashMap<>();
    private final Map<UUID, Long> lastAttackTime = new HashMap<>();

    private int round;
    private int totalRounds;
    private int secondsLeft;
    private int phaseTotal;
    private ArenaTheme theme;
    private ArenaTheme fixedTheme;
    private ArenaTheme adminTheme;
    private String lastThemeId;
    private String adminNextTheme;   // /mcm map: theme for the next round (one-shot)
    private final Random random = new Random();

    BattleBoxGame(BattleBoxPlugin plugin) {
        this.plugin = plugin;
        this.abilities = new Abilities(plugin, this);
    }

    @Override public String id() { return "battlebox"; }
    @Override public String displayName() { return "Battle Box"; }
    @Override public Material icon() { return Material.YELLOW_CONCRETE; }
    @Override public String description() { return "Side vs side: fill the 3x3 centre with your team's blocks."; }
    @Override public boolean useCoreScoreboard() { return true; }

    // =====================================================================
    // lifecycle
    // =====================================================================

    private void readConfig() {
        var c = plugin.getConfig();
        woolMode = "wool".equalsIgnoreCase(c.getString("centre-block", "concrete"));
        Gear.centreToolEfficiency = Math.max(0, Math.min(5, c.getInt("centre-tool-efficiency", 0)));
        rotate = c.getBoolean("rotate-maps-each-round", true);
        preferImported = c.getBoolean("prefer-imported-maps", true);
        forceTheme = c.getString("force-theme", "");
        firstKitSeconds = Math.max(5, c.getInt("first-kit-seconds", 20));
        kitSeconds = Math.max(5, c.getInt("kit-seconds", 15));
        // 0 (default) = no time limit: a round ends only when a side fills the centre, every match is decided
        // (both sides out, or the practice side is out) or an admin skips it. > 0 = limit in seconds (min 15).
        int fs = c.getInt("fight-seconds", 0);
        fightSeconds = fs <= 0 ? 0 : Math.max(15, fs);
        KitMenu.timeLimit = fightSeconds > 0;
        endSeconds = Math.max(3, c.getInt("end-seconds", 5));
        maxRounds = Math.max(1, Math.min(15, c.getInt("max-rounds", 9)));
        geo = new ArenaGeo();
    }

    @Override
    public void start(McmGameContext context) {
        if (running) stop();
        this.ctx = context;
        this.finished = false;
        clearState();
        readConfig();

        world = ensureWorld();
        if (world == null) {
            McmGameContext c = ctx;
            ctx = null;
            finished = true;
            c.broadcast(ChatColor.RED + "Battle Box could not create its world.");
            c.finish("Battle Box world failed to load");
            return;
        }
        running = true;
        queue = new McmBuild.BuildQueue(plugin);

        for (Player p : players()) {
            roster.add(p.getUniqueId());
            names.put(p.getUniqueId(), p.getName());
        }
        buildSides();
        buildSchedule();

        // holding area (small, built synchronously so players can be teleported right away)
        McmBuild.BuildQueue hq = new McmBuild.BuildQueue(plugin);
        ArenaPlacer.enqueue(hq, world, HOLD_X, HOLD_Y, HOLD_Z, HoldingArea.generate(), plugin.getLogger());
        hq.flush();
        removeEntities();
        for (Player p : players()) {
            prep(p, GameMode.ADVENTURE);
            p.teleport(holdSpawn());
        }

        phase = Phase.INTRO;
        secondsLeft = phaseTotal = FastTest.secs(INTRO_SECONDS);
        if (totalRounds > 0) prepareRound();
        intro();
        ctx.setTimer(secondsLeft, phaseTotal);
        updateHud();
        secondTask = Bukkit.getScheduler().runTaskTimer(plugin, this::secondTick, 20L, 20L);
        fastTask = Bukkit.getScheduler().runTaskTimer(plugin, this::fastTick, 5L, 5L);
    }

    @Override
    public void stop() {
        cancelTasks();
        boolean wasRunning = running;
        running = false;
        phase = Phase.IDLE;
        if (wasRunning) {
            if (ctx != null) for (Player p : players()) cleanPlayer(p);
            removeEntities();
        }
        ctx = null;
        clearState();
    }

    private void finishGame(String reason) {
        if (finished || ctx == null) return;
        finished = true;
        McmGameContext c = ctx;
        List<Map.Entry<UUID, Integer>> top = new ArrayList<>(kills.entrySet());
        top.sort((x, y) -> y.getValue() - x.getValue());
        c.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Battle Box" + ChatColor.YELLOW + " is over! Top fighters:");
        for (int i = 0; i < Math.min(3, top.size()); i++) {
            Map.Entry<UUID, Integer> e = top.get(i);
            c.broadcast(ChatColor.GRAY + " #" + (i + 1) + " " + ChatColor.WHITE + names.getOrDefault(e.getKey(), "?")
                    + ChatColor.GRAY + " - " + ChatColor.YELLOW + e.getValue() + " eliminations");
        }
        cancelTasks();
        running = false;
        phase = Phase.IDLE;
        for (Player p : players()) cleanPlayer(p);
        removeEntities();
        ctx = null;
        clearState();
        c.finish(reason);
    }

    private void clearState() {
        sides.clear();
        schedule.clear();
        matches.clear();
        byes.clear();
        roster.clear();
        names.clear();
        matchOf.clear();
        kitChoice.clear();
        kills.clear();
        lastAttacker.clear();
        lastAttackTime.clear();
        round = 0;
        totalRounds = 0;
        theme = null;
        fixedTheme = null;
        lastThemeId = null;
        adminNextTheme = null;
        buildDone = false;
    }

    private void cancelTasks() {
        cancel(secondTask);
        cancel(fastTask);
        secondTask = fastTask = null;
        buildToken++;
        if (queue != null) { queue.cancel(); queue = null; }
        abilities.cancelAll();
    }

    private static void cancel(BukkitTask t) {
        if (t != null) { try { t.cancel(); } catch (Throwable ignored) { } }
    }

    World ensureWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            try {
                w = new WorldCreator(WORLD_NAME).generator(new VoidGenerator()).environment(World.Environment.NORMAL)
                        .generateStructures(false).createWorld();
            } catch (Throwable t) {
                plugin.getLogger().severe("Could not create world " + WORLD_NAME + ": " + t);
                return null;
            }
        }
        if (w == null) return null;
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        w.setGameRule(GameRule.DO_TILE_DROPS, false);
        w.setGameRule(GameRule.DO_ENTITY_DROPS, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.MOB_GRIEFING, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.DO_INSOMNIA, false);
        w.setGameRule(GameRule.DO_PATROL_SPAWNING, false);
        w.setGameRule(GameRule.DO_TRADER_SPAWNING, false);
        w.setGameRule(GameRule.DO_WARDEN_SPAWNING, false);
        w.setGameRule(GameRule.SPAWN_RADIUS, 0);
        w.setGameRule(GameRule.NATURAL_REGENERATION, true);
        w.setTime(6000);
        w.setStorm(false);
        w.setThundering(false);
        w.setDifficulty(Difficulty.NORMAL);
        w.setPVP(true);
        w.setAutoSave(false);
        w.setSpawnLocation(HOLD_X, HOLD_Y + 1, HOLD_Z);
        return w;
    }

    // =====================================================================
    // sides and schedule
    // =====================================================================

    private static final DyeColor[] SQUAD_COLOURS = {
            DyeColor.RED, DyeColor.BLUE, DyeColor.LIME, DyeColor.ORANGE, DyeColor.PURPLE, DyeColor.CYAN,
            DyeColor.PINK, DyeColor.YELLOW, DyeColor.LIGHT_BLUE, DyeColor.GREEN, DyeColor.MAGENTA, DyeColor.BROWN,
            DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BLACK
    };

    private void buildSides() {
        Map<String, List<Player>> raw = ctx.sides();
        Set<DyeColor> used = EnumSet.noneOf(DyeColor.class);
        List<Player> solos = new ArrayList<>();
        List<String> pendingKeys = new ArrayList<>();
        List<List<UUID>> pendingMembers = new ArrayList<>();
        for (Map.Entry<String, List<Player>> e : new ArrayList<>(raw.entrySet())) {
            List<Player> list = e.getValue() == null ? List.of() : new ArrayList<>(e.getValue());
            if (list.isEmpty()) continue;
            String key = e.getKey();
            if (key != null && key.startsWith("TEAM:")) {
                String team = key.substring(5);
                DyeColor dye = Side.dyeForTeam(team);
                List<UUID> ids = new ArrayList<>();
                for (Player p : list) ids.add(p.getUniqueId());
                if (dye == null || dye == DyeColor.WHITE || used.contains(dye)) {
                    pendingKeys.add(key);
                    pendingMembers.add(ids);
                    continue;
                }
                used.add(dye);
                ChatColor chat = ctx.teamColor(team);
                Side s = new Side(ctx.sideLabel(key), chat == null ? Side.chatFor(dye) : chat, dye, false);
                s.members.addAll(ids);
                sides.add(s);
            } else {
                solos.addAll(list);
            }
        }
        for (int i = 0; i < pendingKeys.size(); i++) {
            DyeColor dye = nextColour(used);
            Side s = new Side(ctx.sideLabel(pendingKeys.get(i)), Side.chatFor(dye), dye, false);
            s.members.addAll(pendingMembers.get(i));
            sides.add(s);
        }
        if (!solos.isEmpty()) {
            // squads with a target size of 4; at least two squads when there is no tournament team
            int n = solos.size();
            int groups = Math.max((n + 3) / 4, (sides.isEmpty() && n >= 2) ? 2 : 1);
            Collections.shuffle(solos, random);
            List<List<Player>> grouped = new ArrayList<>();
            for (int g = 0; g < groups; g++) grouped.add(new ArrayList<>());
            for (int i = 0; i < n; i++) grouped.get(i % groups).add(solos.get(i));
            for (List<Player> g : grouped) {
                if (g.isEmpty()) continue;
                DyeColor dye = nextColour(used);
                ChatColor chat = Side.chatFor(dye);
                String label = g.size() == 1 ? chat + g.get(0).getName() : chat + Side.niceName(dye) + " Squad";
                Side s = new Side(label, chat, dye, false);
                for (Player p : g) s.members.add(p.getUniqueId());
                sides.add(s);
            }
        }
        if (sides.size() == 1 && sides.get(0).members.size() >= 2) {
            // everyone on one team: split it so there is someone to fight
            Side only = sides.get(0);
            DyeColor dyeB = nextColour(used);
            Side a = new Side(only.label + " A", only.chat, only.dye, false);
            Side b = new Side(Side.chatFor(dyeB) + ChatColor.stripColor(only.label) + " B", Side.chatFor(dyeB), dyeB, false);
            List<UUID> m = new ArrayList<>(only.members);
            Collections.shuffle(m, random);
            for (int i = 0; i < m.size(); i++) (i % 2 == 0 ? a : b).members.add(m.get(i));
            sides.clear();
            sides.add(a);
            sides.add(b);
        }
    }

    private DyeColor nextColour(Set<DyeColor> used) {
        for (DyeColor c : SQUAD_COLOURS) if (!used.contains(c)) { used.add(c); return c; }
        return SQUAD_COLOURS[random.nextInt(SQUAD_COLOURS.length)];
    }

    private void buildSchedule() {
        schedule.clear();
        if (sides.isEmpty()) { totalRounds = 0; return; }
        if (sides.size() == 1) {
            Side dummy = new Side(ChatColor.GRAY + "Practice Dummy", ChatColor.GRAY,
                    sides.get(0).dye == DyeColor.BLUE ? DyeColor.RED : DyeColor.BLUE, true);
            for (int r = 0; r < PRACTICE_ROUNDS; r++) {
                List<Side[]> pairs = new ArrayList<>();
                pairs.add(new Side[]{sides.get(0), dummy});
                schedule.add(pairs);
            }
        } else {
            List<Side> list = new ArrayList<>(sides);
            Collections.shuffle(list, random);
            if (list.size() % 2 == 1) list.add(null); // bye slot
            int n = list.size();
            int want = Math.min(maxRounds, Math.max(n - 1, MIN_ROUNDS));
            for (int r = 0; r < want; r++) {
                List<Side[]> pairs = new ArrayList<>();
                for (int i = 0; i < n / 2; i++) {
                    Side x = list.get(i), y = list.get(n - 1 - i);
                    pairs.add(r % 2 == 0 ? new Side[]{x, y} : new Side[]{y, x});
                }
                schedule.add(pairs);
                Side last = list.remove(n - 1);   // circle method: keep the first, rotate the rest
                list.add(1, last);
            }
        }
        totalRounds = schedule.size();
    }

    // =====================================================================
    // admin testing hooks (/mcm skipwait | skip | map); no @Override so older cores still load this jar
    // =====================================================================

    /** End the intro, kit selection or round-results wait now. */
    public boolean skipWait() {
        if (!running || ctx == null) return false;
        if (phase != Phase.INTRO && phase != Phase.KIT && phase != Phase.END) return false;
        secondsLeft = 1;
        secondTick();   // same transition as the timer running out
        return true;
    }

    /** End the current round now with no points for it, then go on to the next round (or finish after the last). */
    public boolean skipRound() {
        if (!running || ctx == null) return false;
        if (phase != Phase.KIT && phase != Phase.FIGHT && phase != Phase.END) return false;
        if (phase != Phase.END) {
            for (Match m : matches) {
                if (m.done) continue;
                m.done = true;          // no winner, no points
                m.result = "skipped by admin";
            }
            ctx.broadcast(ChatColor.GRAY + "Round " + (round + 1) + " was skipped by an admin (no points).");
            phase = Phase.END;
        }
        secondsLeft = 1;
        secondTick();   // the normal END -> next round / finish path
        return true;
    }

    public List<String> maps() { return Themes.ids(); }

    public boolean forceNextMap(String id) {
        ArenaTheme t = Themes.byId(id);
        if (t == null) return false;
        adminNextTheme = t.id();
        return true;
    }

    // =====================================================================
    // round flow
    // =====================================================================

    private ArenaTheme pickTheme() {
        if (adminNextTheme != null) {
            ArenaTheme a = Themes.byId(adminNextTheme);
            adminNextTheme = null;
            if (a != null) return a;
        }
        ArenaTheme forced = Themes.byId(forceTheme);
        if (forced != null && forceTheme != null && !forceTheme.isBlank()) return forced;
        List<ArenaTheme> imported = Themes.imported();
        List<ArenaTheme> all = (preferImported && !imported.isEmpty()) ? imported : Themes.all();
        if (!rotate) {
            if (fixedTheme == null) fixedTheme = all.get(random.nextInt(all.size()));
            return fixedTheme;
        }
        List<ArenaTheme> pool = new ArrayList<>();
        for (ArenaTheme t : all) if (!t.id().equals(lastThemeId)) pool.add(t);
        if (pool.isEmpty()) pool = all;
        return pool.get(random.nextInt(pool.size()));
    }

    static Location arenaOrigin(World w, int slot) {
        return new Location(w, slot * ARENA_SPACING, ARENA_Y, 0);
    }

    private Location local(int slot, double x, double y, double z, float yaw, float pitch) {
        Location o = arenaOrigin(world, slot);
        return new Location(world, o.getX() + x, o.getY() + y, o.getZ() + z, yaw, pitch);
    }

    /** Spectator overview high above a side's half of an arena copy, looking down at the centre. */
    private Location overview(int slot, boolean sideB, int i) {
        double[] v = theme != null ? theme.overview(geo, sideB, i) : geo.overview(sideB, i);
        return local(slot, v[0], v[1], v[2], (float) v[3], (float) v[4]);
    }

    /** Keep spectating players inside their own arena's box (teleport back to the overview if they leave). */
    private void keepSpectatorsInside() {
        if (phase != Phase.KIT && phase != Phase.FIGHT && phase != Phase.END) return;
        for (Player p : players()) {
            if (p.getGameMode() != GameMode.SPECTATOR || !p.getWorld().equals(world) || !roster.contains(p.getUniqueId())) continue;
            Match m = matchOf.get(p.getUniqueId());
            int slot = m == null ? 0 : m.arena;
            boolean b = m != null && m.isB(m.sideOf(p.getUniqueId()));
            Location o = arenaOrigin(world, slot);
            Location l = p.getLocation();
            double lx = l.getX() - o.getX(), ly = l.getY() - o.getY(), lz = l.getZ() - o.getZ();
            boolean inside = theme != null ? theme.inSpectatorBox(geo, lx, ly, lz) : geo.inSpectatorBox(lx, ly, lz);
            if (!inside) {
                p.teleport(overview(slot, b, 0));
                McmFx.actionBar(p, ChatColor.GRAY + "Spectators stay inside their arena.");
            }
        }
    }

    private Location holdSpawn() {
        return new Location(world, HOLD_X + 0.5, HOLD_Y + 1, HOLD_Z + 0.5, random.nextFloat() * 360f, 0f);
    }

    /** Create this round's matches and queue their arena copies. startKit runs when the build is done. */
    private void prepareRound() {
        matches.clear();
        byes.clear();
        matchOf.clear();
        kitChoice.clear();
        lastAttacker.clear();
        lastAttackTime.clear();
        theme = pickTheme();
        lastThemeId = theme.id();
        int arena = 0;
        for (Side[] pair : schedule.get(round)) {
            if (pair[0] != null && pair[1] != null) {
                Match m = new Match(arena++, pair[0], pair[1]);
                matches.add(m);
                for (UUID u : m.a.members) matchOf.put(u, m);
                for (UUID u : m.b.members) matchOf.put(u, m);
            } else if (pair[0] != null) byes.add(pair[0]);
            else if (pair[1] != null) byes.add(pair[1]);
        }
        buildDone = false;
        final int token = ++buildToken;
        if (queue == null) queue = new McmBuild.BuildQueue(plugin);
        long seed = random.nextLong();
        for (Match m : matches) {
            Grid g = theme.generate(geo, seed, Side.colourName(m.dyeA), Side.colourName(m.dyeB),
                    Side.centreBlock(m.neutral, woolMode).getKey().getKey());
            Location o = arenaOrigin(world, m.arena);
            ArenaPlacer.enqueue(queue, world, o.getBlockX(), o.getBlockY(), o.getBlockZ(), g, plugin.getLogger());
        }
        queue.then(() -> {
            if (!running || token != buildToken) return;
            buildDone = true;
            if (phase == Phase.BUILDING) startKit();
        }).start();
    }

    private void intro() {
        ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Battle Box" + ChatColor.GRAY + " - " + sides.size()
                + " sides, " + totalRounds + " round" + (totalRounds == 1 ? "" : "s") + ", a new arena every round.");
        ctx.broadcast(ChatColor.YELLOW + " Pick a kit, then the gates open. One life per round"
                + (fightSeconds > 0 ? ", " + fightSeconds + " seconds." : ", no time limit."));
        ctx.broadcast(ChatColor.YELLOW + " Fill the " + ChatColor.WHITE + "3x3 centre" + ChatColor.YELLOW
                + " with your " + (woolMode ? "wool" : "concrete") + " to win"
                + (fightSeconds > 0 ? " instantly; at time-out the most centre blocks wins." : " - the round lasts until a side does."));
        ctx.broadcast(ChatColor.GRAY + " Points: " + KILL_POINTS + " per elimination, " + WIN_POINTS + " each for a round win, "
                + DRAW_POINTS + " for a draw, " + BYE_POINTS + " for a bye.");
        if (sides.size() == 1) ctx.broadcast(ChatColor.GRAY + " Only one side: practice rounds against an empty arena.");
        McmFx.titleCard(players(), ChatColor.GOLD + "" + ChatColor.BOLD + "Battle Box", ChatColor.YELLOW + "Fill the centre with your colour!");
    }

    private void secondTick() {
        if (!running || ctx == null) return;
        boolean untimedFight = phase == Phase.FIGHT && fightSeconds <= 0;
        if (untimedFight) {
            fightElapsed++;
            ctx.setTimer(fightElapsed, Math.max(1, fightElapsed));   // boss bar counts up, bar stays full
        } else {
            if (phase != Phase.BUILDING) secondsLeft--;
            ctx.setTimer(Math.max(0, secondsLeft), Math.max(1, phaseTotal));
        }
        switch (phase) {
            case INTRO -> {
                if (secondsLeft <= 0) {
                    if (totalRounds == 0) { finishGame("Battle Box had no players"); return; }
                    if (buildDone) startKit();
                    else phase = Phase.BUILDING;
                }
            }
            case KIT -> {
                if (secondsLeft > 0 && secondsLeft <= 3) {
                    ChatColor c = secondsLeft == 1 ? ChatColor.GREEN : secondsLeft == 2 ? ChatColor.YELLOW : ChatColor.RED;
                    for (Player p : inMatches()) {
                        McmFx.title(p, c + "" + ChatColor.BOLD + secondsLeft, ChatColor.GRAY + "Gates open soon", 0, 22, 0);
                        McmFx.sound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.4f - 0.2f * (secondsLeft - 1));
                    }
                }
                if (secondsLeft <= 0) startFight();
            }
            case FIGHT -> {
                if (untimedFight) break;   // no limit: only the win conditions (endMatch) end the round
                if (secondsLeft == 30 || secondsLeft == 10) {
                    for (Player p : inMatches()) {
                        McmFx.actionBar(p, ChatColor.YELLOW + "" + secondsLeft + " seconds left!");
                        McmFx.sound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1f);
                    }
                }
                if (secondsLeft > 0 && secondsLeft <= 5) for (Player p : inMatches()) McmFx.sound(p, Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.5f);
                if (secondsLeft <= 0) startEnd();
            }
            case END -> {
                if (secondsLeft <= 0) {
                    round++;
                    if (round >= totalRounds) { finishGame("Battle Box complete"); return; }
                    toHolding();
                    phase = Phase.BUILDING;
                    secondsLeft = phaseTotal = 1;
                    prepareRound();
                }
            }
            default -> { }
        }
        if (running) updateHud();
    }

    private void toHolding() {
        removeEntities();
        for (Player p : players()) {
            if (!roster.contains(p.getUniqueId())) continue;
            prep(p, GameMode.ADVENTURE);
            p.teleport(holdSpawn());
            McmFx.actionBar(p, ChatColor.GRAY + "Building the next arena...");
        }
    }

    private void startKit() {
        if (!running) return;
        phase = Phase.KIT;
        removeEntities();
        Map<Side, Integer> spawnIdx = new HashMap<>();
        for (Player p : players()) {
            UUID u = p.getUniqueId();
            if (!roster.contains(u)) continue;
            Match m = matchOf.get(u);
            if (m != null) {
                Side s = m.sideOf(u);
                int idx = spawnIdx.merge(s, 1, Integer::sum) - 1;
                prep(p, GameMode.ADVENTURE);
                dress(p, m.dyeOf(s));
                p.getInventory().setItem(4, Gear.custom(Gear.Custom.MENU, 1));
                p.getInventory().setHeldItemSlot(4);
                double[] sp = theme != null ? theme.spawn(geo, m.isB(s), idx) : geo.spawn(m.isB(s), idx);
                p.teleport(local(m.arena, sp[0], sp[1], sp[2], (float) sp[3], 0f));
                Side opp = m.opponent(s);
                McmFx.title(p, ChatColor.GOLD + "" + ChatColor.BOLD + "Round " + (round + 1),
                        ChatColor.WHITE + theme.displayName() + ChatColor.GRAY + " - vs " + opp.label, 5, 50, 10);
                p.sendMessage(ChatColor.GOLD + "Round " + (round + 1) + "/" + totalRounds + ChatColor.GRAY + " - "
                        + ChatColor.WHITE + theme.displayName() + ChatColor.GRAY + ": you are " + s.label + ChatColor.GRAY
                        + " vs " + opp.label + ChatColor.GRAY + ". Pick a kit (menu or the star in your hotbar).");
                McmFx.sound(p, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (phase == Phase.KIT && p.isOnline() && matchOf.get(u) == m) openKitMenu(p);
                }, 10L);
            } else {
                prep(p, GameMode.SPECTATOR);
                p.teleport(overview(0, false, 0));
                McmFx.title(p, ChatColor.GRAY + "Bye round", ChatColor.WHITE + "Your side sits this one out (+" + BYE_POINTS + ")", 5, 50, 10);
            }
        }
        secondsLeft = phaseTotal = FastTest.secs((round == 0) ? firstKitSeconds : kitSeconds);
        ctx.setTimer(secondsLeft, phaseTotal);
        updateHud();
    }

    private void startFight() {
        phase = Phase.FIGHT;
        fightElapsed = 0;
        secondsLeft = phaseTotal = fightSeconds;
        if (fightSeconds > 0) ctx.setTimer(secondsLeft, phaseTotal);
        else ctx.setTimer(0, 1);
        Set<UUID> online = onlineIds();
        for (Match m : matches) {
            openGates(m);
            for (Side s : new Side[]{m.a, m.b}) {
                if (s.dummy) continue;
                for (UUID u : s.members) {
                    if (!online.contains(u)) continue;
                    Player p = Bukkit.getPlayer(u);
                    if (p == null) continue;
                    Kit k = kitChoice.get(u);
                    if (k == null) {
                        k = randomFreeKit(s, online);
                        kitChoice.put(u, k);
                        p.sendMessage(ChatColor.GRAY + "You didn't pick a kit, so you got " + k.colored() + ChatColor.GRAY + ".");
                    }
                    p.closeInventory();
                    m.alive.add(u);
                    prep(p, GameMode.SURVIVAL);   // survival: the centre tool must be able to break blocks
                    giveKit(p, k, m.dyeOf(s));
                    McmFx.title(p, ChatColor.RED + "" + ChatColor.BOLD + "FIGHT!", ChatColor.YELLOW + "Fill the centre with "
                            + Side.chatFor(m.dyeOf(s)) + Side.niceName(m.dyeOf(s)), 0, 30, 10);
                    McmFx.sound(p, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.5f);
                    McmFx.sound(p, Sound.BLOCK_PISTON_EXTEND, 1f, 0.7f);
                }
            }
        }
        for (Side s : byes) for (UUID u : s.members) if (online.contains(u)) award(u, BYE_POINTS, "bye");
        for (Match m : new ArrayList<>(matches)) checkAlive(m);
        updateHud();
    }

    /**
     * Drop both gates of a match: every gate cell (team stained glass) vanishes with block particles, then a
     * glass-break + piston + bell sting. A gate cell that is not stained glass is logged (and cleared anyway, so
     * nobody is ever locked in).
     */
    private void openGates(Match m) {
        Location o = arenaOrigin(world, m.arena);
        if (theme != null && theme.isImported()) {
            for (boolean b : new boolean[]{false, true}) {
                double[] sp = theme.spawn(geo, b, 0);
                Location g = o.clone().add(sp[0], sp[1] + 1.0, sp[2]);
                world.spawnParticle(Particle.CLOUD, g, 12, 0.6, 0.6, 0.6, 0.04);
                world.playSound(g, Sound.BLOCK_GLASS_BREAK, 1.5f, 0.8f);
                world.playSound(g, Sound.BLOCK_PISTON_CONTRACT, 1.5f, 0.6f);
                world.playSound(g, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 1.4f);
            }
            world.playSound(o.clone().add(theme.centreX() + 0.5, theme.centreY(geo) + 3, theme.centreZ() + 0.5),
                    Sound.ITEM_GOAT_HORN_SOUND_0, 2f, 1.2f);
            return;
        }
        int missing = 0;
        for (int sg = -1; sg <= 1; sg += 2) {
            for (int x = -geo.GATE_HALF; x <= geo.GATE_HALF; x++)
                for (int y = 1; y <= geo.GATE_TOP; y++) {
                    Block b = world.getBlockAt(o.getBlockX() + x, o.getBlockY() + y, o.getBlockZ() + sg * geo.W);
                    Material t = b.getType();
                    if (!t.name().endsWith("_STAINED_GLASS")) {
                        missing++;
                        plugin.getLogger().warning("Battle Box: gate block at " + b.getX() + "," + b.getY() + "," + b.getZ()
                                + " (arena " + m.arena + ") was " + t + ", expected team stained glass");
                        if (t.isAir()) continue;
                    }
                    Location c = b.getLocation().add(0.5, 0.5, 0.5);
                    world.spawnParticle(Particle.BLOCK, c, 14, 0.3, 0.3, 0.3, 0.1, b.getBlockData());
                    if (y == 2) world.spawnParticle(Particle.CLOUD, c, 2, 0.2, 0.2, 0.2, 0.02);
                    b.setType(Material.AIR, false);
                }
            Location g = o.clone().add(0.5, 2, sg * geo.W + 0.5);
            world.playSound(g, Sound.BLOCK_GLASS_BREAK, 2f, 0.7f);
            world.playSound(g, Sound.BLOCK_PISTON_CONTRACT, 1.5f, 0.6f);
            world.playSound(g, Sound.BLOCK_NOTE_BLOCK_BELL, 1.5f, 1.4f);
        }
        if (missing > 0) plugin.getLogger().warning("Battle Box: " + missing + " gate block(s) in arena " + m.arena + " were not glass.");
        world.playSound(o.clone().add(0.5, 4, 0.5), Sound.ITEM_GOAT_HORN_SOUND_0, 2f, 1.2f);
    }

    private Kit randomFreeKit(Side s, Set<UUID> online) {
        int[] used = new int[Kit.values().length];
        for (UUID o : s.members) { Kit k = kitChoice.get(o); if (k != null && online.contains(o)) used[k.ordinal()]++; }
        int min = Integer.MAX_VALUE;
        for (int v : used) min = Math.min(min, v);
        List<Kit> pool = new ArrayList<>();
        for (Kit k : Kit.values()) if (used[k.ordinal()] == min) pool.add(k);
        return pool.get(random.nextInt(pool.size()));
    }

    private void giveKit(Player p, Kit k, DyeColor dye) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        dress(p, dye);
        int slot = 0;
        for (ItemStack it : Loadouts.items(theme.id(), k)) {
            if (it.getType() == Material.ARROW) inv.setItem(9, it);
            else inv.setItem(slot++, it);
        }
        inv.setItem(7, Gear.centreTool(woolMode));
        inv.setItem(8, Gear.centreBlocks(dye, woolMode, 16));
        inv.setHeldItemSlot(0);
    }

    private static void dress(Player p, DyeColor dye) {
        PlayerInventory inv = p.getInventory();
        inv.setHelmet(Gear.leather(Material.LEATHER_HELMET, dye));
        inv.setChestplate(Gear.leather(Material.LEATHER_CHESTPLATE, dye));
        inv.setLeggings(Gear.leather(Material.LEATHER_LEGGINGS, dye));
        inv.setBoots(Gear.leather(Material.LEATHER_BOOTS, dye));
    }

    private void startEnd() {
        if (phase == Phase.END || !running) return;
        phase = Phase.END;
        secondsLeft = phaseTotal = FastTest.secs(endSeconds);
        ctx.setTimer(secondsLeft, phaseTotal);
        for (Match m : new ArrayList<>(matches)) {
            if (m.done) continue;
            int[] c = centreCounts(m);
            if (m.b.dummy) { endMatch(m, c[0] == 9 ? m.a : null, "time ran out (" + c[0] + "/9 centre)"); continue; }
            int aa = m.aliveOf(m.a), ab = m.aliveOf(m.b);
            if (c[0] != c[1]) endMatch(m, c[0] > c[1] ? m.a : m.b, "more centre blocks (" + Math.max(c[0], c[1]) + " vs " + Math.min(c[0], c[1]) + ")");
            else if (aa > 0 && ab == 0) endMatch(m, m.a, "centre tied - last side standing");
            else if (ab > 0 && aa == 0) endMatch(m, m.b, "centre tied - last side standing");
            else endMatch(m, null, "centre tied " + c[0] + "-" + c[1]);
        }
        ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Round " + (round + 1) + " results " + ChatColor.GRAY + "(" + theme.displayName() + ")");
        for (Match m : matches) {
            ctx.broadcast(ChatColor.GRAY + " " + m.a.label + ChatColor.GRAY + " vs " + m.b.label + ChatColor.GRAY + ": "
                    + (m.winner == null ? ChatColor.YELLOW + "draw" : m.winner.label + ChatColor.GREEN + " won")
                    + ChatColor.DARK_GRAY + " (" + m.result + ")");
        }
        for (Side s : byes) ctx.broadcast(ChatColor.GRAY + " " + s.label + ChatColor.GRAY + " had a bye.");
        updateHud();
    }

    // =====================================================================
    // match logic
    // =====================================================================

    int[] centreCounts(Match m) {
        int ca = 0, cb = 0;
        Location o = arenaOrigin(world, m.arena);
        int cx = theme != null ? theme.centreX() : 0;
        int cy = theme != null ? theme.centreY(geo) : geo.CENTRE_Y;
        int cz = theme != null ? theme.centreZ() : 0;
        for (int x = -1; x <= 1; x++)
            for (int z = -1; z <= 1; z++) {
                Block b = world.getBlockAt(o.getBlockX() + cx + x, o.getBlockY() + cy, o.getBlockZ() + cz + z);
                DyeColor d = Side.dyeOfCentre(b.getType());
                if (d == null) continue;
                if (d == m.dyeA) ca++;
                else if (d == m.dyeB) cb++;
            }
        return new int[]{ca, cb};
    }

    private void checkAlive(Match m) {
        if (m.done || phase != Phase.FIGHT) return;
        int a = m.aliveOf(m.a), b = m.aliveOf(m.b);
        if (m.b.dummy) {
            if (a == 0) endMatch(m, null, "no players left");
            return;
        }
        if (a == 0 && b == 0) {
            int[] c = centreCounts(m);
            if (c[0] != c[1]) endMatch(m, c[0] > c[1] ? m.a : m.b, "both sides out - more centre blocks");
            else endMatch(m, null, "both sides out");
        } else if (a == 0 || b == 0) {
            Side out = a == 0 ? m.a : m.b;
            for (Player p : players()) {
                if (!m.contains(p.getUniqueId())) continue;
                McmFx.title(p, "", out.label + ChatColor.GRAY + " is wiped out - fill the centre!", 0, 40, 10);
            }
        }
    }

    void checkCentre(Match m) {
        if (!running || m.done || phase != Phase.FIGHT || world == null) return;
        int[] c = centreCounts(m);
        if (c[0] == 9) endMatch(m, m.a, "filled the centre");
        else if (c[1] == 9 && !m.b.dummy) endMatch(m, m.b, "filled the centre");
    }

    private void endMatch(Match m, Side winner, String reason) {
        if (m.done) return;
        m.done = true;
        m.winner = winner;
        m.result = reason;
        Set<UUID> online = onlineIds();
        if (m.b.dummy) {
            // practice round: no tournament points
        } else if (winner != null) {
            for (UUID u : winner.members) if (online.contains(u)) award(u, WIN_POINTS, "round win");
        } else {
            for (Side s : new Side[]{m.a, m.b}) for (UUID u : s.members) if (online.contains(u)) award(u, DRAW_POINTS, "draw");
        }
        for (Side s : new Side[]{m.a, m.b}) {
            for (UUID u : s.members) {
                if (!online.contains(u)) continue;
                Player p = Bukkit.getPlayer(u);
                if (p == null) continue;
                if (winner == null) {
                    McmFx.title(p, ChatColor.YELLOW + "" + ChatColor.BOLD + "DRAW", ChatColor.GRAY + reason, 5, 50, 10);
                    McmFx.sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                } else if (winner == s) {
                    McmFx.title(p, ChatColor.GREEN + "" + ChatColor.BOLD + "ROUND WON!", ChatColor.GRAY + reason, 5, 50, 10);
                    McmFx.sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1f);
                } else {
                    McmFx.title(p, ChatColor.RED + "" + ChatColor.BOLD + "ROUND LOST", winner.label + ChatColor.GRAY + " " + reason, 5, 50, 10);
                    McmFx.sound(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
                if (m.alive.contains(u) && p.getGameMode() == GameMode.SURVIVAL) p.setGameMode(GameMode.ADVENTURE);
            }
        }
        if (winner != null) {
            int cx = theme != null ? theme.centreX() : 0;
            int cy = theme != null ? theme.centreY(geo) : geo.CENTRE_Y;
            int cz = theme != null ? theme.centreZ() : 0;
            Location c = arenaOrigin(world, m.arena).add(cx + 0.5, cy + 5, cz + 0.5);
            DyeColor d = m.dyeOf(winner);
            for (int i = 0; i < 4; i++) {
                final int k = i;
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    if (!running) return;
                    McmFx.teamFirework(c.clone().add((k % 2 == 0 ? 4 : -4), k, (k < 2 ? 4 : -4)), d.getColor(), k == 0);
                }, 4L * i);
            }
        }
        boolean allDone = true;
        for (Match x : matches) if (!x.done) { allDone = false; break; }
        if (allDone && phase == Phase.FIGHT) startEnd();
    }

    private void award(UUID u, int pts, String reason) {
        if (ctx == null) return;
        ctx.addPoints(u, pts, "Battle Box " + reason);
        Player p = Bukkit.getPlayer(u);
        if (p != null) {
            p.sendMessage(ChatColor.GOLD + "+" + pts + " " + ChatColor.GRAY + reason);
            McmFx.sound(p, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
        }
    }

    void markAttack(Player victim, Player attacker) {
        if (attacker == null || victim.getUniqueId().equals(attacker.getUniqueId())) return;
        lastAttacker.put(victim.getUniqueId(), attacker.getUniqueId());
        lastAttackTime.put(victim.getUniqueId(), System.currentTimeMillis());
    }

    private void eliminate(Player victim, Player killer, boolean dead) {
        UUID vu = victim.getUniqueId();
        Match m = matchOf.get(vu);
        if (m == null || !m.alive.remove(vu)) return;
        if (killer == null) {
            UUID la = lastAttacker.get(vu);
            Long t = lastAttackTime.get(vu);
            if (la != null && t != null && System.currentTimeMillis() - t <= ASSIST_WINDOW_MS) {
                Player k = Bukkit.getPlayer(la);
                if (k != null && roster.contains(la)) killer = k;
            }
        }
        if (killer != null && killer.getUniqueId().equals(vu)) killer = null;
        McmFx.eliminated(victim, killer, ctx);
        if (killer != null) {
            kills.merge(killer.getUniqueId(), 1, Integer::sum);
            award(killer.getUniqueId(), KILL_POINTS, "elimination");
        }
        if (!dead) toSpectator(victim, m);
        checkAlive(m);
    }

    private void toSpectator(Player p, Match m) {
        prep(p, GameMode.SPECTATOR);
        int i = Math.floorMod(p.getUniqueId().hashCode(), 3);
        p.teleport(overview(m.arena, m.isB(m.sideOf(p.getUniqueId())), i));
        p.sendMessage(ChatColor.GRAY + "You are spectating your arena until the next round. Fly around freely - you stay inside your arena.");
    }

    // =====================================================================
    // listener hooks
    // =====================================================================

    boolean isActive() { return running && world != null && ctx != null; }

    World world() { return world; }

    boolean handles(Player p) {
        return isActive() && p.getWorld().equals(world) && roster.contains(p.getUniqueId());
    }

    boolean inWorld(World w) { return isActive() && w != null && w.equals(world); }

    Phase phase() { return phase; }

    Match matchOf(UUID u) { return matchOf.get(u); }

    boolean woolMode() { return woolMode; }

    boolean freezeMovementDuringKit() {
        return phase == Phase.KIT && theme != null && theme.isImported();
    }

    boolean isAliveFighter(Player p) {
        Match m = matchOf.get(p.getUniqueId());
        return phase == Phase.FIGHT && m != null && !m.done && m.alive.contains(p.getUniqueId());
    }

    void onDamage(Player victim, EntityDamageEvent e) {
        UUID vu = victim.getUniqueId();
        if (!isAliveFighter(victim)) {
            e.setCancelled(true);
            if (e.getCause() == EntityDamageEvent.DamageCause.VOID) rescue(victim);
            return;
        }
        Match m = matchOf.get(vu);
        Player attacker = null;
        if (e instanceof EntityDamageByEntityEvent ed) {
            if (McmFx.isFxFirework(ed.getDamager())) { e.setCancelled(true); return; }
            attacker = playerSource(ed.getDamager());
            if (attacker != null) {
                if (attacker.getUniqueId().equals(vu)) { e.setCancelled(true); return; }
                Match am = matchOf.get(attacker.getUniqueId());
                if (am != m || !m.alive.contains(attacker.getUniqueId()) || m.sideOf(attacker.getUniqueId()) == m.sideOf(vu)) {
                    e.setCancelled(true);  // no friendly fire, no cross-arena damage, no damage from the eliminated
                    return;
                }
                markAttack(victim, attacker);
                // melee hit with a Charge in hand: apply it to the enemy
                if (ed.getDamager() == attacker) {
                    Gear.Custom c = Gear.customOf(attacker.getInventory().getItemInMainHand());
                    if (c != null && c.kind == Gear.Kind.CHARGE) abilities.chargeHit(attacker, victim, c);
                }
            }
        }
        if (e.isCancelled()) return;
        if (victim.getHealth() + victim.getAbsorptionAmount() - e.getFinalDamage() <= 0.0) {
            e.setCancelled(true);
            eliminate(victim, attacker, false);
        }
    }

    static Player playerSource(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        if (damager instanceof AreaEffectCloud c && c.getSource() instanceof Player p) return p;
        if (damager instanceof TNTPrimed t && t.getSource() instanceof Player p) return p;
        return null;
    }

    boolean sameSide(Player a, Player b) {
        if (a.getUniqueId().equals(b.getUniqueId())) return true;
        Match m = matchOf.get(a.getUniqueId());
        if (m == null || m != matchOf.get(b.getUniqueId())) return false;
        return m.sideOf(a.getUniqueId()) == m.sideOf(b.getUniqueId());
    }

    void onDeath(Player p) {
        if (isAliveFighter(p)) eliminate(p, null, true);
    }

    Location respawnLocation(Player p) {
        Match m = matchOf.get(p.getUniqueId());
        if (m != null) return overview(m.arena, m.isB(m.sideOf(p.getUniqueId())), 0);
        return holdSpawn();
    }

    void afterRespawn(Player p) {
        if (!handles(p)) return;
        if (phase == Phase.FIGHT || matchOf.get(p.getUniqueId()) == null) prep(p, GameMode.SPECTATOR);
    }

    void onQuit(Player p) {
        UUID u = p.getUniqueId();
        if (!isActive() || !roster.remove(u)) return;
        Match m = matchOf.get(u);
        if (m != null && m.alive.remove(u)) {
            Side s = m.sideOf(u);
            String msg = (s != null ? s.chat : ChatColor.WHITE) + p.getName() + ChatColor.GRAY + " left the game.";
            for (Player o : players()) if (m.contains(o.getUniqueId()) && !o.getUniqueId().equals(u)) o.sendMessage(msg);
            Bukkit.getScheduler().runTask(plugin, () -> { if (isActive()) checkAlive(m); });
        }
    }

    /** Is (b) one of the 9 centre cells of this match's arena? */
    boolean isCentreCell(Match m, Block b) {
        if (m == null || b == null || !b.getWorld().equals(world)) return false;
        Location o = arenaOrigin(world, m.arena);
        int lx = b.getX() - o.getBlockX(), ly = b.getY() - o.getBlockY(), lz = b.getZ() - o.getBlockZ();
        return theme != null ? theme.isCentreCell(geo, lx, ly, lz) : geo.isCentreCell(lx, ly, lz);
    }

    /**
     * Placement rule: alive fighter, own team's centre block, onto one of the 9 centre cells only, and only into
     * an empty cell (the cells start filled with neutral blocks, so something has to be broken first).
     */
    boolean canPlace(Player p, Block placed, Material type, boolean replacedEmpty) {
        if (!isAliveFighter(p) || !replacedEmpty) return false;
        Match m = matchOf.get(p.getUniqueId());
        DyeColor d = Side.dyeOfCentre(type);
        if (d == null || d != m.dyeOf(m.sideOf(p.getUniqueId()))) return false;
        if (woolMode != type.name().endsWith("_WOOL")) return false;
        return isCentreCell(m, placed);
    }

    /** Break rule: alive fighter, a neutral or enemy concrete/wool block on one of the 9 centre cells (not your own). */
    boolean canBreak(Player p, Block b) {
        if (!isAliveFighter(p)) return false;
        Match m = matchOf.get(p.getUniqueId());
        DyeColor d = Side.dyeOfCentre(b.getType());
        if (d == null || d == m.dyeOf(m.sideOf(p.getUniqueId()))) return false;
        if (woolMode != b.getType().name().endsWith("_WOOL")) return false;
        return isCentreCell(m, b);
    }

    void afterCentreChange(Player p) {
        Match m = matchOf.get(p.getUniqueId());
        if (m != null) Bukkit.getScheduler().runTask(plugin, () -> { checkCentre(m); updateActionBars(); });
    }

    // ---------- kit menu

    void openKitMenu(Player p) {
        Match m = matchOf.get(p.getUniqueId());
        if (m == null || phase != Phase.KIT || theme == null) return;
        Side s = m.sideOf(p.getUniqueId());
        Map<Kit, String> taken = new EnumMap<>(Kit.class);
        Set<UUID> online = onlineIds();
        int cap = kitCap(s, online);
        Map<Kit, List<String>> holders = new EnumMap<>(Kit.class);
        for (UUID o : s.members) {
            if (o.equals(p.getUniqueId()) || !online.contains(o)) continue;
            Kit k = kitChoice.get(o);
            if (k != null) holders.computeIfAbsent(k, x -> new ArrayList<>()).add(names.getOrDefault(o, "?"));
        }
        for (Map.Entry<Kit, List<String>> e : holders.entrySet())
            if (e.getValue().size() >= cap) taken.put(e.getKey(), String.join(", ", e.getValue()));
        KitMenu.open(p, theme.id(), theme.displayName(), woolMode, m.dyeOf(s), taken, kitChoice.get(p.getUniqueId()), false);
    }

    private int kitCap(Side s, Set<UUID> online) {
        int n = 0;
        for (UUID o : s.members) if (online.contains(o)) n++;
        return Math.max(1, (n + 4) / 5);
    }

    void selectKit(Player p, Kit k) {
        if (phase != Phase.KIT) return;
        UUID u = p.getUniqueId();
        Match m = matchOf.get(u);
        if (m == null) return;
        Side s = m.sideOf(u);
        if (s == null) return;
        if (kitChoice.get(u) == k) return;
        Set<UUID> online = onlineIds();
        int taken = 0;
        for (UUID o : s.members) if (!o.equals(u) && online.contains(o) && kitChoice.get(o) == k) taken++;
        if (taken >= kitCap(s, online)) {
            p.sendMessage(ChatColor.RED + "A teammate already has " + k.display + ". Pick another kit.");
            McmFx.sound(p, Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        kitChoice.put(u, k);
        // show the kit's items right away (they can't be used until the gates open)
        PlayerInventory inv = p.getInventory();
        inv.clear();
        DyeColor dye = m.dyeOf(s);
        dress(p, dye);
        int slot = 0;
        for (ItemStack it : Loadouts.items(theme.id(), k)) {
            if (it.getType() == Material.ARROW) inv.setItem(9, it); else inv.setItem(slot++, it);
        }
        inv.setItem(7, Gear.centreTool(woolMode));
        inv.setItem(8, Gear.custom(Gear.Custom.MENU, 1));
        inv.setItem(17, Gear.centreBlocks(dye, woolMode, 16));
        McmFx.sound(p, Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
        p.sendMessage(ChatColor.GREEN + "You picked " + k.colored() + ChatColor.GRAY + " (" + k.role + ").");
        for (UUID o : s.members) {
            if (o.equals(u) || !online.contains(o)) continue;
            Player t = Bukkit.getPlayer(o);
            if (t == null) continue;
            t.sendMessage(s.chat + p.getName() + ChatColor.GRAY + " picked " + k.colored());
            if (t.getOpenInventory().getTopInventory().getHolder() instanceof KitMenu km && !km.inspect) openKitMenu(t);
        }
        p.closeInventory();
        updateActionBars();
    }

    // ---------- admin helpers

    String currentThemeId() { return theme == null ? null : theme.id(); }
    /** Chat feedback log (core 3.2.0+ optional hook; no @Override so older cores still load us). */
    public String currentMap() { return currentThemeId(); }

    /** Build one arena copy of a theme at the admin spot and teleport the player into it. */
    void adminArena(Player p, ArenaTheme t) {
        World w = ensureWorld();
        if (w == null) { p.sendMessage(ChatColor.RED + "World unavailable."); return; }
        if (adminQueue != null) adminQueue.cancel();
        adminQueue = new McmBuild.BuildQueue(plugin);
        adminTheme = t;
        ArenaGeo g = new ArenaGeo();
        Grid grid = t.generate(g, System.nanoTime(), "red", "blue", "white_concrete");
        ArenaPlacer.enqueue(adminQueue, w, ADMIN_X, ARENA_Y, ADMIN_Z, grid, plugin.getLogger());
        long n = grid.count();
        p.sendMessage(ChatColor.YELLOW + "Building " + t.displayName() + " (" + n + " blocks, " + grid.distinctIds() + " block types)...");
        adminQueue.then(() -> {
            if (!p.isOnline()) return;
            double[] pv = t.previewLocation(g);
            p.teleport(new Location(w, ADMIN_X + pv[0], ARENA_Y + pv[1], ADMIN_Z + pv[2], (float) pv[3], (float) pv[4]));
            p.sendMessage(ChatColor.GREEN + "Built " + t.displayName() + ". " + ChatColor.GRAY + t.layoutNotes());
        }).start();
    }

    Location adminMin() {
        ArenaGeo g = new ArenaGeo();
        int[] mn = adminTheme != null ? adminTheme.minBounds(g) : new int[]{g.minX, g.minY, g.minZ};
        return new Location(ensureWorld(), ADMIN_X + mn[0], ARENA_Y + mn[1], ADMIN_Z + mn[2]);
    }

    Location adminMax() {
        ArenaGeo g = new ArenaGeo();
        int[] mx = adminTheme != null ? adminTheme.maxBounds(g) : new int[]{g.maxX, g.maxY, g.maxZ};
        return new Location(ensureWorld(), ADMIN_X + mx[0], ARENA_Y + mx[1], ADMIN_Z + mx[2]);
    }

    void cancelAdmin() {
        if (adminQueue != null) { adminQueue.cancel(); adminQueue = null; }
    }

    // =====================================================================
    // ticking + HUD
    // =====================================================================

    private void fastTick() {
        if (!running || world == null) return;
        keepSpectatorsInside();
        int voidY = ARENA_Y + (theme != null ? theme.voidY() : -8);
        if (phase == Phase.FIGHT) {
            for (Match m : new ArrayList<>(matches)) {
                if (m.done) continue;
                for (UUID u : new ArrayList<>(m.alive)) {
                    Player p = Bukkit.getPlayer(u);
                    if (p == null) continue;
                    if (!p.getWorld().equals(world)) { m.alive.remove(u); checkAlive(m); continue; }
                    if (p.getLocation().getY() < voidY) eliminate(p, null, false);
                }
            }
        } else {
            for (Player p : players()) {
                if (!p.getWorld().equals(world) || p.getGameMode() == GameMode.SPECTATOR) continue;
                double y = p.getLocation().getY();
                if (y < voidY || (y < HOLD_Y - 10 && p.getLocation().getX() < HOLD_X + 40 && p.getLocation().getX() > HOLD_X - 40)) rescue(p);
            }
        }
        updateActionBars();
    }

    private void rescue(Player p) {
        Match m = matchOf.get(p.getUniqueId());
        if (m != null && phase == Phase.KIT) {
            Side s = m.sideOf(p.getUniqueId());
            double[] sp = theme != null ? theme.spawn(geo, m.isB(s), 0) : geo.spawn(m.isB(s), 0);
            p.teleport(local(m.arena, sp[0], sp[1], sp[2], (float) sp[3], 0f));
        } else if (m != null && (phase == Phase.FIGHT || phase == Phase.END)) {
            p.teleport(overview(m.arena, m.isB(m.sideOf(p.getUniqueId())), 0));
        } else {
            p.teleport(holdSpawn());
        }
        p.setFallDistance(0);
    }

    private void updateHud() {
        if (ctx == null) return;
        List<String> lines = new ArrayList<>();
        switch (phase) {
            case INTRO, BUILDING -> {
                lines.add(ChatColor.WHITE + "Sides: " + ChatColor.YELLOW + sides.size() + ChatColor.WHITE + "  Rounds: " + ChatColor.YELLOW + totalRounds);
                lines.add(ChatColor.GRAY + (phase == Phase.INTRO ? "Starting soon..." : "Building arenas..."));
                if (theme != null) lines.add(ChatColor.WHITE + "Next: " + ChatColor.AQUA + theme.displayName());
            }
            default -> {
                lines.add(ChatColor.WHITE + "Round " + ChatColor.YELLOW + (round + 1) + ChatColor.GRAY + "/" + ChatColor.YELLOW + totalRounds
                        + ChatColor.DARK_GRAY + " | " + ChatColor.AQUA + (theme == null ? "" : theme.displayName()));
                lines.add(switch (phase) {
                    case KIT -> ChatColor.AQUA + "Kit select";
                    case FIGHT -> ChatColor.RED + "" + ChatColor.BOLD + "Fight!" + (fightSeconds > 0 ? ""
                            : ChatColor.GRAY + " " + (fightElapsed / 60) + ":" + String.format("%02d", fightElapsed % 60));
                    default -> ChatColor.GRAY + "Round over";
                });
                int shown = matches.size() <= 3 ? matches.size() : 2;
                for (int i = 0; i < shown; i++) lines.add(matchLine(matches.get(i)));
                if (matches.size() > shown) lines.add(ChatColor.GRAY + "+" + (matches.size() - shown) + " more matches");
                Map.Entry<UUID, Integer> best = null;
                for (Map.Entry<UUID, Integer> e : kills.entrySet()) if (best == null || e.getValue() > best.getValue()) best = e;
                if (best != null) lines.add(ChatColor.WHITE + "Top: " + ChatColor.YELLOW + names.getOrDefault(best.getKey(), "?")
                        + ChatColor.GRAY + " (" + best.getValue() + ")");
            }
        }
        ctx.setSidebarLines(lines);
    }

    private void updateActionBars() {
        if (ctx == null || world == null) return;
        Map<Match, int[]> centre = new HashMap<>();
        if (phase == Phase.FIGHT || phase == Phase.END) for (Match m : matches) centre.put(m, centreCounts(m));
        Set<UUID> online = onlineIds();
        for (Player p : players()) {
            UUID u = p.getUniqueId();
            if (!roster.contains(u)) continue;
            Match m = matchOf.get(u);
            String bar;
            if (phase == Phase.INTRO || phase == Phase.BUILDING) bar = ChatColor.YELLOW + "Battle Box - get ready!";
            else if (m == null) bar = ChatColor.GRAY + "Bye round - spectating";
            else {
                Side s = m.sideOf(u);
                Side opp = m.opponent(s);
                if (phase == Phase.KIT) {
                    StringBuilder sb = new StringBuilder();
                    Kit mine = kitChoice.get(u);
                    sb.append(ChatColor.WHITE).append("You: ").append(mine == null ? ChatColor.GRAY + "pick a kit!" : mine.colored());
                    for (UUID o : s.members) {
                        if (o.equals(u) || !online.contains(o)) continue;
                        Kit k = kitChoice.get(o);
                        sb.append(ChatColor.DARK_GRAY).append(" | ").append(ChatColor.GRAY).append(names.getOrDefault(o, "?")).append(": ")
                                .append(k == null ? ChatColor.DARK_GRAY + "-" : k.colored());
                    }
                    bar = sb.toString();
                } else {
                    int[] c = centre.getOrDefault(m, new int[]{0, 0});
                    int mine = m.isB(s) ? c[1] : c[0];
                    int theirs = m.isB(s) ? c[0] : c[1];
                    ChatColor my = Side.chatFor(m.dyeOf(s)), th = Side.chatFor(m.dyeOf(opp));
                    StringBuilder sq = new StringBuilder();
                    sq.append(my).append("■".repeat(mine)).append(ChatColor.DARK_GRAY).append("□".repeat(Math.max(0, 9 - mine - theirs)))
                            .append(th).append("■".repeat(theirs));
                    bar = ChatColor.WHITE + "vs " + opp.label + ChatColor.DARK_GRAY + " | " + ChatColor.WHITE + "⚔ " + ChatColor.YELLOW
                            + kills.getOrDefault(u, 0) + ChatColor.DARK_GRAY + " | " + my + mine + " " + sq + " " + th + theirs;
                }
            }
            McmFx.actionBar(p, bar);
        }
    }

    private String matchLine(Match m) {
        String a = shortLabel(m.a), b = shortLabel(m.b);
        int[] c = (phase == Phase.FIGHT || phase == Phase.END) ? centreCounts(m) : new int[]{0, 0};
        String tail = "";
        if (m.done) tail = m.winner == null ? ChatColor.YELLOW + " =" : (m.winner == m.a ? ChatColor.GREEN + " ◀" : ChatColor.GREEN + " ▶");
        return a + " " + ChatColor.WHITE + c[0] + ChatColor.GRAY + "-" + ChatColor.WHITE + c[1] + " " + b + tail;
    }

    private static String shortLabel(Side s) {
        String plain = ChatColor.stripColor(s.label);
        if (plain.endsWith(" Team")) plain = plain.substring(0, plain.length() - 5);
        if (plain.length() > 9) plain = plain.substring(0, 9);
        return s.chat + plain;
    }

    // =====================================================================
    // helpers
    // =====================================================================

    private List<Player> players() {
        if (ctx == null) return List.of();
        return new ArrayList<>(ctx.participants());
    }

    private Set<UUID> onlineIds() {
        Set<UUID> s = new HashSet<>();
        for (Player p : players()) if (roster.contains(p.getUniqueId())) s.add(p.getUniqueId());
        return s;
    }

    private List<Player> inMatches() {
        List<Player> out = new ArrayList<>();
        for (Player p : players()) if (matchOf.containsKey(p.getUniqueId())) out.add(p);
        return out;
    }

    private void removeEntities() {
        if (world == null) return;
        for (Entity e : new ArrayList<>(world.getEntities())) if (!(e instanceof Player)) e.remove();
    }

    static void prep(Player p, GameMode mode) {
        p.getInventory().clear();
        p.setItemOnCursor(null);
        p.closeInventory();
        p.setGameMode(mode);
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
        AttributeInstance max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(max != null ? max.getValue() : 20.0);
        p.setFoodLevel(20);
        p.setSaturation(5f);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setAbsorptionAmount(0);
        p.setArrowsInBody(0);
    }

    private static void cleanPlayer(Player p) {
        p.getInventory().clear();
        p.setItemOnCursor(null);
        p.closeInventory();
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
        p.setFireTicks(0);
        if (p.getGameMode() == GameMode.SPECTATOR) p.setGameMode(GameMode.SURVIVAL);
    }
}
