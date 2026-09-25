package net.lax1dude.eaglercraft.tailsconnect;

import java.nio.charset.StandardCharsets;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.EaglercraftUUID;
import net.lax1dude.eaglercraft.internal.EnumEaglerConnectionState;
import net.lax1dude.eaglercraft.internal.IWebSocketClient;
import net.lax1dude.eaglercraft.internal.IWebSocketFrame;
import net.lax1dude.eaglercraft.internal.PlatformNetworking;
import net.lax1dude.eaglercraft.profile.EaglerProfile;
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
                    .put("friendCode", getOrCreateCode()).put("name", EaglerProfile.getName()));
            helloSent = true;
            state = "Loading friends";
        }
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
            state = "New friend request";
        } else if (message.startsWith("TC4 FRIEND_STATUS ")) {
            requestListRefresh();
        } else if (message.startsWith("TC4 PRESENCE ")) {
            requestListRefresh();
        } else if (message.startsWith("TC4 ERROR ")) {
            try { error = new JSONObject(message.substring(10)).optString("message", "TC4 error"); }
            catch (Exception ex) { error = "TC4 error"; }
        }
    }

    private static void requestListRefresh() {
        if (socket != null && socket.isOpen()) socket.send("TC4 LIST {}");
    }

    private static void send(String command, JSONObject data) {
        open();
        if (socket != null && socket.isOpen()) socket.send("TC4 " + command + " " + data);
    }

    public static void addFriend(String code) {
        code = code == null ? "" : code.trim().toUpperCase();
        if (!code.matches("[A-Z0-9]{8}")) { error = "Enter an 8-character friend code"; return; }
        send("FRIEND_REQUEST", new JSONObject().put("code", code));
        error = "Friend request sent";
    }

    public static void acceptFriend(String code) {
        send("FRIEND_ACCEPT", new JSONObject().put("code", code));
    }

    public static void removeFriend(String code) {
        send("FRIEND_REMOVE", new JSONObject().put("code", code));
    }

    public static String getFriendCode() { return getOrCreateCode(); }
    public static String getState() { return state; }
    public static String getError() { return error; }
    public static JSONArray getFriends() { return friends; }
    public static JSONArray getRequests() { return requests; }
}
