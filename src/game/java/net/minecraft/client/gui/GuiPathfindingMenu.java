package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.baritone.BaritoneController;

/** GUI-only controls for the local TailsCraft Pathfindinganator 9000. */
public class GuiPathfindingMenu extends GuiScreen {
	private GuiTextField coordinates;
	private GuiTextField block;

	public boolean doesGuiPauseGame() { return true; }

	public void initGui() {
		buttonList.clear();
		coordinates = new GuiTextField(10, fontRendererObj, width / 2 - 100, height / 2 - 48, 200, 20);
		coordinates.setMaxStringLength(64);
		coordinates.setText("100 64 100");
		block = new GuiTextField(11, fontRendererObj, width / 2 - 100, height / 2 - 4, 200, 20);
		block.setMaxStringLength(64);
		block.setText("dirt");
		buttonList.add(new GuiButton(1, width / 2 - 100, height / 2 + 24, 98, 20, "Go to coordinates"));
		buttonList.add(new GuiButton(2, width / 2 + 2, height / 2 + 24, 98, 20, "Mine block"));
		buttonList.add(new GuiButton(3, width / 2 - 100, height / 2 + 50, 98, 20, "Break: " + (BaritoneController.isAllowBreak() ? "ON" : "OFF")));
		buttonList.add(new GuiButton(4, width / 2 + 2, height / 2 + 50, 98, 20, "Place: " + (BaritoneController.isAllowPlace() ? "ON" : "OFF")));
		buttonList.add(new GuiButton(5, width / 2 - 100, height / 2 + 76, 98, 20, "Stop"));
		buttonList.add(new GuiButton(6, width / 2 + 2, height / 2 + 76, 98, 20, "Done"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 1) {
			BaritoneController.handleCommand("#goto " + coordinates.getText());
			mc.displayGuiScreen(null);
		} else if (button.id == 2) {
			BaritoneController.handleCommand("#mine " + block.getText());
			mc.displayGuiScreen(null);
		} else if (button.id == 3) {
			BaritoneController.setAllowBreak(!BaritoneController.isAllowBreak());
			initGui();
		} else if (button.id == 4) {
			BaritoneController.setAllowPlace(!BaritoneController.isAllowPlace());
			initGui();
		} else if (button.id == 5) {
			BaritoneController.handleCommand("#stop");
			mc.displayGuiScreen(null);
		} else if (button.id == 6) {
			mc.displayGuiScreen(null);
		}
	}

	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (!coordinates.textboxKeyTyped(typedChar, keyCode) && !block.textboxKeyTyped(typedChar, keyCode)) super.keyTyped(typedChar, keyCode);
	}

	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		coordinates.mouseClicked(mouseX, mouseY, mouseButton);
		block.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	public void updateScreen() {
		coordinates.updateCursorCounter();
		block.updateCursorCounter();
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "TailsCraft Pathfindinganator 9000", width / 2, height / 2 - 92, 0xFFFFFF);
		drawString(fontRendererObj, "Coordinates (x y z or ~ offsets)", width / 2 - 100, height / 2 - 62, 0xAAAAAA);
		drawString(fontRendererObj, "Block name", width / 2 - 100, height / 2 - 18, 0xAAAAAA);
		coordinates.drawTextBox();
		block.drawTextBox();
		drawString(fontRendererObj, "Status: " + BaritoneController.getStatus(), width / 2 - 100, height / 2 + 104, 0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
