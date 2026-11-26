package com.mcm.games.dodgebolt;

import com.mcm.api.McmFx;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Dodgebolt, the championship finale, following MCC's rules (research: game-plugins/research/mcc_archive/dodgebolt/db-1):
 * two sides on either half of a pool arena, one bow each, only two arrows ("bolts") in play, one hit eliminates,
 * the deck shrinks into the lava as kills happen and shots are fired, first to 3 round wins.
 */
public final class DodgeboltGame implements McmGame, Listener {

    public static final String ID = "dodgebolt";
    public static final String WORLD = "mcc_" + ID;
    /** custom_model_data values the server pack maps to the Dodgebolt bow and bolt models (build_pack.py, "Dodgebolt" section). */
    public static final int BOW_MODEL = 7301, BOLT_MODEL = 7302;
    private static final String TAG = "mcm_dodgebolt";

    private enum State { IDLE, COUNTDOWN, LIVE, ROUND_END, CELEBRATE }

    /** One of the two finalist groups. key is a ctx side key, or null if a single side was split in two. */
    private static final class Group {
        final String key;
        final String name;
        final ChatColor color;
        final DyeColor dye;
        final List<UUID> members = new ArrayList<>();
        Group(String key, String name, ChatColor color, DyeColor dye) {
            this.key = key; this.name = name; this.color = color; this.dye = dye;
        }
        String label() { return color + "" + ChatColor.BOLD + name + ChatColor.RESET; }
    }

    private final DodgeboltPlugin plugin;
    private final NamespacedKey arrowKey;
    private final Random random = new Random();

    private McmGameContext ctx;
    private State state = State.IDLE;
    private World world;
    private Arena arena;
    private final Group[] groups = new Group[2];
    private final Map<UUID, Integer> groupOf = new HashMap<>();
    private final Set<UUID> alive = new HashSet<>();
    private final Set<UUID> arrowItems = new HashSet<>();
    private final Set<UUID> arrowEntities = new HashSet<>();
    private final Map<UUID, TextDisplay> indicators = new HashMap<>();
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final int[] score = new int[2];
    private final int[] matchElims = new int[2];
    private final Map<UUID, Integer> kills = new HashMap<>();
    private boolean testMode;      // one group empty (e.g. an admin testing alone)
    private boolean finished;
    private boolean checkQueued;
    private int round, roundsPlayed, phaseSeconds, elapsed, roundKills, roundShots, tickCount, seatCounter;
    private long generation;
    private SpectatorGuard guard;

    // shrink controller (MCC: warning, red edge, ring drops into the lava, 1 s between consecutive shrinks)
    private int pendingShrinks, shrinkWarnTicks, shrinkGapTicks, lastShrinkRequest;
    private boolean ringMarked;

    // config
    private int roundsToWin, maxRounds, roundSeconds, countdownSeconds, intermissionSeconds, arrowsInPlay;
    private int shotsPerShrink, shrinkAfterThird, maxShrink, warnTicks, gapTicks, idleShrinkSeconds;
    private List<Integer> killShrinks;
    private int ptsElim, ptsRound, ptsMatch;
    private boolean declareWinner = true;
    private boolean winByTwo, crowdInStands, glowingBolts;

    public DodgeboltGame(DodgeboltPlugin plugin) {
        this.plugin = plugin;
        this.arrowKey = new NamespacedKey(plugin, "bolt");
    }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Dodgebolt"; }
    @Override public Material icon() { return Material.BOW; }
    @Override public String description() { return "The finale: top two sides duel with bows. Only two arrows, one hit and you're out, the arena shrinks. First to 3 rounds."; }
    @Override public boolean useCoreScoreboard() { return true; }

    // =================================================================== lifecycle

    /** Called at plugin enable so the world exists at startup (LESSONS: per-world combat settings). */
    void prepareWorld() {
        try { ensureWorld(); } catch (RuntimeException ex) { plugin.getLogger().warning("Could not prepare " + WORLD + ": " + ex.getMessage()); }
    }

