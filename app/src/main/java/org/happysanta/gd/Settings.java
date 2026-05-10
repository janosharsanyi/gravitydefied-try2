package org.happysanta.gd;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Build;
import org.happysanta.gd.API.API;

import static org.happysanta.gd.Helpers.getGDActivity;

public class Settings {

	private static final String LEVEL_ID = "level_id";
	private static final int LEVEL_ID_DEFAULT = 0;

	private static final String PERSPECTIVE_ENABLED = "perspective_enabled";
	private static final boolean PERSPECTIVE_ENABLED_DEFAULT = true;

	// Driver shadow mode. The original game just had a boolean
	// "shadows on/off" — we widen that to also offer a few "neon" modes
	// that re-use the same render path but invert the brightness ramp:
	// instead of fading in the shadow as the bike rises (close = dark,
	// far = light gray) the neon modes fade *out* a glow under the bike
	// (close = bright neon base, far = #363636). Mode 0 keeps "off",
	// mode 1 is the original shadow; modes 2..7 are the neons. Default
	// stays "Shadow" so existing players see no visible change.
	public static final int SHADOW_MODE_OFF = 0;
	public static final int SHADOW_MODE_SHADOW = 1;
	public static final int SHADOW_MODE_NEON_YELLOW = 2;
	public static final int SHADOW_MODE_NEON_RED = 3;
	public static final int SHADOW_MODE_NEON_PURPLE = 4;
	public static final int SHADOW_MODE_NEON_BLUE = 5;
	public static final int SHADOW_MODE_NEON_CYAN = 6;
	public static final int SHADOW_MODE_NEON_GREEN = 7;
	private static final String SHADOW_MODE = "shadow_mode";
	private static final int SHADOW_MODE_DEFAULT = SHADOW_MODE_SHADOW;
	// Floor for the neon "proximity" metric. m_rI in Level._ifiIV measures
	// bike-body distance above ground — but the body sits above the wheels
	// by frame_height even when riding normally, and *collapses* to ~0 when
	// the bike crashes flat / upside-down. Without a floor, a crashed bike
	// reads as brighter than a normally-riding one. Clamping m_rI from below
	// at this baseline means anything from "crashed flat" up to "wheels on
	// ground" all render at max vibrancy; only true airtime starts the fade.
	// Tuned by playtest — increase to widen the "max bright" band.
	public static final int SHADOW_NEON_FULL_BELOW = 0x10000;

	private static final String DRIVER_SPRITE_ENABLED = "driver_sprite_enabled";
	private static final boolean DRIVER_SPRITE_ENABLED_DEFAULT = true;

	private static final String BIKE_SPRITE_ENABLED = "bike_sprite_enabled";
	private static final boolean BIKE_SPRITE_ENABLED_DEFAULT = true;

	private static final String INPUT_OPTION = "input_option";
	private static final int INPUT_OPTION_DEFAULT = 0;

	private static final String LOOK_AHEAD_ENABLED = "look_ahead_enabled";
	private static final boolean LOOK_AHEAD_ENABLED_DEFAULT = true;

	private static final String VIBRATE_ENABLED = "vibrate_enabled";
	private static final boolean VIBRATE_ENABLED_DEFAULT = true;

	private static final String KEYBOARD_IN_MENU_ENABLED = "keyboard_enabled";
	private static final boolean KEYBOARD_IN_MENU_ENABLED_DEFAULT = true;

	// Landscape-only setting: which side of the screen the on-screen keypad
	// In landscape "split-keypad" mode, controls which cluster lands on
	// which screen edge. 0 = normal (cluster A on left, B on right —
	// matches the layout described in MIGRATION_PLAN.md), 1 = flipped
	// (A and B swapped). Ignored in portrait, where the keypad always
	// docks to the bottom edge as a single grid.
	public static final int KEYPAD_SIDE_NORMAL = 0;
	public static final int KEYPAD_SIDE_FLIPPED = 1;
	private static final String KEYPAD_LANDSCAPE_SIDE = "keypad_landscape_side";
	private static final int KEYPAD_LANDSCAPE_SIDE_DEFAULT = KEYPAD_SIDE_NORMAL;

