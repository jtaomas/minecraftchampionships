package com.mcm.games.decisiondome;

import com.mcm.api.McmApi;
import com.mcm.api.McmLobby;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Location;
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

/**
 * {@link DomeVenue} over the core's Decision Dome venue in the MCC hub. Uses the v2 geometry when
 * the venue exposes it (dial, wedges, pods, gates, dividers); the v1 pad methods remain as a
 * compatibility layer so an older venue still renders a usable board.
 */
final class HubDialView implements DomeVenue {
    static final DyeColor[] DYES = {
            DyeColor.RED, DyeColor.ORANGE, DyeColor.YELLOW, DyeColor.LIME, DyeColor.GREEN, DyeColor.CYAN,
            DyeColor.LIGHT_BLUE, DyeColor.BLUE, DyeColor.PURPLE, DyeColor.MAGENTA, DyeColor.PINK, DyeColor.WHITE};

    private final Plugin plugin;
    private final McmLobby.Venue venue;
    private final String sessionId;
    private final List<TextDisplay> labels = new ArrayList<>();
    private final List<ItemDisplay> icons = new ArrayList<>();
    private TextDisplay timer;
    private TextDisplay revealText;
    private ItemDisplay revealIcon;
    private int capacity;
    private DialGeometry geometry;
    private float yaw;
    private int lastSeconds = Integer.MIN_VALUE;

    HubDialView(Plugin plugin, McmLobby.Venue venue, String sessionId) {
        this.plugin = plugin;
        this.venue = venue;
        this.sessionId = sessionId;
        this.geometry = computeGeometry();
    }

    @Override public String id() { return "hub"; }
    @Override public int apiVersion() { return venue == null ? 0 : venue.apiVersion(); }
    @Override public World world() { return venue == null ? null : venue.world(); }
    @Override public boolean available() { return venue != null && venue.world() != null; }

    @Override public DialGeometry geometry() {
        if (geometry == null) geometry = computeGeometry();
        return geometry;
    }

    @Override public Location center() {
        if (venue == null) return null;
        Location c = venue.domeCenter();
        DialGeometry g = geometry();
        return new Location(c.getWorld(), c.getX(), g.floorY(), c.getZ());
    }

    @Override public int capacity() { return capacity; }

    @Override public int podCount() { return venue == null ? 10 : Math.max(1, venue.podCount()); }

