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
  prototype. **Compiles and all 12 `RewriterTest.kt` tests pass**
  (8 rewrite + 4 path-traversal; re-run and confirmed 12/12, 0 failures,
  2026-08-04 on Gradle 9.3.1).
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
- **Window insets are a component responsibility (added 2026-08-01).**
  `GlassBottomSheet` now takes `contentWindowInsets`, defaulting to
  `WindowInsets.safeDrawing.only(Horizontal + Bottom)`, applied to the
  card only — the scrim stays full-bleed so it dims behind the system
  bars. Without this the card renders **under the gesture navigation
  bar on Android 15+**, where edge-to-edge is enforced and cannot be
  opted out of. These are `compose.foundation.layout` APIs (no
  Material, works in commonMain/desktop) and resolve to zero on
  older/non-edge-to-edge setups, so one code path is correct from
  Android 5 through 15+. **Every new component that touches a screen
  edge must handle insets and expose them as a parameter.**
- **Emulator inventory**: `Pixel_35` (API 35) is the one that matters —
  edge-to-edge enforcement, gesture nav, 440dpi. `Pixel_4_33` (API 33)
  is kept for backward-compat checks. The API-33-only setup is what
  hid the inset bug for weeks: **verify layout/inset work on Pixel_35**.
  The testbed's `MainActivity` calls `enableEdgeToEdge()` so inset bugs
  surface on every API level rather than only on modern devices.
- **Testing gotcha — `adb shell input swipe` duration matters.** Under
  ~100ms the injected gesture produces too few MOVE events and Compose
  never recognizes a drag at all (`onDragStopped` never fires, so it
  looks exactly like a broken component). Use **150–300ms** to simulate
  a real flick. Measured on Pixel_35 at 200ms: offsetY 176px vs 176px
  threshold, velocity 1006 px/s vs 344 px/s threshold — both criteria
  fire correctly. Don't diagnose gesture bugs from sub-100ms swipes.
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

## Where things stand (read this first in a new session)

**Built and working:** CLI **v0.3.3**, registry **v0.3.2**. The whole
flow is proven end-to-end by real users: one-Enter `init` (filesystem
auto-detection), `add` with an idempotency guard, correct package
rewriting, `@Preview` on install, zero manual fixes, builds clean on
non-KMP + KMP + version-catalog + multi-module projects. Path traversal
is hardened (12/12 tests). Insets handled for Android 15 edge-to-edge.

**Publicly obtainable as of 2026-08-05.** The main repo is now public.
Verified unauthenticated: repo page, Releases API and the v0.3.3 release
page all 200, and `jetcompose-0.3.3.jar` downloads (1,774,619 bytes) and
runs. The registry repo is public and serving all three manifests from
the v0.3.2 tag. The copy-the-command flow now terminates in a real
artifact instead of a 404 — this was the hard gate on the website, and
it is cleared.

**The actual bottleneck is NOT engineering.** Three components exist,
**zero external users**, no public website, nothing to look at. The
adoption clock only starts now that the thing is obtainable. Do not let
a new session drift back into plumbing — it is comfortable and it is
not what's blocking.

**Design status — first pass rejected by the founder as "too simple".**
A Claude Design project exists: **"JetCompose Design System"**
(`claude.ai/design`, projectId `ecb1d604-a8ff-4b11-9ceb-f06e26e78f57`),
with Foundations/Tokens + Button + Card. Local sources are in
`design/design-system/*.html` (standalone previews, true superellipse
geometry computed via clip-path).

**The diagnosis for why it came out generic — don't repeat this:** the
"must survive re-theming" constraint was applied to the *whole* system,
so everything got designed down to utility blandness. That constraint
only belongs on the **base tier**. Correct model is two tiers:
- **Base tier** (button, card, input, list): quiet, themeable, boring on
  purpose — shadcn's buttons are plain too, that's why people adopt them.
- **Signature tier** (glass sheet and friends): visually striking out of
  the box. This is the hook — nobody adopts a new library for
  "well-considered restraint", they adopt it because they saw something
  that looked incredible.
Also: an aesthetic of restraint without a bold idea underneath is just
plain. Braun/Teenage Engineering strip everything else *after* committing
to a strong idea, not instead of one.

**This diagnosis is now written into the design docs themselves
(2026-08-05), so read them rather than working from this summary.**
`design/PRODUCT_BRIEF.md` holds intent and is upstream; `design/TOKENS.md`
holds values. The two-tier model, the brand-vs-component token split, and
the light/dark requirement all live there now. See the completed backlog
entry for what changed and why.

**Agreed next step: build the website.** It is the one surface with no
theming constraint (so personality is allowed), it is the marketing
artifact everything else depends on, and it converts this from a project
into a product. Ship the site with the components that exist, the
copy-the-command flow, and a live demo; the "hero" signature component
can land in v2 of the page. `design/PRODUCT_BRIEF.md` holds the
non-technical product story to design from.

## Backlog

### Completed since this list was written (kept for the record)

