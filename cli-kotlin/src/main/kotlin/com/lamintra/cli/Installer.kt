package com.lamintra.cli

import java.io.File

object Installer {
    // Registry transport lives in Registry.kt: two commands need it now.
    private fun fetch(relativeUrl: String): String = Registry.fetch(relativeUrl)

    /**
     * Installs a component by name (e.g. "button" or "text-field") into
     * [projectDir], using [config] to decide package names and file
     * locations. Returns a human-readable log of what was written, for
     * the CLI to print.
     */
    fun install(
        componentName: String,
        projectDir: File,
        config: LamintraConfig,
        force: Boolean = false
    ): List<String> = install(componentName, projectDir, config, mutableSetOf(), force)

    /**
     * [installed] guards against doing the same component twice in one run,
     * which happens as soon as two components share a requirement, and against
     * a registry that declares a requirement cycle. Without it a cycle is a
     * stack overflow rather than a message.
     */
    private fun install(
        componentName: String,
        projectDir: File,
        config: LamintraConfig,
        installed: MutableSet<String>,
        force: Boolean
    ): List<String> {
        if (!installed.add(componentName)) return emptyList()

        val log = mutableListOf<String>()

        val manifestText = fetch("$componentName/component.json")
        val manifest = ComponentManifest.parse(manifestText)

        // Requirements first: shared code has to be on disk before the file
        // that imports it, or the project is briefly uncompilable between two
        // writes. Depth-first, so a requirement's own requirements land first.
        for (required in manifest.requires) {
            log += install(required, projectDir, config, installed, force)
        }

        val newRoot = Rewriter.computeNewRootPackage(config, manifest.packageSegment)

        val target = Rewriter.resolveTargetPath(config, manifest, manifest.main)
        val existingElsewhere = findExistingCopyElsewhere(projectDir, config, manifest, target.relativePath)
        if (existingElsewhere != null) {
            error(
                "${manifest.name} already exists in this module at:\n" +
                    "    $existingElsewhere\n" +
                    "Installing to '${target.relativePath}' as well would create duplicate " +
                    "declarations in the same module and break the build (both directories " +
                    "compile). Delete the existing copy first, or re-run 'lamintra init' " +
                    "so the configured source root matches where it already lives."
            )
        }

        log += "Installing ${manifest.name}"
        log += "  registry package : ${manifest.registryPackage}"
        log += "  new root package  : $newRoot"
        if (File(projectDir, target.relativePath).exists()) {
            // This said "updating in place" until 2026-08-21, which was a
            // promise the tool then broke: it rewrote edited files. It is also
            // no longer accurate, because a file that differs is now kept. The
            // per-file lines below say exactly what happened to each one, so
            // this only needs to establish that the component is already here.
            log += "  (component already installed here)"
        }

        val protected = mutableListOf<String>()

        // Everything is fetched and rewritten BEFORE anything is written, so a
        // network failure partway through a component cannot leave files on
        // disk. The writes that follow then go through a journal that can undo
        // them. See FINDING 10: blocking the fourth of switch's five files used
        // to leave LamintraSwitch.kt on disk calling a `softShadow` whose
        // source never arrived, so a project that compiled before the command
        // did not compile after it - and the only thing printed was a raw
        // filesystem path.
        val planned = manifest.files.map { relFile ->
            val originalContent = fetch("$componentName/$relFile")
            val rewritten = Rewriter.rewriteFileContent(originalContent, config, manifest)
            Rewriter.resolveTargetPath(config, manifest, relFile).relativePath to rewritten
        }

        val journal = WriteJournal(projectDir)
        try {
            for ((relativePath, content) in planned) {
                when (writeOrProtect(journal, relativePath, content, force)) {
                    WriteOutcome.WROTE -> log += "  wrote: $relativePath"
                    WriteOutcome.UNCHANGED -> log += "  unchanged: $relativePath"
                    WriteOutcome.PROTECTED -> {
                        protected += relativePath
                        log += "  KEPT YOUR VERSION: $relativePath"
                    }
                }
            }
            installPreviewIfSafe(
                componentName, projectDir, config, manifest, newRoot, log, force, journal
            )
        } catch (t: Throwable) {
            val undone = journal.rollback()
            // Deliberately does NOT claim the project is untouched. A component
            // installed earlier as a requirement completed successfully and is
            // left in place - rolling back a good install would be surprising,
            // and would delete a file the user may already have had. Saying
            // "your project is as it was" would be false in exactly that case,
            // which is the kind of not-quite-true message this whole tool has
            // been audited for.
            error(
                buildString {
                    append("${manifest.name} was not installed: ")
                    append(t.message ?: t::class.java.simpleName)
                    append("\n")
                    if (undone == 0) {
                        append("No files from ${manifest.name} were left behind.")
                    } else {
                        append("$undone partly-written file(s) from ${manifest.name} were ")
                        append("removed, so it has not left a half-installed component behind.")
                    }
                    if (manifest.requires.isNotEmpty()) {
                        append("\nIts requirement(s) - ${manifest.requires.joinToString(", ")} - ")
                        append("installed successfully and were left in place.")
                    }
                    append("\nFix the cause and run the same command again.")
                }
            )
        }

        if (protected.isNotEmpty()) log += describeProtected(protected, componentName)

        return log
    }

