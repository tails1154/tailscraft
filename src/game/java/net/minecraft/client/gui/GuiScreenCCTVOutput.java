package net.minecraft.client.gui;

import java.io.IOException;
import java.util.List;

/** Displays recent CCTV-related server responses captured from chat. */
public class GuiScreenCCTVOutput extends GuiScreen {
	private final GuiScreen parent;

	public GuiScreenCCTVOutput(GuiScreen parent) { this.parent = parent; }

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) this.mc.displayGuiScreen(this.parent);
	}

	public void initGui() { this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height - 35, 200, 20, "Back")); }

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, "Recent CCTV server output", this.width / 2, 18, 0xFFFFFF);
		List<String> output = CCTVOutputState.snapshot();
		int y = 38;
		if (output.isEmpty()) {
			this.drawCenteredString(this.fontRendererObj, "No CCTV response captured yet.", this.width / 2, y, 0xAAAAAA);
		} else {
			int start = Math.max(0, output.size() - 24);
			for (int i = start; i < output.size() && y < this.height - 48; ++i) {
				this.drawString(this.fontRendererObj, output.get(i), 12, y, 0xE0E0E0);
				y += 12;
			}
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
