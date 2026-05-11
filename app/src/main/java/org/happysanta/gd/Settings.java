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
	// "shadows on/off" — we widen that to OFF / SHADOW / NEON. SHADOW is
	// the original gray fade (close = dark, far = light gray); NEON
	// re-uses the same render path but inverts the brightness ramp,
	// fading *out* a glow under the bike (close = bright neon base, far
	// = #363636). The actual neon hue is decoupled from this mode and
	// lives in NEON_COLOR below. Default stays "Shadow" so existing
	// players see no visible change.
	public static final int SHADOW_MODE_OFF = 0;
	public static final int SHADOW_MODE_SHADOW = 1;
	public static final int SHADOW_MODE_NEON = 2;
	private static final String SHADOW_MODE = "shadow_mode";
	private static final int SHADOW_MODE_DEFAULT = SHADOW_MODE_SHADOW;

	// One-time migration when shadow_mode was collapsed from 8 entries
	// (OFF / SHADOW / NEON_YELLOW..GREEN) to 3 (OFF / SHADOW / NEON) and
	// the neon hue was split out into its own NEON_COLOR pref. Old values
	// 2..7 (the per-color neons) map to SHADOW_MODE_NEON + the
	// corresponding NEON_COLOR_*. Safe on fresh install (no SHADOW_MODE
	// key → reads default 1 = SHADOW, hits the "leave alone" branch).
	private static final String SHADOW_MODE_MIGRATED_V2 = "shadow_mode_migrated_v2";

	// Neon hue preset for SHADOW_MODE_NEON. Six built-in colors plus three
	// user-editable Custom slots whose RGB channels live in their own
	// prefs (see NEON_CUSTOM_*_R/G/B). Custom slots seed from
	// NEON_COLOR_YELLOW on first read. Independent of SHADOW_MODE — the
	// hue persists across switching shadow modes off and back on.
	public static final int NEON_COLOR_YELLOW = 0;
	public static final int NEON_COLOR_RED = 1;
	public static final int NEON_COLOR_PURPLE = 2;
	public static final int NEON_COLOR_BLUE = 3;
	public static final int NEON_COLOR_CYAN = 4;
	public static final int NEON_COLOR_GREEN = 5;
	public static final int NEON_COLOR_CUSTOM_1 = 6;
	public static final int NEON_COLOR_CUSTOM_2 = 7;
	public static final int NEON_COLOR_CUSTOM_3 = 8;
	private static final String NEON_COLOR = "neon_color";
	private static final int NEON_COLOR_DEFAULT = NEON_COLOR_YELLOW;
	// Track color preset. Picks the FG/BG color pair fed to the uniform
	// 3-line track render; how the pair is *applied* (single-color vs
	// gradient vs inverted) is controlled by MAP_COLOR_GRADIENT below.
	//   - FG (foreground / lower) = the actual ground contour line, drawn
	//     between adjacent ground points. Brighter of the two colors.
	//   - BG (background / upper) = the raised perspective projection line,
	//     drawn between the projected points; also the raised end of the
	//     across tick gradient. Per design rule BG luma < FG luma.
	// FG/BG color values are HSL-matched against a cyan reference (FG ≈
	// S=85% L=77%, BG ≈ S=23% L=48%). Black/White is theme-aware via
	// isDarkModeEnabled. Original is the two upstream greens, brighter as
	// FG: FG = #00FF00 (upstream's perspective-off bright green, picked so
	// the default GRADIENT_OFF renders bright single-color green and
	// matches upstream's flat track); BG = #00AA00 (upstream's perspective-
	// on dim green). GRADIENT_ON gives a bright→dim ground-to-raised
	// gradient; GRADIENT_INVERTED puts dim on the ground and bright on the
	// raised projection — and dims the perspective-off line too (it uses
	// BG when inverted).
	public static final int TRACK_COLOR_ORIGINAL = 0;
	public static final int TRACK_COLOR_CYAN = 1;
	public static final int TRACK_COLOR_RED = 2;
	public static final int TRACK_COLOR_YELLOW = 3;
	public static final int TRACK_COLOR_LIME = 4;
	public static final int TRACK_COLOR_BLUE = 5;
	public static final int TRACK_COLOR_GRAY = 6;
	public static final int TRACK_COLOR_BW = 7;
	// Three user-editable Custom slots appended after the built-ins. Each
	// slot persists its own six channels (FG R/G/B + BG R/G/B); see
	// TRACK_CUSTOM_*_FG_R / _BG_R below. Slots seed from
	// TRACK_COLOR_ORIGINAL on first read.
	public static final int TRACK_COLOR_CUSTOM_1 = 8;
	public static final int TRACK_COLOR_CUSTOM_2 = 9;
	public static final int TRACK_COLOR_CUSTOM_3 = 10;
	private static final String TRACK_COLOR = "track_color";
	private static final int TRACK_COLOR_DEFAULT = TRACK_COLOR_ORIGINAL;

	// Custom-channel kind selector for getTrackCustomChannel /
	// setTrackCustomChannel / trackCustomChannelKey. Three independent
	// color triples per Custom slot: FG (ground contour), BG (raised
	// projection), FILL (the optional third color used when
	// MAP_FILL_MODE_THIRD is active). Kept as constants rather than an
	// enum to stay in one ABI with the int channel argument.
	public static final int TRACK_CUSTOM_KIND_FG   = 0;
	public static final int TRACK_CUSTOM_KIND_BG   = 1;
	public static final int TRACK_CUSTOM_KIND_FILL = 2;

	// Per-channel storage for the three Custom track presets. Each slot
	// holds nine int prefs (FG R/G/B, BG R/G/B, and FILL R/G/B). Sentinel
	// -1 means "uninitialized" — getTrackCustomChannel seeds the slot
	// from TRACK_COLOR_ORIGINAL on the first read.
	private static final String[] TRACK_CUSTOM_FG_R = {
			"track_custom_1_fg_r", "track_custom_2_fg_r", "track_custom_3_fg_r"};
	private static final String[] TRACK_CUSTOM_FG_G = {
			"track_custom_1_fg_g", "track_custom_2_fg_g", "track_custom_3_fg_g"};
	private static final String[] TRACK_CUSTOM_FG_B = {
			"track_custom_1_fg_b", "track_custom_2_fg_b", "track_custom_3_fg_b"};
	private static final String[] TRACK_CUSTOM_BG_R = {
			"track_custom_1_bg_r", "track_custom_2_bg_r", "track_custom_3_bg_r"};
	private static final String[] TRACK_CUSTOM_BG_G = {
			"track_custom_1_bg_g", "track_custom_2_bg_g", "track_custom_3_bg_g"};
	private static final String[] TRACK_CUSTOM_BG_B = {
			"track_custom_1_bg_b", "track_custom_2_bg_b", "track_custom_3_bg_b"};
	private static final String[] TRACK_CUSTOM_FILL_R = {
			"track_custom_1_fill_r", "track_custom_2_fill_r", "track_custom_3_fill_r"};
	private static final String[] TRACK_CUSTOM_FILL_G = {
			"track_custom_1_fill_g", "track_custom_2_fill_g", "track_custom_3_fill_g"};
	private static final String[] TRACK_CUSTOM_FILL_B = {
			"track_custom_1_fill_b", "track_custom_2_fill_b", "track_custom_3_fill_b"};

	// Per-channel storage for the three Custom neon presets. Each slot
	// holds three int prefs (R/G/B). Sentinel -1 means uninitialized —
	// getNeonCustomChannel seeds the slot from NEON_COLOR_YELLOW on the
	// first read.
	private static final String[] NEON_CUSTOM_R = {
			"neon_custom_1_r", "neon_custom_2_r", "neon_custom_3_r"};
	private static final String[] NEON_CUSTOM_G = {
			"neon_custom_1_g", "neon_custom_2_g", "neon_custom_3_g"};
	private static final String[] NEON_CUSTOM_B = {
			"neon_custom_1_b", "neon_custom_2_b", "neon_custom_3_b"};

	// Map color gradient: how the FG/BG pair combines on the uniform 3-line
	// track render. OFF ignores BG entirely — every line of the render
	// (ground contour, raised projection, across tick) draws in FG; the
	// across "gradient" degenerates to a solid FG tick. ON keeps the
	// canonical 3-line render (ground=FG, raised=BG, across gradients
	// FG→BG). INVERTED swaps FG/BG so the brighter line sits on the raised
	// projection. Default OFF — single-color render across all presets.
	public static final int MAP_COLOR_GRADIENT_OFF = 0;
	public static final int MAP_COLOR_GRADIENT_ON = 1;
	public static final int MAP_COLOR_GRADIENT_INVERTED = 2;
	private static final String MAP_COLOR_GRADIENT = "map_color_gradient";
	private static final int MAP_COLOR_GRADIENT_DEFAULT = MAP_COLOR_GRADIENT_OFF;

	// Perspective-mode fill between the ground contour and the raised
	// projection. OFF preserves the upstream wireframe look (no fill —
	// sky shows through). The three solid modes paint the area between
	// the two curves in a single color: FG matches the ground line,
	// BG matches the raised line, THIRD pulls a curated palette entry
	// from getTrackFillArgb that lives between FG and BG. GRADIENT
	// fills with a strip-fill (N=6 trapezoidal bands) ramping from
	// fgColor at the ground edge to bgColor at the raised edge —
	// same interpolation direction the across-tick gradient already
	// uses, computed via interpArgb (no shader, no per-frame
	// allocations). Default OFF — visual parity with the wireframe.
	public static final int MAP_FILL_MODE_OFF      = 0;
	public static final int MAP_FILL_MODE_FG       = 1;
	public static final int MAP_FILL_MODE_BG       = 2;
	public static final int MAP_FILL_MODE_THIRD    = 3;
	public static final int MAP_FILL_MODE_GRADIENT = 4;
	private static final String MAP_FILL_MODE = "map_fill_mode";
	private static final int MAP_FILL_MODE_DEFAULT = MAP_FILL_MODE_OFF;

	// Independent toggle for the across-tick ribs (the 6 short
	// line segments per track segment running from the ground point
	// up to the raised projection). Separated from MAP_FILL_MODE so
	// users can mix freely — e.g. solid fill with ticks overlay, or
	// gradient fill with ticks off for a cleaner look. Default ON
	// preserves upstream visual output.
	private static final String MAP_ACROSS_TICKS_ENABLED = "map_across_ticks_enabled";
	private static final boolean MAP_ACROSS_TICKS_ENABLED_DEFAULT = true;

	// Subdivision count N for the perspective-mode gradient render:
	// strip-fill bands AND across-tick ribs share this so the two
	// gradients stay visually aligned (a strip's color sample at
	// (2n+1)/2N is the same as the tick's color sample at the matching
	// position). Lower N = chunkier banding / fewer line strokes per
	// tick; higher N = smoother gradient at slightly more per-frame
	// rasterization cost. Default 6 preserves the value the strip-fill
	// and tick render shipped with originally.
	public static final int[] GRADIENT_STEPS_OPTIONS = { 2, 3, 4, 6, 8, 12 };
	private static final String GRADIENT_STEPS = "gradient_steps";
	private static final int GRADIENT_STEPS_DEFAULT = 6;

	// One-time migration when the legacy "Green" preset was removed.
	// Pre-migration encoding: ORIGINAL=0, GREEN=1, CYAN=2, RED=3, YELLOW=4,
	// LIME=5, BLUE=6, GRAY=7, BW=8. New encoding drops GREEN and shifts
	// everything ≥CYAN down by one. Users who had GREEN selected land on
	// ORIGINAL + GRADIENT_ON, which keeps them on a green-themed two-tone
	// gradient — different shade pair than the old Green preset, but the
	// closest visual continuation given Original's reshuffled FG/BG.
	private static final String TRACK_COLOR_MIGRATED_V2 = "track_color_migrated_v2";

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

	private static final String NAME = "name";
	public static final String NAME_DEFAULT = "AAA";
	public static final byte[] NAME_CHARS_DEFALUT = new byte[]{65, 65, 65};

	private static final String LEVELS_SORT = "level_sort"; // in download list
	private static final int LEVELS_SORT_DEFAULT = 0;

	private static SharedPreferences preferences;

	static {
		preferences = getGDActivity().getSharedPreferences("GDSettings", Context.MODE_PRIVATE);
		migrateTrackColorIfNeeded();
		migrateShadowModeIfNeeded();
	}

	// One-shot, idempotent. Translates the pre-removal-of-Green pref into
	// the new compact encoding. Safe on a fresh install (no TRACK_COLOR key
	// → reads default 0, hits the "leave alone" branch, just sets the flag).
	private static void migrateTrackColorIfNeeded() {
		if (preferences.getBoolean(TRACK_COLOR_MIGRATED_V2, false))
			return;
		int legacyGreen = 1;
		int legacyMaxBw = 8;
		int old = preferences.getInt(TRACK_COLOR, TRACK_COLOR_DEFAULT);
		SharedPreferences.Editor editor = preferences.edit();
		if (old == legacyGreen) {
			// Land on the closest green-themed visual: ORIGINAL + GRADIENT_ON
			// is a two-tone green gradient, not the same shade pair the old
			// Green preset used but the closest match the new Original pair
			// allows.
			editor.putInt(TRACK_COLOR, TRACK_COLOR_ORIGINAL);
			editor.putInt(MAP_COLOR_GRADIENT, MAP_COLOR_GRADIENT_ON);
		} else if (old > legacyGreen && old <= legacyMaxBw) {
			editor.putInt(TRACK_COLOR, old - 1);
		}
		editor.putBoolean(TRACK_COLOR_MIGRATED_V2, true);
		editorApply(editor);
	}

	// One-shot, idempotent. Translates the pre-collapse shadow_mode pref
	// (which folded an on/off plus a neon hue choice into one of 8 values)
	// into the new {OFF, SHADOW, NEON} encoding plus an independent
	// NEON_COLOR. Legacy values 2..7 = NEON_YELLOW..NEON_GREEN, now map to
	// SHADOW_MODE_NEON + NEON_COLOR_YELLOW..GREEN (old - 2).
	private static void migrateShadowModeIfNeeded() {
		if (preferences.getBoolean(SHADOW_MODE_MIGRATED_V2, false))
			return;
		SharedPreferences.Editor editor = preferences.edit();
		int legacyMaxNeon = 7;
		int legacyFirstNeon = 2;
		int old = preferences.getInt(SHADOW_MODE, SHADOW_MODE_DEFAULT);
		if (old >= legacyFirstNeon && old <= legacyMaxNeon) {
			editor.putInt(SHADOW_MODE, SHADOW_MODE_NEON);
			editor.putInt(NEON_COLOR, old - legacyFirstNeon);
		}
		editor.putBoolean(SHADOW_MODE_MIGRATED_V2, true);
		editorApply(editor);
	}

	public static void resetAll() {
		setPerspectiveEnabled(PERSPECTIVE_ENABLED_DEFAULT);
		setShadowMode(SHADOW_MODE_DEFAULT);
		setNeonColor(NEON_COLOR_DEFAULT);
		setTrackColorMode(TRACK_COLOR_DEFAULT);
		setMapColorGradient(MAP_COLOR_GRADIENT_DEFAULT);
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
		if (mode < SHADOW_MODE_OFF || mode > SHADOW_MODE_NEON)
			return SHADOW_MODE_DEFAULT;
		return mode;
	}

	public static void setShadowMode(int mode) {
		setInt(SHADOW_MODE, mode);
	}

	public static int getNeonColor() {
		int color = preferences.getInt(NEON_COLOR, NEON_COLOR_DEFAULT);
		if (color < NEON_COLOR_YELLOW || color > NEON_COLOR_CUSTOM_3)
			return NEON_COLOR_DEFAULT;
		return color;
	}

	public static void setNeonColor(int color) {
		setInt(NEON_COLOR, color);
	}

	// 0xAARRGGBB base color for a NEON_COLOR_* preset. Built-in presets
	// return their fixed hue; Custom slots assemble ARGB from their stored
	// R/G/B channels (which seed from NEON_COLOR_YELLOW on first read).
	// Centralized so the renderer and any settings UI agree on the
	// palette. Returns 0 for unknown values.
	public static int getShadowNeonBaseColor(int neonColor) {
		switch (neonColor) {
			case NEON_COLOR_YELLOW: return 0xffFDD449;
			case NEON_COLOR_RED:    return 0xffFD495B;
			case NEON_COLOR_PURPLE: return 0xffE749FD;
			case NEON_COLOR_BLUE:   return 0xff494BFD;
			case NEON_COLOR_CYAN:   return 0xff49FDE8;
			case NEON_COLOR_GREEN:  return 0xff54FD49;
			case NEON_COLOR_CUSTOM_1:
			case NEON_COLOR_CUSTOM_2:
			case NEON_COLOR_CUSTOM_3: {
				int slot = neonColor - NEON_COLOR_CUSTOM_1;
				int r = getNeonCustomChannel(slot, 0);
				int g = getNeonCustomChannel(slot, 1);
				int b = getNeonCustomChannel(slot, 2);
				return 0xff000000 | (r << 16) | (g << 8) | b;
			}
			default: return 0;
		}
	}

	public static int getTrackColorMode() {
		int mode = preferences.getInt(TRACK_COLOR, TRACK_COLOR_DEFAULT);
		if (mode < TRACK_COLOR_ORIGINAL || mode > TRACK_COLOR_CUSTOM_3)
			return TRACK_COLOR_DEFAULT;
		return mode;
	}

	public static void setTrackColorMode(int mode) {
		setInt(TRACK_COLOR, mode);
	}

	public static int getMapColorGradient() {
		int mode = preferences.getInt(MAP_COLOR_GRADIENT, MAP_COLOR_GRADIENT_DEFAULT);
		// Valid range is OFF..INVERTED (0..2). The pre-existing check
		// compared against OFF as if it were the upper bound, which made
		// every stored value other than OFF clamp back to the default —
		// the visible symptom was Gradient / Reversed appearing to revert
		// to Solid every time the menu re-read the setting.
		if (mode < MAP_COLOR_GRADIENT_OFF || mode > MAP_COLOR_GRADIENT_INVERTED)
			return MAP_COLOR_GRADIENT_DEFAULT;
		return mode;
	}

	public static void setMapColorGradient(int mode) {
		setInt(MAP_COLOR_GRADIENT, mode);
	}

	public static int getMapFillMode() {
		int mode = preferences.getInt(MAP_FILL_MODE, MAP_FILL_MODE_DEFAULT);
		// Valid range OFF..GRADIENT (0..4). Defensive clamp — same
		// shape as getMapColorGradient above.
		if (mode < MAP_FILL_MODE_OFF || mode > MAP_FILL_MODE_GRADIENT)
			return MAP_FILL_MODE_DEFAULT;
		return mode;
	}

	public static void setMapFillMode(int mode) {
		setInt(MAP_FILL_MODE, mode);
	}

	public static boolean isAcrossTicksEnabled() {
		return preferences.getBoolean(MAP_ACROSS_TICKS_ENABLED, MAP_ACROSS_TICKS_ENABLED_DEFAULT);
	}

	public static void setAcrossTicksEnabled(boolean enabled) {
		setBoolean(MAP_ACROSS_TICKS_ENABLED, enabled);
	}

	public static int getGradientSteps() {
		int n = preferences.getInt(GRADIENT_STEPS, GRADIENT_STEPS_DEFAULT);
		// Validate against the allowed option list rather than a range —
		// arbitrary values would work but would let stale prefs leak in
		// awkward subdivisions like 5 or 7 that the menu can't represent.
		for (int allowed : GRADIENT_STEPS_OPTIONS)
			if (n == allowed) return n;
		return GRADIENT_STEPS_DEFAULT;
	}

	public static void setGradientSteps(int n) {
		setInt(GRADIENT_STEPS, n);
	}

	// 0xAARRGGBB color for the FOREGROUND line — drawn between actual
	// ground points (the lower / front line visually). Brighter of the two
	// preset colors. Original returns upstream's perspective-off bright
	// green (#00FF00) so the default Off render matches it.
	public static int getTrackForegroundArgb(int mode) {
		switch (mode) {
			case TRACK_COLOR_ORIGINAL: return 0xff00FF00;
			case TRACK_COLOR_CYAN:    return 0xff94E6F6;
			case TRACK_COLOR_RED:     return 0xffF69494;
			case TRACK_COLOR_YELLOW:  return 0xffF6F694;
			case TRACK_COLOR_LIME:    return 0xff94F694;
			case TRACK_COLOR_BLUE:    return 0xff9494F6;
			case TRACK_COLOR_GRAY:    return 0xffC4C4C4;
			// BW: FG must be lighter than BG (rule: BG always darker). In dark
			// mode FG=white on the lower/ground line is the prominent stroke
			// against the black canvas; in light mode FG=mid-gray recedes
			// against white while the BG=black line on the raised/upper
			// position is the prominent one.
			case TRACK_COLOR_BW:      return isDarkModeEnabled() ? 0xffFFFFFF : 0xff666666;
			case TRACK_COLOR_CUSTOM_1:
			case TRACK_COLOR_CUSTOM_2:
			case TRACK_COLOR_CUSTOM_3: {
				int slot = mode - TRACK_COLOR_CUSTOM_1;
				int r = getTrackCustomChannel(slot, TRACK_CUSTOM_KIND_FG, 0);
				int g = getTrackCustomChannel(slot, TRACK_CUSTOM_KIND_FG, 1);
				int b = getTrackCustomChannel(slot, TRACK_CUSTOM_KIND_FG, 2);
				return 0xff000000 | (r << 16) | (g << 8) | b;
			}
			default: return 0;
		}
	}

	// 0xAARRGGBB color for the BACKGROUND line — drawn between the raised
	// perspective-projected points (the upper / back line visually); also
	// the raised-end of the across gradient. Darker of the two preset
	// colors. Original returns upstream's perspective-on dim green
	// (#00AA00) — paired with the bright FG it gives a gradient between
	// the two upstream Original greens under GRADIENT_ON.
	public static int getTrackBackgroundArgb(int mode) {
		switch (mode) {
			case TRACK_COLOR_ORIGINAL: return 0xff00AA00;
			case TRACK_COLOR_CYAN:    return 0xff5E8C96;
			case TRACK_COLOR_RED:     return 0xff965E5E;
			case TRACK_COLOR_YELLOW:  return 0xff96965E;
			case TRACK_COLOR_LIME:    return 0xff5E965E;
			case TRACK_COLOR_BLUE:    return 0xff5E5E96;
			case TRACK_COLOR_GRAY:    return 0xff7A7A7A;
			case TRACK_COLOR_BW:      return isDarkModeEnabled() ? 0xff999999 : 0xff000000;
			case TRACK_COLOR_CUSTOM_1:
			case TRACK_COLOR_CUSTOM_2:
			case TRACK_COLOR_CUSTOM_3: {
				int slot = mode - TRACK_COLOR_CUSTOM_1;
				int r = getTrackCustomChannel(slot, TRACK_CUSTOM_KIND_BG, 0);
				int g = getTrackCustomChannel(slot, TRACK_CUSTOM_KIND_BG, 1);
				int b = getTrackCustomChannel(slot, TRACK_CUSTOM_KIND_BG, 2);
				return 0xff000000 | (r << 16) | (g << 8) | b;
			}
			default: return 0;
		}
	}

	// 0xAARRGGBB third color for solid MAP_FILL_MODE_THIRD — the fill
	// painted between the FG and BG lines in perspective mode. Curated
	// per built-in preset to land visually between FG and BG so both
	// outline lines stay readable on top of it; for Custom 1-3 it pulls
	// from a third R/G/B pref triple alongside the FG/BG triples.
	// Default for an uninitialized Custom FILL channel seeds from
	// Original's mid-green (#007F00) — see getTrackCustomChannel.
	public static int getTrackFillArgb(int mode) {
		switch (mode) {
			case TRACK_COLOR_ORIGINAL: return 0xff007F00;
			case TRACK_COLOR_CYAN:    return 0xff79B9C6;
			case TRACK_COLOR_RED:     return 0xffC67979;
			case TRACK_COLOR_YELLOW:  return 0xffC6C679;
			case TRACK_COLOR_LIME:    return 0xff79C679;
			case TRACK_COLOR_BLUE:    return 0xff7979C6;
			case TRACK_COLOR_GRAY:    return 0xff9F9F9F;
			// BW must satisfy: contrast with both the FG line (lighter
			// of the two) and the BG line (darker). In dark mode FG is
			// white and BG is light gray — pick a mid-gray that sits
			// between them. In light mode FG is mid-gray and BG is
			// black — pick a darker gray nearer black so the FG line
			// keeps its prominence.
			case TRACK_COLOR_BW:      return isDarkModeEnabled() ? 0xff666666 : 0xff333333;
			case TRACK_COLOR_CUSTOM_1:
			case TRACK_COLOR_CUSTOM_2:
			case TRACK_COLOR_CUSTOM_3: {
				int slot = mode - TRACK_COLOR_CUSTOM_1;
				int r = getTrackCustomChannel(slot, TRACK_CUSTOM_KIND_FILL, 0);
				int g = getTrackCustomChannel(slot, TRACK_CUSTOM_KIND_FILL, 1);
				int b = getTrackCustomChannel(slot, TRACK_CUSTOM_KIND_FILL, 2);
				return 0xff000000 | (r << 16) | (g << 8) | b;
			}
			default: return 0;
		}
	}

	// Read one R/G/B channel (channel 0=R, 1=G, 2=B) of a track Custom
	// slot's FG, BG, or FILL color (kind = TRACK_CUSTOM_KIND_*). Returns
	// 0..255. Sentinel -1 in the pref means "uninitialized" — fall
	// through to the seed value pulled from TRACK_COLOR_ORIGINAL so
	// Custom slots open with a sensible not-zero-black appearance and
	// the user can edit from there.
	public static int getTrackCustomChannel(int slot, int kind, int channel) {
		String key = trackCustomChannelKey(slot, kind, channel);
		int v = preferences.getInt(key, -1);
		if (v < 0 || v > 255) {
			int seed;
			switch (kind) {
				case TRACK_CUSTOM_KIND_BG:   seed = getTrackBackgroundArgb(TRACK_COLOR_ORIGINAL); break;
				case TRACK_CUSTOM_KIND_FILL: seed = getTrackFillArgb(TRACK_COLOR_ORIGINAL);       break;
				default:                     seed = getTrackForegroundArgb(TRACK_COLOR_ORIGINAL); break;
			}
			return (seed >> (16 - channel * 8)) & 0xff;
		}
		return v;
	}

	public static void setTrackCustomChannel(int slot, int kind, int channel, int value) {
		if (value < 0) value = 0;
		if (value > 255) value = 255;
		setInt(trackCustomChannelKey(slot, kind, channel), value);
	}

	private static String trackCustomChannelKey(int slot, int kind, int channel) {
		String[] arr;
		switch (kind) {
			case TRACK_CUSTOM_KIND_BG:
				arr = (channel == 0) ? TRACK_CUSTOM_BG_R : (channel == 1) ? TRACK_CUSTOM_BG_G : TRACK_CUSTOM_BG_B;
				break;
			case TRACK_CUSTOM_KIND_FILL:
				arr = (channel == 0) ? TRACK_CUSTOM_FILL_R : (channel == 1) ? TRACK_CUSTOM_FILL_G : TRACK_CUSTOM_FILL_B;
				break;
			default: // TRACK_CUSTOM_KIND_FG
				arr = (channel == 0) ? TRACK_CUSTOM_FG_R : (channel == 1) ? TRACK_CUSTOM_FG_G : TRACK_CUSTOM_FG_B;
				break;
		}
		return arr[slot];
	}

	// 0..2 if the given track preset is one of the Custom slots, -1
	// otherwise. Used by the Colors UI to decide whether the per-channel
	// rows are editable in place (Custom) or trigger the copy-into-Custom
	// chooser (built-in).
	public static int getTrackCustomSlotIndex(int mode) {
		if (mode >= TRACK_COLOR_CUSTOM_1 && mode <= TRACK_COLOR_CUSTOM_3)
			return mode - TRACK_COLOR_CUSTOM_1;
		return -1;
	}

	// Snapshot the FG/BG/FILL channels of any track preset (built-in or
	// Custom) into the given Custom slot. Used by the copy-into-Custom
	// action: the nine channels are written so subsequent edits start
	// from the chosen preset's exact values.
	public static void copyTrackPresetIntoCustom(int srcMode, int dstSlot) {
		int fg   = getTrackForegroundArgb(srcMode);
		int bg   = getTrackBackgroundArgb(srcMode);
		int fill = getTrackFillArgb(srcMode);
		setTrackCustomChannel(dstSlot, TRACK_CUSTOM_KIND_FG,   0, (fg   >> 16) & 0xff);
		setTrackCustomChannel(dstSlot, TRACK_CUSTOM_KIND_FG,   1, (fg   >>  8) & 0xff);
		setTrackCustomChannel(dstSlot, TRACK_CUSTOM_KIND_FG,   2,  fg          & 0xff);
		setTrackCustomChannel(dstSlot, TRACK_CUSTOM_KIND_BG,   0, (bg   >> 16) & 0xff);
		setTrackCustomChannel(dstSlot, TRACK_CUSTOM_KIND_BG,   1, (bg   >>  8) & 0xff);
		setTrackCustomChannel(dstSlot, TRACK_CUSTOM_KIND_BG,   2,  bg          & 0xff);
		setTrackCustomChannel(dstSlot, TRACK_CUSTOM_KIND_FILL, 0, (fill >> 16) & 0xff);
		setTrackCustomChannel(dstSlot, TRACK_CUSTOM_KIND_FILL, 1, (fill >>  8) & 0xff);
		setTrackCustomChannel(dstSlot, TRACK_CUSTOM_KIND_FILL, 2,  fill        & 0xff);
	}

	// Read one R/G/B channel (channel 0=R, 1=G, 2=B) of a neon Custom
	// slot. Same -1-sentinel-seeds-from-default behavior as the track
	// custom channels, but seeds from NEON_COLOR_YELLOW.
	public static int getNeonCustomChannel(int slot, int channel) {
		String key = neonCustomChannelKey(slot, channel);
		int v = preferences.getInt(key, -1);
		if (v < 0 || v > 255) {
			int seed = getShadowNeonBaseColor(NEON_COLOR_YELLOW);
			return (seed >> (16 - channel * 8)) & 0xff;
		}
		return v;
	}

	public static void setNeonCustomChannel(int slot, int channel, int value) {
		if (value < 0) value = 0;
		if (value > 255) value = 255;
		setInt(neonCustomChannelKey(slot, channel), value);
	}

	private static String neonCustomChannelKey(int slot, int channel) {
		String[] arr = (channel == 0) ? NEON_CUSTOM_R : (channel == 1) ? NEON_CUSTOM_G : NEON_CUSTOM_B;
		return arr[slot];
	}

	public static int getNeonCustomSlotIndex(int neonColor) {
		if (neonColor >= NEON_COLOR_CUSTOM_1 && neonColor <= NEON_COLOR_CUSTOM_3)
			return neonColor - NEON_COLOR_CUSTOM_1;
		return -1;
	}

	public static void copyNeonPresetIntoCustom(int srcColor, int dstSlot) {
		int base = getShadowNeonBaseColor(srcColor);
		setNeonCustomChannel(dstSlot, 0, (base >> 16) & 0xff);
		setNeonCustomChannel(dstSlot, 1, (base >> 8) & 0xff);
		setNeonCustomChannel(dstSlot, 2, base & 0xff);
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
