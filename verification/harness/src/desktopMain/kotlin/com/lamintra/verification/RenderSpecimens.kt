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
import com.lamintra.glass_sheet.GlassBottomSheet
import com.lamintra.list_row.LamintraListRow
import com.lamintra.list_row.LamintraListRowColors
import com.lamintra.list_row.LamintraListRowDivider
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
                        value = "Balaji",
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
                        "Email", value = "balaji@mail.com",
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

        render("switch-$s", 380, 150) {
            Surface(scheme) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        LamintraSwitch(true, {}, colors = scheme.switch())
                        LamintraSwitch(false, {}, colors = scheme.switch())
                        LamintraSwitch(true, {}, enabled = false, colors = scheme.switch())
                    }
                }
            }
        }
    }

    // The signature tier. Rendered over a plausible page rather than an empty
    // canvas, because a sheet with nothing behind it does not show what a sheet
    // is for. Settled 2 seconds in so the slide-up enter animation has finished.
    render("glass-sheet-dark", 380, 300, settleNanos = 2_000_000_000L) {
        GlassSheetSpecimen()
    }

    // The hero: a whole screen made of nothing but registry components, which is
    // the claim the site has to make in one image.
    render("hero", 420, 700) { HeroScreen() }

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

@Composable
private fun GlassSheetSpecimen() {
    val s = Scheme.Dark
    Box(modifier = Modifier.fillMaxSize().background(s.surface)) {
        // Something for the sheet to sit over, so the scrim and the glass have
        // content to act on.
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            BasicText(
                "Library",
                style = TextStyle(
                    fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.7).sp, color = s.ink
                )
            )
            Spacer(Modifier.height(16.dp))
            LamintraCard(colors = s.card()) {
                LamintraListRow("Downloads", value = "24", colors = s.row())
                LamintraListRowDivider(colors = s.row())
                LamintraListRow("Offline", value = "6", colors = s.row())
            }
        }

        GlassBottomSheet(
            visible = true,
            onDismiss = {},
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) {
            Column {
                BasicText(
                    "Glass bottom sheet",
                    style = TextStyle(
                        fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.3).sp, color = Color(0xFFF2F5FA)
                    )
                )
                Spacer(Modifier.height(6.dp))
                BasicText(
                    "Drag down to dismiss, flick it, or tap the scrim.",
                    style = TextStyle(fontSize = 14.sp, color = Color(0xFF8A93A5))
                )
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun HeroScreen() {
    val s = Scheme.Dark
    Box(modifier = Modifier.fillMaxSize().background(s.surface)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(28.dp))
            BasicText(
                "Settings",
                style = TextStyle(
                    fontSize = 32.sp, lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp, color = s.ink
                )
            )

            Spacer(Modifier.height(28.dp))
            BasicText("Your name", style = dim(s))
            Spacer(Modifier.height(10.dp))
            LamintraTextField(
                value = "Balaji",
                onValueChange = {},
                placeholder = "Add a name",
                colors = s.field()
            )

            Spacer(Modifier.height(28.dp))
            BasicText("Account", style = dim(s))
            Spacer(Modifier.height(10.dp))
            LamintraCard(colors = s.card()) {
                LamintraListRow("Email", value = "balaji@mail.com", onClick = {}, colors = s.row())
                LamintraListRowDivider(colors = s.row())
                LamintraListRow("Plan", value = "Free", onClick = {}, colors = s.row())
            }

            Spacer(Modifier.height(28.dp))
            BasicText("Preferences", style = dim(s))
            Spacer(Modifier.height(10.dp))
            LamintraCard(colors = s.card()) {
                LamintraListRow("Notifications", colors = s.row()) {
                    LamintraSwitch(true, {}, colors = s.switch())
                }
                LamintraListRowDivider(colors = s.row())
                LamintraListRow("Haptic feedback", colors = s.row()) {
                    LamintraSwitch(false, {}, colors = s.switch())
                }
            }

            Spacer(Modifier.height(32.dp))
            LamintraButton("Save changes", {}, colors = s.button())
            Spacer(Modifier.height(12.dp))
            LamintraButton(
                "Cancel", {},
                emphasis = ButtonEmphasis.Secondary,
                colors = s.button()
            )
        }
    }
}
