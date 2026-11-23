package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * "Lights out" on a 3x3 grid of floor lamps: each button toggles its lamp and the orthogonal neighbours.
 * Always solvable (3x3 lights-out is invertible); the hint marks exactly the buttons still needed.
 */
public final class LampGridRoom extends Room {
    private static final int[] POS = {5, 7, 9};
    private final boolean[] lit = new boolean[9];
    /** Parity of presses needed to solve from the current state. */
    private final boolean[] need = new boolean[9];
    private final long[] lastPress = new long[9];

    public LampGridRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.LAMPS, col, row, seed);
        for (int i = 0; i < 9; i++) lit[i] = true;
        int presses = 3 + rng.nextInt(3);
        for (int k = 0; k < presses; k++) {
            int i = rng.nextInt(9);
            toggleAround(i);
            need[i] = !need[i];
        }
        boolean any = false;
        for (boolean b : need) any |= b;
        if (!any) { toggleAround(4); need[4] = true; }
    }

    @Override public String name() { return "Lamp Grid"; }
    @Override public String objective() { return "Light every lamp - each button flips its neighbours too"; }
    @Override public String hintText() { return "Press each sparkling button exactly once."; }

    @Override
    public String progress() {
        int n = 0;
        for (boolean b : lit) if (b) n++;
        return "Lamps " + n + "/9";
    }

    @Override
    public Material floorAt(int lx, int lz) {
        return ((lx + lz) & 1) == 0 ? Material.DEEPSLATE_TILES : Material.POLISHED_DEEPSLATE;
    }

    private void toggleAround(int i) {
        int gx = i % 3, gz = i / 3;
        flip(gx, gz);
        flip(gx - 1, gz);
        flip(gx + 1, gz);
        flip(gx, gz - 1);
        flip(gx, gz + 1);
    }

    private void flip(int gx, int gz) {
        if (gx < 0 || gx > 2 || gz < 0 || gz > 2) return;
        lit[gz * 3 + gx] = !lit[gz * 3 + gx];
    }

    @Override
    public void buildContent() {
        // Frame around the grid
        for (int x = 4; x <= 10; x++) {
            for (int z = 4; z <= 10; z++) {
                boolean edge = x == 4 || x == 10 || z == 4 || z == 10;
                if (edge) put(x, 0, z, Material.CHISELED_DEEPSLATE);
            }
        }
        for (int i = 0; i < 9; i++) {
            Switch b = (Switch) Material.POLISHED_BLACKSTONE_BUTTON.createBlockData();
            b.setAttachedFace(FaceAttachable.AttachedFace.FLOOR);
            put(POS[i % 3], 1, POS[i / 3], b);
        }
        render();
    }

    private void render() {
        for (int i = 0; i < 9; i++) {
            Lightable l = (Lightable) Material.REDSTONE_LAMP.createBlockData();
            l.setLit(lit[i]);
            block(POS[i % 3], 0, POS[i / 3]).setBlockData(l, false);
        }
    }

    @Override
    public void onInteractBlock(Player p, Block b, PlayerInteractEvent e) {
        for (int i = 0; i < 9; i++) {
            if (!is(b, POS[i % 3], 1, POS[i / 3])) continue;
            e.setCancelled(true);
            long t = System.currentTimeMillis();
            if (t - lastPress[i] < 300) return;
            lastPress[i] = t;
            toggleAround(i);
            need[i] = !need[i];
            render();
            world.playSound(b.getLocation(), Sound.BLOCK_STONE_BUTTON_CLICK_ON, 1f, 1.2f);
            boolean all = true;
            for (boolean l : lit) all &= l;
            if (all) complete();
            return;
        }
    }

    @Override
    public void secondTick() { render(); }

    @Override
    public void hintTick() {
        for (int i = 0; i < 9; i++) if (need[i]) marker(POS[i % 3] + 0.5, 1.5, POS[i / 3] + 0.5);
    }
}
