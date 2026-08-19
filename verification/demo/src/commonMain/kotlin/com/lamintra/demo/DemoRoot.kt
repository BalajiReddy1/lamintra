package com.lamintra.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
 * The live hero: the same settings screen `hero-dark.png` shows, except you can
 * touch it.
 *
 * **It has to match the static render closely**, because the two are the same
 * element at two moments in its life. The page paints the PNG immediately and
 * swaps this in behind a crossfade once the runtime has arrived, so any
 * difference in content or spacing reads as the page glitching rather than as
 * the page waking up.
 *
 * Every component here is a REGISTRY source staged by this module's build, so
 * what a visitor presses is exactly what `lamintra add` installs.
 *
 * There is deliberately no chrome. This canvas sits inside the site's own hero
 * frame, and the site owns everything around it.
 */
@Composable
fun DemoRoot() {
    // Seeded at first composition rather than corrected in an effect: an
    // earlier version defaulted to dark and fixed itself afterwards, so a
    // light-mode visitor watched the screen start dark and flip.
    //
    // The page tells Compose which scheme it resolved before first paint, and
    // that beats isSystemInDarkTheme() here because the reader may have chosen
    // a scheme that disagrees with their device.
    val systemDark = isSystemInDarkTheme()
    var dark by remember { mutableStateOf(pageIsDark() ?: systemDark) }

    LaunchedEffect(Unit) { markDemoReady() }

    // The site's control is the one a visitor sees, so this polls the page for
    // it rather than owning a control of its own. Compose is the passenger.
    ObservePageScheme { dark = it }

    val surface = if (dark) Color(0xFF0A0A0B) else Color(0xFFFFFFFF)
    val ink = if (dark) Color(0xFFFAFAFA) else Color(0xFF09090B)
    val dim = if (dark) Color(0xFF8B8B90) else Color(0xFF70707B)

    val buttonColors = if (dark) LamintraButtonColors.dark() else LamintraButtonColors.light()
    val cardColors = if (dark) LamintraCardColors.dark() else LamintraCardColors.light()
    val fieldColors = if (dark) LamintraTextFieldColors.dark() else LamintraTextFieldColors.light()
    val rowColors = if (dark) LamintraListRowColors.dark() else LamintraListRowColors.light()
    val switchColors = if (dark) LamintraSwitchColors.dark() else LamintraSwitchColors.light()
    val segmentedColors =
        if (dark) LamintraSegmentedColors.dark() else LamintraSegmentedColors.light()

    var name by remember { mutableStateOf("Ada Lovelace") }
    var notifications by remember { mutableStateOf(true) }
    var range by remember { mutableStateOf(1) }
    var saves by remember { mutableStateOf(0) }

    // Spacing is copied from HeroScreen in the harness, value for value. The
    // PNG that renders is what the visitor is looking at when this arrives, so
    // any difference here shows up as the page jumping rather than waking up.
    // No verticalScroll: the content is sized to fit 300x500, and a scroll
    // would let it come to rest cut through the middle of a row.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(surface)
            .padding(horizontal = 20.dp)
    ) {
        // 32, not 16. The title sat too close to the top of the frame and it
        // read as cropped rather than as a screen. Changed in BOTH files on
        // 2026-08-19: HeroScreen here and DemoRoot in :demo are the same screen,
        // and any difference between them shows on the page as a jump when the
        // canvas replaces the PNG rather than as it waking up.
        Spacer(Modifier.height(32.dp))
        BasicText(
            "Settings",
            style = TextStyle(
                fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold,
                letterSpacing = (-0.7).sp, color = ink
            )
        )

        Spacer(Modifier.height(14.dp))
        Caption("Your name", dim)
        Spacer(Modifier.height(4.dp))
        LamintraTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = "Add a name",
            colors = fieldColors
        )

        Spacer(Modifier.height(16.dp))
        LamintraSegmented(
            options = listOf("Day", "Week", "Month"),
            selected = range,
            onSelect = { range = it },
            colors = segmentedColors
        )

        Spacer(Modifier.height(16.dp))
        Caption("Account", dim)
        Spacer(Modifier.height(4.dp))
        LamintraCard(colors = cardColors) {
            LamintraListRow("Email", value = "you@example.com", onClick = {}, colors = rowColors)
            LamintraListRowDivider(colors = rowColors)
            LamintraListRow("Notifications", colors = rowColors) {
                LamintraSwitch(notifications, { notifications = it }, colors = switchColors)
            }
        }

        Spacer(Modifier.height(16.dp))
        LamintraButton(
            text = if (saves == 0) "Save changes" else "Saved x$saves",
            onClick = { saves++ },
            colors = buttonColors
        )
    }
}

@Composable
private fun Caption(text: String, color: Color) {
    BasicText(
        text,
        style = TextStyle(
            fontSize = 13.sp, lineHeight = 20.sp,
            letterSpacing = (-0.1).sp, color = color
        )
    )
}
