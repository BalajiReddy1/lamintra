# JetCompose Design Tokens

**Precedence: `PRODUCT_BRIEF.md` is upstream of this file.** The brief
defines what the product is and who it serves; this file is one concrete
implementation of that intent in specific values. If the two ever
disagree, the brief wins and these values get revised — not the other way
round. Reconciled 2026-08-05; before that both files claimed to be *the*
authoring-time source of truth and contradicted each other on colour,
dark-mode, and neon.

## Scope: two surfaces, and they have different rules

This is the distinction the previous version of this file collapsed, and
collapsing it is what produced a "dark canvas + neon accent" token set
sitting directly under a brief that names dark-mode-with-neon-glow as the
explicit anti-reference.

| Surface | What it is | Constraint |
|---|---|---|
| **Brand** | Website, previews, demo apps, README art, marketing | No *functional* constraint — no host app to survive. Be loud, but see the caveat below. |
| **Component** | Installed source code in someone else's app | Must adapt to a host app whose colours and fonts we never see. |

The brief already sanctions this split ("the site is the poster, the
components are the product"). Everything below is labelled with the
surface it belongs to.

**The rule, stated precisely** — because "installed code" and "component
code" are not the same set, and the loose version of this rule would
forbid things that are actually correct:

- **Component code** (the component and its `internal/` helpers) may
  never reference a brand token to *structure* itself — no brand
  background, no brand text colour, nothing that assumes our canvas is
  behind it. The host app owns those.
- **One crossing is permitted:** a brand hue may be a *parameter default*
  the host can replace, e.g. `NeonOutlineButton(color = accent)`. That's
  a suggestion, not a dependency. It may never be load-bearing for
  recognition (see the grayscale test).
- **`@Preview` and demo files may use brand tokens freely**, and should.
  They ship into the user's project but they are *our* surface — their
  job is to show the component as we intend it. This is why every
  `*Preview.kt` currently paints a `#0B0E14` canvas, and that is correct,
  not drift. When light-scheme support lands, previews should show both
  schemes rather than switching.

## Component tiers

From the design diagnosis in `CLAUDE.md`: applying the re-theming
constraint to *everything* is what made the first pass generic. It
belongs to the base tier only.

| Tier | Components | Brief |
|---|---|---|
| **Base** | button, card, input, list | Quiet, themeable, boring on purpose. shadcn's buttons are plain too — that is why people adopt them. |
| **Signature** | glass sheet and friends | Visually striking out of the box. This is the hook. |

**The signature tier is freed from restraint. It is NOT freed from
adaptability.** A signature component still installs into a stranger's
light-mode app. A sheet that renders a near-black glass card with white
hairlines on a white background does not look striking, it looks broken.
"Signature" buys boldness of *form*, not permission to be dark-only.

## The grayscale test, and what it actually decides

The brief's test — *a component should be recognisable in a grayscale
screenshot* — is not decoration. Applied honestly it settles the neon
question on its own:

- The **glass recipe** is luminance-based: alpha gradients, a
  light-catching top edge, depth. It survives grayscale intact.
  → That is **form**. Keep it. It is the real signature language.
- The **neon accent** is hue-based. In grayscale it collapses to a flat
  mid-tone and the component becomes unrecognisable.
  → That is **decoration**. It belongs to the brand surface.

So the resolution is not "drop the aesthetic" — it is *keep the glass,
demote the neon*. Neon may appear as an opt-in parameter default, but it
may never be what makes a component recognisable, and it should not be
what a component is *named* after.

---

# Component tokens

Every value here is a **default for a parameter**, never a fixed
requirement. The rule is: a developer can restyle any component by
passing arguments, without editing its logic. Components inline these
values so each stays self-contained after install (the per-component
`internal/<prefix>/` isolation is a hard rule); a shared installed token
package remains the deferred shared-utilities-tier decision.

## Form — the colour-free identity

**This section is the actual design system.** It survives grayscale,
re-colouring, and any host theme, which makes it the only part that can
carry recognition. Treat changes here as far more significant than
changes to any colour.

### Spacing — 4pt grid

`4 / 8 / 12 / 16 / 20 / 24 / 32`. Component inner padding: 20
horizontal. Sheet/dialog bottom padding: 24. Floating-surface margin
from screen edge: 12. **No off-grid values** — see the drift audit.

### Radius

| Token | Value | Use |
|---|---|---|
| `radiusSmall` | 4dp | Handles, chips |
| `radiusMedium` | 12dp | Buttons |
| `radiusLarge` | 24dp | Sheets, cards, dialogs |

Surfaces float (margin from screen edges, uniform radius) rather than
sitting flush — flush edges are Material's silhouette.

### Motion

| Token | Value | Use |
|---|---|---|
| `motionFast` | 120ms | Press feedback |
| `motionStandard` | 220ms | Settle/spring-back, exits |
| `motionEnter` | 320ms, decelerate (`LinearOutSlowInEasing`) | Surfaces entering |
| `motionExit` | 220ms, accelerate (`FastOutLinearInEasing`) | Surfaces leaving |

Drag settle inherits release velocity. Nothing pops in — everything
arrives from somewhere.

### Typography

The brief forbids imposing a typeface, and the previous version of this
file said nothing at all about type, which left it ungoverned — and
components started setting their own size and tracking unchecked.

- **Never set `fontFamily`.** The host app's font is used, always.
- Size, weight and tracking *are* form and may be set — but only via an
  exposed `TextStyle` parameter the caller can replace wholesale.
- Default label style for interactive components: 15sp, `Medium`,
  `letterSpacing` 1sp. Body content is the caller's, never ours.
- Components that take a `content` slot must not colour the caller's
  text.

## Colour

Two schemes. Every component that renders a surface must resolve
correctly in both, because real apps have both.

### Dark — VERIFIED on screen (emulator, registry v0.3.1)

| Token | Value | Use |
|---|---|---|
| `glassTint` | `#9BB8FF` | Base tint for glass surfaces (alpha applied by component) |
| `scrim` | `#060A12` @ 60% (`0x99060A12`) | Behind overlays |
| `hairlineTop` | `#FFFFFF` @ 38% | Top of light-catching border gradient |
| `hairlineBottom` | `#FFFFFF` @ 4% | Bottom of border gradient |
| `handle` | `#FFFFFF` @ 28% | Drag handles, inert affordances |

Glass fill recipe: vertical gradient of `glassTint` — 26% alpha at top,
14% at 35%, 8% at bottom — under a 1dp hairline gradient border. Light
always comes from the top.

### Light — ⚠️ UNVERIFIED, MUST NOT SHIP UNTIL RUN ON A REAL SCREEN

These are *derived starting points*, not proven values. Per the hard rule
in `CLAUDE.md`, a component is verified only when it compiles on Android
and desktop **and** has been run on a real screen with its interactions
exercised. Nothing below has cleared that bar. This project already
shipped one component that compiled everywhere and broke on the first
human touch — do not repeat it by treating this table as finished.

The physics change, so the recipe changes rather than inverting:

| Token | Candidate | Reasoning |
|---|---|---|
| `glassTint` | `#FFFFFF` @ 82%→70% + 6% `#9BB8FF` | On a light canvas a translucent light tint vanishes; the surface must read as a frosted card sitting *above* content. |
| `scrim` | `#0B0E14` @ 32% | Dimming still works; less depth needed than over a dark canvas. |
| `hairlineTop` | `#FFFFFF` @ 70% | Light still comes from the top. |
| `hairlineBottom` | `#0B0E14` @ 10% | A white bottom edge is invisible here — the card needs a *dark* lower edge to separate from the background. |
| `handle` | `#0B0E14` @ 22% | Same affordance, inverted contrast. |
| elevation | soft shadow below the card | On dark, depth comes from the light-catching edge; on light it must come from a cast shadow. |

## Colour scheme — DECIDED 2026-08-05

**Mechanism: a per-component colours object. Default: follows the system.**

Every component that draws a surface takes a `colors` parameter:

```
<Component>Colors        // e.g. GlassSheetColors, NeonButtonColors
    .dark()              // explicit
    .light()             // explicit
    .auto()              // reads isSystemInDarkTheme(); the default
```

The object holds **only** the colours that component actually draws, and
every component uses these same three factory names. Consistency here is
not cosmetic — see the migration note below.

### Why per-component rather than one shared theme file

A shared `JetComposeTokens.kt` would be better DX at 20 components, and
it is closer to how shadcn actually works (its components depend on
Tailwind config plus CSS variables — they are not self-contained either).
It was still rejected *for now*, on four grounds:

1. **Reversibility is asymmetric.** Per-component → shared is a forward
   migration that preserves every call site. The reverse is a regression
   nobody would perform. This choice keeps both doors open.
2. **The shared option serialises everything.** It needs CLI machinery
   that does not exist — install-once, idempotent, tracked, never
   clobbering user edits. All 20 components would queue behind it.
3. **The pain is asymmetric.** Passing many colour objects is a
   power-user problem; most users install 2–5 components. Self-containment
   helps everyone on day one.
4. **We would be guessing.** At wave 2, with ~12 real components, we will
   know whether duplication actually hurts.

**The cost is real:** duplication across 20 components, and a poor
theming story for someone who installs many. Revisit at wave 2 — this is
a deferral with a trigger, not a permanent answer.

### The migration seam

**The factories are the seam, and this is the whole reason the convention
is fixed.** A future shared token layer changes what `.auto()` reads —
installed tokens instead of inlined literals — without touching a single
call site or any component's public signature. If the factory names drift
between components, that migration stops being mechanical. Do not let
them drift.

### Why the default follows the system

- **Dark by default** loses on first impression: a light-mode app gets a
  near-black card and looks broken out of the box.
- **Require-explicit** contradicts the zero-config promise; a real tester
  already rejected an earlier build for asking too many questions.
- **Follow-the-system** is correct for the majority of apps, and when it
  is wrong it is wrong *loudly* — a dark card on a light app is glaring
  and takes one parameter to fix. Failure modes that announce themselves
  beat subtle ones.

**Known caveat, must be documented for users:** `isSystemInDarkTheme()`
reads the *device* setting, not the *app's* theme. An app that forces its
own scheme regardless of the system will get the wrong default and must
pass `.light()` or `.dark()` explicitly.

**This changes current behaviour** — today's defaults are hard-dark. With
roughly zero external users this is the cheapest moment it will ever be.
The three existing components must be re-verified in both schemes.

### Gates before any of this is built on

**Gate 1 — `isSystemInDarkTheme()` behaviour. PASSED 2026-08-05.**
It tracks the host environment rather than returning a constant, measured
rather than argued:

| Target | Host state | Reported | Correct? |
|---|---|---|---|
| Desktop (Windows) | OS dark (`AppsUseLightTheme=0`) | `true` | yes |
| Desktop (Ubuntu CI) | no dark preference | `false` | yes |
| Wasm (browser) | emulated **dark** | `true` | yes |
| Wasm (browser) | emulated **light** | `false` | yes |
| Wasm (browser) | emulated **dark** again | `true` | yes (A/B/A) |
| iOS simulator | default light appearance | `false` | yes |

The wasm check mirrors what Compose observes *inside composition* out to
the DOM — reading `prefers-color-scheme` from JS would only prove the
browser works, not that Compose sees it. **`.auto()` is safe to use as the
default on every target.**

**Android was not re-measured in this gate.** It is the API's home
platform and is not in doubt, but that is inference, not measurement.

**Known gap: live reactivity on wasm.** Toggling the scheme *without a
reload* did not change the observed value. That result is confounded —
recomposition on wasm is driven by `requestAnimationFrame`, which stalls
in a non-compositing viewport, so this proves nothing either way.
Startup-correctness is what `.auto()` actually needs and that is proven.
Re-test in a visible viewport if live-switching ever matters.

Remaining gates:

2. **Build and look at the light scheme.** The values in the table above
   are derived, not tested.
3. **Contrast-check light, don't just eyeball it.** The neon accent as a
   label colour already failed on light once (drift item 9).
4. **Previews must show both schemes**, not switch between them.

---

# Brand tokens — website, previews, demos ONLY

**Never referenced by installed component code.**

One caveat, carried over from the brief: the anti-reference has two
objections and only the first is component-scoped. "Only works in dark" is
functional and doesn't apply here — there is no host app. "It's the
current default of AI-generated UI" is a *differentiation* objection and
it applies to this surface too. So neon is permitted here in a way it
isn't in a component, but a site whose identity rests on dark-plus-neon
fails the exact job it exists to do. Loud has to mean an idea of our own.

| Token | Value | Use |
|---|---|---|
| `canvas` | `#0B0E14` | Site/demo background, preview backgrounds |
| `accent` | `#00E5FF` | Neon interactive elements, one accent, used sparingly |
| `textPrimary` | `#F2F5FA` | Titles, primary content |
| `textSecondary` | `#8A93A5` | Supporting text, labels |

These four were previously listed as component tokens. No component uses
any of them, and per the brief no component *can* — `canvas` is the host
app's background and `textPrimary`/`textSecondary` are the host's text.
They were always brand tokens; the file just didn't say so.

---

# Component quality bar

Check every one before a registry tag.

1. **Every visual value is an exposed parameter with a token default.**
   Restyle without touching logic. (Previously this bar read "uses only
   tokens — no ad-hoc colors", which read as a prohibition on the host's
   colours and contradicted the brief. The intent was always: no
   *undeclared* values.)
2. No hard-coded colour anywhere in the component, including internal
   helpers and hairlines.
3. All interaction states designed: rest, pressed, dragging, disabled.
4. Resolves correctly in **both** light and dark host schemes.
5. Recognisable in a grayscale screenshot — recognition rides on form,
   never on hue.
6. `compose.foundation` only. No Material. No new dependencies.
7. **Renders identically on Android, iOS and desktop.** No
   platform-specific drawing APIs — `Modifier.shadow`'s coloured
   `ambientColor`/`spotColor` is Android-only and must not carry a
   component's visual identity.
8. Compiles Android + desktop; run on a real screen; interactions
   exercised; screenshots reviewed (hard rule in `CLAUDE.md`).
9. Correct at 1x and 2.75x density, and at 640dp+ widths.
10. Handles window insets and exposes them as a parameter if it touches a
    screen edge.

---

# Drift audit — shipped components vs. this file

Found 2026-08-05 by reading the shipped sources, not assumed. **All 11
were fixed the same day in the local registry sources** and verified
per the hard rule — compiled on Android + desktop, then run on a real
screen with interactions exercised (see the verification log below).

⚠️ **These fixes are LOCAL ONLY. The published registry tag v0.3.1 still
serves the old code**, so `jetcompose add` installs the unfixed
components until a new registry tag is cut and `REGISTRY_BASE` is
bumped. That release is a founder decision, not done here.

| # | Where | Deviation | Fix |
|---|---|---|---|
| 1 | `NeonButton` | `glowColor = Color.Cyan` (`#00FFFF`) — not the `#00E5FF` accent | Default is now the accent |
| 2 | `NeonButton` | No pressed and no disabled state at all | `enabled` param + `interactionSource`; glow 0.25 disabled / 0.6 rest / 1.0 pressed over `motionFast` |
| 3 | `button_neon/ModifierExtensions.kt` | Glow used `Modifier.shadow(ambientColor/spotColor)` — **Android-only** | Rebuilt as layered widening/fading strokes, the same portable technique `neon_outline` uses |
| 4 | `button_neon/ModifierExtensions.kt` | `RoundedCornerShape(8.dp)` — off the 4/12/24 scale | `cornerRadius` param, defaults to 12dp `radiusMedium` |
| 5 | `bottomsheet_glass/ModifierExtensions.kt` | Hairline hard-coded `Color.White` — the reason the sheet was dark-only | `hairlineColor` param threaded from `GlassBottomSheet`; component applies the alpha ramp |
| 6 | `DragHandle.kt` | `Color.White @ 28%` hard-coded | `color` param threaded from `GlassBottomSheet` as `handleColor` |
| 7 | `BottomSheet.kt` | `fadeOut` 260ms — no such token | Now 220ms `motionExit`. **Decided 2026-08-05: keep 220.** The 40ms scrim overhang was not deliberate; scrim and card now leave together, which is what the code comment always claimed they did |
| 8 | `BottomSheet.kt` | Handle padding `10/14` — off the 4pt grid | `12/12`, identical 28dp total block height |
| 9 | `NeonOutlineButton` | `color` drove outline *and* label | Separate `contentColor`, defaulting to `color` so dark behaviour is unchanged |
| 10 | `NeonOutlineButton` | Type set inline | Replaceable `textStyle` param; still never sets `fontFamily` |
| 11 | `NeonButton` | No KDoc; default `clickable` indication | KDoc added; `indication = null` to match `neon_outline` |

## Verification log — 2026-08-05, Pixel_35 (API 35, 440dpi) + desktop

Both bars met. Android emulator is the API-35 one that enforces
edge-to-edge, and the testbed calls `enableEdgeToEdge()`.

| Check | Result |
|---|---|
| Compile, Android target | pass |
| Compile, desktop target | pass |
| `NeonButton` cross-platform glow | renders on **both** Android and desktop — the old Android-only shadow drew nothing comparable off-Android |
| `NeonButton` disabled | tap ignored (counter stayed at 2, not 3) — functional, not just dimmed |
| `NeonButton` enabled | taps registered |
| Sheet, sub-threshold nudge (100px @ ~111px/s) | springs back |
| Sheet, distance only (250px @ ~208px/s) | dismisses |
| Sheet, velocity only (120px @ ~800px/s) | dismisses |
| Sheet, scrim tap | dismisses |
| Sheet insets, API 35 gesture nav | card clear of the nav bar; scrim stays full-bleed |
| CLI `RewriterTest` | 12/12, unaffected |

**iOS is verified by execution as of 2026-08-05**, via the
`Verify components` workflow: 6/6 interaction tests run on a real
`iosSimulatorArm64` simulator on a hosted macOS runner, including
dismiss-on-drag and spring-back-on-nudge. The suite was mutation-checked
(breaking `enabled` on `NeonButton` failed exactly the test that guards
it), so it is sensitive rather than vacuous.

**What iOS verification still does not cover: appearance.** The tests
prove the components compose and that interactions behave, not that the
glow or glass *look* right on iOS. Visual confirmation there needs an
Xcode app bundle and `simctl io screenshot`; Android and desktop have
been confirmed visually, iOS has not.

**Still unfixed, because it is not drift** — the component *names*
`button/neon` and `button/neon_outline` still put the brief's named
anti-reference into the public install command. See below.

**Naming — needs a decision, not fixable here.** `button/neon` and
`button/neon_outline` put the brief's named anti-reference into the
public install command (`jetcompose add button/neon`). Component names
*are* public API, so renaming is a breaking change for the registry and
is deliberately left to the founder. If it happens, do it before there
are external users, when the cost is still zero.
