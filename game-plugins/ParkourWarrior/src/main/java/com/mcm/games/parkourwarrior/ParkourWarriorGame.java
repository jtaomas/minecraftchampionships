package com.mcm.games.parkourwarrior;

import com.mcm.api.McmFx;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.parkourwarrior.CourseModel.Difficulty;
import com.mcm.games.parkourwarrior.CourseModel.Kind;
import com.mcm.games.parkourwarrior.CourseModel.Obstacle;
import com.mcm.games.parkourwarrior.CourseModel.Pos;
import com.mcm.games.parkourwarrior.Progress.Node;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Parkour Warrior (spec v2): one shared course, medals, bonus paths, three endings, team multiplier. */
public final class ParkourWarriorGame implements McmGame {

    enum Phase { IDLE, PREPARING, COUNTDOWN, RUNNING, OVERTIME, ENDING }

    static final String GHOST_TEAM = "pw_ghost";

    final JavaPlugin plugin;
    final CourseWorld course;
    /** Admin-forced map id for the next games (null = random among the pool), set by /pw map. */
    volatile String forcedMap;
    /** Map chosen at enable for the first game (so the world is built once), consumed by start(). */
    private String plannedMap;
    /** Map picked in the game menu / /mcm map: used for the next game only. */
    private volatile String nextOnce;

    @Override public List<String> maps() { return course.availableMaps(s != null ? s : Settings.load(plugin.getConfig(), plugin.getLogger())); }

    @Override public boolean forceNextMap(String id) {
        if (id == null) return false;
        String c = id.trim().toLowerCase(java.util.Locale.ROOT).split(" ")[0];
        if (!maps().contains(c)) return false;
        nextOnce = c;
        return true;
    }

    @Override public String currentMap() { return running() ? course.mapId() : null; }

    /** The map the next game uses: the forced one, else the planned one, else a random one of the pool. */
    String pickMap(Settings st, boolean consumePlanned) {
        List<String> av = course.availableMaps(st);
        String once = nextOnce;
        if (once != null && av.contains(once)) { if (consumePlanned) nextOnce = null; plannedMap = null; return once; }
        String f = forcedMap;
        if (f != null && av.contains(f)) { plannedMap = null; return f; }
        if (plannedMap != null && av.contains(plannedMap)) { String p = plannedMap; if (consumePlanned) plannedMap = null; return p; }
        plannedMap = null;
        return av.get(new java.util.Random().nextInt(av.size()));
    }

    final NamespacedKey itemKey;
    Settings s;
    McmGameContext ctx;
    Phase phase = Phase.IDLE;
    Progress prog;

    final Map<UUID, Run> runs = new LinkedHashMap<>();
    final Map<String, List<Difficulty>> sideEndings = new HashMap<>();
    final Map<UUID, BossBar> bars = new HashMap<>();
    final Map<UUID, Set<UUID>> hiddenFrom = new HashMap<>();

    BukkitTask tickTask, countdownTask, endTask, prepTask;
    long tick;
    long goTick = -1;
    int elapsed;          // seconds since GO
    int timeAdjust;       // admin /pw time offset (seconds added to elapsed)
    boolean scrollRemoved, switchLocked, finished;
    int session;

    public ParkourWarriorGame(JavaPlugin plugin) {
        this.plugin = plugin;
        this.course = new CourseWorld(plugin);
        this.itemKey = new NamespacedKey(plugin, "pw_item");
    }

    @Override public String id() { return "parkourwarrior"; }
    @Override public String displayName() { return "Parkour Warrior"; }
    @Override public Material icon() { return Material.LEATHER_BOOTS; }
    @Override public String description() { return "One shared course: coins per challenge, bonus paths and three endings."; }
    @Override public boolean useCoreScoreboard() { return true; }

    boolean running() { return phase != Phase.IDLE; }

    /** True while the built map is a linear replica (one line of sections, plugin-side checkpoints; see CourseModel.Linear). */
    boolean linearCourse() { CourseModel m = course.model(); return m != null && m.linear != null; }

    /** Where this runner respawns: the position of the last checkpoint reached (linear course) or the node's respawn. */
    Pos respawnFor(Run r) {
        CourseModel m = course.model();
        if (m != null && m.linear != null) {
            if (r.cp <= 0 || r.cp > m.linear.checkpoints.size()) return prog.startSpawn(r.spawnIndex);
            return m.linear.checkpoints.get(r.cp - 1).respawn();
        }
        return prog.respawn(r.node);
    }
    boolean inPlay() { return phase == Phase.RUNNING || phase == Phase.OVERTIME; }
    Run run(Player p) { return p == null ? null : runs.get(p.getUniqueId()); }
    boolean isRunner(Player p) { Run r = run(p); return r != null && r.status == Run.Status.ACTIVE && r.online && running(); }

    static boolean fastTest() {
        try {
            Object o = Class.forName("com.mcm.api.McmApi").getMethod("isFastTest").invoke(null);
            return o instanceof Boolean b && b;
        } catch (Throwable t) { return false; }
    }

    // =====================================================================================
    // lifecycle
    // =====================================================================================

    /** Pre-build at enable so the first game starts at once. */
    void preload() {
        s = Settings.load(plugin.getConfig(), plugin.getLogger());
        course.ensureWorld();
        plannedMap = pickMap(s, false);
        course.prepare(s, plannedMap, false, null);
    }

