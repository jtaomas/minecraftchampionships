package com.mcm.games.parkourtag;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.parkourtag.map.Cage;
import com.mcm.games.parkourtag.map.MapGrid;
import com.mcm.games.parkourtag.map.Maps;
import com.mcm.games.parkourtag.map.PtMap;
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
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
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
    private static final int INTERMISSION_SECONDS = 8;
    private static final int SELECT_SECONDS = 8;
    private static final int MAX_HUNTS = 4;
    private static final int BUILD_TIMEOUT_SECONDS = 30;

    // MCC coins (base x1.0, config coins.*; the core applies the event multiplier). See game-plugins/COINS.md.
    private int PTS_SURVIVE_TICK = 2;      // MCC: 2 every 10 s a hunted runner survives
    private int PTS_SURVIVE_FULL = 0;      // MCC: nothing extra for being alive at the end (OURS if > 0)
    private int PTS_SIDE_SURVIVE = 60;     // MCC: shared amongst the runners if any runner survives to 0:00
    private int PTS_TAG_BASE = 6;          // MCC: per tag, 1 less every 10 s (min 1)
    private int PTS_TAG_ALL_BASE = 42;     // MCC: hunter tags all runners, 7 less every 10 s
    private int PTS_TAG_ALL_STEP = 7;
    private int PTS_FIRST_CLEAR = 120;     // MCC: shared amongst the team whose hunter tags all enemy runners first
    // Pool shares divide by the players who actually played the match (4-member team: 1 hunter + 3 runners = pool / 3
    // and pool / 4, as in MCC); benched players are not counted.

    private static final int VISION_COOLDOWN_TICKS = 200;
    private static final int VISION_DURATION_TICKS = 60;

    private enum Phase { IDLE, BUILDING, INTRO, SELECT, PREP, LIVE, INTERMISSION }
    private enum Role { HUNTER, RUNNER, OUT, BYE, BENCH }

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
        /** Everyone placed in each cage this round. */
        final List<List<UUID>> members = List.of(new ArrayList<>(), new ArrayList<>());
        /** The side picks its hunter at the sign (volunteer, else random). */
        final boolean[] picksHunter = new boolean[2];
        /** Players of the bigger side sitting this match out (spectating their arena). */
        final List<UUID> benched = new ArrayList<>();
        int bigSize, smallSize;
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
    /** How many matches each player has sat out so far (fair bench rotation). */
    private final Map<UUID, Integer> benchCount = new HashMap<>();
    private boolean benchUneven = true;

    // map: one per game, picked at random among the finished maps
    private static final Map<String, List<ArenaBuilder.Placement>> PARSED = new HashMap<>();
    private PtMap map;
    private int spacing = SPACING;
    private String forcedMap;

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
        loadCoins();
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

        map = chooseMap();
        spacing = Math.max(SPACING, Math.max(map.sizeX(), map.sizeZ()) + 40);
        List<ArenaBuilder.Placement> placements = PtOverlay.merge(map, placements(map));   // + Map Edit overlay (core /mcm mapedit)
        int[] cb = PtOverlay.box(map);
        for (int i = 0; i < arenaCount; i++) {
            int ox = i * spacing;
            builder.clear(world, ox + cb[0], BASE_Y + cb[1], cb[2], ox + cb[3], BASE_Y + cb[4], cb[5]);
            builder.build(world, ox, BASE_Y, 0, placements);
        }
        phase = Phase.BUILDING;
        buildWaitSeconds = 0;
        builder.whenIdle(() -> {
            if (running && phase == Phase.BUILDING) enterIntro();
        });

        StringBuilder sb = new StringBuilder("§6§lParkour Tag §7- ");
        sb.append(sides.size()).append(" side(s), ").append(schedule.size()).append(" round(s). §7Map: §f").append(map.displayName());
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
        benchCount.clear();
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
            if (map != null) {
                int[] cb = PtOverlay.box(map);
                for (int i = 0; i < arenaCount; i++) {
                    int ox = i * spacing;
                    builder.clear(world, ox + cb[0], BASE_Y + cb[1], cb[2], ox + cb[3], BASE_Y + cb[4], cb[5]);
                }
            }
        }
        phase = Phase.IDLE;
        matches.clear();
        roles.clear();
        matchOf.clear();
        frozen.clear();
        clearVision();
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

    // Map Edit hooks (core /mcm mapedit), used by PtEdit
    boolean isRunning() { return running; }
    World editWorld() { return ensureWorld(); }
    ArenaBuilder arenaBuilder() { return builder; }
    List<ArenaBuilder.Placement> basePlacements(PtMap m) { return placements(m); }

    private Location waitSpot() { return new Location(world, WAIT_X + 0.5, WAIT_Y + 1, WAIT_Z + 0.5); }

    private Location arenaOrigin(int arena) { return new Location(world, arena * spacing, BASE_Y, 0); }

    private Location spectatorSpot(int arena) {
        double[] s = map.spectator();
        Location l = arenaOrigin(arena).add(s[0], s[1], s[2]);
        l.setYaw((float) s[3]);
        l.setPitch((float) s[4]);
        return l;
    }

    /** Spectator box of one arena copy (the build plus a margin and room above). */
    private org.bukkit.util.BoundingBox arenaBox(int arena) {
        double x0 = arena * spacing;
        return new org.bukkit.util.BoundingBox(x0 - 15, BASE_Y - 10, -15,
                x0 + map.sizeX() + 15, BASE_Y + map.sizeY() + 25, map.sizeZ() + 15);
    }

    // ------------------------------------------------------------------ maps

    private PtMap chooseMap() {
        List<PtMap> all = new ArrayList<>(Maps.all());
        all.add(new LegacyArenaMap());
        if (forcedMap != null) {
            String f = forcedMap;
            forcedMap = null;
            for (PtMap m : all) if (m.id().equalsIgnoreCase(f)) return m;
        }
        List<PtMap> done = new ArrayList<>();
        for (PtMap m : all) if (m.finished()) done.add(m);
        if (done.isEmpty()) return all.get(all.size() - 1);
        return done.get(random.nextInt(done.size()));
    }

    private List<ArenaBuilder.Placement> placements(PtMap m) {
        List<ArenaBuilder.Placement> cached = PARSED.get(m.id());
        if (cached != null) return cached;
        Map<String, BlockData> parsed = new HashMap<>();
        List<ArenaBuilder.Placement> out = new ArrayList<>();
        for (MapGrid.Block b : MapGrid.of(m).list()) {
            BlockData d = parsed.get(b.data());
            if (d == null) {
                try { d = Bukkit.createBlockData(b.data()); } catch (IllegalArgumentException ex) {
                    plugin.getLogger().warning("Bad block in map " + m.id() + ": " + b.data());
                    d = Material.STONE.createBlockData();
                }
                parsed.put(b.data(), d);
            }
            out.add(new ArenaBuilder.Placement(b.x(), b.y(), b.z(), d));
        }
        PARSED.put(m.id(), out);
        return out;
    }

    /** Admin {@code /mcm map}: the maps a game can use (finished ones first). */
    public List<String> maps() {
        List<String> out = new ArrayList<>();
        for (PtMap m : Maps.all()) out.add(m.id() + (m.finished() ? "" : " (unfinished)"));
        out.add("legacy (old practice arena)");
        return out;
    }

    /** Admin {@code /mcm map <id>}: the next Parkour Tag game uses that map. */
    public boolean forceNextMap(String id) {
        if (id == null) return false;
        String clean = id.trim().split(" ")[0];
        if (clean.equalsIgnoreCase("legacy")) { forcedMap = "legacy"; return true; }
        for (PtMap m : Maps.all()) if (m.id().equalsIgnoreCase(clean)) { forcedMap = m.id(); return true; }
        return false;
    }

    public String currentMap() { return running && map != null ? map.displayName() : null; }

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
        double[] s = map.cages().get(sideIdx).slot(slot);
        Location l = arenaOrigin(m.arena).add(s[0], s[1], s[2]);
        l.setYaw((float) s[3]);
        return l;
    }

    private Block cageBlock(Match m, int[] c) {
        Location o = arenaOrigin(m.arena);
        return world.getBlockAt(o.getBlockX() + c[0], BASE_Y + c[1], o.getBlockZ() + c[2]);
    }

    /** Team glass and carpet, a closed black door and the "Select Hunter:" sign in both cages of a match. */
    private void paintCages(Match m) {
        for (int i = 0; i < 2; i++) {
            Cage cage = map.cages().get(i);
            ChatColor c = m.side[i] == null ? null : m.side[i].color;
            Material glass = dyed(c, "STAINED_GLASS");
            Material carpet = dyed(c, "CARPET");
            for (int[] g : cage.glassCells()) cageBlock(m, g).setType(glass, false);
            for (int[] g : cage.carpetCells()) cageBlock(m, g).setType(carpet, false);
            setDoor(m, i, Material.BLACK_STAINED_GLASS);
            Block sb = cageBlock(m, cage.signPos());
            if (sb.getState() instanceof Sign sign) {
                var front = sign.getSide(org.bukkit.block.sign.Side.FRONT);
                front.line(0, Component.empty());
                front.line(1, text("§lSelect Hunter:"));
                front.line(2, text(m.side[1] == null ? "§7(practice)" : "§8(click to hunt)"));
                front.line(3, Component.empty());
                sign.setWaxed(true);
                sign.update(true, false);
            }
        }
    }

    private void setDoor(Match m, int sideIdx, Material mat) {
        for (int[] d : map.cages().get(sideIdx).doorCells()) cageBlock(m, d).setType(mat, false);
    }

    /** Door glass on "Starting in": black, black, red, yellow, lime, then open (the MG-PT-01 / MCC cage pattern). */
    private static Material doorFor(int left) {
        if (left >= 4) return Material.BLACK_STAINED_GLASS;
        if (left == 3) return Material.RED_STAINED_GLASS;
        if (left == 2) return Material.YELLOW_STAINED_GLASS;
        if (left == 1) return Material.LIME_STAINED_GLASS;
        return Material.AIR;
    }

    private static Material dyed(ChatColor c, String suffix) {
        String dye = switch (c == null ? ChatColor.WHITE : c) {
            case RED -> "RED"; case DARK_RED -> "BROWN"; case GOLD -> "ORANGE"; case YELLOW -> "YELLOW";
            case GREEN -> "LIME"; case DARK_GREEN -> "GREEN"; case DARK_AQUA -> "CYAN"; case AQUA -> "LIGHT_BLUE";
            case BLUE, DARK_BLUE -> "BLUE"; case DARK_PURPLE -> "PURPLE"; case LIGHT_PURPLE -> "PINK";
            case GRAY -> "LIGHT_GRAY"; case DARK_GRAY -> "GRAY"; case BLACK -> "BLACK"; default -> "WHITE";
        };
        Material m = Material.matchMaterial(dye + "_" + suffix);
        return m == null ? Material.WHITE_STAINED_GLASS : m;
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
        if (list.size() % 2 == 1) {
            plugin.getLogger().warning("Parkour Tag: " + list.size() + " sides is an odd number (an event normally has an even"
                    + " number of teams); one side gets a bye each round.");
            list.add(null);
        }
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
            case SELECT -> {
                phaseTicks -= 20;
                int left = Math.max(0, phaseTicks / 20);
                ctx.setTimer(left, SELECT_SECONDS);
                if (left <= 0) {
                    for (Match m : matches) finalizeMatch(m);
                    phase = Phase.PREP;
                    phaseTicks = FastTest.secs(PREP_SECONDS) * 20 + 20;
                }
            }
            case PREP -> {
                phaseTicks -= 20;
                int left = Math.max(0, phaseTicks / 20);
                ctx.setTimer(left, PREP_SECONDS);
                Material door = doorFor(left);
                for (Match m : matches) for (int i = 0; i < 2; i++) if (door != Material.AIR) setDoor(m, i, door);
                for (Player p : online()) {
                    Role r = roles.get(p.getUniqueId());
                    if (r == Role.HUNTER || r == Role.RUNNER) {
                        if (left > 0) {
                            p.showTitle(Title.title(text("§fStarting in"), text("§f§l" + left),
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
                if (liveElapsed % 10 == 0 && liveElapsed < ROUND_SECONDS) announceSurvival();
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
        clearVision();
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
            // Uneven sides: the bigger one benches (bigger - smaller) random players, fairly rotated.
            List<UUID> benched = List.of();
            int big = 0, small = Math.min(ma.size(), mb.size());
            if (benchUneven && ma.size() != mb.size()) {
                List<UUID> bigger = ma.size() > mb.size() ? ma : mb;
                big = bigger.size();
                benched = pickBench(bigger, big - small);
                bigger.removeAll(benched);
            }
            Match m = setupMatch(new Match(arena++, a, b), ma, mb);
            matches.add(m);
            if (!benched.isEmpty()) benchPlayers(m, benched, big, small);
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

        phase = Phase.SELECT;
        phaseTicks = FastTest.secs(SELECT_SECONDS) * 20 + 20;
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

    /** Puts both sides in their cages for the Select Hunter phase. Lone sides get their hunter decided here. */
    private Match setupMatch(Match m, List<UUID> ma, List<UUID> mb) {
        List<List<UUID>> ms = List.of(ma, mb);
        m.members.get(0).addAll(ma);
        m.members.get(1).addAll(mb);
        if (m.side[1] != null) {
            if (ma.size() == 1 && mb.size() == 1) {
                UUID x = ma.get(0), y = mb.get(0);
                int hx = hunts.getOrDefault(x, 0), hy = hunts.getOrDefault(y, 0);
                if (hx < hy || (hx == hy && random.nextBoolean())) m.hunter[0] = x; else m.hunter[1] = y;
            } else {
                for (int i = 0; i < 2; i++) {
                    List<UUID> mine = ms.get(i);
                    if (mine.size() >= 2) m.picksHunter[i] = true;
                    else if (mine.size() == 1) {
                        UUID lone = mine.get(0);
                        if (hunts.getOrDefault(lone, 0) < runs.getOrDefault(lone, 0)) m.hunter[i] = lone;
                    }
                }
            }
            // A hunter with nobody to chase becomes a runner.
            for (int i = 0; i < 2; i++) {
                boolean oppHunts = m.hunter[1 - i] != null || m.picksHunter[1 - i];
                int oppRunners = ms.get(1 - i).size() - (oppHunts ? 1 : 0);
                if (oppRunners <= 0) { m.hunter[i] = null; m.picksHunter[i] = false; }
            }
        }

        paintCages(m);
        String card = m.side[1] == null ? m.side[0].label + " §7(practice)" : m.side[0].label + " §fvs " + m.side[1].label;
        for (int i = 0; i < 2; i++) {
            List<UUID> list = ms.get(i);
            for (int slot = 0; slot < list.size(); slot++) {
                UUID u = list.get(slot);
                Player p = Bukkit.getPlayer(u);
                if (p == null) continue;
                matchOf.put(u, m);
                roles.put(u, Role.RUNNER);
                resetPlayer(p, GameMode.ADVENTURE);
                p.teleport(padSpot(m, i, slot));
                title(p, "§fRound " + (roundIndex + 1) + ":", card);
                if (m.picksHunter[i]) {
                    p.sendMessage("§dSelect Hunter: §7click the §fSelect Hunter:§7 sign in your cage to hunt this round"
                            + " §8(" + Math.max(0, MAX_HUNTS - hunts.getOrDefault(u, 0)) + " hunt(s) left)§7. Nobody? One of you is picked at random.");
                }
            }
        }
        return m;
    }

    /** Fair bench: whoever has sat out the fewest times goes first, random among them. */
    private List<UUID> pickBench(List<UUID> pool, int count) {
        List<UUID> left = new ArrayList<>(pool), out = new ArrayList<>();
        while (out.size() < count && !left.isEmpty()) {
            int min = Integer.MAX_VALUE;
            for (UUID u : left) min = Math.min(min, benchCount.getOrDefault(u, 0));
            List<UUID> cands = new ArrayList<>();
            for (UUID u : left) if (benchCount.getOrDefault(u, 0) == min) cands.add(u);
            UUID pick = cands.get(random.nextInt(cands.size()));
            left.remove(pick);
            out.add(pick);
            benchCount.merge(pick, 1, Integer::sum);
        }
        return out;
    }

    /** Benched players watch their own match's arena from spectator mode; both teams are told. */
    private void benchPlayers(Match m, List<UUID> benched, int big, int small) {
        m.bigSize = big;
        m.smallSize = small;
        String size = "team size " + big + " vs " + small;
        List<String> names = new ArrayList<>();
        for (UUID u : benched) {
            m.benched.add(u);
            matchOf.put(u, m);
            roles.put(u, Role.BENCH);
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            names.add(p.getName());
            SpectatorGuard.makeSpectator(p, spectatorSpot(m.arena));
            title(p, "§7§lYou're sitting out this round", "§f" + size + " §7- watch your team");
            p.sendMessage("§7You're sitting out this round (" + size + "). Watch your team's arena - no coins this match, you play again next round.");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }
        int bigSide = m.sideIndexOf(benched.get(0));
        messageMatch(m, "§e" + String.join(", ", names) + " §7(" + m.side[bigSide].label + "§7) sit" + (names.size() == 1 ? "s" : "")
                + " out this round: " + size + ", so it is " + small + "v" + small + ".");
    }

    /** A player clicked their cage sign during Select Hunter. */
    private void volunteer(Player p) {
        UUID u = p.getUniqueId();
        Match m = matchOf.get(u);
        if (m == null || roles.get(u) == Role.BENCH) return;
        int i = m.sideIndexOf(u);
        if (i < 0) return;
        if (!m.picksHunter[i]) { actionBar(p, "§7Your side's hunter is decided automatically this round."); return; }
        if (m.hunter[i] != null) {
            actionBar(p, u.equals(m.hunter[i]) ? "§aYou are hunting this round." : "§7" + nameOf(m.hunter[i]) + " is already hunting.");
            return;
        }
        if (hunts.getOrDefault(u, 0) >= MAX_HUNTS) { actionBar(p, "§cYou have already hunted " + MAX_HUNTS + " times."); return; }
        m.hunter[i] = u;
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.4f);
        for (UUID t : m.members.get(i)) {
            Player tp = Bukkit.getPlayer(t);
            if (tp != null) tp.sendMessage(m.side[i].color + p.getName() + " §7volunteered to hunt.");
        }
    }

    /** End of Select Hunter: random fallback, roles, kit and the MCC hunter announcement. */
    private void finalizeMatch(Match m) {
        for (int i = 0; i < 2; i++) {
            if (!m.picksHunter[i] || m.hunter[i] != null) continue;
            List<UUID> cands = new ArrayList<>();
            for (UUID u : m.members.get(i)) if (Bukkit.getPlayer(u) != null && hunts.getOrDefault(u, 0) < MAX_HUNTS) cands.add(u);
            if (!cands.isEmpty()) m.hunter[i] = cands.get(random.nextInt(cands.size()));
            else {
                List<UUID> online = new ArrayList<>();
                for (UUID u : m.members.get(i)) if (Bukkit.getPlayer(u) != null) online.add(u);
                if (!online.isEmpty()) m.hunter[i] = pickHunter(online);
            }
        }
        for (int i = 0; i < 2; i++) {
            for (UUID u : m.members.get(i)) {
                if (u.equals(m.hunter[i])) continue;
                m.runners.get(i).add(u);
                m.alive.get(i).add(u);
            }
        }
        for (int i = 0; i < 2; i++) m.hunted[i] = m.hunter[1 - i] != null && !m.runners.get(i).isEmpty();
        m.hadHunter = m.hunter[0] != null || m.hunter[1] != null;

        for (int i = 0; i < 2; i++) {
            UUID h = m.hunter[i];
            if (h != null) {
                hunts.merge(h, 1, Integer::sum);
                int more = Math.max(0, MAX_HUNTS - hunts.get(h));
                messageMatch(m, m.side[i].color + nameOf(h) + " §fis going to hunt the opposing team this round. They can hunt "
                        + more + " more time" + (more == 1 ? "" : "s") + ".");
            }
            for (UUID u : m.members.get(i)) {
                Player p = Bukkit.getPlayer(u);
                if (p == null) continue;
                boolean isHunter = u.equals(h);
                roles.put(u, isHunter ? Role.HUNTER : Role.RUNNER);
                if (!isHunter) runs.merge(u, 1, Integer::sum);
                equip(p, m.side[i], isHunter, m.hunted[i]);
                String opp = m.side[1 - i] == null ? "§7nobody (practice)" : m.side[1 - i].label;
                p.sendMessage("§6Round " + (roundIndex + 1) + " §7- you are " + (isHunter ? "the §c§lHUNTER" : "a §a§lRUNNER") + " §7vs " + opp);
                if (!isHunter && !m.hunted[i]) p.sendMessage("§7No enemy hunter is after you this round - free run.");
            }
        }
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
                setDoor(m, i, Material.AIR);
                UUID hid = m.hunter[i];
                Player hpl = hid == null ? null : Bukkit.getPlayer(hid);
                if (hpl != null) {
                    frozen.add(hid);
                    hpl.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, (HUNTER_DELAY_SECONDS + 1) * 20, 0, false, false, false));
                }
            }
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
                    if (PTS_SURVIVE_TICK > 0) ctx.addPoints(u, PTS_SURVIVE_TICK, "You successfully survived for " + liveElapsed + " seconds.");
                }
            }
        }
    }

    /** MCC chat line every 10 s for each hunted runner still alive. */
    private void announceSurvival() {
        if (PTS_SURVIVE_TICK > 0) return;
        for (Match m : matches) {
            for (int i = 0; i < 2; i++) {
                if (!m.hunted[i]) continue;
                for (UUID u : m.alive.get(i)) {
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) p.sendMessage("§bYou successfully survived for " + liveElapsed + " seconds.");
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
        clearVision();
        Set<UUID> survivorsShown = new HashSet<>();
        for (Match m : matches) for (int i = 0; i < 2; i++) if (m.hunted[i]) survivorsShown.addAll(m.alive.get(i));
        for (Match m : matches) {
            List<String> summary = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                if (!m.hunted[i]) continue;
                List<UUID> survivors = new ArrayList<>();
                for (UUID u : m.alive.get(i)) if (Bukkit.getPlayer(u) != null) survivors.add(u);
                for (UUID u : survivors) {
                    if (timeout && PTS_SURVIVE_TICK > 0) ctx.addPoints(u, PTS_SURVIVE_TICK, "You successfully survived for " + ROUND_SECONDS + " seconds.");
                    if (PTS_SURVIVE_FULL > 0) ctx.addPoints(u, PTS_SURVIVE_FULL, "You survived the round");
                    survivals.merge(u, 1, Integer::sum);
                    Player p = Bukkit.getPlayer(u);
                    if (p != null) { title(p, "§a§lSurvivor!", "§aWell played!"); p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.7f, 1.2f); }
                }
                if (!survivors.isEmpty()) {
                    List<UUID> share = new ArrayList<>();
                    for (UUID u : m.runners.get(i)) if (Bukkit.getPlayer(u) != null) share.add(u);
                    splitPoints(share, PTS_SIDE_SURVIVE, m.runners.get(i).size(), "Your team outlasted the hunter! Well played!");
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
            if (roles.get(p.getUniqueId()) != Role.BYE && roles.get(p.getUniqueId()) != Role.BENCH && !survivorsShown.contains(p.getUniqueId()) && !huntedShown.contains(p.getUniqueId()))
                title(p, "§c§lRound Over!", "");
        }
        huntedShown.clear();
        ctx.setTimer(INTERMISSION_SECONDS, INTERMISSION_SECONDS);
    }

    // ------------------------------------------------------------------ admin testing hooks
    // (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    /** Ends the intro, the pre-round freeze/countdown or the intermission now. */
    public boolean skipWait() {
        if (!running || ctx == null) return false;
        if (phase != Phase.INTRO && phase != Phase.SELECT && phase != Phase.PREP && phase != Phase.INTERMISSION) return false;
        phaseTicks = 20;
        second();   // the normal "time ran out" transition of that phase
        return true;
    }

    /** Ends the current round now with no round-end points, then the next round starts (or the game ends). */
    public boolean skipRound() {
        if (!running || ctx == null) return false;
        if (phase == Phase.SELECT || phase == Phase.PREP || phase == Phase.LIVE) {
            phase = Phase.INTERMISSION;
            frozen.clear();
            clearVision();
            for (Player p : online()) p.removePotionEffect(PotionEffectType.BLINDNESS);
            ctx.broadcast("§7Round " + (roundIndex + 1) + " was skipped by an admin (no round-end points).");
        } else if (phase != Phase.INTERMISSION) {
            return false;
        }
        phaseTicks = 20;
        second();   // INTERMISSION end: next round or endGame
        return true;
    }

    /** Admin {@code /parkourtag hunts <player> <n>}: sets how many times a player has hunted this game. */
    String adminSetHunts(String name, int n) {
        if (!running) return "Parkour Tag is not running.";
        Player p = Bukkit.getPlayerExact(name);
        if (p == null || !everyone.contains(p.getUniqueId())) return "No player " + name + " in this game.";
        hunts.put(p.getUniqueId(), Math.max(0, n));
        return name + " has now hunted " + Math.max(0, n) + " time(s) (cap " + MAX_HUNTS + ").";
    }

    /** Admin {@code /parkourtag status}: one line for logs and bot tests. */
    String adminStatus() {
        StringBuilder sb = new StringBuilder("[PT] phase=" + phase + " round=" + (roundIndex + 1) + "/" + schedule.size()
                + " map=" + (map == null ? "-" : map.id()) + " matches=" + matches.size());
        for (Match m : matches) {
            for (int i = 0; i < 2; i++) {
                if (m.side[i] == null) continue;
                sb.append(" | ").append(stripColor(m.side[i].label)).append(" hunter=").append(m.hunter[i] == null ? "-" : nameOf(m.hunter[i]))
                        .append(" alive=").append(m.alive.get(i).size()).append("/").append(m.runners.get(i).size());
            }
            sb.append(" bench=");
            for (UUID u : m.benched) sb.append(nameOf(u)).append(",");
        }
        sb.append(" | hunts=");
        for (Map.Entry<UUID, Integer> e : hunts.entrySet()) sb.append(nameOf(e.getKey())).append(":").append(e.getValue()).append(" ");
        return sb.toString();
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
        stopVision(v);
        m.tags[hs]++;
        totalTags.merge(h, 1, Integer::sum);
        int bracket = liveElapsed / 10;
        int pts = PTS_TAG_BASE <= 0 ? 0 : Math.max(1, PTS_TAG_BASE - bracket);
        if (pts > 0) ctx.addKillCoins(h, pts, v, victim.getName(), "You tagged " + victim.getName());

        Location at = victim.getLocation();
        world.spawnParticle(Particle.FIREWORK, at.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.08);
        world.playSound(at, Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
        if (pts <= 0) hunter.playSound(hunter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);

        clearPlayer(victim);
        victim.setGameMode(GameMode.SPECTATOR);
        victim.teleport(spectatorSpot(m.arena));
        title(victim, "§e§lTagged!", "§7by " + hunter.getName());
        victim.sendMessage("§cYou got tagged! You survived for " + liveElapsed + " seconds.");
        victim.playSound(victim.getLocation(), Sound.ENTITY_VILLAGER_HURT, 1f, 1f);

        int left = m.alive.get(vs).size();
        String msg = m.side[vs].color + victim.getName() + " §7was tagged by " + m.side[hs].color + hunter.getName() + " §7(" + left + " left)";
        messageMatch(m, msg);

        if (left == 0) {
            int bonus = Math.max(0, PTS_TAG_ALL_BASE - PTS_TAG_ALL_STEP * bracket);
            if (bonus > 0) ctx.addPoints(h, bonus, "You've tagged all enemies! Good job!");
            m.clearedAt[hs] = liveElapsed;
            title(hunter, "§e§lHunted!", "§eWell played!");
            huntedShown.add(h);
            for (UUID u : m.side[vs].members) { Player vp = Bukkit.getPlayer(u); if (vp != null) vp.sendMessage("§c[Parkour Tag] You've all been tagged!"); }
            hunter.playSound(hunter.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            if (!m.firstClearGiven && m.hunted[0] && m.hunted[1] && m.clearedAt[vs] < 0) {
                m.firstClearGiven = true;
                List<UUID> share = new ArrayList<>();
                for (UUID u : m.members.get(hs)) if (Bukkit.getPlayer(u) != null) share.add(u);   // active players only, never the bench
                splitPoints(share, PTS_FIRST_CLEAR, m.members.get(hs).size(), "Your team tagged all enemies first!");
                messageMatch(m, m.side[hs].label + " §7cleared first!");
            }
            // The cleared runners' hunter role ends; that hunter keeps going if their targets remain.
            Player hp = hunter;
            hp.sendMessage("§7All targets tagged - watch your teammates survive!");
        }
        if (allMatchesDone()) endRound(false);
    }

    // ------------------------------------------------------------------ runner vision

    private final Set<UUID> huntedShown = new HashSet<>();
    /** Runner -> hunter whose outline that runner currently sees through packets. */
    private final Map<UUID, UUID> glowTarget = new HashMap<>();

    private void useVision(Player p) {
        UUID u = p.getUniqueId();
        if (phase != Phase.LIVE || roles.get(u) != Role.RUNNER) return;
        Match m = matchOf.get(u);
        if (m == null) return;
        int s = m.sideIndexOf(u);
        if (s < 0 || !m.hunted[s]) { actionBar(p, "§7No hunter is after you."); return; }
        if (p.hasCooldown(Material.ENDER_EYE)) {
            p.sendMessage("§cRunner Vision Unavailable!");
            return;
        }
        p.setCooldown(Material.ENDER_EYE, VISION_COOLDOWN_TICKS);
        visionUntil.put(u, tickCount + VISION_DURATION_TICKS);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.8f);
        tickVision();
    }

    /** Ends one runner's vision and removes the outline from their screen. */
    private void stopVision(UUID runner) {
        visionUntil.remove(runner);
        UUID h = glowTarget.remove(runner);
        if (h != null) GlowPackets.send(Bukkit.getPlayer(runner), Bukkit.getPlayer(h), false);
    }

    private void clearVision() {
        for (UUID u : new ArrayList<>(glowTarget.keySet())) stopVision(u);
        visionUntil.clear();
    }

    private void tickVision() {
        if (visionUntil.isEmpty()) return;
        Particle.DustOptions red = new Particle.DustOptions(Color.fromRGB(255, 40, 40), 1.3f);
        for (Map.Entry<UUID, Long> e : new ArrayList<>(visionUntil.entrySet())) {
            UUID ru = e.getKey();
            if (tickCount > e.getValue()) { stopVision(ru); continue; }
            Player p = Bukkit.getPlayer(ru);
            Match m = matchOf.get(ru);
            if (p == null || m == null) { stopVision(ru); continue; }
            int s = m.sideIndexOf(ru);
            UUID hid = s < 0 ? null : m.hunter[1 - s];
            Player h = hid == null ? null : Bukkit.getPlayer(hid);
            if (h == null || !h.getWorld().equals(p.getWorld())) { stopVision(ru); continue; }
            // A true outline through walls, sent to this runner only. Re-sent every 2 ticks because the server's own
            // metadata updates (sprint, sneak) would otherwise clear the glow bit on the client.
            if (GlowPackets.send(p, h, true)) {
                glowTarget.put(ru, hid);
            } else {
                Location hl = h.getLocation();
                for (double y = 0; y <= 2.0; y += 0.25) {
                    p.spawnParticle(Particle.DUST, hl.clone().add(0.35, y, 0.35), 1, 0, 0, 0, 0, red);
                    p.spawnParticle(Particle.DUST, hl.clone().add(-0.35, y, -0.35), 1, 0, 0, 0, 0, red);
                    p.spawnParticle(Particle.DUST, hl.clone().add(0.35, y, -0.35), 1, 0, 0, 0, 0, red);
                    p.spawnParticle(Particle.DUST, hl.clone().add(-0.35, y, 0.35), 1, 0, 0, 0, 0, red);
                }
            }
        }
    }

    // ------------------------------------------------------------------ HUD

    private void updateSidebar() {
        List<String> lines = new ArrayList<>();
        lines.add("§bMap: §f" + (map == null ? "-" : map.displayName()));
        if (phase == Phase.BUILDING || phase == Phase.INTRO) {
            int left = phase == Phase.INTRO ? Math.max(0, phaseTicks / 20) : INTRO_SECONDS;
            lines.add("§bRounds: §f" + schedule.size() + " §7| §bSides: §f" + sides.size());
            lines.add("§bGame begins: §f" + clock(left));
            ctx.setSidebarLines(lines);
            return;
        }
        lines.add("§bRound: §f" + (roundIndex + 1) + "/" + schedule.size());
        switch (phase) {
            case SELECT -> lines.add("§dSelect Hunter: §f" + clock(Math.max(0, phaseTicks / 20)));
            case PREP -> lines.add("§dStarting in: §f" + clock(Math.max(0, phaseTicks / 20)));
            case LIVE -> lines.add("§cTime left: §f" + clock(Math.max(0, ROUND_SECONDS - liveElapsed)));
            default -> lines.add("§dNext Round: §f" + clock(Math.max(0, phaseTicks / 20)));
        }
        lines.add("§bRunners alive:");
        int shown = 0;
        for (Match m : matches) {
            if (shown == 2 && matches.size() > 2) { lines.add("§7+" + (matches.size() - 2) + " more arenas"); break; }
            if (m.side[1] == null) lines.add(shortLabel(m.side[0]) + " §7practice");
            else lines.add(shortLabel(m.side[0]) + " " + dots(m, 0) + " §8| " + shortLabel(m.side[1]) + " " + dots(m, 1));
            shown++;
        }
        ctx.setSidebarLines(lines);
    }

    private static String clock(int secs) { return String.format("%02d:%02d", secs / 60, secs % 60); }

    /** One dot per runner: yellow = alive, grey = tagged. Before the hunter is picked: one per player. */
    private String dots(Match m, int i) {
        if (phase == Phase.SELECT) return "§f" + "●".repeat(Math.max(0, m.members.get(i).size() - (m.picksHunter[i] || m.hunter[i] != null ? 1 : 0)));
        if (!m.hunted[i]) return "§7-";
        int alive = m.alive.get(i).size();
        int out = Math.max(0, m.runners.get(i).size() - alive);
        return "§e" + "●".repeat(alive) + "§8" + "○".repeat(out);
    }

    private static String shortLabel(Side s) {
        if (s == null) return "§7-";
        String plain = stripColor(s.label);
        if (plain.length() > 9) plain = plain.substring(0, 9);
        return s.color + plain;
    }

    private void updateActionBars() {
        if (phase != Phase.LIVE && phase != Phase.PREP && phase != Phase.SELECT) return;
        for (Player p : online()) {
            if (phase == Phase.SELECT) {
                Match sm = matchOf.get(p.getUniqueId());
                int si = sm == null ? -1 : sm.sideIndexOf(p.getUniqueId());
                if (roles.get(p.getUniqueId()) == Role.BENCH) actionBar(p, "§7Sitting out this round (team size " + sm.bigSize + " vs " + sm.smallSize + ")");
                else if (si >= 0 && sm.picksHunter[si]) actionBar(p, sm.hunter[si] == null ? "§dSelect Hunter: §7click the sign to hunt"
                        : "§d" + nameOf(sm.hunter[si]) + " §7will hunt");
                continue;
            }
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
                    if (phase == Phase.PREP && m.hunted[s]) bar = "§a§lRUNNER §7» get ready";
                    else if (!m.hunted[s]) bar = "§a§lRUNNER §7» free run" + (m.side[1 - s] != null ? " vs " + m.side[1 - s].label : "");
                    else if (p.hasCooldown(Material.ENDER_EYE)) {
                        int segs = 10 - (int) Math.ceil(p.getCooldown(Material.ENDER_EYE) / (double) (VISION_COOLDOWN_TICKS / 10));
                        segs = Math.max(0, Math.min(10, segs));
                        bar = "§eRecharging: §e" + "■".repeat(segs) + "§8" + "■".repeat(10 - segs) + " §8| §6Runner Vision";
                    } else {
                        bar = "§6Runner Vision §aready §7(right-click the eye)";
                    }
                }
                case OUT -> bar = "§7Tagged - spectating " + (m == null ? "" : "arena " + (m.arena + 1));
                case BENCH -> bar = "§7Sitting out this round" + (m == null ? "" : " (team size " + m.bigSize + " vs " + m.smallSize + ")");
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

    private void loadCoins() {
        plugin.saveDefaultConfig();
        var c = plugin.getConfig();
        PTS_SURVIVE_TICK = Math.max(0, c.getInt("coins.survive-10s", 2));
        PTS_SURVIVE_FULL = Math.max(0, c.getInt("coins.survive-round", 0));
        PTS_SIDE_SURVIVE = Math.max(0, c.getInt("coins.runners-survive-pool", 60));
        PTS_TAG_BASE = Math.max(0, c.getInt("coins.tag", 6));
        PTS_TAG_ALL_BASE = Math.max(0, c.getInt("coins.tag-all", 42));
        PTS_TAG_ALL_STEP = Math.max(0, c.getInt("coins.tag-all-step", 7));
        PTS_FIRST_CLEAR = Math.max(0, c.getInt("coins.first-clear-pool", 120));
        benchUneven = c.getBoolean("bench-uneven-teams", true);
    }

    /** "Shared amongst" pool: each listed player gets {@code total / mccShare}, mccShare = players who played the match. */
    private void splitPoints(List<UUID> members, int total, int mccShare, String reason) {
        if (members.isEmpty() || total <= 0) return;
        int per = Math.round((float) total / Math.max(1, mccShare));
        if (per <= 0) return;
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
        if (map != null && to.getY() < BASE_Y + map.voidY()) {
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
        Block clicked = e.getClickedBlock();
        if (clicked != null && phase == Phase.SELECT && clicked.getState() instanceof Sign
                && (e.getAction() == Action.RIGHT_CLICK_BLOCK || e.getAction() == Action.LEFT_CLICK_BLOCK)) {
            e.setCancelled(true);
            volunteer(p);
            return;
        }
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
        stopVision(u);
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
