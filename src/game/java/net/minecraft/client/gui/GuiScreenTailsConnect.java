package net.minecraft.client.gui;

import java.io.IOException;
import net.lax1dude.eaglercraft.tailsconnect.TailsConnectClient;
import net.minecraft.client.resources.I18n;

public class GuiScreenTailsConnect extends GuiScreen {
	private final GuiScreen parent;
	private final boolean sharing;
	private GuiTextField roomCode;
	private int players = 2;
	public GuiScreenTailsConnect(GuiScreen parent, boolean sharing) { this.parent = parent; this.sharing = sharing; }
	public void initGui() {
		buttonList.clear(); buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 219, 98, 20, I18n.format("menu.tailsConnect.back")));
		buttonList.add(new GuiButton(5, width / 2 + 2, height / 4 + 219, 98, 20, "Stop / Cancel"));
		roomCode = new GuiTextField(2, fontRendererObj, width / 2 - 100, height / 4 + 48, 200, 20); roomCode.setMaxStringLength(6);
		buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 75, 200, 20, I18n.format("menu.tailsConnect.join")));
		buttonList.add(new GuiButton(7, width / 2 - 100, height / 4 + 99, 200, 20, "Search Worlds"));
		buttonList.add(new GuiButton(3, width / 2 - 100, height / 4 + 123, 200, 20, I18n.format("menu.tailsConnect.matchmake")));
		buttonList.add(new GuiButton(4, width / 2 - 100, height / 4 + 147, 200, 20, I18n.format("menu.tailsConnect.hostGame")));
		buttonList.add(new GuiButton(6, width / 2 - 100, height / 4 + 171, 200, 20, playerLabel()));
		buttonList.add(new GuiButton(8, width / 2 - 100, height / 4 + 195, 200, 20, publicLabel()));
	}
	private String playerLabel() { return "Players: " + players + " (including host)"; }
	private String publicLabel() { return "Public lobby: " + (TailsConnectClient.isPublicLobby() ? "On" : "Off"); }
	public boolean doesGuiPauseGame() { return false; }
	protected void actionPerformed(GuiButton b) throws IOException { if (b.id == 0) mc.displayGuiScreen(parent); else if (b.id == 5) TailsConnectClient.reset(); else if (b.id == 1) TailsConnectClient.join("ecraft", roomCode.getText().trim().toUpperCase()); else if (b.id == 7) TailsConnectClient.searchWorlds(); else if (b.id == 3) TailsConnectClient.matchmaking("ecraft", players); else if (b.id == 4) TailsConnectClient.host("ecraft", players); else if (b.id == 6) { players = players == 4 ? 2 : players + 1; b.displayString = playerLabel(); } else if (b.id == 8) { TailsConnectClient.setPublicLobby(!TailsConnectClient.isPublicLobby()); b.displayString = publicLabel(); } }
	public void drawScreen(int x, int y, float p) { drawDefaultBackground(); drawCenteredString(fontRendererObj, I18n.format("menu.tailsConnect"), width / 2, height / 4 - 10, 16777215); drawCenteredString(fontRendererObj, TailsConnectClient.getState(), width / 2, height / 4 + 15, 11184810); if (roomCode != null) roomCode.drawTextBox(); String s = TailsConnectClient.getRoomCode(); if (s != null) drawCenteredString(fontRendererObj, s, width / 2, height / 4 + 38, 5635925); if (TailsConnectClient.getError() != null) drawCenteredString(fontRendererObj, TailsConnectClient.getError(), width / 2, height / 4 + 38, 16733525); super.drawScreen(x, y, p); }
	protected void keyTyped(char c, int k) throws IOException { if (roomCode != null) roomCode.textboxKeyTyped(c, k); super.keyTyped(c, k); }
	public void mouseClicked(int x, int y, int b) throws IOException { super.mouseClicked(x, y, b); if (roomCode != null) roomCode.mouseClicked(x, y, b); }
}
