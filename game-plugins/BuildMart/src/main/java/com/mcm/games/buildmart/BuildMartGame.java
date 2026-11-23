package com.mcm.games.buildmart;

import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;

/**
 * Build Mart: every side gets a plot and a pedestal showing a small target build. Gather the exact
 * blocks at the central mart (regenerating stalls + shop counters) and copy the build on your plot.
 */
public final class BuildMartGame implements McmGame, Listener {
    static final String WORLD = "mcc_buildmart";
    private static final int FLOOR_Y = 64;
    private static final int GAME_SECONDS = 480;
    private static final int PREGAME_SECONDS = 10;
    private static final int NORMAL_REGEN_TICKS = 60;
    private static final int SALE_REGEN_TICKS = 10;
    private static final int SALE_EVERY = 75;
    private static final int SALE_LENGTH = 40;
    private static final int VOID_Y = 45;
    // Mart bounds (floor)
    private static final int MART_X = 25, MART_Z = 13;

    private static final Material[] RAW_STALLS = {
            Material.OAK_PLANKS, Material.OAK_LOG, Material.SPRUCE_PLANKS, Material.BIRCH_PLANKS,
            Material.COBBLESTONE, Material.SAND, Material.CLAY, Material.IRON_BLOCK, Material.GLOWSTONE,
            Material.QUARTZ_BLOCK, Material.SANDSTONE, Material.SNOW_BLOCK,
            Material.WHITE_WOOL, Material.RED_WOOL, Material.ORANGE_WOOL, Material.YELLOW_WOOL,
            Material.LIME_WOOL, Material.GREEN_WOOL, Material.LIGHT_BLUE_WOOL, Material.BLUE_WOOL,
            Material.BLACK_WOOL, Material.PINK_WOOL, Material.BROWN_WOOL,
            Material.OAK_LEAVES, Material.PUMPKIN, Material.HAY_BLOCK, Material.MELON
    };

    private static final Material[] SOLO_COLORS = {
            Material.WHITE_CONCRETE, Material.LIGHT_GRAY_CONCRETE, Material.BROWN_CONCRETE, Material.MAGENTA_CONCRETE,
            Material.CYAN_CONCRETE, Material.ORANGE_CONCRETE, Material.LIME_CONCRETE, Material.PINK_CONCRETE,
            Material.LIGHT_BLUE_CONCRETE, Material.YELLOW_CONCRETE, Material.PURPLE_CONCRETE, Material.GREEN_CONCRETE
    };

    // ---------------------------------------------------------------- model

    private static final class Stall {
        final Material material;
        final int x0, z0, height;
        final TextDisplay[] label = new TextDisplay[1];
        long saleUntilTick = -1;
        Stall(Material material, int x0, int z0, int height) { this.material = material; this.x0 = x0; this.z0 = z0; this.height = height; }
    }

    private record Cost(Material material, int amount) {}

    private static final class Shop {
        final String name;
        final List<Cost> costs;
        final Material product;
        final int productAmount;
        Shop(String name, Material product, int productAmount, Cost... costs) {
            this.name = name; this.product = product; this.productAmount = productAmount; this.costs = List.of(costs);
        }
    }

    private static final class Regen {
        final int x, y, z; final Material material; final long due;
        Regen(int x, int y, int z, Material material, long due) { this.x = x; this.y = y; this.z = z; this.material = material; this.due = due; }
    }

    private static final class Area {
        final String side;
        final int cx, cz;
        final int plotX, plotZ, tgtX, tgtZ;
        final Material color;
        Location spawn;
        int seqIndex;
        int completed;
        long buildStartTick;
        int lastCorrect, lastNeeded, lastWrong;
        TextDisplay targetLabel, plotLabel;
        Area(String side, int cx, int cz, Material color) {
            this.side = side; this.cx = cx; this.cz = cz; this.color = color;
            plotX = cx - 7; plotZ = cz - 2;
            tgtX = cx + 3; tgtZ = cz - 2;
        }
    }

    // ---------------------------------------------------------------- state

    private final JavaPlugin plugin;
    private final NamespacedKey toolKey;
    private final Random random = new Random();

    private McmGameContext ctx;
    private World world;
    private boolean running;   // our listeners are active
    private boolean playing;   // building/gathering allowed
    private BukkitTask task;
    private long tick;
    private int secondsLeft;
    private int pregameLeft;

    private final List<int[]> arenaBlocks = new ArrayList<>();
    private final Set<Long> arenaKeys = new HashSet<>();
    private final Set<Long> forcedChunks = new HashSet<>();
    private final List<Entity> entities = new ArrayList<>();
    private final Map<Long, Stall> stallBlocks = new HashMap<>();
    private final List<Stall> stalls = new ArrayList<>();
    private final Map<Long, Shop> shopBlocks = new HashMap<>();
    private final Map<Long, Regen> regen = new HashMap<>();
    private final Map<String, Area> areas = new LinkedHashMap<>();
    private final Map<UUID, String> sideOf = new HashMap<>();
    private final Map<UUID, Long> shopCooldown = new HashMap<>();
    private final Set<String> dirty = new HashSet<>();
    private final List<BuildPreset> sequence = new ArrayList<>();
    private final Map<Integer, Integer> finishersPerIndex = new HashMap<>();

    public BuildMartGame(JavaPlugin plugin) {
        this.plugin = plugin;
        this.toolKey = new NamespacedKey(plugin, "tool");
    }

