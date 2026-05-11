package org.happysanta.gd.Menu;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import org.happysanta.gd.Global;
import org.happysanta.gd.Menu.Views.MenuTextView;

import static org.happysanta.gd.Helpers.getGDActivity;

/**
 * In-place 3-digit RGB channel editor row. Renders as
 * {@code "FG R: 255"} when idle; entering edit mode displays the digits
 * with the active position underlined as {@code "FG R: 2[5]5"}. UP/DOWN
 * cycle the highlighted digit 0..9, LEFT/RIGHT move the cursor, FIRE
 * advances the cursor and exits on the third digit. LEFT off position 0
 * also exits. On exit the assembled value is clamped to 255 (so e.g.
 * {@code 287} writes back as {@code 255}).
 *
 * Built-in presets are not directly editable; FIRE on a row whose
 * {@link ChannelTarget#isCurrentPresetCustom} returns {@code false}
 * navigates to the copy-into-Custom chooser instead.
 *
 * UP/DOWN must reach the highlighted element while editing, but
 * {@link MenuScreen#performAction} consumes them for row navigation. The
 * hosting screen (see {@link ColorsMenuScreen}) overrides performAction
 * to route UP/DOWN to the highlighted element whenever it's a
 * ChannelMenuElement in edit mode.
 */
public class ChannelMenuElement extends ClickableMenuElement {

	public interface ChannelTarget {
		/** Current persisted value, 0..255. */
		int getValue();
		/** Write a clamped value (0..255 enforced by caller). */
		void setValue(int clamped);
		/**
		 * True when the row's enclosing preset is one of the Custom slots
		 * (track Custom 1..3 or neon Custom 1..3) — i.e. the row is
		 * directly editable. False for built-in presets, in which case
		 * FIRE delegates to {@link #enterCopyFlow}.
		 */
		boolean isCurrentPresetCustom();
		/**
		 * Navigate to the copy-into-Custom chooser for this row's family
		 * (track or neon). The chooser picks a slot, copies the current
		 * preset's channels into it, and switches the active preset to
		 * the chosen Custom slot.
		 */
		void enterCopyFlow();
	}

	private final ChannelTarget target;
	private boolean editing = false;
	private int cursorPos = 0; // 0..2 over the three digits
	private final int[] digits = new int[3]; // working buffer during edit

	private MenuTextView valueTextView;

	public ChannelMenuElement(String label, ChannelTarget target) {
		this.text = label;
		this.target = target;
		createAllViews();
		updateViewText();
	}

	@Override
	protected void createAllViews() {
		Context context = getGDActivity();
		super.createAllViews();

		textView.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		));

		valueTextView = new MenuTextView(context);
		valueTextView.setTextColor(getMenuTextView().getTextColors());
		valueTextView.setTextSize(TEXT_SIZE);
		valueTextView.setTypeface(Global.robotoCondensedTypeface);
		valueTextView.setLayoutParams(new LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.WRAP_CONTENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
		));
		valueTextView.setPadding(
				textView.getPaddingLeft(),
				textView.getPaddingTop(),
				textView.getPaddingRight(),
				textView.getPaddingBottom()
		);
		layout.addView(valueTextView);
	}

	public boolean isEditing() {
		return editing;
	}

	/** Refresh the displayed value from {@link ChannelTarget#getValue()}. */
	public void refresh() {
		if (editing) {
			// External refresh during edit is suspicious; bail to idle so
			// the user doesn't see stale digits on the cursor.
			editing = false;
		}
		updateViewText();
	}

	@Override
	protected String getTextForView() {
		return text + ": ";
	}

	@Override
	protected void updateViewText() {
		super.updateViewText();
		if (valueTextView != null) {
			valueTextView.setTextOnUiThread(buildValueString());
		}
	}

	private String buildValueString() {
		if (!editing) {
			return String.valueOf(target.getValue());
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			if (i == cursorPos) {
				sb.append('[').append(digits[i]).append(']');
			} else {
				sb.append(digits[i]);
			}
		}
		return sb.toString();
	}

	private void enterEdit() {
		int v = target.getValue();
		if (v < 0) v = 0;
		if (v > 255) v = 255;
		digits[0] = (v / 100) % 10;
		digits[1] = (v / 10) % 10;
		digits[2] = v % 10;
		cursorPos = 0;
		editing = true;
		updateViewText();
	}

	private void exitEdit() {
		int v = digits[0] * 100 + digits[1] * 10 + digits[2];
		if (v > 255) v = 255;
		if (v < 0) v = 0;
		target.setValue(v);
		editing = false;
		updateViewText();
	}

	/**
	 * If this row is currently in edit mode, commit the assembled digits
	 * (clamped to 255) to the underlying {@link ChannelTarget} and leave
	 * edit mode. No-op otherwise. Invoked by the hosting screen's BACK
	 * handler so the user can confirm an edit with the BACK key the same
	 * way a third FIRE press or a RIGHT off the last position would.
	 */
	public void commitIfEditing() {
		if (editing) exitEdit();
	}

	@Override
	public void performAction(int k) {
		if (!editing) {
			if (k == MenuScreen.KEY_FIRE) {
				if (target.isCurrentPresetCustom()) {
					enterEdit();
				} else {
					target.enterCopyFlow();
				}
			}
			return;
		}

		switch (k) {
			case MenuScreen.KEY_UP:
				digits[cursorPos] = (digits[cursorPos] + 1) % 10;
				updateViewText();
				break;
			case MenuScreen.KEY_DOWN:
				digits[cursorPos] = (digits[cursorPos] + 9) % 10;
				updateViewText();
				break;
			case MenuScreen.KEY_LEFT:
				cursorPos--;
				if (cursorPos < 0) {
					exitEdit();
				} else {
					updateViewText();
				}
				break;
			case MenuScreen.KEY_RIGHT:
				cursorPos++;
				if (cursorPos > 2) {
					exitEdit();
				} else {
					updateViewText();
				}
				break;
			case MenuScreen.KEY_FIRE:
				cursorPos++;
				if (cursorPos > 2) {
					exitEdit();
				} else {
					updateViewText();
				}
				break;
		}
	}
}
