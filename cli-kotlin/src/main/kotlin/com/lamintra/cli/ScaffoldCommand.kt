package com.lamintra.cli

import java.io.File

/**
 * `lamintra scaffold <name>` - writes project structure rather than a
 * component. See [ScaffoldManifest] for why these are separate concepts.
 *
 * The one scaffold today is `ios-shell`, which sets up the arrangement
 * JetBrains documents for Liquid Glass: native SwiftUI owns the tab bar and
 * the navigation stacks, Compose renders the content of each screen. That
 * split is not a style preference. Liquid Glass is drawn by the system through
 * `TabView`, `NavigationStack` and the toolbar APIs, and Compose renders to a
 * Skia canvas, so Compose cannot draw it at all. Owning the chrome natively is
 * the only way to get it.
 */
object ScaffoldCommand {

    fun run(scaffoldName: String, projectDir: File, force: Boolean) {
        val config = LamintraConfig.load(projectDir)
        val manifest = ScaffoldManifest.parse(
            Registry.fetch("scaffolds/$scaffoldName/scaffold.json")
        )

        println("lamintra scaffold ${manifest.name}")
        println("  ${manifest.title}")
        println()

        if (manifest.requiresKmp && !config.isKmp) {
            error(
                "\"${manifest.name}\" needs a Kotlin Multiplatform project, and " +
                    ".lamintra/config.json says this one is Android-only.\n" +
                    "There is no iOS target here for a SwiftUI shell to sit in front of."
            )
        }
        val iosRoot = config.sourceRoots.ios ?: error(
            "\"${manifest.name}\" needs sourceRoots.ios in .lamintra/config.json and " +
                "it is not set.\nRe-run 'lamintra init', or add the path to your " +
                "iosMain source set by hand."
        )
        val commonRoot = config.sourceRoots.common ?: error(
            "\"${manifest.name}\" needs sourceRoots.common in .lamintra/config.json " +
                "and it is not set.\nRe-run 'lamintra init'."
        )

        val swiftDir = findSwiftSourceDir(projectDir) ?: error(
            "Couldn't find an Xcode project in this directory tree.\n" +
                "Looked for a *.xcodeproj at most two levels below " +
                "${projectDir.path}.\nA Compose Multiplatform project normally has " +
                "one at iosApp/iosApp.xcodeproj. Open the iOS app once in Xcode, or " +
                "run this from the repository root."
        )
        val framework = detectFrameworkName(projectDir, iosRoot)

        println("  Package     : ${config.packageName}")
        println("  Swift files : ${swiftDir.relativeTo(projectDir).path.replace('\\', '/')}")
        println("  Framework   : $framework")
        println()

        val packagePath = config.packageName.replace('.', '/')
        val planned = manifest.files.map { file ->
            val relative = when (file.rootKeyword) {
                "swift" -> {
                    val dirRel = swiftDir.relativeTo(projectDir).path.replace('\\', '/')
                    "$dirRel/${file.destPath}"
                }
                "ios" -> "$iosRoot/$packagePath/${file.destPath}"
                "common" -> "$commonRoot/$packagePath/${file.destPath}"
                // Unreachable: ScaffoldManifest.validate rejects anything else at
                // parse time. Kept as an error rather than a silent default so a
                // future root keyword cannot be added in one place only.
                else -> error("Unknown destination root \"${file.rootKeyword}\".")
            }
            file to relative.replace("//", "/")
        }

        // Never clobber without being asked. The Swift half lands beside a
        // user's own app sources, and silently overwriting a file someone has
        // edited is the least forgivable thing an installer can do.
        val existing = planned.filter { (_, rel) -> File(projectDir, rel).exists() }
        if (existing.isNotEmpty() && !force) {
            val list = existing.joinToString("\n") { (_, rel) -> "    $rel" }
            error(
                "${existing.size} file(s) already exist and would be overwritten:\n" +
                    "$list\n" +
                    "Nothing has been written. Re-run with --force to replace them, " +
                    "or move them aside first."
            )
        }

        for ((file, relative) in planned) {
            val content = substitute(
                Registry.fetch("scaffolds/${manifest.name}/${file.from}"),
                config.packageName,
                framework
            )
            Rewriter.writeInstalledFile(projectDir, relative, content)
            println("  wrote: $relative")
        }

        if (manifest.postInstall.isNotEmpty()) {
            println()
            println("Two things this cannot do for you:")
            println()
            manifest.postInstall.forEach { println(it) }
        }
    }

    /**
     * Token substitution, deliberately not package rewriting.
     *
     * A component gets [Rewriter.rewriteFileContent], which finds
     * `com.lamintra.<slug>` and replaces it. That works because a component is
     * real Kotlin that compiles in our own tree. A scaffold is a template: its
     * Swift half references a framework module name that only exists in the
     * user's project, so there is nothing valid to write in the registry copy.
     * Explicit placeholders are honest about that.
     */
    internal fun substitute(content: String, packageName: String, framework: String): String =
        content
            .replace("{{PACKAGE}}", packageName)
            .replace("{{FRAMEWORK}}", framework)

    /**
     * The directory Xcode expects app sources in.
     *
     * The Compose Multiplatform wizard produces `iosApp/iosApp.xcodeproj` with
     * sources in `iosApp/iosApp/`, so the group directory is the one named
     * after the project file. Falls back to the directory holding the
     * `.xcodeproj` when that convention is not followed, which is wrong less
     * often than guessing a name.
     */
    internal fun findSwiftSourceDir(projectDir: File): File? {
        val xcodeProj = findXcodeProject(projectDir) ?: return null
        val xcodeRoot = xcodeProj.parentFile
        val named = File(xcodeRoot, xcodeProj.name.removeSuffix(".xcodeproj"))
        return if (named.isDirectory) named else xcodeRoot
    }

    private fun findXcodeProject(projectDir: File): File? {
        fun scan(dir: File, depth: Int): File? {
            if (depth > 2) return null
            val children = dir.listFiles()?.filter {
                it.isDirectory && !it.name.startsWith(".") && it.name !in setOf("build", "gradle")
            } ?: return null
            children.firstOrNull { it.name.endsWith(".xcodeproj") }?.let { return it }
            for (child in children) scan(child, depth + 1)?.let { return it }
            return null
        }
        return scan(projectDir, 0)
    }

    /**
     * The Swift module name for the Kotlin framework, which Swift needs in its
     * `import`. It is `baseName` in the KMP module's framework block, and it is
     * `ComposeApp` in the wizard's output often enough to be a sane fallback -
     * but reading it is cheap and being wrong here produces a Swift file that
     * does not compile.
     */
    internal fun detectFrameworkName(projectDir: File, iosSourceRoot: String): String {
        val moduleRel = iosSourceRoot.substringBefore("/src/", missingDelimiterValue = "")
        if (moduleRel.isNotEmpty()) {
            val buildFile = listOf("build.gradle.kts", "build.gradle")
                .map { File(projectDir, "$moduleRel/$it") }
                .firstOrNull { it.isFile }
            if (buildFile != null) {
                val withoutComments = buildFile.readLines()
                    .joinToString("\n") { it.substringBefore("//") }
                Regex("""baseName\s*=\s*"([^"]+)"""").find(withoutComments)
                    ?.groupValues?.get(1)
                    ?.let { return it }
            }
        }
        return "ComposeApp"
    }
}
