package org.happysanta.gd;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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

    /** Per-device cache of which axes carry the right stick. Most pads
     *  report it on {@link MotionEvent#AXIS_Z} / {@link MotionEvent#AXIS_RZ},
     *  some on {@link MotionEvent#AXIS_RX} / {@link MotionEvent#AXIS_RY}, and
     *  a few put trigger pressure on Z/RZ instead. {@link #resolveRightStickAxes}
     *  picks the bipolar pair on first sight of a device and stashes the
     *  answer here so we don't reprobe every motion event. Keyed by
     *  {@link InputDevice#getId()}. */
    private final HashMap<Integer, int[]> rightStickAxesByDevice = new HashMap<>();

    /** Most recent raw deflection of the left and right sticks, kept across
     *  motion events so a single-source event can't silently zero the other
     *  stick's contribution. Some pads expose left and right sticks under
     *  different {@link InputDevice} sources (or fire separate events per
     *  stick), and on those an event from the left source returns 0 for the
     *  right-stick axes — without this cache, dual layouts would look like
     *  only one joystick is active at a time because each event clobbered
     *  the other stick's value via {@link #setAnalogInput} writing both
     *  components together. The cache is only updated for axes the event's
     *  source actually carries (see {@link #sourceHasAxis}). */
    private float cachedLx = 0f, cachedLy = 0f, cachedRx = 0f, cachedRy = 0f;

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
                    if (down) {
                        gd.controllerBackPress();
                    } else {
                        // A B-press recorded in-game (brake) can have its
                        // matching release land here if the user paused
                        // mid-brake. Drain the held entry so the engine
                        // doesn't sit with m_LaZ[brakeKey]=true forever.
                        // No-op if no in-game press was recorded for B.
                        Integer prev = heldByButton.remove(code);
                        if (prev != null && prev != 0) sendKey(prev, false);
                    }
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

    /** Analog stick(s) → physics channel. Suppressed in menu.
     *
     *  <p>The {@link Settings#getStickLayout layout} setting selects between:
     *  <ul>
     *    <li>{@code SINGLE}: left stick X = lean, Y = throttle. 2D radial
     *        deadzone so near-pure diagonals still register on both axes.</li>
     *    <li>{@code DUAL_LEAN_LEFT}: left X = lean, right Y = throttle.</li>
     *    <li>{@code DUAL_THROTTLE_LEFT}: right X = lean, left Y = throttle.</li>
     *  </ul>
     *  In dual modes each stick is treated as 1D — its other axis is ignored
     *  entirely (including for the deadzone check), so drift on the unused
     *  axis can't leak into physics. Right-stick axes are resolved per device
     *  via {@link #resolveRightStickAxes} (auto-picks {@code AXIS_Z}/{@code AXIS_RZ}
     *  or {@code AXIS_RX}/{@code AXIS_RY} based on which pair the pad reports
     *  as bipolar), so both common pad conventions work without calibration.
     *
     *  <p>{@link Settings#getStickMode mode} selects analog vs. digital
     *  (digital snaps each component past the deadzone to ±1/0), and
     *  {@link Settings#getStickDeadzonePct deadzone} is shared by all sticks
     *  in all layouts.
     *
     *  <p>Two transforms compose with the layout, applied per *source stick*
     *  (not per physics axis):
     *  <ul>
     *    <li>{@link Settings#getStickAxisFlip axis flip} swaps X↔Y on the
     *        affected stick(s) — picks which physical axis feeds the
     *        physics quantity the layout assigned to that stick. Applied
     *        before deadzone+scale.</li>
     *    <li>{@link Settings#getStickInvert invert} flips the sign of the
     *        post-scale output for the affected stick(s). Applied after
     *        deadzone+scale.</li>
     *  </ul>
     *  In SINGLE mode the right-stick toggles are no-ops (no R stick in use). */
    private boolean dispatchAnalogStick(MotionEvent event) {
        if (gd.gameView == null) return false;

        final InputDevice device = event.getDevice();
        final int source = event.getSource();

        // Live reads from Settings so the user can switch mode / deadzone /
        // layout / invert / flip in the options menu and feel the change
        // immediately on the next motion event. Cheap — five int pref reads.
        final float deadzone = Settings.getStickDeadzonePct() / 100f;
        final boolean digital = Settings.getStickMode() == Settings.STICK_MODE_DIGITAL;
        final int layout = Settings.getStickLayout();
        final int invert = Settings.getStickInvert();
        final int axisFlip = Settings.getStickAxisFlip();

        // Axis flip is applied *before* deadzone+scale: it just selects
        // which raw physical axis on a stick feeds the assigned physics
        // quantity. Invert is applied *after* deadzone+scale: it flips the
        // sign of the post-scale output. Both compose with the layout.
        final boolean flipL = (axisFlip == Settings.STICK_AXIS_FLIP_LEFT)
                || (axisFlip == Settings.STICK_AXIS_FLIP_BOTH);
        final boolean flipR = (axisFlip == Settings.STICK_AXIS_FLIP_RIGHT)
                || (axisFlip == Settings.STICK_AXIS_FLIP_BOTH);
        final boolean invertL = (invert == Settings.STICK_INVERT_LEFT)
                || (invert == Settings.STICK_INVERT_ALL);
        final boolean invertR = (invert == Settings.STICK_INVERT_RIGHT)
                || (invert == Settings.STICK_INVERT_ALL);

        // Whether the throttle channel ends up reading from a physical X
        // axis (rather than the default Y). Default mapping has throttle
        // on Y; a flip on the throttle-providing stick swaps it to X.
        // Used at the sign step below: Y is +down so throttle = -ay
        // (user-natural up=accel), but X is +right so flipped throttle
        // = +ax (user-natural right=accel). Without this, flipping the
        // throttle stick produces stick-right=brake, which combined with
        // Invert just to fix it is a UX trap.
        final boolean throttleFromX;
        if (layout == Settings.STICK_LAYOUT_SINGLE) {
            // Single stick (L) drives both axes; flipL swaps which feeds throttle.
            throttleFromX = flipL;
        } else if (layout == Settings.STICK_LAYOUT_DUAL_LEAN_LEFT) {
            // R drives throttle; flipR swaps R's axes.
            throttleFromX = flipR;
        } else { // STICK_LAYOUT_DUAL_THROTTLE_LEFT
            // L drives throttle; flipL swaps L's axes.
            throttleFromX = flipL;
        }

        // Resolve which axes this event's source actually carries, then
        // refresh only that half of the per-stick cache. Pads sometimes
        // expose left and right sticks under different InputDevice sources
        // (or fire separate events per stick); on those, an event from one
        // source returns 0 for the other source's axes. Reading the cache
        // for unreported axes prevents one stick's event from zeroing the
        // other's contribution. For the common case where both sticks live
        // under the same source, sourceHasAxis is true for both, the cache
        // is fully refreshed every event, and behaviour is unchanged.
        final int[] rightAxes = resolveRightStickAxes(device);
        final boolean leftPresent = sourceHasAxis(device, source, MotionEvent.AXIS_X)
                && sourceHasAxis(device, source, MotionEvent.AXIS_Y);
        final boolean rightPresent = rightAxes[0] >= 0 && rightAxes[1] >= 0
                && sourceHasAxis(device, source, rightAxes[0])
                && sourceHasAxis(device, source, rightAxes[1]);
        // HAT-only events (or any event whose source carries neither stick)
        // shouldn't touch the analog channel. dispatchHat handles those.
        if (!leftPresent && !rightPresent) return false;
        if (leftPresent) {
            cachedLx = event.getAxisValue(MotionEvent.AXIS_X);
            cachedLy = event.getAxisValue(MotionEvent.AXIS_Y);
        }
        if (rightPresent) {
            cachedRx = event.getAxisValue(rightAxes[0]);
            cachedRy = event.getAxisValue(rightAxes[1]);
        }

        float leanComponent;
        float throttleComponent;
        if (layout == Settings.STICK_LAYOUT_SINGLE) {
            float ax = cachedLx;
            float ay = cachedLy;
            if (digital) {
                // Per-axis deadzone — that's how a real d-pad behaves. Chord
                // magnitude would conflate the two axes after snapping and
                // make diagonals feel inconsistent.
                leanComponent     = ax > deadzone ?  1f : (ax < -deadzone ? -1f : 0f);
                throttleComponent = ay > deadzone ?  1f : (ay < -deadzone ? -1f : 0f);
            } else {
                // Analog: chord magnitude so a near-pure diagonal still
                // registers above the deadzone on both axes.
                float mag = (float) Math.sqrt(ax * ax + ay * ay);
                if (mag < deadzone) {
                    leanComponent = 0f;
                    throttleComponent = 0f;
                } else {
                    // Rescale (mag - deadzone) → 0..1, preserve direction,
                    // clamp components to ±1.
                    float scale = (mag - deadzone) / (1f - deadzone) / mag;
                    leanComponent     = ax * scale;
                    throttleComponent = ay * scale;
                    if (leanComponent >  1f) leanComponent =  1f;
                    else if (leanComponent < -1f) leanComponent = -1f;
                    if (throttleComponent >  1f) throttleComponent =  1f;
                    else if (throttleComponent < -1f) throttleComponent = -1f;
                }
            }
            // Single-stick axis flip: swap which axis drives lean vs throttle.
            // Default: stick X = lean, stick Y = throttle.
            // Flipped: stick Y = lean, stick X = throttle.
            // R is unused in single mode → flipR is a no-op here.
            if (flipL) {
                float t = leanComponent;
                leanComponent = throttleComponent;
                throttleComponent = t;
            }
            // Single-stick invert: L is the only stick → invertL flips both
            // outputs. R is unused → invertR is a no-op.
            if (invertL) {
                leanComponent = -leanComponent;
                throttleComponent = -throttleComponent;
            }
        } else {
            // Read each stick from the cache rather than the event so a
            // single-source event preserves the other stick's last-known
            // deflection (see the cache update above). For pads that put
            // no right stick on any axis, cachedRx/cachedRy stay at 0 so
            // trigger pressure can't leak into the throttle/lean channel.
            float lx = cachedLx;
            float ly = cachedLy;
            float rx = cachedRx;
            float ry = cachedRy;
            // Layout decides which stick drives which physics axis; flip
            // decides which physical axis on that stick to read. Default
            // physical axis is X for lean (the lateral motion), Y for
            // throttle (the vertical motion); flipping picks the opposite.
            float leanRaw, throttleRaw;
            if (layout == Settings.STICK_LAYOUT_DUAL_LEAN_LEFT) {
                // L → lean, R → throttle.
                leanRaw     = flipL ? ly : lx;
                throttleRaw = flipR ? rx : ry;
            } else { // STICK_LAYOUT_DUAL_THROTTLE_LEFT — L → throttle, R → lean.
                throttleRaw = flipL ? lx : ly;
                leanRaw     = flipR ? ry : rx;
            }
            leanComponent     = mapAxis1D(leanRaw, deadzone, digital);
            throttleComponent = mapAxis1D(throttleRaw, deadzone, digital);
            // Invert is per source stick, not per physics axis: invertL
            // flips whatever physics quantity L is currently driving,
            // invertR flips whatever R drives. Read the layout to decide.
            if (layout == Settings.STICK_LAYOUT_DUAL_LEAN_LEFT) {
                if (invertL) leanComponent = -leanComponent;
                if (invertR) throttleComponent = -throttleComponent;
            } else { // STICK_LAYOUT_DUAL_THROTTLE_LEFT
                if (invertL) throttleComponent = -throttleComponent;
                if (invertR) leanComponent = -leanComponent;
            }
        }

        boolean inside = (leanComponent == 0f && throttleComponent == 0f);

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
            // Both sticks just dropped into their deadzones — release
            // physics state and stop. (In single mode "both" is "the only
            // stick"; in dual mode we wait until both are inside.)
            gd.gameView.setAnalogInput(0, 0);
            analogActive = false;
            return true;
        }

        // Sign convention: physics expects positive throttle = accel and
        // positive lean = forward (right). Android stick Y is +down, so
        // throttle = -y when reading the Y axis; X is +right, so a flipped
        // throttle (reading X) needs +x to keep right=accel natural. Lean
        // defaults to +x; if a flip moved lean to Y, the user can compose
        // Invert to taste — leaving the lean sign alone preserves the
        // existing behaviour for the unflipped path.
        int throttleSignedI = (int) ((throttleFromX ? throttleComponent : -throttleComponent) * 0x10000);
        int leanSignedI     = (int) ( leanComponent * 0x10000);

        gd.gameView.setAnalogInput(throttleSignedI, leanSignedI);
        analogActive = true;
        return true;
    }

    /** Resolve which Android axes carry the right stick on {@code device}.
     *  Most pads use {@link MotionEvent#AXIS_Z} / {@link MotionEvent#AXIS_RZ},
     *  some use {@link MotionEvent#AXIS_RX} / {@link MotionEvent#AXIS_RY},
     *  and a few put unipolar trigger pressure on Z/RZ — so we pick the pair
     *  the device declares as bipolar (range crosses zero with non-trivial
     *  extent). Result is cached per {@link InputDevice#getId()} on first
     *  call.
     *
     *  <p>If neither pair is bipolar (single-stick pad, or trigger pressure
     *  on Z/RZ with no right stick wired up), returns the sentinel
     *  {@code {-1, -1}} — the caller must read 0 for those axes rather than
     *  calling {@code event.getAxisValue(-1)}. This avoids the failure mode
     *  where pulling a trigger on a single-stick pad in a dual layout
     *  silently drove the bike. */
    private int[] resolveRightStickAxes(InputDevice device) {
        if (device == null) {
            return new int[]{MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ};
        }
        Integer key = device.getId();
        int[] cached = rightStickAxesByDevice.get(key);
        if (cached != null) return cached;

        // Lookup-time pruning: a new device showed up, so opportunistically
        // drop entries for IDs that are no longer present. Android can
        // reissue device IDs on disconnect/reconnect, so without this the
        // map would grow unbounded over a long session. Cheap — happens
        // only on cache miss (which itself is rare; once per pad per run).
        // No InputManager listener required.
        pruneStaleDeviceIds();

        int[] result;
        boolean zPair = isBipolarStickAxis(device, MotionEvent.AXIS_Z)
                && isBipolarStickAxis(device, MotionEvent.AXIS_RZ);
        boolean rPair = isBipolarStickAxis(device, MotionEvent.AXIS_RX)
                && isBipolarStickAxis(device, MotionEvent.AXIS_RY);
        if (zPair) {
            // Prefer Z/RZ when both are valid — it's the more common
            // convention and a pad that exposes both pairs likely treats
            // RX/RY as something else.
            result = new int[]{MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ};
        } else if (rPair) {
            result = new int[]{MotionEvent.AXIS_RX, MotionEvent.AXIS_RY};
        } else {
            // No bipolar pair — sentinel signals "no right stick on this
            // pad". dispatchAnalogStick reads 0 instead of querying these.
            result = new int[]{-1, -1};
        }
        rightStickAxesByDevice.put(key, result);
        return result;
    }

    /** Drop entries from {@link #rightStickAxesByDevice} whose device ID
     *  is no longer present according to {@link InputDevice#getDeviceIds()}.
     *  Called from {@link #resolveRightStickAxes} on cache miss so the map
     *  can't grow unbounded if Android reissues IDs across reconnects. */
    private void pruneStaleDeviceIds() {
        if (rightStickAxesByDevice.isEmpty()) return;
        int[] live = InputDevice.getDeviceIds();
        HashSet<Integer> liveSet = new HashSet<>(live.length);
        for (int id : live) liveSet.add(id);
        Iterator<Integer> it = rightStickAxesByDevice.keySet().iterator();
        while (it.hasNext()) {
            if (!liveSet.contains(it.next())) it.remove();
        }
    }

    /** True if {@code device} declares {@code axis} as a bipolar stick axis
     *  (range crosses zero, half-extent on each side). Filters out
     *  unipolar trigger axes that some pads put on Z/RZ. */
    private static boolean isBipolarStickAxis(InputDevice device, int axis) {
        InputDevice.MotionRange range = device.getMotionRange(axis, InputDevice.SOURCE_JOYSTICK);
        if (range == null) return false;
        return range.getMin() < -0.5f && range.getMax() > 0.5f;
    }

    /** True if the event's {@code source} actually carries {@code axis} on
     *  this {@code device}. Used to detect events that only cover one of
     *  the two sticks: if the source doesn't expose an axis,
     *  {@link MotionEvent#getAxisValue} silently returns 0 — which would
     *  otherwise look like a real "stick centred" reading and clobber the
     *  other stick's cached deflection. With a null device we fall back to
     *  "assume present" so legacy/unknown pads keep the pre-fix behaviour
     *  of trusting every event. */
    private static boolean sourceHasAxis(InputDevice device, int source, int axis) {
        if (device == null) return true;
        return device.getMotionRange(axis, source) != null;
    }

    /** Single-axis deadzone + scaling for dual-stick layouts. The stick's
     *  other axis is ignored entirely — drift on it never reaches physics. */
    private static float mapAxis1D(float v, float deadzone, boolean digital) {
        if (digital) {
            return v > deadzone ? 1f : (v < -deadzone ? -1f : 0f);
        }
        float abs = v >= 0 ? v : -v;
        if (abs < deadzone) return 0f;
        float scaled = (abs - deadzone) / (1f - deadzone);
        if (scaled > 1f) scaled = 1f;
        return v >= 0 ? scaled : -scaled;
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
