package net.lax1dude.eaglercraft.mod.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.lax1dude.eaglercraft.Mouse;
import net.lax1dude.eaglercraft.mod.api.Mod;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

public class GuiModInfo extends GuiScreen {

    private final GuiScreen parentScreen;
    private final Mod mod;
    private final List<String> docPages;
    private int currentPage = 0;
    private int scrollOffset = 0;
    private static final int CONTENT_LEFT = 20;
    private static final int CONTENT_WIDTH = 380;
    private static final int LINE_HEIGHT = 10;

    public GuiModInfo(GuiScreen parentScreen, Mod mod) {
        this.parentScreen = parentScreen;
        this.mod = mod;
        this.docPages = new ArrayList<>(mod.getDocumentationPages());
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int centerX = this.width / 2;

        this.buttonList.add(new GuiButton(0, centerX - 100, this.height - 28, 200, 20, "Back"));

        if (currentPage > 0) {
            this.buttonList.add(new GuiButton(1, centerX - 155, this.height - 28, 50, 20, "< Prev"));
        }

        if (currentPage < docPages.size() - 1) {
            this.buttonList.add(new GuiButton(2, centerX + 105, this.height - 28, 50, 20, "Next >"));
        }

        if (docPages.isEmpty()) {
            this.buttonList.add(new GuiButton(3, centerX - 100, 38, 200, 20, "No documentation available"));
            this.buttonList.get(this.buttonList.size() - 1).enabled = false;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(parentScreen);
        } else if (button.id == 1 && currentPage > 0) {
            currentPage--;
            scrollOffset = 0;
            this.initGui();
        } else if (button.id == 2 && currentPage < docPages.size() - 1) {
            currentPage++;
            scrollOffset = 0;
            this.initGui();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int contentAreaWidth = Math.min(CONTENT_WIDTH, this.width - 40);

        String title = mod.getName() + " v" + mod.getVersion();
        this.drawCenteredString(this.fontRendererObj, title, this.width / 2, 16, 16777215);

        String byLine = "by " + mod.getAuthor();
        this.drawCenteredString(this.fontRendererObj, byLine, this.width / 2, 28, 8421504);

        if (!docPages.isEmpty()) {
            String pageTitle = "Documentation: " + docPages.get(currentPage);
            this.drawString(this.fontRendererObj, pageTitle, 20, 42, 11184810);
        }

        String content = mod.getDocumentationContent(
                docPages.isEmpty() ? "" : docPages.get(currentPage));

        if (!content.isEmpty()) {
            List<String> lines = new ArrayList<>();
            String raw = content;
            String[] paragraphs = raw.split("\n");

            for (String para : paragraphs) {
                if (para.isEmpty()) {
                    lines.add("");
                    continue;
                }

                if (para.contains("§l") || para.startsWith(" ")) {
                    lines.add(para);
                } else {
                    List<String> wrapped = this.fontRendererObj.listFormattedStringToWidth(para, contentAreaWidth);
                    lines.addAll(wrapped);
                }
            }

            int y = 56 - scrollOffset;
            int renderStart = Math.max(0, scrollOffset / LINE_HEIGHT - 1);
            int renderEnd = Math.min(lines.size(), renderStart + (this.height - 90) / LINE_HEIGHT + 2);

            for (int i = renderStart; i < renderEnd; i++) {
                String line = lines.get(i);
                if (!line.isEmpty()) {
                    int color = 16777215;

                    if (line.startsWith("§6§l")) {
                        color = 16776960;
                    } else if (line.startsWith("§e")) {
                        color = 16777120;
                    } else if (line.startsWith("§7")) {
                        color = 11184810;
                    } else if (line.startsWith("§c")) {
                        color = 16733525;
                    }

                    int yPos = 56 + i * LINE_HEIGHT - scrollOffset;
                    if (yPos > 40 && yPos < this.height - 40) {
                        if (line.startsWith("  ")) {
                            this.fontRendererObj.drawString(line, CONTENT_LEFT + 10, yPos, color);
                        } else {
                            this.fontRendererObj.drawString(line, CONTENT_LEFT, yPos, color);
                        }
                    }
                }
            }

            int totalHeight = lines.size() * LINE_HEIGHT;
            int visibleHeight = this.height - 100;
            if (totalHeight > visibleHeight) {
                int scrollBarHeight = Math.max(20, visibleHeight * visibleHeight / totalHeight);
                int scrollBarY = 56 + (visibleHeight - scrollBarHeight) * scrollOffset / (totalHeight - visibleHeight);
                drawRect(this.width - 8, 56, this.width - 4, 56 + visibleHeight, 0x33000000);
                drawRect(this.width - 8, scrollBarY, this.width - 4, scrollBarY + scrollBarHeight, 0x88AAAAAA);
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int scrollDelta = Mouse.getEventDWheel();
        if (scrollDelta != 0) {
            int contentAreaWidth = Math.min(CONTENT_WIDTH, this.width - 40);
            String content = mod.getDocumentationContent(
                    docPages.isEmpty() ? "" : docPages.get(currentPage));

            if (!content.isEmpty()) {
                int numLines = 0;
                String[] paragraphs = content.split("\n");
                for (String para : paragraphs) {
                    if (para.isEmpty()) {
                        numLines++;
                        continue;
                    }
                    if (para.contains("§l") || para.startsWith(" ")) {
                        numLines++;
                    } else {
                        numLines += this.fontRendererObj.listFormattedStringToWidth(para, contentAreaWidth).size();
                    }
                }

                int totalHeight = numLines * LINE_HEIGHT;
                int visibleHeight = this.height - 100;

                if (totalHeight > visibleHeight) {
                    scrollOffset = Math.max(0, Math.min(scrollOffset - (scrollDelta / 120) * LINE_HEIGHT * 3,
                            totalHeight - visibleHeight));
                }
            }
        }
    }
}
