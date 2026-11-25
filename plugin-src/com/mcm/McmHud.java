package com.mcm;

import com.mcm.api.McmFx;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MCC-style HUD: the sidebar ("MCC: EVENT [n/8]: GAME", MAP, ROUNDS, game lines, GAME COINS with team icons, dot
 * leaders and coin glyphs), the timer-only boss bar, the tab list header/footer and team icons for chat and tab.
 *
 * <p>Players with the server pack get MCC's own HUD font ({@code mcc:hud}) and icon glyphs ({@code mcc:icon},
 * codepoints set in {@code _candidates/pack/build_pack.py}); players without it get the same layout in the vanilla
 * font with coloured squares in place of icons.
 */
final class McmHud {
    static final Key HUD = Key.key("mcc", "hud");
    static final Key ICON = Key.key("mcc", "icon");
    static final char COIN = '', CROWN = '', TROPHY = '', SKULL = '', TICK = '',
            CROSS = '', STAR = '', PLAYER = '', TEAM_BASE = '', SPECTATOR = '';
    /** MCC team names, in {@link TeamScoring#TEAM_NAMES} order; the icons in the pack are these animals. */
    private static final String[] ANIMALS = {"Rabbits", "Ocelots", "Yaks", "Llamas", "Geckos", "Coyotes", "Axolotls", "Bats", "Pandas", "Parrots"};
    private static final TextColor LABEL = NamedTextColor.AQUA, VALUE = NamedTextColor.WHITE, HEAD = NamedTextColor.YELLOW;
    private static final String[] ENTRIES = new String[15];
    static {
        for (int i = 0; i < ENTRIES.length; i++) ENTRIES[i] = ChatColor.values()[i].toString() + ChatColor.RESET;
    }

    /** Everything the sidebar needs for one update, gathered by the plugin. */
    record State(String event, String game, int gameNo, int gameTotal, String map, int round, int rounds,
                 List<String> gameLines, Map<String, Integer> sideCoins, boolean gameCoins, int online,
                 double multiplier) { }

    private final TeamScoring scoring;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private final BossBar bar = BossBar.bossBar(Component.empty(), 1f, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

    McmHud(TeamScoring scoring) { this.scoring = scoring; }

    // ------------------------------------------------------------------ text helpers

    static Component hud(String text, TextColor color, boolean pack) {
        // owner: the sidebar uses the default Minecraft font in normal case, like old MCC (was mcc:hud, upper-cased)
        return Component.text(text, color);
    }

    static Component icon(char glyph, boolean pack, String fallback, TextColor fallbackColor) {
        return pack ? Component.text(String.valueOf(glyph), NamedTextColor.WHITE).font(ICON)
                : Component.text(fallback, fallbackColor);
    }

    static int teamIndex(String team) {
        if (team == null) return -1;
        for (int i = 0; i < TeamScoring.TEAM_NAMES.length; i++) if (TeamScoring.TEAM_NAMES[i].equals(team)) return i;
        return -1;
    }

    static String teamDisplay(String team) {
        int i = teamIndex(team);
        return i < 0 ? (team == null ? "Solo" : team) : team + " " + ANIMALS[i];
    }

    Component teamIcon(String team, boolean pack) {
        int i = teamIndex(team);
        TextColor col = color(scoring.teamColor(team));
        return i < 0 ? icon(PLAYER, pack, "■", col) : icon((char) (TEAM_BASE + i), pack, "■", col);
    }

    static TextColor color(ChatColor c) {
        if (c == null) return NamedTextColor.WHITE;
        java.awt.Color a = c.asBungee().getColor();
        return a == null ? NamedTextColor.WHITE : TextColor.color(a.getRGB() & 0xFFFFFF);
    }

    /** Vanilla font advance in GUI pixels, used to line up the dot leaders. */
    static int width(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            w += switch (c) {
                case '!', '\'', ',', '.', ':', ';', 'i', '|' -> 2;
                case '`', 'l' -> 3;
                case ' ', 'I', 't', '[', ']' -> 4;
                case '"', '(', ')', '*', '<', '>', 'f', 'k', '{', '}' -> 5;
                case '@', '~' -> 7;
                default -> 6;
            };
        }
        return w;
    }

