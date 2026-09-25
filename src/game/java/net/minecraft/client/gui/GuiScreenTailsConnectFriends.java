package net.minecraft.client.gui;

import java.io.IOException;
import java.util.Arrays;

import net.lax1dude.eaglercraft.tailsconnect.TailsConnectFriends;
import org.json.JSONArray;
import org.json.JSONObject;

/** TC4 friend-code and presence screen. */
public class GuiScreenTailsConnectFriends extends GuiScreen {
	private final GuiScreen parent;
	private GuiTextField friendCode;

	public GuiScreenTailsConnectFriends(GuiScreen parent) {
		this.parent = parent;
	}

	public boolean doesGuiPauseGame() {
		return false;
	}

	public void initGui() {
		TailsConnectFriends.open();
		buttonList.clear();
		int left = width / 2 - 150;
		friendCode = new GuiTextField(0, fontRendererObj, left, 48, 210, 20);
		friendCode.setMaxStringLength(24);
		buttonList.add(new GuiButton(1, left + 216, 48, 84, 20, "Add Friend"));
		JSONArray requests = TailsConnectFriends.getRequests();
		for (int i = 0; i < requests.length(); i++) {
			buttonList.add(new GuiButton(100 + i, left + 216, 92 + i * 24, 84, 20, "Accept"));
		}
		JSONArray friends = TailsConnectFriends.getFriends();
		for (int i = 0; i < friends.length(); i++) {
			JSONObject friend = friends.getJSONObject(i);
			if (friend.optBoolean("online", false) && friend.optBoolean("joinable", false)) {
				buttonList.add(new GuiButton(200 + i, left + 142, 196 + i * 24, 70, 20, "Join game"));
			}
			buttonList.add(new GuiButton(400 + i, left + 216, 196 + i * 24, 84, 20, "Remove"));
		}
		buttonList.add(new GuiButton(2, left, height - 28, 300, 20, "Back"));
	}

	protected void actionPerformed(GuiButton button) throws IOException {
		JSONArray requests = TailsConnectFriends.getRequests();
		JSONArray friends = TailsConnectFriends.getFriends();
		if (button.id == 1) {
			TailsConnectFriends.addFriend(friendCode.getText());
		} else if (button.id == 2) {
			mc.displayGuiScreen(parent);
			return;
		} else if (button.id >= 100 && button.id < 200) {
			int index = button.id - 100;
			if (index < requests.length()) TailsConnectFriends.acceptFriend(requests.getJSONObject(index).optString("code", ""));
		} else if (button.id >= 200 && button.id < 400) {
			int index = button.id - 200;
			if (index < friends.length()) TailsConnectFriends.requestJoin(friends.getJSONObject(index).optString("code", ""));
		} else if (button.id >= 400) {
			int index = button.id - 400;
			if (index < friends.length()) TailsConnectFriends.removeFriend(friends.getJSONObject(index).optString("code", ""));
		}
		initGui();
	}

	public void keyTyped(char typedChar, int keyCode) throws IOException {
		if (friendCode != null) friendCode.textboxKeyTyped(typedChar, keyCode);
		super.keyTyped(typedChar, keyCode);
	}

	public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (friendCode != null) friendCode.mouseClicked(mouseX, mouseY, mouseButton);
	}

	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		int left = width / 2 - 150;
		drawCenteredString(fontRendererObj, "TailsCraft Friends (TC4)", width / 2, 14, 0xFFFFFF);
		drawCenteredString(fontRendererObj, "Your code: " + TailsConnectFriends.getFriendCode(), width / 2, 30, 0xAAAAAA);
		drawString(fontRendererObj, "Friend name or code:", left, 38, 0xFFFFFF);
		friendCode.drawTextBox();
		drawString(fontRendererObj, "Requests", left, 78, 0xFFFFFF);
		JSONArray requests = TailsConnectFriends.getRequests();
		for (int i = 0; i < requests.length(); i++) {
			JSONObject request = requests.getJSONObject(i);
			drawString(fontRendererObj, request.optString("name", "Player") + " ("
					+ request.optString("code", "") + ")", left, 98 + i * 24, 0xFFFF55);
		}
		drawString(fontRendererObj, "Friends", left, 182, 0xFFFFFF);
		JSONArray friends = TailsConnectFriends.getFriends();
		for (int i = 0; i < friends.length(); i++) {
			JSONObject friend = friends.getJSONObject(i);
			String online = friend.optBoolean("online", false) ? "online" : "offline";
			String game = friend.optString("game", "");
			if (game.length() > 24) game = game.substring(0, 24);
			String presence = online + (game.isEmpty() ? "" : " - " + game);
			drawString(fontRendererObj, fontRendererObj.trimStringToWidth(friend.optString("name", "Player")
				+ " - " + presence, 136), left, 202 + i * 24,
					friend.optBoolean("online", false) ? 0x55FF55 : 0xAAAAAA);
		}
		String status = TailsConnectFriends.getError();
		if (status == null) status = TailsConnectFriends.getState();
		drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(status, width - 16), width / 2,
				height - 42, 0x55FF55);
		super.drawScreen(mouseX, mouseY, partialTicks);
		for (GuiButton button : buttonList) if (button.hovered) {
			drawHoveringText(Arrays.asList(button.id == 1 ? "Send a friend request using their 8-character code."
					: button.id >= 200 && button.id < 400 ? "Ask this friend to let you join their world."
					: button.id >= 400 ? "Remove this friend."
								: "Manage this friend or return to TailsConnect."), mouseX, mouseY);
			break;
		}
	}
}
