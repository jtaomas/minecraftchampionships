package com.mcm.games.acerace.world;

import com.mcm.api.McmApi;
import com.mcm.api.McmFx;
import com.mcm.api.McmGame;
import com.mcm.api.McmGameContext;
import com.mcm.api.McmSpectator;
import com.mcm.api.Waits;
import com.mcm.games.acerace.RaceHud;
import com.mcm.games.acerace.RaceScoring;
import com.mcm.games.acerace.world.WorldMap.Action;
import com.mcm.games.acerace.world.WorldMap.Box;
import com.mcm.games.acerace.world.WorldMap.Rule;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.ChunkSnapshot;
import org.bukkit.entity.Display;
import org.bukkit.entity.TextDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ace Race on imported course worlds. The course blocks stay exactly as downloaded. The map's own command blocks are
 * silenced while a race runs and their pad language (jump pads, speed strips, elytra grants and clears, water boots,
 * checkpoints, kill blocks) is applied here per player instead of to everyone at once, and this class adds what the
 * downloads never had: a start grid, ordered waypoints, finish detection, places and scoring.
 */
public final class WorldRace implements McmGame {
    private enum State { IDLE, PRE, RACING, ENDING }

    private final JavaPlugin plugin;
    private final Map<String, WorldMap> maps = new LinkedHashMap<>();
    private final RaceScoring scoring = new RaceScoring();
    private final RaceHud hud = new RaceHud();
    private final Map<UUID, Racer> racers = new LinkedHashMap<>();
    private final Map<Block, BlockData> saved = new LinkedHashMap<>();
    /** Arrow pads swapped in for this race (restored on stop), and the pads found per map (scanned once). */
    private final Map<Block, BlockData> swapped = new LinkedHashMap<>();
    private final Map<String, List<int[]>> arrowCache = new HashMap<>();
    private final List<TextDisplay> labels = new ArrayList<>();

    private McmGameContext ctx;
    private WorldMap map;
    private World world;
    private State state = State.IDLE;
    private int tick, timeLeft, finishCount;
    private long startMillis;
    private BukkitTask tickTask, countdownTask, endTask;
    private boolean finishCalled;
    private String forced, last;

    public WorldRace(JavaPlugin plugin) { this.plugin = plugin; }

    public void register(WorldMap m) { maps.put(m.id, m); }

    public int mapCount() { return maps.size(); }

    @Override public String id() { return "acerace"; }
    @Override public String displayName() { return "Ace Race"; }
    @Override public Material icon() { return Material.FEATHER; }
    @Override public String description() { return "A lap race over pads, water and elytra flights."; }
    @Override public boolean useCoreScoreboard() { return true; }
    @Override public List<String> maps() { return new ArrayList<>(maps.keySet()); }
    @Override public String currentMap() { return map == null ? null : map.id; }

    @Override
    public boolean forceNextMap(String id) {
        if (id == null || !maps.containsKey(id.toLowerCase(Locale.ROOT))) return false;
        forced = id.toLowerCase(Locale.ROOT);
        return true;
    }

    public boolean isRunning() { return state != State.IDLE; }
    public boolean isPre() { return state == State.PRE; }
    public boolean isRaceWorld(World w) { return state != State.IDLE && world != null && world.equals(w); }
    public boolean isRacer(Player p) { return p != null && racers.containsKey(p.getUniqueId()); }

