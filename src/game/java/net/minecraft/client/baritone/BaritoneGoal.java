package net.minecraft.client.baritone;

import net.minecraft.util.math.BlockPos;

/**
 * Minimal port of Baritone v1.2.19's goal contract and basic block goal.
 * The Forge-only BetterBlockPos/settings/rendering dependencies were removed
 * so the same goal math can run in the WASM client.
 *
 * Upstream: d9cb2d91a06501c5bcba2181509d0df80361f413
 */
public interface BaritoneGoal {
	boolean isInGoal(int x, int y, int z);
	double heuristic(int x, int y, int z);

	default boolean isInGoal(BlockPos pos) { return isInGoal(pos.getX(), pos.getY(), pos.getZ()); }
	default double heuristic(BlockPos pos) { return heuristic(pos.getX(), pos.getY(), pos.getZ()); }

	final class Block implements BaritoneGoal {
		private final int x, y, z;
		public Block(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
		public boolean isInGoal(int x, int y, int z) { return this.x == x && this.y == y && this.z == z; }
		public double heuristic(int x, int y, int z) { return calculate(x - this.x, y - this.y, z - this.z); }
	}

	final class XZ implements BaritoneGoal {
		private static final double SQRT_2 = Math.sqrt(2.0D);
		private final int x, z;
		public XZ(int x, int z) { this.x = x; this.z = z; }
		public boolean isInGoal(int x, int y, int z) { return this.x == x && this.z == z; }
		public double heuristic(int x, int y, int z) { return calculate(x - this.x, z - this.z); }
		private static double calculate(double xDiff, double zDiff) {
			double x = Math.abs(xDiff), z = Math.abs(zDiff);
			double straight = Math.max(x, z) - Math.min(x, z);
			double diagonal = Math.min(x, z) * SQRT_2;
			return straight + diagonal;
		}
	}

	static double calculate(double xDiff, double yDiff, double zDiff) {
		return Math.abs(yDiff) + XZ.calculate(xDiff, zDiff);
	}
}
