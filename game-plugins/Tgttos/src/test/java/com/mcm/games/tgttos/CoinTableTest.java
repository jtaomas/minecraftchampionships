package com.mcm.games.tgttos;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

/**
 * Plain-Java proof that the shipped config.yml pays the MCC TGTTOSAWAF coins (game-plugins/COINS.md).
 * Run from the plugin folder: java -cp target/classes;target/test-classes;&lt;deps&gt; com.mcm.games.tgttos.CoinTableTest
 */
public final class CoinTableTest {
    private static int passed, failed;

    public static void main(String[] args) {
        Settings s = Settings.load(YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml")));
        eq("1st finish", 40, s.placementPoints(1));
        eq("2nd finish", 39, s.placementPoints(2));
        eq("10th finish", 31, s.placementPoints(10));
        eq("40th finish", 1, s.placementPoints(40));
        eq("41st finish", 0, s.placementPoints(41));
        eq("top10 1st", 80, s.top10Bonus(1));
        eq("top10 10th", 35, s.top10Bonus(10));
        eq("top10 11th", 0, s.top10Bonus(11));
        eq("1st total (MCC 120)", 120, s.placementPoints(1) + s.top10Bonus(1));
        if (s.teamBonus.equals(List.of(100, 75, 50, 25))) passed++; else { failed++; System.out.println("FAIL team bonus " + s.teamBonus); }
        System.out.println("CoinTableTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void eq(String what, int want, int got) {
        if (want == got) passed++;
        else { failed++; System.out.println("FAIL " + what + ": want " + want + " got " + got); }
    }
}
