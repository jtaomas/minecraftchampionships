package com.mcm.games.decisiondome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * The vote board. It owns the wedge order, the refill pool and the removed-slot memory. Pure Java:
 * it never knows where the blocks are.
 */
public final class Dial {
    public static final int MIN_CAPACITY = 1;
    public static final int MAX_CAPACITY = 8;
    public static final int GAME_CEILING = 15;

    private final int capacity;
    private final DialGeometry geometry;
    private final List<Wedge> wedges = new ArrayList<>();
    private final List<String> pool = new ArrayList<>();
    private final List<String> allGames = new ArrayList<>();

    public Dial(List<String> gameIds, int capacity, DialGeometry geometry) {
        this(gameIds, capacity, 0, geometry);
    }

    /**
     * Build a dial with a fixed number of blank slots. This is how a chosen game's wedge stays blank:
     * before the refill vote, the dial still has {@code capacity} wedges but the removed slots are
     * empty. Slots are filled in playlist order from the remaining ids.
     */
    public Dial(List<String> gameIds, int capacity, int blankSlots, DialGeometry geometry) {
        int cap = Math.max(MIN_CAPACITY, Math.min(MAX_CAPACITY, capacity));
        LinkedHashSet<String> uniq = new LinkedHashSet<>();
        if (gameIds != null) {
            for (String id : gameIds) {
                if (id == null || id.isBlank()) continue;
                String norm = id.toLowerCase(Locale.ROOT);
                if (uniq.size() >= GAME_CEILING) break;
                uniq.add(norm);
            }
        }
        this.capacity = cap;
        this.geometry = geometry == null ? DialGeometry.orrery() : geometry;
        List<String> all = new ArrayList<>(uniq);
        this.allGames.addAll(all);
        int liveTarget = Math.max(0, cap - Math.max(0, Math.min(cap, blankSlots)));
        int assign = Math.min(liveTarget, all.size());
        for (int i = 0; i < cap; i++) {
            if (i < assign) {
                wedges.add(new Wedge(i, all.get(i), i));
            } else {
                Wedge blank = new Wedge(i, null, i);
                blank.state(Wedge.State.BLANK);
                wedges.add(blank);
            }
        }
        for (int i = assign; i < all.size(); i++) pool.add(all.get(i));
    }

    public int capacity() { return capacity; }

    public int size() { return wedges.size(); }

    public DialGeometry geometry() { return geometry; }

    public List<Wedge> wedges() { return Collections.unmodifiableList(wedges); }

    public Wedge wedge(int slot) {
        if (slot < 0 || slot >= wedges.size()) return null;
        return wedges.get(slot);
    }

    public boolean isLive(int slot) {
        Wedge w = wedge(slot);
        return w != null && w.live();
    }

    public int wedgeAt(double x, double z) {
        return geometry.wedgeAt(x, z, wedges.size());
    }

    public List<Wedge> liveWedges() {
        List<Wedge> out = new ArrayList<>();
        for (Wedge w : wedges) if (w.live()) out.add(w);
        return out;
    }

    public List<Integer> liveSlots() {
        List<Integer> out = new ArrayList<>();
        for (Wedge w : wedges) if (w.live()) out.add(w.slot());
        return out;
    }

    public int liveCount() { return liveWedges().size(); }

    public List<String> onDialGames() {
        List<String> out = new ArrayList<>();
        for (Wedge w : wedges) if (w.live()) out.add(w.gameId());
        return out;
    }

    public List<String> pool() { return Collections.unmodifiableList(pool); }

    public List<String> allGames() { return Collections.unmodifiableList(allGames); }

    public int indexOfGame(String gameId) {
        if (gameId == null) return -1;
        for (Wedge w : wedges) if (gameId.equalsIgnoreCase(w.gameId())) return w.slot();
        return -1;
    }

    /** Mark the winner's slot removed. Returns the game id that was removed, or null. */
    public String removeWinner(int slot) {
        Wedge w = wedge(slot);
        if (w == null || w.gameId() == null) return null;
        String removed = w.gameId();
        w.markWinner();
        w.remove();
        return removed;
    }

    /** Mark an arbitrary slot blank (administrative use). */
    public void blank(int slot) {
        Wedge w = wedge(slot);
        if (w != null) w.clearForNextDial();
    }

    /**
     * Fill every blank/removed slot from the refill pool in playlist order. Returns the games added.
     * Slots keep their original index, so the physical wedge stays where it was and just gets a new label.
     */
    public List<String> refill() {
        List<String> added = new ArrayList<>();
        for (Wedge w : wedges) {
            if (pool.isEmpty()) break;
            if (w.state() == Wedge.State.REMOVED || w.state() == Wedge.State.BLANK) {
                String next = pool.remove(0);
                w.refill(next);
                added.add(next);
            }
        }
        return added;
    }

    /** Reset all tally values before a tally. */
    public void resetTally() {
        for (Wedge w : wedges) w.resetTally();
    }

    public void resetBoard() {
        for (int i = 0; i < wedges.size(); i++) {
            Wedge w = wedges.get(i);
            w.state(Wedge.State.ACTIVE);
            w.resetTally();
        }
    }
}
