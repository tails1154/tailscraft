package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.ai.AICopilotController;

/** World-visible copilot status display. It blocks player input except for Stop. */
public class GuiScreenAICopilotOverlay extends GuiScreen {
	public boolean doesGuiPauseGame() {
		return false;
	}

	public void initGui() {
		buttonList.clear();
		buttonList.add(new GuiButton(0, width - 108, 36, 96, 20, "Stop Copilot"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) {
			net.minecraft.client.ai.AICopilotAgent.stop();
			AICopilotController.stop();
			mc.displayGuiScreen((GuiScreen) null);
		}
	}

	public void updateScreen() {
		super.updateScreen();
		if (!net.minecraft.client.ai.AICopilotAgent.isActive() && !AICopilotController.isActive()) mc.displayGuiScreen((GuiScreen) null);
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		int left = width - 230;
		drawRect(left, 8, width - 8, 76, 0xAA000000);
		drawString(fontRendererObj, "AI Copilot", left + 8, 14, 0xFFFFFF);
		String status = net.minecraft.client.ai.AICopilotAgent.getStatus();
		drawString(fontRendererObj, fontRendererObj.trimStringToWidth(status, 210), left + 8, 25, 0x55FF55);
		drawString(fontRendererObj, "Cycle " + net.minecraft.client.ai.AICopilotAgent.getCompletedSteps() + "/" + net.minecraft.client.ai.AICopilotAgent.getMaxSteps(), left + 8, 36, 0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		// Keep movement and inventory controls blocked while the copilot is active.
	}
}
