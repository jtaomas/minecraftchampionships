package com.mcm.games.tgttos;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/** Snapshot of config.yml (re-read at every game start). */
final class Settings {
    int rounds, roundSeconds, preRoundSeconds, intermissionSeconds;
    boolean timeSkip; double timeSkipFraction; int timeSkipSeconds;
    int baseY, spacing;
    int concrete, snowballs, fireworks; Material pickaxe;
    double kbPunchH, kbPunchV, kbSprint, kbSnowH, kbSnowV; int hitCooldownTicks;
    double kPerPunch, kSameBonus, kDecay, kMoreFrom, kPerKarma, kMaxMult, kGlowFrom, kLockFrom;
    int kSameSeconds, kLockSeconds; boolean kSlowness;
    double protPerHit, protMax, protDecay;
    int ptsFirst, ptsStepEvery, ptsMin; List<Integer> top10 = new ArrayList<>(), teamBonus = new ArrayList<>();
    int chickenCount; boolean chickenWander;
    boolean randomSeeds;
    List<String> punsVoid = new ArrayList<>(), punsHazard = new ArrayList<>();

    static Settings load(FileConfiguration c) {
        Settings s = new Settings();
        s.rounds = Math.max(1, c.getInt("rounds", 6));
        s.roundSeconds = Math.max(10, c.getInt("round-seconds", 100));
        s.preRoundSeconds = Math.max(4, c.getInt("pre-round-seconds", 15));
        s.intermissionSeconds = Math.max(2, c.getInt("intermission-seconds", 6));
        s.timeSkip = c.getBoolean("time-skip.enabled", true);
        s.timeSkipFraction = c.getDouble("time-skip.finished-fraction", 0.8);
        s.timeSkipSeconds = c.getInt("time-skip.seconds-left", 20);
        s.baseY = c.getInt("world.base-y", 64);
        s.spacing = Math.max(520, c.getInt("world.map-spacing", 600));
        s.concrete = c.getInt("kit.concrete", 64);
        s.snowballs = c.getInt("kit.snowballs", 5);
        s.fireworks = c.getInt("kit.fireworks", 2);
        Material pk = Material.matchMaterial(c.getString("kit.pickaxe", "IRON_PICKAXE"));
        s.pickaxe = pk == null ? Material.IRON_PICKAXE : pk;
        s.kbPunchH = c.getDouble("knockback.punch-horizontal", 0.42);
        s.kbPunchV = c.getDouble("knockback.punch-vertical", 0.36);
        s.kbSprint = c.getDouble("knockback.sprint-bonus", 0.35);
        s.kbSnowH = c.getDouble("knockback.snowball-horizontal", 0.38);
        s.kbSnowV = c.getDouble("knockback.snowball-vertical", 0.28);
        s.hitCooldownTicks = c.getInt("knockback.hit-cooldown-ticks", 10);
        s.kPerPunch = c.getDouble("karma.per-punch", 1.0);
        s.kSameBonus = c.getDouble("karma.same-victim-bonus", 0.5);
        s.kSameSeconds = c.getInt("karma.same-victim-seconds", 5);
        s.kDecay = c.getDouble("karma.decay-per-second", 0.35);
        s.kMoreFrom = c.getDouble("karma.more-knockback-from", 3.0);
        s.kPerKarma = c.getDouble("karma.knockback-per-karma", 0.08);
        s.kMaxMult = c.getDouble("karma.max-knockback-mult", 1.8);
        s.kGlowFrom = c.getDouble("karma.glow-from", 6.0);
        s.kSlowness = c.getBoolean("karma.slowness", true);
        s.kLockFrom = c.getDouble("karma.lock-from", 9.0);
        s.kLockSeconds = c.getInt("karma.lock-seconds", 4);
        s.protPerHit = c.getDouble("karma.hit-protection-per-hit", 0.07);
        s.protMax = c.getDouble("karma.hit-protection-max", 0.45);
        s.protDecay = c.getDouble("karma.hit-protection-decay", 0.5);
        s.ptsFirst = c.getInt("points.placement-first", 8);
        s.ptsStepEvery = Math.max(1, c.getInt("points.placement-step-every", 5));
        s.ptsMin = c.getInt("points.placement-min", 1);
        s.top10 = c.getIntegerList("points.top10-bonus");
        if (s.top10.isEmpty()) s.top10 = List.of(16, 15, 14, 13, 12, 11, 10, 9, 8, 7);
        s.teamBonus = c.getIntegerList("points.team-bonus");
        if (s.teamBonus.isEmpty()) s.teamBonus = List.of(20, 15, 10, 5);
        s.chickenCount = Math.max(1, c.getInt("chickens.count", 10));
        s.chickenWander = c.getBoolean("chickens.wander", true);
        s.randomSeeds = c.getBoolean("random-seeds", false);
        s.punsVoid = c.getStringList("puns-void");
        s.punsHazard = c.getStringList("puns-hazard");
        if (s.punsVoid.isEmpty()) s.punsVoid = Puns.VOID;
        if (s.punsHazard.isEmpty()) s.punsHazard = Puns.HAZARD;
        return s;
    }

    /** Placement points for a 1-based place (without the top-10 bonus). */
    int placementPoints(int place) {
        return Math.max(ptsMin, ptsFirst - (place - 1) / ptsStepEvery);
    }

    int top10Bonus(int place) {
        return place >= 1 && place <= top10.size() ? top10.get(place - 1) : 0;
    }
}
