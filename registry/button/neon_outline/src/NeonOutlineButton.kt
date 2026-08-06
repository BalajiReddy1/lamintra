package com.lamintra.button.neon_outline

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
import com.lamintra.button.neon_outline.internal.button_neon_outline.neonOutline

/**
 * A neon-outlined button built entirely on compose.foundation — no
 * Material 3. The glow is drawn as layered, widening, fading strokes
 * (no platform blur API), so it renders identically on Android, iOS,
 * and desktop. Press feedback brightens the glow instead of using a
 * ripple, keeping the component indication-free and theme-agnostic.
 *
 * @param text the button label
 * @param onClick called on tap when [enabled]
 * @param color the neon color used for the outline
 * @param contentColor the label colour; defaults to [color]. Separate from
 *        [color] because the outline sits on the component's own glow while
 *        the label sits on the host app's background — at the default
 *        `#00E5FF` those are the same on dark and very different on light,
 *        where the label needs its own darker value to stay legible.
 * @param cornerRadius corner radius of the outline
 * @param textStyle label style. Never sets `fontFamily`, so the host app's
 *        typeface is used; replace it wholesale to restyle the label.
 * @param enabled when false, the button dims and ignores taps
 */
@Composable
fun NeonOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF00E5FF),
    contentColor: Color = color,
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
            .neonOutline(color = color, cornerRadius = cornerRadius, glowAlpha = glowAlpha)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp)
    )
}
