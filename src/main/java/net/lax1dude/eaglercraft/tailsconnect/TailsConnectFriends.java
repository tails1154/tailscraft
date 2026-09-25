package net.lax1dude.eaglercraft.tailsconnect;

import java.nio.charset.StandardCharsets;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.EaglercraftUUID;
import net.lax1dude.eaglercraft.internal.EnumEaglerConnectionState;
import net.lax1dude.eaglercraft.internal.IWebSocketClient;
import net.lax1dude.eaglercraft.internal.IWebSocketFrame;
import net.lax1dude.eaglercraft.internal.PlatformNetworking;
import net.lax1dude.eaglercraft.profile.EaglerProfile;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.gui.toasts.SystemToast;
import net.minecraft.util.text.TextComponentString;
import org.json.JSONArray;
import org.json.JSONObject;

/** TC4 friend-code and presence client kept separate from TC3 room sockets. */
public final class TailsConnectFriends {
    private static final String STORAGE_KEY = "tc4-friend-code";
    private static final String SERVER = TailsConnectClient.SERVER;
    private static IWebSocketClient socket;
    private static boolean helloSent;
    private static String friendCode;
    private static String state = "Offline";
    private static String error;
    private static JSONArray friends = new JSONArray();
    private static JSONArray requests = new JSONArray();
    private static long lastPresence;
    private static String lastPresenceSignature;
    private static String pendingJoinCode;
    private static String pendingJoinName;
    private static String pendingJoinRoom;
    private static String pendingApprovedRoom;
    private static boolean approvedWorldShutdownRequested;
    private static String pendingApprovalCode;
    private static boolean openJoinDialogNextTick;

    private TailsConnectFriends() { }

    private static String getOrCreateCode() {
        if (friendCode != null) return friendCode;
        byte[] stored = EagRuntime.getStorage(STORAGE_KEY);
        if (stored != null && stored.length > 0) {
            String candidate = new String(stored, StandardCharsets.UTF_8).trim().toUpperCase();
            if (candidate.matches("[A-Z0-9]{8}")) friendCode = candidate;
        }
        if (friendCode == null) {
            friendCode = EaglercraftUUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            EagRuntime.setStorage(STORAGE_KEY, friendCode.getBytes(StandardCharsets.UTF_8));
        }
        return friendCode;
    }

    public static void open() {
        getOrCreateCode();
        if (socket != null && !socket.isClosed() && socket.getState() != EnumEaglerConnectionState.FAILED) return;
        socket = PlatformNetworking.openWebSocket(SERVER);
        helloSent = false;
        error = null;
        state = socket == null ? "Offline" : "Connecting";
        if (socket != null) socket.setEnableStringFrames(true);
    }

