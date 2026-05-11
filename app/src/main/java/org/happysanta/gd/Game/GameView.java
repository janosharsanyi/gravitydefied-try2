package org.happysanta.gd.Game;

import android.graphics.*;
import android.view.View;
import org.happysanta.gd.Command;
import org.happysanta.gd.GDActivity;
import org.happysanta.gd.Global;
import org.happysanta.gd.Menu.Menu;
import org.happysanta.gd.Menu.MenuScreen;
import org.happysanta.gd.Menu.SimpleMenuElement;
import org.happysanta.gd.Settings;

import java.io.IOException;
import java.util.Timer;

import static org.happysanta.gd.Helpers.*;

public class GameView extends View {

	private static int m_VI = 0;
	private static int m_vcI = 0;
	private final int[] startFlagIndexes = {
			2, 0, 1, 0
	};
	private final int[] finishFlagIndexes = {
			4, 3, 5, 3
	};
	public int m_abI;
	public int m_dI;
	public int m_lI;
	public boolean drawTimer;
	public android.graphics.Bitmap m_zaBitmap[];
	public boolean m_KZ;
	public long m_rJ;
	Menu menu;
	int m_uI;
	int m_aiI;
	int m_agI;
	int m_gI;
	private Canvas canvas;
	// Reused per-frame for the perspective-mode fill render path. Path
	// and Paint are allocated once at view construction and mutated in
	// place (rewind() keeps the vertex buffer; setColor(int) mutates an
	// existing field). No `new` in the per-segment hot path. AA is
	// deliberately off — the surrounding line render also draws without
	// AA, and matching that keeps fill edges crisp against the strokes.
	//
	// Skia call batching: the per-segment fill helpers no longer issue
	// drawPath; instead they accumulate sub-paths into these reused
	// Paths and a single drawPath per color is dispatched in
	// endTrackFill at the end of pass 2 in Level._aiIV. For N visible
	// segments this collapses N drawPath calls (solid mode) or 6×N
	// drawPath calls (gradient mode) down to 1 and 6 respectively —
	// payoff is on the JNI / Canvas boundary, not in rasterization.
	// fillPath holds the solid batch; gradientStripPaths[n] holds the
	// n-th strip color's batch. Strip-path array length matches the user-
	// configured Gradient steps (2/3/4/6/8/12); lazily grown by
	// beginTrackFill when the setting is changed at runtime.
	//
	// Each quad ABCD is decomposed into two triangles (ABC and ACD)
	// before being appended — triangles can't self-intersect (only 3
	// vertices) so bowtie quads on steep humps still rasterize cleanly,
	// and a per-triangle signed-area check (see appendTriangle) flips
	// vertex order when needed so every sub-path in the Path winds in
	// the same direction. Combined with the explicit WINDING fill type
	// set below, overlapping triangles sum to winding ≥ 1 → filled,
	// never cancel to 0 → holes. This is what makes batching safe; the
	// previous "append the raw quad" version punched holes through
	// the track on humps because opposite-wound sub-paths cancelled.
	private final Path fillPath = new Path();
	private Path[] gradientStripPaths = new Path[0];
	private final Paint fillPaint = new Paint();
	{
		fillPaint.setStyle(Paint.Style.FILL); /* AA stays off — seams handled by strip overlap, see fillTrackQuadGradient */
		// FillType is persistent across rewind(), so setting it once at
		// construction is enough. Default would already be WINDING; pin it
		// explicitly because the whole batching scheme depends on it.
		fillPath.setFillType(Path.FillType.WINDING);
	}
	private int m_XI;
	private int m_BI;
	private Physics physEngine;
	private int m_TI;
	private int m_QI;
	private GDActivity activity;
	private Paint infoFont;
	private Paint timerFont;

	// FPS / frame-time overlay (Settings.isFpsOverlayEnabled()). Sampled
	// at the top of onDraw — onDraw cadence ≈ vsync because we
	// invalidate() at the bottom of each pass, so these intervals
	// reflect actual on-screen frame times. 30-frame ring buffer
	// smooths single-frame jitter while still reacting in ~half a
	// second on a 60Hz panel; anything bigger feels laggy when you're
	// watching the number.
	private static final int FPS_SAMPLE_FRAMES = 30;
	private final long[] fpsSamplesNs = new long[FPS_SAMPLE_FRAMES];
	private int fpsSampleIdx = 0;
	private int fpsSampleCount = 0;
	private long fpsLastFrameNs = 0;
	private boolean m_ahZ;
	private int m_oI;
	private boolean m_AZ;
	private int m_OI;
	private android.graphics.Bitmap m_MBitmap;
	private Canvas m_dcGraphics;
	private boolean m_ecZ;
	private String infoMessage;
	private int gc;
	private Timer timer;
	private Command menuCommand;
	private Paint paint = new Paint();
	// Cached Paint with a colour-matrix filter that inverts RGB channels and
	// caps the result at 0xAA. Used to tint mostly-black sprite assets (bike
	// wheels) so they read against the dark-mode sky. Built lazily on first
	// use; settings flips between this and {@code null} via {@link
	// #getSpriteTintPaint()}.
	private Paint darkSpritePaint;
	// Cached Paint with a multiplicative dim filter (~70%) for sprite bike
	// parts that already carry their own colors (engine, fender, steering).
	// Unlike {@link #darkSpritePaint} this preserves hue — it just lowers the
	// brightness so the parts don't pop too hard against the dark sky.
	private Paint darkBikePartPaint;
	// Cached Paint with a full RGB invert (no cap). Used for the splash
	// logos in dark mode so the original black-on-white art renders as
	// white-on-black — the muted-gray cap on {@link #darkSpritePaint} left
	// the logo text too faint to read.
	private Paint darkSplashInvertPaint;
	private Object m_ocObject;
	private byte[][] m_DaaB = {
			{0, 0},
			{1, 0},
			{0, -1},
			{0, 0},
			{0, 0},
			{0, 1},
			{-1, 0}
	};
	private byte[][][] m_maaaB = {
			{
					{
							0, 0
					},
					{
							1, -1
					},
					{
							1, 0
					},
					{
							1, 1
					},
					{
							0, -1
					},
					{
							-1, 0
					},
					{
							0, 1
					},
					{
							-1, -1
					},
					{
							-1, 0
					},
					{
							-1, 1
					}
			}, {
			{
					0, 0
			},
			{
					1, 0
			},
			{
					0, 0
			},
			{
					0, 0
			},
			{
					-1, 0
			},
			{
					0, -1
			},
			{
					0, 1
			},
			{
					0, 0
			},
			{
					0, 0
			},
			{
					0, 0
			}
	},
			{
					{
							0, 0
					},
					{
							0, 0
					},
					{
							0, 0
					},
					{
							1, 0
					},
					{
							0, -1
					},
					{
							0, 1
					},
					{
							-1, 0
					},
					{
							0, 0
					},
					{
							0, 0
					},
					{
							0, 0
					}
			}
	};
	private int inputOption;
	private boolean[] m_aeaZ;
	private boolean[] m_LaZ;
	// Cached signed-magnitude analog stick values (.16 fixed-point), updated
	// on each motion event from the gamepad in {@link #setAnalogInput} and
	// merged with the digital key sums in {@link #applyMergedInput} so the
	// two input paths can coexist per-axis (e.g. button-held gas overrides
	// the stick's throttle component while the stick keeps driving lean).
	private int analogThrottleSignedI = 0;
	private int analogLeanSignedI = 0;
	// private int defaultHeight;
	// private int defaultWidth;

