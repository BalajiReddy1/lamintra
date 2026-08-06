package com.lamintra.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Android Studio previews for LamintraCard. Installed to the android source
 * root because the @Preview annotation is Android-tooling-only. Safe to delete.
 *
 * Both schemes are shown as separate previews rather than one that toggles: the
 * component must resolve correctly in a light host app and a dark one, and a
 * toggle only ever proves one of them at a time.
 */
@Preview(name = "LamintraCard - dark", widthDp = 320, heightDp = 240)
@Composable
private fun LamintraCardDarkPreview() {
    CardSpecimen(
        canvas = Color(0xFF0A0A0B),
        ink = Color(0xFFFAFAFA),
        colors = LamintraCardColors.dark()
    )
}

@Preview(name = "LamintraCard - light", widthDp = 320, heightDp = 240)
@Composable
private fun LamintraCardLightPreview() {
    CardSpecimen(
        canvas = Color(0xFFFFFFFF),
        ink = Color(0xFF09090B),
        colors = LamintraCardColors.light()
    )
}

@Composable
private fun CardSpecimen(canvas: Color, ink: Color, colors: LamintraCardColors) {
    Column(
        modifier = Modifier.fillMaxSize().background(canvas).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        LamintraCard(colors = colors, contentPadding = PaddingValues(20.dp)) {
            BasicText("Static card", style = TextStyle(fontSize = 16.sp, color = ink))
        }
        LamintraCard(onClick = {}, colors = colors, contentPadding = PaddingValues(20.dp)) {
            BasicText("Clickable card", style = TextStyle(fontSize = 16.sp, color = ink))
        }
        LamintraCard(
            onClick = {},
            enabled = false,
            colors = colors,
            contentPadding = PaddingValues(20.dp)
        ) {
            BasicText("Disabled", style = TextStyle(fontSize = 16.sp, color = ink))
        }
    }
}
