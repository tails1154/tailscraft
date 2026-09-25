package net.minecraft.client.gui;

import java.io.IOException;
import java.util.Arrays;
import net.lax1dude.eaglercraft.tailsconnect.TailsConnectFriends;
import org.json.JSONArray;
import org.json.JSONObject;

/** TC4 friends and requests presented as paged cards. */
public class GuiScreenTailsConnectFriends extends GuiScreen {
	private final GuiScreen parent;
	private GuiTextField friendName;
	private boolean showingRequests;
	private int page;
	public GuiScreenTailsConnectFriends(GuiScreen parent) { this.parent = parent; }
	public boolean doesGuiPauseGame() { return false; }
	private JSONArray displayed() { return showingRequests ? TailsConnectFriends.getRequests() : TailsConnectFriends.getFriends(); }
	private int pageSize() { return Math.max(1, (height - 150) / 60); }
	private int maxPage() { return Math.max(0, (displayed().length() - 1) / pageSize()); }

	public void initGui() {
		TailsConnectFriends.open(); page = Math.min(page, maxPage()); buttonList.clear();
		int left = width / 2 - 150;
		friendName = new GuiTextField(0, fontRendererObj, left + 112, 72, 96, 20); friendName.setMaxStringLength(24);
		buttonList.add(new GuiButton(10, left, 48, 146, 20, "Requests"));
		buttonList.add(new GuiButton(11, left + 154, 48, 146, 20, "Friends"));
		buttonList.add(new GuiButton(1, left + 216, 72, 84, 20, "Add Friend"));
		JSONArray entries = displayed();
		for (int row = 0; row < pageSize(); row++) {
			int index = page * pageSize() + row; if (index >= entries.length()) break;
			if (showingRequests) buttonList.add(new GuiButton(100 + index, left + 216, 112 + row * 60, 84, 20, "Accept"));
			else {
				JSONObject friend = entries.optJSONObject(index);
                boolean playing = friend != null && friend.optBoolean("online", false)
                        && ("Playing".equalsIgnoreCase(friend.optString("activity", ""))
                                || friend.optBoolean("joinable", false)
                                || !friend.optString("room", "").isEmpty());
                if (playing) buttonList.add(new GuiButton(200 + index, left + 126, 112 + row * 60, 84, 20, "Join game"));
				buttonList.add(new GuiButton(400 + index, left + 216, 112 + row * 60, 84, 20, "Remove"));
			}
		}
		buttonList.add(new GuiButton(0, left, height - 28, 70, 20, "Back"));
		GuiButton previous = new GuiButton(2, left + 74, height - 28, 78, 20, "Previous");
		GuiButton next = new GuiButton(3, left + 230, height - 28, 70, 20, "Next");
		previous.enabled = page > 0; next.enabled = page < maxPage();
		buttonList.add(previous); buttonList.add(new GuiButton(4, left + 156, height - 28, 70, 20, (page + 1) + "/" + (maxPage() + 1))); buttonList.add(next);
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		JSONArray entries = displayed();
		if (button.id == 0) { mc.displayGuiScreen(parent); return; }
		if (button.id == 10 || button.id == 11) { showingRequests = button.id == 10; page = 0; }
		else if (button.id == 1) TailsConnectFriends.addFriend(friendName.getText());
		else if (button.id == 2 && page > 0) page--;
		else if (button.id == 3 && page < maxPage()) page++;
		else if (button.id >= 100 && button.id < 200) { int i = button.id - 100; if (i < entries.length()) TailsConnectFriends.acceptFriend(entries.getJSONObject(i).optString("code", "")); }
		else if (button.id >= 200 && button.id < 400) { int i = button.id - 200; if (i < entries.length()) TailsConnectFriends.requestJoin(entries.getJSONObject(i).optString("code", "")); }
		else if (button.id >= 400) { int i = button.id - 400; if (i < entries.length()) TailsConnectFriends.removeFriend(entries.getJSONObject(i).optString("code", "")); }
		initGui();
	}
	public void keyTyped(char c, int key) throws IOException { if (friendName != null) friendName.textboxKeyTyped(c, key); super.keyTyped(c, key); }
	public void mouseClicked(int x, int y, int b) throws IOException { super.mouseClicked(x, y, b); if (friendName != null) friendName.mouseClicked(x, y, b); }

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground(); int left = width / 2 - 150;
		drawCenteredString(fontRendererObj, "TailsCraft Friends (TC4)", width / 2, 10, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "Your code: " + TailsConnectFriends.getFriendCode(), width / 2, 27, 0xAAAAAA);
		drawString(fontRendererObj, "Friend name or code:", left, 78, 0xFFFFFF); friendName.drawTextBox();
		JSONArray entries = displayed();
		for (int row = 0; row < pageSize(); row++) {
			int index = page * pageSize() + row; if (index >= entries.length()) break;
			JSONObject entry = entries.optJSONObject(index); if (entry == null) continue;
			int y = 104 + row * 60; drawRect(left, y, left + 300, y + 54, 0xCC202020);
			fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(entry.optString("name", "Player"), 196), left + 7, y + 6, 0xFFFFFF);
			if (showingRequests) fontRendererObj.drawStringWithShadow("Code: " + entry.optString("code", ""), left + 7, y + 22, 0xFFFF55);
			else {
				String info = entry.optBoolean("online", false) ? "Online" : "Offline"; String game = entry.optString("game", "");
				if (!game.isEmpty()) info += " - " + game;
				fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(info, 112), left + 7, y + 22, entry.optBoolean("online", false) ? 0x55FF55 : 0xAAAAAA);
			}
		}
		String status = TailsConnectFriends.getError(); if (status == null) status = TailsConnectFriends.getState();
		drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(status, width - 16), width / 2, height - 42, 0x55FF55);
		super.drawScreen(mouseX, mouseY, partialTicks);
		for (GuiButton b : buttonList) if (b.hovered) {
			String tip = b.id == 10 ? "Show pending friend requests." : b.id == 11 ? "Show accepted friends and presence." : b.id == 1 ? "Send a friend request by name or code." : b.id == 2 ? "Show the previous card page." : b.id == 3 ? "Show the next card page." : b.id >= 200 && b.id < 400 ? "Ask this friend to let you join their world." : b.id >= 400 ? "Remove this friend." : "Return to TailsConnect.";
			drawHoveringText(Arrays.asList(tip), mouseX, mouseY); break;
		}
	}
}
