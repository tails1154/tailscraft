# TailsCraft

TailsCraft is a modified Eaglercraft 1.12.2 client built for the WASM-GC runtime. It includes TailsConnect multiplayer features, TailsCraft friends, hosted-world support, AuthMe helpers, client quality-of-life improvements, and a GitHub-backed in-game changelog.

## Requirements

- Java 21 is recommended for the WASM-GC build on this project.
- Java 8 or newer is required by the desktop runtime.
- A POSIX shell is required by the build and deployment scripts.

## Build and deploy

The normal deployment target is `~/ecraft/gaming/ecraft`:

```sh
./deploy.sh
```

`deploy.sh` builds the WASM-GC client, runtime JavaScript, EPK, and EPW bundle. It creates a timestamped backup of the target, then atomically replaces `classes.wasm`, `eagruntime.js`, `assets.epk`, and `assets.epw`.

To deploy only rebuilt assets:

```sh
./deployEPKOnly.sh
```

This rebuilds and deploys only `assets.epk`, preserving the existing WASM and runtime files. Both scripts accept an optional deployment directory:

```sh
./deploy.sh /path/to/ecraft
./deployEPKOnly.sh /path/to/ecraft
```

For individual build stages, use the scripts in `wasm_gc_teavm/`: `CompileWASM.sh`, `CompileEagRuntimeJS.sh`, `CompileEPK.sh`, `CompileBootstrapJS.sh`, `CompileLoaderWASM.sh`, and `MakeWASMClientBundle.sh`.

## Development

The Java sources are compiled with Gradle. Most Java IDEs can import the repository as a Gradle project; IntelliJ is preferred over the legacy Eclipse setup. The desktop runtime can be started with:

```sh
cd desktopRuntime
./gradlew runclient
```

On Linux, the desktop runtime may require `desktopRuntime` in `LD_LIBRARY_PATH`; the included launch configuration normally handles this.

## Multiplayer

Native Eaglercraft servers are normally connected through EaglerXBungee or EaglerXVelocity. The client’s TailsConnect and friends features use the TailsConnect service and remain separate from ordinary Eagler server connections. See [`CHANGELOG.md`](CHANGELOG.md) for the current feature history.

## Repository links

- Project: https://github.com/tails1154/tailscraft
- EaglerXBungee: https://github.com/lax1dude/eagl3rxbungee
- EaglerXVelocity: https://github.com/lax1dude/eagl3rxbungee

The upstream Eaglercraft licensing terms remain applicable; see [`LICENSE`](LICENSE).
