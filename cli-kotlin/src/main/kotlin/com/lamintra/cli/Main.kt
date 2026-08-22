package com.lamintra.cli

import java.io.File

/**
 * The version of this build, read from the jar manifest, which
 * `build.gradle.kts` stamps from the single `version` property.
 *
 * There was no version output at all until 2026-08-21. A bug report could not
 * say which build produced it, and because every release hard-pins a registry
 * tag, the jar version silently decides which component source a user
 * receives - so "I downloaded it recently" was the best anyone could offer.
 *
 * Read from the manifest rather than duplicated as a constant here, because two
 * places holding one version is how the 0.5.0-named-jar problem happened.
 */
val VERSION: String =
    Main::class.java.`package`?.implementationVersion ?: "unknown (development build)"

private object Main

fun main(args: Array<String>) {
    val projectDir = File(".").absoluteFile.normalize()

    when (val command = args.getOrNull(0)) {
        "init" -> runOrFail("init") { InitCommand.run(projectDir) }

        "add" -> {
            val componentName = args.getOrNull(1)?.takeIf { !it.startsWith("-") }
            if (componentName == null) {
                System.err.println("Usage: lamintra add <component> [--force]")
                System.err.println("Example: lamintra add button")
                kotlin.system.exitProcess(1)
            }
            runOrFail("add $componentName") {
                val config = LamintraConfig.load(projectDir)
                val log = Installer.install(
                    componentName = componentName,
                    projectDir = projectDir,
                    config = config,
                    force = args.contains("--force")
                )
                log.forEach(::println)
                println("\nInstalled $componentName with zero manual fixes needed.")
            }
        }

        "scaffold" -> {
            val scaffoldName = args.getOrNull(1)?.takeIf { !it.startsWith("-") }
            if (scaffoldName == null) {
                System.err.println("Usage: lamintra scaffold <name> [--force]")
                System.err.println("Example: lamintra scaffold ios-shell")
                kotlin.system.exitProcess(1)
            }
            runOrFail("scaffold $scaffoldName") {
                ScaffoldCommand.run(
                    scaffoldName = scaffoldName,
                    projectDir = projectDir,
                    force = args.contains("--force")
                )
            }
        }

        "--version", "-v", "version" -> println("lamintra $VERSION")

        "--help", "-h", "help", null -> printHelp()

        // Anything else exited 0 with the help text until 2026-08-21, so a
        // typo'd command succeeded from a script's point of view. An unknown
        // command is a failure and now says so.
        else -> {
            System.err.println("Unknown command: $command")
            System.err.println()
            printHelp(System.err)
            kotlin.system.exitProcess(1)
        }
    }
}

/**
 * Runs a command and turns anything it throws into one readable line.
 *
 * Catches `Throwable`, not `Exception`. That distinction is the whole point:
 * `StackOverflowError` from deeply nested JSON is an `Error`, so until
 * 2026-08-21 it escaped entirely and printed 1,023 stack frames at the user.
 *
 * [context] is what the tool was doing. Without it a failed write printed a
 * bare filesystem path and "(Access is denied)", which does not tell the reader
 * that an install was abandoned partway.
 */
private inline fun runOrFail(context: String, block: () -> Unit) {
    try {
        block()
    } catch (t: Throwable) {
        System.err.println("Error during '$context': ${describe(t)}")
        kotlin.system.exitProcess(1)
    }
}

/**
 * A message for a throwable, never the string "null".
 *
 * `e.message` was printed directly until 2026-08-21. A JDK `IOException` from
 * an unreachable host usually carries no message, so an offline user was told
 * exactly `Error: null`.
 */
private fun describe(t: Throwable): String {
    // Walk the cause chain for the first real message. Some wrappers carry
    // none of their own - ExceptionInInitializerError is the one that caught
    // us out - so the useful text is one level down.
    var current: Throwable? = t
    while (current != null) {
        current.message?.takeIf { it.isNotBlank() }?.let { return it }
        current = current.cause
    }
    return when (t) {
        is StackOverflowError ->
            "the file is nested too deeply to parse, which usually means it is corrupted."
        is OutOfMemoryError ->
            "ran out of memory reading a response from the registry."
        else ->
            "${t::class.java.simpleName} with no further detail. " +
                "Please report this at https://github.com/BalajiReddy1/lamintra/issues."
    }
}

private fun printHelp(out: java.io.PrintStream = System.out) {
    out.println(
        """
        lamintra $VERSION - copy-paste UI components for Jetpack Compose & KMP

        Usage:
          lamintra init              Set up this project (run once)
          lamintra add <component>   Install a component, e.g. button, text-field
          lamintra scaffold <name>   Write project structure, e.g. ios-shell
          lamintra --version         Print the version and exit

        Options:
          --force                    Overwrite files you have edited. Without it,
                                     'add' refuses to replace a changed file and
                                     tells you which one.

        Scaffolds are project wiring rather than components: they write Swift
        and Kotlin outside the component tree and are installed once.
        """.trimIndent()
    )
}
