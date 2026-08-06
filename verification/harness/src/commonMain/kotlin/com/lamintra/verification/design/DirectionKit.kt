package com.lamintra.verification.design

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shared vocabulary for the harness's own screens, plus the two components that
 * are NOT yet wave 1 - the segmented control and the slider.
 *
 * **The design language, for reference.** Selected 2026-08-06 after the layer
 * language was rejected on sight for reading cheap and Material-like:
 *
 * 1. **Two values do the work: ink and surface.** A primary action is solid ink
 *    with surface-coloured text. No translucent faces, no gradients, no
 *    elevation planes. It is a contrast decision rather than a hue decision, so
 *    it survives grayscale and never steals the host app's palette.
 * 2. **Ink carries action; accent carries state.** Buttons are ink. Switches,
 *    selection and progress are accent. The two never compete.
 * 3. **Silhouette carries role.** Actions are capsules, containers are tight
 *    rectangles. With colour removed you can still tell what is pressable.
 * 4. **Space is the luxury signal.**
 * 5. **Every state change is a spring.** Springs carry velocity through an
 *    interruption; duration curves cannot, and that difference is the whole of
 *    what "smooth" means in an iOS or React Native app.
 * 6. **Sentence case, tight tracking.** Uppercase at +1.1sp was the loudest
 *    dated tell in the version this replaces.
 *
 * The five wave-1 components that were prototyped here have been **promoted
 * into the registry and deleted from this file.** Keeping a second copy is the
 * exact drift this project has been bitten by; the registry sources are staged
 * into this harness by the build, so what the screens render is what
 * `lamintra add` ships.
 */

/* ------------------------------- colour -------------------------------- */

internal data class DirectionColors(
    val surface: Color,
    val ink: Color,
    val onInk: Color,
    val inkDim: Color,
    val hairline: Color,
    val group: Color,
    val knob: Color,
    val shadow: Color,
    val accent: Color,
    val danger: Color
) {
    companion object {
        // Neither pure black nor pure white. Pure values vibrate on OLED and
        // read as an unstyled page rather than a designed one.
        fun dark() = DirectionColors(
            surface = Color(0xFF0A0A0B),
            ink = Color(0xFFFAFAFA),
            onInk = Color(0xFF0A0A0B),
            inkDim = Color(0xFF8B8B90),
            hairline = Color(0xFF26262A),
            group = Color(0xFF141416),
            knob = Color(0xFFFFFFFF),
            shadow = Color(0xFF000000),
            accent = Color(0xFF3B82F6),
            danger = Color(0xFFF04438)
        )

        fun light() = DirectionColors(
            surface = Color(0xFFFFFFFF),
            ink = Color(0xFF09090B),
            onInk = Color(0xFFFFFFFF),
            inkDim = Color(0xFF70707B),
            hairline = Color(0xFFE4E4E7),
            group = Color(0xFFF4F4F5),
            knob = Color(0xFFFFFFFF),
            shadow = Color(0xFF09090B),
            accent = Color(0xFF2563EB),
            danger = Color(0xFFD92D20)
        )
    }
}

/* ----------------------------- typography ------------------------------ */

/**
 * Derived from the rule Uber publishes rather than copied values: base 14,
 * ratio 1.125, line height = size x 1.45 rounded to the nearest 4.
 *
 * Weight and size are form and may be set. `fontFamily` never is - the host
 * app's typeface always wins, which is also why this language cannot lean on
 * type the way Uber and Apple do and has to earn its identity from geometry,
 * space and motion instead.
 */
internal object DType {
    val display = TextStyle(fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
    val title = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp)
    val label = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp)
    val caption = TextStyle(fontSize = 13.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.1).sp)
}

/* ------------------------------- motion -------------------------------- */

/** Press: brisk, barely bouncy - Apple's "bounce 0.15" band. */
internal fun <T> pressSpring() = spring<T>(dampingRatio = 0.72f, stiffness = 1400f)

/** Travel: something moving across a distance, with a little overshoot. */
internal fun <T> travelSpring() = spring<T>(dampingRatio = 0.68f, stiffness = 700f)

/** Fade: no bounce, because a bouncing opacity looks like a bug. */
internal fun <T> fadeSpring() = spring<T>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 700f
)

/* -------------------------------- depth -------------------------------- */

