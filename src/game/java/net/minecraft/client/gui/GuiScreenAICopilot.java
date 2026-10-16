package net.minecraft.client.gui;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.client.ai.AICopilotAgent;
import net.minecraft.client.ai.AICopilotController;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;

/** Simple natural-language entry point for the bounded AI copilot agent. */
public class GuiScreenAICopilot extends GuiScreen {
	private final GuiScreen parent;
	private GuiTextField commandField;
	private String status = "Stopped";

	public GuiScreenAICopilot(GuiScreen parent) { this.parent = parent; }

	public boolean doesGuiPauseGame() { return false; }

	public void initGui() {
		buttonList.clear();
		int left = width / 2 - 150;
		commandField = new GuiTextField(7, fontRendererObj, left, 92, 300, 20);
		commandField.setMaxStringLength(180);
		buttonList.add(new GuiButton(7, left, 126, 300, 20, "Start Copilot"));
		buttonList.add(new GuiButton(4, left, 150, 300, 20, "Stop Copilot"));
		buttonList.add(new GuiButton(6, width / 2 - 100, height - 28, 200, 20, "Back"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 7) {
			String request = commandField.getText().trim();
			if (request.length() == 0) { status = "Describe a task, such as: mine diamonds"; return; }
			status = "Observing and planning...";
			AICopilotAgent.start(request, this::context);
			mc.displayGuiScreen(new GuiScreenAICopilotOverlay());
		} else if (button.id == 4) {
			AICopilotAgent.stop(); AICopilotController.stop(); status = "Stopped";
		} else if (button.id == 6) {
			AICopilotAgent.stop(); AICopilotController.stop(); mc.displayGuiScreen(parent);
		}
	}

	public void updateScreen() {
		super.updateScreen();
		boolean locked = AICopilotAgent.isActive() || AICopilotController.isActive();
		commandField.setEnabled(!locked);
		for (GuiButton button : buttonList) button.enabled = !locked || button.id == 4 || button.id == 6;
	}

	private String context() {
		if (mc.player == null) return "no world loaded";
		StringBuilder items = new StringBuilder();
		for (int i = 0; i < mc.player.inventory.mainInventory.size(); ++i) {
			net.minecraft.item.ItemStack stack = mc.player.inventory.mainInventory.get(i);
			if (stack != null && !stack.func_190926_b()) {
				if (items.length() > 0) items.append(", ");
				items.append(i < 9 ? "hotbar:" : "inventory:").append(stack.getDisplayName()).append(" x").append(stack.func_190916_E());
			}
		}
		StringBuilder observation = new StringBuilder();
		int px = (int) Math.floor(mc.player.posX);
		int py = (int) Math.floor(mc.player.posY);
		int pz = (int) Math.floor(mc.player.posZ);
		int seen = 0;
		for (int y = py - 2; y <= py + 2 && seen < 120; ++y) {
			for (int z = pz - 4; z <= pz + 4 && seen < 120; ++z) {
				for (int x = px - 4; x <= px + 4 && seen < 120; ++x) {
					BlockPos pos = new BlockPos(x, y, z);
					if (mc.world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4) == null || mc.world.isAirBlock(pos)) continue;
					Block block = mc.world.getBlockState(pos).getBlock();
					ResourceLocation id = Block.REGISTRY.getNameForObject(block);
					if (observation.length() > 0) observation.append(";");
					observation.append(id == null ? block.getLocalizedName() : id.toString()).append("@").append(x).append(",").append(y).append(",").append(z);
					++seen;
				}
			}
		}
		String targeted = "none";
		if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK) {
			BlockPos target = mc.objectMouseOver.getBlockPos();
			if (target != null) {
				Block block = mc.world.getBlockState(target).getBlock();
				ResourceLocation id = Block.REGISTRY.getNameForObject(block);
				targeted = (id == null ? block.getLocalizedName() : id.toString()) + "@" + target.getX() + "," + target.getY() + "," + target.getZ();
			}
		}
		return "player position=" + px + "," + py + "," + pz + " dimension=" + mc.player.dimension
				+ " items=" + items.toString() + " targeted_block=" + targeted + " nearby_blocks=" + observation.toString();
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "AI Copilot", width / 2, 28, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "Describe what you want it to do", width / 2, 52, 0xAAAAAA);
		drawString(fontRendererObj, "Task", width / 2 - 150, 80, 0xFFFFFF);
		commandField.drawTextBox();
		if (commandField.getText().isEmpty() && !commandField.isFocused()) drawString(fontRendererObj, "Example: find diamonds and mine them", commandField.xPosition + 4, commandField.yPosition + 6, 0x777777);
		drawCenteredString(fontRendererObj, status, width / 2, 184, 0x55FF55);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	protected void keyTyped(char c, int keyCode) throws IOException {
		if (keyCode == 1) {
			AICopilotAgent.stop(); AICopilotController.stop(); mc.displayGuiScreen(parent); return;
		}
		commandField.textboxKeyTyped(c, keyCode);
	}

	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		commandField.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
