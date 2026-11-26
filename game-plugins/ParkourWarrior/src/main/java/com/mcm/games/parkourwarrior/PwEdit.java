package com.mcm.games.parkourwarrior;

import com.mcm.api.McmBuild;
import com.mcm.api.McmEditSpace;
import com.mcm.api.McmMapEditable;
import com.mcm.games.parkourwarrior.CourseModel.Obstacle;
import com.mcm.games.parkourwarrior.CourseModel.Pos;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Core {@code /mcm mapedit} hook (Map Edit core, own class so the plugin still loads on an older core). The course is one
 * long map per map id ({@code classic} + every {@code maps/<id>/course.txt}). An edit session generates the course the
 * game would build (same settings and seed, no overlay) and places it, without labels, movers or timed toggling, at an
 * edit offset far from the real course in {@code mcc_parkourwarrior}. Stages and obstacles are teleport anchors
 * ({@code /mcm mapedit tp M1-2}, {@code hub2}, {@code fork}, {@code E-HARD-1}...).
 */
final class PwEdit implements McmMapEditable {
    private static final int EDIT_OFFSET = 100_000;
    private static int counter;
    private static final int MARGIN = 24;

    private final JavaPlugin plugin;
    private final ParkourWarriorGame game;

    PwEdit(JavaPlugin plugin, ParkourWarriorGame game) { this.plugin = plugin; this.game = game; }

    private Settings settings() { return Settings.load(plugin.getConfig(), plugin.getLogger()); }

    @Override
    public List<String> editableMaps() {
        List<String> out = new ArrayList<>();
        out.add(CourseWorld.CLASSIC);
        File[] dirs = game.course.mapsRoot(settings()).listFiles(File::isDirectory);
        if (dirs != null) {
            Arrays.sort(dirs);
            for (File d : dirs) if (new File(d, "course.txt").isFile()) out.add(d.getName().toLowerCase(Locale.ROOT));
        }
        return out;
    }

    @Override public Material editableMapIcon(String id) { return Material.IRON_BOOTS; }

    @Override
    public String editBlockedReason(String id) {
        if (game.running()) return "Parkour Warrior is running";
        if (game.course.busy()) return "the course is being built, try again in a moment";
        return null;
    }

