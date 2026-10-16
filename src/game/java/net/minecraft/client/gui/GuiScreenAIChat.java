package net.minecraft.client.gui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.lax1dude.eaglercraft.internal.PlatformRuntime;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Surroundings-aware assistant using an asynchronous public text endpoint. */
public class GuiScreenAIChat extends GuiScreen {
	private static final String AI_ENDPOINT = "https://text.pollinations.ai/";
	private final GuiScreen parent;
	private final List<String> messages = new ArrayList<String>();
	private GuiTextField input;
	private boolean waiting;

	public GuiScreenAIChat(GuiScreen parent) {
		this.parent = parent;
	}

	public void initGui() {
		buttonList.clear();
		input = new GuiTextField(0, fontRendererObj, width / 2 - 150, height - 48, 240, 20);
		input.setMaxStringLength(180);
		buttonList.add(new GuiButton(1, width / 2 + 96, height - 48, 54, 20, "Send"));
		buttonList.add(new GuiButton(2, width / 2 - 100, height - 24, 200, 20, "Back"));
		if (messages.isEmpty()) messages.add("AI: I can inspect your nearby surroundings. Ask where you are, what is nearby, or how you are doing.");
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 2) mc.displayGuiScreen(parent);
		else if (button.id == 1) sendMessage();
	}

	private void sendMessage() {
		if (waiting || input == null) return;
		String question = input.getText().trim();
		if (question.length() == 0) return;
		input.setText("");
		messages.add("You: " + question);
		messages.add("AI: Thinking...");
		waiting = true;
		final int pendingLine = messages.size() - 1;
		try {
			String url = AI_ENDPOINT + URLEncoder.encode(buildPrompt(question), "UTF-8");
			PlatformRuntime.downloadRemoteURIByteArray(url, false, data -> {
				String answer = data == null || data.length == 0 ? null : new String(data, StandardCharsets.UTF_8).trim();
				if (answer == null || answer.length() == 0 || answer.length() > 1200) answer = localAnswer(question);
				messages.set(pendingLine, "AI: " + answer.replace('\n', ' '));
				waiting = false;
			});
		} catch (Throwable t) {
			messages.set(pendingLine, "AI: " + localAnswer(question));
			waiting = false;
		}
	}

	private String buildPrompt(String question) {
		return "You are the friendly TailsCraft in-game assistant. Answer briefly and plainly. "
				+ "Use only this live sanitized Minecraft surroundings snapshot; do not claim to see anything else. "
				+ "Snapshot: " + surroundings() + " Player question: " + question;
	}

	private String localAnswer(String question) {
		String q = question.toLowerCase();
		String state = surroundings();
		if (q.indexOf("where") >= 0 || q.indexOf("position") >= 0 || q.indexOf("location") >= 0) return "You are at " + state + ".";
		if (q.indexOf("near") >= 0 || q.indexOf("around") >= 0 || q.indexOf("see") >= 0) return "Nearby surroundings: " + state + ".";
		return "I can currently see: " + state + ". Ask me where you are, what is nearby, or about your health.";
	}

	private String surroundings() {
		EntityPlayerSP player = mc.player;
		World world = mc.world;
		if (player == null || world == null) return "no world is loaded";
		StringBuilder out = new StringBuilder();
		out.append("position ").append((int) player.posX).append(", ").append((int) player.posY).append(", ").append((int) player.posZ)
				.append(", dimension ").append(player.dimension).append(", health ").append((int) player.getHealth()).append("/")
				.append((int) player.getMaxHealth()).append(", hunger ").append(player.getFoodStats().getFoodLevel())
				.append(", time ").append(world.getWorldTime() % 24000L);
		Set<String> blocks = new LinkedHashSet<String>();
		int px = (int) Math.floor(player.posX), py = (int) Math.floor(player.posY), pz = (int) Math.floor(player.posZ);
		for (int x = px - 2; x <= px + 2; ++x) for (int y = py - 1; y <= py + 1; ++y) for (int z = pz - 2; z <= pz + 2; ++z) {
			Block block = world.getBlockState(new BlockPos(x, y, z)).getBlock();
			if (block != null) blocks.add(block.getLocalizedName());
		}
		out.append(", nearby blocks ").append(joinLimited(blocks, 8));
		int entityCount = 0;
		StringBuilder nearby = new StringBuilder();
		for (Object obj : world.loadedEntityList) {
			if (!(obj instanceof Entity) || obj == player) continue;
			Entity entity = (Entity) obj;
			if (entity.getDistanceToEntity(player) > 24.0f) continue;
			if (entityCount++ > 0) nearby.append(", ");
			nearby.append(entity.getName());
			if (entityCount >= 8) break;
		}
		out.append(", nearby entities ").append(entityCount == 0 ? "none" : nearby.toString());
		return out.toString();
	}

	private String joinLimited(Set<String> values, int limit) {
		StringBuilder out = new StringBuilder();
		int i = 0;
		for (String value : values) {
			if (i++ > 0) out.append(", ");
			out.append(value);
			if (i >= limit) break;
		}
		return out.length() == 0 ? "none" : out.toString();
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "TailsCraft AI Chatbot", width / 2, 16, 0xFFFFFF);
		drawCenteredString(fontRendererObj, waiting ? "Asking the free AI service..." : "Only this request's surroundings snapshot is shared", width / 2, 30, 0xAAAAAA);
		int maxLines = Math.max(1, (height - 92) / 12);
		int start = Math.max(0, messages.size() - maxLines);
		for (int i = start; i < messages.size(); ++i) drawString(fontRendererObj, fontRendererObj.trimStringToWidth(messages.get(i), width - 32), 16, 48 + (i - start) * 12, 0xEEEEEE);
		input.drawTextBox();
		if (input.getText().isEmpty() && !input.isFocused()) drawString(fontRendererObj, "Ask about your surroundings...", input.xPosition + 4, input.yPosition + 6, 0x777777);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (keyCode == 1) { mc.displayGuiScreen(parent); return; }
		if (keyCode == 28 || keyCode == 156) { sendMessage(); return; }
		input.textboxKeyTyped(typedChar, keyCode);
	}

	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		input.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
