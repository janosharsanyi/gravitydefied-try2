package org.happysanta.gd;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.Html;
import android.view.*;
import android.widget.FrameLayout;

import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.WindowInsetsCompat;

import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.happysanta.gd.API.*;
import org.happysanta.gd.Game.*;
import org.happysanta.gd.Levels.Loader;
import org.happysanta.gd.Menu.Views.MenuHelmetView;
import org.happysanta.gd.Menu.Views.MenuImageView;
import org.happysanta.gd.Menu.Views.MenuLinearLayout;
import org.happysanta.gd.Menu.Views.MenuTextView;
import org.happysanta.gd.Menu.Views.MenuTitleLinearLayout;
import org.happysanta.gd.Menu.Views.ObservableScrollView;
import org.happysanta.gd.Storage.LevelsManager;
import org.happysanta.gd.Storage.LevelStorage;
// Installation is in this same package (org.happysanta.gd) — no import needed.
// Originally org.acra.util.Installation, replaced when ACRA was dropped.
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;

import static org.happysanta.gd.Helpers.logDebug;

public class GDActivity extends ComponentActivity implements Runnable {

    public static GDActivity shared = null;
    public static final int MENU_TITLE_LAYOUT_TOP_PADDING = 25;
    public static final int MENU_TITLE_LAYOUT_BOTTOM_PADDING = 13;
    public static final int MENU_TITLE_LAYOUT_X_PADDING = 30;
    public static final int MENU_TITLE_FONT_SIZE = 30;
    public static final int GAME_MENU_BUTTON_LAYOUT_WIDTH = 40;
    public static final int GAME_MENU_BUTTON_LAYOUT_HEIGHT = 56;

    private static final long IMAGES_DELAY = 1000L;
    private static final long IMAGES_DELAY_DEBUG = 100L;

    public int m_longI = 0;

    private boolean wasPaused = false;
    private boolean wasStarted = false;
    private boolean wasDestroyed = false;
    private boolean restartingStarted = false;
    public boolean alive = false;
    public boolean m_cZ = true;
    private boolean menuShown = false;
    /**
     * Back-press callback registered with {@link androidx.activity.OnBackPressedDispatcher}.
     * We toggle its {@code enabled} flag in {@link #updateBackCallbackEnabled()}:
     * disabled at the main menu so the system handles back natively (we get
     * the OS predictive-back peek animation and a standard activity finish);
     * enabled everywhere else so we intercept and navigate within the menu.
     * The "abrupt exit" that used to result at root is mitigated by hooking
     * {@link #onPause()} — when {@code isFinishing()}, we kick off the
     * legacy game-loop drain immediately so cleanup happens during the
     * predictive-back animation rather than after.
     */
    private OnBackPressedCallback backCallback;
    public boolean fullResetting = false;
    public boolean exiting = false;

    public GameView gameView = null;
    // public MenuView menuView = null;
    public Loader levelLoader;
    public Physics physEngine;
    public org.happysanta.gd.Menu.Menu menu;
    public boolean m_caseZ;
    public int m_nullI;
    public long m_forJ;
    // public long seconds;
    public long startedTime = 0;
    public long finishedTime = 0;
    public long pausedTime = 0;
    public long pausedTimeStarted = 0;
    public long m_byteJ;
    public boolean inited = false;
    public boolean m_ifZ;
    private Thread thread;
    private MenuImageView menuBtn;
    public MenuTitleLinearLayout titleLayout;
    public ObservableScrollView scrollView;
    private FrameLayout frame;
    private MenuLinearLayout menuLayout;
    private KeyboardController keyboardController;
    private boolean isNormalAndroid = true;
    private boolean buttonCoordsCalculated = false;
    public TextView menuTitleTextView;
    private boolean menuReady = false;
    private ArrayList<Command> commands = new ArrayList<Command>();
    private MenuLinearLayout keyboardLayout;
    private MenuTextView portedTextView;
    private int buttonHeight = 60;
    public LevelsManager levelsManager;

    // SAF folder picker plumbing — see requestLevelsFolderIfNeeded() and
    // onActivityResult() below. Plain Activity has no ActivityResultLauncher,
    // so we use the legacy startActivityForResult / onActivityResult flow.
    private static final int RC_PICK_LEVELS_FOLDER = 0x6FD1; // arbitrary
    private Runnable pendingFolderPickedCallback;

    // Manual .mrg install picker. Replaces the old custom FileDialog rooted
    // at Environment.getExternalStorageDirectory() — both are blocked under
    // scoped storage. Uses ACTION_OPEN_DOCUMENT to get a single content:// URI,
    // then spills the bytes to a cache temp file so the existing
    // LevelsManager.install(File, ...) signature still applies.
    private static final int RC_PICK_MRG_FILE = 0x6FD2;
    private MrgFilePickedCallback pendingMrgFileCallback;

    // "View levels folder" fallback — see openLevelsFolder(). When no app on
    // the device handles ACTION_VIEW on a directory document URI, we re-fire
    // the SAF tree picker scoped to the existing folder via EXTRA_INITIAL_URI
    // so the user can at least browse the contents. Result is intentionally
    // ignored — this is a viewer, not a folder-change action.
    private static final int RC_VIEW_LEVELS_FOLDER = 0x6FD3;

    /**
     * Callback fired when the user picks a {@code .mrg} via the system picker.
     */
    public interface MrgFilePickedCallback {
        /**
         * @param tempFile    cache-dir copy of the picked file — caller owns it
         *                    and is responsible for deleting it when done.
         * @param displayName the original file's display name (e.g. "fun-pack.mrg"),
         *                    or null if the system didn't expose one.
         */
        void onPicked(File tempFile, String displayName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        shared = this;

        // Modern back-press handling. Replaces the deprecated
        // Activity.onBackPressed() override and lets us drop the
        // `enableOnBackInvokedCallback="false"` opt-out from the manifest.
        //
        // Enabled state is toggled by {@link #updateBackCallbackEnabled()}:
        // disabled at the main menu so the system handles back natively
        // (we get the OS predictive-back peek animation, drawn against
        // whatever activity is behind ours), enabled elsewhere so we
        // intercept and navigate within the menu / open the in-game menu.
        //
        // We deliberately do NOT implement the progress callbacks
        // (handleOnBackStarted/Progressed/Cancelled). Overriding any of
        // them would make this callback consume the gesture even at root,
        // which suppresses the system peek animation. The only way to keep
        // the system animation is to not intercept.
        //
        // The "abrupt exit" that used to result from system-direct finish()
        // is mitigated in onPause(): when isFinishing() we kick off the
        // legacy game-loop drain immediately so resource cleanup runs
        // concurrently with the OS predictive-back animation, not after.
        //
        // Initial state: enabled — pre-init back presses get swallowed
        // (same as the old override), since there's no game/menu state.
        backCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (gameView != null && menu != null && inited) {
                    if (menuShown)
                        menu.back();
                    else
                        gameView.showMenu();
                }
                // Pre-init: swallow silently — same as the old override.
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backCallback);

