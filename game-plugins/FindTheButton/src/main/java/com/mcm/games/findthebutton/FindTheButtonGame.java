package com.mcm.games.findthebutton;

import com.mcm.api.McmBuild;
import com.mcm.api.McmFx;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.findthebutton.map.FtbMap;
import com.mcm.games.findthebutton.map.Spot;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Find the Button: N rounds, each on a different map; one stone button hidden at a random hand-placed spot;
 * first finders score most, finders become spectators; hints after 45 s.
 */
final class FindTheButtonGame implements McmGame {
    enum Phase { IDLE, BUILDING, INTRO, COUNTDOWN, SEARCH, RESULTS, ENDING }

    private static final String PREFIX = ChatColor.GOLD + "" + ChatColor.BOLD + "Find the Button " + ChatColor.DARK_GRAY + "» " + ChatColor.RESET;

    private final FindTheButtonPlugin plugin;

    // settings (read at start)
    private int rounds, roundSeconds, countdownSeconds, resultsSeconds, hintStart, hintWide, hintInterval;
    private double hintRadius, hintRadiusWide;
    private int firstPoints, pointStep, minPoints, teamBonus;

    // game state
    private McmGameContext ctx;
    private Phase phase = Phase.IDLE;
    private World world;
    private final List<FtbMap> order = new ArrayList<>();
    private int round = -1;
    private FtbMap map;
    private int mapIndex;
    private Spot spot;
    private Block buttonBlock;
    private BlockData buttonPrevious;
    private int elapsed;
    private final List<UUID> finders = new ArrayList<>();
    private final Map<UUID, Integer> roundPoints = new HashMap<>();
    private final Map<UUID, Integer> gamePoints = new HashMap<>();
    private final Map<UUID, String> names = new HashMap<>();
    private final Set<String> bonusSides = new HashSet<>();

    // tasks
    private McmBuild.BuildQueue queue;
    private BukkitTask introTask, countdownTask, tickTask, resultsTask, endTask, celebrateTask;

    // admin /mcm map: a forced map that was not built for this game is built into its own slot
    private McmBuild.BuildQueue adminQueue;
    private boolean adminBuilding;
    private Runnable afterAdminBuild;

    FindTheButtonGame(FindTheButtonPlugin plugin) { this.plugin = plugin; }

    @Override public String id() { return "findthebutton"; }
    @Override public String displayName() { return "Find the Button"; }
    @Override public Material icon() { return Material.STONE_BUTTON; }
    @Override public String description() { return "Search themed rooms for one hidden stone button. First finders score most."; }

    boolean isRunning() { return ctx != null && phase != Phase.IDLE; }
    Phase phase() { return phase; }
    World world() { return world; }

    // ================================================================== start

    @Override
    public void start(McmGameContext ctx) {
        if (isRunning()) stop();
        this.ctx = ctx;
        readConfig();
        world = plugin.world();
        if (world == null) {
            ctx.broadcast(PREFIX + ChatColor.RED + "The game world could not be created.");
            McmGameContext c = ctx;
            reset();
            c.finish("Find the Button: world missing");
            return;
        }
        order.clear();
        List<FtbMap> pool = new ArrayList<>();
        for (FtbMap m : plugin.maps()) if (!plugin.validSpots(m).isEmpty()) pool.add(m);
        Collections.shuffle(pool);
        for (int i = 0; i < Math.min(rounds, pool.size()); i++) order.add(pool.get(i));
        if (order.isEmpty()) {
            ctx.broadcast(PREFIX + ChatColor.RED + "No valid maps.");
            McmGameContext c = ctx;
            reset();
            c.finish("Find the Button: no maps");
            return;
        }
        rounds = order.size();
        finders.clear(); roundPoints.clear(); gamePoints.clear(); names.clear(); bonusSides.clear();
        for (Player p : ctx.participants()) names.put(p.getUniqueId(), p.getName());

        phase = Phase.BUILDING;
        ctx.setTimer(roundSeconds, roundSeconds);
        ctx.setSidebarLines(List.of(ChatColor.GRAY + "Preparing the rooms..."));
        // (re)build every map used this game, air included: resets anything left over (e.g. a button)
        queue = new McmBuild.BuildQueue(plugin);
        for (FtbMap m : order) MapPlacer.enqueue(queue, world, plugin.indexOf(m), m, plugin.getLogger());
        queue.then(() -> { if (phase == Phase.BUILDING) whenAdminBuilt(() -> { if (phase == Phase.BUILDING && ctx != null) startRound(0); }); });
        queue.start();
    }

