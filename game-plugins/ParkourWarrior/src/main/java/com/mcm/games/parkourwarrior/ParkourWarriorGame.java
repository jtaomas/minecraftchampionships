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
    @Override public String description() { return "One shared course: medals, bonus paths, three endings and a team multiplier."; }
    @Override public boolean useCoreScoreboard() { return true; }

    boolean running() { return phase != Phase.IDLE; }
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
        course.prepare(s, false, null);
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
        course.prepare(s, false, () -> { if (mySession == session && phase == Phase.PREPARING) begin(); });
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
                ChatColor.YELLOW + "Medals, bonus paths and three endings");
        sendRules(all);
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
        p.setCollidable(false);
        if (s.translucent) p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, PotionEffect.INFINITE_DURATION, 0, false, false, false));
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

    void giveItems(Player p, Run r) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setItem(0, medalItem(Material.STONE_BUTTON, r.stone, "§fStone Medals", "stone", "§7Main challenges completed"));
        inv.setItem(1, medalItem(Material.COPPER_INGOT, r.bronze, "§6Bronze Medals", "bronze", "§7First bonus challenge of a stage"));
        inv.setItem(2, medalItem(Material.IRON_INGOT, r.silver, "§7Silver Medals", "silver", "§7Second bonus challenge of a stage"));
        inv.setItem(3, medalItem(Material.GOLD_INGOT, r.gold, "§eGold Medals", "gold", "§7Third bonus challenge of a stage"));
        if (r.status == Run.Status.ACTIVE && inPlay()) {
            if (!scrollRemoved) inv.setItem(7, item(Material.PAPER, 1, "§dTeleportation Scroll §7(right-click)", "scroll",
                    "§7Leave a bonus path and return to", "§7the main line. Medals are kept."));
            if (!switchLocked && prog != null && prog.isEndingPhase(r.mainNode))
                inv.setItem(8, item(Material.COMPASS, 1, "§bReturn to the Fork §7(right-click)", "fork",
                        "§7Switch to a different ending.", "§7Allowed until " + s.switchLockAt + " s remain."));
        }
        inv.setHeldItemSlot(4);
    }

    private ItemStack medalItem(Material m, int n, String name, String id, String lore) {
        if (n <= 0) return item(Material.LIGHT_GRAY_STAINED_GLASS_PANE, 1, name + " §8x0", id, lore);
        return item(m, n, name + " §8x" + n, id, lore);
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
        p.sendMessage(ChatColor.LIGHT_PURPLE + "Whoosh! " + ChatColor.WHITE + "Back on the main line. Your medals are kept.");
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
            teleport(p, course.loc(prog.respawn(r.node)));
            return;
        }
        if (p.getGameMode() != GameMode.ADVENTURE) p.setGameMode(GameMode.ADVENTURE);
        double x = course.lx(l), y = course.ly(l), z = course.lz(l);
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
        // falls
        if (tick >= r.noFallUntilTick && fell(r, x, y, z)) onFall(p, r, x, z);
    }

    private boolean fell(Run r, double x, double y, double z) {
        CourseModel m = course.model();
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
        r.falls++;
        if (phase == Phase.OVERTIME) { knockOut(p, r); return; }
        Pos target = prog.respawn(r.node);
        for (Obstacle o : prog.candidates(r.node)) {
            if (o.area() != null && o.area().containsColumn(x, z)) { target = o.entry(); break; }
        }
        teleport(p, course.loc(target));
        r.noFallUntilTick = tick + 5;
        p.sendMessage(ChatColor.RED + "You fell! Back to your last checkpoint.");
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
                            + ChatColor.GREEN + "Easy (+" + fmtMult(s.multInc.get(Difficulty.EASY)) + ")" + ChatColor.WHITE + ", "
                            + ChatColor.GOLD + "Medium (+" + fmtMult(s.multInc.get(Difficulty.MEDIUM)) + ")" + ChatColor.WHITE + " or "
                            + ChatColor.RED + "Hard (+" + fmtMult(s.multInc.get(Difficulty.HARD)) + ")" + ChatColor.WHITE + " team multiplier.");
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
        Scoring.Medal medal;
        if (o.kind() == Kind.MAIN) { medal = Scoring.Medal.STONE; r.stone++; }
        else if (o.kind() == Kind.BONUS) {
            if (o.index() == 1) { medal = Scoring.Medal.BRONZE; r.bronze++; }
            else if (o.index() == 2) { medal = Scoring.Medal.SILVER; r.silver++; }
            else { medal = Scoring.Medal.GOLD; r.gold++; }
        } else return;
        int count = switch (medal) { case STONE -> r.stone; case BRONZE -> r.bronze; case SILVER -> r.silver; case GOLD -> r.gold; };
        int pts = Scoring.delta(s, medal, count);
        r.medalPoints += pts;
        String mname = switch (medal) {
            case STONE -> ChatColor.GRAY + "stone";
            case BRONZE -> ChatColor.GOLD + "bronze";
            case SILVER -> ChatColor.WHITE + "silver";
            case GOLD -> ChatColor.YELLOW + "gold";
        };
        p.sendMessage(ChatColor.WHITE + "You completed " + ChatColor.YELLOW + o.name() + ChatColor.WHITE + " and earned a " + mname
                + ChatColor.WHITE + " medal! " + ChatColor.DARK_GRAY + "(+" + pts + ")");
        if (pts > 0) ctx.addPoints(r.id, pts, medal.name().toLowerCase(Locale.ROOT) + " medal: " + o.name());
        McmFx.sound(p, medal == Scoring.Medal.STONE ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.ENTITY_PLAYER_LEVELUP, 0.8f,
                medal == Scoring.Medal.GOLD ? 1.6f : 1.2f);
        giveItems(p, r);
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
        double mult = Scoring.multiplier(s, sideEndings.get(r.sideKey));
        p.sendMessage(ChatColor.WHITE + "Team multiplier is now " + ChatColor.AQUA + "x" + fmt2(mult));
        McmFx.title(p, c + "" + ChatColor.BOLD + "FINISHED!", ChatColor.WHITE + CourseWorld.cap(d.name()) + " ending  " + ChatColor.AQUA + "+" + fmtMult(s.multInc.get(d)), 5, 50, 15);
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
        sorted.sort(Comparator.comparingInt(Run::medals).reversed().thenComparing(Comparator.comparingInt((Run r) -> r.medalPoints).reversed()));
        List<String> lines = new ArrayList<>();
        lines.add(ChatColor.GOLD + "" + ChatColor.BOLD + "MEDALS OBTAINED");
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Run r = sorted.get(i);
            lines.add(ChatColor.YELLOW + "" + (i + 1) + ". " + McmFx.sideColor(ctx, r.sideKey) + trim(r.name, 12) + ChatColor.GRAY + " " + r.medals()
                    + (r.status == Run.Status.FINISHED ? ChatColor.GREEN + " ✔" : r.status == Run.Status.OUT ? ChatColor.RED + " ✖" : ""));
        }
        String best = null;
        double bm = 1.0;
        for (Map.Entry<String, List<Difficulty>> e : sideEndings.entrySet()) {
            double mm = Scoring.multiplier(s, e.getValue());
            if (mm > bm + 1e-9) { bm = mm; best = e.getKey(); }
        }
        lines.add(ChatColor.AQUA + "Top multiplier " + ChatColor.WHITE + "x" + fmt2(bm) + (best != null ? " " + McmFx.sideColor(ctx, best) + "●" : ""));
        long fin = runs.values().stream().filter(r -> r.status == Run.Status.FINISHED).count();
        lines.add(ChatColor.GRAY + "Finished " + ChatColor.WHITE + fin + ChatColor.GRAY + "/" + ChatColor.WHITE + runs.size());
        ctx.setSidebarLines(lines);

        int left = phase == Phase.COUNTDOWN ? s.time : secondsLeft();
        boolean ot = phase == Phase.OVERTIME;
        int show = ot ? Math.max(0, s.overtime + left) : Math.max(0, left);
        String title = ot ? ChatColor.RED + "" + ChatColor.BOLD + "OVERTIME " + ChatColor.WHITE + mmss(show)
                : ChatColor.WHITE + "" + ChatColor.BOLD + "TIME LEFT " + ChatColor.YELLOW + mmss(show);
        double progress = ot ? show / (double) Math.max(1, s.overtime) : show / (double) Math.max(1, s.time);
        int mainTotal = prog == null ? 15 : prog.mainCount;
        long now = System.currentTimeMillis();
        for (Run r : runs.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p == null || !r.online) continue;
            BossBar b = bars.computeIfAbsent(r.id, k -> Bukkit.createBossBar("", BarColor.WHITE, BarStyle.SEGMENTED_10));
            if (!b.getPlayers().contains(p)) b.addPlayer(p);
            b.setTitle(title + ChatColor.DARK_GRAY + "  |  " + medalRow(r));
            b.setColor(ot ? BarColor.RED : BarColor.WHITE);
            b.setProgress(Math.max(0, Math.min(1, progress)));
            if (r.status == Run.Status.ACTIVE && inPlay() && now - r.lastMessageMs > 2500) {
                int mainDone = 0;
                for (String id : r.completed) if (id.startsWith("M")) mainDone++;
                double mult = Scoring.multiplier(s, sideEndings.getOrDefault(r.sideKey, List.of()));
                McmFx.actionBar(p, ChatColor.GRAY + "Main challenges left " + ChatColor.WHITE + (mainTotal - mainDone) + "/" + mainTotal
                        + ChatColor.DARK_GRAY + "  |  " + ChatColor.AQUA + "Team multiplier x" + fmt2(mult));
            }
        }
        for (Map.Entry<UUID, BossBar> e : bars.entrySet()) {
            if (runs.containsKey(e.getKey())) continue;
            e.getValue().setTitle(title);
            e.getValue().setColor(ot ? BarColor.RED : BarColor.WHITE);
            e.getValue().setProgress(Math.max(0, Math.min(1, progress)));
        }
    }

    private String medalRow(Run r) {
        return ChatColor.GRAY + "■" + r.stone + " " + ChatColor.GOLD + "■" + r.bronze + " " + ChatColor.WHITE + "■" + r.silver + " "
                + ChatColor.YELLOW + "■" + r.gold;
    }

    static String mmss(int sec) { return (sec / 60) + ":" + (sec % 60 < 10 ? "0" : "") + (sec % 60); }
    static String fmt2(double d) { return String.format(Locale.ROOT, "%.2f", d); }
    static String fmtMult(Double d) { return String.format(Locale.ROOT, "%.2f", d == null ? 0 : d); }
    static String trim(String s, int n) { return s.length() <= n ? s : s.substring(0, n); }

    // =====================================================================================
    // collision / visibility
    // =====================================================================================

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
            Pos at = phase == Phase.COUNTDOWN ? prog.startSpawn(r.spawnIndex) : prog.respawn(r.node);
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
        awardMultiplierShares();
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

    private void awardMultiplierShares() {
        Map<String, List<Scoring.Member>> bySide = new LinkedHashMap<>();
        for (Run r : runs.values()) bySide.computeIfAbsent(r.sideKey, k -> new ArrayList<>()).add(new Scoring.Member(r.id, r.medalPoints, r.ending));
        List<String> summary = new ArrayList<>();
        for (Map.Entry<String, List<Scoring.Member>> e : bySide.entrySet()) {
            double mult = Scoring.multiplier(s, sideEndings.getOrDefault(e.getKey(), List.of()));
            int sum = 0;
            for (Scoring.Member m : e.getValue()) sum += m.medalPoints();
            Map<UUID, Integer> sh = Scoring.shares(s, e.getValue(), mult);
            int extra = 0;
            for (Map.Entry<UUID, Integer> x : sh.entrySet()) {
                if (x.getValue() > 0) { ctx.addPoints(x.getKey(), x.getValue(), "team multiplier share x" + fmt2(mult)); extra += x.getValue(); }
            }
            summary.add(McmFx.sideLabel(ctx, e.getKey()) + ChatColor.GRAY + ": " + ChatColor.WHITE + sum + ChatColor.GRAY + " medal pts x"
                    + ChatColor.AQUA + fmt2(mult) + ChatColor.GRAY + " = " + ChatColor.YELLOW + (sum + extra));
        }
        ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "Parkour Warrior results");
        summary.sort(Comparator.reverseOrder());
        for (String l : summary) ctx.broadcast(" " + l);
    }

    // =====================================================================================
    // rules text / admin
    // =====================================================================================

    private void sendRules(Collection<Player> ps) {
        for (Player p : ps) {
            p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Parkour Warrior");
            p.sendMessage(ChatColor.WHITE + "Run the main line through 5 zones: each challenge is a " + ChatColor.GRAY + "stone medal" + ChatColor.WHITE + " and a checkpoint.");
            p.sendMessage(ChatColor.WHITE + "Each hub has a harder bonus path: " + ChatColor.GOLD + "bronze" + ChatColor.WHITE + ", " + ChatColor.GRAY + "silver"
                    + ChatColor.WHITE + ", " + ChatColor.YELLOW + "gold" + ChatColor.WHITE + ". Your scroll takes you back to the main line.");
            p.sendMessage(ChatColor.WHITE + "At the end pick " + ChatColor.GREEN + "Easy" + ChatColor.WHITE + ", " + ChatColor.GOLD + "Medium" + ChatColor.WHITE + " or "
                    + ChatColor.RED + "Hard" + ChatColor.WHITE + " to raise your team multiplier. " + mmss(s.time) + " + " + s.overtime + " s overtime (a fall in overtime ends your run).");
        }
    }

    /** Admin: mark an obstacle completed for a player as if they stepped on its pad (works mid-game). */
    boolean adminComplete(Player target, String obstacleId, StringBuilder msg) {
        if (!inPlay()) { msg.append("No game running (or not started yet)."); return false; }
        Run r = run(target);
        if (r == null || r.status != Run.Status.ACTIVE) { msg.append(target.getName()).append(" is not an active runner."); return false; }
        Obstacle o = course.model().obstacle(obstacleId);
        if (o == null) { msg.append("Unknown obstacle ").append(obstacleId); return false; }
        complete(target, r, o);
        if (r.status == Run.Status.ACTIVE && !r.returning) {
            teleport(target, course.loc(prog.respawn(r.node)));
            r.noFallUntilTick = tick + 10;
        }
        msg.append("Completed ").append(o.id()).append(" for ").append(target.getName());
        return true;
    }

    /** Admin: teleport to an obstacle's entry; mid-game the runner's checkpoint becomes that entry's node (no medals). */
    void adminTpObstacle(Player p, Obstacle o) {
        Run r = run(p);
        if (inPlay() && r != null && r.status == Run.Status.ACTIVE) {
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
        if (inPlay()) b.append(" left=").append(secondsLeft()).append("s scrollRemoved=").append(scrollRemoved).append(" switchLocked=").append(switchLocked);
        b.append(" runs=").append(runs.size());
        return b.toString();
    }
}
