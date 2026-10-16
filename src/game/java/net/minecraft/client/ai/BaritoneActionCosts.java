package net.minecraft.client.ai;

/**
 * Port of the movement constants from Baritone v1.2.19's ActionCosts.
 * Values are measured in approximate game ticks.
 *
 * Upstream: d9cb2d91a06501c5bcba2181509d0df80361f413
 */
public final class BaritoneActionCosts {
	public static final double WALK_ONE_BLOCK_COST = 20.0D / 4.317D;
	public static final double SPRINT_ONE_BLOCK_COST = 20.0D / 5.612D;
	public static final double WALK_OFF_BLOCK_COST = WALK_ONE_BLOCK_COST * 0.8D;
	public static final double CENTER_AFTER_FALL_COST = WALK_ONE_BLOCK_COST - WALK_OFF_BLOCK_COST;
	public static final double FALL_1_25_BLOCKS_COST = distanceToTicks(1.25D);
	public static final double FALL_0_25_BLOCKS_COST = distanceToTicks(0.25D);
	public static final double JUMP_ONE_BLOCK_COST = FALL_1_25_BLOCKS_COST - FALL_0_25_BLOCKS_COST;
	public static final double BREAK_ONE_BLOCK_COST = 30.0D;
	public static final double PLACE_ONE_BLOCK_COST = WALK_ONE_BLOCK_COST;

	private BaritoneActionCosts() { }

	public static double fallCost(int blocks) {
		return distanceToTicks(Math.max(0, Math.min(256, blocks)));
	}

	private static double velocity(int ticks) { return (Math.pow(0.98D, ticks) - 1.0D) * -3.92D; }
	private static double distanceToTicks(double distance) {
		if (distance == 0.0D) return 0.0D;
		double remaining = distance;
		int ticks = 0;
		while (true) {
			double fallDistance = velocity(ticks);
			if (remaining <= fallDistance) return ticks + remaining / fallDistance;
			remaining -= fallDistance;
			++ticks;
		}
	}
}
