package com.mcm.games.parkourtag;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Per-viewer glowing outline (Runner Vision): sends one player an entity-metadata packet that sets the glowing bit
 * on another player, so only that viewer sees the outline through blocks. Paper 1.21.1 runs with Mojang names, so
 * this uses plain reflection on {@code net.minecraft} classes (no ProtocolLib, no compile-time NMS dependency).
 * If anything is missing, {@link #available()} is false and the caller falls back to particles.
 */
final class GlowPackets {
    private static boolean ready;
    private static Object flagsAccessor;          // EntityDataAccessor<Byte> Entity.DATA_SHARED_FLAGS_ID
    private static Method dataValueCreate;        // SynchedEntityData.DataValue.create(accessor, value)
    private static Constructor<?> packetCtor;     // ClientboundSetEntityDataPacket(int, List)
    private static Method getHandle;              // CraftPlayer.getHandle()
    private static Field connectionField;         // ServerPlayer.connection
    private static Method sendMethod;             // ServerCommonPacketListenerImpl.send(Packet)

    private GlowPackets() {}

    static void init(Plugin plugin) {
        try {
            Class<?> entity = Class.forName("net.minecraft.world.entity.Entity");
            Field f = entity.getDeclaredField("DATA_SHARED_FLAGS_ID");
            f.setAccessible(true);
            flagsAccessor = f.get(null);
            Class<?> accessorCls = Class.forName("net.minecraft.network.syncher.EntityDataAccessor");
            Class<?> dataValue = Class.forName("net.minecraft.network.syncher.SynchedEntityData$DataValue");
            dataValueCreate = dataValue.getMethod("create", accessorCls, Object.class);
            Class<?> pkt = Class.forName("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
            packetCtor = pkt.getConstructor(int.class, List.class);
            Class<?> craftPlayer = Class.forName("org.bukkit.craftbukkit.entity.CraftPlayer");
            getHandle = craftPlayer.getMethod("getHandle");
            Class<?> serverPlayer = Class.forName("net.minecraft.server.level.ServerPlayer");
            connectionField = serverPlayer.getField("connection");
            Class<?> packetCls = Class.forName("net.minecraft.network.protocol.Packet");
            sendMethod = connectionField.getType().getMethod("send", packetCls);
            ready = true;
        } catch (Throwable t) {
            ready = false;
            plugin.getLogger().warning("Runner Vision outline packets unavailable (" + t + "); using particles instead.");
        }
    }

    static boolean available() { return ready; }

    /** Shows (glow = true) or hides the outline of {@code target} for {@code viewer} only. */
    static boolean send(Player viewer, Player target, boolean glow) {
        if (!ready || viewer == null || target == null || !viewer.isOnline()) return false;
        try {
            byte flags = 0;
            if (target.getFireTicks() > 0) flags |= 0x01;
            if (target.isSneaking()) flags |= 0x02;
            if (target.isSprinting()) flags |= 0x08;
            if (target.isSwimming()) flags |= 0x10;
            if (target.isInvisible()) flags |= 0x20;
            if (glow || target.isGlowing()) flags |= 0x40;
            if (target.isGliding()) flags |= (byte) 0x80;
            Object value = dataValueCreate.invoke(null, flagsAccessor, Byte.valueOf(flags));
            Object packet = packetCtor.newInstance(target.getEntityId(), List.of(value));
            Object conn = connectionField.get(getHandle.invoke(viewer));
            sendMethod.invoke(conn, packet);
            return true;
        } catch (Throwable t) {
            ready = false;
            return false;
        }
    }
}
