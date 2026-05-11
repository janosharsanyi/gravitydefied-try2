package org.happysanta.gd.Levels;

import org.happysanta.gd.Game.GameView;
import org.happysanta.gd.Game.Physics;
import org.happysanta.gd.Settings;

import java.io.DataInputStream;

import static org.happysanta.gd.Helpers.getLevelLoader;
import static org.happysanta.gd.Helpers.logDebug;

public class Level {

	public int startX;
	public int startY;
	public int finishX;
	public int m_gotoI;
	public int m_forI;
	public int finishY;
	public int pointsCount;
	public int m_intI;
	public int points[][];
	public String levelName;
	private int m_aI;
	private int m_dI;
	private int m_eI;
	private int m_bI;
	private int m_gI;
	private int m_rI;
	// Average wheel Y, fed in alongside m_gI (body Y). Drives the neon
	// "proximity" metric so the glow follows the bike's *underside* rather
	// than its body — when the bike flips upside-down, wheels point up and
	// the neon correctly fades, even though the body is still near ground.
	// Classic shadow keeps using m_gI / m_rI so its behavior matches upstream.
	private int m_wheelAvgY;
	private int m_rI_neon;

	// Strip-fill gradient colors for perspective-mode fill, pre-computed
	// once per frame in _aiIV when fill mode == MAP_FILL_MODE_GRADIENT and
	// reused across every segment. N tracks the user-configurable
	// gradient-steps setting (Loader.getGradientSteps()) and is mirrored
	// by drawAcrossGradient so the strip-fill bands and across-tick rib
	// stay aligned. Lazy-grown when N changes — typical case is one
	// allocation per session, no GC in the hot path.
	private int[] stripColors = new int[6];

	// Per-POINT camera-ray offset cache used by the multi-pass perspective
	// render in _aiIV. pointOffsetX/Y[j] is the normalized .16 fixed-point
	// vector from points[j] toward the camera anchor, scaled by 4 — the
	// raised projection of points[j] is points[j] + (offsetX, offsetY).
	// Lazy-grown to pointsCount, reused across frames. Lets pass 2 (fills)
	// walk segments in non-forward z-order without redoing the
	// normalization math pass 1 already did.
	private int[] pointOffsetX;
	private int[] pointOffsetY;

	public Level() {
		m_aI = 0;
		m_dI = 0;
		m_eI = 0;
		m_bI = 0;
		m_gI = 0;
		m_gotoI = 0;
		m_forI = 0;
		points = (int[][]) null;
		levelName = "levelname";
		m_rI = 0;
		clear();
	}

	public void clear() {
		startX = 0;
		startY = 0;
		finishX = 0xc80000;
		pointsCount = 0;
		m_intI = 0;
	}

	public int _doII(int j) {
		int k = j - points[m_gotoI][0];
		int i1;
		if (((i1 = points[m_forI][0] - points[m_gotoI][0]) >= 0 ? i1 : -i1) < 3 || k > i1)
			return 0x10000;
		else
			return (int) (((long) k << 32) / (long) i1 >> 16);
	}

	public void _ifIIV(int j, int k) {
		m_aI = (j << 16) >> 3;
		m_dI = (k << 16) >> 3;
	}

	public void _aIIV(int j, int k) {
		m_eI = j >> 1;
		m_bI = k >> 1;
	}

	public void _aIIV(int j, int k, int i1) {
		m_eI = j;
		m_bI = k;
		m_gI = i1;
	}

	// Variant that also captures the average wheel Y for the neon proximity
	// metric. Existing 3-arg call site stays valid for callers that don't
	// have wheel info to share.
	public void _aIIV(int j, int k, int i1, int wheelAvgY) {
		_aIIV(j, k, i1);
		m_wheelAvgY = wheelAvgY;
	}

