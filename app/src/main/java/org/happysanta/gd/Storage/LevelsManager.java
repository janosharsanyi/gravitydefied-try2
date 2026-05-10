package org.happysanta.gd.Storage;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import org.happysanta.gd.API.API;
import org.happysanta.gd.API.DownloadFile;
import org.happysanta.gd.API.DownloadHandler;
import org.happysanta.gd.Callback;
import org.happysanta.gd.DoubleCallback;
import org.happysanta.gd.GDActivity;
import org.happysanta.gd.Global;
import org.happysanta.gd.Levels.LevelHeader;
import org.happysanta.gd.Levels.Reader;
// LevelSource / AssetLevelSource / DocumentLevelSource / LevelStorage are in this same package.
import org.happysanta.gd.Menu.Menu;
import org.happysanta.gd.Menu.MenuScreen;
import org.happysanta.gd.R;
import org.happysanta.gd.Settings;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.happysanta.gd.Helpers.getGDActivity;
import static org.happysanta.gd.Helpers.getGameMenu;
import static org.happysanta.gd.Helpers.getString;
import static org.happysanta.gd.Helpers.getTimestamp;
import static org.happysanta.gd.Helpers.isOnline;
import static org.happysanta.gd.Helpers.logDebug;
import static org.happysanta.gd.Helpers.makeAlertBuilder;
import static org.happysanta.gd.Helpers.showAlert;

public class LevelsManager {

	private LevelsDataSource dataSource;
	private LevelStorage storage;
	private boolean dbOK = false;
	private Level currentLevel;
	/**
	 * Captured during {@link #LevelsManager() construction} when the
	 * active level's on-disk file no longer matches its stored hash —
	 * i.e. the user swapped the file out while the app was off. We can't
	 * show a dialog from the constructor (background thread, no window
	 * yet), so we hold onto the change and let the activity surface it
	 * via {@link #promptPendingLoadChange()} once the menu is up.
	 */
	private ChangedLevel pendingLoadChange = null;
	/**
	 * Re-entrancy guard for {@link #checkActiveLevelOnResume()}: onResume can
	 * fire several times in a row (e.g. dialog dismiss, picker return), and
	 * we don't want to spawn parallel hash threads or stack two prompts.
	 */
	private volatile boolean resumeCheckInFlight = false;

	public LevelsManager() {
		GDActivity gd = getGDActivity();
		dataSource = new LevelsDataSource(gd);
		storage = new LevelStorage(gd);

		try {
			dataSource.open();

			if (!dataSource.isDefaultLevelCreated()) {
				// Default (built-in) level — filename and hash stay empty
				// because we read it from assets, not the SAF tree, and the
				// file never changes out from under us.
				Level level = dataSource.createLevel("GDTR original", "Codebrew Software", "", "", 10, 10, 10, 0, 0, true, 1);
				logDebug("LevelsManager: Default level created!");
				logDebug(level);
			}

			// Best-effort v1→v2 backfill: any row missing a filename gets
			// one derived from its name, and the legacy {id}.mrg in the SAF
			// folder is renamed in place. Safe to run every startup —
			// rows that already have filenames are skipped.
			migrateLegacyFilenames();
		} catch (SQLException e) {
			e.printStackTrace();
			logDebug("LevelsManager: db feels bad :(");
			// return;
		}

		logDebug("LevelsManager: db feels OK :)");

		// Shared prefs
		// SharedPreferences settings = getSharedPreferences();
		// long levelId = settings.getLong(PREFS_LEVEL_ID, 0);
		long levelId = Settings.getLevelId();
		if (levelId < 1 || !mrgIsAvailable(levelId)) {
			logDebug("LevelsManager: levelId = " + levelId + ", < 1 or mrg is not available; now: reset id");
			/*SharedPreferences.Editor editor = settings.edit();
			editor.putLong(PREFS_LEVEL_ID, 1);
			editor.commit();*/
			resetId();
		}

		reload();

		// Launch-time hash check on the active level. If the user swapped
		// the file out between sessions we want to ask before letting any
		// new game time accrue against the old row's scores/unlocks. Same
		// skip rules as load(): default level has no file, empty stored
		// hash means "no baseline to compare to". Errors are non-fatal —
		// the loader path will surface them if the file is unreadable.
		if (currentLevel != null
				&& currentLevel.getId() != 1
				&& currentLevel.getFilename() != null && !currentLevel.getFilename().isEmpty()
				&& currentLevel.getHash() != null && !currentLevel.getHash().isEmpty()) {
			try {
				Fingerprint fp = fingerprint(currentLevel.getFilename());
				if (!currentLevel.getHash().equals(fp.hash)) {
					pendingLoadChange = new ChangedLevel(currentLevel, fp.hash,
							fp.header.getCount(0), fp.header.getCount(1), fp.header.getCount(2));
					logDebug("LevelsManager: active level " + currentLevel.getId()
							+ " has changed on disk — pending dialog");
				}
			} catch (Throwable t) {
				logDebug("LevelsManager: launch hash check failed: " + t);
			}
		}

		dbOK = true;
	}

