package com.mcm.api;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Everything a game needs from the tournament while it runs. All methods must be called on the main thread. */
public interface McmGameContext {
    /** Players taking part (everyone online when the game started, minus anyone who has since quit). */
    Collection<Player> participants();

    /** Tournament team name ("Red", "Blue", ...) or null if the player is playing solo. */
    String teamOf(UUID player);

    /** Chat colour for a team name (white for null). */
    ChatColor teamColor(String team);

    /**
     * Participants grouped into sides. Key is "TEAM:&lt;name&gt;" (e.g. "TEAM:Red") or "SOLO:&lt;uuid&gt;" for a solo player.
     * Use this to split players into arenas/teams.
     */
    Map<String, List<Player>> sides();

    /** Human-readable side label with colour, e.g. "§cRed Team" or a solo player's name. */
    String sideLabel(String sideKey);

    /**
     * Side keys (same format as {@link #sides()}) of participating sides, ordered by total tournament
     * points, highest first. Only sides with at least one online participant are included. Limited to n (n &lt;= 0 = all).
     */
    List<String> topSides(int n);

    /** Total tournament points of a side so far (sum of its members' points). */
    int sidePoints(String sideKey);

    /** Award points to one player (rolls into their team total). */
    void addPoints(UUID player, int points, String reason);

    /** Award points to a whole side, split evenly across its online members. */
    void addSidePoints(String sideKey, int points, String reason);

    /** Broadcast a chat message to everyone. */
    void broadcast(String message);

    /** Lobby spawn. */
    Location lobby();

    /** Drive the boss bar timer. */
    void setTimer(int secondsLeft, int totalSeconds);

    /** Extra sidebar lines under the game name (max ~6 are shown). */
    void setSidebarLines(List<String> lines);

    /**
     * End the game: the core prints standings, returns everyone to the lobby and moves to the next event.
     * Runs synchronously: when it returns, players are already in the lobby, so it is safe to clear the arena
     * right after. The core does NOT call {@link McmGame#stop()} after finish. Calls after the first are ignored.
     */
    void finish(String reason);
}
