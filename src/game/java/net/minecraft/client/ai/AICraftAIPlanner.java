package net.minecraft.client.ai;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

import net.lax1dude.eaglercraft.internal.PlatformRuntime;

/** Converts a natural-language request into the constrained AICraftScriptLangPlus language. */
public final class AICraftAIPlanner {
	private static final String ENDPOINT = "https://text.pollinations.ai/";

	private AICraftAIPlanner() { }

	public static void plan(String request, String context, Consumer<String> callback) {
		String prompt = "You are the TailsCraft command planner. Return ONLY one AICraftScriptLangPlus script, no prose. "
				+ "Allowed syntax: SELECT_ITEM item_name; FIND_BLOCK block_name; IF FAIL: EXPLORE until FIND_BLOCK block_name; MINE block_name; STOP. "
				+ "SELECT_ITEM may only choose an item already present in the player's hotbar. Do not output commands for chat, combat, containers, crafting, or block placement. "
				+ "The context contains nearby_blocks as block_id@x,y,z entries and targeted_block; reason from those text observations only. "
				+ "User request: " + request + " Context: " + context;
		try {
			String url = ENDPOINT + URLEncoder.encode(prompt, "UTF-8");
			PlatformRuntime.downloadRemoteURIByteArray(url, false, data -> {
				String script = data == null || data.length == 0 ? null : new String(data, StandardCharsets.UTF_8).trim();
				callback.accept(script == null ? fallback(request) : script);
			});
		} catch (Throwable t) {
			callback.accept(fallback(request));
		}
	}

	private static String fallback(String request) {
		String lower = request == null ? "" : request.toLowerCase();
		if (lower.indexOf("diamond") >= 0) return "SELECT_ITEM pickaxe;FIND_BLOCK diamond_ore;IF FAIL: EXPLORE until FIND_BLOCK diamond_ore;MINE diamond_ore;STOP";
		return "STOP";
	}
}
