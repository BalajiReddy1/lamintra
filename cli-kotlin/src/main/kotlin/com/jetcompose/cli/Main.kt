package com.jetcompose.cli

import java.io.File

fun main(args: Array<String>) {
    val projectDir = File(".").absoluteFile.normalize()

    when (args.getOrNull(0)) {
        "init" -> InitCommand.run(projectDir)

        "add" -> {
            val componentName = args.getOrNull(1)
            if (componentName == null) {
                System.err.println("Usage: jetcompose add <category>/<style>")
                System.err.println("Example: jetcompose add bottomsheet/glass")
                kotlin.system.exitProcess(1)
            }
            try {
                val config = JetComposeConfig.load(projectDir)
                val log = Installer.install(componentName, projectDir, config)
                log.forEach(::println)
                println("\n✅ Installed $componentName with zero manual fixes needed.")
            } catch (e: Exception) {
                System.err.println("❌ ${e.message}")
                kotlin.system.exitProcess(1)
            }
        }

        else -> printHelp()
    }
}

private fun printHelp() {
    println(
        """
        jetcompose — copy-paste UI components for Jetpack Compose & KMP

        Usage:
          jetcompose init              Set up this project (run once)
          jetcompose add <component>   Install a component, e.g. bottomsheet/glass
        """.trimIndent()
    )
}
