package com.lamintra.cli

import java.io.File

fun main(args: Array<String>) {
    val projectDir = File(".").absoluteFile.normalize()

    when (args.getOrNull(0)) {
        "init" -> InitCommand.run(projectDir)

        "add" -> {
            val componentName = args.getOrNull(1)
            if (componentName == null) {
                System.err.println("Usage: lamintra add <category>/<style>")
                System.err.println("Example: lamintra add bottomsheet/glass")
                kotlin.system.exitProcess(1)
            }
            try {
                val config = LamintraConfig.load(projectDir)
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
        lamintra — copy-paste UI components for Jetpack Compose & KMP

        Usage:
          lamintra init              Set up this project (run once)
          lamintra add <component>   Install a component, e.g. bottomsheet/glass
        """.trimIndent()
    )
}
