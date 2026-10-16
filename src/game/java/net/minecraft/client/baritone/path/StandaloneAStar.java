package net.minecraft.client.baritone.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Dependency-free incremental A* engine.
 *
 * The engine knows nothing about Minecraft, Forge, renderers, or threads. A
 * caller supplies a bounded movement adapter and a goal. Searches can be
 * advanced in small budgets so a frame/tick is never blocked by pathfinding.
 */
public final class StandaloneAStar {
	private StandaloneAStar() { }

	public interface MovementAdapter {
		boolean canEnter(int x, int y, int z);
		double cost(int fromX, int fromY, int fromZ, int toX, int toY, int toZ);
		boolean isWithinSearchBounds(int x, int y, int z);
	}

	public interface Goal {
		boolean isReached(int x, int y, int z);
		double heuristic(int x, int y, int z);
	}

	public static final class Search {
		private final MovementAdapter movement;
		private final Goal goal;
		private final int startX, startY, startZ;
		private final PriorityQueue<Node> open = new PriorityQueue<Node>(Comparator.comparingDouble(node -> node.f));
		private final Map<Long, Node> known = new HashMap<Long, Node>();
		private final Set<Long> closed = new HashSet<Long>();
		private final int maxNodes;
		private List<Point> path = Collections.emptyList();
		private int expanded;
		private boolean done;

		public Search(int startX, int startY, int startZ, Goal goal, MovementAdapter movement, int maxNodes) {
			this.startX = startX;
			this.startY = startY;
			this.startZ = startZ;
			this.goal = goal;
			this.movement = movement;
			this.maxNodes = Math.max(1, maxNodes);
			Node origin = new Node(startX, startY, startZ);
			origin.g = 0.0D;
			origin.f = goal.heuristic(startX, startY, startZ);
			open.add(origin);
			known.put(key(startX, startY, startZ), origin);
		}

		public boolean isDone() { return done; }
		public int getExpanded() { return expanded; }
		public List<Point> getPath() { return path; }

		public void step(int budget) {
			if (done) return;
			int remaining = Math.max(1, budget);
			while (!open.isEmpty() && remaining-- > 0 && expanded < maxNodes) {
				Node current = open.poll();
				if (!closed.add(current.key)) continue;
				++expanded;
				if (goal.isReached(current.x, current.y, current.z)) {
					path = buildPath(current);
					done = true;
					return;
				}
				for (int dx = -1; dx <= 1; ++dx) for (int dz = -1; dz <= 1; ++dz) {
					if (dx == 0 && dz == 0) continue;
					boolean diagonal = dx != 0 && dz != 0;
					if (diagonal && (!movement.canEnter(current.x + dx, current.y, current.z)
							|| !movement.canEnter(current.x, current.y, current.z + dz))) continue;
					for (int dy = 1; dy >= -3; --dy) {
						int nx = current.x + dx, ny = current.y + dy, nz = current.z + dz;
						if (!movement.isWithinSearchBounds(nx, ny, nz) || !movement.canEnter(nx, ny, nz)) continue;
						if (dy > 0 && !movement.canEnter(nx, current.y, nz)) continue;
						long nodeKey = key(nx, ny, nz);
						if (closed.contains(nodeKey)) continue;
						Node next = known.get(nodeKey);
						if (next == null) {
							next = new Node(nx, ny, nz);
							known.put(nodeKey, next);
						}
						double tentative = current.g + movement.cost(current.x, current.y, current.z, nx, ny, nz);
						if (tentative < next.g) {
							next.parent = current;
							next.g = tentative;
							next.f = tentative + goal.heuristic(nx, ny, nz);
							open.add(next);
						}
					}
				}
			}
			if (open.isEmpty() || expanded >= maxNodes) {
				path = Collections.emptyList();
				done = true;
			}
		}

		private List<Point> buildPath(Node end) {
			List<Point> result = new ArrayList<Point>();
			for (Node node = end; node.parent != null; node = node.parent) result.add(new Point(node.x, node.y, node.z));
			Collections.reverse(result);
			return result;
		}
	}

	public static final class Point {
		public final int x, y, z;
		public Point(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
	}

	private static final class Node {
		final int x, y, z;
		final long key;
		double g = Double.POSITIVE_INFINITY, f = Double.POSITIVE_INFINITY;
		Node parent;
		Node(int x, int y, int z) { this.x = x; this.y = y; this.z = z; this.key = key(x, y, z); }
	}

	private static long key(int x, int y, int z) {
		return (((long)x) << 42) ^ (((long)y) << 21) ^ (z & 0x1FFFFFL);
	}
}
