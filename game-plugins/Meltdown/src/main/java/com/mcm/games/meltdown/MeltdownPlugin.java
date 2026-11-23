package com.mcm.games.meltdown;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class MeltdownPlugin extends JavaPlugin {
    private MeltdownGame game;

    @Override
    public void onEnable() {
        game = new MeltdownGame(this);
        getServer().getPluginManager().registerEvents(new MeltdownListener(game), this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try {
                game.stop();
            } catch (Throwable t) {
                getLogger().warning("Error while stopping Meltdown: " + t);
            }
        }
        McmApi.unregisterAll(this);
    }
}
