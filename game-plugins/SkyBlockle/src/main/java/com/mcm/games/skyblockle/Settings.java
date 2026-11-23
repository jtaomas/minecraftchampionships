package com.mcm.games.skyblockle;

import org.bukkit.Difficulty;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/** Snapshot of config.yml taken when a game starts. PENDING_RESEARCH items are all here as data. */
final class Settings {
    int gameSeconds, countdownSeconds, graceSeconds, endDelaySeconds;
    int killPoints, surviveDeathPoints, aliveAtEndPoints, teamKillPoints;
    int killCreditSeconds;
    List<String> tiebreak = new ArrayList<>();
    boolean dropItems, voidDropItems, dropXp;
    int voidDepth;
    List<ItemSpec> startInventory = new ArrayList<>();
    int startXpLevels;
    boolean borderEnabled;
    List<Object[]> borderPoints = new ArrayList<>();  // {time(Double), token(Object)}
    List<double[]> borderDamage = new ArrayList<>();
    double warnDistance;
    int lastStandAt, lastStandSeconds, particlesDistance;
    boolean grave;
    List<Integer> announceRemaining = new ArrayList<>();
    String generator;
    long seed;
    int baseY, minWedges, maxWedges, buildAbove, spectatorMargin, clearMsPerTick, blocksPerTick;
    boolean fluidPhysics;
    boolean randomSlots;
    List<Integer> refillAt = new ArrayList<>();
    Difficulty difficulty;
    boolean naturalRegen, fallDamage, blockPortals, blockEnderChests;

    static Settings load(FileConfiguration c, Logger log) {
        Settings s = new Settings();
        s.gameSeconds = Math.max(30, c.getInt("game-seconds", 1080));
        s.countdownSeconds = Math.max(0, c.getInt("countdown-seconds", 10));
        s.graceSeconds = Math.max(0, c.getInt("grace-seconds", 0));
        s.endDelaySeconds = Math.max(1, c.getInt("end-delay-seconds", 8));
        s.killPoints = c.getInt("points.kill", 14);
        s.surviveDeathPoints = c.getInt("points.survive-death", 2);
        s.aliveAtEndPoints = c.getInt("points.alive-at-end", 30);
        s.teamKillPoints = c.getInt("points.team-kill", 0);
        s.killCreditSeconds = Math.max(0, c.getInt("kill-credit-seconds", 10));
        for (String t : c.getStringList("timeout-tiebreak")) s.tiebreak.add(t.toLowerCase(Locale.ROOT).trim());
        if (s.tiebreak.isEmpty()) s.tiebreak.addAll(List.of("players-alive", "kills"));
        s.dropItems = c.getBoolean("death.drop-items", true);
        s.voidDropItems = c.getBoolean("death.void-drop-items", false);
        s.dropXp = c.getBoolean("death.drop-xp", true);
        s.voidDepth = Math.max(2, c.getInt("death.void-depth", 12));
        s.startInventory = ItemSpec.parseList(c.getList("start-inventory"), log, "config start-inventory");
        s.startXpLevels = Math.max(0, c.getInt("start-xp-levels", 0));

        s.borderEnabled = c.getBoolean("border.enabled", true);
        List<?> pts = c.getList("border.points");
        if (pts != null) for (Object o : pts) {
            if (o instanceof List<?> l && l.size() >= 2) {
                try { s.borderPoints.add(new Object[]{Double.parseDouble(String.valueOf(l.get(0))), l.get(1)}); }
                catch (NumberFormatException e) { log.warning("Sky Blockle: bad border point " + o); }
            } else log.warning("Sky Blockle: bad border point " + o + " (expected [seconds, radius])");
        }
        if (s.borderPoints.isEmpty()) s.borderPoints.add(new Object[]{0.0, "map"});
        List<?> dmg = c.getList("border.damage");
        if (dmg != null) for (Object o : dmg) {
            if (o instanceof List<?> l && l.size() >= 2) {
                try { s.borderDamage.add(new double[]{Double.parseDouble(String.valueOf(l.get(0))), Double.parseDouble(String.valueOf(l.get(1)))}); }
                catch (NumberFormatException e) { log.warning("Sky Blockle: bad border damage point " + o); }
            }
        }
        if (s.borderDamage.isEmpty()) s.borderDamage.add(new double[]{0, 1});
        s.warnDistance = c.getDouble("border.warn-distance", 10);
        s.lastStandAt = c.getInt("border.last-stand-at", 990);
        s.lastStandSeconds = Math.max(0, c.getInt("border.last-stand-seconds", 30));
        s.particlesDistance = Math.max(0, c.getInt("border.particles-distance", 24));
        s.grave = c.getBoolean("death.grave", true);
        s.announceRemaining = c.getIntegerList("announce-remaining");

        s.generator = c.getString("map.generator", "auto").toLowerCase(Locale.ROOT).trim();
        s.seed = c.getLong("map.seed", 0);
        s.baseY = c.getInt("map.base-y", 100);
        s.minWedges = Math.max(1, Math.min(10, c.getInt("map.min-wedges", 2)));
        s.maxWedges = Math.max(s.minWedges, Math.min(10, c.getInt("map.max-wedges", 10)));
        s.buildAbove = Math.max(0, c.getInt("map.build-height-above-map", 24));
        s.spectatorMargin = Math.max(4, c.getInt("map.spectator-margin", 24));
        s.fluidPhysics = c.getBoolean("map.fluid-physics", true);
        s.clearMsPerTick = Math.max(2, Math.min(40, c.getInt("map.clear-ms-per-tick", 20)));
        s.blocksPerTick = Math.max(1000, c.getInt("map.blocks-per-tick", 40000));

        s.randomSlots = c.getBoolean("loot.random-slots", true);
        for (Object o : c.getList("loot.refill-at", List.of())) {
            try { s.refillAt.add((int) Double.parseDouble(String.valueOf(o))); } catch (NumberFormatException ignored) {}
        }

        Difficulty d;
        try { d = Difficulty.valueOf(c.getString("world.difficulty", "normal").toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { d = Difficulty.NORMAL; }
        s.difficulty = d;
        s.naturalRegen = c.getBoolean("world.natural-regeneration", true);
        s.fallDamage = c.getBoolean("world.fall-damage", true);
        s.blockPortals = c.getBoolean("world.block-portals", true);
        s.blockEnderChests = c.getBoolean("world.block-ender-chests", true);
        return s;
    }

    /** Resolve the border schedule for a generated map. */
    BorderSchedule border(MapModel m, Logger log) {
        List<double[]> r = new ArrayList<>();
        for (Object[] p : borderPoints) {
            try { r.add(new double[]{(Double) p[0], BorderSchedule.resolve(p[1], m)}); }
            catch (RuntimeException e) { log.warning("Sky Blockle: unknown border radius '" + p[1] + "'"); }
        }
        return new BorderSchedule(r, borderDamage);
    }
}
