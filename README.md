# JetCompose

Copy-paste UI components for Jetpack Compose and Kotlin Multiplatform —
a shadcn/ui-style registry for Android and iOS.

Unlike a web `.tsx` file, a copy-pasted Kotlin file breaks the build
immediately: its `package` declaration is bound at compile time to its
physical location under the module's source root. JetCompose is a CLI
that rewrites packages, imports and file paths so an installed component
compiles with zero manual fixes, plus the registry those components live
in.

Components use `compose.foundation` only — no Material 3, by design.

> **Status: pre-launch, not yet publicly consumable.** The CLI jar is
> built by CI and attached to releases on this repo, which is currently
> **private**, so there is no public download and no install command to
> hand to an outside user yet. The registry repo *is* public (it must be —
> the installer does unauthenticated fetches). See
> [Not yet public](#not-yet-public).

## Current state

| | Version | Verified |
|---|---|---|
| CLI | **0.3.3** | 12/12 `RewriterTest.kt` tests pass (2026-08-05) |
| Registry | **0.3.2** | All 3 manifests fetch 200 from the pinned tag; jar installs from it end-to-end |

Three components: `bottomsheet/glass`, `button/neon`,
`button/neon_outline`.

The end-to-end flow is proven against four testbeds and by two external
testers: one-Enter `init` via filesystem auto-detection, `add` with a
duplicate-install guard, correct boundary-safe package rewriting,
`@Preview` installed on supported projects, and clean builds on non-KMP,
KMP, version-catalog and multi-module projects.

## Layout

```
cli-kotlin/     Production Kotlin CLI (zero runtime dependencies)
registry/       Component sources; mirrored to the public registry repo
design/         PRODUCT_BRIEF.md, TOKENS.md, design-system/ HTML previews
cli-prototype/  Day-1 JS validation harness for the rewrite algorithm
DAY1-REPORT.md  Historical Day-1 verification record
CLAUDE.md       Working rules, full current state, backlog
```

## Requirements

- **JDK 17 or newer.** The CLI targets JVM 17 (`jvmToolchain(17)`), not
  21 — Gradle itself requires 17+, so it is a floor every Android/KMP
  developer already meets, and a 21-target jar fails with
  `UnsupportedClassVersionError` on JDK 17 machines.
- **Gradle 9.2.1+** to build the CLI. Shadow 9.5.1 calls the `Provider`
  overload of `addVariantsFromConfiguration`, added in 9.2.1; older
  Gradle fails at configuration time before compiling anything. The
  wrapper here is pinned to 9.3.1.

## Building and testing

```bash
cd cli-kotlin && ./gradlew test
```

```bash
cd cli-kotlin && ./gradlew shadowJar
```

That produces `cli-kotlin/build/libs/jetcompose-0.3.3.jar`.

## Using the CLI

Run once per project, then once per component:

```bash
java -jar jetcompose-0.3.3.jar init
```

```bash
java -jar jetcompose-0.3.3.jar add bottomsheet/glass
```

`init` auto-detects the project from the filesystem alone (no Gradle
evaluation): it finds Gradle modules, classifies KMP vs Android by
source-set layout, and reads the root package from your sources. Standard
projects need one Enter to confirm; anything unusual falls back to manual
prompts. It writes `.jetcompose/config.json`, which every `add` reads.

## Registry

Components are served from
[`jetcompose-registry`](https://github.com/BalajiReddy1/jetcompose-registry)
over `raw.githubusercontent.com`, **pinned to a release tag rather than a
branch**. Branch URLs are cached for ~5 minutes, which caused transient
404s and stale content during real testing; tag URLs are immutable.

Two consequences worth knowing:

- Registry changes require cutting a new registry tag *and* bumping
  `REGISTRY_BASE` in `Installer.kt`, which means re-releasing the jar.
  The two repos have independent tag lines.
- **The registry repo must stay public.** The installer fetches
  unauthenticated; making it private silently 404s every `add` for every
  user. This has happened once already.

## Known limitations

- **Shared library modules need `api(...)`.** Installing a component into
  a module like `:feature:ui` only compiles for consuming modules if that
  module exposes Compose via `api(...)` rather than `implementation(...)`
  — the component's public signature exposes Compose types (`Modifier`,
  `Color`, `Dp`, `@Composable`).
- **Style names must be legal Kotlin package segments.** The rewriter
  joins `category`/`style` verbatim into the target package, so
  `neon-outline` would generate an illegal package and fail every
  install. Use underscores. This is not yet validated at manifest load.
- **Installed components nest deeply**
  (`ui/components/<category>/<style>/internal/<prefix>/`). Cosmetic, and
  raised by a real tester.

## Not yet public

Deliberately recorded so it is not mistaken for an oversight:

- This repo is private, so its GitHub Releases — and therefore the CLI
  jar — are not downloadable by anyone outside. Going public is the
  founder's call; the history was reset to purge an internal strategy
  doc, so there is no blocker other than that decision.
- The registry repo is public but has no README or landing content.
- There is no website yet, and no user-facing install documentation
  written for an outside developer. This README is an engineering
  document, not a launch page.

## Contributing / working on this

Read `CLAUDE.md` first. It carries the hard rules — including that a
component counts as verified only when it both compiles on Android and
desktop *and* has been run on a real screen with its interactions
exercised. Compile-only verification is explicitly not sufficient here;
that rule exists because a bottom sheet whose drag-to-dismiss compiled
everywhere broke on the first human touch.
