package com.lamintra.bottomsheet.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.lamintra.bottomsheet.glass.internal.bottomsheet_glass.DragHandle
import com.lamintra.bottomsheet.glass.internal.bottomsheet_glass.glassSurface

/**
 * An "Obsidian Glass" bottom sheet built entirely on compose.foundation —
 * no Material dependency. A full-screen overlay: dimmed scrim (tap to
 * dismiss) behind a floating glass card that slides up from the bottom
 * edge, with drag-to-dismiss. Visual values follow design/TOKENS.md.
 *
 * Dismissal fires on EITHER a downward drag past [dismissThreshold] OR a
 * downward fling faster than [flingVelocityThreshold]; sub-threshold
 * releases settle back inheriting the release velocity. For multi-anchor
 * sheets (peek / half / full) the proper long-term API is foundation's
 * `anchoredDraggable`; this single-anchor dismiss doesn't need it.
 *
 * @param visible whether the sheet is currently shown
 * @param onDismiss called when the scrim is tapped or the sheet is
 *        dragged/flung down enough to dismiss
 * @param tint the glass tint color (alpha gradient applied internally)
 * @param cornerRadius corner radius of the floating card
 * @param dismissThreshold downward drag distance after which release dismisses
 * @param flingVelocityThreshold downward release speed (dp/second) that
 *        dismisses regardless of distance
 * @param scrimColor overlay color behind the sheet
 * @param hairlineColor colour of the light-catching edge around the card.
 *        The component applies the alpha ramp. Defaults to white, which is
 *        correct over a dark background; pass a dark colour for a light
 *        host scheme.
 * @param handleColor colour of the drag handle, including its alpha.
 * @param contentWindowInsets insets the sheet card keeps clear of. Defaults
 *        to the bottom and horizontal edges of [WindowInsets.safeDrawing],
 *        which covers the gesture/navigation bar, display cutouts in
 *        landscape, and the on-screen keyboard — required on Android 15+,
 *        where edge-to-edge is enforced and cannot be opted out of. Resolves
 *        to zero on older/non-edge-to-edge setups, so the default is safe
 *        everywhere. Pass `WindowInsets(0)` if a parent already consumed
 *        these insets, to avoid double padding.
 * @param content the sheet's body content
 */
@Composable
fun GlassBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFF9BB8FF),
    cornerRadius: Dp = 24.dp,
    dismissThreshold: Dp = 64.dp,
    flingVelocityThreshold: Dp = 125.dp,
    scrimColor: Color = Color(0x99060A12),
    hairlineColor: Color = Color.White,
    handleColor: Color = Color.White.copy(alpha = 0.28f),
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing
        .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom),
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    var offsetY by remember { mutableStateOf(0f) }

    val dismissDistancePx = with(density) { dismissThreshold.toPx() }
    val flingVelocityPx = with(density) { flingVelocityThreshold.toPx() }

    LaunchedEffect(visible) {
        offsetY = 0f
    }

    val dragState = rememberDraggableState { delta ->
        // Downward only: the sheet rests at 0 and can't be dragged up past it.
        offsetY = (offsetY + delta).coerceAtLeast(0f)
    }

    // Outer layer fades the scrim; the nested layer slides the card. Both
    // watch the same flag, so exit plays the fade and the slide together.
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 220)),
        exit = fadeOut(animationSpec = tween(durationMillis = 220)),
        modifier = modifier.zIndex(10f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    animationSpec = tween(durationMillis = 320, easing = LinearOutSlowInEasing)
                ) { fullHeight -> fullHeight },
                exit = slideOutVertically(
                    animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
                ) { fullHeight -> fullHeight },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Column(
                    modifier = Modifier
                        // System insets first: the scrim above stays full-bleed
                        // (dimming everything, including behind the system bars)
                        // while the card itself is held clear of the navigation
                        // bar, cutouts, and the keyboard.
                        .windowInsetsPadding(contentWindowInsets)
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .offset { IntOffset(0, offsetY.roundToInt()) }
                        .glassSurface(
                            tint = tint,
                            cornerRadius = cornerRadius,
                            hairlineColor = hairlineColor
                        )
                        .draggable(
                            state = dragState,
                            orientation = Orientation.Vertical,
                            onDragStopped = { velocity ->
                                // Dismiss on distance OR downward fling; otherwise
                                // settle back. velocity is px/second, positive down.
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
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp)
                ) {
                    DragHandle(
                        color = handleColor,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            // 12/12 rather than 10/14: same 28dp total block
                            // height as before, but both values on the 4pt grid.
                            .padding(top = 12.dp, bottom = 12.dp)
                    )
                    content()
                }
            }
        }
    }
}
