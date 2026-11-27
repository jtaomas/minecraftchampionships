package com.mcm;

import com.mcm.api.McmFx;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * Serves the server resource pack straight from this machine and sends it to every player on join.
 *
 * <p>The zip lives in {@code plugins/MinecraftMondayCore/pack/} (never in the repo or the jar). A tiny HTTP server on
 * {@code resource-pack.port} hands it out; each player is told to fetch it from the same host name they typed to join
 * the game, so it works for localhost, LAN and a forwarded port alike (the pack port must be forwarded too).
 * {@code resource-pack.public-host} overrides the host name. A player whose download fails keeps playing without the
 * pack; with {@code force} on, a player who refuses the pack is disconnected by their own client.
 */
final class PackServer implements Listener {
    private final JavaPlugin plugin;
    private final Map<UUID, String> joinHost = new ConcurrentHashMap<>();
    private HttpServer http;
    private byte[] bytes;
    private byte[] sha1;
    private String sha1Hex;
    private int port;
    private boolean force;
    private String publicHost;
    private String prompt;

    PackServer(JavaPlugin plugin) { this.plugin = plugin; }

    void start() {
        FileConfiguration c = plugin.getConfig();
        if (!c.getBoolean("resource-pack.enabled", true)) return;
        File f = new File(plugin.getDataFolder(), c.getString("resource-pack.file", "pack/BlockShowdown_Pack.zip"));
        if (!f.isFile()) {
            File fallback = new File("resourcepack/dist/BlockShowdownPack.zip");
            if (fallback.isFile()) {
                f = fallback;
            } else {
                plugin.getLogger().info("[pack] no resource pack at " + f + " (or " + fallback + ") - none is sent.");
                return;
            }
        }
        port = c.getInt("resource-pack.port", 25581);
        force = c.getBoolean("resource-pack.force", false);
        publicHost = c.getString("resource-pack.public-host", "");
        prompt = c.getString("resource-pack.prompt", "Block Showdown uses this pack for the MCC look and sounds.");
        try {
            bytes = Files.readAllBytes(f.toPath());
            sha1 = MessageDigest.getInstance("SHA-1").digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : sha1) sb.append(String.format("%02x", b));
            sha1Hex = sb.toString();
            http = HttpServer.create(new InetSocketAddress(port), 0);
            http.createContext("/", ex -> {
                try {
                    boolean ok = ex.getRequestURI().getPath().endsWith(".zip");
                    if (!ok) { ex.sendResponseHeaders(404, -1); return; }
                    ex.getResponseHeaders().set("Content-Type", "application/zip");
                    if ("HEAD".equalsIgnoreCase(ex.getRequestMethod())) {
                        ex.sendResponseHeaders(200, -1);
                        return;
                    }
                    ex.sendResponseHeaders(200, bytes.length);
                    try (OutputStream o = ex.getResponseBody()) { o.write(bytes); }
                } finally {
                    ex.close();
                }
            });
            http.setExecutor(Executors.newFixedThreadPool(4, r -> { Thread t = new Thread(r, "MCM-pack-http"); t.setDaemon(true); return t; }));
            http.start();
            Bukkit.getPluginManager().registerEvents(this, plugin);
            plugin.getLogger().info("[pack] serving " + f.getName() + " (" + bytes.length / 1024 + " KB, sha1 " + sha1Hex + ") on port "
                    + port + (force ? ", required" : ", optional"));
            for (Player p : Bukkit.getOnlinePlayers()) send(p);
        } catch (Throwable t) {
            plugin.getLogger().warning("[pack] could not start the pack server on port " + port + ": " + t);
            http = null;
        }
    }

    void stop() {
        if (http != null) { try { http.stop(0); } catch (Throwable ignored) {} http = null; }
    }

    @EventHandler
    public void onLogin(PlayerLoginEvent e) {
        String h = e.getHostname();
        if (h == null) return;
        int nul = h.indexOf('\0');
        if (nul >= 0) h = h.substring(0, nul);
        if (h.startsWith("[")) { int end = h.indexOf(']'); if (end > 0) h = h.substring(0, end + 1); }
        else { int colon = h.lastIndexOf(':'); if (colon > 0) h = h.substring(0, colon); }
        if (h.endsWith(".")) h = h.substring(0, h.length() - 1);
        if (!h.isEmpty() && h.matches("[A-Za-z0-9.\\-\\[\\]:]+")) joinHost.put(e.getPlayer().getUniqueId(), h);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        McmFx.packLoaded(p.getUniqueId(), false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> { if (p.isOnline()) send(p); }, 30L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        joinHost.remove(e.getPlayer().getUniqueId());
        McmFx.packLoaded(e.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onStatus(PlayerResourcePackStatusEvent e) {
        Player p = e.getPlayer();
        switch (e.getStatus()) {
            case ACCEPTED, DOWNLOADED -> plugin.getLogger().info("[pack] " + p.getName() + " status: " + e.getStatus());
            case SUCCESSFULLY_LOADED -> {
                McmFx.packLoaded(p.getUniqueId(), true);
                plugin.getLogger().info("[pack] " + p.getName() + " loaded the resource pack.");
            }
            case FAILED_DOWNLOAD, INVALID_URL, FAILED_RELOAD -> {
                McmFx.packLoaded(p.getUniqueId(), false);
                plugin.getLogger().warning("[pack] " + p.getName() + " could not load the pack (" + e.getStatus() + ") from "
                        + url(p) + " - is port " + port + " reachable for them?");
                p.sendMessage(ChatColor.YELLOW + "The server pack did not download, so you are playing without the MCC look and sounds.");
            }
            case DECLINED, DISCARDED -> {
                McmFx.packLoaded(p.getUniqueId(), false);
                plugin.getLogger().info("[pack] " + p.getName() + " declined/discarded the resource pack (" + e.getStatus() + ").");
                p.sendMessage(ChatColor.YELLOW + "Server resource pack was declined. Set 'Server Resource Packs: Enabled' in your server list entry to load the MCC pack.");
            }
            default -> { }
        }
    }

    private String url(Player p) {
        String h = publicHost != null && !publicHost.isBlank() ? publicHost.trim() : joinHost.getOrDefault(p.getUniqueId(), "localhost");
        return "http://" + h + ":" + port + "/pack-" + sha1Hex.substring(0, 10) + ".zip";
    }

    private void send(Player p) {
        if (http == null) return;
        try {
            String u = url(p);
            plugin.getLogger().info("[pack] sending " + u + " to " + p.getName());
            p.setResourcePack(u, sha1, prompt, force);
        } catch (Throwable t) {
            plugin.getLogger().warning("[pack] could not send the pack to " + p.getName() + ": " + t);
        }
    }
}
