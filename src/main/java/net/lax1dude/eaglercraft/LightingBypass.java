package net.lax1dude.eaglercraft;

/** Client-only fullbright switch used by the TailsCraft lighting bypass. */
public final class LightingBypass {
	private static volatile boolean enabled;

	private LightingBypass() {
	}

	public static boolean isEnabled() {
		return enabled;
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}
}
