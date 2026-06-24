package net.lax1dude.eaglercraft.mod.api;

import java.util.Collections;
import java.util.List;
import net.lax1dude.eaglercraft.mod.api.events.ModEventListener;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public interface Mod extends ModEventListener {

    ModMetadata getModMetadata();

    default String getName() { return getModMetadata().getName(); }
    default String getVersion() { return getModMetadata().getVersion(); }
    default String getAuthor() { return getModMetadata().getAuthor(); }
    default String getDescription() { return getModMetadata().getDescription(); }
    default List<String> getDocumentationPages() { return getModMetadata().getDocumentationPages(); }

    default ModLogger getLogger() { return new ModLogger(getName()); }
    default ModConfig getConfig() { return new ModConfig(getName()); }

    default void onInitialize() {}
    default void onGameStart() {}
    default void onGameShutdown() {}
    default void onWorldLoad() {}
    default void onWorldUnload() {}
    default void onTickInGame() {}
    default void onTickInGui() {}
    default void onActionPerformed(GuiButton button) {}
    default List<GuiButton> getCustomButtons(GuiScreen screen) { return Collections.emptyList(); }

    default String getDocumentationContent(String page) { return ""; }

    @Override
    default void onEvent(net.lax1dude.eaglercraft.mod.api.events.ModEvent event) {
        switch (event.getType()) {
            case GAME_STARTED: onGameStart(); break;
            case GAME_SHUTDOWN: onGameShutdown(); break;
            case WORLD_LOADED: onWorldLoad(); break;
            case WORLD_UNLOADED: onWorldUnload(); break;
            case TICK_IN_GAME: onTickInGame(); break;
            case TICK_IN_GUI: onTickInGui(); break;
            default: break;
        }
    }
}
