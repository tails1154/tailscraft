package com.tailscraft.cameras;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/** Fixed-position CCTV cameras exposed to the TailsCraft client UI. */
public final class TailsCraftCamerasPlugin extends JavaPlugin implements PluginMessageListener {
    private static final String CHANNEL = "TC|Cameras";
    private static final int REQUEST_LIST = 1, REQUEST_VIEW = 2, REQUEST_STOP = 3, REQUEST_CREATE = 4, REQUEST_REMOVE = 5, LIST = 10, VIEW = 11, NOTICE = 12;
    private final Map<Integer, Camera> cameras = new LinkedHashMap<Integer, Camera>();
    private int nextId = 1;

    private static final class Camera {
        final int id; final UUID owner; final String ownerName; final String name; final String world; final double x, y, z; final float yaw, pitch;
        Camera(int id, UUID owner, String ownerName, String name, Location location) {
            this.id = id; this.owner = owner; this.ownerName = ownerName; this.name = name; this.world = location.getWorld().getName();
            this.x = location.getX(); this.y = location.getY(); this.z = location.getZ();
            this.yaw = location.getYaw(); this.pitch = location.getPitch();
        }
    }

    @Override public void onEnable() {
        loadCameras();
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
    }

    @Override public void onDisable() {
        saveCameras();
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL, this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        cameras.clear();
    }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("camera")) return false;
        if (!(sender instanceof Player)) { sender.sendMessage("Only players can manage cameras."); return true; }
        if (!sender.hasPermission("tailscraft.cameras.manage")) { sender.sendMessage(ChatColor.RED + "You cannot manage cameras."); return true; }
        Player player = (Player) sender;
        if (args.length == 0 || args[0].equalsIgnoreCase("list")) {
            player.sendMessage(ChatColor.YELLOW + "Cameras: " + cameras.size());
            for (Camera camera : cameras.values()) player.sendMessage(ChatColor.GRAY + "#" + camera.id + " " + camera.name);
            return true;
        }
        if (args[0].equalsIgnoreCase("create")) {
            if (args.length < 2) { player.sendMessage(ChatColor.RED + "Usage: /camera create <name>"); return true; }
            String name = join(args, 1);
            Camera camera = new Camera(nextId++, player.getUniqueId(), player.getName(), name, player.getLocation()); cameras.put(camera.id, camera); saveCameras();
            player.sendMessage(ChatColor.GREEN + "Created camera #" + camera.id + " " + camera.name);
            return true;
        }
        if (args[0].equalsIgnoreCase("remove") && args.length >= 2) {
            try { int id = Integer.parseInt(args[1]); Camera camera = cameras.get(id); if (camera != null && camera.owner.equals(player.getUniqueId())) { cameras.remove(id); saveCameras(); player.sendMessage(ChatColor.GREEN + "Removed camera #" + id); } else player.sendMessage(ChatColor.RED + "Camera not found."); }
            catch (NumberFormatException exception) { player.sendMessage(ChatColor.RED + "Camera ID must be a number."); }
            return true;
        }
        player.sendMessage(ChatColor.RED + "Usage: /camera <create|remove|list> [name|id]"); return true;
    }

    @Override public void onPluginMessageReceived(String channel, Player player, byte[] bytes) {
        if (!CHANNEL.equals(channel) || bytes == null || bytes.length < 1) return;
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            int type = in.readUnsignedByte();
        if (type == REQUEST_LIST) sendList(player);
            else if (type == REQUEST_VIEW) sendView(player, in.readInt());
            else if (type == REQUEST_STOP) { }
            else if (type == REQUEST_CREATE && player.hasPermission("tailscraft.cameras.manage")) createFromClient(player, readText(in));
            else if (type == REQUEST_REMOVE) removeFromClient(player, in.readInt());
        } catch (IOException | RuntimeException exception) { getLogger().warning("Invalid camera packet from " + player.getName()); }
    }

    private void createFromClient(Player player, String name) throws IOException {
        if (name == null || name.trim().isEmpty()) return;
        Camera camera = new Camera(nextId++, player.getUniqueId(), player.getName(), name.trim(), player.getLocation()); cameras.put(camera.id, camera); saveCameras();
        sendNotice(player, "Created camera #" + camera.id + " " + camera.name); sendList(player);
    }

    private void removeFromClient(Player player, int id) throws IOException {
        Camera camera = cameras.get(id);
        if (camera != null && owns(camera, player)) { cameras.remove(id); saveCameras(); sendNotice(player, "Removed camera #" + id); sendList(player); }
    }

    private void sendNotice(Player player, String message) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(160); DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(NOTICE); writeText(out, message); out.flush(); player.sendPluginMessage(this, CHANNEL, bytes.toByteArray());
    }

    private void sendList(Player player) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(512); DataOutputStream out = new DataOutputStream(bytes);
        List<Camera> owned = new ArrayList<Camera>();
        for (Camera camera : cameras.values()) if (owns(camera, player)) owned.add(camera);
        out.writeByte(LIST); out.writeShort(owned.size());
        for (Camera camera : owned) writeCamera(out, camera);
        out.flush(); player.sendPluginMessage(this, CHANNEL, bytes.toByteArray());
    }

    private void sendView(Player player, int id) throws IOException {
        Camera camera = cameras.get(id); if (camera == null || !owns(camera, player)) return;
        if (player.getWorld() == null || !player.getWorld().getName().equals(camera.world)) return;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(256); DataOutputStream out = new DataOutputStream(bytes);
        out.writeByte(VIEW); writeCamera(out, camera); out.flush(); player.sendPluginMessage(this, CHANNEL, bytes.toByteArray());
    }

    private static void writeCamera(DataOutputStream out, Camera camera) throws IOException {
        out.writeInt(camera.id); byte[] name = camera.name.getBytes(StandardCharsets.UTF_8); out.writeShort(Math.min(name.length, 128)); out.write(name, 0, Math.min(name.length, 128));
        byte[] world = camera.world.getBytes(StandardCharsets.UTF_8); out.writeShort(Math.min(world.length, 128)); out.write(world, 0, Math.min(world.length, 128));
        out.writeDouble(camera.x); out.writeDouble(camera.y); out.writeDouble(camera.z); out.writeFloat(camera.yaw); out.writeFloat(camera.pitch);
    }

    private void loadCameras() {
        cameras.clear(); nextId = 1;
        org.bukkit.configuration.ConfigurationSection section = getConfig().getConfigurationSection("cameras");
        if (section == null) return;
        for (String key : section.getKeys(false)) try {
            int id = Integer.parseInt(key); String path = "cameras." + key;
            org.bukkit.World world = Bukkit.getWorld(getConfig().getString(path + ".world"));
            UUID owner = UUID.fromString(getConfig().getString(path + ".owner"));
            if (world == null) continue;
            Location location = new Location(world, getConfig().getDouble(path + ".x"), getConfig().getDouble(path + ".y"), getConfig().getDouble(path + ".z"), (float) getConfig().getDouble(path + ".yaw"), (float) getConfig().getDouble(path + ".pitch"));
            cameras.put(id, new Camera(id, owner, getConfig().getString(path + ".ownerName", ""), getConfig().getString(path + ".name", "Camera " + id), location)); nextId = Math.max(nextId, id + 1);
        } catch (RuntimeException ignored) { }
    }

    private void saveCameras() {
        getConfig().set("cameras", null);
        for (Camera camera : cameras.values()) {
            String path = "cameras." + camera.id;
            getConfig().set(path + ".owner", camera.owner.toString()); getConfig().set(path + ".ownerName", camera.ownerName); getConfig().set(path + ".name", camera.name); getConfig().set(path + ".world", camera.world);
            getConfig().set(path + ".x", camera.x); getConfig().set(path + ".y", camera.y); getConfig().set(path + ".z", camera.z); getConfig().set(path + ".yaw", camera.yaw); getConfig().set(path + ".pitch", camera.pitch);
        }
        saveConfig();
    }
    private static boolean owns(Camera camera, Player player) { return camera.owner.equals(player.getUniqueId()) || (camera.ownerName != null && camera.ownerName.equalsIgnoreCase(player.getName())); }
    private static String readText(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort(); if (length > 128) throw new IOException("Camera name too long");
        byte[] bytes = new byte[length]; in.readFully(bytes); return new String(bytes, StandardCharsets.UTF_8);
    }
    private static void writeText(DataOutputStream out, String value) throws IOException {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        int length = Math.min(bytes.length, 128); out.writeShort(length); out.write(bytes, 0, length);
    }
    private static String join(String[] args, int start) { StringBuilder result = new StringBuilder(); for (int i = start; i < args.length; ++i) { if (result.length() > 0) result.append(' '); result.append(args[i]); } return result.toString(); }
}
