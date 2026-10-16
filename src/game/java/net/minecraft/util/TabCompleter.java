package net.minecraft.util;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.network.play.client.CPacketTabComplete;
import net.minecraft.util.math.BlockPos;

public abstract class TabCompleter {
	/** The {@link GuiTextField} that is backing this {@link TabCompleter} */
	protected final GuiTextField textField;
	protected final boolean hasTargetBlock;
	protected boolean didComplete;
	protected boolean requestedCompletions;
	protected int completionIdx;
	protected List<String> completions = Lists.<String>newArrayList();
	private boolean popupVisible;
	private String requestedPrefix;
	private boolean automaticRequest;
	private List<String> rootCompletions;

	public TabCompleter(GuiTextField textFieldIn, boolean hasTargetBlockIn) {
		this.textField = textFieldIn;
		this.hasTargetBlock = hasTargetBlockIn;
	}

	/**
	 * Called when tab key pressed. If it's the first time we tried to complete this
	 * string, we ask the server for completions. When the server responds, this
	 * method gets called again (via setCompletions).
	 */
	public void complete() {
		if (this.didComplete && !this.completions.isEmpty()) {
			this.completionIdx = (this.completionIdx + 1) % this.completions.size();
			this.replaceCurrentWord(this.completions.get(this.completionIdx));
			this.popupVisible = true;
			return;
		}
		if (!this.requestedCompletions) {
			int i = this.textField.getNthWordFromPosWS(-1, this.textField.getCursorPosition(), false);
			this.completions.clear();
			this.completionIdx = 0;
			String s = this.textField.getText().substring(0, this.textField.getCursorPosition());
			List<String> filteredRoot = this.getRootCompletions(s);
			if (filteredRoot != null) {
				this.completions.addAll(filteredRoot);
				if ("/".equals(s)) {
					this.popupVisible = !this.completions.isEmpty();
					return;
				}
			} else {
				this.requestCompletions(s);
			}

			if (this.completions.isEmpty()) return;
			this.didComplete = true;
			this.textField.deleteFromCursor(i - this.textField.getCursorPosition());
		}
		if (!this.completions.isEmpty()) {
			if (!"/".equals(this.textField.getText().substring(0, this.textField.getCursorPosition()))) {
				this.replaceCurrentWord(this.completions.get(this.completionIdx));
			}
			this.popupVisible = true;
		}
	}

	private void replaceCurrentWord(String value) {
		this.textField.deleteFromCursor(0);
		this.textField.deleteFromCursor(this.textField.getNthWordFromPosWS(-1, this.textField.getCursorPosition(), false) - this.textField.getCursorPosition());
		this.textField.writeText(value);
	}

	private void requestCompletions(String prefix) {
		if (prefix.length() >= 1) {
			this.requestedPrefix = prefix;
			Minecraft.getMinecraft().player.connection
					.sendPacket(new CPacketTabComplete(prefix, this.getTargetBlockPos(), this.hasTargetBlock));
			this.requestedCompletions = true;
		}
	}

	@Nullable
	public abstract BlockPos getTargetBlockPos();

	/**
	 * Only actually sets completions if they were requested (via
	 * requestCompletions)
	 */
	public void setCompletions(String... newCompl) {
		if (this.requestedCompletions) {
			this.requestedCompletions = false;
			boolean automaticRequest = this.automaticRequest;
			this.automaticRequest = false;
			this.didComplete = false;
			this.completions.clear();

			for (String s : newCompl) {
				if (!s.isEmpty()) {
					this.completions.add(s);
				}
			}
			boolean rootRequest = "/".equals(this.requestedPrefix);
			if (rootRequest) {
				this.rootCompletions = new ArrayList<String>(this.completions);
			}

			String s1 = this.textField.getText().substring(this.textField.getNthWordFromPosWS(-1, this.textField.getCursorPosition(), false));
			String s2 = rootRequest || automaticRequest ? "" : org.apache.commons.lang3.StringUtils.getCommonPrefix(newCompl);

			if (!s2.isEmpty() && !s1.equalsIgnoreCase(s2)) {
				this.textField.deleteFromCursor(0);
				this.textField.deleteFromCursor(
						this.textField.getNthWordFromPosWS(-1, this.textField.getCursorPosition(), false)
								- this.textField.getCursorPosition());
				this.textField.writeText(s2);
			}
			if (!this.completions.isEmpty()) {
				this.didComplete = true;
				this.completionIdx = 0;
				if (!rootRequest && !automaticRequest) {
					this.replaceCurrentWord(this.completions.get(0));
				}
				this.popupVisible = true;
			}
		}
	}