    @Override
    public void start(McmGameContext context) {
        if (state != State.IDLE) cleanup();
        this.ctx = context;
        this.finished = false;
        generation++;
        loadConfig();
        if (guard == null) guard = new SpectatorGuard(plugin, this::spectatorZone);

        world = ensureWorld();
        arena = new Arena(world);

        pickGroups();
        arena.setDyes(groups[0].dye, groups[1].dye);
        long t0 = System.nanoTime();
        arena.buildAll();
        plugin.getLogger().info("Dodgebolt arena built in " + (System.nanoTime() - t0) / 1_000_000 + " ms");

        Arrays.fill(score, 0);
        Arrays.fill(matchElims, 0);
        kills.clear();
        round = 0;
        roundsPlayed = 0;
        seatCounter = 0;

        // Everyone not in the finale watches from the stands.
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (!groupOf.containsKey(p.getUniqueId())) toCrowd(p, null);
        }
        String vs = groups[0].label() + ChatColor.GRAY + "  vs  " + groups[1].label();
        ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "THE FINAL DUEL: DODGEBOLT! " + vs);
        ctx.broadcast(ChatColor.GRAY + "Eliminate each other with 1-shot-kill arrows. There are only ever " + arrowsInPlay
                + " arrows in play. The arena shrinks as more arrows are fired, so don't fall. First to " + roundsToWin
                + " round wins takes the crown.");
        if (testMode) ctx.broadcast(ChatColor.YELLOW + "Only one finalist group: running in test mode (rounds end on the timer).");

        repeat(this::secondTick, 20L, 20L);
        repeat(this::tick, 1L, 1L);
        startRound();
    }

    @Override
    public void stop() {
        cleanup();
    }

    private void loadConfig() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        roundsToWin = Math.max(1, c.getInt("rounds-to-win", 3));
        winByTwo = c.getBoolean("win-by-two", false);
        maxRounds = Math.max(roundsToWin * 2 - 1, c.getInt("max-rounds", 7));
        roundSeconds = Math.max(30, c.getInt("round-seconds", 180));
        countdownSeconds = Math.max(1, c.getInt("countdown-seconds", 3));
        intermissionSeconds = Math.max(2, c.getInt("intermission-seconds", 15));
        arrowsInPlay = Math.max(1, c.getInt("arrows-in-play", 2));
        killShrinks = c.getIntegerList("shrink.first-kills");
        if (killShrinks.isEmpty()) killShrinks = List.of(2, 2, 1);
        shrinkAfterThird = Math.max(0, c.getInt("shrink.per-later-kill", 1));
        shotsPerShrink = Math.max(0, c.getInt("shrink.shots-per-shrink", 6));
        maxShrink = Math.max(0, Math.min(Arena.MAX_SHRINK, c.getInt("shrink.max-blocks", 8)));
        warnTicks = Math.max(1, c.getInt("shrink.warning-ticks", 40));
        gapTicks = Math.max(0, c.getInt("shrink.gap-ticks", 20));
        idleShrinkSeconds = Math.max(0, c.getInt("shrink.idle-seconds", 30));
        crowdInStands = c.getBoolean("crowd-in-stands", true);
        glowingBolts = c.getBoolean("glowing-bolts", true);
        // MCC: "No coins are awarded in Dodgebolt. This one's for the win!" - the winner wins the event (COINS.md).
        ptsElim = c.getInt("coins.elimination", 0);
        ptsRound = c.getInt("coins.round-win", 0);
        ptsMatch = c.getInt("coins.match-win", 0);
        declareWinner = c.getBoolean("coins.winner-wins-event", true);
    }

    /** Cancel everything, remove our entities. Safe to call repeatedly. Never calls finish. */
    private void cleanup() {
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
        removeIndicators();
        removeGameEntities();
        if (world != null) {
            for (Player p : world.getPlayers()) {
                p.getInventory().clear();
                p.setGlowing(false);
                p.setFireTicks(0);
            }
        }
        state = State.IDLE;
        alive.clear();
        groupOf.clear();
        arrowItems.clear();
        arrowEntities.clear();
        checkQueued = false;
        resetShrink();
        // The stadium stays standing between games (it is rebuilt in the match colours on the next start).
    }

    private void removeGameEntities() {
        if (world == null || Bukkit.getWorld(WORLD) == null) return;
        for (Entity e : world.getEntities()) {
            if (e instanceof Item || e instanceof AbstractArrow || e instanceof Firework
                    || (e instanceof Display && e.getScoreboardTags().contains(TAG))) e.remove();
        }
    }

    private void finishOnce(String reason) {
        if (finished) return;
        finished = true;
        McmGameContext c = ctx;
        cleanup();
        if (c != null) c.finish(reason);
    }

    private World ensureWorld() {
        World w = Bukkit.getWorld(WORLD);
        if (w == null) {
            w = new WorldCreator(WORLD).generator(new VoidGenerator()).environment(World.Environment.NORMAL)
                    .generateStructures(false).createWorld();
        }
        if (w == null) throw new IllegalStateException("Could not create world " + WORLD);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.NATURAL_REGENERATION, false);
        w.setTime(6000);
        w.setStorm(false);
        w.setThundering(false);
        w.setSpawnLocation(0, Arena.RIM_Y + 1, -(Arena.WALL_Z + 2));
        return w;
    }

    // =================================================================== picking the finalists

    private void pickGroups() {
        groupOf.clear();
        Map<String, List<Player>> sides = ctx.sides();
        List<String> top;
        try {
            top = new ArrayList<>(ctx.topSides(2));
        } catch (Throwable t) { // older core without topSides: fall back to the two biggest sides
            top = new ArrayList<>(sides.keySet());
            top.sort((a, b) -> Integer.compare(sides.get(b).size(), sides.get(a).size()));
            if (top.size() > 2) top = top.subList(0, 2);
        }
        top.removeIf(k -> sides.get(k) == null || sides.get(k).isEmpty());

        if (top.size() >= 2) {
            for (int g = 0; g < 2; g++) {
                String key = top.get(g);
                groups[g] = makeGroup(key, ChatColor.stripColor(ctx.sideLabel(key)), colorOfSide(key), g == 1 ? groups[0] : null);
                for (Player p : sides.get(key)) groups[g].members.add(p.getUniqueId());
            }
        } else {
            // Only one side present: split its players into two halves.
            List<Player> all = new ArrayList<>(ctx.participants());
            String key = top.isEmpty() ? null : top.get(0);
            if (key != null) all = new ArrayList<>(sides.get(key));
            String base = key == null ? "Finalists" : ChatColor.stripColor(ctx.sideLabel(key));
            ChatColor col = key == null ? ChatColor.WHITE : colorOfSide(key);
            groups[0] = makeGroup(null, base + " West", col, null);
            groups[1] = makeGroup(null, base + " East", col, groups[0]);
            Collections.shuffle(all, random);
            for (int i = 0; i < all.size(); i++) groups[i % 2].members.add(all.get(i).getUniqueId());
        }
        for (int g = 0; g < 2; g++) for (UUID u : groups[g].members) groupOf.put(u, g);
        testMode = groups[0].members.isEmpty() || groups[1].members.isEmpty();
    }

    private ChatColor colorOfSide(String key) {
        if (key.startsWith("TEAM:")) {
            ChatColor c = ctx.teamColor(key.substring(5));
            return c == null ? ChatColor.WHITE : c;
        }
        return ChatColor.WHITE;
    }

    private Group makeGroup(String key, String name, ChatColor color, Group other) {
        DyeColor dye = dyeOf(color);
        if (dye == DyeColor.WHITE || dye == DyeColor.BLACK || dye == DyeColor.LIGHT_GRAY) {
            // A white/black side would vanish on the white deck: use the classic orange vs blue.
            dye = other == null ? DyeColor.ORANGE : DyeColor.LIGHT_BLUE;
        }
        if (other != null && other.dye == dye) {
            boolean bluish = dye == DyeColor.BLUE || dye == DyeColor.LIGHT_BLUE || dye == DyeColor.CYAN;
            dye = bluish ? DyeColor.ORANGE : DyeColor.LIGHT_BLUE;
            color = bluish ? ChatColor.GOLD : ChatColor.AQUA;
        } else if (color == ChatColor.WHITE || color == ChatColor.BLACK || color == ChatColor.GRAY) {
            color = dye == DyeColor.ORANGE ? ChatColor.GOLD : ChatColor.AQUA;
        }
        return new Group(key, name, color, dye);
    }

    private static DyeColor dyeOf(ChatColor c) {
        switch (c) {
            case RED: case DARK_RED: return DyeColor.RED;
            case GOLD: return DyeColor.ORANGE;
            case YELLOW: return DyeColor.YELLOW;
            case GREEN: return DyeColor.LIME;
            case DARK_GREEN: return DyeColor.GREEN;
            case DARK_AQUA: return DyeColor.CYAN;
            case AQUA: return DyeColor.LIGHT_BLUE;
            case BLUE: case DARK_BLUE: return DyeColor.BLUE;
            case DARK_PURPLE: return DyeColor.PURPLE;
            case LIGHT_PURPLE: return DyeColor.PINK;
            case GRAY: return DyeColor.LIGHT_GRAY;
            case DARK_GRAY: return DyeColor.GRAY;
            case BLACK: return DyeColor.BLACK;
            default: return DyeColor.WHITE;
        }
    }

    // =================================================================== rounds

    private List<Player> onlineMembers(int g) {
        List<Player> out = new ArrayList<>();
        Set<UUID> present = new HashSet<>();
        for (Player p : ctx.participants()) present.add(p.getUniqueId());
        for (UUID u : groups[g].members) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && p.isOnline() && present.contains(u)) out.add(p);
        }
        return out;
    }

    private void startRound() {
        removeIndicators();
        removeGameEntities();
        arrowItems.clear();
        arrowEntities.clear();
        alive.clear();
        resetShrink();
        arena.resetFloor();

        List<Player> a = onlineMembers(0), b = onlineMembers(1);
        if (a.isEmpty() && b.isEmpty()) { finishOnce("No finalists left"); return; }
        if (!testMode && (a.isEmpty() || b.isEmpty())) {
            int winner = a.isEmpty() ? 1 : 0;
            ctx.broadcast(groups[1 - winner].label() + ChatColor.GRAY + " has no players left and forfeits the finale.");
            celebrate(winner);
            return;
        }

        round++;
        clutchShown.clear();
        roundKills = 0;
        roundShots = 0;
        elapsed = 0;
        for (int g = 0; g < 2; g++) {
            List<Player> list = g == 0 ? a : b;
            for (int i = 0; i < list.size(); i++) {
                Player p = list.get(i);
                preparePlayer(p, g);
                p.teleport(arena.spawn(g, i, list.size()));
                alive.add(p.getUniqueId());
            }
        }
        state = State.COUNTDOWN;
        phaseSeconds = FastTest.secs(countdownSeconds);
        McmFx.title(world.getPlayers(), ChatColor.GOLD + "" + ChatColor.BOLD + "Round " + round, scoreLine(), 5, 40, 10);
        updateSidebar();
    }

    private void preparePlayer(Player p, int g) {
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.setItemOnCursor(null);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setAllowFlight(false);
        p.setFlying(false);
        AttributeInstance max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(max == null ? 20.0 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setGlowing(false);
        for (org.bukkit.potion.PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        p.getInventory().setItem(0, bow(g));
        p.getInventory().setHeldItemSlot(0);
    }

    private ItemStack bow(int g) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta m = bow.getItemMeta();
        m.setDisplayName(groups[g].color + "" + ChatColor.BOLD + "Dodgebolt Bow");
        m.setUnbreakable(true);
        m.setCustomModelData(BOW_MODEL);
        m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        bow.setItemMeta(m);
        return bow;
    }

    /** A bolt is a spectral-arrow item with the bolt model, so the custom look never touches normal arrows. */
    private ItemStack boltStack(int amount) {
        ItemStack s = new ItemStack(Material.SPECTRAL_ARROW, amount);
        ItemMeta m = s.getItemMeta();
        m.setDisplayName(ChatColor.WHITE + "" + ChatColor.BOLD + "Bolt");
        m.setCustomModelData(BOLT_MODEL);
        m.getPersistentDataContainer().set(arrowKey, PersistentDataType.BYTE, (byte) 1);
        s.setItemMeta(m);
        return s;
    }

    private static boolean isAmmo(ItemStack s) {
        return s != null && (s.getType() == Material.SPECTRAL_ARROW || s.getType() == Material.ARROW || s.getType() == Material.TIPPED_ARROW);
    }

    private static int boltsHeld(Player p) {
        int n = 0;
        for (ItemStack s : p.getInventory().getContents()) if (isAmmo(s)) n += s.getAmount();
        if (isAmmo(p.getItemOnCursor())) n += p.getItemOnCursor().getAmount();
        return n;
    }

    private void goLive() {
        state = State.LIVE;
        elapsed = 0;
        lastShrinkRequest = 0;
        // MCC 22+: the side behind on score gets both arrows on their side; tied = one arrow on each side.
        if (score[0] != score[1]) {
            int loser = score[0] < score[1] ? 0 : 1;
            for (int i = 0; i < arrowsInPlay; i++) spawnArrow(loser);
            ctx.broadcast(groups[loser].label() + ChatColor.GRAY + " is behind, so both arrows drop on their side.");
        } else {
            for (int i = 0; i < arrowsInPlay; i++) spawnArrow(i % 2);
        }
        McmFx.title(world.getPlayers(), ChatColor.GREEN + "" + ChatColor.BOLD + "GO!", ChatColor.GRAY + "Grab the arrows!", 0, 20, 10);
        playAll("mcc.go", Sound.ENTITY_ENDER_DRAGON_GROWL, 0.8f, 1.6f);
    }

    private void secondTick() {
        if (ctx == null || finished) return;
        switch (state) {
            case COUNTDOWN -> {
                if (phaseSeconds <= 0) { goLive(); break; }
                McmFx.title(world.getPlayers(), ChatColor.AQUA + "" + ChatColor.BOLD + phaseSeconds,
                        ChatColor.GRAY + "Starting in...", 0, 22, 0);
                playAll("mcc.countdown", Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                ctx.setTimer(phaseSeconds, countdownSeconds);
                phaseSeconds--;
            }
            case LIVE -> liveSecond();
            case ROUND_END -> {
                if (phaseSeconds > intermissionSeconds) break; // match decided, celebration pending
                ctx.setTimer(Math.max(0, phaseSeconds), intermissionSeconds);
                if (phaseSeconds > 0 && phaseSeconds <= intermissionSeconds) {
                    McmFx.actionBar(world.getPlayers(), ChatColor.WHITE + "NEXT ROUND IN: " + ChatColor.YELLOW
                            + String.format("00:%02d", phaseSeconds));
                }
                if (phaseSeconds-- <= 0) startRound();
            }
            default -> { }
        }
        updateSidebar();
    }

    private void liveSecond() {
        elapsed++;
        ctx.setTimer(Math.max(0, roundSeconds - elapsed), roundSeconds);
        // Not an MCC rule (labelled default): if nothing has shrunk the arena for a while, it shrinks anyway,
        // so a round where nobody shoots still closes in.
        if (idleShrinkSeconds > 0 && elapsed - lastShrinkRequest >= idleShrinkSeconds) requestShrink(1);
        if (elapsed >= roundSeconds) endRoundByTime();
    }

    private void endRoundByTime() {
        int a = aliveCount(0), b = aliveCount(1);
        if (testMode) { endRound(a > 0 ? 0 : (b > 0 ? 1 : -1), "Time!"); return; }
        if (a > b) endRound(0, "Time! More players standing.");
        else if (b > a) endRound(1, "Time! More players standing.");
        else endRound(-1, "Time! Equal players standing.");
    }

    private int aliveCount(int g) {
        int n = 0;
        for (UUID u : alive) {
            Integer gg = groupOf.get(u);
            Player p = Bukkit.getPlayer(u);
            if (gg != null && gg == g && p != null && p.isOnline()) n++;
        }
        return n;
    }

    private void queueCheck() {
        if (checkQueued) return;
        checkQueued = true;
        later(() -> { checkQueued = false; checkRoundEnd(); }, 1L);
    }

    private void checkRoundEnd() {
        if (state != State.LIVE && state != State.COUNTDOWN) return;
        int a = aliveCount(0), b = aliveCount(1);
        boolean aOut = a == 0 && !groups[0].members.isEmpty();
        boolean bOut = b == 0 && !groups[1].members.isEmpty();
        if (aOut && bOut) endRound(-1, "Both sides wiped out!");
        else if (aOut) endRound(1, null);
        else if (bOut) endRound(0, null);
        else if (state == State.LIVE) {
            if (a == 1 && b >= 1 && !testMode) clutchNotice(0, b);
            if (b == 1 && a >= 1 && !testMode) clutchNotice(1, a);
        }
    }

    private final Set<String> clutchShown = new HashSet<>();
    private void clutchNotice(int g, int vs) {
        String k = round + ":" + g;
        if (vs < 2 || !clutchShown.add(k)) return;
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            if (p != null && groupOf.get(u) == g) McmFx.title(p, "", ChatColor.GOLD + "Last one standing! 1v" + vs, 0, 30, 10);
        }
    }

    private void clearRoundObjects() {
        resetShrink();
        removeIndicators();
        removeGameEntities();
        arrowItems.clear();
        arrowEntities.clear();
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) { p.getInventory().clear(); p.setItemOnCursor(null); }
        }
    }

    /** winner: 0/1, or -1 for a drawn round. */
    private void endRound(int winner, String reason) {
        if (state != State.LIVE && state != State.COUNTDOWN) return;
        state = State.ROUND_END;
        roundsPlayed++;
        clearRoundObjects();

        String sub;
        if (winner >= 0) {
            score[winner]++;
            arena.setScore(score[0], score[1]);
            awardSide(winner, ptsRound, "Dodgebolt round win");
            boolean flawless = aliveCount(winner) == onlineMembers(winner).size() && !testMode;
            sub = groups[winner].label() + ChatColor.WHITE + " win the round!  " + scoreLine() + (flawless ? ChatColor.GOLD + "  FLAWLESS" : "");
            ctx.broadcast(ChatColor.GOLD + "» " + groups[winner].label() + ChatColor.WHITE + " win round " + round + "!"
                    + ChatColor.GRAY + "  (" + ChatColor.stripColor(scoreLine()) + ")" + (reason != null ? ChatColor.GRAY + " " + reason : ""));
            playAll("mcc.crowd.cheer", Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1f);
        } else {
            sub = ChatColor.GRAY + "Draw. " + (reason == null ? "" : reason + " ") + scoreLine();
            ctx.broadcast(ChatColor.GOLD + "» " + ChatColor.GRAY + "Round " + round + " is a draw. " + (reason == null ? "" : reason));
        }
        // MCC: "Round Over!" in big pink letters, then "NEXT ROUND IN: 00:17".
        McmFx.title(world.getPlayers(), ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Round Over!", sub, 5, 50, 10);
        playAll(null, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);

        int champ = matchWinner();
        if (champ >= 0) {
            phaseSeconds = Integer.MAX_VALUE; // never start another round
            later(() -> celebrate(champ), FastTest.ticks(50L));
            return;
        }
        phaseSeconds = FastTest.secs(intermissionSeconds);
        updateSidebar();
    }

    // ------------------------------------------------------------------ admin testing hooks
    // (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    private String celebrateReason = "Dodgebolt over";

    /** Ends the round countdown, the intermission, the pause before the celebration, or the celebration. */
    public boolean skipWait() {
        if (ctx == null || finished) return false;
        switch (state) {
            case COUNTDOWN -> {
                phaseSeconds = 0;
                secondTick();   // normal GO transition
                return true;
            }
            case ROUND_END -> {
                if (phaseSeconds > intermissionSeconds) {   // match decided: celebrate now (the pending call is then a no-op)
                    int champ = matchWinner();
                    if (champ < 0) return false;
                    celebrate(champ);
                } else {
                    phaseSeconds = 0;
                    secondTick();   // normal next-round transition
                }
                return true;
            }
            case CELEBRATE -> {
                finishOnce(celebrateReason);
                return true;
            }
            default -> { return false; }
        }
    }

    /** Ends the current round now: no winner, no score, no points; the next round starts (first-to-N, so no "last" round). */
    public boolean skipRound() {
        if (ctx == null || finished) return false;
        if (state == State.ROUND_END) return phaseSeconds <= intermissionSeconds && skipWait();
        if (state != State.COUNTDOWN && state != State.LIVE) return false;
        state = State.ROUND_END;
        clearRoundObjects();
        ctx.broadcast(ChatColor.GRAY + "Round " + round + " was skipped by an admin (no score, no points).");
        phaseSeconds = 0;
        secondTick();   // the normal intermission end: startRound()
        return true;
    }

    /** Returns the champion index if the match is decided, else -1. */
    private int matchWinner() {
        for (int g = 0; g < 2; g++) {
            int lead = score[g] - score[1 - g];
            if (score[g] >= roundsToWin && (!winByTwo || lead >= 2)) return g;
        }
        if (roundsPlayed >= maxRounds) {
            if (score[0] != score[1]) return score[0] > score[1] ? 0 : 1;
            if (matchElims[0] != matchElims[1]) return matchElims[0] > matchElims[1] ? 0 : 1;
            if (groups[0].key != null && groups[1].key != null) {
                int pa = ctx.sidePoints(groups[0].key), pb = ctx.sidePoints(groups[1].key);
                if (pa != pb) return pa > pb ? 0 : 1;
            }
            return 0;
        }
        return -1;
    }

    private void celebrate(int w) {
        if (state == State.CELEBRATE || finished) return;
        state = State.CELEBRATE;
        clearRoundObjects();
        awardSide(w, ptsMatch, "Dodgebolt champions");
        if (declareWinner && groups[w].key != null) ctx.setEventWinner(groups[w].key);
        Group g = groups[w];
        String bar = g.color + "" + ChatColor.BOLD + "==============================";
        ctx.broadcast(bar);
        ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "  ♛ " + g.label() + ChatColor.GOLD + ChatColor.BOLD + " TAKE THE CROWN!");
        ctx.broadcast(ChatColor.GRAY + "  Final score " + ChatColor.stripColor(scoreLine()));
        UUID best = null;
        for (Map.Entry<UUID, Integer> e : kills.entrySet()) if (best == null || e.getValue() > kills.get(best)) best = e.getKey();
        if (best != null) {
            OfflinePlayer bp = Bukkit.getOfflinePlayer(best);
            ctx.broadcast(ChatColor.GRAY + "  Sharpest shot: " + ChatColor.WHITE + bp.getName() + ChatColor.GRAY + " (" + kills.get(best) + " eliminations)");
        }
        ctx.broadcast(bar);
        // MCC: gold crown above the team name in team colour, "TAKE THE CROWN" underneath, over the live arena.
        McmFx.title(Bukkit.getOnlinePlayers(), ChatColor.GOLD + "♛ " + g.label() + ChatColor.GOLD + " ♛",
                ChatColor.GOLD + "" + ChatColor.BOLD + "TAKE THE CROWN", 10, 120, 20);
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!McmFx.packSound(p, "mcc.victory.win", 1f, 1f)) p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            McmFx.packSound(p, "mcc.crowd.cheer", 0.8f, 1f);
        }
        // The champions stand in the middle of their half for the fireworks; everyone else stays in the stands.
        List<Player> champs = onlineMembers(w);
        arena.resetFloor();
        for (int i = 0; i < champs.size(); i++) {
            Player p = champs.get(i);
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.setGlowing(true);
            Location l = arena.spawn(w, i, champs.size()).clone();
            l.setZ(w == 0 ? -9.5 : 10.5);
            p.teleport(l);
        }
        final Color c1 = g.dye.getFireworkColor();
        final int[] n = {0};
        repeat(() -> {
            if (n[0]++ >= 16) return;
            for (int k = 0; k < 3; k++) {
                double x = -10 + random.nextDouble() * 20;
                double z = (random.nextBoolean() ? -1 : 1) * (3 + random.nextDouble() * 12);
                launchFirework(new Location(world, x, Arena.FLOOR_Y + 1, z), c1);
            }
        }, 5L, 10L);
        celebrateReason = ChatColor.stripColor(g.name) + " won Dodgebolt " + score[w] + "-" + score[1 - w];
        later(() -> finishOnce(celebrateReason), FastTest.ticks(200L));
        updateSidebar();
    }

    private void launchFirework(Location l, Color c) {
        try {
            McmFx.teamFirework(l, c, random.nextBoolean());
        } catch (Throwable t) { // older core
            Firework fw = l.getWorld().spawn(l, Firework.class);
            FireworkMeta m = fw.getFireworkMeta();
            m.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BALL_LARGE).withColor(c).withFade(Color.WHITE).trail(true).build());
            m.setPower(1);
            fw.setFireworkMeta(m);
        }
    }

    private void awardSide(int g, int perPlayer, String reason) {
        if (perPlayer <= 0) return;
        Group grp = groups[g];
        List<Player> online = onlineMembers(g);
        if (online.isEmpty()) return;
        if (grp.key != null) {
            List<Player> sideNow = ctx.sides().get(grp.key);
            int n = sideNow == null ? online.size() : Math.max(1, sideNow.size());
            ctx.addSidePoints(grp.key, perPlayer * n, reason);
        } else {
            for (Player p : online) ctx.addPoints(p.getUniqueId(), perPlayer, reason);
        }
    }

    // =================================================================== eliminations

    private void eliminate(Player victim, Player shooter, boolean fell) {
        UUID v = victim.getUniqueId();
        if (!alive.remove(v)) return;
        Integer g = groupOf.get(v);
        if (g == null) return;
        roundKills++;
        removeIndicator(v);
        // Arrows the victim was holding drop back into play on their own side.
        int held = boltsHeld(victim);
        victim.getInventory().clear();
        victim.setItemOnCursor(null);
        Location at = victim.getLocation();
        for (int i = 0; i < held; i++) {
            if (fell) spawnArrow(g);
            else dropBolt(at.clone().add(0, 0.3, 0), -1, false);
        }

        world.spawnParticle(Particle.FIREWORK, at.clone().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.05);
        world.spawnParticle(Particle.WAX_OFF, at.clone().add(0, 1, 0), 20, 0.4, 0.6, 0.4, 0.5);
        world.playSound(at, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1f);
        String vName = groups[g].color + victim.getName();
        if (shooter != null) {
            Integer sg = groupOf.get(shooter.getUniqueId());
            if (sg != null) matchElims[sg]++;
            kills.merge(shooter.getUniqueId(), 1, Integer::sum);
            if (ptsElim > 0) ctx.addKillCoins(shooter.getUniqueId(), ptsElim, v, victim.getName(), "You eliminated " + victim.getName());
            String sName = (sg == null ? ChatColor.WHITE : groups[sg].color) + shooter.getName();
            ctx.broadcast(ChatColor.DARK_GRAY + "☠ " + vName + ChatColor.GRAY + " was tagged by " + sName);
            if (ptsElim <= 0) McmFx.title(shooter, "", ChatColor.GREEN + "Eliminated " + vName, 0, 25, 5);
            shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f);
        } else {
            matchElims[1 - g]++;
            ctx.broadcast(ChatColor.DARK_GRAY + "☠ " + vName + ChatColor.GRAY + (fell ? " fell into the lava" : " is out"));
        }
        if (!McmFx.packSound(victim, "mcc.death", 1f, 1f)) victim.playSound(victim.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.8f);
        McmFx.title(victim, ChatColor.RED + "" + ChatColor.BOLD + "ELIMINATED", ChatColor.GRAY + "Watch from the stands until the next round", 0, 40, 10);
        victim.setFireTicks(0);
        toCrowd(victim, arena.rimSpot(g, seatCounter++));

        // MCC: the arena shrinks for kills (2, 2, 1 for the first three, then 1 per kill), capped at 8 blocks.
        if (state == State.LIVE) {
            int n = roundKills <= killShrinks.size() ? killShrinks.get(roundKills - 1) : shrinkAfterThird;
            requestShrink(n);
        }
        queueCheck();
        updateSidebar();
    }

    /** Out players, non-finalists and late joiners watch from the stands (or in spectator mode if crowd-in-stands is off). */
    private void toCrowd(Player p, Location at) {
        if (p == null || !p.isOnline()) return;
        removeIndicator(p.getUniqueId());
        Location spot = at != null ? at : arena.seat(seatCounter++);
        if (!crowdInStands) {
            SpectatorGuard.makeSpectator(p, overview());
            return;
        }
        p.closeInventory();
        p.getInventory().clear();
        p.setItemOnCursor(null);
        for (org.bukkit.potion.PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(pe.getType());
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.setGlowing(false);
        if (p.isInsideVehicle()) p.leaveVehicle();
        p.setVelocity(new Vector());
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(false);
        p.teleport(spot);
        // Spectators can see who carries an arrow.
        for (TextDisplay d : indicators.values()) p.showEntity(plugin, d);
    }

    /** Spectator-mode fallback: above the stands opposite the scoreboards. */
    private Location overview() {
        return new Location(world, -(Arena.WALK_X + 2) + 0.5, Arena.RIM_Y + 9, 0.5, -90f, 30f);
    }

    private SpectatorGuard.Zone spectatorZone(Player p) {
        if (state == State.IDLE || world == null || !p.getWorld().equals(world)) return null;
        org.bukkit.util.BoundingBox box = new org.bukkit.util.BoundingBox(
                -Arena.WALK_X - Arena.TIERS - 1, Arena.FLOOR_Y - 4, -Arena.WALK_Z - Arena.TIERS - 1,
                Arena.WALK_X + Arena.TIERS + 2, Arena.OUTER_TOP + 25, Arena.WALK_Z + Arena.TIERS + 2);
        return new SpectatorGuard.Zone(world, box, overview());
    }

    // =================================================================== shrinking

    private void resetShrink() {
        pendingShrinks = 0;
        shrinkWarnTicks = 0;
        shrinkGapTicks = 0;
        ringMarked = false;
    }

    /** Queue n one-block shrinks (never beyond the cap). */
    private void requestShrink(int n) {
        if (state != State.LIVE || n <= 0 || arena == null) return;
        lastShrinkRequest = elapsed;
        int queued = arena.shrunk() + (ringMarked ? 1 : 0) + pendingShrinks;
        pendingShrinks += Math.max(0, Math.min(n, maxShrink - queued));
    }

    private void shrinkTick() {
        if (state != State.LIVE) return;
        if (ringMarked) {
            if (--shrinkWarnTicks > 0) return;
            arena.dropRing();
            ringMarked = false;
            shrinkGapTicks = gapTicks;
            world.playSound(new Location(world, 0.5, Arena.FLOOR_Y + 3, 0.5), Sound.BLOCK_LAVA_EXTINGUISH, 2f, 0.7f);
            world.playSound(new Location(world, 0.5, Arena.FLOOR_Y + 3, 0.5), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.6f, 1.4f);
            updateSidebar();
            return;
        }
        if (shrinkGapTicks > 0) { shrinkGapTicks--; return; }
        if (pendingShrinks <= 0) return;
        pendingShrinks--;
        if (arena.shrunk() >= maxShrink || !arena.markNextRing()) { pendingShrinks = 0; return; }
        ringMarked = true;
        shrinkWarnTicks = warnTicks;
        // MCC chat line, sent once per one-block shrink (so twice in a row for a two-block shrink).
        for (Player p : world.getPlayers()) {
            p.sendMessage(ChatColor.RED + "⚠ " + ChatColor.RED + "The arena is about to shrink!");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
        }
    }

    // =================================================================== arrows

    private void spawnArrow(int side) {
        if (arena == null || world == null) return;
        Location l = arena.arrowSpot(side);
        dropBolt(l, -1, true);
    }

    /** Drop a single bolt item. lastSide = side that last fired it (-1 unknown). */
    private Item dropBolt(Location l, int lastSide, boolean effect) {
        Item it = world.dropItem(l, boltStack(1));
        it.setVelocity(new Vector(0, 0, 0));
        it.setGlowing(glowingBolts);
        it.setUnlimitedLifetime(true);
        it.setPickupDelay(5);
        it.addScoreboardTag(TAG);
        if (lastSide >= 0) it.getPersistentDataContainer().set(arrowKey, PersistentDataType.INTEGER, lastSide);
        arrowItems.add(it.getUniqueId());
        if (effect) {
            world.spawnParticle(Particle.END_ROD, l, 15, 0.2, 0.4, 0.2, 0.02);
            world.playSound(l, Sound.ENTITY_ITEM_PICKUP, 1f, 0.5f);
        }
        return it;
    }

    private int countArrows() {
        int n = 0;
        for (UUID u : arrowItems) { Entity e = Bukkit.getEntity(u); if (e instanceof Item i && e.isValid()) n += i.getItemStack().getAmount(); }
        for (UUID u : arrowEntities) { Entity e = Bukkit.getEntity(u); if (e != null && e.isValid()) n++; }
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) n += boltsHeld(p);
        }
        return n;
    }

    private int fewerArrowsSide() {
        int[] c = new int[2];
        for (UUID u : arrowItems) {
            Entity e = Bukkit.getEntity(u);
            if (e != null && e.isValid()) c[Arena.sideOfZ(e.getLocation().getZ())]++;
        }
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            Integer g = groupOf.get(u);
            if (p != null && g != null) c[g] += boltsHeld(p);
        }
        return c[0] <= c[1] ? 0 : 1;
    }

    /** Side an arrow lost at l goes back to: the half it was lost over, or the side that did not shoot it. */
    private int homeSide(Location l, Integer lastShooterSide) {
        if (Math.abs(l.getX() - 0.5) <= Arena.WALL_X + 1 && !Arena.onCentreLine(l.getZ())) return Arena.sideOfZ(l.getZ());
        return lastShooterSide != null ? 1 - lastShooterSide : fewerArrowsSide();
    }

    private static boolean inLava(Entity e) {
        Location l = e.getLocation();
        return l.getY() < Arena.FLOOR_Y + 0.5 || l.getBlock().getType() == Material.LAVA;
    }

    /** Every tick: shrink timing, arrow trails, holder indicators; every 5 ticks: arrow and player upkeep. */
    private void tick() {
        if (ctx == null || world == null || state == State.IDLE) return;
        tickCount++;
        shrinkTick();
        if (state == State.LIVE) {
            for (UUID u : arrowEntities) {
                Entity e = Bukkit.getEntity(u);
                if (e instanceof AbstractArrow a && a.isValid() && !a.isInBlock())
                    world.spawnParticle(Particle.END_ROD, a.getLocation(), 1, 0, 0, 0, 0);
            }
            updateIndicators();
        }
        if (tickCount % 5 == 0) upkeep();
    }

    private void upkeep() {
        if (state == State.LIVE) {
            for (Iterator<UUID> it = arrowItems.iterator(); it.hasNext(); ) {
                Entity e = Bukkit.getEntity(it.next());
                if (e == null || !e.isValid()) { it.remove(); continue; }
                Location l = e.getLocation();
                if (inLava(e) || !Arena.inPool(l)) {
                    Integer last = e.getPersistentDataContainer().get(arrowKey, PersistentDataType.INTEGER);
                    int amount = ((Item) e).getItemStack().getAmount();
                    int to = homeSide(l, last);
                    e.remove();
                    it.remove();
                    for (int i = 0; i < amount; i++) spawnArrow(to);
                }
            }
            for (UUID u : new ArrayList<>(arrowEntities)) {
                Entity e = Bukkit.getEntity(u);
                if (e == null || !e.isValid()) { arrowEntities.remove(u); continue; }
                if (e.getTicksLived() > 200 || inLava(e) || !Arena.inPool(e.getLocation())) {
                    Integer last = e.getPersistentDataContainer().get(arrowKey, PersistentDataType.INTEGER);
                    arrowEntities.remove(u);
                    Location l = e.getLocation();
                    e.remove();
                    spawnArrow(homeSide(l, last));
                }
            }
            // "There are only ever 2 arrows in play": top up if one was lost some other way.
            if (tickCount % 20 == 0) for (int have = countArrows(); have < arrowsInPlay; have++) spawnArrow(fewerArrowsSide());
        }
        for (Player p : new ArrayList<>(world.getPlayers())) {
            UUID u = p.getUniqueId();
            if (alive.contains(u)) {
                if (state == State.LIVE && p.getLocation().getY() < Arena.FLOOR_Y + 0.8) eliminate(p, null, true);
                else if (p.getLocation().getY() < Arena.FLOOR_Y - 4) {
                    Integer g = groupOf.get(u);
                    if (g != null) p.teleport(arena.spawn(g, 0, 1));
                }
                continue;
            }
            if (p.getGameMode() == GameMode.SPECTATOR) continue;
            // Crowd: keep them in the stands (out of the pool, inside the stadium).
            Location l = p.getLocation();
            boolean overPool = Math.abs(l.getX() - 0.5) < Arena.WALL_X + 0.5 && Math.abs(l.getZ() - 0.5) < Arena.WALL_Z + 0.5;
            if (!Arena.inStadium(l) || (overPool && l.getY() < Arena.RIM_Y + 0.5) || l.getY() < Arena.RIM_Y - 1) {
                toCrowd(p, arena.seat(seatCounter++));
            }
            if (p.getFireTicks() > 0) p.setFireTicks(0);
        }
    }

    // ------------------------------------------------------------------ arrow-holder indicator (spectators only)

    private void updateIndicators() {
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            boolean has = boltsHeld(p) > 0;
            TextDisplay d = indicators.get(u);
            if (has && (d == null || !d.isValid())) {
                indicators.put(u, spawnIndicator(p));
            } else if (!has && d != null) {
                removeIndicator(u);
            } else if (d != null) {
                d.teleport(p.getLocation().add(0, 2.45, 0));
            }
        }
    }

    private TextDisplay spawnIndicator(Player p) {
        TextDisplay d = world.spawn(p.getLocation().add(0, 2.45, 0), TextDisplay.class, t -> {
            t.setPersistent(false);
            t.addScoreboardTag(TAG);
            t.text(Component.text(" ➚ ", NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
            t.setBillboard(Display.Billboard.CENTER);
            t.setDefaultBackground(false);
            t.setBackgroundColor(Color.fromARGB(230, 134, 64, 200));
            t.setShadowed(false);
            t.setSeeThrough(false);
            t.setTeleportDuration(2);
            t.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.2f, 1.2f, 1.2f), new AxisAngle4f()));
            t.setVisibleByDefault(true);
        });
        // MCC: only spectators can see it, so hide it from everyone still playing.
        for (UUID u : alive) {
            Player q = Bukkit.getPlayer(u);
            if (q != null) q.hideEntity(plugin, d);
        }
        return d;
    }

    private void removeIndicator(UUID u) {
        TextDisplay d = indicators.remove(u);
        if (d != null) d.remove();
    }

    private void removeIndicators() {
        for (TextDisplay d : indicators.values()) d.remove();
        indicators.clear();
    }

    // =================================================================== HUD

    private String scoreLine() {
        return groups[0].color + "" + ChatColor.BOLD + score[0] + ChatColor.GRAY + " - " + groups[1].color + ChatColor.BOLD + score[1];
    }

    private String pips(int g) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roundsToWin; i++) sb.append(i < score[g] ? ChatColor.GOLD + "●" : ChatColor.DARK_GRAY + "○");
        return sb.toString();
    }

    private String heads(int g) {
        StringBuilder sb = new StringBuilder();
        int tot = onlineMembers(g).size();
        int al = (state == State.LIVE || state == State.COUNTDOWN || state == State.ROUND_END) ? aliveCount(g) : tot;
        for (int i = 0; i < tot; i++) sb.append(i < al ? groups[g].color + "■" : ChatColor.DARK_GRAY + "☠");
        return sb.toString();
    }

    private void updateSidebar() {
        if (ctx == null || groups[0] == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.WHITE + "" + ChatColor.BOLD + "THE FINAL DUEL" + ChatColor.GRAY + "  Round " + Math.max(1, round));
        lines.add(groups[0].color + short12(groups[0].name) + " " + pips(0) + ChatColor.GOLD + " ♛ " + pips(1) + " " + groups[1].color + short12(groups[1].name));
        lines.add(heads(0) + ChatColor.GRAY + "  vs  " + heads(1));
        if (state == State.LIVE) {
            lines.add(ChatColor.YELLOW + "Arrows in play: " + ChatColor.WHITE + countArrows());
            if (arena.shrunk() > 0 || ringMarked) lines.add(ChatColor.RED + "Arena shrunk: " + ChatColor.WHITE + arena.shrunk() + "/" + maxShrink);
        } else if (state == State.CELEBRATE) {
            lines.add(ChatColor.GOLD + "" + ChatColor.BOLD + "CHAMPIONS DECIDED");
        }
        ctx.setSidebarLines(lines);
    }

    private static String short12(String s) { return s.length() > 12 ? s.substring(0, 12) : s; }

    /** Pack sound for players with the server pack, the vanilla sound for everyone else (packKey null = vanilla only). */
    private void playAll(String packKey, Sound fallback, float vol, float pitch) {
        for (Player p : world.getPlayers()) {
            if (packKey != null && McmFx.packSound(p, packKey, vol, pitch)) continue;
            if (fallback != null) p.playSound(p.getLocation(), fallback, vol, pitch);
        }
    }

    // =================================================================== scheduling

    private void repeat(Runnable r, long delay, long period) {
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, r, delay, period));
    }

    private void later(Runnable r, long delay) {
        tasks.add(Bukkit.getScheduler().runTaskLater(plugin, r, delay));
    }

    // =================================================================== listeners (only while running, only in our world)

    private boolean active(World w) {
        return state != State.IDLE && world != null && w != null && w.equals(world);
    }

    private boolean isAlive(Player p) { return alive.contains(p.getUniqueId()); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!active(p.getWorld()) || !isAlive(p)) return;
        Location to = e.getTo();
        Location from = e.getFrom();
        if (state == State.COUNTDOWN) {
            if (to.getX() != from.getX() || to.getZ() != from.getZ()) {
                Location back = from.clone();
                back.setYaw(to.getYaw());
                back.setPitch(to.getPitch());
                e.setTo(back);
            }
            return;
        }
        Integer g = groupOf.get(p.getUniqueId());
        if (g == null) return;
        if (state == State.LIVE && to.getY() < Arena.FLOOR_Y + 0.8) {
            // Fell off the deck into the lava: eliminate next tick (no teleport from inside the move event).
            Bukkit.getScheduler().runTask(plugin, () -> { if (state == State.LIVE && isAlive(p)) eliminate(p, null, true); });
            return;
        }
        // Players can't cross into the other team's half (the black centre line).
        boolean wrong = g == 0 ? to.getZ() > Arena.A_EDGE : to.getZ() < Arena.B_EDGE;
        if (wrong) {
            Location back = from.clone();
            if (g == 0 ? back.getZ() > Arena.A_EDGE : back.getZ() < Arena.B_EDGE) back.setZ(g == 0 ? Arena.A_EDGE - 0.05 : Arena.B_EDGE + 0.05);
            back.setYaw(to.getYaw());
            back.setPitch(to.getPitch());
            e.setTo(back);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!active(e.getEntity().getWorld())) return;
        if (e.getEntity() instanceof Player || e.getEntity() instanceof Item || e.getEntity() instanceof Display) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent e) {
        if (active(e.getEntity().getWorld()) && (e.getEntity() instanceof Player || e.getEntity() instanceof Item)) e.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (active(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (active(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (active(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (!active(e.getPlayer().getWorld())) return;
        if (e.getClickedBlock() != null && e.getAction() == org.bukkit.event.block.Action.PHYSICAL) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (!active(p.getWorld())) return;
        Item it = e.getItemDrop();
        if (state == State.LIVE && isAlive(p) && isAmmo(it.getItemStack())) {
            it.setItemStack(boltStack(it.getItemStack().getAmount()));
            it.setGlowing(glowingBolts);
            it.setUnlimitedLifetime(true);
            it.addScoreboardTag(TAG);
            arrowItems.add(it.getUniqueId());
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent e) {
        if (!active(e.getEntity().getWorld())) return;
        if (e.getEntity() instanceof Player p && state == State.LIVE && isAlive(p) && arrowItems.contains(e.getItem().getUniqueId())) {
            e.setCancelled(false);
            arrowItems.remove(e.getItem().getUniqueId());
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1f, 1.4f);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler
    public void onPickupArrow(PlayerPickupArrowEvent e) {
        if (active(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onMerge(ItemMergeEvent e) {
        if (active(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onDespawn(ItemDespawnEvent e) {
        if (active(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShoot(EntityShootBowEvent e) {
        if (!active(e.getEntity().getWorld())) return;
        if (!(e.getEntity() instanceof Player p) || state != State.LIVE || !isAlive(p)) { e.setCancelled(true); return; }
        Integer g = groupOf.get(p.getUniqueId());
        if (g == null || !(e.getProjectile() instanceof AbstractArrow arrow)) { e.setCancelled(true); return; }
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setGlowing(true);
        if (arrow instanceof SpectralArrow sa) sa.setGlowingTicks(0);
        arrow.addScoreboardTag(TAG);
        arrow.getPersistentDataContainer().set(arrowKey, PersistentDataType.INTEGER, g);
        arrowEntities.add(arrow.getUniqueId());
        world.playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.8f);
        // MCC 6+: the arena shrinks one block for every 6 shots fired.
        roundShots++;
        if (shotsPerShrink > 0 && roundShots % shotsPerShrink == 0) requestShrink(1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHit(ProjectileHitEvent e) {
        if (!active(e.getEntity().getWorld())) return;
        if (!(e.getEntity() instanceof AbstractArrow arrow)) return;
        Integer side = arrow.getPersistentDataContainer().get(arrowKey, PersistentDataType.INTEGER);
        if (side == null || !arrowEntities.contains(arrow.getUniqueId())) {
            // Not one of ours: just get rid of it.
            if (e.getHitEntity() instanceof Player) e.setCancelled(true);
            arrow.remove();
            return;
        }
        Entity hit = e.getHitEntity();
        if (hit != null) {
            if (hit instanceof Player victim && isAlive(victim) && state == State.LIVE) {
                Integer vg = groupOf.get(victim.getUniqueId());
                if (vg != null && !vg.equals(side)) {
                    e.setCancelled(true);
                    arrowEntities.remove(arrow.getUniqueId());
                    arrow.remove();
                    Player shooter = arrow.getShooter() instanceof Player sp ? sp : null;
                    Location drop = victim.getLocation().add(0, 0.3, 0);
                    // Impact: white/gold sparkle burst (032911).
                    world.spawnParticle(Particle.FIREWORK, drop.clone().add(0, 1, 0), 30, 0.3, 0.5, 0.3, 0.08);
                    world.spawnParticle(Particle.WAX_ON, drop.clone().add(0, 1, 0), 15, 0.4, 0.5, 0.4, 0.5);
                    eliminate(victim, shooter, false);
                    if (state == State.LIVE) dropBolt(drop, side, false);
                    return;
                }
            }
            e.setCancelled(true); // teammates / crowd: the arrow passes through
            return;
        }
        // Hit a block: it becomes a pick-up-able bolt right here.
        arrowEntities.remove(arrow.getUniqueId());
        Location l = arrow.getLocation();
        Vector v = arrow.getVelocity();
        if (v.lengthSquared() > 1.0E-4) l.subtract(v.clone().normalize().multiply(0.3));
        arrow.remove();
        world.spawnParticle(Particle.FIREWORK, l, 8, 0.1, 0.1, 0.1, 0.03);
        if (state == State.LIVE) {
            Item it = dropBolt(l, side, false);
            it.setVelocity(new Vector(0, 0, 0));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        removeIndicator(p.getUniqueId());
        if (state == State.IDLE || !alive.contains(p.getUniqueId())) return;
        Integer g = groupOf.get(p.getUniqueId());
        alive.remove(p.getUniqueId());
        int held = boltsHeld(p);
        p.getInventory().clear();
        if (g != null && state == State.LIVE) for (int i = 0; i < held; i++) spawnArrow(g);
        if (ctx != null) ctx.broadcast(ChatColor.GRAY + p.getName() + " left the finale.");
        queueCheck();
    }

    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        if (state == State.IDLE || arena == null) return false;
        toCrowd(player, null);
        player.sendMessage(ChatColor.GOLD + "The Dodgebolt finale is in progress - you are watching from the stands.");
        return true;
    }
}
