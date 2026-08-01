package com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * The pill-shaped grip at the top of the sheet. 40x4dp, `handle` token
 * (white @ 28%) per design/TOKENS.md. Internal to this component.
 */
@Composable
internal fun DragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(40.dp)
            .height(4.dp)
            .background(
                color = Color.White.copy(alpha = 0.28f),
                shape = RoundedCornerShape(2.dp)
            )
    )
}
