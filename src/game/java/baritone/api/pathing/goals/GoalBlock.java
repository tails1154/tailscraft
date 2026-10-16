/*
 * This file is part of Baritone and is distributed under the LGPL-3.0-or-later.
 */

package baritone.api.pathing.goals;

import net.minecraft.util.math.BlockPos;

/** Specific block-position goal from Baritone 1.2.19. */
public class GoalBlock implements Goal {
    public final int x;
    public final int y;
    public final int z;

    public GoalBlock(BlockPos pos) { this(pos.getX(), pos.getY(), pos.getZ()); }
    public GoalBlock(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }

    @Override
    public boolean isInGoal(int x, int y, int z) { return x == this.x && y == this.y && z == this.z; }

    @Override
    public double heuristic(int x, int y, int z) { return calculate(x - this.x, y - this.y, z - this.z); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoalBlock goal = (GoalBlock) o;
        return x == goal.x && y == goal.y && z == goal.z;
    }

    @Override
    public int hashCode() { return (x * 31 + y) * 31 + z; }

    public static double calculate(double xDiff, int yDiff, double zDiff) {
        return GoalYLevel.calculate(0, yDiff) + GoalXZ.calculate(xDiff, zDiff);
    }

    public BlockPos getGoalPos() { return new BlockPos(x, y, z); }
}
