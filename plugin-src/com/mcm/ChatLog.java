package com.mcm;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Owner request: record every chat message with the game mode and map it was typed in, so playtest
 * feedback typed in chat can be read back later. One plain-text file per day in
 * {@code plugins/MinecraftMondayCore/chat-feedback/chat-YYYY-MM-DD.log}; one line per message:
 * {@code [HH:mm:ss] game=<id> map=<map> world=<world> <player>: <message>}.
 */
final class ChatLog implements Listener {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final MinecraftMondayPlugin plugin;
    private final File dir;

    ChatLog(MinecraftMondayPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "chat-feedback");
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChat(AsyncChatEvent e) {
        try {
            String msg = PlainTextComponentSerializer.plainText().serialize(e.message()).replace('\n', ' ').replace('\r', ' ');
            String[] ctx = plugin.chatLogContext();
            String world = e.getPlayer().getWorld().getName();
            String line = "[" + LocalTime.now().format(TIME) + "] game=" + ctx[0]
                    + " map=" + (ctx[1] == null ? "-" : ctx[1]) + " world=" + world
                    + " " + e.getPlayer().getName() + ": " + msg + System.lineSeparator();
            write(line);
        } catch (Throwable t) {
            plugin.getLogger().warning("chat feedback log failed: " + t);
        }
    }

    private synchronized void write(String line) throws Exception {
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        File f = new File(dir, "chat-" + LocalDate.now() + ".log");
        try (Writer w = new OutputStreamWriter(new FileOutputStream(f, true), StandardCharsets.UTF_8)) {
            w.write(line);
        }
    }
}
