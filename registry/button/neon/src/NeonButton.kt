package com.jetcompose.button.neon

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.jetcompose.button.neon.internal.button_neon.neonGlow

@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = Color.Cyan
) {
    BasicText(
        text = text,
        modifier = modifier
            .neonGlow(color = glowColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}
