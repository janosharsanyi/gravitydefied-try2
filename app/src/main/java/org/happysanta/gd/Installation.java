package org.happysanta.gd;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Persistent installation ID used when sending level stats to the server.
 * Replaces ACRA's {@code org.acra.util.Installation} (ACRA was dropped
 * during the modernization port).
 *
 * The ID survives app updates but is regenerated on uninstall or
 * clear-data — same effective semantics as the original.
 */
public class Installation {

	private static final String PREFS_NAME = "installation";
	private static final String KEY_ID = "id";

	public static synchronized String id(Context context) {
		SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		String id = prefs.getString(KEY_ID, null);
		if (id == null) {
			id = UUID.randomUUID().toString();
			prefs.edit().putString(KEY_ID, id).apply();
		}
		return id;
	}

}
