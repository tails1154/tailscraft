package net.minecraft.client.gui;

import java.io.IOException;
import java.util.Arrays;
import net.lax1dude.eaglercraft.tailsconnect.TailsConnectClient;
import net.minecraft.client.resources.I18n;

/** Clear, compact TailsConnect room setup screen. */
public class GuiScreenTailsConnect extends GuiScreen {
	private final GuiScreen parent;
	private final boolean sharing;
	private GuiTextField roomCode;
	private int players = 2;

	public GuiScreenTailsConnect(GuiScreen parent, boolean sharing) {
		this.parent = parent;
		this.sharing = sharing;
	}

	private int panelTop() {
		return Math.max(24, Math.min(82, (height - 248) / 2));
	}

	public void initGui() {
		String savedCode = roomCode == null ? "" : roomCode.getText();
		buttonList.clear();
		int left = width / 2 - 150;
		int top = panelTop();
		roomCode = new GuiTextField(2, fontRendererObj, left, top + 14, 300, 20);
		roomCode.setMaxStringLength(6);
		roomCode.setText(savedCode);

		buttonList.add(new GuiButton(1, left, top + 38, 300, 20, "Join World"));
		buttonList.add(new GuiButton(7, left, top + 62, 300, 20, "Search Public Worlds"));
		buttonList.add(new GuiButton(6, left, top + 104, 146, 20, playerLabel()));
		buttonList.add(new GuiButton(8, left + 154, top + 104, 146, 20, publicLabel()));
		buttonList.add(new GuiButton(4, left, top + 128, 300, 20, "Host World"));
		buttonList.add(new GuiButton(3, left, top + 152, 300, 20, "Find Match"));
		buttonList.add(new GuiButton(9, left, top + 176, 146, 20, "World Transfer"));
		buttonList.add(new GuiButton(10, left + 154, top + 176, 146, 20, "Friends"));
		buttonList.add(new GuiButton(0, left, top + 206, 146, 20, I18n.format("menu.tailsConnect.back")));
		buttonList.add(new GuiButton(5, left + 154, top + 206, 146, 20, "Stop / Cancel"));
	}

	private String playerLabel() { return "Players: " + players; }
	private String publicLabel() { return "Public: " + (TailsConnectClient.isPublicLobby() ? "ON" : "OFF"); }
	public boolean doesGuiPauseGame() { return false; }

	private void openWorldSearch() {
		TailsConnectClient.searchWorlds();
		mc.displayGuiScreen(new GuiTailsConnectWorlds(this));
	}

	private void openWorldTransfer() {
		mc.displayGuiScreen(new GuiScreenTailsConnectTransfer(this));
	}

	private void openFriends() {
		mc.displayGuiScreen(new GuiScreenTailsConnectFriends(this));
	}

	protected void actionPerformed(GuiButton b) throws IOException {
		if (b.id == 0) mc.displayGuiScreen(parent);
		else if (b.id == 5) TailsConnectClient.reset();
		else if (b.id == 1) TailsConnectClient.join("ecraft", roomCode.getText().trim().toUpperCase());
		else if (b.id == 7) openWorldSearch();
		else if (b.id == 3) TailsConnectClient.matchmaking("ecraft", players);
		else if (b.id == 4) TailsConnectClient.host("ecraft", players);
		else if (b.id == 6) { players = players == 4 ? 2 : players + 1; b.displayString = playerLabel(); }
		else if (b.id == 8) { TailsConnectClient.setPublicLobby(!TailsConnectClient.isPublicLobby()); b.displayString = publicLabel(); }
		else if (b.id == 9) openWorldTransfer();
		else if (b.id == 10) openFriends();
	}

	private String tooltipFor(GuiButton button) {
		switch (button.id) {
		case 1: return "Join using the 6-character code in the box above.";
		case 7: return "Browse public worlds that are currently online.";
		case 6: return "Click to choose 2, 3, or 4 players.";
		case 8: return "Public worlds appear in search; private worlds use a code only.";
		case 4: return "Start your singleplayer world and let others join it.";
		case 3: return "Wait in the background until players are matched.";
		case 5: return "Cancel matchmaking or disconnect from the current world.";
		case 9: return "Send the host's saved world to connected players as a new local world.";
		case 10: return "Manage your TC4 friends and friend requests.";
		case 0: return "Return to the previous menu.";
		default: return null;
		}
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		int left = width / 2 - 150;
		int top = panelTop();
		drawCenteredString(fontRendererObj, "TailsConnect", width / 2, top - 25, 0xFFFFFF);
		drawCenteredString(fontRendererObj, TailsConnectClient.getState(), width / 2, top - 11, 0xAAAAAA);
		drawString(fontRendererObj, "Room code (from a friend):", left, top + 2, 0xFFFFFF);
		roomCode.drawTextBox();
		if (roomCode.getText().isEmpty() && !roomCode.isFocused()) drawString(fontRendererObj, "Example: ABC123", left + 4, top + 20, 0x777777);
		drawString(fontRendererObj, "Host a world", left, top + 91, 0xFFFFFF);
		String detail = TailsConnectClient.getError();
		if (detail == null && TailsConnectClient.getRoomCode() != null) detail = "Room: " + TailsConnectClient.getRoomCode();
		if (detail != null) drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(detail, width - 16), width / 2, height - 18, 0x55FF55);
		super.drawScreen(mouseX, mouseY, partialTicks);
		for (GuiButton button : buttonList) if (button.hovered) {
			String tooltip = tooltipFor(button);
			if (tooltip != null) drawHoveringText(Arrays.asList(tooltip), mouseX, mouseY);
			break;
		}
	}

	protected void keyTyped(char c, int keyCode) throws IOException {
		if (roomCode != null) roomCode.textboxKeyTyped(c, keyCode);
		super.keyTyped(c, keyCode);
	}

	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (roomCode != null) roomCode.mouseClicked(mouseX, mouseY, mouseButton);
	}
}
