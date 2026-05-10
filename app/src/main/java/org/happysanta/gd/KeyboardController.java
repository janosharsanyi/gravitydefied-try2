package org.happysanta.gd;

import android.graphics.Rect;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import org.happysanta.gd.Game.GameView;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.happysanta.gd.Helpers.getDp;

public class KeyboardController {

	private static final int MAX_POINTERS = 10;
	public static final int PADDING = 15;
	private static final boolean DISABLE_MOVE = false;

	/** Standard 3×3 layout with all 9 J2ME-style numpad keys. Used in
	 *  portrait and as a fallback when no cluster is supplied. */
	public static final int[][] FULL_GRID = new int[][] {
			{1, 2, 3},
			{4, 5, 6},
			{7, 8, 9}
	};

	private static int PADDING_DP = 0;

	private GDActivity gd;
	private int[] buf;
	private LinearLayout[] btns;
	private PointerInfo[] pointers;
	private StringBuffer logBuffer;
	/**
	 * Default touch handler used when a view registers via the legacy
	 * {@code setOnTouchListener(keyboardController)} pattern — assumes a
	 * single 3×3 grid covering the whole view. Cluster views should call
	 * {@link #makeTouchListener(int[][])} instead.
	 */
	private final ClusterTouchHandler defaultHandler;

	static {
		PADDING_DP = getDp(PADDING);
	}

	KeyboardController(GDActivity gd) {
		this.gd = gd;
		buf = new int[2];
		btns = new LinearLayout[9];
		pointers = new PointerInfo[MAX_POINTERS];
		for (int i = 0; i < MAX_POINTERS; i++) {
			pointers[i] = new PointerInfo(i);
		}

		logBuffer = new StringBuffer();
		defaultHandler = new ClusterTouchHandler(FULL_GRID);
	}

	/**
	 * Backward-compatible touch listener entry point. Behaves like the
	 * legacy implementation: assumes the view it's attached to is a single
	 * 3×3 grid laid out left-to-right, top-to-bottom with keys 1-9.
	 */
	public View.OnTouchListener asLegacyTouchListener() {
		return defaultHandler;
	}

	/**
	 * Build a touch listener for a cluster view whose buttons follow the
	 * supplied {@code keys} grid (rows × cols of J2ME-style numpad numbers,
	 * 1-9; use 0 for "no key" cells). Hit-testing divides the view's bounds
	 * into {@code rows × cols} cells and maps each cell to
	 * {@code keys[row][col]}.
	 *
	 * <p>Multiple cluster listeners share the same {@code btns[]} and
	 * pointer state (registered via {@link #registerButton(LinearLayout, int)})
	 * so press/release feedback and the game key codes stay consistent
	 * across clusters.
	 */
	public View.OnTouchListener makeTouchListener(int[][] keys) {
		return new ClusterTouchHandler(keys);
	}

	/**
	 * Register a {@link LinearLayout} button for the given J2ME-style key
	 * number (1-9). The controller stores it so {@link ClusterTouchHandler}
	 * can drive its pressed state regardless of which cluster the touch
	 * originated from.
	 */
	public void registerButton(LinearLayout btn, int j2meKey) {
		if (j2meKey < 1 || j2meKey > 9) return;
		btns[j2meKey - 1] = btn;
	}

	/**
	 * Legacy registration form retained for the original 3×3 keypad layout
	 * code path: {@code (x, y)} is the cell in a 3×3 grid (key = y*3 + x + 1).
	 */
	public void addButton(LinearLayout btn, int x, int y) {
		btns[y * 3 + x] = btn;
	}

	private class ClusterTouchHandler implements View.OnTouchListener {
		private final int[][] keys;
		private final int rows, cols;

		ClusterTouchHandler(int[][] keys) {
			this.keys = keys;
			this.rows = keys.length;
			// Allow jagged rows — col count is the widest row, and
			// out-of-range cells map to "no key" (-1) on hit-test.
			int maxCols = 0;
			for (int[] row : keys) maxCols = Math.max(maxCols, row.length);
			this.cols = Math.max(1, maxCols);
		}

