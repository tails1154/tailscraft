/*
 * This file is part of Baritone and is distributed under the LGPL-3.0-or-later.
 */

package baritone.api.pathing.goals;

import net.minecraft.util.math.BlockPos;

/** The Baritone goal contract used by its pathing calculator. */
public interface Goal {
    boolean isInGoal(int x, int y, int z);
    double heuristic(int x, int y, int z);

    default boolean isInGoal(BlockPos pos) { return isInGoal(pos.getX(), pos.getY(), pos.getZ()); }
    default double heuristic(BlockPos pos) { return heuristic(pos.getX(), pos.getY(), pos.getZ()); }
    default double heuristic() { return 0; }
}
