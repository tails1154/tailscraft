package net.lax1dude.eaglercraft.sp.relay;

import java.util.List;

/**
 * Query state for fetching the shared worlds list from a relay.
 */
public interface RelayWorldsQuery {

	void update();

	boolean isQueryOpen();

	boolean isQueryFailed();

	void close();

	List<RelayWorld> getWorlds();

}
