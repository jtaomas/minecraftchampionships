package com.mcm.games.tgttos;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.projectiles.ProjectileSource;

/** TGTTOS rules. Every handler only acts while a game runs and only in the game world. */
final class GameListener implements Listener {
    private final TgttosGame game;

    GameListener(TgttosGame game) { this.game = game; }

    private boolean in(Entity e) { return e != null && game.inWorld(e.getWorld()); }

    // ------------------------------------------------------------------ damage, knockback, finish

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        Entity victim = e.getEntity();
        if (!in(victim)) return;
        e.setCancelled(true);                       // no damage, ever
        if (victim instanceof Player p) { p.setFireTicks(0); }
        if (!(e instanceof EntityDamageByEntityEvent ev)) return;
        Entity damager = ev.getDamager();

        if (game.isGameChicken(victim)) {
            if (damager instanceof Player p && ev.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && game.isRacing(p))
                game.whack(p, (Chicken) victim);
            return;
        }
        if (!(victim instanceof Player v) || !game.isRacing(v)) return;
        if (damager instanceof Player a) {
            if (ev.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || !game.isRacing(a) || a.equals(v)) return;
            game.punch(a, v);
        } else if (damager instanceof Snowball sb) {
            ProjectileSource src = sb.getShooter();
            Player shooter = src instanceof Player sp ? sp : null;
            if (shooter != null && (!game.isRacing(shooter) || shooter.equals(v))) return;
            game.snowballHit(shooter, v, sb.getVelocity());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent e) {
        if (in(e.getEntity()) && (e.getEntity() instanceof Player || game.isGameChicken(e.getEntity()))) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        if (!in(e.getEntity())) return;
        e.setKeepInventory(true);
        e.getDrops().clear();
        e.setDeathMessage(null);
    }

    // ------------------------------------------------------------------ blocks

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlace(BlockPlaceEvent e) {
        if (!in(e.getPlayer())) return;
        Block b = e.getBlockPlaced();
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE && !game.isParticipant(e.getPlayer())) return;
        if (game.canPlace(e.getPlayer(), b, b.getType(), e.getBlockReplacedState().getType())) {
            e.setCancelled(false);
            e.setBuild(true);
            game.onPlaced(b);
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBreak(BlockBreakEvent e) {
        if (!in(e.getPlayer())) return;
        if (e.getPlayer().getGameMode() == GameMode.CREATIVE && !game.isParticipant(e.getPlayer())) return;
        Block b = e.getBlock();
        if (game.canBreak(e.getPlayer(), b)) {
            e.setCancelled(false);
            e.setDropItems(false);
            e.setExpToDrop(0);
            game.onBroken(b);
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBucket(PlayerBucketEmptyEvent e) { if (in(e.getPlayer())) e.setCancelled(true); }

    @EventHandler public void onFlow(BlockFromToEvent e) { if (game.inWorld(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler public void onFade(BlockFadeEvent e) { if (game.inWorld(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler public void onForm(BlockFormEvent e) { if (game.inWorld(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler public void onSpread(BlockSpreadEvent e) { if (game.inWorld(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler public void onDecay(LeavesDecayEvent e) { if (game.inWorld(e.getBlock().getWorld())) e.setCancelled(true); }
    @EventHandler public void onChange(EntityChangeBlockEvent e) { if (in(e.getEntity())) e.setCancelled(true); }
    @EventHandler public void onHanging(HangingBreakEvent e) { if (in(e.getEntity())) e.setCancelled(true); }

    // ------------------------------------------------------------------ interaction

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!in(p) || !game.isParticipant(p)) return;
        Block b = e.getClickedBlock();
        if (e.getAction() == Action.PHYSICAL) {
            if (b != null && b.getType() == Material.FARMLAND) e.setCancelled(true);
            return;
        }
        // no fireworks / snowballs before GO
        if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK)
                && game.phase() != TgttosGame.Phase.RUNNING && e.getItem() != null
                && (e.getItem().getType() == Material.FIREWORK_ROCKET || e.getItem().getType() == Material.SNOWBALL)) {
            e.setUseItemInHand(Event.Result.DENY);
            e.setCancelled(true);
            return;
        }
        // right-clicks never toggle map blocks (doors, trapdoors, gates, buttons, chests ...); left-clicks untouched
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && b != null && b.getType().isInteractable())
            e.setUseInteractedBlock(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!in(e.getPlayer())) return;
        if (game.isGameChicken(e.getRightClicked())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDrop(PlayerDropItemEvent e) { if (in(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent e) { if (in(e.getEntity())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemSpawn(ItemSpawnEvent e) { if (in(e.getEntity())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDrop(EntityDropItemEvent e) { if (in(e.getEntity())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInvClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && in(p) && game.isParticipant(p) && p.getGameMode() != GameMode.CREATIVE)
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onItemDamage(PlayerItemDamageEvent e) { if (in(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFood(FoodLevelChangeEvent e) {
        if (in(e.getEntity())) { e.setCancelled(true); e.getEntity().setFoodLevel(20); }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(CreatureSpawnEvent e) {
        if (in(e.getEntity()) && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTeleport(PlayerTeleportEvent e) {
        // spectators must not leave the map through the spectate-player menu
        if (in(e.getPlayer()) && game.isParticipant(e.getPlayer()) && e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE
                && (e.getTo() == null || !game.inWorld(e.getTo().getWorld())))
            e.setCancelled(true);
    }

    // ------------------------------------------------------------------ boats

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleExit(VehicleExitEvent e) {
        if (e.getExited() instanceof Player p && in(p) && game.isParticipant(p) && game.phase() == TgttosGame.Phase.PRE)
            e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleEnter(VehicleEnterEvent e) {
        if (!in(e.getVehicle())) return;
        if (e.getEntered() instanceof Player p) {
            if (game.isParticipant(p) && !game.isOwnBoat(p, e.getVehicle())) e.setCancelled(true);
        } else {
            e.setCancelled(true);                   // no chickens in boats
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleDamage(VehicleDamageEvent e) { if (in(e.getVehicle())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onVehicleDestroy(VehicleDestroyEvent e) { if (in(e.getVehicle())) e.setCancelled(true); }

    // ------------------------------------------------------------------ quit

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { game.onQuit(e.getPlayer()); }
}
