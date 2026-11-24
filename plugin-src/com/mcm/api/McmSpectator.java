package com.mcm.api;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;

/**
 * Small shared helpers for the owner rule "a player who is out or finished while the game still runs is in
 * SPECTATOR mode at an overview point of the arena and kept inside it" (core 3.2.0+). Optional for game plugins:
 * the core uses it for its built-in games and for default late joiners.
 */
public final class McmSpectator {
    private McmSpectator() {}

    /** Where a spectator must stay: the world, a box inside it, and the overview point they are sent back to. */
    public record Zone(World world, BoundingBox box, Location home) {
        /** True if {@code l} is in this zone's world and box. */
        public boolean contains(Location l) {
            return l != null && l.getWorld() != null && world != null && l.getWorld().equals(world)
                    && box.contains(l.getX(), l.getY(), l.getZ());
        }

        /**
         * A zone around {@code home}: {@code horizontal} blocks to each side of {@code centerX/Z} and the world's
         * full build height.
         */
        public static Zone around(Location home, double centerX, double centerZ, double horizontal) {
            World w = home.getWorld();
            int minY = w != null ? w.getMinHeight() - 16 : -80;
            int maxY = w != null ? w.getMaxHeight() + 64 : 384;
            return new Zone(w, new BoundingBox(centerX - horizontal, minY, centerZ - horizontal,
                    centerX + horizontal, maxY, centerZ + horizontal), home);
        }
    }

    /**
     * Puts a player in spectator mode at {@code at} (null = where they are): closes menus and clears inventory,
     * cursor, potion effects, fire, fall distance, velocity and vehicle first.
     */
    public static void makeSpectator(Player p, Location at) {
        if (p == null || !p.isOnline()) return;
        p.closeInventory();
        p.getInventory().clear();
        p.setItemOnCursor(null);
        for (PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(pe.getType());
        p.setFireTicks(0);
        p.setFallDistance(0f);
        if (p.isInsideVehicle()) p.leaveVehicle();
        p.setVelocity(new Vector());
        p.setGameMode(GameMode.SPECTATOR);
        if (at != null) p.teleport(at);
    }
}
