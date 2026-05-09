package org.happysanta.gd.Storage;

import java.io.IOException;
import java.io.InputStream;

/**
 * A re-openable source of level bytes (a {@code .mrg} payload).
 *
 * <p>Replaces the old "{@code java.io.File} you can re-open" pattern that
 * {@link org.happysanta.gd.Levels.Loader} relied on. The loader opens the
 * stream multiple times during a single load (once per track read), so a
 * single {@code InputStream} won't do — we need a factory.
 *
 * <p>Implementations are expected to be cheap to call repeatedly.
 */
public interface LevelSource {

	/**
	 * Open a fresh {@link InputStream} positioned at the start of the level
	 * payload. Caller closes it.
	 */
	InputStream open() throws IOException;
}
