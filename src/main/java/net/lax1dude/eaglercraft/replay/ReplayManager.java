package net.lax1dude.eaglercraft.replay;

import net.minecraft.client.Minecraft;
import net.lax1dude.eaglercraft.internal.PlatformAudio;
import org.teavm.jso.webaudio.MediaStream;

	/** Local browser video capture. It never sends replay data to a server. */
public final class ReplayManager {
	private static boolean recording;

	private ReplayManager() {
	}

	public static boolean isRecording() {
		return recording;
	}

	public static boolean isPlaying() {
		return false;
	}

	public static int getFrameCount() {
		return 0;
	}

	public static boolean canUse(Minecraft mc) {
		return mc != null && mc.world != null && mc.player != null;
	}

	public static void startRecording(Minecraft mc) {
		if (!canUse(mc)) {
			return;
		}
		if (ReplayVideoPlatform.isSupported()) {
			MediaStream audioStream = null;
			try {
				audioStream = PlatformAudio.beginReplayRecording();
			} catch (Throwable ignored) {
				// Video capture remains available when the browser audio context is unavailable.
			}
			recording = ReplayVideoPlatform.start(audioStream);
			if (!recording) {
				PlatformAudio.endReplayRecording();
			}
		}
	}

	public static void stopRecording() {
		if (recording) {
			ReplayVideoPlatform.stop();
			PlatformAudio.endReplayRecording();
		}
		recording = false;
	}

	/** Called once per client tick. */
	public static void tick(Minecraft mc) {
		if (!recording) {
			return;
		}
		if (!canUse(mc)) {
			stopRecording();
		}
	}
}
