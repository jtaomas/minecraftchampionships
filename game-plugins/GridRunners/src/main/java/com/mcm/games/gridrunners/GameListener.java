package com.mcm.games.gridrunners;

import com.mcm.games.gridrunners.rooms.Room;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.SlimeSplitEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class GameListener implements Listener {
    private final GridRunnersGame game;

    public GameListener(GridRunnersGame game) { this.game = game; }

    private boolean inWorld(org.bukkit.World w) { return w != null && w == game.world(); }

    private boolean live(org.bukkit.World w) { return game.isLive() && inWorld(w); }

    private static boolean isContainer(Material m) {
        return m == Material.BARREL || m == Material.CHEST || m == Material.TRAPPED_CHEST || m == Material.HOPPER
                || m == Material.DISPENSER || m == Material.DROPPER || m == Material.FURNACE
                || Tag.SHULKER_BOXES.isTagged(m);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!live(p.getWorld())) return;
        Block b = e.getClickedBlock();
        if (e.getAction() == Action.PHYSICAL) {
            if (b != null && (Tag.PRESSURE_PLATES.isTagged(b.getType()) || b.getType() == Material.FARMLAND)) e.setCancelled(true);
            return;
        }
        if (b == null || e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        boolean container = isContainer(b.getType());
        if (e.getHand() != EquipmentSlot.HAND) {
            if (container) e.setCancelled(true);
            return;
        }
        if (game.isRunning() && p.getGameMode() != GameMode.SPECTATOR) {
            Room r = game.roomAt(b.getLocation());
            if (r != null && r.state == Room.State.ACTIVE && game.laneOf(p.getUniqueId()) == r.lane) {
                r.onInteractBlock(p, b, e);
            }
        }
        if (container) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (!live(e.getPlayer().getWorld())) return;
        Room r = game.roomOf(e.getRightClicked());
        if (r == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND || !game.isRunning()) return;
        if (r.state == Room.State.ACTIVE && game.laneOf(e.getPlayer().getUniqueId()) == r.lane) {
            r.onInteractEntity(e.getPlayer(), e.getRightClicked());
        }
    }

    @EventHandler
    public void onShear(PlayerShearEntityEvent e) {
        if (live(e.getPlayer().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        if (!inWorld(e.getEntity().getWorld()) || e.getEntity() instanceof Player) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        Room r = game.roomOf(e.getEntity());
        if (r != null && r.state == Room.State.ACTIVE && game.isRunning()) r.onEntityDeath(e.getEntity());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!live(e.getEntity().getWorld())) return;
        Entity victim = e.getEntity();
        Entity damager = null;
        if (e instanceof EntityDamageByEntityEvent ed) {
            damager = ed.getDamager();
            if (damager instanceof Projectile pr && pr.getShooter() instanceof Entity sh) damager = sh;
        }
        if (victim instanceof Player p) {
            boolean fromRoomMob = damager != null && !(damager instanceof Player) && game.roomOf(damager) != null;
            if (!fromRoomMob || !game.isRunning() || p.getGameMode() == GameMode.SPECTATOR) {
                e.setCancelled(true);
                return;
            }
            if (e.getFinalDamage() >= p.getHealth()) {
                e.setCancelled(true);
                game.rescue(p);
            }
            return;
        }
        Room r = game.roomOf(victim);
        if (r == null) return;
        if (game.isProtected(victim)) {
            if (damager == null || damager instanceof Player || game.isProtected(damager)) {
                e.setCancelled(true);
                return;
            }
            if (victim instanceof LivingEntity le && e.getFinalDamage() >= le.getHealth()) {
                e.setCancelled(true);
                if (r.state == Room.State.ACTIVE && game.isRunning()) r.onProtectedLethal(le);
            }
            return;
        }
        // Room monsters can only be hurt by players.
        if (!(damager instanceof Player)) e.setCancelled(true);
    }

    @EventHandler
    public void onCombust(EntityCombustEvent e) {
        if (inWorld(e.getEntity().getWorld()) && !(e.getEntity() instanceof Player)) e.setCancelled(true);
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        if (!live(e.getPlayer().getWorld())) return;
        if (!game.isCrystal(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent e) {
        if (!live(e.getEntity().getWorld())) return;
        if (!(e.getEntity() instanceof Player) || !game.isCrystal(e.getItem().getItemStack())) e.setCancelled(true);
    }

    @EventHandler
    public void onHopperPickup(InventoryPickupItemEvent e) {
        if (inWorld(e.getItem().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (live(e.getBlock().getWorld()) && e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        if (live(e.getBlock().getWorld()) && e.getPlayer().getGameMode() != GameMode.CREATIVE) e.setCancelled(true);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent e) {
        if (live(e.getEntity().getWorld())) {
            e.setCancelled(true);
            e.getEntity().setFoodLevel(20);
        }
    }

    @EventHandler
    public void onChangeBlock(EntityChangeBlockEvent e) {
        if (inWorld(e.getBlock().getWorld()) && !(e.getEntity() instanceof Player)) e.setCancelled(true);
    }

    @EventHandler
    public void onSpawn(CreatureSpawnEvent e) {
        if (inWorld(e.getLocation().getWorld()) && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) e.setCancelled(true);
    }

    @EventHandler
    public void onSplit(SlimeSplitEvent e) {
        if (inWorld(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onTransform(EntityTransformEvent e) {
        if (inWorld(e.getEntity().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onDecay(LeavesDecayEvent e) {
        if (inWorld(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onFade(BlockFadeEvent e) {
        if (inWorld(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onSpread(BlockSpreadEvent e) {
        if (inWorld(e.getBlock().getWorld())) e.setCancelled(true);
    }

    @EventHandler
    public void onForm(BlockFormEvent e) {
        if (inWorld(e.getBlock().getWorld())) e.setCancelled(true);
    }
}
