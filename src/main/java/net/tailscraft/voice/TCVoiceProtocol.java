package net.tailscraft.voice;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import net.lax1dude.eaglercraft.EaglercraftUUID;

/**
 * TailsCraft Voice Protocol 1 (TCV1).
 *
 * Voice is opt-in: the client sends HELLO only after the player enables voice
 * and grants microphone permission. This class only describes the
 * signaling/control wire format. Voice media is
 * intentionally not sent through Minecraft plugin messages; browsers carry it
 * through the WebRTC peer connection created after OFFER/ANSWER/ICE exchange.
 */
public final class TCVoiceProtocol {
	public static final String CHANNEL = "TC|Voice";
	public static final int MAGIC = 0x54435631; // ASCII "TCV1"
	public static final int VERSION = 1;
	public static final int MAX_PACKET_BYTES = 65535;
	public static final int MAX_TEXT_BYTES = 32768;

	public static final int HELLO = 1;
	public static final int WELCOME = 2;
	public static final int PEER_JOIN = 3;
	public static final int PEER_LEAVE = 4;
	public static final int SIGNAL = 5;
	public static final int POSITION = 6;
	public static final int MUTE = 7;
	public static final int ERROR = 8;
	public static final int LEAVE = 9;
	public static final int AUDIO = 10;

	public static final int OFFER = 1;
	public static final int ANSWER = 2;
	public static final int ICE = 3;

	private TCVoiceProtocol() {
	}

	public static final class Packet {
		public int type;
		public EaglercraftUUID source;
		public EaglercraftUUID target;
		public String text;
		public int signalType;
		public double x;
		public double y;
		public double z;
		public boolean muted;
		public byte[] audio;

		public static Packet hello(String name) {
			Packet p = new Packet();
			p.type = HELLO;
			p.text = name;
			return p;
		}

		public static Packet signal(EaglercraftUUID target, int signalType, String data) {
			Packet p = new Packet();
			p.type = SIGNAL;
			p.target = target;
			p.signalType = signalType;
			p.text = data;
			return p;
		}
	}

	public static byte[] encode(Packet packet) throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream(256);
		DataOutputStream out = new DataOutputStream(bytes);
		out.writeInt(MAGIC);
		out.writeByte(VERSION);
		out.writeByte(packet.type);
		switch (packet.type) {
		case HELLO:
		case ERROR:
			writeText(out, packet.text);
			break;
		case LEAVE:
			break;
		case WELCOME:
		case PEER_JOIN:
		case PEER_LEAVE:
			writeUuid(out, packet.source);
			if (packet.type != PEER_LEAVE) writeText(out, packet.text);
			break;
		case SIGNAL:
			writeUuid(out, packet.source);
			writeUuid(out, packet.target);
			out.writeByte(packet.signalType);
			writeText(out, packet.text);
			break;
		case POSITION:
			writeUuid(out, packet.source);
			out.writeDouble(packet.x);
			out.writeDouble(packet.y);
			out.writeDouble(packet.z);
			break;
		case MUTE:
			writeUuid(out, packet.source);
			out.writeBoolean(packet.muted);
			break;
		case AUDIO:
			writeUuid(out, packet.source);
			if (packet.audio == null || packet.audio.length < 1 || packet.audio.length > 24000) throw new IOException("Invalid TCV1 audio");
			out.writeInt(packet.audio.length);
			out.write(packet.audio);
			break;
		default:
			throw new IOException("Unknown TCV1 packet type: " + packet.type);
		}
		out.flush();
		byte[] result = bytes.toByteArray();
		if (result.length > MAX_PACKET_BYTES) throw new IOException("TCV1 packet is too large");
		return result;
	}

	public static Packet decode(byte[] data) throws IOException {
		if (data == null || data.length < 6 || data.length > MAX_PACKET_BYTES) {
			throw new IOException("Invalid TCV1 packet length");
		}
		DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
		if (in.readInt() != MAGIC) throw new IOException("Invalid TCV1 magic");
		if (in.readUnsignedByte() != VERSION) throw new IOException("Unsupported TCV1 version");
		Packet packet = new Packet();
		packet.type = in.readUnsignedByte();
		switch (packet.type) {
		case HELLO:
		case ERROR:
			packet.text = readText(in);
			break;
		case LEAVE:
			break;
		case WELCOME:
		case PEER_JOIN:
		case PEER_LEAVE:
			packet.source = readUuid(in);
			if (packet.type != PEER_LEAVE) packet.text = readText(in);
			break;
		case SIGNAL:
			packet.source = readUuid(in);
			packet.target = readUuid(in);
			packet.signalType = in.readUnsignedByte();
			if (packet.signalType < OFFER || packet.signalType > ICE) throw new IOException("Invalid signal type");
			packet.text = readText(in);
			break;
		case POSITION:
			packet.source = readUuid(in);
			packet.x = in.readDouble();
			packet.y = in.readDouble();
			packet.z = in.readDouble();
			break;
		case MUTE:
			packet.source = readUuid(in);
			packet.muted = in.readBoolean();
			break;
		case AUDIO:
			packet.source = readUuid(in);
			int audioLength = in.readInt();
			if (audioLength < 1 || audioLength > 24000 || audioLength > in.available()) throw new IOException("Invalid TCV1 audio");
			packet.audio = new byte[audioLength];
			in.readFully(packet.audio);
			break;
		default:
			throw new IOException("Unknown TCV1 packet type: " + packet.type);
		}
		if (in.available() != 0) throw new IOException("Trailing TCV1 packet data");
		return packet;
	}

	private static void writeUuid(DataOutputStream out, EaglercraftUUID uuid) throws IOException {
		if (uuid == null) throw new IOException("TCV1 UUID is missing");
		out.writeLong(uuid.getMostSignificantBits());
		out.writeLong(uuid.getLeastSignificantBits());
	}

	private static EaglercraftUUID readUuid(DataInputStream in) throws IOException {
		return new EaglercraftUUID(in.readLong(), in.readLong());
	}

	private static void writeText(DataOutputStream out, String text) throws IOException {
		byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
		if (bytes.length > MAX_TEXT_BYTES) throw new IOException("TCV1 text is too large");
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	private static String readText(DataInputStream in) throws IOException {
		int length = in.readInt();
		if (length < 0 || length > MAX_TEXT_BYTES || length > in.available()) {
			throw new IOException("Invalid TCV1 text length");
		}
		byte[] bytes = new byte[length];
		in.readFully(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}
}
