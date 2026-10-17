package net.minecraft.client.gui;

import java.io.IOException;

import net.lax1dude.eaglercraft.CheatMenuState;

/** Local singleplayer quality-of-life and cheat controls. */
public class GuiCheatMenu extends GuiScreen {
	private static final int TAB_VISUAL = 0;
	private static final int TAB_MOVEMENT = 1;
	private static final int TAB_ACTIONS = 2;
	private int activeTab = TAB_VISUAL;

	public boolean doesGuiPauseGame() { return true; }

	public void initGui() {
		buttonList.clear();
		int left = width / 2 - 150;
		int top = height / 2 - 78;
		buttonList.add(new GuiTab(100, left, top, 100, "Visual", activeTab == TAB_VISUAL));
		buttonList.add(new GuiTab(101, left + 100, top, 100, "Movement", activeTab == TAB_MOVEMENT));
		buttonList.add(new GuiTab(102, left + 200, top, 100, "Actions", activeTab == TAB_ACTIONS));
		if (activeTab == TAB_VISUAL) {
			buttonList.add(new GuiButton(1, left, top + 42, 300, 20, "Player ESP: " + on(CheatMenuState.isPlayerEsp())));
			buttonList.add(new GuiButton(2, left, top + 68, 300, 20, "Night Vision: " + on(CheatMenuState.isNightVision())));
		} else if (activeTab == TAB_MOVEMENT) {
			buttonList.add(new GuiButton(3, left, top + 42, 300, 20, "Flight: " + on(CheatMenuState.isFlight())));
			buttonList.add(new GuiButton(4, left, top + 68, 300, 20, "No Fall Damage: " + on(CheatMenuState.isNoFall())));
			buttonList.add(new GuiButton(5, left, top + 94, 300, 20, "Speed Boost: " + on(CheatMenuState.isSpeedBoost())));
		} else {
			buttonList.add(new GuiButton(6, left, top + 42, 146, 20, "Heal"));
			buttonList.add(new GuiButton(7, left + 154, top + 42, 146, 20, "Refill Hunger"));
			buttonList.add(new GuiButton(8, left, top + 68, 300, 20, "Clear Effects"));
		}
		buttonList.add(new GuiButton(0, left, top + 130, 300, 20, "Done"));
	}

	private static String on(boolean enabled) { return enabled ? "ON" : "OFF"; }

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id >= 100 && button.id <= 102) { activeTab = button.id - 100; initGui(); }
		else if (button.id == 0) mc.displayGuiScreen(null);
		else if (button.id >= 1 && button.id <= 8 && CheatMenuState.registerCheatUse()) { return; }
		else if (button.id == 1) { CheatMenuState.togglePlayerEsp(); initGui(); }
		else if (button.id == 2) { CheatMenuState.toggleNightVision(); initGui(); }
		else if (button.id == 3) { CheatMenuState.toggleFlight(); initGui(); }
		else if (button.id == 4) { CheatMenuState.toggleNoFall(); initGui(); }
		else if (button.id == 5) { CheatMenuState.toggleSpeedBoost(); initGui(); }
		else if (button.id == 6) CheatMenuState.heal();
		else if (button.id == 7) CheatMenuState.feed();
		else if (button.id == 8) CheatMenuState.clearEffects();
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		int left = width / 2 - 150;
		int top = height / 2 - 78;
		drawCenteredString(fontRendererObj, "Singleplayer QOL Menu", width / 2, top - 42, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "Ctrl+K, release, then C to reopen", width / 2, top - 26, 0xAAAAAA);
		if (activeTab == TAB_VISUAL) drawString(fontRendererObj, "Client-side visibility helpers", left, top + 26, 0xAAAAAA);
		else if (activeTab == TAB_MOVEMENT) drawString(fontRendererObj, "Local movement helpers", left, top + 26, 0xAAAAAA);
		else drawString(fontRendererObj, "Quick player actions", left, top + 26, 0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
