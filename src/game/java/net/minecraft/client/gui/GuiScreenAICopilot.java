package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.ai.AICopilotAgent;
import net.minecraft.client.ai.AICopilotController;

/** Simple natural-language entry point for the bounded AI copilot agent. */
public class GuiScreenAICopilot extends GuiScreen {
	private final GuiScreen parent;
	private GuiTextField commandField;
	private String status = "Stopped";

	public GuiScreenAICopilot(GuiScreen parent) { this.parent = parent; }

	public boolean doesGuiPauseGame() { return false; }

	public void initGui() {
		buttonList.clear();
		int left = width / 2 - 150;
		commandField = new GuiTextField(7, fontRendererObj, left, 92, 300, 20);
		commandField.setMaxStringLength(180);
		buttonList.add(new GuiButton(7, left, 126, 300, 20, "Start Copilot"));
		buttonList.add(new GuiButton(4, left, 150, 300, 20, "Stop Copilot"));
		buttonList.add(new GuiButton(6, width / 2 - 100, height - 28, 200, 20, "Back"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 7) {
			String request = commandField.getText().trim();
			if (request.length() == 0) { status = "Describe a task, such as: mine diamonds"; return; }
			status = "Observing and planning...";
			AICopilotAgent.start(request, this::context);
			mc.displayGuiScreen(new GuiScreenAICopilotOverlay());
		} else if (button.id == 4) {
			AICopilotAgent.stop(); AICopilotController.stop(); status = "Stopped";
		} else if (button.id == 6) {
			AICopilotAgent.stop(); AICopilotController.stop(); mc.displayGuiScreen(parent);
		}
	}

	public void updateScreen() {
		super.updateScreen();
		boolean locked = AICopilotAgent.isActive() || AICopilotController.isActive();
		commandField.setEnabled(!locked);
		for (GuiButton button : buttonList) button.enabled = !locked || button.id == 4 || button.id == 6;
	}

	private String context() {
		if (mc.player == null) return "no world loaded";
		StringBuilder items = new StringBuilder();
		for (int i = 0; i < mc.player.inventory.mainInventory.size(); ++i) {
			net.minecraft.item.ItemStack stack = mc.player.inventory.mainInventory.get(i);
			if (stack != null && !stack.func_190926_b()) {
				if (items.length() > 0) items.append(", ");
				items.append(i < 9 ? "hotbar:" : "inventory:").append(stack.getDisplayName()).append(" x").append(stack.func_190916_E());
			}
		}
		return "player position=" + (int) mc.player.posX + "," + (int) mc.player.posY + "," + (int) mc.player.posZ
				+ " dimension=" + mc.player.dimension + " items=" + items.toString();
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "AI Copilot", width / 2, 28, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "Describe what you want it to do", width / 2, 52, 0xAAAAAA);
		drawString(fontRendererObj, "Task", width / 2 - 150, 80, 0xFFFFFF);
		commandField.drawTextBox();
		if (commandField.getText().isEmpty() && !commandField.isFocused()) drawString(fontRendererObj, "Example: find diamonds and mine them", commandField.xPosition + 4, commandField.yPosition + 6, 0x777777);
		drawCenteredString(fontRendererObj, status, width / 2, 184, 0x55FF55);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	protected void keyTyped(char c, int keyCode) throws IOException {
		if (keyCode == 1) {
			AICopilotAgent.stop(); AICopilotController.stop(); mc.displayGuiScreen(parent); return;
		}
		commandField.textboxKeyTyped(c, keyCode);
	}

	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		commandField.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
