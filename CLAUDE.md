# JetCompose — CLAUDE.md

Copy-paste UI component registry for Jetpack Compose + Kotlin Multiplatform
(shadcn/ui equivalent for Android + iOS). Two parts: a CLI that installs
components with zero compile errors, and a website that showcases them.
The CLI is the actual engineering; the website is secondary.

**For full project history and reasoning, read the founder's private
context doc once at the start of a session — it lives OUTSIDE this repo
at `C:\Users\balaj\Desktop\jetcompose-private-docs\PROJECT_CONTEXT.md`
(deliberately untracked: it contains internal strategy, and this repo's
history was reset specifically to keep it out). Don't treat this file as
the full story, it's deliberately short because it reloads every turn.
Never copy that doc, or excerpts of its strategy content, back into this
repo.**

## Hard rules — do not relitigate these without being asked

- **No Material 3, anywhere, ever.** Components use `compose.foundation`
  only. If you see a Material 3 import in any component file, that's a bug,
  not a style choice — remove it.
- **CLI distribution is a fat JAR via GitHub Releases**, invoked as
  `java -jar jetcompose.jar ...`. Not a Node/npx CLI, not a Gradle plugin,
  not GraalVM native-image, not SDKMAN as the primary channel. These were
  each considered and rejected for specific, documented reasons (see the
  private context doc's "Architecture decisions" section if you need the
  reasoning). Don't propose switching distribution mechanisms without the
  founder explicitly asking to revisit it.
- **Package/import rewriting must be boundary-safe.** Never use a plain
  string replace for rewriting a component's registry package to a target
  package — it must not corrupt an unrelated package that merely shares a
  prefix (e.g. rewriting `com.jetcompose.bottomsheet.glass` must never
  touch `com.jetcompose.bottomsheet.glassy.Something`). `Rewriter.kt`
  already implements this correctly with a bounded regex — match that
  pattern in any new rewrite logic.
- **Internal component dependencies are namespaced per-component** via
  each manifest's `prefix` field (`internal/<prefix>/...`). This is what
  lets two components ship same-named files (e.g. two different
  `ModifierExtensions.kt`) without colliding. Don't remove or bypass the
  prefix system to "simplify" the manifest.
- **A written file's path must always exactly match its own package
  declaration.** This is a hard Kotlin compiler rule, not a style
  preference — violating it is a guaranteed compile error regardless of
  anything else being correct.
- **Zero-dependency philosophy for the CLI is deliberate, not an
  oversight.** No `kotlinx.serialization`, no CLI-argument-parsing
  framework. `MiniJson.kt` is a small hand-rolled parser sized for exactly
  the two schemas this project needs. Don't add dependencies to the CLI
  module without a concrete reason tied to a real limitation you've hit —
  "this library would be more standard" isn't sufficient justification on
  its own.

## Build behavior

- **Don't run a full `./gradlew build` unless explicitly asked.** Gradle
  daemon cold-starts cost real time; prefer reading code carefully or a
  targeted compile task for routine edits. The Day 2 checklist has
  specific steps that do require a real build — those are the intended
  exceptions to this rule.
- When you do run tests, prefer `./gradlew test` scoped to the relevant
  module, and summarize pass/fail rather than pasting full Gradle output
  into the conversation.

## Current state (update this section as work progresses)

- `cli-kotlin/` — production CLI source, ported from a validated JS
  prototype. **Compiles and all 8 `RewriterTest.kt` tests pass** (verified
  2026-07-11 on Gradle 9.3.1).
- JVM target is **17, not 21** (`jvmToolchain(17)`): Gradle itself
  requires JVM 17+, so 17 is a floor every Android/KMP developer already
  meets, and a 21-target jar failed with `UnsupportedClassVersionError`
  on a real JDK 17 machine. Verified: the 17-target jar runs on JDK 17.
