package net.lax1dude.eaglercraft.replay;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

/** Local, singleplayer-only camera replay capture. It never sends replay data to a server. */
public final class ReplayManager {
	private static final int MAX_FRAMES = 20 * 60 * 15;
	private static final List<Frame> frames = new ArrayList<Frame>();
	private static boolean recording;
	private static boolean playing;
	private static int playbackIndex;

	private ReplayManager() {
	}

	public static boolean isRecording() {
		return recording;
	}

	public static boolean isPlaying() {
		return playing;
	}

	public static int getFrameCount() {
		return frames.size();
	}

	public static boolean canUse(Minecraft mc) {
		return mc != null && mc.isSingleplayer() && mc.world != null && mc.player != null;
	}

	public static void startRecording(Minecraft mc) {
		if (!canUse(mc)) {
			return;
		}
		frames.clear();
		playbackIndex = 0;
		playing = false;
		recording = true;
	}

	public static void stopRecording() {
		recording = false;
	}

	public static boolean startPlayback(Minecraft mc) {
		if (!canUse(mc) || frames.isEmpty()) {
			return false;
		}
		recording = false;
		playing = true;
		playbackIndex = 0;
		return true;
	}

	public static void stopPlayback() {
		playing = false;
		playbackIndex = 0;
	}

	/** Called once per client tick. */
	public static void tick(Minecraft mc) {
		if (!recording && !playing) {
			return;
		}
		if (!canUse(mc)) {
			recording = false;
			playing = false;
			return;
		}
		if (recording) {
			if (frames.size() >= MAX_FRAMES) {
				recording = false;
				return;
			}
			frames.add(Frame.capture(mc));
		} else if (playbackIndex < frames.size()) {
			frames.get(playbackIndex++).apply(mc);
		} else {
			playing = false;
			playbackIndex = 0;
		}
	}

	private static final class Frame {
		private final double x;
		private final double y;
		private final double z;
		private final float yaw;
		private final float pitch;

		private Frame(double x, double y, double z, float yaw, float pitch) {
			this.x = x;
			this.y = y;
			this.z = z;
			this.yaw = yaw;
			this.pitch = pitch;
		}

		private static Frame capture(Minecraft mc) {
			return new Frame(mc.player.posX, mc.player.posY, mc.player.posZ, mc.player.rotationYaw,
					mc.player.rotationPitch);
		}

		private void apply(Minecraft mc) {
			mc.player.setPositionAndRotation(x, y, z, yaw, pitch);
			mc.player.prevPosX = x;
			mc.player.prevPosY = y;
			mc.player.prevPosZ = z;
			mc.player.prevRotationYaw = yaw;
			mc.player.prevRotationPitch = pitch;
		}
	}
}
