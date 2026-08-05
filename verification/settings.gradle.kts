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

rootProject.name = "jetcompose-verification"
include(":harness")
