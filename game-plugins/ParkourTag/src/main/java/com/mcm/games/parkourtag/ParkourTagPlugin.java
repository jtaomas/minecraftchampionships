package com.mcm.games.parkourtag;

import com.mcm.api.McmApi;
import org.bukkit.plugin.java.JavaPlugin;

public final class ParkourTagPlugin extends JavaPlugin {
    private ParkourTagGame game;

    @Override
    public void onEnable() {
        game = new ParkourTagGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping Parkour Tag: " + t); }
        }
        McmApi.unregisterAll(this);
    }
}
