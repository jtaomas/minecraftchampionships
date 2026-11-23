package com.mcm.games.tgttos.maps.b;
import com.mcm.games.tgttos.maps.b.util.G;
import com.mcm.games.tgttos.model.*;
public final class FrozenCanal implements TgMap {
    @Override public String id() { return "x"; }
    @Override public String displayName() { return "x"; }
    @Override public MapType type() { return MapType.BRIDGE; }
    @Override public Kit kit() { return Kit.SNOWBALLS_ONLY; }
    @Override public TgMapData build(long seed) { return generate(seed).d; }
    public G generate(long seed) { return new G(new TgMapData(id(), seed)); }
}
