package com.mcm.games.parkourtag;

import com.mcm.games.parkourtag.map.BlockSink;
import com.mcm.games.parkourtag.map.Cage;
import com.mcm.games.parkourtag.map.PtMap;

import java.util.List;

/** The old generated arena, wrapped as a map. Fallback only, used while no MCC map is finished. */
final class LegacyArenaMap implements PtMap {
    private final List<Cage> cages = List.of(
            new Cage(20, 1, 4, 0, 1, "polished_andesite", "polished_andesite"),
            new Cage(20, 1, ArenaBlueprint.W - 5, 0, -1, "polished_andesite", "polished_andesite"));

    @Override public String id() { return "legacy"; }
    @Override public String displayName() { return "Practice Arena"; }
    @Override public boolean finished() { return false; }
    @Override public int sizeX() { return ArenaBlueprint.W; }
    @Override public int sizeY() { return ArenaBlueprint.H + 2; }
    @Override public int sizeZ() { return ArenaBlueprint.W; }
    @Override public List<Cage> cages() { return cages; }
    @Override public double[] spectator() { return new double[]{20.5, ArenaBlueprint.H + 5, 20.5, 0, 90}; }

    @Override
    public void generate(BlockSink sink) {
        for (ArenaBlueprint.Placement p : ArenaBlueprint.generate()) sink.set(p.x(), p.y(), p.z(), p.data().getAsString());
        for (Cage c : cages) c.build(sink);
    }
}