	public void _ifiIV(GameView view, int k, int i1) {
		if (i1 > pointsCount - 1) return;

		int midground = points[k][1] + points[i1 + 1][1] >> 1;

		// Body proximity (drives classic shadow). Same logic as upstream.
		int j1 = m_gI - midground >= 0 ? m_gI - midground : 0;
		if (m_gI <= points[k][1] || m_gI <= points[i1 + 1][1])
			j1 = j1 >= 0x50000 ? 0x50000 : j1;
		m_rI = (int) ((long) m_rI * 49152L >> 16) + (int) ((long) j1 * 16384L >> 16);

		// Wheel-avg proximity (drives neon). Same shape of computation, but
		// using the wheel-pair midpoint — when the bike flips upside-down,
		// wheels rise above the body and this metric grows accordingly, so
		// the neon (which conceptually emits from the bike's underside)
		// fades out instead of getting brighter as the body collapses to
		// ground.
		int j1n = m_wheelAvgY - midground >= 0 ? m_wheelAvgY - midground : 0;
		if (m_wheelAvgY <= points[k][1] || m_wheelAvgY <= points[i1 + 1][1])
			j1n = j1n >= 0x50000 ? 0x50000 : j1n;
		m_rI_neon = (int) ((long) m_rI_neon * 49152L >> 16) + (int) ((long) j1n * 16384L >> 16);

		int mode = getLevelLoader().getShadowMode();
		boolean isNeon = mode > Settings.SHADOW_MODE_SHADOW;
		boolean dark = Settings.isDarkModeEnabled();
		// Light mode + neon: draw the classic shadow underneath as a base,
		// then overlay the neon glow on top. Dark mode + neon: skip the
		// shadow because in dark mode setColor inverts the near-black
		// shadow color into a bright gray line, which fights the neon for
		// attention. Pure SHADOW mode always draws just the shadow.
		boolean drawShadow = (mode == Settings.SHADOW_MODE_SHADOW)
				|| (isNeon && !dark);
		boolean drawNeon = isNeon;

		// Shape calculation — same line endpoints regardless of color, so
		// compute once and stroke it once per active layer.
		int l1 = points[k][0] - points[k + 1][0];
		int i2 = (int) (((long) (points[k][1] - points[k + 1][1]) << 32) / (long) l1 >> 16);
		int j2 = points[k][1] - (int) ((long) points[k][0] * (long) i2 >> 16);
		int k2 = (int) ((long) m_eI * (long) i2 >> 16) + j2;
		l1 = points[i1][0] - points[i1 + 1][0];
		i2 = (int) (((long) (points[i1][1] - points[i1 + 1][1]) << 32) / (long) l1 >> 16);
		j2 = points[i1][1] - (int) ((long) points[i1][0] * (long) i2 >> 16);
		int l2 = (int) ((long) m_bI * (long) i2 >> 16) + j2;

		if (drawShadow && m_rI <= 0x88000) {
			int sk1 = (int) (0x190000L * (long) m_rI >> 16) >> 16;
			view.setColor(sk1, sk1, sk1);
			drawShadowSegments(view, k, i1, k2, l2);
		}

		if (drawNeon) {
			int floor = Settings.SHADOW_NEON_FULL_BELOW;
			int span = 0x88000 - floor;
			int eff = m_rI_neon < floor ? floor : m_rI_neon;
			// 16.16 fixed-point t to stay in step with surrounding code.
			int t = span > 0 ? (int) (((long) (eff - floor) << 16) / (long) span) : 0;
			if (t < 0) t = 0;
			if (t > 0x10000) t = 0x10000;
			int alpha = 0xff - (int) (((long) 0xff * t) >> 16);
			if (alpha < 0) alpha = 0;
			if (alpha > 0xff) alpha = 0xff;
			if (alpha > 0) {
				int base = Settings.getShadowNeonBaseColor(getLevelLoader().getNeonColor());
				// setRawArgb bypasses the dark-mode remap in setColor so
				// the neon hue passes through untouched and the alpha
				// channel actually takes effect on the line stroke.
				view.setRawArgb((alpha << 24) | (base & 0xffffff));
				drawShadowSegments(view, k, i1, k2, l2);
			}
		}
	}

	// Stroke the shadow shape (one straight line, or a polyline following
	// the track points between m_eI and m_bI). Pure rendering — color must
	// already be set on the view before calling.
	private void drawShadowSegments(GameView view, int k, int i1, int k2, int l2) {
		if (k == i1) {
			view._aIIIV((m_eI << 3) >> 16, (k2 + 0x10000 << 3) >> 16, (m_bI << 3) >> 16, (l2 + 0x10000 << 3) >> 16);
			return;
		}
		view._aIIIV((m_eI << 3) >> 16, (k2 + 0x10000 << 3) >> 16, (points[k + 1][0] << 3) >> 16, (points[k + 1][1] + 0x10000 << 3) >> 16);
		for (int i3 = k + 1; i3 < i1; i3++)
			view._aIIIV((points[i3][0] << 3) >> 16, (points[i3][1] + 0x10000 << 3) >> 16, (points[i3 + 1][0] << 3) >> 16, (points[i3 + 1][1] + 0x10000 << 3) >> 16);
		view._aIIIV((points[i1][0] << 3) >> 16, (points[i1][1] + 0x10000 << 3) >> 16, (m_bI << 3) >> 16, (l2 + 0x10000 << 3) >> 16);
	}

