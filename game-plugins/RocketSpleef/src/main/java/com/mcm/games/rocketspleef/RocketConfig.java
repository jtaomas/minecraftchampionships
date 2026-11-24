package com.mcm.games.rocketspleef;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Configuration values for Rocket Spleef Rush, loaded from config.yml with sensible defaults.
 */
public final class RocketConfig {
    public final int rounds;
    public final int countdownSeconds;
    public final int activeSeconds;
    public final int overtimeSeconds;
    public final int intermissionSeconds;
    public final int elimY;
    public final int clearRadius;

    public final int rocketCooldownTicks;
    public final double rocketSpeed;
    public final int rocketLifeTicks;
    public final double blastRadius;
    public final double knockbackRadius;
    public final double knockbackStrength;
    public final double knockbackVertical;

    public final int updraftCharges;
    public final long updraftCooldownMs;
    public final double updraftVelocity;
    public final boolean allowDoubleJump;

    public final long koCreditMs;

    public final double survivalPool;
    public final int koPoints;
    public final int lastSideBonus;
    public final int sharedSurvivorBonus;
    public final int[] placementBonus;

    public RocketConfig(JavaPlugin plugin) {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        this.rounds = Math.max(1, c.getInt("rounds", 3));
        this.countdownSeconds = Math.max(1, c.getInt("countdown-seconds", 5));
        this.activeSeconds = Math.max(15, c.getInt("active-seconds", 120));
        this.overtimeSeconds = Math.max(10, c.getInt("overtime-seconds", 45));
        this.intermissionSeconds = Math.max(1, c.getInt("intermission-seconds", 7));
        this.elimY = c.getInt("elim-y", 64);
        this.clearRadius = Math.max(30, c.getInt("clear-radius", 46));

        this.rocketCooldownTicks = Math.max(4, c.getInt("rocket.cooldown-ticks", 18));
        this.rocketSpeed = Math.max(0.5, c.getDouble("rocket.speed", 1.8));
        this.rocketLifeTicks = Math.max(10, c.getInt("rocket.life-ticks", 70));
        this.blastRadius = Math.max(1.0, c.getDouble("rocket.blast-radius", 2.7));
        this.knockbackRadius = Math.max(1.5, c.getDouble("rocket.knockback-radius", 4.5));
        this.knockbackStrength = Math.max(0.2, c.getDouble("rocket.knockback-strength", 1.25));
        this.knockbackVertical = Math.max(0.1, c.getDouble("rocket.knockback-vertical", 0.65));

        this.updraftCharges = Math.max(0, c.getInt("updraft.charges-per-round", 2));
        this.updraftCooldownMs = Math.max(200, c.getLong("updraft.cooldown-ms", 1200));
        this.updraftVelocity = Math.max(0.5, c.getDouble("updraft.vertical-velocity", 1.35));
        this.allowDoubleJump = c.getBoolean("updraft.allow-double-jump", false);

        this.koCreditMs = Math.max(1000, c.getLong("ko-credit-ms", 10_000));

        this.survivalPool = Math.max(0.0, c.getDouble("scoring.survival-pool", 18.0));
        this.koPoints = Math.max(0, c.getInt("scoring.ko-points", 5));
        this.lastSideBonus = Math.max(0, c.getInt("scoring.last-side-bonus", 12));
        this.sharedSurvivorBonus = Math.max(0, c.getInt("scoring.shared-survivor-bonus", 6));

        List<Integer> pb = c.getIntegerList("scoring.placement-bonus");
        if (pb.isEmpty()) {
            this.placementBonus = new int[]{5, 3, 2};
        } else {
            this.placementBonus = pb.stream().mapToInt(Integer::intValue).toArray();
        }
    }
}
