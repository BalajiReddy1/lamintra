package com.lamintra.text_field

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Android Studio previews for LamintraTextField. Installed to the android
 * source root because the @Preview annotation is Android-tooling-only. Safe to
 * delete.
 *
 * Both schemes are shown as separate previews rather than one that toggles: the
 * component must resolve correctly in a light host app and a dark one, and a
 * toggle only ever proves one of them at a time.
 */
@Preview(name = "LamintraTextField - dark", widthDp = 320, heightDp = 280)
@Composable
private fun LamintraTextFieldDarkPreview() {
    TextFieldSpecimen(canvas = Color(0xFF0A0A0B), colors = LamintraTextFieldColors.dark())
}

@Preview(name = "LamintraTextField - light", widthDp = 320, heightDp = 280)
@Composable
private fun LamintraTextFieldLightPreview() {
    TextFieldSpecimen(canvas = Color(0xFFFFFFFF), colors = LamintraTextFieldColors.light())
}

@Composable
private fun TextFieldSpecimen(canvas: Color, colors: LamintraTextFieldColors) {
    Column(
        modifier = Modifier.fillMaxSize().background(canvas).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        LamintraTextField(
            value = "Balaji",
            onValueChange = {},
            label = "Display name",
            colors = colors
        )
        LamintraTextField(
            value = "",
            onValueChange = {},
            label = "Email",
            placeholder = "you@example.com",
            colors = colors
        )
        LamintraTextField(
            value = "Locked",
            onValueChange = {},
            enabled = false,
            colors = colors
        )
    }
}
