# EaglercraftX Mod System Documentation

## Overview

The EaglercraftX mod system provides a flexible API for developing and loading mods that extend the base game functionality. This documentation covers the core components of the mod system, including the Mod Loader, API interfaces, and development guidelines.

## Mod Loader

The `ModLoader` class is the central component of the mod system. It manages mod lifecycle events and provides hooks for mod initialization and execution.

### Key Features

- **Mod Registration**: Automatically detect and load mod classes
- **Lifecycle Management**: Handle mod initialization, game start, and shutdown events
- **Event System**: Provide a simple event-driven architecture for mod communication
- **Modular Design**: Support for multiple mods with independent execution

### Usage

```java
import net.lax1dude.eaglercraft.mod.loader.ModLoader;

// Load all available mods
ModLoader.loadMods();

// Check if mods are enabled
boolean modsEnabled = ModLoader.isModsEnabled();

// Enable or disable mods
ModLoader.setModsEnabled(true);
```

## Mod API

The mod API defines the interface that all mods must implement to be compatible with the EaglercraftX mod system.

### Core Interfaces

#### `net.lax1dude.eaglercraft.mod.api.Mod`

The main interface that all mods must implement:

```java
public interface Mod {
    String getName();           // Mod name
    String getVersion();        // Mod version
    String getAuthor();         // Mod author
    
    default void onInitialize() {}      // Called when mod is loaded
    default void onGameStart() {}       // Called when game starts
    default void onGameShutdown() {}    // Called when game shuts down
    default void onTickInGame() {}      // Called each game tick
    default void onTickInGui() {}       // Called each GUI tick
}
```

#### `net.lax1dude.eaglercraft.mod.api.events.ModEventManager`

Provides event-driven architecture for mod communication:

```java
import net.lax1dude.eaglercraft.mod.api.events.ModEventManager;
import net.lax1dude.eaglercraft.mod.api.events.ModEvent;

// Fire an event
ModEventManager.getInstance().fireEvent(new ModEvent("test_event"));
```

## Creating a Mod

### Basic Structure

A mod consists of:

1. **Mod Class**: Implements the `Mod` interface
2. **Mod Metadata**: Name, version, and author information
3. **Event Handlers**: Optional methods for handling game events

### Example Mod Implementation

```java
package net.lax1dude.eaglercraft.mod;

import net.lax1dude.eaglercraft.mod.api.Mod;

public class ExampleMod implements Mod {
    @Override
    public String getName() {
        return "Example Mod";
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
        System.out.println("[ExampleMod] Mod loaded successfully!");
        // Initialization code here
    }
    
    @Override
    public void onGameStart() {
        System.out.println("[ExampleMod] Game started!");
        // Game start code here
    }
    
    @Override
    public void onTickInGame() {
        // Update code here
    }
}
```

## Mod Integration

### Options Menu

The mod system integrates with the main options menu, providing:

- **Mods Toggle**: Enable/disable all mods
- **Mod Status**: Visual indicator of mod status

To access mod settings, open the game options menu and look for the "Mods" and "Mods Enabled" buttons.

### Mod Loading Process

1. **Mod Discovery**: The ModLoader scans for mod classes
2. **Initialization**: Mods are initialized in the order they are discovered
3. **Event Registration**: Mods can register for custom events
4. **Game Lifecycle**: Mods receive callbacks for game events

## Mod Development Guidelines

### Best Practices

1. **Thread Safety**: Ensure mod code is thread-safe when accessing game state
2. **Performance**: Avoid heavy computations in tick handlers
3. **Compatibility**: Check game version before modifying game behavior
4. **Error Handling**: Wrap mod code in try-catch blocks to prevent crashes

### Common Pitfalls

- **Memory Leaks**: Don't store references to game objects indefinitely
- **Concurrent Modification**: Avoid modifying collections during iteration
- **Resource Management**: Properly release resources when mod is unloaded
- **State Management**: Maintain mod state between ticks

## Mod Event System

The mod event system provides a simple way for mods to communicate and react to game events:

```java
// Event definitions
public class ModEvent {
    private final long timestamp;
    private final String eventName;
    
    // Event handling methods
}
```

### Built-in Events

- `PLAYER_JOINED`: When a player joins the world
- `PLAYER_LEFT`: When a player leaves the world
- `GAME_STARTED`: When the game begins
- `GAME_SHUTDOWN`: When the game ends
- `TICK`: Game tick event
- `GUI_TICK`: GUI tick event

## Testing Mods

### Unit Testing

Create test mods in the `src/test/java/net/lax1dude/eaglercraft/mod` directory:

```java
package net.lax1dude.eaglercraft.mod;

import net.lax1dude.eaglercraft.mod.api.Mod;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestMod implements Mod {
    @Test
    public void testModInitialization() {
        // Test mod initialization
    }
    
    @Test
    public void testModEvents() {
        // Test mod event handling
    }
}
```

## Mod Distribution

### Mod Formats

Mods can be distributed as:

1. **Class Files**: `.class` files for compiled Java bytecode
2. **JAR Files**: Packaged mods for easy distribution
3. **Zip Files**: Compressed mod packages

### Mod Installation

1. Place mod files in the `mods` directory
2. Restart the game
3. Mods will be automatically loaded

## Troubleshooting

### Common Issues

1. **Mod Not Loading**
   - Check that mod classes implement the `Mod` interface
   - Ensure mod files are in the correct directory
   - Verify mod name and version strings

2. **Mod Catching Errors**
   - Wrap mod code in try-catch blocks
   - Test mod code in isolation
   - Check for null pointer exceptions

3. **Performance Issues**
   - Reduce computation in tick handlers
   - Use efficient data structures
   - Limit event frequency

## Future Enhancements

### Planned Features

1. **Mod Configuration**: Mod-specific settings options
2. **Mod Dependencies**: Support for mod dependencies
3. **Mod Updates**: Automatic mod update checking
4. **Mod Documentation**: In-game mod documentation system
5. **Mod Permissions**: Role-based mod permissions

### External Resources

- **Mod Development Wiki**: Internal documentation for advanced mod features
- **API Reference**: Complete API documentation
- **Sample Mods**: Collection of example mods for reference
- **Community Forum**: Support and discussion forum

## License

The EaglercraftX Mod System is licensed under the same terms as the main EaglercraftX software.

## Version Information

- **Mod System Version**: 1.0.0
- **Minecraft Compatibility**: 1.12.2
- **EaglercraftX Version**: Based on EaglercraftX fork
