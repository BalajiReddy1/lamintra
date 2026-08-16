import SwiftUI
import UIKit
import {{FRAMEWORK}}

// The bridge, and the whole of it.
//
// Each tab gets its own ComposeUIViewController rather than one shared host
// swapping content. That is what keeps SwiftUI in charge of the back stack:
// a single host would have to be told which screen to show, and the moment
// Compose owns that decision the navigation bar stops being native and the
// system stops applying Liquid Glass to it.

struct ComposeScreen: UIViewControllerRepresentable {
    let route: ShellRoute

    func makeUIViewController(context: Context) -> UIViewController {
        // `shellScreenController` is a top-level Kotlin function in
        // ShellEntry.kt. Kotlin/Native exports those on a class named after
        // the file, with the first letter of the function lowercased.
        ShellEntryKt.shellScreenController(route: route.rawValue)
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {
        // Nothing to push down: the route is fixed for the life of the
        // controller, and Compose owns its own state inside it.
    }
}
