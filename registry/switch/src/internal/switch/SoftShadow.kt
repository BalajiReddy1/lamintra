package com.lamintra.switch.internal.switch

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

/**
 * A soft shadow, drawn as layered widening, fading strokes.
 *
 * **Why not `Modifier.shadow`:** its coloured `ambientColor` / `spotColor` are
 * Android-only. On iOS, desktop and wasm they draw nothing comparable, which
 * would make the knob look like a hole punched in the track everywhere except
 * Android - in a product whose entire pitch is one codebase for both. This
 * project already had to rebuild one component's glow for exactly that reason.
 *
 * This is the one place this design language admits depth at all. A switch knob
 * is a small object physically sitting above its track, and without the shadow
 * it does not read as an object. Everything else here is flat on purpose.
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
            // The widest ring is the faintest, so the falloff reads as a blur
            // rather than as a stack of visible outlines.
            drawPath(
                path,
                color.copy(alpha = maxAlpha * (1f - frac) * (1f - frac)),
                style = Stroke(spreadPx * frac * 2f)
            )
        }
    }
}
