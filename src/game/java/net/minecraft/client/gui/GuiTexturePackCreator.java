package net.minecraft.client.gui;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class GuiTexturePackCreator extends GuiScreen implements GuiYesNoCallback {
	private static final String[] TEXTURES = {
			"textures/block/stone.png", "textures/block/grass_top.png", "textures/block/dirt.png",
			"textures/block/grass_side.png", "textures/block/grass_side_overlay.png", "textures/block/sandstone.png",
			"textures/block/cobblestone.png", "textures/block/planks_oak.png", "textures/block/planks_spruce.png",
			"textures/block/planks_birch.png", "textures/block/planks_jungle.png", "textures/block/planks_acacia.png",
			"textures/block/planks_big_oak.png", "textures/block/sand.png", "textures/block/gravel.png", "textures/block/log_oak.png",
			"textures/block/log_spruce.png", "textures/block/log_birch.png", "textures/block/log_jungle.png",
			"textures/block/leaves_oak.png", "textures/block/glass.png", "textures/block/brick.png",
			"textures/block/obsidian.png", "textures/block/coal_ore.png", "textures/block/iron_ore.png",
			"textures/block/gold_ore.png", "textures/block/diamond_ore.png", "textures/block/torch_on.png",
			"textures/block/emerald_ore.png", "textures/block/lapis_ore.png", "textures/block/redstone_ore.png",
			"textures/block/bookshelf.png", "textures/block/tnt_side.png", "textures/block/tnt_top.png", "textures/block/glowstone.png",
			"textures/block/netherrack.png", "textures/block/end_stone.png", "textures/block/snow.png", "textures/block/ice.png",
			"textures/block/water_still.png", "textures/block/lava_still.png",
			"textures/item/diamond.png", "textures/item/iron_ingot.png", "textures/item/gold_ingot.png",
			"textures/item/coal.png", "textures/item/stick.png", "textures/item/apple.png",
			"textures/item/bread.png", "textures/item/cookie.png", "textures/item/arrow.png", "textures/item/bucket_empty.png",
			"textures/item/diamond_sword.png", "textures/item/diamond_pickaxe.png",
			"textures/item/diamond_axe.png", "textures/item/bow_standby.png", "textures/item/fishing_rod_uncast.png",
			"textures/item/clock.png", "textures/item/compass.png", "textures/item/map_filled.png", "textures/item/shears.png",
			"textures/gui/title/minecraft.png", "textures/gui/widgets.png", "textures/gui/container/inventory.png",
			"textures/entity/steve.png", "textures/particle/particles.png" };
	private static final int[] PALETTE = { 0xFFFFFFFF, 0xFF000000, 0xFFFF3333, 0xFFFFCC33, 0xFF33CC66,
			0xFF3399FF, 0xFF9933CC, 0xFF996633, 0xFF777777, 0x00000000 };
	private static final String PROJECT_STORAGE = "tailscraft_texture_pack_creator_project";

	private final GuiScreen parent;
	private final List<TextureEntry> entries = new ArrayList<TextureEntry>();
	private final Map<String, ImageData> editedTextures = new HashMap<String, ImageData>();
	private final List<ImageData> undoStack = new ArrayList<ImageData>();
	private final List<ImageData> redoStack = new ArrayList<ImageData>();
	private TextureList textureList;
	private GuiTextField packName;
	private ImageData image;
	private String selectedPath;
	private int selectedColor = PALETTE[0];
	private boolean importing;
	private boolean dirty;
	private boolean paintGesture;
	private int tool = 0;
	private int brushSize = 1;
	private int zoomOffset = 0;
	private boolean grid = true;
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
		buttonList.add(new GuiButton(107, 8, 8, 80, 20, "Save project"));
		buttonList.add(new GuiButton(108, 92, 8, 80, 20, "Load project"));
		buttonList.add(new GuiButton(109, width - 86, 8, 78, 20, "New project"));
		buttonList.add(new GuiButton(0, 8, height - 28, 70, 20, I18n.format("gui.back")));
		buttonList.add(new GuiButton(2, width / 2 - 190, height - 28, 90, 20, "Import texture"));
		buttonList.add(new GuiButton(3, width / 2 - 45, height - 28, 90, 20, "Export pack"));
		buttonList.add(new GuiButton(4, width / 2 + 100, height - 28, 90, 20, "Clear canvas"));
		buttonList.add(new GuiButton(5, width / 2 - 190, height - 56, 42, 20, "Undo"));
		buttonList.add(new GuiButton(6, width / 2 - 145, height - 56, 42, 20, "Redo"));
		buttonList.add(new GuiButton(40, width / 2 - 95, height - 56, 52, 20, "Pencil"));
		buttonList.add(new GuiButton(41, width / 2 - 40, height - 56, 52, 20, "Eraser"));
		buttonList.add(new GuiButton(42, width / 2 + 15, height - 56, 45, 20, "Fill"));
		buttonList.add(new GuiButton(43, width / 2 + 63, height - 56, 52, 20, "Pick"));
		buttonList.add(new GuiButton(50, width / 2 - 95, height - 82, 30, 20, "1px"));
		buttonList.add(new GuiButton(51, width / 2 - 62, height - 82, 30, 20, "2px"));
		buttonList.add(new GuiButton(52, width / 2 - 29, height - 82, 30, 20, "4px"));
		buttonList.add(new GuiButton(60, width / 2 + 100, height - 82, 48, 20, "Zoom -"));
		buttonList.add(new GuiButton(61, width / 2 + 151, height - 82, 48, 20, "Zoom +"));
		buttonList.add(new GuiButton(62, width / 2 + 202, height - 82, 48, 20, "Grid: on"));
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
		ImageData edited = editedTextures.get(path);
		if (edited != null) {
			image = edited;
			undoStack.clear();
			redoStack.clear();
			return;
		}
		try (IResource resource = mc.getResourceManager().getResource(new ResourceLocation(path))) {
			image = ImageData.loadImageFile(readAll(resource.getInputStream()));
			if (image != null) image.swapRB();
		} catch (Throwable t) {
			image = new ImageData(16, 16, true);
		}
		if (image == null || image.width <= 0 || image.height <= 0 || image.width > 128 || image.height > 128) {
			image = new ImageData(16, 16, true);
		}
		undoStack.clear();
		redoStack.clear();
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
					imported.swapRB();
					recordUndo();
					image = imported;
					markEdited();
				}
			} catch (Throwable ignored) { }
		}
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 0) mc.displayGuiScreen(parent);
		else if (button.id == 2) { importing = true; EagRuntime.displayFileChooser("image/png,image/jpeg", "png,jpg,jpeg"); }
		else if (button.id == 3) exportPack();
		else if (button.id == 4) { recordUndo(); image = new ImageData(image.width, image.height, true); markEdited(); }
		else if (button.id == 5) undo();
		else if (button.id == 6) redo();
		else if (button.id == 107) saveProject();
		else if (button.id == 108) loadProject();
		else if (button.id == 109) mc.displayGuiScreen(new GuiYesNo(this, "Start a new texture project?", "Unsaved edits will be cleared.", "Start over", "Cancel", 109));
		else if (button.id >= 40 && button.id <= 43) tool = button.id - 40;
		else if (button.id >= 50 && button.id <= 52) brushSize = 1 << (button.id - 50);
		else if (button.id == 60) zoomOffset = Math.max(-4, zoomOffset - 1);
		else if (button.id == 61) zoomOffset = Math.min(8, zoomOffset + 1);
		else if (button.id == 62) { grid = !grid; button.displayString = grid ? "Grid: on" : "Grid: off"; }
		else if (button.id >= 20 && button.id < 20 + PALETTE.length) selectedColor = PALETTE[button.id - 20];
	}

	@Override
	public void confirmClicked(boolean result, int id) {
		if (result && id == 109) {
			editedTextures.clear();
			undoStack.clear();
			redoStack.clear();
			packName.setText("My TailsCraft Pack");
			dirty = false;
			loadTexture(TEXTURES[0]);
			EagRuntime.showPopup("New project started.");
		}
		mc.displayGuiScreen(this);
	}

	private void markEdited() {
		editedTextures.put(selectedPath, image);
		dirty = true;
	}

	private void recordUndo() {
		if (image != null) {
			undoStack.add(copyImage(image));
			if (undoStack.size() > 32) undoStack.remove(0);
			redoStack.clear();
		}
	}

	private void undo() {
		if (undoStack.isEmpty()) return;
		redoStack.add(copyImage(image));
		image = undoStack.remove(undoStack.size() - 1);
		markEdited();
	}

	private void redo() {
		if (redoStack.isEmpty()) return;
		undoStack.add(copyImage(image));
		image = redoStack.remove(redoStack.size() - 1);
		markEdited();
	}

	private static ImageData copyImage(ImageData source) {
		int[] pixels = new int[source.pixels.length];
		System.arraycopy(source.pixels, 0, pixels, 0, pixels.length);
		return new ImageData(source.width, source.height, pixels, source.alpha);
	}

	private void exportPack() {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ZipOutputStream zip = new ZipOutputStream(bytes);
			zip.putNextEntry(new ZipEntry("pack.mcmeta"));
			zip.write(("{\"pack\":{\"pack_format\":3,\"description\":\"" + escape(packName.getText()) + "\"}}").getBytes(StandardCharsets.UTF_8));
			zip.closeEntry();
			Map<String, ImageData> exportTextures = new HashMap<String, ImageData>(editedTextures);
			if (exportTextures.isEmpty()) exportTextures.put(selectedPath, image);
			for (Map.Entry<String, ImageData> texture : exportTextures.entrySet()) {
				zip.putNextEntry(new ZipEntry("assets/minecraft/" + texture.getKey()));
				zip.write(encodePng(texture.getValue()));
				zip.closeEntry();
			}
			zip.finish();
			zip.close();
			EagRuntime.downloadFileWithName(packName.getText().trim().replace(' ', '_') + ".zip", bytes.toByteArray());
			dirty = false;
		} catch (Throwable t) {
			EagRuntime.showPopup("Could not export texture pack: " + t.getClass().getSimpleName());
		}
	}

	private void saveProject() {
		try {
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			DataOutputStream out = new DataOutputStream(bytes);
			out.writeInt(0x54435031); // TCP1
			out.writeUTF(packName.getText());
			out.writeInt(editedTextures.size());
			for (Map.Entry<String, ImageData> entry : editedTextures.entrySet()) {
				ImageData data = entry.getValue();
				out.writeUTF(entry.getKey());
				out.writeShort(data.width);
				out.writeShort(data.height);
				out.writeBoolean(data.alpha);
				for (int pixel : data.pixels) out.writeInt(pixel);
			}
			out.close();
			EagRuntime.setStorage(PROJECT_STORAGE, bytes.toByteArray());
			EagRuntime.showPopup("Project saved locally.");
		} catch (Throwable t) {
			EagRuntime.showPopup("Could not save project: " + t.getClass().getSimpleName());
		}
	}

	private void loadProject() {
		byte[] saved = EagRuntime.getStorage(PROJECT_STORAGE);
		if (saved == null || saved.length == 0) {
			EagRuntime.showPopup("No saved texture project found.");
			return;
		}
		try {
			DataInputStream in = new DataInputStream(new java.io.ByteArrayInputStream(saved));
			if (in.readInt() != 0x54435031) throw new IOException("Unknown project format");
			String savedName = in.readUTF();
			int count = in.readInt();
			if (count < 0 || count > 256) throw new IOException("Invalid project texture count");
			Map<String, ImageData> loaded = new HashMap<String, ImageData>();
			for (int i = 0; i < count; ++i) {
				String path = in.readUTF();
				int w = in.readUnsignedShort(), h = in.readUnsignedShort();
				if (w < 1 || h < 1 || w > 128 || h > 128) throw new IOException("Invalid project texture size");
				ImageData data = new ImageData(w, h, in.readBoolean());
				for (int p = 0; p < data.pixels.length; ++p) data.pixels[p] = in.readInt();
				loaded.put(path, data);
			}
			editedTextures.clear();
			editedTextures.putAll(loaded);
			packName.setText(savedName);
			undoStack.clear();
			redoStack.clear();
			if (!editedTextures.isEmpty()) loadTexture(editedTextures.keySet().iterator().next());
			dirty = false;
			EagRuntime.showPopup("Project loaded.");
		} catch (Throwable t) {
			EagRuntime.showPopup("Could not load project: " + t.getClass().getSimpleName());
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
		if (px < 0 || py < 0 || px >= image.width || py >= image.height) return;
		if (tool == 2) { fill(px, py); return; }
		if (tool == 3) { selectedColor = image.pixels[py * image.width + px]; tool = 0; return; }
		int color = tool == 1 ? 0 : selectedColor;
		for (int yy = py - brushSize / 2; yy < py + (brushSize + 1) / 2; ++yy) {
			for (int xx = px - brushSize / 2; xx < px + (brushSize + 1) / 2; ++xx) {
				if (xx >= 0 && yy >= 0 && xx < image.width && yy < image.height) image.pixels[yy * image.width + xx] = color;
			}
		}
		markEdited();
	}

	private void fill(int x, int y) {
		int target = image.pixels[y * image.width + x];
		int replacement = tool == 1 ? 0 : selectedColor;
		if (target == replacement) return;
		List<Integer> queue = new ArrayList<Integer>();
		queue.add(y * image.width + x);
		for (int i = 0; i < queue.size(); ++i) {
			int pos = queue.get(i), px = pos % image.width, py = pos / image.width;
			if (image.pixels[pos] != target) continue;
			image.pixels[pos] = replacement;
			if (px > 0) queue.add(pos - 1);
			if (px + 1 < image.width) queue.add(pos + 1);
			if (py > 0) queue.add(pos - image.width);
			if (py + 1 < image.height) queue.add(pos + image.width);
		}
		markEdited();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		packName.mouseClicked(mouseX, mouseY, mouseButton);
		if ((mouseButton == 0 || mouseButton == 1) && isOnCanvas(mouseX, mouseY)) {
			recordUndo(); paintGesture = true;
			if (mouseButton == 1 && tool == 0) { int oldTool = tool; tool = 1; paint(mouseX, mouseY); tool = oldTool; }
			else paint(mouseX, mouseY);
		}
	}
	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (packName.textboxKeyTyped(typedChar, keyCode)) return;
		super.keyTyped(typedChar, keyCode);
	}
	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int button, long time) { super.mouseClickMove(mouseX, mouseY, button, time); if (button == 0 || button == 1) paint(mouseX, mouseY); }
	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) { super.mouseReleased(mouseX, mouseY, state); paintGesture = false; }

	private boolean isOnCanvas(int mouseX, int mouseY) {
		return image != null && mouseX >= canvasX && mouseY >= canvasY && mouseX < canvasX + image.width * canvasScale && mouseY < canvasY + image.height * canvasScale;
	}

	@Override
	public void handleMouseInput() throws IOException { super.handleMouseInput(); textureList.handleMouseInput(); }

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		drawDefaultBackground();
		drawCenteredString(fontRendererObj, "Texture Pack Creator 9000", width / 2, 2, 0xFFFFFF);
		packName.drawTextBox();
		drawString(fontRendererObj, "Texture: " + selectedPath, 224, 38, 0xCCCCCC);
		drawString(fontRendererObj, "Tool: " + (tool == 0 ? "Pencil" : tool == 1 ? "Eraser" : tool == 2 ? "Fill" : "Picker") + "  Brush: " + brushSize + "px", 224, height - 70, 0xAAAAAA);
		if (image != null) {
			canvasScale = Math.max(1, Math.min(12, 220 / Math.max(image.width, image.height) + zoomOffset));
			canvasX = width / 2 - 90; canvasY = 58;
			for (int y = 0; y < image.height; ++y) for (int x = 0; x < image.width; ++x) {
				int c = image.pixels[y * image.width + x];
				if ((c >>> 24) == 0) c = 0xFF555555;
				drawRect(canvasX + x * canvasScale, canvasY + y * canvasScale, canvasX + (x + 1) * canvasScale, canvasY + (y + 1) * canvasScale, c);
			}
			if (grid && canvasScale >= 3) {
				for (int x = 0; x <= image.width; ++x) drawRect(canvasX + x * canvasScale, canvasY, canvasX + x * canvasScale + 1, canvasY + image.height * canvasScale, 0x55333333);
				for (int y = 0; y <= image.height; ++y) drawRect(canvasX, canvasY + y * canvasScale, canvasX + image.width * canvasScale, canvasY + y * canvasScale + 1, 0x55333333);
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

	private ImageData getPreview(TextureEntry entry) {
		if (entry.preview != null) return entry.preview;
		ImageData source = editedTextures.get(entry.path);
		if (source == null) {
			try (IResource resource = mc.getResourceManager().getResource(new ResourceLocation(entry.path))) {
				source = ImageData.loadImageFile(readAll(resource.getInputStream()));
				if (source != null) source.swapRB();
			} catch (Throwable ignored) { }
		}
		if (source == null || source.width < 1 || source.height < 1) return null;
		entry.preview = source;
		return source;
	}

	private static class TextureEntry {
		final String path;
		ImageData preview;
		TextureEntry(String path) { this.path = path; }
	}
	private static class TextureList extends GuiSlot {
		private final GuiTexturePackCreator owner;
		TextureList(GuiTexturePackCreator owner, Minecraft mc, int width, int height, int top, int bottom, int slotHeight) { super(mc, width, height, top, bottom, slotHeight); this.owner = owner; }
		protected int getSize() { return owner.entries.size(); }
		protected void elementClicked(int slot, boolean dbl, int x, int y) { owner.selectTexture(slot); }
		protected boolean isSelected(int slot) { return owner.selectedPath.equals(owner.entries.get(slot).path); }
		protected void drawBackground() { owner.drawDefaultBackground(); }
		protected void func_192637_a(int slot, int x, int y, int h, int mouseX, int mouseY, float partial) {
			TextureEntry entry = owner.entries.get(slot);
			ImageData preview = owner.getPreview(entry);
			if (preview != null) {
				int thumbWidth = Math.min(16, preview.width), thumbHeight = Math.min(16, preview.height);
				for (int py = 0; py < thumbHeight; ++py) for (int px = 0; px < thumbWidth; ++px) {
					int sourceX = px * preview.width / thumbWidth;
					int sourceY = py * preview.height / thumbHeight;
					int color = preview.pixels[sourceY * preview.width + sourceX];
					if ((color >>> 24) == 0) color = 0xFF555555;
					owner.drawRect(x + 2 + px, y + 2 + py, x + 3 + px, y + 3 + py, color);
				}
			}
			owner.drawString(owner.fontRendererObj, entry.path.substring(entry.path.lastIndexOf('/') + 1), x + 22, y + 5, 0xFFFFFF);
		}
	}
}
