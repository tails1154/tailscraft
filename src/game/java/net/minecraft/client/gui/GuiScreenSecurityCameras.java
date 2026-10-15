package net.minecraft.client.gui;

import java.io.IOException;
import java.util.List;

import net.minecraft.client.cameras.SecurityCameraClient;
import net.minecraft.client.cameras.SecurityCameraClient.Camera;

/** Compact, card-based security camera selector. */
public class GuiScreenSecurityCameras extends GuiScreen {
    private final GuiScreen parent;
    private int page;
    public GuiScreenSecurityCameras(GuiScreen parent) { this.parent = parent; }
    public void initGui() { buttonList.clear(); SecurityCameraClient.open(); rebuild(); }
    private int maxPage() { return Math.max(0, (SecurityCameraClient.getCameras().size() - 1) / 3); }
    private void rebuild() {
        buttonList.clear(); List<Camera> cameras = SecurityCameraClient.getCameras(); int start = page * 3;
        for (int i = 0; i < 3 && start + i < cameras.size(); ++i) buttonList.add(new GuiButton(100 + i, width / 2 - 120, 54 + i * 46, 240, 32, "View  " + cameras.get(start + i).name));
        buttonList.add(new GuiButton(2, width / 2 - 120, height - 28, 76, 20, "Back"));
        buttonList.add(new GuiButton(3, width / 2 - 40, height - 28, 76, 20, "Previous"));
        buttonList.add(new GuiButton(4, width / 2 + 44, height - 28, 76, 20, "Next"));
        buttonList.get(2).enabled = page > 0; buttonList.get(3).enabled = page < maxPage();
    }
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 2) { SecurityCameraClient.stop(); mc.displayGuiScreen(parent); }
        else if (button.id == 3) { --page; rebuild(); }
        else if (button.id == 4) { ++page; rebuild(); }
        else if (button.id >= 100 && button.id < 103) { int index = page * 3 + button.id - 100; if (index < SecurityCameraClient.getCameras().size()) SecurityCameraClient.view(SecurityCameraClient.getCameras().get(index).id); }
    }
    public boolean doesGuiPauseGame() { return false; }
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground(); drawCenteredString(fontRendererObj, "Security Cameras", width / 2, 16, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Select a fixed camera viewpoint", width / 2, 31, 0xAAAAAA);
        List<Camera> cameras = SecurityCameraClient.getCameras();
        if (cameras.isEmpty()) drawCenteredString(fontRendererObj, "No cameras are available on this server.", width / 2, 90, 0xCCCCCC);
        else for (int i = 0; i < 3 && page * 3 + i < cameras.size(); ++i) { Camera camera = cameras.get(page * 3 + i); drawString(fontRendererObj, "#" + camera.id + "  " + camera.world + "  (" + (int) camera.x + ", " + (int) camera.y + ", " + (int) camera.z + ")", width / 2 - 112, 89 + i * 46, 0x777777); }
        drawCenteredString(fontRendererObj, cameras.isEmpty() ? "" : "Page " + (page + 1) + " / " + (maxPage() + 1), width / 2, height - 42, 0xAAAAAA);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
