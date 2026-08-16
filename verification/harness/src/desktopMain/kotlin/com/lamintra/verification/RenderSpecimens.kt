package com.lamintra.verification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
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
import com.lamintra.sheet.LamintraSheet
import com.lamintra.sheet.LamintraSheetColors
import com.lamintra.switch.LamintraSwitch
import com.lamintra.switch.LamintraSwitchColors
import com.lamintra.text_field.LamintraTextField
import com.lamintra.text_field.LamintraTextFieldColors
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/**
 * Renders every registry component to a PNG, headlessly, straight from the
 * sources the CLI installs.
 *
 * This exists because the website needs images of the components, and there are
 * only three ways to get them: screenshot them by hand, rebuild them in HTML, or
 * render the real thing. The first does not survive a component change and the
 * second is the drift this project has been bitten by repeatedly. This is the
 * third.
 *
 * It also doubles as visual regression material: the output is deterministic, so
 * a diff on these files is a diff on what the components actually look like.
 *
 * Usage: ./gradlew :harness:renderSpecimens -PoutDir=<absolute path>
 */
fun main(args: Array<String>) {
    val outDir = File(args.firstOrNull() ?: "build/specimens").absoluteFile
    outDir.mkdirs()

    // 2x so the images stay sharp on a retina display without being enormous.
    val density = Density(2f)

    /**
     * [settleNanos] advances the scene's clock before capturing. Components with
     * an enter animation render at their starting frame otherwise, which for the
     * sheet means capturing it still off-screen.
     */
    fun render(
        name: String,
        width: Int,
        height: Int,
        settleNanos: Long = 0L,
        content: @Composable () -> Unit
    ) {
        val scene = ImageComposeScene(
            width = width * 2,
            height = height * 2,
            density = density,
            content = content
        )
        try {
            if (settleNanos > 0L) {
                // One frame to compose, then jump past the animation's duration.
                scene.render(0L)
            }
            val image = scene.render(settleNanos)
            val data = image.encodeToData(EncodedImageFormat.PNG)
                ?: error("Failed to encode $name to PNG")
            val file = File(outDir, "$name.png")
            file.writeBytes(data.bytes)
            println("wrote ${file.name} (${data.size} bytes, ${width}x${height} at 2x)")
        } finally {
            scene.close()
        }
    }

    Scheme.entries.forEach { scheme ->
        val s = scheme.suffix

        render("button-$s", 380, 300) {
            Surface(scheme) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LamintraButton("Save changes", {}, colors = scheme.button())
                    LamintraButton(
                        "Cancel", {},
                        emphasis = ButtonEmphasis.Secondary,
                        colors = scheme.button()
                    )
                    LamintraButton(
                        "Delete account", {},
                        emphasis = ButtonEmphasis.Destructive,
                        colors = scheme.button()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        LamintraButton(
                            "Medium", {},
                            size = ButtonSize.Medium, fillWidth = false,
                            colors = scheme.button()
                        )
                        LamintraButton(
                            "Disabled", {},
                            size = ButtonSize.Medium, fillWidth = false, enabled = false,
                            colors = scheme.button()
                        )
                    }
                }
            }
        }

        render("card-$s", 380, 230) {
            Surface(scheme) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LamintraCard(colors = scheme.card(), contentPadding = PaddingValues(20.dp)) {
                        BasicText("Static", style = body(scheme))
                        Spacer(Modifier.height(4.dp))
                        BasicText("Groups content. No press.", style = dim(scheme))
                    }
                    LamintraCard(
                        onClick = {},
                        colors = scheme.card(),
                        contentPadding = PaddingValues(20.dp)
                    ) {
                        BasicText("Interactive", style = body(scheme))
                        Spacer(Modifier.height(4.dp))
                        BasicText("Presses on a spring.", style = dim(scheme))
                    }
                }
            }
        }

        render("text-field-$s", 380, 250) {
            Surface(scheme) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LamintraTextField(
                        value = "Ada Lovelace",
                        onValueChange = {},
                        label = "Display name",
                        colors = scheme.field()
                    )
                    LamintraTextField(
                        value = "",
                        onValueChange = {},
                        placeholder = "you@example.com",
                        colors = scheme.field()
                    )
                }
            }
        }

        render("list-row-$s", 380, 260) {
            Surface(scheme) {
                LamintraCard(colors = scheme.card()) {
                    LamintraListRow(
                        "Email", value = "you@example.com",
                        onClick = {}, colors = scheme.row()
                    )
                    LamintraListRowDivider(colors = scheme.row())
                    LamintraListRow("Plan", value = "Free", onClick = {}, colors = scheme.row())
                    LamintraListRowDivider(colors = scheme.row())
                    LamintraListRow("Notifications", colors = scheme.row()) {
                        LamintraSwitch(true, {}, colors = scheme.switch())
                    }
                }
            }
        }

        // Height sized to the content. At 150 the canvas left visible dead
        // space under a single row of switches, which showed up on the page as
        // an oddly tall empty frame.
        render("switch-$s", 380, 100) {
            Surface(scheme) {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    LamintraSwitch(true, {}, colors = scheme.switch())
                    LamintraSwitch(false, {}, colors = scheme.switch())
                    LamintraSwitch(true, {}, enabled = false, colors = scheme.switch())
                }
            }
        }

        // 212 = three 44dp controls, two 16dp gaps, and the Surface's own 24dp
        // top and bottom. Sized exactly rather than rounded up, because dead
        // canvas under a specimen shows on the page as an oddly tall frame.
        render("segmented-$s", 380, 212) {
            Surface(scheme) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    LamintraSegmented(
                        options = listOf("Day", "Week", "Month"),
                        selected = 1,
                        onSelect = {},
                        colors = scheme.segmented()
                    )
                    LamintraSegmented(
                        options = listOf("All", "Unread"),
                        selected = 0,
                        onSelect = {},
                        colors = scheme.segmented()
                    )
                    LamintraSegmented(
                        options = listOf("Day", "Week", "Month"),
                        selected = 2,
                        onSelect = {},
                        enabled = false,
                        colors = scheme.segmented()
                    )
                }
            }
        }

        // The only specimen that cannot use Surface. The scrim is full-bleed by
        // design, and Surface's 24dp padding would draw an undimmed frame around
        // it - which would misrepresent the component rather than merely look
        // wrong.
        //
        // Rows sit behind it on purpose. A scrim over an empty page is invisible,
        // so half the component would not be in its own photograph. It is also
        // the only specimen showing two components composed, which is the claim
        // the set makes and nowhere demonstrates.
        //
        // A still cannot show the drag, the velocity-seeded settle, or the scrim
        // tracking the finger, which are the reasons this component exists. This
        // is the resting position and nothing more.
        render("sheet-$s", 380, 300) {
            Box(Modifier.fillMaxSize().background(scheme.surface)) {
                Column(Modifier.padding(24.dp)) {
                    LamintraListRow("Plan", value = "Free", onClick = {}, colors = scheme.row())
                    LamintraListRowDivider(colors = scheme.row())
                    LamintraListRow("Storage", value = "12 GB", onClick = {}, colors = scheme.row())
                }
                LamintraSheet(
                    visible = true,
                    onDismiss = {},
                    colors = scheme.sheet(),
                    contentWindowInsets = WindowInsets(0)
                ) {
                    BasicText("Move to folder", style = body(scheme))
                    Spacer(Modifier.height(6.dp))
                    BasicText(
                        "Choose where this goes. You can move it again later.",
                        style = dim(scheme)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // The hero: a whole screen made of nothing but registry components, which
        // is the claim the site has to make in one image. It sits inside the
        // scheme loop because the site's toggle drives every specimen, and the
        // hero was once the only render with no light half.
        //
        // 300x500, which is exactly the box the site gives it. It used to be
        // 420x700 and the browser downscaled the PNG, which was fine while this
        // was only ever an image. It is now also a live Compose canvas, and a
        // canvas cannot be downscaled without taking its 44dp touch targets down
        // to 31px with it. Laying out at the delivered size keeps them honest,
        // and 300x500 at 2x is still exactly 2x on the page.
        render("hero-$s", 300, 500) { HeroScreen(scheme) }
    }

    println("done: ${outDir.absolutePath}")
}

private enum class Scheme(val suffix: String, val surface: Color, val ink: Color, val dimInk: Color) {
    Dark("dark", Color(0xFF0A0A0B), Color(0xFFFAFAFA), Color(0xFF8B8B90)),
    Light("light", Color(0xFFFFFFFF), Color(0xFF09090B), Color(0xFF70707B));

    fun button() = if (this == Dark) LamintraButtonColors.dark() else LamintraButtonColors.light()
    fun card() = if (this == Dark) LamintraCardColors.dark() else LamintraCardColors.light()
    fun field() = if (this == Dark) LamintraTextFieldColors.dark() else LamintraTextFieldColors.light()
    fun row() = if (this == Dark) LamintraListRowColors.dark() else LamintraListRowColors.light()
    fun switch() = if (this == Dark) LamintraSwitchColors.dark() else LamintraSwitchColors.light()
    fun segmented() =
        if (this == Dark) LamintraSegmentedColors.dark() else LamintraSegmentedColors.light()
    fun sheet() = if (this == Dark) LamintraSheetColors.dark() else LamintraSheetColors.light()
}

private fun body(s: Scheme) =
    TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp, color = s.ink)

private fun dim(s: Scheme) =
    TextStyle(fontSize = 13.sp, lineHeight = 20.sp, letterSpacing = (-0.1).sp, color = s.dimInk)

@Composable
private fun Surface(scheme: Scheme, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(scheme.surface).padding(24.dp)
    ) { content() }
}

