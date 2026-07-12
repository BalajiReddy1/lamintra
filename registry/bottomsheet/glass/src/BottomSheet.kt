package com.jetcompose.bottomsheet.glass

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass.DragHandle
import com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass.glassSurface
import kotlinx.coroutines.launch

/**
 * A glassmorphism-styled bottom sheet built entirely on compose.foundation.
 * No Material 3 dependency — colors, corner radius, and blur are fully
 * theme-driven so it adapts to the host app's own design system.
 *
 * @param visible whether the sheet is currently shown
 * @param onDismiss called when the user drags the sheet down past [dismissThreshold]
 * @param tint the base glass tint color (alpha is applied internally)
 * @param cornerRadius corner radius of the sheet's top edge
 * @param dismissThreshold vertical drag distance (in dp) after which the sheet dismisses
 * @param content the sheet's body content
 */
@Composable
fun GlassBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    cornerRadius: Dp = 24.dp,
    dismissThreshold: Dp = 120.dp,
    content: @Composable () -> Unit
) {
    if (!visible) return

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val offsetY = remember { Animatable(0f) }
    var dragAccumulator by remember { mutableStateOf(0f) }

    LaunchedEffect(visible) {
        offsetY.snapTo(0f)
        dragAccumulator = 0f
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .offset { IntOffset(0, offsetY.value.roundToInt().coerceAtLeast(0)) }
            .zIndex(10f)
            .glassSurface(tint = tint, cornerRadius = cornerRadius)
            .padding(top = 8.dp, start = 16.dp, end = 16.dp, bottom = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = Color.Transparent, shape = RoundedCornerShape(cornerRadius))
                .pointerInputDragHandle(
                    onDrag = { delta ->
                        dragAccumulator += delta
                        if (dragAccumulator > 0f) {
                            scope.launch { offsetY.snapTo(dragAccumulator) }
                        }
                    },
                    onDragEnd = {
                        // dragAccumulator is in px; the threshold is dp — convert
                        // before comparing or dismissal fires too easily on
                        // high-density screens.
                        val thresholdPx = with(density) { dismissThreshold.toPx() }
                        if (dragAccumulator > thresholdPx) {
                            onDismiss()
                        } else {
                            scope.launch {
                                offsetY.animateTo(0f, animationSpec = tween(durationMillis = 220))
                            }
                        }
                        dragAccumulator = 0f
                    }
                )
        ) {
            DragHandle(modifier = Modifier.padding(vertical = 12.dp))
            content()
        }
    }
}

/**
 * Thin wrapper kept private to this file so BottomSheet.kt stays readable —
 * delegates the actual gesture detection to compose.foundation's
 * detectVerticalDragGestures.
 */
private fun Modifier.pointerInputDragHandle(
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
): Modifier = this.pointerInput(Unit) {
    detectVerticalDragGestures(
        onDragEnd = { onDragEnd() },
        // A cancelled gesture (e.g. the pointer is stolen by a scroll
        // parent) must settle the sheet the same way a release does.
        onDragCancel = { onDragEnd() }
    ) { _, dragAmount ->
        onDrag(dragAmount)
    }
}
