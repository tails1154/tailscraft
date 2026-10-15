package com.tailscraft.voice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/** Server-side TCV1 codec. Kept independent from the client build. */
final class TailsCraftVoiceCodec {
    static final String CHANNEL = "TC|Voice";
    static final int MAGIC = 0x54435631;
    static final int VERSION = 1;
    static final int MAX_PACKET = 65535;
    static final int MAX_TEXT = 32768;
    static final int HELLO = 1, WELCOME = 2, PEER_JOIN = 3, PEER_LEAVE = 4,
            SIGNAL = 5, POSITION = 6, MUTE = 7, ERROR = 8, LEAVE = 9, AUDIO = 10, SNAPSHOT = 11;
    static final int OFFER = 1, ANSWER = 2, ICE = 3;

    static final class Packet {
        int type;
        UUID source;
        UUID target;
        String text;
        int signalType;
        double x, y, z;
        boolean muted;
        byte[] audio;
    }

    static Packet decode(byte[] data) throws IOException {
        if (data == null || data.length < 6 || data.length > MAX_PACKET) throw new IOException("bad length");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        if (in.readInt() != MAGIC || in.readUnsignedByte() != VERSION) throw new IOException("bad header");
        Packet p = new Packet();
        p.type = in.readUnsignedByte();
        switch (p.type) {
        case HELLO: case ERROR: p.text = text(in); break;
        case LEAVE: break;
        case SIGNAL:
            p.source = uuid(in); p.target = uuid(in); p.signalType = in.readUnsignedByte(); p.text = text(in);
            if (p.signalType < OFFER || p.signalType > ICE) throw new IOException("bad signal");
            break;
        case POSITION: p.source = uuid(in); p.x = in.readDouble(); p.y = in.readDouble(); p.z = in.readDouble(); break;
        case MUTE: p.source = uuid(in); p.muted = in.readBoolean(); break;
        case AUDIO:
            p.source = uuid(in);
            int audioLength = in.readInt();
            if (audioLength < 1 || audioLength > 24000 || audioLength > in.available()) throw new IOException("bad audio length");
            p.audio = new byte[audioLength]; in.readFully(p.audio);
            break;
        default: throw new IOException("client packet type is not accepted: " + p.type);
        }
        if (in.available() != 0) throw new IOException("trailing data");
        return p;
    }

    static byte[] basic(int type, UUID source, String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(128);
        DataOutputStream out = new DataOutputStream(bytes);
        header(out, type);
        if (type == WELCOME || type == PEER_JOIN || type == PEER_LEAVE) uuid(out, source);
        if (type == WELCOME || type == PEER_JOIN || type == ERROR) writeText(out, text);
        out.flush();
        return bytes.toByteArray();
    }

    static byte[] signal(UUID source, UUID target, int signalType, String text) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(256);
        DataOutputStream out = new DataOutputStream(bytes);
        header(out, SIGNAL); uuid(out, source); uuid(out, target); out.writeByte(signalType); writeText(out, text); out.flush();
        return bytes.toByteArray();
    }

    static byte[] position(UUID source, double x, double y, double z) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(64);
        DataOutputStream out = new DataOutputStream(bytes);
        header(out, POSITION); uuid(out, source); out.writeDouble(x); out.writeDouble(y); out.writeDouble(z); out.flush();
        return bytes.toByteArray();
    }

    static byte[] mute(UUID source, boolean muted) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(32);
        DataOutputStream out = new DataOutputStream(bytes);
        header(out, MUTE); uuid(out, source); out.writeBoolean(muted); out.flush();
        return bytes.toByteArray();
    }

    static byte[] audio(UUID source, byte[] audio) throws IOException {
        if (audio == null || audio.length < 1 || audio.length > 24000) throw new IOException("audio too large");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(audio.length + 32);
        DataOutputStream out = new DataOutputStream(bytes);
        header(out, AUDIO); uuid(out, source); out.writeInt(audio.length); out.write(audio); out.flush();
        return bytes.toByteArray();
    }

    static byte[] snapshot(List<PlayerEntry> players) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(256);
        DataOutputStream out = new DataOutputStream(bytes);
        header(out, SNAPSHOT);
        out.writeShort(players.size());
        for (PlayerEntry player : players) {
            uuid(out, player.id);
            writeText(out, player.name);
        }
        out.flush();
        return bytes.toByteArray();
    }

    static final class PlayerEntry {
        final UUID id;
        final String name;
        PlayerEntry(UUID id, String name) { this.id = id; this.name = name; }
    }

    private static void header(DataOutputStream out, int type) throws IOException { out.writeInt(MAGIC); out.writeByte(VERSION); out.writeByte(type); }
    private static UUID uuid(DataInputStream in) throws IOException { return new UUID(in.readLong(), in.readLong()); }
    private static void uuid(DataOutputStream out, UUID uuid) throws IOException { out.writeLong(uuid.getMostSignificantBits()); out.writeLong(uuid.getLeastSignificantBits()); }
    private static String text(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > MAX_TEXT || len > in.available()) throw new IOException("bad text length");
        byte[] data = new byte[len]; in.readFully(data); return new String(data, StandardCharsets.UTF_8);
    }
    private static void writeText(DataOutputStream out, String value) throws IOException {
        byte[] data = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        if (data.length > MAX_TEXT) throw new IOException("text too large");
        out.writeInt(data.length); out.write(data);
    }
}
