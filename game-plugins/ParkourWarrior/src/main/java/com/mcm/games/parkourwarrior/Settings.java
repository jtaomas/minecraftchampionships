package com.mcm.games.parkourwarrior;

import com.mcm.games.parkourwarrior.CourseModel.Difficulty;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/** Snapshot of config.yml (read at each game start). */
final class Settings {
    // map
    String generator = "auto";
    long seed = 20261124L;
    int offX, offY, offZ;
    int blocksPerTick = 30000;
    /** Private imported maps: folder (relative to the plugin data folder) with one sub-folder per map. */
    String mapsFolder = "maps";
    /** Maps a game picks from at random (empty = classic + every map found); /pw map forces one. */
    List<String> mapPool = List.of();
    // timing
    int countdown = 10;
    int time = 600;
    int overtime = 30;
    int scrollRemoveAt = 30;      // seconds of normal time left
    int switchLockAt = 30;        // seconds of normal time left
    int bonusReturnDelayTicks = 30;
    int endDelaySeconds = 6;
    int startLeash = 4;
    // daylight
    long dayStart = 0, dayEnd = 12500;
    // players
    boolean translucent = true;
    double hideWithin = 0;
    double hideShowAt = 5;
    int fallMarginBelowCourse = 8;
    // spectators
    int spectatorMargin = 40;
    // timed blocks
    String warnParticle = "DUST";
    // scoring
    // coins (no medals): every challenge pays straight away, base x1.0, the core applies the event multiplier. COINS.md.
    int mainCoins = 4;                      // each main-line challenge, stage 1
    double stageScale = 0.15;               // +15 % per stage above 1 (main challenges and bonus tracks)
    int bonusBronze = 4, bonusSilver = 6, bonusGold = 8;   // bonus track 1 / 2 / 3 of a hub, stage 1
    int endEasy = 10, endMedium = 20, endHard = 40;          // finishing an ending
    /** coins.course.<mapId>: per-checkpoint / finish coins of a linear (replica) course; missing = derived from coins.main. */
    final Map<String, CourseCoins> courseCoins = new java.util.HashMap<>();

    /** checkpoints = value per checkpoint (the last one repeats), checkpoint = the same value for all (-1 unset), finish = course completion (-1 unset). */
    static final class CourseCoins {
        List<Integer> checkpoints = List.of();
        int checkpoint = -1, finish = -1;
    }

    static Settings load(FileConfiguration c, Logger log) {
        Settings s = new Settings();
        s.generator = c.getString("map.generator", s.generator).toLowerCase(java.util.Locale.ROOT);
        s.seed = c.getLong("map.seed", s.seed);
        s.offX = c.getInt("map.offset-x", 0);
        s.offY = c.getInt("map.offset-y", 0);
        s.offZ = c.getInt("map.offset-z", 0);
        s.blocksPerTick = Math.max(1000, c.getInt("map.blocks-per-tick", s.blocksPerTick));
        s.mapsFolder = c.getString("map.maps-folder", s.mapsFolder);
        List<String> pool = new java.util.ArrayList<>();
        for (String id : c.getStringList("map.pool")) pool.add(id.trim().toLowerCase(java.util.Locale.ROOT));
        s.mapPool = pool;
        s.countdown = Math.max(0, c.getInt("game.countdown-seconds", s.countdown));
        s.time = Math.max(30, c.getInt("game.time-seconds", s.time));
        s.overtime = Math.max(0, c.getInt("game.overtime-seconds", s.overtime));
        s.scrollRemoveAt = Math.max(0, c.getInt("game.scroll-removed-at-seconds-left", s.scrollRemoveAt));
        s.switchLockAt = Math.max(0, c.getInt("game.ending-switch-until-seconds-left", s.switchLockAt));
        s.bonusReturnDelayTicks = Math.max(0, c.getInt("game.bonus-return-delay-ticks", s.bonusReturnDelayTicks));
        s.endDelaySeconds = Math.max(0, c.getInt("game.end-delay-seconds", s.endDelaySeconds));
        s.startLeash = Math.max(1, c.getInt("game.start-leash-blocks", s.startLeash));
        s.dayStart = c.getLong("daylight.start-time", s.dayStart);
        s.dayEnd = c.getLong("daylight.end-time", s.dayEnd);
        // ghost-players (set by /pw ghost) wins; players.translucent is the older name of the same switch
        s.translucent = c.getBoolean("ghost-players", c.getBoolean("players.translucent", s.translucent));
        s.hideWithin = Math.max(0, c.getDouble("players.hide-within", s.hideWithin));
        s.hideShowAt = Math.max(s.hideWithin + 1, c.getDouble("players.show-again-at", s.hideShowAt));
        s.fallMarginBelowCourse = Math.max(1, c.getInt("players.fall-margin-below-course", s.fallMarginBelowCourse));
        s.spectatorMargin = Math.max(5, c.getInt("spectators.bounds-margin", s.spectatorMargin));
        s.warnParticle = c.getString("timed.warn-particle", s.warnParticle);
        s.mainCoins = Math.max(0, c.getInt("coins.main", s.mainCoins));
        s.stageScale = Math.max(0, c.getDouble("coins.stage-scale", s.stageScale));
        s.bonusBronze = Math.max(0, c.getInt("coins.bonus.bronze", s.bonusBronze));
        s.bonusSilver = Math.max(0, c.getInt("coins.bonus.silver", s.bonusSilver));
        s.bonusGold = Math.max(0, c.getInt("coins.bonus.gold", s.bonusGold));
        s.endEasy = Math.max(0, c.getInt("coins.ending.easy", s.endEasy));
        s.endMedium = Math.max(0, c.getInt("coins.ending.medium", s.endMedium));
        s.endHard = Math.max(0, c.getInt("coins.ending.hard", s.endHard));
        org.bukkit.configuration.ConfigurationSection cc = c.getConfigurationSection("coins.course");
        if (cc != null) {
            for (String id : cc.getKeys(false)) {
                org.bukkit.configuration.ConfigurationSection sec = cc.getConfigurationSection(id);
                if (sec == null) continue;
                CourseCoins k = new CourseCoins();
                List<Integer> raw = sec.getIntegerList("checkpoints");
                List<Integer> list = new java.util.ArrayList<>();
                for (int v : raw) list.add(Math.max(0, v));
                k.checkpoints = list;
                k.checkpoint = sec.contains("checkpoint") ? Math.max(0, sec.getInt("checkpoint")) : -1;
                k.finish = sec.contains("finish") ? Math.max(0, sec.getInt("finish")) : -1;
                s.courseCoins.put(id.toLowerCase(java.util.Locale.ROOT), k);
            }
        }
        return s;
    }

    private static int[] table(FileConfiguration c, String path, int[] def, Logger log) {
        List<Integer> l = c.getIntegerList(path);
        if (l == null || l.isEmpty()) return def;
        int[] out = new int[l.size()];
        for (int i = 0; i < out.length; i++) out[i] = l.get(i);
        for (int i = 1; i < out.length; i++) if (out[i] < out[i - 1]) log.warning("Parkour Warrior: " + path + " is not increasing");
        return out;
    }
}
