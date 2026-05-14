# `AppTypography` — Type Tokens

`AppTokens.typography` exposes a fixed set of **named text styles**. Composables never construct `TextStyle(...)` inline; they pick a token like `AppTokens.typography.b14Med()`.

## Shape

```kotlin
@Stable
public data object AppTypography {

    @Composable
    public fun h1(): TextStyle
    @Composable
    public fun h2(): TextStyle
    @Composable
    public fun h3(): TextStyle
    @Composable
    public fun h4(): TextStyle
    @Composable
    public fun h5(): TextStyle
    @Composable
    public fun h6(): TextStyle

    @Composable
    public fun b15Med(): TextStyle

    @Composable
    public fun b14Bold(): TextStyle
    @Composable
    public fun b14Semi(): TextStyle
    @Composable
    public fun b14Med(): TextStyle

    @Composable
    public fun b13Semi(): TextStyle
    @Composable
    public fun b13Bold(): TextStyle
    @Composable
    public fun b13Med(): TextStyle

    @Composable
    public fun b12Semi(): TextStyle
    @Composable
    public fun b12Med(): TextStyle

    @Composable
    public fun b11Bold(): TextStyle
    @Composable
    public fun b11Semi(): TextStyle
    @Composable
    public fun b11Med(): TextStyle
    @Composable
    public fun b11Reg(): TextStyle

    @Composable
    public fun b10Bold(): TextStyle
    @Composable
    public fun b10Semi(): TextStyle
    @Composable
    public fun b10Med(): TextStyle
    @Composable
    public fun b10Reg(): TextStyle
}
```

Each method returns a `TextStyle` built from the product's font family (`manrope()` in the reference repo) with explicit `fontSize`, `lineHeight`, and `fontWeight`:

```kotlin
@Composable
public fun h1(): TextStyle = TextStyle(
    fontSize = 30.sp,
    fontFamily = manrope(),
    lineHeight = 38.sp,
    fontWeight = FontWeight.Bold,
)

@Composable
public fun b14Med(): TextStyle = TextStyle(
    fontSize = 14.sp,
    fontFamily = manrope(),
    lineHeight = 20.sp,
    fontWeight = FontWeight.Medium,
)
```

## Naming convention

- **`hN()`** — headings (H1–H6). Sizes step down: 30/26/22/20/18/16 sp (or your scale).
- **`bNNWeight()`** — body text, e.g. `b14Med` = "body 14sp, Medium weight".
- Weights abbreviated: `Reg` (Regular/Normal), `Med` (Medium 500), `Semi` (SemiBold 600), `Bold` (Bold 700).
- Sizes that appear in the API: 15, 14, 13, 12, 11, 10 — fine-grained because UIs blend in 1-sp steps.

## Font family

A single typeface is used app-wide (e.g. Manrope):

```kotlin
@Composable
internal fun manrope(): FontFamily = FontFamily(
    Font(AppFont.manrope_bold, weight = FontWeight.Bold),
    Font(AppFont.manrope_extra_bold, weight = FontWeight.ExtraBold),
    Font(AppFont.manrope_extra_light, weight = FontWeight.ExtraLight),
    Font(AppFont.manrope_light, weight = FontWeight.Light),
    Font(AppFont.manrope_medium, weight = FontWeight.Medium),
    Font(AppFont.manrope_regular, weight = FontWeight.Normal),
    Font(AppFont.manrope_semi_bold, weight = FontWeight.SemiBold),
)
```

`AppFont` (`Res.font.manrope_bold`, etc.) is in `:design-system:resources:provider`. Font files live in `commonMain/composeResources/font/`.

Multiple typefaces are rare — usually one font with multiple weights covers a product. If a second typeface is needed (display vs body), add a parallel `oswald()` helper and route specific token methods through it.

## `@Composable` on every method

Each token getter is `@Composable` because:

- Some style values may eventually depend on the locale (CJK text needs different line-height handling).
- Some products tweak font sizes for accessibility (system text scale).
- Compose Resources' `Res.font.*` returns a Compose-aware `FontResource` that must be loaded inside `@Composable`.

Composing a `TextStyle` inside a `@Composable` adds negligible overhead (recomposition is skipped when inputs are stable).

## Why methods, not properties

`@Composable val` properties cannot have a backing field; they must use a getter. The code shape:

```kotlin
public val h1: TextStyle
    @Composable get() = TextStyle(...)
```

vs:

```kotlin
@Composable
public fun h1(): TextStyle = TextStyle(...)
```

The reference repo chooses the **method** form. Both are valid; methods are slightly more idiomatic for "compute on every call".

## Adding a new token

1. Confirm the design lists this size + weight in the type scale.
2. If a size that hasn't been used appears, add a method following the naming pattern.
3. Add the method to `AppTypography`. **Don't** modify existing methods (they're committed UI dimensions).

## What MUST NOT happen

- **`TextStyle(...)` inline in a feature module.** Forbidden.
- **`fontSize = 14.sp` somewhere outside `AppTypography`.** Forbidden.
- **`Color(...)` set on a `TextStyle` at the token site.** Color is passed separately (`Text(color = AppTokens.colors.text.primary)`). Tokens are color-agnostic.
- **`LocalTextStyle.current` for the default style.** Always pick a token explicitly.

## Anti-patterns

- **Composing token text styles with `.copy(fontWeight = ...)`** — that's a new style; add a token.
- **Mixing font families across the app** without a second `<typeface>()` helper.
- **Adding a `material3.Typography` parallel to `AppTypography`.** Material3's typography is bypassed entirely; we don't read it.
