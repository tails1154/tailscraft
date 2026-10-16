package net.minecraft.client.baritone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;

/**
 * Bounded A* search using the ported Baritone 1.12.2 goal contract.
 *
 * The movement world adapter is TailsCraft-specific because the original
 * Forge/mixin runtime cannot run in TeaVM. Baritone attribution and LGPL-3.0
 * notice are in the TailsCraft credits and third-party notices.
 */
public final class BaritonePathfinder {
	private static final int MAX_NODES = 4096;
	private static final int HORIZONTAL_RADIUS = 32;

	private BaritonePathfinder() { }

	public static List<BlockPos> findPath(World world, BlockPos start, BlockPos target) {
		return findPath(world, start, new GoalBlock(target.getX(), target.getY(), target.getZ()), false, false);
	}

	public static List<BlockPos> findPath(World world, BlockPos start, Goal goal) {
		return findPath(world, start, goal, false, false);
	}

	public static List<BlockPos> findPath(World world, BlockPos start, Goal goal, boolean allowBreak, boolean allowPlace) {
		if (world == null || start == null || goal == null) return Collections.emptyList();
		Node origin = new Node(start.getX(), start.getY(), start.getZ());
		origin.g = 0.0D;
		origin.f = goal.heuristic(origin.x, origin.y, origin.z);
		PriorityQueue<Node> open = new PriorityQueue<Node>(Comparator.comparingDouble(node -> node.f));
		Map<Long, Node> known = new HashMap<Long, Node>();
		Set<Long> closed = new HashSet<Long>();
		open.add(origin);
		known.put(origin.key(), origin);
		int expanded = 0;
		while (!open.isEmpty() && expanded++ < MAX_NODES) {
			Node current = open.poll();
			if (!closed.add(current.key())) continue;
			if (goal.isInGoal(current.x, current.y, current.z)) {
				return buildPath(current);
			}
			for (int dx = -1; dx <= 1; ++dx) for (int dz = -1; dz <= 1; ++dz) {
				if (dx == 0 && dz == 0) continue;
				boolean diagonal = dx != 0 && dz != 0;
				if (diagonal && (!canStand(world, current.x + dx, current.y, current.z, allowBreak, allowPlace) || !canStand(world, current.x, current.y, current.z + dz, allowBreak, allowPlace))) continue;
				for (int dy = 1; dy >= -3; --dy) {
					int nx = current.x + dx, ny = current.y + dy, nz = current.z + dz;
					if (Math.abs(nx - start.getX()) > HORIZONTAL_RADIUS || Math.abs(nz - start.getZ()) > HORIZONTAL_RADIUS) continue;
					if (!isLoaded(world, nx, nz) || !canStand(world, nx, ny, nz, allowBreak, allowPlace)) continue;
					if (dy > 0 && !canStand(world, nx, current.y, nz, allowBreak, allowPlace)) continue;
					if (isHazard(world, nx, ny, nz)) continue;
					long key = key(nx, ny, nz);
					if (closed.contains(key)) continue;
					Node next = known.get(key);
					double moveCost = (diagonal ? Math.sqrt(2.0D) : 1.0D) * BaritoneActionCosts.WALK_ONE_BLOCK_COST;
					if (dy > 0) moveCost += BaritoneActionCosts.JUMP_ONE_BLOCK_COST;
					if (dy < 0) moveCost += BaritoneActionCosts.fallCost(-dy) + BaritoneActionCosts.CENTER_AFTER_FALL_COST;
					if (allowBreak) moveCost += breakCost(world, nx, ny, nz);
					if (allowPlace && world.isAirBlock(new BlockPos(nx, ny - 1, nz))) moveCost += BaritoneActionCosts.PLACE_ONE_BLOCK_COST;
					double tentative = current.g + moveCost;
					if (next == null) {
						next = new Node(nx, ny, nz);
						known.put(key, next);
					}
					if (tentative < next.g) {
						next.parent = current;
						next.g = tentative;
						next.f = tentative + goal.heuristic(next.x, next.y, next.z);
						open.add(next);
					}
				}
			}
		}
		return Collections.emptyList();
	}

	private static boolean isLoaded(World world, int x, int z) {
		return world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4) != null;
	}

	private static boolean canStand(World world, int x, int y, int z, boolean allowBreak, boolean allowPlace) {
		if (y < 1 || y > 254) return false;
		BlockPos feet = new BlockPos(x, y, z);
		boolean feetClear = world.isAirBlock(feet) || allowBreak && breakable(world.getBlockState(feet).getBlock());
		boolean headClear = world.isAirBlock(feet.up()) || allowBreak && breakable(world.getBlockState(feet.up()).getBlock());
		boolean support = !world.isAirBlock(feet.down()) || allowPlace;
		return feetClear && headClear && support;
	}

	private static boolean breakable(Block block) { return block != Blocks.BEDROCK && block != Blocks.BARRIER && block != Blocks.END_PORTAL && block != Blocks.END_PORTAL_FRAME && block != Blocks.AIR && block != Blocks.LAVA && block != Blocks.FLOWING_LAVA; }
	private static double breakCost(World world, int x, int y, int z) { double cost = 0.0D; if (!world.isAirBlock(new BlockPos(x, y, z))) cost += BaritoneActionCosts.BREAK_ONE_BLOCK_COST; if (!world.isAirBlock(new BlockPos(x, y + 1, z))) cost += BaritoneActionCosts.BREAK_ONE_BLOCK_COST; return cost; }

	private static boolean isHazard(World world, int x, int y, int z) {
		Block feet = world.getBlockState(new BlockPos(x, y, z)).getBlock();
		Block support = world.getBlockState(new BlockPos(x, y - 1, z)).getBlock();
		return feet == Blocks.FIRE || feet == Blocks.LAVA || feet == Blocks.FLOWING_LAVA
				|| support == Blocks.FIRE || support == Blocks.LAVA || support == Blocks.FLOWING_LAVA
				|| support == Blocks.CACTUS;
	}

	private static List<BlockPos> buildPath(Node end) {
		List<BlockPos> result = new ArrayList<BlockPos>();
		for (Node node = end; node.parent != null; node = node.parent) result.add(new BlockPos(node.x, node.y, node.z));
		Collections.reverse(result);
		return result;
	}

	private static long key(int x, int y, int z) { return (((long) x) << 42) ^ (((long) y) << 21) ^ (z & 0x1FFFFF); }

	private static final class Node {
		final int x, y, z;
		double g = Double.POSITIVE_INFINITY, f = Double.POSITIVE_INFINITY;
		Node parent;
		Node(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
		long key() { return BaritonePathfinder.key(x, y, z); }
	}
}
