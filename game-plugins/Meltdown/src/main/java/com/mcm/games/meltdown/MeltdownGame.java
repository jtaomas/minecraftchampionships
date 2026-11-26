package com.mcm.games.meltdown;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public final class MeltdownGame implements McmGame {
    public enum Phase { IDLE, BUILDING, COUNTDOWN, RUNNING, ENDING }

    static final String WORLD_NAME = "mcc_meltdown";
    static final int TOTAL_SECONDS = 480;
    static final int COUNTDOWN_SECONDS = 10;
    static final int WARN_LEAD = 20;
    static final int HEATER_DURATION = 25;
    static final int HEATER_COOLDOWN = 10;
    static final double HEATER_RADIUS_SQ = 4.5 * 4.5;
    static final int THAW_NEEDED = 5;
    static final int SHATTER_AFTER = 40;
    /** After a room has been frozen this long, heaters and warmers no longer help there. */
    static final int ABSOLUTE_ZERO_AFTER = 60;
    static final long WARMER_MILLIS = 8000;

    // MCC coins (base x1.0, config coins.*; the core applies the event multiplier). See game-plugins/COINS.md.
    int PTS_CRATE = 10;          // MCC: team pool per coin crate, shared amongst the team
    int PTS_FREEZE = 25;         // MCC: freezing a player; taken away again if that player is thawed out
    int PTS_FREEZE_ELIM = 0;     // MCC: nothing extra when the frozen player is eliminated (the 25 simply stays)
    int PTS_SURVIVAL = 0;        // MCC: no coins for outliving players
    int[] PLACEMENT_TEAM = {210, 190, 170, 150, 125, 100, 80, 60, 45, 30};   // MCC: team pool by survival placement

    private final MeltdownPlugin plugin;
    final NamespacedKey itemKey;

    McmGameContext ctx;
    Phase phase = Phase.IDLE;
    Arena arena;
    World world;
    private Random rng = new Random();
    private BukkitTask buildTask, tickTask, endTask;
    private boolean finished = true;
    private int elapsed;
    private int countdown;
    private final Set<Long> ticketChunks = new HashSet<>();

    // players & sides
    final Map<UUID, String> sideOf = new HashMap<>();
    final Map<String, List<UUID>> sideMembers = new LinkedHashMap<>();
    final Map<UUID, String> names = new HashMap<>();
    final Set<UUID> alive = new LinkedHashSet<>();
    final Set<String> aliveSides = new LinkedHashSet<>();
    final Map<String, Location> spawnOf = new HashMap<>();
    final Map<String, Color> sideColor = new HashMap<>();
    private int initialPlayers;
    private int initialSides;

    // mechanics
    static final class Frozen {
        final UUID by;
        final int since;
        double thaw;
        Frozen(UUID by, int since) { this.by = by; this.since = since; }
    }

    static final class Heater {
        final Block block;
        final String side;
        final UUID owner;
        final int expiresAt;
        Heater(Block block, String side, UUID owner, int expiresAt) {
            this.block = block; this.side = side; this.owner = owner; this.expiresAt = expiresAt;
        }
    }

    final Map<UUID, Frozen> frozen = new HashMap<>();
    final Map<Long, Heater> heaters = new HashMap<>();
    final Map<UUID, Heater> heaterOf = new HashMap<>();
    final Map<UUID, Integer> heaterReturnAt = new HashMap<>();
    final Map<UUID, Long> warmerUntil = new HashMap<>();
    private final Map<UUID, Room.State> lastState = new HashMap<>();
    private final Map<UUID, Room> lastRoom = new HashMap<>();
    private final Set<Integer> announcedRings = new HashSet<>();
    private int cratesLeft;

    public MeltdownGame(MeltdownPlugin plugin) {
        this.plugin = plugin;
        this.itemKey = new NamespacedKey(plugin, "item");
    }

    @Override public String id() { return "meltdown"; }
    @Override public String displayName() { return "Meltdown"; }
    @Override public Material icon() { return Material.BLUE_ICE; }
    @Override public String description() { return "The facility is freezing from the outside in. Mine to the warm core, freeze rivals, survive."; }
    @Override public boolean useCoreScoreboard() { return true; }

    // ================================================================== lifecycle

    @Override
    public void start(McmGameContext ctx) {
        if (phase != Phase.IDLE) stop();
        this.ctx = ctx;
        this.finished = false;
        plugin.saveDefaultConfig();
        var cfg = plugin.getConfig();
        PTS_CRATE = Math.max(0, cfg.getInt("coins.crate", 10));
        PTS_FREEZE = Math.max(0, cfg.getInt("coins.freeze", 25));
        PTS_FREEZE_ELIM = Math.max(0, cfg.getInt("coins.final-freeze-bonus", 0));
        PTS_SURVIVAL = Math.max(0, cfg.getInt("coins.outlive", 0));
        PLACEMENT_TEAM = com.mcm.api.McmCoins.toIntArray(cfg.getList("coins.placement"), PLACEMENT_TEAM);
        resetState();
        rng = new Random();

        world = ensureWorld();
        if (world == null) {
            Bukkit.getScheduler().runTask(plugin, () -> finishOnce("Meltdown world could not be created"));
            phase = Phase.ENDING;
            return;
        }

        Map<String, List<Player>> sides = ctx.sides();
        for (Map.Entry<String, List<Player>> e : sides.entrySet()) {
            List<UUID> ids = new ArrayList<>();
            for (Player p : e.getValue()) {
                if (p == null || !p.isOnline()) continue;
                ids.add(p.getUniqueId());
                sideOf.put(p.getUniqueId(), e.getKey());
                names.put(p.getUniqueId(), p.getName());
                alive.add(p.getUniqueId());
            }
            if (!ids.isEmpty()) {
                sideMembers.put(e.getKey(), ids);
                aliveSides.add(e.getKey());
            }
        }
        // anyone the core lists as participant but who is missing from sides becomes a solo side
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (sideOf.containsKey(p.getUniqueId())) continue;
            String key = "SOLO:" + p.getUniqueId();
            sideMembers.put(key, new ArrayList<>(List.of(p.getUniqueId())));
            sideOf.put(p.getUniqueId(), key);
            names.put(p.getUniqueId(), p.getName());
            alive.add(p.getUniqueId());
            aliveSides.add(key);
        }
        initialPlayers = alive.size();
        initialSides = aliveSides.size();
        if (initialPlayers == 0) {
            phase = Phase.ENDING;
            Bukkit.getScheduler().runTask(plugin, () -> finishOnce("No players"));
            return;
        }

        arena = new Arena(world, rng);
        scheduleRooms();
        assignSpawns();
        arena.decorate();
        cratesLeft = arena.crateCount();

        // keep the arena chunks loaded while we play
        for (int cx = (Arena.OX - 8) >> 4; cx <= (Arena.OX + Arena.SIZE + 8) >> 4; cx++)
            for (int cz = (Arena.OZ - 8) >> 4; cz <= (Arena.OZ + Arena.SIZE + 8) >> 4; cz++) {
                world.addPluginChunkTicket(cx, cz, plugin);
                ticketChunks.add(((long) cx << 32) | (cz & 0xFFFFFFFFL));
            }
        world.getEntities().stream().filter(en -> !(en instanceof Player)).forEach(Entity::remove);

        phase = Phase.BUILDING;
        for (Player p : onlineAlive()) {
            p.showTitle(title("§b§lMELTDOWN", "§7Building the facility..."));
        }
        buildTask = arena.build(plugin, () -> {
            buildTask = null;
            if (phase != Phase.BUILDING) return;
            beginCountdown();
        });
    }

    @Override
    public void stop() {
        cancelTasks();
        if (phase != Phase.IDLE) {
            cleanupPlayers();
            clearWorldExtras();
        }
        phase = Phase.IDLE;
        finished = true;
    }

    private void resetState() {
        sideOf.clear(); sideMembers.clear(); names.clear(); alive.clear(); aliveSides.clear();
        spawnOf.clear(); sideColor.clear(); frozen.clear(); heaters.clear(); heaterOf.clear();
        heaterReturnAt.clear(); warmerUntil.clear(); lastState.clear(); lastRoom.clear(); announcedRings.clear();
        elapsed = 0; countdown = COUNTDOWN_SECONDS; cratesLeft = 0;
    }

    private void cancelTasks() {
        if (buildTask != null) { buildTask.cancel(); buildTask = null; }
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        if (endTask != null) { endTask.cancel(); endTask = null; }
    }

    private void finishOnce(String reason) {
        if (finished) return;
        finished = true;
        cancelTasks();
        cleanupPlayers();
        clearWorldExtras();
        phase = Phase.IDLE;
        McmGameContext c = ctx;
        if (c != null) c.finish(reason);
    }

    private World ensureWorld() {
        World w = Bukkit.getWorld(WORLD_NAME);
        if (w == null) {
            try {
                WorldCreator wc = new WorldCreator(WORLD_NAME)
                        .generator(new VoidGenerator())
                        .environment(World.Environment.NORMAL)
                        .generateStructures(false);
                w = wc.createWorld();
            } catch (Throwable t) {
                plugin.getLogger().severe("Could not create " + WORLD_NAME + ": " + t);
                return null;
            }
        }
        if (w == null) return null;
        w.setSpawnLocation(Arena.CENTER_X, Arena.SPECTATOR_Y, Arena.CENTER_Z);
        w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        w.setGameRule(GameRule.FALL_DAMAGE, false);
        w.setGameRule(GameRule.KEEP_INVENTORY, true);
        w.setGameRule(GameRule.NATURAL_REGENERATION, false);
        w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        w.setGameRule(GameRule.DO_FIRE_TICK, false);
        w.setGameRule(GameRule.DO_TILE_DROPS, false);
        w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        w.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        w.setStorm(false);
        w.setThundering(false);
        w.setTime(6000);
        w.setDifficulty(Difficulty.EASY);
        return w;
    }

    private void cleanupPlayers() {
        for (UUID id : new ArrayList<>(sideOf.keySet())) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) cleanupPlayer(p);
        }
    }

    void cleanupPlayer(Player p) {
        p.getInventory().clear();
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
        p.setFreezeTicks(0);
        p.setGlowing(false);
        p.setFireTicks(0);
        AttributeHelper.resetHealth(p);
        p.setFoodLevel(20);
        p.setGameMode(GameMode.SURVIVAL);
    }

    private void clearWorldExtras() {
        for (Heater h : new ArrayList<>(heaters.values())) {
            if (h.block.getType() == Arena.HEATER) h.block.setType(Material.AIR, false);
        }
        heaters.clear();
        heaterOf.clear();
        if (world != null) {
            for (Entity e : world.getEntities()) if (!(e instanceof Player)) e.remove();
            for (long k : ticketChunks) world.removePluginChunkTicket((int) (k >> 32), (int) k, plugin);
        }
        ticketChunks.clear();
    }

    // ================================================================== setup

    private void scheduleRooms() {
        // freeze windows per ring: outer ring first, core last
        int[][] windows = {
                {395, 395},  // ring 0 (core)
                {290, 360},  // ring 1
                {190, 270},  // ring 2
                {80, 170}    // ring 3 (outer)
        };
        for (int ring = 0; ring <= Arena.N / 2; ring++) {
            List<Room> list = new ArrayList<>();
            for (Room r : arena.allRooms()) if (r.ring == ring) list.add(r);
            Collections.shuffle(list, rng);
            int[] w = windows[Math.min(ring, windows.length - 1)];
            for (int k = 0; k < list.size(); k++) {
                Room r = list.get(k);
                int t = list.size() == 1 ? w[0] : w[0] + (w[1] - w[0]) * k / (list.size() - 1);
                r.freezeAt = t;
                r.warnAt = t - WARN_LEAD;
            }
        }
    }

    private static final Material[] SOLO_PADS = {
            Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.BROWN_CONCRETE, Material.MAGENTA_CONCRETE,
            Material.LIGHT_BLUE_CONCRETE, Material.GRAY_CONCRETE, Material.BLACK_CONCRETE};
    private static final Color[] SOLO_COLORS = {
            Color.WHITE, Color.SILVER, Color.fromRGB(0x835432), Color.FUCHSIA, Color.fromRGB(0x8FD3FF), Color.GRAY, Color.BLACK};

    private void assignSpawns() {
        List<Room> outer = arena.outerRooms();
        List<String> keys = new ArrayList<>(sideMembers.keySet());
        Collections.shuffle(keys, rng);
        int n = keys.size();
        int offset = rng.nextInt(outer.size());
        int soloIdx = 0;
        for (int k = 0; k < n; k++) {
            Room room;
            int slot;
            if (n <= outer.size()) {
                room = outer.get((offset + k * outer.size() / n) % outer.size());
                slot = 0;
            } else {
                room = outer.get((offset + k) % outer.size());
                slot = k / outer.size();
            }
            String key = keys.get(k);
            Material pad;
            Color color;
            String team = key.startsWith("TEAM:") ? key.substring(5) : null;
            if (team != null) {
                ChatColor cc = ctx.teamColor(team);
                pad = padFor(cc);
                color = colorFor(cc);
            } else {
                pad = SOLO_PADS[soloIdx % SOLO_PADS.length];
                color = SOLO_COLORS[soloIdx % SOLO_COLORS.length];
                soloIdx++;
            }
            int off = slot == 0 ? 0 : (slot % 2 == 1 ? 4 : -4);
            spawnOf.put(key, arena.addPad(room.centerX() + off, room.centerZ() + off, pad));
            sideColor.put(key, color);
        }
    }

    static Material padFor(ChatColor c) {
        if (c == null) return Material.WHITE_CONCRETE;
        return switch (c) {
            case RED, DARK_RED -> Material.RED_CONCRETE;
            case GOLD -> Material.ORANGE_CONCRETE;
            case YELLOW -> Material.YELLOW_CONCRETE;
            case GREEN -> Material.LIME_CONCRETE;
            case DARK_GREEN -> Material.GREEN_CONCRETE;
            case DARK_AQUA -> Material.CYAN_CONCRETE;
            case AQUA -> Material.LIGHT_BLUE_CONCRETE;
            case BLUE, DARK_BLUE -> Material.BLUE_CONCRETE;
            case DARK_PURPLE -> Material.PURPLE_CONCRETE;
            case LIGHT_PURPLE -> Material.PINK_CONCRETE;
            case BLACK -> Material.BLACK_CONCRETE;
            case GRAY, DARK_GRAY -> Material.GRAY_CONCRETE;
            default -> Material.WHITE_CONCRETE;
        };
    }

    static Color colorFor(ChatColor c) {
        if (c == null) return Color.WHITE;
        return switch (c) {
            case RED, DARK_RED -> Color.fromRGB(0xE03030);
            case GOLD -> Color.fromRGB(0xFF8C1A);
            case YELLOW -> Color.fromRGB(0xFFE033);
            case GREEN -> Color.fromRGB(0x7FE030);
            case DARK_GREEN -> Color.fromRGB(0x2E8B2E);
            case DARK_AQUA -> Color.fromRGB(0x16A3A3);
            case AQUA -> Color.fromRGB(0x55FFFF);
            case BLUE, DARK_BLUE -> Color.fromRGB(0x3050E0);
            case DARK_PURPLE -> Color.fromRGB(0x8A2BE2);
            case LIGHT_PURPLE -> Color.fromRGB(0xFF77CC);
            case BLACK -> Color.BLACK;
            case GRAY, DARK_GRAY -> Color.GRAY;
            default -> Color.WHITE;
        };
    }

    private void beginCountdown() {
        phase = Phase.COUNTDOWN;
        countdown = COUNTDOWN_SECONDS;
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            Location spawn = spawnOf.get(sideOf.get(id));
            if (spawn == null) spawn = new Location(world, Arena.CENTER_X + 0.5, Arena.SPECTATOR_Y, Arena.CENTER_Z + 0.5);
            Location to = spawn.clone().add(rng.nextDouble() * 1.6 - 0.8, 0, rng.nextDouble() * 1.6 - 0.8);
            to.setYaw((float) Math.toDegrees(Math.atan2(-(Arena.CENTER_X - to.getX()), Arena.CENTER_Z - to.getZ())));
            p.teleport(to);
            p.setGameMode(GameMode.SURVIVAL);
            cleanupPlayer(p);
            p.setGameMode(GameMode.SURVIVAL);
            p.setSaturation(0f);
            p.setFallDistance(0f);
            giveKit(p);
        }
        ctx.broadcast("§b§lMeltdown §7» §fThe facility is freezing from the outside in. Mine and run toward the §6warm core§f, "
                + "freeze rivals with your §bFrost Bow§f and thaw teammates with §6Heaters§f.");
        ctx.setTimer(TOTAL_SECONDS, TOTAL_SECONDS);
        updateSidebar();
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    // ================================================================== kit

    private ItemStack tagged(Material m, String id, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(text(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        List<Component> l = new ArrayList<>();
        for (String s : lore) l.add(text(s).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        meta.lore(l);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
        it.setItemMeta(meta);
        return it;
    }

    String itemId(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
    }

    ItemStack heaterItem() {
        return tagged(Material.CAMPFIRE, "heater", "§6Heater",
                "§7Place to warm a 4-block radius for " + HEATER_DURATION + "s.",
                "§7Protects your side from the cold",
                "§7and thaws frozen teammates.",
                "§8Enemies can punch it out.");
    }

    ItemStack warmerItem(int amount) {
        ItemStack it = tagged(Material.COAL, "warmer", "§eHand Warmer",
                "§7Right-click: +2 hearts and " + (WARMER_MILLIS / 1000) + "s of cold immunity.");
        it.setAmount(amount);
        return it;
    }

    private void giveKit(Player p) {
        PlayerInventory inv = p.getInventory();
        inv.clear();
        ItemStack pick = tagged(Material.IRON_PICKAXE, "pick", "§bIce Pick", "§7Mine through ice and snow walls", "§7and break §6coin crates§7.");
        ItemMeta pm = pick.getItemMeta();
        pm.addEnchant(Enchantment.EFFICIENCY, 3, true);
        pick.setItemMeta(pm);
        ItemStack bow = tagged(Material.BOW, "bow", "§bFrost Bow", "§7One hit freezes an enemy solid.", "§7Frozen players shatter unless thawed.");
        ItemMeta bm = bow.getItemMeta();
        bm.addEnchant(Enchantment.INFINITY, 1, true);
        bow.setItemMeta(bm);
        ItemStack map = tagged(Material.COMPASS, "map", "§fFacility Map", "§7Points to the warm core.", "§7Right-click to view the room map.");
        CompassMeta cm = (CompassMeta) map.getItemMeta();
        cm.setLodestone(new Location(world, Arena.CENTER_X + 0.5, Arena.FLOOR_Y + 1, Arena.CENTER_Z + 0.5));
        cm.setLodestoneTracked(false);
        map.setItemMeta(cm);

        inv.setItem(0, pick);
        inv.setItem(1, bow);
        inv.setItem(2, heaterItem());
        inv.setItem(3, warmerItem(2));
        inv.setItem(8, map);
        inv.setItem(17, new ItemStack(Material.ARROW));

        Color color = sideColor.getOrDefault(sideOf.get(p.getUniqueId()), Color.WHITE);
        inv.setChestplate(leather(Material.LEATHER_CHESTPLATE, color));
        inv.setLeggings(leather(Material.LEATHER_LEGGINGS, color));
        inv.setHeldItemSlot(0);
    }

    private ItemStack leather(Material m, Color c) {
        ItemStack it = new ItemStack(m);
        LeatherArmorMeta meta = (LeatherArmorMeta) it.getItemMeta();
        meta.setColor(c);
        meta.setUnbreakable(true);
        meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DYE);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, "armor");
        it.setItemMeta(meta);
        return it;
    }

    // ================================================================== main loop

    private void tick() {
        if (phase == Phase.COUNTDOWN) {
            countdown--;
            for (Player p : onlineAlive()) {
                if (countdown > 0) {
                    if (countdown <= 5 || countdown == COUNTDOWN_SECONDS - 1) {
                        p.showTitle(title("§b" + countdown, "§7Head for the warm core!"));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.2f);
                    }
                } else {
                    p.showTitle(title("§b§lGO!", "§7The cold is coming..."));
                    p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.6f);
                }
            }
            if (countdown <= 0) phase = Phase.RUNNING;
            updateSidebar();
            return;
        }
        if (phase != Phase.RUNNING) return;

        elapsed++;
        ctx.setTimer(Math.max(0, TOTAL_SECONDS - elapsed), TOTAL_SECONDS);

        // disconnected players
        for (UUID id : new ArrayList<>(alive)) {
            Player p = Bukkit.getPlayer(id);
            if (p == null || !p.isOnline() || p.getWorld() != world) eliminate(id, "left the facility", true);
        }

        updateRooms();
        updateHeaters();
        for (UUID id : new ArrayList<>(alive)) {
            if (!alive.contains(id)) continue;
            Player p = Bukkit.getPlayer(id);
            if (p != null) tickPlayer(p);
        }
        for (String side : new ArrayList<>(aliveSides)) checkSideWipe(side);
        updateSidebar();

        boolean timeUp = elapsed >= TOTAL_SECONDS;
        if (timeUp || aliveSides.isEmpty() || (initialSides > 1 && aliveSides.size() <= 1)) {
            endGame(timeUp ? "Time up - the whole facility froze" : "Last side standing");
        }
    }

    private void updateRooms() {
        for (Room r : arena.allRooms()) {
            if (r.state == Room.State.SAFE && elapsed >= r.warnAt) {
                r.state = Room.State.WARNING;
                arena.applyWarning(r);
                if (announcedRings.add(r.ring)) {
                    ctx.broadcast("§b§lMeltdown §7» §fThe §b" + Room.ringName(r.ring) + "§f is starting to freeze!");
                }
            } else if (r.state == Room.State.WARNING && elapsed >= r.freezeAt) {
                r.state = Room.State.FROZEN;
                r.frozenSince = elapsed;
                arena.applyFrozen(r);
                cratesLeft -= arena.freezeCrates(r);
                world.playSound(new Location(world, r.centerX(), Arena.FLOOR_Y + 2, r.centerZ()), Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
                if (r.ring == 0) {
                    ctx.broadcast("§b§lMeltdown §7» §cThe warm core has frozen! Nowhere left to hide.");
                }
            }
            if (r.state == Room.State.FROZEN) arena.freezeStep(r);
            if (r.state != Room.State.SAFE && (elapsed & 1) == 0) {
                world.spawnParticle(Particle.SNOWFLAKE, r.centerX() + 0.5, Arena.FLOOR_Y + 3, r.centerZ() + 0.5,
                        r.state == Room.State.FROZEN ? 40 : 15, 6, 2, 6, 0.01);
            }
        }
    }

    private void updateHeaters() {
        for (Heater h : new ArrayList<>(heaters.values())) {
            if (h.block.getType() != Arena.HEATER || elapsed >= h.expiresAt) {
                removeHeater(h, h.block.getType() == Arena.HEATER ? "§7Your heater burned out." : null);
                continue;
            }
            Location c = h.block.getLocation().add(0.5, 0.5, 0.5);
            world.spawnParticle(Particle.FLAME, c, 6, 2.0, 0.4, 2.0, 0.0);
        }
        // return heater items after cooldown
        for (Map.Entry<UUID, Integer> e : new ArrayList<>(heaterReturnAt.entrySet())) {
            if (elapsed < e.getValue()) continue;
            heaterReturnAt.remove(e.getKey());
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null || !alive.contains(e.getKey())) continue;
            p.getInventory().addItem(heaterItem());
            p.sendMessage("§6Your heater is ready again.");
            p.playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.6f, 1.4f);
        }
    }

    void removeHeater(Heater h, String msgToOwner) {
        heaters.remove(Arena.key(h.block.getX(), h.block.getY(), h.block.getZ()));
        if (heaterOf.get(h.owner) == h) heaterOf.remove(h.owner);
        if (h.block.getType() == Arena.HEATER) h.block.setType(Material.AIR, false);
        world.spawnParticle(Particle.LARGE_SMOKE, h.block.getLocation().add(0.5, 0.5, 0.5), 10, 0.3, 0.3, 0.3, 0.01);
        if (phase == Phase.RUNNING && alive.contains(h.owner)) heaterReturnAt.put(h.owner, elapsed + HEATER_COOLDOWN);
        Player owner = Bukkit.getPlayer(h.owner);
        if (owner != null && msgToOwner != null) owner.sendMessage(msgToOwner + " §8(back in " + HEATER_COOLDOWN + "s)");
    }

    private int heatersNear(Location loc, String side) {
        int n = 0;
        for (Heater h : heaters.values()) {
            if (!h.side.equals(side)) continue;
            if (h.block.getLocation().add(0.5, 0.5, 0.5).distanceSquared(loc) <= HEATER_RADIUS_SQ) n++;
        }
        return n;
    }

    private void tickPlayer(Player p) {
        UUID id = p.getUniqueId();
        String side = sideOf.get(id);
        Room room = arena.roomAt(p.getLocation());
        Room.State st = room == null ? Room.State.SAFE : room.state;
        int heat = heatersNear(p.getLocation(), side);
        boolean warmer = warmerUntil.getOrDefault(id, 0L) > System.currentTimeMillis();
        boolean absoluteZero = st == Room.State.FROZEN && elapsed - room.frozenSince >= ABSOLUTE_ZERO_AFTER;
        boolean protectedNow = !absoluteZero && (heat > 0 || warmer);
        Frozen fz = frozen.get(id);

        // state change titles
        Room.State prev = lastState.put(id, st);
        Room prevRoom = lastRoom.put(id, room);
        if (room != null && (prev != st || prevRoom != room)) {
            if (st == Room.State.WARNING && (prev != Room.State.WARNING || prevRoom != room)) {
                p.showTitle(title("§e⚠ FREEZING ⚠", "§fRoom " + room.label + " freezes in " + Math.max(0, room.freezeAt - elapsed) + "s - move inward!"));
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            } else if (st == Room.State.FROZEN && prev != Room.State.FROZEN) {
                p.showTitle(title("§b❄ FROZEN ZONE ❄", "§cGet out or freeze to death!"));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.6f);
            }
        }
        if (st == Room.State.WARNING && (elapsed % 2 == 0)) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.6f, 0.5f);
        }

        // cold damage
        if (st == Room.State.FROZEN && !protectedNow) {
            int t = elapsed - room.frozenSince;
            double dmg = t < 15 ? 1.0 : t < 35 ? 2.0 : 4.0;
            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40, 1, false, false, true));
            p.setFreezeTicks(p.getMaxFreezeTicks());
            if (!hurt(p, dmg)) {
                eliminate(id, fz != null ? "shattered in the cold" : "froze to death in room " + room.label, false);
                return;
            }
        } else if (fz == null) {
            p.setFreezeTicks(Math.max(0, p.getFreezeTicks() - 40));
        }

        // heater healing for own side
        if (heat > 0 && !absoluteZero && fz == null && (elapsed % 2 == 0)) heal(p, 1.0);

        // frozen state: thaw or shatter
        if (fz != null) {
            p.setFreezeTicks(p.getMaxFreezeTicks());
            if (heat > 0 && !absoluteZero) {
                fz.thaw += heat;
                world.spawnParticle(Particle.FALLING_WATER, p.getLocation().add(0, 1, 0), 10, 0.3, 0.6, 0.3, 0);
                if (fz.thaw >= THAW_NEEDED) {
                    thaw(p);
                    fz = null;
                }
            }
            if (fz != null && elapsed - fz.since >= SHATTER_AFTER) {
                eliminate(id, "shattered", false);
                return;
            }
            if (fz != null) {
                int left = SHATTER_AFTER - (elapsed - fz.since);
                p.sendActionBar(text("§b❄ FROZEN §7- thaw " + (int) Math.min(100, fz.thaw * 100 / THAW_NEEDED) + "% §8| §cshatters in " + left + "s"));
                world.spawnParticle(Particle.SNOWFLAKE, p.getLocation().add(0, 1, 0), 8, 0.3, 0.6, 0.3, 0);
                return;
            }
        }

        // action bar
        StringBuilder sb = new StringBuilder();
        if (room != null) sb.append("§7Room §f").append(room.label).append(" §8| ");
        switch (st) {
            case SAFE -> sb.append("§aSafe");
            case WARNING -> sb.append("§eFreezing in ").append(Math.max(0, room.freezeAt - elapsed)).append("s");
            case FROZEN -> sb.append(protectedNow ? "§6Warmed" : "§b❄ FROZEN §c-" + ((elapsed - room.frozenSince) < 15 ? "½" : (elapsed - room.frozenSince) < 35 ? "1" : "2") + "❤/s");
        }
        if (absoluteZero) sb.append(" §4(absolute zero)");
        double dist = Math.hypot(Arena.CENTER_X + 0.5 - p.getLocation().getX(), Arena.CENTER_Z + 0.5 - p.getLocation().getZ());
        sb.append(" §8| §7Core §f").append((int) dist).append("m");
        Heater mine = heaterOf.get(id);
        Integer ret = heaterReturnAt.get(id);
        if (mine != null) sb.append(" §8| §6Heater ").append(Math.max(0, mine.expiresAt - elapsed)).append("s");
        else if (ret != null) sb.append(" §8| §7Heater in ").append(Math.max(0, ret - elapsed)).append("s");
        p.sendActionBar(text(sb.toString()));
    }

    /** Applies damage manually; returns false if the player would die. */
    private boolean hurt(Player p, double dmg) {
        double hp = p.getHealth() - dmg;
        if (hp <= 0.0) return false;
        p.setHealth(hp);
        p.playHurtAnimation(0f);
        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 0.8f, 1f);
        return true;
    }

    private void heal(Player p, double amount) {
        double max = AttributeHelper.maxHealth(p);
        p.setHealth(Math.min(max, p.getHealth() + amount));
    }

    // ================================================================== PvP: freeze / thaw

    void onArrowHit(Player shooter, Player victim) {
        if (phase != Phase.RUNNING) return;
        UUID s = shooter.getUniqueId(), v = victim.getUniqueId();
        if (!alive.contains(s) || !alive.contains(v) || s.equals(v)) return;
        if (frozen.containsKey(s) || frozen.containsKey(v)) return;
        String side = sideOf.get(v);
        if (Objects.equals(sideOf.get(s), side)) return;

        frozen.put(v, new Frozen(s, elapsed));
        victim.getInventory().setHelmet(new ItemStack(Material.ICE));
        victim.setGlowing(true);
        victim.setFreezeTicks(victim.getMaxFreezeTicks());
        victim.showTitle(title("§b§l❄ FROZEN ❄", "§7A teammate's heater can thaw you"));
        victim.playSound(victim.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.5f);
        world.spawnParticle(Particle.SNOWFLAKE, victim.getLocation().add(0, 1, 0), 40, 0.4, 0.8, 0.4, 0.05);
        if (PTS_FREEZE > 0) ctx.addKillCoins(s, PTS_FREEZE, v, victim.getName(), "You froze " + victim.getName());
        else shooter.playSound(shooter.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.6f);
        ctx.broadcast("§b❄ " + coloredName(v) + " §7was frozen by " + coloredName(s));
        for (UUID mate : sideMembers.getOrDefault(side, List.of())) {
            Player m = Bukkit.getPlayer(mate);
            if (m != null && !mate.equals(v) && alive.contains(mate)) {
                m.sendMessage("§c" + victim.getName() + " is frozen! Place a §6Heater §cnear them to thaw them.");
            }
        }
        checkSideWipe(side);
    }

    private void thaw(Player p) {
        Frozen fz = frozen.remove(p.getUniqueId());
        // MCC: the freeze coins are taken away again when the frozen player is thawed out
        if (fz != null && fz.by != null && PTS_FREEZE > 0 && phase == Phase.RUNNING)
            ctx.addCoins(fz.by, -PTS_FREEZE, p.getName() + " was unfrozen by their team");
        ItemStack helm = p.getInventory().getHelmet();
        if (helm != null && helm.getType() == Material.ICE) p.getInventory().setHelmet(null);
        p.setGlowing(false);
        p.setFreezeTicks(0);
        p.showTitle(title("§6§lTHAWED", "§7Back in the fight!"));
        p.playSound(p.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1.2f);
        ctx.broadcast("§6☀ " + coloredName(p.getUniqueId()) + " §7was thawed out.");
    }

    void checkSideWipe(String side) {
        if (side == null || !aliveSides.contains(side)) return;
        List<UUID> living = new ArrayList<>();
        for (UUID m : sideMembers.getOrDefault(side, List.of())) if (alive.contains(m)) living.add(m);
        if (living.isEmpty()) { sideEliminated(side); return; }
        for (UUID m : living) if (!frozen.containsKey(m)) return;
        for (UUID m : living) eliminate(m, "frozen with no one left to thaw them", false);
    }

    // ================================================================== elimination & scoring

    void eliminate(UUID id, String why, boolean quit) {
        if (!alive.remove(id)) return;
        Frozen fz = frozen.remove(id);
        Heater h = heaterOf.get(id);
        if (h != null) removeHeater(h, null);
        heaterReturnAt.remove(id);
        warmerUntil.remove(id);
        Player p = Bukkit.getPlayer(id);

        if (fz != null && fz.by != null && phase == Phase.RUNNING && Bukkit.getPlayer(fz.by) != null) {
            if (PTS_FREEZE_ELIM > 0) {
                String vn = names.getOrDefault(id, "a player");
                ctx.addKillCoins(fz.by, PTS_FREEZE_ELIM, id, vn, "You eliminated " + vn);
            }
        }
        if (p != null && p.isOnline()) {
            if (quit) {
                cleanupPlayer(p);
            } else {
                p.getInventory().clear();
                p.setGlowing(false);
                for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
                p.setFreezeTicks(0);
                p.setGameMode(GameMode.SPECTATOR);
                p.teleport(new Location(world, Arena.CENTER_X + 0.5, Arena.SPECTATOR_Y, Arena.CENTER_Z + 0.5, 0f, 70f));
                p.showTitle(title("§c§lELIMINATED", "§7You " + why + "."));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_HURT_FREEZE, 1f, 0.5f);
            }
        }
        ctx.broadcast("§c☠ " + coloredName(id) + " §7" + why + ". §8(" + alive.size() + " left)");

        if (!quit && phase == Phase.RUNNING) {
            for (UUID other : new ArrayList<>(alive)) {
                if (PTS_SURVIVAL > 0 && Bukkit.getPlayer(other) != null) ctx.addCoins(other, PTS_SURVIVAL, names.getOrDefault(id, "A player") + " was eliminated");
            }
        }
        String side = sideOf.get(id);
        checkSideWipe(side);
    }

    private void sideEliminated(String side) {
        if (!aliveSides.remove(side)) return;
        int place = aliveSides.size() + 1;
        awardPlacement(side, place);
        ctx.broadcast("§b§lMeltdown §7» " + ctx.sideLabel(side) + " §7has been wiped out §8(#" + place + ")");
    }

    private void awardPlacement(String side, int place) {
        int pool = com.mcm.api.McmCoins.placementOrLast(PLACEMENT_TEAM, place);
        List<UUID> on = new ArrayList<>();
        for (UUID m : sideMembers.getOrDefault(side, List.of())) if (Bukkit.getPlayer(m) != null) on.add(m);
        double each = ctx.teamShare(pool, on.size());
        for (UUID m : on) if (each > 0) ctx.addCoins(m, each, "Placed #" + place);
    }

    private void endGame(String reason) {
        if (phase != Phase.RUNNING) return;
        phase = Phase.ENDING;
        // rank remaining sides: most unfrozen survivors, then most survivors, then total health
        List<String> remaining = new ArrayList<>(aliveSides);
        Map<String, double[]> score = new HashMap<>();
        for (String s : remaining) {
            double unfrozen = 0, living = 0, hp = 0;
            for (UUID m : sideMembers.getOrDefault(s, List.of())) {
                if (!alive.contains(m)) continue;
                living++;
                if (!frozen.containsKey(m)) unfrozen++;
                Player p = Bukkit.getPlayer(m);
                if (p != null) hp += p.getHealth();
            }
            score.put(s, new double[]{unfrozen, living, hp});
        }
        Comparator<String> cmp = (a, b) -> {
            double[] x = score.get(a), y = score.get(b);
            for (int k = 0; k < 3; k++) { int c = Double.compare(y[k], x[k]); if (c != 0) return c; }
            return 0;
        };
        remaining.sort(cmp);
        int place = 1;
        List<String> winners = new ArrayList<>();
        for (int k = 0; k < remaining.size(); k++) {
            if (k > 0 && cmp.compare(remaining.get(k - 1), remaining.get(k)) != 0) place = k + 1;
            awardPlacement(remaining.get(k), place);
            if (place == 1) winners.add(remaining.get(k));
        }
        aliveSides.clear();

        String winText = winners.isEmpty() ? "§7Nobody survived the freeze." : "§fWinner: " + String.join("§7, ", winners.stream().map(ctx::sideLabel).toList());
        ctx.broadcast("§b§lMeltdown §7» §f" + reason + ". " + winText);
        for (UUID id : sideOf.keySet()) {
            Player p = Bukkit.getPlayer(id);
            if (p == null) continue;
            p.showTitle(title("§b§lMELTDOWN OVER", winText));
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1f);
        }
        ctx.setTimer(0, TOTAL_SECONDS);
        updateSidebar();
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        endTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            endTask = null;
            finishOnce(reason);
        }, 120L);
    }

    void onCrateBroken(Player p, Block b) {
        arena.removeCrate(b);
        cratesLeft = Math.max(0, cratesLeft - 1);
        // MCC: a crate's coins are shared amongst the team
        String cs = sideOf.get(p.getUniqueId());
        List<UUID> mates = new ArrayList<>();
        for (UUID m : sideMembers.getOrDefault(cs, List.of(p.getUniqueId()))) if (Bukkit.getPlayer(m) != null) mates.add(m);
        if (mates.isEmpty()) mates.add(p.getUniqueId());
        double share = ctx.teamShare(PTS_CRATE, mates.size());
        for (UUID m : mates) if (share > 0) ctx.addCoins(m, share, "Coin crate");
        p.playSound(b.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1.2f);
        world.spawnParticle(Particle.WAX_OFF, b.getLocation().add(0.5, 0.5, 0.5), 15, 0.4, 0.4, 0.4, 0.1);
        if (rng.nextDouble() < 0.35) {
            p.getInventory().addItem(warmerItem(1));
            p.sendMessage("§eThe crate had a Hand Warmer inside!");
        }
    }

    void onHeaterPlaced(Player p, Block b) {
        Heater h = new Heater(b, sideOf.get(p.getUniqueId()), p.getUniqueId(), elapsed + HEATER_DURATION);
        heaters.put(Arena.key(b.getX(), b.getY(), b.getZ()), h);
        heaterOf.put(p.getUniqueId(), h);
        p.playSound(b.getLocation(), Sound.ITEM_FIRECHARGE_USE, 1f, 1f);
    }

    Heater heaterAt(Block b) {
        if (b.getWorld() != world) return null;
        return heaters.get(Arena.key(b.getX(), b.getY(), b.getZ()));
    }

    void useWarmer(Player p) {
        if (p.hasCooldown(Material.COAL)) return;
        ItemStack item = p.getInventory().getItemInMainHand();
        if (!"warmer".equals(itemId(item))) return;
        item.setAmount(item.getAmount() - 1);
        p.getInventory().setItemInMainHand(item.getAmount() <= 0 ? null : item);
        warmerUntil.put(p.getUniqueId(), System.currentTimeMillis() + WARMER_MILLIS);
        heal(p, 4.0);
        p.setFreezeTicks(0);
        p.setCooldown(Material.COAL, 20);
        p.playSound(p.getLocation(), Sound.ITEM_FIRECHARGE_USE, 0.8f, 1.6f);
        world.spawnParticle(Particle.FLAME, p.getLocation().add(0, 1, 0), 15, 0.3, 0.5, 0.3, 0.02);
    }

    void showMap(Player p) {
        Room here = arena.roomAt(p.getLocation());
        p.sendMessage("§b§lFacility map §7(§a■ safe §e■ freezing §b■ frozen §6◆ you§7) §8N ↑");
        for (int j = 0; j < Arena.N; j++) {
            StringBuilder sb = new StringBuilder("§8 ").append(j + 1).append(" ");
            for (int i = 0; i < Arena.N; i++) {
                Room r = arena.rooms[i][j];
                if (r == here) { sb.append("§6◆ "); continue; }
                String col = switch (r.state) {
                    case SAFE -> r.ring == 0 ? "§6" : "§a";
                    case WARNING -> "§e";
                    case FROZEN -> "§b";
                };
                sb.append(col).append("■ ");
            }
            p.sendMessage(sb.toString());
        }
        StringBuilder letters = new StringBuilder("§8   ");
        for (int i = 0; i < Arena.N; i++) letters.append((char) ('A' + i)).append(' ');
        p.sendMessage(letters.toString());
    }

    // ================================================================== HUD helpers

    private void updateSidebar() {
        if (ctx == null || arena == null) return;
        int frozenRooms = 0, total = 0, nextFreeze = Integer.MAX_VALUE, front = -1;
        for (Room r : arena.allRooms()) {
            total++;
            if (r.state == Room.State.FROZEN) frozenRooms++;
            else {
                nextFreeze = Math.min(nextFreeze, r.freezeAt);
                front = Math.max(front, r.ring);
            }
        }
        List<String> lines = new ArrayList<>();
        lines.add("§fAlive: §b" + alive.size() + "§7/" + initialPlayers);
        lines.add("§fSides alive: §b" + aliveSides.size() + "§7/" + initialSides);
        lines.add("§fFreezing: §b" + (front < 0 ? "Everything" : Room.ringName(front)));
        lines.add("§fRooms frozen: §b" + frozenRooms + "§7/" + total);
        if (nextFreeze != Integer.MAX_VALUE && phase == Phase.RUNNING) {
            lines.add("§fNext freeze: §e" + Math.max(0, nextFreeze - elapsed) + "s");
        } else if (phase == Phase.COUNTDOWN) {
            lines.add("§fStarting in: §e" + Math.max(0, countdown) + "s");
        }
        lines.add("§fCoin crates: §6" + Math.max(0, cratesLeft));
        ctx.setSidebarLines(lines);
    }

    String coloredName(UUID id) {
        String side = sideOf.get(id);
        String name = names.getOrDefault(id, "?");
        if (side != null && side.startsWith("TEAM:")) return ctx.teamColor(side.substring(5)) + name;
        return "§f" + name;
    }

    private List<Player> onlineAlive() {
        List<Player> list = new ArrayList<>();
        for (UUID id : alive) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) list.add(p);
        }
        return list;
    }

    static Component text(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    static Title title(String a, String b) {
        return Title.title(text(a), text(b), Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1800), Duration.ofMillis(400)));
    }

    // ================================================================== queries for the listener

    boolean active() { return phase != Phase.IDLE && world != null; }
    MeltdownPlugin plugin() { return plugin; }

    Location lobbyOrSpawn() {
        try {
            if (ctx != null && ctx.lobby() != null) return ctx.lobby();
        } catch (Throwable ignored) {
            // stale context
        }
        for (World w : Bukkit.getWorlds()) if (!w.getName().equals(WORLD_NAME)) return w.getSpawnLocation();
        return null;
    }
    boolean inWorld(Entity e) { return world != null && e.getWorld() == world; }
    boolean isAlive(UUID id) { return alive.contains(id); }
    boolean isFrozen(UUID id) { return frozen.containsKey(id); }
    boolean isParticipant(UUID id) { return sideOf.containsKey(id); }
}
