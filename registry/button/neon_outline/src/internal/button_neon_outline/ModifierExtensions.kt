package com.jetcompose.button.neon_outline.internal.button_neon_outline

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Draws the neon outline: one crisp core stroke plus three widening,
 * fading halo strokes that fake a blur without any platform-specific
 * blur API. Deliberately named ModifierExtensions.kt — the same filename
 * other components use for their internal helpers — because the
 * per-component `internal/<prefix>/` namespacing is what guarantees no
 * collision on install.
 */
internal fun Modifier.neonOutline(
    color: Color,
    cornerRadius: Dp,
    glowAlpha: Float
): Modifier = this.drawBehind {
    val radius = CornerRadius(cornerRadius.toPx())
    val coreWidth = 1.5.dp.toPx()
    drawRoundRect(
        color = color.copy(alpha = glowAlpha),
        cornerRadius = radius,
        style = Stroke(width = coreWidth)
    )
    for (i in 1..3) {
        drawRoundRect(
            color = color.copy(alpha = glowAlpha * 0.22f / i),
            cornerRadius = radius,
            style = Stroke(width = coreWidth + i * 2.dp.toPx())
        )
    }
}