    private enum class WriteOutcome { WROTE, UNCHANGED, PROTECTED }

    /**
     * Writes files, remembering enough to undo them.
     *
     * A component is several files that only make sense together - `switch`
     * installs five, and its main file calls into two of the others - so a
     * failure halfway through leaves a component that cannot compile. There is
     * no transaction on a filesystem, so this keeps the smallest thing that
     * gives one: what each file was before it was touched.
     *
     * Rollback restores content for files that already existed and deletes
     * files this run created. Directories created along the way are left; an
     * empty directory breaks nothing, and removing them risks deleting one that
     * was already there.
     */
    private class WriteJournal(private val projectDir: File) {
        /** null [contentBefore] means the file did not exist before this run. */
        private data class Entry(val relativePath: String, val contentBefore: String?)

        private val entries = mutableListOf<Entry>()

        /**
         * The entry is recorded only AFTER the write succeeds.
         *
         * Recording it first looks harmless and is not: a write that throws
         * would then be rolled back too, and rollback deletes paths it believes
         * it created. Caught in testing when a blocked write left a directory
         * in the journal and rollback removed it - a directory that was there
         * before the command ran and belonged to the user. Nothing that was not
         * written gets undone.
         */
        fun write(relativePath: String, content: String) {
            val existing = File(projectDir, relativePath)
            val before = if (existing.isFile) runCatching { existing.readText() }.getOrNull() else null
            Rewriter.writeInstalledFile(projectDir, relativePath, content)
            entries += Entry(relativePath, before)
        }

        fun exists(relativePath: String): Boolean = File(projectDir, relativePath).isFile

        fun read(relativePath: String): String? =
            runCatching { File(projectDir, relativePath).readText() }.getOrNull()

        /** Undoes every write, newest first. Returns how many files it touched. */
        fun rollback(): Int {
            var undone = 0
            for (entry in entries.asReversed()) {
                val file = File(projectDir, entry.relativePath)
                val restored = runCatching {
                    when {
                        // Never delete a directory. Only a file this run wrote
                        // can be removed, and only a file can be restored.
                        !file.isFile -> false
                        entry.contentBefore == null -> file.delete()
                        else -> { file.writeText(entry.contentBefore); true }
                    }
                }.getOrDefault(false)
                if (restored) undone++
            }
            entries.clear()
            return undone
        }
    }

    /**
     * Writes a file unless doing so would destroy an edit the user made.
     *
     * **This is the product's central promise, and until 2026-08-21 the tool
     * broke it.** Every component requires `theme`, so every `add` of anything
     * rewrote `LamintraTheme.kt` - 385 lines of colour ramp, radius scale and
     * motion tokens, and the single file users are most likely to have
     * customised. An edit to it survived exactly until the next `add`, which
     * destroyed it with no prompt, no backup, and the closing line "Installed
     * card with zero manual fixes needed."
     *
     * The install page argues that the absence of an upgrade command is a
     * deliberate position: "once a component is in your repository, it is yours
     * to change, and a tool that rewrote your edits later would be taking that
     * back." That is now true.
     *
     * Three outcomes, and the common one is silent:
     * - the file is absent -> write it
     * - the file is byte-identical to what we would write -> nothing to do,
     *   which is what re-installing an untouched component looks like
     * - the file differs -> it has been edited, or it came from a different
     *   registry version. Either way the bytes on disk are the user's, so keep
     *   them and say so. `--force` is the way to take the new version.
     */
    private fun writeOrProtect(
        journal: WriteJournal,
        relativePath: String,
        content: String,
        force: Boolean
    ): WriteOutcome {
        if (journal.exists(relativePath) && !force) {
            val onDisk = journal.read(relativePath)
            if (onDisk == content) return WriteOutcome.UNCHANGED
            if (onDisk != null) return WriteOutcome.PROTECTED
        }
        journal.write(relativePath, content)
        return WriteOutcome.WROTE
    }

    private fun describeProtected(protected: List<String>, componentName: String): String =
        buildString {
            append("\n")
            append(
                if (protected.size == 1) "One file was left alone because it differs "
                else "${protected.size} files were left alone because they differ "
            )
            append("from the registry's version:\n")
            protected.forEach { append("    $it\n") }
            append("These are yours. Most often it means you edited them, and nothing\n")
            append("here will overwrite an edit you made.\n")
            append("If you do want the registry's current version, and you are willing\n")
            append("to lose those changes:\n")
            append("    lamintra add $componentName --force")
        }

