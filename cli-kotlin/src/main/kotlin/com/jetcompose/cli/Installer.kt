package com.jetcompose.cli

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object Installer {
    // GitHub raw content base. Using raw.githubusercontent.com means no
    // GitHub API rate limits for unauthenticated reads at this scale, and
    // no extra client library — it's a plain HTTPS GET.
    //
    // Pinned to a release tag, NOT `main`: raw.githubusercontent.com
    // caches branch URLs for ~5 minutes, so fetching `main` can serve
    // transient 404s or stale files right after a registry push. Tagged
    // URLs are immutable — bump the tag here (and re-release the jar)
    // to pick up registry changes.
    private const val REGISTRY_BASE = "https://raw.githubusercontent.com/BalajiReddy1/jetcompose-registry/v0.2.1"

    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private fun fetch(relativeUrl: String): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$REGISTRY_BASE/$relativeUrl"))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            error(
                "Failed to fetch $relativeUrl (HTTP ${response.statusCode()}). " +
                    "Check the component name is correct, e.g. 'bottomsheet/glass'."
            )
        }
        return response.body()
    }

    /**
     * Installs a component by name (e.g. "bottomsheet/glass") into
     * [projectDir], using [config] to decide package names and file
     * locations. Returns a human-readable log of what was written, for
     * the CLI to print.
     */
    fun install(componentName: String, projectDir: File, config: JetComposeConfig): List<String> {
        val log = mutableListOf<String>()

        val manifestText = fetch("$componentName/component.json")
        val manifest = ComponentManifest.parse(manifestText)

        val newRoot = Rewriter.computeNewRootPackage(config, manifest.category, manifest.style)

        val target = Rewriter.resolveTargetPath(config, manifest, manifest.main)
        val existingElsewhere = findExistingCopyElsewhere(projectDir, config, manifest, target.relativePath)
        if (existingElsewhere != null) {
            error(
                "${manifest.name} already exists in this module at:\n" +
                    "    $existingElsewhere\n" +
                    "Installing to '${target.relativePath}' as well would create duplicate " +
                    "declarations in the same module and break the build (both directories " +
                    "compile). Delete the existing copy first, or re-run 'jetcompose init' " +
                    "so the configured source root matches where it already lives."
            )
        }

        log += "Installing ${manifest.name}"
        log += "  registry package : ${manifest.registryPackage}"
        log += "  new root package  : $newRoot"
        if (File(projectDir, target.relativePath).exists()) {
            log += "  (component already installed here — updating in place)"
        }

        for (relFile in manifest.files) {
            val originalContent = fetch("$componentName/$relFile")
            val rewritten = Rewriter.rewriteFileContent(originalContent, config, manifest)
            val target = Rewriter.resolveTargetPath(config, manifest, relFile)

            Rewriter.writeInstalledFile(projectDir, target.relativePath, rewritten)
            log += "  wrote: ${target.relativePath}"
        }

        return log
    }

    /**
     * Guards against the duplicate-install foot-gun: run init, install,
     * re-run init with a different source root (or package), install again
     * — and the same component now exists twice in one module, which is a
     * guaranteed "conflicting overloads" compile error because Gradle
     * compiles every source root (src/main/java AND src/main/kotlin both
     * count). Scans the module's src/ tree for this component's
     * category/style/main-file path landing anywhere other than the
     * current target.
     */
    private fun findExistingCopyElsewhere(
        projectDir: File,
        config: JetComposeConfig,
        manifest: ComponentManifest,
        targetRelativePath: String
    ): String? {
        val sourceRoot = config.activeSourceRoot()
        val moduleRel = sourceRoot.substringBefore("/src/", missingDelimiterValue = "")
        if (moduleRel.isEmpty()) return null
        val srcDir = File(projectDir, "$moduleRel/src")
        if (!srcDir.isDirectory) return null

        val mainFileName = manifest.main.substringAfterLast('/')
        val suffix = "${manifest.category}/${manifest.style}/$mainFileName"
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
