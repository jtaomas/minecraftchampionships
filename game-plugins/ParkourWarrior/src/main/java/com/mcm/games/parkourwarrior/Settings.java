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
    // MCC coins, Parkour Warrior Dojo (current PW; base x1.0, the core applies the event multiplier). COINS.md.
    int stone = 2;                          // MCC: per stone medal (main challenge)
    int[] bronze = {5, 15, 25, 40, 60};     // MCC: total for holding 1..5 bronze medals
    int[] silver = {10, 25, 45, 70, 100};   // MCC: total for holding 1..5 silver medals
    int[] gold = {15, 30, 55, 90, 140};     // MCC: total for holding 1..5 gold medals
    final Map<Difficulty, Double> multInc = new EnumMap<>(Difficulty.class);
    final Map<Difficulty, Double> weight = new EnumMap<>(Difficulty.class);
    double weightNone = 0.40;

    static Settings load(FileConfiguration c, Logger log) {
        Settings s = new Settings();
        s.generator = c.getString("map.generator", s.generator).toLowerCase(java.util.Locale.ROOT);
        s.seed = c.getLong("map.seed", s.seed);
        s.offX = c.getInt("map.offset-x", 0);
        s.offY = c.getInt("map.offset-y", 0);
        s.offZ = c.getInt("map.offset-z", 0);
        s.blocksPerTick = Math.max(1000, c.getInt("map.blocks-per-tick", s.blocksPerTick));
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
        s.stone = c.getInt("scoring.stone", s.stone);
        s.bronze = table(c, "scoring.bronze", s.bronze, log);
        s.silver = table(c, "scoring.silver", s.silver, log);
        s.gold = table(c, "scoring.gold", s.gold, log);
        s.multInc.put(Difficulty.EASY, c.getDouble("scoring.multiplier.easy", 0.15));
        s.multInc.put(Difficulty.MEDIUM, c.getDouble("scoring.multiplier.medium", 0.35));
        s.multInc.put(Difficulty.HARD, c.getDouble("scoring.multiplier.hard", 0.80));
        s.weightNone = c.getDouble("scoring.share-weight.none", 0.40);
        s.weight.put(Difficulty.EASY, c.getDouble("scoring.share-weight.easy", 0.45));
        s.weight.put(Difficulty.MEDIUM, c.getDouble("scoring.share-weight.medium", 0.65));
        s.weight.put(Difficulty.HARD, c.getDouble("scoring.share-weight.hard", 1.10));
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
