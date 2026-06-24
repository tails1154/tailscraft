package net.lax1dude.eaglercraft.mod;

import net.lax1dude.eaglercraft.mod.api.Mod;
import net.lax1dude.eaglercraft.mod.api.ModLogger;
import net.lax1dude.eaglercraft.mod.api.ModMetadata;

public class TestMod implements Mod {

    private final ModLogger logger = new ModLogger("TestMod");

    @Override
    public ModMetadata getModMetadata() {
        return new ModMetadata.Builder("Test Mod", "1.0.0", "Eaglercraft Team")
            .description("A test mod to demonstrate the modding API. Logs lifecycle events and serves as a reference implementation.")
            .documentationPage("Overview")
            .documentationPage("API Reference")
            .documentationPage("Events")
            .build();
    }

    @Override
    public void onInitialize() {
        logger.info("Test Mod initializing...");
    }

    @Override
    public void onGameStart() {
        logger.info("Game started!");
    }

    @Override
    public void onGameShutdown() {
        logger.info("Game shutting down!");
    }

    @Override
    public void onWorldLoad() {
        logger.info("World loaded!");
    }

    @Override
    public void onWorldUnload() {
        logger.info("World unloaded!");
    }

    @Override
    public void onTickInGame() {
    }

    @Override
    public void onTickInGui() {
    }

    @Override
    public String getDocumentationContent(String page) {
        switch (page) {
            case "Overview":
                return "§6§lTest Mod v1.0.0§r\n\n"
                    + "§7Author:§r Eaglercraft Team\n\n"
                    + "This is a sample mod that demonstrates the Eaglercraft modding API.\n"
                    + "It registers lifecycle hooks and logs messages to the console.\n\n"
                    + "§6Features:§r\n"
                    + "§7-§r Lifecycle event logging\n"
                    + "§7-§r World load/unload tracking\n"
                    + "§7-§r Reference for mod developers\n\n"
                    + "Use this mod as a template when creating your own mods.";

            case "API Reference":
                return "§6§lMod API Reference§r\n\n"
                    + "§eMod interface:§r\n"
                    + "  Implement this to create a mod.\n\n"
                    + "§eModMetadata:§r\n"
                    + "  Name, version, author, description,\n"
                    + "  dependencies, documentation pages.\n\n"
                    + "§eModLogger:§r\n"
                    + "  Scoped logging per mod.\n\n"
                    + "§eModConfig:§r\n"
                    + "  Persistent key-value config.\n\n"
                    + "§eModEventManager:§r\n"
                    + "  Fire and listen to game events.\n\n"
                    + "§eKey lifecycle methods:§r\n"
                    + "  onInitialize, onGameStart,\n"
                    + "  onGameShutdown, onWorldLoad,\n"
                    + "  onWorldUnload, onTickInGame,\n"
                    + "  onTickInGui, onActionPerformed";

            case "Events":
                return "§6§lEvent System§r\n\n"
                    + "Mods receive events through the\n"
                    + "ModEventManager system.\n\n"
                    + "§eAvailable events:§r\n"
                    + "§7-§r MODS_LOADED\n"
                    + "§7-§r GAME_STARTED\n"
                    + "§7-§r GAME_SHUTDOWN\n"
                    + "§7-§r WORLD_LOADED\n"
                    + "§7-§r WORLD_UNLOADED\n"
                    + "§7-§r TICK_IN_GAME\n"
                    + "§7-§r TICK_IN_GUI\n"
                    + "§7-§r CHAT_MESSAGE\n"
                    + "§7-§r PLAYER_JOINED\n"
                    + "§7-§r PLAYER_LEFT\n\n"
                    + "Listeners are registered automatically\n"
                    + "when a mod is loaded via ModLoader.";

            default:
                return "§cDocumentation not found: " + page;
        }
    }
}
