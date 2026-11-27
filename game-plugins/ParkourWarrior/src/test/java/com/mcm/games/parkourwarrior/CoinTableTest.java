package com.mcm.games.parkourwarrior;

import com.mcm.games.parkourwarrior.CourseModel.Difficulty;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.logging.Logger;

/** Plain-Java check that the shipped config.yml coin table matches game-plugins/COINS.md (Parkour Warrior section). */
public final class CoinTableTest {
    private static int passed, failed;

    public static void main(String[] args) {
        Settings s = Settings.load(YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml")), Logger.getLogger("test"));
        eq("main coins", 4, s.mainCoins);
        eq("bonus bronze/silver/gold", 4 * 10000 + 6 * 100 + 8, s.bonusBronze * 10000 + s.bonusSilver * 100 + s.bonusGold);
        eq("ending easy", 10, Scoring.ending(s, Difficulty.EASY));
        eq("ending medium", 20, Scoring.ending(s, Difficulty.MEDIUM));
        eq("ending hard", 40, Scoring.ending(s, Difficulty.HARD));
        // linear replica course mcc6_full: 23 checkpoints (4 .. 8 coins by stage) + finish; one line, no branch / difficulty multipliers
        int sum = 0;
        for (int k = 1; k <= 23; k++) sum += Scoring.linearCheckpoint(s, "mcc6_full", k, 3);
        eq("mcc6_full checkpoint 1", 4, Scoring.linearCheckpoint(s, "mcc6_full", 1, 3));
        eq("mcc6_full checkpoint 23", 8, Scoring.linearCheckpoint(s, "mcc6_full", 23, 3));
        eq("mcc6_full 23 checkpoints", 139, sum);
        eq("mcc6_full finish", 40, Scoring.finish(s, "mcc6_full"));
        eq("unlisted linear map: stage-scaled default, stage 1", 4, Scoring.linearCheckpoint(s, "other", 1, 3));
        eq("unlisted linear map: stage-scaled default, stage 8", 8, Scoring.linearCheckpoint(s, "other", 22, 3));
        System.out.println("CoinTableTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    private static void eq(String what, int want, int got) {
        if (want == got) passed++;
        else { failed++; System.out.println("FAIL " + what + ": want " + want + " got " + got); }
    }
}