        if (Helpers.isSDK10OrLower()) {
            isNormalAndroid = false;
        }

        final GDActivity self = this;
        Request request = API.getNotifications(Global.INSTALLED_FROM_APK, new ResponseHandler() {
            @Override
            public void onResponse(final Response apiResponse) {
                try {
                    final NotificationsResponse response = new NotificationsResponse(apiResponse);
                    if (!response.isEmpty()) {
                        final Runnable onOk = new Runnable() {
                            @Override
                            public void run() {
                                if (response.hasURL()) {
                                    String url = response.getURL();
                                    try {
                                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                                        startActivity(browserIntent);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                        };

                        if (response.hasTwoButtons()) {
                            AlertDialog.Builder alert = new AlertDialog.Builder(self)
                                    .setTitle(response.getTitle())
                                    .setMessage(response.getMessage())
                                    .setPositiveButton(response.getOKButton(), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            onOk.run();
                                        }
                                    })
                                    .setNegativeButton(response.getCancelButton(), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {

                                        }
                                    });
                            alert.show();
                        } else {
                            AlertDialog alertDialog = new AlertDialog.Builder(self)
                                    .setTitle(response.getTitle())
                                    .setMessage(response.getMessage())
                                    .setPositiveButton(response.getOKButton(), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            onOk.run();
                                        }
                                    })
                                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                        @Override
                                        public void onCancel(DialogInterface dialog) {
                                        }
                                    })
                                    .create();
                            alertDialog.show();
                        }
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                }
            }

