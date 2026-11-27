package com.mcm.games.acerace.map.maps;

import com.mcm.games.acerace.map.RaceBuilder;
import com.mcm.games.acerace.map.RaceGeo.BoostPad;
import com.mcm.games.acerace.map.RaceGeo.BoostRing;
import com.mcm.games.acerace.map.RaceGeo.Dir;
import com.mcm.games.acerace.map.RaceGeo.EffectSpec;
import com.mcm.games.acerace.map.RaceGeo.Gate;
import com.mcm.games.acerace.map.RaceGeo.ItemSpec;
import com.mcm.games.acerace.map.RaceGeo.Loadout;
import com.mcm.games.acerace.map.RaceGeo.PadKind;
import com.mcm.games.acerace.map.RaceGeo.Region;
import com.mcm.games.acerace.map.RaceGeo.Section;
import com.mcm.games.acerace.map.RaceGeo.Spawn;
import com.mcm.games.acerace.map.RaceMap;
import com.mcm.games.acerace.map.RaceMapData;

import java.util.ArrayList;
import java.util.List;

/**
 * Built-in 3-lap MCC-style Ace Race circuit (Speedway -> Aqua Riptide Canal -> Elytra Sky Flight -> Emerald Terraces).
 * Used automatically when no external imported Ace Race world (such as mcc_clouds2) is installed on the server.
 */
public final class SkyCircuitMap implements RaceMap {
    public static final String ID = "skycircuit";

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Sky Circuit"; }
    @Override public String themeId() { return "skycircuit"; }
    @Override public int laps() { return 3; }
    @Override public int timeLimitSeconds() { return 600; }
    @Override public int countdownSeconds() { return 10; }
    @Override public int blockBudget() { return 120_000; }

