package com.mcm.games.tgttos;

import com.mcm.api.McmEditSpace;
import com.mcm.api.McmMapEditable;
import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.TgMapData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Core {@code /mcm mapedit} hook (Map Edit core; kept in its own class so the plugin still loads on an older core).
 * Edit sessions paste the map as played (fixed seed, first option of every variant group, no overlay) into a fresh
 * slot far away from every game slot, in the normal game world; the core diffs it on save and the result is layered
 * over the map by {@link Overlays} at every later build.
 */
final class TgttosEdit implements McmMapEditable {
    /** Edit slots start here (game slots are 0..n), a new one per session so the edit area is always pristine. */
    private static final int EDIT_SLOT_BASE = 1000;
    private static int editCounter;
    private static final int MARGIN = 32;

    private final TgttosPlugin plugin;

    TgttosEdit(TgttosPlugin plugin) { this.plugin = plugin; }

    @Override
    public List<String> editableMaps() {
        List<String> out = new ArrayList<>();
        MapRegistry r = plugin.registry();
        if (r != null) for (MapRegistry.Entry e : r.all()) if (!e.stub) out.add(e.map.id());
        return out;
    }

    @Override
    public String editableMapName(String id) {
        MapRegistry.Entry e = plugin.registry().byId(id);
        return e == null ? id : e.map.displayName();
    }

    @Override
    public Material editableMapIcon(String id) { return Material.FEATHER; }

    @Override
    public String editBlockedReason(String id) {
        if (!plugin.registry().ready()) return "the maps are still being generated, try again in a moment";
        MapRegistry.Entry e = plugin.registry().byId(id);
        if (e == null) return "unknown map";
        if (e.broken) return "the map is not playable (see /tg info " + id + ")";
        if (plugin.world() == null) return "the TGTTOS world is missing";
        return null;
    }

    @Override
    public void buildForEdit(String mapId, Consumer<McmEditSpace> done) {
        MapRegistry.Entry e = plugin.registry().byId(mapId);
        World w = plugin.world();
        if (e == null || w == null) { done.accept(null); return; }
        Settings s = Settings.load(plugin.getConfig());
        plugin.placer().baseY = s.baseY;
        plugin.placer().spacing = s.spacing;
        final int slot = EDIT_SLOT_BASE + (editCounter++);
        plugin.worker().execute(() -> {
            TgMapData d;
            try { d = e.map.build(e.fixedSeed()); }
            catch (Throwable t) { plugin.getLogger().severe("TGTTOS Map Edit: " + mapId + " failed to build: " + t); d = null; }
            TgMapData data = d;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (data == null || !plugin.isEnabled()) { done.accept(null); return; }
                Map<String, String> choice = new LinkedHashMap<>();
                for (TgMapData.VariantGroup g : data.variants) if (!g.options.isEmpty()) choice.put(g.name, g.options.keySet().iterator().next());
                Paster.Job job = plugin.paster().pasteFresh("edit " + mapId, w, slot, data, choice);
                job.then(() -> {
                    if (job.failed || job.cancelled) { plugin.paster().forgetSlot(slot); done.accept(null); return; }
                    done.accept(space(e, data, w, slot));
                });
            });
        });
    }

    private McmEditSpace space(MapRegistry.Entry e, TgMapData d, World w, int slot) {
        MapPlacer pl = plugin.placer();
        TgMapData.Region bb = d.blockBounds();
        if (bb == null) bb = TgMapData.Region.of(-8, -8, -8, 8, 8, 8);
        TgMapData.Spawn first = !d.overviewPoints.isEmpty() ? d.overviewPoints.get(0) : d.startSpawns.get(0);
        Location spawn = pl.loc(w, slot, first);
        McmEditSpace sp = new McmEditSpace(w, pl.ox(slot), pl.oy(), pl.oz(slot),
                bb.minX() - MARGIN, bb.minY() - 8, bb.minZ() - MARGIN, bb.maxX() + MARGIN, bb.maxY() + 40, bb.maxZ() + MARGIN, spawn);
        if (!d.startSpawns.isEmpty()) sp.anchor("start", pl.loc(w, slot, d.startSpawns.get(0)));
        if (d.pen != null) sp.anchor("goal", pl.loc(w, slot, d.pen.centerX(), d.pen.minY() + 2, d.pen.centerZ()));
        for (int i = 0; i < d.overviewPoints.size(); i++) sp.anchor("overview" + (i + 1), pl.loc(w, slot, d.overviewPoints.get(i)));
        boolean boat = e.map.kit() == Kit.BOAT;
        for (TgMapData.Spawn s : d.startSpawns) mark(sp, "start spawn", s, boat ? McmEditSpace.Kind.FREE : McmEditSpace.Kind.STAND);
        for (TgMapData.Spawn s : d.chickenSpawns) mark(sp, "chicken spawn", s, McmEditSpace.Kind.FREE);
        sp.note("Shown exactly as played (fixed seed). Start barriers are real blocks here; the game removes them at GO.");
        sp.note("Chickens, decoys, hazards and moving features are the game's own and are not part of the saved blocks.");
        if (!d.variants.isEmpty())
            sp.note("Gates with variants show their first option; the game rolls them each round, but your edits at those cells always win.");
        sp.note("Start spawns, the chicken pen and spawn headroom are checked when you save.");
        sp.token = slot;
        return sp;
    }

    private static void mark(McmEditSpace sp, String label, TgMapData.Spawn s, McmEditSpace.Kind kind) {
        int x = (int) Math.floor(s.x()), z = (int) Math.floor(s.z());
        double fy = Math.floor(s.y());
        if (s.y() == fy) sp.marker(label, x, (int) fy, z, kind);
        else sp.marker(label + " (headroom)", x, (int) fy + 1, z, McmEditSpace.Kind.FREE);
    }

    @Override
    public void overlayChanged(String mapId) {
        MapRegistry.Entry e = plugin.registry().byId(mapId);
        if (e == null) return;
        plugin.worker().execute(() -> {
            TgMapData d;
            try { d = Overlays.apply(e.map.id(), e.map.build(e.fixedSeed())); }
            catch (Throwable t) { plugin.getLogger().warning("TGTTOS: could not rebuild " + mapId + " after a Map Edit change: " + t); return; }
            TgMapData data = d;
            Bukkit.getScheduler().runTask(plugin, () -> {
                e.cached = data;
                plugin.getLogger().info("TGTTOS: map " + mapId + " now uses its Map Edit overlay (" + data.blocks.size() + " blocks)");
            });
        });
    }

    @Override
    public void editClosed(String mapId, McmEditSpace space) {
        if (space != null && space.token instanceof Integer slot) plugin.paster().forgetSlot(slot);
    }
}
