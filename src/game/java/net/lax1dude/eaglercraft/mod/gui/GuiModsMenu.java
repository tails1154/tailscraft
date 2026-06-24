package net.lax1dude.eaglercraft.mod.gui;

import java.io.IOException;
import java.util.List;

import net.lax1dude.eaglercraft.mod.api.Mod;
import net.lax1dude.eaglercraft.mod.loader.ModLoader;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;

public class GuiModsMenu extends GuiScreen {

    private final GuiScreen parentScreen;
    private GuiButton modsEnabledButton;
    private ModListSlot modListSlot;
    private int listWidth = 220;

    public GuiModsMenu(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.listWidth = Math.min(220, this.width - 20);

        if (modListSlot == null) {
            modListSlot = new ModListSlot(this);
        }
        modListSlot.setDimensions(this.width, this.height, 32, this.height - 64);

        int centerX = this.width / 2;

        this.modsEnabledButton = new GuiButton(0, centerX - 100, this.height - 52, 200, 20,
                "Mods Enabled: " + (ModLoader.isModsEnabled() ? "ON" : "OFF"));
        this.buttonList.add(modsEnabledButton);

        this.buttonList.add(new GuiButton(1, centerX - 100, this.height - 28, 200, 20,
                I18n.format("gui.done")));

        this.buttonList.add(new GuiButton(2, centerX + 110, 32, 60, 20, "Info"));
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (modListSlot != null) {
            modListSlot.handleMouseInput();
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            ModLoader.setModsEnabled(!ModLoader.isModsEnabled());
            button.displayString = "Mods Enabled: " + (ModLoader.isModsEnabled() ? "ON" : "OFF");
        } else if (button.id == 1) {
            this.mc.displayGuiScreen(parentScreen);
        } else if (button.id == 2) {
            Mod selected = modListSlot.getSelectedMod();
            if (selected != null) {
                this.mc.displayGuiScreen(new GuiModInfo(this, selected));
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        if (modListSlot != null) {
            modListSlot.drawScreen(mouseX, mouseY, partialTicks);
        }
        this.drawCenteredString(this.fontRendererObj, "Mods", this.width / 2, 16, 16777215);

        List<Mod> mods = ModLoader.getLoadedMods();
        int count = mods.size();
        String status = count + " mod" + (count == 1 ? "" : "s") + " loaded"
                + (ModLoader.isModsEnabled() ? "" : " (disabled)");
        this.drawCenteredString(this.fontRendererObj, status, this.width / 2, this.height - 62, 8421504);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    public ModListSlot getModListSlot() {
        return modListSlot;
    }
}