	// Auto-hide the on-screen keypad after touch goes idle. Touching the
	// screen always re-shows the keypad and resets the timer.
	//   -1 = Always visible (no timer)
	//    0 = Immediately (hide as soon as touch ends)
	//   >0 = hide N seconds after touch ends
	// Default Always so users without a pad don't lose their only input.
	// The manual "Keyboard in menu" toggle still wins — if the user
	// disabled it, the keypad stays hidden in menu regardless of touch.
	public static final int CONTROLLER_AUTOHIDE_ALWAYS = -1;
	public static final int CONTROLLER_AUTOHIDE_IMMEDIATELY = 0;
	public static final int[] CONTROLLER_AUTOHIDE_TIMEOUT_VALUES = {-1, 0, 5, 10, 15, 30};
	private static final String CONTROLLER_AUTOHIDE_TIMEOUT_SEC = "controller_autohide_timeout_sec";
	private static final int CONTROLLER_AUTOHIDE_TIMEOUT_SEC_DEFAULT = CONTROLLER_AUTOHIDE_ALWAYS;

	// Hide the status bar while the activity is in the foreground. The nav
	// bar stays visible (separate concern; deferred). With
	// BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE the user can swipe from the
	// top to peek the bar back. Default off — preserve the original
	// "system bars visible" feel; users who want a more fullscreen game
	// can opt in.
	private static final String IMMERSIVE_MODE_ENABLED = "immersive_mode_enabled";
	private static final boolean IMMERSIVE_MODE_ENABLED_DEFAULT = false;

	// AMOLED-friendly dark mode: flips the menu chrome and in-game sky to
	// pitch black (0x000000) and the menu text to white. The track and
	// sprites still draw their own colors — only the empty space behind them
	// goes black. Single source of truth: getMenuBgColor / getMenuFgColor /
	// getMenuItemColorStateList. Default off — keep the original light look.
	private static final String DARK_MODE_ENABLED = "dark_mode_enabled";
	private static final boolean DARK_MODE_ENABLED_DEFAULT = false;
	private static final int MENU_COLOR_LIGHT_BG = 0xffffffff;
	private static final int MENU_COLOR_LIGHT_FG = 0xff000000;
	private static final int MENU_COLOR_DARK_BG = 0xff000000;
	private static final int MENU_COLOR_DARK_FG = 0xffffffff;
	// Highlight color for pressed/focused/selected menu items — matches the
	// original menu_item_color.xml selector. Same green works on both light
	// and dark backgrounds.
	private static final int MENU_HIGHLIGHT_COLOR = 0xff00a000;

	private static final String LAST_SEND_STATS = "last_send_stats";
	private static final long LAST_SEND_STATS_DEFAULT = 0;

	private static final String NAME = "name";
	public static final String NAME_DEFAULT = "AAA";
	public static final byte[] NAME_CHARS_DEFALUT = new byte[]{65, 65, 65};

	private static final String LEVELS_SORT = "level_sort"; // in download list
	private static final int LEVELS_SORT_DEFAULT = 0;

	private static SharedPreferences preferences;

	static {
		preferences = getGDActivity().getSharedPreferences("GDSettings", Context.MODE_PRIVATE);
	}

	public static void resetAll() {
		setPerspectiveEnabled(PERSPECTIVE_ENABLED_DEFAULT);
		setShadowMode(SHADOW_MODE_DEFAULT);
		setDriverSpriteEnabled(DRIVER_SPRITE_ENABLED_DEFAULT);
		setBikeSpriteEnabled(BIKE_SPRITE_ENABLED_DEFAULT);
		setLookAheadEnabled(LOOK_AHEAD_ENABLED_DEFAULT);
		setVibrateOnTouchEnabled(VIBRATE_ENABLED_DEFAULT);
		setKeyboardInMenuEnabled(KEYBOARD_IN_MENU_ENABLED_DEFAULT);
		setKeypadLandscapeSide(KEYPAD_LANDSCAPE_SIDE_DEFAULT);
		setControllerAutoHideTimeoutSec(CONTROLLER_AUTOHIDE_TIMEOUT_SEC_DEFAULT);
		setImmersiveModeEnabled(IMMERSIVE_MODE_ENABLED_DEFAULT);
		setDarkModeEnabled(DARK_MODE_ENABLED_DEFAULT);
		setInputOption(INPUT_OPTION_DEFAULT);
		setLevelsSort(LEVELS_SORT_DEFAULT);
		setName(NAME_CHARS_DEFALUT);
	}

	public static long getLevelId() {
		return preferences.getLong(LEVEL_ID, LEVEL_ID_DEFAULT);
	}

