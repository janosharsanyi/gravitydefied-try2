package org.happysanta.gd.Menu;

import java.util.ArrayList;
import java.util.List;

/**
 * Dedicated submenu under Options that hosts every color-related
 * setting: track color preset + track gradient + 6 in-place editable RGB
 * channel rows (FG R/G/B + BG R/G/B), neon color preset + 3 in-place
 * editable RGB channel rows. Built-in presets are read-only; selecting a
 * channel row on a built-in routes through the copy-into-Custom chooser
 * instead of in-place edit (see {@link ChannelMenuElement}).
 *
 * Overrides {@link MenuScreen#performAction} because the base class
 * intercepts KEY_UP / KEY_DOWN for row navigation and never delivers
 * them to the highlighted element. While a ChannelMenuElement on this
 * screen is in edit mode, UP/DOWN must reach the element so it can
 * cycle the active digit — we route accordingly here. All other keys
 * (including UP/DOWN when not editing) flow through the base class as
 * usual.
 */
public class ColorsMenuScreen extends MenuScreen {

	private final List<ChannelMenuElement> trackRows = new ArrayList<>();
	private final List<ChannelMenuElement> neonRows = new ArrayList<>();

	public ColorsMenuScreen(String title, MenuScreen navTarget) {
		super(title, navTarget);
	}

	/** Register a row that belongs to the track FG/BG channel group. */
	public void registerTrackRow(ChannelMenuElement row) {
		trackRows.add(row);
	}

	/** Register a row that belongs to the neon channel group. */
	public void registerNeonRow(ChannelMenuElement row) {
		neonRows.add(row);
	}

	/** Re-pull values from Settings on every track row. Called after a
	 * track preset change or after a copy-into-Custom completes. */
	public void refreshTrackRows() {
		for (ChannelMenuElement row : trackRows) row.refresh();
	}

	public void refreshNeonRows() {
		for (ChannelMenuElement row : neonRows) row.refresh();
	}

	@Override
	public void performAction(int k) {
		if (selectedIndex >= 0 && selectedIndex < menuItems.size()) {
			Object el = menuItems.elementAt(selectedIndex);
			if (el instanceof ChannelMenuElement && ((ChannelMenuElement) el).isEditing()) {
				((ChannelMenuElement) el).performAction(k);
				return;
			}
		}
		super.performAction(k);
	}

	/**
	 * If the highlighted row is a channel editor in edit mode, BACK
	 * commits the in-progress digits and stays on the screen — matching
	 * the user expectation that BACK exits the editor first, the screen
	 * second. Otherwise BACK falls through to the default screen pop.
	 */
	@Override
	public boolean handleBack() {
		if (selectedIndex >= 0 && selectedIndex < menuItems.size()) {
			Object el = menuItems.elementAt(selectedIndex);
			if (el instanceof ChannelMenuElement && ((ChannelMenuElement) el).isEditing()) {
				((ChannelMenuElement) el).commitIfEditing();
				return true;
			}
		}
		return false;
	}
}
