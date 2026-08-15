package com.lamintra.cli

data class ComponentManifest(
    /**
     * The component's flat slug, and the only identifier it has: what a user
     * types (`lamintra add text-field`), the registry directory name, and -
     * once mapped through [packageSegment] - the package it installs under.
     *
     * Flat rather than the old `<category>/<style>`, matching every component
     * registry that has actually reached this scale: shadcn/ui installs
     * `button`, Magic UI `shimmer-button`, Aceternity (200+ components)
     * `magnetic-button`. shadcn's registry spec explicitly rejects nested
     * paths in an item name. A path in the identifier forces an invented
     * style word onto components that only ever have one design, and leaves
     * nowhere sensible to put the ones that are not a variant of anything.
     */
    val name: String,
    /**
     * Grouping for the website, and deliberately NOT part of [name].
     *
     * Decoupling these is what lets names stay short: Aceternity serves a
     * /categories/button browse page while the install slug is
     * `magnetic-button`. Optional - an uncategorised component still installs
     * correctly, it just has no home on the showcase page.
     */
    val categories: List<String>,
    val registryPackage: String,
    val main: String,
    val prefix: String,
    val files: List<String>,
    // Optional @Preview demo file. Routed differently from `files`: it uses
    // the androidx preview annotation, so it installs to the ANDROID source
    // root (androidMain for KMP - the annotation doesn't exist in common
    // code), and only when the module's build file shows the ui-tooling
    // dependency actually exists.
    val preview: String? = null,
    /**
     * Other registry components this one needs on disk to compile.
     *
     * Added 2026-08-11 for the shared theme. Until then every component was an
     * island: two or three files that imported nothing but Compose, so the
     * installer only ever had to rewrite ONE package prefix. `button` now
     * imports `com.lamintra.theme`, and without this field that import would
     * land in a user's project still pointing at our namespace and fail to
     * compile - the exact outcome this product exists to prevent.
     *
     * Names are slugs, same as [name]. Requirements install first, and a
     * requirement may itself require others.
     */
    val requires: List<String> = emptyList()
) {
    /**
     * [name] as a legal Kotlin package segment.
     *
     * Hyphens are the convention across every registry a Compose developer
     * reads, and they are illegal in a Kotlin package. This is the one place
     * the two worlds meet, so the slug is *mapped* rather than restricted -
     * `lamintra add text_field` would look amateurish next to
     * `npx shadcn add text-field`, and the mapping costs one line.
     */
    val packageSegment: String get() = name.replace('-', '_')

    companion object {
        /**
         * Lowercase kebab-case: starts with a letter, no leading/trailing or
         * doubled hyphens. Anything else either cannot become a package
         * segment or produces one that reads badly.
         */
        private val SLUG = Regex("^[a-z][a-z0-9]*(-[a-z0-9]+)*$")

        /**
         * Kotlin's hard keywords - the ones that can never be an identifier,
         * so a package segment matching any of them is a guaranteed compile
         * error in every project that installs the component. Soft and
         * modifier keywords are fine as package segments and are not listed.
         */
        private val HARD_KEYWORDS = setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for",
            "fun", "if", "in", "interface", "is", "null", "object", "package",
            "return", "super", "this", "throw", "true", "try", "typealias",
            "typeof", "val", "var", "when", "while"
        )

        fun fromJson(json: JsonValue): ComponentManifest {
            val manifest = ComponentManifest(
                name = json["name"]!!.asString(),
                categories = json["categories"]?.asStringList() ?: emptyList(),
                registryPackage = json["registryPackage"]!!.asString(),
                main = json["main"]!!.asString(),
                prefix = json["prefix"]!!.asString(),
                files = json["files"]!!.asStringList(),
                preview = json["preview"]?.asStringOrNull(),
                requires = json["requires"]?.asStringList() ?: emptyList()
            )
            validate(manifest)
            return manifest
        }

        /**
         * Rejects a manifest whose name cannot survive becoming a package
         * segment, at load time rather than at the user's next compile.
         *
         * The failure this prevents is not hypothetical: the name is joined
         * verbatim into the target package, so an illegal segment produces a
         * component that installs cleanly and then fails to compile - the one
         * outcome this whole product exists to prevent.
         */
        private fun validate(manifest: ComponentManifest) {
            require(SLUG.matches(manifest.name)) {
                "Invalid component name \"${manifest.name}\". Names must be lowercase " +
                    "kebab-case (letters, digits and single hyphens, starting with a " +
                    "letter), e.g. \"button\" or \"text-field\"."
            }
            manifest.requires.forEach { required ->
                require(SLUG.matches(required)) {
                    "Component \"${manifest.name}\" requires \"$required\", which is not a " +
                        "valid component name. Requirements become package segments in the " +
                        "user's project, so they follow the same rule as names."
                }
            }
            require(manifest.packageSegment !in HARD_KEYWORDS) {
                "Invalid component name \"${manifest.name}\": it becomes the package " +
                    "segment \"${manifest.packageSegment}\", which is a reserved Kotlin " +
                    "keyword and cannot appear in a package declaration."
            }
        }

        fun parse(text: String): ComponentManifest = fromJson(MiniJson.parse(text))
    }
}
