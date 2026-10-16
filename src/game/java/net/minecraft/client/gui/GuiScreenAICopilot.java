package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.ai.AICopilotController;
import net.minecraft.client.ai.AICraftAIPlanner;
import net.minecraft.client.ai.AICraftScriptLangPlus;

/** Opt-in control panel for the constrained AI copilot. */
public class GuiScreenAICopilot extends GuiScreen {
	private final GuiScreen parent;
	private GuiTextField xField;
	private GuiTextField yField;
	private GuiTextField zField;
	private GuiTextField commandField;
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
		commandField = new GuiTextField(7, fontRendererObj, left, 174, 300, 20);
		commandField.setMaxStringLength(180);
		buttonList.add(new GuiButton(3, left, 92, 146, 20, "Start pathfinding"));
		buttonList.add(new GuiButton(4, left + 154, 92, 146, 20, "Stop Copilot"));
		buttonList.add(new GuiButton(5, left, 116, 300, 20, "Look around"));
		buttonList.add(new GuiButton(7, left, 198, 300, 20, "Ask AI to plan"));
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
		} else if (button.id == 7) {
			String request = commandField.getText().trim();
			if (request.length() == 0) {
				status = "Type a request such as: mine diamonds";
			} else {
				status = "Asking AI for an AICraftScriptLangPlus plan...";
				AICraftAIPlanner.plan(request, context(), script -> {
					java.util.List<AICraftScriptLangPlus.Step> steps = AICraftScriptLangPlus.parse(script);
					if (steps.isEmpty()) status = "AI: I need a concrete task, such as 'mine diamonds'.";
					else { AICopilotController.runScript(steps); status = "AI plan accepted: " + script; }
				});
			}
		} else if (button.id == 6) {
			AICopilotController.stop();
			mc.displayGuiScreen(parent);
		}
	}

	public void updateScreen() {
		super.updateScreen();
		boolean locked = AICopilotController.isActive();
		xField.setEnabled(!locked);
		yField.setEnabled(!locked);
		zField.setEnabled(!locked);
		commandField.setEnabled(!locked);
		for (GuiButton button : buttonList) button.enabled = !locked || button.id == 4 || button.id == 6;
	}

	private String context() {
		if (mc.player == null) return "no world loaded";
		return "player position=" + (int) mc.player.posX + "," + (int) mc.player.posY + "," + (int) mc.player.posZ
				+ " dimension=" + mc.player.dimension;
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "AI Copilot", width / 2, 20, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "AI plans movement, exploration, looking, and scripted mining", width / 2, 36, 0xAAAAAA);
		drawString(fontRendererObj, "Target X", width / 2 - 150, 50, 0xFFFFFF);
		drawString(fontRendererObj, "Target Y", width / 2 - 48, 50, 0xFFFFFF);
		drawString(fontRendererObj, "Target Z", width / 2 + 54, 50, 0xFFFFFF);
		xField.drawTextBox();
		yField.drawTextBox();
		zField.drawTextBox();
		drawString(fontRendererObj, "Natural-language request", width / 2 - 150, 162, 0xFFFFFF);
		commandField.drawTextBox();
		if (commandField.getText().isEmpty() && !commandField.isFocused()) drawString(fontRendererObj, "Example: mine diamonds", commandField.xPosition + 4, commandField.yPosition + 6, 0x777777);
		String liveStatus = AICopilotController.isActive() ? AICopilotController.getStatus() : status;
		drawString(fontRendererObj, fontRendererObj.trimStringToWidth(liveStatus, 210), width - 220, 8, 0x55FF55);
		drawCenteredString(fontRendererObj, liveStatus, width / 2, 228, 0x55FF55);
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
		commandField.textboxKeyTyped(c, keyCode);
	}

	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		xField.mouseClicked(mouseX, mouseY, mouseButton);
		yField.mouseClicked(mouseX, mouseY, mouseButton);
		zField.mouseClicked(mouseX, mouseY, mouseButton);
		commandField.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
