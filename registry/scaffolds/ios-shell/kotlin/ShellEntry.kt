package {{PACKAGE}}.ui.shell

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/**
 * The single crossing point between SwiftUI and Compose.
 *
 * Lives in iosMain because `ComposeUIViewController` and `UIViewController`
 * only exist there. Everything it renders lives in commonMain, so the shared
 * half of the app stays shared: this file is a doorway, not a place to put
 * logic.
 *
 * Swift calls it as `ShellEntryKt.shellScreenController(route:)` - Kotlin/Native
 * exports top-level functions on a class named after the file, lowercasing the
 * first letter. Renaming this file renames that Swift symbol, which is a
 * compile error on the Swift side and nowhere else, so don't.
 */
fun shellScreenController(route: String): UIViewController =
    ComposeUIViewController {
        ShellScreen(ShellRoute.from(route))
    }
