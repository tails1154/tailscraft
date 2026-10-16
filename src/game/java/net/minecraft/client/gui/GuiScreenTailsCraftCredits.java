package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.resources.I18n;

/** Credits for TailsCraft additions and bundled third-party assets. */
public class GuiScreenTailsCraftCredits extends GuiScreen {
	private final GuiScreen parent;

	private static final String[] LINES = new String[] {
			"TailsCraft Credits",
			"",
			"TailsCraft client additions",
			"TailsConnect, TailsCraft UI, and client features",
			"",
			"Music",
			"Sonic Blaster - F-777",
			"https://soundcloud.com/f-777/sonic-blaster",
			"Shiawase VIP (Remix) - razaplays",
			"https://www.newgrounds.com/audio/listen/1296027",
			"",
			"Bundled resources",
			"MS Paintcraft - Modrinth resource pack",
			"https://modrinth.com/resourcepack/mspaintcraft",
			"Minecraft assets and code remain property of their respective owners."
	};

	public GuiScreenTailsCraftCredits(GuiScreen parent) {
		this.parent = parent;
	}

	public void initGui() {
		buttonList.clear();
		buttonList.add(new GuiButton(0, width / 2 - 100, height - 28, 200, 20, I18n.format("gui.done")));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) mc.displayGuiScreen(parent);
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, LINES[0], width / 2, 20, 0xFFFFFF);
		int y = 52;
		for (int i = 1; i < LINES.length; ++i) {
			int color = LINES[i].startsWith("http") ? 0x88AAFF : 0xEEEEEE;
			drawCenteredString(fontRendererObj, LINES[i], width / 2, y, color);
			y += 18;
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
