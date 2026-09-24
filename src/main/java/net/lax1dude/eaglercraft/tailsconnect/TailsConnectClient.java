package net.lax1dude.eaglercraft.tailsconnect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.lax1dude.eaglercraft.*;
import net.lax1dude.eaglercraft.internal.*;
import net.lax1dude.eaglercraft.profile.EaglerProfile;
import net.lax1dude.eaglercraft.socket.ConnectionHandshake;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.lax1dude.eaglercraft.sp.internal.ClientPlatformSingleplayer;
import net.lax1dude.eaglercraft.sp.ipc.IPCPacket0CPlayerChannel;
import net.lax1dude.eaglercraft.sp.socket.NetHandlerSingleplayerLogin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.login.client.CPacketLoginStart;
import net.minecraft.util.text.TextComponentString;

/** One host plus three independently routed guests over the broadcast relay. */
public final class TailsConnectClient {
    public static final String SERVER = "wss://tails1154.com:9842";
    public static final String CHANNEL_PREFIX = "tailsconnect-v2-";
    private static final String GAME = "tailscraft-1.12.2-packets-v2";
    private static final Map<String, Long> guests = new LinkedHashMap<>();
    private static IWebSocketClient socket;
    private static TailsConnectNetworkManager network;
    private static String state = "Idle", roomCode, error, pending, selfId, hostId;
    private static boolean hosting;
    private static int maxPlayers = 4;
    private static long started, heartbeat, hostHeartbeat, hello;

    public static void reset() { shutdown("TailsConnect stopped", true); error = null; }

    private static void shutdown(String reason, boolean announce) {
        // Detach before invoking the Minecraft disconnect handler: it can call closeChannel again.
        IWebSocketClient oldSocket = socket;
        TailsConnectNetworkManager oldNetwork = network;
        socket = null; network = null;
        if (oldSocket != null) {
            if (announce && oldSocket.isOpen() && roomCode != null)
                oldSocket.send("TC2 " + (hosting ? "STOP " : "LEAVE ") + selfId);
            oldSocket.close();
        }
        for (String id : new ArrayList<>(guests.keySet())) closeGuest(id);
        guests.clear();
        hostId = pending = roomCode = null;
        state = "Idle";
        if (oldNetwork != null) oldNetwork.closeChannel(new TextComponentString(reason));
    }

    private static void fail(String message) { shutdown(message, true); error = message; state = "Disconnected"; }
    public static boolean isHosting() { return socket != null && hosting; }
    public static void host(String ignored, int players) {
        if (!SingleplayerServerController.isWorldReady()) { error = "Open a singleplayer world first"; return; }
        maxPlayers = normalizePlayers(players);
        begin("HOST " + GAME + "," + maxPlayers, true);
    }
    public static void join(String ignored, String code) {
        if (Minecraft.getMinecraft().world != null) { error = "Save and quit your world before joining"; return; }
        if (!code.matches("[A-Fa-f0-9]{6}")) { error = "Enter a six-character room code"; return; }
        begin("JOIN " + GAME + "," + code.toUpperCase(), false);
    }
    public static void matchmaking(String ignored, int players) {
        maxPlayers = normalizePlayers(players);
        begin("MATCHMAKE " + GAME + "," + maxPlayers, SingleplayerServerController.isWorldReady());
    }
    private static int normalizePlayers(int players) { return Math.max(2, Math.min(4, players)); }
    private static void begin(String command, boolean host) {
        reset(); hosting = host; pending = command;
        selfId = EaglercraftUUID.randomUUID().toString().replace("-", "");
        started = EagRuntime.steadyTimeMillis(); heartbeat = hello = 0;
        state = "Connecting";
        socket = PlatformNetworking.openWebSocket(SERVER);
        if (socket == null) { fail("Could not open TailsConnect"); return; }
        socket.setEnableStringFrames(true); socket.setEnableBinaryFrames(true);
    }

    private static void closeGuest(String id) {
        if (guests.remove(id) != null && SingleplayerServerController.isWorldRunning())
            SingleplayerServerController.sendIPCPacket(new IPCPacket0CPlayerChannel(CHANNEL_PREFIX + id, false));
    }
    public static void channelClosed(String channel) {
        if (!channel.startsWith(CHANNEL_PREFIX)) return;
        String id = channel.substring(CHANNEL_PREFIX.length());
        if (guests.remove(id) != null && socket != null && socket.isOpen())
            socket.send("TC2 KICK " + selfId + " " + id);
    }
    public static void forward(String channel, byte[] bytes) {
        if (!channel.startsWith(CHANNEL_PREFIX) || !isHosting()) return;
        String recipient = channel.substring(CHANNEL_PREFIX.length());
        if (guests.containsKey(recipient) && socket.isOpen()) send(recipient, bytes);
    }
    public static void sendGame(byte[] bytes) {
        if (socket != null && socket.isOpen() && hostId != null) send(hostId, bytes);
    }
    private static void send(String recipient, byte[] bytes) {
        socket.send(TailsConnectFrame.encode(selfId, recipient, bytes));
    }

