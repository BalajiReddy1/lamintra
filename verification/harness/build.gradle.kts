import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.1.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("org.jetbrains.compose") version "1.8.1"
}

// The components under test, by registry name. Names are flat slugs, and each
// one's registryPackage is com.lamintra.<slug with hyphens as underscores>, so
// the target directory is a direct function of the name - no package rewriting
// needed here. This harness consumes the registry sources as-is, unlike an
// installed component, which the CLI rewrites into the host app's namespace.
val components = listOf(
    // wave 1, base tier: contrast, space and physics
    "button",
    "card",
    "text-field",
    "list-row",
    "switch",
    // signature tier
    "glass-sheet"
)

// Copies registry sources into a generated source root at the path their own
// package declarations require. Previews are excluded: they use
// androidx.compose.ui.tooling.preview, which is Android-only and would break
// the iOS and desktop compilations.
//
// The hyphen->underscore mapping mirrors ComponentManifest.packageSegment. If
// the two ever disagree the harness compiles sources at a path their package
// declaration does not match, which is a guaranteed Kotlin compile error - so
// the failure is loud rather than silent.
val syncRegistrySources by tasks.registering(Sync::class) {
    description = "Stages registry component sources for the verification harness."
    into(layout.buildDirectory.dir("generated/registry"))
    components.forEach { component ->
        from(rootProject.file("../registry/$component/src")) {
            into("com/lamintra/${component.replace('-', '_')}")
            exclude("**/*Preview.kt")
        }
    }
}

// Surface println from tests so the light/dark gate's reported value is
// visible in CI logs for each target, not just pass/fail.
//
// AbstractTestTask, not Test: Kotlin/Native simulator tests are not `Test`
// tasks, so a withType<Test> filter silently misses iOS - the value ends up
// only in the binary results file and never in the readable log.
tasks.withType<AbstractTestTask>().configureEach {
    testLogging { showStandardStreams = true }
}

kotlin {
    jvmToolchain(17)

    // Desktop exists so the interaction tests can be validated locally on any
    // machine. iOS is the target that actually matters here - it is the one
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
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
        val desktopTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

// Renders every registry component to a PNG, headlessly, from the sources the
// CLI installs. The website needs component images, and the alternatives are
// hand-screenshotting (does not survive a component change) or rebuilding the
// components in HTML (the exact drift this project keeps getting bitten by).
//
//   ./gradlew :harness:renderSpecimens -PoutDir=C:/path/to/site/public/img
val renderSpecimens by tasks.registering(JavaExec::class) {
    group = "verification"
    description = "Renders registry components to PNG for the website."
    dependsOn("desktopMainClasses")
    mainClass.set("com.lamintra.verification.RenderSpecimensKt")
    val desktopMain = kotlin.targets.getByName("desktop").compilations.getByName("main")
    classpath = files(desktopMain.output.allOutputs, desktopMain.runtimeDependencyFiles)
    argumentProviders.add {
        listOf(project.findProperty("outDir")?.toString() ?: "build/specimens")
    }
}

// `./gradlew :harness:run` opens the harness in a native window. The visual
// check is a hard gate on every release and this is the fastest way to reach
// it - ~30s here against ~2.5min for a wasm build.
compose.desktop {
    application {
        mainClass = "com.lamintra.verification.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "lamintra-harness"
            packageVersion = "1.0.0"
        }
    }
}
