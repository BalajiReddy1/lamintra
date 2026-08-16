package com.lamintra.cli

import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * Where registry content comes from, and the only thing that talks to the
 * network.
 *
 * Extracted from Installer on 2026-08-16, unchanged, when scaffolds arrived.
 * Fetching is registry transport rather than component installation, and two
 * callers now need it: `add` (components) and `scaffold` (project shells).
 * Leaving it on Installer would have made scaffolding depend on the component
 * installer for no reason other than where the code happened to be first.
 */
object Registry {
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
        "https://raw.githubusercontent.com/BalajiReddy1/lamintra-registry/v0.5.4"

    /**
     * Where content is read from. The published tag, unless overridden.
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
    val base: String =
        System.getenv("LAMINTRA_REGISTRY")?.takeIf { it.isNotBlank() } ?: PUBLISHED_REGISTRY

    private val client: HttpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    /** True when [base] points at a working tree rather than a published tag. */
    fun isLocal(): Boolean = !base.startsWith("http://") && !base.startsWith("https://")

    fun fetch(relativeUrl: String): String {
        if (isLocal()) {
            val local = File(base, relativeUrl)
            if (!local.isFile) {
                error(
                    "Not found in the local registry: ${local.path}. " +
                        "LAMINTRA_REGISTRY is set to '$base'. Unset it to use the " +
                        "published registry."
                )
            }
            return local.readText()
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$base/$relativeUrl"))
            .GET()
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            error(
                "Failed to fetch $relativeUrl (HTTP ${response.statusCode()}). " +
                    "Check the name is correct, e.g. 'button' or 'text-field'."
            )
        }
        return response.body()
    }
}
