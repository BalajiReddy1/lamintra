# Changelog

All notable changes to the Lamintra CLI and the component registry.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and
this project uses [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

**The CLI and the registry have independent version numbers.** Each CLI release
pins exactly one registry tag, so the pairing is what actually decides which
component source you receive. Both are listed for every release.

| | |
|---|---|
| Current CLI | **v0.9.0** |
| Registry it pins | **v0.8.0** |

---

## [Unreleased]

Fixes from an adversarial test run against the released v0.9.0. Each one was
reproduced, fixed, and re-tested against a jar built from the changed source.

### Fixed

- **`add` no longer overwrites files you have edited.** Every component depends
  on `theme`, so every `add` rewrote `LamintraTheme.kt` - the colour ramp,
  radius scale and motion tokens, and the file you are most likely to have
  customised. An edit to it survived exactly until the next `add`. A file that
  differs from the registry's version is now kept, the run says which file and
  why, and `--force` takes the registry version if you want it.
- **A failed install no longer leaves a broken project.** Writes are staged
  through a journal and undone if any of them fails. Previously, a failure on
  the fourth of `switch`'s five files left `LamintraSwitch.kt` calling a
  `softShadow` whose source never arrived, so a project that compiled before the
  command did not compile after it.
- **An unreachable registry says so.** It printed `Error: null` and did not
  retry, because the retry logic only covered HTTP status codes and a connection
  failure throws before producing one. Now three attempts with backoff and a
  message naming the host.
- **Malformed JSON no longer prints 1,023 stack frames.** The parser is depth
  limited, and the CLI catches `Throwable` rather than `Exception` -
  `StackOverflowError` is an `Error` and escaped entirely.
- **A symlink or directory junction inside your project can no longer redirect a
  write outside it.** The containment check used `File.getCanonicalFile()`, which
  does not traverse a Windows junction; it now resolves the real path.
- **`lamintra --version`** exists. There was no way to tell which build you had.
- **An unknown command exits 1** instead of printing help and exiting 0.
- **Re-running `init` no longer discards a customised config.** It shows your
  current settings and keeps them by default.
- **A typo at the module prompt asks again** instead of abandoning a correct
  detection and falling back to defaults that did not match the project.
- **The manual-flow source-root defaults are read from your project** rather
  than naming `composeApp`, which the Kotlin Multiplatform wizard has not
  produced for some time.
- **`swipe-row` compiles without warnings.** It emitted two
  "Condition is always 'true'" warnings into every build that installed it.
- **`LAMINTRA_REGISTRY` rejects plaintext `http://`** to anywhere but localhost.
- **`\uXXXX` escapes in JSON are parsed**, rather than becoming literal text.

### Added

- `--force` on `add`, to take the registry's version of a file you have edited.
- `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, this changelog, issue
  templates, and a licence and README for the registry repository.

### Removed

- `day1-verification-proof-of-install/` and `cli-prototype/`, both historical.

---

## [0.9.0] - 2026-08-19

Registry **v0.8.0**.

### Added
- `swipe-row` gained `fillMaxWidth()` by default, applied before the caller's
  modifier so `Modifier.width(...)` still wins.

### Changed
- **A visual system, replacing per-component invented values.** A radius scale
  (8 segmented, 12 button and text-field, 16 card, 20 sheet); the capsule
  retired everywhere except the switch knob; `Squircle` made visible at 12dp,
  where a superellipse and a circular arc are no longer the same curve.
- **The palette is monochrome.** `accent` resolved to `ink`. Lime and Olive
  remain in the palette as the opt-in route back.
- **Dark elevation is visible.** Surface to container measured 1.07:1, below
  what an eye resolves; `Neutral900` is `#1C1C20` at 1.16:1, and a third rung
  `containerOverlay` separates a sheet from a card.
- **Disabled is a recessed surface**, not a faded one.

### Fixed
- **A 429 no longer tells you the component name is wrong.** One message covered
  every non-200 status. During a GitHub incident that produced 429s across all
  repositories, the message sent users looking for a typo. Now 404, 429, 5xx and
  everything else are distinguished, with three retries on transient statuses.

## [0.8.0] - 2026-08-16

Registry **v0.7.0**.

### Added
- **`swipe-row`** - actions revealed by a horizontal drag, parallaxed, with a
  velocity-seeded spring settle and a long swipe that arms the destructive
  action. Every action is also published as a semantics custom action, because a
  gesture is invisible to a screen reader.

## [0.7.0] - 2026-08-16

Registry **v0.6.0**.

### Added
- **`sheet`** - the first component whose value is the gesture. Spring settle
  seeded with release velocity, scrim tracking the drag, upward resistance.

## [0.6.1] - 2026-08-16

Registry **v0.5.4**.

### Fixed
- Scaffold placeholders no longer reference Material 3.

## [0.6.0] - 2026-08-16

Registry **v0.5.3**.

### Added
- **`ios-shell`, a scaffold** - a new kind of registry entry. Scaffolds are
  project structure rather than components: Swift as well as Kotlin, installed
  once per project, never package-rewritten. SwiftUI draws the tab bar and
  navigation bars so iOS 26 applies Liquid Glass to them; Compose renders each
  screen.
- **`segmented`**, published for the first time. The site had documented it
  since 2026-08-10 while the pinned registry tag never contained it, so it 404'd
  for every user for six days.
- A shared `theme` every component depends on.

### Removed
- **`glass-sheet`**, retired. It did not hold to the design language the rest of
  the set follows. It had been absent from the website since 2026-08-09 while
  still being installable, which was the wrong way round.

## [0.5.0] - 2026-08-06

### Changed
- **Slugs are flat.** `bottomsheet/glass` and other `<category>/<style>` names
  were replaced by single-segment slugs, matching every component registry that
  has reached this scale.

### Removed
- **`neon-button` and `neon-outline-button`**, retired. They were built in a
  design language this project later rejected, and were named after the exact
  aesthetic the product brief lists as its anti-reference.

## [0.4.0] - 2026-08-06

### Changed
- **The project was renamed to Lamintra.** `bottomsheet` became `glass-sheet`.

## [0.3.3] - 2026-08-05

### Fixed
- Eleven design-token deviations across the component set.

## [0.3.2] - 2026-08-01

### Fixed
- Window insets for the bottom sheet.

### Security
- The installer refuses a manifest path that resolves outside the project
  directory.

## [0.3.1] - 2026-07-30

### Added
- `@Preview` files installed alongside a component, but only when the module's
  build file shows the `ui-tooling-preview` dependency - a missing dependency
  would otherwise produce a file that cannot compile.

## [0.3.0] - 2026-07-20

### Added
- **`init` auto-detects your project** from the filesystem alone, with no Gradle
  evaluation. Standard projects need one Enter to confirm.
- An idempotency guard on `add`.

## [0.2.1] - 2026-07-20

### Fixed
- Drag-to-dismiss on the bottom sheet.

## [0.2.0] - 2026-07-12

### Added
- `button/neon_outline`.

## [0.1.0] - 2026-07-12

Initial public release.

---

## A note on retired components

**A name that used to work and now 404s is worse than one that never existed**,
so removals are recorded here rather than quietly dropped.

Retired so far: `neon-button`, `neon-outline-button` (v0.5.0), and `glass-sheet`
(registry, 2026-08-11). `bottomsheet` became `glass-sheet` in v0.4.0 before that
was retired in turn. Asking for any of them now fails with a readable error
naming the components page.

**Retiring a component does not reach anyone already holding a jar.** Because
each CLI release pins a registry tag, an existing v0.9.0 keeps installing from
registry v0.8.0, which still contains whatever it contained on the day it was
tagged. That is the immutability guarantee working in the awkward direction, and
it is the honest meaning of "retired" here: no new registry tag carries it, and
no future CLI release will reach it.

[Unreleased]: https://github.com/BalajiReddy1/lamintra/compare/v0.9.0...HEAD
[0.9.0]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.9.0
[0.8.0]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.8.0
[0.7.0]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.7.0
[0.6.1]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.6.1
[0.6.0]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.6.0
[0.5.0]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.5.0
[0.4.0]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.4.0
[0.3.3]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.3.3
[0.3.2]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.3.2
[0.3.1]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.3.1
[0.3.0]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.3.0
[0.2.1]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.2.1
[0.2.0]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.2.0
[0.1.0]: https://github.com/BalajiReddy1/lamintra/releases/tag/v0.1.0
