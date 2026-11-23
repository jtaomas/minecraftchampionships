package com.mcm.games.skyblockle;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** All Sky Blockle rules, only in the Sky Blockle world and only while a game is active. */
final class GameListener implements Listener {
    private static final Set<PotionEffectType> HARMFUL = Set.of(
            PotionEffectType.POISON, PotionEffectType.INSTANT_DAMAGE, PotionEffectType.WEAKNESS, PotionEffectType.SLOWNESS,
            PotionEffectType.WITHER, PotionEffectType.BLINDNESS, PotionEffectType.NAUSEA, PotionEffectType.HUNGER,
            PotionEffectType.MINING_FATIGUE, PotionEffectType.LEVITATION, PotionEffectType.DARKNESS,
            PotionEffectType.UNLUCK, PotionEffectType.GLOWING);

    private final SkyBlockleGame game;
    private final Set<UUID> spectateOnRespawn = new HashSet<>();

    GameListener(SkyBlockleGame game) { this.game = game; }

    private boolean ours(World w) { World m = game.map.world(); return m != null && w == m; }
    private boolean active(World w) { return game.active() && ours(w); }
    private boolean live() { return game.state == SkyBlockleGame.State.LIVE; }

    static Player attackerOf(Entity damager) {
        if (damager instanceof Player p) return p;
        if (damager instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player p) return p;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) return p;
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player p) return p;
        return null;
    }

    // ---------------------------------------------------------------- combat

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player v) || !active(v.getWorld())) return;
        if (!live() || !game.isAlive(v.getUniqueId())) { e.setCancelled(true); return; }
        if (e instanceof EntityDamageByEntityEvent ed) {
            Player a = attackerOf(ed.getDamager());
            if (a != null && !a.equals(v)) {
                UUID au = a.getUniqueId();
                // friendly fire never; PvP only after the grace period; spectators/dead can't hurt anyone
                if (!game.isAlive(au) || game.sameSide(au, v.getUniqueId()) || !game.pvpOn()) {
                    e.setCancelled(true);
                    return;
                }
                game.recordHit(v.getUniqueId(), au);
            }
        }
    }

    /** Lethal damage of any cause eliminates (one life). A held totem is left to vanilla. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onLethal(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player v) || !active(v.getWorld())) return;
        if (!live() || !game.isAlive(v.getUniqueId())) return;
        if (e.getFinalDamage() < v.getHealth()) return;
        if (v.getInventory().getItemInMainHand().getType() == Material.TOTEM_OF_UNDYING
                || v.getInventory().getItemInOffHand().getType() == Material.TOTEM_OF_UNDYING) return;
        e.setCancelled(true);
        boolean isVoid = e.getCause() == EntityDamageEvent.DamageCause.VOID;
        game.eliminate(v.getUniqueId(), v, causeText(e.getCause()), isVoid);
    }

    static String causeText(EntityDamageEvent.DamageCause c) {
        return switch (c) {
            case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> "was slain";
            case PROJECTILE -> "was shot";
            case FALL -> "fell from a high place";
            case VOID -> "fell out of the world";
            case WORLD_BORDER -> "was caught by the border";
            case LAVA, FIRE, FIRE_TICK, HOT_FLOOR -> "burned to death";
            case DROWNING -> "drowned";
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> "was blown up";
            case SUFFOCATION, FALLING_BLOCK -> "was squashed";
            case STARVATION -> "starved";
            case POISON, MAGIC, WITHER -> "died to magic";
            default -> "died";
        };
    }

    /** Backup for deaths that bypass the damage event (e.g. /kill). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        if (!active(p.getWorld()) || !game.isAlive(p.getUniqueId())) return;
        e.getDrops().clear();
        e.setDroppedExp(0);
        e.setKeepInventory(true);
        e.deathMessage(null);
        EntityDamageEvent last = p.getLastDamageCause();
        boolean isVoid = last != null && last.getCause() == EntityDamageEvent.DamageCause.VOID
                || p.getLocation().getY() < game.voidY();
        game.eliminate(p.getUniqueId(), p, "died", isVoid, false);
        spectateOnRespawn.add(p.getUniqueId());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        if (!spectateOnRespawn.remove(p.getUniqueId())) return;
        if (!game.active() || game.map.world() == null) return;
        Location ov = game.map.overview();
        e.setRespawnLocation(ov);
        Bukkit.getScheduler().runTask(game.plugin, () -> {
            if (p.isOnline() && game.active()) game.makeSpectator(p, ov);
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent e) {
        ThrownPotion pot = e.getPotion();
        if (!active(pot.getWorld()) || !(pot.getShooter() instanceof Player thrower)) return;
        if (!harmful(pot.getEffects())) return;
        for (LivingEntity le : e.getAffectedEntities()) {
            if (le instanceof Player t && !t.equals(thrower) && blocked(thrower, t)) e.setIntensity(le, 0);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCloud(AreaEffectCloudApplyEvent e) {
        AreaEffectCloud c = e.getEntity();
        if (!active(c.getWorld()) || !(c.getSource() instanceof Player thrower)) return;
        boolean bad = harmful(c.getCustomEffects()) || (c.getBasePotionType() != null && harmful(c.getBasePotionType().getPotionEffects()));
        if (!bad) return;
        e.getAffectedEntities().removeIf(le -> le instanceof Player t && !t.equals(thrower) && blocked(thrower, t));
    }

    private boolean blocked(Player attacker, Player target) {
        return !game.isAlive(attacker.getUniqueId()) || game.sameSide(attacker.getUniqueId(), target.getUniqueId()) || !game.pvpOn();
    }

    private static boolean harmful(Collection<PotionEffect> effects) {
        if (effects == null) return false;
        for (PotionEffect pe : effects) if (HARMFUL.contains(pe.getType())) return true;
        return false;
    }

    // ---------------------------------------------------------------- movement

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        if (!active(p.getWorld())) return;
        Location to = e.getTo();
        Location from = e.getFrom();
        UUID u = p.getUniqueId();
        if (game.isAlive(u)) {
            if (game.state == SkyBlockleGame.State.COUNTDOWN) {
                if (from.getX() != to.getX() || from.getZ() != to.getZ() || to.getY() > from.getY()) {
                    Location back = from.clone();
                    back.setYaw(to.getYaw());
                    back.setPitch(to.getPitch());
                    e.setTo(back);
                }
                return;
            }
            if (to.getY() < game.voidY()) {
                if (live()) game.eliminate(u, p, "fell out of the world", true);
                else if (game.state == SkyBlockleGame.State.ENDING) p.teleport(game.map.castleCentre());
            }
        } else if (p.getGameMode() == GameMode.SPECTATOR && game.settings != null
                && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ())) {
            game.confineSpectator(p);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        if (!active(e.getFrom().getWorld())) return;
        if (e.getTo() == null || !ours(e.getTo().getWorld())) e.setCancelled(true);
    }

    // ---------------------------------------------------------------- building

    private boolean mayBuild(Player p) {
        return live() && game.isAlive(p.getUniqueId()) && p.getGameMode() == GameMode.SURVIVAL;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (!active(e.getBlock().getWorld())) return;
        if (!mayBuild(e.getPlayer())) { e.setCancelled(true); return; }
        if (e.getBlock().getY() > game.buildCapY()) {
            e.setCancelled(true);
            e.getPlayer().sendActionBar(net.kyori.adventure.text.Component.text("Build height limit reached", net.kyori.adventure.text.format.NamedTextColor.RED));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlaced(BlockPlaceEvent e) {
        if (!ours(e.getBlock().getWorld())) return;
        game.map.markDirty(e.getBlock());
        if (e instanceof BlockMultiPlaceEvent mp) for (BlockState s : mp.getReplacedBlockStates()) game.map.markDirty(s.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (!active(e.getBlock().getWorld())) return;
        if (!mayBuild(e.getPlayer())) { e.setCancelled(true); return; }
        if (game.graves.isGrave(e.getBlock())) {
            e.setDropItems(false);           // no free chest item
            game.graves.broken(e.getBlock()); // the grave's contents spill out
        }
    }

    /** Right-click a grave: open "<name>'s Grave" (any living player may loot it). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onGraveOpen(PlayerInteractEvent e) {
        if (e.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK || e.getClickedBlock() == null) return;
        if (!active(e.getPlayer().getWorld()) || !game.graves.isGrave(e.getClickedBlock())) return;
        e.setCancelled(true);
        if (live() && game.isAlive(e.getPlayer().getUniqueId())) game.graves.open(e.getPlayer(), e.getClickedBlock());
    }

    /** Explosions don't destroy graves. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGraveExplode(EntityExplodeEvent e) {
        if (ours(e.getEntity().getWorld())) e.blockList().removeIf(game.graves::isGrave);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onGraveBlockExplode(BlockExplodeEvent e) {
        if (ours(e.getBlock().getWorld())) e.blockList().removeIf(game.graves::isGrave);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent e) {
        if (!active(e.getBlock().getWorld())) return;
        if (!mayBuild(e.getPlayer()) || e.getBlock().getY() > game.buildCapY()) { e.setCancelled(true); return; }
        game.map.markDirty(e.getBlock());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent e) {
        if (!active(e.getBlock().getWorld())) return;
        if (!mayBuild(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (!active(e.getPlayer().getWorld())) return;
        if (game.isAlive(e.getPlayer().getUniqueId()) && !live()) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFood(FoodLevelChangeEvent e) {
        if (e.getEntity() instanceof Player p && active(p.getWorld()) && !live()) e.setCancelled(true);
    }

    // ---------------------------------------------------------------- world protection & change tracking

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPortal(PortalCreateEvent e) {
        if (ours(e.getWorld()) && game.plugin.getConfig().getBoolean("world.block-portals", true)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerPortal(PlayerPortalEvent e) {
        if (ours(e.getFrom().getWorld())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        if (e.getInventory().getType() != InventoryType.ENDER_CHEST) return;
        if (!(e.getPlayer() instanceof Player p) || !active(p.getWorld())) return;
        if (game.plugin.getConfig().getBoolean("world.block-ender-chests", true)) {
            e.setCancelled(true);
            p.sendMessage(ChatColor.RED + "Ender chests are disabled in Sky Blockle.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlow(BlockFromToEvent e) { if (ours(e.getBlock().getWorld())) game.map.markDirty(e.getToBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onForm(BlockFormEvent e) { if (ours(e.getBlock().getWorld())) game.map.markDirty(e.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent e) { if (ours(e.getBlock().getWorld())) game.map.markDirty(e.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGrow(BlockGrowEvent e) { if (ours(e.getBlock().getWorld())) game.map.markDirty(e.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent e) { if (ours(e.getBlock().getWorld())) game.map.markDirty(e.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBurn(BlockBurnEvent e) { if (ours(e.getBlock().getWorld())) game.map.markDirty(e.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTree(StructureGrowEvent e) {
        if (!ours(e.getWorld())) return;
        for (BlockState s : e.getBlocks()) game.map.markDirtyBlock(s.getX(), s.getZ());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityBlock(EntityChangeBlockEvent e) { if (ours(e.getBlock().getWorld())) game.map.markDirty(e.getBlock()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent e) {
        if (!ours(e.getBlock().getWorld())) return;
        Block b = e.getBlock();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) game.map.markDirtyBlock(b.getX() + dx, b.getZ() + dz);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonOut(BlockPistonExtendEvent e) { markMoved(e.getBlock(), e.getBlocks(), e.getDirection()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPistonIn(BlockPistonRetractEvent e) { markMoved(e.getBlock(), e.getBlocks(), e.getDirection()); }

    private void markMoved(Block piston, List<Block> blocks, org.bukkit.block.BlockFace dir) {
        if (!ours(piston.getWorld())) return;
        game.map.markDirty(piston.getRelative(dir));
        for (Block b : blocks) { game.map.markDirty(b); game.map.markDirty(b.getRelative(dir)); }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplode(EntityExplodeEvent e) {
        if (!ours(e.getEntity().getWorld())) return;
        for (Block b : e.blockList()) game.map.markDirty(b);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (!ours(e.getBlock().getWorld())) return;
        for (Block b : e.blockList()) game.map.markDirty(b);
    }

    // ---------------------------------------------------------------- quitters

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        spectateOnRespawn.remove(p.getUniqueId());
        if (!game.active() || !game.isAlive(p.getUniqueId())) return;
        // quitters count as eliminated (items handled like a death if they were on the map)
        game.eliminate(p.getUniqueId(), p, "disconnected", false, false);
        if (ours(p.getWorld())) {
            p.getInventory().clear();
            p.setGameMode(GameMode.SURVIVAL);
        }
    }
}