	public synchronized void _aiIV(GameView view, int k, int i1) {
		// Single uniform 3-line render. Every track-color preset (including
		// Original) flows through the same path; the only thing that varies
		// is the (fg, bg) pair we set per line. Colors set per draw via
		// setRawArgb so the dark-mode invert in setColor doesn't clobber
		// the palette. Naming follows the visual depth of the lines (see
		// Settings):
		//   fgColor → drawn on the lower/ground contour line (brighter)
		//   bgColor → drawn on the upper/raised projection line (darker)
		// Across tick gradients from fgColor at the ground end up to
		// bgColor at the raised end. Gradient transform: ON = identity,
		// INVERTED = swap FG/BG, OFF = collapse BG onto FG (so the across
		// gradient between two equal colors degenerates to a solid FG tick
		// and the raised line matches the ground line — single-color render
		// without a separate code path).
		int trackMode = getLevelLoader().getTrackColorMode();
		int rawFg = Settings.getTrackForegroundArgb(trackMode);
		int rawBg = Settings.getTrackBackgroundArgb(trackMode);
		int fgColor, bgColor;
		switch (getLevelLoader().getMapColorGradient()) {
			case Settings.MAP_COLOR_GRADIENT_INVERTED:
				fgColor = rawBg; bgColor = rawFg;
				break;
			case Settings.MAP_COLOR_GRADIENT_OFF:
				fgColor = rawFg; bgColor = rawFg;
				break;
			default: // MAP_COLOR_GRADIENT_ON
				fgColor = rawFg; bgColor = rawBg;
				break;
		}

		// Perspective-mode area-fill state. Read once per frame; used in
		// the per-segment loop below. Default behavior (fill OFF, ticks ON)
		// reproduces the wireframe render exactly — no visual diff on
		// upgrade for existing users.
		int fillMode = getLevelLoader().getMapFillMode();
		boolean ticksEnabled = getLevelLoader().isAcrossTicksEnabled();
		int fillSolidColor;
		switch (fillMode) {
			case Settings.MAP_FILL_MODE_FG:    fillSolidColor = fgColor; break;
			case Settings.MAP_FILL_MODE_BG:    fillSolidColor = bgColor; break;
			case Settings.MAP_FILL_MODE_THIRD: fillSolidColor = Settings.getTrackFillArgb(trackMode); break;
			default:                           fillSolidColor = 0; break;
		}
		// Gradient subdivision count — drives both the strip-fill bands
		// and the across-tick rib so they stay aligned regardless of
		// user setting (2/3/4/6/8/12).
		int gradN = getLevelLoader().getGradientSteps();
		if (fillMode == Settings.MAP_FILL_MODE_GRADIENT) {
			// Same (2n+1)/2N sample positions as drawAcrossGradient so a
			// strip's color sample sits at the same fraction along the
			// ground→raised axis as the matching tick sub-segment.
			if (stripColors.length < gradN)
				stripColors = new int[gradN];
			for (int n = 0; n < gradN; n++)
				stripColors[n] = interpArgb(fgColor, bgColor, 2 * n + 1, 2 * gradN);
		}

		// Lazy-grow the per-point camera-offset cache to fit the level.
		// Reused across frames once allocated — no GC pressure in steady
		// state.
		if (pointOffsetX == null || pointOffsetX.length < pointsCount) {
			pointOffsetX = new int[pointsCount];
			pointOffsetY = new int[pointsCount];
		}

		// PASS 1 — scan visible segment range, cache per-point camera-ray
		// offsets, track the shadow-mask boundaries (k2/l2) and the last
		// drawn segment index. No rendering yet. Mirrors the bookkeeping
		// the upstream single-pass loop did inline.
		int k2 = 0;
		int l2 = 0;
		int firstIdx;
		for (firstIdx = 0; firstIdx < pointsCount - 1 && points[firstIdx][0] <= m_aI; firstIdx++) ;
		if (firstIdx > 0)
			firstIdx--;

		cachePointOffset(firstIdx, k, i1);

		int j2 = firstIdx;
		int lastIdx = firstIdx;
		do {
			if (j2 >= pointsCount - 1)
				break;
			cachePointOffset(j2 + 1, k, i1);
			if (j2 > 1) {
				if (points[j2][0] > m_eI && k2 == 0)
					k2 = j2 - 1;
				if (points[j2][0] > m_bI && l2 == 0)
					l2 = j2 - 1;
			}
			lastIdx = j2;
			if (points[j2][0] > m_dI)
				break;
			j2++;
		} while (true);

		// PASS 2 + PASS 3 merged — each segment draws its fill and its
		// overlay (foreground contour, across-tick rib, raised line,
		// flags) together, in painter's z order. Splitting fills from
		// overlays was not enough: a far segment's raised line / tick /
		// ground line drawn after a near segment's fill would still
		// rasterize through the near hill's interior. Interleaving
		// per-segment means a later (closer) segment's fill covers
		// previously-drawn (farther) segment overlays at the overlap.
		//
		// Painter's z splits the visible range at playerIdx (segment
		// whose X range straddles the bike). Left half walks forward
		// (closest-on-left lands last on its half); right half + the
		// player segment walk in reverse (player segment is drawn very
		// last → on top of everything).
		//
		// OFF mode preserves the upstream forward-iteration overlay
		// render so the default settings stay bit-for-bit identical
		// against upstream — no fill exists to be occluded by, so the
		// hump artifact doesn't apply and we'd rather not perturb the
		// wireframe baseline.
		if (fillMode == Settings.MAP_FILL_MODE_OFF) {
			for (int idx = firstIdx; idx <= lastIdx; idx++)
				drawSegmentOverlay(view, idx, fgColor, bgColor, ticksEnabled, gradN);
		} else {
			int playerIdx = lastIdx;
			for (int idx = firstIdx; idx < lastIdx; idx++) {
				if (points[idx + 1][0] > k) {
					playerIdx = idx;
					break;
				}
			}
			for (int idx = firstIdx; idx < playerIdx; idx++)
				drawSegment(view, idx, fillMode, fillSolidColor, fgColor, bgColor, ticksEnabled, gradN);
			for (int idx = lastIdx; idx >= playerIdx; idx--)
				drawSegment(view, idx, fillMode, fillSolidColor, fgColor, bgColor, ticksEnabled, gradN);
		}

		// Trailing across tick at the last point, matching the in-loop ticks.
		// Uses the offset of (lastIdx + 1) — the right side of the final
		// drawn segment — preserving upstream behavior bit-for-bit.
		// Drawn last regardless of fill mode; it's a single 1-point-wide
		// rib at the rightmost edge of the visible track, any z-fighting
		// it might cause is bounded to that edge and not worth a special
		// case in the painter's loop above.
		if (ticksEnabled)
			drawAcrossGradient(view, points[pointsCount - 1][0], points[pointsCount - 1][1], pointOffsetX[lastIdx + 1], pointOffsetY[lastIdx + 1], fgColor, bgColor, gradN);
		if (getLevelLoader().isShadowsEnabled())
			_ifiIV(view, k2, l2);
	}

