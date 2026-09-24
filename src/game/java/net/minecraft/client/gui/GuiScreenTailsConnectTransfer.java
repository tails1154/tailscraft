package net.minecraft.client.gui;

import java.io.IOException;
import java.util.Arrays;

import net.lax1dude.eaglercraft.tailsconnect.TailsConnectClient;

/** TailsConnect world-transfer submenu. */
public class GuiScreenTailsConnectTransfer extends GuiScreen {
	private final GuiScreen parent;

	public GuiScreenTailsConnectTransfer(GuiScreen parent) {
		this.parent = parent;
	}

	public boolean doesGuiPauseGame() {
		return false;
	}

	public void initGui() {
		buttonList.clear();
		int left = width / 2 - 150;
		int top = Math.max(42, height / 2 - 48);
		GuiButton send = new GuiButton(1, left, top + 42, 300, 20,
				TailsConnectClient.isHosting() ? "Send Current World" : "Waiting for Host");
		send.enabled = TailsConnectClient.isHosting();
		buttonList.add(send);
		buttonList.add(new GuiButton(0, left, top + 72, 300, 20, "Back"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) {
			mc.displayGuiScreen(parent);
		} else if (button.id == 1) {
			TailsConnectClient.transferWorld();
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		int top = Math.max(42, height / 2 - 48);
		drawCenteredString(fontRendererObj, "World Transfer", width / 2, top, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "The host sends a copy; the original is not deleted.", width / 2, top + 14, 0xAAAAAA);
		String status = TailsConnectClient.getError();
		if (status == null) status = TailsConnectClient.getState();
		drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(status, width - 16), width / 2, top + 112, 0x55FF55);
		super.drawScreen(mouseX, mouseY, partialTicks);
		for (GuiButton button : buttonList) if (button.hovered) {
			String tooltip = button.id == 1 ? "Transfer the current singleplayer save to every connected guest."
					: "Return to the TailsConnect menu.";
			drawHoveringText(Arrays.asList(tooltip), mouseX, mouseY);
			break;
		}
	}
}
