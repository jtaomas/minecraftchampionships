package com.mcm.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

/**
 * Shared event-presentation helpers (titles, countdowns, sounds, fireworks, team colours, placements)
 * so every MCC game looks and sounds the same. Since core 3.2.0.
 *
 * <p><b>Threading:</b> every method must be called on the server main thread.
 * <p><b>Text:</b> all strings are legacy-formatted with section-sign codes (use {@link ChatColor}, e.g.
 * {@code ChatColor.RED + "Hi"}). {@code null} strings are treated as empty.
 * <p><b>Player collections:</b> collections are copied when a method schedules work, and players that
 * went offline in the meantime are skipped, so passing {@code ctx.participants()} or
 * {@code Bukkit.getOnlinePlayers()} is always safe.
 * <p><b>Fireworks:</b> every firework spawned here is tagged {@link #FX_TAG}, explodes instantly and
 * never deals damage or knockback (the core cancels damage from tagged fireworks in every world, even
 * during external games).
 */
public final class McmFx {
    private McmFx() {}

    /** Scoreboard tag put on every firework spawned by McmFx. The core cancels all damage from entities with this tag. */
    public static final String FX_TAG = "mcm_fx";

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    // =====================================================================================
    // Titles, action bars, sounds
    // =====================================================================================

    /** MCC's HUD font from the server pack ({@code mcc:hud}); titles and action bars use it for players who have the pack. */
    public static final net.kyori.adventure.key.Key HUD_FONT = net.kyori.adventure.key.Key.key("mcc", "hud");

    /** The component in MCC's HUD font when this player has the server pack, unchanged otherwise. */
    public static Component hudFont(Player p, Component c) {
        return hasPack(p) && c != null ? c.font(HUD_FONT) : c;
    }

    /** Converts a legacy section-sign string to an Adventure component (null-safe). */
    public static Component text(String legacy) {
        return LEGACY.deserialize(legacy == null ? "" : legacy);
    }

    /**
     * Shows a title to one player. Timings are in ticks.
     */
    public static void title(Player p, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (p == null || !p.isOnline()) return;
        p.showTitle(Title.title(hudFont(p, text(title)), hudFont(p, text(subtitle)), Title.Times.times(
                Duration.ofMillis(Math.max(0, fadeInTicks) * 50L),
                Duration.ofMillis(Math.max(0, stayTicks) * 50L),
                Duration.ofMillis(Math.max(0, fadeOutTicks) * 50L))));
    }

    /** Shows a title to every player in the collection. Timings are in ticks. */
    public static void title(Collection<? extends Player> players, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
        if (players == null) return;
        for (Player p : new ArrayList<>(players)) title(p, title, subtitle, fadeInTicks, stayTicks, fadeOutTicks);
    }

    /**
     * Big "map name" style title card: slow fade in, ~3.5 s on screen, slow fade out (about 1 s in admin fast-test mode), plus a soft
     * amethyst chime. Example: {@code titleCard(ctx.participants(), "§b§lFrozen Harbour", "§7An icy dockyard")}.
     * If the title has no colour code it is shown gold and bold.
     */
    public static void titleCard(Collection<? extends Player> players, String title, String subtitle) {
        String t = title == null ? "" : title;
        if (!t.startsWith(String.valueOf(ChatColor.COLOR_CHAR))) t = ChatColor.GOLD + "" + ChatColor.BOLD + t;
        String s = subtitle == null ? "" : subtitle;
        if (!s.isEmpty() && !s.startsWith(String.valueOf(ChatColor.COLOR_CHAR))) s = ChatColor.GRAY + s;
        if (Waits.fast()) title(players, t, s, 4, 12, 4); // fast-test mode: ~1 s
        else title(players, t, s, 12, 70, 20);
        sound(players, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 0.8f);
        sound(players, Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 1.2f);
    }

    /** Sends an action-bar message to one player. */
    public static void actionBar(Player p, String message) {
        if (p == null || !p.isOnline()) return;
        p.sendActionBar(hudFont(p, text(message)));
    }

    /** Sends an action-bar message to every player in the collection. */
    public static void actionBar(Collection<? extends Player> players, String message) {
        if (players == null) return;
        for (Player p : new ArrayList<>(players)) actionBar(p, message);
    }

