# `:toolkit:theme` and `:toolkit:localization`

Two small toolkit modules expose the **system dark/light theme** and the **system locale** as Composable + non-Composable accessors.

## `:toolkit:theme` — `AppTheme.current`

```kotlin
// commonMain
public expect object AppTheme {
    public val current: Boolean
        @Composable get

    public fun current(): Boolean
}
```

### Android

```kotlin
public actual object AppTheme {
    public actual val current: Boolean
        @Composable get() =
            (LocalConfiguration.current.uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES

    public actual fun current(): Boolean {
        val uiMode = Resources.getSystem().configuration.uiMode
        return (uiMode and UI_MODE_NIGHT_MASK) == UI_MODE_NIGHT_YES
    }
}
```

### iOS

```kotlin
public actual object AppTheme {
    public actual val current: Boolean
        @Composable get() = LocalSystemTheme.current == SystemTheme.Dark

    public actual fun current(): Boolean {
        val style = UIScreen.mainScreen.traitCollection.userInterfaceStyle
        return style == UIUserInterfaceStyle.UIUserInterfaceStyleDark
    }
}
```

`true` = dark, `false` = light.

### Usage

```kotlin
// inside RootScreen
val systemDarkTheme = AppTheme.current
AppTheme(darkTheme = systemDarkTheme, localeTag = ...) {
    // ...
}
```

`AppTheme.current` follows the system. If the product offers a manual theme override (Light/Dark/System), the override is read from `:data-services:datastore` and passed instead.

## `:toolkit:localization` — `AppLocale.current`

```kotlin
// commonMain
public expect object AppLocale {
    public val current: String
        @Composable get

    public fun current(): String
}
```

Returns a BCP-47 language tag: `"en"`, `"en-US"`, `"uk-UA"`, `"ru-RU"`.

### Android

```kotlin
public actual object AppLocale {
    public actual val current: String
        @Composable get() {
            val appTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            if (appTags.isNotBlank()) return appTags
            val conf = LocalConfiguration.current
            val confTags = conf.locales.toLanguageTags()
            if (confTags.isNotBlank()) return confTags
            return Locale.getDefault().toLanguageTag()
        }

    public actual fun current(): String = Locale.getDefault().toLanguageTag()
}
```

The Composable accessor prefers `AppCompatDelegate.getApplicationLocales()` (the user's per-app language preference, supported on Android 13+). Falls back to the configuration locale, then the system default.

### iOS

```kotlin
public actual object AppLocale {
    public actual val current: String
        @Composable get() = normalizeTag(rememberSystemTag())

    public actual fun current(): String = systemTagNow()
}

@Composable
private fun rememberSystemTag(): String {
    var tag by remember { mutableStateOf(systemTagNow()) }
    DisposableEffect(Unit) {
        val center = NSNotificationCenter.defaultCenter
        val observers = mutableListOf<Any>()
        fun observe(name: String) {
            observers += center.addObserverForName(name, `object` = null, queue = null) { _ ->
                tag = systemTagNow()
            }
        }
        UIApplicationWillEnterForegroundNotification?.let { observe(it) }
        UIApplicationDidBecomeActiveNotification?.let { observe(it) }
        onDispose { observers.forEach { center.removeObserver(it) } }
    }
    return tag
}

private fun systemTagNow(): String =
    (NSLocale.preferredLanguages.firstOrNull() as? String) ?: "en"

private fun normalizeTag(tag: String): String = tag.replace('_', '-')
```

`NSLocale.preferredLanguages` already returns BCP-47-shaped tags (`"uk-UA"`); `normalizeTag` is a defensive `_`→`-` swap. The `DisposableEffect` re-reads the tag when the app returns to the foreground / becomes active, so a language switch in Settings flows through without an app relaunch.

### Usage

```kotlin
// inside RootScreen
val systemLocaleTag = AppLocale.current

LaunchedEffect(systemLocaleTag) {
    DateFormatting.install(systemLocaleTag)
}

AppTheme(darkTheme = ..., localeTag = systemLocaleTag) {
    // ...
}
```

```kotlin
// inside BackendClient.defaultRequest (non-Composable)
header(HttpHeaders.AcceptLanguage, AppLocale.current())
```

## Two accessors — why

- **`val current: @Composable Boolean/String`**: lifecycle-aware, recomposes when the value changes (system theme switch, locale switch).
- **`fun current(): Boolean/String`**: synchronous; for non-Composable contexts (`BackendClient`, `RootViewModel`'s init).

Both must agree on the current value. If they diverge (e.g. one reads `AppCompatDelegate`, the other reads `Locale.getDefault()`), there's a bug — choose the priority order and commit.

## Why these are toolkit modules

`AppTheme` and `AppLocale` are read by:

- `:design-system:core/AppTheme` Composable wrapper (`darkTheme` parameter).
- `:toolkit:date-utils/DateFormatting.install(localeTag)`.
- `:data-services:backend/BackendClient.defaultRequest` (`Accept-Language`).

Different layers consume them. Toolkit is the lowest layer where the dependency can live without breaking the data layer's "no UI" rule.

## Build

```kotlin
// :toolkit:theme
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.theme" }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
    }
}

// :toolkit:localization — same plugins; needs androidx.appcompat on Android
plugins {
    id("android.library.convention")
    id("kotlin.multiplatform.convention")
    id("compose.multiplatform.convention")
}

kotlin {
    android { namespace = "com.<org>.<product>.toolkit.localization" }

    sourceSets.commonMain.dependencies {
        implementation(compose.foundation)
    }
    sourceSets.androidMain.dependencies {
        implementation(libs.androidx.appcompat)   // AppCompatDelegate.getApplicationLocales()
    }
}
```

## Rules

- **`AppTheme.current` follows system unless overridden by user preference**. The override layer is `:data-features:local-settings` (read from `:data-services:datastore`).
- **`AppLocale.current` follows system unless overridden**.
- **The `Composable` accessors must be wrapped in a `LaunchedEffect` for downstream side effects** (e.g. `DateFormatting.install(localeTag)`).

## Anti-patterns

- **Caching `AppTheme.current` in a `var` field.** It's a CompositionLocal read; cache at the call site if needed.
- **Bypassing `AppLocale.current` and using `Locale.getDefault()` directly.** Locale fallback differs (the project's accessor checks `AppCompatDelegate` first).
- **Hardcoded `darkTheme = false`** in `AppTheme(...)` outside debug. Always read from the system or user preference.
