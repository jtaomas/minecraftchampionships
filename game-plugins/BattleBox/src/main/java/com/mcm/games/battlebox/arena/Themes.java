package com.mcm.games.battlebox.arena;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Registry of the arena themes (pure Java). */
public final class Themes {
    private Themes() {}

    public static List<ArenaTheme> all() {
        List<ArenaTheme> l = new ArrayList<>();
        l.add(new GreenhouseTheme());
        l.add(new ToyWorkshopTheme());
        l.add(new SkiLodgeTheme());
        l.add(new CanalShipsTheme());
        l.add(new TrainStationTheme());
        l.add(new VillaCourtyardTheme());
        return l;
    }

    public static ArenaTheme byId(String id) {
        if (id == null) return null;
        String k = id.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
        for (ArenaTheme t : all()) {
            if (t.id().equals(k)) return t;
        }
        if (k.startsWith("green")) return new GreenhouseTheme();
        if (k.startsWith("toy")) return new ToyWorkshopTheme();
        if (k.startsWith("ski")) return new SkiLodgeTheme();
        if (k.startsWith("canal") || k.startsWith("ship") || k.startsWith("harbour")) return new CanalShipsTheme();
        if (k.startsWith("train") || k.startsWith("station") || k.startsWith("terminus")) return new TrainStationTheme();
        if (k.startsWith("villa") || k.startsWith("courtyard")) return new VillaCourtyardTheme();
        return null;
    }

    public static List<String> ids() {
        List<String> l = new ArrayList<>();
        for (ArenaTheme t : all()) l.add(t.id());
        return l;
    }
}
