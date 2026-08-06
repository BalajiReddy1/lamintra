package com.lamintra.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamintra.verification.design.Gallery
import com.lamintra.verification.design.SettingsScreen

/**
 * The three surfaces a component has to survive, reachable from one window.
 *
 * - **Gallery** judges each component alone, with its states - the shape the
 *   website needs.
 * - **Settings screen** judges whether five of them sitting together still read
 *   as one family, which is wave 1's actual claim.
 * - **Signature** is the older screen for the glass sheet and the Day-1 neon
 *   fixtures, kept until those are retired. It is still in the previous design
 *   language, so the jump between tabs is expected.
 *
 * Claude cannot see any of this: the browser pane does not composite for it, so
 * a render it has "checked" is a render nobody has looked at. Three rounds of
 * design work were lost that way. This exists so a human can look in one pass.
 */
@Composable
fun HarnessRoot() {
    var surface by remember { mutableStateOf(HarnessSurface.Gallery) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (surface) {
            HarnessSurface.Gallery -> Gallery()
            HarnessSurface.Settings -> SettingsScreen()
            HarnessSurface.Signature -> VerificationScreen()
        }

        // Deliberately plain chrome. It is scaffolding, not a specimen - if it
        // were styled in the language it would compete with what it is framing.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xE60C0F14))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
        ) {
            HarnessSurface.entries.forEach { entry ->
                SurfaceTab(
                    label = entry.label,
                    selected = entry == surface,
                    onClick = { surface = entry }
                )
            }
        }
    }
}

enum class HarnessSurface(val label: String) {
    Gallery("GALLERY"),
    Settings("SETTINGS SCREEN"),
    Signature("SIGNATURE + DAY-1")
}

@Composable
private fun SurfaceTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        BasicText(
            text = label,
            style = TextStyle(
                fontSize = 10.5.sp,
                letterSpacing = 1.1.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) Color(0xFFF2F5FA) else Color(0xFF6B7280)
            )
        )
    }
}
