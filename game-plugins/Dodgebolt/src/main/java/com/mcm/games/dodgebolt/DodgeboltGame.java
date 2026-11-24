package com.mcm.games.dodgebolt;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.util.Vector;

import java.util.*;

/**
 * Dodgebolt: the championship finale. Two sides, one bow each, a handful of arrows, one hit and you're out.
 */
public final class DodgeboltGame implements McmGame, Listener {

    public static final String ID = "dodgebolt";
    public static final String WORLD = "mcc_" + ID;

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
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final int[] score = new int[2];
    private final int[] matchElims = new int[2];
    private final Map<UUID, Integer> kills = new HashMap<>();
    private boolean testMode;      // one group empty (e.g. an admin testing alone)
    private boolean finished;
    private boolean checkQueued;
    private int round, roundsPlayed, phaseSeconds, elapsed, roundKills, ringsDropped;
    private boolean overtimeAnnounced;
    private long generation;
    private SpectatorGuard guard;

    // config
    private int roundsToWin, maxRounds, roundSeconds, overtimeAt, ringInterval, countdownSeconds, intermissionSeconds;
    private int startArrows, extraArrowInterval, maxArrows, ptsElim, ptsRound, ptsMatch;
    private boolean winByTwo;

    public DodgeboltGame(DodgeboltPlugin plugin) {
        this.plugin = plugin;
        this.arrowKey = new NamespacedKey(plugin, "bolt");
    }

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Dodgebolt"; }
    @Override public Material icon() { return Material.BOW; }
    @Override public String description() { return "The finale: top two sides duel with bows. One hit and you're out. First to 3 rounds."; }
    @Override public boolean useCoreScoreboard() { return true; }

    // =================================================================== lifecycle

