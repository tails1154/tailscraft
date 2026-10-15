package net.minecraft.client.voice;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import io.netty.buffer.Unpooled;
import net.lax1dude.eaglercraft.EaglercraftUUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;

/** Client-side TCV1 transport. Audio stays in browser WebRTC; Minecraft carries control only. */
public final class TailsCraftVoiceClient {
    private static final boolean PREFER_RELAY = true;
    private static final String CHANNEL = "TC|Voice";
    private static final int MAGIC = 0x54435631;
    private static final int VERSION = 1;
    private static final int HELLO = 1, WELCOME = 2, PEER_JOIN = 3, PEER_LEAVE = 4, SIGNAL = 5, POSITION = 6, MUTE = 7, ERROR = 8, LEAVE = 9, AUDIO = 10, SNAPSHOT = 11;
    private static final int OFFER = 1, ANSWER = 2, ICE = 3;
    private static final Map<String, String> peers = new HashMap<String, String>();
    private static NetHandlerPlayClient connection;
    private static EaglercraftUUID localId;
    private static boolean active;
    private static boolean browserReady;
    private static int mediaPeers;
    private static boolean audioBlocked;
    private static String mediaState;
    private static boolean relayStarted;
    private static long enabledAt;
    private static long lastPoll;
    private static long lastHello;

    private TailsCraftVoiceClient() {}

    public static void enable(NetHandlerPlayClient handler) {
        Minecraft mc = Minecraft.getMinecraft();
        if (handler == null || mc.player == null || !VoicePlatform.isSecureContext()) return;
        connection = handler;
        localId = mc.player.getUniqueID();
        active = true;
        sendHello(handler, mc.getSession().getUsername());
        VoicePlatform.start();
    }

    public static void disable() {
        if (active && connection != null && localId != null) sendLeave();
        active = false;
        peers.clear();
        connection = null;
        localId = null;
        browserReady = false;
        mediaPeers = 0;
        audioBlocked = false;
        mediaState = null;
        relayStarted = false;
        enabledAt = System.currentTimeMillis();
        lastPoll = 0L;
        lastHello = 0L;
        VoicePlatform.stop();
        VoicePlatform.stopRelay();
        relayStarted = false;
    }

    public static boolean isActive() { return active; }

    /** Returns true only after the server has advertised this opted-in TCV1 peer. */
    public static boolean hasVoicePeer(EaglercraftUUID id) {
        if (id != null && peers.containsKey(id.toString())) return true;
        return false;
    }

    /** Name fallback is applied by RenderPlayer when a proxy rewrites UUIDs. */
    public static boolean hasVoicePeer(String name) {
        if (name == null) return false;
        for (String peerName : peers.values()) if (name.equals(peerName)) return true;
        return false;
    }

    public static String getStatus() {
        if (!VoicePlatform.isSecureContext()) return "HTTPS required";
        if (!VoiceConsentState.isOptedIn()) return "Voice chat disabled";
        if (VoiceConsentState.getError() != null) return VoiceConsentState.getError();
        if (!browserReady) return "Waiting for microphone permission...";
        if (audioBlocked) return "Remote audio blocked; click this menu once";
        int count = peers.size();
        if (count == 0) return "Voice enabled; no TCV1 peers opted in";
        if (PREFER_RELAY && relayStarted) return "WebSocket relay active; TCV1 peers: " + count;
        return "TCV1 peers: " + count + "; audio connected: " + mediaPeers
                + (mediaState == null ? "" : " (" + mediaState + ")");
    }

    public static void setMuted(boolean muted) {
        VoicePlatform.setMuted(muted);
        if (active && connection != null) sendMute(muted);
    }

    public static void setDeafened(boolean deafened) { VoicePlatform.setDeafened(deafened); }

    public static void resumeAudio() { if (active) VoicePlatform.resumeAudio(); }

