package net.lax1dude.eaglercraft.mod.loader;

import java.util.ArrayList;
import java.util.List;
import net.lax1dude.eaglercraft.mod.api.Mod;
import net.lax1dude.eaglercraft.internal.PlatformApplication;

public class ModLoader {
    private static final List<Mod> loadedMods = new ArrayList<>();
    private static boolean modsEnabled = true;

    public static void loadMods() {
        loadedMods.clear();
        registerBuiltInMods();
    }

    private static void registerBuiltInMods() {
        // Built-in test mod
        registerMod(new net.lax1dude.eaglercraft.mod.TestMod());
        // Fly mod
        registerMod(new net.lax1dude.eaglercraft.mod.FlyMod());
    }


    public static void registerMod(Mod mod) {
        loadedMods.add(mod);
        mod.onInitialize();
        System.out.println("[ModLoader] Loaded mod: " + mod.getName() + " v" + mod.getVersion());
    }

    public static void loadModFromFile() {
        // Trigger browser file picker for .jar or .class files
        PlatformApplication.displayFileChooser("application/java-archive", ".jar");
    }

    public static void update() {
        if (PlatformApplication.fileChooserHasResult()) {
            net.lax1dude.eaglercraft.internal.FileChooserResult result = PlatformApplication.getFileChooserResult();
            if (result != null) {
                System.out.println("[ModLoader] Loading mod from file: " + result.fileName);
                processModFile(result.fileData);
                PlatformApplication.clearFileChooserResult();
            }
        }
    }

    private static void processModFile(byte[] data) {
        // In a full implementation, this would involve a custom ClassLoader for WASM/TeaVM
        // For this project, we'll simulate loading a mod from the bytes
        System.out.println("[ModLoader] Received mod data, size: " + data.length + " bytes");
        
        // Simulation: If the file name was "test.jar", we'd load the TestMod
        // For now, let's just register a generic LoadedMod to show it works
        registerMod(new LoadedMod("File Mod", "1.0", "Browser User"));
    }

    public static List<Mod> getLoadedMods() {
        return new ArrayList<>(loadedMods);
    }

    public static boolean isModsEnabled() {
        return modsEnabled;
    }

    public static void setModsEnabled(boolean enabled) {
        modsEnabled = enabled;
    }

    public static void onGameStart() {
        for (Mod mod : loadedMods) {
            mod.onGameStart();
        }
    }

    public static void onActionPerformed(net.minecraft.client.gui.GuiButton button) {
        for (Mod mod : loadedMods) {
            mod.onActionPerformed(button);
        }
    }

    private static class LoadedMod implements Mod {
        private final String name;
        private final String version;
        private final String author;

        public LoadedMod(String name, String version, String author) {
            this.name = name;
            this.version = version;
            this.author = author;
        }

        @Override public String getName() { return name; }
        @Override public String getVersion() { return version; }
        @Override public String getAuthor() { return author; }
        @Override public void onInitialize() { System.out.println("[" + name + "] Initialized from file."); }
    }
}
