package com.mcm.games.bingo;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bingo but Fast (shared card, tier points scaled by completion order, line bonuses) and, with {@code lockout = true},
 * Lockout Bingo (first side to obtain an item claims its square, tier points).
 *
 * <p>Flow: start → PREPARING (async chunk loads for the shared start and the structure sites, then structures placed
 * one per tick) → card generated from what this area really has → COUNTDOWN → PLAYING.
 */
public final class BingoGame implements McmGame, Listener {

    // ---------------------------------------------------------------- fixed tuning
    private static final int BINGO_SECONDS = 600;
    private static final int LOCKOUT_SECONDS = 720;
    private static final int COUNTDOWN_SECONDS = 5;
    private static final int START_CHUNK_RADIUS = 3;

    private enum Phase { IDLE, PREPARING, COUNTDOWN, PLAYING }

    private static final Deque<Set<Material>> RECENT_CARDS = new ArrayDeque<>();

    private final BingoPlugin plugin;
    private final BingoWorld worldMgr;
    private final boolean lockout;
    private final CaveFinder finder;

    // ---------------------------------------------------------------- per-game state
    private McmGameContext ctx;
    private World world;
    private Settings cfg = new Settings();
    private Phase phase = Phase.IDLE;
    private BukkitTask task, placeTask;
    private boolean halfSecond;
    private int countdown;
    private int secondsLeft;
    private int totalSeconds;

    private AreaLayout layout;
    private Env env;
    private final List<AreaLayout.Site> placed = new ArrayList<>();
    private List<BingoItem> card = List.of();
    private final Map<String, boolean[]> got = new HashMap<>();
    private final Map<String, Set<Integer>> linesDone = new HashMap<>();
    private final Map<String, Boolean> fullCard = new HashMap<>();
    private int[] completions = new int[25];
    private String[] claimedBy = new String[25];
    private final Map<UUID, String> sideOf = new HashMap<>();
    private final List<String> sideOrder = new ArrayList<>();
    private final Map<UUID, Long> tpCooldown = new HashMap<>();
    private final Map<UUID, Long> topCooldown = new HashMap<>();
    private final Map<UUID, Long> combatUntil = new HashMap<>();
    private final Map<UUID, Long> finderCooldown = new HashMap<>();
    private final Set<UUID> finderBusy = new HashSet<>();
    /** Centre of this game's area (Y not meaningful until the chunks are loaded) and the shared start point. */
    private Location areaCenter, startSpot;
    /** Spots within a few blocks of the start, handed out round-robin so players don't stack. */
    private final List<Location> startSpots = new ArrayList<>();
    private int spotIdx;
    /** Bumped by every start and cleanup so stale async chunk callbacks do nothing. */
    private int prepGen;
    private BukkitTask prepTimeout, ticketRelease;
    private SpectatorGuard guard;

    public BingoGame(BingoPlugin plugin, BingoWorld worldMgr, boolean lockout) {
        this.plugin = plugin;
        this.worldMgr = worldMgr;
        this.lockout = lockout;
        this.finder = new CaveFinder(plugin);
    }

    // ================================================================ McmGame

    @Override public String id() { return lockout ? "lockout" : "bingo"; }

    @Override public String displayName() { return lockout ? "Lockout Bingo" : "Bingo but Fast"; }

    @Override public Material icon() { return lockout ? Material.IRON_BARS : Material.KNOWLEDGE_BOOK; }

    @Override
    public String description() {
        return lockout
                ? "Survival race: the first side to obtain an item claims its square. Harder items are worth more."
                : "Survival race: collect the 25 items on the shared card, harder items and lines pay more.";
    }

    @Override public boolean useCoreScoreboard() { return true; }

    @Override
    public void start(McmGameContext ctx) {
        if (phase != Phase.IDLE) cleanup(false);
        this.ctx = ctx;
        resetState();
        cfg = plugin.loadSettings();
        if (guard == null) guard = new SpectatorGuard(plugin, this::spectatorZone);

        layout = worldMgr.nextArea();
        world = layout == null ? null : worldMgr.get();
        if (world == null) {
            McmGameContext c = this.ctx;
            this.ctx = null;
            c.broadcast("§c[" + displayName() + "] Could not prepare the world, skipping.");
            c.finish("World generation failed");
            return;
        }

        totalSeconds = lockout ? LOCKOUT_SECONDS : BINGO_SECONDS;
        secondsLeft = totalSeconds;

        for (Map.Entry<String, List<Player>> e : ctx.sides().entrySet()) {
            if (e.getValue() == null || e.getValue().isEmpty()) continue;
            String key = e.getKey();
            sideOrder.add(key);
            for (Player p : e.getValue()) sideOf.put(p.getUniqueId(), key);
        }
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (!sideOf.containsKey(p.getUniqueId())) {
                String key = "SOLO:" + p.getUniqueId();
                sideOrder.add(key);
                sideOf.put(p.getUniqueId(), key);
            }
        }
        for (String s : sideOrder) {
            got.put(s, new boolean[25]);
            linesDone.put(s, new HashSet<>());
            fullCard.put(s, false);
        }

