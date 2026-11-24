package com.mcm;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Owner request: two feedback commands that write structured, AI-readable lines during playtests.
 *
 * <ul>
 *   <li>{@code /pfb <message>} — point feedback: game, map, world, exact player coordinates, yaw/pitch and message.</li>
 *   <li>{@code /gfb <message>} — general feedback: game and map only (no coordinates), for balance/whole-game notes.</li>
 * </ul>
 *
 * One JSON object per line in {@code plugins/MinecraftMondayCore/feedback/feedback.jsonl}, UTF-8:
 * <pre>
 * {"v":1,"time":"2026-11-24T12:34:56Z","kind":"point","player":"Name","uuid":"...",
 *  "event":"MCC","championship":true,"game":"battlebox","gameName":"Battle Box",
 *  "map":"greenhouse","mapName":"greenhouse","world":"mcc_battlebox",
 *  "x":12.34,"y":65.0,"z":-8.9,"yaw":90.0,"pitch":12.3,"message":"..."}
 * </pre>
 * General feedback omits {@code world} and the coordinate fields.
 */
final class FeedbackLog {
    private final MinecraftMondayPlugin plugin;
    private final File dir;
    private final File file;

    FeedbackLog(MinecraftMondayPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "feedback");
        this.file = new File(dir, "feedback.jsonl");
    }

    /** Handles /pfb (point) and /gfb (general). Always returns true so the command is consumed. */
    boolean handle(CommandSender sender, String command, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage(ChatColor.RED + "/" + command + " can only be used by a player in game.");
            return true;
        }
        String message = String.join(" ", args).trim();
        if (message.isEmpty()) {
            p.sendMessage(ChatColor.YELLOW + "Usage: /" + command + " <message>");
            return true;
        }

        boolean point = command.equalsIgnoreCase("pfb");
        String gameId = plugin.feedbackGameId();
        String gameName = plugin.feedbackGameName();
        String mapId = plugin.feedbackMapId();
        String mapName = plugin.feedbackMapName();
        String event = plugin.feedbackEvent();
        boolean championship = plugin.feedbackChampionship();
        Location loc = p.getLocation().clone();

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("v", 1);
        row.put("time", Instant.now().toString());
        row.put("kind", point ? "point" : "general");
        row.put("player", p.getName());
        row.put("uuid", p.getUniqueId().toString());
        row.put("event", event);
        row.put("championship", championship);
        row.put("game", gameId);
        row.put("gameName", gameName);
        row.put("map", mapId);
        row.put("mapName", mapName);
        if (point) {
            row.put("world", loc.getWorld() == null ? "unknown" : loc.getWorld().getName());
            row.put("x", round(loc.getX(), 2));
            row.put("y", round(loc.getY(), 2));
            row.put("z", round(loc.getZ(), 2));
            row.put("yaw", round(loc.getYaw(), 1));
            row.put("pitch", round(loc.getPitch(), 1));
        }
        row.put("message", message);

        String line = json(row);
        p.sendMessage(ChatColor.GREEN + "Feedback saved " + ChatColor.GRAY + "("
                + (point ? "point + coordinates" : "general") + ")");
        plugin.getLogger().info("Feedback " + (point ? "pfb" : "gfb") + " from " + p.getName()
                + " [" + event + " " + gameId + "/" + mapId + "]: " + message);

        // Keep the main thread free of file I/O. The write is serialized so two players cannot interleave a line.
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                write(line);
            } catch (Throwable t) {
                plugin.getLogger().warning("feedback write failed: " + t);
            }
        });
        return true;
    }

    private synchronized void write(String line) throws Exception {
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        try (Writer w = new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.UTF_8)) {
            w.write(line);
            w.write(System.lineSeparator());
        }
    }

    private static double round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }

    private static String json(Map<String, Object> row) {
        StringBuilder b = new StringBuilder(256);
        b.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : row.entrySet()) {
            if (!first) b.append(',');
            first = false;
            b.append(quote(e.getKey())).append(':');
            Object v = e.getValue();
            if (v == null) {
                b.append("null");
            } else if (v instanceof Number || v instanceof Boolean) {
                b.append(v);
            } else {
                b.append(quote(String.valueOf(v)));
            }
        }
        b.append('}');
        return b.toString();
    }

    private static String quote(String s) {
        StringBuilder b = new StringBuilder(s.length() + 2);
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                case '\b' -> b.append("\\b");
                case '\f' -> b.append("\\f");
                default -> {
                    if (c < 0x20) b.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                    else b.append(c);
                }
            }
        }
        b.append('"');
        return b.toString();
    }
}
