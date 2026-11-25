package com.mcm.games.buildmart;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class BuildMartPlugin extends JavaPlugin {
    private BuildMartGame game;

    @Override
    public void onEnable() {
        game = new BuildMartGame(this);
        game.enable();
        getServer().getPluginManager().registerEvents(game, this);
        getServer().getPluginManager().registerEvents(game.trolley, this);
        var cmd = getCommand("bm");
        if (cmd != null) cmd.setExecutor(game);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping Build Mart: " + t); }
        }
    }
}
