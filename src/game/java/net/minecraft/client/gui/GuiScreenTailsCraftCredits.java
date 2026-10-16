package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.client.resources.I18n;

/** Paged credits and third-party license notices. */
public class GuiScreenTailsCraftCredits extends GuiScreen {
	private final GuiScreen parent;
	private int page;

	private static final String[][] PAGES = new String[][] {
			{
				"TailsCraft Credits", "", "TailsCraft client additions",
				"TailsConnect, friends, UI, and client features", "Baritone-compatible command client",
				"TailsCraft project", "https://github.com/tails1154/tailscraft", "",
				"Minecraft and Eaglercraft assets/code remain the property of their respective authors."
			},
			{
				"Pathfinding and Baritone", "", "Baritone", "Copyright (c) 2018-2026 Baritone contributors",
				"https://github.com/cabaletta/baritone", "License: GNU LGPL-3.0-or-later",
				"TailsCraft ports the basic 1.12.2-era Baritone command surface:",
				"#goto, #mine, and #stop. See the third-party notice for the source commit.",
				"See THIRD_PARTY_NOTICES.md for source and license information."
			},
			{
				"Bundled resources", "", "Sonic Blaster - F-777",
				"https://soundcloud.com/f-777/sonic-blaster", "Shiawase VIP (Remix) - razaplays",
				"https://www.newgrounds.com/audio/listen/1296027", "MS Paintcraft resource pack",
				"https://modrinth.com/resourcepack/mspaintcraft", "", "Open-source dependencies retain their own notices."
			}
	};

	public GuiScreenTailsCraftCredits(GuiScreen parent) { this.parent = parent; }

	public void initGui() {
		buttonList.clear();
		buttonList.add(new GuiButton(1, width / 2 - 150, height - 28, 96, 20, "Previous"));
		buttonList.add(new GuiButton(0, width / 2 - 48, height - 28, 96, 20, I18n.format("gui.done")));
		buttonList.add(new GuiButton(2, width / 2 + 54, height - 28, 96, 20, "Next"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) mc.displayGuiScreen(parent);
		else if (button.id == 1 && page > 0) --page;
		else if (button.id == 2 && page + 1 < PAGES.length) ++page;
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, PAGES[page][0], width / 2, 20, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "Page " + (page + 1) + " / " + PAGES.length, width / 2, 36, 0xAAAAAA);
		int y = 62;
		for (int i = 1; i < PAGES[page].length; ++i) {
			String line = PAGES[page][i];
			int color = line.startsWith("http") ? 0x88AAFF : 0xEEEEEE;
			drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(line, width - 32), width / 2, y, color);
			y += 18;
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