    @Override
    public RaceMapData build(long seed) {
        RaceBuilder b = new RaceBuilder();

        // 1. Start Pen (x=-26..-7, z=-5..5, y=0) & Start Corner (x=-6..5, z=-5..5, y=0)
        b.floor(-26, 5, -5, 5, 0, "minecraft:quartz_block", "minecraft:cyan_concrete");
        for (int x = -26; x <= -8; x++) {
            for (int z = -4; z <= 4; z++) {
                b.set(x, 0, z, ((x + z) & 1) == 0 ? "minecraft:white_concrete" : "minecraft:light_gray_concrete");
            }
        }
        // Start barrier at x=-7 so the loop corner (x=-5..5, z=-5..5) stays unobstructed on laps 2-3
        for (int z = -5; z <= 5; z++) {
            for (int y = 1; y <= 4; y++) {
                b.barrier(-7, y, z, "minecraft:cyan_stained_glass");
            }
        }

        // 2. East Speedway Straight (x=6..135, z=-5..5): speed pads + jump pad over sky gap + elevated terrace
        b.floor(6, 56, -5, 5, 0, "minecraft:smooth_quartz", "minecraft:light_blue_concrete");
        b.fill(18, 0, -3, 25, 0, 3, "minecraft:yellow_glazed_terracotta"); // pad 0: speed
        b.fill(52, 0, -3, 56, 0, 3, "minecraft:lime_concrete");            // pad 1: jump over gap x=57..63
        // Elevated east terrace (x=64..136, z=-5..5, y=2)
        b.floor(64, 136, -5, 5, 2, "minecraft:smooth_quartz", "minecraft:gold_block");
        b.fill(88, 2, -3, 95, 2, 3, "minecraft:yellow_glazed_terracotta"); // pad 2: speed
        // Decorative arch at corner entry
        archYZ(b, 120, 2, 0, 6, "minecraft:sea_lantern", "minecraft:prismarine_bricks");

        // 3. South Aqua Riptide Canal (x=125..136, z=6..140)
        b.floor(125, 136, 6, 12, 2, "minecraft:prismarine_bricks", "minecraft:sea_lantern");
        // Water channel z=13..114 (prismarine floor at y=1, water at y=2, raised walls at y=2..3)
        b.fill(125, 1, 13, 136, 1, 114, "minecraft:prismarine");
        b.fill(126, 2, 13, 135, 2, 114, "minecraft:water");
        b.fill(125, 2, 13, 125, 3, 114, "minecraft:dark_prismarine");
        b.fill(136, 2, 13, 136, 3, 114, "minecraft:dark_prismarine");
        // Mid-canal lantern ribs + boost marker
        for (int z = 28; z <= 100; z += 24) {
            archXY(b, 130, 3, z, 6, "minecraft:sea_lantern", "minecraft:prismarine_bricks");
        }
        b.fill(127, 1, 56, 134, 1, 63, "minecraft:magma_block"); // visual marker under pad 3 (water boost)
        // Exit deck + Launch ramp up to Elytra Sky Tower (z=115..140)
        b.floor(125, 136, 115, 127, 2, "minecraft:prismarine_bricks", "minecraft:sea_lantern");
        b.fill(127, 2, 118, 134, 2, 125, "minecraft:orange_concrete"); // pad 4: launch up to tower
        // Safety stairs alongside launch pad in case a player walks up
        for (int s = 0; s < 16; s++) {
            b.fill(135, 2 + s, 115 + s, 136, 2 + s, 115 + s, "minecraft:quartz_block");
        }
        // Elytra Sky Tower Deck (x=114..136, z=128..140, y=18)
        b.floor(114, 136, 128, 140, 18, "minecraft:white_concrete", "minecraft:gold_block");
        b.fill(115, 18, 130, 121, 18, 138, "minecraft:orange_concrete"); // pad 5: elytra launch west

        // 4. West Elytra Sky Flight (x=113..12, z=128..140) with 4 glowing boost rings + safety cloud islands
        b.ringYZ(96, 20, 134, 5, "minecraft:gold_block");
        b.ringYZ(72, 18, 134, 5, "minecraft:sea_lantern");
        b.ringYZ(48, 15, 134, 5, "minecraft:gold_block");
        b.ringYZ(26, 12, 134, 5, "minecraft:sea_lantern");
        // Safety cloud stepping platforms below flight path at y=6
        for (int x = 18; x <= 102; x += 20) {
            b.floor(x, x + 8, 131, 137, 6, "minecraft:white_wool", "minecraft:light_blue_concrete");
            b.fill(x + 2, 6, 132, x + 6, 6, 136, "minecraft:lime_concrete");
        }
        // Cloud Landing Deck (x=-6..14, z=124..144, y=6)
        b.floor(-6, 14, 124, 144, 6, "minecraft:snow_block", "minecraft:diamond_block");

        // 5. North Emerald Terraces Return (x=-5..5, z=123..4) stepping down y=6 -> 4 -> 2 -> 0
        b.floor(-5, 5, 96, 123, 6, "minecraft:smooth_quartz", "minecraft:emerald_block");
        b.fill(-3, 6, 102, 3, 6, 108, "minecraft:yellow_glazed_terracotta"); // pad 6: speed north
        b.floor(-5, 5, 66, 95, 4, "minecraft:smooth_quartz", "minecraft:emerald_block");
        b.fill(-3, 4, 66, 3, 4, 71, "minecraft:lime_concrete");              // pad 7: jump over gap z=65..58
        b.floor(-5, 5, 32, 57, 2, "minecraft:smooth_quartz", "minecraft:emerald_block");
        b.fill(-3, 2, 38, 3, 2, 44, "minecraft:yellow_glazed_terracotta");   // pad 8: speed north
        b.floor(-5, 5, 6, 31, 0, "minecraft:smooth_quartz", "minecraft:cyan_concrete");
        b.fill(-3, 0, 16, 3, 0, 22, "minecraft:yellow_glazed_terracotta");   // pad 9: final sprint boost

        // Finish line checkerboard at z=4..5 & finish gantry
        for (int x = -5; x <= 5; x++) {
            b.set(x, 0, 4, (x & 1) == 0 ? "minecraft:white_concrete" : "minecraft:black_concrete");
            b.set(x, 0, 5, (x & 1) == 0 ? "minecraft:black_concrete" : "minecraft:white_concrete");
        }
        archXY(b, 0, 0, 4, 6, "minecraft:gold_block", "minecraft:sea_lantern");

        // 40 unique start grid slots (8 rows x 5 lanes) facing East (-90 yaw)
        List<Spawn> grid = new ArrayList<>();
        for (int row = 0; row < 8; row++) {
            int x = -9 - row * 2;
            for (int lane = -4; lane <= 4; lane += 2) {
                grid.add(new Spawn(x + 0.5, 1.0, lane + 0.5, -90f));
            }
        }

        Region startStrip = new Region(-26, 0, -5, -7, 4, 5);
        Spawn startSpawn = new Spawn(-9.5, 1.0, 0.5, -90f);

        Loadout sprintLoadout = new Loadout(true, List.of(),
                List.of(new EffectSpec("speed", 20 * 600, 1), new EffectSpec("saturation", 20 * 600, 10)));
        Loadout aquaLoadout = new Loadout(true, List.of(
                new ItemSpec("TRIDENT", 1, "&b&lRiptide Trident", List.of("riptide:2"), true, 0),
                new ItemSpec("DIAMOND_BOOTS", 1, "&b&lAqua Boots", List.of("depth_strider:3"), true, 36)
        ), List.of(
                new EffectSpec("dolphins_grace", 20 * 600, 1),
                new EffectSpec("water_breathing", 20 * 600, 1),
                new EffectSpec("saturation", 20 * 600, 10)
        ));
        Loadout elytraLoadout = new Loadout(true, List.of(
                new ItemSpec("ELYTRA", 1, "&e&lSky Wings", List.of(), true, 38)
        ), List.of(
                new EffectSpec("saturation", 20 * 600, 10)
        ));

        // Pads (indices 0..14: main pads 0..9 + safety cloud jump pads 10..14)
        List<BoostPad> pads = List.of(
                new BoostPad(0, new Region(18, 0, -3, 25, 2, 3), PadKind.SPEED, 1, 0, 0, 0.85, 15, null),
                new BoostPad(1, new Region(52, 0, -3, 56, 2, 3), PadKind.JUMP, 0.82, 0.60, 0, 1.35, 15, null),
                new BoostPad(2, new Region(88, 2, -3, 95, 4, 3), PadKind.SPEED, 1, 0, 0, 0.85, 15, null),
                new BoostPad(3, new Region(126, 1, 56, 135, 4, 63), PadKind.SPEED, 0, 0, 1, 0.90, 15, null),
                new BoostPad(4, new Region(127, 2, 118, 134, 4, 125), PadKind.LAUNCH, 0, 0.94, 0.34, 2.05, 20, null),
                new BoostPad(5, new Region(115, 18, 130, 121, 20, 138), PadKind.LAUNCH, -0.88, 0.48, 0, 1.65, 20, elytraLoadout),
                new BoostPad(6, new Region(-3, 6, 102, 3, 8, 108), PadKind.SPEED, 0, 0, -1, 0.85, 15, null),
                new BoostPad(7, new Region(-3, 4, 66, 3, 6, 71), PadKind.JUMP, 0, 0.58, -0.82, 1.25, 15, null),
                new BoostPad(8, new Region(-3, 2, 38, 3, 4, 44), PadKind.SPEED, 0, 0, -1, 0.85, 15, null),
                new BoostPad(9, new Region(-3, 0, 16, 3, 2, 22), PadKind.SPEED, 0, 0, -1, 0.90, 15, null),
                new BoostPad(10, new Region(99, 6, 132, 105, 8, 136), PadKind.LAUNCH, -0.82, 0.62, 0, 1.50, 15, elytraLoadout),
                new BoostPad(11, new Region(79, 6, 132, 85, 8, 136), PadKind.LAUNCH, -0.82, 0.62, 0, 1.50, 15, elytraLoadout),
                new BoostPad(12, new Region(59, 6, 132, 65, 8, 136), PadKind.LAUNCH, -0.82, 0.62, 0, 1.50, 15, elytraLoadout),
                new BoostPad(13, new Region(39, 6, 132, 45, 8, 136), PadKind.LAUNCH, -0.82, 0.62, 0, 1.50, 15, elytraLoadout),
                new BoostPad(14, new Region(19, 6, 132, 25, 8, 136), PadKind.LAUNCH, -0.82, 0.62, 0, 1.45, 15, elytraLoadout)
        );

        // Elytra Boost Rings (indices 0..3)
        List<BoostRing> rings = List.of(
                new BoostRing(0, 96, 20, 134, -1, 0, 0, 5.2, 0.90, false, 15),
                new BoostRing(1, 72, 18, 134, -1, 0, 0, 5.2, 0.90, false, 15),
                new BoostRing(2, 48, 15, 134, -1, 0, 0, 5.2, 0.90, false, 15),
                new BoostRing(3, 26, 12, 134, -1, 0, 0, 5.2, 0.90, false, 15)
        );

        // Sequential directional gates around the loop
        Gate startGate = new Gate(new Region(6, -1, -6, 8, 6, 6), Dir.EAST);
        Gate cp1 = new Gate(new Region(124, 1, 7, 137, 8, 10), Dir.SOUTH);
        Gate cp2 = new Gate(new Region(122, 17, 127, 125, 26, 141), Dir.WEST);
        Gate cp3 = new Gate(new Region(-7, 5, 122, 15, 18, 125), Dir.NORTH);
        Gate cp4 = new Gate(new Region(-6, -1, 10, 6, 7, 13), Dir.NORTH);
        Gate finish = new Gate(new Region(-6, -1, 3, 6, 7, 6), Dir.NORTH);

        // Remember: section[k] holds pads/rings encountered while heading toward section[k].gate()
        List<Section> sections = List.of(
                new Section(0, "Start Straight", startGate, startSpawn, sprintLoadout, sprintLoadout, -12, List.of(), List.of()),
                new Section(1, "Speedway", cp1, new Spawn(130.5, 3.0, 9.5, 0f), aquaLoadout, aquaLoadout, -12, List.of(0, 1, 2), List.of()),
                new Section(2, "Aqua Canal", cp2, new Spawn(122.5, 19.0, 134.5, 90f), elytraLoadout, elytraLoadout, -12, List.of(3, 4), List.of()),
                new Section(3, "Sky Flight", cp3, new Spawn(0.5, 7.0, 122.5, 180f), sprintLoadout, sprintLoadout, -12, List.of(5, 10, 11, 12, 13, 14), List.of(0, 1, 2, 3)),
                new Section(4, "Emerald Terraces", cp4, new Spawn(0.5, 1.0, 14.5, 180f), sprintLoadout, sprintLoadout, -12, List.of(6, 7, 8, 9), List.of())
        );

        Region playBounds = new Region(-34, -15, -16, 148, 45, 156);
        List<Spawn> overviews = List.of(new Spawn(65, 32, 68, 135f));
        Region spectatorBox = new Region(-34, -10, -16, 148, 45, 156);

        return new RaceMapData(ID, seed, b.blocks(), laps(), timeLimitSeconds(), countdownSeconds(),
                grid, startStrip, b.barrier(),
                sections, finish, true, startSpawn,
                pads, rings, List.of(), playBounds, overviews, spectatorBox,
                -15, blockBudget());
    }

    private static void archYZ(RaceBuilder b, int x, int baseY, int cz, int halfW, String accent, String body) {
        for (int y = baseY + 1; y <= baseY + 7; y++) {
            b.set(x, y, cz - halfW, (y & 1) == 0 ? accent : body);
            b.set(x, y, cz + halfW, (y & 1) == 0 ? accent : body);
        }
        for (int z = cz - halfW; z <= cz + halfW; z++) {
            b.set(x, baseY + 7, z, ((z & 1) == 0) ? accent : body);
        }
    }

    private static void archXY(RaceBuilder b, int cx, int baseY, int z, int halfW, String accent, String body) {
        for (int y = baseY + 1; y <= baseY + 7; y++) {
            b.set(cx - halfW, y, z, (y & 1) == 0 ? accent : body);
            b.set(cx + halfW, y, z, (y & 1) == 0 ? accent : body);
        }
        for (int x = cx - halfW; x <= cx + halfW; x++) {
            b.set(x, baseY + 7, z, ((x & 1) == 0) ? accent : body);
        }
    }
}
