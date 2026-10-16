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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Server-side compatibility layer for TailsCraft lanterns.
 *
 * Uses legal 1.12 item stacks for compatibility, then upgrades placed blocks
 * to TailsCraft's dedicated client block IDs.
 */
public final class TailsCraftLanternsPlugin extends JavaPlugin implements Listener {
    @Override public void onEnable() {
        registerServerBlocks();
        getServer().getPluginManager().registerEvents(this, this);
    }

    /** Register the IDs used by the TailsCraft client as real Paper blocks. */
    private void registerServerBlocks() {
        try {
            String nms = getServer().getClass().getPackage().getName().replace("org.bukkit.craftbukkit", "net.minecraft.server");
            Class<?> blockClass = Class.forName(nms + ".Block");
            Class<?> materialClass = Class.forName(nms + ".Material");
            Class<?> keyClass = Class.forName(nms + ".MinecraftKey");
            Field registryField = blockClass.getField("REGISTRY");
            Field registryIdField = blockClass.getField("REGISTRY_ID");
            Object registry = registryField.get(null);
            Object registryId = registryIdField.get(null);
            Constructor<?> keyConstructor = keyClass.getConstructor(String.class);
            Constructor<?> blockConstructor = blockClass.getDeclaredConstructor(materialClass);
            blockConstructor.setAccessible(true);
            Object material = materialClass.getField("HEAVY").get(null);
            // RegistryBlocks erases its generic key/value parameters to Object.
            Method register = registry.getClass().getMethod("a", int.class, Object.class, Object.class);
            Method get = registry.getClass().getMethod("get", Object.class);
            Method getState = blockClass.getMethod("getBlockData");
            Method registerState = registryId.getClass().getMethod("a", Object.class, int.class);

            registerServerBlock(253, "lantern", blockClass, keyConstructor, blockConstructor, material,
                    registry, registryId, register, get, getState, registerState);
            registerServerBlock(254, "chain", blockClass, keyConstructor, blockConstructor, material,
                    registry, registryId, register, get, getState, registerState);
            getLogger().info("Registered TailsCraft lantern and chain blocks (253/254)");
        } catch (Throwable ex) {
            getLogger().severe("Could not register TailsCraft blocks: " + ex);
        }
    }

    private void registerServerBlock(int id, String name, Class<?> blockClass, Constructor<?> keyConstructor,
            Constructor<?> blockConstructor, Object material, Object registry, Object registryId, Method register,
            Method get, Method getState, Method registerState) throws Exception {
        Object key = keyConstructor.newInstance("tailscraft:" + name);
        if (get.invoke(registry, key) != null) return;
        Object block = blockConstructor.newInstance(material);
        register.invoke(registry, id, key, block);
        registerState.invoke(registryId, getState.invoke(block), id << 4);
    }

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
