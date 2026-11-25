package com.mcm.games.rocketspleef;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Old MCC Rocket Spleef settings (config.yml). Every value carries its source and confidence in config.yml and in
 * research/mcc_archive/rocket_spleef_old/notes.md. Reloadable with /rs reload.
 */
public final class RocketConfig {
    public final int rounds, countdownSeconds, roundSeconds, tntTimeAtSeconds, intermissionSeconds;
    public final int mapAnchorY, elimDepth;

    public final int clip, startAmmo, reloadTicks, shotGapTicks, rocketLifeTicks, dropInTitleSeconds, tntWarningSeconds;
    public final boolean reloadResetsOnFire, tntNight;
    public final double meleeKnockback;
    public final int dropRoomHeight;
    public final double rocketSpeed, blastRadius, knockbackRadius, knockbackHorizontal, rocketJumpHeight;

    public final int fireworks, fireworkFlight;

    public final double tntPerSecond, tntBlastRadius, tntKnockbackRadius;
    public final int tntDropMin, tntDropMax, tntFuseTicks;

    public final boolean punching;
    public final long killCreditMs;

    public final double ptsOutlive;
    public final int ptsElimination, ptsSoleTeamSurvival;

    /** Launch speed (blocks/tick) that peaks at {@link #rocketJumpHeight} blocks under vanilla player gravity. */
    public final double rocketJumpVelocity;

    public RocketConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();
        rounds = c.getInt("rounds", 3);
        countdownSeconds = c.getInt("countdown-seconds", 15);
        dropInTitleSeconds = c.getInt("drop-in-title-seconds", 10);
        dropRoomHeight = c.getInt("drop-room-height", 12);
        roundSeconds = c.getInt("round-seconds", 240);
        tntTimeAtSeconds = c.getInt("tnt-time-at-seconds", 120);
        intermissionSeconds = c.getInt("intermission-seconds", 10);
        mapAnchorY = c.getInt("map-anchor-y", 80);
        elimDepth = c.getInt("elimination-depth", 10);

        clip = c.getInt("launcher.clip", 3);
        startAmmo = c.getInt("launcher.start-ammo", 0);
        reloadTicks = c.getInt("launcher.reload-ticks", 42);
        reloadResetsOnFire = c.getBoolean("launcher.reload-resets-on-fire", true);
        meleeKnockback = c.getDouble("launcher.melee-knockback", 0.9);
        shotGapTicks = c.getInt("launcher.shot-gap-ticks", 8);
        rocketSpeed = c.getDouble("launcher.rocket-speed", 2.0);
        rocketLifeTicks = c.getInt("launcher.rocket-life-ticks", 60);
        blastRadius = c.getDouble("launcher.blast-radius", 2.5);
        knockbackRadius = c.getDouble("launcher.knockback-radius", 4.0);
        knockbackHorizontal = c.getDouble("launcher.knockback-horizontal", 1.1);
        rocketJumpHeight = c.getDouble("launcher.rocket-jump-height", 9.0);

        fireworks = c.getInt("firework.count", 1);
        fireworkFlight = c.getInt("firework.flight-duration", 1);

        tntPerSecond = c.getDouble("tnt.per-second", 4.0);
        tntWarningSeconds = c.getInt("tnt.warning-seconds", 10);
        tntNight = c.getBoolean("tnt.night-sky", true);
        tntDropMin = c.getInt("tnt.drop-height-min", 15);
        tntDropMax = c.getInt("tnt.drop-height-max", 25);
        tntFuseTicks = c.getInt("tnt.fuse-ticks", 80);
        tntBlastRadius = c.getDouble("tnt.blast-radius", 3.0);
        tntKnockbackRadius = c.getDouble("tnt.knockback-radius", 4.5);

        punching = c.getBoolean("punching", true);
        killCreditMs = c.getLong("kill-credit-ms", 10000L);

        // MCC coins, old Rocket Spleef (final values MCC 12-13; base x1.0, the core applies the event multiplier). COINS.md
        ptsOutlive = c.getDouble("coins.outlive", 3);                   // MCC 8+: another player dies while you are alive
        ptsElimination = c.getInt("coins.elimination", 10);             // MCC 12+: personal elimination (last hit)
        ptsSoleTeamSurvival = c.getInt("coins.sole-team-survival", 100); // MCC 3+: lasting the round, only if one team is left

        rocketJumpVelocity = velocityForHeight(rocketJumpHeight);
    }

    /** Vanilla player motion per tick: y += v; v = (v - 0.08) * 0.98. Binary-searches v for a given peak height. */
    public static double velocityForHeight(double h) {
        double lo = 0, hi = 5;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            if (peak(mid) < h) lo = mid; else hi = mid;
        }
        return (lo + hi) / 2;
    }

    public static double peak(double v) {
        double y = 0, best = 0;
        for (int t = 0; t < 400 && v > 0; t++) {
            y += v;
            v = (v - 0.08) * 0.98;
            best = Math.max(best, y);
        }
        return best;
    }
}