    @Override
    public boolean prepare(Dial dial) {
        if (venue == null || venue.world() == null) return false;
        clearLabels();
        capacity = Math.max(1, dial.size());
        try { venue.setDial(capacity); } catch (Throwable ignored) { }
        for (int i = 0; i < capacity; i++) {
            Wedge wedge = dial.wedge(i);
            final String id = wedge == null ? null : wedge.gameId();
            final int color = wedge == null ? i : wedge.colorIndex();
            Location anchor = venue.wedgeCenter(i).clone().add(0, 5.0, 0);
            Location icon = venue.wedgeCenter(i).clone().add(0, 2.4, 0);
            labels.add(venue.world().spawn(anchor, TextDisplay.class, d -> {
                d.setPersistent(false);
                d.addScoreboardTag(DomeArena.TAG);
                d.addScoreboardTag(uiTag());
                d.setBillboard(Display.Billboard.CENTER);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setShadowed(true);
                d.setBackgroundColor(Color.fromARGB(140, 8, 10, 30));
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.5f, 1.5f, 1.5f), new AxisAngle4f()));
                d.text(c(id == null ? "§8—" : DomeArena.labelText(color, id, 0, false, false)));
            }));
            if (id == null) {
                icons.add(null);
            } else {
                icons.add(venue.world().spawn(icon, ItemDisplay.class, d -> {
                    d.setPersistent(false);
                    d.addScoreboardTag(DomeArena.TAG);
                    d.addScoreboardTag(uiTag());
                    d.setItemStack(new ItemStack(DomeArena.iconFor(id)));
                    d.setBillboard(Display.Billboard.FIXED);
                    d.setTeleportDuration(3);
                    d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(1.4f, 1.4f, 1.4f), new AxisAngle4f()));
                }));
            }
            if (wedge != null) setWedge(i, wedge.state(), id, color);
        }
        Location clock = venue.interiorClock();
        if (clock != null && clock.getWorld() != null) {
            timer = clock.getWorld().spawn(clock, TextDisplay.class, d -> {
                d.setPersistent(false);
                d.addScoreboardTag(DomeArena.TAG);
                d.addScoreboardTag(uiTag());
                d.setBillboard(Display.Billboard.VERTICAL);
                d.setAlignment(TextDisplay.TextAlignment.CENTER);
                d.setShadowed(true);
                d.setBackgroundColor(Color.fromARGB(150, 0, 0, 0));
                d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(6f, 6f, 6f), new AxisAngle4f()));
                d.text(c("§e§lVOTE!"));
            });
        }
        lastSeconds = Integer.MIN_VALUE;
        return true;
    }

    private DialGeometry computeGeometry() {
        if (venue == null) return DialGeometry.orrery();
        Location c = venue.domeCenter();
        Location w0 = venue.wedgeCenter(0);
        double outer = Math.max(3.0, venue.wedgeRadius());
        double inner = Math.max(1.0, Math.min(outer * 0.62, outer - 1.0));
        double base = Math.atan2(w0.getZ() - c.getZ(), w0.getX() - c.getX());
        if (Double.isNaN(base) || (Math.abs(w0.getX() - c.getX()) < 1.0e-6 && Math.abs(w0.getZ() - c.getZ()) < 1.0e-6)) {
            base = Math.PI / 2.0;
        }
        return new DialGeometry(w0.getY(), inner, outer, base);
    }

    @Override public Location podSpawn(int pod) {
        if (venue == null) return null;
        try { return venue.podSpawn(pod); } catch (Throwable t) { return venue.interiorSpawn(); }
    }

    @Override public Location wedgeCenter(int slot) {
        if (venue == null) return null;
        return venue.wedgeCenter(slot);
    }

    @Override public Location at(double x, double z, double dy) {
        Location c = center();
        if (c == null) return null;
        return new Location(c.getWorld(), c.getX() + x, c.getY() + dy, c.getZ() + z);
    }

    @Override
    public void setWedge(int slot, Wedge.State state, String gameId, int colorIndex) {
        if (venue == null) return;
        DyeColor dye = switch (state) {
            case ACTIVE -> DYES[Math.floorMod(colorIndex, DYES.length)];
            case HIGHLIGHT -> DyeColor.WHITE;
            case WINNER -> DyeColor.YELLOW;
            case REMOVED, BLANK -> null;
        };
        try { venue.setWedge(slot, dye, dye != null); } catch (Throwable ignored) { }
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

    @Override
    public void setDividers(double progress) {
        if (venue == null) return;
        try { venue.setDividers(progress); } catch (Throwable ignored) { }
    }

    @Override
    public void setPodGate(int pod, boolean open) {
        if (venue == null) return;
        try { venue.setPodGate(pod, open); } catch (Throwable ignored) { }
    }

    @Override
    public void setPodState(int pod, String state) {
        if (venue == null) return;
        try { venue.setPodState(pod, state); } catch (Throwable ignored) { }
    }

    @Override public void setPodColor(int pod, int colorIndex) { }

    @Override
    public void clearRuntime() {
        clearLabels();
        if (venue != null) {
            try { venue.clearRuntime(); } catch (Throwable ignored) { }
            try { venue.showCountdown(-1); } catch (Throwable ignored) { }
            World w = venue.world();
            if (w != null) {
                for (org.bukkit.entity.Entity e : w.getEntities()) {
                    if (e.getScoreboardTags().contains(uiTag())) e.remove();
                }
            }
        }
    }

    private void clearLabels() {
        for (TextDisplay d : labels) if (d != null && d.isValid()) d.remove();
        for (ItemDisplay d : icons) if (d != null && d.isValid()) d.remove();
        labels.clear();
        icons.clear();
        if (timer != null && timer.isValid()) timer.remove();
        if (revealText != null && revealText.isValid()) revealText.remove();
        if (revealIcon != null && revealIcon.isValid()) revealIcon.remove();
        timer = null;
        revealText = null;
        revealIcon = null;
        lastSeconds = Integer.MIN_VALUE;
    }

    @Override
    public void reveal(int slot, String gameId, VoteTally tally) {
        if (venue == null || gameId == null) return;
        setWedge(slot, Wedge.State.WINNER, gameId, slot);
        updateLabel(slot, gameId, tally == null || tally.line(slot) == null ? 0 : tally.line(slot).score(), true);
        try { venue.revealWedge(slot, gameId); } catch (Throwable ignored) { }
        Location surface = venue.revealSurface(0);
        if (surface == null) surface = venue.revealPodium();
        World w = venue.world();
        if (w == null) return;
        revealIcon = w.spawn(surface.clone().add(0, 2.5, 0), ItemDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(DomeArena.TAG);
            d.addScoreboardTag(uiTag());
            d.setItemStack(new ItemStack(DomeArena.iconFor(gameId)));
            d.setBillboard(Display.Billboard.FIXED);
            d.setTeleportDuration(3);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(3.2f, 3.2f, 3.2f), new AxisAngle4f()));
        });
        revealText = w.spawn(surface.clone().add(0, 5.8, 0), TextDisplay.class, d -> {
            d.setPersistent(false);
            d.addScoreboardTag(DomeArena.TAG);
            d.addScoreboardTag(uiTag());
            d.setBillboard(Display.Billboard.CENTER);
            d.setAlignment(TextDisplay.TextAlignment.CENTER);
            d.setShadowed(true);
            d.setBackgroundColor(Color.fromARGB(160, 40, 25, 0));
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(3f, 3f, 3f), new AxisAngle4f()));
            d.text(c("§6§l★ " + McmApi.displayName(gameId) + " ★\n§eTeleporting soon..."));
        });
    }

    @Override
    public Location revealLocation() {
        if (venue == null) return null;
        Location l = venue.revealSurface(0);
        return l == null ? venue.revealPodium() : l;
    }

    @Override
    public void showCountdown(int seconds) {
        if (seconds == lastSeconds) return;
        lastSeconds = seconds;
        if (venue != null) {
            try { venue.showCountdown(seconds); } catch (Throwable ignored) { }
        }
        if (timer != null && timer.isValid()) {
            String text = seconds < 0 ? "§d§lDECIDING..." :
                    (seconds <= 5 ? "§c§l" : seconds <= 10 ? "§6§l" : "§e§l")
                            + String.format("%d:%02d", seconds / 60, seconds % 60);
            timer.text(c(text));
        }
    }

    @Override
    public void tick(int totalTick) {
        if (totalTick % 3 != 0) return;
        yaw = (yaw + 9f) % 360f;
        for (ItemDisplay d : icons) if (d != null && d.isValid()) d.setRotation(yaw, 0f);
        if (revealIcon != null && revealIcon.isValid()) revealIcon.setRotation(-yaw * 0.7f, 0f);
    }

    @Override public boolean isVenueWorld(World w) {
        return venue != null && w != null && w.equals(venue.world());
    }

    private String uiTag() { return DomeArena.TAG + ":" + sessionId + ":ui"; }
}
