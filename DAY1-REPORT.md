# JetCompose — Day 1 Engineering Report

> **HISTORICAL RECORD — 2026-07-10. Superseded; do not read as current
> state.** This was the repo's `README.md` until 2026-08-04. It is kept
> because it is the primary record of what the rewrite algorithm was
> verified against on Day 1, and that verification still stands.
> Everything it lists as *unverified* has since been resolved:
>
> | Day 1 said | Actual status now |
> |---|---|
> | Kotlin CLI has never been compiled | Compiles; 12/12 `RewriterTest.kt` tests |
> | Components never compiled against real Compose | Compile on Android + desktop; run on device |
> | `BottomSheet.kt` drag gesture is stubbed | Wired, fixed, hand-verified on device (registry v0.2.1) |
> | `Installer.kt` points at a placeholder registry URL | Points at the real registry, pinned to tag v0.3.1 |
> | No real Gradle project touched by the CLI | Verified on four testbeds and by two external testers |
>
> The "Day 2 checklist" at the end is complete. For current state, read
> [README.md](README.md) and `CLAUDE.md`.

## Sandbox constraints (stated as fact, not caveat-hedging)

This build environment has:
- ✅ Java 21 **JRE** (`java` works)
- ❌ No JDK — `javac` does not exist, confirmed by direct search of the filesystem
- ❌ No `kotlinc`
- ❌ No network access — confirmed by a direct request to `repo.maven.apache.org`,
  which was rejected by the sandbox's egress allowlist
- ✅ Node.js and Python 3 are present with zero install needed

**What this means:** the production Kotlin CLI in `cli-kotlin/` has **not been
compiled in this sandbox**. It cannot be — there's no compiler here. What
follows is exactly what was and wasn't verified today, with no blurring
between the two.

---

## What was actually built and executed today (real, not simulated)

### 1. The rewrite engine — validated in Node.js first, then ported to Kotlin

The single highest-risk piece of this whole project, identified and
re-confirmed across many rounds of planning, is the package/import rewrite
logic. Since no Kotlin/Java compiler is available here, the algorithm was
implemented in Node.js (available with zero install), executed for real
against realistic fixtures, and only ported to Kotlin *after* it passed.

Test fixtures built (real files, not descriptions of files):
- `registry/bottomsheet/glass/` — a full glassmorphism bottom sheet
  component: `BottomSheet.kt` + two internal dependencies (`DragHandle.kt`,
  `ModifierExtensions.kt`), using only `compose.foundation`, no Material 3.
- `registry/button/neon/` — a second, unrelated component that
  **deliberately ships its own file also named `ModifierExtensions.kt`**,
  specifically to stress-test namespace collision handling.
- `fake-target-project/.jetcompose/config.json` — a simulated real user
  project, deliberately configured with a **non-default** `componentPath`
  (`features/shared/widgets`) and KMP enabled, so the test exercises
  configurability rather than only the happy-path default.

