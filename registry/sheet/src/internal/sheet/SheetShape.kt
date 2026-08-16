package com.lamintra.sheet.internal.sheet

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * A sheet outline: superelliptical corners on the TOP two only, square at the
 * bottom.
 *
 * The other components carry `Squircle`, which rounds all four. A sheet cannot
 * use it. The bottom edge of a sheet sits against the bottom of the screen (or
 * against the navigation-bar inset), so rounding it either produces two visible
 * notches where the card meets the edge, or - if the card is inset far enough
 * to clear them - a floating slab that reads as a dialog rather than something
 * anchored to the edge it slid in from.
 *
 * The corner maths is deliberately the same superellipse at the same exponent
 * as `Squircle`, because a sheet whose corners are a plain arc next to a card
 * whose corners are superelliptical is visible the moment they share a screen,
 * and it is the kind of difference nobody can name and everybody notices.
 */
internal object SheetShape {

    /**
     * Outline in draw order, clockwise from the top-left tangent point.
     *
     * [n] is the superellipse exponent; 4.2 matches `Squircle`. Consecutive
     * points are axis-aligned or a short step along a corner arc, never a long
     * diagonal - the failure mode that shipped three times in the Squircle work
     * and survived a bounds check every time.
     */
    fun outline(
        width: Float,
        height: Float,
        radius: Float,
        n: Float = 4.2f,
        segments: Int = 16
    ): List<Offset> {
        val r = radius.coerceAtMost(minOf(width, height) / 2f)
        val pts = ArrayList<Offset>(segments * 2 + 6)

        fun s(t: Float) = abs(sin(t * PI.toFloat() / 2f)).pow(2f / n)
        fun c(t: Float) = abs(cos(t * PI.toFloat() / 2f)).pow(2f / n)
        fun step(i: Int) = i.toFloat() / segments

        pts += Offset(r, 0f)
        pts += Offset(width - r, 0f)
        // top-right: (w-r, 0) -> (w, r)
        for (i in 0..segments) {
            val t = step(i)
            pts += Offset(width - r + r * s(t), r - r * c(t))
        }
        // straight down the right edge, across the bottom, up the left edge
        pts += Offset(width, height)
        pts += Offset(0f, height)
        pts += Offset(0f, r)
        // top-left: (0, r) -> (r, 0)
        for (i in 0..segments) {
            val t = step(i)
            pts += Offset(r - r * c(t), r - r * s(t))
        }
        return pts
    }

    fun path(width: Float, height: Float, radius: Float): Path {
        val pts = outline(width, height, radius)
        return Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
            close()
        }
    }
}
