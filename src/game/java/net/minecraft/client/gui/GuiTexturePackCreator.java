package net.minecraft.client.gui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.internal.FileChooserResult;
import net.lax1dude.eaglercraft.opengl.ImageData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;

/** A small local texture-pack workbench for the browser client. */
public class GuiTexturePackCreator extends GuiScreen {
	private static final String[] TEXTURES = {
			"textures/block/stone.png", "textures/block/grass_top.png", "textures/block/dirt.png",
			"textures/block/cobblestone.png", "textures/block/planks_oak.png", "textures/block/planks_spruce.png",
			"textures/block/sand.png", "textures/block/gravel.png", "textures/block/log_oak.png",
			"textures/block/leaves_oak.png", "textures/block/glass.png", "textures/block/brick.png",
			"textures/block/obsidian.png", "textures/block/coal_ore.png", "textures/block/iron_ore.png",
			"textures/block/gold_ore.png", "textures/block/diamond_ore.png", "textures/block/torch_on.png",
			"textures/block/bookshelf.png", "textures/block/tnt_side.png", "textures/block/glowstone.png",
			"textures/item/diamond.png", "textures/item/iron_ingot.png", "textures/item/gold_ingot.png",
			"textures/item/coal.png", "textures/item/stick.png", "textures/item/apple.png",
			"textures/item/bread.png", "textures/item/arrow.png", "textures/item/bucket_empty.png",
			"textures/item/diamond_sword.png", "textures/item/diamond_pickaxe.png",
			"textures/item/diamond_axe.png", "textures/item/bow_standby.png", "textures/item/fishing_rod_uncast.png",
			"textures/gui/title/minecraft.png", "textures/gui/widgets.png", "textures/gui/container/inventory.png",
			"textures/entity/steve.png", "textures/particle/particles.png" };
	private static final int[] PALETTE = { 0xFFFFFFFF, 0xFF000000, 0xFFFF3333, 0xFFFFCC33, 0xFF33CC66,
			0xFF3399FF, 0xFF9933CC, 0xFF996633, 0xFF777777, 0x00000000 };

	private final GuiScreen parent;
	private final List<TextureEntry> entries = new ArrayList<TextureEntry>();
	private TextureList textureList;
	private GuiTextField packName;
	private ImageData image;
	private String selectedPath;
	private int selectedColor = PALETTE[0];
	private boolean importing;
	private boolean dirty;
	private int canvasX, canvasY, canvasScale;

	public GuiTexturePackCreator(GuiScreen parent) {
		this.parent = parent;
		for (String path : TEXTURES) entries.add(new TextureEntry(path));
	}

	@Override
	public void initGui() {
		buttonList.clear();
		packName = new GuiTextField(20, fontRendererObj, width / 2 - 70, 8, 140, 20);
		packName.setText("My TailsCraft Pack");
		buttonList.add(new GuiButton(0, 8, height - 28, 70, 20, I18n.format("gui.back")));
		buttonList.add(new GuiButton(2, width / 2 - 190, height - 28, 90, 20, "Import texture"));
		buttonList.add(new GuiButton(3, width / 2 - 45, height - 28, 90, 20, "Export pack"));
		buttonList.add(new GuiButton(4, width / 2 + 100, height - 28, 90, 20, "Clear canvas"));
		for (int i = 0; i < PALETTE.length; ++i) {
			GuiButton b = new GuiButton(20 + i, width / 2 + 20 + (i % 5) * 22, height - 56 + (i / 5) * 22, 20, 20, "");
			buttonList.add(b);
		}
		textureList = new TextureList(this, mc, 205, height - 68, 34, 34, 22);
		textureList.setSlotXBoundsFromLeft(6);
		textureList.registerScrollButtons(7, 8);
		loadTexture(TEXTURES[0]);
	}

	void selectTexture(int index) {
		if (index >= 0 && index < entries.size()) loadTexture(entries.get(index).path);
	}

