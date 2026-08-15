package com.lamintra.cli

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object Installer {
    // GitHub raw content base. Using raw.githubusercontent.com means no
    // GitHub API rate limits for unauthenticated reads at this scale, and
    // no extra client library - it's a plain HTTPS GET.
    //
    // Pinned to a release tag, NOT `main`: raw.githubusercontent.com
    // caches branch URLs for ~5 minutes, so fetching `main` can serve
    // transient 404s or stale files right after a registry push. Tagged
    // URLs are immutable - bump the tag here (and re-release the jar)
    // to pick up registry changes.
    private const val PUBLISHED_REGISTRY =
        "https://raw.githubusercontent.com/BalajiReddy1/lamintra-registry/v0.5.2"

    /**
     * Where components are read from. The published tag, unless overridden.
     *
     * **Why an override exists.** Until 2026-08-11 this was a hard-coded
     * constant, which made the release order impossible to get right: the only
     * way to test an install was to publish a registry tag first, and the only
     * responsible time to publish is after testing an install. Every release
     * therefore shipped its registry changes untested, and the one time that
     * was checked properly it was checked *after the fact*.
     *
     * Set `LAMINTRA_REGISTRY` to close that loop:
     *
     *     # a working tree, so you test the exact files you are about to tag
     *     LAMINTRA_REGISTRY=/path/to/jetcompose/registry lamintra add button
     *
     *     # or a branch, before cutting the tag
     *     LAMINTRA_REGISTRY=https://raw.githubusercontent.com/OWNER/REPO/main
     *
     * A value that does not start with http is treated as a local directory.
     * This is a developer affordance and is not needed by users: unset, the
     * behaviour is exactly what it always was.
     */
    private val REGISTRY_BASE: String =
        System.getenv("LAMINTRA_REGISTRY")?.takeIf { it.isNotBlank() } ?: PUBLISHED_REGISTRY

    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private fun fetch(relativeUrl: String): String {
        if (!REGISTRY_BASE.startsWith("http://") && !REGISTRY_BASE.startsWith("https://")) {
            val local = File(REGISTRY_BASE, relativeUrl)
            if (!local.isFile) {
                error(
                    "Not found in the local registry: ${local.path}. " +
                        "LAMINTRA_REGISTRY is set to '$REGISTRY_BASE'. Unset it to use the " +
                        "published registry."
                )
            }
            return local.readText()
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$REGISTRY_BASE/$relativeUrl"))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            error(
                "Failed to fetch $relativeUrl (HTTP ${response.statusCode()}). " +
                    "Check the component name is correct, e.g. 'button' or 'text-field'."
            )
        }
        return response.body()
    }

    /**
     * Installs a component by name (e.g. "button" or "text-field") into
     * [projectDir], using [config] to decide package names and file
     * locations. Returns a human-readable log of what was written, for
     * the CLI to print.
     */
    fun install(componentName: String, projectDir: File, config: LamintraConfig): List<String> =
        install(componentName, projectDir, config, mutableSetOf())

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
        installed: MutableSet<String>
    ): List<String> {
        if (!installed.add(componentName)) return emptyList()

        val log = mutableListOf<String>()

        val manifestText = fetch("$componentName/component.json")
        val manifest = ComponentManifest.parse(manifestText)

        // Requirements first: shared code has to be on disk before the file
        // that imports it, or the project is briefly uncompilable between two
        // writes. Depth-first, so a requirement's own requirements land first.
        for (required in manifest.requires) {
            log += install(required, projectDir, config, installed)
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
            log += "  (component already installed here - updating in place)"
        }

        for (relFile in manifest.files) {
            val originalContent = fetch("$componentName/$relFile")
            val rewritten = Rewriter.rewriteFileContent(originalContent, config, manifest)
            val target = Rewriter.resolveTargetPath(config, manifest, relFile)

            Rewriter.writeInstalledFile(projectDir, target.relativePath, rewritten)
            log += "  wrote: ${target.relativePath}"
        }

        installPreviewIfSafe(componentName, projectDir, config, manifest, newRoot, log)

        return log
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
        log: MutableList<String>
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
        Rewriter.writeInstalledFile(projectDir, relativePath, rewritten)
        log += "  wrote: $relativePath (Android Studio preview)"
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