    /**
     * Installs the component's optional @Preview demo file, but only when
     * it cannot break the build: the androidx preview annotation needs the
     * ui-tooling-preview artifact, so the module's build file is
     * text-scanned (comments stripped - never evaluated) for evidence of
     * that dependency. Found -> install to the android source root
     * (androidMain for KMP; the annotation doesn't exist in common code).
     * Not found -> skip the file and say how to enable it. A false negative
     * degrades to a printed hint; a missing dependency never gets a file
     * that would fail to compile - the zero-compile-error promise wins
     * over preview convenience.
     */
    private fun installPreviewIfSafe(
        componentName: String,
        projectDir: File,
        config: LamintraConfig,
        manifest: ComponentManifest,
        newRoot: String,
        log: MutableList<String>,
        force: Boolean,
        journal: WriteJournal
    ) {
        val previewRel = manifest.preview ?: return
        val androidRoot = config.sourceRoots.android
        if (androidRoot == null) {
            log += "  (preview skipped - no android source root in .lamintra/config.json)"
            return
        }
        if (!moduleBuildFileShowsPreviewDep(projectDir, config)) {
            log += "  Preview file skipped: couldn't confirm the ui-tooling-preview"
            log += "  dependency in this module's build file. To get Android Studio"
            log += "  previews, add the dependency, e.g.:"
            log += "      implementation(\"androidx.compose.ui:ui-tooling-preview\")"
            log += "      debugImplementation(\"androidx.compose.ui:ui-tooling\")"
            log += "  then re-run: lamintra add $componentName"
            return
        }
        val content = fetch("$componentName/$previewRel")
        val rewritten = Rewriter.rewriteFileContent(content, config, manifest)
        val fileName = previewRel.substringAfterLast('/')
        val relativePath = listOf(androidRoot, newRoot.replace('.', '/'), fileName)
            .joinToString("/")
            .replace("//", "/")
        when (writeOrProtect(journal, relativePath, rewritten, force)) {
            WriteOutcome.WROTE -> log += "  wrote: $relativePath (Android Studio preview)"
            WriteOutcome.UNCHANGED -> log += "  unchanged: $relativePath (Android Studio preview)"
            WriteOutcome.PROTECTED -> log += "  KEPT YOUR VERSION: $relativePath (Android Studio preview)"
        }
    }

    private fun moduleBuildFileShowsPreviewDep(projectDir: File, config: LamintraConfig): Boolean {
        val sourceRoot = config.activeSourceRoot()
        val moduleRel = sourceRoot.substringBefore("/src/", missingDelimiterValue = "")
        if (moduleRel.isEmpty()) return false
        val buildFile = listOf("build.gradle.kts", "build.gradle")
            .map { File(projectDir, "$moduleRel/$it") }
            .firstOrNull { it.isFile } ?: return false
        val withoutComments = buildFile.readLines().joinToString("\n") { it.substringBefore("//") }
        // Catches the artifact id (ui-tooling-preview), version-catalog
        // accessors (androidx.ui.tooling.preview), and Compose Multiplatform
        // DSL accessors (compose.uiTooling / compose.preview).
        return listOf("ui-tooling", "ui.tooling", "uiTooling", "compose.preview")
            .any { withoutComments.contains(it) }
    }

    /**
     * Guards against the duplicate-install foot-gun: run init, install,
     * re-run init with a different source root (or package), install again
     * - and the same component now exists twice in one module, which is a
     * guaranteed "conflicting overloads" compile error because Gradle
     * compiles every source root (src/main/java AND src/main/kotlin both
     * count). Scans the module's src/ tree for this component's
     * package-segment/main-file path landing anywhere other than the
     * current target.
     */
    private fun findExistingCopyElsewhere(
        projectDir: File,
        config: LamintraConfig,
        manifest: ComponentManifest,
        targetRelativePath: String
    ): String? {
        val sourceRoot = config.activeSourceRoot()
        val moduleRel = sourceRoot.substringBefore("/src/", missingDelimiterValue = "")
        if (moduleRel.isEmpty()) return null
        val srcDir = File(projectDir, "$moduleRel/src")
        if (!srcDir.isDirectory) return null

        val mainFileName = manifest.main.substringAfterLast('/')
        val suffix = "${manifest.packageSegment}/$mainFileName"
        val targetCanonical = File(projectDir, targetRelativePath).canonicalFile

        for (file in srcDir.walkTopDown().onEnter { it.name != "build" }) {
            if (!file.isFile || file.name != mainFileName) continue
            val rel = file.relativeTo(projectDir).path.replace('\\', '/')
            if (rel.endsWith(suffix) && file.canonicalFile != targetCanonical) {
                return rel
            }
        }
        return null
    }
}
