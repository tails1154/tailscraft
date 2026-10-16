package net.minecraft.client.ai;

import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Small Baritone command surface for the 1.12.2 client.
 *
 * This is intentionally command driven: there is no remote planner or AI
 * service. The command names and prefix follow Baritone (#goto, #mine, #stop).
 */
public final class BaritoneController {
	private static boolean active;
	private static boolean mining;
	private static int targetX, targetY, targetZ;
	private static Block mineBlock;
	private static BlockPos mineTarget;
	private static BaritoneGoal pathGoal;
	private static List<BlockPos> path = Collections.emptyList();
	private static int recalc;
	private static String status = "Stopped";

	private BaritoneController() { }

	public static boolean handleCommand(String input) {
		if (input == null || !input.startsWith("#")) return false;
		String[] args = input.substring(1).trim().split("\\s+");
		String command = args.length == 0 ? "" : args[0].toLowerCase();
		if ("stop".equals(command)) {
			stop();
			message("Baritone: stopped");
			return true;
		}
		if ("goto".equals(command)) {
			if (args.length != 3 && args.length != 4) { message("Usage: #goto <x> [y] <z>"); return true; }
			try {
				int x = Integer.parseInt(args[1]);
				int y = args.length == 4 ? Integer.parseInt(args[2]) : (Minecraft.getMinecraft().player == null ? 64 : (int)Math.floor(Minecraft.getMinecraft().player.posY));
				int z = Integer.parseInt(args[args.length - 1]);
				startGoto(x, y, z, args.length == 3);
			} catch (NumberFormatException ex) { message("Usage: #goto <x> [y] <z>"); }
			return true;
		}
		if ("mine".equals(command)) {
			if (args.length != 2) { message("Usage: #mine <block>"); return true; }
			Block block = Block.REGISTRY.getObject(new ResourceLocation(args[1].toLowerCase()));
			if (block == null) { message("Baritone: unknown block " + args[1]); return true; }
			startMine(block, args[1].toLowerCase());
			return true;
		}
		message("Baritone commands: #goto <x> [y] <z>, #mine <block>, #stop");
		return true;
	}

	private static void startGoto(int x, int y, int z, boolean xzOnly) {
		active = true; mining = false; mineBlock = null; mineTarget = null;
		targetX = x; targetY = y; targetZ = z; pathGoal = xzOnly ? new BaritoneGoal.XZ(x, z) : new BaritoneGoal.Block(x, y, z);
		path = Collections.emptyList(); recalc = 0;
		status = "Going to " + x + " " + y + " " + z;
		message("Baritone: " + status);
	}

	private static void startMine(Block block, String name) {
		active = true; mining = true; mineBlock = block; mineTarget = null; pathGoal = null; path = Collections.emptyList(); recalc = 0;
		status = "Searching for " + name;
		message("Baritone: " + status);
	}

	public static void tick(Minecraft mc) {
		if (!active) return;
		EntityPlayerSP player = mc.player;
		World world = mc.world;
		if (player == null || world == null) { releaseKeys(mc); return; }
		if (mining) {
			if (mineTarget == null || world.getBlockState(mineTarget).getBlock() != mineBlock) {
				mineTarget = findBlock(world, player, mineBlock);
				if (mineTarget == null) { status = "No loaded " + blockName(mineBlock) + " found"; releaseKeys(mc); return; }
				targetForBlock(world, mineTarget);
				path = Collections.emptyList(); recalc = 0; status = "Going to " + blockName(mineBlock);
			}
			if (!nearTarget(player)) { moveTo(mc, player, world); return; }
			faceAndMine(mc, player, mineTarget);
			return;
		}
		if (nearTarget(player)) { stop(); message("Baritone: destination reached"); return; }
		moveTo(mc, player, world);
	}

	private static void moveTo(Minecraft mc, EntityPlayerSP player, World world) {
		if (++recalc >= 10 || path.isEmpty()) {
			if (pathGoal == null) pathGoal = new BaritoneGoal.Block(targetX, targetY, targetZ);
			path = BaritonePathfinder.findPath(world, new BlockPos(player), pathGoal);
			recalc = 0;
		}
		while (!path.isEmpty() && isNear(player, path.get(0))) path.remove(0);
		if (path.isEmpty() && !nearTarget(player)) { status = "No path found"; releaseKeys(mc); return; }
		BlockPos next = path.isEmpty() ? new BlockPos(targetX, targetY, targetZ) : path.get(0);
		float yaw = (float)(Math.atan2(-(next.getX() + 0.5D - player.posX), next.getZ() + 0.5D - player.posZ) * 180.0D / Math.PI);
		player.rotationYaw = yaw; player.prevRotationYaw = yaw;
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), player.isCollidedHorizontally || next.getY() > (int)Math.floor(player.posY));
		status = "Following path (" + path.size() + " nodes)";
	}

	private static void faceAndMine(Minecraft mc, EntityPlayerSP player, BlockPos pos) {
		releaseKeys(mc);
		float yaw = (float)(Math.atan2(-(pos.getX() + 0.5D - player.posX), pos.getZ() + 0.5D - player.posZ) * 180.0D / Math.PI);
		player.rotationYaw = yaw; player.prevRotationYaw = yaw;
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
		status = "Mining " + blockName(mineBlock);
	}

	private static void targetForBlock(World world, BlockPos block) {
		for (int i = 0; i < 4; ++i) {
			int x = block.getX() + (i == 0 ? 1 : i == 1 ? -1 : 0);
			int z = block.getZ() + (i == 2 ? 1 : i == 3 ? -1 : 0);
			if (canStand(world, x, block.getY(), z)) { targetX = x; targetY = block.getY(); targetZ = z; pathGoal = new BaritoneGoal.Block(targetX, targetY, targetZ); return; }
		}
		targetX = block.getX(); targetY = block.getY(); targetZ = block.getZ();
		pathGoal = new BaritoneGoal.Block(targetX, targetY, targetZ);
	}

	private static BlockPos findBlock(World world, EntityPlayerSP player, Block wanted) {
		int px = (int)Math.floor(player.posX), py = (int)Math.floor(player.posY), pz = (int)Math.floor(player.posZ);
		BlockPos best = null; double distance = Double.MAX_VALUE;
		for (int x = px - 24; x <= px + 24; ++x) for (int z = pz - 24; z <= pz + 24; ++z) {
			if (world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4) == null) continue;
			for (int y = Math.max(0, py - 48); y <= Math.min(255, py + 16); ++y) {
				BlockPos pos = new BlockPos(x, y, z);
				if (world.getBlockState(pos).getBlock() == wanted && player.getDistanceSq(pos) < distance) { distance = player.getDistanceSq(pos); best = pos; }
			}
		}
		return best;
	}

	private static boolean nearTarget(EntityPlayerSP player) {
		return pathGoal instanceof BaritoneGoal.XZ
				? Math.abs(player.posX - targetX) < 1.2D && Math.abs(player.posZ - targetZ) < 1.2D
				: Math.abs(player.posX - targetX) < 1.2D && Math.abs(player.posY - targetY) < 2.0D && Math.abs(player.posZ - targetZ) < 1.2D;
	}
	private static boolean isNear(EntityPlayerSP player, BlockPos pos) { return Math.abs(player.posX - (pos.getX() + .5D)) < .8D && Math.abs(player.posY - pos.getY()) < 1.5D && Math.abs(player.posZ - (pos.getZ() + .5D)) < .8D; }
	private static boolean canStand(World world, int x, int y, int z) { BlockPos p = new BlockPos(x, y, z); return world.isAirBlock(p) && world.isAirBlock(p.up()) && !world.isAirBlock(p.down()); }
	private static String blockName(Block block) { ResourceLocation id = Block.REGISTRY.getNameForObject(block); return id == null ? "block" : id.toString(); }
	public static String getStatus() { return status; }
	public static void stop() { active = false; mining = false; path = Collections.emptyList(); pathGoal = null; mineTarget = null; releaseKeys(Minecraft.getMinecraft()); status = "Stopped"; }
	private static void releaseKeys(Minecraft mc) { if (mc == null) return; KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false); KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false); KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false); KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false); }
	private static void message(String text) { Minecraft mc = Minecraft.getMinecraft(); if (mc != null && mc.ingameGUI != null) mc.ingameGUI.getChatGUI().printChatMessage(new net.minecraft.util.text.TextComponentString(text)); }
}
