package com.lamintra.button

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Android Studio previews for LamintraButton. Installed to the android source
 * root because the @Preview annotation is Android-tooling-only. Safe to delete.
 *
 * Both schemes are shown as separate previews rather than one that toggles: the
 * component must resolve correctly in a light host app and a dark one, and a
 * toggle only ever proves one of them at a time.
 */
@Preview(name = "LamintraButton - dark", widthDp = 320, heightDp = 340)
@Composable
private fun LamintraButtonDarkPreview() {
    ButtonSpecimen(canvas = Color(0xFF0A0A0B), colors = LamintraButtonColors.dark())
}

@Preview(name = "LamintraButton - light", widthDp = 320, heightDp = 340)
@Composable
private fun LamintraButtonLightPreview() {
    ButtonSpecimen(canvas = Color(0xFFFFFFFF), colors = LamintraButtonColors.light())
}

@Composable
private fun ButtonSpecimen(canvas: Color, colors: LamintraButtonColors) {
    Column(
        modifier = Modifier.fillMaxSize().background(canvas).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        LamintraButton("Primary", {}, colors = colors)
        LamintraButton("Secondary", {}, emphasis = ButtonEmphasis.Secondary, colors = colors)
        LamintraButton("Destructive", {}, emphasis = ButtonEmphasis.Destructive, colors = colors)
        LamintraButton("Ghost", {}, emphasis = ButtonEmphasis.Ghost, colors = colors)
        LamintraButton("Disabled", {}, enabled = false, colors = colors)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LamintraButton("Medium", {}, size = ButtonSize.Medium, fillWidth = false, colors = colors)
            LamintraButton(
                "Medium",
                {},
                emphasis = ButtonEmphasis.Secondary,
                size = ButtonSize.Medium,
                fillWidth = false,
                colors = colors
            )
        }
    }
}
