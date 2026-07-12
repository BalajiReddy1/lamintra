package com.jetcompose.button.neon.internal.button_neon

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Applies a neon-glow shadow effect. Deliberately named identically to
 * bottomsheet/glass's internal ModifierExtensions.kt to serve as the
 * collision test case for the CLI's namespacing logic — these two files
 * must never end up sharing a package after install.
 */
internal fun Modifier.neonGlow(color: Color): Modifier = this.shadow(
    elevation = 16.dp,
    shape = RoundedCornerShape(8.dp),
    ambientColor = color,
    spotColor = color
)
