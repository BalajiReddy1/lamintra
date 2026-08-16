package com.lamintra.cli

/**
 * A scaffold is project structure, not a component, and the difference is
 * load-bearing rather than pedantic.
 *
 * A component is Kotlin that lands inside a source root, under the user's
 * package, with `com.lamintra.<slug>` rewritten to match. Every one of those
 * assumptions is false for an iOS shell: it is Swift as well as Kotlin, part
 * of it lands in `iosApp/` where no Gradle source root reaches, Swift has no
 * package declaration to rewrite, and it is installed once per project rather
 * than once per component.
 *
 * Running it through [ComponentManifest] would have meant teaching Rewriter a
 * Swift mode and teaching `resolveTargetPath` to escape the source root - and
 * the website would then try to parse a props table out of a `.swift` file.
 * The registry's guarantee is that every component is standalone Kotlin that
 * compiles where it lands. This keeps that true by not pretending.
 *
 * What IS shared is transport: scaffolds ship on the same registry tag, so a
 * fix to a Swift template does not need a CLI release.
 */
data class ScaffoldFile(
    /** Path within the scaffold's registry directory. */
    val from: String,
    /**
     * Destination, as `<root>:<path>`. The root keyword is resolved by the
     * CLI rather than the manifest, so a registry cannot name an arbitrary
     * location on disk:
     *
     *  - `swift:`  -> the Xcode app directory, project-relative. Outside every
     *                 Gradle source root, which is exactly why it needs its own
     *                 keyword.
     *  - `ios:`    -> `sourceRoots.ios`, under the user's package.
     *  - `common:` -> `sourceRoots.common`, under the user's package.
     */
    val to: String
) {
    val rootKeyword: String get() = to.substringBefore(':', missingDelimiterValue = "")
    val destPath: String get() = to.substringAfter(':')
}

data class ScaffoldManifest(
    val name: String,
    val title: String,
    /** Refuse to run against an Android-only project rather than writing junk. */
    val requiresKmp: Boolean,
    val files: List<ScaffoldFile>,
    /** Printed verbatim after a successful run. The manual steps we cannot do. */
    val postInstall: List<String>
) {
    companion object {
        private val ROOTS = setOf("swift", "ios", "common")

        fun parse(text: String): ScaffoldManifest = fromJson(MiniJson.parse(text))

        fun fromJson(json: JsonValue): ScaffoldManifest {
            val fileItems = (json["files"] as? JsonValue.JsonArray)?.items
                ?: error("Scaffold manifest is missing a \"files\" array.")
            val manifest = ScaffoldManifest(
                name = json["name"]!!.asString(),
                title = json["title"]?.asStringOrNull() ?: json["name"]!!.asString(),
                requiresKmp = json["requiresKmp"]?.asBool(true) ?: true,
                files = fileItems.map {
                    ScaffoldFile(
                        from = it["from"]!!.asString(),
                        to = it["to"]!!.asString()
                    )
                },
                postInstall = (json["postInstall"] as? JsonValue.JsonArray)
                    ?.items?.map { it.asString() } ?: emptyList()
            )
            validate(manifest)
            return manifest
        }

        private fun validate(manifest: ScaffoldManifest) {
            require(manifest.files.isNotEmpty()) {
                "Scaffold \"${manifest.name}\" declares no files."
            }
            manifest.files.forEach { file ->
                require(file.rootKeyword in ROOTS) {
                    "Scaffold \"${manifest.name}\" sends \"${file.from}\" to " +
                        "\"${file.to}\", but \"${file.rootKeyword}\" is not a known " +
                        "destination root. Use one of: ${ROOTS.sorted().joinToString(", ")}."
                }
                require(file.destPath.isNotBlank()) {
                    "Scaffold \"${manifest.name}\" declares an empty destination for " +
                        "\"${file.from}\"."
                }
            }
        }
    }
}
