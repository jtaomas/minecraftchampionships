package com.mcm.games.race;

import com.mcm.games.race.model.MapFeatures;
import com.mcm.games.race.model.TgMapData;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Crash rule for elytra maps (owner, feedback 3: "they need to go through the holes, otherwise they die"). No real
 * damage is dealt: a crash sends the racer back to the last checkpoint like a hazard. Checked every tick from the
 * measured movement (position deltas), so it does not depend on vanilla's fly-into-wall speed threshold:
 * <ul>
 *   <li><b>wall slam</b>: gliding last tick at >= {@code elytra.crash-min-speed} blocks/tick horizontally, this tick
 *       less than {@code elytra.crash-keep} of that speed is left, and a solid block is right in front of the racer
 *       in the old flight direction (a slow face-plant into a screen counts: 0.25 b/t = 5 b/s);</li>
 *   <li><b>hard landing</b>: gliding and falling faster than {@code elytra.hard-landing-speed} b/t last tick, on the
 *       ground now (a nose dive into the meadow or a ledge);</li>
 *   <li><b>hard fall</b>: on the ground after a fall of {@code elytra.hard-fall-blocks} or more (vanilla fall
 *       distance, not in water);</li>
 *   <li><b>floor</b> (only with {@code elytra.floor-kills: true}, default off): standing on a course floor block
 *       outside the map's safe regions.</li>
 * </ul>
 */
final class CrashCheck {
    private static final class State { double vx, vy, vz; boolean gliding, valid; float fall; }

    private final Map<UUID, State> st = new HashMap<>();

    void forget(UUID id) { st.remove(id); }

    void clear() { st.clear(); }

    /**
     * @param vel   measured movement this tick (world blocks/tick)
     * @param fresh the racer was placed / respawned in the last few ticks (state is reset)
     * @param local local map coordinates of the feet (for the floor rule's safe regions)
     * @return the crash description for chat ("slammed into ..."), or null
     */
    String step(Player p, Vector vel, boolean fresh, Settings s, MapFeatures mf, double[] local) {
        State a = st.computeIfAbsent(p.getUniqueId(), k -> new State());
        boolean gl = p.isGliding();
        @SuppressWarnings("deprecation") boolean ground = p.isOnGround();
        float fd = p.getFallDistance();
        String why = null;
        if (fresh) {
            p.setFallDistance(0f);
            a.valid = false;
        } else if (a.valid) {
            double hPrev = Math.hypot(a.vx, a.vz), hNow = Math.hypot(vel.getX(), vel.getZ());
            boolean water = p.isInWater() || p.getLocation().getBlock().getType() == Material.WATER;
            if (a.gliding && hPrev >= s.crashMinSpeed && hNow < hPrev * s.crashKeep && wallAhead(p.getLocation(), a.vx, a.vz)) {
                why = "slammed into a wall";
            } else if (ground && !water && a.gliding && a.vy <= -s.hardLandingSpeed) {
                why = "nose-dived into the ground";
            } else if (ground && !water && !gl && a.fall >= s.hardFallBlocks) {
                why = "fell " + Math.round(a.fall) + " blocks";
            } else if (s.floorKills && ground && !mf.floorBlocks.isEmpty() && onFloor(p, mf, local)) {
                why = "touched the canyon floor";
            }
        }
        a.vx = vel.getX(); a.vy = vel.getY(); a.vz = vel.getZ();
        a.gliding = gl;
        a.fall = fd;
        a.valid = why == null;
        return why;
    }

    /** Solid (non-passable) block within 1 block ahead of the racer in direction (vx, vz), at body height. */
    private static boolean wallAhead(Location l, double vx, double vz) {
        double h = Math.hypot(vx, vz);
        if (h < 1e-6) return false;
        double ux = vx / h, uz = vz / h;
        World w = l.getWorld();
        for (double k : new double[]{0.35, 0.7, 1.05})
            for (double dy : new double[]{0.1, 0.5, 0.9}) {
                Block b = w.getBlockAt((int) Math.floor(l.getX() + ux * k), (int) Math.floor(l.getY() + dy), (int) Math.floor(l.getZ() + uz * k));
                if (!b.isPassable() && b.getType() != Material.BARRIER) return true;
            }
        return false;
    }

    private static boolean onFloor(Player p, MapFeatures mf, double[] local) {
        Block under = p.getLocation().clone().add(0, -0.2, 0).getBlock();
        if (!mf.floorBlocks.contains(under.getType().getKey().getKey())) return false;
        for (TgMapData.Region r : mf.floorSafe) if (r.contains(local[0], local[1], local[2])) return false;
        return true;
    }
}