        areaCenter = new Location(world, layout.cx + 0.5, 64, layout.cz + 0.5);
        phase = Phase.PREPARING;
        ctx.setTimer(totalSeconds, totalSeconds);
        ctx.setSidebarLines(List.of("§7Preparing the world..."));
        ctx.broadcast("§6" + displayName() + " §7- preparing a fresh area of the world, one moment...");
        preloadArea(++prepGen);
    }

    // ================================================================ preparing

    /** Chunk keys around a block position. */
    private static Set<Long> chunksAround(int x, int z, int r) {
        Set<Long> out = new LinkedHashSet<>();
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) out.add(chunkKey((x >> 4) + dx, (z >> 4) + dz));
        return out;
    }

    /**
     * Loads (generating as needed) the chunks around the shared start and around every structure site,
     * asynchronously, with a plugin ticket each. Nothing here blocks the main thread; when the last chunk arrives
     * (or after 60 s) the structures are placed one per tick, then {@link #beginAfterPreload} runs.
     */
    private void preloadArea(int gen) {
        final World w = world;
        final long t0 = System.nanoTime();
        Set<Long> keys = new LinkedHashSet<>(chunksAround(layout.cx, layout.cz, START_CHUNK_RADIUS));
        for (AreaLayout.Site s : layout.sites) keys.addAll(chunksAround(s.x(), s.z(), s.chunkRadius()));
        final int[] pending = {keys.size()};
        final boolean[] fired = {false};
        final Runnable fire = () -> {
            if (fired[0]) return;
            fired[0] = true;
            if (prepTimeout != null) { prepTimeout.cancel(); prepTimeout = null; }
            if (cfg.debugTiming) plugin.getLogger().info(String.format("[debug-timing] %s area preload: %d chunks in %.0f ms (%d unanswered)",
                    displayName(), keys.size(), (System.nanoTime() - t0) / 1e6, pending[0]));
            placeStructures(gen);
        };
        for (long k : keys) {
            final int cx = (int) (k >> 32), cz = (int) k;
            w.getChunkAtAsync(cx, cz, true).whenComplete((chunk, err) -> {
                // Paper completes chunk futures on the main thread.
                if (chunk != null && gen == prepGen && phase == Phase.PREPARING) w.addPluginChunkTicket(cx, cz, plugin);
                if (--pending[0] <= 0) fire.run();
            });
        }
        if (keys.isEmpty()) fire.run();
        else if (!fired[0]) prepTimeout = Bukkit.getScheduler().runTaskLater(plugin, fire, 1200L);
    }

    /** Places the planned structures with the vanilla /place command, one per tick, only where all chunks are loaded. */
    private void placeStructures(int gen) {
        if (gen != prepGen || phase != Phase.PREPARING || world == null) return;
        Deque<AreaLayout.Site> queue = new ArrayDeque<>(layout.sites);
        placeTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (gen != prepGen || phase != Phase.PREPARING || world == null) {
                if (placeTask != null) { placeTask.cancel(); placeTask = null; }
                return;
            }
            AreaLayout.Site s = queue.poll();
            if (s == null) {
                if (placeTask != null) { placeTask.cancel(); placeTask = null; }
                beginAfterPreload(gen);
                return;
            }
            if (!allLoaded(s)) {
                plugin.getLogger().warning("Bingo: skipped " + s.kind() + " (chunks not loaded in time)");
                return;
            }
            long t0 = System.nanoTime();
            boolean ok = placeOne(s);
            if (ok) placed.add(s);
            plugin.getLogger().info(String.format("Bingo: %s %s at %d,%d (%s, %.0f ms)", ok ? "placed" : "could not place",
                    s.id(), s.x(), s.z(), s.kind(), (System.nanoTime() - t0) / 1e6));
        }, 1L, 1L);
    }

    private boolean allLoaded(AreaLayout.Site s) {
        for (long k : chunksAround(s.x(), s.z(), s.chunkRadius())) if (!world.isChunkLoaded((int) (k >> 32), (int) k)) return false;
        return true;
    }

    private boolean placeOne(AreaLayout.Site s) {
        String cmd = "execute in " + world.getKey().asString() + " run place structure " + s.id() + " " + s.x() + " 64 " + s.z();
        List<String> keys = new ArrayList<>();
        try {
            CommandSender snd = Bukkit.createCommandSender(c -> collectKeys(c, keys));
            Bukkit.dispatchCommand(snd, cmd);
        } catch (Throwable t) {
            plugin.getLogger().warning("Bingo: /place failed: " + t);
            return false;
        }
        boolean success = false, failure = false;
        for (String k : keys) {
            if (k.startsWith("commands.place.structure.success")) success = true;
            else if (k.startsWith("commands.") || k.startsWith("argument") || k.contains("failed") || k.contains("invalid")) failure = true;
        }
        if (!success && !failure && keys.isEmpty()) {
            plugin.getLogger().warning("Bingo: /place gave no feedback for " + s.id() + "; assuming it worked.");
            return true;
        }
        if (!success) plugin.getLogger().warning("Bingo: /place feedback for " + s.id() + ": " + keys);
        return success;
    }

    private static void collectKeys(Component c, List<String> out) {
        if (c instanceof TranslatableComponent tc) {
            out.add(tc.key());
            for (Component a : tc.args()) collectKeys(a, out);
        }
        for (Component ch : c.children()) collectKeys(ch, out);
    }

    private static long chunkKey(int cx, int cz) { return ((long) cx << 32) | (cz & 0xFFFFFFFFL); }

    private void releaseTickets() {
        if (ticketRelease != null) { ticketRelease.cancel(); ticketRelease = null; }
        World w = Bukkit.getWorld(BingoWorld.NAME);
        if (w != null) w.removePluginChunkTickets(plugin);
    }

    /** The shared start point (surface), for respawns, the card teleport and late joiners. */
    private Location start() {
        return startSpot != null ? startSpot.clone() : (areaCenter != null ? areaCenter.clone() : world.getSpawnLocation());
    }

    /** One of the spread-out spots around the start. */
    private Location nextStartSpot() {
        if (startSpots.isEmpty()) return start();
        Location l = startSpots.get(spotIdx++ % startSpots.size()).clone();
        l.setYaw(ThreadLocalRandom.current().nextFloat() * 360f);
        return l;
    }

    private void beginAfterPreload(int gen) {
        if (gen != prepGen || phase != Phase.PREPARING || ctx == null || world == null) return;
        McmGameContext ctx = this.ctx;
        long t0 = System.nanoTime();
        computeStart();

        Set<String> structures = new LinkedHashSet<>(Env.ALWAYS_STRUCTURES);
        for (AreaLayout.Site s : placed) structures.add(s.kind());
        env = new Env(layout.biomes, structures);
        Random rnd = ThreadLocalRandom.current();
        Set<Material> recent = new HashSet<>();
        for (Set<Material> prev : RECENT_CARDS) recent.addAll(prev);
        card = BingoItem.generateCard(rnd, env, lockout ? cfg.lockoutMix : cfg.bingoMix, cfg.woodCap, recent);
        Set<Material> used = new HashSet<>(25);
        for (BingoItem it : card) used.add(it.icon);
        RECENT_CARDS.addLast(used);
        while (RECENT_CARDS.size() > 2) RECENT_CARDS.removeFirst();
        plugin.getLogger().info("Bingo card env " + env);

        world.setTime(cfg.startTime);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, cfg.daylightCycle);
        world.setGameRule(GameRule.DO_MOB_SPAWNING, cfg.mobSpawning);
        world.setSpawnFlags(cfg.mobSpawning, cfg.mobSpawning);
        for (Player p : new ArrayList<>(ctx.participants())) {
            preparePlayer(p);
            p.teleport(nextStartSpot());
            p.showTitle(Title.title(txt("§6§l" + displayName()),
                    txt(lockout ? "§fFirst to get an item claims it!" : "§fCollect the items on your card!"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(500))));
        }

        ctx.broadcast("§6§l" + displayName() + "§r§7 - " + (lockout
                ? "Get items before anyone else to claim their squares; harder items are worth more."
                : "Collect the 25 items on the card; harder items, early finds and full lines pay more.")
                + " §7Click your card to see it. PvP is on (keep inventory). §f/top§7 takes you to the surface.");

        phase = Phase.COUNTDOWN;
        countdown = FastTest.secs(COUNTDOWN_SECONDS);
        halfSecond = false;
        ctx.setTimer(totalSeconds, totalSeconds);
        updateSidebar();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
        // Players hold their own chunks now; drop ours once everyone has arrived.
        ticketRelease = Bukkit.getScheduler().runTaskLater(plugin, () -> { ticketRelease = null; releaseTickets(); }, 200L);
        if (cfg.debugTiming) plugin.getLogger().info(String.format("[debug-timing] %s card + spawn + teleport tick: %.2f ms",
                displayName(), (System.nanoTime() - t0) / 1e6));
    }

    /** Finds a dry, solid start near the centre and up to 16 spread spots within 3 blocks of it. */
    private void computeStart() {
        startSpots.clear();
        spotIdx = 0;
        int bx = areaCenter.getBlockX(), bz = areaCenter.getBlockZ();
        Location s = null;
        Random r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 60 && s == null; attempt++) {
            int spread = attempt == 0 ? 0 : Math.min(40, 2 + attempt);
            int x = bx + (attempt == 0 ? 0 : r.nextInt(-spread, spread + 1));
            int z = bz + (attempt == 0 ? 0 : r.nextInt(-spread, spread + 1));
            if (!world.isChunkLoaded(x >> 4, z >> 4)) continue;
            if (safeGround(x, z)) s = new Location(world, x + 0.5, world.getHighestBlockYAt(x, z) + 1, z + 0.5);
        }
        if (s == null) s = new Location(world, bx + 0.5, world.getHighestBlockYAt(bx, bz) + 1, bz + 0.5);
        startSpot = s;
        for (int dx = -3; dx <= 3; dx++)
            for (int dz = -3; dz <= 3; dz++) {
                int x = s.getBlockX() + dx, z = s.getBlockZ() + dz;
                if (!safeGround(x, z)) continue;
                int y = world.getHighestBlockYAt(x, z) + 1;
                if (Math.abs(y - s.getBlockY()) > 2) continue;
                startSpots.add(new Location(world, x + 0.5, y, z + 0.5));
            }
        Collections.shuffle(startSpots, r);
        if (startSpots.isEmpty()) startSpots.add(s.clone());
    }

    private boolean safeGround(int x, int z) {
        Block b = world.getHighestBlockAt(x, z);
        Material t = b.getType();
        return t.isSolid() && !t.name().endsWith("_LEAVES") && t != Material.MAGMA_BLOCK && t != Material.CACTUS
                && t != Material.POWDER_SNOW;
    }

    @Override
    public void stop() {
        if (phase == Phase.IDLE && ctx == null) return;
        cleanup(true);
    }

    /** Plugin disable: like stop, but without scheduling anything. */
    public void shutdown() {
        if (phase == Phase.IDLE && ctx == null) return;
        cleanup(false);
    }

    @Override
    public boolean onLateJoin(Player player, McmGameContext ctx) {
        if (phase == Phase.IDLE || phase == Phase.PREPARING || world == null) return false;
        SpectatorGuard.makeSpectator(player, overview());
        player.sendMessage(txt("§7You joined mid-game and are spectating " + displayName() + "."));
        return true;
    }

    /** How far (blocks, horizontally) from the area centre spectators may fly. */
    private static final double WATCH_RADIUS = 320;

    /** Above the start, looking out over it. */
    private Location overview() {
        Location l = start().add(0, 15, 0);
        l.setPitch(35f);
        return l;
    }

    private SpectatorGuard.Zone spectatorZone(Player p) {
        if (!running() || areaCenter == null || !p.getWorld().equals(world)) return null;
        double cx = areaCenter.getX(), cz = areaCenter.getZ();
        return new SpectatorGuard.Zone(world, new org.bukkit.util.BoundingBox(cx - WATCH_RADIUS, world.getMinHeight() - 16,
                cz - WATCH_RADIUS, cx + WATCH_RADIUS, world.getMaxHeight() + 64, cz + WATCH_RADIUS), overview());
    }

    /** A side that completed the whole card has nothing left to collect: its members watch the rest (owner rule). */
    private void finishedSideSpectates(String side) {
        for (Player m : members(side)) {
            SpectatorGuard.makeSpectator(m, overview());
            m.sendMessage(txt("§7Your side completed the card - you are spectating until " + displayName() + " ends."));
        }
    }

    // ================================================================ lifecycle helpers

    private void resetState() {
        got.clear();
        linesDone.clear();
        fullCard.clear();
        completions = new int[25];
        claimedBy = new String[25];
        sideOf.clear();
        sideOrder.clear();
        tpCooldown.clear();
        topCooldown.clear();
        combatUntil.clear();
        finderCooldown.clear();
        finderBusy.clear();
        placed.clear();
        startSpots.clear();
        card = List.of();
        areaCenter = null;
        startSpot = null;
        layout = null;
        env = null;
    }

    private void cleanup(boolean scheduleWorldCleanup) {
        prepGen++;
        if (prepTimeout != null) { prepTimeout.cancel(); prepTimeout = null; }
        if (placeTask != null) { placeTask.cancel(); placeTask = null; }
        releaseTickets();
        if (task != null) { task.cancel(); task = null; }
        phase = Phase.IDLE;
        McmGameContext c = ctx;
        ctx = null;
        List<Player> players = new ArrayList<>();
        if (c != null) players.addAll(c.participants());
        if (world != null) for (Player p : world.getPlayers()) if (!players.contains(p)) players.add(p);
        for (Player p : players) resetPlayer(p);
        world = null;
        if (scheduleWorldCleanup) worldMgr.scheduleCleanup(100L);
        resetState();
    }

    private void endGame(String reason) {
        if (phase == Phase.IDLE || ctx == null) return;
        McmGameContext c = ctx;
        if (task != null) { task.cancel(); task = null; }
        phase = Phase.IDLE;

        List<String> ranking = ranking();
        if (lockout) {
            int place = 0, prevCount = -1, idx = 0;
            for (String s : ranking) {
                int n = count(s);
                idx++;
                if (n != prevCount) { place = idx; prevCount = n; }
                if (n == 0 || place > cfg.lockoutPlacement.length) continue;
                awardSide(c, s, cfg.lockoutPlacement[place - 1], "Lockout " + ordinal(place) + " place");
            }
        }

        c.broadcast("§6§l" + displayName() + " results §7(" + reason + ")");
        int shown = 0;
        for (String s : ranking) {
            if (shown++ >= 5) break;
            c.broadcast("§e" + shown + ". " + c.sideLabel(s) + " §7- §f" + count(s) + " items§7, §e"
                    + linesDone.getOrDefault(s, Set.of()).size() + " lines");
        }

        for (Player p : new ArrayList<>(c.participants())) resetPlayer(p);
        releaseTickets();
        ctx = null;
        world = null;
        resetState();
        worldMgr.scheduleCleanup(200L);
        c.finish(reason);
    }

    private void preparePlayer(Player p) {
        p.closeInventory();
        p.setGameMode(GameMode.SURVIVAL);
        p.getInventory().clear();
        p.setItemOnCursor(null);
        for (PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(pe.getType());
        AttributeInstance max = p.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        p.setHealth(max != null ? max.getValue() : 20.0);
        p.setFoodLevel(20);
        p.setSaturation(5f);
        p.setExhaustion(0f);
        p.setLevel(0);
        p.setExp(0f);
        p.setFireTicks(0);
        p.setFallDistance(0f);
        clearBingoRespawn(p);
        giveKit(p);
        applySpeed(p);
    }

    private void resetPlayer(Player p) {
        if (p == null || !p.isOnline()) return;
        if (p.getOpenInventory().getTopInventory().getHolder() instanceof CardMenu) p.closeInventory();
        p.getInventory().clear();
        p.setItemOnCursor(null);
        for (PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(pe.getType());
        p.setLevel(0);
        p.setExp(0f);
        p.setFireTicks(0);
        clearBingoRespawn(p);
    }

    /** A bed set in an earlier Bingo area must not pull the player out of this one. */
    private static void clearBingoRespawn(Player p) {
        try {
            Location r = p.getRespawnLocation();
            if (r != null && r.getWorld() != null && BingoWorld.NAME.equals(r.getWorld().getName())) p.setRespawnLocation(null);
        } catch (Throwable ignored) { }
    }

    private void applySpeed(Player p) {
        if (!p.hasPotionEffect(PotionEffectType.SPEED)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, false, true));
        }
    }

    private static ItemStack tool(Material m) {
        ItemStack it = new ItemStack(m);
        it.addUnsafeEnchantment(Enchantment.EFFICIENCY, 2);
        ItemMeta meta = it.getItemMeta();
        meta.setUnbreakable(true);
        it.setItemMeta(meta);
        return it;
    }

    private void giveKit(Player p) {
        p.getInventory().setItem(0, tool(Material.IRON_PICKAXE));
        p.getInventory().setItem(1, tool(Material.IRON_AXE));
        p.getInventory().setItem(2, tool(Material.IRON_SHOVEL));
        if (cfg.beef > 0) p.getInventory().setItem(3, new ItemStack(Material.COOKED_BEEF, cfg.beef));
        p.getInventory().setItem(7, finderItem());
        p.getInventory().setItem(8, cardItem());
    }

    // ================================================================ main loop

    // admin testing hooks (/mcm skipwait | skip; no @Override so the jar still loads on an older core)

    /** Ends the start countdown: GO on the next half-second tick. */
    public boolean skipWait() {
        if (ctx == null || phase != Phase.COUNTDOWN) return false;
        countdown = 0;
        halfSecond = true;   // the next tick runs the once-per-second part (the normal GO transition)
        return true;
    }

    /** A single round: nothing to skip to. */
    public boolean skipRound() { return false; }

    private void tick() {
        if (phase == Phase.IDLE || ctx == null) return;
        if (ctx.participants().isEmpty()) { endGame("No players left"); return; }

        if (phase == Phase.PLAYING) poll();
        if (phase == Phase.IDLE) return; // poll may have ended the game

        halfSecond = !halfSecond;
        if (halfSecond) return; // the rest runs once per second

        if (phase == Phase.COUNTDOWN) {
            if (countdown > 0) {
                for (Player p : ctx.participants()) {
                    p.showTitle(Title.title(txt("§e" + countdown), txt("§7Get ready..."),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(900), Duration.ofMillis(100))));
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                }
                countdown--;
            } else {
                phase = Phase.PLAYING;
                for (Player p : ctx.participants()) {
                    p.showTitle(Title.title(txt("§a§lGO!"), txt("§7Click your card to see it"),
                            Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(300))));
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                }
            }
            ctx.setTimer(totalSeconds, totalSeconds);
            updateSidebar();
            return;
        }

        // PLAYING, once per second
        secondsLeft--;
        ctx.setTimer(Math.max(0, secondsLeft), totalSeconds);
        if (world != null && !cfg.daylightCycle) world.setTime(cfg.startTime);
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (!p.isDead() && p.getGameMode() == GameMode.SURVIVAL) applySpeed(p);
            String side = sideOf.get(p.getUniqueId());
            if (side == null || finderBusy.contains(p.getUniqueId())) continue;
            p.sendActionBar(txt("§a" + count(side) + "§7/25 items  §e" + linesDone.get(side).size()
                    + "§7 lines  §8|  §fClick your card"));
        }
        updateSidebar();
        if (secondsLeft == 60 || secondsLeft == 30 || secondsLeft == 10) {
            ctx.broadcast("§e" + secondsLeft + " seconds left in " + displayName() + "!");
        }
        if (secondsLeft <= 0) endGame("Time's up");
    }

    private void poll() {
        boolean changed = false;
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (phase != Phase.PLAYING) return;
            if (!p.isOnline() || p.isDead() || world == null || !p.getWorld().equals(world)) continue;
            String side = sideOf.get(p.getUniqueId());
            if (side == null) continue;

            Set<Material> held = EnumSet.noneOf(Material.class);
            boolean hasCard = false, hasFinder = false;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null || it.getType().isAir()) continue;
                if (isCard(it)) { hasCard = true; continue; }
                if (isFinder(it)) { hasFinder = true; continue; }
                held.add(it.getType());
            }
            ItemStack cursor = p.getItemOnCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (isCard(cursor)) hasCard = true;
                else if (isFinder(cursor)) hasFinder = true;
                else held.add(cursor.getType());
            }
            if (p.getGameMode() == GameMode.SURVIVAL) {
                if (!hasCard) p.getInventory().addItem(cardItem());
                if (!hasFinder) p.getInventory().addItem(finderItem());
            }
            if (held.isEmpty()) continue;

            boolean[] mine = got.get(side);
            for (int i = 0; i < 25 && i < card.size() && phase == Phase.PLAYING; i++) {
                if (mine[i]) continue;
                if (lockout && claimedBy[i] != null) continue;
                if (!card.get(i).matches(held)) continue;
                if (lockout) claim(side, i, p); else complete(side, i, p);
                changed = true;
                if (ctx == null) return;
            }
        }
        if (changed && phase == Phase.PLAYING) {
            refreshMenus();
            updateSidebar();
            checkEnd();
        }
    }

    /** Bingo points for a square of {@code tier} completed {@code place}-th. */
    private int bingoPoints(int tier, int place) {
        int base = cfg.bingoTierPoints[BingoItem.clampTier(tier)];
        int[] pct = cfg.placementPercent;
        int p = pct.length == 0 ? 100 : pct[Math.min(place, pct.length) - 1];
        return Math.max(1, (int) Math.round(base * p / 100.0));
    }

    private int lockoutPoints(int tier) { return cfg.lockoutTierPoints[BingoItem.clampTier(tier)]; }

    private void complete(String side, int cell, Player finder) {
        boolean[] mine = got.get(side);
        mine[cell] = true;
        completions[cell]++;
        int place = completions[cell];
        BingoItem item = card.get(cell);
        int pts = bingoPoints(item.tier, place);
        awardSide(ctx, side, pts, "Bingo: " + item.name);
        for (Player m : members(side)) {
            m.sendMessage(txt("§a✔ " + (m.equals(finder) ? "You" : finder.getName()) + " got " + BingoItem.TIER_COLORS[item.tier]
                    + item.name + " §7(" + ordinal(place) + ", +" + pts + ")"));
            m.playSound(m.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.4f);
        }
        if (place == 1) ctx.broadcast("§7[Bingo] " + ctx.sideLabel(side) + " §7was first to get "
                + BingoItem.TIER_COLORS[item.tier] + item.name + "§7!");
        checkLines(side);
        if (count(side) == 25 && !fullCard.get(side)) {
            fullCard.put(side, true);
            awardSide(ctx, side, cfg.bingoFullCard, "Bingo: full card");
            ctx.broadcast("§6§lFULL CARD! " + ctx.sideLabel(side) + " §6completed the entire card!");
            for (Player m : members(side)) {
                m.showTitle(Title.title(txt("§6§lFULL CARD!"), txt("§f+" + cfg.bingoFullCard + " points"),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(2500), Duration.ofMillis(500))));
                m.playSound(m.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
            finishedSideSpectates(side);
        }
    }

    private void claim(String side, int cell, Player finder) {
        got.get(side)[cell] = true;
        claimedBy[cell] = side;
        BingoItem item = card.get(cell);
        int pts = lockoutPoints(item.tier);
        awardSide(ctx, side, pts, "Lockout: " + item.name);
        ctx.broadcast("§7[Lockout] " + ctx.sideLabel(side) + " §7claimed " + BingoItem.TIER_COLORS[item.tier] + item.name
                + " §7(" + finder.getName() + ", +" + pts + ")");
        for (Player p : ctx.participants()) {
            boolean mineSide = side.equals(sideOf.get(p.getUniqueId()));
            p.playSound(p.getLocation(), mineSide ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.BLOCK_NOTE_BLOCK_BASS,
                    1f, mineSide ? 1.4f : 0.8f);
        }
        checkLines(side);
    }

    private void checkLines(String side) {
        boolean[] mine = got.get(side);
        Set<Integer> done = linesDone.get(side);
        for (int li = 0; li < BingoItem.LINES.length; li++) {
            if (done.contains(li)) continue;
            boolean all = true;
            for (int c : BingoItem.LINES[li]) if (!mine[c]) { all = false; break; }
            if (!all) continue;
            done.add(li);
            int pts = lockout ? cfg.lockoutLine : (done.size() <= 4 ? cfg.bingoLineFirst4 : cfg.bingoLineLater);
            awardSide(ctx, side, pts, "Bingo line");
            ctx.broadcast("§6§lBINGO! " + ctx.sideLabel(side) + " §6completed " + BingoItem.lineName(li)
                    + " §7(" + done.size() + (done.size() == 1 ? " line" : " lines") + ")");
            for (Player m : members(side)) {
                m.showTitle(Title.title(txt("§6§lBINGO!"), txt("§f" + BingoItem.lineName(li) + " §7+" + pts),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(1800), Duration.ofMillis(400))));
                m.playSound(m.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1.2f);
            }
        }
    }

    private void checkEnd() {
        if (phase != Phase.PLAYING || ctx == null) return;
        if (lockout) {
            for (String s : claimedBy) if (s == null) return;
            endGame("Every square claimed");
            return;
        }
        Set<String> active = new HashSet<>();
        for (Player p : ctx.participants()) {
            String s = sideOf.get(p.getUniqueId());
            if (s != null) active.add(s);
        }
        if (active.isEmpty()) return;
        for (String s : active) if (!fullCard.getOrDefault(s, false)) return;
        endGame("Every side completed the card");
    }

    // ================================================================ scoring / HUD helpers

    private void awardSide(McmGameContext c, String side, int pts, String reason) {
        if (c == null || pts <= 0) return;
        for (Player p : new ArrayList<>(c.participants())) {
            if (side.equals(sideOf.get(p.getUniqueId()))) c.addPoints(p.getUniqueId(), pts, reason);
        }
    }

    private List<Player> members(String side) {
        List<Player> out = new ArrayList<>();
        if (ctx == null) return out;
        for (Player p : ctx.participants()) if (side.equals(sideOf.get(p.getUniqueId()))) out.add(p);
        return out;
    }

    private int count(String side) {
        boolean[] g = got.get(side);
        if (g == null) return 0;
        int n = 0;
        for (boolean b : g) if (b) n++;
        return n;
    }

    private List<String> ranking() {
        List<String> r = new ArrayList<>(sideOrder);
        r.sort(Comparator.<String>comparingInt(this::count).reversed()
                .thenComparing(Comparator.<String>comparingInt(s -> linesDone.getOrDefault(s, Set.of()).size()).reversed()));
        return r;
    }

    private void updateSidebar() {
        if (ctx == null) return;
        List<String> lines = new ArrayList<>();
        if (lockout) {
            int open = 0;
            for (String s : claimedBy) if (s == null) open++;
            lines.add("§7Unclaimed: §f" + open + "§7/25");
        } else {
            lines.add("§7Items §8| §eLines");
        }
        Set<String> online = new HashSet<>();
        for (Player p : ctx.participants()) { String s = sideOf.get(p.getUniqueId()); if (s != null) online.add(s); }
        for (String s : ranking()) {
            if (lines.size() >= 6) break;
            if (!online.contains(s)) continue;
            String label = ctx.sideLabel(s);
            if (label.length() > 20) label = label.substring(0, 20);
            lines.add(label + " §f" + count(s) + " §8| §e" + linesDone.get(s).size());
        }
        ctx.setSidebarLines(lines);
    }

    private static String ordinal(int n) {
        int mod100 = n % 100;
        if (mod100 >= 11 && mod100 <= 13) return n + "th";
        return switch (n % 10) { case 1 -> n + "st"; case 2 -> n + "nd"; case 3 -> n + "rd"; default -> n + "th"; };
    }

    static Component txt(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy).decoration(TextDecoration.ITALIC, false);
    }

    // ================================================================ kit items

    private ItemStack cardItem() {
        ItemStack it = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(txt("§6§lBingo Card §7(click)"));
        meta.lore(List.of(txt(lockout ? "§7Lockout: first side to get an item claims it." : "§7Collect the items on the card."),
                txt("§7Right-click in hand, or click it in your inventory,"),
                txt("§7to view the card and teleport.")));
        meta.getPersistentDataContainer().set(plugin.cardKey(), PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    private boolean isCard(ItemStack it) {
        if (it == null || it.getType() != Material.KNOWLEDGE_BOOK || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer().has(plugin.cardKey(), PersistentDataType.BYTE);
    }

    private ItemStack finderItem() {
        ItemStack it = new ItemStack(Material.COMPASS);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(txt("§b§lCave Finder §7(right-click)"));
        meta.lore(List.of(txt("§7Points to the nearest big cave"), txt("§7under the ground around you."),
                txt("§8Does not count as a Compass for the card.")));
        meta.getPersistentDataContainer().set(plugin.toolKey(), PersistentDataType.BYTE, (byte) 1);
        it.setItemMeta(meta);
        return it;
    }

    private boolean isFinder(ItemStack it) {
        if (it == null || it.getType() != Material.COMPASS || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer().has(plugin.toolKey(), PersistentDataType.BYTE);
    }

    // ================================================================ cave finder

    private void useFinder(Player p) {
        if (phase != Phase.PLAYING) { p.sendActionBar(txt("§7The Cave Finder works once the game has started.")); return; }
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        long until = finderCooldown.getOrDefault(id, 0L);
        if (now < until) { p.sendActionBar(txt("§cCave Finder recharging: " + ((until - now) / 1000 + 1) + "s")); return; }
        if (finderBusy.contains(id)) return;
        finderCooldown.put(id, now + cfg.finderCooldown * 1000L);
        finderBusy.add(id);
        p.setCooldown(Material.COMPASS, cfg.finderCooldown * 20);
        p.sendActionBar(txt("§bScanning for caves..."));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f);
        final int gen = prepGen;
        finder.find(p, cfg.finderRadiusChunks, (target, ms) -> {
            finderBusy.remove(id);
            if (gen != prepGen || !isPlayer(p) || !p.getWorld().equals(world)) return;
            if (cfg.debugTiming) plugin.getLogger().info(String.format("[debug-timing] cave finder scan %.1f ms (async)", ms));
            if (target == null) {
                int r = cfg.finderRadiusChunks * 16 + 8;
                p.sendActionBar(txt("§7No big cave within about " + r + " blocks. Try somewhere else."));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
                return;
            }
            showCave(p, target);
        });
    }

    private void showCave(Player p, Location target) {
        Location eye = p.getEyeLocation();
        Vector d = target.toVector().subtract(eye.toVector());
        double horiz = Math.hypot(d.getX(), d.getZ());
        int dy = (int) Math.round(target.getY() - p.getLocation().getY());
        String vert = dy < -1 ? "§f" + (-dy) + " below you" : dy > 1 ? "§f" + dy + " above you" : "§fat your level";
        p.sendActionBar(txt("§bCave: §f" + Math.round(horiz) + " blocks " + CaveFinder.compass(d) + "§7, " + vert));
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 1f, 1.2f);
        // Point the compass needle there.
        for (ItemStack it : p.getInventory().getContents()) {
            if (!isFinder(it)) continue;
            if (it.getItemMeta() instanceof CompassMeta cm) {
                cm.setLodestone(target);
                cm.setLodestoneTracked(false);
                it.setItemMeta(cm);
            }
        }
        // A short particle trail from the player towards the cave, visible only to them.
        final int[] runs = {0};
        final BukkitTask[] t = {null};
        t[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!p.isOnline() || !p.getWorld().equals(target.getWorld()) || ++runs[0] > 8) { t[0].cancel(); return; }
            Location from = p.getEyeLocation();
            Vector dir = target.toVector().subtract(from.toVector());
            double len = dir.length();
            if (len < 0.5) { t[0].cancel(); return; }
            dir.multiply(1.0 / len);
            for (double s = 1.5; s <= Math.min(8, len); s += 0.75) {
                p.spawnParticle(Particle.END_ROD, from.clone().add(dir.clone().multiply(s)), 1, 0, 0, 0, 0);
            }
        }, 0L, 8L);
    }

    // ================================================================ /top

    /** Handles /top for a player of this game; false if the player isn't in this game. */
    public boolean handleTop(Player p) {
        if (!isPlayer(p)) return false;
        if (phase != Phase.PLAYING || !p.getWorld().equals(world) || p.getGameMode() != GameMode.SURVIVAL) {
            p.sendMessage(txt("§c/top works while the game is running."));
            return true;
        }
        UUID id = p.getUniqueId();
        long now = System.currentTimeMillis();
        if (cfg.topBlockedInCombat && now < combatUntil.getOrDefault(id, 0L)) {
            p.sendMessage(txt("§cYou were just hit by a player - /top is blocked for "
                    + ((combatUntil.get(id) - now) / 1000 + 1) + "s."));
            return true;
        }
        long until = topCooldown.getOrDefault(id, 0L);
        if (now < until) {
            p.sendMessage(txt("§c/top on cooldown for " + ((until - now) / 1000 + 1) + "s."));
            return true;
        }
        Location l = p.getLocation();
        int x = l.getBlockX(), z = l.getBlockZ();
        Block top = world.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
        if (top.getY() < l.getBlockY()) {
            p.sendMessage(txt("§7You are already at the surface."));
            return true;
        }
        Material t = top.getType();
        if (t == Material.LAVA || t == Material.MAGMA_BLOCK || t == Material.FIRE || t == Material.CAMPFIRE) {
            p.sendMessage(txt("§cThe surface above you isn't safe."));
            return true;
        }
        topCooldown.put(id, now + cfg.topCooldown * 1000L);
        Location dest = new Location(world, x + 0.5, top.getY() + 1, z + 0.5, l.getYaw(), l.getPitch());
        p.setFallDistance(0f);
        p.teleport(dest);
        p.playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.2f);
        return true;
    }

    // ================================================================ card GUI

    private static final class CardMenu implements InventoryHolder {
        final BingoGame game;
        final UUID viewer;
        final Map<Integer, UUID> tpTargets = new HashMap<>();
        Inventory inv;

        CardMenu(BingoGame game, UUID viewer) { this.game = game; this.viewer = viewer; }

        @Override public Inventory getInventory() { return inv; }
    }

    private static int cellSlot(int cell) { return (cell / 5) * 9 + 2 + (cell % 5); }

    private void openCard(Player p) {
        if (!isPlayer(p) || card.isEmpty()) return;
        CardMenu m = new CardMenu(this, p.getUniqueId());
        m.inv = Bukkit.createInventory(m, 54, txt("§8" + displayName() + " card"));
        render(m, p);
        p.openInventory(m.inv);
    }

    private void refreshMenus() {
        if (ctx == null) return;
        for (Player p : ctx.participants()) {
            if (p.getOpenInventory().getTopInventory().getHolder() instanceof CardMenu m && m.game == this) render(m, p);
        }
    }

    /** Arrow for a target at (dx, dz) relative to a player facing {@code yaw} (Minecraft yaw: 0 = +Z, 90 = -X). */
    static String arrow(float yaw, double dx, double dz) {
        double target = Math.toDegrees(Math.atan2(-dx, dz)); // same convention as yaw
        double rel = ((target - yaw) % 360 + 540) % 360 - 180; // -180..180, 0 = straight ahead, + = to the right
        String[] a = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
        return a[(int) Math.round((rel + 360) % 360 / 45) % 8];
    }

    private static String structureName(String kind) {
        return switch (kind) {
            case "village" -> "Village";
            case "desert_pyramid" -> "Desert Pyramid";
            case "jungle_pyramid" -> "Jungle Temple";
            case "ruined_portal" -> "Ruined Portal";
            case "shipwreck" -> "Shipwreck";
            default -> BingoItem.pretty(kind);
        };
    }

    private static Material structureIcon(String kind) {
        return switch (kind) {
            case "village" -> Material.BELL;
            case "desert_pyramid" -> Material.CHISELED_SANDSTONE;
            case "jungle_pyramid" -> Material.MOSSY_COBBLESTONE;
            case "ruined_portal" -> Material.CRYING_OBSIDIAN;
            case "shipwreck" -> Material.OAK_BOAT;
            default -> Material.COMPASS;
        };
    }

    private void render(CardMenu m, Player viewer) {
        Inventory inv = m.inv;
        inv.clear();
        m.tpTargets.clear();
        String side = sideOf.get(viewer.getUniqueId());
        ItemStack frame = simple(Material.BLACK_STAINED_GLASS_PANE, " ", List.of());
        for (int row = 0; row < 5; row++) {
            for (int col : new int[]{0, 1, 7, 8}) inv.setItem(row * 9 + col, frame);
        }
        for (int s = 45; s < 54; s++) inv.setItem(s, simple(Material.GRAY_STAINED_GLASS_PANE, " ", List.of()));

        // Info column
        if (side != null && ctx != null) {
            inv.setItem(0, simple(Material.NETHER_STAR, ctx.sideLabel(side), List.of(
                    "§7Items: §a" + count(side) + "§7/25",
                    "§7Lines: §e" + linesDone.get(side).size() + "§7/12",
                    "§7Time left: §f" + (secondsLeft / 60) + ":" + String.format("%02d", Math.max(0, secondsLeft % 60)))));
        }
        int[] tp = lockout ? cfg.lockoutTierPoints : cfg.bingoTierPoints;
        List<String> scoring = new ArrayList<>();
        scoring.add("§7Square value by difficulty:");
        for (int t = 1; t <= 4; t++) scoring.add("  " + BingoItem.TIER_COLORS[t] + BingoItem.TIER_NAMES[t] + "§7: §f+" + tp[t]);
        if (lockout) {
            scoring.add("§7Own a full line: §f+" + cfg.lockoutLine);
            scoring.add("§7Final placement: §f+" + join(cfg.lockoutPlacement, " / +"));
        } else {
            scoring.add("§7Later finders get " + join(cfg.placementPercent, "% / ") + "%");
            scoring.add("§7Line (first 4): §f+" + cfg.bingoLineFirst4 + "§7, later §f+" + cfg.bingoLineLater);
            scoring.add("§7Full card: §f+" + cfg.bingoFullCard);
        }
        scoring.add("§7Items count while in your inventory.");
        inv.setItem(9, simple(Material.OAK_HANGING_SIGN, "§eScoring", scoring));
        // Structures: one entry each in the second column (slots 1, 10, 19, 28, 37), nearest to the viewer first.
        // Buried treasure is never listed (finding it is the point).
        Location here = viewer.getLocation();
        List<AreaLayout.Site> shown = new ArrayList<>();
        for (AreaLayout.Site s : placed) if (!s.kind().equals("buried_treasure")) shown.add(s);
        shown.sort(Comparator.comparingDouble(s -> Math.hypot(s.x() + 0.5 - here.getX(), s.z() + 0.5 - here.getZ())));
        int[] structSlots = {1, 10, 19, 28, 37};
        for (int i = 0; i < shown.size() && i < structSlots.length; i++) {
            AreaLayout.Site s = shown.get(i);
            double dx = s.x() + 0.5 - here.getX(), dz = s.z() + 0.5 - here.getZ();
            long dist = Math.round(Math.hypot(dx, dz));
            Vector d = new Vector(dx, 0, dz);
            String dir = dist < 8 ? "§aYou are here" : "§f" + arrow(here.getYaw(), dx, dz) + " " + CaveFinder.compass(d) + "§7  ·  §f" + dist + " blocks";
            inv.setItem(structSlots[i], simple(structureIcon(s.kind()), "§e§l" + structureName(s.kind()), List.of(
                    dir,
                    "§8x " + s.x() + ",  z " + s.z())));
        }
        List<String> sHead = new ArrayList<>();
        sHead.add(shown.isEmpty() ? "§7None placed in this area." : "§7Listed to the right, nearest first.");
        sHead.add("§7Arrows: from the way you faced");
        sHead.add("§7when you opened this menu.");
        sHead.add("§7Mineshafts: underground everywhere.");
        inv.setItem(18, simple(Material.FILLED_MAP, "§e§lStructures", sHead));
        List<String> bl = new ArrayList<>();
        if (layout != null) {
            List<String> names = new ArrayList<>();
            for (AreaLayout.Cell c : layout.cells) {
                if (!layout.biomes.contains(c.surface())) continue;
                names.add(BingoItem.pretty(c.surface().name()) + (c.inner() ? " §7(near start)" : "") + "§f");
            }
            for (int i = 0; i < names.size(); i += 2) bl.add("§f" + names.get(i) + (i + 1 < names.size() ? "§7, §f" + names.get(i + 1) : ""));
            if (layout.biomes.contains(org.bukkit.block.Biome.LUSH_CAVES)) bl.add("§aLush caves §7underground");
            if (layout.biomes.contains(org.bukkit.block.Biome.DRIPSTONE_CAVES)) bl.add("§6Dripstone caves §7underground");
        }
        inv.setItem(27, simple(Material.GRASS_BLOCK, "§eBiomes in this area", bl));

        // Cells
        for (int i = 0; i < 25 && i < card.size(); i++) {
            BingoItem item = card.get(i);
            boolean mine = side != null && got.containsKey(side) && got.get(side)[i];
            List<String> lore = new ArrayList<>();
            lore.add("§7Difficulty: " + BingoItem.TIER_COLORS[item.tier] + BingoItem.TIER_NAMES[item.tier]);
            if (item.hint != null) lore.add("§7Where: §f" + item.hint);
            if (item.anyVariant) lore.add("§7Any of: §f" + variants(item));
            ItemStack icon;
            if (lockout) {
                String owner = claimedBy[i];
                if (owner == null) {
                    lore.add("§7Worth: §f+" + lockoutPoints(item.tier));
                    lore.add("§fUnclaimed §7- first to get it claims it!");
                    icon = simple(item.icon, "§f" + item.name + " §7(+" + lockoutPoints(item.tier) + ")", lore);
                } else if (owner.equals(side)) {
                    lore.add("§aClaimed by your side");
                    icon = simple(item.icon, "§a✔ " + item.name, lore);
                    glint(icon);
                } else {
                    lore.add("§cClaimed by " + (ctx != null ? ctx.sideLabel(owner) : owner));
                    icon = simple(paneFor(owner), "§c✖ " + item.name, lore);
                }
            } else {
                int n = completions[i];
                lore.add("§7Sides with it: §f" + n);
                if (mine) {
                    lore.add("§aObtained by your side");
                    icon = simple(item.icon, "§a✔ " + item.name, lore);
                    glint(icon);
                } else {
                    int worth = bingoPoints(item.tier, n + 1);
                    lore.add("§7Worth now: §f+" + worth + " §8(full value +" + bingoPoints(item.tier, 1) + ")");
                    icon = simple(item.icon, "§f" + item.name + " §7(+" + worth + ")", lore);
                }
            }
            inv.setItem(cellSlot(i), icon);
        }

        // Teleports
        inv.setItem(45, simple(Material.ENDER_PEARL, "§bTeleport to the start point",
                List.of("§7Cooldown: " + cfg.teleportCooldown + "s")));
        if (side != null) {
            int slot = 47;
            for (Player mate : members(side)) {
                if (slot > 53) break;
                if (mate.equals(viewer)) continue;
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(mate);
                sm.displayName(txt("§bTeleport to " + mate.getName()));
                sm.lore(List.of(txt("§7Cooldown: " + cfg.teleportCooldown + "s")));
                head.setItemMeta(sm);
                inv.setItem(slot, head);
                m.tpTargets.put(slot, mate.getUniqueId());
                slot++;
            }
        }
    }

    private static String variants(BingoItem item) {
        List<String> n = new ArrayList<>();
        for (Material mat : item.accepted) n.add(BingoItem.pretty(mat.name()));
        return String.join(", ", n);
    }

    private static String join(int[] a, String sep) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < a.length; i++) { if (i > 0) sb.append(sep); sb.append(a[i]); }
        return sb.toString();
    }

    private static ItemStack simple(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.displayName(txt(name));
            List<Component> l = new ArrayList<>();
            for (String s : lore) l.add(txt(s));
            meta.lore(l);
            meta.addItemFlags(ItemFlag.values());
            it.setItemMeta(meta);
        }
        return it;
    }

    private static void glint(ItemStack it) {
        ItemMeta meta = it.getItemMeta();
        if (meta == null) return;
        meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
    }

    private static Material paneFor(String side) {
        if (side == null || !side.startsWith("TEAM:")) return Material.WHITE_STAINED_GLASS_PANE;
        return switch (side.substring(5).toLowerCase(Locale.ROOT)) {
            case "red" -> Material.RED_STAINED_GLASS_PANE;
            case "orange" -> Material.ORANGE_STAINED_GLASS_PANE;
            case "yellow" -> Material.YELLOW_STAINED_GLASS_PANE;
            case "lime" -> Material.LIME_STAINED_GLASS_PANE;
            case "green" -> Material.GREEN_STAINED_GLASS_PANE;
            case "cyan" -> Material.CYAN_STAINED_GLASS_PANE;
            case "aqua" -> Material.LIGHT_BLUE_STAINED_GLASS_PANE;
            case "blue" -> Material.BLUE_STAINED_GLASS_PANE;
            case "purple" -> Material.PURPLE_STAINED_GLASS_PANE;
            case "pink" -> Material.PINK_STAINED_GLASS_PANE;
            default -> Material.WHITE_STAINED_GLASS_PANE;
        };
    }

    /**
     * A click in the card menu. Runs one tick after the click (never inside the click event: closing the inventory
     * and teleporting during InventoryClickEvent is unreliable), and every refusal tells the player why.
     */
    private void handleMenuClick(Player p, CardMenu m, int slot) {
        boolean isStart = slot == 45;
        UUID mateId = m.tpTargets.get(slot);
        if (!isStart && mateId == null) return; // not a button
        if (world == null || !isPlayer(p)) { deny(p, "§cTeleports only work for players in this game."); return; }
        if (phase != Phase.PLAYING) { deny(p, "§cTeleports work once the game has started."); return; }
        if (p.getGameMode() != GameMode.SURVIVAL || p.isDead()) { deny(p, "§cYou can't teleport right now."); return; }
        if (!p.getWorld().equals(world)) { deny(p, "§cYou are not in the Bingo world."); return; }
        String side = sideOf.get(p.getUniqueId());
        Location dest;
        if (isStart) {
            dest = nextStartSpot();
        } else {
            Player mate = Bukkit.getPlayer(mateId);
            if (mate == null || !mate.isOnline() || mate.isDead() || !mate.getWorld().equals(world)
                    || mate.getGameMode() != GameMode.SURVIVAL || !side.equals(sideOf.get(mate.getUniqueId()))) {
                deny(p, "§cThat teammate can't be reached right now.");
                return;
            }
            dest = mate.getLocation();
        }
        if (dest == null || dest.getWorld() == null) { deny(p, "§cNo start point yet - try again in a moment."); return; }
        long now = System.currentTimeMillis();
        long until = tpCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now < until) { deny(p, "§cTeleport on cooldown for " + ((until - now) / 1000 + 1) + "s."); return; }
        long combat = combatUntil.getOrDefault(p.getUniqueId(), 0L);
        if (cfg.topBlockedInCombat && now < combat) {
            deny(p, "§cYou were just hit by a player - teleports are blocked for " + ((combat - now) / 1000 + 1) + "s.");
            return;
        }
        final Location target = dest.clone();
        final int gen = prepGen;
        p.closeInventory();
        p.setFallDistance(0f);
        // teleportAsync loads the target chunk off the main thread first (the start area may have unloaded).
        p.teleportAsync(target).whenComplete((ok, err) -> Bukkit.getScheduler().runTask(plugin, () -> {
            if (!p.isOnline()) return;
            if (Boolean.TRUE.equals(ok) && err == null) {
                if (gen == prepGen) tpCooldown.put(p.getUniqueId(), System.currentTimeMillis() + cfg.teleportCooldown * 1000L);
                p.setFallDistance(0f);
                p.playSound(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
            } else {
                plugin.getLogger().warning("Bingo: card teleport of " + p.getName() + " failed" + (err != null ? ": " + err : " (cancelled by a listener)"));
                deny(p, "§cThe teleport didn't go through - try again.");
            }
        }));
    }

    private static void deny(Player p, String msg) {
        if (p == null || !p.isOnline()) return;
        p.sendMessage(txt(msg));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
    }

    // ================================================================ listeners (only act while running, only in our world)

    private boolean running() { return phase != Phase.IDLE && ctx != null && world != null; }

    private boolean isPlayer(Player p) { return running() && sideOf.containsKey(p.getUniqueId()); }

    private boolean inWorld(Entity e) { return running() && e.getWorld().equals(world); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (!running()) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack it = e.getItem();
        if (isCard(it)) {
            e.setCancelled(true);
            if (isPlayer(e.getPlayer())) openCard(e.getPlayer());
        } else if (isFinder(it)) {
            e.setCancelled(true);
            if (isPlayer(e.getPlayer()) && e.getHand() == org.bukkit.inventory.EquipmentSlot.HAND) useFinder(e.getPlayer());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (!running()) return;
        ItemStack it = e.getItemDrop().getItemStack();
        if (isCard(it) || isFinder(it)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapHands(PlayerSwapHandItemsEvent e) {
        if (!isPlayer(e.getPlayer())) return;
        if (isCard(e.getMainHandItem()) || isCard(e.getOffHandItem())) e.setCancelled(true);
    }

    /** No crafting with the kit items (the Cave Finder is a compass; it must not become a Map). */
    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        if (!running()) return;
        for (ItemStack it : e.getInventory().getMatrix()) {
            if (isFinder(it) || isCard(it)) { e.getInventory().setResult(null); return; }
        }
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof CardMenu m && m.game == this) {
            e.setCancelled(true);
            if (!(e.getWhoClicked() instanceof Player p)) return;
            // Top-inventory slots are raw slots 0..size-1 (no identity check on inventory wrappers).
            int raw = e.getRawSlot();
            if (raw < 0 || raw >= m.inv.getSize()) return;
            Bukkit.getScheduler().runTask(plugin, () -> { if (p.isOnline()) handleMenuClick(p, m, raw); });
            return;
        }
        // Any click on the card in the player's own inventory opens the card; the card never moves.
        if (!(e.getWhoClicked() instanceof Player p) || !isPlayer(p)) return;
        boolean onCard = isCard(e.getCurrentItem());
        boolean touchesCard = onCard || isCard(e.getCursor())
                || (e.getClick() == ClickType.NUMBER_KEY && e.getHotbarButton() >= 0 && isCard(p.getInventory().getItem(e.getHotbarButton())))
                || (e.getClick() == ClickType.SWAP_OFFHAND && isCard(p.getInventory().getItemInOffHand()));
        if (!touchesCard) return;
        e.setCancelled(true);
        if (onCard && e.getClickedInventory() != null && e.getClickedInventory().getType() == org.bukkit.event.inventory.InventoryType.PLAYER) {
            Bukkit.getScheduler().runTask(plugin, () -> { if (isPlayer(p) && p.isOnline()) openCard(p); });
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof CardMenu m && m.game == this) { e.setCancelled(true); return; }
        if (e.getWhoClicked() instanceof Player p && isPlayer(p) && isCard(e.getOldCursor())) e.setCancelled(true);
    }

    private static Player attackerOf(Entity d) {
        if (d instanceof Player p) return p;
        if (d instanceof Projectile pr) {
            ProjectileSource src = pr.getShooter();
            if (src instanceof Player p) return p;
        }
        if (d instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) return p;
        return null;
    }

    /** PvP between sides while playing; no friendly fire; nothing during the countdown. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !inWorld(victim)) return;
        Player attacker = attackerOf(e.getDamager());
        if (attacker == null || attacker.equals(victim)) return;
        String vs = sideOf.get(victim.getUniqueId()), as = sideOf.get(attacker.getUniqueId());
        if (phase != Phase.PLAYING || vs == null || as == null || vs.equals(as)) { e.setCancelled(true); return; }
        combatUntil.put(victim.getUniqueId(), System.currentTimeMillis() + cfg.combatSeconds * 1000L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (phase != Phase.COUNTDOWN) return;
        if (e.getEntity() instanceof Player p && inWorld(p)) e.setCancelled(true);
    }

    /** Keep inventory and levels, nothing drops (no items for the killer). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!isPlayer(p) || !p.getWorld().equals(world)) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setKeepLevel(true);
        e.setDroppedExp(0);
        Player killer = p.getKiller();
        if (killer != null && cfg.killPoints > 0 && phase == Phase.PLAYING && ctx != null) {
            String ks = sideOf.get(killer.getUniqueId());
            if (ks != null && !ks.equals(sideOf.get(p.getUniqueId()))) ctx.addPoints(killer.getUniqueId(), cfg.killPoints, "Bingo kill");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (!running() || !e.getLocation().getWorld().equals(world)) return;
        if (e.getEntityType() == EntityType.PHANTOM) e.setCancelled(true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!isPlayer(p)) return;
        Location r = e.getRespawnLocation();
        boolean ownSpawn = (e.isBedSpawn() || e.isAnchorSpawn()) && r != null && world.equals(r.getWorld())
                && world.getWorldBorder().isInside(r);
        if (!ownSpawn) e.setRespawnLocation(nextStartSpot());
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (isPlayer(p) && p.isOnline()) applySpeed(p);
        }, 2L);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPortal(PlayerPortalEvent e) {
        if (!running() || !e.getFrom().getWorld().equals(world)) return;
        e.setCancelled(true);
        e.getPlayer().sendActionBar(txt("§cPortals are disabled in " + displayName()));
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPortal(EntityPortalEvent e) {
        if (!running() || !e.getFrom().getWorld().equals(world)) return;
        e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (phase != Phase.COUNTDOWN || !isPlayer(e.getPlayer())) return;
        Location from = e.getFrom(), to = e.getTo();
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            Location fixed = from.clone();
            fixed.setYaw(to.getYaw());
            fixed.setPitch(to.getPitch());
            e.setTo(fixed);
        }
    }
}
