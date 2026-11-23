package com.mcm.games.decisiondome;

import com.mcm.api.McmApi;
import com.mcm.api.McmFx;
import com.mcm.api.McmLobby;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

import static com.mcm.games.decisiondome.DomeArena.c;

/** The vote inside the core's Decision Dome venue in the MCC hub (via {@link McmLobby}). */
final class HubStage implements VoteStage {
    static final DyeColor[] DYES = {
            DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN,
            DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK, DyeColor.WHITE};

    private final Plugin plugin;
    private McmLobby.Venue venue;
    private int n;
    private int[] padOf = new int[0];
    private final List<TextDisplay> labels = new ArrayList<>();
    private final List<ItemDisplay> icons = new ArrayList<>();
    private TextDisplay timer;
    private ItemDisplay revealIcon;
    private TextDisplay revealText;
    private float yaw = 0f;
    private int lastSecs = Integer.MIN_VALUE;

    HubStage(Plugin plugin) { this.plugin = plugin; }

    static boolean available() { return McmLobby.isAvailable(); }

    @Override public int maxOptions() { McmLobby.Venue v = McmLobby.venue(); return v == null ? 12 : v.padCount(); }

    @Override public boolean prepare(List<String> ids) {
        venue = McmLobby.venue();
        if (venue == null) return false;
        clear();
        venue = McmLobby.venue();
        n = Math.min(ids.size(), venue.padCount());
        int pads = venue.padCount();
        padOf = new int[n];
        boolean[] used = new boolean[pads];
        for (int i = 0; i < n; i++) {
            int p = (int) Math.round(i * (double) pads / n) % pads;
            while (used[p]) p = (p + 1) % pads;
            used[p] = true;
            padOf[i] = p;
        }
        // dark out every pad, then light the used ones in their game colour
        for (int p = 0; p < pads; p++) if (!used[p]) venue.setPad(p, null, false);
        World w = venue.world();
        for (int i = 0; i < n; i++) {
            Location pc = venue.padCenter(padOf[i]);
            venue.setPad(padOf[i], DYES[i % DYES.length], true);
            final String id = ids.get(i);
            final int idx = i;
            labels.add(w.spawn(pc.clone().add(0, 4.4, 0), TextDisplay.class, d -> {
                d.setPersistent(false);
                d.addScoreboardTag(DomeArena.TAG);
                d.setBillboard(Display.Billboard.CENTER);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setShadowed(true);
                d.setBackgroundColor(Color.fromARGB(140, 8, 10, 30));
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.5f, 1.5f, 1.5f), new AxisAngle4f()));
                d.text(c(DomeArena.labelText(idx, id, 0, false)));
            }));
            icons.add(w.spawn(pc.clone().add(0, 2.4, 0), ItemDisplay.class, d -> {
                d.setPersistent(false);
                d.addScoreboardTag(DomeArena.TAG);
                d.setItemStack(new ItemStack(DomeArena.iconFor(id)));
                d.setBillboard(Display.Billboard.FIXED);
                d.setTeleportDuration(3);
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.4f, 1.4f, 1.4f), new AxisAngle4f()));
            }));
            w.spawnParticle(Particle.END_ROD, pc.clone().add(0, 1, 0), 30, 1.5, 1.0, 1.5, 0.02);
        }
        timer = w.spawn(venue.interiorTimer(), TextDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(DomeArena.TAG);
            d.setBillboard(Display.Billboard.VERTICAL);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setShadowed(true);
            d.setBackgroundColor(Color.fromARGB(150, 0, 0, 0));
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(6f, 6f, 6f), new AxisAngle4f()));
            d.text(c("§e§lVOTE!"));
        });
        lastSecs = Integer.MIN_VALUE;
        return true;
    }

    @Override public World world() { return venue == null ? null : venue.world(); }

    @Override public boolean isStageWorld(World w) { return venue != null && w != null && w.equals(venue.world()); }

    @Override public Location arrival(Random rng) {
        if (venue == null) venue = McmLobby.venue();
        Location c = venue.domeCenter();
        Location base = venue.interiorSpawn();
        double a = rng.nextDouble() * Math.PI * 2;
        double r = 9.5 + rng.nextDouble() * 4.0;   // between the podium (r<8) and the pads (r>15)
        double x = c.getX() + Math.cos(a) * r, z = c.getZ() + Math.sin(a) * r;
        float yaw = (float) Math.toDegrees(Math.atan2(-(c.getX() - x), c.getZ() - z));
        return new Location(c.getWorld(), x, base.getY(), z, yaw + 180f, 0f);
    }

    @Override public int optionAt(Location l) {
        if (venue == null || l == null || !isStageWorld(l.getWorld())) return -1;
        double r = venue.padRadius() + 0.4;
        for (int i = 0; i < n; i++) {
            Location pc = venue.padCenter(padOf[i]);
            double dx = l.getX() - pc.getX(), dz = l.getZ() - pc.getZ();
            if (dx * dx + dz * dz <= r * r && l.getY() >= pc.getY() - 1.6 && l.getY() <= pc.getY() + 4) return i;
        }
        return -1;
    }

    @Override public void updateLabel(int idx, String id, int votes, boolean winner) {
        if (idx < 0 || idx >= labels.size()) return;
        TextDisplay td = labels.get(idx);
        if (td != null && td.isValid()) td.text(c(DomeArena.labelText(idx, id, votes, winner)));
    }

    @Override public void tickVisuals(int tick) {
        if (tick % 3 != 0) return;
        yaw = (yaw + 9f) % 360f;
        for (ItemDisplay d : icons) if (d != null && d.isValid()) d.setRotation(yaw, 0f);
        if (revealIcon != null && revealIcon.isValid()) revealIcon.setRotation(-yaw * 0.7f, 0f);
    }

    @Override public void setCountdown(int seconds) {
        if (venue == null || seconds == lastSecs) return;
        lastSecs = seconds;
        venue.showCountdown(seconds);
        if (timer != null && timer.isValid()) {
            String col = seconds < 0 ? "§e" : seconds <= 5 ? "§c" : seconds <= 10 ? "§6" : "§e";
            timer.text(c(seconds < 0 ? "§d§lDECIDING..." : col + "§l" + String.format("%d:%02d", seconds / 60, seconds % 60)));
        }
    }

    @Override public void highlight(int idx, boolean on) {
        if (venue == null || idx < 0 || idx >= n) return;
        venue.setPad(padOf[idx], on ? DyeColor.WHITE : DYES[idx % DYES.length], true);
    }

    @Override public void eliminate(int idx, String id) {
        if (venue == null || idx < 0 || idx >= n) return;
        venue.setPad(padOf[idx], null, false);
        Location pc = venue.padCenter(padOf[idx]);
        World w = pc.getWorld();
        w.spawnParticle(Particle.EXPLOSION, pc.clone().add(0, 1, 0), 3, 1.2, 0.5, 1.2, 0);
        w.spawnParticle(Particle.LARGE_SMOKE, pc.clone().add(0, 1, 0), 40, 1.8, 0.8, 1.8, 0.03);
        w.playSound(pc, Sound.BLOCK_BEACON_DEACTIVATE, 2f, 0.8f);
        w.playSound(pc, Sound.ENTITY_GENERIC_EXPLODE, 0.6f, 1.4f);
        if (idx < labels.size() && labels.get(idx) != null && labels.get(idx).isValid())
            labels.get(idx).text(c("§8§m" + McmApi.displayName(id)));
        if (idx < icons.size() && icons.get(idx) != null && icons.get(idx).isValid()) icons.get(idx).remove();
    }

    @Override public void winner(int idx, String id, int votes) {
        if (venue == null || idx < 0 || idx >= n) return;
        venue.setPad(padOf[idx], DyeColor.YELLOW, true);
        updateLabel(idx, id, votes, true);
        World w = venue.world();
        Location pod = venue.revealPodium();
        // the winning game rises above the central podium
        revealIcon = w.spawn(pod.clone().add(0, 3.5, 0), ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(DomeArena.TAG);
            d.setItemStack(new ItemStack(DomeArena.iconFor(id)));
            d.setBillboard(Display.Billboard.FIXED);
            d.setTeleportDuration(3);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(3.5f, 3.5f, 3.5f), new AxisAngle4f()));
        });
        revealText = w.spawn(pod.clone().add(0, 7.2, 0), TextDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(DomeArena.TAG);
            d.setBillboard(Display.Billboard.CENTER);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setShadowed(true);
            d.setBackgroundColor(Color.fromARGB(160, 40, 25, 0));
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(3f, 3f, 3f), new AxisAngle4f()));
            d.text(c("§6§l★ " + McmApi.displayName(id) + " ★"));
        });
        Location pc = venue.padCenter(padOf[idx]);
        w.spawnParticle(Particle.TOTEM_OF_UNDYING, pc.clone().add(0, 1.5, 0), 80, 1.5, 1.5, 1.5, 0.4);
        // a trail of light from the pad to the podium
        for (int k = 0; k <= 20; k++) {
            double t = k / 20.0;
            w.spawnParticle(Particle.END_ROD, pc.getX() + (pod.getX() - pc.getX()) * t, pc.getY() + 1 + Math.sin(t * Math.PI) * 4,
                    pc.getZ() + (pod.getZ() - pc.getZ()) * t, 2, 0.1, 0.1, 0.1, 0.01);
        }
        McmFx.fireworkShow(plugin, pod.clone().add(0, 4, 0), Color.YELLOW, 3);
        if (timer != null && timer.isValid()) timer.text(c("§6§l" + McmApi.displayName(id)));
    }

    @Override public Location optionLocation(int idx) {
        if (venue == null || idx < 0 || idx >= n) return revealLocation();
        return venue.padCenter(padOf[idx]).add(0, 1.5, 0);
    }

    @Override public Location revealLocation() { return venue == null ? null : venue.revealPodium().add(0, 2, 0); }

    @Override public void clear() {
        for (TextDisplay d : labels) if (d != null && d.isValid()) d.remove();
        for (ItemDisplay d : icons) if (d != null && d.isValid()) d.remove();
        labels.clear();
        icons.clear();
        if (timer != null && timer.isValid()) timer.remove();
        if (revealIcon != null && revealIcon.isValid()) revealIcon.remove();
        if (revealText != null && revealText.isValid()) revealText.remove();
        timer = null; revealIcon = null; revealText = null;
        McmLobby.Venue v = McmLobby.venue();
        if (v != null) {
            try { v.resetPads(); v.showCountdown(-1); } catch (Throwable ignored) { }
            World w = v.world();
            if (w != null) for (org.bukkit.entity.Entity e : w.getEntities()) if (e.getScoreboardTags().contains(DomeArena.TAG)) e.remove();
        }
        n = 0;
        padOf = new int[0];
        lastSecs = Integer.MIN_VALUE;
    }

    @Override public double voidY() { return venue == null ? -64 : venue.domeCenter().getY() - 30; }
}
