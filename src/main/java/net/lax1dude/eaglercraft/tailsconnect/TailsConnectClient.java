package net.lax1dude.eaglercraft.tailsconnect;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import net.lax1dude.eaglercraft.*;
import net.lax1dude.eaglercraft.internal.*;
import net.lax1dude.eaglercraft.profile.EaglerProfile;
import net.lax1dude.eaglercraft.crypto.SHA256Digest;
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
    public static final String GAME_ID = "tailscraft-1.12.2-packets-v2";
    private static final String GAME = GAME_ID;
    private static final Map<String, Boolean> guests = new LinkedHashMap<>();
    private static IWebSocketClient socket;
    private static TailsConnectNetworkManager network;
    private static String state = "Idle", roomCode, error, pending, selfId, hostId;
    private static boolean handshakeSent, welcomeReceived;
    private static boolean publicLobby;
    private static org.json.JSONArray searchResults = new org.json.JSONArray();
    public static org.json.JSONArray getSearchResults() { return searchResults; }
    private static boolean hosting;
    private static int maxPlayers = 4;
    private static long started;
    private static final int TRANSFER_CHUNK_SIZE = 900000;
    private static final int MAX_TRANSFER_SIZE = 64 * 1024 * 1024;
    private static boolean transferPreparing;
    private static byte[] outgoingWorld;
    private static int outgoingChunk;
    private static String outgoingWorldName;
    private static String incomingWorldName;
    private static byte[][] incomingChunks;
    private static int incomingChunkCount;
    private static int incomingChunksReceived;
    private static int incomingBytes;
    private static boolean hostedMode;
    private static boolean hostedExportRequested;
    private static boolean hostedUploadReady;
    private static String hostedWorldId;
    private static String hostedUploadHash;

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
        transferPreparing = false;
        outgoingWorld = null;
        outgoingChunk = 0;
        outgoingWorldName = null;
        incomingWorldName = null;
        incomingChunks = null;
        incomingChunkCount = incomingChunksReceived = incomingBytes = 0;
        hostedMode = false;
        hostedExportRequested = hostedUploadReady = false;
        hostedWorldId = null;
        hostedUploadHash = null;
        hostId = pending = roomCode = null;
        state = "Idle";
        if (oldNetwork != null) oldNetwork.closeChannel(new TextComponentString(reason));
    }

    private static void fail(String message) { shutdown(message, true); error = message; state = "Disconnected"; }
    public static boolean isHosting() { return socket != null && hosting; }
    public static void transferWorld() {
        if (!isHosting()) { error = "Host a world before transferring it"; return; }
        if (guests.isEmpty()) { error = "No connected players to receive the world"; return; }
        if (transferPreparing || outgoingWorld != null) { error = "A world transfer is already running"; return; }
        try {
            transferPreparing = true;
            state = "Preparing world transfer";
            SingleplayerServerController.requestWorldExport();
        } catch (Exception ex) {
            transferPreparing = false;
            error = "Could not prepare world: " + ex.getMessage();
        }
    }
    public static void host(String ignored, int players) {
        hostInternal(players, publicLobby);
    }
    public static void hostPrivate(int players) {
        hostInternal(players, false);
    }
    public static void hostHosted(int players) {
        hostInternal(players, publicLobby);
        hostedMode = true;
    }
    private static void hostInternal(int players, boolean advertisePublicly) {
        if (!SingleplayerServerController.isWorldReady()) { error = "Open a singleplayer world first"; return; }
        maxPlayers = normalizePlayers(players);
        JSONObject data = new JSONObject().put("game", GAME).put("maxPlayers", maxPlayers);
        String worldName = SingleplayerServerController.getCurrentWorldName();
        if (worldName == null || worldName.trim().isEmpty()) {
            worldName = "Tailscraft World";
        } else {
            worldName = worldName.trim();
        }
        if (worldName.length() > 64) {
            worldName = worldName.substring(0, 64);
        }
        data.put("advertisement", new JSONObject().put("public", advertisePublicly).put("name", worldName));
        begin("TC3 HOST " + data, true);
    }
    public static void join(String ignored, String code) {
        if (Minecraft.getMinecraft().world != null) { error = "Save and quit your world before joining"; return; }
        if (!code.matches("[A-Fa-f0-9]{6}")) { error = "Enter a six-character room code"; return; }
        begin("TC3 JOIN " + new JSONObject().put("game", GAME).put("room", code.toUpperCase()), false);
    }
    public static void matchmaking(String ignored, int players) {
        maxPlayers = normalizePlayers(players);
        if (maxPlayers == 0) { error = "Matchmaking needs a fixed player count"; return; }
        begin("TC3 MATCHMAKE " + new JSONObject().put("game", GAME).put("players", maxPlayers), SingleplayerServerController.isWorldReady());
    }
    public static void searchWorlds() {
        searchResults = new org.json.JSONArray();
        begin("TC3 SEARCH {\"game\":\"" + GAME + "\",\"limit\":20}", false);
    }
    public static void setPublicLobby(boolean value) { publicLobby = value; }
    public static boolean isPublicLobby() { return publicLobby; }
    private static int normalizePlayers(int players) { return players == 0 ? 0 : Math.max(2, Math.min(4, players)); }
    private static String guestLimitText() { return maxPlayers == 0 ? "unlimited" : Integer.toString(maxPlayers - 1); }
    private static void begin(String command, boolean host) {
        reset(); hosting = host; pending = command; handshakeSent = false; welcomeReceived = false;
        selfId = EaglercraftUUID.randomUUID().toString().replace("-", "");
        started = EagRuntime.steadyTimeMillis();
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

    private static String sha256(byte[] data) throws Exception {
        SHA256Digest sha = new SHA256Digest();
        sha.update(data, 0, data.length);
        byte[] digest = new byte[32];
        sha.doFinal(digest, 0);
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            int unsigned = value & 255;
            if (unsigned < 16) result.append('0');
            result.append(Integer.toHexString(unsigned));
        }
        return result.toString();
    }

    private static byte[] transferChunk(int index, int count, byte[] data, int offset, int length) {
        byte[] packet = new byte[12 + length];
        packet[0] = 'T'; packet[1] = 'C'; packet[2] = '3'; packet[3] = 'W';
        packet[4] = (byte) (index >>> 24); packet[5] = (byte) (index >>> 16);
        packet[6] = (byte) (index >>> 8); packet[7] = (byte) index;
        packet[8] = (byte) (count >>> 24); packet[9] = (byte) (count >>> 16);
        packet[10] = (byte) (count >>> 8); packet[11] = (byte) count;
        System.arraycopy(data, offset, packet, 12, length);
        return packet;
    }

    private static boolean isTransferChunk(byte[] packet) {
        return packet != null && packet.length >= 12 && packet[0] == 'T' && packet[1] == 'C'
                && packet[2] == '3' && packet[3] == 'W';
    }

    private static int readInt(byte[] data, int offset) {
        return ((data[offset] & 255) << 24) | ((data[offset + 1] & 255) << 16)
                | ((data[offset + 2] & 255) << 8) | (data[offset + 3] & 255);
    }

    private static void receiveTransferChunk(byte[] packet) {
        if (incomingChunks == null || packet.length < 12) return;
        int index = readInt(packet, 4);
        int count = readInt(packet, 8);
        if (count != incomingChunkCount || index < 0 || index >= count || incomingChunks[index] != null) return;
        byte[] chunk = new byte[packet.length - 12];
        System.arraycopy(packet, 12, chunk, 0, chunk.length);
        incomingChunks[index] = chunk;
        incomingChunksReceived++;
        incomingBytes += chunk.length;
        state = "Receiving world (" + incomingChunksReceived + "/" + incomingChunkCount + ")";
        if (incomingChunksReceived != incomingChunkCount) return;
        try {
            ByteArrayOutputStream result = new ByteArrayOutputStream(incomingBytes);
            for (byte[] part : incomingChunks) result.write(part);
            SingleplayerServerController.importWorld(incomingWorldName, result.toByteArray());
            state = "World received: " + incomingWorldName;
        } catch (Exception ex) {
            error = "Could not import world: " + ex.getMessage();
        } finally {
            incomingWorldName = null;
            incomingChunks = null;
            incomingChunkCount = incomingChunksReceived = incomingBytes = 0;
        }
    }

    private static void updateWorldTransfer() {
        if (hosting && transferPreparing) {
            byte[] result = SingleplayerServerController.getExportResponse();
            if (result != null) {
                if (result.length == 0 || result.length > MAX_TRANSFER_SIZE) {
                    transferPreparing = false;
                    error = "World is too large to transfer (maximum 64 MB)";
                } else {
                    outgoingWorld = result;
                    outgoingWorldName = SingleplayerServerController.getCurrentWorldName();
                    if (outgoingWorldName == null || outgoingWorldName.trim().isEmpty()) outgoingWorldName = "Shared World";
                    outgoingWorldName = outgoingWorldName.trim();
                    outgoingChunk = 0;
                    transferPreparing = false;
                }
            }
        }
        if (hosting && outgoingWorld != null && socket != null && socket.isOpen() && !guests.isEmpty()) {
            int count = (outgoingWorld.length + TRANSFER_CHUNK_SIZE - 1) / TRANSFER_CHUNK_SIZE;
            if (outgoingChunk == 0) {
                socket.send("TC3 WORLD_OFFER " + new JSONObject().put("name", outgoingWorldName)
                        .put("size", outgoingWorld.length).put("chunks", count));
                state = "Sending world (0/" + count + ")";
            }
            int offset = outgoingChunk * TRANSFER_CHUNK_SIZE;
            int length = Math.min(TRANSFER_CHUNK_SIZE, outgoingWorld.length - offset);
            byte[] packet = transferChunk(outgoingChunk, count, outgoingWorld, offset, length);
            for (String id : new ArrayList<>(guests.keySet())) send(id, packet);
            outgoingChunk++;
            state = "Sending world (" + outgoingChunk + "/" + count + ")";
            if (outgoingChunk >= count) {
                outgoingWorld = null;
                outgoingWorldName = null;
                outgoingChunk = 0;
                state = "World transfer complete";
            }
        }
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
                if (hostedMode && hosting && !hostedExportRequested) {
                    hostedExportRequested = true;
                    SingleplayerServerController.requestWorldExport();
                    state = "Preparing hosted world";
                }
            } catch (Exception ignored) { }
            return;
        }
        if (message.startsWith("TC5 UPLOAD_READY ")) {
            if (!hostedMode || !hosting) return;
            try {
                JSONObject data = new JSONObject(message.substring(17));
                hostedWorldId = data.optString("world", null);
                hostedUploadReady = hostedWorldId != null && outgoingWorld != null;
                outgoingChunk = 0;
                state = "Uploading hosted world (0/" + ((outgoingWorld.length + TRANSFER_CHUNK_SIZE - 1) / TRANSFER_CHUNK_SIZE) + ")";
            } catch (Exception ex) { error = "Invalid hosted-world upload response"; }
            return;
        }
        if (message.startsWith("TC5 STORED ")) {
            if (!hostedMode) return;
            try {
                JSONObject data = new JSONObject(message.substring(12));
                hostedWorldId = data.optString("world", hostedWorldId);
                state = "Hosted world ready: " + hostedWorldId;
            } catch (Exception ignored) { state = "Hosted world ready"; }
            return;
        }
        if (message.startsWith("TC5 ERROR ")) {
            try { error = new JSONObject(message.substring(10)).optString("message", "Hosted-world error"); }
            catch (Exception ignored) { error = "Hosted-world error"; }
            hostedUploadReady = false;
            return;
        }
        if (message.startsWith("TC3 PEER_JOIN ")) {
            try {
                JSONObject data = new JSONObject(message.substring(14));
                String id = data.optString("peerId", "");
                if ("host".equals(data.optString("role"))) {
                    hostId = id;
                    startGuestNetwork();
                } else if (hosting && id.length() > 0 && !guests.containsKey(id)) {
                    guests.put(id, Boolean.TRUE);
                    SingleplayerServerController.sendIPCPacket(new IPCPacket0CPlayerChannel(CHANNEL_PREFIX + id, true));
                }
            } catch (Exception ignored) { }
            return;
        }
        if (message.startsWith("TC3 WORLD_OFFER ")) {
            if (hosting) return;
            try {
                JSONObject data = new JSONObject(message.substring(16));
                int size = data.optInt("size", 0);
                int chunks = data.optInt("chunks", 0);
                if (size <= 0 || size > MAX_TRANSFER_SIZE || chunks <= 0
                        || chunks > (MAX_TRANSFER_SIZE + TRANSFER_CHUNK_SIZE - 1) / TRANSFER_CHUNK_SIZE) {
                    error = "The offered world is too large or invalid";
                    return;
                }
                incomingWorldName = data.optString("name", "Shared World");
                incomingChunkCount = chunks;
                incomingChunks = new byte[chunks][];
                incomingChunksReceived = incomingBytes = 0;
                state = "Receiving world (0/" + chunks + ")";
            } catch (Exception ex) { error = "Invalid world transfer offer"; }
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
            state = hosting ? "Sharing world (0/" + guestLimitText() + " guests)" : "Finding host";
            // The relay can transfer data before its room fills. HELLO establishes each peer
            // independently; TC3 PEER_LEAVE and ROOM_CLOSED handle disconnects.
            if (!hosting) socket.send("TC2 HELLO " + selfId);
            return;
        }
        String[] p = message.split(" ");
        if (p.length < 3 || !p[0].equals("TC2") || !TailsConnectFrame.validId(p[2])) return;
        String id = p[2];
        if (id.equals(selfId)) return;
        if (hosting) {
            if (p[1].equals("HELLO")) {
                if (!guests.containsKey(id)) {
                    if (maxPlayers > 0 && guests.size() >= maxPlayers - 1) return;
                    guests.put(id, Boolean.TRUE);
                    SingleplayerServerController.sendIPCPacket(new IPCPacket0CPlayerChannel(CHANNEL_PREFIX + id, true));
                }
                socket.send("TC3 READY {\"to\":\"" + id + "\"}");
                    state = "Sharing world (" + guests.size() + "/" + guestLimitText() + " guests)";
            }
            else if (p[1].equals("LEAVE")) closeGuest(id);
            else if (p[1].equals("READY")) { fail("Match has multiple hosts; only one player should open a world"); }
        } else {
            if (p[1].equals("READY") && p.length == 4 && p[3].equals(selfId) && network == null) {
                hostId = id;
                Minecraft mc = Minecraft.getMinecraft();
                network = new TailsConnectNetworkManager();
                network.setConnectionState(EnumConnectionState.LOGIN);
                network.setNetHandler(new NetHandlerSingleplayerLogin(network, mc, new GuiMainMenu()));
                network.sendPacket(new CPacketLoginStart(mc.getSession().getProfile(), EaglerProfile.getSkinPacket(3), EaglerProfile.getCapePacket(), ConnectionHandshake.getSPHandshakeProtocolData(), EaglercraftVersion.clientBrandUUID));
                state = "Connected to host";
            } else if (id.equals(hostId)) {
                if (p[1].equals("STOP")) fail("Host stopped sharing the world");
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
                    socket.send("TC3 HELLO " + new JSONObject().put("version", 3)
                            .put("features", new org.json.JSONArray(new String[] { "rooms", "presence", "search", "disconnects", "relay" }))
                            .put("friendCode", TailsConnectFriends.getFriendCode()));
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
                if (!hosting && packet.sender.equals(hostId) && isTransferChunk(packet.payload))
                    receiveTransferChunk(packet.payload);
                else if (hosting && guests.containsKey(packet.sender))
                    ClientPlatformSingleplayer.sendPacket(new IPCPacketData(CHANNEL_PREFIX + packet.sender, packet.payload));
                else if (!hosting && network != null && packet.sender.equals(hostId)) network.addRecievedPacket(packet.payload);
            }
        }
        if (socket == null) return;
        if (roomCode != null && !hosting && network == null) {
            if (now - started > 60000) { fail("No host with an open world was found"); return; }
        }
        if (hosting && roomCode != null) state = "Sharing world (" + guests.size() + "/" + guestLimitText() + " guests)";
        if (hostedMode) updateHostedWorld();
        else updateWorldTransfer();
        if (network != null) {
            try { network.processReceivedPackets(); if (network != null) network.checkDisconnected(); }
            catch (Exception ex) { fail("Game connection failed: " + ex.getMessage()); }
        }
    }
    public static String getState() { return state; }
    public static String getRoomCode() { return roomCode; }
    public static String getError() { return error; }

    private static void updateHostedWorld() {
        if (!hosting || !hostedExportRequested || socket == null || !socket.isOpen()) return;
        if (!hostedUploadReady && outgoingWorld == null) {
            byte[] result = SingleplayerServerController.getExportResponse();
            if (result != null) {
                if (result.length == 0 || result.length > MAX_TRANSFER_SIZE) {
                    error = "World is too large to host (maximum 64 MiB)";
                    hostedExportRequested = false;
                    return;
                }
                try {
                    outgoingWorld = result;
                    outgoingWorldName = SingleplayerServerController.getCurrentWorldName();
                    if (outgoingWorldName == null || outgoingWorldName.trim().isEmpty()) outgoingWorldName = "Hosted World";
                    outgoingWorldName = outgoingWorldName.trim();
                    hostedUploadHash = sha256(result);
                    socket.send("TC5 HOST_WORLD " + new JSONObject().put("game", GAME)
                            .put("name", outgoingWorldName).put("size", result.length).put("sha256", hostedUploadHash));
                    state = "Requesting hosted-world storage";
                } catch (Exception ex) { error = "Could not prepare hosted world: " + ex.getMessage(); }
            }
            return;
        }
        if (!hostedUploadReady || outgoingWorld == null) return;
        int count = (outgoingWorld.length + TRANSFER_CHUNK_SIZE - 1) / TRANSFER_CHUNK_SIZE;
        int offset = outgoingChunk * TRANSFER_CHUNK_SIZE;
        int length = Math.min(TRANSFER_CHUNK_SIZE, outgoingWorld.length - offset);
        socket.send(transferChunk(outgoingChunk, count, outgoingWorld, offset, length));
        outgoingChunk++;
        state = "Uploading hosted world (" + outgoingChunk + "/" + count + ")";
        if (outgoingChunk >= count) {
            outgoingWorld = null;
            outgoingChunk = 0;
            hostedUploadReady = false;
        }
    }
}
