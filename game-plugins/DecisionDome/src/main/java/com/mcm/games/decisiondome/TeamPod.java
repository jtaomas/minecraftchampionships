package com.mcm.games.decisiondome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** A team's room around the dial. Pure model; the venue owns the blocks. */
public final class TeamPod {
    private final int index;
    private final String teamKey;
    private final int colorIndex;
    private final List<UUID> members = new ArrayList<>();
    private final Set<UUID> grantedEggs = new LinkedHashSet<>();
    private boolean gateOpen = true;
    private String state = "normal";
    private boolean dunked;
    private long frozenUntil;
    private long cagedUntil;
    private int extraEggs;

    public TeamPod(int index, String teamKey, int colorIndex) {
        this.index = index;
        this.teamKey = teamKey == null ? "" : teamKey;
        this.colorIndex = colorIndex;
    }

    public int index() { return index; }
    public String teamKey() { return teamKey; }
    public int colorIndex() { return colorIndex; }
    public List<UUID> members() { return Collections.unmodifiableList(members); }
    public Set<UUID> grantedEggs() { return grantedEggs; }
    public boolean gateOpen() { return gateOpen; }
    public String state() { return state; }
    public boolean dunked() { return dunked; }
    public long frozenUntil() { return frozenUntil; }
    public long cagedUntil() { return cagedUntil; }
    public int extraEggs() { return extraEggs; }

    public void addMember(UUID uuid) {
        if (uuid != null && !members.contains(uuid)) members.add(uuid);
    }

    public void gateOpen(boolean open) { this.gateOpen = open; }

    public void state(String state) { this.state = state == null ? "normal" : state; }

    public void dunk() {
        this.dunked = true;
        this.state = "dunked";
        this.gateOpen = false;
    }

    public void freeze(long untilTick) {
        this.frozenUntil = Math.max(frozenUntil, untilTick);
        this.state = "frozen";
    }

    public void cage(long untilTick) {
        this.cagedUntil = Math.max(cagedUntil, untilTick);
        this.state = "caged";
        this.gateOpen = false;
    }

    public boolean frozen(long tick) { return frozenUntil > tick; }

    public boolean caged(long tick) { return cagedUntil > tick; }

    public void addExtraEggs(int n) { extraEggs += Math.max(0, n); }

    public int takeExtraEgg() {
        if (extraEggs <= 0) return 0;
        extraEggs--;
        return 1;
    }

    public void markGranted(UUID uuid) { if (uuid != null) grantedEggs.add(uuid); }

    public boolean granted(UUID uuid) { return grantedEggs.contains(uuid); }

    public void reset() {
        gateOpen = true;
        state = "normal";
        dunked = false;
        frozenUntil = 0;
        cagedUntil = 0;
        extraEggs = 0;
        grantedEggs.clear();
    }

    @Override
    public String toString() {
        return "#" + index + " " + (teamKey.isBlank() ? "Solos" : teamKey) + " (" + members.size() + ")";
    }
}
