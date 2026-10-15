# TailsCraft Voice Protocol (TCV1)

This is an original signaling protocol for TailsCraft's browser voice chat.
It does not copy Simple Voice Chat code or require that mod.

Voice is fully opt-in. The client must explicitly enable voice and request the
browser microphone permission before it sends `HELLO`; until then the server
does not add that player to the voice peer set or send them voice peer data.
Disabling voice means stopping the WebRTC tracks and no longer sending voice
packets. The server never requests microphone access on its own.

`TC|Voice` carries only control data:

- `HELLO`: client announces voice capability and display name.
- `WELCOME`: server confirms TCV1 and the client's UUID.
- `PEER_JOIN` / `PEER_LEAVE`: nearby voice peers.
- `SIGNAL`: SDP offer/answer or ICE candidate for WebRTC.
- `POSITION`: server-authoritative player position snapshots.
- `MUTE`: microphone state.
- `ERROR`: protocol errors.

The browser captures audio with `getUserMedia()` and sends it over a WebRTC
connection. The Minecraft server only relays signaling, sends nearby positions,
and enforces a 128-block discovery/signaling range; it never receives or stores
microphone audio.

The packet header is `TCV1` (big-endian `0x54435631`) followed by version `1`
and a one-byte packet type. Text fields are UTF-8 with a four-byte length and a
32 KiB limit. SDP/ICE payloads use the `SIGNAL` packet and are capped at 32 KiB.

Build with `./build.sh`, then install `build/TailsCraftVoice.jar` in a Paper,
Spigot, or Bukkit server's `plugins/` directory and restart the server.
