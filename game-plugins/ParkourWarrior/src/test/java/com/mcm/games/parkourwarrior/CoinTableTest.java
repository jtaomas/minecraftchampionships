package com.mcm.games.parkourwarrior;

import com.mcm.games.parkourwarrior.CourseModel.Difficulty;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Plain-Java proof that the shipped config.yml pays the MCC Parkour Warrior Dojo coins (game-plugins/COINS.md).
 * Run from the plugin folder: java -cp target/classes;target/test-classes;&lt;deps&gt; com.mcm.games.parkourwarrior.CoinTableTest
 */
public final class CoinTableTest {
    private static int passed, failed;

    public static void main(String[] args) {
        Settings s = Settings.load(YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml")),
                Logger.getLogger("test"));
        eq("stone medal", 2, Scoring.delta(s, Scoring.Medal.STONE, 1));
        int[][] want = {{5, 15, 25, 40, 60}, {10, 25, 45, 70, 100}, {15, 30, 55, 90, 140}};
        Scoring.Medal[] metals = {Scoring.Medal.BRONZE, Scoring.Medal.SILVER, Scoring.Medal.GOLD};
        for (int m = 0; m < 3; m++) {
            int total = 0;
            for (int n = 1; n <= 5; n++) {
                total += Scoring.delta(s, metals[m], n);
                eq(metals[m] + " total for " + n, want[m][n - 1], total);
            }
        }
        eq("15 stone + 5 of each metal", 30 + 60 + 100 + 140, Scoring.medalPoints(s, 15, 5, 5, 5));
        double mult = Scoring.multiplier(s, List.of(Difficulty.HARD, Difficulty.MEDIUM));
        eqd("hard + medium multiplier", 2.15, mult);
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        Map<UUID, Integer> sh = Scoring.shares(s, List.of(new Scoring.Member(a, 100, Difficulty.HARD),
                new Scoring.Member(b, 100, null)), mult);
        eq("extra adds up to sum x (mult - 1)", 230, sh.get(a) + sh.get(b));
        System.out.println("CoinTableTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void eq(String what, int want, int got) {
        if (want == got) passed++;
        else { failed++; System.out.println("FAIL " + what + ": want " + want + " got " + got); }
    }

    private static void eqd(String what, double want, double got) {
        if (Math.abs(want - got) < 1e-9) passed++;
        else { failed++; System.out.println("FAIL " + what + ": want " + want + " got " + got); }
    }
}
