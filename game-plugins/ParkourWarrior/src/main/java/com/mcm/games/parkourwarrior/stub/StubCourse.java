package com.mcm.games.parkourwarrior.stub;

import com.mcm.games.parkourwarrior.CourseModel;
import com.mcm.games.parkourwarrior.CourseModel.BlockPos;
import com.mcm.games.parkourwarrior.CourseModel.Box;
import com.mcm.games.parkourwarrior.CourseModel.Difficulty;
import com.mcm.games.parkourwarrior.CourseModel.Hub;
import com.mcm.games.parkourwarrior.CourseModel.Kind;
import com.mcm.games.parkourwarrior.CourseModel.Obstacle;
import com.mcm.games.parkourwarrior.CourseModel.Pos;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal stand-in course (pure Java) used until / unless {@code com.mcm.games.parkourwarrior.map.CourseMap} exists,
 * or with config {@code map.generator: stub}. Plain stepping stones along +X with hubs, bonus paths along +Z,
 * a fork and three short endings; obstacle M4-2 is timed. Only for testing the mechanics, not the real design.
 */
public final class StubCourse {
    private StubCourse() {}

    private static final int Y = 99;            // floor block y; feet at 100
    private static final String PAD = "minecraft:lime_glazed_terracotta";
    private static final String HUB = "minecraft:smooth_quartz";
    private static final String[] ZONES = {"Toy Box", "Market Rooftops", "Shipwreck in the Sky", "Clockwork Workshop", "Crystal Mine"};
    private static final String[] STONE = {"minecraft:red_concrete", "minecraft:orange_terracotta", "minecraft:spruce_planks",
            "minecraft:copper_block", "minecraft:amethyst_block"};

