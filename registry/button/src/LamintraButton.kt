package com.lamintra.button

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamintra.button.internal.button.Squircle

/**
 * Emphasis is a parameter, not four separate components - install one file and
 * get all four. This mirrors how shadcn/ui treats `variant`, and it is why the
 * registry will never need a `primary-button` alongside this one.
 */
enum class ButtonEmphasis { Primary, Secondary, Ghost, Destructive }

/** Two sizes, both at or above the 44dp minimum touch target. */
enum class ButtonSize(val height: Dp, val horizontalPadding: Dp) {
    Large(56.dp, 28.dp),
    Medium(44.dp, 20.dp)
}

/**
 * Colours for [LamintraButton].
 *
 * The three factory names - `dark()`, `light()`, `auto()` - are identical on
 * every Lamintra component on purpose. They are the migration seam: a future
 * shared token layer changes only what `auto()` reads, without touching a
 * single call site. Do not rename them.
 *
 * **Ink carries action; accent carries state.** A button is an action, so a
 * primary button is solid ink rather than solid accent - which is also what
 * keeps it from competing with the components that do encode state.
 */
data class LamintraButtonColors(
    val ink: Color,
    val onInk: Color,
    val inkDim: Color,
    val hairline: Color,
    val danger: Color,
    val onDanger: Color,
    val focus: Color
) {
    companion object {
        fun dark(): LamintraButtonColors = LamintraButtonColors(
            // Neither pure black nor pure white anywhere: pure values vibrate
            // on OLED and read as an unstyled page rather than a designed one.
            ink = Color(0xFFFAFAFA),
            onInk = Color(0xFF0A0A0B),
            inkDim = Color(0xFF8B8B90),
            hairline = Color(0xFF26262A),
            danger = Color(0xFFF04438),
            onDanger = Color(0xFFFFFFFF),
            focus = Color(0xFFFAFAFA)
        )

        fun light(): LamintraButtonColors = LamintraButtonColors(
            ink = Color(0xFF09090B),
            onInk = Color(0xFFFFFFFF),
            inkDim = Color(0xFF70707B),
            hairline = Color(0xFFE4E4E7),
            danger = Color(0xFFD92D20),
            onDanger = Color(0xFFFFFFFF),
            focus = Color(0xFF09090B)
        )

        /**
         * Follows the device's colour scheme. This is the default because when
         * it is wrong it is wrong *loudly* - a dark button on a light app is
         * glaring and takes one parameter to fix.
         *
         * Note it reads the *device* setting, not your app's theme. An app that
         * forces its own scheme should pass [dark] or [light] explicitly.
         */
        @Composable
        fun auto(): LamintraButtonColors = if (isSystemInDarkTheme()) dark() else light()
    }
}

/**
 * A button.
 *
 * Flat solid fill, capsule silhouette, and a press that scales the whole
 * control down on a **spring** rather than a fixed-duration curve. That last
 * detail is not cosmetic: a spring carries velocity through an interruption and
 * a duration curve cannot, which is the whole of what "smooth" means in an iOS
 * or React Native app.
 *
 * No gradients, no elevation planes, no blurred shadows - so it renders
 * identically on Android, iOS, desktop and wasm. `compose.foundation` only.
 *
 * @param text the label. Sentence case; this component never transforms your
 *        content, because `uppercase()` here would corrupt every non-Latin
 *        script and is the loudest dated tell in a UI besides
 * @param onClick called on tap when [enabled]
 * @param emphasis Primary fills with ink, Destructive with the danger colour,
 *        Secondary draws a contour only, Ghost is text alone
 * @param enabled when false the button drops to [disabledAlpha] and ignores taps
 * @param colors see [LamintraButtonColors]; defaults to following the system
 * @param cornerRadius `null` means a capsule (half the height), which is the
 *        default silhouette for an action. Pass a value for a tighter shape
 * @param pressScale how far the control shrinks while held
 * @param textStyle label style. Never sets `fontFamily`, so the host app's
 *        typeface is used; replace it wholesale to restyle the label
 */
@Composable
fun LamintraButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    emphasis: ButtonEmphasis = ButtonEmphasis.Primary,
    size: ButtonSize = ButtonSize.Large,
    enabled: Boolean = true,
    fillWidth: Boolean = true,
    colors: LamintraButtonColors = LamintraButtonColors.auto(),
    cornerRadius: Dp? = null,
    contentPadding: Dp = size.horizontalPadding,
    pressScale: Float = 0.97f,
    disabledAlpha: Float = 0.3f,
    borderWidth: Dp = 1.5.dp,
    focusRingGap: Dp = 4.dp,
    focusRingWidth: Dp = 2.dp,
    textStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp
    )
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()

    // Brisk, barely bouncy - Apple's "bounce 0.15" band. The motion IS the
    // press feedback; nothing travels and nothing casts a shadow.
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressScale else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 1400f)
    )

    val fill: Color? = when (emphasis) {
        ButtonEmphasis.Primary -> colors.ink
        ButtonEmphasis.Destructive -> colors.danger
        ButtonEmphasis.Secondary, ButtonEmphasis.Ghost -> null
    }
    val label = when (emphasis) {
        ButtonEmphasis.Primary -> colors.onInk
        ButtonEmphasis.Destructive -> colors.onDanger
        ButtonEmphasis.Secondary -> colors.ink
        ButtonEmphasis.Ghost -> colors.inkDim
    }

    Box(
        modifier = modifier
            .then(if (fillWidth) Modifier.fillMaxWidth() else Modifier)
            .height(size.height)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else disabledAlpha
            }
            .drawBehind {
                val radius = cornerRadius?.toPx() ?: (this.size.height / 2f)
                val path = Squircle.path(this.size.width, this.size.height, radius)

                if (focused && enabled) {
                    val pad = focusRingGap.toPx()
                    val ring = Squircle.path(
                        this.size.width + pad * 2,
                        this.size.height + pad * 2,
                        radius + pad
                    )
                    translate(left = -pad, top = -pad) {
                        drawPath(ring, colors.focus, style = Stroke(focusRingWidth.toPx()))
                    }
                }

                when {
                    fill != null -> drawPath(path, fill)
                    emphasis == ButtonEmphasis.Secondary ->
                        drawPath(path, colors.hairline, style = Stroke(borderWidth.toPx()))
                    else -> Unit
                }
            }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = contentPadding),
        contentAlignment = Alignment.Center
    ) {
        BasicText(text = text, style = textStyle.copy(color = label))
    }
}
