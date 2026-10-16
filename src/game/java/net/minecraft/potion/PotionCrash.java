package net.minecraft.potion;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;

/** Deliberate, saved-world-only joke effect for singleplayer testing. */
public class PotionCrash extends Potion {
	public PotionCrash(boolean badEffect, int color) {
		super(badEffect, color);
	}

	public boolean isReady(int duration, int amplifier) {
		return true;
	}

	public void performEffect(EntityLivingBase entity, int amplifier) {
		if (!(entity instanceof EntityPlayer)) {
			return;
		}
		MinecraftServer server = entity.world.getMinecraftServer();
		if (server == null) {
			return;
		}
		if (!server.isSinglePlayer()) {
			if (entity instanceof EntityPlayerMP) {
				NetHandlerPlayServer connection = ((EntityPlayerMP) entity).connection;
				if (connection != null) {
					connection.func_194028_b(new TextComponentString("What do you think you're doing?"));
				}
			}
			return;
		}
		if (!entity.world.isRemote) {
			server.saveAllWorlds(true);
		} else if (entity.world.isRemote) {
			throw new RuntimeException("undefined function: TailsCraftCrash");
		}
	}
}
