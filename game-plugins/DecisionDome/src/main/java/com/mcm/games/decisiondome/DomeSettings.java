package com.mcm.games.decisiondome;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EnumMap;
import java.util.Map;

/** Loaded dome configuration. All durations are seconds in config and ticks to the engine. */
public final class DomeSettings {
    public int dialCapacity = 8;
    public int maxGames = 15;
    public int podCount = 10;
    public int refillVote = 6;

    public double openingSeconds = 5.0;
    public double votingSeconds = 30.0;
    public double settleSeconds = 13.0;
    public double dividersSeconds = 1.5;
    public double tallySeconds = 1.0;
    public double revealSeconds = 5.0;
    public double removeSeconds = 2.0;
    public double refillSeconds = 2.0;
    public double teleportSeconds = 8.0;

    public boolean powerUps = true;
    public boolean excludeFirstVote = true;
    public boolean excludeAudience = true;
    public boolean phaseTwoPowerUps = false;
    public int maxPowerUpsPerTeam = 2;
    public boolean debug = false;
    public boolean allowCrossWedge = true;
    public int maxChickens = 200;

    public static DomeSettings load(JavaPlugin plugin) {
        DomeSettings s = new DomeSettings();
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        s.dialCapacity = clamp(c.getInt("dial.capacity", 8), 6, 8);
        s.maxGames = clamp(c.getInt("dial.max-games", 15), 1, Dial.GAME_CEILING);
        s.podCount = clamp(c.getInt("dial.pods", 10), 1, 10);
        s.refillVote = clamp(c.getInt("dial.refill-vote", 6), 2, 15);

        s.openingSeconds = seconds(c, "phases.opening-seconds", 5.0);
        s.votingSeconds = seconds(c, "phases.voting-seconds", 30.0);
        s.settleSeconds = seconds(c, "phases.settle-seconds", 13.0);
        s.dividersSeconds = seconds(c, "phases.dividers-seconds", 1.5);
        s.tallySeconds = seconds(c, "phases.tally-seconds", 1.0);
        s.revealSeconds = seconds(c, "phases.reveal-seconds", 5.0);
        s.removeSeconds = seconds(c, "phases.remove-seconds", 2.0);
        s.refillSeconds = seconds(c, "phases.refill-seconds", 2.0);
        s.teleportSeconds = seconds(c, "phases.teleport-seconds", 8.0);

        s.powerUps = c.getBoolean("powerups.enabled", true);
        s.excludeFirstVote = c.getBoolean("powerups.exclude-first-vote", true);
        s.excludeAudience = c.getBoolean("powerups.exclude-audience", true);
        s.phaseTwoPowerUps = c.getBoolean("powerups.phase-2", false);
        s.maxPowerUpsPerTeam = clamp(c.getInt("powerups.max-per-team", 2), 0, 4);
        s.debug = c.getBoolean("debug", false);
        s.allowCrossWedge = c.getBoolean("chickens.allow-cross-wedge", true);
        s.maxChickens = clamp(c.getInt("chickens.max", 200), 20, 400);
        return s;
    }

    private static double seconds(FileConfiguration c, String key, double def) {
        double v = c.getDouble(key, def);
        if (Double.isNaN(v) || v <= 0) v = def;
        return Math.min(120.0, v);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    /** Engine tick durations; fast-test clamping is applied by the engine itself. */
    public Map<Phase, Integer> phaseTicks() {
        Map<Phase, Integer> map = new EnumMap<>(Phase.class);
        map.put(Phase.OPENING, ticks(openingSeconds));
        map.put(Phase.VOTING, ticks(votingSeconds));
        map.put(Phase.SETTLE, ticks(settleSeconds));
        map.put(Phase.DIVIDERS, ticks(dividersSeconds));
        map.put(Phase.TALLY, ticks(tallySeconds));
        map.put(Phase.REVEAL, ticks(revealSeconds));
        map.put(Phase.REMOVE, ticks(removeSeconds));
        map.put(Phase.REFILL, ticks(refillSeconds));
        map.put(Phase.TELEPORT, ticks(teleportSeconds));
        return map;
    }

    public static int ticks(double seconds) {
        return Math.max(1, (int) Math.round(seconds * 20.0));
    }

    public boolean powerUpsFor(int eventIndex, boolean audienceVote) {
        if (!powerUps) return false;
        if (excludeFirstVote && eventIndex <= 1) return false;
        if (excludeAudience && audienceVote) return false;
        return true;
    }
}
