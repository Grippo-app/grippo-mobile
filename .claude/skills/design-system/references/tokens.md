# Tokens — the `AppTokens` facade

`AppTokens` is the **only** way Composables access design values. Hardcoded colors,
sizes, fonts, or strings inside UI code are forbidden. All values flow through
`AppTokens`, which reads from `CompositionLocal`s provided by `AppTheme(...)`. Lives in
`:design-system:core`.

## Object signature

```kotlin
@Stable
public object AppTokens {
    public val colors: AppColor
        @Composable @ReadOnlyComposable
        get() = LocalAppColors.current

    public val icons: AppIcon
        @Composable @ReadOnlyComposable
        get() = LocalAppIcons.current

    public val typography: AppTypography
        @Composable @ReadOnlyComposable
        get() = LocalAppTypography.current

    public val strings: AppString
        @Composable @ReadOnlyComposable
        get() = LocalAppStrings.current

    public val drawables: AppDrawable
        @Composable @ReadOnlyComposable
        get() = LocalAppDrawables.current

    public val dp: AppDp
        @Composable @ReadOnlyComposable
        get() = LocalAppDp.current
}
```

## Six token categories

| Property | Type | Purpose |
|---|---|---|
| `colors` | `AppColor` | Every color used in the UI (text, background, semantic, brand, charts, ...) |
| `icons` | `AppIcon` | Vector/painter resources for icons |
| `typography` | `AppTypography` | Text styles (`h1()`, `b14Bold()`, ...) |
| `strings` | `AppString` | String resource accessor (`res(Res.string.foo)`) |
| `drawables` | `AppDrawable` | Drawable resource accessor (`res(Res.drawable.bar)`) |
| `dp` | `AppDp` | Spacing, sizing, radius, icon sizes |

## Usage in Composables

```kotlin
Text(
    text = AppTokens.strings.res(Res.string.note_title),
    style = AppTokens.typography.h2(),
    color = AppTokens.colors.text.primary,
    modifier = Modifier.padding(horizontal = AppTokens.dp.screen.horizontalPadding),
)

Image(
    painter = AppTokens.drawables.res(Res.drawable.note_thumbnail),
    contentDescription = null,
    modifier = Modifier.size(AppTokens.dp.noteCard.icon),
)

Icon(
    imageVector = AppTokens.icons.Search,
    tint = AppTokens.colors.icon.primary,
    contentDescription = null,
)
```

`@ReadOnlyComposable` on the getter means the compiler skips
recomposition-scope/group bookkeeping for the call — no runtime tracking groups are
generated.

## Why `object` instead of `class`

- `AppTokens` is **stateless** — it just reads CompositionLocals. An `object` makes the
  API a noun without "create an instance".
- `@Stable` on the object signals Compose can skip recomposing functions that take
  `AppTokens` as a parameter (it never changes identity).
- All six accessors are `@Composable @ReadOnlyComposable val` — they cannot be cached
  outside `@Composable` context, which forces correct usage.

## Why six separate accessors (not one big record)

- `AppColor` is an **interface**, so an alternative implementation can be passed to a
  `PreviewContainer` variant for per-preview overrides. The remaining categories
  (`AppDp`, `AppTypography`, `AppString`, `AppDrawable`, `AppIcon`) are singletons
  (`object` / `@Stable data object`) and are not per-preview-overridable.
- Different categories have different change frequencies: typography rarely changes per
  theme, while colors flip on dark/light. Separate `CompositionLocal`s allow
  finer-grained recomposition.
- Six accessors are flat — no chained lookup like `AppTokens.theme.colors.text.primary`.
  Calls stay short.

## What `AppTokens` is NOT

- It is **not** a runtime resolver — every accessor is `@Composable`. You cannot call
  `AppTokens.colors.text.primary` from a ViewModel.
- It is **not** mutable — there's no `AppTokens.colors.text.primary = ...`. To change a
  token, swap the implementation via `AppTheme(...)`.
- It is **not** the only token source — text styles, dimensions, and resources are also
  configurable per-component (e.g. `WheelPicker(textStyle = ...)`). But for screens,
  `AppTokens` is the default.

## VM-side equivalent

