package com.mcm.games.parkourtag;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Difficulty;
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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

public final class ParkourTagGame implements McmGame, Listener {
    static final String WORLD_NAME = "mcc_parkourtag";
    static final int BASE_Y = 64;
    static final int SPACING = 120;
    static final int WAIT_X = -80, WAIT_Y = 100, WAIT_Z = 20;

    private static final int MIN_ROUNDS = 4;
    private static final int MAX_ROUNDS = 9;
    private static final int ROUND_SECONDS = 60;
    private static final int PREP_SECONDS = 5;
    private static final int HUNTER_DELAY_SECONDS = 3;
    private static final int INTRO_SECONDS = 8;
    private static final int INTERMISSION_SECONDS = 5;
    private static final int BUILD_TIMEOUT_SECONDS = 30;

    // scoring
    private static final int PTS_SURVIVE_TICK = 1;      // per 10 s alive
    private static final int PTS_SURVIVE_FULL = 4;      // alive at the end
    private static final int PTS_SIDE_SURVIVE = 12;     // shared by a side's runners if any survive
    private static final int PTS_TAG_BASE = 6;          // -1 per 10 s, min 1
    private static final int PTS_TAG_ALL_BASE = 20;     // -3 per 10 s, min 2
    private static final int PTS_FIRST_CLEAR = 24;      // shared by the side whose hunter clears first

    private static final int VISION_COOLDOWN_TICKS = 200;
    private static final int VISION_DURATION_TICKS = 60;

    private enum Phase { IDLE, BUILDING, INTRO, PREP, LIVE, INTERMISSION }
    private enum Role { HUNTER, RUNNER, OUT, BYE }

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    /** A group of players that plays together: a tournament team, a group of solos, or half of a lone team. */
    private static final class Side {
        final String key;
        final String label;
        final ChatColor color;
        final List<UUID> members = new ArrayList<>();
        Side(String key, String label, ChatColor color) { this.key = key; this.label = label; this.color = color; }
    }

    /** One arena for one round: side 0 versus side 1 (side 1 may be null in solo practice). */
    private static final class Match {
        final int arena;
        final Side[] side = new Side[2];
        final UUID[] hunter = new UUID[2];
        final List<Set<UUID>> runners = List.of(new LinkedHashSet<>(), new LinkedHashSet<>());
        final List<Set<UUID>> alive = List.of(new LinkedHashSet<>(), new LinkedHashSet<>());
        final boolean[] hunted = new boolean[2];
        final int[] clearedAt = {-1, -1};
        final int[] tags = new int[2];
        boolean hadHunter;
        boolean firstClearGiven;
        Match(int arena, Side a, Side b) { this.arena = arena; side[0] = a; side[1] = b; }

        int sideIndexOf(UUID id) {
            if (side[0] != null && side[0].members.contains(id)) return 0;
            if (side[1] != null && side[1].members.contains(id)) return 1;
            return -1;
        }

        boolean done() {
            if (!hadHunter) return false;
            for (int i = 0; i < 2; i++) {
                if (hunted[i] && !alive.get(i).isEmpty() && hunter[1 - i] != null) return false;
            }
            return true;
        }
    }

    private record Round(List<Side[]> pairs, List<Side> byes) {}

    private final Plugin plugin;
    private final ArenaBuilder builder;
    private final Random random = new Random();

    private McmGameContext ctx;
    private World world;
    private BukkitTask task;
    private boolean running;
    private boolean finished;
    private SpectatorGuard guard;
    private Phase phase = Phase.IDLE;
    private int phaseTicks;
    private long tickCount;
    private int buildWaitSeconds;

    private List<Side> sides = new ArrayList<>();
    private List<Round> schedule = new ArrayList<>();
    private int roundIndex;
    private int arenaCount;
    private int liveElapsed;
    private long hunterReleaseTick;
    private final List<Match> matches = new ArrayList<>();
    private final List<Side> byeSides = new ArrayList<>();
    private final Map<UUID, Side> sideOf = new HashMap<>();
    private final Map<UUID, Role> roles = new HashMap<>();
    private final Map<UUID, Match> matchOf = new HashMap<>();
    private final Set<UUID> frozen = new HashSet<>();
    private final Map<UUID, Long> visionUntil = new HashMap<>();
    private final Map<UUID, Integer> hunts = new HashMap<>();
    private final Map<UUID, Integer> runs = new HashMap<>();
    private final Map<UUID, Integer> totalTags = new HashMap<>();
    private final Map<UUID, Integer> survivals = new HashMap<>();
    private final Set<UUID> everyone = new HashSet<>();

    public ParkourTagGame(Plugin plugin) {
        this.plugin = plugin;
        this.builder = new ArenaBuilder(plugin);
    }

    @Override public String id() { return "parkourtag"; }
    @Override public String displayName() { return "Parkour Tag"; }
    @Override public Material icon() { return Material.LADDER; }
    @Override public String description() { return "Side vs side tag in parkour arenas: one hunter, the rest run."; }
    @Override public boolean useCoreScoreboard() { return true; }

    // ------------------------------------------------------------------ lifecycle

