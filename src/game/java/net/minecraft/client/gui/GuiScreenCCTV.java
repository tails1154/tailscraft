package net.minecraft.client.gui;

import java.io.IOException;

import net.lax1dude.eaglercraft.Keyboard;

/** Server-side CCTV command launcher. Commands are sent only after the user confirms. */
public class GuiScreenCCTV extends GuiScreen {
	private final GuiScreen parent;
	private int inputMode;
	private GuiTextField firstField;
	private GuiTextField secondField;

	public GuiScreenCCTV(GuiScreen parent) { this.parent = parent; }

	public void initGui() {
		Keyboard.enableRepeatEvents(true);
		this.buttonList.clear();
		if (this.inputMode == 0) {
			int left = this.width / 2 - 152;
			int top = 42;
			this.buttonList.add(new GuiButton(0, left, top, 148, 20, "Create camera"));
			this.buttonList.add(new GuiButton(1, left + 156, top, 148, 20, "Create group"));
			this.buttonList.add(new GuiButton(2, left, top + 26, 148, 20, "Add camera to group"));
			this.buttonList.add(new GuiButton(3, left + 156, top + 26, 148, 20, "Create CCTV computer"));
			this.buttonList.add(new GuiButton(4, left, top + 52, 148, 20, "Set computer group"));
			this.buttonList.add(new GuiButton(5, left + 156, top + 52, 148, 20, "List cameras"));
			this.buttonList.add(new GuiButton(6, left, top + 78, 148, 20, "List groups"));
			this.buttonList.add(new GuiButton(7, left + 156, top + 78, 148, 20, "List computers"));
			this.buttonList.add(new GuiButton(8, left, top + 104, 148, 20, "View camera"));
			this.buttonList.add(new GuiButton(9, left + 156, top + 104, 148, 20, "Return from camera"));
			this.buttonList.add(new GuiButton(10, left, top + 130, 304, 20, "Get camera ID"));
			this.buttonList.add(new GuiButton(11, left, top + 162, 304, 20, "Back"));
		} else {
			int left = this.width / 2 - 100;
			int top = 62;
			this.firstField = new GuiTextField(0, this.fontRendererObj, left, top + 22, 200, 20);
			this.firstField.setMaxStringLength(64);
			this.firstField.setFocused(true);
			if (this.inputMode == 1) {
				this.secondField = new GuiTextField(1, this.fontRendererObj, left, top + 66, 200, 20);
				this.secondField.setMaxStringLength(64);
			}
			this.buttonList.add(new GuiButton(100, left, top + (this.inputMode == 1 ? 104 : 60), 98, 20, "Run command"));
			this.buttonList.add(new GuiButton(101, left + 102, top + (this.inputMode == 1 ? 104 : 60), 98, 20, "Cancel"));
		}
	}

	public void onGuiClosed() { Keyboard.enableRepeatEvents(false); }

	private void runCommand(String command) {
		if (this.mc.player != null) this.mc.player.sendChatMessage(command);
		this.mc.displayGuiScreen((GuiScreen) null);
		this.mc.setIngameFocus();
	}

	private void showInput(int mode) { this.inputMode = mode; this.initGui(); }

	protected void actionPerformed(GuiButton button) throws IOException {
		if (this.inputMode != 0) {
			if (button.id == 101) { this.inputMode = 0; this.initGui(); }
			else if (button.id == 100) {
				String first = this.firstField.getText().trim();
				if (first.isEmpty()) return;
				if (this.inputMode == 1) {
					String second = this.secondField.getText().trim();
					if (!second.isEmpty()) this.runCommand("/cctv group addcamera " + first + " " + second);
				} else if (this.inputMode == 2) this.runCommand("/cctv computer setgroup " + first);
				else if (this.inputMode == 3) this.runCommand("/cctv camera view " + first);
			}
			return;
		}
		switch (button.id) {
		case 0: this.runCommand("/cctv camera create"); break;
		case 1: this.runCommand("/cctv group create"); break;
		case 2: this.showInput(1); break;
		case 3: this.runCommand("/cctv computer create"); break;
		case 4: this.showInput(2); break;
		case 5: this.runCommand("/cctv camera list"); break;
		case 6: this.runCommand("/cctv group list"); break;
		case 7: this.runCommand("/cctv computer list"); break;
		case 8: this.showInput(3); break;
		case 9: this.runCommand("/cctv camera return"); break;
		case 10: this.runCommand("/cctv camera get"); break;
		case 11: this.mc.displayGuiScreen(this.parent); break;
		default: break;
		}
	}

	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1) { this.mc.displayGuiScreen(this.inputMode == 0 ? this.parent : new GuiScreenCCTV(this.parent)); return; }
		if (this.inputMode != 0) {
			if (this.firstField.textboxKeyTyped(typedChar, keyCode)) return;
			if (this.secondField != null) this.secondField.textboxKeyTyped(typedChar, keyCode);
			if (keyCode == 28 || keyCode == 156) this.actionPerformed(this.buttonList.get(0));
		}
	}

	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (this.inputMode != 0) { this.firstField.mouseClicked(mouseX, mouseY, mouseButton); if (this.secondField != null) this.secondField.mouseClicked(mouseX, mouseY, mouseButton); }
	}

	public void updateScreen() { if (this.firstField != null) this.firstField.updateCursorCounter(); if (this.secondField != null) this.secondField.updateCursorCounter(); }

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, "CCTV controls", this.width / 2, 20, 0xFFFFFF);
		if (this.inputMode == 0) this.drawCenteredString(this.fontRendererObj, "Actions close this menu and run the server command", this.width / 2, 30, 0xAAAAAA);
		else {
			String label = this.inputMode == 1 ? "Group ID, then camera ID" : (this.inputMode == 2 ? "Look at a CCTV computer, then enter group ID" : "Camera ID");
			this.drawCenteredString(this.fontRendererObj, label, this.width / 2, 40, 0xAAAAAA);
			this.drawString(this.fontRendererObj, this.inputMode == 1 ? "Group ID" : "ID", this.width / 2 - 100, 68, 0xFFFFFF);
			this.firstField.drawTextBox();
			if (this.secondField != null) { this.drawString(this.fontRendererObj, "Camera ID", this.width / 2 - 100, 112, 0xFFFFFF); this.secondField.drawTextBox(); }
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
