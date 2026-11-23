package com.mcm.games.sandsoftime;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.games.sandsoftime.Layout.Room;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class SandsOfTimeGame implements McmGame, Listener {
    // Tuning
    static final int START_TIME = 120;     // seconds on each hourglass at the start
    static final int MAX_TIME = 150;       // hourglass capacity
    static final int SAND_SECONDS = 10;    // per sand block
    static final int GRACE = 20;           // seconds before the hourglasses start draining
    static final int HARD_CAP = 480;       // whole game, seconds
    static final int COUNTDOWN = 5;
    static final int AUTO_REVIVE = 20;     // downed players free themselves after this many seconds
    static final double BANK_FEE = 0.20;   // Sphinx fee
    static final double POINTS_PER_COIN = 0.12;
    static final int ESCAPE_BONUS = 15;
    static final int[] SIDE_BONUS = {30, 20, 10};

    static final int HOLD_X = -60, HOLD_Y = 120, HOLD_Z = -60;

    enum Status { ACTIVE, DOWNED, ESCAPED, TRAPPED, QUIT }

    enum Phase { BUILDING, COUNTDOWN, PLAYING, ENDING }

    static final class PS {
        final UUID id;
        final String name;
        final String side;
        Status status = Status.ACTIVE;
        int carried, banked, awarded;
        int downedAt;

        PS(UUID id, String name, String side) { this.id = id; this.name = name; this.side = side; }

        boolean inside() { return status == Status.ACTIVE || status == Status.DOWNED; }
    }

    private final SandsOfTimePlugin plugin;
    private final Random rnd = new Random();
    private McmGameContext ctx;
    private World world;
    private boolean running;
    private int gen;
    private Phase phase;
    private int countdown, playSec;
    private final List<BukkitTask> tasks = new ArrayList<>();
    private final Map<UUID, PS> states = new LinkedHashMap<>();
    private final List<Dungeon> dungeons = new ArrayList<>();
    private final Map<String, Dungeon> bySide = new HashMap<>();
    private final Map<String, List<UUID>> members = new LinkedHashMap<>();
    private final List<Entity> spawned = new ArrayList<>();

    public SandsOfTimeGame(SandsOfTimePlugin plugin) {
        this.plugin = plugin;
    }

    @Override public String id() { return "sandsoftime"; }
    @Override public String displayName() { return "Sands of Time"; }
    @Override public Material icon() { return Material.SAND; }
    @Override public String description() { return "Raid a desert dungeon for gold and get out before your hourglass runs dry."; }
    @Override public boolean useCoreScoreboard() { return true; }

    // ================================================================== lifecycle

    @Override
    public void start(McmGameContext ctx) {
        if (running) stop();
        World w = plugin.world();
        if (w == null) {
            ctx.finish("Sands of Time world could not be loaded");
            return;
        }
        this.ctx = ctx;
        this.world = w;
        running = true;
        gen++;
        phase = Phase.BUILDING;
        playSec = 0;
        states.clear();
        dungeons.clear();
        bySide.clear();
        members.clear();
        spawned.clear();

        Layout layout = new Layout(new Random());
        int k = 0;
        for (Map.Entry<String, List<Player>> e : ctx.sides().entrySet()) {
            List<UUID> ids = new ArrayList<>();
            for (Player p : e.getValue()) {
                if (p == null || !p.isOnline()) continue;
                ids.add(p.getUniqueId());
                states.put(p.getUniqueId(), new PS(p.getUniqueId(), p.getName(), e.getKey()));
            }
            if (ids.isEmpty()) continue;
            Dungeon d = new Dungeon(k++, e.getKey(), layout, w);
            d.time = START_TIME;
            dungeons.add(d);
            bySide.put(e.getKey(), d);
            members.put(e.getKey(), ids);
        }
        if (dungeons.isEmpty()) {
            running = false;
            ctx.finish("No players");
            return;
        }

        buildHolding(true);
        Location hold = new Location(w, HOLD_X + 0.5, HOLD_Y + 1, HOLD_Z + 0.5);
        for (Player p : new ArrayList<>(ctx.participants())) {
            resetPlayer(p, GameMode.ADVENTURE);
            p.teleport(hold);
        }
        ctx.broadcast("§6§lSands of Time §7— the dungeons are being excavated...");
        ctx.setTimer(HARD_CAP, HARD_CAP);
        ctx.setSidebarLines(List.of("§7Generating dungeons..."));

        final int g = gen;
        for (Dungeon d : dungeons) plugin.enqueue(() -> { if (g == gen && running) d.build(plugin); });
        plugin.enqueue(() -> { if (g == gen && running) begin(); });
    }

    @Override
    public void stop() {
        if (!running) return;
        cleanup();
    }

    private void begin() {
        phase = Phase.COUNTDOWN;
        countdown = FastTest.secs(COUNTDOWN);
        for (Dungeon d : dungeons) {
            d.spawnEntities(this);
            d.setHubBarriers(true);
            d.updateHourglass(MAX_TIME);
        }
        for (Map.Entry<String, List<UUID>> e : members.entrySet()) {
            Dungeon d = bySide.get(e.getKey());
            for (UUID id : e.getValue()) {
                Player p = Bukkit.getPlayer(id);
                PS s = states.get(id);
                if (p == null || !p.isOnline()) { if (s != null) s.status = Status.QUIT; continue; }
                resetPlayer(p, GameMode.SURVIVAL);
                giveKit(p, e.getKey());
                p.teleport(d.hubSpawn());
            }
        }
        buildHolding(false);
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::fastTick, 2L, 2L));
        tasks.add(Bukkit.getScheduler().runTaskTimer(plugin, this::slowTick, 20L, 20L));

        ctx.broadcast("§6§lSands of Time");
        ctx.broadcast("§7Grab §6gold§7 in your team's dungeon. Bank it at the §6Sphinx§7 (20% fee) or walk out the §aEXIT§7 to keep all of it.");
        ctx.broadcast("§7Your hourglass drains constantly — feed it §eSand§7 (+10s each). If it runs dry while you're inside, you're §ctrapped§7 and lose what you carry.");
        ctx.broadcast("§7Keys open the §cRed§7, §aGreen§7 and §6Gold§7 vaults. Watch out for pressure plates, crumbling floors and husks!");
        hud();
    }

    private void cleanup() {
        running = false;
        gen++;
        phase = null;
        for (BukkitTask t : tasks) t.cancel();
        tasks.clear();
        for (Entity e : spawned) if (e.isValid()) e.remove();
        spawned.clear();
        for (Dungeon d : dungeons) for (List<Husk> l : d.husks.values()) for (Husk h : l) if (h.isValid()) h.remove();
        if (world != null) {
            for (Player p : new ArrayList<>(world.getPlayers())) {
                if (!states.containsKey(p.getUniqueId())) continue;
                resetPlayer(p, GameMode.SURVIVAL);
            }
        }
        if (plugin.isEnabled() && world != null) {
            List<Dungeon> old = new ArrayList<>(dungeons);
            for (Dungeon d : old) {
                plugin.enqueue(() -> { d.purgeEntities(); d.clearPart(0); });
                plugin.enqueue(() -> { d.clearPart(1); d.removeTickets(plugin); });
            }
            plugin.enqueue(() -> buildHolding(false));
        }
        dungeons.clear();
        bySide.clear();
        members.clear();
        states.clear();
    }

    private void endGame(String reason) {
        if (phase == Phase.ENDING || !running) return;
        phase = Phase.ENDING;
        // Placement bonus by banked coins.
        List<Dungeon> order = new ArrayList<>(dungeons);
        order.sort(Comparator.comparingInt((Dungeon d) -> -sideBanked(d)));
        ctx.broadcast("§6§lSands of Time §7— " + reason);
        for (int n = 0; n < order.size(); n++) {
            Dungeon d = order.get(n);
            int banked = sideBanked(d);
            String line = "§e#" + (n + 1) + " " + ctx.sideLabel(d.sideKey) + " §7— §6" + banked + " coins banked";
            if (order.size() > 1 && n < SIDE_BONUS.length && banked > 0) {
                ctx.addSidePoints(d.sideKey, SIDE_BONUS[n], "Sands of Time placement #" + (n + 1));
                line += " §a(+" + SIDE_BONUS[n] + " team bonus)";
            }
            if (n < 8) ctx.broadcast(line);
        }
        hud();
        final int g = gen;
        endReason = reason;
        tasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (g != gen || !running) return;
            cleanup();
            ctx.finish(reason);
        }, FastTest.ticks(80L)));
    }

    // ================================================================== admin testing hooks
    // (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    private String endReason = "Sands of Time over";

    /** Ends the start countdown (barriers drop now) or the results wait. */
    public boolean skipWait() {
        if (!running || ctx == null) return false;
        if (phase == Phase.COUNTDOWN) {
            countdown = 0;
            slowTick();   // the normal GO transition
            return true;
        }
        if (phase == Phase.ENDING) {
            cleanup();    // bumps gen, so the pending finish task does nothing
            ctx.finish(endReason);
            return true;
        }
        return false;
    }

    /** A single round: nothing to skip to. */
    public boolean skipRound() { return false; }

    // ================================================================== ticking

    private void slowTick() {
        if (!running) return;
        if (phase == Phase.COUNTDOWN) {
            if (countdown > 0) {
                for (Player p : onlinePlayers()) {
                    p.showTitle(Title.title(Items.legacy("§6" + countdown), Items.legacy("§7Get ready to raid the dungeon"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(100))));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                }
                countdown--;
            } else {
                phase = Phase.PLAYING;
                playSec = 0;
                for (Dungeon d : dungeons) { d.setHubBarriers(false); d.started = true; }
                for (Player p : onlinePlayers()) {
                    p.showTitle(Title.title(Items.legacy("§6§lGO!"), Items.legacy("§7The hourglass starts draining in " + GRACE + "s"),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(300))));
                    p.playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.6f);
                }
            }
            ctx.setTimer(HARD_CAP, HARD_CAP);
            hud();
            return;
        }
        if (phase != Phase.PLAYING) return;

        playSec++;
        int left = HARD_CAP - playSec;
        ctx.setTimer(Math.max(0, left), HARD_CAP);
        boolean draining = playSec > GRACE;
        if (playSec == GRACE) {
            for (Player p : onlinePlayers()) {
                p.sendActionBar(Items.legacy("§e⌛ The hourglasses have begun to drain!"));
                p.playSound(p.getLocation(), Sound.BLOCK_BELL_USE, 1f, 0.6f);
            }
        }

        for (Dungeon d : dungeons) {
            if (d.done) continue;
            if (insideCount(d) == 0) {
                d.done = true;
                ctx.broadcast(ctx.sideLabel(d.sideKey) + " §7has left the dungeon with §6" + sideBanked(d) + " coins§7.");
                continue;
            }
            if (draining && d.time > 0) d.time--;
            d.updateHourglass(MAX_TIME);
            if (draining) tickSound(d);
            if (d.time <= 0) runOut(d, false);
        }

        // Downed players free themselves after a while (also covers solo players).
        for (PS s : new ArrayList<>(states.values())) {
            if (s.status != Status.DOWNED || playSec - s.downedAt < AUTO_REVIVE) continue;
            Player p = Bukkit.getPlayer(s.id);
            Dungeon d = bySide.get(s.side);
            if (p != null && d != null) revive(p, s, d, null);
        }

        // Red key: released once the guardian husks are dead.
        for (Dungeon d : dungeons) {
            if (d.redReleased || d.L.mobKeyI < 0) continue;
            int idx = d.L.mobKeyI * Layout.H + d.L.mobKeyJ;
            if (!d.triggered[idx]) continue;
            List<Husk> hs = d.husks.getOrDefault(idx, List.of());
            boolean alive = false;
            for (Husk h : hs) if (h.isValid() && !h.isDead()) { alive = true; break; }
            if (!alive) releaseRedKey(d);
        }

        if (left == 60 || left == 30 || left == 10) {
            for (Player p : onlinePlayers()) {
                PS s = states.get(p.getUniqueId());
                if (s != null && s.inside()) {
                    p.showTitle(Title.title(Items.legacy("§c" + left + "s"), Items.legacy("§7until the whole dungeon seals shut!"),
                            Title.Times.times(Duration.ZERO, Duration.ofSeconds(2), Duration.ofMillis(300))));
                }
            }
        }
        if (left <= 0) {
            for (Dungeon d : dungeons) if (!d.done) runOut(d, true);
            hud();
            endGame("The dungeons have sealed shut!");
            return;
        }
        boolean allDone = true;
        for (Dungeon d : dungeons) if (!d.done) { allDone = false; break; }
        hud();
        if (allDone) endGame("Every team is out of the dungeon.");
    }

    private void tickSound(Dungeon d) {
        int t = d.time;
        if (t <= 0) return;
        boolean tick = t % 10 == 0 || t <= 10;
        if (!tick) return;
        float pitch = t <= 10 ? 1.4f : (t <= 30 ? 0.5f : 0.8f);
        for (UUID id : members.getOrDefault(d.sideKey, List.of())) {
            PS s = states.get(id);
            Player p = Bukkit.getPlayer(id);
            if (s == null || p == null || !s.inside()) continue;
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.2f, pitch);
            if (t <= 10) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 0.8f);
            if (t == 30 || t == 10) {
                p.showTitle(Title.title(Items.legacy("§e⌛ " + t + "s"), Items.legacy("§7left on your hourglass!"),
                        Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(300))));
            }
        }
    }

    private void fastTick() {
        if (!running || (phase != Phase.PLAYING && phase != Phase.COUNTDOWN)) return;
        for (PS s : new ArrayList<>(states.values())) {
            Player p = Bukkit.getPlayer(s.id);
            if (p == null || !p.isOnline() || p.getWorld() != world) continue;
            Dungeon d = bySide.get(s.side);
            if (d == null) continue;
            if (s.status == Status.DOWNED) {
                if (p.getLocation().distanceSquared(d.cage()) > 6) p.teleport(d.cage());
                continue;
            }
            if (s.status != Status.ACTIVE || phase != Phase.PLAYING) continue;
            Location l = p.getLocation();
            if (l.getY() < d.oy - 6) { down(p, s, d, "fell into the depths"); continue; }
            int bx = l.getBlockX(), bz = l.getBlockZ();
            double fy = l.getY() - d.oy;
            if (fy < 1.6 && d.exitPad.contains(Dungeon.key(bx, d.oy, bz))) { escape(p, s, d); continue; }
            if (fy >= 0.9 && fy < 1.4) checkCrumble(d, Dungeon.key(bx, d.oy, bz));
            int[] cell = d.cellOf(l);
            if (cell != null) onEnterRoom(p, d, cell[0], cell[1]);
            for (Entity e : p.getNearbyEntities(1.2, 1.3, 1.2)) {
                if (e instanceof Item it && it.isValid()) pickup(p, s, d, it);
            }
        }
    }

    // ================================================================== mechanics

    private void checkCrumble(Dungeon d, long k) {
        if (!d.crumble.contains(k) || d.crumbling.contains(k)) return;
        d.crumbling.add(k);
        Location at = keyLoc(k);
        world.playSound(at, Sound.BLOCK_SAND_BREAK, 1f, 0.6f);
        later(9, () -> {
            d.setFloor(k, Material.AIR, true);
            world.playSound(at, Sound.BLOCK_GRAVEL_BREAK, 1f, 0.5f);
        });
        later(9 + 120, () -> {
            d.setFloor(k, Material.SMOOTH_SANDSTONE, false);
            d.crumbling.remove(k);
        });
    }

    private Location keyLoc(long k) {
        int x = (int) (k >> 38), z = (int) ((k << 26) >> 38), y = (int) (k & 0xFFF);
        return new Location(world, x + 0.5, y + 0.5, z + 0.5);
    }

    private void onEnterRoom(Player p, Dungeon d, int i, int j) {
        Room r = d.roomAt(i, j);
        if (r != Room.MOB && r != Room.MOB_KEY) return;
        int idx = i * Layout.H + j;
        if (d.triggered[idx]) return;
        d.triggered[idx] = true;
        int sideSize = members.getOrDefault(d.sideKey, List.of()).size();
        int depth = d.L.depth[i][j];
        int count = r == Room.MOB_KEY ? 3 + depth / 4 + (sideSize >= 3 ? 1 : 0) : 2 + depth / 3 + (sideSize >= 3 ? 1 : 0);
        count = Math.min(5, count);
        double[][] spots = {{3.5, 6.5}, {9.5, 6.5}, {6.5, 3.5}, {6.5, 9.5}, {4.5, 9.5}};
        List<Husk> list = new ArrayList<>();
        for (int n = 0; n < count; n++) {
            double[] s = spots[n % spots.length];
            Location loc = d.roomPoint(i, j, s[0], 1.0, s[1]);
            Husk h = world.spawn(loc, Husk.class, hk -> {
                hk.setPersistent(false);
                hk.setRemoveWhenFarAway(false);
                hk.setCanPickupItems(false);
                hk.setAdult();
                if (hk.getEquipment() != null) hk.getEquipment().clear();
            });
            list.add(h);
            spawned.add(h);
            world.spawnParticle(org.bukkit.Particle.CLOUD, loc.clone().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
        }
        d.husks.put(idx, list);
        world.playSound(p.getLocation(), Sound.ENTITY_HUSK_AMBIENT, 1.2f, 0.7f);
        p.showTitle(Title.title(Items.legacy(""), Items.legacy(r == Room.MOB_KEY ? "§cThe key's guardians awaken!" : "§cHusks rise from the sand!"),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(300))));
    }

    private void releaseRedKey(Dungeon d) {
        d.redReleased = true;
        d.removeMobKeyBars();
        Location loc = d.mobKeyPedestal();
        if (d.L.vaults.contains(KeyColor.RED)) {
            d.keySpawn.put(KeyColor.RED, loc.clone());
            spawnItem(loc, Items.key(KeyColor.RED));
            sideMessage(d, "§7The guardians are slain — the " + KeyColor.RED.keyName() + " §7is free!");
        } else {
            spawnItem(loc, Items.coin(Material.GOLD_BLOCK, 30, 1));
            sideMessage(d, "§7The guardians are slain — their treasure is free!");
        }
        world.playSound(loc, Sound.BLOCK_IRON_DOOR_OPEN, 1f, 0.8f);
    }

    private void pickup(Player p, PS s, Dungeon d, Item it) {
        ItemStack st = it.getItemStack();
        String kind = Items.kind(st);
        if (kind == null) return;
        switch (kind) {
            case Items.COIN -> {
                int val = Items.value(st) * st.getAmount();
                s.carried += val;
                it.remove();
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7f, 1.3f);
                p.sendActionBar(Items.legacy("§6+" + val + " coins §7(carrying §6" + s.carried + "§7)"));
            }
            case Items.SAND -> {
                giveOrDrop(p, Items.sand(st.getAmount()));
                it.remove();
                p.playSound(p.getLocation(), Sound.BLOCK_SAND_PLACE, 1f, 1.2f);
                p.sendActionBar(Items.legacy("§e+" + st.getAmount() + " Sand of Time"));
            }
            case Items.KEY -> {
                KeyColor c = Items.color(st);
                if (c == null) { it.remove(); return; }
                giveOrDrop(p, Items.key(c));
                it.remove();
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                sideMessage(d, "§f" + p.getName() + " §7found the " + c.keyName() + "§7!");
            }
            case Items.GEAR -> {
                equip(p, st.getType());
                it.remove();
                p.playSound(p.getLocation(), Sound.ITEM_ARMOR_EQUIP_IRON, 1f, 1f);
            }
            default -> { }
        }
    }

    private void giveOrDrop(Player p, ItemStack st) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(st);
        for (ItemStack rest : left.values()) spawnItem(p.getLocation(), rest);
    }

    private void equip(Player p, Material m) {
        PlayerInventory inv = p.getInventory();
        ItemStack st = Items.gear(m);
        switch (m) {
            case IRON_HELMET -> { if (inv.getHelmet() == null || inv.getHelmet().getType().isAir()) { inv.setHelmet(st); return; } }
            case CHAINMAIL_CHESTPLATE -> { inv.setChestplate(st); return; }
            case IRON_BOOTS -> { if (inv.getBoots() == null || inv.getBoots().getType().isAir()) { inv.setBoots(st); return; } }
            case SHIELD -> { if (inv.getItemInOffHand().getType().isAir()) { inv.setItemInOffHand(st); return; } }
            case IRON_SWORD -> {
                for (int n = 0; n < 9; n++) {
                    ItemStack cur = inv.getItem(n);
                    if (cur != null && cur.getType() == Material.STONE_SWORD) { inv.setItem(n, st); return; }
                }
            }
            default -> { }
        }
        giveOrDrop(p, st);
    }

    private void depositSand(Player p, Dungeon d) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (!Items.SAND.equals(Items.kind(hand))) {
            p.sendActionBar(Items.legacy("§7Hold §eSand of Time §7to refill the hourglass. §e⌛ " + fmt(d.time)));
            return;
        }
        int need = (int) Math.ceil((MAX_TIME - d.time) / (double) SAND_SECONDS);
        int use = Math.min(hand.getAmount(), need);
        if (use <= 0) {
            p.sendActionBar(Items.legacy("§eThe hourglass is full §7(" + fmt(d.time) + ")"));
            return;
        }
        hand.setAmount(hand.getAmount() - use);
        p.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
        d.time = Math.min(MAX_TIME, d.time + use * SAND_SECONDS);
        d.updateHourglass(MAX_TIME);
        world.playSound(p.getLocation(), Sound.BLOCK_SAND_PLACE, 1f, 0.8f);
        world.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        sideMessage(d, "§f" + p.getName() + " §7added §e+" + (use * SAND_SECONDS) + "s §7to the hourglass §8(§e" + fmt(d.time) + "§8)");
    }

    private void bank(Player p, PS s, Dungeon d) {
        if (s.carried <= 0) {
            p.sendActionBar(Items.legacy("§7The Sphinx wants gold. You carry none. §8(banked " + s.banked + ")"));
            return;
        }
        int fee = (int) Math.round(s.carried * BANK_FEE);
        int add = s.carried - fee;
        s.banked += add;
        s.carried = 0;
        syncPoints(s);
        world.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 0.8f);
        p.sendMessage("§6The Sphinx accepts your offering: §e+" + add + " coins banked §7(fee " + fee + "). Total banked: §6" + s.banked);
    }

    private void shrine(Player p, Dungeon d) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        PS target = null;
        for (UUID id : members.getOrDefault(d.sideKey, List.of())) {
            PS t = states.get(id);
            if (t != null && t.status == Status.DOWNED && Bukkit.getPlayer(id) != null && (target == null || t.downedAt < target.downedAt)) target = t;
        }
        if (target == null) {
            p.sendActionBar(Items.legacy("§7Nobody on your team needs reviving."));
            return;
        }
        if (!Items.SAND.equals(Items.kind(hand))) {
            p.sendActionBar(Items.legacy("§7Offer §e1 Sand of Time §7to revive §f" + target.name));
            return;
        }
        hand.setAmount(hand.getAmount() - 1);
        p.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
        Player tp = Bukkit.getPlayer(target.id);
        if (tp != null) revive(tp, target, d, p.getName());
    }

    private void tryOpen(Player p, Dungeon d, KeyColor door) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        KeyColor held = Items.KEY.equals(Items.kind(hand)) ? Items.color(hand) : null;
        if (held != door) {
            p.sendActionBar(Items.legacy("§7This is the " + door.vaultName() + "§7. It needs the " + door.keyName() + "§7."));
            world.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_CLOSE, 0.6f, 1.4f);
            return;
        }
        hand.setAmount(hand.getAmount() - 1);
        p.getInventory().setItemInMainHand(hand.getAmount() <= 0 ? null : hand);
        d.openDoor(door);
        world.playSound(p.getLocation(), Sound.BLOCK_IRON_DOOR_OPEN, 1f, 0.6f);
        world.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.8f);
        for (UUID id : members.getOrDefault(d.sideKey, List.of())) {
            Player m = Bukkit.getPlayer(id);
            if (m == null) continue;
            m.showTitle(Title.title(Items.legacy(""), Items.legacy(door.vaultName() + " §7opened by §f" + p.getName()),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(300))));
        }
    }

    private void fireTrap(Player p, Dungeon d, long plateKey) {
        Integer room = d.plateRoom.get(plateKey);
        if (room == null) return;
        int now = Bukkit.getCurrentTick();
        Integer last = d.plateCooldown.get(plateKey);
        if (last != null && now - last < 30) return;
        d.plateCooldown.put(plateKey, now);
        List<Location> disp = new ArrayList<>(d.dispensers.getOrDefault(room, List.of()));
        Collections.shuffle(disp, rnd);
        Location eye = p.getEyeLocation();
        for (int n = 0; n < Math.min(3, disp.size()); n++) {
            Location from = disp.get(n);
            Vector dir = eye.toVector().subtract(from.toVector());
            if (dir.lengthSquared() < 0.01) continue;
            Arrow a = world.spawnArrow(from, dir.normalize(), 1.5f, 4f);
            a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
            a.setDamage(1.2);
            a.setPersistent(false);
            spawned.add(a);
            world.playSound(from, Sound.BLOCK_DISPENSER_LAUNCH, 1f, 1f);
        }
    }

    // ================================================================== player state changes

    private void down(Player p, PS s, Dungeon d, String why) {
        int lost = s.carried;
        s.carried = 0;
        s.status = Status.DOWNED;
        s.downedAt = playSec;
        stripGameItems(p, d);
        healFully(p);
        p.setGameMode(GameMode.ADVENTURE);
        p.teleport(d.cage());
        p.showTitle(Title.title(Items.legacy("§cYou fell!"), Items.legacy("§7Lost §6" + lost + " coins§7. Free in " + AUTO_REVIVE + "s, or a teammate offers sand"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))));
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT, 1f, 0.6f);
        sideMessage(d, "§f" + p.getName() + " §c" + why + " §7and dropped §6" + lost + " coins§7. Offer sand at the §dRevive Shrine§7!");
    }

    private void revive(Player p, PS s, Dungeon d, String by) {
        s.status = Status.ACTIVE;
        healFully(p);
        p.setGameMode(GameMode.SURVIVAL);
        p.teleport(d.hubSpawn());
        p.showTitle(Title.title(Items.legacy("§aRevived"), Items.legacy(by == null ? "§7The sands release you" : "§7Thanks to §f" + by),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(300))));
        p.playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 0.6f, 1.4f);
        if (by != null) sideMessage(d, "§f" + by + " §7revived §f" + p.getName() + "§7.");
    }

    private void escape(Player p, PS s, Dungeon d) {
        int carried = s.carried;
        s.banked += carried;
        s.carried = 0;
        s.status = Status.ESCAPED;
        syncPoints(s);
        ctx.addPoints(s.id, ESCAPE_BONUS, "Escaped the dungeon");
        stripGameItems(p, d);
        resetPlayer(p, GameMode.SPECTATOR);
        p.teleport(d.viewPos());
        p.showTitle(Title.title(Items.legacy("§aEscaped!"), Items.legacy("§7Kept §6" + carried + " coins §7— §6" + s.banked + " §7banked in total"),
                Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))));
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1.2f);
        ctx.broadcast(ctx.teamColor(ctx.teamOf(s.id)) + p.getName() + " §7escaped with §6" + s.banked + " coins §7banked.");
    }

    private void trap(Player p, PS s, Dungeon d, boolean collapse) {
        int lost = s.carried;
        s.carried = 0;
        s.status = Status.TRAPPED;
        if (p != null) {
            stripGameItems(p, d);
            resetPlayer(p, GameMode.SPECTATOR);
            p.teleport(d.viewPos());
            p.showTitle(Title.title(Items.legacy("§4Trapped!"), Items.legacy("§7" + (collapse ? "The dungeon sealed" : "Your hourglass ran dry") + " — lost §6" + lost + " coins"),
                    Title.Times.times(Duration.ZERO, Duration.ofSeconds(3), Duration.ofMillis(500))));
            p.playSound(p.getLocation(), Sound.BLOCK_END_PORTAL_SPAWN, 0.6f, 0.6f);
        }
    }

    private void runOut(Dungeon d, boolean collapse) {
        d.done = true;
        d.ranOut = true;
        d.time = 0;
        d.updateHourglass(MAX_TIME);
        int n = 0;
        for (UUID id : members.getOrDefault(d.sideKey, List.of())) {
            PS s = states.get(id);
            if (s == null || !s.inside()) continue;
            Player p = Bukkit.getPlayer(id);
            if (p == null) { s.status = Status.QUIT; s.carried = 0; continue; }
            trap(p, s, d, collapse);
            n++;
        }
        ctx.broadcast(ctx.sideLabel(d.sideKey) + (collapse ? " §7was sealed in" : "§7's hourglass ran dry") + (n > 0 ? " §c— " + n + " trapped!" : "."));
    }

    /** Removes keys (returned to their spawn spot) and sand from a player. Gear and markers are kept. */
    private void stripGameItems(Player p, Dungeon d) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getContents();
        for (int n = 0; n < contents.length; n++) {
            ItemStack st = contents[n];
            String kind = Items.kind(st);
            if (kind == null) continue;
            if (Items.KEY.equals(kind)) {
                KeyColor c = Items.color(st);
                if (c != null && d != null && !d.opened.contains(c)) {
                    Location at = d.keySpawn.get(c);
                    if (at != null) {
                        spawnItem(at, Items.key(c));
                        sideMessage(d, "§7The " + c.keyName() + " §7returned to where it was found.");
                    }
                }
                inv.setItem(n, null);
            } else if (Items.SAND.equals(kind)) {
                inv.setItem(n, null);
            }
        }
    }

    private void syncPoints(PS s) {
        int target = (int) Math.floor(s.banked * POINTS_PER_COIN);
        if (target > s.awarded) {
            ctx.addPoints(s.id, target - s.awarded, "Banked treasure");
            s.awarded = target;
        }
    }

    // ================================================================== HUD

    private void hud() {
        if (ctx == null) return;
        List<Dungeon> order = new ArrayList<>(dungeons);
        order.sort(Comparator.comparingInt((Dungeon d) -> -sideBanked(d)));
        List<String> lines = new ArrayList<>();
        int max = order.size() > 6 ? 5 : 6;
        for (int n = 0; n < Math.min(max, order.size()); n++) {
            Dungeon d = order.get(n);
            String status;
            if (d.done) status = d.ranOut ? "§4SEALED" : "§aOUT";
            else if (phase != Phase.PLAYING || playSec <= GRACE) status = "§e⌛" + fmt(d.time) + "§7*";
            else status = (d.time <= 30 ? "§c" : "§e") + "⌛" + fmt(d.time);
            lines.add(ctx.sideLabel(d.sideKey) + " " + status + " §6" + sideBanked(d) + "c");
        }
        if (order.size() > max) lines.add("§7+" + (order.size() - max) + " more sides");
        ctx.setSidebarLines(lines);

        for (PS s : states.values()) {
            Player p = Bukkit.getPlayer(s.id);
            if (p == null || !p.isOnline()) continue;
            Dungeon d = bySide.get(s.side);
            if (d == null) continue;
            String bar = switch (s.status) {
                case ACTIVE -> "§e⌛ " + fmt(d.time) + (playSec <= GRACE ? " §7(paused)" : "") + "   §6Carrying " + s.carried
                        + "   §eBanked " + s.banked + "   §7Sand " + countSand(p);
                case DOWNED -> "§cFallen §7— free in " + Math.max(0, AUTO_REVIVE - (playSec - s.downedAt)) + "s §8| §e⌛ " + fmt(d.time);
                case ESCAPED -> "§aEscaped §7— §6" + s.banked + " coins banked";
                case TRAPPED -> "§4Trapped §7— §6" + s.banked + " coins banked";
                default -> "";
            };
            if (phase == Phase.ENDING) bar = "§6" + s.banked + " coins banked";
            p.sendActionBar(Items.legacy(bar));
        }
    }

    private static int countSand(Player p) {
        int n = 0;
        for (ItemStack st : p.getInventory().getContents()) if (Items.SAND.equals(Items.kind(st))) n += st.getAmount();
        return n;
    }

    private static String fmt(int sec) {
        sec = Math.max(0, sec);
        return (sec / 60) + ":" + String.format("%02d", sec % 60);
    }

    // ================================================================== helpers

    private int sideBanked(Dungeon d) {
        int sum = 0;
        for (UUID id : members.getOrDefault(d.sideKey, List.of())) {
            PS s = states.get(id);
            if (s != null) sum += s.banked;
        }
        return sum;
    }

    private int insideCount(Dungeon d) {
        int n = 0;
        for (UUID id : members.getOrDefault(d.sideKey, List.of())) {
            PS s = states.get(id);
            if (s != null && s.inside() && Bukkit.getPlayer(id) != null) n++;
        }
        return n;
    }

    private List<Player> onlinePlayers() {
        List<Player> out = new ArrayList<>();
        for (PS s : states.values()) {
            Player p = Bukkit.getPlayer(s.id);
            if (p != null && p.isOnline()) out.add(p);
        }
        return out;
    }

    private void sideMessage(Dungeon d, String msg) {
        for (UUID id : members.getOrDefault(d.sideKey, List.of())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(msg);
        }
    }

    private void later(long ticks, Runnable r) {
        final int g = gen;
        tasks.add(Bukkit.getScheduler().runTaskLater(plugin, () -> { if (g == gen && running) r.run(); }, ticks));
    }

    void spawnItem(Location loc, ItemStack st) {
        Item it = world.dropItem(loc, st, i -> {
            i.setVelocity(new Vector(0, 0, 0));
            i.setPickupDelay(32767);
            i.setUnlimitedLifetime(true);
            i.setCanMobPickup(false);
            i.setPersistent(false);
            i.setInvulnerable(true);
        });
        spawned.add(it);
    }

    void spawnText(Location loc, String text) {
        TextDisplay td = world.spawn(loc, TextDisplay.class, t -> {
            t.text(Items.legacy(text));
            t.setBillboard(Display.Billboard.CENTER);
            t.setPersistent(false);
            t.setShadowed(true);
        });
        spawned.add(td);
    }

    private static void healFully(Player p) {
        AttributeInstance a = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(Math.max(1.0, a == null ? 20.0 : a.getValue()));
        p.setFireTicks(0);
        p.setFallDistance(0);
        p.setFoodLevel(20);
        p.setSaturation(5f);
        for (PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(pe.getType());
    }

    private static void resetPlayer(Player p, GameMode mode) {
        p.getInventory().clear();
        p.setItemOnCursor(null);
        healFully(p);
        p.setGameMode(mode);
    }

    private void giveKit(Player p, String sideKey) {
        DyeColor dye = dyeFor(sideKey.startsWith("TEAM:") ? sideKey.substring(5) : null);
        PlayerInventory inv = p.getInventory();
        inv.setItem(0, Items.kit(Material.STONE_SWORD, 1, "§fKhopesh"));
        Material carpet = Material.matchMaterial(dye.name() + "_CARPET");
        if (carpet == null) carpet = Material.WHITE_CARPET;
        inv.setItem(8, Items.kit(carpet, 24, "§fTrail Marker §7(place to mark your path)"));
        Color c = dye.getColor();
        inv.setChestplate(Items.kitArmor(Material.LEATHER_CHESTPLATE, c, "§fExplorer's Tunic"));
        inv.setHelmet(Items.kitArmor(Material.LEATHER_HELMET, c, "§fExplorer's Hood"));
    }

    private static DyeColor dyeFor(String team) {
        if (team == null) return DyeColor.WHITE;
        return switch (team.toLowerCase()) {
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

    /** Small sandstone waiting platform used while the dungeons are generated. */
    private void buildHolding(boolean build) {
        if (world == null) return;
        for (int x = HOLD_X - 5; x <= HOLD_X + 5; x++)
            for (int z = HOLD_Z - 5; z <= HOLD_Z + 5; z++) {
                boolean edge = x == HOLD_X - 5 || x == HOLD_X + 5 || z == HOLD_Z - 5 || z == HOLD_Z + 5;
                world.getBlockAt(x, HOLD_Y, z).setType(build ? (edge ? Material.CHISELED_SANDSTONE : Material.SMOOTH_SANDSTONE) : Material.AIR, false);
                for (int y = 1; y <= 2; y++)
                    world.getBlockAt(x, HOLD_Y + y, z).setType(build && edge ? Material.GLASS : Material.AIR, false);
            }
    }

    private boolean inGameWorld(World w) {
        return running && world != null && w == world;
    }

    private PS stateOf(Player p) {
        return states.get(p.getUniqueId());
    }

    // ================================================================== listeners

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!inGameWorld(e.getEntity().getWorld())) return;
        if (!(e.getEntity() instanceof Player p)) {
            if (e.getEntity() instanceof Item || e.getEntity() instanceof TextDisplay) e.setCancelled(true);
            return;
        }
        PS s = stateOf(p);
        if (s == null || s.status != Status.ACTIVE || phase != Phase.PLAYING) { e.setCancelled(true); return; }
        if (e instanceof EntityDamageByEntityEvent ed) {
            Entity src = ed.getDamager();
            if (src instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) src = shooter;
            if (src instanceof Player) { e.setCancelled(true); return; }
        }
        if (p.getHealth() - e.getFinalDamage() <= 0) {
            e.setCancelled(true);
            Dungeon d = bySide.get(s.side);
            if (d == null) return;
            String why = switch (e.getCause()) {
                case LAVA, FIRE, FIRE_TICK -> "fell into the lava";
                case PROJECTILE -> "was struck by a dart trap";
                case ENTITY_ATTACK -> "was overwhelmed by husks";
                default -> "fell";
            };
            down(p, s, d, why);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!inGameWorld(p.getWorld())) return;
        PS s = stateOf(p);
        if (s == null) return;
        e.getDrops().clear();
        e.setKeepInventory(true);
        e.setDroppedExp(0);
        if (s.status == Status.ACTIVE) {
            Dungeon d = bySide.get(s.side);
            s.carried = 0;
            s.status = Status.DOWNED;
            s.downedAt = playSec;
            stripGameItems(p, d);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!running) return;
        PS s = stateOf(p);
        Dungeon d = s == null ? null : bySide.get(s.side);
        if (d == null) return;
        e.setRespawnLocation(s.status == Status.DOWNED ? d.cage() : (s.status == Status.ACTIVE ? d.hubSpawn() : d.viewPos()));
        final GameMode mode = s.status == Status.DOWNED ? GameMode.ADVENTURE : (s.status == Status.ACTIVE ? GameMode.SURVIVAL : GameMode.SPECTATOR);
        later(1, () -> { if (p.isOnline()) p.setGameMode(mode); });
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDeathOther(EntityDeathEvent e) {
        if (e instanceof PlayerDeathEvent) return;
        if (!inGameWorld(e.getEntity().getWorld())) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent e) {
        if (inGameWorld(e.getEntity().getWorld())) {
            e.setCancelled(true);
            e.getEntity().setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        if (!inGameWorld(e.getBlock().getWorld())) return;
        PS s = stateOf(e.getPlayer());
        boolean carpet = Tag.WOOL_CARPETS.isTagged(e.getBlockPlaced().getType());
        boolean ok = carpet && s != null && s.status == Status.ACTIVE && phase == Phase.PLAYING
                && e.getBlockReplacedState().getType().isAir();
        if (!ok) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        if (!inGameWorld(e.getBlock().getWorld())) return;
        if (Tag.WOOL_CARPETS.isTagged(e.getBlock().getType())) {
            e.setDropItems(false);
            e.setExpToDrop(0);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!inGameWorld(p.getWorld())) return;
        Block b = e.getClickedBlock();
        PS s = stateOf(p);
        if (e.getAction() == Action.PHYSICAL) {
            if (b != null && b.getType() == Material.STONE_PRESSURE_PLATE && s != null && s.status == Status.ACTIVE && phase == Phase.PLAYING) {
                Dungeon d = bySide.get(s.side);
                if (d != null) fireTrap(p, d, Dungeon.key(b.getX(), b.getY(), b.getZ()));
            } else if (b != null && b.getType() != Material.STONE_PRESSURE_PLATE) {
                e.setCancelled(true);
            }
            return;
        }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || b == null) return;
        ItemStack item = e.getItem();
        boolean carpet = item != null && Tag.WOOL_CARPETS.isTagged(item.getType());
        if (e.getHand() != EquipmentSlot.HAND) {
            if (!carpet) e.setCancelled(true);
            return;
        }
        if (s == null || s.status != Status.ACTIVE || phase != Phase.PLAYING) { e.setCancelled(true); return; }
        Dungeon d = bySide.get(s.side);
        if (d == null) { e.setCancelled(true); return; }
        long k = Dungeon.key(b.getX(), b.getY(), b.getZ());
        if (d.hourglass.contains(k)) { e.setCancelled(true); depositSand(p, d); return; }
        if (d.sphinx.contains(k)) { e.setCancelled(true); bank(p, s, d); return; }
        if (d.shrine.contains(k)) { e.setCancelled(true); shrine(p, d); return; }
        KeyColor door = d.doorBlocks.get(k);
        if (door != null) { e.setCancelled(true); tryOpen(p, d, door); return; }
        if (!carpet) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent e) {
        if (inGameWorld(e.getEntity().getWorld())) e.setCancelled(true); // pickups are handled in fastTick
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        if (inGameWorld(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFlow(BlockFromToEvent e) {
        if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChangeBlock(EntityChangeBlockEvent e) {
        if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!inGameWorld(e.getEntity().getWorld())) return;
        if (e.getEntity() instanceof Arrow a) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> { if (a.isValid()) a.remove(); }, 2L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (!running) return;
        Player p = e.getPlayer();
        PS s = stateOf(p);
        if (s == null || !s.inside()) return;
        Dungeon d = bySide.get(s.side);
        if (p.getWorld() == world) stripGameItems(p, d);
        p.getInventory().clear();
        s.carried = 0;
        s.status = Status.QUIT;
        if (d != null) sideMessage(d, "§f" + p.getName() + " §7left the game.");
    }

}
