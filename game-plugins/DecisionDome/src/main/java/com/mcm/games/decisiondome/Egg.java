package com.mcm.games.decisiondome;

import java.util.UUID;

/** One throwable vote payload. The value is fixed at throw time. */
public final class Egg {
    public enum Kind {
        CHICKEN(1, "Chicken"),
        FROG(-1, "Frog"),
        MEGA_CHICKEN(3, "Mega Chicken"),
        MEGA_FROG(-3, "Mega Frog");

        private final int value;
        private final String label;

        Kind(int value, String label) {
            this.value = value;
            this.label = label;
        }

        public int value() { return value; }
        public String label() { return label; }
        public boolean frog() { return this == FROG || this == MEGA_FROG; }
        public boolean mega() { return this == MEGA_CHICKEN || this == MEGA_FROG; }
    }

    public enum State { THROWN, LANDED, HATCHED, LOST, CANCELLED }

    private final UUID id;
    private final UUID owner;
    private final String team;
    private final Kind kind;
    private final long thrownTick;
    private State state = State.THROWN;
    private double x;
    private double z;

    public Egg(UUID id, UUID owner, String team, Kind kind, long thrownTick, double x, double z) {
        this.id = id;
        this.owner = owner;
        this.team = team == null ? "" : team;
        this.kind = kind == null ? Kind.CHICKEN : kind;
        this.thrownTick = thrownTick;
        this.x = x;
        this.z = z;
    }

    public UUID id() { return id; }
    public UUID owner() { return owner; }
    public String team() { return team; }
    public Kind kind() { return kind; }
    public int value() { return kind.value(); }
    public long thrownTick() { return thrownTick; }
    public State state() { return state; }
    public double x() { return x; }
    public double z() { return z; }

    public void land(double x, double z) {
        this.x = x;
        this.z = z;
        this.state = State.LANDED;
    }

    public void hatched() { this.state = State.HATCHED; }

    public void lost() { this.state = State.LOST; }

    public void cancel() { this.state = State.CANCELLED; }

    public boolean inFlight() { return state == State.THROWN; }
}
