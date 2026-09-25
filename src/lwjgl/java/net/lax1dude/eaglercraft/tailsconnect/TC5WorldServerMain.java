package net.lax1dude.eaglercraft.tailsconnect;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import net.lax1dude.eaglercraft.internal.IPCPacketData;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacket00StartServer;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacket02InitWorld;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacket07ImportWorld;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacketBase;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacketFFProcessKeepAlive;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacketManager;
import net.lax1dude.eaglercraft.sp.server.EaglerIntegratedServerWorker;
import net.lax1dude.eaglercraft.sp.server.internal.ServerPlatformSingleplayer;
import net.lax1dude.eaglercraft.sp.server.internal.lwjgl.MemoryConnection;

/**
 * Headless TC5 host for the existing Eagler integrated-server implementation.
 *
 * The controller protocol is deliberately small and private to the local
 * TailsConnect daemon:
 *   frame = uint32 length, byte type, body
 *   type 1: OPEN, type 4: CLOSE, body uint16 channel length + UTF-8 channel
 *   type 2: DATA, body uint16 channel length + channel + packet bytes
 *   type 3: IPC, body serialized IPC packet
 *
 * Outbound type 2 frames are player packets and type 3 frames are IPC/status
 * packets. The daemon is responsible for authenticating and routing channels.
 */
public final class TC5WorldServerMain {

    private static final byte OPEN = 1;
    private static final byte DATA = 2;
    private static final byte IPC = 3;
    private static final byte CLOSE = 4;
    private static final int MAX_FRAME = 16 * 1024 * 1024;

    private static final IPCPacketManager PACKETS = new IPCPacketManager();
    private static volatile boolean running = true;
    private static volatile boolean startSent;
    private static String hostedWorldName;
    private static String hostedFolderName;

    private TC5WorldServerMain() {
    }

    public static void main(String[] args) throws Exception {
        String epkPath = option(args, "--epk");
        String worldName = option(args, "--world-name");
        String folderName = option(args, "--folder-name");
        int port = Integer.parseInt(option(args, "--control-port"));
        if (epkPath == null || worldName == null || folderName == null) {
            throw new IllegalArgumentException("--epk, --world-name, --folder-name, and --control-port are required");
        }
        hostedWorldName = worldName;
        hostedFolderName = folderName;

        ServerPlatformSingleplayer.initializeContext();
        Thread worker = new Thread(EaglerIntegratedServerWorker::serverMain, "TC5-EaglerServer");
        // The controller owns the process lifetime. If the bridge disconnects,
        // the worker must not keep an orphaned Minecraft server JVM alive.
        worker.setDaemon(true);
        worker.start();

        try (ServerSocket listener = new ServerSocket(port, 1, java.net.InetAddress.getLoopbackAddress())) {
            listener.setReuseAddress(true);
            try (Socket socket = listener.accept()) {
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                DataInputStream input = new DataInputStream(socket.getInputStream());
                DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                Thread outbound = new Thread(() -> drainServerPackets(output), "TC5-outbound");
                outbound.setDaemon(true);
                outbound.start();

                enqueue(new IPCPacket07ImportWorld(worldName,
                        Files.readAllBytes(Paths.get(epkPath)), IPCPacket07ImportWorld.WORLD_FORMAT_EAG, (byte) 0));
                while (running) {
                    int length;
                    try {
                        length = input.readInt();
                    } catch (EOFException eof) {
                        break;
                    }
                    if (length < 1 || length > MAX_FRAME) {
                        throw new IOException("Invalid TC5 bridge frame length: " + length);
                    }
                    byte type = input.readByte();
                    byte[] body = new byte[length - 1];
                    input.readFully(body);
                    if (type == OPEN) {
                        readChannelCommand(body, true);
                    } else if (type == CLOSE) {
                        readChannelCommand(body, false);
                    } else if (type == DATA) {
                        readDataCommand(body);
                    } else if (type == IPC) {
                        enqueue(new IPCPacketData(SingleplayerServerController.IPC_CHANNEL, body));
                    } else {
                        throw new IOException("Unknown TC5 bridge frame type: " + type);
                    }
                }
                running = false;
                try {
                    enqueue(new net.lax1dude.eaglercraft.sp.ipc.IPCPacket01StopServer());
                } catch (Throwable ignored) {
                }
            }
        }
    }

