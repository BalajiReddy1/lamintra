package com.jetcompose.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jetcompose.bottomsheet.glass.GlassBottomSheet
import com.jetcompose.button.neon.NeonButton
import com.jetcompose.button.neon_outline.NeonOutlineButton

/**
 * The demo screen used by every verification target: desktop, iOS simulator
 * and the browser (wasmJs).
 *
 * Colours here are BRAND tokens — this is a demo surface, not component code,
 * so per design/TOKENS.md it may use them freely. The components themselves
 * take every colour as a parameter and none of these values reach them.
 */
@Composable
fun VerificationScreen() {
    var sheetVisible by remember { mutableStateOf(false) }
    var taps by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0E14)) // brand token: canvas
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            BasicText(
                text = "jetcompose components",
                style = TextStyle(
                    color = Color(0xFF8A93A5),
                    fontSize = 13.sp,
                    letterSpacing = 2.sp
                )
            )
            NeonOutlineButton(
                text = if (sheetVisible) "HIDE SHEET" else "SHOW SHEET",
                onClick = { sheetVisible = !sheetVisible }
            )
            NeonButton(
                text = if (taps == 0) "NEON" else "TAPPED $taps",
                onClick = { taps++ }
            )
            NeonButton(
                text = "DISABLED",
                onClick = { taps++ },
                enabled = false
            )
        }

        GlassBottomSheet(
            visible = sheetVisible,
            onDismiss = { sheetVisible = false }
        ) {
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
