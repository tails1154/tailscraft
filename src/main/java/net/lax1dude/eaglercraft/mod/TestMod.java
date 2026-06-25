package net.lax1dude.eaglercraft.mod;

public class TestMod implements net.lax1dude.eaglercraft.mod.api.Mod {
    @Override
    public String getName() {
        return "Test Mod";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getAuthor() {
        return "Eaglercraft Team";
    }
    
    @Override
    public void onInitialize() {
        System.out.println("[TestMod] Test Mod is initializing...");
    }
    
    @Override
    public void onGameStart() {
        System.out.println("[TestMod] Test Mod is starting!");
    }
    
    @Override
    public void onGameShutdown() {
        System.out.println("[TestMod] Test Mod is shutting down!");
    }
    
    @Override
    public void onTickInGame() {
        // Can add mod functionality here
    }
    
    @Override
    public void onTickInGui() {
        // Can add GUI functionality here
    }
}