Verification suite run (`cli-prototype/run-verification-suite.js` +
`verify-path-matches-package.js`) — **17 of 17 assertions passed**,
independently re-checked against the actual files written to disk
(not just the script's own self-reported log):

- Package declarations correctly rewritten to the target namespace
- Cross-file internal imports correctly rewritten (`BottomSheet.kt`'s
  imports of `DragHandle` and `glassSurface` both updated)
- Unrelated imports (`androidx.compose.*`, `kotlinx.coroutines.*`) left
  **untouched**
- **Boundary-safety proven**: a decoy import
  (`com.jetcompose.bottomsheet.glassy...`, note the trailing "y") was
  correctly left alone — a naive global string-replace would have
  corrupted it. Same for a decoy ending in a digit (`glass2`).
- **Collision test passed**: the two same-named `ModifierExtensions.kt`
  files from unrelated components landed at different paths with
  different package declarations, both existing simultaneously with no
  overwrite.
- **Path/package agreement proven for all 5 installed files**: Kotlin
  requires a file's physical location to exactly mirror its package
  declaration, or it won't compile, independent of anything else being
  correct. This was checked programmatically, file by file, not eyeballed.

One real bug was found and fixed during this process (a `process.cwd()`
misuse in the initial path-resolution draft that would have produced wrong
paths) — caught in review before the first test run, not left for you to
discover later.

### 2. Production Kotlin CLI — written, not yet compiled

`cli-kotlin/src/main/kotlin/com/jetcompose/cli/`:
- `MiniJson.kt` — zero-dependency JSON parser (deliberate: avoids pulling
  in `kotlinx.serialization` just to parse two small, flat schemas)
- `Config.kt`, `Manifest.kt` — data models + loaders
- `Rewriter.kt` — **direct port** of the validated JS logic, same function
  names, same boundary-safe regex approach, same shared
  `computeNewRootPackage` used by both content-rewriting and path
  resolution so they can't drift apart
- `Installer.kt` — fetches from a GitHub-raw-hosted registry using
  `java.net.http.HttpClient` (built into the JDK since Java 11 — no extra
  HTTP library dependency needed)
- `InitCommand.kt`, `Main.kt` — the `init` and `add` subcommands

`cli-kotlin/src/test/kotlin/.../RewriterTest.kt` — a JUnit 5 port of the
same assertions proven in Node today. **This is your very first move
tomorrow**: run `./gradlew test` and confirm these pass in the real target
language, on real Kotlin, with a real compiler. If they don't pass
immediately, the JS-to-Kotlin port has a translation bug, not a logic bug —
much faster to find than debugging both algorithm and syntax at once.

Sanity checks that don't require a compiler were run against all Kotlin
files (brace/paren balance, consistent package declarations) — everything
checks out structurally, but **this is not a substitute for actually
compiling it.** Treat the Kotlin source as "very likely correct, unverified
by execution" until you run it.

### 3. Gradle build file — using verified-current plugin coordinates

`cli-kotlin/build.gradle.kts` uses `com.gradleup.shadow` for the fat-JAR
build, not the older `com.github.johnrengelman.shadow` ID. This was
confirmed via live web search today: maintainership transferred to the
GradleUp organization, and the plugin ID changed accordingly. Using the
outdated ID still technically works through a compatibility shim, but
would have been a stale-training-data mistake to hand you silently.
**Double-check the pinned version number** against
https://plugins.gradle.org/plugin/com.gradleup.shadow before your first
build — plugin versions move faster than this document can track.

---

## What is NOT yet verified — be precise about this

- The Kotlin CLI has never been compiled. First real compile happens on
  your machine.
- The Compose component files (`BottomSheet.kt`, `NeonButton.kt`, etc.)
  have never been compiled against real Compose Multiplatform
  dependencies — that requires Maven access this sandbox doesn't have.
  They were written carefully against known-stable `compose.foundation`
  APIs, but "written correctly" and "compiles" are different claims and
  only the second one matters.
- `BottomSheet.kt`'s actual drag-gesture wiring
  (`detectVerticalDragGestures`) was stubbed with a comment rather than
  fully wired, flagged explicitly in the file itself — this was cut for
  fixture brevity today since it doesn't touch the rewrite logic being
  validated, not hidden.
- `Installer.kt` points at a placeholder registry URL
  (`github.com/jetcompose/registry`) that doesn't exist yet — you'll
  create the real repo and update this constant.
- No real Gradle project has been touched by this CLI yet — only the
  simulated fake-target-project fixture.

---

## Day 2 checklist — in order

1. **Compile it for real.** Set up a real JDK (21+) and Kotlin toolchain
   on your machine (Android Studio bundles both — you likely already have
   what you need). `cd cli-kotlin && ./gradlew test`. If `RewriterTest.kt`
   passes, the port is faithful and the core logic is now doubly-proven.
2. **Create the actual GitHub repo** for the registry, push the contents
   of `registry/`, and update `Installer.kt`'s `REGISTRY_BASE` constant to
   point at it.
3. **Build the fat JAR**: `./gradlew shadowJar`, confirm it produces a
   runnable `jetcompose-0.1.0.jar`.
4. **Create one real KMP project** (or use an existing one) and run the
   actual CLI against it — `java -jar jetcompose.jar init`, then
   `java -jar jetcompose.jar add bottomsheet/glass`. This is the first
   time the whole pipeline touches a real Gradle/Compose project.
5. **Fix the drag-gesture stub** in `BottomSheet.kt` — wire up
   `detectVerticalDragGestures` for real.
6. **Only after that works**, move to the Week 4 real-world-testing phase
   already scoped in the earlier planning conversation: test against
   3–5 different real projects, not just your own.

Nothing here should be treated as "done" until step 1 and step 4 both
happen for real. Today proved the algorithm. Tomorrow proves the build.
