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
        SURVIVAL("Speed Survival (15m)", Material.GOLDEN_APPLE);

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

    private record DropperStage(String name, String hint, Material targetMat, double x, double y, double z, float yaw) {}

    private static final List<DropperStage> ALL_DROPPER_STAGES = Arrays.asList(
            new DropperStage("Jungle Drop", "VINES", Material.VINE, 19.5, 201.0, 5038.5, -90f),
            new DropperStage("City Drop", "SLIME BLOCKS", Material.SLIME_BLOCK, 19.5, 201.0, 5098.5, -90f),
            new DropperStage("Space Drop", "WATER", Material.WATER, 19.5, 201.0, 5158.5, -90f),
            new DropperStage("Mineshaft Drop", "COBWEBS", Material.COBWEB, 19.5, 201.0, 5219.5, 90f),
            new DropperStage("Poolrooms Drop", "WATER", Material.WATER, 19.5, 201.0, 5279.5, -90f),
            new DropperStage("Village Drop", "SLIME BLOCKS", Material.SLIME_BLOCK, 19.5, 201.0, 5309.5, -90f)
    );

    private final TeamScoring scoring = new TeamScoring();
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
    private final List<GameType> playlist = Arrays.asList(
            GameType.SURVIVAL_GAMES, GameType.PARKOUR, GameType.DROPPER,
            GameType.TNT_RUN, GameType.SPLEEF, GameType.KIT_PVP, GameType.SURVIVAL
    );
    private int playlistIndex = 0;

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
    private final int[] stageFinishCounts = new int[5];

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

    private static final int[] TEAM_GUI_SLOTS = {2, 3, 4, 5, 6, 11, 12, 13, 14, 15};
    private static final String ADMIN_GUI_TITLE = ChatColor.GOLD + "" + ChatColor.BOLD + "MCM Tournament Controller";
    private static final String TEAM_GUI_TITLE = ChatColor.AQUA + "" + ChatColor.BOLD + "Select Your Team";
    private static final String KIT_GUI_TITLE = ChatColor.DARK_RED + "" + ChatColor.BOLD + "Choose Your PvP Kit";

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
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
                ChatColor.GOLD + "" + ChatColor.BOLD + "★ MINECRAFT MONDAY ★ " + ChatColor.GRAY + "┃ " + ChatColor.YELLOW + "Lobby",
                BarColor.YELLOW, BarStyle.SOLID
        );

        new BukkitRunnable() {
            @Override public void run() { updateAllScoreboards(); }
        }.runTaskTimer(this, 20L, 20L);

        getLogger().info("MinecraftMondayCore v3.1.0 enabled! 8 community maps + survival_world ready.");
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
    }

    // ==================== COMMANDS & GUIS ====================

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("mcm") && args.length > 0 && args[0].equalsIgnoreCase("selftest")) {
            if (sender instanceof Player pl && !pl.isOp()) {
                pl.sendMessage(ChatColor.RED + "Only tournament admins (OPs) can run selftest.");
                return true;
            }
            runSelfTest(sender);
            return true;
        }
        if (!(sender instanceof Player p)) {
            if (args.length > 0 && args[0].equalsIgnoreCase("skip")) {
                skipCurrentEvent(null);
            } else if (args.length > 0 && args[0].equalsIgnoreCase("stop")) {
                championshipMode = false;
                returnToLobby(ChatColor.YELLOW + "Tournament stopped by Console.");
            } else if (args.length >= 2 && args[0].equalsIgnoreCase("play")) {
                championshipMode = false;
                GameType gt = parseGame(args[1]);
                if (gt == null) sender.sendMessage("Unknown game. Options: sg, parkour, dropper, tntrun, spleef, kitpvp, survival, oneshot");
                else startEvent(gt);
            } else {
                sender.sendMessage("Use in-game as OP (/mcm or /team) or run 'mcm selftest|play <game>|skip|stop' from console.");
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
            case "start" -> startChampionship();
            case "skip", "next" -> skipCurrentEvent(p);
            case "stop" -> {
                championshipMode = false;
                returnToLobby(ChatColor.YELLOW + "Tournament stopped by " + p.getName() + ".");
            }
            case "reset" -> {
                scoring.resetScores();
                broadcast(ChatColor.RED + "All tournament scores have been reset!");
                updateAllScoreboards();
            }
            case "teamsize" -> {
                if (args.length >= 2) {
                    try {
                        setTeamSize(Integer.parseInt(args[1]));
                    } catch (NumberFormatException ignored) {}
                }
            }
            case "play" -> {
                if (args.length < 2) {
                    p.sendMessage(ChatColor.YELLOW + "Usage: /mcm play <sg|parkour|dropper|tntrun|spleef|kitpvp|survival|oneshot>");
                    return true;
                }
                championshipMode = false;
                GameType gt = parseGame(args[1]);
                if (gt == null) p.sendMessage(ChatColor.RED + "Unknown game. Options: sg, parkour, dropper, tntrun, spleef, kitpvp, survival, oneshot");
                else startEvent(gt);
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
        if (currentGame == GameType.LOBBY && pendingTransitionTask != null) {
            // Skipping the between-game / between-round countdown!
            pendingTransitionTask.cancel();
            pendingTransitionTask = null;
            if (championshipMode && playlistIndex < playlist.size()) {
                broadcast(ChatColor.GOLD + who + " skipped the countdown!");
                startEvent(playlist.get(playlistIndex));
            }
            return;
        }
        broadcast(ChatColor.GOLD + "⏭ " + who + " skipped " + currentGame.display + "!");
        roundNumber = maxRounds; // force skipping remaining sub-rounds of multi-round events
        endCurrentEvent("Skipped by Admin");
    }

    private void setTeamSize(int size) {
        scoring.setMaxTeamSize(size);
        broadcast(ChatColor.AQUA + "Team Size set to " + ChatColor.YELLOW + scoring.getTeamSizeLabel()
                + ChatColor.AQUA + "! Right-click your Wool or type /team to join.");
        scoring.applyTabNames();
        refreshLobbyItems();
    }

    private void randomizeTeams() {
        scoring.randomizeOnlinePlayers();
        broadcast(ChatColor.AQUA + "Auto-balanced players into teams of " + ChatColor.YELLOW + scoring.getMaxTeamSize() + ChatColor.AQUA + "!");
        scoring.applyTabNames();
        refreshLobbyItems();
    }

    private void openAdminGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, ADMIN_GUI_TITLE);
        inv.setItem(4, makeItem(Material.NETHER_STAR, ChatColor.GOLD + "" + ChatColor.BOLD + "★ START FULL CHAMPIONSHIP ★",
                ChatColor.GRAY + "Plays all 7 events back-to-back! (incl. 15m Survival)", ChatColor.YELLOW + "Click to start"));

        inv.setItem(10, makeItem(Material.IRON_SWORD, ChatColor.RED + "1. Survival Games (Blitz SG)", ChatColor.GRAY + "4m • SG4 & Kalas, Blitz Star, Refill, DM"));
        inv.setItem(11, makeItem(Material.FEATHER, ChatColor.GREEN + "2. Parkour Spiral", ChatColor.GRAY + "4m • Real Hielke Parkour Spiral tower"));
        inv.setItem(12, makeItem(Material.WATER_BUCKET, ChatColor.BLUE + "3. Multi-Stage Dropper", ChatColor.GRAY + "3.5m • 5 shafts in Delightful Dropper 2"));
        inv.setItem(13, makeItem(Material.TNT, ChatColor.GOLD + "4. TNT Run (Best of 3)", ChatColor.GRAY + "3 rounds • Jungle, Castle, Pyramid + Double Jump"));
        inv.setItem(14, makeItem(Material.GOLDEN_SHOVEL, ChatColor.AQUA + "5. Spleef / Splegg (Best of 3)", ChatColor.GRAY + "3 rounds • Golden Shovel Instamine + Snowball KB"));
        inv.setItem(15, makeItem(Material.DIAMOND_CHESTPLATE, ChatColor.DARK_RED + "6. Battle Arena (Best of 2)", ChatColor.GRAY + "2 quick rounds • Overcast Harb + 4 PvP Kits"));
        inv.setItem(16, makeItem(Material.GOLDEN_APPLE, ChatColor.DARK_GREEN + "7. Speed Survival (15m)", ChatColor.GRAY + "15m • Fresh biome, CutClean, Timber, Supply Drops, DM"));
        inv.setItem(17, makeItem(Material.BOW, ChatColor.LIGHT_PURPLE + "Optional: Pink Valley (One Shot)", ChatColor.GRAY + "Not in Championship • Cleaned 70x70 arena"));

        inv.setItem(19, makeItem(Material.REPEATER, ChatColor.AQUA + "" + ChatColor.BOLD + "Team Size: " + ChatColor.YELLOW + scoring.getTeamSizeLabel(),
                ChatColor.GRAY + "Click to cycle: 1 -> 2 -> 3 -> 4 -> 5"));
        inv.setItem(20, makeItem(Material.RED_WOOL, ChatColor.GREEN + "Auto-Balance / Randomize Teams",
                ChatColor.GRAY + "Splits online players into teams of " + scoring.getMaxTeamSize()));
        inv.setItem(21, makeItem(Material.ARROW, ChatColor.GOLD + "" + ChatColor.BOLD + "Skip Current Event (/mcm skip)",
                ChatColor.GRAY + "Ends current event & advances to next"));
        inv.setItem(22, makeItem(Material.BARRIER, ChatColor.RED + "" + ChatColor.BOLD + "STOP / RETURN TO LOBBY",
                ChatColor.GRAY + "Emergency stop tournament & return"));
        inv.setItem(24, makeItem(Material.CLOCK, ChatColor.YELLOW + "Reset All Scores",
                ChatColor.GRAY + "Set everyone's points back to 0"));
        p.openInventory(inv);
    }

    private void openTeamGui(Player p) {
        Inventory inv = Bukkit.createInventory(null, 27, TEAM_GUI_TITLE);
        int max = scoring.getMaxTeamSize();
        for (int i = 0; i < TeamScoring.TEAM_NAMES.length; i++) {
            String tName = TeamScoring.TEAM_NAMES[i];
            List<String> members = scoring.getOnlineMembers(tName);
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Slots: " + ChatColor.WHITE + members.size() + " / " + max
                    + ChatColor.GRAY + " | Team Pts: " + ChatColor.GREEN + scoring.getTeamPoints(tName));
            if (members.isEmpty()) {
                lore.add(ChatColor.DARK_GRAY + "No players yet");
            } else {
                for (String m : members) lore.add(ChatColor.YELLOW + "• " + ChatColor.WHITE + m);
            }
            lore.add("");
            lore.add(members.size() >= max ? ChatColor.RED + "Team is full!" : ChatColor.GREEN + "Click to join!");
            inv.setItem(TEAM_GUI_SLOTS[i], makeItem(TeamScoring.TEAM_WOOLS[i],
                    scoring.teamColor(tName) + "" + ChatColor.BOLD + tName + " Team", lore.toArray(new String[0])));
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

        if (!title.equals(ADMIN_GUI_TITLE) && !title.equals(TEAM_GUI_TITLE) && !title.equals(KIT_GUI_TITLE)) {
            if (currentGame == GameType.LOBBY && p.getGameMode() != GameMode.CREATIVE) {
                e.setCancelled(true);
            }
            return;
        }
        e.setCancelled(true);
        if (e.getCurrentItem() == null) return;

        if (title.equals(TEAM_GUI_TITLE)) {
            int slot = e.getRawSlot();
            if (slot == 22) {
                scoring.setTeam(p.getUniqueId(), null);
                p.sendMessage(ChatColor.YELLOW + "You left your team (now Solo).");
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1f, 1f);
                scoring.applyTabNames();
                refreshLobbyItems();
                p.closeInventory();
                return;
            }
            for (int i = 0; i < TEAM_GUI_SLOTS.length; i++) {
                if (slot == TEAM_GUI_SLOTS[i]) {
                    String tName = TeamScoring.TEAM_NAMES[i];
                    if (tName.equals(scoring.getTeam(p.getUniqueId()))) {
                        p.sendMessage(ChatColor.YELLOW + "You're already on " + scoring.teamColor(tName) + tName + " Team!");
                        p.closeInventory();
                        return;
                    }
                    if (scoring.getOnlineMembers(tName).size() >= scoring.getMaxTeamSize()) {
                        p.sendMessage(ChatColor.RED + tName + " Team is full (" + scoring.getMaxTeamSize() + "/" + scoring.getMaxTeamSize() + ")!");
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                        return;
                    }
                    scoring.setTeam(p.getUniqueId(), tName);
                    broadcast(ChatColor.WHITE + p.getName() + ChatColor.GRAY + " joined " + scoring.teamColor(tName) + "" + ChatColor.BOLD + tName + " Team" + ChatColor.GRAY + "!");
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.3f);
                    scoring.applyTabNames();
                    refreshLobbyItems();
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

        // Admin GUI
        if (!p.isOp()) return;
        int slot = e.getRawSlot();
        if (slot == 19) {
            setTeamSize((scoring.getMaxTeamSize() % 5) + 1);
            openAdminGui(p);
            return;
        }
        p.closeInventory();
        switch (slot) {
            case 4 -> startChampionship();
            case 10 -> { championshipMode = false; startEvent(GameType.SURVIVAL_GAMES); }
            case 11 -> { championshipMode = false; startEvent(GameType.PARKOUR); }
            case 12 -> { championshipMode = false; startEvent(GameType.DROPPER); }
            case 13 -> { championshipMode = false; startEvent(GameType.TNT_RUN); }
            case 14 -> { championshipMode = false; startEvent(GameType.SPLEEF); }
            case 15 -> { championshipMode = false; startEvent(GameType.KIT_PVP); }
            case 16 -> { championshipMode = false; startEvent(GameType.SURVIVAL); }
            case 17 -> { championshipMode = false; startEvent(GameType.ONE_SHOT); }
            case 20 -> randomizeTeams();
            case 21 -> skipCurrentEvent(p);
            case 22 -> { championshipMode = false; returnToLobby(ChatColor.YELLOW + "Returned to lobby."); }
            case 24 -> { scoring.resetScores(); broadcast(ChatColor.RED + "Tournament scores reset!"); }
        }
    }

    // ==================== CHAMPIONSHIP & MULTI-ROUND FLOW ====================

    private void startChampionship() {
        championshipMode = true;
        playlistIndex = 0;
        scoring.resetScores();
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "=== MINECRAFT MONDAY CHAMPIONSHIP STARTING! ===");
        broadcast(ChatColor.YELLOW + "7 Events | Holistic Team Split Scoring | Highest Team Total Wins!");
        startEvent(playlist.get(playlistIndex));
    }

    private void startEvent(GameType game) {
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
        startFreezeTicks = (game == GameType.KIT_PVP) ? 4 : 3;

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

        totalDuration = Math.max(1, timeLeft);
        for (Player p : Bukkit.getOnlinePlayers()) {
            String sub = game == GameType.KIT_PVP ? ChatColor.YELLOW + "Pick your Kit (4s)!"
                    : (maxRounds > 1 ? ChatColor.YELLOW + "Round " + roundNumber + " of " + maxRounds : ChatColor.YELLOW + "Get Ready!");
            p.sendTitle(ChatColor.GOLD + game.display, sub, 5, 45, 10);
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
                    if (startFreezeTicks == 0) {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (currentGame == GameType.KIT_PVP) {
                                KitType k = playerKits.getOrDefault(p.getUniqueId(), KitType.KNIGHT);
                                applyKitPvpGear(p, k); // removes kit book, equips chosen kit cleanly
                            }
                            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GO!", "", 0, 20, 5);
                            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.5f);
                        }
                    }
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

    private void endSubRound(String reason) {
        if (gameLoopTask != null) { gameLoopTask.cancel(); gameLoopTask = null; }
        restoreArenas();

        // Award end-of-round bonuses
        if (currentGame == GameType.ONE_SHOT) {
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
            broadcast(ChatColor.YELLOW + "Round over (" + reason + ")! Next round (" + roundNumber + "/" + maxRounds + ") in 4 seconds...");
            GameType active = currentGame;
            pendingTransitionTask = new BukkitRunnable() {
                @Override public void run() {
                    pendingTransitionTask = null;
                    startSubRound(active);
                }
            }.runTaskLater(this, 80L);
            return;
        }

        endCurrentEvent(reason);
    }

    private void endCurrentEvent(String reason) {
        cancelTasks();
        restoreArenas();
        GameType finished = currentGame;
        currentGame = GameType.LOBBY;
        if (sgWorld != null) sgWorld.getWorldBorder().reset();
        if (sg4World != null) sg4World.getWorldBorder().reset();
        if (oneshotWorld != null) oneshotWorld.getWorldBorder().reset();
        setSurvivalWorldActive(false);

        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "=== " + finished.display + " OVER! (" + reason + ") ===");
        printLeaderboard();
        returnToLobby(null);

        if (championshipMode) {
            playlistIndex++;
            if (playlistIndex < playlist.size()) {
                GameType next = playlist.get(playlistIndex);
                broadcast(ChatColor.AQUA + "Next event (" + next.display + ") starting in 10 seconds! (/mcm skip to start sooner)");
                pendingTransitionTask = new BukkitRunnable() {
                    @Override public void run() {
                        pendingTransitionTask = null;
                        if (championshipMode && currentGame == GameType.LOBBY) startEvent(next);
                    }
                }.runTaskLater(this, 200L);
            } else {
                championshipMode = false;
                announceChampion();
            }
        }
    }

    private void announceChampion() {
        List<Map.Entry<String, Integer>> teamStandings = scoring.getStandings();
        List<Map.Entry<UUID, Integer>> indStandings = scoring.getIndividualStandings();

        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "==================================");
        broadcast(ChatColor.YELLOW + "" + ChatColor.BOLD + "   ★ CHAMPIONSHIP COMPLETE! ★");
        if (!teamStandings.isEmpty()) {
            String winName = teamStandings.get(0).getKey();
            int winPts = teamStandings.get(0).getValue();
            broadcast(ChatColor.GREEN + "" + ChatColor.BOLD + "   CHAMPIONS: " + winName + ChatColor.GREEN + " (" + winPts + " pts)");
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.sendTitle(winName + ChatColor.GOLD + " WINS!",
                        ChatColor.YELLOW + "Champions with " + winPts + " total points!", 10, 80, 20);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
        if (!indStandings.isEmpty()) {
            String mvp = scoring.playerName(indStandings.get(0).getKey());
            broadcast(ChatColor.AQUA + "   Individual MVP: " + ChatColor.WHITE + mvp + ChatColor.GRAY + " (" + indStandings.get(0).getValue() + " pts)");
        }
        broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "==================================");

        Location l = world.getSpawnLocation();
        for (int i = 0; i < 6; i++) {
            new BukkitRunnable() {
                @Override public void run() {
                    Firework fw = world.spawn(l.clone().add(rng.nextInt(11) - 5, 2, rng.nextInt(11) - 5), Firework.class);
                    FireworkMeta fm = fw.getFireworkMeta();
                    fm.addEffect(FireworkEffect.builder().withColor(Color.YELLOW, Color.AQUA, Color.LIME).with(FireworkEffect.Type.BALL_LARGE).build());
                    fm.setPower(1);
                    fw.setFireworkMeta(fm);
                }
            }.runTaskLater(this, i * 12L);
        }
    }

    private void returnToLobby(String msg) {
        cancelTasks();
        restoreArenas();
        currentGame = GameType.LOBBY;
        if (sgWorld != null) sgWorld.getWorldBorder().reset();
        if (sg4World != null) sg4World.getWorldBorder().reset();
        if (oneshotWorld != null) oneshotWorld.getWorldBorder().reset();
        setSurvivalWorldActive(false);
        if (msg != null && !msg.isEmpty()) broadcast(msg);
        Location lobby = world.getSpawnLocation().clone().add(0.5, 1.0, 0.5);
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
        useSg4Map = !useSg4Map; // alternate between SG4 and Kalas each game
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
        broadcast(ChatColor.GREEN + "Climb Hielke's Parkour Spiral to the summit (Y=230)! Checkpoints give +3 pts!");
    }

    // 3. MULTI-STAGE DROPPER (Real Delightful Dropper 2 — 5 Stages!)
    private void setupDropper() {
        World w = dropperWorld != null ? dropperWorld : world;
        List<DropperStage> pool = new ArrayList<>(ALL_DROPPER_STAGES);
        Collections.shuffle(pool);
        activeDropperStages.clear();
        for (int i = 0; i < 5; i++) activeDropperStages.add(pool.get(i));
        Arrays.fill(stageFinishCounts, 0);

        DropperStage first = activeDropperStages.get(0);
        Location spawn = new Location(w, first.x(), first.y(), first.z(), first.yaw(), 45f);
        ItemStack retry = makeItem(Material.FEATHER, ChatColor.AQUA + "Retry Current Drop", ChatColor.GRAY + "Right-click to restart this stage");
        for (Player p : Bukkit.getOnlinePlayers()) {
            resetPlayer(p, GameMode.ADVENTURE);
            dropperStageIdx.put(p.getUniqueId(), 0);
            p.getInventory().setItem(4, retry.clone());
            p.teleport(spawn);
            p.sendTitle(ChatColor.AQUA + "Stage 1 / 5: " + first.name(), ChatColor.YELLOW + "Aim for: " + first.hint() + "!", 5, 45, 10);
        }
        timeLeft = 210;
        broadcast(ChatColor.BLUE + "Clear 5 themed Dropper shafts! Watch the title for what to land on (Water, Slime, Cobwebs, or Vines)!");
        broadcast(ChatColor.AQUA + "Stage 1: " + ChatColor.WHITE + first.name() + ChatColor.YELLOW + " — Aim for " + first.hint() + "!");
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
        broadcast(label + ChatColor.YELLOW + " dropped at (" + x + ", " + sy + ", " + z + ")! Right-click for +10 bonus pts!");
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
        if (currentGame == GameType.LOBBY) {
            resetPlayer(p, GameMode.ADVENTURE);
            p.teleport(world.getSpawnLocation().clone().add(0.5, 1.0, 0.5));
            giveLobbyItems(p);
        } else {
            p.setGameMode(GameMode.SPECTATOR);
            sanitizeAttributes(p);
            p.sendMessage(ChatColor.YELLOW + "Event in progress (" + currentGame.display + "). Spectating until next round!");
        }
        scoring.applyTabNames();
        p.sendMessage(ChatColor.AQUA + "Tip: " + ChatColor.WHITE + "Right-click your Wool (or type " + ChatColor.YELLOW + "/team" + ChatColor.WHITE + ") to pick a team!");
        if (p.isOp()) {
            p.sendMessage(ChatColor.GOLD + "Admin: " + ChatColor.WHITE + "Right-click Compass or type " + ChatColor.YELLOW + "/mcm" + ChatColor.WHITE + " (" + ChatColor.YELLOW + "/mcm skip" + ChatColor.WHITE + " skips an event).");
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
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
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();

        if (currentGame == GameType.SURVIVAL && e.getAction() == Action.RIGHT_CLICK_BLOCK
                && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.CHEST) {
            Block cb = e.getClickedBlock();
            String cKey = cb.getX() + "," + cb.getY() + "," + cb.getZ();
            if (survivalSupplyChests.remove(cKey)) {
                scoring.addIndividualPoints(p.getUniqueId(), 10, "Supply Drop Claim!");
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
                broadcast(ChatColor.GOLD + p.getName() + ChatColor.YELLOW + " reached a Supply Crate first! (+10 pts)");
            }
        }

        ItemStack item = e.getItem();
        if (item == null) return;

        if (currentGame == GameType.LOBBY) {
            if (item.getType() == Material.COMPASS && p.isOp()) {
                cancelItemUse(e, p);
                openAdminGui(p);
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
            int idx = dropperStageIdx.getOrDefault(p.getUniqueId(), 0);
            if (idx < activeDropperStages.size()) {
                DropperStage st = activeDropperStages.get(idx);
                World w = dropperWorld != null ? dropperWorld : world;
                p.teleport(new Location(w, st.x(), st.y(), st.z(), st.yaw(), 45f));
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
        if (currentGame == GameType.LOBBY && e.getPlayer().getGameMode() != GameMode.CREATIVE) {
            e.setCancelled(true);
        } else if (currentGame == GameType.PARKOUR || currentGame == GameType.DROPPER
                || currentGame == GameType.TNT_RUN || currentGame == GameType.SPLEEF || currentGame == GameType.ONE_SHOT) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Location to = e.getTo();
        if (to == null) return;
        Player p = e.getPlayer();

        if (currentGame == GameType.LOBBY) {
            if (to.getY() < world.getSpawnLocation().getY() - 50) {
                p.teleport(world.getSpawnLocation().clone().add(0.5, 1.0, 0.5));
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
                p.setGameMode(GameMode.SPECTATOR);
                broadcast(ChatColor.GREEN + p.getName() + " reached the Parkour Summit #" + place + "! (+" + pts + " pts)");
                if (finishOrder.size() >= alivePlayers.size()) endSubRound("All players finished!");
            }
        }

        // Dropper landing detection (Water, Slime, Cobwebs, Vines) & multi-stage progression
        if (currentGame == GameType.DROPPER && to.getY() <= 106.0) {
            long now = System.currentTimeMillis();
            if (now - dropperAdvanceCooldown.getOrDefault(p.getUniqueId(), 0L) < 1000L) return;

            Material feetMat = to.getBlock().getType();
            Material headMat = to.clone().add(0, 0.5, 0).getBlock().getType();
            Material belowMat = to.clone().subtract(0, 0.6, 0).getBlock().getType();

            if (isSafeDropperBlock(feetMat) || isSafeDropperBlock(headMat) || isSafeDropperBlock(belowMat)
                    || (to.getY() <= 104.5 && p.isOnGround() && p.getFallDistance() < 3.0f)) {
                int idx = dropperStageIdx.getOrDefault(p.getUniqueId(), 0);
                if (idx >= activeDropperStages.size()) return;

                dropperAdvanceCooldown.put(p.getUniqueId(), now);
                p.setFallDistance(0f);
                stageFinishCounts[idx]++;
                int sPlace = stageFinishCounts[idx];
                int stagePts = sPlace == 1 ? 12 : sPlace == 2 ? 8 : sPlace == 3 ? 6 : 4;
                scoring.addIndividualPoints(p.getUniqueId(), stagePts, "Dropper Stage " + (idx + 1) + "/5 (#" + sPlace + ")");

                int nextIdx = idx + 1;
                dropperStageIdx.put(p.getUniqueId(), nextIdx);
                if (nextIdx >= activeDropperStages.size()) {
                    if (!finishOrder.contains(p.getUniqueId())) {
                        finishOrder.add(p.getUniqueId());
                        int overall = finishOrder.size();
                        int finishPts = overall == 1 ? 25 : overall == 2 ? 15 : overall == 3 ? 10 : 5;
                        scoring.addIndividualPoints(p.getUniqueId(), finishPts, "All 5 Stages Clear #" + overall + "!");
                        p.setGameMode(GameMode.SPECTATOR);
                        broadcast(ChatColor.GREEN + p.getName() + " finished all 5 Dropper stages (#" + overall + ")!");
                        if (finishOrder.size() >= alivePlayers.size()) endSubRound("All players finished!");
                    }
                } else {
                    DropperStage nextStage = activeDropperStages.get(nextIdx);
                    World w = dropperWorld != null ? dropperWorld : world;
                    p.teleport(new Location(w, nextStage.x(), nextStage.y(), nextStage.z(), nextStage.yaw(), 45f));
                    p.sendTitle(ChatColor.AQUA + "Stage " + (nextIdx + 1) + " / 5: " + nextStage.name(),
                            ChatColor.YELLOW + "Aim for: " + nextStage.hint() + "!", 5, 40, 10);
                    p.sendMessage(ChatColor.AQUA + "Stage " + (nextIdx + 1) + "/5 (" + nextStage.name() + ") — Aim for " + ChatColor.YELLOW + nextStage.hint() + "!");
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.3f);
                }
            }
        }
    }

    private boolean isSafeDropperBlock(Material m) {
        return m == Material.WATER || m == Material.SLIME_BLOCK || m == Material.COBWEB
                || m == Material.VINE || m == Material.TWISTING_VINES
                || m == Material.TWISTING_VINES_PLANT || m == Material.HONEY_BLOCK;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
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
                broadcast(ChatColor.LIGHT_PURPLE + killer.getName() + ChatColor.GRAY + " sniped " + ChatColor.WHITE + victim.getName() + ChatColor.GOLD + " (+5 pts)");
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
        victim.setGameMode(GameMode.SPECTATOR);

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
            broadcast(ChatColor.RED + victim.getName() + ChatColor.GRAY + " was eliminated by " + ChatColor.GOLD + killer.getName() + ChatColor.YELLOW + " (+" + killPts + " pts)");
        } else {
            broadcast(ChatColor.RED + victim.getName() + ChatColor.GRAY + " was eliminated!");
        }

        // Authentic MCM split survival bonus (+4 per team, split across living teammates)
        scoring.awardSplitTeamBonus(alivePlayers, 4, "Survival Bonus");
        if (scoring.isOneSideRemaining(alivePlayers)) {
            endSubRound("Last standing!");
        }
    }

    @EventHandler
    public void onBlockDamage(BlockDamageEvent e) {
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
        if (!(e.getEntity() instanceof Player p)) return;
        if (currentGame == GameType.LOBBY && p.getGameMode() == GameMode.CREATIVE) return;
        if (currentGame != GameType.SURVIVAL_GAMES && currentGame != GameType.KIT_PVP && currentGame != GameType.SURVIVAL) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (currentGame != GameType.SURVIVAL_GAMES && currentGame != GameType.KIT_PVP && currentGame != GameType.SURVIVAL) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPortal(PlayerPortalEvent e) {
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
            broadcast(ChatColor.GOLD + "#" + (i + 1) + " " + top.get(i).getKey() + ChatColor.GRAY + " - " + ChatColor.GREEN + top.get(i).getValue() + " pts");
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
        List<Map.Entry<String, Integer>> top = scoring.getStandings();
        String roundSuffix = maxRounds > 1 ? ChatColor.GRAY + " (R" + roundNumber + "/" + maxRounds + ")" : "";
        if (bossBar != null) {
            String title = currentGame == GameType.LOBBY
                    ? ChatColor.GOLD + "" + ChatColor.BOLD + "★ MINECRAFT MONDAY ★ " + ChatColor.GRAY + "┃ " + ChatColor.YELLOW + "Lobby"
                    : ChatColor.GOLD + "" + ChatColor.BOLD + "⚔ " + currentGame.display.toUpperCase() + roundSuffix + " " + ChatColor.GRAY + "┃ " + ChatColor.AQUA + formatTime(timeLeft);
            bossBar.setTitle(title);
            bossBar.setProgress(currentGame == GameType.LOBBY ? 1.0 : Math.max(0.0, Math.min(1.0, (double) timeLeft / Math.max(1, totalDuration))));
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (bossBar != null && !bossBar.getPlayers().contains(p)) bossBar.addPlayer(p);
            Scoreboard sb = sm.getNewScoreboard();
            Objective obj = sb.registerNewObjective("mcm", Criteria.DUMMY, ChatColor.GOLD + "" + ChatColor.BOLD + "★ MINECRAFT MONDAY ★");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            int line = 13;
            obj.getScore(ChatColor.DARK_GRAY + "━━━━━━━━━━━━━━━━━━").setScore(line--);
            obj.getScore(ChatColor.YELLOW + "» " + ChatColor.WHITE + currentGame.display + roundSuffix).setScore(line--);
            if (currentGame != GameType.LOBBY) {
                obj.getScore(ChatColor.AQUA + "⏱ " + ChatColor.WHITE + "Time: " + ChatColor.YELLOW + formatTime(timeLeft)).setScore(line--);
                if (currentGame == GameType.SURVIVAL) {
                    String phaseStr = gracePeriod
                            ? ChatColor.GREEN + "Grace (" + formatTime(Math.max(0, timeLeft - 600)) + ")"
                            : (survivalDeathmatch ? ChatColor.DARK_RED + "DEATHMATCH" : ChatColor.RED + "Hunt");
                    obj.getScore(ChatColor.GRAY + "Phase: " + phaseStr).setScore(line--);
                    int halfB = survivalWorld != null ? (int) Math.round(survivalWorld.getWorldBorder().getSize() / 2.0) : 160;
                    obj.getScore(ChatColor.GRAY + "Border: " + ChatColor.WHITE + "±" + halfB + "m " + ChatColor.DARK_GRAY + "| " + ChatColor.AQUA + "Alive: " + alivePlayers.size()).setScore(line--);
                }
            } else {
                String t = scoring.getTeam(p.getUniqueId());
                obj.getScore(ChatColor.GRAY + "Team: " + (t == null ? ChatColor.WHITE + "Solo" : scoring.teamColor(t) + t) + ChatColor.DARK_GRAY + " (" + scoring.getMaxTeamSize() + "/team)").setScore(line--);
            }
            obj.getScore(" ").setScore(line--);
            obj.getScore(ChatColor.GOLD + "" + ChatColor.BOLD + "✦ STANDINGS").setScore(line--);
            for (int i = 0; i < Math.min(5, top.size()); i++) {
                obj.getScore(ChatColor.GOLD + "#" + (i + 1) + " " + top.get(i).getKey() + ChatColor.GRAY + " ┃ " + ChatColor.GREEN + top.get(i).getValue()).setScore(line--);
            }
            p.setScoreboard(sb);
        }
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
                    ChatColor.GRAY + "Start events, skip, toggle team size, reset"));
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

    private void broadcast(String msg) {
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "[" + ChatColor.GOLD + "MCM" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET + msg);
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
