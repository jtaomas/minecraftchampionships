package com.mcm.games.race;

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
    /** Race rules (defaults, not researched): finish on a line / gate volume, no snowballs, no punching. */
    boolean finishLine, raceSnowballs, punching;
    double kbPunchH, kbPunchV, kbSprint, kbSnowH, kbSnowV; int hitCooldownTicks;
    double kPerPunch, kSameBonus, kDecay, kMoreFrom, kPerKarma, kMaxMult, kGlowFrom, kLockFrom;
    int kSameSeconds, kLockSeconds; boolean kSlowness, kEnabled;
    double protPerHit, protMax, protDecay;
    int ptsFirst, ptsStepEvery, ptsMin; List<Integer> top10 = new ArrayList<>(), teamBonus = new ArrayList<>();
    int chickenCount; boolean chickenWander;
    String chickenName; double decoyPenaltySeconds; boolean logPunches;
    boolean infiniteBlocks; int refillDelayTicks;
    double glideKbMult; int elytraFloorDelaySeconds;
    boolean crashKills, floorKills; double crashMinSpeed, crashKeep, hardLandingSpeed; float hardFallBlocks;
    boolean snowKarma, logSnowballs;
    boolean randomSeeds;
    boolean bonusEnabled; List<Integer> bonusTier = new ArrayList<>(); int bonusCap;
    String decoyMessage; int decoyRadius; boolean decoysEnabled;
    List<String> punsVoid = new ArrayList<>(), punsHazard = new ArrayList<>();

    static Settings load(FileConfiguration c) {
        Settings s = new Settings();
        s.rounds = Math.max(1, c.getInt("rounds", 6));
        s.roundSeconds = Math.max(10, c.getInt("round-seconds", 300));
        s.preRoundSeconds = Math.max(4, c.getInt("pre-round-seconds", 15));
        s.intermissionSeconds = Math.max(2, c.getInt("intermission-seconds", 6));
        s.timeSkip = c.getBoolean("time-skip.enabled", true);
        s.timeSkipFraction = c.getDouble("time-skip.finished-fraction", 0.5);
        s.timeSkipSeconds = c.getInt("time-skip.seconds-left", 20);
        s.baseY = c.getInt("world.base-y", 64);
        s.spacing = Math.max(520, c.getInt("world.map-spacing", 600));
        s.concrete = c.getInt("kit.concrete", 64);
        s.snowballs = c.getInt("kit.snowballs", 25);
        s.finishLine = !"chickens".equalsIgnoreCase(c.getString("race.finish", "line"));
        s.raceSnowballs = c.getBoolean("race.snowballs", false);
        s.punching = c.getBoolean("race.punching", false);
        s.fireworks = c.getInt("kit.fireworks", 2);
        Material pk = Material.matchMaterial(c.getString("kit.pickaxe", "IRON_PICKAXE"));
        s.pickaxe = pk == null ? Material.IRON_PICKAXE : pk;
        s.kbPunchH = c.getDouble("knockback.punch-horizontal", 0.42);
        s.kbPunchV = c.getDouble("knockback.punch-vertical", 0.36);
        s.kbSprint = c.getDouble("knockback.sprint-bonus", 0.35);
        s.kbSnowH = c.getDouble("knockback.snowball", c.getDouble("knockback.snowball-horizontal", 1.375));
        s.kbSnowV = c.getDouble("knockback.snowball-vertical", 0.45);
        s.snowKarma = c.getBoolean("karma.snowball-hits-count", false);
        s.logSnowballs = c.getBoolean("knockback.log-snowball-hits", true);
        s.hitCooldownTicks = c.getInt("knockback.hit-cooldown-ticks", 10);
        s.kEnabled = c.getBoolean("karma.enabled", false);
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
        // MCC coins (TGTTOSAWAF; base x1.0, the core applies the event multiplier). See game-plugins/COINS.md.
        s.ptsFirst = c.getInt("coins.finish-first", 40);                       // MCC: the 1st finisher
        s.ptsStepEvery = Math.max(0, c.getInt("coins.finish-step", 1));        // MCC: 1 less per lower placement
        s.ptsMin = Math.max(0, c.getInt("coins.finish-minimum", 0));
        s.top10 = c.getIntegerList("coins.top10-bonus");
        if (s.top10.isEmpty()) s.top10 = List.of(80, 75, 70, 65, 60, 55, 50, 45, 40, 35);   // MCC: first ten finishers
        s.teamBonus = c.getIntegerList("coins.team-bonus");
        if (s.teamBonus.isEmpty()) s.teamBonus = List.of(100, 75, 50, 25);    // MCC: first four whole teams (shared)
        s.chickenCount = Math.max(1, c.getInt("chickens.count", 10));
        s.chickenWander = c.getBoolean("chickens.wander", false);
        s.chickenName = org.bukkit.ChatColor.translateAlternateColorCodes('&', c.getString("goal.chicken-name", "&e&lPunch me!"));
        s.decoyPenaltySeconds = c.getDouble("goal.decoy-penalty-seconds", 0);
        s.logPunches = c.getBoolean("goal.log-punches", true);
        s.infiniteBlocks = c.getBoolean("bridging.infinite-blocks", true);
        s.refillDelayTicks = Math.max(1, c.getInt("bridging.refill-delay-ticks", 1));
        s.glideKbMult = c.getDouble("knockback.glide-multiplier", 1.6);
        s.elytraFloorDelaySeconds = Math.max(0, c.getInt("elytra.floor-drop-delay-seconds", 3));
        s.crashKills = c.getBoolean("elytra.crash-kills", true);
        s.floorKills = c.getBoolean("elytra.floor-kills", false);
        s.crashMinSpeed = Math.max(0.05, c.getDouble("elytra.crash-min-speed", 0.25));
        s.crashKeep = Math.max(0.05, Math.min(0.95, c.getDouble("elytra.crash-keep", 0.4)));
        s.hardLandingSpeed = Math.max(0.2, c.getDouble("elytra.hard-landing-speed", 0.75));
        s.hardFallBlocks = (float) Math.max(3, c.getDouble("elytra.hard-fall-blocks", 8));
        s.randomSeeds = c.getBoolean("random-seeds", false);
        s.bonusEnabled = c.getBoolean("bonus.enabled", true);
        s.bonusTier = c.getIntegerList("coins.line-bonus-tier");                // OURS (MCC has no line bonus)
        if (s.bonusTier.isEmpty()) s.bonusTier = List.of(5, 15);
        s.bonusCap = Math.max(0, c.getInt("coins.line-bonus-cap", 75));
        s.decoyMessage = org.bukkit.ChatColor.translateAlternateColorCodes('&', c.getString("goal.decoy-message", "haha lol"));
        s.decoyRadius = Math.max(1, c.getInt("goal.decoy-leash-radius", 4));
        s.decoysEnabled = c.getBoolean("goal.decoys", false);
        s.punsVoid = c.getStringList("puns-void");
        s.punsHazard = c.getStringList("puns-hazard");
        if (s.punsVoid.isEmpty()) s.punsVoid = Puns.VOID;
        if (s.punsHazard.isEmpty()) s.punsHazard = Puns.HAZARD;
        return s;
    }

    /** Line-bonus points for a gate tier (1 = orange / medium, 2 = red / hard; higher tiers pay the last entry). */
    int tierPoints(int tier) {
        if (!bonusEnabled || tier < 1) return 0;
        return bonusTier.get(Math.min(tier, bonusTier.size()) - 1);
    }

    /** Placement points for a 1-based place (without the top-10 bonus). */
    /** MCC: 40 coins for the 1st finisher, 1 less per lower placement (ptsStepEvery = coins less per place). */
    int placementPoints(int place) {
        return Math.max(ptsMin, ptsFirst - (place - 1) * ptsStepEvery);
    }

    int top10Bonus(int place) {
        return place >= 1 && place <= top10.size() ? top10.get(place - 1) : 0;
    }
}
