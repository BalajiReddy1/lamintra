package com.jetcompose.bottomsheet.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android Studio preview for GlassBottomSheet. Installed to the android
 * source root (not commonMain) because the @Preview annotation is
 * Android-tooling-only. Safe to delete — it ships purely so the component
 * is visible in the IDE the moment it's installed.
 */
@Preview(name = "GlassBottomSheet", showBackground = true, backgroundColor = 0xFF0E1116, widthDp = 360, heightDp = 260)
@Composable
private fun GlassBottomSheetPreview() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0E1116)),
        contentAlignment = Alignment.BottomCenter
    ) {
        GlassBottomSheet(
            visible = true,
            onDismiss = {},
            tint = Color(0xFF9BB8FF)
        ) {
            BasicText(
                text = "Glass bottom sheet — drag down to dismiss",
                style = TextStyle(color = Color.White, fontSize = 14.sp),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
