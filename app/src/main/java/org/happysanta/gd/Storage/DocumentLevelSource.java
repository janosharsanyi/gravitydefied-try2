package org.happysanta.gd.Storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link LevelSource} backed by a {@code .mrg} living in the user-chosen SAF
 * tree. Delegates to {@link LevelStorage#openLevel(String)} so the URI /
 * DocumentFile lookup stays in one place.
 */
public class DocumentLevelSource implements LevelSource {

	private final LevelStorage storage;
	private final String filename;

	public DocumentLevelSource(LevelStorage storage, String filename) {
		this.storage = storage;
		this.filename = filename;
	}

	@Override
	public InputStream open() throws IOException {
		return storage.openLevel(filename);
	}
}