	public static void setLevelId(long levelId) {
		setLong(LEVEL_ID, levelId);
	}

	public static boolean isPerspectiveEnabled() {
		return preferences.getBoolean(PERSPECTIVE_ENABLED, PERSPECTIVE_ENABLED_DEFAULT);
	}

	public static void setPerspectiveEnabled(boolean enabled) {
		setBoolean(PERSPECTIVE_ENABLED, enabled);
	}

	public static int getShadowMode() {
		int mode = preferences.getInt(SHADOW_MODE, SHADOW_MODE_DEFAULT);
		if (mode < SHADOW_MODE_OFF || mode > SHADOW_MODE_NEON_GREEN)
			return SHADOW_MODE_DEFAULT;
		return mode;
	}

	public static void setShadowMode(int mode) {
		setInt(SHADOW_MODE, mode);
	}

	// 0xRRGGBB base for a neon mode, or 0 if the mode is Off / classic Shadow
	// (callers should special-case those before calling this). Centralized so
	// the renderer and any future settings UI agree on the palette.
	public static int getShadowNeonBaseColor(int mode) {
		switch (mode) {
			case SHADOW_MODE_NEON_YELLOW: return 0xffFDD449;
			case SHADOW_MODE_NEON_RED:    return 0xffFD495B;
			case SHADOW_MODE_NEON_PURPLE: return 0xffE749FD;
			case SHADOW_MODE_NEON_BLUE:   return 0xff494BFD;
			case SHADOW_MODE_NEON_CYAN:   return 0xff49FDE8;
			case SHADOW_MODE_NEON_GREEN:  return 0xff54FD49;
			default: return 0;
		}
	}

	public static boolean isDriverSpriteEnabled() {
		return preferences.getBoolean(DRIVER_SPRITE_ENABLED, DRIVER_SPRITE_ENABLED_DEFAULT);
	}

	public static void setDriverSpriteEnabled(boolean enabled) {
		setBoolean(DRIVER_SPRITE_ENABLED, enabled);
	}

	public static boolean isBikeSpriteEnabled() {
		return preferences.getBoolean(BIKE_SPRITE_ENABLED, BIKE_SPRITE_ENABLED_DEFAULT);
	}

	public static void setBikeSpriteEnabled(boolean enabled) {
		setBoolean(BIKE_SPRITE_ENABLED, enabled);
	}

	public static boolean isLookAheadEnabled() {
		return preferences.getBoolean(LOOK_AHEAD_ENABLED, LOOK_AHEAD_ENABLED_DEFAULT);
	}

	public static void setLookAheadEnabled(boolean enabled) {
		setBoolean(LOOK_AHEAD_ENABLED, enabled);
	}

	public static boolean isKeyboardInMenuEnabled() {
		return preferences.getBoolean(KEYBOARD_IN_MENU_ENABLED, KEYBOARD_IN_MENU_ENABLED_DEFAULT);
	}

	public static void setKeyboardInMenuEnabled(boolean enabled) {
		setBoolean(KEYBOARD_IN_MENU_ENABLED, enabled);
	}

	public static int getKeypadLandscapeSide() {
		return preferences.getInt(KEYPAD_LANDSCAPE_SIDE, KEYPAD_LANDSCAPE_SIDE_DEFAULT);
	}

	public static void setKeypadLandscapeSide(int side) {
		setInt(KEYPAD_LANDSCAPE_SIDE, side);
	}

	public static int getControllerAutoHideTimeoutSec() {
		return preferences.getInt(CONTROLLER_AUTOHIDE_TIMEOUT_SEC, CONTROLLER_AUTOHIDE_TIMEOUT_SEC_DEFAULT);
	}

	public static void setControllerAutoHideTimeoutSec(int seconds) {
		setInt(CONTROLLER_AUTOHIDE_TIMEOUT_SEC, seconds);
	}

	public static boolean isImmersiveModeEnabled() {
		return preferences.getBoolean(IMMERSIVE_MODE_ENABLED, IMMERSIVE_MODE_ENABLED_DEFAULT);
	}

	public static void setImmersiveModeEnabled(boolean enabled) {
		setBoolean(IMMERSIVE_MODE_ENABLED, enabled);
	}

	public static boolean isDarkModeEnabled() {
		return preferences.getBoolean(DARK_MODE_ENABLED, DARK_MODE_ENABLED_DEFAULT);
	}

