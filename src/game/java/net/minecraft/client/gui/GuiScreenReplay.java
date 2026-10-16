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
		buttonList.add(new GuiButton(1, left, height / 2 - 18, 200, 20,
				ReplayManager.isRecording() ? "Stop & Save Video" : "Start Video Recording"));
		buttonList.add(new GuiButton(0, left, height / 2 + 18, 200, 20, "Back"));
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
		} else if (button.id == 0) {
			mc.displayGuiScreen(parent);
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "Replay Mode", width / 2, height / 2 - 58, 0xFFFFFF);
		String status = ReplayManager.isRecording() ? "Recording game video..."
				: "Saves a local .webm video when stopped";
		drawCenteredString(fontRendererObj, status, width / 2, height / 2 - 40, 0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
