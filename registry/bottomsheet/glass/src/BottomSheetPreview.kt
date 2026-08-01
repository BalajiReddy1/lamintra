package com.jetcompose.bottomsheet.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android Studio preview for GlassBottomSheet. Installed to the android
 * source root (not commonMain) because the @Preview annotation is
 * Android-tooling-only. Safe to delete — it ships purely so the component
 * is visible in the IDE the moment it's installed.
 */
@Preview(name = "GlassBottomSheet", showBackground = true, backgroundColor = 0xFF0B0E14, widthDp = 360, heightDp = 560)
@Composable
private fun GlassBottomSheetPreview() {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0E14))) {
        BasicText(
            text = "content behind the sheet",
            style = TextStyle(color = Color(0xFF8A93A5), fontSize = 13.sp, letterSpacing = 2.sp),
            modifier = Modifier.align(Alignment.Center)
        )
        GlassBottomSheet(visible = true, onDismiss = {}) {
            Column {
                BasicText(
                    text = "Glass bottom sheet",
                    style = TextStyle(
                        color = Color(0xFFF2F5FA),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                BasicText(
                    text = "Drag down to dismiss, or tap the scrim.",
                    style = TextStyle(color = Color(0xFF8A93A5), fontSize = 14.sp)
                )
            }
        }
    }
}
