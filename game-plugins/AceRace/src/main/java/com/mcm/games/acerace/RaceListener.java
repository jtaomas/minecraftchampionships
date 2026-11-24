package com.mcm.games.acerace;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;

/** World-only guards: no damage, no editing, no item moves, start-pen freeze, spectator confinement. */
public final class RaceListener implements Listener {
    private final AceRacePlugin plugin;
    private final RaceGame game;

    public RaceListener(AceRacePlugin plugin, RaceGame game) {
        this.plugin = plugin;
        this.game = game;
    }

    private boolean active(org.bukkit.World w) {
        return game.isRunning() && game.isRaceWorld(w);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (active(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (active(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (active(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (active(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (active(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (active(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (active(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventory(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && active(p.getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (!active(e.getPlayer().getWorld()) || e.getPlayer().getGameMode() == GameMode.SPECTATOR) return;
        // Riptide: allow the trident's right-click in water/rain; everything else stays locked.
        if (e.getPlayer().getInventory().getItemInMainHand().getType() == org.bukkit.Material.TRIDENT
                && (e.getPlayer().isInWater() || e.getPlayer().getWorld().hasStorm())) {
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!game.isRunning() || !game.isRaceWorld(e.getPlayer().getWorld())) return;
        Player p = e.getPlayer();
        if (game.isPre() && game.isParticipant(p)) {
            e.setTo(e.getFrom());
            return;
        }
        if (p.getGameMode() == GameMode.SPECTATOR) {
            game.confineSpectator(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (!game.isRunning() || !game.isRaceWorld(e.getPlayer().getWorld())) return;
        if (e.getPlayer().getGameMode() == GameMode.SPECTATOR
                && !game.isInsideSpectator(e.getTo())) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        game.onQuit(e.getPlayer());
    }
}