- **Preview-on-install — SHIPPED in CLI v0.3.1** (registry v0.3.0).
  Every component ships a `@Preview` demo file (optional `preview`
  manifest field). The CLI routes it to the ANDROID source root
  (androidMain for KMP — the androidx annotation doesn't exist in common
  code) and installs it ONLY if the module build file text
  (comment-stripped, never evaluated) shows a ui-tooling dependency;
  otherwise it skips the file and prints the dependency hint. False
  negatives degrade to the hint; a build-breaking install is impossible.
  Verified: skip path, non-KMP install path, KMP androidMain path, both
  targets compile.
- **`bottomsheet/glass` visual redesign — SHIPPED in registry v0.3.1**,
  since superseded by v0.3.2 (drift fixes). Design language is written
  down in `design/TOKENS.md` (4pt spacing, 4/12/24dp radii, 120/220/320ms
  motion; the colour section was restructured on 2026-08-05 — see the
  reconciliation entry below). Components still
  inline the values so each stays self-contained after install; a shared
  installed token package remains the deferred shared-utilities-tier
  decision. Sheet redesign: full-screen overlay with tap-to-dismiss
  scrim, floating card (12dp margins, 24dp radius, 640dp max width),
  gradient glass fill + light-catching hairline drawn in `drawBehind`,
  slide-up/slide-down enter/exit. Verified on emulator: nudge springs
  back, fast flick / slow drag / scrim tap all dismiss.
  **Open question, never resolved in writing:** this entry previously
  said "awaiting founder's design approval before release" — it was
  released anyway. Whether approval was given or the step was skipped is
  unrecorded.

- **Brief/tokens reconciliation — DONE 2026-08-05.** Precedence is now
  explicit: `PRODUCT_BRIEF.md` (intent) is upstream of `TOKENS.md`
  (values); if they disagree the brief wins. The contradictions were
  resolved by two splits, both now written into both files:
  - **Surface split.** Brand tokens (`canvas`, `accent`, `textPrimary`,
    `textSecondary`) belong to the website/previews and **may never be
    referenced by installed component code** — no component used them
    anyway, and per the brief none can, since the host app owns its
    background and text. Component tokens are all parameter *defaults*.
  - **Tier split.** Base tier quiet and themeable; signature tier bold.
    The key clarification: the signature tier is freed from *restraint*,
    not from *adaptability* — it still installs into a stranger's app,
    so nothing may be dark-only.

  The grayscale test decided the neon question on its own: glass is
  luminance and survives grayscale (form, keep it); neon is hue and
  collapses (decoration, demote it). Also added: a typography section
  (the old file had none, so components were setting type unchecked), a
  light-scheme colour table marked **UNVERIFIED — must not ship until
  run on a real screen**, and a rewritten quality bar. The brief's
  anti-reference was split into its functional objection
  (component-scoped) and its differentiation objection (applies to the
  website too). One thing it surfaced is still NOT done: the
  component-naming decision at the end of `TOKENS.md`.

- **Component drift fixed and RELEASED — registry v0.3.2 / CLI v0.3.3,
  2026-08-05.** All 11 token deviations fixed; full table and
  verification log at the end of `design/TOKENS.md`. The substantive
  ones: `NeonButton`'s glow was `Modifier.shadow(ambientColor/spotColor)`
  — Android-only, rendering nothing comparable on desktop or iOS in a
  product sold on one codebase for both — rebuilt as layered
  widening/fading strokes; `NeonButton` had no pressed or disabled state
  at all; and the glass hairline and drag handle hard-coded
  `Color.White`, the mechanical reason the sheet was dark-only, now
  `hairlineColor`/`handleColor` params with unchanged defaults so dark
  rendering is pixel-identical.

  Verified to the hard rule before release: compiled Android + desktop,
  run on Pixel_35 (API 35, edge-to-edge) with disabled-tap-ignored,
  spring-back, distance-dismiss, velocity-dismiss, scrim-dismiss and
  insets all exercised; desktop window confirmed the rebuilt glow
  renders off-Android; then the built jar was run end-to-end against a
  fixture project to confirm it fetches v0.3.2 and installs the fixed
  code with correct package rewriting.

  **iOS is now verified by execution** — see the entry below.

- **Main repo made public — 2026-08-05.** The CLI jar is now downloadable
  by anyone. Verified unauthenticated: repo page, Releases API and the
  v0.3.3 release page all 200; `jetcompose-0.3.3.jar` downloads and runs.
  This was the hard gate on the website. **Note:** the repo's git history
  is now public too, and it contains one line of internal strategy (the
  kill-criteria numbers) in an older CLAUDE.md revision. The working tree
  is scrubbed; removing it from history would need a rewrite + force-push,
  which is a founder decision and probably not worth it for one goal
  metric on a repo with no forks yet.

