package net.minecraft.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Captures only CCTV-related server chat so the CCTV screen can show command results. */
public final class CCTVOutputState {
	private static final List<String> lines = new ArrayList<String>();

	private CCTVOutputState() {}

	public static synchronized void record(String message) {
		if (message == null) return;
		String lower = message.toLowerCase();
		if (!lower.contains("cctv") && !lower.contains("camera") && !lower.contains("group") && !lower.contains("computer")) return;
		for (String line : message.split("\\r?\\n")) {
			String clean = line.trim();
			if (!clean.isEmpty()) lines.add(clean);
		}
		while (lines.size() > 80) lines.remove(0);
	}

	public static synchronized List<String> snapshot() {
		List<String> copy = new ArrayList<String>(lines);
		Collections.reverse(copy);
		return copy;
	}
}
