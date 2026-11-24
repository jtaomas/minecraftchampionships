package com.mcm;

import org.bukkit.Bukkit;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;

/**
 * Owner request (1.8-style rod in the Minecraft Monday games): a fishing bobber that hits a player only
 * knocks them back; it must not stay attached to them. OldCombatMechanics applies the knockback on the
 * hit; one tick later the hook is removed so nothing is hooked or dragged. MCC-style worlds
 * ({@code mcc_*}) keep vanilla behaviour.
 */
final class FishHookFix implements Listener {
    private final Plugin plugin;

    FishHookFix(Plugin plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onHit(ProjectileHitEvent e) {
        if (!(e.getEntity() instanceof FishHook hook)) return;
        if (!(e.getHitEntity() instanceof Player)) return;
        if (hook.getWorld().getName().startsWith("mcc_")) return;
        Bukkit.getScheduler().runTask(plugin, () -> { if (hook.isValid()) hook.remove(); });
    }
}
