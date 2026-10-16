package net.minecraft.client.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Constrained local copilot: bounded block-grid pathfinding and camera rotation only. */
public final class AICopilotController {
	private static boolean active;
	private static int targetX, targetY, targetZ;
	private static boolean looking;
	private static int lookTicks;
	private static int recalc;
	private static List<Node> path = Collections.emptyList();

	private AICopilotController() { }

	public static void setTarget(int x, int y, int z) {
		targetX = x; targetY = y; targetZ = z; recalc = 0;
	}

	public static void start() {
		active = true;
		looking = false;
		recalc = 0;
	}

	public static void stop() {
		active = false;
		looking = false;
		path = Collections.emptyList();
		releaseKeys();
	}

	public static void lookAround() {
		active = true;
		looking = true;
		lookTicks = 0;
		releaseKeys();
	}

	public static boolean isActive() { return active; }

	public static void tick(Minecraft mc) {
		if (!active) return;
		EntityPlayerSP player = mc.player;
		World world = mc.world;
		if (player == null || world == null) { releaseKeys(); return; }
		if (looking) {
			releaseKeys();
			player.rotationYaw += 6.0F;
			player.prevRotationYaw = player.rotationYaw;
			if (++lookTicks >= 60) stop();
			return;
		}
		if (Math.abs(player.posX - targetX) < 1.2D && Math.abs(player.posY - targetY) < 2.0D && Math.abs(player.posZ - targetZ) < 1.2D) {
			stop();
			return;
		}
		if (++recalc >= 10 || path.isEmpty()) {
			path = findPath(world, new BlockPos(player), new BlockPos(targetX, targetY, targetZ));
			recalc = 0;
		}
		Node next = path.isEmpty() ? new Node(targetX, targetY, targetZ) : path.get(0);
		float yaw = (float) (Math.atan2(-(next.x + 0.5D - player.posX), next.z + 0.5D - player.posZ) * 180.0D / Math.PI);
		player.rotationYaw = yaw;
		player.prevRotationYaw = yaw;
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), true);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
		boolean jump = player.isCollidedHorizontally || next.y > (int) Math.floor(player.posY);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), jump);
	}

	private static List<Node> findPath(World world, BlockPos start, BlockPos target) {
		final int radius = 16;
		final Map<Long, Node> nodes = new HashMap<Long, Node>();
		List<Node> open = new ArrayList<Node>();
		Node origin = new Node(start.getX(), start.getY(), start.getZ());
		origin.g = 0; origin.h = distance(origin, target);
		open.add(origin); nodes.put(origin.key(), origin);
		while (!open.isEmpty()) {
			Collections.sort(open, Comparator.comparingInt(AICopilotController::score));
			Node current = open.remove(0);
			if (Math.abs(current.x - target.getX()) <= 1 && Math.abs(current.z - target.getZ()) <= 1) return buildPath(current);
			if (Math.abs(current.x - start.getX()) > radius || Math.abs(current.z - start.getZ()) > radius) continue;
			for (int i = 0; i < 4; ++i) {
				int nx = current.x + (i == 0 ? 1 : i == 1 ? -1 : 0);
				int nz = current.z + (i == 2 ? 1 : i == 3 ? -1 : 0);
				for (int ny = current.y - 1; ny <= current.y + 1; ++ny) {
					if (!canStand(world, nx, ny, nz)) continue;
					Node candidate = new Node(nx, ny, nz);
					Node known = nodes.get(candidate.key());
					int cost = current.g + 1 + Math.abs(ny - current.y) * 2;
					if (known == null || cost < known.g) {
						candidate.parent = current; candidate.g = cost; candidate.h = distance(candidate, target);
						nodes.put(candidate.key(), candidate); open.add(candidate);
					}
				}
			}
		}
		return Collections.emptyList();
	}

	private static boolean canStand(World world, int x, int y, int z) {
		BlockPos feet = new BlockPos(x, y, z);
		return world.isAirBlock(feet) && world.isAirBlock(feet.up()) && !world.isAirBlock(feet.down());
	}

	private static int distance(Node n, BlockPos target) { return Math.abs(n.x - target.getX()) + Math.abs(n.y - target.getY()) + Math.abs(n.z - target.getZ()); }
	private static int score(Node n) { return n.g + n.h; }
	private static List<Node> buildPath(Node end) {
		List<Node> ret = new ArrayList<Node>();
		for (Node n = end; n.parent != null; n = n.parent) ret.add(n);
		Collections.reverse(ret);
		return ret;
	}

	private static void releaseKeys() {
		Minecraft mc = Minecraft.getMinecraft();
		if (mc == null) return;
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.getKeyCode(), false);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), false);
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindJump.getKeyCode(), false);
	}

	private static class Node {
		final int x, y, z; int g, h; Node parent;
		Node(int x, int y, int z) { this.x = x; this.y = y; this.z = z; }
		long key() { return (((long) x) << 42) ^ (((long) y) << 21) ^ (z & 0x1FFFFF); }
	}
}
