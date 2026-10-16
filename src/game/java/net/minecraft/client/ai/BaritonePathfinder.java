package net.minecraft.client.ai;

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

/**
 * Bounded Baritone-style A* movement search adapted for TailsCraft 1.12.2.
 *
 * This is an independent compatibility implementation based on the movement
 * and cost-search concepts in Baritone, not a binary copy of its newer
 * Minecraft implementation. Baritone attribution and LGPL-3.0 notice are in
 * the TailsCraft credits and third-party notices.
 */
public final class BaritonePathfinder {
	private static final int MAX_NODES = 4096;
	private static final int HORIZONTAL_RADIUS = 32;

	private BaritonePathfinder() { }

	public static List<BlockPos> findPath(World world, BlockPos start, BlockPos target) {
		if (world == null || start == null || target == null) return Collections.emptyList();
		Node origin = new Node(start.getX(), start.getY(), start.getZ());
		origin.g = 0.0D;
		origin.f = heuristic(origin, target);
		PriorityQueue<Node> open = new PriorityQueue<Node>(Comparator.comparingDouble(node -> node.f));
		Map<Long, Node> known = new HashMap<Long, Node>();
		Set<Long> closed = new HashSet<Long>();
		open.add(origin);
		known.put(origin.key(), origin);
		int expanded = 0;
		while (!open.isEmpty() && expanded++ < MAX_NODES) {
			Node current = open.poll();
			if (!closed.add(current.key())) continue;
			if (Math.abs(current.x - target.getX()) <= 1 && Math.abs(current.y - target.getY()) <= 1 && Math.abs(current.z - target.getZ()) <= 1) {
				return buildPath(current);
			}
			for (int dx = -1; dx <= 1; ++dx) for (int dz = -1; dz <= 1; ++dz) {
				if (dx == 0 && dz == 0) continue;
				boolean diagonal = dx != 0 && dz != 0;
				if (diagonal && (!canStand(world, current.x + dx, current.y, current.z) || !canStand(world, current.x, current.y, current.z + dz))) continue;
				for (int dy = 1; dy >= -3; --dy) {
					int nx = current.x + dx, ny = current.y + dy, nz = current.z + dz;
					if (Math.abs(nx - start.getX()) > HORIZONTAL_RADIUS || Math.abs(nz - start.getZ()) > HORIZONTAL_RADIUS) continue;
					if (!isLoaded(world, nx, nz) || !canStand(world, nx, ny, nz)) continue;
					if (dy > 0 && !canStand(world, nx, current.y, nz)) continue;
					if (isHazard(world, nx, ny, nz)) continue;
					long key = key(nx, ny, nz);
					if (closed.contains(key)) continue;
					Node next = known.get(key);
					double moveCost = diagonal ? 1.414D : 1.0D;
					moveCost += Math.abs(dy) * 0.75D;
					double tentative = current.g + moveCost;
					if (next == null) {
						next = new Node(nx, ny, nz);
						known.put(key, next);
					}
					if (tentative < next.g) {
						next.parent = current;
						next.g = tentative;
						next.f = tentative + heuristic(next, target);
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

	private static boolean canStand(World world, int x, int y, int z) {
		if (y < 1 || y > 254) return false;
		BlockPos feet = new BlockPos(x, y, z);
		return world.isAirBlock(feet) && world.isAirBlock(feet.up()) && !world.isAirBlock(feet.down());
	}

	private static boolean isHazard(World world, int x, int y, int z) {
		Block feet = world.getBlockState(new BlockPos(x, y, z)).getBlock();
		Block support = world.getBlockState(new BlockPos(x, y - 1, z)).getBlock();
		return feet == Blocks.FIRE || feet == Blocks.LAVA || feet == Blocks.FLOWING_LAVA
				|| support == Blocks.FIRE || support == Blocks.LAVA || support == Blocks.FLOWING_LAVA
				|| support == Blocks.CACTUS;
	}

	private static double heuristic(Node node, BlockPos target) {
		return Math.abs(node.x - target.getX()) + Math.abs(node.y - target.getY()) + Math.abs(node.z - target.getZ());
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