    public static CourseModel generate(long seed) {
        CourseModel m = new CourseModel(seed);
        int spacing = 60;
        for (int s = 1; s <= 5; s++) {
            int hx = (s - 1) * spacing;
            platform(m, hx - 3, hx + 3, -3, 3, HUB);
            m.hubs.add(new Hub(s, Pos.block(hx, Y + 1, 0, -90), Pos.block(hx, Y + 1, 3, 0), ZONES[s - 1]));
            m.labels.add(new CourseModel.Label(new Pos(hx + 0.5, Y + 4, 0.5, 0), "&e&l" + ZONES[s - 1]));
            // main obstacles
            int x = hx + 3;
            Pos entry = Pos.block(hx, Y + 1, 0, -90);
            for (int i = 1; i <= 3; i++) {
                int stones = i == 3 ? 8 : 3;
                List<BlockPos> st = new ArrayList<>();
                for (int k = 0; k < stones; k++) { x += 3; st.add(new BlockPos(x, Y, 0)); }
                boolean timed = s == 4 && i == 2;
                String id = CourseModel.mainId(s, i);
                if (timed) {
                    for (int k = 0; k < st.size(); k++)
                        m.timed.add(new CourseModel.Timed(id, List.of(st.get(k)), "minecraft:copper_grate", 60, 40, k * 15, 20,
                                "minecraft:light_blue_stained_glass", null));
                } else for (BlockPos b : st) m.blocks.set(b.x(), b.y(), b.z(), STONE[s - 1]);
                Box pad;
                Pos next;
                if (i < 3) {
                    int px = x + 3;
                    platform(m, px, px + 2, -1, 1, PAD);
                    pad = new Box(px, Y, -1, px + 2, Y + 2, 1);
                    next = Pos.block(px + 1, Y + 1, 0, -90);
                    x = px + 2;
                } else {
                    int nhx = s * spacing;
                    if (s == 5) {
                        pad = new Box(nhx - 4, Y, -10, nhx + 4, Y + 2, 10);
                        next = Pos.block(nhx, Y + 1, 0, -90);
                    } else {
                        pad = new Box(nhx - 3, Y, -3, nhx + 3, Y + 2, 3);
                        next = Pos.block(nhx, Y + 1, 0, -90);
                    }
                }
                m.obstacles.add(new Obstacle(id, ZONES[s - 1] + " Run " + i, Kind.MAIN, s, i, null, entry, pad, Y - 6,
                        new Pos(entry.x() + 2, Y + 3, entry.z(), 0), new Box(entry.x() > 0 ? (int) entry.x() : 0, Y - 6, -2, x, Y + 6, 2)));
                entry = next;
            }
            // bonus path along +Z (gaps of 3)
            int z = 3;
            Pos be = Pos.block(hx, Y + 1, 3, 0);
            for (int i = 1; i <= 3; i++) {
                for (int k = 0; k < 3; k++) { z += 4; m.blocks.set(hx, Y + (k == 1 ? 1 : 0), z, "minecraft:gold_block"); }
                int pz = z + 4;
                platform(m, hx - 1, hx + 1, pz, pz + 2, PAD);
                Box pad = new Box(hx - 1, Y, pz, hx + 1, Y + 2, pz + 2);
                m.obstacles.add(new Obstacle(CourseModel.bonusId(s, i), ZONES[s - 1] + " Bonus " + i, Kind.BONUS, s, i, null, be, pad, Y - 6,
                        new Pos(be.x() + 1.5, Y + 3, be.z() + 2, 0), new Box(hx - 2, Y - 6, (int) be.z(), hx + 2, Y + 6, pz + 2)));
                be = Pos.block(hx, Y + 1, pz + 1, 0);
                z = pz + 2;
            }
        }
        // fork
        int fx = 5 * spacing;
        platform(m, fx - 4, fx + 4, -10, 10, HUB);
        m.fork = Pos.block(fx, Y + 1, 0, -90);
        Difficulty[] ds = Difficulty.values();
        String[] gate = {"minecraft:lime_concrete", "minecraft:orange_concrete", "minecraft:red_concrete"};
        int[] gap = {3, 4, 4};
        for (int d = 0; d < 3; d++) {
            int lz = -8 + d * 8;
            for (int y = Y + 1; y <= Y + 4; y++) { m.blocks.set(fx + 4, y, lz - 2, gate[d]); m.blocks.set(fx + 4, y, lz + 2, gate[d]); }
            for (int zz = lz - 2; zz <= lz + 2; zz++) m.blocks.set(fx + 4, Y + 5, zz, gate[d]);
            m.labels.add(new CourseModel.Label(new Pos(fx + 4.5, Y + 6.5, lz + 0.5, 0), (d == 0 ? "&a" : d == 1 ? "&6" : "&c") + "&l" + ds[d].name()));
            int x = fx + 4;
            Pos entry = Pos.block(fx + 3, Y + 1, lz, -90);
            for (int i = 1; i <= 2; i++) {
                for (int k = 0; k < 3; k++) { x += gap[d]; m.blocks.set(x, Y + (d == 2 ? k : 0), lz, gate[d]); }
                int px = x + gap[d] - 1;
                int py = Y + (d == 2 ? 2 : 0);
                platform(m, px, px + 2, lz - 1, lz + 1, PAD, py);
                Box pad = new Box(px, py, lz - 1, px + 2, py + 2, lz + 1);
                String id = CourseModel.endingId(ds[d], i);
                m.obstacles.add(new Obstacle(id, ds[d].name().charAt(0) + ds[d].name().substring(1).toLowerCase() + " Ending " + i,
                        Kind.ENDING, 0, i, ds[d], entry, pad, Y - 6, new Pos(entry.x() + 2, py + 3, lz + 0.5, 0),
                        new Box((int) entry.x(), Y - 6, lz - 2, px + 2, Y + 10, lz + 2)));
                if (i == 2) {
                    m.finishes.put(ds[d], pad);
                    m.finishStands.put(ds[d], Pos.block(px + 1, py + 1, lz, -90));
                }
                entry = Pos.block(px + 1, py + 1, lz, -90);
                x = px + 2;
            }
        }
        // start island = hub 1 area
        for (int i = 0; i < 8; i++) m.spawns.add(new Pos(-1.5 + (i % 4), Y + 1, -1.5 + (i / 4) * 3, -90));
        m.overviews.add(new CourseModel.Overview("Spine (top-down)", new Pos(150.5, Y + 80, 0.5, -90), 90));
        m.overviews.add(new CourseModel.Overview("Fork", new Pos(fx - 10.5, Y + 12, 0.5, -90), 30));
        m.notes.put("generator", "stub");
        m.recomputeBounds();
        return m;
    }

    private static void platform(CourseModel m, int x1, int x2, int z1, int z2, String b) { platform(m, x1, x2, z1, z2, b, Y); }

    private static void platform(CourseModel m, int x1, int x2, int z1, int z2, String b, int y) {
        for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) m.blocks.set(x, y, z, b);
    }
}