	public GameView(GDActivity micro) {
		super(micro);
		// clear static
		m_vcI = 0;
		m_VI = 0;

		canvas = null;
		physEngine = null;
		menu = null;
		m_TI = 0;
		m_QI = 0;
		activity = null;
		infoFont = null;
		m_ahZ = false;
		drawTimer = true;
		m_oI = 1;
		m_uI = 0;
		m_zaBitmap = null;

		m_KZ = false;
		m_AZ = true;
		m_MBitmap = null;
		m_dcGraphics = null;
		m_ecZ = false;
		infoMessage = null;
		gc = 0;
		timer = new Timer();
		m_rJ = -1L;
		m_ocObject = new Object();
		m_aiI = 0;
		m_agI = 0;
		m_gI = -1;
		inputOption = 2;
		m_aeaZ = new boolean[7];
		m_LaZ = new boolean[10];
		// String s;
		// String s1;
		paint.setAntiAlias(true);
		paint.setStrokeWidth(1);

		invalidate();
		m_KZ = false;
		activity = micro;
		_ifvV();

		infoFont = new Paint();
		infoFont.setTextSize(20);
		infoFont.setAntiAlias(true);
		infoFont.setTypeface(Global.robotoCondensedTypeface);

		timerFont = new Paint();
		timerFont.setTextSize(18);
		timerFont.setAntiAlias(true);
		timerFont.setTypeface(Global.robotoCondensedTypeface);

		m_XI = 0;
		m_BI = m_dI;
		menuCommand = new Command("Menu", 1, 1);
	}

	public void drawBitmap(Bitmap b, float x, float y) {
		drawBitmap(b, x, y, canvas);
	}

	public void drawBitmap(Bitmap b, float x, float y, Canvas g) {
		drawBitmap(b, x, y, g, null);
	}

	/**
	 * Draws a sprite with an optional tint Paint. Pass {@code tint == null}
	 * for normal (untinted) rendering; pass a Paint with a ColorFilter (see
	 * {@link #getSpriteTintPaint()}) to recolor the sprite — used so the
	 * mostly-black bike wheels stay legible on the dark-mode sky without
	 * needing a separate set of bitmap assets.
	 */
	public void drawBitmap(Bitmap b, float x, float y, Canvas g, Paint tint) {
		Paint paint = tint;
		if (paint == null && !isSDK11OrHigher()) {
			paint = new Paint();
			paint.setFlags(Paint.DITHER_FLAG);
			paint.setFilterBitmap(true);
		}
		g.drawBitmap(b.bitmap,
				new Rect(0, 0, b.getWidth(), b.getHeight()),
				new RectF(x, y, x + b.getWidthDp(), y + b.getHeightDp()),
				paint);
	}

	/**
	 * Returns a cached Paint that inverts a sprite's RGB channels and caps
	 * each at 0xAA, or {@code null} when dark mode is off (so callers can
	 * pass it straight to {@link #drawBitmap(Bitmap, float, float, Canvas,
	 * Paint)} without branching).
	 *
	 * <p>The matrix maps black (0,0,0) → (170,170,170) so dark wheel pixels
	 * become a muted mid-gray, and white → black so any highlights flip
	 * symmetrically. Cached because the colour matrix and filter would
	 * otherwise allocate every frame inside the render loop.
	 */
	private Paint getSpriteTintPaint() {
		if (!Settings.isDarkModeEnabled()) {
			return null;
		}
		if (darkSpritePaint == null) {
			darkSpritePaint = new Paint();
			darkSpritePaint.setFilterBitmap(true);
			ColorMatrix m = new ColorMatrix(new float[]{
					-0.7f, 0,    0,    0, 170,
					0,    -0.7f, 0,    0, 170,
					0,     0,   -0.7f, 0, 170,
					0,     0,    0,    1,   0,
			});
			darkSpritePaint.setColorFilter(new ColorMatrixColorFilter(m));
		}
		return darkSpritePaint;
	}

	/**
	 * Returns a cached Paint that multiplicatively dims a sprite by ~30% (so
	 * a bright pixel renders at ~70% of its original brightness), or
	 * {@code null} when dark mode is off. Hue is preserved — unlike
	 * {@link #getSpriteTintPaint()} this does not invert.
	 *
	 * <p>Use for sprite bike parts (engine, fender, steering) that already
	 * carry their own colors and just need to be a touch less bright against
	 * the dark sky. Cached for the same per-frame allocation reason.
	 */
	/**
	 * Returns a cached Paint that fully inverts a sprite's RGB channels
	 * (slope -1, offset 255), or {@code null} when dark mode is off. Maps
	 * black → white and white → black with no cap, so the splash-screen
	 * logos read clearly against the black backdrop.
	 */
	private Paint getSplashInvertPaint() {
		if (!Settings.isDarkModeEnabled()) {
			return null;
		}
		if (darkSplashInvertPaint == null) {
			darkSplashInvertPaint = new Paint();
			darkSplashInvertPaint.setFilterBitmap(true);
			ColorMatrix m = new ColorMatrix(new float[]{
					-1, 0,  0, 0, 255,
					 0, -1, 0, 0, 255,
					 0, 0, -1, 0, 255,
					 0, 0,  0, 1,   0,
			});
			darkSplashInvertPaint.setColorFilter(new ColorMatrixColorFilter(m));
		}
		return darkSplashInvertPaint;
	}

	private Paint getSpriteBikePartPaint() {
		if (!Settings.isDarkModeEnabled()) {
			return null;
		}
		if (darkBikePartPaint == null) {
			darkBikePartPaint = new Paint();
			darkBikePartPaint.setFilterBitmap(true);
			ColorMatrix m = new ColorMatrix(new float[]{
					0.7f, 0,    0,    0, 0,
					0,    0.7f, 0,    0, 0,
					0,    0,    0.7f, 0, 0,
					0,    0,    0,    1, 0,
			});
			darkBikePartPaint.setColorFilter(new ColorMatrixColorFilter(m));
		}
		return darkBikePartPaint;
	}

	public static void _dovV() {
		m_vcI += 655;
		int j = 32768 + ((FPMath.sin(m_vcI) >= 0 ? FPMath.sin(m_vcI) : -FPMath.sin(m_vcI)) >> 1);
		m_VI += (int) (6553L * (long) j >> 16);
	}

	/*
	TODO
	суть этого метода в том, что после того, как splash-картинки проигрались, они удаляются из памяти, т.к. они больше нафиг не нужны
	видимо это было критично на старых телефонах
	можно и тут в принципе сделать
	 */
	public void _doIV(int j) {
		m_oI = j;
		if (j == 0) {
			// Bitmap.get(Bitmap.CODEBREW_LOGO) = null;
			// Bitmap.get(Bitmap.GD_LOGO) = null;
		}
	}

	public void _aZV(boolean flag) {
		m_AZ = flag;
		_ifvV();
	}

	public void _ifvV() {
		m_abI = getScaledWidth();
		m_lI = m_dI = getScaledHeight();
		if (m_KZ && m_AZ)
			m_dI -= 80;
		//postInvalidate();
	}

	public android.graphics.Bitmap[] spritesFromBitmap(android.graphics.Bitmap bitmap, int j, int k) {
		int l = bitmap.getWidth() / j;
		int i1 = bitmap.getHeight() / k;
		android.graphics.Bitmap aBitmap[] = new android.graphics.Bitmap[j * k];
		for (int j1 = 0; j1 < j * k; j1++) {
			aBitmap[j1] = android.graphics.Bitmap.createBitmap(l, i1, android.graphics.Bitmap.Config.ARGB_8888);
			new Canvas(aBitmap[j1]).drawBitmap(bitmap, -l * (j1 % j), -i1 * (j1 / j), null);
		}

		return aBitmap;
	}

