package com.mcm.games.acerace;

import com.mcm.games.acerace.map.RaceMap;
import com.mcm.games.acerace.map.RaceMapData;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /acerace list|info|build|tp|validate|reload|stop */
public final class AceRaceCommand implements CommandExecutor, TabCompleter {
    private final AceRacePlugin plugin;

    public AceRaceCommand(AceRacePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] args) {
        if (!s.hasPermission("acerace.admin")) {
            s.sendMessage(ChatColor.RED + "You need acerace.admin.");
            return true;
        }
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> list(s);
            case "info" -> {
                if (args.length < 2) { usage(s); return true; }
                info(s, args[1]);
            }
            case "validate" -> {
                if (args.length < 2) { usage(s); return true; }
                validate(s, args[1]);
            }
            case "build", "tp" -> {
                if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
                if (args.length < 2) { usage(s); return true; }
                buildOrTp(p, args[1], sub.equals("build"));
            }
            case "reload" -> {
                plugin.reloadConfig();
                s.sendMessage(ChatColor.GREEN + "Ace Race config reloaded.");
            }
            case "stop" -> {
                plugin.game().stop();
                s.sendMessage(ChatColor.YELLOW + "Ace Race stopped.");
            }
            default -> usage(s);
        }
        return true;
    }

    private void list(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "Ace Race maps:");
        for (RaceMap m : plugin.registry().maps()) {
            RaceMapData d = plugin.registry().data(m.id());
            int blocks = d == null ? 0 : d.blocks().size();
            s.sendMessage(ChatColor.YELLOW + m.id() + ChatColor.GRAY + " - " + ChatColor.WHITE + m.displayName()
                    + ChatColor.GRAY + " | " + m.laps() + " laps | " + m.timeLimitSeconds() + "s | "
                    + blocks + " blocks | slot " + plugin.registry().slot(m.id()));
        }
    }

    private void info(CommandSender s, String id) {
        RaceMap map = plugin.registry().map(id);
        RaceMapData d = plugin.registry().data(id);
        if (map == null || d == null) { s.sendMessage(ChatColor.RED + "Unknown map: " + id); return; }
        s.sendMessage(ChatColor.GOLD + map.displayName() + ChatColor.GRAY + " (" + map.id() + ")");
        s.sendMessage(ChatColor.GRAY + "Laps: " + ChatColor.WHITE + map.laps()
                + ChatColor.GRAY + "  Time: " + ChatColor.WHITE + map.timeLimitSeconds() + "s"
                + ChatColor.GRAY + "  Countdown: " + ChatColor.WHITE + map.countdownSeconds() + "s");
        s.sendMessage(ChatColor.GRAY + "Blocks: " + ChatColor.WHITE + d.blocks().size()
                + ChatColor.GRAY + "  Sections: " + ChatColor.WHITE + d.sections.size()
                + ChatColor.GRAY + "  Pads: " + ChatColor.WHITE + d.pads.size()
                + ChatColor.GRAY + "  Rings: " + ChatColor.WHITE + d.rings.size());
        s.sendMessage(ChatColor.GRAY + "Start slots: " + ChatColor.WHITE + d.startGrid.size()
                + ChatColor.GRAY + "  Slot: " + ChatColor.WHITE + plugin.registry().slot(map.id()));
    }

    private void validate(CommandSender s, String id) {
        RaceMapData d = plugin.registry().data(id);
        if (d == null) { s.sendMessage(ChatColor.RED + "Unknown map: " + id); return; }
        List<String> errors = d.check();
        if (errors.isEmpty()) s.sendMessage(ChatColor.GREEN + id + ": OK (" + d.blocks().size() + " blocks)");
        else {
            s.sendMessage(ChatColor.RED + id + ": " + errors.size() + " error(s)");
            for (String e : errors) s.sendMessage(ChatColor.GRAY + " - " + e);
        }
    }

    private void buildOrTp(Player p, String id, boolean announce) {
        RaceMapData d = plugin.registry().data(id);
        if (d == null) { p.sendMessage(ChatColor.RED + "Unknown map: " + id); return; }
        int slot = plugin.registry().slot(id);
        p.sendMessage(ChatColor.YELLOW + "Preparing " + d.mapId + "...");
        plugin.world().paste(slot, d, () -> {
            Location to;
            if (!d.overviews.isEmpty()) {
                var s = d.overviews.get(0);
                to = plugin.world().toWorld(slot, s.x(), s.y(), s.z(), s.yaw());
            } else {
                to = plugin.world().world() == null ? p.getLocation() : plugin.world().world().getSpawnLocation();
            }
            if (p.isOnline()) {
                p.teleport(to);
                p.setGameMode(org.bukkit.GameMode.CREATIVE);
                p.sendMessage(ChatColor.GREEN + "Ready: " + d.mapId + " (" + d.blocks().size() + " blocks)");
            }
        });
    }

    private void usage(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "/acerace list" + ChatColor.GRAY + " - maps");
        s.sendMessage(ChatColor.GOLD + "/acerace info <id>" + ChatColor.GRAY + " - map details");
        s.sendMessage(ChatColor.GOLD + "/acerace validate <id>" + ChatColor.GRAY + " - contract check");
        s.sendMessage(ChatColor.GOLD + "/acerace build <id>" + ChatColor.GRAY + " - paste and visit");
        s.sendMessage(ChatColor.GOLD + "/acerace tp <id>" + ChatColor.GRAY + " - visit built map");
        s.sendMessage(ChatColor.GOLD + "/acerace reload" + ChatColor.GRAY + " - config");
        s.sendMessage(ChatColor.GOLD + "/acerace stop" + ChatColor.GRAY + " - stop a running race");
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String x : List.of("list", "info", "validate", "build", "tp", "reload", "stop"))
                if (x.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(x);
        } else if (args.length == 2 && List.of("info", "validate", "build", "tp").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (String x : plugin.registry().ids())
                if (x.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(x);
        }
        return out;
    }
}
