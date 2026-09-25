package net.lax1dude.eaglercraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCheatMenu;
import net.lax1dude.eaglercraft.sp.SingleplayerServerController;

/** Local-only cheat toggles. Never active on external multiplayer servers. */
public final class CheatMenuState {
    private static boolean playerEsp;
    private static boolean ctrlKArmed;
    private static long ctrlKDeadline;

    private CheatMenuState() { }

    public static boolean isAllowed() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.world != null && SingleplayerServerController.isClientInEaglerSingleplayer();
    }

    public static boolean isPlayerEsp() { return isAllowed() && playerEsp; }
    public static void setPlayerEsp(boolean enabled) { playerEsp = enabled && isAllowed(); }
    public static boolean togglePlayerEsp() {
        setPlayerEsp(!playerEsp);
        return playerEsp;
    }

    public static boolean handleKey(int keyCode) {
        if (!isAllowed()) {
            ctrlKArmed = false;
            return false;
        }
        boolean keyK = keyCode == KeyboardConstants.KEY_K || keyCode == 75;
        boolean keyC = keyCode == KeyboardConstants.KEY_C || keyCode == 67;
        if (keyK && (Keyboard.isKeyDown(KeyboardConstants.KEY_LCONTROL)
                || Keyboard.isKeyDown(KeyboardConstants.KEY_RCONTROL)
                || Keyboard.isKeyDown(17))) {
            ctrlKArmed = true;
            ctrlKDeadline = Minecraft.getSystemTime() + 1500L;
            return true;
        }
        if (keyC && ctrlKArmed && Minecraft.getSystemTime() <= ctrlKDeadline) {
            ctrlKArmed = false;
            Minecraft.getMinecraft().displayGuiScreen(new GuiCheatMenu());
            return true;
        }
        if (Minecraft.getSystemTime() > ctrlKDeadline) ctrlKArmed = false;
        return false;
    }
}
