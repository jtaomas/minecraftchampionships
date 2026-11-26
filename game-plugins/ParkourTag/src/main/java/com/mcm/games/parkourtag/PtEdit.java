package com.mcm.games.parkourtag;

import com.mcm.api.McmEditSpace;
import com.mcm.api.McmMapEditable;
import com.mcm.games.parkourtag.map.Cage;
import com.mcm.games.parkourtag.map.Maps;
import com.mcm.games.parkourtag.map.PtMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Core {@code /mcm mapedit} hook (Map Edit core, own class so the plugin still loads on an older core). An edit session
 * builds the map exactly like a game arena (same placements, no overlay) at a fresh x offset far from every game arena
 * in {@code mcc_parkourtag}, with no game running. The result is saved as the map's overlay ({@link PtOverlay}).
 */
final class PtEdit implements McmMapEditable {
    private static final int EDIT_X0 = 100_000;
    private static int counter;

    private final ParkourTagGame game;

    PtEdit(ParkourTagGame game) { this.game = game; }

    private static PtMap find(String id) {
        for (PtMap m : Maps.all()) if (m.id().equalsIgnoreCase(id)) return m;
        return null;
    }

    @Override
    public List<String> editableMaps() {
        List<String> out = new ArrayList<>();
        for (PtMap m : Maps.all()) out.add(m.id());
        return out;
    }

    @Override public String editableMapName(String id) { PtMap m = find(id); return m == null ? id : m.displayName() + (m.finished() ? "" : " (unfinished)"); }

    @Override public Material editableMapIcon(String id) { return Material.LADDER; }

    @Override
    public String editBlockedReason(String id) {
        if (find(id) == null) return "unknown map";
        if (game.isRunning()) return "Parkour Tag is running";
        return null;
    }

    @Override
    public void buildForEdit(String mapId, Consumer<McmEditSpace> done) {
        PtMap m = find(mapId);
        World w = game.editWorld();
        if (m == null || w == null) { done.accept(null); return; }
        final int ox = EDIT_X0 + (counter++) * Math.max(600, Math.max(m.sizeX(), m.sizeZ()) + 200);
        final int oy = ParkourTagGame.BASE_Y;
        ArenaBuilder b = game.arenaBuilder();
        b.build(w, ox, oy, 0, game.basePlacements(m));
        b.whenIdle(() -> done.accept(space(m, w, ox, oy)));
    }

    private McmEditSpace space(PtMap m, World w, int ox, int oy) {
        double[] s = m.spectator();
        Location spawn = new Location(w, ox + s[0], oy + s[1], s[2], (float) s[3], (float) s[4]);
        int g = PtOverlay.MARGIN;
        McmEditSpace sp = new McmEditSpace(w, ox, oy, 0, -g, -8, -g, m.sizeX() - 1 + g, m.sizeY() + 30, m.sizeZ() - 1 + g, spawn);
        sp.anchor("overview", spawn);
        for (int i = 0; i < m.cages().size(); i++) {
            Cage c = m.cages().get(i);
            double[] p = c.slot(0);
            sp.anchor("cage" + (i + 1), new Location(w, ox + p[0], oy + p[1], p[2], (float) p[3], 0f));
            for (int k = 0; k < 5; k++) {
                double[] q = c.slot(k);
                sp.marker("cage " + (i + 1) + " spawn pad " + (k + 1), (int) Math.floor(q[0]), (int) q[1], (int) Math.floor(q[2]), McmEditSpace.Kind.STAND);
            }
            int[] sg = c.signPos();
            sp.marker("cage " + (i + 1) + " 'Select Hunter' sign", sg[0], sg[1], sg[2], McmEditSpace.Kind.SOLID);
        }
        sp.note("Shown like an arena copy. Cage glass, doors, carpet and the hunter sign are painted by the game each round: edits to those cells are overwritten.");
        sp.note("Edits are saved for every arena copy; blocks more than " + g + " blocks outside the map box are not tracked.");
        return sp;
    }

    @Override
    public void editClosed(String mapId, McmEditSpace space) {
        if (space == null) return;
        // the edit copy sits at its own x offset; clear it so the world does not keep it (the core keeps the chunks loaded until now)
        ArenaBuilder b = game.arenaBuilder();
        b.clear(space.world, space.originX + space.minX, space.originY + space.minY, space.originZ + space.minZ,
                space.originX + space.maxX, space.originY + space.maxY, space.originZ + space.maxZ);
    }
}
