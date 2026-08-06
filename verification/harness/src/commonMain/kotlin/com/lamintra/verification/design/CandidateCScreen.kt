package com.lamintra.verification.design

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
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Candidate C rendered in the real medium, on a real screen, with real content.
 *
 * This exists because three HTML mockups of the same idea shipped broken — a
 * lozenge shape, muddy light-mode greys, then diagonal chords across every
 * surface. Compose is the target medium, so nothing here can be true in the
 * mockup and false in the product.
 */
@Composable
fun CandidateCScreen() {
    var dark by remember { mutableStateOf(true) }
    val colors = if (dark) LayerColors.dark() else LayerColors.light()

    var notifications by remember { mutableStateOf(true) }
    var haptics by remember { mutableStateOf(false) }
    var saves by remember { mutableStateOf(0) }
    var tab by remember { mutableStateOf(0) }
    var volume by remember { mutableStateOf(0.42f) }

    Box(
        modifier = Modifier.fillMaxSize().background(colors.surface),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth().padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    text = "Settings",
                    style = TextStyle(
                        fontSize = 27.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.ink
                    ),
                    modifier = Modifier.weight(1f)
                )
                SchemeToggle(dark = dark, colors = colors, onToggle = { dark = !dark })
            }

            Spacer(Modifier.height(24.dp))
            CField(label = "Display name", value = "Balaji", colors = colors)

            Spacer(Modifier.height(22.dp))
            SectionLabel("Account", colors)
            CCard(colors) {
                CRow(label = "Email", colors = colors, value = "balaji@mail.com")
                CDivider(colors)
                CRow(label = "Plan", colors = colors, value = "Free")
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Preferences", colors)
            CCard(colors) {
                CRow(label = "Notifications", colors = colors) {
                    CSwitch(notifications, { notifications = it }, colors)
                }
                CDivider(colors)
                CRow(label = "Haptic feedback", colors = colors) {
                    CSwitch(haptics, { haptics = it }, colors)
                }
            }

            // ---- the slider test: two components the language was NOT
            // designed around, built only from its existing rules ----
            Spacer(Modifier.height(22.dp))
            SectionLabel("Derived — not designed for", colors)
            CTabs(
                options = listOf("Day", "Week", "Month"),
                selected = tab,
                onSelect = { tab = it },
                colors = colors
            )
            Spacer(Modifier.height(18.dp))
            CSlider(value = volume, onValueChange = { volume = it }, colors = colors)

            Spacer(Modifier.height(26.dp))
            CButton(
                text = if (saves == 0) "SAVE CHANGES" else "SAVED ×$saves",
                onClick = { saves++ },
                colors = colors,
                primary = true
            )
            Spacer(Modifier.height(12.dp))
            CButton(text = "CANCEL", onClick = { saves = 0 }, colors = colors)
            Spacer(Modifier.height(12.dp))
            CButton(text = "DISABLED", onClick = { saves++ }, colors = colors, enabled = false)

            Spacer(Modifier.height(18.dp))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                BasicText(
                    text = "DELETE ACCOUNT",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.2.sp,
                        color = colors.danger
                    )
                )
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String, colors: LayerColors) {
    BasicText(
        text = text.uppercase(),
        style = TextStyle(fontSize = 11.sp, letterSpacing = 1.3.sp, color = colors.inkFaint),
        modifier = Modifier.padding(start = 2.dp, bottom = 9.dp)
    )
}

@Composable
private fun SchemeToggle(dark: Boolean, colors: LayerColors, onToggle: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(32.dp)
            .layerSurface(colors, cornerRadius = 10.dp, baseOffset = 2.dp)
            .clickable(interactionSource = interaction, indication = null, onClick = onToggle)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = if (dark) "DARK" else "LIGHT",
            style = TextStyle(
                fontSize = 10.5.sp,
                letterSpacing = 1.1.sp,
                fontWeight = FontWeight.Medium,
                color = colors.inkDim
            )
        )
    }
}