    @Override
    public void start(McmGameContext context) {
        stop();
        ctx = context;
        scoring.load(plugin.getConfig());
        if (maps.isEmpty()) { context.finish("No Ace Race maps installed"); return; }
        List<String> ids = new ArrayList<>(maps.keySet());
        String chosen = forced != null ? forced : ids.get((ids.indexOf(last) + 1) % ids.size());
        forced = null;
        map = maps.get(chosen);
        world = Bukkit.getWorld(map.worldName);
        if (world == null) {
            String missing = map.worldName;
            map = null;
            context.finish("Ace Race world " + missing + " is not loaded");
            return;
        }
        last = chosen;
        state = State.PRE;
        finishCalled = false;
        finishCount = 0;
        tick = 0;
        timeLeft = map.timeLimit;

        saved.clear();
        List<Box> boxes = new ArrayList<>(map.open);
        boxes.add(map.gate);
        for (Box b : boxes) for (Block bl : blocks(b)) saved.putIfAbsent(bl, bl.getBlockData());
        for (Block bl : blocks(map.gate)) if (bl.isPassable()) bl.setType(Material.BARRIER, false);
        swapArrows();
        spawnLabels();

        List<Location> grid = grid();
        int i = 0;
        for (Player p : new ArrayList<>(ctx.participants())) {
            Location at = grid.get(i++ % grid.size());
            Racer r = new Racer(p.getUniqueId());
            r.checkpoint = at.clone();
            racers.put(p.getUniqueId(), r);
            p.teleport(at);
            prepare(p);
        }
        if (racers.isEmpty()) { context.finish("No Ace Race participants"); return; }
        board();
        List<Player> online = online();
        countdownTask = McmFx.countdown(plugin, online, McmApi.isFastTest() ? 1 : map.countdown,
                ChatColor.GRAY + map.name, this::go);
    }

    private void go() {
        if (ctx == null || state != State.PRE) return;
        state = State.RACING;
        tick = 0;
        startMillis = System.currentTimeMillis();
        for (Block b : saved.keySet()) b.setType(Material.AIR, false);
        tickTask = new BukkitRunnable() {
            @Override public void run() { tick(); }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void tick() {
        if (ctx == null || state != State.RACING) return;
        tick++;
        if (tick % 20 == 0) {
            if (--timeLeft <= 0) { end("Time"); return; }
            board();
        }
        for (Racer r : new ArrayList<>(racers.values())) {
            if (r.finished || r.dnf) continue;
            Player p = Bukkit.getPlayer(r.id);
            if (p == null || !p.isOnline() || p.getGameMode() == GameMode.SPECTATOR) { r.dnf = true; continue; }
            if (!world.equals(p.getWorld())) continue;
            Location loc = p.getLocation();
            if (loc.getY() < map.voidY) { respawn(p, r); continue; }
            if (tick % 10 == 1 || r.banks == null) r.banks = banksNear(loc);
            if (tick % 10 == 1) for (Action a : map.always) run(p, r, a);

            boolean killed = false;
            for (Rule rule : map.rules) {
                if (rule.banks() != null && !intersects(rule.banks(), r.banks)) continue;
                Block b = world.getBlockAt((int) Math.floor(loc.getX() + rule.dx()),
                        (int) Math.floor(loc.getY() + rule.dy()), (int) Math.floor(loc.getZ() + rule.dz()));
                Material type = b.getType();
                if (map.arrowTo != null && type == map.arrowTo) type = map.arrowFrom;
                if (type != rule.block()) continue;
                for (Action a : rule.actions()) {
                    if (a.kind().equals("kill")) { killed = true; break; }
                    run(p, r, a);
                }
                if (killed) break;
            }
            if (killed) { respawn(p, r); continue; }
            for (Box z : map.elytraZones) {
                if (z.contains(loc.getX(), loc.getY(), loc.getZ())) {
                    if (setSlot(p, EquipmentSlot.CHEST, "elytra")) cue(p, r, "mcc.ar.elytra_zone");
                    break;
                }
            }
            if (map.hoop != null && r.prev != null && throughHoop(r.prev, loc)) setSlot(p, EquipmentSlot.CHEST, "elytra");
            if (p.isGliding()) {
                for (int i = 0; i < map.boostRings.size(); i++) {
                    double[] ring = map.boostRings.get(i);
                    double dx = loc.getX() - ring[0], dy = loc.getY() + 0.9 - ring[1], dz = loc.getZ() - ring[2];
                    if (dx * dx + dy * dy + dz * dz > ring[3] * ring[3]) continue;
                    Integer ready = r.rings.get(i);
                    if (ready != null && ready > tick) continue;
                    r.rings.put(i, tick + 40);
                    p.setVelocity(loc.getDirection().multiply(map.boostStrength));
                    if (!McmFx.packSound(p, "mcc.ar.launch1", 1f, 1f)) p.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1.2f);
                    world.spawnParticle(org.bukkit.Particle.FIREWORK, loc, 20, 0.6, 0.6, 0.6, 0.05);
                }
            }

            if (r.nextWp < map.waypoints.size()) {
                int skipped = -1;
                for (int j = r.nextWp + 1; j < map.waypoints.size(); j++) {
                    if (map.waypoints.get(j).contains(loc.getX(), loc.getY(), loc.getZ())) { skipped = j; break; }
                }
                if (skipped >= 0 && !map.waypoints.get(r.nextWp).contains(loc.getX(), loc.getY(), loc.getZ())) {
                    respawn(p, r);
                    McmFx.title(p, ChatColor.RED + "" + ChatColor.BOLD + "MISSED CHECKPOINT " + (r.nextWp + 1),
                            ChatColor.GRAY + "Follow the course - no shortcuts", 2, 40, 8);
                    continue;
                }
                if (map.waypoints.get(r.nextWp).contains(loc.getX(), loc.getY(), loc.getZ())) {
                    if (((org.bukkit.entity.Entity) p).isOnGround()) setCheckpoint(p, r);
                    r.nextWp++;
                    scoring.checkpoint(ctx, r.id, r.nextWp, map.waypoints.size());
                    hud.checkpoint(p, r.nextWp, map.waypoints.size(), (tick - r.splitTick) / 20.0, true);
                    McmFx.packSound(p, "mcc.ui.score", 1f, 1f);
                    r.splitTick = tick;
                }
            } else if (map.finish.contains(loc.getX(), loc.getY(), loc.getZ())) {
                if (r.lap < map.laps) {
                    scoring.lap(ctx, r.id, r.lap, map.laps);
                    r.lap++;
                    r.nextWp = 0;
                    r.splitTick = tick;
                    McmFx.title(p, ChatColor.GOLD + "" + ChatColor.BOLD + "LAP " + r.lap + "/" + map.laps,
                            ChatColor.GRAY + "Keep going", 3, 30, 5);
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 1.2f);
                } else {
                    finish(p, r);
                    continue;
                }
            }
            r.prev = loc;
            if (tick % 4 == 0) hud.action(p, r.lap, map.laps, r.nextWp, map.waypoints.size(), place(r));
        }
        if (allDone()) end("All finished");
    }

