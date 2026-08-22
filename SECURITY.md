# Security

Lamintra is a CLI that downloads Kotlin source over the network and writes
it into your project. That is a meaningful amount of trust, so this page
states exactly what it does, what it does not do, and how to report a
problem.

## Reporting a vulnerability

**Please do not open a public issue for a security problem.**

Use GitHub's private reporting on this repository:
[Security → Report a vulnerability](https://github.com/BalajiReddy1/lamintra/security/advisories/new).

If that is unavailable to you, open an issue titled "Security - contact
request" with no details in it, and a private channel will be arranged.

This is a single-maintainer project with no on-call rotation. Realistic
expectations rather than invented ones:

| | Target |
|---|---|
| First response | within 5 working days |
| Assessment and plan | within 14 days |
| Fix for a confirmed high-severity issue | as fast as a release can be cut, which is same-day if needed |

You will be credited in the release notes unless you ask not to be.

## Supported versions

Only the **latest release** is supported. There are no long-term support
branches, and a fix ships as a new version rather than a patch to an old
one.

Because each CLI release hard-pins a registry tag, running an old jar also
means receiving old component source. If you are more than one release
behind, upgrade before reporting.

## What the CLI accesses

**Reads**

- `.lamintra/config.json` in the current working directory.
- Your project's Gradle build files and source tree, to detect the project
  layout and to check whether a module has the Compose preview dependency.
  Detection is filesystem-only: **no Gradle build script is ever evaluated
  or executed.**

**Writes**

- `.lamintra/config.json`, on `init`.
- Component `.kt` files under the source root and component path recorded
  in that config.
- For `scaffold ios-shell` only: `.swift` files under `iosApp/`, and
  Kotlin under your shared source sets.

It writes nowhere else. It does not modify your build files, your Gradle
configuration, your git history, or anything outside the project directory
you run it in.

**Network**

- Exactly one host by default:
  `raw.githubusercontent.com/BalajiReddy1/lamintra-registry`, over HTTPS,
  pinned to an immutable release tag.
- Unauthenticated GET requests only. No credentials are sent, read, or
  stored.

**Executes**

- Nothing. The CLI does not run your build, does not shell out, and does
  not execute any downloaded content. Downloaded files are treated as text
  to be rewritten and written to disk.

## What it does not do

- **No telemetry.** The CLI collects nothing, sends nothing, and phones
  home never. There is no analytics, no crash reporting, no usage counter.
- **No account, no key, no login.** Nothing to authenticate with.
- **No dependency added to your build.** Installed components import
  `androidx.compose.foundation` and nothing else. Deleting the jar and the
  `.lamintra` directory is the whole uninstall; your project still
  compiles.

## The `LAMINTRA_REGISTRY` override

Setting `LAMINTRA_REGISTRY` redirects where component source is fetched
from - either a URL or a local directory. It exists so a registry change
can be tested before it is published.

**Treat it as a trust boundary.** Anything that can set that variable in
your environment can choose the source code the CLI writes into your
project. Leave it unset unless you are developing Lamintra itself, and
treat an unexpected value in CI the way you would treat any other altered
dependency source.

## Known gaps, stated rather than discovered

Being explicit about what is not implemented is more useful than implying
a completeness that does not exist.

- **No checksum or signature verification** on downloaded component
  source. Integrity currently rests on HTTPS and on the registry tag being
  immutable. If you need supply-chain guarantees, vendor the registry and
  point `LAMINTRA_REGISTRY` at your own copy.
- **A failed install is not rolled back.** If a write fails partway
  through a multi-file component, the files already written remain. This
  can leave a component that does not compile until the install is re-run.
- **The registry repository must be public** for unauthenticated fetches
  to work, so component source is public by necessity, not by oversight.

## Scope

In scope: the CLI, the registry content, this repository's workflows, and
lamintra.com.

Out of scope: vulnerabilities in JetBrains Compose, AndroidX, Gradle, the
JDK, or GitHub itself - report those upstream. Also out of scope: the
consequences of running the CLI against a registry you have pointed it at
yourself via `LAMINTRA_REGISTRY`.
