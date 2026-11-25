package com.mcm.games.decisiondome;

import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Locale;

/** Tiny append-only JSONL vote log. Never throws into the vote path. */
final class DomeLog {
    private final Plugin plugin;
    private final Path file;

    DomeLog(Plugin plugin, String sessionId) {
        this.plugin = plugin;
        Path dir = plugin.getDataFolder().toPath().resolve("logs");
        this.file = dir.resolve("vote-" + sessionId + ".jsonl");
        try { Files.createDirectories(dir); } catch (IOException ignored) { }
    }

    void event(String type, String detail) {
        String line = "{\"t\":\"" + Instant.now() + "\",\"type\":\"" + esc(type)
                + "\",\"detail\":\"" + esc(detail) + "\"}\n";
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            plugin.getLogger().fine("Dome log write skipped: " + e.getMessage());
        }
    }

    void phase(Phase phase, int tick) { event("phase", phase.name().toLowerCase(Locale.ROOT) + "@" + tick); }

    void tally(VoteTally tally) { if (tally != null) event("tally", tally.debug()); }

    void throwEgg(Egg egg) {
        if (egg == null) return;
        event("throw", egg.kind().name() + " team=" + egg.team() + " value=" + egg.value());
    }

    void hatch(Chicken chicken) {
        if (chicken == null) return;
        event("hatch", chicken.kind().name() + " team=" + chicken.team()
                + " x=" + String.format(Locale.ROOT, "%.2f", chicken.x())
                + " z=" + String.format(Locale.ROOT, "%.2f", chicken.z()));
    }

    void winner(String game, VoteTally tally) {
        event("winner", game + " " + (tally == null ? "" : tally.debug()));
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }
}
