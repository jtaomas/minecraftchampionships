package com.mcm.games.bingo;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
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
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Bingo but Fast (shared card, placement-based points per square, line bonuses) and,
 * with {@code lockout = true}, Lockout Bingo (first side to obtain a square claims it).
 */
public final class BingoGame implements McmGame, Listener {

    // ---------------------------------------------------------------- tuning
    private static final int BINGO_SECONDS = 600;
    private static final int LOCKOUT_SECONDS = 720;
    private static final int COUNTDOWN_SECONDS = 5;

    /** Bingo: points to each member of the side for completing a square, by completion order (1st..5th+). */
    private static final int[] PLACE_PTS = {6, 5, 4, 3, 2};
    private static final int LINE_PTS_FIRST4 = 12;
    private static final int LINE_PTS_LATER = 6;
    private static final int FULL_CARD_PTS = 30;

    /** Lockout: per member. */
    private static final int LOCK_CLAIM_PTS = 8;
    private static final int LOCK_LINE_PTS = 15;
    private static final int[] LOCK_PLACE_PTS = {20, 12, 6};

    private static final long TP_COOLDOWN_MS = 45_000;

    private enum Phase { IDLE, COUNTDOWN, PLAYING }

    private final BingoPlugin plugin;
    private final BingoWorld worldMgr;
    private final boolean lockout;

    // ---------------------------------------------------------------- per-game state
    private McmGameContext ctx;
    private World world;
    private Phase phase = Phase.IDLE;
    private BukkitTask task;
    private boolean halfSecond;
    private int countdown;
    private int secondsLeft;
    private int totalSeconds;

    private List<BingoItem> card = List.of();
    private final Map<String, boolean[]> got = new HashMap<>();
    private final Map<String, Set<Integer>> linesDone = new HashMap<>();
    private final Map<String, Boolean> fullCard = new HashMap<>();
    private int[] completions = new int[25];
    private String[] claimedBy = new String[25];
    private final Map<UUID, String> sideOf = new HashMap<>();
    private final List<String> sideOrder = new ArrayList<>();
    private final Map<String, Location> sideSpawn = new HashMap<>();
    private final Map<UUID, Long> tpCooldown = new HashMap<>();

    public BingoGame(BingoPlugin plugin, BingoWorld worldMgr, boolean lockout) {
        this.plugin = plugin;
        this.worldMgr = worldMgr;
        this.lockout = lockout;
    }

    // ================================================================ McmGame

    @Override public String id() { return lockout ? "lockout" : "bingo"; }

    @Override public String displayName() { return lockout ? "Lockout Bingo" : "Bingo but Fast"; }

    @Override public Material icon() { return lockout ? Material.IRON_BARS : Material.KNOWLEDGE_BOOK; }

    @Override
    public String description() {
        return lockout
                ? "Survival race: the first side to obtain an item claims its square."
                : "Survival race: collect the 25 items on the shared card, lines give bonuses.";
    }

    @Override public boolean useCoreScoreboard() { return true; }

