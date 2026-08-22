# Lamintra

Copy-paste UI components for Jetpack Compose and Kotlin Multiplatform -
a shadcn/ui-style registry for Android, iOS, desktop and web.

**[lamintra.com](https://lamintra.com)** - every component, rendered, with its
source and install command.

Unlike a web `.tsx` file, a copy-pasted Kotlin file breaks the build
immediately: its `package` declaration is bound at compile time to its
physical location under the module's source root. Lamintra is a CLI
that rewrites packages, imports and file paths so an installed component
compiles with zero manual fixes, plus the registry those components live
in.

Components use `compose.foundation` only - no Material 3, by design.

> **Status: v0. Working end to end, and used by very few people.** Eight
> components, a project scaffold, a published CLI and a live site. What it
> has not had is many users - see [What is not done](#what-is-not-done),
> which is deliberately specific.

## Current state

| | Version | Verified |
|---|---|---|
| CLI | **0.9.0** | Installs all eight components into fresh KMP and Android projects; both compile |
| Registry | **0.8.0** | Published tag is byte-identical to `registry/` in this repo |

Eight components: `button`, `card`, `list-row`, `segmented`, `sheet`,
`swipe-row`, `switch`, `text-field`. Plus a shared `theme` every component
depends on, and one scaffold, `ios-shell`.

Slugs are flat. `bottomsheet/glass`-style nested names were removed on
2026-08-06 and are no longer valid.

## Install

Requires **JDK 17 or newer** - you already have one if you run Gradle.
Download the jar from the
[latest release](https://github.com/BalajiReddy1/lamintra/releases/latest):

```bash
curl -LO https://github.com/BalajiReddy1/lamintra/releases/download/v0.9.0/lamintra-0.9.0.jar
```

Run once per project, then once per component:

```bash
java -jar lamintra-0.9.0.jar init
```

```bash
java -jar lamintra-0.9.0.jar add button
```

There is no `lamintra` on your PATH - it is a jar, not an installer. The
[install page](https://lamintra.com/install/) has a `doskey` line, a
PowerShell function and a shell alias if you want to type less.

`init` auto-detects the project from the filesystem alone, with no Gradle
evaluation: it finds Gradle modules, classifies KMP vs Android by
source-set layout, and reads the root package from your sources. Standard
projects need one Enter to confirm; anything unusual falls back to manual
prompts. It writes `.lamintra/config.json`, which every `add` reads.

## Scaffolds

```bash
java -jar lamintra-0.9.0.jar scaffold ios-shell
```

A scaffold is project structure rather than a component: `ios-shell`
writes Swift into `iosApp/` and Kotlin into your shared source set, so
SwiftUI draws the tab bar and navigation bars while Compose renders each
screen. That split is what lets iOS 26 apply Liquid Glass to the chrome
without any styling code - Compose renders to a Skia canvas and cannot
draw it itself.

Scaffolds are installed once, are never package-rewritten, and require a
KMP project. In an Android-only project the command refuses and explains
why rather than half-writing.

## Compatibility

**Tested**, rather than claimed. These are the combinations the components
have actually been compiled against:

| | Gradle | AGP | Kotlin | Compose |
|---|---|---|---|---|
| KMP | 9.1.0 | 9.0.1 | 2.4.10 | Multiplatform 1.11.1 |
| Android | 9.5.0 | 9.3.1 | 2.2.10 | BOM 2026.02.01 |

The CLI itself needs **JDK 17+**; the jar is compiled to bytecode 61 and
will not start on anything older.

Nothing below those versions has been tested. If a component fails to
compile on an older Compose, that is a version gap rather than a broken
component - please open an issue with your versions.

## Requirements to build the CLI

- **JDK 17 or newer.** The CLI targets JVM 17 (`jvmToolchain(17)`), not
  21 - Gradle itself requires 17+, so it is a floor every Android/KMP
  developer already meets, and a 21-target jar fails with
  `UnsupportedClassVersionError` on JDK 17 machines.
- **Gradle 9.2.1+.** Shadow 9.5.1 calls the `Provider` overload of
  `addVariantsFromConfiguration`, added in 9.2.1; older Gradle fails at
  configuration time before compiling anything. The wrapper here is
  pinned to 9.3.1.

```bash
cd cli-kotlin && ./gradlew test
cd cli-kotlin && ./gradlew shadowJar
```

That produces `cli-kotlin/build/libs/lamintra-0.9.0.jar`. The jar name
derives from the single `version` property, and `release.yml` names the
release asset from the git tag - a mismatch fails the release rather than
publishing one.

## Layout

```
cli-kotlin/     The Kotlin CLI (zero runtime dependencies)
registry/       Component sources; mirrored to the public registry repo
design/         PRODUCT_BRIEF.md, TOKENS.md, design-system/ HTML previews
.github/        Three workflows: release, verify-components, verify-ios-shell
CLAUDE.md       Working rules and the traps that cost real time
```

## Registry

Components are served from
[`lamintra-registry`](https://github.com/BalajiReddy1/lamintra-registry)
over `raw.githubusercontent.com`, **pinned to a release tag rather than a
branch**. Branch URLs are cached for ~5 minutes, which caused transient
404s and stale content during real testing; tag URLs are immutable.

Two consequences worth knowing:

- Registry changes require cutting a new registry tag *and* bumping
  `PUBLISHED_REGISTRY` in `Registry.kt`, which means re-releasing the jar.
  The two repos have independent tag lines.
- **The registry repo must stay public.** The installer fetches
  unauthenticated; making it private silently 404s every `add` for every
  user. This has happened once already.

Set `LAMINTRA_REGISTRY` to a local directory or a branch URL to install
from something other than the published tag. It is a development
affordance - leave it unset and you get what everyone else gets.

## Known limitations

- **Shared library modules need `api(...)`.** Installing a component into
  a module like `:feature:ui` only compiles for consuming modules if that
  module exposes Compose via `api(...)` rather than `implementation(...)` -
  the component's public signature exposes Compose types (`Modifier`,
  `Color`, `Dp`, `@Composable`).
- **Every component depends on `theme`**, so every `add` touches
  `LamintraTheme.kt`. In v0.9.0 that means an edit to it is overwritten -
  **back it up before installing anything else.** Fixed in the next release:
  see [Unreleased](CHANGELOG.md#unreleased).
- **A failed install is not rolled back in v0.9.0.** If a write fails partway
  through a multi-file component, the files already written stay. Re-run the
  same `add` once the cause is fixed. Also fixed in the next release.
- **Duplicate component trees are refused, not merged.** If `theme`
  already exists somewhere else in the module, `add` stops and names both
  paths rather than creating a second copy that would break the build.

## What is not done

Recorded so none of it is mistaken for an oversight.

- **iOS appearance is not verified.** `verify-ios-shell.yml` type-checks
  the generated Swift against the real iOS 26 SDK on a macOS runner, so it
  compiles. Nobody has yet *looked* at it on a simulator or a device.
- **The gesture components have not been tuned on a thumb.** `sheet` and
  `swipe-row` depend on spring constants that are measured and compiled
  but not yet judged by anyone holding a phone.
- **No screen reader has been run over the components.** `swipe-row`
  publishes its actions as semantics custom actions, because a gesture is
  invisible to a screen reader. Nobody has heard them.
- **Eight components is a v0 registry, not a component library.**

## Documentation

| | |
|---|---|
| [Components and install guide](https://lamintra.com) | The site - every component, rendered, with its source |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | How the CLI, the rewriter and the registry fit together |
| [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) | Every error message, and what to do about it |
| [CHANGELOG.md](CHANGELOG.md) | Every release, and what has been retired |
| [CONTRIBUTING.md](CONTRIBUTING.md) | What is worth sending, and what a change has to clear |
| [SECURITY.md](SECURITY.md) | What the CLI reads, writes and requests - and how to report a vulnerability |
| [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) | |

## Contributing

Issues and component requests are welcome - the roadmap is genuinely
"whoever asks first". See [CONTRIBUTING.md](CONTRIBUTING.md).

For security issues, see [SECURITY.md](SECURITY.md).

`CLAUDE.md` carries the working rules, including that a component counts
as verified only when it both compiles on Android and desktop *and* has
been run on a real screen with its interactions exercised. Compile-only
verification is explicitly not sufficient here; that rule exists because a
bottom sheet whose drag-to-dismiss compiled everywhere broke on the first
human touch.

## Licence

MIT. See [LICENSE](LICENSE). The components the CLI writes into your
project are yours - modify them, ship them commercially, keep them
whatever happens upstream.