/**
 * Sized to fit 300x500 with room to spare, and it has to STAY fitting, because
 * the site swaps this render for a live Compose canvas of the same box. If the
 * content grows past the box the canvas starts scrolling and comes to rest cut
 * through the middle of a row, which reads as a broken page rather than a phone
 * screen.
 *
 * Budget, measured rather than guessed: text field 56, list row 60, segmented
 * 44, button 56. Every component in the registry appears exactly once, which is
 * the actual job of this image.
 *
 * Kept character-identical to `DemoRoot` in the :demo module. The two are the
 * same element at two moments in its life, so any difference between them shows
 * up on the page as a jump at the moment the runtime arrives.
 */
@Composable
private fun HeroScreen(s: Scheme) {
    Box(modifier = Modifier.fillMaxSize().background(s.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Spacer(Modifier.height(16.dp))
            BasicText(
                "Settings",
                style = TextStyle(
                    fontSize = 28.sp, lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp, color = s.ink
                )
            )

            Spacer(Modifier.height(14.dp))
            BasicText("Your name", style = dim(s))
            Spacer(Modifier.height(4.dp))
            LamintraTextField(
                value = "Ada Lovelace",
                onValueChange = {},
                placeholder = "Add a name",
                colors = s.field()
            )

            Spacer(Modifier.height(16.dp))
            LamintraSegmented(
                options = listOf("Day", "Week", "Month"),
                selected = 1,
                onSelect = {},
                colors = s.segmented()
            )

            Spacer(Modifier.height(16.dp))
            BasicText("Account", style = dim(s))
            Spacer(Modifier.height(4.dp))
            LamintraCard(colors = s.card()) {
                LamintraListRow("Email", value = "you@example.com", onClick = {}, colors = s.row())
                LamintraListRowDivider(colors = s.row())
                LamintraListRow("Notifications", colors = s.row()) {
                    LamintraSwitch(true, {}, colors = s.switch())
                }
            }

            Spacer(Modifier.height(16.dp))
            LamintraButton("Save changes", {}, colors = s.button())
        }
    }
}
