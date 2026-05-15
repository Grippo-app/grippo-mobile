# `AppColor` — Color Tokens

`AppColor` is the interface exposed via `AppTokens.colors`. It declares a flat list of **nested color groups**, each containing semantic color slots.

## Top-level groups

```kotlin
public interface AppColor {
    public val button: ButtonColors
    public val input: InputColors
    public val background: BackgroundColors
    public val dialog: DialogColors
    public val text: TextColors
    public val divider: DividerColors
    public val semantic: SemanticColors
    public val overlay: OverlayColors
    public val border: BorderColors
    public val brand: BrandColors
    public val icon: IconColors
    public val toggle: ToggleColors
    public val muscle: MuscleColors        // product-specific (rename per product)
    public val segment: SegmentColors
    public val konfetti: Konfetti
    public val charts: Charts
    public val example: ExampleColors
    public val profile: ProfileColors      // product-specific
    public val palette: PaletteColors
    public val static: Static
    public val context: ContextColors
    public val selectableCardColors: SelectableCardColors

    public interface TextColors { /* ... */ }
    public interface BackgroundColors { /* ... */ }
    // ... every `*Colors` interface is nested inside `AppColor`
}
```

Each group is a nested interface inside `AppColor` with `val` properties of type `androidx.compose.ui.graphics.Color`. Nested interfaces (not flat top-level types) keep the namespace scoped and avoid name collisions with unrelated `TextColors`/`IconColors` from other libraries.

## Representative groups

```kotlin
public interface TextColors {
    public val primary: Color
    public val secondary: Color
    public val tertiary: Color
    public val disabled: Color
}

public interface BackgroundColors {
    public val screen: Color
    public val dialog: Color
    public val card: Color
}

public interface BorderColors {
    public val default: Color
    public val focus: Color
}

public interface SemanticColors {
    public val success: Color
    public val error: Color
    public val warning: Color
    public val info: Color
    public val notice: Color
}

public interface IconColors {
    public val primary: Color
    public val secondary: Color
    public val tertiary: Color
    public val disabled: Color
}

public interface ButtonColors {
    // Primary / Secondary / Tertiary / Transparent / Disabled variants are
    // expressed as flat sibling slots rather than a nested per-variant interface.
    public val backgroundPrimary1: Color
    public val backgroundPrimary2: Color
    public val borderPrimary: Color
    public val textPrimary: Color
    public val iconPrimary: Color

    public val backgroundSecondary1: Color
    public val backgroundSecondary2: Color
    public val borderSecondary: Color
    public val textSecondary: Color
    public val iconSecondary: Color

    public val textTertiary: Color
    public val iconTertiary: Color
    public val borderTertiary: Color

    public val textTransparent: Color
    public val iconTransparent: Color

    public val backgroundDisabled: Color
    public val contentDisabled: Color
}
```

The structure stays mostly **two levels deep** — `AppTokens.colors.button.backgroundPrimary1`, `AppTokens.colors.text.primary`. A handful of groups (`profile.experience.beginner`, `example.category.compound`, `charts.ring.success.indicator`) go three levels for sub-domains; don't go deeper than three — flatten when in doubt.

## Light vs dark theme

The shape allows for multiple per-theme implementations (e.g. `LightAppColors`, `DarkAppColors`) wired in via `AppTheme(darkTheme = ...)`:

```kotlin
public object DarkColor : AppColor {
    override val text: AppColor.TextColors = object : AppColor.TextColors {
        override val primary = AppPalette.NeutralDark.N800
        override val secondary = AppPalette.NeutralDark.N700
        override val tertiary = AppPalette.NeutralDark.N500
        override val disabled = AppPalette.NeutralDark.N400
    }
    // ... every group implemented
}
```

Concrete colors are stored in `AppPalette` (internal — `NeutralDark.N050..N800` with intermediate halves like `N150`/`N250`/`N450`/`N550`, `Common.White/Black`, `Blue.P300..P900`, `Unique.Red/Orange/Coral/...`, plus a `Gradient` bucket of `List<Color>` palettes) and the per-theme object only routes palette entries into the semantic slots. Each implementation exposes the **same** `AppColor` interface; only the values differ. Composables don't know which theme is active — they just read `AppTokens.colors.text.primary` and get the right color.

> The reference repo currently ships only `DarkColor`. The `AppTheme(darkTheme = ...)` parameter is reserved for adding a `LightColor` peer later; until then the `darkTheme` flag is accepted but not yet consumed by `ProvideResources`.

## Adding a new color slot

1. Decide the **semantic** name. `text.primary`, not `gray900`.
2. Add the field to the relevant group interface in `:design-system:resources:provider/AppColor.kt`.
3. Provide values for both Light and Dark implementations.
4. Use it via `AppTokens.colors.<group>.<slot>` in Composables.

If a new slot doesn't fit any existing group, add a new group — but ask first whether it's really needed; the existing groups are exhaustive for most cases.

## Product-specific groups

The reference repo has `muscle`, `profile`, `konfetti`, `charts` — these are product-tied. When forking these requirements:

- Keep the **structural** groups (`button`, `input`, `background`, `dialog`, `text`, `divider`, `semantic`, `border`, `brand`, `icon`, `toggle`, `segment`, `palette`).
- Drop or replace product-specific groups (`muscle`, `profile`).
- Add new product-specific groups as needed (`payment.success`, `tournament.banner`, ...).

## Rules

- **Every color access uses `AppTokens.colors.*`.** No `Color(0xFF...)` literals in feature modules.
- **Names are semantic, not visual.** `text.primary` (semantic) not `gray900` (visual). Visual names break when themes change.
- **No alpha-blending at the call site.** If a state needs transparency, add a slot (`text.disabled` with built-in transparency) rather than computing `text.primary.copy(alpha = 0.4f)` inline.
- **`Color` only.** Never `Brush` in tokens — gradients are usually one-off and live in the Composable that uses them.
- **Theme-aware tints work via the token system.** A "disabled" icon doesn't manually fade `primary`; it uses `icon.disabled` which already has the right value per theme.

## Why an interface + per-theme impl (not a `data class`)

Two reasons:

1. **Per-theme implementations** can compute or share values across groups (e.g. `border.default` might reuse `divider.default`, or `icon.disabled` can be derived from `text.disabled`). A `data class` constructor would require duplicating values; an interface lets the impl chose.
2. **Test/preview overrides** are easier — pass an alternative `AppColor` implementation to a `PreviewContainer` variant.

## Reading colors at the call site

```kotlin
Surface(
    color = AppTokens.colors.background.card,
    shape = RoundedCornerShape(AppTokens.dp.input.radius),
    border = BorderStroke(width = 1.dp, color = AppTokens.colors.border.default),
) {
    Text(
        text = AppTokens.strings.res(Res.string.label),
        color = AppTokens.colors.text.primary,
        style = AppTokens.typography.b14Med(),
    )
}
```

The `dp` example pulls a radius from a component-scoped group (`input.radius`); there is no public `AppTokens.dp.radius.*` path (the `Radius` scale is private inside `AppDp`). Pick the closest semantic group (`bottomSheet.radius`, `bannerCard.radius`, ...) or add one if none fits — see `03-app-dp.md`.

Stylistically: prefer one token lookup per parameter line. Don't chain lookups into pseudo-functions.