/**
 * A soft shadow drawn as layered, widening, fading strokes.
 *
 * The one place this language admits depth at all: a slider handle or switch
 * knob is a small object physically above its track, and without this it reads
 * as a hole punched in the track.
 *
 * Deliberately not `Modifier.shadow` - its coloured `ambientColor`/`spotColor`
 * are Android-only and draw nothing comparable on iOS, desktop or wasm. The
 * registry's `switch` ships its own copy of this for the same reason every
 * component is standalone after install.
 */
internal fun DrawScope.softShadow(
    path: Path,
    color: Color,
    spread: Dp,
    dropDown: Dp,
    maxAlpha: Float,
    steps: Int = 7
) {
    val spreadPx = spread.toPx()
    translate(top = dropDown.toPx()) {
        for (i in steps downTo 1) {
            val frac = i / steps.toFloat()
            drawPath(
                path,
                color.copy(alpha = maxAlpha * (1f - frac) * (1f - frac)),
                style = Stroke(spreadPx * frac * 2f)
            )
        }
    }
}

/* --------------------------- segmented control -------------------------- */

/**
 * NOT wave 1 - a candidate for the next wave, kept here where it can be judged
 * before it earns a registry entry.
 *
 * The selected indicator *slides* between segments on a spring rather than
 * cutting, which is the whole reason this pattern reads as expensive on iOS.
 */
@Composable
internal fun DSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    c: DirectionColors,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val count = options.size.coerceAtLeast(1)
    val segmentWidth = with(density) { (widthPx / count).toDp() }
    val indicatorX by animateDpAsState(
        targetValue = segmentWidth * selected,
        animationSpec = travelSpring()
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged {
                widthPx = (it.width - with(density) { 8.dp.toPx() }).coerceAtLeast(0f)
            }
            .drawBehind {
                drawPath(Squircle.path(size.width, size.height, size.height / 2f), c.group)
            }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(start = indicatorX)
                .size(width = segmentWidth, height = 36.dp)
                .drawBehind {
                    drawPath(Squircle.path(size.width, size.height, size.height / 2f), c.ink)
                }
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                val interaction = remember { MutableInteractionSource() }
                val t by animateFloatAsState(
                    targetValue = if (index == selected) 1f else 0f,
                    animationSpec = fadeSpring()
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clickable(interactionSource = interaction, indication = null) {
                            onSelect(index)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        text = option,
                        style = DType.caption.copy(
                            fontSize = 14.sp,
                            color = lerp(c.inkDim, c.onInk, t)
                        )
                    )
                }
            }
        }
    }
}

/* -------------------------------- slider ------------------------------- */

/**
 * NOT wave 1 - the other next-wave candidate.
 *
 * The knob grows while dragged, which is the iOS behaviour and reads as the
 * object being picked up rather than pushed. The old layer-language slider had
 * a handle-alignment bug that survived two rounds; here the grown knob is
 * centred on the value explicitly on both axes.
 */
@Composable
internal fun DSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    c: DirectionColors,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val dragging by interaction.collectIsDraggedAsState()
    val knobSize = 28.dp
    val knobPx = with(LocalDensity.current) { knobSize.toPx() }
    var travelPx by remember { mutableStateOf(1f) }

    val grow by animateFloatAsState(if (dragging) 1.12f else 1f, travelSpring())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .onSizeChanged { travelPx = (it.width - knobPx).coerceAtLeast(1f) }
            .draggable(
                orientation = Orientation.Horizontal,
                interactionSource = interaction,
                state = rememberDraggableState { delta ->
                    onValueChange((value + delta / travelPx).coerceIn(0f, 1f))
                }
            )
            .drawBehind {
                val h = 6.dp.toPx()
                val top = (size.height - h) / 2f
                translate(top = top) {
                    drawPath(Squircle.path(size.width, h, h / 2f), c.hairline)
                    val filled = (knobPx / 2f + travelPx * value).coerceAtLeast(h)
                    drawPath(Squircle.path(filled, h, h / 2f), c.ink)
                }

                val k = knobPx * grow
                val knob = Squircle.path(k, k, k / 2f)
                translate(
                    left = travelPx * value - (k - knobPx) / 2f,
                    top = (size.height - k) / 2f
                ) {
                    softShadow(
                        path = knob,
                        color = c.shadow,
                        spread = 4.dp,
                        dropDown = 1.5.dp,
                        maxAlpha = 0.5f
                    )
                    drawPath(knob, c.knob)
                }
            }
    )
}
