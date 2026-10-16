package net.minecraft.client.gui;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.network.AuthMeStorage;

/** Options screen for removing locally remembered AuthMe passwords. */
public class GuiScreenSavedPasswords extends GuiScreen {
	private final GuiScreen parent;
	private List<String> servers;

	public GuiScreenSavedPasswords(GuiScreen parent) {
		this.parent = parent;
	}

	public void initGui() {
		AuthMeStorage.cleanupUnlistedLegacyPasswords();
		servers = AuthMeStorage.getServers();
		buttonList.clear();
		int left = width / 2 - 150;
		int top = height / 6 + 30;
		for (int i = 0; i < servers.size(); ++i) {
			buttonList.add(new GuiButton(100 + i, left + 170, top + i * 24, 130, 20, "Forget"));
		}
		buttonList.add(new GuiButton(200, width / 2 - 100, height - 38, 200, 20, "Done"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 200) {
			mc.displayGuiScreen(parent);
		} else if (button.id >= 100 && button.id < 100 + servers.size()) {
			AuthMeStorage.forget(servers.get(button.id - 100));
			initGui();
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "Saved AuthMe passwords", width / 2, 15, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "Stored only when Remember me is enabled", width / 2, 31, 0xAAAAAA);
		int left = width / 2 - 150;
		int top = height / 6 + 30;
		if (servers.isEmpty()) {
			drawCenteredString(fontRendererObj, "No saved passwords", width / 2, top + 8, 0xAAAAAA);
		} else {
			for (int i = 0; i < servers.size(); ++i) {
				drawString(fontRendererObj, servers.get(i), left, top + i * 24 + 6, 0xFFFFFF);
			}
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