    @Override public String id() { return "buildmart"; }
    @Override public String displayName() { return "Build Mart"; }
    @Override public Material icon() { return Material.BRICKS; }
    @Override public String description() { return "Gather blocks at the mart and copy the target builds on your plot."; }
    @Override public boolean useCoreScoreboard() { return true; }

    // ---------------------------------------------------------------- lifecycle

    World ensureWorld() {
        World w = Bukkit.getWorld(WORLD);
        if (w == null) {
            try {
                w = new WorldCreator(WORLD).generator(new VoidGenerator()).environment(World.Environment.NORMAL)
                        .generateStructures(false).createWorld();
            } catch (Throwable t) {
                plugin.getLogger().log(Level.SEVERE, "Could not create world " + WORLD, t);
                return null;
            }
        }
        if (w != null) {
            w.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            w.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            w.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
            w.setGameRule(GameRule.FALL_DAMAGE, false);
            w.setGameRule(GameRule.KEEP_INVENTORY, true);
            w.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
            w.setGameRule(GameRule.DO_FIRE_TICK, false);
            w.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
            w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            w.setGameRule(GameRule.DO_TILE_DROPS, false);
            w.setGameRule(GameRule.MOB_GRIEFING, false);
            w.setTime(6000);
            w.setStorm(false);
            w.setThundering(false);
            w.setDifficulty(Difficulty.PEACEFUL);
            w.setSpawnLocation(0, FLOOR_Y + 1, 0);
        }
        world = w;
        return w;
    }

