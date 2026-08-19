package com.lamintra.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/*
 * The theme.
 *
 * WHY THIS EXISTS. Before 2026-08-11 every component carried its own private
 * copy of the palette. Counted across the six: fifteen distinct colour
 * literals, ten of them duplicated across files, and #09090B and #FAFAFA
 * appearing in all six. Changing a brand's ink colour meant editing six files
 * and hoping you found them all. Miss one and the app has a visible seam,
 * which is the exact inconsistency this project claims to remove.
 *
 * So there was no theme. There were six components that happened to agree.
 *
 * THREE TIERS, ONE DIRECTION. The structure is the one every production token
 * system converges on, and the dependency arrow only ever points one way:
 *
 *   primitive  ->  semantic  ->  component
 *   LamintraPalette   LamintraColors   LamintraButtonColors and friends
 *   "what it is"      "what it means"  "where it is used"
 *
 * Primitives are named for their value and are NOT read by components.
 * Semantics are named for their purpose, never their value, because a token
 * called `lime` has to be renamed on the day someone rebrands and a token
 * called `accent` does not. The per-component colour classes were already the
 * third tier before this file existed; all that changed is where they get
 * their values from.
 *
 * WHAT THIS IS NOT. It is not a palette we impose. design/TOKENS.md draws a
 * hard line between the BRAND surface, which may be loud because it has no
 * host to survive, and COMPONENT code, which installs into a stranger's app
 * whose colours we never see. Every value below is a DEFAULT the host is
 * expected to replace. Nothing here structures a component; it only supplies
 * the values a component would otherwise hard-code.
 *
 * Reference points, because this was checked rather than guessed. Wise ships
 * eighteen colour tokens in total: a four-stop accent ramp, four surface
 * neutrals and a semantic positive/warning/negative set. Their CTA is always
 * the same green, never substituted and never reused as a status colour. The
 * discipline worth stealing is the SIZE of the set, not the hues: a small
 * vocabulary that a person can hold in their head. This file ships fifteen.
 */

/**
 * Tier 1. Raw values, no meaning attached.
 *
 * Do not read these from a component. They exist so the semantic layer below
 * has something to point at, and so a value that appears in two schemes is
 * written down once.
 *
 * The names describe the value, which is exactly why components must not use
 * them: `ink900` tells you nothing about whether it is text or a fill.
 */
object LamintraPalette {
    // Neutral ramp. This does most of the work on any screen, which is the
    // part people miss when they copy a palette: the accent is 10% of the
    // pixels at most.
    val White = Color(0xFFFFFFFF)
    val Neutral50 = Color(0xFFFAFAFA)
    val Neutral100 = Color(0xFFF4F4F5)
    val Neutral200 = Color(0xFFE4E4E7)
    val Neutral500 = Color(0xFF70707B)
    val Neutral600 = Color(0xFF8B8B90)
    val Neutral800 = Color(0xFF26262A)
    // Raised from #141416 on 2026-08-17. Against surface (#09090B) the old value
    // measured 1.07:1, which is below what an eye resolves, so every container
    // in the dark scheme sat visually flat on the ground it was supposed to be
    // lifted off - most visibly the sheet, which lost its own edge. This is
    // 1.16:1, close to the step iOS uses from systemBackground to
    // secondarySystemBackground. Still between Neutral950 and Neutral800, so the
    // scale stays ordered.
    val Neutral900 = Color(0xFF1C1C20)
    val Neutral950 = Color(0xFF09090B)
    val Black = Color(0xFF0A0A0B)
    val TrueBlack = Color(0xFF000000)

    // Accent. Two values, and they are NOT the same hue lightened.
    // Full lime measures 1.18:1 against a white page, so on light the track of
    // a switch would dissolve into the page and read as permanently off. The
    // olive measures 5.57:1 against white. See design/TOKENS.md.
    // Opt-in since 2026-08-17, not defaults. The switch was the only consumer
    // of `accent`, so a single acid green appeared twice on a screen that was
    // otherwise white, black and one red - which read as a brand colour chosen
    // by accident. The system is monochrome now and `accent` resolves to `ink`.
    // Both stay here because they are still how you put the colour back:
    // LamintraSwitch.dark(accent = LamintraPalette.Lime).
    val Lime = Color(0xFFC8FF34)
    val Olive = Color(0xFF57710A)

    // Status. Only danger exists because only danger is used: the button's
    // Destructive emphasis. Positive and warning are a deliberate GAP rather
    // than an oversight, and belong here the day a component needs them.
    val Red500 = Color(0xFFF04438)
    val Red600 = Color(0xFFD92D20)
}

