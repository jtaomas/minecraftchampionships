package com.mcm.games.sandsoftime;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.function.Function;

/**
 * Owner rule (LESSONS.md): a player who is out or finished while the game is still running is in SPECTATOR mode,
 * sent to an overview point of the arena and kept inside it. This listener does the "kept inside" part: a
 * spectator who flies out of their zone is put back on the overview point, and the spectator hotbar teleport
 * cannot take them out of it (or into another world).
 */
final class SpectatorGuard implements Listener {

    /** Where one spectator must stay: the world, the box inside it, and the overview point they are sent back to. */
    record Zone(World world, BoundingBox box, Location home) {}

    private final Function<Player, Zone> zoneOf;

    /** {@code zoneOf} returns the zone for a spectating player, or null if this game does not manage them right now. */
    SpectatorGuard(Plugin plugin, Function<Player, Zone> zoneOf) {
        this.zoneOf = zoneOf;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /** Puts a player in spectator mode at {@code at} (null = where they are): inventory, effects and fire cleared. */
    static void makeSpectator(Player p, Location at) {
        if (p == null || !p.isOnline()) return;
        p.closeInventory();
        p.getInventory().clear();
        p.setItemOnCursor(null);
        for (PotionEffect pe : new ArrayList<>(p.getActivePotionEffects())) p.removePotionEffect(pe.getType());
        p.setFireTicks(0);
        p.setFallDistance(0f);
        p.setGlowing(false);
        if (p.isInsideVehicle()) p.leaveVehicle();
        p.setVelocity(new Vector());
        p.setGameMode(GameMode.SPECTATOR);
        if (at != null) p.teleport(at);
    }

    private Zone zone(Player p) {
        if (p.getGameMode() != GameMode.SPECTATOR) return null;
        try {
            return zoneOf.apply(p);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static boolean inside(Zone z, Location l) {
        return l != null && l.getWorld() != null && l.getWorld().equals(z.world())
                && z.box().contains(l.getX(), l.getY(), l.getZ());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpectatorMove(PlayerMoveEvent e) {
        Location from = e.getFrom(), to = e.getTo();
        if (to == null || (from.getX() == to.getX() && from.getY() == to.getY() && from.getZ() == to.getZ())) return;
        Player p = e.getPlayer();
        Zone z = zone(p);
        if (z == null || z.home() == null || !z.world().equals(to.getWorld()) || inside(z, to)) return;
        e.setTo(z.home().clone());
        p.sendActionBar(Component.text("You can only watch inside the arena.", NamedTextColor.GRAY));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpectatorTeleport(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        Zone z = zone(e.getPlayer());
        if (z == null || inside(z, e.getTo())) return;
        e.setCancelled(true);
        e.getPlayer().sendActionBar(Component.text("You can only watch players inside this game's arena.", NamedTextColor.GRAY));
    }
}
