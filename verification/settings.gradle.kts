// Standalone Gradle build, deliberately separate from cli-kotlin/. The CLI is
// a zero-dependency JVM tool; this harness pulls in the whole Compose
// Multiplatform toolchain. Keeping them apart means a Compose or AGP version
// bump here can never affect the CLI's build.
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "lamintra-verification"
include(":harness")

// The public demo. A separate module rather than a second entry point in
// :harness, because the two have opposite audiences: :harness is a dev tool
// carrying test dependencies, a composed settings screen, and a Day-1 screen
// still written in the rejected design language. None of that can reach a
// stranger. Splitting them also keeps the shipped wasm bundle down to the
// gallery, which is the only surface a visitor should ever see.
include(":demo")