    /** Plays a sound to each player at their own position (only they hear it). */
    public static void sound(Collection<? extends Player> players, Sound sound, float volume, float pitch) {
        if (players == null || sound == null) return;
        for (Player p : new ArrayList<>(players)) sound(p, sound, volume, pitch);
    }

    /** Plays a sound to one player at their own position (only they hear it). */
    // ---- server resource pack sounds (keys "mcc.*" from the Block Showdown pack) ----
    private static final java.util.Set<java.util.UUID> PACK = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** Core only: records whether this player's client has the server pack loaded. */
    public static void packLoaded(java.util.UUID id, boolean loaded) { if (loaded) PACK.add(id); else PACK.remove(id); }

    /** True when the player's client has the server resource pack (so "mcc.*" sounds are audible). */
    public static boolean hasPack(Player p) { return p != null && PACK.contains(p.getUniqueId()); }

    /** Plays a pack sound (e.g. "mcc.go") to one player if they have the pack. Returns false if they do not. */
    public static boolean packSound(Player p, String key, float volume, float pitch) {
        if (!hasPack(p) || !p.isOnline()) return false;
        p.playSound(p.getLocation(), "minecraft:" + key, org.bukkit.SoundCategory.MASTER, volume, pitch);
        return true;
    }

    /** Pack sound for those who have the pack; everyone else is returned so the caller can play a vanilla fallback. */
    public static List<Player> packSound(Collection<? extends Player> players, String key, float volume, float pitch) {
        List<Player> without = new ArrayList<>();
        if (players == null) return without;
        for (Player p : players) if (!packSound(p, key, volume, pitch)) without.add(p);
        return without;
    }

    public static void sound(Player p, Sound sound, float volume, float pitch) {
        if (p == null || sound == null || !p.isOnline()) return;
        p.playSound(p.getLocation(), sound, volume, pitch);
    }

    /** Plays a sound in the world at a location (everyone nearby hears it). */
    public static void soundAt(Location loc, Sound sound, float volume, float pitch) {
        if (loc == null || loc.getWorld() == null || sound == null) return;
        loc.getWorld().playSound(loc, sound, volume, pitch);
    }

    // =====================================================================================
    // Countdown
    // =====================================================================================

    /**
     * Big coloured countdown: shows {@code seconds}, {@code seconds-1} ... {@code 1} as titles one second
     * apart (3 = red, 2 = yellow, 1 = green, higher numbers aqua) with a rising note-block pling, then a big
     * green "GO!" with a strong sound, then runs {@code onGo} (on the main thread, exceptions are caught and logged).
     *
     * <p>The first number is shown immediately; {@code onGo} runs {@code seconds * 20} ticks after the call
     * (next tick if {@code seconds <= 0}). Cancelling the returned task stops the countdown and {@code onGo}
     * is then never run - cancel it from your game's {@code stop()}.
     * In admin fast-test mode ({@link McmApi#isFastTest()}) the countdown runs for at most 1 second.
     *
     * @param plugin   your plugin (owner of the scheduled task)
     * @param players  who sees and hears the countdown (copied; offline players skipped)
     * @param seconds  number of seconds to count down from (typically 3 to 10)
     * @param subtitle subtitle under each number (may be null), e.g. "§7Get ready!"
     * @param onGo     run when "GO!" shows; may be null
     * @return the running task (cancellable)
     */
    public static BukkitTask countdown(Plugin plugin, Collection<? extends Player> players, int seconds, String subtitle, Runnable onGo) {
        final List<Player> audience = players == null ? new ArrayList<>() : new ArrayList<>(players);
        final String sub = subtitle == null ? "" : subtitle;
        // fast-test mode (admin): at most 1 second
        final int total = Waits.seconds(Math.max(0, seconds));
        RUNNING.removeIf(Countdown::cancelledOutside);
        Countdown cd = new Countdown(plugin, audience, sub, total, onGo);
        return cd.runTaskTimer(plugin, 0L, 20L);
    }

    /** Countdowns that are running right now (for the admin's /mcm skipwait). */
    private static final java.util.Set<Countdown> RUNNING = new java.util.LinkedHashSet<>();