	// Computes and caches the normalized camera-ray offset for a single
	// point. Same fixed-point math the upstream loop did inline: vector
	// from point to camera anchor (k, i1 + 0x320000), normalized to .16
	// fixed-point then scaled by 4 (the (>> 1 >> 1) chain divides the
	// length by 4 before the divide, effectively multiplying the result).
	private void cachePointOffset(int pointIdx, int cameraX, int cameraY) {
		int dx = cameraX - points[pointIdx][0];
		int dy = (cameraY + 0x320000) - points[pointIdx][1];
		int len = Physics._doIII(dx, dy);
		pointOffsetX[pointIdx] = (int) (((long) dx << 32) / (long) (len >> 1 >> 1) >> 16);
		pointOffsetY[pointIdx] = (int) (((long) dy << 32) / (long) (len >> 1 >> 1) >> 16);
	}

	// One segment of the painter's-ordered merged render: fill first
	// (immediately dispatched, no cross-segment batching), overlay
	// after. Each call leaves the fill and overlay for segment `idx`
	// on the canvas as one composite unit, so the next call's fill
	// can overwrite both previous fill and previous overlay where it
	// geometrically overlaps. This is what kills the cross-segment
	// see-through-hill artifact for ticks / fg curve / bg curve.
	private void drawSegment(GameView view, int idx, int fillMode,
							  int fillSolidColor, int fgColor, int bgColor,
							  boolean ticksEnabled, int gradN) {
		view.beginTrackFill(fillMode, gradN);
		paintTrackQuad(view, idx, fillMode, gradN);
		view.endTrackFill(fillMode, fillSolidColor, stripColors, gradN);
		drawSegmentOverlay(view, idx, fgColor, bgColor, ticksEnabled, gradN);
	}

