# `AppTheme` composable

`AppTheme(darkTheme, localeTag, content)` is the **root wrapper** that provides every
`CompositionLocal` consumed by `AppTokens`. It is called exactly once inside
`RootComponent.Render()` and once inside `PreviewContainer`.

## Signature

```kotlin
@Composable
public fun AppTheme(
    darkTheme: Boolean,
    localeTag: String,
    content: @Composable () -> Unit,
)
```

Lives in `:design-system:core`.

## Implementation shape

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
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean,
    @Suppress("UNUSED_PARAMETER") localeTag: String,
    vararg values: ProvidedValue<*>,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalAppColors provides DarkColor,
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

`ProvideResources` is the internal seam. `AppTheme` is the public face — clients pass the
inputs and don't see the `Local*` mechanics. The `vararg values: ProvidedValue<*>` slot lets
a feature inject extra composition locals (e.g. a feature-local theme) without bypassing the
design-system providers.

In the current the template the `darkTheme` and `localeTag` parameters are accepted but
**not yet read** — `ProvideResources` always wires `LocalAppColors` to the single `DarkColor`
implementation, and Compose Resources handles locale resolution on its own based on the
system locale. The parameters are reserved for the future light-theme / explicit-locale-override
wiring.

> **Two `AppTheme` symbols coexist by design.** The composable
> `AppTheme(darkTheme, localeTag) { … }` documented here lives in `:design-system:core`; the
> `AppTheme.current` you pass to its `darkTheme` is the **object** property from
> `:toolkit:theme` (the system dark/light flag). Same name, different modules — Kotlin
> resolves them by call site, and `RootComponent` imports both.

## Inputs

- **`darkTheme: Boolean`** — true for dark, false for light. Pass `AppTheme.current` from
  `:toolkit:theme` to follow the system theme; or a state-driven boolean if the product
  supports a manual theme override.
- **`localeTag: String`** — BCP-47 language tag (e.g. `"en-US"`, `"de-DE"`, `"fr-FR"`). Pass
  `AppLocale.current` from `:toolkit:localization`.

## Where it is called

### Once at app root

`AppTheme` is wrapped inside `RootComponent.Render()`, **not** inside `RootScreen`. The
screen composable itself only renders the navigation stack; the theme + locale + dialog
overlay live one level up so previews of `RootScreen` do not need to install the theme
themselves.

```kotlin
// shared/.../root/RootComponent.kt
@Composable
override fun Render() {
    val state = viewModel.state.collectAsStateMultiplatform()
    val loaders = viewModel.loaders.collectAsStateMultiplatform()

    val systemIsDark = AppTheme.current
    val systemLocaleTag = AppLocale.current

    LaunchedEffect(systemLocaleTag) {
        DateFormatting.install(systemLocaleTag)
    }

    AppTheme(darkTheme = systemIsDark, localeTag = systemLocaleTag) {
        RootScreen(this, state.value, loaders.value, viewModel)
        dialogComponent.Render()
    }
}
```

The `LaunchedEffect(systemLocaleTag)` ensures `DateTimeUtils.format(...)` uses the right
locale. The two `AppTheme.current` / `AppLocale.current` reads come from
`:toolkit:theme` / `:toolkit:localization` (`expect object` with a `@Composable val current`);
`AppLocale`'s non-Composable `.current()` sibling is used by `BackendClient.defaultRequest`
for the `Accept-Language` header.

### Once in `PreviewContainer`

```kotlin
@Composable
public fun PreviewContainer(content: @Composable ColumnScope.() -> Unit) {
    AppTheme(darkTheme = true, localeTag = "en") {
        Column(...) { content() }
    }
}
```

The preview is locked to a fixed theme/locale to keep previews predictable. (Full preview
contract: `previews.md`.)

## Locale switching

When `localeTag` changes:

1. Compose Resources picks the matching `values-XX/strings.xml` (Compose Resources reads
   `LocalLocale`).
2. `DateFormatting.install(localeTag)` is called via `LaunchedEffect(localeTag)` to refresh
   date formatters.
3. `BackendClient.defaultRequest` sees `AppLocale.current()` next time it builds a request →
   `Accept-Language` header updates.

Locale handling is **not** purely client-side: changing the language often means the server
returns localized strings (error messages, content). The `Accept-Language` header is the
lever.

## Theme switching

When `darkTheme` changes (target state — currently only `DarkColor` is wired; see above):

1. `AppTheme` switches `LocalAppColors` from `LightColor` to `DarkColor`.
2. Recomposition propagates — Composables reading `AppTokens.colors.*` get new values.
3. UI flips dark/light.

`staticCompositionLocalOf` (vs `compositionLocalOf`) is used for `LocalAppColors` because
theme changes are infrequent — the slight cost (full subtree recomposition) is acceptable,
and the runtime overhead during steady state is lower.

## Manual override (per-screen)

A specific screen can override the theme — wrap a subtree in another `AppTheme(...)`:

```kotlin
@Composable
internal fun DebugScreen(...) {
    AppTheme(darkTheme = false, localeTag = "en") {
        // ... always light, always English, regardless of system
    }
}
```

Rare. Reserved for debug screens, marketing splash, or onboarding flows that must look
consistent across devices.

## Why no Material3 `MaterialTheme` wrap

Material3's `MaterialTheme` is **not** wrapped by `AppTheme`. The project does not consume
`MaterialTheme.colorScheme.primary` etc. — every value flows through `AppTokens`. Material3
components inside `:design-system:components` use raw values passed in via parameters.

If a specific Material3 component (e.g. `ModalBottomSheet`) requires
`MaterialTheme.colorScheme`, wrap **that one component** in a minimal `MaterialTheme`
provider locally — don't add it to the root.

## Anti-patterns

- **Calling `AppTheme` twice in nested scopes** without a reason. Wasteful recomposition.
- **Passing a `darkTheme` state owned by a feature ViewModel** to `AppTheme`. Theme belongs
  to the root.
- **Wrapping individual screens in `AppTheme`** to override. Done only for debug/marketing.
- **Forgetting `LaunchedEffect(systemLocaleTag)` for date format install.** Dates will
  render in the wrong locale after a system language change.

## Stop-and-ask

Edits to `AppTheme.kt`, composition-local wiring, the `AppTokens.kt` aggregator, or any
`:design-system:core/build.gradle.kts` are stop-and-ask items — they are not routine
design-system token/component work.
