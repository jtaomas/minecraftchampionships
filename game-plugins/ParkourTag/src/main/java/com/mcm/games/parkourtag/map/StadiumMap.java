package com.mcm.games.parkourtag.map;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Stadium: daniellaur's Parkour Tag stadium ("daniellaur's minigames", MG-PT-01), private use only.
 *
 * <p>The blocks are the original arena, bowl seating and banner, read from the bundled resource
 * {@code /maps/stadium.mcmdump.gz} (MCMDUMP text with full block states, made by tools/stadium_extract.py and
 * tools/stadium_build.py). The command machine (121 + 2 command blocks, redstone, repeaters, pistons, buttons,
 * plates, signs) and the underground waiting lobby are stripped; our {@code ParkourTagGame} runs the round.
 *
 * <p>Local coordinates are the source world shifted by (-1035, -9, -972): the arena floor is y 1, the play box is
 * x 13..36, z 6..38 (the 20 x 20 course x 15..36 / z 12..33 plus the two start rooms), the gallery floor and the
 * barrier roof are y 18, the banner is x 0..1.
 * The two start rooms (north runner, south hunter) are rebuilt as our cages. Containment is the original shell:
 * cyan terracotta / stone walls to y 17, the invisible barrier roof over the course and the gallery floor around
 * it, so nothing leaves the arena. Spectators fly on the gallery above the roof.
 */
public final class StadiumMap implements PtMap {
    static final int SX = 44, SY = 50, SZ = 46;
    private static final String RESOURCE = "/maps/stadium.mcmdump.gz";
    private static List<Object[]> cache;

    private final List<Cage> cages = List.of(
            new Cage(26, 8, 12, 0, 1, "cyan_terracotta", "dead_horn_coral_block"),    // north room, runner start 1061 17 982
            new Cage(26, 8, 33, 0, -1, "cyan_terracotta", "dead_horn_coral_block"));  // south room, hunter start 1060 17 1007

    @Override public String id() { return "stadium"; }
    @Override public String displayName() { return "Stadium"; }
    @Override public boolean finished() { return true; }
    @Override public int sizeX() { return SX; }
    @Override public int sizeY() { return SY; }
    @Override public int sizeZ() { return SZ; }
    @Override public List<Cage> cages() { return cages; }
    @Override public int[] playBox() { return new int[]{13, 6, 36, 38}; }
    @Override public double[] spectator() { return new double[]{26.5, 21, 23.5, 0, 60}; }
    @Override public boolean pitKeep(int x, int y, int z, String b) { return y <= 17; }

    private static synchronized List<Object[]> load() {
        if (cache != null) return cache;
        List<Object[]> out = new ArrayList<>(8000);
        try (InputStream in = StadiumMap.class.getResourceAsStream(RESOURCE)) {
            if (in == null) throw new IllegalStateException("missing resource " + RESOURCE);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(new GZIPInputStream(in), StandardCharsets.UTF_8))) {
                String line = r.readLine();   // MCMDUMP 1 sx sy sz
                if (line == null || !line.startsWith("MCMDUMP")) throw new IllegalStateException("bad " + RESOURCE);
                while ((line = r.readLine()) != null) {
                    String[] p = line.split(" ", 4);
                    if (p.length < 4) continue;
                    out.add(new Object[]{Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), p[3]});
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        cache = out;
        return out;
    }

    @Override
    public void generate(BlockSink sink) {
        for (Object[] b : load()) sink.set((Integer) b[0], (Integer) b[1], (Integer) b[2], (String) b[3]);
        // the sealed 2 x 2 x 2 air pocket inside the NE reactor is invisible and unreachable: fill it, so the route check has no phantom dead end
        solid(sink, 29, 2, 15, 30, 3, 16);
        // start rooms: solid cyan block (floor y 1+6 stays), then each cage carves its room out of it
        solid(sink, 22, 7, 7, 29, 12, 11);
        solid(sink, 22, 7, 34, 29, 12, 38);
        for (Cage c : cages) c.build(sink);
    }

    private static void solid(BlockSink s, int x0, int y0, int z0, int x1, int y1, int z1) {
        for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) s.set(x, y, z, "cyan_terracotta");
    }
}
