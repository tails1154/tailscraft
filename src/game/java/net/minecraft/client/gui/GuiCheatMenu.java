package net.minecraft.client.gui;

import java.io.IOException;

import net.lax1dude.eaglercraft.CheatMenuState;

/** Local singleplayer cheat menu. */
public class GuiCheatMenu extends GuiScreen {
    public boolean doesGuiPauseGame() { return true; }

    public void initGui() {
        buttonList.clear();
        int left = width / 2 - 100;
        buttonList.add(new GuiButton(1, left, height / 2 - 34, 200, 20,
                "Player ESP: " + (CheatMenuState.isPlayerEsp() ? "ON" : "OFF")));
        buttonList.add(new GuiButton(2, left, height / 2 + 2, 200, 20, "Done"));
    }

    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            CheatMenuState.togglePlayerEsp();
            initGui();
        } else if (button.id == 2) {
            mc.displayGuiScreen(null);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "Singleplayer Cheat Menu", width / 2, height / 2 - 70, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Ctrl+K, release, then C to reopen", width / 2, height / 2 - 52, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
