import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.1"
}

// The components under test, by registry name. Each one's registryPackage is
// com.jetcompose.<category>.<style>, so the target directory is a direct
// function of the name — no package rewriting needed here. This harness
// consumes the registry sources as-is, unlike an installed component, which
// the CLI rewrites into the host app's namespace.
val components = listOf(
    "bottomsheet/glass",
    "button/neon",
    "button/neon_outline"
)

// Copies registry sources into a generated source root at the path their own
// package declarations require. Previews are excluded: they use
// androidx.compose.ui.tooling.preview, which is Android-only and would break
// the iOS and desktop compilations.
val syncRegistrySources by tasks.registering(Sync::class) {
    description = "Stages registry component sources for the verification harness."
    into(layout.buildDirectory.dir("generated/registry"))
    components.forEach { component ->
        from(rootProject.file("../registry/$component/src")) {
            into("com/jetcompose/$component")
            exclude("**/*Preview.kt")
        }
    }
}

// Surface println from tests so the light/dark gate's reported value is
// visible in CI logs for each target, not just pass/fail.
tasks.withType<Test>().configureEach {
    testLogging { showStandardStreams = true }
}

kotlin {
    jvmToolchain(17)

    // Desktop exists so the interaction tests can be validated locally on any
    // machine. iOS is the target that actually matters here — it is the one
    // platform the project could not previously verify by execution.
    jvm("desktop")
    iosSimulatorArm64()

    // Browser target. The website's whole conversion story is "touch the real
    // component", and this is the only way to do that without shipping a
    // re-implementation that would silently drift from the Kotlin. Proving the
    // components compile and run here is a prerequisite for that plan, not a
    // detail of it.
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        moduleName = "harness"
        browser {
            commonWebpackConfig {
                outputFileName = "harness.js"
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
        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}
