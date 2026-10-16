package net.minecraft.client.ai;

import java.util.ArrayList;
import java.util.List;

/** Tiny, deliberately constrained command language produced by the AI planner. */
public final class AICraftScriptLangPlus {
	public enum Type { FIND_BLOCK, EXPLORE, MINE, STOP }

	public static final class Step {
		public final Type type;
		public final String argument;
		public boolean exploreOnFail;
		Step(Type type, String argument) { this.type = type; this.argument = argument; }
	}

	private AICraftScriptLangPlus() { }

	public static List<Step> parse(String source) {
		List<Step> steps = new ArrayList<Step>();
		if (source == null) return steps;
		String clean = source.replace("```", "").replace('\n', ' ').trim();
		for (String raw : clean.split(";")) {
			String token = raw.trim().toUpperCase();
			if (token.length() == 0) continue;
			if (token.startsWith("IF FAIL:")) {
				if (!steps.isEmpty() && token.indexOf("EXPLORE") >= 0) steps.get(steps.size() - 1).exploreOnFail = true;
				continue;
			}
			if (token.startsWith("FIND_BLOCK ")) steps.add(new Step(Type.FIND_BLOCK, token.substring(11).trim().toLowerCase()));
			else if (token.startsWith("MINE ")) steps.add(new Step(Type.MINE, token.substring(5).trim().toLowerCase()));
			else if (token.startsWith("EXPLORE")) steps.add(new Step(Type.EXPLORE, ""));
			else if (token.equals("STOP")) steps.add(new Step(Type.STOP, ""));
		}
		return steps;
	}
}
