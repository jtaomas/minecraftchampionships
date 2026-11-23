package com.mcm.games.rocketspleef;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class RocketSpleefPlugin extends JavaPlugin {
    private RocketSpleefGame game;

    @Override
    public void onEnable() {
        game = new RocketSpleefGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try {
                game.stop();
            } catch (Throwable t) {
                getLogger().warning("Error while stopping Rocket Spleef Rush: " + t);
            }
        }
        McmApi.unregisterAll(this);
    }
}
