package com.mcm.games.decisiondome;

import java.util.UUID;

/** One power-up instance held by a team. Definitions live in {@link PowerUpRegistry}. */
public final class PowerUp {
    public enum Category { SPELL, CURSE, RELIC, EGG }
    public enum State { AVAILABLE, ACTIVE, USED, CANCELLED }

    private final String id;
    private final String displayName;
    private final Category category;
    private final String team;
    private final UUID caster;
    private int charges;
    private long startTick;
    private long endTick;
    private State state = State.AVAILABLE;

    public PowerUp(String id, String displayName, Category category, String team, UUID caster, int charges) {
        this.id = id;
        this.displayName = displayName;
        this.category = category;
        this.team = team == null ? "" : team;
        this.caster = caster;
        this.charges = Math.max(1, charges);
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Category category() { return category; }
    public String team() { return team; }
    public UUID caster() { return caster; }
    public int charges() { return charges; }
    public long startTick() { return startTick; }
    public long endTick() { return endTick; }
    public State state() { return state; }
    public boolean available() { return state == State.AVAILABLE && charges > 0; }

    public void activate(long now, long duration) {
        this.state = State.ACTIVE;
        this.startTick = now;
        this.endTick = now + Math.max(0, duration);
    }

    public boolean expired(long tick) { return state == State.ACTIVE && endTick > 0 && tick >= endTick; }

    public void expire() { this.state = State.USED; }

    public void cancel() { this.state = State.CANCELLED; this.charges = 0; }

    /** Consume one charge; returns true when a charge was actually spent. */
    public boolean spend() {
        if (charges <= 0) return false;
        charges--;
        if (charges <= 0) state = State.USED;
        return true;
    }

    public void charges(int charges) { this.charges = Math.max(0, charges); }

    @Override
    public String toString() {
        return displayName + "(" + team + ", " + charges + ")";
    }
}
