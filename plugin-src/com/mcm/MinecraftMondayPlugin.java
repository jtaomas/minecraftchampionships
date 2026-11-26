package com.mcm;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.*;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;
import com.mcm.api.*;
import com.mcm.dropper.ImportedDropper;
import com.mcm.dropper.ImportedDroppers;
import org.bukkit.event.server.PluginDisableEvent;
import com.mcm.lobby.HubManager;

public class MinecraftMondayPlugin extends JavaPlugin implements Listener {

    public enum GameType {
        LOBBY("Lobby", Material.NETHER_STAR),
        SURVIVAL_GAMES("Survival Games (Blitz SG)", Material.IRON_SWORD),
        PARKOUR("Parkour Spiral", Material.FEATHER),
        DROPPER("Multi-Stage Dropper", Material.WATER_BUCKET),
        TNT_RUN("TNT Run", Material.TNT),
        SPLEEF("Spleef / Splegg", Material.DIAMOND_SHOVEL),
        KIT_PVP("Battle Arena (KitPvP)", Material.DIAMOND_CHESTPLATE),
        ONE_SHOT("One Shot (OITC)", Material.BOW),
        SURVIVAL("Speed Survival (15m)", Material.GOLDEN_APPLE),
        EXTERNAL("MCC Game", Material.COMMAND_BLOCK);

        public final String display;
        public final Material icon;
        GameType(String display, Material icon) { this.display = display; this.icon = icon; }
    }

    public enum KitType {
        KNIGHT("Knight", Material.DIAMOND_SWORD),
        ARCHER("Archer", Material.BOW),
        TANK("Tank", Material.DIAMOND_CHESTPLATE),
        ASSASSIN("Assassin", Material.FEATHER);

        public final String display;
        public final Material icon;
        KitType(String display, Material icon) { this.display = display; this.icon = icon; }
    }

    /**
     * A Multi-Stage Dropper shaft. Built-in stages are in dropper_world at hard-coded spots and are completed by landing
     * on {@code targetMat} at the bottom (y <= 106); imported stages ({@code imported != null}, loaded at runtime from
     * plugins/MinecraftMondayCore/imported-droppers/) are completed by landing in their pool.
     */
    private record DropperStage(String name, String hint, Material targetMat, double x, double y, double z, float yaw, ImportedDropper imported) {
        DropperStage(String name, String hint, Material targetMat, double x, double y, double z, float yaw) {
            this(name, hint, targetMat, x, y, z, yaw, null);
        }
    }

    private static final List<DropperStage> ALL_DROPPER_STAGES = Arrays.asList(
            new DropperStage("Jungle Drop", "VINES", Material.VINE, 19.5, 201.0, 5038.5, -90f),
            new DropperStage("City Drop", "SLIME BLOCKS", Material.SLIME_BLOCK, 19.5, 201.0, 5098.5, -90f),
            new DropperStage("Space Drop", "WATER", Material.WATER, 19.5, 201.0, 5158.5, -90f),
            new DropperStage("Mineshaft Drop", "COBWEBS", Material.COBWEB, 19.5, 201.0, 5219.5, 90f),
            new DropperStage("Poolrooms Drop", "WATER", Material.WATER, 19.5, 201.0, 5279.5, -90f),
            new DropperStage("Village Drop", "SLIME BLOCKS", Material.SLIME_BLOCK, 19.5, 201.0, 5309.5, -90f)
    );

    private final TeamScoring scoring = new TeamScoring();
    private final McmHud hud = new McmHud(scoring);
    private final Random rng = new Random();

    private World world;
    private World sgWorld;
    private World sg4World;
    private World parkourWorld;
    private World dropperWorld;
    private World tntrunWorld;
    private World spleefWorld;
    private World pvpWorld;
    private World oneshotWorld;
    private World survivalWorld;

    private BossBar bossBar;
    private GameType currentGame = GameType.LOBBY;
    private boolean championshipMode = false;
    private static final List<String> DEFAULT_BUILT_IN_PLAYLIST = Arrays.asList(
            "sg", "parkour", "dropper", "tntrun", "spleef", "kitpvp", "survival"
    );
    private final List<String> playlist = new ArrayList<>();

    // External (API-registered) games
    private McmGame activeExternal;
    private ExternalContext activeCtx;
    private String finishedNameOverride;
    private final List<String> externalSidebar = new ArrayList<>();
    private final Set<UUID> externalParticipants = new LinkedHashSet<>();
    /** Everyone who was a participant when the running external game started (to hand rejoiners back to it). */
    private final Set<UUID> externalRoster = new HashSet<>();
    private static final String EXTRA_GUI_TITLE = ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "More MCC Games";
    private int playlistIndex = 0;
    /** MCC coin multiplier of the external game running now (set in startExternal). */
    private double coinMultiplier = 1.0;
    /** Side key declared event winner by a game (Dodgebolt), or null. Reset when a championship starts. */
    private String declaredChampionSide;
    private String podiumChampionSide;

    // Event & multi-round state
    private int roundNumber = 1;
    private int maxRounds = 1;
    private int timeLeft = 0;
    private int totalDuration = 180;
    private int startFreezeTicks = 0;
    private boolean gracePeriod = false;
    private boolean blitzDropped = false;
    private boolean sgRefilled = false;
    private boolean sgDeathmatch = false;
    private boolean useSg4Map = true;

    // Speed Survival (15m) state
    private int survivalMatchNumber = 0;
    private int survivalCenterX = 0;
    private int survivalCenterZ = 0;
    private boolean survivalDrop1 = false;
    private boolean survivalDrop2 = false;
    private boolean survivalDeathmatch = false;
    private final Set<String> survivalSupplyChests = new HashSet<>();

    private final Set<UUID> alivePlayers = new HashSet<>();
    private final List<UUID> finishOrder = new ArrayList<>();
    /**
     * Players the core itself put in SPECTATOR mode while a game runs: finished/eliminated players of a built-in
     * game and default late joiners (built-in or external without its own late-join handling). They are kept
     * inside the running game's arena; cleared whenever a round/game starts or everyone returns to the lobby.
     */
    private final Set<UUID> coreSpectators = new HashSet<>();
    /** Overview point and watch box of the running built-in sub-round (null outside built-in games). */
    private McmSpectator.Zone builtInZone;
    private final Map<UUID, Integer> roundKills = new HashMap<>();
    private final Map<UUID, Integer> killstreaks = new HashMap<>();

    // Parkour tracking
    private final Map<UUID, Location> parkourCheckpoint = new HashMap<>();
    private final Map<UUID, Set<String>> visitedPlates = new HashMap<>();
    private final Map<UUID, Double> maxParkourY = new HashMap<>();
    private final Map<UUID, Boolean> climbedPastWater = new HashMap<>();

    // Dropper multi-stage tracking
    private final List<DropperStage> activeDropperStages = new ArrayList<>();
    private final Map<UUID, Integer> dropperStageIdx = new HashMap<>();
    private final Map<UUID, Long> dropperAdvanceCooldown = new HashMap<>();
    private int[] stageFinishCounts = new int[5];
    /** Imported dropper shafts (null until enable; empty without the imported-droppers folder). */
    private ImportedDroppers importedDroppers;

    // TNT Run & Spleef block restoration + abilities
    private final Set<String> scheduledTntKeys = new HashSet<>();
    private final List<Location> brokenTntBlocks = new ArrayList<>();
    private final Map<UUID, Integer> doubleJumpsLeft = new HashMap<>();
    private final Map<Location, BlockData> brokenSpleefBlocks = new LinkedHashMap<>();
    private final Map<UUID, Long> spleggCooldowns = new HashMap<>();
    private final Set<UUID> spleggProjectiles = new HashSet<>();

    // KitPvP kits
    private final Map<UUID, KitType> playerKits = new HashMap<>();

    private BukkitTask gameLoopTask;
    private BukkitTask pendingTransitionTask;
    private BukkitTask fastHazardTask;

    // 3.2.0 presentation: per-game points snapshot (for placement reveals), pending reveal continuation, podium
    private final Map<UUID, Integer> gameStartPoints = new HashMap<>();
    private Runnable pendingRevealAfter;
    // Admin testing hooks: the core wait that is running now (round break, intermission, vote, reveal) and how to finish it
    private String pendingWaitLabel;
    private Runnable pendingWaitFinish;
    private boolean skipRoundNoAwards = false;
    private String forcedSgMap;
    private AdminTools adminTools;
    void openMapEdit(Player p) { if (mapEdit != null) mapEdit.openMenu(p); }
    private com.mcm.mapedit.MapEditManager mapEdit;
    private FeedbackLog feedback;
    private final Map<Location, BlockData> podiumOriginal = new LinkedHashMap<>();
    private final List<UUID> podiumPlayers = new ArrayList<>();
    private Location podiumCenter;
    private BukkitTask podiumRemoveTask;
    private BukkitTask podiumShowTask;
    private World podiumWorld;

    // MCC hub (world mcc_lobby): lobby for MCC (external) games only; the MM lobby (world) is unchanged
    private HubManager hub;
    /** True while the current/last event context is MCC (MCC championship or a single MCC game). */
    private boolean mccContext = false;
    private static final String MAP_GUI_TITLE = ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Pick a map";
    /** Map picker: which game each admin is choosing a map for, and the map ids in slot order. */
    private final Map<UUID, String> mapPickGame = new HashMap<>();
    private final Map<UUID, List<String>> mapPickIds = new HashMap<>();
    private static final String MCC_GUI_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "MCC Hub " + ChatColor.DARK_GRAY + "- Games";
    private static final String SELECTOR_GUI_TITLE = ChatColor.AQUA + "" + ChatColor.BOLD + "Lobby Selector";

    private static final int[] TEAM_GUI_SLOTS = {2, 3, 4, 5, 6, 11, 12, 13, 14, 15};
    private static final String ADMIN_GUI_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "MCM Tournament Controller";
    private static final String TEAM_GUI_TITLE = ChatColor.AQUA + "" + ChatColor.BOLD + "Select Your Team";
    private static final String KIT_GUI_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Choose Your PvP Kit";

    /** Context for the chat feedback log: {game id, map id}. */
    String[] chatLogContext() {
        try {
            return new String[] { feedbackGameId(), feedbackMapId() };
        } catch (Throwable t) {
            return new String[] { "unknown", "unknown" };
        }
    }

    /** Current game id for /pfb, /gfb and chat logs: registered game id, built-in id, or "lobby". */
    String feedbackGameId() {
        try {
            if (currentGame == GameType.EXTERNAL && activeExternal != null) return activeExternal.id();
            return currentGame == GameType.LOBBY ? "lobby" : builtInId(currentGame);
        } catch (Throwable t) {
            return "unknown";
        }
    }

    /** Current game display name for feedback. */
    String feedbackGameName() {
        try {
            return currentDisplay();
        } catch (Throwable t) {
            return feedbackGameId();
        }
    }

    /**
     * Current map/arena id for feedback. Built-in games have one or two fixed arenas; external games
     * report their own current map. Never null.
     */
    String feedbackMapId() {
        try {
            if (currentGame == GameType.EXTERNAL && activeExternal != null) {
                String m = activeExternal.currentMap();
                return (m == null || m.isBlank()) ? "default" : m;
            }
            return switch (currentGame) {
                case SURVIVAL_GAMES -> useSg4Map ? "sg4" : "kalas";
                case PARKOUR -> "spiral";
                case DROPPER -> "multi-stage";
                case TNT_RUN -> "tntrun";
                case SPLEEF -> "spleef";
                case KIT_PVP -> "battle-arena";
                case SURVIVAL -> "survival";
                case ONE_SHOT -> "oneshot";
                default -> "lobby";
            };
        } catch (Throwable t) {
            return "unknown";
        }
    }

    /** Human-readable map/arena name for feedback; the id itself when a game has no separate display name. */
    String feedbackMapName() {
        try {
            if (currentGame == GameType.EXTERNAL && activeExternal != null) return feedbackMapId();
            return switch (currentGame) {
                case SURVIVAL_GAMES -> useSg4Map ? "SG4" : "Kalas";
                case PARKOUR -> "Parkour Spiral";
                case DROPPER -> "Multi-Stage Dropper";
                case TNT_RUN -> "TNT Run";
                case SPLEEF -> "Spleef / Splegg";
                case KIT_PVP -> "Battle Arena";
                case SURVIVAL -> "Speed Survival";
                case ONE_SHOT -> "One Shot";
                default -> "Lobby";
            };
        } catch (Throwable t) {
            return feedbackMapId();
        }
    }

    /** "MCC" for an MCC event, "MM" for Minecraft Monday, "lobby" between events. */
    String feedbackEvent() {
        if (currentGame == GameType.LOBBY && !championshipMode) return "lobby";
        return mccContext ? "MCC" : "MM";
    }

    boolean feedbackChampionship() { return championshipMode; }

