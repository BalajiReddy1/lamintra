package com.lamintra.list_row

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import com.lamintra.theme.LamintraColors
import com.lamintra.theme.LamintraTheme
import com.lamintra.theme.lamintraDarkColors
import com.lamintra.theme.lamintraLightColors
import com.lamintra.list_row.internal.list_row.Squircle

/**
 * Colours for [LamintraListRow].
 *
 * The three factory names - `dark()`, `light()`, `auto()` - are identical on
 * every Lamintra component on purpose. They are the migration seam: a future
 * shared token layer changes only what `auto()` reads, without touching a
 * single call site. Do not rename them.
 */
data class LamintraListRowColors(
    val content: Color,
    val contentSecondary: Color,
    val divider: Color,
    val focus: Color
) {
    companion object {
        /** Maps the shared semantic layer onto this component's own slots. */
        fun from(colors: LamintraColors): LamintraListRowColors = LamintraListRowColors(
            content = colors.ink,
            contentSecondary = colors.inkMuted,
            divider = colors.hairline,
            focus = colors.focus
        )

        fun dark(): LamintraListRowColors = from(lamintraDarkColors())

        fun light(): LamintraListRowColors = from(lamintraLightColors())

        /**
         * Follows the device's colour scheme. Reads the *device* setting, not
         * your app's theme - an app that forces its own scheme should pass
         * [dark] or [light] explicitly.
         */
        @Composable
        fun auto(): LamintraListRowColors = from(LamintraTheme.colors)
    }
}

/**
 * A row of a settings or detail list, designed to sit inside a LamintraCard.
 *
 * That reference is prose rather than a KDoc link on purpose. A link would have
 * to be fully qualified, and the installer only rewrites a component's own
 * package. Another component's package would survive verbatim and point
 * somewhere that need not exist here, since components install independently.
 *
 * **A pressed row dims.** It does not rise, recess, travel or cast anything -
 * a row is a region of a surface rather than an object floating on one, and
 * dimming is what both Uber and Apple actually do. An earlier version of this
 * component built an elaborate recessing face for the same job; it was a
 * detailed answer to a question that does not need one.
 *
 * @param label the primary text
 * @param value optional secondary text, right-aligned before [trailing]
 * @param onClick when non-null the row becomes interactive
 * @param enabled drops the row to [disabledAlpha] and ignores taps
 * @param colors see [LamintraListRowColors]; defaults to following the system
 * @param pressedAlpha how far the row dims while held
 */
@Composable
fun LamintraListRow(
    label: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: LamintraListRowColors = LamintraListRowColors.auto(),
    minHeight: Dp = 60.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    contentSpacing: Dp = 12.dp,
    pressedAlpha: Float = 0.5f,
    disabledAlpha: Float = 0.3f,
    focusCornerRadius: Dp = 12.dp,
    focusRingInset: Dp = 4.dp,
    focusRingWidth: Dp = 2.dp,
    labelTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    valueTextStyle: TextStyle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.2).sp
    ),
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (RowScope.() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val interactive = onClick != null

    val alpha by animateFloatAsState(
        targetValue = when {
            interactive && !enabled -> disabledAlpha
            pressed && enabled && interactive -> pressedAlpha
            else -> 1f
        },
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 1400f)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = minHeight)
            .graphicsLayer { this.alpha = alpha }
            .drawBehind {
                // Focus sits INSIDE the row's bounds, unlike the button's
                // detached ring - a row is flush against the card's edge, so an
                // outward ring would be clipped or collide with its corners.
                if (!focused || !enabled || !interactive) return@drawBehind
                val inset = focusRingInset.toPx()
                val w = size.width - inset * 2
                if (w <= 0f) return@drawBehind
                translate(left = inset) {
                    drawPath(
                        Squircle.path(w, size.height - inset * 2, focusCornerRadius.toPx()),
                        colors.focus,
                        style = Stroke(focusRingWidth.toPx())
                    )
                }
            }
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(contentSpacing)
    ) {
        leading?.invoke()
        BasicText(
            text = label,
            style = labelTextStyle.copy(color = colors.content),
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            BasicText(text = value, style = valueTextStyle.copy(color = colors.contentSecondary))
        }
        trailing?.invoke(this)
    }
}

/**
 * The hairline that separates two [LamintraListRow]s.
 *
 * It ships with the row rather than with the card because it divides rows - a
 * card holding anything else should not inherit a row separator. Components are
 * standalone by design, so this is the only correct home for it.
 *
 * @param startIndent leaves the divider clear of a leading icon or avatar,
 *        which is what stops a list reading as a table
 */
@Composable
fun LamintraListRowDivider(
    modifier: Modifier = Modifier,
    colors: LamintraListRowColors = LamintraListRowColors.auto(),
    startIndent: Dp = 20.dp,
    thickness: Dp = 1.dp
) {
    Box(modifier = modifier.fillMaxWidth().padding(start = startIndent)) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(thickness)
                .drawBehind { drawRect(colors.divider) }
        )
    }
}