	/**
	 * If the launch-time hash check flagged the active level's file as
	 * swapped, surface the Keep/Reset dialog and <b>block the calling
	 * thread</b> until the user decides and the resulting DB writes are
	 * applied. Must be called from a non-UI thread (we post the dialog to
	 * the main looper and {@link java.util.concurrent.CountDownLatch#await()}
	 * here).
	 *
	 * <p>Why blocking: this has to run between {@code new LevelsManager()}
	 * and {@code new Loader(...)} / menu construction in the activity's
	 * background init thread. If we let init proceed and then post the
	 * dialog after the menu is up, the menu will already have been built
	 * from stale {@code currentLevel} counts/unlocks — the Reset wipe (or
	 * Keep's count refresh) would land too late to be reflected without
	 * a manual refresh.
	 */
	public void resolvePendingLoadChangeBlocking() {
		final ChangedLevel change = pendingLoadChange;
		if (change == null) return;
		pendingLoadChange = null;

		final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
		// Single-element array so the dialog callbacks (anonymous inner
		// classes) can mutate it. Default = Keep, so any unexpected dismiss
		// path errs on the side of preserving the user's progress.
		final boolean[] reset = { false };

		getGDActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String msg = String.format(getString(R.string.changed_file_load_message),
						change.level.getName());
				makeAlertBuilder(getGDActivity())
						.setTitle(getString(R.string.changed_files_title))
						.setMessage(msg)
						.setPositiveButton(getString(R.string.keep_progress),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										latch.countDown();
									}
								})
						.setNegativeButton(getString(R.string.reset_progress),
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										reset[0] = true;
										latch.countDown();
									}
								})
						.setCancelable(false)
						.show();
			}
		});

		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			// Treat interruption as "Keep" — least destructive default.
		}

		if (reset[0]) {
			applyChanged(java.util.Collections.singletonList(change));
		} else {
			acknowledgeChanged(java.util.Collections.singletonList(change));
		}
	}

	/**
	 * Re-check the active level's on-disk hash whenever the activity comes
	 * back from background. Catches the realistic "user backgrounds the app,
	 * swaps the .mrg in a file manager, returns" workflow.
	 *
	 * <p>This is the non-blocking sibling of
	 * {@link #resolvePendingLoadChangeBlocking()}: at startup we can stall
	 * the bg init thread before the {@link org.happysanta.gd.Levels.Loader}
	 * is built, but on resume the loader and menu are already up and holding
	 * cached pointers / track names from the old file. So once the user
	 * picks Keep or Reset we have to {@link GDActivity#restartApp()} —
	 * a DB-only update would still leave stale {@code Loader.pointers},
	 * {@code Loader.names}, and any cached track listings in place, and the
	 * very next track pick would index into the new file with the old
	 * offsets (= garbage).
	 *
	 * <p>Safe to call from the UI thread (we push the SAF read off to a
	 * worker, then post the dialog back). Safe to call multiple times in
	 * quick succession — guarded by {@link #resumeCheckInFlight}.
	 *
	 * <p>No-op for the bundled level (no file), for rows with no recorded
	 * hash (no baseline to compare to), and on read errors (let the loader
	 * path surface failures — we don't want a transient SAF hiccup to
	 * trigger a Reset/Keep prompt against an unread file).
	 */
	public void checkActiveLevelOnResume() {
		final Level active = currentLevel;
		if (active == null
				|| active.getId() == 1
				|| active.getFilename() == null || active.getFilename().isEmpty()
				|| active.getHash() == null || active.getHash().isEmpty()) {
			return;
		}
		if (resumeCheckInFlight) return;
		resumeCheckInFlight = true;

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					final Fingerprint fp;
					try {
						fp = fingerprint(active.getFilename());
					} catch (Throwable t) {
						logDebug("LevelsManager.checkActiveLevelOnResume: read failed: " + t);
						return;
					}
					if (active.getHash().equals(fp.hash)) return;
					getGDActivity().runOnUiThread(new Runnable() {
						@Override
						public void run() {
							try {
								promptResumeChange(active, fp);
							} finally {
								// Flag is reset only after the prompt is up
								// (or skipped). Once the user picks, restart
								// kicks in and a fresh process will set its
								// own flag from scratch.
								resumeCheckInFlight = false;
							}
						}
					});
				} catch (Throwable outer) {
					// Belt-and-suspenders: never leave the in-flight flag
					// stuck on if something unexpected blows up before we
					// get to the UI-thread post.
					resumeCheckInFlight = false;
					logDebug("LevelsManager.checkActiveLevelOnResume: " + outer);
				}
			}
		}, "GD-resume-hashcheck").start();
	}

	private void promptResumeChange(final Level active, final Fingerprint fp) {
		final ChangedLevel change = new ChangedLevel(active, fp.hash,
				fp.header.getCount(0), fp.header.getCount(1), fp.header.getCount(2));
		String msg = String.format(getString(R.string.changed_file_load_message),
				active.getName());
		makeAlertBuilder(getGDActivity())
				.setTitle(getString(R.string.changed_files_title))
				.setMessage(msg)
				.setPositiveButton(getString(R.string.keep_progress),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								acknowledgeChanged(java.util.Collections.singletonList(change));
								// See method javadoc: Loader and menu hold
								// stale pointers / names — only a process
								// restart rebuilds them against the file
								// that's actually on disk now.
								getGDActivity().restartApp();
							}
						})
				.setNegativeButton(getString(R.string.reset_progress),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								applyChanged(java.util.Collections.singletonList(change));
								getGDActivity().restartApp();
							}
						})
				.setCancelable(false)
				.show();
	}

	public void resetId() {
		Settings.setLevelId(1);
	}

	public void reload() {
		long id = Settings.getLevelId();
		currentLevel = dataSource.getLevel(id);

		if (currentLevel == null) {
			logDebug("LevelsManager: failed to load currentLevel; currentId = " + id);
		} else {
			logDebug("LevelsManager: level = " + currentLevel);
		}

	}

	public void closeDataSource() {
		dataSource.close();
	}

	public long getCurrentId() {
		return currentLevel.getId();
	}

	public void setCurrentId(long id) {
		// currentId = id;
		Settings.setLevelId(id);
		/*SharedPreferences settings = getSharedPreferences();
		SharedPreferences.Editor edit = settings.edit();
		edit.putLong(PREFS_LEVEL_ID, id);
		edit.commit();*/
	}

	public Level getCurrentLevel() {
		return currentLevel;
	}

	/**
	 * Returns the {@link LevelSource} that {@link org.happysanta.gd.Levels.Loader}
	 * should read from for the current level.
	 *
	 * <p>Built-in level (id == 1) → bundled {@code assets/levels.mrg} via
	 * {@link AssetLevelSource}. Downloaded levels (id &gt; 1) → SAF-backed
	 * {@code {id}.mrg} in the user-chosen folder via {@link DocumentLevelSource}.
	 */
	public LevelSource getCurrentLevelSource() {
		if (currentLevel.getId() == 1) {
			return new AssetLevelSource(getGDActivity(), "levels.mrg");
		}
		return new DocumentLevelSource(storage, currentLevel.getFilename());
	}

	public LevelStorage getStorage() {
		return storage;
	}

	private boolean mrgIsAvailable(long id) {
		if (id == 1) // This is default built-in levels.mrg
			return true;

		// Pre-SAF: this checked external storage state + file.exists() on a
		// raw /storage/emulated/0/GDLevels path. Now: ask the storage layer
		// — false if no folder picked, no filename in DB, or the file isn't
		// in the chosen folder.
		Level level = dataSource.getLevel(id);
		if (level == null) return false;
		String filename = level.getFilename();
		if (filename == null || filename.isEmpty()) return false;
		return storage.hasLevel(filename);
	}

	public boolean isDbOK() {
		return dbOK;
	}

	public long install(File file, String name, String author, long apiId) throws Exception {
		if (!isSpaceAvailable(file.length())) {
			throw new Exception(getString(R.string.e_no_space_left));
		}

		InputStream inputStream = new FileInputStream(file);
		LevelHeader header = Reader.readHeader(inputStream);
		try {
			inputStream.close();
		} catch (IOException e) {
		}

		if (!header.isCountsOk()) {
			throw new IOException(file.getName() + " is not valid");
		}

		// Decide on a filename *before* the DB insert so the row carries it
		// from the start. fallbackId == 0 because we don't have the row id
		// yet; sanitizer will only fall back to "level-0" if the entire name
		// got stripped, which we treat as an unusual but valid case (the
		// uniqueness loop will still bump it to "level-0 (2).mrg" etc. on
		// collision).
		String base = Filenames.sanitizeBase(name, 0);
		String filename = Filenames.uniqueIn(storage.getTree(), base, dataSource.getAllFilenames());

		// Hash the source temp file before copy so we can recognize it later
		// if the user swaps it out via a file manager (rescan path uses this
		// to detect "same name, different content").
		String hash;
		try {
			hash = Hashing.sha256(file);
		} catch (IOException e) {
			// Hashing failure shouldn't block install; an empty hash just
			// means the next rescan will silently backfill from disk.
			logDebug("LevelsManager.install: hashing failed: " + e);
			hash = "";
		}

		Level level = dataSource.createLevel(name, author, filename, hash, header.getCount(0), header.getCount(1), header.getCount(2), 0, getTimestamp(), false, apiId);
		long id = level.getId();
		if (id < 1) {
			throw new Exception(getString(R.string.e_cannot_save_level));
		}

		// Copy the downloaded temp file into the SAF-backed user folder
		// under the chosen filename. The source `file` is still a regular
		// File (cache dir); only the destination is via ContentResolver.
		copyToStorage(file, filename);

		return id;
	}

	public void installAsync(File file, String name, String author, long apiId, final DoubleCallback callback) {
		GDActivity gd = getGDActivity();
		final ProgressDialog progressDialog = ProgressDialog.show(gd, getString(R.string.install), getString(R.string.installing), true);

		new AsyncInstallLevel() {
			@Override
			protected void onPostExecute(Object result) {
				progressDialog.dismiss();

				if (result instanceof Throwable) {
					Throwable throwable = (Throwable) result;
					throwable.printStackTrace();
					showAlert(getString(R.string.error), throwable.getMessage(), null);
					if (callback != null)
						callback.onFail();
					return;
				}

				if (callback != null)
					callback.onDone((long) result);
			}
		}.execute(file, name, author, apiId);
	}

	public void load(final Level level) throws RuntimeException {
		// Hash precheck: if the on-disk file no longer matches the hash we
		// recorded at install/scan time, ask the user before binding scores
		// and unlocks to a different level pack. Skip for the bundled level
		// (no file), for rows with no stored hash (legacy / freshly-adopted
		// — adopt silently after load), and if the storage layer can't get
		// us the file at all (let the loader path surface the error).
		boolean canVerify = level != null
				&& level.getId() != 1
				&& level.getFilename() != null && !level.getFilename().isEmpty()
				&& level.getHash() != null && !level.getHash().isEmpty();
		if (!canVerify) {
			doLoad(level);
			return;
		}

		Fingerprint fp;
		try {
			fp = fingerprint(level.getFilename());
		} catch (Throwable t) {
			logDebug("LevelsManager.load: hash precheck failed: " + t);
			doLoad(level);
			return;
		}

		if (level.getHash().equals(fp.hash)) {
			doLoad(level);
			return;
		}

		// Content drifted. Same Keep/Reset choice as the rescan path, but
		// scoped to this single level — the user's intent is "play this
		// level *now*", so we settle the bind question and continue.
		final ChangedLevel change = new ChangedLevel(level, fp.hash,
				fp.header.getCount(0), fp.header.getCount(1), fp.header.getCount(2));
		String msg = String.format(getString(R.string.changed_file_load_message), level.getName());
		makeAlertBuilder(getGDActivity())
				.setTitle(getString(R.string.changed_files_title))
				.setMessage(msg)
				.setPositiveButton(getString(R.string.keep_progress),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								// Acknowledge so we don't keep flagging
								// the same change next launch / rescan.
								acknowledgeChanged(java.util.Collections.singletonList(change));
								doLoad(level);
							}
						})
				.setNegativeButton(getString(R.string.reset_progress),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								applyChanged(java.util.Collections.singletonList(change));
								doLoad(level);
							}
						})
				.setCancelable(false)
				.show();
	}

	private void doLoad(Level level) {
		setCurrentId(level.getId());
		getGDActivity().restartApp();
	}

	public boolean isApiIdInstalled(long apiId) {
		return dataSource.isApiIdInstalled(apiId);
	}

	public Level[] getInstalledLevels(int offset, int count) {
		return dataSource.getLevels(offset, count).toArray(new Level[0]);
	}

	public Level getLeveL(long id) {
		return dataSource.getLevel(id);
	}

	public Level[] getAllInstalledLevels() {
		return dataSource.getAllLevels().toArray(new Level[0]);
	}

	public synchronized HashMap<String, Double> getLevelsStat() {
		Level[] levels = getAllInstalledLevels();
		HashMap<String, Double> stat = new HashMap<>();
		if (levels.length > 0) {
			for (Level level : levels) {
				int[] completed = level.getUnlockedAll();
				int completedCount = 0;
				for (int i = 0; i < completed.length; i++) {
					if (completed[i] < 0) completed[i] = 0;
					completedCount += completed[i];
				}

				double totalCount = level.getCountEasy() + level.getCountMedium() + level.getCountHard();
				double per = completedCount / totalCount * 100;

				stat.put(String.valueOf(level.getApiId()), per);
			}
		}
		return stat;
	}

	public void delete(Level level) {
		// If the user is deleting the currently-active level (today only
		// reachable via the "remove missing level" flow — the normal Delete
		// action is hidden when a level is active), fall back to the bundled
		// default before nuking the row. Otherwise currentLevel would be
		// pointing at a freshly-deleted row, and the next getCurrentId() /
		// getCurrentLevelSource() call would NPE / FileNotFoundException.
		if (currentLevel != null && level.getId() == currentLevel.getId()) {
			resetId();
			reload();
		}

		dataSource.deleteLevel(level);
		try {
			String filename = level.getFilename();
			if (filename != null && !filename.isEmpty()) {
				storage.deleteLevel(filename);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void deleteAsync(Level level, final Runnable callback) {
		GDActivity gd = getGDActivity();
		final ProgressDialog progressDialog = ProgressDialog.show(gd, getString(R.string.delete), getString(R.string.deleting), true);

		new AsyncDeleteLevel() {
			@Override
			protected void onPostExecute(Void v) {
				progressDialog.dismiss();
				if (callback != null)
					callback.run();
			}
		}.execute(level);
	}

	public void updateLevelSettings() {
		dataSource.updateLevel(currentLevel);
	}

	public void downloadLevel(final Level level, final Callback successCallback) {
		final GDActivity gd = getGDActivity();

		// First, make sure we have a SAF folder to write into. If not, ask
		// the user to pick one — the activity launches the picker and
		// re-invokes this method via the callback once the URI is persisted.
		gd.requestLevelsFolderIfNeeded(new Runnable() {
			@Override
			public void run() {
				doDownloadLevel(level, successCallback);
			}
		});
	}

	private void doDownloadLevel(final Level level, final Callback successCallback) {
		final GDActivity gd = getGDActivity();
		File outputDir = gd.getCacheDir();

		try {
			if (!isOnline()) {
				throw new Exception(getString(R.string.e_no_network_connection));
			}

			if (!isSpaceAvailable(level.getSize())) {
				throw new Exception(getString(R.string.e_no_space_left));
			}

			final File outputFile = File.createTempFile("levels" + level.getApiId(), "mrg", outputDir);
			FileOutputStream out = new FileOutputStream(outputFile);

			// logDebug("downloadLevel: 4");
			// final API api = new API();
			final ProgressDialog progress;
			final DownloadFile downloadFile = new DownloadFile(API.getMrgURL(level.getApiId()), out);

			progress = new ProgressDialog(gd);
			progress.setMessage(getString(R.string.downloading));
			progress.setIndeterminate(true);
			progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progress.setCancelable(true);

			final DownloadHandler handler = new DownloadHandler() {
				@Override
				public void onFinish(Throwable error) {
					progress.dismiss();

					if (error != null) {
						// error.printStackTrace();
						error.printStackTrace();
						showAlert(getString(R.string.error), error.getMessage(), null);

						outputFile.delete();
						return;
					}

					// Install
					installAsync(outputFile, level.getName(), level.getAuthor(), level.getApiId(), new DoubleCallback() {
						@Override
						public void onDone(Object... objects) {
							long id = (long) objects[0];
							outputFile.delete();

							if (successCallback != null)
								successCallback.onDone(id);
						}

						@Override
						public void onFail() {
							outputFile.delete();
						}
					});
				}

				@Override
				public void onStart() {
					progress.show();
				}

				@Override
				public void onProgress(int pr) {
					progress.setIndeterminate(false);
					progress.setMax(100);
					progress.setProgress(pr);
				}
			};
			progress.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					downloadFile.cancel();
					handler.onFinish(new InterruptedException(getString(R.string.e_downloading_was_interrupted)));
				}
			});

			downloadFile.setDownloadHandler(handler);
			downloadFile.start();
		} catch (Exception e) {
			showAlert(getString(R.string.error), e.getMessage(), null);
		}
	}

	public void showSuccessfullyInstalledDialog() {
		GDActivity gd = getGDActivity();
		AlertDialog success = makeAlertBuilder(gd)
				.setTitle(getString(R.string.installed))
				.setMessage(getString(R.string.successfully_installed))
				.setPositiveButton(getString(R.string.ok), null)
				.setNegativeButton(getString(R.string.open_installed), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Menu menu = getGameMenu();
						MenuScreen currentMenu = getGameMenu().getCurrentMenu(),
								newMenu = menu.managerInstalledScreen;

						if (currentMenu == menu.managerDownloadScreen || currentMenu.getNavTarget() == menu.managerDownloadScreen) {
							menu.managerDownloadScreen.onHide(menu.managerScreen);
						}

						menu.setCurrentMenu(newMenu, false);
					}
				})
				.create();
		success.show();
	}

	public HashMap<Long, Long> findInstalledLevels(ArrayList<Long> apiIds) {
		return dataSource.findInstalledLevels(apiIds);
	}

	public HighScores getHighScores(int level, int track) {
		HighScores scores = dataSource.getHighScores(currentLevel.getId(), level, track);
		// logDebug("LevelsManager.getHighScores: " + scores);
		return scores;
	}

	public void saveHighScores(HighScores scores) {
		dataSource.updateHighScores(scores);
	}

	public void clearHighScores() {
		dataSource.clearHighScores(currentLevel.getId());
	}

	public void clearAllHighScores() {
		dataSource.clearHighScores(0);
	}

	public void resetAllLevelsSettings() {
		dataSource.resetAllLevelsSettings();

		logDebug("All levels now: " + dataSource.getAllLevels());
		logDebug("Level#1: " + dataSource.getLevel(1));
	}

	/**
	 * Copy a regular {@link File} (cache-dir temp from the downloader) into
	 * the SAF-backed user folder under {@code filename}.
	 */
	private void copyToStorage(File src, String filename) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = storage.createLevel(filename);
		try {
			byte[] buf = new byte[8192];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			try { in.close(); } catch (IOException ignore) {}
			try { out.close(); } catch (IOException ignore) {}
		}
	}

	/**
	 * Backfill {@code filename} for rows created under schema v1, where the
	 * file on disk was named {@code {id}.mrg}. For each such row we pick a
	 * sanitized, unique filename, rename the file in place via SAF (best
	 * effort — if the user has moved the storage folder or hasn't picked
	 * one yet, we just record the desired filename in the DB and let the
	 * next launch / scan / rescan pick up the file once it's reachable).
	 *
	 * <p>Idempotent: skips rows whose filename is already set, and skips
	 * the bundled level (id == 1, no on-disk file).
	 */
	private void migrateLegacyFilenames() {
		List<Level> all = dataSource.getAllLevels();
		java.util.HashSet<String> reserved = new java.util.HashSet<>();
		for (Level l : all) {
			String f = l.getFilename();
			if (f != null && !f.isEmpty()) reserved.add(f);
		}
		androidx.documentfile.provider.DocumentFile tree = storage.getTree();

		for (Level l : all) {
			if (l.getId() == 1) continue;
			String existing = l.getFilename();
			if (existing != null && !existing.isEmpty()) continue;

			String legacyName = l.getId() + ".mrg";
			String base = Filenames.sanitizeBase(l.getName(), l.getId());
			String desired = Filenames.uniqueIn(tree, base, reserved);

			// If the legacy file is actually present, rename it. The provider
			// gets the final say on the post-rename name (it may suffix to
			// avoid collisions on case-insensitive filesystems), so we use
			// whatever name comes back.
			String actual = desired;
			if (tree != null) {
				String renamed = storage.renameLevel(legacyName, desired);
				if (renamed != null) {
					actual = renamed;
				}
				// If renameLevel returned null, the legacy file isn't there.
				// Still record the desired filename so future scans / installs
				// know not to use it.
			}

			dataSource.updateFilename(l.getId(), actual);
			reserved.add(actual);
			logDebug("LevelsManager.migrateLegacyFilenames: row " + l.getId()
					+ " \"" + l.getName() + "\" → " + actual);
		}
	}

	/**
	 * Result of {@link #scanFolder()}: counts of newly-adopted files plus a
	 * list of existing rows whose on-disk content has changed since we last
	 * recorded a hash. Caller (Menu) prompts the user before applying the
	 * changed-file replacements via {@link #applyChanged(List)}.
	 */
	public static class ScanResult {
		public final int added;
		public final List<ChangedLevel> changed;

		ScanResult(int added, List<ChangedLevel> changed) {
			this.added = added;
			this.changed = changed;
		}
	}

	/**
	 * A row whose stored hash no longer matches the on-disk file. Carries
	 * the new header info so {@link #applyChanged(List)} doesn't need to
	 * re-read and re-parse the file.
	 */
	public static class ChangedLevel {
		public final Level level;
		public final String newHash;
		public final int newCountEasy;
		public final int newCountMedium;
		public final int newCountHard;

		ChangedLevel(Level level, String newHash, int e, int m, int h) {
			this.level = level;
			this.newHash = newHash;
			this.newCountEasy = e;
			this.newCountMedium = m;
			this.newCountHard = h;
		}
	}

	/**
	 * Walk the SAF folder and reconcile with the DB. Two outputs:
	 * <ul>
	 *   <li><b>added</b>: count of {@code .mrg} files we hadn't seen before
	 *       and just created rows for.</li>
	 *   <li><b>changed</b>: existing rows whose recorded {@link Level#getHash()}
	 *       no longer matches the on-disk file. These need a user decision
	 *       (replace metadata + wipe scores, or keep the lie) — we don't
	 *       silently re-bind, because the new file could be a totally
	 *       different level pack the user dropped in under the same name.</li>
	 * </ul>
	 *
	 * <p>Files with no stored hash (legacy v2 rows) are silently backfilled
	 * with whatever's on disk — there's no prior fingerprint to compare to,
	 * so flagging them as "changed" would be noise.
	 *
	 * <p>Files that don't parse as {@code .mrg} are skipped silently — the
	 * user might have unrelated junk in the folder.
	 */
	public synchronized ScanResult scanFolder() {
		if (!storage.hasLocation()) return new ScanResult(0, java.util.Collections.<ChangedLevel>emptyList());

		// Build a name → existing-row map so we can spot existing files in
		// one pass without N round-trips to the DB.
		java.util.Map<String, Level> known = new java.util.HashMap<>();
		for (Level l : dataSource.getAllLevels()) {
			String f = l.getFilename();
			if (f != null && !f.isEmpty()) known.put(f, l);
		}

		List<String> onDisk = storage.listMrgFiles();
		int added = 0;
		List<ChangedLevel> changed = new ArrayList<>();

		for (String name : onDisk) {
			Fingerprint fp;
			try {
				fp = fingerprint(name);
			} catch (Throwable t) {
				logDebug("LevelsManager.scanFolder: skipping " + name + " — " + t);
				continue;
			}
			if (!fp.header.isCountsOk()) {
				logDebug("LevelsManager.scanFolder: skipping " + name + " — bad counts");
				continue;
			}

			Level existing = known.get(name);
			if (existing == null) {
				// Brand new file. Display name strips the .mrg extension.
				String displayName = name.substring(0, name.length() - 4);
				Level level = dataSource.createLevel(displayName, "", name, fp.hash,
						fp.header.getCount(0), fp.header.getCount(1), fp.header.getCount(2),
						0, getTimestamp(), false, 0);
				if (level != null && level.getId() > 0) {
					added++;
					logDebug("LevelsManager.scanFolder: added " + name + " as id " + level.getId());
				}
				continue;
			}

			String existingHash = existing.getHash();
			if (existingHash == null || existingHash.isEmpty()) {
				// First time we've seen this row's file post-v3 upgrade.
				// Adopt the current hash silently — no prior fingerprint
				// means no honest way to call this "changed".
				dataSource.updateHash(existing.getId(), fp.hash);
				logDebug("LevelsManager.scanFolder: backfilled hash for " + name + " (id " + existing.getId() + ")");
				continue;
			}

			if (!existingHash.equals(fp.hash)) {
				logDebug("LevelsManager.scanFolder: " + name + " changed (id " + existing.getId() + ")");
				changed.add(new ChangedLevel(existing, fp.hash,
						fp.header.getCount(0), fp.header.getCount(1), fp.header.getCount(2)));
			}
		}
		return new ScanResult(added, changed);
	}

	/**
	 * Header + content hash from a single SAF read. Tees bytes through a
	 * SHA-256 {@link java.security.DigestInputStream} while the header
	 * parser reads from the front of the file, then drains the rest into
	 * a discard buffer to finish the hash. One open instead of two — SAF
	 * IPC dwarfs hashing cost.
	 */
	static class Fingerprint {
		final LevelHeader header;
		final String hash;

		Fingerprint(LevelHeader header, String hash) {
			this.header = header;
			this.hash = hash;
		}
	}

	private Fingerprint fingerprint(String filename) throws IOException {
		InputStream in = storage.openLevel(filename);
		try {
			java.security.MessageDigest md = Hashing.newSha256();
			final java.security.DigestInputStream dis = new java.security.DigestInputStream(in, md);
			// Reader.readHeader closes the stream it's given (Loader depends
			// on that). Shield our DigestInputStream so we can keep reading
			// after the header parse to drain the rest into the digest.
			java.io.FilterInputStream shield = new java.io.FilterInputStream(dis) {
				@Override public void close() { /* no-op */ }
			};
			LevelHeader header = Reader.readHeader(shield);
			byte[] buf = new byte[8192];
			int n;
			while ((n = dis.read(buf)) != -1) {
				// Discard — we only care about feeding the digest.
			}
			return new Fingerprint(header, Hashing.toHex(md.digest()));
		} finally {
			try { in.close(); } catch (IOException ignore) {}
		}
	}

	/**
	 * Apply the user-confirmed "Reset progress" decision to a list of
	 * {@link ChangedLevel}s from a prior {@link #scanFolder()} call. For
	 * each row: refresh hash + counts + install timestamp, and wipe scores
	 * / unlocks (the old data referred to a different track layout).
	 *
	 * <p>If the active level is one of the replaced rows, refresh
	 * {@code currentLevel} from the DB so it reflects the cleared state —
	 * we stay on the same level since the user is presumably about to play
	 * it (the prompt fires *because* they tried to load it or because the
	 * launch path detected the swap).
	 */
	public synchronized void applyChanged(List<ChangedLevel> changes) {
		if (changes == null || changes.isEmpty()) return;
		long now = getTimestamp();
		boolean activeReplaced = false;
		for (ChangedLevel c : changes) {
			dataSource.replaceLevelContent(c.level.getId(), c.newHash,
					c.newCountEasy, c.newCountMedium, c.newCountHard, now);
			if (currentLevel != null && c.level.getId() == currentLevel.getId()) {
				activeReplaced = true;
			}
		}
		if (activeReplaced) {
			// Re-pull from DB so cached counts / cleared unlocks are correct.
			reload();
		}
	}

	/**
	 * Apply the user-confirmed "Keep progress" decision: refresh the
	 * stored hash + cached counts so we don't keep flagging the same
	 * change on every check, but preserve scores and unlocks. The user
	 * has explicitly opted to let their existing progress carry over to
	 * whatever the new file's tracks happen to be.
	 */
	public synchronized void acknowledgeChanged(List<ChangedLevel> changes) {
		if (changes == null || changes.isEmpty()) return;
		long now = getTimestamp();
		boolean activeAcked = false;
		for (ChangedLevel c : changes) {
			dataSource.acknowledgeContent(c.level.getId(), c.newHash,
					c.newCountEasy, c.newCountMedium, c.newCountHard, now);
			if (currentLevel != null && c.level.getId() == currentLevel.getId()) {
				activeAcked = true;
			}
		}
		if (activeAcked) {
			// Refresh cached counts so the listing is consistent with the
			// new file. Unlocks are intentionally preserved.
			reload();
		}
	}

	/**
	 * Pre-SAF this used {@code StatFs} on the GDLevels directory. With SAF
	 * there's no clean way to inspect free space on the chosen tree (the
	 * URI may not even resolve to a real path on the local filesystem).
	 * Stubbed to {@code true}; an out-of-space write will surface as an
	 * {@link IOException} from the actual download/copy.
	 */
	public static boolean isSpaceAvailable(long bytes) {
		return true;
	}

	private class AsyncDeleteLevel extends AsyncTask<Level, Void, Void> {
		@Override
		protected Void doInBackground(Level... levels) {
			delete(levels[0]);
			return null;
		}
	}

	private class AsyncInstallLevel extends AsyncTask<Object, Void, Object> {
		@Override
		protected Object doInBackground(Object... objects) {
			File file = (File) objects[0];
			String name = (String) objects[1];
			String author = (String) objects[2];
			long apiId = (long) objects[3];

			long id = 0;
			try {
				id = install(file, name, author, apiId);
			} catch (Throwable e) {
				return e;
			}

			return id;
		}
	}

}
