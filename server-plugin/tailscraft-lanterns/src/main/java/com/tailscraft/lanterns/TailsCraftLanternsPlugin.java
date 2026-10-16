package com.tailscraft.lanterns;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Server-side compatibility layer for TailsCraft lanterns.
 *
 * Uses legal 1.12 item stacks for compatibility, then upgrades placed blocks
 * to TailsCraft's dedicated client block IDs.
 */
public final class TailsCraftLanternsPlugin extends JavaPlugin implements Listener {
    @Override public void onEnable() { getServer().getPluginManager().registerEvents(this, this); }

    @EventHandler
    public void onLanternPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        byte data = (byte) ("Soul Lantern".equalsIgnoreCase(name) ? 1 : 0);
        if (event.getBlockAgainst().getY() > event.getBlockPlaced().getY()) data |= 2;
        final int blockId;
        if ("Lantern".equalsIgnoreCase(name) || "Soul Lantern".equalsIgnoreCase(name)) {
            blockId = 253;
        } else if ("Chain".equalsIgnoreCase(name)) {
            blockId = 254;
            data = 0;
        } else {
            return;
        }

        // Bukkit has already performed the normal placement by this point. Apply
        // the compatibility block one tick later so the server's placement
        // packet cannot overwrite the custom client block with the placeholder.
        final byte finalData = data;
        getServer().getScheduler().runTask(this, () ->
                event.getBlockPlaced().setTypeIdAndData(blockId, finalData, true));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("lantern") && !command.getName().equalsIgnoreCase("chain")) return false;
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use /lantern.");
            return true;
        }
        boolean chain = command.getName().equalsIgnoreCase("chain");
        boolean soul = !chain && args.length > 0 && args[0].equalsIgnoreCase("soul");
        Player player = (Player) sender;
        ItemStack item = new ItemStack(chain ? Material.IRON_FENCE : Material.BARRIER, 1, (short) (soul ? 1 : 0));
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + (chain ? "Chain" : (soul ? "Soul Lantern" : "Lantern")));
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
        player.sendMessage(ChatColor.GREEN + "Given a " + (chain ? "chain" : (soul ? "soul lantern" : "lantern")) + ".");
        return true;
    }
}
