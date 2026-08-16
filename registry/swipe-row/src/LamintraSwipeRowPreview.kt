package com.lamintra.swipe_row

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
 * Android Studio previews for LamintraSwipeRow. Installed to the android source
 * root because the @Preview annotation is Android-tooling-only. Safe to delete.
 *
 * A preview cannot be dragged, so both rows here are at rest and the actions
 * are hidden underneath, which is the honest picture of what the component
 * looks like before it is touched. The parallax, the velocity-seeded snap and
 * the full-swipe arming are the reasons it exists and none of them survive a
 * still - run it on a device.
 */
@Preview(name = "LamintraSwipeRow - dark", widthDp = 340, heightDp = 200)
@Composable
private fun LamintraSwipeRowDarkPreview() {
    SwipeRowSpecimen(
        canvas = Color(0xFF09090B),
        ink = Color(0xFFFAFAFA),
        colors = LamintraSwipeRowColors.dark()
    )
}

@Preview(name = "LamintraSwipeRow - light", widthDp = 340, heightDp = 200)
@Composable
private fun LamintraSwipeRowLightPreview() {
    SwipeRowSpecimen(
        canvas = Color(0xFFFFFFFF),
        ink = Color(0xFF09090B),
        colors = LamintraSwipeRowColors.light()
    )
}

@Composable
private fun SwipeRowSpecimen(canvas: Color, ink: Color, colors: LamintraSwipeRowColors) {
    Column(
        modifier = Modifier.fillMaxSize().background(canvas).padding(16.dp)
    ) {
        LamintraSwipeRow(
            actions = listOf(
                LamintraSwipeAction("Archive", onClick = {}),
                LamintraSwipeAction("Delete", onClick = {}, destructive = true)
            ),
            colors = colors
        ) {
            PreviewRow("Quarterly report", ink)
        }
        LamintraSwipeRow(
            actions = listOf(LamintraSwipeAction("Delete", onClick = {}, destructive = true)),
            colors = colors
        ) {
            PreviewRow("Meeting notes", ink)
        }
    }
}

/**
 * A stand-in for a real row. The preview does not use the list-row component
 * because components install independently: importing one here would put a
 * package in this file that need not exist in the project it lands in.
 */
@Composable
private fun PreviewRow(label: String, ink: Color) {
    Box(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 60.dp).padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BasicText(label, style = TextStyle(color = ink, fontSize = 16.sp))
    }
}