    public static void tick() {
        if (!active || connection == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) VoicePlatform.setLocalPosition(mc.player.posX, mc.player.posY, mc.player.posZ);
        long now = System.currentTimeMillis();
        if (PREFER_RELAY && !relayStarted && !peers.isEmpty() && browserReady) {
            VoicePlatform.startRelay();
            relayStarted = true;
            mediaState = "relay";
        } else if (!PREFER_RELAY && relayStarted && mediaPeers > 0) {
            VoicePlatform.stopRelay();
            relayStarted = false;
        }
        // Re-register periodically so a PlugMan reload can recover the
        // server-side peer registry without forcing players out of the world.
        if (now - lastHello >= 5000L) {
            sendHello(connection, mc.getSession().getUsername());
            lastHello = now;
        }
        if (now - lastPoll < 10L) return;
        lastPoll = now;
        String events = VoicePlatform.poll();
        if (events == null || events.length() == 0) return;
        try {
            JSONArray array = new JSONArray(events);
            for (int i = 0; i < array.length(); ++i) handleBrowserEvent(array.getJSONObject(i));
        } catch (RuntimeException ignored) {
            // Browser errors are surfaced by the consent UI in a later pass; never break the game tick.
        }
    }

    public static void handlePacket(PacketBuffer data) throws IOException {
        if (data == null || data.readableBytes() < 6) return;
        if (data.readInt() != MAGIC || data.readUnsignedByte() != VERSION) return;
        int type = data.readUnsignedByte();
        if (type == WELCOME) {
            readUuid(data);
            readText(data);
        } else if (type == PEER_JOIN) {
            String id = readUuid(data).toString();
            String name = readText(data);
            peers.put(id, name);
            if (!PREFER_RELAY) VoicePlatform.addPeer(id, localId != null && localId.toString().compareTo(id) < 0);
        } else if (type == PEER_LEAVE) {
            String id = readUuid(data).toString();
            peers.remove(id);
            VoicePlatform.removePeer(id);
        } else if (type == SIGNAL) {
            String source = readUuid(data).toString();
            readUuid(data);
            int signalType = data.readUnsignedByte();
            VoicePlatform.signal(source, signalType, readText(data));
        } else if (type == MUTE) {
            String id = readUuid(data).toString();
            VoicePlatform.setPeerMuted(id, data.readBoolean());
        } else if (type == POSITION) {
            String id = readUuid(data).toString();
            VoicePlatform.setPeerPosition(id, data.readDouble(), data.readDouble(), data.readDouble());
        } else if (type == ERROR) {
            readText(data);
        } else if (type == AUDIO) {
            String source = readUuid(data).toString();
            int length = data.readInt();
            if (length < 1 || length > 24000 || length > data.readableBytes()) throw new IOException("Invalid TCV1 audio");
            byte[] audio = new byte[length];
            data.readBytes(audio);
            VoicePlatform.receiveRelayAudio(source, net.lax1dude.eaglercraft.Base64.encodeBase64String(audio));
        } else if (type == SNAPSHOT) {
            int count = data.readUnsignedShort();
            if (count > 128) throw new IOException("Invalid TCV1 peer snapshot");
            Map<String, String> current = new HashMap<String, String>();
            for (int i = 0; i < count; ++i) {
                String id = readUuid(data).toString();
                current.put(id, readText(data));
            }
            for (String id : new HashMap<String, String>(peers).keySet()) {
                if (!current.containsKey(id)) VoicePlatform.removePeer(id);
            }
            peers.clear();
            peers.putAll(current);
            if (!PREFER_RELAY) {
                for (String id : current.keySet()) {
                    VoicePlatform.addPeer(id, localId != null && localId.toString().compareTo(id) < 0);
                }
            }
        }
    }

