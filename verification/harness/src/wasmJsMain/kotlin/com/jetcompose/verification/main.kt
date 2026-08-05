package com.jetcompose.verification

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "composeTarget") {
        // Light/dark gate: mirror what Compose observes INSIDE composition out
        // to the DOM. Reading `prefers-color-scheme` from JS directly would
        // only prove the browser works — this proves Compose's own
        // `isSystemInDarkTheme()` tracks it on wasm, which is what every
        // component's `.auto()` default depends on. Written to the DOM rather
        // than drawn on the canvas so it can be read without a compositing
        // viewport.
        val dark = isSystemInDarkTheme()
        LaunchedEffect(dark) {
            document.getElementById("gate")?.textContent = "isSystemInDarkTheme=$dark"
        }

        VerificationScreen()
    }
}
