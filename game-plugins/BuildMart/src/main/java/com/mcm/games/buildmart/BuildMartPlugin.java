package com.mcm.games.buildmart;

import com.mcm.api.McmApi;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class BuildMartPlugin extends JavaPlugin {
    private BuildMartGame game;

    @Override
    public void onEnable() {
        // Start from a fresh void world every server start (clears leftovers from a crash).
        if (Bukkit.getWorld(BuildMartGame.WORLD) == null) {
            deleteFolder(new File(Bukkit.getWorldContainer(), BuildMartGame.WORLD).toPath());
        }
        game = new BuildMartGame(this);
        getServer().getPluginManager().registerEvents(game, this);
        game.ensureWorld();
        McmApi.registerGame(this, game);
    }

    @Override
    public void onDisable() {
        if (game != null) {
            try { game.stop(); } catch (Throwable t) { getLogger().warning("Error stopping Build Mart: " + t); }
        }
    }

    private void deleteFolder(Path root) {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.deleteIfExists(p); } catch (IOException ignored) { }
            });
        } catch (IOException e) {
            getLogger().warning("Could not delete old world folder " + root + ": " + e);
        }
    }
}
