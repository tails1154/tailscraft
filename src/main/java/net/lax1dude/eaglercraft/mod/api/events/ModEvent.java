package net.lax1dude.eaglercraft.mod.api.events;

public class ModEvent {

    private final ModEventType type;
    private final long timestamp;
    private final Object data;

    public ModEvent(ModEventType type) {
        this(type, null);
    }

    public ModEvent(ModEventType type, Object data) {
        this.type = type;
        this.timestamp = System.currentTimeMillis();
        this.data = data;
    }

    public ModEventType getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @SuppressWarnings("unchecked")
    public <T> T getData() {
        return (T) data;
    }

    @Override
    public String toString() {
        return "ModEvent{type=" + type + "}";
    }
}
