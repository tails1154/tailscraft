package net.tailscraft.voice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.server.MinecraftServer;
import net.lax1dude.eaglercraft.EaglercraftUUID;

/**
 * TCV1 signaling relay for the integrated server. It carries no microphone
 * data; browser WebRTC carries the actual audio after this relay negotiates it.
 */
public final class IntegratedVoiceRelay {
    private static final double DISCOVERY_RANGE = 128.0D;
    private static final Set<EaglercraftUUID> enabled = new HashSet<EaglercraftUUID>();
    private static final Set<String> visiblePairs = new HashSet<String>();

    private IntegratedVoiceRelay() {}

    public static void handle(MinecraftServer server, EntityPlayerMP sender, PacketBuffer data) throws IOException {
        byte[] bytes = new byte[data.readableBytes()];
        data.readBytes(bytes);
        TCVoiceProtocol.Packet packet = TCVoiceProtocol.decode(bytes);
        EaglercraftUUID senderId = uuid(sender);
        if (packet.type == TCVoiceProtocol.HELLO) {
            enabled.add(senderId);
            send(sender, basic(TCVoiceProtocol.WELCOME, senderId, "TCV1"));
            refresh(server);
        } else if (packet.type == TCVoiceProtocol.LEAVE) {
            leave(server, senderId);
        } else if (packet.type == TCVoiceProtocol.SIGNAL) {
            if (!senderId.equals(packet.source) || packet.target == null || !visiblePairs.contains(pairKey(senderId, packet.target))) return;
            EntityPlayerMP target = find(server, packet.target);
            if (target != null && enabled.contains(packet.target)) send(target, TCVoiceProtocol.encode(packet));
        } else if (packet.type == TCVoiceProtocol.MUTE) {
            if (!senderId.equals(packet.source)) return;
            for (EntityPlayerMP target : players(server)) {
                EaglercraftUUID targetId = uuid(target);
                if (!targetId.equals(senderId) && visiblePairs.contains(pairKey(senderId, targetId))) {
                    send(target, TCVoiceProtocol.encode(packet));
                }
            }
        } else if (packet.type == TCVoiceProtocol.AUDIO) {
            if (!senderId.equals(packet.source)) return;
            for (EntityPlayerMP target : players(server)) {
                EaglercraftUUID targetId = uuid(target);
                if (!targetId.equals(senderId) && enabled.contains(targetId) && visiblePairs.contains(pairKey(senderId, targetId))) {
                    send(target, TCVoiceProtocol.encode(packet));
                }
            }
        }
    }

    public static void tick(MinecraftServer server) {
        if (enabled.isEmpty()) return;
        refresh(server);
    }

    public static void leave(MinecraftServer server, EntityPlayerMP player) {
        leave(server, uuid(player));
    }

    private static void leave(MinecraftServer server, EaglercraftUUID id) {
        enabled.remove(id);
        List<String> removed = new ArrayList<String>();
        for (String pair : visiblePairs) if (pair.startsWith(id.toString() + ":") || pair.endsWith(":" + id.toString())) removed.add(pair);
        for (String pair : removed) visiblePairs.remove(pair);
        try {
            TCVoiceProtocol.Packet packet = new TCVoiceProtocol.Packet();
            packet.type = TCVoiceProtocol.PEER_LEAVE;
            packet.source = id;
            byte[] encoded = TCVoiceProtocol.encode(packet);
            for (EntityPlayerMP target : players(server)) {
                if (enabled.contains(uuid(target))) send(target, encoded);
            }
        } catch (IOException ignored) {
        }
    }

    private static void refresh(MinecraftServer server) {
        List<EntityPlayerMP> snapshot = players(server);
        Set<EaglercraftUUID> live = new HashSet<EaglercraftUUID>();
        for (EntityPlayerMP player : snapshot) live.add(uuid(player));
        for (EaglercraftUUID id : new HashSet<EaglercraftUUID>(enabled)) if (!live.contains(id)) leave(server, id);
        for (int i = 0; i < snapshot.size(); ++i) {
            EntityPlayerMP first = snapshot.get(i);
            EaglercraftUUID firstId = uuid(first);
            if (!enabled.contains(firstId)) continue;
            for (int j = i + 1; j < snapshot.size(); ++j) {
                EntityPlayerMP second = snapshot.get(j);
                EaglercraftUUID secondId = uuid(second);
                if (!enabled.contains(secondId)) continue;
                String pair = pairKey(firstId, secondId);
                boolean visible = first.getServerWorld().equals(second.getServerWorld())
                        && first.getDistanceSq(second.posX, second.posY, second.posZ) <= DISCOVERY_RANGE * DISCOVERY_RANGE;
                boolean known = visiblePairs.contains(pair);
                try {
                    if (visible && !known) {
                        visiblePairs.add(pair);
                        send(first, peerJoin(second));
                        send(second, peerJoin(first));
                    } else if (!visible && known) {
                        visiblePairs.remove(pair);
                        send(first, peerLeave(secondId));
                        send(second, peerLeave(firstId));
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    private static byte[] basic(int type, EaglercraftUUID source, String text) throws IOException {
        TCVoiceProtocol.Packet packet = new TCVoiceProtocol.Packet();
        packet.type = type;
        packet.source = source;
        packet.text = text;
        return TCVoiceProtocol.encode(packet);
    }

    private static byte[] peerJoin(EntityPlayerMP player) throws IOException {
        return basic(TCVoiceProtocol.PEER_JOIN, uuid(player), player.getName());
    }

    private static byte[] peerLeave(EaglercraftUUID id) throws IOException {
        return basic(TCVoiceProtocol.PEER_LEAVE, id, null);
    }

    private static void send(EntityPlayerMP player, byte[] bytes) {
        player.connection.sendPacket(new SPacketCustomPayload(TCVoiceProtocol.CHANNEL,
                new PacketBuffer(Unpooled.wrappedBuffer(bytes))));
    }

    private static EntityPlayerMP find(MinecraftServer server, EaglercraftUUID id) {
        for (EntityPlayerMP player : players(server)) if (uuid(player).equals(id)) return player;
        return null;
    }

    private static List<EntityPlayerMP> players(MinecraftServer server) {
        return new ArrayList<EntityPlayerMP>(server.getPlayerList().getPlayerList());
    }

    private static EaglercraftUUID uuid(EntityPlayerMP player) {
        return player.getUniqueID();
    }

    private static String pairKey(EaglercraftUUID first, EaglercraftUUID second) {
        return first.toString().compareTo(second.toString()) < 0
                ? first.toString() + ":" + second.toString() : second.toString() + ":" + first.toString();
    }
}
