package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.ai.AICopilotController;

/** Opt-in control panel for the constrained AI copilot. */
public class GuiScreenAICopilot extends GuiScreen {
	private final GuiScreen parent;
	private GuiTextField xField;
	private GuiTextField yField;
	private GuiTextField zField;
	private String status = "Stopped";

	public GuiScreenAICopilot(GuiScreen parent) {
		this.parent = parent;
	}

	public boolean doesGuiPauseGame() {
		return false;
	}

	public void initGui() {
		buttonList.clear();
		int left = width / 2 - 150;
		xField = new GuiTextField(0, fontRendererObj, left, 62, 96, 20);
		yField = new GuiTextField(1, fontRendererObj, left + 102, 62, 96, 20);
		zField = new GuiTextField(2, fontRendererObj, left + 204, 62, 96, 20);
		xField.setText("0");
		yField.setText("64");
		zField.setText("0");
		xField.setMaxStringLength(8);
		yField.setMaxStringLength(8);
		zField.setMaxStringLength(8);
		buttonList.add(new GuiButton(3, left, 92, 146, 20, "Start pathfinding"));
		buttonList.add(new GuiButton(4, left + 154, 92, 146, 20, "Stop Copilot"));
		buttonList.add(new GuiButton(5, left, 116, 300, 20, "Look around"));
		buttonList.add(new GuiButton(6, width / 2 - 100, height - 28, 200, 20, "Back"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 3) {
			try {
				AICopilotController.setTarget(Integer.parseInt(xField.getText().trim()), Integer.parseInt(yField.getText().trim()), Integer.parseInt(zField.getText().trim()));
				AICopilotController.start();
				status = "Pathfinding to " + xField.getText() + ", " + yField.getText() + ", " + zField.getText();
			} catch (NumberFormatException ex) {
				status = "Enter integer XYZ coordinates";
			}
		} else if (button.id == 4) {
			AICopilotController.stop();
			status = "Stopped";
		} else if (button.id == 5) {
			AICopilotController.lookAround();
			status = "Looking around";
		} else if (button.id == 6) {
			AICopilotController.stop();
			mc.displayGuiScreen(parent);
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "AI Copilot", width / 2, 20, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "Only pathfinding and camera movement are enabled", width / 2, 36, 0xAAAAAA);
		drawString(fontRendererObj, "Target X", width / 2 - 150, 50, 0xFFFFFF);
		drawString(fontRendererObj, "Target Y", width / 2 - 48, 50, 0xFFFFFF);
		drawString(fontRendererObj, "Target Z", width / 2 + 54, 50, 0xFFFFFF);
		xField.drawTextBox();
		yField.drawTextBox();
		zField.drawTextBox();
		drawCenteredString(fontRendererObj, status, width / 2, 150, 0x55FF55);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	protected void keyTyped(char c, int keyCode) throws IOException {
		if (keyCode == 1) {
			AICopilotController.stop();
			mc.displayGuiScreen(parent);
			return;
		}
		xField.textboxKeyTyped(c, keyCode);
		yField.textboxKeyTyped(c, keyCode);
		zField.textboxKeyTyped(c, keyCode);
	}

	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		xField.mouseClicked(mouseX, mouseY, mouseButton);
		yField.mouseClicked(mouseX, mouseY, mouseButton);
		zField.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
