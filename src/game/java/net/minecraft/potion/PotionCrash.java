package net.minecraft.potion;

import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;

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
		if (server == null || !server.isSinglePlayer()) {
			return;
		}
		if (!entity.world.isRemote) {
			server.saveAllWorlds(true);
		} else if (entity == Minecraft.getMinecraft().player && Minecraft.getMinecraft().isSingleplayer()) {
			Minecraft.getMinecraft().crashed(CrashReport.makeCrashReport(
					new RuntimeException("Potion of Crashing activated after saving the world"),
					"Drank a Potion of Crashing"));
		}
	}
}
