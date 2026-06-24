package net.lax1dude.eaglercraft.mod.api.events;

public class ModEventManager {
    private static final ModEventManager instance = new ModEventManager();
    
    public static ModEventManager getInstance() {
        return instance;
    }
    
    public void fireEvent(ModEvent event) {
        // In a real implementation, you would notify all registered mod listeners
        // For now, just print for testing
        System.out.println("[MOD] Event fired: " + event.getEventName() + " at " + event.getTimestamp());
    }
}
