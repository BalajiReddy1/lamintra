package com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies the frosted-glass look used by GlassBottomSheet: a translucent
 * tinted background, a subtle light-catching border, and rounded top
 * corners. Kept internal and namespaced per-component so two different
 * glass-styled components in the same registry can each ship their own
 * variant of this file without colliding on install.
 */
internal fun Modifier.glassSurface(tint: Color, cornerRadius: Dp): Modifier {
    val shape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius)
    return this
        .background(color = tint.copy(alpha = 0.18f), shape = shape)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.35f),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = shape
        )
}
