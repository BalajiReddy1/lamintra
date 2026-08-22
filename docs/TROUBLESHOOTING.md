# Troubleshooting

Every entry here is something that has actually happened, with the exact message
you would see.

If your problem is not here, [open an issue](https://github.com/BalajiReddy1/lamintra/issues)
with the command you ran, the output verbatim, and your Kotlin/AGP/Compose
versions. That is enough to reproduce almost anything.

---

## `'lamintra' is not recognized` / `command not found`

**There is no `lamintra` on your PATH.** It is a jar, not an installer. Run:

```bash
java -jar lamintra-0.9.0.jar add button
```

To type less, alias it once:

```bash
# Windows, cmd
doskey lamintra=java -jar C:\tools\lamintra-0.9.0.jar $*

# Windows, PowerShell profile
function lamintra { java -jar C:\tools\lamintra-0.9.0.jar @args }

# macOS / Linux
alias lamintra='java -jar ~/tools/lamintra-0.9.0.jar'
```

## `Access is denied` on Windows, with a path in the message

Usually **an unquoted path containing a space**, read by the shell as a
redirect. Quote it:

```powershell
java -jar "C:\My Tools\lamintra-0.9.0.jar" add button
```

If the path has no spaces, it is a real permission problem - see
"a partly-installed component" below.

## `No .lamintra/config.json found`

Run `lamintra init` first, **in your project's root directory**. The CLI uses
the current working directory as the project, never the jar's location, so `cd`
into the project first.

## `Not found in the registry: <name>/component.json (HTTP 404)`

The component name is wrong, or it has been retired. Names are lowercase
kebab-case: `text-field`, not `textField` or `TextField`, and `add BUTTON` will
404 as well.

The current list is at [lamintra.com/components](https://lamintra.com/components/).
Retired names are recorded in [CHANGELOG.md](../CHANGELOG.md).

## `Couldn't reach the registry at raw.githubusercontent.com`

Network, not your project. Nothing was installed. It retries three times over
about 1.5 seconds before giving up.

If you are online and it persists, check
[githubstatus.com](https://www.githubstatus.com) - the registry is served by
`raw.githubusercontent.com`.

## `The registry is rate limiting us (HTTP 429)`

Temporary and not your fault. Wait a minute and run the same command again. Also
`githubstatus.com`.

## `theme already exists in this module at: ...`

You have a component tree in two places in one module - usually from running
`init` again with a different source root or `componentPath` and then installing
again. Gradle compiles every source root, so two copies of `LamintraTheme.kt`
would be duplicate declarations and break the build.

The message names both paths. Either delete the copy you do not want, or re-run
`init` so the configured source root matches where the components already live.

## `KEPT YOUR VERSION: .../LamintraTheme.kt`

Not an error. **You edited that file, and the CLI will not overwrite your
edits.** Every component depends on `theme`, so every `add` would otherwise
rewrite it.

If you want the registry's current version and are willing to lose your changes:

```bash
java -jar lamintra-0.9.0.jar add theme --force
```

## `<component> was not installed: ... 3 partly-written file(s) were removed`

A write failed partway through. The files already written were rolled back, so
you do not have half a component. Fix the cause - a read-only directory, a full
disk, a file locked by your IDE - and run the same command again.

## `Refusing to install "...": it resolves outside the project directory`

A path resolved outside your project. Two realistic causes: a hand-edited
`.lamintra/config.json` with an absolute or `../` source root, or a symlink
inside your project pointing out of it. Nothing was written.

## `Preview file skipped: couldn't confirm the ui-tooling-preview dependency`

Not an error either. The `@Preview` file needs `androidx.compose.ui:ui-tooling-preview`,
and installing it without that dependency would produce a file that cannot
compile. Add:

```kotlin
implementation("androidx.compose.ui:ui-tooling-preview")
debugImplementation("androidx.compose.ui:ui-tooling")
```

then re-run the same `add`. The component itself installed fine.

## Installed files do not compile: `Unresolved reference`

Three things to check, in order:

1. **Compose version.** Components are tested against Compose Multiplatform
   1.11.1 and Compose BOM 2026.02.01. An older Compose may not have an API a
   component uses. That is a version gap, not a broken component - please open
   an issue with your versions.
2. **A shared library module needs `api(...)`.** If you installed into something
   like `:feature:ui`, consuming modules only compile if that module exposes
   Compose with `api(...)` rather than `implementation(...)` - the components'
   public signatures expose `Modifier`, `Color`, `Dp` and `@Composable`.
3. **A partial install from an older CLI.** Versions before the rollback fix
   could leave a component missing one file. Delete the component's directory
   and re-add it.

## Files installed somewhere Gradle never compiles

Symptom: `add` says it succeeded, and nothing you install is visible to your
build.

Check `.lamintra/config.json` - the `sourceRoots` entry should be a directory
that exists and sits inside a real Gradle module. Older CLI versions could write
a source root naming a module that did not exist. Fix the path, or delete
`.lamintra/` and run `init` again.

## The Android Studio preview does not show the component

`@Preview` only exists on Android. In a KMP project the preview file installs to
`androidMain`, not `commonMain`, because the annotation does not exist in common
code. That is expected.

## Removing Lamintra

Delete the jar and the `.lamintra/` directory. That is the whole uninstall - the
components stay, because they were only ever your files, and they import
`compose.foundation` and nothing else. Your project still compiles.
