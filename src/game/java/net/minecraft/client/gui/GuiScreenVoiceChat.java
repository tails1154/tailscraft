package net.minecraft.client.gui;

import java.io.IOException;
import net.minecraft.client.voice.VoiceConsentState;
import net.minecraft.client.voice.TailsCraftVoiceClient;
import net.minecraft.client.voice.VoicePlatform;

/** Consent and local controls for TailsCraft voice chat. */
public final class GuiScreenVoiceChat extends GuiScreen {
    private final GuiScreen parent;
    private final boolean consentPrompt;
    public GuiScreenVoiceChat(GuiScreen parent, boolean consentPrompt) { this.parent = parent; this.consentPrompt = consentPrompt; }
    public void initGui() {
        buttonList.clear();
        int y = height / 2 - (consentPrompt ? 10 : 35);
        if (consentPrompt) {
            buttonList.add(new GuiButton(1, width / 2 - 100, y + 32, VoicePlatform.isSecureContext() ? "Enable voice chat" : "Open HTTPS version"));
            buttonList.add(new GuiButton(2, width / 2 - 100, y + 57, "Not now"));
        } else {
            buttonList.add(new GuiButton(3, width / 2 - 100, y + 32,
                    !VoicePlatform.isSecureContext() ? "Open HTTPS version" : (VoiceConsentState.isOptedIn() ? (VoiceConsentState.isMuted() ? "Unmute microphone" : "Mute microphone") : "Enable voice chat")));
            buttonList.add(new GuiButton(4, width / 2 - 100, y + 57, VoiceConsentState.isDeafened() ? "Undeafen" : "Deafen"));
            buttonList.add(new GuiButton(5, width / 2 - 100, y + 82, "Done"));
        }
    }
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 1) {
            if (!VoicePlatform.isSecureContext()) { VoicePlatform.openSecurePage(); return; }
            VoiceConsentState.optIn(); TailsCraftVoiceClient.enable(mc.getConnection()); TailsCraftVoiceClient.resumeAudio(); mc.displayGuiScreen(parent);
        }
        else if (button.id == 2) { VoiceConsentState.optOut(); TailsCraftVoiceClient.disable(); mc.displayGuiScreen(parent); }
        else if (button.id == 3) {
            if (!VoicePlatform.isSecureContext()) { VoicePlatform.openSecurePage(); return; }
            if (!VoiceConsentState.isOptedIn()) {
                VoiceConsentState.optIn();
                TailsCraftVoiceClient.enable(mc.getConnection());
                TailsCraftVoiceClient.resumeAudio();
            } else {
                VoiceConsentState.setMuted(!VoiceConsentState.isMuted());
                TailsCraftVoiceClient.setMuted(VoiceConsentState.isMuted());
            }
            initGui();
        }
        else if (button.id == 4) { VoiceConsentState.setDeafened(!VoiceConsentState.isDeafened()); TailsCraftVoiceClient.setDeafened(VoiceConsentState.isDeafened()); initGui(); }
        else if (button.id == 5) mc.displayGuiScreen(parent);
    }
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        drawCenteredString(fontRendererObj, "TailsCraft Voice Chat", width / 2, height / 2 - 65, 0xFFFFFF);
        if (consentPrompt) {
            drawCenteredString(fontRendererObj, "Press V any time to open voice controls.", width / 2, height / 2 - 42, 0xCCCCCC);
            drawCenteredString(fontRendererObj, VoicePlatform.isSecureContext() ? "Your microphone is never used until you enable voice chat." : "Voice chat requires HTTPS before microphone access can start.", width / 2, height / 2 - 29, 0xAAAAAA);
        } else {
            drawCenteredString(fontRendererObj, VoiceConsentState.isOptedIn() ? "Voice chat enabled" : "Voice chat disabled", width / 2, height / 2 - 42, 0xCCCCCC);
            drawCenteredString(fontRendererObj, TailsCraftVoiceClient.getStatus(), width / 2, height / 2 - 29, 0xAAAAAA);
        }
        if (VoiceConsentState.getError() != null) drawCenteredString(fontRendererObj, VoiceConsentState.getError(), width / 2, height / 2 + 45, 0xFF5555);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}
