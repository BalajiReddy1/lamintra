package com.lamintra.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.withTimeoutOrNull

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
 * Suspends until the page's `data-theme` attribute changes.
 *
 * The contract with the site is unchanged and is still exactly that attribute.
 * What changed is who speaks first: the page now tells this canvas, instead of
 * the canvas asking on a timer.
 */
internal expect suspend fun awaitPageSchemeChange()

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
            // Event-driven, not polled. This was delay(200) until 2026-08-17
            // and delay(16) until 2026-08-19; both were guesses at a number
            // small enough not to be seen, and the founder still saw the render
            // arrive late at 16. A guess at an interval cannot be right - the
            // page knows exactly when the attribute changed, so it should say
            // so rather than be asked sixty times a second.
            //
            // The timeout is a failsafe, not a poll. If the observer never
            // resolves - a browser without MutationObserver, an attribute
            // changed in a way that does not fire it - this falls back to
            // re-reading every two seconds rather than hanging forever with a
            // canvas stuck in the wrong scheme. Slow is a degradation; never is
            // a bug, and this repo has shipped the second one before by
            // gating on a mechanism with no floor under it.
            withTimeoutOrNull(2_000) { awaitPageSchemeChange() }
            val now = pageIsDark()
            if (now != null && now != last) {
                last = now
                callback(now)
            }
        }
    }
}
