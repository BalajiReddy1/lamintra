package com.lamintra.verification

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Desktop entry point for the harness.
 *
 * Exists because the visual check is a hard gate on every component release and
 * wasm costs ~2.5 minutes per look, against ~30 seconds here. Validate on
 * desktop first, then confirm on wasm and device.
 *
 *     ./gradlew :harness:run
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Lamintra harness - wave 1",
        state = rememberWindowState(width = 780.dp, height = 900.dp)
    ) {
        HarnessRoot()
    }
}
