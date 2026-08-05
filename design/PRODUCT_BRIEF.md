# JetCompose — Product Brief (for design work)

Non-technical. This is what the product *is* and who it's for, written so a
designer (human or AI) can make decisions without reading any code.

**This file is upstream of `TOKENS.md`.** This one holds intent; that one
holds the specific values implementing it. If they disagree, this file
wins and the tokens get revised. (Reconciled 2026-08-05 — before that both
claimed to be the single source of truth and contradicted each other on
colour, dark mode, and neon.)

## What it is

A library of beautiful, copy-paste UI components for mobile apps built with
Kotlin — one component works on both Android and iPhone. A developer browses a
website, copies one command, runs it, and the component appears in their
project ready to edit. They own the code; nothing is hidden in a dependency.

## The problem it solves

Almost every Android app looks the same, because almost every Android app uses
Google's Material Design — the default that ships in the box. It's competent
and completely anonymous. Designers call it "the Bootstrap of mobile."
Developers who want an app with a personality have two options today: hire a
designer, or hand-build every component from scratch. Most do neither and ship
something generic.

Worse for cross-platform teams: Material is Google's design language, so an
app using it looks visibly *wrong* on an iPhone — like an Android app someone
ported. Teams sharing one codebase across both platforms feel this daily.

## Who it's for

1. **Cross-platform teams (primary)** — shipping one Kotlin codebase to
   Android and iOS. They need components that look intentional on both, not
   Google's language on Apple's hardware.
2. **Solo developers and small teams** — no designer on staff, but they refuse
   to ship something that looks like a template.
3. **AI coding agents (strategic)** — tools like Claude Code and Cursor
   increasingly install components rather than generate them. Being the
   registry they reach for is a compounding advantage.

## What the user feels

Before: "My app works, but it looks like every other Android app, and it looks
out of place on iPhone."

After: "It looks like someone designed it."

The emotional promise is **relief plus pride** — the app now looks
intentional, and they didn't have to become a designer to get there.

## The hard constraint that shapes everything

These components are installed into **other people's apps**, which already
have their own colors and fonts. So:

- The design system cannot impose a color palette. Every color is a parameter.
- It cannot impose a typeface. The host app's font is used.
- It must work in **light and dark**, because real apps have both.

**Therefore the components' distinctiveness must live in their FORM — shape,
spacing, proportion, and motion — not in their color.** That is the central
design problem. A JetCompose component should be recognizable in a grayscale
screenshot. If it's only recognizable because it's dark with a neon glow, we
have decoration, not a design system.

## Two tiers — the correction that makes the above workable

The first design pass came out generic, and the reason was diagnosed
precisely: the constraint above was applied to *everything*, so every
component got designed down to utility blandness. It belongs to the base
tier only.

- **Base tier** (button, card, input, list) — quiet, themeable, boring on
  purpose. shadcn's buttons are plain too; that's *why* people adopt them.
  They have to disappear into somebody else's app.
- **Signature tier** (the glass sheet and its relatives) — visually
  striking out of the box. This is the hook. Nobody adopts a new library
  for well-considered restraint; they adopt it because they saw something
  that looked incredible.

**The signature tier is freed from restraint. It is not freed from
adaptability.** It still installs into a stranger's app, so it must still
resolve in their light or dark scheme — a near-black glass card with white
edges on a white background doesn't read as striking, it reads as broken.
Boldness of form, yes; dark-only, no.

Related: an aesthetic of restraint without a bold idea underneath is just
plain. Braun and Teenage Engineering strip everything else away *after*
committing to a strong idea, not instead of having one.

Applied honestly, the grayscale test also settles what stays and what
goes. Glass is luminance — depth, a light-catching top edge, alpha
gradients — and it survives grayscale, so it is form, and it is the real
signature language. A neon accent is hue; in grayscale it collapses to a
flat gray and the component stops being recognizable, so it is decoration.
Keep the glass, demote the neon.

## What "good" means here

The components must survive being placed in an app the designer has never
seen, in a color scheme they didn't pick, next to fonts they didn't choose —
and still look better than the Material default. That's a higher bar than
"looks nice in a screenshot."

## Two surfaces, one identity

1. **The components** — theme-agnostic and structurally distinctive;
   restrained in the base tier, bold in the signature tier. Personality
   comes from geometry and motion, in both.
2. **The website** — the showcase, where the brand can be loud. Visitors copy
   a command and see live components. This is the marketing artifact; it can
   have a strong point of view the components themselves cannot afford.

They must feel related without being identical: the site is the poster, the
components are the product.

This split is load-bearing, and `TOKENS.md` now enforces it: colours that
belong to the poster (the dark canvas, the neon accent, our text colours)
are **brand tokens** and installed component code may never reference
them. A host app owns its own background and its own text.

## References — for stance, not for copying

- **Braun / Dieter Rams** — restraint, honest materials, nothing decorative.
- **Teenage Engineering** — technical objects with personality; precision as
  an aesthetic.
- **Linear** — density and typographic discipline for a developer audience.
- **Things 3 (iOS)** — proof that soft, warm, and precise can coexist.
- **Swiss/International typographic style** — grid, hierarchy, restraint.

Explicitly NOT the reference: dark-mode-with-neon-glow developer aesthetic
(Aceternity-style). That rejection rests on two separate objections, and
they have different scopes — worth keeping straight, because the project
currently ships components literally named `neon`:

1. **It only works in dark.** A *functional* objection, and it applies to
   components only. This is why no component may be dark-only.
2. **It reads as decoration, and it's the current default of AI-generated
   UI.** A *differentiation* objection, and it applies everywhere —
   including the website. A site that looks like every other AI-generated
   dark-neon landing page fails at the one job it exists to do.

So the brand surface is permitted neon in a way components are not, but it
must not be what the brand rests on. "Loud" has to mean a strong idea of
our own, not the genre default turned up.

## Anti-goals

- Not a Material Design reskin.
- Not a theme or a dependency — installed source code the developer owns.
- Not decoration-first. Every flourish must survive being re-colored.
- Not dark-only. Applies to **every** component, both tiers — the signature
  tier gets boldness, not an exemption from the host's colour scheme.
- Not plain for its own sake. Restraint is the base tier's job, not the
  whole system's.