    private PackServer packServer;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new CommandBlockGuard(), this);
        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
            public void onHudChat(io.papermc.paper.event.player.AsyncChatEvent e) {
                e.renderer((source, name, message, viewer) -> hud.chat(source, message, viewer instanceof Player vp && McmFx.hasPack(vp)));
            }
            @EventHandler
            public void onHudQuit(PlayerQuitEvent e) { hud.forget(e.getPlayer()); CoinFeedback.forget(e.getPlayer().getUniqueId()); }
        }, this);
        CoinFeedback.init(this);
        packServer = new PackServer(this);
        packServer.start();
        world = Bukkit.getWorlds().get(0);
        setupWorldRules(world);

        sg4World = loadMapWorld("sg4_world");
        sgWorld = loadMapWorld("sg_world");
        parkourWorld = loadMapWorld("parkour_world");
        dropperWorld = loadMapWorld("dropper_world");
        tntrunWorld = loadMapWorld("tntrun_world");
        spleefWorld = loadMapWorld("spleef_world");
        pvpWorld = loadMapWorld("pvp_world");
        oneshotWorld = loadMapWorld("oneshot_world");
        survivalWorld = loadOrCreateSurvivalWorld();

        bossBar = Bukkit.createBossBar(
                ChatColor.GOLD + "" + ChatColor.BOLD + "★ MINECRAFT CHAMPIONSHIPS ★ " + ChatColor.GRAY + "┃ " + ChatColor.YELLOW + "Lobby",
                BarColor.YELLOW, BarStyle.SOLID
        );

        new BukkitRunnable() {
            @Override public void run() { updateAllScoreboards(); }
        }.runTaskTimer(this, 20L, 20L);
        new BukkitRunnable() {
            @Override public void run() { confineExternalCoreSpectators(); }
        }.runTaskTimer(this, 20L, 20L);

        McmApi.setBridge(new McmApi.Bridge() {
            @Override public String displayNameOf(String id) {
                McmGame g = McmApi.game(id);
                if (g != null) return g.displayName();
                GameType gt = parseGame(id);
                return gt != null ? gt.display : id;
            }
            @Override public List<String> builtInGameIds() { return List.of("sg", "parkour", "dropper", "tntrun", "spleef", "kitpvp", "survival", "oneshot"); }
            @Override public boolean isChampionshipRunning() { return championshipMode; }
            @Override public String currentGameId() {
                if (currentGame == GameType.EXTERNAL && activeExternal != null) return activeExternal.id();
                return currentGame == GameType.LOBBY ? "lobby" : builtInId(currentGame);
            }
        });
        getConfig().addDefault("championship-playlist", new ArrayList<String>());
        getConfig().addDefault("mcc-hub.enabled", true);
        getConfig().addDefault("mcc-hub.ambient-fireworks-per-minute", 5.0);
        getConfig().addDefault("mcc-championship-playlist", new ArrayList<String>());
        getConfig().addDefault("finale-game", "dodgebolt");
        getConfig().addDefault("fast-test", false);
        getConfig().addDefault("admin-tools-item", true);
        getConfig().addDefault("admin-tools-hidden-in-games", new ArrayList<>(List.of("bingo", "skyblockle")));
        getConfig().addDefault("minecraft-monday-external-games", new ArrayList<>(List.of("findthebutton")));
        getConfig().addDefault("hidden-games", new ArrayList<>(List.of("footrace", "decisiondome")));
        getConfig().addDefault("default-lobby", "mcc");
        getConfig().addDefault("hud.event-name", "MINECRAFT CHAMPIONSHIPS");
        getConfig().addDefault("dropper.stage-count", 5);
        getConfig().addDefault("dropper.imported-stages", "always");
        getConfig().addDefault("coins.event-multiplier.enabled", true);
        getConfig().addDefault("coins.event-multiplier.eight-games", Arrays.asList(1.0, 1.5, 1.5, 2.0, 2.0, 2.5, 2.5, 3.0));
        getConfig().addDefault("coins.event-multiplier.six-games", Arrays.asList(1.0, 1.5, 2.0, 2.0, 2.5, 3.0));
        getConfig().addDefault("coins.event-multiplier.no-coin-games", new ArrayList<>(List.of("dodgebolt", "decisiondome")));
        getConfig().addDefault("coins.event-multiplier.outside-championship", 1.0);
        getConfig().addDefault("coins.mcc-team-size", 4);
        getConfig().addDefault("coins.team-pool-split", "mcc");
        getConfig().addDefault("coins.short-team-bonus", true);
        getConfig().addDefault("teams.size", 4);
        getConfig().addDefault("teams.count", 8);
        getConfig().addDefault("staff", new ArrayList<String>());
        getConfig().options().copyDefaults(true);
        getConfig().setInlineComments("coins.short-team-bonus", List.of(
                "Short-team bonus: every coin a player earns is multiplied by teams.size / registered team size (a team of 3 = x1.33, a duo = x2).",
                "The size is snapshotted at the start of each game. Team pools and split side points are not scaled twice."));
        getConfig().setInlineComments("teams.size", List.of(
                "Players per full team (/mcm teamsize <n> changes and saves it). Auto-balance and the team cap count registered members, online or not."));
        getConfig().setInlineComments("teams.count", List.of(
                "How many teams are in use (1-10, in the order Red, Orange, Yellow, Lime, Green, Cyan, Aqua, Blue, Purple, Pink): team pads, /team slots, auto-balance, Decision Dome pods."));
        getConfig().setInlineComments("staff", List.of(
                "Player names that are staff/spectators: never put on a team or into a game as a solo side. Same as /mcm spectate on, or the permission mcm.staff."));
        getConfig().setInlineComments("mcc-hub.enabled", List.of(
                "Build/load the MCC hub island (world mcc_lobby) as the lobby for MCC games."));
        getConfig().setInlineComments("hidden-games", List.of(
                "Installed games kept out of the menus, map picker and championship playlists (still startable with /mcm play <id>)."));
        getConfig().setInlineComments("default-lobby", List.of(
                "Where players spawn: mcc = the Block Showdown MCC hub (when built), mm = the Minecraft Monday lobby. The other one is reached from the compass."));
        getConfig().setInlineComments("hud.event-name", List.of("Event name at the top of the MCC-style sidebar and tab list."));
        getConfig().setInlineComments("mcc-hub.ambient-fireworks-per-minute", List.of(
                "Average ambient fireworks per minute over the MCC hub between games (0 = none). Game wins, the podium and the vote reveal still fire."));
        getConfig().setInlineComments("mcc-championship-playlist", List.of(
                "MCC game ids for /mcm start mcc. Empty = every installed MCC game."));
        getConfig().setInlineComments("finale-game", List.of(
                "Played last, between the top two teams, and never a Decision Dome vote option (MCC: Dodgebolt). Empty = none."));
        getConfig().setInlineComments("championship-playlist", List.of(
                "Game ids in order. Empty = the 7 built-in events followed by every installed MCC game.",
                "Built-ins: sg, parkour, dropper, tntrun, spleef, kitpvp, survival, oneshot. Run /mcm games for MCC game ids."));
        getConfig().setInlineComments("fast-test", List.of(
                "Admin fast-test mode (/mcm fast on|off): every core wait (countdowns, intermissions, votes, reveals) is cut to <= 1 s."));
        getConfig().setInlineComments("admin-tools-item", List.of(
                "Give ops the Admin Tools feather (hotbar slot 9 + inventory slot 18) that opens /mcm tools."));
        getConfig().setInlineComments("admin-tools-hidden-in-games", List.of(
                "Game ids during which the Admin Tools feather is removed (they scan inventories, e.g. Bingo counts feathers). Use /mcm tools then."));
        getConfig().setInlineComments("minecraft-monday-external-games", List.of(
                "Ids of installed external games that belong to Minecraft Monday (with the 7 built-ins, MM lobby, /mcm start),",
                "not MCC (excluded from the MCC hub list, /mcm start mcc and the Decision Dome vote)."));
        getConfig().setInlineComments("coins.event-multiplier.enabled", List.of(
                "MCC coin multiplier per game slot of a championship (game-plugins/COINS.md). Games award base (x1.0) MCC coins."));
        getConfig().setInlineComments("coins.event-multiplier.eight-games", List.of(
                "MCC regular event: game 1 x1.0, 2-3 x1.5, 4-5 x2.0, 6-7 x2.5, 8 x3.0. Other playlist lengths (except 6) map proportionally onto this list."));
        getConfig().setInlineComments("coins.event-multiplier.six-games", List.of("MCC 6-game event: x1.0, x1.5, x2.0, x2.0, x2.5, x3.0."));
        getConfig().setInlineComments("coins.event-multiplier.no-coin-games", List.of(
                "Playlist games that award no coins (Dodgebolt, the dome): skipped when counting game slots, always x1.0."));
        getConfig().setInlineComments("coins.event-multiplier.outside-championship", List.of(
                "Multiplier for a single game started with /mcm play (not part of a championship). Staging tests can set e.g. 2.5."));
        getConfig().setInlineComments("coins.mcc-team-size", List.of(
                "MCC team size. A team pool ('160 coins shared amongst the team') pays pool / this to each member, the MCC per-member share."));
        getConfig().setInlineComments("coins.team-pool-split", List.of(
                "mcc = each member gets pool / coins.mcc-team-size (default); team = the pool is split across the side's online members."));
        getConfig().setInlineComments("dropper.stage-count", List.of(
                "Stages per Multi-Stage Dropper round (1-12, capped by the stages available)."));
        getConfig().setInlineComments("dropper.imported-stages", List.of(
                "Imported shafts from plugins/MinecraftMondayCore/imported-droppers/: always = every ready one is in the round, the rest",
                "built-in stages; mixed = shuffled into one pool with the built-ins; off = built-in stages only."));
        saveConfig();
        scoring.configure(getConfig().getInt("teams.size", 4), getConfig().getInt("teams.count", 8),
                getConfig().getBoolean("coins.short-team-bonus", true));
        scoring.load(new java.io.File(getDataFolder(), "teams.yml"));
        scoring.setExcluded(id -> {
            Player op = Bukkit.getPlayer(id);
            if (op != null ? isStaff(op) : scoring.isMarkedSpectator(id) || staffByName(scoring.nameOf(id))) return true;
            return championshipMode && scoring.getTeam(id) == null;
        });
        getLogger().info("Teams: " + scoring.getActiveTeams() + " x up to " + scoring.getMaxTeamSize() + ", "
                + scoring.registeredTotal() + " players registered (teams.yml).");
        importedDroppers = new ImportedDroppers(this);
        try { importedDroppers.enable(dropperWorld); }
        catch (Throwable t) { getLogger().log(Level.WARNING, "Imported droppers failed to start", t); }
        McmApi.setFastTest(getConfig().getBoolean("fast-test", false));
        if (McmApi.isFastTest()) getLogger().info("Admin fast-test mode is ON (all core waits <= 1 s). /mcm fast off to disable.");
        adminTools = new AdminTools(this);
        mapEdit = new com.mcm.mapedit.MapEditManager(this);
        mapEdit.enable();
        feedback = new FeedbackLog(this);
        Bukkit.getPluginManager().registerEvents(new FishHookFix(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatLog(this), this);
        adminTools.enable();

        McmLobby.setBridge(new McmLobby.Bridge() {
            @Override public McmLobby.Venue venue() { return hubReady() ? hub : null; }
            @Override public boolean isMccEvent() { return mccContext; }
        });
        if (getConfig().getBoolean("mcc-hub.enabled", true)) {
            try {
                hub = new HubManager(this, this::hubBoardLines, this::joinTeamFromPad,
                        () -> currentGame == GameType.LOBBY, this::hubTeamPadLabel);
                hub.setActiveTeams(scoring::getActiveTeams);
                hub.enable();
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "MCC hub failed to start", t);
                hub = null;
            }
        }

        getLogger().info("MinecraftMondayCore v3.2.0 enabled! 8 community maps + survival_world ready.");
    }

    private World loadOrCreateSurvivalWorld() {
        World w = Bukkit.getWorld("survival_world");
        if (w == null) {
            w = new WorldCreator("survival_world")
                    .environment(World.Environment.NORMAL)
                    .type(WorldType.NORMAL)
                    .createWorld();
        }
        if (w != null) {
            setupWorldRules(w);
            w.setKeepSpawnInMemory(false);
            w.setDifficulty(Difficulty.NORMAL);
        }
        return w;
    }

    private void setSurvivalWorldActive(boolean active) {
        if (survivalWorld == null) return;
        if (active) {
            survivalWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
            survivalWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            survivalWorld.setGameRule(GameRule.DO_MOB_SPAWNING, true);
            survivalWorld.setGameRule(GameRule.KEEP_INVENTORY, false);
            survivalWorld.setGameRule(GameRule.NATURAL_REGENERATION, true);
            survivalWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            survivalWorld.setTime(1000L);
            survivalWorld.setStorm(false);
            survivalWorld.setDifficulty(Difficulty.NORMAL);
        } else {
            setupWorldRules(survivalWorld);
            survivalWorld.getWorldBorder().reset();
        }
    }

    @Override
    public void onDisable() {
        scoring.save();
        CoinFeedback.shutdown();
        if (packServer != null) packServer.stop();
        if (mapEdit != null) { try { mapEdit.disable(); } catch (Throwable t) { getLogger().log(Level.WARNING, "Map Edit shutdown", t); } }
        if (adminTools != null) { try { adminTools.disable(); } catch (Throwable ignored) {} }
        if (importedDroppers != null) { try { importedDroppers.disable(); } catch (Throwable ignored) {} }
        removePodium(false);
        if (hub != null) { try { hub.disable(); } catch (Throwable ignored) {} }
        McmLobby.setBridge(null);
        stopExternalIfActive();
        McmApi.setBridge(null);
        cancelTasks();
        restoreArenas();
    }

    private World loadMapWorld(String name) {
        World w = Bukkit.getWorld(name);
        if (w == null && new java.io.File(Bukkit.getWorldContainer(), name).exists()) {
            w = new WorldCreator(name).createWorld();
        }
        if (w != null) {
            setupWorldRules(w);
            w.setKeepSpawnInMemory(false);
            if (name.equals("oneshot_world")) {
                w.getEntitiesByClass(Item.class).forEach(Entity::remove);
            }
        }
        return w;
    }

    private void setupWorldRules(World w) {
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setTime(6000);
        w.setStorm(false);
    }

    private void cancelTasks() {
        if (gameLoopTask != null) { gameLoopTask.cancel(); gameLoopTask = null; }
        if (pendingTransitionTask != null) { pendingTransitionTask.cancel(); pendingTransitionTask = null; }
        if (fastHazardTask != null) { fastHazardTask.cancel(); fastHazardTask = null; }
        pendingRevealAfter = null;
        clearPendingWait();
    }

    // ==================== COMMANDS & GUIS ====================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("pfb") || cmd.getName().equalsIgnoreCase("gfb")) {
            if (feedback == null) feedback = new FeedbackLog(this);
            feedback.handle(sender, cmd.getName(), args);
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("mcm") && args.length > 0 && args[0].equalsIgnoreCase("cointest")) {
            if (sender instanceof Player pl && !pl.isOp()) {
                pl.sendMessage(ChatColor.RED + "Only tournament admins (OPs) can run cointest.");
                return true;
            }
            coinTest(sender, args);
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("mcm") && args.length > 0 && args[0].equalsIgnoreCase("selftest")) {
            if (sender instanceof Player pl && !pl.isOp()) {
                pl.sendMessage(ChatColor.RED + "Only tournament admins (OPs) can run selftest.");
                return true;
            }
            runSelfTest(sender);
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("mcm") && args.length > 0 && isTestingSub(args[0])) {
            if (sender instanceof Player pl && !pl.isOp()) {
                pl.sendMessage(ChatColor.RED + "Only tournament admins (OPs) can use /mcm. Use /team to pick a team!");
                return true;
            }
            testingCommand(sender, args);
            return true;
        }
        if (!(sender instanceof Player p)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
                championshipMode = false;
                returnToLobby(ChatColor.YELLOW + "Tournament stopped by Console.");
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("play")) {
                championshipMode = false;
                if (!startById(args[1])) sender.sendMessage("Unknown game. Options: " + String.join(", ", allGameIds()));
            } else {
                if (args.length > 0 && args[0].equalsIgnoreCase("mapedit")) mapEdit.command(sender, args);
                else if (args.length > 0 && (args[0].equalsIgnoreCase("team") || args[0].equalsIgnoreCase("spectate")
                        || args[0].equalsIgnoreCase("teamsize"))) teamAdminCommand(sender, args);
                else if (args.length > 0 && args[0].equalsIgnoreCase("games")) listGames(sender);
                else if (args.length > 0 && args[0].equalsIgnoreCase("start")) startChampionship(args.length > 1 && args[1].equalsIgnoreCase("mcc"));
                else if (args.length > 0 && args[0].equalsIgnoreCase("lobby")) lobbyCommand(sender, args);
                else sender.sendMessage("Use in-game as OP (/mcm or /team) or run 'mcm selftest|start|games|play <game>|stop|help' from console (see mcm help).");
            }
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("team")) {
            openTeamGui(p);
            return true;
        }
        if (!p.isOp()) {
            p.sendMessage(ChatColor.RED + "Only tournament admins (OPs) can use /mcm. Use /team to pick a team!");
            return true;
        }
        if (args.length == 0) {
            openAdminGui(p);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "start" -> startChampionship(args.length > 1 && args[1].equalsIgnoreCase("mcc"));
            case "championship" -> startChampionship(args.length > 1 && args[1].equalsIgnoreCase("mcc"));
            case "lobby", "hub" -> lobbyCommand(p, args);
            case "stop" -> {
                championshipMode = false;
                returnToLobby(ChatColor.YELLOW + "Tournament stopped by " + p.getName() + ".");
            }
            case "games" -> listGames(p);
            case "preview" -> previewCommand(p, args);
            case "reset" -> {
                scoring.resetScores();
                broadcast(ChatColor.RED + "All tournament scores have been reset!");
                updateAllScoreboards();
                if (hub != null) hub.refreshDisplays();
            }
            case "teamsize", "team", "spectate" -> teamAdminCommand(p, args);
            case "play" -> {
                if (args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "Usage: /mcm play <" + String.join("|", allGameIds()) + ">");
                    return true;
                }
                championshipMode = false;
                if (!startById(args[1])) p.sendMessage(ChatColor.RED + "Unknown game. Options: " + String.join(", ", allGameIds()));
            }
            default -> openAdminGui(p);
        }
        return true;
    }

    private GameType parseGame(String s) {
        return switch (s.toLowerCase()) {
            case "sg", "survivalgames", "blitz", "hg" -> GameType.SURVIVAL_GAMES;
            case "parkour", "pk", "spiral" -> GameType.PARKOUR;
            case "dropper", "drop" -> GameType.DROPPER;
            case "tntrun", "tnt" -> GameType.TNT_RUN;
            case "spleef", "splegg" -> GameType.SPLEEF;
            case "kitpvp", "pvp", "arena", "harb" -> GameType.KIT_PVP;
            case "survival", "uhc", "speeduhc", "surv", "hc" -> GameType.SURVIVAL;
            case "oneshot", "oitq", "oitc", "bow" -> GameType.ONE_SHOT;
            default -> null;
        };
    }

    private void skipCurrentEvent(Player admin) {
        if (currentGame == GameType.LOBBY && pendingTransitionTask == null) {
            if (admin != null) admin.sendMessage(ChatColor.YELLOW + "No event is currently running.");
            return;
        }
        String who = admin != null ? admin.getName() : "Console";
        if (currentGame == GameType.LOBBY && pendingTransitionTask != null && pendingRevealAfter != null) {
            // Skipping the placement reveal: fast-forward to whatever comes after it
            Runnable after = pendingRevealAfter;
            pendingRevealAfter = null;
            pendingTransitionTask.cancel();
            pendingTransitionTask = null;
            broadcast(ChatColor.GOLD + who + " skipped the results reveal.");
            after.run();
            return;
        }
        if (currentGame == GameType.LOBBY && pendingTransitionTask != null) {
            // Skipping the between-game / between-round countdown!
            pendingTransitionTask.cancel();
            pendingTransitionTask = null;
            McmVoteProvider vp = McmApi.voteProvider();
            if (vp != null) vp.cancelVote();
            if (championshipMode && playlistIndex < playlist.size()) {
                broadcast(ChatColor.GOLD + who + " skipped the countdown!");
                startById(playlist.get(playlistIndex));
            }
            return;
        }
        broadcast(ChatColor.GOLD + "⏭ " + who + " skipped " + currentDisplay() + "!");
        roundNumber = maxRounds; // force skipping remaining sub-rounds of multi-round events
        endCurrentEvent("Skipped by Admin");
    }

    // ==================== TEAMS: staff, lock, persistence, admin commands ====================

    /** Staff/spectator: marked with /mcm spectate, listed in config {@code staff}, or holding the permission {@code mcm.staff}. */
    boolean isStaff(Player p) {
        if (p == null) return false;
        if (scoring.isMarkedSpectator(p.getUniqueId()) || staffByName(p.getName())) return true;
        return p.hasPermission("mcm.staff");
    }

    private boolean staffByName(String name) {
        if (name == null) return false;
        for (String n : getConfig().getStringList("staff")) if (n.equalsIgnoreCase(name)) return true;
        return false;
    }

    /** A player who takes part in games: not staff, and during a championship on a team. Everyone else spectates. */
    boolean isEventPlayer(Player p) {
        return p != null && !isStaff(p) && !(championshipMode && scoring.getTeam(p.getUniqueId()) == null);
    }

    /** Teams cannot change while a championship runs; ops use /mcm team set. */
    private boolean teamsLocked() { return championshipMode; }

    private void tellTeamsLocked(CommandSender s) {
        s.sendMessage(ChatColor.RED + "Teams are locked during the championship." + (s.isOp() ? ChatColor.GRAY + " Ops: /mcm team set <player> <team|none>." : ""));
    }

    private void refreshTeamViews() {
        scoring.applyTabNames();
        refreshLobbyItems();
        if (hub != null) hub.refreshDisplays();
    }

    private boolean setTeamSize(int size) {
        if (teamsLocked()) { for (Player op : Bukkit.getOnlinePlayers()) if (op.isOp()) tellTeamsLocked(op); return false; }
        scoring.setMaxTeamSize(size);
        getConfig().set("teams.size", scoring.getMaxTeamSize());
        saveConfig();
        broadcast(ChatColor.AQUA + "Team Size set to " + ChatColor.YELLOW + scoring.getTeamSizeLabel()
                + ChatColor.AQUA + "! Step onto an East Team Booth, right-click your Wool, or type /team to join.");
        refreshTeamViews();
        return true;
    }

    private void randomizeTeams() {
        if (teamsLocked()) { for (Player op : Bukkit.getOnlinePlayers()) if (op.isOp()) tellTeamsLocked(op); return; }
        List<Player> eligible = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) if (!isStaff(p)) eligible.add(p);
        int left = scoring.autoBalance(eligible);
        broadcast(ChatColor.AQUA + "Auto-balanced " + eligible.size() + " players into " + ChatColor.YELLOW + scoring.getActiveTeams()
                + ChatColor.AQUA + " teams of up to " + ChatColor.YELLOW + scoring.getMaxTeamSize() + ChatColor.AQUA + "!");
        if (left > 0) broadcast(ChatColor.RED + "" + left + " player(s) did not fit (" + scoring.getActiveTeams() + " teams x " + scoring.getMaxTeamSize()
                + ") and stay unteamed - raise teams.count / use /mcm team count.");
        refreshTeamViews();
    }

    /** /mcm teamsize, /mcm team ..., /mcm spectate ... (ops and console). */
    private void teamAdminCommand(CommandSender s, String[] args) {
        String head = args[0].toLowerCase(Locale.ROOT);
        if (head.equals("teamsize")) {
            if (args.length < 2) { s.sendMessage(ChatColor.YELLOW + "Team size is " + scoring.getMaxTeamSize() + " (teams.size). Usage: /mcm teamsize <1-5>"); return; }
            try { setTeamSize(Integer.parseInt(args[1])); }
            catch (NumberFormatException ex) { s.sendMessage(ChatColor.YELLOW + "Usage: /mcm teamsize <1-5>"); }
            return;
        }
        if (head.equals("spectate")) { spectateCommand(s, args); return; }
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "list";
        switch (sub) {
            case "balance" -> randomizeTeams();
            case "clear" -> {
                if (teamsLocked()) { tellTeamsLocked(s); return; }
                scoring.clearTeams();
                broadcast(ChatColor.YELLOW + "All teams were cleared.");
                refreshTeamViews();
            }
            case "count" -> {
                if (args.length < 3) { s.sendMessage(ChatColor.YELLOW + "Teams in use: " + scoring.getActiveTeams() + ". Usage: /mcm team count <1-10>"); return; }
                if (teamsLocked()) { tellTeamsLocked(s); return; }
                int n;
                try { n = Integer.parseInt(args[2]); } catch (NumberFormatException ex) { s.sendMessage(ChatColor.YELLOW + "Usage: /mcm team count <1-10>"); return; }
                scoring.setActiveTeams(n);
                getConfig().set("teams.count", scoring.getActiveTeams());
                saveConfig();
                int freed = 0;
                for (int i = scoring.getActiveTeams(); i < TeamScoring.TEAM_NAMES.length; i++) {
                    for (UUID u : scoring.registeredMembers(TeamScoring.TEAM_NAMES[i])) { scoring.setTeam(u, null); freed++; }
                }
                broadcast(ChatColor.AQUA + "Teams in use: " + ChatColor.YELLOW + scoring.getActiveTeams() + ChatColor.AQUA + "."
                        + (freed > 0 ? ChatColor.GRAY + " " + freed + " player(s) of closed teams are unteamed." : ""));
                refreshTeamViews();
            }
            case "add" -> {
                // /mcm team add <player> [team]: fill a partly filled team, best team when no team is named
                if (args.length < 3) { s.sendMessage(ChatColor.YELLOW + "Usage: /mcm team add <player> [team]"); return; }
                UUID id = scoring.findByName(args[2]);
                if (id == null) { s.sendMessage(ChatColor.RED + "Unknown player: " + args[2] + " (they must have joined once)."); return; }
                String name = scoring.nameOf(id);
                Player online = Bukkit.getPlayer(id);
                if (scoring.isMarkedSpectator(id) || (online != null && isStaff(online))) {
                    s.sendMessage(ChatColor.RED + name + " is staff/spectator (/mcm spectate off first).");
                    return;
                }
                String existing = scoring.getTeam(id);
                if (args.length <= 3 && existing != null) {
                    s.sendMessage(ChatColor.YELLOW + name + " is already on " + scoring.teamColor(existing) + existing + " Team"
                            + ChatColor.YELLOW + " (" + scoring.registeredCount(existing) + "/" + scoring.getMaxTeamSize() + ").");
                    return;
                }
                String team = null;
                if (args.length > 3) {
                    for (String t : TeamScoring.TEAM_NAMES) if (t.equalsIgnoreCase(args[3])) team = t;
                    if (team == null || !scoring.isTeamActive(team)) {
                        s.sendMessage(ChatColor.RED + "Unknown or inactive team: " + args[3] + ". Teams in use: "
                                + String.join(", ", Arrays.copyOf(TeamScoring.TEAM_NAMES, scoring.getActiveTeams())));
                        return;
                    }
                } else {
                    team = scoring.bestTeamFor(id);
                    if (team == null) {
                        s.sendMessage(ChatColor.RED + "Every team is full (" + scoring.getActiveTeams() + " x " + scoring.getMaxTeamSize()
                                + ", registered online or not). Remove someone or raise teams.count.");
                        return;
                    }
                }
                if (!scoring.trySetTeam(id, team)) {
                    s.sendMessage(ChatColor.RED + team + " is full (" + scoring.registeredCount(team) + "/" + scoring.getMaxTeamSize()
                            + " registered, online or not). Use /mcm team add " + name + " for the best partial team.");
                    return;
                }
                s.sendMessage(ChatColor.GREEN + name + " is now on " + scoring.teamColor(team) + team + " Team" + ChatColor.GREEN + " ("
                        + scoring.registeredCount(team) + "/" + scoring.getMaxTeamSize() + ").");
                if (online != null) online.sendMessage(ChatColor.YELLOW + "An admin put you on " + scoring.teamColor(team) + team + " Team" + ChatColor.YELLOW + ".");
                refreshTeamViews();
            }
            case "set" -> {
                if (args.length < 4) { s.sendMessage(ChatColor.YELLOW + "Usage: /mcm team set <player> <team|none> [force]"); return; }
                UUID id = scoring.findByName(args[2]);
                if (id == null) { s.sendMessage(ChatColor.RED + "Unknown player: " + args[2] + " (they must have joined once)."); return; }
                String name = scoring.nameOf(id);
                Player online = Bukkit.getPlayer(id);
                boolean force = args.length > 4 && args[4].equalsIgnoreCase("force");
                if (args[3].equalsIgnoreCase("none") || args[3].equalsIgnoreCase("solo")) {
                    scoring.setTeam(id, null);
                    s.sendMessage(ChatColor.GREEN + name + " has no team now.");
                } else {
                    String team = null;
                    for (String t : TeamScoring.TEAM_NAMES) if (t.equalsIgnoreCase(args[3])) team = t;
                    if (team == null || (!scoring.isTeamActive(team) && !force)) {
                        s.sendMessage(ChatColor.RED + "Unknown or inactive team: " + args[3] + ". Teams in use: "
                                + String.join(", ", Arrays.copyOf(TeamScoring.TEAM_NAMES, scoring.getActiveTeams())));
                        return;
                    }
                    if (scoring.isMarkedSpectator(id) || (online != null && isStaff(online))) {
                        s.sendMessage(ChatColor.RED + name + " is staff/spectator (/mcm spectate off first).");
                        return;
                    }
                    if (!force && !scoring.trySetTeam(id, team)) {
                        s.sendMessage(ChatColor.RED + team + " is full (" + scoring.registeredCount(team) + "/" + scoring.getMaxTeamSize()
                                + " registered, online or not). Remove someone first or add 'force'.");
                        return;
                    }
                    if (force) scoring.setTeam(id, team);
                    s.sendMessage(ChatColor.GREEN + name + " is now on " + scoring.teamColor(team) + team + " Team" + ChatColor.GREEN + " (" + scoring.registeredCount(team) + "/" + scoring.getMaxTeamSize() + ").");
                    if (online != null) online.sendMessage(ChatColor.YELLOW + "An admin put you on " + scoring.teamColor(team) + team + " Team" + ChatColor.YELLOW + ".");
                }
                refreshTeamViews();
            }
            default -> {
                s.sendMessage(ChatColor.GOLD + "Teams: " + scoring.getActiveTeams() + " x " + scoring.getMaxTeamSize()
                        + ChatColor.GRAY + " (registered, online or not)" + (championshipMode ? ChatColor.RED + "  LOCKED (championship)" : ""));
                for (int i = 0; i < scoring.getActiveTeams(); i++) {
                    String t = TeamScoring.TEAM_NAMES[i];
                    StringBuilder sb = new StringBuilder();
                    for (UUID u : scoring.registeredMembers(t)) {
                        if (sb.length() > 0) sb.append(ChatColor.GRAY).append(", ");
                        sb.append(Bukkit.getPlayer(u) != null ? ChatColor.WHITE : ChatColor.DARK_GRAY).append(scoring.nameOf(u));
                    }
                    s.sendMessage(scoring.teamColor(t) + t + ChatColor.GRAY + " " + scoring.registeredCount(t) + "/" + scoring.getMaxTeamSize()
                            + "  " + (sb.length() == 0 ? ChatColor.DARK_GRAY + "-" : sb.toString()));
                }
                List<String> sp = new ArrayList<>();
                for (Player p : Bukkit.getOnlinePlayers()) if (isStaff(p)) sp.add(p.getName());
                if (!sp.isEmpty()) s.sendMessage(ChatColor.GRAY + "Staff/spectators online: " + ChatColor.WHITE + String.join(", ", sp));
            }
        }
    }

    private void spectateCommand(CommandSender s, String[] args) {
        String mode = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "toggle";
        if (mode.equals("list")) {
            List<String> names = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (isStaff(p)) names.add(p.getName());
            s.sendMessage(ChatColor.GRAY + "Staff/spectators online: " + ChatColor.WHITE + (names.isEmpty() ? "-" : String.join(", ", names)));
            return;
        }
        Player target = args.length > 2 ? Bukkit.getPlayerExact(args[2]) : (s instanceof Player pl ? pl : null);
        if (target == null) { s.sendMessage(ChatColor.YELLOW + "Usage: /mcm spectate [on|off|list] [player]"); return; }
        boolean on = switch (mode) { case "on" -> true; case "off" -> false; default -> !scoring.isMarkedSpectator(target.getUniqueId()); };
        scoring.setMarkedSpectator(target.getUniqueId(), on);
        if (on) {
            scoring.setTeam(target.getUniqueId(), null);
            externalParticipants.remove(target.getUniqueId());
            alivePlayers.remove(target.getUniqueId());
            target.sendMessage(ChatColor.AQUA + "You are a spectator/staff now: no team, no game side. " + ChatColor.GRAY + "(/mcm spectate off to play)");
            if (currentGame == GameType.EXTERNAL && activeExternal != null) {
                McmSpectator.Zone z = externalSpectatorZone();
                McmSpectator.makeSpectator(target, z != null ? z.home().clone() : null);
                coreSpectators.add(target.getUniqueId());
            }
        } else {
            coreSpectators.remove(target.getUniqueId());
            target.sendMessage(ChatColor.AQUA + "You are a player again. " + ChatColor.GRAY + "Pick a team with /team"
                    + (isStaff(target) ? ChatColor.YELLOW + " (still staff through config 'staff' or the mcm.staff permission)" : ""));
        }
        if (s != target) s.sendMessage(ChatColor.GREEN + target.getName() + (on ? " is a spectator now." : " is a player again."));
        refreshTeamViews();
    }

    // ==================== COMPASS MENUS (spaced layout, click actions by slot) ====================

    /** What each slot of the open compass menu does, per viewer; filled when the menu is built. */
    private final Map<UUID, Map<Integer, Runnable>> menuActions = new HashMap<>();

    /** One menu entry: an item and what clicking it does (null = display only). */
    private record MenuEntry(ItemStack item, Runnable action) { }

    /** Columns for k entries in one row, a gap between each and centred: 1 -> 4, 2 -> 3 5, 3 -> 2 4 6, 4 -> 1 3 5 7. */
    private static int[] spacedColumns(int k) {
        return switch (k) {
            case 1 -> new int[]{4};
            case 2 -> new int[]{3, 5};
            case 3 -> new int[]{2, 4, 6};
            case 4 -> new int[]{1, 3, 5, 7};
            default -> java.util.Arrays.copyOf(new int[]{1, 2, 3, 4, 5, 6, 7}, Math.min(k, 7));
        };
    }

    /**
     * Header row (slot 4 = {@code header}), the games in spaced rows of four, then the control bar ({@code controls}
     * indexed by column, null = filler). Everything else is a dark pane, so the games read as a clean grid.
     */
    private void openMenu(Player p, String title, MenuEntry header, List<MenuEntry> games, MenuEntry[] controls) {
        int perRow = games.size() > 16 ? 7 : 4;
        int rows = Math.max(1, (games.size() + perRow - 1) / perRow);
        if (rows > 4) { rows = 4; games = games.subList(0, 4 * perRow); }
        int size = 9 * (rows + 2);
        Inventory inv = Bukkit.createInventory(null, size, title);
        Map<Integer, Runnable> acts = new HashMap<>();
        ItemStack dark = makeItem(Material.BLACK_STAINED_GLASS_PANE, " ");
        ItemStack light = makeItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int s = 0; s < size; s++) inv.setItem(s, s < 9 || s >= size - 9 ? dark : light);
        if (header != null) { inv.setItem(4, header.item()); if (header.action() != null) acts.put(4, header.action()); }
        for (int r = 0; r < rows; r++) {
            int from = r * perRow, to = Math.min(games.size(), from + perRow);
            int[] cols = spacedColumns(to - from);
            for (int i = from; i < to; i++) {
                int slot = 9 * (r + 1) + cols[i - from];
                inv.setItem(slot, games.get(i).item());
                if (games.get(i).action() != null) acts.put(slot, games.get(i).action());
            }
        }
        for (int c = 0; c < 9 && c < controls.length; c++) {
            if (controls[c] == null) continue;
            int slot = size - 9 + c;
            inv.setItem(slot, controls[c].item());
            if (controls[c].action() != null) acts.put(slot, controls[c].action());
        }
        menuActions.put(p.getUniqueId(), acts);
        p.openInventory(inv);
    }

    private MenuEntry builtIn(Player p, Material icon, String name, String line, GameType type) {
        return new MenuEntry(makeItem(icon, ChatColor.YELLOW + "" + ChatColor.BOLD + name, ChatColor.GRAY + line, "",
                ChatColor.GREEN + "▶ Click to play"), () -> { p.closeInventory(); championshipMode = false; startEvent(type); });
    }

    private MenuEntry hubTravelEntry(Player p) {
        if (hub == null) return new MenuEntry(makeItem(Material.COMMAND_BLOCK, ChatColor.DARK_AQUA + "" + ChatColor.BOLD + "More MCC Games (" + mccGames().size() + ")",
                ChatColor.GRAY + "Play an installed MCC game on its own"), () -> openExtraGamesGui(p));
        return new MenuEntry(makeItem(Material.ENDER_EYE, ChatColor.GOLD + "" + ChatColor.BOLD + "MCC Lobby",
                ChatColor.GRAY + "Travel to the Minecraft Championships hub", hubReady() ? ChatColor.YELLOW + "Click to teleport" : ChatColor.RED + "Hub is " + hub.status()),
                () -> travelToHub(p));
    }

    private void openAdminGui(Player p) {
        List<MenuEntry> games = new ArrayList<>();
        games.add(builtIn(p, Material.IRON_SWORD, "Survival Games", "Blitz SG • SG4 & Kalas, refill, deathmatch", GameType.SURVIVAL_GAMES));
        games.add(builtIn(p, Material.FEATHER, "Parkour Spiral", "Hielke's parkour spiral tower", GameType.PARKOUR));
        games.add(builtIn(p, Material.WATER_BUCKET, "Multi-Stage Dropper", "5 shafts in Delightful Dropper 2", GameType.DROPPER));
        games.add(builtIn(p, Material.TNT, "TNT Run", "Best of 3 • double jump", GameType.TNT_RUN));
        games.add(builtIn(p, Material.GOLDEN_SHOVEL, "Spleef / Splegg", "Best of 3 • instamine + snowballs", GameType.SPLEEF));
        games.add(builtIn(p, Material.DIAMOND_CHESTPLATE, "Battle Arena", "Best of 2 • four PvP kits", GameType.KIT_PVP));
        games.add(builtIn(p, Material.GOLDEN_APPLE, "Speed Survival", "15 min • CutClean, Timber, supply drops", GameType.SURVIVAL));
        games.add(builtIn(p, Material.BOW, "One Shot", "Pink Valley • not in the championship", GameType.ONE_SHOT));
        for (McmGame g : mmExternalGames()) {
            games.add(new MenuEntry(makeItem(g.icon(), ChatColor.YELLOW + "" + ChatColor.BOLD + g.displayName(),
                    ChatColor.GRAY + (g.description().isEmpty() ? "Minecraft Monday game" : g.description()), "",
                    ChatColor.GREEN + "▶ Click to play"), () -> pickMapThenStart(p, g.id())));
        }
        MenuEntry header = new MenuEntry(makeItem(Material.NETHER_STAR, ChatColor.GOLD + "" + ChatColor.BOLD + "★ START FULL CHAMPIONSHIP ★",
                ChatColor.GRAY + "Plays all " + buildPlaylist().size() + " events back-to-back", ChatColor.YELLOW + "Click to start"),
                () -> { p.closeInventory(); startChampionship(false); });
        boolean fastOn = McmApi.isFastTest();
        MenuEntry[] controls = {
                hubTravelEntry(p),
                new MenuEntry(makeItem(Material.REPEATER, ChatColor.AQUA + "" + ChatColor.BOLD + "Team Size: " + ChatColor.YELLOW + scoring.getTeamSizeLabel(),
                        ChatColor.GRAY + "Click to cycle: 1 → 5"), () -> { setTeamSize((scoring.getMaxTeamSize() % 5) + 1); openAdminGui(p); }),
                new MenuEntry(makeItem(Material.RED_WOOL, ChatColor.GREEN + "" + ChatColor.BOLD + "Auto-Balance Teams",
                        ChatColor.GRAY + "Teams of " + scoring.getMaxTeamSize()), () -> { p.closeInventory(); randomizeTeams(); }),
                null,
                new MenuEntry(makeItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "STOP / RETURN TO LOBBY", ChatColor.GRAY + "/mcm stop"),
                        () -> { p.closeInventory(); championshipMode = false; returnToLobby(ChatColor.YELLOW + "Returned to lobby."); }),
                null,
                new MenuEntry(makeItem(fastOn ? Material.LIME_DYE : Material.GRAY_DYE,
                        ChatColor.AQUA + "" + ChatColor.BOLD + "Testing: " + (fastOn ? ChatColor.GREEN + "FAST" : ChatColor.RED + "OFF"),
                        ChatColor.GRAY + "Fast test cuts every wait to 1 s (/mcm fast)", "",
                        ChatColor.YELLOW + "Left-click: toggle fast test", ChatColor.YELLOW + "Right-click: skip wait  " + ChatColor.DARK_GRAY + "/mcm skipwait"),
                        () -> { if (lastMenuClickRight.contains(p.getUniqueId())) { p.closeInventory(); cmdSkipWait(p); } else { cmdFast(p, null); openAdminGui(p); } }),
                new MenuEntry(makeItem(Material.ARROW, ChatColor.GOLD + "" + ChatColor.BOLD + "Skip",
                        ChatColor.YELLOW + "Left-click: skip whole event " + ChatColor.DARK_GRAY + "/mcm next",
                        ChatColor.YELLOW + "Right-click: skip one round " + ChatColor.DARK_GRAY + "/mcm skip"),
                        () -> { p.closeInventory(); if (lastMenuClickRight.contains(p.getUniqueId())) cmdSkipRound(p); else skipCurrentEvent(p); }),
                new MenuEntry(makeItem(Material.CLOCK, ChatColor.YELLOW + "Reset All Scores", ChatColor.GRAY + "Everyone back to 0", ChatColor.RED + "Shift-click to confirm"),
                        () -> { if (!lastMenuClickShift.contains(p.getUniqueId())) return;
                            p.closeInventory(); scoring.resetScores(); broadcast(ChatColor.RED + "Tournament scores reset!"); }),
        };
        openMenu(p, ADMIN_GUI_TITLE, header, games, controls);
    }

    /** Click kind of the menu click being handled (set just before the slot's action runs). */
    private final java.util.Set<UUID> lastMenuClickRight = new java.util.HashSet<>(), lastMenuClickShift = new java.util.HashSet<>();

    private void openTeamGui(Player p) {
        if (teamsLocked()) {
            String mine = scoring.getTeam(p.getUniqueId());
            p.sendMessage(ChatColor.RED + "Teams are locked during the championship."
                    + (mine != null ? ChatColor.GRAY + " You are on " + scoring.teamColor(mine) + mine + " Team" + ChatColor.GRAY + "." : ChatColor.GRAY + " Ask an admin to put you on a team."));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            return;
        }
        Inventory inv = Bukkit.createInventory(null, 27, TEAM_GUI_TITLE);
        int max = scoring.getMaxTeamSize();
        for (int i = 0; i < scoring.getActiveTeams(); i++) {
            String tName = TeamScoring.TEAM_NAMES[i];
            List<UUID> members = scoring.registeredMembers(tName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Slots: " + ChatColor.WHITE + members.size() + " / " + max
                    + ChatColor.GRAY + " | Team Coins: " + ChatColor.GREEN + scoring.getTeamPoints(tName));
            if (members.isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "No players yet");
            } else {
                for (UUID m : members) lore.add(ChatColor.YELLOW + "• " + (Bukkit.getPlayer(m) != null ? ChatColor.WHITE : ChatColor.DARK_GRAY) + scoring.nameOf(m));
            }
            lore.add("");
            lore.add(members.size() >= max && !members.contains(p.getUniqueId()) ? ChatColor.RED + "Team is full!" : ChatColor.GREEN + "Click to join!");
            inv.setItem(TEAM_GUI_SLOTS[i], makeItem(TeamScoring.TEAM_WOOLS[i],
                    scoring.teamColor(tName) + "" + ChatColor.BOLD + McmHud.teamDisplay(tName), lore.toArray(new String[0])));
        }
        inv.setItem(22, makeItem(Material.BARRIER, ChatColor.WHITE + "Leave Team (Play Solo)", ChatColor.GRAY + "Remove yourself from your current team"));
        p.openInventory(inv);
    }

    private void openKitGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 9, KIT_GUI_TITLE);
        inv.setItem(1, makeItem(Material.DIAMOND_SWORD, ChatColor.RED + "" + ChatColor.BOLD + "Knight",
                ChatColor.GRAY + "Full Iron + Diamond Sword + Rod + Bow + 2 Gapples"));
        inv.setItem(3, makeItem(Material.BOW, ChatColor.GREEN + "" + ChatColor.BOLD + "Archer",
                ChatColor.GRAY + "Power II Bow + 24 Arrows + Stone Sword + Rod + Speed I"));
        inv.setItem(5, makeItem(Material.DIAMOND_CHESTPLATE, ChatColor.BLUE + "" + ChatColor.BOLD + "Tank",
                ChatColor.GRAY + "Diamond Chest/Boots + Iron Helm/Legs + Iron Sword + 3 Gapples"));
        inv.setItem(7, makeItem(Material.FEATHER, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Assassin",
                ChatColor.GRAY + "Speed II + Diamond Sword (Sharp I) + Ender Pearl + Rod"));
        p.openInventory(inv);
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        String title = e.getView().getTitle();

        if (title.equals(MAP_GUI_TITLE)) {
            e.setCancelled(true);
            if (e.getCurrentItem() != null && p.isOp()) onMapPickClick(p, e.getRawSlot());
            return;
        }
        if (title.equals(MCC_GUI_TITLE) || title.equals(SELECTOR_GUI_TITLE) || title.equals(ADMIN_GUI_TITLE)) {
            e.setCancelled(true);
            Map<Integer, Runnable> acts = menuActions.get(p.getUniqueId());
            Runnable a = acts == null ? null : acts.get(e.getRawSlot());
            if (a == null) return;
            if (title.equals(ADMIN_GUI_TITLE) && !p.isOp()) return;
            UUID id = p.getUniqueId();
            if (e.isRightClick()) lastMenuClickRight.add(id); else lastMenuClickRight.remove(id);
            if (e.isShiftClick()) lastMenuClickShift.add(id); else lastMenuClickShift.remove(id);
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, 1.2f);
            a.run();
            return;
        }
        if (!title.equals(ADMIN_GUI_TITLE) && !title.equals(TEAM_GUI_TITLE) && !title.equals(KIT_GUI_TITLE) && !title.equals(EXTRA_GUI_TITLE)) {
            if (currentGame == GameType.LOBBY && p.getGameMode() != GameMode.CREATIVE) {
                e.setCancelled(true);
            }
            return;
        }
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        if (title.equals(TEAM_GUI_TITLE)) {
            int slot = e.getRawSlot();
            if (teamsLocked()) { p.closeInventory(); tellTeamsLocked(p); return; }
            if (slot == 22) {
                scoring.setTeam(p.getUniqueId(), null);
                p.sendMessage(ChatColor.YELLOW + "You left your team (now Solo).");
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                if (hub != null) Bukkit.getScheduler().runTask(this, () -> { if (hub != null) hub.refreshDisplays(); });
        scoring.applyTabNames();
                refreshLobbyItems();
                p.closeInventory();
                return;
            }
            for (int i = 0; i < scoring.getActiveTeams(); i++) {
                if (slot == TEAM_GUI_SLOTS[i]) {
                    String tName = TeamScoring.TEAM_NAMES[i];
                    if (tName.equals(scoring.getTeam(p.getUniqueId()))) {
                        p.sendMessage(ChatColor.YELLOW + "You're already on " + scoring.teamColor(tName) + tName + " Team!");
                        p.closeInventory();
                        return;
                    }
                    if (!joinTeam(p, tName)) return;
                    p.closeInventory();
                    return;
                }
            }
            return;
        }

        if (title.equals(KIT_GUI_TITLE)) {
            KitType chosen = switch (e.getRawSlot()) {
                case 1 -> KitType.KNIGHT;
                case 3 -> KitType.ARCHER;
                case 5 -> KitType.TANK;
                case 7 -> KitType.ASSASSIN;
                default -> null;
            };
            if (chosen != null) {
                playerKits.put(p.getUniqueId(), chosen);
                p.closeInventory();
                if (currentGame == GameType.KIT_PVP && startFreezeTicks > 0) {
                    applyKitPvpGear(p, chosen);
                }
                p.sendMessage(ChatColor.GREEN + "Selected Kit: " + ChatColor.GOLD + chosen.display);
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
            }
            return;
        }

        if (title.equals(EXTRA_GUI_TITLE)) {
            if (!p.isOp()) return;
            List<McmGame> list = mccGames();
            int slot = e.getRawSlot();
            if (slot >= 0 && slot < list.size()) {
                pickMapThenStart(p, list.get(slot).id());
            }
            return;
        }

    }

    // ==================== ADMIN TESTING HOOKS (/mcm fast|skipwait|skip|next|map|tools|help) ====================

    /**
     * Op-only feedback probe (no game needed): {@code /mcm cointest <player> kill <victim> [coins] [froze]},
     * {@code ... obj <banner|-> [coins] [reason...]}, {@code ... burst <n> [coins]}, {@code ... add <coins> [reason...]}.
     * Goes through the same TeamScoring/CoinFeedback path as ctx.addCoins/addKillCoins/addObjectiveCoins, without a multiplier.
     */
    private void coinTest(CommandSender sender, String[] args) {
        if (args.length < 3) { sender.sendMessage("Usage: /mcm cointest <player> kill <victim> [coins] [froze] | obj <banner|-> [coins] [reason] | burst <n> [coins] | add <coins> [reason]"); return; }
        Player p = Bukkit.getPlayerExact(args[1]);
        if (p == null) { sender.sendMessage("No such player: " + args[1]); return; }
        scoring.ensurePlayer(p.getUniqueId());
        try {
            switch (args[2].toLowerCase(Locale.ROOT)) {
                case "kill" -> {
                    Player v = args.length > 3 ? Bukkit.getPlayerExact(args[3]) : null;
                    double c = args.length > 4 ? Double.parseDouble(args[4]) : 15;
                    boolean froze = args.length > 5 && args[5].equalsIgnoreCase("froze");
                    String reason = froze ? "Froze " + (args.length > 3 ? args[3] : "?") : "Elimination";
                    scoring.addCoins(p.getUniqueId(), c, reason);
                    CoinFeedback.kill(p, v == null ? null : v.getUniqueId(), args.length > 3 ? args[3] : null,
                            v == null ? null : scoring.getTeam(v.getUniqueId()), reason);
                }
                case "obj" -> {
                    String banner = args.length > 3 && !args[3].equals("-") ? args[3].replace('_', ' ') : null;
                    double c = args.length > 4 ? Double.parseDouble(args[4]) : 0;
                    String reason = args.length > 5 ? String.join(" ", Arrays.copyOfRange(args, 5, args.length)) : "Objective";
                    scoring.addCoins(p.getUniqueId(), c, reason);
                    if (banner != null) CoinFeedback.objective(p, banner, reason);
                }
                case "burst" -> {
                    int n = Integer.parseInt(args[3]);
                    double c = args.length > 4 ? Double.parseDouble(args[4]) : 3;
                    for (int i = 0; i < n; i++) scoring.addCoins(p.getUniqueId(), c, "Burst " + (i + 1));
                }
                default -> scoring.addCoins(p.getUniqueId(), Double.parseDouble(args[3]),
                        args.length > 4 ? String.join(" ", Arrays.copyOfRange(args, 4, args.length)) : "Test");
            }
        } catch (RuntimeException ex) {
            sender.sendMessage("cointest: " + ex);
        }
    }

    private static final List<String> TESTING_SUBS = List.of("fast", "skipwait", "skip", "next", "map", "maps", "tools", "mapedit", "help");

    private boolean isTestingSub(String s) { return TESTING_SUBS.contains(s.toLowerCase(Locale.ROOT)); }

    /** Remember the core wait that is running now so /mcm skipwait can finish it. */
    private void setPendingWait(String label, Runnable finish) {
        pendingWaitLabel = label;
        pendingWaitFinish = finish;
    }

    private void clearPendingWait() {
        pendingWaitLabel = null;
        pendingWaitFinish = null;
    }

    /** "1 second" / "10 seconds" for a tick delay. */
    private static String secondsLabel(long ticks) {
        long s = Math.max(1, Math.round(ticks / 20.0));
        return s + (s == 1 ? " second" : " seconds");
    }

    private void testingCommand(CommandSender sender, String[] args) {
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "fast" -> {
                Boolean on = null;
                if (args.length > 1) {
                    String a = args[1].toLowerCase(Locale.ROOT);
                    if (a.equals("on") || a.equals("true") || a.equals("1")) on = true;
                    else if (a.equals("off") || a.equals("false") || a.equals("0")) on = false;
                    else { sender.sendMessage(ChatColor.YELLOW + "Usage: /mcm fast [on|off]"); return; }
                }
                cmdFast(sender, on);
            }
            case "skipwait" -> cmdSkipWait(sender);
            case "skip" -> cmdSkipRound(sender);
            case "next" -> cmdSkipEvent(sender);
            case "map", "maps" -> mapCommand(sender, args);
            case "mapedit" -> mapEdit.command(sender, args);
            case "tools" -> {
                if (sender instanceof Player pl) adminTools.openMain(pl);
                else sender.sendMessage("The Admin Tools menu is in-game only.");
            }
            default -> sendHelp(sender);
        }
    }

    private void sendHelp(CommandSender s) {
        String y = ChatColor.YELLOW.toString(), g = ChatColor.GRAY.toString();
        s.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "MCM admin commands" + ChatColor.GRAY + " (ops only)");
        s.sendMessage(y + "/mcm" + g + " - Tournament Controller GUI   " + y + "/mcm tools" + g + " - Admin Tools GUI (also the feather)");
        s.sendMessage(y + "/mcm start [mcc]" + g + " - start the MM (or MCC) championship   " + y + "/mcm play <game>" + g + " - play one game");
        s.sendMessage(y + "/mcm fast [on|off]" + g + " - fast-test mode: every lobby/intermission/vote/reveal/countdown wait <= 1 s (saved)");
        s.sendMessage(y + "/mcm skipwait" + g + " - finish the wait that is running now (countdown, vote, intermission, reveal)");
        s.sendMessage(y + "/mcm skip" + g + " - skip the current ROUND/MAP only (no coins); the game continues with the next round");
        s.sendMessage(y + "/mcm next" + g + " - skip the WHOLE current game/event and advance the championship");
        s.sendMessage(y + "/mcm map [id]" + g + " - list the running game's maps / force the next map; " + y + "/mcm map <game> [id]" + g + " before starting");
        s.sendMessage(y + "/mcm mapedit" + g + " - Map Edit: open a game map in creative and save it as an overlay the game applies (" + y + "/mcm mapedit help" + g + ")");
        s.sendMessage(y + "/mcm stop" + g + " - stop & return to lobby   " + y + "/mcm reset" + g + " - reset scores   " + y + "/mcm games" + g + " - list games");
        s.sendMessage(y + "/mcm teamsize <1-5>" + g + ", " + y + "/mcm lobby [tp|mm|rebuild|status]" + g + ", " + y + "/mcm preview ..." + g + ", " + y + "/mcm selftest");
        s.sendMessage(y + "/mcm team list|add <player> [team]|balance|clear|count <n>|set <player> <team|none>" + g + " - teams (set/add also work while /team is locked in a championship)");
        s.sendMessage(y + "/mcm spectate [on|off|list] [player]" + g + " - mark staff/spectators: never on a team, never a game side");
    }

    void cmdFast(CommandSender sender, Boolean on) {
        boolean now = on == null ? !McmApi.isFastTest() : on;
        McmApi.setFastTest(now);
        getConfig().set("fast-test", now);
        saveConfig();
        sender.sendMessage(ChatColor.GOLD + "Fast test: " + (now ? ChatColor.GREEN + "" + ChatColor.BOLD + "ON" + ChatColor.GRAY + " - every core wait is now <= 1 s (saved, survives restarts)."
                : ChatColor.RED + "" + ChatColor.BOLD + "OFF" + ChatColor.GRAY + " - normal waiting times (saved)."));
        if (adminTools != null) adminTools.refreshOpenMenus();
    }

    /** Finish the running wait: a core wait first, then the built-in pre-round freeze, then the game's own skipWait(). */
    void cmdSkipWait(CommandSender sender) {
        String who = sender instanceof Player pl ? pl.getName() : "Console";
        if (pendingTransitionTask != null && pendingWaitFinish != null) {
            String what = pendingWaitLabel == null ? "wait" : pendingWaitLabel;
            Runnable finish = pendingWaitFinish;
            pendingTransitionTask.cancel();
            pendingTransitionTask = null;
            pendingRevealAfter = null;
            clearPendingWait();
            sender.sendMessage(ChatColor.GREEN + "Skipped: " + ChatColor.WHITE + what + ChatColor.GREEN + ".");
            broadcast(ChatColor.GOLD + who + " skipped the " + what + ".");
            finish.run();
            return;
        }
        if (currentGame != GameType.LOBBY && currentGame != GameType.EXTERNAL && startFreezeTicks > 0) {
            releaseStartFreeze();
            sender.sendMessage(ChatColor.GREEN + "Skipped: " + ChatColor.WHITE + "pre-round countdown of " + currentDisplay() + ChatColor.GREEN + ".");
            return;
        }
        if (currentGame == GameType.EXTERNAL && activeExternal != null) {
            McmGame g = activeExternal;
            boolean handled = false;
            try { handled = g.skipWait(); }
            catch (Throwable t) { getLogger().log(Level.WARNING, "skipWait() failed in " + g.id(), t); }
            if (handled) {
                sender.sendMessage(ChatColor.GREEN + "Skipped the current waiting phase of " + ChatColor.WHITE + g.displayName() + ChatColor.GREEN + ".");
                return;
            }
        }
        int n = McmFx.finishRunningCountdowns();
        if (n > 0) {
            sender.sendMessage(ChatColor.GREEN + "Skipped " + n + " running countdown" + (n == 1 ? "" : "s") + ".");
            return;
        }
        sender.sendMessage(ChatColor.YELLOW + "Nothing is waiting right now" + (currentGame == GameType.LOBBY ? "."
                : " (" + currentDisplay() + " is in play; /mcm skip skips the round, /mcm next the whole game)."));
    }

    /** /mcm skip: the current round/map only. */
    void cmdSkipRound(CommandSender sender) {
        String who = sender instanceof Player pl ? pl.getName() : "Console";
        if (currentGame == GameType.LOBBY) {
            if (pendingTransitionTask != null && pendingWaitFinish != null) { cmdSkipWait(sender); return; }
            sender.sendMessage(ChatColor.YELLOW + "No game is running, so there is no round to skip.");
            return;
        }
        if (currentGame == GameType.EXTERNAL) {
            McmGame g = activeExternal;
            boolean handled = false;
            if (g != null) {
                try { handled = g.skipRound(); }
                catch (Throwable t) { getLogger().log(Level.WARNING, "skipRound() failed in " + g.id(), t); }
            }
            if (handled) {
                sender.sendMessage(ChatColor.GREEN + "Skipped the current round/map of " + ChatColor.WHITE + g.displayName() + ChatColor.GREEN + ".");
                broadcast(ChatColor.GOLD + who + " skipped the current round.");
            } else {
                sender.sendMessage(ChatColor.YELLOW + (g == null ? "This game" : g.displayName()) + " has no rounds to skip. "
                        + ChatColor.GRAY + "(/mcm next ends the whole game)");
            }
            return;
        }
        if (maxRounds > 1 && pendingTransitionTask == null) {
            broadcast(ChatColor.GOLD + who + " skipped round " + roundNumber + "/" + maxRounds + " of " + currentDisplay() + ".");
            sender.sendMessage(ChatColor.GREEN + "Skipped round " + roundNumber + "/" + maxRounds + " (no coins for it).");
            skipRoundNoAwards = true;
            endSubRound("Round skipped by admin");
            return;
        }
        if (pendingTransitionTask != null && pendingWaitFinish != null) { cmdSkipWait(sender); return; }
        sender.sendMessage(ChatColor.YELLOW + currentDisplay() + " has no rounds to skip. " + ChatColor.GRAY + "(/mcm next ends the whole game)");
    }

    /** /mcm next: the whole current game/event (the old meaning of /mcm skip). */
    void cmdSkipEvent(CommandSender sender) {
        skipCurrentEvent(sender instanceof Player pl ? pl : null);
        if (!(sender instanceof Player)) sender.sendMessage("Skipped.");
    }

    void cmdStop(CommandSender sender) {
        championshipMode = false;
        returnToLobby(ChatColor.YELLOW + "Tournament stopped by " + (sender instanceof Player pl ? pl.getName() : "Console") + ".");
    }

    /** Id of the running game, or null in the lobby. */
    String runningGameId() {
        if (currentGame == GameType.LOBBY) return null;
        if (currentGame == GameType.EXTERNAL) return activeExternal == null ? null : activeExternal.id();
        return builtInId(currentGame);
    }

    /** What the core is waiting on right now (for the tools menu), or null. */
    String pendingWaitDescription() {
        if (pendingTransitionTask != null && pendingWaitLabel != null) return pendingWaitLabel;
        if (currentGame != GameType.LOBBY && currentGame != GameType.EXTERNAL && startFreezeTicks > 0) return "pre-round countdown";
        return null;
    }

    /** Selectable maps of a game (built-in or MCC), never null. */
    List<String> mapsOf(String gameId) {
        if (gameId == null) return List.of();
        McmGame g = McmApi.game(gameId);
        if (g != null) {
            try { List<String> m = g.maps(); return m == null ? List.of() : new ArrayList<>(m); }
            catch (Throwable t) { getLogger().log(Level.WARNING, "maps() failed in " + g.id(), t); return List.of(); }
        }
        if (parseGame(gameId) == GameType.SURVIVAL_GAMES) return List.of("sg4", "kalas");
        return List.of();
    }

    boolean forceMap(CommandSender sender, String gameId, String mapId) {
        McmGame g = McmApi.game(gameId);
        boolean ok = false;
        if (g != null) {
            try { ok = g.forceNextMap(mapId); }
            catch (Throwable t) { getLogger().log(Level.WARNING, "forceNextMap() failed in " + g.id(), t); }
        } else if (parseGame(gameId) == GameType.SURVIVAL_GAMES && mapsOf(gameId).contains(mapId.toLowerCase(Locale.ROOT))) {
            forcedSgMap = mapId.toLowerCase(Locale.ROOT);
            ok = true;
        }
        String name = McmApi.displayName(gameId);
        if (ok) sender.sendMessage(ChatColor.GREEN + "Next map of " + ChatColor.WHITE + name + ChatColor.GREEN + ": " + ChatColor.AQUA + mapId);
        else if (mapsOf(gameId).isEmpty()) sender.sendMessage(ChatColor.YELLOW + name + " has no selectable maps.");
        else sender.sendMessage(ChatColor.RED + name + " rejected map '" + mapId + "'. Maps: " + String.join(", ", mapsOf(gameId)));
        return ok;
    }

    private boolean isGameId(String s) { return McmApi.game(s) != null || parseGame(s) != null; }

    private String canonicalGameId(String s) {
        McmGame g = McmApi.game(s);
        if (g != null) return g.id().toLowerCase(Locale.ROOT);
        GameType gt = parseGame(s);
        return gt == null ? s.toLowerCase(Locale.ROOT) : builtInId(gt);
    }

    /** /mcm map [id] | /mcm map <game> [id] */
    private void mapCommand(CommandSender sender, String[] args) {
        String running = runningGameId();
        if (args.length == 1) {
            if (running == null) {
                sender.sendMessage(ChatColor.YELLOW + "No game is running. /mcm map <game> lists a game's maps, /mcm map <game> <id> picks one before starting.");
                return;
            }
            listMaps(sender, running);
            return;
        }
        String a = args[1];
        if (args.length >= 3) {
            if (!isGameId(a)) { sender.sendMessage(ChatColor.RED + "Unknown game '" + a + "'. Options: " + String.join(", ", allGameIds())); return; }
            forceMap(sender, canonicalGameId(a), args[2]);
            return;
        }
        if (running != null && mapsOf(running).stream().anyMatch(m -> m.equalsIgnoreCase(a))) { forceMap(sender, running, a); return; }
        if (isGameId(a)) { listMaps(sender, canonicalGameId(a)); return; }
        if (running != null) { forceMap(sender, running, a); return; }
        sender.sendMessage(ChatColor.RED + "No game is running and '" + a + "' is not a game id. Usage: /mcm map <game> [id]");
    }

    private void listMaps(CommandSender sender, String gameId) {
        List<String> maps = mapsOf(gameId);
        String name = McmApi.displayName(gameId);
        if (maps.isEmpty()) { sender.sendMessage(ChatColor.YELLOW + name + " has no selectable maps."); return; }
        boolean runningNow = gameId.equalsIgnoreCase(String.valueOf(runningGameId()));
        sender.sendMessage(ChatColor.GOLD + name + " maps: " + ChatColor.AQUA + String.join(ChatColor.GRAY + ", " + ChatColor.AQUA, maps));
        sender.sendMessage(ChatColor.GRAY + (runningNow ? "/mcm map <id>" : "/mcm map " + gameId + " <id>") + " = play that map next");
    }

    List<String> gameIds() { return allGameIds(); }

    Material gameIcon(String id) {
        McmGame g = McmApi.game(id);
        if (g != null) { try { return g.icon(); } catch (Throwable t) { return Material.COMMAND_BLOCK; } }
        GameType gt = parseGame(id);
        return gt == null ? Material.PAPER : gt.icon;
    }

    /** Start one game from the Admin Tools menu (same as /mcm play). */
    void adminStart(Player p, String id) {
        championshipMode = false;
        stopExternalIfActive();
        if (!startById(id)) p.sendMessage(ChatColor.RED + "Unknown game. Options: " + String.join(", ", allGameIds()));
    }

    boolean hasHub() { return hub != null; }
    void goHub(Player p) { travelToHub(p); }
    void goMm(Player p) { travelToMm(p); }
    void openController(Player p) { openAdminGui(p); }
    boolean adminToolsItemEnabled() { return getConfig().getBoolean("admin-tools-item", true); }
    /** True while an MCC/external game runs (its own item handlers may react to any feather). */
    boolean externalGameRunning() { return currentGame == GameType.EXTERNAL && activeExternal != null; }
    /** True while a game listed in admin-tools-hidden-in-games runs (it scans inventories, e.g. Bingo counts feathers). */
    boolean toolHiddenForRunningGame() {
        String id = externalGameRunning() ? activeExternal.id() : null;
        if (id == null) return false;
        for (String s : getConfig().getStringList("admin-tools-hidden-in-games")) if (s.equalsIgnoreCase(id)) return true;
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("mcm")) return List.of();
        if (sender instanceof Player pl && !pl.isOp()) return List.of(); // admin commands are never offered to players
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            out.addAll(List.of("start", "play", "fast", "skipwait", "skip", "next", "map", "mapedit", "tools", "stop", "reset", "games",
                    "teamsize", "team", "spectate", "lobby", "preview", "selftest", "help"));
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "team" -> out.addAll(List.of("list", "add", "set", "balance", "clear", "count"));
                case "spectate" -> out.addAll(List.of("on", "off", "list"));
                case "fast" -> out.addAll(List.of("on", "off"));
                case "start", "championship" -> out.add("mcc");
                case "play" -> out.addAll(allGameIds());
                case "teamsize" -> out.addAll(List.of("1", "2", "3", "4", "5"));
                case "lobby", "hub" -> out.addAll(List.of("tp", "mm", "rebuild", "status"));
                case "mapedit" -> { if (mapEdit != null) return mapEdit.tab(sender, args); }
                case "map", "maps" -> {
                    String running = runningGameId();
                    if (running != null) out.addAll(mapsOf(running));
                    out.addAll(allGameIds());
                }
                default -> { }
            }
        } else if (args.length >= 2 && args[0].equalsIgnoreCase("mapedit") && mapEdit != null) {
            return mapEdit.tab(sender, args);
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("map") || args[0].equalsIgnoreCase("maps")) && isGameId(args[1])) {
            out.addAll(mapsOf(canonicalGameId(args[1])));
        } else if (args.length == 3 && args[0].equalsIgnoreCase("team") && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("add"))) {
            for (Player op : Bukkit.getOnlinePlayers()) out.add(op.getName());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("team")
                && (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("add"))) {
            out.add("none");
            for (int i = 0; i < scoring.getActiveTeams(); i++) out.add(TeamScoring.TEAM_NAMES[i]);
        } else if (args.length == 3 && args[0].equalsIgnoreCase("spectate")) {
            for (Player op : Bukkit.getOnlinePlayers()) out.add(op.getName());
        }
        String pre = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        out.removeIf(s -> !s.toLowerCase(Locale.ROOT).startsWith(pre));
        return out;
    }

    // ==================== CHAMPIONSHIP & MULTI-ROUND FLOW ====================

    /** {@code mcc}: MCC championship (MCC games only, MCC hub as lobby, votes in the dome); else the MM championship. */
    private void startChampionship(boolean mcc) {
        playlist.clear();
        playlist.addAll(mcc ? buildMccPlaylist() : buildPlaylist());
        if (playlist.isEmpty()) { broadcast(ChatColor.RED + (mcc ? "MCC championship playlist is empty (no MCC games installed?)!" : "Championship playlist is empty!")); return; }
        getLogger().info("Championship playlist: " + playlist);
        championshipMode = true;
        mccContext = mcc;
        playlistIndex = 0;
        declaredChampionSide = null;
        scoring.resetScores();
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + (mcc ? "=== MINECRAFT CHAMPIONSHIPS (MCC) STARTING! ===" : "=== MINECRAFT MONDAY CHAMPIONSHIP STARTING! ==="));
        broadcast(ChatColor.YELLOW + "" + playlist.size() + " Events | Holistic Team Split Scoring | Highest Team Total Wins!");
        startById(playlist.get(playlistIndex));
    }

    private void startEvent(GameType game) {
        if (mapEdit != null) mapEdit.evictAll("A game is starting.");
        if (!championshipMode) mccContext = false;
        snapshotGamePoints();
        scoring.snapshotTeamSizes();
        roundNumber = 1;
        maxRounds = switch (game) {
            case TNT_RUN, SPLEEF -> 3;
            case KIT_PVP -> 2;
            default -> 1;
        };
        startSubRound(game);
    }

    private void startSubRound(GameType game) {
        cancelTasks();
        restoreArenas();
        currentGame = game;
        coreSpectators.clear();
        builtInZone = null;
        alivePlayers.clear();
        finishOrder.clear();
        roundKills.clear();
        killstreaks.clear();
        parkourCheckpoint.clear();
        visitedPlates.clear();
        maxParkourY.clear();
        climbedPastWater.clear();
        scheduledTntKeys.clear();
        doubleJumpsLeft.clear();
        spleggCooldowns.clear();
        spleggProjectiles.clear();
        dropperAdvanceCooldown.clear();
        gracePeriod = false;
        blitzDropped = false;
        sgRefilled = false;
        sgDeathmatch = false;
        survivalDrop1 = false;
        survivalDrop2 = false;
        survivalDeathmatch = false;
        survivalSupplyChests.clear();
        startFreezeTicks = Waits.seconds((game == GameType.KIT_PVP) ? 4 : 3); // pre-round freeze; fast-test: 1 s

        if (sgWorld != null) sgWorld.getWorldBorder().reset();
        if (sg4World != null) sg4World.getWorldBorder().reset();
        if (oneshotWorld != null) oneshotWorld.getWorldBorder().reset();
        if (survivalWorld != null && game != GameType.SURVIVAL) setSurvivalWorldActive(false);

        for (Player p : Bukkit.getOnlinePlayers()) {
            scoring.ensurePlayer(p.getUniqueId());
            alivePlayers.add(p.getUniqueId());
            roundKills.put(p.getUniqueId(), 0);
            killstreaks.put(p.getUniqueId(), 0);
            resetPlayer(p, GameMode.SURVIVAL);
        }

        if (roundNumber == 1 && Bukkit.getOnlinePlayers().size() >= 2 && scoring.isOneSideRemaining(alivePlayers)
                && game != GameType.PARKOUR && game != GameType.DROPPER) {
            broadcast(ChatColor.YELLOW + "Heads up: everyone is on the same team! Use /team in the lobby to pick different teams for PvP.");
        }

        String roundTag = maxRounds > 1 ? ChatColor.GRAY + " (Round " + roundNumber + "/" + maxRounds + ")" : "";
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + ">>> " + ChatColor.WHITE + game.display + roundTag + ChatColor.GOLD + " <<<");

        switch (game) {
            case SURVIVAL_GAMES -> setupSurvivalGames();
            case PARKOUR -> setupParkour();
            case DROPPER -> setupDropper();
            case TNT_RUN -> setupTntRun();
            case SPLEEF -> setupSpleef();
            case KIT_PVP -> setupKitPvp();
            case ONE_SHOT -> setupOneShot();
            case SURVIVAL -> setupSurvival();
            default -> returnToLobby("");
        }
        if (currentGame == game) builtInZone = computeBuiltInZone(game);

        totalDuration = Math.max(1, timeLeft);
        for (Player p : Bukkit.getOnlinePlayers()) {
            String sub = game == GameType.KIT_PVP ? ChatColor.YELLOW + "Pick your Kit (" + startFreezeTicks + "s)!"
                    : (maxRounds > 1 ? ChatColor.YELLOW + "Round " + roundNumber + " of " + maxRounds : ChatColor.YELLOW + "Get Ready!");
            if (McmApi.isFastTest()) p.sendTitle(ChatColor.GOLD + game.display, sub, 2, 14, 4);
            else p.sendTitle(ChatColor.GOLD + game.display, sub, 5, 45, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.2f);
        }
        startGameLoop();
        if (game == GameType.TNT_RUN || game == GameType.SPLEEF) {
            startFastHazardLoop();
        }
    }

    private void startFastHazardLoop() {
        fastHazardTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentGame != GameType.TNT_RUN && currentGame != GameType.SPLEEF) {
                    cancel();
                    return;
                }
                for (UUID u : new ArrayList<>(alivePlayers)) {
                    Player p = Bukkit.getPlayer(u);
                    if (p == null || p.getGameMode() == GameMode.SPECTATOR) continue;
                    Location loc = p.getLocation();
                    Material feet = loc.getBlock().getType();
                    Material below = loc.clone().subtract(0, 0.3, 0).getBlock().getType();
                    if (currentGame == GameType.TNT_RUN) {
                        int voidY = roundNumber == 1 ? 0 : -1;
                        if (loc.getY() < voidY || feet == Material.LAVA || below == Material.LAVA || p.getFireTicks() > 0) {
                            eliminate(p, null);
                        }
                    } else if (currentGame == GameType.SPLEEF) {
                        if (loc.getY() < 69 || feet == Material.LAVA || below == Material.LAVA || p.getFireTicks() > 0) {
                            eliminate(p, null);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 4L, 4L);
    }

    private void startGameLoop() {
        gameLoopTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (currentGame == GameType.LOBBY) { cancel(); return; }
                if (startFreezeTicks > 0) {
                    startFreezeTicks--;
                    if (startFreezeTicks == 0) releaseStartFreeze();
                }
                timeLeft--;

                // Survival Games timed phases (240s total)
                if (currentGame == GameType.SURVIVAL_GAMES) {
                    if (gracePeriod && timeLeft == 225) {
                        gracePeriod = false;
                        broadcast(ChatColor.RED + "" + ChatColor.BOLD + "Grace period over! PvP is ENABLED! Player Tracker Compass active!");
                        for (UUID u : alivePlayers) {
                            Player p = Bukkit.getPlayer(u);
                            if (p != null) {
                                p.getInventory().addItem(makeItem(Material.COMPASS, ChatColor.AQUA + "Player Tracker", ChatColor.GRAY + "Points to nearest enemy"));
                            }
                        }
                    }
                    if (!gracePeriod && timeLeft % 2 == 0) {
                        updateAllTrackers();
                    }
                    if (!blitzDropped && timeLeft <= 165) {
                        blitzDropped = true;
                        dropBlitzStar();
                    }
                    if (!sgRefilled && timeLeft <= 120) {
                        sgRefilled = true;
                        refillSgChests(true);
                        broadcast(ChatColor.AQUA + "" + ChatColor.BOLD + "★ ALL CHESTS HAVE BEEN REFILLED WITH UPGRADED LOOT! ★");
                    }
                    if (!sgDeathmatch && timeLeft <= 60) {
                        sgDeathmatch = true;
                        triggerSgDeathmatch();
                    }
                }

                // Speed Survival 15-minute (900s) phase engine
                if (currentGame == GameType.SURVIVAL) {
                    if (timeLeft == 885) {
                        broadcast(ChatColor.YELLOW + "Drop-in invulnerability ended! Mobs & fall damage live (PvP still OFF until 5:00).");
                    }
                    if (timeLeft == 660) {
                        broadcast(ChatColor.YELLOW + "⏰ 1 minute until PvP & World Border shrink!");
                    }
                    if (timeLeft == 610) {
                        broadcast(ChatColor.RED + "⚔ PvP & Border Shrink in 10 seconds!");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
                        }
                    }
                    if (gracePeriod && timeLeft <= 600) {
                        gracePeriod = false;
                        if (survivalWorld != null) {
                            survivalWorld.getWorldBorder().setSize(60.0, 420);
                        }
                        broadcast(ChatColor.RED + "" + ChatColor.BOLD + "⚔ GRACE OVER! PvP ENABLED & BORDER SHRINKING TO 60x60! TRACKER ACTIVE! ⚔");
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            p.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "PvP ENABLED!",
                                    ChatColor.YELLOW + "Border shrinking to 60x60! Tracker active!", 5, 50, 10);
                            p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.7f, 1.1f);
                        }
                    }
                    if (!gracePeriod && timeLeft % 2 == 0) {
                        updateAllTrackers();
                    }
                    if (!survivalDrop1 && timeLeft <= 480) {
                        survivalDrop1 = true;
                        spawnSurvivalSupplyDrop(false);
                    }
                    if (!survivalDrop2 && timeLeft <= 300) {
                        survivalDrop2 = true;
                        spawnSurvivalSupplyDrop(true);
                    }
                    if (!survivalDeathmatch && timeLeft <= 180) {
                        survivalDeathmatch = true;
                        triggerSurvivalDeathmatch();
                    }
                }

                if (timeLeft <= 0) {
                    endSubRound("Time's up!");
                    cancel();
                }
            }
        }.runTaskTimer(this, 20L, 20L);
    }

    /** End of the built-in pre-round freeze ("GO!"); KitPvP equips the chosen kits. */
    private void releaseStartFreeze() {
        startFreezeTicks = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (currentGame == GameType.KIT_PVP) {
                KitType k = playerKits.getOrDefault(p.getUniqueId(), KitType.KNIGHT);
                applyKitPvpGear(p, k); // removes kit book, equips chosen kit cleanly
            }
            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GO!", "", 0, 20, 5);
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
        }
    }

    private void endSubRound(String reason) {
        if (gameLoopTask != null) { gameLoopTask.cancel(); gameLoopTask = null; }
        restoreArenas();

        // Award end-of-round bonuses (none when the admin skipped the round with /mcm skip)
        boolean noAwards = skipRoundNoAwards;
        skipRoundNoAwards = false;
        if (noAwards) {
            // skipped round: no points
        } else if (currentGame == GameType.ONE_SHOT) {
            List<Map.Entry<UUID, Integer>> sorted = roundKills.entrySet().stream()
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue())).toList();
            int[] bonuses = {25, 15, 10};
            for (int i = 0; i < Math.min(3, sorted.size()); i++) {
                if (sorted.get(i).getValue() > 0) {
                    scoring.addIndividualPoints(sorted.get(i).getKey(), bonuses[i], "#" + (i + 1) + " in One Shot");
                }
            }
        } else if (currentGame == GameType.PARKOUR) {
            // Award height bonus to players who didn't finish the summit
            List<Map.Entry<UUID, Double>> climbers = maxParkourY.entrySet().stream()
                    .filter(e -> !finishOrder.contains(e.getKey()))
                    .sorted((a, b) -> b.getValue().compareTo(a.getValue())).toList();
            int[] hBonus = {20, 12, 6};
            for (int i = 0; i < Math.min(3, climbers.size()); i++) {
                scoring.addIndividualPoints(climbers.get(i).getKey(), hBonus[i], "Parkour Height #" + (i + 1));
            }
        } else if (currentGame != GameType.DROPPER) {
            int winBonus = (currentGame == GameType.SURVIVAL) ? 40
                    : (currentGame == GameType.SURVIVAL_GAMES) ? 30
                    : (currentGame == GameType.KIT_PVP) ? 15 : 20;
            scoring.awardSplitTeamBonus(alivePlayers, winBonus, "Round Victory!");
        }

        if (roundNumber < maxRounds) {
            roundNumber++;
            long gapTicks = Waits.ticks(80L); // fast-test: <= 1 s
            broadcast(ChatColor.YELLOW + "Round over (" + reason + ")! Next round (" + roundNumber + "/" + maxRounds + ") in " + secondsLabel(gapTicks) + "...");
            GameType active = currentGame;
            Runnable next = () -> {
                pendingTransitionTask = null;
                clearPendingWait();
                startSubRound(active);
            };
            pendingTransitionTask = new BukkitRunnable() {
                @Override public void run() { next.run(); }
            }.runTaskLater(this, gapTicks);
            setPendingWait("break before round " + roundNumber, next);
            return;
        }

        endCurrentEvent(reason);
    }

    private void endCurrentEvent(String reason) {
        String finishedName = finishedNameOverride != null ? finishedNameOverride : currentDisplay();
        finishedNameOverride = null;
        stopExternalIfActive();
        cancelTasks();
        restoreArenas();
        currentGame = GameType.LOBBY;
        if (sgWorld != null) sgWorld.getWorldBorder().reset();
        if (sg4World != null) sg4World.getWorldBorder().reset();
        if (oneshotWorld != null) oneshotWorld.getWorldBorder().reset();
        setSurvivalWorldActive(false);

        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "=== " + finishedName + " OVER! (" + reason + ") ===");
        returnToLobby(null);

        // Decide what comes next now, run it after the placement reveal (skippable with /mcm skip).
        final boolean wasChampionship = championshipMode;
        if (championshipMode) {
            playlistIndex++;
            if (playlistIndex >= playlist.size()) championshipMode = false;
        }
        final boolean championshipOver = wasChampionship && !championshipMode;
        final int idxAfter = playlistIndex;
        List<String> placeKeys = new ArrayList<>();
        List<String> placeDetails = new ArrayList<>();
        computeGamePlacements(placeKeys, placeDetails);
        Runnable after = () -> {
            pendingTransitionTask = null;
            pendingRevealAfter = null;
            clearPendingWait();
            printLeaderboard();
            if (championshipOver) {
                announceChampion();
            } else if (wasChampionship && championshipMode && playlistIndex == idxAfter && currentGame == GameType.LOBBY
                    && playlistIndex < playlist.size()) {
                continueChampionship();
            }
        };
        pendingRevealAfter = after;
        // McmFx.placementReveal shortens itself to ~1 s in fast-test mode
        pendingTransitionTask = McmFx.placementReveal(this, new CoreView(), placeKeys, placeDetails, after);
        setPendingWait("results reveal", after);
    }

    /** Between events of a championship (playlistIndex already points at the next event). */
    private void continueChampionship() {
        McmVoteProvider vp = McmApi.voteProvider();
        // Minecraft Monday external games (e.g. Find the Button) are never Decision Dome vote options
        List<String> voteIds = new ArrayList<>();
        for (String id : playlist.subList(playlistIndex, playlist.size())) if (!isMmExternal(id) && !isFinale(id)) voteIds.add(id);
        if (vp != null && voteIds.size() > 1) {
            broadcast(ChatColor.AQUA + "Head to the vote! The next event is chosen by the players.");
            // placeholder task so /mcm skipwait treats the vote as a wait it can finish;
            // also a safety net: if the vote never reports back, continue in order after 90s
            final int idxAtVote = playlistIndex;
            // fast-test mode: after <= 1 s the core asks the provider to end the vote now (McmVoteProvider.skipWait);
            // if it can't, the core cancels the vote and continues in playlist order
            final long voteTicks = Waits.ticks(20L * 90);
            Runnable hardFallback = () -> {
                pendingTransitionTask = null;
                clearPendingWait();
                if (championshipMode && playlistIndex == idxAtVote && currentGame == GameType.LOBBY) {
                    if (!McmApi.isFastTest()) getLogger().warning("Vote did not finish in time; continuing in playlist order.");
                    McmVoteProvider stuck = McmApi.voteProvider();
                    if (stuck != null) { try { stuck.cancelVote(); } catch (Throwable ignored) {} }
                    startById(playlist.get(playlistIndex));
                }
            };
            // ask the provider to end the vote with its current result; keep a 5 s safety net for onChosen
            Runnable endVoteNow = () -> {
                pendingTransitionTask = null;
                clearPendingWait();
                if (!championshipMode || playlistIndex != idxAtVote || currentGame != GameType.LOBBY) return;
                boolean ended = false;
                McmVoteProvider cur = McmApi.voteProvider();
                if (cur != null) { try { ended = cur.skipWait(); } catch (Throwable t) { getLogger().log(Level.WARNING, "vote skipWait() failed", t); } }
                if (!ended) { hardFallback.run(); return; }
                pendingTransitionTask = new BukkitRunnable() {
                    @Override public void run() { hardFallback.run(); }
                }.runTaskLater(this, 100L);
                setPendingWait("Decision Dome vote result", hardFallback);
            };
            pendingTransitionTask = new BukkitRunnable() {
                @Override public void run() { if (McmApi.isFastTest()) endVoteNow.run(); else hardFallback.run(); }
            }.runTaskLater(this, voteTicks);
            setPendingWait("Decision Dome vote", endVoteNow);
            Map<UUID, String> teams = new HashMap<>();
            // every registered player, online or not, so a rejoiner during the vote lands in their own pod
            for (Map.Entry<UUID, String> reg : scoring.registeredTeams().entrySet()) {
                Player rp = Bukkit.getPlayer(reg.getKey());
                if (rp != null ? isStaff(rp) : scoring.isMarkedSpectator(reg.getKey())) continue;
                if (reg.getValue() != null && !reg.getValue().isBlank()) teams.put(reg.getKey(), reg.getValue());
            }
            McmVoteRequest request = new McmVoteRequest(voteIds, teams, playlistIndex + 1,
                    playlistIndex > 0, false, 8);
            try {
                vp.runVote(request, chosen -> {
                    if (!championshipMode || playlistIndex != idxAtVote || currentGame != GameType.LOBBY) return;
                    if (pendingTransitionTask != null) { pendingTransitionTask.cancel(); pendingTransitionTask = null; }
                    clearPendingWait();
                    if (chosen != null) {
                        int at = playlist.indexOf(chosen.toLowerCase());
                        if (at >= playlistIndex) { playlist.remove(at); playlist.add(playlistIndex, chosen.toLowerCase()); }
                    }
                    startById(playlist.get(playlistIndex));
                });
            } catch (Throwable t) {
                getLogger().log(Level.SEVERE, "Vote provider failed; continuing in order", t);
                if (pendingTransitionTask != null) { pendingTransitionTask.cancel(); pendingTransitionTask = null; }
                clearPendingWait();
                startById(playlist.get(playlistIndex));
            }
            return;
        }
        String next = playlist.get(playlistIndex);
        long interTicks = Waits.ticks(200L); // fast-test: <= 1 s
        broadcast(ChatColor.AQUA + "Next event (" + McmApi.displayName(next) + ") starting in " + secondsLabel(interTicks) + "! (/mcm skipwait to start sooner)");
        Runnable go = () -> {
            pendingTransitionTask = null;
            clearPendingWait();
            if (championshipMode && currentGame == GameType.LOBBY) startById(next);
        };
        pendingTransitionTask = new BukkitRunnable() {
            @Override public void run() { go.run(); }
        }.runTaskLater(this, interTicks);
        setPendingWait("intermission before " + McmApi.displayName(next), go);
    }

    private void announceChampion() {
        List<Map.Entry<String, Integer>> teamStandings = scoring.getStandings();
        List<Map.Entry<UUID, Integer>> indStandings = scoring.getIndividualStandings();

        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "==================================");
        broadcast(ChatColor.YELLOW + "" + ChatColor.BOLD + "   ★ CHAMPIONSHIP COMPLETE! ★");
        if (!teamStandings.isEmpty() || declaredChampionSide != null) {
            String winName = teamStandings.isEmpty() ? "?" : teamStandings.get(0).getKey();
            int winPts = teamStandings.isEmpty() ? 0 : teamStandings.get(0).getValue();
            String dk = declaredChampionSide;
            declaredChampionSide = null;
            podiumChampionSide = dk;
            if (dk != null && dk.startsWith("TEAM:")) {          // MCC: the Dodgebolt winner wins the event
                String t = dk.substring(5);
                winName = scoring.teamColor(t) + t + " Team";
                winPts = scoring.getTeamPoints(t);
            } else if (dk != null && dk.startsWith("SOLO:")) {
                try {
                    UUID u = UUID.fromString(dk.substring(5));
                    winName = ChatColor.WHITE + scoring.playerName(u);
                    winPts = scoring.getPlayerPoints(u);
                } catch (IllegalArgumentException ignored) { }
            }
            if (dk != null) broadcast(ChatColor.YELLOW + "   Dodgebolt decides the event winner.");
            broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "   CHAMPIONS: " + winName + ChatColor.GREEN + " (" + winPts + " coins)");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(winName + ChatColor.GOLD + " WINS!",
                        ChatColor.YELLOW + "Champions with " + winPts + " total coins!", 10, 80, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.4f, 1f);   // owner: end sound too loud
            }
        }
        if (!indStandings.isEmpty()) {
            String mvp = scoring.playerName(indStandings.get(0).getKey());
            broadcast(ChatColor.AQUA + "   Individual MVP: " + ChatColor.WHITE + mvp + ChatColor.GRAY + " (" + indStandings.get(0).getValue() + " coins)");
        }
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "==================================");

        try {
            buildChampionPodium();
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Champion podium failed", t);
            removePodium(false);
            if (world != null) McmFx.fireworkShow(this, lobbyLocation(), Color.YELLOW, 6);
        }
    }

    private void returnToLobby(String msg) {
        stopExternalIfActive();
        if (!championshipMode && McmApi.voteProvider() != null) McmApi.voteProvider().cancelVote();
        cancelTasks();
        restoreArenas();
        currentGame = GameType.LOBBY;
        scoring.clearTeamSizeSnapshot();
        coreSpectators.clear();
        builtInZone = null;
        if (sgWorld != null) sgWorld.getWorldBorder().reset();
        if (sg4World != null) sg4World.getWorldBorder().reset();
        if (oneshotWorld != null) oneshotWorld.getWorldBorder().reset();
        setSurvivalWorldActive(false);
        if (msg != null && !msg.isEmpty()) broadcast(msg);
        Location lobby = lobbyLocation();
        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayer(p, GameMode.ADVENTURE);
            p.teleport(lobby);
            giveLobbyItems(p);
        }
    }

    private void restoreArenas() {
        for (Location loc : brokenTntBlocks) {
            loc.getBlock().setType(Material.ANDESITE, false);
        }
        brokenTntBlocks.clear();
        scheduledTntKeys.clear();

        for (Map.Entry<Location, BlockData> entry : brokenSpleefBlocks.entrySet()) {
            entry.getKey().getBlock().setBlockData(entry.getValue(), false);
        }
        brokenSpleefBlocks.clear();
    }

    // ==================== ALL 7 GAMEMODE SETUPS ====================

    private World getActiveSgWorld() {
        if (useSg4Map && sg4World != null) return sg4World;
        return sgWorld != null ? sgWorld : world;
    }

    // 1. SURVIVAL GAMES (BLITZ SG) — Alternates SG4 & Kalas!
    private void setupSurvivalGames() {
        if (forcedSgMap != null) { useSg4Map = forcedSgMap.equals("sg4"); forcedSgMap = null; } // /mcm map sg <id>
        else useSg4Map = !useSg4Map; // alternate between SG4 and Kalas each game
        World w = getActiveSgWorld();
        Location center = w.getSpawnLocation().clone();
        int cx = center.getBlockX(), cz = center.getBlockZ();

        int chests = refillSgChests(false);

        List<Player> ps = new ArrayList<>(Bukkit.getOnlinePlayers());
        int ringR = Math.max(7, Math.min(18, ps.size()));
        for (int i = 0; i < ps.size(); i++) {
            double ang = (2 * Math.PI * i) / Math.max(1, ps.size());
            int sx = cx + (int) Math.round(Math.cos(ang) * ringR);
            int sz = cz + (int) Math.round(Math.sin(ang) * ringR);
            int sy = findSafeGround(w, sx, sz, (w == sgWorld ? 140 : 85), 20, center.getBlockY());
            ps.get(i).teleport(new Location(w, sx + 0.5, sy, sz + 0.5));
        }

        WorldBorder wb = w.getWorldBorder();
        wb.setCenter(cx + 0.5, cz + 0.5);
        wb.setSize(240);
        wb.setSize(28, 175);

        gracePeriod = true;
        timeLeft = 240;
        String mapLabel = (w == sg4World) ? "Survival Games 4" : "Kalas SG";
        broadcast(ChatColor.YELLOW + "⚔ Map: " + ChatColor.WHITE + mapLabel + ChatColor.YELLOW + " | 15s Grace | " + chests + " chests filled!");
    }

    private int refillSgChests(boolean midGameUpgrade) {
        World w = getActiveSgWorld();
        Location center = w.getSpawnLocation();
        int cChunkX = center.getBlockX() >> 4, cChunkZ = center.getBlockZ() >> 4;
        int count = 0;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                Chunk ch = w.getChunkAt(cChunkX + dx, cChunkZ + dz);
                for (BlockState bs : ch.getTileEntities()) {
                    if (bs instanceof Chest chest) {
                        boolean mid = midGameUpgrade || bs.getLocation().distanceSquared(center) < 400;
                        fillLootChest(chest, mid);
                        count++;
                    }
                }
            }
        }
        return count;
    }

    private void dropBlitzStar() {
        World w = getActiveSgWorld();
        Location c = w.getSpawnLocation().clone();
        int y = findSafeGround(w, c.getBlockX(), c.getBlockZ(), (w == sgWorld ? 140 : 85), 20, c.getBlockY());
        Block b = w.getBlockAt(c.getBlockX(), y, c.getBlockZ());
        b.setType(Material.CHEST);
        if (b.getState() instanceof Chest chest) {
            ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
            ItemMeta sm = sword.getItemMeta();
            sm.addEnchant(Enchantment.SHARPNESS, 1, true);
            sword.setItemMeta(sm);
            chest.getInventory().addItem(sword);
            chest.getInventory().addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE));
            chest.getInventory().addItem(new ItemStack(Material.DIAMOND_CHESTPLATE));
            ItemStack spd = new ItemStack(Material.POTION);
            PotionMeta pm = (PotionMeta) spd.getItemMeta();
            pm.setDisplayName(ChatColor.AQUA + "Blitz Speed II");
            pm.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 25, 1), true);
            spd.setItemMeta(pm);
            chest.getInventory().addItem(spd);
        }
        w.strikeLightningEffect(b.getLocation());
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "★ THE BLITZ STAR CHEST HAS DROPPED AT CORNUCOPIA! ★");
    }

    private void triggerSgDeathmatch() {
        World w = getActiveSgWorld();
        Location center = w.getSpawnLocation().clone();
        int cx = center.getBlockX(), cz = center.getBlockZ();
        List<UUID> alive = new ArrayList<>(alivePlayers);
        for (int i = 0; i < alive.size(); i++) {
            Player p = Bukkit.getPlayer(alive.get(i));
            if (p == null) continue;
            double ang = (2 * Math.PI * i) / Math.max(1, alive.size());
            int sx = cx + (int) Math.round(Math.cos(ang) * 9);
            int sz = cz + (int) Math.round(Math.sin(ang) * 9);
            int sy = findSafeGround(w, sx, sz, (w == sgWorld ? 140 : 85), 20, center.getBlockY());
            p.teleport(new Location(w, sx + 0.5, sy, sz + 0.5));
        }
        WorldBorder wb = w.getWorldBorder();
        wb.setSize(30);
        wb.setSize(8, 55);
        broadcast(ChatColor.DARK_RED + "" + ChatColor.BOLD + "⚔ DEATHMATCH! Everyone teleported to center! 60s remaining!");
    }

    private void updateAllTrackers() {
        for (UUID u : alivePlayers) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            Player nearest = findNearestEnemy(p);
            if (nearest != null) p.setCompassTarget(nearest.getLocation());
        }
    }

    private Player findNearestEnemy(Player p) {
        String mySide = scoring.getSideKey(p.getUniqueId());
        Player best = null;
        double bestDist = Double.MAX_VALUE;
        for (UUID u : alivePlayers) {
            if (u.equals(p.getUniqueId()) || scoring.getSideKey(u).equals(mySide)) continue;
            Player other = Bukkit.getPlayer(u);
            if (other != null && other.getWorld().equals(p.getWorld())) {
                double d = other.getLocation().distanceSquared(p.getLocation());
                if (d < bestDist) { bestDist = d; best = other; }
            }
        }
        return best;
    }

    private void fillLootChest(Chest chest, boolean highTier) {
        Inventory inv = chest.getInventory();
        inv.clear();
        ItemStack[] pool = highTier ? new ItemStack[]{
                new ItemStack(Material.IRON_SWORD), new ItemStack(Material.STONE_SWORD),
                new ItemStack(Material.IRON_AXE), new ItemStack(Material.BOW),
                new ItemStack(Material.ARROW, 12), new ItemStack(Material.GOLDEN_APPLE, 1),
                new ItemStack(Material.IRON_CHESTPLATE), new ItemStack(Material.IRON_HELMET),
                new ItemStack(Material.IRON_LEGGINGS), new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.COOKED_BEEF, 4), new ItemStack(Material.FISHING_ROD),
                new ItemStack(Material.ENDER_PEARL, 1)
        } : new ItemStack[]{
                new ItemStack(Material.STONE_SWORD), new ItemStack(Material.WOODEN_SWORD),
                new ItemStack(Material.BOW), new ItemStack(Material.ARROW, 8),
                new ItemStack(Material.CHAINMAIL_CHESTPLATE), new ItemStack(Material.LEATHER_HELMET),
                new ItemStack(Material.CHAINMAIL_LEGGINGS), new ItemStack(Material.IRON_BOOTS),
                new ItemStack(Material.COOKED_BEEF, 3), new ItemStack(Material.GOLDEN_APPLE, 1),
                new ItemStack(Material.FISHING_ROD)
        };
        int count = 3 + rng.nextInt(4);
        for (int i = 0; i < count; i++) inv.setItem(rng.nextInt(27), pool[rng.nextInt(pool.length)].clone());
    }

    // 2. PARKOUR SPIRAL (Real Hielke Map!)
    private void setupParkour() {
        World w = parkourWorld != null ? parkourWorld : world;
        Location start = new Location(w, -1.0, 52.0, -50.7, 0f, 10f);
        ItemStack feather = makeItem(Material.FEATHER, ChatColor.GREEN + "Back to Checkpoint", ChatColor.GRAY + "Right-click to warp to last checkpoint");
        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayer(p, GameMode.ADVENTURE);
            parkourCheckpoint.put(p.getUniqueId(), start.clone());
            visitedPlates.put(p.getUniqueId(), new HashSet<>());
            maxParkourY.put(p.getUniqueId(), 52.0);
            climbedPastWater.put(p.getUniqueId(), false);
            p.getInventory().setItem(4, feather.clone());
            p.teleport(start);
        }
        timeLeft = 240;
        broadcast(ChatColor.GREEN + "Climb Hielke's Parkour Spiral to the summit (Y=230)! Checkpoints give +3 coins!");
    }

    // 3. MULTI-STAGE DROPPER (Real Delightful Dropper 2 — 5 Stages!)
    private void setupDropper() {
        World w = dropperWorld != null ? dropperWorld : world;
        activeDropperStages.clear();
        activeDropperStages.addAll(pickDropperStages());
        stageFinishCounts = new int[activeDropperStages.size()];
        int n = activeDropperStages.size();

        DropperStage first = activeDropperStages.get(0);
        Location spawn = dropperStart(first);
        ItemStack retry = makeItem(Material.FEATHER, ChatColor.AQUA + "Retry Current Drop", ChatColor.GRAY + "Right-click to restart this stage");
        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayer(p, GameMode.ADVENTURE);
            dropperStageIdx.put(p.getUniqueId(), 0);
            p.getInventory().setItem(4, retry.clone());
            p.teleport(spawn);
            afterDropperTeleport(p, first);
            p.sendTitle(ChatColor.AQUA + "Stage 1 / " + n + ": " + first.name(), dropperSubtitle(first), 5, 45, 10);
        }
        timeLeft = 210;
        boolean anyImported = activeDropperStages.stream().anyMatch(st -> st.imported() != null);
        broadcast(ChatColor.BLUE + "Clear " + n + " themed Dropper shafts! Watch the title for what to land on (Water, Slime, Cobwebs, or Vines)!"
                + (anyImported ? " Imported shafts end in a pool." : ""));
        broadcast(ChatColor.AQUA + "Stage 1: " + ChatColor.WHITE + first.name() + ChatColor.YELLOW + " — Aim for " + first.hint() + "!"
                + dropperCreditSuffix(first));
    }

    /**
     * Stages for this round (config dropper.stage-count, default 5; dropper.imported-stages: always | mixed | off).
     * Without ready imported shafts this is exactly the old pick: 5 of the 6 built-in stages, shuffled.
     */
    private List<DropperStage> pickDropperStages() {
        int count = Math.max(1, Math.min(12, getConfig().getInt("dropper.stage-count", 5)));
        String mode = getConfig().getString("dropper.imported-stages", "always").toLowerCase(Locale.ROOT);
        List<DropperStage> imported = new ArrayList<>();
        if (importedDroppers != null && !mode.equals("off") && dropperWorld != null) {
            for (ImportedDropper d : importedDroppers.ready()) imported.add(importedStage(d));
        }
        List<DropperStage> builtIn = new ArrayList<>(ALL_DROPPER_STAGES);
        Collections.shuffle(builtIn);
        List<DropperStage> out = new ArrayList<>();
        if (imported.isEmpty()) {
            for (int i = 0; i < Math.min(count, builtIn.size()); i++) out.add(builtIn.get(i));
            return out;
        }
        Collections.shuffle(imported);
        if (mode.equals("mixed")) {
            List<DropperStage> pool = new ArrayList<>(builtIn);
            pool.addAll(imported);
            Collections.shuffle(pool);
            for (int i = 0; i < Math.min(count, pool.size()); i++) out.add(pool.get(i));
        } else {
            for (int i = 0; i < imported.size() && out.size() < count; i++) out.add(imported.get(i));
            for (int i = 0; i < builtIn.size() && out.size() < count; i++) out.add(builtIn.get(i));
            Collections.shuffle(out);
        }
        return out;
    }

    private static DropperStage importedStage(ImportedDropper d) {
        double[] s = d.spawn();
        return new DropperStage(d.name, "THE POOL", Material.WATER, s[0], s[1], s[2], d.yaw, d);
    }

    /** Where a player (re)starts a stage. Imported shafts: in the air above the shaft, looking down. */
    private Location dropperStart(DropperStage st) {
        World w = dropperWorld != null ? dropperWorld : world;
        return new Location(w, st.x(), st.y(), st.z(), st.yaw(), st.imported() != null ? 60f : 45f);
    }

    /** Imported shafts have no ledge: a short slow fall at the top lets the player get their bearings. */
    private void afterDropperTeleport(Player p, DropperStage st) {
        p.setFallDistance(0f);
        p.setFireTicks(0);
        if (st.imported() != null) {
            p.setVelocity(new Vector());
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 40, 0, false, false, false));
        }
    }

    private void sendToDropperStart(Player p, DropperStage st) {
        p.teleport(dropperStart(st));
        afterDropperTeleport(p, st);
    }

    private static String dropperCreditSuffix(DropperStage st) {
        return st.imported() != null ? ChatColor.GRAY + " (Map by " + st.imported().credit + ")" : "";
    }

    private static String dropperSubtitle(DropperStage st) {
        if (st.imported() != null) return ChatColor.YELLOW + "Land in the pool! " + ChatColor.GRAY + "Map by " + st.imported().credit;
        return ChatColor.YELLOW + "Aim for: " + st.hint() + "!";
    }

    private DropperStage currentDropperStage(Player p) {
        int idx = dropperStageIdx.getOrDefault(p.getUniqueId(), 0);
        return idx >= 0 && idx < activeDropperStages.size() ? activeDropperStages.get(idx) : null;
    }

    // 4. TNT RUN — Best of 3 Rounds (Real RunnonStone: Jungle -> Castle -> Pyramid!)
    private void setupTntRun() {
        World w = tntrunWorld != null ? tntrunWorld : world;
        Location spawn = switch (roundNumber) {
            case 2 -> new Location(w, 100.5, 8.0, 235.5);
            case 3 -> new Location(w, 100.5, 12.0, 375.5);
            default -> new Location(w, 100.5, 15.0, 100.5);
        };
        String arenaName = roundNumber == 1 ? "Jungle" : roundNumber == 2 ? "Castle" : "Pyramid";
        ItemStack dj = makeItem(Material.FEATHER, ChatColor.GOLD + "Double Jump (3 Uses)", ChatColor.GRAY + "Right-click mid-air to leap up!");
        dj.setAmount(3);
        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayer(p, GameMode.ADVENTURE);
            doubleJumpsLeft.put(p.getUniqueId(), 3);
            p.getInventory().setItem(0, dj.clone());
            p.teleport(spawn);
        }
        timeLeft = 90;
        broadcast(ChatColor.GOLD + "Arena: " + ChatColor.WHITE + arenaName + ChatColor.GOLD + " | 3 Double-Jump Feathers each!");
    }

    // 5. SPLEEF / SPLEGG — Best of 3 Rounds (Real Hive Inferno!)
    private void setupSpleef() {
        World w = spleefWorld != null ? spleefWorld : world;
        int sy = findSafeGround(w, -13, 22, 105, 80, 89);
        Location spawn = new Location(w, -13.5, sy, 22.5);

        ItemStack shovel = makeItem(Material.GOLDEN_SHOVEL, ChatColor.GOLD + "" + ChatColor.BOLD + "MCM Golden Shovel (Left: Instamine | Right: Splegg)",
                ChatColor.GRAY + "Instant-break blocks OR right-click to fire Snowballs!");
        ItemMeta sm = shovel.getItemMeta();
        sm.addEnchant(Enchantment.EFFICIENCY, 5, true);
        sm.setUnbreakable(true);
        shovel.setItemMeta(sm);

        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayer(p, GameMode.SURVIVAL);
            p.getInventory().setItem(0, shovel.clone());
            p.getInventory().setItem(1, new ItemStack(Material.SNOWBALL, 4));
            p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 100, 4, false, false));
            p.teleport(spawn);
        }
        timeLeft = 90;
        broadcast(ChatColor.AQUA + "Left-click with your Golden Shovel to INSTAMINE OR Right-click / throw Snowballs for knockback!");
    }

    // 6. BATTLE ARENA / KITPVP — Best of 2 Rounds (Real Overcast Harb!)
    private void setupKitPvp() {
        World w = pvpWorld != null ? pvpWorld : world;

        List<String> sideOrder = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            String key = scoring.getSideKey(p.getUniqueId());
            if (!sideOrder.contains(key)) sideOrder.add(key);
        }

        int sideACount = 0;
        int sideBCount = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayer(p, GameMode.ADVENTURE);
            KitType chosen = playerKits.getOrDefault(p.getUniqueId(), KitType.KNIGHT);
            applyKitPvpGear(p, chosen);
            p.getInventory().setItem(8, makeItem(Material.BOOK, ChatColor.GOLD + "" + ChatColor.BOLD + "Select Kit (4s)", ChatColor.GRAY + "Right-click to change kit!"));
            int idx = sideOrder.indexOf(scoring.getSideKey(p.getUniqueId()));
            boolean isA = (idx % 2 == 0);
            int offsetIdx = isA ? sideACount++ : sideBCount++;
            double xOff = ((offsetIdx % 7) - 3) * 1.8;
            Location spawn = isA ? new Location(w, 0.5 + xOff, 33.0, 48.5, 180f, 0f)
                    : new Location(w, 0.5 + xOff, 33.0, -47.5, 0f, 0f);
            p.teleport(spawn);
        }
        timeLeft = 90;
        broadcast(ChatColor.DARK_RED + "Harb Arena | 4s to pick your Kit (Right-click Book)!");
    }

    private void applyKitPvpGear(Player p, KitType kit) {
        p.getInventory().clear();
        for (PotionEffect pe : p.getActivePotionEffects()) p.removePotionEffect(pe.getType());
        switch (kit) {
            case KNIGHT -> {
                p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
                p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
                p.getInventory().setItem(0, new ItemStack(Material.DIAMOND_SWORD));
                p.getInventory().setItem(1, new ItemStack(Material.FISHING_ROD));
                p.getInventory().setItem(2, new ItemStack(Material.BOW));
                p.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 2));
                p.getInventory().setItem(7, new ItemStack(Material.ARROW, 12));
            }
            case ARCHER -> {
                p.getInventory().setHelmet(new ItemStack(Material.CHAINMAIL_HELMET));
                p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                p.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
                ItemStack bow = new ItemStack(Material.BOW);
                ItemMeta bm = bow.getItemMeta();
                bm.addEnchant(Enchantment.POWER, 2, true);
                bow.setItemMeta(bm);
                p.getInventory().setItem(0, new ItemStack(Material.STONE_SWORD));
                p.getInventory().setItem(1, new ItemStack(Material.FISHING_ROD));
                p.getInventory().setItem(2, bow);
                p.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 2));
                p.getInventory().setItem(7, new ItemStack(Material.ARROW, 24));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 90, 0, false, false));
            }
            case TANK -> {
                p.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
                p.getInventory().setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
                p.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
                p.getInventory().setBoots(new ItemStack(Material.DIAMOND_BOOTS));
                p.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
                p.getInventory().setItem(1, new ItemStack(Material.FISHING_ROD));
                p.getInventory().setItem(2, new ItemStack(Material.GOLDEN_APPLE, 3));
            }
            case ASSASSIN -> {
                p.getInventory().setHelmet(new ItemStack(Material.LEATHER_HELMET));
                p.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
                p.getInventory().setLeggings(new ItemStack(Material.CHAINMAIL_LEGGINGS));
                p.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta sm = sword.getItemMeta();
                sm.addEnchant(Enchantment.SHARPNESS, 1, true);
                sword.setItemMeta(sm);
                p.getInventory().setItem(0, sword);
                p.getInventory().setItem(1, new ItemStack(Material.FISHING_ROD));
                p.getInventory().setItem(2, new ItemStack(Material.ENDER_PEARL, 1));
                p.getInventory().setItem(3, new ItemStack(Material.GOLDEN_APPLE, 1));
                p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 90, 1, false, false));
            }
        }
        p.getInventory().setItem(6, new ItemStack(Material.COOKED_BEEF, 8));
        if (currentGame == GameType.KIT_PVP && startFreezeTicks > 0) {
            p.getInventory().setItem(8, makeItem(Material.BOOK, ChatColor.GOLD + "" + ChatColor.BOLD + "Select Kit", ChatColor.GRAY + "Right-click to change kit!"));
        }
    }

    // 7. OPTIONAL: PINK VALLEY (ONE SHOT) — Cleaned 70x70 Arena
    private void setupOneShot() {
        World w = oneshotWorld != null ? oneshotWorld : world;
        w.getEntitiesByClass(Item.class).forEach(Entity::remove);
        WorldBorder wb = w.getWorldBorder();
        wb.setCenter(51.5, 100.5);
        wb.setSize(70);

        for (Player p : Bukkit.getOnlinePlayers()) {
            giveOneShotKit(p);
            teleportOneShotSpawn(p);
        }
        timeLeft = 180;
        broadcast(ChatColor.LIGHT_PURPLE + "Pink Valley Arena (70x70) | Bows are 1-HIT KILL! First to 15 kills or most in 3m wins!");
    }

    private void giveOneShotKit(Player p) {
        resetPlayer(p, GameMode.ADVENTURE);
        p.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        p.getInventory().setItem(1, new ItemStack(Material.BOW));
        p.getInventory().setItem(8, new ItemStack(Material.ARROW, 1));
        p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 190, 0, false, false));
    }

    private void teleportOneShotSpawn(Player p) {
        World w = oneshotWorld != null ? oneshotWorld : world;
        int rx = 51 + rng.nextInt(21) - 10;
        int rz = 100 + rng.nextInt(21) - 10;
        int ry = findSafeGround(w, rx, rz, 65, 25, 42);
        if (ry <= 43 && w.getBlockAt(rx, ry - 1, rz).isSolid()) {
            p.teleport(new Location(w, rx + 0.5, ry, rz + 0.5));
        } else {
            p.teleport(new Location(w, 51.5, 42.0, 100.5));
        }
    }

    // 8. SPEED SURVIVAL (15m UHC-Style Survival Finale)
    private void setupSurvival() {
        World w = survivalWorld != null ? survivalWorld : world;
        int baseX = survivalMatchNumber * 1600;
        survivalMatchNumber++;
        int[] center = findLandCenter(w, baseX, 0);
        survivalCenterX = center[0];
        survivalCenterZ = center[1];
        setSurvivalWorldActive(true);

        WorldBorder wb = w.getWorldBorder();
        wb.setCenter(survivalCenterX + 0.5, survivalCenterZ + 0.5);
        wb.setSize(320.0);
        wb.setDamageBuffer(3.0);
        wb.setDamageAmount(0.4);

        Map<String, List<Player>> sideGroups = new LinkedHashMap<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            sideGroups.computeIfAbsent(scoring.getSideKey(p.getUniqueId()), k -> new ArrayList<>()).add(p);
        }

        List<List<Player>> groups = new ArrayList<>(sideGroups.values());
        for (int i = 0; i < groups.size(); i++) {
            double ang = (2 * Math.PI * i) / Math.max(1, groups.size());
            Location teamSpawn = findSafeScatterPoint(w, survivalCenterX, survivalCenterZ, ang, 110);
            for (Player p : groups.get(i)) {
                resetPlayer(p, GameMode.SURVIVAL);
                giveSurvivalStarterKit(p);
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 20 * 300, 0, false, false));
                p.teleport(teamSpawn);
            }
        }

        gracePeriod = true;
        timeLeft = 900;
        broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "★ SPEED SURVIVAL (15m) | Center: (" + survivalCenterX + ", " + survivalCenterZ + ") | Border: 320x320 ★");
        broadcast(ChatColor.YELLOW + "5m Grace (PvP OFF, CutClean 2x Ores, Timber, Haste I) | Supply Drops at 7:00 & 10:00 | Deathmatch at 12:00!");
    }

    private void giveSurvivalStarterKit(Player p) {
        p.getInventory().setItem(0, makeSpeedTool(Material.STONE_PICKAXE, ChatColor.AQUA + "Speed Pickaxe (Eff II)"));
        p.getInventory().setItem(1, makeSpeedTool(Material.STONE_AXE, ChatColor.AQUA + "Timber Axe (Eff II)"));
        p.getInventory().setItem(2, makeSpeedTool(Material.STONE_SHOVEL, ChatColor.AQUA + "Speed Shovel (Eff II)"));
        p.getInventory().setItem(3, new ItemStack(Material.COOKED_BEEF, 8));
        p.getInventory().setItem(4, new ItemStack(Material.CRAFTING_TABLE, 1));
        p.getInventory().setItem(5, new ItemStack(Material.TORCH, 8));
        p.getInventory().setItem(8, makeItem(Material.COMPASS, ChatColor.AQUA + "Player Tracker", ChatColor.GRAY + "Activates when PvP begins (5:00)"));
    }

    private ItemStack makeSpeedTool(Material mat, String name) {
        ItemStack is = new ItemStack(mat);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(name);
        im.addEnchant(Enchantment.EFFICIENCY, 2, true);
        im.setUnbreakable(true);
        is.setItemMeta(im);
        return is;
    }

    private ItemStack makeEnchantBook(Enchantment ench, int level) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        if (book.getItemMeta() instanceof EnchantmentStorageMeta esm) {
            esm.addStoredEnchant(ench, level, true);
            book.setItemMeta(esm);
        }
        return book;
    }

    private boolean isDryLandMaterial(Material m) {
        String mn = m.name();
        return m.isSolid() && m != Material.ICE && m != Material.PACKED_ICE && m != Material.BLUE_ICE
                && m != Material.MAGMA_BLOCK && m != Material.CACTUS
                && !mn.contains("LEAVES") && !mn.contains("LOG");
    }

    private int findNaturalSurface(World w, int x, int z) {
        int top = Math.min(w.getHighestBlockYAt(x, z) + 2, 220);
        for (int y = top; y >= 60; y--) {
            Material m = w.getBlockAt(x, y, z).getType();
            if (m == Material.WATER || m == Material.LAVA) return -1;
            if (isDryLandMaterial(m)) {
                if (w.getBlockAt(x, y + 1, z).isPassable() && w.getBlockAt(x, y + 2, z).isPassable()) {
                    return y + 1;
                }
                return -1;
            }
        }
        return -1;
    }

    private int[] findLandCenter(World w, int baseX, int baseZ) {
        int[] fallback = null;
        for (int r = 0; r <= 3; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    int cx = baseX + dx * 64;
                    int cz = baseZ + dz * 64;
                    int sy = findNaturalSurface(w, cx, cz);
                    if (sy >= 62 && fallback == null) fallback = new int[]{cx, cz};
                    if (sy >= 63 && sy <= 115) {
                        int dryCardinals = 0;
                        if (findNaturalSurface(w, cx + 32, cz) >= 63) dryCardinals++;
                        if (findNaturalSurface(w, cx - 32, cz) >= 63) dryCardinals++;
                        if (findNaturalSurface(w, cx, cz + 32) >= 63) dryCardinals++;
                        if (findNaturalSurface(w, cx, cz - 32) >= 63) dryCardinals++;
                        if (dryCardinals >= 3) return new int[]{cx, cz};
                    }
                }
            }
        }
        return fallback != null ? fallback : new int[]{baseX, baseZ};
    }

    private Location findSafeScatterPoint(World w, int cx, int cz, double angle, int radius) {
        int[] radii = {radius, Math.max(20, radius - 20), radius + 20, Math.max(15, radius - 40)};
        double[] offsets = {0.0, 0.18, -0.18};
        for (int r : radii) {
            for (double off : offsets) {
                int sx = cx + (int) Math.round(Math.cos(angle + off) * r);
                int sz = cz + (int) Math.round(Math.sin(angle + off) * r);
                int sy = findNaturalSurface(w, sx, sz);
                if (sy >= 62) {
                    return new Location(w, sx + 0.5, sy, sz + 0.5);
                }
            }
        }
        for (int dx = -24; dx <= 24; dx += 4) {
            for (int dz = -24; dz <= 24; dz += 4) {
                int sy = findNaturalSurface(w, cx + dx, cz + dz);
                if (sy >= 62) {
                    return new Location(w, cx + dx + 0.5, sy, cz + dz + 0.5);
                }
            }
        }
        int fy = Math.max(64, w.getHighestBlockYAt(cx, cz) + 1);
        return new Location(w, cx + 0.5, fy, cz + 0.5);
    }

    private void spawnSurvivalSupplyDrop(boolean tier2) {
        World w = survivalWorld != null ? survivalWorld : world;
        int x = survivalCenterX + (tier2 ? 2 : 0);
        int z = survivalCenterZ;
        int sy = findNaturalSurface(w, x, z);
        if (sy < 63) sy = Math.max(63, w.getHighestBlockYAt(x, z) + 1);
        Block b = w.getBlockAt(x, sy, z);
        b.setType(Material.CHEST);
        if (b.getState() instanceof Chest chest) {
            Inventory inv = chest.getInventory();
            if (!tier2) {
                inv.addItem(new ItemStack(Material.ENCHANTING_TABLE));
                inv.addItem(new ItemStack(Material.LAPIS_LAZULI, 16));
                inv.addItem(new ItemStack(Material.GOLDEN_APPLE, 2));
                inv.addItem(new ItemStack(Material.ARROW, 16));
                inv.addItem(new ItemStack(Material.DIAMOND, 2));
                inv.addItem(new ItemStack(Material.EXPERIENCE_BOTTLE, 16));
                inv.addItem(makeEnchantBook(Enchantment.SHARPNESS, 2));
                inv.addItem(makeEnchantBook(Enchantment.POWER, 2));
            } else {
                ItemStack sword = new ItemStack(Material.DIAMOND_SWORD);
                ItemMeta sm = sword.getItemMeta();
                sm.addEnchant(Enchantment.SHARPNESS, 1, true);
                sword.setItemMeta(sm);
                inv.addItem(sword);
                inv.addItem(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1));
                inv.addItem(new ItemStack(Material.DIAMOND_CHESTPLATE));
                ItemStack bow = new ItemStack(Material.BOW);
                ItemMeta bm = bow.getItemMeta();
                bm.addEnchant(Enchantment.POWER, 2, true);
                bow.setItemMeta(bm);
                inv.addItem(bow);
                inv.addItem(new ItemStack(Material.ARROW, 24));
                inv.addItem(new ItemStack(Material.ENDER_PEARL, 2));
                ItemStack spd = new ItemStack(Material.POTION);
                PotionMeta pm = (PotionMeta) spd.getItemMeta();
                pm.setDisplayName(ChatColor.AQUA + "Speed II");
                pm.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 30, 1), true);
                spd.setItemMeta(pm);
                inv.addItem(spd);
            }
        }
        survivalSupplyChests.add(x + "," + sy + "," + z);
        w.strikeLightningEffect(b.getLocation());
        String label = tier2 ? ChatColor.GOLD + "" + ChatColor.BOLD + "★ BLITZ WAR CRATE" : ChatColor.AQUA + "" + ChatColor.BOLD + "★ ENCHANTING CRATE";
        broadcast(label + ChatColor.YELLOW + " dropped at (" + x + ", " + sy + ", " + z + ")! Right-click for +10 bonus coins!");
    }

    private void triggerSurvivalDeathmatch() {
        World w = survivalWorld != null ? survivalWorld : world;
        int centerY = findNaturalSurface(w, survivalCenterX, survivalCenterZ);
        if (centerY < 62) centerY = 70;
        List<UUID> alive = new ArrayList<>(alivePlayers);
        int pulled = 0;
        for (int i = 0; i < alive.size(); i++) {
            Player p = Bukkit.getPlayer(alive.get(i));
            if (p == null) continue;
            Location loc = p.getLocation();
            double dist = Math.hypot(loc.getX() - (survivalCenterX + 0.5), loc.getZ() - (survivalCenterZ + 0.5));
            if (!p.getWorld().equals(w) || loc.getY() < centerY - 12 || dist > 28.0) {
                double ang = (2 * Math.PI * i) / Math.max(1, alive.size());
                Location tp = findSafeScatterPoint(w, survivalCenterX, survivalCenterZ, ang, 18);
                p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 3, 4, false, false));
                p.teleport(tp);
                pulled++;
            }
        }
        WorldBorder wb = w.getWorldBorder();
        wb.setCenter(survivalCenterX + 0.5, survivalCenterZ + 0.5);
        wb.setSize(60.0);
        wb.setSize(12.0, 160);
        broadcast(ChatColor.DARK_RED + "" + ChatColor.BOLD + "⚔ FINAL DEATHMATCH! " + ChatColor.YELLOW + "Border shrinking 60 -> 12! (" + pulled + " pulled to surface)");
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(ChatColor.DARK_RED + "" + ChatColor.BOLD + "FINAL DEATHMATCH!",
                    ChatColor.YELLOW + "Border shrinking to 12x12!", 5, 50, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.7f, 1.0f);
        }
    }

    // ==================== GAMEPLAY LISTENERS ====================

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        scoring.ensurePlayer(p.getUniqueId());
        scoring.rememberName(p.getUniqueId(), p.getName());
        final boolean staff = isStaff(p);
        // Late joiner during a championship: fill the partly filled team (3/4) before deciding whether he plays,
        // so he gets the team colour, tab, sidebar and the game's late-join path like a reconnecting player.
        if (championshipMode && !staff && scoring.getTeam(p.getUniqueId()) == null) {
            String placed = scoring.placeInBestTeam(p.getUniqueId());
            if (placed != null) {
                p.sendMessage(ChatColor.AQUA + "Late join: you are on " + scoring.teamColor(placed) + ChatColor.BOLD + placed
                        + " Team" + ChatColor.AQUA + " (" + scoring.registeredCount(placed) + "/" + scoring.getMaxTeamSize() + ").");
                getLogger().info("Late join: " + p.getName() + " placed on " + placed + " ("
                        + scoring.registeredCount(placed) + "/" + scoring.getMaxTeamSize() + ")");
            } else {
                p.sendMessage(ChatColor.RED + "All " + scoring.getActiveTeams() + " teams are full (" + scoring.getMaxTeamSize()
                        + " each) - you are spectating. Ask an admin to make room.");
            }
        }
        final String rejoinTeam = scoring.getTeam(p.getUniqueId());
        if (currentGame == GameType.LOBBY) {
            resetPlayer(p, GameMode.ADVENTURE);
            Location dest = defaultLobby();
            p.teleport(dest);
            giveLobbyItems(p);
            if (hub != null && hub.isHubWorld(dest.getWorld())) {
                McmFx.title(p, ChatColor.GOLD + "" + ChatColor.BOLD + "MINECRAFT CHAMPIONSHIPS",
                        ChatColor.YELLOW + "Step on an East Team Booth or type /team!", 5, 50, 12);
            }
        } else {
            boolean handled = false;
            boolean eventPlayer = isEventPlayer(p);
            if (eventPlayer && currentGame == GameType.EXTERNAL && activeExternal != null && activeCtx != null) {
                try { handled = activeExternal.onLateJoin(p, activeCtx); } catch (Throwable t) { getLogger().log(Level.WARNING, "onLateJoin failed", t); }
                // A starter the running game took back is a participant again, and so is anyone the game put back
                // into play (not SPECTATOR). A new late joiner the game only seats in the stands is not a side:
                // he must not earn team coins while watching and plays from the next game.
                if (handled && (externalRoster.contains(p.getUniqueId()) || p.getGameMode() != GameMode.SPECTATOR)) {
                    externalParticipants.add(p.getUniqueId());
                }
            }
            if (!eventPlayer && currentGame == GameType.EXTERNAL) {
                p.sendMessage(ChatColor.YELLOW + (staff ? "You are staff/spectator: watching " + currentDisplay() + "."
                        : "You have no team, so you are spectating. Ask an admin to put you on a team."));
            }
            if (!handled) {
                sanitizeAttributes(p);
                // Spectate the running game from its arena instead of wherever the player logged in, kept inside it
                if (currentGame == GameType.EXTERNAL) {
                    McmSpectator.Zone z = externalSpectatorZone();
                    McmSpectator.makeSpectator(p, z != null ? z.home().clone() : null);
                    coreSpectators.add(p.getUniqueId());
                } else {
                    spectateInBuiltIn(p);
                }
                if (eventPlayer) p.sendMessage(ChatColor.YELLOW + "Event in progress (" + currentDisplay() + "). Spectating until next round!");
            }
        }
        if (rejoinTeam != null) {
            if (currentGame != GameType.LOBBY) {
                getLogger().info("Rejoin: " + p.getName() + " back on " + rejoinTeam + " during " + currentDisplay()
                        + (externalParticipants.contains(p.getUniqueId()) ? " (handed back to the game)" : " (spectating)"));
            }
            p.sendMessage(ChatColor.AQUA + "Welcome back! You are on " + scoring.teamColor(rejoinTeam) + ChatColor.BOLD + rejoinTeam + " Team" + ChatColor.AQUA + ".");
        }
        if (hub != null) Bukkit.getScheduler().runTask(this, () -> { if (hub != null) hub.refreshDisplays(); });
        scoring.applyTabNames();
        p.sendMessage(ChatColor.AQUA + "Tip: " + ChatColor.WHITE + "Right-click your Wool (or type " + ChatColor.YELLOW + "/team" + ChatColor.WHITE + ") to pick a team!");
        if (p.isOp()) {
            p.sendMessage(ChatColor.GOLD + "Admin: " + ChatColor.WHITE + "Right-click Compass or type " + ChatColor.YELLOW + "/mcm" + ChatColor.WHITE + ", Admin Tools feather or " + ChatColor.YELLOW + "/mcm tools" + ChatColor.WHITE + " (fast test, skip wait/round, maps). " + ChatColor.YELLOW + "/mcm help" + ChatColor.WHITE + " lists commands.");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        externalParticipants.remove(e.getPlayer().getUniqueId());
        coreSpectators.remove(e.getPlayer().getUniqueId());
        if (hub != null) Bukkit.getScheduler().runTask(this, () -> { if (hub != null) hub.refreshDisplays(); });
        if (currentGame != GameType.LOBBY && alivePlayers.remove(e.getPlayer().getUniqueId())) {
            if (currentGame == GameType.PARKOUR || currentGame == GameType.DROPPER) {
                if (finishOrder.size() >= alivePlayers.size() && !alivePlayers.isEmpty()) {
                    endSubRound("All remaining players finished!");
                }
            } else if (currentGame != GameType.ONE_SHOT && scoring.isOneSideRemaining(alivePlayers)) {
                endSubRound("Last standing!");
            }
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (AdminTools.isTool(e.getItem())) return; // the admin feather is handled by AdminTools, never by game items
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        if (currentGame == GameType.DROPPER && e.getClickedBlock() != null) {
            DropperStage cur = currentDropperStage(p);
            // imported shafts keep the source map's buttons, chests, signs: they are scenery only
            if (cur != null && cur.imported() != null) e.setUseInteractedBlock(Event.Result.DENY);
        }

        if (currentGame == GameType.SURVIVAL && e.getAction() == Action.RIGHT_CLICK_BLOCK
                && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.CHEST) {
            Block cb = e.getClickedBlock();
            String cKey = cb.getX() + "," + cb.getY() + "," + cb.getZ();
            if (survivalSupplyChests.remove(cKey)) {
                scoring.addIndividualPoints(p.getUniqueId(), 10, "Supply Drop Claim!");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                broadcast(ChatColor.GOLD + p.getName() + ChatColor.YELLOW + " reached a Supply Crate first! (+10 coins)");
            }
        }

        ItemStack item = e.getItem();
        if (item == null) return;

        if (currentGame == GameType.LOBBY) {
            if (item.getType() == Material.COMPASS && (p.isOp() || hub != null)) {
                cancelItemUse(e, p);
                openSelector(p);
            } else if (item.getType().name().endsWith("_WOOL")) {
                cancelItemUse(e, p);
                openTeamGui(p);
            }
            return;
        }

        if (currentGame == GameType.PARKOUR && item.getType() == Material.FEATHER) {
            cancelItemUse(e, p);
            Location cp = parkourCheckpoint.get(p.getUniqueId());
            if (cp != null) {
                p.teleport(cp);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1f);
            }
            return;
        }

        if (currentGame == GameType.DROPPER && item.getType() == Material.FEATHER) {
            cancelItemUse(e, p);
            DropperStage st = currentDropperStage(p);
            if (st != null) {
                sendToDropperStart(p, st);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.7f, 1f);
            }
            return;
        }

        if (currentGame == GameType.TNT_RUN && item.getType() == Material.FEATHER) {
            cancelItemUse(e, p);
            if (startFreezeTicks > 0 || p.getGameMode() == GameMode.SPECTATOR) return;
            int left = doubleJumpsLeft.getOrDefault(p.getUniqueId(), 0);
            if (left <= 0) {
                p.sendMessage(ChatColor.RED + "No Double Jumps remaining!");
                return;
            }
            doubleJumpsLeft.put(p.getUniqueId(), left - 1);
            if (left - 1 <= 0) p.getInventory().setItem(0, null);
            else item.setAmount(left - 1);
            Vector vel = p.getLocation().getDirection().multiply(0.45).setY(0.85);
            p.setVelocity(vel);
            p.playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.9f, 1.2f);
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 12, 0.3, 0.1, 0.3, 0.05);
            return;
        }

        if (currentGame == GameType.SPLEEF && item.getType() == Material.GOLDEN_SHOVEL) {
            cancelItemUse(e, p);
            if (startFreezeTicks > 0 || p.getGameMode() == GameMode.SPECTATOR) return;
            long now = System.currentTimeMillis();
            if (now - spleggCooldowns.getOrDefault(p.getUniqueId(), 0L) < 350L) return;
            spleggCooldowns.put(p.getUniqueId(), now);
            Snowball sb = p.launchProjectile(Snowball.class);
            sb.setVelocity(p.getLocation().getDirection().multiply(1.7));
            spleggProjectiles.add(sb.getUniqueId());
            p.playSound(p.getLocation(), Sound.ENTITY_EGG_THROW, 0.6f, 1.3f);
            return;
        }

        if ((currentGame == GameType.SURVIVAL_GAMES || currentGame == GameType.SURVIVAL) && item.getType() == Material.COMPASS) {
            cancelItemUse(e, p);
            if (currentGame == GameType.SURVIVAL && gracePeriod) {
                p.sendMessage(ChatColor.YELLOW + "Tracker offline during Grace (" + formatTime(Math.max(0, timeLeft - 600)) + " remaining).");
                return;
            }
            Player target = findNearestEnemy(p);
            if (target != null) {
                p.setCompassTarget(target.getLocation());
                int dist = (int) Math.round(target.getLocation().distance(p.getLocation()));
                p.sendMessage(ChatColor.AQUA + "Tracker: " + ChatColor.WHITE + target.getName() + ChatColor.GRAY + " is " + ChatColor.YELLOW + dist + "m away!");
            } else {
                p.sendMessage(ChatColor.YELLOW + "No living enemies found.");
            }
            return;
        }

        if (currentGame == GameType.KIT_PVP && item.getType() == Material.BOOK && startFreezeTicks > 0) {
            cancelItemUse(e, p);
            openKitGui(p);
        }
    }

    private void cancelItemUse(PlayerInteractEvent e, Player p) {
        e.setCancelled(true);
        e.setUseItemInHand(Event.Result.DENY);
        e.setUseInteractedBlock(Event.Result.DENY);
        p.updateInventory();
    }

    @EventHandler
    public void onDropItem(PlayerDropItemEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (currentGame == GameType.LOBBY && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
        } else if (currentGame == GameType.PARKOUR || currentGame == GameType.DROPPER
                || currentGame == GameType.TNT_RUN || currentGame == GameType.SPLEEF || currentGame == GameType.ONE_SHOT) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        Location to = e.getTo();
        if (to == null) return;
        Player p = e.getPlayer();

        if (currentGame == GameType.LOBBY) {
            if (hub != null && hub.isHubWorld(to.getWorld())) {
                Location hs = hub.spawn();
                if (hs != null && to.getY() < hs.getY() - 50) { p.setFallDistance(0f); p.teleport(hs); }
                return;
            }
            if (to.getWorld() == world) {
                if (to.getY() < world.getSpawnLocation().getY() - 50) {
                    p.teleport(world.getSpawnLocation().clone().add(0.5, 1.0, 0.5));
                }
            } else if (to.getWorld() != null && to.getY() < to.getWorld().getMinHeight() - 16) {
                // Not in a lobby (e.g. an op previewing a game world such as mcc_battlebox via /bb arena while no
                // game runs): never yank them back to the lobby; only rescue them from the void of that world,
                // since lobby-phase damage is cancelled and they would otherwise fall forever.
                p.setFallDistance(0f);
                p.teleport(to.getWorld().getSpawnLocation().clone().add(0.5, 1.0, 0.5));
            }
            return;
        }
        if (p.getGameMode() == GameMode.SPECTATOR || !alivePlayers.contains(p.getUniqueId())) return;

        // Freeze phase during KitPvP prep so teams stay in spawn fortresses
        if (currentGame == GameType.KIT_PVP && startFreezeTicks > 0 && e.getFrom().distanceSquared(to) > 0.01) {
            Location back = e.getFrom().clone();
            back.setYaw(to.getYaw());
            back.setPitch(to.getPitch());
            p.teleport(back);
            return;
        }

        // TNT Run (RunnonStone Andesite floor + void / lava)
        if (currentGame == GameType.TNT_RUN) {
            int voidY = roundNumber == 1 ? 0 : -1;
            Material feet = to.getBlock().getType();
            Material below = to.clone().subtract(0, 0.3, 0).getBlock().getType();
            if (to.getY() < voidY || feet == Material.LAVA || below == Material.LAVA) {
                eliminate(p, null);
                return;
            }
            if (startFreezeTicks > 0) return;

            double[] offsets = {-0.28, 0.28};
            for (double dx : offsets) {
                for (double dz : offsets) {
                    for (double dy : new double[]{-0.1, -1.0}) {
                        Block b = to.clone().add(dx, dy, dz).getBlock();
                        if (b.getType() == Material.ANDESITE) {
                            String key = b.getX() + "," + b.getY() + "," + b.getZ();
                            if (scheduledTntKeys.add(key)) {
                                new BukkitRunnable() {
                                    @Override public void run() {
                                        if (currentGame == GameType.TNT_RUN && b.getType() == Material.ANDESITE) {
                                            brokenTntBlocks.add(b.getLocation());
                                            b.setType(Material.AIR, false);
                                        }
                                    }
                                }.runTaskLater(this, 8L);
                            }
                        }
                    }
                }
            }
        }

        // Spleef elimination
        if (currentGame == GameType.SPLEEF) {
            if (to.getY() < 69 || to.getBlock().getType() == Material.LAVA) {
                eliminate(p, null);
            }
        }

        // Parkour Spiral checkpoints, hazards, and summit win
        if (currentGame == GameType.PARKOUR) {
            double maxY = Math.max(maxParkourY.getOrDefault(p.getUniqueId(), 52.0), to.getY());
            maxParkourY.put(p.getUniqueId(), maxY);
            if (to.getY() >= 65.0) climbedPastWater.put(p.getUniqueId(), true);

            Location cp = parkourCheckpoint.getOrDefault(p.getUniqueId(), to);
            boolean fellIntoVoid = to.getY() < cp.getY() - 14.0;
            boolean hitBottomWater = climbedPastWater.getOrDefault(p.getUniqueId(), false)
                    && to.getY() <= 52.0 && to.getBlock().getType() == Material.WATER;
            boolean hitLavaPit = to.clone().subtract(0, 0.2, 0).getBlock().getType() == Material.RED_GLAZED_TERRACOTTA;

            if (fellIntoVoid || hitBottomWater || hitLavaPit) {
                p.teleport(cp);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6f, 0.8f);
                return;
            }

            Block feet = to.getBlock();
            if (feet.getType().name().endsWith("_PRESSURE_PLATE")) {
                String pKey = feet.getX() + "," + feet.getY() + "," + feet.getZ();
                Set<String> seen = visitedPlates.computeIfAbsent(p.getUniqueId(), k -> new HashSet<>());
                if (seen.add(pKey)) {
                    Location saved = to.clone();
                    saved.setX(feet.getX() + 0.5);
                    saved.setZ(feet.getZ() + 0.5);
                    parkourCheckpoint.put(p.getUniqueId(), saved);
                    scoring.addIndividualPoints(p.getUniqueId(), 3, "Parkour Checkpoint");
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HARP, 0.8f, 1.5f);
                }
            }

            if (to.getY() >= 230.0 && Math.hypot(to.getX(), to.getZ()) <= 35.0 && !finishOrder.contains(p.getUniqueId())) {
                finishOrder.add(p.getUniqueId());
                int place = finishOrder.size();
                int pts = place == 1 ? 35 : place == 2 ? 25 : place == 3 ? 15 : 10;
                scoring.addIndividualPoints(p.getUniqueId(), pts, "Summit Finish #" + place + "!");
                spectateInBuiltIn(p);
                broadcast(ChatColor.GREEN + p.getName() + " reached the Parkour Summit #" + place + "! (+" + pts + " coins)");
                if (finishOrder.size() >= alivePlayers.size()) endSubRound("All players finished!");
            }
        }

        // Dropper landing detection (Water, Slime, Cobwebs, Vines) & multi-stage progression
        if (currentGame == GameType.DROPPER) {
            DropperStage cur = currentDropperStage(p);
            if (cur != null && cur.imported() != null) {
                importedDropperMove(p, cur, to);
                return;
            }
        }
        if (currentGame == GameType.DROPPER && to.getY() <= 106.0) {
            long now = System.currentTimeMillis();
            if (now - dropperAdvanceCooldown.getOrDefault(p.getUniqueId(), 0L) < 1000L) return;

            Material feetMat = to.getBlock().getType();
            Material headMat = to.clone().add(0, 0.5, 0).getBlock().getType();
            Material belowMat = to.clone().subtract(0, 0.6, 0).getBlock().getType();

            if (isSafeDropperBlock(feetMat) || isSafeDropperBlock(headMat) || isSafeDropperBlock(belowMat)
                    || (to.getY() <= 104.5 && p.isOnGround() && p.getFallDistance() < 3.0f)) {
                completeDropperStage(p);
            }
        }
    }

    /** Imported shaft: the pool at the bottom completes the stage; the void or leaving the shaft sends back to the start. */
    private void importedDropperMove(Player p, DropperStage st, Location to) {
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        ImportedDropper d = st.imported();
        if (to.getWorld() == null || dropperWorld == null || !to.getWorld().equals(dropperWorld)) return;
        if (d.outOfBounds(to.getX(), to.getY(), to.getZ())) {
            dropperRespawn(p, st, ChatColor.RED + "Out of the shaft! Back to the top of " + st.name() + ".");
            return;
        }
        int bx = to.getBlockX(), by = to.getBlockY(), bz = to.getBlockZ();
        if (!d.inPool(bx, by, bz) && !d.inPool(bx, by + 1, bz)) return;
        long now = System.currentTimeMillis();
        if (now - dropperAdvanceCooldown.getOrDefault(p.getUniqueId(), 0L) < 1000L) return;
        Block feet = to.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        boolean wet = (feet.getType() == Material.WATER && d.inPool(bx, by, bz)) || (head.getType() == Material.WATER && d.inPool(bx, by + 1, bz));
        if (wet) completeDropperStage(p);
    }

    /** Back to the start of the player's current stage (a "death" on the way down). */
    private void dropperRespawn(Player p, DropperStage st, String msg) {
        sendToDropperStart(p, st);
        if (msg != null) p.sendMessage(msg);
        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
    }

    /** The player cleared their current stage: points, then the next stage or the finish. */
    private void completeDropperStage(Player p) {
        int idx = dropperStageIdx.getOrDefault(p.getUniqueId(), 0);
        if (idx >= activeDropperStages.size()) return;
        int n = activeDropperStages.size();

        dropperAdvanceCooldown.put(p.getUniqueId(), System.currentTimeMillis());
        p.setFallDistance(0f);
        stageFinishCounts[idx]++;
        int sPlace = stageFinishCounts[idx];
        int stagePts = sPlace == 1 ? 12 : sPlace == 2 ? 8 : sPlace == 3 ? 6 : 4;
        scoring.addIndividualPoints(p.getUniqueId(), stagePts, "Dropper Stage " + (idx + 1) + "/" + n + " (#" + sPlace + ")");

        int nextIdx = idx + 1;
        dropperStageIdx.put(p.getUniqueId(), nextIdx);
        if (nextIdx >= n) {
            if (!finishOrder.contains(p.getUniqueId())) {
                finishOrder.add(p.getUniqueId());
                int overall = finishOrder.size();
                int finishPts = overall == 1 ? 25 : overall == 2 ? 15 : overall == 3 ? 10 : 5;
                scoring.addIndividualPoints(p.getUniqueId(), finishPts, "All " + n + " Stages Clear #" + overall + "!");
                spectateInBuiltIn(p);
                broadcast(ChatColor.GREEN + p.getName() + " finished all " + n + " Dropper stages (#" + overall + ")!");
                if (finishOrder.size() >= alivePlayers.size()) endSubRound("All players finished!");
            }
        } else {
            DropperStage nextStage = activeDropperStages.get(nextIdx);
            sendToDropperStart(p, nextStage);
            p.sendTitle(ChatColor.AQUA + "Stage " + (nextIdx + 1) + " / " + n + ": " + nextStage.name(), dropperSubtitle(nextStage), 5, 40, 10);
            p.sendMessage(ChatColor.AQUA + "Stage " + (nextIdx + 1) + "/" + n + " (" + nextStage.name() + ") — Aim for " + ChatColor.YELLOW + nextStage.hint() + "!"
                    + dropperCreditSuffix(nextStage));
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
        }
    }

    /**
     * Damage on an imported shaft never hurts: a landing from {@code lethal-fall} blocks or more (or any damaging fall
     * when the map sets none) onto anything but slime/honey/water, lava, fire, cactus, magma, suffocation or the void
     * sends the player back to the stage start - like a splat on a built-in stage.
     */
    private void importedDropperDamage(EntityDamageEvent e, Player p, DropperStage st) {
        e.setCancelled(true);
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        ImportedDropper d = st.imported();
        switch (e.getCause()) {
            case FALL -> {
                Material under = p.getLocation().subtract(0, 0.6, 0).getBlock().getType();
                float fell = Math.max(p.getFallDistance(), (float) e.getDamage() + 3f);
                p.setFallDistance(0f);
                if (isSafeDropperBlock(under)) return;
                if (d.lethalFall > 0 && fell < d.lethalFall) return;
                dropperRespawn(p, st, ChatColor.RED + "Splat! Land in the pool at the bottom of " + st.name() + ".");
            }
            case LAVA, FIRE, FIRE_TICK, HOT_FLOOR, CONTACT, SUFFOCATION, VOID, DROWNING, FREEZE ->
                    dropperRespawn(p, st, ChatColor.RED + "Ouch! Back to the top of " + st.name() + ".");
            default -> { }
        }
    }

    private boolean isSafeDropperBlock(Material m) {
        return m == Material.WATER || m == Material.SLIME_BLOCK || m == Material.COBWEB
                || m == Material.VINE || m == Material.TWISTING_VINES
                || m == Material.TWISTING_VINES_PLANT || m == Material.HONEY_BLOCK;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (currentGame == GameType.ONE_SHOT && e.getEntity() instanceof Arrow a) {
            a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            a.remove();
            return;
        }
        if (currentGame == GameType.SPLEEF && e.getEntity() instanceof Snowball sb) {
            if (startFreezeTicks <= 0 && e.getHitBlock() != null) {
                Block hit = e.getHitBlock();
                if (hit.getY() >= 72 && hit.getY() <= 105 && hit.getType() != Material.BEDROCK && hit.getType() != Material.BARRIER) {
                    brokenSpleefBlocks.putIfAbsent(hit.getLocation(), hit.getBlockData().clone());
                    hit.setType(Material.AIR, false);
                    hit.getWorld().playSound(hit.getLocation(), Sound.BLOCK_SNOW_BREAK, 0.7f, 1.2f);
                }
            }
            spleggProjectiles.remove(sb.getUniqueId());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (!(e.getEntity() instanceof Player p)) return;
        if (currentGame == GameType.LOBBY || currentGame == GameType.PARKOUR || startFreezeTicks > 0) {
            e.setCancelled(true);
            return;
        }
        if (currentGame == GameType.TNT_RUN) {
            if (e.getCause() == EntityDamageEvent.DamageCause.LAVA || e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                e.setCancelled(true);
                eliminate(p, null);
            } else {
                e.setCancelled(true);
            }
            return;
        }
        if (currentGame == GameType.SPLEEF) {
            if (e instanceof EntityDamageByEntityEvent edbe && edbe.getDamager() instanceof Snowball) {
                return;
            }
            if (e.getCause() == EntityDamageEvent.DamageCause.LAVA || e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                e.setCancelled(true);
                eliminate(p, null);
            } else {
                e.setCancelled(true);
            }
            return;
        }
        if (currentGame == GameType.DROPPER) {
            DropperStage cur = currentDropperStage(p);
            if (cur != null && cur.imported() != null) {
                importedDropperDamage(e, p, cur);
                return;
            }
            if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
                e.setCancelled(true);
                Material under = p.getLocation().subtract(0, 0.6, 0).getBlock().getType();
                if (isSafeDropperBlock(under)) {
                    return;
                }
                p.setFallDistance(0f);
                int idx = dropperStageIdx.getOrDefault(p.getUniqueId(), 0);
                if (idx < activeDropperStages.size()) {
                    DropperStage st = activeDropperStages.get(idx);
                    World w = dropperWorld != null ? dropperWorld : world;
                    p.teleport(new Location(w, st.x(), st.y(), st.z(), st.yaw(), 45f));
                    p.sendMessage(ChatColor.RED + "Splat! Aim for " + st.hint() + " on Stage " + (idx + 1) + "!");
                }
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            } else if (e.getCause() == EntityDamageEvent.DamageCause.LAVA || e.getCause() == EntityDamageEvent.DamageCause.FIRE || e.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
                e.setCancelled(true);
                p.setFireTicks(0);
                p.setFallDistance(0f);
                int idx = dropperStageIdx.getOrDefault(p.getUniqueId(), 0);
                if (idx < activeDropperStages.size()) {
                    DropperStage st = activeDropperStages.get(idx);
                    World w = dropperWorld != null ? dropperWorld : world;
                    p.teleport(new Location(w, st.x(), st.y(), st.z(), st.yaw(), 45f));
                }
                p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1f);
            }
            return;
        }
        if (currentGame == GameType.SURVIVAL_GAMES && gracePeriod) {
            e.setCancelled(true);
        } else if (currentGame == GameType.SURVIVAL && timeLeft > 885) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (!(e.getEntity() instanceof Player victim)) return;

        if (currentGame == GameType.SPLEEF && e.getDamager() instanceof Snowball sb) {
            if (sb.getShooter() instanceof Player shooter && !scoring.areTeammates(victim.getUniqueId(), shooter.getUniqueId()) && !shooter.equals(victim)) {
                e.setDamage(0.0);
                Vector kb = sb.getVelocity().setY(0);
                if (kb.lengthSquared() > 0.001) kb = kb.normalize().multiply(0.58);
                kb.setY(0.34);
                victim.setVelocity(kb);
                victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 0.7f, 1.1f);
            } else {
                e.setCancelled(true);
            }
            return;
        }

        Player killer = null;
        if (e.getDamager() instanceof Player dp) killer = dp;
        else if (e.getDamager() instanceof Arrow a && a.getShooter() instanceof Player sp) killer = sp;
        if (killer == null) return;

        if (e.getDamager() instanceof Player attacker && e.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            Location eye = attacker.getEyeLocation();
            BoundingBox bb = victim.getBoundingBox();
            double cx = Math.max(bb.getMinX(), Math.min(eye.getX(), bb.getMaxX()));
            double cy = Math.max(bb.getMinY(), Math.min(eye.getY(), bb.getMaxY()));
            double cz = Math.max(bb.getMinZ(), Math.min(eye.getZ(), bb.getMaxZ()));
            double dist = eye.toVector().distance(new Vector(cx, cy, cz));
            if (dist > 3.65) {
                e.setCancelled(true);
                return;
            }
        }

        if (scoring.areTeammates(victim.getUniqueId(), killer.getUniqueId()) && !killer.equals(victim)) {
            e.setCancelled(true);
            return;
        }
        if (currentGame == GameType.SURVIVAL && gracePeriod && !killer.equals(victim)) {
            e.setCancelled(true);
            killer.sendMessage(ChatColor.YELLOW + "PvP is disabled during Grace (" + formatTime(Math.max(0, timeLeft - 600)) + " remaining).");
            return;
        }
        if (currentGame == GameType.ONE_SHOT && e.getDamager() instanceof Arrow) {
            e.setDamage(100.0);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        Player victim = e.getEntity();
        e.setDeathMessage(null);
        if (currentGame == GameType.SURVIVAL) {
            e.getDrops().add(new ItemStack(Material.GOLDEN_APPLE, 1));
            e.setDroppedExp(Math.max(e.getDroppedExp(), 45));
        } else {
            e.getDrops().clear();
        }
        Player killer = victim.getKiller();

        if (currentGame == GameType.ONE_SHOT) {
            killstreaks.put(victim.getUniqueId(), 0);
            if (killer != null && !killer.equals(victim)) {
                int kills = roundKills.merge(killer.getUniqueId(), 1, Integer::sum);
                int streak = killstreaks.merge(killer.getUniqueId(), 1, Integer::sum);
                scoring.addIndividualPoints(killer.getUniqueId(), 5, "One Shot Kill");
                killer.getInventory().addItem(new ItemStack(Material.ARROW, 1));
                killer.setHealth(20.0);
                killer.playSound(killer.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1.5f);
                broadcast(ChatColor.LIGHT_PURPLE + killer.getName() + ChatColor.GRAY + " sniped " + ChatColor.WHITE + victim.getName() + ChatColor.GOLD + " (+5 coins)");
                if (streak == 3) {
                    scoring.addIndividualPoints(killer.getUniqueId(), 5, "3-Kill Streak!");
                    killer.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 15, 1, false, false));
                } else if (streak == 5) {
                    scoring.addIndividualPoints(killer.getUniqueId(), 10, "5-Kill Streak!");
                    killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
                    broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "★ " + killer.getName() + " is on a 5-KILL STREAK! ★");
                }
                if (kills >= 15) {
                    endSubRound(killer.getName() + " reached 15 kills!");
                    new BukkitRunnable() { @Override public void run() { victim.spigot().respawn(); } }.runTaskLater(this, 2L);
                    return;
                }
            }
            new BukkitRunnable() {
                @Override public void run() {
                    victim.spigot().respawn();
                    if (currentGame == GameType.ONE_SHOT) {
                        giveOneShotKit(victim);
                        teleportOneShotSpawn(victim);
                    }
                }
            }.runTaskLater(this, 2L);
            return;
        }

        new BukkitRunnable() {
            @Override public void run() {
                victim.spigot().respawn();
                eliminate(victim, killer);
            }
        }.runTaskLater(this, 2L);
    }

    private void eliminate(Player victim, Player killer) {
        if (!alivePlayers.remove(victim.getUniqueId())) return;
        spectateInBuiltIn(victim);

        if (killer != null && !killer.equals(victim)) {
            int killPts = (currentGame == GameType.SURVIVAL) ? 15 : 10;
            String killReason = (currentGame == GameType.SURVIVAL) ? "Survival Kill" : "Elimination";
            scoring.addIndividualPoints(killer.getUniqueId(), killPts, killReason);
            if (currentGame == GameType.KIT_PVP && alivePlayers.contains(killer.getUniqueId())) {
                killer.setHealth(Math.min(20.0, killer.getHealth() + 6.0));
                killer.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 1));
            } else if (currentGame == GameType.SURVIVAL && alivePlayers.contains(killer.getUniqueId())) {
                killer.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, false, false));
                killer.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 300, 0, false, false));
            }
            broadcast(ChatColor.RED + victim.getName() + ChatColor.GRAY + " was eliminated by " + ChatColor.GOLD + killer.getName() + ChatColor.YELLOW + " (+" + killPts + " coins)");
        } else {
            broadcast(ChatColor.RED + victim.getName() + ChatColor.GRAY + " was eliminated!");
        }

        // Authentic MCM split survival bonus (+4 per team, split across living teammates)
        scoring.awardSplitTeamBonus(alivePlayers, 4, "Survival Bonus");
        if (scoring.isOneSideRemaining(alivePlayers)) {
            endSubRound("Last standing!");
        }
    }

    // ==================== SPECTATORS (finished / eliminated / late joiners) ====================

    /** Overview point (above the arena, looking down) and generous watch box of a built-in game's current sub-round. */
    private McmSpectator.Zone computeBuiltInZone(GameType g) {
        try {
            switch (g) {
                case SURVIVAL_GAMES -> {
                    World w = getActiveSgWorld();
                    Location c = w.getSpawnLocation();
                    int y = findSafeGround(w, c.getBlockX(), c.getBlockZ(), (w == sgWorld ? 140 : 85), 20, c.getBlockY());
                    return overviewZone(w, c.getBlockX() + 0.5, y + 30, c.getBlockZ() + 0.5, 0f, 70f, c.getBlockX() + 0.5, c.getBlockZ() + 0.5, 170);
                }
                case PARKOUR -> {
                    World w = parkourWorld != null ? parkourWorld : world;
                    // spiral tower around (0,0), start at z=-50.7 facing +z, summit Y=230
                    return overviewZone(w, -1.0, 140, -80.0, 0f, 25f, 0, 0, 140);
                }
                case DROPPER -> {
                    World w = dropperWorld != null ? dropperWorld : world;
                    DropperStage st = activeDropperStages.isEmpty() ? ALL_DROPPER_STAGES.get(0) : activeDropperStages.get(0);
                    // above the first shaft, looking straight down it; box covers every stage (z 5038..5309)
                    double x0 = 19.5, x1 = 19.5, z0 = 5174.0 - 220, z1 = 5174.0 + 220;
                    boolean anyImported = false;
                    for (DropperStage s : activeDropperStages) {
                        if (s.imported() == null) continue;
                        anyImported = true;
                        x0 = Math.min(x0, s.imported().centreX() - 80); x1 = Math.max(x1, s.imported().centreX() + 80);
                        z0 = Math.min(z0, s.imported().centreZ() - 80); z1 = Math.max(z1, s.imported().centreZ() + 80);
                    }
                    if (!anyImported) return overviewZone(w, st.x(), st.y() + 8, st.z(), st.yaw(), 90f, 19.5, 5174.0, 220);
                    double half = Math.max(x1 - x0, z1 - z0) / 2 + 20;
                    return overviewZone(w, st.x(), st.y() + 8, st.z(), st.yaw(), 90f, (x0 + x1) / 2, (z0 + z1) / 2, half);
                }
                case TNT_RUN -> {
                    World w = tntrunWorld != null ? tntrunWorld : world;
                    double z = roundNumber == 2 ? 235.5 : roundNumber == 3 ? 375.5 : 100.5;
                    double y = roundNumber == 2 ? 8.0 : roundNumber == 3 ? 12.0 : 15.0;
                    return overviewZone(w, 100.5, y + 28, z, 0f, 75f, 100.5, z, 100);
                }
                case SPLEEF -> {
                    World w = spleefWorld != null ? spleefWorld : world;
                    return overviewZone(w, -13.5, 122, 22.5, 0f, 80f, -13.5, 22.5, 100);
                }
                case KIT_PVP -> {
                    World w = pvpWorld != null ? pvpWorld : world;
                    // spawns at z=+48.5 / -47.5, y=33; look down over the middle from the side
                    return overviewZone(w, 0.5, 62, 0.5, 90f, 60f, 0.5, 0.5, 120);
                }
                case ONE_SHOT -> {
                    World w = oneshotWorld != null ? oneshotWorld : world;
                    return overviewZone(w, 51.5, 72, 100.5, 0f, 80f, 51.5, 100.5, 80);
                }
                case SURVIVAL -> {
                    World w = survivalWorld != null ? survivalWorld : world;
                    int y = w.getHighestBlockYAt(survivalCenterX, survivalCenterZ);
                    return overviewZone(w, survivalCenterX + 0.5, y + 35, survivalCenterZ + 0.5, 0f, 60f, survivalCenterX + 0.5, survivalCenterZ + 0.5, 210);
                }
                default -> { return null; }
            }
        } catch (RuntimeException ex) {
            getLogger().log(Level.WARNING, "Could not compute the spectator overview for " + g, ex);
            return null;
        }
    }

    private static McmSpectator.Zone overviewZone(World w, double x, double y, double z, float yaw, float pitch,
                                                  double centerX, double centerZ, double half) {
        if (w == null) return null;
        return McmSpectator.Zone.around(new Location(w, x, y, z, yaw, pitch), centerX, centerZ, half);
    }

    /** Finished/eliminated/late-joined in a built-in game: spectator at the arena overview, kept inside the arena. */
    private void spectateInBuiltIn(Player p) {
        if (builtInZone == null && currentGame != GameType.LOBBY && currentGame != GameType.EXTERNAL) {
            builtInZone = computeBuiltInZone(currentGame);
        }
        McmSpectator.makeSpectator(p, builtInZone != null ? builtInZone.home().clone() : null);
        coreSpectators.add(p.getUniqueId());
    }

    /**
     * Watch zone for a default late joiner of an external game (the core does not know that game's arena): the world
     * where most active participants are, a box 96 blocks around them, home just above one of them.
     */
    private McmSpectator.Zone externalSpectatorZone() {
        if (currentGame != GameType.EXTERNAL || activeCtx == null) return null;
        List<Player> active = new ArrayList<>(), all = new ArrayList<>();
        for (UUID id : externalParticipants) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline() || coreSpectators.contains(id)) continue;
            all.add(p);
            if (p.getGameMode() != GameMode.SPECTATOR) active.add(p);
        }
        List<Player> pool = active.isEmpty() ? all : active;
        if (pool.isEmpty()) return null;
        Map<World, List<Player>> byWorld = new LinkedHashMap<>();
        for (Player p : pool) byWorld.computeIfAbsent(p.getWorld(), k -> new ArrayList<>()).add(p);
        List<Player> group = byWorld.values().stream().max(Comparator.comparingInt(List::size)).orElse(pool);
        World w = group.get(0).getWorld();
        double minX = Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxX = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Player p : group) {
            Location l = p.getLocation();
            minX = Math.min(minX, l.getX()); maxX = Math.max(maxX, l.getX());
            minZ = Math.min(minZ, l.getZ()); maxZ = Math.max(maxZ, l.getZ());
        }
        double pad = 96;
        Location home = group.get(0).getLocation().clone().add(0, 6, 0);
        home.setPitch(45f);
        BoundingBox box = new BoundingBox(minX - pad, w.getMinHeight() - 16, minZ - pad, maxX + pad, w.getMaxHeight() + 64, maxZ + pad);
        return new McmSpectator.Zone(w, box, home);
    }

    /** Once a second: core-managed spectators of an external game who left its world/area go back to it. */
    private void confineExternalCoreSpectators() {
        if (currentGame != GameType.EXTERNAL || coreSpectators.isEmpty()) return;
        McmSpectator.Zone z = externalSpectatorZone();
        if (z == null) return;
        for (UUID id : new ArrayList<>(coreSpectators)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || p.getGameMode() != GameMode.SPECTATOR || z.contains(p.getLocation())) continue;
            p.teleport(z.home().clone());
            p.sendActionBar(net.kyori.adventure.text.Component.text("You can only watch inside the game's arena.",
                    net.kyori.adventure.text.format.NamedTextColor.GRAY));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCoreSpectatorMove(PlayerMoveEvent e) {
        if (currentGame == GameType.LOBBY || currentGame == GameType.EXTERNAL) return;
        Player p = e.getPlayer();
        if (p.getGameMode() != GameMode.SPECTATOR || !coreSpectators.contains(p.getUniqueId())) return;
        Location to = e.getTo();
        McmSpectator.Zone z = builtInZone;
        if (to == null || z == null || z.home() == null || !z.world().equals(to.getWorld()) || z.contains(to)) return;
        e.setTo(z.home().clone());
        p.sendActionBar(net.kyori.adventure.text.Component.text("You can only watch inside the arena.",
                net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCoreSpectatorTeleport(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE || currentGame == GameType.LOBBY) return;
        Player p = e.getPlayer();
        if (!coreSpectators.contains(p.getUniqueId())) return;
        McmSpectator.Zone z = currentGame == GameType.EXTERNAL ? externalSpectatorZone() : builtInZone;
        if (z == null || z.contains(e.getTo())) return;
        e.setCancelled(true);
        p.sendActionBar(net.kyori.adventure.text.Component.text("You can only watch players inside this game's arena.",
                net.kyori.adventure.text.format.NamedTextColor.GRAY));
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (currentGame == GameType.SPLEEF && startFreezeTicks <= 0 && e.getPlayer().getGameMode() != GameMode.SPECTATOR) {
            Block b = e.getBlock();
            if (b.getY() >= 72 && b.getY() <= 105 && b.getType() != Material.BEDROCK && b.getType() != Material.BARRIER) {
                e.setCancelled(true);
                brokenSpleefBlocks.putIfAbsent(b.getLocation(), b.getBlockData().clone());
                b.setType(Material.AIR, false);
                b.getWorld().playSound(b.getLocation(), Sound.BLOCK_SNOW_BREAK, 0.7f, 1.2f);
                Player p = e.getPlayer();
                int count = 0;
                for (ItemStack is : p.getInventory().getContents()) {
                    if (is != null && is.getType() == Material.SNOWBALL) count += is.getAmount();
                }
                if (count < 16) p.getInventory().addItem(new ItemStack(Material.SNOWBALL, 1));
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (currentGame == GameType.SPLEEF && startFreezeTicks <= 0) {
            Block b = e.getBlock();
            if (b.getY() >= 72 && b.getY() <= 105 && b.getType() != Material.BEDROCK && b.getType() != Material.BARRIER) {
                brokenSpleefBlocks.putIfAbsent(b.getLocation(), b.getBlockData().clone());
                e.setDropItems(false);
                Player p = e.getPlayer();
                int count = 0;
                for (ItemStack is : p.getInventory().getContents()) {
                    if (is != null && is.getType() == Material.SNOWBALL) count += is.getAmount();
                }
                if (count < 16) p.getInventory().addItem(new ItemStack(Material.SNOWBALL, 1));
                return;
            }
        }
        if (currentGame == GameType.SURVIVAL && startFreezeTicks <= 0 && e.getPlayer().getGameMode() == GameMode.SURVIVAL) {
            Player p = e.getPlayer();
            Block b = e.getBlock();
            Material mat = b.getType();
            ItemStack tool = p.getInventory().getItemInMainHand();
            Location dropLoc = b.getLocation().add(0.5, 0.5, 0.5);

            if (!b.getDrops(tool, p).isEmpty()) {
                ItemStack cut1 = null, cut2 = null;
                int bonusXp = 0;
                switch (mat) {
                    case IRON_ORE, DEEPSLATE_IRON_ORE -> { cut1 = new ItemStack(Material.IRON_INGOT, 2); bonusXp = 3; }
                    case GOLD_ORE, DEEPSLATE_GOLD_ORE -> { cut1 = new ItemStack(Material.GOLD_INGOT, 2); bonusXp = 4; }
                    case COPPER_ORE, DEEPSLATE_COPPER_ORE -> { cut1 = new ItemStack(Material.COPPER_INGOT, 2); bonusXp = 2; }
                    case COAL_ORE, DEEPSLATE_COAL_ORE -> { cut1 = new ItemStack(Material.COAL, 2); cut2 = new ItemStack(Material.TORCH, 2); bonusXp = 3; }
                    case DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE -> { cut1 = new ItemStack(Material.DIAMOND, 2); bonusXp = 6; }
                    case LAPIS_ORE, DEEPSLATE_LAPIS_ORE -> { cut1 = new ItemStack(Material.LAPIS_LAZULI, 8); bonusXp = 5; }
                    case REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE -> { cut1 = new ItemStack(Material.REDSTONE, 6); bonusXp = 5; }
                    case EMERALD_ORE, DEEPSLATE_EMERALD_ORE -> { cut1 = new ItemStack(Material.EMERALD, 2); bonusXp = 6; }
                    default -> {}
                }
                if (cut1 != null) {
                    e.setDropItems(false);
                    b.getWorld().dropItemNaturally(dropLoc, cut1);
                    if (cut2 != null) b.getWorld().dropItemNaturally(dropLoc, cut2);
                    e.setExpToDrop(e.getExpToDrop() + bonusXp);
                    return;
                }
            }
            if (mat == Material.GRAVEL && rng.nextDouble() < 0.50) {
                e.setDropItems(false);
                b.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.FLINT, 1));
                return;
            }
            if (mat.name().endsWith("_LEAVES") && rng.nextDouble() < 0.12) {
                b.getWorld().dropItemNaturally(dropLoc, new ItemStack(Material.APPLE, 1));
                return;
            }
            if (mat.name().endsWith("_LOG") && !p.isSneaking() && tool != null && tool.getType().name().endsWith("_AXE")) {
                chopConnectedLogs(b, mat, 32);
            }
            return;
        }
        if (currentGame != GameType.LOBBY || e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
        }
    }

    private void chopConnectedLogs(Block start, Material logMat, int limit) {
        Set<Block> visited = new HashSet<>();
        Deque<Block> queue = new ArrayDeque<>();
        visited.add(start);
        queue.add(start);
        int chopped = 0;
        while (!queue.isEmpty() && chopped < limit) {
            Block cur = queue.poll();
            for (int dy = 0; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block nb = cur.getRelative(dx, dy, dz);
                        if (nb.getType() == logMat && visited.add(nb)) {
                            queue.add(nb);
                            nb.setType(Material.AIR, false);
                            nb.getWorld().dropItemNaturally(nb.getLocation().add(0.5, 0.5, 0.5), new ItemStack(logMat, 1));
                            chopped++;
                            if (chopped >= limit) return;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (currentGame != GameType.SURVIVAL || e.getEntity() instanceof Player) return;
        List<ItemStack> drops = e.getDrops();
        for (int i = 0; i < drops.size(); i++) {
            ItemStack is = drops.get(i);
            if (is == null) continue;
            Material cooked = switch (is.getType()) {
                case BEEF -> Material.COOKED_BEEF;
                case PORKCHOP -> Material.COOKED_PORKCHOP;
                case MUTTON -> Material.COOKED_MUTTON;
                case CHICKEN -> Material.COOKED_CHICKEN;
                case RABBIT -> Material.COOKED_RABBIT;
                default -> null;
            };
            if (cooked != null) {
                drops.set(i, new ItemStack(cooked, is.getAmount()));
            }
        }
        Entity ent = e.getEntity();
        if (ent instanceof Cow) drops.add(new ItemStack(Material.LEATHER, 1));
        else if (ent instanceof Chicken) drops.add(new ItemStack(Material.FEATHER, 2));
        else if (ent instanceof Spider || ent instanceof CaveSpider) drops.add(new ItemStack(Material.STRING, 2));
        else if (ent instanceof Skeleton) drops.add(new ItemStack(Material.ARROW, 4));
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (currentGame == GameType.SURVIVAL && startFreezeTicks <= 0) {
            if (e.getBlockPlaced().getY() > 240) {
                e.setCancelled(true);
                e.getPlayer().sendMessage(ChatColor.RED + "Build height cap in Speed Survival is Y=240!");
            }
            return;
        }
        if (currentGame != GameType.LOBBY || e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
            if (currentGame == GameType.LOBBY) {
                e.getPlayer().updateInventory();
            }
        }
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (!(e.getEntity() instanceof Player p)) return;
        if (currentGame == GameType.LOBBY && p.getGameMode() == GameMode.CREATIVE) return;
        if (currentGame != GameType.SURVIVAL_GAMES && currentGame != GameType.KIT_PVP && currentGame != GameType.SURVIVAL) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (currentGame != GameType.SURVIVAL_GAMES && currentGame != GameType.KIT_PVP && currentGame != GameType.SURVIVAL) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
        if (currentGame == GameType.EXTERNAL) return; // external games handle their own rules
        if (currentGame == GameType.SURVIVAL) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "Nether & End portals are disabled in Speed Survival!");
        }
    }

    // ==================== SCOREBOARD, BOSSBAR & HELPERS ====================

    private int findSafeGround(World w, int x, int z, int topY, int minY, int fallbackY) {
        for (int y = topY; y >= minY; y--) {
            Material m = w.getBlockAt(x, y, z).getType();
            String mn = m.name();
            if ((m.isSolid() || m == Material.WATER)
                    && !mn.contains("GLASS") && !mn.contains("BARRIER")
                    && !mn.contains("LOG") && !mn.contains("LEAVES") && !mn.contains("WOOD")
                    && m != Material.BEDROCK) {
                if (w.getBlockAt(x, y + 1, z).isPassable() && w.getBlockAt(x, y + 2, z).isPassable()) {
                    return y + 1;
                }
            }
        }
        return fallbackY + 1;
    }

    private void printLeaderboard() {
        broadcast(ChatColor.YELLOW + "--- Holistic Standings ---");
        List<Map.Entry<String, Integer>> top = scoring.getStandings();
        for (int i = 0; i < Math.min(5, top.size()); i++) {
            broadcast(ChatColor.GOLD + "#" + (i + 1) + " " + top.get(i).getKey() + ChatColor.GRAY + " - " + ChatColor.GREEN + top.get(i).getValue() + " coins");
        }
    }

    private String formatTime(int sec) {
        if (sec >= 60) {
            int m = sec / 60;
            int s = sec % 60;
            return m + ":" + (s < 10 ? "0" : "") + s;
        }
        return sec + "s";
    }

    private void updateAllScoreboards() {
        ScoreboardManager sm = Bukkit.getScoreboardManager();
        if (sm == null) return;
        boolean lobby = currentGame == GameType.LOBBY;
        if (bossBar != null) {
            String ev = getConfig().getString("hud.event-name", "MINECRAFT CHAMPIONSHIPS").toUpperCase(Locale.ROOT);
            String title = lobby
                    ? ChatColor.YELLOW + "" + ChatColor.BOLD + ev
                    : ChatColor.YELLOW + "TIME LEFT: " + ChatColor.WHITE + String.format(Locale.ROOT, "%02d:%02d", Math.max(0, timeLeft) / 60, Math.max(0, timeLeft) % 60);
            bossBar.setTitle(title);
            bossBar.setProgress(lobby ? 1.0 : Math.max(0.0, Math.min(1.0, (double) timeLeft / Math.max(1, totalDuration))));
        }
        boolean externalOwnBoard = currentGame == GameType.EXTERNAL && activeExternal != null && !activeExternal.useCoreScoreboard();
        if (externalOwnBoard && bossBar != null) bossBar.removeAll();
        McmHud.State state = hudState();
        float progress = lobby ? 1f : (float) Math.max(0.0, Math.min(1.0, (double) timeLeft / Math.max(1, totalDuration)));
        boolean tabNames = (++hudTicks % 5) == 1;
        for (Player p : Bukkit.getOnlinePlayers()) {
            hud.tab(p, state);
            if (tabNames) hud.tabName(p);
            if (externalOwnBoard) { hud.bossBar(p, false, "", 0, 0f); continue; }
            boolean pack = McmFx.hasPack(p);
            if (bossBar != null) {
                if (pack) bossBar.removePlayer(p);
                else if (!bossBar.getPlayers().contains(p)) bossBar.addPlayer(p);
            }
            hud.bossBar(p, true, lobby ? state.event() : "TIME LEFT", lobby ? -1 : Math.max(0, timeLeft), progress);
            hud.sidebar(p, state);
        }
    }

    private int hudTicks;

    /** Snapshot of the event for the MCC-style HUD (sidebar, tab, boss bar). */
    private McmHud.State hudState() {
        boolean lobby = currentGame == GameType.LOBBY;
        String event = getConfig().getString("hud.event-name", "MINECRAFT CHAMPIONSHIPS");
        String map = null;
        List<String> lines = new ArrayList<>();
        if (!lobby) {
            if (currentGame == GameType.EXTERNAL && activeExternal != null) {
                map = activeExternal.currentMap();
                lines.addAll(externalSidebar);
            } else {
                map = feedbackMapId();
            }
            if (currentGame == GameType.SURVIVAL) {
                lines.add(ChatColor.GRAY + "Phase: " + (gracePeriod ? "Grace " + formatTime(Math.max(0, timeLeft - 600))
                        : survivalDeathmatch ? "Deathmatch" : "Hunt"));
                int halfB = survivalWorld != null ? (int) Math.round(survivalWorld.getWorldBorder().getSize() / 2.0) : 160;
                lines.add(ChatColor.GRAY + "Border: " + ChatColor.WHITE + "±" + halfB);
                lines.add(ChatColor.GRAY + "Players remaining: " + ChatColor.WHITE + alivePlayers.size());
            }
        }
        if (map != null) map = map.replace('_', ' ').replace('-', ' ');
        if ("default".equalsIgnoreCase(map)) map = null;
        LinkedHashMap<String, Integer> totals = overallSideTotals();
        for (Player p : Bukkit.getOnlinePlayers()) if (isEventPlayer(p)) totals.putIfAbsent(scoring.getSideKey(p.getUniqueId()), 0);
        Map<String, Integer> coins = new HashMap<>();
        if (lobby) {
            coins.putAll(totals);
        } else {
            for (String k : totals.keySet()) coins.put(k, 0);
            for (Map.Entry<UUID, Integer> e : scoring.getIndividualStandings()) {
                int d = e.getValue() - gameStartPoints.getOrDefault(e.getKey(), 0);
                if (d != 0) coins.merge(scoring.getSideKey(e.getKey()), d, Integer::sum);
            }
        }
        LinkedHashMap<String, Integer> ranked = new LinkedHashMap<>();
        coins.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> ranked.put(e.getKey(), e.getValue()));
        int gameNo = championshipMode ? Math.min(playlist.size(), playlistIndex + 1) : 0;
        return new McmHud.State(event, lobby ? null : currentDisplay(), gameNo, championshipMode ? playlist.size() : 0, map,
                roundNumber, maxRounds, lines, ranked, !lobby, Bukkit.getOnlinePlayers().size(), lobby ? 1.0 : currentCoinMultiplier());
    }

    private void resetPlayer(Player p, GameMode gm) {
        p.leaveVehicle();
        p.setGameMode(gm);
        if (gm != GameMode.CREATIVE && gm != GameMode.SPECTATOR) {
            p.setAllowFlight(false);
            p.setFlying(false);
        }
        sanitizeAttributes(p);
        p.getInventory().clear();
        p.setHealth(20.0);
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        for (PotionEffect pe : p.getActivePotionEffects()) p.removePotionEffect(pe.getType());
    }

    private void sanitizeAttributes(Player p) {
        // Leave GENERIC_ATTACK_SPEED untouched so OldCombatMechanics keeps 1.8.9 no-cooldown PvP working
        setAttr(p, Attribute.GENERIC_SCALE, 1.0);
        setAttr(p, Attribute.GENERIC_MOVEMENT_SPEED, 0.1);
        setAttr(p, Attribute.GENERIC_JUMP_STRENGTH, 0.42);
        setAttr(p, Attribute.GENERIC_GRAVITY, 0.08);
        setAttr(p, Attribute.GENERIC_STEP_HEIGHT, 0.6);
        setAttr(p, Attribute.GENERIC_MAX_HEALTH, 20.0);
        setAttr(p, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0.0);
        setAttr(p, Attribute.GENERIC_ATTACK_DAMAGE, 1.0);
        setAttr(p, Attribute.PLAYER_ENTITY_INTERACTION_RANGE, 3.0);
        setAttr(p, Attribute.PLAYER_BLOCK_INTERACTION_RANGE, 4.5);
        p.setWalkSpeed(0.2f);
        p.setFlySpeed(0.1f);
    }

    private void setAttr(Player p, Attribute attr, double base) {
        AttributeInstance inst = p.getAttribute(attr);
        if (inst == null) return;
        for (org.bukkit.attribute.AttributeModifier mod : new ArrayList<>(inst.getModifiers())) {
            inst.removeModifier(mod);
        }
        inst.setBaseValue(base);
    }

    private void refreshLobbyItems() {
        if (currentGame != GameType.LOBBY) return;
        for (Player p : Bukkit.getOnlinePlayers()) giveLobbyItems(p);
    }

    private void giveLobbyItems(Player p) {
        String t = scoring.getTeam(p.getUniqueId());
        p.getInventory().setItem(0, makeItem(scoring.teamWool(t),
                ChatColor.AQUA + "" + ChatColor.BOLD + "Team Selector (Right-Click)",
                ChatColor.GRAY + "Current: " + (t == null ? ChatColor.WHITE + "Solo" : scoring.teamColor(t) + t + " Team"),
                ChatColor.YELLOW + "Max Team Size: " + scoring.getMaxTeamSize()));
        if (p.isOp()) {
            p.getInventory().setItem(4, makeItem(Material.COMPASS,
                    ChatColor.GOLD + "" + ChatColor.BOLD + "Tournament Controller (Right-Click)",
                    ChatColor.GRAY + "Start events, skip, toggle team size, reset",
                    hub != null ? ChatColor.GRAY + "Also travels between the MM lobby and the MCC hub" : ""));
        } else if (hub != null) {
            p.getInventory().setItem(4, makeItem(Material.COMPASS,
                    ChatColor.AQUA + "" + ChatColor.BOLD + "Lobby Selector (Right-Click)",
                    ChatColor.GRAY + "Travel between the MM lobby and the MCC hub"));
        }
    }

    private ItemStack makeItem(Material m, String name, String... lore) {
        ItemStack is = new ItemStack(m);
        ItemMeta im = is.getItemMeta();
        im.setDisplayName(name);
        im.setLore(Arrays.asList(lore));
        im.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        is.setItemMeta(im);
        return is;
    }

    // ==================== EXTERNAL MCC GAMES (com.mcm.api) ====================

    private String builtInId(GameType gt) {
        return switch (gt) {
            case SURVIVAL_GAMES -> "sg";
            case PARKOUR -> "parkour";
            case DROPPER -> "dropper";
            case TNT_RUN -> "tntrun";
            case SPLEEF -> "spleef";
            case KIT_PVP -> "kitpvp";
            case SURVIVAL -> "survival";
            case ONE_SHOT -> "oneshot";
            default -> gt.name().toLowerCase();
        };
    }

    private String currentDisplay() {
        if (currentGame == GameType.EXTERNAL && activeExternal != null) return activeExternal.displayName();
        return currentGame.display;
    }

    private List<String> allGameIds() {
        List<String> ids = new ArrayList<>(List.of("sg", "parkour", "dropper", "tntrun", "spleef", "kitpvp", "survival", "oneshot"));
        for (McmGame g : McmApi.games()) if (!hiddenGame(g.id())) ids.add(g.id());
        return ids;
    }

    /** Games installed but kept out of the menus, the map picker and championship playlists (config hidden-games). */
    private boolean hiddenGame(String id) {
        if (id == null) return false;
        for (String h : getConfig().getStringList("hidden-games")) if (h.equalsIgnoreCase(id)) return true;
        return false;
    }

    /** Lobby new players spawn in: the MCC hub (Block Showdown) unless default-lobby is "mm" or the hub is not built. */
    private Location defaultLobby() {
        if (!"mm".equalsIgnoreCase(getConfig().getString("default-lobby", "mcc")) && hubReady()) return hub.spawn();
        return world.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
    }

    private List<String> buildPlaylist() {
        List<String> configured = getConfig().getStringList("championship-playlist");
        List<String> out = new ArrayList<>();
        if (configured.isEmpty()) {
            out.addAll(DEFAULT_BUILT_IN_PLAYLIST);
            for (McmGame g : mmExternalGames()) out.add(g.id().toLowerCase(Locale.ROOT)); // MM-group externals (e.g. findthebutton)
            for (McmGame g : mccGames()) out.add(g.id());
        } else {
            for (String id : configured) {
                if (hiddenGame(id)) getLogger().info("championship-playlist: '" + id + "' is in hidden-games - skipped");
                else if (parseGame(id) != null || McmApi.game(id) != null) out.add(id.toLowerCase());
                else getLogger().warning("championship-playlist: unknown game '" + id + "' (is its plugin installed?) - skipped");
            }
        }
        return finaleLast(out);
    }

    private boolean isFinale(String id) {
        String f = getConfig().getString("finale-game", "dodgebolt");
        return id != null && f != null && !f.isBlank() && f.equalsIgnoreCase(id);
    }

    /** Moves the finale game (Dodgebolt) to the end of a playlist, if it is in it. */
    private List<String> finaleLast(List<String> list) {
        List<String> fin = new ArrayList<>();
        list.removeIf(id -> { if (isFinale(id)) { fin.add(id); return true; } return false; });
        if (!fin.isEmpty()) list.add(fin.get(0));
        return list;
    }

    private void listGames(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "Built-in: " + ChatColor.WHITE + "sg, parkour, dropper, tntrun, spleef, kitpvp, survival, oneshot");
        for (McmGame g : mmExternalGames()) {
            s.sendMessage(ChatColor.GREEN + g.id() + ChatColor.GRAY + " - " + ChatColor.WHITE + g.displayName() + ChatColor.GREEN + " (Minecraft Monday game)");
        }
        if (mccGames().isEmpty()) {
            s.sendMessage(ChatColor.GRAY + "No MCC game plugins installed.");
        } else {
            for (McmGame g : mccGames()) {
                s.sendMessage(ChatColor.AQUA + g.id() + ChatColor.GRAY + " - " + ChatColor.WHITE + g.displayName()
                        + (g.description().isEmpty() ? "" : ChatColor.DARK_GRAY + " (" + g.description() + ")"));
            }
        }
        s.sendMessage(ChatColor.GRAY + "Championship order: " + ChatColor.WHITE + String.join(" > ", buildPlaylist()));
    }

    /** Ids (lowercase) of registered external games that belong to the Minecraft Monday group, from config. */
    private boolean isMmExternal(String id) {
        if (id == null) return false;
        String l = id.toLowerCase(Locale.ROOT);
        for (String s : getConfig().getStringList("minecraft-monday-external-games")) if (s.toLowerCase(Locale.ROOT).equals(l)) return true;
        return false;
    }

    /** Registered external games that are Minecraft Monday games (shown/run with the 7 built-ins, MM lobby). */
    private List<McmGame> mmExternalGames() {
        List<McmGame> out = new ArrayList<>();
        for (McmGame g : McmApi.games()) if (isMmExternal(g.id()) && !hiddenGame(g.id())) out.add(g);
        return out;
    }

    /** Registered external games that are MCC games (MCC hub, MCC championship, Decision Dome). */
    private List<McmGame> mccGames() {
        List<McmGame> out = new ArrayList<>();
        for (McmGame g : McmApi.games()) if (!isMmExternal(g.id()) && !hiddenGame(g.id())) out.add(g);
        return out;
    }

    private void openExtraGamesGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 54, EXTRA_GUI_TITLE);
        int i = 0;
        for (McmGame g : mccGames()) {
            if (i >= 54) break;
            inv.setItem(i++, makeItem(g.icon(), ChatColor.AQUA + "" + ChatColor.BOLD + g.displayName(),
                    ChatColor.GRAY + g.description(), ChatColor.DARK_GRAY + "/mcm play " + g.id(), ChatColor.YELLOW + "Click to play"));
        }
        p.openInventory(inv);
    }

    /** Starts a built-in or external game by id. Returns false if the id is unknown. */
    private boolean startById(String id) {
        McmGame ext = McmApi.game(id);
        if (!championshipMode) mccContext = ext != null && !isMmExternal(ext.id());
        if (ext != null) { startExternal(ext); return true; }
        GameType gt = parseGame(id);
        if (gt == null) return false;
        startEvent(gt);
        return true;
    }

    /**
     * MCC event multiplier for an external game: its slot among the championship's coin games (games in
     * coins.event-multiplier.no-coin-games do not count), x1.0 when disabled, and
     * coins.event-multiplier.outside-championship for a single /mcm play game.
     */
    double computeCoinMultiplier(String gameId) {
        if (!getConfig().getBoolean("coins.event-multiplier.enabled", true)) return 1.0;
        Set<String> noCoin = new HashSet<>();
        for (String s : getConfig().getStringList("coins.event-multiplier.no-coin-games")) noCoin.add(s.toLowerCase(Locale.ROOT));
        String id = gameId == null ? "" : gameId.toLowerCase(Locale.ROOT);
        if (noCoin.contains(id)) return 1.0;
        if (!championshipMode || playlist.isEmpty()) {
            double m = getConfig().getDouble("coins.event-multiplier.outside-championship", 1.0);
            return m > 0 ? m : 1.0;
        }
        int games = 0, index = -1;
        for (int i = 0; i < playlist.size(); i++) {
            if (noCoin.contains(playlist.get(i).toLowerCase(Locale.ROOT))) continue;
            if (i == playlistIndex) index = games;
            games++;
        }
        if (index < 0) return 1.0;
        double[] eight = McmCoins.toArray(getConfig().getList("coins.event-multiplier.eight-games"), McmCoins.MCC_EIGHT);
        double[] six = McmCoins.toArray(getConfig().getList("coins.event-multiplier.six-games"), McmCoins.MCC_SIX);
        return McmCoins.multiplier(index, games, eight, six);
    }

    /** Current game's coin multiplier (x1.0 in the lobby and in built-in games). */
    double currentCoinMultiplier() {
        return currentGame == GameType.EXTERNAL ? coinMultiplier : 1.0;
    }

    private void startExternal(McmGame g) {
        if (mapEdit != null) mapEdit.evictAll("A game is starting.");
        if (!championshipMode) mccContext = !isMmExternal(g.id()); // MM-group externals use the MM lobby/venue
        stopExternalIfActive();
        cancelTasks();
        restoreArenas();
        roundNumber = 1;
        maxRounds = 1;
        startFreezeTicks = 0;
        timeLeft = 0;
        totalDuration = 1;
        alivePlayers.clear();
        finishOrder.clear();
        externalSidebar.clear();
        externalParticipants.clear();
        coreSpectators.clear();
        builtInZone = null;
        currentGame = GameType.EXTERNAL;
        activeExternal = g;
        scoring.snapshotTeamSizes();
        externalRoster.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            scoring.ensurePlayer(p.getUniqueId());
            if (!isEventPlayer(p)) {
                // staff, or no team during a championship: never a side, they watch
                resetPlayer(p, GameMode.SPECTATOR);
                coreSpectators.add(p.getUniqueId());
                p.sendMessage(ChatColor.YELLOW + (isStaff(p) ? "You are staff/spectator: watching this game."
                        : "You have no team, so you are spectating. Ask an admin to put you on a team."));
                continue;
            }
            externalParticipants.add(p.getUniqueId());
            externalRoster.add(p.getUniqueId());
            resetPlayer(p, GameMode.SURVIVAL);
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            double f = externalParticipants.contains(p.getUniqueId()) ? scoring.teamCoinFactor(p.getUniqueId()) : 1.0;
            if (f > 1.0) p.sendMessage(ChatColor.AQUA + "Team bonus " + ChatColor.YELLOW + TeamScoring.formatFactor(f) + ChatColor.GRAY
                    + " - your team has " + scoring.teamSizeNow(scoring.getTeam(p.getUniqueId())) + " of " + scoring.getMaxTeamSize()
                    + " players, so every coin you earn is multiplied.");
        }
        snapshotGamePoints();
        String cardSub = championshipMode && !playlist.isEmpty()
                ? ChatColor.YELLOW + "Round " + Math.min(playlistIndex + 1, playlist.size()) + " of " + playlist.size()
                : ChatColor.YELLOW + "Get Ready!";
        McmFx.titleCard(Bukkit.getOnlinePlayers(), ChatColor.GOLD + "" + ChatColor.BOLD + g.displayName(), cardSub);
        McmFx.sound(Bukkit.getOnlinePlayers(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.4f, 1.3f);
        McmFx.sound(Bukkit.getOnlinePlayers(), Sound.BLOCK_BELL_USE, 0.9f, 0.8f);
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + ">>> " + ChatColor.WHITE + g.displayName() + ChatColor.GOLD + " <<<");
        coinMultiplier = computeCoinMultiplier(g.id());
        if (coinMultiplier != 1.0) broadcast(ChatColor.AQUA + "Game Coins: " + ChatColor.YELLOW + "(" + McmCoins.formatMultiplier(coinMultiplier) + ")"
                + ChatColor.GRAY + " - every coin in this game is multiplied.");
        ExternalContext ctx = new ExternalContext(g);
        activeCtx = ctx;
        StringBuilder sideLog = new StringBuilder();
        for (Map.Entry<String, List<Player>> se : ctx.sides().entrySet()) {
            sideLog.append(sideLog.length() > 0 ? ", " : "").append(se.getKey()).append('=').append(se.getValue().size())
                    .append('/').append(ctx.registeredTeamSize(se.getKey()));
        }
        getLogger().info("Game " + g.id() + " start: " + externalParticipants.size() + " participants in " + ctx.sides().size()
                + " sides [" + sideLog + "] (online/registered), " + coreSpectators.size() + " spectating");
        try {
            g.start(ctx);
        } catch (Throwable t) {
            getLogger().log(Level.SEVERE, "Game '" + g.id() + "' crashed on start", t);
            ctx.finish("Error starting " + g.displayName());
        }
    }

    private void stopExternalIfActive() {
        if (activeExternal == null) return;
        McmGame g = activeExternal;
        activeExternal = null;
        if (activeCtx != null) activeCtx.done = true;
        activeCtx = null;
        try { g.stop(); } catch (Throwable t) { getLogger().log(Level.SEVERE, "Game '" + g.id() + "' failed to stop cleanly", t); }
    }

    @EventHandler
    public void onPluginDisable(PluginDisableEvent e) {
        if (e.getPlugin() == this) return;
        boolean ownsActive = activeExternal != null
                && activeExternal.getClass().getClassLoader() == e.getPlugin().getClass().getClassLoader();
        McmApi.unregisterAll(e.getPlugin());
        if (ownsActive) {
            activeExternal = null;
            if (activeCtx != null) activeCtx.done = true;
            activeCtx = null;
            championshipMode = false;
            returnToLobby(ChatColor.RED + "The running game's plugin was disabled. Returned to lobby.");
        }
    }

    private final class ExternalContext implements McmGameContext {
        private final McmGame game;
        boolean done = false;

        ExternalContext(McmGame game) { this.game = game; }

        @Override public Collection<Player> participants() {
            List<Player> out = new ArrayList<>();
            for (UUID id : externalParticipants) {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) out.add(p);
            }
            return out;
        }

        @Override public String teamOf(UUID player) { return scoring.getTeam(player); }

        @Override public ChatColor teamColor(String team) { return scoring.teamColor(team); }

        @Override public Map<String, List<Player>> sides() {
            Map<String, List<Player>> m = new LinkedHashMap<>();
            for (Player p : participants()) m.computeIfAbsent(scoring.getSideKey(p.getUniqueId()), k -> new ArrayList<>()).add(p);
            return m;
        }

        @Override public String sideLabel(String sideKey) {
            if (sideKey == null) return "?";
            if (sideKey.startsWith("TEAM:")) {
                String t = sideKey.substring(5);
                return scoring.teamColor(t) + t + " Team";
            }
            if (sideKey.startsWith("SOLO:")) {
                try { return ChatColor.WHITE + scoring.playerName(UUID.fromString(sideKey.substring(5))); }
                catch (IllegalArgumentException ex) { return sideKey; }
            }
            return sideKey;
        }

        @Override public int sidePoints(String sideKey) {
            if (sideKey == null) return 0;
            if (sideKey.startsWith("TEAM:")) return scoring.getTeamPoints(sideKey.substring(5));
            if (sideKey.startsWith("SOLO:")) {
                try { return scoring.getPlayerPoints(UUID.fromString(sideKey.substring(5))); } catch (IllegalArgumentException ex) { return 0; }
            }
            return 0;
        }

        @Override public List<String> topSides(int n) {
            List<String> keys = new ArrayList<>(sides().keySet());
            keys.sort((a, b) -> Integer.compare(sidePoints(b), sidePoints(a)));
            return n > 0 && keys.size() > n ? new ArrayList<>(keys.subList(0, n)) : keys;
        }

        @Override public void addPoints(UUID player, int points, String reason) {
            if (points != 0) addCoins(player, points, reason);   // negative = take back (e.g. a skipped round)
        }

        @Override public void addSidePoints(String sideKey, int points, String reason) {
            if (done || points == 0) return;
            List<UUID> members = sideMembers(sideKey);
            if (members.isEmpty()) return;
            double each = points / (double) members.size();
            // A side total is a pool for the whole side (a team bonus, a team-wide objective): the short-team factor
            // makes a trio's total equal a full team's, then it is split per member. Games that already paid a pool
            // through addCoins per member must not use this path (they would be scaled twice).
            if (points > 0) each *= scoring.teamCoinFactor(members.get(0));
            for (UUID u : members) pay(u, each, reason, false);
        }

        @Override public void addCoins(UUID player, double coins, String reason) {
            pay(player, coins, reason, true);
        }

        private void pay(UUID player, double coins, String reason, boolean teamBonus) {
            if (done || player == null || coins == 0) return;
            scoring.addCoins(player, coins * coinMultiplier, reason, teamBonus);
        }

        @Override public double teamCoinFactor(UUID player) { return scoring.teamCoinFactor(player); }

        @Override public int registeredTeamSize(String sideKey) { return scoring.registeredSizeOfSide(sideKey); }

        @Override public int fullTeamSize() { return scoring.getMaxTeamSize(); }

        @Override public void addKillCoins(UUID killer, double coins, UUID victim, String victimName, String reason) {
            if (done || killer == null) return;
            addCoins(killer, coins, reason);
            Player kp = Bukkit.getPlayer(killer);
            if (kp != null) CoinFeedback.kill(kp, victim, victimName, victim == null ? null : scoring.getTeam(victim), reason);
        }

        @Override public void addObjectiveCoins(UUID player, double coins, String banner, String reason) {
            if (done || player == null) return;
            addCoins(player, coins, reason);   // 0 coins: no chat line, no ding, but the banner still shows
            Player p = Bukkit.getPlayer(player);
            if (p != null && banner != null && !banner.isBlank()) CoinFeedback.objective(p, banner, reason);
        }

        @Override public void addTeamCoins(String sideKey, double pool, String reason) {
            if (done || pool == 0) return;
            List<UUID> members = sideMembers(sideKey);
            if (members.isEmpty()) return;
            double each = teamShare(pool, members.size());
            // "mcc": pool / 4 per member, scaled by the short-team factor so a trio still earns the whole pool.
            // "team": pool / online members is already the whole pool, so no factor.
            boolean teamSplit = "team".equalsIgnoreCase(getConfig().getString("coins.team-pool-split", "mcc"));
            for (UUID u : members) pay(u, each, reason, !teamSplit);
        }

        @Override public double teamShare(double pool, int members) {
            if (members <= 0) return 0;
            boolean teamSplit = "team".equalsIgnoreCase(getConfig().getString("coins.team-pool-split", "mcc"));
            int mccSize = Math.max(1, getConfig().getInt("coins.mcc-team-size", 4));
            return pool / (teamSplit ? members : mccSize);
        }

        @Override public double coinMultiplier() { return coinMultiplier; }

        @Override public void setEventWinner(String sideKey) {
            if (!done && sideKey != null && championshipMode) declaredChampionSide = sideKey;
        }

        private List<UUID> sideMembers(String sideKey) {
            List<UUID> members = new ArrayList<>();
            if (sideKey == null) return members;
            for (Player p : participants()) {
                if (scoring.getSideKey(p.getUniqueId()).equals(sideKey)) members.add(p.getUniqueId());
            }
            return members;
        }

        @Override public void broadcast(String message) { MinecraftMondayPlugin.this.broadcast(message); }

        @Override public Location lobby() { return lobbyLocation(); }

        @Override public void setTimer(int secondsLeft, int totalSeconds) {
            if (done) return;
            timeLeft = Math.max(0, secondsLeft);
            totalDuration = Math.max(1, totalSeconds);
        }

        @Override public void setSidebarLines(List<String> lines) {
            if (done) return;
            externalSidebar.clear();
            if (lines != null) externalSidebar.addAll(lines);
        }

        @Override public void finish(String reason) {
            if (done) return;
            done = true;
            // Synchronous on purpose: games clear their arena right after finish(), so players
            // must already be back in the lobby when this returns. game.stop() is NOT called.
            Runnable end = () -> {
                if (activeExternal == game) {
                    activeExternal = null;
                    activeCtx = null;
                    finishedNameOverride = game.displayName();
                    endCurrentEvent(reason == null ? "Finished" : reason);
                }
            };
            if (Bukkit.isPrimaryThread()) end.run();
            else Bukkit.getScheduler().runTask(MinecraftMondayPlugin.this, end);
        }
    }

    // ==================== 3.2.0 PRESENTATION: PLACEMENTS, PODIUM, PREVIEW, FX ====================

    /** Remember everyone's coins at the start of an event so the end-of-event reveal can rank this event only. */
    private void snapshotGamePoints() {
        gameStartPoints.clear();
        for (Map.Entry<UUID, Integer> e : scoring.getIndividualStandings()) gameStartPoints.put(e.getKey(), e.getValue());
    }

    /** Sum of points per side key, highest first. */
    private LinkedHashMap<String, Integer> overallSideTotals() {
        Map<String, Integer> m = new HashMap<>();
        for (Map.Entry<UUID, Integer> e : scoring.getIndividualStandings()) {
            m.merge(scoring.getSideKey(e.getKey()), e.getValue(), Integer::sum);
        }
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        m.entrySet().stream().sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .forEach(e -> out.put(e.getKey(), e.getValue()));
        return out;
    }

    /** Top 3 sides by points earned in the event that just ended; falls back to the top 3 overall. */
    private void computeGamePlacements(List<String> keys, List<String> details) {
        Map<String, Integer> delta = new HashMap<>();
        for (Map.Entry<UUID, Integer> e : scoring.getIndividualStandings()) {
            int d = e.getValue() - gameStartPoints.getOrDefault(e.getKey(), 0);
            if (d != 0) delta.merge(scoring.getSideKey(e.getKey()), d, Integer::sum);
        }
        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(delta.entrySet());
        ranked.removeIf(e -> e.getValue() <= 0);
        ranked.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (!ranked.isEmpty()) {
            for (int i = 0; i < Math.min(3, ranked.size()); i++) {
                keys.add(ranked.get(i).getKey());
                details.add(ChatColor.GREEN + "+" + ranked.get(i).getValue() + " coins");
            }
            return;
        }
        int i = 0;
        for (Map.Entry<String, Integer> e : overallSideTotals().entrySet()) {
            if (i++ >= 3 || e.getValue() <= 0) break;
            keys.add(e.getKey());
            details.add(ChatColor.GREEN + "" + e.getValue() + " coins total");
        }
    }

    private boolean podiumAreaClear(int bx, int by, int bz) {
        for (int x = -5; x <= 5; x++) for (int y = 0; y <= 7; y++) for (int z = -2; z <= 2; z++) {
            if (!podiumWorld.getBlockAt(bx + x, by + y, bz + z).isEmpty()) return false;
        }
        return true;
    }

    private void setPodiumBlock(int x, int y, int z, BlockData d) {
        Block b = podiumWorld.getBlockAt(x, y, z);
        podiumOriginal.putIfAbsent(b.getLocation(), b.getBlockData().clone());
        b.setBlockData(d, false);
    }

    /**
     * End of championship: a temporary 1st/2nd/3rd podium floating above lobby spawn (quartz pillars topped with
     * gold/iron/copper, team-colour banners), top-3 sides' online players teleported onto it, firework show,
     * removed again (original blocks restored) after 30 seconds.
     */
    private void buildChampionPodium() {
        removePodium(false);
        if (world == null) return;
        CoreView view = new CoreView();
        List<String> top = new ArrayList<>();
        String champ = podiumChampionSide;
        podiumChampionSide = null;
        if (champ != null) top.add(champ);                 // Dodgebolt winner stands on top
        for (Map.Entry<String, Integer> e : overallSideTotals().entrySet()) {
            if (top.size() >= 3) break;
            if (!e.getKey().equals(champ)) top.add(e.getKey());
        }
        if (top.isEmpty()) return;
        // MCC championship with the hub built: the ceremony happens on the reveal podium inside the dome
        boolean inDome = mccContext && hubReady();
        podiumWorld = inDome ? hub.world() : world;
        int bx, by, bz;
        if (inDome) {
            Location rp = hub.revealPodium();
            bx = rp.getBlockX(); bz = rp.getBlockZ(); by = rp.getBlockY() - 1;
            List<Location> seats = hub.standSpots();
            Set<UUID> onPodium = new HashSet<>();
            for (String k : top) for (Player p : McmFx.playersOfSide(view, k)) onPodium.add(p.getUniqueId());
            int si = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (onPodium.contains(p.getUniqueId()) || seats.isEmpty()) continue;
                p.setFallDistance(0f);
                p.teleport(seats.get((si++ * 7) % seats.size()));
            }
        } else {
            Location sp = world.getSpawnLocation();
            bx = sp.getBlockX(); bz = sp.getBlockZ();
            int maxY = world.getMaxHeight() - 10;
            by = Math.min(maxY, sp.getBlockY() + 14);
            for (int off : new int[]{14, 22, 30, 40}) {
                int y = sp.getBlockY() + off;
                if (y > maxY) break;
                if (podiumAreaClear(bx, y, bz)) { by = y; break; }
            }
        }
        BlockData quartz = Material.QUARTZ_BLOCK.createBlockData();
        BlockData smooth = Material.SMOOTH_QUARTZ.createBlockData();
        BlockData lantern = Material.SEA_LANTERN.createBlockData();
        for (int x = -5; x <= 5; x++) for (int z = -2; z <= 2; z++) {
            boolean corner = Math.abs(x) == 5 && Math.abs(z) == 2;
            setPodiumBlock(bx + x, by, bz + z, corner ? lantern : smooth);
        }
        int[] ox = {0, -3, 3};
        int[] hh = {4, 3, 2};
        Material[] metal = {Material.GOLD_BLOCK, Material.IRON_BLOCK, Material.COPPER_BLOCK};
        int[][] spots = {{0, 0}, {-1, 0}, {1, 0}, {0, -1}, {-1, -1}, {1, -1}, {0, 1}, {-1, 1}, {1, 1}};
        List<Player> champs = new ArrayList<>();
        podiumCenter = new Location(podiumWorld, bx + 0.5, by + 1, bz + 0.5);
        for (int i = 0; i < top.size(); i++) {
            ChatColor c = McmFx.sideColor(view, top.get(i));
            BlockData top3 = metal[i].createBlockData();
            for (int px = -1; px <= 1; px++) for (int pz = -1; pz <= 1; pz++) for (int y = 1; y <= hh[i]; y++) {
                setPodiumBlock(bx + ox[i] + px, by + y, bz + pz, y == hh[i] ? top3 : quartz);
            }
            BlockData concrete = McmFx.concreteOf(c).createBlockData();
            for (int px = -1; px <= 1; px++) setPodiumBlock(bx + ox[i] + px, by, bz + 2, concrete);
            BlockData banner = McmFx.wallBannerOf(c).createBlockData();
            if (banner instanceof org.bukkit.block.data.Directional d) d.setFacing(org.bukkit.block.BlockFace.SOUTH);
            setPodiumBlock(bx + ox[i], by + 1, bz + 2, banner);

            int k = 0;
            for (Player p : McmFx.playersOfSide(view, top.get(i))) {
                if (!p.isOnline()) continue;
                int[] s = spots[k++ % spots.length];
                Location l = new Location(podiumWorld, bx + ox[i] + s[0] + 0.5, by + hh[i] + 1, bz + s[1] + 0.5, 0f, 0f);
                p.setFallDistance(0f);
                p.teleport(l);
                podiumPlayers.add(p.getUniqueId());
                if (i == 0) champs.add(p);
            }
        }
        Color winColor = McmFx.colorOf(McmFx.sideColor(view, top.get(0)));
        podiumShowTask = McmFx.fireworkShow(this, podiumCenter.clone().add(0, 2, 0), winColor, 12);
        if (!champs.isEmpty()) McmFx.celebrate(this, champs, winColor);
        broadcast(ChatColor.GOLD + (podiumWorld == world ? "Champions' podium above spawn! " : "Champions' podium in the Decision Dome! ") + ChatColor.GRAY + "(disappears in " + secondsLabel(Waits.ticks(20L * 30)) + ")");
        podiumRemoveTask = new BukkitRunnable() {
            @Override public void run() {
                podiumRemoveTask = null;
                removePodium(true);
            }
        }.runTaskLater(this, Waits.ticks(20L * 30)); // fast-test: <= 1 s
    }

    /** Restores the blocks under the podium; optionally brings players still standing on it back to spawn. */
    private void removePodium(boolean teleportBack) {
        if (podiumRemoveTask != null) { podiumRemoveTask.cancel(); podiumRemoveTask = null; }
        if (podiumShowTask != null) { podiumShowTask.cancel(); podiumShowTask = null; }
        if (teleportBack && podiumCenter != null && world != null && currentGame == GameType.LOBBY) {
            Location lobby = lobbyLocation();
            for (UUID u : podiumPlayers) {
                Player p = Bukkit.getPlayer(u);
                if (p != null && p.isOnline() && p.getWorld() == podiumCenter.getWorld() && p.getLocation().distanceSquared(podiumCenter) < 15 * 15) {
                    p.setFallDistance(0f);
                    p.teleport(lobby);
                }
            }
        }
        List<Map.Entry<Location, BlockData>> entries = new ArrayList<>(podiumOriginal.entrySet());
        Collections.reverse(entries);
        for (Map.Entry<Location, BlockData> e : entries) {
            try { e.getKey().getBlock().setBlockData(e.getValue(), false); } catch (Throwable ignored) {}
        }
        podiumOriginal.clear();
        podiumPlayers.clear();
        podiumCenter = null;
    }

    /** /mcm preview x1 y1 z1 x2 y2 z2 [name] - dump a region of the admin's world for the preview renderer. */
    private void previewCommand(Player p, String[] args) {
        String usage = ChatColor.YELLOW + "Usage: /mcm preview <x1> <y1> <z1> <x2> <y2> <z2> [name]" + ChatColor.GRAY + " (~ = your position)";
        if (args.length < 7) { p.sendMessage(usage); return; }
        Location base = p.getLocation();
        int[] c = new int[6];
        try {
            for (int i = 0; i < 6; i++) {
                double b = i % 3 == 0 ? base.getX() : i % 3 == 1 ? base.getY() : base.getZ();
                c[i] = parseCoord(args[i + 1], b);
            }
        } catch (NumberFormatException ex) {
            p.sendMessage(usage);
            return;
        }
        String name = args.length >= 8 ? args[7] : "preview_" + (System.currentTimeMillis() / 1000L);
        name = name.replaceAll("[^A-Za-z0-9_\\-]", "_");
        if (name.isEmpty()) name = "preview";
        long vol = (long) (Math.abs(c[3] - c[0]) + 1) * (Math.abs(c[4] - c[1]) + 1) * (Math.abs(c[5] - c[2]) + 1);
        if (vol > 64_000_000L) {
            p.sendMessage(ChatColor.RED + "Region too big (" + vol + " blocks, max 64,000,000).");
            return;
        }
        World w = p.getWorld();
        java.io.File out = new java.io.File(new java.io.File(getDataFolder(), "previews"), name + ".mcmdump.gz");
        UUID id = p.getUniqueId();
        p.sendMessage(ChatColor.AQUA + "Dumping " + vol + " blocks of " + w.getName() + " to " + ChatColor.WHITE + "previews/" + out.getName() + ChatColor.AQUA + "...");
        final String fname = out.getName();
        McmBuild.dumpRegion(w, new Location(w, c[0], c[1], c[2]), new Location(w, c[3], c[4], c[5]), out, res -> {
            Player pp = Bukkit.getPlayer(id);
            String msg = res.success()
                    ? ChatColor.GREEN + "Preview dump done: " + ChatColor.WHITE + fname + ChatColor.GRAY + " (" + res.blocks() + " blocks, "
                      + res.sizeX() + "x" + res.sizeY() + "x" + res.sizeZ() + ")"
                    : ChatColor.RED + "Preview dump failed: " + res.error();
            if (pp != null) pp.sendMessage(msg);
            getLogger().info(ChatColor.stripColor(msg));
        });
    }

    private int parseCoord(String s, double base) {
        if (s.startsWith("~")) {
            String r = s.substring(1);
            return (int) Math.floor(base + (r.isEmpty() ? 0 : Double.parseDouble(r)));
        }
        return (int) Math.floor(Double.parseDouble(s));
    }

    /** McmFx fireworks never hurt anyone, in any world, also during external games (not a game rule handler). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFxFireworkDamage(EntityDamageByEntityEvent e) {
        if (McmFx.isFxFirework(e.getDamager())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFxFireworkDamageFinal(EntityDamageByEntityEvent e) {
        if (McmFx.isFxFirework(e.getDamager())) e.setCancelled(true);
    }

    /** Read-only game context view used by the core's own McmFx presentation (writes are ignored). */
    private final class CoreView implements McmGameContext {
        @Override public Collection<Player> participants() {
            List<Player> out = new ArrayList<>();
            for (Player p : Bukkit.getOnlinePlayers()) if (!isStaff(p)) out.add(p);
            return out;
        }
        @Override public String teamOf(UUID player) { return scoring.getTeam(player); }
        @Override public ChatColor teamColor(String team) { return scoring.teamColor(team); }
        @Override public Map<String, List<Player>> sides() {
            Map<String, List<Player>> m = new LinkedHashMap<>();
            for (Player p : participants()) {
                if (championshipMode && scoring.getTeam(p.getUniqueId()) == null) continue;
                m.computeIfAbsent(scoring.getSideKey(p.getUniqueId()), k -> new ArrayList<>()).add(p);
            }
            return m;
        }
        @Override public double teamCoinFactor(UUID player) { return scoring.teamCoinFactor(player); }
        @Override public int registeredTeamSize(String sideKey) { return scoring.registeredSizeOfSide(sideKey); }
        @Override public int fullTeamSize() { return scoring.getMaxTeamSize(); }
        @Override public String sideLabel(String sideKey) {
            if (sideKey == null) return "?";
            if (sideKey.startsWith("TEAM:")) {
                String t = sideKey.substring(5);
                return scoring.teamColor(t) + t + " Team";
            }
            if (sideKey.startsWith("SOLO:")) {
                try { return ChatColor.WHITE + scoring.playerName(UUID.fromString(sideKey.substring(5))); }
                catch (IllegalArgumentException ex) { return sideKey; }
            }
            return sideKey;
        }
        @Override public List<String> topSides(int n) {
            List<String> keys = new ArrayList<>(sides().keySet());
            keys.sort((a, b) -> Integer.compare(sidePoints(b), sidePoints(a)));
            return n > 0 && keys.size() > n ? new ArrayList<>(keys.subList(0, n)) : keys;
        }
        @Override public int sidePoints(String sideKey) {
            if (sideKey == null) return 0;
            if (sideKey.startsWith("TEAM:")) return scoring.getTeamPoints(sideKey.substring(5));
            if (sideKey.startsWith("SOLO:")) {
                try { return scoring.getPlayerPoints(UUID.fromString(sideKey.substring(5))); } catch (IllegalArgumentException ex) { return 0; }
            }
            return 0;
        }
        @Override public void addPoints(UUID player, int points, String reason) {}
        @Override public void addSidePoints(String sideKey, int points, String reason) {}
        @Override public void broadcast(String message) { MinecraftMondayPlugin.this.broadcast(message); }
        @Override public Location lobby() { return lobbyLocation(); }
        @Override public void setTimer(int secondsLeft, int totalSeconds) {}
        @Override public void setSidebarLines(List<String> lines) {}
        @Override public void finish(String reason) {}
    }

    private void broadcast(String msg) {
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "MCM" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET + msg);
    }

    // ==================== MCC HUB (mcc_lobby) ====================

    private boolean hubReady() { return hub != null && hub.isReady(); }

    /** Lobby for the current context: the MCC hub for MCC events (when built), else the MM lobby. */
    private Location lobbyLocation() {
        if (mccContext && hubReady()) return hub.spawn();
        return world.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
    }

    private List<String> buildMccPlaylist() {
        List<String> configured = getConfig().getStringList("mcc-championship-playlist");
        List<String> out = new ArrayList<>();
        if (configured.isEmpty()) {
            for (McmGame g : mccGames()) out.add(g.id().toLowerCase(Locale.ROOT)); // MM-group externals excluded
        } else {
            for (String id : configured) {
                if (hiddenGame(id)) getLogger().info("mcc-championship-playlist: '" + id + "' is in hidden-games - skipped");
                else if (McmApi.game(id) != null && isMmExternal(id)) getLogger().warning("mcc-championship-playlist: '" + id + "' is a Minecraft Monday game (minecraft-monday-external-games) - skipped");
                else if (McmApi.game(id) != null) out.add(id.toLowerCase(Locale.ROOT));
                else getLogger().warning("mcc-championship-playlist: '" + id + "' is not an installed MCC game - skipped");
            }
        }
        return finaleLast(out);
    }

    /** Joins a team (same rules as the team GUI). Returns false if refused. */
    private boolean joinTeam(Player p, String tName) {
        if (tName.equals(scoring.getTeam(p.getUniqueId()))) {
            p.sendMessage(ChatColor.YELLOW + "You're already on " + scoring.teamColor(tName) + tName + " Team!");
            return true;
        }
        if (teamsLocked()) {
            tellTeamsLocked(p);
            return false;
        }
        if (isStaff(p)) {
            p.sendMessage(ChatColor.RED + "You are staff/spectator and cannot join a team. " + ChatColor.GRAY + "(/mcm spectate off)");
            return false;
        }
        if (!scoring.isTeamActive(tName)) {
            p.sendMessage(ChatColor.RED + tName + " Team is not in this event.");
            return false;
        }
        if (!scoring.trySetTeam(p.getUniqueId(), tName)) {
            p.sendMessage(ChatColor.RED + tName + " Team is full (" + scoring.registeredCount(tName) + "/" + scoring.getMaxTeamSize() + ")!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return false;
        }
        broadcast(ChatColor.WHITE + p.getName() + ChatColor.GRAY + " joined " + scoring.teamColor(tName) + "" + ChatColor.BOLD + McmHud.teamDisplay(tName) + ChatColor.GRAY + "!");
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f);
        scoring.applyTabNames();
        refreshLobbyItems();
        if (hub != null) hub.refreshDisplays();
        return true;
    }

    /** Hub team pads (index in TeamScoring order); only between events. */
    private void joinTeamFromPad(Player p, Integer idx) {
        if (currentGame != GameType.LOBBY || idx == null || idx < 0 || idx >= scoring.getActiveTeams()) return;
        if (championshipMode) {
            if (System.currentTimeMillis() - padLockNotice.getOrDefault(p.getUniqueId(), 0L) > 5000) {
                padLockNotice.put(p.getUniqueId(), System.currentTimeMillis());
                tellTeamsLocked(p);
            }
            return;
        }
        joinTeam(p, TeamScoring.TEAM_NAMES[idx]);
    }

    private final Map<UUID, Long> padLockNotice = new HashMap<>();

    /** Multi-line TextDisplay content above each East team booth in the MCC hub. */
    private String hubTeamPadLabel(int idx) {
        if (idx < 0 || idx >= scoring.getActiveTeams()) return "";
        String t = TeamScoring.TEAM_NAMES[idx];
        ChatColor tc = scoring.teamColor(t);
        List<UUID> members = scoring.registeredMembers(t);
        List<String> names = new ArrayList<>();
        for (UUID u : members) names.add((Bukkit.getPlayer(u) != null ? ChatColor.GRAY : ChatColor.DARK_GRAY) + scoring.nameOf(u));
        int max = scoring.getMaxTeamSize();
        String roster = members.isEmpty()
                ? ChatColor.GREEN + "0/" + max + ChatColor.GRAY + " • Open"
                : ChatColor.WHITE + "" + members.size() + "/" + max + " " + String.join(ChatColor.GRAY + ", ", names);
        String action = championshipMode ? ChatColor.GRAY + "✖ LOCKED"
                : members.size() >= max ? ChatColor.RED + "✖ FULL" : ChatColor.YELLOW + "▶ STEP TO JOIN";
        return tc + "" + ChatColor.BOLD + McmHud.teamDisplay(t) + "\n" + roster + "\n" + action;
    }

    /** Lines for the hub's live leaderboard wall. */
    private List<String> hubBoardLines() {
        List<String> out = new ArrayList<>();
        out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ MINECRAFT CHAMPIONSHIPS ✦");
        int online = 0;
        for (Player op : Bukkit.getOnlinePlayers()) if (!isStaff(op)) online++;
        String mode = championshipMode
                ? ChatColor.YELLOW + "Event " + Math.min(playlistIndex + 1, playlist.size()) + "/" + playlist.size()
                : ChatColor.GREEN + "Lobby";
        out.add(ChatColor.GRAY + "Online: " + ChatColor.WHITE + online + ChatColor.DARK_GRAY + "  ┃  "
                + ChatColor.GRAY + "Team Size: " + ChatColor.YELLOW + scoring.getMaxTeamSize() + ChatColor.DARK_GRAY + "  ┃  " + mode);
        out.add("");
        out.add(ChatColor.GOLD + "" + ChatColor.BOLD + "TEAM STANDINGS");
        List<String> activeTeams = new ArrayList<>();
        for (String t : TeamScoring.TEAM_NAMES) {
            if (scoring.registeredCount(t) > 0 || scoring.getTeamPoints(t) > 0) activeTeams.add(t);
        }
        activeTeams.sort((a, b) -> {
            int c = Integer.compare(scoring.getTeamPoints(b), scoring.getTeamPoints(a));
            return c != 0 ? c : Integer.compare(scoring.registeredCount(b), scoring.registeredCount(a));
        });
        int max = scoring.getMaxTeamSize();
        int shownTeams = 0;
        for (String t : activeTeams) {
            if (shownTeams >= 10) break;
            shownTeams++;
            ChatColor rankCol = shownTeams == 1 ? ChatColor.GOLD : shownTeams == 2 ? ChatColor.WHITE : shownTeams == 3 ? ChatColor.YELLOW : ChatColor.GRAY;
            int cnt = scoring.registeredCount(t);
            int pts = scoring.getTeamPoints(t);
            out.add(rankCol + "" + ChatColor.BOLD + "#" + shownTeams + " "
                    + scoring.teamColor(t) + ChatColor.BOLD + McmHud.teamDisplay(t)
                    + ChatColor.GRAY + " (" + cnt + "/" + max + ")  "
                    + ChatColor.YELLOW + pts + ChatColor.GRAY + " coins");
        }
        if (shownTeams == 0) {
            out.add(ChatColor.GRAY + "No teams joined yet — step on an East booth →");
        }
        out.add("");
        out.add(ChatColor.AQUA + "" + ChatColor.BOLD + "TOP PLAYERS");
        List<UUID> playerList = new ArrayList<>();
        for (Map.Entry<UUID, Integer> e : scoring.getIndividualStandings()) playerList.add(e.getKey());
        for (Player pl : Bukkit.getOnlinePlayers()) if (!playerList.contains(pl.getUniqueId()) && !scoring.isExcluded(pl.getUniqueId())) playerList.add(pl.getUniqueId());
        playerList.sort((a, b) -> {
            int c = Integer.compare(scoring.getPlayerPoints(b), scoring.getPlayerPoints(a));
            return c != 0 ? c : scoring.playerName(a).compareToIgnoreCase(scoring.playerName(b));
        });
        int j = 0;
        for (UUID id : playerList) {
            if (j >= 5) break;
            j++;
            String t = scoring.getTeam(id);
            ChatColor tc = t == null ? ChatColor.WHITE : scoring.teamColor(t);
            out.add(ChatColor.GRAY + "" + j + ". " + tc + scoring.playerName(id)
                    + ChatColor.WHITE + "  " + ChatColor.YELLOW + scoring.getPlayerPoints(id) + ChatColor.GRAY + " coins");
        }
        if (j == 0) out.add(ChatColor.DARK_GRAY + "Waiting for players...");
        return out;
    }

    private void travelToHub(Player p) {
        if (!hubReady()) { p.sendMessage(ChatColor.RED + "The MCC hub is " + (hub == null ? "disabled" : hub.status()) + "."); return; }
        if (currentGame != GameType.LOBBY) { p.sendMessage(ChatColor.RED + "You can only travel between lobbies between events."); return; }
        p.closeInventory();
        p.setFallDistance(0f);
        p.teleport(hub.spawn());
        McmFx.title(p, ChatColor.GOLD + "" + ChatColor.BOLD + "MINECRAFT CHAMPIONSHIPS", ChatColor.YELLOW + "Welcome to the MCC Hub!", 5, 45, 10);
        p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1.3f);
    }

    private void travelToMm(Player p) {
        if (currentGame != GameType.LOBBY) { p.sendMessage(ChatColor.RED + "You can only travel between lobbies between events."); return; }
        p.closeInventory();
        p.setFallDistance(0f);
        p.teleport(world.getSpawnLocation().clone().add(0.5, 1.0, 0.5));
        p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
    }

    /** Compass: in the MCC hub shows the MCC games; in the MM lobby the MM controller (ops) or a lobby selector. */
    private void openSelector(Player p) {
        if (hub != null && hub.isHubWorld(p.getWorld())) { openMccGui(p); return; }
        if (p.isOp()) { openAdminGui(p); return; }
        Object[][] mm = {{Material.IRON_SWORD, "Survival Games"}, {Material.FEATHER, "Parkour Spiral"}, {Material.WATER_BUCKET, "Multi-Stage Dropper"},
                {Material.TNT, "TNT Run"}, {Material.GOLDEN_SHOVEL, "Spleef / Splegg"}, {Material.DIAMOND_CHESTPLATE, "Battle Arena"},
                {Material.GOLDEN_APPLE, "Speed Survival"}, {Material.BOW, "One Shot"}};
        List<MenuEntry> games = new ArrayList<>();
        for (Object[] g : mm) games.add(new MenuEntry(makeItem((Material) g[0], ChatColor.YELLOW + "" + ChatColor.BOLD + g[1],
                ChatColor.GRAY + "Minecraft Monday game"), null));
        for (McmGame g : mmExternalGames()) games.add(new MenuEntry(makeItem(g.icon(), ChatColor.YELLOW + "" + ChatColor.BOLD + g.displayName(),
                ChatColor.GRAY + "Minecraft Monday game"), null));
        MenuEntry header = new MenuEntry(makeItem(Material.CLOCK, ChatColor.AQUA + "" + ChatColor.BOLD + "Minecraft Monday",
                ChatColor.GRAY + "An op runs these games from here"), null);
        MenuEntry[] controls = new MenuEntry[9];
        controls[4] = hubTravelEntry(p);
        openMenu(p, SELECTOR_GUI_TITLE, header, games, controls);
    }

    /** Clicking a game in a menu: games with more than one map show a map picker first; the rest start at once. */
    private void pickMapThenStart(Player p, String gameId) {
        List<String> maps = mapsOf(gameId);
        if (maps.size() < 2) {
            p.closeInventory();
            championshipMode = false;
            startById(gameId);
            return;
        }
        if (maps.size() > 44) maps = new ArrayList<>(maps.subList(0, 44));
        Inventory inv = Bukkit.createInventory(null, 54, MAP_GUI_TITLE);
        inv.setItem(0, makeItem(Material.ENDER_PEARL, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Random map",
                ChatColor.GRAY + McmApi.displayName(gameId), ChatColor.YELLOW + "Click to play the normal rotation"));
        Material[] icons = {Material.MAP, Material.FILLED_MAP, Material.PAPER};
        for (int i = 0; i < maps.size(); i++) {
            String id = maps.get(i);
            String nice = id.replace('_', ' ');
            inv.setItem(i + 1, makeItem(icons[i % icons.length], ChatColor.AQUA + "" + ChatColor.BOLD + nice,
                    ChatColor.GRAY + McmApi.displayName(gameId), ChatColor.DARK_GRAY + "/mcm map " + id, ChatColor.YELLOW + "Click to play this map"));
        }
        inv.setItem(53, makeItem(Material.BARRIER, ChatColor.RED + "Cancel"));
        mapPickGame.put(p.getUniqueId(), gameId);
        mapPickIds.put(p.getUniqueId(), maps);
        p.openInventory(inv);
    }

    private void onMapPickClick(Player p, int slot) {
        String gameId = mapPickGame.get(p.getUniqueId());
        List<String> maps = mapPickIds.get(p.getUniqueId());
        if (gameId == null || maps == null) { p.closeInventory(); return; }
        if (slot == 53) { p.closeInventory(); return; }
        if (slot < 0 || slot > maps.size()) return;
        p.closeInventory();
        mapPickGame.remove(p.getUniqueId());
        mapPickIds.remove(p.getUniqueId());
        if (currentGame != GameType.LOBBY) { p.sendMessage(ChatColor.RED + "A game is already running. Stop it first."); return; }
        if (slot > 0) forceMap(p, gameId, maps.get(slot - 1));
        championshipMode = false;
        startById(gameId);
    }

    private void openMccGui(Player p) {
        boolean op = p.isOp();
        List<MenuEntry> games = new ArrayList<>();
        for (McmGame g : mccGames()) {
            int maps = mapsOf(g.id()).size();
            List<String> lore = new ArrayList<>();
            if (!g.description().isEmpty()) lore.add(ChatColor.GRAY + g.description());
            if (maps > 1) lore.add(ChatColor.DARK_AQUA + "" + maps + " maps");
            lore.add("");
            lore.add(op ? ChatColor.GREEN + "▶ Click to play" : ChatColor.DARK_GRAY + "Minecraft Championships game");
            games.add(new MenuEntry(makeItem(g.icon(), ChatColor.AQUA + "" + ChatColor.BOLD + g.displayName(), lore.toArray(new String[0])),
                    op ? () -> pickMapThenStart(p, g.id()) : null));
        }
        MenuEntry header = op
                ? new MenuEntry(makeItem(Material.NETHER_STAR, ChatColor.GOLD + "" + ChatColor.BOLD + "★ START MCC CHAMPIONSHIP ★",
                        ChatColor.GRAY + "" + buildMccPlaylist().size() + " MCC games, votes in the Decision Dome", ChatColor.DARK_GRAY + "/mcm start mcc",
                        ChatColor.YELLOW + "Click to start"), () -> { p.closeInventory(); startChampionship(true); })
                : new MenuEntry(makeItem(Material.NETHER_STAR, ChatColor.GOLD + "" + ChatColor.BOLD + getConfig().getString("hud.event-name", "MINECRAFT CHAMPIONSHIPS"),
                        ChatColor.GRAY + "Your coins: " + ChatColor.GOLD + scoring.getPlayerPoints(p.getUniqueId())), null);
        MenuEntry[] controls = new MenuEntry[9];
        controls[0] = new MenuEntry(makeItem(Material.GRASS_BLOCK, ChatColor.GREEN + "" + ChatColor.BOLD + "Minecraft Monday Lobby",
                ChatColor.GRAY + "Switch to the Minecraft Monday games", ChatColor.YELLOW + "Click to teleport"), () -> travelToMm(p));
        if (op) {
            controls[2] = new MenuEntry(makeItem(Material.REPEATER, ChatColor.AQUA + "" + ChatColor.BOLD + "Team Size: " + ChatColor.YELLOW + scoring.getTeamSizeLabel(),
                    ChatColor.GRAY + "Click to cycle: 1 → 5"), () -> { setTeamSize((scoring.getMaxTeamSize() % 5) + 1); openMccGui(p); });
            controls[3] = new MenuEntry(makeItem(Material.RED_WOOL, ChatColor.GREEN + "" + ChatColor.BOLD + "Auto-Balance Teams",
                    ChatColor.GRAY + "Teams of " + scoring.getMaxTeamSize()), () -> { p.closeInventory(); randomizeTeams(); });
            controls[5] = new MenuEntry(makeItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "STOP / RETURN TO LOBBY", ChatColor.GRAY + "/mcm stop"),
                    () -> { p.closeInventory(); championshipMode = false; returnToLobby(ChatColor.YELLOW + "Returned to lobby."); });
            controls[6] = new MenuEntry(makeItem(Material.ARROW, ChatColor.GOLD + "" + ChatColor.BOLD + "Skip",
                    ChatColor.YELLOW + "Left-click: skip whole event " + ChatColor.DARK_GRAY + "/mcm next",
                    ChatColor.YELLOW + "Right-click: skip one round " + ChatColor.DARK_GRAY + "/mcm skip"),
                    () -> { p.closeInventory(); if (lastMenuClickRight.contains(p.getUniqueId())) cmdSkipRound(p); else skipCurrentEvent(p); });
            controls[8] = new MenuEntry(makeItem(Material.CLOCK, ChatColor.YELLOW + "Reset All Scores", ChatColor.GRAY + "Everyone back to 0", ChatColor.RED + "Shift-click to confirm"),
                    () -> { if (!lastMenuClickShift.contains(p.getUniqueId())) return;
                        p.closeInventory(); scoring.resetScores(); if (hub != null) hub.refreshDisplays(); broadcast(ChatColor.RED + "Tournament scores reset!"); });
        }
        openMenu(p, MCC_GUI_TITLE, header, games, controls);
    }

    /** /mcm lobby [tp|mm|rebuild|status] */
    private void lobbyCommand(CommandSender sender, String[] args) {
        String sub = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "status";
        if (hub == null) { sender.sendMessage(ChatColor.RED + "The MCC hub is disabled (mcc-hub.enabled: false)."); return; }
        switch (sub) {
            case "rebuild" -> {
                if (currentGame != GameType.LOBBY) { sender.sendMessage(ChatColor.RED + "Stop the running event first."); return; }
                if (hub.isBuilding()) { sender.sendMessage(ChatColor.YELLOW + "The hub is already building: " + hub.status()); return; }
                for (Player pl : hub.world() == null ? List.<Player>of() : hub.world().getPlayers()) pl.teleport(world.getSpawnLocation().clone().add(0.5, 1.0, 0.5));
                hub.rebuild();
                sender.sendMessage(ChatColor.AQUA + "Rebuilding the MCC hub over the next minutes (progress in the console).");
            }
            case "tp" -> { if (sender instanceof Player pl) travelToHub(pl); }
            case "mm" -> { if (sender instanceof Player pl) travelToMm(pl); }
            default -> sender.sendMessage(ChatColor.GOLD + "MCC hub (" + HubManager.WORLD + "): " + ChatColor.WHITE + hub.status()
                    + ChatColor.GRAY + " | /mcm lobby tp|mm|rebuild  |  /mcm start mcc = MCC championship");
        }
    }

    // ==================== AUTOMATED LIVE-WORLD SELF-TEST ====================

    private void runSelfTest(CommandSender sender) {
        int passed = 0;
        int failed = 0;

        // 1. All 7 Required Plugins Enabled
        String[] reqPlugins = {"MinecraftMondayCore", "OldCombatMechanics", "ViaVersion", "ViaBackwards", "ViaRewind", "Multiverse-Core", "TAB"};
        List<String> missingPlugins = new ArrayList<>();
        for (String pn : reqPlugins) {
            if (!Bukkit.getPluginManager().isPluginEnabled(pn)) missingPlugins.add(pn);
        }
        if (reportCheck(sender, "Plugins (all 7)", missingPlugins.isEmpty(),
                missingPlugins.isEmpty() ? "all 7 enabled" : "missing=" + missingPlugins)) passed++; else failed++;

        // 2. Lobby 3x3 Clearance + WorldBorder
        Location lobby = world.getSpawnLocation();
        int lx = lobby.getBlockX(), ly = lobby.getBlockY(), lz = lobby.getBlockZ();
        boolean lobbyClear3x3 = true;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!hasSafeStanding(world, lx + dx, ly, lz + dz, 2)
                        || !world.getBlockAt(lx + dx, ly + 2, lz + dz).isPassable()) {
                    lobbyClear3x3 = false;
                }
            }
        }
        double lobbyBorder = world.getWorldBorder().getSize();
        boolean lobbyOk = lobbyClear3x3 && lobbyBorder > 1000.0;
        if (reportCheck(sender, "Lobby (world)", lobbyOk,
                "spawn=" + lx + "," + ly + "," + lz + " clear3x3=" + lobbyClear3x3 + " border=" + (long) lobbyBorder)) passed++; else failed++;

        // 2b. MCC hub (only when enabled and built)
        if (hub != null && hubReady()) {
            World hw = hub.world();
            Location hs = hub.spawn();
            boolean hubClear = true;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                if (!hasSafeStanding(hw, hs.getBlockX() + dx, hs.getBlockY(), hs.getBlockZ() + dz, 2)
                        || !hw.getBlockAt(hs.getBlockX() + dx, hs.getBlockY() + 2, hs.getBlockZ() + dz).isPassable()) hubClear = false;
            }
            boolean hubOk = hubClear && hw.getWorldBorder().getSize() > 1000.0 && McmLobby.isAvailable();
            if (reportCheck(sender, "MCC hub (" + HubManager.WORLD + ")", hubOk, "spawn clear3x3=" + hubClear + " border=" + (long) hw.getWorldBorder().getSize()
                    + " venue=" + McmLobby.isAvailable())) passed++; else failed++;
        } else if (hub != null) {
            sender.sendMessage(ChatColor.GRAY + "  (MCC hub: " + hub.status() + " - not checked)");
        }

        // 3. All Worlds Loaded, Gamerules & Entity Hygiene
        World[] allWorlds = {world, sg4World, sgWorld, parkourWorld, dropperWorld, tntrunWorld, spleefWorld, pvpWorld, oneshotWorld, survivalWorld};
        List<String> ruleIssues = new ArrayList<>();
        for (int i = 0; i < allWorlds.length; i++) {
            World w = allWorlds[i];
            if (w == null) { ruleIssues.add("null#" + i); continue; }
            if (!Boolean.FALSE.equals(w.getGameRuleValue(org.bukkit.GameRule.DO_MOB_SPAWNING))) ruleIssues.add(w.getName() + ":mobs");
            if (!Boolean.TRUE.equals(w.getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY))) ruleIssues.add(w.getName() + ":keepInv");
        }
        int initialStrayItems = oneshotWorld == null ? -1 : oneshotWorld.getEntitiesByClass(org.bukkit.entity.Item.class).size();
        if (initialStrayItems != 0) ruleIssues.add("oneshotItems=" + initialStrayItems);
        if (reportCheck(sender, "Worlds & Gamerules", ruleIssues.isEmpty(),
                ruleIssues.isEmpty() ? "10/10 worlds clean, strayItems=0" : String.join(",", ruleIssues))) passed++; else failed++;

        // 4. Survival Games (SG4 + Kalas): Chests & 8-Angle Spawn Ring
        for (World w : new World[]{sg4World, sgWorld}) {
            if (w == null) { reportCheck(sender, "SG World missing", false, "null"); failed++; continue; }
            Location c = w.getSpawnLocation();
            int cx = c.getBlockX(), cz = c.getBlockZ();
            int chests = countChestsNear(w, cx, cz);
            int ringR = 7;
            int ringOk = 0;
            for (int i = 0; i < 8; i++) {
                double ang = (2 * Math.PI * i) / 8.0;
                int sx = cx + (int) Math.round(Math.cos(ang) * ringR);
                int sz = cz + (int) Math.round(Math.sin(ang) * ringR);
                int sy = findSafeGround(w, sx, sz, (w == sgWorld ? 140 : 85), 20, c.getBlockY());
                if (hasSafeStanding(w, sx, sy, sz, 1)) ringOk++;
            }
            boolean ok = chests >= 15 && ringOk == 8;
            if (reportCheck(sender, "SG (" + w.getName() + ")", ok, "chests=" + chests + " ringSpawns=" + ringOk + "/8")) passed++; else failed++;
        }

        // 5. Parkour Spiral Start & Checkpoints
        if (parkourWorld != null) {
            boolean startOk = hasSafeStanding(parkourWorld, -1, 52, -51, 1);
            int plates = countPressurePlatesInChunks(parkourWorld, -2, -4, 2, 2, 50, 235);
            boolean ok = startOk && plates >= 1;
            if (reportCheck(sender, "Parkour (parkour_world)", ok, "start=(-1,52,-51) startOk=" + startOk + " plates=" + plates)) passed++; else failed++;
        } else { reportCheck(sender, "Parkour", false, "world null"); failed++; }

        // 6. All 6 Dropper Stages: Ledge, Open Jump-Off & Target Blocks
        if (dropperWorld != null) {
            for (DropperStage st : ALL_DROPPER_STAGES) {
                int sx = (int) Math.floor(st.x());
                int sy = (int) Math.floor(st.y());
                int sz = (int) Math.floor(st.z());
                boolean ledgeOk = dropperWorld.getBlockAt(sx, sy - 1, sz).getType().isSolid()
                        && dropperWorld.getBlockAt(sx, sy, sz).isPassable()
                        && dropperWorld.getBlockAt(sx, sy + 1, sz).isPassable();
                int jx = sx + (st.yaw() < 0 ? 2 : -2);
                boolean jumpOpen = dropperWorld.getBlockAt(jx, sy - 1, sz).isPassable()
                        && dropperWorld.getBlockAt(jx, sy, sz).isPassable();
                int targetHits = countMaterialInBox(dropperWorld, st.targetMat(), 6, 98, sz - 15, 33, 106, sz + 15);
                boolean ok = ledgeOk && jumpOpen && targetHits > 0;
                if (reportCheck(sender, "Dropper " + st.name(), ok,
                        "ledge=" + ledgeOk + " jumpOpen(x=" + jx + ")=" + jumpOpen + " target=" + st.targetMat() + " x" + targetHits)) passed++; else failed++;
            }
        } else { reportCheck(sender, "Dropper", false, "world null"); failed++; }
        if (dropperWorld != null && importedDroppers != null) {
            for (ImportedDropper d : importedDroppers.all()) {
                if (!d.isReady()) { reportCheck(sender, "Imported dropper " + d.id, false, "not pasted yet (slot " + d.slot() + ")"); failed++; continue; }
                double[] sp = d.spawn();
                int sx = (int) Math.floor(sp[0]), sy = (int) Math.floor(sp[1]), sz = (int) Math.floor(sp[2]);
                boolean air = dropperWorld.getBlockAt(sx, sy, sz).isPassable() && dropperWorld.getBlockAt(sx, sy + 1, sz).isPassable()
                        && dropperWorld.getBlockAt(sx, sy - 1, sz).isPassable();
                int water = 0;
                int cx = (int) Math.floor(d.centreX()), cz = (int) Math.floor(d.centreZ());
                for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++)
                    if (dropperWorld.getBlockAt(cx + dx, d.oy() + d.poolTopY, cz + dz).getType() == Material.WATER) water++;
                boolean ok = air && water > 0;
                if (reportCheck(sender, "Imported dropper " + d.id, ok, "start=(" + sx + "," + sy + "," + sz + ") air=" + air
                        + " poolWater(7x7 at centre)=" + water + " by " + d.credit)) passed++; else failed++;
            }
        }

        // 7. All 3 TNT Run Arenas: Andesite Spawn, Headroom & Lava Floor
        if (tntrunWorld != null) {
            int[][] arenas = {{100, 15, 100}, {100, 8, 235}, {100, 12, 375}};
            String[] names = {"Jungle", "Castle", "Pyramid"};
            for (int i = 0; i < 3; i++) {
                int ax = arenas[i][0], ay = arenas[i][1], az = arenas[i][2];
                boolean andesiteOk = tntrunWorld.getBlockAt(ax, ay - 1, az).getType() == Material.ANDESITE;
                boolean headOk = tntrunWorld.getBlockAt(ax, ay, az).isPassable() && tntrunWorld.getBlockAt(ax, ay + 1, az).isPassable();
                int lavaHits = countMaterialInBox(tntrunWorld, Material.LAVA, ax - 15, -5, az - 15, ax + 15, ay - 2, az + 15);
                boolean ok = andesiteOk && headOk && lavaHits > 0;
                if (reportCheck(sender, "TNT Run " + names[i], ok,
                        "spawn=(" + ax + "," + ay + "," + az + ") andesite=" + andesiteOk + " lava=" + lavaHits)) passed++; else failed++;
            }
        } else { reportCheck(sender, "TNT Run", false, "world null"); failed++; }

        // 8. Spleef Spawn & Arena Floor
        if (spleefWorld != null) {
            int sy = findSafeGround(spleefWorld, -13, 22, 105, 80, 89);
            boolean spawnOk = hasSafeStanding(spleefWorld, -13, sy, 22, 1);
            int floorBlocks = 0;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (spleefWorld.getBlockAt(-13 + dx, sy - 1, 22 + dz).getType().isSolid()) floorBlocks++;
                }
            }
            boolean ok = spawnOk && floorBlocks >= 5;
            if (reportCheck(sender, "Spleef (spleef_world)", ok, "spawn=(-13," + sy + ",22) floor3x3=" + floorBlocks + "/9")) passed++; else failed++;
        } else { reportCheck(sender, "Spleef", false, "world null"); failed++; }

        // 9. KitPvP Harb: All 7x2 Spread Spawns
        if (pvpWorld != null) {
            int redOk = 0, blueOk = 0;
            for (int i = 0; i < 7; i++) {
                int bx = (int) Math.floor(0.5 + (i - 3) * 1.8);
                if (hasSafeStanding(pvpWorld, bx, 33, 48, 2)) redOk++;
                if (hasSafeStanding(pvpWorld, bx, 33, -47, 2)) blueOk++;
            }
            boolean ok = redOk == 7 && blueOk == 7;
            if (reportCheck(sender, "KitPvP Harb (pvp_world)", ok, "red=" + redOk + "/7 blue=" + blueOk + "/7")) passed++; else failed++;
        } else { reportCheck(sender, "KitPvP", false, "world null"); failed++; }

        // 10. One Shot (OITC Pink Valley) & Entity Cleanup
        if (oneshotWorld != null) {
            int ry = findSafeGround(oneshotWorld, 51, 100, 65, 25, 42);
            boolean spawnOk = hasSafeStanding(oneshotWorld, 51, ry, 100, 1);
            int strayItems = oneshotWorld.getEntitiesByClass(org.bukkit.entity.Item.class).size();
            boolean ok = spawnOk && strayItems == 0;
            if (reportCheck(sender, "One Shot (oneshot_world)", ok, "spawn=(51," + ry + ",100) strayItems=" + strayItems)) passed++; else failed++;
        } else { reportCheck(sender, "One Shot", false, "world null"); failed++; }

        // 11. Speed Survival (15m) Land Center, Scatter & Helpers
        if (survivalWorld != null) {
            int[] sc = findLandCenter(survivalWorld, 0, 0);
            int centerY = findNaturalSurface(survivalWorld, sc[0], sc[1]);
            int scatterOk = 0;
            for (int i = 0; i < 8; i++) {
                double ang = (2 * Math.PI * i) / 8.0;
                Location pt = findSafeScatterPoint(survivalWorld, sc[0], sc[1], ang, 105);
                if (findNaturalSurface(survivalWorld, pt.getBlockX(), pt.getBlockZ()) >= 62
                        && hasSafeStanding(survivalWorld, pt.getBlockX(), pt.getBlockY(), pt.getBlockZ(), 1)) {
                    scatterOk++;
                }
            }
            boolean helperOk = parseGame("survival") == GameType.SURVIVAL
                    && parseGame("uhc") == GameType.SURVIVAL
                    && formatTime(900).equals("15:00")
                    && formatTime(65).equals("1:05")
                    && formatTime(45).equals("45s");
            boolean ok = centerY >= 63 && scatterOk == 8 && helperOk;
            if (reportCheck(sender, "Speed Survival (survival_world)", ok,
                    "center=(" + sc[0] + "," + centerY + "," + sc[1] + ") scatter=" + scatterOk + "/8 helpers=" + helperOk)) passed++; else failed++;
        } else { reportCheck(sender, "Speed Survival", false, "world null"); failed++; }

        // 12. Online Players Sanity
        List<String> badPlayers = new ArrayList<>();
        int onlineCount = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            onlineCount++;
            AttributeInstance scale = p.getAttribute(Attribute.GENERIC_SCALE);
            AttributeInstance reach = p.getAttribute(Attribute.PLAYER_ENTITY_INTERACTION_RANGE);
            Location ploc = p.getLocation();
            boolean scaleOk = scale != null && Math.abs(scale.getValue() - 1.0) < 0.01;
            boolean walkOk = p.getWalkSpeed() >= 0.19f && p.getWalkSpeed() <= 0.21f;
            boolean reachOk = reach != null && reach.getValue() <= 3.05;
            boolean spaceOk = ploc.getBlock().isPassable() && ploc.clone().add(0, 1, 0).getBlock().isPassable();
            if (!scaleOk || !walkOk || !reachOk || !spaceOk) {
                badPlayers.add(p.getName() + "[s=" + (scale == null ? "?" : scale.getValue())
                        + ",w=" + p.getWalkSpeed() + ",r=" + (reach == null ? "?" : reach.getValue()) + ",sp=" + spaceOk + "]");
            }
        }
        if (reportCheck(sender, "Online Players Sanity", badPlayers.isEmpty(),
                badPlayers.isEmpty() ? "online=" + onlineCount + " all sane" : String.join(",", badPlayers))) passed++; else failed++;

        // 12. TeamScoring In-Memory Unit Test
        TeamScoring ts = new TeamScoring();
        ts.setMaxTeamSize(0);
        boolean clampLow = ts.getMaxTeamSize() == 1;
        ts.setMaxTeamSize(99);
        boolean clampHigh = ts.getMaxTeamSize() == 5;
        ts.setMaxTeamSize(2);
        boolean setTwo = ts.getMaxTeamSize() == 2;
        UUID u1 = UUID.randomUUID(), u2 = UUID.randomUUID(), u3 = UUID.randomUUID();
        ts.ensurePlayer(u1); ts.ensurePlayer(u2); ts.ensurePlayer(u3);
        ts.setTeam(u1, "Red"); ts.setTeam(u2, "Red"); ts.setTeam(u3, "Blue");
        boolean mateOk = ts.areTeammates(u1, u2) && !ts.areTeammates(u1, u3);
        boolean sideOk = ts.isOneSideRemaining(new HashSet<>(Arrays.asList(u1, u2)))
                && ts.isOneSideRemaining(new HashSet<>(Collections.singletonList(u3)))
                && !ts.isOneSideRemaining(new HashSet<>(Arrays.asList(u1, u2, u3)));
        ts.awardSplitTeamBonus(new HashSet<>(Arrays.asList(u1, u2, u3)), 30, "test-split");
        boolean splitOk = ts.getPlayerPoints(u1) == 15 && ts.getPlayerPoints(u2) == 15
                && ts.getPlayerPoints(u3) == 30
                && ts.getTeamPoints("Red") == 30 && ts.getTeamPoints("Blue") == 30;
        ts.addIndividualPoints(u1, 10, "test-ind");
        boolean totalOk = ts.getTeamPoints("Red") == 40;
        boolean tsOk = clampLow && clampHigh && setTwo && mateOk && sideOk && splitOk && totalOk;
        if (reportCheck(sender, "TeamScoring Logic", tsOk,
                "clamp=" + (clampLow && clampHigh && setTwo) + " mates=" + mateOk + " side=" + sideOk + " split=" + splitOk + " totals=" + totalOk)) passed++; else failed++;

        int total = passed + failed;
        String summary = "SELFTEST SUMMARY: " + passed + "/" + total + " PASSED, " + failed + " FAILED";
        getLogger().info(summary);
        sender.sendMessage((failed == 0 ? ChatColor.GREEN : ChatColor.RED) + summary);
    }

    private int countPressurePlatesInChunks(World w, int minCx, int minCz, int maxCx, int maxCz, int minY, int maxY) {
        int count = 0;
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                org.bukkit.ChunkSnapshot snap = w.getChunkAt(cx, cz).getChunkSnapshot(false, false, false);
                for (int y = minY; y <= maxY; y++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            if (snap.getBlockType(x, y, z).name().endsWith("_PRESSURE_PLATE")) count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    private boolean reportCheck(CommandSender sender, String label, boolean ok, String detail) {
        String tag = ok ? "[SELFTEST PASS] " : "[SELFTEST FAIL] ";
        getLogger().info(tag + label + " (" + detail + ")");
        if (sender instanceof Player) {
            sender.sendMessage((ok ? ChatColor.GREEN : ChatColor.RED) + tag + ChatColor.WHITE + label + ChatColor.GRAY + " (" + detail + ")");
        }
        return ok;
    }

    private boolean hasSafeStanding(World w, int x, int y, int z, int searchDown) {
        boolean solid = false;
        for (int dy = 1; dy <= searchDown; dy++) {
            Material m = w.getBlockAt(x, y - dy, z).getType();
            if (m.isSolid() || m == Material.WATER) { solid = true; break; }
        }
        return solid && w.getBlockAt(x, y, z).isPassable() && w.getBlockAt(x, y + 1, z).isPassable();
    }

    private int countChestsNear(World w, int cx, int cz) {
        int cChunkX = cx >> 4, cChunkZ = cz >> 4;
        int count = 0;
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                for (BlockState bs : w.getChunkAt(cChunkX + dx, cChunkZ + dz).getTileEntities()) {
                    if (bs instanceof Chest) count++;
                }
            }
        }
        return count;
    }

    private int countMaterialInBox(World w, Material mat, int x1, int y1, int z1, int x2, int y2, int z2) {
        int count = 0;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                for (int y = y1; y <= y2; y++) {
                    if (w.getBlockAt(x, y, z).getType() == mat) count++;
                }
            }
        }
        return count;
    }
}
