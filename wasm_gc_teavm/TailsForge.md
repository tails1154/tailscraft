# TailsForge JavaScript API

TailsForge is the JavaScript mod API for the WASM-GC client. It uses explicit
events and bridges instead of rewriting or patching the `classes.wasm` binary.

## Loading a mod

Add URLs before the runtime starts:

```html
<script>
window.tailsForgeMods = ["mods/example.js"];
</script>
```

Or configure them in `window.eaglercraftXOpts.mods`.

The in-game Options → Mods screen has **Upload .js Mod**. Uploaded scripts are
stored in the client’s namespaced LocalStorage and loaded automatically on the
next game launch.

## Mod format

```js
TailsForge.register({
  id: "hello-mod",
  name: "Hello Mod",
  version: "1.0.0",
  author: "you",
  init(api) {
    api.on("runtime:ready", () => console.log("TailsForge is ready"));
    api.on("wasm:loaded", () => console.log("Minecraft WASM loaded"));
    api.on("frame", event => {
      // Keep work small: this runs once per browser animation frame.
    });
  }
});
```

## API

- `TailsForge.on(event, callback)` registers a listener and returns an unsubscribe function.
- `TailsForge.off(event, callback)` removes a listener.
- `TailsForge.once(event, callback)` listens for one event.
- `TailsForge.emit(event, data)` emits an event.
- `TailsForge.register(mod)` registers a mod descriptor.
- `TailsForge.load(url)` fetches and executes another JavaScript mod asynchronously.
- `TailsForge.storage.get/set/remove(key, value)` provides namespaced local storage.
- `TailsForge.game.loaded` reports whether the WASM module has loaded.

Current events are `runtime:ready`, `wasm:loaded`, `mod:loaded`,
`mod:executed`, and `frame`. Game-specific events and safe game object bridges
will be added explicitly as they are implemented; raw WASM memory patching is
not supported.
