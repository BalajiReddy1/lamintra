package com.lamintra.verification.design

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.animation.core.animateFloatAsState
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
import com.lamintra.button.ButtonSize
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
 * Component gallery - every component shown ISOLATED, with its own states and
 * its own install command.
 *
 * Composing components into one screen was misleading on its own: it implied a
 * developer copies a screen, when in fact they install exactly one component at
 * a time. This is also the shape the website needs, so the harness and the site
 * stay structurally the same. The composed check lives in [CandidateCScreen].
 *
 * These are the REGISTRY sources, staged by the harness build - not a copy. If
 * something here looks wrong, it is wrong in what `lamintra add` ships.
 */
@Composable
fun Gallery() {
    var dark by remember { mutableStateOf(true) }
    val page = if (dark) DirectionColors.dark() else DirectionColors.light()
    val scroll: ScrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize().background(page.surface),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    BasicText(text = "Components", style = DType.display.copy(color = page.ink))
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        text = "Contrast, space and physics. Each one installs on its own.",
                        style = DType.caption.copy(color = page.inkDim)
                    )
                }
                SchemeChip(dark = dark, page = page) { dark = !dark }
            }

            Spacer(Modifier.height(36.dp))

            Entry(
                name = "Button",
                install = "add button",
                note = "Emphasis and size are parameters - install one file, get all four kinds.",
                page = page
            ) {
                val colors = if (dark) LamintraButtonColors.dark() else LamintraButtonColors.light()
                var taps by remember { mutableStateOf(0) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LamintraButton(
                        text = if (taps == 0) "Primary" else "Tapped $taps",
                        onClick = { taps++ },
                        colors = colors
                    )
                    LamintraButton("Secondary", { taps++ }, emphasis = ButtonEmphasis.Secondary, colors = colors)
                    LamintraButton("Destructive", { taps = 0 }, emphasis = ButtonEmphasis.Destructive, colors = colors)
                    LamintraButton("Ghost", { taps++ }, emphasis = ButtonEmphasis.Ghost, colors = colors)
                    LamintraButton("Disabled", { taps++ }, enabled = false, colors = colors)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LamintraButton("Medium", { taps++ }, size = ButtonSize.Medium, fillWidth = false, colors = colors)
                        LamintraButton(
                            "Medium",
                            { taps++ },
                            emphasis = ButtonEmphasis.Secondary,
                            size = ButtonSize.Medium,
                            fillWidth = false,
                            colors = colors
                        )
                    }
                    Note("Press and hold - the whole control scales on a spring. Tab to it for focus.", page)
                }
            }

            Entry(
                name = "Card",
                install = "add card",
                note = "Static or interactive is decided by onClick, not by a variant parameter.",
                page = page
            ) {
                val colors = if (dark) LamintraCardColors.dark() else LamintraCardColors.light()
                var clicks by remember { mutableStateOf(0) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LamintraCard(colors = colors, contentPadding = PaddingValues(20.dp)) {
                        BasicText("Static", style = DType.body.copy(color = page.ink))
                    }
                    LamintraCard(onClick = { clicks++ }, colors = colors, contentPadding = PaddingValues(20.dp)) {
                        BasicText(
                            if (clicks == 0) "Interactive - press me" else "Pressed $clicks",
                            style = DType.body.copy(color = page.ink)
                        )
                    }
                    LamintraCard(
                        onClick = { clicks++ },
                        enabled = false,
                        colors = colors,
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        BasicText("Disabled", style = DType.body.copy(color = page.ink))
                    }
                    Note("Containers are tight rectangles against the capsule actions.", page)
                }
            }

            Entry(
                name = "Text field",
                install = "add text-field",
                note = "Wraps BasicTextField. No Material, and it never sets a fontFamily.",
                page = page
            ) {
                val colors = if (dark) LamintraTextFieldColors.dark() else LamintraTextFieldColors.light()
                var name by remember { mutableStateOf("Ada Lovelace") }
                var email by remember { mutableStateOf("") }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LamintraTextField(value = name, onValueChange = { name = it }, label = "Display name", colors = colors)
                    LamintraTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        placeholder = "you@example.com",
                        colors = colors
                    )
                    LamintraTextField(value = "Locked", onValueChange = {}, enabled = false, colors = colors)
                    Note("Focus thickens and darkens the field's own contour, on a spring.", page)
                }
            }

            Entry(
                name = "List row",
                install = "add list-row",
                note = "A pressed row dims. No recess, no shadow, no travel.",
                page = page
            ) {
                val colors = if (dark) LamintraListRowColors.dark() else LamintraListRowColors.light()
                val cardColors = if (dark) LamintraCardColors.dark() else LamintraCardColors.light()
                var taps by remember { mutableStateOf(0) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LamintraCard(colors = cardColors) {
                        LamintraListRow("Email", value = "you@example.com", onClick = { taps++ }, colors = colors)
                        LamintraListRowDivider(colors = colors)
                        LamintraListRow(
                            "Plan",
                            value = if (taps == 0) "Free" else "Tapped $taps",
                            onClick = { taps++ },
                            colors = colors
                        )
                        LamintraListRowDivider(colors = colors)
                        LamintraListRow("Not tappable", value = "static", colors = colors)
                        LamintraListRowDivider(colors = colors)
                        LamintraListRow("Disabled", value = "off", onClick = { taps++ }, enabled = false, colors = colors)
                    }
                    Note("Tab through the rows - focus sits inside the bounds, not outside.", page)
                }
            }

            Entry(
                name = "Switch",
                install = "add switch",
                note = "Position carries on/off; the accent reinforces it. Never the other way round.",
                page = page
            ) {
                val colors = if (dark) LamintraSwitchColors.dark() else LamintraSwitchColors.light()
                var a by remember { mutableStateOf(true) }
                var b by remember { mutableStateOf(false) }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        LamintraSwitch(a, { a = it }, colors = colors)
                        LamintraSwitch(b, { b = it }, colors = colors)
                        LamintraSwitch(true, {}, enabled = false, colors = colors)
                        LamintraSwitch(false, {}, enabled = false, colors = colors)
                    }
                    Note(
                        "The knob springs across with a little overshoot and carries a real " +
                            "shadow - drawn as fading strokes, so it renders on iOS and wasm too.",
                        page
                    )
                }
            }

            Entry(
                name = "Segmented",
                install = "add segmented",
                note = "One choice out of a few. Where the thumb is IS the answer, so it reads in grayscale.",
                page = page
            ) {
                val colors =
                    if (dark) LamintraSegmentedColors.dark() else LamintraSegmentedColors.light()
                var range by remember { mutableStateOf(1) }
                var view by remember { mutableStateOf(0) }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                    Note(
                        "Tap across two segments at once: the thumb retargets from wherever it " +
                            "is rather than restarting, and the labels fade in step with it " +
                            "because they read its position rather than running their own timer.",
                        page
                    )
                }
            }

            Spacer(Modifier.height(72.dp))
        }
    }
}