    @Override
    public void start(McmGameContext context) {
        if (running()) cleanup();
        plugin.reloadConfig();
        this.ctx = context;
        this.s = Settings.load(plugin.getConfig(), plugin.getLogger());
        finished = false;
        scrollRemoved = false;
        switchLocked = false;
        timeAdjust = 0;
        elapsed = 0;
        goTick = -1;
        tick = 0;
        runs.clear();
        sideEndings.clear();
        phase = Phase.PREPARING;
        final int mySession = ++session;
        ctx.setTimer(s.time, s.time);
        ctx.setSidebarLines(List.of(ChatColor.GRAY + "Preparing the course..."));
        if (course.world() == null) course.ensureWorld();
        if (!course.ready()) McmFx.actionBar(ctx.participants(), ChatColor.YELLOW + "Building the Parkour Warrior course...");
        final String mapId = pickMap(s, true);
        course.prepare(s, mapId, false, () -> { if (mySession == session && phase == Phase.PREPARING) begin(); });
        prepTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (mySession != session || phase != Phase.PREPARING) { if (prepTask != null) { prepTask.cancel(); prepTask = null; } return; }
            if (course.state() == CourseWorld.State.FAILED || course.state() == CourseWorld.State.NONE) {
                plugin.getLogger().severe("Parkour Warrior: course could not be prepared; ending the game");
                endNow("Course failed to build");
                return;
            }
            McmFx.actionBar(ctx.participants(), ChatColor.YELLOW + "Building the Parkour Warrior course...");
        }, 20L, 20L);
    }

    private void begin() {
        CourseModel m = course.model();
        prog = new Progress(m);
        World w = course.world();
        w.setTime(s.dayStart);
        course.holdChunks();
        course.spawnLabels();
        if (course.movers() != null && !course.movers().isEmpty()) course.movers().begin(0);
        int i = 0;
        for (Player p : new ArrayList<>(ctx.participants())) {
            String side = McmFx.sideKeyOf(ctx, p);
            Run r = new Run(p.getUniqueId(), p.getName(), side, i++, prog.start());
            runs.put(p.getUniqueId(), r);
            sideEndings.computeIfAbsent(side, k -> new ArrayList<>());
            preparePlayer(p, r);
            teleport(p, course.loc(prog.startSpawn(r.spawnIndex)));
        }
        phase = Phase.COUNTDOWN;
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::onTick, 1L, 1L);
        List<Player> all = new ArrayList<>(ctx.participants());
        if (!fastTest()) McmFx.titleCard(all, ChatColor.GOLD + "Parkour Warrior",
                ChatColor.YELLOW + (m.linear != null ? "Coins for every checkpoint" : "Coins for every challenge, bonus paths, three endings"));
        sendRules(all);
        if (m.credit != null && !m.credit.isEmpty())
            for (Player p : all) p.sendMessage(ChatColor.GRAY + "Map: " + ChatColor.WHITE + m.mapId + ChatColor.GRAY + (m.linear != null ? " - " : " - course sections built by ") + ChatColor.WHITE + m.credit);
        int cd = fastTest() ? 1 : s.countdown;
        countdownTask = McmFx.countdown(plugin, all, cd, ChatColor.YELLOW + "Get ready to run!", this::go);
        updateHud();
    }

    private void go() {
        if (phase != Phase.COUNTDOWN) return;
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        phase = Phase.RUNNING;
        goTick = tick;
        elapsed = 0;
        for (Run r : runs.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p != null && r.online) giveItems(p, r);
        }
        ctx.setTimer(s.time, s.time);
        updateHud();
    }

    /** Admin /mcm skipwait. */
    public boolean skipWait() {
        if (phase == Phase.COUNTDOWN) { go(); return true; }
        if (phase == Phase.ENDING && endTask != null) {
            endTask.cancel();
            endTask = null;
            doFinish();
            return true;
        }
        return false;
    }

    /** Admin /mcm skip: single course, nothing to skip. */
    public boolean skipRound() { return false; }

    @Override
    public void stop() {
        session++;
        if (!running()) { cleanupWorldOnly(); return; }
        cleanup();
    }

    private void cleanupWorldOnly() {
        course.removeLabels();
        course.releaseChunks();
    }

    private void cleanup() {
        phase = Phase.IDLE;
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (endTask != null) { endTask.cancel(); endTask = null; }
        if (prepTask != null) { prepTask.cancel(); prepTask = null; }
        for (BossBar b : bars.values()) b.removeAll();
        bars.clear();
        for (Map.Entry<UUID, Set<UUID>> e : hiddenFrom.entrySet()) {
            Player v = Bukkit.getPlayer(e.getKey());
            if (v == null) continue;
            for (UUID t : e.getValue()) { Player tp = Bukkit.getPlayer(t); if (tp != null) v.showPlayer(plugin, tp); }
        }
        hiddenFrom.clear();
        for (Run r : runs.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p == null) continue;
            resetPlayer(p);
        }
        course.removeLabels();
        course.releaseChunks();
        course.resetTimed();
        if (course.movers() != null) course.movers().stop();
        World w = course.world();
        if (w != null) w.setTime(6000);
    }

    private void resetPlayer(Player p) {
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.setCollidable(true);
        p.getInventory().clear();
        Scoreboard sb = p.getScoreboard();
        if (sb != Bukkit.getScoreboardManager().getMainScoreboard()) {
            Team t = sb.getTeam(GHOST_TEAM);
            if (t != null) t.unregister();
        }
    }

    // =====================================================================================
    // players
    // =====================================================================================

    private void preparePlayer(Player p, Run r) {
        p.leaveVehicle();
        p.setGameMode(GameMode.ADVENTURE);
        p.setAllowFlight(false);
        p.setFlying(false);
        p.getInventory().clear();
        AttributeInstance hp = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(hp == null ? 20.0 : hp.getValue());
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.setExp(0f);
        p.setLevel(0);
        for (PotionEffect pe : p.getActivePotionEffects()) p.removePotionEffect(pe.getType());
        // no setCollidable(false): on Paper it also makes the player untargetable by projectiles; the ghost team's
        // COLLISION_RULE NEVER (ensureGhostTeams) turns player pushing off in both ghost modes
        if (s.translucent) p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
        p.getInventory().setBoots(teamBoots(p));
        BossBar b = bars.computeIfAbsent(p.getUniqueId(), k -> Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SEGMENTED_10));
        b.addPlayer(p);
        if (inPlay()) giveItems(p, r);
    }

    void teleport(Player p, Location l) {
        p.setFallDistance(0f);
        p.setVelocity(new Vector(0, 0, 0));
        p.teleport(l);
        p.setFallDistance(0f);
    }

    private ItemStack item(Material m, int amount, String name, String id, String... lore) {
        ItemStack it = new ItemStack(m, Math.max(1, Math.min(64, amount)));
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(List.of(lore));
        meta.addItemFlags(ItemFlag.values());
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
        it.setItemMeta(meta);
        return it;
    }

    String itemId(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    /** Owner request (as in Hole in the Wall): invisible runners wear leather boots in their team colour. */
    private ItemStack teamBoots(Player p) {
        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        if (boots.getItemMeta() instanceof org.bukkit.inventory.meta.LeatherArmorMeta meta) {
            ChatColor c = ctx == null ? ChatColor.WHITE : ctx.teamColor(ctx.teamOf(p.getUniqueId()));
            meta.setColor(bootColor(c));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
            boots.setItemMeta(meta);
        }
        return boots;
    }

    private static org.bukkit.Color bootColor(ChatColor c) {
        if (c == null) return org.bukkit.Color.WHITE;
        return switch (c) {
            case RED -> org.bukkit.Color.fromRGB(0xFF5555);
            case DARK_RED -> org.bukkit.Color.fromRGB(0xAA0000);
            case GOLD -> org.bukkit.Color.fromRGB(0xFFAA00);
            case YELLOW -> org.bukkit.Color.fromRGB(0xFFFF55);
            case GREEN -> org.bukkit.Color.fromRGB(0x55FF55);
            case DARK_GREEN -> org.bukkit.Color.fromRGB(0x00AA00);
            case AQUA -> org.bukkit.Color.fromRGB(0x55FFFF);
            case DARK_AQUA -> org.bukkit.Color.fromRGB(0x00AAAA);
            case BLUE -> org.bukkit.Color.fromRGB(0x5555FF);
            case DARK_BLUE -> org.bukkit.Color.fromRGB(0x0000AA);
            case LIGHT_PURPLE -> org.bukkit.Color.fromRGB(0xFF55FF);
            case DARK_PURPLE -> org.bukkit.Color.fromRGB(0xAA00AA);
            case GRAY -> org.bukkit.Color.fromRGB(0xAAAAAA);
            case DARK_GRAY -> org.bukkit.Color.fromRGB(0x555555);
            case BLACK -> org.bukkit.Color.fromRGB(0x1D1D21);
            default -> org.bukkit.Color.WHITE;
        };
    }

    void giveItems(Player p, Run r) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setBoots(teamBoots(p));
        if (r.status == Run.Status.ACTIVE && inPlay()) {
            if (!scrollRemoved && !linearCourse()) inv.setItem(7, item(Material.PAPER, 1, "§dTeleportation Scroll §7(right-click)", "scroll",
                    "§7Leave a bonus path and return to", "§7the main line. Your coins are kept."));
            if (!switchLocked && prog != null && prog.isEndingPhase(r.mainNode))
                inv.setItem(8, item(Material.COMPASS, 1, "§bReturn to the Fork §7(right-click)", "fork",
                        "§7Switch to a different ending.", "§7Allowed until " + s.switchLockAt + " s remain."));
        }
        inv.setHeldItemSlot(4);
    }

    void onItemUse(Player p, String id) {
        Run r = run(p);
        if (r == null || r.status != Run.Status.ACTIVE || !inPlay()) return;
        long now = System.currentTimeMillis();
        if (now - r.lastItemUseMs < 750) return;
        r.lastItemUseMs = now;
        if ("scroll".equals(id)) useScroll(p, r);
        else if ("fork".equals(id)) useForkItem(p, r);
    }

    private void useScroll(Player p, Run r) {
        if (scrollRemoved) return;
        boolean onBonus = prog.isBonus(r.node);
        if (!onBonus && r.node.type() == Progress.Type.HUB) {
            Obstacle b1 = prog.bonus(r.node.stage(), 1);
            Location l = p.getLocation();
            if (b1 != null && b1.area() != null && b1.area().containsColumn(course.lx(l), course.lz(l))) onBonus = true;
        }
        if (!onBonus) { p.sendMessage(ChatColor.GRAY + "The scroll only works on a bonus path."); return; }
        r.node = r.mainNode;
        r.returning = false;
        teleport(p, course.loc(prog.respawn(r.mainNode)));
        r.noFallUntilTick = tick + 10;
        p.sendMessage(ChatColor.LIGHT_PURPLE + "Whoosh! " + ChatColor.WHITE + "Back on the main line. Your coins are kept.");
        McmFx.sound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
    }

    private void useForkItem(Player p, Run r) {
        if (switchLocked || !prog.isEndingPhase(r.mainNode)) return;
        r.node = Node.FORK;
        r.mainNode = Node.FORK;
        teleport(p, course.loc(prog.respawn(Node.FORK)));
        r.noFallUntilTick = tick + 10;
        p.sendMessage(ChatColor.AQUA + "Back at the fork. " + ChatColor.WHITE + "Pick any ending: "
                + ChatColor.GREEN + "Easy" + ChatColor.WHITE + ", " + ChatColor.GOLD + "Medium" + ChatColor.WHITE + " or " + ChatColor.RED + "Hard" + ChatColor.WHITE + ".");
        McmFx.sound(p, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.0f);
    }

    // =====================================================================================
    // tick
    // =====================================================================================

    private void onTick() {
        if (!running() || phase == Phase.PREPARING) return;
        try {
            tick++;
            course.updateTimed(tick);
            MoverRuntime mr = course.movers();
            if (mr != null && !mr.isEmpty()) {
                List<Player> live = new ArrayList<>();
                for (Run r : runs.values()) { Player lp = r.online ? Bukkit.getPlayer(r.id) : null; if (lp != null && r.status == Run.Status.ACTIVE) live.add(lp); }
                mr.tick(tick, live);
            }
            ensureGhostTeams();
            if (phase == Phase.COUNTDOWN) leash();
            if (inPlay()) {
                for (Run r : new ArrayList<>(runs.values())) {
                    if (r.status != Run.Status.ACTIVE || !r.online) continue;
                    Player p = Bukkit.getPlayer(r.id);
                    if (p == null) continue;
                    checkPlayer(p, r);
                    if (!inPlay()) return;
                }
                if (s.hideWithin > 0 && tick % 5 == 0) updateHiding();
                if ((tick - goTick) % 20 == 0) secondTick();
            } else if (phase == Phase.COUNTDOWN && tick % 20 == 0) {
                updateHud();
            }
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Parkour Warrior tick failed", t);
        }
    }

    private void leash() {
        for (Run r : runs.values()) {
            if (!r.online) continue;
            Player p = Bukkit.getPlayer(r.id);
            if (p == null) continue;
            Location sp = course.loc(prog.startSpawn(r.spawnIndex));
            Location l = p.getLocation();
            if (l.getWorld() != sp.getWorld() || Math.hypot(l.getX() - sp.getX(), l.getZ() - sp.getZ()) > s.startLeash
                    || l.getY() < sp.getY() - 3) {
                sp.setYaw(l.getYaw());
                sp.setPitch(l.getPitch());
                teleport(p, sp);
            }
        }
    }

    private void checkPlayer(Player p, Run r) {
        Location l = p.getLocation();
        if (l.getWorld() != course.world()) {
            teleport(p, course.loc(respawnFor(r)));
            return;
        }
        if (p.getGameMode() != GameMode.ADVENTURE && !(p.isOp() && p.getGameMode() == GameMode.CREATIVE)) p.setGameMode(GameMode.ADVENTURE);   // ops may stay in creative
        double x = course.lx(l), y = course.ly(l), z = course.lz(l);
        if (linearCourse()) {      // replica imported 1:1: the checkpoints its command blocks implemented
            if (checkLinear(p, r, x, y, z)) return;
            if (tick >= r.noFallUntilTick && fell(r, x, y, z)) onFall(p, r, x, z);
            return;
        }
        // pads
        for (Obstacle o : prog.padCandidates(r.node, r.mainNode, !switchLocked)) {
            if (o.pad() != null && o.pad().containsFeet(x, y, z)) {
                if (complete(p, r, o)) return;
            }
        }
        // finish regions
        Difficulty d = prog.endingOf(r.node);
        if (d != null) {
            CourseModel.Box f = course.model().finishes.get(d);
            if (f != null && f.containsFeet(x, y, z)) {
                Obstacle last = prog.ending(d, prog.lastIndex(Kind.ENDING, 0, d));
                if (last != null && !r.completed.contains(last.id())) complete(p, r, last);
                else finishRun(p, r, d);
                return;
            }
        }
        // moving traps: a hit counts as a fall (sweep from last tick's position to this one)
        MoverRuntime mr = course.movers();
        double[] lp = r.lastPos;
        r.lastPos = new double[]{x, y, z};
        if (lp != null && Math.abs(lp[0] - x) + Math.abs(lp[1] - y) + Math.abs(lp[2] - z) > 6) lp = null;   // teleported
        if (mr != null && !mr.isEmpty() && lp != null && tick >= r.noFallUntilTick) {
            CourseModel.Mover hit = mr.hit(lp[0], lp[1], lp[2], x, y, z);
            if (hit != null) { onFall(p, r, x, z, ChatColor.RED + "Hit by the " + hit.name() + "! Back to your last checkpoint."); return; }
        }
        // falls
        if (tick >= r.noFallUntilTick && fell(r, x, y, z)) onFall(p, r, x, z);
    }

    private boolean fell(Run r, double x, double y, double z) {
        CourseModel m = course.model();
        if (m.linear != null) return y < m.linear.fallY;      // the replica's own fall line (command block: y below -30)
        if (y < m.minY - s.fallMarginBelowCourse) return true;
        List<Obstacle> c = prog.fallCandidates(r.node, r.mainNode);
        if (c.isEmpty()) return false;
        int min = Integer.MAX_VALUE;
        for (Obstacle o : c) {
            min = Math.min(min, o.killY());
            if (o.area() != null && o.area().containsColumn(x, z) && y < o.killY()) return true;
        }
        return y < min;
    }

    private void onFall(Player p, Run r, double x, double z) {
        onFall(p, r, x, z, linearCourse() ? ChatColor.RED + "You fell! Returning to last checkpoint..." : ChatColor.RED + "You fell! Back to your last checkpoint.");
    }

    // =====================================================================================
    // linear (replica) course: the logic of the replica's command blocks, in plugin code
    // =====================================================================================

    /** Touch tests of a linear course, per tick. True if the run was moved/finished (stop checking this tick). */
    private boolean checkLinear(Player p, Run r, double x, double y, double z) {
        CourseModel.Linear L = course.model().linear;
        for (CourseModel.Checkpoint c : L.checkpoints) {
            if (c.n() > r.cp && c.trigger().hitsPlayer(x, y, z)) { completeLinear(p, r, c.n()); return false; }
        }
        if (L.finish != null && L.finish.hitsPlayer(x, y, z) && r.status == Run.Status.ACTIVE) { finishLinear(p, r); return true; }
        return false;
    }

    /** Obstacle id of section k (1-based) of a linear course. */
    private String sectionId(int k) {
        int per = course.model().linear.perStage;
        return CourseModel.mainId((k - 1) / per + 1, (k - 1) % per + 1);
    }

    /** Checkpoint k reached: coins (once), the checkpoint moves forward only, "Spawn point updated". */
    private void completeLinear(Player p, Run r, int k) {
        CourseModel m = course.model();
        CourseModel.Linear L = m.linear;
        String id = sectionId(k);
        boolean first = r.completed.add(id);
        r.cp = Math.max(r.cp, k);
        Obstacle o = m.obstacle(id);
        String name = k - 1 < L.sections.size() ? L.sections.get(k - 1) : id;
        int per = L.perStage;
        if (first) {
            int pts = o == null ? 0 : Scoring.coins(s, m, o);
            r.coins += pts;
            p.sendMessage(ChatColor.WHITE + "You completed " + ChatColor.YELLOW + "Stage " + ((k - 1) / per + 1) + "-" + ((k - 1) % per + 1)
                    + ChatColor.WHITE + " (" + ChatColor.YELLOW + name + ChatColor.WHITE + ")!");
            if (pts > 0) ctx.addObjectiveCoins(r.id, pts, null, "Checkpoint " + k + " completed");
            if (k % per == 0) {
                CourseModel.Hub h = m.hub(k / per);
                String zone = h != null ? h.zoneName() : "Stage " + (k / per);
                p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You completed the " + zone + " zone!");
                McmFx.title(p, "", ChatColor.GOLD + zone + " zone complete!", 5, 30, 10);
                McmFx.sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
            }
        }
        spawnUpdated(p, r);
    }

    /** The finish volume: the course is done (one line, one finish); coins from coins.course.<map>.finish. */
    private void finishLinear(Player p, Run r) {
        if (r.status != Run.Status.ACTIVE) return;
        CourseModel m = course.model();
        r.status = Run.Status.FINISHED;
        r.finishTick = tick;
        r.completed.add(sectionId(m.linear.sectionCount()));
        String who = McmFx.sideColor(ctx, r.sideKey) + p.getName();
        ctx.broadcast(who + ChatColor.WHITE + " finished the entire course!");
        int fc = Scoring.finish(s, m.mapId);
        r.coins += fc;
        if (fc > 0) ctx.addObjectiveCoins(r.id, fc, null, "Course completed");
        McmFx.title(p, ChatColor.GOLD + "" + ChatColor.BOLD + "FINISHED!", ChatColor.WHITE + "You finished the entire course", 5, 50, 15);
        McmFx.teamFirework(p.getLocation().add(0, 1, 0), McmFx.sideColor(ctx, r.sideKey), true);
        McmFx.sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        makeSpectator(p);
        checkAllDone();
    }

    private void onFall(Player p, Run r, double x, double z, String msg) {
        r.falls++;
        if (phase == Phase.OVERTIME) { knockOut(p, r); return; }
        Pos target = respawnFor(r);
        if (!linearCourse()) for (Obstacle o : prog.candidates(r.node)) {
            if (o.area() != null && o.area().containsColumn(x, z)) { target = o.entry(); break; }
        }
        teleport(p, course.loc(target));
        r.noFallUntilTick = tick + 5;
        r.lastPos = null;
        p.sendMessage(msg);
        McmFx.sound(p, Sound.ENTITY_PLAYER_HURT, 0.6f, 1.0f);
        McmFx.sound(p, Sound.BLOCK_NOTE_BLOCK_BASS, 0.7f, 0.6f);
    }

    private void knockOut(Player p, Run r) {
        r.status = Run.Status.OUT;
        p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You fell in overtime! " + ChatColor.RED + "Your run is over.");
        McmFx.title(p, ChatColor.RED + "" + ChatColor.BOLD + "RUN OVER", ChatColor.GRAY + "You fell in overtime", 5, 40, 10);
        McmFx.sound(p, Sound.ENTITY_ITEM_BREAK, 1f, 0.6f);
        makeSpectator(p);
        checkAllDone();
    }

    // =====================================================================================
    // progress
    // =====================================================================================

    /** Returns true if the player was moved/finished (stop checking this tick). */
    boolean complete(Player p, Run r, Obstacle o) {
        boolean first = r.completed.add(o.id());
        Node next = prog.afterCompletion(o);
        if (first) award(p, r, o);
        switch (o.kind()) {
            case MAIN -> {
                if (next.equals(r.node) && next.equals(r.mainNode)) return false;
                r.node = next;
                r.mainNode = next;
                spawnUpdated(p, r);
                if (o.index() >= prog.lastIndex(Kind.MAIN, o.stage(), null) && first) {
                    CourseModel.Hub h = course.model().hub(o.stage());
                    String zone = h != null ? h.zoneName() : "Stage " + o.stage();
                    p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You completed the " + zone + " zone!");
                    McmFx.title(p, "", ChatColor.GOLD + zone + " zone complete!", 5, 30, 10);
                    McmFx.sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1.2f);
                }
                if (next.type() == Progress.Type.FORK) {
                    p.sendMessage(ChatColor.AQUA + "You reached the fork! " + ChatColor.WHITE + "Pick an ending: "
                            + ChatColor.GREEN + "Easy (+" + s.endEasy + " coins)" + ChatColor.WHITE + ", "
                            + ChatColor.GOLD + "Medium (+" + s.endMedium + ")" + ChatColor.WHITE + " or "
                            + ChatColor.RED + "Hard (+" + s.endHard + ")" + ChatColor.WHITE + ".");
                    giveItems(p, r);
                }
                return false;
            }
            case BONUS -> {
                if (next == null) {
                    if (r.returning) return false;
                    r.returning = true;
                    r.node = r.mainNode;
                    p.sendMessage(ChatColor.GOLD + "Bonus path complete! " + ChatColor.WHITE + "Returning you to the main line...");
                    final UUID id = r.id;
                    final int ses = session;
                    int delay = fastTest() ? 5 : s.bonusReturnDelayTicks;
                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (ses != session || !inPlay()) return;
                        Run rr = runs.get(id);
                        Player pp = Bukkit.getPlayer(id);
                        if (rr == null || pp == null || !rr.returning || rr.status != Run.Status.ACTIVE) return;
                        rr.returning = false;
                        rr.node = rr.mainNode;
                        teleport(pp, course.loc(prog.respawn(rr.mainNode)));
                        rr.noFallUntilTick = tick + 10;
                        McmFx.sound(pp, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
                    }, Math.max(1, delay));
                    return false;
                }
                if (next.equals(r.node)) return false;
                r.node = next;
                r.returning = false;
                spawnUpdated(p, r);
                return false;
            }
            default -> {
                if (next == null) { finishRun(p, r, o.ending()); return true; }
                if (next.equals(r.node)) return false;
                boolean switched = prog.endingOf(r.mainNode) != null && prog.endingOf(r.mainNode) != o.ending();
                r.node = next;
                r.mainNode = next;
                spawnUpdated(p, r);
                if (first) p.sendMessage(ChatColor.WHITE + "You completed " + ChatColor.YELLOW + o.name() + ChatColor.WHITE + "!");
                else if (switched) p.sendMessage(ChatColor.AQUA + "You switched to the " + CourseWorld.cap(o.ending().name()) + " ending.");
                return false;
            }
        }
    }

    private void award(Player p, Run r, Obstacle o) {
        if (o.kind() == Kind.ENDING) return;
        int pts = Scoring.coins(s, o);
        r.coins += pts;
        String what = o.kind() == Kind.MAIN ? "Checkpoint " + mainDone(r) + " completed" : "Bonus: " + o.name();
        p.sendMessage(ChatColor.WHITE + "You completed " + ChatColor.YELLOW + o.name() + ChatColor.WHITE + "!");
        if (pts > 0) ctx.addObjectiveCoins(r.id, pts, null, what);
        giveItems(p, r);
    }

    private int mainDone(Run r) {
        int n = 0;
        for (String id : r.completed) if (id.startsWith("M")) n++;
        return n;
    }

    private void spawnUpdated(Player p, Run r) {
        r.lastMessageMs = System.currentTimeMillis();
        McmFx.actionBar(p, ChatColor.GREEN + "Spawn point updated");
        McmFx.sound(p, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.5f);
    }

    private void finishRun(Player p, Run r, Difficulty d) {
        if (r.status != Run.Status.ACTIVE) return;
        r.status = Run.Status.FINISHED;
        r.ending = d;
        r.finishTick = tick;
        sideEndings.computeIfAbsent(r.sideKey, k -> new ArrayList<>()).add(d);
        ChatColor c = d == Difficulty.EASY ? ChatColor.GREEN : d == Difficulty.MEDIUM ? ChatColor.GOLD : ChatColor.RED;
        String who = McmFx.sideColor(ctx, r.sideKey) + p.getName();
        ctx.broadcast(who + ChatColor.WHITE + " finished the entire course! " + c + "(" + CourseWorld.cap(d.name()) + " ending)");
        int ec = Scoring.ending(s, d);
        r.coins += ec;
        if (ec > 0) ctx.addObjectiveCoins(r.id, ec, null, "You finished the " + CourseWorld.cap(d.name()) + " ending");
        McmFx.title(p, c + "" + ChatColor.BOLD + "FINISHED!", ChatColor.WHITE + CourseWorld.cap(d.name()) + " ending", 5, 50, 15);
        McmFx.teamFirework(p.getLocation().add(0, 1, 0), McmFx.sideColor(ctx, r.sideKey), true);
        McmFx.sound(p, Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        makeSpectator(p);
        checkAllDone();
    }

    void makeSpectator(Player p) {
        p.getInventory().clear();
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.setGameMode(GameMode.SPECTATOR);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (running() && p.isOnline() && p.getGameMode() == GameMode.SPECTATOR) teleport(p, course.overview(0));
        }, 30L);
        sendOverviewLinks(p);
    }

    void sendOverviewLinks(Player p) {
        CourseModel m = course.model();
        if (m == null) return;
        Component c = Component.text("Spectator views: ", NamedTextColor.GRAY);
        int n = Math.max(1, m.overviews.size());
        for (int i = 0; i < n; i++) {
            String name = m.overviews.isEmpty() ? "Overview" : m.overviews.get(i).name();
            c = c.append(Component.text("[" + name + "] ", NamedTextColor.AQUA).clickEvent(ClickEvent.runCommand("/pwview " + (i + 1))));
        }
        p.sendMessage(c);
    }

    // =====================================================================================
    // seconds / time
    // =====================================================================================

    int secondsLeft() { return s.time - elapsed - timeAdjust; }

    private void secondTick() {
        elapsed++;
        int left = secondsLeft();
        if (!scrollRemoved && left <= s.scrollRemoveAt) removeScrolls();
        if (!switchLocked && left <= s.switchLockAt) lockSwitching();
        if (phase == Phase.RUNNING && left <= 0) startOvertime();
        if (phase == Phase.OVERTIME) {
            int ot = s.overtime + left;
            ctx.setTimer(Math.max(0, ot), Math.max(1, s.overtime));
            if (ot <= 0) { endGame("Time's up!"); return; }
        } else {
            ctx.setTimer(Math.max(0, left), s.time);
        }
        World w = course.world();
        if (w != null) {
            double f = Math.min(1.0, Math.max(0.0, (elapsed + timeAdjust) / (double) Math.max(1, s.time + s.overtime)));
            w.setTime(s.dayStart + Math.round((s.dayEnd - s.dayStart) * f));
        }
        confineSpectators();
        updateHud();
        checkAllDone();
    }

    /** Admin: set the remaining normal time (negative = into overtime). */
    void setSecondsLeft(int left) {
        timeAdjust = s.time - elapsed - left;
        if (left > 0 && phase == Phase.OVERTIME) phase = Phase.RUNNING;
        if (left > s.scrollRemoveAt && scrollRemoved) {
            scrollRemoved = false;
            refreshItems();
        }
        if (left > s.switchLockAt && switchLocked) { switchLocked = false; refreshItems(); }
    }

    private void refreshItems() {
        for (Run r : runs.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p != null && r.online && r.status == Run.Status.ACTIVE) giveItems(p, r);
        }
    }

    private void removeScrolls() {
        scrollRemoved = true;
        for (Run r : runs.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p == null || !r.online || r.status != Run.Status.ACTIVE) continue;
            p.sendMessage(ChatColor.LIGHT_PURPLE + "Your teleportation scroll was taken away: less than " + s.scrollRemoveAt + " seconds remain!");
            giveItems(p, r);
        }
    }

    private void lockSwitching() {
        switchLocked = true;
        for (Run r : runs.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p == null || !r.online || r.status != Run.Status.ACTIVE) continue;
            if (prog.isEndingPhase(r.mainNode)) p.sendMessage(ChatColor.AQUA + "Endings are locked in: you can no longer switch ending.");
            giveItems(p, r);
        }
    }

    private void startOvertime() {
        phase = Phase.OVERTIME;
        List<Player> act = new ArrayList<>();
        for (Run r : runs.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p != null && r.online && r.status == Run.Status.ACTIVE) act.add(p);
        }
        McmFx.title(act, ChatColor.RED + "" + ChatColor.BOLD + "OVERTIME", ChatColor.YELLOW + "One fall ends your run!", 5, 40, 10);
        McmFx.sound(act, Sound.BLOCK_BELL_USE, 1f, 0.8f);
        for (Player p : act) p.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "OVERTIME! " + ChatColor.YELLOW + s.overtime + " s left. You can still complete challenges, but a fall ends your run.");
    }

    private void confineSpectators() {
        World w = course.world();
        if (w == null) return;
        for (Run r : runs.values()) {
            if (r.status == Run.Status.ACTIVE || !r.online) continue;
            Player p = Bukkit.getPlayer(r.id);
            if (p == null || p.getGameMode() != GameMode.SPECTATOR) continue;
            if (p.getSpectatorTarget() != null) continue;
            if (!course.inBounds(p.getLocation(), s.spectatorMargin)) teleport(p, course.overview(0));
        }
    }

    void checkAllDone() {
        if (!inPlay()) return;
        boolean anyActive = false;
        for (Run r : runs.values()) if (r.online && r.status == Run.Status.ACTIVE) { anyActive = true; break; }
        if (!anyActive) {
            boolean anyFinished = runs.values().stream().anyMatch(r -> r.status == Run.Status.FINISHED);
            endGame(anyFinished ? "Every runner is done!" : "No runners left");
        }
    }

    // =====================================================================================
    // HUD
    // =====================================================================================

    void updateHud() {
        if (ctx == null) return;
        List<Run> sorted = new ArrayList<>(runs.values());
        sorted.sort(Comparator.comparingInt((Run r) -> r.coins).reversed());
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "" + ChatColor.BOLD + "COINS");
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Run r = sorted.get(i);
            lines.add(ChatColor.YELLOW + "" + (i + 1) + ". " + McmFx.sideColor(ctx, r.sideKey) + trim(r.name, 12) + ChatColor.GRAY + " " + r.coins
                    + (r.status == Run.Status.FINISHED ? ChatColor.GREEN + " ✔" : r.status == Run.Status.OUT ? ChatColor.RED + " ✖" : ""));
        }
        int mainTotalAll = prog == null ? 15 : prog.mainCount;
        int bestDone = 0;
        for (Run r : runs.values()) bestDone = Math.max(bestDone, mainDone(r));
        lines.add(ChatColor.GRAY + "Challenges left " + ChatColor.WHITE + Math.max(0, mainTotalAll - bestDone) + ChatColor.GRAY + "/" + ChatColor.WHITE + mainTotalAll);
        long fin = runs.values().stream().filter(r -> r.status == Run.Status.FINISHED).count();
        lines.add(ChatColor.GRAY + "Finished " + ChatColor.WHITE + fin + ChatColor.GRAY + "/" + ChatColor.WHITE + runs.size());
        ctx.setSidebarLines(lines);

        int left = phase == Phase.COUNTDOWN ? s.time : secondsLeft();
        boolean ot = phase == Phase.OVERTIME;
        int show = ot ? Math.max(0, s.overtime + left) : Math.max(0, left);
        // owner: "time left ... it's duplicated" - the core's bar already shows TIME LEFT, so ours only says OVERTIME
        String title = ot ? ChatColor.RED + "" + ChatColor.BOLD + "OVERTIME " + ChatColor.WHITE + mmss(show) : "";
        double progress = ot ? show / (double) Math.max(1, s.overtime) : show / (double) Math.max(1, s.time);
        int mainTotal = prog == null ? 15 : prog.mainCount;
        long now = System.currentTimeMillis();
        for (Run r : runs.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p == null || !r.online) continue;
            BossBar b = bars.computeIfAbsent(r.id, k -> Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SEGMENTED_10));
            if (!b.getPlayers().contains(p)) b.addPlayer(p);
            b.setTitle(title);
            b.setColor(ot ? BarColor.RED : BarColor.WHITE);
            b.setProgress(Math.max(0, Math.min(1, progress)));
            if (r.status == Run.Status.ACTIVE && inPlay() && now - r.lastMessageMs > 2500) {
                int mainDone = 0;
                for (String id : r.completed) if (id.startsWith("M")) mainDone++;
                McmFx.actionBar(p, ChatColor.GOLD + "Coins " + ChatColor.WHITE + r.coins
                        + ChatColor.DARK_GRAY + "  |  " + ChatColor.GRAY + "Challenges left " + ChatColor.WHITE + (mainTotal - mainDone) + "/" + mainTotal);
            }
        }
        for (Map.Entry<UUID, BossBar> e : bars.entrySet()) {
            if (runs.containsKey(e.getKey())) continue;
            e.getValue().setTitle(title);
            e.getValue().setColor(ot ? BarColor.RED : BarColor.WHITE);
            e.getValue().setProgress(Math.max(0, Math.min(1, progress)));
        }
    }

    static String mmss(int sec) { return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60); }
    static String fmt2(double d) { return String.format(Locale.ROOT, "%.2f", d); }
    static String fmtMult(Double d) { return String.format(Locale.ROOT, "%.2f", d == null ? 0 : d); }
    static String trim(String s, int n) { return s.length() <= n ? s : s.substring(0, n); }

    // =====================================================================================
    // collision / visibility
    // =====================================================================================

    boolean ghost() { return s != null && s.translucent; }

    /**
     * /pw ghost: ON = runners invisible + same team with "see friendly invisibles" (translucent to each other),
     * OFF = fully visible. Collision stays off in both modes (team rule). Applies at once to a running game.
     */
    void setGhost(boolean on) {
        if (s != null) s.translucent = on;
        if (!running()) return;
        for (Run r : runs.values()) {
            if (!r.online || r.status != Run.Status.ACTIVE) continue;
            Player p = Bukkit.getPlayer(r.id);
            if (p == null || p.getGameMode() == GameMode.SPECTATOR) continue;
            if (on) p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
            else p.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    private void ensureGhostTeams() {
        List<String> names = new ArrayList<>();
        for (Run r : runs.values()) if (r.online) names.add(r.name);
        Scoreboard main = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Run r : runs.values()) {
            if (!r.online) continue;
            Player p = Bukkit.getPlayer(r.id);
            if (p == null) continue;
            Scoreboard sb = p.getScoreboard();
            if (sb == main) continue; // never touch the main scoreboard; the core gives each player its own
            Team t = sb.getTeam(GHOST_TEAM);
            if (t == null) {
                t = sb.registerNewTeam(GHOST_TEAM);
                t.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
                t.setCanSeeFriendlyInvisibles(true);
                t.setAllowFriendlyFire(false);
            }
            if (t.getSize() != names.size()) for (String n : names) if (!t.hasEntry(n)) t.addEntry(n);
        }
    }

    private void updateHiding() {
        List<Player> act = new ArrayList<>();
        for (Run r : runs.values()) {
            if (!r.online || r.status != Run.Status.ACTIVE) continue;
            Player p = Bukkit.getPlayer(r.id);
            if (p != null) act.add(p);
        }
        double h2 = s.hideWithin * s.hideWithin, s2 = s.hideShowAt * s.hideShowAt;
        for (Player v : act) {
            Set<UUID> hid = hiddenFrom.computeIfAbsent(v.getUniqueId(), k -> new HashSet<>());
            for (Player t : act) {
                if (t == v || t.getWorld() != v.getWorld()) continue;
                double d = t.getLocation().distanceSquared(v.getLocation());
                if (!hid.contains(t.getUniqueId()) && d < h2) { v.hidePlayer(plugin, t); hid.add(t.getUniqueId()); }
                else if (hid.contains(t.getUniqueId()) && d > s2) { v.showPlayer(plugin, t); hid.remove(t.getUniqueId()); }
            }
        }
    }

    private void unhideAllFor(Player p) {
        Set<UUID> hid = hiddenFrom.remove(p.getUniqueId());
        if (hid != null) for (UUID t : hid) { Player tp = Bukkit.getPlayer(t); if (tp != null) p.showPlayer(plugin, tp); }
        for (Map.Entry<UUID, Set<UUID>> e : hiddenFrom.entrySet()) {
            if (e.getValue().remove(p.getUniqueId())) { Player v = Bukkit.getPlayer(e.getKey()); if (v != null) v.showPlayer(plugin, p); }
        }
    }

    // =====================================================================================
    // join / quit
    // =====================================================================================

    void onQuit(Player p) {
        Run r = run(p);
        if (r == null || !running()) return;
        r.online = false;
        BossBar b = bars.get(r.id);
        if (b != null) b.removePlayer(p);
        unhideAllFor(p);
        resetPlayer(p);
        Bukkit.getScheduler().runTask(plugin, this::checkAllDone);
    }

    @Override
    public boolean onLateJoin(Player p, McmGameContext context) {
        if (!running() || phase == Phase.PREPARING || course.world() == null) return false;
        Run r = run(p);
        if (r != null && r.status == Run.Status.ACTIVE && phase != Phase.ENDING) {
            r.online = true;
            preparePlayer(p, r);
            Pos at = phase == Phase.COUNTDOWN ? prog.startSpawn(r.spawnIndex) : respawnFor(r);
            teleport(p, course.loc(at));
            r.noFallUntilTick = tick + 20;
            p.sendMessage(ChatColor.GREEN + "Welcome back! You resume at your last checkpoint.");
            return true;
        }
        if (r != null) r.online = true;
        p.getInventory().clear();
        p.setGameMode(GameMode.SPECTATOR);
        teleport(p, course.overview(0));
        BossBar b = bars.computeIfAbsent(p.getUniqueId(), k -> Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SEGMENTED_10));
        b.addPlayer(p);
        p.sendMessage(ChatColor.YELLOW + "Parkour Warrior is in progress - you are spectating.");
        sendOverviewLinks(p);
        return true;
    }

    // =====================================================================================
    // end
    // =====================================================================================

    void endGame(String reason) {
        if (!inPlay() && phase != Phase.COUNTDOWN) return;
        phase = Phase.ENDING;
        finishReason = reason;
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        announceResults();
        List<Player> online = new ArrayList<>();
        for (Run r : runs.values()) { Player p = Bukkit.getPlayer(r.id); if (p != null && r.online) online.add(p); }
        for (Player p : online) {
            Run r = run(p);
            if (r != null && r.status == Run.Status.ACTIVE) { p.getInventory().clear(); p.setGameMode(GameMode.SPECTATOR); }
        }
        McmFx.title(online, ChatColor.GOLD + "" + ChatColor.BOLD + "GAME OVER", ChatColor.YELLOW + reason, 5, 50, 15);
        McmFx.sound(online, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        List<Player> finishers = new ArrayList<>();
        for (Run r : runs.values()) if (r.status == Run.Status.FINISHED) { Player p = Bukkit.getPlayer(r.id); if (p != null) finishers.add(p); }
        boolean fast = fastTest();
        if (!finishers.isEmpty() && !fast) McmFx.celebrate(plugin, finishers);
        final String why = reason;
        final int ses = session;
        long delay = fast ? 20L : Math.max(1, s.endDelaySeconds) * 20L;
        endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> { if (ses == session) { endTask = null; doFinish(why); } }, delay);
    }

    private String finishReason = "Parkour Warrior over";

    private void doFinish() { doFinish(finishReason); }

    private void doFinish(String reason) {
        if (finished) return;
        finished = true;
        McmGameContext c = ctx;
        cleanup();
        try { c.finish(reason); } catch (Throwable t) { plugin.getLogger().log(Level.SEVERE, "Parkour Warrior: finish failed", t); }
    }

    /** Used when the game cannot start at all. */
    private void endNow(String reason) {
        if (finished) return;
        finished = true;
        McmGameContext c = ctx;
        cleanup();
        Bukkit.getScheduler().runTask(plugin, () -> c.finish(reason));
    }

    private void announceResults() {
        List<Run> sorted = new ArrayList<>(runs.values());
        sorted.sort(Comparator.comparingInt((Run r) -> r.coins).reversed());
        ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Parkour Warrior results");
        for (Run r : sorted) ctx.broadcast(" " + McmFx.sideColor(ctx, r.sideKey) + r.name + ChatColor.GRAY + ": " + ChatColor.YELLOW + r.coins + " coins"
                + (r.ending != null ? ChatColor.GRAY + " (" + CourseWorld.cap(r.ending.name()) + " ending)" : ""));
    }

    // =====================================================================================
    // rules text / admin
    // =====================================================================================

    private void sendRules(Collection<Player> ps) {
        if (linearCourse()) {
            CourseModel.Linear L = course.model().linear;
            for (Player p : ps) {
                p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Parkour Warrior");
                p.sendMessage(ChatColor.WHITE + "Run the whole course: " + ChatColor.GOLD + "each of the " + (L.sectionCount() - 1) + " checkpoints pays coins" + ChatColor.WHITE
                        + " and sets your spawn point. Fall and you return to the last one. Reach the end (" + L.sections.get(L.sectionCount() - 1) + ") for a finish bonus.");
                p.sendMessage(ChatColor.WHITE + mmss(s.time) + " + " + s.overtime + " s overtime (a fall in overtime ends your run).");
            }
            return;
        }
        for (Player p : ps) {
            p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Parkour Warrior");
            p.sendMessage(ChatColor.WHITE + "Run the main line through 5 zones: each challenge pays " + ChatColor.GOLD + "coins" + ChatColor.WHITE + " straight away and is a checkpoint.");
            p.sendMessage(ChatColor.WHITE + "Each hub has bonus paths that pay more: " + ChatColor.GOLD + "bronze" + ChatColor.WHITE + ", " + ChatColor.GRAY + "silver"
                    + ChatColor.WHITE + ", " + ChatColor.YELLOW + "gold" + ChatColor.WHITE + ". Your scroll takes you back to the main line.");
            p.sendMessage(ChatColor.WHITE + "At the end pick " + ChatColor.GREEN + "Easy" + ChatColor.WHITE + ", " + ChatColor.GOLD + "Medium" + ChatColor.WHITE + " or "
                    + ChatColor.RED + "Hard" + ChatColor.WHITE + " for a finish bonus. " + mmss(s.time) + " + " + s.overtime + " s overtime (a fall in overtime ends your run).");
        }
    }

    /** Admin: mark an obstacle completed for a player as if they stepped on its pad (works mid-game). */
    boolean adminComplete(Player target, String obstacleId, StringBuilder msg) {
        if (!inPlay()) { msg.append("No game running (or not started yet)."); return false; }
        Run r = run(target);
        if (r == null || r.status != Run.Status.ACTIVE) { msg.append(target.getName()).append(" is not an active runner."); return false; }
        Obstacle o = course.model().obstacle(obstacleId);
        if (o == null) { msg.append("Unknown obstacle ").append(obstacleId); return false; }
        if (linearCourse()) {
            int k = (o.stage() - 1) * course.model().linear.perStage + o.index();
            if (k >= course.model().linear.sectionCount()) finishLinear(target, r);
            else completeLinear(target, r, k);
        } else complete(target, r, o);
        if (r.status == Run.Status.ACTIVE && !r.returning) {
            teleport(target, course.loc(respawnFor(r)));
            r.noFallUntilTick = tick + 10;
        }
        msg.append("Completed ").append(o.id()).append(" for ").append(target.getName());
        return true;
    }

    /** Admin: teleport to an obstacle's entry; mid-game the runner's checkpoint becomes that entry's node (no medals). */
    void adminTpObstacle(Player p, Obstacle o) {
        Run r = run(p);
        if (inPlay() && r != null && r.status == Run.Status.ACTIVE && linearCourse()) {
            r.cp = Math.max(0, (o.stage() - 1) * course.model().linear.perStage + o.index() - 1);   // its checkpoint = the one before it
            r.noFallUntilTick = tick + 20;
            giveItems(p, r);
        } else if (inPlay() && r != null && r.status == Run.Status.ACTIVE) {
            Node n = nodeBefore(o);
            r.node = n;
            if (o.kind() != Kind.BONUS) r.mainNode = n;
            else if (prog.stageOf(r.mainNode) != o.stage()) r.mainNode = Node.hub(o.stage());
            r.returning = false;
            r.noFallUntilTick = tick + 20;
            giveItems(p, r);
        }
        teleport(p, course.loc(o.entry()));
    }

    /** The checkpoint node from which {@code o} is attempted. */
    Node nodeBefore(Obstacle o) {
        if (o.index() <= 1) {
            if (o.kind() == Kind.ENDING) return Node.FORK;
            return Node.hub(o.stage());
        }
        String prev = switch (o.kind()) {
            case MAIN -> CourseModel.mainId(o.stage(), o.index() - 1);
            case BONUS -> CourseModel.bonusId(o.stage(), o.index() - 1);
            case ENDING -> CourseModel.endingId(o.ending(), o.index() - 1);
        };
        return Node.after(prev);
    }

    String info() {
        StringBuilder b = new StringBuilder();
        b.append("phase=").append(phase).append(" course=").append(course.state()).append(" generator=").append(course.generatorUsed());
        b.append(" map=").append(course.mapId()).append(forcedMap != null ? " (forced " + forcedMap + ")" : " (random of " + course.availableMaps(s != null ? s : Settings.load(plugin.getConfig(), plugin.getLogger())) + ")");
        if (inPlay()) b.append(" left=").append(secondsLeft()).append("s scrollRemoved=").append(scrollRemoved).append(" switchLocked=").append(switchLocked);
        b.append(" runs=").append(runs.size()).append(" ghost=").append(ghost() ? "on" : "off");
        return b.toString();
    }
}
