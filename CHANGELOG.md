# TailsCraft changelog

This is a project-history changelog. Entries marked removed or reverted are kept
for context and are not part of the current client.

## Current client

- Added TailsConnect multiplayer integration, including the menu, room controls,
  world names, private room joining, TC3 transport, and background connection
  handling.
- Added TC4 friends and presence: friend cards, requests, online presence,
  join requests, approval dialogs, join progress, and automatic friends-service
  connection.
- Added local singleplayer replay mode with pause-menu controls for recording
  the game canvas to a downloadable `.webm` video.
- Replay recording is browser-local and does not retain player/world frames or
  upload anything to a server.
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
