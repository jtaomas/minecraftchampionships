package com.mcm.games.parkourwarrior.map;

/** Temporary placeholder obstacle (replaced zone by zone). */
final class Stub {
    private Stub() {}
    static Ob make(W w, Ob.Slot s, String name) {
        Ob o = new Ob(w, s, name + " (stub " + s.id() + ")", "STUB" + s.id());
        for (int i = 0; i < 5; i++) {
            int z = 4 + i * 4;
            o.fill(-1, -3, z, 1, 0, z + 1, "stone");
            o.area(-1, 0, z, 1, z + 1);
        }
        o.end(0, 0, 26);
        return o;
    }
}
