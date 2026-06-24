package net.lax1dude.eaglercraft.mod.api;

import java.util.HashMap;
import java.util.Map;

public class ModConfig {

    private final String modName;
    private final Map<String, String> entries = new HashMap<>();

    public ModConfig(String modName) {
        this.modName = modName;
    }

    public String getString(String key, String defaultValue) {
        return entries.getOrDefault(key, defaultValue);
    }

    public void setString(String key, String value) {
        entries.put(key, value);
    }

    public int getInt(String key, int defaultValue) {
        String val = entries.get(key);
        if (val != null) {
            try {
                return Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    public void setInt(String key, int value) {
        entries.put(key, Integer.toString(value));
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = entries.get(key);
        if (val != null) {
            return Boolean.parseBoolean(val);
        }
        return defaultValue;
    }

    public void setBoolean(String key, boolean value) {
        entries.put(key, Boolean.toString(value));
    }

    public boolean hasKey(String key) {
        return entries.containsKey(key);
    }

    public Map<String, String> getAll() {
        return new HashMap<>(entries);
    }

    @Override
    public String toString() {
        return "ModConfig{" + modName + ": " + entries.size() + " entries}";
    }
}