    @Override
    public void start(McmGameContext context) {
        stop();
        this.ctx = context;
        try {
            if (ensureWorld() == null) throw new IllegalStateException("world " + WORLD + " unavailable");
            tick = 0;
            secondsLeft = GAME_SECONDS;
            pregameLeft = PREGAME_SECONDS;
            buildSequence();
            buildMart();

            Map<String, List<Player>> sides = context.sides();
            List<String> keys = new ArrayList<>();
            for (Map.Entry<String, List<Player>> e : sides.entrySet()) {
                if (e.getValue() != null && !e.getValue().isEmpty()) keys.add(e.getKey());
            }
            int n = Math.max(1, keys.size());
            double radius = n < 2 ? 48 : Math.max(48, 11.5 / Math.sin(Math.PI / n));
            int soloIdx = 0;
            for (int i = 0; i < keys.size(); i++) {
                String key = keys.get(i);
                double ang = 2 * Math.PI * i / n - Math.PI / 2;
                int cx = (int) Math.round(radius * Math.cos(ang));
                int cz = (int) Math.round(radius * Math.sin(ang));
                Material color;
                if (key.startsWith("TEAM:")) color = concreteFor(context.teamColor(key.substring(5)));
                else color = SOLO_COLORS[soloIdx++ % SOLO_COLORS.length];
                Area a = new Area(key, cx, cz, color);
                areas.put(key, a);
                for (Player p : sides.get(key)) sideOf.put(p.getUniqueId(), key);
                buildArea(a);
            }
            for (Area a : areas.values()) showTarget(a);

            running = true;
            playing = false;
            for (Player p : new ArrayList<>(context.participants())) {
                Area a = areaOf(p);
                if (a == null) continue;
                preparePlayer(p);
                p.teleport(a.spawn);
            }
            context.broadcast("§6§lBuild Mart §7- Copy the target build next to your plot! Gather blocks at the mart in the middle.");
            context.broadcast("§7Stalls regrow a few seconds after mining. Glass, Stone Bricks, Bricks and Gold are made at the shop counters (right-click).");
            context.setTimer(GAME_SECONDS, GAME_SECONDS);
            updateSidebar();
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::tickLoop, 1L, 1L);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.SEVERE, "Build Mart failed to start", t);
            McmGameContext c = ctx;
            stop();
            if (c != null) Bukkit.getScheduler().runTask(plugin, () -> c.finish("Build Mart failed to start"));
        }
    }

    @Override
    public void stop() {
        running = false;
        playing = false;
        if (task != null) { task.cancel(); task = null; }
        resetPlayers();
        cleanupArena();
        ctx = null;
    }

    private void endGame(String reason) {
        if (!running || ctx == null) return;
        McmGameContext c = ctx;
        running = false;
        playing = false;
        if (task != null) { task.cancel(); task = null; }
        try {
            awardFinalBonuses(c);
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Error awarding final bonuses", t);
        }
        resetPlayers();
        ctx = null;
        try {
            c.finish(reason);
        } finally {
            cleanupArena();
        }
    }

    // ---------------------------------------------------------------- arena building

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    private void forceChunk(int x, int z) {
        int cx = x >> 4, cz = z >> 4;
        long k = ((long) cx << 32) | (cz & 0xFFFFFFFFL);
        if (forcedChunks.add(k)) world.setChunkForceLoaded(cx, cz, true);
    }

    private static void setType(Block b, Material m) {
        BlockData data = m.createBlockData();
        if (data instanceof Leaves leaves) leaves.setPersistent(true);
        b.setBlockData(data, false);
    }

    /** Place an arena block and remember it for cleanup. */
    private void put(int x, int y, int z, Material m) {
        forceChunk(x, z);
        setType(world.getBlockAt(x, y, z), m);
        if (arenaKeys.add(key(x, y, z))) arenaBlocks.add(new int[]{x, y, z});
    }

    private TextDisplay label(double x, double y, double z, String text, float scale) {
        Location loc = new Location(world, x, y, z);
        forceChunk(loc.getBlockX(), loc.getBlockZ());
        TextDisplay d = world.spawn(loc, TextDisplay.class, td -> {
            td.text(c(text));
            td.setBillboard(Display.Billboard.CENTER);
            td.setPersistent(false);
            td.setShadowed(true);
            td.setLineWidth(220);
            td.setBackgroundColor(Color.fromARGB(110, 0, 0, 0));
            if (scale != 1f) {
                td.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                        new Vector3f(scale, scale, scale), new AxisAngle4f()));
            }
        });
        entities.add(d);
        return d;
    }

    private void buildMart() {
        // floor
        for (int x = -MART_X; x <= MART_X; x++) {
            for (int z = -MART_Z; z <= MART_Z; z++) {
                boolean edge = Math.abs(x) == MART_X || Math.abs(z) == MART_Z;
                Material m;
                if (edge) m = (Math.abs(x) == MART_X && Math.abs(z) == MART_Z) ? Material.SEA_LANTERN : Material.POLISHED_BLACKSTONE;
                else m = ((x + z) & 1) == 0 ? Material.SMOOTH_STONE : Material.POLISHED_ANDESITE;
                put(x, FLOOR_Y, z, m);
            }
        }
        // lamp posts at the corners
        for (int sx : new int[]{-MART_X, MART_X}) for (int sz : new int[]{-MART_Z, MART_Z}) {
            put(sx, FLOOR_Y + 1, sz, Material.POLISHED_BLACKSTONE_WALL);
            put(sx, FLOOR_Y + 2, sz, Material.POLISHED_BLACKSTONE_WALL);
            put(sx, FLOOR_Y + 3, sz, Material.GLOWSTONE);
        }
        label(0.5, FLOOR_Y + 11, 0.5, "§6§lBUILD MART\n§eBig Sales every day!", 3f);

        // stall grid: 8 columns x 4 rows; centre 4 slots are shops
        Shop[] shopDefs = {
                new Shop("Glassworks", Material.GLASS, 1, new Cost(Material.SAND, 2)),
                new Shop("Mason", Material.STONE_BRICKS, 1, new Cost(Material.COBBLESTONE, 2)),
                new Shop("Kiln", Material.BRICKS, 1, new Cost(Material.CLAY, 2)),
                new Shop("Jeweller", Material.GOLD_BLOCK, 2, new Cost(Material.IRON_BLOCK, 1), new Cost(Material.GLOWSTONE, 1))
        };
        Material[] shopBlocksMat = {Material.FURNACE, Material.STONECUTTER, Material.SMOKER, Material.SMITHING_TABLE};
        int raw = 0, shop = 0;
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 8; col++) {
                int x0 = -22 + col * 6, z0 = -10 + row * 6;
                boolean isShop = (col == 3 || col == 4) && (row == 1 || row == 2);
                if (isShop) {
                    Shop s = shopDefs[shop];
                    Material blockMat = shopBlocksMat[shop];
                    shop++;
                    for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 2; dz++)
                        put(x0 + dx, FLOOR_Y, z0 + dz, Material.ORANGE_TERRACOTTA);
                    for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) {
                        put(x0 + dx, FLOOR_Y + 1, z0 + dz, blockMat);
                        put(x0 + dx, FLOOR_Y + 2, z0 + dz, s.product);
                        shopBlocks.put(key(x0 + dx, FLOOR_Y + 1, z0 + dz), s);
                        shopBlocks.put(key(x0 + dx, FLOOR_Y + 2, z0 + dz), s);
                    }
                    StringBuilder cost = new StringBuilder();
                    for (Cost cst : s.costs) {
                        if (cost.length() > 0) cost.append(" §7+ ");
                        cost.append("§f").append(cst.amount()).append(" ").append(nice(cst.material()));
                    }
                    label(x0 + 1, FLOOR_Y + 4.2, z0 + 1, "§6§l" + s.name + "\n" + cost + " §7→ §a" + s.productAmount + " " + nice(s.product)
                            + "\n§8Right-click (sneak = x16)", 1f);
                } else {
                    if (raw >= RAW_STALLS.length) continue;
                    Material m = RAW_STALLS[raw++];
                    int h = m.hasGravity() ? 1 : 3;
                    Stall st = new Stall(m, x0, z0, h);
                    stalls.add(st);
                    for (int dx = -1; dx <= 2; dx++) for (int dz = -1; dz <= 2; dz++)
                        put(x0 + dx, FLOOR_Y, z0 + dz, (dx == -1 || dx == 2 || dz == -1 || dz == 2) ? Material.STRIPPED_SPRUCE_WOOD : Material.SPRUCE_PLANKS);
                    for (int dx = 0; dx <= 1; dx++) for (int dz = 0; dz <= 1; dz++) for (int dy = 1; dy <= h; dy++) {
                        put(x0 + dx, FLOOR_Y + dy, z0 + dz, m);
                        stallBlocks.put(key(x0 + dx, FLOOR_Y + dy, z0 + dz), st);
                    }
                    st.label[0] = label(x0 + 1, FLOOR_Y + h + 1.4, z0 + 1, stallText(st), 1f);
                }
            }
        }
    }

    private String stallText(Stall st) {
        if (st.saleUntilTick > tick) return "§c§lSALE! §f" + nice(st.material) + "\n§efast restock";
        return "§f" + nice(st.material);
    }

    private void buildArea(Area a) {
        int y = FLOOR_Y;
        // platform 17 x 11
        for (int x = a.cx - 8; x <= a.cx + 8; x++) for (int z = a.cz - 5; z <= a.cz + 5; z++) {
            boolean edge = x == a.cx - 8 || x == a.cx + 8 || z == a.cz - 5 || z == a.cz + 5;
            put(x, y, z, edge ? a.color : Material.SMOOTH_STONE);
        }
        // rings around plot and pedestal
        for (int[] base : new int[][]{{a.plotX, a.plotZ}, {a.tgtX, a.tgtZ}}) {
            for (int x = base[0] - 1; x <= base[0] + 5; x++) for (int z = base[1] - 1; z <= base[1] + 5; z++) {
                boolean ring = x == base[0] - 1 || x == base[0] + 5 || z == base[1] - 1 || z == base[1] + 5;
                if (ring) put(x, y, z, base[0] == a.tgtX ? Material.QUARTZ_BRICKS : a.color);
            }
        }
        // spawn pad in the middle strip
        for (int x = a.cx - 1; x <= a.cx + 1; x++) for (int z = a.cz - 1; z <= a.cz + 1; z++)
            put(x, y, z, (x == a.cx && z == a.cz) ? Material.SEA_LANTERN : a.color);
        a.spawn = new Location(world, a.cx + 0.5, y + 1, a.cz + 0.5);
        a.spawn.setDirection(new Vector(-a.cx, 0, -a.cz).lengthSquared() < 1 ? new Vector(0, 0, 1) : new Vector(-a.cx, 0, -a.cz));

        // path towards the mart
        double len = Math.sqrt((double) a.cx * a.cx + (double) a.cz * a.cz);
        if (len > 1) {
            double dx = -a.cx / len, dz = -a.cz / len;
            for (double t = 0; t < len; t += 0.5) {
                int px = (int) Math.floor(a.cx + 0.5 + dx * t), pz = (int) Math.floor(a.cz + 0.5 + dz * t);
                if (Math.abs(px) <= MART_X && Math.abs(pz) <= MART_Z) break;
                for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) {
                    int bx = px + ox, bz = pz + oz;
                    if (arenaKeys.contains(key(bx, y, bz))) continue;
                    if (bx >= a.cx - 8 && bx <= a.cx + 8 && bz >= a.cz - 5 && bz <= a.cz + 5) continue;
                    put(bx, y, bz, (ox == 0 && oz == 0) ? Material.POLISHED_ANDESITE : Material.STONE_BRICK_SLAB);
                }
            }
        }
        forceChunk(a.plotX, a.plotZ);
        forceChunk(a.plotX + 4, a.plotZ + 4);
        forceChunk(a.tgtX + 4, a.tgtZ + 4);

        a.plotLabel = label(a.plotX + 2.5, y + 7.3, a.plotZ + 2.5, "§a§l▼ BUILD HERE ▼\n" + label(a.side), 1f);
        a.targetLabel = label(a.tgtX + 2.5, y + 7.3, a.tgtZ + 2.5, "", 1f);
    }

    private String label(String side) {
        try { return ctx == null ? side : ctx.sideLabel(side); } catch (Throwable t) { return side; }
    }

    private void clearRegion(int bx, int bz) {
        for (int x = 0; x < 5; x++) for (int y = 1; y <= 5; y++) for (int z = 0; z < 5; z++) {
            Block b = world.getBlockAt(bx + x, FLOOR_Y + y, bz + z);
            if (!b.getType().isAir()) b.setType(Material.AIR, false);
        }
    }

    private BuildPreset presetAt(int index) {
        while (sequence.size() <= index) appendShuffle(false);
        return sequence.get(index);
    }

    private void buildSequence() {
        sequence.clear();
        appendShuffle(true);
    }

    /** Adds one pass over all presets. The first pass ramps difficulty up; later passes are random. */
    private void appendShuffle(boolean ramp) {
        List<BuildPreset> list = new ArrayList<>(BuildPreset.ALL);
        if (ramp) {
            Map<BuildPreset, Double> w = new HashMap<>();
            for (BuildPreset p : list) w.put(p, p.stars + random.nextDouble() * 2.0);
            list.sort(Comparator.comparingDouble(w::get));
        } else {
            Collections.shuffle(list, random);
            if (!sequence.isEmpty() && list.get(0) == sequence.get(sequence.size() - 1)) Collections.swap(list, 0, list.size() - 1);
        }
        sequence.addAll(list);
    }

    private void showTarget(Area a) {
        BuildPreset p = presetAt(a.seqIndex);
        clearRegion(a.tgtX, a.tgtZ);
        for (int x = 0; x < 5; x++) for (int z = 0; z < 5; z++) {
            Material floor = p.columnUsed(x, z) ? Material.LIGHT_GRAY_CONCRETE : Material.WHITE_CONCRETE;
            put(a.plotX + x, FLOOR_Y, a.plotZ + z, floor);
            put(a.tgtX + x, FLOOR_Y, a.tgtZ + z, floor);
            for (int y = 0; y < 5; y++) {
                Material m = p.at(x, y, z);
                if (m != Material.AIR) setType(world.getBlockAt(a.tgtX + x, FLOOR_Y + 1 + y, a.tgtZ + z), m);
            }
        }
        StringBuilder sb = new StringBuilder();
        sb.append("§eBuild #").append(a.seqIndex + 1).append(": §f§l").append(p.name).append("\n").append(p.starString()).append("\n");
        for (Map.Entry<Material, Integer> e : p.counts.entrySet()) {
            sb.append("§f").append(e.getValue()).append("x §7").append(nice(e.getKey()));
            if (BuildPreset.SHOP_MATERIALS.contains(e.getKey())) sb.append(" §6(shop)");
            sb.append("\n");
        }
        if (a.targetLabel != null && a.targetLabel.isValid()) a.targetLabel.text(c(sb.toString().trim()));
        a.buildStartTick = tick;
        a.lastCorrect = 0; a.lastNeeded = p.blocks; a.lastWrong = 0;
    }

    private void cleanupArena() {
        World w = world != null ? Bukkit.getWorld(WORLD) : null;
        if (w != null) {
            try {
                for (Area a : areas.values()) {
                    clearRegion(a.plotX, a.plotZ);
                    clearRegion(a.tgtX, a.tgtZ);
                }
                for (int i = arenaBlocks.size() - 1; i >= 0; i--) {
                    int[] p = arenaBlocks.get(i);
                    w.getBlockAt(p[0], p[1], p[2]).setType(Material.AIR, false);
                }
                for (Entity e : entities) if (e.isValid()) e.remove();
                for (Entity e : w.getEntities()) if (!(e instanceof Player)) e.remove();
                for (long k : forcedChunks) w.setChunkForceLoaded((int) (k >> 32), (int) k, false);
            } catch (Throwable t) {
                plugin.getLogger().log(Level.WARNING, "Error while clearing the Build Mart arena", t);
            }
        }
        arenaBlocks.clear();
        arenaKeys.clear();
        forcedChunks.clear();
        entities.clear();
        stallBlocks.clear();
        stalls.clear();
        shopBlocks.clear();
        regen.clear();
        areas.clear();
        sideOf.clear();
        shopCooldown.clear();
        dirty.clear();
        finishersPerIndex.clear();
    }

    // ---------------------------------------------------------------- players

    private ItemStack tool(Material m) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setUnbreakable(true);
            meta.addEnchant(Enchantment.EFFICIENCY, 5, true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.getPersistentDataContainer().set(toolKey, PersistentDataType.BYTE, (byte) 1);
            it.setItemMeta(meta);
        }
        return it;
    }

    private boolean isTool(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return false;
        ItemMeta m = it.getItemMeta();
        return m != null && m.getPersistentDataContainer().has(toolKey, PersistentDataType.BYTE);
    }

    private void preparePlayer(Player p) {
        p.setGameMode(GameMode.SURVIVAL);
        PlayerInventory inv = p.getInventory();
        inv.clear();
        inv.setArmorContents(null);
        inv.setItem(0, tool(Material.IRON_PICKAXE));
        inv.setItem(1, tool(Material.IRON_AXE));
        inv.setItem(2, tool(Material.IRON_SHOVEL));
        inv.setItem(3, tool(Material.SHEARS));
        p.setFoodLevel(20);
        p.setSaturation(20f);
        p.setFallDistance(0);
        p.setFireTicks(0);
        p.setAllowFlight(true);
        p.setFlying(false);
        p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, PotionEffect.INFINITE_DURATION, 1, false, false, true));
    }

    private void resetPlayers() {
        if (ctx == null) return;
        Collection<Player> parts;
        try { parts = new ArrayList<>(ctx.participants()); } catch (Throwable t) { return; }
        for (Player p : parts) {
            if (p == null || !p.isOnline()) continue;
            if (!p.getWorld().getName().equals(WORLD)) continue;
            p.getInventory().clear();
            p.removePotionEffect(PotionEffectType.HASTE);
            p.setFlying(false);
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) p.setAllowFlight(false);
        }
    }

    private Area areaOf(Player p) {
        String s = sideOf.get(p.getUniqueId());
        return s == null ? null : areas.get(s);
    }

    private List<Player> members(String side) {
        List<Player> out = new ArrayList<>();
        if (ctx == null) return out;
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (p != null && p.isOnline() && side.equals(sideOf.get(p.getUniqueId()))) out.add(p);
        }
        return out;
    }

    private boolean inOurWorld(World w) {
        return running && world != null && w != null && w.getName().equals(WORLD);
    }

    private static boolean inRegion(Block b, int bx, int bz) {
        int x = b.getX() - bx, z = b.getZ() - bz, y = b.getY() - FLOOR_Y;
        return x >= 0 && x < 5 && z >= 0 && z < 5 && y >= 1 && y <= 5;
    }

    // ---------------------------------------------------------------- game loop

    private void tickLoop() {
        if (!running || ctx == null) return;
        try {
            tick++;
            processRegen();
            if (!dirty.isEmpty()) {
                List<String> d = new ArrayList<>(dirty);
                dirty.clear();
                if (playing) for (String s : d) {
                    Area a = areas.get(s);
                    if (a != null) evaluate(a, true);
                }
            }
            if (tick % 5 == 0) voidCheck();
            if (tick % 20 == 0) second();
        } catch (Throwable t) {
            plugin.getLogger().log(Level.WARNING, "Error in Build Mart tick", t);
        }
    }

    private void processRegen() {
        if (regen.isEmpty()) return;
        Iterator<Regen> it = regen.values().iterator();
        while (it.hasNext()) {
            Regen r = it.next();
            if (r.due > tick) continue;
            Block b = world.getBlockAt(r.x, r.y, r.z);
            if (b.getType().isAir()) {
                setType(b, r.material);
                if (r.y == FLOOR_Y + 1 || random.nextInt(4) == 0)
                    world.spawnParticle(Particle.HAPPY_VILLAGER, r.x + 0.5, r.y + 0.5, r.z + 0.5, 3, 0.3, 0.3, 0.3, 0);
            }
            it.remove();
        }
    }

    private void voidCheck() {
        for (Player p : new ArrayList<>(ctx.participants())) {
            if (!p.getWorld().getName().equals(WORLD) || p.getLocation().getY() > VOID_Y) continue;
            Area a = areaOf(p);
            if (a == null) continue;
            // Like the original: falling off costs you the blocks you carried.
            PlayerInventory inv = p.getInventory();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack it = inv.getItem(i);
                if (it != null && !isTool(it)) inv.setItem(i, null);
            }
            p.setFallDistance(0);
            p.setVelocity(new Vector());
            p.teleport(a.spawn);
            p.sendMessage("§cYou fell off and dropped everything you were carrying!");
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
        }
    }

    private void second() {
        if (ctx.participants().isEmpty()) {
            endGame("Everyone left");
            return;
        }
        if (!playing) {
            pregameLeft--;
            if (pregameLeft > 0) {
                for (Player p : new ArrayList<>(ctx.participants())) {
                    if (pregameLeft <= 5) {
                        p.showTitle(title("§e" + pregameLeft, "§7Study your first build!", 0, 900, 100));
                        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                    Area a = areaOf(p);
                    if (a != null) p.sendActionBar(c("§eFirst build: §f" + presetAt(a.seqIndex).name + " §7- starting in " + pregameLeft + "s"));
                }
                ctx.setTimer(GAME_SECONDS, GAME_SECONDS);
                return;
            }
            playing = true;
            for (Area a : areas.values()) a.buildStartTick = tick;
            for (Player p : new ArrayList<>(ctx.participants())) {
                p.showTitle(title("§a§lGO!", "§7Shop, build, repeat!", 0, 1000, 300));
                p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
                Area a = areaOf(p);
                if (a != null) sendShoppingList(p, a);
            }
            ctx.setTimer(secondsLeft, GAME_SECONDS);
            return;
        }

        secondsLeft--;
        ctx.setTimer(Math.max(0, secondsLeft), GAME_SECONDS);
        int elapsed = GAME_SECONDS - secondsLeft;

        // Big Sales
        if (elapsed % SALE_EVERY == 0 && secondsLeft > 20) startSale();
        for (Stall st : stalls) {
            if (st.saleUntilTick > 0 && st.saleUntilTick <= tick) {
                st.saleUntilTick = -1;
                if (st.label[0] != null && st.label[0].isValid()) st.label[0].text(c(stallText(st)));
            }
        }

        for (Area a : new ArrayList<>(areas.values())) evaluate(a, true);
        if (!running) return;

        for (Player p : new ArrayList<>(ctx.participants())) {
            Area a = areaOf(p);
            if (a == null) continue;
            BuildPreset bp = presetAt(a.seqIndex);
            String wrong = a.lastWrong > 0 ? " §c(" + a.lastWrong + " wrong)" : "";
            p.sendActionBar(c("§e#" + (a.seqIndex + 1) + " " + bp.name + " §7| §a" + a.lastCorrect + "§7/" + a.lastNeeded + " placed" + wrong));
        }
        if (secondsLeft == 60 || secondsLeft == 30 || secondsLeft == 10) ctx.broadcast("§6[Build Mart] §e" + secondsLeft + " seconds left!");
        updateSidebar();
        if (secondsLeft <= 0) endGame("Time's up");
    }

    private void startSale() {
        List<Stall> pool = new ArrayList<>(stalls);
        Collections.shuffle(pool, random);
        List<String> names = new ArrayList<>();
        for (int i = 0; i < Math.min(3, pool.size()); i++) {
            Stall st = pool.get(i);
            st.saleUntilTick = tick + SALE_LENGTH * 20L;
            names.add(nice(st.material));
            if (st.label[0] != null && st.label[0].isValid()) st.label[0].text(c(stallText(st)));
        }
        if (ctx == null) return;
        ctx.broadcast("§c§lBIG SALE! §e" + String.join(", ", names) + " §7restock almost instantly for " + SALE_LENGTH + "s!");
        for (Player p : new ArrayList<>(ctx.participants())) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
    }

    /** Compare the plot with the target; completes the build when it matches exactly. */
    private void evaluate(Area a, boolean allowComplete) {
        BuildPreset p = presetAt(a.seqIndex);
        int correct = 0, wrong = 0;
        for (int x = 0; x < 5; x++) for (int y = 0; y < 5; y++) for (int z = 0; z < 5; z++) {
            Material want = p.at(x, y, z);
            Material have = world.getBlockAt(a.plotX + x, FLOOR_Y + 1 + y, a.plotZ + z).getType();
            if (have.isAir()) have = Material.AIR;
            if (want != Material.AIR) {
                if (have == want) correct++;
                else if (have != Material.AIR) wrong++;
            } else if (have != Material.AIR) wrong++;
        }
        a.lastCorrect = correct;
        a.lastNeeded = p.blocks;
        a.lastWrong = wrong;
        if (allowComplete && playing && correct == p.blocks && wrong == 0) complete(a);
    }

    private void complete(Area a) {
        BuildPreset p = presetAt(a.seqIndex);
        int order = finishersPerIndex.merge(a.seqIndex, 1, Integer::sum);
        int secs = (int) ((tick - a.buildStartTick) / 20);
        int base = 4 + 3 * p.stars;
        int speed = Math.max(0, 5 - secs / 30);
        int orderBonus = areas.size() > 1 ? (order == 1 ? 4 : order == 2 ? 2 : order == 3 ? 1 : 0) : 0;
        int pts = base + speed + orderBonus;
        List<Player> mem = members(a.side);
        for (Player m : mem) ctx.addPoints(m.getUniqueId(), pts, "Build Mart: built " + p.name);
        a.completed++;

        String place = orderBonus > 0 ? (order == 1 ? " §6(first!)" : order == 2 ? " §7(2nd)" : " §7(3rd)") : "";
        ctx.broadcast("§6[Build Mart] " + label(a.side) + " §7completed §f" + p.name + " §7in " + fmt(secs) + place + " §a+" + pts);
        for (Player m : mem) {
            m.showTitle(title("§a§lBUILD COMPLETE!", "§f" + p.name + " §a+" + pts + " points", 0, 1400, 300));
            m.playSound(m.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1f);
        }
        world.spawnParticle(Particle.TOTEM_OF_UNDYING, a.plotX + 2.5, FLOOR_Y + 3, a.plotZ + 2.5, 60, 1.5, 1.5, 1.5, 0.3);

        clearRegion(a.plotX, a.plotZ);
        a.seqIndex++;
        showTarget(a);
        for (Player m : mem) sendShoppingList(m, a);
        updateSidebar();
    }

    private void sendShoppingList(Player p, Area a) {
        BuildPreset bp = presetAt(a.seqIndex);
        StringBuilder sb = new StringBuilder("§6[Build Mart] §eBuild #" + (a.seqIndex + 1) + ": §f§l" + bp.name + " " + bp.starString() + "\n§7Needs: ");
        boolean first = true;
        for (Map.Entry<Material, Integer> e : bp.counts.entrySet()) {
            if (!first) sb.append("§7, ");
            first = false;
            sb.append("§f").append(e.getValue()).append(" ").append(nice(e.getKey()));
            if (BuildPreset.SHOP_MATERIALS.contains(e.getKey())) sb.append(" §6(shop)");
        }
        p.sendMessage(sb.toString());
    }

    private void updateSidebar() {
        if (ctx == null) return;
        List<Area> list = new ArrayList<>(areas.values());
        list.sort((x, y) -> Integer.compare(y.completed, x.completed));
        List<String> lines = new ArrayList<>();
        lines.add("§7Builds completed:");
        for (int i = 0; i < Math.min(5, list.size()); i++) {
            Area a = list.get(i);
            String name = presetAt(a.seqIndex).name;
            if (name.length() > 12) name = name.substring(0, 12);
            lines.add(label(a.side) + " §f" + a.completed + " §8" + name);
        }
        ctx.setSidebarLines(lines);
    }

    private void awardFinalBonuses(McmGameContext c) {
        List<Area> list = new ArrayList<>(areas.values());
        list.sort((x, y) -> Integer.compare(y.completed, x.completed));
        c.broadcast("§6§lBuild Mart results:");
        int[] bonus = {10, 6, 3};
        int rank = 0;
        for (int i = 0; i < list.size(); i++) {
            Area a = list.get(i);
            if (i > 0 && a.completed < list.get(i - 1).completed) rank = i;
            c.broadcast("§e#" + (rank + 1) + " " + label(a.side) + " §7- §f" + a.completed + " builds");
            if (list.size() >= 2 && rank < bonus.length && a.completed > 0) {
                for (Player m : members(a.side)) c.addPoints(m.getUniqueId(), bonus[rank], "Build Mart: placed #" + (rank + 1));
            }
        }
    }

    // ---------------------------------------------------------------- listeners

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!inOurWorld(b.getWorld())) return;
        Player p = e.getPlayer();
        Area a = areaOf(p);
        if (!playing || a == null || p.getGameMode() != GameMode.SURVIVAL) { e.setCancelled(true); return; }
        long k = key(b.getX(), b.getY(), b.getZ());
        Stall st = stallBlocks.get(k);
        if (st != null && b.getType() == st.material) {
            if (!give(p, st.material)) { e.setCancelled(true); return; }
            e.setCancelled(false);
            e.setDropItems(false);
            e.setExpToDrop(0);
            long delay = st.saleUntilTick > tick ? SALE_REGEN_TICKS : NORMAL_REGEN_TICKS;
            regen.put(k, new Regen(b.getX(), b.getY(), b.getZ(), st.material, tick + delay));
            return;
        }
        if (inRegion(b, a.plotX, a.plotZ)) {
            Material m = b.getType();
            if (m.isItem() && !give(p, m)) { e.setCancelled(true); return; }
            e.setCancelled(false);
            e.setDropItems(false);
            e.setExpToDrop(0);
            dirty.add(a.side);
            return;
        }
        e.setCancelled(true);
    }

    private boolean give(Player p, Material m) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(new ItemStack(m));
        if (!left.isEmpty()) {
            p.sendActionBar(c("§cYour inventory is full!"));
            return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (!inOurWorld(b.getWorld())) return;
        Player p = e.getPlayer();
        Area a = areaOf(p);
        if (!playing || a == null || p.getGameMode() != GameMode.SURVIVAL || !inRegion(b, a.plotX, a.plotZ)) {
            e.setCancelled(true);
            if (playing && a != null) p.sendActionBar(c("§cYou can only build on your own plot."));
            return;
        }
        e.setCancelled(false);
        e.setBuild(true);
        dirty.add(a.side);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null || !inOurWorld(e.getClickedBlock().getWorld())) return;
        Block b = e.getClickedBlock();
        Shop s = shopBlocks.get(key(b.getX(), b.getY(), b.getZ()));
        if (s == null) {
            // block opening anything that isn't ours (nothing else interactive exists, but be safe)
            if (e.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && b.getType().isInteractable()
                    && !inPlotOf(e.getPlayer(), b)) {
                e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            }
            return;
        }
        e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        e.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        e.setCancelled(true);
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!playing || areaOf(p) == null) return;
        long now = System.currentTimeMillis();
        Long last = shopCooldown.get(p.getUniqueId());
        if (last != null && now - last < 150) return;
        shopCooldown.put(p.getUniqueId(), now);
        int times = p.isSneaking() ? 16 : 1;
        int done = 0;
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < times; i++) {
            boolean ok = true;
            for (Cost cst : s.costs) if (!inv.containsAtLeast(new ItemStack(cst.material()), cst.amount())) { ok = false; break; }
            if (!ok) break;
            if (!hasRoom(inv, s.product, s.productAmount)) {
                p.sendActionBar(c("§cYour inventory is full!"));
                return;
            }
            for (Cost cst : s.costs) inv.removeItem(new ItemStack(cst.material(), cst.amount()));
            inv.addItem(new ItemStack(s.product, s.productAmount));
            done++;
        }
        if (done > 0) {
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.7f, 1.3f);
            p.sendActionBar(c("§a+" + (done * s.productAmount) + " " + nice(s.product)));
        } else {
            StringBuilder need = new StringBuilder();
            for (Cost cst : s.costs) {
                if (need.length() > 0) need.append(" + ");
                need.append(cst.amount()).append(" ").append(nice(cst.material()));
            }
            p.sendActionBar(c("§c" + s.name + " needs " + need));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.7f, 1f);
        }
    }

    private static boolean hasRoom(PlayerInventory inv, Material m, int amount) {
        int room = 0;
        for (ItemStack it : inv.getStorageContents()) {
            if (it == null || it.getType().isAir()) room += m.getMaxStackSize();
            else if (it.getType() == m && !it.hasItemMeta()) room += Math.max(0, it.getMaxStackSize() - it.getAmount());
            if (room >= amount) return true;
        }
        return false;
    }

    private boolean inPlotOf(Player p, Block b) {
        Area a = areaOf(p);
        return a != null && inRegion(b, a.plotX, a.plotZ);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && inOurWorld(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent e) {
        if (inOurWorld(e.getEntity().getWorld())) {
            e.setCancelled(true);
            e.getEntity().setFoodLevel(20);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        if (inOurWorld(e.getPlayer().getWorld()) && isTool(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLeavesDecay(LeavesDecayEvent e) {
        if (e.getBlock().getWorld().getName().equals(WORLD)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFade(BlockFadeEvent e) {
        if (e.getBlock().getWorld().getName().equals(WORLD)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpread(BlockSpreadEvent e) {
        if (e.getBlock().getWorld().getName().equals(WORLD)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFallingLand(EntityChangeBlockEvent e) {
        // sand dropped inside a plot: re-check that plot after it lands
        if (!inOurWorld(e.getBlock().getWorld()) || !(e.getEntity() instanceof FallingBlock)) return;
        for (Area a : areas.values()) if (inRegion(e.getBlock(), a.plotX, a.plotZ)) dirty.add(a.side);
    }

    // ---------------------------------------------------------------- utils

    private static Component c(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy);
    }

    private static Title title(String t, String s, int inMs, int stayMs, int outMs) {
        return Title.title(c(t), c(s), Title.Times.times(Duration.ofMillis(inMs), Duration.ofMillis(stayMs), Duration.ofMillis(outMs)));
    }

    private static String fmt(int secs) {
        return (secs / 60) + ":" + String.format("%02d", secs % 60);
    }

    static String nice(Material m) {
        String[] parts = m.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String s : parts) {
            if (s.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(s.charAt(0))).append(s.substring(1));
        }
        return sb.toString();
    }

    private static Material concreteFor(ChatColor color) {
        if (color == null) return Material.WHITE_CONCRETE;
        return switch (color) {
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
            case GRAY -> Material.LIGHT_GRAY_CONCRETE;
            case DARK_GRAY -> Material.GRAY_CONCRETE;
            case BLACK -> Material.BLACK_CONCRETE;
            default -> Material.WHITE_CONCRETE;
        };
    }
}
