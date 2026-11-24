package com.mcm.games.findthebutton;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
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
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * Find the Button rules. Everything here only acts while a game is running and only in the game world.
 * - the real button: the right-click goes through (never cancelled) and is scored;
 * - every other right-click on a block is denied (no chests, doors, trapdoors, beds, pots ... no block changes);
 * - no damage, hunger, block breaking/placing, item drops/pickups;
 * - frozen in the start area until GO; falling out of the map returns you to the start;
 * - spectators (finders) are kept inside the map's box.
 */
final class GameListener implements Listener {
    private final FindTheButtonPlugin plugin;
    private final FindTheButtonGame game;

    GameListener(FindTheButtonPlugin plugin, FindTheButtonGame game) {
        this.plugin = plugin;
        this.game = game;
    }

    private boolean inGameWorld(World w) {
        return game.isRunning() && w != null && w.equals(game.world());
    }

    private boolean active(Player p) { return inGameWorld(p.getWorld()); }

    // ------------------------------------------------------------------ the button & interactions

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!active(p)) return;
        Block b = e.getClickedBlock();
        if (e.getAction() == Action.PHYSICAL) { e.setCancelled(true); return; }
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;     // left clicks are never denied (adventure can't break anyway)
        if (b != null && game.isButton(b)) {
            // let vanilla press it (click sound, pressed look); score it
            e.setUseInteractedBlock(Event.Result.ALLOW);
            e.setUseItemInHand(Event.Result.DENY);
            game.onPress(p);
            return;
        }
        e.setUseInteractedBlock(Event.Result.DENY);
        e.setUseItemInHand(Event.Result.DENY);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (active(e.getPlayer())) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ no damage / hunger / changes / items

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        Entity ent = e.getEntity();
        if (!inGameWorld(ent.getWorld())) return;
        e.setCancelled(true);
        if (ent instanceof Player p && e.getCause() == EntityDamageEvent.DamageCause.VOID) backToStart(p);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent e) {
        if (inGameWorld(e.getEntity().getWorld())) { e.setCancelled(true); e.getEntity().setFoodLevel(20); }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) { if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) { if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucket(PlayerBucketEmptyEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketFill(PlayerBucketFillEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) { if (active(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent e) { if (inGameWorld(e.getEntity().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityChangeBlock(EntityChangeBlockEvent e) { if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onHangingBreak(HangingBreakEvent e) { if (inGameWorld(e.getEntity().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFade(BlockFadeEvent e) { if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onForm(BlockFormEvent e) { if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpread(BlockSpreadEvent e) { if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFlow(BlockFromToEvent e) { if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDecay(LeavesDecayEvent e) { if (inGameWorld(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawn(CreatureSpawnEvent e) {
        if (inGameWorld(e.getLocation().getWorld())) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ movement: freeze, out of map, spectator box

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!active(p)) return;
        boolean participant = game.participant(p);
        // late joiners / rejoiners are not participants, but as spectators they are kept in the room too
        if (!participant && p.getGameMode() != GameMode.SPECTATOR) return;
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        if (participant && game.frozen() &&(from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY())) {
            Location stay = from.clone();
            stay.setYaw(to.getYaw());
            stay.setPitch(to.getPitch());
            if (to.getY() < from.getY()) stay.setY(to.getY());   // may settle down, never walk or jump
            e.setTo(stay);
            return;
        }
        double[] box = game.bounds(p.getGameMode() == GameMode.SPECTATOR ? 0.3 : 2.0);
        if (box == null) return;
        boolean outside = to.getX() < box[0] || to.getX() > box[3] || to.getZ() < box[2] || to.getZ() > box[5]
                || to.getY() < box[1] - (p.getGameMode() == GameMode.SPECTATOR ? 0 : 1) || to.getY() > box[4];
        if (!outside) return;
        if (p.getGameMode() == GameMode.SPECTATOR) {
            Location c = to.clone();
            c.setX(clamp(to.getX(), box[0] + 0.2, box[3] - 0.2));
            c.setY(clamp(to.getY(), box[1] + 0.2, box[4] - 0.2));
            c.setZ(clamp(to.getZ(), box[2] + 0.2, box[5] - 0.2));
            e.setTo(c);
        } else {
            backToStart(p);
        }
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }

    private void backToStart(Player p) {
        Location s = game.startLocation();
        if (s == null) return;
        p.setFallDistance(0f);
        p.teleport(s);
    }

    /** Spectators can't use the spectator menu / teleport out of the map. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        Player p = e.getPlayer();
        if (!active(p) || (!game.participant(p) && p.getGameMode() != GameMode.SPECTATOR)) return;
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        Location to = e.getTo();
        double[] box = game.bounds(0.3);
        if (to == null || box == null || !inGameWorld(to.getWorld())
                || to.getX() < box[0] || to.getX() > box[3] || to.getZ() < box[2] || to.getZ() > box[5]
                || to.getY() < box[1] || to.getY() > box[4]) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        if (game.isRunning()) game.onQuit(e.getPlayer());
    }
}
