# TailsCraft changelog

## AI surroundings chatbot
- Added an in-game AI Chatbot button to the pause menu.
- Uses the asynchronous public Pollinations text endpoint with a local fallback.
- Sends only a small surroundings snapshot; no passwords, inventory, or world files.

## Constrained AI Copilot
- Added an opt-in AI Copilot panel with bounded block-grid pathfinding to XYZ targets.
- Added a look-around action; the copilot cannot chat, attack, place blocks, use inventory, or interact with containers.
- Stop and closing the panel release all copilot movement keys.
- Added AICraftScriptLangPlus AI planning, including natural-language requests, `FIND_BLOCK`, `EXPLORE`, `MINE`, `STOP`, and failure-retry chains.
- Added inventory-aware planning: the AI receives item names and counts, and `SELECT_ITEM` can safely choose a matching existing hotbar item.
- Active copilot actions now show the world behind an input-blocking status overlay with a Stop button.
- Manual copilot targets now default to the player's current position instead of `0,64,0`, preventing immediate completion when the overlay opens.
- Simplified the copilot screen to one task field and essential controls, and added a bounded observe-plan-act agent loop.
- Prevented look-around script steps from falling through into movement on their first tick.
- Added automatic bounded retries for failed server handshakes, with Cancel still available and a clear retry count.
- Added bounded text-only block observations to AI Copilot context, including nearby block IDs, coordinates, and the targeted block; no screenshots are sent.
- Added explicit copilot phase/status reporting and clean restart handling after Stop.

This is a project-history changelog. Entries marked removed or reverted are kept
for context and are not part of the current client.

## Current client

- Added a main-menu Credits button with attribution for bundled music and
  resource-pack content.
- Replaced the `record.chirp` music disc audio and label with “Shiawase VIP
  (Remix)” by razaplays, a remix of Dion Timmer’s “Shiawase VIP.”
  Source: https://www.newgrounds.com/audio/listen/1296027
- Restyled the update notice to match the game’s dark menu controls and added a
  `Later` button that remembers the dismissed update until a newer build exists.
- Replaced the `record.wait` music disc audio and label with “Sonic Blaster”
  by F-777. Source: https://soundcloud.com/f-777/sonic-blaster
- Added TailsConnect multiplayer integration, including the menu, room controls,
  world names, private room joining, TC3 transport, and background connection
  handling.
- Added TC4 friends and presence: friend cards, requests, online presence,
  join requests, approval dialogs, join progress, and automatic friends-service
  connection.
- Added local replay mode with pause-menu controls in singleplayer and
  multiplayer for recording
  the game canvas to a downloadable `.webm` video.
- Replay recording is browser-local and does not retain player/world frames or
  upload anything to a server.
- Replay videos now include the client’s game audio stream and prefer the
  lighter VP8 encoder for smoother capture.
- Reduced replay capture to 30 FPS and 2 Mbps to reduce gameplay lag while
  recording.
- Added the client-only Lighting Bypass option for fullbright rendering without
  applying client light updates from light-emitting blocks.
- Lighting Bypass defaults to ON for new game profiles and remembers an explicit
  player choice afterward.
- Added Video Settings -> MS Paint Mode, which loads the bundled
  1.12.x-compatible MS Paint resource pack locally, with a network fallback
  for older/custom builds.
- Credit: MS Paint Mode bundles the “MS Paintcraft” resource pack from
  Modrinth: https://modrinth.com/resourcepack/mspaintcraft
- Added Video Settings -> REALISTIC MODE with a bundled 128x TailsCraft
  texture pack covering core overworld, Nether, End, utility blocks, menus,
  inventory panels, resource-pack screens, widgets, and icons.
- Fixed REALISTIC MODE activation and made built-in resource packs reuse their
  cached installation instead of creating duplicate copies.
- Removed GUI overrides from REALISTIC MODE; it now changes only the bundled
  world textures, with a cache-bumped pack ID to avoid stale GUI copies.
- Added a non-blocking client updater that embeds the build commit, checks the
  server manifest every second, and offers a cache-busted refresh when a newer
  commit is available.
- Removed TailsConnect heartbeat traffic and added clearer connection errors.
- Added the AuthMe register/login prompt with masked password fields.
- Added a native-style AuthMe “Remember me” checkbox. Credentials are saved
  only after `Successful login!`, keyed per server, and automatically used for
  future `/login` prompts.
- Added `Options -> Saved passwords`, with per-server Forget controls.
- Added startup cleanup for the orphaned legacy
  `ws://tails1154.com:25565` credential when it is not in the saved-server list.
- Added the GitHub button and client defaults for fast graphics, bright
  brightness, eight-chunk render distance, auto-jump off, fog/clouds/entity
  shadows off, minimal particles, smooth lighting off, and VSync on.
- Added the TailsCraft logo/splash update and deployment of the client bundle,
  including `classes.wasm`, `eagruntime.js`, `assets.epk`, and `assets.epw`.
- Updated deployment scripts to build the required artifacts, create backups,
  and deploy the complete WASM-GC bundle.
- Added the 1.13-style command suggestion/tab-completion UI.
- Added `/gamemode` support for singleplayer.
- Added automatic sprinting and later changed it to the requested always-sprint
  behavior.

## Removed or reverted

- Removed the broken security-camera client and server feature.
- Removed the lantern and chain backport, custom textures/models, protocol
  placeholder conversion, and the TailsCraft lantern server plugin.
- Removed the spear/grappling-hook experiment and its orphaned registrations.
- Removed the offensive splash text.
- Removed world transfer after it caused client crashes.
- Removed the broken “Host as TC5 Cloud World” control from the TailsConnect
  menu; normal TailsConnect world hosting remains available.
- Voice chat work was abandoned after repeated browser/API and disconnect
  instability; it is not an active feature.

## Historical experiments and fixes

- Added and then stabilized friend join routing through private room codes.
- Fixed join-request key handling, automatic dialog opening, unlimited-room
  support, active-world restrictions, and stale friend prompts.
- Fixed TC5 upload framing, null rotations, portal ticks, integrated-worker
  startup, bridge disconnect cleanup, world-selection crashes, and render
  distance defaults.
- Added server-side compatibility experiments for newer blocks and items, then
  removed the broken implementations rather than leaving placeholder blocks.
