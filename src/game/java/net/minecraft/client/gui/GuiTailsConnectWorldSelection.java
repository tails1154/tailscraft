package net.minecraft.client.gui;

/** Selects an existing local save before starting TC5 hosting. */
public class GuiTailsConnectWorldSelection extends GuiWorldSelection {
	private final int players;

	public GuiTailsConnectWorldSelection(GuiScreen parent, int players) {
		super(parent);
		this.players = players;
	}

	@Override
	public void postInit() {
		super.postInit();
		for (GuiButton button : buttonList) {
			if (button.id == 1) button.displayString = "Host TC5 World";
		}
	}

	@Override
	protected void selectWorldEntry(GuiListWorldSelectionEntry entry) {
		entry.hostWorldOnTailsConnect(players);
	}
}
