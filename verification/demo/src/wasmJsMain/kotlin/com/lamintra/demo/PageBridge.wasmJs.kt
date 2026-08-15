package com.lamintra.demo

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
