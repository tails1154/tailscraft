package net.lax1dude.eaglercraft.mod.gui;

import java.util.List;

import net.lax1dude.eaglercraft.mod.api.Mod;
import net.lax1dude.eaglercraft.mod.loader.ModLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;

public class ModListSlot extends GuiSlot {

    private final GuiModsMenu parent;
    private final List<Mod> mods;

    public ModListSlot(GuiModsMenu parent) {
        super(Minecraft.getMinecraft(), parent.width, parent.height, 32, parent.height - 64, 36);
        this.parent = parent;
        this.mods = ModLoader.getLoadedMods();
    }

    @Override
    protected int getSize() {
        return mods.size();
    }

    @Override
    protected void elementClicked(int slotIndex, boolean isDoubleClick, int mouseX, int mouseY) {
        if (slotIndex >= 0 && slotIndex < mods.size()) {
            parent.getModListSlot().selectElement(slotIndex);
        }
    }

    private void selectElement(int index) {
        this.selectedElement = index;
    }

    public Mod getSelectedMod() {
        if (selectedElement >= 0 && selectedElement < mods.size()) {
            return mods.get(selectedElement);
        }
        return null;
    }

    @Override
    protected boolean isSelected(int slotIndex) {
        return slotIndex == selectedElement;
    }

    @Override
    protected void drawBackground() {
    }

    @Override
    protected void func_192637_a(int slotIndex, int xPos, int yPos, int heightIn, int mouseXIn, int mouseYIn, float partialTicks) {
        if (slotIndex < 0 || slotIndex >= mods.size()) return;
        Mod mod = mods.get(slotIndex);

        String name = mod.getName();
        String version = "v" + mod.getVersion();
        String author = mod.getAuthor();

        this.mc.fontRendererObj.drawStringWithShadow(name, xPos + 4, yPos + 2, 16777215);
        this.mc.fontRendererObj.drawString(version, xPos + 4, yPos + 14, 8421504);

        int versionWidth = this.mc.fontRendererObj.getStringWidth(version);
        this.mc.fontRendererObj.drawString("by " + author, xPos + 6 + versionWidth, yPos + 14, 11184810);

        String desc = mod.getDescription();
        if (!desc.isEmpty()) {
            String trimmed = desc;
            int maxWidth = parent.width - 40;
            if (this.mc.fontRendererObj.getStringWidth(trimmed) > maxWidth) {
                while (this.mc.fontRendererObj.getStringWidth(trimmed + "...") > maxWidth && trimmed.length() > 2) {
                    trimmed = trimmed.substring(0, trimmed.length() - 1);
                }
                trimmed += "...";
            }
            this.mc.fontRendererObj.drawString(trimmed, xPos + 4, yPos + 24, 10526880);
        }
    }

    @Override
    public int getListWidth() {
        return parent.width - 20;
    }

    @Override
    protected int getScrollBarX() {
        return parent.width - 10;
    }
}
