# JetCompose — Product Brief (for design work)

Non-technical. This is what the product *is* and who it's for, written so a
designer (human or AI) can make decisions without reading any code.

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

## What "good" means here

The components must survive being placed in an app the designer has never
seen, in a color scheme they didn't pick, next to fonts they didn't choose —
and still look better than the Material default. That's a higher bar than
"looks nice in a screenshot."

## Two surfaces, one identity

1. **The components** — theme-agnostic, restrained, structurally distinctive.
   Personality comes from geometry and motion.
2. **The website** — the showcase, where the brand can be loud. Visitors copy
   a command and see live components. This is the marketing artifact; it can
   have a strong point of view the components themselves cannot afford.

They must feel related without being identical: the site is the poster, the
components are the product.

## References — for stance, not for copying

- **Braun / Dieter Rams** — restraint, honest materials, nothing decorative.
- **Teenage Engineering** — technical objects with personality; precision as
  an aesthetic.
- **Linear** — density and typographic discipline for a developer audience.
- **Things 3 (iOS)** — proof that soft, warm, and precise can coexist.
- **Swiss/International typographic style** — grid, hierarchy, restraint.

Explicitly NOT the reference: dark-mode-with-neon-glow developer aesthetic
(Aceternity-style). It's the current default of AI-generated UI, it only works
in dark, and it reads as decoration.

## Anti-goals

- Not a Material Design reskin.
- Not a theme or a dependency — installed source code the developer owns.
- Not decoration-first. Every flourish must survive being re-colored.
- Not dark-only.
