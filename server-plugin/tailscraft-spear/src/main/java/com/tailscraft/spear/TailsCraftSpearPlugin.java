package com.tailscraft.spear;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.util.Vector;

/** Server half of the TailsCraft grappling spear. */
public final class TailsCraftSpearPlugin extends JavaPlugin implements PluginMessageListener, Listener {
    private static final String CHANNEL = "TC|Grapple";
    private static final double MAX_RANGE = 32.0D;
    private final Map<UUID, Long> recentGrapples = new HashMap<UUID, Long>();

    @Override
    public void onEnable() {
        getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, this);
        getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getServer().getMessenger().unregisterIncomingPluginChannel(this, CHANNEL, this);
        getServer().getMessenger().unregisterOutgoingPluginChannel(this, CHANNEL);
        recentGrapples.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("spear")) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /spear.");
            return true;
        }
        String tier = args.length == 0 ? "iron" : args[0].toLowerCase();
        ItemStack spear = makeSpear(tier);
        if (spear == null) {
            sender.sendMessage(ChatColor.RED + "Unknown grappling spear tier.");
            return true;
        }
        ((Player) sender).getInventory().addItem(spear);
        sender.sendMessage(ChatColor.GREEN + "Given a " + tier + " grappling spear.");
        return true;
    }

    private ItemStack makeSpear(String tier) {
        Material material;
        if (tier.equals("wood") || tier.equals("wooden")) {
            material = Material.WOOD_SWORD;
            tier = "wooden";
        } else if (tier.equals("stone")) {
            material = Material.STONE_SWORD;
        } else if (tier.equals("copper")) {
            material = Material.STONE_SWORD;
        } else if (tier.equals("iron")) {
            material = Material.IRON_SWORD;
        } else if (tier.equals("gold") || tier.equals("golden")) {
            material = Material.GOLD_SWORD;
            tier = "golden";
        } else if (tier.equals("diamond")) {
            material = Material.DIAMOND_SWORD;
        } else if (tier.equals("netherite")) {
            material = Material.DIAMOND_SWORD;
        } else {
            return null;
        }

        ItemStack result = new ItemStack(material);
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + capitalize(tier) + " Grappling Hook");
        result.setItemMeta(meta);
        return result;
    }

    private static String capitalize(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    private boolean isSpear(ItemStack item) {
        if (item == null || !item.getType().name().endsWith("_SWORD")) return false;
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return false;
        String name = item.getItemMeta().getDisplayName().toLowerCase();
        return name.contains(" grappling hook") || name.contains(" spear");
    }

    /** Fallback for servers that do not forward custom plugin messages from Eagler clients. */
    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK)
                && isSpear(event.getItem())) {
            tryGrapple(event.getPlayer(), -1, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel) || !isSpear(player.getInventory().getItemInMainHand())) return;
        try {
            DataInputStream input = new DataInputStream(new ByteArrayInputStream(message));
            if (input.readByte() != 1) return;
            int targetId = input.readInt();
            int blockX = input.readInt();
            int blockY = input.readInt();
            int blockZ = input.readInt();
            input.readInt(); // material is cosmetic; the server validates the held item.

            tryGrapple(player, targetId, blockX, blockY, blockZ);
        } catch (IOException | RuntimeException exception) {
            getLogger().warning("Invalid TailsCraft grapple packet from " + player.getName());
        }
    }

    private void tryGrapple(Player player, int targetId, int blockX, int blockY, int blockZ) {
        long now = System.currentTimeMillis();
        Long previous = recentGrapples.get(player.getUniqueId());
        if (previous != null && now - previous.longValue() < 100L) return;
        recentGrapples.put(player.getUniqueId(), Long.valueOf(now));

        Entity target = null;
        if (targetId >= 0) {
            for (Entity entity : player.getWorld().getEntities()) {
                if (entity.getEntityId() == targetId) {
                    target = entity;
                    break;
                }
            }
        }

        Vector anchor;
        if (target != null && target != player && player.getLocation().distanceSquared(target.getLocation())
                <= MAX_RANGE * MAX_RANGE && player.hasLineOfSight(target)) {
            anchor = target.getLocation().add(0.0D, target.getHeight() * 0.5D, 0.0D).toVector();
        } else if (blockX != Integer.MIN_VALUE) {
            Block block = player.getWorld().getBlockAt(blockX, blockY, blockZ);
            if (block.getType() == Material.AIR || player.getLocation().distanceSquared(block.getLocation())
                    > MAX_RANGE * MAX_RANGE) return;
            anchor = block.getLocation().add(0.5D, 0.5D, 0.5D).toVector();
        } else {
            Block block = player.getTargetBlock(null, (int) MAX_RANGE);
            if (block == null || block.getType() == Material.AIR) return;
            anchor = block.getLocation().add(0.5D, 0.5D, 0.5D).toVector();
        }

        Vector delta = anchor.subtract(player.getEyeLocation().toVector());
        double distance = delta.length();
        if (distance < 1.0D || distance > MAX_RANGE + 1.0D) return;
        double strength = Math.min(0.85D, 0.22D + distance * 0.018D);
        Vector velocity = delta.normalize().multiply(strength);
        velocity.setY(velocity.getY() + 0.04D);
        player.setVelocity(player.getVelocity().add(velocity));
    }
}
