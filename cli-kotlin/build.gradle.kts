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
    archiveVersion.set("0.5.0")
}

tasks.test {
    useJUnitPlatform()
}
