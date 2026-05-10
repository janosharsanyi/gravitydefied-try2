package org.happysanta.gd;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.HashMap;

/**
 * Translates physical gamepad input into the J2ME-style numpad keypresses
 * the rest of the game already understands. Lives next to
 * {@link KeyboardController} (which does the same job for on-screen
 * touch input).
 *
 * <p>Two key dispatch modes:
 * <ul>
 *   <li><b>In menu</b> — uses the canonical menu-nav mapping
 *       (2/4/5/6/8 = UP/LEFT/FIRE/RIGHT/DOWN), matching
 *       {@link org.happysanta.gd.Game.GameView#getGameAction(int)}. Keyset-
 *       independent because the menu code translates these via
 *       {@code getGameAction()} regardless of {@link Settings#getInputOption()}.</li>
 *   <li><b>In game</b> — uses {@link #GAMEPLAY_KEY}, a per-keyset table that
 *       maps each logical action (accelerate / brake / lean back / lean
 *       forward) to the J2ME numpad number that the active keyset wires
 *       to that action. The keysets are genuinely disjoint in the physics
 *       engine ({@code m_maaaB[keyset][numpadKey]} returns {@code (0,0)}
 *       for keys outside the keyset's vocabulary), so per-keyset
 *       translation here is the only way to make a controller behave
 *       consistently regardless of which keyset is selected.</li>
 * </ul>
 *
 * <p><b>Press/release pairing.</b> The mapping from controller button →
 * J2ME key depends on {@link GDActivity#isMenuShown()} (and on the active
 * keyset for in-game), and that state can flip between a press and its
 * matching release — e.g. user holds ACCEL on the d-pad, opens the menu,
 * releases. To keep the engine's key state balanced, every press records
 * the exact J2ME key that was sent in {@link #heldByButton} /
 * {@link #heldByStickX} / {@link #heldByStickY}. Releases consult that
 * record rather than recomputing — guaranteeing the release always
 * targets the same key the press did.
 *
 * <p>Controller input does not affect the on-screen keypad's visibility —
 * that's driven by a touch-idle timer in {@link GDActivity}. The two input
 * sources coexist: pad and touch can be used interchangeably.
 */
public class ControllerInputHandler {

    // Logical action indices into a GAMEPLAY_KEY row.
    private static final int ACT_ACCEL = 0;
    private static final int ACT_BRAKE = 1;
    private static final int ACT_LEFT  = 2;
    private static final int ACT_RIGHT = 3;

    /**
     * {@code [keyset 0..2][action]} = J2ME numpad key (1-9). Mirrors the
     * keyset_text help string verbatim — if you change one, change the other.
     * Order: ACCEL, BRAKE, LEAN_LEFT (back), LEAN_RIGHT (forward).
     */
    private static final int[][] GAMEPLAY_KEY = {
            /* keyset 1 */ {2, 8, 4, 6},
            /* keyset 2 */ {1, 4, 5, 6},
            /* keyset 3 */ {3, 6, 4, 5},
    };

    // Menu-nav canonical keys (matches GameView.getGameAction()).
    private static final int MENU_KEY_UP    = 2;
    private static final int MENU_KEY_DOWN  = 8;
    private static final int MENU_KEY_LEFT  = 4;
    private static final int MENU_KEY_RIGHT = 6;
    private static final int MENU_KEY_FIRE  = 5;

    // Threshold for treating the d-pad-as-HAT axis as a digital direction.
    private static final float HAT_DEADZONE = 0.5f;

    // Deadzone applied to the analog stick (AXIS_X / AXIS_Y) before it
    // drives the physics engine — read live from Settings each motion
    // event so the user can re-pick it from the options menu without
    // restarting. Smaller than {@link #HAT_DEADZONE} because the stick
    // is meant to be proportional, not switch-like; this is the
    // threshold below which we consider the stick "centered" and stop
    // sending analog updates. See Settings.STICK_DEADZONE_PCT_VALUES.

    private final GDActivity gd;

    /** Maps controller {@link KeyEvent} keycode → J2ME key currently held
     *  for that button. Populated on {@code ACTION_DOWN}, drained on
     *  {@code ACTION_UP}. Persists across menu/game transitions so the
     *  release always sends the same J2ME key the press did. */
    private final HashMap<Integer, Integer> heldByButton = new HashMap<>();

    /** J2ME key currently held for the d-pad-as-HAT X axis, or 0 if
     *  neutral. Same role as {@link #heldByButton} for the digital
     *  buttons — the analog stick uses the {@link #setAnalogInput}
     *  channel instead and doesn't go through this. */
    private int heldByHatX = 0;
    private int heldByHatY = 0;

    // Last quantised (-1/0/+1) HAT axis value, used for edge detection.
    private int lastHatX = 0;
    private int lastHatY = 0;

    /** True while the analog stick is outside the deadzone — i.e. while
     *  the controller is currently driving physics through the analog
     *  channel. Used to push one final "neutral" frame when the stick
     *  returns to centre, then stop spamming the engine with zeros that
     *  would otherwise stomp any concurrent keyboard input. */
    private boolean analogActive = false;

