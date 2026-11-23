package com.mcm.games.meltdown;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/** Enforces Meltdown's rules - only while the game is active and only inside its world. */
public final class MeltdownListener implements Listener {
    private final MeltdownGame game;

    public MeltdownListener(MeltdownGame game) {
        this.game = game;
    }

    private boolean ours(Entity e) {
        return game.active() && game.inWorld(e);
    }

    private boolean oursBlock(Block b) {
        return game.active() && game.world != null && b.getWorld() == game.world;
    }

    /** Alive, not frozen, game running. */
    private boolean canAct(Player p) {
        return game.phase == MeltdownGame.Phase.RUNNING && game.isAlive(p.getUniqueId()) && !game.isFrozen(p.getUniqueId());
    }

    // ------------------------------------------------------------------ blocks

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (!oursBlock(b)) return;
        Player p = e.getPlayer();
        if (!canAct(p) || game.arena == null) { e.setCancelled(true); return; }
        MeltdownGame.Heater h = game.heaterAt(b);
        if (h != null) {
            e.setCancelled(true);
            punchHeater(p, h);
            return;
        }
        if (game.arena.isCrate(b)) {
            e.setCancelled(false);
            e.setDropItems(false);
            e.setExpToDrop(0);
            game.onCrateBroken(p, b);
            return;
        }
        if (game.arena.isBreakableWall(b)) {
            e.setCancelled(false);
            e.setDropItems(false);
            e.setExpToDrop(0);
            return;
        }
        e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamageBlock(BlockDamageEvent e) {
        Block b = e.getBlock();
        if (!oursBlock(b)) return;
        MeltdownGame.Heater h = game.heaterAt(b);
        if (h == null) return;
        e.setCancelled(true);
        if (canAct(e.getPlayer())) punchHeater(e.getPlayer(), h);
    }

    private void punchHeater(Player p, MeltdownGame.Heater h) {
        boolean own = h.owner.equals(p.getUniqueId());
        boolean sameSide = h.side.equals(game.sideOf.get(p.getUniqueId()));
        if (sameSide && !own) {
            p.sendMessage("§7That's a teammate's heater.");
            return;
        }
        game.removeHeater(h, own ? "§7You picked up your heater." : "§cYour heater was smashed by " + p.getName() + "!");
        if (!own) p.sendMessage("§aYou smashed an enemy heater!");
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlockPlaced();
        if (!oursBlock(b)) return;
        Player p = e.getPlayer();
        ItemStack item = e.getItemInHand();
        if (!canAct(p) || game.arena == null || !"heater".equals(game.itemId(item))) { e.setCancelled(true); return; }
        if (!game.arena.isInsidePlayable(b) || game.heaterOf.containsKey(p.getUniqueId())) {
            e.setCancelled(true);
            return;
        }
        e.setCancelled(false);
        game.onHeaterPlaced(p, b);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) { if (oursBlock(e.getBlock())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent e) { if (oursBlock(e.getBlock())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) { if (oursBlock(e.getBlock())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) { if (oursBlock(e.getBlock())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) { if (oursBlock(e.getBlock())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) { if (oursBlock(e.getBlock())) e.setCancelled(true); }

    // ------------------------------------------------------------------ combat

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile proj = e.getEntity();
        if (!ours(proj)) return;
        if (e.getHitEntity() instanceof Player victim && proj.getShooter() instanceof Player shooter) {
            game.onArrowHit(shooter, victim);
        }
        if (proj instanceof AbstractArrow) proj.remove();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!ours(e.getEntity())) return;
        // all player damage in Meltdown is applied manually by the game (cold); arrows freeze instead of hurting
        if (e.getEntity() instanceof Player) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onShoot(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p) || !ours(p)) return;
        if (!canAct(p)) { e.setCancelled(true); return; }
        if (e.getProjectile() instanceof AbstractArrow a) a.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        p.setCooldown(Material.BOW, 20);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent e) {
        if (!ours(e.getEntity())) return;
        e.setCancelled(true);
        e.getEntity().setFoodLevel(20);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRegain(EntityRegainHealthEvent e) {
        if (!ours(e.getEntity())) return;
        if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
                || e.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ items & interaction

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!ours(p) || !game.isParticipant(p.getUniqueId())) return;
        if (e.getHand() == EquipmentSlot.OFF_HAND) { e.setCancelled(true); return; }
        if (!canAct(p)) {
            if (game.phase == MeltdownGame.Phase.COUNTDOWN && e.getItem() != null && "map".equals(game.itemId(e.getItem()))) {
                game.showMap(p);
            }
            e.setCancelled(true);
            return;
        }
        Action a = e.getAction();
        // never let players use campfires (e.g. put items on them)
        if (a == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType() == Material.CAMPFIRE
                && !"heater".equals(game.itemId(e.getItem()))) {
            e.setCancelled(true);
            return;
        }
        if (a != Action.RIGHT_CLICK_AIR && a != Action.RIGHT_CLICK_BLOCK) return;
        String id = game.itemId(e.getItem());
        if ("warmer".equals(id)) {
            e.setCancelled(true);
            game.useWarmer(p);
        } else if ("map".equals(id)) {
            e.setCancelled(true);
            game.showMap(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) {
        if (ours(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent e) {
        if (ours(e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickupArrow(PlayerPickupArrowEvent e) {
        if (ours(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (ours(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p) || !ours(p) || !game.isParticipant(p.getUniqueId())) return;
        if (game.isFrozen(p.getUniqueId()) || e.getSlotType() == InventoryType.SlotType.ARMOR) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ movement & connection

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!ours(p)) return;
        UUID id = p.getUniqueId();
        if (!game.isAlive(id)) return;
        boolean locked = game.phase == MeltdownGame.Phase.COUNTDOWN || game.phase == MeltdownGame.Phase.BUILDING || game.isFrozen(id);
        if (!locked || !e.hasChangedPosition()) return;
        Location from = e.getFrom();
        Location to = e.getTo();
        Location fixed = from.clone();
        fixed.setYaw(to.getYaw());
        fixed.setPitch(to.getPitch());
        // allow falling straight down onto the floor so frozen players do not hover
        if (to.getY() < from.getY() && game.isFrozen(id)) fixed.setY(to.getY());
        e.setTo(fixed);
    }

    /** Someone who logged out inside the arena and returns after the game: send them back to the lobby. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!p.getWorld().getName().equals(MeltdownGame.WORLD_NAME)) return;
        org.bukkit.Bukkit.getScheduler().runTaskLater(game.plugin(), () -> {
            if (!p.isOnline() || !p.getWorld().getName().equals(MeltdownGame.WORLD_NAME)) return;
            if (game.active()) return; // the core handles late joiners during a game
            Location lobby = game.lobbyOrSpawn();
            if (lobby != null) {
                p.getInventory().clear();
                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
                p.teleport(lobby);
            }
        }, 5L);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (!game.active() || !game.isParticipant(p.getUniqueId())) return;
        if (game.isAlive(p.getUniqueId())) {
            game.eliminate(p.getUniqueId(), "left the game", true);
        } else {
            game.cleanupPlayer(p);
        }
    }
}
