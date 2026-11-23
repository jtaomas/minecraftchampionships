package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Switch;

/** Match the lever row to the code board, three codes in a row. */
public final class LeverCodeRoom extends Room {
    private static final int[] XS = {3, 5, 7, 9, 11, 13};
    private static final int LEVER_Z = 10, BOARD_Z = 4, ROUNDS = 3;
    private final boolean[][] codes = new boolean[ROUNDS][XS.length];
    private int round;

    public LeverCodeRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.LEVERS, col, row, seed);
        boolean[] prev = new boolean[XS.length]; // levers start all off
        for (int r = 0; r < ROUNDS; r++) {
            boolean[] c;
            do {
                c = new boolean[XS.length];
                int on = 0;
                for (int i = 0; i < XS.length; i++) { c[i] = rng.nextBoolean(); if (c[i]) on++; }
                if (on < 2) continue;
                if (java.util.Arrays.equals(c, prev)) continue;
                break;
            } while (true);
            codes[r] = c;
            prev = c;
        }
    }

    @Override public String name() { return "Lever Lock"; }
    @Override public String objective() { return "Set the levers to match the code board (" + ROUNDS + " codes)"; }
    @Override public String hintText() { return "Green on the board = lever ON (flicked), red = OFF. Wrong levers are now sparkling."; }
    @Override public String progress() { return "Code " + Math.min(round + 1, ROUNDS) + "/" + ROUNDS + " §7| correct " + correctCount() + "/" + XS.length; }

    @Override
    public Material floorAt(int lx, int lz) {
        return ((lx + lz) & 1) == 0 ? Material.POLISHED_BLACKSTONE_BRICKS : Material.POLISHED_BLACKSTONE;
    }

    @Override
    public void buildContent() {
        for (int x : XS) {
            put(x, 1, LEVER_Z, Material.POLISHED_BLACKSTONE);
            Switch lever = (Switch) Material.LEVER.createBlockData();
            lever.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
            lever.setFacing(BlockFace.NORTH);
            lever.setPowered(false);
            put(x, 2, LEVER_Z, lever);
            put(x, 1, BOARD_Z, Material.POLISHED_BLACKSTONE);
            put(x, 4, BOARD_Z, Material.CHISELED_POLISHED_BLACKSTONE);
        }
        renderBoard();
    }

    private void renderBoard() {
        boolean[] c = codes[Math.min(round, ROUNDS - 1)];
        for (int i = 0; i < XS.length; i++) {
            Material m = c[i] ? Material.LIME_CONCRETE : Material.RED_CONCRETE;
            put(XS[i], 2, BOARD_Z, m);
            put(XS[i], 3, BOARD_Z, m);
        }
        for (int r = 0; r < ROUNDS; r++) put(6 + r, 6, BOARD_Z, r < round ? Material.SEA_LANTERN : Material.GRAY_CONCRETE);
    }

    private boolean leverOn(int i) {
        BlockData d = block(XS[i], 2, LEVER_Z).getBlockData();
        return d instanceof Switch s && s.isPowered();
    }

    private int correctCount() {
        if (round >= ROUNDS) return XS.length;
        int n = 0;
        for (int i = 0; i < XS.length; i++) if (leverOn(i) == codes[round][i]) n++;
        return n;
    }

    @Override
    public void fastTick() {
        if (round >= ROUNDS) return;
        if (correctCount() == XS.length) {
            round++;
            if (round >= ROUNDS) {
                renderBoard();
                complete();
                return;
            }
            renderBoard();
            soundInside(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.6f);
            tellInside("§aCode accepted! §7New code on the board (" + (round + 1) + "/" + ROUNDS + ").");
        }
    }

    @Override
    public void hintTick() {
        if (round >= ROUNDS) return;
        for (int i = 0; i < XS.length; i++) if (leverOn(i) != codes[round][i]) marker(XS[i] + 0.5, 3, LEVER_Z + 0.5);
    }
}
