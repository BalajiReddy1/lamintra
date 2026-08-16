package {{PACKAGE}}.ui.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * The screens the shell can show, and the Kotlin half of the contract with
 * `ShellRoute` in LamintraShell.swift. Adding a tab means editing both.
 *
 * Two enums rather than one generated from the other, because the alternative
 * is a build-time codegen step across a language boundary for a list that is
 * usually four items long. The cost is kept visible instead: [from] fails
 * loudly on a name Swift knows and Kotlin does not, so drift shows up as a
 * crash on that tab with the name in the message, rather than a blank screen.
 */
enum class ShellRoute {
    Home,
    Settings;

    companion object {
        fun from(raw: String): ShellRoute =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: error(
                    "Unknown shell route \"$raw\". Swift's ShellRoute and Kotlin's " +
                        "ShellRoute have drifted - every case in the Swift enum needs a " +
                        "matching entry here."
                )
    }
}

/**
 * Renders one screen. SwiftUI decides which one and draws everything around
 * it, so there is no navigation here on purpose: adding a Compose nav host
 * inside a tab would put the back stack back on the Compose side and take the
 * native navigation bar - and with it Liquid Glass - away again.
 */
@Composable
fun ShellScreen(route: ShellRoute) {
    when (route) {
        ShellRoute.Home -> HomeScreen()
        ShellRoute.Settings -> SettingsScreen()
    }
}

// Placeholders, so the scaffold builds and runs the moment it is installed.
// Replace the bodies; keep the signatures wired into ShellScreen above.
//
// BasicText rather than Material's Text on purpose. Every component in this
// registry is written on compose.foundation alone, and a scaffold that pulled
// material3 into a project would add a dependency the user did not ask for -
// to a product whose entire claim is that it is not Material. It would also
// fail to compile in any project that had not already added it.

@Composable
private fun HomeScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText("Home")
    }
}

@Composable
private fun SettingsScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        BasicText("Settings")
    }
}
