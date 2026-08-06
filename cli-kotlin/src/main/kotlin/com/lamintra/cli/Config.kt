package com.lamintra.cli

import java.io.File

data class SourceRoots(
    val common: String? = null,
    val android: String? = null,
    val ios: String? = null
)

data class LamintraConfig(
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
                "run 'lamintra init' again or edit .lamintra/config.json directly."
        )
    }

    companion object {
        fun fromJson(json: JsonValue): LamintraConfig {
            val sourceRootsJson = json["sourceRoots"]
            val sourceRoots = SourceRoots(
                common = sourceRootsJson?.get("common")?.asStringOrNull(),
                android = sourceRootsJson?.get("android")?.asStringOrNull(),
                ios = sourceRootsJson?.get("ios")?.asStringOrNull()
            )
            return LamintraConfig(
                packageName = json["packageName"]!!.asString(),
                isKmp = json["isKmp"]?.asBool(false) ?: false,
                sourceRoots = sourceRoots,
                componentPath = json["componentPath"]?.asStringOrNull() ?: ""
            )
        }

        fun load(projectDir: File): LamintraConfig {
            val configFile = File(projectDir, ".lamintra/config.json")
            // Back-compat: projects initialised before the rename have a
            // .jetcompose/ directory. Read it rather than making an existing
            // user re-run init — their config is still valid, only the folder
            // name changed. Written configs always use the new path.
            val legacyFile = File(projectDir, ".jetcompose/config.json")
            val source = when {
                configFile.exists() -> configFile
                legacyFile.exists() -> legacyFile
                else -> error(
                    "No .lamintra/config.json found in ${projectDir.absolutePath}.\n" +
                        "Run 'lamintra init' first."
                )
            }
            val json = MiniJson.parse(source.readText())
            return fromJson(json)
        }
    }
}
