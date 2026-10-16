package net.lax1dude.eaglercraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiPathfindingMenu;

/** Opens the local pathfinding controls without exposing commands in chat. */
public final class PathfindingMenuState {
	private static boolean ctrlKArmed;
	private static long ctrlKDeadline;

	private PathfindingMenuState() { }

	public static boolean handleKey(int keyCode) {
		boolean keyK = keyCode == KeyboardConstants.KEY_K || keyCode == 75;
		boolean keyP = keyCode == KeyboardConstants.KEY_P || keyCode == 80;
		if (keyK && (Keyboard.isKeyDown(KeyboardConstants.KEY_LCONTROL)
				|| Keyboard.isKeyDown(KeyboardConstants.KEY_RCONTROL) || Keyboard.isKeyDown(17))) {
			ctrlKArmed = true;
			ctrlKDeadline = Minecraft.getSystemTime() + 1500L;
			return true;
		}
		if (keyP && ctrlKArmed && Minecraft.getSystemTime() <= ctrlKDeadline) {
			ctrlKArmed = false;
			Minecraft.getMinecraft().displayGuiScreen(new GuiPathfindingMenu());
			return true;
		}
		if (Minecraft.getSystemTime() > ctrlKDeadline) ctrlKArmed = false;
		return false;
	}
}
