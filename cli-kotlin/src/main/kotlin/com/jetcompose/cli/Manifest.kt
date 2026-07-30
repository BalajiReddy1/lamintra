package com.jetcompose.cli

data class ComponentManifest(
    val name: String,
    val category: String,
    val style: String,
    val registryPackage: String,
    val main: String,
    val prefix: String,
    val files: List<String>,
    // Optional @Preview demo file. Routed differently from `files`: it uses
    // the androidx preview annotation, so it installs to the ANDROID source
    // root (androidMain for KMP — the annotation doesn't exist in common
    // code), and only when the module's build file shows the ui-tooling
    // dependency actually exists.
    val preview: String? = null
) {
    companion object {
        fun fromJson(json: JsonValue): ComponentManifest {
            return ComponentManifest(
                name = json["name"]!!.asString(),
                category = json["category"]!!.asString(),
                style = json["style"]!!.asString(),
                registryPackage = json["registryPackage"]!!.asString(),
                main = json["main"]!!.asString(),
                prefix = json["prefix"]!!.asString(),
                files = json["files"]!!.asStringList(),
                preview = json["preview"]?.asStringOrNull()
            )
        }

        fun parse(text: String): ComponentManifest = fromJson(MiniJson.parse(text))
    }
}