In ViewModels (which aren't `@Composable`), the equivalent of `AppTokens.strings.res(...)`
is `StringProvider.get(...)`:

```kotlin
internal class FooViewModel(
    private val stringProvider: StringProvider,
) : ... {

    private suspend fun showNotification() {
        val title = stringProvider.get(Res.string.notification_title)
        val body = stringProvider.get(Res.string.notification_body, currentUserName)
        notificationManager.show(AppNotification(id, title, body), 7.days)
    }
}
```

There's no VM-side equivalent for colors/dp/typography — those are pure UI concerns. If
you ever feel the need to read a color in a ViewModel, restructure: pass a Boolean state
field to the UI and let the UI choose the color.

## CompositionLocal definitions

In `:design-system:core` (internal — the `Local*` keys are not part of the public API;
consumers go through `AppTokens`):

```kotlin
internal val LocalAppColors = staticCompositionLocalOf<AppColor> {
    error("No colors provided")
}
internal val LocalAppDp = staticCompositionLocalOf<AppDp> {
    error("No dp provided")
}

// ... one Local* per category (colors, icons, typography, strings, drawables, dp)
```

`staticCompositionLocalOf` (not `compositionLocalOf`) — these don't change frequently; the
static variant is more efficient (it triggers full subtree recomposition on change, which
is fine for theme switches because they're rare).

## `AppTheme` provides them

`AppTheme` is a thin public wrapper around an internal `ProvideResources` composable that
installs the `Local*` keys via `CompositionLocalProvider`:

```kotlin
@Composable
public fun AppTheme(
    darkTheme: Boolean,
    localeTag: String,
    content: @Composable () -> Unit,
) {
    ProvideResources(
        darkTheme = darkTheme,
        localeTag = localeTag,
        content = content,
    )
}

@Composable
internal fun ProvideResources(
    darkTheme: Boolean,
    localeTag: String,
    vararg values: ProvidedValue<*>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides /* light or dark AppColor impl */,
        LocalAppIcons provides AppIcon,
        LocalAppTypography provides AppTypography,
        LocalAppStrings provides AppString,
        LocalAppDrawables provides AppDrawable,
        LocalAppDp provides AppDp,
        *values,
        content = content,
    )
}
```

`AppTheme` is wrapped once at the root (`RootComponent.Render()`) and once in
`PreviewContainer`. Inside, every Composable reads tokens via `AppTokens.*`. (Full theme
contract: `theme.md`.)

In the template only a single `AppColor` implementation (`DarkColor`) is wired today,
and the `darkTheme` / `localeTag` parameters are not yet read inside `ProvideResources`.
The signature is still kept so a light theme or per-locale provider can be added without
changing call sites.

## When to add a new token

Add when:

- The **same value** appears in **two or more** Composables and could change per
  theme/product.
- The value has **semantic meaning** that justifies a name
  (`AppTokens.colors.text.disabled` is better than `AppTokens.colors.gray400`).

Don't add for:

- One-off values used in a single Composable parameter.
- Computed values. Use an existing semantic `AppDp` token, or add a named slot to the
  closest semantic group instead of writing call-site token math.

## When to add a new category

Don't, unless absolutely necessary. The six categories (`colors`, `icons`, `typography`,
`strings`, `drawables`, `dp`) cover everything. New categories would fragment the
namespace; expand within existing ones instead (e.g. add a new field to `AppDp` rather
than creating `AppSpacing`). A new top-level category is a **stop-and-ask** item.

## Anti-patterns

- **`Color(0xFF...)` in a feature module.** Forbidden — use `AppTokens.colors.*`.
- **`12.dp` for screen padding.** Forbidden — use `AppTokens.dp.screen.horizontalPadding`
  (or equivalent semantic token).
- **`TextStyle(fontSize = 14.sp, ...)` inline.** Forbidden — use
  `AppTokens.typography.b14Med()`.
- **`stringResource(...)` from `androidx.compose.ui.res`.** Forbidden — use
  `AppTokens.strings.res(Res.string.*)`.
- **`painterResource(R.drawable...)`.** Forbidden — use
  `AppTokens.drawables.res(Res.drawable.*)`.
- **Reading `AppTokens` outside `@Composable`.** Compile error (the getters are
  `@Composable`); use `StringProvider` instead.