    @Override
    public void start(McmGameContext context) {
        if (state != State.IDLE) cleanup();
        this.ctx = context;
        this.finished = false;
        generation++;
        loadConfig();
        if (guard == null) guard = new SpectatorGuard(plugin, this::spectatorZone);

        world = ensureWorld();
        arena = new Arena(plugin, world);

        pickGroups();
        arena.setDyes(groups[0].dye, groups[1].dye);
        arena.buildAll();

        Arrays.fill(score, 0);
        Arrays.fill(matchElims, 0);
        kills.clear();
        round = 0;
        roundsPlayed = 0;

        // Everyone not in the finale watches from the stands.
        int i = 0;
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (!groupOf.containsKey(p.getUniqueId())) toStands(p, i++);
        }
        String vs = groups[0].label() + ChatColor.GRAY + "  vs  " + groups[1].label();
        ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "DODGEBOLT FINALE! " + vs);
        ctx.broadcast(ChatColor.GRAY + "One hit eliminates. Arrows landing on your half are yours. First to "
                + roundsToWin + " rounds" + (winByTwo ? " (win by 2)" : "") + ".");
        if (testMode) ctx.broadcast(ChatColor.YELLOW + "Only one finalist group: running in test mode (rounds end on the timer).");

        repeat(this::secondTick, 20L, 20L);
        repeat(this::arrowMaintenance, 5L, 5L);
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
        roundSeconds = Math.max(30, c.getInt("round-seconds", 120));
        overtimeAt = Math.max(5, Math.min(roundSeconds - 10, c.getInt("overtime-at", 60)));
        ringInterval = Math.max(1, c.getInt("ring-interval", 4));
        countdownSeconds = Math.max(1, c.getInt("countdown-seconds", 5));
        intermissionSeconds = Math.max(2, c.getInt("intermission-seconds", 6));
        startArrows = Math.max(1, c.getInt("start-arrows-per-side", 1));
        extraArrowInterval = Math.max(5, c.getInt("extra-arrow-interval", 30));
        maxArrows = Math.max(startArrows * 2, c.getInt("max-arrows", 4));
        ptsElim = c.getInt("points.elimination", 4);
        ptsRound = c.getInt("points.round-win", 8);
        ptsMatch = c.getInt("points.match-win", 25);
    }

    /** Cancel everything, remove our entities. Safe to call repeatedly. Never calls finish. */
    private void cleanup() {
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
        if (arena != null) arena.cancelTasks();
        removeGameEntities();
        if (world != null) {
            for (Player p : world.getPlayers()) {
                p.getInventory().clear();
                p.setGlowing(false);
            }
        }
        state = State.IDLE;
        alive.clear();
        groupOf.clear();
        arrowItems.clear();
        arrowEntities.clear();
        checkQueued = false;
        // Wipe the arena once the core has moved everyone out (skipped if a new game started meanwhile).
        final long gen = ++generation;
        final Arena a = arena;
        if (a != null && plugin.isEnabled()) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (state == State.IDLE && gen == generation && Bukkit.getWorld(WORLD) != null) a.clearAll();
            }, 40L);
        }
    }

    private void removeGameEntities() {
        if (world == null || Bukkit.getWorld(WORLD) == null) return;
        for (Entity e : world.getEntities()) {
            if (e instanceof Item || e instanceof AbstractArrow || e instanceof Firework) e.remove();
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
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.NATURAL_REGENERATION, false);
        w.setTime(6000);
        w.setStorm(false);
        w.setThundering(false);
        w.setSpawnLocation(0, Arena.FLOOR_Y + 1, 0);
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
        if (other != null && other.dye == dye) {
            boolean bluish = dye == DyeColor.BLUE || dye == DyeColor.LIGHT_BLUE || dye == DyeColor.CYAN;
            dye = bluish ? DyeColor.ORANGE : DyeColor.LIGHT_BLUE;
            color = bluish ? ChatColor.GOLD : ChatColor.AQUA;
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
        removeGameEntities();
        arrowItems.clear();
        arrowEntities.clear();
        alive.clear();
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
        ringsDropped = 0;
        elapsed = 0;
        overtimeAnnounced = false;
        for (int g = 0; g < 2; g++) {
            List<Player> list = g == 0 ? a : b;
            for (int i = 0; i < list.size(); i++) {
                Player p = list.get(i);
                Location pad = arena.pad(g, i, list.size());
                arena.markPad(pad, g);
                preparePlayer(p, g);
                p.teleport(pad);
                alive.add(p.getUniqueId());
            }
        }
        state = State.COUNTDOWN;
        phaseSeconds = FastTest.secs(countdownSeconds);
        for (Player p : world.getPlayers()) {
            p.sendTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "Round " + round, scoreLine(), 5, 40, 10);
        }
        updateSidebar();
    }

    private void preparePlayer(Player p, int g) {
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        p.setFireTicks(0);
        p.setFallDistance(0);
        AttributeInstance max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(max == null ? 20.0 : max.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setGlowing(false);
        for (org.bukkit.potion.PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        p.getInventory().setItem(0, bow(g));
    }

    private ItemStack bow(int g) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta m = bow.getItemMeta();
        m.setDisplayName(groups[g].color + "" + ChatColor.BOLD + "Dodgebolt Bow");
        m.setUnbreakable(true);
        m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        bow.setItemMeta(m);
        return bow;
    }

    private ItemStack arrowStack(int amount) {
        ItemStack s = new ItemStack(Material.ARROW, amount);
        ItemMeta m = s.getItemMeta();
        m.setDisplayName(ChatColor.YELLOW + "" + ChatColor.BOLD + "Bolt");
        m.getPersistentDataContainer().set(arrowKey, PersistentDataType.BYTE, (byte) 1);
        s.setItemMeta(m);
        return s;
    }

    private void goLive() {
        state = State.LIVE;
        elapsed = 0;
        // MCC rule: the side behind on score gets all the starting arrows; tied = one each (per side setting).
        int total = startArrows * 2;
        if (score[0] != score[1]) {
            int loser = score[0] < score[1] ? 0 : 1;
            for (int i = 0; i < total; i++) spawnArrow(loser);
            ctx.broadcast(groups[loser].label() + ChatColor.GRAY + " is behind and starts with all " + total + " arrows.");
        } else {
            for (int i = 0; i < startArrows; i++) { spawnArrow(0); spawnArrow(1); }
        }
        for (Player p : world.getPlayers()) {
            p.sendTitle(ChatColor.GREEN + "" + ChatColor.BOLD + "GO!", ChatColor.GRAY + "Grab the arrows!", 0, 20, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.6f, 1.6f);
        }
    }

    private void secondTick() {
        if (ctx == null || finished) return;
        switch (state) {
            case COUNTDOWN -> {
                if (phaseSeconds <= 0) { goLive(); break; }
                for (Player p : world.getPlayers()) {
                    p.sendTitle(ChatColor.YELLOW + "" + ChatColor.BOLD + phaseSeconds, ChatColor.GRAY + "Round " + round, 0, 22, 0);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1f);
                }
                ctx.setTimer(phaseSeconds, countdownSeconds);
                phaseSeconds--;
            }
            case LIVE -> liveSecond();
            case ROUND_END -> {
                if (phaseSeconds > intermissionSeconds) break; // match decided, celebration pending
                ctx.setTimer(Math.max(0, phaseSeconds), intermissionSeconds);
                if (phaseSeconds-- <= 0) startRound();
            }
            default -> { }
        }
        updateSidebar();
    }

    private void liveSecond() {
        elapsed++;
        ctx.setTimer(Math.max(0, roundSeconds - elapsed), roundSeconds);

        // Extra arrows over time.
        if (elapsed % extraArrowInterval == 0 && countArrows() + 2 <= maxArrows) {
            spawnArrow(0);
            spawnArrow(1);
            actionBarAll(ChatColor.YELLOW + "Two more arrows have dropped!");
        }
        // Safety: never fewer than the starting arrows in play.
        int have = countArrows();
        for (int i = have; i < startArrows * 2; i++) spawnArrow(i % 2);

        // Overtime: the floor collapses ring by ring, then crumbles.
        if (elapsed >= overtimeAt) {
            if (!overtimeAnnounced) {
                overtimeAnnounced = true;
                for (Player p : world.getPlayers()) {
                    p.sendTitle("", ChatColor.RED + "" + ChatColor.BOLD + "OVERTIME - the floor is collapsing!", 0, 40, 10);
                    p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1f, 0.6f);
                }
            }
            if ((elapsed - overtimeAt) % ringInterval == 0) {
                if (ringsDropped < Arena.RINGS) arena.dropRing(ringsDropped++);
            }
            if (ringsDropped >= Arena.RINGS && (elapsed - overtimeAt) > Arena.RINGS * ringInterval) arena.shatter(5);
        }
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
            if (p != null && groupOf.get(u) == g) {
                p.sendTitle("", ChatColor.GOLD + "Last one standing! 1v" + vs, 0, 30, 10);
            }
        }
    }

    /** winner: 0/1, or -1 for a drawn round. */
    private void endRound(int winner, String reason) {
        if (state != State.LIVE && state != State.COUNTDOWN) return;
        state = State.ROUND_END;
        roundsPlayed++;
        arena.cancelTasks();
        removeGameEntities();
        arrowItems.clear();
        arrowEntities.clear();
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.getInventory().clear();
        }

        String title, sub;
        if (winner >= 0) {
            score[winner]++;
            awardSide(winner, ptsRound, "Dodgebolt round win");
            boolean flawless = aliveCount(winner) == onlineMembers(winner).size() && !testMode;
            title = groups[winner].label() + ChatColor.WHITE + " wins round " + round + "!";
            sub = scoreLine() + (flawless ? ChatColor.GOLD + "  FLAWLESS" : "");
            ctx.broadcast(ChatColor.GOLD + "» " + title + ChatColor.GRAY + "  (" + ChatColor.stripColor(scoreLine()) + ")"
                    + (reason != null ? ChatColor.GRAY + " " + reason : ""));
        } else {
            title = ChatColor.GRAY + "" + ChatColor.BOLD + "Round " + round + " drawn";
            sub = (reason == null ? "" : ChatColor.GRAY + reason + " ") + scoreLine();
            ctx.broadcast(ChatColor.GOLD + "» " + ChatColor.GRAY + "Round " + round + " is a draw. " + (reason == null ? "" : reason));
        }
        for (Player p : world.getPlayers()) {
            p.sendTitle(title, sub, 5, 50, 10);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }

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
        arena.cancelTasks();
        removeGameEntities();
        arrowItems.clear();
        arrowEntities.clear();
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            if (p != null) p.getInventory().clear();
        }
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
        arena.cancelTasks();
        removeGameEntities();
        awardSide(w, ptsMatch, "Dodgebolt champions");
        Group g = groups[w];
        String bar = g.color + "" + ChatColor.BOLD + "==============================";
        ctx.broadcast(bar);
        ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "  CHAMPIONS: " + g.label());
        ctx.broadcast(ChatColor.GRAY + "  Final score " + ChatColor.stripColor(scoreLine()));
        UUID best = null;
        for (Map.Entry<UUID, Integer> e : kills.entrySet()) if (best == null || e.getValue() > kills.get(best)) best = e.getKey();
        if (best != null) {
            OfflinePlayer bp = Bukkit.getOfflinePlayer(best);
            ctx.broadcast(ChatColor.GRAY + "  Sharpest shot: " + ChatColor.WHITE + bp.getName() + ChatColor.GRAY + " (" + kills.get(best) + " eliminations)");
        }
        ctx.broadcast(bar);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle(g.label(), ChatColor.GOLD + "" + ChatColor.BOLD + "ARE THE CHAMPIONS!", 10, 120, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        }
        // The champions stand on the centre of their half for the fireworks.
        List<Player> champs = onlineMembers(w);
        arena.resetFloor();
        for (int i = 0; i < champs.size(); i++) {
            Player p = champs.get(i);
            p.getInventory().clear();
            p.setGameMode(GameMode.ADVENTURE);
            p.setGlowing(true);
            Location l = arena.pad(w, i, champs.size()).clone();
            l.setZ(w == 0 ? -5.5 : 6.5);
            p.teleport(l);
        }
        final Color c1 = g.dye.getFireworkColor();
        final int[] n = {0};
        repeat(() -> {
            if (n[0]++ >= 16) return;
            for (int k = 0; k < 3; k++) {
                double x = -9 + random.nextDouble() * 18;
                double z = (random.nextBoolean() ? -1 : 1) * (3 + random.nextDouble() * 10);
                launchFirework(new Location(world, x, Arena.FLOOR_Y + 1, z), c1);
            }
            for (Player p : champs) if (p.isOnline() && random.nextBoolean()) launchFirework(p.getLocation(), c1);
        }, 5L, 10L);
        celebrateReason = ChatColor.stripColor(g.name) + " won Dodgebolt " + score[w] + "-" + score[1 - w];
        later(() -> finishOnce(celebrateReason), FastTest.ticks(200L));
        updateSidebar();
    }

    private void launchFirework(Location l, Color c) {
        Firework fw = l.getWorld().spawn(l, Firework.class);
        FireworkMeta m = fw.getFireworkMeta();
        m.addEffect(FireworkEffect.builder().with(random.nextBoolean() ? FireworkEffect.Type.BALL_LARGE : FireworkEffect.Type.STAR)
                .withColor(c).withFade(Color.WHITE, Color.YELLOW).flicker(true).trail(true).build());
        m.setPower(1);
        fw.setFireworkMeta(m);
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
        // Return any arrows the victim was holding to their side.
        int held = 0;
        for (ItemStack s : victim.getInventory().getContents()) if (s != null && s.getType() == Material.ARROW) held += s.getAmount();
        victim.getInventory().clear();
        for (int i = 0; i < held; i++) spawnArrow(g);

        world.spawnParticle(Particle.FIREWORK, victim.getLocation().add(0, 1, 0), 40, 0.3, 0.6, 0.3, 0.05);
        world.playSound(victim.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1.5f, 1f);
        String vName = groups[g].color + victim.getName();
        if (shooter != null) {
            Integer sg = groupOf.get(shooter.getUniqueId());
            if (sg != null) matchElims[sg]++;
            kills.merge(shooter.getUniqueId(), 1, Integer::sum);
            if (ptsElim > 0) ctx.addPoints(shooter.getUniqueId(), ptsElim, "Dodgebolt elimination");
            String sName = (sg == null ? ChatColor.WHITE : groups[sg].color) + shooter.getName();
            ctx.broadcast(ChatColor.DARK_GRAY + "☠ " + vName + ChatColor.GRAY + " was bolted by " + sName);
            shooter.sendTitle("", ChatColor.GREEN + "Eliminated " + vName, 0, 25, 5);
            shooter.playSound(shooter.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1f, 1f);
        } else {
            matchElims[1 - g]++;
            ctx.broadcast(ChatColor.DARK_GRAY + "☠ " + vName + ChatColor.GRAY + (fell ? " fell into the void" : " is out"));
        }
        victim.sendTitle(ChatColor.RED + "" + ChatColor.BOLD + "ELIMINATED", ChatColor.GRAY + "Spectating until the next round", 0, 40, 10);
        toStands(victim, random.nextInt(10));

        // MCC-style: after the third elimination each further one shrinks the arena (up to 3 rings before overtime).
        if (state == State.LIVE && roundKills > 3 && ringsDropped < 3) arena.dropRing(ringsDropped++);
        queueCheck();
        updateSidebar();
    }

    /** Out players, non-finalists and late joiners watch in spectator mode from above the court (owner rule). */
    private void toStands(Player p, int i) {
        SpectatorGuard.makeSpectator(p, overview(i));
    }

    /** Spectator area: the court, its walls and the stands around it, with room above. */
    private static final org.bukkit.util.BoundingBox WATCH_BOX = new org.bukkit.util.BoundingBox(
            -Arena.WALL_X - 20, Arena.FLOOR_Y - 10, -Arena.WALL_Z - 18,
            Arena.WALL_X + 21, Arena.FLOOR_Y + 35, Arena.WALL_Z + 19);

    /** High above the east or west stands, looking down at the river in the middle. */
    private Location overview(int i) {
        boolean east = (i & 1) == 0;
        return new Location(world, east ? Arena.WALL_X + 9.5 : -Arena.WALL_X - 8.5, Arena.FLOOR_Y + 13, 0.5,
                east ? 90f : -90f, 38f);
    }

    private SpectatorGuard.Zone spectatorZone(Player p) {
        if (state == State.IDLE || world == null || !p.getWorld().equals(world)) return null;
        return new SpectatorGuard.Zone(world, WATCH_BOX, overview(p.getUniqueId().hashCode()));
    }

    // =================================================================== arrows

    private boolean isBolt(ItemStack s) {
        return s != null && s.getType() == Material.ARROW && s.hasItemMeta()
                && s.getItemMeta().getPersistentDataContainer().has(arrowKey, PersistentDataType.BYTE);
    }

    private void spawnArrow(int side) {
        if (arena == null || world == null) return;
        Location l = arena.arrowSpot(side);
        if (l == null) return;
        dropBolt(l, -1, true);
    }

    /** Drop a single bolt item. lastSide = side that last fired it (-1 unknown). */
    private Item dropBolt(Location l, int lastSide, boolean effect) {
        Item it = world.dropItem(l, arrowStack(1));
        it.setVelocity(new Vector(0, 0, 0));
        it.setGlowing(true);
        it.setUnlimitedLifetime(true);
        it.setPickupDelay(5);
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
        for (UUID u : arrowItems) { Entity e = Bukkit.getEntity(u); if (e != null && e.isValid()) n += ((Item) e).getItemStack().getAmount(); }
        for (UUID u : arrowEntities) { Entity e = Bukkit.getEntity(u); if (e != null && e.isValid()) n++; }
        for (UUID u : alive) {
            Player p = Bukkit.getPlayer(u);
            if (p == null) continue;
            for (ItemStack s : p.getInventory().getContents()) if (s != null && s.getType() == Material.ARROW) n += s.getAmount();
            ItemStack cursor = p.getItemOnCursor();
            if (cursor != null && cursor.getType() == Material.ARROW) n += cursor.getAmount();
        }
        return n;
    }

    private int fewerArrowsSide() {
        int[] c = new int[2];
        for (UUID u : arrowItems) {
            Entity e = Bukkit.getEntity(u);
            if (e != null && e.isValid()) c[Arena.sideOfZ(e.getLocation().getZ())]++;
        }
        return c[0] <= c[1] ? 0 : 1;
    }

    /** Every 5 ticks: re-home arrows that fell in the river or void, and tidy stray projectiles; also guard players. */
    private void arrowMaintenance() {
        if (ctx == null || world == null || state == State.IDLE) return;
        if (state == State.LIVE) {
            for (Iterator<UUID> it = arrowItems.iterator(); it.hasNext(); ) {
                Entity e = Bukkit.getEntity(it.next());
                if (e == null || !e.isValid()) { it.remove(); continue; }
                Location l = e.getLocation();
                boolean lost = l.getY() < Arena.FLOOR_Y - 3 || !Arena.inBox(l)
                        || (Arena.inRiver(l.getZ()) && l.getY() < Arena.FLOOR_Y + 0.6 && e.isOnGround());
                if (lost) {
                    Integer last = e.getPersistentDataContainer().get(arrowKey, PersistentDataType.INTEGER);
                    int amount = ((Item) e).getItemStack().getAmount();
                    int to = last != null ? 1 - last : fewerArrowsSide();
                    e.remove();
                    it.remove();
                    for (int i = 0; i < amount; i++) spawnArrow(to);
                }
            }
            List<UUID> arrows = new ArrayList<>(arrowEntities);
            for (UUID u : arrows) {
                Entity e = Bukkit.getEntity(u);
                if (e == null || !e.isValid()) { arrowEntities.remove(u); continue; }
                if (e.getTicksLived() > 200 || e.getLocation().getY() < Arena.FLOOR_Y - 6 || !Arena.inBox(e.getLocation())) {
                    Integer last = e.getPersistentDataContainer().get(arrowKey, PersistentDataType.INTEGER);
                    arrowEntities.remove(u);
                    e.remove();
                    spawnArrow(last != null ? 1 - last : fewerArrowsSide());
                }
            }
        }
        // Players who left the stands / arena via the void.
        for (Player p : new ArrayList<>(world.getPlayers())) {
            if (p.getLocation().getY() > Arena.FLOOR_Y - 5) continue;
            UUID u = p.getUniqueId();
            if (alive.contains(u) && state == State.LIVE) eliminate(p, null, true);
            else if (alive.contains(u)) {
                Integer g = groupOf.get(u);
                if (g != null) p.teleport(arena.pad(g, 0, 1));
            } else if (p.getGameMode() != GameMode.SPECTATOR) toStands(p, random.nextInt(10));
        }
    }

    // =================================================================== HUD

    private String scoreLine() {
        return groups[0].color + "" + ChatColor.BOLD + score[0] + ChatColor.GRAY + " - " + groups[1].color + ChatColor.BOLD + score[1];
    }

    private void updateSidebar() {
        if (ctx == null || groups[0] == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.WHITE + "Round " + Math.max(1, round) + ChatColor.GRAY + "  first to " + roundsToWin + (winByTwo ? " (+2)" : ""));
        lines.add(groups[0].color + short12(groups[0].name) + " " + ChatColor.WHITE + ChatColor.BOLD + score[0]
                + ChatColor.GRAY + " - " + ChatColor.WHITE + ChatColor.BOLD + score[1] + " " + groups[1].color + short12(groups[1].name));
        for (int g = 0; g < 2; g++) {
            int tot = onlineMembers(g).size();
            int al = (state == State.LIVE || state == State.COUNTDOWN || state == State.ROUND_END) ? aliveCount(g) : tot;
            lines.add(groups[g].color + short12(groups[g].name) + ChatColor.GRAY + ": " + ChatColor.WHITE + al + "/" + tot + " left");
        }
        if (state == State.LIVE) {
            lines.add(ChatColor.YELLOW + "Arrows in play: " + ChatColor.WHITE + countArrows());
            if (elapsed >= overtimeAt) lines.add(ChatColor.RED + "" + ChatColor.BOLD + "OVERTIME");
        } else if (state == State.CELEBRATE) {
            lines.add(ChatColor.GOLD + "" + ChatColor.BOLD + "CHAMPIONS DECIDED");
        }
        ctx.setSidebarLines(lines);
    }

    private static String short12(String s) { return s.length() > 12 ? s.substring(0, 12) : s; }

    private void actionBarAll(String msg) {
        for (Player p : world.getPlayers()) p.sendActionBar(msg);
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
        boolean wrong = g == 0 ? to.getZ() > Arena.A_EDGE : to.getZ() < Arena.B_EDGE;
        if (wrong) {
            Location back = from.clone();
            // If somehow already past the line, push them back onto their half.
            if (g == 0 ? back.getZ() > Arena.A_EDGE : back.getZ() < Arena.B_EDGE) back.setZ(g == 0 ? Arena.A_EDGE - 0.5 : Arena.B_EDGE + 0.5);
            back.setYaw(to.getYaw());
            back.setPitch(to.getPitch());
            e.setTo(back);
            p.sendActionBar(ChatColor.RED + "Stay on your side of the river!");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!active(e.getEntity().getWorld())) return;
        if (e.getEntity() instanceof Player || e.getEntity() instanceof Item) e.setCancelled(true);
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
        if (state == State.LIVE && isAlive(p) && it.getItemStack().getType() == Material.ARROW) {
            it.setItemStack(arrowStack(it.getItemStack().getAmount()));
            it.setGlowing(true);
            it.setUnlimitedLifetime(true);
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
        if (g == null || !(e.getProjectile() instanceof Arrow arrow)) { e.setCancelled(true); return; }
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrow.setGlowing(true);
        arrow.setColor(groups[g].dye.getColor());
        arrow.getPersistentDataContainer().set(arrowKey, PersistentDataType.INTEGER, g);
        arrowEntities.add(arrow.getUniqueId());
        world.playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 0.8f);
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
                    eliminate(victim, shooter, false);
                    dropBolt(drop, side, false);
                    return;
                }
            }
            e.setCancelled(true); // teammates / spectators: the arrow passes through
            return;
        }
        // Hit a block: turn it into a pick-up-able item right here.
        arrowEntities.remove(arrow.getUniqueId());
        Location l = arrow.getLocation();
        Vector v = arrow.getVelocity();
        if (v.lengthSquared() > 1.0E-4) l.subtract(v.clone().normalize().multiply(0.3));
        arrow.remove();
        if (state == State.LIVE) {
            Item it = dropBolt(l, side, false);
            it.setVelocity(new Vector(0, 0, 0));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (state == State.IDLE || !alive.contains(p.getUniqueId())) return;
        Integer g = groupOf.get(p.getUniqueId());
        alive.remove(p.getUniqueId());
        int held = 0;
        for (ItemStack s : p.getInventory().getContents()) if (s != null && s.getType() == Material.ARROW) held += s.getAmount();
        p.getInventory().clear();
        if (g != null && state == State.LIVE) for (int i = 0; i < held; i++) spawnArrow(g);
        if (ctx != null) ctx.broadcast(ChatColor.GRAY + p.getName() + " left the finale.");
        queueCheck();
    }

    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        if (state == State.IDLE || arena == null) return false;
        toStands(player, random.nextInt(10));
        player.sendMessage(ChatColor.GOLD + "The Dodgebolt finale is in progress - you are spectating.");
        return true;
    }
}
