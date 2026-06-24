# EaglercraftX Mod System

## Overview

This document provides comprehensive documentation for the EaglercraftX mod system. The mod system allows developers to create extensions and modifications to the base game through a clean, event-driven API.

## Quick Start

1. Create a new class that implements `net.lax1dude.eaglercraft.mod.api.Mod`
2. Implement required methods: `getName()`, `getVersion()`, `getAuthor()`
3. Add optional event handlers for game lifecycle
4. Place your mod class in the classpath
5. Mods will be automatically loaded and initialized

## API Reference

### Core Interface

```java
package net.lax1dude.eaglercraft.mod.api;

public interface Mod {
    // Required methods
    String getName();           // Return the mod name
    String getVersion();        // Return the mod version  
    String getAuthor();         // Return the mod author
    
    // Optional event handlers
    default void onInitialize() {}      // Called when mod is loaded
    default void onGameStart() {}       // Called when game starts
    default void onGameShutdown() {}    // Called when game shuts down
    default void onTickInGame() {}      // Called each game tick
    default void onTickInGui() {}       // Called each GUI tick
}
```

### Event System

```java
package net.lax1dude.eaglercraft.mod.api.events;

public class ModEventManager {
    public static ModEventManager getInstance();
    
    public void fireEvent(ModEvent event);
}

public class ModEvent {
    public ModEvent(String eventName);
    public String getEventName();
    public long getTimestamp();
}
```

### Mod Loader

```java
package net.lax1dude.eaglercraft.mod.loader;

public class ModLoader {
    public static void loadMods();                    // Load all mods
    public static void setModsEnabled(boolean enabled); // Enable/disable mods
    public static boolean isModsEnabled();             // Check if mods are enabled
    public static List<Mod> getLoadedMods();           // Get list of loaded mods
    public static void onGameStart();                  // Call when game starts
    public static void onGameShutdown();               // Call when game shuts down
}
```

## Development Guide

### Creating a Mod

To create a mod, extend the `Mod` interface:

```java
package com.mycompany.mymod;

import net.lax1dude.eaglercraft.mod.api.Mod;

public class MyMod implements Mod {
    @Override
    public String getName() {
        return "My Awesome Mod";
    }
    
    @Override
    public String getVersion() {
        return "1.0.0";
    }
    
    @Override
    public String getAuthor() {
        return "Your Name";
    }
    
    @Override
    public void onInitialize() {
        System.out.println("[MyMod] Initializing...");
        // Initialization code
    }
    
    @Override
    public void onGameStart() {
        System.out.println("[MyMod] Game started!");
        // Game start code
    }
}
```

### Mod Features

#### Event Handling

The mod system provides a simple event system for mods to communicate:

```java
import net.lax1dude.eaglercraft.mod.api.events.ModEventManager;
import net.lax1dude.eaglercraft.mod.api.events.ModEvent;

// Fire a custom event
ModEventManager.getInstance().fireEvent(
    new ModEvent("my_custom_event")
);
```

#### Mod Configuration

You can store mod-specific data:

```java
public class MyMod implements Mod {
    private boolean enabled = true;
    private String configFile = "my_mod_config.txt";
    
    @Override
    public void onInitialize() {
        // Load configuration from file
        loadConfig();
    }
    
    private void loadConfig() {
        // Implementation would go here
    }
}
```

### Mod Lifecycle

1. **Initialization**: `onInitialize()` is called when the mod is loaded
2. **Game Start**: `onGameStart()` is called when the game begins
3. **Game Tick**: `onTickInGame()` is called each game tick
4. **GUI Tick**: `onTickInGui()` is called each GUI tick
5. **Game Shutdown**: `onGameShutdown()` is called when the game exits

## Mod Integration

### Options Menu

The mod system integrates with the game's options menu:

- **Mods Button**: Located in the options menu at position (x=2, y=height/6+120-6)
- **Mods Enabled Button**: Located at position (x=-155, y=height/6+120-6)

Clicking the "Mods Enabled" button toggles all mods on or off.

### Integration Points

1. **Client Platform**: Mod loader is integrated with the client's main loop
2. **Event System**: Mods can listen for and respond to game events
3. **Configuration**: Mod settings can be stored in game configuration files

## Mod Development Best Practices

### Thread Safety

