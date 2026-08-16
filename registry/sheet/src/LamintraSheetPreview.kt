package com.lamintra.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android Studio previews for LamintraSheet. Installed to the android source
 * root because the @Preview annotation is Android-tooling-only. Safe to delete.
 *
 * Both schemes are shown as separate previews rather than one that toggles: the
 * component must resolve correctly in a light host app and a dark one, and a
 * toggle only ever proves one of them at a time.
 *
 * `visible = true` and no state: a preview cannot be dragged, so the only thing
 * worth showing is the resting position. The drag, the spring settle and the
 * scrim tracking are the parts you have to run to judge.
 *
 * WindowInsets(0) because a preview canvas has no system bars to avoid, and the
 * default safeDrawing insets would push the card up off a short preview.
 */
@Preview(name = "LamintraSheet - dark", widthDp = 320, heightDp = 360)
@Composable
private fun LamintraSheetDarkPreview() {
    SheetSpecimen(
        canvas = Color(0xFF09090B),
        ink = Color(0xFFFAFAFA),
        colors = LamintraSheetColors.dark()
    )
}

@Preview(name = "LamintraSheet - light", widthDp = 320, heightDp = 360)
@Composable
private fun LamintraSheetLightPreview() {
    SheetSpecimen(
        canvas = Color(0xFFFFFFFF),
        ink = Color(0xFF09090B),
        colors = LamintraSheetColors.light()
    )
}

@Composable
private fun SheetSpecimen(canvas: Color, ink: Color, colors: LamintraSheetColors) {
    Box(Modifier.fillMaxSize().background(canvas)) {
        LamintraSheet(
            visible = true,
            onDismiss = {},
            colors = colors,
            contentWindowInsets = WindowInsets(0)
        ) {
            BasicText("Move to folder", style = TextStyle(color = ink, fontSize = 17.sp))
            Spacer(Modifier.height(8.dp))
            BasicText(
                "Choose where this goes. You can move it again later.",
                style = TextStyle(color = ink.copy(alpha = 0.6f), fontSize = 14.sp)
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