    /** mcc:hud advances (hud.ttf at size 7, measured), for ' '..'~'; lowercase never reaches it (text is upper-cased). */
    private static final String HUD_CHARS = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`{|}~";
    private static final int[] HUD_W = {4, 2, 4, 6, 6, 6, 6, 2, 3, 3, 4, 6, 3, 5, 2, 6, 5, 4, 5, 5, 5, 5, 5, 5, 5, 5, 2, 3, 4, 5, 4, 5, 6, 6, 6, 6, 6, 5, 5, 6, 6, 6, 6, 5, 5, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 3, 6, 3, 6, 5, 3, 4, 2, 4, 5};

    /** Rendered width of a component in GUI pixels, following fonts down the tree. */
    static int measure(Component c) { return measure(c, null); }

    private static int measure(Component c, Key font) {
        Key f = c.font() != null ? c.font() : font;
        int w = c instanceof TextComponent t ? textWidth(t.content(), f) : 0;
        for (Component child : c.children()) w += measure(child, f);
        return w;
    }

    private static int textWidth(String s, Key font) {
        if (ICON.equals(font)) {
            int w = 0;
            for (int i = 0; i < s.length(); i++) w += s.charAt(i) == ' ' ? 4 : 9;
            return w;
        }
        if (!HUD.equals(font)) return width(s);
        int w = 0;
        for (int i = 0; i < s.length(); i++) {
            int k = HUD_CHARS.indexOf(s.charAt(i));
            w += k < 0 ? 6 : HUD_W[k];
        }
        return w;
    }

    // ------------------------------------------------------------------ sidebar

    void sidebar(Player p, State s) {
        boolean pack = McmFx.hasPack(p);
        Scoreboard sb = boards.get(p.getUniqueId());
        if (sb == null || p.getScoreboard() != sb) {
            sb = Bukkit.getScoreboardManager().getNewScoreboard();
            boards.put(p.getUniqueId(), sb);
        }
        Objective obj = sb.getObjective("mcmhud");
        if (obj == null) {
            obj = sb.registerNewObjective("mcmhud", Criteria.DUMMY, Component.text("MCM"));
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.numberFormat(NumberFormat.blank());
        }
        Component title = hud(s.event(), HEAD, pack);
        if (s.game() != null) {
            if (s.gameTotal() > 0) title = title.append(hud(" [" + s.gameNo() + "/" + s.gameTotal() + "]", NamedTextColor.GRAY, pack));
            title = title.append(hud(": ", NamedTextColor.GRAY, pack)).append(hud(s.game(), VALUE, pack));
        }
        obj.displayName(title);

        List<Component> left = new ArrayList<>();
        List<Component> right = new ArrayList<>();
        if (s.game() != null) {
            if (s.map() != null && !s.map().isBlank()) add(left, right, labelled("Map", s.map(), LABEL, pack), null);
            if (s.rounds() > 1) add(left, right, rounds(s.round(), s.rounds(), pack), null);
            for (String line : s.gameLines()) {
                Component c = gameLine(line, s, pack);
                if (c != null && left.size() < 6) add(left, right, c, null);
            }
        } else {
            String t = scoring.getTeam(p.getUniqueId());
            add(left, right, hud("Team: ", LABEL, pack).append(teamIcon(t, pack)).append(Component.space())
                    .append(Component.text(teamDisplay(t), color(scoring.teamColor(t)))), null);
            add(left, right, labelled("Players Online", String.valueOf(s.online()), NamedTextColor.GREEN, pack), null);
        }
        add(left, right, Component.empty(), null);
        // MCC: "Game Coins: (x2.5)" - the event multiplier of this game slot, in yellow parentheses
        Component coinHead = hud(s.gameCoins() ? "Game Coins:" : "Event Coins:", HEAD, pack);
        if (s.gameCoins()) coinHead = coinHead.append(hud(" (" + com.mcm.api.McmCoins.formatMultiplier(s.multiplier()) + ")", NamedTextColor.YELLOW, pack));
        add(left, right, coinHead, null);
        int coinFrom = left.size();
        List<TextColor> dotColor = new ArrayList<>();
        coinRows(p, s, pack, left, right, dotColor);
        int coinTo = left.size();
        add(left, right, Component.empty(), null);
        add(left, right, hud("Your Coins: ", HEAD, pack).append(hud(String.valueOf(scoring.getPlayerPoints(p.getUniqueId())), VALUE, pack))
                .append(Component.space()).append(icon(COIN, pack, "⛃", NamedTextColor.GOLD)), null);

        // The sidebar is as wide as its widest line (name + ": " + score); dot leaders run each coin row out to it.
        final int spacer = 6, minDots = 2 * 2;
        int wide = measure(title);
        for (int i = 0; i < left.size(); i++) {
            boolean coin = i >= coinFrom && i < coinTo && right.get(i) != null;
            int w = measure(left.get(i)) + (coin ? minDots : 0) + (right.get(i) == null ? 0 : spacer + measure(right.get(i)));
            wide = Math.max(wide, w);
        }
        wide = Math.max(wide, 118);
        for (int i = coinFrom, k = 0; i < coinTo; i++) {
            if (right.get(i) == null) continue;
            int room = wide - spacer - measure(right.get(i)) - measure(left.get(i));
            left.set(i, left.get(i).append(Component.text(".".repeat(Math.max(2, room / 2)), dotColor.get(k++))));
        }

        int n = Math.min(ENTRIES.length, left.size());
        for (int i = 0; i < ENTRIES.length; i++) {
            if (i >= n) { sb.resetScores(ENTRIES[i]); continue; }
            Score sc = obj.getScore(ENTRIES[i]);
            sc.setScore(n - i);
            sc.customName(left.get(i));
            sc.numberFormat(right.get(i) == null ? NumberFormat.blank() : NumberFormat.fixed(right.get(i)));
        }
        if (p.getScoreboard() != sb) p.setScoreboard(sb);
    }

    private static void add(List<Component> l, List<Component> r, Component left, Component right) {
        l.add(left);
        r.add(right);
    }

    private static Component labelled(String label, String value, TextColor labelColor, boolean pack) {
        return hud(label + ": ", labelColor, pack).append(hud(value, VALUE, pack));
    }

    private static Component rounds(int round, int rounds, boolean pack) {
        return labelled("Round", Math.max(1, round) + "/" + rounds, NamedTextColor.GREEN, pack);
    }

    private static final Pattern ROUND = Pattern.compile("(?i)^\\s*rounds?\\s*:?\\s*(\\d+)\\s*/\\s*(\\d+)\\s*$");
    private static final Pattern SEG = Pattern.compile("§([0-9a-fk-or])");

    /** Turns a game's "§7Label §fvalue" line into "LABEL: VALUE" in the MCC colours; null drops it. */
    private Component gameLine(String legacy, State s, boolean pack) {
        if (legacy == null) return null;
        String plain = ChatColor.stripColor(legacy).trim();
        if (plain.isEmpty()) return null;
        if (plain.equalsIgnoreCase(s.game())) return null;
        Matcher r = ROUND.matcher(plain);
        if (r.matches()) return rounds(Integer.parseInt(r.group(1)), Integer.parseInt(r.group(2)), pack);
        if (plain.contains("|")) return segments(legacy, pack);
        String label = null, value;
        int colon = plain.indexOf(':');
        if (colon > 0 && colon < plain.length() - 1) {
            label = plain.substring(0, colon).trim();
            value = plain.substring(colon + 1).trim();
        } else {
            List<String> parts = new ArrayList<>();
            for (String part : SEG.split(legacy)) if (!part.isBlank()) parts.add(part.trim());
            if (parts.size() >= 2) {
                label = parts.get(0);
                value = String.join(" ", parts.subList(1, parts.size())).replace(" / ", "/").replace("/ ", "/");
            } else {
                value = plain;
            }
        }
        if (label != null) {
            String l = label.toUpperCase(Locale.ROOT);
            if (l.equals("MAP") || l.equals("TIME") || l.equals("TIME LEFT")) return null;
            boolean people = l.contains("PLAYER") || l.contains("ALIVE") || l.contains("REMAINING") || l.contains("FINISHED")
                    || l.contains("RACING");
            return labelled(label, value, people ? NamedTextColor.GREEN : LABEL, pack);
        }
        return hud(value, color(firstColor(legacy)), pack);
    }

    /** Column-style lines ("Items | Lines", "Name 3 | 1") keep the game's own colours, in the HUD font. */
    private static Component segments(String legacy, boolean pack) {
        Component out = Component.empty();
        ChatColor cur = ChatColor.WHITE;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < legacy.length(); i++) {
            char ch = legacy.charAt(i);
            if (ch == '§' && i + 1 < legacy.length()) {
                if (sb.length() > 0) { out = out.append(hud(sb.toString(), color(cur), pack)); sb.setLength(0); }
                ChatColor cc = ChatColor.getByChar(legacy.charAt(++i));
                if (cc != null && cc.isColor()) cur = cc;
            } else sb.append(ch);
        }
        if (sb.length() > 0) out = out.append(hud(sb.toString(), color(cur), pack));
        return out;
    }

    private static ChatColor firstColor(String legacy) {
        Matcher m = SEG.matcher(legacy);
        while (m.find()) {
            ChatColor c = ChatColor.getByChar(m.group(1).charAt(0));
            if (c != null && c.isColor()) return c;
        }
        return ChatColor.WHITE;
    }

    /** Leader, then the viewer's side with its neighbours (all sides when there are five or fewer), MCC style. */
    private void coinRows(Player p, State s, boolean pack, List<Component> left, List<Component> right, List<TextColor> dotColor) {
        List<Map.Entry<String, Integer>> ranked = new ArrayList<>(s.sideCoins().entrySet());
        if (ranked.isEmpty()) {
            add(left, right, Component.text("-", NamedTextColor.GRAY), null);
            return;
        }
        int[] rank = new int[ranked.size()];
        for (int i = 0; i < ranked.size(); i++) {
            rank[i] = i > 0 && ranked.get(i).getValue().equals(ranked.get(i - 1).getValue()) ? rank[i - 1] : i + 1;
        }
        String mine = scoring.getSideKey(p.getUniqueId());
        int me = -1;
        for (int i = 0; i < ranked.size(); i++) if (ranked.get(i).getKey().equals(mine)) me = i;
        List<Integer> show = new ArrayList<>();
        if (ranked.size() <= 5) {
            for (int i = 0; i < ranked.size(); i++) show.add(i);
        } else {
            show.add(0);
            int from = me < 0 ? 1 : Math.max(1, me - 1);
            for (int i = from; i < ranked.size() && show.size() < 4; i++) show.add(i);
        }
        int prev = -1;
        for (int i : show) {
            if (prev >= 0 && i != prev + 1) add(left, right, Component.empty(), null);
            prev = i;
            String key = ranked.get(i).getKey();
            String team = key.startsWith("TEAM:") ? key.substring(5) : null;
            String name = name(key);
            TextColor col = team == null ? NamedTextColor.WHITE : color(scoring.teamColor(team));
            Component l = Component.text(rank[i] + ". ", VALUE).append(teamIcon(team, pack)).append(Component.space())
                    .append(Component.text(name, col));
            dotColor.add(col);
            Component r = Component.text(ranked.get(i).getValue(), VALUE).append(icon(COIN, pack, " ⛃", NamedTextColor.GOLD));
            add(left, right, l, r);
        }
    }

    private String name(String key) {
        if (key.startsWith("TEAM:")) return teamDisplay(key.substring(5));
        if (key.startsWith("SOLO:")) {
            try { return scoring.playerName(UUID.fromString(key.substring(5))); } catch (IllegalArgumentException ignored) { }
        }
        return key;
    }

    // ------------------------------------------------------------------ boss bar, tab, chat

    /** MCC shows only a timer in the boss bar slot; the pack hides the bar itself. Pack players only. */
    void bossBar(Player p, boolean show, String label, int seconds, float progress) {
        if (!show || !McmFx.hasPack(p)) { p.hideBossBar(bar); return; }
        Component name = seconds < 0 ? hud(label, HEAD, true)
                : hud(label + ": ", HEAD, true).append(hud(String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60), VALUE, true));
        bar.name(name);
        bar.progress(Math.max(0f, Math.min(1f, progress)));
        p.showBossBar(bar);
    }

    void tab(Player p, State s) {
        boolean pack = McmFx.hasPack(p);
        Component header = Component.newline().append(hud(s.event(), HEAD, pack)).append(Component.newline());
        header = header.append(s.game() == null ? hud("Lobby", VALUE, pack)
                : hud((s.gameTotal() > 0 ? "Game " + s.gameNo() + "/" + s.gameTotal() + ": " : "Now Playing: "), NamedTextColor.GRAY, pack)
                .append(hud(s.game(), VALUE, pack))).append(Component.newline());
        String t = scoring.getTeam(p.getUniqueId());
        Component footer = Component.newline().append(hud("Team: ", LABEL, pack)).append(teamIcon(t, pack)).append(Component.space())
                .append(Component.text(teamDisplay(t), color(scoring.teamColor(t))))
                .append(hud("   Coins: ", HEAD, pack)).append(Component.text(scoring.getPlayerPoints(p.getUniqueId()), VALUE))
                .append(Component.space()).append(icon(COIN, pack, "⛃", NamedTextColor.GOLD))
                .append(Component.newline()).append(hud("Players Online: " + s.online(), NamedTextColor.GRAY, pack)).append(Component.newline());
        p.sendPlayerListHeaderAndFooter(header, footer);
    }

    /** Tab list name: team icon + name in the team colour (everyone has the pack, it is required). */
    void tabName(Player p) {
        String t = scoring.getTeam(p.getUniqueId());
        p.playerListName(teamIcon(t, true).append(Component.space()).append(Component.text(p.getName(), color(scoring.teamColor(t)))));
    }

    Component chat(Player source, Component message, boolean viewerPack) {
        String t = scoring.getTeam(source.getUniqueId());
        return teamIcon(t, viewerPack).append(Component.space())
                .append(Component.text(source.getName(), color(scoring.teamColor(t))))
                .append(Component.text(": ", NamedTextColor.WHITE)).append(message.colorIfAbsent(NamedTextColor.WHITE));
    }

    void forget(Player p) {
        boards.remove(p.getUniqueId());
        p.hideBossBar(bar);
    }
}
