package com.mcm.games.decisiondome;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static com.mcm.games.decisiondome.DomeArena.c;

/** {@link DomeVenue} over the plugin-owned {@code mcc_dome} world. */
final class FallbackDialView implements DomeVenue {
    private final Plugin plugin;
    private final DomeArena arena;
    private final String sessionId;
    private final List<TextDisplay> labels = new ArrayList<>();
    private final List<ItemDisplay> icons = new ArrayList<>();
    private TextDisplay revealText;
    private TextDisplay revealIconLabel;
    private ItemDisplay revealIcon;
    private int capacity;
    private double baseAngle = Math.PI / 2.0;
    private float yaw;

    FallbackDialView(Plugin plugin, DomeArena arena, String sessionId) {
        this.plugin = plugin;
        this.arena = arena;
        this.sessionId = sessionId;
    }

    @Override public String id() { return "mcc_dome"; }
    @Override public int apiVersion() { return 1; }
    @Override public World world() { return arena.world(); }
    @Override public boolean available() { return arena.ensureWorld() != null; }

    @Override public DialGeometry geometry() {
        return new DialGeometry(arena.floorY(), DomeArena.HUB_R, DomeArena.WEDGE_R, baseAngle);
    }

    @Override public Location center() { return arena.center(); }

    @Override public int capacity() { return capacity; }

    @Override public int podCount() { return DomeArena.POD_ANGLES_DEG.length; }

    @Override
    public boolean prepare(Dial dial) {
        if (arena.ensureWorld() == null) return false;
        clearLabels();
        capacity = Math.max(1, dial.size());
        arena.buildDial(capacity, baseAngle);
        World w = arena.world();
        for (int i = 0; i < capacity; i++) {
            Wedge wedge = dial.wedge(i);
            final String id = wedge == null ? null : wedge.gameId();
            final int color = wedge == null ? i : wedge.colorIndex();
            Location anchor = arena.wedgeCenter(i).add(0, 5.2, 0);
            labels.add(w.spawn(anchor, TextDisplay.class, d -> {
                d.setPersistent(false);
                d.addScoreboardTag(DomeArena.TAG);
                d.addScoreboardTag(uiTag());
                d.setBillboard(Display.Billboard.CENTER);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setShadowed(true);
                d.setBackgroundColor(Color.fromARGB(120, 0, 0, 0));
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.55f, 1.55f, 1.55f), new AxisAngle4f()));
                d.text(c(id == null ? "§8—" : DomeArena.labelText(color, id, 0, false, false)));
            }));
            if (id == null) {
                icons.add(null);
            } else {
                icons.add(w.spawn(arena.wedgeCenter(i).add(0, 2.6, 0), ItemDisplay.class, d -> {
                    d.setPersistent(false);
                    d.addScoreboardTag(DomeArena.TAG);
                    d.addScoreboardTag(uiTag());
                    d.setItemStack(new ItemStack(DomeArena.iconFor(id)));
                    d.setBillboard(Display.Billboard.FIXED);
                    d.setTeleportDuration(3);
                    d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.5f, 1.5f, 1.5f), new AxisAngle4f()));
                }));
            }
            if (wedge != null && !wedge.live()) setWedge(i, wedge.state(), null, i);
        }
        return true;
    }

    @Override public Location podSpawn(int pod) { return arena.podSpawn(pod); }

    @Override public Location wedgeCenter(int slot) { return arena.wedgeCenter(slot); }

    @Override public Location at(double x, double z, double dy) { return arena.at(x, z, dy); }

    @Override
    public void setWedge(int slot, Wedge.State state, String gameId, int colorIndex) {
        arena.setWedge(slot, state, colorIndex);
    }

    @Override
    public void updateLabel(int slot, String gameId, int count, boolean showCount) {
        if (slot < 0 || slot >= labels.size()) return;
        TextDisplay td = labels.get(slot);
        if (td == null || !td.isValid()) return;
        if (gameId == null) {
            td.text(c("§8§m—"));
            return;
        }
        td.text(c(DomeArena.labelText(Math.floorMod(slot, 12), gameId, count, showCount, false)));
    }

    @Override public void setDividers(double progress) { arena.setDividers(progress); }

    @Override public void setPodGate(int pod, boolean open) { }

    @Override public void setPodState(int pod, String state) { }

    @Override public void setPodColor(int pod, int colorIndex) { arena.setPodColor(pod, colorIndex); }

    @Override
    public void clearRuntime() {
        clearLabels();
        arena.clearRuntime();
        if (arena.world() != null) {
            for (org.bukkit.entity.Entity e : arena.world().getEntities()) {
                if (e.getScoreboardTags().contains(uiTag())) e.remove();
            }
        }
    }

    private void clearLabels() {
        for (TextDisplay d : labels) if (d != null && d.isValid()) d.remove();
        for (ItemDisplay d : icons) if (d != null && d.isValid()) d.remove();
        labels.clear();
        icons.clear();
        if (revealText != null && revealText.isValid()) revealText.remove();
        if (revealIcon != null && revealIcon.isValid()) revealIcon.remove();
        if (revealIconLabel != null && revealIconLabel.isValid()) revealIconLabel.remove();
        revealText = null;
        revealIcon = null;
        revealIconLabel = null;
    }

    @Override
    public void reveal(int slot, String gameId, VoteTally tally) {
        if (gameId == null) return;
        setWedge(slot, Wedge.State.WINNER, gameId, Math.floorMod(slot, 12));
        updateLabel(slot, gameId, tally == null ? 0 : tally.line(slot) == null ? 0 : tally.line(slot).score(), true);
        World w = arena.world();
        if (w == null) return;
        Location reveal = arena.revealLocation();
        revealIcon = w.spawn(reveal.clone().add(0, 3.5, 0), ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(DomeArena.TAG);
            d.addScoreboardTag(uiTag());
            d.setItemStack(new ItemStack(DomeArena.iconFor(gameId)));
            d.setBillboard(Display.Billboard.FIXED);
            d.setTeleportDuration(3);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(3.5f, 3.5f, 3.5f), new AxisAngle4f()));
        });
        revealText = w.spawn(reveal.clone().add(0, 7.5, 0), TextDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(DomeArena.TAG);
            d.addScoreboardTag(uiTag());
            d.setBillboard(Display.Billboard.CENTER);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setShadowed(true);
            d.setBackgroundColor(Color.fromARGB(160, 40, 25, 0));
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(3f, 3f, 3f), new AxisAngle4f()));
            d.text(c("§6§l★ " + com.mcm.api.McmApi.displayName(gameId) + " ★"));
        });
    }

    @Override public Location revealLocation() { return arena.revealLocation(); }

    @Override public void showCountdown(int seconds) { }

    @Override
    public void tick(int totalTick) {
        if (totalTick % 3 != 0) return;
        yaw = (yaw + 9f) % 360f;
        for (ItemDisplay d : icons) if (d != null && d.isValid()) d.setRotation(yaw, 0f);
        if (revealIcon != null && revealIcon.isValid()) revealIcon.setRotation(-yaw * 0.7f, 0f);
    }

    @Override public boolean isVenueWorld(World w) { return arena.isDomeWorld(w); }

    private String uiTag() { return DomeArena.TAG + ":" + sessionId + ":ui"; }
}
