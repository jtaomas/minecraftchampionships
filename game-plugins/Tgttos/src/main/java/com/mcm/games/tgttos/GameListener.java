package com.mcm.games.tgttos;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
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
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
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
import org.bukkit.event.world.EntitiesLoadEvent;

/** TGTTOS rules. Every handler only acts while a game runs and only in the game world. */
final class GameListener implements Listener {
    private final TgttosGame game;

    GameListener(TgttosGame game) { this.game = game; }

    private boolean in(Entity e) { return e != null && game.inWorld(e.getWorld()); }

    /** Persistent TGTTOS chickens left over from an earlier round (chunk unloaded before cleanup) are removed on load. */
    @EventHandler
    public void onEntitiesLoad(EntitiesLoadEvent e) {
        if (e.getWorld().getName().equals(TgttosPlugin.WORLD)) game.onEntitiesLoad(e.getEntities());
    }

    // ------------------------------------------------------------------ damage, knockback, finish

    /**
     * Chicken punches. Goal chickens are invulnerable (so nothing can kill them), and the server drops an attack on
     * an invulnerable entity inside LivingEntity.hurt() before any EntityDamageEvent is fired - so the finish is
     * detected here, at the attack itself (Paper fires this before the invulnerability check). Cancelled so the
     * chicken takes no hit and no knockback. Ignores earlier cancellation on purpose.
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAttack(PrePlayerAttackEntityEvent e) {
        Entity target = e.getAttacked();
        if (!in(target) || !game.isGameChicken(target)) return;
        e.setCancelled(true);
        game.chickenPunched(e.getPlayer(), (Chicken) target, "attack");
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(EntityDamageEvent e) {
        Entity victim = e.getEntity();
        if (!in(victim)) return;
        e.setCancelled(true);                       // no damage, ever
        if (victim instanceof Player p) { p.setFireTicks(0); }
        if (!(e instanceof EntityDamageByEntityEvent ev)) return;
        Entity damager = ev.getDamager();

        if (game.isGameChicken(victim)) {
            // fallback only (e.g. creative attackers bypass invulnerability); whack() is idempotent per racer
            if (damager instanceof Player p && ev.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
                game.chickenPunched(p, (Chicken) victim, "damage");
            return;
        }
        if (damager instanceof Snowball sb && victim instanceof Player v) {
            game.snowballHit(sb, v, "damage");      // de-duplicated per snowball; decides + logs
            return;
        }
        if (!(victim instanceof Player v) || !game.isRacing(v)) return;
        if (damager instanceof Player a) {
            if (ev.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK || !game.isRacing(a) || a.equals(v)) return;
            game.punch(a, v);
        }
    }

    /** Snowball knockback, first path: the projectile hit itself (fires before any damage event). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity() instanceof Snowball sb && e.getHitEntity() instanceof Player v && in(v))
            game.snowballHit(sb, v, "hit");
    }

    /** Racers' snowballs are tracked so the game can sweep them against racers itself (see TgttosGame.sweepSnowballs). */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (e.getEntity() instanceof Snowball sb && in(sb) && sb.getShooter() instanceof Player p && game.isParticipant(p))
            game.trackSnowball(sb);
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
            game.onPlaced(e.getPlayer(), b);
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
        if (game.isGameChicken(e.getRightClicked())) e.setCancelled(true);   // no leads, no name tags, no riding
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
        // ... nor jump to another map slot in the same world (only inside the current map's play box)
        Player p = e.getPlayer();
        if (in(p) && (game.isParticipant(p) || game.isWatcher(p)) && e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE
                && !game.spectatorMayGo(e.getTo()))
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
