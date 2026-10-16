package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;
import net.lax1dude.eaglercraft.opengl.GlStateManager;

/** Small native-style tab element for client panels that need real tab state. */
public class GuiTab extends GuiButton {
	private boolean selected;

	public GuiTab(int id, int x, int y, int width, String label, boolean selected) {
		super(id, x, y, width, 22, label);
		this.selected = selected;
	}

	public void setSelected(boolean selected) { this.selected = selected; }

	public void func_191745_a(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!visible) return;
		hovered = mouseX >= xPosition && mouseY >= yPosition && mouseX < xPosition + width && mouseY < yPosition + height;
		int color = selected ? 0xFF315E3A : hovered ? 0xFF3E3E3E : 0xFF242424;
		GlStateManager.disableTexture2D();
		drawRect(xPosition, yPosition, xPosition + width, yPosition + height, color);
		if (selected) drawRect(xPosition, yPosition + height - 2, xPosition + width, yPosition + height, 0xFF80FF80);
		GlStateManager.enableTexture2D();
		drawCenteredString(mc.fontRendererObj, displayString, xPosition + width / 2, yPosition + 7, selected ? 0xFFFFFFFF : 0xFFCCCCCC);
	}
}
