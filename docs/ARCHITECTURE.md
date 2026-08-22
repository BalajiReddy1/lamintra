# Architecture

How Lamintra is put together, and why. If you are trying to fix a bug or add a
component, this is the map.

```
You run:  java -jar lamintra-0.9.0.jar add button

  Main.kt          parses the command, turns anything thrown into one line
    |
  Config.kt        reads .lamintra/config.json - where YOUR code lives
    |
  Registry.kt      HTTPS GET to a pinned registry tag. The only network code
    |
  Manifest.kt      parses component.json, validates the slug
    |
  Rewriter.kt      rewrites packages, resolves target paths, refuses escapes
    |
  Installer.kt     orders requirements, writes through an undo journal
    |
  Your project     ordinary Kotlin files you now own
```

## The problem this exists to solve

A copy-pasted `.tsx` file works. **A copy-pasted Kotlin file does not**, because
its `package` declaration is bound at compile time to its physical location
under the module's source root. Move the file and it stops compiling.

So a component registry for Compose cannot be "copy this snippet". It has to
rewrite the package declaration, every internal cross-reference, and the file's
path on disk, consistently, so that the three always agree. **That rewrite is
the entire product.** Everything else is delivery.

## The three repositories

| Repository | What it is | Why separate |
|---|---|---|
| `lamintra` | CLI, component sources, CI | The thing you build |
| `lamintra-registry` | What the CLI actually fetches | Must be public and independently taggable |
| `lamintra-site` | lamintra.com | Deploys on its own cadence |

**The registry is separate on purpose.** The CLI fetches over
`raw.githubusercontent.com` unauthenticated, pinned to a release tag rather than
a branch, because branch URLs are cached for around five minutes and served
transient 404s and stale files during real testing. Tag URLs are immutable.

Two consequences that catch people out:

- **The registry repo must stay public.** Private means unauthenticated fetches
  404 - every `add`, for every user. This has happened once.
- **Registry and CLI tags are independent lines.** A registry change needs a tag
  there *and* a bump of `PUBLISHED_REGISTRY` in `Registry.kt`, which means
  re-releasing the jar. A CLI release pins exactly one registry tag.

## Detection: filesystem only

`InitCommand` decides your project's shape **without evaluating a single Gradle
build script.** It looks for directories two levels deep that hold a build file
and a `src/`, classifies KMP versus Android by source-set layout, and reads the
root package off the shallowest `.kt` file - accepting it only if the declared
package matches its own directory path.

This is a deliberate trade. Evaluating Gradle would be more accurate and vastly
more fragile: it means running a user's build to install a file. Reading the
filesystem is fast, cannot execute anything, and is wrong only in shapes where
it can ask instead.

**It writes `.lamintra/config.json` and never looks again.** Every `add` reads
that file. If detection got it wrong, one file is wrong, and you can edit it.

## Package rewriting

`Rewriter` does one thing carefully: replace the registry's root package with
yours, **on token boundaries**. Rewriting `com.lamintra.glass` must not corrupt
`com.lamintra.glassy`, so the match is anchored with `(?![A-Za-z0-9_])`.

Two rules keep paths and packages from ever disagreeing:

1. `computeNewRootPackage` is used by **both** content rewriting and path
   resolution. One function, so a file's location cannot contradict its own
   `package` line.
2. A component's shared internals live under `internal/<component>/`. Six
   components ship a `Squircle.kt`; the per-component directory is what lets all
   six coexist in one module.

A component may declare `requires`. Those install depth-first, before the file
that imports them, and their packages are rewritten in the dependent's source
too - otherwise an installed `button` would still import `com.lamintra.theme`,
which does not exist in your project.

## Writing files: the safety properties

Three things the installer guarantees, each because it once did not.

**It cannot write outside your project.** Every write goes through
`resolveSafeTarget`, which rejects absolute paths and resolves the real path -
`Path.toRealPath()`, not `File.getCanonicalFile()`, because the latter does not
traverse a Windows directory junction and a junction inside a project could
redirect a write outside it.

**It will not overwrite your edits.** A file that differs from what the registry
would write is kept, and named. `--force` opts in to losing your changes. This
is the product's central promise: once a component is in your repository it is
yours, and a tool that rewrote your edits later would be taking that back.

**A failure does not leave half a component.** Every file is fetched and
rewritten before anything is written, so a network failure writes nothing at
all. The writes then go through a journal that restores prior content and
deletes files it created if any write throws.

## Scaffolds are not components

A scaffold is project structure: `ios-shell` writes Swift into `iosApp/` and
Kotlin into your shared source sets. It is installed once, is **never**
package-rewritten, requires a KMP project, and refuses with an explanation
elsewhere. Read by `ScaffoldManifest`, not `ComponentManifest`.

## Why there are no dependencies

`cli-kotlin/build.gradle.kts` has no runtime dependencies. `MiniJson.kt` is
about a hundred lines and covers exactly what these manifests need.

That is a decision, not an oversight: every dependency is one more thing that
can break a stranger's first run, and the product's own pitch is that installed
components pull in nothing. A CLI that contradicted that would be odd.

The same applies to the components: `androidx.compose.foundation` only, never
Material 3. `grep -r "material3"` over installed files must return nothing.

## CI, and the reason there are three workflows

| Workflow | What it proves |
|---|---|
| `release.yml` | The jar builds and is named from the tag |
| `verify-components.yml` | The components compile and their tests pass |
| `verify-ios-shell.yml` | Generated Swift type-checks against the real iOS SDK on macOS |

**Watch all three, not the one your change looks like it touched.** Two releases
once shipped green from `release.yml` while `verify-components.yml` had been red
for six hours - `release.yml` only builds the jar and never compiles the harness.

`verify-ios-shell.yml` runs against the **working-tree** registry rather than a
published tag, so a scaffold is verified before it ships.

## What this architecture does not do

- **No dependency resolution.** `requires` is a flat list installed depth-first.
  No version constraints, because components have no versions independent of the
  registry tag.
- **No integrity verification.** No checksums, no signatures. Integrity rests on
  HTTPS and tag immutability. Stated in `SECURITY.md` rather than left to be
  discovered.
- **No update command.** Deliberately. See "It will not overwrite your edits".
