package net.lax1dude.eaglercraft.mod;

import net.lax1dude.eaglercraft.mod.api.Mod;
import net.lax1dude.eaglercraft.mod.loader.ModLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import java.lang.reflect.Field;
import java.util.List;

public class FlyMod implements Mod {
    private boolean flying = false;
    private boolean buttonAdded = false;
    private static final int FLY_BUTTON_ID = 1001;

    @Override
    public String getName() { return "Fly Mod"; }
    @Override
    public String getVersion() { return "1.0.0"; }
    @Override
    public String getAuthor() { return "AI Assistant"; }

    @Override
    public void onTickInGui() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen instanceof GuiIngameMenu) {
            if (!buttonAdded) {
                addFlyButton(mc.currentScreen);
                buttonAdded = true;
            }
        } else {
            buttonAdded = false;
        }
    }

    private void addFlyButton(net.minecraft.client.gui.GuiScreen screen) {
        try {
            Field buttonListField = net.minecraft.client.gui.GuiScreen.class.getDeclaredField("buttonList");
            buttonListField.setAccessible(true);
            List<GuiButton> buttonList = (List<GuiButton>) buttonListField.get(screen);
            
            String label = "Fly: " + (flying ? "ON" : "OFF");
            buttonList.add(new GuiButton(FLY_BUTTON_ID, screen.width / 2 - 100, screen.height / 4 + 48, 200, 20, label));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActionPerformed(GuiButton button) {
        if (button.id == FLY_BUTTON_ID) {
            flying = !flying;
            button.displayString = "Fly: " + (flying ? "ON" : "OFF");
            
            // Apply flying to player
            if (Minecraft.getMinecraft().player != null) {
                Minecraft.getMinecraft().player.capabilities.allowFlying = flying;
                if (flying) {
                    Minecraft.getMinecraft().player.capabilities.isFlying = true;
                } else {
                    Minecraft.getMinecraft().player.capabilities.isFlying = false;
                }
            }
        }
    }
}
