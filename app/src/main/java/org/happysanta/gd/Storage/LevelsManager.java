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
import static org.happysanta.gd.Helpers.showAlert;

public class LevelsManager {

	private LevelsDataSource dataSource;
	private LevelStorage storage;
	private boolean dbOK = false;
	private Level currentLevel;

	public LevelsManager() {
		GDActivity gd = getGDActivity();
		dataSource = new LevelsDataSource(gd);
		storage = new LevelStorage(gd);

		try {
			dataSource.open();

			if (!dataSource.isDefaultLevelCreated()) {
				// Default (built-in) level — filename stays empty because
				// we read it from assets, not the SAF tree.
				Level level = dataSource.createLevel("GDTR original", "Codebrew Software", "", 10, 10, 10, 0, 0, true, 1);
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
		dbOK = true;
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

		Level level = dataSource.createLevel(name, author, filename, header.getCount(0), header.getCount(1), header.getCount(2), 0, getTimestamp(), false, apiId);
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

	public void load(Level level) throws RuntimeException {
		/*File file = getMrgFileById(level.getId());
		if (!mrgIsAvailable(level.getId())) {
			throw new RuntimeException("Unable to load levels \"" +level.getName() + "\"");
		}*/

		// Loader loader = getLevelLoader();
		// Menu menu = getGameMenu();

		// loader.setLevelsFile(file);
		// menu.reloadLevels();

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
		AlertDialog success = new AlertDialog.Builder(gd)
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
	 * Walk the SAF folder and add a DB row for each {@code .mrg} file that
	 * isn't already tracked. Returns the number of new rows. Triggered from
	 * the "Rescan folder" menu entry — lets the user drop a {@code .mrg}
	 * into the folder from a file manager and have it show up in-game.
	 *
	 * <p>We read the level header to get the track counts (and validate
	 * that the file is a real {@code .mrg}). Files that don't parse are
	 * skipped silently — surfacing each parse error would be noisy if the
	 * user has unrelated junk in the folder.
	 */
	public synchronized int scanFolder() {
		if (!storage.hasLocation()) return 0;

		java.util.Set<String> known = dataSource.getAllFilenames();
		List<String> onDisk = storage.listMrgFiles();
		int added = 0;
		for (String name : onDisk) {
			if (known.contains(name)) continue;

			LevelHeader header;
			InputStream in = null;
			try {
				in = storage.openLevel(name);
				header = Reader.readHeader(in);
			} catch (Throwable t) {
				logDebug("LevelsManager.scanFolder: skipping " + name + " — " + t);
				continue;
			} finally {
				if (in != null) try { in.close(); } catch (IOException ignore) {}
			}
			if (!header.isCountsOk()) {
				logDebug("LevelsManager.scanFolder: skipping " + name + " — bad counts");
				continue;
			}

			// Display name: strip ".mrg" extension. Author and apiId aren't
			// in the file format, so we leave them empty / 0; the user can
			// see the file is there even without those.
			String displayName = name.substring(0, name.length() - 4);
			Level level = dataSource.createLevel(displayName, "", name,
					header.getCount(0), header.getCount(1), header.getCount(2),
					0, getTimestamp(), false, 0);
			if (level != null && level.getId() > 0) {
				added++;
				logDebug("LevelsManager.scanFolder: added " + name + " as id " + level.getId());
			}
		}
		return added;
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
