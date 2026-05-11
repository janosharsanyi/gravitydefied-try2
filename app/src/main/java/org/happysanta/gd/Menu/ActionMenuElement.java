package org.happysanta.gd.Menu;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import org.happysanta.gd.Menu.Views.MenuImageView;
import org.happysanta.gd.Menu.Views.MenuTextView;
import org.happysanta.gd.R;
import org.happysanta.gd.Settings;

import static org.happysanta.gd.Helpers.getDp;
import static org.happysanta.gd.Helpers.getGDActivity;

public class ActionMenuElement
		extends ClickableMenuElement
		implements MenuHandler, MenuElement {

	protected static final int DISABLED_COLOR = 0xff999999;
	public static final int LOCK_IMAGE_MARGIN_RIGHT = 5;
	public static final int locks[] = new int[]{
			R.drawable.s_lock0,
			R.drawable.s_lock1,
			R.drawable.s_lock2
	};

	public static final int LINE_SPACING = 15;
	public static final int X_OFFSET = 48;

	public static final int OK = 0;
	public static final int BACK = 1;
	public static final int EXIT = 2;
	public static final int YES = 3;
	public static final int NO = 4;
	public static final int PLAY_MENU = 5;
	public static final int GO_TO_MAIN = 6;
	public static final int RESTART = 7;
	public static final int NEXT = 8;
	public static final int CONTINUE = 9;
	public static final int INSTALL = 10;
	public static final int LOAD = 11;
	public static final int SELECT_FILE = 12;
	public static final int DELETE = 13;
	public static final int RESTART_WITH_NEW_LEVEL = 14;
	public static final int SEND_LOGS = 15;
	public static final int OPEN_LEVELS_FOLDER = 16;
	public static final int CHANGE_LEVELS_FOLDER = 17;
	public static final int RESCAN_FOLDER = 18;
	public static final int REMOVE_FROM_LIST = 19;
	// Copy-into-Custom action values for the Colors submenu. Six values —
	// three slots × two families (track / neon). Used as the actionValue
	// on the ActionMenuElements inside the inline copy chooser screens
	// built in Menu so {@link Menu#handleAction} can dispatch them.
	public static final int COPY_TRACK_INTO_CUSTOM_1 = 20;
	public static final int COPY_TRACK_INTO_CUSTOM_2 = 21;
	public static final int COPY_TRACK_INTO_CUSTOM_3 = 22;
	public static final int COPY_NEON_INTO_CUSTOM_1 = 23;
	public static final int COPY_NEON_INTO_CUSTOM_2 = 24;
	public static final int COPY_NEON_INTO_CUSTOM_3 = 25;

	protected MenuHandler handler;
	protected boolean isLocked = false;
	protected boolean isBlackLock = true;
	protected MenuImageView lockImage = null;

	protected int actionValue = -1;

	public ActionMenuElement(String s, int value, MenuHandler handler) {
		actionValue = value;
		this.handler = handler;

		text = s;

		createAllViews();
	}

	@Override
	protected void createAllViews() {
		super.createAllViews();

		Context context = getGDActivity();
		lockImage = new MenuImageView(context);
		lockImage.setScaleType(ImageView.ScaleType.CENTER);
		lockImage.setVisibility(View.GONE);

		LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
		lp.setMargins(0, 0, getDp(ActionMenuElement.LOCK_IMAGE_MARGIN_RIGHT), 0);
		lockImage.setLayoutParams(lp);

		layout.addView(lockImage, 1);
	}

	public ActionMenuElement(String s, MenuHandler handler/*, MenuScreen screen*/) {
		this(s, -1, handler);
	}

	public int getActionValue() {
		return actionValue;
	}

	public void setHandler(MenuHandler hander) {
		this.handler = hander;
	}

	/**
	 * Pick the idle (non-highlighted) lock icon for the current theme:
	 * dark mode → lighter {@code s_lock1}, light mode → black {@code s_lock0}.
	 * Highlighted state stays {@code s_lock2} (green) in both themes.
	 */
	public static int idleLockResource() {
		return Settings.isDarkModeEnabled() ? locks[1] : locks[0];
	}

	// flag1 (formerly isBlackLock) is kept for source-compat with existing
	// callers but is now ignored — the icon variant follows the dark-mode
	// setting instead. refreshLockIcon() repaints already-built screens
	// when the user toggles dark mode.
	public void setLock(boolean flag, boolean flag1) {
		isLocked = flag;
		isBlackLock = flag1;

		lockImage.setVisibility(isLocked ? View.VISIBLE : View.GONE);
		lockImage.setImageResource(idleLockResource());
	}

	public void refreshLockIcon() {
		if (isLocked && !isHighlighted) {
			lockImage.setImageResource(idleLockResource());
		}
	}

	@Override
	public void setText(String s) {
		text = s;
		updateViewText();
	}

	@Override
	public void performAction(int k) {
		if (disabled || handler == null) return;

		if (k == MenuScreen.KEY_FIRE) {
			handler.handleAction(this);
		}
	}

	@Override
	protected void onHighlightChanged() {
		lockImage.setImageResource(isHighlighted ? locks[2] : idleLockResource());
	}

	@Override
	public MenuScreen getCurrentMenu() {
		return null;
	}

	@Override
	public void setCurrentMenu(MenuScreen e1, boolean flag) {
	}

	@Override
	public void handleAction(MenuElement item) {
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;

		if (disabled) {
			((MenuTextView) textView).setTextColor(DISABLED_COLOR);
		} else {
			((MenuTextView) textView).setTextColor(defaultColorStateList());
		}
	}

}
