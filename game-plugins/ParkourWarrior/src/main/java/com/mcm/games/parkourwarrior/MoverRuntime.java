package com.mcm.games.parkourwarrior;

import com.mcm.games.parkourwarrior.CourseModel.BlockPos;
import com.mcm.games.parkourwarrior.CourseModel.Belt;
import com.mcm.games.parkourwarrior.CourseModel.Crumble;
import com.mcm.games.parkourwarrior.CourseModel.MBox;
import com.mcm.games.parkourwarrior.CourseModel.Mover;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Live side of the course's moving parts (port of TGTTOS FeatureRuntime, read-only copy adapted to Parkour Warrior):
 * block-display movers animated on the game clock (the same clock as timed blocks), dropping ledges and conveyor
 * belts. The game asks {@link #hit} for every runner every tick; a hit counts as a fall. Main thread only.
 *
 * <p>Telegraphs (PLAN_v3 rule 6, at least 1 s): pendulums whoosh as they pass the bottom, rotors grind every quarter
 * turn and their arms glow, sliders glow red for the 20 ticks before they close and thud as they move; dropping
 * ledges creak with block particles for their whole delay (>= 12 ticks) before they fall.
 */
final class MoverRuntime {
    static final int EVERY = 2;
    static final String TAG = "pw_mover";

    private final World w;
    private final CourseModel m;
    private final int ox, oy, oz;
    private final Function<String, BlockData> data;
    private final List<List<BlockDisplay>> displays = new ArrayList<>();
    private final double[] lastAngle;
    private final boolean[] lastWarn;
    private final long[] armed, dropped;
    private final Map<Long, Belt> beltAt = new HashMap<>();
    private long t;

    MoverRuntime(World w, CourseModel m, int ox, int oy, int oz, Function<String, BlockData> data) {
        this.w = w; this.m = m; this.ox = ox; this.oy = oy; this.oz = oz; this.data = data;
        lastAngle = new double[m.movers.size()];
        lastWarn = new boolean[m.movers.size()];
        armed = new long[m.crumbles.size()];
        dropped = new long[m.crumbles.size()];
        java.util.Arrays.fill(armed, -1);
        java.util.Arrays.fill(dropped, -1);
        for (Belt b : m.belts) for (BlockPos p : b.cells()) beltAt.put(key(p.x(), p.y(), p.z()), b);
    }

    static long key(int x, int y, int z) { return CourseModel.SparseBlocks.key(x, y, z); }

    boolean isEmpty() { return m.movers.isEmpty() && m.crumbles.isEmpty() && m.belts.isEmpty(); }

    /** Spawn the displays at the pose of tick {@code t0}. */
    void begin(long t0) {
        remove();
        t = t0;
        for (int i = 0; i < m.movers.size(); i++) {
            Mover mv = m.movers.get(i);
            List<BlockDisplay> l = new ArrayList<>();
            Location at = new Location(w, ox + mv.px(), oy + mv.py(), oz + mv.pz(), 0f, 0f);
            for (MBox b : mv.boxes()) {
                BlockData bd = data.apply(b.block());
                BlockDisplay d = w.spawn(at, BlockDisplay.class, e -> {
                    e.addScoreboardTag(TAG);
                    e.addScoreboardTag(CourseWorld.TAG);
                    e.setPersistent(false);
                    e.setBlock(bd);
                    e.setViewRange(2f);
                    e.setInterpolationDelay(0);
                    e.setInterpolationDuration(every(mv));
                    e.setGlowColorOverride(Color.RED);
                });
                d.setTransformation(transform(mv, b, t0));
                l.add(d);
            }
            displays.add(l);
            lastAngle[i] = mv.angle(t0);
        }
    }

    static int every(Mover mv) { return mv.motion() == CourseModel.Motion.SLIDER ? 1 : EVERY; }

    /** One game tick: animate, telegraph, crumble, belts. {@code runners} = active runners in the course world. */
    void tick(long ticks, List<Player> runners) {
        t = ticks;
        for (int i = 0; i < m.movers.size() && i < displays.size(); i++) {
            Mover mv = m.movers.get(i);
            int ev = every(mv);
            if (t % ev != 0) continue;
            List<BlockDisplay> l = displays.get(i);
            double tt = t + ev;   // the client interpolates towards the pose ev ticks ahead
            for (int k = 0; k < l.size(); k++) {
                BlockDisplay d = l.get(k);
                if (!d.isValid()) continue;
                d.setInterpolationDelay(0);
                d.setTransformation(transform(mv, mv.boxes().get(k), tt));
            }
            telegraph(i, mv, runners, l);
        }
        crumbles(runners);
        belts(runners);
    }

    /** The mover a runner moving from a to b (course coordinates, feet) touched this tick, or null. */
    Mover hit(double x0, double y0, double z0, double x1, double y1, double z1) {
        double len = Math.sqrt((x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0) + (z1 - z0) * (z1 - z0));
        int n = Math.max(1, (int) Math.ceil(len / 0.4));
        for (Mover mv : m.movers) {
            double reach = mv.reach() + 2.5 + len;
            double dx = x1 - mv.px(), dy = y1 - mv.py(), dz = z1 - mv.pz();
            if (dx * dx + dy * dy + dz * dz > reach * reach) continue;
            for (int k = 0; k <= n; k++) {
                double u = k / (double) n;
                // the pose the player saw: the display trails the model by up to one update
                if (mv.hitsPlayer(t - 1 + u, x0 + (x1 - x0) * u, y0 + (y1 - y0) * u, z0 + (z1 - z0) * u)) return mv;
            }
        }
        return null;
    }

    private void telegraph(int i, Mover mv, List<Player> near, List<BlockDisplay> l) {
        double a = mv.angle(t);
        switch (mv.motion()) {
            case PENDULUM -> {
                if (Math.signum(a) != Math.signum(lastAngle[i]) && lastAngle[i] != 0) sound(mv.px(), mv.py(), mv.pz(), mv.sound(), near, 1.2f);
            }
            case ROTOR -> {
                if (Math.floor(a / 90) != Math.floor(lastAngle[i] / 90)) sound(mv.px(), mv.py(), mv.pz(), mv.sound(), near, 0.8f);
            }
            case SLIDER -> {
                double toClose = mv.ticksToClose(t);
                boolean warn = toClose > 0 && toClose <= 20;
                if (warn != lastWarn[i]) {
                    lastWarn[i] = warn;
                    for (BlockDisplay d : l) d.setGlowing(warn);
                    if (!warn) sound(mv.px(), mv.py(), mv.pz(), mv.sound(), near, 1.0f);
                }
            }
        }
        lastAngle[i] = a;
    }

    private void crumbles(List<Player> runners) {
        for (int i = 0; i < m.crumbles.size(); i++) {
            Crumble c = m.crumbles.get(i);
            BlockPos c0 = c.cells().get(0);
            if (armed[i] < 0 && dropped[i] < 0) {
                for (Player p : runners) {
                    if (!p.isOnGround() || p.getWorld() != w) continue;
                    if (standsOn(p, c)) {
                        armed[i] = t;
                        sound(c0.x() + 0.5, c0.y() + 0.5, c0.z() + 0.5, "block.wood.break", runners, 0.6f);
                        break;
                    }
                }
            } else if (armed[i] >= 0) {
                if ((t - armed[i]) % 3 == 0)
                    for (BlockPos p : c.cells())
                        w.spawnParticle(Particle.BLOCK, ox + p.x() + 0.5, oy + p.y() + 1.0, oz + p.z() + 0.5, 3, 0.3, 0.05, 0.3, data.apply(c.state()));
                if (t - armed[i] >= c.delayTicks()) {
                    armed[i] = -1;
                    dropped[i] = t;
                    for (BlockPos p : c.cells()) w.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setType(Material.AIR, false);
                    sound(c0.x() + 0.5, c0.y() + 0.5, c0.z() + 0.5, "block.scaffolding.break", runners, 0.8f);
                }
            } else if (t - dropped[i] >= c.restoreTicks()) {
                boolean busy = false;
                for (BlockPos p : c.cells()) {
                    BoundingBox bb = new BoundingBox(ox + p.x(), oy + p.y(), oz + p.z(), ox + p.x() + 1, oy + p.y() + 1, oz + p.z() + 1);
                    for (Player r : runners) if (r.getWorld() == w && r.getBoundingBox().overlaps(bb)) { busy = true; break; }
                    if (busy) break;
                }
                if (!busy) { restore(c); dropped[i] = -1; }
            }
        }
    }

    private boolean standsOn(Player p, Crumble c) {
        Location l = p.getLocation();
        double x = l.getX() - ox, y = l.getY() - oy, z = l.getZ() - oz;
        for (BlockPos q : c.cells()) {
            if (Math.abs(y - (q.y() + 1)) > 0.55) continue;
            if (x > q.x() - 0.3 && x < q.x() + 1.3 && z > q.z() - 0.3 && z < q.z() + 1.3) return true;
        }
        return false;
    }

    private void restore(Crumble c) {
        BlockData d = data.apply(c.state());
        for (BlockPos p : c.cells()) w.getBlockAt(ox + p.x(), oy + p.y(), oz + p.z()).setBlockData(d, false);
    }

    /**
     * Belts: every {@link #BELT_EVERY} ticks a runner standing on a belt is shifted by the belt's push for those ticks
     * with a RELATIVE teleport (x/y/z/yaw/pitch relative), which keeps the client's own velocity: a jump that starts on
     * the belt is never cancelled, unlike a velocity packet. Phys adds the same push per tick to the position.
     */
    static final int BELT_EVERY = 4;

    private void belts(List<Player> runners) {
        if (beltAt.isEmpty() || t % BELT_EVERY != 0) return;
        for (Player p : runners) {
            if (!p.isOnGround() || p.getWorld() != w) continue;
            Location l = p.getLocation();
            double x = l.getX() - ox, y = l.getY() - oy, z = l.getZ() - oz;
            Belt b = beltAt.get(key((int) Math.floor(x), (int) Math.floor(y - 0.2), (int) Math.floor(z)));
            if (b == null) continue;
            Location to = l.clone().add(b.vx() * BELT_EVERY, 0, b.vz() * BELT_EVERY);
            p.teleport(to, org.bukkit.event.player.PlayerTeleportEvent.TeleportCause.PLUGIN,
                    io.papermc.paper.entity.TeleportFlag.Relative.X, io.papermc.paper.entity.TeleportFlag.Relative.Y,
                    io.papermc.paper.entity.TeleportFlag.Relative.Z, io.papermc.paper.entity.TeleportFlag.Relative.YAW,
                    io.papermc.paper.entity.TeleportFlag.Relative.PITCH);
        }
    }

    /** Remove displays and put every dropped ledge back. */
    void stop() {
        remove();
        for (int i = 0; i < m.crumbles.size(); i++) { restore(m.crumbles.get(i)); armed[i] = -1; dropped[i] = -1; }
    }

    private void remove() {
        for (List<BlockDisplay> l : displays) for (BlockDisplay d : l) if (d.isValid()) d.remove();
        displays.clear();
        for (Entity e : new ArrayList<>(w.getEntities())) if (e.getScoreboardTags().contains(TAG)) e.remove();
    }

    String status() {
        int n = 0;
        for (List<BlockDisplay> l : displays) for (BlockDisplay d : l) if (d.isValid()) n++;
        int down = 0;
        for (long d : dropped) if (d >= 0) down++;
        return m.movers.size() + " movers (" + n + " displays), " + m.crumbles.size() + " dropping ledges (" + down + " down), "
                + m.belts.size() + " belts, t=" + t;
    }

    private static Transformation transform(Mover mv, MBox b, double t) {
        float[] q = mv.quaternion(t);
        Quaternionf rot = new Quaternionf(q[0], q[1], q[2], q[3]);
        double e = mv.extension(t);
        Vector3f b0 = new Vector3f((float) b.x0(), (float) b.y0(), (float) b.z0());
        rot.transform(b0);
        Vector3f tr = new Vector3f((float) (mv.dx() * e) + b0.x, (float) (mv.dy() * e) + b0.y, (float) (mv.dz() * e) + b0.z);
        Vector3f sc = new Vector3f((float) (b.x1() - b.x0()), (float) (b.y1() - b.y0()), (float) (b.z1() - b.z0()));
        return new Transformation(tr, rot, sc, new Quaternionf());
    }

    private void sound(double lx, double ly, double lz, String key, List<Player> near, float pitch) {
        if (key == null) return;
        Location at = new Location(w, ox + lx, oy + ly, oz + lz);
        for (Player p : near) if (p.getWorld() == w && p.getLocation().distanceSquared(at) < 28 * 28) { w.playSound(at, key, SoundCategory.BLOCKS, 1.2f, pitch); return; }
    }
}