    private static void handleBrowserEvent(JSONObject event) {
        String type = event.optString("type", "");
        String id = event.optString("id", "");
        if ("offer".equals(type) || "answer".equals(type) || "ice".equals(type)) {
            String target = id;
            if (!peers.containsKey(target)) return;
            int signal = "offer".equals(type) ? OFFER : ("answer".equals(type) ? ANSWER : ICE);
            sendSignal(new EaglercraftUUID(target), signal, event.optString("data", ""));
        } else if ("ready".equals(type)) {
            browserReady = true;
            VoiceConsentState.setError(null);
            if (PREFER_RELAY && !peers.isEmpty() && !relayStarted) {
                VoicePlatform.startRelay();
                relayStarted = true;
                mediaState = "relay";
            }
        } else if ("error".equals(type)) {
            browserReady = false;
            String error = event.optString("data", "Voice chat could not start");
            VoiceConsentState.setError(error);
        } else if ("open".equals(type)) {
            ++mediaPeers;
            audioBlocked = false;
        } else if ("close".equals(type)) {
            if (mediaPeers > 0) --mediaPeers;
        } else if ("audioBlocked".equals(type)) {
            audioBlocked = true;
        } else if ("state".equals(type)) {
            mediaState = event.optString("data", "");
            if ("failed".equals(mediaState)) VoiceConsentState.setError("Voice connection failed; reopen the voice menu to retry");
        } else if ("relayAudio".equals(type)) {
            sendAudio(event.optString("data", ""));
        }
    }

    private static void sendHello(NetHandlerPlayClient handler, String name) {
        try {
            PacketBuffer out = buffer(HELLO);
            writeText(out, name);
            handler.sendPacket(new CPacketCustomPayload(CHANNEL, out));
        } catch (IOException ignored) {}
    }

    private static void sendSignal(EaglercraftUUID target, int signalType, String text) {
        if (connection == null || localId == null) return;
        try {
            PacketBuffer out = buffer(SIGNAL);
            writeUuid(out, localId); writeUuid(out, target); out.writeByte(signalType); writeText(out, text);
            connection.sendPacket(new CPacketCustomPayload(CHANNEL, out));
        } catch (IOException ignored) {}
    }

    private static void sendMute(boolean muted) {
        PacketBuffer out = buffer(MUTE); writeUuid(out, localId); out.writeBoolean(muted);
        connection.sendPacket(new CPacketCustomPayload(CHANNEL, out));
    }

    private static void sendAudio(String base64) {
        if (connection == null || localId == null || base64 == null || base64.length() == 0) return;
        try {
            byte[] audio = net.lax1dude.eaglercraft.Base64.decodeBase64(base64);
            if (audio.length < 1 || audio.length > 24000) return;
            PacketBuffer out = buffer(AUDIO);
            writeUuid(out, localId); out.writeInt(audio.length); out.writeBytes(audio);
            connection.sendPacket(new CPacketCustomPayload(CHANNEL, out));
        } catch (RuntimeException ignored) {
        }
    }

    private static void sendLeave() {
        try {
            connection.sendPacket(new CPacketCustomPayload(CHANNEL, buffer(LEAVE)));
        } catch (RuntimeException ignored) {
        }
    }

    private static PacketBuffer buffer(int type) { PacketBuffer out = new PacketBuffer(Unpooled.buffer(256)); out.writeInt(MAGIC); out.writeByte(VERSION); out.writeByte(type); return out; }
    private static void writeUuid(PacketBuffer out, EaglercraftUUID id) { out.writeLong(id.msb); out.writeLong(id.lsb); }
    private static EaglercraftUUID readUuid(PacketBuffer in) { return new EaglercraftUUID(in.readLong(), in.readLong()); }
    private static void writeText(PacketBuffer out, String text) throws IOException { byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8); if (bytes.length > 32768) throw new IOException("TCV1 text too large"); out.writeInt(bytes.length); out.writeBytes(bytes); }
    private static String readText(PacketBuffer in) throws IOException { int len = in.readInt(); if (len < 0 || len > 32768 || len > in.readableBytes()) throw new IOException("Invalid TCV1 text"); byte[] bytes = new byte[len]; in.readBytes(bytes); return new String(bytes, StandardCharsets.UTF_8); }
}