	// Draws one segment's overlay (foreground contour line, optional
	// across-tick rib, background raised line, start/finish flags).
	// In fill-on modes this runs immediately after the segment's fill
	// via drawSegment; in OFF mode it's the only per-segment work.
	private void drawSegmentOverlay(GameView view, int idx, int fgColor, int bgColor, boolean ticksEnabled, int gradN) {
		int j1 = pointOffsetX[idx];
		int l1 = pointOffsetY[idx];
		int i3 = pointOffsetX[idx + 1];
		int j3 = pointOffsetY[idx + 1];

		// Foreground contour: line between actual ground points
		// (lower / front line visually).
		view.setRawArgb(fgColor);
		view._aIIIV((points[idx][0] << 3) >> 16, (points[idx][1] << 3) >> 16, (points[idx + 1][0] << 3) >> 16, (points[idx + 1][1] << 3) >> 16);
		// Across tick from ground (fg) up to raised (bg). When Off
		// collapses bg onto fg this degenerates to a solid fg tick.
		if (ticksEnabled)
			drawAcrossGradient(view, points[idx][0], points[idx][1], j1, l1, fgColor, bgColor, gradN);
		// Background raised line (upper / back line visually).
		view.setRawArgb(bgColor);
		view._aIIIV((points[idx][0] + j1 << 3) >> 16, (points[idx][1] + l1 << 3) >> 16, (points[idx + 1][0] + i3 << 3) >> 16, (points[idx + 1][1] + j3 << 3) >> 16);

		if (m_gotoI == idx) {
			view.drawStartFlag((points[m_gotoI][0] + j1 << 3) >> 16, (points[m_gotoI][1] + l1 << 3) >> 16);
			// Restore the last color used in the iteration (raised line = bg).
			view.setRawArgb(bgColor);
		}
		if (m_forI == idx) {
			view.drawFinishFlag((points[m_forI][0] + j1 << 3) >> 16, (points[m_forI][1] + l1 << 3) >> 16);
			view.setRawArgb(bgColor);
		}
	}

	// Appends one perspective-mode quad to the batched fill Paths
	// (solid or per-strip-color), using the cached camera offsets.
	// Called from pass 2 of _aiIV between beginTrackFill / endTrackFill.
	private void paintTrackQuad(GameView view, int segIdx, int fillMode, int gradN) {
		int ax = points[segIdx][0],     ay = points[segIdx][1];
		int bx = points[segIdx + 1][0], by = points[segIdx + 1][1];
		int j1 = pointOffsetX[segIdx],     l1 = pointOffsetY[segIdx];
		int i3 = pointOffsetX[segIdx + 1], j3 = pointOffsetY[segIdx + 1];
		int cx = bx + i3, cy = by + j3;
		int dx = ax + j1, dy = ay + l1;
		if (fillMode == Settings.MAP_FILL_MODE_GRADIENT)
			view.fillTrackQuadGradient(ax, ay, bx, by, cx, cy, dx, dy, stripColors, gradN);
		else
			view.fillTrackQuadSolid(ax, ay, bx, by, cx, cy, dx, dy);
	}

	// Across tick rendered as N straight sub-segments from the ground point
	// (gx, gy) up to the raised projection (gx + dx, gy + dy). Color steps
	// from groundColor at the ground end to raisedColor at the raised end
	// — sampled at each sub-segment's midpoint, no shader, no per-frame
	// allocations. N is user-configurable (Gradient steps setting), default
	// 6 — same value drives the strip-fill subdivision so the rib aligns
	// with the strip boundaries.
	private static void drawAcrossGradient(GameView view, int gx, int gy, int dx, int dy, int groundColor, int raisedColor, int N) {
		for (int n = 0; n < N; n++) {
			int sx = gx + (int) ((long) dx * n / N);
			int sy = gy + (int) ((long) dy * n / N);
			int ex = gx + (int) ((long) dx * (n + 1) / N);
			int ey = gy + (int) ((long) dy * (n + 1) / N);
			view.setRawArgb(interpArgb(groundColor, raisedColor, 2 * n + 1, 2 * N));
			view._aIIIV((sx << 3) >> 16, (sy << 3) >> 16, (ex << 3) >> 16, (ey << 3) >> 16);
		}
	}

