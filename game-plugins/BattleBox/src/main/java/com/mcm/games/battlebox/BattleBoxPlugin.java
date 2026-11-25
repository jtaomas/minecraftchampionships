package com.mcm.games.battlebox;

import com.mcm.api.McmApi;
import com.mcm.api.McmBuild;
import com.mcm.games.battlebox.arena.ArenaTheme;
import com.mcm.games.battlebox.arena.Themes;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BattleBoxPlugin extends JavaPlugin {
    private BattleBoxGame game;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        Gear.init(this);
        Themes.loadImported(new File(getDataFolder(), "imported-arenas"), getLogger());
        game = new BattleBoxGame(this);
        game.ensureWorld();
        getServer().getPluginManager().registerEvents(new GameListener(this, game), this);
        PluginCommand cmd = getCommand("bb");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try {
                game.stop();
                game.cancelAdmin();
            } catch (Throwable t) {
                getLogger().warning("Error while stopping Battle Box: " + t);
            }
        }
        McmApi.unregisterAll(this);
    }

    @Override
    public ChunkGenerator getDefaultWorldGenerator(String worldName, String id) { return new VoidGenerator(); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "Battle Box admin: " + ChatColor.YELLOW + "/bb arena <theme>" + ChatColor.GRAY + " build + visit a preview arena, "
                    + ChatColor.YELLOW + "/bb kits [theme]" + ChatColor.GRAY + " inspect the kit menu, "
                    + ChatColor.YELLOW + "/bb theme <id|off>" + ChatColor.GRAY + " force a theme, "
                    + ChatColor.YELLOW + "/bb dump" + ChatColor.GRAY + " dump the preview arena, "
                    + ChatColor.YELLOW + "/bb themes" + ChatColor.GRAY + " list themes.");
            return true;
        }
        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "themes" -> {
                for (ArenaTheme t : Themes.all())
                    sender.sendMessage(ChatColor.YELLOW + t.id() + ChatColor.GRAY + " - " + ChatColor.WHITE + t.displayName() + ChatColor.GRAY + ": " + t.tagline());
                String f = getConfig().getString("force-theme", "");
                sender.sendMessage(ChatColor.GRAY + "Forced theme: " + (f == null || f.isBlank() ? "off" : f)
                        + ", prefer imported: " + getConfig().getBoolean("prefer-imported-maps", true)
                        + " (" + Themes.imported().size() + " loaded)"
                        + ", rotate each round: " + getConfig().getBoolean("rotate-maps-each-round", true)
                        + ", centre block: " + getConfig().getString("centre-block", "concrete"));
            }
            case "arena" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                ArenaTheme t = args.length > 1 ? Themes.byId(args[1]) : null;
                if (t == null) { sender.sendMessage(ChatColor.RED + "Usage: /bb arena <" + String.join("|", Themes.ids()) + ">"); return true; }
                game.adminArena(p, t);
            }
            case "kits" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                String id = args.length > 1 ? args[1] : game.currentThemeId();
                ArenaTheme t = Themes.byId(id == null ? "greenhouse" : id);
                if (t == null) { sender.sendMessage(ChatColor.RED + "Unknown theme. " + Themes.ids()); return true; }
                boolean wool = "wool".equalsIgnoreCase(getConfig().getString("centre-block", "concrete"));
                Gear.centreToolEfficiency = Math.max(0, Math.min(5, getConfig().getInt("centre-tool-efficiency", 0)));
                KitMenu.open(p, t.id(), t.displayName(), wool, null, null, null, true);
            }
            case "theme" -> {
                if (args.length < 2) { sender.sendMessage(ChatColor.RED + "Usage: /bb theme <" + String.join("|", Themes.ids()) + "|off>"); return true; }
                String v = args[1].toLowerCase(Locale.ROOT);
                if (v.equals("off") || v.equals("random") || v.equals("none")) {
                    getConfig().set("force-theme", "");
                    sender.sendMessage(ChatColor.GREEN + "Theme forcing off (random rotation).");
                } else {
                    ArenaTheme t = Themes.byId(v);
                    if (t == null) { sender.sendMessage(ChatColor.RED + "Unknown theme. " + Themes.ids()); return true; }
                    getConfig().set("force-theme", t.id());
                    sender.sendMessage(ChatColor.GREEN + "Every round will use " + t.displayName() + " (from the next game).");
                }
                saveConfig();
            }
            case "dump" -> {
                File dir = new File(getDataFolder(), "dumps");
                dir.mkdirs();
                File out = new File(dir, "preview_arena.mcmdump.gz");
                McmBuild.dumpRegion(game.adminMin().getWorld(), game.adminMin(), game.adminMax(), out,
                        r -> sender.sendMessage(r.error() == null ? ChatColor.GREEN + "Dumped " + r.blocks() + " blocks to " + r.file()
                                : ChatColor.RED + "Dump failed: " + r.error()));
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown subcommand. /bb for help.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            for (String s : List.of("arena", "kits", "theme", "dump", "themes")) if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) out.add(s);
        } else if (args.length == 2 && List.of("arena", "kits", "theme").contains(args[0].toLowerCase(Locale.ROOT))) {
            for (String s : Themes.ids()) if (s.startsWith(args[1].toLowerCase(Locale.ROOT))) out.add(s);
            if (args[0].equalsIgnoreCase("theme")) out.add("off");
        }
        return out;
    }
}
