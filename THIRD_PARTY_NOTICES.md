# TailsCraft third-party notices

## Baritone pathing adaptation

TailsCraft includes an independent compatibility implementation in
`src/game/java/net/minecraft/client/ai/BaritonePathfinder.java`. It adapts
the bounded A* movement-search concepts used by Baritone to the TailsCraft
1.12.2/WASM API; Baritone's newer Minecraft mixins and runtime are not bundled.

Baritone is Copyright (c) 2018-2026 Baritone contributors and is licensed
under the GNU Lesser General Public License, version 3 or later (LGPL-3.0-or-later).

- Source: https://github.com/cabaletta/baritone
- License text: https://www.gnu.org/licenses/lgpl-3.0.html
- Reference branch studied for compatibility: `1.13.2`

The complete license text is available at the license-text URL above and is
also shown in the in-game Credits pages.

## Other resources

Other bundled libraries, assets, music, and upstream Eaglercraft/Minecraft
components retain their original copyright and license notices. See `LICENSE`,
the `desktopRuntime/*_license.txt` files, and the in-game Credits pages.
