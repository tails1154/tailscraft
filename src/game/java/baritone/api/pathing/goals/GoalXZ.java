/*
 * This file is part of Baritone and is distributed under the LGPL-3.0-or-later.
 */

package baritone.api.pathing.goals;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import baritone.api.pathing.movement.ActionCosts;

/** Long-range goal that ignores the Y coordinate, from Baritone 1.2.19. */
public class GoalXZ implements Goal {
    private static final double SQRT_2 = Math.sqrt(2);
    private final int x;
    private final int z;

    public GoalXZ(int x, int z) { this.x = x; this.z = z; }
    public GoalXZ(BlockPos pos) { this(pos.getX(), pos.getZ()); }

    @Override
    public boolean isInGoal(int x, int y, int z) { return x == this.x && z == this.z; }

    @Override
    public double heuristic(int x, int y, int z) { return calculate(x - this.x, z - this.z); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GoalXZ goal = (GoalXZ) o;
        return x == goal.x && z == goal.z;
    }

    @Override
    public int hashCode() { return 31 * x + z; }

    public static double calculate(double xDiff, double zDiff) {
        double x = Math.abs(xDiff), z = Math.abs(zDiff);
        double straight = Math.max(x, z) - Math.min(x, z);
        double diagonal = Math.min(x, z) * SQRT_2;
        return (diagonal + straight) * 1.0D;
    }

    public static GoalXZ fromDirection(Vec3d origin, float yaw, double distance) {
        float theta = (float) Math.toRadians(yaw);
        return new GoalXZ(MathHelper.floor(origin.xCoord - MathHelper.sin(theta) * distance),
                MathHelper.floor(origin.zCoord + MathHelper.cos(theta) * distance));
    }

    public int getX() { return x; }
    public int getZ() { return z; }
}
