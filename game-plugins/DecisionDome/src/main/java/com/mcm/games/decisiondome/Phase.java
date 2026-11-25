package com.mcm.games.decisiondome;

/**
 * The pure vote clock. Durations are normal-mode ticks; fast-test mode clamps every phase to at most
 * one second by using {@link #fastTicks()}.
 */
public enum Phase {
    IDLE(0, 0),
    OPENING(5 * 20, 20),
    VOTING(30 * 20, 20),
    SETTLE(13 * 20, 20),
    DIVIDERS(30, 10),
    TALLY(20, 10),
    REVEAL(5 * 20, 20),
    REMOVE(2 * 20, 10),
    REFILL(2 * 20, 20),
    TELEPORT(8 * 20, 20),
    FINISHED(0, 0);

    private final int normalTicks;
    private final int fastTicks;

    Phase(int normalTicks, int fastTicks) {
        this.normalTicks = normalTicks;
        this.fastTicks = fastTicks;
    }

    public int normalTicks() { return normalTicks; }

    public int fastTicks() { return fastTicks; }

    /** Can a player still throw an egg in this phase? */
    public boolean acceptsEggs() { return this == VOTING; }

    /** In-flight eggs may still resolve here, and hatches are accepted. */
    public boolean acceptsHatch() { return this == VOTING || this == SETTLE; }

    /** Chickens are logically allowed to move. */
    public boolean movesChickens() { return this == OPENING || this == VOTING || this == SETTLE; }

    /** True for the three countdown groups shown on the one boss bar. */
    public String hudLabel() {
        return switch (this) {
            case OPENING -> "VOTING BEGINS IN";
            case VOTING -> "VOTING CLOSES IN";
            case SETTLE, DIVIDERS, TALLY -> "GAME CHOSEN IN";
            case REVEAL, REMOVE, REFILL, TELEPORT -> "TELEPORTING IN";
            default -> null;
        };
    }

    public boolean hatchingGroup() { return this == SETTLE || this == DIVIDERS || this == TALLY; }

    public boolean teleportGroup() { return this == REVEAL || this == REMOVE || this == REFILL || this == TELEPORT; }
}
