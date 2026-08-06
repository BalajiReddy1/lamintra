package com.lamintra.switch

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
 * Android Studio previews for LamintraSwitch. Installed to the android source
 * root because the @Preview annotation is Android-tooling-only. Safe to delete.
 *
 * Both schemes are shown as separate previews rather than one that toggles: the
 * component must resolve correctly in a light host app and a dark one, and a
 * toggle only ever proves one of them at a time.
 */
@Preview(name = "LamintraSwitch - dark", widthDp = 300, heightDp = 150)
@Composable
private fun LamintraSwitchDarkPreview() {
    SwitchSpecimen(canvas = Color(0xFF0A0A0B), colors = LamintraSwitchColors.dark())
}

@Preview(name = "LamintraSwitch - light", widthDp = 300, heightDp = 150)
@Composable
private fun LamintraSwitchLightPreview() {
    SwitchSpecimen(canvas = Color(0xFFFFFFFF), colors = LamintraSwitchColors.light())
}

@Composable
private fun SwitchSpecimen(canvas: Color, colors: LamintraSwitchColors) {
    Column(
        modifier = Modifier.fillMaxSize().background(canvas).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            LamintraSwitch(checked = true, onCheckedChange = {}, colors = colors)
            LamintraSwitch(checked = false, onCheckedChange = {}, colors = colors)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            LamintraSwitch(checked = true, onCheckedChange = {}, enabled = false, colors = colors)
            LamintraSwitch(checked = false, onCheckedChange = {}, enabled = false, colors = colors)
        }
    }
}
