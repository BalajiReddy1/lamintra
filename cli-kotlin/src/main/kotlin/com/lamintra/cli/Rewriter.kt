package com.lamintra.cli

import java.io.File

data class ResolvedTarget(
    val fullPackage: String,
    val relativePath: String,
    val filename: String
)

/**
 * Originally a direct port of a day-1 JavaScript prototype, which validated
 * the rewrite algorithm against real fixtures before any Kotlin existed. That
 * prototype has been removed from the repository; the properties it proved are
 * now covered by `RewriterTest.kt` and by the rules below:
 *   - boundary-safe rewriting (doesn't corrupt "glassy" or "glass2" when
 *     rewriting "glass")
 *   - cross-file internal imports resolving correctly
 *   - two components sharing a filename landing at different paths with
 *     different packages, no collision
 *   - every installed file's path exactly matching its own package
 *     declaration (the specific rule Kotlin enforces at compile time)
 *
 * The paragraph that stood here until 2026-08-21 said this Kotlin had never
 * been compiled and should be run "as your first step on your own machine".
 * That was true on day one and has been false since the first release.
 */
object Rewriter {

    /**
     * Boundary-safe replacement of [oldRoot] with [newRoot] anywhere it
     * appears in [content] as a complete package-path token - not as a
     * prefix of some longer, unrelated identifier.
     *
     * Example: rewriting "com.lamintra.glass_sheet" must NOT touch
     * "com.lamintra.glass_sheety.Something" (a different, unrelated package
     * that merely starts with the same characters).
     */
    fun rewriteRootPackage(content: String, oldRoot: String, newRoot: String): String {
        val escaped = Regex.escape(oldRoot)
        val boundarySafe = Regex("$escaped(?![A-Za-z0-9_])")
        return boundarySafe.replace(content) { newRoot }
    }

    /**
     * Computes the new root package a component will live under once
     * installed. Used by BOTH content rewriting and path resolution -
     * sharing this single function guarantees a written file's path can
     * never disagree with its own package declaration, which is itself
     * a guaranteed Kotlin compile error if it ever happened.
     */
    fun computeNewRootPackage(config: LamintraConfig, componentSegment: String): String {
        val componentPathDotted = config.componentPath
            .split("/")
            .filter { it.isNotBlank() }
            .joinToString(".")
        val parts = mutableListOf(config.packageName)
        if (componentPathDotted.isNotEmpty()) parts.add(componentPathDotted)
        parts.add(componentSegment)
        return parts.joinToString(".")
    }

    /**
     * Resolves where a single manifest-declared file should land on disk,
     * and what package it should declare once there.
     *
     * Throws if a component's internal/ folder name doesn't match its
     * own declared `prefix` - this is a real bug class we want to catch
     * at install time, not silently ship a package/path mismatch.
     */
    fun resolveTargetPath(
        config: LamintraConfig,
        manifest: ComponentManifest,
        manifestRelativeFilePath: String
    ): ResolvedTarget {
        val sourceRoot = config.activeSourceRoot()

        val withoutSrc = manifestRelativeFilePath.removePrefix("src/")
        val segments = withoutSrc.split("/").toMutableList()
        val filename = segments.removeAt(segments.size - 1)

        val newRoot = computeNewRootPackage(config, manifest.packageSegment)

        val fullPackage: String = if (segments.getOrNull(0) == "internal") {
            val declaredPrefix = segments.getOrNull(1)
            require(declaredPrefix == manifest.prefix) {
                "Manifest prefix mismatch: manifest declares prefix \"${manifest.prefix}\" " +
                    "but file path uses \"internal/$declaredPrefix/\". These must match exactly."
            }
            val extraSegments = segments.drop(2)
            (listOf(newRoot, "internal", manifest.prefix) + extraSegments).joinToString(".")
        } else {
            (listOf(newRoot) + segments).joinToString(".")
        }

        val packageAsPath = fullPackage.replace(".", "/")
        val relativePath = listOf(sourceRoot, packageAsPath, filename)
            .joinToString("/")
            .replace("//", "/")

        return ResolvedTarget(fullPackage, relativePath, filename)
    }

