package com.jetcompose.cli

import java.io.File

data class SourceRoots(
    val common: String? = null,
    val android: String? = null,
    val ios: String? = null
)

data class JetComposeConfig(
    val packageName: String,
    val isKmp: Boolean,
    val sourceRoots: SourceRoots,
    val componentPath: String
) {
    /**
     * Which source root a file should land under. KMP projects install
     * shared UI into commonMain; Android-only projects use the android
     * root. This single decision point is what lets the same component
     * work correctly whether or not the target project is KMP.
     */
    fun activeSourceRoot(): String {
        val root = if (isKmp) sourceRoots.common else sourceRoots.android
        return root ?: error(
            "Config is missing sourceRoots.${if (isKmp) "common" else "android"} — " +
                "run 'jetcompose init' again or edit .jetcompose/config.json directly."
        )
    }

    companion object {
        fun fromJson(json: JsonValue): JetComposeConfig {
            val sourceRootsJson = json["sourceRoots"]
            val sourceRoots = SourceRoots(
                common = sourceRootsJson?.get("common")?.asStringOrNull(),
                android = sourceRootsJson?.get("android")?.asStringOrNull(),
                ios = sourceRootsJson?.get("ios")?.asStringOrNull()
            )
            return JetComposeConfig(
                packageName = json["packageName"]!!.asString(),
                isKmp = json["isKmp"]?.asBool(false) ?: false,
                sourceRoots = sourceRoots,
                componentPath = json["componentPath"]?.asStringOrNull() ?: ""
            )
        }

        fun load(projectDir: File): JetComposeConfig {
            val configFile = File(projectDir, ".jetcompose/config.json")
            if (!configFile.exists()) {
                error(
                    "No .jetcompose/config.json found in ${projectDir.absolutePath}.\n" +
                        "Run 'jetcompose init' first."
                )
            }
            val json = MiniJson.parse(configFile.readText())
            return fromJson(json)
        }
    }
}
