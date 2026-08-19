package com.lamintra.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay

/**
 * What Compose needs from the page it is embedded in.
 *
 * The direction of control matters here. This canvas is a passenger inside the
 * marketing site: the site owns the scheme control, the header, the copy and
 * the fallback image, and Compose only reads. An earlier version had Compose
 * driving the page's scheme, which was correct when the demo was a standalone
 * page and became backwards the moment it moved into the site.
 */

/**
 * Tells the page Compose has composed its first frame, so the static render can
 * be crossfaded out. Called from composition rather than from `main`, because
 * `main` returns long before anything is on screen and swapping there would
 * flash an empty canvas over the hero.
 */
internal expect fun markDemoReady()

/** The scheme the page has resolved, or null if it has not said. */
internal expect fun pageIsDark(): Boolean?

/**
 * Follows the site's scheme control.
 *
 * Polled rather than pushed. A push would need the page to hold a reference
 * back into the wasm module, which means the site's script has to know the
 * demo's export names and breaks whenever they change. Reading an attribute
 * five times a second costs nothing and keeps the site's markup as the only
 * contract between the two.
 */
@Composable
internal fun ObservePageScheme(onChange: (Boolean) -> Unit) {
    val callback by rememberUpdatedState(onChange)
    LaunchedEffect(Unit) {
        var last = pageIsDark()
        while (true) {
            // 16ms, one frame, not 200. At 200 the average lag between the
            // toggle flipping and this canvas following was 100ms and the worst
            // case was 200, which the founder saw immediately on 2026-08-17 and
            // described as the render arriving late. It did. One getAttribute
            // per frame is genuinely free next to compositing this canvas at
            // 60fps, and the markup stays the only contract between the two.
            //
            // A MutationObserver would be zero-poll and is the proper fix; it
            // needs a JS-to-Kotlin callback across the wasm boundary, which is
            // more machinery than this earns today.
            delay(16)
            val now = pageIsDark()
            if (now != null && now != last) {
                last = now
                callback(now)
            }
        }
    }
}