Mods should be aware that they may be called from different threads:

```java
@Override
public void onTickInGame() {
    // This may be called from different threads
    // Ensure thread-safe operations
    synchronized(this) {
        // Thread-safe code
    }
}
```

### Resource Management

Properly manage resources to avoid memory leaks:

```java
public class MyMod implements Mod {
    private List<Resource> resources = new ArrayList<>();
    
    @Override
    public void onInitialize() {
        // Load resources
        resources.add(new Resource("example.txt"));
    }
    
    @Override
    public void onGameShutdown() {
        // Clean up resources
        for (Resource r : resources) {
            r.cleanup();
        }
        resources.clear();
    }
}
```

### Error Handling

Always handle errors gracefully:

```java
@Override
public void onTickInGame() {
    try {
        // Mod logic here
    } catch (Exception e) {
        System.err.println("[MyMod] Error in tick handler: " + e.getMessage());
        e.printStackTrace();
    }
}
```

## Testing Mods

### Testing Framework

Create test mods in the test directory:

```java
package net.lax1dude.eaglercraft.mod;

import net.lax1dude.eaglercraft.mod.api.Mod;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestMod implements Mod {
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
        return "Test Author";
    }
    
    @Test
    public void testModName() {
        assertEquals("Test Mod", getName());
    }
    
    @Test
    public void testModVersion() {
        assertEquals("1.0.0", getVersion());
    }
}
```

### Running Tests

```bash
# Navigate to the project root
./gradlew test
```

## Mod Distribution

### Mod Formats

Mods can be distributed as:

- **JAR Files**: Recommended for distribution
- **Class Files**: For development and testing
- **Zip Files**: Alternative distribution format

### Installation

1. Create a `mods` directory in the game root
2. Place your mod `.class` or `.jar` files in the `mods` directory
3. Restart the game
4. Mods will be automatically loaded and initialized

## Advanced Topics

### Custom Events

Create custom events for mod communication:

```java
public class CustomEvent extends ModEvent {
    private String data;
    
    public CustomEvent(String eventName, String data) {
        super(eventName);
        this.data = data;
    }
    
    public String getData() {
        return data;
    }
}
```

### Mod Dependencies

Implement dependency checking:

```java
public class DependencyMod implements Mod {
    private boolean hasDependency = true;
    
    @Override
    public void onInitialize() {
        if (!hasDependency) {
            System.out.println("[DependencyMod] Required dependency not found. Disabling.");
            // Disable mod
            return;
        }
    }
}
```

### Performance Optimization

For performance-critical mods:

1. Cache frequently accessed data
2. Minimize object creation
3. Use efficient algorithms
4. Profile mod performance regularly

## Troubleshooting

### Common Issues

1. **Mod Not Loading**
   - Ensure mod is in classpath
   - Verify mod implements `Mod` interface
   - Check for compilation errors

2. **Mod Crashing**
   - Wrap mod code in try-catch blocks
   - Test mod in isolation
   - Check for null pointer exceptions

3. **Mod Performance Issues**
   - Profile mod execution
   - Reduce object allocation
   - Optimize loops and algorithms

4. **Mod Integration Issues**
   - Check mod event handlers
   - Verify mod lifecycle callbacks
   - Debug mod initialization

### Debugging

Enable mod debugging:

```java
public class DebugMod implements Mod {
    @Override
    public void onInitialize() {
        System.out.println("[DebugMod] Mod initialized");
        // Enable debugging output
    }
    
    @Override
    public void onTickInGame() {
        // Debug output each tick
        System.out.println("[DebugMod] Tick");
    }
}
```

## Future Enhancements

The mod system will be enhanced with:

1. **Mod Configuration GUI**: In-game configuration interface
2. **Mod Dependencies**: Automatic dependency resolution
3. **Mod Updates**: Update checking and installation
4. **Mod Permissions**: Role-based access control
5. **Mod Logging**: Advanced logging system
6. **Mod Profiling**: Performance monitoring tools

## Version History

- **1.0.0**: Initial release with basic mod API and loader
- **1.1.0**: Enhanced event system and mod configuration
- **1.2.0**: Added mod integration with options menu

## License

The EaglercraftX Mod System is licensed under the same terms as the main EaglercraftX software.

For more information, visit the EaglercraftX documentation website or contact the development team.