/**
 * Tier 2. What a component reads. Named for purpose, never for value.
 *
 * Fifteen slots. Adding a sixteenth should require an argument, because the
 * value of a token set is inversely proportional to how many of them a person
 * has to remember. `shadow` was the fifteenth and its argument is written on
 * the field itself.
 */
@Immutable
data class LamintraColors(
    /**
     * The page the HOST owns. Components almost never paint this: it is here
     * so a component can compute a colour that sits correctly on it, and so a
     * preview has something to draw on.
     */
    val surface: Color,
    /**
     * What a container is filled with: a card, a text field, a well.
     *
     * NOT called "raised" or "sunk", because it is neither consistently. On
     * dark it is lighter than the page and on light it is darker, so a name
     * borrowed from elevation would be a lie in one of the two schemes. It is
     * named for the role it plays instead.
     */
    val container: Color,
    /** A recessed container: a code block, the inside of a bar. */
    val containerSunk: Color,

    /** Primary text, and the fill of a primary action. Ink carries action. */
    val ink: Color,
    /** Body text, secondary labels. */
    val inkMuted: Color,
    /** Placeholders, captions, table headers. The quietest readable step. */
    val inkFaint: Color,
    /** Text that sits ON ink, such as the label of a filled button. */
    val onInk: Color,

    /** Dividers, and the resting contour of an input. */
    val hairline: Color,
    /** A contour that has to be seen: a focused field, an interactive edge. */
    val hairlineStrong: Color,

    /**
     * State, and only state. Ink carries action, accent carries state, and the
     * two never compete.
     *
     * This is the one load-bearing colour in the library: a switch's on-state
     * track. Everywhere else colour decorates a form that already reads
     * without it, which is what lets the set survive a grayscale screenshot.
     * A host rebranding Lamintra changes this first.
     */
    val accent: Color,
    /** Text or an icon sitting on the accent. */
    val onAccent: Color,

    /** Destructive actions only. Never a decoration. */
    val danger: Color,
    /** Text sitting on danger. */
    val onDanger: Color,

    /**
     * The focus ring. Ink rather than accent, deliberately: accent means state
     * here, and a focus ring is not a state of the control, it is a state of
     * the keyboard.
     */
    val focus: Color,

    /**
     * What depth is drawn in, under the one kind of object allowed to float.
     *
     * This is the fifteenth token, and the comment above this class says a
     * fifteenth needs an argument, so here it is. The value cannot be derived
     * from any other token: on dark it is true black, and on light it is the
     * ink value, and no rule connects those two. Leaving it out forced the
     * switch to reach past the semantic layer into the raw palette AND to
     * re-resolve the colour scheme for itself, which is two violations to
     * avoid adding one field.
     *
     * Exactly one component uses it, by design. See rule 8 in
     * design/TOKENS.md: depth appears once, under a small object that floats.
     */
    val shadow: Color
)

/**
 * The dark scheme.
 *
 * Every parameter has a default, so a host that likes fifteen of the sixteen
 * values overrides one and keeps the rest:
 *
 *     lamintraDarkColors(accent = MyBrand.Green)
 */
fun lamintraDarkColors(
    surface: Color = LamintraPalette.Neutral950,
    container: Color = LamintraPalette.Neutral900,
    containerSunk: Color = LamintraPalette.Black,
    ink: Color = LamintraPalette.Neutral50,
    inkMuted: Color = LamintraPalette.Neutral600,
    inkFaint: Color = LamintraPalette.Neutral600,
    onInk: Color = LamintraPalette.Black,
    hairline: Color = LamintraPalette.Neutral800,
    hairlineStrong: Color = LamintraPalette.Neutral50,
    accent: Color = LamintraPalette.Neutral50,
    onAccent: Color = LamintraPalette.Black,
    danger: Color = LamintraPalette.Red500,
    onDanger: Color = LamintraPalette.White,
    focus: Color = LamintraPalette.Neutral50,
    shadow: Color = LamintraPalette.TrueBlack
): LamintraColors = LamintraColors(
    surface = surface,
    container = container,
    containerSunk = containerSunk,
    ink = ink,
    inkMuted = inkMuted,
    inkFaint = inkFaint,
    onInk = onInk,
    hairline = hairline,
    hairlineStrong = hairlineStrong,
    accent = accent,
    onAccent = onAccent,
    danger = danger,
    onDanger = onDanger,
    focus = focus,
    shadow = shadow
)

