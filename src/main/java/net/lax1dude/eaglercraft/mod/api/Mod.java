package net.lax1dude.eaglercraft.mod.api;

import net.minecraft.client.gui.GuiButton;

public interface Mod {
    String getName();
    String getVersion();
    String getAuthor();
    
    default void onInitialize() {}
    default void onGameStart() {}
    default void onGameShutdown() {}
    default void onTickInGame() {}
    default void onTickInGui() {}
    default void onActionPerformed(GuiButton button) {}
}
