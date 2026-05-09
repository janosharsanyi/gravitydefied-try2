package org.happysanta.gd.Storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link LevelSource} backed by a downloaded {@code {id}.mrg} living in the
 * user-chosen SAF tree. Delegates to {@link LevelStorage#openLevel(long)}
 * so the URI / DocumentFile lookup stays in one place.
 */
public class DocumentLevelSource implements LevelSource {

	private final LevelStorage storage;
	private final long levelId;

	public DocumentLevelSource(LevelStorage storage, long levelId) {
		this.storage = storage;
		this.levelId = levelId;
	}

	@Override
	public InputStream open() throws IOException {
		return storage.openLevel(levelId);
	}
}
