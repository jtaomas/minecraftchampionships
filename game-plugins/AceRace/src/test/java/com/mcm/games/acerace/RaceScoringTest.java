package com.mcm.games.acerace;

import com.mcm.games.acerace.map.RaceMapData;
import com.mcm.games.acerace.map.maps.SkyCircuitMap;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

/**
 * Plain-Java proof that the shipped config.yml pays the MCC Ace Race coins (game-plugins/COINS.md). Run from the
 * plugin folder: java -cp target/classes;target/test-classes;&lt;paper-api deps&gt; com.mcm.games.acerace.RaceScoringTest
 */
public final class RaceScoringTest {
    private static int passed, failed;

    public static void main(String[] args) {
        RaceScoring s = new RaceScoring();
        s.load(YamlConfiguration.loadConfiguration(new File("src/main/resources/config.yml")));
        eq("1st finisher", 400, s.finishCoins(1));
        eq("2nd finisher", 390, s.finishCoins(2));
        eq("40th finisher", 10, s.finishCoins(40));
        eq("41st finisher", 0, s.finishCoins(41));
        eq("placement 1", 300, s.placementCoins(1));
        eq("placement 2", 240, s.placementCoins(2));
        eq("placement 3", 180, s.placementCoins(3));
        eq("placement 4", 120, s.placementCoins(4));
        eq("placement 9", 120, s.placementCoins(9));
        eq("placement 10", 60, s.placementCoins(10));
        eq("placement 14", 60, s.placementCoins(14));
        eq("placement 15", 15, s.placementCoins(15));
        eq("placement 19", 15, s.placementCoins(19));
        eq("placement 20", 0, s.placementCoins(20));
        eq("1st total (MCC 700)", 700, s.placePoints(1));
        eq("no checkpoint coins", 0, s.checkpointCoins);
        eq("no lap coins", 0, s.lapCoins);
        RaceMapData sc = new SkyCircuitMap().build(0L);
        List<String> errs = sc.check();
        eq("SkyCircuitMap check errors", 0, errs.size());
        if (!errs.isEmpty()) System.out.println("SkyCircuitMap errors: " + errs);
        System.out.println("RaceScoringTest: " + passed + " passed, " + failed + " failed (SkyCircuit blocks=" + sc.blocks().size() + ")");
        if (failed > 0) System.exit(1);
    }

    private static void eq(String what, int want, int got) {
        if (want == got) passed++;
        else { failed++; System.out.println("FAIL " + what + ": want " + want + " got " + got); }
    }
}