- Gradle wrapper now exists in `cli-kotlin/`, pinned to **Gradle 9.3.1**.
  Minimum required Gradle is **9.2.1+**: Shadow plugin 9.5.1 calls the
  `Provider` overload of `addVariantsFromConfiguration`, which was added
  in Gradle 9.2.1 — older Gradle (e.g. 9.1.0) fails at configuration time
  with a `NoSuchMethodError`-style failure before compiling anything.
- `registry/` — three components: `bottomsheet/glass` and `button/neon`
  (Day 1 fixtures, since compile-verified) plus `button/neon_outline`,
  the first post-fixture shippable component (2026-07-12). **Component
  style names must be legal Kotlin package segments** — `Rewriter`
  joins `category`/`style` verbatim into the target package, so
  kebab-case (`neon-outline`) would generate an illegal package and fail
  every install; use underscores. Docs/CLI-validation item for later.
- The registry repo is live at
  `github.com/BalajiReddy1/jetcompose-registry` (public — required, since
  the installer does unauthenticated raw.githubusercontent.com fetches).
  `Installer.kt`'s `REGISTRY_BASE` points at it, **pinned to a release
  tag, not `main`**: raw.githubusercontent.com caches branch URLs for
  ~5 minutes, which caused a transient 404 and stale-content serving
  during real-project testing. Tag URLs are immutable — registry changes
  require cutting a new tag and bumping `REGISTRY_BASE` (jar re-release).
- `jetcompose init` validates each entered source root by walking up
  (depth-bounded: the root + 4 ancestors, never above the project dir)
  looking for a `build.gradle(.kts)`. No build file nearby → warning +
  confirm-before-write, not a hard block. Heuristic, not a guarantee:
  a typo in the source-set segment of a shallow path can still slip
  through if the module directory itself is valid.
- **Known limitation (docs, not code)**: installing a component into a
  shared library module (e.g. `:feature:ui`) only compiles for consuming
  modules if that module exposes Compose via `api(...)` dependencies —
  the component's public signature exposes Compose types (`Modifier`,
  `Color`, `Dp`, `@Composable`). With plain `implementation(...)`,
  consumers fail to compile against it unless they declare their own
  Compose deps. Found in multi-module real-project testing (2026-07-11);
  needs a line in user-facing docs when those exist.
- `BottomSheet.kt`'s drag-gesture handling is fully wired
  (`pointerInput` + `detectVerticalDragGestures`, offset applied to the
  sheet, dp→px threshold conversion) and compile-verified against real
  Compose Multiplatform deps on Android + desktop targets. Runtime
  (on-device) behavior not yet exercised.

## Cutting a CLI release

The CLI ships as a fat JAR attached to a GitHub Release on the main repo
(`github.com/BalajiReddy1/jetcompose`, currently **private** — history
was reset 2026-07-12 to purge the internal strategy doc, so going public
is now purely the founder's call). The release pipeline is
`.github/workflows/release.yml`, triggered by pushing a `v*` tag:

1. Bump `archiveVersion` in `cli-kotlin/build.gradle.kts` if the version
   is changing; commit.
2. If registry content changed, first cut a new tag in the registry repo
   (`jetcompose-registry`) and bump `REGISTRY_BASE` in `Installer.kt` to
   match — the two repos have **independent tag lines**.
3. `git tag vX.Y.Z && git push origin master vX.Y.Z`
4. Actions builds the jar on JDK 17, smoke-tests it (`--help` output),
   and creates the release with `jetcompose-X.Y.Z.jar` attached.
5. Verify with `gh release view vX.Y.Z` or download and run the jar.

## Key commands

```bash
cd cli-kotlin
./gradlew test                     # run RewriterTest.kt
./gradlew shadowJar                # build the fat JAR
java -jar build/libs/jetcompose-0.2.0.jar init
java -jar build/libs/jetcompose-0.2.0.jar add bottomsheet/glass
java -jar build/libs/jetcompose-0.2.0.jar add button/neon_outline
```
