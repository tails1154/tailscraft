package net.minecraft.client.baritone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.settings.KeyBinding;
import net.lax1dude.eaglercraft.opengl.EaglercraftGPU;
import net.lax1dude.eaglercraft.opengl.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.pathing.goals.GoalYLevel;

/**
 * Local Pathfindinganator controller for the 1.12.2 client.
 *
	 * The command parser is private to the GUI; chat never invokes it. The
	 * operation names follow Baritone (#goto, #mine, #stop) for familiarity.
 */
public final class BaritoneController {
	private static boolean active;
	private static boolean mining;
	private static int targetX, targetY, targetZ;
	private static Block mineBlock;
	private static List<Block> mineBlocks = Collections.emptyList();
	private static BlockPos mineTarget;
	private static int mineLimit;
	private static boolean goingToBlock;
	private static Block gotoBlock;
	private static BlockPos gotoTarget;
	private static Goal pathGoal;
	private static List<BlockPos> path = Collections.emptyList();
	private static BaritonePathfinder.Search pathSearch;
	private static int recalc;
	private static String status = "Stopped";
	/* Baritone 1.2.19 defaults both traversal settings to enabled. */
	private static boolean allowBreak = true;
	private static boolean allowPlace = true;
	/* World access stays on the client thread, but scanning is time-sliced. */
	private static final int SEARCH_BATCH = 512;
	private static List<Block> searchBlocks = Collections.emptyList();
	private static boolean scanActive;
	private static boolean searchFinished;
	private static int scanMinY, scanMaxY;
	private static int scanCenterX, scanCenterZ, scanRadius, scanRingIndex;
	private static int scanX, scanY, scanZ;

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
		if ("set".equals(command)) {
			if (args.length != 3 || !("allowBreak".equalsIgnoreCase(args[1]) || "allowPlace".equalsIgnoreCase(args[1]))) { message("Usage: #set allowBreak|allowPlace true|false"); return true; }
			boolean value = "true".equalsIgnoreCase(args[2]) || "on".equalsIgnoreCase(args[2]);
			if ("allowBreak".equalsIgnoreCase(args[1])) allowBreak = value; else allowPlace = value;
			message("Baritone: " + args[1] + " = " + value);
			return true;
		}
		if ("goto".equals(command)) {
			if (args.length == 2) {
				EntityPlayerSP player = Minecraft.getMinecraft().player;
				int originY = player == null ? 64 : (int)Math.floor(player.posY);
				try {
					startGotoY(parseCoordinate(args[1], originY));
				} catch (NumberFormatException ex) {
					Block block = Block.getBlockFromName(args[1].toLowerCase());
					if (block == null) { message("Baritone: unknown block " + args[1]); return true; }
					startGotoBlock(block);
				}
				return true;
			}
			if (args.length != 3 && args.length != 4) { message("Usage: #goto <x> [y] <z>"); return true; }
			try {
				int x;
				EntityPlayerSP player = Minecraft.getMinecraft().player;
				int originX = player == null ? 0 : (int)Math.floor(player.posX);
				int originY = player == null ? 64 : (int)Math.floor(player.posY);
				int originZ = player == null ? 0 : (int)Math.floor(player.posZ);
				int y = args.length == 4 ? parseCoordinate(args[2], originY) : originY;
				int z = parseCoordinate(args[args.length - 1], originZ);
				x = parseCoordinate(args[1], originX);
				startGoto(x, y, z, args.length == 3);
			} catch (NumberFormatException ex) { message("Usage: #goto <x> [y] <z> (coordinates may use ~)"); }
			return true;
		}
		if ("mine".equals(command)) {
			if (args.length < 2) { message("Usage: #mine <block> [block ...]"); return true; }
			int firstBlock = 1;
			int limit = 0;
			if (args.length >= 3) {
				try {
					limit = Integer.parseInt(args[1]);
					if (limit <= 0) throw new NumberFormatException();
					firstBlock = 2;
				} catch (NumberFormatException ignored) {
					limit = 0;
				}
			}
			List<Block> blocks = new ArrayList<Block>();
			for (int i = firstBlock; i < args.length; ++i) {
				Block block = Block.getBlockFromName(args[i].toLowerCase());
				if (block == null) { message("Baritone: unknown block " + args[i]); return true; }
				blocks.add(block);
			}
			startMine(blocks, limit);
			return true;
		}
		message("Baritone commands: #goto <block>|<x> [y] <z>, #mine [quantity] <block> [block ...], #set allowBreak|allowPlace true|false, #stop");
		return true;
	}

	private static void startGoto(int x, int y, int z, boolean xzOnly) {
		active = true; mining = false; goingToBlock = false; gotoBlock = null; gotoTarget = null; mineBlock = null; mineBlocks = Collections.emptyList(); mineTarget = null; mineLimit = 0;
		resetSearch();
		targetX = x; targetY = y; targetZ = z; pathGoal = xzOnly ? new GoalXZ(x, z) : new GoalBlock(x, y, z);
		path = Collections.emptyList(); pathSearch = null; recalc = 10;
		status = "Going to " + x + " " + y + " " + z;
		message("Baritone: " + status);
	}

	private static void startGotoBlock(Block block) {
		active = true; mining = false; goingToBlock = true; gotoBlock = block; gotoTarget = null; mineBlock = null; mineBlocks = Collections.emptyList(); mineTarget = null; mineLimit = 0; pathGoal = null; path = Collections.emptyList(); pathSearch = null; recalc = 10;
		searchBlocks = Collections.singletonList(block);
		resetSearch();
		status = "Searching for " + blockName(block);
		message("Baritone: " + status);
	}

	private static void startGotoY(int y) {
		active = true; mining = false; goingToBlock = false; gotoBlock = null; gotoTarget = null; mineBlock = null; mineBlocks = Collections.emptyList(); mineTarget = null; mineLimit = 0;
		searchBlocks = Collections.emptyList(); resetSearch();
		targetY = y; pathGoal = new GoalYLevel(y); path = Collections.emptyList(); pathSearch = null; recalc = 10;
		status = "Going to Y=" + y;
		message("Baritone: " + status);
	}

	private static void startMine(List<Block> blocks, int limit) {
		active = true; mining = true; goingToBlock = false; gotoBlock = null; gotoTarget = null; mineBlock = null; mineBlocks = blocks; mineTarget = null; mineLimit = limit; pathGoal = null; path = Collections.emptyList(); pathSearch = null; recalc = 10;
		searchBlocks = blocks;
		resetSearch();
		status = "Searching for " + blockNames(blocks);
		message("Baritone: " + status);
	}

	public static void tick(Minecraft mc) {
		if (!active) return;
		EntityPlayerSP player = mc.player;
		World world = mc.world;
		if (player == null || world == null) { releaseKeys(mc); return; }
		if (mining) {
			boolean targetChanged = mineTarget != null && world.getBlockState(mineTarget).getBlock() != mineBlock;
			if (mineTarget == null || targetChanged) {
				if (targetChanged && mineLimit > 0 && --mineLimit == 0) {
					stop();
					message("Baritone: mining quota reached");
					return;
				}
				if (targetChanged) resetSearch();
				mineTarget = findBlock(world, player);
				if (mineTarget == null) {
					status = searchFinished ? "No loaded target blocks found" : "Searching for " + blockNames(mineBlocks);
					releaseKeys(mc);
					return;
				}
				targetForBlock(world, mineTarget);
				path = Collections.emptyList(); pathSearch = null; recalc = 10; status = "Going to " + blockName(mineBlock);
			}
			if (!nearTarget(player)) { moveTo(mc, player, world); return; }
			faceAndMine(mc, player, mineTarget);
			return;
		}
		if (goingToBlock) {
			boolean targetChanged = gotoTarget != null && world.getBlockState(gotoTarget).getBlock() != gotoBlock;
			if (gotoTarget == null || targetChanged) {
				if (targetChanged) resetSearch();
				gotoTarget = findBlock(world, player);
				if (gotoTarget == null) {
					status = searchFinished ? "No loaded target blocks found" : "Searching for " + blockName(gotoBlock);
					releaseKeys(mc);
					return;
				}
				targetForBlock(world, gotoTarget);
				path = Collections.emptyList(); pathSearch = null; recalc = 10; status = "Going to " + blockName(gotoBlock);
			}
			if (nearTarget(player)) { stop(); message("Baritone: destination reached"); return; }
			moveTo(mc, player, world);
			return;
		}
		if (nearTarget(player)) { stop(); message("Baritone: destination reached"); return; }
		moveTo(mc, player, world);
	}

	private static void moveTo(Minecraft mc, EntityPlayerSP player, World world) {
		if (pathSearch == null && path.isEmpty() && recalc >= 10) {
			if (pathGoal == null) pathGoal = new GoalBlock(targetX, targetY, targetZ);
			boolean canPlace = allowPlace && hasPlaceableBlock(player);
			pathSearch = BaritonePathfinder.createSearch(world, new BlockPos(player), pathGoal, allowBreak, canPlace);
			recalc = 0;
		}
		if (pathSearch != null && !pathSearch.isDone()) {
			pathSearch.step(128);
			status = "Calculating path (" + pathSearch.getExpanded() + " nodes)";
			releaseMovement(mc);
			return;
		}
		if (pathSearch != null && pathSearch.isDone()) {
			path = pathSearch.getPath();
			pathSearch = null;
		}
		while (!path.isEmpty() && isNear(player, path.get(0))) path.remove(0);
		if (path.isEmpty() && !nearTarget(player)) { status = "No path found"; releaseKeys(mc); ++recalc; return; }
		BlockPos next = path.isEmpty() ? new BlockPos(targetX, targetY, targetZ) : path.get(0);
		if (handleObstacle(mc, player, world, next)) return;
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

	private static boolean handleObstacle(Minecraft mc, EntityPlayerSP player, World world, BlockPos next) {
		BlockPos feet = next;
		BlockPos head = next.up();
		if (allowBreak && !world.isAirBlock(feet)) { face(player, feet); mc.playerController.clickBlock(feet, EnumFacing.UP); status = "Breaking " + blockName(world.getBlockState(feet).getBlock()); releaseMovement(mc); return true; }
		if (allowBreak && !world.isAirBlock(head)) { face(player, head); mc.playerController.clickBlock(head, EnumFacing.UP); status = "Breaking " + blockName(world.getBlockState(head).getBlock()); releaseMovement(mc); return true; }
		if (allowPlace && world.isAirBlock(next.down()) && world instanceof WorldClient) {
			ItemStack stack = findPlaceableBlock(player);
			if (stack != null) {
				face(player, next.down());
				mc.playerController.processRightClickBlock(player, (WorldClient)world, next.down(), EnumFacing.UP, new Vec3d(next.getX() + .5D, next.getY() - .01D, next.getZ() + .5D), EnumHand.MAIN_HAND);
				status = "Placing support"; releaseMovement(mc); return true;
			}
		}
		return false;
	}
	private static void face(EntityPlayerSP player, BlockPos pos) { float yaw = (float)(Math.atan2(-(pos.getX() + .5D - player.posX), pos.getZ() + .5D - player.posZ) * 180.0D / Math.PI); player.rotationYaw = yaw; player.prevRotationYaw = yaw; }
	private static void releaseMovement(Minecraft mc) { KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false); KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false); KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false); }
	private static boolean hasPlaceableBlock(EntityPlayerSP player) { for (int i = 0; i < 9; ++i) { ItemStack stack = player.inventory.mainInventory.get(i); if (stack != null && !stack.func_190926_b() && stack.getItem() instanceof ItemBlock) return true; } return false; }
	private static ItemStack findPlaceableBlock(EntityPlayerSP player) { for (int i = 0; i < 9; ++i) { ItemStack stack = player.inventory.mainInventory.get(i); if (stack != null && !stack.func_190926_b() && stack.getItem() instanceof ItemBlock) { player.inventory.currentItem = i; return stack; } } return null; }

	private static void targetForBlock(World world, BlockPos block) {
		for (int i = 0; i < 4; ++i) {
			int x = block.getX() + (i == 0 ? 1 : i == 1 ? -1 : 0);
			int z = block.getZ() + (i == 2 ? 1 : i == 3 ? -1 : 0);
			if (canStand(world, x, block.getY(), z)) { targetX = x; targetY = block.getY(); targetZ = z; pathGoal = new GoalBlock(targetX, targetY, targetZ); return; }
		}
		targetX = block.getX(); targetY = block.getY(); targetZ = block.getZ();
		pathGoal = new GoalBlock(targetX, targetY, targetZ);
		pathSearch = null;
	}

	private static BlockPos findBlock(World world, EntityPlayerSP player) {
		if (!scanActive && !searchFinished) {
			int px = (int)Math.floor(player.posX), py = (int)Math.floor(player.posY), pz = (int)Math.floor(player.posZ);
			scanMinY = Math.max(0, py - 48); scanMaxY = Math.min(255, py + 16);
			scanCenterX = px; scanCenterZ = pz; scanRadius = 0; scanRingIndex = 0;
			scanX = px; scanY = scanMinY; scanZ = pz;
			scanActive = true;
		}
		int budget = SEARCH_BATCH;
		while (scanActive && budget-- > 0) {
			if (world.getChunkProvider().getLoadedChunk(scanX >> 4, scanZ >> 4) != null) {
				BlockPos pos = new BlockPos(scanX, scanY, scanZ);
				Block found = world.getBlockState(pos).getBlock();
				if (searchBlocks.contains(found)) {
					mineBlock = found;
					searchFinished = true;
					scanActive = false;
					return pos;
				}
			}
			advanceSearchCursor();
		}
		if (scanActive) return null;
		searchFinished = true;
		return null;
	}

	private static void advanceSearchCursor() {
		if (++scanY <= scanMaxY) return;
		scanY = scanMinY;
		if (scanRadius == 0) {
			scanRadius = 1;
			scanRingIndex = 0;
			setRingPosition();
			return;
		}
		if (++scanRingIndex < scanRadius * 8) {
			setRingPosition();
			return;
		}
		if (++scanRadius > 24) {
			scanActive = false;
			return;
		}
		scanRingIndex = 0;
		setRingPosition();
	}

	private static void setRingPosition() {
		int r = scanRadius;
		if (r == 0) {
			scanX = scanCenterX;
			scanZ = scanCenterZ;
			return;
		}
		int side = r * 2 + 1;
		int i = scanRingIndex;
		if (i < side) {
			scanX = scanCenterX - r + i;
			scanZ = scanCenterZ - r;
		} else if ((i -= side) < r * 2) {
			scanX = scanCenterX + r;
			scanZ = scanCenterZ - r + 1 + i;
		} else if ((i -= r * 2) < r * 2) {
			scanX = scanCenterX + r - 1 - i;
			scanZ = scanCenterZ + r;
		} else {
			scanX = scanCenterX - r;
			scanZ = scanCenterZ + r - 1 - (i - r * 2);
		}
	}

	private static void resetSearch() {
		searchBlocks = searchBlocks == null ? Collections.<Block>emptyList() : searchBlocks;
		scanActive = false;
		searchFinished = false;
	}

	/** Draw the current Baritone route over the world, including an in-progress route. */
	public static void renderPath(double cameraX, double cameraY, double cameraZ) {
		if (!active || path.isEmpty()) return;
		Tessellator tessellator = Tessellator.getInstance();
		GlStateManager.pushMatrix();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(net.lax1dude.eaglercraft.opengl.RealOpenGLEnums.GL_SRC_ALPHA,
				net.lax1dude.eaglercraft.opengl.RealOpenGLEnums.GL_ONE_MINUS_SRC_ALPHA,
				net.lax1dude.eaglercraft.opengl.RealOpenGLEnums.GL_ONE,
				net.lax1dude.eaglercraft.opengl.RealOpenGLEnums.GL_ZERO);
		GlStateManager.disableTexture2D();
		GlStateManager.disableDepth();
		EaglercraftGPU.glLineWidth(3.0F);
		EntityPlayerSP player = Minecraft.getMinecraft().player;
		WorldRendererCompat.beginPath(tessellator, player, cameraX, cameraY, cameraZ);
		tessellator.draw();
		EaglercraftGPU.glLineWidth(1.0F);
		GlStateManager.enableDepth();
		GlStateManager.enableTexture2D();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	private static final class WorldRendererCompat {
		private static void beginPath(Tessellator tessellator, EntityPlayerSP player, double cameraX, double cameraY, double cameraZ) {
			net.lax1dude.eaglercraft.opengl.WorldRenderer buffer = tessellator.getBuffer();
			buffer.begin(Tessellator.GL_LINE_STRIP, DefaultVertexFormats.POSITION_COLOR);
			if (player != null) {
				buffer.pos(player.posX - cameraX, player.posY + 0.1D - cameraY, player.posZ - cameraZ)
						.color(64, 255, 64, 230).endVertex();
			}
			int count = Math.min(path.size(), 512);
			for (int i = 0; i < count; ++i) {
				BlockPos point = path.get(i);
				int red = Math.min(255, 64 + i / 2);
				buffer.pos(point.getX() + 0.5D - cameraX, point.getY() + 0.15D - cameraY,
						point.getZ() + 0.5D - cameraZ).color(red, 255, 64, 230).endVertex();
			}
		}
	}

	private static boolean nearTarget(EntityPlayerSP player) {
		return pathGoal instanceof GoalYLevel
				? Math.abs(player.posY - targetY) < 1.2D
				: pathGoal instanceof GoalXZ
				? Math.abs(player.posX - targetX) < 1.2D && Math.abs(player.posZ - targetZ) < 1.2D
				: Math.abs(player.posX - targetX) < 1.2D && Math.abs(player.posY - targetY) < 2.0D && Math.abs(player.posZ - targetZ) < 1.2D;
	}
	private static boolean isNear(EntityPlayerSP player, BlockPos pos) { return Math.abs(player.posX - (pos.getX() + .5D)) < .8D && Math.abs(player.posY - pos.getY()) < 1.5D && Math.abs(player.posZ - (pos.getZ() + .5D)) < .8D; }
	private static boolean canStand(World world, int x, int y, int z) { BlockPos p = new BlockPos(x, y, z); return world.isAirBlock(p) && world.isAirBlock(p.up()) && !world.isAirBlock(p.down()); }
	private static String blockName(Block block) { ResourceLocation id = Block.REGISTRY.getNameForObject(block); return id == null ? "block" : id.toString(); }
	private static String blockNames(List<Block> blocks) { StringBuilder result = new StringBuilder(); for (Block block : blocks) { if (result.length() > 0) result.append(", "); result.append(blockName(block)); } return result.toString(); }
	private static int parseCoordinate(String value, int origin) {
		if (!value.startsWith("~")) return Integer.parseInt(value);
		if (value.length() == 1) return origin;
		return origin + Integer.parseInt(value.substring(1));
	}
	public static String getStatus() { return status; }
	public static boolean isActive() { return active; }
	public static boolean isAllowBreak() { return allowBreak; }
	public static boolean isAllowPlace() { return allowPlace; }
	public static void setAllowBreak(boolean value) { allowBreak = value; }
	public static void setAllowPlace(boolean value) { allowPlace = value; }
	public static void stop() { active = false; mining = false; goingToBlock = false; mineBlocks = Collections.emptyList(); searchBlocks = Collections.emptyList(); path = Collections.emptyList(); pathSearch = null; pathGoal = null; mineTarget = null; gotoTarget = null; gotoBlock = null; mineLimit = 0; resetSearch(); releaseKeys(Minecraft.getMinecraft()); status = "Stopped"; }
	private static void releaseKeys(Minecraft mc) { if (mc == null) return; KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false); KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false); KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false); KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false); }
	private static void message(String text) { Minecraft mc = Minecraft.getMinecraft(); if (mc != null && mc.ingameGUI != null) mc.ingameGUI.getChatGUI().printChatMessage(new net.minecraft.util.text.TextComponentString(text)); }
}
