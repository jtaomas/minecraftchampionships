package com.mcm.games.acerace;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class AceRacePlugin extends JavaPlugin {
    private AceRaceGame game;

    @Override
    public void onEnable() {
        game = new AceRaceGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try {
                game.stop();
            } catch (Throwable t) {
                getLogger().warning("Error stopping Ace Race: " + t);
            }
        }
        McmApi.unregisterAll(this);
    }
}
