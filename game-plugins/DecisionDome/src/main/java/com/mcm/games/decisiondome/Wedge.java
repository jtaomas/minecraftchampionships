package com.mcm.games.decisiondome;

/** One slot on the dial. A slot can be live, won, removed (blank) or empty. */
public final class Wedge {
    public enum State { ACTIVE, WINNER, REMOVED, BLANK, HIGHLIGHT }

    private final int slot;
    private String gameId;
    private final int colorIndex;
    private State state = State.ACTIVE;
    private int score;
    private int chickens;
    private int frogs;
    private int eggs;

    Wedge(int slot, String gameId, int colorIndex) {
        this.slot = slot;
        this.gameId = gameId;
        this.colorIndex = colorIndex;
    }

    public int slot() { return slot; }

    public String gameId() { return gameId; }

    public int colorIndex() { return colorIndex; }

    public State state() { return state; }

    public void state(State state) { this.state = state; }

    /** A slot is choosable only while it still carries a live game. */
    public boolean live() { return gameId != null && state == State.ACTIVE; }

    public boolean removed() { return state == State.REMOVED || state == State.BLANK; }

    public int score() { return score; }

    public int chickens() { return chickens; }

    public int frogs() { return frogs; }

    public int eggs() { return eggs; }

    void resetTally() { score = 0; chickens = 0; frogs = 0; eggs = 0; }

    void addVote(int value, boolean frog) {
        score += value;
        if (frog) frogs++;
        else chickens++;
    }

    void addEgg() { eggs++; }

    /** Remove the game from the dial, keeping the slot's colour for the blank board. */
    void remove() {
        state = State.REMOVED;
    }

    /** Fill a blank/removed slot with a new game from the refill pool. */
    void refill(String newGameId) {
        this.gameId = newGameId;
        this.state = State.ACTIVE;
        resetTally();
    }

    void markWinner() { state = State.WINNER; }

    void clearForNextDial() {
        this.gameId = null;
        this.state = State.BLANK;
        resetTally();
    }
}
