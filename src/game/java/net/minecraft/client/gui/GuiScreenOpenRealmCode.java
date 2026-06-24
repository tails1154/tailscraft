package net.minecraft.client.gui;

import java.io.IOException;

import net.lax1dude.eaglercraft.sp.SingleplayerServerController;
import net.minecraft.client.resources.I18n;

public class GuiScreenOpenRealmCode extends GuiScreen {

	private final GuiScreen parent;
	private int ticksElapsed = 0;
	private String realmCode = null;

	public GuiScreenOpenRealmCode(GuiScreen parent) {
		this.parent = parent;
	}

	public void initGui() {
		this.buttonList.clear();
		this.buttonList.add(new GuiButton(0, this.width / 2 - 100, this.height / 4 + 120, I18n.format("gui.done")));
	}

	public void updateScreen() {
		++this.ticksElapsed;
		if (this.realmCode == null) {
			this.realmCode = SingleplayerServerController.getCurrentRealmCode();
		}
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 0) {
			this.mc.displayGuiScreen(this.parent);
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
		this.drawCenteredString(this.fontRendererObj, I18n.format("menu.shareToLan"), this.width / 2, this.height / 4 - 10, 16777215);
		if (this.realmCode == null) {
			this.drawCenteredString(this.fontRendererObj, I18n.format("eaglercraft.lanServer.pleaseWait"), this.width / 2, this.height / 4 + 20, 16777215);
			if (this.ticksElapsed > 200) {
				this.drawCenteredString(this.fontRendererObj, "Timed out waiting for realm code", this.width / 2, this.height / 4 + 36, 16733525);
			}
		} else {
			this.drawCenteredString(this.fontRendererObj, "Realm Code: " + this.realmCode, this.width / 2, this.height / 4 + 20, 5635925);
			this.drawCenteredString(this.fontRendererObj, "Players can join with: realms://" + this.realmCode, this.width / 2, this.height / 4 + 36, 11184810);
		}
		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
