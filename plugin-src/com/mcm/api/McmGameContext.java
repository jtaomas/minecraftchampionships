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

    // ---------------------------------------------------------------------------------------------
    // MCC coins (core 3.3.0+). Games pass BASE coins (the x1.0 wiki values, see game-plugins/COINS.md);
    // the core applies the event multiplier of the current game slot and keeps fractions exact.
    // addPoints/addSidePoints are multiplied the same way.
    // ---------------------------------------------------------------------------------------------

    /**
     * Award base MCC coins to one player (may be fractional, e.g. 12.5; negative takes coins away, as in Meltdown).
     * The core multiplies by {@link #coinMultiplier()} and prints "+N coins (reason)".
     * @since 3.3.0
     */
    default void addCoins(UUID player, double coins, String reason) {
        addPoints(player, (int) Math.round(coins), reason);
    }

    /**
     * MCC "shared amongst the team": a team pool of base coins (e.g. Battle Box round win = 160). Each online
     * member of the side receives {@code pool / 4} (MCC's team size, config {@code coins.mcc-team-size}), so a
     * player earns exactly the MCC per-member share whatever our team size is. Config
     * {@code coins.team-pool-split: team} splits the pool across the side's online members instead.
     * @since 3.3.0
     */
    default void addTeamCoins(String sideKey, double pool, String reason) {
        addSidePoints(sideKey, (int) Math.round(pool), reason);
    }

    /**
     * Per-member share of a team pool for a group of {@code members} online players, for games whose groups are
     * not tournament sides (squads of solos, split teams): {@code pool / 4} by default (MCC per-member share), or
     * {@code pool / members} with {@code coins.team-pool-split: team}. Pay it with {@link #addCoins}. @since 3.3.0
     */
    default double teamShare(double pool, int members) { return members <= 0 ? 0 : pool / members; }

    /**
     * Kill coins (MCC "ELIMINATION!" banner: crossed swords + the victim's name, double/triple tiers, ~4 s). The core
     * pays like {@link #addCoins}, prints the MCC chat line and shows the banner to the killer. A core without the
     * feature just pays and prints. {@code victim} may be null when the victim is not a player.
     * @since 3.4.0
     */
    default void addKillCoins(UUID killer, double coins, UUID victim, String victimName, String reason) {
        addCoins(killer, coins, reason);
    }

    /**
     * Objective coins. {@code banner} is the MCC centre graphic for this objective ("LAP COMPLETED!",
     * "ROOM COMPLETED!") or null where MCC shows none (TGTTOS finish, Hole in the Wall, checkpoints): chat line and
     * sound only.
     * @since 3.4.0
     */
    default void addObjectiveCoins(UUID player, double coins, String banner, String reason) {
        addCoins(player, coins, reason);
    }

    /** The event coin multiplier of the game running now (x1.0 outside a championship). @since 3.3.0 */
    default double coinMultiplier() { return 1.0; }

    /**
     * Declare the side that won the whole event (Dodgebolt: "this one's for the win"). The core's championship
     * result then names this side as champion instead of the side with the most coins. @since 3.3.0
     */
    default void setEventWinner(String sideKey) { }

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
