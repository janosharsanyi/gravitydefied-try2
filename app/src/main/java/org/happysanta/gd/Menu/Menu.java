package org.happysanta.gd.Menu;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Html;
import android.text.InputType;
import android.widget.EditText;
import org.happysanta.gd.Command;
import org.happysanta.gd.DoubleCallback;
import org.happysanta.gd.GDActivity;
import org.happysanta.gd.Game.GameView;
import org.happysanta.gd.Global;
import org.happysanta.gd.Levels.InvalidTrackException;
import org.happysanta.gd.Levels.Loader;
import org.happysanta.gd.R;
import org.happysanta.gd.Settings;
import org.happysanta.gd.Storage.HighScores;
import org.happysanta.gd.Storage.Level;
import org.happysanta.gd.Storage.LevelsManager;

import java.io.File;
import java.io.UnsupportedEncodingException;

import static org.happysanta.gd.Helpers.*;
import static org.happysanta.gd.Helpers.logDebug;

public class Menu
		implements MenuHandler {

	// private final static int SETTINGS_LENGTH = 21;
	// private final static boolean ENABLE_MANAGER = true;

	public MenuScreen currentMenu;
	public Level level;
	private HighScores currentScores;
	public int selectedLeague = 0;
	public boolean m_blZ = false;
	public boolean menuDisabled = false;
	// byte[] unlockedTracks = new byte[3];
	// byte leaguesUnlockedCount = 0;
	// byte levelsUnlockedCount = 0;
	int[] selectedTrack = {
			0, 0, 0
	};
	String[][] trackNames;
	String[] leagues = new String[3];
	String[] fullLeaguesList = new String[4];
	// private byte[] settings;
	// private SaveManager saveManager;
	private Command okCommand;
	private Command backCommand;
	private MenuScreen mainMenu;
	private MenuScreen playMenu;
	private MenuScreen optionsMenu;
	private MenuScreen aboutScreen;
	private MenuScreen helpMenu;
	private MenuScreen eraseScreen;
	private MenuScreen resetScreen;
	private MenuScreen finishedMenu;
	private MenuScreen ingameScreen;
	private SimpleMenuElementNew gameMenuItem;
	// private SimpleMenuElementNew optionsMenuItem;
	// private SimpleMenuElementNew helpMenuItem;
	private OptionsMenuElement levelSelector;
	private MenuScreen levelSelectorCurrentMenu;
	private OptionsMenuElement trackSelector;
	private MenuScreen trackSelectorCurrentMenu;
	private OptionsMenuElement leagueSelector;
	private MenuScreen leagueSelectorCurrentMenu;
	private MenuScreen highScoreMenu;
	private SimpleMenuElementNew highscoreItem;
	private ActionMenuElement startItem;
	private OptionsMenuElement perspectiveOptionItem;
	private OptionsMenuElement shadowModeOptionItem;
	private OptionsMenuElement trackColorOptionItem;
	private OptionsMenuElement mapColorGradientOptionItem;
	private OptionsMenuElement mapFillModeOptionItem;
	private OptionsMenuElement mapAcrossTicksOptionItem;
	private OptionsMenuElement gradientStepsOptionItem;
	private OptionsMenuElement neonColorOptionItem;
	private ColorsMenuScreen colorsScreen;
	private MenuScreen trackCopyChooserScreen;
	private MenuScreen neonCopyChooserScreen;
	private SimpleMenuElementNew colorsMenuItem;
	private SimpleMenuElementNew copyTrackPresetItem;
	private SimpleMenuElementNew copyNeonPresetItem;
	private OptionsMenuElement driverSpriteOptionItem;
	private OptionsMenuElement bikeSpriteOptionItem;
	private OptionsMenuElement inputOptionItem;
	private OptionsMenuElement lookAheadOptionItem;
	private OptionsMenuElement keyboardInMenuOptionItem;
	private OptionsMenuElement keypadLandscapeSideOptionItem;
	private OptionsMenuElement controllerAutohideOptionItem;
	private OptionsMenuElement stickModeOptionItem;
	private OptionsMenuElement stickLayoutOptionItem;
	private OptionsMenuElement stickInvertOptionItem;
	private OptionsMenuElement stickAxisFlipOptionItem;
	private OptionsMenuElement stickDeadzoneOptionItem;
	private OptionsMenuElement buttonAbSwapOptionItem;
	private OptionsMenuElement immersiveModeOptionItem;
	private OptionsMenuElement immersiveNavOptionItem;
	private OptionsMenuElement darkModeOptionItem;
	private OptionsMenuElement fpsOverlayOptionItem;
	private OptionsMenuElement vibrateOnTouchOptionItem;
	private SimpleMenuElementNew clearHighscoreOptionItem;
	private SimpleMenuElementNew fullResetItem;
	// private ActionMenuElement yesAction;
	// private ActionMenuElement noAction;
	private SimpleMenuElementNew aboutMenuItem;
	private MenuScreen objectiveHelpScreen;
	private SimpleMenuElementNew objectiveHelpItem;
	private MenuScreen keysHelpScreen;
	private SimpleMenuElementNew keysHelpItem;
	private MenuScreen unlockingHelpScreen;
	private SimpleMenuElementNew unlockingHelpItem;
	private MenuScreen highscoreHelpScreen;
	private SimpleMenuElementNew highscoreHelpItem;
	private MenuScreen optionsHelpScreen;
	private SimpleMenuElementNew optionsHelpItem;
	private NameInputMenuScreen nameScreen;
	private ActionMenuElement continueAction;
	// private ActionMenuElement goToMainAction;
	// private ActionMenuElement exitMenuItem;
	private ActionMenuElement ingameRestartAction;
	private ActionMenuElement finishedRestartAction;
	private ActionMenuElement nextAction;
	// private ActionMenuElement okAction;
	private ActionMenuElement nameAction;
	private long lastTrackTime;
	private int m_ajI;
	private int m_atI;
	private String finishedTime;
	private byte[] nameChars;
	// private RecordStore recordStore;
	// private int m_afI = -1;
	private boolean settingsLoadedOK;
	private int levelIndex = 0;
	private int track = 0;
	private boolean leagueCompleted = false;
	private boolean m_SZ = false;
	private Object m_BObject;
	private String[] difficultyLevels = null; /* {
			"Easy", "Medium", "Hard"
	}; */
	// private long finishTime = 0L;
	/*private byte perspectiveOptionEnabled = 0;
	private byte shadowsOptionEnabled = 0;
	private byte driverSpriteOptionEnabled = 0;
	private byte bikerSpriteOptionEnabled = 0;
	private byte inputOptionValue = 0;
	private byte lookAheadOptionEnabled = 0;
	private byte keyboardInMenuEnabled = 1;
	private byte vibrateOnTouchEnabled = 1;*/
	// private byte selectedTrackIndex = 0;
	// private byte selectedLevel = 0;
	// private byte selectedLeague = 0;
	// private byte m_aTB = 0;
	// private byte m_arB = 0;
	private String[] onOffStrings = null;
	private String[] keysetStrings = null;
	private String[] keypadSideStrings = null;
	private String[] controllerAutohideStrings = null;
	private String[] stickModeStrings = null;
	private String[] stickLayoutStrings = null;
	private String[] stickLrStrings = null;
	private String[] stickDeadzoneStrings = null;
	private String[] shadowModeStrings = null;
	private String[] trackColorStrings = null;
	private String[] mapColorGradientStrings = null;
	private String[] mapFillModeStrings = null;
	private String[] gradientStepsStrings = null;
	private String[] neonColorStrings = null;
	private String[] customSlotStrings = null;
	// private EmptyLineMenuElement emptyLine;
	// private EmptyLineMenuElement emptyLineBeforeAction;
	// private AlertDialog alertDialog = null;
	private Paint bgPaint;
	public MenuScreen managerScreen;
	public InstalledLevelsMenuScreen managerInstalledScreen;
	public DownloadLevelsMenuScreen managerDownloadScreen;
	// private MenuScreen managerDownloadOptionsScreen;
	private SimpleMenuElementNew managerMenuItem;
	public MenuScreen levelScreen;
	// private SimpleMenuElement managerInstalledItem;
	// private SimpleMenuElement managerDownloadItem;
	// private HelmetRotationTask helmetRotationTask;
	// private Timer helmetRotationTimer;
	// public int helmetAngle;

	public Menu() {
		// Translucent scrim drawn over the game canvas while a menu is up.
		// Color is recomputed each frame in drawBackgroundColor() so the
		// dark-mode toggle takes effect without rebuilding the menu.
		bgPaint = new Paint();
	}

	public void load(int step) {
		GDActivity activity = getGDActivity();
		Loader loader = getLevelLoader();
		LevelsManager levelsManager = getLevelsManager();

		level = levelsManager.getCurrentLevel();

		switch (step) {
			case 1:
				m_BObject = new Object();
				nameChars = new byte[]{
						65, 65, 65 // A A A
				};
				onOffStrings = getStringArray(R.array.on_off);
				keysetStrings = getStringArray(R.array.keyset);
				keypadSideStrings = getStringArray(R.array.keypad_side_options);
				controllerAutohideStrings = getStringArray(R.array.controller_autohide_options);
				stickModeStrings = getStringArray(R.array.stick_mode_options);
				stickLayoutStrings = getStringArray(R.array.stick_layout_options);
				stickLrStrings = getStringArray(R.array.stick_lr_options);
				stickDeadzoneStrings = getStringArray(R.array.stick_deadzone_options);
				shadowModeStrings = getStringArray(R.array.shadow_mode_options);
				trackColorStrings = getStringArray(R.array.track_color_options);
				mapColorGradientStrings = getStringArray(R.array.map_color_gradient_options);
			mapFillModeStrings = getStringArray(R.array.map_fill_mode_options);
				gradientStepsStrings = getStringArray(R.array.gradient_steps_options);
				neonColorStrings = getStringArray(R.array.neon_color_options);
				customSlotStrings = getStringArray(R.array.custom_slot_options);
				difficultyLevels = getStringArray(R.array.difficulty);

				// saveManager = new SaveManager();
				lastTrackTime = -1L;
				m_ajI = -1;
				m_atI = -1;
				finishedTime = null;
				// settingsLoadedOK = false;
				/*settings = new byte[SETTINGS_LENGTH];
				for (int l = 0; l < SETTINGS_LENGTH; l++)
					settings[l] = -127;*/

				settingsLoadedOK = true;
				/*try {
					recordStore = RecordStore.openRecordStore(*//*(Loader.levelsFile != null ? Loader.levelsFile.getName().hashCode() : "") + *//*
							getLevelsManager().getCurrentId() + "_" + "GDTRStates", true);
					settingsLoadedOK = true;
					return;
				} catch (Exception _ex) {
					settingsLoadedOK = false;
				}*/

				break;

			case 2:
				// m_afI = -1;
				/*RecordEnumeration enumeration;
				try {
					enumeration = recordStore.enumerateRecords(null, null, false);
				} catch (*//*RecordStoreNotOpen*//*Exception _ex) {
					return;
				}
				if (enumeration.numRecords() > 0) {
					byte[] abyte0;
					try {
						abyte0 = enumeration.nextRecord();
						enumeration.reset();
						m_afI = enumeration.nextRecordId();
					} catch (Exception _ex) {
						return;
					}
					if (abyte0.length <= SETTINGS_LENGTH)
						System.arraycopy(abyte0, 0, settings, 0, abyte0.length);
					enumeration.destroy();
				}*/

				/*byte[] chars;
				if ((chars = readNameChars(16, (byte) -1)) != null && chars[0] != -1) {
					for (int i = 0; i < 3; i++)
						nameChars[i] = chars[i];

				}*/

				nameChars = Settings.getName();
				// if (nameChars[0] == 82 && nameChars[1] == 75 && nameChars[2] == 69) {
				if (isNameCheat(nameChars)) {
					// Unlock everything for cheat
					level.setUnlockedLeagues(3);
					level.setUnlockedLevels(2);
					level.setUnlocked(
							loader.names[0].length - 1,
							loader.names[1].length - 1,
							loader.names[2].length - 1
					);
					// logDebug(level);
					// leaguesUnlockedCount = 3;
					// levelsUnlockedCount = 2;
					/*unlockedTracks[0] = (byte) (loader.names[0].length - 1);
					unlockedTracks[1] = (byte) (loader.names[1].length - 1);
					unlockedTracks[2] = (byte) (loader.names[2].length - 1);*/
				} else if (level.isSettingsClear()) {
					level.setUnlockedLeagues(0);
					level.setUnlockedLevels(1);
					level.setUnlocked(0, 0, -1);
					// leaguesUnlockedCount = 0;
					// levelsUnlockedCount = 1;
					/*unlockedTracks[0] = 0;
					unlockedTracks[1] = 0;
					unlockedTracks[2] = -1;*/
				}
				break;

			case 3:
				// Load settings
				/*perspectiveOptionEnabled = readSetting(0, perspectiveOptionEnabled);
				shadowsOptionEnabled = readSetting(1, shadowsOptionEnabled);
				driverSpriteOptionEnabled = readSetting(2, driverSpriteOptionEnabled);
				bikerSpriteOptionEnabled = readSetting(3, bikerSpriteOptionEnabled);
				lookAheadOptionEnabled = readSetting(4, lookAheadOptionEnabled);

				keyboardInMenuEnabled = readSetting(13, keyboardInMenuEnabled);
				inputOptionValue = readSetting(14, inputOptionValue);*/
				// m_arB = readSetting(15, m_arB); // nonsense

				/*vibrateOnTouchEnabled = readSetting(19, keyboardInMenuEnabled);

				selectedLevel = readSetting(10, selectedLevel);
				selectedTrackIndex = readSetting(11, selectedTrackIndex);
				selectedLeague = readSetting(12, selectedLeague);*/

				// byte levelsSort = readSetting(20, (byte)0);

				DownloadLevelsMenuScreen.sort = Settings.getLevelsSort();


				levelIndex = level.getSelectedLevel();
				track = level.getSelectedTrack();

				if (nameChars[0] != 82 || nameChars[1] != 75 || nameChars[2] != 69) {
					//level.setUnlockedLeagues();
					/*leaguesUnlockedCount = readSetting(5, leaguesUnlockedCount);
					levelsUnlockedCount = readSetting(6, levelsUnlockedCount);
					for (int i = 0; i < 3; i++)
						unlockedTracks[i] = readSetting(7 + i, unlockedTracks[i]);*/
				}

				try {
					selectedTrack[level.getSelectedLevel()] = level.getSelectedTrack();
				} catch (ArrayIndexOutOfBoundsException _ex) {
					level.setSelectedLevel(0);
					level.setSelectedTrack(0);
					selectedTrack[level.getSelectedLevel()] = level.getSelectedTrack();
				}
				getLevelLoader().setPerspectiveEnabled(Settings.isPerspectiveEnabled());
				getLevelLoader().setShadowMode(Settings.getShadowMode());
				getLevelLoader().setNeonColor(Settings.getNeonColor());
				getLevelLoader().setTrackColorMode(Settings.getTrackColorMode());
				getLevelLoader().setMapColorGradient(Settings.getMapColorGradient());
				getLevelLoader().setMapFillMode(Settings.getMapFillMode());
				getLevelLoader().setAcrossTicksEnabled(Settings.isAcrossTicksEnabled());
				getLevelLoader().setGradientSteps(Settings.getGradientSteps());
				activity.physEngine._ifZV(Settings.isLookAheadEnabled());
				getGDView().setInputOption(Settings.getInputOption());
				// getGDView()._aZV(m_aTB == 0);
				getGDView()._aZV(true);
				String[] leaguesList = getStringArray(R.array.leagues);
				fullLeaguesList = getStringArray(R.array.leagues_full);
				trackNames = getLevelLoader().names;

				if (level.getUnlockedLeagues() < 3) {
					leagues = leaguesList;
				} else {
					leagues = fullLeaguesList;
				}

				selectedLeague = level.getSelectedLeague();

				break;

			case 4:
				mainMenu = new MenuScreen(getString(R.string.main), null);
				playMenu = new MenuScreen(getString(R.string.play), mainMenu);
				managerScreen = new MenuScreen(getString(R.string.mods), mainMenu);
				optionsMenu = new MenuScreen(getString(R.string.options), mainMenu);
				// Pre-create the Colors submenu and its two copy-into-Custom
				// choosers so the track / gradient / neon option items below
				// can take colorsScreen as their parent screen. Their
				// children (channel rows, copy entries) are wired up later by
				// buildColorsSubmenu().
				colorsScreen = new ColorsMenuScreen(getString(R.string.colors), optionsMenu);
				trackCopyChooserScreen = new MenuScreen(getString(R.string.copy_into_custom_title), colorsScreen);
				neonCopyChooserScreen = new MenuScreen(getString(R.string.copy_into_custom_title), colorsScreen);
				aboutScreen = new MenuScreen(getString(R.string.about) + " v" + getAppVersion(), mainMenu);
				helpMenu = new MenuScreen(getString(R.string.help), mainMenu);

				continueAction = new ActionMenuElement(getString(R.string._continue), ActionMenuElement.CONTINUE, this);
				nextAction = new ActionMenuElement(getString(R.string.track) + ": " + getLevelLoader().getLevelName(0, 1), ActionMenuElement.NEXT, this);
				ingameRestartAction = new ActionMenuElement(getString(R.string.restart) + ": " + getLevelLoader().getLevelName(0, 0), ActionMenuElement.RESTART, this);
				finishedRestartAction = new ActionMenuElement(getString(R.string.restart) + ": " + getLevelLoader().getLevelName(0, 0), ActionMenuElement.RESTART, this);

				/*nextAction = new ActionMenuElement(getString(R.string.track) + ": DEFAULT", ActionMenuElement.NEXT, this);
				ingameRestartAction = new ActionMenuElement(getString(R.string.restart) + ": DEFAULT", ActionMenuElement.RESTART, this);
				finishedRestartAction = new ActionMenuElement(getString(R.string.restart) + ": DEFAULT", ActionMenuElement.RESTART, this);*/

				highScoreMenu = new MenuScreen(getString(R.string.highscores), playMenu);
				finishedMenu = new MenuScreen(getString(R.string.finished), playMenu);
				ingameScreen = new MenuScreen(getString(R.string.ingame), playMenu);
				nameScreen = new NameInputMenuScreen(getString(R.string.enter_name), finishedMenu, nameChars);
				eraseScreen = new MenuScreen(getString(R.string.confirm_clear), optionsMenu);
				resetScreen = new MenuScreen(getString(R.string.confirm_reset), eraseScreen);

				gameMenuItem = new SimpleMenuElementNew(getString(R.string.play_menu), playMenu, this);
				managerMenuItem = new SimpleMenuElementNew(getString(R.string.mods), managerScreen, this);
				aboutMenuItem = new SimpleMenuElementNew(getString(R.string.about), aboutScreen, this);

				mainMenu.addItem(gameMenuItem);
				//if (ENABLE_MANAGER)
				mainMenu.addItem(new SimpleMenuElementNew(getString(R.string.mods), managerScreen, this));
				mainMenu.addItem(new SimpleMenuElementNew(getString(R.string.options), optionsMenu, this));
				mainMenu.addItem(new SimpleMenuElementNew(getString(R.string.help), helpMenu, this));
				mainMenu.addItem(aboutMenuItem);
				mainMenu.addItem(createAction(ActionMenuElement.EXIT));

				levelSelector = new OptionsMenuElement(getString(R.string.level), level.getSelectedLevel(), this, difficultyLevels, false, playMenu);
				trackSelector = new OptionsMenuElement(getString(R.string.track), selectedTrack[level.getSelectedLevel()], this, trackNames[level.getSelectedLevel()], false, playMenu);
				leagueSelector = new OptionsMenuElement(getString(R.string.league), selectedLeague, this, leagues, false, playMenu);
				try {
					trackSelector.setUnlockedCount(level.getUnlocked(level.getSelectedLevel()));
				} catch (ArrayIndexOutOfBoundsException _ex) {
					trackSelector.setUnlockedCount(0);
				}
				levelSelector.setUnlockedCount(level.getUnlockedLevels());
				leagueSelector.setUnlockedCount(level.getUnlockedLeagues());
				highscoreItem = new SimpleMenuElementNew(getString(R.string.highscores), highScoreMenu, this);
				highScoreMenu.addItem(createAction(ActionMenuElement.BACK));
				startItem = new ActionMenuElement(getString(R.string.start) + ">", this);
				playMenu.addItem(startItem);
				playMenu.addItem(levelSelector);
				playMenu.addItem(trackSelector);
				playMenu.addItem(leagueSelector);
				playMenu.addItem(highscoreItem);
				playMenu.addItem(createAction(ActionMenuElement.GO_TO_MAIN));
				// if (hasPointer)
				// 	softwareJoystickOptionItem = new ActionMenuElement("Software Joystick", m_aTB, this, onOffStrings, true, activity, optionsMenu, false);
				perspectiveOptionItem = new OptionsMenuElement(getString(R.string.perspective), Settings.isPerspectiveEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				shadowModeOptionItem = new OptionsMenuElement(getString(R.string.shadows), Settings.getShadowMode(), this, shadowModeStrings, false, optionsMenu);
				trackColorOptionItem = new OptionsMenuElement(getString(R.string.track_color), Settings.getTrackColorMode(), this, trackColorStrings, false, colorsScreen);
				mapColorGradientOptionItem = new OptionsMenuElement(getString(R.string.map_color_gradient), Settings.getMapColorGradient(), this, mapColorGradientStrings, false, colorsScreen);
				mapFillModeOptionItem = new OptionsMenuElement(getString(R.string.map_fill_mode), Settings.getMapFillMode(), this, mapFillModeStrings, false, colorsScreen);
				// Across ticks: 0=On, 1=Off — matches the perspectiveOptionItem
				// convention (a boolean rendered through the onOffStrings array).
				mapAcrossTicksOptionItem = new OptionsMenuElement(getString(R.string.map_across_ticks), Settings.isAcrossTicksEnabled() ? 0 : 1, this, onOffStrings, true, colorsScreen);
				gradientStepsOptionItem = new OptionsMenuElement(getString(R.string.gradient_steps), gradientStepsIndexFromN(Settings.getGradientSteps()), this, gradientStepsStrings, false, colorsScreen);
				neonColorOptionItem = new OptionsMenuElement(getString(R.string.neon_color), Settings.getNeonColor(), this, neonColorStrings, false, colorsScreen);
				driverSpriteOptionItem = new OptionsMenuElement(getString(R.string.driver_sprite), Settings.isDriverSpriteEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				bikeSpriteOptionItem = new OptionsMenuElement(getString(R.string.bike_sprite), Settings.isBikeSpriteEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				inputOptionItem = new OptionsMenuElement(getString(R.string.input), Settings.getInputOption(), this, keysetStrings, false, optionsMenu);
				lookAheadOptionItem = new OptionsMenuElement(getString(R.string.look_ahead), Settings.isLookAheadEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				vibrateOnTouchOptionItem = new OptionsMenuElement(getString(R.string.vibrate_on_touch), Settings.isVibrateOnTouchEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				keyboardInMenuOptionItem = new OptionsMenuElement(getString(R.string.keyboard_in_menu), Settings.isKeyboardInMenuEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				keypadLandscapeSideOptionItem = new OptionsMenuElement(getString(R.string.keypad_landscape_side), Settings.getKeypadLandscapeSide(), this, keypadSideStrings, false, optionsMenu);
				controllerAutohideOptionItem = new OptionsMenuElement(getString(R.string.controller_autohide), controllerAutohideIndexFromSeconds(Settings.getControllerAutoHideTimeoutSec()), this, controllerAutohideStrings, false, optionsMenu);
				stickModeOptionItem = new OptionsMenuElement(getString(R.string.stick_mode), Settings.getStickMode(), this, stickModeStrings, false, optionsMenu);
				stickLayoutOptionItem = new OptionsMenuElement(getString(R.string.stick_layout), Settings.getStickLayout(), this, stickLayoutStrings, false, optionsMenu);
				stickInvertOptionItem = new OptionsMenuElement(getString(R.string.stick_invert), Settings.getStickInvert(), this, stickLrStrings, false, optionsMenu);
				stickAxisFlipOptionItem = new OptionsMenuElement(getString(R.string.stick_axis_flip), Settings.getStickAxisFlip(), this, stickLrStrings, false, optionsMenu);
				stickDeadzoneOptionItem = new OptionsMenuElement(getString(R.string.stick_deadzone), stickDeadzoneIndexFromPct(Settings.getStickDeadzonePct()), this, stickDeadzoneStrings, false, optionsMenu);
				buttonAbSwapOptionItem = new OptionsMenuElement(getString(R.string.button_ab_swap), Settings.isButtonAbSwapEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				immersiveModeOptionItem = new OptionsMenuElement(getString(R.string.immersive_mode), Settings.isImmersiveModeEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				immersiveNavOptionItem = new OptionsMenuElement(getString(R.string.immersive_nav), Settings.isImmersiveNavEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				darkModeOptionItem = new OptionsMenuElement(getString(R.string.dark_mode), Settings.isDarkModeEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				fpsOverlayOptionItem = new OptionsMenuElement(getString(R.string.fps_overlay), Settings.isFpsOverlayEnabled() ? 0 : 1, this, onOffStrings, true, optionsMenu);
				clearHighscoreOptionItem = new SimpleMenuElementNew(getString(R.string.clear_highscore), eraseScreen, this);
				colorsMenuItem = new SimpleMenuElementNew(getString(R.string.colors), colorsScreen, this);
				copyTrackPresetItem = new SimpleMenuElementNew(getString(R.string.copy_track_preset), trackCopyChooserScreen, this);
				copyNeonPresetItem = new SimpleMenuElementNew(getString(R.string.copy_neon_preset), neonCopyChooserScreen, this);

				// Colors submenu — single place for every color-related setting.
				// Track color preset + gradient picker move here; neon color
				// preset moves out of the main Shadows option and into here as
				// a separate picker. Two inline copy-into-Custom chooser
				// screens (track and neon) are wired up below.
				buildColorsSubmenu();

				// if (hasPointer)
				//	optionsMenu.addItem(softwareJoystickOptionItem);
				optionsMenu.addItem(perspectiveOptionItem);
				optionsMenu.addItem(shadowModeOptionItem);
				optionsMenu.addItem(colorsMenuItem);
				optionsMenu.addItem(driverSpriteOptionItem);
				optionsMenu.addItem(bikeSpriteOptionItem);
				optionsMenu.addItem(inputOptionItem);
				optionsMenu.addItem(lookAheadOptionItem);
				optionsMenu.addItem(vibrateOnTouchOptionItem);
				optionsMenu.addItem(keyboardInMenuOptionItem);
				optionsMenu.addItem(keypadLandscapeSideOptionItem);
				optionsMenu.addItem(controllerAutohideOptionItem);
				optionsMenu.addItem(stickModeOptionItem);
				optionsMenu.addItem(stickLayoutOptionItem);
				optionsMenu.addItem(stickInvertOptionItem);
				optionsMenu.addItem(stickAxisFlipOptionItem);
				optionsMenu.addItem(stickDeadzoneOptionItem);
				optionsMenu.addItem(buttonAbSwapOptionItem);
				optionsMenu.addItem(immersiveModeOptionItem);
				optionsMenu.addItem(immersiveNavOptionItem);
				optionsMenu.addItem(darkModeOptionItem);
				optionsMenu.addItem(fpsOverlayOptionItem);
				optionsMenu.addItem(clearHighscoreOptionItem);
				optionsMenu.addItem(createAction(ActionMenuElement.BACK));

				// noAction = new ActionMenuElement(getString(R.string.no), 0, this, null, false, mainMenu, true);
				// yesAction = new ActionMenuElement(getString(R.string.yes), 0, this, null, false, mainMenu, true);
				fullResetItem = new SimpleMenuElementNew(getString(R.string.full_reset), resetScreen, this);
				eraseScreen.addItem(new TextMenuElement(getString(R.string.erase_text1)));
				eraseScreen.addItem(new TextMenuElement(getString(R.string.erase_text2)));
				eraseScreen.addItem(createEmptyLine(true));
				eraseScreen.addItem(createAction(ActionMenuElement.NO));
				eraseScreen.addItem(createAction(ActionMenuElement.YES));
				eraseScreen.addItem(fullResetItem);
				resetScreen.addItem(new TextMenuElement(getString(R.string.reset_text1)));
				resetScreen.addItem(new TextMenuElement(getString(R.string.reset_text2)));
				resetScreen.addItem(createEmptyLine(true));
				resetScreen.addItem(createAction(ActionMenuElement.NO));
				resetScreen.addItem(createAction(ActionMenuElement.YES));

				objectiveHelpScreen = new MenuScreen(getString(R.string.objective), helpMenu);
				objectiveHelpScreen.setIsTextScreen(true);
				objectiveHelpItem = new SimpleMenuElementNew(getString(R.string.objective), objectiveHelpScreen, this);
				objectiveHelpScreen.addItem(new TextMenuElement(Html.fromHtml(getString(R.string.objective_text))));
				objectiveHelpScreen.addItem(createAction(ActionMenuElement.BACK));

				keysHelpScreen = new MenuScreen(getString(R.string.keys), helpMenu);
				keysHelpScreen.setIsTextScreen(true);
				keysHelpItem = new SimpleMenuElementNew(getString(R.string.keys), keysHelpScreen, this);
				keysHelpScreen.addItem(new TextMenuElement(Html.fromHtml(getString(R.string.keyset_text))));
				keysHelpScreen.addItem(new ActionMenuElement(getString(R.string.back), ActionMenuElement.BACK, this));

				unlockingHelpScreen = new MenuScreen(getString(R.string.unlocking), helpMenu);
				unlockingHelpScreen.setIsTextScreen(true);
				unlockingHelpItem = new SimpleMenuElementNew(getString(R.string.unlocking), unlockingHelpScreen, this);
				unlockingHelpScreen.addItem(new TextMenuElement(Html.fromHtml(getString(R.string.unlocking_text))));
				unlockingHelpScreen.addItem(createAction(ActionMenuElement.BACK));

				highscoreHelpScreen = new MenuScreen(getString(R.string.highscores), helpMenu);
				highscoreHelpScreen.setIsTextScreen(true);
				highscoreHelpItem = new SimpleMenuElementNew(getString(R.string.highscores), highscoreHelpScreen, this);
				highscoreHelpScreen.addItem(new TextMenuElement(Html.fromHtml(getString(R.string.highscore_text))));
				highscoreHelpScreen.addItem(createAction(ActionMenuElement.BACK));

				optionsHelpScreen = new MenuScreen(getString(R.string.options), helpMenu);
				optionsHelpScreen.setIsTextScreen(true);
				optionsHelpItem = new SimpleMenuElementNew(getString(R.string.options), optionsHelpScreen, this);
				optionsHelpScreen.addItem(new TextMenuElement(Html.fromHtml(getString(R.string.options_text))));
				optionsHelpScreen.addItem(createAction(ActionMenuElement.BACK));

				helpMenu.addItem(objectiveHelpItem);
				helpMenu.addItem(keysHelpItem);
				helpMenu.addItem(unlockingHelpItem);
				helpMenu.addItem(highscoreHelpItem);
				helpMenu.addItem(optionsHelpItem);
				helpMenu.addItem(createAction(ActionMenuElement.BACK));

				aboutScreen.setIsTextScreen(true);
				aboutScreen.addItem(new TextMenuElement(Html.fromHtml(getString(R.string.about_text))));
				aboutScreen.addItem(createAction(ActionMenuElement.BACK));

				ingameScreen.addItem(continueAction);
				ingameScreen.addItem(ingameRestartAction);
				ingameScreen.addItem(new SimpleMenuElementNew(getString(R.string.options), optionsMenu, this));
				ingameScreen.addItem(new SimpleMenuElementNew(getString(R.string.help), helpMenu, this));
				ingameScreen.addItem(createAction(ActionMenuElement.PLAY_MENU));
				nameAction = new ActionMenuElement(getString(R.string.name) + " - " + new String(nameChars), 0, this);
				okCommand = new Command(getString(R.string.ok), 4, 1);
				backCommand = new Command(getString(R.string.back), 2, 1);
				setCurrentMenu(mainMenu, false);

				// LevelsManager
				managerInstalledScreen = new InstalledLevelsMenuScreen(getString(R.string.installed_mods), managerScreen);
				managerDownloadScreen = new DownloadLevelsMenuScreen(getString(R.string.download_mods), managerScreen);
				// managerDownloadOptionsScreen = new MenuScreen(getString(R.string.download_options), managerDownloadScreen);

				/*managerInstalledScreen.setIsLevelsList(true);
				managerDownloadScreen.setIsLevelsList(true);*/

				// LevelsManager
				managerScreen.addItem(new SimpleMenuElementNew(getString(R.string.download_mods), managerDownloadScreen, this));
				managerScreen.addItem(new SimpleMenuElementNew(getString(R.string.installed_mods), managerInstalledScreen, this));
				managerScreen.addItem(createEmptyLine(true));
				// managerScreen.addItem(new ActionMenuElement(getString(R.string.install_mrg), this));
				managerScreen.addItem(new ActionMenuElement(getString(R.string.install_mrg), ActionMenuElement.SELECT_FILE, this));
				managerScreen.addItem(createEmptyLine(true));
				managerScreen.addItem(new ActionMenuElement(getString(R.string.open_levels_folder), ActionMenuElement.OPEN_LEVELS_FOLDER, this));
				managerScreen.addItem(new ActionMenuElement(getString(R.string.change_levels_folder), ActionMenuElement.CHANGE_LEVELS_FOLDER, this));
				managerScreen.addItem(new ActionMenuElement(getString(R.string.rescan_folder), ActionMenuElement.RESCAN_FOLDER, this));

				// LevelsManager installed
				// managerInstalledScreen.addItem(new TextMenuElement(getString(R.string.installed_levels_text)));

				// Level screen
				levelScreen = new MenuScreen("", null);
				break;
		}
	}

	/*public void reloadLevels() {
		Loader loader = getLevelLoader();
		trackNames = loader.names;
		setUnlockedLevels();
	}*/

	protected ActionMenuElement createAction(int action) {
		int r;
		switch (action) {
			case ActionMenuElement.BACK:
				r = R.string.back;
				break;

			case ActionMenuElement.NO:
				r = R.string.no;
				break;

			case ActionMenuElement.YES:
				r = R.string.yes;
				break;

			case ActionMenuElement.EXIT:
				r = R.string.exit;
				break;

			case ActionMenuElement.OK:
				r = R.string.ok;
				break;

			case ActionMenuElement.PLAY_MENU:
				r = R.string.play_menu;
				break;

			case ActionMenuElement.GO_TO_MAIN:
				r = R.string.go_to_main;
				break;

			case ActionMenuElement.RESTART:
				r = R.string.restart;
				break;

			case ActionMenuElement.NEXT:
				r = R.string.next;
				break;

			case ActionMenuElement.CONTINUE:
				r = R.string._continue;
				break;

			case ActionMenuElement.LOAD:
				r = R.string.load_this_game;
				break;

			case ActionMenuElement.INSTALL:
				r = R.string.install_kb;
				break;

			case ActionMenuElement.DELETE:
				r = R.string.delete;
				break;

			case ActionMenuElement.RESTART_WITH_NEW_LEVEL:
				r = R.string.restart_with_new_level;
				break;

			default:
				return null;
		}

		return new ActionMenuElement(getString(r), action, this);
	}

	public EmptyLineMenuElement createEmptyLine(boolean beforeAction) {
		return new EmptyLineMenuElement(beforeAction ? 10 : 20);
	}

	// Construct the Colors submenu and its two inline copy-into-Custom
	// chooser screens. Track and neon families share the
	// ChannelMenuElement / ChannelTarget plumbing; the chooser screens
	// each get three slot ActionMenuElements + Back. The OptionsMenuElement
	// pickers (track, gradient, neon) are already constructed at this
	// point — we only add them to colorsScreen here.
	private void buildColorsSubmenu() {
		// Track FG channel rows (FG R / G / B).
		ChannelMenuElement trackFgR = makeTrackChannelRow(getString(R.string.track_fg_r), Settings.TRACK_CUSTOM_KIND_FG, 0);
		ChannelMenuElement trackFgG = makeTrackChannelRow(getString(R.string.track_fg_g), Settings.TRACK_CUSTOM_KIND_FG, 1);
		ChannelMenuElement trackFgB = makeTrackChannelRow(getString(R.string.track_fg_b), Settings.TRACK_CUSTOM_KIND_FG, 2);
		// Track BG channel rows (BG R / G / B).
		ChannelMenuElement trackBgR = makeTrackChannelRow(getString(R.string.track_bg_r), Settings.TRACK_CUSTOM_KIND_BG, 0);
		ChannelMenuElement trackBgG = makeTrackChannelRow(getString(R.string.track_bg_g), Settings.TRACK_CUSTOM_KIND_BG, 1);
		ChannelMenuElement trackBgB = makeTrackChannelRow(getString(R.string.track_bg_b), Settings.TRACK_CUSTOM_KIND_BG, 2);
		// Track FILL channel rows (FILL R / G / B) — the third color used
		// when MAP_FILL_MODE_THIRD is active. Only meaningful when the
		// user has picked the "Third" fill mode; otherwise the values
		// are stored but unused. Same edit flow as FG/BG: editable on
		// Custom slots, read-only on built-ins (clicking triggers the
		// copy-into-Custom chooser).
		ChannelMenuElement trackFillR = makeTrackChannelRow(getString(R.string.track_fill_r), Settings.TRACK_CUSTOM_KIND_FILL, 0);
		ChannelMenuElement trackFillG = makeTrackChannelRow(getString(R.string.track_fill_g), Settings.TRACK_CUSTOM_KIND_FILL, 1);
		ChannelMenuElement trackFillB = makeTrackChannelRow(getString(R.string.track_fill_b), Settings.TRACK_CUSTOM_KIND_FILL, 2);
		// Neon channel rows (R / G / B).
		ChannelMenuElement neonR = makeNeonChannelRow(getString(R.string.neon_r), 0);
		ChannelMenuElement neonG = makeNeonChannelRow(getString(R.string.neon_g), 1);
		ChannelMenuElement neonB = makeNeonChannelRow(getString(R.string.neon_b), 2);

		colorsScreen.registerTrackRow(trackFgR);
		colorsScreen.registerTrackRow(trackFgG);
		colorsScreen.registerTrackRow(trackFgB);
		colorsScreen.registerTrackRow(trackBgR);
		colorsScreen.registerTrackRow(trackBgG);
		colorsScreen.registerTrackRow(trackBgB);
		colorsScreen.registerTrackRow(trackFillR);
		colorsScreen.registerTrackRow(trackFillG);
		colorsScreen.registerTrackRow(trackFillB);
		colorsScreen.registerNeonRow(neonR);
		colorsScreen.registerNeonRow(neonG);
		colorsScreen.registerNeonRow(neonB);

		// Layout: track section, neon section, back. Empty lines separate
		// groups; matches the visual rhythm of the existing Options screen.
		colorsScreen.addItem(trackColorOptionItem);
		colorsScreen.addItem(mapColorGradientOptionItem);
		colorsScreen.addItem(mapFillModeOptionItem);
		colorsScreen.addItem(mapAcrossTicksOptionItem);
		colorsScreen.addItem(gradientStepsOptionItem);
		colorsScreen.addItem(trackFgR);
		colorsScreen.addItem(trackFgG);
		colorsScreen.addItem(trackFgB);
		colorsScreen.addItem(trackBgR);
		colorsScreen.addItem(trackBgG);
		colorsScreen.addItem(trackBgB);
		colorsScreen.addItem(trackFillR);
		colorsScreen.addItem(trackFillG);
		colorsScreen.addItem(trackFillB);
		colorsScreen.addItem(copyTrackPresetItem);
		colorsScreen.addItem(createEmptyLine(false));
		colorsScreen.addItem(neonColorOptionItem);
		colorsScreen.addItem(neonR);
		colorsScreen.addItem(neonG);
		colorsScreen.addItem(neonB);
		colorsScreen.addItem(copyNeonPresetItem);
		colorsScreen.addItem(createEmptyLine(true));
		colorsScreen.addItem(createAction(ActionMenuElement.BACK));

		// Track copy-into-Custom chooser — three slot actions + Back. The
		// handler in handleAction reads the active track preset, copies its
		// channels into the chosen slot, switches the active preset to
		// that slot, and pops back to colorsScreen.
		trackCopyChooserScreen.addItem(new ActionMenuElement(
				customSlotStrings[0], ActionMenuElement.COPY_TRACK_INTO_CUSTOM_1, this));
		trackCopyChooserScreen.addItem(new ActionMenuElement(
				customSlotStrings[1], ActionMenuElement.COPY_TRACK_INTO_CUSTOM_2, this));
		trackCopyChooserScreen.addItem(new ActionMenuElement(
				customSlotStrings[2], ActionMenuElement.COPY_TRACK_INTO_CUSTOM_3, this));
		trackCopyChooserScreen.addItem(createEmptyLine(true));
		trackCopyChooserScreen.addItem(createAction(ActionMenuElement.BACK));

		neonCopyChooserScreen.addItem(new ActionMenuElement(
				customSlotStrings[0], ActionMenuElement.COPY_NEON_INTO_CUSTOM_1, this));
		neonCopyChooserScreen.addItem(new ActionMenuElement(
				customSlotStrings[1], ActionMenuElement.COPY_NEON_INTO_CUSTOM_2, this));
		neonCopyChooserScreen.addItem(new ActionMenuElement(
				customSlotStrings[2], ActionMenuElement.COPY_NEON_INTO_CUSTOM_3, this));
		neonCopyChooserScreen.addItem(createEmptyLine(true));
		neonCopyChooserScreen.addItem(createAction(ActionMenuElement.BACK));
	}

	// Build a ChannelMenuElement bound to one R/G/B channel of one of
	// the three Custom track slots' FG, BG, or FILL color (kind =
	// Settings.TRACK_CUSTOM_KIND_*). The active slot is resolved at
	// click time from Settings.getTrackColorMode() — if the current
	// preset is a built-in, ChannelTarget.isCurrentPresetCustom()
	// returns false and enterCopyFlow() takes the user to the chooser
	// instead of entering edit mode.
	private ChannelMenuElement makeTrackChannelRow(String label, final int kind, final int channel) {
		final ChannelMenuElement.ChannelTarget target = new ChannelMenuElement.ChannelTarget() {
			@Override
			public int getValue() {
				int mode = Settings.getTrackColorMode();
				int slot = Settings.getTrackCustomSlotIndex(mode);
				if (slot < 0) {
					// Built-in preset: surface the read-only ARGB for display
					// (the row never enters edit mode for these; the value is
					// just shown so the user can see what they'd copy).
					int argb;
					switch (kind) {
						case Settings.TRACK_CUSTOM_KIND_BG:   argb = Settings.getTrackBackgroundArgb(mode); break;
						case Settings.TRACK_CUSTOM_KIND_FILL: argb = Settings.getTrackFillArgb(mode);       break;
						default:                              argb = Settings.getTrackForegroundArgb(mode); break;
					}
					return (argb >> (16 - channel * 8)) & 0xff;
				}
				return Settings.getTrackCustomChannel(slot, kind, channel);
			}

			@Override
			public void setValue(int clamped) {
				int slot = Settings.getTrackCustomSlotIndex(Settings.getTrackColorMode());
				if (slot < 0) return; // built-in — should not have entered edit
				Settings.setTrackCustomChannel(slot, kind, channel, clamped);
			}

			@Override
			public boolean isCurrentPresetCustom() {
				return Settings.getTrackCustomSlotIndex(Settings.getTrackColorMode()) >= 0;
			}

			@Override
			public void enterCopyFlow() {
				setCurrentMenu(trackCopyChooserScreen, false);
			}
		};
		return new ChannelMenuElement(label, target);
	}

	private ChannelMenuElement makeNeonChannelRow(String label, final int channel) {
		final ChannelMenuElement.ChannelTarget target = new ChannelMenuElement.ChannelTarget() {
			@Override
			public int getValue() {
				int color = Settings.getNeonColor();
				int slot = Settings.getNeonCustomSlotIndex(color);
				if (slot < 0) {
					int argb = Settings.getShadowNeonBaseColor(color);
					return (argb >> (16 - channel * 8)) & 0xff;
				}
				return Settings.getNeonCustomChannel(slot, channel);
			}

			@Override
			public void setValue(int clamped) {
				int slot = Settings.getNeonCustomSlotIndex(Settings.getNeonColor());
				if (slot < 0) return;
				Settings.setNeonCustomChannel(slot, channel, clamped);
			}

			@Override
			public boolean isCurrentPresetCustom() {
				return Settings.getNeonCustomSlotIndex(Settings.getNeonColor()) >= 0;
			}

			@Override
			public void enterCopyFlow() {
				setCurrentMenu(neonCopyChooserScreen, false);
			}
		};
		return new ChannelMenuElement(label, target);
	}

	public int getSelectedLevel() {
		return levelSelector.getSelectedOption();
	}

	public int getSelectedTrack() {
		return trackSelector.getSelectedOption();
	}

	// not sure about this name
	public boolean canStartTrack() {
		if (m_SZ) {
			m_SZ = false;
			return true;
		} else {
			return false;
		}
	}

	private void saveCompletedTrack() {
		// ATTENTION!!!
		// WHEN CHANGING THIS CODE, COPY-PASTE TO startTrack() !!!

		LevelsManager levelsManager = getLevelsManager();

		try {
			currentScores.saveHighScore(leagueSelector.getSelectedOption(), new String(nameChars, "UTF-8"), lastTrackTime);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			showAlert(getString(R.string.error), e.getMessage(), null);
		}
		// saveManager.write();
		levelsManager.saveHighScores(currentScores);

		leagueCompleted = false;

		finishedMenu.clear();
		finishedMenu.addItem(new TextMenuElement(Html.fromHtml("<b>" + getString(R.string.time) + "</b>: " + finishedTime)));

		System.gc();
		String[] as = currentScores.getScores(leagueSelector.getSelectedOption());
		for (int k = 0; k < as.length; k++)
			if (as[k] != null)
				finishedMenu.addItem(new TextMenuElement("" + (k + 1) + ". " + as[k]));

		byte byte0 = -1;
		// logDebug("trackSelector.getUnlockedCount() = " + trackSelector.getUnlockedCount());
		// logDebug("trackSelector.getSelectedOption() = " + trackSelector.getSelectedOption());

		// {
		// int unlockedTracks = trackSelector.getUnlockedCount();
		// int selectedTrack = trackSelector.getSelectedOption();
		// int selectedLevel = levelSelector.getSelectedOption();
		// int unlockedInLevel = level.getUnlocked(selectedLevel);

		// logDebug("unlockedTracks (trackSelector) = " + trackSelector.getUnlockedCount());
		// logDebug("selectedTrack (trackSelector) = " + selectedTrack);
		// logDebug("selectedLevel (levelSelector) = " + levelSelector.getSelectedOption());
		// logDebug("unlockedInLevel (level.getUnlocked()) = " + level.getUnlocked(levelSelector.getSelectedOption()));

		if (trackSelector.getUnlockedCount() >= trackSelector.getSelectedOption()) {
			trackSelector.setUnlockedCount(
					trackSelector.getSelectedOption() + 1 >= level.getUnlocked(levelSelector.getSelectedOption())
							? trackSelector.getSelectedOption() + 1
							: level.getUnlocked(levelSelector.getSelectedOption())
			);
			level.setUnlocked(levelSelector.getSelectedOption(),
					trackSelector.getUnlockedCount() >= level.getUnlocked(levelSelector.getSelectedOption())
							? trackSelector.getUnlockedCount()
							: level.getUnlocked(levelSelector.getSelectedOption())
			);
		}
		// }

		// Completed league
		if (trackSelector.getSelectedOption() == trackSelector.getOptionCount()) {
			leagueCompleted = true;
			switch (levelSelector.getSelectedOption()) {
				default:
					break;

				case 0:
					if (level.getUnlockedLeagues() < 1) {
						byte0 = 1;
						level.setUnlockedLeagues(1);
						// leaguesUnlockedCount = 1;
						leagueSelector.setUnlockedCount(level.getUnlockedLeagues());
					}
					break;

				case 1:
					if (level.getUnlockedLeagues() < 2) {
						byte0 = 2;
						level.setUnlockedLeagues(2);
						// leaguesUnlockedCount = 2;
						leagueSelector.setUnlockedCount(level.getUnlockedLeagues());
					}
					break;

				case 2:
					if (level.getUnlockedLeagues() < 3) {
						byte0 = 3;
						level.setUnlockedLeagues(3);
						leagueSelector.setOptions(fullLeaguesList);
						leagues = fullLeaguesList;
						leagueSelector.setUnlockedCount(level.getUnlockedLeagues());
					}
					break;
			}

			levelSelector.setUnlockedCount(levelSelector.getUnlockedCount() + 1);

			int newUnlocked = level.getUnlocked(levelSelector.getSelectedOption()) + 1,
					tracksCount = level.getCount(levelSelector.getSelectedOption());

			if (newUnlocked > tracksCount)
				newUnlocked = tracksCount;

			level.setUnlocked(levelSelector.getSelectedOption(), newUnlocked);
			if (level.getUnlocked(levelSelector.getUnlockedCount()) == -1) {
				level.setUnlocked(levelSelector.getUnlockedCount(), 0);
			}
			// if (unlockedTracks[levelSelector.getUnlockedCount()] == -1)
			//	unlockedTracks[levelSelector.getUnlockedCount()] = 0;
		} else {
			trackSelector.performAction(MenuScreen.KEY_RIGHT);
		}

		// int completedCount = _bbII(levelSelector.getSelectedOption());
		int completedCount = level.getUnlocked(levelSelector.getSelectedOption()); // TODO test
		finishedMenu.addItem(new TextMenuElement(Html.fromHtml(String.format(getString(R.string.tracks_completed_tpl),
				completedCount, trackNames[levelSelector.getSelectedOption()].length, difficultyLevels[levelSelector.getSelectedOption()]))));
		System.gc();

		if (!leagueCompleted) {
			ingameRestartAction.setText(getString(R.string.restart) + ": " + getLevelLoader().getLevelName(levelSelector.getSelectedOption(), trackSelector.getSelectedOption()));
			nextAction.setText(getString(R.string.next) + ": " + getLevelLoader().getLevelName(levelIndex, track + 1));

			// getLevelsManager().updateLevelSettings();
			saveAll();
		} else {
			// League completed
			if (levelSelector.getSelectedOption() < levelSelector.getOptionCount()) {
				levelSelector.setSelectedOption(levelSelector.getSelectedOption() + 1);
				trackSelector.setSelectedOption(0);
				trackSelector.setUnlockedCount(level.getUnlocked(levelSelector.getSelectedOption()));
			}

			if (byte0 != -1) {
				finishedMenu.addItem(new TextMenuElement(getString(R.string.congratulations) + leagues[byte0]));
				if (byte0 == 3)
					finishedMenu.addItem(new TextMenuElement(getString(R.string.enjoy)));
				showAlert(getString(R.string.league_unlocked), getString(R.string.league_unlocked_text) + leagues[byte0], null);

				// getLevelsManager().updateLevelSettings();
				saveAll();
			} else {
				boolean flag = true;
				for (int i1 = 0; i1 < 3; i1++)
					if (level.getUnlocked(i1) != getLevelLoader().names[i1].length - 1)
						flag = false;

				if (!flag)
					finishedMenu.addItem(new TextMenuElement(getString(R.string.level_completed_text)));
			}
		}

		if (!leagueCompleted)
			finishedMenu.addItem(nextAction);

		finishedRestartAction.setText(getString(R.string.restart) + ": " + getLevelLoader().getLevelName(levelIndex, track));
		finishedMenu.addItem(finishedRestartAction);
		finishedMenu.addItem(createAction(ActionMenuElement.PLAY_MENU));

		setCurrentMenu(finishedMenu, false);
	}

	//public void _hvV() {
	//	getGDActivity().m_di.postInvalidate();
	//}

	/* public int getGameViewScaledHeight() {
		return getGDView().getScaledHeight();
	}

	public int getGameViewScaledWidth() {
		return getGDView().getScaledWidth();
	} */

	public void showMenu(int k) {
		logDebug("[Menu] showMenu()");
		// k = 2;

		GDActivity gd = getGDActivity();
		GameView view = getGDView();
		Loader loader = getLevelLoader();

		m_blZ = false;
		menuDisabled = false;
		switch (k) {
			case 0: // Just started
				setCurrentMenu(mainMenu, false);
				gd.physEngine._casevV();
				m_SZ = true;
				break;

			case 1: // Ingame
				levelIndex = levelSelector.getSelectedOption();
				track = trackSelector.getSelectedOption();
				ingameRestartAction.setText(getString(R.string.restart) + ": " + loader.getLevelName(levelIndex, track));
				m_SZ = false;
				ingameScreen.resetHighlighted();
				setCurrentMenu(ingameScreen, false);
				break;

			case 2: // Finished
				// finishTime = System.currentTimeMillis();
				finishedMenu.clear();

				levelIndex = levelSelector.getSelectedOption();
				track = trackSelector.getSelectedOption();
				HighScores scores = getLevelsManager().getHighScores(levelSelector.getSelectedOption(), trackSelector.getSelectedOption());
				currentScores = scores;

				// saveManager.setTrack(levelSelector.getSelectedOption(), trackSelector.getSelectedOption());
				int place = scores.getPlace(leagueSelector.getSelectedOption(), lastTrackTime);
				finishedTime = getDurationString(lastTrackTime);

				if (place >= 0 && place <= 2) {
					HighScoreTextMenuElement placeText = new HighScoreTextMenuElement("");
					placeText.setText(getStringArray(R.array.finished_places)[place]);
					placeText.setMedal(true, place);

					finishedMenu.addItem(placeText);

					TextMenuElement h2 = new TextMenuElement(finishedTime);
					finishedMenu.addItem(h2);

					// finishedMenu.addItem(createEmptyLine(true));
					finishedMenu.addItem(createAction(ActionMenuElement.OK));
					finishedMenu.addItem(nameAction);

					setCurrentMenu(finishedMenu, false);
					m_blZ = false;
				} else {
					saveCompletedTrack();
				}
				break;

			default:
				setCurrentMenu(mainMenu, false);
				break;
		}

		long l1 = System.currentTimeMillis();
		view.drawTimer = false;
		long l4 = 0L;
		int i1 = 50;
		gd.physEngine._charvV();
		gd.gameToMenu();

		do {
			if (!gd.isMenuShown() || !gd.alive || currentMenu == null)
				break;

			if (gd.m_cZ) {
				while (gd.m_cZ) {
					// logDebug("[Menu] showMenu() waiting loop");
					if (!gd.alive || currentMenu == null) {
						break;
					}

					try {
						Thread.sleep(100L);
					} catch (InterruptedException e) {
					}
				}
			}
			if (gd.physEngine != null && gd.physEngine._gotovZ()) {
				int j1;
				if ((j1 = gd.physEngine._dovI()) != 0 && j1 != 4)
					try {
						gd.physEngine._doZV(true);
					} catch (NullPointerException e) {
					}
				gd.physEngine._charvV();
				// _hvV();
				long l2;
				if ((l2 = System.currentTimeMillis()) - l4 < (long) i1) {
					try {
						synchronized (m_BObject) {
							m_BObject.wait((long) i1 - (l2 - l4) >= 1L ? (long) i1 - (l2 - l4) : 1L);
						}
					} catch (InterruptedException e) {
					}
					l4 = System.currentTimeMillis();
				} else {
					l4 = l2;
				}
			} else {
				i1 = 50;
				long l3;
				if ((l3 = System.currentTimeMillis()) - l4 < (long) i1) {
					Object obj;
					try {
						synchronized (obj = new Object()) {
							obj.wait((long) i1 - (l3 - l4) >= 1L ? (long) i1 - (l3 - l4) : 1L);
						}
					} catch (InterruptedException e) {
					}
					l4 = System.currentTimeMillis();
				} else {
					l4 = l3;
				}
			}
		} while (true);

		logDebug("[Menu.showMenu] out loop");

		gd.m_forJ += System.currentTimeMillis() - l1;
		if (view != null)
			view.drawTimer = true;

		if (currentMenu == null && gd != null) {
			logDebug("[Menu.showMenu] currentMenu == null, set alive = false");
			gd.exiting = true;
			gd.alive = false;
		}
	}

	public synchronized void draw(Canvas g1) {
		if (currentMenu != null && !m_blZ) {
			getGDView().drawGame(g1);
			drawBackgroundColor(g1);
			// currentMenu.draw(g1);
		}
	}

	private void drawBackgroundColor(Canvas g1) {
		// 0x80 alpha keeps the scrim translucent in both modes; the RGB
		// follows the menu background so dark mode flips white→black here
		// too.
		bgPaint.setColor(0x80000000 | (Settings.getMenuBgColor() & 0x00FFFFFF));
		g1.drawRect(0, 0, getGDView().getScaledWidth(), getGDView().getScaledHeight(), bgPaint);
	}

	public void _tryIV(int k) {
		// logDebug("_tryIV k = " + k);
		if (getGDView().getGameAction(k) != 8)
			keyPressed(k);
	}

	public void keyPressed(int k) {
		if (currentMenu != null && !menuDisabled)
			switch (getGDView().getGameAction(k)) {
				case MenuScreen.KEY_UP: // up
					currentMenu.performAction(MenuScreen.KEY_UP);
					return;

				case MenuScreen.KEY_DOWN: // down
					currentMenu.performAction(MenuScreen.KEY_DOWN);
					return;

				case MenuScreen.KEY_FIRE: // fire
					currentMenu.performAction(MenuScreen.KEY_FIRE);
					return;

				case MenuScreen.KEY_RIGHT: // right
					currentMenu.performAction(MenuScreen.KEY_RIGHT);
					if (currentMenu == highScoreMenu) {
						selectedLeague++;
						if (selectedLeague > leagueSelector.getUnlockedCount())
							selectedLeague = leagueSelector.getUnlockedCount();
						showHighScoreMenu(selectedLeague);
						return;
					}
					break;

				case MenuScreen.KEY_LEFT: // left
					currentMenu.performAction(MenuScreen.KEY_LEFT);
					if (currentMenu != highScoreMenu)
						break;
					selectedLeague--;
					if (selectedLeague < 0)
						selectedLeague = 0;
					showHighScoreMenu(selectedLeague);
					break;
			}
	}

	public void onCommand(Command command) {
		if (command == okCommand) {
			ok();
		} else if (command == backCommand && currentMenu != null) {
			back();
		}
	}

	public void back() {
		if (currentMenu == ingameScreen) {
			getGDActivity().menuToGame();
			return;
		}
		// Give the current screen a chance to consume BACK — e.g.
		// ColorsMenuScreen commits an active channel-row edit and swallows
		// the press so the screen itself doesn't also pop.
		if (currentMenu != null && currentMenu.handleBack()) {
			return;
		}
		if (currentMenu != null)
			setCurrentMenu(currentMenu.getNavTarget(), true);
	}

	public void ok() {
		if (currentMenu != null) {
			currentMenu.performAction(1);
			return;
		}
	}

	public MenuScreen getCurrentMenu() {
		return currentMenu;
	}

	/**
	 * Walk every initialized menu screen's layout and re-apply text colors
	 * from current {@link Settings} (i.e. honor a fresh dark-mode toggle).
	 * Each {@link MenuScreen} caches its own layout in memory, and the
	 * TextViews inside hold a frozen {@code ColorStateList} from creation
	 * time — so flipping the toggle in Options has to push new colors
	 * into screens the user hasn't visited yet (Main, Play, Help, ...),
	 * not just the currently-mounted one.
	 *
	 * <p>Lazy-init screens (manager / level / nameScreen and friends) may
	 * still be null at the time of the toggle; the null guard skips them
	 * — they pick up the right colors at construction.
	 */
	public void refreshAllScreenColors() {
		GDActivity gd = getGDActivity();
		int fg = Settings.getMenuFgColor();
		MenuScreen[] screens = new MenuScreen[]{
				mainMenu, playMenu, optionsMenu, aboutScreen, helpMenu,
				eraseScreen, resetScreen, finishedMenu, ingameScreen,
				levelSelectorCurrentMenu, trackSelectorCurrentMenu,
				leagueSelectorCurrentMenu, highScoreMenu,
				objectiveHelpScreen, keysHelpScreen, unlockingHelpScreen,
				highscoreHelpScreen, optionsHelpScreen,
				nameScreen, managerScreen, managerInstalledScreen,
				managerDownloadScreen, levelScreen,
				colorsScreen, trackCopyChooserScreen, neonCopyChooserScreen,
		};
		for (MenuScreen s : screens) {
			if (s != null && s.getLayout() != null) {
				gd.repaintMenuTextViews(s.getLayout(), fg);
				// Lock icon variant tracks dark mode too — repaint each
				// item's icon (no-op when no lock is shown).
				s.refreshLockIcons();
			}
		}
	}

	/**
	 * True when the user is on the top-level main menu. Used by
	 * {@link GDActivity#updateBackCallbackEnabled()} to decide whether to
	 * intercept the back press (sub-menu / in-game) or let the system
	 * handle it natively (main menu → predictive back + activity finish).
	 */
	public boolean isAtMainMenu() {
		return currentMenu == mainMenu;
	}

	public void setCurrentMenu(MenuScreen newMenu, boolean flag) {
		menuDisabled = false;
		GDActivity gd = getGDActivity();
		GameView view = getGDView();

		if (!Settings.isKeyboardInMenuEnabled()) {
			if (newMenu == nameScreen) {
				gd.showKeyboardLayout();
			} else {
				gd.hideKeyboardLayout();
			}
		}

		view.removeCommand(backCommand);
		if (newMenu != mainMenu && newMenu != finishedMenu && newMenu != null)
			view.addCommand(backCommand);

		if (newMenu == highScoreMenu) {
			selectedLeague = leagueSelector.getSelectedOption();
			showHighScoreMenu(selectedLeague);
		} else if (newMenu == finishedMenu) {
			// logDebug("it's finished!!!");
			nameChars = nameScreen.getChars();
			nameAction.setText(getString(R.string.name) + " - " + new String(nameChars));
		} else if (newMenu == playMenu) {
			trackSelector.setOptions(getLevelLoader().names[levelSelector.getSelectedOption()], false);
			if (currentMenu == trackSelectorCurrentMenu) {
				selectedTrack[levelSelector.getSelectedOption()] = trackSelector.getSelectedOption();
			}
			trackSelector.setUnlockedCount(level.getUnlocked(levelSelector.getSelectedOption()));
			trackSelector.setSelectedOption(selectedTrack[levelSelector.getSelectedOption()]);
		}
		if (newMenu == mainMenu || newMenu == playMenu && gd.physEngine != null)
			gd.physEngine._casevV();

		if (currentMenu != null)
			currentMenu.onHide(newMenu);

		currentMenu = newMenu;
		if (currentMenu != null) {
			gd.setMenu(currentMenu.getLayout());
			currentMenu.onShow();

			// getGDActivity().setMenu(currentMenu.getTable());
			// if (!isOnOffToggle) currentMenu.scrollUp();
		}

		// getGDActivity().physEngine._casevV();
		m_blZ = false;

		// */

		// Re-evaluate whether the system or our callback handles back.
		// At the main menu we want the system to handle it (predictive
		// back peek + native finish); elsewhere we intercept to navigate.
		gd.updateBackCallbackEnabled();
	}

	public void showHighScoreMenu(int league) {
		HighScores highScores = getLevelsManager().getHighScores(levelSelector.getSelectedOption(), trackSelector.getSelectedOption());

		highScoreMenu.clear();
		highScoreMenu.setTitle(getString(R.string.highscores) + ": " + getLevelLoader().getLevelName(levelSelector.getSelectedOption(), trackSelector.getSelectedOption()));

		HighScoreTextMenuElement subtitle = new HighScoreTextMenuElement(Html.fromHtml(getString(R.string.league) + ": " + leagueSelector.getOptions()[league]));
		subtitle.setIsSubtitle(true);

		highScoreMenu.addItem(subtitle);

		String[] scores = highScores.getScores(league);

		for (int place = 0; place < scores.length; place++) {
			if (scores[place] == null)
				continue;

			HighScoreTextMenuElement h1 = new HighScoreTextMenuElement("" + (place + 1) + ". " + scores[place]);
			if (place == 0)
				h1.setMedal(true, 0);
			else if (place == 1)
				h1.setMedal(true, 1);
			else if (place == 2)
				h1.setMedal(true, 2);

			h1.setLayoutPadding(true);
			highScoreMenu.addItem(h1);
		}

		// saveManager.closeRecordStore();
		if (scores[0] == null)
			highScoreMenu.addItem(new TextMenuElement(getString(R.string.no_highscores)));

		highScoreMenu.addItem(createAction(ActionMenuElement.BACK));
		highScoreMenu.highlightElement();

		// System.gc();
	}

	public synchronized void destroy() {
		currentMenu = null;
	}

	public synchronized void saveAll() {
		logDebug("saveAll()");

		try {
			if (level != null) {
				Settings.setName(nameChars);

				level.setUnlockedLeagues(leagueSelector.getUnlockedCount());
				level.setUnlockedLevels(levelSelector.getUnlockedCount());

				level.setSelectedLevel(levelSelector.getSelectedOption());
				level.setSelectedTrack(trackSelector.getSelectedOption());
				level.setSelectedLeague(leagueSelector.getSelectedOption());

				getLevelsManager().updateLevelSettings();
			} else {
				logDebug("saveAll(): level == null");
			}
		} catch (Exception e) {
			logDebug("saveAll exception: " + e);
		}
	}

	public void handleAction(MenuElement item) {
		final GDActivity gd = getGDActivity();

		if (currentMenu == null) {
			return;
		}

		if (item == startItem)
			if (levelSelector.getSelectedOption() > levelSelector.getUnlockedCount() || trackSelector.getSelectedOption() > trackSelector.getUnlockedCount() || leagueSelector.getSelectedOption() > leagueSelector.getUnlockedCount()) {
				showAlert("GD Classic", getString(R.string.complete_to_unlock), null);
				return;
			} else {
				gd.physEngine._avV();
				startTrack(levelSelector.getSelectedOption(), trackSelector.getSelectedOption());
				gd.physEngine.setLeague(leagueSelector.getSelectedOption());
				m_SZ = true;
				gd.menuToGame();
				return;
			}

		if (item == vibrateOnTouchOptionItem) {
			Settings.setVibrateOnTouchEnabled(((OptionsMenuElement) item).getSelectedOption() == 0);
		}
		if (item == buttonAbSwapOptionItem) {
			// ControllerInputHandler reads Settings.isButtonAbSwapEnabled()
			// per dispatchKey call, so no notify is needed — the next A/B
			// press picks up the new mapping.
			Settings.setButtonAbSwapEnabled(((OptionsMenuElement) item).getSelectedOption() == 0);
		}
		if (item == keyboardInMenuOptionItem) {
			boolean enabled = ((OptionsMenuElement) item).getSelectedOption() == 0;
			Settings.setKeyboardInMenuEnabled(enabled);
			if (enabled) gd.showKeyboardLayout();
			else gd.hideKeyboardLayout();
		}
		if (item == immersiveModeOptionItem) {
			boolean enabled = ((OptionsMenuElement) item).getSelectedOption() == 0;
			Settings.setImmersiveModeEnabled(enabled);
			gd.applyImmersiveMode();
		}
		if (item == immersiveNavOptionItem) {
			boolean enabled = ((OptionsMenuElement) item).getSelectedOption() == 0;
			Settings.setImmersiveNavEnabled(enabled);
			gd.applyImmersiveMode();
		}
		if (item == darkModeOptionItem) {
			boolean enabled = ((OptionsMenuElement) item).getSelectedOption() == 0;
			Settings.setDarkModeEnabled(enabled);
			gd.applyDarkMode();
		}
		if (item == fpsOverlayOptionItem) {
			// GameView.onDraw / drawGame read the flag every frame, so the
			// overlay appears/disappears on the next vsync without notify.
			Settings.setFpsOverlayEnabled(((OptionsMenuElement) item).getSelectedOption() == 0);
		}
		if (item == controllerAutohideOptionItem) {
			// Click cycles through the 5 timeout choices (Never/5/10/15/30s).
			// Same _charvZ()-then-advance pattern as the other multi-state
			// click-to-toggle items below.
			if (controllerAutohideOptionItem._charvZ()) {
				controllerAutohideOptionItem.setSelectedOption(
						controllerAutohideOptionItem.getSelectedOption() + 1);
			}
			Settings.setControllerAutoHideTimeoutSec(
					controllerAutohideSecondsFromIndex(controllerAutohideOptionItem.getSelectedOption()));
			gd.refreshKeypadIdleTimer();
			return;
		}
		if (item == stickModeOptionItem) {
			// Click cycles through Analog / Digital. Same advance pattern
			// as the other multi-state options; the controller re-reads
			// Settings on every motion event so no notify is needed.
			if (stickModeOptionItem._charvZ()) {
				stickModeOptionItem.setSelectedOption(
						stickModeOptionItem.getSelectedOption() + 1);
			}
			Settings.setStickMode(stickModeOptionItem.getSelectedOption());
			return;
		}
		if (item == stickLayoutOptionItem) {
			// Click cycles through Single / Dual lean-L / Dual gas-L. Same
			// advance pattern as the other multi-state options; the
			// controller re-reads Settings on every motion event so no
			// notify is needed.
			if (stickLayoutOptionItem._charvZ()) {
				stickLayoutOptionItem.setSelectedOption(
						stickLayoutOptionItem.getSelectedOption() + 1);
			}
			Settings.setStickLayout(stickLayoutOptionItem.getSelectedOption());
			return;
		}
		if (item == stickInvertOptionItem) {
			// Click cycles through None / Left / Right / All.
			if (stickInvertOptionItem._charvZ()) {
				stickInvertOptionItem.setSelectedOption(
						stickInvertOptionItem.getSelectedOption() + 1);
			}
			Settings.setStickInvert(stickInvertOptionItem.getSelectedOption());
			return;
		}
		if (item == stickAxisFlipOptionItem) {
			// Click cycles through None / Left / Right / Both.
			if (stickAxisFlipOptionItem._charvZ()) {
				stickAxisFlipOptionItem.setSelectedOption(
						stickAxisFlipOptionItem.getSelectedOption() + 1);
			}
			Settings.setStickAxisFlip(stickAxisFlipOptionItem.getSelectedOption());
			return;
		}
		if (item == stickDeadzoneOptionItem) {
			// Click cycles through the 4 deadzone presets. Same advance
			// pattern as controllerAutohideOptionItem; the controller
			// re-reads Settings on every motion event so no notify is
			// needed.
			if (stickDeadzoneOptionItem._charvZ()) {
				stickDeadzoneOptionItem.setSelectedOption(
						stickDeadzoneOptionItem.getSelectedOption() + 1);
			}
			Settings.setStickDeadzonePct(
					stickDeadzonePctFromIndex(stickDeadzoneOptionItem.getSelectedOption()));
			return;
		}
		if (item == keypadLandscapeSideOptionItem) {
			// _charvZ() returns true when the user fired (clicked/tapped)
			// the item rather than nudging it with left/right. For this
			// 2-state option a click should toggle, so advance the index
			// (setSelectedOption wraps past the end automatically).
			if (keypadLandscapeSideOptionItem._charvZ()) {
				keypadLandscapeSideOptionItem.setSelectedOption(
						keypadLandscapeSideOptionItem.getSelectedOption() + 1);
			}
			Settings.setKeypadLandscapeSide(keypadLandscapeSideOptionItem.getSelectedOption());
			// In landscape (split mode), swapping the side swaps which
			// cluster is on which edge — that's a structural change, so we
			// rebuild rather than just reflow params.
			gd.rebuildKeypad();
			if (gd.isKeyboardLayoutVisible()) {
				gd.showKeyboardLayout();
			}
		}
		if (item == perspectiveOptionItem) {
			gd.physEngine._aZV(perspectiveOptionItem.getSelectedOption() == 0);
			getLevelLoader().setPerspectiveEnabled(perspectiveOptionItem.getSelectedOption() == 0);
			Settings.setPerspectiveEnabled(perspectiveOptionItem.getSelectedOption() == 0);
			return;
		}
		if (item == shadowModeOptionItem) {
			// Tri-state cycle (Off / Shadow / Neon): _charvZ()-then-advance
			// pattern. Neon hue is decoupled — picked separately via
			// neonColorOptionItem under the Colors submenu.
			if (shadowModeOptionItem._charvZ()) {
				shadowModeOptionItem.setSelectedOption(
						shadowModeOptionItem.getSelectedOption() + 1);
			}
			int mode = shadowModeOptionItem.getSelectedOption();
			getLevelLoader().setShadowMode(mode);
			Settings.setShadowMode(mode);
			return;
		}
		if (item == trackColorOptionItem) {
			// Multi-state cycle (8 built-ins + 3 Custom slots). On change,
			// re-pull the 6 track channel rows so they reflect the newly
			// selected preset's values.
			if (trackColorOptionItem._charvZ()) {
				trackColorOptionItem.setSelectedOption(
						trackColorOptionItem.getSelectedOption() + 1);
			}
			int mode = trackColorOptionItem.getSelectedOption();
			getLevelLoader().setTrackColorMode(mode);
			Settings.setTrackColorMode(mode);
			if (colorsScreen != null) colorsScreen.refreshTrackRows();
			return;
		}
		if (item == mapColorGradientOptionItem) {
			// Tri-state cycle (On / Inverted / Off). Same advance pattern.
			if (mapColorGradientOptionItem._charvZ()) {
				mapColorGradientOptionItem.setSelectedOption(
						mapColorGradientOptionItem.getSelectedOption() + 1);
			}
			int mode = mapColorGradientOptionItem.getSelectedOption();
			getLevelLoader().setMapColorGradient(mode);
			Settings.setMapColorGradient(mode);
			return;
		}
		if (item == mapFillModeOptionItem) {
			// 5-state cycle: Off / Foreground / Background / Third / Gradient.
			// Same advance pattern as mapColorGradientOptionItem above.
			if (mapFillModeOptionItem._charvZ()) {
				mapFillModeOptionItem.setSelectedOption(
						mapFillModeOptionItem.getSelectedOption() + 1);
			}
			int mode = mapFillModeOptionItem.getSelectedOption();
			getLevelLoader().setMapFillMode(mode);
			Settings.setMapFillMode(mode);
			return;
		}
		if (item == mapAcrossTicksOptionItem) {
			// 2-state cycle (On=0 / Off=1) — boolean rendered through
			// onOffStrings, mirrors perspectiveOptionItem's shape.
			if (mapAcrossTicksOptionItem._charvZ()) {
				mapAcrossTicksOptionItem.setSelectedOption(
						mapAcrossTicksOptionItem.getSelectedOption() + 1);
			}
			boolean enabled = mapAcrossTicksOptionItem.getSelectedOption() == 0;
			getLevelLoader().setAcrossTicksEnabled(enabled);
			Settings.setAcrossTicksEnabled(enabled);
			return;
		}
		if (item == gradientStepsOptionItem) {
			// 6-state cycle over GRADIENT_STEPS_OPTIONS = {2,3,4,6,8,12}.
			if (gradientStepsOptionItem._charvZ()) {
				gradientStepsOptionItem.setSelectedOption(
						gradientStepsOptionItem.getSelectedOption() + 1);
			}
			int n = gradientStepsNFromIndex(gradientStepsOptionItem.getSelectedOption());
			getLevelLoader().setGradientSteps(n);
			Settings.setGradientSteps(n);
			return;
		}
		if (item == neonColorOptionItem) {
			// 9-state cycle (6 built-in hues + 3 Custom slots). On change,
			// re-pull the 3 neon channel rows so they reflect the newly
			// selected hue. Loader mirror so the render path doesn't hit
			// SharedPreferences per frame.
			if (neonColorOptionItem._charvZ()) {
				neonColorOptionItem.setSelectedOption(
						neonColorOptionItem.getSelectedOption() + 1);
			}
			int color = neonColorOptionItem.getSelectedOption();
			getLevelLoader().setNeonColor(color);
			Settings.setNeonColor(color);
			if (colorsScreen != null) colorsScreen.refreshNeonRows();
			return;
		}
		if (item == driverSpriteOptionItem) {
			if (driverSpriteOptionItem._charvZ()) {
				driverSpriteOptionItem.setSelectedOption(driverSpriteOptionItem.getSelectedOption() + 1);
			}
			Settings.setDriverSpriteEnabled(driverSpriteOptionItem.getSelectedOption() == 0);
		} else if (item == bikeSpriteOptionItem) {
			if (bikeSpriteOptionItem._charvZ()) {
				bikeSpriteOptionItem.setSelectedOption(bikeSpriteOptionItem.getSelectedOption() + 1);
			}
			Settings.setBikeSpriteEnabled(bikeSpriteOptionItem.getSelectedOption() == 0);
		} else {
			if (item == inputOptionItem) {
				if (inputOptionItem._charvZ())
					inputOptionItem.setSelectedOption(inputOptionItem.getSelectedOption() + 1);
				getGDView().setInputOption(inputOptionItem.getSelectedOption());
				Settings.setInputOption(inputOptionItem.getSelectedOption());
				// Cluster shapes in landscape "split-keypad" mode depend on
				// the active keyset — rebuild so the layout updates live.
				gd.rebuildKeypad();
				if (gd.isKeyboardLayoutVisible()) {
					gd.showKeyboardLayout();
				}
				return;
			}
			if (item == lookAheadOptionItem) {
				gd.physEngine._ifZV(lookAheadOptionItem.getSelectedOption() == 0);
				Settings.setLookAheadEnabled(lookAheadOptionItem.getSelectedOption() == 0);
				return;
			}
			if (item instanceof ActionMenuElement) {
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.RESTART_WITH_NEW_LEVEL) {
					LevelsManager manager = gd.levelsManager;
					long nextId = manager.getCurrentId() == 1 ? 2 : 1;
					gd.levelsManager.load(manager.getLeveL(nextId));
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.SELECT_FILE) {
					installFromFileBrowse();
					return;
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.OPEN_LEVELS_FOLDER) {
					gd.openLevelsFolder();
					return;
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.CHANGE_LEVELS_FOLDER) {
					gd.pickNewLevelsFolder(null);
					return;
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.RESCAN_FOLDER) {
					rescanLevelsFolder();
					return;
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.YES) {
					if (currentMenu == eraseScreen) {
						getLevelsManager().clearHighScores();
						showAlert(getString(R.string.cleared), getString(R.string.cleared_text), null);
					} else if (currentMenu == resetScreen) {
						showAlert(getString(R.string.reset), getString(R.string.reset_text), new Runnable() {
							@Override
							public void run() {
								resetAll();
							}
						});
					}
					setCurrentMenu(currentMenu.getNavTarget(), false);
					return;
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.NO) {
					setCurrentMenu(currentMenu.getNavTarget(), false);
					return;
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.BACK) {
					setCurrentMenu(currentMenu.getNavTarget(), true);
					return;
				}
				{
					// Copy-into-Custom dispatch. Each slot action snapshots
					// the current preset's channels into the chosen Custom
					// slot, switches the active preset to that slot, refreshes
					// the channel rows, and pops back to the Colors screen.
					int av = ((ActionMenuElement) item).getActionValue();
					if (av >= ActionMenuElement.COPY_TRACK_INTO_CUSTOM_1
							&& av <= ActionMenuElement.COPY_TRACK_INTO_CUSTOM_3) {
						int slot = av - ActionMenuElement.COPY_TRACK_INTO_CUSTOM_1;
						int src = Settings.getTrackColorMode();
						Settings.copyTrackPresetIntoCustom(src, slot);
						int dstMode = Settings.TRACK_COLOR_CUSTOM_1 + slot;
						Settings.setTrackColorMode(dstMode);
						getLevelLoader().setTrackColorMode(dstMode);
						trackColorOptionItem.setSelectedOption(dstMode);
						colorsScreen.refreshTrackRows();
						setCurrentMenu(colorsScreen, false);
						return;
					}
					if (av >= ActionMenuElement.COPY_NEON_INTO_CUSTOM_1
							&& av <= ActionMenuElement.COPY_NEON_INTO_CUSTOM_3) {
						int slot = av - ActionMenuElement.COPY_NEON_INTO_CUSTOM_1;
						int src = Settings.getNeonColor();
						Settings.copyNeonPresetIntoCustom(src, slot);
						int dstColor = Settings.NEON_COLOR_CUSTOM_1 + slot;
						Settings.setNeonColor(dstColor);
						getLevelLoader().setNeonColor(dstColor);
						neonColorOptionItem.setSelectedOption(dstColor);
						colorsScreen.refreshNeonRows();
						setCurrentMenu(colorsScreen, false);
						return;
					}
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.PLAY_MENU) {
					levelSelector.setSelectedOption(levelIndex);
					trackSelector.setUnlockedCount(level.getUnlocked(levelIndex));
					trackSelector.setSelectedOption(track);
					setCurrentMenu(currentMenu.getNavTarget(), false);
					return;
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.GO_TO_MAIN) {
					setCurrentMenu(mainMenu, false);
					return;
				}
				if (((ActionMenuElement) item).getActionValue() == ActionMenuElement.EXIT) {
					getGDActivity().exiting = true;
					if (currentMenu != null) {
						setCurrentMenu(currentMenu.getNavTarget(), false);
					} else {
						setCurrentMenu(null, false);
					}
					return;
				}
			}

			if (item == ingameRestartAction || item == finishedRestartAction) {
				if (leagueSelector.getSelectedOption() <= leagueSelector.getUnlockedCount()) {
					levelSelector.setSelectedOption(levelIndex);
					trackSelector.setUnlockedCount(level.getUnlocked(levelIndex));
					trackSelector.setSelectedOption(track);
					gd.physEngine.setLeague(leagueSelector.getSelectedOption());
					m_SZ = true;
					gd.menuToGame();
					return;
				}
			} else {
				if (item == nextAction) {
					// if (!leagueCompleted)
					//	trackSelector.performAction(MenuScreen.KEY_RIGHT);
					startTrack(levelSelector.getSelectedOption(), trackSelector.getSelectedOption());
					gd.physEngine.setLeague(leagueSelector.getSelectedOption());
					// saveAll();
					// getLevelsManager().updateLevelSettings();
					m_SZ = true;
					gd.menuToGame();
					return;
				}
				if (item == continueAction) {
					// _hvV();
					gd.menuToGame();
					return;
				}
				if (item == nameAction) {
					nameScreen.resetCursorPosition();
					setCurrentMenu(nameScreen, false);
					return;
				}
				if (item instanceof ActionMenuElement && ((ActionMenuElement) item).getActionValue() == ActionMenuElement.OK) {
					saveCompletedTrack();
					return;
				}
				if (item == trackSelector) {
					if (trackSelector._charvZ()) {
						trackSelector.setUnlockedCount(level.getUnlocked(levelSelector.getSelectedOption()));
						trackSelector.update();
						trackSelectorCurrentMenu = trackSelector.getCurrentMenu();
						setCurrentMenu(trackSelectorCurrentMenu, false);
						// trackSelectorCurrentMenu._doIV(trackSelector.getSelectedOption());
					}
					selectedTrack[levelSelector.getSelectedOption()] = trackSelector.getSelectedOption();
					return;
				}
				if (item == levelSelector) {
					if (levelSelector._charvZ()) {
						levelSelectorCurrentMenu = levelSelector.getCurrentMenu();
						setCurrentMenu(levelSelectorCurrentMenu, false);
					}
					trackSelector.setOptions(getLevelLoader().names[levelSelector.getSelectedOption()], false);
					trackSelector.setUnlockedCount(level.getUnlocked(levelSelector.getSelectedOption()));
					trackSelector.setSelectedOption(selectedTrack[levelSelector.getSelectedOption()]);
					// trackSelector.update();
					// logDebug("update tracks ");
					return;
				}
				if (item == leagueSelector && leagueSelector._charvZ()) {
					leagueSelectorCurrentMenu = leagueSelector.getCurrentMenu();
					// leagueSelector.update();
					leagueSelector.setScreen(currentMenu);
					setCurrentMenu(leagueSelectorCurrentMenu, false);
					// leagueSelectorCurrentMenu._doIV(leagueSelector.getSelectedOption());
				}
			}
		}
	}

	protected void startTrack(int levelIndex, int trackIndex) {
		// ATTENTION!!!
		// WHEN CHANGING THIS CODE, COPY-PASTE TO saveCompletedTrack() !!!

		/*Menu _menu = null;
		_menu.back();*/

		try {
			getLevelLoader()._doIII(levelIndex, trackIndex);
		} catch (InvalidTrackException e) {
			showConfirm(getString(R.string.oops), getString(R.string.e_level_damaged), new Runnable() {
				@Override
				public void run() {
					if (trackSelector.getSelectedOption() + 1 < level.getCount(levelSelector.getSelectedOption())) {
						trackSelector.setUnlockedCount(trackSelector.getSelectedOption() + 1);
						level.setUnlocked(levelSelector.getSelectedOption(), trackSelector.getUnlockedCount());
					} else {
						switch (levelSelector.getSelectedOption()) {
							case 0:
								if (level.getUnlockedLeagues() < 1) {
									level.setUnlockedLeagues(1);
									leagueSelector.setUnlockedCount(level.getUnlockedLeagues());
								}
								break;

							case 1:
								if (level.getUnlockedLeagues() < 2) {
									level.setUnlockedLeagues(2);
									leagueSelector.setUnlockedCount(level.getUnlockedLeagues());
								}
								break;

							case 2:
								if (level.getUnlockedLeagues() < 3) {
									level.setUnlockedLeagues(3);
									leagueSelector.setOptions(fullLeaguesList);
									leagues = fullLeaguesList;
									leagueSelector.setUnlockedCount(level.getUnlockedLeagues());
								}
								break;
						}

						int newUnlocked = level.getUnlocked(levelSelector.getSelectedOption()) + 1,
								tracksCount = level.getCount(levelSelector.getSelectedOption());

						if (newUnlocked > tracksCount)
							newUnlocked = tracksCount;

						levelSelector.setUnlockedCount(levelSelector.getUnlockedCount() + 1);
						level.setUnlocked(levelSelector.getSelectedOption(), newUnlocked);
					}
				}
			}, null);
		}
	}

	public int _jvI() {
		int k = 0;
		if (driverSpriteOptionItem.getSelectedOption() == 0)
			k |= 2;
		if (bikeSpriteOptionItem.getSelectedOption() == 0)
			k |= 1;
		return k;
	}

	public void _intIV(int k) {
		bikeSpriteOptionItem.setSelectedOption(1);
		driverSpriteOptionItem.setSelectedOption(1);
		if ((k & 1) > 0)
			bikeSpriteOptionItem.setSelectedOption(0);
		if ((k & 2) > 0)
			driverSpriteOptionItem.setSelectedOption(0);
	}

	/*public int _ovI() {
		return levelSelector.getSelectedOption();
	}

	public int _nvI() {
		return trackSelector.getSelectedOption();
	}

	public int _lvI() {
		return leagueSelector.getSelectedOption();
	}*/

	public void setLastTrackTime(long l) {
		lastTrackTime = l;
	}

	/*private byte[] readNameChars(int pos, byte defaultValue) {
		switch (pos) {
			case 16: // '\020'
				byte[] abyte0 = new byte[3];
				for (int l = 0; l < 3; l++)
					abyte0[l] = settings[16 + l];

				if (abyte0[0] == -127)
					abyte0[0] = defaultValue;
				return abyte0;
		}
		return null;
	}

	private byte readSetting(int index, byte defaultValue) {
		if (settings[index] == -127)
			return defaultValue;
		else
			return settings[index];
	}

	private void saveNameChars(int pos, byte[] chars) {
		if (settingsLoadedOK && pos == 16) {
			for (int l = 0; l < 3; l++)
				settings[16 + l] = chars[l];

		}
	}*/

	private String getDurationString(long l) {
		m_ajI = (int) (l / 100L);
		m_atI = (int) (l % 100L);
		String s;
		if (m_ajI / 60 < 10)
			s = " 0" + m_ajI / 60;
		else
			s = " " + m_ajI / 60;
		if (m_ajI % 60 < 10)
			s = s + ":0" + m_ajI % 60;
		else
			s = s + ":" + m_ajI % 60;
		if (m_atI < 10)
			s = s + ".0" + m_atI;
		else
			s = s + "." + m_atI;
		return s;
	}

	/*private void setSetting(int k, byte byte0) {
		if (settingsLoadedOK)
			settings[k] = byte0;
	}*/

	/**
	 * Map a saved-seconds value (0/5/10/15/30) back to its index in
	 * {@link Settings#CONTROLLER_AUTOHIDE_TIMEOUT_VALUES} for display in the
	 * options menu. Falls through to index 0 (Never) for unknown values so
	 * a corrupt or future preference doesn't crash the menu.
	 */
	private static int controllerAutohideIndexFromSeconds(int seconds) {
		int[] vals = Settings.CONTROLLER_AUTOHIDE_TIMEOUT_VALUES;
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] == seconds) return i;
		}
		return 0;
	}

	/** Inverse of {@link #controllerAutohideIndexFromSeconds(int)}. */
	private static int controllerAutohideSecondsFromIndex(int index) {
		int[] vals = Settings.CONTROLLER_AUTOHIDE_TIMEOUT_VALUES;
		if (index < 0 || index >= vals.length) return vals[0];
		return vals[index];
	}

	/**
	 * Map a saved gradient-steps value (one of 2/3/4/6/8/12) back to its
	 * index in {@link Settings#GRADIENT_STEPS_OPTIONS}. Falls through to
	 * the index of the default (6) for unknown values so a corrupt or
	 * future preference doesn't crash the menu.
	 */
	private static int gradientStepsIndexFromN(int n) {
		int[] vals = Settings.GRADIENT_STEPS_OPTIONS;
		int defaultIdx = 0;
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] == n) return i;
			if (vals[i] == 6) defaultIdx = i;
		}
		return defaultIdx;
	}

	/** Inverse of {@link #gradientStepsIndexFromN(int)}. */
	private static int gradientStepsNFromIndex(int index) {
		int[] vals = Settings.GRADIENT_STEPS_OPTIONS;
		if (index < 0 || index >= vals.length) return 6;
		return vals[index];
	}

	/**
	 * Map a saved deadzone-percent value back to its index in
	 * {@link Settings#STICK_DEADZONE_PCT_VALUES}. Falls through to the
	 * Default index for unknown values.
	 */
	private static int stickDeadzoneIndexFromPct(int pct) {
		int[] vals = Settings.STICK_DEADZONE_PCT_VALUES;
		for (int i = 0; i < vals.length; i++) {
			if (vals[i] == pct) return i;
		}
		// 1 == Default in the values array; preserves existing feel if a
		// future / corrupted preference value would otherwise crash.
		return 1;
	}

	/** Inverse of {@link #stickDeadzoneIndexFromPct(int)}. */
	private static int stickDeadzonePctFromIndex(int index) {
		int[] vals = Settings.STICK_DEADZONE_PCT_VALUES;
		if (index < 0 || index >= vals.length) return vals[1];
		return vals[index];
	}

	private void resetAll() {
		Settings.resetAll();
		getLevelsManager().resetAllLevelsSettings();
		getLevelsManager().clearAllHighScores();

		getGDActivity().fullResetting = true;
		getGDActivity().destroyApp(true);
	}

	public void removeCommands() {
		getGDView().removeCommand(okCommand);
		getGDView().removeCommand(backCommand);
	}

	public void addCommands() {
		if (currentMenu != mainMenu && currentMenu != finishedMenu && currentMenu != null)
			getGDView().addCommand(backCommand);
		getGDView().addCommand(okCommand);
	}

	/*private int _bbII(int k) {
		String[] as = RecordStore.listRecordStores();
		if (saveManager == null || as == null)
			return 0;
		int l = 0;
		for (int i1 = 0; i1 < as.length; i1++)
			if (as[i1].startsWith("" + k))
				l++;

		return l;
	}*/

	/*public boolean isKeyboardEnabled() {
		return keyboardInMenuEnabled == 0;
	}*/

	// public boolean isVibrateOnTouchEnabled() {
	//	return vibrateOnTouchEnabled == 0;
	//}

	/*public void hideKeyboard(boolean firstRun) {
		if (!Settings.isKeyboardInMenuEnabled()) {
			getGDActivity().hideKeyboardLayout();
			// MenuScreen.setSize(getGDView().getScaledWidth(), getGDView().getScaledHeight());
		} else if (firstRun) {
			getGDActivity().showKeyboardLayout();
		}*//*else {
			// MenuScreen.setSize(getGDView().getScaledWidth(), getGDView().getScaledHeight() - getGDActivity().getButtonsLayoutHeight());
		}*//*
	}

	public void showKeyboard() {
		getGDActivity().showKeyboardLayout();
		// MenuScreen.setSize(getGDView().getScaledWidth(), getGDView().getScaledHeight() - getGDActivity().getButtonsLayoutHeight());
	}*/

	public void installFromFileBrowse() {
		// Pre-scoped-storage this opened a custom FileDialog rooted at
		// Environment.getExternalStorageDirectory(). Modern replacement: SAF
		// single-file picker (ACTION_OPEN_DOCUMENT) via GDActivity, which
		// hands us a cache-dir temp file we then feed to the existing
		// installAsync() path. Same prompt-for-name UX as the original.
		final GDActivity gd = getGDActivity();
		gd.requestMrgFile(new GDActivity.MrgFilePickedCallback() {
			@Override
			public void onPicked(final File tempFile, String displayName) {
				// Default name: the picked file's display name minus ".mrg".
				// Keeps parity with the old flow which fell back to file.getName().
				String defaultName = displayName != null ? displayName : tempFile.getName();
				if (defaultName.toLowerCase().endsWith(".mrg")) {
					defaultName = defaultName.substring(0, defaultName.length() - 4);
				}

				final EditText input = new EditText(gd);
				input.setInputType(InputType.TYPE_CLASS_TEXT);
				input.setText(defaultName);

				makeAlertBuilder(gd)
						.setTitle(getString(R.string.enter_levels_name_title))
						.setMessage(getString(R.string.enter_levels_name))
						.setView(input)
						.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int whichButton) {
								String name = input.getText().toString();
								if (name.equals("")) name = tempFile.getName();

								// SAF storage may not be set up yet — gate the install on the
								// folder picker the same way downloadLevel does.
								final String finalName = name;
								gd.requestLevelsFolderIfNeeded(new Runnable() {
									@Override
									public void run() {
										gd.levelsManager.installAsync(tempFile, finalName, "", 0, new DoubleCallback() {
											@Override
											public void onDone(Object... objects) {
												tempFile.delete();
												gd.levelsManager.showSuccessfullyInstalledDialog();
											}
											@Override
											public void onFail() {
												tempFile.delete();
												// installAsync already showed an error dialog.
											}
										});
									}
								});
							}
						})
						.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								tempFile.delete();
							}
						})
						.setOnCancelListener(new DialogInterface.OnCancelListener() {
							@Override
							public void onCancel(DialogInterface dialog) {
								tempFile.delete();
							}
						})
						.show();
			}
		});
	}

	/**
	 * Walk the SAF folder for any {@code .mrg} files the DB doesn't know
	 * about and adopt them. Triggered from the manager-screen entry; lets
	 * the user copy a level pack into the chosen folder via a file manager
	 * and have it appear in-game without a fresh install flow.
	 *
	 * <p>Gated on having a folder picked. The scan itself is fast for any
	 * realistic number of files, so we run it on the UI thread and just
	 * show an alert with the result.
	 */
	public void rescanLevelsFolder() {
		final GDActivity gd = getGDActivity();
		gd.requestLevelsFolderIfNeeded(new Runnable() {
			@Override
			public void run() {
				final org.happysanta.gd.Storage.LevelsManager.ScanResult result =
						gd.levelsManager.scanFolder();
				String msg;
				if (result.added == 0) {
					msg = getString(R.string.rescan_none);
				} else if (result.added == 1) {
					msg = getString(R.string.rescan_one);
				} else {
					msg = String.format(getString(R.string.rescan_many), result.added);
				}
				showAlert(getString(R.string.rescan_folder), msg, new Runnable() {
					@Override
					public void run() {
						// Refresh the installed-levels list so newly-adopted
						// rows show up immediately.
						managerInstalledScreen.reloadLevels();
						// Then, if any existing files have content-shifted
						// out from under us, ask the user what to do. We
						// stack this *after* the "found N new" alert so the
						// two questions don't blur into each other.
						if (!result.changed.isEmpty()) {
							promptReplaceChanged(result.changed);
						}
					}
				});
			}
		});
	}

	/**
	 * Show a single Replace/Skip dialog listing all rows whose on-disk file
	 * has been swapped for different content since we last hashed it. v1
	 * intentionally treats it as all-or-nothing — per-file choice would mean
	 * a dialog per row, which is annoying when (e.g.) the user batch-updated
	 * their whole levels folder. If they want finer control they can rename
	 * files first.
	 */
	private void promptReplaceChanged(final java.util.List<org.happysanta.gd.Storage.LevelsManager.ChangedLevel> changed) {
		final GDActivity gd = getGDActivity();
		StringBuilder list = new StringBuilder();
		for (int i = 0; i < changed.size(); i++) {
			if (i > 0) list.append('\n');
			list.append("• ").append(changed.get(i).level.getName());
		}
		String msg = String.format(getString(R.string.changed_files_message), list.toString());
		// Positive button = the safe, non-destructive choice (Keep progress).
		// Material convention: the default button shouldn't wipe user data.
		// Both options bump the stored hash so we don't re-flag the same
		// files on the next rescan / launch — the difference is whether
		// scores and unlocks survive.
		makeAlertBuilder(gd)
				.setTitle(getString(R.string.changed_files_title))
				.setMessage(msg)
				.setPositiveButton(getString(R.string.keep_progress),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								gd.levelsManager.acknowledgeChanged(changed);
								managerInstalledScreen.reloadLevels();
							}
						})
				.setNegativeButton(getString(R.string.reset_progress),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								gd.levelsManager.applyChanged(changed);
								managerInstalledScreen.reloadLevels();
							}
						})
				.show();
	}

	public static boolean isNameCheat(byte[] chars) {
		return chars[0] == 82 && chars[1] == 75 && chars[2] == 69;
	}

}
