package com.mcm.games.gridrunners.rooms;

import com.mcm.games.gridrunners.GridRunnersGame;
import com.mcm.games.gridrunners.Lane;
import com.mcm.games.gridrunners.Msg;
import org.bukkit.DyeColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The pedestal shows a colour; shear a sheep of that colour. Repeat. Wool regrows, lost sheep respawn. */
public final class SheepRoom extends Room {
    private static final DyeColor[] PALETTE = {DyeColor.RED, DyeColor.BLUE, DyeColor.YELLOW, DyeColor.LIME,
            DyeColor.PURPLE, DyeColor.ORANGE, DyeColor.WHITE, DyeColor.PINK};
    private static final int NEEDED = 4;
    private final DyeColor[] colors = new DyeColor[4];
    private final DyeColor[] sequence = new DyeColor[NEEDED];
    private final DyeColor[] slotColor = new DyeColor[8];
    private final UUID[] slots = new UUID[8];
    private final Map<UUID, Integer> regrowAt = new HashMap<>();
    private int progress;
    private int clock;

    public SheepRoom(GridRunnersGame g, Lane lane, int col, int row, long seed) {
        super(g, lane, RoomType.SHEEP, col, row, seed);
        List<DyeColor> pal = new ArrayList<>(Arrays.asList(PALETTE));
        java.util.Collections.shuffle(pal, rng);
        for (int i = 0; i < 4; i++) colors[i] = pal.get(i);
        DyeColor prev = null;
        for (int i = 0; i < NEEDED; i++) {
            DyeColor c;
            do { c = colors[rng.nextInt(4)]; } while (c == prev);
            sequence[i] = c;
            prev = c;
        }
        for (int i = 0; i < 8; i++) slotColor[i] = colors[i % 4];
    }

    private DyeColor target() { return sequence[Math.min(progress, NEEDED - 1)]; }

    private static String colorName(DyeColor c) {
        String code = switch (c) {
            case RED -> "§c";
            case BLUE -> "§9";
            case YELLOW -> "§e";
            case LIME -> "§a";
            case PURPLE -> "§5";
            case ORANGE -> "§6";
            case PINK -> "§d";
            default -> "§f";
        };
        return code + c.name().toLowerCase().replace('_', ' ');
    }

    @Override public String name() { return "Colour Shear"; }
    @Override public String objective() { return "Shear the sheep shown on the pedestal (" + NEEDED + " times)"; }
    @Override public String hintText() { return "Look at the wool on the centre pedestal and shear a matching sheep. The right ones now glow."; }
    @Override public String progress() { return "Shorn " + progress + "/" + NEEDED + " §7| need " + colorName(target()); }

    @Override
    public Material floorAt(int lx, int lz) {
        return ((lx * 3 + lz * 5) % 7 == 0) ? Material.ROOTED_DIRT : Material.MOSS_BLOCK;
    }

    @Override
    public void buildContent() {
        for (int x = 7; x <= 8; x++)
            for (int z = 7; z <= 8; z++) {
                put(x, 1, z, Material.STRIPPED_OAK_LOG);
                put(x, 2, z, Material.STRIPPED_OAK_LOG);
            }
        updateDisplay();
        int[][] fences = {{1, 1}, {14, 1}, {1, 14}, {14, 14}};
        for (int[] f : fences) put(f[0], 1, f[1], Material.HAY_BLOCK);
    }

    private void updateDisplay() {
        Material wool = Material.valueOf(target().name() + "_WOOL");
        for (int x = 7; x <= 8; x++)
            for (int z = 7; z <= 8; z++) put(x, 3, z, wool);
    }

    @Override
    public void onActivate() {
        for (int i = 0; i < 8; i++) spawnSheep(i);
    }

    private void spawnSheep(int slot) {
        double x = 2.5 + rng.nextInt(11), z = 2.5 + rng.nextInt(11);
        if (x >= 6 && x <= 10 && z >= 6 && z <= 10) x = 3.5;
        final DyeColor c = slotColor[slot];
        Sheep s = spawn(loc(x, 1, z), Sheep.class, sh -> {
            sh.setColor(c);
            sh.setAdult();
            sh.setSheared(false);
            game.protect(sh);
        });
        slots[slot] = s.getUniqueId();
    }

    @Override
    public void secondTick() {
        clock++;
        for (int i = 0; i < 8; i++) {
            if (!alive(slots[i])) {
                if (slots[i] != null) entities.remove(slots[i]);
                spawnSheep(i);
            }
        }
        regrowAt.entrySet().removeIf(en -> {
            if (en.getValue() > clock) return false;
            Entity e = entity(en.getKey());
            if (e instanceof Sheep s) s.setSheared(false);
            return true;
        });
        keepInside(Arrays.asList(slots));
        if (hinted) {
            for (UUID u : slots) {
                Entity e = entity(u);
                if (e instanceof Sheep s) s.setGlowing(s.getColor() == target() && !s.isSheared());
            }
        }
    }

    @Override
    public void onInteractEntity(Player p, Entity en) {
        if (!(en instanceof Sheep s) || !entities.contains(s.getUniqueId())) return;
        if (p.getInventory().getItemInMainHand().getType() != Material.SHEARS) {
            Msg.bar(p, "§eHold your shears to shear sheep!");
            return;
        }
        if (s.isSheared()) {
            Msg.bar(p, "§7That sheep's wool hasn't grown back yet.");
            return;
        }
        if (s.getColor() != target()) {
            Msg.bar(p, "§cWrong colour! §7Shear a " + colorName(target()) + " §7sheep.");
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 0.5f);
            return;
        }
        s.setSheared(true);
        s.setGlowing(false);
        regrowAt.put(s.getUniqueId(), clock + 6);
        Location l = s.getLocation().add(0, 0.8, 0);
        world.playSound(l, Sound.ENTITY_SHEEP_SHEAR, 1f, 1f);
        world.spawnParticle(Particle.CLOUD, l, 8, 0.3, 0.3, 0.3, 0.02);
        progress++;
        if (progress >= NEEDED) {
            complete();
            return;
        }
        updateDisplay();
        tellInside("§aNice! §7Next colour: " + colorName(target()));
        soundInside(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.5f);
    }

    @Override
    protected Location returnSpot() { return loc(4.5, 1, 4.5); }

    @Override
    protected void onCleanup() {
        regrowAt.clear();
        Arrays.fill(slots, null);
    }
}
