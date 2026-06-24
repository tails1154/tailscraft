package net.lax1dude.eaglercraft.mod.api.events;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModEventManager {

    private static final ModEventManager instance = new ModEventManager();

    private final Map<ModEventType, List<ModEventListener>> listeners = new EnumMap<>(ModEventType.class);

    public static ModEventManager getInstance() {
        return instance;
    }

    public void registerListener(ModEventType type, ModEventListener listener) {
        listeners.computeIfAbsent(type, k -> new ArrayList<>()).add(listener);
    }

    public void unregisterListener(ModEventType type, ModEventListener listener) {
        List<ModEventListener> list = listeners.get(type);
        if (list != null) {
            list.remove(listener);
            if (list.isEmpty()) {
                listeners.remove(type);
            }
        }
    }

    public void unregisterAll(ModEventListener listener) {
        for (List<ModEventListener> list : listeners.values()) {
            list.remove(listener);
        }
    }

    public void fireEvent(ModEvent event) {
        List<ModEventListener> list = listeners.get(event.getType());
        if (list != null) {
            for (ModEventListener listener : list) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    System.err.println("[ModEventManager] Listener threw exception on event " + event.getType() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }

    public void fireEvent(ModEventType type) {
        fireEvent(new ModEvent(type));
    }

    public void fireEvent(ModEventType type, Object data) {
        fireEvent(new ModEvent(type, data));
    }
}
