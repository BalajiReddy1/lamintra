// Minimal Compose Multiplatform project, existing only so CI can prove the
// ios-shell scaffold produces Swift and Kotlin that actually compile together.
// Versions track verification/demo/build.gradle.kts; if that moves, move this.
pluginManagement {
    repositories { google(); gradlePluginPortal(); mavenCentral() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
}
rootProject.name = "ios-shell-fixture"
include(":composeApp")
