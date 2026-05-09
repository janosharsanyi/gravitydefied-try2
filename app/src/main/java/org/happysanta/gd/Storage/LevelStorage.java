package org.happysanta.gd.Storage;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * User-visible, SAF-backed storage for downloaded {@code {id}.mrg} level
 * packs. The user picks a folder once via
 * {@link Intent#ACTION_OPEN_DOCUMENT_TREE} (anywhere on internal/external
 * storage, SD card, etc.), and we persist the tree URI with
 * {@link ContentResolver#takePersistableUriPermission(Uri, int)} so it
 * survives reboot and reinstall.
 *
 * <p>Why SAF: dropping {@code WRITE_EXTERNAL_STORAGE} (Android 6+ scoped
 * storage) means raw {@code /storage/emulated/0/...} paths no longer work.
 * SAF keeps the files user-visible (any file explorer can see them, and
 * the user chose where they live) without needing the Play-restricted
 * {@code MANAGE_EXTERNAL_STORAGE} permission.
 *
 * <p>Files are named {@code {id}.mrg} where {@code id} is the level row's
 * primary key in {@link LevelsDataSource}. The built-in {@code levels.mrg}
 * (id == 1) is bundled in {@code assets/} and lives outside this storage
 * — see {@link AssetLevelSource}.
 *
 * <p>This class only owns the tree URI + per-file lookups. Launching the
 * picker {@link Intent} belongs to the activity (it owns the
 * {@code ActivityResultLauncher}). Use {@link #createPickerIntent()} to
 * build the intent and {@link #setLocation(Uri)} when the result arrives.
 */
public class LevelStorage {

	private static final String PREFS_NAME = "level_storage";
	private static final String KEY_TREE_URI = "tree_uri";
	private static final String MIME_MRG = "application/octet-stream";

	private final Context context;
	private final SharedPreferences prefs;

	public LevelStorage(Context context) {
		this.context = context.getApplicationContext();
		this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
	}

	// --- Location management --------------------------------------------------

	/** Has the user picked a folder yet? */
	public boolean hasLocation() {
		return getLocation() != null;
	}

	/** The persisted tree URI, or {@code null} if the user hasn't picked one. */
	public Uri getLocation() {
		String uriString = prefs.getString(KEY_TREE_URI, null);
		return uriString != null ? Uri.parse(uriString) : null;
	}

	/**
	 * Build the intent to launch the SAF folder picker. The activity is
	 * expected to fire this via an {@link androidx.activity.result.ActivityResultLauncher}
	 * and call {@link #setLocation(Uri)} with the result.
	 */
	public static Intent createPickerIntent() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
				| Intent.FLAG_GRANT_WRITE_URI_PERMISSION
				| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		return intent;
	}

	/**
	 * Persist a freshly-picked tree URI: takes persistable read/write
	 * permission and stores the URI in {@link SharedPreferences}.
	 */
	public void setLocation(Uri treeUri) {
		int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
				| Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
		context.getContentResolver().takePersistableUriPermission(treeUri, flags);
		prefs.edit().putString(KEY_TREE_URI, treeUri.toString()).apply();
	}

	// --- Per-level file ops ---------------------------------------------------

	/** True if {@code {id}.mrg} exists in the chosen folder. */
	public boolean hasLevel(long id) {
		DocumentFile file = findLevel(id);
		return file != null && file.exists();
	}

	/**
	 * Open {@code {id}.mrg} for reading. Throws {@link FileNotFoundException}
	 * if no folder is chosen or the file isn't there.
	 */
	public InputStream openLevel(long id) throws IOException {
		DocumentFile file = findLevel(id);
		if (file == null || !file.exists()) {
			throw new FileNotFoundException("Level " + id + ".mrg not in storage");
		}
		InputStream in = context.getContentResolver().openInputStream(file.getUri());
		if (in == null) {
			throw new IOException("ContentResolver returned null for " + file.getUri());
		}
		return in;
	}

	/**
	 * Create {@code {id}.mrg} in the chosen folder (overwriting if present)
	 * and return an {@link OutputStream} to write into. Caller closes it.
	 */
	public OutputStream createLevel(long id) throws IOException {
		DocumentFile tree = requireTree();
		String name = fileNameFor(id);

		// Overwrite-by-delete-and-create — DocumentFile has no truncating
		// "create or open" primitive, and concatenating onto an existing
		// .mrg would corrupt it.
		DocumentFile existing = tree.findFile(name);
		if (existing != null) {
			existing.delete();
		}
		DocumentFile created = tree.createFile(MIME_MRG, name);
		if (created == null) {
			throw new IOException("Could not create " + name + " in " + tree.getUri());
		}
		OutputStream out = context.getContentResolver().openOutputStream(created.getUri());
		if (out == null) {
			throw new IOException("ContentResolver returned null for " + created.getUri());
		}
		return out;
	}

	/** Delete {@code {id}.mrg} if present. No-op if not. */
	public void deleteLevel(long id) {
		DocumentFile file = findLevel(id);
		if (file != null && file.exists()) {
			file.delete();
		}
	}

	// --- Internals ------------------------------------------------------------

	private DocumentFile findLevel(long id) {
		Uri treeUri = getLocation();
		if (treeUri == null) return null;
		DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
		if (tree == null) return null;
		return tree.findFile(fileNameFor(id));
	}

	private DocumentFile requireTree() throws IOException {
		Uri treeUri = getLocation();
		if (treeUri == null) {
			throw new IOException("No level storage folder chosen");
		}
		DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
		if (tree == null || !tree.exists()) {
			throw new IOException("Level storage folder no longer accessible: " + treeUri);
		}
		return tree;
	}

	private static String fileNameFor(long id) {
		return id + ".mrg";
	}
}