    @Override
    public void buildForEdit(String mapId, Consumer<McmEditSpace> done) {
        Settings st = settings();
        World w = game.course.ensureWorld();
        if (w == null) { done.accept(null); return; }
        final File importDir = CourseWorld.CLASSIC.equals(mapId) ? null : new File(game.course.mapsRoot(st), mapId);
        final int off = EDIT_OFFSET * (1 + counter++);
        final int ox = st.offX + off, oy = st.offY, oz = st.offZ;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            CourseModel m = null;
            try { m = CourseWorld.generate(st.generator, st.seed, importDir, plugin.getLogger(), new String[]{"?"}); }
            catch (Throwable t) { plugin.getLogger().log(java.util.logging.Level.SEVERE, "Parkour Warrior Map Edit: generating " + mapId + " failed", t); }
            final CourseModel model = m;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (model == null || !plugin.isEnabled()) { done.accept(null); return; }
                McmBuild.BuildQueue q = new McmBuild.BuildQueue(plugin, st.blocksPerTick);
                model.blocks.forEach((x, y, z, b) -> q.set(w, x + ox, y + oy, z + oz, game.course.data(b)));
                for (CourseModel.Timed t : model.timed)
                    for (CourseModel.BlockPos p : t.positions()) q.set(w, p.x() + ox, p.y() + oy, p.z() + oz, game.course.data(t.blockState()));
                q.then(() -> { game.course.applySigns(model, w, ox, oy, oz); done.accept(space(model, w, ox, oy, oz)); });
                q.start();
            });
        });
    }

    private McmEditSpace space(CourseModel m, World w, int ox, int oy, int oz) {
        if (m.minX == 0 && m.maxX == 0 && m.minY == 0 && m.maxY == 0) m.recomputeBounds();
        Progress pr = new Progress(m);
        Location start = loc(w, ox, oy, oz, pr.startSpawn(0));
        McmEditSpace sp = new McmEditSpace(w, ox, oy, oz, m.minX - MARGIN, m.minY - 8, m.minZ - MARGIN, m.maxX + MARGIN, m.maxY + 40, m.maxZ + MARGIN, start);
        sp.anchor("start", start);
        try { sp.anchor("fork", loc(w, ox, oy, oz, pr.respawn(Progress.Node.FORK))); } catch (Throwable ignored) { }
        for (CourseModel.Hub h : m.hubs) sp.anchor("hub" + h.stage(), loc(w, ox, oy, oz, h.respawn()));
        for (int i = 0; i < m.overviews.size(); i++) sp.anchor("view" + (i + 1), loc(w, ox, oy, oz, m.overviews.get(i).pos()));
        for (Obstacle o : m.obstacles) if (o.entry() != null) sp.anchor(o.id(), loc(w, ox, oy, oz, o.entry()));
        for (Pos p : m.spawns) mark(sp, "start spawn", p);
        for (CourseModel.Hub h : m.hubs) mark(sp, "hub " + h.stage() + " respawn", h.respawn());
        for (Obstacle o : m.obstacles) if (o.entry() != null) mark(sp, o.id() + " entry", o.entry());
        sp.note("One long map: jump around with /mcm mapedit tp <place> (start, fork, hub1..5, view1.., or an obstacle id like M1-2, B2-1, E-HARD-1).");
        sp.note("Labels, moving parts (pendulums, sliders, crumbles, belts) and the timed-block toggling are the game's own and are not shown or saved.");
        sp.note("Spawns, hub respawns and obstacle entries are checked when you save; edits on timed-block cells are warned about.");
        sp.token = new Object[]{m};
        return sp;
    }

    private static Location loc(World w, int ox, int oy, int oz, Pos p) { return new Location(w, p.x() + ox, p.y() + oy, p.z() + oz, p.yaw(), 0f); }

    private static void mark(McmEditSpace sp, String label, Pos p) {
        int x = (int) Math.floor(p.x()), z = (int) Math.floor(p.z());
        double fy = Math.floor(p.y());
        if (p.y() == fy) sp.marker(label, x, (int) fy, z, McmEditSpace.Kind.STAND);
        else sp.marker(label + " (headroom)", x, (int) fy + 1, z, McmEditSpace.Kind.FREE);
    }

    @Override
    public List<String> editWarnings(String mapId, McmEditSpace space) {
        List<String> out = new ArrayList<>();
        if (!(space.token instanceof Object[] t) || !(t[0] instanceof CourseModel m)) return out;
        int changed = 0;
        for (CourseModel.Timed tm : m.timed) {
            org.bukkit.block.data.BlockData want = game.course.data(tm.blockState());
            for (CourseModel.BlockPos p : tm.positions()) {
                if (!space.world.isChunkLoaded((p.x() + space.originX) >> 4, (p.z() + space.originZ) >> 4)) continue;
                if (!space.world.getBlockAt(p.x() + space.originX, p.y() + space.originY, p.z() + space.originZ).getBlockData().equals(want)) changed++;
            }
        }
        if (changed > 0) out.add(changed + " timed-block cell(s) differ from the generated course; the game toggles those cells itself, so these edits only show while a timed group is in its 'on' state / get overwritten");
        return out;
    }

    @Override
    public void overlayChanged(String mapId) {
        // the placed course carries the old overlay: rebuild it now if it is this map and nothing is running
        if (game.running() || game.course.busy() || !mapId.equalsIgnoreCase(game.course.mapId())) return;
        Settings st = settings();
        game.course.prepare(st, mapId, true, () -> {
            game.course.removeLabels();
            plugin.getLogger().info("Parkour Warrior: course '" + mapId + "' rebuilt with its Map Edit overlay");
        });
    }
}
