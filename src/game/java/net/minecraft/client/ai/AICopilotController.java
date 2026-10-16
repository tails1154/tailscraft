package net.minecraft.client.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Executes only the movement/look/mining subset of AICraftScriptLangPlus. */
public final class AICopilotController {
	private static boolean active;
	private static int targetX, targetY, targetZ;
	private static int recalc;
	private static List<Node> path = Collections.emptyList();
	private static List<AICraftScriptLangPlus.Step> script;
	private static int scriptIndex;
	private static BlockPos objective;
	private static int phase;
	private static int exploreDirection;
	private static String status = "Stopped";

	private AICopilotController() { }

	public static void setTarget(int x, int y, int z) {
		script = null; targetX = x; targetY = y; targetZ = z; recalc = 0; phase = 1; status = "Pathfinding";
	}

	public static void start() { active = true; recalc = 0; phase = 1; status = "Pathfinding to target"; }

	public static void runScript(List<AICraftScriptLangPlus.Step> steps) {
		if (steps == null || steps.isEmpty()) { stop(); return; }
		if (steps.size() == 1 && steps.get(0).type == AICraftScriptLangPlus.Type.STOP) { active = false; status = "AI: I could not turn that request into a safe action."; releaseKeys(); return; }
		script = steps; scriptIndex = 0; objective = null; phase = 0; active = true; status = "Running AICraftScriptLangPlus";
	}

	public static void stop() {
		active = false; script = null; objective = null; path = Collections.emptyList(); phase = 0; status = "Stopped"; releaseKeys();
	}

	public static boolean isActive() { return active; }
	public static String getStatus() { return status; }

	public static void tick(Minecraft mc) {
		if (!active) return;
		EntityPlayerSP player = mc.player; World world = mc.world;
		if (player == null || world == null) { releaseKeys(); return; }
		if (script != null && phase == 0) prepareScriptStep(world, player);
		if (script != null && phase == 3) { executeMining(mc, player, world); return; }
		if (script != null && phase == 4) {
			if (nearTarget(player)) { objective = null; phase = 0; path = Collections.emptyList(); recalc = 0; return; }
			if (++recalc > 20) { objective = findBlock(world, player, blockFor(script.get(scriptIndex).argument)); recalc = 0; }
			if (objective != null) { targetForBlock(world, objective); phase = 1; }
			else moveTo(targetX, targetY, targetZ, player, world);
			return;
		}
		if (nearTarget(player)) {
			if (script == null) { stop(); return; }
			if (script.get(scriptIndex).type == AICraftScriptLangPlus.Type.MINE) { phase = 3; return; }
			++scriptIndex; phase = 0; path = Collections.emptyList(); return;
		}
		moveTo(targetX, targetY, targetZ, player, world);
	}

	private static void prepareScriptStep(World world, EntityPlayerSP player) {
		if (scriptIndex >= script.size()) { stop(); return; }
		AICraftScriptLangPlus.Step step = script.get(scriptIndex);
		if (step.type == AICraftScriptLangPlus.Type.STOP) { stop(); return; }
		if (step.type == AICraftScriptLangPlus.Type.FIND_BLOCK) {
			objective = findBlock(world, player, blockFor(step.argument));
			if (objective == null) {
				if (!step.exploreOnFail) { status = "Could not find " + step.argument + " in loaded chunks"; stop(); return; }
				chooseExplore(player); phase = 4; status = "Exploring for " + step.argument; return;
			}
			targetForBlock(world, objective); phase = 1; status = "Pathfinding to " + step.argument;
		} else if (step.type == AICraftScriptLangPlus.Type.EXPLORE) {
			chooseExplore(player); phase = 1; status = "Exploring";
		} else if (step.type == AICraftScriptLangPlus.Type.SELECT_ITEM) {
			if (!selectHotbarItem(player, step.argument)) {
				status = "Could not find " + step.argument + " in the hotbar";
				stop();
				return;
			}
			status = "Selected " + player.inventory.getCurrentItem().getDisplayName();
			++scriptIndex; phase = 0;
		} else if (step.type == AICraftScriptLangPlus.Type.MINE) {
			if (objective == null) { ++scriptIndex; return; }
			targetForBlock(world, objective); phase = 1; status = "Pathfinding to mine " + step.argument;
		}
	}

	private static void chooseExplore(EntityPlayerSP player) {
		int d = exploreDirection++ & 3;
		targetX = (int) Math.floor(player.posX) + (d == 0 ? 32 : d == 1 ? -32 : 0);
		targetY = (int) Math.floor(player.posY);
		targetZ = (int) Math.floor(player.posZ) + (d == 2 ? 32 : d == 3 ? -32 : 0);
		recalc = 0; path = Collections.emptyList();
	}

	private static void targetForBlock(World world, BlockPos block) {
		for (int i = 0; i < 4; ++i) {
			int x = block.getX() + (i == 0 ? 1 : i == 1 ? -1 : 0);
			int z = block.getZ() + (i == 2 ? 1 : i == 3 ? -1 : 0);
			if (canStand(world, x, block.getY(), z)) { targetX = x; targetY = block.getY(); targetZ = z; return; }
		}
		targetX = block.getX(); targetY = block.getY(); targetZ = block.getZ();
	}

