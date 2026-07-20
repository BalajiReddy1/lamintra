package com.jetcompose.cli

import java.io.File

object InitCommand {
    fun run(projectDir: File) {
        println("jetcompose init")

        val detected = detectLayout(projectDir)
        if (detected != null) {
            val componentPath = "ui/components"
            println("\nDetected from your project:")
            println("  Type         : ${if (detected.isKmp) "Kotlin Multiplatform" else "Android"}")
            println("  Source root  : ${detected.primaryRoot()}")
            println("  Root package : ${detected.packageName}")
            println(
                "  Components   : ${detected.packageName}." +
                    componentPath.replace('/', '.') + ".*"
            )
            val accepted = promptYesNo("\nUse these settings?", default = true)
            if (accepted) {
                val config = JetComposeConfig(
                    packageName = detected.packageName,
                    isKmp = detected.isKmp,
                    sourceRoots = SourceRoots(
                        common = detected.commonRoot,
                        android = detected.androidRoot,
                        ios = detected.iosRoot
                    ),
                    componentPath = componentPath
                )
                writeConfig(projectDir, config)
                printDone()
                return
            }
            println("No problem — answer a few questions instead.\n")
        } else {
            println("Couldn't auto-detect the project layout — answer a few questions.\n")
        }
        runManual(projectDir)
    }

    // ------------------------------------------------------------------
    // Detection: filesystem only, no Gradle evaluation. The layout, the
    // source roots, and the root package are all readable straight off
    // disk — that keeps init to a single confirm for standard projects
    // while never being as heavy or fragile as parsing build scripts.
    // ------------------------------------------------------------------

    private data class DetectedLayout(
        val isKmp: Boolean,
        val commonRoot: String?,
        val androidRoot: String?,
        val iosRoot: String?,
        val packageName: String
    ) {
        fun primaryRoot(): String = (if (isKmp) commonRoot else androidRoot) ?: "?"
    }

    private fun detectLayout(projectDir: File): DetectedLayout? {
        val modules = findGradleModules(projectDir)
        if (modules.isEmpty()) return null

        val kmpModules = modules.filter { File(it, "src/commonMain/kotlin").isDirectory }
        val androidModules = modules.filter {
            File(it, "src/main/java").isDirectory || File(it, "src/main/kotlin").isDirectory
        }

        val isKmp: Boolean
        val module: File
        when {
            kmpModules.size == 1 -> { isKmp = true; module = kmpModules.single() }
            kmpModules.size > 1 -> { isKmp = true; module = pickModule(projectDir, kmpModules) ?: return null }
            androidModules.size == 1 -> { isKmp = false; module = androidModules.single() }
            androidModules.size > 1 -> { isKmp = false; module = pickModule(projectDir, androidModules) ?: return null }
            else -> return null
        }

        val moduleRel = module.relativeTo(projectDir).path.replace('\\', '/')

        if (isKmp) {
            val commonRoot = "$moduleRel/src/commonMain/kotlin"
            val packageName = detectRootPackage(File(projectDir, commonRoot)) ?: return null
            return DetectedLayout(
                isKmp = true,
                commonRoot = commonRoot,
                androidRoot = "$moduleRel/src/androidMain/kotlin",
                iosRoot = "$moduleRel/src/iosMain/kotlin",
                packageName = packageName
            )
        }

        // Android Studio's template puts Kotlin sources under src/main/java;
        // prefer whichever root actually contains .kt files.
        val javaRoot = File(module, "src/main/java")
        val kotlinRoot = File(module, "src/main/kotlin")
        val root = when {
            kotlinRoot.isDirectory && containsKotlin(kotlinRoot) -> kotlinRoot
            javaRoot.isDirectory && containsKotlin(javaRoot) -> javaRoot
            kotlinRoot.isDirectory -> kotlinRoot
            else -> javaRoot
        }
        val androidRoot = "$moduleRel/src/main/${root.name}"
        val packageName = detectRootPackage(root) ?: return null
        return DetectedLayout(
            isKmp = false,
            commonRoot = null,
            androidRoot = androidRoot,
            iosRoot = null,
            packageName = packageName
        )
    }

    /** Gradle module dirs: subdirectories (2 levels deep max) holding a build file and a src/ dir. */
    private fun findGradleModules(projectDir: File): List<File> {
        val result = mutableListOf<File>()
        fun scan(dir: File, depth: Int) {
            if (depth > 2) return
            val children = dir.listFiles()
                ?.filter { it.isDirectory && !it.name.startsWith(".") && it.name !in setOf("build", "gradle") }
                ?: return
            for (child in children) {
                val hasBuildFile = File(child, "build.gradle.kts").isFile || File(child, "build.gradle").isFile
                if (hasBuildFile && File(child, "src").isDirectory) result += child
                scan(child, depth + 1)
            }
        }
        scan(projectDir, 0)
        return result
    }

    /** Multiple candidate modules → one numbered pick instead of free-typed paths. */
    private fun pickModule(projectDir: File, modules: List<File>): File? {
        println("\nSeveral modules found — which should components install into?")
        modules.forEachIndexed { i, m ->
            println("  ${i + 1}) ${m.relativeTo(projectDir).path.replace('\\', '/')}")
        }
        print("Enter a number [1]: ")
        val input = readlnOrNull()?.trim()
        val index = if (input.isNullOrEmpty()) 0 else (input.toIntOrNull()?.minus(1) ?: return null)
        return modules.getOrNull(index)
    }

    private fun containsKotlin(dir: File): Boolean =
        dir.walkTopDown().any { it.isFile && it.extension == "kt" }

    /**
     * The root package is readable from the source tree itself: the
     * shallowest .kt file declares it. Verified against the file's actual
     * directory path (Kotlin's path-must-match-package rule) so a
     * nonstandard layout falls back to asking rather than guessing wrong.
     */
    private fun detectRootPackage(sourceRoot: File): String? {
        if (!sourceRoot.isDirectory) return null
        val shallowest = sourceRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .minByOrNull { it.relativeTo(sourceRoot).path.count { c -> c == File.separatorChar } }
        if (shallowest != null) {
            val declared = Regex("""^\s*package\s+([\w.]+)""", RegexOption.MULTILINE)
                .find(shallowest.readText())?.groupValues?.get(1) ?: return null
            val dirPath = shallowest.parentFile.relativeTo(sourceRoot).path
                .replace(File.separatorChar, '.')
            return if (declared == dirPath) declared else null
        }
        // No sources yet (fresh project): a chain of single directories is
        // still an unambiguous package path.
        val segments = mutableListOf<String>()
        var dir = sourceRoot
        while (true) {
            val subdirs = dir.listFiles()?.filter { it.isDirectory } ?: return null
            if (subdirs.size != 1) break
            segments += subdirs.single().name
            dir = subdirs.single()
        }
        return if (segments.isEmpty()) null else segments.joinToString(".")
    }

    // ------------------------------------------------------------------
    // Manual flow — unchanged behavior, used when detection fails or the
    // user rejects the detected settings.
    // ------------------------------------------------------------------

    private fun runManual(projectDir: File) {
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
        printDone()
    }

    private fun printDone() {
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
