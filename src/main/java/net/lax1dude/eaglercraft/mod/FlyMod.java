package net.lax1dude.eaglercraft.mod;

import java.lang.reflect.Field;
import java.util.List;

import net.lax1dude.eaglercraft.mod.api.Mod;
import net.lax1dude.eaglercraft.mod.api.ModLogger;
import net.lax1dude.eaglercraft.mod.api.ModMetadata;
import net.lax1dude.eaglercraft.mod.loader.ModLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiScreen;

public class FlyMod implements Mod {

    private boolean flying = false;
    private boolean buttonAdded = false;
    private static final int FLY_BUTTON_ID = 1001;
    private final ModLogger logger = new ModLogger("FlyMod");

    @Override
    public ModMetadata getModMetadata() {
        return new ModMetadata.Builder("Fly Mod", "1.0.0", "AI Assistant")
            .description("Adds a toggleable fly mode button to the in-game pause menu. Allows creative-style flying in any game mode.")
            .documentationPage("Usage")
            .documentationPage("Configuration")
            .build();
    }

    @Override
    public void onInitialize() {
        logger.info("Fly Mod initializing...");
    }

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

    private void addFlyButton(GuiScreen screen) {
        try {
            Field buttonListField = GuiScreen.class.getDeclaredField("buttonList");
            buttonListField.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<GuiButton> buttonList = (List<GuiButton>) buttonListField.get(screen);

            String label = "Fly: " + (flying ? "ON" : "OFF");
            buttonList.add(new GuiButton(FLY_BUTTON_ID, screen.width / 2 - 100, screen.height / 4 + 144, 200, 20, label));
        } catch (Exception e) {
            logger.error("Failed to add fly button", e);
        }
    }

    @Override
    public void onActionPerformed(GuiButton button) {
        if (button.id == FLY_BUTTON_ID) {
            flying = !flying;
            button.displayString = "Fly: " + (flying ? "ON" : "OFF");

            if (Minecraft.getMinecraft().player != null) {
                Minecraft.getMinecraft().player.capabilities.allowFlying = flying;
                Minecraft.getMinecraft().player.capabilities.isFlying = flying;
            }
            logger.info("Fly mode " + (flying ? "enabled" : "disabled"));
        }
    }

    @Override
    public String getDocumentationContent(String page) {
        switch (page) {
            case "Usage":
                return "§6§lFly Mod§r\n\n"
                    + "§7Author:§r AI Assistant\n"
                    + "§7Version:§r 1.0.0\n\n"
                    + "This mod adds a toggleable fly button\n"
                    + "to the in-game pause menu.\n\n"
                    + "§6How to use:§r\n"
                    + "1. Press §eEsc§r to open the pause menu\n"
                    + "2. Find the §eFly: OFF§r button\n"
                    + "3. Click it to enable flight\n"
                    + "4. Click again to disable\n\n"
                    + "Double-tap §eSpace§r to start flying\n"
                    + "while fly mode is enabled.";

            case "Configuration":
                return "§6§lFly Mod Configuration§r\n\n"
                    + "No configuration options available\n"
                    + "in this version.\n\n"
                    + "The mod is always active when loaded\n"
                    + "and can be toggled via the main menu.\n\n"
                    + "§ePlanned features:§r\n"
                    + "§7-§r Configurable fly speed\n"
                    + "§7-§r Auto-fly on join\n"
                    + "§7-§r Flight mode (creative/spectator)\n"
                    + "§7-§r Key binding customization";

            default:
                return "§cDocumentation not found: " + page;
        }
    }
}
