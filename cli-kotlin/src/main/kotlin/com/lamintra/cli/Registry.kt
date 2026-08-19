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
        "https://raw.githubusercontent.com/BalajiReddy1/lamintra-registry/v0.8.0"

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

        var lastStatus = -1
        repeat(RETRIES) { attempt ->
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val status = response.statusCode()
            if (status == 200) return response.body()
            lastStatus = status
            if (!isTransient(status) || attempt == RETRIES - 1) {
                error(describeFailure(relativeUrl, status))
            }
            Thread.sleep(BACKOFF_MS shl attempt)
        }
        error(describeFailure(relativeUrl, lastStatus))
    }

    /**
     * How many times a transient failure is retried, and the first pause.
     *
     * Three attempts at 500ms then 1000ms: about 1.5s of waiting in the worst
     * case, which is short enough that a user does not think the tool has hung
     * and long enough to ride out the kind of wobble that produced this code.
     * `shl attempt` doubles it each time.
     */
    private const val RETRIES = 3
    private const val BACKOFF_MS = 500L

    /**
     * Whether a status is worth trying again.
     *
     * 429 is rate limiting and 5xx is the server having a bad time; both pass
     * on their own. A 404 never will, so retrying one only makes a typo take
     * three times as long to report.
     */
    private fun isTransient(status: Int): Boolean = status == 429 || status in 500..599

    /**
     * The message a user actually gets, chosen by status.
     *
     * This existed as one line for every status until 2026-08-17, and it told
     * the reader to check the component name whatever had gone wrong. On
     * 2026-08-17 raw.githubusercontent.com returned 429 across ALL repositories
     * during a GitHub partial outage, and the founder, who wrote this tool, was
     * told the name was wrong and duly tried a second correct name. A stranger
     * would have concluded the tool was broken and left.
     *
     * A 404 is the only status that means what the old message said.
     */
    private fun describeFailure(relativeUrl: String, status: Int): String = when {
        status == 404 ->
            "Not found in the registry: $relativeUrl (HTTP 404). Check the name is " +
                "correct, e.g. 'button' or 'text-field'. The full list is at " +
                "https://lamintra.com/components/."
        status == 429 ->
            "The registry is rate limiting us (HTTP 429), so this is temporary and " +
                "nothing is wrong with your project or the name you typed. Wait a " +
                "minute and run the same command again. If it persists, check " +
                "https://www.githubstatus.com - the registry is served by " +
                "raw.githubusercontent.com."
        status in 500..599 ->
            "The registry is unavailable right now (HTTP $status). This is a problem " +
                "at GitHub rather than with your project. Wait a minute and run the " +
                "same command again, or check https://www.githubstatus.com."
        else ->
            "Failed to fetch $relativeUrl (HTTP $status)."
    }
}