- **iOS verification in CI — DONE 2026-08-05.** `verification/` is a
  standalone Gradle build (deliberately separate from `cli-kotlin/`, so the
  Compose toolchain can never affect the CLI's zero-dependency build). It
  stages the registry sources into a generated source root at the paths
  their own packages require — no rewriting, since the harness consumes
  them directly — and excludes `*Preview.kt` (Android-only tooling).
  `.github/workflows/verify-components.yml` runs the interaction tests on a
  real `iosSimulatorArm64` simulator on `macos-latest`, plus the same tests
  on the JVM so an iOS failure is distinguishable from a broken test.
  **6/6 green on the simulator**, including dismiss-on-drag and
  spring-back-on-nudge — the historical bug class.

  Two things worth knowing:
  - The first run failed with "the JVM garbage collector is thrashing"
    before any test ran. That looked like an iOS failure and was purely
    out-of-memory: Kotlin/Native needs far more heap than the JVM build,
    and the build had no `gradle.properties`. Fixed there.
  - The harness also targets desktop **so the tests can be validated
    locally before spending macOS CI minutes** — keep that target.
  - The suite is mutation-checked: breaking `enabled` on `NeonButton`
    failed exactly the test that guards it.

  **Still not covered on iOS: appearance.** The tests prove composition and
  interaction, not that the glow and glass look right. That needs an Xcode
  app bundle plus `simctl io screenshot`.

- **Wasm feasibility spike — PROVED 2026-08-05.** The website needs to show
  the *real* components, not an HTML re-implementation that would silently
  drift from the Kotlin. `verification/harness` now has a `wasmJs` target
  (`VerificationScreen` is shared by desktop, iOS and browser). Findings:
  - **It compiles.** All three components build for wasmJs unchanged —
    including `WindowInsets.safeDrawing`, which was the API most likely to
    be platform-limited. `compose.foundation`-only really does reach the
    browser.
  - **Payload is ~11 MB**: `skiko.wasm` **8.0 MB** (the Skia renderer,
    a fixed cost paid once regardless of component count), our code 1.5 MB,
    plus a 352 KB JS shim. This is the number the website design has to
    plan around — it is not something to discover later.
  - Loads with zero console errors, the wasm fetches 200, Compose mounts and
    acquires a WebGL context. **Painted pixels and interactivity in the
    browser are NOT yet confirmed** — the check ran in a headless pane where
    `visibilityState` is `hidden`, so Skiko never sizes its surface. Serve
    `harness/build/dist/wasmJs/productionExecutable` and look, before
    building anything on top of this.
  - Not wired into CI (a wasm build is ~5 min). Worth adding as a
    compile-only guard if the website depends on it.

- **Light/dark mechanism — DECIDED 2026-08-05.** Per-component colours
  object (`<Component>Colors` with `.dark()` / `.light()` / `.auto()`),
  defaulting to `.auto()`, which follows the system. Full reasoning and the
  caveats are in `design/TOKENS.md`; the short version:
  - A shared `JetComposeTokens.kt` would be better DX at 20 components and
    is closer to how shadcn really works — but per-component → shared is a
    forward migration that preserves every call site, while the reverse is
    not, and the shared route needs CLI machinery (install-once,
    idempotent, tracked) that would serialise all 20 components behind it.
    **Deferred with a trigger: revisit at wave 2 (~12 components).**
  - **The factories are the migration seam.** A shared layer later changes
    only what `.auto()` reads. If factory names drift between components
    that migration stops being mechanical — keep them identical.
  - Default follows the system because when it is wrong it is wrong
    *loudly* (a dark card on a light app is glaring, one param to fix),
    and because require-explicit contradicts the zero-config promise.
  - **This changes today's hard-dark defaults**, so the three existing
    components need re-verifying in both schemes. Cheapest to do now, at
    ~zero external users.
  - **Blocking gate:** `isSystemInDarkTheme()` behaviour is unconfirmed on
    wasm and iOS. If it returns a constant on wasm, every component on the
    website renders in the wrong scheme — on the surface built to sell
    them. Verify before building on this.

### Still open, in rough order

1. Folder-depth complaint from real-dev test (deep
   `ui/components/<cat>/<style>/internal/<prefix>/` nesting) — cosmetic.
2. CLI validation: reject non-package-legal style names at manifest load.
3. **Launch-facing docs.** `README.md` was rewritten 2026-08-04 into an
   accurate engineering README (current versions, JDK 17+, Gradle 9.2.1+,
   the `api(...)` shared-module note, registry pinning, and an explicit
   "not yet public" section). The Day-1 report it replaced is preserved
   as `DAY1-REPORT.md` with a header marking it historical. What remains
   is the *outside-developer* install documentation — blocked on both
   item 1 and the pending product-name decision, since it bakes in the
   command name and a download URL.
4. Website — **the agreed next step** (see "Where things stand"). Both
   things that gated it are now done: the design-doc reconciliation, and
   the component drift (all three components now meet the quality bar
   and are released, so they are fit to put on a showcase page). The one
   remaining hard gate is item 1 — the copy-the-command flow needs a jar
   an outsider can actually download. The product-name decision is also
   still pending and is upstream of any public surface.

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
java -jar build/libs/jetcompose-0.3.3.jar init
java -jar build/libs/jetcompose-0.3.3.jar add bottomsheet/glass
java -jar build/libs/jetcompose-0.3.3.jar add button/neon_outline
```
