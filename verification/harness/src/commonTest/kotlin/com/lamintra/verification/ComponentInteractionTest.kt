package com.lamintra.verification

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import com.lamintra.bottomsheet.glass.GlassBottomSheet
import com.lamintra.button.neon.NeonButton
import com.lamintra.button.neon_outline.NeonOutlineButton
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Interaction tests that run on a real iOS simulator via
 * `iosSimulatorArm64Test`, and on the JVM via `desktopTest`.
 *
 * These exist because compile-only verification is explicitly not sufficient
 * for this project: a bottom sheet whose drag-to-dismiss compiled on every
 * target was broken the first time a human touched it. iOS was the one
 * platform that had never been verified by execution at all, only by
 * reasoning about the APIs used.
 */
@OptIn(ExperimentalTestApi::class)
class ComponentInteractionTest {

    @Test
    fun neonButtonFiresWhenEnabled() = runComposeUiTest {
        var taps = 0
        setContent { NeonButton(text = "TAP ME", onClick = { taps++ }) }

        onNodeWithText("TAP ME").performClick()

        assertEquals(1, taps, "an enabled NeonButton should report the tap")
    }

    @Test
    fun neonButtonIgnoresTapsWhenDisabled() = runComposeUiTest {
        var taps = 0
        setContent { NeonButton(text = "OFF", onClick = { taps++ }, enabled = false) }

        onNodeWithText("OFF").performClick()

        assertEquals(0, taps, "a disabled NeonButton must not report taps")
    }

    @Test
    fun neonOutlineButtonIgnoresTapsWhenDisabled() = runComposeUiTest {
        var taps = 0
        setContent { NeonOutlineButton(text = "OFF", onClick = { taps++ }, enabled = false) }

        onNodeWithText("OFF").performClick()

        assertEquals(0, taps, "a disabled NeonOutlineButton must not report taps")
    }

    @Test
    fun sheetShowsItsContentWhenVisible() = runComposeUiTest {
        setContent {
            GlassBottomSheet(visible = true, onDismiss = {}) { BasicText("SHEET BODY") }
        }

        onNodeWithText("SHEET BODY").assertIsDisplayed()
    }

    /**
     * A long, slow drag: past the 64dp distance threshold, so it dismisses on
     * distance regardless of release velocity.
     */
    @Test
    fun sheetDismissesOnLongDrag() = runComposeUiTest {
        var dismissed = false
        setContent {
            var visible by mutableStateOf(true)
            GlassBottomSheet(
                visible = visible,
                onDismiss = { dismissed = true; visible = false }
            ) { BasicText("SHEET BODY") }
        }

        onNodeWithText("SHEET BODY").performTouchInput {
            down(center)
            repeat(10) { moveBy(Offset(0f, 30f), delayMillis = 16) }
            up()
        }
        waitForIdle()

        assertTrue(dismissed, "a 300px downward drag should dismiss the sheet")
    }

    /**
     * A short, slow nudge: under the 64dp distance threshold AND under the
     * 125dp/s fling threshold, so neither dismiss criterion fires and the
     * sheet must spring back instead.
     */
    @Test
    fun sheetSpringsBackOnSmallSlowNudge() = runComposeUiTest {
        var dismissed = false
        setContent {
            var visible by mutableStateOf(true)
            GlassBottomSheet(
                visible = visible,
                onDismiss = { dismissed = true; visible = false }
            ) { BasicText("SHEET BODY") }
        }

        onNodeWithText("SHEET BODY").performTouchInput {
            down(center)
            repeat(4) { moveBy(Offset(0f, 4f), delayMillis = 250) }
            up()
        }
        waitForIdle()

        assertFalse(dismissed, "a 16px nudge at ~16px/s must not dismiss the sheet")
        onNodeWithText("SHEET BODY").assertIsDisplayed()
    }
}
