package net.minecraft.client.multiplayer;

import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.lax1dude.eaglercraft.EagRuntime;
import net.lax1dude.eaglercraft.cookie.ServerCookieDataStore;
import net.lax1dude.eaglercraft.internal.EnumEaglerConnectionState;
import net.lax1dude.eaglercraft.internal.IWebSocketClient;
import net.lax1dude.eaglercraft.internal.IWebSocketFrame;
import net.lax1dude.eaglercraft.internal.PlatformNetworking;
import net.lax1dude.eaglercraft.socket.AddressResolver;
import net.lax1dude.eaglercraft.socket.ConnectionHandshake;
import net.lax1dude.eaglercraft.socket.RateLimitTracker;
import net.lax1dude.eaglercraft.socket.WebSocketNetworkManager;
import net.lax1dude.eaglercraft.socket.protocol.GamePluginMessageConstants;
import net.lax1dude.eaglercraft.socket.protocol.GamePluginMessageProtocol;
import net.lax1dude.eaglercraft.socket.protocol.client.GameProtocolMessageController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.util.text.TextComponentString;

/**+
 * This portion of EaglercraftX contains deobfuscated Minecraft 1.8 source code.
 * 
 * Minecraft 1.8.8 bytecode is (c) 2015 Mojang AB. "Do not distribute!"
 * Mod Coder Pack v9.18 deobfuscation configs are (c) Copyright by the MCP Team
 * 
 * EaglercraftX 1.8 patch files (c) 2022-2025 lax1dude, ayunami2000. All Rights Reserved.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * 
 */
public class GuiConnecting extends GuiScreen {
	private static final Logger logger = LogManager.getLogger();
	private IWebSocketClient webSocket;
	private NetworkManager networkManager;
	private String currentAddress;
	private String currentPassword;
	private boolean allowPlaintext;
	private boolean allowCookies;
	private boolean cancel;
	private boolean hasOpened;
	private final GuiScreen previousGuiScreen;
	private int timer = 0;
	private long connectionStartedAt;
	private String connectionPhase = "Preparing connection";
	private String connectionDetail = "Waiting for the client networking layer";

	public GuiConnecting(GuiScreen parGuiScreen, Minecraft mcIn, ServerData parServerData) {
		this(parGuiScreen, mcIn, parServerData, false);
	}

	public GuiConnecting(GuiScreen parGuiScreen, Minecraft mcIn, ServerData parServerData, boolean allowPlaintext) {
		this(parGuiScreen, mcIn, parServerData, null, allowPlaintext);
	}

	public GuiConnecting(GuiScreen parGuiScreen, Minecraft mcIn, ServerData parServerData, String password) {
		this(parGuiScreen, mcIn, parServerData, password, false);
	}

	public GuiConnecting(GuiScreen parGuiScreen, Minecraft mcIn, ServerData parServerData, String password,
			boolean allowPlaintext) {
		this.mc = mcIn;
		this.previousGuiScreen = parGuiScreen;
		String serveraddress = AddressResolver.resolveURI(parServerData);
		mcIn.loadWorld((WorldClient) null);
		mcIn.setServerData(parServerData);
		if (RateLimitTracker.isLockedOut(serveraddress)) {
			logger.error("Server locked this client out on a previous connection, will not attempt to reconnect");
		} else {
			this.connect(serveraddress, password, allowPlaintext,
					parServerData.enableCookies && EagRuntime.getConfiguration().isEnableServerCookies());
		}
	}

	public GuiConnecting(GuiScreen parGuiScreen, Minecraft mcIn, String hostName, int port) {
		this(parGuiScreen, mcIn, hostName, port, false, EagRuntime.getConfiguration().isEnableServerCookies());
	}

	public GuiConnecting(GuiScreen parGuiScreen, Minecraft mcIn, String hostName, int port, boolean allowCookies) {
		this(parGuiScreen, mcIn, hostName, port, false, allowCookies);
	}

	public GuiConnecting(GuiScreen parGuiScreen, Minecraft mcIn, String hostName, int port, boolean allowPlaintext,
			boolean allowCookies) {
		this(parGuiScreen, mcIn, hostName, port, null, allowPlaintext, allowCookies);
	}

	public GuiConnecting(GuiScreen parGuiScreen, Minecraft mcIn, String hostName, int port, String password,
			boolean allowCookies) {
		this(parGuiScreen, mcIn, hostName, port, password, false, allowCookies);
	}

	public GuiConnecting(GuiScreen parGuiScreen, Minecraft mcIn, String hostName, int port, String password,
			boolean allowPlaintext, boolean allowCookies) {
		this.mc = mcIn;
		this.previousGuiScreen = parGuiScreen;
		mcIn.loadWorld((WorldClient) null);
		this.connect(hostName, password, allowPlaintext,
				allowCookies && EagRuntime.getConfiguration().isEnableServerCookies());
	}

	public GuiConnecting(GuiConnecting previous, String password) {
		this(previous, password, false);
	}

	public GuiConnecting(GuiConnecting previous, String password, boolean allowPlaintext) {
		this.mc = previous.mc;
		this.previousGuiScreen = previous.previousGuiScreen;
		this.connect(previous.currentAddress, password, allowPlaintext, previous.allowCookies);
	}

