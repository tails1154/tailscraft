package net.minecraft.client.baritone;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalBlock;
import net.minecraft.block.Block;
import net.minecraft.client.baritone.path.StandaloneAStar;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Minecraft movement adapter for the dependency-free TailsCraft A* core. */
public final class BaritonePathfinder {
	private static final int MAX_NODES = 4096;
	private static final int HORIZONTAL_RADIUS = 32;

	private BaritonePathfinder() { }

	public static List<BlockPos> findPath(World world, BlockPos start, BlockPos target) {
		return findPath(world, start, new GoalBlock(target.getX(), target.getY(), target.getZ()), false, false);
	}

	public static List<BlockPos> findPath(World world, BlockPos start, Goal goal, boolean allowBreak, boolean allowPlace) {
		Search search = createSearch(world, start, goal, allowBreak, allowPlace);
		if (search == null) return Collections.emptyList();
		while (!search.isDone()) search.step(MAX_NODES);
		return search.getPath();
	}

	public static Search createSearch(World world, BlockPos start, Goal goal, boolean allowBreak, boolean allowPlace) {
		return world == null || start == null || goal == null ? null : new Search(world, start, goal, allowBreak, allowPlace);
	}

	public static final class Search {
		private final StandaloneAStar.Search delegate;
		private List<BlockPos> path = Collections.emptyList();

		private Search(final World world, BlockPos start, final Goal goal, final boolean allowBreak, final boolean allowPlace) {
			StandaloneAStar.MovementAdapter movement = new StandaloneAStar.MovementAdapter() {
				public boolean canEnter(int x, int y, int z) {
					return isLoaded(world, x, z) && canStand(world, x, y, z, allowBreak, allowPlace) && !isHazard(world, x, y, z);
				}
				public double cost(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
					boolean diagonal = fromX != toX && fromZ != toZ;
					double cost = (diagonal ? Math.sqrt(2.0D) : 1.0D) * BaritoneActionCosts.WALK_ONE_BLOCK_COST;
					if (toY > fromY) cost += BaritoneActionCosts.JUMP_ONE_BLOCK_COST;
					if (toY < fromY) cost += BaritoneActionCosts.fallCost(fromY - toY) + BaritoneActionCosts.CENTER_AFTER_FALL_COST;
					cost += breakCost(world, toX, toY, toZ, allowBreak);
					if (allowPlace && world.isAirBlock(new BlockPos(toX, toY - 1, toZ))) cost += BaritoneActionCosts.PLACE_ONE_BLOCK_COST;
					return cost;
				}
				public boolean isWithinSearchBounds(int x, int y, int z) {
					return y >= 1 && y <= 254 && Math.abs(x - start.getX()) <= HORIZONTAL_RADIUS && Math.abs(z - start.getZ()) <= HORIZONTAL_RADIUS;
				}
			};
			StandaloneAStar.Goal adaptedGoal = new StandaloneAStar.Goal() {
				public boolean isReached(int x, int y, int z) { return goal.isInGoal(x, y, z); }
				public double heuristic(int x, int y, int z) { return goal.heuristic(x, y, z); }
			};
			delegate = new StandaloneAStar.Search(start.getX(), start.getY(), start.getZ(), adaptedGoal, movement, MAX_NODES);
		}

		public boolean isDone() { return delegate.isDone(); }
		public int getExpanded() { return delegate.getExpanded(); }
		public List<BlockPos> getPath() {
			if (path.isEmpty() && delegate.isDone()) {
				List<StandaloneAStar.Point> points = delegate.getPath();
				if (!points.isEmpty()) {
					path = new ArrayList<BlockPos>(points.size());
					for (StandaloneAStar.Point point : points) path.add(new BlockPos(point.x, point.y, point.z));
				}
			}
			return path;
		}
		public void step(int budget) { delegate.step(budget); }
	}

	private static boolean isLoaded(World world, int x, int z) { return world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4) != null; }
	private static boolean canStand(World world, int x, int y, int z, boolean allowBreak, boolean allowPlace) {
		if (y < 1 || y > 254) return false;
		BlockPos feet = new BlockPos(x, y, z);
		boolean feetClear = world.isAirBlock(feet) || allowBreak && breakable(world.getBlockState(feet).getBlock());
		boolean headClear = world.isAirBlock(feet.up()) || allowBreak && breakable(world.getBlockState(feet.up()).getBlock());
		return feetClear && headClear && (!world.isAirBlock(feet.down()) || allowPlace);
	}
	private static boolean breakable(Block block) { return block != Blocks.BEDROCK && block != Blocks.BARRIER && block != Blocks.END_PORTAL && block != Blocks.END_PORTAL_FRAME && block != Blocks.AIR && block != Blocks.LAVA && block != Blocks.FLOWING_LAVA; }
	private static double breakCost(World world, int x, int y, int z, boolean allowBreak) {
		if (!allowBreak) return 0.0D;
		double cost = 0.0D;
		if (!world.isAirBlock(new BlockPos(x, y, z))) cost += BaritoneActionCosts.BREAK_ONE_BLOCK_COST;
		if (!world.isAirBlock(new BlockPos(x, y + 1, z))) cost += BaritoneActionCosts.BREAK_ONE_BLOCK_COST;
		return cost;
	}
	private static boolean isHazard(World world, int x, int y, int z) {
		Block feet = world.getBlockState(new BlockPos(x, y, z)).getBlock();
		Block support = world.getBlockState(new BlockPos(x, y - 1, z)).getBlock();
		return feet == Blocks.FIRE || feet == Blocks.LAVA || feet == Blocks.FLOWING_LAVA || support == Blocks.FIRE || support == Blocks.LAVA || support == Blocks.FLOWING_LAVA || support == Blocks.CACTUS;
	}
}
