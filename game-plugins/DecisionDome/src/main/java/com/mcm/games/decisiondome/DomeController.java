package com.mcm.games.decisiondome;

import com.mcm.api.McmLobby;
import com.mcm.api.McmVoteProvider;
import com.mcm.api.McmVoteRequest;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Thin {@link McmVoteProvider}: owns the shared tick task and exactly-once result delivery.
 * All vote logic lives in {@link DomeSession} and {@link VoteEngine}.
 */
final class DomeController implements McmVoteProvider {
    private final DecisionDomePlugin plugin;
    private final DomeArena arena;
    private final DomeSettings settings;
    private DomeSession active;
    private BukkitTask task;
    private long voteCounter;

    DomeController(DecisionDomePlugin plugin, DomeArena arena, DomeSettings settings) {
        this.plugin = plugin;
        this.arena = arena;
        this.settings = settings;
    }

    // ---------------------------------------------------------------------------------------------
    // McmVoteProvider
    // ---------------------------------------------------------------------------------------------

    @Override
    public void runVote(List<String> remainingGameIds, Consumer<String> onChosen) {
        McmVoteRequest request = new McmVoteRequest(remainingGameIds, teamsSnapshot(), 1,
                false, false, settings.dialCapacity);
        begin(request, onChosen, false, false);
    }

    @Override
    public void runVote(McmVoteRequest request, Consumer<String> onChosen) {
        begin(request, onChosen, false, false);
    }

    @Override
    public void cancelVote() {
        DomeSession s = active;
        active = null;
        if (s != null) s.abort();
        stopTaskIfIdle();
    }

    @Override
    public boolean skipWait() {
        DomeSession s = active;
        return s != null && s.skipWait();
    }

    // ---------------------------------------------------------------------------------------------
    // Admin
    // ---------------------------------------------------------------------------------------------

    /** Demo vote with no result callback. Returns false if a championship vote is already running. */
    boolean startTest(List<String> ids, boolean preferHub) {
        if (active != null && active.running() && !active.testMode()) return false;
        if (active != null) active.abort();
        McmVoteRequest request = new McmVoteRequest(ids, syntheticTeams(), 1, true, false,
                settings.dialCapacity);
        begin(request, null, true, preferHub);
        return true;
    }

    void adminCancel() {
        DomeSession s = active;
        if (s != null) s.adminCancel();
    }

    void endVoting() {
        if (active != null) active.endVoting();
    }

    void stopAll() {
        DomeSession s = active;
        active = null;
        if (s != null) s.abort();
        if (task != null) { task.cancel(); task = null; }
    }

    boolean isActive() { return active != null && active.running(); }

    boolean isTest() { return active != null && active.testMode(); }

    Phase phase() { return active == null || active.engine() == null ? Phase.IDLE : active.engine().phase(); }

    DomeSession session() { return active; }

    String status() {
        if (active == null) {
            return "idle; venue=" + venueDescription() + "; api=" + (McmLobby.venue() == null ? 0 : McmLobby.venue().apiVersion());
        }
        return active.status() + " test=" + active.testMode();
    }

    String venueDescription() {
        McmLobby.Venue v = McmLobby.venue();
        if (v != null && v.apiVersion() >= 2) return "hub dial (v2)";
        if (v != null) return "hub v1 (fallback preferred)";
        return arena.ensureWorld() == null ? "unavailable" : "mcc_dome";
    }

    // ---------------------------------------------------------------------------------------------
    // Lifecycle
    // ---------------------------------------------------------------------------------------------

    private void begin(McmVoteRequest request, Consumer<String> callback, boolean test, boolean preferHub) {
        if (request == null) request = McmVoteRequest.simple(List.of());
        if (active != null && active.running()) active.abort();
        DomeVenue venue = chooseVenue(preferHub);
        long seed = (System.nanoTime() * 0x9E3779B97F4A7C15L) ^ (++voteCounter * 0xBF58476D1CE4E5B9L);
        DomeSession session = new DomeSession(plugin, this, request, venue, settings, test, callback, seed);
        active = session;
        ensureTask();
        try {
            session.start();
        } catch (Throwable t) {
            session.error(t);
        }
    }

    private DomeVenue chooseVenue(boolean preferHub) {
        McmLobby.Venue v = McmLobby.venue();
        if (v != null && (v.apiVersion() >= 2 || preferHub)) {
            plugin.getLogger().info("Decision Dome using the MCC hub venue (api v" + v.apiVersion() + ").");
            return new HubDialView(plugin, v, "hub-" + voteCounter);
        }
        if (arena.ensureWorld() == null) {
            plugin.getLogger().warning("No hub venue and no fallback world; the vote will fail safe.");
        }
        return new FallbackDialView(plugin, arena, "dome-" + voteCounter);
    }

    private void ensureTask() {
        if (task != null) return;
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    private void tick() {
        DomeSession s = active;
        if (s == null) { stopTaskIfIdle(); return; }
        s.tickSafe();
        if (active == null) stopTaskIfIdle();
    }

    void sessionFinished(DomeSession session) {
        if (active == session) active = null;
        stopTaskIfIdle();
    }

    private void stopTaskIfIdle() {
        if (active == null && task != null) {
            task.cancel();
            task = null;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Team snapshots for the v1 fallback
    // ---------------------------------------------------------------------------------------------

    private Map<UUID, String> teamsSnapshot() {
        // The full v2 request supplies the real teams. On the v1 id-only path, make human-sized
        // groups so the fallback dome still has pods to throw from.
        return syntheticTeams();
    }

    private Map<UUID, String> syntheticTeams() {
        List<Player> online = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.isDead() && p.getGameMode() != org.bukkit.GameMode.SPECTATOR) online.add(p);
        }
        Map<UUID, String> teams = new LinkedHashMap<>();
        int n = 1;
        for (int i = 0; i < online.size(); i++) {
            if (i > 0 && i % 4 == 0) n++;
            teams.put(online.get(i).getUniqueId(), "Team " + n);
        }
        return teams;
    }
}