/**
 * The light scheme.
 *
 * Not the dark one inverted. Two values disagree on purpose:
 *
 * - `accent` is a darkened olive rather than the brand lime, because full lime
 *   on a white page measures 1.18:1 and the control would vanish.
 * - `container` is DARKER than the page here and LIGHTER than it on dark.
 *   A light page has room below it; a near-black page does not.
 */
fun lamintraLightColors(
    surface: Color = LamintraPalette.White,
    container: Color = LamintraPalette.Neutral100,
    containerSunk: Color = LamintraPalette.Neutral100,
    ink: Color = LamintraPalette.Neutral950,
    inkMuted: Color = LamintraPalette.Neutral500,
    inkFaint: Color = LamintraPalette.Neutral500,
    onInk: Color = LamintraPalette.White,
    hairline: Color = LamintraPalette.Neutral200,
    hairlineStrong: Color = LamintraPalette.Neutral950,
    accent: Color = LamintraPalette.Neutral950,
    onAccent: Color = LamintraPalette.White,
    danger: Color = LamintraPalette.Red600,
    onDanger: Color = LamintraPalette.White,
    focus: Color = LamintraPalette.Neutral950,
    shadow: Color = LamintraPalette.Neutral950
): LamintraColors = LamintraColors(
    surface = surface,
    container = container,
    containerSunk = containerSunk,
    ink = ink,
    inkMuted = inkMuted,
    inkFaint = inkFaint,
    onInk = onInk,
    hairline = hairline,
    hairlineStrong = hairlineStrong,
    accent = accent,
    onAccent = onAccent,
    danger = danger,
    onDanger = onDanger,
    focus = focus,
    shadow = shadow
)

/**
 * Motion.
 *
 * These three specs were copy-pasted into all six components before this file
 * existed: `dampingRatio 0.72 / stiffness 1400` appeared six times.
 *
 * Springs rather than fixed-duration curves, because a spring carries velocity
 * through an interruption and a duration curve cannot, which is the whole of
 * what "smooth" means in an iOS or well-built React Native app.
 */
object LamintraMotion {
    /** A press. Inside the 100-160ms band a finger reads as immediate. */
    fun <T> press(): SpringSpec<T> = spring(dampingRatio = 0.72f, stiffness = 1400f)

    /** Something travelling across a distance: a switch knob, a thumb. */
    fun <T> travel(): SpringSpec<T> = spring(dampingRatio = 0.68f, stiffness = 700f)

    /**
     * A fade. Critically damped on purpose: a bouncing opacity reads as a bug
     * rather than as life.
     */
    fun <T> fade(): SpringSpec<T> =
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 700f)
}

/**
 * The default when no [LamintraTheme] wraps the content.
 *
 * `staticCompositionLocalOf` rather than `compositionLocalOf` because the theme
 * changes approximately never, and the static form skips the invalidation
 * bookkeeping that the dynamic form pays for on every read.
 *
 * The default matters more than it looks. A component dropped into an app that
 * has never heard of Lamintra still has to render correctly, so this cannot be
 * an error or an empty object. It resolves per scheme at the point of use.
 */
val LocalLamintraColors = staticCompositionLocalOf<LamintraColors?> { null }

/**
 * Wrap your app once, or do not wrap it at all.
 *
 *     LamintraTheme {
 *         SettingsScreen()
 *     }
 *
 * To rebrand, pass a scheme with the values you want changed:
 *
 *     LamintraTheme(
 *         dark = lamintraDarkColors(accent = MyBrand.Green),
 *         light = lamintraLightColors(accent = MyBrand.GreenDark)
 *     ) { App() }
 *
 * Both schemes are taken rather than one resolved colour set, so the theme
 * still follows the device when the device changes underneath it.
 */
@Composable
fun LamintraTheme(
    dark: LamintraColors = lamintraDarkColors(),
    light: LamintraColors = lamintraLightColors(),
    isDark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLamintraColors provides if (isDark) dark else light,
        content = content
    )
}

/** Accessor, so component code reads `LamintraTheme.colors.ink`. */
object LamintraTheme {
    /**
     * Falls back to the scheme-appropriate defaults when no provider is above
     * this point in the tree, which is what keeps a single component usable in
     * an app that never adopted the theme.
     */
    val colors: LamintraColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLamintraColors.current
            ?: if (isSystemInDarkTheme()) lamintraDarkColors() else lamintraLightColors()
}
