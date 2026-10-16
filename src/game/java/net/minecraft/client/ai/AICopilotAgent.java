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

	private AICopilotAgent() { }

	public static void start(String requestIn, Supplier<String> contextIn) {
		stop();
		request = requestIn;
		contextSupplier = contextIn;
		active = true;
		completedSteps = 0;
		requestNextPlan();
	}

	public static void tick(Minecraft mc) {
		if (active && !planning && !AICopilotController.isActive()) requestNextPlan();
	}

	public static boolean isActive() { return active; }

	public static void stop() {
		active = false;
		planning = false;
		request = null;
		contextSupplier = null;
	}

	private static void requestNextPlan() {
		if (!active || planning || completedSteps >= MAX_STEPS) {
			if (completedSteps >= MAX_STEPS) stop();
			return;
		}
		planning = true;
		AICraftAIPlanner.plan(request, contextSupplier == null ? "no context" : contextSupplier.get(), script -> {
			if (!active) return;
			List<AICraftScriptLangPlus.Step> steps = AICraftScriptLangPlus.parse(script);
			planning = false;
			if (steps.isEmpty() || (steps.size() == 1 && steps.get(0).type == AICraftScriptLangPlus.Type.STOP)) {
				stop();
				return;
			}
			++completedSteps;
			AICopilotController.runScript(steps);
		});
	}
}
