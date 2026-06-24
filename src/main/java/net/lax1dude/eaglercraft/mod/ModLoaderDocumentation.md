# Eaglercraft Mod System Documentation

## Overview

The Eaglercraft mod system provides a flexible API for developing mods that extend the base game. Mods are written in Java, implement the `Mod` interface, and are loaded at game startup.

## Quick Start

1. Create a class implementing `net.lax1dude.eaglercraft.mod.api.Mod`
2. Implement `getModMetadata()` returning name, version, author, description
3. Override lifecycle hooks (onInitialize, onTickInGame, etc.)
4. Optionally provide documentation pages via `getDocumentationContent(page)`
5. Register your mod in `ModLoader.registerBuiltInMods()`
6. Access from in-game via Options > Mods or Pause Menu > Mods

## Core API

### `net.lax1dude.eaglercraft.mod.api.Mod`

```java
public interface Mod extends ModEventListener {
    ModMetadata getModMetadata();
    default String getName() { return getModMetadata().getName(); }
    default String getVersion() { return getModMetadata().getVersion(); }
    default String getAuthor() { return getModMetadata().getAuthor(); }
    default String getDescription() { return getModMetadata().getDescription(); }

    // Lifecycle hooks
    default void onInitialize() {}
    default void onGameStart() {}
    default void onGameShutdown() {}
    default void onWorldLoad() {}
    default void onWorldUnload() {}
    default void onTickInGame() {}
    default void onTickInGui() {}
    default void onActionPerformed(GuiButton button) {}

    // Documentation (displayed in the in-game Mods menu)
    default List<String> getDocumentationPages() { return getModMetadata().getDocumentationPages(); }
    default String getDocumentationContent(String page) { return ""; }
}
```

### `net.lax1dude.eaglercraft.mod.api.ModMetadata`

Metadata builder:
```java
new ModMetadata.Builder("My Mod", "1.0.0", "Author")
    .description("What my mod does")
    .documentationPage("Usage")
    .documentationPage("Configuration")
    .dependency("Other Mod")
    .build();
```

### `net.lax1dude.eaglercraft.mod.api.ModLogger`

Scoped logger per mod:
```java
private final ModLogger logger = new ModLogger("MyMod");
logger.info("Hello!");
logger.warn("Something odd");
logger.error("Failed!", exception);
```

### `net.lax1dude.eaglercraft.mod.api.ModConfig`

Persistent key-value config:
```java
ModConfig config = getConfig();
config.setBoolean("enabled", true);
boolean val = config.getBoolean("enabled", false);
config.setString("key", "value");
config.setInt("count", 42);
```

### `net.lax1dude.eaglercraft.mod.api.events.ModEventManager`

Fire and listen to typed events:
```java
ModEventManager.getInstance().fireEvent(ModEventType.GAME_STARTED);
ModEventManager.getInstance().fireEvent(ModEventType.CHAT_MESSAGE, chatMessage);
```

Event types: `MODS_LOADED`, `GAME_STARTED`, `GAME_SHUTDOWN`, `WORLD_LOADED`, `WORLD_UNLOADED`, `TICK_IN_GAME`, `TICK_IN_GUI`, `CHAT_MESSAGE`, `PLAYER_JOINED`, `PLAYER_LEFT`.

## In-Game Mods Menu

- **Access**: Options > Mods, or Pause Menu > Mods
- **Features**: List all mods, enable/disable all mods, view mod details
- **Documentation**: Click "Info" on a mod, then browse documentation pages with Prev/Next
- Mod details show name, version, author, description, and scrollable documentation

## Creating a Mod

```java
package com.example;

import net.lax1dude.eaglercraft.mod.api.Mod;
import net.lax1dude.eaglercraft.mod.api.ModLogger;
import net.lax1dude.eaglercraft.mod.api.ModMetadata;
import net.minecraft.client.gui.GuiButton;

public class MyMod implements Mod {
    private final ModLogger logger = new ModLogger("MyMod");

    @Override
    public ModMetadata getModMetadata() {
        return new ModMetadata.Builder("My Mod", "1.0.0", "Me")
            .description("An example mod")
            .documentationPage("Overview")
            .build();
    }

    @Override
    public void onInitialize() {
        logger.info("MyMod loaded!");
    }

    @Override
    public void onTickInGame() {
        // Runs every game tick
    }

    @Override
    public String getDocumentationContent(String page) {
        return "§6§lMy Mod§r\n\nThis is the documentation shown\nin the in-game mods menu.\n\nUse §e§ for color codes.";
    }
}
```

Register your mod in `net.lax1dude.eaglercraft.mod.loader.ModLoader.registerBuiltInMods()`.

## Mod Lifecycle

1. **Game startup**: `ModLoader.loadMods()` → `onInitialize()` on each mod
2. **Game starts** (world enters): `onGameStart()`
3. **World loaded**: `onWorldLoad()`
4. **Each tick** (game unpaused): `onTickInGame()`
5. **Each tick** (GUI open): `onTickInGui()`
6. **Button click**: `onActionPerformed(GuiButton)` — when a mod-owned button is clicked
7. **World unloaded**: `onWorldUnload()`
8. **Game shutdown**: `onGameShutdown()`

## Mod Distribution

Built-in mods register via `ModLoader.registerBuiltInMods()`. External mod loading via file picker is supported through `ModLoader.loadModFromFile()` and `ModLoader.update()` (classloader integration planned).

## Best Practices

- Use `ModLogger` instead of `System.out`
- Keep tick handlers lightweight
- Check `ModLoader.isModsEnabled()` before custom behavior
- Wrap button injection in try/catch for cross-version compatibility
- Use `§` color codes in documentation content for rich text
