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
- **A component change is "verified" only when it (a) compiles on
  Android + desktop against real Compose deps AND (b) has been run on a
  real screen with its interactions actually exercised and visually
  confirmed (screenshots or a human looking at it).** Compile-only
  verification is explicitly NOT sufficient for this project — the
  product's entire value is visual. This rule exists because Day 1–2
  shipped a bottom sheet whose drag-to-dismiss compiled everywhere and
  was broken the first time a human touched it (2026-07-12): every
  testbed was compile-only, no app had ever been launched. Don't tag a
  registry release for a component that hasn't met both bars.

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
- `jetcompose init` now **auto-detects the project** (filesystem-only —
  no Gradle evaluation, consistent with the config-over-parsing
  decision): finds Gradle modules (build file + src/, ≤2 levels deep),
  classifies KMP vs Android by source-set layout, prefers whichever of
  `src/main/java`/`src/main/kotlin` actually holds .kt files (AS
  templates use `java/`), and reads the root package from the shallowest
  .kt file's `package` line cross-checked against its directory path.
  Standard projects: one Enter to confirm. Detection failure or user
  rejection → the old manual prompts (with the depth-bounded
  build-file-nearby validation). Driven by real-user testing 2026-07-20:
  a tester hit the wrong-default → re-init → duplicate-install trap and
  said flatly he'd never use a tool with this many questions.
- `jetcompose add` has an **idempotency guard**: before writing, it scans
  the target module's `src/` tree for an existing copy of the component
  (matching `<category>/<style>/<mainfile>` suffix) at any other path —
  found → hard abort naming both paths (duplicate declarations in one
  module = guaranteed "conflicting overloads", since Gradle compiles
  both `src/main/java` and `src/main/kotlin`). Same-path re-add remains
  allowed as an in-place update. Both changes verified against all four
  testbeds + an AS-template-shaped fixture (incl. replaying the exact
  duplicate disaster), then **retested by the same real user who hit the
  original trap — passed (one-Enter init, clean add). Released as CLI
  v0.3.0** (registry unchanged, still v0.2.1).
- **Known limitation (docs, not code)**: installing a component into a
  shared library module (e.g. `:feature:ui`) only compiles for consuming
  modules if that module exposes Compose via `api(...)` dependencies —
  the component's public signature exposes Compose types (`Modifier`,
  `Color`, `Dp`, `@Composable`). With plain `implementation(...)`,
  consumers fail to compile against it unless they declare their own
  Compose deps. Found in multi-module real-project testing (2026-07-11);
  needs a line in user-facing docs when those exist.
- **`BottomSheet.kt` drag-to-dismiss FIXED (registry v0.2.1) and
  founder-hand-verified on device (2026-07-20).** The bug (found in the
  first human on-device test, 2026-07-12): dismissal was distance-only
  against a 120dp default threshold = 330px at 2.75x density, while the
  demo sheet afforded only ~232px of finger travel to the screen edge —
  numerically confirmed via on-device diagnostics (slow max-length drag
  accumulated 231.8px; a fast flick just 56px). Fix: foundation's
  `Modifier.draggable`, dismissal on EITHER >64dp downward drag (new
  reachable default) OR a downward fling >125dp/s (new optional
  `flingVelocityThreshold` param, Material's classic swipeable
  velocity); spring-back inherits release velocity. `anchoredDraggable`
  remains the long-term API for multi-anchor sheets. Lesson encoded in
  the verification hard rule above: this compiled everywhere for days
  and fell to the first real finger.
- **`jetcompose-registry` MUST stay public.** The installer does
  unauthenticated raw.githubusercontent.com GETs — making that repo
  private silently 404s every `jetcompose add` for every user (this
  actually happened 2026-07-20 and stalled the v0.2.1 release). The
  main `jetcompose` repo can be private; the registry cannot.
- **Testbed inventory** (all under `Downloads\Projects\Testbeds\`):
  `jetcompose-kmp-testbed` is the ONLY runnable one (MainActivity +
  launcher manifest + `@Preview` + desktop entry point, added
  2026-07-12 after the gap was found). The other three —
  `jetcompose-testbed-android`, `jetcompose-testbed-catalog`,
  `jetcompose-testbed-multimodule` — are **compile-only**: they have no
  Activity or launcher and exist to test config-routing patterns
  (non-KMP path, version catalog, multi-module source roots). Do not
  assume they can run; a "successful build" there proves compilation
  only. All four originally shipped with no Activity at all — that gap
  is what let the drag bug live undetected.

## Backlog — founder-acknowledged, deliberately deferred until the flow is solid

Priority right now is the end-to-end flow working without surprises, NOT
polish. These are recorded so they aren't forgotten, in rough order:

1. **Preview-on-install** (ACTIVE next item): installed components show
   nothing in Android Studio's preview pane — no `@Preview` composable
   ships with them. Founder hit this twice (2026-07-12 testbed,
   2026-07-20 real project).
2. **`bottomsheet/glass` visual redesign** — founder verdict 2026-07-20:
   functionality fine, looks bad. Polish pass on all components once the
   flow is proven; product value is visual, so this matters before any
   public launch even though it's parked now.
3. Folder-depth complaint from real-dev test (deep
   `ui/components/<cat>/<style>/internal/<prefix>/` nesting) — cosmetic,
   revisit alongside the redesign.
4. CLI validation: reject non-package-legal style names at manifest load.
5. User-facing docs: README with install instructions, the `api(...)`
   shared-module note, JDK 17+ requirement.
6. Website — explicitly AFTER the flow is stable and components look
   good. Don't start it before then.

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
java -jar build/libs/jetcompose-0.3.0.jar init
java -jar build/libs/jetcompose-0.3.0.jar add bottomsheet/glass
java -jar build/libs/jetcompose-0.3.0.jar add button/neon_outline
```
