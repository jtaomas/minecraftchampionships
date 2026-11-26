package com.mcm.games.parkourtag;

import com.mcm.api.McmApi;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParkourTagPlugin extends JavaPlugin {
    private ParkourTagGame game;

    @Override
    public void onEnable() {
        GlowPackets.init(this);
        game = new ParkourTagGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
        PtOverlay.init(this);
        try {
            McmApi.registerMapEditable(this, game.id(), new PtEdit(game));   // core /mcm mapedit (Map Edit core; older cores skip it)
        } catch (Throwable t) {
            getLogger().info("Map Edit is not available on this core (" + t.getClass().getSimpleName() + ").");
        }
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping Parkour Tag: " + t); }
        }
        McmApi.unregisterAll(this);
    }

    /** /parkourtag status | hunts &lt;player&gt; &lt;n&gt; (admin). */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (game == null) return true;
        if (args.length >= 1 && args[0].equalsIgnoreCase("status")) { sender.sendMessage(game.adminStatus()); return true; }
        if (args.length >= 3 && args[0].equalsIgnoreCase("hunts")) {
            try { sender.sendMessage(game.adminSetHunts(args[1], Integer.parseInt(args[2]))); }
            catch (NumberFormatException e) { sender.sendMessage("Usage: /parkourtag hunts <player> <n>"); }
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("map")) {
            sender.sendMessage(game.forceNextMap(args[1]) ? "Next game uses " + args[1] : "Unknown map " + args[1] + " (" + game.maps() + ")");
            return true;
        }
        sender.sendMessage("Usage: /parkourtag status | hunts <player> <n> | map <id>");
        return true;
    }
}
