import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.1"
}

// The components the public demo shows. Same list shape as :harness, but this
// one is a product decision rather than a test matrix: glass-sheet is absent
// because it is not on the website either, and anything unfinished stays off
// until it has been looked at.
val components = listOf(
    // The shared token layer. Must come first: every component below reads it.
    // Absent from this list until 2026-08-17, which is why the demo stopped
    // compiling the moment anything referenced it - the wasm hero on the live
    // site was built from a pre-theme snapshot and had been stale since the
    // shared theme shipped on 2026-08-16. :harness has always had it.
    "theme",
    "button",
    "card",
    "text-field",
    "list-row",
    "switch",
    "segmented"
)

// The identical staging mechanism :harness uses. The demo must render the real
// registry sources rather than a copy, or the thing a visitor presses drifts
// from the thing the CLI installs - which is the failure this whole project is
// organised to prevent.
val syncRegistrySources by tasks.registering(Sync::class) {
    description = "Stages registry component sources for the public demo."
    into(layout.buildDirectory.dir("generated/registry"))
    components.forEach { component ->
        from(rootProject.file("../registry/$component/src")) {
            into("com/lamintra/${component.replace('-', '_')}")
            exclude("**/*Preview.kt")
        }
    }
}

kotlin {
    jvmToolchain(17)

    // Browser only. This module exists to be deployed, and every other target
    // would be weight nobody downloads.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "demo"
        browser {
            commonWebpackConfig {
                outputFileName = "demo.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            kotlin.srcDir(syncRegistrySources)
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)
            }
        }
    }
}
