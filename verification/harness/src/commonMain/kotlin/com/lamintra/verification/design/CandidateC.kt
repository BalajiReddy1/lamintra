package com.lamintra.verification.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Candidate C — the base tier "defined by LAYER".
 *
 * Every interactive component is two planes: a face, and a base peeking out
 * beneath it. Depth comes from honest offset geometry rather than a blurred
 * shadow, so it renders identically on Android, iOS, desktop and wasm — the
 * same reason NeonButton's Android-only shadow had to be rebuilt.
 *
 * The restraint that keeps this a language rather than an embossing effect:
 * only INTERACTIVE things get a base plane. Static surfaces stay flat and are
 * divided by hairlines.
 *
 * Everything derives from two host-supplied colours — surface and ink — which
 * is what lets it survive a theme we never chose.
 */
internal data class LayerColors(
    val surface: Color,
    val ink: Color,
    val base: Color,
    val face: Color,
    val edge: Color,
    val cardFill: Color,
    val cardEdge: Color,
    val accent: Color,
    val onAccent: Color,
    val danger: Color
) {
    val inkDim: Color get() = ink.copy(alpha = 0.5f)
    val inkFaint: Color get() = ink.copy(alpha = 0.45f)

    companion object {
        private val ACCENT = Color(0xFF4C7DF0)

        fun dark(accent: Color = ACCENT): LayerColors {
            val ink = Color(0xFFF2F5FA)
            return LayerColors(
                surface = Color(0xFF0F1216),
                ink = ink,
                base = ink.copy(alpha = 0.24f),
                face = ink.copy(alpha = 0.10f),
                edge = ink.copy(alpha = 0.22f),
                cardFill = ink.copy(alpha = 0.045f),
                cardEdge = ink.copy(alpha = 0.10f),
                accent = accent,
                onAccent = Color.White,
                danger = Color(0xFFE5484D)
            )
        }

        /**
         * On dark, a raised face is a LIGHTER fill. That identical rule on light
         * produces a dirty grey slab, so elevation is re-expressed: the face
         * stays surface-coloured and depth comes from the cast base plus a finer
         * hairline. Same structural idea, corrected physics.
         */
        fun light(accent: Color = ACCENT): LayerColors {
            val ink = Color(0xFF12151A)
            val surface = Color(0xFFFBFBFD)
            return LayerColors(
                surface = surface,
                ink = ink,
                base = ink.copy(alpha = 0.15f),
                face = surface,
                edge = ink.copy(alpha = 0.13f),
                cardFill = surface,
                cardEdge = ink.copy(alpha = 0.12f),
                accent = accent,
                onAccent = Color.White,
                danger = Color(0xFFCE2C31)
            )
        }
    }
}

/** Interactive surface: base plane beneath, face on top. */
internal fun Modifier.layerSurface(
    colors: LayerColors,
    cornerRadius: Dp,
    baseOffset: Dp = 3.dp,
    faceOffset: Dp = 0.dp,
    fill: Color? = null
): Modifier = drawBehind {
    val path = Squircle.path(size.width, size.height, cornerRadius.toPx())
    translate(top = baseOffset.toPx()) {
        drawPath(path, fill?.copy(alpha = 0.42f) ?: colors.base)
    }
    translate(top = faceOffset.toPx()) {
        drawPath(path, fill ?: colors.face)
        if (fill == null) drawPath(path, colors.edge, style = Stroke(1.dp.toPx()))
    }
}

/** Static surface: no base plane, by design. */
internal fun Modifier.flatSurface(colors: LayerColors, cornerRadius: Dp): Modifier = drawBehind {
    val path = Squircle.path(size.width, size.height, cornerRadius.toPx())
    drawPath(path, colors.cardFill)
    drawPath(path, colors.cardEdge, style = Stroke(1.dp.toPx()))
}

private val LabelStyle = TextStyle(fontSize = 13.5.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.2.sp)

@Composable
internal fun CButton(
    text: String,
    onClick: () -> Unit,
    colors: LayerColors,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // The press: the face travels down and bottoms out on its base, like a key.
    val face by animateDpAsState(
        targetValue = if (pressed && enabled) 3.dp else 0.dp,
        animationSpec = tween(120)
    )
    val alpha = if (enabled) 1f else 0.38f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .layerSurface(
                colors = colors,
                cornerRadius = 12.dp,
                baseOffset = if (enabled) 3.dp else 1.dp,
                faceOffset = face,
                fill = if (primary) colors.accent.copy(alpha = alpha) else null
            )
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
            style = LabelStyle.copy(
                color = if (primary) colors.onAccent.copy(alpha = alpha)
                else colors.ink.copy(alpha = 0.82f * alpha)
            ),
            modifier = Modifier.padding(top = face)
        )
    }
}

@Composable
internal fun CField(label: String, value: String, colors: LayerColors) {
    Column {
        BasicText(
            text = label.uppercase(),
            style = TextStyle(fontSize = 11.sp, letterSpacing = 1.1.sp, color = colors.inkFaint),
            modifier = Modifier.padding(start = 2.dp, bottom = 7.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .layerSurface(colors, cornerRadius = 12.dp, baseOffset = 2.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicText(
                text = value,
                style = TextStyle(fontSize = 15.sp, color = colors.ink),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

@Composable
internal fun CCard(colors: LayerColors, content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().flatSurface(colors, cornerRadius = 18.dp)) {
        Column { content() }
    }
}

@Composable
internal fun CRow(
    label: String,
    colors: LayerColors,
    value: String? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicText(
            text = label,
            style = TextStyle(fontSize = 14.5.sp, color = colors.ink),
            modifier = Modifier.weight(1f)
        )
        if (value != null) {
            BasicText(text = value, style = TextStyle(fontSize = 13.5.sp, color = colors.inkDim))
        }
        trailing?.invoke()
    }
}

@Composable
internal fun CDivider(colors: LayerColors) {
    Box(
        Modifier.fillMaxWidth().padding(start = 16.dp).height(1.dp)
            .drawBehind { drawRect(colors.ink.copy(alpha = 0.10f)) }
    )
}

@Composable
internal fun CSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, colors: LayerColors) {
    val interaction = remember { MutableInteractionSource() }
    val knob by animateDpAsState(if (checked) 25.dp else 3.dp, tween(160))
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 30.dp)
            .clickable(interactionSource = interaction, indication = null) {
                onCheckedChange(!checked)
            }
            .drawBehind {
                val track = Squircle.path(size.width, size.height, size.height / 2f)
                drawPath(track, if (checked) colors.accent else colors.face)
                if (!checked) drawPath(track, colors.edge, style = Stroke(1.dp.toPx()))

                val k = 24.dp.toPx()
                val knobPath = Squircle.path(k, k, k / 2f)
                translate(left = knob.toPx(), top = 3.dp.toPx() + 2.dp.toPx()) {
                    drawPath(knobPath, colors.ink.copy(alpha = if (checked) 0.18f else 0.26f))
                }
                translate(left = knob.toPx(), top = 3.dp.toPx()) {
                    drawPath(knobPath, if (checked) Color.White else colors.face)
                    if (!checked) drawPath(knobPath, colors.edge, style = Stroke(1.dp.toPx()))
                }
            }
    )
}
