package com.jetcompose.bottomsheet.glass

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass.DragHandle
import com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass.glassSurface

/**
 * A glassmorphism-styled bottom sheet built entirely on compose.foundation.
 * No Material 3 dependency — colors, corner radius, and blur are fully
 * theme-driven so it adapts to the host app's own design system.
 *
 * Drag-to-dismiss uses foundation's [draggable], whose `onDragStopped`
 * reports release velocity. The sheet dismisses on EITHER a downward drag
 * past [dismissThreshold] OR a downward fling faster than
 * [flingVelocityThreshold] — matching how real bottom sheets feel, and
 * ensuring a quick flick dismisses even when there isn't room to drag the
 * full threshold distance (a low sheet near the screen edge affords little
 * finger travel). For a multi-anchor sheet (peek / half / full), the proper
 * long-term API is foundation's `anchoredDraggable` + `AnchoredDraggableState`;
 * this single-anchor dismiss doesn't need that machinery yet.
 *
 * @param visible whether the sheet is currently shown
 * @param onDismiss called when the sheet is dragged/flung down enough to dismiss
 * @param tint the base glass tint color (alpha is applied internally)
 * @param cornerRadius corner radius of the sheet's top edge
 * @param dismissThreshold downward drag distance after which release dismisses
 * @param flingVelocityThreshold downward release speed (dp/second) that
 *        dismisses regardless of distance dragged
 * @param content the sheet's body content
 */
@Composable
fun GlassBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    cornerRadius: Dp = 24.dp,
    dismissThreshold: Dp = 64.dp,
    flingVelocityThreshold: Dp = 125.dp,
    content: @Composable () -> Unit
) {
    if (!visible) return

    val density = LocalDensity.current
    var offsetY by remember { mutableStateOf(0f) }

    // Thresholds are density-independent (dp); convert once to the px space
    // that drag deltas and release velocity are reported in.
    val dismissDistancePx = with(density) { dismissThreshold.toPx() }
    val flingVelocityPx = with(density) { flingVelocityThreshold.toPx() }

    LaunchedEffect(visible) {
        offsetY = 0f
    }

    val dragState = rememberDraggableState { delta ->
        // Downward only: the sheet rests at 0 and can't be dragged up past it.
        offsetY = (offsetY + delta).coerceAtLeast(0f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .zIndex(10f)
            .glassSurface(tint = tint, cornerRadius = cornerRadius)
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Transparent, shape = RoundedCornerShape(cornerRadius))
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    onDragStopped = { velocity ->
                        // Dismiss on distance OR downward fling; otherwise settle back.
                        // velocity is px/second, positive downward.
                        if (offsetY > dismissDistancePx || velocity > flingVelocityPx) {
                            onDismiss()
                        } else {
                            animate(
                                initialValue = offsetY,
                                targetValue = 0f,
                                initialVelocity = velocity,
                                animationSpec = tween(durationMillis = 220)
                            ) { value, _ -> offsetY = value }
                        }
                    }
                )
        ) {
            DragHandle(modifier = Modifier.padding(vertical = 12.dp))
            content()
        }
    }
}
