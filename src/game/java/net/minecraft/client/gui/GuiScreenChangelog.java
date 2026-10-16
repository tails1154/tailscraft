package net.minecraft.client.gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import net.lax1dude.eaglercraft.internal.PlatformRuntime;

/** Compact in-game changelog viewer. */
public class GuiScreenChangelog extends GuiScreen {
	private static final String CHANGELOG_URL = "https://raw.githubusercontent.com/tails1154/tailscraft/main/CHANGELOG.md";
	private static final String[] FALLBACK_LINES = new String[] {
			"TailsCraft changes", "", "TailsConnect multiplayer and TC3 transport", "TC4 friends, presence, and join requests",
			"TC5 hosted worlds with upload progress", "EPW world export and headless hosting", "", "AuthMe register/login prompt",
			"Remember me with per-server saved passwords", "Options > Saved passwords > Forget", "Automatic orphan-password cleanup",
			"1.13-style command suggestions", "Singleplayer /gamemode command", "Always sprint and graphics defaults", "", "Removed:",
			"Broken security cameras", "Broken lanterns and chains", "Spear/grappling-hook experiment", "World transfer", "Voice chat experiment" };
	private final GuiScreen parent;
	private int page;
	private String[] lines;
	private boolean loading;
	private boolean failed;

	public GuiScreenChangelog(GuiScreen parent) {
		this.parent = parent;
	}

	public void initGui() {
		lines = null;
		loading = true;
		failed = false;
		PlatformRuntime.downloadRemoteURIByteArray(CHANGELOG_URL, false, data -> {
			if (data != null && data.length > 0) {
				lines = new String(data, StandardCharsets.UTF_8).replace("\r", "").split("\n");
				loading = false;
			} else {
				failed = true;
				loading = false;
			}
			updateButtons();
		});
		buttonList.clear();
		buttonList.add(new GuiButton(0, width / 2 - 100, height - 28, 200, 20, "Back"));
		buttonList.add(new GuiButton(1, width / 2 - 105, height - 52, 100, 20, "Previous"));
		buttonList.add(new GuiButton(2, width / 2 + 5, height - 52, 100, 20, "Next"));
		updateButtons();
	}

	private void updateButtons() {
		buttonList.get(1).enabled = page > 0;
		buttonList.get(2).enabled = (page + 1) * 12 < lineCount();
	}

	private int lineCount() {
		return lines != null ? lines.length : FALLBACK_LINES.length;
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) mc.displayGuiScreen(parent);
		if (button.id == 1 && page > 0) { --page; updateButtons(); }
		if (button.id == 2 && (page + 1) * 12 < lineCount()) { ++page; updateButtons(); }
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "TailsCraft Changelog", width / 2, 18, 0xFFFFFF);
		if (loading) drawCenteredString(fontRendererObj, "Loading changelog from GitHub...", width / 2, 70, 0xAAAAAA);
		if (failed) drawCenteredString(fontRendererObj, "GitHub unavailable; showing bundled changelog", width / 2, 34, 0xFFCC66);
		String[] activeLines = lines != null ? lines : FALLBACK_LINES;
		int start = page * 12;
		for (int i = 0; i < 12 && start + i < activeLines.length; ++i) {
			drawString(fontRendererObj, activeLines[start + i], width / 2 - 150, 48 + i * 16, 0xEEEEEE);
		}
		drawCenteredString(fontRendererObj, "Page " + (page + 1), width / 2, height - 66, 0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
