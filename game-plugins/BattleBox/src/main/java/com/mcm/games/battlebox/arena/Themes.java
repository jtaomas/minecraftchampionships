package com.mcm.games.battlebox.arena;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

/** Registry of the arena themes (pure Java). */
public final class Themes {
    private static volatile List<ArenaTheme> IMPORTED = List.of();

    private Themes() {}

    /** Load all valid imported arenas from {@code dir} (e.g. {@code plugins/McmBattleBox/imported-arenas}). */
    public static void loadImported(File dir, Logger log) {
        List<ImportedBbArena> loaded = ImportedBbLoader.loadAll(dir, log);
        List<ArenaTheme> list = new ArrayList<>(loaded.size());
        for (ImportedBbArena a : loaded) {
            list.add(new ImportedArenaTheme(a));
        }
        IMPORTED = Collections.unmodifiableList(list);
    }

    public static List<ArenaTheme> procedural() {
        List<ArenaTheme> l = new ArrayList<>();
        l.add(new GreenhouseTheme());
        l.add(new ToyWorkshopTheme());
        l.add(new SkiLodgeTheme());
        return l;
    }

    public static List<ArenaTheme> imported() {
        return IMPORTED;
    }

    public static List<ArenaTheme> all() {
        List<ArenaTheme> l = procedural();
        l.addAll(IMPORTED);
        return l;
    }

    public static ArenaTheme byId(String id) {
        if (id == null) return null;
        String raw = id.trim().toLowerCase(Locale.ROOT);
        String k = raw.replace("_", "").replace("-", "").replace(" ", "");
        for (ArenaTheme t : all()) {
            if (t.id().equalsIgnoreCase(raw) || t.id().replace("_", "").equals(k)) return t;
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
