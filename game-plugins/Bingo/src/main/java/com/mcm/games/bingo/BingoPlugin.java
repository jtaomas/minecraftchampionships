package com.mcm.games.bingo;

import com.mcm.api.McmApi;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class BingoPlugin extends JavaPlugin {
    private BingoWorld world;
    private BingoGame bingo;
    private BingoGame lockout;
    private NamespacedKey cardKey;

    @Override
    public void onEnable() {
        cardKey = new NamespacedKey(this, "bingo_card");
        world = new BingoWorld(this);
        // A leftover folder from a crash/shutdown is not loaded at this point; clear it out.
        if (Bukkit.getWorld(BingoWorld.NAME) == null) world.deleteFolderSafely();

        bingo = new BingoGame(this, world, false);
        lockout = new BingoGame(this, world, true);
        Bukkit.getPluginManager().registerEvents(bingo, this);
        Bukkit.getPluginManager().registerEvents(lockout, this);
        McmApi.registerGame(this, bingo);
        McmApi.registerGame(this, lockout);
    }

    @Override
    public void onDisable() {
        if (bingo != null) bingo.shutdown();
        if (lockout != null) lockout.shutdown();
        if (world != null) {
            try { world.unloadNow(); } catch (Throwable t) { getLogger().warning("Unload on disable failed: " + t.getMessage()); }
        }
    }

    public NamespacedKey cardKey() { return cardKey; }
}
