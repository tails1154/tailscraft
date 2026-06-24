package net.lax1dude.eaglercraft.sp.relay;

/**
 * Metadata describing a world currently advertised by a relay.
 */
public class RelayWorld {

	public String relayURI;

	public final String code;
	public final String name;
	public final boolean hidden;
	public final java.util.List<String> iceServers;

	public RelayWorld(String code, String name, boolean hidden) {
		this(code, name, hidden, null);
	}

	public RelayWorld(String code, String name, boolean hidden, java.util.List<String> iceServers) {
		this.code = code;
		this.name = name;
		this.hidden = hidden;
		this.iceServers = iceServers;
	}

	public boolean openToLAN(int gameMode, boolean cheats) {
		return net.lax1dude.eaglercraft.sp.SingleplayerServerController.openWorldToLAN(this, gameMode, cheats);
	}

}
