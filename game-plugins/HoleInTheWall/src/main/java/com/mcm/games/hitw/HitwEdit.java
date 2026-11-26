package com.mcm.games.hitw;

import com.mcm.api.McmEditSpace;
import com.mcm.api.McmMapEditable;
import com.mcm.api.McmMapOverlay;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.function.Consumer;

/**
 * Core {@code /mcm mapedit} hook (Map Edit core, own class so the plugin loads on an older core). The platform is the
 * only map and always sits at the world origin, so it is edited in place: the editor sees the freshly built platform
 * (no walls, no game running) and the saved overlay ({@code plugins/McmHoleInTheWall/mapedits/platform.overlay.gz})
 * is applied by {@link #applyOverlay} after {@link Arena#build}. Edits are tracked in the arena's clear box
 * (x/z -19..19, y 78..112) so {@link Arena#clear} always removes them again.
 */
final class HitwEdit implements McmMapEditable {
    static final String MAP = "platform";
    private static final int R = 19, Y0 = 78, Y1 = 112;

    private final Plugin plugin;
    private final HoleInTheWallGame game;
    private final Arena arena;

    HitwEdit(Plugin plugin, HoleInTheWallGame game, Arena arena) { this.plugin = plugin; this.game = game; this.arena = arena; }

    @Override public List<String> editableMaps() { return List.of(MAP); }
    @Override public String editableMapName(String id) { return "Platform"; }
    @Override public Material editableMapIcon(String id) { return Material.WHITE_CONCRETE; }

    @Override
    public void buildForEdit(String mapId, Consumer<McmEditSpace> done) {
        World w = arena.ensureWorld();
        if (w == null) { done.accept(null); return; }
        arena.clear();
        arena.build();
        Location spawn = new Location(w, 0.5, Arena.FLOOR_Y + 4, 0.5, 0f, 30f);
        McmEditSpace sp = new McmEditSpace(w, 0, 0, 0, -R, Y0, -R, R, Y1, R, spawn);
        sp.marker("centre spawn", 0, Arena.FLOOR_Y + 1, 0, McmEditSpace.Kind.STAND);
        sp.note("Edited in place at the arena (world origin). The walls, pads and shrinking rings are the game's own and are not shown.");
        sp.note("Edits are tracked inside x/z -" + R + ".." + R + ", y " + Y0 + ".." + Y1 + " (the area the game clears after every round).");
        done.accept(sp);
    }

    @Override
    public void editClosed(String mapId, McmEditSpace space) {
        if (!game.isRunning()) arena.clear();
    }

    /** Called right after {@link Arena#build}: the saved platform edits on top. */
    static void applyOverlay(Plugin plugin, World w) {
        try {
            McmMapOverlay ov = McmMapOverlay.of(plugin, MAP);
            if (ov == null || ov.isEmpty()) return;
            int n = ov.applyTo(w, 0, 0, 0);
            plugin.getLogger().info("Hole in the Wall: applied the Map Edit overlay (" + n + " blocks)");
        } catch (Throwable t) {
            plugin.getLogger().warning("Hole in the Wall: map overlay could not be applied: " + t);
        }
    }
}
