package com.mcm.games.hitw;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class HoleInTheWallPlugin extends JavaPlugin {
    private HoleInTheWallGame game;

    @Override
    public void onEnable() {
        game = new HoleInTheWallGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping game: " + t); }
        }
        McmApi.unregisterGame("hitw");
    }
}
