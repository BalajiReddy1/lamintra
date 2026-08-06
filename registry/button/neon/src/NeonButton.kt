package com.lamintra.button.neon

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lamintra.button.neon.internal.button_neon.neonGlow

/**
 * A neon glow button built entirely on compose.foundation — no Material 3.
 * The glow is drawn as layered, widening, fading strokes (no platform blur
 * or coloured-shadow API), so it renders identically on Android, iOS and
 * desktop. Press brightens the glow instead of using a ripple, keeping the
 * component indication-free and theme-agnostic.
 *
 * For the outline-only variant, see `button/neon_outline`.
 *
 * @param text the button label
 * @param onClick called on tap when [enabled]
 * @param glowColor the neon colour used for the wash, edge and halo
 * @param contentColor the label colour; defaults to [glowColor]
 * @param cornerRadius corner radius of the glow shape
 * @param textStyle label style. Never sets `fontFamily`, so the host app's
 *        typeface is used; replace it wholesale to restyle the label.
 * @param enabled when false, the glow dims and taps are ignored
 */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = Color(0xFF00E5FF),
    contentColor: Color = glowColor,
    cornerRadius: Dp = 12.dp,
    textStyle: TextStyle = TextStyle(
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp
    ),
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val glowAlpha by animateFloatAsState(
        targetValue = when {
            !enabled -> 0.25f
            pressed -> 1f
            else -> 0.6f
        },
        animationSpec = tween(durationMillis = 120)
    )

    BasicText(
        text = text,
        style = textStyle.copy(
            color = contentColor.copy(alpha = if (enabled) 1f else 0.4f)
        ),
        modifier = modifier
            .neonGlow(
                color = glowColor,
                cornerRadius = cornerRadius,
                glowAlpha = glowAlpha
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}
