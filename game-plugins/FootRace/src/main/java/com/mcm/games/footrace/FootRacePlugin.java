package com.mcm.games.footrace;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class FootRacePlugin extends JavaPlugin {
    private FootRaceGame game;

    @Override
    public void onEnable() {
        game = new FootRaceGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        game.ensureWorld();
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try {
                game.stop();
            } catch (Throwable t) {
                getLogger().warning("Error stopping Foot Race: " + t);
            }
        }
    }
}
