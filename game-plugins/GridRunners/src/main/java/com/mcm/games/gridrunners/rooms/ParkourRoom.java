package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/** Climb a spiral of floating blocks and press the button at the top. Falling just drops you back to the floor. */
public final class ParkourRoom extends Room {
    private static final int[][] PATH = {
            {4, 1, 4}, {6, 1, 4}, {8, 2, 4}, {10, 2, 4}, {11, 3, 6}, {11, 3, 8}, {11, 4, 10}, {9, 4, 11}, {7, 4, 11}, {5, 4, 11}, {4, 4, 11}
    };
    private static final int[] BUTTON = {4, 5, 11};
    private int bestStep;

    public ParkourRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.PARKOUR, col, row, seed);
    }

    @Override public String name() { return "Sky Steps"; }
    @Override public String objective() { return "Parkour up to the button and press it"; }
    @Override public String hintText() { return "Start at the white block near the corner and follow the green sparkles up and around."; }
    @Override public String progress() { return "Highest step " + bestStep + "/" + (PATH.length - 1); }

    @Override
    public Material floorAt(int lx, int lz) {
        return ((lx / 2 + lz / 2) & 1) == 0 ? Material.LIGHT_BLUE_TERRACOTTA : Material.CYAN_TERRACOTTA;
    }

    @Override
    public void buildContent() {
        for (int i = 0; i < PATH.length; i++) {
            int[] p = PATH[i];
            Material m = i >= PATH.length - 2 ? Material.GOLD_BLOCK : (i % 2 == 0 ? Material.WHITE_CONCRETE : Material.LIGHT_BLUE_CONCRETE);
            put(p[0], p[1], p[2], m);
        }
        Switch button = (Switch) Material.STONE_BUTTON.createBlockData();
        button.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
        put(BUTTON[0], BUTTON[1], BUTTON[2], button);
    }

    @Override
    public void onPlayerFast(Player p) {
        int x = lx(p.getLocation()), y = ly(p.getLocation()) - 1, z = lz(p.getLocation());
        for (int i = 0; i < PATH.length; i++) {
            int[] s = PATH[i];
            if (s[0] == x && s[1] == y && s[2] == z && i > bestStep) bestStep = Math.min(i, PATH.length - 1);
        }
    }

    @Override
    public void onInteractBlock(Player p, Block b, PlayerInteractEvent e) {
        if (is(b, BUTTON[0], BUTTON[1], BUTTON[2])) complete();
    }

    @Override
    public void hintTick() {
        for (int[] s : PATH) marker(s[0] + 0.5, s[1] + 1.3, s[2] + 0.5);
    }
}