	public int _intII(int j) {
		synchronized (m_ocObject) {
			try {
				{
					if ((j & 1) != 0) {
						/* try {
							if (Bitmap.get(Bitmap.FENDER) == null) {
								//Bitmap.get(Bitmap.FENDER) = Bitmap.fromAsset("/fender.png");
								//Bitmap.get(Bitmap.FENDER).mulWidth = 1.0f/6.0f;
								//Bitmap.get(Bitmap.FENDER).mulHeight = 1.0f/6.0;
								Bitmap.get(Bitmap.FENDER) = Bitmap.fromDrawable(R.drawable.s_fender);
							}
							if (Bitmap.get(Bitmap.ENGINE) == null) {
								//Bitmap.get(Bitmap.ENGINE) = Bitmap.fromAsset("/engine.png");
								//Bitmap.get(Bitmap.ENGINE).mulHeight = 1.0f/6.0f;
								//Bitmap.get(Bitmap.ENGINE).mulWidth = 1.0f/6.0f;
								Bitmap.get(Bitmap.ENGINE) = Bitmap.fromDrawable(R.drawable.s_engine);
							}
						} catch (Throwable _ex) {
							Bitmap.get(Bitmap.FENDER) = Bitmap.get(Bitmap.ENGINE) = null;
							j &= -2;
						} */
					} else {
						// Bitmap.get(Bitmap.ENGINE) = Bitmap.get(Bitmap.FENDER) = null;
						// System.gc();
					}
					if ((j & 2) != 0) {
						// blueleg
						/*try {
							if (bikerSprites[1] == null)
								bikerSprites[1] = Bitmap.fromDrawable(R.drawable.s_blueleg);
						} catch (Throwable _ex) {
							bikerSprites[1] = null;
							bikerSprites[0] = null;
							bikerSprites[2] = null;
							j &= -3;
							System.out.println("There may be error");
							return 0;
						}*/

						// bluearm
						/*try {
							bikerSprites[0] = Bitmap.fromDrawable(R.drawable.s_bluearm);
						} catch (Throwable _ex) {
							bikerSprites[0] = bikerSprites[1];
						}

						// bluebody
						try {
							bikerSprites[2] = Bitmap.fromDrawable(R.drawable.s_bluebody);
						} catch (Throwable _ex) {
							bikerSprites[2] = bikerSprites[1];
						}*/
					} else {
						// bikerSprites[1] = bikerSprites[2] = bikerSprites[0] = null;
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("There may be error [1]");
			}
		}
		return j;
	}

	public android.graphics.Bitmap[] loadSprites(String s, int j, int k) {
		android.graphics.Bitmap bitmap;
		try {
			bitmap = loadBitmapFromAsset(s);
		} catch (IOException _ex) {
			android.graphics.Bitmap aBitmap[] = new android.graphics.Bitmap[j * k];
			for (int l = 0; l < j * k; l++)
				aBitmap[l] = Bitmap.getEmpty().bitmap;

			return aBitmap;
		}
		return spritesFromBitmap(bitmap, j, k);
	}

	public void _casevV() {
		physEngine._nullvV();
		_avV();
		m_aiI = 0;
		m_agI = 0;
	}

	public void setPhysicsEngine(Physics b1) {
		physEngine = b1;
		physEngine._caseIV(m_abI >= m_dI ? m_dI : m_abI);
	}

	public void setMenu(Menu menu) {
		this.menu = menu;
	}

	/* public Bitmap _aStringBitmap(String s) {
		Bitmap bitmap = null;
		try {
			bitmap = Global.loadBitmapFromAsset(s);
		} catch (IOException _ex) {
		}
		return bitmap;
	} */

	public void _doIIV(int j, int k) {
		m_XI = j;
		m_BI = k;
		physEngine._ifIIV(-j, -j + m_abI);
	}

	public int _charvI() {
		return m_XI;
	}

	private float offsetX(float j) {
		return j + m_XI;
	}

	private float offsetY(float j) {
		// Compensate for the keypad's vertical footprint at the centre of
		// the screen — full strip height in portrait, zero in landscape
		// where the split keypad leaves the bottom centre clear. Old code
		// used getButtonsLayoutHeight() unconditionally, which left the
		// bike top-shifted by ~105dp in landscape.
		return -j + m_BI - getGDActivity().getPlayfieldBottomReservation() / 2;
	}

	public void _newvV() {
		paint.setColor(0xFFFFFFFF);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawRect(0, m_dI, m_abI, 80, paint);
		byte byte0 = 35;
		int j = m_abI / 2;
		int k = m_dI + 40;
		// m_CGraphics.setColor(150, 0, 0);
		paint.setColor(0xFF960000);
		int i1;
		paint.setStyle(Paint.Style.STROKE);
		if (m_aiI != 0 || m_agI != 0) {
			i1 = (int) (((long) (int) ((long) m_OI * 0xb40000L >> 16) << 32) / 0x3243fL >> 16) >> 16;
			int l = i1 - i1 % 45;
			l -= 90;
			//m_CGraphics.fillArc(j - byte0, k - byte0, 2 * byte0, 2 * byte0, l - 22, 45);
			canvas.drawArc(new RectF(j - byte0, k - byte0, 2 * byte0 + j - byte0, 2 * byte0 + k - byte0), l - 22, 45, true, paint);
		}
		//m_CGraphics.setColor(0, 0, 0);
		paint.setColor(0xFF000000);
		canvas.drawArc(new RectF(j - byte0, k - byte0, 2 * byte0 + j - byte0, 2 * byte0 + k - byte0), 0, 360, true, paint);
		byte0 = 2;
		canvas.drawArc(new RectF(j - byte0, k - byte0, 2 * byte0 + j - byte0, 2 * byte0 + k - byte0), 0, 360, true, paint);
		paint.setStyle(Paint.Style.FILL);
	}

	public void _aIIIV(int j, int k, int l, int i1) {
		canvas.drawLine(offsetX(j), offsetY(k), offsetX(l), offsetY(i1), paint);
	}

	// Begin a perspective-mode fill frame. Rewinds the per-color Paths
	// the per-segment helpers append into. Caller is Level.drawSegment
	// (per-segment) — must be paired with endTrackFill in the same
	// segment. Cheap (rewind preserves the underlying vertex buffer).
	// Lazily grows the strip-path array when gradN exceeds current
	// capacity (user changed the Gradient steps setting to a larger N).
	public void beginTrackFill(int fillMode, int gradN) {
		if (fillMode == Settings.MAP_FILL_MODE_OFF)
			return;
		if (fillMode == Settings.MAP_FILL_MODE_GRADIENT) {
			if (gradientStripPaths.length < gradN) {
				Path[] grown = new Path[gradN];
				System.arraycopy(gradientStripPaths, 0, grown, 0, gradientStripPaths.length);
				for (int i = gradientStripPaths.length; i < gradN; i++) {
					Path p = new Path();
					p.setFillType(Path.FillType.WINDING);
					grown[i] = p;
				}
				gradientStripPaths = grown;
			}
			for (int n = 0; n < gradN; n++) gradientStripPaths[n].rewind();
		} else {
			fillPath.rewind();
		}
	}

	// Flush the segment's accumulated fills. Solid modes (FG / BG /
	// THIRD): 1 drawPath for the segment quad. Gradient mode: gradN
	// drawPath calls (one per strip color), drawn strip 0 → strip
	// gradN-1 so the higher-strip color wins where adjacent strips
	// overlap by the seam-closing overdraw added in fillTrackQuadGradient.
	public void endTrackFill(int fillMode, int fillSolidColor, int[] stripColors, int gradN) {
		if (fillMode == Settings.MAP_FILL_MODE_OFF)
			return;
		if (fillMode == Settings.MAP_FILL_MODE_GRADIENT) {
			for (int n = 0; n < gradN; n++) {
				fillPaint.setColor(stripColors[n]);
				canvas.drawPath(gradientStripPaths[n], fillPaint);
			}
		} else {
			fillPaint.setColor(fillSolidColor);
			canvas.drawPath(fillPath, fillPaint);
		}
	}

	// Append one perspective-mode track segment quadrilateral to the
	// solid-fill batch. The quad is ABCD with A,B on the ground curve
	// and D,C on the raised projection. Same (coord << 3) >> 16 shift
	// the line render uses so the fill aligns pixel-exactly with the
	// strokes drawn over it. No drawPath here — the whole batch is
	// flushed by endTrackFill.
	//
	// Decomposed into two triangles (ABC + ACD) — see class-level
	// comment on fillPath for why. The per-triangle winding fixup in
	// appendTriangle is what keeps the batch hole-free on humps.
	//
	// Each segment is inflated along the A→B / D→C track direction by
	// ~1 pixel on each side so adjacent segments overlap at their
	// shared across edge. The painter-z order of segments overpaints
	// the overlap on whichever side ends up last → no sub-pixel sky
	// seam at the cross-tick line. Same rationale as the along-track
	// overlap in fillTrackQuadGradient; needed independently here
	// because each segment in solid mode is now per-segment-flushed
	// (since the painter-z fix), no longer one giant Path.
	public void fillTrackQuadSolid(int ax, int ay, int bx, int by,
								   int cx, int cy, int dx, int dy) {
		float aX = offsetX((ax << 3) >> 16), aY = offsetY((ay << 3) >> 16);
		float bX = offsetX((bx << 3) >> 16), bY = offsetY((by << 3) >> 16);
		float cX = offsetX((cx << 3) >> 16), cY = offsetY((cy << 3) >> 16);
		float dX = offsetX((dx << 3) >> 16), dY = offsetY((dy << 3) >> 16);

		final float OVERLAP_PX = 1.0f;
		float abLx = bX - aX, abLy = bY - aY;
		float abUx = cX - dX, abUy = cY - dY;
		float abLLen = (float) Math.hypot(abLx, abLy);
		float abULen = (float) Math.hypot(abUx, abUy);
		float epsAlongL = abLLen > 0f ? OVERLAP_PX / abLLen : 0f;
		float epsAlongU = abULen > 0f ? OVERLAP_PX / abULen : 0f;
		float aXn = aX - epsAlongL * abLx, aYn = aY - epsAlongL * abLy;
		float bXn = bX + epsAlongL * abLx, bYn = bY + epsAlongL * abLy;
		float cXn = cX + epsAlongU * abUx, cYn = cY + epsAlongU * abUy;
		float dXn = dX - epsAlongU * abUx, dYn = dY - epsAlongU * abUy;

		appendTriangle(fillPath, aXn, aYn, bXn, bYn, cXn, cYn);
		appendTriangle(fillPath, aXn, aYn, cXn, cYn, dXn, dYn);
	}

	// Append one triangle to a Path as a closed 3-vertex sub-path, with
	// vertex order normalized to a single consistent winding direction.
	// The sign of the (×2) signed area picks which way the input is
	// already wound; we flip the last two vertices when needed so every
	// triangle in the host Path winds the same way. Under the WINDING
	// fill rule this means overlapping triangles' winding numbers sum
	// to ≥ 1 inside the union (filled) and never cancel to 0 (holes).
	// Without this fixup, bowtie quads on steep humps decompose into
	// triangles with opposite orientation and rasterize as a punched-
	// out band — the "fills not filling all the rectangles / hills
	// inside out" symptom from the unbatched-quad version.
	private static void appendTriangle(Path p,
									   float x0, float y0,
									   float x1, float y1,
									   float x2, float y2) {
		float cross = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0);
		p.moveTo(x0, y0);
		if (cross >= 0f) {
			p.lineTo(x1, y1);
			p.lineTo(x2, y2);
		} else {
			p.lineTo(x2, y2);
			p.lineTo(x1, y1);
		}
		p.close();
	}

	// Append one perspective-mode track segment quadrilateral to the
	// gradient-fill batch. The quad ABCD is sliced into N=gradN
	// trapezoidal bands parallel to the ground edge AB and the raised
	// edge DC; each band's sub-path goes into gradientStripPaths[n],
	// which gets painted in one drawPath call by endTrackFill with the
	// matching color from stripColors (pre-computed once per frame by
	// Level._aiIV via interpArgb). gradN is the user-configurable
	// Gradient steps setting (2/3/4/6/8/12).
	public void fillTrackQuadGradient(int ax, int ay, int bx, int by,
									  int cx, int cy, int dx, int dy,
									  int[] stripColors, int gradN) {
		final int N = gradN;
		float aX = offsetX((ax << 3) >> 16), aY = offsetY((ay << 3) >> 16);
		float bX = offsetX((bx << 3) >> 16), bY = offsetY((by << 3) >> 16);
		float cX = offsetX((cx << 3) >> 16), cY = offsetY((cy << 3) >> 16);
		float dX = offsetX((dx << 3) >> 16), dY = offsetY((dy << 3) >> 16);

		// Per-edge ground→raised vectors (left edge A→D, right edge B→C).
		// Strip n nominally covers [n/N .. (n+1)/N] along these edges.
		float lDx = dX - aX, lDy = dY - aY;
		float rDx = cX - bX, rDy = cY - bY;

		// Each strip is inflated by ~OVERLAP_PX in screen space along
		// every direction it borders an adjacent fill region, so that
		// rasterization sub-pixel slop at boundaries never lets sky
		// color leak through:
		//
		//   - Across direction (between strip n and strip n+1 in the
		//     same segment): each strip's upper edge extends past the
		//     nominal boundary by epsT (in t-space along A→D / B→C).
		//     The next strip starts at the nominal boundary, so its
		//     color overwrites the overhang — no visible boundary
		//     shift, but the boundary pixel is always covered.
		//   - Along-track direction (between adjacent segments): each
		//     strip's left edge extends past A→D by epsAlong toward
		//     the previous segment (in the −A→B / −D→C direction) and
		//     the right edge extends past B→C toward the next segment
		//     in the +A→B / +D→C direction. Adjacent segments share
		//     coordinates exactly on their common across edge, but
		//     the top-left fill rule plus floating-point coord math
		//     could otherwise leave a sub-pixel seam right where the
		//     across-tick rib is drawn — the user-visible "sky shows
		//     through at the cross-ticks" artifact. Both sides extend,
		//     so the painter-z order of segments doesn't matter; the
		//     last-painted segment wins the seam pixel.
		//
		// ε for the across overlap uses the shorter ground→raised edge
		// length so the screen-space overlap stays ≥ OVERLAP_PX on
		// both edges. Likewise for the along-track overlap, computed
		// independently per upper/lower edge. The outermost strip's
		// overshoots (t1 > 1, and the segment-edge extensions) sit
		// behind the BG/FG line strokes which paint last in the per-
		// segment overlay step, so they're hidden.
		final float OVERLAP_PX = 1.0f;
		float lLen = (float) Math.hypot(lDx, lDy);
		float rLen = (float) Math.hypot(rDx, rDy);
		float edgeLen = lLen < rLen ? lLen : rLen;
		float epsT = edgeLen > 0f ? OVERLAP_PX / edgeLen : 0f;

		// Along-track vectors (A→B for the lower edge, D→C for the
		// upper edge). Used to nudge each strip's left/right corners
		// slightly past the segment boundary.
		float abLx = bX - aX, abLy = bY - aY;
		float abUx = cX - dX, abUy = cY - dY;
		float abLLen = (float) Math.hypot(abLx, abLy);
		float abULen = (float) Math.hypot(abUx, abUy);
		float epsAlongL = abLLen > 0f ? OVERLAP_PX / abLLen : 0f;
		float epsAlongU = abULen > 0f ? OVERLAP_PX / abULen : 0f;

		for (int n = 0; n < N; n++) {
			float t0 = (float) n / N;
			float t1 = (float) (n + 1) / N + epsT;

			float lLx = aX + t0 * lDx, lLy = aY + t0 * lDy; // left  lower
			float lRx = bX + t0 * rDx, lRy = bY + t0 * rDy; // right lower
			float uLx = aX + t1 * lDx, uLy = aY + t1 * lDy; // left  upper
			float uRx = bX + t1 * rDx, uRy = bY + t1 * rDy; // right upper

			// Along-track extensions: lower corners follow the A→B
			// direction; upper corners follow the D→C direction. Each
			// scaled by epsAlong* so it's exactly OVERLAP_PX in screen
			// space along that edge.
			lLx -= epsAlongL * abLx; lLy -= epsAlongL * abLy;
			lRx += epsAlongL * abLx; lRy += epsAlongL * abLy;
			uLx -= epsAlongU * abUx; uLy -= epsAlongU * abUy;
			uRx += epsAlongU * abUx; uRy += epsAlongU * abUy;

			// Strip quad (lL, lR, uR, uL) → two triangles. Same triangle
			// split + per-triangle winding fixup as the solid path, so
			// hump overlaps within gradientStripPaths[n] don't cancel.
			Path p = gradientStripPaths[n];
			appendTriangle(p, lLx, lLy, lRx, lRy, uRx, uRy);
			appendTriangle(p, lLx, lLy, uRx, uRy, uLx, uLy);
		}
	}

	public void drawLine(int j, int k, int l, int i1) {
		canvas.drawLine(offsetX((j << 2) / (float) 0xFFFF), offsetY((k << 2) / (float) 0xFFFF), offsetX((l << 2) / (float) 0xFFFF), offsetY((i1 << 2) / (float) 0xFFFF), paint);
	}

	public void _aIIIV(int j, int k, int l, int i1, int j1) {
		drawBikerPart(j, k, l, i1, j1, 32768);
	}

	public void drawBikerPart(int j, int k, int l, int i1, int j1, int k1) {
		float l1 = offsetX((int) ((long) l * (long) k1 >> 16) + (int) ((long) j * (long) (0x10000 - k1) >> 16) >> 16);
		float i2 = offsetY((int) ((long) i1 * (long) k1 >> 16) + (int) ((long) k * (long) (0x10000 - k1) >> 16) >> 16);
		int j2 = FPMath._ifIII(l - j, i1 - k);
		int index = 0;
		switch (j1) {
			case 0: // '\0'
				index = _aIIII(j2, 0, 0x3243f, 16, false);
				break;

			case 1: // '\001'
				index = _aIIII(j2, 0, 0x3243f, 16, false);
				break;

			case 2: // '\002'
				index = _aIIII(j2, 0, 0x3243f, 16, false);
				break;
		}
		float fAngleDeg = (float) (j2 / (float) 0xFFFF / Math.PI * 180) - 180;
		index = 0;

		Bitmap bikerSprite = Bitmap.get(Bitmap.BIKER, j1);
		if (bikerSprite != null) {
			float x = l1 - bikerSprite.getWidthDp() / 2;
			float y = i2 - bikerSprite.getHeightDp() / 2;

			canvas.save();
			canvas.rotate(fAngleDeg, x + bikerSprite.getWidthDp() / 2, y + bikerSprite.getHeightDp() / 2);
			drawBitmap(bikerSprite, x, y);
			canvas.restore();
		}
	}

	// �������� �����
	public void _ifIIIV(int j, int k, int l, int i1) {
		l++;
		float j1 = offsetX(j - l);
		float k1 = offsetY(k + l);
		int l1 = l << 1;
		if ((i1 = -(int) (((long) (int) ((long) i1 * 0xb40000L >> 16) << 32) / 0x3243fL >> 16)) < 0)
			i1 += 360;
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawArc(new RectF(j1, k1, j1 + l1, k1 + l1), -((i1 >> 16) + 170), -90, false, paint);
		paint.setStyle(Paint.Style.FILL);
	}

	// Draws red circle
	public void drawLineWheel(float j, float k, int l) {
		float i1 = l / 2;
		float j1 = offsetX(j - i1);
		float k1 = offsetY(k + i1);

		paint.setStyle(Paint.Style.STROKE);
		canvas.drawArc(new RectF(j1, k1, j1 + l, k1 + l), 0, 360, true, paint);
		paint.setStyle(Paint.Style.FILL);
	}

	public void _forIIIV(int j, int k, int l, int i1) {
		float j1 = offsetX(j);
		float k1 = offsetY(k);
		canvas.drawRect(j1, k1, j1 + l, k1 + i1, paint);
	}

	public void drawSteering(int j, int k) {
		float x = offsetX(j - Bitmap.get(Bitmap.STEERING).getWidthDp() / 2);
		float y = offsetY(k + Bitmap.get(Bitmap.STEERING).getHeightDp() / 2);

		drawBitmap(Bitmap.get(Bitmap.STEERING), x, y, canvas, getSpriteBikePartPaint());
	}

	public void drawHelmet(float j, float k, int l) {
		float fAngleDeg = (float) (l / (float) 0xFFFF / Math.PI * 180) - 90 - 10;
		if (fAngleDeg >= 360) fAngleDeg -= 360;
		if (fAngleDeg < 0) fAngleDeg = 360 + fAngleDeg;
		if (Bitmap.get(Bitmap.HELMET) != null) {
			float x = offsetX(j) - Bitmap.get(Bitmap.HELMET).getWidthDp() / 2;
			float y = offsetY(k) - Bitmap.get(Bitmap.HELMET).getHeightDp() / 2;
			canvas.save();
			canvas.rotate(fAngleDeg, x + Bitmap.get(Bitmap.HELMET).getWidthDp() / 2, y + Bitmap.get(Bitmap.HELMET).getHeightDp() / 2);

			drawBitmap(Bitmap.get(Bitmap.HELMET), x, y);
			canvas.restore();
		}
	}

	public void _ifIIIIV(int j, int k, int l, int i1, int j1, int k1) {
	}

	public void drawTimer(long l) {
		// logDebug("Timer: " + l);
		String txt = String.format("%d:%02d:%02d", l / 6000, (l / 100) % 60, l % 100);
		// logDebug("drawTimter: long = " + l + ", string = " + txt);
		// Pick color from Settings each frame so the dark-mode toggle flips
		// the timer without us having to wire a separate notification.
		// Paint defaults to BLACK if never set, which used to be fine on
		// the white sky but vanishes against the dark-mode sky.
		timerFont.setColor(getOverlayTextColor());
		canvas.drawText(txt, 18, -infoFont.ascent() + 17, timerFont);
	}

	/**
	 * Top-center diagnostic overlay: average frame interval (ms) and the
	 * derived frame rate (fps) over the last {@link #FPS_SAMPLE_FRAMES}
	 * frames. Mirrors {@link #drawTimer}'s font, color, and Y baseline so
	 * the timer and FPS read line up visually. Centered (rather than
	 * top-right) to clear the in-game 3-dot overflow menu in the top-right
	 * corner. Caller is expected to gate this on
	 * {@link Settings#isFpsOverlayEnabled()}.
	 */
	public void drawFps() {
		if (fpsSampleCount == 0) return;
		long sum = 0;
		for (int i = 0; i < fpsSampleCount; i++) sum += fpsSamplesNs[i];
		double avgNs = (double) sum / fpsSampleCount;
		double ms = avgNs / 1_000_000.0;
		double fps = avgNs > 0 ? 1_000_000_000.0 / avgNs : 0;
		// Cap displayed fps at 999 to keep the string width bounded; on a
		// 120Hz panel real numbers stay around 60-120 anyway.
		String txt = String.format("%.1fms / %.0ffps", ms, Math.min(fps, 999));
		timerFont.setColor(getOverlayTextColor());
		float x = (getScaledWidth() - timerFont.measureText(txt)) / 2f;
		canvas.drawText(txt, x, -infoFont.ascent() + 17, timerFont);
	}

	/**
	 * Color for in-game text overlays (timer, info messages like
	 * "Finished" / "Crashed"). Tracks {@link Settings#getMenuFgColor()},
	 * but dims to mid-grey while a menu is overlaid so the overlay text
	 * sinks behind the menu chrome — same intent the original
	 * {@link #setColor(int, int, int)} brightening had for black-on-white,
	 * generalised to work for both dark and light themes.
	 */
	private int getOverlayTextColor() {
		if (getGDActivity().isMenuShown()) {
			return 0xff808080;
		}
		return Settings.getMenuFgColor();
	}

	public synchronized void showInfoMessage(String s, int j) {
		m_ahZ = false;
		gc++;
		infoMessage = s;
		if (timer != null) {
			timer.schedule(new SimpleMenuElement(gc), j);
		}
	}

	public void _tryIV(int j) {
		if (gc == j)
			m_ahZ = true;
	}

	public void drawStartFlag(int j, int k) {
		if (m_VI > 0x38000)
			m_VI = 0;
		setColor(0, 0, 0);
		_aIIIV(j, k, j, k + 32);
		drawBitmap(Bitmap.get(Bitmap.FLAGS, startFlagIndexes[m_VI >> 16]), offsetX(j), offsetY(k) - 32);
	}

	public void drawFinishFlag(int j, int k) {
		if (m_VI > 0x38000)
			m_VI = 0;
		setColor(0, 0, 0);
		_aIIIV(j, k, j, k + 32);
		drawBitmap(Bitmap.get(Bitmap.FLAGS, finishFlagIndexes[m_VI >> 16]), offsetX(j), offsetY(k) - 32);
	}

	public void drawWheel(float j, float k, int l) {
		int wheel;
		if (l == 1)
			wheel = 0; // small
		else
			wheel = 1; // big

		float x = offsetX(j - Bitmap.get(Bitmap.WHEELS, wheel).getWidthDp() / 2);
		float y = offsetY(k + Bitmap.get(Bitmap.WHEELS, wheel).getHeightDp() / 2);

		// Tint when dark mode is on; pass-through (null) otherwise. The tint
		// paint inverts black sprite pixels to a soft light-gray so the
		// wheels stay legible on the dark sky.
		drawBitmap(Bitmap.get(Bitmap.WHEELS, wheel), x, y, canvas, getSpriteTintPaint());
	}

	int _aIIII(int j, int k, int l, int i1, boolean flag) {
		for (j += k; j < 0; j += l) ;
		for (; j >= l; j -= l) ;
		if (flag)
			j = l - j;
		int j1;
		if ((j1 = (int) ((long) (int) (((long) j << 32) / (long) l >> 16) * (long) (i1 << 16) >> 16)) >> 16 < i1 - 1)
			return j1 >> 16;
		else
			return i1 - 1;
	}

	public void drawEngine(float j, float k, int l) {
		float fAngleDeg = (float) (l / (float) 0xFFFF / Math.PI * 180) - 180;
		float x = offsetX(j) - Bitmap.get(Bitmap.ENGINE).getWidthDp() / 2;
		float y = offsetY(k) - Bitmap.get(Bitmap.ENGINE).getHeightDp() / 2;
		if (Bitmap.get(Bitmap.ENGINE) != null) {
			canvas.save();
			canvas.rotate(fAngleDeg, x + Bitmap.get(Bitmap.ENGINE).getWidthDp() / 2, y + Bitmap.get(Bitmap.ENGINE).getHeightDp() / 2);
			drawBitmap(Bitmap.get(Bitmap.ENGINE), x, y, canvas, getSpriteBikePartPaint());
			canvas.restore();
		}
	}

	public void drawFender(float j, float k, int l) {
		float fAngleDeg = (float) (l / (float) 0xFFFF / Math.PI * 180) - 180 + 15;
		if (fAngleDeg >= 360) fAngleDeg -= 360;
		if (Bitmap.get(Bitmap.FENDER) != null) {
			float x = offsetX(j) - Bitmap.get(Bitmap.FENDER).getWidthDp() / 2;
			float y = offsetY(k) - Bitmap.get(Bitmap.FENDER).getHeightDp() / 2;

			canvas.save();
			canvas.rotate(fAngleDeg, x + Bitmap.get(Bitmap.FENDER).getWidthDp() / 2, y + Bitmap.get(Bitmap.FENDER).getHeightDp() / 2);
			drawBitmap(Bitmap.get(Bitmap.FENDER), x, y, canvas, getSpriteBikePartPaint());
			canvas.restore();
		}
	}

	public void _tryvV() {
		// In-game sky / clear color. Reads from Settings each frame so the
		// dark-mode toggle takes effect on the next render without needing
		// to rebuild anything.
		paint.setColor(Settings.getMenuBgColor());
		canvas.drawRect(0, 0, m_abI, m_dI, paint);
	}

	public void setColor(int r, int g, int b) {
		GDActivity _tmp = activity;
		boolean dark = Settings.isDarkModeEnabled();
		// Dark mode: two-branch remap so structural lines stay visible on the
		// black sky without losing the hue of intentional accent colors.
		//
		// (a) Near-black inputs (max channel < 96) — flag poles, line wheels,
		//     bike frame (50,50,50), driver arms, steering. Complement and
		//     cap at 0xAA so they read as a muted mid-gray (was 0xCC; user
		//     asked for the wheels and bike to be a touch darker).
		// (b) Mid-bright inputs (96 ≤ max < 200) — driver body (0,0,128) and
		//     helmet line (156,0,0). Brighten each channel additively so the
		//     hue stays intact (blue body remains blue) but the line becomes
		//     readable on black; the original light-mode values were tuned
		//     against a white background and would otherwise sink.
		// (c) Already-saturated inputs (max ≥ 200) — brake indicators
		//     (255,0,0) / (100,100,255). Pass through unchanged — they're
		//     plenty visible on either background and changing the hue would
		//     muddle the in-game signal.
		if (dark) {
			int max = r;
			if (g > max) max = g;
			if (b > max) max = b;
			if (max < 96) {
				r = 255 - r;
				g = 255 - g;
				b = 255 - b;
				if (r > 0xAA) r = 0xAA;
				if (g > 0xAA) g = 0xAA;
				if (b > 0xAA) b = 0xAA;
			} else if (max < 200) {
				r += 96;
				g += 96;
				b += 96;
				if (r > 255) r = 255;
				if (g > 255) g = 255;
				if (b > 255) b = 255;
			}
		}
		if (getGDActivity().isMenuShown()) {
			if (dark) {
				// Mirror the light-mode "lift toward background" dim:
				// background is black, so push the color *down* by 128
				// (clamped) so overlay graphics sink behind the menu chrome.
				r -= 128;
				g -= 128;
				b -= 128;
				if (r < 16) r = 16;
				if (g < 16) g = 16;
				if (b < 16) b = 16;
			} else {
				r += 128;
				g += 128;
				b += 128;
				if (r > 240)
					r = 240;
				if (g > 240)
					g = 240;
				if (b > 240)
					b = 240;
			}
		}
		//m_CGraphics.setColor(j, k, l);
		paint.setColor(0xFF000000 | (r << 16) | (g << 8) | b);
	}

	// Set the paint to a raw ARGB color, bypassing setColor's dark-mode
	// remap and the menu-shown dim. Used by render paths that have already
	// chosen the exact pixel they want (e.g. neon shadow with alpha fade) —
	// re-running them through setColor would, for example, send a faded
	// near-black through the "structural line" invert branch and turn it
	// bright instead of letting it disappear.
	public void setRawArgb(int argb) {
		paint.setColor(argb);
	}

	// Draw boot logos and something else
	public void drawGame(Canvas g) {
		final GDActivity gd = getGDActivity();
		label0:
		{
			int j;
			synchronized (m_ocObject) {
				if (gd.alive && !gd.m_caseZ)
					break label0;
			}
			return;
		}

		if (m_ecZ)
			canvas = m_dcGraphics;
		else
			canvas = g;
		if (m_oI != 0) {
			// Splash background tracks the dark-mode toggle so the loading
			// screens don't flash white before the menu loads. Logos are
			// drawn with the sprite tint paint when dark mode is on so the
			// (mostly-black) artwork stays visible against the black bg.
			if (m_oI == 1) {
				// Draw codebrew
				paint.setColor(Settings.getMenuBgColor());
				canvas.drawRect(0, 0, getScaledWidth(), getScaledHeight(), paint);
				if (Bitmap.get(Bitmap.CODEBREW_LOGO) != null) {
					drawBitmap(Bitmap.get(Bitmap.CODEBREW_LOGO),
							getScaledWidth() / 2 - Bitmap.get(Bitmap.CODEBREW_LOGO).getWidthDp() / 2,
							(float) (getScaledHeight() / 2 - Bitmap.get(Bitmap.CODEBREW_LOGO).getHeightDp() / 1.6),
							canvas, getSplashInvertPaint());
				}
			} else {
				// Draw gd
				paint.setColor(Settings.getMenuBgColor());
				canvas.drawRect(0, 0, getScaledWidth(), getScaledHeight(), paint);
				if (Bitmap.get(Bitmap.GD_LOGO) != null) {
					drawBitmap(Bitmap.get(Bitmap.GD_LOGO),
							getScaledWidth() / 2 - Bitmap.get(Bitmap.GD_LOGO).getWidthDp() / 2,
							(float) (getScaledHeight() / 2 - Bitmap.get(Bitmap.GD_LOGO).getHeightDp() / 1.6),
							canvas, getSplashInvertPaint());
				}
			}
			int j = (int) (((long) (gd.m_longI << 16) << 32) / 0xa0000L >> 16);
			drawProgress(j, true);
		} else {
			if (m_lI != getHeight())
				_ifvV();
			physEngine._voidvV();
			_doIIV(-physEngine._elsevI() + m_TI + m_abI / 2, physEngine._ifvI() + m_QI + m_dI / 2);
			physEngine._ifiV(this);
			if (drawTimer) {
				long time = 0, finished;
				if (gd.startedTime > 0) {
					if (gd.finishedTime > 0)
						finished = gd.finishedTime;
					else
						finished = System.currentTimeMillis();
					time = (finished - gd.startedTime - gd.pausedTime) / 10;
				}
				drawTimer(time);
			}
			// FPS / frame-time overlay opposite-corner from the timer. Drawn
			// regardless of whether drawTimer is on — useful for benchmarking
			// menu→track transitions, splash skip, etc.
			if (Settings.isFpsOverlayEnabled()) {
				drawFps();
			}
			if (infoMessage != null) {
				// Was: setColor(0, 0, 0); infoFont.setColor(paint.getColor());
				// — hardcoded black, brightened to mid-grey when menu shown.
				// Replaced with getOverlayTextColor() so the message reads
				// correctly against either light or dark sky.
				infoFont.setColor(getOverlayTextColor());
				/*if (m_dI <= 128)
					canvas.drawText(infoMessage, m_abI / 2 - infoFont.measureText(infoMessage) / 2, 1, infoFont);
				else*/

				canvas.drawText(infoMessage, m_abI / 2 - infoFont.measureText(infoMessage) / 2, m_dI / 5, infoFont);
				if (m_ahZ) {
					m_ahZ = false;
					infoMessage = null;
				}
			}
			int j = physEngine._tryvI();
			drawProgress(j, false);
			if (m_KZ && m_AZ)
				_newvV();
		}
		canvas = null;
		if (m_ecZ)
			g.drawBitmap(m_MBitmap, 0, 0, null);
	}

	public void drawProgress(int j, boolean flag) {
		double progr = j / (double) 0xFFFF;

		// Dark mode: dark-grey bar over a black sky (the original light-grey
		// reads as a glaring stripe), with a darker green fill so the
		// progress indicator doesn't pop too hard either.
		boolean dark = Settings.isDarkModeEnabled();
		paint.setColor(dark ? 0xff404040 : 0xffc4c4c4);
		canvas.drawRect(0, 0, getScaledWidth(), 3, paint);

		paint.setColor(dark ? 0xff185a17 : 0xff29aa27);
		canvas.drawRect(0, 0, Math.round(getScaledWidth() * Math.min(Math.max(progr, 0), 1)), 3, paint);
	}

	private void _ifIIV(int j, int k) {
		if (!getGDActivity().isMenuShown()) {
			byte byte0 = 0;
			byte byte1 = 0;
			m_aiI = j;
			m_agI = k;
			int l = j << 16;
			int i1 = k << 16;
			int j1 = m_abI / 2 << 16;
			int k1 = m_dI + 40 << 16;
			if (m_KZ && m_AZ) {
				int l1 = FPMath._ifIII(l - j1, i1 - k1);
				for (l1 += 25735; l1 < 0; l1 += 0x6487e) ;
				for (; l1 > 0x6487e; l1 -= 0x6487e) ;
				m_OI = l1;
				int i2;
				if ((i2 = 51471) >= l1)
					byte0 = -1;
				else if (l1 < (int) ((long) i2 * 0x20000L >> 16)) {
					byte0 = -1;
					byte1 = 1;
				} else if (l1 < (int) ((long) i2 * 0x30000L >> 16))
					byte1 = 1;
				else if (l1 < (int) ((long) i2 * 0x40000L >> 16)) {
					byte0 = 1;
					byte1 = 1;
				} else if (l1 < (int) ((long) i2 * 0x50000L >> 16))
					byte0 = 1;
				else if (l1 < (int) ((long) i2 * 0x60000L >> 16)) {
					byte0 = 1;
					byte1 = -1;
				} else if (l1 < (int) ((long) i2 * 0x70000L >> 16))
					byte1 = -1;
				else if (l1 < (int) ((long) i2 * 0x80000L >> 16)) {
					byte0 = -1;
					byte1 = -1;
				}
				applyMergedInput(byte0, byte1);
			}
		}
	}

	public void _pointerPressedIIV(int j, int k) {
		if (!getGDActivity().isMenuShown())
			_ifIIV(j, k);
	}

	public void _pointerReleasedIIV(int j, int k) {
		if (!getGDActivity().isMenuShown()) {
			m_aiI = 0;
			m_agI = 0;
			physEngine._nullvV();
		}
	}

	public void _pointerDraggedIIV(int j, int k) {
		if (!getGDActivity().isMenuShown())
			_ifIIV(j, k);
	}

	public void setInputOption(int option) {
		inputOption = option;
	}

	private void _avV() {
		for (int j = 0; j < 10; j++)
			m_LaZ[j] = false;

		for (int k = 0; k < 7; k++)
			m_aeaZ[k] = false;

	}

	private void _xavV() {
		int j = 0;
		int k = 0;
		int l = inputOption;
		for (int i1 = 0; i1 < 10; i1++)
			if (m_LaZ[i1]) {
				j += m_maaaB[l][i1][0];
				k += m_maaaB[l][i1][1];
			}

		for (int j1 = 0; j1 < 7; j1++)
			if (m_aeaZ[j1]) {
				j += m_DaaB[j1][0];
				k += m_DaaB[j1][1];
			}

		applyMergedInput(j, k);
	}

	protected void processKeyPressed(int j) {
		int k = getGameAction(j);
		int l;
		if ((l = j - 48) >= 0 && l < 10)
			m_LaZ[l] = true;
		else if (k >= 0 && k < 7)
			m_aeaZ[k] = true;
		_xavV();
	}

	protected void processKeyReleased(int j) {
		int k = getGameAction(j);
		int l;
		if ((l = j - 48) >= 0 && l < 10)
			m_LaZ[l] = false;
		else if (k >= 0 && k < 7)
			m_aeaZ[k] = false;
		_xavV();
	}

	public void showMenu() {
		if (menu != null) {
			menu.m_blZ = true;
			getGDActivity().gameToMenu();
		}
	}

	/* protected void hideNotify() {
		if (!getGDActivity().isMenuShown()) {
			GDActivity.m_cZ = true;
			activity.gameToMenu();
		}
	} */

	/* protected void showNotify() {
		GDActivity.m_cZ = false;
	} */

	protected void keyRepeated(int j) {
		if (getGDActivity().isMenuShown() && menu != null)
			menu._tryIV(j);
	}

	public synchronized void keyPressed(int j) {
		// Snapshot menu state *before* dispatching: menu.keyPressed can
		// call menuToGame() mid-call (e.g. FIRE on a "Resume" item),
		// flipping isMenuShown() to false before the gate below would see
		// it. Without this snapshot, the menu-closing keypress would still
		// bleed into m_LaZ and perturb physics on the next _dovI tick —
		// reproducible as a brief phantom lean on every pause-menu close
		// in keysets 1 / 2 (where numpad-5 has gameplay meaning).
		boolean wasInMenu = getGDActivity().isMenuShown();
		if (wasInMenu && menu != null)
			menu.keyPressed(j);
		// Skip gameplay key-state updates while a menu is up: m_LaZ /
		// m_aeaZ should reflect what the user is holding *for gameplay*,
		// and the menu has its own key-handling path. Releases stay
		// unconditional below so a key held into the menu still clears
		// cleanly when the user lets go.
		if (!wasInMenu) processKeyPressed(j);
	}

	public synchronized void keyReleased(int j) {
		processKeyReleased(j);
	}

	/**
	 * Re-fire the merged input path with the current digital key state.
	 * Idempotent: if {@link #m_LaZ} / {@link #m_aeaZ} already reflect what
	 * physics has stored, this is a no-op. Used by
	 * {@link GDActivity#menuToGame()} as a defensive resync — guards
	 * against any code path where the physics engine's stored
	 * {@code _aIIVAnalog} flags could drift from the user's actual held
	 * keys across a menu transition.
	 */
	public synchronized void resyncMergedInput() {
		_xavV();
	}

	/**
	 * Analog input bridge — pushes signed throttle / lean magnitudes from a
	 * gamepad stick straight into the physics engine, bypassing the
	 * digital-numpad-key path that keyboard / on-screen keypad / D-pad use.
	 *
	 * <p>Both args are signed fixed-point .16 in {@code [-0x10000, +0x10000]}
	 * (i.e. {@code 0x10000 = 1.0}). Sign convention matches
	 * {@link Physics#_aIIV}: positive throttle = accelerate, negative =
	 * brake; positive lean = forward, negative = back. Pass {@code (0, 0)}
	 * when the stick returns to its deadzone to release physics state.
	 */
	public synchronized void setAnalogInput(int throttleSignedI, int leanSignedI) {
		analogThrottleSignedI = throttleSignedI;
		analogLeanSignedI = leanSignedI;
		if (physEngine == null) return;
		// Re-fire the merged path so any currently-held digital key still
		// dominates its axis (per-axis priority — see {@link #applyMergedInput}).
		// _xavV recomputes the digital sum from m_LaZ/m_aeaZ each call, which
		// is cheap and lets us keep all merge logic in one place.
		_xavV();
	}

	/**
	 * Merge digital key state with the cached analog stick deflection and
	 * push a single combined input to physics. Called from every digital
	 * input site (key event sums via {@link #_xavV}, on-screen virtual d-pad
	 * via {@link #_ifIIV}) and from {@link #setAnalogInput} on stick events.
	 *
	 * <p>Per-axis priority: a non-zero digital sign on an axis pins that
	 * axis to full magnitude (matches the original {@link Physics#_aIIV}
	 * "buttons mean full pull" semantics), while the other axis falls
	 * through to the stick's signed magnitude. With no digital input on
	 * either axis this is a pure stick frame; with no stick deflection
	 * it's a pure digital frame; either way we go through
	 * {@link Physics#_aIIVAnalog} so the unaffected axis preserves its
	 * proportional value when the user is mixing inputs (e.g. holding
	 * gas on the gamepad while leaning the stick).
	 *
	 * @param digitalThrottleSign {@code > 0} = accel held, {@code < 0} = brake held, 0 = none
	 * @param digitalLeanSign     {@code > 0} = lean forward held, {@code < 0} = back, 0 = none
	 */
	private void applyMergedInput(int digitalThrottleSign, int digitalLeanSign) {
		if (physEngine == null) return;
		int throttleOut = digitalThrottleSign != 0
				? (digitalThrottleSign > 0 ? 0x10000 : -0x10000)
				: analogThrottleSignedI;
		int leanOut = digitalLeanSign != 0
				? (digitalLeanSign > 0 ? 0x10000 : -0x10000)
				: analogLeanSignedI;
		physEngine._aIIVAnalog(throttleOut, leanOut);
	}

	@Override
	public void onDraw(Canvas g) {
		// Sample frame interval before any drawing work so the measured
		// time spans the full draw — ring buffer guarded by the toggle so
		// the no-overlay path stays one volatile-bool read per frame.
		if (Settings.isFpsOverlayEnabled()) {
			long now = System.nanoTime();
			if (fpsLastFrameNs != 0) {
				fpsSamplesNs[fpsSampleIdx] = now - fpsLastFrameNs;
				fpsSampleIdx = (fpsSampleIdx + 1) % FPS_SAMPLE_FRAMES;
				if (fpsSampleCount < FPS_SAMPLE_FRAMES) fpsSampleCount++;
			}
			fpsLastFrameNs = now;
		} else {
			// Reset so a re-enable doesn't blend a stale "frame" that spans
			// the disabled period (would read as 0.0fps for ~30 frames).
			fpsLastFrameNs = 0;
			fpsSampleCount = 0;
		}
		g.save();
		if (!Global.DISABLE_SCALING)
			g.scale(Global.density, Global.density);
		if (getGDActivity().isMenuShown() && menu != null) {
			menu.draw(g);
		} else {
			drawGame(g);
		}
		g.restore();
		invalidate();
	}

	public void commandAction(Command command) {
		if (getGDActivity().isMenuShown() && menu != null) {
			menu.onCommand(command);
		} else {
			showMenu();
		}
	}

	public void removeMenuCommand() {
		removeCommand(menuCommand);
	}

	public void addMenuCommand() {
		addCommand(menuCommand);
	}

	public static int getGameAction(int key) {
		// logDebug("getGameAction: key = " + key);
		switch (key) {
			case 50: // 2
				return MenuScreen.KEY_UP; // up
			case 56: // 8
				return MenuScreen.KEY_DOWN; // down
			case 52: // 4
				return MenuScreen.KEY_LEFT; // left
			case 54: // 6
				return MenuScreen.KEY_RIGHT; // right
			case 53: // 5
				return MenuScreen.KEY_FIRE; // fire
		}
		return 0;
	}

	public void addCommand(Command cmd) {
		GDActivity.shared.addCommand(cmd);
	}

	public void removeCommand(Command cmd) {
		GDActivity.shared.removeCommand(cmd);
	}

	public int getScaledWidth() {
		float density = Global.DISABLE_SCALING ? 1 : Global.density;
		return Math.round(getWidth() / density);
	}

	public int getScaledHeight() {
		float density = Global.DISABLE_SCALING ? 1 : Global.density;
		return Math.round(getHeight() / density);
	}

	@Override
	public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);

		int width = MeasureSpec.getSize(widthMeasureSpec), height = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(width, height);
	}

	public synchronized void destroy() {
		if (timer != null) {
			timer.cancel();
			timer.purge();
			timer = null;
		}
	}

}
