package net.minecraft.client.ai;

import java.util.List;
import java.util.function.Supplier;

import net.minecraft.client.Minecraft;

/** Re-observes the game and requests another constrained plan after each action. */
public final class AICopilotAgent {
	private static final int MAX_STEPS = 32;
	private static boolean active;
	private static boolean planning;
	private static int completedSteps;
	private static String request;
	private static Supplier<String> contextSupplier;
	private static String status = "Stopped";

	private AICopilotAgent() { }

	public static void start(String requestIn, Supplier<String> contextIn) {
		stop();
		AICopilotController.stop();
		request = requestIn;
		contextSupplier = contextIn;
		active = true;
		completedSteps = 0;
		status = "Observing world";
		requestNextPlan();
	}

	public static void tick(Minecraft mc) {
		if (active && !planning && !AICopilotController.isActive()) requestNextPlan();
	}

	public static boolean isActive() { return active; }
	public static String getStatus() {
		if (!active) return status;
		if (planning) return status;
		if (AICopilotController.isActive()) return "Executing: " + AICopilotController.getStatus();
		return status;
	}
	public static int getCompletedSteps() { return completedSteps; }
	public static int getMaxSteps() { return MAX_STEPS; }

	public static void stop() {
		active = false;
		planning = false;
		request = null;
		contextSupplier = null;
		status = "Stopped";
	}

	private static void requestNextPlan() {
		if (!active || planning || completedSteps >= MAX_STEPS) {
			if (completedSteps >= MAX_STEPS) stop();
			return;
		}
		planning = true;
		status = "Planning next action";
		AICraftAIPlanner.plan(request, contextSupplier == null ? "no context" : contextSupplier.get(), script -> {
			if (!active) return;
			List<AICraftScriptLangPlus.Step> steps = AICraftScriptLangPlus.parse(script);
			planning = false;
			if (steps.isEmpty() || (steps.size() == 1 && steps.get(0).type == AICraftScriptLangPlus.Type.STOP)) {
				stop();
				return;
			}
			++completedSteps;
			status = "Executing action " + completedSteps + " of " + MAX_STEPS;
			AICopilotController.runScript(steps);
		});
	}
}
