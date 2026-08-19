package com.lamintra.demo
import kotlin.js.Promise
import kotlinx.coroutines.await

internal actual fun markDemoReady() {
    setDemoState()
}

internal actual fun pageIsDark(): Boolean? = when (readTheme()) {
    "dark" -> true
    "light" -> false
    else -> null
}

// Kotlin/Wasm requires a js(...) call to be the entire body of its function,
// so each of these is a thin wrapper rather than an inline call above.
private fun setDemoState() {
    js("document.documentElement.setAttribute('data-live', 'ready')")
}

private fun readTheme(): String {
    js("return document.documentElement.getAttribute('data-theme') || ''")
}

internal actual suspend fun awaitPageSchemeChange() {
    schemeChangePromise().await<JsAny?>()
}

/**
 * A promise that resolves the next time `data-theme` changes, then stops
 * observing.
 *
 * One observer per wait rather than one for the lifetime of the page, because
 * the caller is a loop: it disconnects on the first mutation and the next
 * iteration installs the next one. That keeps the state in the coroutine rather
 * than in a JS variable the Kotlin side cannot see, and there is no listener
 * left behind if the composition goes away.
 *
 * attributeFilter matters. Without it this resolves on every attribute the page
 * writes to <html>, and the site writes several - data-live when the canvas is
 * ready, and the scheme script's own bookkeeping.
 */
private fun schemeChangePromise(): Promise<JsAny?> =
    js("""
        new Promise(function (resolve) {
            var observer = new MutationObserver(function () {
                observer.disconnect();
                resolve(null);
            });
            observer.observe(document.documentElement, {
                attributes: true,
                attributeFilter: ['data-theme']
            });
        })
    """)
