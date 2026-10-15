package net.minecraft.client.voice;

import org.teavm.jso.JSBody;

/** Small WASM-GC bridge to the browser microphone/WebRTC implementation. */
public final class VoicePlatform {
    private VoicePlatform() {}
    @JSBody(params = {}, script = "return window['TailsCraftVoice'] && window['TailsCraftVoice']['poll'] ? window['TailsCraftVoice']['poll']() : '[]';")
    public static native String poll();
    @JSBody(params = {}, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['start']();")
    public static native void start();
    @JSBody(params = {}, script = "if(window['TailsCraftVoice'] && typeof window['TailsCraftVoice']['stop'] === 'function') { try { window['TailsCraftVoice']['stop'](); } catch(e) {} }")
    public static native void stop();
    @JSBody(params = { "id", "offer" }, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['addPeer'](id, offer);")
    public static native void addPeer(String id, boolean offer);
    @JSBody(params = { "id" }, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['removePeer'](id);")
    public static native void removePeer(String id);
    @JSBody(params = { "id", "signalType", "data" }, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['signal'](id, signalType, data);")
    public static native void signal(String id, int signalType, String data);
    @JSBody(params = { "muted" }, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['setMuted'](muted);")
    public static native void setMuted(boolean muted);
    @JSBody(params = { "deafened" }, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['setDeafened'](deafened);")
    public static native void setDeafened(boolean deafened);
    @JSBody(params = { "id", "muted" }, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['setPeerMuted'](id, muted);")
    public static native void setPeerMuted(String id, boolean muted);
    @JSBody(params = { "id", "x", "y", "z" }, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['setPeerPosition'](id, x, y, z);")
    public static native void setPeerPosition(String id, double x, double y, double z);
    @JSBody(params = { "x", "y", "z" }, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['setLocalPosition'](x, y, z);")
    public static native void setLocalPosition(double x, double y, double z);
    @JSBody(params = {}, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['resumeAudio']();")
    public static native void resumeAudio();
    @JSBody(params = {}, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['startRelay']();")
    public static native void startRelay();
    @JSBody(params = {}, script = "if(window['TailsCraftVoice'] && typeof window['TailsCraftVoice']['stopRelay'] === 'function') { try { window['TailsCraftVoice']['stopRelay'](); } catch(e) {} }")
    public static native void stopRelay();
    @JSBody(params = { "id", "base64" }, script = "if(window['TailsCraftVoice']) window['TailsCraftVoice']['receiveRelayAudio'](id, base64);")
    public static native void receiveRelayAudio(String id, String base64);
    @JSBody(params = {}, script = "return window.location.protocol === 'https:';")
    public static native boolean isSecureContext();
    @JSBody(params = {}, script = "window.location.href = window.location.href.replace(/^http:/, 'https:');")
    public static native void openSecurePage();
}
