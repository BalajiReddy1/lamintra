plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.1"
}

kotlin {
    jvmToolchain(17)

    // Simulator only. The CI check is "does this compile against the real iOS
    // SDK", and arm64 device linking would double the build for no extra
    // signal.
    listOf(iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            // Deliberately NOT "ComposeApp". detectFrameworkName falls back to
            // that name, so a fixture using it would pass even if the
            // build-file parsing were broken.
            baseName = "NotesKit"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
    }
}
