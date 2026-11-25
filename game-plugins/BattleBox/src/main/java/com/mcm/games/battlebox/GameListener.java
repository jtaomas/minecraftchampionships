package com.mcm.games.battlebox;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;

/**
 * All Battle Box rules. Game handlers only act while the game runs, for participants in the Battle Box world.
 *
 * Centre blocks: fighters are in SURVIVAL during the fight, so vanilla breaking/placing works; every place/break
 * that is not a team concrete/wool block on one of the 9 centre cells is cancelled here. Valid centre breaks mine
 * at normal vanilla speed with the centre tool (iron pickaxe on concrete, shears on wool) and drop nothing.
 */
final class GameListener implements Listener {
    private final org.bukkit.plugin.Plugin plugin;
    private final BattleBoxGame game;

    GameListener(org.bukkit.plugin.Plugin plugin, BattleBoxGame game) {
        this.plugin = plugin;
        this.game = game;
    }

    // ------------------------------------------------------------------ combat

    @EventHandler(priority = EventPriority.HIGH)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (!game.handles(p)) return;
        game.onDamage(p, e);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent e) {
        ThrownPotion potion = e.getPotion();
        if (!game.inWorld(potion.getWorld())) return;
        Player thrower = BattleBoxGame.playerSource(potion);
        if (thrower == null || !game.isAliveFighter(thrower)) { e.setCancelled(true); return; }
        boolean harmful = false, healing = false;
        for (PotionEffect eff : potion.getEffects()) {
            PotionEffectType t = eff.getType();
            if (t.equals(PotionEffectType.INSTANT_DAMAGE) || t.equals(PotionEffectType.POISON)
                    || t.equals(PotionEffectType.WEAKNESS) || t.equals(PotionEffectType.SLOWNESS)) harmful = true;
            if (t.equals(PotionEffectType.INSTANT_HEALTH) || t.equals(PotionEffectType.REGENERATION)) healing = true;
        }
        for (LivingEntity le : new ArrayList<>(e.getAffectedEntities())) {
            if (!(le instanceof Player t) || !game.isAliveFighter(t)) { e.setIntensity(le, 0); continue; }
            boolean same = game.sameSide(thrower, t);
            if (harmful && same) e.setIntensity(le, 0);
            if (healing && !same) e.setIntensity(le, 0);
            if (harmful && !same) game.markAttack(t, thrower);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!game.handles(p)) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.setKeepInventory(true);
        e.setKeepLevel(true);
        e.setDeathMessage(null);
        game.onDeath(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!game.handles(p)) return;
        e.setRespawnLocation(game.respawnLocation(p));
        Bukkit.getScheduler().runTask(plugin, () -> game.afterRespawn(p));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) { game.onQuit(e.getPlayer()); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!game.handles(p) || p.getGameMode() == GameMode.SPECTATOR) return;
        if (!game.freezeMovementDuringKit() || game.matchOf(p.getUniqueId()) == null) return;
        Location from = e.getFrom(), to = e.getTo();
        if (to == null) return;
        if (from.getX() != to.getX() || from.getZ() != to.getZ()) {
            e.setTo(new Location(from.getWorld(), from.getX(), to.getY(), from.getZ(), to.getYaw(), to.getPitch()));
        }
    }

    // ------------------------------------------------------------------ centre blocks

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlace(BlockPlaceEvent e) {
        Player p = e.getPlayer();
        if (!game.handles(p)) return;
        if (!game.canPlace(p, e.getBlockPlaced(), e.getBlockPlaced().getType(), e.getBlockReplacedState().getType().isAir())) {
            e.setCancelled(true);
            return;
        }
        e.setCancelled(false);   // our rule decides: valid centre placement
        game.afterCentreChange(p);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockDamage(BlockDamageEvent e) {
        Player p = e.getPlayer();
        if (!game.handles(p)) return;
        if (game.canBreak(p, e.getBlock())) e.setCancelled(false);   // vanilla mining speed, no insta-break
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (!game.handles(p)) return;
        if (!game.canBreak(p, e.getBlock())) {
            e.setCancelled(true);
            return;
        }
        e.setCancelled(false);
        e.setDropItems(false);
        e.setExpToDrop(0);
        game.afterCentreChange(p);
    }

    // ------------------------------------------------------------------ items

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!game.handles(p)) return;
        Action a = e.getAction();
        if (a == Action.PHYSICAL) { e.setCancelled(true); return; }
        boolean right = a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK;
        Gear.Custom c = Gear.customOf(e.getItem());

        if (game.phase() != BattleBoxGame.Phase.FIGHT || !game.isAliveFighter(p)) {
            // kit select / spectating / holding: nothing may be used; the star opens the kit menu
            if (right || a == Action.RIGHT_CLICK_BLOCK) e.setCancelled(true);
            if (a == Action.LEFT_CLICK_BLOCK) e.setCancelled(true);
            if (right && e.getHand() == EquipmentSlot.HAND && c == Gear.Custom.MENU) game.openKitMenu(p);
            return;
        }
        if (c != null && right) {
            e.setCancelled(true);
            if (c != Gear.Custom.MENU) game.abilities.use(p, c, e.getHand() == null ? EquipmentSlot.HAND : e.getHand());
            return;
        }
        // never open containers / doors / trapdoors in the arena; placement of the held block still goes through
        if (a == Action.RIGHT_CLICK_BLOCK && e.getClickedBlock() != null && e.getClickedBlock().getType().isInteractable()) {
            e.setUseInteractedBlock(Event.Result.DENY);
        }
        // LEFT_CLICK_BLOCK is deliberately untouched so centre blocks can be mined
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrop(PlayerDropItemEvent e) { if (game.handles(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPickup(EntityPickupItemEvent e) { if (e.getEntity() instanceof Player p && game.handles(p)) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onArrowPickup(PlayerPickupArrowEvent e) { if (game.handles(e.getPlayer())) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && game.handles(p)) { e.setCancelled(true); p.setFoodLevel(20); }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCraft(CraftItemEvent e) { if (e.getWhoClicked() instanceof Player p && game.handles(p)) e.setCancelled(true); }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLaunch(ProjectileLaunchEvent e) {
        if (!game.inWorld(e.getEntity().getWorld())) return;
        if (e.getEntity() instanceof AbstractArrow arrow && arrow.getShooter() instanceof Player)
            arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof Snowball s) || !game.inWorld(s.getWorld())) return;
        if (!s.getPersistentDataContainer().has(game.abilities.throwKey, PersistentDataType.STRING)) return;
        game.abilities.landed(s);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onExplode(EntityExplodeEvent e) {
        if (ours(e.getLocation().getWorld())) e.blockList().clear();
    }

    // ------------------------------------------------------------------ menus

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getView().getTopInventory().getHolder() instanceof KitMenu menu) {
            e.setCancelled(true);
            if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;
            if (menu.preview) {
                if (e.getSlot() == 26) Bukkit.getScheduler().runTask(plugin, () -> KitMenu.open(p, menu.theme, themeName(menu.theme),
                        menu.wool, null, null, null, true));
                return;
            }
            Kit k = menu.kitAt(e.getSlot());
            if (k == null) return;
            if (menu.inspect) Bukkit.getScheduler().runTask(plugin, () -> KitMenu.openPreview(p, menu.theme, themeName(menu.theme), menu.wool, k));
            else Bukkit.getScheduler().runTask(plugin, () -> game.selectKit(p, k));
            return;
        }
        // keep team armour on
        if (game.handles(p) && e.getSlotType() == InventoryType.SlotType.ARMOR) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof KitMenu) e.setCancelled(true);
    }

    static String themeName(String id) {
        var t = com.mcm.games.battlebox.arena.Themes.byId(id);
        return t == null ? id : t.displayName();
    }

    // ------------------------------------------------------------------ world protection (our world only)

    private boolean ours(org.bukkit.World w) { return w != null && BattleBoxGame.WORLD_NAME.equals(w.getName()); }

    @EventHandler(ignoreCancelled = true)
    public void onFade(BlockFadeEvent e) { if (ours(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) { if (ours(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onForm(BlockFormEvent e) { if (ours(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) { if (ours(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onDecay(LeavesDecayEvent e) { if (ours(e.getBlock().getWorld())) e.setCancelled(true); }

    @EventHandler(ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (ours(e.getLocation().getWorld()) && e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.CUSTOM) e.setCancelled(true);
    }
}
