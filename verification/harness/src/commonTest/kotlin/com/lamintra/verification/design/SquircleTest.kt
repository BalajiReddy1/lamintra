package com.lamintra.verification.design

import kotlin.math.abs
import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Geometry gate for the squircle.
 *
 * Three earlier attempts at this shape shipped visibly broken, and both times
 * the check I ran passed: a bounds check and a flat-top-span check are both
 * satisfied by a shape whose corners are wrong. These tests assert the
 * properties that actually failed.
 */
class SquircleTest {

    private val w = 324f
    private val h = 46f
    private val r = 12f

    @Test
    fun cornerRadiusIsFixedAndDoesNotScaleWithHeight() {
        // The original bug inscribed a superellipse in the whole box, so the
        // corner radius became h/2. On a 98dp card that read as a stadium.
        val pts = Squircle.outline(324f, 98f, 18f)
        val flatTop = pts.filter { abs(it.y) < 0.01f }
        val span = flatTop.maxOf { it.x } - flatTop.minOf { it.x }
        assertEquals(324f - 2 * 18f, span, 0.5f, "top edge should be flat between the corners")
    }

    @Test
    fun outlineHasNoDiagonalChords() {
        // This is the check that would have caught the reversed corners: two
        // quadrants were traversed backwards, replacing each arc with a
        // straight diagonal — including one 312px slash across the bottom.
        val pts = Squircle.outline(w, h, r)
        val offenders = mutableListOf<String>()
        for (i in 1 until pts.size) {
            val a = pts[i - 1]
            val b = pts[i]
            val dx = b.x - a.x
            val dy = b.y - a.y
            val axisAligned = abs(dx) < 0.01f || abs(dy) < 0.01f
            val shortArcStep = hypot(dx, dy) <= r * 0.75f
            if (!axisAligned && !shortArcStep) {
                offenders += "(${a.x},${a.y})->(${b.x},${b.y}) len=${hypot(dx, dy)}"
            }
        }
        assertTrue(
            offenders.isEmpty(),
            "outline must be edges + short arc steps only; found diagonal chords: $offenders"
        )
    }

    @Test
    fun outlineIsClosedAndStaysInsideBounds() {
        val pts = Squircle.outline(w, h, r)
        assertTrue(pts.all { it.x >= -0.01f && it.x <= w + 0.01f }, "x out of bounds")
        assertTrue(pts.all { it.y >= -0.01f && it.y <= h + 0.01f }, "y out of bounds")
        assertEquals(w, pts.maxOf { it.x }, 0.01f)
        assertEquals(h, pts.maxOf { it.y }, 0.01f)
        assertEquals(0f, pts.minOf { it.x }, 0.01f)
        assertEquals(0f, pts.minOf { it.y }, 0.01f)
        // first and last points should meet once the path closes
        assertTrue(hypot(pts.first().x - pts.last().x, pts.first().y - pts.last().y) <= r * 0.75f)
    }

    @Test
    fun allFourCornersAreActuallyRounded() {
        // A reversed quadrant collapses a corner to a straight line. Each corner
        // region must contain points strictly inside its bounding square.
        val pts = Squircle.outline(w, h, r)
        fun cornerHasArc(cx: Float, cy: Float): Boolean = pts.any {
            val dx = abs(it.x - cx)
            val dy = abs(it.y - cy)
            dx in 0.5f..(r - 0.5f) && dy in 0.5f..(r - 0.5f)
        }
        assertTrue(cornerHasArc(0f, 0f), "top-left corner is not rounded")
        assertTrue(cornerHasArc(w, 0f), "top-right corner is not rounded")
        assertTrue(cornerHasArc(w, h), "bottom-right corner is not rounded")
        assertTrue(cornerHasArc(0f, h), "bottom-left corner is not rounded")
    }

    @Test
    fun radiusIsClampedOnSmallElements() {
        // Switch tracks ask for a pill: r = h/2 must not overflow.
        val pts = Squircle.outline(52f, 30f, 15f)
        assertTrue(pts.all { it.y >= -0.01f && it.y <= 30.01f })
        assertEquals(52f, pts.maxOf { it.x }, 0.01f)
    }
}
