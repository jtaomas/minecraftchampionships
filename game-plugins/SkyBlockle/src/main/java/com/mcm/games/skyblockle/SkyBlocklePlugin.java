package com.mcm.games.skyblockle;

import com.mcm.api.McmApi;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class SkyBlocklePlugin extends JavaPlugin {
    private SkyBlockleGame game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "loot.yml").exists()) saveResource("loot.yml", false);
        game = new SkyBlockleGame(this);
        getServer().getPluginManager().registerEvents(new GameListener(game), this);
        PluginCommand cmd = getCommand("sbl");
        if (cmd != null) { cmd.setExecutor(this); cmd.setTabCompleter(this); }
        McmApi.registerGame(this, game);
        // create the world at enable (LESSONS: per-world combat settings key off the world existing at startup)
        try { game.map.ensureWorld(); } catch (Throwable t) { getLogger().warning("Sky Blockle: could not create world: " + t); }
        String gen;
        try { Class.forName(MapWorld.SKYMAP_CLASS, false, getClassLoader()); gen = "SkyMap present"; }
        catch (ClassNotFoundException e) { gen = "SkyMap missing - the stub map will be used"; }
        getLogger().info("Sky Blockle ready (map.generator=" + getConfig().getString("map.generator", "auto") + ", " + gen + ")");
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping Sky Blockle: " + t); }
        }
        McmApi.unregisterAll(this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Sky Blockle admin: " + ChatColor.YELLOW + "/sbl build [wedges] [seed]" + ChatColor.GRAY + " preview map, "
                    + ChatColor.YELLOW + "/sbl tp" + ChatColor.GRAY + " go to the castle, "
                    + ChatColor.YELLOW + "/sbl info" + ChatColor.GRAY + " map + border info, "
                    + ChatColor.YELLOW + "/sbl loot <type>" + ChatColor.GRAY + " roll a chest, "
                    + ChatColor.YELLOW + "/sbl clear" + ChatColor.GRAY + " wipe the map, "
                    + ChatColor.YELLOW + "/sbl reload" + ChatColor.GRAY + " reload config + loot.");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "build" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                int wedges = 4;
                long seed = 0;
                try {
                    if (args.length > 1) wedges = Math.max(1, Math.min(10, Integer.parseInt(args[1])));
                    if (args.length > 2) seed = Long.parseLong(args[2]);
                } catch (NumberFormatException e) { sender.sendMessage(ChatColor.RED + "Usage: /sbl build [wedges] [seed]"); return true; }
                game.adminBuild(p, wedges, seed);
            }
            case "tp" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (game.map.world() == null) { sender.sendMessage(ChatColor.RED + "World missing."); return true; }
                p.teleport(game.map.overview());
            }
            case "info" -> {
                MapModel m = game.map.model();
                if (m == null) { sender.sendMessage(ChatColor.GRAY + "No map built yet (/sbl build)."); return true; }
                sender.sendMessage(ChatColor.GOLD + "Map: " + ChatColor.WHITE + m.wedges + " wedges, seed " + m.seed + ", " + m.blocks.size()
                        + " blocks, radius " + m.mapRadius + ", castle r" + m.castleRadius + ", arena r" + m.arenaRadius + ", y " + m.minY + ".." + m.maxY);
                sender.sendMessage(ChatColor.GOLD + "Chests: " + ChatColor.WHITE + game.map.chestCounts() + ", brewing stands " + game.map.brewingStands()
                        + ", spawns " + m.spawns.size() + ", team markers " + m.teamMarkers.size());
                sender.sendMessage(ChatColor.GOLD + "Rings: " + ChatColor.WHITE + m.rings);
                if (!m.notes.isEmpty()) sender.sendMessage(ChatColor.GOLD + "Notes: " + ChatColor.WHITE + m.notes);
                Settings s = Settings.load(getConfig(), getLogger());
                BorderSchedule b = s.border(m, getLogger());
                StringBuilder sb = new StringBuilder(ChatColor.GOLD + "Border: " + ChatColor.WHITE);
                for (int t : new int[]{0, 300, 360, 600, 900, 1000, 1080}) sb.append(SkyBlockleGame.fmt(t)).append("=").append((int) Math.round(b.radiusAt(t)))
                        .append(" (").append(String.format(Locale.ROOT, "%.1f", b.damageAt(t))).append("/s) ");
                sender.sendMessage(sb.toString());
            }
            case "loot" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                String type = args.length > 1 ? args[1].toLowerCase(Locale.ROOT) : "castle";
                LootTables lt = game.loadLoot();
                if (lt.table(type) == null) { sender.sendMessage(ChatColor.RED + "No table '" + type + "'. Tables: " + lt.all().keySet()); return true; }
                Inventory inv = Bukkit.createInventory(null, 27, "Loot roll: " + type);
                lt.fill(inv, type, new Random(), getConfig().getBoolean("loot.random-slots", true));
                p.openInventory(inv);
            }
            case "clear" -> {
                if (game.active()) { sender.sendMessage(ChatColor.RED + "A game is running."); return true; }
                game.map.wipe(() -> sender.sendMessage(ChatColor.GREEN + "Sky Blockle map wiped."));
            }
            case "reload" -> {
                reloadConfig();
                LootTables lt = game.loadLoot();
                sender.sendMessage(ChatColor.GREEN + "Reloaded config and loot (" + lt.all().size() + " tables). Applies from the next game.");
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. /sbl for help.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("build", "tp", "info", "loot", "clear", "reload")) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("loot")) {
            for (Map.Entry<String, LootTables.Table> e : game.loadLoot().all().entrySet())
                if (e.getKey().startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(e.getKey());
        }
        return out;
    }
}
