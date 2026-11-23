package com.mcm.games.railroadrush;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class RailroadRushPlugin extends JavaPlugin {
    private RailroadRushGame game;

    @Override
    public void onEnable() {
        game = new RailroadRushGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) game.stop();
    }
}
