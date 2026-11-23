package com.mcm.games.gridrunners;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public final class Msg {
    private Msg() {}

    public static Component c(String legacy) {
        return LegacyComponentSerializer.legacySection().deserialize(legacy == null ? "" : legacy);
    }

    public static void send(Player p, String s) { p.sendMessage(c(s)); }

    public static void bar(Player p, String s) { p.sendActionBar(c(s)); }

    public static void title(Player p, String title, String sub, int in, int stay, int out) {
        p.showTitle(Title.title(c(title), c(sub), Title.Times.times(
                Duration.ofMillis(in * 50L), Duration.ofMillis(stay * 50L), Duration.ofMillis(out * 50L))));
    }

    public static String ordinal(int n) {
        int m = n % 100;
        if (m >= 11 && m <= 13) return n + "th";
        return switch (n % 10) {
            case 1 -> n + "st";
            case 2 -> n + "nd";
            case 3 -> n + "rd";
            default -> n + "th";
        };
    }

    public static String time(int seconds) {
        return (seconds / 60) + ":" + String.format("%02d", seconds % 60);
    }
}
