package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.baritone.BaritoneController;

/** GUI-only tabbed controls for the local TailsCraft Pathfindinganator 9000. */
public class GuiPathfindingMenu extends GuiScreen {
	private static final int TAB_NAVIGATION = 0;
	private static final int TAB_MINING = 1;
	private static final int TAB_SETTINGS = 2;

	private int activeTab = TAB_NAVIGATION;
	private GuiTextField coordinates;
	private GuiTextField block;

	public boolean doesGuiPauseGame() { return true; }

	public void initGui() {
		buttonList.clear();
		int left = width / 2 - 150;
		int top = height / 2 - 88;
		buttonList.add(new GuiTab(100, left, top, 100, "Navigation", activeTab == TAB_NAVIGATION));
		buttonList.add(new GuiTab(101, left + 100, top, 100, "Mining", activeTab == TAB_MINING));
		buttonList.add(new GuiTab(102, left + 200, top, 100, "Settings", activeTab == TAB_SETTINGS));

		coordinates = null;
		block = null;
		int panelTop = top + 34;
		if (activeTab == TAB_NAVIGATION) {
			coordinates = new GuiTextField(10, fontRendererObj, left + 20, panelTop + 24, 260, 20);
			coordinates.setMaxStringLength(64);
			coordinates.setText("100 64 100");
			buttonList.add(new GuiButton(1, left + 20, panelTop + 58, 260, 20, "Go to coordinates"));
		} else if (activeTab == TAB_MINING) {
			block = new GuiTextField(11, fontRendererObj, left + 20, panelTop + 24, 260, 20);
			block.setMaxStringLength(64);
			block.setText("dirt");
			buttonList.add(new GuiButton(2, left + 20, panelTop + 58, 260, 20, "Mine block"));
		} else {
			buttonList.add(new GuiButton(3, left + 20, panelTop + 24, 125, 20,
					"Break: " + (BaritoneController.isAllowBreak() ? "ON" : "OFF")));
			buttonList.add(new GuiButton(4, left + 155, panelTop + 24, 125, 20,
					"Place: " + (BaritoneController.isAllowPlace() ? "ON" : "OFF")));
		}

		buttonList.add(new GuiButton(5, left + 20, height / 2 + 56, 125, 20, "Stop"));
		buttonList.add(new GuiButton(6, left + 155, height / 2 + 56, 125, 20, "Done"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id >= 100 && button.id <= 102) {
			activeTab = button.id - 100;
			initGui();
			return;
		}
		if (button.id == 1 && coordinates != null) {
			BaritoneController.handleCommand("#goto " + coordinates.getText());
			mc.displayGuiScreen(null);
		} else if (button.id == 2 && block != null) {
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
		boolean handled = coordinates != null && coordinates.textboxKeyTyped(typedChar, keyCode);
		handled = (block != null && block.textboxKeyTyped(typedChar, keyCode)) || handled;
		if (!handled) super.keyTyped(typedChar, keyCode);
	}

	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		if (coordinates != null) coordinates.mouseClicked(mouseX, mouseY, mouseButton);
		if (block != null) block.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	public void updateScreen() {
		if (coordinates != null) coordinates.updateCursorCounter();
		if (block != null) block.updateCursorCounter();
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		int left = width / 2 - 150;
		int top = height / 2 - 88;
		drawCenteredString(fontRendererObj, "TailsCraft Pathfindinganator 9000", width / 2, top - 22, 0xFFFFFF);
		if (activeTab == TAB_NAVIGATION) {
			drawString(fontRendererObj, "Coordinates (x y z or ~ offsets)", left + 20, top + 44, 0xAAAAAA);
			if (coordinates != null) coordinates.drawTextBox();
		} else if (activeTab == TAB_MINING) {
			drawString(fontRendererObj, "Block name or ID", left + 20, top + 44, 0xAAAAAA);
			if (block != null) block.drawTextBox();
		} else {
			drawString(fontRendererObj, "Movement permissions", left + 20, top + 44, 0xAAAAAA);
			drawString(fontRendererObj, "Allow breaking obstacles and placing support blocks.", left + 20, top + 70, 0x888888);
		}
		drawString(fontRendererObj, "Status: " + BaritoneController.getStatus(), left + 20, height / 2 + 36, 0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