    private void readConfig() {
        FileConfiguration c = plugin.getConfig();
        rounds = Math.max(1, c.getInt("rounds", 5));
        roundSeconds = Math.max(20, c.getInt("round-seconds", 90));
        countdownSeconds = Math.max(1, c.getInt("countdown-seconds", 5));
        resultsSeconds = Math.max(1, c.getInt("results-seconds", 4));
        hintStart = c.getInt("hints.start-seconds", 45);
        hintWide = c.getInt("hints.wide-seconds", 70);
        hintInterval = Math.max(1, c.getInt("hints.interval-seconds", 3));
        hintRadius = c.getDouble("hints.radius", 12);
        hintRadiusWide = c.getDouble("hints.wide-radius", 24);
        // OURS (Find the Button is a Minecraft Monday game, not MCC): MCC-style coins modelled on TGTTOS. COINS.md.
        firstPoints = c.getInt("coins.first", 80);
        pointStep = c.getInt("coins.step", 4);
        minPoints = c.getInt("coins.minimum", 20);
        teamBonus = c.getInt("coins.team-bonus", 15);
    }

    // ================================================================== rounds

    private void startRound(int r) {
        if (ctx == null) return;
        round = r;
        map = order.get(r);
        mapIndex = plugin.indexOf(map);
        finders.clear();
        roundPoints.clear();
        bonusSides.clear();
        elapsed = 0;

        clearAllButtons();
        List<Spot> spots = plugin.validSpots(map);
        spot = spots.get(ThreadLocalRandom.current().nextInt(spots.size()));
        placeButton();

        List<Player> players = new ArrayList<>(ctx.participants());
        List<Location> pads = padLocations();
        Collections.shuffle(pads);
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            names.put(p.getUniqueId(), p.getName());
            prepare(p, GameMode.ADVENTURE);
            p.teleport(pads.get(i % pads.size()));
        }
        phase = Phase.INTRO;
        ctx.setTimer(roundSeconds, roundSeconds);
        updateSidebar();
        McmFx.titleCard(players, ChatColor.GOLD + "" + ChatColor.BOLD + map.displayName(),
                ChatColor.GRAY + "Round " + (round + 1) + "/" + rounds + ChatColor.DARK_GRAY + " · " + ChatColor.GRAY + map.tagline());
        ctx.broadcast(PREFIX + ChatColor.YELLOW + "Round " + (round + 1) + "/" + rounds + ": " + ChatColor.GOLD + map.displayName()
                + ChatColor.GRAY + " - one stone button is hidden somewhere. Right-click it!");
        introTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            introTask = null;
            if (phase != Phase.INTRO || ctx == null) return;
            phase = Phase.COUNTDOWN;
            countdownTask = McmFx.countdown(plugin, ctx.participants(), FastTest.secs(countdownSeconds),
                    ChatColor.YELLOW + "Find the button!", this::beginSearch);
        }, FastTest.ticks(50L));
    }

    private List<Location> padLocations() {
        List<Location> out = new ArrayList<>();
        for (int x = map.padX1(); x <= map.padX2(); x++)
            for (int z = map.padZ1(); z <= map.padZ2(); z++) {
                Location l = MapPlacer.at(world, mapIndex, x + 0.5, 1, z + 0.5);
                l.setYaw(map.padYaw());
                l.setPitch(0f);
                out.add(l);
            }
        return out;
    }

    /** Where a player who fell out of the map is put back. */
    Location startLocation() {
        if (map == null || world == null) return null;
        Location l = MapPlacer.at(world, mapIndex, (map.padX1() + map.padX2()) / 2 + 0.5, 1, (map.padZ1() + map.padZ2()) / 2 + 0.5);
        l.setYaw(map.padYaw());
        return l;
    }

    /** Spectator overview: centre of the room under the ceiling, looking down towards the start. */
    private Location overview() {
        if (map == null || world == null) return null;
        Location l = MapPlacer.at(world, mapIndex, 10.5, Math.max(2, map.height - 0.8), 15.5);
        l.setYaw(0f);      // facing south, over the room
        l.setPitch(40f);
        return l;
    }

    private void prepare(Player p, GameMode gm) {
        p.leaveVehicle();
        p.setGameMode(gm);
        p.getInventory().clear();
        p.setHealth(Math.max(1.0, p.getHealth()));
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.setVelocity(new Vector());
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        var max = p.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
        if (max != null) p.setHealth(max.getValue());
    }

    private void beginSearch() {
        countdownTask = null;
        if (ctx == null || phase != Phase.COUNTDOWN) return;
        phase = Phase.SEARCH;
        elapsed = 0;
        McmFx.actionBar(ctx.participants(), ChatColor.YELLOW + "" + ChatColor.BOLD + "Find the button!");
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
        checkRoundOver();   // e.g. everyone quit during the countdown
    }

    private void tick() {
        if (ctx == null || phase != Phase.SEARCH) return;
        elapsed++;
        int left = Math.max(0, roundSeconds - elapsed);
        ctx.setTimer(left, roundSeconds);
        if (elapsed == hintStart) ctx.broadcast(PREFIX + ChatColor.AQUA + "Hint: listen and look for sparkles near the button (" + (int) hintRadius + " blocks).");
        if (elapsed == hintWide) ctx.broadcast(PREFIX + ChatColor.AQUA + "Hint widened: the button now sparkles within " + (int) hintRadiusWide + " blocks.");
        if (elapsed >= hintStart && (elapsed - hintStart) % hintInterval == 0) hintPulse(elapsed >= hintWide ? hintRadiusWide : hintRadius);
        for (Player p : ctx.participants()) {
            Integer pts = roundPoints.get(p.getUniqueId());
            if (pts != null) {
                int place = finders.indexOf(p.getUniqueId()) + 1;
                McmFx.actionBar(p, ChatColor.GREEN + "You found it! " + ChatColor.GRAY + "#" + place);
            } else {
                String hint = elapsed >= hintWide ? ChatColor.AQUA + " (wide hints)" : elapsed >= hintStart ? ChatColor.AQUA + " (hints on)" : "";
                McmFx.actionBar(p, ChatColor.YELLOW + "" + ChatColor.BOLD + "Find the button!" + hint);
            }
        }
        updateSidebar();
        if (left <= 0) endRound(false);
    }

    /** Particle pulse + soft chime at the button, only for players within the radius. */
    private void hintPulse(double radius) {
        Location c = buttonCentre();
        if (c == null) return;
        double r2 = radius * radius;
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distanceSquared(c) > r2) continue;
            p.spawnParticle(Particle.END_ROD, c, 10, 0.25, 0.25, 0.25, 0.02);
            p.spawnParticle(Particle.WAX_ON, c, 8, 0.3, 0.3, 0.3, 0.5);
            p.playSound(c, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 0.7f, 1.4f);
        }
    }

    // ================================================================== button

    private void placeButton() {
        int ox = MapPlacer.originX(mapIndex), oz = MapPlacer.originZ(mapIndex);
        buttonBlock = world.getBlockAt(ox + spot.x(), MapPlacer.BASE_Y + spot.y(), oz + spot.z());
        buttonPrevious = buttonBlock.getBlockData();
        buttonBlock.setBlockData(MapPlacer.data(spot.blockState(), plugin.getLogger()), false);
        if (buttonBlock.getType() != Material.STONE_BUTTON)
            plugin.getLogger().warning("Find the Button: button did not appear at " + buttonBlock.getLocation());
    }

    /** Removes the hidden button (restores what was in that cell). Safe to call repeatedly. */
    private void removeButton() {
        if (buttonBlock != null) {
            if (buttonBlock.getType() == Material.STONE_BUTTON)
                buttonBlock.setBlockData(buttonPrevious != null ? buttonPrevious : Material.AIR.createBlockData(), false);
            buttonBlock = null;
            buttonPrevious = null;
        }
    }

    /** Make sure no button is left at any spot of the current map (the build has none of its own). */
    private void clearAllButtons() {
        removeButton();
        int ox = MapPlacer.originX(mapIndex), oz = MapPlacer.originZ(mapIndex);
        for (Spot s : map.spots()) {
            Block b = world.getBlockAt(ox + s.x(), MapPlacer.BASE_Y + s.y(), oz + s.z());
            if (b.getType() == Material.STONE_BUTTON)
                b.setBlockData(MapPlacer.data(map.grid().get(s.x(), s.y(), s.z()), plugin.getLogger()), false);
        }
    }

    boolean isButton(Block b) {
        return buttonBlock != null && b != null && b.getWorld().equals(buttonBlock.getWorld())
                && b.getX() == buttonBlock.getX() && b.getY() == buttonBlock.getY() && b.getZ() == buttonBlock.getZ();
    }

    private Location buttonCentre() {
        if (spot == null || world == null) return null;
        double[] c = spot.centre();
        return MapPlacer.at(world, mapIndex, c[0], c[1], c[2]);
    }

    // ================================================================== presses

    /** Called by the listener for a right-click on the current button. Returns true if it scored. */
    boolean onPress(Player p) {
        if (ctx == null || phase != Phase.SEARCH) return false;
        UUID id = p.getUniqueId();
        if (!isParticipant(p) || p.getGameMode() == GameMode.SPECTATOR || finders.contains(id)) return false;
        finders.add(id);
        int place = finders.size();
        int pts = Math.max(minPoints, firstPoints - pointStep * (place - 1));
        roundPoints.put(id, pts);
        gamePoints.merge(id, pts, Integer::sum);
        ctx.addCoins(id, pts, "You were #" + place + " to find the button");

        String side = McmFx.sideKeyOf(ctx, p);
        ChatColor col = McmFx.sideColor(ctx, side);
        McmFx.title(p, ChatColor.GREEN + "" + ChatColor.BOLD + "FOUND IT!", ChatColor.GRAY + "#" + place, 0, 40, 10);
        McmFx.sound(p, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.3f);
        McmFx.sound(p, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.6f);
        Location c = buttonCentre();
        if (c != null) McmFx.teamFirework(c.clone().add(0, 0.8, 0), col, place == 1);
        McmFx.teamFirework(p.getLocation().add(0, 2.2, 0), col, false);
        List<Player> others = new ArrayList<>(ctx.participants());
        others.remove(p);
        McmFx.sound(others, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6f, 0.8f);
        ctx.broadcast(PREFIX + col + p.getName() + ChatColor.GREEN + " found the button! " + ChatColor.GRAY + "(#" + place + ")");

        // become a spectator next tick (the vanilla press is still being processed in this event)
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (ctx == null || !p.isOnline() || !finders.contains(id)) return;
            p.setGameMode(GameMode.SPECTATOR);
            Location o = overview();
            if (o != null) p.teleport(o);
            McmFx.actionBar(p, ChatColor.GREEN + "You found it! " + ChatColor.GRAY + "Spectating until the round ends.");
        });
        checkTeamBonus(side);
        updateSidebar();
        Bukkit.getScheduler().runTask(plugin, this::checkRoundOver);
        return true;
    }

    private boolean isParticipant(Player p) {
        if (ctx == null) return false;
        for (Player q : ctx.participants()) if (q.getUniqueId().equals(p.getUniqueId())) return true;
        return false;
    }

    /** +bonus to each member of a side (2+ online members) once every online member found the button. */
    private void checkTeamBonus(String side) {
        if (ctx == null || side == null || bonusSides.contains(side) || teamBonus <= 0) return;
        List<Player> members = ctx.sides().get(side);
        if (members == null || members.size() < 2) return;
        for (Player m : members) if (!finders.contains(m.getUniqueId())) return;
        bonusSides.add(side);
        for (Player m : members) {
            ctx.addCoins(m.getUniqueId(), teamBonus, "Your whole team found the button!");
            gamePoints.merge(m.getUniqueId(), teamBonus, Integer::sum);
            roundPoints.merge(m.getUniqueId(), teamBonus, Integer::sum);
            McmFx.sound(m, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        }
        ctx.broadcast(PREFIX + ctx.sideLabel(side) + ChatColor.GREEN + " all found it!");
    }

    /** Ends the round early once every (still online) participant has found the button. */
    private void checkRoundOver() {
        if (ctx == null || phase != Phase.SEARCH) return;
        for (Player p : ctx.participants()) if (!finders.contains(p.getUniqueId())) return;
        endRound(true);
    }

    /** Late joiners and rejoiners (the core no longer counts them) spectate the current room from the overview. */
    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        if (ctx == null || world == null) return false;
        Location o = overview();
        if (o == null) return false;   // still building: the core's default spectator mode is fine
        player.closeInventory();
        player.getInventory().clear();
        player.setFireTicks(0);
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(o);
        player.sendMessage(PREFIX + ChatColor.GRAY + "A round is in progress - you are spectating.");
        return true;
    }

    void onQuit(Player p) {
        if (ctx == null) return;
        // participants() drops the player; re-check next tick
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (ctx == null || phase != Phase.SEARCH) return;
            for (String side : ctx.sides().keySet()) checkTeamBonus(side);
            updateSidebar();
            checkRoundOver();
        });
    }

    // ================================================================== round end / game end

    private void endRound(boolean everyoneFound) {
        if (ctx == null || phase != Phase.SEARCH) return;
        phase = Phase.RESULTS;
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        Location c = buttonCentre();
        String where = spot == null ? "?" : spot.name();
        ctx.setTimer(0, roundSeconds);
        ctx.broadcast(PREFIX + (everyoneFound ? ChatColor.GREEN + "Everyone found it!" : ChatColor.RED + "Time's up!")
                + ChatColor.GRAY + " The button was " + ChatColor.WHITE + where + ChatColor.GRAY + ".");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, finders.size()); i++) {
            if (i > 0) sb.append(ChatColor.DARK_GRAY).append(", ");
            sb.append(ChatColor.YELLOW).append("#").append(i + 1).append(" ").append(ChatColor.WHITE).append(names.getOrDefault(finders.get(i), "?"));
        }
        if (finders.isEmpty()) ctx.broadcast(PREFIX + ChatColor.GRAY + "Nobody found it this round.");
        else ctx.broadcast(PREFIX + ChatColor.GRAY + finders.size() + " found it: " + sb);
        for (Player p : ctx.participants()) {
            Integer pts = roundPoints.get(p.getUniqueId());
            String sub = pts != null ? ChatColor.GREEN + "You found it #" + (finders.indexOf(p.getUniqueId()) + 1) + ChatColor.GRAY + " (+" + pts + ")"
                    : ChatColor.GRAY + "It was " + where;
            McmFx.title(p, everyoneFound ? ChatColor.GREEN + "" + ChatColor.BOLD + "All found!" : ChatColor.GOLD + "" + ChatColor.BOLD + "Round over",
                    sub, 5, 60, 10);
        }
        McmFx.sound(ctx.participants(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1f);
        if (c != null) McmFx.burst(c, Color.YELLOW, 20);
        // remove the button one tick later (a press in this tick would otherwise re-place it)
        Bukkit.getScheduler().runTask(plugin, () -> { if (phase == Phase.RESULTS) removeButton(); });
        updateSidebar();
        resultsTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            resultsTask = null;
            nextRoundOrEnd();
        }, FastTest.secs(resultsSeconds) * 20L);
    }

    /** After the results screen (or an admin skip): the next round, or the end of the game. */
    private void nextRoundOrEnd() {
        if (ctx == null || phase != Phase.RESULTS) return;
        whenAdminBuilt(() -> {
            if (ctx == null || phase != Phase.RESULTS) return;
            removeButton();
            if (round + 1 < rounds) startRound(round + 1);
            else endGame();
        });
    }

    private void whenAdminBuilt(Runnable r) {
        if (adminBuilding) afterAdminBuild = r;
        else r.run();
    }

    private void endGame() {
        phase = Phase.ENDING;
        removeButton();
        List<Map.Entry<UUID, Integer>> top = new ArrayList<>(gamePoints.entrySet());
        top.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        ctx.broadcast(PREFIX + ChatColor.GOLD + "Game over! " + ChatColor.GRAY + "Top finders:");
        List<Player> winners = new ArrayList<>();
        for (int i = 0; i < Math.min(3, top.size()); i++) {
            UUID id = top.get(i).getKey();
            ctx.broadcast(ChatColor.YELLOW + "  #" + (i + 1) + " " + ChatColor.WHITE + names.getOrDefault(id, "?") + ChatColor.GRAY + " - " + top.get(i).getValue() + " pts");
            Player p = Bukkit.getPlayer(id);
            if (i == 0 && p != null && p.isOnline()) winners.add(p);
        }
        McmFx.title(ctx.participants(), ChatColor.GOLD + "" + ChatColor.BOLD + "Game over!", ChatColor.GRAY + "Thanks for searching", 5, 50, 10);
        if (!winners.isEmpty()) celebrateTask = McmFx.celebrate(plugin, winners);
        endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endTask = null;
            finishNow();
        }, FastTest.ticks(60L));
    }

    private void finishNow() {
        if (ctx == null || phase != Phase.ENDING) return;
        McmGameContext c = ctx;
        cancelTasks();
        removeButton();
        reset();
        c.finish("Find the Button finished");   // synchronous: players are in the lobby when this returns
    }

    // ================================================================== admin testing hooks
    // (/mcm skipwait | skip | map; no @Override so the jar still loads on an older core)

    /** Ends the title card + countdown (straight to searching), the results screen, or the final screen. */
    public boolean skipWait() {
        if (ctx == null) return false;
        switch (phase) {
            case INTRO, COUNTDOWN -> {
                cancelTask(introTask); introTask = null;
                cancelTask(countdownTask); countdownTask = null;
                phase = Phase.COUNTDOWN;
                beginSearch();
                return true;
            }
            case RESULTS -> {
                if (adminBuilding) return false;   // still building a forced map
                cancelTask(resultsTask); resultsTask = null;
                nextRoundOrEnd();
                return true;
            }
            case ENDING -> {
                cancelTask(endTask); endTask = null;
                finishNow();
                return true;
            }
            default -> { return false; }
        }
    }

    /** Ends the current round now (nobody else can score in it) and moves to the next map, or ends the game. */
    public boolean skipRound() {
        if (ctx == null) return false;
        if (phase != Phase.INTRO && phase != Phase.COUNTDOWN && phase != Phase.SEARCH && phase != Phase.RESULTS) return false;
        if (adminBuilding && phase == Phase.RESULTS) return false;
        cancelTask(introTask); introTask = null;
        cancelTask(countdownTask); countdownTask = null;
        cancelTask(tickTask); tickTask = null;
        cancelTask(resultsTask); resultsTask = null;
        if (phase != Phase.RESULTS) {
            ctx.broadcast(PREFIX + ChatColor.GRAY + "Round " + (round + 1) + " was skipped by an admin.");
            phase = Phase.RESULTS;
        }
        removeButton();
        nextRoundOrEnd();
        return true;
    }

    public List<String> maps() {
        List<String> ids = new ArrayList<>();
        for (FtbMap m : plugin.maps()) if (!plugin.validSpots(m).isEmpty()) ids.add(m.id());
        return ids;
    }

    /** The next round uses map {@code id} (built now if this game had not built it yet). */
    public boolean forceNextMap(String id) {
        if (ctx == null || id == null || phase == Phase.IDLE || phase == Phase.ENDING) return false;
        FtbMap want = null;
        for (FtbMap m : plugin.maps()) if (m.id().equalsIgnoreCase(id) && !plugin.validSpots(m).isEmpty()) want = m;
        if (want == null) return false;
        int next = phase == Phase.BUILDING ? 0 : round + 1;
        if (next >= order.size()) return false;   // no round left
        int at = order.indexOf(want);
        if (at >= next) { Collections.swap(order, at, next); return true; }   // already built for a later round
        boolean built = at >= 0;                                              // played earlier this game: still built
        order.set(next, want);
        if (!built) {
            if (adminQueue != null) adminQueue.cancel();
            adminBuilding = true;
            adminQueue = new McmBuild.BuildQueue(plugin);
            MapPlacer.enqueue(adminQueue, world, plugin.indexOf(want), want, plugin.getLogger());
            adminQueue.then(() -> {
                adminBuilding = false;
                adminQueue = null;
                Runnable r = afterAdminBuild;
                afterAdminBuild = null;
                if (r != null && ctx != null) r.run();
            });
            adminQueue.start();
        }
        return true;
    }

    private static void cancelTask(BukkitTask t) { if (t != null) t.cancel(); }

    // ================================================================== stop / cleanup

    @Override
    public void stop() {
        boolean wasRunning = ctx != null;
        cancelTasks();
        removeButton();
        if (wasRunning && world != null) {
            for (Player p : world.getPlayers()) {
                if (p.getGameMode() == GameMode.SPECTATOR) p.setGameMode(GameMode.ADVENTURE);
                p.setFallDistance(0f);
            }
        }
        reset();
    }

    private void cancelTasks() {
        if (queue != null) { queue.cancel(); queue = null; }
        if (adminQueue != null) { adminQueue.cancel(); adminQueue = null; }
        adminBuilding = false;
        afterAdminBuild = null;
        for (BukkitTask t : new BukkitTask[]{introTask, countdownTask, tickTask, resultsTask, endTask, celebrateTask})
            if (t != null) t.cancel();
        introTask = countdownTask = tickTask = resultsTask = endTask = celebrateTask = null;
    }

    private void reset() {
        phase = Phase.IDLE;
        ctx = null;
        round = -1;
        map = null;
        spot = null;
        finders.clear();
        roundPoints.clear();
        bonusSides.clear();
    }

    // ================================================================== listener helpers

    boolean frozen() { return phase == Phase.INTRO || phase == Phase.COUNTDOWN; }

    boolean participant(Player p) { return isParticipant(p); }

    /** Interior box of the current map in world coordinates: {minX, minY, minZ, maxX, maxY, maxZ} (block bounds). */
    double[] bounds(double margin) {
        if (map == null) return null;
        double ox = MapPlacer.originX(mapIndex), oz = MapPlacer.originZ(mapIndex), oy = MapPlacer.BASE_Y;
        return new double[]{ox + 1 - margin, oy + 1 - margin, oz + 1 - margin, ox + 21 + margin, oy + map.height + 1 + margin, oz + 21 + margin};
    }

    // ================================================================== HUD

    private void updateSidebar() {
        if (ctx == null || map == null) return;
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GRAY + "Round " + ChatColor.WHITE + (round + 1) + ChatColor.GRAY + "/" + ChatColor.WHITE + rounds);
        lines.add(ChatColor.GRAY + "Map: " + ChatColor.GOLD + map.displayName());
        int total = ctx.participants().size();
        int found = 0;
        for (Player p : ctx.participants()) if (finders.contains(p.getUniqueId())) found++;
        lines.add(ChatColor.GRAY + "Found: " + ChatColor.GREEN + found + ChatColor.GRAY + "/" + ChatColor.WHITE + total);
        ChatColor[] cols = {ChatColor.YELLOW, ChatColor.WHITE, ChatColor.GOLD};
        for (int i = 0; i < Math.min(3, finders.size()); i++)
            lines.add(cols[i] + "#" + (i + 1) + " " + ChatColor.WHITE + names.getOrDefault(finders.get(i), "?"));
        ctx.setSidebarLines(lines);
    }

    /** For tests/diagnostics. */
    Map<String, Object> debugState() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("phase", phase);
        m.put("round", round);
        m.put("map", map == null ? null : map.id());
        m.put("spot", spot == null ? null : spot.name());
        m.put("finders", finders.size());
        return m;
    }
}
