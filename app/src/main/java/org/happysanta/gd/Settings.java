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
	// Track color preset. Original keeps the legacy single-color render
	// (perspective-on draws one green line per segment, perspective-off the
	// same in brighter green; no background contour, no across gradient).
	// Every other preset opts in to the new 3-color render. Naming follows
	// the visual depth of the two lines, *not* draw order:
	//   - FG (foreground / lower) = the actual ground contour line, drawn
	//     between adjacent ground points. Brighter of the two colors.
	//   - BG (background / upper) = the raised perspective projection line,
	//     drawn between the projected points. Darker of the two colors.
	//   - Across tick is a gradient from FG (ground end) up to BG (raised
	//     end). Per design rule the BG luma is always lower than the FG.
	// FG/BG color values are HSL-matched against a cyan reference (FG ≈
	// S=85% L=77%, BG ≈ S=23% L=48%), except Green which uses the original
	// perspective color (#00AA00) as FG and the same hue dimmed by the
	// cyan FG→BG ratio (~0.61) for BG. Black/White is theme-aware via
	// isDarkModeEnabled.
	public static final int TRACK_COLOR_ORIGINAL = 0;
	public static final int TRACK_COLOR_GREEN = 1;
	public static final int TRACK_COLOR_CYAN = 2;
	public static final int TRACK_COLOR_RED = 3;
	public static final int TRACK_COLOR_YELLOW = 4;
	public static final int TRACK_COLOR_LIME = 5;
	public static final int TRACK_COLOR_BLUE = 6;
	public static final int TRACK_COLOR_GRAY = 7;
	public static final int TRACK_COLOR_BW = 8;
	private static final String TRACK_COLOR = "track_color";
	private static final int TRACK_COLOR_DEFAULT = TRACK_COLOR_ORIGINAL;

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

	// Auto-hide the on-screen keypad after touch goes idle. For >0
	// timeouts, touching the screen re-shows the keypad and resets the
	// timer; for 0 it's a hard off-switch.
	//   -1 = Always show (no timer)
	//    0 = Always Hide (keypad fully disabled — does not show in menu
	//        or in-game, touch does not wake it). Useful for controller-
	//        only play; also fixes the menu case where a momentarily-
	//        woken keypad would obscure the bottom items and eat scroll.
	//   >0 = hide N seconds after touch ends; touch wakes
	// Default Always so users without a pad don't lose their only input.
	// The manual "Keyboard in menu" toggle still wins — if the user
	// disabled it, the keypad stays hidden in menu regardless of touch.
	public static final int CONTROLLER_AUTOHIDE_ALWAYS_SHOW = -1;
	public static final int[] CONTROLLER_AUTOHIDE_TIMEOUT_VALUES = {-1, 0, 5, 10, 15, 30};
	private static final String CONTROLLER_AUTOHIDE_TIMEOUT_SEC = "controller_autohide_timeout_sec";
	private static final int CONTROLLER_AUTOHIDE_TIMEOUT_SEC_DEFAULT = CONTROLLER_AUTOHIDE_ALWAYS_SHOW;

	// Stick deadzone for the analog gamepad channel — axis values whose
	// magnitude is below this fraction of full deflection are treated as
	// "centered" and don't drive the physics engine. Stored as a percent
	// (* 100) to keep prefs readable and avoid float pref boilerplate;
	// the controller divides by 100f before use. Order matches
	// R.array.stick_deadzone_options (Precise / Default / Forgiving /
	// Very forgiving). Default = 15% — same value the controller used
	// hardcoded before this preset was added.
	public static final int[] STICK_DEADZONE_PCT_VALUES = {8, 15, 25, 40};
	private static final String STICK_DEADZONE_PCT = "stick_deadzone_pct";
	private static final int STICK_DEADZONE_PCT_DEFAULT = 15;

	// Stick mode for the analog gamepad channel. Analog (default) feeds
	// magnitude-proportional values into the physics engine — half-pull
	// produces half torque/throttle. Digital snaps each axis past the
	// deadzone to ±1 / 0, making the stick behave like the d-pad
	// (deadzone setting still applies — tells the snap how far the
	// stick must travel before triggering). Order matches
	// R.array.stick_mode_options.
	public static final int STICK_MODE_ANALOG = 0;
	public static final int STICK_MODE_DIGITAL = 1;
	private static final String STICK_MODE = "stick_mode";
	private static final int STICK_MODE_DEFAULT = STICK_MODE_ANALOG;

	// Stick layout: which physical sticks drive which physics axes.
	// SINGLE — left stick does both throttle (Y) and lean (X); right
	//   stick is unused. Original behavior, default.
	// DUAL_LEAN_LEFT — left stick X = lean, right stick Y = throttle.
	//   Most racing games put steering on the left thumb.
	// DUAL_THROTTLE_LEFT — left stick Y = throttle, right stick X = lean.
	//   Some prefer throttle/brake on the dominant thumb.
	// In dual modes each stick is treated as 1D (its other axis is
	// ignored entirely, including for deadzone) so drift on the unused
	// axis can never leak into physics. Order matches
	// R.array.stick_layout_options.
	public static final int STICK_LAYOUT_SINGLE = 0;
	public static final int STICK_LAYOUT_DUAL_LEAN_LEFT = 1;
	public static final int STICK_LAYOUT_DUAL_THROTTLE_LEFT = 2;
	private static final String STICK_LAYOUT = "stick_layout";
	private static final int STICK_LAYOUT_DEFAULT = STICK_LAYOUT_SINGLE;

	// Stick invert: flip the SIGN of the affected stick's outputs.
	// Applied per source stick — composes with whatever physics axis the
	// stick is mapped to by the layout. e.g. in DUAL_LEAN_LEFT (L=lean,
	// R=throttle), "Invert R" flips throttle. In DUAL_THROTTLE_LEFT
	// (L=throttle, R=lean), "Invert R" flips lean. In SINGLE the left
	// stick is the only one, so LEFT and ALL behave identically and RIGHT
	// is a no-op. Order matches R.array.stick_lr_options.
	public static final int STICK_INVERT_NONE = 0;
	public static final int STICK_INVERT_LEFT = 1;
	public static final int STICK_INVERT_RIGHT = 2;
	public static final int STICK_INVERT_ALL = 3;
	private static final String STICK_INVERT = "stick_invert";
	private static final int STICK_INVERT_DEFAULT = STICK_INVERT_NONE;

	// Stick axis flip: swap WHICH PHYSICAL AXIS on each stick drives the
	// assigned physics quantity. In SINGLE the left stick's X/Y are
	// swapped (so stick Y becomes lean and stick X becomes throttle). In
	// dual modes each flipped stick switches between its two axes for the
	// physics quantity the layout assigns to it (the layout still decides
	// which stick drives lean vs throttle; flip only decides X vs Y on
	// that stick). RIGHT is a no-op in SINGLE. Order matches
	// R.array.stick_lr_options.
	public static final int STICK_AXIS_FLIP_NONE = 0;
	public static final int STICK_AXIS_FLIP_LEFT = 1;
	public static final int STICK_AXIS_FLIP_RIGHT = 2;
	public static final int STICK_AXIS_FLIP_BOTH = 3;
	private static final String STICK_AXIS_FLIP = "stick_axis_flip";
	private static final int STICK_AXIS_FLIP_DEFAULT = STICK_AXIS_FLIP_NONE;

	// Swap the A and B controller buttons. Default mapping: in menu A =
	// select (FIRE), B = back; in game A = accelerate, B = brake.
	// With this on: A and B trade roles in both contexts (menu A = back,
	// B = select; game A = brake, B = accelerate). Useful for users on
	// pads with Nintendo-style A/B placement, or who simply prefer the
	// alternate ergonomics. Affects only KEYCODE_BUTTON_A / _BUTTON_B
	// dispatch in ControllerInputHandler — the d-pad, sticks, START and
	// MENU paths are unchanged.
	private static final String BUTTON_AB_SWAP_ENABLED = "button_ab_swap_enabled";
	private static final boolean BUTTON_AB_SWAP_ENABLED_DEFAULT = false;

	// Hide the status bar while the activity is in the foreground. With
	// BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE the user can swipe from the
	// top to peek the bar back. Default off — preserve the original
	// "system bars visible" feel; users who want a more fullscreen game
	// can opt in. Independent of {@link #IMMERSIVE_NAV_ENABLED}: each
	// bar can be hidden separately.
	private static final String IMMERSIVE_MODE_ENABLED = "immersive_mode_enabled";
	private static final boolean IMMERSIVE_MODE_ENABLED_DEFAULT = false;

	// Hide the navigation bar (the gesture/3-button bar at the bottom)
	// while the activity is in the foreground. Same swipe-to-peek
	// behaviour as IMMERSIVE_MODE_ENABLED — the BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
	// is a window-level setting that applies to whichever bars are
	// hidden, so a single applyImmersiveMode() pass can toggle each
	// bar independently. Default off — keep the original look unless
	// the user opts in. Useful for gesture-nav devices where the
	// bottom inset eats screen real estate the game could otherwise
	// use.
	private static final String IMMERSIVE_NAV_ENABLED = "immersive_nav_enabled";
	private static final boolean IMMERSIVE_NAV_ENABLED_DEFAULT = false;

	// AMOLED-friendly dark mode: flips the menu chrome and in-game sky to
	// pitch black (0x000000) and the menu text to white. The track and
	// sprites still draw their own colors — only the empty space behind them
	// goes black. Single source of truth: getMenuBgColor / getMenuFgColor /
	// getMenuItemColorStateList. Default off — keep the original light look.
	private static final String DARK_MODE_ENABLED = "dark_mode_enabled";
	private static final boolean DARK_MODE_ENABLED_DEFAULT = false;

	// Diagnostic overlay: top-right corner, in the same font/color as the
	// in-game timer. Format "X.Xms / Yfps" computed from a 30-frame ring
	// buffer of onDraw inter-arrival times. Default off — the original
	// game has no such overlay; users opt in.
	private static final String FPS_OVERLAY_ENABLED = "fps_overlay_enabled";
	private static final boolean FPS_OVERLAY_ENABLED_DEFAULT = false;
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
		setTrackColorMode(TRACK_COLOR_DEFAULT);
		setDriverSpriteEnabled(DRIVER_SPRITE_ENABLED_DEFAULT);
		setBikeSpriteEnabled(BIKE_SPRITE_ENABLED_DEFAULT);
		setLookAheadEnabled(LOOK_AHEAD_ENABLED_DEFAULT);
		setVibrateOnTouchEnabled(VIBRATE_ENABLED_DEFAULT);
		setKeyboardInMenuEnabled(KEYBOARD_IN_MENU_ENABLED_DEFAULT);
		setKeypadLandscapeSide(KEYPAD_LANDSCAPE_SIDE_DEFAULT);
		setControllerAutoHideTimeoutSec(CONTROLLER_AUTOHIDE_TIMEOUT_SEC_DEFAULT);
		setStickDeadzonePct(STICK_DEADZONE_PCT_DEFAULT);
		setStickMode(STICK_MODE_DEFAULT);
		setStickLayout(STICK_LAYOUT_DEFAULT);
		setStickInvert(STICK_INVERT_DEFAULT);
		setStickAxisFlip(STICK_AXIS_FLIP_DEFAULT);
		setButtonAbSwapEnabled(BUTTON_AB_SWAP_ENABLED_DEFAULT);
		setImmersiveModeEnabled(IMMERSIVE_MODE_ENABLED_DEFAULT);
		setImmersiveNavEnabled(IMMERSIVE_NAV_ENABLED_DEFAULT);
		setDarkModeEnabled(DARK_MODE_ENABLED_DEFAULT);
		setFpsOverlayEnabled(FPS_OVERLAY_ENABLED_DEFAULT);
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

	public static int getTrackColorMode() {
		int mode = preferences.getInt(TRACK_COLOR, TRACK_COLOR_DEFAULT);
		if (mode < TRACK_COLOR_ORIGINAL || mode > TRACK_COLOR_BW)
			return TRACK_COLOR_DEFAULT;
		return mode;
	}

	public static void setTrackColorMode(int mode) {
		setInt(TRACK_COLOR, mode);
	}

	// 0xAARRGGBB color for the FOREGROUND line — drawn between actual
	// ground points (the lower / front line visually). Brighter of the
	// two preset colors. 0 for ORIGINAL (callers must special-case ORIGINAL
	// — it falls through to the legacy single-color render path).
	public static int getTrackForegroundArgb(int mode) {
		switch (mode) {
			case TRACK_COLOR_GREEN:  return 0xff00AA00;
			case TRACK_COLOR_CYAN:   return 0xff94E6F6;
			case TRACK_COLOR_RED:    return 0xffF69494;
			case TRACK_COLOR_YELLOW: return 0xffF6F694;
			case TRACK_COLOR_LIME:   return 0xff94F694;
			case TRACK_COLOR_BLUE:   return 0xff9494F6;
			case TRACK_COLOR_GRAY:   return 0xffC4C4C4;
			// BW: FG must be lighter than BG (rule: BG always darker). In dark
			// mode FG=white on the lower/ground line is the prominent stroke
			// against the black canvas; in light mode FG=mid-gray recedes
			// against white while the BG=black line on the raised/upper
			// position is the prominent one.
			case TRACK_COLOR_BW:     return isDarkModeEnabled() ? 0xffFFFFFF : 0xff666666;
			default: return 0;
		}
	}

	// 0xAARRGGBB color for the BACKGROUND line — drawn between the raised
	// perspective-projected points (the upper / back line visually); also
	// the raised-end of the across gradient. Darker of the two preset
	// colors. 0 for ORIGINAL.
	public static int getTrackBackgroundArgb(int mode) {
		switch (mode) {
			case TRACK_COLOR_GREEN:  return 0xff006900;
			case TRACK_COLOR_CYAN:   return 0xff5E8C96;
			case TRACK_COLOR_RED:    return 0xff965E5E;
			case TRACK_COLOR_YELLOW: return 0xff96965E;
			case TRACK_COLOR_LIME:   return 0xff5E965E;
			case TRACK_COLOR_BLUE:   return 0xff5E5E96;
			case TRACK_COLOR_GRAY:   return 0xff7A7A7A;
			case TRACK_COLOR_BW:     return isDarkModeEnabled() ? 0xff999999 : 0xff000000;
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

	public static int getStickDeadzonePct() {
		return preferences.getInt(STICK_DEADZONE_PCT, STICK_DEADZONE_PCT_DEFAULT);
	}

	public static void setStickDeadzonePct(int pct) {
		setInt(STICK_DEADZONE_PCT, pct);
	}

	public static int getStickMode() {
		return preferences.getInt(STICK_MODE, STICK_MODE_DEFAULT);
	}

	public static void setStickMode(int mode) {
		setInt(STICK_MODE, mode);
	}

	public static int getStickLayout() {
		return preferences.getInt(STICK_LAYOUT, STICK_LAYOUT_DEFAULT);
	}

	public static void setStickLayout(int layout) {
		setInt(STICK_LAYOUT, layout);
	}

	public static int getStickInvert() {
		return preferences.getInt(STICK_INVERT, STICK_INVERT_DEFAULT);
	}

	public static void setStickInvert(int invert) {
		setInt(STICK_INVERT, invert);
	}

	public static int getStickAxisFlip() {
		return preferences.getInt(STICK_AXIS_FLIP, STICK_AXIS_FLIP_DEFAULT);
	}

	public static void setStickAxisFlip(int flip) {
		setInt(STICK_AXIS_FLIP, flip);
	}

	public static boolean isButtonAbSwapEnabled() {
		return preferences.getBoolean(BUTTON_AB_SWAP_ENABLED, BUTTON_AB_SWAP_ENABLED_DEFAULT);
	}

	public static void setButtonAbSwapEnabled(boolean enabled) {
		setBoolean(BUTTON_AB_SWAP_ENABLED, enabled);
	}

	public static boolean isImmersiveModeEnabled() {
		return preferences.getBoolean(IMMERSIVE_MODE_ENABLED, IMMERSIVE_MODE_ENABLED_DEFAULT);
	}

	public static void setImmersiveModeEnabled(boolean enabled) {
		setBoolean(IMMERSIVE_MODE_ENABLED, enabled);
	}

	public static boolean isImmersiveNavEnabled() {
		return preferences.getBoolean(IMMERSIVE_NAV_ENABLED, IMMERSIVE_NAV_ENABLED_DEFAULT);
	}

	public static void setImmersiveNavEnabled(boolean enabled) {
		setBoolean(IMMERSIVE_NAV_ENABLED, enabled);
	}

	public static boolean isDarkModeEnabled() {
		return preferences.getBoolean(DARK_MODE_ENABLED, DARK_MODE_ENABLED_DEFAULT);
	}

	public static void setDarkModeEnabled(boolean enabled) {
		setBoolean(DARK_MODE_ENABLED, enabled);
	}

	public static boolean isFpsOverlayEnabled() {
		return preferences.getBoolean(FPS_OVERLAY_ENABLED, FPS_OVERLAY_ENABLED_DEFAULT);
	}

	public static void setFpsOverlayEnabled(boolean enabled) {
		setBoolean(FPS_OVERLAY_ENABLED, enabled);
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
