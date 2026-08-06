package com.lamintra.verification

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.lamintra.button.LamintraButton
import com.lamintra.card.LamintraCard
import com.lamintra.list_row.LamintraListRow
import com.lamintra.switch.LamintraSwitch
import com.lamintra.text_field.LamintraTextField
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Interaction tests for the wave-1 base tier, run on a real iOS simulator via
 * `iosSimulatorArm64Test` and on the JVM via `desktopTest`.
 *
 * Compile-only verification is explicitly not sufficient for this project: a
 * bottom sheet whose drag-to-dismiss compiled on every target was broken the
 * first time a human touched it. These are the cheap half of the bar - they
 * prove the components compose and that the enabled/disabled contract holds.
 * They cannot prove anything about appearance; that still needs a human.
 *
 * Each `enabled` case is paired with its `disabled` twin on purpose. A disabled
 * component that merely *looks* dimmed while still firing its callback is the
 * exact bug class this project has already shipped once.
 */
@OptIn(ExperimentalTestApi::class)
class LamintraComponentTest {

    @Test
    fun buttonFiresWhenEnabled() = runComposeUiTest {
        var taps = 0
        setContent { LamintraButton(text = "TAP ME", onClick = { taps++ }) }

        onNodeWithText("TAP ME").performClick()

        assertEquals(1, taps, "an enabled LamintraButton should report the tap")
    }

    @Test
    fun buttonIgnoresTapsWhenDisabled() = runComposeUiTest {
        var taps = 0
        setContent { LamintraButton(text = "OFF", onClick = { taps++ }, enabled = false) }

        onNodeWithText("OFF").performClick()

        assertEquals(0, taps, "a disabled LamintraButton must not report taps")
    }

    @Test
    fun cardFiresWhenGivenAnOnClick() = runComposeUiTest {
        var taps = 0
        setContent {
            LamintraCard(onClick = { taps++ }) { BasicText("CARD BODY") }
        }

        onNodeWithText("CARD BODY").performClick()

        assertEquals(1, taps, "a card with an onClick should report the tap")
    }

    @Test
    fun cardIgnoresTapsWhenDisabled() = runComposeUiTest {
        var taps = 0
        setContent {
            LamintraCard(onClick = { taps++ }, enabled = false) { BasicText("CARD BODY") }
        }

        onNodeWithText("CARD BODY").performClick()

        assertEquals(0, taps, "a disabled card must not report taps")
    }

    /**
     * A card with no `onClick` is the static form of the component. Tapping it
     * must be inert rather than throwing or swallowing the gesture - this is
     * the branch that decides whether it draws a base plane at all.
     */
    @Test
    fun staticCardStillRendersItsContent() = runComposeUiTest {
        setContent { LamintraCard { BasicText("STATIC BODY") } }

        onNodeWithText("STATIC BODY").performClick()

        onNodeWithText("STATIC BODY").assertIsDisplayed()
    }

    @Test
    fun textFieldAcceptsInputWhenEnabled() = runComposeUiTest {
        var text = "ab"
        setContent {
            var value by mutableStateOf(text)
            LamintraTextField(value = value, onValueChange = { value = it; text = it })
        }

        onNodeWithText("ab").performTextInput("c")

        assertTrue(text.contains("c"), "an enabled LamintraTextField should accept typing, got: $text")
    }

    @Test
    fun textFieldRejectsInputWhenDisabled() = runComposeUiTest {
        var text = "ab"
        setContent {
            var value by mutableStateOf(text)
            LamintraTextField(
                value = value,
                onValueChange = { value = it; text = it },
                enabled = false
            )
        }

        onNodeWithText("ab").performClick()

        assertEquals("ab", text, "a disabled LamintraTextField must not accept typing")
    }

    @Test
    fun textFieldShowsPlaceholderOnlyWhileEmpty() = runComposeUiTest {
        setContent {
            Column {
                LamintraTextField(value = "", onValueChange = {}, placeholder = "EMPTY HINT")
                LamintraTextField(value = "filled", onValueChange = {}, placeholder = "FILLED HINT")
            }
        }

        onNodeWithText("EMPTY HINT").assertIsDisplayed()
        onNodeWithText("filled").assertIsDisplayed()
    }

    @Test
    fun listRowFiresWhenGivenAnOnClick() = runComposeUiTest {
        var taps = 0
        setContent { LamintraListRow(label = "ROW LABEL", onClick = { taps++ }) }

        onNodeWithText("ROW LABEL").performClick()

        assertEquals(1, taps, "a row with an onClick should report the tap")
    }

    @Test
    fun listRowIgnoresTapsWhenDisabled() = runComposeUiTest {
        var taps = 0
        setContent {
            LamintraListRow(label = "ROW LABEL", onClick = { taps++ }, enabled = false)
        }

        onNodeWithText("ROW LABEL").performClick()

        assertEquals(0, taps, "a disabled row must not report taps")
    }

    @Test
    fun listRowRendersItsValueAndTrailingSlot() = runComposeUiTest {
        setContent {
            LamintraListRow(label = "ROW LABEL", value = "ROW VALUE") {
                BasicText("TRAILING")
            }
        }

        onNodeWithText("ROW LABEL").assertIsDisplayed()
        onNodeWithText("ROW VALUE").assertIsDisplayed()
        onNodeWithText("TRAILING").assertIsDisplayed()
    }

    @Test
    fun switchTogglesWhenEnabled() = runComposeUiTest {
        var checked = false
        setContent {
            var state by mutableStateOf(false)
            LamintraSwitch(
                checked = state,
                onCheckedChange = { state = it; checked = it }
            )
        }

        onNode(isToggleable()).performClick()

        assertTrue(checked, "an enabled LamintraSwitch should report the new state")
    }

    @Test
    fun switchIgnoresTapsWhenDisabled() = runComposeUiTest {
        var changes = 0
        setContent {
            LamintraSwitch(checked = false, onCheckedChange = { changes++ }, enabled = false)
        }

        onNode(isToggleable()).performClick()

        assertEquals(0, changes, "a disabled LamintraSwitch must not report a state change")
    }
}