	public static void setDarkModeEnabled(boolean enabled) {
		setBoolean(DARK_MODE_ENABLED, enabled);
	}

	// Single source of truth for the menu/in-game background color. Returns
	// black when dark mode is on, white otherwise.
	public static int getMenuBgColor() {
		return isDarkModeEnabled() ? MENU_COLOR_DARK_BG : MENU_COLOR_LIGHT_BG;
	}

	// Single source of truth for the menu text/foreground color. Inverse of
	// getMenuBgColor.
	public static int getMenuFgColor() {
		return isDarkModeEnabled() ? MENU_COLOR_DARK_FG : MENU_COLOR_LIGHT_FG;
	}

	// On-screen keypad row background. ~60% white in light mode (matches the
	// original 0x99ffffff that reduces playfield obscuration); ~60% black in
	// dark mode (mirrors the same translucency over the dark sky).
	public static int getKeypadRowBgColor() {
		return isDarkModeEnabled() ? 0x99000000 : 0x99ffffff;
	}

	// On-screen keypad button text color. Tracks getMenuFgColor so the
	// digits stay legible against getKeypadRowBgColor in either theme.
	public static int getKeypadTextColor() {
		return getMenuFgColor();
	}

	// Programmatic replacement for the static R.drawable.menu_item_color
	// selector — needed because the foreground color depends on dark mode
	// and a static XML selector can't follow that. Highlight color is the
	// same green in both modes.
	public static ColorStateList getMenuItemColorStateList() {
		int[][] states = new int[][]{
				new int[]{android.R.attr.state_pressed},
				new int[]{android.R.attr.state_focused},
				new int[]{android.R.attr.state_selected},
				new int[]{}
		};
		int[] colors = new int[]{
				MENU_HIGHLIGHT_COLOR,
				MENU_HIGHLIGHT_COLOR,
				MENU_HIGHLIGHT_COLOR,
				getMenuFgColor()
		};
		return new ColorStateList(states, colors);
	}

	public static boolean isVibrateOnTouchEnabled() {
		return preferences.getBoolean(VIBRATE_ENABLED, VIBRATE_ENABLED_DEFAULT);
	}

	public static void setVibrateOnTouchEnabled(boolean enabled) {
		setBoolean(VIBRATE_ENABLED, enabled);
	}

	public static int getInputOption() {
		return preferences.getInt(INPUT_OPTION, INPUT_OPTION_DEFAULT);
	}

	public static void setInputOption(int value) {
		setInt(INPUT_OPTION, value);
	}

	public static long getLastSendStats() {
		return preferences.getLong(LAST_SEND_STATS, LAST_SEND_STATS_DEFAULT);
	}

	public static void setLastSendStats(long value) {
		setLong(LAST_SEND_STATS, value);
	}

	public static API.LevelsSortType getLevelsSort() {
		return API.getSortTypeById(preferences.getInt(LEVELS_SORT, LEVELS_SORT_DEFAULT));
	}

	public static void setLevelsSort(API.LevelsSortType type) {
		setInt(LEVELS_SORT, API.getIdBySortType(type));
	}

	public static void setLevelsSort(int type) {
		setInt(LEVELS_SORT, type);
	}

	public static byte[] getName() {
		String name = preferences.getString(NAME, NAME_DEFAULT);
		if (name.length() < 3) {
			name = NAME_DEFAULT;
		}
		return new byte[]{
				(byte) name.charAt(0),
				(byte) name.charAt(1),
				(byte) name.charAt(2)
		};
	}

	public static void setName(byte[] chars) {
		if (chars.length < 3) {
			setString(NAME, NAME_DEFAULT);
		} else {
			String name = "";
			for (int i = 0; i < 3; i++) {
				name += String.valueOf((char) chars[i]);
			}
			setString(NAME, name);
		}
	}

	private static void setLong(String key, long value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putLong(key, value);
		editorApply(editor);
	}

	private static void setInt(String key, int value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putInt(key, value);
		editorApply(editor);
	}

	private static void setBoolean(String key, boolean value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putBoolean(key, value);
		editorApply(editor);
	}

	private static void setString(String key, String value) {
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(key, value);
		editorApply(editor);
	}

	private static void editorApply(SharedPreferences.Editor editor) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD)
			editor.apply();
		else
			editor.commit();
	}

}
