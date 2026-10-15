package com.tailscraft.voice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

/** Relays opt-in TCV1 WebRTC signaling; it never receives or stores microphone audio. */
public final class TailsCraftVoicePlugin extends JavaPlugin implements PluginMessageListener, Listener {
    private static final double DISCOVERY_RANGE = 128.0D;
    private final Map<UUID, Player> peers = new HashMap<UUID, Player>();
    private final Set<String> visiblePairs = new HashSet<String>();
    private int syncTicks;

    @Override
    public void onEnable() {
        Bukkit.getMessenger().registerIncomingPluginChannel(this, TailsCraftVoiceCodec.CHANNEL, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, TailsCraftVoiceCodec.CHANNEL);
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getScheduler().runTaskTimer(this, this::broadcastPositions, 10L, 10L);
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, TailsCraftVoiceCodec.CHANNEL, this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, TailsCraftVoiceCodec.CHANNEL);
        peers.clear();
        visiblePairs.clear();
    }

    @Override
    public void onPluginMessageReceived(String channel, Player sender, byte[] bytes) {
        if (!TailsCraftVoiceCodec.CHANNEL.equals(channel)) return;
        try {
            TailsCraftVoiceCodec.Packet packet = TailsCraftVoiceCodec.decode(bytes);
            if (packet.type == TailsCraftVoiceCodec.HELLO) {
                getLogger().info("TCV1 voice opt-in from " + sender.getName());
                join(sender, packet.text);
            } else if (packet.type == TailsCraftVoiceCodec.LEAVE) {
                getLogger().info("TCV1 voice opt-out from " + sender.getName());
                leave(sender);
            } else if (packet.type == TailsCraftVoiceCodec.SIGNAL) {
                relaySignal(sender, packet);
            } else if (packet.type == TailsCraftVoiceCodec.MUTE) {
                broadcast(TailsCraftVoiceCodec.mute(sender.getUniqueId(), packet.muted), sender);
            } else if (packet.type == TailsCraftVoiceCodec.AUDIO) {
                relayAudio(sender, packet);
            }
        } catch (IOException | RuntimeException exception) {
            getLogger().warning("Rejected malformed TCV1 packet from " + sender.getName());
        }
    }

    private void relayAudio(Player sender, TailsCraftVoiceCodec.Packet packet) throws IOException {
        if (packet.source == null || !sender.getUniqueId().equals(packet.source)) return;
        byte[] encoded = TailsCraftVoiceCodec.audio(sender.getUniqueId(), packet.audio);
        for (Player target : peers.values()) {
            if (target.isOnline() && !target.equals(sender)
                    && sender.getWorld().equals(target.getWorld())
                    && sender.getLocation().distanceSquared(target.getLocation()) <= DISCOVERY_RANGE * DISCOVERY_RANGE) {
                send(target, encoded);
            }
        }
    }

    private void join(Player player, String requestedName) throws IOException {
        List<Player> existing = new ArrayList<Player>(peers.values());
        peers.put(player.getUniqueId(), player);
        send(player, TailsCraftVoiceCodec.basic(TailsCraftVoiceCodec.WELCOME, player.getUniqueId(), "TCV1"));
        for (Player peer : existing) {
            if (!peer.isOnline() || peer.equals(player) || !peer.getWorld().equals(player.getWorld())
                    || peer.getLocation().distanceSquared(player.getLocation()) > DISCOVERY_RANGE * DISCOVERY_RANGE) continue;
            send(player, TailsCraftVoiceCodec.basic(TailsCraftVoiceCodec.PEER_JOIN,
                    peer.getUniqueId(), safeName(peer)));
            send(peer, TailsCraftVoiceCodec.basic(TailsCraftVoiceCodec.PEER_JOIN,
                    player.getUniqueId(), safeName(player)));
        }
        getLogger().info("TCV1 peer registered: " + player.getName());
        refreshVisibility();
    }

    private void leave(Player player) throws IOException {
        UUID id = player.getUniqueId();
        peers.remove(id);
        List<String> removed = new ArrayList<String>();
        for (String pair : visiblePairs) if (pair.startsWith(id.toString() + ":") || pair.endsWith(":" + id.toString())) removed.add(pair);
        for (String pair : removed) visiblePairs.remove(pair);
        byte[] packet = TailsCraftVoiceCodec.basic(TailsCraftVoiceCodec.PEER_LEAVE, id, null);
        broadcast(packet, player);
    }

    private void relaySignal(Player sender, TailsCraftVoiceCodec.Packet packet) throws IOException {
        if (packet.target == null || !sender.getUniqueId().equals(packet.source)) return;
        Player target = peers.get(packet.target);
        if (target == null || !target.isOnline() || !sender.getWorld().equals(target.getWorld())
                || sender.getLocation().distanceSquared(target.getLocation()) > DISCOVERY_RANGE * DISCOVERY_RANGE) return;
        // ICE commonly emits several candidates in the same tick. Do not
        // drop them: doing so breaks WebRTC on some NATs with no clear error.
        getLogger().info("TCV1 signal " + packet.signalType + " " + sender.getName() + " -> " + target.getName());
        send(target, TailsCraftVoiceCodec.signal(sender.getUniqueId(), target.getUniqueId(), packet.signalType, packet.text));
    }

    private void broadcastPositions() {
        refreshVisibility();
        syncTicks += 10;
        for (Player source : new ArrayList<Player>(peers.values())) {
            if (!source.isOnline()) continue;
            try {
                byte[] packet = TailsCraftVoiceCodec.position(source.getUniqueId(), source.getLocation().getX(),
                        source.getLocation().getY(), source.getLocation().getZ());
                for (Player target : peers.values()) {
                    if (!target.equals(source) && target.isOnline() && target.getWorld().equals(source.getWorld())
                            && target.getLocation().distanceSquared(source.getLocation()) <= DISCOVERY_RANGE * DISCOVERY_RANGE) {
                        send(target, packet);
                    }
                }
            } catch (IOException ignored) {
            }
        }
        if (syncTicks >= 40) {
            syncTicks = 0;
            for (Player target : new ArrayList<Player>(peers.values())) {
                if (!target.isOnline()) continue;
                try {
                    // Resend the original PEER_JOIN packet as a compatibility
                    // sync. Some proxies preserve plugin messages but do not
                    // forward newer application packet types reliably.
                    for (Player peer : peers.values()) {
                        if (!peer.equals(target) && peer.isOnline()
                                && target.getWorld().equals(peer.getWorld())
                                && target.getLocation().distanceSquared(peer.getLocation()) <= DISCOVERY_RANGE * DISCOVERY_RANGE) {
                            send(target, TailsCraftVoiceCodec.basic(TailsCraftVoiceCodec.PEER_JOIN,
                                    peer.getUniqueId(), safeName(peer)));
                        }
                    }
                } catch (IOException ignored) { }
            }
        }
    }

    private byte[] snapshotFor(Player target) throws IOException {
        List<TailsCraftVoiceCodec.PlayerEntry> visible = new ArrayList<TailsCraftVoiceCodec.PlayerEntry>();
        for (Player peer : peers.values()) {
            if (peer.equals(target) || !peer.isOnline()) continue;
            if (target.getWorld().equals(peer.getWorld())
                    && target.getLocation().distanceSquared(peer.getLocation()) <= DISCOVERY_RANGE * DISCOVERY_RANGE) {
                visible.add(new TailsCraftVoiceCodec.PlayerEntry(peer.getUniqueId(), safeName(peer)));
            }
        }
        return TailsCraftVoiceCodec.snapshot(visible);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        try { leave(event.getPlayer()); } catch (IOException ignored) { }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (peers.containsKey(event.getPlayer().getUniqueId())) refreshVisibility();
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (peers.containsKey(event.getPlayer().getUniqueId())) refreshVisibility();
    }

    private void refreshVisibility() {
        List<Player> snapshot = new ArrayList<Player>(peers.values());
        for (int i = 0; i < snapshot.size(); ++i) {
            Player first = snapshot.get(i);
            if (!first.isOnline()) continue;
            for (int j = i + 1; j < snapshot.size(); ++j) {
                Player second = snapshot.get(j);
                if (!second.isOnline()) continue;
                String pair = pairKey(first.getUniqueId(), second.getUniqueId());
                boolean visible = first.getWorld().equals(second.getWorld())
                        && first.getLocation().distanceSquared(second.getLocation()) <= DISCOVERY_RANGE * DISCOVERY_RANGE;
                boolean known = visiblePairs.contains(pair);
                try {
                    if (visible && !known) {
                        visiblePairs.add(pair);
                        getLogger().info("TCV1 nearby pair: " + first.getName() + " <-> " + second.getName());
                        send(first, TailsCraftVoiceCodec.basic(TailsCraftVoiceCodec.PEER_JOIN, second.getUniqueId(), safeName(second)));
                        send(second, TailsCraftVoiceCodec.basic(TailsCraftVoiceCodec.PEER_JOIN, first.getUniqueId(), safeName(first)));
                    } else if (!visible && known) {
                        visiblePairs.remove(pair);
                        send(first, TailsCraftVoiceCodec.basic(TailsCraftVoiceCodec.PEER_LEAVE, second.getUniqueId(), null));
                        send(second, TailsCraftVoiceCodec.basic(TailsCraftVoiceCodec.PEER_LEAVE, first.getUniqueId(), null));
                    }
                } catch (IOException ignored) { }
            }
        }
    }

    private static String pairKey(UUID first, UUID second) {
        return first.toString().compareTo(second.toString()) < 0
                ? first.toString() + ":" + second.toString() : second.toString() + ":" + first.toString();
    }

    private void broadcast(byte[] packet, Player except) {
        for (Player player : peers.values()) if (player.isOnline() && !player.equals(except)) send(player, packet);
    }

    private void send(Player player, byte[] packet) {
        player.sendPluginMessage(this, TailsCraftVoiceCodec.CHANNEL, packet);
    }

    private static String safeName(Player player) {
        String name = player.getName();
        return name.length() > 64 ? name.substring(0, 64) : name;
    }
}
