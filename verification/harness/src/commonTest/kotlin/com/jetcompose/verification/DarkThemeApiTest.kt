package com.jetcompose.verification

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Gate for the colour-scheme decision (see design/TOKENS.md).
 *
 * `.auto()` is the default for every component, and it is built on
 * `isSystemInDarkTheme()`. That API is `compose.foundation`, so it is
 * Material-safe — but "it compiles" is not "it works". If it failed or
 * returned a hardcoded constant on a target, every component would render
 * in the wrong scheme there, and on wasm that target is the website itself.
 *
 * This asserts only that the API resolves inside composition without
 * crashing, and prints the value so each target's result is visible in CI
 * logs. Deliberately does NOT assert a specific value: the correct result
 * depends on the host environment, which is the very property under test —
 * an assertion either way would fail spuriously on a machine themed the
 * other way.
 */
@OptIn(ExperimentalTestApi::class)
class DarkThemeApiTest {

    @Test
    fun systemDarkThemeApiResolvesInComposition() = runComposeUiTest {
        var observed: Boolean? = null

        setContent {
            observed = isSystemInDarkTheme()
            BasicText(text = "resolved")
        }

        onNodeWithText("resolved").assertIsDisplayed()
        assertNotNull(observed, "isSystemInDarkTheme() never resolved in composition")
        println("GATE isSystemInDarkTheme=$observed")
    }
}
