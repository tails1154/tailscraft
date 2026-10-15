package net.minecraft.client.gui;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.cameras.SecurityCameraClient;
import net.minecraft.client.cameras.SecurityCameraClient.Camera;

/** Compact, card-based security camera selector. */
public class GuiScreenSecurityCameras extends GuiScreen {
    private final GuiScreen parent;
    private int page;
    private int seenRevision = -1;
    private GuiTextField cameraName;
    public GuiScreenSecurityCameras(GuiScreen parent) { this.parent = parent; }
    public void initGui() { buttonList.clear(); cameraName = new GuiTextField(20, fontRendererObj, width / 2 - 120, 38, 160, 14); cameraName.setMaxStringLength(32); SecurityCameraClient.open(); seenRevision = SecurityCameraClient.getRevision(); rebuild(); }
    private int maxPage() { return Math.max(0, (SecurityCameraClient.getCameras().size() - 1) / 3); }
    private void rebuild() {
        buttonList.clear(); List<Camera> cameras = SecurityCameraClient.getCameras(); int start = page * 3;
        for (int i = 0; i < 3 && start + i < cameras.size(); ++i) { buttonList.add(new GuiButton(100 + i, width / 2 - 120, 66 + i * 46, 168, 32, "View  " + cameras.get(start + i).name)); buttonList.add(new GuiButton(200 + i, width / 2 + 52, 66 + i * 46, 68, 32, "Remove")); }
        buttonList.add(new GuiButton(10, width / 2 + 48, 36, 72, 18, "Add"));
        buttonList.add(new GuiButton(2, width / 2 - 120, height - 28, 76, 20, "Back"));
        buttonList.add(new GuiButton(3, width / 2 - 40, height - 28, 76, 20, "Previous"));
        buttonList.add(new GuiButton(4, width / 2 + 44, height - 28, 76, 20, "Next"));
        buttonList.get(buttonList.size() - 2).enabled = page > 0; buttonList.get(buttonList.size() - 1).enabled = page < maxPage();
    }
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 2) { SecurityCameraClient.stop(); mc.displayGuiScreen(parent); }
        else if (button.id == 3) { --page; rebuild(); }
        else if (button.id == 4) { ++page; rebuild(); }
        else if (button.id >= 100 && button.id < 103) { int index = page * 3 + button.id - 100; if (index < SecurityCameraClient.getCameras().size()) SecurityCameraClient.view(SecurityCameraClient.getCameras().get(index).id); }
        else if (button.id == 10) { SecurityCameraClient.create(cameraName.getText()); cameraName.setText(""); }
        else if (button.id >= 200 && button.id < 203) { int index = page * 3 + button.id - 200; if (index < SecurityCameraClient.getCameras().size()) SecurityCameraClient.remove(SecurityCameraClient.getCameras().get(index).id); }
    }
    public boolean doesGuiPauseGame() { return false; }
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (seenRevision != SecurityCameraClient.getRevision()) { seenRevision = SecurityCameraClient.getRevision(); page = Math.min(page, maxPage()); rebuild(); }
        drawDefaultBackground(); drawCenteredString(fontRendererObj, "Security Cameras", width / 2, 16, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Select a fixed camera viewpoint", width / 2, 20, 0xAAAAAA);
        drawString(fontRendererObj, "New camera name:", width / 2 - 120, 25, 0xFFFFFF); cameraName.drawTextBox();
        List<Camera> cameras = SecurityCameraClient.getCameras();
        if (cameras.isEmpty()) drawCenteredString(fontRendererObj, "No cameras are available on this server.", width / 2, 90, 0xCCCCCC);
        else for (int i = 0; i < 3 && page * 3 + i < cameras.size(); ++i) { Camera camera = cameras.get(page * 3 + i); drawString(fontRendererObj, "#" + camera.id + "  " + camera.world + "  (" + (int) camera.x + ", " + (int) camera.y + ", " + (int) camera.z + ")", width / 2 - 112, 101 + i * 46, 0x777777); }
        drawCenteredString(fontRendererObj, cameras.isEmpty() ? "" : "Page " + (page + 1) + " / " + (maxPage() + 1), width / 2, height - 42, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
    protected void keyTyped(char c, int keyCode) throws IOException { if (cameraName != null) cameraName.textboxKeyTyped(c, keyCode); super.keyTyped(c, keyCode); }
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException { super.mouseClicked(mouseX, mouseY, mouseButton); if (cameraName != null) cameraName.mouseClicked(mouseX, mouseY, mouseButton); }
}
