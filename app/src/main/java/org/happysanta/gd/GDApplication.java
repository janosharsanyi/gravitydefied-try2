package org.happysanta.gd;

import android.app.Application;

/**
 * Application stub. The original used this to initialize ACRA, which we
 * dropped during modernization. Kept as a hook for future global init.
 */
public class GDApplication extends Application {

	@Override
	public void onCreate() {
		super.onCreate();
	}

}
