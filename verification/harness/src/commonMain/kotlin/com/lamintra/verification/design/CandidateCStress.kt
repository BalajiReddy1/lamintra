package com.lamintra.verification.design

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The "slider test" — two components the layer language was NOT designed
 * around, built strictly from its existing seven rules to find out whether it
 * is generative or merely consistent.
 *
 * Rules under test:
 *  1. interactive = face + base plane; static = flat + hairline
 *  2. press = face travels down and bottoms out on its base, 120ms
 *  3. disabled/inactive = offset collapses, alpha drops (form, not colour)
 *  4. squircle at fixed radius, 12dp controls / 18dp containers
 *  5. everything derived from surface + ink at alpha
 *  6. light and dark express elevation differently
 *  7. never set fontFamily
 */

/**
 * TABS — derives cleanly.
 *
 * Selection is expressed as elevation: the selected tab is the only one with a
 * base plane, so "selected" literally means "raised". That is rule 1 applied
 * unchanged, it needs no colour to read, and it survives grayscale. The
 * container is static, so it is flat with a hairline — rule 1 again.
 */
@Composable
internal fun CTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    colors: LayerColors
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .flatSurface(colors, cornerRadius = 12.dp)
            .padding(3.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selected
                val interaction = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .then(
                            if (isSelected) {
                                Modifier.layerSurface(colors, cornerRadius = 9.dp, baseOffset = 2.dp)
                            } else {
                                Modifier
                            }
                        )
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = label,
                        style = TextStyle(
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.6.sp,
                            // rule 3: inactive loses weight through alpha, not hue
                            color = colors.ink.copy(alpha = if (isSelected) 0.9f else 0.42f)
                        )
                    )
                }
            }
        }
    }
}

/**
 * SLIDER — the component that stresses the language.
 *
 * Track is static, so flat + hairline. The handle is the interactive part, so
 * it carries the face + base plane.
 *
 * The unresolved bit is rule 2. "Face travels down to meet its base" was
 * written for something you tap. Applied literally to a handle you drag
 * sideways, the handle sinks while moving horizontally. It is applied
 * unchanged here rather than quietly special-cased, precisely so it can be
 * judged rather than hidden.
 */
@Composable
internal fun CSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    colors: LayerColors
) {
    val interaction = remember { MutableInteractionSource() }
    val dragging by interaction.collectIsDraggedAsState()
    val faceDrop by animateDpAsState(if (dragging) 3.dp else 0.dp, tween(120))

    var widthPx by remember { mutableStateOf(1f) }
    val handle = 26.dp
    val trackHeight = 6.dp
    val handlePx = with(LocalDensity.current) { handle.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(handle + 6.dp)
            .onSizeChanged { widthPx = (it.width - handlePx).coerceAtLeast(1f) }
            .draggable(
                orientation = Orientation.Horizontal,
                interactionSource = interaction,
                state = rememberDraggableState { delta ->
                    if (widthPx > 0f) {
                        onValueChange((value + delta / widthPx).coerceIn(0f, 1f))
                    }
                }
            )
            .drawBehind {
                val h = trackHeight.toPx()
                val top = (size.height - h) / 2f

                // track: static -> flat + hairline (rule 1)
                translate(top = top) {
                    val track = Squircle.path(size.width, h, h / 2f)
                    drawPath(track, colors.cardFill)
                    drawPath(track, colors.cardEdge, style = Stroke(1.dp.toPx()))
                }
                // filled portion carries the host accent
                val filled = (handle.toPx() / 2f + widthPx * value).coerceAtLeast(h)
                translate(top = top) {
                    drawPath(Squircle.path(filled, h, h / 2f), colors.accent)
                }

                // handle: interactive -> face + base plane (rule 1)
                val hp = handle.toPx()
                val x = widthPx * value
                val handlePath = Squircle.path(hp, hp, hp / 2f)
                val hy = (size.height - hp) / 2f
                translate(left = x, top = hy + 3.dp.toPx()) {
                    drawPath(handlePath, colors.base)
                }
                translate(left = x, top = hy + faceDrop.toPx()) {
                    // Rule 1 applied unchanged, deliberately not special-cased:
                    // `face` is ink at 10% on dark, tuned for a large button
                    // surface. On a 26dp handle sitting on a track that may be
                    // too weak to read. Left as-is so the gap is visible rather
                    // than papered over — that is the point of this test.
                    drawPath(handlePath, colors.face)
                    drawPath(handlePath, colors.edge, style = Stroke(1.dp.toPx()))
                }
            }
    )
}