    private static void control(String message, long now) {
        if (message.startsWith("ERROR ")) { fail(message.substring(6)); return; }
        if (message.startsWith("ROOM ") || message.startsWith("JOINED ")) {
            roomCode = message.substring(message.indexOf(' ') + 1);
            started = now;
            state = hosting ? "Sharing world (0/" + (maxPlayers - 1) + " guests)" : "Finding host";
            // The relay can transfer data before its room fills. HELLO establishes each peer
            // independently; heartbeats start only after the READY exchange.
            if (!hosting) { socket.send("TC2 HELLO " + selfId); hello = now; }
            return;
        }
        String[] p = message.split(" ");
        if (p.length < 3 || !p[0].equals("TC2") || !TailsConnectFrame.validId(p[2])) return;
        String id = p[2];
        if (id.equals(selfId)) return;
        if (hosting) {
            if (p[1].equals("HELLO")) {
                if (!guests.containsKey(id)) {
                    if (guests.size() >= maxPlayers - 1) return;
                    guests.put(id, now);
                    SingleplayerServerController.sendIPCPacket(new IPCPacket0CPlayerChannel(CHANNEL_PREFIX + id, true));
                }
                socket.send("TC2 READY " + selfId + " " + id);
                state = "Sharing world (" + guests.size() + "/" + (maxPlayers - 1) + " guests)";
            } else if (p[1].equals("HEARTBEAT") && guests.containsKey(id)) guests.put(id, now);
            else if (p[1].equals("LEAVE")) closeGuest(id);
            else if (p[1].equals("READY")) { fail("Match has multiple hosts; only one player should open a world"); }
        } else {
            if (p[1].equals("READY") && p.length == 4 && p[3].equals(selfId) && network == null) {
                hostId = id; hostHeartbeat = now;
                Minecraft mc = Minecraft.getMinecraft();
                network = new TailsConnectNetworkManager();
                network.setConnectionState(EnumConnectionState.LOGIN);
                network.setNetHandler(new NetHandlerSingleplayerLogin(network, mc, new GuiMainMenu()));
                network.sendPacket(new CPacketLoginStart(mc.getSession().getProfile(), EaglerProfile.getSkinPacket(3), EaglerProfile.getCapePacket(), ConnectionHandshake.getSPHandshakeProtocolData(), EaglercraftVersion.clientBrandUUID));
                state = "Connected to host";
            } else if (id.equals(hostId)) {
                if (p[1].equals("HEARTBEAT")) hostHeartbeat = now;
                else if (p[1].equals("STOP")) fail("Host stopped sharing the world");
                else if (p[1].equals("KICK") && p.length == 4 && p[3].equals(selfId)) fail("Disconnected by host");
            }
        }
    }

    public static void update() {
        if (socket == null) return;
        long now = EagRuntime.steadyTimeMillis();
        if (hosting && !SingleplayerServerController.isWorldRunning()) { fail("Host world closed"); return; }
        if (socket.isClosed() || socket.getState() == EnumEaglerConnectionState.FAILED) { fail("Relay disconnected"); return; }
        if (pending != null) {
            if (socket.isOpen()) { socket.send(pending); pending = null; state = "Waiting for players"; }
            else if (now - started > 15000) { fail("Connection timed out"); return; }
        }
        // Consume in wire order so a final disconnect packet precedes the host's KICK control.
        for (int i = 0; i < 512 && socket != null && socket.availableFrames() > 0; i++) {
            IWebSocketFrame frame = socket.getNextFrame();
            if (frame == null) break;
            if (frame.isString()) control(frame.getString(), now);
            else {
                TailsConnectFrame packet = TailsConnectFrame.decode(frame.getByteArray());
                if (packet == null || !packet.recipient.equals(selfId)) continue;
                if (hosting && guests.containsKey(packet.sender))
                    ClientPlatformSingleplayer.sendPacket(new IPCPacketData(CHANNEL_PREFIX + packet.sender, packet.payload));
                else if (!hosting && network != null && packet.sender.equals(hostId)) network.addRecievedPacket(packet.payload);
            }
        }
        if (socket == null) return;
        if (roomCode != null && !hosting && network == null) {
            if (now - hello >= 2000) { socket.send("TC2 HELLO " + selfId); hello = now; }
            if (now - started > 60000) { fail("No host with an open world was found"); return; }
        }
        if (!guests.isEmpty() || hostId != null) {
            if (now - heartbeat >= 3000) { socket.send("TC2 HEARTBEAT " + selfId); heartbeat = now; }
            for (String id : new ArrayList<>(guests.keySet())) {
                if (now - guests.get(id) > 20000) {
                    socket.send("TC2 KICK " + selfId + " " + id);
                    closeGuest(id);
                }
            }
            if (!hosting && now - hostHeartbeat > 20000) { fail("Host heartbeat timed out"); return; }
        }
        if (hosting && roomCode != null) state = "Sharing world (" + guests.size() + "/" + (maxPlayers - 1) + " guests)";
        if (network != null) {
            try { network.processReceivedPackets(); if (network != null) network.checkDisconnected(); }
            catch (Exception ex) { fail("Game connection failed: " + ex.getMessage()); }
        }
    }
    public static String getState() { return state; }
    public static String getRoomCode() { return roomCode; }
    public static String getError() { return error; }
}
