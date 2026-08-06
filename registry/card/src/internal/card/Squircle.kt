package com.lamintra.card.internal.card

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * A rounded rectangle whose corners are superelliptical arcs at a FIXED radius.
 *
 * Kept as a pure point generator, separate from the [Path] it feeds, so the
 * geometry can be unit-tested. Three earlier attempts at this shape shipped
 * broken - first a superellipse inscribed in the whole box (corner radius
 * scaled with element height, so every surface became a lozenge), then two
 * corner quadrants traversed in reverse, which turned them into straight
 * diagonal chords across the shape. Both survived a bounds check. Continuity
 * is the property that actually matters, and it is asserted in SquircleTest.
 */
internal object Squircle {

    /**
     * The outline, in draw order, starting at the top-left tangent point and
     * running clockwise. Consecutive points are either axis-aligned (the four
     * straight edges) or a short step along a corner arc - never a long
     * diagonal.
     */
    fun outline(
        width: Float,
        height: Float,
        radius: Float,
        n: Float = 4.2f,
        segments: Int = 16
    ): List<Offset> {
        val r = radius.coerceAtMost(minOf(width, height) / 2f)
        val pts = ArrayList<Offset>(segments * 4 + 8)

        // Superelliptical unit ramps. s goes 0 -> 1, c goes 1 -> 0.
        fun s(t: Float) = abs(sin(t * PI.toFloat() / 2f)).pow(2f / n)
        fun c(t: Float) = abs(cos(t * PI.toFloat() / 2f)).pow(2f / n)
        fun step(i: Int) = i.toFloat() / segments

        // Each corner is written out explicitly with its own start and end
        // point. The bug that produced diagonal chords came from reusing one
        // parametrisation for all four and getting two of them backwards.
        pts += Offset(r, 0f)
        pts += Offset(width - r, 0f)
        // top-right: (w-r, 0) -> (w, r)
        for (i in 0..segments) {
            val t = step(i)
            pts += Offset(width - r + r * s(t), r - r * c(t))
        }
        pts += Offset(width, height - r)
        // bottom-right: (w, h-r) -> (w-r, h)
        for (i in 0..segments) {
            val t = step(i)
            pts += Offset(width - r + r * c(t), height - r + r * s(t))
        }
        pts += Offset(r, height)
        // bottom-left: (r, h) -> (0, h-r)
        for (i in 0..segments) {
            val t = step(i)
            pts += Offset(r - r * s(t), height - r + r * c(t))
        }
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
