package net.minecraft.client.gui;

import java.io.IOException;
import java.util.List;
import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.internal.FileChooserResult;
import net.minecraft.client.resources.I18n;
import net.lax1dude.eaglercraft.mod.api.Mod;
import net.lax1dude.eaglercraft.mod.loader.ModLoader;

/** Mod information screen for the WASM-GC Java/JavaScript mod system. */
public class GuiScreenTailsForgeMods extends GuiScreen {
	private final GuiScreen parent;
	private List<Mod> javaMods;
	private int javaScriptModCount;
	private String status;

	public GuiScreenTailsForgeMods(GuiScreen parent) {
		this.parent = parent;
	}

	public boolean doesGuiPauseGame() {
		return false;
	}

	public void initGui() {
		javaMods = ModLoader.getLoadedMods();
		javaScriptModCount = ModLoader.getJavaScriptModCount();
		buttonList.clear();
		buttonList.add(new GuiButton(2, width / 2 - 150, height - 52, 98, 20, "Upload .js Mod"));
		buttonList.add(new GuiButton(1, width / 2 - 48, height - 52, 98, 20, "Refresh"));
		buttonList.add(new GuiButton(0, width / 2 + 54, height - 52, 98, 20, I18n.format("gui.done")));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) mc.displayGuiScreen(parent);
		else if (button.id == 1) initGui();
		else if (button.id == 2) EagRuntime.displayFileChooser("application/javascript", "js");
	}

	public void updateScreen() {
		if (!EagRuntime.fileChooserHasResult()) return;
		FileChooserResult result = EagRuntime.getFileChooserResult();
		EagRuntime.clearFileChooserResult();
		if (result == null || result.fileName == null || !result.fileName.toLowerCase().endsWith(".js")) {
			status = "Please choose a .js file";
			return;
		}
		ModLoader.installJavaScriptMod(result.fileName, result.fileData);
		status = "Stored " + result.fileName + "; reload the game to activate it";
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "TailsForge Mods", width / 2, 15, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "JavaScript mods are loaded from window.tailsForgeMods", width / 2, 32, 0xAAAAAA);
		drawCenteredString(fontRendererObj, "Stored JavaScript mods: " + javaScriptModCount, width / 2, 45, 0x99CCFF);
		int y = 68;
		if (javaMods.isEmpty()) {
			drawCenteredString(fontRendererObj, "No built-in Java mods loaded", width / 2, y, 0xAAAAAA);
		} else {
			for (Mod mod : javaMods) {
				drawString(fontRendererObj, mod.getName() + " " + mod.getVersion(), width / 2 - 145, y, 0xFFFFFF);
				drawString(fontRendererObj, "by " + mod.getAuthor(), width / 2 - 145, y + 12, 0xAAAAAA);
				y += 32;
			}
		}
		if (status != null) drawCenteredString(fontRendererObj, status, width / 2, height - 72, 0x55FF55);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