    @Override
    public void start(McmGameContext ctx) {
        if (running) cleanup();
        if (guard == null) guard = new SpectatorGuard(plugin, this::spectatorZone);
        this.ctx = ctx;
        this.running = true;
        this.finished = false;
        resetState();

        world = ensureWorld();
        if (world == null) {
            running = false;
            ctx.broadcast("§cParkour Tag could not create its world.");
            finished = true;
            ctx.finish("World could not be created");
            return;
        }

        sides = buildSides(ctx);
        for (Side s : sides) for (UUID u : s.members) { sideOf.put(u, s); everyone.add(u); }
        schedule = buildSchedule(sides);
        arenaCount = 1;
        for (Round r : schedule) arenaCount = Math.max(arenaCount, r.pairs().size());

        buildWaitingPlatform();
        Location wait = waitSpot();
        for (Player p : new ArrayList<>(ctx.participants())) {
            resetPlayer(p, GameMode.ADVENTURE);
            p.teleport(wait);
        }

        for (int i = 0; i < arenaCount; i++) {
            int ox = i * SPACING;
            builder.clear(world, ox, BASE_Y, 0, ox + ArenaBlueprint.W - 1, BASE_Y + ArenaBlueprint.H + 1, ArenaBlueprint.W - 1);
            builder.build(world, ox, BASE_Y, 0);
        }
        phase = Phase.BUILDING;
        buildWaitSeconds = 0;
        builder.whenIdle(() -> {
            if (running && phase == Phase.BUILDING) enterIntro();
        });

        StringBuilder sb = new StringBuilder("§6§lParkour Tag §7- ");
        sb.append(sides.size()).append(" side(s), ").append(schedule.size()).append(" round(s).");
        ctx.broadcast(sb.toString());
        ctx.broadcast("§7Each round your side's §cHunter§7 chases the other side's runners while their hunter chases you. Hit a runner to tag them!");
        updateSidebar();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    @Override
    public void stop() {
        cleanup();
    }

    private void resetState() {
        phase = Phase.IDLE;
        phaseTicks = 0;
        tickCount = 0;
        roundIndex = 0;
        liveElapsed = 0;
        matches.clear();
        byeSides.clear();
        sideOf.clear();
        roles.clear();
        matchOf.clear();
        frozen.clear();
        visionUntil.clear();
        hunts.clear();
        runs.clear();
        totalTags.clear();
        survivals.clear();
        everyone.clear();
    }

    /** Cancels everything and restores the arenas. Safe to call any number of times. */
    private void cleanup() {
        boolean wasRunning = running;
        running = false;
        if (task != null) { task.cancel(); task = null; }
        builder.cancelCallbacks();
        if (!wasRunning) return;
        for (UUID u : new ArrayList<>(everyone)) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            try {
                clearPlayer(p);
                if (p.getGameMode() == GameMode.SPECTATOR || p.getGameMode() == GameMode.ADVENTURE) p.setGameMode(GameMode.SURVIVAL);
            } catch (Throwable ignored) { }
        }
        if (world != null && Bukkit.getWorld(world.getUID()) != null && plugin.isEnabled()) {
            for (int i = 0; i < arenaCount; i++) {
                int ox = i * SPACING;
                builder.clear(world, ox, BASE_Y, 0, ox + ArenaBlueprint.W - 1, BASE_Y + ArenaBlueprint.H + 1, ArenaBlueprint.W - 1);
            }
        }
        phase = Phase.IDLE;
        matches.clear();
        roles.clear();
        matchOf.clear();
        frozen.clear();
        visionUntil.clear();
    }

    private void endGame(String reason) {
        if (finished) return;
        finished = true;
        McmGameContext c = ctx;
        announceAwards(c);
        cleanup();
        if (c != null) c.finish(reason);
    }

    private void announceAwards(McmGameContext c) {
        if (c == null) return;
        UUID bestTagger = null; int bestTags = 0;
        for (Map.Entry<UUID, Integer> e : totalTags.entrySet()) if (e.getValue() > bestTags) { bestTags = e.getValue(); bestTagger = e.getKey(); }
        UUID bestRunner = null; int bestSurv = 0;
        for (Map.Entry<UUID, Integer> e : survivals.entrySet()) if (e.getValue() > bestSurv) { bestSurv = e.getValue(); bestRunner = e.getKey(); }
        if (bestTagger != null) c.broadcast("§6Top hunter: §f" + nameOf(bestTagger) + " §7(" + bestTags + " tags)");
        if (bestRunner != null) c.broadcast("§6Top runner: §f" + nameOf(bestRunner) + " §7(survived " + bestSurv + " rounds)");
    }

    // ------------------------------------------------------------------ world