	private void connect(String ip, String password, boolean allowPlaintext, boolean allowCookies) {
		this.currentAddress = ip;
		this.currentPassword = password;
		this.allowPlaintext = allowPlaintext;
		this.allowCookies = allowCookies;
		this.connectionStartedAt = System.currentTimeMillis();
		this.connectionPhase = "Preparing connection";
		this.connectionDetail = "Resolving the requested server address";
	}

	/**+
	 * Called from the main game loop to update the screen.
	 */
	public void updateScreen() {
		++timer;
		if (timer > 1) {
			if (this.currentAddress == null) {
				this.connectionPhase = "Rate limited";
				this.connectionDetail = "The server previously blocked this client";
				mc.displayGuiScreen(GuiDisconnected.createRateLimitKick(previousGuiScreen));
			} else if (webSocket == null) {
				this.connectionPhase = "Opening WebSocket";
				this.connectionDetail = "Creating a browser transport to " + currentAddress;
				logger.info("Connecting to: {}", currentAddress);
				webSocket = PlatformNetworking.openWebSocket(currentAddress);
				if (webSocket == null) {
					mc.displayGuiScreen(new GuiDisconnected(previousGuiScreen, "connect.failed",
							new TextComponentString("Could not open WebSocket to \"" + currentAddress + "\"!")));
				}
			} else {
				if (webSocket.getState() == EnumEaglerConnectionState.CONNECTED) {
					this.connectionPhase = "WebSocket connected";
					this.connectionDetail = "Transport is open; starting the Eagler handshake";
					if (!hasOpened) {
						hasOpened = true;
						logger.info("Logging in: {}", currentAddress);
						this.connectionPhase = "Authenticating";
						this.connectionDetail = "Negotiating protocol, profile, cookies, and server capabilities";
						byte[] cookieData = null;
						if (allowCookies) {
							ServerCookieDataStore.ServerCookie cookie = ServerCookieDataStore
									.loadCookie(currentAddress);
							if (cookie != null) {
								cookieData = cookie.cookie;
							}
						}
						if (ConnectionHandshake.attemptHandshake(this.mc, webSocket, this, previousGuiScreen,
								currentPassword, allowPlaintext, allowCookies, cookieData)) {
							logger.info("Handshake Success");
							this.connectionPhase = "Handshake complete";
							this.connectionDetail = "Switching the connection into the PLAY state";
							webSocket.setEnableStringFrames(false);
							webSocket.clearStringFrames();
							this.networkManager = new WebSocketNetworkManager(webSocket);
							this.networkManager.setPluginInfo(ConnectionHandshake.pluginBrand,
									ConnectionHandshake.pluginVersion);
							//TODO
							//mc.bungeeOutdatedMsgTimer = 80;
							mc.clearTitles();
							this.networkManager.setConnectionState(EnumConnectionState.PLAY);
							NetHandlerPlayClient netHandler = new NetHandlerPlayClient(this.mc, previousGuiScreen,
									this.networkManager, this.mc.getSession().getProfile());
							this.networkManager.setNetHandler(netHandler);
							netHandler.setEaglerMessageController(new GameProtocolMessageController(
									GamePluginMessageProtocol.getByVersion(ConnectionHandshake.protocolVersion),
									GamePluginMessageConstants.CLIENT_TO_SERVER,
									GameProtocolMessageController
											.createClientHandler(ConnectionHandshake.protocolVersion, netHandler),
									(ch, msg) -> netHandler.sendPacket(new CPacketCustomPayload(ch, msg))));
						} else {
							if (mc.currentScreen == this) {
								checkRatelimit();
								logger.info("Handshake Failure");
								mc.getSession().reset();
								mc.displayGuiScreen(
										new GuiDisconnected(previousGuiScreen, "connect.failed", new TextComponentString(
												"Handshake Failure\n\nAre you sure this is an eagler 1.12 server?")));
							}
							webSocket.close();
							return;
						}
					}
					if (this.networkManager != null) {
						this.connectionPhase = "Loading play connection";
						this.connectionDetail = "Processing packets while the server prepares the world";
						try {
							this.networkManager.processReceivedPackets();
						} catch (IOException ex) {
						}
					}
				} else {
					this.connectionPhase = "Waiting for WebSocket";
					this.connectionDetail = "Browser transport state: " + webSocket.getState();
					if (webSocket.getState() == EnumEaglerConnectionState.FAILED) {
						this.connectionPhase = "WebSocket failed";
						this.connectionDetail = "The browser could not establish a transport to the server";
						if (!hasOpened) {
							mc.getSession().reset();
							checkRatelimit();
							if (mc.currentScreen == this) {
								if (RateLimitTracker.isProbablyLockedOut(currentAddress)) {
									mc.displayGuiScreen(GuiDisconnected.createRateLimitKick(previousGuiScreen));
								} else {
									mc.displayGuiScreen(new GuiDisconnected(previousGuiScreen, "connect.failed",
											new TextComponentString("Connection Refused")));
								}
							}
						}
					} else {
						if (this.networkManager != null && this.networkManager.checkDisconnected()) {
							this.mc.getSession().reset();
							checkRatelimit();
							if (mc.currentScreen == this) {
								if (RateLimitTracker.isProbablyLockedOut(currentAddress)) {
									mc.displayGuiScreen(GuiDisconnected.createRateLimitKick(previousGuiScreen));
								} else {
									mc.displayGuiScreen(new GuiDisconnected(previousGuiScreen, "connect.failed",
											new TextComponentString("Connection Refused")));
								}
							}
						}
					}
				}
			}
			if (timer > 200) {
				this.connectionPhase = "Connection timed out";
				this.connectionDetail = "No usable play connection after 10 seconds; cancel and try again";
				if (webSocket != null) {
					webSocket.close();
				}
				mc.displayGuiScreen(new GuiDisconnected(previousGuiScreen, "connect.failed",
						new TextComponentString("Please try rejoning.")));
			}
		}
	}

