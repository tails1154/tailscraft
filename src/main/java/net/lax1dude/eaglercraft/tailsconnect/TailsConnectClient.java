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
import org.json.JSONObject;

/** One host plus three independently routed guests over the broadcast relay. */
public final class TailsConnectClient {
    public static final String SERVER = "wss://tails1154.com:9842";
    public static final String CHANNEL_PREFIX = "tailsconnect-v3-";
    private static final String GAME = "tailscraft-1.12.2-packets-v2";
    private static final Map<String, Long> guests = new LinkedHashMap<>();
    private static IWebSocketClient socket;
    private static TailsConnectNetworkManager network;
    private static String state = "Idle", roomCode, error, pending, selfId, hostId;
    private static boolean handshakeSent, welcomeReceived;
    private static boolean publicLobby;
    private static org.json.JSONArray searchResults = new org.json.JSONArray();
    public static org.json.JSONArray getSearchResults() { return searchResults; }
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
                oldSocket.send("TC3 " + (hosting ? "STOP" : "LEAVE"));
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
        JSONObject data = new JSONObject().put("game", GAME).put("maxPlayers", maxPlayers);
        data.put("advertisement", new JSONObject().put("public", publicLobby).put("name", "Tailscraft World"));
        begin("TC3 HOST " + data, true);
    }
    public static void join(String ignored, String code) {
        if (Minecraft.getMinecraft().world != null) { error = "Save and quit your world before joining"; return; }
        if (!code.matches("[A-Fa-f0-9]{6}")) { error = "Enter a six-character room code"; return; }
        begin("TC3 JOIN " + new JSONObject().put("game", GAME).put("room", code.toUpperCase()), false);
    }
    public static void matchmaking(String ignored, int players) {
        maxPlayers = normalizePlayers(players);
        begin("TC3 MATCHMAKE " + new JSONObject().put("game", GAME).put("players", maxPlayers), SingleplayerServerController.isWorldReady());
    }
    public static void searchWorlds() {
        searchResults = new org.json.JSONArray();
        begin("TC3 SEARCH {\"game\":\"" + GAME + "\",\"limit\":20}", false);
    }
    public static void setPublicLobby(boolean value) { publicLobby = value; }
    public static boolean isPublicLobby() { return publicLobby; }
    private static int normalizePlayers(int players) { return Math.max(2, Math.min(4, players)); }
    private static void begin(String command, boolean host) {
        reset(); hosting = host; pending = command; handshakeSent = false; welcomeReceived = false;
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
            socket.send("TC3 KICK {\"to\":\"" + id + "\"}");
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

    private static void startGuestNetwork() {
        if (network != null || hostId == null) return;
        Minecraft mc = Minecraft.getMinecraft();
        network = new TailsConnectNetworkManager();
        network.setConnectionState(EnumConnectionState.LOGIN);
        network.setNetHandler(new NetHandlerSingleplayerLogin(network, mc, new GuiMainMenu()));
        network.sendPacket(new CPacketLoginStart(mc.getSession().getProfile(), EaglerProfile.getSkinPacket(3), EaglerProfile.getCapePacket(), ConnectionHandshake.getSPHandshakeProtocolData(), EaglercraftVersion.clientBrandUUID));
        state = "Connected to host";
    }

    private static void control(String message, long now) {
        if (message.startsWith("TC3 WELCOME ")) {
            try {
                String assignedId = new JSONObject(message.substring(12)).optString("peerId", "");
                if (TailsConnectFrame.validId(assignedId)) selfId = assignedId;
                welcomeReceived = true;
            } catch (Exception ignored) { }
            return;
        }
        if (message.startsWith("TC3 ROOM ")) {
            try {
                JSONObject data = new JSONObject(message.substring(9));
                roomCode = data.optString("room", roomCode);
                maxPlayers = data.optInt("maxPlayers", maxPlayers);
                if ("host".equals(data.optString("role"))) hosting = true;
            } catch (Exception ignored) { }
            return;
        }
        if (message.startsWith("TC3 PEER_JOIN ")) {
            try {
                JSONObject data = new JSONObject(message.substring(14));
                String id = data.optString("peerId", "");
                if ("host".equals(data.optString("role"))) {
                    hostId = id;
                    hostHeartbeat = now;
                    startGuestNetwork();
                } else if (hosting && id.length() > 0 && !guests.containsKey(id)) {
                    guests.put(id, now);
                    SingleplayerServerController.sendIPCPacket(new IPCPacket0CPlayerChannel(CHANNEL_PREFIX + id, true));
                }
            } catch (Exception ignored) { }
            return;
        }
        if (message.startsWith("TC3 PEER_LEAVE ")) {
            try {
                JSONObject data = new JSONObject(message.substring(15));
                String id = data.optString("peerId", "");
                if (id.equals(hostId)) fail("Host disconnected: " + data.optString("reason", "connection_closed"));
                else closeGuest(id);
            } catch (Exception ignored) { }
            return;
        }
        if (message.startsWith("TC3 ROOM_CLOSED ")) {
            try { fail("Room closed: " + new JSONObject(message.substring(16)).optString("reason", "closed")); }
            catch (Exception ignored) { fail("Room closed"); }
            return;
        }
        if (message.startsWith("TC3 KICK ")) {
            fail("Disconnected by host");
            return;
        }
        if (message.startsWith("TC3 ADVERTISED ")) return;
        if (message.startsWith("TC3 SEARCH_RESULTS ")) {
            try {
                searchResults = new org.json.JSONArray(message.substring("TC3 SEARCH_RESULTS ".length()));
                state = searchResults.length() == 0 ? "No public worlds found" : "Found worlds: " + searchResults.length();
            }
            catch (Exception ignored) { state = "World search complete"; }
            return;
        }
        if (message.startsWith("TC3 ERROR ")) {
            try { fail(new JSONObject(message.substring(10)).optString("message", "TailsConnect error")); }
            catch (Exception ignored) { fail("TailsConnect error"); }
            return;
        }
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
                socket.send("TC3 READY {\"to\":\"" + id + "\"}");
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
            if (socket.isOpen()) {
                if (!handshakeSent) {
                    socket.send("TC3 HELLO {\"version\":3,\"features\":[\"rooms\",\"presence\",\"search\",\"disconnects\",\"relay\"]}");
                    handshakeSent = true;
                } else if (welcomeReceived) {
                    socket.send(pending);
                    pending = null;
                    state = "Waiting for players";
                }
            }
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
            if (now - hello >= 3000 && hostId == null) { socket.send("TC3 PING"); hello = now; }
            if (now - started > 60000) { fail("No host with an open world was found"); return; }
        }
        if (!guests.isEmpty() || hostId != null) {
            if (now - heartbeat >= 3000) { socket.send("TC3 PING"); heartbeat = now; }
            for (String id : new ArrayList<>(guests.keySet())) {
                if (now - guests.get(id) > 20000) {
                    socket.send("TC3 KICK {\"to\":\"" + id + "\"}");
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
