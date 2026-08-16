package com.lamintra.cli

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The end-to-end cases read the real registry through LAMINTRA_REGISTRY,
 * which build.gradle.kts points at ../registry for the test task. That is
 * deliberate: it means these tests exercise the actual scaffold files that
 * ship, so a broken scaffold.json fails here rather than on a user's machine.
 */
class ScaffoldTest {

    // ---------------------------------------------------------------- manifest

    @Test
    fun `parses a well-formed manifest`() {
        val manifest = ScaffoldManifest.parse(
            """
            {
              "name": "ios-shell",
              "title": "Native chrome",
              "requiresKmp": true,
              "files": [
                { "from": "swift/A.swift", "to": "swift:A.swift" },
                { "from": "kotlin/B.kt", "to": "ios:ui/shell/B.kt" }
              ],
              "postInstall": ["do a thing"]
            }
            """.trimIndent()
        )
        assertEquals("ios-shell", manifest.name)
        assertTrue(manifest.requiresKmp)
        assertEquals(2, manifest.files.size)
        assertEquals("swift", manifest.files[0].rootKeyword)
        assertEquals("A.swift", manifest.files[0].destPath)
        assertEquals("ui/shell/B.kt", manifest.files[1].destPath)
        assertEquals(listOf("do a thing"), manifest.postInstall)
    }

    @Test
    fun `rejects an unknown destination root`() {
        val error = assertFailsWith<IllegalArgumentException> {
            ScaffoldManifest.parse(
                """
                {
                  "name": "bad",
                  "files": [ { "from": "x.kt", "to": "everywhere:x.kt" } ]
                }
                """.trimIndent()
            )
        }
        assertContains(error.message!!, "everywhere")
    }

    @Test
    fun `rejects a manifest with no files`() {
        assertFailsWith<IllegalArgumentException> {
            ScaffoldManifest.parse("""{ "name": "bad", "files": [] }""")
        }
    }

    // ------------------------------------------------------------ substitution

    @Test
    fun `substitutes both tokens`() {
        val out = ScaffoldCommand.substitute(
            "package {{PACKAGE}}.ui\nimport {{FRAMEWORK}}\n// {{PACKAGE}} again",
            packageName = "com.testco.demo",
            framework = "SharedKit"
        )
        assertEquals(
            "package com.testco.demo.ui\nimport SharedKit\n// com.testco.demo again",
            out
        )
    }

    // --------------------------------------------------------------- detection

    @Test
    fun `finds the Xcode group directory beside the project file`(@TempDir dir: File) {
        File(dir, "iosApp/iosApp.xcodeproj").mkdirs()
        File(dir, "iosApp/iosApp").mkdirs()
        val found = ScaffoldCommand.findSwiftSourceDir(dir)
        assertEquals(File(dir, "iosApp/iosApp").canonicalFile, found!!.canonicalFile)
    }

    @Test
    fun `falls back to the directory holding the Xcode project`(@TempDir dir: File) {
        File(dir, "ios/MyApp.xcodeproj").mkdirs()
        val found = ScaffoldCommand.findSwiftSourceDir(dir)
        assertEquals(File(dir, "ios").canonicalFile, found!!.canonicalFile)
    }

    @Test
    fun `returns null when there is no Xcode project`(@TempDir dir: File) {
        File(dir, "composeApp/src").mkdirs()
        assertEquals(null, ScaffoldCommand.findSwiftSourceDir(dir))
    }

    @Test
    fun `reads the framework baseName from the module build file`(@TempDir dir: File) {
        File(dir, "composeApp").mkdirs()
        File(dir, "composeApp/build.gradle.kts").writeText(
            """
            kotlin {
                listOf(iosArm64(), iosSimulatorArm64()).forEach { t ->
                    t.binaries.framework {
                        baseName = "SharedKit"
                        isStatic = true
                    }
                }
            }
            """.trimIndent()
        )
        assertEquals(
            "SharedKit",
            ScaffoldCommand.detectFrameworkName(dir, "composeApp/src/iosMain/kotlin")
        )
    }

    @Test
    fun `ignores a baseName that is commented out`(@TempDir dir: File) {
        File(dir, "composeApp").mkdirs()
        File(dir, "composeApp/build.gradle.kts").writeText(
            "// baseName = \"Wrong\"\nkotlin { }"
        )
        assertEquals(
            "ComposeApp",
            ScaffoldCommand.detectFrameworkName(dir, "composeApp/src/iosMain/kotlin")
        )
    }

    // -------------------------------------------------------------- end to end

    private fun kmpProject(dir: File, framework: String = "ComposeApp") {
        File(dir, ".lamintra").mkdirs()
        File(dir, ".lamintra/config.json").writeText(
            """
            {
              "packageName": "com.testco.demo",
              "isKmp": true,
              "sourceRoots": {
                "common": "composeApp/src/commonMain/kotlin",
                "android": "composeApp/src/androidMain/kotlin",
                "ios": "composeApp/src/iosMain/kotlin"
              },
              "componentPath": "ui/components"
            }
            """.trimIndent()
        )
        File(dir, "composeApp").mkdirs()
        File(dir, "composeApp/build.gradle.kts").writeText(
            "kotlin { binaries.framework { baseName = \"$framework\" } }"
        )
        File(dir, "iosApp/iosApp.xcodeproj").mkdirs()
        File(dir, "iosApp/iosApp").mkdirs()
    }

