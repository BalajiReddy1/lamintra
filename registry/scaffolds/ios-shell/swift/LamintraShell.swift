import SwiftUI

// The native chrome.
//
// SwiftUI owns the tab bar and the navigation stacks. Compose renders only
// what sits inside them. That division is not a preference: on iOS 26 Liquid
// Glass is drawn by the system through TabView, NavigationStack and the
// toolbar APIs, and Compose renders to its own Skia canvas, so Compose cannot
// draw it at all. Adopting Liquid Glass means using the native containers -
// which is why there is no Liquid Glass code in this file and should not be.
//
// Deliberately NOT marked @main. The Compose Multiplatform template already
// ships an @main App and a second one is a compile error, so this is a plain
// View that your existing App points at.

/// The tabs, and the only place Swift and Kotlin agree on a screen name.
/// `rawValue` is what crosses the bridge; `ShellRoute` in ShellRoutes.kt is
/// the other half and the two must stay in step. Kotlin fails loudly on an
/// unknown name rather than rendering a blank screen.
enum ShellRoute: String {
    case home
    case settings
}

struct LamintraShell: View {
    var body: some View {
        TabView {
            Tab("Home", systemImage: "house") {
                NavigationStack {
                    ComposeScreen(route: .home)
                        .navigationTitle("Home")
                }
            }
            Tab("Settings", systemImage: "gearshape") {
                NavigationStack {
                    ComposeScreen(route: .settings)
                        .navigationTitle("Settings")
                }
            }
        }
    }
}