    public ControllerInputHandler(GDActivity gd) {
        this.gd = gd;
    }

    /** True if the event source bitmask names a gamepad/joystick/d-pad. */
    private static boolean fromGamepad(int source) {
        return (source & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
                || (source & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
                || (source & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD;
    }

    /**
     * Feed a key event from {@code Activity.dispatchKeyEvent}. Returns
     * {@code true} if we recognised and consumed it.
     */
    public boolean dispatchKey(KeyEvent event) {
        if (!fromGamepad(event.getSource())) return false;
        int code = event.getKeyCode();
        // Bail on KEYCODE_BACK — let the OnBackPressedDispatcher handle it.
        if (code == KeyEvent.KEYCODE_BACK) return false;

        // Suppress synthetic auto-repeat — we only act on edges.
        if (event.getRepeatCount() > 0) {
            return true;
        }

        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                handleButton(code, leftKey(), down);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                handleButton(code, rightKey(), down);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                handleButton(code, accelKey(), down);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                handleButton(code, brakeKey(), down);
                return true;
            case KeyEvent.KEYCODE_BUTTON_A:
                // Menu: A = select (FIRE). Game: A = accelerate.
                handleButton(code, gd.isMenuShown() ? MENU_KEY_FIRE : accelKey(), down);
                return true;
            case KeyEvent.KEYCODE_BUTTON_B:
                // Menu: B = back (momentary, no held key). Game: B = brake.
                if (gd.isMenuShown()) {
                    if (down) gd.controllerBackPress();
                    // No key tracking — back is a one-shot.
                } else {
                    handleButton(code, brakeKey(), down);
                }
                return true;
            case KeyEvent.KEYCODE_BUTTON_START:
            case KeyEvent.KEYCODE_MENU:
                // Same as system back: in-game opens the menu, in-menu pops
                // the current screen. One-shot, no held key.
                if (down) gd.controllerBackPress();
                return true;
        }
        return false;
    }

    /**
     * Feed a generic motion event from {@code Activity.dispatchGenericMotionEvent}.
     *
     * <p>Two axis sources, two paths:
     * <ul>
     *   <li><b>HAT</b> (d-pad-as-axis on pads that don't fire {@code KEYCODE_DPAD_*}):
     *       quantised to ±1/0 with a 0.5 deadzone and sent as J2ME numpad key
     *       press/release through {@link #sendKey}. Identical to the digital
     *       d-pad button path.</li>
     *   <li><b>Left stick</b> (AXIS_X / AXIS_Y): goes through the analog
     *       channel — deadzone-clipped and pushed to {@link Physics#_aIIVAnalog}
     *       via {@link org.happysanta.gd.Game.GameView#setAnalogInput} so
     *       half-tilt produces visibly less torque/throttle than full-tilt.
     *       Suppressed in the menu (menu nav stays digital).</li>
     * </ul>
     */
    public boolean dispatchMotion(MotionEvent event) {
        if (!fromGamepad(event.getSource())) return false;
        if (event.getAction() != MotionEvent.ACTION_MOVE) return false;

        boolean changed = false;
        changed |= dispatchHat(event);
        changed |= dispatchAnalogStick(event);
        return changed;
    }

    /** HAT (d-pad-as-axis) → digital, quantised to ±1/0 and sent as J2ME keys. */
    private boolean dispatchHat(MotionEvent event) {
        float hx = event.getAxisValue(MotionEvent.AXIS_HAT_X);
        float hy = event.getAxisValue(MotionEvent.AXIS_HAT_Y);

        int sx = hx < -HAT_DEADZONE ? -1 : (hx > HAT_DEADZONE ? 1 : 0);
        int sy = hy < -HAT_DEADZONE ? -1 : (hy > HAT_DEADZONE ? 1 : 0);

        boolean changed = false;
        if (sx != lastHatX) {
            if (heldByHatX != 0) {
                sendKey(heldByHatX, false);
                heldByHatX = 0;
            }
            if (sx < 0) {
                heldByHatX = leftKey();
                sendKey(heldByHatX, true);
            } else if (sx > 0) {
                heldByHatX = rightKey();
                sendKey(heldByHatX, true);
            }
            lastHatX = sx;
            changed = true;
        }
        if (sy != lastHatY) {
            if (heldByHatY != 0) {
                sendKey(heldByHatY, false);
                heldByHatY = 0;
            }
            if (sy < 0) {
                heldByHatY = accelKey();
                sendKey(heldByHatY, true);
            } else if (sy > 0) {
                heldByHatY = brakeKey();
                sendKey(heldByHatY, true);
            }
            lastHatY = sy;
            changed = true;
        }
        return changed;
    }

    /** Left stick → analog physics channel. Suppressed in menu.
     *  Stick mode (Settings.getStickMode) selects between analog
     *  magnitude-proportional output and digital ±1/0 snap. */
    private boolean dispatchAnalogStick(MotionEvent event) {
        if (gd.gameView == null) return false;

        float ax = event.getAxisValue(MotionEvent.AXIS_X);
        float ay = event.getAxisValue(MotionEvent.AXIS_Y);

        // Live reads from Settings so the user can switch mode / deadzone
        // in the options menu and feel the change immediately on the next
        // motion event. Cheap — two int pref reads.
        final float deadzone = Settings.getStickDeadzonePct() / 100f;
        final boolean digital = Settings.getStickMode() == Settings.STICK_MODE_DIGITAL;

        // Per-mode: figure out whether the stick is "active" and what
        // (x, y) to ship to physics.
        boolean inside;
        float x, y;
        if (digital) {
            // Per-axis deadzone — that's how a real d-pad behaves. Chord
            // magnitude would conflate the two axes after snapping and
            // make diagonals feel inconsistent.
            x = ax > deadzone ?  1f : (ax < -deadzone ? -1f : 0f);
            y = ay > deadzone ?  1f : (ay < -deadzone ? -1f : 0f);
            inside = (x == 0f && y == 0f);
        } else {
            // Analog: chord magnitude so a near-pure diagonal still
            // registers above the deadzone on both axes.
            float mag = (float) Math.sqrt(ax * ax + ay * ay);
            inside = mag < deadzone;
            if (inside) {
                x = 0f;
                y = 0f;
            } else {
                // Rescale (mag - deadzone) → 0..1, preserve direction,
                // clamp components to ±1.
                float scale = (mag - deadzone) / (1f - deadzone) / mag;
                x = ax * scale;
                y = ay * scale;
                if (x >  1f) x =  1f; else if (x < -1f) x = -1f;
                if (y >  1f) y =  1f; else if (y < -1f) y = -1f;
            }
        }

        // In menu: never drive the analog channel. If we'd been driving it
        // before the menu opened, push one neutral frame to release.
        if (gd.isMenuShown()) {
            if (analogActive) {
                gd.gameView.setAnalogInput(0, 0);
                analogActive = false;
                return true;
            }
            return false;
        }

        if (inside) {
            if (!analogActive) return false;
            // Stick just returned to centre — release physics state and stop.
            gd.gameView.setAnalogInput(0, 0);
            analogActive = false;
            return true;
        }

        // Sign convention: physics expects positive throttle = accel and
        // positive lean = forward (right). Android stick Y is +down, so
        // throttle = -y. Stick X is +right, so lean = +x.
        int throttleSignedI = (int) (-y * 0x10000);
        int leanSignedI     = (int) ( x * 0x10000);

        gd.gameView.setAnalogInput(throttleSignedI, leanSignedI);
        analogActive = true;
        return true;
    }

    /**
     * Press or release a single controller button. On press, sends
     * {@code j2meKey} and remembers it under {@code controllerCode} so
     * the matching release sends the same key — even if the menu/game
     * mode flipped in between.
     */
    private void handleButton(int controllerCode, int j2meKey, boolean down) {
        if (down) {
            // Defensive: if a stale press is recorded for this button (no
            // matching release was ever delivered), release it first so the
            // engine's key state stays balanced.
            Integer prev = heldByButton.get(controllerCode);
            if (prev != null && prev != 0 && prev != j2meKey) {
                sendKey(prev, false);
            }
            heldByButton.put(controllerCode, j2meKey);
            sendKey(j2meKey, true);
        } else {
            Integer held = heldByButton.remove(controllerCode);
            if (held != null && held != 0) {
                sendKey(held, false);
            }
        }
    }

    // --- key lookups: all consult current keyset / menu state ----------------

    private int accelKey() {
        return gd.isMenuShown() ? MENU_KEY_UP    : GAMEPLAY_KEY[clampedKeyset()][ACT_ACCEL];
    }

    private int brakeKey() {
        return gd.isMenuShown() ? MENU_KEY_DOWN  : GAMEPLAY_KEY[clampedKeyset()][ACT_BRAKE];
    }

    private int leftKey() {
        return gd.isMenuShown() ? MENU_KEY_LEFT  : GAMEPLAY_KEY[clampedKeyset()][ACT_LEFT];
    }

    private int rightKey() {
        return gd.isMenuShown() ? MENU_KEY_RIGHT : GAMEPLAY_KEY[clampedKeyset()][ACT_RIGHT];
    }

    private void sendKey(int j2meKey, boolean down) {
        if (gd.gameView == null) return;
        if (j2meKey < 1 || j2meKey > 9) return;
        int code = j2meKey + 48; // '1' == 49, matches GameView.getGameAction()
        if (down) gd.gameView.keyPressed(code);
        else gd.gameView.keyReleased(code);
    }

    private static int clampedKeyset() {
        int ks = Settings.getInputOption();
        if (ks < 0) return 0;
        if (ks >= GAMEPLAY_KEY.length) return GAMEPLAY_KEY.length - 1;
        return ks;
    }
}
