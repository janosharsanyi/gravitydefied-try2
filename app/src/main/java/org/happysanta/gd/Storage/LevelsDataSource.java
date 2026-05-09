package org.happysanta.gd.Storage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.happysanta.gd.Helpers.logDebug;

public class LevelsDataSource {

	private SQLiteDatabase db;
	private LevelsSQLiteOpenHelper dbHelper;

	public LevelsDataSource(Context context) {
		dbHelper = new LevelsSQLiteOpenHelper(context);
	}

	public synchronized void open() throws SQLException {
		db = dbHelper.getWritableDatabase();
	}

	public synchronized void close() {
		dbHelper.close();
	}

	public synchronized Level createLevel(String name, String author, String filename, String hash, int countEasy, int countMedium, int countHard, long addedTs, long installedTs, boolean isDefault, long apiId) {
		ContentValues values = new ContentValues();
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_NAME, name);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_AUTHOR, author);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_FILENAME, filename == null ? "" : filename);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_HASH, hash == null ? "" : hash);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_EASY, countEasy);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_MEDIUM, countMedium);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_HARD, countHard);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_ADDED, addedTs);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_INSTALLED, installedTs);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_IS_DEFAULT, isDefault ? 1 : 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_API_ID, apiId);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_EASY, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_MEDIUM, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_HARD, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_TRACK, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEVEL, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEAGUE, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEVELS, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEAGUES, 0);

		long insertId = db.insert(LevelsSQLiteOpenHelper.TABLE_LEVELS, null, values);
		Cursor cursor = db.query(LevelsSQLiteOpenHelper.TABLE_LEVELS, null,
				LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " = " + insertId,
				null, null, null, null);

		cursor.moveToFirst();

		Level level = cursorToLevel(cursor);
		cursor.close();
		return level;
	}

	public synchronized void deleteLevel(Level level) {
		long id = level.getId();
		db.delete(LevelsSQLiteOpenHelper.TABLE_LEVELS, LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " = " + id, null);
		db.delete(LevelsSQLiteOpenHelper.TABLE_HIGHSCORES, LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_LEVEL_ID + " = " + id, null);
	}

	// This will also reset auto increment counter
	public synchronized void deleteAllLevels() {
		db.delete(LevelsSQLiteOpenHelper.TABLE_LEVELS, null, null);
		db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + LevelsSQLiteOpenHelper.TABLE_LEVELS + "'");
	}

	public synchronized void resetAllLevelsSettings() {
		ContentValues values = new ContentValues();
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_EASY, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_MEDIUM, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_HARD, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEAGUE, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEVEL, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_TRACK, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEAGUES, 0);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEVELS, 0);

		int result = db.update(LevelsSQLiteOpenHelper.TABLE_LEVELS, values, null, null);
		logDebug("LevelsDataSource.resetAllLevelsSettings: result = " + result);
	}

	public synchronized void updateLevel(Level level) {
		ContentValues values = new ContentValues();
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_EASY, level.getUnlockedEasy());
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_MEDIUM, level.getUnlockedMedium());
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_HARD, level.getUnlockedHard());
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEAGUE, level.getSelectedLeague());
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEVEL, level.getSelectedLevel());
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_TRACK, level.getSelectedTrack());
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEAGUES, level.getUnlockedLeagues());
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEVELS, level.getUnlockedLevels());

		// logDebug("LevelsDataSource.updateLevel selectedLeague: " + level.getSelectedLeague());

		db.update(LevelsSQLiteOpenHelper.TABLE_LEVELS, values, LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " = " + level.getId(), null);
	}

	/** Set the on-disk filename for a row. Used by the v1→v2 backfill path. */
	public synchronized void updateFilename(long id, String filename) {
		ContentValues values = new ContentValues();
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_FILENAME, filename == null ? "" : filename);
		db.update(LevelsSQLiteOpenHelper.TABLE_LEVELS, values,
				LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " = " + id, null);
	}

	/**
	 * Set the content hash for a row. Used by the rescan path to backfill a
	 * v2-era empty hash without disturbing scores or unlocks.
	 */
	public synchronized void updateHash(long id, String hash) {
		ContentValues values = new ContentValues();
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_HASH, hash == null ? "" : hash);
		db.update(LevelsSQLiteOpenHelper.TABLE_LEVELS, values,
				LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " = " + id, null);
	}

	/**
	 * Acknowledge new file contents under an existing filename without
	 * wiping the user's progress: refresh the cached header (counts +
	 * install timestamp) and bump the stored hash so we stop asking, but
	 * keep highscores rows and unlock bitmasks intact.
	 *
	 * <p>Used when the user picks "Keep progress" on a content-change
	 * prompt — they explicitly opted into letting their old times/unlocks
	 * apply to whatever the new file's tracks are.
	 */
	public synchronized void acknowledgeContent(long id, String hash,
			int countEasy, int countMedium, int countHard, long installedTs) {
		ContentValues values = new ContentValues();
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_HASH, hash == null ? "" : hash);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_EASY, countEasy);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_MEDIUM, countMedium);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_HARD, countHard);
		values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_INSTALLED, installedTs);
		db.update(LevelsSQLiteOpenHelper.TABLE_LEVELS, values,
				LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " = " + id, null);
	}

	/**
	 * Re-bind a row to new file contents under the same filename. Refreshes
	 * the cached header info (counts, hash) and the install timestamp, then
	 * wipes any saved state that was tied to the old level's track layout:
	 * highscores rows for this {@code level_id}, plus the unlock bitmasks.
	 *
	 * <p>Done in a transaction because partial application would leave the
	 * row referring to new tracks but with stale unlocks/scores — which is
	 * exactly the bug we're trying to prevent.
	 */
	public synchronized void replaceLevelContent(long id, String hash,
			int countEasy, int countMedium, int countHard, long installedTs) {
		db.beginTransaction();
		try {
			ContentValues values = new ContentValues();
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_HASH, hash == null ? "" : hash);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_EASY, countEasy);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_MEDIUM, countMedium);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_HARD, countHard);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_INSTALLED, installedTs);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_EASY, 0);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_MEDIUM, 0);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_HARD, 0);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEVELS, 0);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEAGUES, 0);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEVEL, 0);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_TRACK, 0);
			values.put(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEAGUE, 0);
			db.update(LevelsSQLiteOpenHelper.TABLE_LEVELS, values,
					LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " = " + id, null);

			db.delete(LevelsSQLiteOpenHelper.TABLE_HIGHSCORES,
					LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_LEVEL_ID + " = " + id, null);

			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}

	/**
	 * All non-empty filenames currently stored. Used by:
	 * <ul>
	 *   <li>{@code scanFolder()} to skip {@code .mrg} files already known to the DB</li>
	 *   <li>{@code Filenames.uniqueIn(...)} to reserve names against in-flight inserts</li>
	 * </ul>
	 */
	public synchronized java.util.Set<String> getAllFilenames() {
		java.util.HashSet<String> out = new java.util.HashSet<>();
		Cursor cursor = db.query(LevelsSQLiteOpenHelper.TABLE_LEVELS,
				new String[]{LevelsSQLiteOpenHelper.LEVELS_COLUMN_FILENAME},
				LevelsSQLiteOpenHelper.LEVELS_COLUMN_FILENAME + " <> ''",
				null, null, null, null);
		while (cursor.moveToNext()) {
			String f = cursor.getString(0);
			if (f != null && !f.isEmpty()) out.add(f);
		}
		cursor.close();
		return out;
	}

	public synchronized HashMap<Long, Long> findInstalledLevels(ArrayList<Long> apiIds) {
		HashMap<Long, Long> installed = new HashMap<>();

		String[] apiIdsArray = new String[apiIds.size()];
		for (int i = 0; i < apiIdsArray.length; i++) {
			apiIdsArray[i] = apiIds.get(i).toString();
		}

		Cursor cursor = db.rawQuery("SELECT " + LevelsSQLiteOpenHelper.LEVELS_COLUMN_API_ID + ", " + LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " FROM " + LevelsSQLiteOpenHelper.TABLE_LEVELS + " WHERE " + LevelsSQLiteOpenHelper.LEVELS_COLUMN_API_ID + " IN (" + makePlaceholders(apiIdsArray.length) + ")", apiIdsArray);
		cursor.moveToFirst();

		while (!cursor.isAfterLast()) {
			long apiId = cursor.getLong(0),
					id = cursor.getLong(1);
			installed.put(apiId, id);
			cursor.moveToNext();
		}
		cursor.close();

		return installed;
	}

	public synchronized List<Level> getAllLevels() {
		Cursor cursor = db.query(LevelsSQLiteOpenHelper.TABLE_LEVELS, null, null, null, null, null, null);

		List<Level> levels = levelsFromCursor(cursor);
		cursor.close();

		return levels;
	}

	public synchronized List<Level> getLevels(int offset, int count) {
		Cursor cursor = db.query(LevelsSQLiteOpenHelper.TABLE_LEVELS, null, null, null, null, LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " ASC", offset + ", " + count);

		List<Level> levels = levelsFromCursor(cursor);
		cursor.close();

		return levels;
	}

	public synchronized Level getLevel(long id) {
		Cursor cursor = db.query(LevelsSQLiteOpenHelper.TABLE_LEVELS, null, LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID + " = " + id, null, null, null, null);
		cursor.moveToFirst();

		Level level = null;
		if (cursor.getCount() > 0) {
			level = cursorToLevel(cursor);
		}

		cursor.close();
		return level;
	}

	public List<Level> levelsFromCursor(Cursor cursor) {
		cursor.moveToFirst();
		List<Level> levels = new ArrayList<>();
		while (!cursor.isAfterLast()) {
			Level level = cursorToLevel(cursor);
			levels.add(level);
			cursor.moveToNext();
		}
		return levels;
	}

	public synchronized boolean isDefaultLevelCreated() {
		Cursor cursor = db.query(LevelsSQLiteOpenHelper.TABLE_LEVELS, new String[]{LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID}, LevelsSQLiteOpenHelper.LEVELS_COLUMN_IS_DEFAULT + " = 1", null, null, null, null);
		boolean created = cursor.getCount() > 0;
		cursor.close();
		return created;
	}

	public synchronized boolean isApiIdInstalled(long apiId) {
		Cursor cursor = db.query(LevelsSQLiteOpenHelper.TABLE_LEVELS, new String[]{LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID}, LevelsSQLiteOpenHelper.LEVELS_COLUMN_API_ID + " = " + apiId, null, null, null, null);
		boolean installed = cursor.getCount() > 0;
		cursor.close();
		return installed;
	}

	public synchronized HighScores getHighScores(long levelId, int level, int track) {
		Cursor cursor = db.query(LevelsSQLiteOpenHelper.TABLE_HIGHSCORES, null,
				LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_LEVEL_ID + " = " + levelId + " AND " + LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_LEVEL + " = " + level + " AND " + LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_TRACK + " = " + track,
				null, null, null, null);
		cursor.moveToFirst();

		HighScores highScores = new HighScores();
		highScores.setLevelId(levelId);
		highScores.setLevel(level);
		highScores.setTrack(track);
		if (cursor.getCount() > 0)
			fillHighScoresFromCursor(cursor, highScores);
		else {
			long id = createEmptyHighScore(levelId, level, track);
			highScores.setId(id);
		}

		cursor.close();
		return highScores;
	}

	private synchronized long createEmptyHighScore(long levelId, int level, int track) {
		ContentValues values = new ContentValues();
		values.put(LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_LEVEL_ID, levelId);
		values.put(LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_LEVEL, level);
		values.put(LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_TRACK, track);
		for (int league = 0; league < 4; league++) {
			for (int place = 0; place < 3; place++) {
				values.put(LevelsSQLiteOpenHelper.getHighscoresTimeColumn(league, place), 0);
				values.put(LevelsSQLiteOpenHelper.getHighscoresNameColumn(league, place), 0);
			}
		}

		long insertId = db.insert(LevelsSQLiteOpenHelper.TABLE_HIGHSCORES, null, values);
		return insertId;
	}

	public synchronized void updateHighScores(HighScores scores) {
		ContentValues values = new ContentValues();
		for (int league = 0; league < 4; league++) {
			for (int place = 0; place < 3; place++) {
				values.put(LevelsSQLiteOpenHelper.getHighscoresTimeColumn(league, place), scores.getTime(league, place));
				values.put(LevelsSQLiteOpenHelper.getHighscoresNameColumn(league, place), scores.getName(league, place));
			}
		}

		db.update(LevelsSQLiteOpenHelper.TABLE_HIGHSCORES, values, LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_ID + " = " + scores.getId(), null);
	}

	public synchronized void clearHighScores(long levelId) {
		db.delete(LevelsSQLiteOpenHelper.TABLE_HIGHSCORES,
				levelId > 0 ? LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_LEVEL_ID + " = " + levelId : null,
				null);
		if (levelId == 0) {
			db.execSQL("DELETE FROM SQLITE_SEQUENCE WHERE NAME = '" + LevelsSQLiteOpenHelper.TABLE_HIGHSCORES + "'");
		}
	}

	private Level cursorToLevel(Cursor cursor) {
		Level level = new Level();
		level.setId(cursor.getLong(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_ID)));
		level.setName(cursor.getString(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_NAME)));
		level.setAuthor(cursor.getString(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_AUTHOR)));
		int filenameIdx = cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_FILENAME);
		// Defensive: column may briefly not exist on a freshly-upgraded DB if
		// onUpgrade hasn't run for some reason. Treat as empty in that case.
		level.setFilename(filenameIdx >= 0 ? cursor.getString(filenameIdx) : "");
		int hashIdx = cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_HASH);
		level.setHash(hashIdx >= 0 ? cursor.getString(hashIdx) : "");
		level.setCount(
				cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_EASY)),
				cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_MEDIUM)),
				cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_COUNT_HARD)));
		level.setAddedTs(cursor.getLong(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_ADDED)));
		level.setInstalledTs(cursor.getLong(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_INSTALLED)));
		level.setIsDefault(cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_IS_DEFAULT)) == 1);
		level.setApiId(cursor.getLong(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_API_ID)));
		level.setUnlocked(
				cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_EASY)),
				cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_MEDIUM)),
				cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_HARD)));
		level.setSelectedLevel(cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEVEL)));
		level.setSelectedTrack(cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_TRACK)));
		level.setSelectedLeague(cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_SELECTED_LEAGUE)));
		level.setUnlockedLevels(cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEVELS)));
		level.setUnlockedLeagues(cursor.getInt(cursor.getColumnIndex(LevelsSQLiteOpenHelper.LEVELS_COLUMN_UNLOCKED_LEAGUES)));

		return level;
	}

	private void fillHighScoresFromCursor(Cursor cursor, HighScores highScores) {
		highScores.setId(cursor.getLong(cursor.getColumnIndex(LevelsSQLiteOpenHelper.HIGHSCORES_COLUMN_ID)));

		for (int league = 0; league < 4; league++) {
			for (int place = 0; place < 3; place++) {
				highScores.setTime(league, place, cursor.getLong(cursor.getColumnIndex(LevelsSQLiteOpenHelper.getHighscoresTimeColumn(league, place))));
				highScores.setName(league, place, cursor.getString(cursor.getColumnIndex(LevelsSQLiteOpenHelper.getHighscoresNameColumn(league, place))));
			}
		}
	}

	private String makePlaceholders(int len) {
		if (len < 1) {
			throw new RuntimeException("No placeholders");
		} else {
			StringBuilder sb = new StringBuilder(len * 2 - 1);
			sb.append("?");
			for (int i = 1; i < len; i++) {
				sb.append(",?");
			}
			return sb.toString();
		}
	}

}
