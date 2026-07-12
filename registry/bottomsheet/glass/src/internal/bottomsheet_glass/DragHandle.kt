package com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The small pill-shaped grip shown at the top of the bottom sheet.
 * Internal to this component — not exported for external use, which is
 * exactly why it lives under the internal/<prefix> namespace instead of
 * the component's public package.
 */
@Composable
internal fun DragHandle(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .width(36.dp)
            .height(4.dp)
            .background(
                color = Color.White.copy(alpha = 0.5f),
                shape = RoundedCornerShape(50)
            )
    )
}
