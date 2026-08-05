package com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The glass surface: a tinted vertical-gradient fill that is brighter
 * where light lands (top) and fades toward the bottom, under a 1dp
 * light-catching hairline border. Values follow design/TOKENS.md.
 * Kept internal and namespaced per-component so two glass-styled
 * components can each ship their own variant without colliding.
 *
 * [hairlineColor] is a parameter rather than a hard-coded white because a
 * white edge is only correct over a dark background — hard-coding it is
 * what made this component dark-only. The component applies the alpha
 * ramp (38% / 12% / 4%, top to bottom); pass a dark colour for a light
 * host scheme.
 */
internal fun Modifier.glassSurface(
    tint: Color,
    cornerRadius: Dp,
    hairlineColor: Color
): Modifier = drawBehind {
    val radius = CornerRadius(cornerRadius.toPx())
    // glass fill — light from the top
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to tint.copy(alpha = 0.26f),
            0.35f to tint.copy(alpha = 0.14f),
            1f to tint.copy(alpha = 0.08f)
        ),
        cornerRadius = radius
    )
    // light-catching hairline
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to hairlineColor.copy(alpha = 0.38f),
            0.25f to hairlineColor.copy(alpha = 0.12f),
            1f to hairlineColor.copy(alpha = 0.04f)
        ),
        cornerRadius = radius,
        style = Stroke(width = 1.dp.toPx())
    )
}