	private void loadTexture(String path) {
		selectedPath = path;
		try (IResource resource = mc.getResourceManager().getResource(new ResourceLocation(path))) {
			image = ImageData.loadImageFile(readAll(resource.getInputStream()));
		} catch (Throwable t) {
			image = new ImageData(16, 16, true);
		}
		if (image == null || image.width <= 0 || image.height <= 0 || image.width > 128 || image.height > 128) {
			image = new ImageData(16, 16, true);
		}
		dirty = false;
	}

	private static byte[] readAll(InputStream in) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		byte[] buf = new byte[4096];
		int len;
		while ((len = in.read(buf)) >= 0) if (len > 0) out.write(buf, 0, len);
		return out.toByteArray();
	}

	@Override
	public void updateScreen() {
		if (importing && EagRuntime.fileChooserHasResult()) {
			FileChooserResult result = EagRuntime.getFileChooserResult();
			importing = false;
			EagRuntime.clearFileChooserResult();
			try {
				ImageData imported = ImageData.loadImageFile(result.fileData, ImageData.getMimeFromType(result.fileName));
				if (imported != null && imported.width <= 128 && imported.height <= 128) {
					image = imported;
					dirty = true;
				}
			} catch (Throwable ignored) { }
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) mc.displayGuiScreen(parent);
		else if (button.id == 2) { importing = true; EagRuntime.displayFileChooser("image/png,image/jpeg", "png,jpg,jpeg"); }
		else if (button.id == 3) exportPack();
		else if (button.id == 4) { image = new ImageData(image.width, image.height, true); dirty = true; }
		else if (button.id >= 20 && button.id < 20 + PALETTE.length) selectedColor = PALETTE[button.id - 20];
	}

	private void exportPack() {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ZipOutputStream zip = new ZipOutputStream(bytes);
			zip.putNextEntry(new ZipEntry("pack.mcmeta"));
			zip.write(("{\"pack\":{\"pack_format\":3,\"description\":\"" + escape(packName.getText()) + "\"}}").getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
			zip.putNextEntry(new ZipEntry("assets/minecraft/" + selectedPath));
			zip.write(encodePng(image));
			zip.closeEntry();
			zip.finish();
			zip.close();
			EagRuntime.downloadFileWithName(packName.getText().trim().replace(' ', '_') + ".zip", bytes.toByteArray());
			dirty = false;
		} catch (Throwable t) {
			EagRuntime.showPopup("Could not export texture pack: " + t.getClass().getSimpleName());
		}
	}

	private static String escape(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }

	private static byte[] encodePng(ImageData data) throws IOException {
		ByteArrayOutputStream raw = new ByteArrayOutputStream();
		for (int y = 0; y < data.height; ++y) {
			raw.write(0);
			for (int x = 0; x < data.width; ++x) {
				int p = data.pixels[y * data.width + x];
				raw.write((p >>> 16) & 255); raw.write((p >>> 8) & 255); raw.write(p & 255); raw.write((p >>> 24) & 255);
			}
		}
		ByteArrayOutputStream compressed = new ByteArrayOutputStream();
		DeflaterOutputStream deflater = new DeflaterOutputStream(compressed); deflater.write(raw.toByteArray()); deflater.close();
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		png.write(new byte[] { (byte)137, 80, 78, 71, 13, 10, 26, 10 });
		ByteArrayOutputStream header = new ByteArrayOutputStream(); writeInt(header, data.width); writeInt(header, data.height); header.write(8); header.write(6); header.write(0); header.write(0); header.write(0); chunk(png, "IHDR", header.toByteArray());
		chunk(png, "IDAT", compressed.toByteArray()); chunk(png, "IEND", new byte[0]); return png.toByteArray();
	}

	private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
		writeInt(out, data.length); byte[] t = type.getBytes(StandardCharsets.US_ASCII); out.write(t); out.write(data);
		CRC32 crc = new CRC32(); crc.update(t); crc.update(data); writeInt(out, (int) crc.getValue());
	}
	private static void writeInt(ByteArrayOutputStream out, int v) { out.write((v >>> 24) & 255); out.write((v >>> 16) & 255); out.write((v >>> 8) & 255); out.write(v & 255); }

	private void paint(int mouseX, int mouseY) {
		if (image == null || mouseX < canvasX || mouseY < canvasY) return;
		int px = (mouseX - canvasX) / canvasScale, py = (mouseY - canvasY) / canvasScale;
		if (px >= 0 && py >= 0 && px < image.width && py < image.height) { image.pixels[py * image.width + px] = selectedColor; dirty = true; }
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		packName.mouseClicked(mouseX, mouseY, mouseButton);
		if (mouseButton == 0) paint(mouseX, mouseY);
	}
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (packName.textboxKeyTyped(typedChar, keyCode)) return;
		super.keyTyped(typedChar, keyCode);
	}
	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int button, long time) { super.mouseClickMove(mouseX, mouseY, button, time); if (button == 0) paint(mouseX, mouseY); }

	@Override
	public void handleMouseInput() throws IOException { super.handleMouseInput(); textureList.handleMouseInput(); }

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "Texture Pack Creator 9000", width / 2, 2, 0xFFFFFF);
		packName.drawTextBox();
		drawString(fontRendererObj, "Texture: " + selectedPath, 224, 38, 0xCCCCCC);
		drawString(fontRendererObj, "Click or drag to paint • right-click is reserved for future tools", 224, height - 70, 0xAAAAAA);
		if (image != null) {
			canvasScale = Math.max(1, Math.min(8, 220 / Math.max(image.width, image.height)));
			canvasX = width / 2 - 90; canvasY = 58;
			for (int y = 0; y < image.height; ++y) for (int x = 0; x < image.width; ++x) {
				int c = image.pixels[y * image.width + x];
				if ((c >>> 24) == 0) c = 0xFF555555;
				drawRect(canvasX + x * canvasScale, canvasY + y * canvasScale, canvasX + (x + 1) * canvasScale, canvasY + (y + 1) * canvasScale, c);
			}
			int right = canvasX + image.width * canvasScale;
			int bottom = canvasY + image.height * canvasScale;
			drawRect(canvasX - 1, canvasY - 1, right + 1, canvasY, 0xFFFFFFFF);
			drawRect(canvasX - 1, bottom, right + 1, bottom + 1, 0xFFFFFFFF);
			drawRect(canvasX - 1, canvasY, canvasX, bottom, 0xFFFFFFFF);
			drawRect(right, canvasY, right + 1, bottom, 0xFFFFFFFF);
		}
		for (int i = 0; i < PALETTE.length; ++i) drawRect(width / 2 + 22 + (i % 5) * 22, height - 54 + (i / 5) * 22, width / 2 + 38 + (i % 5) * 22, height - 38 + (i / 5) * 22, PALETTE[i] == 0 ? 0xFF333333 : PALETTE[i]);
		textureList.drawScreen(mouseX, mouseY, partialTicks);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	private static class TextureEntry { final String path; TextureEntry(String path) { this.path = path; } }
	private static class TextureList extends GuiSlot {
		private final GuiTexturePackCreator owner;
		TextureList(GuiTexturePackCreator owner, Minecraft mc, int width, int height, int top, int bottom, int slotHeight) { super(mc, width, height, top, bottom, slotHeight); this.owner = owner; }
		protected int getSize() { return owner.entries.size(); }
		protected void elementClicked(int slot, boolean dbl, int x, int y) { owner.selectTexture(slot); }
		protected boolean isSelected(int slot) { return owner.selectedPath.equals(owner.entries.get(slot).path); }
		protected void drawBackground() { owner.drawDefaultBackground(); }
		protected void func_192637_a(int slot, int x, int y, int h, int mouseX, int mouseY, float partial) { owner.drawString(owner.fontRendererObj, owner.entries.get(slot).path.substring(owner.entries.get(slot).path.lastIndexOf('/') + 1), x + 2, y + 5, 0xFFFFFF); }
	}
}
