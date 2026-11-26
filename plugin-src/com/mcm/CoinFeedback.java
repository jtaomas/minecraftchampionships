package com.mcm;

import com.mcm.api.McmCoins;
import com.mcm.api.McmFx;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.title.TitlePart;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Strict MCC coin feedback (see game-plugins/COIN_FEEDBACK.md):
 * <ul>
 *   <li>every award: chat line {@code [+N<coin>] reason} (earner only) and one ding, at most one per tick per player;</li>
 *   <li>personal eliminations / freezes: the crossed-swords ELIMINATION banner, tiers by kills inside a window;</li>
 *   <li>objectives that MCC decorates (lap / room completed): the same box with a green title.</li>
 * </ul>
 * Plain coin awards get no pop-up. Main thread only.
 */
final class CoinFeedback {
    private CoinFeedback() {}

    // timeline in ticks since the kill (R2: dot 0, box 1, icon 4, text 6; text off ~4.2 s, box fades 0.13 s later)
    static final int TEXT_ON = Callout.FULL, TEXT_OFF = 84, FADE_START = 86, FADE_TICKS = 3, END = FADE_START + FADE_TICKS;
    static final int DEFAULT_WINDOW_SECONDS = 10;

    private static Plugin plugin;
    private static final Map<UUID, Integer> LAST_DING = new HashMap<>();
    private static final Map<UUID, Banner> BANNERS = new HashMap<>();
    private static final Map<String, int[]> STREAK = new HashMap<>();   // killer:kind -> {count, last tick}
    private static BukkitTask ticker;

    private static final class Banner {
        final Callout.Spec spec;
        int age;
        Banner(Callout.Spec spec) { this.spec = spec; }
    }

    static void init(Plugin p) { plugin = p; }

    static void shutdown() {
        if (ticker != null) { ticker.cancel(); ticker = null; }
        BANNERS.clear();
        STREAK.clear();
        LAST_DING.clear();
    }

    static void forget(UUID id) {
        BANNERS.remove(id);
        LAST_DING.remove(id);
        STREAK.remove(id + ":k");
        STREAK.remove(id + ":f");
    }

    // ------------------------------------------------------------------ chat + sound (every award)

    /** "40", "22.5" (already multiplied); sign handled by the caller. */
    static String amount(double coins) { return McmCoins.format(Math.abs(coins)); }

    /** {@code [+N<coin>] reason}: white digits, gold coin glyph (pack) or gold ⛃ (no pack), grey reason. */
    static Component chatLine(boolean pack, double coins, String reason) {
        boolean plus = coins > 0;
        Component line = Component.text((plus ? "[+" : "[-") + amount(coins), plus ? NamedTextColor.WHITE : NamedTextColor.RED)
                .append(McmHud.icon(McmHud.COIN, pack, "⛃", NamedTextColor.GOLD))
                .append(Component.text("] ", plus ? NamedTextColor.WHITE : NamedTextColor.RED));
        if (reason != null && !reason.isBlank()) line = line.append(Component.text(reason, NamedTextColor.GRAY));
        return line;
    }

    static void award(Player p, double coins, String reason) {
        if (p == null || coins == 0) return;
        boolean pack = McmFx.hasPack(p);
        p.sendMessage(chatLine(pack, coins, reason));
        if (coins > 0) ding(p, pack);
    }