            @Override
            public void onError(APIException error) {

            }
        });

        if (true) {
            gameView = new GameView(this);

            // Edge-to-edge is enforced for targetSdk >= 35, so the activity
            // draws under the system bars by default. The original code used
            // the deprecated FLAG_FULLSCREEN (status bar only); we keep the
            // nav bar visible and instead inset the root content view by the
            // system bar insets (see frame setup below) so neither game nor
            // menus draw under status / nav bars.

            scrollView = new ObservableScrollView(this);
            scrollView.setBackgroundColor(0x00ffffff);
            scrollView.setFillViewport(true);
            scrollView.setOnScrollListener(new ObservableScrollView.OnScrollListener() {
                @Override
                public void onScroll(ObservableScrollView scrollView, int x, int y, int oldx, int oldy) {
                    if (isMenuShown() && menu != null && menu.currentMenu != null) {
                        int h = scrollView.getChildAt(0).getHeight() - scrollView.getHeight();
                        double p = 100.0 * y / h;
                        if (p > 100f)
                            p = 100f;

                        menu.currentMenu.onScroll(p);
                    }
                }
            });
            scrollView.setVisibility(View.GONE);

            frame = new FrameLayout(this);
            frame.setBackgroundColor(0xffffffff);

            // Inset the root frame by the system bars so neither game nor
            // menus draw under the status bar (top) or nav bar (bottom).
            // Required since edge-to-edge is enforced for targetSdk >= 35.
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(frame, (v, insets) -> {
                androidx.core.graphics.Insets bars =
                        insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                return insets;
            });

            titleLayout = new MenuTitleLinearLayout(this);
            titleLayout.setBackgroundColor(0x00ffffff);
            titleLayout.setGravity(Gravity.TOP);
            titleLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            titleLayout.setPadding(Helpers.getDp(MENU_TITLE_LAYOUT_X_PADDING), Helpers.getDp(MENU_TITLE_LAYOUT_TOP_PADDING), Helpers.getDp(MENU_TITLE_LAYOUT_X_PADDING), Helpers.getDp(MENU_TITLE_LAYOUT_BOTTOM_PADDING));

            menuTitleTextView = new TextView(this);
            menuTitleTextView.setText(getString(R.string.main));
            menuTitleTextView.setTextColor(0xff000000);
            menuTitleTextView.setTypeface(Global.robotoCondensedTypeface);
            menuTitleTextView.setTextSize(MENU_TITLE_FONT_SIZE);
            menuTitleTextView.setLineSpacing(0f, 1.1f);
            menuTitleTextView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            ));
            menuTitleTextView.setVisibility(android.view.View.GONE);

            titleLayout.addView(menuTitleTextView);

            scrollView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1));

            // Keyboard
            if (getString(R.string.screen_type).equals("tablet")) {
                buttonHeight = 85;
            } else if (Global.density < 1.5) {
                buttonHeight = 55;
            }

            keyboardController = new KeyboardController(this);

            // Outer container — children (cluster views) handle touches via
            // their own listeners, so this one must NOT intercept.
            keyboardLayout = new MenuLinearLayout(this, false);
            rebuildKeypad();

            hideKeyboardLayout();

            menuBtn = new MenuImageView(this);
            menuBtn.setImageResource(R.drawable.ic_menu);
            menuBtn.setScaleType(ImageView.ScaleType.CENTER);
            menuBtn.setLayoutParams(new FrameLayout.LayoutParams(Helpers.getDp(GAME_MENU_BUTTON_LAYOUT_WIDTH), Helpers.getDp(GAME_MENU_BUTTON_LAYOUT_HEIGHT), Gravity.RIGHT | Gravity.TOP));
            menuBtn.setOnClickListener(new android.view.View.OnClickListener() {
                @Override
                public void onClick(android.view.View v) {
                    gameView.showMenu();
                }
            });
            menuBtn.setVisibility(android.view.View.GONE);

            menuLayout = new MenuLinearLayout(this);
            menuLayout.setOrientation(LinearLayout.VERTICAL);
            menuLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            ));

            portedTextView = new MenuTextView(this);
            portedTextView.setTypeface(Global.robotoCondensedTypeface);
            portedTextView.setTextSize(15);
            portedTextView.setLineSpacing(0f, 1.2f);
            portedTextView.setText(Html.fromHtml(getString(R.string.ported_text)));
            portedTextView.setGravity(Gravity.CENTER);
            portedTextView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));
            portedTextView.setPadding(0, 0, 0, Helpers.getDp(10));

            menuLayout.addView(titleLayout);
            menuLayout.addView(scrollView);

            frame.addView(menuLayout);
            frame.addView(keyboardLayout);
            frame.addView(menuBtn);
            frame.addView(portedTextView);

            gameView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
            frame.addView(gameView, 0);

            setContentView(frame);

            gameView._doIV(1); // flag for 1st image, as I understand..
            thread = null;
            m_caseZ = false;
            m_nullI = 2;
            m_forJ = 0;
            m_byteJ = 0;
            inited = false;
            m_ifZ = false;
            wasDestroyed = false;
            restartingStarted = false;

            frame.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                @Override
                public boolean onPreDraw() {
                    frame.getViewTreeObserver().removeOnPreDrawListener(this);
                    // setButtonsLayoutHeight();
                    doStart();
                    return true;
                }
            });



		/* gameView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
			@Override
			public boolean onPreDraw() {
				gameView.getViewTreeObserver().removeOnPreDrawListener(this);
				doStart();
				return true;
			}
		}); */

			/* alive = true;
			m_cZ = false;

			Thread.currentThread().setName("main_thread");

			if (thread == null) {
				thread = new Thread(this);
				thread.setName("game_thread");
			} */

			/*synchronized (thread) {
				thread.start();
				try {
					thread.wait();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}

			for (int i = 1; i <= 4; i++) {
				menu.load(i);
			}

			wasStarted = true;*/
        }
    }

    protected void doStart() {
        alive = true;
        m_cZ = false;

        Thread.currentThread().setName("main_thread");

        if (thread == null) {
            thread = new Thread(this);
            thread.setName("game_thread");
            thread.start();
        }

        wasStarted = true;
    }

    // protected boolean viewDone = false;

    @Override
    public void run() {
        Helpers.logDebug("!!! run()");
        long l1;

        if (!inited) {
            Helpers.logDebug("run(): initing");
            try {
                // Game view
				/* gameView = new GameView(shared);
				gameView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT, 1));
				frame.addView(gameView, 0); */

				/* gameView._doIV(1);
				thread = null;
				m_caseZ = false;
				m_nullI = 2;
				m_forJ = 0L;
				seconds = 0L;
				m_byteJ = 0L;
				inited = false;
				m_ifZ = false; */

                long imageDelay = Global.DEBUG ? IMAGES_DELAY_DEBUG : IMAGES_DELAY; // delay of first image
                Thread.yield();

				/*gameView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw() {
						gameView.getViewTreeObserver().removeOnPreDrawListener(this);
						viewDone = true;
						logDebug("gameView is ready");
						//doStart();
						return true;
					}
				});

				logDebug("before while..");
				while (!viewDone) {
					// Thread.sleep(1);
				}
				logDebug("after while..");*/

                // do we really need this?!
				/*while (gameView == null || gameView.getParent() == null) {
					try {
						Thread.sleep(100);
					} catch (Exception x) {}
				}*/

                MenuHelmetView.clearStaticFields();

                levelsManager = new LevelsManager();
                // If the active level's file was swapped while the app was
                // off, ask the user (Keep/Reset) BEFORE we build the loader
                // and menu — both consume currentLevel's counts/unlocks, and
                // we want them to see the post-decision state. Blocks this
                // bg init thread until the user answers.
                levelsManager.resolvePendingLoadChangeBlocking();
                try {
                    levelLoader = new Loader(levelsManager.getCurrentLevelSource());
                } catch (IOException e) {
                    e.printStackTrace();
                    // logDebug("Reset level id now");
                    levelsManager.resetId();
                    levelsManager.reload();

                    levelLoader = new Loader(levelsManager.getCurrentLevelSource());
                }

                physEngine = new Physics(levelLoader);
                gameView.setPhysicsEngine(physEngine);

                // logDebug(levelsManager.getLevelsStat());
                sendStats();

				/* synchronized (Thread.currentThread()) {
					Thread.currentThread().notify();
				} */
                menu = new org.happysanta.gd.Menu.Menu();
                // menu = null;
                // menu.hideKeyboard();
                for (int i = 1; i <= 4; i++) {
                    menu.load(i);
                }

                // menu = new Menu();
                // menu.hideKeyboard();

				/*menu.load(1);
				menu.load(2);
				menu.load(3);

				Runnable createMenuRunnable = new Runnable() {
					@Override
					public void run() {
						menu.load(4);
						synchronized (this) {
							notify();
						}
					}
				};

				synchronized (createMenuRunnable) {
					// logDebug("before runOnUiThread()");
					runOnUiThread(createMenuRunnable);
					try {
						// logDebug("before wait()");
						createMenuRunnable.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}*/

                portedTextView.setVisibility(View.VISIBLE);

                gameView.setMenu(menu);
                gameView._doIIV(-50, 150);
                setMode(1);

                // Show first image
                Helpers.logDebug("show first image");
                long l2;
                for (; imageDelay > 0L; imageDelay -= l2)
                    l2 = _avJ();

                // Show second image
                portedTextView.setVisibility(View.GONE);
                Helpers.logDebug("show second image");
                imageDelay = Global.DEBUG ? IMAGES_DELAY_DEBUG : IMAGES_DELAY;
                gameView._doIV(2);
                long l3;
                for (long l4 = imageDelay; l4 > 0L; l4 -= l3)
                    l3 = _avJ();

                while (m_longI < 10)
                    _avJ();

                gameView._doIV(0);
                Helpers.logDebug("images DONE");
                inited = true;

            } catch (Exception _ex) {
                _ex.printStackTrace();
                // Log.w("GDTR", _ex);
                throw new RuntimeException(_ex);
            }
        }

        // logDebug("inited, continue");

        restart(false);
        // logDebug("showMenu() now");

        /*if (menu != null) */
        menu.showMenu(0);
        if (/*menu != null && */menu.canStartTrack())
            restart(true);

        l1 = 0L;

        // try {
        Helpers.logDebug("start main loop");
        while (alive) {
			/*if (!alive) {
				logDebug("!alive");
				break;
			}*/

            // try {
            if (physEngine._bytevI() != menu._jvI()) {
                int j = gameView._intII(menu._jvI());
                physEngine._doIV(j);
                menu._intIV(j);
            }

            if (menuShown) {
                menu.showMenu(1);
                if (menu.canStartTrack())
                    restart(true);
            }

            for (int i1 = m_nullI; i1 > 0 && alive; i1--) {
			/* if (m_ifZ)
				seconds += 20L; */
                if (m_forJ == 0L)
                    m_forJ = System.currentTimeMillis();
                int k = 0;
                if (/*physEngine != null && */(k = physEngine._dovI()) == 3 && m_byteJ == 0L) {
                    m_byteJ = System.currentTimeMillis() + 3000L;
                    gameView.showInfoMessage(getString(R.string.crashed), 3000);
                    //m_di.postInvalidate();
                    //m_di.serviceRepaints();
                }
                if (m_byteJ != 0L && m_byteJ < System.currentTimeMillis())
                    restart(true);
                if (k == 5) {
                    finishedTime = System.currentTimeMillis();
                    gameView.showInfoMessage(getString(R.string.crashed), 3000);
                    //m_di.postInvalidate();
                    //m_di.serviceRepaints();
                    try {
                        long l2 = 1000L;
                        if (m_byteJ > 0L)
                            l2 = Math.min(m_byteJ - System.currentTimeMillis(), 1000L);
                        if (l2 > 0L)
                            Thread.sleep(l2);
                    } catch (InterruptedException _ex) {
                    }
                    restart(true);
                } else if (k == 4) {
                    // logDebug("k == 4");
                    m_forJ = 0;
                    // seconds = 0;
                    startedTime = 0;
                    finishedTime = 0;
                    pausedTime = 0;
                } else if (k == 1 || k == 2) {
                    finishedTime = System.currentTimeMillis();
                    // logDebug("game-run: k = " + k);
				/* if (k == 2)
					seconds -= 10L; */
                    goalLoop();
                    // menu.setLastTrackTime(seconds / 10L);
                    menu.setLastTrackTime((finishedTime - startedTime) / 10);
                    menu.showMenu(2);

                    if (menu.canStartTrack())
                        restart(true);
                    if (!alive) {
                        Helpers.logDebug("!alive (2)");
                        break;
                    }
                }
                m_ifZ = k != 4;
                if (m_ifZ && startedTime == 0) {
                    startedTime = System.currentTimeMillis();
                }
            }

            if (!alive) {
                Helpers.logDebug("!alive (3)");
                break;
            }

            //try {
            /*if (physEngine != null)*/
            physEngine._charvV();
            long l;
            if ((l = System.currentTimeMillis()) - l1 < 30L) {
                try {
                    synchronized (this) {
                        wait(Math.max(30L - (l - l1), 1L));
                    }
                } catch (InterruptedException interruptedexception) {
                }
                l1 = System.currentTimeMillis();
            } else {
                l1 = l;
            }
            //m_di.postInvalidate();
		/*} catch (Exception exception) {
			exception.printStackTrace();
		}*/
        }
        // } catch (Exception e) {
        //	e.printStackTrace();
        //}

        Helpers.logDebug("game thread finished, destroyApp(false) next");

        // finish();
        destroyApp(false);
        // return;
    }

    @Override
    protected void onResume() {
        Helpers.logDebug("@@@ [GDActivity \"+hashCode()+\"] onResume()");
        super.onResume();
        Helpers.logDebug("[GDActivity \"+hashCode()+\"] onResume(), inited = " + inited);
        if (wasPaused && wasStarted) {
            // logDebug("onResume(): wasPaused && wasResumed");
            // start();
            m_cZ = false;
            wasPaused = false;

            // Menu.HelmetRotation.start();

            // Resume from background = the user might have swapped the
            // active level's .mrg via a file manager while we weren't
            // looking. The Loader and menu hold cached pointers / names
            // from the old file — playing a track now would index the
            // stale offsets into the new bytes (= garbage, likely crash).
            // checkActiveLevelOnResume() runs the SAF read off the UI
            // thread, prompts Keep/Reset on mismatch, and triggers a
            // restartApp() on either decision so all caches rebuild.
            if (levelsManager != null) {
                levelsManager.checkActiveLevelOnResume();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onPause()");

        wasPaused = true;
        m_cZ = true;
        Helpers.logDebug("inited : " + inited);
        if (!menuShown && inited)
            gameToMenu();

        // menu.helmetRotationStop();
        // Menu.HelmetRotation.stop();
        // if (menu != null)
        // 	menu.saveAll();
        // levelsManager.updateLevelSettings();

        // Activity is going away (back at main menu, or someone else
        // called finish()). Kick off the legacy graceful exit chain NOW
        // rather than waiting for onDestroy. Reason: when the system
        // handles back at root it draws the predictive-back peek
        // animation between onPause and onDestroy — running cleanup here
        // means resource teardown overlaps the animation, instead of the
        // activity window snapping away with a still-active game thread
        // behind it. By the time onDestroy fires, destroyApp is a no-op
        // (wasDestroyed=true), so nothing runs twice.
        if (isFinishing()) {
            exiting = true;
            destroyApp(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onDestroy()");
        destroyApp(false);
    }

    @Override
    protected void onStop() {
        super.onStop();
        Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onStop()");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onStart()");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onRestart()");
    }

    /**
     * Make sure the SAF levels folder is picked, then run {@code onReady}.
     * If the user has already picked a folder in a prior session, the
     * callback runs immediately. Otherwise we launch the system folder
     * picker and run the callback once the URI is persisted.
     *
     * <p>Cancelling the picker drops the callback silently.
     */
    public void requestLevelsFolderIfNeeded(Runnable onReady) {
        LevelStorage storage = levelsManager.getStorage();
        if (storage.hasLocation()) {
            onReady.run();
            return;
        }
        pendingFolderPickedCallback = onReady;
        startActivityForResult(LevelStorage.createPickerIntent(), RC_PICK_LEVELS_FOLDER);
    }

    /**
     * Force the SAF folder picker even if one is already chosen — for the
     * "Change levels folder" menu entry. The new URI replaces the old one
     * via {@link LevelStorage#setLocation(Uri)} (which also releases the
     * prior permission). Existing {@code {id}.mrg} files in the old folder
     * stay where they are; the user is responsible for moving them if they
     * want them in the new location. {@code onReady} runs only on success.
     */
    public void pickNewLevelsFolder(Runnable onReady) {
        pendingFolderPickedCallback = onReady;
        startActivityForResult(LevelStorage.createPickerIntent(), RC_PICK_LEVELS_FOLDER);
    }

    /**
     * Try to open the chosen levels folder in a file-manager-like app.
     * Shows a friendly alert if no folder is set yet. If no app on this
     * device handles directory {@code ACTION_VIEW} (stock AOSP doesn't
     * ship a directory viewer), falls back to firing the SAF tree picker
     * scoped to the existing folder via {@code EXTRA_INITIAL_URI} — the
     * picker isn't a viewer, but it's part of the system (always present
     * on API 21+) and shows the folder contents. The user can browse and
     * back out; we ignore any folder they pick (this entry is a viewer,
     * not a re-pick action).
     */
    public void openLevelsFolder() {
        LevelStorage storage = levelsManager.getStorage();
        Intent viewIntent = storage.createViewFolderIntent();
        if (viewIntent == null) {
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.no_levels_folder))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }
        try {
            startActivity(viewIntent);
            return;
        } catch (android.content.ActivityNotFoundException e) {
            // Fall through to SAF picker fallback below.
        }

        Intent fallback = LevelStorage.createPickerIntent();
        // EXTRA_INITIAL_URI is honored on API 26+; harmless on older. It scopes
        // the picker to the existing folder so the user lands on the contents.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            fallback.putExtra(android.provider.DocumentsContract.EXTRA_INITIAL_URI, storage.getLocation());
        }
        try {
            startActivityForResult(fallback, RC_VIEW_LEVELS_FOLDER);
        } catch (android.content.ActivityNotFoundException e) {
            // Genuinely impossible on a real Android device (the SAF picker
            // is part of system DocumentsUI), but be defensive.
            new AlertDialog.Builder(this)
                    .setMessage(getString(R.string.no_file_manager))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        }
    }

    /**
     * Launch the SAF single-file picker so the user can choose a {@code .mrg}
     * from anywhere on the device. The picker hands back a {@code content://}
     * URI; we copy its bytes into a cache-dir temp file and pass that to the
     * callback (so callers can keep the existing {@code File}-based install
     * path). Cancelling the picker drops the callback silently.
     *
     * <p>MIME type is {@code *&#47;*} because {@code .mrg} isn't a registered
     * MIME — restricting would hide valid files. Validity is enforced later
     * by {@code LevelsManager.install} via the header check.
     */
    public void requestMrgFile(MrgFilePickedCallback callback) {
        pendingMrgFileCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, RC_PICK_MRG_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_PICK_LEVELS_FOLDER) {
            Runnable callback = pendingFolderPickedCallback;
            pendingFolderPickedCallback = null;

            if (resultCode != RESULT_OK || data == null || data.getData() == null) {
                // User cancelled or system returned no URI — no further action.
                return;
            }
            levelsManager.getStorage().setLocation(data.getData());
            if (callback != null) callback.run();
            return;
        }

        if (requestCode == RC_VIEW_LEVELS_FOLDER) {
            // View-only fallback — ignore whatever the user picked. We don't
            // want to silently change the levels folder when they only asked
            // to look at it. "Change levels folder" is a separate menu entry.
            return;
        }

        if (requestCode == RC_PICK_MRG_FILE) {
            MrgFilePickedCallback callback = pendingMrgFileCallback;
            pendingMrgFileCallback = null;

            if (resultCode != RESULT_OK || data == null || data.getData() == null || callback == null) {
                return;
            }
            Uri uri = data.getData();
            String displayName = queryDisplayName(uri);
            try {
                File tempFile = copyUriToCache(uri);
                callback.onPicked(tempFile, displayName);
            } catch (IOException e) {
                e.printStackTrace();
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.error))
                        .setMessage("Could not read picked file: " + e.getMessage())
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
        }
    }

    /**
     * Look up DISPLAY_NAME for a content URI, or null on miss / non-content URI.
     */
    private String queryDisplayName(Uri uri) {
        if (uri == null || !"content".equals(uri.getScheme())) return null;
        try (Cursor cursor = getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) return cursor.getString(idx);
            }
        } catch (Exception ignore) {
            // Some providers throw on metadata queries; fall through to null.
        }
        return null;
    }

    /**
     * Spill a content URI to a unique cache-dir file. Caller deletes when done.
     */
    private File copyUriToCache(Uri uri) throws IOException {
        File temp = File.createTempFile("manual-", ".mrg", getCacheDir());
        try (InputStream in = getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(temp)) {
            if (in == null) {
                throw new IOException("ContentResolver returned null for " + uri);
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
        return temp;
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        menu.clear();
        int id = 1;
        for (Command cmd : commands) {
            MenuItem item = menu.add(0, id, 0, cmd.title);
            id++;
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        gameView.commandAction(commands.get(item.getItemId() - 1));
        return true;
    }

    public void setMode(int j) {
        physEngine._byteIV(j);
    }

    public boolean isMenuShown() {
        return menuShown;
    }

    // @UiThread
    public void setMenu(final LinearLayout layout) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                scrollView.removeAllViews();
                if (layout.getParent() != null) {
                    ((ViewManager) layout.getParent()).removeView(layout);
                }
                scrollView.addView(layout);
            }
        });
    }

    public void goalLoop() {
        if (!alive) {
            return;
        }

        long l1 = 0L;
        if (!physEngine.m_NZ)
            gameView.showInfoMessage(getString(R.string.wheelie), 1000);
        else
            gameView.showInfoMessage(getString(R.string.finished1), 1000);
        for (long l2 = System.currentTimeMillis() + 1000L; l2 > System.currentTimeMillis(); gameView.postInvalidate()) {
            if (menuShown) {
                //m_di.postInvalidate();
                return;
            }
            for (int j = m_nullI; j > 0; j--)
                if (physEngine._dovI() == 5)
                    try {
                        long l3;
                        if ((l3 = l2 - System.currentTimeMillis()) > 0L)
                            Thread.sleep(l3);
                        return;
                    } catch (InterruptedException _ex) {
                        return;
                    }

            physEngine._charvV();
            long l;
            if ((l = System.currentTimeMillis()) - l1 < 30L) {
                try {
                    synchronized (this) {
                        wait(Math.max(30L - (l - l1), 1L));
                    }
                } catch (InterruptedException interruptedexception) {
                }
                l1 = System.currentTimeMillis();
            } else {
                l1 = l;
            }
        }
    }

    public void restart(boolean flag) {
        // logDebug("[GDActivity] restart()");
        if (!alive) {
            return;
        }

        physEngine._doZV(true);
        // logDebug("[GDActivity] restart(): 1");
        m_forJ = 0;
        // seconds = 0;
        startedTime = 0;
        finishedTime = 0;
        pausedTime = 0;
        m_byteJ = 0;
        if (flag)
            gameView.showInfoMessage(levelLoader.getLevelName(menu.getSelectedLevel(), menu.getSelectedTrack()), 3000);
        // logDebug("[GDActivity] restart(): 2");
        gameView._casevV();
        // logDebug("[GDActivity] restart(): 3");
    }

    public void destroyApp(final boolean restart) {
        if (wasDestroyed) {
            return;
        }

        wasDestroyed = true;
        alive = false;

        final GDActivity self = this;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Helpers.logDebug("[GDActivity " + self.hashCode() + "] destroyApp()");
                inited = false;
                m_caseZ = true;

                synchronized (gameView) {
                    destroyResources();

                    if (exiting || restart) {
                        finish();
                    }

                    if (restart) {
                        doRestartApp();
                    }
                }
            }
        });
    }

    private void destroyResources() {
        Helpers.logDebug("[GDActivity " + hashCode() + "]  destroyResources()");

        // if (thread != null) thread.interrupt();
        if (gameView != null) gameView.destroy();

        menuShown = false;
        if (menu != null) {
            if (!fullResetting) menu.saveAll();
            menu.destroy();
        }

        if (levelsManager != null) levelsManager.closeDataSource();
    }

    public int getButtonsLayoutHeight() {
        return buttonHeight * 3 + KeyboardController.PADDING * 2;
    }

    /**
     * How much vertical space at the bottom of the screen the on-screen
     * keypad reserves *along the playfield's centre line* — i.e. how far
     * the playfield's vertical centring should be shifted up to keep the
     * bike out from under the keypad.
     *
     * <ul>
     *   <li>Portrait: full keypad-strip height. The keypad spans the
     *       whole bottom edge, so the centre column loses that much.</li>
     *   <li>Landscape (split-keypad mode): 0. The two clusters dock into
     *       the bottom-left and bottom-right corners with the centre
     *       bottom intentionally clear, so the bike's vertical path is
     *       unblocked and no compensation is needed.</li>
     * </ul>
     *
     * <p>This is what {@code GameView.offsetY()} should subtract to
     * vertically centre the bike in the actually-visible playfield.
     */
    public int getPlayfieldBottomReservation() {
        boolean landscape = getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        return landscape ? 0 : getButtonsLayoutHeight();
    }

    public boolean isKeyboardLayoutVisible() {
        return keyboardLayout != null
                && keyboardLayout.getVisibility() == android.view.View.VISIBLE;
    }

    /**
     * Build a single cluster sub-view from a {@code keys} grid of J2ME-style
     * key numbers (1-9; use 0 for "no key" cells, though no current cluster
     * uses that). The view is a vertical {@link LinearLayout} of horizontal
     * rows; each cell is a square button. The view intercepts its own touch
     * events and routes them through
     * {@link KeyboardController#makeTouchListener(int[][])}, which hit-tests
     * against the same {@code keys} grid.
     */
    private MenuLinearLayout buildClusterView(int[][] keys) {
        MenuLinearLayout cluster = new MenuLinearLayout(this, true);
        cluster.setOrientation(LinearLayout.VERTICAL);
        int rows = keys.length;
        // Allow jagged grids — col count is the widest row; missing cells
        // in shorter rows render as empty (no-key) placeholders so hit-test
        // geometry stays aligned with the visible buttons.
        int cols = 0;
        for (int[] r : keys) cols = Math.max(cols, r.length);
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(this);
            row.setPadding(
                    Helpers.getDp(KeyboardController.PADDING),
                    r == 0 ? Helpers.getDp(KeyboardController.PADDING) : 0,
                    Helpers.getDp(KeyboardController.PADDING),
                    0);
            row.setOrientation(LinearLayout.HORIZONTAL);
            // Background ~60% white (was ~78% / 0xc6) — reduces how much of
            // the playfield the keypad visually obscures.
            row.setBackgroundColor(0x99ffffff);
            for (int c = 0; c < cols; c++) {
                int j2me = (c < keys[r].length) ? keys[r][c] : 0;
                LinearLayout btn = new LinearLayout(this);
                if (j2me >= 1 && j2me <= 9) {
                    TextView btnText = new TextView(this);
                    btnText.setText(String.valueOf(j2me));
                    btnText.setTextColor(0xff000000);
                    btnText.setTextSize(17);
                    btn.setBackgroundResource(getResources().getIdentifier("btn_n", "drawable", getPackageName()));
                    btn.addView(btnText, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    btn.setGravity(Gravity.CENTER);
                    btn.setWeightSum(1);
                    keyboardController.registerButton(btn, j2me);
                }
                row.addView(btn, new LinearLayout.LayoutParams(Helpers.getDp(buttonHeight), Helpers.getDp(buttonHeight), 1));
            }
            cluster.addView(row);
        }
        cluster.setOnTouchListener(keyboardController.makeTouchListener(keys));
        return cluster;
    }

    /**
     * Cluster definitions for landscape "split-keypad" mode. Indexed by
     * {@link Settings#getInputOption()} (0/1/2 → keysets 1/2/3). Each entry
     * is a {@code {clusterA, clusterB}} pair; cluster A defaults to the left
     * side of the screen when {@link Settings#getKeypadLandscapeSide()} is
     * {@link Settings#KEYPAD_SIDE_NORMAL} (the default), swapped when set to
     * {@link Settings#KEYPAD_SIDE_FLIPPED}. Spec from user:
     * keyset 1: A = 1 2 3 / 7 8 9, B = 4 / 6
     * keyset 2: A = 1 / 4,         B = 6 / 5
     * keyset 3: A = 5 / 4,         B = 3 / 6
     */
    private static final int[][][][] LANDSCAPE_CLUSTERS_BY_KEYSET = {
            {new int[][]{{1, 2, 3}, {7, 8, 9}}, new int[][]{{4, 5, 6}}},
            {new int[][]{{1, 2}, {4, 8}}, new int[][]{{5, 6}}},
            {new int[][]{{4, 5}}, new int[][]{{2, 3}, {8, 6}}},
    };

    /**
     * Tear down and rebuild {@link #keyboardLayout}'s contents. Call this
     * whenever orientation, input keyset ({@link Settings#getInputOption()}),
     * or landscape side ({@link Settings#getKeypadLandscapeSide()}) changes.
     *
     * <p>Layouts:
     * <ul>
     *   <li>Portrait: single 3×3 grid (legacy keypad).</li>
     *   <li>Landscape: two clusters docked to opposite bottom edges with a
     *       weight-1 spacer between them. Mapping per keyset is from
     *       {@link #LANDSCAPE_CLUSTERS_BY_KEYSET}.</li>
     * </ul>
     */
    public void rebuildKeypad() {
        if (keyboardLayout == null) return;
        keyboardLayout.removeAllViews();

        boolean landscape = getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        int keyset = Settings.getInputOption();
        int[][][] clusters = (landscape && keyset >= 0 && keyset < LANDSCAPE_CLUSTERS_BY_KEYSET.length)
                ? LANDSCAPE_CLUSTERS_BY_KEYSET[keyset]
                : (int[][][]) null;

        if (clusters != null) {
            // Split mode: container is a horizontal strip across the
            // bottom; cluster A and B sit at opposite edges with a
            // weight-1 spacer between, so the playfield centre stays clear.
            keyboardLayout.setOrientation(LinearLayout.HORIZONTAL);
            keyboardLayout.setGravity(Gravity.BOTTOM);
            boolean swap = Settings.getKeypadLandscapeSide() == Settings.KEYPAD_SIDE_FLIPPED;
            int[][] leftKeys = swap ? clusters[1] : clusters[0];
            int[][] rightKeys = swap ? clusters[0] : clusters[1];
            MenuLinearLayout left = buildClusterView(leftKeys);
            MenuLinearLayout right = buildClusterView(rightKeys);
            keyboardLayout.addView(left,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            View spacer = new View(this);
            keyboardLayout.addView(spacer,
                    new LinearLayout.LayoutParams(0, 1, 1));
            keyboardLayout.addView(right,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        } else {
            // Portrait (or fallback): legacy single 3×3 grid filling the
            // container width.
            keyboardLayout.setOrientation(LinearLayout.VERTICAL);
            keyboardLayout.setGravity(Gravity.BOTTOM);
            MenuLinearLayout grid = buildClusterView(KeyboardController.FULL_GRID);
            keyboardLayout.addView(grid,
                    new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        }

        applyKeypadOrientation();
    }

    /**
     * Configure {@link #keyboardLayout}'s outer placement based on the
     * current device orientation. In both portrait and landscape (split)
     * modes the container is full-width, bottom-anchored — internal layout
     * (single grid vs. two clusters with spacer) is set up by
     * {@link #rebuildKeypad()}.
     *
     * @return true if landscape (caller can use this to update related
     * layouts like the scroll view margin)
     */
    public boolean applyKeypadOrientation() {
        if (keyboardLayout == null) return false;
        boolean landscape = getResources().getConfiguration().orientation
                == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
        keyboardLayout.setPadding(0, 0, 0, Helpers.getDp(KeyboardController.PADDING));
        keyboardLayout.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM));
        return landscape;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Helpers.logDebug("@@@ [GDActivity " + hashCode() + "] onConfigurationChanged() orientation="
                + newConfig.orientation);
        // We declared orientation|screenSize|screenLayout in the manifest's
        // configChanges, so Android calls us instead of recreating the
        // activity on rotation. Rebuild the keypad (single grid vs. split
        // clusters); everything else (game thread, menu, GameView) keys off
        // live measured dimensions and adapts naturally.
        rebuildKeypad();
        // If the menu's currently visible scroll view has a margin from
        // showKeyboardLayout(), refresh it to match the new keypad position.
        if (isKeyboardLayoutVisible()) {
            showKeyboardLayout();
        }
    }

    // @UiThread
    public void hideKeyboardLayout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                keyboardLayout.setVisibility(android.view.View.GONE);

                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scrollView.getLayoutParams();
                params.setMargins(0, 0, 0, 0);
                scrollView.setLayoutParams(params);
            }
        });
    }

    // @UiThread
    public void showKeyboardLayout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                keyboardLayout.setVisibility(android.view.View.VISIBLE);

                // Inset the menu scroll view so its items don't sit under
                // the keypad. The keypad is now bottom-anchored in both
                // orientations — in portrait it's a full strip, in landscape
                // it's two corner clusters with a clear centre — so a
                // bottom margin is the right inset for both. We use the
                // (smaller) cluster height in landscape since that's the
                // keypad's actual height there.
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) scrollView.getLayoutParams();
                boolean landscape = getResources().getConfiguration().orientation
                        == android.content.res.Configuration.ORIENTATION_LANDSCAPE;
                int bottomPx = landscape
                        ? Helpers.getDp(buttonHeight * 2 + KeyboardController.PADDING)
                        : Helpers.getDp(getButtonsLayoutHeight());
                params.setMargins(0, 0, 0, bottomPx);
                scrollView.setLayoutParams(params);
            }
        });
    }

    public void addCommand(Command cmd) {
        if (!commands.contains(cmd))
            commands.add(cmd);
        if (isNormalAndroid)
            invalidateOptionsMenu();
    }

    public void removeCommand(Command cmd) {
        commands.remove(cmd);
        if (isNormalAndroid)
            invalidateOptionsMenu();
    }

    public void gameToMenu() {
        Helpers.logDebug("gameToMenu()");

        if (gameView == null) {
            Helpers.logDebug("gameToMenu(): gameView == null");
            return;
        }

        pausedTimeStarted = System.currentTimeMillis();

        gameView.removeMenuCommand();
        menuShown = true;
        // menu.helmetRotationStart();
        // Menu.HelmetRotation.start();
        if (menu != null)
            menu.addCommands();

        // hideKeyboardLayout();
        if (!Settings.isKeyboardInMenuEnabled())
            hideKeyboardLayout();
        else
            showKeyboardLayout();

        gameToMenuUpdateUi();
        updateBackCallbackEnabled();
    }

    // @UiThread
    protected void gameToMenuUpdateUi() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                menuBtn.setVisibility(android.view.View.GONE);
                menuTitleTextView.setVisibility(android.view.View.VISIBLE);
                scrollView.setVisibility(android.view.View.VISIBLE);
            }
        });
    }

    public void menuToGame() {
        Helpers.logDebug("menuToGame()");

        if (pausedTimeStarted > 0 && startedTime > 0) {
            pausedTime += (System.currentTimeMillis() - pausedTimeStarted);
            pausedTimeStarted = 0;
        }

        if (menu != null) menu.removeCommands();
        menuShown = false;
        // menu.helmetRotationStop();
        // Menu.HelmetRotation.stop();
        if (gameView != null) gameView.addMenuCommand();
        showKeyboardLayout();
        // menu.showKeyboard();

        menuToGameUpdateUi();
        updateBackCallbackEnabled();

        keyboardController.clearLogBuffer();
    }

    /**
     * Recompute the back callback's {@code enabled} state from current UI
     * context. Disabled only at the main menu (so the system runs back
     * natively, drawing the predictive-back peek animation and finishing
     * the activity); enabled everywhere else so we intercept back to
     * navigate within the menu or open the in-game menu. Called from
     * {@link #gameToMenu()}, {@link #menuToGame()}, and
     * {@link Menu#setCurrentMenu(MenuScreen, boolean)} whenever the inputs
     * change.
     *
     * <p>Always posts to the UI thread because
     * {@link OnBackPressedCallback#setEnabled(boolean)} is {@code @MainThread}
     * and our callers (the game thread, etc.) aren't all on it.
     */
    public void updateBackCallbackEnabled() {
        if (backCallback == null) return;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (backCallback == null) return;
                boolean atRoot = inited && menuShown && menu != null && menu.isAtMainMenu();
                backCallback.setEnabled(!atRoot);
            }
        });
    }

    // @UiThread
    protected void menuToGameUpdateUi() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                menuBtn.setVisibility(android.view.View.VISIBLE);
                menuTitleTextView.setVisibility(android.view.View.GONE);
                scrollView.setVisibility(android.view.View.GONE);

                // Clear menu
                scrollView.removeAllViews();
                menuTitleTextView.setText("");
                menu.menuDisabled = true;
                // menu.currentMenu = null;
            }
        });
    }

    public void scrollTextMenuUp() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int y = scrollView.getScrollY();
                scrollView.scrollTo(0, y - Helpers.getDp(20));
            }
        });
    }

    public void scrollTextMenuDown() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int y = scrollView.getScrollY();
                scrollView.scrollTo(0, y + Helpers.getDp(20));
            }
        });
    }

    public void scrollToView(final View view) {
        final GDActivity gd = Helpers.getGDActivity();
        final ObservableScrollView scrollView = gd.scrollView;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Rect scrollBounds = new Rect();
                scrollView.getHitRect(scrollBounds);

                if (!view.getLocalVisibleRect(scrollBounds)
                        || scrollBounds.height() < view.getHeight()) {
                    int top = view.getTop(),
                            height = view.getHeight(),
                            scrollY = scrollView.getScrollY(),
                            scrollHeight = scrollView.getHeight(),
                            y = top;

					/*logDebug("top = " + top);
					logDebug("height = " + height);
					logDebug("scrollY = " + scrollY);
					logDebug("scrollHeight = " + scrollHeight);*/

                    if (top < scrollY) {
                        // scroll to y
                    } else if (top + height > scrollY + scrollHeight) {
                        y = top + height - scrollHeight;
                        if (y < 0)
                            y = 0;
                    }

                    // logDebug("View is not visible, scroll to y = " + y);
                    scrollView.scrollTo(0, y);
                } else {
                    // logDebug("View is visible");
                }
            }
        });
    }

    private long _avJ() {
        m_longI++;
        long l = System.currentTimeMillis();
        if (m_longI < 1 || m_longI > 10) { // maybe < 1 not needed?
            m_longI--;
            try {
                Thread.sleep(100L);
            } catch (InterruptedException _ex) {
            }
        }
        return System.currentTimeMillis() - l;
    }

    public void restartApp() {
        if (!restartingStarted) {
            destroyApp(true);
            restartingStarted = true;
        }
    }

    private void doRestartApp() {
        // Old code scheduled an AlarmManager + PendingIntent to relaunch the activity 100ms
        // after finish(). On modern Android (12+) inexact alarms from background apps are
        // heavily throttled and frequently never fire — symptom: app exits, never restarts.
        // Plain startActivity() with NEW_TASK | CLEAR_TASK works synchronously and is what
        // the AlarmManager hack was emulating anyway.
        Intent restart = new Intent(this, GDActivity.class);
        restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(restart);
    }

    private void sendStats() {
        long lastTs = Settings.getLastSendStats();
        Helpers.logDebug("sendStats: lastTs = " + lastTs);
        if (lastTs == 0) {
            Helpers.logDebug("sendStats: set it to current ts and return");
            Settings.setLastSendStats(Helpers.getTimestamp());
            return;
        }

        // if (Helpers.getTimestamp() < lastTs + 3600 * 8) {
        if (Helpers.getTimestamp() < lastTs + 10) {
            Helpers.logDebug("sendStats: just return");
            return;
        }

        final GDActivity self = this;
        Thread statsThread = new Thread() {
            @Override
            public void run() {
                try {
                    HashMap<String, Double> stats = levelsManager.getLevelsStat();

                    JSONObject statsJSON = new JSONObject(stats);
                    String id = Installation.id(self);
                    int useCheats = org.happysanta.gd.Menu.Menu.isNameCheat(Settings.getName()) ? 1 : 0;

                    API.sendStats(statsJSON.toString(), id, useCheats, new ResponseHandler() {
                        @Override
                        public void onResponse(Response response) {
                            Helpers.logDebug("send stats OK");
                            Settings.setLastSendStats(Helpers.getTimestamp());
                        }

                        @Override
                        public void onError(APIException error) {
                            Helpers.logDebug("send stats error: " + error.getMessage());
                            // logDebug(error);
                            // error.printStackTrace();
                        }
                    });
                } catch (Exception e) {
                    Helpers.logDebug("send stats exception: " + e.getMessage());
                    // e.printStackTrace();
                }
            }
        };
        statsThread.start();
    }

    public void sendKeyboardLogs() {
        final ProgressDialog progressDialog = ProgressDialog.show(this, getString(R.string.please_wait), getString(R.string.please_wait), true);
        API.sendKeyboardLogs(keyboardController.getLog(), new ResponseHandler() {
            @Override
            public void onResponse(Response response) {
                progressDialog.dismiss();
                keyboardController.clearLogBuffer();
                Helpers.showAlert(getString(R.string.ok), "Done", null);
            }

            @Override
            public void onError(APIException error) {
                progressDialog.dismiss();
                Helpers.showAlert(getString(R.string.error), "Unable to send logs. Maybe log is empty?", null);
            }
        });
    }

    private class ButtonCoords {

        public int x = 0;
        public int y = 0;
        public int w = 0;
        public int h = 0;

        public ButtonCoords() {
        }

        public boolean in(float x, float y) {
            if (x < this.x || x > this.x + this.w || y < this.y || y > this.y + this.h) {
                return false;
            }
            return true;
        }

    }

}