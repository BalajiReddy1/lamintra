package com.jetcompose.cli

data class ComponentManifest(
    val name: String,
    val category: String,
    val style: String,
    val registryPackage: String,
    val main: String,
    val prefix: String,
    val files: List<String>
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
                files = json["files"]!!.asStringList()
            )
        }

        fun parse(text: String): ComponentManifest = fromJson(MiniJson.parse(text))
    }
}