    private static String option(String[] args, String name) {
        for (int i = 0; i + 1 < args.length; ++i) {
            if (name.equals(args[i])) {
                return args[i + 1];
            }
        }
        return null;
    }

    private static void enqueue(IPCPacketBase packet) throws IOException {
        enqueue(new IPCPacketData(SingleplayerServerController.IPC_CHANNEL, PACKETS.IPCSerialize(packet)));
    }

    private static void enqueue(IPCPacketData packet) {
        synchronized (MemoryConnection.clientToServerQueue) {
            MemoryConnection.clientToServerQueue.add(packet);
        }
    }

    private static volatile boolean importAck;

    private static void drainServerPackets(DataOutputStream output) {
        try {
            while (running) {
                List<IPCPacketData> packets;
                synchronized (MemoryConnection.serverToClientQueue) {
                    packets = MemoryConnection.serverToClientQueue.isEmpty() ? null
                            : new java.util.ArrayList<>(MemoryConnection.serverToClientQueue);
                    if (packets != null) {
                        MemoryConnection.serverToClientQueue.clear();
                    }
                }
                if (packets == null) {
                    Thread.sleep(2L);
                    continue;
                }
                for (IPCPacketData packet : packets) {
                    if (SingleplayerServerController.IPC_CHANNEL.equals(packet.channel)) {
                        markImportAck(packet.contents);
                        writeFrame(output, IPC, packet.contents);
                    } else {
                        writeChannelFrame(output, DATA, packet.channel, packet.contents);
                    }
                }
            }
        } catch (Throwable ignored) {
            running = false;
        }
    }

    private static void markImportAck(byte[] contents) {
        try {
            IPCPacketBase packet = PACKETS.IPCDeserialize(contents);
            if (packet instanceof IPCPacketFFProcessKeepAlive
                    && ((IPCPacketFFProcessKeepAlive) packet).ack == IPCPacket07ImportWorld.ID) {
                importAck = true;
                if (!startSent) {
                    startSent = true;
                    try {
                        enqueue(new IPCPacket00StartServer("eaglercraft", hostedFolderName, hostedWorldName,
                                "TC5", 0, 8, false));
                    } catch (IOException ex) {
                        running = false;
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void readChannelCommand(byte[] body, boolean open) throws IOException {
        java.io.ByteArrayInputStream bytes = new java.io.ByteArrayInputStream(body);
        DataInputStream input = new DataInputStream(bytes);
        String channel = input.readUTF();
        if (open) {
            enqueue(new net.lax1dude.eaglercraft.sp.ipc.IPCPacket0CPlayerChannel(channel, true));
        } else {
            enqueue(new net.lax1dude.eaglercraft.sp.ipc.IPCPacket0CPlayerChannel(channel, false));
        }
    }

    private static void readDataCommand(byte[] body) throws IOException {
        java.io.ByteArrayInputStream bytes = new java.io.ByteArrayInputStream(body);
        DataInputStream input = new DataInputStream(bytes);
        String channel = input.readUTF();
        byte[] payload = new byte[bytes.available()];
        input.readFully(payload);
        enqueue(new IPCPacketData(channel, payload));
    }

    private static void writeChannelFrame(DataOutputStream output, byte type, String channel, byte[] payload)
            throws IOException {
        java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        DataOutputStream body = new DataOutputStream(bytes);
        body.writeUTF(channel);
        body.write(payload);
        body.flush();
        writeFrame(output, type, bytes.toByteArray());
    }

    private static synchronized void writeFrame(DataOutputStream output, byte type, byte[] body) throws IOException {
        output.writeInt(body.length + 1);
        output.writeByte(type);
        output.write(body);
        output.flush();
    }
}
