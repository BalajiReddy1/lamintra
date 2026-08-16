package com.lamintra.cli

import java.io.File

fun main(args: Array<String>) {
    val projectDir = File(".").absoluteFile.normalize()

    when (args.getOrNull(0)) {
        "init" -> InitCommand.run(projectDir)

        "add" -> {
            val componentName = args.getOrNull(1)
            if (componentName == null) {
                System.err.println("Usage: lamintra add <component>")
                System.err.println("Example: lamintra add button")
                kotlin.system.exitProcess(1)
            }
            try {
                val config = LamintraConfig.load(projectDir)
                val log = Installer.install(componentName, projectDir, config)
                log.forEach(::println)
                println("\nInstalled $componentName with zero manual fixes needed.")
            } catch (e: Exception) {
                System.err.println("Error: ${e.message}")
                kotlin.system.exitProcess(1)
            }
        }

        "scaffold" -> {
            val scaffoldName = args.getOrNull(1)
            if (scaffoldName == null) {
                System.err.println("Usage: lamintra scaffold <name> [--force]")
                System.err.println("Example: lamintra scaffold ios-shell")
                kotlin.system.exitProcess(1)
            }
            try {
                ScaffoldCommand.run(
                    scaffoldName = scaffoldName,
                    projectDir = projectDir,
                    force = args.contains("--force")
                )
            } catch (e: Exception) {
                System.err.println("Error: ${e.message}")
                kotlin.system.exitProcess(1)
            }
        }

        else -> printHelp()
    }
}

private fun printHelp() {
    println(
        """
        lamintra - copy-paste UI components for Jetpack Compose & KMP

        Usage:
          lamintra init              Set up this project (run once)
          lamintra add <component>   Install a component, e.g. button, text-field
          lamintra scaffold <name>   Write project structure, e.g. ios-shell

        Scaffolds are project wiring rather than components: they write Swift
        and Kotlin outside the component tree and are installed once. Pass
        --force to overwrite files that already exist.
        """.trimIndent()
    )
}
