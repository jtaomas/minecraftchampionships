package com.mcm.games.bingo;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/** Values from plugins/McmBingo/config.yml (all "defaults, not researched" unless the README says otherwise). */
final class Settings {
    int[] bingoMix = {0, 9, 10, 5, 1};
    int[] lockoutMix = {0, 6, 10, 6, 3};
    int woodCap = 2;
    int[] bingoTierPoints = {0, 3, 5, 8, 12};
    int[] placementPercent = {100, 80, 60, 40};
    int[] lockoutTierPoints = {0, 5, 8, 12, 16};
    int bingoLineFirst4 = 12, bingoLineLater = 6, bingoFullCard = 30;
    int lockoutLine = 15;
    int[] lockoutPlacement = {20, 12, 6};
    int killPoints = 0;

    int topCooldown = 30;
    boolean topBlockedInCombat = true;
    int combatSeconds = 8;
    int finderCooldown = 20;
    int finderRadiusChunks = 2;
    int teleportCooldown = 45;
    int beef = 64;

    boolean mobSpawning = true;
    boolean daylightCycle = true;
    long startTime = 0;

    boolean placeStructures = true;
    double caveChance = 0.06;
    double caveEntranceShare = 0.3;

    boolean debugTiming = false;

    static Settings load(FileConfiguration c) {
        Settings s = new Settings();
        s.bingoMix = mix(c.getIntegerList("card.bingo-mix"), s.bingoMix);
        s.lockoutMix = mix(c.getIntegerList("card.lockout-mix"), s.lockoutMix);
        s.woodCap = c.getInt("card.max-wood-only-items", s.woodCap);
        s.bingoTierPoints = mix(c.getIntegerList("points.bingo-tier"), s.bingoTierPoints);
        s.placementPercent = arr(c.getIntegerList("points.bingo-placement-percent"), s.placementPercent);
        s.lockoutTierPoints = mix(c.getIntegerList("points.lockout-tier"), s.lockoutTierPoints);
        s.bingoLineFirst4 = c.getInt("points.bingo-line-first-4", s.bingoLineFirst4);
        s.bingoLineLater = c.getInt("points.bingo-line-later", s.bingoLineLater);
        s.bingoFullCard = c.getInt("points.bingo-full-card", s.bingoFullCard);
        s.lockoutLine = c.getInt("points.lockout-line", s.lockoutLine);
        s.lockoutPlacement = arr(c.getIntegerList("points.lockout-placement"), s.lockoutPlacement);
        s.killPoints = Math.max(0, c.getInt("points.pvp-kill", s.killPoints));
        s.topCooldown = Math.max(0, c.getInt("top.cooldown-seconds", s.topCooldown));
        s.topBlockedInCombat = c.getBoolean("top.blocked-in-combat", s.topBlockedInCombat);
        s.combatSeconds = Math.max(0, c.getInt("top.combat-seconds", s.combatSeconds));
        s.finderCooldown = Math.max(1, c.getInt("cave-finder.cooldown-seconds", s.finderCooldown));
        s.finderRadiusChunks = Math.max(1, Math.min(4, c.getInt("cave-finder.radius-chunks", s.finderRadiusChunks)));
        s.teleportCooldown = Math.max(0, c.getInt("card.teleport-cooldown-seconds", s.teleportCooldown));
        s.beef = Math.max(0, Math.min(64, c.getInt("kit.cooked-beef", s.beef)));
        s.mobSpawning = c.getBoolean("world.mob-spawning", s.mobSpawning);
        s.daylightCycle = c.getBoolean("world.daylight-cycle", s.daylightCycle);
        s.startTime = Math.floorMod(c.getLong("world.start-time", s.startTime), 24000L);
        s.placeStructures = c.getBoolean("world.place-structures", s.placeStructures);
        s.caveChance = c.getDouble("world.extra-cave-chance-per-chunk", s.caveChance);
        s.caveEntranceShare = c.getDouble("world.extra-cave-entrance-share", s.caveEntranceShare);
        s.debugTiming = c.getBoolean("debug-timing", false);
        return s;
    }

    /** A per-tier list of 4 values becomes an array indexed 1..4. */
    private static int[] mix(List<Integer> l, int[] def) {
        if (l == null || l.size() != 4) return def;
        return new int[]{0, l.get(0), l.get(1), l.get(2), l.get(3)};
    }

    private static int[] arr(List<Integer> l, int[] def) {
        if (l == null || l.isEmpty()) return def;
        int[] a = new int[l.size()];
        for (int i = 0; i < a.length; i++) a[i] = l.get(i);
        return a;
    }
}
