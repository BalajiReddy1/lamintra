package com.jetcompose.cli

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RewriterTest {

    private val testConfig = JetComposeConfig(
        packageName = "com.testapp.myapp",
        isKmp = true,
        sourceRoots = SourceRoots(
            common = "composeApp/src/commonMain/kotlin",
            android = "composeApp/src/androidMain/kotlin",
            ios = "composeApp/src/iosMain/kotlin"
        ),
        componentPath = "features/shared/widgets"
    )

    private val bottomSheetManifest = ComponentManifest(
        name = "bottomsheet/glass",
        category = "bottomsheet",
        style = "glass",
        registryPackage = "com.jetcompose.bottomsheet.glass",
        main = "src/BottomSheet.kt",
        prefix = "bottomsheet_glass",
        files = listOf(
            "src/BottomSheet.kt",
            "src/internal/bottomsheet_glass/DragHandle.kt",
            "src/internal/bottomsheet_glass/ModifierExtensions.kt"
        )
    )

    private val neonManifest = ComponentManifest(
        name = "button/neon",
        category = "button",
        style = "neon",
        registryPackage = "com.jetcompose.button.neon",
        main = "src/NeonButton.kt",
        prefix = "button_neon",
        files = listOf(
            "src/NeonButton.kt",
            "src/internal/button_neon/ModifierExtensions.kt"
        )
    )

    @Test
    fun `package declaration is rewritten to the target namespace`() {
        val original = "package com.jetcompose.bottomsheet.glass\n\nimport androidx.compose.runtime.Composable\n"
        val rewritten = Rewriter.rewriteFileContent(original, testConfig, bottomSheetManifest)
        assertTrue(
            rewritten.startsWith("package com.testapp.myapp.features.shared.widgets.bottomsheet.glass"),
            "got: ${rewritten.lines().first()}"
        )
    }

    @Test
    fun `cross-file internal import is rewritten to the new namespace`() {
        val original = "package com.jetcompose.bottomsheet.glass\n" +
            "import com.jetcompose.bottomsheet.glass.internal.bottomsheet_glass.DragHandle\n"
        val rewritten = Rewriter.rewriteFileContent(original, testConfig, bottomSheetManifest)
        assertTrue(
            rewritten.contains(
                "import com.testapp.myapp.features.shared.widgets.bottomsheet.glass.internal.bottomsheet_glass.DragHandle"
            )
        )
    }

    @Test
    fun `unrelated androidx imports are left untouched`() {
        val original = "package com.jetcompose.bottomsheet.glass\n" +
            "import androidx.compose.foundation.background\n"
        val rewritten = Rewriter.rewriteFileContent(original, testConfig, bottomSheetManifest)
        assertTrue(rewritten.contains("import androidx.compose.foundation.background"))
    }

    @Test
    fun `boundary-safe rewrite does not corrupt a decoy package with a longer name`() {
        val original = "package com.jetcompose.bottomsheet.glass\n" +
            "import com.jetcompose.bottomsheet.glassy.SomeUnrelatedThing\n" +
            "import com.jetcompose.bottomsheet.glass2.AnotherUnrelatedThing\n"
        val rewritten = Rewriter.rewriteFileContent(original, testConfig, bottomSheetManifest)
        assertTrue(rewritten.contains("import com.jetcompose.bottomsheet.glassy.SomeUnrelatedThing"))
        assertTrue(rewritten.contains("import com.jetcompose.bottomsheet.glass2.AnotherUnrelatedThing"))
    }

    @Test
    fun `two components sharing a filename resolve to different paths and packages`() {
        val bottomSheetTarget = Rewriter.resolveTargetPath(
            testConfig, bottomSheetManifest, "src/internal/bottomsheet_glass/ModifierExtensions.kt"
        )
        val neonTarget = Rewriter.resolveTargetPath(
            testConfig, neonManifest, "src/internal/button_neon/ModifierExtensions.kt"
        )
        assertNotEquals(bottomSheetTarget.relativePath, neonTarget.relativePath)
        assertNotEquals(bottomSheetTarget.fullPackage, neonTarget.fullPackage)
    }

    @Test
    fun `resolved file path exactly matches its own declared package`() {
        val target = Rewriter.resolveTargetPath(testConfig, bottomSheetManifest, "src/BottomSheet.kt")
        val expectedDirSuffix = target.fullPackage.replace(".", "/")
        assertTrue(
            target.relativePath.contains(expectedDirSuffix),
            "relativePath '${target.relativePath}' should contain '$expectedDirSuffix'"
        )
    }

    @Test
    fun `mismatched manifest prefix throws instead of silently installing a broken package`() {
        val brokenManifest = bottomSheetManifest.copy(prefix = "wrong_prefix")
        val thrown = runCatching {
            Rewriter.resolveTargetPath(
                testConfig, brokenManifest, "src/internal/bottomsheet_glass/DragHandle.kt"
            )
        }
        assertTrue(thrown.isFailure, "Expected an exception when prefix doesn't match the folder name")
    }

    @Test
    fun `componentPath is configurable and actually changes the resolved package`() {
        val customConfig = testConfig.copy(componentPath = "presentation")
        val target = Rewriter.resolveTargetPath(customConfig, bottomSheetManifest, "src/BottomSheet.kt")
        assertEquals(
            "com.testapp.myapp.presentation.bottomsheet.glass",
            target.fullPackage
        )
    }
}
