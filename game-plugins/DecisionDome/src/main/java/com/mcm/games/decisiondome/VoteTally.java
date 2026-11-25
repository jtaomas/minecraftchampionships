package com.mcm.games.decisiondome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Immutable result of one tally. The scores are signed; negative totals are legal and can win.
 * Ties are resolved uniformly among the leaders only (or among all live wedges when nobody voted).
 */
public final class VoteTally {
    public record Line(int slot, String gameId, int chickens, int frogs, int score, boolean live, boolean removed) {
        public String displayName() {
            return gameId == null ? "(blank)" : gameId;
        }
    }

    private final List<Line> lines;
    private final List<Integer> leaders;
    private final Map<String, Integer> byTeam;
    private final int winnerSlot;
    private final String winnerGameId;
    private final boolean tie;
    private final boolean emptyVotes;
    private final boolean audience;
    private final int tieRoll;
    private final int countedChickens;
    private final int ignoredOutside;
    private final int ignoredRemoved;
    private final int frogCount;
    private final int megaCount;

    private VoteTally(List<Line> lines, List<Integer> leaders, Map<String, Integer> byTeam,
                      int winnerSlot, String winnerGameId, boolean tie, boolean emptyVotes, boolean audience,
                      int tieRoll, int countedChickens, int ignoredOutside, int ignoredRemoved,
                      int frogCount, int megaCount) {
        this.lines = List.copyOf(lines);
        this.leaders = List.copyOf(leaders);
        this.byTeam = Collections.unmodifiableMap(new LinkedHashMap<>(byTeam));
        this.winnerSlot = winnerSlot;
        this.winnerGameId = winnerGameId;
        this.tie = tie;
        this.emptyVotes = emptyVotes;
        this.audience = audience;
        this.tieRoll = tieRoll;
        this.countedChickens = countedChickens;
        this.ignoredOutside = ignoredOutside;
        this.ignoredRemoved = ignoredRemoved;
        this.frogCount = frogCount;
        this.megaCount = megaCount;
    }

    /**
     * Count every live chicken once. A chicken on the podium, outside the rim or on a removed/blank
     * wedge contributes nothing and is logged.
     */
    public static VoteTally count(Dial dial, Iterable<Chicken> chickens, DomeRng tieRng, boolean audienceVote) {
        dial.resetTally();
        int counted = 0;
        int outside = 0;
        int removed = 0;
        int frogs = 0;
        int megas = 0;
        Map<String, Integer> byTeam = new LinkedHashMap<>();

        if (chickens != null) {
            for (Chicken chicken : chickens) {
                if (chicken == null || !chicken.alive()) continue;
                int slot = dial.wedgeAt(chicken.x(), chicken.z());
                chicken.wedge(slot);
                if (slot < 0) {
                    outside++;
                    continue;
                }
                Wedge wedge = dial.wedge(slot);
                if (wedge == null || !wedge.live()) {
                    removed++;
                    continue;
                }
                wedge.addVote(chicken.value(), chicken.frog());
                counted++;
                if (chicken.frog()) frogs++;
                if (chicken.mega()) megas++;
                byTeam.merge(chicken.team(), chicken.value(), Integer::sum);
            }
        }

        List<Wedge> live = dial.liveWedges();
        List<Integer> leaders = new ArrayList<>();
        boolean empty = counted == 0;
        boolean tie = false;
        int roll = 0;
        int winnerSlot = -1;
        String winnerGame = null;

        if (!live.isEmpty()) {
            if (empty) {
                for (Wedge w : live) leaders.add(w.slot());
            } else {
                int best = audienceVote ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                for (Wedge w : live) {
                    best = audienceVote ? Math.min(best, w.score()) : Math.max(best, w.score());
                }
                for (Wedge w : live) {
                    if (w.score() == best) leaders.add(w.slot());
                }
            }
            if (leaders.isEmpty()) {
                for (Wedge w : live) leaders.add(w.slot());
            }
            if (leaders.size() == 1) {
                winnerSlot = leaders.get(0);
            } else {
                tie = true;
                roll = tieRng == null ? 0 : tieRng.nextInt(leaders.size());
                winnerSlot = leaders.get(roll);
            }
            Wedge winner = dial.wedge(winnerSlot);
            winnerGame = winner == null ? null : winner.gameId();
        }

        List<Line> lines = new ArrayList<>();
        for (Wedge w : dial.wedges()) {
            lines.add(new Line(w.slot(), w.gameId(), w.chickens(), w.frogs(), w.score(),
                    w.live(), w.removed()));
        }

        return new VoteTally(lines, leaders, byTeam, winnerSlot, winnerGame, tie, empty, audienceVote,
                roll, counted, outside, removed, frogs, megas);
    }

    public List<Line> lines() { return lines; }
    public List<Integer> leaders() { return leaders; }
    public Map<String, Integer> byTeam() { return byTeam; }
    public int winnerSlot() { return winnerSlot; }
    public String winnerGameId() { return winnerGameId; }
    public boolean tie() { return tie; }
    public boolean emptyVotes() { return emptyVotes; }
    public boolean audience() { return audience; }
    public int tieRoll() { return tieRoll; }
    public int countedChickens() { return countedChickens; }
    public int ignoredOutside() { return ignoredOutside; }
    public int ignoredRemoved() { return ignoredRemoved; }
    public int frogCount() { return frogCount; }
    public int megaCount() { return megaCount; }
    public boolean hasWinner() { return winnerGameId != null; }

    public Line line(int slot) {
        for (Line l : lines) if (l.slot() == slot) return l;
        return null;
    }

    /** Ranked "Game n" list, highest first; negative scores keep their sign. */
    public String rankedText() {
        List<Line> ranked = new ArrayList<>(lines);
        ranked.sort((a, b) -> Integer.compare(b.score(), a.score()));
        StringBuilder sb = new StringBuilder();
        for (Line l : ranked) {
            if (l.gameId() == null) continue;
            if (sb.length() > 0) sb.append("§7, ");
            sb.append(l.live() ? "§f" : "§8").append(l.displayName()).append(" §7").append(l.score());
        }
        return sb.toString();
    }

    public String winnerText() {
        return winnerGameId == null ? "(no live game)" : winnerGameId;
    }

    public String debug() {
        return "counted=" + countedChickens + " outside=" + ignoredOutside + " removed=" + ignoredRemoved
                + " frogs=" + frogCount + " mega=" + megaCount + " scores=" + scoreList()
                + " winner=" + winnerGameId + " tie=" + tie + " empty=" + emptyVotes
                + (tie ? " leaders=" + leaders + " roll=" + tieRoll : "");
    }

    public String scoreList() {
        StringBuilder sb = new StringBuilder("[");
        for (Line l : lines) {
            if (sb.length() > 1) sb.append(", ");
            sb.append(l.slot()).append(':').append(l.gameId() == null ? "-" : l.gameId()).append('=').append(l.score());
        }
        return sb.append(']').toString();
    }
}
