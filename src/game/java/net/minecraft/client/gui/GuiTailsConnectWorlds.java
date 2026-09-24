package net.minecraft.client.gui;

import java.io.IOException;
import java.util.Arrays;
import org.json.JSONArray;
import org.json.JSONObject;
import net.lax1dude.eaglercraft.tailsconnect.TailsConnectClient;

/** Paged world cards keep the browser usable at every supported GUI scale. */
public class GuiTailsConnectWorlds extends GuiScreen {
    private final GuiScreen parent;
    private int page;
    private JSONArray displayed;
    private int pageSize() { return Math.max(1, (height - 90) / 54); }
    public GuiTailsConnectWorlds(GuiScreen parent) { this.parent = parent; }
    public boolean doesGuiPauseGame() { return false; }
    public void initGui() {
        displayed = TailsConnectClient.getSearchResults();
        page = Math.min(page, Math.max(0, (displayed.length() - 1) / pageSize()));
        buttonList.clear();
        int left = width / 2 - 150;
        buttonList.add(new GuiButton(0, left, height - 28, 70, 20, "Back"));
        buttonList.add(new GuiButton(1, left + 74, height - 28, 78, 20, "Refresh"));
        GuiButton prev = new GuiButton(2, left + 156, height - 28, 70, 20, "Previous");
        GuiButton next = new GuiButton(3, left + 230, height - 28, 70, 20, "Next");
        prev.enabled = page > 0;
        next.enabled = (page + 1) * pageSize() < displayed.length();
        buttonList.add(prev); buttonList.add(next);
        for (int row = 0; row < pageSize(); row++) {
            int index = page * pageSize() + row;
            if (index >= displayed.length()) break;
            JSONObject world = displayed.optJSONObject(index);
            if (world == null) continue;
            int capacity = world.optInt("maxPlayers", 0);
            boolean full = capacity > 0 && world.optInt("players", 0) >= capacity;
            GuiButton join = new GuiButton(100 + index, left + 242, 50 + row * 54, 52, 20, full ? "Full" : "Join");
            join.enabled = !full && mc.world == null && world.optString("room").matches("[A-Fa-f0-9]{6}");
            buttonList.add(join);
        }
    }
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;
        if (button.id == 0) mc.displayGuiScreen(parent);
        else if (button.id == 1) { page = 0; TailsConnectClient.searchWorlds(); initGui(); }
        else if (button.id == 2) { page--; initGui(); }
        else if (button.id == 3) { page++; initGui(); }
        else if (button.id >= 100) {
            String code = displayed.getJSONObject(button.id - 100).getString("room");
            TailsConnectClient.join("ecraft", code);
            mc.displayGuiScreen(parent);
        }
    }
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (displayed != TailsConnectClient.getSearchResults()) initGui();
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "TailsConnect - Public Worlds", width / 2, 10, 0xFFFFFF);
        String status = TailsConnectClient.getError();
        if (status == null) status = TailsConnectClient.getState();
        drawCenteredString(fontRendererObj, fontRendererObj.trimStringToWidth(status, width - 16), width / 2, 25, 0xAAAAAA);
        int left = width / 2 - 150;
        for (int row = 0; row < pageSize(); row++) {
            int index = page * pageSize() + row;
            if (index >= displayed.length()) break;
            JSONObject world = displayed.optJSONObject(index);
            if (world == null) continue;
            int y = 42 + row * 54;
            drawRect(left, y, left + 300, y + 49, 0xCC202020);
            fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(world.optString("name", "World"), 228), left + 6, y + 5, 0xFFFFFF);
            int capacity = world.optInt("maxPlayers", 0);
            String info = world.optInt("players", 0) + "/" + (capacity == 0 ? "Unlimited" : capacity) + " players | " + world.optString("room");
            fontRendererObj.drawStringWithShadow(info, left + 6, y + 18, 0x99CCFF);
            fontRendererObj.drawStringWithShadow(fontRendererObj.trimStringToWidth(world.optString("description", ""), 228), left + 6, y + 32, 0xAAAAAA);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
        for (GuiButton button : buttonList) {
            if (!button.hovered) continue;
            String tooltip = null;
            if (button.id == 0) tooltip = "Return to the TailsConnect menu.";
            else if (button.id == 1) tooltip = "Search again for currently public worlds.";
            else if (button.id == 2) tooltip = "Show the previous page of worlds.";
            else if (button.id == 3) tooltip = "Show the next page of worlds.";
            else if (button.id >= 100) tooltip = "Join this public world.";
            if (tooltip != null) drawHoveringText(Arrays.asList(tooltip), mouseX, mouseY);
            break;
        }
    }
}