    public static void update() {
        if (socket == null) return;
        if (socket.isClosed() || socket.getState() == EnumEaglerConnectionState.FAILED) {
            state = "Offline";
            return;
        }
        if (socket.isOpen() && !helloSent) {
            socket.send("TC4 HELLO " + new JSONObject().put("version", 4)
                    .put("friendCode", getOrCreateCode()).put("name", EaglerProfile.getName())
                    .put("game", TailsConnectClient.GAME_ID).put("room", TailsConnectClient.getRoomCode())
                    .put("joinable", TailsConnectClient.isHosting())
                    .put("activity", TailsConnectClient.getRoomCode() == null ? "Menu" : "Playing"));
            helloSent = true;
            state = "Loading friends";
        }
        if (openJoinDialogNextTick) {
            openJoinDialogNextTick = false;
            showJoinRequestDialog();
        }
        if (pendingApprovedRoom != null) {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.world == null) {
                String room = pendingApprovedRoom;
                pendingApprovedRoom = null;
                approvedWorldShutdownRequested = false;
                TailsConnectClient.join("", room);
            } else if (!approvedWorldShutdownRequested) {
                approvedWorldShutdownRequested = true;
                state = "Leaving current world";
                if (mc.isIntegratedServerRunning()) mc.shutdownIntegratedServer(new GuiMainMenu());
                else mc.loadWorld(null);
            }
        }
        if (pendingApprovalCode != null && TailsConnectClient.isHosting()
                && TailsConnectClient.getRoomCode() != null) {
            send("JOIN_APPROVE", new JSONObject().put("code", pendingApprovalCode)
                    .put("room", TailsConnectClient.getRoomCode()).put("game", TailsConnectClient.GAME_ID));
            SystemToast.func_193657_a(Minecraft.getMinecraft().func_193033_an(),
                    SystemToast.Type.TAILSCONNECT_JOIN,
                    new TextComponentString("Private world ready"),
                    new TextComponentString("Join code sent"));
            pendingApprovalCode = null;
            state = "Private world shared; join code sent";
        }
        updatePresence();
        for (int i = 0; i < 64 && socket.availableFrames() > 0; i++) {
            IWebSocketFrame frame = socket.getNextFrame();
            if (frame != null && frame.isString()) handle(frame.getString());
        }
    }

    private static void handle(String message) {
        if (message.startsWith("TC4 WELCOME ")) {
            state = "Online";
        } else if (message.startsWith("TC4 FRIENDS ")) {
            try {
                JSONObject data = new JSONObject(message.substring(12));
                friends = data.optJSONArray("friends");
                requests = data.optJSONArray("requests");
                if (friends == null) friends = new JSONArray();
                if (requests == null) requests = new JSONArray();
                state = "Online";
            } catch (Exception ex) { error = "Could not read friend list"; }
        } else if (message.startsWith("TC4 FRIEND_REQUEST ")) {
            try {
                JSONObject data = new JSONObject(message.substring(19));
                requestListRefresh();
                SystemToast.func_193657_a(Minecraft.getMinecraft().func_193033_an(),
                        SystemToast.Type.TAILSCONNECT_FRIEND,
                        new TextComponentString("New friend request"),
                        new TextComponentString("Open Friends to accept"));
            } catch (Exception ex) { error = "Could not read friend request"; }
        } else if (message.startsWith("TC4 JOIN_REQUEST ")) {
            try {
                JSONObject data = new JSONObject(message.substring(17));
                pendingJoinCode = data.optString("code", "");
                pendingJoinName = data.optString("name", "A friend");
                pendingJoinRoom = data.optString("room", "").toUpperCase();
                state = "Join request from " + pendingJoinName;
                SystemToast.func_193657_a(Minecraft.getMinecraft().func_193033_an(),
                        SystemToast.Type.TAILSCONNECT_JOIN,
                        new TextComponentString("Join request"),
                        new TextComponentString("Press J"));
                // Defer the screen transition until the next client tick so a
                // menu/world screen cannot overwrite it in the same frame.
                openJoinDialogNextTick = true;
            } catch (Exception ex) { error = "Could not read join request"; }
        } else if (message.startsWith("TC4 FRIEND_STATUS ")) {
            requestListRefresh();
        } else if (message.startsWith("TC4 PRESENCE ")) {
            requestListRefresh();
        } else if (message.startsWith("TC4 RPC ")) {
            requestListRefresh();
        } else if (message.startsWith("TC4 JOIN_APPROVED ")) {
            try {
                JSONObject data = new JSONObject(message.substring(18));
                String room = data.optString("room", "").toUpperCase();
                if (room.matches("[A-F0-9]{6}")) {
                    pendingApprovedRoom = room;
                    approvedWorldShutdownRequested = false;
                    error = null;
                    state = "Join approved - connecting";
                    SystemToast.func_193657_a(Minecraft.getMinecraft().func_193033_an(),
                            SystemToast.Type.TAILSCONNECT_JOIN,
                            new TextComponentString("Join approved"),
                            new TextComponentString("Connecting to friend world"));
                } else {
                    error = "Friend approval did not include a valid room";
                }
            } catch (Exception ex) { error = "Could not join friend world"; }
        } else if (message.startsWith("TC4 JOIN_STATUS ")) {
            try {
                JSONObject data = new JSONObject(message.substring(16));
                state = "approved".equals(data.optString("status", ""))
                        ? "Join request approved" : "Join request sent";
            } catch (Exception ex) { state = "Join request sent"; }
        } else if (message.startsWith("TC4 JOIN_DENIED ")) {
            error = "Join request declined";
        } else if (message.startsWith("TC4 ERROR ")) {
            try { error = new JSONObject(message.substring(10)).optString("message", "TC4 error"); }
            catch (Exception ex) { error = "TC4 error"; }
            SystemToast.func_193657_a(Minecraft.getMinecraft().func_193033_an(),
                    SystemToast.Type.TAILSCONNECT_JOIN,
                    new TextComponentString("TailsConnect error"), new TextComponentString(error));
        }
    }

    private static void requestListRefresh() {
        if (socket != null && socket.isOpen()) socket.send("TC4 LIST {}");
    }

    private static void send(String command, JSONObject data) {
        open();
        if (socket != null && socket.isOpen()) socket.send("TC4 " + command + " " + data);
    }

    private static void updatePresence() {
        if (!helloSent || socket == null || !socket.isOpen()) return;
        long now = EagRuntime.steadyTimeMillis();
        String room = TailsConnectClient.getRoomCode();
        if (room == null) room = "";
        boolean joinable = TailsConnectClient.isHosting();
        String activity = room.isEmpty() ? "Menu" : "Playing";
        String signature = TailsConnectClient.GAME_ID + "|" + room + "|" + joinable + "|" + activity;
        if (signature.equals(lastPresenceSignature) && now - lastPresence < 2000L) return;
        lastPresenceSignature = signature;
        lastPresence = now;
        socket.send("TC4 RPC " + new JSONObject().put("game", TailsConnectClient.GAME_ID)
                .put("room", room).put("joinable", joinable).put("activity", activity));
    }

    public static void addFriend(String code) {
        code = code == null ? "" : code.trim();
        if (code.length() < 1 || code.length() > 24) { error = "Enter a friend's name or code"; return; }
        JSONObject request = new JSONObject();
        if (code.matches("[A-Za-z0-9]{8}")) request.put("code", code.toUpperCase());
        else request.put("name", code);
        send("FRIEND_REQUEST", request);
        error = "Friend request sent";
    }

    public static void acceptFriend(String code) {
        send("FRIEND_ACCEPT", new JSONObject().put("code", code));
    }

    public static void removeFriend(String code) {
        send("FRIEND_REMOVE", new JSONObject().put("code", code));
    }

    public static void requestJoin(String code) {
        send("JOIN_REQUEST", new JSONObject().put("code", code == null ? "" : code.trim().toUpperCase()));
        state = "Join request sent";
    }

    private static boolean showJoinRequestDialog() {
        if (pendingJoinCode == null) return false;
        final String requestCode = pendingJoinCode;
        final String requestName = pendingJoinName == null ? "this player" : pendingJoinName;
        final String requestRoom = pendingJoinRoom;
        final GuiScreen previous = Minecraft.getMinecraft().currentScreen;
        pendingJoinCode = null;
        pendingJoinName = null;
        pendingJoinRoom = null;
        Minecraft.getMinecraft().displayGuiScreen(new GuiYesNo(new GuiYesNoCallback() {
            public void confirmClicked(boolean result, int id) {
                if (result) {
                    String room = requestRoom;
                    if (room == null || room.isEmpty()) room = TailsConnectClient.getRoomCode();
                    if (room == null || room.isEmpty()) {
                        if (SingleplayerServerController.isWorldReady()
                                || Minecraft.getMinecraft().world != null) {
                            pendingApprovalCode = requestCode;
                            TailsConnectClient.hostPrivate(0);
                            SystemToast.func_193657_a(Minecraft.getMinecraft().func_193033_an(),
                                    SystemToast.Type.TAILSCONNECT_JOIN,
                                    new TextComponentString("Starting private sharing"),
                                    new TextComponentString("Preparing a room for your friend"));
                            state = "Starting private world sharing";
                        } else {
                            error = "Open a world before accepting this request";
                            SystemToast.func_193657_a(Minecraft.getMinecraft().func_193033_an(),
                                    SystemToast.Type.TAILSCONNECT_JOIN,
                                    new TextComponentString("Cannot accept join request"),
                                    new TextComponentString("Open a world first"));
                        }
                    } else {
                        send("JOIN_APPROVE", new JSONObject().put("code", requestCode)
                                .put("room", room).put("game", TailsConnectClient.GAME_ID));
                        state = "Join request accepted";
                    }
                } else {
                    send("JOIN_DENY", new JSONObject().put("code", requestCode));
                    state = "Join request declined";
                }
                Minecraft.getMinecraft().displayGuiScreen(previous);
            }
        }, "Join request", "Would you like to allow " + requestName + " to join?", "Allow", "Deny", 0));
        return true;
    }

    public static boolean handleKey(int keyCode) {
        // Eagler's legacy table uses 0x24, while browser/GLFW events can arrive
        // as the native J key value (74).
        if ((keyCode != net.lax1dude.eaglercraft.KeyboardConstants.KEY_J && keyCode != 74)
                || pendingJoinCode == null) return false;
        return showJoinRequestDialog();
    }

    public static String getFriendCode() { return getOrCreateCode(); }
    public static String getState() { return state; }
    public static String getError() { return error; }
    public static JSONArray getFriends() { return friends; }
    public static JSONArray getRequests() { return requests; }
}
