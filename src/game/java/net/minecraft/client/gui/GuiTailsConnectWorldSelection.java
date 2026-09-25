package net.minecraft.client.gui;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;

import net.lax1dude.eaglercraft.tailsconnect.TailsConnectClient;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.storage.WorldSummary;

/** A paged, card-based picker for selecting a local world to publish to TC5. */
public class GuiTailsConnectWorldSelection extends GuiScreen {
	private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
	private static final int CARD_HEIGHT = 58;
	// Three cards fit comfortably above the bottom controls at the smallest
	// supported game window size.
	private static final int CARDS_PER_PAGE = 3;
	private final GuiScreen parent;
	private final int players;
	private List<WorldSummary> worlds;
	private int page;
	private int selected = -1;
	private GuiButton hostButton;
	private GuiButton previousButton;
	private GuiButton nextButton;

	public GuiTailsConnectWorldSelection(GuiScreen parent, int players) {
		this.parent = parent;
		this.players = players;
	}

	@Override
	public void initGui() {
		loadWorlds();
		buttonList.clear();
		int left = width / 2 - 150;
		int bottom = height - 28;
		hostButton = addButton(new GuiButton(1, left - 70, bottom, 140, 20, "Host selected world"));
		previousButton = addButton(new GuiButton(2, left + 78, bottom, 70, 20, "Previous"));
		nextButton = addButton(new GuiButton(3, left + 154, bottom, 70, 20, "Next"));
		addButton(new GuiButton(4, left + 230, bottom, 70, 20, "Back"));
		updateButtons();
	}

	private void loadWorlds() {
		try {
			worlds = mc.getSaveLoader().getSaveList();
		} catch (Exception ex) {
			worlds = new java.util.ArrayList<WorldSummary>();
		}
		if (page > maxPage()) page = maxPage();
		if (selected >= worlds.size()) selected = -1;
	}

	private int maxPage() {
		return worlds == null || worlds.isEmpty() ? 0 : (worlds.size() - 1) / CARDS_PER_PAGE;
	}

	private void updateButtons() {
		if (hostButton != null) hostButton.enabled = selected >= 0 && selected < worlds.size();
		if (previousButton != null) previousButton.enabled = page > 0;
		if (nextButton != null) nextButton.enabled = page < maxPage();
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		if (button.id == 1 && button.enabled) {
			WorldSummary world = worlds.get(selected);
			if (mc.getSaveLoader().canLoadWorld(world.getFileName())) {
				mc.launchIntegratedServer(world.getFileName(), world.getDisplayName(), null);
				TailsConnectClient.queueHosted(players);
			}
		} else if (button.id == 2 && page > 0) {
			page--;
			selected = -1;
			updateButtons();
		} else if (button.id == 3 && page < maxPage()) {
			page++;
			selected = -1;
			updateButtons();
		} else if (button.id == 4) {
			mc.displayGuiScreen(parent);
		}
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "Host a TC5 Cloud World", width / 2, 14, 0xFFFFFF);
		drawCenteredString(fontRendererObj, worlds.isEmpty() ? "No local worlds found" : "Choose a world to upload and host",
				width / 2, 28, 0xAAAAAA);
		int left = Math.max(20, width / 2 - 220);
		int cardWidth = Math.min(440, width - 40);
		int first = page * CARDS_PER_PAGE;
		for (int row = 0; row < CARDS_PER_PAGE; row++) {
			int index = first + row;
			if (index >= worlds.size()) break;
			int top = 46 + row * CARD_HEIGHT;
			WorldSummary world = worlds.get(index);
			boolean isSelected = selected == index;
			Gui.drawRect(left, top, left + cardWidth, top + CARD_HEIGHT - 5,
					isSelected ? 0xFF315A78 : 0xFF20252B);
			Gui.drawRect(left, top, left + 5, top + CARD_HEIGHT - 5,
					isSelected ? 0xFF55B7FF : 0xFF3B4652);
			String name = world.getDisplayName();
			if (name == null || name.trim().isEmpty()) name = world.getFileName();
			name = fontRendererObj.trimStringToWidth(name, cardWidth - 24);
			String details = world.getEnumGameType().getName();
			if (world.isHardcoreModeEnabled()) details = "hardcore";
			if (world.getCheatsEnabled()) details += ", cheats";
			String date = DATE_FORMAT.format(new Date(world.getLastTimePlayed()));
			fontRendererObj.drawString(name, left + 14, top + 8, 0xFFFFFF);
			fontRendererObj.drawString(details, left + 14, top + 24, 0xB8C4CF);
			fontRendererObj.drawString(date + "  -  " + world.getFileName(), left + 14, top + 39, 0x7F8A95);
			if (world.requiresConversion()) {
				fontRendererObj.drawString(TextFormatting.YELLOW + "Conversion may be required", left + cardWidth - 150, top + 8, 0xFFFFFF);
			}
		}
		drawCenteredString(fontRendererObj, worlds.isEmpty() ? "" : "Page " + (page + 1) + " / " + (maxPage() + 1),
				width / 2, height - 43, 0xAAAAAA);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		int left = Math.max(20, width / 2 - 220);
		int cardWidth = Math.min(440, width - 40);
		if (mouseX < left || mouseX > left + cardWidth) return;
		int row = (mouseY - 46) / CARD_HEIGHT;
		if (row < 0 || row >= CARDS_PER_PAGE || (mouseY - 46) % CARD_HEIGHT >= CARD_HEIGHT - 5) return;
		int index = page * CARDS_PER_PAGE + row;
		if (index >= 0 && index < worlds.size()) {
			selected = index;
			updateButtons();
		}
	}
}
