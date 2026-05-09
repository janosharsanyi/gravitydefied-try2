package org.happysanta.gd.Storage;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import android.provider.DocumentsContract;

import androidx.documentfile.provider.DocumentFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
 * <p>Files are addressed by filename (e.g. {@code "Crazy Cliffs.mrg"}). The
 * filename is chosen at install time by sanitizing the user-visible name
 * via {@link Filenames#sanitizeBase}, then resolving collisions with
 * {@link Filenames#uniqueIn}. The DB row keeps the chosen filename in
 * {@code LEVELS_COLUMN_FILENAME} so we can find the file on disk again.
 * The built-in {@code levels.mrg} (id == 1) is bundled in {@code assets/}
 * and lives outside this storage — see {@link AssetLevelSource}.
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
	 * permission and stores the URI in {@link SharedPreferences}. If a
	 * different folder was previously persisted, releases that grant so
	 * we don't accumulate stale permissions over time.
	 */
	public void setLocation(Uri treeUri) {
		Uri previous = getLocation();
		int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
				| Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
		ContentResolver resolver = context.getContentResolver();
		resolver.takePersistableUriPermission(treeUri, flags);
		if (previous != null && !previous.equals(treeUri)) {
			try {
				resolver.releasePersistableUriPermission(previous, flags);
			} catch (SecurityException ignore) {
				// Old grant already gone — nothing to release.
			}
		}
		prefs.edit().putString(KEY_TREE_URI, treeUri.toString()).apply();
	}

	/**
	 * Build an intent that asks a file-manager-like app to view the chosen
	 * folder's contents. Only works on devices with an app that handles
	 * {@code ACTION_VIEW} on a directory document URI (Files / Material
	 * Files / etc.). Returns {@code null} if no folder is set.
	 *
	 * <p>Caller should {@code startActivity} inside a try/catch on
	 * {@link android.content.ActivityNotFoundException} since stock Android
	 * has no guaranteed handler.
	 */
	public Intent createViewFolderIntent() {
		Uri treeUri = getLocation();
		if (treeUri == null) return null;
		Uri docUri = DocumentsContract.buildDocumentUriUsingTree(
				treeUri, DocumentsContract.getTreeDocumentId(treeUri));
		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setDataAndType(docUri, DocumentsContract.Document.MIME_TYPE_DIR);
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
				| Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
		return intent;
	}

	// --- Per-level file ops ---------------------------------------------------

	/** True if {@code filename} exists in the chosen folder. */
	public boolean hasLevel(String filename) {
		if (filename == null || filename.isEmpty()) return false;
		DocumentFile file = findLevel(filename);
		return file != null && file.exists();
	}

	/**
	 * Open {@code filename} for reading. Throws {@link FileNotFoundException}
	 * if no folder is chosen or the file isn't there.
	 */
	public InputStream openLevel(String filename) throws IOException {
		DocumentFile file = findLevel(filename);
		if (file == null || !file.exists()) {
			throw new FileNotFoundException("Level " + filename + " not in storage");
		}
		InputStream in = context.getContentResolver().openInputStream(file.getUri());
		if (in == null) {
			throw new IOException("ContentResolver returned null for " + file.getUri());
		}
		return in;
	}

	/**
	 * Create {@code filename} in the chosen folder (overwriting if present)
	 * and return an {@link OutputStream} to write into. Caller closes it.
	 */
	public OutputStream createLevel(String filename) throws IOException {
		DocumentFile tree = requireTree();

		// Overwrite-by-delete-and-create — DocumentFile has no truncating
		// "create or open" primitive, and concatenating onto an existing
		// .mrg would corrupt it.
		DocumentFile existing = tree.findFile(filename);
		if (existing != null) {
			existing.delete();
		}
		DocumentFile created = tree.createFile(MIME_MRG, filename);
		if (created == null) {
			throw new IOException("Could not create " + filename + " in " + tree.getUri());
		}
		OutputStream out = context.getContentResolver().openOutputStream(created.getUri());
		if (out == null) {
			throw new IOException("ContentResolver returned null for " + created.getUri());
		}
		return out;
	}

	/** Delete {@code filename} if present. No-op if not. */
	public void deleteLevel(String filename) {
		if (filename == null || filename.isEmpty()) return;
		DocumentFile file = findLevel(filename);
		if (file != null && file.exists()) {
			file.delete();
		}
	}

	/**
	 * Rename a file in the SAF tree. Returns the actual on-disk name after
	 * the rename — providers are allowed to disambiguate by appending
	 * {@code (1)}, {@code (2)} etc., so we re-query rather than trusting
	 * the requested name. Returns {@code null} if {@code oldName} doesn't
	 * exist or the rename failed.
	 *
	 * <p>Used by the v1→v2 startup migration to lift legacy {@code {id}.mrg}
	 * files to human-readable names.
	 */
	public String renameLevel(String oldName, String newName) {
		if (oldName == null || newName == null || oldName.equals(newName)) return null;
		Uri treeUri = getLocation();
		if (treeUri == null) return null;
		DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
		if (tree == null) return null;
		DocumentFile file = tree.findFile(oldName);
		if (file == null || !file.exists()) return null;
		boolean ok = file.renameTo(newName);
		if (!ok) return null;
		// Re-query: provider may have changed the name to avoid collision.
		String actual = file.getName();
		return actual != null ? actual : newName;
	}

	/**
	 * List all {@code .mrg} filenames in the chosen folder. Empty list if no
	 * folder is set. Used by the folder rescan path.
	 */
	public List<String> listMrgFiles() {
		ArrayList<String> out = new ArrayList<>();
		Uri treeUri = getLocation();
		if (treeUri == null) return out;
		DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
		if (tree == null || !tree.exists()) return out;
		// listFiles() can be expensive on large trees, but for a hand-curated
		// levels folder this is fine. Filter strictly to .mrg so we don't
		// pick up README.txt etc.
		for (DocumentFile child : tree.listFiles()) {
			if (child == null || child.isDirectory()) continue;
			String name = child.getName();
			if (Filenames.isMrgFilename(name)) {
				out.add(name);
			}
		}
		return out;
	}

	/** Direct DocumentFile lookup. Package-private for the rescan path. */
	DocumentFile findFile(String filename) {
		return findLevel(filename);
	}

	// --- Internals ------------------------------------------------------------

	private DocumentFile findLevel(String filename) {
		Uri treeUri = getLocation();
		if (treeUri == null) return null;
		DocumentFile tree = DocumentFile.fromTreeUri(context, treeUri);
		if (tree == null) return null;
		return tree.findFile(filename);
	}

	/** Public for callers (LevelsManager) that need direct tree access for {@link Filenames#uniqueIn}. */
	public DocumentFile getTree() {
		Uri treeUri = getLocation();
		if (treeUri == null) return null;
		return DocumentFile.fromTreeUri(context, treeUri);
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
}