    private static void ding(Player p, boolean pack) {
        int now = Bukkit.getCurrentTick();
        Integer last = LAST_DING.put(p.getUniqueId(), now);
        if (last != null && last == now) return;   // a burst plays one ding per tick
        // the MCC award ding is the event file mcc.ui.coins_small (its 9.14 kHz -> 10.88 kHz onset is what the VODs show)
        if (!McmFx.packSound(p, "mcc.ui.coins_small", 0.9f, 1f))
            p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.9f, 1.0f);
    }

    // ------------------------------------------------------------------ banners

    /** Personal elimination / freeze: banner for the killer only. {@code victim}/{@code team} may be null. */
    static void kill(Player killer, UUID victim, String victimName, String victimTeam, String reason) {
        if (killer == null || !killer.isOnline()) return;
        // Meltdown pays freezes as "You froze <name>" (the final elimination is "You eliminated <name>")
        boolean freeze = reason != null && reason.toLowerCase(Locale.ROOT).contains("froze");
        int tier = streak(killer.getUniqueId(), freeze ? ":f" : ":k");
        String name = victimName;
        if (name == null && victim != null) name = Bukkit.getOfflinePlayer(victim).getName();
        if (name == null) name = "";
        String word = freeze ? "FREEZE!" : "ELIMINATION!";
        String title = (tier == 1 ? "" : tier == 2 ? "DOUBLE " : "TRIPLE ") + word;
        char icon = freeze ? Callout.FREEZE : tier == 1 ? Callout.SINGLE : tier == 2 ? Callout.DOUBLE : Callout.TRIPLE;
        int team = victimTeam == null ? -1 : Math.max(-1, McmHud.teamIndex(victimTeam));
        show(killer, new Callout.Spec(icon, freeze ? Callout.CYAN : Callout.RED, title, name, team));
    }

    /** Objective graphic (lap / room completed). Only called with a non-null banner. */
    static void objective(Player p, String banner, String reason) {
        if (p == null || !p.isOnline() || banner == null || banner.isBlank()) return;
        String up = banner.toUpperCase(Locale.ROOT);
        char icon = up.contains("LAP") ? Callout.LAP : up.contains("ROOM") ? Callout.ROOM : Callout.OBJECTIVE;
        show(p, new Callout.Spec(icon, Callout.GREEN, banner, reason == null ? "" : reason, -2));
    }

    private static int streak(UUID killer, String kind) {
        int window = Math.max(1, plugin == null ? DEFAULT_WINDOW_SECONDS : plugin.getConfig().getInt("coins.multikill-window-seconds", DEFAULT_WINDOW_SECONDS)) * 20;
        int now = Bukkit.getCurrentTick();
        int[] s = STREAK.computeIfAbsent(killer + kind, k -> new int[]{0, Integer.MIN_VALUE / 2});
        s[0] = now - s[1] <= window ? s[0] + 1 : 1;
        s[1] = now;
        return Math.min(3, s[0]);
    }

    private static void show(Player p, Callout.Spec spec) {
        Banner b = new Banner(spec);
        BANNERS.put(p.getUniqueId(), b);   // one slot: a new banner replaces the old one and restarts the timer
        if (McmFx.hasPack(p)) {
            p.showTitle(Title.title(Component.empty(), Callout.frame(spec, 0, false),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(50L * 100), Duration.ofMillis(50L * FADE_TICKS))));
            startTicker();
        } else {
            // no pack: plain subtitle with the same text, held for the same time
            p.showTitle(Title.title(Component.empty(), spec.plain(),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(50L * (FADE_START - 1)), Duration.ofMillis(50L * FADE_TICKS))));
            startTicker();
        }
    }

    private static void startTicker() {
        if (ticker != null || plugin == null) return;
        ticker = Bukkit.getScheduler().runTaskTimer(plugin, CoinFeedback::tick, 1L, 1L);
    }

    private static void tick() {
        for (Iterator<Map.Entry<UUID, Banner>> it = BANNERS.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Banner> e = it.next();
            Player p = Bukkit.getPlayer(e.getKey());
            Banner b = e.getValue();
            if (p == null || !p.isOnline() || ++b.age >= END) { it.remove(); continue; }
            boolean pack = McmFx.hasPack(p);
            if (pack && b.age <= TEXT_ON) p.sendTitlePart(TitlePart.SUBTITLE, Callout.frame(b.spec, b.age, true));
            else if (pack && b.age == TEXT_OFF) p.sendTitlePart(TitlePart.SUBTITLE, Callout.frame(b.spec, Callout.FULL, false));
            if (b.age == FADE_START && pack)
                p.sendTitlePart(TitlePart.TIMES, Title.Times.times(Duration.ZERO, Duration.ZERO, Duration.ofMillis(50L * FADE_TICKS)));
        }
        if (BANNERS.isEmpty() && ticker != null) { ticker.cancel(); ticker = null; }
    }
}
