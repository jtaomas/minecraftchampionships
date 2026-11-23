package com.mcm.games.gridrunners;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.gridrunners.rooms.Room;
import com.mcm.games.gridrunners.rooms.RoomType;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class GridRunnersGame implements McmGame {
    public static final String ID = "gridrunners";
    public static final String WORLD = "mcc_" + ID;
    public static final int Y0 = 64;
    public static final int LANE_SPACING = 80;
    public static final int MAX_LANES = 12;
    public static final int GAME_SECONDS = 600;
    public static final int HINT_AT = 60;
    public static final int SKIP_AT = 180;
    private static final int[] FINISH_POINTS = {40, 32, 26, 21, 17, 14, 12, 10, 8, 6};

    private enum Phase { IDLE, BUILDING, COUNTDOWN, RUNNING, ENDING }

    private final GridRunnersPlugin plugin;
    private final NamespacedKey roomKey;
    private final NamespacedKey protKey;
    private final NamespacedKey itemKey;

    private McmGameContext ctx;
    private World world;
    private Phase phase = Phase.IDLE;
    private final List<Lane> lanes = new ArrayList<>();
    private final Map<UUID, Lane> laneOf = new HashMap<>();
    private final Map<String, Room> roomById = new HashMap<>();
    private final Map<UUID, Room> lastRoom = new HashMap<>();
    private BukkitTask buildTask, mainTask, endTask, clearTask;
    private long steps;
    private int countdown;
    private int timeLeft;
    private final int[] roomClears = new int[9];
    private int finishedCount;

    public GridRunnersGame(GridRunnersPlugin plugin) {
        this.plugin = plugin;
        this.roomKey = new NamespacedKey(plugin, "room");
        this.protKey = new NamespacedKey(plugin, "protected");
        this.itemKey = new NamespacedKey(plugin, "item");
    }

    // ------------------------------------------------------------------ McmGame

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Grid Runners"; }
    @Override public Material icon() { return Material.TINTED_GLASS; }
    @Override public String description() { return "Clear a grid of co-op challenge rooms and sprint to the finish."; }
    @Override public boolean useCoreScoreboard() { return true; }

    @Override
    public void start(McmGameContext context) {
        stop();
        if (clearTask != null) { clearTask.cancel(); clearTask = null; }
        this.ctx = context;
        this.world = Arena.ensureWorld();
        if (world == null) {
            plugin.getLogger().severe("Could not create world " + WORLD);
            McmGameContext c = ctx;
            ctx = null;
            c.finish("Grid Runners world failed to load");
            return;
        }
        removeWorldEntities();
        lanes.clear();
        laneOf.clear();
        roomById.clear();
        lastRoom.clear();
        Arrays.fill(roomClears, 0);
        finishedCount = 0;
        steps = 0;
        timeLeft = GAME_SECONDS;

        buildLanes();
        long seed = new Random().nextLong();
        List<RoomType> pool = new ArrayList<>(List.of(RoomType.MOB_PIT, RoomType.PLATES, RoomType.PARKOUR,
                RoomType.SEARCH, RoomType.LAMPS, RoomType.SHEEP, RoomType.HOPPER, RoomType.MAZE,
                RoomType.LEVERS, RoomType.DEFEND));
        Collections.shuffle(pool, new Random(seed));
        List<RoomType> layout = new ArrayList<>(pool.subList(0, 8));
        layout.add(RoomType.FINISH);
        for (Lane lane : lanes) {
            for (int i = 0; i < 9; i++) {
                Room r = layout.get(i).create(this, lane, i % 3, i / 3, seed);
                lane.rooms[i] = r;
                roomById.put(r.id, r);
            }
            lane.rooms[0].state = Room.State.OPEN;
        }
        addChunkTickets();

        phase = Phase.BUILDING;
        ctx.setTimer(GAME_SECONDS, GAME_SECONDS);
        ctx.setSidebarLines(List.of("§7Building the grid..."));
        buildTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (phase != Phase.BUILDING) { cancel(); return; }
                if (t < MAX_LANES) {
                    Arena.clearLane(world, t);
                    if (t < lanes.size()) Arena.buildLane(world, lanes.get(t));
                    t++;
                } else {
                    cancel();
                    buildTask = null;
                    beginCountdown();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public void stop() {
        if (buildTask != null) { buildTask.cancel(); buildTask = null; }
        if (mainTask != null) { mainTask.cancel(); mainTask = null; }
        if (endTask != null) { endTask.cancel(); endTask = null; }
        boolean wasActive = phase != Phase.IDLE;
        if (ctx != null) cleanupPlayers();
        cleanupWorld();
        phase = Phase.IDLE;
        ctx = null;
        if (wasActive) scheduleArenaClear();
    }

    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        if (world == null || lanes.isEmpty() || phase == Phase.IDLE) return false;
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(lanes.get(0).overview(world));
        Msg.send(player, "§7Grid Runners is in progress - you are spectating.");
        return true;
    }

    // ------------------------------------------------------------------ setup

    private static DyeColor teamDye(String team) {
        if (team == null) return DyeColor.WHITE;
        return switch (team.toLowerCase(Locale.ROOT)) {
            case "red" -> DyeColor.RED;
            case "orange" -> DyeColor.ORANGE;
            case "yellow" -> DyeColor.YELLOW;
            case "lime" -> DyeColor.LIME;
            case "green" -> DyeColor.GREEN;
            case "cyan" -> DyeColor.CYAN;
            case "aqua" -> DyeColor.LIGHT_BLUE;
            case "blue" -> DyeColor.BLUE;
            case "purple" -> DyeColor.PURPLE;
            case "pink" -> DyeColor.PINK;
            default -> DyeColor.WHITE;
        };
    }

    private static final DyeColor[] SOLO_DYES = {DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.MAGENTA,
            DyeColor.BROWN, DyeColor.GRAY, DyeColor.BLACK, DyeColor.ORANGE, DyeColor.LIME, DyeColor.CYAN,
            DyeColor.PINK, DyeColor.YELLOW, DyeColor.BLUE};

    private void buildLanes() {
        Map<String, List<Player>> sides = ctx.sides();
        List<String> teamKeys = new ArrayList<>();
        List<String> soloKeys = new ArrayList<>();
        for (String k : sides.keySet()) {
            if (sides.get(k) == null || sides.get(k).isEmpty()) continue;
            if (k.startsWith("TEAM:")) teamKeys.add(k); else soloKeys.add(k);
        }
        Collections.sort(teamKeys);
        int soloIdx = 0;
        for (String k : teamKeys) {
            if (lanes.size() >= MAX_LANES) { soloKeys.add(k); continue; }
            Lane lane = new Lane(lanes.size(), teamDye(k.substring(5)), ctx.sideLabel(k));
            addSide(lane, k, sides.get(k));
            lanes.add(lane);
        }
        for (String k : soloKeys) {
            if (lanes.size() < MAX_LANES) {
                Lane lane = new Lane(lanes.size(), SOLO_DYES[soloIdx++ % SOLO_DYES.length], ctx.sideLabel(k));
                addSide(lane, k, sides.get(k));
                lanes.add(lane);
            } else {
                Lane smallest = lanes.stream().min(Comparator.comparingInt(l -> l.members.size())).orElse(lanes.get(0));
                addSide(smallest, k, sides.get(k));
            }
        }
        if (lanes.isEmpty()) {
            Lane lane = new Lane(0, DyeColor.WHITE, "§fRunners");
            for (Player p : ctx.participants()) { lane.members.add(p.getUniqueId()); laneOf.put(p.getUniqueId(), lane); }
            lanes.add(lane);
        }
    }

    private void addSide(Lane lane, String key, List<Player> players) {
        lane.sideKeys.add(key);
        for (Player p : players) {
            if (laneOf.containsKey(p.getUniqueId())) continue;
            lane.members.add(p.getUniqueId());
            laneOf.put(p.getUniqueId(), lane);
        }
    }

    private void addChunkTickets() {
        for (Lane lane : lanes) {
            for (int cx = Math.floorDiv(lane.x0 - 11, 16); cx <= Math.floorDiv(lane.x0 + 47, 16); cx++)
                for (int cz = Math.floorDiv(-2, 16); cz <= Math.floorDiv(47, 16); cz++)
                    world.addPluginChunkTicket(cx, cz, plugin);
        }
    }

    private ItemStack kitItem(Material m, String name) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(Msg.c(name));
            meta.setUnbreakable(true);
            it.setItemMeta(meta);
        }
        return it;
    }

    private void preparePlayer(Player p, Lane lane) {
        p.teleport(lane.spawn(world));
        p.setGameMode(GameMode.ADVENTURE);
        p.getInventory().clear();
        for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
        p.setHealth(p.getMaxHealth());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setGlowing(false);
        p.getInventory().setItem(0, kitItem(Material.STONE_SWORD, "§fRunner's Sword"));
        p.getInventory().setItem(1, kitItem(Material.SHEARS, "§fRunner's Shears"));
    }

    private void beginCountdown() {
        if (ctx == null) return;
        phase = Phase.COUNTDOWN;
        countdown = FastTest.secs(5);
        for (Player p : new ArrayList<>(ctx.participants())) {
            Lane lane = laneOf.get(p.getUniqueId());
            if (lane == null) {
                lane = lanes.get(0);
                lane.members.add(p.getUniqueId());
                laneOf.put(p.getUniqueId(), lane);
            }
            preparePlayer(p, lane);
            Msg.send(p, "§6§lGrid Runners §7- clear the 3x3 grid of challenge rooms with your team.");
            Msg.send(p, "§7Each cleared room opens the doors next to it. Clear all 8 to unlock the §6gold §7final room, then sprint to the finish.");
            Msg.send(p, "§7Hints appear after §e60s§7 in a room; a room auto-skips after §e3 min§7 for no points.");
            Msg.title(p, "§6Grid Runners", "§eWork together - clear every room!", 5, 50, 10);
        }
        updateSidebar();
        mainTask = Bukkit.getScheduler().runTaskTimer(plugin, this::step, 5L, 5L);
    }

    // ------------------------------------------------------------------ loop

    private void step() {
        if (ctx == null) return;
        steps++;
        if (ctx.participants().isEmpty()) { endGame("Everyone left"); return; }
        if (phase == Phase.COUNTDOWN) {
            if (steps % 4 == 0) {
                if (countdown > 0) {
                    for (Player p : new ArrayList<>(ctx.participants())) {
                        Msg.title(p, "§e" + countdown, "§7Get ready...", 0, 22, 0);
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                    countdown--;
                } else {
                    startRunning();
                }
            }
            return;
        }
        if (phase != Phase.RUNNING) return;
        fast();
        if (phase == Phase.RUNNING && steps % 4 == 0) second();
    }

    private void startRunning() {
        phase = Phase.RUNNING;
        timeLeft = GAME_SECONDS;
        for (Lane lane : lanes) Arena.openStartDoor(world, lane);
        for (Player p : new ArrayList<>(ctx.participants())) {
            Msg.title(p, "§a§lGO!", "§eRoom 1 is open", 0, 25, 10);
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }
        ctx.setTimer(timeLeft, GAME_SECONDS);
    }

    private void fast() {
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (phase != Phase.RUNNING) return;
            if (p.getWorld() != world || p.getGameMode() == GameMode.SPECTATOR) continue;
            Lane lane = laneOf.get(p.getUniqueId());
            if (lane == null || lane.finished) continue;
            if (p.getLocation().getY() < Y0 - 6) {
                p.teleport(lane.spawn(world));
                p.setFallDistance(0);
                continue;
            }
            Room r = lane.roomAt(p.getLocation());
            if (r == null) continue;
            if (r.state == Room.State.OPEN) activate(r);
            if (lastRoom.get(p.getUniqueId()) != r) {
                lastRoom.put(p.getUniqueId(), r);
                if (r.state == Room.State.ACTIVE) {
                    Msg.title(p, "§e" + r.title(), "§f" + r.objective(), 3, 40, 10);
                }
            }
            if (r.state == Room.State.ACTIVE) r.onPlayerFast(p);
        }
        for (Lane lane : lanes) {
            if (phase != Phase.RUNNING) return;
            if (lane.finished || online(lane).isEmpty()) continue;
            for (Room r : lane.rooms) if (r.state == Room.State.ACTIVE) r.fastTick();
        }
    }

    private void activate(Room r) {
        r.state = Room.State.ACTIVE;
        r.activeSeconds = 0;
        r.hinted = false;
        r.onActivate();
    }

    private void second() {
        timeLeft--;
        ctx.setTimer(Math.max(0, timeLeft), GAME_SECONDS);
        if (timeLeft == 60 || timeLeft == 30 || timeLeft == 10) {
            ctx.broadcast("§e" + timeLeft + " seconds left in Grid Runners!");
        }
        for (Lane lane : lanes) {
            if (phase != Phase.RUNNING) return;
            List<Player> on = online(lane);
            if (on.isEmpty() || lane.finished) continue;
            for (Room r : lane.rooms) {
                if (r.state != Room.State.ACTIVE) continue;
                r.activeSeconds++;
                r.secondTick();
                if (r.state != Room.State.ACTIVE || r.type == RoomType.FINISH) continue;
                if (r.activeSeconds == HINT_AT) {
                    r.hinted = true;
                    for (Player p : on) {
                        Msg.send(p, "§6[Hint] §e" + r.title() + "§7: §f" + r.hintText());
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1f, 1.2f);
                    }
                    r.onHint();
                }
                if (r.hinted) r.hintTick();
                if (r.activeSeconds >= SKIP_AT && r.state == Room.State.ACTIVE) roomCompleted(r, true);
            }
            for (Player p : on) actionBar(p, lane);
        }
        if (phase != Phase.RUNNING) return;
        updateSidebar();
        if (allLanesDone()) endGame("All courses complete");
        else if (timeLeft <= 0) endGame("Time's up");
    }

    private void actionBar(Player p, Lane lane) {
        if (p.getGameMode() == GameMode.SPECTATOR) return;
        Room r = lane.roomAt(p.getLocation());
        if (r != null && r.state == Room.State.ACTIVE) {
            String extra = "";
            if (r.type != RoomType.FINISH) {
                if (r.activeSeconds >= SKIP_AT - 60) extra = " §7| §cskip in " + (SKIP_AT - r.activeSeconds) + "s";
                else if (!r.hinted) extra = " §7| hint in " + (HINT_AT - r.activeSeconds) + "s";
            }
            String prog = r.progress();
            Msg.bar(p, "§e" + r.name() + (prog.isEmpty() ? "" : " §7| §f" + prog) + extra);
        } else if (lane.finalOpen) {
            Msg.bar(p, "§6Final room is open - sprint to the gold finish!");
        } else {
            Msg.bar(p, "§7Rooms done: §f" + lane.done() + "/8");
        }
    }

    private boolean allLanesDone() {
        for (Lane lane : lanes) if (!lane.finished && !online(lane).isEmpty()) return false;
        return true;
    }

    private void updateSidebar() {
        if (ctx == null) return;
        List<Lane> sorted = new ArrayList<>(lanes);
        sorted.sort((a, b) -> {
            if (a.finished != b.finished) return a.finished ? -1 : 1;
            if (a.finished) return Integer.compare(a.place, b.place);
            return Integer.compare(b.done(), a.done());
        });
        List<String> lines = new ArrayList<>();
        lines.add("§7Rooms cleared §8(of 8)");
        int max = sorted.size() > 5 ? 4 : 5;
        for (int i = 0; i < sorted.size() && i < max; i++) {
            Lane l = sorted.get(i);
            String state = l.finished ? "§a✔ " + Msg.ordinal(l.place) : "§f" + l.cleared + (l.skipped > 0 ? "§7+" + l.skipped : "") + "§7/8";
            lines.add(l.label + "§7: " + state);
        }
        if (sorted.size() > max) lines.add("§7...and " + (sorted.size() - max) + " more");
        ctx.setSidebarLines(lines);
    }

    // ------------------------------------------------------------------ progression

    public void roomCompleted(Room room, boolean skipped) {
        if (phase != Phase.RUNNING || ctx == null) return;
        if (room.state != Room.State.ACTIVE && room.state != Room.State.OPEN) return;
        room.state = skipped ? Room.State.SKIPPED : Room.State.DONE;
        room.cleanup();
        Lane lane = room.lane;
        List<Player> on = online(lane);
        if (!skipped) {
            lane.cleared++;
            int place = ++roomClears[room.index];
            int pts = Math.max(4, 12 - (place - 1));
            for (Player p : on) {
                ctx.addPoints(p.getUniqueId(), pts, "Grid Runners: " + room.name() + " (" + Msg.ordinal(place) + ")");
                Msg.title(p, "§aRoom Cleared!", "§e+" + pts + " §7- " + Msg.ordinal(place) + " team to clear " + room.name(), 3, 35, 10);
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.4f);
            }
        } else {
            lane.skipped++;
            for (Player p : on) {
                Msg.send(p, "§7" + room.title() + " was auto-skipped after 3 minutes §8(no points)§7. The doors are open.");
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            }
        }
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int[] d : dirs) {
            Room n = lane.room(room.col + d[0], room.row + d[1]);
            if (n == null || n.type == RoomType.FINISH) continue;
            Arena.openDoor(world, lane, room, n);
            if (n.state == Room.State.LOCKED) n.state = Room.State.OPEN;
        }
        if (!lane.finalOpen && lane.allChallengesDone()) {
            lane.finalOpen = true;
            Room fin = lane.rooms[8];
            for (int[] d : dirs) {
                Room n = lane.room(fin.col + d[0], fin.row + d[1]);
                if (n != null) Arena.openDoor(world, lane, fin, n);
            }
            if (fin.state == Room.State.LOCKED) fin.state = Room.State.OPEN;
            for (Player p : on) {
                Msg.title(p, "§6Final Room Unlocked!", "§eSprint to the golden finish pad!", 5, 40, 10);
                p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
        updateSidebar();
    }

    public void laneFinished(Lane lane) {
        if (phase != Phase.RUNNING || ctx == null || lane.finished) return;
        lane.finished = true;
        lane.place = ++finishedCount;
        lane.finishSeconds = GAME_SECONDS - timeLeft;
        int pts = lane.place <= FINISH_POINTS.length ? FINISH_POINTS[lane.place - 1] : 5;
        lane.rooms[8].state = Room.State.DONE;
        for (Player p : online(lane)) {
            ctx.addPoints(p.getUniqueId(), pts, "Grid Runners: finished " + Msg.ordinal(lane.place));
            Msg.title(p, "§6§lCourse Complete!", "§e" + Msg.ordinal(lane.place) + " place §7- §e+" + pts, 5, 60, 15);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            p.getInventory().clear();
            p.setGameMode(GameMode.SPECTATOR);
        }
        ctx.broadcast(lane.label + " §afinished Grid Runners in §e" + Msg.time(lane.finishSeconds)
                + " §7(" + Msg.ordinal(lane.place) + ", " + lane.cleared + "/8 rooms cleared)");
        updateSidebar();
    }

    private void endGame(String reason) {
        if (phase == Phase.ENDING || phase == Phase.IDLE || ctx == null) return;
        phase = Phase.ENDING;
        if (mainTask != null) { mainTask.cancel(); mainTask = null; }
        if (buildTask != null) { buildTask.cancel(); buildTask = null; }
        for (Lane lane : lanes) for (Room r : lane.rooms) r.cleanup();
        List<Lane> sorted = new ArrayList<>(lanes);
        sorted.sort((a, b) -> {
            if (a.finished != b.finished) return a.finished ? -1 : 1;
            if (a.finished) return Integer.compare(a.place, b.place);
            return Integer.compare(b.cleared, a.cleared);
        });
        ctx.broadcast("§6§lGrid Runners §7- " + reason);
        for (Lane l : sorted) {
            ctx.broadcast(" " + l.label + "§7: " + (l.finished ? "§afinished " + Msg.ordinal(l.place) + " §7in " + Msg.time(l.finishSeconds)
                    : "§fdid not finish") + " §7- " + l.cleared + " rooms cleared");
        }
        for (Player p : new ArrayList<>(ctx.participants())) Msg.title(p, "§6Grid Runners over", "§7" + reason, 5, 50, 10);
        endReason = reason;
        endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endTask = null;
            finishNow(reason);
        }, FastTest.ticks(60L));
    }

    private void finishNow(String reason) {
        McmGameContext c = ctx;
        if (c == null || phase != Phase.ENDING) return;
        cleanupPlayers();
        cleanupWorld();
        phase = Phase.IDLE;
        ctx = null;
        c.finish(reason);
        scheduleArenaClear();
    }

    // ------------------------------------------------------------------ admin testing hooks
    // (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    private String endReason = "Grid Runners over";

    /** Ends the start countdown (doors open now) or the results wait. */
    public boolean skipWait() {
        if (ctx == null) return false;
        if (phase == Phase.COUNTDOWN) {
            startRunning();
            return true;
        }
        if (phase == Phase.ENDING && endTask != null) {
            endTask.cancel();
            endTask = null;
            finishNow(endReason);
            return true;
        }
        return false;
    }

    /**
     * Grid Runners is one round; "skip" skips every challenge room that is active right now, exactly like the
     * 3-minute auto-skip (no points, doors open).
     */
    public boolean skipRound() {
        if (ctx == null || phase != Phase.RUNNING) return false;
        List<Room> active = new ArrayList<>();
        for (Lane lane : lanes) {
            if (lane.finished) continue;
            for (Room r : lane.rooms) if (r.state == Room.State.ACTIVE && r.type != RoomType.FINISH) active.add(r);
        }
        if (active.isEmpty()) return false;
        ctx.broadcast("§7An admin skipped the active room(s) (no points).");
        for (Room r : active) roomCompleted(r, true);
        return true;
    }

    // ------------------------------------------------------------------ cleanup

    private void cleanupPlayers() {
        if (ctx == null) return;
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (p.getWorld() != world) continue;
            p.getInventory().clear();
            for (PotionEffect e : p.getActivePotionEffects()) p.removePotionEffect(e.getType());
            p.setGlowing(false);
            p.setGameMode(GameMode.SURVIVAL);
        }
    }

    private void cleanupWorld() {
        for (Lane lane : lanes) for (Room r : lane.rooms) if (r != null) r.cleanup();
        if (world != null) {
            removeWorldEntities();
            world.removePluginChunkTickets(plugin);
        }
    }

    private void removeWorldEntities() {
        if (world == null) return;
        for (Entity e : new ArrayList<>(world.getEntities())) if (!(e instanceof Player)) e.remove();
    }

    private void scheduleArenaClear() {
        if (world == null || !plugin.isEnabled()) return;
        if (clearTask != null) clearTask.cancel();
        final World w = world;
        clearTask = new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= MAX_LANES || phase != Phase.IDLE) { cancel(); clearTask = null; return; }
                Arena.clearLane(w, t++);
            }
        }.runTaskTimer(plugin, 20L, 2L);
    }

    // ------------------------------------------------------------------ queries used by rooms / listener

    public World world() { return world; }

    public boolean isLive() { return phase != Phase.IDLE && ctx != null; }

    public boolean isRunning() { return phase == Phase.RUNNING; }

    public List<Player> online(Lane lane) {
        List<Player> out = new ArrayList<>();
        if (ctx == null) return out;
        for (Player p : ctx.participants()) if (laneOf.get(p.getUniqueId()) == lane) out.add(p);
        return out;
    }

    public Lane laneOf(UUID u) { return laneOf.get(u); }

    public Room roomAt(org.bukkit.Location l) {
        if (l == null || l.getWorld() != world) return null;
        int idx = Math.floorDiv(l.getBlockX() + 15, LANE_SPACING);
        if (idx < 0 || idx >= lanes.size()) return null;
        return lanes.get(idx).roomAt(l);
    }

    public void tag(Entity e, String roomId) {
        e.getPersistentDataContainer().set(roomKey, PersistentDataType.STRING, roomId);
    }

    public void protect(Entity e) {
        e.getPersistentDataContainer().set(protKey, PersistentDataType.BYTE, (byte) 1);
    }

    public Room roomOf(Entity e) {
        if (e == null) return null;
        String id = e.getPersistentDataContainer().get(roomKey, PersistentDataType.STRING);
        return id == null ? null : roomById.get(id);
    }

    public boolean isProtected(Entity e) {
        return e != null && e.getPersistentDataContainer().has(protKey, PersistentDataType.BYTE);
    }

    public ItemStack crystal(int amount) {
        ItemStack it = new ItemStack(Material.AMETHYST_SHARD, amount);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(Msg.c("§dPower Crystal"));
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "crystal");
            it.setItemMeta(meta);
        }
        return it;
    }

    public boolean isCrystal(ItemStack it) {
        if (it == null || it.getType() != Material.AMETHYST_SHARD || !it.hasItemMeta()) return false;
        return "crystal".equals(it.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING));
    }

    public void rescue(Player p) {
        p.setHealth(p.getMaxHealth());
        p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, false, false, true));
        p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 0.5f, 1.5f);
        Msg.title(p, "§cKnocked down!", "§7You got back up - keep going", 0, 25, 8);
    }

    public GridRunnersPlugin plugin() { return plugin; }
}
