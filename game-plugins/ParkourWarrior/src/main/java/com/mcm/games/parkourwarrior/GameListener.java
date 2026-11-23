package com.mcm.games.parkourwarrior;

import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Rules in the Parkour Warrior world: no damage, hunger, drops, item moves or block edits. */
final class GameListener implements Listener {
    private final ParkourWarriorGame game;

    GameListener(ParkourWarriorGame game) { this.game = game; }

    private boolean inWorld(World w) { return w != null && w == game.course.world(); }

    private boolean inWorld(Entity e) { return e != null && inWorld(e.getWorld()); }

    /** Admins in creative outside a game may edit the world. */
    private boolean editor(Player p) { return p.getGameMode() == GameMode.CREATIVE && game.run(p) == null; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) { if (inWorld(e.getEntity())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onCombust(EntityCombustEvent e) { if (inWorld(e.getEntity()) && e.getEntity() instanceof Player) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (inWorld(e.getEntity()) && e.getFoodLevel() < e.getEntity().getFoodLevel()) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) { if (inWorld(e.getBlock().getWorld()) && !editor(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) { if (inWorld(e.getBlock().getWorld()) && !editor(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onHanging(HangingBreakEvent e) { if (inWorld(e.getEntity())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) { if (inWorld(e.getPlayer()) && !editor(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) { if (inWorld(e.getEntity())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) { if (inWorld(e.getPlayer()) && !editor(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && inWorld(p) && !editor(p)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p && inWorld(p) && !editor(p)) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) { if (inWorld(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) { if (inWorld(e.getPlayer()) && !editor(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!inWorld(p) || editor(p)) return;
        // no trapdoor/door/button toggling, no pressure plates, no item use (eyes, etc.)
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        String id = game.itemId(e.getItem());
        if (id != null) game.onItemUse(p, id);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFlight(PlayerToggleFlightEvent e) {
        if (e.isFlying() && game.isRunner(e.getPlayer())) { e.setCancelled(true); e.getPlayer().setAllowFlight(false); }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { game.onQuit(e.getPlayer()); }
}
