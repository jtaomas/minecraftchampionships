package com.mcm.games.battlebox;

import com.mcm.api.McmBuild;
import com.mcm.api.McmEditSpace;
import com.mcm.api.McmMapEditable;
import com.mcm.games.battlebox.arena.ArenaGeo;
import com.mcm.games.battlebox.arena.ArenaTheme;
import com.mcm.games.battlebox.arena.Grid;
import com.mcm.games.battlebox.arena.Themes;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Core {@code /mcm mapedit} hook (Map Edit core; own class so the plugin still loads on an older core). An edit
 * session builds one arena copy exactly like a game round does (no overlay, red/blue gates closed, fixed seed) at a
 * free edit origin far from every game slot in {@code mcc_battlebox}. The core diffs it on save; the result is the
 * theme's overlay, layered over every arena copy by {@link ArenaPlacer} (see {@link BbOverlay}).
 */
final class BbEdit implements McmMapEditable {
    private static final int EDIT_X0 = 300_000, EDIT_STEP = 1_000;
    private static final long EDIT_SEED = 0xBB0EDL;

    private record State(int slot, Grid grid, ArenaTheme theme) {}

    private final BattleBoxPlugin plugin;
    private final BattleBoxGame game;
    private final Set<Integer> used = new HashSet<>();

    BbEdit(BattleBoxPlugin plugin, BattleBoxGame game) { this.plugin = plugin; this.game = game; }

    private static ArenaTheme find(String id) {
        for (ArenaTheme t : Themes.all()) if (t.id().equalsIgnoreCase(id)) return t;
        return null;
    }

    @Override
    public List<String> editableMaps() {
        List<String> out = new ArrayList<>();
        for (ArenaTheme t : Themes.all()) out.add(t.id());
        return out;
    }

    @Override public String editableMapName(String id) { ArenaTheme t = find(id); return t == null ? id : t.displayName(); }

    @Override public Material editableMapIcon(String id) { return Material.YELLOW_CONCRETE; }

    @Override
    public String editBlockedReason(String id) {
        if (find(id) == null) return "unknown arena";
        if (game.ensureWorld() == null) return "the Battle Box world is missing";
        return null;
    }

    @Override
    public void buildForEdit(String mapId, Consumer<McmEditSpace> done) {
        ArenaTheme t = find(mapId);
        World w = game.ensureWorld();
        if (t == null || w == null) { done.accept(null); return; }
        ArenaGeo geo = new ArenaGeo();
        final Grid g;
        try {
            g = t.generate(geo, EDIT_SEED, "red", "blue", "white_concrete");
        } catch (Throwable e) {
            plugin.getLogger().severe("Battle Box Map Edit: " + mapId + " failed to generate: " + e);
            done.accept(null);
            return;
        }
        int slot = 0;
        while (used.contains(slot)) slot++;
        used.add(slot);
        final int s = slot;
        final int ox = EDIT_X0 + s * EDIT_STEP, oy = BattleBoxGame.ARENA_Y;
        McmBuild.BuildQueue q = new McmBuild.BuildQueue(plugin);
        ArenaPlacer.enqueue(q, w, ox, oy, 0, g, null, plugin.getLogger(), null);
        q.then(() -> done.accept(space(t, geo, g, w, s, ox, oy))).start();
    }

    private McmEditSpace space(ArenaTheme t, ArenaGeo geo, Grid g, World w, int slot, int ox, int oy) {
        int[] box = BbOverlay.box(t, geo);
        double[] pv = t.previewLocation(geo);
        Location spawn = new Location(w, ox + pv[0], oy + pv[1], pv[2], (float) pv[3], (float) pv[4]);
        McmEditSpace sp = new McmEditSpace(w, ox, oy, 0, box[0], box[1], box[2], box[3], box[4], box[5], spawn);
        sp.anchor("overview", loc(w, ox, oy, t.overview(geo, false, 1)));
        sp.anchor("overview_b", loc(w, ox, oy, t.overview(geo, true, 1)));
        sp.anchor("centre", new Location(w, ox + t.centreX() + 0.5, oy + t.centreY(geo) + 3, t.centreZ() + 0.5, 0f, 30f));
        sp.anchor("spawn_a", loc(w, ox, oy, t.spawn(geo, false, 0)));
        sp.anchor("spawn_b", loc(w, ox, oy, t.spawn(geo, true, 0)));
        sp.note("Shown like a game round (red vs blue, gates closed). Edits are saved for every arena copy of " + t.displayName() + ".");
        sp.note("The game rebuilds the team gates (glass), the centre pad (" + t.centreLabel() + "), the spawn pads / kit rooms every round: edits to those cells are ignored.");
        sp.note("Blocks more than " + BbOverlay.MARGIN + " blocks outside the arena box are not tracked.");
        sp.token = new State(slot, g, t);
        return sp;
    }

    private static Location loc(World w, int ox, int oy, double[] p) {
        return new Location(w, ox + p[0], oy + p[1], p[2], (float) p[3], p.length > 4 ? (float) p[4] : 0f);
    }

    @Override
    public void overlayChanged(String mapId) {
        BbOverlay.invalidate(mapId);
    }

    @Override
    public List<String> editWarnings(String mapId, McmEditSpace space) {
        List<String> out = new ArrayList<>();
        if (!(space.token instanceof State st)) return out;
        Map<String, Integer> diff = new LinkedHashMap<>();
        Map<String, BlockData> parsed = new java.util.HashMap<>();
        for (ArenaTheme.Zone z : st.theme.managedZones(new ArenaGeo())) {
            for (int x = z.x1(); x <= z.x2(); x++)
                for (int y = z.y1(); y <= z.y2(); y++)
                    for (int zz = z.z1(); zz <= z.z2(); zz++) {
                        String want = st.grid.get(x, y, zz);
                        BlockData wd = parsed.computeIfAbsent(want, s -> {
                            try { return Bukkit.createBlockData(s); } catch (IllegalArgumentException e) { return Material.AIR.createBlockData(); }
                        });
                        BlockData have = space.world.getBlockAt(space.originX + x, space.originY + y, space.originZ + zz).getBlockData();
                        if (!have.equals(wd)) diff.merge(z.label(), 1, Integer::sum);
                    }
        }
        for (Map.Entry<String, Integer> e : diff.entrySet())
            out.add(e.getValue() + " block(s) changed in the " + e.getKey() + ": the game rebuilds it every round, so those edits are ignored.");
        return out;
    }

    @Override
    public void editClosed(String mapId, McmEditSpace space) {
        if (space == null) return;
        if (space.token instanceof State st) used.remove(st.slot);
        McmBuild.BuildQueue q = new McmBuild.BuildQueue(plugin);
        q.region(space.world, space.originX + space.minX, space.originY + space.minY, space.originZ + space.minZ,
                space.originX + space.maxX, space.originY + space.maxY, space.originZ + space.maxZ,
                (x, y, z) -> Material.AIR.createBlockData(), true);
        q.start();
    }
}
