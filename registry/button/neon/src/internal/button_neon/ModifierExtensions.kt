package com.lamintra.button.neon.internal.button_neon

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws the neon glow: a soft inner wash under a crisp edge, wrapped in a
 * halo of widening, fading strokes.
 *
 * Deliberately NOT `Modifier.shadow(ambientColor/spotColor)`. Coloured
 * shadows there are an Android-only capability — on iOS and desktop the
 * glow simply does not render, which is disqualifying for a component
 * whose whole pitch is one codebase for every target. Faking the blur
 * with layered strokes is the same technique `button/neon_outline` uses,
 * and it draws identically everywhere.
 *
 * Deliberately named identically to bottomsheet/glass's internal
 * ModifierExtensions.kt to serve as the collision test case for the CLI's
 * namespacing logic — these two files must never end up sharing a package
 * after install.
 */
internal fun Modifier.neonGlow(
    color: Color,
    cornerRadius: Dp,
    glowAlpha: Float
): Modifier = this.drawBehind {
    val radius = CornerRadius(cornerRadius.toPx())
    val coreWidth = 1.5.dp.toPx()

    // Inner wash, so the label sits on a lit surface rather than on nothing.
    drawRoundRect(
        color = color.copy(alpha = glowAlpha * 0.12f),
        cornerRadius = radius
    )
    // Crisp edge.
    drawRoundRect(
        color = color.copy(alpha = glowAlpha),
        cornerRadius = radius,
        style = Stroke(width = coreWidth)
    )
    // Halo — widening and fading outward.
    for (i in 1..3) {
        drawRoundRect(
            color = color.copy(alpha = glowAlpha * 0.22f / i),
            cornerRadius = radius,
            style = Stroke(width = coreWidth + i * 2.dp.toPx())
        )
    }
}
