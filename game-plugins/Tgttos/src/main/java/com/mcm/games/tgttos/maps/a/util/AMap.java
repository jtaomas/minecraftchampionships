package com.mcm.games.tgttos.maps.a.util;

import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;

/**
 * Base for builder-A maps: {@link #generate} returns the block data plus the design record for the offline
 * validators; {@link #build} runs the generator and the connection post-pass.
 */
public abstract class AMap implements TgMap {

    /** Draw everything into {@code g} (blocks, spawns, regions, design lines). */
    protected abstract void draw(Gen g);

    public final Gen generate(long seed) {
        Gen g = new Gen(new TgMapData(id(), seed));
        draw(g);
        PostPass.run(g.b);
        g.design.voidY = g.d.voidY;
        g.design.bridgingKit = kit().bridging();
        return g;
    }

    @Override
    public final TgMapData build(long seed) { return generate(seed).d; }
}
