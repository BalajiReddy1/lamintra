package com.lamintra.verification.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Emphasis is a parameter, not three separate components. */
enum class ButtonEmphasis { Primary, Ghost, Destructive }

/**
 * Per-component colours, per the mechanism in design/TOKENS.md:
 * `.dark()` / `.light()` / `.auto()`, defaulting to auto. The factories are
 * the migration seam for a future shared token layer.
 */
data class LayerButtonColors(
    val faceTop: Color,
    val faceBottom: Color,
    val base: Color,
    val hairline: Color,
    val edge: Color,
    val content: Color,
    val accent: Color,
    val onAccent: Color,
    val destructive: Color,
    val focus: Color
) {
    companion object {
        fun dark(
            accent: Color = Color(0xFF5B8CFF),
            destructive: Color = Color(0xFFE5484D)
        ): LayerButtonColors {
            val ink = Color(0xFFF2F5FA)
            return LayerButtonColors(
                // Lit from above: the face is brighter at its top edge.
                faceTop = ink.copy(alpha = 0.16f),
                faceBottom = ink.copy(alpha = 0.055f),
                // The base is the SHADOWED side of the extrusion, so it must be
                // darker than the host surface. The previous version used ink at
                // 28%, which on a dark surface drew a pale rim under the button —
                // that reads as a glow, not as depth, and was the main reason
                // this looked flat and cheap.
                base = Color.Black.copy(alpha = 0.55f),
                // The light catch along the top edge. This one detail does most
                // of the work in making a surface look real rather than filled.
                hairline = ink.copy(alpha = 0.34f),
                edge = ink.copy(alpha = 0.11f),
                content = ink.copy(alpha = 0.90f),
                accent = accent,
                onAccent = Color.White,
                destructive = destructive,
                focus = ink.copy(alpha = 0.55f)
            )
        }

        fun light(
            accent: Color = Color(0xFF2C5CE0),
            destructive: Color = Color(0xFFCE2C31)
        ): LayerButtonColors {
            val ink = Color(0xFF12151A)
            return LayerButtonColors(
                // On light the face cannot be a lighter fill without turning
                // grey, so it stays surface-coloured with only a whisper of
                // gradient, and depth comes from the cast base.
                faceTop = Color(0xFFFFFFFF),
                faceBottom = Color(0xFFF3F4F7),
                base = ink.copy(alpha = 0.22f),
                hairline = Color.White.copy(alpha = 0.9f),
                edge = ink.copy(alpha = 0.15f),
                content = ink.copy(alpha = 0.90f),
                accent = accent,
                onAccent = Color.White,
                destructive = destructive,
                focus = ink.copy(alpha = 0.45f)
            )
        }

        @Composable
        fun auto(): LayerButtonColors = if (isSystemInDarkTheme()) dark() else light()
    }
}

/**
 * The base-tier button, "defined by layer".
 *
 * Two planes: a face lit from above, and a base beneath it in shadow. Press
 * drives the face down onto the base and it bottoms out, like a key. Depth is
 * honest offset geometry — no blurred shadow — so it renders identically on
 * Android, iOS, desktop and wasm.
 */
@Composable
fun LayerButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: ButtonEmphasis = ButtonEmphasis.Ghost,
    enabled: Boolean = true,
    colors: LayerButtonColors = LayerButtonColors.auto(),
    cornerRadius: Dp = 12.dp,
    baseOffset: Dp = 4.dp,
    textStyle: TextStyle = TextStyle(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.1.sp
    )
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    // Rule 2: the face travels toward its base under direct pressure.
    val faceTravel by animateDpAsState(
        targetValue = if (pressed && enabled) baseOffset else 0.dp,
        animationSpec = tween(120)
    )
    // Rule 3: disabled collapses the offset entirely, so the component sits
    // flush and reads as unmistakably not-raised — form carrying the state,
    // not a grey fill.
    val restingBase by animateDpAsState(
        targetValue = if (enabled) baseOffset else 0.dp,
        animationSpec = tween(120)
    )

    val fill: Color? = when (emphasis) {
        ButtonEmphasis.Primary -> colors.accent
        ButtonEmphasis.Destructive -> colors.destructive
        ButtonEmphasis.Ghost -> null
    }
    val label = if (fill != null) colors.onAccent else colors.content

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 104.dp, minHeight = 46.dp)
            .alpha(if (enabled) 1f else 0.42f)
            .drawBehind {
                val path = Squircle.path(size.width, size.height, cornerRadius.toPx())

                // Focus: a detached outer contour, so it never competes with the
                // pressed state and needs no colour of its own to read.
                if (focused && enabled) {
                    val pad = 3.dp.toPx()
                    val ring = Squircle.path(
                        size.width + pad * 2,
                        size.height + pad * 2,
                        cornerRadius.toPx() + pad
                    )
                    translate(left = -pad, top = -pad) {
                        drawPath(ring, colors.focus, style = Stroke(1.5.dp.toPx()))
                    }
                }

                // Base plane — always darker than the surface. For a filled
                // button it is a darker shade of whatever accent the host
                // passed, derived rather than hard-coded so a custom accent
                // still casts a correct shadow.
                val baseColor = if (fill != null) lerp(fill, Color.Black, 0.45f) else colors.base
                translate(top = restingBase.toPx()) { drawPath(path, baseColor) }

                translate(top = faceTravel.toPx()) {
                    // Face, lit from above.
                    val faceBrush = if (fill != null) {
                        Brush.verticalGradient(
                            listOf(lerp(fill, Color.White, 0.10f), fill),
                            startY = 0f,
                            endY = size.height
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(colors.faceTop, colors.faceBottom),
                            startY = 0f,
                            endY = size.height
                        )
                    }
                    drawPath(path, faceBrush)

                    // Full contour, kept subtle — it defines the silhouette
                    // without turning into an outline-button look.
                    if (fill == null) {
                        drawPath(path, colors.edge, style = Stroke(1.dp.toPx()))
                    }

                    // The light catch: bright along the top edge, gone by the
                    // midpoint. This is the detail that separates a real
                    // surface from a filled rectangle.
                    drawPath(
                        path,
                        Brush.verticalGradient(
                            0f to (if (fill != null) Color.White.copy(alpha = 0.35f) else colors.hairline),
                            0.45f to Color.Transparent,
                            startY = 0f,
                            endY = size.height
                        ),
                        style = Stroke(1.dp.toPx())
                    )
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        BasicText(
            text = text,
            style = textStyle.copy(color = label),
            modifier = Modifier.padding(horizontal = 26.dp).padding(top = faceTravel)
        )
    }
}
