package com.lamintra.verification

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.lamintra.verification.design.Gallery

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "composeTarget") {
        Gallery()
    }
}
