package org.happysanta.gd.Storage;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link LevelSource} backed by a file in the APK's {@code assets/} folder.
 * Used for the built-in {@code levels.mrg} that ships with the app.
 */
public class AssetLevelSource implements LevelSource {

	private final Context context;
	private final String assetName;

	public AssetLevelSource(Context context, String assetName) {
		this.context = context.getApplicationContext();
		this.assetName = assetName;
	}

	@Override
	public InputStream open() throws IOException {
		return context.getAssets().open(assetName);
	}
}