	/**+
	 * Fired when a key is typed (except F11 which toggles full
	 * screen). This is the equivalent of
	 * KeyListener.keyTyped(KeyEvent e). Args : character (character
	 * on the key), keyCode (lwjgl Keyboard key code)
	 */
	protected void keyTyped(char parChar1, int parInt1) {
	}

	/**+
	 * Adds the buttons (and other controls) to the screen in
	 * question. Called when the GUI is displayed and when the
	 * window resizes, the buttonList is cleared beforehand.
	 */
	public void initGui() {
		this.buttonList.clear();
		this.buttonList.add(
				new GuiButton(0, this.width / 2 - 100, this.height / 2 + 58, I18n.format("gui.cancel", new Object[0])));
	}

	/**+
	 * Called by the controls from the buttonList when activated.
	 * (Mouse pressed for buttons)
	 */
	protected void actionPerformed(GuiButton parGuiButton) {
		if (parGuiButton.id == 0) {
			this.cancel = true;
			if (this.networkManager != null) {
				this.networkManager.closeChannel(new TextComponentString("Aborted"));
			} else if (this.webSocket != null) {
				this.webSocket.close();
			}

			this.mc.displayGuiScreen(this.previousGuiScreen);
		}

	}

	/**+
	 * Draws the screen and all the components in it. Args : mouseX,
	 * mouseY, renderPartialTicks
	 */
	public void drawScreen(int i, int j, float f) {
		this.drawDefaultBackground();
		int top = this.height / 2 - 112;
		this.drawCenteredString(this.fontRendererObj, "Connecting to server", this.width / 2, top, 0xFFFFFF);
		this.drawCenteredString(this.fontRendererObj, this.connectionPhase, this.width / 2, top + 24, 0x55FFFF);
		this.drawCenteredString(this.fontRendererObj, this.connectionDetail, this.width / 2, top + 40, 0xDDDDDD);
		this.drawConnectionDetail("Target: " + (currentAddress == null ? "unknown" : currentAddress), top + 68, 0xFFFFFF);
		this.drawConnectionDetail("Elapsed: " + ((System.currentTimeMillis() - connectionStartedAt) / 1000L) + " seconds", top + 82, 0xAAAAAA);
		this.drawConnectionDetail("Transport: " + (webSocket == null ? "not opened" : webSocket.getState()), top + 96, 0xAAAAAA);
		this.drawConnectionDetail("Handshake: " + (hasOpened ? "started" : "not started") + " | Network manager: " + (networkManager == null ? "not created" : "created"), top + 110, 0xAAAAAA);
		this.drawConnectionDetail("Options: plaintext=" + allowPlaintext + " | cookies=" + allowCookies, top + 124, 0xAAAAAA);
		this.drawConnectionDetail("Packets are processed on the normal game loop; Cancel remains available below.", top + 148, 0x777777);

		super.drawScreen(i, j, f);
	}

	private void drawConnectionDetail(String text, int y, int color) {
		String clipped = this.fontRendererObj.trimStringToWidth(text, this.width - 24);
		this.drawCenteredString(this.fontRendererObj, clipped, this.width / 2, y, color);
	}

	private void checkRatelimit() {
		if (this.webSocket != null) {
			List<IWebSocketFrame> strFrames = webSocket.getNextStringFrames();
			if (strFrames != null) {
				for (int i = 0; i < strFrames.size(); ++i) {
					String str = strFrames.get(i).getString();
					if (str.equalsIgnoreCase("BLOCKED")) {
						RateLimitTracker.registerBlock(currentAddress);
						mc.displayGuiScreen(GuiDisconnected.createRateLimitKick(previousGuiScreen));
						logger.info("Handshake Failure: Too Many Requests!");
					} else if (str.equalsIgnoreCase("LOCKED")) {
						RateLimitTracker.registerLockOut(currentAddress);
						mc.displayGuiScreen(GuiDisconnected.createRateLimitKick(previousGuiScreen));
						logger.info("Handshake Failure: Too Many Requests!");
						logger.info("Server has locked this client out");
					}
				}
			}
		}
	}

	public boolean canCloseGui() {
		return false;
	}
}
