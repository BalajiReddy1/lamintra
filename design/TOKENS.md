# JetCompose Design Language — "Obsidian Glass"

One aesthetic, owned completely: deep dark canvas, luminous glass
surfaces, a single neon accent. Every component must look like it came
from the same hand. Distinctive beats neutral — Material owns neutral
on Android; this is the other lane.

**How tokens ship:** this file is the authoring-time source of truth.
Components inline these values (as parameter defaults and internal
constants) so each stays fully self-contained after install — the
per-component `internal/<prefix>/` isolation is a hard rule. A shared
installed token package is the deferred "shared utilities tier"
decision; do not introduce one without the founder reopening it.
When a value here changes, every component that uses it must be
updated and re-verified on screen.

## Color

| Token | Value | Use |
|---|---|---|
| `canvas` | `#0B0E14` | App/demo background, preview backgrounds |
| `scrim` | `#060A12` @ 60% (`0x99060A12`) | Behind overlays |
| `glassTint` | `#9BB8FF` | Base tint for glass surfaces (alpha applied by component) |
| `accent` | `#00E5FF` | Neon interactive elements (one accent, used sparingly) |
| `textPrimary` | `#F2F5FA` | Titles, primary content |
| `textSecondary` | `#8A93A5` | Supporting text, labels |
| `hairlineTop` | `#FFFFFF` @ 38% | Top of light-catching border gradients |
| `hairlineBottom` | `#FFFFFF` @ 4% | Bottom of border gradients |
| `handle` | `#FFFFFF` @ 28% | Drag handles, inert affordances |

Glass fill recipe: vertical gradient of `glassTint` — 26% alpha at top,
14% at 35%, 8% at bottom — under a 1dp hairline gradient border.
Light always comes from the top.

## Spacing — 4pt grid

`4 / 8 / 12 / 16 / 20 / 24 / 32`. Component inner padding: 20
horizontal. Sheet/dialog bottom padding: 24. Floating-surface margin
from screen edge: 12.

## Radius

| Token | Value | Use |
|---|---|---|
| `radiusSmall` | 4dp | Handles, chips |
| `radiusMedium` | 12dp | Buttons |
| `radiusLarge` | 24dp | Sheets, cards, dialogs |

Surfaces float (margin from screen edges, uniform radius) rather than
sitting flush — flush edges are Material's silhouette.

## Motion

| Token | Value | Use |
|---|---|---|
| `motionFast` | 120ms | Press feedback (glow, scale) |
| `motionStandard` | 220ms | Settle/spring-back, exits |
| `motionEnter` | 320ms, decelerate (`LinearOutSlowInEasing`) | Surfaces entering |
| `motionExit` | 220ms, accelerate (`FastOutLinearInEasing`) | Surfaces leaving |

Drag settle inherits release velocity. Nothing pops in — everything
arrives from somewhere.

## Component quality bar (check every one before a registry tag)

1. Uses only tokens above — no ad-hoc colors/durations.
2. All interaction states designed: rest, pressed, dragging, disabled.
3. compose.foundation only. No Material. No new dependencies.
4. Every visual param exposed with a token default (restyle without
   touching logic).
5. Compiles Android + desktop; run on a real screen; interactions
   exercised; screenshots reviewed (hard rule in CLAUDE.md).
6. Looks correct at 1x and 2.75x density, and at 640dp+ widths
   (floating surfaces cap width).