    @Test
    fun `installs the ios-shell scaffold to the right four paths`(@TempDir dir: File) {
        kmpProject(dir)
        ScaffoldCommand.run("ios-shell", dir, force = false)

        val swiftShell = File(dir, "iosApp/iosApp/LamintraShell.swift")
        val swiftBridge = File(dir, "iosApp/iosApp/ComposeScreen.swift")
        val entry = File(dir, "composeApp/src/iosMain/kotlin/com/testco/demo/ui/shell/ShellEntry.kt")
        val routes = File(dir, "composeApp/src/commonMain/kotlin/com/testco/demo/ui/shell/ShellRoutes.kt")

        assertTrue(swiftShell.isFile, "LamintraShell.swift not written")
        assertTrue(swiftBridge.isFile, "ComposeScreen.swift not written")
        assertTrue(entry.isFile, "ShellEntry.kt not written to iosMain")
        assertTrue(routes.isFile, "ShellRoutes.kt not written to commonMain")

        // The Kotlin half must declare the user's package, not ours: a
        // leftover {{PACKAGE}} or a com.lamintra prefix is the failure this
        // whole product exists to prevent.
        assertContains(entry.readText(), "package com.testco.demo.ui.shell")
        assertContains(routes.readText(), "package com.testco.demo.ui.shell")
        assertFalse(entry.readText().contains("{{"), "unsubstituted token left in ShellEntry.kt")
        assertFalse(routes.readText().contains("{{"), "unsubstituted token left in ShellRoutes.kt")
        assertFalse(entry.readText().contains("com.lamintra"), "registry package survived")

        // Swift imports the framework module, and there is exactly one @main
        // in the project - ours is deliberately not it.
        assertContains(swiftBridge.readText(), "import ComposeApp")
        assertFalse(swiftBridge.readText().contains("{{"), "unsubstituted token in Swift")
        // The ATTRIBUTE, not the substring: the file's header comment explains
        // at length why it is not @main, and a plain `contains` matches that
        // prose and passes for the wrong reason.
        assertFalse(
            swiftShell.readLines().any { it.trimStart().startsWith("@main") },
            "LamintraShell must not be @main: the CMP template already has one"
        )
    }

    @Test
    fun `picks up a non-default framework name`(@TempDir dir: File) {
        kmpProject(dir, framework = "SharedKit")
        ScaffoldCommand.run("ios-shell", dir, force = false)
        assertContains(
            File(dir, "iosApp/iosApp/ComposeScreen.swift").readText(),
            "import SharedKit"
        )
    }

    @Test
    fun `refuses to overwrite without force, and writes nothing`(@TempDir dir: File) {
        kmpProject(dir)
        val existing = File(dir, "iosApp/iosApp/LamintraShell.swift")
        existing.parentFile.mkdirs()
        existing.writeText("// my own edits")

        val error = assertFailsWith<IllegalStateException> {
            ScaffoldCommand.run("ios-shell", dir, force = false)
        }
        assertContains(error.message!!, "already exist")
        assertEquals("// my own edits", existing.readText(), "the user's file was modified")
        assertFalse(
            File(dir, "iosApp/iosApp/ComposeScreen.swift").exists(),
            "a partial install happened despite the guard"
        )
    }

    @Test
    fun `force overwrites`(@TempDir dir: File) {
        kmpProject(dir)
        val existing = File(dir, "iosApp/iosApp/LamintraShell.swift")
        existing.parentFile.mkdirs()
        existing.writeText("// my own edits")

        ScaffoldCommand.run("ios-shell", dir, force = true)
        assertFalse(existing.readText().contains("my own edits"))
        assertContains(existing.readText(), "struct LamintraShell")
    }

    @Test
    fun `rejects an Android-only project`(@TempDir dir: File) {
        File(dir, ".lamintra").mkdirs()
        File(dir, ".lamintra/config.json").writeText(
            """
            {
              "packageName": "com.testco.demo",
              "isKmp": false,
              "sourceRoots": { "android": "app/src/main/kotlin" },
              "componentPath": "ui/components"
            }
            """.trimIndent()
        )
        val error = assertFailsWith<IllegalStateException> {
            ScaffoldCommand.run("ios-shell", dir, force = false)
        }
        assertContains(error.message!!, "Kotlin Multiplatform")
    }

    @Test
    fun `explains itself when there is no Xcode project`(@TempDir dir: File) {
        kmpProject(dir)
        File(dir, "iosApp/iosApp.xcodeproj").deleteRecursively()
        val error = assertFailsWith<IllegalStateException> {
            ScaffoldCommand.run("ios-shell", dir, force = false)
        }
        assertContains(error.message!!, "xcodeproj")
    }
}
