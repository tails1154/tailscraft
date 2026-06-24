package net.lax1dude.eaglercraft.mod.api.events;

public class ModEvent {
    private final long timestamp;
    private final String eventName;
    
    public ModEvent(String eventName) {
        this.timestamp = System.currentTimeMillis();
        this.eventName = eventName;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public String getEventName() {
        return eventName;
    }
}