    @Override
    public void start(McmGameContext ctx) {
        if (phase != Phase.IDLE) cleanup(false);
        this.ctx = ctx;
        resetState();

        world = worldMgr.createFresh();
        if (world == null) {
            McmGameContext c = this.ctx;
            this.ctx = null;
            c.broadcast("§c[" + displayName() + "] Could not generate the world, skipping.");
            c.finish("World generation failed");
            return;
        }

        Random rnd = ThreadLocalRandom.current();
        card = lockout ? BingoItem.generateCard(rnd, 8, 12, 5) : BingoItem.generateCard(rnd, 10, 11, 4);
        totalSeconds = lockout ? LOCKOUT_SECONDS : BINGO_SECONDS;
        secondsLeft = totalSeconds;

        // Sides
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

        computeSpawns();

        for (Player p : new ArrayList<>(ctx.participants())) {
            String side = sideOf.get(p.getUniqueId());
            Location loc = sideSpawn.getOrDefault(side, world.getSpawnLocation());
            preparePlayer(p);
            p.teleport(loc);
            p.showTitle(Title.title(txt("§6§l" + displayName()),
                    txt(lockout ? "§fFirst to get an item claims it!" : "§fCollect the items on your card!"),
                    Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000), Duration.ofMillis(500))));
        }

        ctx.broadcast("§6§l" + displayName() + "§r§7 - " + (lockout
                ? "Get items before anyone else to claim their squares. Right-click your card to see it."
                : "Collect the 25 items on the card. Earlier completions and full lines pay more. Right-click your card to see it."));

        phase = Phase.COUNTDOWN;
        countdown = FastTest.secs(COUNTDOWN_SECONDS);
        halfSecond = false;
        ctx.setTimer(totalSeconds, totalSeconds);
        updateSidebar();
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 10L, 10L);
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
        if (phase == Phase.IDLE || world == null) return false;
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(world.getSpawnLocation().clone().add(0, 15, 0));
        player.sendMessage(txt("§7You joined mid-game and are spectating " + displayName() + "."));
        return true;
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
        sideSpawn.clear();
        tpCooldown.clear();
        card = List.of();
    }

    private void cleanup(boolean scheduleWorldCleanup) {
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
            // Placement bonus by squares claimed (ties share the higher placement).
            int place = 0, prevCount = -1, idx = 0;
            for (String s : ranking) {
                int n = count(s);
                idx++;
                if (n != prevCount) { place = idx; prevCount = n; }
                if (n == 0 || place > LOCK_PLACE_PTS.length) continue;
                awardSide(c, s, LOCK_PLACE_PTS[place - 1], "Lockout " + ordinal(place) + " place");
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
    }

    private void applySpeed(Player p) {
        if (!p.hasPotionEffect(PotionEffectType.SPEED)) {
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 0, false, false, true));
        }
    }

    private void giveKit(Player p) {
        ItemStack pick = new ItemStack(Material.IRON_PICKAXE);
        pick.addUnsafeEnchantment(Enchantment.EFFICIENCY, 2);
        pick.addUnsafeEnchantment(Enchantment.UNBREAKING, 2);
        ItemStack axe = new ItemStack(Material.IRON_AXE);
        axe.addUnsafeEnchantment(Enchantment.EFFICIENCY, 2);
        ItemStack shovel = new ItemStack(Material.IRON_SHOVEL);
        shovel.addUnsafeEnchantment(Enchantment.EFFICIENCY, 2);
        p.getInventory().setItem(0, pick);
        p.getInventory().setItem(1, axe);
        p.getInventory().setItem(2, shovel);
        p.getInventory().setItem(3, new ItemStack(Material.COOKED_BEEF, 12));
        p.getInventory().setItem(8, cardItem());
    }

    private void computeSpawns() {
        Location base = world.getSpawnLocation();
        int n = sideOrder.size();
        double radius = n <= 1 ? 0 : Math.min(6 + n * 2.0, 30);
        for (int i = 0; i < n; i++) {
            double a = 2 * Math.PI * i / Math.max(1, n);
            int x = (int) Math.round(base.getX() + Math.cos(a) * radius);
            int z = (int) Math.round(base.getZ() + Math.sin(a) * radius);
            Location loc = safeSpot(x, z);
            loc.setYaw((float) Math.toDegrees(Math.atan2(-(base.getX() - loc.getX()), base.getZ() - loc.getZ())));
            sideSpawn.put(sideOrder.get(i), loc);
        }
    }

    private Location safeSpot(int bx, int bz) {
        Random r = ThreadLocalRandom.current();
        for (int attempt = 0; attempt < 40; attempt++) {
            int spread = 2 + attempt;
            int x = attempt == 0 ? bx : bx + r.nextInt(-spread, spread + 1);
            int z = attempt == 0 ? bz : bz + r.nextInt(-spread, spread + 1);
            Block b = world.getHighestBlockAt(x, z);
            Material t = b.getType();
            if (t.isSolid() && !t.name().endsWith("_LEAVES") && t != Material.MAGMA_BLOCK && t != Material.CACTUS) {
                return new Location(world, x + 0.5, b.getY() + 1, z + 0.5);
            }
        }
        Location s = world.getSpawnLocation();
        Block top = world.getHighestBlockAt(s);
        return new Location(world, s.getBlockX() + 0.5, top.getY() + 1, s.getBlockZ() + 0.5);
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
                    p.showTitle(Title.title(txt("§a§lGO!"), txt("§7Right-click your card to see it"),
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
        if (world != null) world.setTime(1000);
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (!p.isDead() && p.getGameMode() == GameMode.SURVIVAL) applySpeed(p);
            String side = sideOf.get(p.getUniqueId());
            if (side == null) continue;
            p.sendActionBar(txt("§a" + count(side) + "§7/25 items  §e" + linesDone.get(side).size()
                    + "§7 lines  §8|  §fRight-click your card"));
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
            boolean hasCard = false;
            for (ItemStack it : p.getInventory().getContents()) {
                if (it == null || it.getType().isAir()) continue;
                if (isCard(it)) { hasCard = true; continue; }
                held.add(it.getType());
            }
            ItemStack cursor = p.getItemOnCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                if (isCard(cursor)) hasCard = true; else held.add(cursor.getType());
            }
            if (!hasCard && p.getGameMode() == GameMode.SURVIVAL) p.getInventory().addItem(cardItem());
            if (held.isEmpty()) continue;

            boolean[] mine = got.get(side);
            for (int i = 0; i < 25 && phase == Phase.PLAYING; i++) {
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

    private void complete(String side, int cell, Player finder) {
        boolean[] mine = got.get(side);
        mine[cell] = true;
        completions[cell]++;
        int place = completions[cell];
        int pts = PLACE_PTS[Math.min(place, PLACE_PTS.length) - 1];
        BingoItem item = card.get(cell);
        awardSide(ctx, side, pts, "Bingo: " + item.name);
        for (Player m : members(side)) {
            m.sendMessage(txt("§a✔ " + (m.equals(finder) ? "You" : finder.getName()) + " got §e" + item.name
                    + " §7(" + ordinal(place) + ", +" + pts + ")"));
            m.playSound(m.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.4f);
        }
        if (place == 1) ctx.broadcast("§7[Bingo] " + ctx.sideLabel(side) + " §7was first to get §e" + item.name + "§7!");
        checkLines(side);
        if (count(side) == 25 && !fullCard.get(side)) {
            fullCard.put(side, true);
            awardSide(ctx, side, FULL_CARD_PTS, "Bingo: full card");
            ctx.broadcast("§6§lFULL CARD! " + ctx.sideLabel(side) + " §6completed the entire card!");
            for (Player m : members(side)) {
                m.showTitle(Title.title(txt("§6§lFULL CARD!"), txt("§f+" + FULL_CARD_PTS + " points"),
                        Title.Times.times(Duration.ofMillis(100), Duration.ofMillis(2500), Duration.ofMillis(500))));
                m.playSound(m.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
            }
        }
    }

    private void claim(String side, int cell, Player finder) {
        got.get(side)[cell] = true;
        claimedBy[cell] = side;
        BingoItem item = card.get(cell);
        awardSide(ctx, side, LOCK_CLAIM_PTS, "Lockout: " + item.name);
        ctx.broadcast("§7[Lockout] " + ctx.sideLabel(side) + " §7claimed §e" + item.name + " §7(" + finder.getName() + ")");
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
            int pts = lockout ? LOCK_LINE_PTS : (done.size() <= 4 ? LINE_PTS_FIRST4 : LINE_PTS_LATER);
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

    // ================================================================ card item + GUI

    private ItemStack cardItem() {
        ItemStack it = new ItemStack(Material.KNOWLEDGE_BOOK);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(txt("§6§lBingo Card §7(right-click)"));
        meta.lore(List.of(txt(lockout ? "§7Lockout: first side to get an item claims it." : "§7Collect the items on the card."),
                txt("§7Right-click to view the card and teleport.")));
        meta.getPersistentDataContainer().set(plugin.cardKey(), PersistentDataType.BYTE, (byte) 1);
        meta.setEnchantmentGlintOverride(true);
        it.setItemMeta(meta);
        return it;
    }

    private boolean isCard(ItemStack it) {
        if (it == null || it.getType() != Material.KNOWLEDGE_BOOK || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer().has(plugin.cardKey(), PersistentDataType.BYTE);
    }

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

        // Info
        if (side != null && ctx != null) {
            inv.setItem(0, simple(Material.NETHER_STAR, ctx.sideLabel(side), List.of(
                    "§7Items: §a" + count(side) + "§7/25",
                    "§7Lines: §e" + linesDone.get(side).size() + "§7/12",
                    "§7Time left: §f" + (secondsLeft / 60) + ":" + String.format("%02d", Math.max(0, secondsLeft % 60)))));
        }
        inv.setItem(9, simple(Material.OAK_HANGING_SIGN, "§eScoring", lockout
                ? List.of("§7Claim a square: §f+" + LOCK_CLAIM_PTS + " each", "§7Own a full line: §f+" + LOCK_LINE_PTS,
                "§7Final placement: §f+20 / +12 / +6", "§7Items count while in your inventory.")
                : List.of("§7Square, by order: §f+6 +5 +4 +3 +2", "§7Line (first 4): §f+" + LINE_PTS_FIRST4,
                "§7Line (after that): §f+" + LINE_PTS_LATER, "§7Full card: §f+" + FULL_CARD_PTS,
                "§7Items count while in your inventory.")));

        // Cells
        for (int i = 0; i < 25 && i < card.size(); i++) {
            BingoItem item = card.get(i);
            boolean mine = side != null && got.containsKey(side) && got.get(side)[i];
            List<String> lore = new ArrayList<>();
            lore.add("§7Difficulty: " + (item.tier == 1 ? "§aEasy" : item.tier == 2 ? "§eMedium" : "§cHard"));
            if (item.anyVariant) lore.add("§7Any variant counts");
            ItemStack icon;
            if (lockout) {
                String owner = claimedBy[i];
                if (owner == null) {
                    lore.add("§fUnclaimed §7- first to get it claims it!");
                    icon = simple(item.icon, "§f" + item.name, lore);
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
                    lore.add("§7Worth now: §f+" + PLACE_PTS[Math.min(n + 1, PLACE_PTS.length) - 1]);
                    icon = simple(item.icon, "§f" + item.name, lore);
                }
            }
            inv.setItem(cellSlot(i), icon);
        }

        // Teleports
        inv.setItem(45, simple(Material.ENDER_PEARL, "§bTeleport to your start point",
                List.of("§7Cooldown: " + (TP_COOLDOWN_MS / 1000) + "s")));
        if (side != null) {
            int slot = 47;
            for (Player mate : members(side)) {
                if (slot > 53) break;
                if (mate.equals(viewer)) continue;
                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta sm = (SkullMeta) head.getItemMeta();
                sm.setOwningPlayer(mate);
                sm.displayName(txt("§bTeleport to " + mate.getName()));
                sm.lore(List.of(txt("§7Cooldown: " + (TP_COOLDOWN_MS / 1000) + "s")));
                head.setItemMeta(sm);
                inv.setItem(slot, head);
                m.tpTargets.put(slot, mate.getUniqueId());
                slot++;
            }
        }
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

    private void handleMenuClick(Player p, CardMenu m, int slot) {
        if (phase != Phase.PLAYING || world == null) return;
        String side = sideOf.get(p.getUniqueId());
        if (side == null) return;
        Location dest = null;
        if (slot == 45) {
            dest = sideSpawn.getOrDefault(side, world.getSpawnLocation());
        } else if (m.tpTargets.containsKey(slot)) {
            Player mate = Bukkit.getPlayer(m.tpTargets.get(slot));
            if (mate == null || !mate.isOnline() || mate.isDead() || !mate.getWorld().equals(world)
                    || !side.equals(sideOf.get(mate.getUniqueId()))) {
                p.sendMessage(txt("§cThat teammate can't be reached right now."));
                return;
            }
            dest = mate.getLocation();
        }
        if (dest == null) return;
        long now = System.currentTimeMillis();
        long until = tpCooldown.getOrDefault(p.getUniqueId(), 0L);
        if (now < until) {
            p.sendMessage(txt("§cTeleport on cooldown for " + ((until - now) / 1000 + 1) + "s."));
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.6f);
            return;
        }
        tpCooldown.put(p.getUniqueId(), now + TP_COOLDOWN_MS);
        Location target = dest.clone();
        p.closeInventory();
        p.setFallDistance(0f);
        p.teleport(target);
        p.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
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
        if (!isCard(e.getItem())) return;
        e.setCancelled(true);
        if (isPlayer(e.getPlayer())) openCard(e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (!running()) return;
        if (isCard(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CardMenu m) || m.game != this) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
        handleMenuClick(p, m, e.getRawSlot());
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof CardMenu m && m.game == this) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPvp(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim) || !inWorld(victim)) return;
        Player attacker = null;
        Entity d = e.getDamager();
        if (d instanceof Player p) attacker = p;
        else if (d instanceof Projectile pr) {
            ProjectileSource src = pr.getShooter();
            if (src instanceof Player p) attacker = p;
        } else if (d instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) attacker = p;
        if (attacker != null && !attacker.equals(victim)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (phase != Phase.COUNTDOWN) return;
        if (e.getEntity() instanceof Player p && inWorld(p)) e.setCancelled(true);
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
        String side = sideOf.get(p.getUniqueId());
        e.setRespawnLocation(sideSpawn.getOrDefault(side, world.getSpawnLocation()));
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
