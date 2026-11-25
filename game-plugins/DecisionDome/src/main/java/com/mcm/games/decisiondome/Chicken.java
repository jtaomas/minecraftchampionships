package com.mcm.games.decisiondome;

import java.util.UUID;

/**
 * One logical vote. The visual entity is a slave of this object; the logical position is authoritative
 * at tally time. The position is allowed to cross wedge boundaries until the dividers are sealed.
 */
public final class Chicken {
    public enum State { WANDERING, SETTLED, COUNTED, REMOVED }

    private final UUID id;
    private final UUID owner;
    private final String team;
    private Egg.Kind kind;
    private final long hatchTick;
    private double x;
    private double z;
    private State state = State.WANDERING;
    private long frozenUntil;
    private long levitateUntil;
    private int scalePercent = 100;
    private long rngState;
    private int lastWedge = -2;

    public Chicken(UUID id, UUID owner, String team, Egg.Kind kind, long hatchTick, double x, double z, long rngSeed) {
        this.id = id;
        this.owner = owner;
        this.team = team == null ? "" : team;
        this.kind = kind == null ? Egg.Kind.CHICKEN : kind;
        this.hatchTick = hatchTick;
        this.x = x;
        this.z = z;
        this.rngState = rngSeed == 0 ? 0x9E3779B97F4A7C15L : rngSeed;
        this.scalePercent = this.kind.mega() ? 170 : 100;
    }

    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public String team() { return team; }
    public Egg.Kind kind() { return kind; }
    public int value() { return kind.value(); }
    public boolean frog() { return kind.frog(); }
    public boolean mega() { return kind.mega(); }
    public long hatchTick() { return hatchTick; }
    public double x() { return x; }
    public double z() { return z; }
    public State state() { return state; }
    public boolean alive() { return state != State.REMOVED; }
    public int scalePercent() { return scalePercent; }
    public long frozenUntil() { return frozenUntil; }
    public long levitateUntil() { return levitateUntil; }
    public int lastWedge() { return lastWedge; }

    public void position(double x, double z) { this.x = x; this.z = z; }

    public void wedge(int wedge) { this.lastWedge = wedge; }

    public void settle() { if (state == State.WANDERING) state = State.SETTLED; }

    public void count() { state = State.COUNTED; }

    public void remove() { state = State.REMOVED; }

    public void freeze(long untilTick) { this.frozenUntil = Math.max(frozenUntil, untilTick); }

    public void levitate(long untilTick) { this.levitateUntil = Math.max(levitateUntil, untilTick); }

    public boolean frozen(long tick) { return frozenUntil > tick; }

    public boolean levitating(long tick) { return levitateUntil > tick; }

    public void scale(int percent) { this.scalePercent = Math.max(50, Math.min(250, percent)); }

    public void kind(Egg.Kind kind) {
        if (kind == null) return;
        this.kind = kind;
        this.scalePercent = kind.mega() ? Math.max(170, scalePercent) : Math.min(100, scalePercent);
    }

    /** Per-chicken deterministic random in [0,1). */
    public double random() {
        long z = (rngState += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= (z >>> 31);
        return (z >>> 11) * 0x1.0p-53;
    }

    public long rngState() { return rngState; }

    @Override
    public String toString() {
        return kind.label() + "[" + String.format("%.2f,%.2f", x, z) + "]";
    }
}
