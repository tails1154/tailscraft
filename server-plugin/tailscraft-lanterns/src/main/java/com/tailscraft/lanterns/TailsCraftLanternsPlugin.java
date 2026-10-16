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
 * Barrier is the 1.12 wire/fallback block: TailsCraft renders it as a lantern,
 * while older clients still receive a legal vanilla block. The item name is
 * intentionally stable so the client can resolve it without a new protocol.
 */
public final class TailsCraftLanternsPlugin extends JavaPlugin implements Listener {
    @Override public void onEnable() { getServer().getPluginManager().registerEvents(this, this); }

    @EventHandler
    public void onLanternPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) return;
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if ("Lantern".equalsIgnoreCase(name)) event.getBlockPlaced().setTypeIdAndData(253, (byte) 0, false);
        else if ("Soul Lantern".equalsIgnoreCase(name)) event.getBlockPlaced().setTypeIdAndData(254, (byte) 0, false);
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