    /**
     * Internal (core, /mcm skipwait): finish every running {@link #countdown} now - shows "GO!" and runs its
     * onGo immediately. Returns how many were finished.
     */
    public static int finishRunningCountdowns() {
        List<Countdown> list = new ArrayList<>(RUNNING);
        int n = 0;
        for (Countdown c : list) { if (c.finishNow()) n++; }
        return n;
    }

    private static final class Countdown extends BukkitRunnable {
        private final Plugin plugin;
        private final List<Player> audience;
        private final String sub;
        private final Runnable onGo;
        private int n;
        private boolean done;

        Countdown(Plugin plugin, List<Player> audience, String sub, int total, Runnable onGo) {
            this.plugin = plugin; this.audience = audience; this.sub = sub; this.n = total; this.onGo = onGo;
            RUNNING.add(this);
        }

        private List<Player> online() {
            List<Player> online = new ArrayList<>();
            for (Player p : audience) if (p.isOnline()) online.add(p);
            return online;
        }

        @Override public void run() {
            if (done) { try { cancel(); } catch (IllegalStateException ignored) {} return; }
            List<Player> online = online();
            if (n > 0) {
                ChatColor c = n == 1 ? ChatColor.GREEN : n == 2 ? ChatColor.YELLOW : n == 3 ? ChatColor.RED : ChatColor.AQUA;
                title(online, c + "" + ChatColor.BOLD + n, sub, 0, 18, 4);
                float pitch = (float) Math.max(0.5, Math.min(2.0, 1.4 - 0.2 * (n - 1)));
                List<Player> plain = n <= 3 ? packSound(online, "mcc.countdown", 1f, 1f) : online;
                sound(plain, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, pitch);
                sound(plain, Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, pitch);
                n--;
                return;
            }
            go();
        }

        boolean cancelledOutside() {
            if (done) return true;
            try { if (isCancelled()) { done = true; return true; } } catch (IllegalStateException ignored) {}
            return false;
        }

        boolean finishNow() {
            if (cancelledOutside()) { RUNNING.remove(this); return false; }
            go();
            return true;
        }

        private void go() {
            if (done) return;
            done = true;
            RUNNING.remove(this);
            try { cancel(); } catch (IllegalStateException ignored) {}
            List<Player> online = online();
            title(online, ChatColor.GREEN + "" + ChatColor.BOLD + "GO!", "", 0, 20, 10);
            List<Player> plain = packSound(online, "mcc.go", 1f, 1f);
            sound(plain, Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            sound(plain, Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
            sound(plain, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 0.8f, 1f);
            safeRun(plugin, onGo, "countdown onGo");
        }
    }

    // =====================================================================================
    // Fireworks & particles
    // =====================================================================================

    /** Instant team-coloured firework burst at a location (no damage). {@code big} = large ball with trail and flicker. */
    public static Firework teamFirework(Location loc, ChatColor color, boolean big) {
        return teamFirework(loc, colorOf(color), big);
    }

    /**
     * Instant firework burst at a location in the given colour (white fade), tagged {@link #FX_TAG} so it
     * never damages anyone. {@code big} = large ball with trail and flicker, otherwise a small ball/burst.
     * Returns the firework entity (already detonating) or null if the location has no world.
     */
    public static Firework teamFirework(Location loc, Color color, boolean big) {
        if (loc == null || loc.getWorld() == null) return null;
        Color c = color == null ? Color.WHITE : color;
        FireworkEffect.Type type = big ? FireworkEffect.Type.BALL_LARGE
                : (ThreadLocalRandom.current().nextBoolean() ? FireworkEffect.Type.BALL : FireworkEffect.Type.BURST);
        return spawnFirework(loc, FireworkEffect.builder().withColor(c).withFade(Color.WHITE)
                .with(type).trail(big).flicker(big).build());
    }

    /** Spawns an instantly-detonating, damage-free, {@link #FX_TAG}-tagged firework with one effect. */
    public static Firework spawnFirework(Location loc, FireworkEffect effect) {
        if (loc == null || loc.getWorld() == null || effect == null) return null;
        Firework fw = loc.getWorld().spawn(loc, Firework.class, f -> {
            f.addScoreboardTag(FX_TAG);
            FireworkMeta fm = f.getFireworkMeta();
            fm.clearEffects();
            fm.addEffect(effect);
            fm.setPower(0);
            f.setFireworkMeta(fm);
            f.setSilent(false);
        });
        fw.detonate();
        return fw;
    }

    /** True if the entity is a firework spawned by McmFx (used by the core to cancel its damage). */
    public static boolean isFxFirework(Entity e) {
        return e instanceof Firework && e.getScoreboardTags().contains(FX_TAG);
    }

    /**
     * Firework show around a centre point: 1-2 bursts every 10 ticks for {@code seconds} seconds (was 2-3 every
     * 5 ticks; cut after playtest feedback "too many fireworks"), random
     * offsets within ~7 blocks horizontally and 3-12 blocks above the centre, in {@code color} mixed with
     * white/gold. Cancel the returned task to stop early.
     */
    public static BukkitTask fireworkShow(Plugin plugin, Location center, Color color, int seconds) {
        final Location c = center == null ? null : center.clone();
        final Color main = color == null ? Color.YELLOW : color;
        final int ticks = Waits.ticks(Math.max(1, seconds) * 20);
        return new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (c == null || c.getWorld() == null || t >= ticks) { cancel(); return; }
                ThreadLocalRandom r = ThreadLocalRandom.current();
                int n = 1 + r.nextInt(2);
                for (int i = 0; i < n; i++) {
                    Location l = c.clone().add(r.nextDouble(-7, 7), r.nextDouble(3, 12), r.nextDouble(-7, 7));
                    FireworkEffect.Type[] types = FireworkEffect.Type.values();
                    FireworkEffect.Type type = types[r.nextInt(types.length)];
                    if (type == FireworkEffect.Type.CREEPER) type = FireworkEffect.Type.STAR;
                    Color second = r.nextBoolean() ? Color.WHITE : Color.fromRGB(0xFFD24A);
                    spawnFirework(l, FireworkEffect.builder().withColor(main, second).withFade(main)
                            .with(type).trail(r.nextBoolean()).flicker(r.nextBoolean()).build());
                }
                t += 10;
            }
        }.runTaskTimer(plugin, 0L, 10L);
    }

