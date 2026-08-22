plugins {
    kotlin("jvm") version "2.1.0"
    application
    // Plugin maintainership moved from com.github.johnrengelman.shadow to
    // com.gradleup.shadow (GradleUp org) - verified July 2026. Check
    // https://plugins.gradle.org/plugin/com.gradleup.shadow for the
    // current version before your first real build; plugin versions
    // move faster than any static doc can track.
    id("com.gradleup.shadow") version "9.5.1"
}

// One version, and the jar filename derives from it.
//
// It was a literal inside tasks.shadowJar until 2026-08-16, which made a
// mislabeled release a single forgotten edit away: tagging v0.5.1 while that
// literal still said 0.5.0 produces a release named for one version
// containing a jar named for another. The site derives its install command
// from the release, so the copy button would have handed every visitor a
// filename that 404s - the same failure shape as the lamintra.jar bug, which
// no automated check caught either.
version = "0.9.0"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
}

kotlin {
    // 17, not 21: Gradle itself needs JVM 17+, so every Android/KMP dev
    // already has it - a 21-target jar would fail with
    // UnsupportedClassVersionError on the many machines still running 17.
    jvmToolchain(17)
}

application {
    mainClass.set("com.lamintra.cli.MainKt")
}

// Deliberately NOT adding kotlinx.serialization, clikt, or any other
// dependency here - MiniJson.kt and plain args parsing cover everything
// V1 needs. Every dependency is one more thing that can break a user's
// first build; zero-dependency was a design decision, not an oversight.

tasks.shadowJar {
    archiveBaseName.set("lamintra")
    archiveClassifier.set("")
    archiveVersion.set(project.version.toString())

    // Stamps the version into the jar so `lamintra --version` can report it.
    // Read at runtime via Package.getImplementationVersion() rather than
    // duplicated as a Kotlin constant, because one version held in two places
    // is exactly how a release once shipped a jar named for the wrong one.
    manifest {
        attributes(
            "Implementation-Title" to "lamintra",
            "Implementation-Version" to project.version.toString()
        )
    }
}

tasks.test {
    useJUnitPlatform()

    // Point the registry at the working tree for tests. Two reasons, and the
    // second is the important one: the suite never touches the network, and
    // the scaffold tests exercise the ACTUAL files in ../registry, so a
    // malformed scaffold.json or a stray {{TOKEN}} fails here rather than on
    // a user's machine. Same override users have for testing an install.
    environment("LAMINTRA_REGISTRY", projectDir.parentFile.resolve("registry").absolutePath)
}
