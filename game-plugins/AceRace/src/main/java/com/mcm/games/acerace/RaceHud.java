package com.mcm.games.acerace;

import com.mcm.api.McmFx;
import com.mcm.api.McmGameContext;
import com.mcm.games.acerace.map.RaceMapData;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.List;

/** Sidebar, timer, action bar and checkpoint/finish feedback. */
public final class RaceHud {

    public void board(McmGameContext ctx, RaceMapData data, int lap, int laps,
                      int cp, int cps, int alive, int total, int timeLeft, int totalTime) {
        if (ctx == null) return;
        ctx.setTimer(Math.max(0, timeLeft), Math.max(1, totalTime));
        ctx.setSidebarLines(List.of(
                ChatColor.GOLD + "" + ChatColor.BOLD + "ACE RACE",
                ChatColor.GRAY + "Map " + ChatColor.WHITE + data.mapId,
                ChatColor.GRAY + "Lap " + ChatColor.YELLOW + lap + "/" + laps,
                ChatColor.GRAY + "Checkpoint " + ChatColor.YELLOW + cp + "/" + cps,
                ChatColor.GRAY + "Players " + ChatColor.YELLOW + alive + "/" + total,
                ChatColor.GRAY + "Time " + ChatColor.YELLOW + format(timeLeft)
        ));
    }

    public void action(Player p, int lap, int laps, int cp, int cps, int place) {
        if (p == null || !p.isOnline()) return;
        McmFx.actionBar(p, ChatColor.YELLOW + "Lap " + lap + "/" + laps
                + ChatColor.GRAY + "  |  " + ChatColor.WHITE + "CP " + cp + "/" + cps
                + ChatColor.GRAY + "  |  " + ChatColor.WHITE + place + "th");
    }

    public void checkpoint(Player p, int cp, int total, double splitSeconds, boolean show) {
        if (p == null || !p.isOnline() || !show) return;
        p.sendMessage(ChatColor.GOLD + "Checkpoint " + ChatColor.WHITE + cp + "/" + total
                + ChatColor.GRAY + "  -  " + ChatColor.YELLOW + String.format(java.util.Locale.ROOT, "%.2fs", splitSeconds));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.4f);
    }

    public void finish(Player p, int place, long millis) {
        if (p == null || !p.isOnline()) return;
        p.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You finished "
                + ChatColor.WHITE + place + getPlaceSuffix(place) + ChatColor.GOLD + " place!"
                + ChatColor.GRAY + "  (" + formatMillis(millis) + ")");
        McmFx.title(p, ChatColor.GOLD + "" + ChatColor.BOLD + "FINISHED",
                ChatColor.YELLOW + "" + place + getPlaceSuffix(place) + " place", 5, 50, 10);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
    }

    public static String format(int seconds) {
        int s = Math.max(0, seconds);
        return String.format(java.util.Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    public static String formatMillis(long millis) {
        long total = Math.max(0, millis);
        long minutes = total / 60_000L;
        long seconds = (total / 1000L) % 60L;
        long hundredths = (total / 10L) % 100L;
        return String.format(java.util.Locale.ROOT, "%d:%02d.%02d", minutes, seconds, hundredths);
    }

    private static String getPlaceSuffix(int place) {
        if (place % 100 >= 11 && place % 100 <= 13) return "th";
        return switch (place % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }
}
