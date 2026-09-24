package net.minecraft.client.gui;

import java.io.IOException;

/** AuthMe register/login prompt opened from server chat instructions. */
public class GuiScreenAuthMe extends GuiScreen {
	private final GuiScreen parent;
	private final boolean register;
	private GuiTextField password;
	private GuiTextField confirmation;
	private String error;

	public GuiScreenAuthMe(GuiScreen parent, boolean register) {
		this.parent = parent;
		this.register = register;
	}

	public boolean doesGuiPauseGame() {
		return false;
	}

	public void initGui() {
		int left = width / 2 - 100;
		int top = height / 2 - (register ? 64 : 45);
		password = new GuiTextField(0, fontRendererObj, left, top + 24, 200, 20);
		password.setMaxStringLength(64);
		password.setPasswordMode(true);
		password.setFocused(true);
		buttonList.clear();
		if (register) {
			confirmation = new GuiTextField(1, fontRendererObj, left, top + 62, 200, 20);
			confirmation.setMaxStringLength(64);
			confirmation.setPasswordMode(true);
		}
		buttonList.add(new GuiButton(0, left, top + (register ? 100 : 62), 98, 20, register ? "Register" : "Login"));
		buttonList.add(new GuiButton(1, left + 102, top + (register ? 100 : 62), 98, 20, "Cancel"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 1 || button.displayString.equals("Cancel")) {
			mc.displayGuiScreen(parent);
			return;
		}
		String value = password.getText();
		if (value.isEmpty()) {
			error = "Enter a password.";
			return;
		}
		if (register && !value.equals(confirmation.getText())) {
			error = "Passwords do not match.";
			return;
		}
		mc.player.sendChatMessage(register ? "/register " + value + " " + confirmation.getText() : "/login " + value);
		mc.displayGuiScreen(parent);
	}

	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1) {
			mc.displayGuiScreen(parent);
			return;
		}
		if (password.isFocused()) password.textboxKeyTyped(typedChar, keyCode);
		else if (confirmation != null && confirmation.isFocused()) confirmation.textboxKeyTyped(typedChar, keyCode);
	}

	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		password.mouseClicked(mouseX, mouseY, mouseButton);
		if (confirmation != null) confirmation.mouseClicked(mouseX, mouseY, mouseButton);
	}

	public void updateScreen() {
		password.updateCursorCounter();
		if (confirmation != null) confirmation.updateCursorCounter();
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		int top = height / 2 - (register ? 64 : 45);
		drawCenteredString(fontRendererObj, register ? "Server Registration" : "Server Login", width / 2, top, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "AuthMe", width / 2, top - 14, 0xAAAAAA);
		drawString(fontRendererObj, "Password", width / 2 - 100, top + 14, 0xFFFFFF);
		password.drawTextBox();
		if (register) {
			drawString(fontRendererObj, "Confirm password", width / 2 - 100, top + 52, 0xFFFFFF);
			confirmation.drawTextBox();
		}
		if (error != null) drawCenteredString(fontRendererObj, error, width / 2, top + (register ? 126 : 88), 0xFF5555);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