    /** {@link #celebrate(Plugin, Collection, Color)} with a gold/random-colour mix. */
    public static BukkitTask celebrate(Plugin plugin, Collection<? extends Player> winners) {
        return celebrate(plugin, winners, null);
    }

    /**
     * Fireworks around each winner for ~5 seconds, 1 s in admin fast-test mode (one burst per winner every 16 ticks, 1-3 blocks away,
     * 2-5 blocks up) plus a victory sound for them. A winner who goes offline or changes world stops getting
     * fireworks. {@code color} null = gold mixed with random bright colours.
     */
    public static BukkitTask celebrate(Plugin plugin, Collection<? extends Player> winners, Color color) {
        final List<Player> list = winners == null ? new ArrayList<>() : new ArrayList<>(winners);
        final Map<UUID, World> startWorld = new java.util.HashMap<>();
        for (Player p : list) startWorld.put(p.getUniqueId(), p.getWorld());
        sound(packSound(list, "mcc.victory.win", 0.5f, 1f), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.4f, 1f);   // owner: end sound too loud
        final int celebrateTicks = Waits.ticks(100);
        final Color[] bright = {Color.fromRGB(0xFFD24A), Color.fromRGB(0xFF3B3B), Color.fromRGB(0x5FD8FF),
                Color.fromRGB(0x7CFF3B), Color.fromRGB(0xFF6FD8), Color.fromRGB(0xB86BFF)};
        return new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (t >= celebrateTicks) { cancel(); return; }
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (Player p : list) {
                    if (!p.isOnline() || p.getWorld() != startWorld.get(p.getUniqueId())) continue;
                    double ang = r.nextDouble(Math.PI * 2);
                    double dist = r.nextDouble(1.5, 3.2);
                    Location l = p.getLocation().add(Math.cos(ang) * dist, r.nextDouble(2, 5), Math.sin(ang) * dist);
                    Color c = color != null ? color : bright[r.nextInt(bright.length)];
                    teamFirework(l, c, r.nextInt(3) == 0);
                }
                t += 16;
            }
        }.runTaskTimer(plugin, 0L, 16L);
    }

    /** Puff of coloured dust plus cloud particles at a location (e.g. pickups, eliminations). */
    public static void burst(Location loc, Color color, int count) {
        if (loc == null || loc.getWorld() == null) return;
        Color c = color == null ? Color.WHITE : color;
        int n = Math.max(1, count);
        loc.getWorld().spawnParticle(Particle.DUST, loc, n, 0.4, 0.7, 0.4, 0, new Particle.DustOptions(c, 1.6f));
        loc.getWorld().spawnParticle(Particle.CLOUD, loc, Math.max(1, n / 2), 0.3, 0.5, 0.3, 0.05);
    }

    // =====================================================================================
    // Team colours
    // =====================================================================================

    private static final Map<ChatColor, DyeColor> DYES = new EnumMap<>(ChatColor.class);
    private static final Map<ChatColor, Color> COLORS = new EnumMap<>(ChatColor.class);
    static {
        dye(ChatColor.RED, DyeColor.RED, 0xFF3B3B);
        dye(ChatColor.DARK_RED, DyeColor.RED, 0xAA1E1E);
        dye(ChatColor.GOLD, DyeColor.ORANGE, 0xFF9A1F);
        dye(ChatColor.YELLOW, DyeColor.YELLOW, 0xFFE53B);
        dye(ChatColor.GREEN, DyeColor.LIME, 0x7CFF3B);
        dye(ChatColor.DARK_GREEN, DyeColor.GREEN, 0x2E8B2E);
        dye(ChatColor.DARK_AQUA, DyeColor.CYAN, 0x16A3A3);
        dye(ChatColor.AQUA, DyeColor.LIGHT_BLUE, 0x5FD8FF);
        dye(ChatColor.BLUE, DyeColor.BLUE, 0x3B5BFF);
        dye(ChatColor.DARK_BLUE, DyeColor.BLUE, 0x1E2A8C);
        dye(ChatColor.DARK_PURPLE, DyeColor.PURPLE, 0x8B2BE2);
        dye(ChatColor.LIGHT_PURPLE, DyeColor.PINK, 0xFF6FD8);
        dye(ChatColor.WHITE, DyeColor.WHITE, 0xFFFFFF);
        dye(ChatColor.GRAY, DyeColor.LIGHT_GRAY, 0xAAAAAA);
        dye(ChatColor.DARK_GRAY, DyeColor.GRAY, 0x555555);
        dye(ChatColor.BLACK, DyeColor.BLACK, 0x222222);
    }
    private static void dye(ChatColor c, DyeColor d, int rgb) { DYES.put(c, d); COLORS.put(c, Color.fromRGB(rgb)); }

    /**
     * Firework/leather/particle colour for a chat colour (bright, saturated). Formatting codes and null give white.
     * Team mapping: Red=RED, Orange=GOLD, Yellow=YELLOW, Lime=GREEN, Green=DARK_GREEN, Cyan=DARK_AQUA,
     * Aqua=AQUA, Blue=BLUE, Purple=DARK_PURPLE, Pink=LIGHT_PURPLE.
     */
    public static Color colorOf(ChatColor c) {
        Color col = c == null ? null : COLORS.get(c);
        return col == null ? Color.WHITE : col;
    }

    /**
     * Dye colour for a chat colour: RED/DARK_RED=red, GOLD=orange, YELLOW=yellow, GREEN=lime, DARK_GREEN=green,
     * DARK_AQUA=cyan, AQUA=light blue, BLUE/DARK_BLUE=blue, DARK_PURPLE=purple, LIGHT_PURPLE=pink, WHITE=white,
     * GRAY=light gray, DARK_GRAY=gray, BLACK=black. Null/formatting codes give white.
     */
    public static DyeColor dyeOf(ChatColor c) {
        DyeColor d = c == null ? null : DYES.get(c);
        return d == null ? DyeColor.WHITE : d;
    }

    private static Material dyed(ChatColor c, String suffix) {
        Material m = Material.matchMaterial(dyeOf(c).name() + suffix);
        return m == null ? Material.matchMaterial("WHITE" + suffix) : m;
    }

    /** Team wool, e.g. RED -> RED_WOOL, AQUA -> LIGHT_BLUE_WOOL, LIGHT_PURPLE -> PINK_WOOL. */
    public static Material woolOf(ChatColor c) { return dyed(c, "_WOOL"); }
    /** Team concrete. */
    public static Material concreteOf(ChatColor c) { return dyed(c, "_CONCRETE"); }
    /** Team concrete powder (falls if physics apply - place with physics off). */
    public static Material concretePowderOf(ChatColor c) { return dyed(c, "_CONCRETE_POWDER"); }
    /** Team stained glass (full block). */
    public static Material glassOf(ChatColor c) { return dyed(c, "_STAINED_GLASS"); }
    /** Team stained glass pane. */
    public static Material glassPaneOf(ChatColor c) { return dyed(c, "_STAINED_GLASS_PANE"); }
    /** Team terracotta (dyed, not glazed). */
    public static Material terracottaOf(ChatColor c) { return dyed(c, "_TERRACOTTA"); }
    /** Team glazed terracotta. */
    public static Material glazedTerracottaOf(ChatColor c) { return dyed(c, "_GLAZED_TERRACOTTA"); }
    /** Team carpet. */
    public static Material carpetOf(ChatColor c) { return dyed(c, "_CARPET"); }
    /** Team standing banner (plain, block or item). */
    public static Material bannerOf(ChatColor c) { return dyed(c, "_BANNER"); }
    /** Team wall banner (block only; set its {@code Directional} facing). */
    public static Material wallBannerOf(ChatColor c) { return dyed(c, "_WALL_BANNER"); }
    /** Team candle. */
    public static Material candleOf(ChatColor c) { return dyed(c, "_CANDLE"); }
    /** Team bed (item). */
    public static Material bedOf(ChatColor c) { return dyed(c, "_BED"); }
    /** Team shulker box. */
    public static Material shulkerBoxOf(ChatColor c) { return dyed(c, "_SHULKER_BOX"); }

    // =====================================================================================
    // Sides
    // =====================================================================================

    /** Side key of a player: "TEAM:&lt;team&gt;" or "SOLO:&lt;uuid&gt;" (same format as {@link McmGameContext#sides()}). */
    public static String sideKeyOf(McmGameContext ctx, Player p) {
        if (p == null) return null;
        String team = ctx == null ? null : ctx.teamOf(p.getUniqueId());
        return team != null ? "TEAM:" + team : "SOLO:" + p.getUniqueId();
    }

    /** Chat colour of a side: its team colour for "TEAM:" keys, white for solo players or null keys. */
    public static ChatColor sideColor(McmGameContext ctx, String sideKey) {
        if (sideKey == null || !sideKey.startsWith("TEAM:")) return ChatColor.WHITE;
        String team = sideKey.substring(5);
        if (ctx != null) {
            ChatColor c = ctx.teamColor(team);
            if (c != null) return c;
        }
        return switch (team) {
            case "Red" -> ChatColor.RED;
            case "Orange" -> ChatColor.GOLD;
            case "Yellow" -> ChatColor.YELLOW;
            case "Lime" -> ChatColor.GREEN;
            case "Green" -> ChatColor.DARK_GREEN;
            case "Cyan" -> ChatColor.DARK_AQUA;
            case "Aqua" -> ChatColor.AQUA;
            case "Blue" -> ChatColor.BLUE;
            case "Purple" -> ChatColor.DARK_PURPLE;
            case "Pink" -> ChatColor.LIGHT_PURPLE;
            default -> ChatColor.WHITE;
        };
    }

    /** Coloured display label of a side (uses ctx.sideLabel; falls back to the raw key without ctx). */
    public static String sideLabel(McmGameContext ctx, String sideKey) {
        if (sideKey == null) return "?";
        if (ctx != null) return ctx.sideLabel(sideKey);
        if (sideKey.startsWith("TEAM:")) return sideColor(null, sideKey) + sideKey.substring(5) + " Team";
        return sideKey;
    }

    /**
     * Online players belonging to a side key, regardless of whether they are still participants
     * ("TEAM:x" = online players whose ctx.teamOf is x; "SOLO:uuid" = that player if online).
     */
    public static List<Player> playersOfSide(McmGameContext ctx, String sideKey) {
        List<Player> out = new ArrayList<>();
        if (sideKey == null) return out;
        if (sideKey.startsWith("SOLO:")) {
            try {
                Player p = Bukkit.getPlayer(UUID.fromString(sideKey.substring(5)));
                if (p != null && p.isOnline()) out.add(p);
            } catch (IllegalArgumentException ignored) {}
            return out;
        }
        if (sideKey.startsWith("TEAM:") && ctx != null) {
            String team = sideKey.substring(5);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (team.equals(ctx.teamOf(p.getUniqueId()))) out.add(p);
            }
        }
        return out;
    }

    private static String coloredName(McmGameContext ctx, Player p) {
        return sideColor(ctx, sideKeyOf(ctx, p)) + p.getName();
    }

    // =====================================================================================
    // Eliminations
    // =====================================================================================

    /**
     * Standard elimination feedback: chat broadcast ("☠ Victim was eliminated by Killer", names in team colours),
     * a red "ELIMINATED" title + break sound for the victim and a team-coloured particle burst at the victim.
     * Nothing is shown to the killer any more: the core's ELIMINATION banner and award ding come with
     * {@code ctx.addKillCoins}. Does NOT change game modes or award points.
     *
     * @param victim       the eliminated player (null = no-op)
     * @param killerOrNull the killer, or null for void/hazard deaths
     * @param ctx          the game context (used for team colours and the broadcast); may be null
     */
    public static void eliminated(Player victim, Player killerOrNull, McmGameContext ctx) {
        if (victim == null) return;
        String v = coloredName(ctx, victim);
        String msg = killerOrNull != null && !killerOrNull.equals(victim)
                ? ChatColor.RED + "☠ " + v + ChatColor.GRAY + " was eliminated by " + coloredName(ctx, killerOrNull)
                : ChatColor.RED + "☠ " + v + ChatColor.GRAY + " was eliminated";
        if (ctx != null) ctx.broadcast(msg); else Bukkit.broadcastMessage(msg);

        title(victim, ChatColor.RED + "" + ChatColor.BOLD + "ELIMINATED",
                killerOrNull != null && !killerOrNull.equals(victim) ? ChatColor.GRAY + "by " + coloredName(ctx, killerOrNull) : "",
                0, 40, 15);
        if (!packSound(victim, "mcc.death", 1f, 1f)) {
            sound(victim, Sound.ENTITY_ITEM_BREAK, 1f, 0.6f);
            sound(victim, Sound.ENTITY_PLAYER_HURT, 0.8f, 0.8f);
        }
        // killer side: the core's ELIMINATION banner + award ding (addKillCoins) replace the old action bar and sounds
        burst(victim.getLocation().add(0, 1, 0), colorOf(sideColor(ctx, sideKeyOf(ctx, victim))), 40);
    }

    /**
     * Standard "team eliminated" feedback: chat broadcast, a subtitle for every online player, a thunder
     * sting, and a red "TEAM ELIMINATED" title for that side's own members.
     */
    public static void teamEliminated(String sideKey, McmGameContext ctx) {
        if (sideKey == null) return;
        String label = sideLabel(ctx, sideKey);
        String msg = ChatColor.DARK_RED + "✖ " + label + ChatColor.GRAY + " has been eliminated!";
        if (ctx != null) ctx.broadcast(msg); else Bukkit.broadcastMessage(msg);
        List<Player> own = playersOfSide(ctx, sideKey);
        List<Player> others = new ArrayList<>(Bukkit.getOnlinePlayers());
        others.removeAll(own);
        title(others, "", label + ChatColor.GRAY + " eliminated!", 5, 40, 10);
        title(own, ChatColor.RED + "" + ChatColor.BOLD + "TEAM ELIMINATED", label, 5, 50, 15);
        sound(Bukkit.getOnlinePlayers(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.3f);
        sound(Bukkit.getOnlinePlayers(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
    }

    // =====================================================================================
    // Placements
    // =====================================================================================

    /** {@link #placementReveal(Plugin, McmGameContext, List, List, Runnable)} without detail lines. */
    public static BukkitTask placementReveal(Plugin plugin, McmGameContext ctx, List<String> sideKeysInOrder, Runnable after) {
        return placementReveal(plugin, ctx, sideKeysInOrder, null, after);
    }

    /**
     * Dramatic placement reveal shown to every online player: 3rd, then 2nd, then 1st, 2.5 s apart (whole reveal ~1 s in admin fast-test mode), each with
     * a title, chat line and sound; 1st also gets fireworks + {@link #celebrate} around its online members.
     * Only the first three keys are used; with fewer sides the missing places are skipped.
     * {@code after} runs ~3 s after 1st is revealed (main thread; exceptions logged). It is never run
     * synchronously inside this call (with no keys it runs on the next tick), and never if the returned task
     * is cancelled first.
     *
     * @param sideKeysInOrder side keys, best first ("TEAM:Red", "SOLO:&lt;uuid&gt;")
     * @param details         optional text per place (same order, may be shorter or null), shown after the
     *                        label, e.g. "+85 coins"
     */
    public static BukkitTask placementReveal(Plugin plugin, McmGameContext ctx, List<String> sideKeysInOrder, List<String> details, Runnable after) {
        final List<String> keys = new ArrayList<>();
        if (sideKeysInOrder != null) for (String k : sideKeysInOrder) { if (k != null && keys.size() < 3) keys.add(k); }
        final List<String> det = details == null ? List.of() : new ArrayList<>(details);
        final int n = keys.size();
        final boolean fast = Waits.fast();
        final int step = fast ? 6 : 50;          // fast-test mode: whole reveal in ~1 s
        final int endTick = n == 0 ? 0 : (n - 1) * step + (fast ? 8 : 60);
        return new BukkitRunnable() {
            int t = 0;
            @Override public void run() {
                if (n > 0 && t % step == 0 && t / step < n) {
                    int place = n - 1 - t / step; // index into keys: last place first
                    reveal(plugin, ctx, keys.get(place), place, place < det.size() ? det.get(place) : null);
                }
                if (t >= endTick) {
                    cancel();
                    safeRun(plugin, after, "placementReveal after");
                    return;
                }
                t++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static void reveal(Plugin plugin, McmGameContext ctx, String key, int place, String detail) {
        String label = sideLabel(ctx, key);
        String[] names = {"1ST PLACE", "2ND PLACE", "3RD PLACE"};
        ChatColor[] cols = {ChatColor.YELLOW, ChatColor.WHITE, ChatColor.GOLD};
        String d = detail == null || detail.isEmpty() ? "" : ChatColor.GRAY + " (" + detail + ChatColor.GRAY + ")";
        Collection<? extends Player> all = Bukkit.getOnlinePlayers();
        String head = cols[place] + "" + ChatColor.BOLD + names[place];
        String chat = cols[place] + "" + ChatColor.BOLD + "#" + (place + 1) + " " + ChatColor.RESET + label + d;
        if (ctx != null) ctx.broadcast(chat); else Bukkit.broadcastMessage(chat);
        if (Waits.fast()) title(all, head, label + d, 2, place == 0 ? 16 : 8, 2);
        else title(all, head, label + d, 5, place == 0 ? 70 : 40, 10);
        if (place == 0) {
            List<Player> plainAll = packSound(all, "mcc.victory.big_success", 0.5f, 1f);   // owner: end sound too loud
            sound(plainAll, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.4f, 1f);
            sound(plainAll, Sound.ENTITY_PLAYER_LEVELUP, 0.4f, 1f);
            List<Player> winners = playersOfSide(ctx, key);
            Color c = colorOf(sideColor(ctx, key));
            for (Player w : winners) teamFirework(w.getLocation().add(0, 2.5, 0), c, true);
            if (!winners.isEmpty()) celebrate(plugin, winners, c);
        } else {
            sound(all, Sound.BLOCK_NOTE_BLOCK_BELL, 1f, place == 1 ? 1.0f : 0.8f);
            sound(all, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, place == 1 ? 1.2f : 0.9f);
        }
    }

    private static void safeRun(Plugin plugin, Runnable r, String what) {
        if (r == null) return;
        try { r.run(); } catch (Throwable t) {
            if (plugin != null) plugin.getLogger().log(Level.SEVERE, "McmFx " + what + " threw", t);
            else Bukkit.getLogger().log(Level.SEVERE, "McmFx " + what + " threw", t);
        }
    }
}
