package net.minecraft.client.cameras;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;

/** Client-side CCTV browser and fixed camera view controller. */
public final class SecurityCameraClient {
    public static final String CHANNEL = "TC|Cameras";
    private static final int REQUEST_LIST = 1, REQUEST_VIEW = 2, REQUEST_STOP = 3, REQUEST_CREATE = 4, REQUEST_REMOVE = 5, LIST = 10, VIEW = 11, NOTICE = 12;
    private static final List<Camera> cameras = new ArrayList<Camera>();
    private static Entity cameraEntity;
    private static boolean viewing;
    private static int revision;
    private static boolean loaded;

    public static final class Camera {
        public final int id; public final String name; public final String world;
        public final double x, y, z; public final float yaw, pitch;
        Camera(int id, String name, String world, double x, double y, double z, float yaw, float pitch) {
            this.id = id; this.name = name; this.world = world; this.x = x; this.y = y; this.z = z; this.yaw = yaw; this.pitch = pitch;
        }
    }

    private SecurityCameraClient() { }
    public static List<Camera> getCameras() { return Collections.unmodifiableList(cameras); }
    public static boolean isViewing() { return viewing; }
    public static int getRevision() { return revision; }
    public static boolean isLoaded() { return loaded; }

    public static void open() { if (!loaded) request(REQUEST_LIST); }
    public static void view(int id) { requestInt(REQUEST_VIEW, id); }
    public static void stop() {
        Minecraft mc = Minecraft.getMinecraft();
        if (cameraEntity != null) cameraEntity.setDead();
        cameraEntity = null; viewing = false;
        if (mc.player != null) mc.setRenderViewEntity(mc.player);
        request(REQUEST_STOP);
    }
    public static void create(String name) { requestText(REQUEST_CREATE, name); }
    public static void remove(int id) { requestInt(REQUEST_REMOVE, id); }

    private static void request(int type) {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        if (connection == null) return;
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(8)); buffer.writeByte(type);
        connection.sendPacket(new CPacketCustomPayload(CHANNEL, buffer));
    }
    private static void requestInt(int type, int value) {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection();
        if (connection == null) return;
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(12)); buffer.writeByte(type); buffer.writeInt(value);
        connection.sendPacket(new CPacketCustomPayload(CHANNEL, buffer));
    }
    private static void requestText(int type, String value) {
        NetHandlerPlayClient connection = Minecraft.getMinecraft().getConnection(); if (connection == null) return;
        byte[] text = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer(text.length + 4)); buffer.writeByte(type); buffer.writeShort(Math.min(text.length, 128)); buffer.writeBytes(text, 0, Math.min(text.length, 128));
        connection.sendPacket(new CPacketCustomPayload(CHANNEL, buffer));
    }

    public static void handlePacket(PacketBuffer in) throws IOException {
        int type = in.readUnsignedByte();
        if (type == LIST) {
            int count = in.readUnsignedShort(); if (count > 256) throw new IOException("Too many cameras");
            cameras.clear(); for (int i = 0; i < count; ++i) cameras.add(readCamera(in));
            ++revision;
            loaded = true;
        } else if (type == VIEW) {
            Camera camera = readCamera(in); show(camera);
        } else if (type == NOTICE) {
            Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new net.minecraft.util.text.TextComponentString(readText(in)));
        }
    }

    private static Camera readCamera(PacketBuffer in) throws IOException {
        int id = in.readInt(); String name = readText(in); String world = readText(in);
        return new Camera(id, name, world, in.readDouble(), in.readDouble(), in.readDouble(), in.readFloat(), in.readFloat());
    }
    private static String readText(PacketBuffer in) throws IOException {
        int length = in.readUnsignedShort(); if (length > 128 || length > in.readableBytes()) throw new IOException("Invalid camera text");
        byte[] bytes = new byte[length]; in.readBytes(bytes); return new String(bytes, StandardCharsets.UTF_8);
    }
    private static void show(Camera camera) {
        Minecraft mc = Minecraft.getMinecraft(); if (mc.world == null) return;
        if (cameraEntity == null) cameraEntity = new SecurityCameraEntity(mc.world);
        cameraEntity.setPositionAndRotation(camera.x, camera.y, camera.z, camera.yaw, camera.pitch);
        cameraEntity.prevPosX = camera.x; cameraEntity.prevPosY = camera.y; cameraEntity.prevPosZ = camera.z;
        cameraEntity.prevRotationYaw = camera.yaw; cameraEntity.prevRotationPitch = camera.pitch;
        mc.setRenderViewEntity(cameraEntity); viewing = true;
    }
}