    private void finish(Player p, Racer r) {
        r.finished = true;
        r.place = ++finishCount;
        scoring.finish(ctx, r.id);
        scoring.place(ctx, r.id, r.place);
        hud.finish(p, r.place, Math.max(0L, System.currentTimeMillis() - startMillis));
        strip(p);
        McmSpectator.makeSpectator(p, startLoc());
    }

    private void respawn(Player p, Racer r) {
        Location to = r.checkpoint == null ? startLoc() : r.checkpoint;
        p.setGliding(false);
        p.teleport(to);
        p.setVelocity(new Vector());
        p.setFallDistance(0f);
        p.setFireTicks(0);
        r.prev = null;
        if (r.cpElytra) setSlot(p, EquipmentSlot.CHEST, "elytra");
        if (map.trident && !p.getInventory().contains(Material.TRIDENT) && p.getInventory().getChestplate() == null) {
            p.getInventory().setItem(0, trident());
        }
        McmFx.actionBar(p, ChatColor.RED + "Back to your last checkpoint");
        if (!McmFx.packSound(p, "mcc.death", 1f, 1f)) p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.7f);
    }

    // ------------------------------------------------------------------ the map's pad language, per player

    private void run(Player p, Racer r, Action a) {
        switch (a.kind()) {
            case "effect" -> {
                PotionEffectType t = Registry.EFFECT.get(NamespacedKey.minecraft(a.arg()));
                if (t != null) p.addPotionEffect(new PotionEffect(t, a.n1() < 0 ? PotionEffect.INFINITE_DURATION : a.n1() * 20,
                        a.n2(), false, false, true));
                if (a.arg().equals("jump_boost") && a.n2() >= 5) cue(p, r, a.n2() >= 10 ? "mcc.ar.launch1" : "mcc.ar.jump1");
                else if (a.arg().equals("speed") && a.n2() >= 8) cue(p, r, "mcc.ar.speed1");
            }
            case "uneffect" -> {
                if (a.arg().equals("*")) {
                    for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
                } else {
                    PotionEffectType t = Registry.EFFECT.get(NamespacedKey.minecraft(a.arg()));
                    if (t != null) p.removePotionEffect(t);
                }
            }
            case "item" -> {
                String[] s = a.arg().split(" ");
                switch (s[0]) {
                    case "armor.chest" -> {
                        if (setSlot(p, EquipmentSlot.CHEST, s[1])) cue(p, r, s[1].equals("elytra") ? "mcc.ar.elytra_zone" : "mcc.ar.elytra_remove");
                    }
                    case "armor.feet" -> setSlot(p, EquipmentSlot.FEET, s[1]);
                    default -> {
                        ItemStack cur = p.getInventory().getItem(0);
                        if (cur == null || cur.getType() != Material.TRIDENT) p.getInventory().setItem(0, trident());
                    }
                }
            }
            case "clear" -> {
                if (a.arg().equals("*")) {
                    if (!p.getInventory().isEmpty()) p.getInventory().clear();
                } else {
                    Material m = Material.matchMaterial(a.arg());
                    if (m != null && p.getInventory().contains(m)) p.getInventory().remove(m);
                }
            }
            case "sound" -> {
                Integer lastPlayed = r.sounds.get(a.arg());
                if (lastPlayed == null || tick - lastPlayed >= 4) {
                    r.sounds.put(a.arg(), tick);
                    p.playSound(p.getLocation(), a.arg(), SoundCategory.BLOCKS, 1f, 1f);
                }
            }
            case "checkpoint" -> {
                if (r.checkpoint == null || !r.checkpoint.getWorld().equals(p.getWorld()) || r.checkpoint.distanceSquared(p.getLocation()) > 64) {
                    cue(p, r, "mcc.ar.checkpoint_pass");
                }
                setCheckpoint(p, r);
            }
            case "launch" -> {
                if (r.launchReady > tick) return;
                r.launchReady = tick + 12;
                String[] v = a.arg().split(" ");
                double up = Double.parseDouble(v[0]), fwd = Double.parseDouble(v[1]);
                Vector dir = p.getLocation().getDirection().setY(0);
                if (dir.lengthSquared() < 1e-4) dir = new Vector(0, 0, 0); else dir.normalize();
                Vector cur = p.getVelocity();
                p.setVelocity(new Vector(cur.getX() + dir.getX() * fwd, up, cur.getZ() + dir.getZ() * fwd));
                p.setFallDistance(0f);
                cue(p, r, up >= 1.4 ? "mcc.ar.launch1" : "mcc.ar.jump1");
                p.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, p.getLocation(), 12, 0.4, 0.1, 0.4, 0);
            }
            default -> { }
        }
    }

    /** MCC Ace Race sound from the server pack, at most once a second per cue. */
    private void cue(Player p, Racer r, String key) {
        Integer last = r.sounds.get(key);
        if (last != null && tick - last < 20) return;
        r.sounds.put(key, tick);
        McmFx.packSound(p, key, 1f, 1f);
    }

    /** True if the slot changed. */
    private boolean setSlot(Player p, EquipmentSlot slot, String what) {
        ItemStack cur = p.getInventory().getItem(slot);
        Material have = cur == null ? Material.AIR : cur.getType();
        if (what.equals("air")) {
            if (have == Material.AIR) return false;
            p.getInventory().setItem(slot, null);
            return true;
        } else if (what.equals("elytra")) {
            if (have == Material.ELYTRA) return false;
            ItemStack e = new ItemStack(Material.ELYTRA);
            ItemMeta m = e.getItemMeta();
            m.setUnbreakable(true);
            e.setItemMeta(m);
            p.getInventory().setItem(slot, e);
            return true;
        } else if (what.equals("depth_boots")) {
            if (have != Material.LEATHER_BOOTS) {
                ItemStack b = new ItemStack(Material.LEATHER_BOOTS);
                LeatherArmorMeta m = (LeatherArmorMeta) b.getItemMeta();
                m.setColor(Color.WHITE);
                m.addEnchant(Enchantment.DEPTH_STRIDER, 255, true);
                m.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                m.setUnbreakable(true);
                b.setItemMeta(m);
                p.getInventory().setItem(slot, b);
                return true;
            }
        }
        return false;
    }

    private ItemStack trident() {
        ItemStack t = new ItemStack(Material.TRIDENT);
        ItemMeta m = t.getItemMeta();
        if (map.riptide > 0) m.addEnchant(Enchantment.RIPTIDE, map.riptide, true);
        if (map.unbreaking > 0) m.addEnchant(Enchantment.UNBREAKING, map.unbreaking, true);
        m.setUnbreakable(true);
        t.setItemMeta(m);
        return t;
    }

    /** Command banks that would be ticking around this player in the original map (all of them if none is near). */
    private Set<String> banksNear(Location loc) {
        Set<String> out = new HashSet<>();
        String nearest = null;
        double best = Double.MAX_VALUE;
        for (Map.Entry<String, int[]> e : map.banks.entrySet()) {
            double d = Math.max(Math.abs(loc.getX() - e.getValue()[0]), Math.abs(loc.getZ() - e.getValue()[1]));
            if (d <= map.bankRange) out.add(e.getKey());
            if (d < best) { best = d; nearest = e.getKey(); }
        }
        if (out.isEmpty() && nearest != null) out.add(nearest);
        return out;
    }

    private static boolean intersects(Set<String> a, Set<String> b) {
        for (String s : a) if (b.contains(s)) return true;
        return false;
    }

    /** True if the move passed through the opening of a ring built from the hoop block. */
    private boolean throughHoop(Location from, Location to) {
        if (!from.getWorld().equals(to.getWorld())) return false;
        Vector d = to.toVector().subtract(from.toVector());
        double len = d.length();
        if (len > 12) return false;
        int steps = Math.max(1, (int) Math.ceil(len / 0.5));
        for (int i = 1; i <= steps; i++) {
            Vector v = from.toVector().add(d.clone().multiply(i / (double) steps));
            int x = v.getBlockX(), y = (int) Math.floor(v.getY() + 0.9), z = v.getBlockZ();
            if (world.getBlockAt(x, y, z).getType() == map.hoop) continue;
            boolean ud = ray(x, y, z, 0, 1, 0) && ray(x, y, z, 0, -1, 0);
            boolean ew = ray(x, y, z, 1, 0, 0) && ray(x, y, z, -1, 0, 0);
            boolean ns = ray(x, y, z, 0, 0, 1) && ray(x, y, z, 0, 0, -1);
            if ((ud && (ew || ns)) || (ew && ns)) return true;
        }
        return false;
    }

    private boolean ray(int x, int y, int z, int dx, int dy, int dz) {
        for (int i = 1; i <= 8; i++) {
            Block b = world.getBlockAt(x + dx * i, y + dy * i, z + dz * i);
            if (b.getType() == map.hoop) return true;
            if (!b.isPassable()) return false;
        }
        return false;
    }

    private void setCheckpoint(Player p, Racer r) {
        r.checkpoint = p.getLocation();
        ItemStack chest = p.getInventory().getChestplate();
        r.cpElytra = chest != null && chest.getType() == Material.ELYTRA;
    }

    // ------------------------------------------------------------------ arrow pads and course labels

    /** Swaps the map's arrow pads for the arrow block, facing along the racing line (pads found by {@link #prescan}). */
    private void swapArrows() {
        if (map.arrowFrom == null || map.arrowArea == null) return;
        List<int[]> cached = arrowCache.get(map.id);
        if (cached != null) applyArrows(cached);
        else scan(map);                              // not ready yet: applied when the scan lands, if this race still runs
    }

    /** Finds every map's arrow pads in the background (chunks load async, the block scan runs off-thread). */
    public void prescan() { for (WorldMap m : maps.values()) if (m.arrowFrom != null && m.arrowArea != null) scan(m); }

    private final Set<String> scanning = new HashSet<>();

    private void scan(WorldMap m) {
        World w = Bukkit.getWorld(m.worldName);
        if (w == null || arrowCache.containsKey(m.id) || !scanning.add(m.id)) return;
        int[] a = m.arrowArea;
        List<int[]> keys = new ArrayList<>();
        for (int cx = a[0] >> 4; cx <= a[3] >> 4; cx++) for (int cz = a[2] >> 4; cz <= a[5] >> 4; cz++) keys.add(new int[]{cx, cz});
        List<ChunkSnapshot> snaps = java.util.Collections.synchronizedList(new ArrayList<>());
        java.util.concurrent.atomic.AtomicInteger left = new java.util.concurrent.atomic.AtomicInteger(keys.size());
        final Material from = m.arrowFrom;
        final int minY = Math.max(a[1], w.getMinHeight()), maxY = Math.min(a[4], w.getMaxHeight() - 1);
        for (int[] k : keys) {
            w.getChunkAtAsync(k[0], k[1]).whenComplete((chunk, err) -> {
                if (chunk != null) snaps.add(chunk.getChunkSnapshot(false, false, false));
                if (left.decrementAndGet() > 0) return;
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    List<int[]> found = new ArrayList<>();
                    for (ChunkSnapshot s : new ArrayList<>(snaps)) {
                        int bx = s.getX() << 4, bz = s.getZ() << 4;
                        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                            int wx = bx + x, wz = bz + z;
                            if (wx < a[0] || wx > a[3] || wz < a[2] || wz > a[5]) continue;
                            for (int y = minY; y <= maxY; y++) if (s.getBlockType(x, y, z) == from) found.add(new int[]{wx, y, wz});
                        }
                    }
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        scanning.remove(m.id);
                        arrowCache.put(m.id, found);
                        plugin.getLogger().info("Ace Race " + m.id + ": " + found.size() + " arrow pads");
                        if (map == m && state != State.IDLE && swapped.isEmpty()) applyArrows(found);
                    });
                });
            });
        }
    }

    private void applyArrows(List<int[]> pads) {
        BlockData base = map.arrowTo.createBlockData();
        java.util.Set<Long> at = new java.util.HashSet<>();
        for (int[] q : pads) at.add(key(q[0], q[1], q[2]));
        for (int[] q : pads) {
            Block b = world.getBlockAt(q[0], q[1], q[2]);
            if (b.getType() != map.arrowFrom) continue;
            BlockData old = b.getBlockData();
            // "keep": the map makers' own facings follow the racing line (their chevron fields agree 100% locally);
            // only a lone pad with no other pad near it (a decorative one-off) is pointed along the route instead
            boolean lone = true;
            for (int dx = -4; dx <= 4 && lone; dx++) for (int dz = -4; dz <= 4 && lone; dz++) for (int dy = -2; dy <= 2 && lone; dy++)
                if ((dx | dy | dz) != 0 && at.contains(key(q[0] + dx, q[1] + dy, q[2] + dz))) lone = false;
            BlockFace face = map.arrowKeepFacing && !(lone && map.route.size() >= 2) && old instanceof Directional od ? od.getFacing() : routeFacing(q[0] + 0.5, q[2] + 0.5);
            BlockData nd = base.clone();
            if (nd instanceof Directional d) d.setFacing(face);
            swapped.put(b, old);
            b.setBlockData(nd, false);
        }
    }

    private static long key(int x, int y, int z) { return ((long) x & 0x3FFFFFF) << 38 | ((long) z & 0x3FFFFFF) << 12 | (y & 0xFFF); }

    /** Glazed terracotta draws its top texture upright when facing south, so the arrow points against the facing. */
    private BlockFace routeFacing(double x, double z) {
        double best = Double.MAX_VALUE, dx = 0, dz = 1;
        for (int i = 0; i + 1 < map.route.size(); i++) {
            double[] p1 = map.route.get(i), p2 = map.route.get(i + 1);
            double sx = p2[0] - p1[0], sz = p2[1] - p1[1], len2 = sx * sx + sz * sz;
            if (len2 < 1e-6) continue;
            double t = Math.max(0, Math.min(1, ((x - p1[0]) * sx + (z - p1[1]) * sz) / len2));
            double qx = p1[0] + t * sx - x, qz = p1[1] + t * sz - z, d2 = qx * qx + qz * qz;
            if (d2 < best) { best = d2; dx = sx; dz = sz; }
        }
        if (Math.abs(dx) > Math.abs(dz)) return dx > 0 ? BlockFace.WEST : BlockFace.EAST;
        return dz > 0 ? BlockFace.NORTH : BlockFace.SOUTH;
    }

    /** Floating "CHECKPOINT n" / "FINISH" labels so the next target is visible from a distance. */
    private void spawnLabels() {
        for (int i = 0; i < map.waypoints.size(); i++) label(map.waypoints.get(i), "CHECKPOINT " + (i + 1), NamedTextColor.YELLOW);
        label(map.finish, "FINISH", NamedTextColor.GREEN);
    }

    private void label(Box b, String text, NamedTextColor color) {
        if (b.maxY() - b.minY() > 120) return;          // whole-column boxes: no sensible place for a label
        int cx = (b.minX() + b.maxX()) / 2, cz = (b.minZ() + b.maxZ()) / 2;
        int top = Integer.MIN_VALUE;
        for (int y = b.maxY(); y >= b.minY(); y--) if (world.getBlockAt(cx, y, cz).getType().isSolid()) { top = y; break; }
        double y = top == Integer.MIN_VALUE ? (b.minY() + b.maxY()) / 2.0 : top + 4.5;
        Location at = new Location(world, cx + 0.5, y, cz + 0.5);
        labels.add(world.spawn(at, TextDisplay.class, t -> {
            t.text(Component.text(text, color).decorate(TextDecoration.BOLD));
            t.setBillboard(Display.Billboard.CENTER);
            t.setShadowed(true);
            t.setViewRange(8f);
            t.setPersistent(false);
            t.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(4f, 4f, 4f), new AxisAngle4f()));
        }));
    }

    // ------------------------------------------------------------------ start grid, state, ending

    private Location startLoc() {
        return new Location(world, map.startX, map.startY, map.startZ, map.startYaw, 0f);
    }

    private List<Location> grid() {
        List<Location> out = new ArrayList<>();
        Box b = map.pen;
        for (int x = b.minX(); x <= b.maxX(); x++) for (int z = b.minZ(); z <= b.maxZ(); z++) {
            for (int y = b.minY(); y <= b.maxY(); y++) {
                if (world.getBlockAt(x, y - 1, z).getType().isSolid() && world.getBlockAt(x, y, z).isPassable()
                        && world.getBlockAt(x, y + 1, z).isPassable()) {
                    out.add(new Location(world, x + 0.5, y, z + 0.5, map.startYaw, 0f));
                    break;
                }
            }
        }
        double gx = (map.gate.minX() + map.gate.maxX()) / 2.0 + 0.5, gz = (map.gate.minZ() + map.gate.maxZ()) / 2.0 + 0.5;
        boolean alongX = map.gate.maxX() - map.gate.minX() >= map.gate.maxZ() - map.gate.minZ();
        out.sort(Comparator.comparingDouble((Location l) -> alongX ? Math.abs(l.getZ() - gz) : Math.abs(l.getX() - gx))
                .thenComparingDouble(l -> alongX ? Math.abs(l.getX() - gx) : Math.abs(l.getZ() - gz)));
        if (out.isEmpty()) out.add(startLoc());
        return out;
    }

    private List<Block> blocks(Box b) {
        List<Block> out = new ArrayList<>();
        for (int x = b.minX(); x <= b.maxX(); x++) for (int y = b.minY(); y <= b.maxY(); y++)
            for (int z = b.minZ(); z <= b.maxZ(); z++) out.add(world.getBlockAt(x, y, z));
        return out;
    }

    private void prepare(Player p) {
        strip(p);
        p.setGameMode(GameMode.ADVENTURE);
        p.setFoodLevel(20);
        p.setHealth(Math.min(20.0, p.getMaxHealth()));
        p.setFallDistance(0f);
        p.setVelocity(new Vector());
        if (map.trident) p.getInventory().setItem(0, trident());
        p.getInventory().setHeldItemSlot(0);
        final WorldMap token = map;
        // Multiverse re-applies the world's own gamemode a tick after the world change.
        for (long delay : new long[]{3L, 12L, 40L}) Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (map == token && p.isOnline() && isRacer(p) && isRaceWorld(p.getWorld())
                    && p.getGameMode() != GameMode.ADVENTURE && p.getGameMode() != GameMode.SPECTATOR) {
                p.setGameMode(GameMode.ADVENTURE);
            }
        }, delay);
    }

    private void strip(Player p) {
        p.setGliding(false);
        p.getInventory().clear();
        p.setItemOnCursor(null);
        p.setFireTicks(0);
        for (PotionEffect e : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(e.getType());
    }

    private List<Player> online() {
        List<Player> out = new ArrayList<>();
        for (Racer r : racers.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p != null) out.add(p);
        }
        return out;
    }

    private void board() {
        if (ctx == null || map == null) return;
        int left = 0;
        for (Racer r : racers.values()) if (!r.finished && !r.dnf) left++;
        ctx.setTimer(Math.max(0, timeLeft), map.timeLimit);
        ctx.setSidebarLines(List.of(
                ChatColor.GRAY + "Map " + ChatColor.WHITE + map.name,
                ChatColor.GRAY + "Laps " + ChatColor.YELLOW + map.laps,
                ChatColor.GRAY + "Finished " + ChatColor.YELLOW + finishCount + "/" + racers.size(),
                ChatColor.GRAY + "Racing " + ChatColor.YELLOW + left));
    }

    private int place(Racer r) {
        int ahead = finishCount;
        for (Racer o : racers.values()) {
            if (o == r || o.finished || o.dnf) continue;
            if (o.lap > r.lap || (o.lap == r.lap && o.nextWp > r.nextWp)) ahead++;
        }
        return ahead + 1;
    }

    private boolean allDone() {
        for (Racer r : racers.values()) if (!r.finished && !r.dnf) return false;
        return true;
    }

    private void end(String reason) {
        if (state != State.RACING) return;
        state = State.ENDING;
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        List<Racer> top = racers.values().stream().filter(r -> r.finished)
                .sorted(Comparator.comparingInt(r -> r.place)).limit(5).toList();
        if (ctx != null) {
            ctx.broadcast(ChatColor.GOLD + "" + ChatColor.BOLD + "ACE RACE - " + reason);
            for (Racer r : top) {
                Player p = Bukkit.getPlayer(r.id);
                ctx.broadcast(ChatColor.GRAY + "" + r.place + ". " + ChatColor.WHITE + (p == null ? "?" : p.getName()));
            }
            ctx.setTimer(0, map.timeLimit);
        }
        endTask = new BukkitRunnable() {
            @Override public void run() {
                if (finishCalled) return;
                finishCalled = true;
                McmGameContext c = ctx;
                if (c != null) c.finish("Finished");
            }
        }.runTaskLater(plugin, Waits.ticks(60L));
    }

    @Override
    public void stop() {
        if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        if (endTask != null) { endTask.cancel(); endTask = null; }
        for (Map.Entry<Block, BlockData> e : saved.entrySet()) e.getKey().setBlockData(e.getValue(), false);
        saved.clear();
        for (Map.Entry<Block, BlockData> e : swapped.entrySet()) e.getKey().setBlockData(e.getValue(), false);
        swapped.clear();
        for (TextDisplay t : labels) if (t.isValid()) t.remove();
        labels.clear();
        for (Racer r : racers.values()) {
            Player p = Bukkit.getPlayer(r.id);
            if (p == null) continue;
            strip(p);
            if (p.getGameMode() == GameMode.SPECTATOR) p.setGameMode(GameMode.ADVENTURE);
        }
        racers.clear();
        ctx = null;
        map = null;
        world = null;
        state = State.IDLE;
    }

    @Override
    public boolean skipWait() {
        if (state == State.PRE) {
            if (countdownTask != null) { countdownTask.cancel(); countdownTask = null; }
            go();
            return true;
        }
        if (state == State.ENDING && endTask != null) {
            endTask.cancel();
            endTask = null;
            if (!finishCalled) { finishCalled = true; if (ctx != null) ctx.finish("Finished"); }
            return true;
        }
        return false;
    }

    @Override
    public boolean onLateJoin(Player player, McmGameContext context) {
        if (world == null) return false;
        McmSpectator.makeSpectator(player, startLoc());
        return true;
    }

    public void onQuit(Player p) {
        Racer r = racers.get(p.getUniqueId());
        if (r == null || r.finished) return;
        r.dnf = true;
        if (state == State.RACING && allDone()) end("All finished");
    }

    private static final class Racer {
        final UUID id;
        int lap = 1, nextWp, place, splitTick, launchReady;
        boolean finished, dnf, cpElytra;
        Location checkpoint, prev;
        Set<String> banks;
        final Map<String, Integer> sounds = new HashMap<>();
        final Map<Integer, Integer> rings = new HashMap<>();

        Racer(UUID id) { this.id = id; }
    }
}
