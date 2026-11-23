package com.mcm.games.tgttos.stub;

import com.mcm.games.tgttos.model.Kit;
import com.mcm.games.tgttos.model.MapType;
import com.mcm.games.tgttos.model.TgMap;
import com.mcm.games.tgttos.model.TgMapData;
import com.mcm.games.tgttos.model.TgMapData.Region;
import com.mcm.games.tgttos.model.TgMapData.Spawn;

/**
 * Development stub: flat start strip, 30-block void gap with a stepping-stone line, goal island with a pen
 * and a landmark. Also demonstrates a variant group. Not meant for real play.
 */
public final class StubBridgeMap implements TgMap {

    @Override public String id() { return "stub_bridge"; }
    @Override public String displayName() { return "Stub Bridge"; }
    @Override public MapType type() { return MapType.BRIDGE; }
    @Override public Kit kit() { return Kit.CONCRETE_PICKAXE_SNOWBALLS; }

    @Override
    public TgMapData build(long seed) {
        TgMapData d = new TgMapData(id(), seed);
        // start strip: x -18..17, z 0..6, floor y 0
        d.fill(-18, 0, 0, 17, 0, 6, "minecraft:smooth_stone");
        d.fill(-19, 0, -1, 18, 0, -1, "minecraft:polished_andesite");
        d.fill(-19, 1, -1, 18, 2, -1, "minecraft:glass");
        for (int z = 0; z <= 6; z++) {
            d.blocks.set(-19, 1, z, "minecraft:glass"); d.blocks.set(-19, 2, z, "minecraft:glass");
            d.blocks.set(18, 1, z, "minecraft:glass"); d.blocks.set(18, 2, z, "minecraft:glass");
        }
        d.startStrip = Region.of(-18, 1, 0, 17, 4, 6);
        for (int x = -18; x <= 17; x++) {
            for (int y = 1; y <= 3; y++) {
                d.blocks.set(x, y, 7, "minecraft:white_stained_glass");
                d.startBarrier.add(new TgMapData.Pos(x, y, 7));
            }
        }
        d.fill(-18, 0, 7, 17, 0, 7, "minecraft:polished_andesite");
        for (int i = 0; i < 40; i++) {
            double x = -17.5 + (i % 20) * 1.8;
            double z = 2.5 + (i / 20) * 2;
            d.startSpawns.add(new Spawn(x, 1, z, 0f));
        }
        // stepping stones (the built line): 1x1 posts 3 apart on the left
        for (int z = 10; z <= 36; z += 3) d.blocks.set(-10, 0, z, "minecraft:stone_bricks");
        // goal island z 38..54
        d.fill(-10, 0, 38, 10, 0, 54, "minecraft:grass_block");
        d.fill(-6, 1, 42, 6, 1, 42, "minecraft:oak_fence[east=true,west=true]");
        d.fill(-6, 1, 54, 6, 1, 54, "minecraft:oak_fence[east=true,west=true]");
        d.fill(-6, 1, 42, -6, 1, 54, "minecraft:oak_fence[north=true,south=true]");
        d.fill(6, 1, 42, 6, 1, 54, "minecraft:oak_fence[north=true,south=true]");
        d.fill(-1, 1, 42, 1, 1, 42, null); // pen entrance
        d.pen = Region.of(-5, 1, 43, 5, 3, 53);
        for (int i = 0; i < 8; i++) d.chickenSpawns.add(new Spawn(-4.5 + i * 1.3, 1, 48.5, 180f));
        d.fill(0, 1, 56, 0, 20, 56, "minecraft:gold_block"); // landmark
        // variant demo: a small block that is either a step or nothing
        TgMapData.VariantGroup g = d.variant("demo_step", "demo");
        g.add("ON", 0, 0, 20, "minecraft:stone_bricks");
        g.add("OFF", 0, 0, 20, null);
        d.variantRules.add(new TgMapData.VariantRule("demo", "ON", 0, -1));

        d.voidY = -12;
        d.playBounds = Region.of(-40, -30, -10, 40, 40, 70);
        d.buildBounds = Region.of(-30, -5, 8, 30, 20, 40);
        d.overviewPoints.add(new Spawn(0, 18, -6, 0f, 30f));
        d.notes.put("stub", "development stub map");
        return d;
    }
}