    /**
     * Rewrites one file's content: replaces the component's registry
     * root package with its new, project-specific root, wherever it
     * appears (the file's own package line, plus any cross-references to
     * sibling internal files that share the same root).
     *
     * AND the same for every package the component declares in `requires`.
     * This second half was added 2026-08-11 with the shared theme. Before it,
     * a component that imported another component's package installed a file
     * still importing `com.lamintra.theme`, which does not exist in the user's
     * project. It compiled here and failed there, which is the worst shape a
     * bug can have.
     *
     * The registry's own namespace is derived from the manifest rather than
     * hard-coded: `com.lamintra.button` minus its last segment gives the
     * prefix every sibling shares.
     */
    fun rewriteFileContent(
        originalContent: String,
        config: LamintraConfig,
        manifest: ComponentManifest
    ): String {
        var content = rewriteRootPackage(
            originalContent,
            manifest.registryPackage,
            computeNewRootPackage(config, manifest.packageSegment)
        )
        val registryNamespace = manifest.registryPackage.substringBeforeLast('.')
        for (required in manifest.requires) {
            val segment = required.replace('-', '_')
            content = rewriteRootPackage(
                content,
                "$registryNamespace.$segment",
                computeNewRootPackage(config, segment)
            )
        }
        return content
    }

    /**
     * Writes [content] to [relativePath] under [projectDir], creating parent
     * dirs as needed.
     *
     * [relativePath] is derived from registry-supplied manifest fields
     * (`name`, `prefix`, `files`), so it is untrusted input: a
     * manifest containing `..` segments or an absolute path would otherwise
     * write anywhere on the user's disk. The resolved target is therefore
     * required to stay inside [projectDir]. This matters most for the
     * planned third-party/private registries, where manifests come from
     * someone other than us - but it costs nothing to enforce now.
     */
    fun writeInstalledFile(projectDir: File, relativePath: String, content: String) {
        val target = resolveSafeTarget(projectDir, relativePath)
        target.parentFile.mkdirs()
        target.writeText(content)
    }

    /**
     * Resolves [relativePath] against [projectDir] and fails if it escapes.
     *
     * Resolves `..`, mixed separators, symlinks AND Windows directory
     * junctions, so none of them can smuggle a path past a plain string check.
     *
     * **Why `toRealPath` and not `canonicalFile`.** This used `canonicalFile`
     * until 2026-08-21, and on Windows `File.getCanonicalPath` does not
     * traverse a directory junction. A junction placed inside a project and
     * pointing outside it therefore canonicalised to a path that still looked
     * contained, the containment check passed, and the OS followed the link
     * when the bytes were written - four files landed outside the project
     * while the tool printed paths that looked innocent and reported success.
     * `Path.toRealPath` resolves reparse points and is the correct instrument.
     *
     * The target does not exist yet, so the real path of its deepest existing
     * ancestor is resolved instead and the remaining segments appended. That is
     * what closes the hole: every directory that could be a link is on the
     * ancestor side.
     */
    internal fun resolveSafeTarget(projectDir: File, relativePath: String): File {
        require(!File(relativePath).isAbsolute) {
            "Refusing to install \"$relativePath\": absolute paths are not allowed."
        }
        val root = projectDir.toPath().toRealPath()
        val requested = root.resolve(relativePath).normalize()

        // Walk up to the deepest part of the path that exists on disk, resolve
        // links there, then re-attach what does not exist yet.
        var existing = requested
        while (!java.nio.file.Files.exists(existing)) {
            existing = existing.parent
                ?: error("Refusing to install \"$relativePath\": it has no resolvable parent.")
        }
        val resolved = existing.toRealPath().resolve(existing.relativize(requested)).normalize()

        require(resolved.startsWith(root) && resolved != root) {
            "Refusing to install \"$relativePath\": it resolves outside the project " +
                "directory ($resolved). This usually means a malformed or " +
                "untrusted component manifest, or a symlink inside the project " +
                "that points out of it."
        }
        return resolved.toFile()
    }
}
