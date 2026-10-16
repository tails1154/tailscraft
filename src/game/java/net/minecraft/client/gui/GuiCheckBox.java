package net.minecraft.client.gui;

import net.minecraft.client.Minecraft;

/** Small vanilla-style checkbox control for screens that need a boolean option. */
public class GuiCheckBox extends GuiButton {
	private boolean checked;

	public GuiCheckBox(int buttonId, int x, int y, String label, boolean checkedIn) {
		super(buttonId, x, y, 200, 20, label);
		this.checked = checkedIn;
	}

	public boolean isChecked() {
		return checked;
	}

	public void setChecked(boolean checkedIn) {
		this.checked = checkedIn;
	}

	@Override
	public boolean mousePressed(Minecraft mc, int mouseX, int mouseY) {
		if (super.mousePressed(mc, mouseX, mouseY)) {
			checked = !checked;
			return true;
		}
		return false;
	}

	@Override
	public void func_191745_a(Minecraft mc, int mouseX, int mouseY, float partialTicks) {
		if (!visible) return;
		hovered = mouseX >= xPosition && mouseY >= yPosition
				&& mouseX < xPosition + width && mouseY < yPosition + height;
		int boxColor = enabled ? 0xffeeeeee : 0xff888888;
		drawRect(xPosition, yPosition + 3, xPosition + 13, yPosition + 16, boxColor);
		drawRect(xPosition + 1, yPosition + 4, xPosition + 12, yPosition + 15, 0xff202020);
		if (checked) {
			drawRect(xPosition + 3, yPosition + 6, xPosition + 10, yPosition + 13,
					enabled ? 0xff55aa55 : 0xff557755);
		}
		drawString(mc.fontRendererObj, displayString, xPosition + 19, yPosition + 5,
				enabled ? 0xffffffff : 0xffaaaaaa);
	}
}
