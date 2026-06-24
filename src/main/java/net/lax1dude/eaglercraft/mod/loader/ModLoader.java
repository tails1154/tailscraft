package net.lax1dude.eaglercraft.mod.loader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.lax1dude.eaglercraft.mod.api.Mod;
import net.lax1dude.eaglercraft.mod.api.ModMetadata;
import net.lax1dude.eaglercraft.mod.api.events.ModEventManager;
import net.lax1dude.eaglercraft.mod.api.events.ModEventType;

public class ModLoader {

    private static final List<Mod> loadedMods = new ArrayList<>();
    private static boolean modsEnabled = true;
    private static boolean initialized = false;

    public static void loadMods() {
        if (initialized) return;
        initialized = true;
        loadedMods.clear();
        registerBuiltInMods();
        ModEventManager.getInstance().fireEvent(ModEventType.MODS_LOADED);
    }

    private static void registerBuiltInMods() {
        registerMod(new net.lax1dude.eaglercraft.mod.TestMod());
        registerMod(new net.lax1dude.eaglercraft.mod.FlyMod());
    }

    public static void registerMod(Mod mod) {
        loadedMods.add(mod);
        mod.onInitialize();
        for (ModEventType type : ModEventType.values()) {
            ModEventManager.getInstance().registerListener(type, mod);
        }
        getLogger().info("Loaded mod: " + mod.getName() + " v" + mod.getVersion());
        String desc = mod.getDescription();
        if (!desc.isEmpty()) {
            getLogger().info("  Description: " + desc);
        }
    }

    private static ModLogger getLogger() {
        return new ModLogger("ModLoader");
    }

    public static List<Mod> getLoadedMods() {
        return Collections.unmodifiableList(loadedMods);
    }

    public static Mod getModByName(String name) {
        for (Mod mod : loadedMods) {
            if (mod.getName().equals(name)) {
                return mod;
            }
        }
        return null;
    }

    public static boolean isModsEnabled() {
        return modsEnabled;
    }

    public static void setModsEnabled(boolean enabled) {
        modsEnabled = enabled;
    }

    public static void onGameStart() {
        if (!modsEnabled) return;
        ModEventManager.getInstance().fireEvent(ModEventType.GAME_STARTED);
    }

    public static void onGameShutdown() {
        if (!modsEnabled) return;
        ModEventManager.getInstance().fireEvent(ModEventType.GAME_SHUTDOWN);
    }

    public static void onWorldLoad() {
        if (!modsEnabled) return;
        ModEventManager.getInstance().fireEvent(ModEventType.WORLD_LOADED);
    }

    public static void onWorldUnload() {
        if (!modsEnabled) return;
        ModEventManager.getInstance().fireEvent(ModEventType.WORLD_UNLOADED);
    }

    public static void onTickInGame() {
        if (!modsEnabled) return;
        ModEventManager.getInstance().fireEvent(ModEventType.TICK_IN_GAME);
    }

    public static void onTickInGui() {
        if (!modsEnabled) return;
        ModEventManager.getInstance().fireEvent(ModEventType.TICK_IN_GUI);
    }

    public static void onActionPerformed(net.minecraft.client.gui.GuiButton button) {
        if (!modsEnabled) return;
        for (Mod mod : loadedMods) {
            try {
                mod.onActionPerformed(button);
            } catch (Exception e) {
                System.err.println("[ModLoader] " + mod.getName() + " threw exception in onActionPerformed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public static Mod findMod(String name) {
        for (Mod mod : loadedMods) {
            if (mod.getName().equalsIgnoreCase(name)) {
                return mod;
            }
        }
        return null;
    }

    private static class ModLogger {
        private final String prefix;
        ModLogger(String prefix) { this.prefix = prefix; }
        void info(String msg) { System.out.println("[" + prefix + "] " + msg); }
    }
}
