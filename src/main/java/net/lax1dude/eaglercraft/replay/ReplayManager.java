package net.lax1dude.eaglercraft.replay;

import net.minecraft.client.Minecraft;

/** Local, singleplayer-only browser video capture. It never sends replay data to a server. */
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
		return mc != null && mc.isSingleplayer() && mc.world != null && mc.player != null;
	}

	public static void startRecording(Minecraft mc) {
		if (!canUse(mc)) {
			return;
		}
		if (ReplayVideoPlatform.isSupported()) {
			recording = ReplayVideoPlatform.start();
		}
	}

	public static void stopRecording() {
		if (recording) {
			ReplayVideoPlatform.stop();
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