	private List<String> getRootCompletions(String prefix) {
		if (prefix.startsWith("/") && prefix.indexOf(' ') < 0) {
			if (this.rootCompletions != null) {
				List<String> filtered = new ArrayList<String>();
				for (String completion : this.rootCompletions) {
					if (completion.regionMatches(true, 0, prefix, 0, prefix.length())) {
						filtered.add(completion);
					}
				}
				return filtered;
			}
		}
		return null;
	}

	/** Refreshes the visible command list from the cached root command index. */
	public void updateSuggestions() {
		if (this.requestedCompletions) return;
		String prefix = this.textField.getText().substring(0, this.textField.getCursorPosition());
		if (prefix.startsWith("/") && prefix.endsWith(" ")) {
			this.automaticRequest = true;
			this.requestCompletions(prefix);
			return;
		}
		if (prefix.startsWith("/") && prefix.indexOf(' ') < 0 && this.rootCompletions != null) {
			List<String> filtered = this.getRootCompletions(prefix);
			this.completions.clear();
			this.completions.addAll(filtered);
			this.completionIdx = 0;
			this.didComplete = false;
			this.popupVisible = !this.completions.isEmpty();
		} else {
			this.popupVisible = false;
			this.didComplete = false;
		}
	}

	public boolean isPopupVisible() { return this.popupVisible && !this.completions.isEmpty(); }

	public boolean handleNavigation(int keyCode) {
		if (!isPopupVisible()) return false;
		if (keyCode == 200) this.completionIdx = (this.completionIdx + this.completions.size() - 1) % this.completions.size();
		else if (keyCode == 208) this.completionIdx = (this.completionIdx + 1) % this.completions.size();
		else return false;
		this.replaceCurrentWord(this.completions.get(this.completionIdx));
		return true;
	}

	public boolean hideSuggestions() {
		if (!isPopupVisible()) return false;
		this.popupVisible = false;
		this.didComplete = false;
		return true;
	}

	public void drawSuggestions(FontRenderer font, int x, int bottom, int maxWidth) {
		if (!isPopupVisible()) return;
		int visible = Math.min(8, this.completions.size());
		int first = Math.max(0, Math.min(this.completionIdx - visible + 1, this.completions.size() - visible));
		int width = 20;
		for (int i = first; i < first + visible; ++i) width = Math.max(width, Math.min(maxWidth, font.getStringWidth(this.completions.get(i)) + 8));
		int top = bottom - visible * 12 - 4;
		Gui.drawRect(x, top, x + width, bottom, 0xE0000000);
		for (int row = 0; row < visible; ++row) {
			int index = first + row;
			int y = top + 2 + row * 12;
			if (index == this.completionIdx) Gui.drawRect(x + 1, y - 1, x + width - 1, y + 11, 0xFF555555);
			font.drawStringWithShadow(this.completions.get(index), x + 4, y, 0xFFFFFFFF);
		}
	}

	/**
	 * Called when new text is entered, or backspace pressed
	 */
	public void resetDidComplete() {
		this.didComplete = false;
		this.popupVisible = false;
	}

	public void resetRequested() {
		this.requestedCompletions = false;
		this.popupVisible = false;
	}
}