	// Linear interpolation between two opaque ARGB colors. num/den is the
	// fractional position of the result (0/den = a, den/den = b).
	// Alpha is forced to 0xff — the track render path is fully opaque.
	private static int interpArgb(int a, int b, int num, int den) {
		int ar = (a >> 16) & 0xff;
		int ag = (a >> 8) & 0xff;
		int ab = a & 0xff;
		int br = (b >> 16) & 0xff;
		int bg = (b >> 8) & 0xff;
		int bb = b & 0xff;
		int r = ar + (br - ar) * num / den;
		int g = ag + (bg - ag) * num / den;
		int bl = ab + (bb - ab) * num / den;
		return 0xff000000 | (r << 16) | (g << 8) | bl;
	}

	public synchronized void _aiV(GameView view) {
		// Flat / perspective-off render. Each segment is one line between
		// the actual ground points — visually the lower / foreground line
		// of the track. The gradient setting picks which preset color
		// paints it: ON / OFF use FG (the "primary"); INVERTED uses BG so
		// the swap stays consistent with the perspective-on render.
		int trackMode = getLevelLoader().getTrackColorMode();
		int rawFg = Settings.getTrackForegroundArgb(trackMode);
		int rawBg = Settings.getTrackBackgroundArgb(trackMode);
		int fgColor = (getLevelLoader().getMapColorGradient() == Settings.MAP_COLOR_GRADIENT_INVERTED)
				? rawBg : rawFg;
		view.setRawArgb(fgColor);
		int k;
		for (k = 0; k < pointsCount - 1 && points[k][0] <= m_aI; k++) ;
		if (k > 0)
			k--;
		do {
			if (k >= pointsCount - 1)
				break;
			view._aIIIV((points[k][0] << 3) >> 16, (points[k][1] << 3) >> 16, (points[k + 1][0] << 3) >> 16, (points[k + 1][1] << 3) >> 16);
			if (m_gotoI == k) {
				view.drawStartFlag((points[m_gotoI][0] << 3) >> 16, (points[m_gotoI][1] << 3) >> 16);
				view.setRawArgb(fgColor);
			}
			if (m_forI == k) {
				view.drawFinishFlag((points[m_forI][0] << 3) >> 16, (points[m_forI][1] << 3) >> 16);
				view.setRawArgb(fgColor);
			}
			if (points[k][0] > m_dI)
				break;
			k++;
		} while (true);
	}

	public void unpackInt(int x, int y) {
		addPoint((x << 16) >> 3, (y << 16) >> 3);
	}

	public void addPoint(int x, int y) {
		if (points == null || points.length <= pointsCount) {
			int i1 = 100;
			if (points != null)
				i1 = i1 >= points.length + 30 ? i1 : points.length + 30;
			int ai[][] = new int[i1][2];
			if (points != null)
				System.arraycopy(points, 0, ai, 0, points.length);
			points = ai;
		}
		if (pointsCount == 0 || points[pointsCount - 1][0] < x) {
			points[pointsCount][0] = x;
			points[pointsCount][1] = y;
			pointsCount++;
		}
	}

	public synchronized void readTrackData(DataInputStream in) {
		try {
			clear();
			if (in.readByte() == 50) {
				byte bytes[] = new byte[20];
				in.readFully(bytes);
			}
			m_forI = 0;
			m_gotoI = 0;
			startX = in.readInt();
			startY = in.readInt();
			finishX = in.readInt();
			finishY = in.readInt();
			short pointsCount = in.readShort();
			int firstPointX = in.readInt();
			int firstPointY = in.readInt();
			int k1 = firstPointX;
			int l1 = firstPointY;
			unpackInt(k1, l1);
			for (int i = 1; i < pointsCount; i++) {
				int x;
				int y;
				byte byte0;
				if ((byte0 = in.readByte()) == -1) {
					k1 = l1 = 0;
					x = in.readInt();
					y = in.readInt();
				} else {
					x = byte0;
					y = in.readByte();
				}
				k1 += x;
				l1 += y;
				unpackInt(k1, l1);
			}

			/*logDebug("Points: ");
			for (int[] point: points) {
				logDebug("(" + ((point[0] >> 16) << 3) + ", " + ((point[1] >> 16) << 3) + ")");
			}*/
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
