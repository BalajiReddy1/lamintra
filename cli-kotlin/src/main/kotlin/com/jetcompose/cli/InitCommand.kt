package com.jetcompose.cli

import java.io.File

object InitCommand {
    fun run(projectDir: File) {
        println("jetcompose init")
        println("Answer a few questions once — this writes .jetcompose/config.json")
        println("so every future 'jetcompose add' knows exactly where things go.\n")

        val packageName = prompt("Root package name", default = "com.mycompany.app")
        val isKmp = promptYesNo("Is this a Kotlin Multiplatform project?", default = true)

        val commonRoot: String?
        val androidRoot: String?
        val iosRoot: String?
        val suspiciousRoots = mutableListOf<String>()

        fun promptSourceRoot(question: String, default: String): String {
            val root = prompt(question, default)
            if (!hasGradleBuildFileNearby(projectDir, root)) {
                println(
                    "  Warning: no Gradle build file (build.gradle.kts / build.gradle) " +
                        "found near '$root' — double-check it's correct. A wrong source " +
                        "root means installed files land where Gradle never compiles them."
                )
                suspiciousRoots += root
            }
            return root
        }

        if (isKmp) {
            commonRoot = promptSourceRoot(
                "Source root for shared (commonMain) code",
                default = "composeApp/src/commonMain/kotlin"
            )
            androidRoot = promptSourceRoot(
                "Source root for Android-specific code",
                default = "composeApp/src/androidMain/kotlin"
            )
            iosRoot = promptSourceRoot(
                "Source root for iOS-specific code",
                default = "composeApp/src/iosMain/kotlin"
            )
        } else {
            commonRoot = null
            androidRoot = promptSourceRoot(
                "Source root for your app's Kotlin code",
                default = "app/src/main/kotlin"
            )
            iosRoot = null
        }

        val componentPath = prompt(
            "Where should installed components live, under your package root?\n" +
                "  (e.g. 'ui/components', 'presentation/widgets', or leave blank for none)",
            default = "ui/components"
        )

        if (suspiciousRoots.isNotEmpty()) {
            val proceed = promptYesNo(
                "\n${suspiciousRoots.size} source root(s) had no Gradle build file nearby. " +
                    "Write config anyway?",
                default = false
            )
            if (!proceed) {
                println("Aborted — nothing written. Re-run 'jetcompose init' with corrected paths.")
                return
            }
        }

        val config = JetComposeConfig(
            packageName = packageName,
            isKmp = isKmp,
            sourceRoots = SourceRoots(common = commonRoot, android = androidRoot, ios = iosRoot),
            componentPath = componentPath
        )

        writeConfig(projectDir, config)
        println("\n✅ Wrote .jetcompose/config.json")
        println("   Run 'jetcompose add <component>' to install your first component,")
        println("   e.g.: jetcompose add bottomsheet/glass")
    }

    private fun writeConfig(projectDir: File, config: JetComposeConfig) {
        val dir = File(projectDir, ".jetcompose")
        dir.mkdirs()
        val json = buildString {
            appendLine("{")
            appendLine("  \"packageName\": \"${config.packageName}\",")
            appendLine("  \"isKmp\": ${config.isKmp},")
            appendLine("  \"sourceRoots\": {")
            val roots = listOfNotNull(
                config.sourceRoots.common?.let { "    \"common\": \"$it\"" },
                config.sourceRoots.android?.let { "    \"android\": \"$it\"" },
                config.sourceRoots.ios?.let { "    \"ios\": \"$it\"" }
            )
            append(roots.joinToString(",\n"))
            appendLine()
            appendLine("  },")
            appendLine("  \"componentPath\": \"${config.componentPath}\"")
            appendLine("}")
        }
        File(dir, "config.json").writeText(json)
    }

    /**
     * A plausible source root should have a Gradle build file somewhere in
     * its module directory — e.g. `composeApp/src/commonMain/kotlin` sits
     * three levels below `composeApp/build.gradle.kts`. Walks from the
     * entered path (which may not exist yet — iosMain often doesn't) up
     * through at most four ancestors, never above the project root.
     *
     * Depth-bounded on purpose: in a multi-module repo the project root
     * often has its own build.gradle.kts, and an unbounded walk would
     * "find" it for any garbage path, defeating the check. The bound means
     * a typo in the module segment of a deep path (e.g. `featur/ui/src/...`)
     * is caught, while a typo in the source-set segment of a shallow path
     * (e.g. `composeApp/src/comonMain/...`) can still slip through — this
     * is a heuristic warning, not a guarantee.
     */
    private fun hasGradleBuildFileNearby(projectDir: File, sourceRoot: String): Boolean {
        val projectCanonical = projectDir.canonicalFile
        var dir: File? = File(projectDir, sourceRoot).canonicalFile
        var levelsLeft = 5 // the source root itself + four ancestors
        while (dir != null && levelsLeft > 0) {
            if (File(dir, "build.gradle.kts").isFile || File(dir, "build.gradle").isFile) {
                return true
            }
            if (dir == projectCanonical) return false // don't escape the project
            dir = dir.parentFile
            levelsLeft--
        }
        return false
    }

    private fun prompt(question: String, default: String): String {
        print("$question [$default]: ")
        val input = readlnOrNull()?.trim()
        return if (input.isNullOrEmpty()) default else input
    }

    private fun promptYesNo(question: String, default: Boolean): Boolean {
        val defaultLabel = if (default) "Y/n" else "y/N"
        print("$question [$defaultLabel]: ")
        val input = readlnOrNull()?.trim()?.lowercase()
        return when (input) {
            null, "" -> default
            "y", "yes" -> true
            "n", "no" -> false
            else -> default
        }
    }
}
