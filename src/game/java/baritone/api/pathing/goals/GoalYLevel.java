/*
 * This file is part of Baritone and is distributed under the LGPL-3.0-or-later.
 */

package baritone.api.pathing.goals;

import baritone.api.pathing.movement.ActionCosts;

/** Y-level goal used by Baritone's goal heuristic. */
public class GoalYLevel implements Goal, ActionCosts {
    public final int level;
    public GoalYLevel(int level) { this.level = level; }

    @Override
    public boolean isInGoal(int x, int y, int z) { return y == level; }

    @Override
    public double heuristic(int x, int y, int z) { return calculate(level, y); }

    public static double calculate(int goalY, int currentY) {
        if (currentY > goalY) return FALL_N_BLOCKS_COST[2] / 2 * (currentY - goalY);
        if (currentY < goalY) return (goalY - currentY) * JUMP_ONE_BLOCK_COST;
        return 0;
    }
}
