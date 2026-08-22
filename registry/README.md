# lamintra-registry

The component source that [`lamintra`](https://github.com/BalajiReddy1/lamintra)
fetches and writes into your project.

**You do not need to clone this.** Install components with the CLI:

```bash
java -jar lamintra-0.9.0.jar add button
```

Full instructions: **[lamintra.com/install](https://lamintra.com/install/)**

## What is here

One directory per component, each with a `component.json` manifest and its
Kotlin source, plus `theme/` (which every component depends on) and
`scaffolds/`.

Components use `androidx.compose.foundation` only - no Material 3, by design.

## Why it is a separate repository

The CLI fetches over `raw.githubusercontent.com`, unauthenticated, **pinned to a
release tag rather than a branch**. Branch URLs are cached for around five
minutes, which caused transient 404s and stale content during real testing; tag
URLs are immutable.

Two consequences:

- **This repository must stay public.** Making it private silently 404s every
  `lamintra add` for every user. That has happened once already.
- Tags here are independent of the CLI's tags. A registry change needs a tag
  here *and* a matching CLI release, because each CLI release pins one tag.

## Licence

MIT - see [LICENSE](LICENSE). The files the CLI writes into your project are
yours: modify them, ship them commercially, and keep them whatever happens
upstream.
