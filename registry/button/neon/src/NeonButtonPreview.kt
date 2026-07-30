package com.jetcompose.button.neon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Android Studio preview for NeonButton. Installed to the android source
 * root because the @Preview annotation is Android-tooling-only. Safe to
 * delete.
 */
@Preview(name = "NeonButton", showBackground = true, backgroundColor = 0xFF0E1116, widthDp = 240, heightDp = 120)
@Composable
private fun NeonButtonPreview() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0E1116)).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        NeonButton(text = "NEON", onClick = {})
    }
}
