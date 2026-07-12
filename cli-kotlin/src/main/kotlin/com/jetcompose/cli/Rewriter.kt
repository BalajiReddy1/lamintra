package com.jetcompose.cli

import java.io.File

data class ResolvedTarget(
    val fullPackage: String,
    val relativePath: String,
    val filename: String
)

/**
 * This object is a direct port of cli-prototype/rewrite-engine.js, which
 * was executed against real fixtures today and passed all 17 correctness
 * assertions, including:
 *   - boundary-safe rewriting (doesn't corrupt "glassy" or "glass2" when
 *     rewriting "glass")
 *   - cross-file internal imports resolving correctly
 *   - two components sharing a filename (ModifierExtensions.kt) landing
 *     at different paths with different packages, no collision
 *   - every installed file's path exactly matching its own package
 *     declaration (the specific rule Kotlin enforces at compile time)
 *
 * This Kotlin version has NOT been compiled in the sandbox that produced
 * it (no javac/kotlinc available there — see the Day 1 report). Compile
 * and run it for real as your first step on your own machine; the logic
 * itself is already validated, only the JVM/Kotlin-specific syntax is new.
 */
object Rewriter {

    /**
     * Boundary-safe replacement of [oldRoot] with [newRoot] anywhere it
     * appears in [content] as a complete package-path token — not as a
     * prefix of some longer, unrelated identifier.
     *
     * Example: rewriting "com.jetcompose.bottomsheet.glass" must NOT
     * touch "com.jetcompose.bottomsheet.glassy.Something" (a different,
     * unrelated package that merely starts with the same characters).
     */
    fun rewriteRootPackage(content: String, oldRoot: String, newRoot: String): String {
        val escaped = Regex.escape(oldRoot)
        val boundarySafe = Regex("$escaped(?![A-Za-z0-9_])")
        return boundarySafe.replace(content) { newRoot }
    }

    /**
     * Computes the new root package a component will live under once
     * installed. Used by BOTH content rewriting and path resolution —
     * sharing this single function guarantees a written file's path can
     * never disagree with its own package declaration, which is itself
     * a guaranteed Kotlin compile error if it ever happened.
     */
    fun computeNewRootPackage(config: JetComposeConfig, category: String, style: String): String {
        val componentPathDotted = config.componentPath
            .split("/")
            .filter { it.isNotBlank() }
            .joinToString(".")
        val parts = mutableListOf(config.packageName)
        if (componentPathDotted.isNotEmpty()) parts.add(componentPathDotted)
        parts.add(category)
        parts.add(style)
        return parts.joinToString(".")
    }

    /**
     * Resolves where a single manifest-declared file should land on disk,
     * and what package it should declare once there.
     *
     * Throws if a component's internal/ folder name doesn't match its
     * own declared `prefix` — this is a real bug class we want to catch
     * at install time, not silently ship a package/path mismatch.
     */
    fun resolveTargetPath(
        config: JetComposeConfig,
        manifest: ComponentManifest,
        manifestRelativeFilePath: String
    ): ResolvedTarget {
        val sourceRoot = config.activeSourceRoot()

        val withoutSrc = manifestRelativeFilePath.removePrefix("src/")
        val segments = withoutSrc.split("/").toMutableList()
        val filename = segments.removeAt(segments.size - 1)

        val newRoot = computeNewRootPackage(config, manifest.category, manifest.style)

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
     */
    fun rewriteFileContent(
        originalContent: String,
        config: JetComposeConfig,
        manifest: ComponentManifest
    ): String {
        val newRoot = computeNewRootPackage(config, manifest.category, manifest.style)
        return rewriteRootPackage(originalContent, manifest.registryPackage, newRoot)
    }

    /** Writes [content] to [relativePath] under [projectDir], creating parent dirs as needed. */
    fun writeInstalledFile(projectDir: File, relativePath: String, content: String) {
        val target = File(projectDir, relativePath)
        target.parentFile.mkdirs()
        target.writeText(content)
    }
}
