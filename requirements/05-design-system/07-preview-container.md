# `@AppPreview` and `PreviewContainer`

Every Screen and every reusable Composable has a preview directly below its definition. Two helpers make previews concise and consistent:

- **`@AppPreview`** — a multi-preview annotation that expands to two `@Preview` configurations (small UK phone + big EN phone).
- **`PreviewContainer { ... }`** — wraps preview content in `AppTheme(darkTheme = true)`, a Coil image preview handler, and a padded `Column`.

## `@AppPreview` annotation

```kotlin
@Preview(
    name = "Phone • UK • Small",
    group = "📱 Phone",
    widthDp = 360, heightDp = 800,
    locale = "uk",
)
@Preview(
    name = "Phone • EN • Big",
    group = "📱 Phone",
    widthDp = 412, heightDp = 915,
    locale = "en",
)
public annotation class AppPreview
```

Applied to a `private @Composable fun ...Preview()`:

- **Two configurations** — a smaller UK device and a larger EN device — catch issues with localization (longer strings in some languages) and density (narrow vs wide screens).
- **Grouped** under "📱 Phone" for IDE filtering.
- **No light/dark split** by default — `PreviewContainer` is dark-themed; if a screen needs a light variant, add a separate `@AppPreviewLight` annotation or pass a parameter.

For tablet or landscape previews, define additional multi-previews (`@AppPreviewTablet`, `@AppPreviewLandscape`) following the same pattern.

## `PreviewContainer` composable

```kotlin
@OptIn(ExperimentalCoilApi::class)
@Composable
public fun PreviewContainer(
    content: @Composable ColumnScope.() -> Unit,
) {
    val previewHandler = AsyncImagePreviewHandler {
        ColorImage(Color.Black.copy(alpha = 0.2f).toArgb())
    }

    CompositionLocalProvider(LocalAsyncImagePreviewHandler provides previewHandler) {
        Column {
            AppTheme(darkTheme = true, localeTag = "en") {
                Column(
                    modifier = Modifier
                        .background(Color.Black)
                        .padding(12.dp),
                    content = content,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                )
            }
        }
    }
}
```

What it provides:

- **`AppTheme(darkTheme = true, localeTag = "en")`** — uniform theme + locale for all previews.
- **`AsyncImagePreviewHandler`** — Coil image previews render a fallback color instead of trying to fetch from network in the IDE.
- **Padded `Column`** — visual breathing room for nested content.
- **Black background** — matches the dark-theme screen background.

## Usage pattern

```kotlin
@Composable
internal fun ProfileBodyScreen(
    state: ProfileBodyState,
    loaders: ImmutableSet<ProfileBodyLoader>,
    contract: ProfileBodyContract,
) {
    BaseComposeScreen(background = ScreenBackground.Color(AppTokens.colors.background.screen)) {
        // ... real content
    }
}

@AppPreview
@Composable
private fun ProfileBodyScreenPreview() {
    PreviewContainer {
        ProfileBodyScreen(
            state = ProfileBodyState(
                weight = WeightFormatState.of(72f),
                height = HeightFormatState.of(180),
                history = stubWeightHistoryList(),
                user = stubUser(),
            ),
            loaders = persistentSetOf(),
            contract = ProfileBodyContract.Empty,
        )
    }
}
```

Rules:

- **Preview function is `private`**, named `<ScreenName>Preview` (matches the screen).
- **Lives in the same file** as the screen.
- **Uses stub data** from `:ui-core:state` (`stubWeightHistoryList()`, `stubUser()`, ...).
- **Uses `Contract.Empty`** for the contract argument.
- **One preview per Composable**. Multiple variants get multiple `*Preview` functions (`...EmptyStatePreview()`, `...LoadingPreview()`).

## Stub data

Stubs live in `:ui-core:state` next to the corresponding State class:

```kotlin
// :ui-core:state/.../WeightHistoryState.kt
public fun stubWeightHistoryList(): ImmutableList<WeightPoint> = persistentListOf(
    WeightPoint(date = LocalDate(2026, 1, 1), value = 72.5f),
    WeightPoint(date = LocalDate(2026, 1, 8), value = 72.1f),
    WeightPoint(date = LocalDate(2026, 1, 15), value = 71.8f),
)

// stubUser() lives next to User-related state
public fun stubUser(): User = User(
    id = "preview-user",
    name = "Alex",
    email = "alex@example.com",
    weight = 72.5f,
    height = 180,
)
```

Single source of realistic preview data: all previews use the same stubs.

## Why dark by default

The product's primary identity in the reference is dark-themed. Light theme has fewer surfaces and edge cases — previewing dark catches more issues.

For light-theme previews, add a `@AppPreviewLight` annotation or a per-screen light variant:

```kotlin
@AppPreview
@Composable
private fun FooScreenPreview() {
    PreviewContainer { FooScreen(...) }
}

@Preview(name = "Phone • EN • Light", widthDp = 412, heightDp = 915, locale = "en")
@Composable
private fun FooScreenLightPreview() {
    Column(Modifier.background(Color.White).padding(12.dp)) {
        AppTheme(darkTheme = false, localeTag = "en") {
            FooScreen(...)
        }
    }
}
```

## Coil preview handler

Coil's `AsyncImagePreviewHandler` resolves all `AsyncImage(...)` calls to a fixed color in previews. Without it, Coil would try to load from network in the IDE — slow and unreliable.

The handler returns a translucent black square. For richer previews (a real avatar), pass an explicit `ContentScale.Fit` `painter` to the Composable instead of relying on `AsyncImage`.

## Anti-patterns

- **Plain `@Preview`** without `@AppPreview`. Single-config previews miss density/locale issues.
- **Preview without `PreviewContainer`**. Missing tokens (`AppTokens.colors.background.screen` is null) — screen renders blank.
- **Constructing stub data inline in the preview** — copy-paste of stub values across previews diverges.
- **Calling a real `ViewModel`** in a preview. The preview should use `Contract.Empty` or a lightweight inline implementation. ViewModels need Koin, which isn't available in previews.
- **Preview function `public`**. Previews are file-local; `private`.
- **Preview takes parameters.** Compose previews don't support parameters (use `@PreviewParameter` if absolutely needed, but prefer multiple `*Preview` functions).
