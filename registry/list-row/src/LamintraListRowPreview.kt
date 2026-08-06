package com.lamintra.list_row

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lamintra.list_row.internal.list_row.Squircle

/**
 * Android Studio previews for LamintraListRow. Installed to the android source
 * root because the @Preview annotation is Android-tooling-only. Safe to delete.
 *
 * Both schemes are shown as separate previews rather than one that toggles: the
 * component must resolve correctly in a light host app and a dark one, and a
 * toggle only ever proves one of them at a time.
 *
 * The rows sit in a plain rounded container drawn here rather than by importing
 * LamintraCard - components are standalone after install, so a preview may
 * never depend on another component being installed too.
 */
@Preview(name = "LamintraListRow - dark", widthDp = 340, heightDp = 300)
@Composable
private fun LamintraListRowDarkPreview() {
    ListRowSpecimen(
        canvas = Color(0xFF0A0A0B),
        container = Color(0xFF141416),
        colors = LamintraListRowColors.dark()
    )
}

@Preview(name = "LamintraListRow - light", widthDp = 340, heightDp = 300)
@Composable
private fun LamintraListRowLightPreview() {
    ListRowSpecimen(
        canvas = Color(0xFFFFFFFF),
        container = Color(0xFFF4F4F5),
        colors = LamintraListRowColors.light()
    )
}

@Composable
private fun ListRowSpecimen(canvas: Color, container: Color, colors: LamintraListRowColors) {
    Column(modifier = Modifier.fillMaxSize().background(canvas).padding(20.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawPath(Squircle.path(size.width, size.height, 16.dp.toPx()), container)
                }
        ) {
            LamintraListRow(label = "Email", value = "balaji@mail.com", onClick = {}, colors = colors)
            LamintraListRowDivider(colors = colors)
            LamintraListRow(label = "Plan", value = "Free", onClick = {}, colors = colors)
            LamintraListRowDivider(colors = colors)
            LamintraListRow(label = "Not tappable", value = "static", colors = colors)
            LamintraListRowDivider(colors = colors)
            LamintraListRow(
                label = "Disabled",
                value = "off",
                onClick = {},
                enabled = false,
                colors = colors
            )
        }
    }
}
