package com.lamintra.segmented

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Android Studio previews for LamintraSegmented. Installed to the android
 * source root because the @Preview annotation is Android-tooling-only. Safe to
 * delete.
 *
 * Both schemes are shown as separate previews rather than one that toggles: the
 * component must resolve correctly in a light host app and a dark one, and a
 * toggle only ever proves one of them at a time.
 */
@Preview(name = "LamintraSegmented - dark", widthDp = 320, heightDp = 220)
@Composable
private fun LamintraSegmentedDarkPreview() {
    SegmentedSpecimen(canvas = Color(0xFF0A0A0B), colors = LamintraSegmentedColors.dark())
}

@Preview(name = "LamintraSegmented - light", widthDp = 320, heightDp = 220)
@Composable
private fun LamintraSegmentedLightPreview() {
    SegmentedSpecimen(canvas = Color(0xFFFFFFFF), colors = LamintraSegmentedColors.light())
}

@Composable
private fun SegmentedSpecimen(canvas: Color, colors: LamintraSegmentedColors) {
    var range by remember { mutableStateOf(1) }
    var view by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier.fillMaxSize().background(canvas).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        LamintraSegmented(
            options = listOf("Day", "Week", "Month"),
            selected = range,
            onSelect = { range = it },
            colors = colors
        )
        LamintraSegmented(
            options = listOf("All", "Unread"),
            selected = view,
            onSelect = { view = it },
            colors = colors
        )
        LamintraSegmented(
            options = listOf("Day", "Week", "Month"),
            selected = 2,
            onSelect = {},
            enabled = false,
            colors = colors
        )
    }
}