/* ---------- gallery chrome (brand surface, not component code) ---------- */

@Composable
private fun Entry(
    name: String,
    install: String,
    note: String,
    page: DirectionColors,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 40.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = name,
                style = DType.title.copy(color = page.ink),
                modifier = Modifier.weight(1f)
            )
            InstallPill(install, page)
        }
        Spacer(Modifier.height(4.dp))
        BasicText(text = note, style = DType.caption.copy(color = page.inkDim))
        Spacer(Modifier.height(18.dp))
        content()
    }
}

@Composable
private fun InstallPill(command: String, page: DirectionColors) {
    Box(
        modifier = Modifier
            .drawBehind {
                drawPath(
                    Squircle.path(size.width, size.height, size.height / 2f),
                    page.hairline
                )
            }
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        BasicText(
            text = command,
            style = DType.caption.copy(fontSize = 12.sp, color = page.inkDim)
        )
    }
}

@Composable
private fun Note(text: String, page: DirectionColors) {
    BasicText(text = text, style = DType.caption.copy(color = page.inkDim))
}

@Composable
private fun SchemeChip(dark: Boolean, page: DirectionColors, onToggle: () -> Unit) {
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
                    page.hairline,
                    style = Stroke(1.5.dp.toPx())
                )
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = if (dark) "Dark" else "Light",
            style = DType.caption.copy(fontSize = 13.sp, color = page.inkDim)
        )
    }
}
