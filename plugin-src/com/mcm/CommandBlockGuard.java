package com.mcm;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps a map's command blocks inside that map's world.
 *
 * <p>Downloaded maps were written for a server that runs one world, so their command blocks target {@code @a}:
 * "give everyone jump boost", "put everyone in adventure", "set everyone's max health". On this server that reached
 * every player in every world, lobby included. A selector with a distance filter only matches entities in the
 * dimension the command runs in, so every {@code @a}, {@code @r} and {@code @e} of a command block is given
 * {@code distance=0..}; the command then behaves exactly as before for players in its own world and no longer
 * touches anyone else. A world with no players in it does not run its command blocks at all.
 */
final class CommandBlockGuard implements Listener {
    private static final Pattern SELECTOR = Pattern.compile("@([are])(\\[)?");

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(ServerCommandEvent e) {
        CommandSender s = e.getSender();
        org.bukkit.World w;
        if (s instanceof BlockCommandSender b) w = b.getBlock().getWorld();
        else if (s instanceof CommandMinecart m) w = m.getWorld();
        else return;
        if (w.getPlayers().isEmpty()) { e.setCancelled(true); return; }
        String cmd = e.getCommand();
        if (cmd.indexOf('@') < 0) return;
        e.setCommand(scope(cmd));
    }

    static String scope(String cmd) {
        Matcher m = SELECTOR.matcher(cmd);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String rep;
            if (m.group(2) == null) {
                rep = "@" + m.group(1) + "[distance=0..]";
            } else {
                int close = cmd.indexOf(']', m.end());
                String args = close < 0 ? "" : cmd.substring(m.end(), close);
                boolean scoped = args.contains("distance=") || args.matches("(?s)(.*,)?\\s*d?[xyz]=.*");
                rep = "@" + m.group(1) + (scoped ? "[" : args.isBlank() ? "[distance=0.." : "[distance=0..,");
            }
            m.appendReplacement(out, Matcher.quoteReplacement(rep));
        }
        m.appendTail(out);
        return out.toString();
    }
}
