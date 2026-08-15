package com.lamintra.verification.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamintra.button.ButtonEmphasis
import com.lamintra.button.LamintraButton
import com.lamintra.button.LamintraButtonColors
import com.lamintra.card.LamintraCard
import com.lamintra.card.LamintraCardColors
import com.lamintra.list_row.LamintraListRow
import com.lamintra.list_row.LamintraListRowColors
import com.lamintra.list_row.LamintraListRowDivider
import com.lamintra.segmented.LamintraSegmented
import com.lamintra.segmented.LamintraSegmentedColors
import com.lamintra.switch.LamintraSwitch
import com.lamintra.switch.LamintraSwitchColors
import com.lamintra.text_field.LamintraTextField
import com.lamintra.text_field.LamintraTextFieldColors

/**
 * The composed-screen check.
 *
 * Wave 1's claim is that button, card, text field, list row and switch "build a
 * complete settings screen in one language". This is where that claim is either
 * true on a real screen or it isn't - the gallery judges each component alone,
 * this judges whether five of them together still read as one family.
 *
 * Everything is a REGISTRY source staged by the harness build, so this screen
 * is made of exactly what `lamintra add` installs. The segmented control was
 * the first of the two next-wave candidates to earn a registry entry and now
 * appears here as the real component; the slider is still a prototype and is
 * deliberately kept alongside it, because a language that only holds for the
 * components it was designed around is not a language.
 */
@Composable
fun SettingsScreen() {
    var dark by remember { mutableStateOf(true) }
    val c = if (dark) DirectionColors.dark() else DirectionColors.light()

    val buttonColors = if (dark) LamintraButtonColors.dark() else LamintraButtonColors.light()
    val cardColors = if (dark) LamintraCardColors.dark() else LamintraCardColors.light()
    val fieldColors = if (dark) LamintraTextFieldColors.dark() else LamintraTextFieldColors.light()
    val rowColors = if (dark) LamintraListRowColors.dark() else LamintraListRowColors.light()
    val switchColors = if (dark) LamintraSwitchColors.dark() else LamintraSwitchColors.light()
    val segmentedColors =
        if (dark) LamintraSegmentedColors.dark() else LamintraSegmentedColors.light()

    var name by remember { mutableStateOf("Ada Lovelace") }
    var email by remember { mutableStateOf("") }
    var notifications by remember { mutableStateOf(true) }
    var haptics by remember { mutableStateOf(false) }
    var range by remember { mutableStateOf(0) }
    var volume by remember { mutableStateOf(0.42f) }
    var saves by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier.fillMaxSize().background(c.surface),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    text = "Settings",
                    style = DType.display.copy(color = c.ink),
                    modifier = Modifier.weight(1f)
                )
                SchemeChip(dark = dark, c = c) { dark = !dark }
            }

            Spacer(Modifier.height(32.dp))
            Caption("Your name", c)
            Spacer(Modifier.height(10.dp))
            LamintraTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "Add a name",
                colors = fieldColors
            )
            Spacer(Modifier.height(14.dp))
            LamintraTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "you@example.com",
                colors = fieldColors
            )

            Spacer(Modifier.height(32.dp))
            Caption("Account", c)
            Spacer(Modifier.height(10.dp))
            LamintraCard(colors = cardColors) {
                LamintraListRow("Email", value = "you@example.com", onClick = {}, colors = rowColors)
                LamintraListRowDivider(colors = rowColors)
                LamintraListRow("Plan", value = "Free", onClick = {}, colors = rowColors)
            }

            Spacer(Modifier.height(32.dp))
            Caption("Preferences", c)
            Spacer(Modifier.height(10.dp))
            LamintraCard(colors = cardColors) {
                LamintraListRow("Notifications", colors = rowColors) {
                    LamintraSwitch(notifications, { notifications = it }, colors = switchColors)
                }
                LamintraListRowDivider(colors = rowColors)
                LamintraListRow("Haptic feedback", colors = rowColors) {
                    LamintraSwitch(haptics, { haptics = it }, colors = switchColors)
                }
            }

            Spacer(Modifier.height(32.dp))
            Caption("Usage", c)
            Spacer(Modifier.height(10.dp))
            LamintraSegmented(
                options = listOf("Day", "Week", "Month"),
                selected = range,
                onSelect = { range = it },
                colors = segmentedColors
            )

            Spacer(Modifier.height(32.dp))
            Caption("Not yet in the registry", c)
            Spacer(Modifier.height(10.dp))
            DSlider(value = volume, onValueChange = { volume = it }, c = c)

            Spacer(Modifier.height(36.dp))
            LamintraButton(
                text = if (saves == 0) "Save changes" else "Saved x$saves",
                onClick = { saves++ },
                colors = buttonColors
            )
            Spacer(Modifier.height(12.dp))
            LamintraButton(
                "Cancel",
                { saves = 0 },
                emphasis = ButtonEmphasis.Secondary,
                colors = buttonColors
            )
            Spacer(Modifier.height(12.dp))
            LamintraButton(
                "Delete account",
                {},
                emphasis = ButtonEmphasis.Ghost,
                colors = buttonColors
            )

            Spacer(Modifier.height(28.dp))
            BasicText(
                text = "One solid primary per screen. Ink carries the actions; the accent is " +
                    "spent only on the switches, where it encodes state.",
                style = DType.caption.copy(color = c.inkDim)
            )
            Spacer(Modifier.height(64.dp))
        }
    }
}

@Composable
private fun Caption(text: String, c: DirectionColors) {
    BasicText(text = text, style = DType.caption.copy(color = c.inkDim))
}

@Composable
private fun SchemeChip(dark: Boolean, c: DirectionColors, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, pressSpring())
    Box(
        modifier = Modifier
            .height(36.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .drawBehind {
                drawPath(
                    Squircle.path(size.width, size.height, size.height / 2f),
                    c.hairline,
                    style = Stroke(1.5.dp.toPx())
                )
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = if (dark) "Dark" else "Light",
            style = DType.caption.copy(fontSize = 13.sp, color = c.inkDim)
        )
    }
}
