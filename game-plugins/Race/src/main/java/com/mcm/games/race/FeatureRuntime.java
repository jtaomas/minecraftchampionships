package com.mcm.games.race;

import com.mcm.games.race.model.MapFeatures;
import com.mcm.games.race.model.MapFeatures.Box;
import com.mcm.games.race.model.MapFeatures.Mover;
import com.mcm.games.race.model.MapFeatures.Toggle;
import com.mcm.games.race.model.TgMapData;
import com.mcm.games.race.model.TgMapData.Placement;
import com.mcm.games.race.model.TgMapData.Pos;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Live side of {@link MapFeatures} for one round: block-display movers (animated from GO, frozen at their rest pose
 * before, hit-tested by the game every tick) and timed block toggles (placed without physics, never into an occupied
 * cell, with indicator blocks that warn before a change). {@link #stop} removes every display and restores every
 * toggled / barrier cell to the generator's frame-0 state. Main thread only. Cost: one transformation update per
 * display every {@link #EVERY} ticks, a distance pre-check per mover and racer, block writes only on frame changes.
 */
final class FeatureRuntime {
    static final int EVERY = 2;                 // display update interval (ticks); interpolation over the same time
    private static final float GLOW_RANGE = 3f; // view range multiplier (x 64 blocks)

    private final World w;
    private final MapFeatures f;
    private final MapPlacer placer;
    private final int ox, oy, oz;
    private final TgMapData data;
    private final List<List<BlockDisplay>> displays = new ArrayList<>();
    private final List<Set<Long>> barrier = new ArrayList<>();
    private final int[] frame;
    private final boolean[] warned;
    private final List<List<Placement>> pending = new ArrayList<>();
    private final double[] lastAngle;
    private final boolean[] lastClosedWarn;
    private final List<Entity> entities;
    private long t;
    /** Traps: tick the trap was set off (-1 idle), tick it dropped (-1 standing). */
    private final long[] trapArmed, trapDropped;
    private final List<FallingBlock> falling = new ArrayList<>();
    private final List<Long> fallingAt = new ArrayList<>();

    FeatureRuntime(World w, int slot, MapFeatures f, MapPlacer placer, TgMapData data, List<Entity> entities) {
        this.w = w; this.f = f; this.placer = placer; this.data = data; this.entities = entities;
        ox = placer.ox(slot); oy = placer.oy(); oz = placer.oz(slot);
        frame = new int[f.toggles.size()];
        warned = new boolean[f.toggles.size()];
        lastAngle = new double[f.movers.size()];
        lastClosedWarn = new boolean[f.movers.size()];
        for (int i = 0; i < f.toggles.size(); i++) pending.add(new ArrayList<>());
        trapArmed = new long[f.traps.size()];
        trapDropped = new long[f.traps.size()];
        java.util.Arrays.fill(trapArmed, -1);
        java.util.Arrays.fill(trapDropped, -1);
    }

    MapFeatures features() { return f; }

    /** Pre-round: displays at their rest pose (t = 0), toggles at frame 0, indicators off. */
    void begin() {
        for (int i = 0; i < f.movers.size(); i++) {
            Mover m = f.movers.get(i);
            List<BlockDisplay> l = new ArrayList<>();
            Location at = new Location(w, ox + m.px(), oy + m.py(), oz + m.pz(), 0f, 0f);
            for (Box b : m.boxes()) {
                BlockData bd = placer.data(b.block());
                BlockDisplay d = w.spawn(at, BlockDisplay.class, e -> {
                    e.addScoreboardTag(RaceGame.ENT_TAG);
                    e.setPersistent(false);
                    e.setBlock(bd);
                    e.setViewRange(GLOW_RANGE);
                    e.setInterpolationDelay(0);
                    e.setInterpolationDuration(EVERY);
                    e.setGlowColorOverride(Color.RED);
                });
                d.setTransformation(transform(m, b, 0));
                l.add(d);
                entities.add(d);
            }
            displays.add(l);
            barrier.add(new HashSet<>());
            lastAngle[i] = m.angle(0);
            if (m.solid()) placeBarrier(i, 0);
        }
        for (int i = 0; i < f.toggles.size(); i++) { frame[i] = 0; setIndicators(f.toggles.get(i), false); }
    }

    /** One tick of the running phase; {@code ticks} = ticks since GO; {@code near} = racer locations (sounds). */
    void tick(long ticks, List<Location> near, java.util.function.Predicate<BoundingBox> occupied) {
        t = ticks;
        if (t % EVERY == 0) {
            for (int i = 0; i < f.movers.size(); i++) {
                Mover m = f.movers.get(i);
                List<BlockDisplay> l = displays.get(i);
                double tt = t + EVERY;                      // the client interpolates towards the pose EVERY ticks ahead
                for (int k = 0; k < l.size(); k++) {
                    BlockDisplay d = l.get(k);
                    if (!d.isValid()) continue;
                    d.setInterpolationDelay(0);
                    d.setTransformation(transform(m, m.boxes().get(k), tt));
                }
                telegraph(i, m, near, l);
                if (m.solid()) placeBarrier(i, t);
            }
        }
        for (int i = 0; i < f.toggles.size(); i++) {
            Toggle g = f.toggles.get(i);
            int fr = g.frameAt(t);
            if (fr != frame[i]) {
                frame[i] = fr;
                pending.get(i).clear();
                pending.get(i).addAll(g.frames().get(fr).blocks());
                Pos c = g.indicators().isEmpty() ? (g.frames().get(fr).blocks().isEmpty() ? null : toPos(g.frames().get(fr).blocks().get(0))) : g.indicators().get(0);
                if (c != null && g.sound() != null) sound(c.x() + 0.5, c.y() + 0.5, c.z() + 0.5, g.sound(), near, 1.0f);
            }
            if (!pending.get(i).isEmpty()) apply(pending.get(i), occupied);
            boolean warn = g.warning(t);
            if (warn != warned[i]) { warned[i] = warn; setIndicators(g, warn); }
        }
        traps(near, occupied);
    }

    /**
     * Traps (Ticket Gates couplings): a racer's feet in the trigger sets it off - a creak (anvil land sound, low pitch)
     * and a few dust particles; delayTicks later the cells turn to air and the block falls (visual only, removed before
     * it lands); restoreTicks after that the generator blocks come back once nobody stands in them.
     */
    private void traps(List<Location> near, java.util.function.Predicate<BoundingBox> occupied) {
        for (int i = 0; i < f.traps.size(); i++) {
            MapFeatures.Trap tr = f.traps.get(i);
            Pos c = tr.gone().get(0);
            if (trapArmed[i] < 0 && trapDropped[i] < 0) {
                for (Location p : near) {
                    if (p.getWorld() != w) continue;
                    if (tr.trigger().contains(p.getX() - ox, p.getY() - oy, p.getZ() - oz)) {
                        trapArmed[i] = t;
                        sound(c.x() + 0.5, c.y() + 0.5, c.z() + 0.5, tr.warnSound(), near, 0.6f);
                        w.spawnParticle(org.bukkit.Particle.BLOCK, ox + c.x() + 0.5, oy + c.y() + 1.0, oz + c.z() + 0.5, 12, 0.3, 0.05, 0.3,
                                placer.data(data.blocks.get(c.x(), c.y(), c.z()) == null ? "anvil" : data.blocks.get(c.x(), c.y(), c.z())));
                        break;
                    }
                }
            } else if (trapArmed[i] >= 0 && t - trapArmed[i] >= tr.delayTicks()) {
                trapArmed[i] = -1;
                trapDropped[i] = t;
                for (Pos p : tr.gone()) w.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setType(Material.AIR, false);
                if (tr.falling() != null) {
                    @SuppressWarnings("deprecation")
                    FallingBlock fb = w.spawnFallingBlock(new Location(w, ox + c.x() + 0.5, oy + c.y(), oz + c.z() + 0.5), placer.data(tr.falling()));
                    fb.addScoreboardTag(RaceGame.ENT_TAG);
                    fb.setPersistent(false);
                    fb.setDropItem(false);
                    fb.setCancelDrop(true);
                    fb.setHurtEntities(false);
                    falling.add(fb);
                    fallingAt.add(t);
                    entities.add(fb);
                }
                sound(c.x() + 0.5, c.y() + 0.5, c.z() + 0.5, tr.dropSound(), near, 1.0f);
            } else if (trapDropped[i] >= 0 && t - trapDropped[i] >= tr.restoreTicks()) {
                boolean busy = false;
                for (Pos p : tr.gone()) {
                    int wx = ox + p.x(), wy = oy + p.y(), wz = oz + p.z();
                    if (occupied.test(new BoundingBox(wx, wy, wz, wx + 1, wy + 2, wz + 1))) { busy = true; break; }
                }
                if (!busy) { restoreTrap(tr); trapDropped[i] = -1; }
            }
        }
        for (int k = falling.size() - 1; k >= 0; k--)
            if (t - fallingAt.get(k) >= 8 || !falling.get(k).isValid()) {
                if (falling.get(k).isValid()) falling.get(k).remove();
                falling.remove(k); fallingAt.remove(k);
            }
    }

    private void restoreTrap(MapFeatures.Trap tr) {
        for (Pos p : tr.gone()) {
            String b = data.blocks.get(p.x(), p.y(), p.z());
            w.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setBlockData(placer.data(b == null ? "air" : b), false);
        }
    }

    /** Is a mover hit by a sphere moving from (x0,y0,z0) to (x1,y1,z1) (local coordinates) this tick? Returns it or null. */
    Mover hit(double x0, double y0, double z0, double x1, double y1, double z1, double r) {
        double len = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        int n = Math.max(1, (int) Math.ceil(len / 0.4));
        for (Mover m : f.movers) {
            double reach = m.reach() + r + len + 1;
            double dx = x1 - m.px(), dy = y1 - m.py(), dz = z1 - m.pz();
            if (dx * dx + dy * dy + dz * dz > reach * reach) continue;
            for (int k = 0; k <= n; k++) {
                double u = k / (double) n;
                // the pose the players saw: the display trails the model by up to one update
                if (m.hits(t - 1 + u, x0 + (x1 - x0) * u, y0 + (y1 - y0) * u, z0 + (z1 - z0) * u, r)) return m;
            }
        }
        return null;
    }

    /** Remove displays, restore toggled and barrier cells to the generator's blocks. */
    void stop() {
        for (List<BlockDisplay> l : displays) for (BlockDisplay d : l) if (d.isValid()) d.remove();
        displays.clear();
        for (Set<Long> s : barrier)
            for (long k : s) {
                Block b = w.getBlockAt(TgMapData.SparseBlocks.kx(k), TgMapData.SparseBlocks.ky(k), TgMapData.SparseBlocks.kz(k));
                if (b.getType() == Material.BARRIER) b.setType(Material.AIR, false);
            }
        barrier.clear();
        for (int i = 0; i < f.toggles.size(); i++) {
            Toggle g = f.toggles.get(i);
            for (Placement p : g.frames().get(0).blocks()) set(p);
            setIndicators(g, false);
        }
        for (FallingBlock fb : falling) if (fb.isValid()) fb.remove();
        falling.clear(); fallingAt.clear();
        for (int i = 0; i < f.traps.size(); i++) { restoreTrap(f.traps.get(i)); trapArmed[i] = -1; trapDropped[i] = -1; }
    }

    // ------------------------------------------------------------------ internals

    private static Pos toPos(Placement p) { return new Pos(p.x(), p.y(), p.z()); }

    private static Transformation transform(Mover m, Box b, double t) {
        float[] q = m.quaternion(t);
        Quaternionf rot = new Quaternionf(q[0], q[1], q[2], q[3]);
        double e = m.extension(t);
        Vector3f b0 = new Vector3f((float) b.x0(), (float) b.y0(), (float) b.z0());
        rot.transform(b0);
        Vector3f tr = new Vector3f((float) (m.dx() * e) + b0.x, (float) (m.dy() * e) + b0.y, (float) (m.dz() * e) + b0.z);
        Vector3f sc = new Vector3f((float) (b.x1() - b.x0()), (float) (b.y1() - b.y0()), (float) (b.z1() - b.z0()));
        return new Transformation(tr, rot, sc, new Quaternionf());
    }

    /**
     * Telegraphs: pendulum - a whoosh as it passes the bottom; rotor - a grind every quarter turn; slider - its parts
     * glow red for the last second before it extends, a piston sound as it moves. Sounds only reach racers within 28 blocks.
     */
    private void telegraph(int i, Mover m, List<Location> near, List<BlockDisplay> l) {
        double a = m.angle(t);
        switch (m.motion()) {
            case PENDULUM -> {
                if (Math.signum(a) != Math.signum(lastAngle[i]) && lastAngle[i] != 0) sound(m.px(), m.py(), m.pz(), m.sound(), near, 1.2f);
            }
            case ROTOR -> {
                if (Math.floor(a / 90) != Math.floor(lastAngle[i] / 90)) sound(m.px(), m.py(), m.pz(), m.sound(), near, 0.8f);
            }
            case SLIDER -> {
                double toClose = m.ticksToClose(t);
                boolean warn = toClose > 0 && toClose <= 20;
                if (warn != lastClosedWarn[i]) {
                    lastClosedWarn[i] = warn;
                    for (BlockDisplay d : l) d.setGlowing(warn);
                    if (!warn) sound(m.px(), m.py(), m.pz(), m.sound(), near, 1.0f);
                }
            }
        }
        lastAngle[i] = a;
    }

    private void sound(double lx, double ly, double lz, String key, List<Location> near, float pitch) {
        if (key == null) return;
        Location at = new Location(w, ox + lx, oy + ly, oz + lz);
        for (Location p : near) if (p.getWorld() == w && p.distanceSquared(at) < 28 * 28) { w.playSound(at, key, SoundCategory.BLOCKS, 1.2f, pitch); return; }
    }

    private void apply(List<Placement> list, java.util.function.Predicate<BoundingBox> occupied) {
        for (var it = list.iterator(); it.hasNext(); ) {
            Placement p = it.next();
            BlockData bd = placer.data(p.block());
            boolean solid = bd.getMaterial().isSolid();
            int wx = ox + p.x(), wy = oy + p.y(), wz = oz + p.z();
            if (solid && occupied.test(new BoundingBox(wx, wy, wz, wx + 1, wy + 1, wz + 1))) continue;   // retry next tick
            w.getBlockAt(wx, wy, wz).setBlockData(bd, false);
            it.remove();
        }
    }

    private void set(Placement p) {
        w.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setBlockData(placer.data(p.block()), false);
    }

    private void setIndicators(Toggle g, boolean warn) {
        for (Pos p : g.indicators()) {
            String b = warn ? g.warnBlock() : data.blocks.get(p.x(), p.y(), p.z());
            w.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setBlockData(placer.data(b), false);
        }
    }

    /** Solid movers: invisible barrier blocks in the cells their hazard boxes cover now (air cells only). */
    private void placeBarrier(int i, double t) {
        Mover m = f.movers.get(i);
        Set<Long> now = new HashSet<>();
        for (Pos p : m.cells(t)) now.add(TgMapData.SparseBlocks.key(ox + p.x(), oy + p.y(), oz + p.z()));
        Set<Long> old = barrier.get(i);
        for (long k : old)
            if (!now.contains(k)) {
                Block b = w.getBlockAt(TgMapData.SparseBlocks.kx(k), TgMapData.SparseBlocks.ky(k), TgMapData.SparseBlocks.kz(k));
                if (b.getType() == Material.BARRIER) b.setType(Material.AIR, false);
            }
        Set<Long> kept = new HashSet<>();
        for (long k : now) {
            Block b = w.getBlockAt(TgMapData.SparseBlocks.kx(k), TgMapData.SparseBlocks.ky(k), TgMapData.SparseBlocks.kz(k));
            if (b.getType().isAir()) b.setType(Material.BARRIER, false);
            if (b.getType() == Material.BARRIER) kept.add(k);
        }
        barrier.set(i, kept);
    }

    /** For /race info. */
    List<String> status() {
        List<String> out = new ArrayList<>();
        Map<String, Integer> n = new HashMap<>();
        for (List<BlockDisplay> l : displays) for (BlockDisplay d : l) n.merge(d.isValid() ? "valid" : "gone", 1, Integer::sum);
        out.add("§7Features: §f" + f.movers.size() + " movers §8(" + n + " displays)§7, " + f.toggles.size() + " toggles, "
                + f.gates.size() + " bonus gates, " + f.checkpoints.size() + " checkpoints, " + f.refills.size() + " refills, " + f.depots.size() + " depots, " + f.traps.size() + " traps; t=" + t);
        for (int i = 0; i < f.traps.size(); i++)
            out.add("§7  trap " + f.traps.get(i).name() + ": " + (trapArmed[i] >= 0 ? "§ecreaking" : trapDropped[i] >= 0 ? "§cdropped" : "§astanding"));
        return out;
    }
}
