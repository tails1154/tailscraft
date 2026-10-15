package net.minecraft.client.voice;

/** Per-connection voice consent and local controls. Disabled by default. */
public final class VoiceConsentState {
    private static boolean optedIn;
    private static boolean muted = true;
    private static boolean deafened;
    private static boolean promptShown;
    private static String error;
    private VoiceConsentState() {}
    public static void beginConnection() { optedIn = false; muted = true; deafened = false; promptShown = false; error = null; }
    public static void reset() { beginConnection(); }
    public static boolean shouldPrompt() { return !promptShown; }
    public static void markPromptShown() { promptShown = true; }
    public static boolean isOptedIn() { return optedIn; }
    public static void optIn() { optedIn = true; muted = false; }
    public static void optOut() { optedIn = false; muted = true; }
    public static boolean isMuted() { return muted; }
    public static void setMuted(boolean value) { muted = value; }
    public static boolean isDeafened() { return deafened; }
    public static void setDeafened(boolean value) { deafened = value; }
    public static void setError(String value) { error = value; }
    public static String getError() { return error; }
}
