package net.minecraft.client.gui;

import java.io.IOException;

import net.lax1dude.eaglercraft.replay.ReplayManager;

/** Controls for the local singleplayer replay recorder and player. */
public class GuiScreenReplay extends GuiScreen {
	private final GuiScreen parent;

	public GuiScreenReplay(GuiScreen parent) {
		this.parent = parent;
	}

	public boolean doesGuiPauseGame() {
		return true;
	}

	public void initGui() {
		buttonList.clear();
		int left = width / 2 - 100;
		buttonList.add(new GuiButton(1, left, height / 2 - 42, 200, 20,
				ReplayManager.isRecording() ? "Stop Recording" : "Start Recording"));
		GuiButton play = new GuiButton(2, left, height / 2 - 18, 200, 20, "Play Last Replay");
		play.enabled = !ReplayManager.isRecording() && ReplayManager.getFrameCount() > 0;
		buttonList.add(play);
		GuiButton stop = new GuiButton(3, left, height / 2 + 6, 200, 20, "Stop Playback");
		stop.enabled = ReplayManager.isPlaying();
		buttonList.add(stop);
		buttonList.add(new GuiButton(0, left, height / 2 + 42, 200, 20, "Back"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 1) {
			if (ReplayManager.isRecording()) {
				ReplayManager.stopRecording();
				initGui();
			} else {
				ReplayManager.startRecording(mc);
				mc.displayGuiScreen(null);
				mc.setIngameFocus();
			}
		} else if (button.id == 2 && ReplayManager.startPlayback(mc)) {
			mc.displayGuiScreen(null);
			mc.setIngameFocus();
		} else if (button.id == 3) {
			ReplayManager.stopPlayback();
			initGui();
		} else if (button.id == 0) {
			mc.displayGuiScreen(parent);
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "Replay Mode", width / 2, height / 2 - 82, 0xFFFFFF);
		String status = ReplayManager.isRecording() ? "Recording singleplayer camera..."
				: ReplayManager.isPlaying() ? "Playing replay..." : "Frames captured: " + ReplayManager.getFrameCount();
		drawCenteredString(fontRendererObj, status, width / 2, height / 2 - 64, 0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
