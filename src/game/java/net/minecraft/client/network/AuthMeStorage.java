package net.minecraft.client.network;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.lax1dude.eaglercraft.EagRuntime;

/** Local, opt-in storage for per-server AuthMe credentials. */
public final class AuthMeStorage {
	private static final String SERVERS_KEY = "tailscraft_authme_servers";

	private AuthMeStorage() {
	}

	public static String key(String server) {
		return "tailscraft_authme_" + server.replaceAll("[^A-Za-z0-9_.-]", "_");
	}

	public static String get(String server) {
		byte[] stored = EagRuntime.getStorage(key(server));
		return stored == null || stored.length == 0 ? null : new String(stored, StandardCharsets.UTF_8);
	}

	public static void save(String server, String password) {
		EagRuntime.setStorage(key(server), password.getBytes(StandardCharsets.UTF_8));
		List<String> servers = getServers();
		if (!servers.contains(server)) {
			servers.add(server);
			writeServers(servers);
		}
	}

	public static void forget(String server) {
		EagRuntime.setStorage(key(server), new byte[0]);
		List<String> servers = getServers();
		if (servers.remove(server)) writeServers(servers);
	}

	public static List<String> getServers() {
		byte[] stored = EagRuntime.getStorage(SERVERS_KEY);
		List<String> result = new ArrayList<String>();
		if (stored == null || stored.length == 0) return result;
		String value = new String(stored, StandardCharsets.UTF_8);
		for (String server : value.split("\\n")) {
			if (server.length() > 0 && !result.contains(server)) result.add(server);
		}
		return result;
	}

	private static void writeServers(List<String> servers) {
		StringBuilder value = new StringBuilder();
		for (String server : servers) {
			if (value.length() > 0) value.append('\n');
			value.append(server);
		}
		EagRuntime.setStorage(SERVERS_KEY, value.toString().getBytes(StandardCharsets.UTF_8));
	}
}
