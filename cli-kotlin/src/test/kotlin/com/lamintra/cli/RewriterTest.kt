package com.lamintra.cli

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RewriterTest {

    private val testConfig = LamintraConfig(
        packageName = "com.testapp.myapp",
        isKmp = true,
        sourceRoots = SourceRoots(
            common = "composeApp/src/commonMain/kotlin",
            android = "composeApp/src/androidMain/kotlin",
            ios = "composeApp/src/iosMain/kotlin"
        ),
        componentPath = "features/shared/widgets"
    )

    private val glassSheetManifest = ComponentManifest(
        name = "glass-sheet",
        categories = listOf("overlay"),
        registryPackage = "com.lamintra.glass_sheet",
        main = "src/BottomSheet.kt",
        prefix = "glass_sheet",
        files = listOf(
            "src/BottomSheet.kt",
            "src/internal/glass_sheet/DragHandle.kt",
            "src/internal/glass_sheet/ModifierExtensions.kt"
        )
    )

    private val buttonManifest = ComponentManifest(
        name = "button",
        categories = listOf("button"),
        registryPackage = "com.lamintra.button",
        main = "src/LayerButton.kt",
        prefix = "button",
        files = listOf(
            "src/LayerButton.kt",
            "src/internal/button/Squircle.kt"
        )
    )

    @Test
    fun `package declaration is rewritten to the target namespace`() {
        val original = "package com.lamintra.glass_sheet\n\nimport androidx.compose.runtime.Composable\n"
        val rewritten = Rewriter.rewriteFileContent(original, testConfig, glassSheetManifest)
        assertTrue(
            rewritten.startsWith("package com.testapp.myapp.features.shared.widgets.glass_sheet"),
            "got: ${rewritten.lines().first()}"
        )
    }

    @Test
    fun `cross-file internal import is rewritten to the new namespace`() {
        val original = "package com.lamintra.glass_sheet\n" +
            "import com.lamintra.glass_sheet.internal.glass_sheet.DragHandle\n"
        val rewritten = Rewriter.rewriteFileContent(original, testConfig, glassSheetManifest)
        assertTrue(
            rewritten.contains(
                "import com.testapp.myapp.features.shared.widgets.glass_sheet.internal.glass_sheet.DragHandle"
            )
        )
    }

    @Test
    fun `unrelated androidx imports are left untouched`() {
        val original = "package com.lamintra.glass_sheet\n" +
            "import androidx.compose.foundation.background\n"
        val rewritten = Rewriter.rewriteFileContent(original, testConfig, glassSheetManifest)
        assertTrue(rewritten.contains("import androidx.compose.foundation.background"))
    }

    @Test
    fun `boundary-safe rewrite does not corrupt a decoy package with a longer name`() {
        val original = "package com.lamintra.glass_sheet\n" +
            "import com.lamintra.glass_sheety.SomeUnrelatedThing\n" +
            "import com.lamintra.glass_sheet2.AnotherUnrelatedThing\n"
        val rewritten = Rewriter.rewriteFileContent(original, testConfig, glassSheetManifest)
        assertTrue(rewritten.contains("import com.lamintra.glass_sheety.SomeUnrelatedThing"))
        assertTrue(rewritten.contains("import com.lamintra.glass_sheet2.AnotherUnrelatedThing"))
    }

    @Test
    fun `two components sharing a filename resolve to different paths and packages`() {
        val sheetTarget = Rewriter.resolveTargetPath(
            testConfig, glassSheetManifest, "src/internal/glass_sheet/ModifierExtensions.kt"
        )
        val cardManifest = buttonManifest.copy(
            name = "card",
            registryPackage = "com.lamintra.card",
            prefix = "card",
            files = listOf("src/LayerCard.kt", "src/internal/card/ModifierExtensions.kt")
        )
        val cardTarget = Rewriter.resolveTargetPath(
            testConfig, cardManifest, "src/internal/card/ModifierExtensions.kt"
        )
        assertNotEquals(sheetTarget.relativePath, cardTarget.relativePath)
        assertNotEquals(sheetTarget.fullPackage, cardTarget.fullPackage)
    }

    @Test
    fun `resolved file path exactly matches its own declared package`() {
        val target = Rewriter.resolveTargetPath(testConfig, glassSheetManifest, "src/BottomSheet.kt")
        val expectedDirSuffix = target.fullPackage.replace(".", "/")
        assertTrue(
            target.relativePath.contains(expectedDirSuffix),
            "relativePath '${target.relativePath}' should contain '$expectedDirSuffix'"
        )
    }

    @Test
    fun `mismatched manifest prefix throws instead of silently installing a broken package`() {
        val brokenManifest = glassSheetManifest.copy(prefix = "wrong_prefix")
        val thrown = runCatching {
            Rewriter.resolveTargetPath(
                testConfig, brokenManifest, "src/internal/glass_sheet/DragHandle.kt"
            )
        }
        assertTrue(thrown.isFailure, "Expected an exception when prefix doesn't match the folder name")
    }

    @Test
    fun `componentPath is configurable and actually changes the resolved package`() {
        val customConfig = testConfig.copy(componentPath = "presentation")
        val target = Rewriter.resolveTargetPath(customConfig, glassSheetManifest, "src/BottomSheet.kt")
        assertEquals(
            "com.testapp.myapp.presentation.glass_sheet",
            target.fullPackage
        )
    }

    /**
     * The hyphen mapping is the single place the web naming convention meets
     * Kotlin's package rules. A hyphen surviving into a package declaration is
     * an immediate compile error in the user's project.
     */
    @Test
    fun `a hyphenated component name becomes an underscored package segment`() {
        val textField = buttonManifest.copy(
            name = "text-field",
            registryPackage = "com.lamintra.text_field",
            prefix = "text_field",
            main = "src/LayerTextField.kt",
            files = listOf("src/LayerTextField.kt")
        )
        val target = Rewriter.resolveTargetPath(testConfig, textField, "src/LayerTextField.kt")
        assertEquals(
            "com.testapp.myapp.features.shared.widgets.text_field",
            target.fullPackage
        )
        assertTrue(
            !target.relativePath.contains("-"),
            "no hyphen may survive into an installed path: ${target.relativePath}"
        )
    }

    @Test
    fun `a name that is not package-legal is rejected at manifest load`() {
        val json = """
            {
              "name": "Text Field!",
              "registryPackage": "com.lamintra.text_field",
              "main": "src/LayerTextField.kt",
              "prefix": "text_field",
              "files": ["src/LayerTextField.kt"]
            }
        """.trimIndent()
        val thrown = runCatching { ComponentManifest.parse(json) }
        assertTrue(thrown.isFailure, "A non-kebab-case component name must be rejected")
    }

    @Test
    fun `a name that collides with a Kotlin keyword is rejected at manifest load`() {
        val json = """
            {
              "name": "object",
              "registryPackage": "com.lamintra.object",
              "main": "src/Thing.kt",
              "prefix": "object",
              "files": ["src/Thing.kt"]
            }
        """.trimIndent()
        val thrown = runCatching { ComponentManifest.parse(json) }
        assertTrue(thrown.isFailure, "A name that becomes a reserved keyword must be rejected")
    }

    @Test
    fun `categories are optional metadata and do not affect the installed package`() {
        val withCategories = buttonManifest.copy(categories = listOf("button", "form"))
        val without = buttonManifest.copy(categories = emptyList())
        assertEquals(
            Rewriter.resolveTargetPath(testConfig, withCategories, "src/LayerButton.kt").fullPackage,
            Rewriter.resolveTargetPath(testConfig, without, "src/LayerButton.kt").fullPackage
        )
    }

    @Test
    fun `a normal path resolves inside the project`() {
        val projectDir = createTempDirectory("lamintra-safe").toFile()
        val target = Rewriter.resolveSafeTarget(
            projectDir, "composeApp/src/commonMain/kotlin/com/x/Button.kt"
        )
        assertTrue(
            target.canonicalPath.startsWith(projectDir.canonicalPath),
            "A normal relative path must resolve inside the project directory"
        )
    }

    @Test
    fun `a traversing path is refused instead of writing outside the project`() {
        val projectDir = createTempDirectory("lamintra-traversal").toFile()
        val thrown = runCatching {
            Rewriter.resolveSafeTarget(projectDir, "../../../../evil.kt")
        }
        assertTrue(
            thrown.isFailure,
            "A manifest path escaping the project directory must be refused"
        )
    }

    @Test
    fun `an absolute path is refused`() {
        val projectDir = createTempDirectory("lamintra-absolute").toFile()
        val absolute = File(createTempDirectory("lamintra-elsewhere").toFile(), "evil.kt").absolutePath
        val thrown = runCatching { Rewriter.resolveSafeTarget(projectDir, absolute) }
        assertTrue(thrown.isFailure, "An absolute manifest path must be refused")
    }

    @Test
    fun `a sibling directory sharing the project name prefix is refused`() {
        // ".../proj" must not be treated as containing ".../proj-evil" - the
        // guard compares path boundaries, not raw string prefixes.
        val base = createTempDirectory("lamintra-prefix").toFile()
        val projectDir = File(base, "proj").apply { mkdirs() }
        File(base, "proj-evil").mkdirs()
        val thrown = runCatching { Rewriter.resolveSafeTarget(projectDir, "../proj-evil/x.kt") }
        assertTrue(thrown.isFailure, "A sibling dir sharing the name prefix must be refused")
    }

    // ---- Cross-component requirements. Added 2026-08-11 with the shared theme.

    private val themedButtonManifest = ComponentManifest(
        name = "button",
        categories = listOf("button"),
        registryPackage = "com.lamintra.button",
        main = "src/LamintraButton.kt",
        prefix = "button",
        files = listOf("src/LamintraButton.kt"),
        requires = listOf("theme")
    )

    /**
     * The bug this whole field exists for: a component importing shared code
     * used to install with that import still pointing at OUR namespace, which
     * compiles in the registry and fails in the user's project.
     */
    @Test
    fun `rewrites a required component's package, not just its own`() {
        val source = """
            package com.lamintra.button

            import com.lamintra.theme.LamintraTheme
            import com.lamintra.theme.lamintraDarkColors
            import com.lamintra.button.internal.button.Squircle
        """.trimIndent()

        val out = Rewriter.rewriteFileContent(source, testConfig, themedButtonManifest)

        assertTrue(
            out.contains("package com.testapp.myapp.features.shared.widgets.button"),
            "the component's own package must still be rewritten"
        )
        assertTrue(
            out.contains("import com.testapp.myapp.features.shared.widgets.theme.LamintraTheme"),
            "a required component's package must be rewritten too"
        )
        assertTrue(
            out.contains("import com.testapp.myapp.features.shared.widgets.button.internal.button.Squircle"),
            "internal cross-references must still work"
        )
        assertTrue(
            !out.contains("com.lamintra"),
            "no reference to the registry namespace may survive: found in $out"
        )
    }

    /** A component that requires nothing must be byte-identical to before. */
    @Test
    fun `a component with no requirements is unaffected`() {
        val source = "package com.lamintra.button\n\nimport com.lamintra.button.internal.button.Squircle\n"

        val withField = Rewriter.rewriteFileContent(source, testConfig, themedButtonManifest.copy(requires = emptyList()))
        val withoutField = Rewriter.rewriteFileContent(source, testConfig, buttonManifest)

        assertEquals(withoutField, withField)
    }

    /** Requirements become package segments, so they take the same slug rule. */
    @Test
    fun `an illegal requirement name is rejected at parse time`() {
        val json = """
            {
              "name": "button",
              "registryPackage": "com.lamintra.button",
              "main": "src/LamintraButton.kt",
              "prefix": "button",
              "files": ["src/LamintraButton.kt"],
              "requires": ["Not-A-Slug"]
            }
        """.trimIndent()

        val failure = runCatching { ComponentManifest.parse(json) }.exceptionOrNull()
        assertTrue(
            failure is IllegalArgumentException,
            "expected a rejection, got: $failure"
        )
    }
}
