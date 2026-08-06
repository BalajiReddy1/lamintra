package com.lamintra.verification.design

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Component gallery — every component shown ISOLATED, with its own states and
 * its own install command.
 *
 * This replaces the settings-screen demo deliberately. Composing components
 * into one screen was misleading: it implied a developer copies a screen, when
 * in fact they install exactly one component at a time. This is also the shape
 * the website needs, so the harness and the site stay structurally the same.
 */
@Composable
fun Gallery() {
    var dark by remember { mutableStateOf(true) }
    val page = if (dark) GalleryPalette.dark() else GalleryPalette.light()
    val scroll: ScrollState = rememberScrollState()

    Box(
        modifier = Modifier.fillMaxSize().background(page.canvas),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(34.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    BasicText(
                        text = "Components",
                        style = TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp,
                            color = page.ink
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        text = "Base tier — defined by layer. Each one installs on its own.",
                        style = TextStyle(fontSize = 13.5.sp, color = page.dim)
                    )
                }
                SchemeChip(dark = dark, page = page) { dark = !dark }
            }

            Spacer(Modifier.height(34.dp))

            ComponentEntry(
                name = "Button",
                install = "add button",
                note = "Emphasis is a parameter, not three components — install one file, get all three.",
                page = page
            ) {
                val colors = if (dark) LayerButtonColors.dark() else LayerButtonColors.light()
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LayerButton("PRIMARY", {}, emphasis = ButtonEmphasis.Primary, colors = colors)
                        LayerButton("GHOST", {}, emphasis = ButtonEmphasis.Ghost, colors = colors)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        LayerButton("DELETE", {}, emphasis = ButtonEmphasis.Destructive, colors = colors)
                        LayerButton("DISABLED", {}, enabled = false, colors = colors)
                    }
                    StateNote(
                        "Press and hold — the face travels down and bottoms out on its base. " +
                            "Disabled collapses the offset instead of greying out.",
                        page
                    )
                }
            }

            Spacer(Modifier.height(60.dp))
        }
    }
}

/* ---------- gallery chrome (brand surface, not component code) ---------- */

internal data class GalleryPalette(
    val canvas: Color,
    val panel: Color,
    val hairline: Color,
    val ink: Color,
    val dim: Color,
    val mono: Color
) {
    companion object {
        // Neither pure black nor pure white, on purpose.
        fun dark() = GalleryPalette(
            canvas = Color(0xFF0C0F14),
            panel = Color(0xFF11151C),
            hairline = Color(0x1AF2F5FA),
            ink = Color(0xFFF2F5FA),
            dim = Color(0xFF8A93A5),
            mono = Color(0xFF7FD1E0)
        )

        fun light() = GalleryPalette(
            canvas = Color(0xFFF7F7F9),
            panel = Color(0xFFFCFCFE),
            hairline = Color(0x1A12151A),
            ink = Color(0xFF12151A),
            dim = Color(0xFF6B7280),
            mono = Color(0xFF1E7A8C)
        )
    }
}

@Composable
private fun ComponentEntry(
    name: String,
    install: String,
    note: String,
    page: GalleryPalette,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = name,
                style = TextStyle(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = page.ink
                ),
                modifier = Modifier.weight(1f)
            )
            InstallPill(install, page)
        }
        Spacer(Modifier.height(6.dp))
        BasicText(text = note, style = TextStyle(fontSize = 13.sp, color = page.dim))
        Spacer(Modifier.height(18.dp))

        // The specimen sits on its own panel so it is judged in isolation,
        // never as part of a composed screen.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    val p = Squircle.path(size.width, size.height, 16.dp.toPx())
                    drawPath(p, page.panel)
                    drawPath(p, page.hairline, style = Stroke(1.dp.toPx()))
                }
                .padding(26.dp)
        ) { content() }
    }
}

@Composable
private fun InstallPill(command: String, page: GalleryPalette) {
    Box(
        modifier = Modifier
            .drawBehind {
                val p = Squircle.path(size.width, size.height, 8.dp.toPx())
                drawPath(p, page.hairline)
            }
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        BasicText(
            text = command,
            style = TextStyle(fontSize = 12.sp, letterSpacing = 0.3.sp, color = page.mono)
        )
    }
}

@Composable
private fun StateNote(text: String, page: GalleryPalette) {
    BasicText(
        text = text,
        style = TextStyle(fontSize = 12.5.sp, color = page.dim.copy(alpha = 0.85f))
    )
}

@Composable
private fun SchemeChip(dark: Boolean, page: GalleryPalette, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .drawBehind {
                val p = Squircle.path(size.width, size.height, 9.dp.toPx())
                drawPath(p, page.hairline)
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 9.dp)
    ) {
        BasicText(
            text = if (dark) "DARK" else "LIGHT",
            style = TextStyle(
                fontSize = 10.5.sp,
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.Medium,
                color = page.dim
            )
        )
    }
}