    private World ensureWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            try {
                w = new WorldCreator(WORLD_NAME).generator(new VoidGenerator()).generateStructures(false).createWorld();
            } catch (Throwable t) {
                plugin.getLogger().severe("Could not create world " + WORLD_NAME + ": " + t);
                return null;
            }
        }
        if (w == null) return null;
        w.setSpawnLocation(WAIT_X, WAIT_Y + 1, WAIT_Z);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.DO_TILE_DROPS, false);
        w.setTime(6000);
        w.setStorm(false);
        w.setThundering(false);
        w.setPVP(true);
        w.setDifficulty(Difficulty.PEACEFUL);
        return w;
    }

    private void buildWaitingPlatform() {
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                boolean edge = Math.abs(x) == 6 || Math.abs(z) == 6;
                world.getBlockAt(WAIT_X + x, WAIT_Y, WAIT_Z + z).setType(edge ? Material.CYAN_STAINED_GLASS : ((x + z) % 2 == 0 ? Material.WHITE_STAINED_GLASS : Material.LIGHT_BLUE_STAINED_GLASS), false);
                if (edge) for (int y = 1; y <= 3; y++) world.getBlockAt(WAIT_X + x, WAIT_Y + y, WAIT_Z + z).setType(Material.BARRIER, false);
            }
        }
    }

    private Location waitSpot() { return new Location(world, WAIT_X + 0.5, WAIT_Y + 1, WAIT_Z + 0.5); }

    private Location arenaOrigin(int arena) { return new Location(world, arena * SPACING, BASE_Y, 0); }

    private Location spectatorSpot(int arena) {
        Location l = arenaOrigin(arena).add(20.5, ArenaBlueprint.H + 5, 20.5);
        l.setPitch(90f);
        return l;
    }

    /** Spectator box of one arena copy (the build plus a margin and room above). */
    private static org.bukkit.util.BoundingBox arenaBox(int arena) {
        double x0 = arena * SPACING;
        return new org.bukkit.util.BoundingBox(x0 - 15, BASE_Y - 10, -15,
                x0 + ArenaBlueprint.W + 15, BASE_Y + ArenaBlueprint.H + 25, ArenaBlueprint.W + 15);
    }

    /** Tagged runners watch their own match's arena; bye sides and late joiners may watch any arena in use. */
    private SpectatorGuard.Zone spectatorZone(Player p) {
        if (!running || world == null || !p.getWorld().equals(world)) return null;
        Match m = matchOf.get(p.getUniqueId());
        if (m != null) return new SpectatorGuard.Zone(world, arenaBox(m.arena), spectatorSpot(m.arena));
        int last = 0;
        for (Match x : matches) last = Math.max(last, x.arena);
        return new SpectatorGuard.Zone(world, arenaBox(0).union(arenaBox(last)), spectatorSpot(0));
    }

    /** Late joiners and rejoiners (the core no longer counts them) spectate above arena 0. */
    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        if (!running || world == null) return false;
        SpectatorGuard.makeSpectator(player, spectatorSpot(0));
        player.sendMessage("§6Parkour Tag §7is in progress - you are spectating.");
        return true;
    }

    private Location padSpot(Match m, int sideIdx, int slot) {
        int x = 18 + (slot % 5);
        int z = sideIdx == 0 ? 2 : ArenaBlueprint.W - 3;
        Location l = arenaOrigin(m.arena).add(x + 0.5, 1, z + 0.5);
        l.setYaw(sideIdx == 0 ? 0f : 180f);
        return l;
    }

    private void paintPads(Match m) {
        Location o = arenaOrigin(m.arena);
        Material a = concreteOf(m.side[0] == null ? null : m.side[0].color);
        Material b = concreteOf(m.side[1] == null ? null : m.side[1].color);
        for (int x = ArenaBlueprint.PAD_X0; x <= ArenaBlueprint.PAD_X1; x++) {
            for (int z = ArenaBlueprint.PAD_A_Z0; z <= ArenaBlueprint.PAD_A_Z1; z++)
                world.getBlockAt(o.getBlockX() + x, BASE_Y, o.getBlockZ() + z).setType(a, false);
            for (int z = ArenaBlueprint.PAD_B_Z0; z <= ArenaBlueprint.PAD_B_Z1; z++)
                world.getBlockAt(o.getBlockX() + x, BASE_Y, o.getBlockZ() + z).setType(b, false);
        }
    }

    // ------------------------------------------------------------------ sides & schedule

    private static final ChatColor[] PALETTE = {
            ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.DARK_GREEN,
            ChatColor.DARK_AQUA, ChatColor.AQUA, ChatColor.BLUE, ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE,
            ChatColor.WHITE, ChatColor.GRAY, ChatColor.DARK_RED, ChatColor.DARK_BLUE, ChatColor.DARK_GRAY, ChatColor.BLACK
    };

    private List<Side> buildSides(McmGameContext ctx) {
        List<Side> out = new ArrayList<>();
        List<Player> solos = new ArrayList<>();
        Set<ChatColor> used = new HashSet<>();
        int teamPlayers = 0, teamCount = 0;
        Map<String, List<Player>> raw = ctx.sides();
        for (Map.Entry<String, List<Player>> e : raw.entrySet()) {
            List<Player> ps = new ArrayList<>();
            for (Player p : e.getValue()) if (p != null && p.isOnline()) ps.add(p);
            if (ps.isEmpty()) continue;
            String key = e.getKey();
            if (key.startsWith("TEAM:")) {
                String team = key.substring(5);
                ChatColor c = ctx.teamColor(team);
                if (c == null) c = ChatColor.WHITE;
                used.add(c);
                Side s = new Side(key, c + team, c);
                for (Player p : ps) s.members.add(p.getUniqueId());
                out.add(s);
                teamPlayers += ps.size();
                teamCount++;
            } else {
                solos.addAll(ps);
            }
        }
        if (!solos.isEmpty()) {
            Collections.shuffle(solos, random);
            int n = solos.size();
            int size;
            if (teamCount > 0) size = Math.max(1, Math.round((float) teamPlayers / teamCount));
            else size = n <= 3 ? 1 : Math.min(4, n / 2);
            int k = 0;
            for (int i = 0; i < n; i += size) {
                List<Player> chunk = solos.subList(i, Math.min(n, i + size));
                ChatColor c = nextColor(used);
                String label = chunk.size() == 1 ? c + chunk.get(0).getName() : c + "Squad " + (char) ('A' + (k % 26));
                Side s = new Side("SQUAD:" + k, label, c);
                for (Player p : chunk) s.members.add(p.getUniqueId());
                out.add(s);
                k++;
            }
        }
        // Everyone on one side: split it in two so there is someone to play against.
        if (out.size() == 1 && out.get(0).members.size() >= 2) {
            Side only = out.remove(0);
            List<UUID> m = new ArrayList<>(only.members);
            Collections.shuffle(m, random);
            ChatColor c2 = nextColor(used);
            Side a = new Side(only.key + "#A", only.color + stripColor(only.label) + " A", only.color);
            Side b = new Side(only.key + "#B", c2 + stripColor(only.label) + " B", c2);
            for (int i = 0; i < m.size(); i++) (i % 2 == 0 ? a : b).members.add(m.get(i));
            out.add(a);
            out.add(b);
        }
        Collections.shuffle(out, random);
        return out;
    }

    private static ChatColor nextColor(Set<ChatColor> used) {
        for (ChatColor c : PALETTE) if (!used.contains(c)) { used.add(c); return c; }
        return ChatColor.WHITE;
    }

    private static String stripColor(String s) {
        String r = ChatColor.stripColor(s);
        return r == null ? "" : r;
    }

    /** Round-robin (circle method); an odd side out gets a bye. Repeats until MIN_ROUNDS, capped at MAX_ROUNDS. */
    private List<Round> buildSchedule(List<Side> sides) {
        List<Round> out = new ArrayList<>();
        if (sides.isEmpty()) return out;
        if (sides.size() == 1) {
            // Lone tester: practice runs with no hunter.
            for (int r = 0; r < 2; r++) {
                List<Side[]> pairs = new ArrayList<>();
                pairs.add(new Side[]{sides.get(0), null});
                out.add(new Round(pairs, new ArrayList<>()));
            }
            return out;
        }
        List<Side> list = new ArrayList<>(sides);
        if (list.size() % 2 == 1) list.add(null);
        int n = list.size();
        List<Round> cycle = new ArrayList<>();
        for (int r = 0; r < n - 1; r++) {
            List<Side[]> pairs = new ArrayList<>();
            List<Side> byes = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                Side a = list.get(i), b = list.get(n - 1 - i);
                if (a == null) byes.add(b);
                else if (b == null) byes.add(a);
                else pairs.add(r % 2 == 0 ? new Side[]{a, b} : new Side[]{b, a});
            }
            cycle.add(new Round(pairs, byes));
            list.add(1, list.remove(n - 1));
        }
        while (out.size() < MIN_ROUNDS) out.addAll(cycle);
        while (out.size() > MAX_ROUNDS) out.remove(out.size() - 1);
        return out;
    }

    // ------------------------------------------------------------------ main loop

    private void tick() {
        if (!running) return;
        tickCount++;
        try {
            if (phase == Phase.LIVE) {
                if (tickCount == hunterReleaseTick) releaseHunters();
                if (tickCount % 2 == 0) tickVision();
            }
            if (tickCount % 20 == 0) second();
        } catch (Throwable t) {
            plugin.getLogger().warning("Parkour Tag tick error: " + t);
            t.printStackTrace();
        }
    }

    private void second() {
        pruneOffline();
        if (!running) return;
        if (ctx.participants().isEmpty()) { endGame("Everyone left"); return; }

        switch (phase) {
            case BUILDING -> {
                buildWaitSeconds++;
                ctx.setTimer(0, 0);
                for (Player p : online()) actionBar(p, "§7Building arenas...");
                if (buildWaitSeconds >= BUILD_TIMEOUT_SECONDS) enterIntro();
            }
            case INTRO -> {
                phaseTicks -= 20;
                int left = Math.max(0, phaseTicks / 20);
                ctx.setTimer(left, INTRO_SECONDS);
                if (left == INTRO_SECONDS - 1) {
                    for (Player p : online()) title(p, "§6§lParkour Tag", "§7Tag the enemy runners. Don't get tagged!");
                }
                if (left == 3) {
                    for (Player p : online()) title(p, "§e" + schedule.size() + " Rounds", "§7No fall damage - climb everything");
                }
                if (left <= 0) startRound(0);
            }
            case PREP -> {
                phaseTicks -= 20;
                int left = Math.max(0, phaseTicks / 20);
                ctx.setTimer(left, PREP_SECONDS);
                for (Player p : online()) {
                    Role r = roles.get(p.getUniqueId());
                    if (r == Role.HUNTER || r == Role.RUNNER) {
                        if (left > 0) {
                            p.showTitle(Title.title(text("§e" + left), text(r == Role.HUNTER ? "§cYou are the HUNTER" : "§aYou are a RUNNER"),
                                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ZERO)));
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                        }
                    }
                }
                if (left <= 0) goLive();
            }
            case LIVE -> {
                liveElapsed++;
                ctx.setTimer(Math.max(0, ROUND_SECONDS - liveElapsed), ROUND_SECONDS);
                if (liveElapsed % 10 == 0 && liveElapsed < ROUND_SECONDS) awardSurvivalTick();
                if (liveElapsed >= ROUND_SECONDS) { endRound(true); return; }
                if (allMatchesDone()) { endRound(false); return; }
            }
            case INTERMISSION -> {
                phaseTicks -= 20;
                int left = Math.max(0, phaseTicks / 20);
                ctx.setTimer(left, INTERMISSION_SECONDS);
                if (left <= 0) {
                    if (roundIndex + 1 >= schedule.size()) { endGame("All rounds played"); return; }
                    startRound(roundIndex + 1);
                }
            }
            default -> { }
        }
        if (running) {
            updateSidebar();
            updateActionBars();
        }
    }

    private void enterIntro() {
        phase = Phase.INTRO;
        phaseTicks = FastTest.secs(INTRO_SECONDS) * 20;
    }

    private void startRound(int r) {
        roundIndex = r;
        matches.clear();
        byeSides.clear();
        roles.clear();
        matchOf.clear();
        frozen.clear();
        visionUntil.clear();
        liveElapsed = 0;

        Round round = schedule.get(r);
        int arena = 0;
        for (Side[] pair : round.pairs()) {
            Side a = pair[0], b = pair[1];
            List<UUID> ma = onlineMembers(a), mb = onlineMembers(b);
            if (b == null) {
                if (!ma.isEmpty()) matches.add(setupMatch(new Match(arena++, a, null), ma, List.of()));
                continue;
            }
            if (ma.isEmpty() || mb.isEmpty()) {
                if (!ma.isEmpty()) byeSides.add(a);
                if (!mb.isEmpty()) byeSides.add(b);
                continue;
            }
            matches.add(setupMatch(new Match(arena++, a, b), ma, mb));
        }
        for (Side s : round.byes()) if (!onlineMembers(s).isEmpty()) byeSides.add(s);

        // Bye sides watch from above arena 0.
        for (Side s : byeSides) {
            for (UUID u : onlineMembers(s)) {
                Player p = Bukkit.getPlayer(u);
                if (p == null) continue;
                roles.put(u, Role.BYE);
                clearPlayer(p);
                p.setGameMode(GameMode.SPECTATOR);
                p.teleport(spectatorSpot(0));
                title(p, "§7Round " + (r + 1), "§7Your side has a bye this round - spectate!");
            }
        }
        // Anyone not placed anywhere (e.g. side vanished) spectates too.
        for (Player p : online()) {
            if (!roles.containsKey(p.getUniqueId())) {
                roles.put(p.getUniqueId(), Role.BYE);
                clearPlayer(p);
                p.setGameMode(GameMode.SPECTATOR);
                p.teleport(spectatorSpot(0));
            }
        }

        phase = Phase.PREP;
        phaseTicks = FastTest.secs(PREP_SECONDS) * 20 + 20;
        ctx.broadcast("§6Round " + (r + 1) + "/" + schedule.size() + ": " + matchupSummary());
        if (matches.isEmpty()) {
            // Nobody can play this round; skip quickly.
            phase = Phase.INTERMISSION;
            phaseTicks = 40;
        }
    }

    private String matchupSummary() {
        StringBuilder sb = new StringBuilder();
        for (Match m : matches) {
            if (sb.length() > 0) sb.append("§7, ");
            sb.append(m.side[0].label);
            if (m.side[1] != null) sb.append(" §7vs ").append(m.side[1].label);
            else sb.append(" §7(practice)");
        }
        for (Side s : byeSides) sb.append("§7, ").append(s.label).append(" §7(bye)");
        return sb.toString();
    }

    private Match setupMatch(Match m, List<UUID> ma, List<UUID> mb) {
        List<List<UUID>> ms = List.of(ma, mb);
        if (m.side[1] != null) {
            if (ma.size() >= 2) m.hunter[0] = pickHunter(ma);
            if (mb.size() >= 2) m.hunter[1] = pickHunter(mb);
            if (ma.size() == 1 && mb.size() == 1) {
                UUID x = ma.get(0), y = mb.get(0);
                int hx = hunts.getOrDefault(x, 0), hy = hunts.getOrDefault(y, 0);
                if (hx < hy || (hx == hy && random.nextBoolean())) m.hunter[0] = x; else m.hunter[1] = y;
            } else {
                for (int i = 0; i < 2; i++) {
                    List<UUID> mine = ms.get(i);
                    if (mine.size() == 1) {
                        UUID lone = mine.get(0);
                        if (hunts.getOrDefault(lone, 0) < runs.getOrDefault(lone, 0)) m.hunter[i] = lone;
                    }
                }
            }
            // A hunter with nobody to chase becomes a runner.
            for (int i = 0; i < 2; i++) {
                List<UUID> opp = ms.get(1 - i);
                UUID oppHunter = m.hunter[1 - i];
                int oppRunners = opp.size() - (oppHunter != null ? 1 : 0);
                if (m.hunter[i] != null && oppRunners <= 0) m.hunter[i] = null;
            }
        }
        for (int i = 0; i < 2; i++) {
            for (UUID u : ms.get(i)) {
                if (u.equals(m.hunter[i])) continue;
                m.runners.get(i).add(u);
                m.alive.get(i).add(u);
            }
        }
        for (int i = 0; i < 2; i++) m.hunted[i] = m.hunter[1 - i] != null && !m.runners.get(i).isEmpty();
        m.hadHunter = m.hunter[0] != null || m.hunter[1] != null;

        paintPads(m);
        for (int i = 0; i < 2; i++) {
            List<UUID> list = ms.get(i);
            for (int slot = 0; slot < list.size(); slot++) {
                UUID u = list.get(slot);
                Player p = Bukkit.getPlayer(u);
                if (p == null) continue;
                boolean isHunter = u.equals(m.hunter[i]);
                matchOf.put(u, m);
                roles.put(u, isHunter ? Role.HUNTER : Role.RUNNER);
                if (isHunter) hunts.merge(u, 1, Integer::sum); else runs.merge(u, 1, Integer::sum);
                resetPlayer(p, GameMode.ADVENTURE);
                p.teleport(padSpot(m, i, slot));
                frozen.add(u);
                equip(p, m.side[i], isHunter, m.hunted[i]);
                if (isHunter) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (PREP_SECONDS + HUNTER_DELAY_SECONDS + 1) * 20, 0, false, false, false));
                }
                String opp = m.side[1 - i] == null ? "§7nobody (practice)" : m.side[1 - i].label;
                p.sendMessage("§6Round " + (roundIndex + 1) + " §7- you are " + (isHunter ? "the §c§lHUNTER" : "a §a§lRUNNER") + " §7vs " + opp);
                if (!isHunter && !m.hunted[i]) p.sendMessage("§7No enemy hunter is after you this round - free run.");
            }
        }
        return m;
    }

    private UUID pickHunter(List<UUID> members) {
        int best = Integer.MAX_VALUE;
        List<UUID> cands = new ArrayList<>();
        for (UUID u : members) {
            int h = hunts.getOrDefault(u, 0);
            if (h < best) { best = h; cands.clear(); cands.add(u); }
            else if (h == best) cands.add(u);
        }
        return cands.get(random.nextInt(cands.size()));
    }

    private void goLive() {
        phase = Phase.LIVE;
        liveElapsed = 0;
        hunterReleaseTick = tickCount + HUNTER_DELAY_SECONDS * 20L;
        for (Match m : matches) {
            for (int i = 0; i < 2; i++) {
                for (UUID u : m.runners.get(i)) {
                    frozen.remove(u);
                    Player p = Bukkit.getPlayer(u);
                    if (p == null) continue;
                    p.showTitle(Title.title(text("§a§lRUN!"), text(m.hunted[i] ? "§7The hunter is released in " + HUNTER_DELAY_SECONDS + "s" : "§7Free run"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(300))));
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
                }
                UUID h = m.hunter[i];
                Player hp = h == null ? null : Bukkit.getPlayer(h);
                if (hp != null) title(hp, "§cGet ready...", "§7Released in " + HUNTER_DELAY_SECONDS + "s");
            }
        }
        ctx.setTimer(ROUND_SECONDS, ROUND_SECONDS);
    }

    private void releaseHunters() {
        for (Match m : matches) {
            for (int i = 0; i < 2; i++) {
                UUID h = m.hunter[i];
                if (h == null) continue;
                frozen.remove(h);
                Player p = Bukkit.getPlayer(h);
                if (p == null) continue;
                p.removePotionEffect(PotionEffectType.BLINDNESS);
                p.showTitle(Title.title(text("§c§lHUNT!"), text("§7Tag every " + stripColor(m.side[1 - i].label) + " runner"),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(300))));
                p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.4f);
            }
        }
    }

    private void awardSurvivalTick() {
        for (Match m : matches) {
            for (int i = 0; i < 2; i++) {
                if (!m.hunted[i]) continue;
                for (UUID u : new ArrayList<>(m.alive.get(i))) {
                    if (Bukkit.getPlayer(u) == null) continue;
                    ctx.addPoints(u, PTS_SURVIVE_TICK, "Survived " + liveElapsed + "s");
                }
            }
        }
    }

    private boolean allMatchesDone() {
        if (matches.isEmpty()) return true;
        for (Match m : matches) if (!m.done()) return false;
        return true;
    }

    private void endRound(boolean timeout) {
        phase = Phase.INTERMISSION;
        phaseTicks = FastTest.secs(INTERMISSION_SECONDS) * 20;
        frozen.clear();
        visionUntil.clear();
        for (Match m : matches) {
            List<String> summary = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                if (!m.hunted[i]) continue;
                List<UUID> survivors = new ArrayList<>();
                for (UUID u : m.alive.get(i)) if (Bukkit.getPlayer(u) != null) survivors.add(u);
                for (UUID u : survivors) {
                    if (timeout) ctx.addPoints(u, PTS_SURVIVE_TICK, "Survived " + ROUND_SECONDS + "s");
                    ctx.addPoints(u, PTS_SURVIVE_FULL, "Survived the round");
                    survivals.merge(u, 1, Integer::sum);
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) { title(p, "§a§lSURVIVED!", "§7+" + PTS_SURVIVE_FULL + " points"); p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f); }
                }
                if (!survivors.isEmpty()) {
                    List<UUID> share = new ArrayList<>();
                    for (UUID u : m.runners.get(i)) if (Bukkit.getPlayer(u) != null) share.add(u);
                    splitPoints(share, PTS_SIDE_SURVIVE, "Side survived Parkour Tag round");
                }
                summary.add(m.side[i].label + "§7: " + survivors.size() + "/" + m.runners.get(i).size() + " survived");
            }
            for (int i = 0; i < 2; i++) {
                if (m.hunter[i] != null && m.side[1 - i] != null) {
                    String t = m.clearedAt[i] >= 0 ? " §7(all in " + m.clearedAt[i] + "s)" : "";
                    summary.add(m.side[i].label + " §7hunter " + nameOf(m.hunter[i]) + ": " + m.tags[i] + " tag(s)" + t);
                }
            }
            String text = "§eArena " + (m.arena + 1) + " §8| " + String.join(" §8| ", summary);
            for (int i = 0; i < 2; i++) {
                if (m.side[i] == null) continue;
                for (UUID u : m.side[i].members) { Player p = Bukkit.getPlayer(u); if (p != null) p.sendMessage(text); }
            }
        }
        for (Player p : online()) {
            p.removePotionEffect(PotionEffectType.BLINDNESS);
            if (!timeout && roles.get(p.getUniqueId()) != Role.BYE) actionBar(p, "§7Round over!");
        }
        ctx.setTimer(INTERMISSION_SECONDS, INTERMISSION_SECONDS);
    }

    // ------------------------------------------------------------------ admin testing hooks
    // (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    /** Ends the intro, the pre-round freeze/countdown or the intermission now. */
    public boolean skipWait() {
        if (!running || ctx == null) return false;
        if (phase != Phase.INTRO && phase != Phase.PREP && phase != Phase.INTERMISSION) return false;
        phaseTicks = 20;
        second();   // the normal "time ran out" transition of that phase
        return true;
    }

    /** Ends the current round now with no round-end points, then the next round starts (or the game ends). */
    public boolean skipRound() {
        if (!running || ctx == null) return false;
        if (phase == Phase.PREP || phase == Phase.LIVE) {
            phase = Phase.INTERMISSION;
            frozen.clear();
            visionUntil.clear();
            for (Player p : online()) p.removePotionEffect(PotionEffectType.BLINDNESS);
            ctx.broadcast("§7Round " + (roundIndex + 1) + " was skipped by an admin (no round-end points).");
        } else if (phase != Phase.INTERMISSION) {
            return false;
        }
        phaseTicks = 20;
        second();   // INTERMISSION end: next round or endGame
        return true;
    }

    // ------------------------------------------------------------------ tagging

    private void tryTag(Player hunter, Player victim) {
        if (phase != Phase.LIVE) return;
        UUID h = hunter.getUniqueId(), v = victim.getUniqueId();
        Match m = matchOf.get(h);
        if (m == null || m != matchOf.get(v)) return;
        if (roles.get(h) != Role.HUNTER || frozen.contains(h)) return;
        int hs = m.sideIndexOf(h);
        if (hs < 0 || !h.equals(m.hunter[hs])) return;
        int vs = 1 - hs;
        if (!m.alive.get(vs).remove(v)) return;

        roles.put(v, Role.OUT);
        visionUntil.remove(v);
        m.tags[hs]++;
        totalTags.merge(h, 1, Integer::sum);
        int bracket = liveElapsed / 10;
        int pts = Math.max(1, PTS_TAG_BASE - bracket);
        ctx.addPoints(h, pts, "Tagged " + victim.getName());

        Location at = victim.getLocation();
        world.spawnParticle(Particle.FIREWORK, at.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.08);
        world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
        hunter.playSound(hunter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
        actionBar(hunter, "§aTagged " + victim.getName() + " §6+" + pts);

        clearPlayer(victim);
        victim.setGameMode(GameMode.SPECTATOR);
        victim.teleport(spectatorSpot(m.arena));
        title(victim, "§c§lTAGGED!", "§7by " + hunter.getName() + " after " + liveElapsed + "s");
        victim.playSound(victim.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1f, 1f);

        int left = m.alive.get(vs).size();
        String msg = m.side[vs].color + victim.getName() + " §7was tagged by " + m.side[hs].color + hunter.getName() + " §7(" + left + " left)";
        messageMatch(m, msg);

        if (left == 0) {
            int bonus = Math.max(2, PTS_TAG_ALL_BASE - 3 * bracket);
            ctx.addPoints(h, bonus, "Tagged every runner");
            m.clearedAt[hs] = liveElapsed;
            title(hunter, "§6§lALL TAGGED!", "§7in " + liveElapsed + "s §6+" + bonus);
            hunter.playSound(hunter.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            if (!m.firstClearGiven && m.hunted[0] && m.hunted[1] && m.clearedAt[vs] < 0) {
                m.firstClearGiven = true;
                List<UUID> share = new ArrayList<>();
                for (UUID u : m.side[hs].members) if (Bukkit.getPlayer(u) != null) share.add(u);
                splitPoints(share, PTS_FIRST_CLEAR, "Hunter cleared first");
                messageMatch(m, m.side[hs].label + " §7cleared first! §6+" + PTS_FIRST_CLEAR + " §7shared by the side.");
            }
            // The cleared runners' hunter role ends; that hunter keeps going if their targets remain.
            Player hp = hunter;
            hp.sendMessage("§7All targets tagged - watch your teammates survive!");
        }
        if (allMatchesDone()) endRound(false);
    }

    // ------------------------------------------------------------------ runner vision

    private void useVision(Player p) {
        UUID u = p.getUniqueId();
        if (phase != Phase.LIVE || roles.get(u) != Role.RUNNER) return;
        Match m = matchOf.get(u);
        if (m == null) return;
        int s = m.sideIndexOf(u);
        if (s < 0 || !m.hunted[s]) { actionBar(p, "§7No hunter is after you."); return; }
        if (p.hasCooldown(Material.ENDER_EYE)) {
            actionBar(p, "§cRunner Vision recharging (" + (p.getCooldown(Material.ENDER_EYE) / 20 + 1) + "s)");
            return;
        }
        p.setCooldown(Material.ENDER_EYE, VISION_COOLDOWN_TICKS);
        visionUntil.put(u, tickCount + VISION_DURATION_TICKS);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
    }

    private void tickVision() {
        if (visionUntil.isEmpty()) return;
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(255, 40, 40), 1.3f);
        Particle.DustOptions pink = new Particle.DustOptions(Color.fromRGB(255, 120, 200), 0.9f);
        Iterator<Map.Entry<UUID, Long>> it = visionUntil.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (tickCount > e.getValue()) { it.remove(); continue; }
            Player p = Bukkit.getPlayer(e.getKey());
            Match m = matchOf.get(e.getKey());
            if (p == null || m == null) { it.remove(); continue; }
            int s = m.sideIndexOf(e.getKey());
            UUID hid = s < 0 ? null : m.hunter[1 - s];
            Player h = hid == null ? null : Bukkit.getPlayer(hid);
            if (h == null || !h.getWorld().equals(p.getWorld())) { it.remove(); continue; }
            Location hl = h.getLocation();
            // Outline column on the hunter (only this runner sees it)
            for (double y = 0; y <= 2.0; y += 0.25) {
                p.spawnParticle(Particle.DUST, hl.clone().add(0.35, y, 0.35), 1, 0, 0, 0, 0, red);
                p.spawnParticle(Particle.DUST, hl.clone().add(-0.35, y, -0.35), 1, 0, 0, 0, 0, red);
                p.spawnParticle(Particle.DUST, hl.clone().add(0.35, y, -0.35), 1, 0, 0, 0, 0, red);
                p.spawnParticle(Particle.DUST, hl.clone().add(-0.35, y, 0.35), 1, 0, 0, 0, 0, red);
            }
            // Pointer beam from the runner towards the hunter
            Location eye = p.getEyeLocation().subtract(0, 0.4, 0);
            Vector dir = hl.clone().add(0, 1, 0).toVector().subtract(eye.toVector());
            double dist = dir.length();
            if (dist > 0.1) {
                dir.normalize();
                double len = Math.min(dist - 0.5, 6);
                for (double d = 1; d < len; d += 0.5) p.spawnParticle(Particle.DUST, eye.clone().add(dir.clone().multiply(d)), 1, 0, 0, 0, 0, pink);
            }
            if (tickCount % 10 == 0) {
                double dy = hl.getY() - p.getLocation().getY();
                String vert = dy > 2 ? " §7(above)" : dy < -2 ? " §7(below)" : "";
                actionBar(p, "§dRunner Vision §7» §c" + h.getName() + " §f" + (int) Math.round(dist) + "m" + vert);
            }
        }
    }

    // ------------------------------------------------------------------ HUD

    private void updateSidebar() {
        List<String> lines = new ArrayList<>();
        if (phase == Phase.BUILDING || phase == Phase.INTRO) {
            lines.add("§fRounds: §e" + schedule.size());
            lines.add("§fSides: §e" + sides.size());
            lines.add("§fArenas: §e" + arenaCount);
            ctx.setSidebarLines(lines);
            return;
        }
        lines.add("§fRound §e" + (roundIndex + 1) + "§7/§e" + schedule.size());
        int hunted = 0;
        for (Match m : matches) for (int i = 0; i < 2; i++) if (m.hunted[i]) hunted += m.alive.get(i).size();
        lines.add("§fRunners left: §a" + hunted);
        int shown = 0;
        for (Match m : matches) {
            if (shown == 3 && matches.size() > 4) { lines.add("§7+" + (matches.size() - 3) + " more arenas"); break; }
            String a = shortLabel(m.side[0]);
            if (m.side[1] == null) lines.add(a + " §7practice");
            else lines.add(a + " §f" + m.alive.get(0).size() + "§7-§f" + m.alive.get(1).size() + " " + shortLabel(m.side[1]));
            shown++;
        }
        while (lines.size() > 6) lines.remove(lines.size() - 1);
        ctx.setSidebarLines(lines);
    }

    private static String shortLabel(Side s) {
        if (s == null) return "§7-";
        String plain = stripColor(s.label);
        if (plain.length() > 9) plain = plain.substring(0, 9);
        return s.color + plain;
    }

    private void updateActionBars() {
        if (phase != Phase.LIVE && phase != Phase.PREP) return;
        for (Player p : online()) {
            UUID u = p.getUniqueId();
            Long vis = visionUntil.get(u);
            if (vis != null && vis >= tickCount) continue;
            Role r = roles.get(u);
            Match m = matchOf.get(u);
            if (r == null) continue;
            String bar;
            switch (r) {
                case HUNTER -> {
                    int s = m == null ? -1 : m.sideIndexOf(u);
                    if (s < 0) continue;
                    int left = m.alive.get(1 - s).size();
                    bar = "§c§lHUNTER §7» §f" + left + " §7target(s) left on " + m.side[1 - s].label
                            + (frozen.contains(u) && phase == Phase.LIVE ? " §8(frozen)" : "");
                }
                case RUNNER -> {
                    int s = m == null ? -1 : m.sideIndexOf(u);
                    if (s < 0) continue;
                    if (!m.hunted[s]) bar = "§a§lRUNNER §7» free run" + (m.side[1 - s] != null ? " vs " + m.side[1 - s].label : "");
                    else {
                        Player hp = Bukkit.getPlayer(m.hunter[1 - s]);
                        bar = "§a§lRUNNER §7» §f" + m.alive.get(s).size() + "/" + m.runners.get(s).size() + " §7alive §8| §7hunter: "
                                + m.side[1 - s].color + (hp == null ? "?" : hp.getName()) + " §8| §dEye§7 = Runner Vision";
                    }
                }
                case OUT -> bar = "§7Tagged - spectating " + (m == null ? "" : "arena " + (m.arena + 1));
                default -> bar = "§7Bye round - your side sits out";
            }
            actionBar(p, bar);
        }
    }

    // ------------------------------------------------------------------ players

    private void equip(Player p, Side side, boolean hunter, boolean hunted) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        Color c = leatherColor(side.color);
        inv.setChestplate(leather(Material.LEATHER_CHESTPLATE, c));
        inv.setLeggings(leather(Material.LEATHER_LEGGINGS, c));
        inv.setBoots(leather(Material.LEATHER_BOOTS, c));
        if (hunter) {
            inv.setHelmet(named(new ItemStack(Material.REDSTONE_BLOCK), "§c§lHunter"));
            inv.setItem(0, named(new ItemStack(Material.BLAZE_ROD), "§c§lTagger §7(hit a runner)"));
        } else {
            inv.setHelmet(leather(Material.LEATHER_HELMET, c));
            if (hunted) inv.setItem(0, named(new ItemStack(Material.ENDER_EYE), "§d§lRunner Vision §7(right-click)"));
        }
        inv.setHeldItemSlot(0);
    }

    private static ItemStack leather(Material m, Color c) {
        ItemStack it = new ItemStack(m);
        if (it.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(c);
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
            it.setItemMeta(meta);
        }
        return it;
    }

    private static ItemStack named(ItemStack it, String name) {
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(text(name));
            it.setItemMeta(meta);
        }
        return it;
    }

    private void resetPlayer(Player p, GameMode mode) {
        clearPlayer(p);
        p.setGameMode(mode);
        p.setAllowFlight(false);
        p.setFlying(false);
        AttributeInstance maxHp = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(maxHp != null ? maxHp.getValue() : 20.0);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
    }

    private void clearPlayer(Player p) {
        p.getInventory().clear();
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
        p.setCooldown(Material.ENDER_EYE, 0);
    }

    private void splitPoints(List<UUID> members, int total, String reason) {
        if (members.isEmpty() || total <= 0) return;
        int per = Math.max(1, Math.round((float) total / members.size()));
        for (UUID u : members) ctx.addPoints(u, per, reason);
    }

    private void messageMatch(Match m, String msg) {
        for (int i = 0; i < 2; i++) {
            if (m.side[i] == null) continue;
            for (UUID u : m.side[i].members) { Player p = Bukkit.getPlayer(u); if (p != null) p.sendMessage(msg); }
        }
    }

    private List<Player> online() { return new ArrayList<>(ctx.participants()); }

    private List<UUID> onlineMembers(Side s) {
        List<UUID> out = new ArrayList<>();
        if (s == null) return out;
        Set<UUID> present = new HashSet<>();
        for (Player p : ctx.participants()) present.add(p.getUniqueId());
        for (UUID u : s.members) if (present.contains(u)) out.add(u);
        return out;
    }

    /** Removes players who left from the live round state. */
    private void pruneOffline() {
        if (ctx == null) return;
        Set<UUID> present = new HashSet<>();
        for (Player p : ctx.participants()) present.add(p.getUniqueId());
        for (Match m : matches) {
            for (int i = 0; i < 2; i++) {
                m.alive.get(i).removeIf(u -> !present.contains(u));
                if (m.hunter[i] != null && !present.contains(m.hunter[i])) m.hunter[i] = null;
            }
        }
        matchOf.keySet().removeIf(u -> !present.contains(u));
        frozen.removeIf(u -> !present.contains(u));
        visionUntil.keySet().removeIf(u -> !present.contains(u));
    }

    private boolean isOurs(Player p) {
        return running && world != null && p.getWorld().equals(world) && everyone.contains(p.getUniqueId());
    }

    private String nameOf(UUID u) {
        Player p = Bukkit.getPlayer(u);
        if (p != null) return p.getName();
        String n = Bukkit.getOfflinePlayer(u).getName();
        return n == null ? "?" : n;
    }

    private static Component text(String legacy) { return LEGACY.deserialize(legacy); }

    private static void title(Player p, String t, String sub) {
        p.showTitle(Title.title(text(t), text(sub), Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(2200), Duration.ofMillis(400))));
    }

    private static void actionBar(Player p, String s) { p.sendActionBar(text(s)); }

    private static Material concreteOf(ChatColor c) {
        if (c == null) return Material.WHITE_CONCRETE;
        return switch (c) {
            case RED -> Material.RED_CONCRETE;
            case DARK_RED -> Material.RED_TERRACOTTA;
            case GOLD -> Material.ORANGE_CONCRETE;
            case YELLOW -> Material.YELLOW_CONCRETE;
            case GREEN -> Material.LIME_CONCRETE;
            case DARK_GREEN -> Material.GREEN_CONCRETE;
            case DARK_AQUA -> Material.CYAN_CONCRETE;
            case AQUA -> Material.LIGHT_BLUE_CONCRETE;
            case BLUE -> Material.BLUE_CONCRETE;
            case DARK_BLUE -> Material.BLUE_TERRACOTTA;
            case DARK_PURPLE -> Material.PURPLE_CONCRETE;
            case LIGHT_PURPLE -> Material.PINK_CONCRETE;
            case GRAY -> Material.LIGHT_GRAY_CONCRETE;
            case DARK_GRAY -> Material.GRAY_CONCRETE;
            case BLACK -> Material.BLACK_CONCRETE;
            default -> Material.WHITE_CONCRETE;
        };
    }

    private static Color leatherColor(ChatColor c) {
        if (c == null) return Color.WHITE;
        return switch (c) {
            case RED -> Color.fromRGB(0xE03030);
            case DARK_RED -> Color.fromRGB(0x8B1010);
            case GOLD -> Color.fromRGB(0xF08020);
            case YELLOW -> Color.fromRGB(0xF0E030);
            case GREEN -> Color.fromRGB(0x70E030);
            case DARK_GREEN -> Color.fromRGB(0x2E7D32);
            case DARK_AQUA -> Color.fromRGB(0x158C99);
            case AQUA -> Color.fromRGB(0x55DDF5);
            case BLUE -> Color.fromRGB(0x3050E0);
            case DARK_BLUE -> Color.fromRGB(0x1A237E);
            case DARK_PURPLE -> Color.fromRGB(0x8030C0);
            case LIGHT_PURPLE -> Color.fromRGB(0xF070C0);
            case GRAY -> Color.fromRGB(0xA0A0A0);
            case DARK_GRAY -> Color.fromRGB(0x505050);
            case BLACK -> Color.fromRGB(0x151515);
            default -> Color.WHITE;
        };
    }

    // ------------------------------------------------------------------ listeners

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !isOurs(victim)) return;
        e.setCancelled(true);
        if (e instanceof EntityDamageByEntityEvent ed && ed.getDamager() instanceof Player attacker && isOurs(attacker)) {
            tryTag(attacker, victim);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!isOurs(p)) return;
        Location from = e.getFrom(), to = e.getTo();
        if (frozen.contains(p.getUniqueId())) {
            if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
                Location back = from.clone();
                back.setYaw(to.getYaw());
                back.setPitch(to.getPitch());
                e.setTo(back);
            }
            return;
        }
        if (to.getY() < BASE_Y - 4) {
            Match m = matchOf.get(p.getUniqueId());
            Role r = roles.get(p.getUniqueId());
            if (m != null && (r == Role.HUNTER || r == Role.RUNNER)) {
                int s = Math.max(0, m.sideIndexOf(p.getUniqueId()));
                p.setFallDistance(0f);
                e.setTo(padSpot(m, s, random.nextInt(5)));
            } else if (p.getGameMode() != GameMode.SPECTATOR) {
                e.setTo(waitSpot());
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!isOurs(p)) return;
        ItemStack item = e.getItem();
        if (item != null && item.getType() == Material.ENDER_EYE) {
            e.setCancelled(true);
            if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) useVision(p);
            return;
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.PHYSICAL) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) { if (isOurs(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) { if (isOurs(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) { if (isOurs(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent e) { if (isOurs(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && isOurs(p)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && isOurs(p)) { e.setCancelled(true); p.setFoodLevel(20); }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!running) return;
        UUID u = e.getPlayer().getUniqueId();
        Match m = matchOf.remove(u);
        frozen.remove(u);
        visionUntil.remove(u);
        if (m != null) {
            for (int i = 0; i < 2; i++) {
                m.alive.get(i).remove(u);
                if (u.equals(m.hunter[i])) {
                    m.hunter[i] = null;
                    messageMatch(m, "§7The " + m.side[i].label + " §7hunter left the game.");
                }
            }
        }
        // Re-check on the next tick, after the core has dropped the player from participants().
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (running && phase == Phase.LIVE && allMatchesDone()) endRound(false);
        });
    }
}