		/** Returns the index into {@code btns[]}/{@code pointers[]} (= j2meKey - 1)
		 *  for the cell under (x, y), or -1 if the cell maps to "no key". */
		private int whichButton(Rect rect, int x, int y) {
			int cellW = Math.max(1, rect.width() / cols);
			int cellH = Math.max(1, rect.height() / rows);
			int relX = x - PADDING_DP;
			int relY = y - PADDING_DP;
			int posX = Math.max(0, Math.min(cols - 1, relX / cellW));
			int posY = Math.max(0, Math.min(rows - 1, relY / cellH));
			int[] row = keys[posY];
			int j2me = (posX < row.length) ? row[posX] : 0;
			if (j2me < 1 || j2me > 9) return -1;
			return j2me - 1;
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			GameView gameView = gd.gameView;

			v.getLocationOnScreen(buf);
			Rect rect = new Rect(buf[0], buf[1], buf[0] + v.getWidth(), buf[1] + v.getHeight());

			rect.left += PADDING_DP;
			rect.right -= PADDING_DP;
			rect.top += PADDING_DP;
			rect.bottom -= PADDING_DP;

			int action = event.getActionMasked();
			if (action == MotionEvent.ACTION_DOWN
					|| action == MotionEvent.ACTION_POINTER_DOWN
					|| action == MotionEvent.ACTION_UP
					|| action == MotionEvent.ACTION_POINTER_UP) {

				int index = event.getActionIndex();
				int pointerId = event.getPointerId(index);

				if (pointerId >= MAX_POINTERS) {
					return true;
				}

				int x = Math.round(event.getX(index));
				int y = Math.round(event.getY(index));

				LinearLayout btn;
				PointerInfo pointer = pointers[pointerId];

				int btnIndex = whichButton(rect, x, y);

				switch (action) {
					case MotionEvent.ACTION_DOWN:
					case MotionEvent.ACTION_POINTER_DOWN:
						if (btnIndex < 0) break;
						press(v);

						pointer.setButtonIndex(btnIndex);
						btn = pointer.getButton();

						if (btn != null) btn.setPressed(true);
						gameView.keyPressed(gameKeyCode(btnIndex));
						break;

					case MotionEvent.ACTION_UP:
					case MotionEvent.ACTION_POINTER_UP:
						btn = pointer.getButton();
						if (btn != null) {
							btn.setPressed(false);
							if (DISABLE_MOVE) {
								btnIndex = pointer.btnIndex;
							}
							if (btnIndex >= 0) {
								gameView.keyReleased(gameKeyCode(btnIndex));
							}
							pointer.finish();
						}
						break;
				}
			} else if (action == MotionEvent.ACTION_MOVE && !gd.isMenuShown() && !DISABLE_MOVE) {
				int pointerCount = event.getPointerCount();
				LinearLayout btn, oldBtn;
				PointerInfo pointer;

				for (int pointerIndex = 0; pointerIndex < pointerCount; pointerIndex++) {
					int pointerId = event.getPointerId(pointerIndex);
					if (pointerId >= MAX_POINTERS) continue;

					int x = Math.round(event.getX(pointerIndex));
					int y = Math.round(event.getY(pointerIndex));

					int btnIndex = whichButton(rect, x, y);

					pointer = pointers[pointerId];
					if (btnIndex != pointer.btnIndex) {
						if (pointer.btnIndex >= 0) {
							oldBtn = btns[pointer.btnIndex];
							if (oldBtn != null) oldBtn.setPressed(false);
							gameView.keyReleased(gameKeyCode(pointer.btnIndex));
						}

						if (btnIndex >= 0) {
							press(v);

							pointer.setButtonIndex(btnIndex);
							btn = pointer.getButton();
							if (btn != null) btn.setPressed(true);
							gameView.keyPressed(gameKeyCode(pointer.btnIndex));
						} else {
							pointer.finish();
						}
					}
				}
			}

			return true;
		}
	}

	private synchronized void log(Object o, boolean last) {
		String logStr = o.toString();
		Log.d("GD Keyboard", o.toString());

		if (last)
			Log.d("", "");
	}

	private void log(Object o) {
		log(o, false);
	}

	public synchronized void clearLogBuffer() {
		logBuffer = null;
		logBuffer = new StringBuffer();
	}

	public String getLog() {
		return logBuffer.toString();
	}

	private static String getCurrentTime() {
		Calendar cal = Calendar.getInstance();
		cal.getTime();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		return sdf.format(cal.getTime());
	}

	private static String actionToString(int action) {
		switch (action) {
			case MotionEvent.ACTION_DOWN:
				return "ACTION_DOWN";
			case MotionEvent.ACTION_POINTER_DOWN:
				return "ACTION_POINTER_DOWN";
			case MotionEvent.ACTION_POINTER_2_DOWN:
				return "ACTION_POINTER_2_DOWN";
			case MotionEvent.ACTION_POINTER_2_UP:
				return "ACTION_POINTER_2_UP";
			case MotionEvent.ACTION_POINTER_3_DOWN:
				return "ACTION_POINTER_3_DOWN";
			case MotionEvent.ACTION_POINTER_3_UP:
				return "ACTION_POINTER_3_UP";
			case MotionEvent.ACTION_UP:
				return "ACTION_UP";
			case MotionEvent.ACTION_POINTER_UP:
				return "ACTION_POINTER_UP";
			case MotionEvent.ACTION_MOVE:
				return "ACTION_MOVE";
		}
		return "?";
	}

	private static int gameKeyCode(int btnIndex) {
		return btnIndex + 49;
	}

	private static void press(View v) {
		if (Settings.isVibrateOnTouchEnabled()) {
			v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY, HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
		}
	}

	private class PointerInfo {

		private int id;
		private int btnIndex = -1;
		private boolean active = false;

		PointerInfo(int id) {
			this.id = id;
		}

		void finish() {
			active = false;
			btnIndex = -1;
		}

		void setButtonIndex(int index) {
			active = true;
			btnIndex = index;
		}

		LinearLayout getButton() {
			if (!active || btnIndex < 0)
				return null;
			return btns[btnIndex];
		}

	}

}