	private static void executeMining(Minecraft mc, EntityPlayerSP player, World world) {
		if (objective == null || world.getBlockState(objective).getBlock() != blockFor(script.get(scriptIndex).argument)) { ++scriptIndex; phase = 0; releaseKeys(); return; }
		releaseKeys();
		float yaw = (float) (Math.atan2(-(objective.getX() + 0.5D - player.posX), objective.getZ() + 0.5D - player.posZ) * 180.0D / Math.PI);
		player.rotationYaw = yaw; player.prevRotationYaw = yaw;
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
		status = "Mining " + script.get(scriptIndex).argument;
	}

	private static void moveTo(int x, int y, int z, EntityPlayerSP player, World world) {
		if (++recalc >= 10 || path.isEmpty()) { path = findPath(world, new BlockPos(player), new BlockPos(x, y, z)); recalc = 0; }
		Node next = path.isEmpty() ? new Node(x, y, z) : path.get(0);
		float yaw = (float) (Math.atan2(-(next.x + 0.5D - player.posX), next.z + 0.5D - player.posZ) * 180.0D / Math.PI);
		player.rotationYaw = yaw; player.prevRotationYaw = yaw;
		KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindForward.getKeyCode(), true);
		KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindSprint.getKeyCode(), true);
		KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindJump.getKeyCode(), player.isCollidedHorizontally || next.y > (int) Math.floor(player.posY));
	}

	private static boolean nearTarget(EntityPlayerSP player) { return Math.abs(player.posX - targetX) < 1.2D && Math.abs(player.posY - targetY) < 2.0D && Math.abs(player.posZ - targetZ) < 1.2D; }
	private static boolean selectHotbarItem(EntityPlayerSP player, String wanted) {
		String needle = wanted == null ? "" : wanted.toLowerCase();
		for (int slot = 0; slot < 9; ++slot) {
			net.minecraft.item.ItemStack stack = player.inventory.mainInventory.get(slot);
			if (stack != null && !stack.func_190926_b() && stack.getDisplayName().toLowerCase().indexOf(needle) >= 0) {
				player.inventory.currentItem = slot;
				return true;
			}
		}
		return false;
	}
	private static Block blockFor(String name) { return "diamond_ore".equals(name) || "diamond".equals(name) ? Blocks.DIAMOND_ORE : null; }

	private static BlockPos findBlock(World world, EntityPlayerSP player, Block wanted) {
		if (wanted == null) return null;
		int px = (int) Math.floor(player.posX), py = (int) Math.floor(player.posY), pz = (int) Math.floor(player.posZ);
		BlockPos best = null; double bestDistance = Double.MAX_VALUE;
		for (int x = px - 24; x <= px + 24; ++x) for (int z = pz - 24; z <= pz + 24; ++z) {
			if (world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4) == null) continue;
			for (int y = Math.max(0, py - 48); y <= Math.min(255, py + 16); ++y) {
				BlockPos pos = new BlockPos(x, y, z);
				if (world.getBlockState(pos).getBlock() == wanted) {
					double distance = player.getDistanceSq(pos);
					if (distance < bestDistance) { bestDistance = distance; best = pos; }
				}
			}
		}
		return best;
	}

	private static List<Node> findPath(World world, BlockPos start, BlockPos target) {
		final int radius = 16; final Map<Long, Node> nodes = new HashMap<Long, Node>(); List<Node> open = new ArrayList<Node>();
		Node origin = new Node(start.getX(), start.getY(), start.getZ()); origin.g = 0; origin.h = distance(origin, target); open.add(origin); nodes.put(origin.key(), origin);
		while (!open.isEmpty()) {
			Collections.sort(open, Comparator.comparingInt(AICopilotController::score)); Node current = open.remove(0);
			if (Math.abs(current.x - target.getX()) <= 1 && Math.abs(current.z - target.getZ()) <= 1) return buildPath(current);
			if (Math.abs(current.x - start.getX()) > radius || Math.abs(current.z - start.getZ()) > radius) continue;
			for (int i = 0; i < 4; ++i) {
				int nx = current.x + (i == 0 ? 1 : i == 1 ? -1 : 0), nz = current.z + (i == 2 ? 1 : i == 3 ? -1 : 0);
				for (int ny = current.y - 1; ny <= current.y + 1; ++ny) if (canStand(world, nx, ny, nz)) {
					Node candidate = new Node(nx, ny, nz), known = nodes.get(candidate.key()); int cost = current.g + 1 + Math.abs(ny - current.y) * 2;
					if (known == null || cost < known.g) { candidate.parent = current; candidate.g = cost; candidate.h = distance(candidate, target); nodes.put(candidate.key(), candidate); open.add(candidate); }
				}
			}
		}
		return Collections.emptyList();
	}

	private static boolean canStand(World world, int x, int y, int z) { BlockPos feet = new BlockPos(x, y, z); return world.isAirBlock(feet) && world.isAirBlock(feet.up()) && !world.isAirBlock(feet.down()); }
	private static int distance(Node n, BlockPos target) { return Math.abs(n.x - target.getX()) + Math.abs(n.y - target.getY()) + Math.abs(n.z - target.getZ()); }
	private static int score(Node n) { return n.g + n.h; }
	private static List<Node> buildPath(Node end) { List<Node> ret = new ArrayList<Node>(); for (Node n = end; n.parent != null; n = n.parent) ret.add(n); Collections.reverse(ret); return ret; }

	private static void releaseKeys() {
		Minecraft mc = Minecraft.getMinecraft(); if (mc == null) return;
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), false);
	}

	private static class Node { final int x, y, z; int g, h; Node parent; Node(int x, int y, int z) { this.x = x; this.y = y; this.z = z; } long key() { return (((long) x) << 42) ^ (((long) y) << 21) ^ (z & 0x1FFFFF); } }
}
