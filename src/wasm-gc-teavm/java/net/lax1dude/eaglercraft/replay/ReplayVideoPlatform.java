package net.lax1dude.eaglercraft.replay;

import org.teavm.jso.JSBody;

/** Browser bridge for local canvas video capture. Nothing is uploaded. */
public final class ReplayVideoPlatform {
    private ReplayVideoPlatform() {
    }

    @JSBody(params = {}, script = "return !!(window.MediaRecorder && HTMLCanvasElement.prototype.captureStream && document.querySelector('canvas')); ")
    public static native boolean isSupported();

    @JSBody(params = {}, script = ""
        + "if (window.__tailscraftReplayRecorder) return true;"
        + "var canvas = document.querySelector('canvas');"
        + "if (!canvas || !canvas.captureStream || !window.MediaRecorder) return false;"
        + "try {"
        + "var stream = canvas.captureStream(30);"
        + "var mime = '';"
        + "var choices = ['video/webm;codecs=vp9', 'video/webm;codecs=vp8', 'video/webm'];"
        + "for (var i = 0; i < choices.length; ++i) {"
        + "if (MediaRecorder.isTypeSupported(choices[i])) { mime = choices[i]; break; }"
        + "}"
        + "var chunks = [];"
        + "var recorder = new MediaRecorder(stream, mime ? {mimeType: mime} : undefined);"
        + "recorder.ondataavailable = function(e) { if (e.data && e.data.size) chunks.push(e.data); };"
        + "recorder.onstop = function() {"
        + "try {"
        + "var blob = new Blob(chunks, {type: recorder.mimeType || 'video/webm'});"
        + "var url = URL.createObjectURL(blob);"
        + "var link = document.createElement('a');"
        + "link.href = url;"
        + "link.download = 'tailscraft-replay-' + new Date().toISOString().replace(/[:.]/g, '-') + '.webm';"
        + "document.body.appendChild(link); link.click(); link.remove();"
        + "setTimeout(function() { URL.revokeObjectURL(url); }, 1000);"
        + "} finally {"
        + "stream.getTracks().forEach(function(track) { track.stop(); });"
        + "window.__tailscraftReplayRecorder = null;"
        + "}"
        + "};"
        + "recorder.start(1000);"
        + "window.__tailscraftReplayRecorder = recorder;"
        + "return true;"
        + "} catch (e) { window.__tailscraftReplayRecorder = null; return false; }")
    public static native boolean start();

    @JSBody(params = {}, script = "if(window.__tailscraftReplayRecorder) { try { window.__tailscraftReplayRecorder.stop(); } catch(e) {} }")
    public static native void stop();
}
