# Contributing

Lamintra is a single-maintainer v0 project. The most useful thing you can
send is not a pull request - it is a report of what happened when you
actually tried to use it.

## The three things most worth sending

**1. A component request.** The roadmap is genuinely "whoever asks first".
There are eight components; which one is missing for you is real
information, and asking costs you one issue.

**2. An install that did not work.** Include the four things that make it
reproducible, because without them a report cannot be acted on:

```
WHAT I RAN      the exact command, and the directory you ran it in
EXPECTED        what you thought would happen
GOT             the exact output, pasted verbatim
ENVIRONMENT     OS, `java -version`, `./gradlew --version`,
                and your Kotlin / AGP / Compose versions
```

**3. "I looked at this and did not use it, because..."** The reason is
worth more than a bug. Nobody outside the maintainer has built a product
on this yet, so the reasons people bounce are the most valuable unknown in
the project.

## Reporting a bug

Open an [issue](https://github.com/BalajiReddy1/lamintra/issues). Include
the block above.

If a component compiles but looks or feels wrong, say which device, which
colour scheme, and what you expected instead. Screenshots and screen
recordings are extremely welcome - much of this library is motion, and
motion does not survive a text description.

**Security issues do not go in the issue tracker.** See
[SECURITY.md](SECURITY.md).

## Pull requests

PRs are welcome, with two things to know first.

**Open an issue before writing a component.** Components carry design
decisions - radius, motion, elevation, colour - that come from a system
documented in `design/TOKENS.md`. A component that ignores it is more work
to merge than to write, and that is a bad trade for you.

**Small fixes need no ceremony.** Typos, broken links, a wrong version in
a document, an error message that misled you - send them directly.

### What a change has to clear

- `cd cli-kotlin && ./gradlew test` passes.
- The three workflows go green: `release`, `verify-components`,
  `verify-ios-shell`. Watch all three, not only the one your change looks
  like it touches - two releases once shipped green while
  `verify-components` had been red for six hours.
- **A component is not verified by compiling.** It counts as verified when
  it compiles on Android and desktop *and* has been run on a real screen
  with its interactions exercised. That rule exists because a bottom sheet
  whose drag-to-dismiss compiled everywhere broke on the first human
  touch. If you cannot run it on a device, say so in the PR - that is
  fine, and it is better than an unstated assumption.

### House rules for component code

- **`compose.foundation` only.** No Material 3, ever. `grep -r "material3"`
  over your change must return nothing. This is the product's main
  differentiator, not a style preference.
- **Use the design tokens.** Radius, spacing, colour and motion come from
  the shared theme. A component that invents its own numbers is how the
  library ended up with six different corner radii once already.
- **Anything shared goes under `internal/<component>/`.** Two components
  may carry a file of the same name - `Squircle.kt` exists five times -
  and the per-component directory is what stops them colliding when both
  are installed.

## Working on the CLI

`CLAUDE.md` is the real document: working rules, the release chain, and a
list of traps that each cost real time when they were first hit. Read it
before changing anything in `cli-kotlin/`.

Two things that are easy to get wrong:

- **The registry and the CLI have independent tag lines.** A registry
  change needs a new registry tag *and* a bump of `PUBLISHED_REGISTRY` in
  `Registry.kt`, which means re-releasing the jar.
- **Test with `LAMINTRA_REGISTRY` pointed at your working tree**, then
  test again with it unset against the published tag before calling
  anything done. Unset is what users get.

## Code of conduct

Be straightforward and assume good faith. Criticism of the product is
welcome and wanted - the project's documents are unusually blunt about
what does not work yet, and contributors are held to the same standard,
not a politer one. Personal attacks are not welcome and will be removed.

## Licence

By contributing you agree your contribution is licensed under the
[MIT licence](LICENSE), the same as the rest of the project.
