# Eaglercraft 1.12 — Agent Guide

Minecraft 1.12.2 ported to run in browsers (via TeaVM JS or WASM GC) and natively (via LWJGL). Gradle Java project, no Node.js/package.json.

## Build targets

| Target | Directory | Toolchain | Main command |
|--------|-----------|-----------|--------------|
| Web JS client | root | TeaVM 0.9.2 JS | `./CompileJS.sh` (runs `./gradlew generateJavascript`) |
| Web WASM GC client | `wasm_gc_teavm/` | TeaVM 0.11.0-EAGLER-R1 fork, Gradle 8.5 | `cd wasm_gc_teavm && ./CompileWASM.sh` (runs `./gradlew generateWasmGC`) |
| Desktop LWJGL | root | native Java | `./gradlew runclient` |

**Build order matters**: always run `./CompileEPK.sh` first (packs assets), then compile, then optionally bundle via `MakeOfflineDownload.sh` or `MakeWASMClientBundle.sh`.

## Key facts

- **Java 17+** required for compiling. Java 8+ for desktop runtime.
- **Gradle wrapper**: root uses Gradle 8.0 (`-Xmx2G -Xms2G`), `wasm_gc_teavm/` uses Gradle 8.5 (`-Xmx4G -Xms4G`). Never assume the other's wrapper works.
- **Java source sets** differ per target — code shared via relative `../src/` paths (main/java, game/java, protocol-game/java plus a platform-specific dir).
- **No tests, no linters, no formatters, no typecheck, no CI**. The only "test" file is `TestMod.java` (a sample mod, not a test).
- Desktop runtime on Linux needs `desktopRuntime/` on `LD_LIBRARY_PATH`.

## WASM GC quirks (from wasm_gc_teavm/README.md)

- Disable VSync? Don't — it causes bad input lag. Force VSync on.
- No `@Async` or JS callbacks (`addEventListener`, etc.) in Java. Implement async in `src/wasm-gc-teavm/js/` via JSPI or polling queues.
- Functions imported via `@Import` don't catch exceptions — wrap in JSO if needed.
- TeaVM WASM GC may generate broken code with nested try/finally in try/catch.

## Source layout

| Source set dir | Content |
|---|---|
| `src/main/java/` | Core game, platform abstraction interfaces, mod API |
| `src/game/java/` | Minecraft vanilla game code |
| `src/protocol-game/java/` | Multiplayer/networking game code |
| `src/lwjgl/java/` | Desktop LWJGL implementation |
| `src/teavm/java/` | TeaVM JS platform implementation |
| `src/wasm-gc-teavm/java/` | WASM GC platform Java implementation |
| `src/wasm-gc-teavm/js/` | WASM GC JS glue code (21 files) |
| `src/wasm-gc-teavm-loader/c/` | C loader for EPW asset unpacking (needs emscripten) |

## Entrypoints

- **Web JS**: `net.lax1dude.eaglercraft.internal.teavm.MainClass`
- **Web WASM GC**: `net.lax1dude.eaglercraft.internal.wasm_gc_teavm.MainClass`
- **Desktop**: `net.lax1dude.eaglercraft.internal.MainClass` (LWJGL)

## Mod system

API in `net.lax1dude.eaglercraft.mod`. Mods implement `Mod` with metadata, lifecycle hooks (onInitialize, onTickInGame, etc.), and documentation pages. Integrated into game loop via `ModLoader` — loads at splash screen, ticks each frame, dispatches GUI actions.

### Access points
- **Options > Mods** (GuiOptions.java:99)
- **Pause Menu > Mods** (GuiIngameMenu.java:85)
- In-game Mods menu shows list → click Info → scrollable docs with Prev/Next

### Key classes
| Class | Location |
|---|---|
| `Mod` interface | `src/main/java/.../mod/api/Mod.java` |
| `ModMetadata.Builder` | `src/main/java/.../mod/api/ModMetadata.java` |
| `ModLoader` | `src/main/java/.../mod/loader/ModLoader.java` |
| `ModEventManager` | `src/main/java/.../mod/api/events/ModEventManager.java` |
| `GuiModsMenu` | `src/game/java/.../mod/gui/GuiModsMenu.java` |
| `GuiModInfo` | `src/game/java/.../mod/gui/GuiModInfo.java` |

### Registering a built-in mod
Add to `ModLoader.registerBuiltInMods()`:
```java
registerMod(new MyMod());
```

### Documentation content
Use `§` color codes for rich text. See `TestMod` or `FlyMod` for examples. Full docs at `ModLoaderDocumentation.md`.
