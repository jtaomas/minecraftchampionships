package com.mcm.games.acerace.map.maps;

import com.mcm.games.acerace.map.RaceBuilder;
import com.mcm.games.acerace.map.RaceGeo.BoostPad;
import com.mcm.games.acerace.map.RaceGeo.BoostRing;
import com.mcm.games.acerace.map.RaceGeo.Dir;
import com.mcm.games.acerace.map.RaceGeo.Gate;
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
 * Phase-1 test course: a flat rectangular circuit with a start pen, three checkpoints, a speed pad,
 * a jump pad and one boost ring. It exists to prove the contract, paste, runtime, HUD and spectator
 * flow before the real maps are decorated.
 */
public final class TestGridMap implements RaceMap {
    public static final String ID = "testgrid";

    @Override public String id() { return ID; }
    @Override public String displayName() { return "Test Grid"; }
    @Override public String themeId() { return "test"; }
    @Override public int laps() { return 1; }
    @Override public int timeLimitSeconds() { return 120; }
    @Override public int countdownSeconds() { return 5; }
    @Override public int blockBudget() { return 20_000; }

    @Override
    public RaceMapData build(long seed) {
        RaceBuilder b = new RaceBuilder();

        // Track loop, 7 wide, floor top at y=0. The start apron extends west so all 40 grid slots stand on it.
        b.floor(-24, 123, -3, 3, 0, "minecraft:white_concrete", "minecraft:black_concrete");
        b.floor(117, 123, -3, 63, 0, "minecraft:white_concrete", "minecraft:black_concrete");
        b.floor(-3, 123, 57, 63, 0, "minecraft:white_concrete", "minecraft:black_concrete");
        b.floor(-3, 3, -3, 63, 0, "minecraft:white_concrete", "minecraft:black_concrete");

        // Start apron: checkered.
        for (int x = -24; x <= -3; x++) {
            for (int z = -3; z <= 3; z++) {
                b.set(x, 0, z, ((x + z) & 1) == 0 ? "minecraft:white_concrete" : "minecraft:light_gray_concrete");
            }
        }

        // Speed pad and jump pad.
        b.fill(40, 0, -2, 46, 0, 2, "minecraft:yellow_glazed_terracotta");
        b.fill(80, 0, -2, 86, 0, 2, "minecraft:lime_concrete");

        // A visible gold hoop at x=70, in the Y/Z plane.
        b.ringYZ(70, 3, 0, 3, "minecraft:gold_block");

        // Finish line.
        for (int x = -3; x <= 3; x++) {
            b.set(x, 0, 1, (x & 1) == 0 ? "minecraft:white_concrete" : "minecraft:black_concrete");
            b.set(x, 0, 2, (x & 1) == 0 ? "minecraft:black_concrete" : "minecraft:white_concrete");
        }

        // Start barrier at x=-1, above the floor so clearing it cannot leave a hole.
        for (int z = -3; z <= 3; z++) {
            for (int y = 1; y <= 3; y++) {
                b.barrier(-1, y, z, "minecraft:light_blue_stained_glass");
            }
        }

        List<Spawn> grid = new ArrayList<>();
        for (int row = 0; row < 6; row++) {
            int x = -12 - row * 2;
            for (int lane = -3; lane <= 3; lane++) {
                grid.add(new Spawn(x, 1, lane, -90f)); // facing east
            }
        }
        while (grid.size() > 40) grid.remove(grid.size() - 1);

        Region startStrip = new Region(-23, 0, -4, -1, 3, 4);
        Spawn startSpawn = new Spawn(-12, 1, 0, -90f);

        Gate startGate = new Gate(new Region(-1, 0, -4, 0, 4, 4), Dir.EAST);
        Gate cp1 = new Gate(new Region(120, 0, -4, 121, 4, 4), Dir.EAST);
        Gate cp2 = new Gate(new Region(116, 0, 60, 124, 4, 61), Dir.NORTH);
        Gate cp3 = new Gate(new Region(-1, 0, 56, 0, 4, 64), Dir.WEST);
        Gate finish = new Gate(new Region(-4, 0, 1, 4, 4, 2), Dir.SOUTH);

        List<Section> sections = List.of(
                new Section(0, "Start", startGate, startSpawn, Loadout.EMPTY, Loadout.EMPTY, -10, List.of(), List.of()),
                new Section(1, "East Straight", cp1, new Spawn(110, 1, 0, -90f), Loadout.EMPTY, Loadout.EMPTY, -10, List.of(0, 1), List.of(0)),
                new Section(2, "North Straight", cp2, new Spawn(120, 1, 50, 180f), Loadout.EMPTY, Loadout.EMPTY, -10, List.of(), List.of()),
                new Section(3, "West Straight", cp3, new Spawn(5, 1, 60, 90f), Loadout.EMPTY, Loadout.EMPTY, -10, List.of(), List.of())
        );

        List<BoostPad> pads = List.of(
                new BoostPad(0, new Region(40, 0, -2, 46, 1, 2), PadKind.SPEED, 1, 0, 0, 0.60, 20, Loadout.EMPTY),
                new BoostPad(1, new Region(80, 0, -2, 86, 1, 2), PadKind.JUMP, 0, 1, 0, 0.80, 20, Loadout.EMPTY)
        );

        List<BoostRing> rings = List.of(
                new BoostRing(0, 70, 3, 0, 1, 0, 0, 3.0, 0.35, false, 20)
        );

        Region playBounds = new Region(-30, -5, -30, 150, 30, 90);
        List<Spawn> overviews = List.of(new Spawn(60, 20, 30, 180f));
        Region spectatorBox = new Region(30, 10, 10, 90, 30, 50);

        return new RaceMapData(ID, seed, b.blocks(), laps(), timeLimitSeconds(), countdownSeconds(), grid, startStrip, b.barrier(),
                sections, finish, false, startSpawn,
                pads, rings, List.of(), playBounds, overviews, spectatorBox,
                -10, blockBudget());
    }
}
